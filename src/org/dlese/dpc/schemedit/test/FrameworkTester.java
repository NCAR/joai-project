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
package org.dlese.dpc.schemedit.test;

import java.io.*;
import java.util.*;
import java.text.*;
import java.net.URI;

import org.dlese.dpc.schemedit.*;
import org.dlese.dpc.schemedit.config.*;
import org.dlese.dpc.schemedit.input.EnsureMinimalDocument;
import org.dlese.dpc.schemedit.security.user.User;
import org.dlese.dpc.xml.*;
import org.dlese.dpc.xml.schema.*;
import org.dlese.dpc.xml.schema.compositor.Compositor;
import org.dlese.dpc.util.*;
import org.dlese.dpc.webapps.tools.GeneralServletTools;
import org.dlese.dpc.repository.*;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import org.dom4j.Attribute;
import org.dom4j.Node;
import org.dom4j.QName;

/**
 *  Tester for {@link org.dlese.dpc.schemedit.config.FrameworkConfigReader} and
 {@link org.dlese.dpc.schemedit.MetaDataFramework}
 *
 *@author    ostwald
 */
public class FrameworkTester {
	FrameworkConfigReader reader = null;
	public MetaDataFramework framework = null;
	FrameworkRegistry registry = null;


	/**
	 *  Constructor for the FrameworkTester object
	 */
	public FrameworkTester(String format) throws Exception {
		String errorMsg;
		String configFileName = format+".xml";
		String configDirPath = TesterUtils.getFrameworkConfigDir();
		File sourceFile = new File(configDirPath, configFileName);
		if (!sourceFile.exists()) {
			prtln("source File does not exist at " + sourceFile.toString());
			return;
		}
		else {
			prtln ("reading frameworkconfig file from: " + sourceFile.toString());
		}

		try {
			reader = new FrameworkConfigReader(sourceFile);
			String docRoot = "/devel/ostwald/tomcat/tomcat/webapps/schemedit/";
			framework = new MetaDataFramework(reader, docRoot);
			framework.loadSchemaHelper();
		} catch (Exception e) {
			errorMsg = "Error loading Schema Helper: " + e.getMessage();
			e.printStackTrace();
			throw new Exception (errorMsg);
			
		}
	}
			
	public MetaDataFramework getFramework () {
		return framework;
	}
			
	/**
	 *  Description of the Method
	 */
	public void showFramework() {
		prtln("FRAMEWORK");
		prtln(framework.toString());
		prtln("FieldInfoMap: \n" + framework.getFieldInfoMap().toString());
	}

	
	Document getEditableDocument1 (String filePath, MetaDataFramework framework) throws Exception, DocumentException {
		String rawXml;
		try {
			rawXml = Files.readFile (filePath).toString();
		} catch (IOException e) {
			throw new Exception (e.getMessage());
		}
		rawXml = SchemEditUtils.expandAmpersands(rawXml);
		// prtln (rawXml);
		Document doc = null;
		try {
			doc = Dom4jUtils.getXmlDocument(rawXml);
		} catch (DocumentException de) {
			throw new Exception (de.getMessage());
		} 
		// pp (doc);
		Element root = doc.getRootElement();
		Attribute schemaLocAtt = root.attribute ("schemaLocation");
		
		if (schemaLocAtt == null) {
			throw new Exception ("couldn't find schemaLocation attribute") ;
		}

		String targetNameSpace = schemaLocAtt.getText().split("\\s")[0];
		// prtln ("Document target namespace: " + targetNameSpace);
		
		String schemaLoc = schemaLocAtt.getText().split("\\s")[1];
		// prtln ("Document schema location: " + schemaLoc);

		String schemaHelperTNS = framework.getSchemaHelper().getTargetNamespace();
		
		if (!targetNameSpace.equals(schemaHelperTNS)) {
			throw new Exception ("document target namespace does not match schema helper (" + schemaHelperTNS + ")");
		}
		
		if (!schemaLoc.equals(framework.getSchemaURI())) {
			throw new Exception ("schema location does not match metadata framework (" + framework.getSchemaURI() + ")");
		}
		
		// doc = Dom4jUtils.localizeXml(doc, root.getName());
		doc = Dom4jUtils.localizeXml(doc);

		pp (doc);
		return doc;
	}
	
	static String getWritableRecordXmlFoo (Document doc, MetaDataFramework framework) throws DocumentException {
		try {
			String fwrkRootElementName = framework.getRootElementName();
			String rootElementName = doc.getRootElement().getName();
			if (!rootElementName.equals(fwrkRootElementName)) {
				throw new Exception ("Root element (\"" + rootElementName + 
					 "\") does not match schema (expected \"" +
					 fwrkRootElementName + "\")");
			}

			// Document delocalizedDoc = Dom4jUtils.delocalizeXml(doc, rootElementName, framework.getNameSpaceInfo());
			Document delocalizedDoc = framework.getWritableRecord(doc);
			if (delocalizedDoc == null) {
				throw new Exception ("Unable to delocalize document");
			}
			
			return SchemEditUtils.contractAmpersands(delocalizedDoc.asXML());
		} catch (Exception e) {
			throw new DocumentException ("getWritableRecordXml ERROR: " + e.getMessage());
		} catch (Throwable t) {
			t.printStackTrace();
			throw new DocumentException ("getWritableRecordXml: unknown error");
		}
	}
	
	
	static Document getEditableDocumentFoo (String filePath, MetaDataFramework framework) throws DocumentException {
		prtln ("\ngetEditableDocument()");
		
		boolean enforceSchemaLocation = false;
		String errorMsg = null;
		
		String schemaTargetNameSpace = framework.getSchemaHelper().getTargetNamespace();
		String schemaLocation = framework.getSchemaURI();
		
		prtln ("framework atttributes");
		prtln ("\t schemaTargetNameSpace: " + schemaTargetNameSpace);
		prtln ("\t schemaLocation: " + schemaLocation);
		prtln ("");
		try {
			String rawXml = Files.readFile (filePath).toString();

			rawXml = SchemEditUtils.expandAmpersands(rawXml);
			// prtln (rawXml);
			
			Document doc = Dom4jUtils.getXmlDocument(rawXml);
			// pp (doc);
			Element root = doc.getRootElement();
			if (!root.getName().equals (framework.getRootElementName())) {
				throw new Exception ("Root element (\"" + root.getName() + 
									 "\") does not match schema (expected \"" +
									 framework.getRootElementName() + "\")");
			}
			
			// schema location
			Attribute schemaLocAtt = root.attribute ("schemaLocation");
			Attribute noSchemaLocAtt = root.attribute ("noNamespaceSchemaLocation");
			String docTargetNameSpace = null;;
			String docSchemaLocation = null;
			
			if (schemaLocAtt != null) {
				docTargetNameSpace = schemaLocAtt.getText().split("\\s")[0];
				docSchemaLocation = schemaLocAtt.getText().split("\\s")[1];
			} else if (noSchemaLocAtt != null) {
				docSchemaLocation = noSchemaLocAtt.getText();
			} else {
				throw new Exception ("schemaLocation not specified in instance document");
			}

			// make sure docSchemaLocation is an absolute path
			URI uri = null;
			try {
				uri = new URI(docSchemaLocation);
			} catch (Exception e) {
				throw new Exception ("docSchemaLocation (" + docSchemaLocation + ") could not be parsed as a URI");
			}
			if (!uri.isAbsolute()) {
				docSchemaLocation = "file:" + GeneralServletTools.getAbsolutePath(docSchemaLocation, framework.getDocRoot());
			}
			
			prtln ("\n document atttributes");
			prtln ("\t docSchemaLocation: " + docSchemaLocation);
			prtln ("\t docTargetNameSpace: " + docTargetNameSpace);
			prtln ("");
			
			// if schemaTargetNameSpace is specified, then the document must declare a targetNameSpace
			if (schemaTargetNameSpace != null &&
				(docTargetNameSpace == null ||
				!docTargetNameSpace.equals(schemaTargetNameSpace))) {
				throw new Exception ("document target namespace does not match schema (" + schemaTargetNameSpace + ")");
			}
			
			if (enforceSchemaLocation &&
				!docSchemaLocation.equals(schemaLocation)) {
				throw new Exception ("schema location does not match metadata framework (" + schemaLocation + ")");
			}
			
			// doc = Dom4jUtils.localizeXml(doc, framework.getRootElementName());
			doc = Dom4jUtils.localizeXml(doc);
	
			pp (doc);
			if (doc == null)
				throw new Exception ("document could not be parsed");
			
			return doc;
/* 		} catch (IOException ioe) {
			throw new DocumentException (ioe.getMessage());
		} catch (DocumentException de) {
			throw new DocumentException (de.getMessage()); */
		} catch (Exception e) {
			e.printStackTrace();
			throw new DocumentException ("getEditableDocument ERROR processing " + filePath + ": " + e.getMessage());
		} catch (Throwable t) {
			t.printStackTrace();
			throw new DocumentException ("getEditableDocument: unknown error processing " + filePath);
		}
	}
	
	/**
	 *  The main program for the FrameworkTester class
	 *
	 *@param  args  The command line arguments
	 */
	public static void main(String[] args) throws Exception {
		TesterUtils.setSystemProps ();
		prtln("FrameworkTester\n");
		
		String format = "eng_path";  // dcs_data news_opps adn framework_config
		
		if (args.length > 0)
			format = args[0];

 		FrameworkTester tester = new FrameworkTester(format);
		MetaDataFramework framework = tester.getFramework();
		// tester.showSchemaPaths();
		
		UserInfo userInfo = framework.getUserInfo();
		if (userInfo != null) {
			prtln (userInfo.toString());
		}
		else
			prtln ("User Info is NULL");
		
		// tester.copyRecordTester();
		
		User user = new User();
		user.setEmail("foo@farb.com");
		
		Document doc = framework.makeMinimalRecord("fooberry", null, user);
		pp (doc);

		
	}
	
	void ensureMinimalDocumentTester() throws Exception {
		// String itemPath = "/Users/ostwald/devel/dcs-records/2009_06_10-records/smile_item9/1246053008647/S9-000-000-000-005.xml";
		// String itemPath = "/Users/ostwald/devel/dcs-records/2009_06_10-records/library_dc/1246122420417/LFOO-000-000-000-001.xml";
		String itemPath = "C:/Documents and Settings/ostwald/devel/dcs-instance-data/local-ndr/records/res_qual/1251311367520/RESQ-000-000-000-005.xml ";
		Document doc = Dom4jUtils.getXmlDocument(new File (itemPath));
		doc = Dom4jUtils.localizeXml(doc);
		doc = framework.preprocessEditableDocument(doc);
		/* EnsureMinimalDocument ensurer = new EnsureMinimalDocument (doc, tester.framework.getSchemaHelper());
		ensurer.process (doc, framework.getSchemaHelper()); */
		EnsureMinimalDocument.process (doc, framework.getSchemaHelper());
		// pp (doc);
		prtln ("----------------");
		prtln ("** InstanceDoc **\n" +Dom4jUtils.prettyPrint(doc));
		prtln ("----------------");
		
		this.addElement (doc);
	}
	
	void addElement (Document doc) throws Exception {
		SchemaHelper sh = this.framework.getSchemaHelper();
		DocMap docMap = new DocMap (doc, sh);
		Element e = sh.getNewElement("/record/contentAlignment/phenomenon");
		pp (e);
		e = sh.getNewElement("/record/contentAlignment/phenomenon/benchmark");
		pp (e);		
		// docMap.addElement();
	}
	
	void copyRecordTester () throws Exception {
		String copyrecordsamples = "C:/tmp/copyrecordsamples";
		String filename = framework.getXmlFormat() + ".xml";
		
		StringBuffer content = Files.readFile(new File (copyrecordsamples, filename));
		String id = "XXXXXXX";
		CollectionConfig collectionConfig = null;
		Document copyDoc = null;
		try {
			copyDoc = framework.copyRecord(content.toString(), id, collectionConfig, null);
		} catch (Throwable t) {
			t.printStackTrace();
			throw new Exception("populateFields caught error: " + t.getMessage());
		}
		// pp (copyDoc);
	}
	
/* 	Document ensureRequiredPaths (Document instanceDoc) {
		Element root = instanceDoc.getRootElement();
		ensureRequiredPaths (root);
		return instanceDoc;
	} */
	
	void addRequiredPaths (Element e) {
		String path = e.getPath();
		SchemaNode schemaNode = this.framework.getSchemaHelper().getSchemaNode (path);
		prtln (schemaNode.toString());
	}
	
/* 	private void ensureRequiredPaths(Element e) {
		// prtln ("SchemaHelper.pruneTree(" + e.getPath() + ")");

		SchemaNode schemaNode = getSchemaNode(e.getPath());

		// Ignore ABSTRACT ELEMENTS
		if (schemaNode == null) {
			String msg = "WARNING: ensureRequiredPaths could not find schemaNode for" + e.getPath();
			prtln(msg);
			return;
		}

		GlobalDef typeDef = schemaNode.getTypeDef();
		
		Compositor compositor = schemaNode.getCompositor();
			
		if (compositor != null) {
			List leafMemberNames = compositor.getLeafMemberNames();
			List children

		List attributes = e.attributes();
		for (int i = attributes.size() - 1; i > -1; i--) {
			Attribute attr = (Attribute) attributes.get(i);
			// String path = attr.getPath();
			String path = e.getPath() + "/@" + attr.getQualifiedName();
			SchemaNode attrNode = getSchemaNode(path);
			if (attrNode == null) {
				// prtln("pruneTree: schemaNode not found for attribute " + path);
				continue;
			}

			if (!attrNode.isRequired()) {
				// prtln("pruneTree: removing attribute " + attr.getName() + " from " + path);
				e.remove(attr);
			}
		}

		List children = e.elements();
		for (int i = children.size() - 1; i > -1; i--) {
			Element child = (Element) children.get(i);
			String path = child.getPath();
			SchemaNode childSchemaNode = getSchemaNode(path);
			if (childSchemaNode == null) {
				prtln("pruneTree: schemaNode not found for element at " + path);
				continue;
			}

			if (!childSchemaNode.isRequired()) {
				// prtln("removing " + childSchemaNode.getXpath());
				if (!e.remove(child)) {
					prtln("pruneTree: failed to remove element " + child.getName() + " from " + path);
				}
				continue;
			}
			if (isChoiceElement(childSchemaNode) || this.isAnyTypeElement(path)) {
				// prtln("removing choice element at " + path);
				if (!e.remove(child)) {
					prtln("pruneTree: failed to remove " + child.getName() + " from " + path);
				}
				continue;
			}
			else {
				pruneTree(child);
			}
		}
	} */
	
	void showSchemaPaths () {
		SchemaPathMap schemaPathMap = getFramework().getSchemaPathMap();
		prtln (schemaPathMap.toString());
	}
	

	void inOutTest (String format) {
		MetaDataFramework framework = getFramework();
		File testDir = new File ("/devel/ostwald/tmp/doctest");
		File in = null;
		if (format.equals("news_opps"))
			in = new File (testDir, "news-opps-test-record.xml");
		else if (format.equals("dcs_data"))
			in = new File (testDir, "dcs-data-test-record.xml");
		
		File out = new File (testDir, "out.xml");
		Document doc;
		String xmlString;
		prtln ("\n---------------------\n");
		try {

			doc = SchemEditUtils.getEditableDocument("file:" + in.getAbsolutePath(), framework);
			
			xmlString = framework.getWritableRecordXml(doc);
			Files.writeFile(xmlString, out);

			doc = framework.getEditableDocument(out.getAbsolutePath());
			pp (doc);
		} catch (Exception de) {
			prtln (de.getMessage());
			return;
		}
	}
	
	static void pp (Node node) {
		prtln (Dom4jUtils.prettyPrint(node));
	}
	
	/**
	 *  Description of the Method
	 *
	 *@param  s  Description of the Parameter
	 */
	public static void prtln(String s) {
		System.out.println(s);
	}
}

