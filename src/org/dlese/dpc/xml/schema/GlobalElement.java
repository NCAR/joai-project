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
 *  Wrapper for global element definitions in an XML Schema, which are defined as an immediate child of the
 *  schema element.<p>
 *
 *  Global element definitions have an XPath of the form "/&lt;xsd:schema/&lt;xsd:element ...>", where the
 *  "xsd" prefix refers to a prefix that is mapped to the W3C Schema for Datatype Definitions (having a
 *  namespace uri of "http://www.w3.org/2001/XMLSchema").<p>
 *
 *  GlobalElement definitions are associated with a type in one of the following ways:
 *  <ul>
 *    <li> via a "type" attribute,
 *    <li> via a "ref" attribute
 *    <li> via an in-line type definition
 *  </ul>
 *
 * @author     Jonathan Ostwald
 * @version    $Id: GlobalElement.java,v 1.7 2009/03/20 23:34:01 jweather Exp $
 */
public class GlobalElement extends GlobalDeclaration {

	private List substitutionGroup;
	
	/**
	 *  Constructor for the GlobalElement object
	 *
	 * @param  element    NOT YET DOCUMENTED
	 * @param  location   NOT YET DOCUMENTED
	 * @param  namespace  NOT YET DOCUMENTED
	 */
	public GlobalElement(Element element, String location, Namespace namespace, SchemaReader schemaReader) {
		super (element, location, namespace, schemaReader);
		substitutionGroup = new ArrayList();
	}

	/**
	 *  Gets the dataType attribute of the GlobalElement object
	 *
	 * @return    The dataType value
	 */
	public int getDataType() {
		return GLOBAL_ELEMENT;
	}

	public boolean isAbstract () {
		String abstractValue = getElement().attributeValue ("abstract", null);
		return (abstractValue != null && abstractValue.equals("true"));
	}

	public boolean isHeadElement () {
		return (!getSubstitutionGroup().isEmpty());
	}
	
	public boolean isSubstitutionGroupMember () {
		return (getElement().attributeValue ("substitutionGroup", null) != null);
	}
	
	public boolean hasSubstitutionGroup () {
		return !substitutionGroup.isEmpty();
	}
	
	public List getSubstitutionGroup () {
		return substitutionGroup;
	}
	
	/**
	* NOTE: substitutionGroupMembers must have the same type as head or be derived from head's type
	*/
	public void addSubstitutionGroupMember (GlobalElement typeDef) {
		substitutionGroup.add (typeDef);
	}
	
	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @return    NOT YET DOCUMENTED
	 */
	public String toString() {
		String s = "GlobalElement: " + name;
 		String nl = "\n\t";
/*		s += nl + "type: " + type;
		s += nl + "location: " + getLocation();
		if (getNamespace() != null && getNamespace() != Namespace.NO_NAMESPACE)
			s += nl + "namespace: " + getNamespace().getPrefix() + ": " + getNamespace().getURI();
		else
			s += nl + "namespace: null"; */
		s += super.toString();
		if (isAbstract ())
			s += nl + "isAbstract";
		List sg = getSubstitutionGroup();
		if (sg.size() > 0) {
			s += nl + "substitutionGroup:";
			for (Iterator i=sg.iterator();i.hasNext();)
				s += nl + "\t" + ((GlobalElement)i.next()).getName();
		}
		return s;
	}

}

