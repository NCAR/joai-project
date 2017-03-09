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
 *  Reads ServiceDescription data stream as a dom4j.Document and provides access
 *  to components.
 *
 * @author    ostwald
 */
public class ServiceDescriptionReader {

	private static boolean debug = true;

	private Document doc = null;
	Map namespaces = null;


	/**
	 *  Constructor for the ServiceDescriptionReader object
	 *
	 * @param  xml            NOT YET DOCUMENTED
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public ServiceDescriptionReader(String xml) throws Exception {
		this.doc = DocumentHelper.parseText(xml);
	}


	/**
	 *  Constructor for the ServiceDescriptionReader object
	 *
	 * @param  url            NOT YET DOCUMENTED
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public ServiceDescriptionReader(URL url) throws Exception {
		// this.doc = Dom4jUtils.getXmlDocument(url);
		this.doc = NdrUtils.getNDRObjectDoc(url);
	}


	/**
	 *  Gets the document attribute of the ServiceDescriptionReader object
	 *
	 * @return    The document value
	 */
	public Document getDocument() {
		return this.doc;
	}


	/**
	 *  Gets the nSMap attribute of the ServiceDescriptionReader object
	 *
	 * @return    The nSMap value
	 */
	public Map getNSMap() {
		if (namespaces == null) {
			namespaces = new HashMap();
			namespaces.put("dc", "http://purl.org/dc/elements/1.1/");
		}
		return namespaces;
	}


	private String getValue(String xpath) {
		XPath xpathObj = DocumentHelper.createXPath(xpath);
		xpathObj.setNamespaceURIs(getNSMap());
		Node node = xpathObj.selectSingleNode(doc);
		if (node != null)
			return node.getText();
		else
			return null;
	}


	/**
	 *  Gets the title attribute of the ServiceDescriptionReader object
	 *
	 * @return    The title value
	 */
	public String getTitle() {
		return getValue("/serviceDescription/dc:title");
	}


	/**
	 *  Gets the description attribute of the ServiceDescriptionReader object
	 *
	 * @return    The description value
	 */
	public String getDescription() {
		return getValue("/serviceDescription/dc:description");
	}


	/**
	 *  The main program for the ServiceDescriptionReader class
	 *
	 * @param  args           The command line arguments
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public static void main(String[] args) throws Exception {
		URL url = new URL("http://ndrtest.nsdl.org/api/get/2200/test.20061207181243233T/serviceDescription");
		ServiceDescriptionReader reader = new ServiceDescriptionReader(url);

		pp(reader.doc);

		prtln("title: " + reader.getTitle());
		prtln("description: " + reader.getDescription());
	}


	/**
	 *  Prints a dom4j.Node as formatted string.
	 *
	 * @param  node  NOT YET DOCUMENTED
	 */
	protected static void pp(Node node) {
		prtln(Dom4jUtils.prettyPrint(node));
	}


	/**
	 *  Description of the Method
	 *
	 * @param  s  Description of the Parameter
	 */
	private static void prtln(String s) {
		if (debug) {
			System.out.println("ServiceDescriptionReader: " + s);
		}
	}

}

