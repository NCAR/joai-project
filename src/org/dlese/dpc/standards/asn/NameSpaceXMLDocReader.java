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
package org.dlese.dpc.standards.asn;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.dlese.dpc.xml.Dom4jUtils;
import org.dlese.dpc.xml.schema.NamespaceRegistry;
import org.dlese.dpc.util.TimedURLConnection;
import org.dlese.dpc.util.URLConnectionTimedOutException;

import org.dom4j.*;
import org.jaxen.SimpleNamespaceContext;

import java.util.*;
import java.net.URL;
import java.io.File;

/**
 *  Class to read the ASN topics docuement and provide lookups by topic purl.
 *
 * @author    Jonathan Ostwald
 */
public class NameSpaceXMLDocReader {
	private static boolean debug = true;

	private Document doc = null;
	private SimpleNamespaceContext nsContext = null;
	private NamespaceRegistry namespaces = null;

	public NameSpaceXMLDocReader (String xml) throws Exception {
		this.doc = DocumentHelper.parseText(xml);
		init();
	}
	
	public NameSpaceXMLDocReader (Document doc) throws Exception {
		this.doc = doc;
		init();
	}

	/**
	 *  Constructor for the NameSpaceXMLDocReader object
	 *
	 * @param  url            NOT YET DOCUMENTED
	 * @param  nsContext      NOT YET DOCUMENTED
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public NameSpaceXMLDocReader(URL url) throws Exception {
		this.doc = readDocument(url);
		init();
	}

		/**
	 *  Constructor for the NameSpaceXMLDocReader object
	 *
	 * @param  url            NOT YET DOCUMENTED
	 * @param  nsContext      NOT YET DOCUMENTED
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public NameSpaceXMLDocReader(File file) throws Exception {
		this.doc = Dom4jUtils.getXmlDocument(file);
		init();
	}
	
	
	protected void init() throws Exception {
		this.namespaces = new NamespaceRegistry();
		this.namespaces.registerNamespaces(this.doc);		
		this.nsContext = namespaces.getNamespaceContext();
	}
	
	public QName getQName (String qualifiedName) {
		try {
			return namespaces.getQName(qualifiedName);
		} catch (Exception e) {
			prtln ("WARNING: could not obtain QName for \"" + qualifiedName + ": " + e.getMessage());
		}
		return null;
	}
	
	public Namespace getNamespace (String prefix) {
		return this.namespaces.getNSforPrefix(prefix);
	}

	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  url            NOT YET DOCUMENTED
	 * @return                NOT YET DOCUMENTED
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	private Document readDocument(URL url) throws Exception {

		// timed connection
		int millis = 10000;
		String content = null;
		Document doc = null;
		try {
			content = TimedURLConnection.importURL(url.toString(), "utf-8", millis);
			doc = Dom4jUtils.getXmlDocument(content);
		} catch (URLConnectionTimedOutException e) {
			prtln("connection timed out: " + e);
			throw new Exception("Connection to " + url + " timed out after " + (millis / 1000) + " seconds");
		} catch (DocumentException e) {
			throw new Exception("Could not process Document: " + e.getMessage());
		}
		if (doc == null)
			throw new Exception("Document was not processed");
		return doc;
	}

	public Document getDocument () {
		return this.doc;
	}
	
	public Element getRootElement () {
		return this.doc.getRootElement();
	}
	
	public String getNodeText (String xpath) {
		Node node = getNode (xpath);
		if (node == null)
			return null;
		else
			return node.getText();
	}

	public String getValueAtPath(Element baseElement, String relativePath) throws Exception {
		Node node = getNode (baseElement, relativePath);
		if (node == null) {
			throw new Exception("node not found at relative path: " + relativePath);
		}
		String ret = node.getText();
		if (ret == null) {
			prtln("WARNING: text not found for node at relative path: " + relativePath);
			return ret;
		}
		return ret.trim();
	}
	
	/**
	 *  Gets the node attribute of the NameSpaceXMLDocReader object
	 *
	 * @param  xpath  NOT YET DOCUMENTED
	 * @return        The node value
	 */
	public Node getNode(String xpath) {
		return getNode (null, xpath);
	}
	
	public Node getNode (Node baseNode, String xpath) {
		List list = getNodes(baseNode, xpath);
		if ((list == null) || (list.size() == 0)) {
			// prtln ("getNode() did not find node for " + xpath);
			return null;
		}
		if (list.size() > 1) {
			prtln("getNode() found mulitple modes for " + xpath + " (returning first)");
		}
		return (Node) list.get(0);
	}


	/**
	 *  Gets the nodes attribute of the NameSpaceXMLDocReader object
	 *
	 * @param  path  NOT YET DOCUMENTED
	 * @return       The nodes value
	 */
	public List getNodes(String path) {
		return getNodes (null, path);
	}

	public List getNodes(Node baseNode, String path) {
		if (baseNode == null)
			baseNode = this.doc;
		try {
			XPath xpath = getXPath(path);
			return xpath.selectNodes(baseNode);
		} catch (Throwable e) {
			// prtln("getNodes() failed with " + path + ": " + e);
		}
		return null;
	}
	
	
	/**
	 *  Gets the xPath attribute of the NameSpaceXMLDocReader object
	 *
	 * @param  path  NOT YET DOCUMENTED
	 * @return       The xPath value
	 */
	public XPath getXPath(String path) {
		XPath xpath = DocumentHelper.createXPath(path);
		xpath.setNamespaceContext(this.nsContext);
		// prtln ("xpath: " + xpath.toString());
		return xpath;
	}

	public String getChildElementText(Element e, String childElementName) {
		Element child = e.element(getQName (childElementName));
		if (child == null) {
			prtln("getChildElementText could not find subElement for " + childElementName);
			// prtln(Dom4jUtils.prettyPrint(e));
			return "";
		}
		return child.getTextTrim();
	}
	
	public String getChildElementAttribute(Element e, String childElementName, String attributeName) {
		Element child = e.element(getQName (childElementName));
		if (child == null) {
			prtln("getChildElementText could not find childElement for " + childElementName);
			return "";
		}
		return child.attributeValue(getQName (attributeName), null);
	}
	
	public void destroy () {
		this.doc = null;
		this.nsContext = null;
		this.namespaces = null;
	}

	private static void prtln(String s) {
		if (debug) {
			System.out.println("NameSpaceXMLDocReader: " + s);
		}
	}
	
	private static void pp(Node node) {
		prtln(Dom4jUtils.prettyPrint(node));
	}
	
	public static void main (String [] args) throws Exception {
		NameSpaceXMLDocReader xsdReader = null;
		String loc = "http://www.dls.ucar.edu/people/ostwald/Metadata/sif/DataModelAnnotated/InstructionalServices/Activity.xsd";
		prtln ("LOC: " + loc);

		try {
			URL url = new URL (loc);
			// xsdDoc = Dom4jUtils.getXmlDocument(url);
			xsdReader = new NameSpaceXMLDocReader (url);
		} catch (Exception e) {
			prtln ("couldn't get typeDef document: " + e.getMessage());
			return;
		}
		
/* 		String attName = "LearningResourceRefId";
		String xpath = "//xs:element[@name=\'" + attName + "\']/xs:annotation/xs:documentation"; */

		String attName = "EvaluationType";
		String xpath = "//xs:attribute[@name=\'" + attName + "\']/xs:annotation/xs:documentation";		
		
		// String xpath = "/xs:schema/xs:complexType";
		prtln ("\nxpath: " + xpath);
		Element globalElement = (Element) xsdReader.getNode (xpath);
		
		prtln ("GLOBAL ELEMENT for " + attName);
		if (globalElement == null)
			prtln ("\t NOT FOUND");
		else
			prtln (globalElement.getText());

	}
}

