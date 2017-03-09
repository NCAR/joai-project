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
package org.dlese.dpc.schemedit.vocab;

import java.io.*;
import java.util.*;
import java.net.*;

import org.dom4j.*;

import org.dlese.dpc.xml.*;
import org.dlese.dpc.util.Utils;
import org.dlese.dpc.schemedit.SchemEditUtils;
import org.dlese.dpc.schemedit.MetaDataFramework;
import org.dlese.dpc.schemedit.test.FrameworkTester;
import org.dlese.dpc.schemedit.test.TesterUtils;
import org.dlese.dpc.serviceclients.webclient.*;
import org.dlese.dpc.xml.schema.*;
import org.dlese.dpc.util.Files;

import org.dlese.dpc.standards.asn.NameSpaceXMLDocReader;

import org.apache.struts.util.LabelValueBean;

/**
 *  Class to generate fields files for given framework. <p>NOTE: currently only generates field DEFINITION!
 *
 * @author    ostwald
 */
public class SIFFieldInfoWriter {
	private static boolean debug = true;
	/**  Description of the Field */

	private MetaDataFramework fieldsFramework = null;
	private MetaDataFramework itemFramework = null;
	SchemaNode schemaNode = null;
	String xpath = null;
	Map valueMap = null;
	File baseDir = null;
	File destDir;
	String xmlFormat;


	/**
	 *  Constructor for the SIFFieldInfoWriter object
	 *
	 * @param  basePath       directory into which fields files are written
	 * @exception  Exception  if basePath does not exist
	 */
	public SIFFieldInfoWriter(String xmlFormat, String basePath) throws Exception {
		this.baseDir = new File(basePath);
		this.xmlFormat = xmlFormat;
		if (!baseDir.exists() && baseDir.isDirectory())
			throw new Exception("destDir does not exist or is not Directory at " + basePath);
		this.init();
	}

	/**
	 *  Read MetaDataFramework in prepration for creation of fields files.
	 *
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	void init() throws Exception {
		MetaDataFramework.setDebug(false);
		SchemaReader.setDebug(false);
		fieldsFramework = getFramework("fields_file");
		if (fieldsFramework == null)
			throw new Exception ("Fields Framework NOT initialized");

		itemFramework = getFramework(this.xmlFormat);
		if (itemFramework == null)
			throw new Exception ("Item Framework NOT initialized for " + xmlFormat);
		initializeDirectories ();
		prtln ("destDir: " + this.destDir);
	}
	
	void initializeDirectories () throws Exception {
		if (itemFramework == null)
			throw new Exception ("can't make fields files dir until itemFramework is loaded");
		
		File frameworkDir = new File (baseDir, getMetaFormat());
		if (!frameworkDir.exists())
			frameworkDir.mkdir();
		
		File versionDir = new File (frameworkDir, getMetaVersion());
		if (!versionDir.exists())
			versionDir.mkdir();
		
		File buildDir = new File (versionDir, "build");
		if (!buildDir.exists())
			buildDir.mkdir();
		
		destDir = new File (versionDir, "fields");
		if (!destDir.exists())
			destDir.mkdir();
	}

	
	
	/**
	 *  Creates a fields file Document for specified xpath
	 *
	 * @param  xpath          xpath of field
	 * @return                fields file document
	 * @exception  Exception  if there is a problem with the xpath or document
	 *      creation
	 */
	Document makeFieldsFileDocument(String xpath) throws Exception {
		this.xpath = xpath;
		Document doc = fieldsFramework.makeMinimalRecord(null);
		schemaNode = itemFramework.getSchemaHelper().getSchemaNode(this.xpath);
		if (schemaNode == null)
			throw new Exception("schemaNode not found for " + this.xpath);
		try {
			this.populate(doc);
		} catch (Exception e) {
			throw new Exception("populate error: " + e.getMessage());
		}
		// return this.getWritableDoc();
		return doc;
	}





	/**
	 *  Gets the nodeName attribute of the SIFFieldInfoWriter object
	 *
	 * @return    The nodeName value
	 */
	String getNodeName() {
		return XPathUtils.getLeaf(xpath);
	}


	/**
	 *  Gets the fieldName attribute of the SIFFieldInfoWriter object
	 *
	 * @return    The fieldName value
	 */
	String getFieldName() {
		String leaf = getNodeName();
		return leaf.substring(0, 1).toUpperCase() + leaf.substring(1);
	}


	/**
	 *  Gets the language attribute of the SIFFieldInfoWriter object
	 *
	 * @return    The language value
	 */
	String getLanguage() {
		return "en-us";
	}


	/**
	 *  Gets the fileName attribute of the SIFFieldInfoWriter object
	 *
	 * @return    The fileName value
	 */
	String getFileName() {
		return NamespaceRegistry.stripNamespacePrefix(getNodeName()) + "-" + this.getLanguage() + ".xml";
	}


	/**
	 *  Gets the metaFormat attribute of the SIFFieldInfoWriter object
	 *
	 * @return    The metaFormat value
	 */
	String getMetaFormat() {
		return itemFramework.getXmlFormat();
	}


	/**
	 *  Gets the metaVersion attribute of the SIFFieldInfoWriter object
	 *
	 * @return    The metaVersion value
	 */
	String getMetaVersion() {
		return itemFramework.getVersion();
	}


	/**
	 *  Populate the fields of the fields file document
	 *
	 * @param  doc            NOT YET DOCUMENTED
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	void populate(Document doc) throws Exception {
		SchemaHelper sh = fieldsFramework.getSchemaHelper();
		DocMap docMap = new DocMap(doc, sh);
		docMap.smartPut("/metadataFieldInfo/field/@name", getFieldName());
		docMap.smartPut("/metadataFieldInfo/field/@language", "en-us");
		docMap.smartPut("/metadataFieldInfo/field/@metaFormat", this.getMetaFormat());
		docMap.smartPut("/metadataFieldInfo/field/@metaVersion", this.getMetaVersion());
		docMap.smartPut("/metadataFieldInfo/field/@path", this.xpath);
	}
	
	/**
	convert from schema location to the location of data model schema. only works for SIF
	
	xsdLoc: http://www.dls.ucar.edu/people/ostwald/Metadata/sif/InstructionalServices/Activity.xsd
	dataModelLoc: http://www.dls.ucar.edu/people/ostwald/Metadata/sif/DataModelAnnotated/InstructionalServices/Activity.xsd
	*/
	static URL getDataModelLoc (String xsdLoc) throws Exception {
		String root = "http://www.dls.ucar.edu/people/ostwald/Metadata/sif/";
		if (!xsdLoc.startsWith(root))
			throw new Exception ("root not found");
		return new URL (root + "DataModelAnnotated/" + xsdLoc.substring(root.length()));
	}
		
	/**
	* Critical insight: we want the typeDef of the PARENT of the schemaNode at given path!?
	(since it is the parent's type definition that will document ...)
	*/
	void setSIFDefinition (Document doc) {
		String itemXmlFormat = this.itemFramework.getXmlFormat();
		if (!itemXmlFormat.startsWith("sif")) {
			prtln ("can't set SIF Definition for " + itemXmlFormat + " format");
			return;
		}
		
		SchemaHelper itemSh = this.itemFramework.getSchemaHelper();
		SchemaHelper sh = fieldsFramework.getSchemaHelper();
		DocMap docMap = new DocMap(doc, sh);
		SchemaNode schemaNode = itemSh.getSchemaNode(this.xpath);
		if (schemaNode == null) {
			// prtln ("schemaNode not found for " + xpath);
			return;
		}
		
/* 		prtln ("\n---- schemaNode ----");
		prtln (schemaNode.toString());
		prtln ("----------"); */
		
		SchemaNode parent = schemaNode.getParent();
		if (parent != null) {
/* 			prtln ("\n---- parentNode ----");
			prtln (parent.toString());
			prtln ("----------"); */
		}
		else {
			return;
		}
		
		GlobalDef typeDef = parent.getTypeDef();
		
/* 		prtln ("\n----------");
		prtln (typeDef.toString());
		prtln ("----------"); */
		
		Document xsdDoc = null;
		NameSpaceXMLDocReader xsdReader = null;
		String loc = typeDef.getLocation();
		// prtln ("LOC: " + loc);

		try {
			URL url = getDataModelLoc (typeDef.getLocation());
			// xsdDoc = Dom4jUtils.getXmlDocument(url);
			xsdReader = new NameSpaceXMLDocReader (url);
		} catch (Exception e) {
			prtln ("couldn't get typeDef document: " + e.getMessage());
			return;
		}
		
		String elementType = (schemaNode.isAttribute() ? "attribute" : "element");
		String attName = NamespaceRegistry.stripNamespacePrefix(this.getNodeName());
		String xpath = "//xs:" + elementType + "[@name=\'" + attName + "\']/xs:annotation/xs:documentation";
		// prtln ("\nxpath: " + xpath);
		Element globalElement = (Element) xsdReader.getNode (xpath);
		
		String definition = null;
		// prtln ("GLOBAL ELEMENT for " + this.getNodeName());
		if (globalElement == null) {
			prtln ("\t definition NOT FOUND");
		}
		else {
			definition = globalElement.getText();
		}
		
		if (definition != null) {
			try {
				docMap.smartPut("/metadataFieldInfo/field/definition", definition);
			} catch (Exception e) {
				prtln ("Set Definition error: " + e.getMessage());
			}
		}
	}
	
	static boolean hasDefinition (Document doc) {
		Node node = doc.selectSingleNode ("/metadataFieldInfo/field/definition");
		return (node != null && node.getText().length() > 0);
	}
	
	/**
	 *  Gets the writableDoc attribute of the SIFFieldInfoWriter object
	 *
	 * @return                The writableDoc value
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
/* 	Document getWritableDoc() throws Exception {
		return fieldsFramework.getWritableRecord(doc);
	} */


	/**
	 *  Write fields file document to disk.
	 *
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	void write(Document doc) throws Exception {
		File dest = new File(this.destDir, this.getFileName());
		prtln ("dest: " + dest);
		Document writableDoc = fieldsFramework.getWritableRecord(doc);
		Dom4jUtils.writePrettyDocToFile(writableDoc, dest);
	}

	static void makeAllFieldsFiles (String xmlFormat, String basePath) throws Exception {
		
		SIFFieldInfoWriter writer;

		try {
			writer = new SIFFieldInfoWriter(xmlFormat, basePath);
		} catch (Exception e) {
			prtln("error: " + e.getMessage());
			return;
		}
		

		SchemaNodeMap schemaNodeMap = writer.itemFramework.getSchemaHelper().getSchemaNodeMap();
		int counter = 0;
		int max = Integer.MAX_VALUE;
		for (Iterator i = schemaNodeMap.getKeys().iterator(); i.hasNext(); ) {
			String fieldPath = (String) i.next();
			Document doc = writer.makeFieldsFileDocument(fieldPath);
			// prtln("\n**" + writer.getFileName() + " **");
			// pp (writer.doc);
			writer.setSIFDefinition(doc);
			if (hasDefinition (doc))
				writer.write(doc);
			if (++counter > max)
				break;
		}
	}
	
	static void makeFieldsFile (String fieldPath, String xmlFormat, String basePath) throws Exception {
		SIFFieldInfoWriter writer;

		try {
			writer = new SIFFieldInfoWriter(xmlFormat, basePath);
		} catch (Exception e) {
			prtln("error: " + e.getMessage());
			e.printStackTrace();
			return;
		}
		
		Document doc = writer.makeFieldsFileDocument(fieldPath);
		writer.setSIFDefinition(doc);
		pp (doc);
		writer.write (doc);
	}
	
	/**
	 *  Walk the schema and create a fields file for each xpath.
	 *
	 * @param  args           The command line arguments
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public static void main(String[] args) throws Exception {
		TesterUtils.setSystemProps();
		setDebug(true);
		
		String xmlFormat = "sif_activity";
		// String basePath = "C:/tmp/sif_fields_files";
		String basePath = "/Users/ostwald/tmp/fields-files/";
		
		// make fields file for specified format and field
		String fieldPath = "/sif:Activity/sif:LearningResources/sif:LearningResourceRefId";
		makeFieldsFile (fieldPath, xmlFormat, basePath);
		
		// make all fields files for specified format
		// makeAllFieldsFiles (xmlFormat, basePath);
		
		// make fields files for all SIF formats
		// makeSIFFieldsFiles ();
	}

	static void makeSIFFieldsFiles () throws Exception {
		String basePath = "C:/tmp/sif_fields_files";
		
		String fieldPath = "/sif:Activity/sif:LearningResources/sif:LearningResourceRefId";
		// makeFieldsFile (fieldPath, xmlFormat, basePath);
		
		String frameworksPath = TesterUtils.getFrameworkConfigDir();
		prtln (frameworksPath);
		File [] files = new File (frameworksPath).listFiles (new XMLFileFilter());
		for (int i=0;i<files.length;i++) {
			File configFile = files[i];
			String fileName = configFile.getName();
			if (!fileName.startsWith("sif_")) continue;
			String xmlFormat = fileName.substring (0, fileName.length() - 4);
			prtln (xmlFormat);
			makeAllFieldsFiles (xmlFormat, basePath);
		}
	}

	/**
	 *  Gets the framework attribute of the SIFFieldInfoWriter object
	 *
	 * @param  xmlFormat      NOT YET DOCUMENTED
	 * @return                The framework value
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	MetaDataFramework getFramework(String xmlFormat) throws Exception {
		FrameworkTester ft = new FrameworkTester(xmlFormat);
		if (ft != null) {
			return ft.framework;
		}
		return null;
	}


	/**
	 *  Sets the debug attribute of the SIFFieldInfoWriter class
	 *
	 * @param  d  The new debug value
	 */
	public static void setDebug(boolean d) {
		debug = d;
	}



	/**
	 *  Output a line of text to standard out, with datestamp, if debug is set to
	 *  true.
	 *
	 * @param  s  The String that will be output.
	 */
	private static void prtln(String s) {
		if (debug) {
			SchemEditUtils.prtln(s, "");
		}
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  node  NOT YET DOCUMENTED
	 */
	private static void pp(Node node) {
		prtln(Dom4jUtils.prettyPrint(node));
	}

}

