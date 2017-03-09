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
package org.dlese.dpc.schemedit.input;

import org.dlese.dpc.schemedit.SchemEditUtils;
import org.dlese.dpc.schemedit.url.UrlHelper;
import org.dlese.dpc.xml.XPathUtils;
import org.dlese.dpc.xml.schema.SchemaHelper;

import org.dlese.dpc.xml.schema.SchemaNode;
import org.dom4j.Node;
import java.util.*;

/**
 *  Helper for translating between http request parameters and metadata
 *  elements.<p>
 *
 *  The metadata editor creates request parameters named for the accessor each
 *  field uses to obtain its value (e.g., valueOf(/itemRecord/lifecycle/contributors/contributor_2_/person/nameLast)).
 *  This class stores information derived from the parameterName, such as the
 *  {@link org.dlese.dpc.xml.schema.SchemaNode} for this field, and provides
 *  information about this node that aids in the processing of its value.
 *
 * @author    ostwald
 */
public class InputField {

	private static boolean debug = true;

	private String paramName = null;
	private String value = null;
	private SchemaNode schemaNode = null;
	private String xpath = null;
	private InputManager im = null;
	private String fieldName = null;
	private List entityErrors = null;


	/**
	 *  InputField constructor. Below are examples of typical parameters:
	 *  <dl>
	 *    <dt> paramName
	 *    <dd> valueOf(/itemRecord/lifecycle/contributors/contributor_2_/person/emailAlt)
	 *
	 *    <dt> value
	 *    <dd> anyone@foo.com
	 *    <dt> xpath
	 *    <dd> /itemRecord/lifecycle/contributors/contributor[2]/person/emailAlt
	 *
	 *    <dt> normalizedXPath
	 *    <dd> /itemRecord/lifecycle/contributors/contributor/person/emailAlt
	 *  </dl>
	 *  paramName is used with the error-report mechanism to locate the field
	 *  element in the UI
	 *
	 * @param  paramName        paramName as received from the request
	 * @param  value            the value as received from the request
	 * @param  schemaNode       SchemaNode instance for this field
	 * @param  xpath            the xpath of this field
	 * @param  inputManager     the inputManager instance for this field
	 */
	protected InputField(String paramName,
	                     String value,
	                     SchemaNode schemaNode,
	                     String xpath,
	                     InputManager inputManager) {
		this.schemaNode = schemaNode;
		this.paramName = paramName;
		this.xpath = xpath;
		this.im = inputManager;

		ReferenceResolver.ResolverResults results = this.im.resolveReferences(value);

		this.entityErrors = results.errors;
		String myValue = results.content;

		// normalize the values of fields that are specified as "urlPaths" in
		// the framework configuration.
		List urlPaths = this.im.getUrlSchemaPaths();
		if (urlPaths.contains(getNormalizedXPath())) {
			myValue = UrlHelper.normalize(myValue);
		}
		
		this.setValue(myValue);
	}


	/**
	 *  Returns the request parameter name for this field.<p>
	 *
	 *  E.g., valueOf(/collectionConfigRecord/tuples/tuple_1_/name)
	 *
	 * @return    The paramName value
	 */
	public String getParamName() {
		return paramName;
	}


	/**
	 *  Sets the paramName attribute of the InputField object
	 *
	 * @param  paramName  The new paramName value
	 */
	public void setParamName(String paramName) {
		this.paramName = paramName;
	}


	/**
	 *  returns a list of entity errors found in the value for this InputField
	 *
	 * @return    The entityErrors value
	 */
	public List getEntityErrors() {
		return entityErrors;
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @return    NOT YET DOCUMENTED
	 */
	public boolean hasEntityErrors() {
		return (entityErrors != null && !entityErrors.isEmpty());
	}


	/**
	 *  Gets the SchemaNode associated with this InputField.
	 *
	 * @return    The schemaNode value
	 */
	public SchemaNode getSchemaNode() {
		return schemaNode;
	}


	/**
	 *  Gets the xPath attribute of the InputField object.
	 *
	 * @return    The xPath value
	 */
	public String getXPath() {
		return xpath;
	}


	/**
	 *  Sets the xPath attribute of the InputField object
	 *
	 * @param  xpath  The new xPath value
	 */
	public void setXPath(String xpath) {
		this.xpath = xpath;
	}


	/**
	 *  Returns an xpath containing no indexing for the element corresponding to
	 *  this InputField.<p>
	 *
	 *  Note: this is the same path used to key the SchemaNodeMap, so why do we
	 *  need it as an attribute? Could this accessor just as well return
	 *  schemaNode.getXPath?
	 *
	 * @return    The normlizedXPath value
	 */
	public String getNormalizedXPath() {
		return getSchemaNode().getXpath();
	}


	/**
	 *  Gets the leaf of the xpath attribute for the InputField object.
	 *
	 * @return    The fieldName value
	 */
	public String getFieldName() {
		int lastSlash = xpath.lastIndexOf("/");
		String name = "";
		if (isAttribute()) {
			name = xpath.substring(lastSlash + 2);
		}
		else {
			name = xpath.substring(lastSlash + 1);
		}
		return name;
	}


	/**
	 *  Gets the value entered in the metadata editor for this field.
	 *
	 * @return    The value value
	 */
	public String getValue() {
		return value;
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  str  NOT YET DOCUMENTED
	 * @return      NOT YET DOCUMENTED
	 */
	private String handleWhiteSpace(String str) {
		SchemaHelper schemaHelper = this.im.getSchemaHelper();
		if (schemaHelper == null)
			return str;
		String vTypeName = schemaNode.getValidatingType().getName();
		com.sun.msv.datatype.xsd.XSDatatypeImpl xsdDataType =
			(com.sun.msv.datatype.xsd.XSDatatypeImpl) schemaHelper.getXSDataType(vTypeName);
		if (xsdDataType == null) {
			// we don't expect an xsdDataType to be found for a derived complex type
			// prtln ("xsdDataType not found for " + vTypeName + " (" + schemaNode.getXpath() + ")");
			return str;
		}
		else {
			com.sun.msv.datatype.xsd.WhiteSpaceProcessor whiteSpaceProcessor = xsdDataType.whiteSpace;

			// here is where we would massage value (str) depending on value of whiteSpaceProcessor
			return whiteSpaceProcessor.process(str);
		}
	}


	/**
	 *  Sets the value attribute of the InputField object.
	 *
	 * @param  str    The new value value
	 */
	public void setValue(String str) {
		// prtln ("\nsetValue(" + XPathUtils.getLeaf(this.getXPath()) +") in: \"" + str + "\"");

		value = handleWhiteSpace(str);
		// prtln ("\t out: \"" + value + "\"");
	}


	/**
	 *  Gets the anyType attribute of the InputField object
	 *
	 * @return    The anyType value
	 */
	public boolean isAnyType() {
		return schemaNode.getTypeDef().isAnyType();
	}


	/**
	 *  Returns true if this field represents an attribute (as opposed to an
	 *  element).
	 *
	 * @return    The attribute value
	 */
	public boolean isAttribute() {
		return schemaNode.isAttribute();
	}


	/**
	 *  Returns true if this field represents an element (as opposed to an
	 *  attribute).
	 *
	 * @return    The element value
	 */
	public boolean isElement() {
		return schemaNode.isElement();
	}


	/**
	 *  Gets the nillable attribute of the SchemaNode associated with this
	 *  InputField.
	 *
	 * @return    The nillable value
	 */
	public boolean isNillable() {
		return schemaNode.isNillable();
	}


	/**
	 *  Debugging utility returns a string listing key fields and values.
	 *
	 * @return    NOT YET DOCUMENTED
	 */
	public String toString() {
		String ret = "---";
		ret += "\n\t" + "fieldName: " + getFieldName();
		ret += "\n\t" + "paramName: " + getParamName();
		ret += "\n\t" + "value: " + getValue();
		ret += "\n\t" + "xpath: " + getXPath();
		ret += "\n\t" + "normalizedXPath: " + getNormalizedXPath();
		return ret + "\n";
	}


	/**
	 *  Description of the Method
	 *
	 * @param  s  Description of the Parameter
	 */
	private static void prtln(String s) {
		if (debug) {
			SchemEditUtils.prtln(s, "InputField");
		}
	}
}

