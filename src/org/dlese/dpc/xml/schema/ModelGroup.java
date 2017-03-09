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

import org.dlese.dpc.util.*;

import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.QName;
import org.dom4j.Namespace;

import java.util.*;
import java.io.*;

/**
 *  Wrapper for ModelGroup definitions in XML Schemas.
 *
 * @author     ostwald
 * @version    $Id: ModelGroup.java,v 1.3 2009/03/20 23:34:01 jweather Exp $
 */
public class ModelGroup extends ComplexType {

	private static boolean debug = true;

	// modelGroup can be all, choice, simpleContent, sequence
	// public String modelGroup = null;

	private Compositor compositor = null;

	// contentModel holds the "name" of the first element in the ModelGroup definition
	// it can be a compositor, a derivation (simpleContent, complexContent),
	private String contentModel = "unknown";


	/**
	 *  Constructor for the ModelGroup object
	 *
	 * @param  element    Description of the Parameter
	 * @param  location   Description of the Parameter
	 * @param  namespace  NOT YET DOCUMENTED
	 */
	public ModelGroup(Element element, String location, Namespace namespace, SchemaReader schemaReader) {

		super(element, location, namespace, schemaReader);

	}


	/**
	 *  Gets the dataType attribute of the ModelGroup object
	 *
	 * @return    COMPLEX_TYPE - The dataType value as an int
	 */
	public int getDataType() {
		return MODEL_GROUP;
	}


	/**
	 *  String representation of the ModelGroup object
	 *
	 * @return    a String representation of the ModelGroup object
	 */
	public String toString() {
		String s = "ModelGroup: " + getName();
		String nl = "\n\t";
		s += nl + "type: " + getType();
		s += nl + "location: " + getLocation();
		s += nl + "contentmodel: " + getContentModel();
		if (getNamespace() != Namespace.NO_NAMESPACE)
			s += nl + "namespace: " + getNamespace().getPrefix() + ": " + getNamespace().getURI();
		else
			s += nl + "namespace: null";
		//		s += nl + "modelGroup: " + modelGroup;
		//		s += nl + "there are " + getChildren().size() + " content elements";
		/*
			 *  if (modelGroup == null)
			 *  s += "\n" + element.asXML();
			 */
		// s += Dom4jUtils.prettyPrint (getElement());
		return s;
	}


	/**
	 *  print a string to std out
	 *
	 * @param  s  Description of the Parameter
	 */
/* 	protected static void prtln(String s) {
		if (debug)
			System.out.println(s);
	} */
}

