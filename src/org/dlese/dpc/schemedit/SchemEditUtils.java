/*
	Copyright 2017 Digital Learning Sciences (DLS) at the
	University Corporation for Atmospheric Research (UCAR),
	P.O. Box 3000, Boulder, CO 80307

	Licensed under the Apache License, Version 2.0 (the "License");
	you may not use this file except in compliance with the License.
	You may obtain a copy of the License at

	http://www.apache.org/licenses/LICENSE-2.0

	Unless required by applicable law or agreed to in writing, software
	distributed under the License is distributed on an "AS IS" BASIS,
	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
	See the License for the specific language governing permissions and
	limitations under the License.
*/
package org.dlese.dpc.schemedit;

import org.dlese.dpc.schemedit.dcs.*;
import org.dlese.dpc.schemedit.security.user.User;
import org.dlese.dpc.schemedit.security.access.ActionPath;
import org.dlese.dpc.xml.schema.*;
import org.dlese.dpc.xml.*;
import org.dlese.dpc.index.*;
import org.dlese.dpc.index.reader.*;
import org.dlese.dpc.webapps.tools.GeneralServletTools;
import org.dlese.dpc.serviceclients.webclient.*;

import org.dlese.dpc.util.*;
import org.dlese.dpc.util.strings.FindAndReplace;
import org.dlese.dpc.repository.*;

import java.util.*;
import java.util.regex.*;
import java.io.*;
import java.net.*;
import java.text.*;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import org.dom4j.*;
import org.dom4j.io.XMLWriter;
import org.dom4j.io.OutputFormat;

/**
 *  Utility methods for SchemEdit
 *
 * @author    ostwald
 */
public class SchemEditUtils {

	static boolean debug = true;


	/**
	 *  General method to forward control to the appropriate page (Collections,
	 *  Search or View) after completing an operation. The editedRecId parameter is
	 *  supplied by metadata editor. It is used to highlight a record in the search
	 *  view. It may be different from SessionBean.getId(), which is set by the
	 *  viewing tool.<p>
	 *
	 *
	 *
	 * @param  request      the request object
	 * @param  editedRecId  id of record edited in metadata editor (if any)
	 * @param  sessionBean  a sessionBean instance
	 * @return              ActionForward directing to the caller of last
	 *      operation.
	 */
	public static ActionForward forwardToCaller(
	                                            HttpServletRequest request,
	                                            String editedRecId,
	                                            SessionBean sessionBean) {
		// if all else fails, we return to the collections page
		String DEFAULT_FORWARD = "/browse/home.do";
		String forwardPath = null;

		// if there is a recId in the sessionBean, then we came from the viewer
		// somewhere we should check to make sure this record exists!
		// (or leave to viewAction to handle) ...
		String recId = sessionBean.getRecId();
		if (recId != null && recId.length() > 0) {
			forwardPath = "/browse/view.do?id=" + recId;
			return new ActionForward(forwardPath);
		}

		// searchParams describes the last search performed
		String searchParams = sessionBean.getSearchParams();
		if (searchParams != null && searchParams.length() > 0) {
			// setting SessionRecId will cause the record to be highlighted in search view
			sessionBean.setRecId(editedRecId);
			forwardPath = "/browse/query.do?&s=" + sessionBean.getPaigingParam(editedRecId) + searchParams;
			return new ActionForward(forwardPath);
		}

		// prtln(" .. neither current record or query were found");
		return new ActionForward(DEFAULT_FORWARD);
	}


	/**
	 *  Contract expanded ampersands (&amp;) that are part of entity references.
	 *  For example, "&amp;#169;" and "&amp;amp;#169;" both become "&#169". "&amp;"
	 *  by itself is not contracted. Used to convert xml record into a form
	 *  desireable for xml files
	 *
	 * @param  in  String to be processed
	 * @return     processed string
	 */
	public static String contractAmpersands(String in) {
		prtln("contractAmpersands");
		Pattern p = Pattern.compile("(&amp;)[0-9a-zA-Z#]+?;");
		StringBuffer ret = new StringBuffer();
		int ind1 = 0;
		Matcher m = p.matcher(in);

		while (m.find()) {
			int ind2 = m.start();

			ret.append(in.substring(ind1, ind2));

			in = "&" + in.substring(ind2 + m.group(1).length());
			ind1 = 0;
			m = p.matcher(in);
		}
		ret.append(in.substring(ind1));
		return ret.toString();
	}


	/**
	 *  Escape spaces in the given uriStr so that it may be accepted by URI
	 *  constructor.
	 *
	 * @param  uriStr  Description of the Parameter
	 * @return         Description of the Return Value
	 */
	public static String escapeUriSpaces(String uriStr) {
		return FindAndReplace.replace(uriStr, " ", "%20", false);
	}


	/**
	 *  Expand expanded ampersands that are part of entities. For example, "&#169;"
	 *  becomes "&amp;#169". Used to convert xml text from file form into a from
	 *  preferred by dom4j.
	 *
	 * @param  in  Description of the Parameter
	 * @return     Description of the Return Value
	 */
	public static String expandAmpersands(String in) {
		prtln("expandAmpersands");

		Pattern p = Pattern.compile("&[0-9a-zA-Z#]+?;");

		StringBuffer ret = new StringBuffer();
		int ind1 = 0;
		Matcher m = p.matcher(in);

		while (m.find()) {
			int ind2 = m.start();
			// prtln ("ind1: " + ind1 + "  ind2: " + ind2);
			ret.append(in.substring(ind1, ind2));

			if (m.group().startsWith("&amp;")) {
				ret.append(m.group());
			}
			else {
				String replace = m.group().replaceFirst("&", "&amp;");
				ret.append(replace);
			}
			in = in.substring(ind2 + m.group().length());
			ind1 = 0;
			m = p.matcher(in);
		}
		ret.append(in.substring(ind1));
		return ret.toString();
	}


	/**
	 *  Returns a localized xmlDocument for the given uri. NOTE: URI's must have a
	 *  scheme of either "file" or "http".<P>
	 *
	 *  NOTEs:
	 *  <ul>
	 *    <li> this method should use getUriRef to resolve the uri into either a
	 *    file or url.
	 *    <li> what is the difference between this and getEditableXml()?
	 *  </ul>
	 *
	 *
	 * @param  uri            uri pointing to a file containing xml
	 * @return                A localized Document
	 * @exception  Exception  If scheme is not recognized or if a Document could
	 *      not be created
	 */
	public static Document getLocalizedXmlDocument(URI uri)
		 throws Exception {
		Document doc = null;
		// prtln ("\t uri.toString(): " + uri.toString());
		String scheme = uri.getScheme();
		String errorMsg = "getLocalizedXmlDocument: ";
		if (scheme != null && scheme.equals("file")) {
			String path = uri.getPath();
			File file = new File(path);
			// prtln ("\t schemaFile: " + file);

			if (!file.exists()) {
				errorMsg += "field info file does not exist at " + file;
				throw new Exception(errorMsg);
			}

			try {
				doc = Dom4jUtils.getXmlDocument(file);
			} catch (Exception e) {
				errorMsg += "file at " + path + " could not be parsed";
				throw new Exception(errorMsg);
			}

		}
		else if (scheme != null && scheme.equals("http")) {
			URL url = uri.toURL();
			try {
				doc = WebServiceClient.getTimedXmlDocument(url);
				/*
				 *  String rootElementName = rawDoc.getRootElement().getName();
				 *  doc = Dom4jUtils.localizeXml(rawDoc, rootElementName);
				 */
			} catch (Exception e) {
				errorMsg += "couldn't parse url: " + url.toString();
				throw new Exception(errorMsg);
			}
		}
		else {
			errorMsg += "unrecognized uri scheme: " + scheme + " (recognized schemes are \"http\" and \"file\")";
			// prtln(errorMsg);
			throw new Exception(errorMsg);
		}
		doc = localizeXml(doc);
		if (doc == null) {
			prtln("WARNING: returning null from getLocalizedXmlDocument (" + uri.toString() + ")");
		}

		return doc;
	}


	/**
	 *  Gets the uriRef attribute of the SchemEditUtils class
	 *
	 * @param  uriStr                  Description of the Parameter
	 * @return                         The uriRef value
	 * @exception  Exception           Description of the Exception
	 * @exception  URISyntaxException  Description of the Exception
	 */
	public static Object getUriRef(String uriStr)
		 throws Exception, URISyntaxException {
		return getUriRef(uriStr, null);
	}


	/**
	 *  Gets the uriRef attribute of the SchemEditUtils class
	 *
	 * @param  uri            Description of the Parameter
	 * @return                The uriRef value
	 * @exception  Exception  Description of the Exception
	 */
	public static Object getUriRef(URI uri)
		 throws Exception {
		return getUriRef(uri, null);
	}


	/**
	 *  Gets the uriRef attribute of the SchemEditUtils class
	 *
	 * @param  uriStr                  Description of the Parameter
	 * @param  docRoot                 Description of the Parameter
	 * @return                         The uriRef value
	 * @exception  Exception           Description of the Exception
	 * @exception  URISyntaxException  Description of the Exception
	 */
	public static Object getUriRef(String uriStr, String docRoot)
		 throws Exception, URISyntaxException {
		URI uri = new URI(uriStr);
		return getUriRef(uri, docRoot);
	}


	/**
	 *  Given a string representation of a uri, returns either a File or URL
	 *  instance.<p>
	 *
	 *  Relative paths are assumed to be file paths relative to "docRoot" and
	 *  expanded accordingly into absolute files.
	 *
	 * @param  docRoot        Path to docRoot for this application to aid in
	 *      resolving relative paths.
	 * @param  uri            Description of the Parameter
	 * @return                The uriRef value
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public static Object getUriRef(URI uri, String docRoot)
		 throws Exception {

		try {
			String scheme = uri.getScheme();
			String errorMsg = null;

			// non-absolute URIs do not specify a scheme - THESE ARE ASSUMED TO BE FILE PATHS
			// relative file paths do not begin with a "/" - these require docRoot to be resolved.
			if (!uri.isAbsolute() || scheme.equals("file")) {

				String path = uri.getPath();
				if (path == null || path.length() == 0) {
					throw new Exception("url did not contain a path component (" + uri.toString() + ")");
				}

				// now we have to determine if the FILE path specified by the URI is relative or absolute
				boolean pathIsRelative = !path.substring(0, 1).equals("/");
				if (pathIsRelative) {
					if (docRoot == null) {
						throw new Exception("cannot resolve relative path without a value for docRoot");
					}
					else {
						// if we are running on a Windows platform, convert docRoot (if non-null)
						// to Unix form to be compatable with the uri string, which presumably will
						// be expressed as a unix path.
						if (File.separator.equals("\\")) {
							docRoot = FindAndReplace.replace(docRoot, "\\", "/", false);
						}

						String absPath = GeneralServletTools.getAbsolutePath(path, docRoot);
						return new File(absPath);
					}
				}
				else {
					return new File(path);
				}
			}
			else if (scheme.equals("http")) {
				return uri.toURL();
			}
			else {
				errorMsg = "unrecognized uri scheme: " + scheme;
				prtln(errorMsg);
				throw new Exception(errorMsg);
			}
		} catch (Throwable e) {
			e.printStackTrace();
			throw new Exception(e.getMessage());
		}
	}


	/**
	 *  Gets the editableXml attribute of the SchemEditUtils class
	 *
	 * @param  file           NOT YET DOCUMENTED
	 * @return                The editableXml value
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public static String getEditableXml(File file) throws Exception {
		String uriStr = file.toURI().toString();
		return getEditableXml(uriStr, null);
	}


	/**
	 *  Returns the xml content from the given uri.
	 *
	 * @param  docUri         Description of the Parameter
	 * @param  docRoot        Description of the Parameter
	 * @return                The editableXml value
	 * @exception  Exception  Description of the Exception
	 */
	private static String getEditableXml(String docUri, String docRoot)
		 throws Exception {
		Object docLocation = getUriRef(docUri, docRoot);
		String xml = null;
		if (docLocation instanceof File) {
			xml = Files.readFileToEncoding((File) docLocation, "UTF-8").toString();
		}
		else if (docLocation instanceof URL) {
			xml = WebServiceClient.getTimedURL((URL) docLocation);
		}
		return (xml);
	}


	/**
	 *  Removes all namespace information from a Document, including
	 *  schemaLocation.
	 *
	 * @param  doc  NOT YET DOCUMENTED
	 * @return      NOT YET DOCUMENTED
	 */
	private static Document localizeXml(Document doc) {
		Element rootElement = doc.getRootElement();
		// remove the schemaLocation or noNamespaceSchemaLocation attributes
		if (rootElement != null) {
			Attribute schemaLocation = rootElement.attribute("schemaLocation");
			if (schemaLocation != null) {
				rootElement.remove(schemaLocation);
			}
			Attribute noNamespaceSchemaLocation = rootElement.attribute("noNamespaceSchemaLocation");
			if (noNamespaceSchemaLocation != null) {
				rootElement.remove(noNamespaceSchemaLocation);
			}
		}
		return Dom4jUtils.localizeXml(doc);
	}


	/**
	 *  Gets the editableDocument attribute of the SchemEditUtils class
	 *
	 * @param  docUri                 Description of the Parameter
	 * @return                        The editableDocument value
	 * @exception  DocumentException  Description of the Exception
	 */
	private static Document getEditableDocument(String docUri)
		 throws DocumentException {
		return getEditableDocument(docUri, true);
	}


	/**
	 *  Returns the localized dom4j.Document (with ampersands expanded) from the
	 *  given uri.
	 *
	 * @param  docUri                 Description of the Parameter
	 * @param  localize               Description of the Parameter
	 * @return                        The editableDocument value
	 * @exception  DocumentException  Description of the Exception
	 */
	private static Document getEditableDocument(String docUri, boolean localize)
		 throws DocumentException {
		try {
			String xml = getEditableXml(docUri, null);
			Document doc = Dom4jUtils.getXmlDocument(xml);
			Element root = doc.getRootElement();
			// return Dom4jUtils.localizeXml(doc, root.getName());

			// prtln("schemEditUtils.getEditableDocument before localizing:" + Dom4jUtils.prettyPrint(doc));

			if (localize) {
				Document localizedDoc = localizeXml(doc);
				// prtln("\n\n\nschemEditUtils.getEditableDocument AFTER localizing:" + Dom4jUtils.prettyPrint(localizedDoc));
				return localizedDoc;
			}
			else {
				// prtln("\n\n\nschemEditUtils.getEditableDocument NOT localized:" + Dom4jUtils.prettyPrint(doc));
				return doc;
			}
		} catch (Exception e) {
			throw new DocumentException(e.getMessage());
		}
	}


	/**
	 *  Returns the localized dom4j.Document from the given uri after checking that
	 *  the schemaLocation declared by the document at uri corresponds with the
	 *  schemaLocation known to the MetaDataFramework parameter.<p>
	 *
	 *  called from MetaDataFramework.getEditableDocument.
	 *
	 * @param  docUri                 uri of xml document
	 * @param  framework              MetaDataFramework instance for consistency
	 *      checking.
	 * @return                        localized dom4j.Document with ampersands
	 *      expanded for editing.
	 * @exception  DocumentException  Description of the Exception
	 */
	public static Document getEditableDocument(String docUri, MetaDataFramework framework)
		 throws DocumentException {
		// prtln("getEditableDocument() with " + docUri);

		boolean enforceSchemaLocation = false;
		String errorMsg = null;

		String schemaTargetNameSpace = framework.getSchemaHelper().getTargetNamespace();
		String schemaLocation = framework.getSchemaURI();

		/*
		 *  prtln("framework attributes");
		 *  prtln("\t schemaTargetNameSpace: " + schemaTargetNameSpace);
		 *  prtln("\t schemaLocation: " + schemaLocation);
		 *  prtln("");
		 */
		try {
			String rawXml = getEditableXml(docUri, framework.getDocRoot());

			Document doc = Dom4jUtils.getXmlDocument(rawXml);
			// pp (doc);
			Element root = doc.getRootElement();
			String rootName = root.getQualifiedName();
			if (!rootName.equals(framework.getRootElementName())) {
				throw new Exception("Root element (\"" + rootName +
					"\") does not match schema (expected \"" +
					framework.getRootElementName() + "\")");
			}
			/*
			 *  if (!root.getName().equals(framework.getRootElementName())) {
			 *  throw new Exception("Root element (\"" + root.getName() +
			 *  "\") does not match schema (expected \"" +
			 *  framework.getRootElementName() + "\")");
			 *  }
			 */
			// schema location
			Attribute schemaLocAtt = root.attribute("schemaLocation");
			Attribute noSchemaLocAtt = root.attribute("noNamespaceSchemaLocation");
			String docTargetNameSpace = null;
			String docSchemaLocation = null;

			if (schemaLocAtt != null) {
				String[] atts = schemaLocAtt.getText().split("\\s");
				if (atts.length < 2) {
					errorMsg = "Malformed record: schema location attribute of root element must contain two elements";
					throw new Exception(errorMsg);
				}
				docTargetNameSpace = atts[0];
				docSchemaLocation = atts[1];
			}
			else if (noSchemaLocAtt != null) {
				docSchemaLocation = noSchemaLocAtt.getText();
			}
			else {
				throw new Exception("schemaLocation not specified in instance document");
			}

			// For now we don't check schema location for collection_config documents due to
			// trouble with windows paths as schemaLocations (schemaLocations are NOT URIs??)
			if (!framework.getXmlFormat().equals("collection_config")) {
				// make sure docSchemaLocation is an absolute path
				URI uri = null;
				try {
					uri = new URI(docSchemaLocation);
				} catch (Exception e) {
					throw new Exception("docSchemaLocation (" + docSchemaLocation + ") could not be parsed as a URI");
				}
				if (!uri.isAbsolute()) {
					docSchemaLocation = "file:" + GeneralServletTools.getAbsolutePath(docSchemaLocation, framework.getDocRoot());
				}

				/*
				 *  prtln(" document attributes");
				 *  prtln("\t docSchemaLocation: " + docSchemaLocation);
				 *  prtln("\t docTargetNameSpace: " + docTargetNameSpace);
				 *  prtln("");
				 */
				// if schemaTargetNameSpace is specified, then the document must declare a targetNameSpace
				if (schemaTargetNameSpace != null &&
					(docTargetNameSpace == null ||
					!docTargetNameSpace.equals(schemaTargetNameSpace))) {
					throw new Exception("document target namespace does not match schema (" + schemaTargetNameSpace + ")");
				}

				if (!docSchemaLocation.equals(schemaLocation)) {
					errorMsg = "schema location (" + schemaLocation + ") does not match metadata framework (" + docSchemaLocation + ")";
					if (enforceSchemaLocation) {
						throw new Exception(errorMsg);
					}
					else {
						prtln("WARNING: " + errorMsg);
					}
				}
			}

			if (!framework.getSchemaHelper().getNamespaceEnabled()) {
				doc = localizeXml(doc);
			}
			else {
				// prtln("\n\n\nschemEditUtils.getEditableDocument NOT localized:" + Dom4jUtils.prettyPrint(doc));
			}

			if (doc == null) {
				throw new Exception("document could not be parsed");
			}

			return doc;
		} catch (Exception e) {
			e.printStackTrace();
			throw new DocumentException("getEditableDocument ERROR processing " + docUri + ": " + e.getMessage());
		} catch (Throwable t) {
			t.printStackTrace();
			throw new DocumentException("getEditableDocument: unknown error processing " + docUri);
		}
	}


	/**
	 *  Displays request params for debugging.
	 *
	 * @param  request  Description of the Parameter
	 */
	public static void showReferer(HttpServletRequest request) {
		prtln("referer: " + request.getHeader("referer"));
	}


	/**
	 *  Displays request params for debugging.<p>
	 *
	 *  Filtering out kinds of parameters. Construct an array containing strings
	 *  which, if found in a parameter name, will filter that param from the
	 *  display. E.g.,
	 *  <ul>
	 *    <li> metadata editor form values contain params: "("
	 *    <li> metadata editor collapse params contain "_^_"
	 *  </ul>
	 *
	 *
	 * @param  request  Description of the Parameter
	 */
	public static void showRequestParameters(HttpServletRequest request) {
		String[] defaultFilter = new String[]{"(", "_^_"};
		showRequestParameters(request, defaultFilter);
	}


	/**
	 *  Description of the Method
	 *
	 * @param  request      Description of the Parameter
	 * @param  paramFilter  Description of the Parameter
	 */
	public static void showRequestParameters(HttpServletRequest request, String[] paramFilter) {
		if (debug) {
			prtln("Filtered Request Parameters", "");
			for (Enumeration e = request.getParameterNames(); e.hasMoreElements(); ) {
				boolean rejected = false;
				String param = (String) e.nextElement();
				// prtln ("param: " + param);
				if (paramFilter != null && paramFilter.length > 0) {
					for (int i = 0; i < paramFilter.length; i++) {
						// prtln ("  ... checking against: " + paramFilter[i]);
						if (param.indexOf(paramFilter[i]) != -1) {
							// prtln ("  ...... rejected");
							rejected = true;
							break;
						}
					}
				}
				if (rejected) {
					continue;
				}
				String[] values = request.getParameterValues(param);
				if (values.length == 1) {
					if (values[0] != null && values[0].trim().length() > 0) {
						System.out.println("\t" + param + ": " + values[0]);
					}
				}
				else {
					System.out.println("\t" + param + " (" + values.length + "):");
					for (int i = 0; i < values.length; i++) {
						System.out.println("\t\t" + values[i]);
					}
				}
			}
		}
	}


	/**
	 *  DEPRECIATED - use getCollectionOfIndexedRecord Returns the collection
	 *  associated with an indexed record.
	 *
	 * @param  id     record id
	 * @param  index  a SimpleLuceneIndex instance
	 * @return        The collection attribute for the record identified by id
	 */
	public static String getCollectionFromIndex(String id, SimpleLuceneIndex index) {
		ResultDocList results = index.searchDocs("id:\"" + SimpleLuceneIndex.encodeToTerm(id) + "\"");
		if (results == null || results.size() == 0) {
			prtln("index record not found for id: " + id);
			return null;
		}
		XMLDocReader docReader = (XMLDocReader) ((ResultDoc)results.get(0)).getDocReader();
		return docReader.getCollection();
	}


	/**
	 *  Returns the collection associated with an indexed record.
	 *
	 * @param  id  record id
	 * @param  rm  Description of the Parameter
	 * @return     The collection attribute for the record identified by id
	 */
	public static String getCollectionOfIndexedRecord(String id, RepositoryManager rm) {
		ResultDoc record = rm.getRecord(id);
		if (record == null) {
			prtln("indexed record not found for id: " + id);
			return null;
		}
		XMLDocReader docReader = (XMLDocReader) record.getDocReader();
		if (docReader == null) {
			return null;
		}
		else {
			return docReader.getCollection();
		}
	}


	/**
	 *  Gets the setInfo attribute of the SchemEditUtils class
	 *
	 * @param  collection  Description of the Parameter
	 * @param  rm          Description of the Parameter
	 * @return             The setInfo value
	 */
	public static SetInfo getSetInfo(String collection, RepositoryManager rm) {
		SetInfo setInfo = rm.getSetInfo(collection);
		if (setInfo == null) {
			prtln("SchemEditUtils.getSetInfo(): setInfo not found for " + collection);
		}
		return setInfo;
	}


	/**
	 *  Gets the dcsSetInfo attribute of the SchemEditUtils class
	 *
	 * @param  collection  Description of the Parameter
	 * @param  rm          Description of the Parameter
	 * @return             The dcsSetInfo value
	 */
	public static DcsSetInfo getDcsSetInfo(String collection, RepositoryManager rm) {
		// prtln("DcsSetInfo trying to find collection: " + collection);
		DcsSetInfo dcsSetInfo = null;
		SetInfo setInfo = getSetInfo(collection, rm);
		if (setInfo != null) {
			dcsSetInfo = new DcsSetInfo(setInfo);
			dcsSetInfo.setSetInfoData(rm);
		}
		return dcsSetInfo;
	}



	/**
	 *  pick a given DcsSetInfo from a list
	 *
	 * @param  collection   Description of the Parameter
	 * @param  dcsSetInfos  Description of the Parameter
	 * @return              The dcsSetInfo value
	 */
	public static DcsSetInfo getDcsSetInfo(String collection, List dcsSetInfos) {
		// prtln("DcsSetInfo trying to find collection: " + collection);
		for (Iterator i = dcsSetInfos.iterator(); i.hasNext(); ) {
			DcsSetInfo dcsSetInfo = (DcsSetInfo) i.next();
			if (dcsSetInfo.getSetSpec().equals(collection)) {
				return dcsSetInfo;
			}
		}
		prtln("SchemEditUtils.getDcsSetInfo(): dcsSetInfo not found for " + collection);
		return null;
	}


	/**
	 *  Returns the file associated with an id in an index.
	 *
	 * @param  id  record id
	 * @param  rm  Description of the Parameter
	 * @return     The source file for the record identified by id
	 */
	public static File getFileFromIndex(String id, RepositoryManager rm) {
		ResultDoc record = rm.getRecord(id);
		if (record == null) {
			prtln("indexed record not found for id: " + id);
			return null;
		}
		XMLDocReader docReader = (XMLDocReader) record.getDocReader();
		return docReader.getFile();
	}



	/**
	 *  Description of the Method
	 *
	 * @param  sessionUser  Description of the Parameter
	 * @param  mapping      Description of the Parameter
	 */
	public static void showRoleInfo(User sessionUser, ActionMapping mapping) {
		prtln("\n RoleInfo");
		if (sessionUser != null) {
			prtln(sessionUser.toString());
		}
		else {
			prtln("SessionUser is NULL");
		}
		prtln(ActionPath.getRole(mapping).toString());
	}


	/**
	 *  Checks given string for characters outside the range of ASCII values
	 *
	 * @param  s  String to be checked
	 * @return    true if String contains characters with ASCII value above 127.
	 */
	public static boolean hasBadChar(String s) {
		for (int i = 0; i < s.length(); ++i) {
			char c = s.charAt(i);
			if ((int) c > 127) {
				return true;
			}
		}
		return false;
	}



	/**
	 *  Gets the uniqueId attribute of the SchemEditUtils class
	 *
	 * @return    The uniqueId value
	 */
	public static String getUniqueId() {
		// long uid = new Date().getTime();
		long uid = Utils.getUniqueID();
		return new Long(uid).toString();
	}


	/**
	 *  Display request headers for debugging.
	 *
	 * @param  request  Description of the Parameter
	 */
	public static void showRequestHeaders(HttpServletRequest request) {
		if (debug) {
			prtln("Request Headers");
			for (Enumeration e = request.getHeaderNames(); e.hasMoreElements(); ) {
				String header = (String) e.nextElement();
				String value = request.getHeader(header);
				System.out.println("\t" + header + ": " + value);
			}
		}
	}


	/**
	 *  Displays the request attributes for debugging..
	 *
	 * @param  request  Description of the Parameter
	 */
	private void showRequestAttributes(HttpServletRequest request) {
		if (debug) {
			prtln("Request Attributes");
			for (Enumeration e = request.getAttributeNames(); e.hasMoreElements(); ) {
				String attribute = (String) e.nextElement();
				Object o = request.getAttribute(attribute);
				if (o instanceof String) {
					System.out.println("\t" + attribute + ": " + (String) o);
				}
				else {
					System.out.println("\t" + attribute + " (" + o.getClass().getName() + ")");
					try {
						System.out.println("\t\t" + o.toString());
					} catch (Throwable t) {}
				}
			}
		}
	}


	/**
	 *  Encodes query strings beginning with "id:" or "url:" using {@link
	 *  SimpleLuceneIndex#encodeToTerm}.
	 *
	 * @param  q  the query string
	 * @return    thge formatted query string
	 */
	public static String formatQuery(String q) {
		q = q.trim();
		if (q.startsWith("id:") && !q.matches(".*\\s+.*")) {
			q = q.substring(3, q.length());
			q = "id:" + SimpleLuceneIndex.encodeToTerm(q, false);
			return q;
		}
		if (q.startsWith("url:") && !q.matches(".*\\s+.*")) {
			q = q.substring(4, q.length());
			q = "urlenc:" + SimpleLuceneIndex.encodeToTerm(q, false);
			return q;
		}
		return q;
	}


	/**
	 *  Return the given string wrapped in quotation marks
	 *
	 * @param  s  the string to quote wrap
	 * @return    the quoted string
	 */
	public static String quoteWrap(String s) {
		if (s == null) {
			return "";
		}
		else {
			return "\"" + s + "\"";
		}
	}


	/**  "yyyy-MM-dd'T'HH:mm:ss'Z'"  */
	public static String utcDateFormatString = "yyyy-MM-dd'T'HH:mm:ss'Z'";
	/**  Formats and parses dates according to utcDateFormatString.  */
	public static SimpleDateFormat utcDateFormat = new SimpleDateFormat(utcDateFormatString);

	/**  same as utcDateFormatString  */
	public static String fullDateFormatString = utcDateFormatString;
	/**
	 *  * Formats and parses dates according to {@link #fullDateFormatString}.
	 */
	public static SimpleDateFormat fullDateFormat = new SimpleDateFormat(fullDateFormatString);


	/**
	 *  Converts a Date object into a string formatted with {@link #fullDateFormat}
	 *
	 * @param  date  date to be formatted
	 * @return       formatted date
	 */
	public static String fullDateString(Date date) {
		if (date == null) {
			return fullDateFormat.format(new Date(0));
		}
		else {
			return fullDateFormat.format(date);
		}
	}


	/**  "yyyy-MM-dd"  */
	public static String simpleDateFormatString = "yyyy-MM-dd";
	/**
	 *  Formats and parses dates according to {@link #simpleDateFormatString}.
	 */
	public static SimpleDateFormat simpleDateFormat = new SimpleDateFormat(simpleDateFormatString);


	/**
	 *  Converts a Date object into a string formatted with {@link #simpleDateFormat}
	 *
	 * @param  date  date to be formatted
	 * @return       formatted date
	 */
	public static String simpleDateString(Date date) {
		if (date == null) {
			return simpleDateFormat.format(new Date(0));
		}
		else {
			return simpleDateFormat.format(date);
		}
	}


	/**
	 *  formats the given date in the form "yyyy-MM-dd'T'HH:mm:ss'Z'". If given
	 *  date is null the string return reflects "the epoch", namely January 1,
	 *  1970, 00:00:00 GMT.
	 *
	 * @param  date  a date object to be formatted
	 * @return       the formatted string representation of date
	 */
	public static String utcDateString(Date date) {
		if (date != null) {
			return utcDateFormat.format(date);
		}
		else {
			return utcDateFormat.format(new Date(0));
		}
	}


	/**
	 *  Gets the utcTime attribute of the SchemEditUtils class
	 *
	 * @return    The utcTime value
	 */
	public static Date getUtcTime() {
		return getUtcTime(new Date());
	}


	/**
	 *  Gets the utcTime attribute of the SchemEditUtils class
	 *
	 * @param  date  the date object
	 * @return       The utcTime value
	 */
	public static Date getUtcTime(Date date) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		int calOffset = -(cal.get(Calendar.ZONE_OFFSET) + cal.get(Calendar.DST_OFFSET)) / (60 * 1000);
		cal.add(Calendar.MINUTE, calOffset);
		return cal.getTime();
	}



	/**
	 *  Return a string representation of the elapsed time since an event date
	 *  (which is represented as "clicks" since "the epoch").<p>
	 *
	 *  NOTE: this method does not consider elapsed times greater than 24 hours
	 *  (the elapsed days are simply not shown).
	 *
	 * @param  clicks  Description of the Parameter
	 * @return         The elapsedTimeSimple value
	 */
	public static String getElapsedTimeSimple(long clicks) {
		long now = SchemEditUtils.getUtcTime().getTime();
		long elapsedTime = now - clicks;
		Calendar cal = new GregorianCalendar();
		// SimpleDateFormat df = new SimpleDateFormat( "H'hrs' mm'mins' ss'secs'");
		SimpleDateFormat df = new SimpleDateFormat("H'hrs' m'mins' s'secs'");
		cal.setTimeInMillis(elapsedTime);
		String formattedTime = df.format(cal.getTime());
		prtln("getElapsedTime returning: " + formattedTime);
		return formattedTime;
	}


	/**
	 *  The main program for the SchemEditUtils class
	 *
	 * @param  args           The command line arguments
	 * @exception  Exception  Description of the Exception
	 */
	public static void main(String[] args) throws Exception {
		prtln(toWinPath("/foo/farb/"));

		String path = "D:/Documents and Settings/ostwald/devel/projects/metadata-ui-project";
		File file = new File(path);
		String uriStr = file.toURI().toString();
		Object ref = SchemEditUtils.getUriRef(uriStr);
		if (ref instanceof File) {
			prtln("file: " + ((File) ref).toString());
		}
		else if (ref instanceof URL) {
			prtln("url: " + ((URL) ref).toString());
		}
	}


	/**
	 *  Replace forward slashes in provided path with backward slashes
	 *
	 * @param  path  the path to convert
	 * @return       converted path
	 */
	public static String toWinPath(String path) {
		return FindAndReplace.replace(path, "/", "\\", false);
	}


	/**
	 *  Description of the Method
	 *
	 * @param  name   Description of the Parameter
	 * @param  value  Description of the Parameter
	 */
	static void prtParam(String name, String value) {
		if (value == null) {
			prtln(name + " is null");
		}
		else {
			prtln(name + " is " + value);
		}
	}


	/**
	 *  Extracts mirrorUrl values from an adn record
	 *
	 * @param  recordXml  xml string to search for mirrorUrls
	 * @return            The mirrorUrls value
	 */
	public static List getMirrorUrls(String recordXml) {
		// create a localized Document from the xmlString
		List mirrors = new ArrayList();
		try {
			Document doc = Dom4jUtils.getXmlDocument(recordXml);
			if (doc == null) {
				throw new Exception("could not parse provided recordXML");
			}

			String mirrorPath = "/itemRecord/technical/online/mirrorURLs/mirrorURL";
			List nodes = doc.selectNodes(mirrorPath);

			if (nodes != null) {
				for (Iterator i = nodes.iterator(); i.hasNext(); ) {
					Element e = (Element) i.next();
					String mirror = e.getText();
					if (mirror != null && mirror.trim().length() > 0) {
						mirrors.add(mirror);
					}
				}
			}
		} catch (Throwable t) {
			prtln("getMirrorUrls error: " + t.getMessage());
			t.printStackTrace();
		}
		return mirrors;
	}


	/**
	 *  Insert a new id into an existing metadata record
	 *
	 * @param  recordXml      metadata record as xml string
	 * @param  id             id to insert into the metadata record
	 * @param  framework      MetaDataFramework object for the format of the record
	 * @return                Description of the Return Value
	 * @exception  Exception  Description of the Exception
	 */
	public static String stuffId(String recordXml, String id, MetaDataFramework framework)
		 throws Exception {
		// prtln ("stuffId: \n\trecordXml: " + recordXml + "\n\txmlFormat: " + xmlFormat + "\n\tid: " + id);
		String idPath = framework.getIdPath();
		String xmlFormat = framework.getXmlFormat();
		SchemaHelper schemaHelper = framework.getSchemaHelper();

		// create a localized Document from the xmlString
		Document doc = Dom4jUtils.getXmlDocument(recordXml);
		if (doc == null) {
			throw new Exception("could not parse provided recordXML");
		}

		if (!framework.getSchemaHelper().getNamespaceEnabled()) {
			doc = localizeXml(doc); // only need to localize if schema is NOT namespace aware ...
			if (doc == null) {
				throw new Exception("doc could not be localized - please unsure the record's root element contains namespace information");
			}
		}

		DocMap docMap = new DocMap(doc, schemaHelper);
		try {
			docMap.smartPut(idPath, id);
		} catch (Exception e) {
			throw new Exception("Unable to insert ID: " + e.getMessage());
		}

		return framework.getWritableRecordXml(doc);
	}


	/**
	 *  Based on attribute name, attempts to get the value of that attribute using
	 *  a bean-style "gettter".
	 *
	 * @param  obj   object for which to get attr
	 * @param  attr  attribute to get
	 * @return       The attr value
	 */
	public static Object getAttr(Object obj, String attr) {
		Object value = null;
		if (attr == null || attr.length() < 2) {
			// prtln ("bogus attr: \"" + attr + "\"");
			return value;
		}
		String methodName = "get" + attr.substring(0, 1).toUpperCase() + attr.substring(1);
		// prtln ("methodName: " + methodName);
		java.lang.reflect.Method method = null;
		try {
			method = new User().getClass().getMethod(methodName, new Class[0]);
		} catch (NoSuchMethodException e) {
			prtln("WARNING: accessor not found for \"" + attr + "\"");
			return null;
		} catch (Exception e) {
			prtln("WARNING: couldnt create User for class ..");
			return null;
		}

		try {
			value = method.invoke(obj);
		} catch (Throwable e) {
			// throw new Exception (e.getCause().getMessage());
			prtln("WARNING: accessor not found for \"" + attr + "\"");
		}
		return value;
	}


	/**
	 *  Description of the Method
	 *
	 * @param  s       Description of the Parameter
	 * @param  prefix  Description of the Parameter
	 */
	public static void prtln(String s, String prefix) {

		while (s != null && s.length() > 0 && s.charAt(0) == '\n') {
			System.out.println("");
			s = s.substring(1);
		}

		if (prefix == null || prefix.trim().length() == 0) {
			System.out.println(s);
		}
		else {
			System.out.println(prefix + ": " + s);
		}
	}


	/**
	 *  Print a line to standard out.
	 *
	 * @param  s  The String to print.
	 */
	public static void prtln(String s) {
		if (debug) {
			// System.out.println("SchemEditUtils: " + s);
			SchemEditUtils.prtln(s, "SchemEditUtils");
		}
	}


	/**
	 *  Description of the Method
	 *
	 * @param  s  Description of the Parameter
	 */
	public static void box(String s) {
		System.out.println("\n----------------------------------");
		System.out.println(s);
		System.out.println("----------------------------------\n");
	}


	/**
	 *  Description of the Method
	 *
	 * @param  s  Description of the Parameter
	 */
	static void prtlnErr(String s) {
		System.err.println("SchemEditUtils: " + s);
	}

}

