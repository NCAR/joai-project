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
package org.dlese.dpc.schemedit.test;

import org.dlese.dpc.schemedit.*;
import java.util.*;
import java.util.regex.*;
import java.net.*;
import java.io.*;
import org.dlese.dpc.xml.*;
import org.dlese.dpc.xml.schema.*;
import org.dlese.dpc.util.Files;
import org.dom4j.*;


/**
 *  Class for testing dom manipulation with help from {@link org.dlese.dpc.xml.schema.SchemaHelper}
 *
 *@author     ostwald<p>
 $Id $
 */
public class BadCharTester {

	private static boolean debug = true;
	MetaDataFramework framework = null;
	Map badCharMap = null;
	List badCharList = null;
	
	String configFileDir = "/devel/ostwald/projects/schemedit-project/web/WEB-INF/framework-config";
	
	BadCharTester (String xmlFormat) {
		File configFile = new File(configFileDir, xmlFormat+".xml");
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
				prtln ("schemaHelper load error: " + e.getMessage());
			}
		}
	}

	void nameSpaceTester () throws Exception {
		String rootElementName = framework.getRootElementName();

		String recordPath = "/export/devel/ostwald/records/dlese_anno/1099093288179/TEST-ANNO-000-000-000-001.xml";
		Document rawDoc = Dom4jUtils.getXmlDocument(new File (recordPath));
		Document localDoc = Dom4jUtils.localizeXml(rawDoc, rootElementName);
		prtln (Dom4jUtils.prettyPrint(localDoc));
		prtln ("\n\n");
		Document doc = framework.getWritableRecord(localDoc);
		prtln (Dom4jUtils.prettyPrint(doc));
	}
	
 	Document getLocalizedDoc (String recordPath) throws Exception {
		String rootElementName = framework.getRootElementName();
		Document rawDoc = Dom4jUtils.getXmlDocument(new File (recordPath));
		return Dom4jUtils.localizeXml(rawDoc, rootElementName);
	}
	
	public static void main (String [] args) throws Exception {
		String xmlFormat = "adn";

		BadCharTester dt = new BadCharTester (xmlFormat);
		SchemaHelper sh = dt.framework.getSchemaHelper();
		if (sh == null)
			throw new Exception ("schemahelper not initialized");
		String badCharFilePath = "/devel/ostwald/SchemEdit/testers/badchars/FAKE-000-000-000-004.xml";
		
		Document doc = dt.getLocalizedDoc (badCharFilePath);
		List bcList = null;
		if (doc != null)
			bcList = new BadCharChecker ().check(doc);
			
		if (bcList.size() > 0) {
			for (int i=0;i<bcList.size();i++) {
				BadCharChecker.BadCharEntry entry = (BadCharChecker.BadCharEntry) bcList.get(i);
				prtln(entry.xpath);
					prtln("\t" + entry.value);
			}
		}
		else
			prtln ("No bad chars found");
		
	}

	private static void  pp (Node node) {
		prtln (Dom4jUtils.prettyPrint(node));
	}
	
/* 	public class BadCharEntry {
		String xpath;
		String value;
		
		public BadCharEntry (String xpath, String value) {
			this.xpath = xpath;
			this.value = value;
		}
	} */
	
	/**
	 *  Description of the Method
	 *
	 *@param  s  Description of the Parameter
	 */
	private static void prtln(String s) {
		if (debug) {
			System.out.println("BadCharTester: " + s);
		}
	}
	
	public Map getBadCharMap1 (String recordPath) throws Exception {
		StringBuffer buf = Files.readFile (recordPath);
		badCharMap = new HashMap ();
		prtln ("File has " + buf.length() + " characters");
		if (SchemEditUtils.hasBadChar(buf.toString())) {
			prtln ("bad chars found  ... creating map");
			Document doc = getLocalizedDoc (recordPath);
			getBadChars1 (doc.getRootElement());
		}
		return badCharMap;
	}
		
	public void getBadChars1 (Element e) {
		List attributes = e.attributes();
		for (int i = attributes.size() - 1; i > -1; i--) {
			Attribute attr = (Attribute) attributes.get(i);
			String val = attr.getValue();
			if (val != null && SchemEditUtils.hasBadChar (val))
				badCharMap.put (attr.getPath(), val);
		}

		List children = e.elements();
		for (int i = children.size() - 1; i > -1; i--) {
			Element child = (Element) children.get(i);
			String path = child.getPath();
			String val = child.getText();
			if (val != null && SchemEditUtils.hasBadChar (val))
				badCharMap.put (child.getPath(), val);
			getBadChars1(child);
		}
	}
	
	public Map getBadCharMap2 (String recordPath) throws Exception {
		StringBuffer buf = Files.readFile (recordPath);
		badCharMap = new TreeMap ();
		prtln ("File has " + buf.length() + " characters");
		if (SchemEditUtils.hasBadChar(buf.toString())) {
			prtln ("bad chars found  ... creating map");
			Document doc = getLocalizedDoc (recordPath);
			Element root = doc.getRootElement();
			getBadChars2 (root, root.getPath());
		}
		return badCharMap;
	}
		
	public void getBadChars2 (Element e, String xpath) {
		List attributes = e.attributes();
		for (int i = attributes.size() - 1; i > -1; i--) {
			Attribute attr = (Attribute) attributes.get(i);
			String val = attr.getValue();
			if (val != null && SchemEditUtils.hasBadChar (val)) {
				String path = xpath + "/@" + attr.getName();
				badCharMap.put (path, val);
			}
		}

		List children = e.elements();
		boolean useIndex = false;
		if (children.size() > 1) {
			Element c0 = (Element) children.get(0);
			Element c1 = (Element) children.get(1);
			if (c0.getName().equals(c1.getName())) {
				useIndex = true;
			}
		}
		
		if (useIndex)
			prtln ("Using index for children of " + xpath);
		
		for (int i = 0;i < children.size(); i++) {
			Element child = (Element) children.get(i);
			String path = xpath + "/" + child.getName();
			if (useIndex)
				path += "[" + (i+1) + "]";
			String val = child.getText();
			if (val != null && SchemEditUtils.hasBadChar (val))
				badCharMap.put (path, val);
			getBadChars2(child, path);
		}
	}

	
}

