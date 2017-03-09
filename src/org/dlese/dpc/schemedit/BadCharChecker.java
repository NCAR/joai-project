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

import java.util.*;
import java.util.regex.*;
import java.net.*;
import java.io.*;
import org.dlese.dpc.xml.*;
import org.dlese.dpc.xml.schema.*;
import org.dlese.dpc.util.Files;
import org.dom4j.*;


/**
 *  Checks XML documents for presence of bad characters and builds a list of BadCharEntries containing
 the element xpath and value containing the bad char.
 *
 *@author     ostwald<p>
 $Id $
 */
public class BadCharChecker {

	private static boolean debug = true;

	private List badCharList = null;
	
	public List check (String recordPath, MetaDataFramework framework) throws Exception {
		StringBuffer buf = Files.readFile (recordPath);
		badCharList = new ArrayList ();
		prtln ("File has " + buf.length() + " characters");
		if (SchemEditUtils.hasBadChar(buf.toString())) {
			prtln ("bad chars found  ... creating map");
			String rootElementName = framework.getRootElementName();
			Document rawDoc = Dom4jUtils.getXmlDocument(new File (recordPath));
			Document doc = Dom4jUtils.localizeXml(rawDoc, rootElementName);
			
			check (doc);
		}
		return badCharList;
	}
	
	/**
	* Check localized xmlRecord in string form for bad characters
	*/
	public List check (String xmlRecord) throws Exception {
		Document doc = Dom4jUtils.getXmlDocument(xmlRecord);
		return check (doc);
	}
	
	/**
	* First checks for the presense of a "badChar" in the document as a string, and if one is found
	* creates a list of BadCharEntry
	*/
	public List check (Document doc) throws Exception {
		badCharList = new ArrayList ();
		if (SchemEditUtils.hasBadChar(doc.asXML())) {
			Element root = doc.getRootElement();
			getBadChars (root, root.getPath());
		}
		return badCharList;
	}
	
	private void getBadChars (Element e, String xpath) {
		List attributes = e.attributes();
		for (int i = 0; i<attributes.size(); i++) {
			Attribute attr = (Attribute) attributes.get(i);
			String val = attr.getValue();
			if (val != null && SchemEditUtils.hasBadChar (val)) {
				String path = xpath + "/@" + attr.getName();
				badCharList.add (new BadCharEntry (path, val));
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
		
		for (int i = 0;i < children.size(); i++) {
			Element child = (Element) children.get(i);
			String path = xpath + "/" + child.getName();
			if (useIndex)
				path += "[" + (i+1) + "]";
			String val = child.getText();
			if (val != null && SchemEditUtils.hasBadChar (val))
				badCharList.add (new BadCharEntry (path, val));
			getBadChars(child, path);
		}
	}
	
	public BadCharEntry getBadCharEntry (String xpath, String value) {
		return new BadCharEntry (xpath, value);
	}
				
	public class BadCharEntry implements Serializable {
		public String xpath;
		public String value;
		
		public BadCharEntry (String xpath, String value) {
			this.xpath = xpath;
			this.value = value;
		}
	}
	
	/**
	 *  Description of the Method
	 *
	 *@param  s  Description of the Parameter
	 */
	private static void prtln(String s) {
		if (debug) {
			System.out.println("BadCharChecker: " + s);
		}
	}	
}

