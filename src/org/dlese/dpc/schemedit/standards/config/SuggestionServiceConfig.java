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
package org.dlese.dpc.schemedit.standards.config;

import org.dlese.dpc.schemedit.standards.StandardsRegistry;
import org.dlese.dpc.schemedit.standards.asn.AsnDocKey;
import org.dlese.dpc.xml.schema.*;
import org.dlese.dpc.xml.Dom4jUtils;
import org.dlese.dpc.schemedit.*;

import java.io.*;
import java.util.*;

import java.net.*;

import org.dom4j.*;

/**
 *  Encapsulates configuration for a suggestion service, incuding the framework that the service operates for
 and what standards files are to be used for that particular framework.
 *
 *@author    ostwald
 */
public class SuggestionServiceConfig {

	private static boolean debug = true;
	private Element element = null;
	public static String DATA_TYPES = "dataTypes";
	public static String STANDARDS_FILE = "standardsFile";
	public static String STANDARDS_DIRECTORY = "standardsDirectory";


	/**
	 *  Constructor for the SuggestionServiceConfig object with provided
	 *  configuration Element.
	 *
	 *@param  config         A element form a standards config file
	 */
	public SuggestionServiceConfig(Element config) {
		this.element = config;
	}


	/**
	 *  Gets the version attribute of the SuggestionServiceConfig object
	 *
	 *@return    The version value
	 */
	public String getVersion() {
		return getNodeText("xmlFormat/@version");
	}


	/**
	 *  Gets the xpath attribute of the SuggestionServiceConfig object
	 *
	 *@return    The xpath value
	 */
	public String getXpath() {
		return getNodeText("xpath");
	}


	/**
	 *  Gets the standardSourceType attribute of the SuggestionServiceConfig object
	 *
	 *@return    The standardSourceType value
	 */
	public String getStandardSourceType() {
		Element stdSource = (Element) getNode("stdSource");
		return ((Element) stdSource.elements().get(0)).getName();
	}


	/**
	 *  Gets the helperClass attribute of the SuggestionServiceConfig object
	 *
	 *@return    The helperClass value
	 */
	public String getHelperClass() {
		return getNodeText("helperClass");
	}


	/**
	 *  Gets the pluginClass that supplies framework-specific informations, like xpaths, to
	 the suggestionService.
	 *
	 *@return    The pluginClass value
	 */
	public String getPluginClass() {
		return getNodeText("plugin");
	}


	/**
	 *  Gets the dataTypes attribute of the SuggestionServiceConfig object
	 *
	 *@return                The dataTypes value
	 *@exception  Exception  Description of the Exception
	 */
	public List getDataTypes() throws Exception {
		String sourceType = getStandardSourceType();
		if (!sourceType.equals(DATA_TYPES)) {
			throw new Exception("cannot get dataTypes for " + sourceType);
		}
		List dataTypes = new ArrayList();
		List nodes = getNodes("stdSource/dataTypes/dataType");
		for (Iterator i = nodes.iterator(); i.hasNext(); ) {
			dataTypes.add(((Node) i.next()).getText());
		}
		return dataTypes;
	}


	/**
	 *  Gets the standardsDirectory attribute of the SuggestionServiceConfig object, which 
	 designates a directory containing standards files.
	 *
	 *@return                The standardsDirectory value
	 *@exception  Exception  Description of the Exception
	 */
	public String getStandardsDirectory() throws Exception {
		String sourceType = getStandardSourceType();
		if (!sourceType.equals(STANDARDS_DIRECTORY)) {
			throw new Exception("cannot get standardsDirectory for " + sourceType);
		}
		return getNodeText("stdSource/standardsDirectory");
	}


	/**
	 *  Gets the defaultDocKey attribute of the SuggestionServiceConfig object
	 *
	 *@return                The defaultDocKey value
	 *@exception  Exception  Description of the Exception
	 */
	public String getDefaultDocKey() throws Exception {
		String sourceType = getStandardSourceType();
		if (!sourceType.equals(STANDARDS_DIRECTORY)) {
			throw new Exception("cannot get getDefaultDocKey for " + sourceType);
		}
		Element sdElement = (Element) getNode("stdSource/standardsDirectory");
		String author = sdElement.attributeValue("defaultAuthor", "");
		String topic = sdElement.attributeValue("defaultTopic", "");
		String year = sdElement.attributeValue("defaultYear", "");
		// return StandardsRegistry.makeDocKey (author, topic, year);
		return new AsnDocKey(author, topic, year).toString();
	}


	/**
	 *  Gets the defaultDoc attribute of the SuggestionServiceConfig object
	 *
	 *@return                The defaultDoc value
	 *@exception  Exception  Description of the Exception
	 */
	public String getDefaultDoc() throws Exception {
		String standardsDirectory = getStandardsDirectory();
		return getNodeText("stdSource/standardsDirectory/@defaultDoc");
	}


	/**
	 *  Gets the standardsFile attribute of the SuggestionServiceConfig object
	 *
	 *@return                The standardsFile value
	 *@exception  Exception  Description of the Exception
	 */
	public String getStandardsFile() throws Exception {
		String sourceType = getStandardSourceType();
		if (!sourceType.equals(STANDARDS_FILE)) {
			throw new Exception("cannot get dataTypes for " + sourceType);
		}
		return getNodeText("stdSource/standardsFile");
	}

	// ----------- DOM utilities -------------------
	/**
	 *  Get all Nodes satisfying the given xpath.
	 *
	 *@param  xpath  an XPath
	 *@return        a List of all modes satisfying given XPath, or null
	 */
	private List getNodes(String xpath) {
		try {
			return element.selectNodes(xpath);
		} catch (Throwable e) {
			prtln("getNodes() failed with " + xpath + ": " + e);
		}
		return null;
	}


	/**
	 *  Gets a single Node satisfying give XPath. If more than one Node is found,
	 *  the first is returned (and a msg is printed).
	 *
	 *@param  xpath  an XPath
	 *@return        a dom4j Node
	 */

	private Node getNode(String xpath) {
		List list = getNodes(xpath);
		if ((list == null) || (list.size() == 0)) {
			// prtln ("getNode() did not find node for " + xpath);
			return null;
		}
		if (list.size() > 1) {
			prtln("getNode() found multiple modes for " + xpath + " (returning first)");
		}
		return (Node) list.get(0);
	}


	/**
	 *  return the Text of a Node satisfying the given XPath.
	 *
	 *@param  xpath  an XPath\
	 *@return        Text of Node or empty String if no Node is found
	 */
	private String getNodeText(String xpath) {
		Node node = getNode(xpath);
		try {
			return node.getText().trim();
		} catch (Throwable t) {

			// prtln ("getNodeText() failed with " + xpath + "\n" + t.getMessage());
			// Dom4jUtils.prettyPrint (docMap.getDocument());
		}
		return "";
	}


	/**
	 *  Description of the Method
	 *
	 *@param  node  Description of the Parameter
	 */
	private static void pp(Node node) {
		prtln(Dom4jUtils.prettyPrint(node));
	}


	/**
	 *  Print a line to standard out.
	 *
	 *@param  s  The String to print.
	 */
	private static void prtln(String s) {
		if (debug) {
			SchemEditUtils.prtln(s, "");
		}
	}

}

