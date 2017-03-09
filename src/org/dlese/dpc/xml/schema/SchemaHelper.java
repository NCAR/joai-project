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
package org.dlese.dpc.xml.schema;

import org.dlese.dpc.xml.*;
import org.dlese.dpc.xml.schema.compositor.*;

import java.io.*;
import java.util.*;

import java.net.*;
import org.dom4j.Node;
import org.dom4j.Attribute;
import org.dom4j.Element;
import org.dom4j.Document;
import org.dom4j.Namespace;
import org.dom4j.DocumentException;
import org.dom4j.QName;
import org.dom4j.io.SAXReader;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;

import com.sun.msv.datatype.xsd.*;

/**
 *  Class encapsluting semantics of XML Schemas.<p>
 *
 *  Aggregates tools and data structures for working with XML Schemas, and is in
 *  turn encapsulated in the {@link org.dlese.dpc.schemedit.MetaDataFramework}
 *  object. Major components include
 *  <ul>
 *    <li> instanceDocument - an xml document representing a "normalized" schema
 *    (i.e., in which all elements are defined in-line rather than as
 *    dataTypes). An schema instance document is x-path equivalent to a valid
 *    xml document for the schema, but contains no values.</li>
 *    <li> {@link org.dlese.dpc.xml.schema.XSDatatypeManager} for performing
 *    iterative validation of xml documents using datatypes defined in the
 *    Schema.</li>
 *    <li> {@link org.dlese.dpc.xml.schema.DefinitionMiner} which traverses an
 *    XML Schema and builds a dictionary of {@link org.dlese.dpc.xml.schema.GlobalElement}
 *    objects, one for each element defined in the Schema.</li>
 *    <li> {@link org.dlese.dpc.xml.schema.SchemaNodeMap} that represents the
 *    structure defined by the Schema as a tree of {@link org.dlese.dpc.xml.schema.SchemaNode}
 *    objects.</li>
 *  </ul>
 *  The SchemaHelper constructors take either a URI or File and builds these
 *  structures as it traverseses the schema definition, which may consist of
 *  several files.<p>
 *
 *  NOTE: The SchemaHelper assumes that XML Schemas are represented using the
 *  "Venetian Blind" technique.
 *
 * @author     ostwald
 */
public class SchemaHelper {

	private static boolean debug = false;
	private static boolean verbose = false;

	static short ATTRIBUTE_NODE = Node.ATTRIBUTE_NODE;
	static short ELEMENT_NODE = Node.ELEMENT_NODE;

	/**  NOT YET DOCUMENTED */
	public final static int MINOCCURS_DEFAULT = 1;
	/**  NOT YET DOCUMENTED */
	public final static int MAXOCCURS_DEFAULT = 1;
	/**  NOT YET DOCUMENTED */
	public final static String NILLABLE_DEFAULT = "false";
	/**  NOT YET DOCUMENTED */
	public final static int UNBOUNDED = Integer.MAX_VALUE;

	private GlobalDefMap globalDefMap = null;
	private Document instanceDocument = null;
	private Document minimalDocument = null;
	private XSDatatypeManager xsdDatatypeManager = null;
	private SchemaNodeMap schemaNodeMap = null;
	private DefinitionMiner definitionMiner;
	private Element schemaDocRoot;

	private URI rootURI = null;
	private boolean namespaceEnabled = false;

	private SchemaProps schemaProps = null;

	private String version;
	private String targetNamespace;
	private String elementFormDefault = "qualified";
	private String attributeFormDefault = "unqualified";

	private Log log = new Log();


	/**
	 *  Constructor for the SchemaHelper object
	 *
	 * @param  schemaFile                 NOT YET DOCUMENTED
	 * @exception  SchemaHelperException  NOT YET DOCUMENTED
	 */
	public SchemaHelper(File schemaFile) throws SchemaHelperException {
		this(schemaFile, null);
	}


	/**
	 *  Constructor for the SchemaHelper object for disk-based schema
	 *
	 * @param  schemaFile                 path to root file of schema
	 * @param  rootElementName            NOT YET DOCUMENTED
	 * @exception  SchemaHelperException  Description of the Exception
	 */
	public SchemaHelper(File schemaFile, String rootElementName)
		 throws SchemaHelperException {
		rootURI = schemaFile.toURI();
		init(rootElementName);
	}


	/**
	 *  Constructor for the SchemaHelper object
	 *
	 * @param  schemaURL                  NOT YET DOCUMENTED
	 * @exception  SchemaHelperException  NOT YET DOCUMENTED
	 */
	public SchemaHelper(URL schemaURL) throws SchemaHelperException {
		this(schemaURL, null);
	}


	/**
	 *  Constructor for the SchemaHelper object using web-based schema.
	 *
	 * @param  schemaURL                  URL to root file of schema
	 * @param  rootElementName            NOT YET DOCUMENTED
	 * @exception  SchemaHelperException  Description of the Exception
	 */
	public SchemaHelper(URL schemaURL, String rootElementName)
		 throws SchemaHelperException {

		if (schemaURL != null) {
			try {
				rootURI = schemaURL.toURI();
			} catch (URISyntaxException e) {
				throw new SchemaHelperException("could not construct URI from URL (" + schemaURL + "):" + e.getMessage());
			}
		}
		init(rootElementName);
	}


	/**
	 *  Initialize the SchemaHelper. Note, the order in which the fields of
	 *  SchemaHelper are populated is important: the DefinitionMiner must
	 *  instantiated before the XSDatatypeManger, which must be instantiated before
	 *  the StructureWalker.
	 *
	 * @param  rootElementName            NOT YET DOCUMENTED
	 * @exception  SchemaHelperException  Description of the Exception
	 */
	private void init(String rootElementName)
		 throws SchemaHelperException {

		schemaProps = new SchemaProps(rootURI);
		definitionMiner = new DefinitionMiner(rootURI, rootElementName, log);

		globalDefMap = definitionMiner.getGlobalDefMap();

		if (verbose)
			SchemaUtils.showGlobalDefs(this);
		if (verbose)
			SchemaUtils.showSubstitutionGroups(this);

		// schemaRootElement is the root of the instance Doc??
		Element schemaRootElement = definitionMiner.getSchemaRootElement();

		schemaProps.init(definitionMiner.getRootDoc());
		if (verbose)
			prtln("\nInitializing xsdDatatypeManager\n");
		xsdDatatypeManager = new XSDatatypeManager(globalDefMap);
		if (verbose)
			prtln("xsdDatatypeManager instantiated\n");

		// if (verbose) showDataTypes ();

		if (verbose)
			prtln("\nInitializing structureWalker\n");
		// StructureWalker structureWalker = new StructureWalker(schemaRootElement, globalDefMap, rootURI.toString());
		StructureWalker structureWalker = new StructureWalker(schemaRootElement, this);
		namespaceEnabled = structureWalker.getNamespaceEnabled();
		if (verbose)
			prtln("structureWalker instantiated\n");
		instanceDocument = structureWalker.instanceDocument;

		String instanceRootName = instanceDocument.getRootElement().getQualifiedName();
 		if (rootElementName != null && !instanceRootName.equals(rootElementName)) {
			throw new SchemaHelperException("provided rootElementName (" + rootElementName +
				") does not match found rootElementName (" + instanceRootName + ")");
		}

		// stash the instanceDoc rootElement name in schemaProps
		schemaProps.setProp("rootElementName", instanceDocument.getRootElement().getQualifiedName());
		schemaNodeMap = structureWalker.schemaNodeMap;
		if (verbose)
			SchemaUtils.showSchemaNodeMap(this);

		// if (verbose) showInstanceDoc();

	}


	/**
	 *  Gets the namespaceEnabled attribute of the SchemaHelper object
	 *
	 * @return    The namespaceEnabled value
	 */
	public boolean getNamespaceEnabled() {
		return namespaceEnabled;
	}


	/**
	 *  Gets the schemaProps attribute of the SchemaHelper object
	 *
	 * @return    The schemaProps value
	 */
	public SchemaProps getSchemaProps() {
		return schemaProps;
	}


	/**
	 *  Gets the rootURI attribute of the SchemaHelper object
	 *
	 * @return    The rootURI value
	 */
	public String getSchemaLocation() {
		// return rootURI;
		return (String) schemaProps.getProp("schemaLocation");
	}

	public Namespace getSchemaNamespace () {
		return this.definitionMiner.getNamespaces().getSchemaNamespace();
	}

	/**
	 *  Gets the version attribute of the SchemaHelper object
	 *
	 * @return    The version value
	 */
	public String getVersion() {
		// return version;
		return (String) schemaProps.getProp("version");
	}


	/**
	 *  Gets the rootElementName attribute of the SchemaHelper object
	 *
	 * @return    The rootElementName value
	 */
	public String getRootElementName() {
		return (String) schemaProps.getProp("rootElementName");
	}


	/**
	 *  Gets the targetNamespace attribute of the SchemaHelper object
	 *
	 * @return    The targetNamespace value
	 */
	public String getTargetNamespace() {
		// return targetNamespace;
		return (String) schemaProps.getProp("targetNamespace");
	}


	/**
	 *  Gets a globalDef from the default namespace of the globalDefMap.
	 *
	 * @param  typeName  the name of the globalDef object
	 * @return           a {@link GlobalDef} object, or null if not found.
	 */
	public GlobalDef getGlobalDef(String typeName) {
		// prtln ("getGlobalDef (" + typeName + ")");
		return (GlobalDef) globalDefMap.getValue(typeName);
	}


	/**
	 *  Gets a globalDef from the globalDefMap.
	 *
	 * @param  schemaNode  a {@link org.dlese.dpc.xml.schema.SchemaNode} object
	 * @return             a {@link org.dlese.dpc.xml.schema.GlobalDef} object, or
	 *      null if not found.
	 */
	public GlobalDef getGlobalDef(SchemaNode schemaNode) {
		/* 		String typeName = schemaNode.getDataTypeName();
		return getGlobalDef(typeName); */
		return schemaNode.getTypeDef();
	}


	/**
	 *  Gets the compositor attribute of the SchemaHelper object
	 *
	 * @param  xpath  NOT YET DOCUMENTED
	 * @return        The compositor value
	 */
	public Compositor getCompositor(String xpath) {
		String schemaPath = toSchemaPath(xpath);
		SchemaNode schemaNode = getSchemaNode(schemaPath);
		if (schemaNode == null) {
			prtln("getChoiceCompositor(): schemaNode not found for " + schemaPath);
			return null;
		}
		return getCompositor(schemaNode);
	}


	/**
	 *  Gets the compositor attribute of the SchemaHelper object
	 *
	 * @param  schemaNode  NOT YET DOCUMENTED
	 * @return             The compositor value
	 */
	public Compositor getCompositor(SchemaNode schemaNode) {
		Compositor compositor = null;
		try {
			GlobalDef typeDef = getGlobalDef(schemaNode);
			if (typeDef != null && typeDef.isComplexType()) {
				compositor = ((ComplexType) typeDef).getCompositor();
			}
		} catch (NullPointerException np) {
			prtln("getCompositor(): " + np.getMessage());
			np.printStackTrace();
		} catch (Exception e) {
			prtln("getCompositor(): " + e.getMessage());
		}
		return compositor;
	}


	/**
	 *  Gets a {@link org.dom4j.Node} from the instanceDocument
	 *
	 * @param  xpath  xpath specifying the desired node
	 * @return        The instanceDocument Node (or null if Node is not found)
	 */
	public Node getInstanceDocNode(String xpath) {
		// xpath may be decoded, and it must be normalized before accessing instanceDoc!
		String normalizedPath = XPathUtils.normalizeXPath(XPathUtils.decodeXPath(xpath));
		try {
			return instanceDocument.selectSingleNode(normalizedPath);
		} catch (org.dom4j.InvalidXPathException e) {
			prtln("getInstanceDocNode error: " + e.getMessage());
			return null;
		}
	}


	/**
	 *  Gets the instanceDocNodes attribute of the SchemaHelper object
	 *
	 * @param  xpath  NOT YET DOCUMENTED
	 * @return        The instanceDocNodes value
	 */
	public List getInstanceDocNodes(String xpath) {
		// xpath may be decoded, and it must be normalized before accessing instanceDoc!
		String normalizedPath = XPathUtils.normalizeXPath(XPathUtils.decodeXPath(xpath));
		try {
			return instanceDocument.selectNodes(normalizedPath);
		} catch (org.dom4j.InvalidXPathException e) {
			prtln("getInstanceDocNode error: " + e.getMessage());
			return null;
		}
	}


	/**
	 *  Gets a schemaNode from the schemaNodeMap. Note: the schemaNodeMap is
	 *  indexed by "normalized" xpaths (i.e., having no indexing). Since
	 *  normalizing is idempotent, perhaps we normalize the path before accessing
	 *  the map . . .
	 *
	 * @param  xpath  A string representation of an XPath
	 * @return        {@link SchemaNode} object , or null if not found
	 */
	public SchemaNode getSchemaNode(String xpath) {
		// prtln ("getSchemaNode with xpath: " + xpath);
		String normXpath = XPathUtils.normalizeXPath(xpath);
		return (SchemaNode) schemaNodeMap.getValue(normXpath);
	}


	/**
	 *  set the isReadOnly attribute of the schemaNode specified by the given xpath
	 *  to true
	 *
	 * @param  xpath  The new schemaNodeReadOnly value
	 */
	public void setSchemaNodeReadOnly(String xpath) {
		SchemaNode schemaNode = getSchemaNode(xpath);
		if (schemaNode != null) {
			schemaNode.setReadOnly(true);
		}
		else {
			prtln("setSchemaNodeReadOnly did not find schemaNode for " + xpath);
		}
	}


	/**
	 *  Gets the parent of the SchemaNode associated with a given xpath
	 *
	 * @param  xpath  A string representation of an XPath
	 * @return        The parent SchemaNode
	 */
	public SchemaNode getParentSchemaNode(String xpath) {
		String parentPath = XPathUtils.getParentXPath(xpath);
		return getSchemaNode(parentPath);
	}


	/**
	 *  Gets the parent of a SchemaNode
	 *
	 * @param  schemaNode  a {@link SchemaNode}
	 * @return             The parent SchemaNode
	 */
	public SchemaNode getParentSchemaNode(SchemaNode schemaNode) {
		return getParentSchemaNode(schemaNode.getXpath());
	}


	/**
	 *  The main program for the SchemaHelper class
	 *
	 * @param  args  The command line arguments
	 */
	public static void main(String[] args) {
		String webServiceBaseUrl = "http://www.dlese.org/dds/services";
		String schemaFilePath = "/export/devel/ostwald/metadata-frameworks/ADN-v0.6.50/record.xsd";
		File schemaFile = new File(schemaFilePath);
		try {
			SchemaHelper sh = new SchemaHelper(schemaFile);
		} catch (Throwable e) {
			prtln("SchemaHelper caught an error: " + e);
		}
	}


	/**
	 *  Get a named {@link XSDatatype} object used to validate input. Used only by
	 *  EditorAction in the "suggest" package org.dlese.dpc.to validate the email
	 *  field. this is necessary because the email field is not really specified as
	 *  being of this type and therefore another way is necessary for finding a
	 *  validating type.
	 *
	 * @param  dataTypeName  The name of a {@link XSDatatype} object
	 * @return               The xSDataType object
	 */
	public XSDatatype getXSDataType(String dataTypeName) {
		return xsdDatatypeManager.getTypeByName(dataTypeName);
	}


	/**
	 *  Gets the xsdDatatypeManager attribute of the SchemaHelper object.
	 *
	 * @return    The xSDatatypeManager value
	 */
	public XSDatatypeManager getXSDatatypeManager() {
		return xsdDatatypeManager;
	}


	/**
	 *  Gets the globalDefMap attribute of the SchemaHelper object. Currently only
	 *  used by SchemaHelperTester and therefore can be eliminated when not
	 *  debugging.
	 *
	 * @return    The globalDefMap value
	 */
	public GlobalDefMap getGlobalDefMap() {
		return globalDefMap;
	}


	/**
	 *  Gets the instanceDocument attribute of the SchemaHelper object
	 *
	 * @return    The instanceDocument value
	 */
	public Document getInstanceDocument() {
		return instanceDocument;
	}


	/**
	 *  Gets the definitionMiner attribute of the SchemaHelper object
	 *
	 * @return    The definitionMiner value
	 */
	public DefinitionMiner getDefinitionMiner() {
		return definitionMiner;
	}


	/**
	 *  Checks to see if a value is valid against a specified datatype. The
	 *  paramName is from a request and has the form of "valueOf(/itemRecord/education/blah_1_/@url)".
	 *  The paraName is used to obtain a XSDatatype.
	 *
	 * @param  value          Value to be validated
	 * @param  typeName       Name of the DataType to be validated against
	 * @return                true if value is valid
	 * @exception  Exception  If the value is not valid. The exception usually
	 *      contains helpful information
	 */
	public boolean checkValidValue(String typeName, String value)
		 throws Exception {
		return xsdDatatypeManager.checkValid(typeName, value);
	}


	/**
	 *  Gets the schemaNodeMap attribute of the SchemaHelper object
	 *
	 * @return    The schemaNodeMap value
	 */
	public SchemaNodeMap getSchemaNodeMap() {
		return schemaNodeMap;
	}


	/**
	 *  convert a jsp-encoded path into a normalized form for accessing schemaNodes
	 *
	 * @param  encodedXPath  Description of the Parameter
	 * @return               Description of the Return Value
	 */
	public static String toSchemaPath(String encodedXPath) {
		return XPathUtils.normalizeXPath(XPathUtils.decodeXPath(encodedXPath));
	}


	/**
	 *  Gets the repeatingElement attribute of the SchemaHelper object
	 *
	 * @param  encodedXPath  Description of the Parameter
	 * @return               The repeatingElement value
	 */
	public boolean isRepeatingElement(String encodedXPath) {
		String schemaPath = toSchemaPath(encodedXPath);
		// prtln ("isRepeatingElement with schemaPath: " + schemaPath);
		SchemaNode schemaNode = getSchemaNode(schemaPath);
		if (schemaNode == null) {
			prtln("isRepeatingElement() could not find a schemaNode for " + schemaPath);
			return false;
		}
		return (isRepeatingElement(schemaNode));
	}


	/**
	 *  Gets the repeatingElement attribute of the SchemaHelper object
	 *
	 * @param  schemaNode  Description of the Parameter
	 * @return             The repeatingElement value
	 */
	public boolean isRepeatingElement(SchemaNode schemaNode) {
		return (isRepeatingElement(schemaNode, schemaNode.getTypeDef()));
	}


	/**
	 *  Determines whether an instanceDocumentNode is a repeating element (i.e., it
	 *  has maxOccurs > 1 and is NOT an enumerationType). Note: we pass in the
	 *  dataTypeName explicitly, rather than obtain it from the schemaNode, because
	 *  sometimes the Renderer is asked to render a particular schemaNode as
	 *  specific type (e.g., in the case of derived Content Models) rather than the
	 *  type it is declared as in the typeDefinition.
	 *
	 * @param  schemaNode  a wrapper to a Node in the instanceDocument
	 * @param  typeDef     a DataType to be tested for isEumeration
	 * @return             The repeatingElement value
	 */
	// QUESTION are repeating elements always complexTypes??

	public boolean isRepeatingElement(SchemaNode schemaNode, GlobalDef typeDef) {
		// prtln ("isRepeatingElement with schemaNode and typeDef (" + typeDef.getQualifiedName() + ")");


		/* 	derivedModels are processed first as their native typeDef, and then again as
			the derivation base typeDef. on this later pass, we don't want to process as
			repeatingElement, so we return false
		*/
		if (schemaNode.isDerivedModel() && schemaNode.getTypeDef() != typeDef) {
			// prtln (" ... schemaNode.getTypeDef() != typeDef for " + schemaNode.getXpath());
			return false;
		}

		if (schemaNode.getMaxOccurs() > 1 && !isEnumerationType(typeDef))
			return true;

		// treat schemaNodes that are repeating compositor singletons as repeating elements ...
		if (schemaNode.isCompositorSingleton() && schemaNode.getParentCompositorMaxOccurs() > 1)
			return true;

		// prtln (" ... returning false");
		return false;
	}

	public boolean isRecursiveElement(String encodedXPath) {
		String schemaPath = toSchemaPath(encodedXPath);
		// prtln ("isRecursiveElement with schemaPath: " + schemaPath);
		SchemaNode schemaNode = getSchemaNode(schemaPath);
		if (schemaNode == null) {
			prtln("isRecursiveElement() could not find a schemaNode for " + schemaPath);
			return false;
		}
		return (schemaNode.isRecursive());
	}

	/**
	 *  Gets the singleton attribute of the SchemaHelper object
	 *
	 * @param  xpath  NOT YET DOCUMENTED
	 * @return        The singleton value
	 */
	public boolean isSingleton(String xpath) {
		SchemaNode schemaNode = this.getSchemaNode(xpath);
		return (schemaNode != null ? schemaNode.isCompositorSingleton() : false);
	}


	/**
	 *  Returns true if this schemaNode is the only child of it's parent.
	 *
	 * @param  schemaNode  NOT YET DOCUMENTED
	 * @return             The singleton value
	 */
	public boolean isSingleton(SchemaNode schemaNode) {
		return schemaNode.isCompositorSingleton();
	}


	/**
	 *  Returns true if the provided path meets the requirememts for a repeating
	 *  complex singleton element. Requirements are:
	 *  <ul>
	 *    <li> a ComplexType element
	 *    <li> has a maxOccurs of more than 1
	 *    <li> is the only child of it's parent element
	 *  </ul>
	 *
	 *
	 * @param  xpath  NOT YET DOCUMENTED
	 * @return        The repeatingComplexSingleton value
	 */
	public boolean isRepeatingComplexSingleton(String xpath) {
		return hasRepeatingComplexSingleton(XPathUtils.getParentXPath(xpath));
	}


	/**
	 *  Returns true if the node at the provided path contains a Single, Complex
	 *  element with maxOccurs of more than 1.
	 *
	 * @param  xpath  NOT YET DOCUMENTED
	 * @return        NOT YET DOCUMENTED
	 */
	public boolean hasRepeatingComplexSingleton(String xpath) {
		String singletonChildName = getRepeatingComplexSingletonChildName(xpath);
		return (singletonChildName != null && singletonChildName.trim().length() > 0);
	}


	/**
	 *  Gets the qualified element name of the repeatingComplexSingleton child of
	 *  the node specified by the provided path, or an empty string if such a child
	 *  does not exist.
	 *
	 * @param  xpath  NOT YET DOCUMENTED
	 * @return        The repeatingComplexSingletonChildName value
	 */
	public String getRepeatingComplexSingletonChildName(String xpath) {
		String normalizedXPath = XPathUtils.normalizeXPath(xpath);
		Node instanceDocNode = getInstanceDocNode(normalizedXPath);
		if (instanceDocNode != null && instanceDocNode.getNodeType() == org.dom4j.Node.ELEMENT_NODE) {
			List children = ((Element) instanceDocNode).elements();
			if (children.size() == 1) {
				Element childElement = (Element) children.get(0);
				String childPath = childElement.getPath();
				SchemaNode schemaNode = getSchemaNode(childPath);
				if (schemaNode != null && schemaNode.getTypeDef().isComplexType() && isRepeatingElement(childPath))
					return childElement.getQualifiedName();
			}
		}
		return "";
	}

	public boolean isAnyTypeElement (String xpath) {
		SchemaNode schemaNode = this.getSchemaNode(toSchemaPath(xpath));
		try {
			if (schemaNode != null) {
				GlobalDef typeDef = schemaNode.getTypeDef();
				if (typeDef != null)
					return typeDef.isAnyType();
				else
					throw new Exception ("typeDef not found");
			}
			else
				throw new Exception ("schemaNode not found");
		} catch (Exception e) {
			/* only print message for debugging. many xpaths will fail here, since 
			this predicate is called for many paths that don't refer to an actual node */
			// prtln ("isAnyTypeElement (" + xpath + "): " + e.getMessage());
		}
		return false;
	}

	/**
	 *  Returns true if the given path corresponds to a schemaNode that allows for
	 *  multiple choice children elements.
	 *
	 * @param  encodedXPath  xpath corresponding to a schemaNode to be tested
	 * @return               The multiChoiceElement value
	 */
	public boolean isMultiChoiceElement(String encodedXPath) {
		String schemaPath = toSchemaPath(encodedXPath);
		// prtln ("isRepeatingElement with schemaPath: " + schemaPath);
		SchemaNode schemaNode = getSchemaNode(schemaPath);
		if (schemaNode == null) {
			prtln("isMultiChoiceElement() could not find a schemaNode for " + schemaPath);
			return false;
		}
		return (isMultiChoiceElement(schemaNode));
	}


	/**
	 *  Returns true if the given schemaNode allows for multiple choice children
	 *  elements.
	 *
	 * @param  schemaNode  schemaNode to be tested
	 * @return             true if schemaNode has a choice compositor and has a
	 *      maxOccurs value greater than 1
	 */
	public boolean isMultiChoiceElement(SchemaNode schemaNode) {
		if (hasChoiceCompositor(schemaNode)) {
			ComplexType cType = (ComplexType) getGlobalDef(schemaNode);
			return (cType.getCompositor().getMaxOccurs() > 1);
		}
		return false;
	}


	/**
	 *  Gets the choiceElement attribute of the SchemaHelper object
	 *
	 * @param  encodedXPath  NOT YET DOCUMENTED
	 * @return               The choiceElement value
	 */
	public boolean isChoiceElement(String encodedXPath) {
		String normalizedXPath = toSchemaPath(encodedXPath);
		SchemaNode schemaNode = getSchemaNode(normalizedXPath);
		if (schemaNode == null) {
			prtln("isChoiceElement couldn't find schemaNode for " + normalizedXPath);
			return false;
		}
		return isChoiceElement(schemaNode);
	}


	/**
	 *  Return true if this element is a member of a choice compositor.<p>
	 *
	 *  This is determined by the typeDefinition of the element's parent.
	 *
	 * @param  schemaNode  Description of the Parameter
	 * @return             The choiceElement value
	 */
	public boolean isChoiceElement(SchemaNode schemaNode) {
		return schemaNode.getIsChoiceMember();
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  encodedXPath  NOT YET DOCUMENTED
	 * @return               NOT YET DOCUMENTED
	 */
	public boolean hasChoiceCompositor(String encodedXPath) {
		String normalizedXPath = toSchemaPath(encodedXPath);
		SchemaNode schemaNode = getSchemaNode(normalizedXPath);
		if (schemaNode == null) {
			prtln("hasChoiceCompositor couldn't find schemaNode for " + normalizedXPath);
			return false;
		}
		return schemaNode.hasChoiceCompositor();
	}


	/**
	 *  Returns true if the schemaNode for the specified path has a compositor.
	 *
	 * @param  encodedXPath  NOT YET DOCUMENTED
	 * @return               NOT YET DOCUMENTED
	 */
	public boolean hasCompositor(String encodedXPath) {
		String normalizedXPath = toSchemaPath(encodedXPath);
		SchemaNode schemaNode = getSchemaNode(normalizedXPath);
		if (schemaNode == null) {
			prtln("hasCompositor couldn't find schemaNode for " + normalizedXPath);
			return false;
		}
		return schemaNode.hasCompositor();
	}


	/**
	 *  Returns true if the Node's type definition specifies a choice compositor
	 *
	 * @param  schemaNode  Description of the Parameter
	 * @return             Description of the Return Value
	 */
	public boolean hasChoiceCompositor(SchemaNode schemaNode) {
		return schemaNode.hasChoiceCompositor();
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  encodedXPath  NOT YET DOCUMENTED
	 * @return               NOT YET DOCUMENTED
	 */

	public boolean hasSequenceCompositor(String encodedXPath) {
		String normalizedXPath = toSchemaPath(encodedXPath);
		SchemaNode schemaNode = getSchemaNode(normalizedXPath);
		if (schemaNode == null) {
			prtln("hasSequenceCompositor couldn't find schemaNode for " + normalizedXPath);
			return false;
		}
		return schemaNode.hasSequenceCompositor();
	}


	/**
	 *  Returns true if the Node's type definition specifies a sequence compositor
	 *
	 * @param  schemaNode  Description of the Parameter
	 * @return             Description of the Return Value
	 */
	public boolean hasSequenceCompositor(SchemaNode schemaNode) {
		// prtln ("hasSequenceCompositor: " + schemaNode.getXpath());
		return schemaNode.hasSequenceCompositor();
	}



	/**
	 *  Gets the multiSelect attribute of the SchemaHelper object
	 *
	 * @param  encodedXPath  Description of the Parameter
	 * @return               The multiSelect value
	 */
	public boolean isMultiSelect(String encodedXPath) {
		String normalizedXPath = toSchemaPath(encodedXPath);
		SchemaNode schemaNode = getSchemaNode(normalizedXPath);
		if (schemaNode == null) {
			prtln("isMultiSelect couldn't find schemaNode for " + normalizedXPath);
			return false;
		}
		return isMultiSelect(schemaNode);
	}


	/**
	 *  Determines if the SchemaNode should rendered as a MultiSelect Element
	 *  (e.g., checkboxes). Returns true IFF an element:
	 *  <ul>
	 *    <li> is an enumeration type</li> , AND
	 *    <li> has a minOccurs of 1</li> , AND
	 *    <li> maxOccurs of "unbounded"
	 *  </ul>
	 *
	 *
	 * @param  schemaNode  Description of the Parameter
	 * @return             The MultiSelect value
	 */
	public boolean isMultiSelect(SchemaNode schemaNode) {
		// first make sure the element is an enumeration type
		// if (!isEnumerationType(schemaNode.getDataTypeName())) {
		if (!isEnumerationType(schemaNode.getTypeDef())) {
			// prtln ("isMultiChoice() got a non-enumeration: " + normalizedXPath);
			return false;
		}

		// now check that minOccurs is "1" and the maxOccurs is unbounded
		// ISSUE: why do we care if minOccurs is 1???
		// boolean ret = (schemaNode.isUnbounded() && schemaNode.getMinOccurs() == 1);

		boolean ret = (schemaNode.isUnbounded() && schemaNode.getMaxOccurs() > 1);

		if (ret) {
			// prtln("isMultiSelect() is returning true for " + xpath);
		}
		return ret;
	}


	/**
	 *  multiselect iff
	 *  <ul>
	 *    <li> isComplex</li>
	 *    <li> hasSequenceCompositor</li>
	 *    <ul>
	 *      <li> containing a single multiSelect element</li>
	 *    </ul>
	 *
	 *  </ul>
	 *
	 *
	 * @param  schemaNode  NOT YET DOCUMENTED
	 * @return             NOT YET DOCUMENTED
	 */
	public boolean hasMultiSelect(SchemaNode schemaNode) {
		// prtln ("\nhasMultiSelect()");
		// prtln ("schemaNode: " + schemaNode.toString());
		if (!schemaNode.isElement()) {
			return false;
		}

		GlobalDef typeDef = getGlobalDef(schemaNode);
		if (typeDef == null) {
			// prtln(" ... unable to find type def for " + schemaNode.getDataTypeName());
			return false;
		}
		if (!typeDef.isComplexType()) {
			// prtln(" ... is not complexType");
			return false;
		}

		ComplexType cType = (ComplexType) typeDef;
		if (cType.getCompositorType() == Compositor.SEQUENCE) {
			List seqNodes = cType.getElement().selectNodes(NamespaceRegistry.makeQualifiedName(cType.getXsdPrefix(), "sequence") + "/*");
			if (seqNodes.size() != 1) {
				// prtln(" ... more than one sequence elements found - returning false");
				return false;
			}

			// There is only one element contained in the sequence. If this elsment is an enumeration
			// then return true ...
			Element seqElement = (Element) seqNodes.get(0);
			String typeName = seqElement.attributeValue("type");
			return isEnumerationType(typeName);
		}
		else {
			// prtln(" ... non-sequence compositor - returning false");
			return false;
		}
	}


	/**
	 *  Test a JSP-encoded pathArg for whether it refers to an element that
	 *  satisfies the {@link #hasOptionalMultiSelect(SchemaNode)} predicate.<p>
	 *
	 *  NOTE: NEVER REFERENCED
	 *
	 * @param  pathArg  an JSP-encoded XPath
	 * @return          true if pathArg refers to an element that has an
	 *      optionalMultiSelect child element
	 */
	public boolean hasOptionalMultiSelect(String pathArg) {
		String xpath = toSchemaPath(pathArg);
		SchemaNode schemaNode = getSchemaNode(xpath);
		if (schemaNode == null) {
			prtln("isEnumerationPathArg failed to find schemaNode for " + pathArg);
			return false;
		}
		prtln("about to call hasOptionalMultiSelect for " + xpath);
		boolean ret = hasOptionalMultiSelect(schemaNode);
		return ret;
	}


	/**
	 *  Returns true if this element is optional and contains only a single
	 *  multiSelect element. More specifically it returns true if an element:<br>
	 *
	 *  <ul>
	 *    <li> isOptional</li>
	 *    <li> isComplex</li>
	 *    <li> hasSequenceCompositor</li>
	 *    <ul>
	 *      <li> containing a single multiSelect element</li>
	 *    </ul>
	 *
	 *  </ul>
	 *  NOTE: NEVER REFERENCED
	 *
	 * @param  schemaNode  Description of the Parameter
	 * @return             Description of the Return Value
	 */
	public boolean hasOptionalMultiSelect(SchemaNode schemaNode) {
		return (hasMultiSelect(schemaNode) && schemaNode.getMinOccurs() == 0);
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  schemaNode  NOT YET DOCUMENTED
	 * @return             NOT YET DOCUMENTED
	 */
	public boolean hasRequiredMultiSelect(SchemaNode schemaNode) {
		return (hasMultiSelect(schemaNode) && schemaNode.getMinOccurs() > 0);
	}


	/**
	 *  Returns true if the schemaNode is an element, a complexType (a branch) and
	 *  is required.
	 *
	 * @param  schemaNode  Description of the Parameter
	 * @return             The requiredBranch value
	 */
	public boolean isRequiredBranch(SchemaNode schemaNode) {
		/* 		if (! (schemaNode.isElement() && schemaNode.getTypeDef().isComplexType())) {
			// prtln("isRequiredBranch got an non-Element node ... returning false");
			return false;
		}
		return schemaNode.isRequired(); */
		return (schemaNode.isElement() &&
			schemaNode.getTypeDef().isComplexType() &&
			schemaNode.isRequired());
	}


	/**
	 *  Returns true if the given schemaNode must have a text value (in addition to
	 *  possibly having subelements). The tricky part of this method is that we are
	 *  treating "stringTextType" as required (enforcing an implicit convention
	 *  used in DLESE schemas).<p>
	 *
	 *  NOTE: currently we are calling ANY extended element (besides
	 *  stringTextType) optional. I don't even know how you would define it as
	 *  required in the schema, but I'm not worrying about it for now ...
	 *
	 * @param  schemaNode  Description of the Parameter
	 * @return             true if the schemaNode represents an element that must
	 *      have content
	 */
	public boolean isRequiredContentElement(SchemaNode schemaNode) {
		if (schemaNode.isAttribute()) {
			// prtln("isRequiredContentElement() called with an Attribute! (" + schemaNode.toString());
			return false;
		}
		if (schemaNode.isElement()) {
			GlobalDef typeDef = getGlobalDef(schemaNode);
			if ((typeDef == null) || typeDef.isBuiltIn() || typeDef.isSimpleType()) {
				// we were only interested in simple - or complexContent, so if the typeDef is not found we bail
				return schemaNode.isRequired();
			}
			if (typeDef.isComplexType()) {
				ComplexType complexTypeDef = (ComplexType) typeDef;
				// if (complexTypeDef.hasSimpleContent() || complexTypeDef.hasComplexContent()) {
				if (complexTypeDef.isDerivedType()) {
					// return (schemaNode.getValidatingTypeName().equals("stringTextType"));
					String validatingTypeName = schemaNode.getValidatingType().getQualifiedName();

/* 					prtln ("isRequiredContentElement() checking " + schemaNode.getXpath() + " ...");
					prtln ("... dataType is " + complexTypeDef.getName());
					prtln ("... validatingTypeName is " + validatingTypeName); */
					
					// End run the KLUDGE below for special cases
					if (validatingTypeName.equals ("union.pubNameType"))
						return schemaNode.isRequired();
					
					/* A KLUDGE that causes much trouble: Declare this base element REQUIRED content if
					   it is not an xsd:string type
					*/					
					return !validatingTypeName.equals(NamespaceRegistry.makeQualifiedName(complexTypeDef.getXsdPrefix(), "string"));
				}
				else {
					return schemaNode.isRequired();
				}
			}
		}
		return false;
	}


	/**
	 *  Returns true if a {@link org.dlese.dpc.xml.schema.SchemaNode} defines a
	 *  required attribute.
	 *
	 * @param  schemaNode  Description of the Parameter
	 * @return             The requiredAttribute value
	 */
	public boolean isRequiredAttribute(SchemaNode schemaNode) {
		if (schemaNode.isAttribute()) {
			return schemaNode.isRequired();
		}
		else {
			return false;
		}
	}


	/**
	 *  Gets the comboUnionType attribute of the SchemaHelper object
	 *
	 * @param  typeDef  NOT YET DOCUMENTED
	 * @return          The comboUnionType value
	 */
	public boolean isComboUnionType(GlobalDef typeDef) {
		if (typeDef == null || !typeDef.isSimpleType()) {
			return false;
		}
		/* 		String typeName = typeDef.getName();
		return isComboUnionType(typeName); */
		return ((SimpleType) typeDef).isComboUnionType();
	}


	/**
	 *  Gets the comboUnionType attribute of the SchemaHelper object
	 *
	 * @param  typeDef   NOT YET DOCUMENTED
	 * @return           The comboUnionType value
	 */
	/* 	public boolean isComboUnionType(String typeName) {
		prtln ("isComboUnionType() with " + typeName);
		boolean ret;
		GlobalDef globalDef = (GlobalDef) globalDefMap.getValue(typeName);
		if (globalDef == null || globalDef.isBuiltIn()) {
			// prtln (" ... couldn't find globalDef for " + typeName + " .... returning false");
			return false;
		}
		// combo union types are simple
		if (!globalDef.isSimpleType()) {
			return false;
		}
		SimpleType simpleType = (SimpleType) globalDef;
		// We can have at most one non-enumeration type, and (for now) it has to be "xsd:string" or "stringTextType"
		if (simpleType.isUnion()) {
			List nonUnionFields = Arrays.asList(new String[]{simpleType.getXsdPrefix() + "string", "stringTextType"});
			String[] memberTypeNames = simpleType.getUnionMemberTypeNames();
			// prtln ("\tmember types: " + simpleType.getUnionMemberTypesAsString());
			String nonUnionTypeName = null;
			for (int i = 0; i < memberTypeNames.length; i++) {
				String memberName = memberTypeNames[i];
				if (!isEnumerationType(memberName)) {
					if (nonUnionTypeName == null && nonUnionFields.contains(memberName)) {
						// prtln ("  ... nonUnionTypeName: " + memberName);
						nonUnionTypeName = memberName;
					}
					else {
						// prtln ("kicking out with " + memberName);
						return false;
					}
				}
			}
			return (nonUnionTypeName != null);
		}
		// catch all
		return false;
	}
 */
	/**
	 *  Returns true if the given typeDef represents an eumeration datatype
	 *
	 * @param  typeDef  Description of the Parameter
	 * @return          The enumerationType value
	 */
	public boolean isEnumerationType(GlobalDef typeDef) {
		if (typeDef == null || !typeDef.isSimpleType()) {
			return false;
		}
		return ((SimpleType) typeDef).isEnumerationType();
	}


	/**
	 *  Returns true if the typeName corresponds to datatype that represents an
	 *  enumeration. Union-types are Enumerations IFF all the member types are also
	 *  enumerations.
	 *
	 * @param  typeName  the name of a datatype defined by a schema
	 * @return           true if the datatype specifies an enumeration
	 */
	public boolean isEnumerationType(String typeName) {
		// we can get typeName == null under normal circumstances. if this happens,
		// don't even bother consulting GlobalDefMap
		if (typeName == null)
			return false;
		return isEnumerationType(getGlobalDef(typeName));
	}


	/**
	 *  Returns a list of element names for the given path from the instanceDoc.
	 *  Used to enforce ordering of elements having sequence compositor.
	 *
	 * @param  xpath  NOT YET DOCUMENTED
	 * @return        The childrenOrder value
	 */
	public List getChildrenOrder(String xpath) {
		String normalizedXPath = XPathUtils.normalizeXPath(xpath);
		List list = new ArrayList();
		Node node = getInstanceDocNode(normalizedXPath);
		if (node != null) {
			for (Iterator i = ((Element) node).elementIterator(); i.hasNext(); ) {
				Element child = (Element) i.next();
				list.add(child.getName());
			}
		}
		return list;
	}


	/**
	 *  Gets the enumerationValues that are specified by the typeName. If the <i>
	 *  getLeafValues</i> parameter is true, then human-readable values (e.g.,
	 *  "DLESE:" would be stripped from "DLESE:author")are returned, if possible.
	 *
	 * @param  getLeafValues  specifies whether human-readable values are returned
	 * @param  globalDef      NOT YET DOCUMENTED
	 * @return                The enumerationValues value, or null if the typeNode
	 *      is not found.
	 */
	public List getEnumerationValues(GlobalDef globalDef, boolean getLeafValues) {
		if (globalDef == null || !globalDef.isSimpleType())
			return null;
		return ((SimpleType) globalDef).getEnumerationValues(getLeafValues);
	}


	/*
	* returns enumeration values for a given typeName in the default namespace
	*/
	/**
	 *  Gets the enumerationValues attribute of the SchemaHelper object
	 *
	 * @param  typeName       NOT YET DOCUMENTED
	 * @param  getLeafValues  NOT YET DOCUMENTED
	 * @return                The enumerationValues value
	 */
	public List getEnumerationValues(String typeName, boolean getLeafValues) {
		return getEnumerationValues(getGlobalDef(typeName), getLeafValues);
	}


	/**
	 *  Finds a globalDef object for a given xpath. This is done by first obtaining
	 *  the globalDef NAME from the {@link org.dlese.dpc.xml.schema.SchemaNodeMap}
	 *  and then using the name as an index into the {@link org.dlese.dpc.xml.schema.GlobalDefMap}.
	 *
	 * @param  xpath  an xpath to a specific node
	 * @return        an {@link org.dlese.dpc.xml.schema.GlobalDef} object
	 */
	public GlobalDef getGlobalDefFromXPath(String xpath) {
		// use a (normalized) xpath to obtain a typeName
		// prtln ("getGlobalDefFromXPath()");
		String normalizedXPath = XPathUtils.normalizeXPath(xpath);
		SchemaNode schemaNode = (SchemaNode) schemaNodeMap.getValue(normalizedXPath);
		if (schemaNode == null) {
			prtln("getGlobalDefFromXPath no schemaNode found for " + normalizedXPath);
			return null;
		}
		return schemaNode.getTypeDef();
	}


	/**
	 *  Create a miminal instance document that is used as the starting point for a
	 *  new document. The miminal document should contain only those elements that
	 *  are required for a valid document for the current framework.
	 *
	 * @return    The minimalDocument as a {@link org.dom4j.Document}
	 */
	public Document getMinimalDocument() {
		if (minimalDocument == null) {
			Document miniDoc = (Document) instanceDocument.clone();
			pruneTree(miniDoc.getRootElement());
			minimalDocument = miniDoc;

			/* 			// this call chokes xslt
			if (!namespaceEnabled)
				minimalDocument = Dom4jUtils.localizeXml(minimalDocument);
*/
		}
		return (Document) minimalDocument.clone();
	}


	/**
	 *  Creates a "miminalElement" (one that contains only required elements) from
	 *  the instanceDocument.
	 *
	 * @param  encodedPath  JSP-encoded xpath that specifies the element to create
	 * @return              An element from the instanceDocument pruned to
	 *      eliminate non-required branches.
	 */
	public Element getNewElement(String encodedPath) {
		String xpath = toSchemaPath(encodedPath);
		// prtln ("SchemaHelper.getNewElement() with " + xpath);
		SchemaNode schemaNode = getSchemaNode(xpath);
		Document doc = (Document) instanceDocument.clone();
		Element newElement = null;
		if (schemaNode.isSubstitutionGroupMember()) {
			newElement = schemaNode.getSubstitutionElement();

			String newElementQName = XPathUtils.getNodeName(xpath);
			String newElementPrefix = NamespaceRegistry.getNamespacePrefix(newElementQName);
			String newElementName = NamespaceRegistry.stripNamespacePrefix(newElementQName);
			// prtln ("\t newElementQName: " + newElementQName);
			// prtln ("\t newElementPrefix: " + newElementPrefix);
			// prtln ("\t newElementName: " + newElementName);
			Namespace ns = globalDefMap.getNamespaces().getNSforPrefix(newElementPrefix);

			Element parent = (Element) doc.selectSingleNode(XPathUtils.getParentXPath(xpath));
			parent.add(newElement);
			newElement.setQName(new QName(newElementName, ns));
			// prtln (Dom4jUtils.prettyPrint(doc));
		}
		else {

			Node node = doc.selectSingleNode(xpath);
			if (node == null) {
				prtln("getNewElement failed to find instanceDocument node at " + xpath);
				return null;
			}
			newElement = (Element) node;
		}

		pruneTree(newElement);
		// prtln("getNewElement returning:\n" + newElement.asXML());
		return (Element) newElement.detach();
	}

	/**
	 *  Recursively remove non-required elements from a branch of the instanceDoc.
	 *  The instanceDoc has no content, so we don't test for the presence of
	 *  values, but rather simply if a node is required or not.
	 *
	 * @param  e  Description of the Parameter
	 */
	private void pruneTree(Element e) {
		// prtln ("SchemaHelper.pruneTree(" + e.getPath() + ")");

		/* test for ABSTRACT ELEMENT - WHY DO THIS HERE?? This is an arbitrary substitution!!!
		   change node name to member of substitutionGroup

		   - when is a better time to resolve abstract elements?
		   -- before they are rendered as input elements (theh path can be
		   		manipulated during rendering (form abstract into real)
		 */
		SchemaNode schemaNode = getSchemaNode(e.getPath());

		// REMOVE ABSTRACT ELEMENTS
		if (schemaNode == null) {
			String msg = "WARNING: Prune tree could not find schemaNode for" + e.getPath();
			prtln(msg);
			return;
		}

		if (schemaNode.isAbstract()) {
			// prtln ("removing abstract element: " + e.getPath());
			Node n = e.detach();
			n = null;
		}

		/*  	// no longer do any substitution at this point for abstract elements
		// this is left to the editor
		if (schemaNode.isAbstract()) {

			box ("processing abstract element", "sh");
			try {
				List substitutionGroup = schemaNode.getSubstitutionGroup();
				GlobalElement replaceDef = (GlobalElement) substitutionGroup.get(0);
				prtln (" ... replacement: " + replaceDef.getQualifiedInstanceName());
				Namespace ns = globalDefMap.getNamespaces().getNSforUri(replaceDef.getNamespace().getURI());
				e.setQName(new QName (replaceDef.getName(), ns));

			} catch (Throwable ex) {
				prtln ("WARNING: could not substitute for abstract element: " + ex.getMessage());
				ex.printStackTrace();
			}
		}
*/

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
	}


	/**
	 *  Converts a string representation of an schema element occurrence value into
	 *  a number.<p>
	 *
	 *  "unbounded" gets converted to Integer.MAX_INT, otherwise, the string gets
	 *  converted to an int.
	 *
	 * @param  s             value of a minOccurs or maxOccurs attribute
	 * @param  defaultValue  default value defined by schema spec
	 * @return               an int representation
	 */
	private static int getOccurrenceInt(String s, int defaultValue) {
		if (s == null) {
			// prtln("getOccurrenceInt got a null string as input");
			return defaultValue;
		}
		else if (s.equals("unbounded"))
			return SchemaHelper.UNBOUNDED;

		int val = defaultValue;
		try {
			val = Integer.parseInt(s);
		} catch (NumberFormatException e) {
			prtln("getOccurrenceInt error: " + e.getMessage() + "\n returning default (" + defaultValue + ")");
		}
		return val;
	}


	/**
	 *  Utility to extract the maxOccurs attribute of the given element and returns
	 *  an int equivalent. Returns default value if element does not contain
	 *  maxOccurs attribute.
	 *
	 * @param  e  element from a xml schema
	 * @return    The maxOccurs value as or default as int
	 */
	public static int getMaxOccurs(Element e) {
		if (e == null)
			return SchemaHelper.MAXOCCURS_DEFAULT;
		return SchemaHelper.getOccurrenceInt(e.attributeValue("maxOccurs"), SchemaHelper.MAXOCCURS_DEFAULT);
	}


	/**
	 *  Extract the minOccurs attribute of the given element and returns an int
	 *  equivalent. Returns default value if element does not contain minOccurs
	 *  attribute.
	 *
	 * @param  e  element from a xml schema
	 * @return    The minOccurs value as or default as int
	 */
	public static int getMinOccurs(Element e) {
		if (e == null)
			return SchemaHelper.MINOCCURS_DEFAULT;
		return SchemaHelper.getOccurrenceInt(e.attributeValue("minOccurs"), SchemaHelper.MINOCCURS_DEFAULT);
	}

 	public String encodePathIfAnyType (String xpath) {
		if (this.isAnyTypeElement (xpath))
			return encodeAnyTypeXpath(xpath);
		return xpath;
	}


	/**
	* Replaces xpath having node name of "any" with a wild-card version that
	* can actually access the any element (which is anonymous) in the XML Document.
	*/
	public String encodeAnyTypeXpath (String xpath) {
		String leaf = XPathUtils.getLeaf (xpath);
		String anyType = NamespaceRegistry
			.makeQualifiedName(this.getSchemaNamespace().getPrefix(), "any");
		if (leaf.startsWith(anyType))
			return XPathUtils.getParentXPath(xpath) + "/*" + leaf.substring(anyType.length());
		return xpath;
	}
	
	/**
	* Replaces xpath used to access an "anyType" node in the XML Document with a version
	that is known to the schema. I.e., Replaces the node-name of '*' with 'any' while preserving
	any indexing.
	*/
	public String decodeAnyTypeXpath (String xpath) {
		String leaf = XPathUtils.getLeaf (xpath);
		String anyType = NamespaceRegistry
			.makeQualifiedName(this.getSchemaNamespace().getPrefix(), "any");
		if (leaf.startsWith("*"))
			return XPathUtils.getParentXPath(xpath) + "/" + anyType + leaf.substring("*".length());
		return xpath;
	}

	
	
	/**  Description of the Method */
	public void destroy() {
		globalDefMap.destroy();
		schemaNodeMap.destroy();
		xsdDatatypeManager.destroy();
		instanceDocument = null;
		minimalDocument = null;
		definitionMiner.destroy();
	}


	/**
	 *  Sets the debug attribute of the SchemaHelper class
	 *
	 * @param  d  The new debug value
	 */
	public static void setDebug(boolean d) {
		debug = d;
	}


	/**
	 *  Sets the verbose attribute of the SchemaHelper class
	 *
	 * @param  v  The new verbose value
	 */
	public static void setVerbose(boolean v) {
		verbose = v;
	}


	/**
	 *  Description of the Method
	 *
	 * @param  n  NOT YET DOCUMENTED
	 */
	private void pp(Node n) {
		prtln(Dom4jUtils.prettyPrint(n));
	}


	/**
	 *  Description of the Method
	 *
	 * @param  s  Description of the Parameter
	 */
	private static void prtln(String s) {
		if (debug) {

			while (s.length() > 0 && s.charAt(0) == '\n') {
				System.out.println("");
				s = s.substring(1);
			}

			System.out.println("sh: " + s);
		}
	}

		private static void prtlnErr(String s) {
			SchemaUtils.prtln(s, "SchemaHelper");
/* 			while (s.length() > 0 && s.charAt(0) == '\n') {
				System.out.println("");
				s = s.substring(1);
			}

			System.out.println("sh: " + s); */
	}
	

	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  s       NOT YET DOCUMENTED
	 * @param  prefix  NOT YET DOCUMENTED
	 */
	public static void box(String s, String prefix) {
		prtln("\n----------------------------------");
		if (prefix == null || prefix.trim().length() == 0)
			prtln(s);
		else
			prtln(prefix + ": " + s);
		prtln("----------------------------------\n");
	}

}

