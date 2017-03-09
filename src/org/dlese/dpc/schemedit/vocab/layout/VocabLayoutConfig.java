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
package org.dlese.dpc.schemedit.vocab.layout;

import java.io.*;
import java.util.*;
import java.text.*;
import java.net.*;

import org.dom4j.*;

import org.dlese.dpc.schemedit.SchemEditUtils;
import org.dlese.dpc.xml.Dom4jUtils;

/**
 *  Populates the a groupInfoMap by reading from a file specified in Framework config file (with name ending
 *  in "fields-list.xml"). The fields-list file contains a list of relative URIs that can be used to calculate
 *  the absolute URI to the individual fields files.<p>
 *
 *  E.g.,
 *  <ul>
 *    <li> the fields-list for dlese_anno format is http://www.dlese.org/Metadata/annotation/1.0.00/build/fields-list.xml.
 *
 *    <li> within the fields-list file a relative URI is: annotation/1.0.00/fields/annotation-anno-fields-en-us.xml
 *
 *    <li> the absolute uri to this field file is: http://www.dlese.org/Metadata/annotation/1.0.00/fields/annotation-anno-fields-en-us.xml.
 *    The resolution of absolute Uris is done in {@link #getFieldsFileUri(URI, String)}.
 *  </ul>
 *
 *
 * @author     ostwald <p>
 *
 */
public class VocabLayoutConfig  {
	private static boolean debug = true;
	protected HashMap map = null;
	protected String configPath = null;
	private Document doc = null;

	/**  Constructor for the VocabLayoutConfig object */
	public VocabLayoutConfig() {
		map = new HashMap();
	}
	
	public VocabLayoutConfig(String configIndexPath) {
		this();
		this.configPath = configIndexPath;
	}

	/**
	 *  Constructor for the VocabLayoutConfig object
	 *
	 * @param  uri  NOT YET DOCUMENTED
	 */
	public VocabLayoutConfig(File configIndexFile) {
		this();
		this.configPath = configIndexFile.toURI().toString();
	}

		/**
	 *  Add a VocabLayout to the map.
	 *
	 *@param  xpath   Description of the Parameter
	 *@param  reader  Description of the Parameter
	 */
	public void putVocabLayout(String xpath, VocabLayout reader) {
		map.put(xpath, reader);
	}

	public VocabLayout getVocabLayout(String xpath) {
		return (VocabLayout) map.get(xpath);
	}
	
	public void removeVocabLayout(String xpath) {
		map.remove(xpath);
	}


	/**
	 *  Gets the keySet attribute of the VocabLayoutMap object
	 *
	 *@return    The keySet value
	 */
	public Set getKeySet() {
		if (map == null) {
			map = new HashMap();
		}
		return map.keySet();
	}


	/**
	 *  Reload all the VocabLayouts in the map
	 *
	 *@exception  Exception  Description of the Exception
	 */
	public void reload()
		throws Exception {
		init();
	}


	/**
	 *  Gets all the VocabLayouts as a list.
	 *
	 *@return    a List of VocabLayouts
	 */
	public List getAllVocabLayout() {
		ArrayList list = new ArrayList();
		list.addAll(map.values());
		return list;
	}


	/**
	 *  Gets all the fields (xpaths) having VocabLayouts
	 *
	 *@return    a list of xpaths to Fields
	 */
	public List getFields() {
		ArrayList keys = new ArrayList();
		for (Iterator i = getKeySet().iterator(); i.hasNext(); ) {
			keys.add((String) i.next());
		}
		return keys;
	}


	/**
	 *  Returns true if there is a VocabLayout present for the specified xpath.
	 *
	 *@param  xpath  Description of the Parameter
	 *@return        Description of the Return Value
	 */
	public boolean hasVocabLayout(String xpath) {
		return getFields().contains(xpath);
	}


	/**
	 *  Description of the Method
	 *
	 *@return    Description of the Return Value
	 */
	public String toString() {
		String ret = "";
		for (Iterator i = map.keySet().iterator(); i.hasNext(); ) {
			String key = (String) i.next();
			ret += key + "\n";
		}
		return ret;
	}


	
	/**
	 *  Read a listing of URIs Fields files from directoryUri and then loads each of the listed files as
	 *  VocabLayout objects, which are stored in a map.
	 *
	 * @exception  Exception  Description of the Exception
	 */
	public void init()
		 throws Exception {
		map.clear();
		
		String uriStr = configPath.replaceAll(" ", "+");
		uriStr = new URI (uriStr).toString();
		
		Object ref = SchemEditUtils.getUriRef(uriStr);
		if (ref instanceof File)
			doc = Dom4jUtils.getXmlDocument((File) ref);
		else if (ref instanceof URL)
			doc = Dom4jUtils.getXmlDocument((URL) ref);
		else
			throw new Exception ("Could not resolve path (" + configPath + ")");
		
		prtln ("reading " + configPath);
		Document groupsFileListing = Dom4jUtils.localizeXml(doc);

		// print xml file for debugging
		// prtln("FieldsDirectory at : " + fileListUri.toString());
		// prtln (Dom4jUtils.prettyPrint(groupsFileListing));


		Node filesNode = groupsFileListing.selectSingleNode("metadataGroupsInfo/files");
		if (filesNode == null) {
			prtln("no filesNode found");
			return;
		}

		Element filesElement = (Element) filesNode;
		// String baseUri = filesElement.attributeValue("uri");

		// load each of the files in the groups file listing
		for (Iterator i = filesElement.elementIterator(); i.hasNext(); ) {
			Node fileNode = (Node) i.next();
			URI uri = null;
			try {
				String filePath = fileNode.getText().trim();
				uri = new URI (filePath);
				if (!uri.isAbsolute())
					uri = getFieldsFileUri(filePath);
			} catch (Exception e) {
				prtln ("could not compute uri to groups file: " + e.getMessage());
				continue;
			}
			
			// FILTER - should be applied ONLY to MUI group lists!
			if (uri.toString().indexOf("-cataloger-") == -1) {
				// prtln ("URI: " + uri.toString());
				continue;
			}
			try {
				VocabLayout reader = new VocabLayout(uri.toString());
				putVocabLayout(reader.getPath(), reader);
				// prtln("path: " + reader.getPath());
			} catch (Exception e) {
				prtln(e.getMessage());
			}
		}
		prtln("VocabLayoutMap initialized - " + map.size() + " groups files read");
	}


	/**
	 *  Gets an absolute URI be resolving a baseUri (pointing to the groups-list file) and a relativePath.<p>
	 *
	 *  The first two parts of the relative path form a format, version key. E.g. for relativePath
	 *  "annotation/1.0.00/fields/annotation-anno-fields-en-us.xml" the key is "annotation", "1.0.00". To form an
	 *  absolute fieldsFileUri, the part of the baseUri above the key are joined with the relativePath.<p>
	 *
	 *  Exceptions are thrown if the relativePath does not contain a key, or if the baseUri does not contain the
	 *  key specified by the relativePath.
	 *
	 * @param  baseUri        baseUri pointing to a fields-list file
	 * @param  relativePath   a relative path containing a framework, version key and the name of the fields
	 *      file.
	 * @return                an absolute groupsFileUri
	 * @exception  Exception  if absoluteFieldsFileUri cannot be computed
	 */
	public URI getFieldsFileUri(String relativePath) throws Exception {

		
		// relativePath must not begin with '/'
		if (relativePath.charAt(0) == '/')
			throw new Exception("relativePath (\"" + relativePath + "\") is not relative");

		// the first two levels of the relativePath are the key - they specify frameworkName and version
		String[] relativeSplits = relativePath.split("/");
		if (relativeSplits.length < 3)
			throw new Exception("illegal relative path (\"" + relativePath + "\") - at least 3 segements are required: ");
		String frameworkName = relativeSplits[0];
		String version = relativeSplits[1];
		String key = frameworkName + "/" + version;

		// now get rid of the tail segments of the baseUri from the key down
		int x = configPath.indexOf(key);
		if (x == -1)
			throw new Exception("framework/version key (\"" + key + "\") not found in baseUri (\"" + configPath + "\")");

		String newPath = configPath.substring(0, x) + relativePath;

		// this contructor works with fileURIs as well as with urlURIs
		return new URI (newPath);
	}


	/**
	 *  Read a set of groups files
	 *
	 * @param  args           The command line arguments
	 * @exception  Exception  Description of the Exception
	 */
	public static void main(String[] args)
		 throws Exception {
		setDebug(true);
		org.dlese.dpc.schemedit.test.TesterUtils.setSystemProps();
		prtln("VocabLayoutConfig");
		// String path = "/Users/ostwald/devel/projects/metadata-ui-project/frameworks/adn-item/0.6.50/build/groups-list.xml";
		// String path = "http://localhost/vocabLayoutTest/build/groups-list.xml";
		
		String path = "http://www.dls.ucar.edu/people/ostwald/etc/adn-item/0.6.50/build/my-groups-list.xml";
		VocabLayoutConfig vlc = new VocabLayoutConfig (path);
		
		// testing windows FILE path
		// String path = "D:/Documents and Settings/ostwald/devel/projects/metadata-ui-project/frameworks/adn-item/0.6.50/build/my-groups-list.xml";
		// VocabLayoutConfig vlc = new VocabLayoutConfig (new File(path));
		vlc.init();
		for (Iterator i=vlc.getAllVocabLayout().iterator();i.hasNext();) {
			VocabLayout vocabLayout = (VocabLayout)i.next();
			prtln ("\t" + vocabLayout.getPath());
			// vocabLayout.report();
		}
	}

	public static void setDebug(boolean d) {
		debug = d;
	}

	/**
	 *  Output a line of text to standard out, with datestamp, if debug is set to true.
	 *
	 * @param  s  The String that will be output.
	 */
	protected static void prtln(String s) {
		if (debug) {
			SchemEditUtils.prtln (s, "VocabLayoutConfig");
		}
	}
}

