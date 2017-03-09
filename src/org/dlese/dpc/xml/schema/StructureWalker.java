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

import org.dom4j.dom.DOMElement;
import org.dom4j.io.XMLWriter;
import org.dom4j.DocumentFactory;
import org.dom4j.Node;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Attribute;
import org.dom4j.Namespace;
import org.dom4j.QName;
import org.dom4j.XPath;
import org.dom4j.tree.NamespaceStack;

import java.util.*;
import java.net.URI;

/**
 *  Creates an empty "XPath-equivant" instance document Document based on a
 *  venetian-blind-type XML Schema. Provides an intermediate structure for
 *  building a {@link org.dlese.dpc.xml.schema.SchemaNodeMap} that is accessible
 *  via XPath keys, and that will hold important information about the Document
 *  elements (see {@link org.dlese.dpc.xml.schema.SchemaNode}. For example, the
 *  SchemaNodeMap (that is enabled by an StructureWalker) can provide type
 *  definition and validation information, defaults, etc that are accessible via
 *  XPath keys
 *
 * @author     ostwald
 * @created    June 28, 2006
 */
public class StructureWalker {
	private static boolean debug = false;
	private static int debugThreshold = 1;

	private Element hatrack = null;
	private Document hatrackDoc = null;
	// this is the temporary holder for the XPath equiv ItemRecord

	private SchemaHelper schemaHelper = null;

	private GlobalDefMap globalDefMap = null;
	/**  Mapping from xpath to schemaNode instance */
	public SchemaNodeMap schemaNodeMap = null;
	/**  The instance document created by Structure Walker */
	public Document instanceDocument = null;
	private DocumentFactory df;
	private NamespaceRegistry namespaces;
	private Namespace defaultNamespace;
	private Namespace targetNamespace;
	private String schemaUri;

	private NamespaceStack nsStack;
	private ReaderStack readerStack;
	private boolean namespaceEnabled = false;
	private boolean attributeDefaultQualified = true;


	/**
	 *  Constructor for the StructureWalker object
	 *
	 * @param  schemaRootElement          Description of the Parameter
	 * @param  schemaHelper               Description of the Parameter
	 * @exception  SchemaHelperException  NOT YET DOCUMENTED
	 */
	public StructureWalker(Element schemaRootElement, SchemaHelper schemaHelper)
		 throws SchemaHelperException {

		this.schemaHelper = schemaHelper;
		this.schemaUri = schemaHelper.getSchemaLocation();
		this.globalDefMap = schemaHelper.getGlobalDefMap();
		this.namespaces = globalDefMap.getNamespaces();
		schemaNodeMap = new SchemaNodeMap();
		df = DocumentFactory.getInstance();

		/*
		 *  See NamespaceRegistry.getNamedDefaultNamespace()
		 *  How it works determines whether namespaces will be enabled or not
		 */
		/* 		defaultNamespace = namespaces.getNamedDefaultNamespace();
		if (defaultNamespace != Namespace.NO_NAMESPACE) {
			namespaceEnabled = true;
		} */
		targetNamespace = namespaces.getTargetNamespace();
		defaultNamespace = namespaces.getDefaultNamespace();
		if (namespaces.isMultiNamespace()) {
			namespaceEnabled = true;
		}

		// prtln("namespaceEnabled: " + namespaceEnabled, 5);

		// initialize readerStack
		readerStack = new ReaderStack(globalDefMap);
		readerStack.push(getSchemaReader(schemaUri));
		prtln("\n" + readerStack.toString(), 1);

		// initialize namespaceStack
		nsStack = new NamespaceStack(df);
		pushNS(targetNamespace);

		String targetNSUri = namespaces.getTargetNamespaceUri();

		// hatrack is an element upon which we will hang elements as we create them
		hatrack = createChildElement("hatrack");
		hatrackDoc = df.createDocument(hatrack);

		try {
			processSchemaElement(schemaRootElement, hatrack);
		} catch (Exception e) {
			e.printStackTrace();
			throw new SchemaHelperException("StructureWalker init ERROR: " + e.getMessage());
		}
		// grab the first element (itemRecord) from hatrack
		// and create a new document with a cloned copy of the element as root
		Element hatrackRoot = (Element) hatrack.elements().get(0);
		Element instanceRoot = (Element) hatrackRoot.clone();

		// REQUIRED to add namespaces to root element so the local namespace definitions disappear
		prtln("adding namespaces to instanceRoot", 1);
		prtln(" ... targetNSUri: " + targetNSUri, 1);

		if (namespaceEnabled) {

			// add schemaLocation attribute and schemaInstance/schemaURI namespace declaration
			Namespace instNS = namespaces.getSchemaInstanceNamespace();
			QName instQname = df.createQName("schemaLocation", instNS);
			instanceRoot.addNamespace(instNS.getPrefix(), instNS.getURI());
			instanceRoot.addAttribute(instQname, targetNSUri + " " + schemaUri);
			prtln("\t adding defaultNamespace (" + NamespaceRegistry.nsToString(defaultNamespace) + ")", 1);

			if (!defaultNamespace.getURI().equals(namespaces.getSchemaNamespace().getURI()) &&
				defaultNamespace.getPrefix() != null && defaultNamespace.getPrefix().length() > 0) {
				instanceRoot.addNamespace(defaultNamespace.getPrefix(), defaultNamespace.getURI());
				prtln("\tadded \"" + defaultNamespace.getPrefix() + "\": " + defaultNamespace.getURI(), 1);
			}

			for (Iterator i = namespaces.getNamespaces().iterator(); i.hasNext(); ) {
				Namespace ns = (Namespace) i.next();
				if (!targetNSUri.equals(ns.getURI()) &&
					ns.getURI() != namespaces.getSchemaNamespace().getURI() &&
					!ns.getPrefix().equals("")) {
					instanceRoot.addNamespace(ns.getPrefix(), ns.getURI());
					prtln("\t added to instanceRoot: " + ns.getPrefix() + ": " + ns.getURI(), 1);
				}
			}
		}

		instanceDocument = df.createDocument(instanceRoot);

		prtln ("about to prune substitute nodes ...", 2);
		pruneSubstituteNodes();

		/*
		 // for debugging - prints out the namespaces associated with the instanceDocument
		 NamespaceRegistry nsr = new NamespaceRegistry();
		 nsr.registerNamespaces(instanceDocument);
		 prtln(nsr.toString(), 1);
		 */
	}


	/**
	 *  Remove all the elements of all SubstitutionGroup members from the
	 *  instanceDocument.<p>
	 *
	 *  we need to retain the schemaNodes for these elements, but we don't want
	 *  them to exist in the instance document.
	 */
	private void pruneSubstituteNodes() {
		Iterator schemaNodes = schemaNodeMap.getValues().iterator();
		while (schemaNodes.hasNext()) {
			SchemaNode schemaNode = (SchemaNode) schemaNodes.next();

			if (schemaNode.isAttribute())
				continue;

			Element instanceElement = null;
			
			try {
				instanceElement = (Element) instanceDocument.selectSingleNode(schemaNode.getXpath());
			} catch (Exception e) {
				prtln ("WARNING: pruneSubstituteNodes unable to process element for " + schemaNode.getXpath() + ": " + e.getMessage(), 2);
			}
			
			if (instanceElement == null) {
				// prtln("\t ... WARNING pruneSubstitutionNodes did not find instanceElement", 1);
				// prtln("\t\t ... was looking for " + schemaNode.getXpath(), 1);
				continue;
			}

			if (schemaNode.isSubstitutionGroupMember()) {
				//box ("pruneSubstituteNodes about to remove child from instance document ...", 1);
				prtln(schemaNode.toString());
				prtln("------------------------\n" + instanceElement.asXML() + "\n----------------\n", 1);
				instanceElement = (Element) instanceElement.detach();
				schemaNode.setSubstitutionElement(instanceElement);
			}

			// remove CONTENT of ASTRACT elements from instance document
			// - issue - should we remove attributes as well?
			if (schemaNode.isAbstract()) {
				// box ("about to empty abstract node at " + schemaNode.getXpath());

				prtln("------------------------\n" + instanceElement.asXML() + "\n----------------\n", 1);
				instanceElement.clearContent();

				// probably don't need to store instance element for abstract node,
				// since it will never be instantiated, anyway ...
				schemaNode.setSubstitutionElement(instanceElement);
			}
		}
	}


	/**
	 *  Gets the schemaReader attribute of the StructureWalker object
	 *
	 * @param  nsUri  Description of the Parameter
	 * @return        The schemaReader value
	 */
	private SchemaReader getSchemaReader(String nsUri) {
		return schemaHelper.getDefinitionMiner().getSchemaReader(nsUri);
	}


	/**
	 *  Gets the namespaceEnabled attribute of the StructureWalker object
	 *
	 * @return    The namespaceEnabled value
	 */
	public boolean getNamespaceEnabled() {
		return namespaceEnabled;
	}


	/**
	 *  Gets the currentReaderUri attribute of the StructureWalker object
	 *
	 * @return    The currentReaderUri value
	 */
	private String getCurrentReaderUri() {
		if (getCurrentReader() != null) {
			return ReaderStack.getReaderUri(getCurrentReader());
		}
		else {
			return null;
		}
	}


	/**
	 *  Gets the currentReader attribute of the StructureWalker object
	 *
	 * @return    The currentReader value
	 */
	private SchemaReader getCurrentReader() {
		return readerStack.getTos();
	}


	/**
	 *  Gets the currentNamespace attribute of the StructureWalker object
	 *
	 * @return    The currentNamespace value
	 */
	private Namespace getCurrentNamespace() {
		int size = nsStack.size();
		prtln("getCurrentNamespace (" + size + ")", 1);
		if (size < 1) {
			prtlnErr("WARNING: Namespace stack is empty!");
			return null;
		}
		else {
			Namespace ns = nsStack.getNamespace(size - 1);
			prtln(" ... current namespace: " + ns.getPrefix() + ": " + ns.getURI(), 1);
			return ns;
		}
	}


	/**
	 *  Push a namespace object onto the namespaceStack
	 *
	 * @param  ns  NOT YET DOCUMENTED
	 */
	private void pushNS(Namespace ns) {
		nsStack.push(ns);
		prtln("\n namespaceStack (" + nsStack.size() + ") PUSHED namespace: " + ns.getPrefix() + ": " + ns.getURI(), 1);
	}


	/**
	 *  Pop a namespace object from the namespaceStack
	 *
	 * @return    a namespace object
	 */
	private Namespace popNS() {
		Namespace ns = nsStack.pop();
		prtln("  ... stack (" + nsStack.size() + ") POPPED namespace: " + ns.getPrefix() + ": " + ns.getURI(), 1);
		prtln("  ..... currentNS is now (" + getCurrentNamespace().getPrefix() + ": " + getCurrentNamespace().getURI(), 1);
		return ns;
	}


	/**
	 *  Tries to resolve give QName into a "top-level" namespace and prefix. If the
	 *  there is no prefix and the namespace is the default namespace, assign the
	 *  NAMED defaultNamespace.
	 *
	 * @param  qName          Description of the Parameter
	 * @return                Description of the Return Value
	 * @exception  Exception  Description of the Exception
	 */
	private QName resolveQName(QName qName) throws Exception {
		String prefix = qName.getNamespacePrefix();
		String name = qName.getName();
		String uri = qName.getNamespaceURI();
		prtln("\n\t resolveQName: ", 1);
		prtln("\t\t name: " + name, 1);
		prtln("\t\t prefix: " + prefix, 1);
		prtln("\t\t uri: " + uri, 1);
		Namespace topLevelNS = namespaces.getNSforUri(uri);
		if (topLevelNS == null) {
			prtlnErr("ERROR: resolveQName could not find top-level namespace for " + uri);
			return null;
		}
		else if (topLevelNS == namespaces.getDefaultNamespace()) {
			topLevelNS = namespaces.getNamedDefaultNamespace();
		}

		return df.createQName(name, topLevelNS);
	}


	/**
	 *  Resolve qualified name against top-level namespace registry
	 *
	 * @param  name  NOT YET DOCUMENTED
	 * @return       NOT YET DOCUMENTED
	 */
	private Element createChildElement(String name) {
		prtln("\n createChildElement() name: " + name, 1);
		Namespace ns = getCurrentNamespace();
		prtln("\t currentNamespace -- " + ns.getPrefix() + ": " + ns.getURI(), 1);
		Element child = null;
		if (ns == null || !namespaceEnabled) {
			child = df.createElement(name);
		}
		else {
			QName baseQName = df.createQName(name, ns);
			try {
				child = df.createElement(resolveQName(baseQName));
			} catch (Exception e) {
				prtlnErr("createChildElement error: " + e.getMessage());
				e.printStackTrace();
				return null;
			}
		}
		prtln("   ... child: " + child.asXML(), 1);
		return child;
	}


	/**
	 *  Create an attribute in the instanceDocument and a schemaNode for this
	 *  attribute.<p>
	 *
	 *  NOTE: the attribute name may not be present in the schema element for this
	 *  attribute if a reference is involved.
	 *
	 * @param  e              schema Element representing the attribute
	 * @param  parent         The parent of this attribute in the instance Document
	 * @param  attrName       The attribute name
	 * @exception  Exception  Description of the Exception
	 */
	private void createAttribute(Element e, Element parent, String attrName) throws Exception {
		createAttribute(e, parent, attrName, null);
	}


	/**
	 *  Create an attribute in the instanceDocument and a schemaNode for this
	 *  attribute, using the attribute type if specified, or the type specified in
	 *  the schema element otherwise.<p>
	 *
	 *  NOTE: the attribute name may not be present in the schema element for this
	 *  attribute if a reference is involved.
	 *
	 * @param  e              schema Element representing the attribute
	 * @param  parent         The parent of this attribute in the instance Document
	 * @param  attrName       The attribute name
	 * @param  attrType       typeName (if present) overrides type specified in
	 *      schema element
	 * @exception  Exception  Description of the Exception
	 */
	private void createAttribute(Element e, Element parent, String attrName, GlobalDef attrType) throws Exception {
		Attribute a = null;

		prtln("\nCREATE ATTRIBUTE() attrName: (" + attrName + ")", 1);
		if (attrType == null) {
			prtln("\t" + "no attrType provided");
		}
		else {
			prtln("\t ... attribute typeName - " + attrType.getQualifiedName(), 1);
			// prtln ("\t" + "attrType provided: " + attrType.toString());
		}

		/*
		 *  we don't bother qualifying the attribute unless it's name is qualified
		 */
		if (NamespaceRegistry.isQualified(attrName)) {
			// resolve prefix and create qualified attribute name
			prtln("\t attrName is qualified");
			String prefix = NamespaceRegistry.getNamespacePrefix(attrName);
			Namespace attrNS = namespaces.getNSforPrefix(prefix);
			QName qname = df.createQName(NamespaceRegistry.stripNamespacePrefix(attrName), attrNS);
			a = df.createAttribute(parent, qname, "");
		}
		else {
			a = df.createAttribute(parent, attrName, "");
		}

		prtln("\n\t attribute: " + a.asXML(), 1);
		parent.add(a);
		String xpath = getPath(a);
		SchemaNode schemaNode = null;
		GlobalDef globalDef = null;

		globalDef = getElementTypeDef(e);

		if (globalDef == null) {
			throw new Exception("createAttribute got a null globalDef for " + e.asXML());
		}
		if (attrType != null) {
			prtln("\n\t CreateAttribute() instantiating SchemaNode with explicitly provided attrTypeDef", 1);
			schemaNode = new SchemaNode(e, globalDef, xpath, schemaNodeMap, attrType);
		}
		else {
			prtln("\n\tCreateAttribute() instantiating SchemaNode with implicitly provided attrTypeDef (" +
				globalDef.getQualifiedName() + ")", 1);

			schemaNode = new SchemaNode(e, globalDef, xpath, schemaNodeMap);
		}
		schemaNodeMap.setValue(xpath, schemaNode);
		prtln("\nexiting create attribute\n------------------------\n", 1);
	}



	/**
	 *  Finds a {@link org.dlese.dpc.xml.schema.GlobalDef} (datatype definition)
	 *  object in the {@link org.dlese.dpc.xml.schema.GlobalDefMap} that
	 *  "corresponds" to the "type" or "ref" attribute in the given schema element
	 *  definition.<p>
	 *
	 *  Handles instance element definitions, e.g.,
	 *  <ul>
	 *    <li> &lt;xs:element name="tape" type="tp:tapeType"/>
	 *    <li> &lt;xs:element ref="sh:album"/>
	 *  </ul>
	 *
	 *
	 * @param  e              an instance element definition represented as a
	 *      {@link org.dom4j.Element}
	 * @return                The typedef (a GlobalDef) for the given Schema
	 *      Element or null if either a typeName is not present or a GlobalDef
	 *      cannot be found for the typeName.
	 * @exception  Exception  Description of the Exception
	 */
	private GlobalDef getElementTypeDef(Element e) throws Exception {
		String typeName = e.attributeValue("type", null);

		String errorMsg = "";

		if (typeName == null) {
			// there was no type attribute, use the ref attribute instead
			typeName = e.attributeValue("ref", null);
		}

		if (typeName == null) {
			// there was neither a type nor a ref attribute - type defaults to string

			typeName = NamespaceRegistry.makeQualifiedName(e.getNamespacePrefix(), "string");
			e.addAttribute("type", typeName);
			prtln("\t assigned default type: " + e.attributeValue("type"), 1);

			/* 			errorMsg = "getElementTypeDef(): typeName not found for typeDef element: " + e.asXML();
			throw new Exception(errorMsg); */
		}

		prtln("\t current Reader Uri: " + this.getCurrentReaderUri(), 1);
		prtln("\t getElementTypeDef calling readerStack getGlobalDef with \"" + typeName + "\"", 1);
		GlobalDef ret = readerStack.getGlobalDef(typeName);
		if (ret == null) {
			// DEBUGGING 8/12/08
			// SchemaUtils.showGlobalDefs(this.schemaHelper);
			throw new Exception("GlobalDef not found for typeName: \"" + typeName + "\"");
		}
		prtln("\t\t qualifiedName: " + ret.getQualifiedName(), 1);
		prtln("\t\t namespace: " + NamespaceRegistry.nsToString(ret.getNamespace()), 1);
		return ret;
	}



	/**
	 *  Test if the "type" (or "ref") attribute of the given Schema Element
	 *  designates a built-in datatype, as opposed to a derived type.<p>
	 *
	 *  An qualified attribute value is determined to be BuiltIn if it has the same
	 *  prefix as the qualified name of the element in which it is defined.<p>
	 *
	 *  E.g., in the following element, the type is "xs:string", which is a
	 *  built-in type: &lt;xs:element name="artist" type="xs:string"/>.
	 *
	 * @param  e  Description of the Parameter
	 * @return    true if the given schema element refers to a built-in datatype.
	 */
	private boolean isBuiltInType(Element e) {
		String prefix = e.getNamespacePrefix();
		String type = e.attributeValue("type", null);
		if (type == null) {
			type = e.attributeValue("ref", null);
		}
		if (type == null) {
			return false;
		}

		String typePrefix = NamespaceRegistry.getNamespacePrefix(type);
		if (prefix != null && (prefix.equals(typePrefix) || typePrefix.equals("xml"))) {
			return true;
		}
		else {
			return false;
		}
	}


	/**
	 *  Gets the extensionElement attribute of the StructureWalker object
	 *
	 * @param  e  NOT YET DOCUMENTED
	 * @return    The extensionElement value
	 */
	private boolean isExtensionElement(Element e) {
		return (e.attributeValue("extension") != null);
	}


	/**
	 *  Recursively expand schema elements using type definitions. The goal is to
	 *  build an empty XPath-equivelent document, not a valid instance document of
	 *  the schema
	 *
	 * @param  e              current schema Element to process
	 * @param  parent         instanceDoc Element to which we attach new instanceElement
	 * @exception  Exception  if this element cannot be processed
	 */
	private void processSchemaElement(Element e, Element parent)
		 throws Exception {
		String elementName = e.getName();
		prtln("\n================================================================" +
			"\nprocessSchemaElement() with elementName: " +
			elementName + "\n\t" + e.asXML() +
			"\n\tparent path: " + parent.getPath() +
			"\n================================================================", 1);

		/*
		 *  if the element name is "element" (as opposed to a type definition) we ASSUME we are looking at
		 *  a TYPED element or a global Reference. Each element encounted in this way will result in a
		 *  new SchemaNode being created and a new element added to the instanceDocument
		 */
		if (elementName.equals("element")) {
			// we have either a simple or a complex element or a reference
			processInstanceElement(e, parent);
		}
		else {
			// we're dealing with a compositor, an extension, an attribute, ...

			if (elementName.equals("all") ||
				elementName.equals("sequence") ||
				elementName.equals("choice")) {

				prtln("processing an *" + elementName + "* element", 1);
				for (Iterator i = e.elementIterator(); i.hasNext(); ) {
					Element childElement = (Element) i.next();
					processSchemaElement(childElement, parent);
				}
			}
			else if (elementName.equals("simpleContent") ||
				elementName.equals("complexContent")) {
				prtln("processing a *" + elementName + "* element", 1);

				// we must iterate over the elements to pick up the extension, etc
				for (Iterator i = e.elementIterator(); i.hasNext(); ) {
					Element contentElement = (Element) i.next();
					processSchemaElement(contentElement, parent);
				}
			}
			else if (elementName.equals("extension")) {
				processExtension(e, parent);
			}
			else if (elementName.equals("restriction")) {
				processRestriction(e, parent);
			}
			else if (elementName.equals("attribute")) {
				processInstanceAttribute(e, parent);
			}
			else if (elementName.equals("group")) {
				processModelGroup(e, parent);
			}
			else if (elementName.equals("attributeGroup")) {
				processAttributeGroup(e, parent);
			}
			else if (elementName.equals("any")) {
				// prtln ("Any element: " + e.asXML(), 2);
				processAnyElement(e, parent);
			}
			/* 			else if (elementName.equals("annotation")) {
				String parentXPath = getPath(parent);
				prtln ("\nANNOTATION! parent path: " + parentXPath, 4);
				prtln (Dom4jUtils.prettyPrint(e), 1);
				SchemaNode schemaNode = (SchemaNode)this.schemaNodeMap.getValue(parentXPath);
				if (schemaNode == null) {
					prtln ("\t SchemaNode not found!", 1);
				}
				Element docElement = e.element ("documentation");
				if (docElement != null && schemaNode != null) {
					String documentation = docElement.getTextTrim();
					schemaNode.setDocumentation(documentation);
				}
			} */
			else {
				// prtlnErr("Recieved an unknown element type: " + elementName + "\n" + e.asXML());
				// prtln(e.asXML() + "\n", 1);
				throw new Exception("Recieved an unknown element type: " + elementName + "\n" + e.asXML());
			}

			// process a annotation child if present
			// WRONG PLACE - We don't have access to schemaNode!
			/* 			if (e.element("annotation") != null) {
				String xpath = getPath(e);
				prtln ("\nANNOTATION!  path: " + xpath, 4);
				prtln (Dom4jUtils.prettyPrint(e), 1);
				SchemaNode schemaNode = (SchemaNode)this.schemaNodeMap.getValue(xpath);
				if (schemaNode == null) {
					prtln ("\t SchemaNode not found!", 1);
				}
				Element docElement = (Element)e.selectSingleNode ("//documentation");
				if (docElement != null && schemaNode != null) {
					String documentation = docElement.getTextTrim();
					schemaNode.setDocumentation(documentation);
				}
			} */
		}
	}


	/**
	 *  Process a ModelGroup element
	 *
	 * @param  e              current schema Element to process
	 * @param  parent         instanceDoc Element to which we attach new instanceElement
	 * @exception  Exception  if this element cannot be processed
	 */
	private void processModelGroup(Element e, Element parent) throws Exception {
		// find the type Definition for this element's ref

		// Debugging
		prtln("\n processModelGroup()", 1);
		prtln("\t element: " + e.asXML(), 1);

		GlobalDef globalDef = getElementTypeDef(e);
		if (globalDef == null) {
			throw new Exception("processModelGroup received null globalDef for " + e.asXML());
		}

		if (!globalDef.isModelGroup()) {
			throw new Exception("processModelGroup did not get ModelGroup def for " + e.asXML());
		}

		// now we've got our model group, and we can process it just as we would a complexType?

		ModelGroup groupDef = (ModelGroup) globalDef;

		Namespace ns = groupDef.getNamespace();
		pushNS(ns);
		readerStack.push(groupDef.getSchemaReader());
		prtln("\n" + readerStack.toString(), 1);

		List children = groupDef.getChildren();
		for (Iterator i = children.iterator(); i.hasNext(); ) {
			Element childElement = (Element) i.next();
			prtln("\t...with " + childElement.getName(), 1);
			processSchemaElement(childElement, parent);
		}
		popNS();
		SchemaReader popped = readerStack.pop();
		prtln("\n readerStack (" + readerStack.size() + ") popped " + popped.getLocation().toString());
	}


	/**
	 *  Process an "any" element - we create a schemaNode based
	 *
	 * @param  e              current schema Element to process
	 * @param  parent         instanceDoc Element to which we attach new instanceElement
	 * @exception  Exception  if this element cannot be processed
	 */
	private void processAnyElement(Element e, Element parent) throws Exception {

		prtln("processAnyElement", 2);
		prtln("\tparent: " + parent.asXML(), 1);
		prtln("\te: " + e.asXML(), 2);

		String prefix = this.namespaces.getSchemaNamespace().getPrefix();
		Element child = df.createElement(NamespaceRegistry.makeQualifiedName(prefix, "any"));

		prtln("\t ... child: " + child.asXML(), 2);

		parent.add(child);
		String xpath = getPath(child);
		prtln("\t ... xpath: " + xpath, 2);

		BuiltInType typeDef = (BuiltInType) readerStack.getTos().getGlobalDef(e.getQualifiedName());
		prtln("\t processAnyElement() instantiating SchemaNode with built-in type (name: " + typeDef.getName() + ")", 2);
		SchemaNode schemaNode = new SchemaNode(e, typeDef, xpath, schemaNodeMap);
		schemaNodeMap.setValue(xpath, schemaNode);
		prtln ("DONE with any", 2);
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  e              current schema Element to process
	 * @param  parent         instanceDoc Element to which we attach new instanceElement
	 * @exception  Exception  if this element cannot be processed
	 */
	private void processAttributeGroup(Element e, Element parent) throws Exception {
		// find the type Definition for this element's ref

		// Debugging
		prtln("\n processAttributeGroup()", 1);
		prtln("\t element: " + e.asXML(), 1);

		GlobalDef globalDef = getElementTypeDef(e);
		if (globalDef == null) {
			throw new Exception("processAttributeGroup received null globalDef for " + e.asXML());
		}

		if (!globalDef.isAttributeGroup()) {
			throw new Exception("processAttributeGroup did not get attributeGroup def for " + e.asXML());
		}

		// now we've got our attributeGroup, and we can process it just as we would

		AttributeGroup groupDef = (AttributeGroup) globalDef;

		Namespace ns = groupDef.getNamespace();
		pushNS(ns);
		readerStack.push(groupDef.getSchemaReader());
		prtln("\n" + readerStack.toString(), 1);

		List attributes = groupDef.getAttributes();
		for (Iterator i = attributes.iterator(); i.hasNext(); ) {
			Element attrElement = (Element) i.next();
			prtln("\t...with " + attrElement.getName(), 1);
			processInstanceAttribute(attrElement, parent);
		}
		popNS();
		SchemaReader popped = readerStack.pop();
		prtln("\n readerStack (" + readerStack.size() + ") popped " + popped.getLocation().toString());
	}


	/**
	 *  Process a schema element defining an attribute.<p>
	 *
	 *  For Example: e.g., <xsd:attribute name="foo" type="xsd:string" />
	 *
	 * @param  e              current schema Element to process
	 * @param  parent         instanceDoc Element to which we attach new instanceElement
	 * @exception  Exception  if this element cannot be processed
	 */
	private void processInstanceAttribute(Element e, Element parent) throws Exception {
		prtln("\n processInstanceAttribute", 1);
		prtln("\t" + e.asXML(), 1);

		String attrName = e.attributeValue("name");
		String attrType = e.attributeValue("type", null);
		if (attrType == null) {
			attrType = e.attributeValue("ref");
		}
		if (attrType == null) {
			attrType = NamespaceRegistry.makeQualifiedName(e.getNamespacePrefix(), "string");
			e.addAttribute("type", attrType);
			prtln("\t assigned default type: " + e.attributeValue("type"), 1);
		}

		// Debugging
		prtln("\t name: " + attrName + "  type: " + attrType, 1);

		GlobalDef globalDef = getElementTypeDef(e);

		if (globalDef == null) {
			throw new Exception("processInstanceAttribute got a null globalDef");
		}
		else if (globalDef.isBuiltIn()) {
			prtln("\t globalDef is Built-In city", 1);
		}
		else if (globalDef.isGlobalAttribute()) {
			prtln("\t IS global attribute", 1);
		}
		else {
			prtln("\t is NOT global attribute", 1);
		}

		if (globalDef.isBuiltIn()) {
			createAttribute(e, parent, attrName, globalDef);
			return;
		}

		if (globalDef.isSimpleType()) {
			prtln("\t SimpleType!", 1);
			prtln("\t adding attribute: " + attrName, 1);
			createAttribute(e, parent, attrName);
			return;
		}

		if (globalDef.isGlobalAttribute()) {
			// we are processing a REFERENCE to a globalAttribute
			prtln("\n\t\t Processing a REFERENCE to a globalAttribute", 1);
			prtln("\t\tglobalDef: " + globalDef.toString(), 1);
			prtln("\t\tglobalDef type: " + globalDef.getType(), 1);

			// just for fun, show the namespace of the reference to globalAttribute
			Namespace ns = globalDef.getNamespace();
			Namespace cns = getCurrentNamespace();

			attrName = globalDef.getQualifiedName();
			prtln("*** attrName BEFORE resolving/qualifying: " + attrName, 1);
			prtln("\t\t currentNamespace: " + cns.getPrefix() + ": " + cns.getURI(), 1);
			prtln("\t\t globalDef namespace -- " + ns.getPrefix() + ": " + ns.getURI(), 1);

			// qualify / resolve attribute name if required
			if (ns != getCurrentNamespace() && !NamespaceRegistry.isQualified(attrName)) {
				prtln("\t\t ns != currentNamespace", 1);
				// qualify name
				String prefix = namespaces.getNSforUri(ns.getURI()).getPrefix();
				attrName = NamespaceRegistry.makeQualifiedName(prefix, attrName);
			}
			else if (NamespaceRegistry.isQualified(attrName)) {
				// resolve local prefix
				String prefix = NamespaceRegistry.getNamespacePrefix(attrName);
				String nsUri = globalDef.getSchemaReader().getNamespaces().getNSforPrefix(prefix).getURI();
				String topLevelPrefix = namespaces.getNSforUri(nsUri).getPrefix();
				// prtln ("\n top level namespaces\n" + namespaces.toString(), 1);
				attrName = NamespaceRegistry.makeQualifiedName(topLevelPrefix, NamespaceRegistry.stripNamespacePrefix(attrName));
			}

			prtln("*** attrName after resolving/qualifying: " + attrName, 1);
			attrType = globalDef.getType();

			pushNS(ns);
			readerStack.push(globalDef.getSchemaReader());
			prtln("\n" + readerStack.toString(), 1);

			// the attribute NAME is supplied by the name attribute of the referred to GlobalAttribute
			//    it must be qualified using the namespace prefix of the reference
			// the attribute TYPE is supplied by the typeof the referred to GlobalAttribute

			/*
			 *  prtln ("\t\tcalling globalDefMap.getValue (" + attrType + ") from processInstanceAttribute", 1);
			 *  GlobalDef attrTypeDef = (GlobalDef) globalDefMap.getValue (attrType);
			 *  if (attrTypeDef == null)
			 *  attrTypeDef = new BuiltInType (attrType);
			 */
			GlobalDef attrTypeDef = (GlobalDef) readerStack.getTos().getGlobalDef(attrType);

			prtln("\t\t adding attribute - name: " + attrName + " attrType: " + attrType, 1);
			createAttribute(e, parent, attrName, attrTypeDef);
			popNS();
			SchemaReader popped = readerStack.pop();
			prtln("\n readerStack (" + readerStack.size() + ") popped " + popped.getLocation().toString());
			return;
		}

		throw new Exception("unable to process attribute with globalRef: " + globalDef.toString());
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  e              current schema Element to process
	 * @param  parent         instanceDoc Element to which we attach new instanceElement
	 * @exception  Exception  if this element cannot be processed
	 */
	private void processBuiltIn(Element e, Element parent) throws Exception {
		// BUILT-IN TYPE - but simply bypass extension elements, since they have already
		// been added to the instanceDocument
		if (isExtensionElement(e)) {
			return;
		}

		prtln("\n Built-in (" + e.attributeValue("name") + ")", 1);
		prtln("\tparent: " + parent.asXML(), 1);
		prtln("\te: " + e.asXML(), 1);

		/*
		 *  QName qname = df.createQName(e.attributeValue("name"), getCurrentNamespace());
		 *  Element child = df.createElement(qname);
		 */
		Element child = createChildElement(e.attributeValue("name"));

		prtln("\t ... child: " + child.asXML(), 1);

		// if this element has a fixed value, set it in the instance document
		// an example of a fixed element is
		if (e.attributeValue("fixed") != null) {
			prtln("  ... fixed: " + e.asXML(), 1);
			child.setText(e.attributeValue("fixed"));
		}
		parent.add(child);
		String xpath = getPath(child);
		// BuiltInType typeDef = new BuiltInType (e.attributeValue("type"));

		BuiltInType typeDef = (BuiltInType) readerStack.getTos().getGlobalDef(e.attributeValue("type"));
		prtln("\t processBuiltIn() instantiating SchemaNode with built-in type (name: " + typeDef.getName() + ")", 1);
		SchemaNode schemaNode = new SchemaNode(e, typeDef, xpath, schemaNodeMap);

		// experimental
		if (e.attributeValue("substitutionGroup", null) != null) {
			String sg = e.attributeValue("substitutionGroup");
			prtln("\n\t substitutionGroup: " + sg, 1);
			String sgPrefix = NamespaceRegistry.getNamespacePrefix(sg);
			String sgName = NamespaceRegistry.stripNamespacePrefix(sg);
			String resolvedPrefix = this.getCurrentReader().resolveToInstancePrefix(sgPrefix);
			schemaNode.setHeadElementName(NamespaceRegistry.makeQualifiedName(resolvedPrefix, sgName));
		}
		schemaNodeMap.setValue(xpath, schemaNode);
	}


	/**
	 *  this method will indeed insert the right elements, but we do we want them
	 *  in the instance document?? i'm thinking that we DO want them in the
	 *  schemaNodeMap, but not in the instanceDocument ... so we can remove them
	 *  from the instance document
	 *
	 * @param  globalElementDef  the GlobalElement type definition containing the sub group element
	 * @param  e              current schema Element to process
	 * @param  parent         instanceDoc Element to which we attach new instanceElement
	 * @exception  Exception  if this element cannot be processed
	 */
	private void processSubstitutionGroup(GlobalElement globalElementDef, Element e, Element parent) throws Exception {
		prtln("\nprocessSubstitutionGroup()", 1);

		if (globalElementDef.isAbstract()) {
			prtln("\n\t ABSTRACT GLOBAL ELEMENT!\n", 1);
		}

		Iterator subGroup = globalElementDef.getSubstitutionGroup().iterator();
		prtln("\t substitution group", 1);
		while (subGroup.hasNext()) {
			GlobalElement member = (GlobalElement) subGroup.next();
			prtln("\n Substitution Member: " + member.getQualifiedInstanceName(), 1);
			Namespace memberNS = member.getNamespace();

			prtln("\t namespace obtained from globalDef: " + NamespaceRegistry.nsToString(memberNS), 1);

			pushNS(memberNS);
			readerStack.push(member.getSchemaReader());
			processSchemaElement(member.getElement(), parent);
			this.popNS();
			readerStack.pop();
		}
	}


	/**
	 *  Processes a schemaElement that declares an element in the instanceDocument
	 *  (i.e., the name of this schemaElement is "element").
	 *
	 * @param  e              current schema Element to process
	 * @param  parent         instanceDoc Element to which we attach new instanceElement
	 * @exception  Exception  if this element cannot be processed
	 */
	private void processInstanceElement(Element e, Element parent) throws Exception {
		// find the type Definition for this element's type (or ref) in the globalDefMap

		// Debugging
		prtln("\nprocessInstanceElement()", 1);
		prtln("\t name: " + e.attributeValue("name"), 1);

		if (e.attributeValue("substitutionGroup", null) != null) {
			prtln("\n\t *** SUBSTITUTION MEMBER! ***", 1);
		}

		prtln("\t element: " + e.asXML(), 1);

		GlobalDef globalDef = getElementTypeDef(e);

		prtln("\t globalDef: " + globalDef.getQualifiedInstanceName(), 1);

		if (globalDef == null) {
			throw new Exception("processInstanceElement got a null globalDef");
		}

		/*
		 *  if a definition was not found, the element's type is either a built-in type
		 *  (e.g., "xsd:string") or it is an unknown type, which will throw an error
		 */
		if (globalDef.isBuiltIn()) {
			processBuiltIn(e, parent);
			return;
		}

		/*
		 *  for SimpleTypes and ComplexTypes, we create a SchemaNode for this element and create an
		 *  instanceDocument element. In the case of ComplexTypes, we then expand each of the children
		 */
		if (globalDef.isSimpleType() || globalDef.isComplexType()) {
			String name = e.attributeValue("name");
			prtln("\t Expanding " + name, 1);

			Element child = null;

			if (isExtensionElement(e)) {
				/*
				 *  we don't want to create a new schemaNode for "extension elements", which extend
				 *  a schemaNode that has already been created. Extension elements are chiefly used
				 *  in Simple and ComplexContent elements of ComplexType definitions.
				 *  When extension elements are encountered
				 *  during expansion , a dummy element of the extension's base type is created and
				 *  then expanded in place of the original element.
				 *  (search this buffer for 'elementName.equals("extension")' to see where
				 *  in this method they are processed)
				 */
				prtln("\t Extension Element");
				child = parent;
			}

			else {

				prtln("\n Simple or Complex Type (" + e.attributeValue("name") + ")", 1);
				prtln("\tparent: " + parent.asXML(), 1);
				prtln("\te: " + e.asXML(), 1);

				/*
				 *  QName qname = df.createQName(e.attributeValue("name"), getCurrentNamespace ());
				 *  child = df.createElement(qname);
				 */
				child = createChildElement(e.attributeValue("name"));

				parent.add(child);
				String xpath = getPath(child);
				prtln("\n\t processInstanceElement() instantiating SchemaNode", 1);

				SchemaNode schemaNode = new SchemaNode(e, globalDef, xpath, schemaNodeMap);
				if (e.attributeValue("substitutionGroup", null) != null) {
					String sg = e.attributeValue("substitutionGroup");
					prtln("\n\t substitutionGroup: " + sg, 1);
					String sgPrefix = NamespaceRegistry.getNamespacePrefix(sg);
					String sgName = NamespaceRegistry.stripNamespacePrefix(sg);
					String resolvedPrefix = this.getCurrentReader().resolveToInstancePrefix(sgPrefix);
					schemaNode.setHeadElementName(NamespaceRegistry.makeQualifiedName(resolvedPrefix, sgName));
				}

				prtln("-----------\n" + schemaNode.toString() + "----------------\n", 1);
				schemaNodeMap.setValue(xpath, schemaNode);

				if (schemaNode.isRecursive()) {
					prtln("\n*** RECURSIVE ****  " + schemaNode.getXpath() + "\n", 1);
					return;
				}
			}

			if (globalDef.isSimpleType()) {
				// we've already added the child for this simple type, there is nothing else to do
				prtln(" simple type - kicking out of processSchemaElement", 1);
				return;
			}
			else if (globalDef.isComplexType()) {
				// expand complex types by traversing the type definition
				ComplexType complexType = (ComplexType) globalDef;
				prtln("\n\tcomplex type (" + complexType.getName() + ")\n\t" + complexType.getLocation(), 1);
				prtln("\t getting namespace from complexType" + complexType.toString(), 1);
				prtln("\t - about to iterate", 1);

				Namespace ns = complexType.getNamespace();
				/*
				 *  if (ns.getPrefix().equals("")) {
				 *  prtln ("\t\t NAMESPACE PREFIX IS EMPTY, assigning default namespace", 1);
				 *  ns = defaultNamespace;
				 *  }
				 */
				pushNS(ns);
				readerStack.push(complexType.getSchemaReader());
				prtln("\n" + readerStack.toString(), 1);

				List children = complexType.getChildren();
				for (Iterator i = children.iterator(); i.hasNext(); ) {
					Element childElement = (Element) i.next();
					prtln("\t...with " + childElement.getName(), 1);
					processSchemaElement(childElement, child);
					/* 					try {
						processSchemaElement(childElement, child);
					} catch (Exception expandException) {
						String msg = "processSchemaElement encountered error processing element of complexType: " + expandException.getMessage();
						expandException.printStackTrace();
						throw new Exception(msg);
					} */
				}
				popNS();
				SchemaReader popped = readerStack.pop();
				prtln("\n readerStack (" + readerStack.size() + ") popped " + popped.getLocation().toString());
			}
		}
		else if (globalDef.isGlobalElement()) {
			/*
			 *  we're dealing with a REFERENCE to a global Element (globalDef wraps this global Element)
			 *  we only handle the cases where the referred to global element has a datatype of either built-in or
			 *  GlobalDef.SIMPLE_TYPE!
			 *  in these cases, a SchemaNode is created that points to the referred to type, and processing of this
			 *  branch of the document stops (processSchemaElement is not called recursively);
			 */
			processReferenceToGlobalElement(globalDef, e, parent);
		}
	}


	/**
	 *  Process an exention element of a DerivedContent model. For extensions (used
	 *  in Simple and ComplexContent type definitions, we create a dummy element of
	 *  the extension's base type. The dummy element has an attribute of
	 *  "extension" so it can be identified and handled properly when it is fed
	 *  back into processSchemaElement and expanded as if it were an element of the
	 *  baseType.<p>
	 *
	 *  Finaly the attributes (if any) are processed as if they were defined in the
	 *  parent of the parent element, which is the enclosing ComplexType.
	 *
	 * @param  e              The schema extention element being processed
	 * @param  parent         The instanceDoc parent of the extention (a simple or
	 *      complexContent element)
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	private void processExtension(Element e, Element parent) throws Exception {

		String baseType = e.attributeValue("base");
		String parentName = XPathUtils.getNodeName(getPath(parent));

		prtln("\n processExtension() handling extension (baseType: " + baseType + ")", 1);
		prtln(pp(e));

		// create dummy element
		//  embed namespace information into the dummy element so it is available to "isBuiltInType"
		QName qname = df.createQName("element", e.getNamespace());
		Element extElement = df.createElement(qname);

		extElement.addAttribute("type", baseType);
		extElement.addAttribute("extension", "true");
		extElement.addAttribute("name", parentName);

		prtln("\t ... extn element: " + extElement.asXML() + "\n", 1);

		/*
		 *  // print out some DEBUGGING info
		 *  if (parentName.equals("catalog")) {
		 *  prtln("parent name = " + parentName, 1);
		 *  prtln("base = " + baseType, 1);
		 *  prtln("dummy extension element: " + extElement.asXML(), 1);
		 *  }
		 */
		// this (or something like it must be here to catch the extension attributes?
		processSchemaElement(extElement, parent);

		// take care of the attributes of this extention
		for (Iterator i = e.elementIterator(); i.hasNext(); ) {
			Element grandParent = parent.getParent();
			Element childAttribute = (Element) i.next();
			processSchemaElement(childAttribute, parent);
		}
	}


	/**
	 *  Process restriction element of a ComplexContent, which redefines the
	 *  content model by including only a subset of the elements defined in the
	 *  base. We are not interested in validating the restriction, so we simply
	 *  pass the content of the restriction element back into processSchemaElement
	 *  as if there was no restriction element at all
	 *
	 * @param  e              current schema Element to process
	 * @param  parent         instanceDoc Element to which we attach new instanceElement
	 * @exception  Exception  if this element cannot be processed
	 */
	private void processRestriction(Element e, Element parent) throws Exception {

		String baseType = e.attributeValue("base");
		String parentName = XPathUtils.getNodeName(getPath(parent));

		prtln("\n processRestriction() handling RESTRICTION", 1);
		prtln(pp(e), 1);

		/*
		 *  // print out some DEBUGGING info
		 *  if (parentName.equals("catalog")) {
		 *  prtln("parent name = " + parentName, 1);
		 *  prtln("base = " + baseType, 1);
		 *  prtln("dummy extension element: " + extElement.asXML(), 1);
		 *  }
		 */
		// this (or something like it must be here to catch the extension attributes?
		/* 		Element child = (Element)e.elementIterator().next();
		processSchemaElement(child, parent); */
		for (Iterator i = e.elementIterator(); i.hasNext(); ) {
			Element child = (Element) i.next();
			processSchemaElement(child, parent);
		}

	}


	/**
	 *  Process an element that REFERS to a global Element (globalDef wraps the
	 *  referred-to global Element).<P>
	 *
	 *  E.g., &lt;xs:element ref="sh:album"/><p>
	 *
	 *  Algorith: Add a child to the instanceDocument, and process the
	 *  typeDefinition of the referred-to Global Element.
	 *
	 * @param  globalDef      the referred-to GlobalElement definition
	 * @param  e              the Element to process
	 * @param  parent         parent in the instance document
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	private void processReferenceToGlobalElement(GlobalDef globalDef, Element e, Element parent) throws Exception {
		if (globalDef == null) {
			// When would this occur? For now throw an exception and later figure out if
			// we have to handle it
			throw new Exception("processReferenceToGlobalElement did not receive a globalDef");
		}

		if (!globalDef.isGlobalElement()) {
			// When would this occur? For now throw an exception and later figure out if
			// we have to handle it
			throw new Exception("processReferenceToGlobalElement did not receive a GlobalElement");
		}
		GlobalElement globalElementDef = (GlobalElement) globalDef;

		prtln("\n Reference to Global Element", 2);
		prtln("\t" + e.asXML(), 1);
		prtln("\tglobalDef: " + globalElementDef.toString(), 2);

		if (globalElementDef.hasSubstitutionGroup()) {
			prtln("\n\n\t HAS SubstitutionGroup!\n", 1);
			prtln("\t" + e.asXML(), 1);
			processSubstitutionGroup(globalElementDef, e, parent);
		}

		Namespace ns = globalElementDef.getNamespace();

		prtln("\t namespace obtained from globalElementDef: " + NamespaceRegistry.nsToString(ns), 2);

		pushNS(ns);
		readerStack.push(globalElementDef.getSchemaReader());
		prtln("\n" + readerStack.toString(), 1);

		Element child = createChildElement(globalElementDef.getName());
		parent.add(child);

		// get the type definition for the referred to Global Element
		SchemaReader reader = getSchemaReader(globalElementDef.getLocation());
		if (reader == null) {
			throw new Exception("failed to get SchemaReader for " + ns.getURI());
		}

		prtln("globalElementDef: " + globalElementDef.toString());

		GlobalDef typeDef = (GlobalDef) reader.getGlobalDef(globalElementDef.getType());

		if (typeDef.isTypeDef()) {
			String xpath = getPath(child);
			String type = globalElementDef.getType();
			prtln("\n\t processReferenceToGlobalElement() creating schemaNode for path: " + xpath, 1);

			SchemaNode schemaNode = new SchemaNode(e, typeDef, xpath, schemaNodeMap);
			schemaNode.setIsAbstract(globalElementDef.isAbstract());
			schemaNode.setSubstitutionGroup(globalElementDef.getSubstitutionGroup());
			schemaNodeMap.setValue(xpath, schemaNode);

			if (schemaNode.isRecursive()) {
				prtln("\n*** RECURSIVE ****  " + schemaNode.getXpath() + "\n", 1);
				return;
			}

			// process the children of a complex type
			if (typeDef.isComplexType() && !schemaNode.isRecursive()) {
				prtln("handling a complex globalElementDef", 1);
				ComplexType complexType = (ComplexType) typeDef;
				prtln("complex type (" + complexType.getName() + ")\n\t" + complexType.getLocation(), 1);
				prtln(" - about to iterate", 1);
				List children = complexType.getChildren();
				for (Iterator i = children.iterator(); i.hasNext(); ) {
					Element childElement = (Element) i.next();
					prtln("\t...with " + childElement.getName(), 1);
					try {
						processSchemaElement(childElement, child);
					} catch (Throwable expandException) {
						String msg = "processReferenceToGlobalElement choked on the following element\n";
						expandException.printStackTrace();
						throw new Exception(msg + childElement.asXML());
					}
				}
			}
		}
		else {
			String msg = "processSchemaElement encountered unknown GlobalDef type: " + typeDef.getDataType();
			throw new Exception(msg + "\n" + typeDef.getElement().asXML());
		}
		popNS();
		SchemaReader popped = readerStack.pop();
		prtln("\n readerStack (" + readerStack.size() + ") popped " + popped.getLocation().toString());

	}


	/**
	 *  get the XPath to this element relative to the given context currently, this
	 *  method will print an XPath that is of the form /itemRecord/educational/....
	 *  this form can be altered using different context and prefix
	 *
	 * @param  n  Description of the Parameter
	 * @return    The path value
	 */
	public String getPath(Node n) {
		String prefix = "/";
		Element context = hatrack;
		return prefix + n.getPath(context);
	}


	/**
	 *  Gets the urisFromNR attribute of the StructureWalker object
	 *
	 * @return    The urisFromNR value
	 */
	private Map getUrisFromNR() {
		Map uris = new HashMap();
		for (Iterator i = namespaces.getPrefixMap().keySet().iterator(); i.hasNext(); ) {
			String prefix = (String) i.next();
			Namespace ns = namespaces.getNSforPrefix(prefix);
			/*
			 *  if (prefix.trim().length() > 0)
			 *  uris.put(prefix, ns.getURI());
			 */
			uris.put(prefix, ns.getURI());
		}
		return uris;
	}


	/**
	 *  Gets the path attribute of the StructureWalker object
	 *
	 * @param  a  Description of the Parameter
	 * @return    The path value
	 */
	public String getPath(Attribute a) {
		prtln("getPath: attribute: " + a.asXML(), 1);
		prtln(" ... attributeName: " + a.getName(), 1);
		prtln(" ... qualifiedName: " + a.getQualifiedName(), 1);
		Element parent = a.getParent();
		if (parent == null) {
			prtln(" ... parent is null!", 1);
		}
		// return getPath((Node) a);
		return getPath(parent) + "/@" + a.getQualifiedName();
	}


	/**
	 *  Gets the path attribute of the StructureWalker object
	 *
	 * @param  e  Description of the Parameter
	 * @return    The path value
	 */
	public String getPath(Element e) {
		return getPath((Node) e);
	}


	/**
	 *  Sets the debug attribute of the StructureWalker class
	 *
	 * @param  bool  The new debug value
	 */
	public static void setDebug(boolean bool) {
		debug = bool;
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  n  NOT YET DOCUMENTED
	 * @return    NOT YET DOCUMENTED
	 */
	private String pp(Node n) {
		return Dom4jUtils.prettyPrint(n);
	}


	/**
	 *  Description of the Method
	 *
	 * @param  s      Description of the Parameter
	 * @param  level  Description of the Parameter
	 */
	static void prtln(String s, int level) {
		if (debug && (level > debugThreshold)) {

			while (s.length() > 0 && s.charAt(0) == '\n') {
				System.out.println("");
				s = s.substring(1);
			}

			// System.out.println("Structure Walker: " + s);
			System.out.println("Walker: " + s);
			// System.out.println(s);
		}
	}


	/**
	 *  Description of the Method
	 *
	 * @param  s  Description of the Parameter
	 */
	static void prtln(String s) {
		if (debug) {
			prtln(s, 0);
		}
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  s  NOT YET DOCUMENTED
	 */
	static void box(String s) {
		SchemaHelper.box(s, "Walker");
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  s  NOT YET DOCUMENTED
	 */
	private final void prtlnErr(String s) {
		System.err.println("StructureWalker: " + s);
	}

}

