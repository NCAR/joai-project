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
 *  Wrapper for global delclarations in an XML Schema, which are defined as an immediate child of the
 *  schema element.<p>
 *  Global Declarations do NOT define a data type or model, but they do define a construct that can be 
 *  referred to by other schema elements via the "ref" attribute.
 *
 * @author     Jonathan Ostwald
 */
public class GlobalDeclaration implements GlobalDef {

	protected static boolean debug = false;
	private static boolean schemaDocAware = false;
	protected String name = null;
	protected String type = null;
	protected Element element = null;
	protected String location = null;
	protected Namespace namespace = null;
	protected SchemaReader schemaReader;
	protected String documentation = null;
	protected String xsdPrefix = null;


	/**
	 *  Constructor for the GlobalDeclaration object
	 *
	 * @param  element    NOT YET DOCUMENTED
	 * @param  location   NOT YET DOCUMENTED
	 * @param  namespace  NOT YET DOCUMENTED
	 */
	public GlobalDeclaration(Element element, String location, Namespace namespace, SchemaReader schemaReader) {
		this.name = element.attributeValue("name");
		this.xsdPrefix = schemaReader.getXsdPrefix();
		this.type = element.attributeValue("type", this.xsdPrefix+":string");
		this.element = element;
		this.location = location;
		this.namespace = namespace;
		this.schemaReader = schemaReader;
		if (schemaDocAware)
			extractDocumentation();
/*  		else
			filterChildren(xsdPrefix + ":annotation");  */

	}

	public SchemaReader getSchemaReader () {
		return schemaReader;
	}

	public boolean isTypeDef() {
		return false;
	}
	
		
	public void extractDocumentation () {
		Element docElement = (Element) this.element.selectSingleNode (this.xsdPrefix + ":annotation/"  +
									                                  this.xsdPrefix + ":documentation");
		if (docElement != null) {
			String docString = docElement.getTextTrim();
			if (docString != null && docString.length() > 0)
				this.documentation = docString;
			else
				this.documentation = null;
		}
	}
	
	public String getDocumentation () {
		return this.documentation;
	}
	
	/**
	 *  Gets the simpleType attribute of the GlobalDeclaration object
	 *
	 * @return    The simpleType value
	 */
	public boolean isSimpleType() {
		return false;
	}


	/**
	 *  Gets the complexType attribute of the GlobalDeclaration object
	 *
	 * @return    The complexType value
	 */
	public boolean isComplexType() {
		return false;
	}

	public boolean isModelGroup () {
		return (getDataType() == MODEL_GROUP);
	}

	/**
	 *  Gets the GlobalDeclaration attribute of the GlobalDeclaration object
	 *
	 * @return    The GlobalDeclaration value
	 */
	public boolean isGlobalDeclaration() {
		return true;
	}

	public boolean isGlobalAttribute() {
		return (getDataType() == GLOBAL_ATTRIBUTE);
	}

	public boolean isGlobalElement() {
		return (getDataType() == GLOBAL_ELEMENT);
	}
	
	public boolean isAttributeGroup() {
		return (getDataType() == ATTRIBUTE_GROUP);
	}
	
	/**
	 *  Gets the builtIn attribute of the GlobalDeclaration object
	 *
	 * @return    The builtIn value
	 */
	public boolean isBuiltIn() {
		return false;
	}

	public boolean isAnyType() {
		return false;
	}

	/**
	 *  Gets the dataType attribute of the GlobalDeclaration object
	 *
	 * @return    The dataType value
	 */
	public int getDataType() {
		return GLOBAL_DECLARATION;
	}


	/**
	 *  Gets the type attribute of the GlobalDeclaration object
	 *
	 * @return    The type value
	 */
	public String getType() {
		return type;
	}


	/**
	 *  Gets the name attribute of the GlobalDeclaration object
	 *
	 * @return    The name value
	 */
	public String getName() {
		return name;
	}

	public String getQualifiedName () {
		return NamespaceRegistry.makeQualifiedName (getNamespace(), getName());
	}
	
	/**
	* use prefix for namespace as defined at the instance level. 
		NOTE:  namespaces may not be defined at the instance level!
		*/
 	public String getQualifiedInstanceName () {
		String mode = "old_way";
		if (mode.equals("old_way")) {
			String prefix = schemaReader.getInstanceNamespaces().getPrefixforNS(getNamespace());
			return NamespaceRegistry.makeQualifiedName (prefix, getName());
		}
		
		else
			return schemaReader.getInstanceQualifiedName(getQualifiedName());
	}
	
	
	/**
	 *  Gets the namespace attribute of the GlobalDeclaration object
	 *
	 * @return    The namespace value
	 */
	public Namespace getNamespace() {
		return namespace;
	}


	/**
	 *  Gets the location attribute of the GlobalDeclaration object
	 *
	 * @return    The location value
	 */
	public String getLocation() {
		return location;
	}


	/**
	 *  Gets the element attribute of the GlobalDeclaration object
	 *
	 * @return    The element value
	 */
	public Element getElement() {
		return element;
	}

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
		return s;
	}

	protected static void prtln(String s) {
		if (debug)
			System.out.println(s);
	}
	
	protected static void prtlnErr (String s) {
			System.err.println(s);
	}
	
}

