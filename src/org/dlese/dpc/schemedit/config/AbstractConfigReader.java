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
package org.dlese.dpc.schemedit.config;

import java.io.*;
import java.util.*;
import java.text.*;

import org.dlese.dpc.xml.*;
import org.dlese.dpc.xml.schema.DocMap;
import org.dlese.dpc.util.*;
import org.dlese.dpc.repository.*;
import org.dlese.dpc.schemedit.*;

import org.dom4j.Document;
import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import org.dom4j.Attribute;
import org.dom4j.Node;

/**
 *  Abstract class for extracting information from, and writing to, XML config
 *  files
 *
 *@author    ostwald <p>
 *
 */
public class AbstractConfigReader implements Serializable {

	/**
	 *  Description of the Field
	 */
	protected static boolean debug = false;
	/**
	 *  Description of the Field
	 */
	protected static boolean writeOnDestroy = false;

	protected String rootElementName;
	protected String nameSpaceInfo = "";
	protected DocMap docMap = null;
	protected File source = null;
	protected DocumentFactory df = DocumentFactory.getInstance();


	/**
	 *  Create a AbstractConfigReader.
	 *
	 *@param  source  Description of the Parameter
	 */
	public AbstractConfigReader(File source) throws Exception {
		prtln ("Initializing config reader at " + source.getAbsolutePath());
		this.source = source;
		try {
			if (!source.exists()) {
				throw new Exception ("source file does not exist at " + source.getAbsolutePath());
			}
			if (!source.canRead()) {
				throw new Exception ("source file exists but cannot be read at " + source.getAbsolutePath());
			}
		
			docMap = getDocMap();
			if (docMap == null)
				throw new Exception ("failed to process config file");
			/* rootElementName = docMap.getDocument().getRootElement().getName(); */
		} catch (Exception e) {
			throw new Exception (" ERROR: AbstractConfigReader could not initialize: " + e.getMessage());
		}
	}
	
	public AbstractConfigReader(String xmlSource) throws Exception {
		prtln ("Initializing config reader from xmlSource");
		try {
			Document doc = Dom4jUtils.getXmlDocument(xmlSource);
			rootElementName = doc.getRootElement().getName();
			Document localizedDoc = Dom4jUtils.localizeXml(doc, rootElementName);
			nameSpaceInfo = Dom4jUtils.getNameSpaceInfo(doc, rootElementName);
			docMap = new DocMap(localizedDoc);
			if (docMap == null)
				throw new Exception ("failed to process provided XML");
			/* rootElementName = docMap.getDocument().getRootElement().getName(); */
		} catch (Exception e) {
			throw new Exception (" ERROR: AbstractConfigReader could not initialize: " + e.getMessage());
		}
	}
	

	/**
	 *  Gets the sourcePath attribute of the AbstractConfigReader object
	 *
	 *@return    The sourcePath value
	 */
	public String getSourcePath() {
		return source.getAbsolutePath();
	}


	//------------------------------------------------------------
	/**
	 *  Gets the document attribute of the AbstractConfigReader object
	 *
	 *@return                The document value
	 *@exception  Exception  Description of the Exception
	 */
	public DocMap getDocMap()
		throws Exception {
			
		if (docMap == null) {
			if (source != null && source.exists()) {
				String xml = Files.readFileToEncoding(source, "UTF-8").toString();
				Document doc = Dom4jUtils.getXmlDocument(xml);
				rootElementName = doc.getRootElement().getName();
				Document localizedDoc = Dom4jUtils.localizeXml(doc, rootElementName);
				nameSpaceInfo = Dom4jUtils.getNameSpaceInfo(doc, rootElementName);
				docMap = new DocMap(localizedDoc);
			}
			else {
				throw new Exception ("configReader error: source file not found at " + 
					this.source.getAbsolutePath());
			}
		}
		return docMap;
	}


	/**
	 *  Gets the source attribute of the AbstractConfigReader object
	 *
	 *@return    The source value
	 */
	public File getSource() {
		return source;
	}


	/**
	 *  set source file, used when creating new CollectionConfig's by first reading a
	 *  default config file and then assigning a new source so it will be written
	 *  to a new config file
	 *
	 *@param  file  The new source value
	 */
	protected void setSource(File file) {
		source = file;
	}

	/**
	* Gets the localized Document for this reader.
	*/
	public Document getDocument () {
		DocMap docMap;
		try {
			return getDocMap().getDocument();
		} catch (Throwable t) {
			prtln ("WARNING could not obtain document");
			t.printStackTrace();
		}
		return null;
	}
	
	/**
	* Gets delocalized Document for this reader.
	*/
	public Document getDelocalizedDoc () throws Exception {
		Document doc = getDocument();

		Document delocalizedDoc = Dom4jUtils.delocalizeXml(doc, rootElementName, nameSpaceInfo);
		if (delocalizedDoc == null) {
			throw new Exception("getDelocalizedDoc() could not delocalize xmlDocument");
		}
		return delocalizedDoc;
	}

	/**
	 *  Write config document to disk and then force reread of values.
	 *
	 *@exception  Exception  Description of the Exception
	 */
	public void flush()
		throws Exception {
		if (this.source == null)
			throw new Exception ("Flush failed because source is undefined");
		Dom4jUtils.writePrettyDocToFile(getDelocalizedDoc(), source);
		prtln ("\n\n*** config file written to " + source.getAbsolutePath() + " ***\n\n");
		this.refresh();
	}

	/**
	 *  Write Document to disk and then set docMap to null, forcing a re-read upon next access.
	 */
	public void refresh() {
		this.docMap = null;
		prtln (" ... refreshed");
	}


	// DOM utilities
	/**
	 *  Get all Nodes satisfying the given xpath.
	 *
	 *@param  xpath  an XPath
	 *@return        a List of all modes satisfying given XPath, or null
	 */
	public List getNodes(String xpath) {
		try {
			return getDocMap().selectNodes(xpath);
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

	public Node getNode(String xpath) {
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
	public String getNodeText(String xpath) {
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
	 *  Sets the nodeText for specified path, creating new node if necessary.
	 *
	 *@param  xpath  The new nodeText value
	 *@param  value  The new nodeText value
	 */
	protected void setNodeText(String xpath, String value) {
		Node node = getNode(xpath);
		if (node == null) {
			prtln("setNodeText did not find node at " + xpath + " - creating ...");
			try {
				node = docMap.createNewNode(xpath);
			} catch (Exception e) {
				prtln("configReader could not find or create node at " + xpath);
				return;
			}
		}
		node.setText(value);
	}


	/**
	 *  This method is called at the conclusion of processing and may be used for
	 *  tear-down.
	 */
	public void destroy() {
		if (writeOnDestroy) {
			try {
				flush();
				prtln("config file written to disk: " + getSource().getAbsolutePath());
			} catch (Throwable e) {
				prtln("destroy() putDocumentError: " + e.getMessage());
			}
		}
		docMap = null;
	}


	/**
	 *  Print a line to standard out.
	 *
	 *@param  s  The String to print.
	 */
	protected static void prtln(String s) {
		if (debug) {
			SchemEditUtils.prtln(s, "AbstractConfigReader");
		}
	}

}

