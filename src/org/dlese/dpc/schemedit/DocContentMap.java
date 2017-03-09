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

import java.util.regex.*;
import java.net.*;
import java.io.*;
import java.util.*;
import org.dlese.dpc.xml.*;
import org.dlese.dpc.xml.schema.*;
import org.dlese.dpc.util.Files;
import org.dlese.dpc.util.strings.FindAndReplace;
import org.dom4j.*;
import org.dom4j.util.NodeComparator;

/**
 *  Utilities for comparing metadata records for content-based equality (rather
 *  than structural equality).<p>
 *
 *  Note: why isn't {@link org.dom4j.util.NodeComparator} used to compare? see
 *  <a href="http://www.dom4j.org/faq.html#compare-nodes">more info on
 *  NodeComparator</a>
 *
 * @author    ostwald<p>
 *
 *
 */
public class DocContentMap {

	private static boolean debug = true;
	MetaDataFramework framework = null;
	Document doc = null;


	/**
	 *  Constructor for the DocContentMap object, used for Debugging.
	 *
	 * @param  xmlFormat      Description of the Parameter
	 * @param  configFileDir  Description of the Parameter
	 */
	DocContentMap(String xmlFormat, File configFileDir) {
		File configFile = new File(configFileDir, xmlFormat + ".xml");
		String configFilePath = configFile.toString();

		// make sure the prop file really exists
		if (!configFile.exists()) {
			prtln("propfile doesn't exist at " + configFilePath);
			return;
		}
		else {
			framework = new MetaDataFramework(configFilePath, "");
			try {
				framework.loadSchemaHelper();
			} catch (Exception e) {
				prtln("schemaHelper load error: " + e.getMessage());
			}
		}
	}


	/**
	 *  Gets the document attribute of the DocContentMap object
	 *
	 * @return    The document value
	 */
	public Document getDocument() {
		return doc;
	}


	/**
	 *  Returns a representation of a metadata record that can be compared with
	 *  another record to determine if there are differences in content.<p>
	 *
	 *  The DocContentMap has an entry for each normalized (i.e., containing no
	 *  indexing info) xpath having a value. The entries are a sorted list of the
	 *  values for that xpath. As a map with sorted entries, this representation
	 *  allows records to be compared using the Map "equals" method.<p>
	 *
	 *  DocContentMaps are used to determine if a metadata has been edited in the
	 *  metadata editor so the user can be warned of unsaved changes before
	 *  exiting.
	 *
	 * @param  doc        a {@link org.dom4j.Document} representing a metadata
	 *      record
	 * @param  framework  the MetaDataFramework for the metadata record
	 * @return            The docContentMap value
	 */
	public static Map getDocContentMap(Document doc, MetaDataFramework framework) {
		doc.normalize();
		Element root = doc.getRootElement();
		Map map = new TreeMap();

		// string representation of "cr" that we will remove from all values, since it wreaks
		// havoc with the compare
		String cr = Character.toString((char) 13);

		// schemaNodeMap contains all legal paths.
		// for each path create a sorted list of values in the instanceDoc
		SchemaNodeMap schemaNodeMap = framework.getSchemaHelper().getSchemaNodeMap();
		for (Iterator i = schemaNodeMap.getKeys().iterator(); i.hasNext(); ) {
			String path = (String) i.next();
			// prtln (path);
			List nodes = doc.selectNodes(path);
			if (nodes == null) {
				continue;
			}

			List values = new ArrayList();
			for (Iterator j = nodes.iterator(); j.hasNext(); ) {
				Node node = (Node) j.next();
				String val = node.getText();
				if (val != null && val.trim().length() > 0) {
					val = FindAndReplace.replace(val, cr, "", false);
					values.add(val.trim());
				}
			}
			if (values.size() > 0) {
				Collections.sort(values);
				map.put(path, values);
			}
		}
		return map;
	}


	/**
	 *  Returns a content map of the provided file, assuming it belongs to the
	 *  MetaDataFramework of this DocContentMap instance.
	 *
	 * @param  file  Description of the Parameter
	 * @return       The docContentMap value
	 */
	public Map getDocContentMap(File file) {
		String rootElementName = framework.getRootElementName();

		try {
			doc = Dom4jUtils.getXmlDocument(file);
			doc = Dom4jUtils.localizeXml(doc, rootElementName);
		} catch (Exception e) {
			prtln("getXmlDocument error: " + e.getMessage());
		}
		return getDocContentMap(doc, framework);
	}


	/**
	 *  Debugging method to compare xml documents contained in two files.
	 *
	 * @exception  Exception  Description of the Exception
	 */
	private static void compareDebug()
		 throws Exception {
		String xmlFormat = "news_opps";
		File configFileDir = new File("/devel/ostwald/projects/schemedit-project/web/WEB-INF/framework-config");
		DocContentMap dcm = new DocContentMap(xmlFormat, configFileDir);

		String compareDir = "/devel/ostwald/SchemEdit/testers/domCompare";
		Map map1 = dcm.getDocContentMap(new File(compareDir, "doc1.xml"));
		Map map2 = dcm.getDocContentMap(new File(compareDir, "doc2.xml"));
		if (map1.equals(map2)) {
			prtln("EQUAL");
		}
		else {
			prtln("DIFFERENT");
		}
		cmpMaps(map1, map2);
	}


	/**
	 *  The main program for the DocContentMap class
	 *
	 * @param  args           The command line arguments
	 * @exception  Exception  Description of the Exception
	 */
	public static void main(String[] args)
		 throws Exception {

		compareDebug();

	}


	/**
	 *  Utility method to print the contents of a contentMap
	 *
	 * @param  map  Description of the Parameter
	 */
	public static void prtmap(Map map) {
		for (Iterator i = map.keySet().iterator(); i.hasNext(); ) {
			String key = (String) i.next();
			Object val = (Object) map.get(key);
			prtln(key);
			if (val instanceof List) {
				List vals = (List) val;
				for (Iterator j = vals.iterator(); j.hasNext(); ) {
					prtln("\t" + (String) j.next());
				}
			}
			else {
				prtln("\t" + (String) val);
			}
		}
	}


	/**
	 *  Compares two docContent Maps - used for debugging and testing.
	 *
	 * @param  map1  Description of the Parameter
	 * @param  map2  Description of the Parameter
	 */
	public static void cmpMaps(Map map1, Map map2) {
		boolean diff = false;
		prtln ("\nDocContentMap cmpMaps");
		for (Iterator i = map1.keySet().iterator(); i.hasNext(); ) {
			String key = (String) i.next();
			List vals1 = (List) map1.get(key);
			List vals2 = (List) map2.get(key);
			if (!vals1.equals(vals2)) {
				prtln("vals not equal for *" + key + "*");
				diff = true;
				// cmpVals(vals1, vals2);
				cmpValLists(vals1, vals2);
			}
		}
		if (diff) {
			prtln("maps are different");
		}
		else {
			prtln("maps are EQUAL");
		}
	}


	/**
	 *  Utility method to generate a printable String from a given char (useful
	 *  only for ASCII).
	 *
	 * @param  c  Description of the Parameter
	 * @return    The printable value
	 */
	private static String getPrintable(char c) {
		int i = (int) c;

		if (i == 10) {
			return "nl";
		}
		if (i == 13) {
			return "cr";
		}
		if (i < 31) {
			return "...";
		}
		return Character.toString(c);
	}


	/**
	 *  Debugging method to compare two value-lists.
	 *
	 * @param  list1  Description of the Parameter
	 * @param  list2  Description of the Parameter
	 */
	public static void cmpVals(List list1, List list2) {
		prtln("list1 has " + list1.size() + " members");
		prtln("list2 has " + list2.size() + " members");
		int d = 0;
		for (int i = 0; i < list1.size(); i++) {
			String val1 = (String) list1.get(i);
			String val2 = (String) list2.get(i);
			for (int j = 0; j < val1.length() && d < 20; j++) {
				char c1 = val1.charAt(j);
				char c2 = val2.charAt(j);

				String s1 = getPrintable(c1);
				String s2 = getPrintable(c2);

				if (c1 != c2) {
					prtln(j + "\t 1: " + (int) c1 + "(" + s1 + ")\t 2: " + (int) c2 + "(" + s2 + ")");
					// prtln (j + "\t 1: " + (int)c1  +"\t 2: " + (int)c2 );
					d++;
				}
			}
		}
	}

	/**
	* compare the values of a doc field for the cashed and active docs
	*/
	public static void cmpValLists(List list1, List list2) {
		prtln("list1 has " + list1.size() + " members");
		prtln("list2 has " + list2.size() + " members");
	
		int max = Math.max(list1.size(),  list2.size());
		for (int i = 0; i < max; i++) {
			String val1 = i < list1.size() ? (String)list1.get(i) : "--------";
			String val2 = i < list2.size() ? (String)list2.get(i) : "--------";
			prtln (val1 + "\t" + val2);
		}
	}

	/**
	 *  Description of the Method
	 *
	 * @param  node  Description of the Parameter
	 */
	private static void pp(Node node) {
		prtln(Dom4jUtils.prettyPrint(node));
	}


	/**
	 *  Description of the Method
	 *
	 * @param  s  Description of the Parameter
	 */
	private static void prtln(String s) {
		if (debug) {
			//System.out.println("DocContentMap: " + s);
			System.out.println(s);
		}
	}
}

