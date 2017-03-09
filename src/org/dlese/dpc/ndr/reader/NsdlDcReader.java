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
package org.dlese.dpc.ndr.reader;

import org.dlese.dpc.ndr.NdrUtils;
import org.dlese.dpc.xml.Dom4jUtils;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.XPath;
import java.net.URL;
import java.util.*;

/**
 * Reads NsdlDc data stream as a dom4j.Document and provides access to components.
 *
 *@author     ostwald
 */
public class NsdlDcReader {

	private static boolean debug = true;


	private Document doc = null;
	Map namespaces = null;

	/**
	 *  Constructor for the NsdlDcReader object
	 *
	 *@param  response       NOT YET DOCUMENTED
	 *@exception  Exception  NOT YET DOCUMENTED
	 */
	public NsdlDcReader(String xml) throws Exception {
		this.doc = DocumentHelper.parseText(xml);
	}

	public NsdlDcReader(URL url) throws Exception {
		// this.doc = Dom4jUtils.getXmlDocument(url);
		this.doc = NdrUtils.getNDRObjectDoc(url);
	}

	public Map getNSMap () {
		if (namespaces == null) {
			namespaces = new HashMap ();
			namespaces.put ("dc", "http://purl.org/dc/elements/1.1/");
			namespaces.put ("nsdl_dc", "http://ns.nsdl.org/nsdl_dc_v1.02/");
		}
		return namespaces;
	}
	
	private String getValue (String xpath) {
		XPath xpathObj = DocumentHelper.createXPath(xpath);
		xpathObj.setNamespaceURIs(getNSMap());
		Node node = xpathObj.selectSingleNode(doc);
		if (node != null)
			return node.getText();
		else
			return null;
	}
	
	public String getTitle () {
		return getValue ("/nsdl_dc:nsdl_dc/dc:title");
	}
	
	public String getDescription () {
		return getValue ("/nsdl_dc:nsdl_dc/dc:description");
	}
	
	public String getIdentifier () {
		return getValue ("/nsdl_dc:nsdl_dc/dc:identifier");
	}
	
	public static void main (String [] args) throws Exception {
		URL url = new URL ("http://ndr.nsdl.org/api/get/2200/20061002124900276T/format_nsdl_dc");
		NsdlDcReader reader = new NsdlDcReader (url);
		
		pp (reader.doc);
		
		prtln ("title: " + reader.getTitle());
		prtln ("description: " + reader.getDescription());
		prtln ("identifier: " + reader.getIdentifier());
	}
	
	/**
	 *  Prints a dom4j.Node as formatted string.
	 *
	 *@param  node  NOT YET DOCUMENTED
	 */
	protected static void pp(Node node) {
		prtln(Dom4jUtils.prettyPrint(node));
	}


	/**
	 *  Description of the Method
	 *
	 *@param  s  Description of the Parameter
	 */
	private static void prtln(String s) {
		if (debug) {
			System.out.println("NsdlDcReader: " + s);
		}
	}

}

