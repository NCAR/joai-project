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

import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.Namespace;

import java.util.*;
import java.io.*;

/**
 *  Wrapper for GenericType definitions ({@link org.dom4j.Element}).
 *
 * @author    Jonathan Ostwald
 */
public class GenericType implements GlobalDef {
	private static boolean debug = false;
	private static boolean schemaDocAware = false;
	/**  NOT YET DOCUMENTED */
	protected String name;
	/**  NOT YET DOCUMENTED */
	protected String location;
	/**  NOT YET DOCUMENTED */
	protected Element element;
	/**  NOT YET DOCUMENTED */
	protected String path;
	/**  NOT YET DOCUMENTED */
	protected String type;
	/**  NOT YET DOCUMENTED */
	protected Namespace namespace;
	/**  NOT YET DOCUMENTED */
	protected String xsdPrefix;
	/**  NOT YET DOCUMENTED */
	protected SchemaReader schemaReader;
	private String documentation = null;
	/**  NOT YET DOCUMENTED */
	protected boolean inline = false;
	private List children = null;


	/**
	 *  Constructor for the GenericType object
	 *
	 * @param  name  NOT YET DOCUMENTED
	 */
	public GenericType(String name) {
		this(name, null);
	}


	/**
	 *  Constructor for the GenericType object
	 *
	 * @param  name       NOT YET DOCUMENTED
	 * @param  namespace  NOT YET DOCUMENTED
	 */
	public GenericType(String name, Namespace namespace) {
		this.xsdPrefix = null;
		this.location = "";
		this.name = name;
		this.element = null;
		this.namespace = namespace;
		this.path = null;
		this.type = null;
	}


	/**
	 *  Constructor - accounting for location and schemaReader.
	 *
	 * @param  element       NOT YET DOCUMENTED
	 * @param  location      NOT YET DOCUMENTED
	 * @param  namespace     NOT YET DOCUMENTED
	 * @param  schemaReader  NOT YET DOCUMENTED
	 */
	public GenericType(Element element, String location, Namespace namespace, SchemaReader schemaReader) {
		this.xsdPrefix = schemaReader.getNamespaces().getSchemaNamespace().getPrefix();
		this.location = location;
		this.name = element.attributeValue("name");
		this.element = element;
		this.namespace = namespace;
		this.schemaReader = schemaReader;
		this.path = element.getUniquePath();
		this.type = element.getName();
		if (schemaDocAware)
			extractDocumentation();
		else
			filterChildren(xsdPrefix + ":annotation");
	}


	/**  Find documentation within this type definition. */
	public void extractDocumentation() {
		Element docElement = (Element) this.element.selectSingleNode(this.xsdPrefix + ":annotation/" +
			this.xsdPrefix + ":documentation");
		if (docElement != null) {
			String docString = docElement.getTextTrim();
			if (docString != null && docString.length() > 0)
				this.documentation = docString;
			else
				this.documentation = null;
		}
	}


	/**
	 *  All instances of GenericType are type definitions.
	 *
	 * @return    true
	 */
	public boolean isTypeDef() {
		return true;
	}


	/**
	 *  Is this definition explicitly named or is in an "inline" definition (i.e.,
	 *  not explicitly named).
	 *
	 * @return    true if this type definition is defined "inline"
	 */
	public boolean isInline() {
		return this.inline;
	}


	/**
	 *  Sets the inline attribute of the GenericType object
	 *
	 * @param  bool  The new inline value
	 */
	public void setInline(boolean bool) {
		this.inline = bool;
	}


	/**
	 *  Gets the simpleType attribute of the GenericType object
	 *
	 * @return    The simpleType value
	 */
	public boolean isSimpleType() {
		return (getDataType() == SIMPLE_TYPE);
	}


	/**
	 *  Gets the complexType attribute of the GenericType object
	 *
	 * @return    The complexType value
	 */
	public boolean isComplexType() {
		return (getDataType() == COMPLEX_TYPE || getDataType() == MODEL_GROUP);
	}


	/**
	 *  The xsdPrefix is the prefix (e.g., "xsd") used to refer to the Schema
	 *  Datatype namespace (htp:/www.w3.org/2001/XMLSchema.
	 *
	 * @return    The xsdPrefix value
	 */
	public String getXsdPrefix() {
		return xsdPrefix;
	}


	/**
	 *  Gets the schemaReader attribute of the GenericType object
	 *
	 * @return    The schemaReader value
	 */
	public SchemaReader getSchemaReader() {
		return schemaReader;
	}


	/**
	 *  Gets the globalDeclaration attribute of the GenericType object
	 *
	 * @return    The globalDeclaration value
	 */
	public boolean isGlobalDeclaration() {
		return false;
	}


	/**
	 *  Gets the globalElement attribute of the GenericType object
	 *
	 * @return    The globalElement value
	 */
	public boolean isGlobalElement() {
		return false;
	}


	/**
	 *  Gets the globalAttribute attribute of the GenericType object
	 *
	 * @return    The globalAttribute value
	 */
	public boolean isGlobalAttribute() {
		return false;
	}


	/**
	 *  Gets the attributeGroup attribute of the GenericType object
	 *
	 * @return    The attributeGroup value
	 */
	public boolean isAttributeGroup() {
		return false;
	}


	/**
	 *  Gets the builtIn attribute of the GenericType object
	 *
	 * @return    The builtIn value
	 */
	public boolean isBuiltIn() {
		return (getDataType() == BUILT_IN_TYPE);
	}

	public boolean isAnyType() {
		return (isBuiltIn() &&((BuiltInType)this).isAnyType());
	}

	/**
	 *  Gets the modelGroup attribute of the GenericType object
	 *
	 * @return    The modelGroup value
	 */
	public boolean isModelGroup() {
		return (getDataType() == MODEL_GROUP);
	}


	/**
	 *  Gets the dataType attribute of the GenericType object
	 *
	 * @return    The dataType value
	 */
	public int getDataType() {
		return GENERIC_TYPE;
	}


	/**
	 *  Gets the type attribute of the GenericType object
	 *
	 * @return    The type value
	 */
	public String getType() {
		return type;
	}


	/**
	 *  Gets the name attribute of the GenericType object
	 *
	 * @return    The name value
	 */
	public String getName() {
		return name;
	}


	/**
	 *  Gets the qualifiedName attribute of the GenericType object
	 *
	 * @return    The qualifiedName value
	 */
	public String getQualifiedName() {
		return NamespaceRegistry.makeQualifiedName(getNamespace(), getName());
	}


	/**
	 *  Gets a qualified name using the prefix for namespace as defined at the
	 *  INSTANCE level. <p>
	 *
	 *  NOTE: in some cases namespaces occurring in included schemas may not be
	 *  defined at the instance level!
	 *
	 * @return    The qualifiedInstanceName value
	 */
	public String getQualifiedInstanceName() {
		String prefix = schemaReader.getInstanceNamespaces().getPrefixforNS(getNamespace());
		return NamespaceRegistry.makeQualifiedName(prefix, getName());
	}


	/**
	 *  Gets the namespace attribute of the GenericType object
	 *
	 * @return    The namespace value
	 */
	public Namespace getNamespace() {
		return namespace;
	}


	/**
	 *  Gets the location attribute of the GenericType object
	 *
	 * @return    The location value
	 */
	public String getLocation() {
		return location;
	}


	/**
	 *  Gets the element attribute of the GenericType object
	 *
	 * @return    The element value
	 */
	public Element getElement() {
		return element;
	}


	/**
	 *  Gets the elementAsXml attribute of the GenericType object
	 *
	 * @return    The elementAsXml value
	 */
	public String getElementAsXml() {
		return element.asXML();
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @return    NOT YET DOCUMENTED
	 */
	public String toString() {
		String s = "";
		String nl = "\n\t";
		s += nl + "type: " + type;
		s += nl + "location: " + getLocation();
		if (getNamespace() != null && getNamespace() != Namespace.NO_NAMESPACE)
			s += nl + "namespace: " + getNamespace().getPrefix() + ": " + getNamespace().getURI();
		else
			s += nl + "namespace: null";
		s += nl + this.getElementAsXml();
		return s;
	}


	/**
	 *  Removes all elements of type elementName
	 *
	 * @param  qualifiedElementName  NOT YET DOCUMENTED
	 */
	public void filterChildren(String qualifiedElementName) {
		filterChildren(qualifiedElementName, element);
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  qualifiedElementName  NOT YET DOCUMENTED
	 * @param  parent                NOT YET DOCUMENTED
	 */
	public void filterChildren(String qualifiedElementName, Element parent) {
		List children = parent.elements();
		for (int i = children.size() - 1; i > -1; i--) {
			Element child = (Element) children.get(i);
			if (child.getQualifiedName().equals(qualifiedElementName))
				parent.remove(child);
			else
				filterChildren(qualifiedElementName, child);
		}
	}


	/**
	 *  Gets the firstChild attribute of the GenericType object
	 *
	 * @return    The firstChild value
	 */
	public Element getFirstChild() {
		List children = getChildren();
		if (children.size() < 1)
			return null;
		return (Element) children.get(0);
	}


	/**
	 *  Returns (filtered) list of child elements
	 *
	 * @return    The children value
	 */
	public List getChildren() {
		if (this.children == null) {
			this.children = new ArrayList();
			Iterator i = element.elementIterator();
			while (i.hasNext()) {
				Element e = (Element) i.next();
				if (!e.getQualifiedName().equals(this.xsdPrefix + ":annotation"))
					this.children.add(e);
			}
		}
		return this.children;
	}


	/**
	 *  Gets the documentation attribute of the GenericType object
	 *
	 * @return    The documentation value
	 */
	public String getDocumentation() {
		return this.documentation;
	}


	/**
	 *  Gets the enumerationType attribute of the GenericType object
	 *
	 * @return    The enumerationType value
	 */
	public boolean isEnumerationType() {
		// prtln ("\nGenericType: isEnumerationType() with " + getName());

		// must be a simple type
		if (!isSimpleType()) {
			return false;
		}

		SimpleType simpleType = (SimpleType) this;
		if (simpleType.isEnumeration()) {
			return true;
		}

		// unions can be enumerations if each of the members are enumerations
		if (simpleType.isUnion()) {
			List memberTypeDefs = simpleType.getUnionMemberTypeDefs();
			boolean membersAreEnumeration = true;
			for (Iterator i = memberTypeDefs.iterator(); i.hasNext(); ) {
				GenericType memberTypeDef = (GenericType) i.next();
				membersAreEnumeration = memberTypeDef.isEnumerationType();
				if (!membersAreEnumeration) {
					break;
				}
			}
			return membersAreEnumeration;
		}
		return false;
	}


	/**
	 *  Gets the enumerationValues attribute of the GenericType object
	 *
	 * @return    The enumerationValues value
	 */
	public List getEnumerationValues() {
		return getEnumerationValues(true);
	}


	/**
	 *  Gets the enumerationValues attribute of the GenericType object
	 *
	 * @param  getLeafValues  NOT YET DOCUMENTED
	 * @return                The enumerationValues value
	 */
	public List getEnumerationValues(boolean getLeafValues) {
		// prtln ("getEnumerationValues () for " + this.getQualifiedInstanceName());

		if (!isSimpleType()) {
			// prtln ("\t NOT a simple type - returning null");
			return null;
		}

		SimpleType simpleType = (SimpleType) this;

		if (simpleType.isEnumeration()) {
			// prtln ("\t isEnumeration");
			if (getLeafValues) {
				String leafTypeName = makeLeafTypeName(simpleType.getName());

				GlobalDef leafTypeNode = getSchemaReader().getGlobalDef(leafTypeName);

				if (leafTypeNode != null && leafTypeNode.isSimpleType()) {
					return ((SimpleType) leafTypeNode).getSimpleEnumerationValues();
				}
				else {
					return simpleType.getSimpleEnumerationValues();
				}
			}
			else {
				return simpleType.getSimpleEnumerationValues();
			}
		}
		if (simpleType.isUnion()) {
			// prtln ("\t isUnion");
			ArrayList allValues = new ArrayList(); // collector for enumeration values
			List memberTypeDefs = simpleType.getUnionMemberTypeDefs();
			// prtln ("\t " + memberTypeDefs.size() + " member types");

			for (Iterator i = memberTypeDefs.iterator(); i.hasNext(); ) {
				GenericType memberTypeDef = (GenericType) i.next();
				List values = memberTypeDef.getEnumerationValues(getLeafValues);
				if (values == null) {
					// comboUnionTypes will have one memeber that has no enumeration values
					if (!simpleType.isComboUnionType()) {
						prtln("WARNING: getEnumerationValues got a comboUnionType: " + memberTypeDef.getQualifiedName());
					}
				}
				else {
					allValues.addAll(values);
				}
			}
			return allValues;
		}
		return null;
	}


	/**
	 *  Create the leafType name of an enumerated type. Many of the enumarations
	 *  have a set of human readable values, or "leafTypes". By convention, these
	 *  leaftypes are named with "Leaf" inserted in before the "Type" in the base
	 *  name. for example, the leaf type name for "FooType" would be "FooLeafType"
	 *
	 * @param  valueTypeName  Description of the Parameter
	 * @return                Description of the Return Value
	 */
	private static String makeLeafTypeName(String valueTypeName) {
		int x = valueTypeName.lastIndexOf("Type");
		if (x == -1) {
			return "";
		}
		return valueTypeName.substring(0, x) + "LeafType";
	}


	/**  NOT YET DOCUMENTED */
	public void printElements() {
		System.out.println(name + " elements");
		for (Iterator i = element.elementIterator(); i.hasNext(); ) {
			Element e = (Element) i.next();
			System.out.println(e.getName());
		}
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  s  NOT YET DOCUMENTED
	 */
	protected static void prtln(String s) {
		if (debug)
			System.out.println("GenericType: " + s);
	}

}

