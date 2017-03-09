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

import org.dlese.dpc.serviceclients.webclient.WebServiceClient;
import org.dlese.dpc.xml.*;
import org.dlese.dpc.xml.schema.*;
import org.dlese.dpc.xml.schema.compositor.*;
import org.dlese.dpc.schemedit.*;
import org.dlese.dpc.util.strings.*;

import java.io.*;
import java.util.*;
import java.text.*;
import java.util.regex.*;

import java.net.*;
import org.dom4j.Node;
import org.dom4j.Element;
import org.dom4j.Document;
import org.dom4j.DocumentFactory;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;

import com.sun.msv.datatype.xsd.*;
import org.relaxng.datatype.*;

/**
 *  Description of the Class
 *
 * @author    ostwald
 */
public class SchemaHelperTester {
	private WebServiceClient webServiceClient = null;
	private GlobalDefMap globalDefMap = null;
	private Document instanceDocument = null;
	private XSDatatypeManager xsdDatatypeManager = null;
	private SchemaNodeMap schemaNodeMap = null;
	private DefinitionMiner definitionMiner;
	private XMLWriter writer;
	private DocumentFactory df = DocumentFactory.getInstance();
	public SchemaHelper sh;
	String xmlFormat;


		/**
	 *  The main program for the SchemaHelperTester class
	 *
	 * @param  args  The command line arguments
	 */
	public static void main(String[] args) throws Exception {
		SchemaHelper.setVerbose(false);
		SchemaHelperTester t = null;
		DefinitionMiner.setDebug(false);
		SchemaReader.setDebug(false);
		String xmlFormat = "dlese_anno";
		if (args.length > 0)
			xmlFormat = args[0];
		try {
			t = new SchemaHelperTester(xmlFormat);
			if (t.sh == null)
				throw new Exception ("schemaHelper not instantiated");
		} catch (Exception e) {
			prtln ("ERROR: " + e.getMessage());
			return;
		}

		// t.msp2_tests ();
		t.dlese_anno_tests ();
		
		
		// String path = "/itemRecord/geospatialCoverages/geospatialCoverage/body";
		
		// t.showRepeatingComplexSingletons();
		// t.showNodesHavingRepeatingComplexSingletons();
		
		// if (t.sh.hasRepeatingComplexSingleton(path))
		// 	prtln ("YUP: " + path);
		
		// t.pathTester (path);
		
		// t.enumTester(path);
		
		// -- NOTE: most/all the following are now implemented in SchemaUtils!
		
		// t.showEnumerationTypes();
		// t.showXSDStringExtensionFields();
		// t.showComboUnionFields();
		// t.showRequiredBranches();
		// t.showRequiredContentElements();
		// t.showGlobalElements();
		
		// SchemaUtils.showDerivedDataTypes (t.sh);
		// SchemaUtils.showSimpleAndComplexContentElements (t.sh);
		// SchemaUtils.showDerivedContentModelElements (t.sh);
	
		// t.showIdRefTypes();

	}
	
	
	void msp2_tests ()  throws Exception {
		prtln ("msp2tests");
		String path = "/record/coverage/location_1_";
		path = XPathUtils.decodeXPath(path);
		prtln ("decoded path: " + path);
		path = XPathUtils.normalizeXPath(path);
		prtln ("normalized path: " + path);
		if (sh.getSchemaNode (path) == null)
			throw new Exception ("schemaNode not found for " + path);
		if (sh.isChoiceElement(path))
			prtln ("choice");
		if (sh.isMultiChoiceElement(path))
			prtln ("multichoice");		
		if (sh.isRepeatingElement(path))
			prtln ("repeating");
	}
	
	/*
	* encoded path, e.g., "/annotationRecord/annotation/contributors/contributor_1_"
	*/
	public void  dlese_anno_tests() {

	}
	
	void showIdRefTypes () {
		for (Iterator i=schemaNodeMap.getValues().iterator();i.hasNext();) {
			SchemaNode schemaNode = (SchemaNode)i.next();
 			if (schemaNode.getValidatingType().getName().equals("IdRefType"))
				prtln (schemaNode.getXpath());
			// prtln (schemaNode.getValidatingType().getName());
		}
	}
		
	void pathTester (String path) {
		
		prtln ("testing " + path);
		prtln ("\t repeating Element?");
		prtln ((sh.isRepeatingElement (path)) ? "\t\t YES" : "\t\t no");
		
		prtln ("\t is repeatingComplexSingleton??");
		prtln ((sh.isRepeatingComplexSingleton (path)) ? "\t\t YES" : "\t\t no");
	}
	
	/* three cases
	1 - simple Enumeration (GenericType.isEnumerationType()) -> genericType.getEnumerationValues
	2 - comboUnion Type (SimpleType.isComboUnionType() -> genericType.getEnumerationValues 
		(?will this work with comboUnions?)
	3 - derivedTextOnlyModel (that extends either an enumeration or comboUnion)
		ComplexType.isDerivedTextOnlyModel -> work with ComplexType.getExtensionType
	*/
	void enumTester (String path) {
		prtln ("Enum Tester with path: " + path);
		try {
			SchemaNode schemaNode = sh.getSchemaNode(path);
			if (schemaNode == null)
				throw new Exception ("schemaNode not found for " + path);
			
			GlobalDef globalDef = schemaNode.getTypeDef();
			if (globalDef == null)
				throw new Exception ("globalDef not found for " + path);
			
			// Case 3 - ComplexType && isDerivedTextOnlyModel
			
			if (globalDef.isComplexType()) {
				ComplexType complexType = (ComplexType) globalDef;
				if (!complexType.isDerivedTextOnlyModel())
					throw new Exception ("globalDef is complexType but not derivedTextOnlyModel - cannot define enumeration");
				globalDef = complexType.getExtensionType();
				prtln ("working with extension base: " + globalDef.getQualifiedInstanceName());
			}
			
			if (!globalDef.isTypeDef())
				throw new Exception ("globalDef is NOT typeDef (" + globalDef.getQualifiedInstanceName());
			
			GenericType typeDef = (GenericType) globalDef;
			
			if (!globalDef.isSimpleType())
				// now we need to test for a model that EXTENDS a simpleType ...
				throw new Exception ("globalDef is NOT simpleType (" + globalDef.getQualifiedInstanceName());
			
			SimpleType simpleType = (SimpleType) globalDef;
			
			prtln (typeDef.toString());
			
			if (! (simpleType.isEnumeration() || simpleType.isComboUnionType()))
				throw new Exception ("NOT an enumeration or comboUnion");
			else {
				if (simpleType.isEnumeration())
					prtln (" ... Enumeration");
				if (simpleType.isComboUnionType())
					prtln (" ... ComboUnionType");
			}
				
			prtln ("calling getEnumerationValues");
			List terms = ((GenericType)typeDef).getEnumerationValues();
			prtln ("Enumeration values");
			for (Iterator i=terms.iterator();i.hasNext();) {
				prtln ("\t" + (String)i.next());
			}
		} catch (Exception e) {
			prtln ("enumTester: " + e.getMessage());
		}
	}
	
	/**
	 *  Constructor for the SchemaHelperTester object
	 */
	public SchemaHelperTester(String xmlFormat) throws Exception {
		this.xmlFormat = xmlFormat;
		SchemaRegistry sr = new SchemaRegistry();
		
		SchemaRegistry.Schema schema = (SchemaRegistry.Schema)sr.getSchema(xmlFormat);
		if (schema == null) {
			throw new Exception ("Schema not found for \"" + xmlFormat + "\"");
		}
		
		String path = schema.path;
		String rootElementName = schema.rootElementName;
		SimpleSchemaHelperTester t = null;
		try {
			if (path.indexOf ("http:") == 0) {
				URL	schemaUrl = new URL (path);
				sh = new SchemaHelper(schemaUrl, rootElementName);
				// sh = new SchemaHelper(schemaUrl);
			}
			else {
				prtln ("path: " + path);
				sh = new SchemaHelper (new File (path), rootElementName);
			}
			
			if (sh == null) {
				throw new Exception ("\n\n ** schemaHelper not instantiated **");
			}
			else {
				prtln("SchemaHelper instantiated");
			}
			schemaNodeMap = sh.getSchemaNodeMap();
			globalDefMap = sh.getGlobalDefMap();
			instanceDocument = sh.getInstanceDocument();
			writer = Dom4jUtils.getXMLWriter();
	
			xsdDatatypeManager = sh.getXSDatatypeManager();
			
		} catch (Exception e) {
			prtln ("failed to instantiate SimpleSchemaHelperTester: " + e.getMessage());
			e.printStackTrace();
			System.exit(1);
		}
	}

	public void stuffValue (DocMap docMap, String xpath, String value, SchemaHelper schemaHelper) throws Exception {
		
		if (schemaHelper.getSchemaNode(xpath) == null)
			throw new Exception ("stuffValue got an illegal xpath: " + xpath);
		
		Node node = docMap.selectSingleNode (xpath);
		if (node == null)
			node = docMap.createNewNode(xpath);
		if (node == null)
			throw new Exception ("node not found for " + xpath);
		else
			node.setText(value);
	}
		
	
	/**
	 *  Description of the Method
	 *
	 * @param  test  Description of the Parameter
	 */
	public static void charTest(String test) {
		// String test = "ABCD";
		for (int i = 0; i < test.length(); ++i) {
			char c = test.charAt(i);
			int j = (int) c;
			System.out.println(j);
		}
	}

	/**
	 *  Description of the Method
	 */
	public static void charTest() {
		for (int i = 0; i < 128; i++) {
			char c = (char) i;
			prtln(c + ": " + (int) c);
		}

	}


	/**
	 *  Description of the Method
	 *
	 * @param  uriStr  Description of the Parameter
	 */
	public static void uriTest(String uriStr) {
		URI uri = null;
		try {
			uri = new URI(uriStr);
		} catch (Exception e) {
			prtln(e.getMessage());
			return;
		}
		// t = new SchemaHelperTester(uri);
		prtln("URI: " + uri.toString());
		String scheme = uri.getScheme();
		prtln("Scheme: " + scheme);
		prtln("isAbsolute: " + uri.isAbsolute());
		String path = uri.getPath();
		if (path != null) {
			prtln("path: " + path);
		}
		else {
			prtln("path is null");
		}

		if (scheme == null) {
			prtln("scheme is null");
		}
		else if (scheme.equals("file")) {
			prtln("scheme is file");
			try {
				File file = new File(path);
				if (file.exists()) {
					prtln("file exists at " + path);
				}
				else {
					prtln("file doesn't exist at " + path);
				}
			} catch (Exception e) {
				prtln(e.getMessage());
			}
		}
		else if (scheme.equals("http")) {
			prtln("scheme is http");
			URL url = null;
			try {
				url = uri.toURL();
				prtln("made a url!");
			} catch (Exception e) {
				prtln("failed to form URL: " + e.getMessage());
				return;
			}
			prtln("url: " + url.toString());
		}
		else {
			prtln("unrecognized scheme: " + scheme);
		}
	}


	/**
	 *  Print a listing of the globalDefs that are used more than once in the
	 *  InstanceDoc
	 */
	private void displayTypeUsers() {
		TreeMap map = new TreeMap();
		List paths = schemaNodeMap.getKeys();
		for (Iterator i = paths.iterator(); i.hasNext(); ) {
			String xpath = (String) i.next();
			SchemaNode schemaNode = (SchemaNode) schemaNodeMap.getValue(xpath);
			// String typeName = schemaNode.getDataTypeName();
			String typeName = schemaNode.getTypeDef().getName();
			GlobalDef globalDef = (GlobalDef) globalDefMap.getValue(typeName);
			if (globalDef == null) {
				String schemaNSPrefix = this.sh.getSchemaNamespace().getPrefix();
				if (!typeName.startsWith(schemaNSPrefix + ":")) {
					prtln(typeName + " - not found in globalDefMap");
				}
				continue;
			}
			if (globalDef.getDataType() == GlobalDef.COMPLEX_TYPE) {
				// prtln (globalDef.getName());
				if (map.containsKey(typeName)) {
					ArrayList l = (ArrayList) map.get(typeName);
					l.add(xpath);
					map.put(typeName, l);
				}
				else {
					ArrayList a = new ArrayList();
					a.add(xpath);
					map.put(typeName, a);
				}
			}
		}
		Set keys = map.keySet();
		for (Iterator i = keys.iterator(); i.hasNext(); ) {
			String key = (String) i.next();
			List list = (List) map.get(key);
			if (list.size() > 1) {
				prtln("\n" + key + "(" + list.size() + " items)");
				for (Iterator x = list.iterator(); x.hasNext(); ) {
					String path = (String) x.next();
					prtln("\t" + path);
				}
			}
		}
	}


	/**
	 *  Description of the Method
	 */
	private void displayComplexTypes() {
		// List keys = globalDefMap.getKeys(GlobalDef.COMPLEX_TYPE);
		List complexTypes = globalDefMap.getComplexTypes();
		prtln("\n** Complex types (" + complexTypes.size() + ") **");
		for (Iterator i = complexTypes.iterator(); i.hasNext(); ) {
			GlobalDef def = (GlobalDef) i.next();
			prtln(def.toString());
			try {
				write(def.getElement());
			} catch (Exception e) {
				prtln(e.getMessage());
			}
		}
	}


	/**
	 *  Find the dataTypes of elements in the SchemaNodeMap that are not present in
	 *  the XSDatatypeManager. We are only interested in the SimpleTypes, since
	 *  complexTypes are not handled by the XSDatatypeManager.
	 */
	public void reportMissingXSDatatypes() {
		List keys = schemaNodeMap.getKeys();
		List mia = new ArrayList();
		for (Iterator i = keys.iterator(); i.hasNext(); ) {
			String key = (String) i.next();
			SchemaNode schemaNode = (SchemaNode) schemaNodeMap.getValue(key);

			// String typeName = schemaNode.getDataTypeName();
			String typeName = schemaNode.getTypeDef().getName();
			if (globalDefMap.containsKey(typeName)) {
				GlobalDef def = (GlobalDef) globalDefMap.getValue(typeName);
				if (def.getDataType() == GlobalDef.COMPLEX_TYPE) {
					continue;
				}
				XSDatatype dt = xsdDatatypeManager.getTypeByName(typeName);
				// add to the missing in action (mia) map
				if (dt == null) {
					if (!mia.contains(typeName)) {
						mia.add(typeName);
					}
				}
			}
			else {
				prtln("WARNING: " + typeName + " not found in globalDefMap");
			}
		}

		prtln("missing (Simple and built-in) XSDatatypes");
		for (Iterator i = mia.iterator(); i.hasNext(); ) {
			String typeName = (String) i.next();
			if (globalDefMap.containsKey(typeName)) {
				GlobalDef def = (GlobalDef) globalDefMap.getValue(typeName);

				String dataType = String.valueOf(def.getDataType());
				prtln(typeName + " IS in globalDefMap");
			}
			else {
				prtln(typeName + " is NOT in globalDefMap");
			}
		}
	}


	/**
	 *  Displays specific info about all the SchemaNodeMap schemaNodes that wrap
	 *  attribute elements from the schema
	 */
	public void displayAttributes() {
		prtln("Attributes from the SchemaNodeMap");
		List attrXPaths = schemaNodeMap.getKeys(Node.ATTRIBUTE_NODE);
		for (Iterator i = attrXPaths.iterator(); i.hasNext(); ) {
			String xpath = (String) i.next();
			SchemaNode schemaNode = (SchemaNode) schemaNodeMap.getValue(xpath);
			String use = schemaNode.getAttr("use");
			// prtln("\n" + xpath + "\n\tdataType: " + schemaNode.getDataTypeName() + "\n\tuse: " + use);
			prtln("\n" + xpath + "\n\tdataType: " + schemaNode.getTypeDef().getName() + "\n\tuse: " + use);
		}
	}


	/**
	 *  Gets the compositors attribute of the SchemaHelperTester object
	 *
	 * @param  filter  Description of the Parameter
	 */
	public void displayCompositors(String filter) {
		// List complexTypeNames = globalDefMap.getKeys(GlobalDef.COMPLEX_TYPE);
		List complexTypes = globalDefMap.getComplexTypes();
		prtln("Compositors for ComplexTypes");
		for (Iterator i = complexTypes.iterator(); i.hasNext(); ) {
			ComplexType def = (ComplexType) i.next();
			String typeName = def.getName();
			Compositor compositor = def.getCompositor();
			if (compositor == null)
				continue;
			String compositorName = compositor.getName();
			if (compositorName.equals(filter) || filter.equals("*")) {
				prtln("----------\n" + typeName + ": " + compositorName);
				// prtln("\t" + def.getLocation());
				// write(def.getElement());
				List choices = def.getChoices();
				for (Iterator c = choices.iterator(); c.hasNext(); ) {
					prtln((String) c.next());
				}
				prtln("");
			}
		}
	}


	/**
	 *  Description of the Method
	 *
	 * @param  o  Description of the Parameter
	 */
	private void write(Object o) {
		try {
			writer.write(o);
			prtln("");
		} catch (Exception e) {
			prtln("couldn write");
		}
	}


	/**
	 *  Description of the Method
	 *
	 * @param  node  Description of the Parameter
	 */
	private static void pp(Node node) {
		prtln(Dom4jUtils.prettyPrint(node));
	}


	/**
	 *  Description of the Method
	 *
	 * @param  s  Description of the Parameter
	 */
	private static void prtln(String s) {
		// System.out.prtlnln("SchemaHelperTester: " + s);
		System.out.println(s);
	}
}

