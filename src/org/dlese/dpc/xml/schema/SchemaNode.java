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

import org.dlese.dpc.xml.schema.compositor.*;
import org.dlese.dpc.xml.Dom4jUtils;
import org.dlese.dpc.xml.XPathUtils;

import java.lang.*;
import java.util.*;

import org.dom4j.Element;
import org.dom4j.Attribute;
import org.dom4j.Node;

/**
 *  SchemaNode wraps a node in the Schema's instance document, and stores
 *  information about it such as whether the node is an attribute or element, if
 *  is required, and its xpath. SchemaNodes are stored in a {@link
 *  org.dlese.dpc.xml.schema.SchemaNodeMap} that is keyed by XPaths.
 *
 *@author    ostwald<p>
 *
 *      $Id $
 */
public class SchemaNode {
	private static boolean debug = true;
	private static boolean schemaDocAware = false;
	
	private short nodeType = Node.UNKNOWN_NODE;

	private SchemaNodeMap schemaNodeMap = null;
	private GlobalDef typeDef = null;
	private HashMap attMap = null;
	private HashMap propMap = null;
	
	private GlobalDef validatingType = null;
	private String xpath = null;
	private boolean readOnly = false;
	private int maxOccurs = -1;
	private int minOccurs = -1;
	private int docOrderIndex = -1;
	private List substitutionGroup = new ArrayList();
	private Element substitutionElement = null;
	private String headElementName = "";
	private String documentation = null;
	private boolean isChoiceMember;


	/**
	 *  Constructor for the SchemaNode object used for Schema elements that refer
	 *  to Global Elements, such as attributes and elements
	 *
	 *@param  typeName      data type of the referred-to element
	 *@param  e             schema element that refers to a global element
	 *@param  xpath         Description of the Parameter
	 *@param  typeDef  Description of the Parameter
	 */
	public SchemaNode(Element e, GlobalDef typeDef, String xpath,  SchemaNodeMap schemaNodeMap) {
		// prtln ("schemaNode #1 (" + xpath + ")");
		this.typeDef = typeDef;
		this.schemaNodeMap = schemaNodeMap;
		this.xpath = xpath;
		init(e);
	}

	/**
	 *  SchemaNode constructor used for schema elements defined as SimpleType or
	 *  ComplexType types. From the element we can extract attributes such as
	 *  occurrence information minOccurs and MaxOccurs. From the typeDef we can
	 *  determine the validatingDataType (in the case of ComplexTypes with
	 *  simpleContent or complexContent).
	 *
	 *@param  e             Description of the Parameter
	 *@param  typeDef     Description of the Parameter
	 *@param  xpath         Description of the Parameter
	 */
	public SchemaNode(Element e, GlobalDef typeDef, String xpath, SchemaNodeMap schemaNodeMap, GlobalDef validatingType) {
		// prtln ("schemaNode #2 (" + xpath + ")");
		
		this.typeDef = typeDef;
		this.schemaNodeMap = schemaNodeMap;
		this.xpath = xpath;
		this.validatingType = validatingType;
		init(e);
		if (this.isAbstract()) {
			prtln ("\n\n*** ABSTRACT NODE at " + xpath);
		}
	}
	
	/**
	 *  Description of the Method
	 *
	 *@param  e  Description of the Parameter
	 */
	private void init(Element e) {
		// prtln ("init with:\n" + e.asXML());
		if (validatingType == null) {
			// validatingType = typeDef;
			validatingType = findValidatingType(getTypeDef());
		}
		setIsChoiceMember();
		attMap = new HashMap();
		propMap = new HashMap();
		String nodeTypeString = e.getName();

		if (nodeTypeString.equals("element") || nodeTypeString.equals ("any")) {
			nodeType = Node.ELEMENT_NODE;
			
			minOccurs = SchemaHelper.getMinOccurs(e);
			maxOccurs = SchemaHelper.getMaxOccurs(e);
			// prtln ("minOccurs: " + minOccurs + "  maxOccurs: " + maxOccurs);
			
			String nillable = e.attributeValue("nillable", SchemaHelper.NILLABLE_DEFAULT);
			if ((nillable != null) && (nillable.trim().length() > 0)) {
				attMap.put("nillable", nillable);
			}

			if (e.attributeValue("fixed", null) != null) {
				readOnly = true;
			}
			
			attMap.put ("substitutionGroup", e.attributeValue("substitutionGroup", ""));
			attMap.put ("abstract", e.attributeValue("abstract", ""));
		}

		else if (nodeTypeString.equals("attribute")) {
			nodeType = Node.ATTRIBUTE_NODE;
			String use = e.attributeValue("use");
			if ((use != null) && (use.trim().length() > 0)) {
				attMap.put("use", use);
			}
		}
		else {
			nodeType = Node.UNKNOWN_NODE;
		}
		
		if (schemaDocAware)
			extractDocumentation (e);
		
		// prtln ("Instantiated SchemaNode\n" + toString());
	}

	private void extractDocumentation (Element e) {
		Element annotationEl = e.element ("annotation");
		if (annotationEl == null) {
			return;
		}
		Element docEl = annotationEl.element("documentation");
		if (docEl == null) {
			return;
		}
		this.setDocumentation(docEl.getTextTrim());
	}
		


	/**
	* NOT USED!! Only override existing attributes if the particular attribute is explicitly defined in the
	donor element.
	*/
	public void assignAttributes (Element donor) {
		// prtln ("assignAttributes()");
		
		String minOccurs = donor.attributeValue ("minOccurs");
		if (minOccurs != null && minOccurs.trim().length() > 0) {
			attMap.put("minOccurs", minOccurs);
			this.minOccurs = -1;
		}
		
		String maxOccurs = donor.attributeValue ("maxOccurs");
		if (maxOccurs != null && maxOccurs.trim().length() > 0) {
			attMap.put("maxOccurs", maxOccurs);
			this.maxOccurs = -1;
		}
		
		String nillable = donor.attributeValue ("nillable");
		if (nillable != null && nillable.trim().length() > 0)
			attMap.put("nillable", nillable);
		
		if (donor.attributeValue("fixed", null) != null) {
			readOnly = true;
		}
	}

	public String getDocumentation () {
		return this.documentation;
	}
	
	public void setDocumentation (String doc) {
		// prtln ("setting Documentation for " + this.xpath);
		if (doc != null && doc.trim().length() > 0)
			this.documentation = doc.trim();
		else
			this.documentation = null;
	}
	
	public int getMaxOccurs () {
		return maxOccurs;
	}
		
	public int getMinOccurs () {
		return minOccurs;
	}

	/**
	 *  Gets the unbounded attribute of the SchemaNode object
	 *
	 *@return    The unbounded value
	 */
	public boolean isUnbounded() {
		return (getMaxOccurs() == SchemaHelper.UNBOUNDED);
	}


	/**
	 *  Gets the readOnly attribute of the SchemaNode object
	 *
	 *@return    The readOnly value
	 */
	public boolean isReadOnly() {
		return readOnly;
	}


	/**
	 *  Sets the readOnly attribute of the SchemaNode object
	 *
	 *@param  readOnly  The new readOnly value
	 */
	public void setReadOnly(boolean readOnly) {
		this.readOnly = readOnly;
	}

	public int getDocOrderIndex () {
		return docOrderIndex;
	}
	
	public void setDocOrderIndex (int i) {
		docOrderIndex = i;
	}

	/**
	 *  Gets the nillable attribute of the SchemaNode object
	 *
	 *@return    The nillable value
	 */
	public boolean isNillable() {
		String m = getAttr("nillable");
		return m.equals("true");
	}

	public boolean isAbstract () {
		String m = getAttr("abstract");
		return m.equals("true");
	}
	
	public void setIsAbstract (boolean b) {
		if (b)
			attMap.put("abstract", "true");
		else
			attMap.put ("abstract", "");
	}
	
	public List getSubstitutionGroup () {
		return substitutionGroup;
	}
	
	/**
	 * A "headElement" has a substitutionGroup of elements that may be substituted
	 * for it.
	 */
	public boolean isHeadElement () {
		return !substitutionGroup.isEmpty();
	}
	
	public Element getSubstitutionElement () {
		return (Element)substitutionElement.clone();
	}
	
	/**
	 * SubstitutionElement is the instance Element, complete with children, 
	 for this schemaNode. Since this is a substitution element, we don't want
	 to recompute it.
	 NOTE: why do individual schemaNodes have to store this element? 
	 why isn't it just stored ONCE (i.e., in the headElement?) 
	 */
	public void setSubstitutionElement (Element e) {
		substitutionElement = e;
	}
	
	public String getHeadElementName () {
		return headElementName;
	}
	
	public void setHeadElementName (String name) {
		headElementName = name;
	}
	
	/**
	* SubstitutionGroup is a list of GlobalElements
	*/
	public void setSubstitutionGroup (List sg) {
		substitutionGroup = sg;
	}
	
	public boolean isSubstitutionGroupMember () {
		return !getAttr("substitutionGroup").equals("");
	}
	
	public boolean isRepeatingCompositorMember () {
		return getParentCompositorMaxOccurs() > 1;
	}
	
	/**
	* Get the "maxOccurs" value of this SchemaNode's parent compositor.<p>
	Return 1 if there is no parentCompositor - is this OK?
	*/
	public int getParentCompositorMaxOccurs () {
		Compositor parentCompositor = getParentCompositor();
		if (parentCompositor == null) {
			return 1;
		}
		return parentCompositor.getMaxOccurs();
	}
	
	/**
	* Returns true if this SchemaNode is the only child of a compositor
	*/
	public boolean isCompositorSingleton () {
		Compositor parentCompositor = this.getParentCompositor();
		return parentCompositor != null && parentCompositor.getMembers().size() == 1;
	}
		
	
	/**
	 *  Gets the attribute attribute of the SchemaNode object
	 *
	 *@return    The attribute value
	 */
	public boolean isAttribute() {
		return (getNodeType() == Node.ATTRIBUTE_NODE);
	}


	/**
	 *  Gets the element attribute of the SchemaNode object
	 *
	 *@return    The element value
	 */
	public boolean isElement() {
		return (getNodeType() == Node.ELEMENT_NODE);
	}

	/**
	 *  Gets the required attribute of the SchemaNode object
	 *
	 *@return    The required value
	 */
	public boolean isRequired() {
		if (isElement()) {
			return minOccurs > 0;
		}
		if (isAttribute()) {
			String use = getAttr("use");
			return use.equals("required");
		}
		return false;
	}
	
	/** gets names of the attributes of the schema element (NOT the instance element!)
	*/
	public List getAttributeNames () {
		List names = new ArrayList();
		Set keys = attMap.keySet();
		if (keys != null) {
			names = new ArrayList (keys);
			Collections.sort(names);
		}
		return names;
	}

	
	/**
	 *  Gets the attr attribute of the SchemaNode object
	 *
	 *@param  key  Description of the Parameter
	 *@return      The attr value
	 */
	public String getAttr(String key) {
		String val = (String) attMap.get(key);
		if (val == null) {
			return "";
		}
		return val;
	}

		/**
	 *  Gets the attr attribute of the SchemaNode object
	 *
	 *@param  key  Description of the Parameter
	 *@return      The attr value
	 */
	public Object getProp(String key) {
		return this.propMap.get(key);
	}

	/**
	 *  Gets the xpath for this SchemaNode in the context of the SchemaHelper.instanceDocument.
	 *
	 *@return    The xpath value
	 */
	public String getXpath() {
		return xpath;
	}


	public GlobalDef getTypeDef () {
		return typeDef;
	}
	

	public boolean isDerivedModel () {
		GlobalDef typeDef = getTypeDef();
		if (typeDef == null || !typeDef.isComplexType())
			return false;
		return ((ComplexType)typeDef).isDerivedType();
	}

	public boolean isDerivedTextOnlyModel() {
		GlobalDef typeDef = getTypeDef();
		if (typeDef == null || !typeDef.isComplexType())
			return false;
		return ((ComplexType)typeDef).isDerivedTextOnlyModel();
	}		
	
	public boolean isDerivedContentModel () {
		return (isDerivedModel() && ((ComplexType)typeDef).isDerivedContentModel());
	}
	
	/**
	 *  Gets the validatingTypeName attribute of the SchemaNode object. This is the
	 *  name of the XSDatatype object that will be used to validate values for this
	 *  node.
	 *
	 *@return    The validatingTypeName value
	 */
	public GlobalDef getValidatingType () {
		return validatingType;
	}

	/**
	 *  Finds a validating type for ComplexTypes defining derived Models. For these cases, 
	 	the validatingType is found by recursively traversing
	 *  the type definition tree until a Simple or Built-in data type is located
	 *  for the extension element.
	 *
	 *@param  globalDef  Type definnition for which we want to find the validating type
	 *@return            The type definition of the validating typep
	 */
	private GlobalDef findValidatingType (GlobalDef typeDef) {
		// if typeDef is not complex, then we are done - simply return the typeDef
		if (typeDef.isComplexType()) {
			/*
			   we are working with a ComplexType. If it has an exension type (i.e., if it is a derived model)
			   then we must determine the validating type for the extension element. 
		   */
			ComplexType complexType = (ComplexType) typeDef;
			GlobalDef extnType = complexType.getExtensionType();
			return (extnType == null) ? typeDef : findValidatingType(extnType);
		}
		return typeDef;
	}
	
	private Compositor getParentCompositor () {
		SchemaNode parent = getParent();
		if (parent != null)
			return parent.getCompositor();
		else
			return null;
	}
	
	public boolean hasCompositor () {
		return this.getCompositor() != null;
	}
	
	public boolean isCompositorMember () {
		return getParentCompositor() != null;
	}
	
	public boolean hasChoiceCompositor () {
		return hasCompositorType (Compositor.CHOICE);
	}
	
	public boolean hasSequenceCompositor () {
		return hasSequenceCompositorSIMPLE();
	}
	
	/* simple version */
	public boolean hasSequenceCompositorSIMPLE () {
		return hasCompositorType (Compositor.SEQUENCE);
	}
	
	/** EXPERIMENTAL */
	public boolean hasSequenceCompositorEXPERIMENTAL () {
		if (hasCompositorType (Compositor.SEQUENCE))
			return true;
		if (isDerivedModel()) {
			ComplexType typeDef = (ComplexType)getTypeDef();
			return typeDef.getDerivedCompositorType() == Compositor.SEQUENCE;
/* 			try {
				ComplexType typeDef = (ComplexType)getTypeDef();
				ComplexType extensionRootType = (ComplexType) typeDef.getExtensionRootType();
				prtln ("\t ... extensionRootType: " + extensionRootType.getName());
				return (extensionRootType.getCompositorType() == Compositor.SEQUENCE);
			} catch (Throwable t) {
				prtln ("hasSequenceCompositor: " + t);
				t.printStackTrace();
			} */
		}
		return false;
	}
	
	private boolean hasCompositorType (int compositorType) {
		Compositor c = getCompositor();
		return (c != null && c.getType() == compositorType);
	}
	
	/**
	* Returns the compositor associated with this SchemaNode's typeDef, or null if
	* the typeDef does not have a Compositor.
	*/
	public Compositor getCompositor () {
		GlobalDef typeDef = getTypeDef();
		if ((typeDef != null) && typeDef.isComplexType()) {
			ComplexType complexType = (ComplexType) typeDef;
			return complexType.getCompositor();
		}
		return null;
	}
	
	public SchemaNode getParent () {
		return (SchemaNode) schemaNodeMap.getValue(XPathUtils.getParentXPath(getXpath()));
	}
	
	public boolean isRecursive() {
		// prtln ("isRecursive");
		int max_levels = 4;
		String RECURSIVE = "recursive";
		int level = 0;
		if (!this.propMap.containsKey(RECURSIVE)) {
			boolean recursive = false;
			List anscestorTypes = new ArrayList();
			SchemaNode ptr = getParent();
			anscestorTypes.add (this.getTypeDef());
			while (ptr != null) {
				GlobalDef typeDef = ptr.getTypeDef();
				if (anscestorTypes.contains(typeDef)) {
					//level++;
					// prtln ("\t" + typeDef.getName() + " (" + level + "/" + levels + ")");
					if (++level >= max_levels) {
						recursive = true;
						break;
					}
				}
				anscestorTypes.add (typeDef);
				ptr = ptr.getParent();
			}
			this.propMap.put (RECURSIVE, recursive);
		}
		return (Boolean)this.propMap.get (RECURSIVE);
	}
	
	private void setIsChoiceMember () {
		// prtln ("\nsetIsChoiceMember for xpath: " + getXpath());
		SchemaNode parentNode = getParent();
		if (parentNode == null) {
			// prtln ("\t parentNode not found - isChoiceMember is false");
			isChoiceMember = false;
			return;
		}
		if (!parentNode.hasChoiceCompositor()) {
			// prtln ("\t parentNode does not have a choice compositor - isChoiceMember is false");
			isChoiceMember = false;
			return;
		}
		
		ComplexType parentTypeDef = (ComplexType)parentNode.getTypeDef();
		Choice choiceCompositor = (Choice) parentTypeDef.getCompositor ();
		// prtln (choiceCompositor.toString());
		String elementName = XPathUtils.getNodeName(getXpath());
		// prtln ("\t looking for \"" + elementName + "\" ..");
		
		isChoiceMember = choiceCompositor.hasMember (elementName);
		if (!isChoiceMember) {
			// prtln ("\t elementName not found in members - returning false");
		}
		else {
			// prtln ("\t elementName FOUND in members - returning true");
			// prtln ("isChoiceMember: " + xpath);
		}
	}

	public boolean getIsChoiceMember () {
		return isChoiceMember;
	}
	
	/**
	 *  Gets the nodeType attribute of the SchemaNode object (corresponding to the
	 *  type of XML node this schemaNode wraps). Value will be one of
	 *  Node.ATTRIBUTE_NODE, Node.ELEMENT_NODE, or Node.UNKNOWN_NODE
	 *
	 *@return    The nodeType value
	 */
	public short getNodeType() {
		return nodeType;
	}


	/**
	 *  Produces string representation for debugging purposes.
	 *
	 *@return    Description of the Return Value
	 */
	public String toString() {
		StringBuffer s = new StringBuffer();
		s.append ("\n xpath: " + getXpath());
		if (getTypeDef() == null)
			s.append("\n\t dataType: NOT FOUND!");
		else
			s.append("\n\t dataType: " + getTypeDef().getQualifiedName());
		s.append ("\n\t nodeType: " + getNodeType());
		s.append ("\n\t minOccurs: " + getMinOccurs());
		s.append ("\n\t maxOccurs: " + getMaxOccurs());
		s.append ("\n\t headElementName: " + getHeadElementName());
		s.append("\n\t attributes:");
		Set keys = attMap.keySet();
		for (Iterator i = keys.iterator(); i.hasNext(); ) {
			String key = (String) i.next();
			String value = (String) attMap.get(key);
			s.append("\n\t\t" + key + ": " + value);
		}
		s.append ("\n");
		return s.toString();
	}


	/**
	 *  Description of the Method
	 *
	 *@param  s  Description of the Parameter
	 */
/* 	public static void prtln(String s) {
		if (debug)
			System.out.println("SchemaNode: " + s);
	} */
	
	public static void prtln(String s) {
		if (debug) {
			if (s.charAt(0) == '\n') {
				System.out.println ("\n");
				s = s.substring(1);
			}
			System.out.println("SchemaNode: " + s);
		}
	}
}

