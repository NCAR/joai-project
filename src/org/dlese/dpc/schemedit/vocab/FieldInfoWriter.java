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
 *  Class to generate fields files for given framework. <p>
 *
 *  NOTE: currently only generates field DEFINITION!
 *
 * @author    ostwald
 */
public class FieldInfoWriter {
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
	 *  Constructor for the FieldInfoWriter object
	 *
	 * @param  basePath       directory into which fields files are written
	 * @param  xmlFormat      NOT YET DOCUMENTED
	 * @exception  Exception  if basePath does not exist
	 */
	public FieldInfoWriter(String xmlFormat, String basePath) throws Exception {
		this.baseDir = new File(basePath);
		this.xmlFormat = xmlFormat;
		if (!baseDir.exists() || !baseDir.isDirectory())
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
			throw new Exception("Fields Framework NOT initialized");

		itemFramework = getFramework(this.xmlFormat);
		if (itemFramework == null)
			throw new Exception("Item Framework NOT initialized for " + xmlFormat);
		initializeDirectories();
		prtln("destDir: " + this.destDir);
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	void initializeDirectories() throws Exception {
		if (itemFramework == null)
			throw new Exception("can't make fields files dir until itemFramework is loaded");

		File frameworkDir = new File(baseDir, getMetaFormat());
		if (!frameworkDir.exists())
			frameworkDir.mkdir();

		File versionDir = new File(frameworkDir, getMetaVersion());
		if (!versionDir.exists())
			versionDir.mkdir();

		File buildDir = new File(versionDir, "build");
		if (!buildDir.exists())
			buildDir.mkdir();

		destDir = new File(versionDir, "fields");
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

		setDefinition(getDefinition(xpath), doc);

		// return this.getWritableDoc();
		return doc;
	}


	/**
	 *  Gets the nodeName attribute of the FieldInfoWriter object
	 *
	 * @return    The nodeName value
	 */
	String getNodeName() {
		return XPathUtils.getLeaf(xpath);
	}


	/**
	 *  Gets the definition attribute of the FieldInfoWriter object
	 *
	 * @param  xpath  NOT YET DOCUMENTED
	 * @return        The definition value
	 */
	String getDefinition(String xpath) {
		String defn = null;
		SchemaNode schemaNode = itemFramework.getSchemaHelper().getSchemaNode(xpath);
		if (schemaNode == null)
			return null;
		defn = schemaNode.getDocumentation();
		if (defn == null)
			defn = schemaNode.getTypeDef().getDocumentation();
		return defn;
	}


	/**
	 *  Sets the definition attribute of the FieldInfoWriter object
	 *
	 * @param  definition  The new definition value
	 * @param  doc         The new definition value
	 */
	void setDefinition(String definition, Document doc) {
		SchemaHelper sh = fieldsFramework.getSchemaHelper();
		DocMap docMap = new DocMap(doc, sh);
		if (definition != null) {
			try {
				docMap.smartPut("/metadataFieldInfo/field/definition", definition);
			} catch (Exception e) {
				prtln("Set Definition error: " + e.getMessage());
			}
		}
	}


	/**
	 *  Gets the fieldName attribute of the FieldInfoWriter object
	 *
	 * @return    The fieldName value
	 */
	String getFieldName() {
		String leaf = getNodeName();
		return leaf.substring(0, 1).toUpperCase() + leaf.substring(1);
	}


	/**
	 *  Gets the language attribute of the FieldInfoWriter object
	 *
	 * @return    The language value
	 */
	String getLanguage() {
		return "en-us";
	}


	/**
	 *  Gets the fileName attribute of the FieldInfoWriter object
	 *
	 * @return    The fileName value
	 */
	String getFileName() {
		return NamespaceRegistry.stripNamespacePrefix(getNodeName()) + "-" + this.getLanguage() + ".xml";
	}


	/**
	 *  Gets the metaFormat attribute of the FieldInfoWriter object
	 *
	 * @return    The metaFormat value
	 */
	String getMetaFormat() {
		return itemFramework.getXmlFormat();
	}


	/**
	 *  Gets the metaVersion attribute of the FieldInfoWriter object
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
	 *  NOT YET DOCUMENTED
	 *
	 * @param  doc  NOT YET DOCUMENTED
	 * @return      NOT YET DOCUMENTED
	 */
	static boolean hasDefinition(Document doc) {
		Node node = doc.selectSingleNode("/metadataFieldInfo/field/definition");
		return (node != null && node.getText().length() > 0);
	}


	/**
	 *  Write fields file document to disk.
	 *
	 * @param  doc            NOT YET DOCUMENTED
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	void write(Document doc) throws Exception {
		File dest = new File(this.destDir, this.getFileName());
		prtln("dest: " + dest);
		Document writableDoc = fieldsFramework.getWritableRecord(doc);
		Dom4jUtils.writePrettyDocToFile(writableDoc, dest);
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  xmlFormat      NOT YET DOCUMENTED
	 * @param  basePath       NOT YET DOCUMENTED
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	static void makeAllFieldsFiles(String xmlFormat, String basePath) throws Exception {

		FieldInfoWriter writer;

		try {
			writer = new FieldInfoWriter(xmlFormat, basePath);
		} catch (Exception e) {
			prtln("error: " + e.getMessage());
			return;
		}

		SchemaNodeMap schemaNodeMap = writer.itemFramework.getSchemaHelper().getSchemaNodeMap();
		int counter = 3;
		int max = Integer.MAX_VALUE;
		for (Iterator i = schemaNodeMap.getKeys().iterator(); i.hasNext(); ) {
			String fieldPath = (String) i.next();
			Document doc = writer.makeFieldsFileDocument(fieldPath);
			if (hasDefinition(doc))
				writer.write(doc);
			if (++counter > max)
				break;
		}
	}


	/**
	 *  Debugging method (when we create a bunch of fields files we use the same
	 *  FieldInfoWriter over and over.
	 *
	 * @param  fieldPath      NOT YET DOCUMENTED
	 * @param  xmlFormat      NOT YET DOCUMENTED
	 * @param  basePath       NOT YET DOCUMENTED
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	static void makeFieldsFile(String fieldPath, String xmlFormat, String basePath) throws Exception {
		FieldInfoWriter writer;

		try {
			writer = new FieldInfoWriter(xmlFormat, basePath);
		} catch (Exception e) {
			prtln("error: " + e.getMessage());
			e.printStackTrace();
			return;
		}

		Document doc = writer.makeFieldsFileDocument(fieldPath);
		// pp(doc);
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

		String xmlFormat = "mets";
		String basePath = "C:/tmp/mets_fields_files";

		// make fields file for specified format and field
		String fieldPath = "/this:mets/this:behaviorSec/this:behavior/this:interfaceDef";
		// makeFieldsFile(fieldPath, xmlFormat, basePath);

		// make all fields files for specified format
		makeAllFieldsFiles (xmlFormat, basePath);

	}


	/**
	 *  Gets the framework attribute of the FieldInfoWriter object
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
	 *  Sets the debug attribute of the FieldInfoWriter class
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

