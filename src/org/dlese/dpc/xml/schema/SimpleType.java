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
 *  Wrapper for SimpleType definitions in XML Schemas. Filters out annotation
 *  elements for now
 *
 *@author    ostwald
 */
public class SimpleType extends GenericType {
	private static boolean debug = true;

	/**
	 *  Gets the dataType attribute of the SimpleType object
	 *
	 *@return    The dataType value
	 */
	public int getDataType() {
		return SIMPLE_TYPE;
	}


	/**
	 *  Constructor for the SimpleType object
	 *
	 *@param  element   Description of the Parameter
	 *@param  location  Description of the Parameter
	 */
	public SimpleType(Element element, String location, Namespace namespace, SchemaReader schemaReader) {
		super(element, location, namespace, schemaReader);
	}


	/**
	 *  Gets the enumeration attribute of the SimpleType object
	 *
	 *@return    The enumeration value
	 */
	public boolean isEnumeration() {
		List list = getElement().selectNodes(NamespaceRegistry.makeQualifiedName(xsdPrefix, "restriction") + "/" + 
											 NamespaceRegistry.makeQualifiedName(xsdPrefix, "enumeration"));
		return (list.size() > 0);
	}

	/**
	* Returns the typeDef for the base enumeration type, or null if this SimpleType is not an enumeration
	*/
	public GlobalDef getEnumerationBaseType () {
		if (!isEnumeration()) return null;
		Element restriction = (Element)getElement().selectSingleNode(NamespaceRegistry.makeQualifiedName(xsdPrefix, "restriction"));
		if (restriction == null) return null;
		String baseTypeName = restriction.attributeValue("base", null);
		return (baseTypeName == null ? null : getSchemaReader().getGlobalDef(baseTypeName));
	}

	/**
	 *  Gets the union attribute of the SimpleType object
	 *
	 *@return    The union value
	 */
	public boolean isUnion() {
		List list = getElement().selectNodes(NamespaceRegistry.makeQualifiedName(xsdPrefix, "union"));
		return (list.size() > 0);
	}


	/**
	 *  Gets the unionMemberTypesAsString attribute of the SimpleType object
	 *
	 *@return    The unionMemberTypesAsString value
	 */
	public String getUnionMemberTypesAsString() {
		String members = "";
		List list = getElement().selectNodes(NamespaceRegistry.makeQualifiedName(xsdPrefix, "union"));
		Iterator i = list.iterator();
		if (i.hasNext()) {
			Element e = (Element) i.next();
			members = e.attributeValue("memberTypes");
		}
		return members;
	}


	/**
	 *  Gets the unionMemberTypes attribute of the SimpleType object
	 *
	 *@return    The unionMemberTypes value
	 */
	public String[] getUnionMemberTypeNames() {
		String members = "";
		List list = getElement().selectNodes(NamespaceRegistry.makeQualifiedName (xsdPrefix, "union"));
		Iterator i = list.iterator();
		if (i.hasNext()) {
			Element e = (Element) i.next();
			members = e.attributeValue("memberTypes", "");
		}
		// prtln ("getUnionMemberTypeNames: " + members);
		if (members.length() > 1) {
			return members.split("\\s+");
		}
		else {
			return new String[]{};
		}
	}

	/** Get a list of the type definitions for the members of a union type 
	*/
	public List getUnionMemberTypeDefs() {
		// prtln ("\n getUnionMemberTypeDefs() for " + this.getQualifiedInstanceName());
		// prtln (this.toString());
		List typeDefs = new ArrayList();
		String [] memberTypeNames = getUnionMemberTypeNames();
		// prtln ("\t " + memberTypeNames.length + " typeNames found");
		for (int i=0;i<memberTypeNames.length;i++) {
			String typeName = memberTypeNames[i];
			typeDefs.add (schemaReader.getGlobalDef (typeName));
		}
		return typeDefs;
	}
	
	/**
	* comboUnion types are enumerations with one additional member of either "xsd:string" or "stringTextType". The are
	rendered as a textInput with a drop-down menu of choices
	*/
	public boolean isComboUnionType() {
		
		/*
		We can have at most one non-enumeration type, and (for now) it has to 
		be "${XSD}:string" or "stringTextType" - where ${XSD} stands for the 
		XML NamespaceSchema prefix at the instanceDoc level (rather than at the
		level of the schema file at which this type is defined.
		*/
		if (isUnion()) {
			
			String instanceLevelXsdPrefix = this.schemaReader.resolveToInstancePrefix(this.getXsdPrefix());
			
			List nonUnionFields = Arrays.asList(
				new String[]{
					NamespaceRegistry.makeQualifiedName (instanceLevelXsdPrefix, "string"), 
					"stringTextType"
				});
			List memberTypeDefs = getUnionMemberTypeDefs();
			String nonUnionTypeName = null;
			
			for (Iterator i = memberTypeDefs.iterator();i.hasNext();) {
				GenericType memberTypeDef = (GenericType)i.next();
				
				if (!memberTypeDef.isEnumerationType()) {
					String memberName = memberTypeDef.getQualifiedName();
					if (nonUnionTypeName == null && nonUnionFields.contains(memberName)) {
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
		// prtln ("returning false (catch all)");
		return false;
	}
	
	/**
	 *  Gets the enumerationValues attribute of the SimpleType object
	 *
	 *@return    The enumerationValues value
	 */
	public List getSimpleEnumerationValues() {
		// prtln ("getSimpleEnumerationValues() for " + this.getQualifiedInstanceName());
		ArrayList values = new ArrayList();
		List list = getElement().selectNodes(NamespaceRegistry.makeQualifiedName(xsdPrefix, "restriction") + "/" + 
											 NamespaceRegistry.makeQualifiedName(xsdPrefix, "enumeration"));
		for (Iterator i = list.iterator(); i.hasNext(); ) {
			Element e = (Element) i.next();
			String v = e.attributeValue("value");
			values.add(v);
		}
		return values;
	}
	
	/**
	 *  Description of the Method
	 *
	 *@return    Description of the Return Value
	 */
	public String toString() {
		String s = "SimpleType: " + getName();
		s += "\n\t location: " + getLocation();
		if (isEnumeration()) {
			s += "\n\tEnumeration";
			for (Iterator i = getSimpleEnumerationValues().iterator(); i.hasNext(); ) {
				s += "\n\t\t" + (String) i.next();
			}
		}
		else if (isUnion()) {
			s += "\n\tUnion (" + getUnionMemberTypesAsString() + ")";
		}
		else {
			s += getElement().asXML();
		}
		if (getNamespace() != Namespace.NO_NAMESPACE)
			s += "\n\tnamespace: " + getNamespace().getPrefix() + ": " + getNamespace().getURI();
		else
			s += "\n\tnamespace: null";
		return s;
	}
		protected static void prtln(String s) {
		if (debug) {
			SchemaUtils.prtln(s, "SimpleType");
		}
	}
	
}

