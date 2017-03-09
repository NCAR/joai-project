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

import org.dlese.dpc.ndr.apiproxy.InfoXML;

import org.dlese.dpc.schemedit.SchemEditUtils;
import org.dlese.dpc.xml.Dom4jUtils;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import java.util.*;

/**
 *  Base Class for reading NDR responses to requests that return handle lists,
 *  such as "List" and "Find" requests. To read responses for requests that
 *  return NDRObjects, use NdrObjectReader.
 *
 * @author    ostwald
 */
public class NdrResponseReader {

	private static boolean debug = false;

	/**  NOT YET DOCUMENTED */
	protected InfoXML infoXML = null;
	/**  NOT YET DOCUMENTED */
	protected Document doc = null;
	/**  NOT YET DOCUMENTED */
	protected List handleList = null;


	/**
	 *  Constructor for the NdrResponseReader object
	 *
	 * @param  infoXML                 NOT YET DOCUMENTED
	 * @exception  Exception           NOT YET DOCUMENTED
	 */
	public NdrResponseReader(InfoXML infoXML) throws Exception {
		this.infoXML = infoXML;
		this.init();
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	private void init() throws Exception {
		this.doc = Dom4jUtils.localizeXml(DocumentHelper.parseText(infoXML.getResponse()));
	}


	/**
	 *  Gets the handle attribute of the NdrResponseReader object
	 *
	 * @return                The handle value
	 * @exception  Exception  NOT YET DOCUMENTED
	 */

	public List getHandleList() throws Exception {
		if (handleList == null) {
			handleList = new ArrayList();
			String xpath = "NSDLDataRepository/resultData/handleList/handle";
			List handleElements = getNodes(xpath);
			if (handleElements == null) {
				prtln("no handles found");
				return handleList;
			}
			for (Iterator i = handleElements.iterator(); i.hasNext(); ) {
				Element handleElement = (Element) i.next();
				handleList.add(handleElement.getText());
			}
		}
		return handleList;
	}


	/**
	 *  The main program for the NdrResponseReader class
	 *
	 * @param  args  The command line arguments
	 */
	public static void main(String[] args) {

	}

	// DOM utilities
	/**
	 *  Get all Nodes satisfying the given xpath.
	 *
	 * @param  xpath  an XPath
	 * @return        a List of all modes satisfying given XPath, or null
	 */
	protected List getNodes(String xpath) {
		try {
			return doc.selectNodes(xpath);
		} catch (Throwable e) {
			prtln("getNodes() failed with " + xpath + ": " + e);
		}
		return null;
	}


	/**
	 *  Gets a single Node satisfying give XPath. If more than one Node is found,
	 *  the first is returned (and a msg is printed).
	 *
	 * @param  xpath  an XPath
	 * @return        a dom4j Node
	 */

	protected Node getNode(String xpath) {
		List list = getNodes(xpath);
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
	 *  return the Text of a Node satisfying the given XPath.
	 *
	 * @param  xpath  an XPath\
	 * @return        Text of Node or empty String if no Node is found
	 */
	protected String getNodeText(String xpath) {
		Node node = getNode(xpath);
		try {
			return node.getText();
		} catch (Throwable t) {

			// prtln ("getNodeText() failed with " + xpath + "\n" + t.getMessage());
			// Dom4jUtils.prettyPrint (docMap.getDocument());
		}
		return "";
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
			SchemEditUtils.prtln(s, "NdrResponseReader");
			// SchemEditUtils.prtln(s, "");
		}
	}

}

