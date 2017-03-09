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
package org.dlese.dpc.xml.nldr;

import org.dlese.dpc.util.Utils;
import org.dlese.dpc.util.Files;
import org.dlese.dpc.xml.Dom4jUtils;
import org.dlese.dpc.xml.XMLUtils;
import org.dlese.dpc.xml.*;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Attribute;
import org.dom4j.Node;

import java.util.*;
import java.lang.*;
import java.io.*;
import java.text.*;
import java.net.*;
import java.util.Hashtable;
import java.util.regex.*;

/**
 *  Reads XML records and converts to an exported form (i.e., "containing
 *  citableUrls"). NLDR metadata records may involve mulitple-namespaces and may conttain "assets"
 *  (primary content in the NLDR).
 *
 *@author    Jonathan Ostwald
 */
public abstract class NldrMetadataRecordExporter {
	private static boolean debug = true;
	private Node recordNode = null;

	/**
	 *  Constructor that loads the given record. No validation is performed.
	 *
	 *@param  xml                    The XML to start with
	 *@exception  DocumentException  If error parsing the XML
	 */
	public NldrMetadataRecordExporter(String xml) throws DocumentException {
		recordNode = Dom4jUtils.getXmlDocument(xml);
	}

	/**
	 *  Gets the id attribute of the NldrMetadataRecord object
	 *
	 *@return    The id value
	 */
	public abstract String getId();


	/**
	 *  Gets the assetNodes attribute of the NldrMetadataRecord object
	 *
	 *@return    The assetNodes value
	 */
	public abstract List getAssetNodes();

	/**
	 *  Gets the textAtPath attribute of the NldrMetadataRecord object
	 *
	 *@param  path  NOT YET DOCUMENTED
	 *@return       The textAtPath value
	 */
	public String getTextAtPath(String path) {
		return recordNode.valueOf(makeXPath(path));
	}


	/**
	 *  Sets the textAtPath attribute of the NldrMetadataRecord object
	 *
	 *@param  xpath  xpath of element
	 *@param  value  text to set
	 */
	public void setTextAtPath(String xpath, String value) {
		recordNode.selectSingleNode(makeXPath(xpath)).setText(value);
	}


	/**
	 *  Select Nodes for provided xpath
	 *
	 *@param  xpath  xpath
	 *@return        list of selected nodes
	 */
	public List selectNodes(String xpath) {
		return recordNode.selectNodes(makeXPath(xpath));
	}


	/**
	 *  Select Single node for provided xpath
	 *
	 *@param  xpath  xpath
	 *@return        NOT YET DOCUMENTED
	 */
	public Node selectSingleNode(String xpath) {
		return recordNode.selectSingleNode(makeXPath(xpath));
	}


	/**
	 *  Expands xpath into namespace-aware version, hanlding attributes and
	 *  attribute/value specifiers.<p>
	 *
	 *
	 *  <dl>
	 *    <dt> /record/relation
	 *    <dd> /*[local-name()='record']/*[local-name()='relation'
	 *    <dt> //relation/@type
	 *    <dd> //*[local-name()='relation']/@type
	 *    <dt> /record/relation[@type='Has part']
	 *    <dd> /*[local-name()='record']/*[local-name()='relation'][@type='Has
	 *    part']
	 *  </dl>
	 *
	 *
	 *@param  s  xpath as a qualifed string
	 *@return    namespace aware xpath
	 */
	public static String makeXPath(String s) {
		// prtln ("\n" + s);
		String[] splits = s.split("/");
		String xpath = "";

		for (int i = 1; i < splits.length; i++) {
			String segment = splits[i];
			xpath += "/";

			if (segment.startsWith("@")) {
				xpath += segment;
			} else {
				String elementName = segment;
				String attrValueSpecifier = "";
				int avsStart = segment.indexOf("[@");
				if (avsStart != -1) {
					attrValueSpecifier = segment.substring(avsStart);
					elementName = segment.substring(0, avsStart);
				}
				/*
				 *  prtln ("elementName: " + elementName);
				 *  prtln ("attrValueSpecifier: " + attrValueSpecifier);
				 */
				if (elementName.length() > 0) {
					xpath += "*[local-name()=\'" + elementName + "\']";
				}
				if (attrValueSpecifier != null) {
					xpath += attrValueSpecifier;
				}
			}
		}
		return xpath;
	}


	/**
	 *  Gets the xmlNode attribute of the NldrMetadataRecord object
	 *
	 *@return    The xmlNode value
	 */
	public Node getXmlNode() {
		return recordNode;
	}


	/**
	 *  Gets the xml attribute of the NldrMetadataRecord object
	 *
	 *@return    The xml value
	 */
	public String getXml() {
		if (recordNode == null) {
			return "";
		} else {
			return recordNode.asXML();
		}
	}


	/**
	 *  Gets the xml stripped of the XML declaration and DTD declaration.
	 *
	 *@return    The xml value
	 */
	public String getXmlStripped() {
		if (recordNode == null) {
			return "";
		} else {
			try {
				StringBuffer xml = XMLConversionService.stripXmlDeclaration(
						new BufferedReader(new StringReader(recordNode.asXML())));
				return xml.toString();
			} catch (Throwable t) {
				return "";
			}
		}
	}


	/**
	 *  Get a String representation of this XML.
	 *
	 *@return    The XML string
	 */
	public String toString() {
		return getXml();
	}

	// ---------------------- Debug info --------------------

	/**
	 *  Return a string for the current time and date, sutiable for display in log
	 *  files and output to standout:
	 *
	 *@return    The dateStamp value
	 */
	protected final static String getDateStamp() {
		return
				new SimpleDateFormat("MMM d, yyyy h:mm:ss a zzz").format(new Date());
	}


	/**
	 *  Description of the Method
	 *
	 *@param  node  Description of the Parameter
	 */
	protected final static void pp(Node node) {
		System.out.println(Dom4jUtils.prettyPrint(node));
	}


	/**
	 *  Output a line of text to error out, with datestamp.
	 *
	 *@param  s  The text that will be output to error out.
	 */
	protected final void prtlnErr(String s) {
		System.err.println(getDateStamp() + " NldrMetadataRecord Error: " + s);
	}


	/**
	 *  Output a line of text to standard out, with datestamp, if debug is set to
	 *  true.
	 *
	 *@param  s  The String that will be output.
	 */
	private static void prtln(String s) {
		if (debug) {
			// System.out.println(getDateStamp() + " NldrMetadataRecord: " + s);
			System.out.println(" NldrMetadataRecord: " + s);
		}
	}


	/**
	 *  Sets the debug attribute of the object
	 *
	 *@param  db  The new debug value
	 */
	public static void setDebug(boolean db) {
		debug = db;
	}
}

