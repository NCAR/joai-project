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
 * wrapper for BuiltInType definitions ({@link org.dom4j.Element}).
 * filters out annotation elements for now
 */
public class BuiltInType extends GenericType {
/* 	protected String name;
	protected String location;
	protected Element element;
	protected String path;
	protected String type;
	protected Namespace namespace;
	protected String xsdPrefix; */

	
	/**
	 * Constructor - 
	 */
	 public BuiltInType (String name) {
		 this (name, null);
	 }
	 
	public BuiltInType (String name, Namespace namespace) {
		super (name, namespace);
		location = "http://www.w3.org/2001/XMLSchema.xsd";
	}
	
	public SchemaReader getSchemaReader () {
		return null;
	}
	
	public boolean isSimpleType () {
		return (getDataType() == SIMPLE_TYPE);
	}
	public boolean isComplexType () {
		return (getDataType() == COMPLEX_TYPE);
	}
	
	public String getXsdPrefix () {
		return xsdPrefix;
	}
	
	public boolean isGlobalElement () {
		return false;
	}

	public boolean isGlobalDeclaration () {
        return false;
    }
	
	public boolean isGlobalAttribute () {
		return false;
	}
	
	public boolean isBuiltIn () {
		return (getDataType() == BUILT_IN_TYPE);
	}
	
	public int getDataType () {
		return BUILT_IN_TYPE;
	}
	
	public String getType () {
		return type;
	}
	
	public boolean isAnyType () {
		String qName = NamespaceRegistry.makeQualifiedName(getNamespace().getPrefix(), "any");
		return this.getName().equals(qName);
	}
	
	public String getName() {
		return name;
	}
	
	public String getQualifiedName () {
		// return NamespaceRegistry.makePrefix(getNamespace().getPrefix()) + getName();
		return name;
	}
	
	/**
	* schemaReader is unavailable, so we return a bogus prefix
	*/
	public String getQualifiedInstanceName () {
		return name;
	}
	
	public Namespace getNamespace () {
		return namespace;
	}
	
	public String getLocation() {
		return location;
	}
	
	public Element getElement() {
		return element;
	}
	
	public String getElementAsXml() {
		return "";
	}
	
	public String toString () {
		return "";
	}

}
