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

import java.net.URL;
import java.text.DateFormat;
import java.util.*;
import java.io.File;
import java.util.regex.*;
import java.net.*;

import org.dlese.dpc.xml.*;
import org.dlese.dpc.util.strings.FindAndReplace;
import org.dlese.dpc.util.Files;
import org.dlese.dpc.index.reader.*;
import org.dlese.dpc.index.SimpleLuceneIndex;
import org.dlese.dpc.repository.SetInfo;
import org.dlese.dpc.schemedit.*;
import org.dlese.dpc.schemedit.dcs.*;
import org.dlese.dpc.schemedit.config.StatusFlags;
import org.dlese.dpc.schemedit.security.user.User;
import org.dlese.dpc.schemedit.security.user.UserManager;
import org.dlese.dpc.schemedit.vocab.FieldInfoReader;
import org.dlese.dpc.schemedit.standards.CATServiceHelper;
import org.dlese.dpc.schemedit.standards.asn.AsnStandardsNode;

import javax.servlet.http.HttpServletRequest;

import org.apache.lucene.document.Document;
import org.dom4j.*;

/**
 *  Provides functionality used by jsp function calls.
 *
 * @author    ostwald
 */
public class JspFunctions {

	private static boolean debug = false;


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  user  NOT YET DOCUMENTED
	 * @param  role  NOT YET DOCUMENTED
	 * @return       NOT YET DOCUMENTED
	 */
	public static boolean hasRole(User user, String role) {
		return user.hasRole(role);
	}
	
	/**
	 *  Returns true if user has specified permission for specified collectino
	 *
	 * @param  user  the user oject
	 * @param  role  role represented as a string (e.g., "manager")
	 * @return       true if user has at least specified role
	 */
	public static boolean hasCollectionRole(User user, String role, String collection) {
		return user.hasRole(role, collection);
	}

	/**
	 *  Gets the authorized attribute of the JspFunctions class
	 *
	 * @param  operation    NOT YET DOCUMENTED
	 * @param  sessionBean  NOT YET DOCUMENTED
	 * @return              The authorized value
	 */
	public static boolean isAuthorized(String operation, SessionBean sessionBean) {
		return isAuthorized(operation, sessionBean, null);
	}


	/**
	 *  Returns the xpath for the provided named schema path (pathName) as
	 *  configured for the specified framework (xmlFormat). Used on the "create
	 *  collection page" to give background information about some optional
	 *  information associated with some frameworks.
	 *
	 * @param  pathName   configured path name (from framework config)
	 * @param  xmlFormat  speicified metadata framework (e.g., "adn")
	 * @param  reg        the FrameworkRegistry
	 * @return            The bestPracticesLink value
	 */
	public static String getNamedXPath(String pathName, String xmlFormat, FrameworkRegistry reg) {
		if (xmlFormat != null && reg != null) {
			MetaDataFramework framework = reg.getFramework(xmlFormat);
			if (framework != null) {
				String xpath = framework.getNamedSchemaPathXpath(pathName);
				if (xpath != null && framework.getFieldInfo(xpath) != null) {
					return xpath;
				}
			}
		}
		return null;
	}


	/**
	 *  Gets the authorized attribute of the JspFunctions class
	 *
	 * @param  operation    NOT YET DOCUMENTED
	 * @param  sessionBean  NOT YET DOCUMENTED
	 * @param  collection   NOT YET DOCUMENTED
	 * @return              The authorized value
	 */
	public static boolean isAuthorized(String operation, SessionBean sessionBean, String collection) {
		if (sessionBean == null) {
			prtln("WARNING: isAuthorized cannot access sessionBean and is blindly authorizing the operation");
			return true;
		}
		return sessionBean.isAuthorized(operation, collection);
	}


	/**
	 *  Test for existence of file relative to the jsp page in which the function
	 *  is called.
	 *
	 * @param  relativePath  NOT YET DOCUMENTED
	 * @param  request       NOT YET DOCUMENTED
	 * @return               NOT YET DOCUMENTED
	 */
	public static boolean fileExists(String relativePath, HttpServletRequest request) {
		File file = getFile(relativePath, request);
		return file.exists();
	}


	/**
	 *  Finds a file relative to the jsp page in which the function is called.
	 *
	 * @param  relativePath  NOT YET DOCUMENTED
	 * @param  request       NOT YET DOCUMENTED
	 * @return               The file value
	 */
	private static File getFile(String relativePath, HttpServletRequest request) {
		String servletPath = request.getServletPath();
		String realPath = request.getRealPath(servletPath);
		File dir = new File(realPath).getParentFile();
		File file = new File(dir, relativePath);
		return file;
	}


	/**
	 *  Gets the dcsDataDocReader attribute of the JspFunctions class
	 *
	 * @param  docReader  Description of the Parameter
	 * @return            The dcsDataDocReader value
	 */
	public static DcsDataDocReader getDcsDataDocReader(DocReader docReader) {
		SimpleLuceneIndex index = docReader.getIndex();
		Document doc = docReader.getDocument();
		return new DcsDataDocReader(doc, index);
	}


	/**
	 *  Truncate provided string to specified length.
	 *
	 * @param  s       string to truncate
	 * @param  length  truncation length
	 * @return         truncated string
	 */
	public static String truncate(String s, int length) {
		if (s == null)
			return "";
		if (s.length() > length)
			return s.substring(0, length) + "&nbsp;...";
		return s;
	}


	/**
	 *  Gets the fullName attribute of the JspFunctions class
	 *
	 * @param  username     a username
	 * @param  userManager  the UserManager
	 * @return              the fullName of specified user
	 */
	public static String getFullName(String username, UserManager userManager) {
		User user = userManager.getUser(username);
		return (user != null ? user.getFullName() : "<i>" + username + "</i>");
	}


	/**
	 *  Gets the statusLabel for the specified collection, converting rawLabel to
	 *  it's human-readable form if it is a "finalStatus".
	 *
	 * @param  rawLabel     NOT YET DOCUMENTED
	 * @param  collection   NOT YET DOCUMENTED
	 * @param  sessionBean  NOT YET DOCUMENTED
	 * @return              The statusLabel value
	 */
	public static String getStatusLabel(String rawLabel, String collection, SessionBean sessionBean) {
		if (StatusFlags.isFinalStatusValue(rawLabel)) {
			String derivedCollection = StatusFlags.getCollection(rawLabel);
			// prtln ("derivedCollection " + derivedCollection);
			if (!derivedCollection.equals(collection)) {
				// prtln ("WARNING: statusLabel (" + rawLabel + ") could not be resolved for given collection (" + collection + ")");
			}
		}
		if (rawLabel.equals(StatusFlags.getFinalStatusValue(collection))) {
			return sessionBean.getFinalStatusLabel(collection);
		}
		else
			return rawLabel;
	}


	/**
	 *  Gets the setLabel attribute of the JspFunctions class
	 *
	 * @param  setInfo  NOT YET DOCUMENTED
	 * @return          The setLabel value
	 */
	public static String getSetLabel(SetInfo setInfo) {
		return ("0" + setInfo.getSetSpec());
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  items  NOT YET DOCUMENTED
	 * @param  item   NOT YET DOCUMENTED
	 * @return        NOT YET DOCUMENTED
	 */
	public static boolean listContains(List items, String item) {
		if (items == null)
			return false;
		return items.contains(item);
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  items  NOT YET DOCUMENTED
	 * @param  item   NOT YET DOCUMENTED
	 * @return        NOT YET DOCUMENTED
	 */
	public static boolean arrayContains(String[] items, String item) {
		if (items == null || items.length == 0)
			return false;
		return Arrays.asList(items).contains(item);
	}


	/**
	 *  Converts either a list or a iterable to a comma-delmited string, which can
	 *  then be converted into a list for use by Javascript (and in particular
	 *  Prototype.Array),<p>
	 *
	 *  Used when we want to convert a jsp object (List or Array) into a javascript
	 *  variable.
	 *
	 * @param  o  NOT YET DOCUMENTED
	 * @return    NOT YET DOCUMENTED
	 */
	public static String toCommaDelimited(Object o) {
		String ret = "";
		if (o == null)
			return "";
		try {
			if (o instanceof String)
				return (String) o;
			else if (o instanceof Iterable) {
				Iterator i = ((Iterable) o).iterator();
				while (i.hasNext()) {
					ret = ret + ((String) i.next()).trim();
					if (i.hasNext())
						ret = ret + ",";
				}
				return ret;
			}
			else if (o instanceof String[]) {
				String[] array = (String[]) o;
				for (int i = 0; i < array.length; i++) {
					ret = ret + (String) array[i].trim();
					if (i < array.length - 1)
						ret = ret + ",";
				}
				return ret;
			}
		} catch (Throwable e) {
			prtln("toCommaDelimited error: " + e.getMessage());
			return ret;
		}
		return o.toString();
	}


	/**
	 *  Gets the dcsField attribute of the JspFunctions class
	 *
	 * @param  fieldName  Description of the Parameter
	 * @param  docReader  Description of the Parameter
	 * @return            The dcsField value
	 */
	public static String getDcsField(String fieldName, DocReader docReader) {
		Document doc = docReader.getDocument();
		return doc.get(DcsDataFileIndexingPlugin.FIELD_NS + fieldName);
	}


	/**
	 *  Removes jsp-encoded xpath indexing from a string<p>
	 *
	 *  e.g., /itemRecord/foo_1_/farb_4_ becomes /itemRecord/foo/farb
	 *
	 * @param  s  Description of the Parameter
	 * @return    Description of the Return Value
	 */
	public static String decodePath(String s) {
		// prtln ("decodePath with " + s);
		Pattern p = Pattern.compile("_[0-9]+?_");
		Matcher m;
		// replace occurrences one by one
		while (true) {
			m = p.matcher(s);
			if (m.find()) {
				String index = s.substring(m.start() + 1, m.end() - 1);
				String replaceStr = "";
				s = p.matcher(s).replaceFirst(replaceStr);
			}
			else {
				break;
			}
		}
		// prtln ("  ... returning " + s);
		return s;
	}


	/**
	 *  Gets the localizedSchemaXML attribute of the JspFunctions class
	 *
	 * @param  url  NOT YET DOCUMENTED
	 * @return      The localizedSchemaXML value
	 */
	public static String getLocalizedSchemaXML(String url) {
		org.dom4j.Document doc = null;
		try {
			doc = Dom4jUtils.getXmlDocument(new URL(url));
		} catch (Throwable e) {
			return "getLocalizedDoc failed with " + url;
		}
		return Dom4jUtils.localizeXml(doc.asXML());
	}


	/**
	 *  Gets the prompts defined in the fieldFile for the current xpath and framework.
	 *
	 * @param  xpath      xpath for prompt
	 * @param  framework  the framework
	 * @return            a list of prompt strings, or null
	 */
	public static List getPrompts(String xpath, MetaDataFramework framework) {
		String normalizedPath = XPathUtils.normalizeXPath(XPathUtils.decodeXPath(xpath));
		FieldInfoReader fieldInfo = framework.getFieldInfoMap().getFieldInfo(normalizedPath);
		if (fieldInfo != null) {
			return fieldInfo.getPrompts();
		}
		else {
			return null;
		}
	}

	public static AsnStandardsNode getAsnStandard (String asnId, CATServiceHelper helper) {
		prtln  ("getAsnStandard");
		try {
			return (AsnStandardsNode)helper.getStandardsDocument().getStandard(asnId);
		} catch (Throwable t) {
			prtln ("sf:getAsnStandard: " + t.getMessage());
		}
		return null;
	}

	/**
	 *  The main program for the JspFunctions class
	 *
	 * @param  args  The command line arguments
	 */
	public static void main(String[] args) {
		debug = true;
		String path = "/record/educational/standard_ASN_ID";
		if (args.length > 0)
			path = args[0];
		prtln("in: " + path);
		prtln("out: " + decodePath(path));
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  s  NOT YET DOCUMENTED
	 */
	public static void prtln(String s) {
		if (debug)
			SchemEditUtils.prtln(s, "JspFunctions");
	}
}

