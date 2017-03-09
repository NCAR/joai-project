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

import org.dlese.dpc.schemedit.MetaDataFramework;
import org.dlese.dpc.schemedit.SchemEditUtils;
import org.dlese.dpc.xml.schema.*;
import org.dlese.dpc.util.*;
import org.dlese.dpc.xml.*;
import java.util.*;
import java.util.regex.*;
import javax.servlet.http.HttpServletRequest;

import org.dom4j.*;

/**
 *  Aids metadata editor in processing of requests, and more specifically in
 *  processing (incl. validating) metadata field values as they are passed in
 *  the Request object (see {@link SchemEditValidator}).
 *  <p>
 *
 *  The request parameters corrsponding to metadata elements contain the xpath
 *  of the element, and are interspersed with other types of request parameters.
 *  The InputManager selects the parameters for metadata elements and creates a
 *  Map of {@link InputField} instances, keyed by xpath.
 *  <p>
 *
 *  Example of a metadata request parameter:
 *  <ul><code>valueOf(/itemRecord/lifecycle/contributors/contributor_2_/person/nameLast)</code>
 *
 *  </ul>
 *
 *
 *@author    ostwald
 */
public class InputManager {

	private static boolean debug = false;
	private HashMap fieldsMap = null;
	private MetaDataFramework framework = null;
	private SchemaHelper schemaHelper = null;
	private HttpServletRequest request = null;
	private ReferenceResolver refResolver = null;
	private List urlSchemaPaths = null;
	private List attributeFields = null;
	private List elementFields = null;
	private List entityErrorFields = null;
	private List multiValueFields = null;
	private List anyTypeFields = null;


	/**
	 *  Constructor for the InputManager object
	 *
	 *@param  request    Description of the Parameter
	 *@param  framework  Description of the Parameter
	 */
	public InputManager(HttpServletRequest request, MetaDataFramework framework) {
		this.framework = framework;
		if (this.framework != null) {
			schemaHelper = framework.getSchemaHelper();
		}
		this.request = request;
		this.refResolver = new ReferenceResolver();

		fieldsMap = new HashMap();
		attributeFields = new ArrayList();
		elementFields = new ArrayList();
		multiValueFields = new ArrayList();
		anyTypeFields = new ArrayList();

		Enumeration paramNames = request.getParameterNames();

		// prtln ("processing params");
		while (paramNames.hasMoreElements()) {
			String paramName = (String) paramNames.nextElement();

			// only process params that correspond to field inputs (they have a schemaNode)
			if (hasSchemaNode(paramName)) {
				SchemaNode schemaNode = getSchemaNode(paramName);
				// CAUTION: need to know when a parameter might have multiple values!!
				String xpath = paramNameToXPath(paramName);

				String[] valueArray = request.getParameterValues(paramName);
				// prtln ("- paramName: " + paramName + ", values: " + valueArray.length);
				if (valueArray.length == 1) {
					InputField inputField = createInputField(paramName, valueArray[0], schemaNode, xpath, this);
					this.addField(paramName, inputField);
				} else {
					// params with multiple values are expanded into separate, indexed param-names
					for (int i = 0; i < valueArray.length; i++) {
						String value = valueArray[i];
						String indexedParamName = getIndexedParamName(paramName, i + 1);
						String augXPath = paramNameToXPath(indexedParamName);
						InputField inputField = createInputField(paramName, value, schemaNode, augXPath, this);
						this.addField(indexedParamName, inputField);
					}
				}
			}
		}
		try {
			// sort element fields to ensure processing order

			/*
			 *  WHY should element fields be sorted DEscending??
			 *  -- i can't remember or figure out, so we'll go with Ascending for now ...
			 *  WHY should element fields be sorted ASscending??
			 *  -- indexed fields need to see the lower indexes first when inserting several so that we
			 *  don't get a "target not found" error from docMap
			 */
			// Collections.sort(this.elementFields, new SortInputFieldDescending()); // current
			Collections.sort(this.elementFields, new SortInputFieldAscending());
			// test
			Collections.sort(this.multiValueFields, new SortInputFieldAscending());
		} catch (Throwable t) {
			prtln("SORT ERROR: " + t.getMessage());
			t.printStackTrace();
		}
	}


	/**
	 *  Gets the inputField for the given param
	 *
	 *@param  paramName  NOT YET DOCUMENTED
	 *@return                   The inputField value
	 */
	public InputField getInputField(String paramName) {
		return (InputField) this.fieldsMap.get(paramName);
	}


	/**
	 *  A factory that creates either a plain {@link
	 *  org.dlese.dpc.schemedit.input.InputField} object or an {@link
	 *  org.dlese.dpc.schemedit.input.AnyTypeInputField} object as appropriate.
	 *
	 *@param  paramName     NOT YET DOCUMENTED
	 *@param  value         NOT YET DOCUMENTED
	 *@param  schemaNode    NOT YET DOCUMENTED
	 *@param  xpath         NOT YET DOCUMENTED
	 *@param  inputManager  NOT YET DOCUMENTED
	 *@return               NOT YET DOCUMENTED
	 */
	private InputField createInputField(String paramName,
			String value,
			SchemaNode schemaNode,
			String xpath,
			InputManager inputManager) {
		if (schemaNode.getTypeDef().isAnyType()) {
			return new AnyTypeInputField(paramName, value, schemaNode, xpath, this);
		} else {
			return new InputField(paramName, value, schemaNode, xpath, this);
		}
	}



	/**
	 *  Adds an InputField to the fieldsMap and the secondary list (i.e.,
	 *  attributeFields, anyTypeFields, etc) collecting similar fields.
	 *
	 *@param  paramName  The feature to be added to the Field attribute
	 *@param  inputField        The feature to be added to the Field attribute
	 */
	private void addField(String paramName, InputField inputField) {
		fieldsMap.put(paramName, inputField);

		/*
		 *  test order is important!
		 *  - we must test for anyType and multiValue fields BEFORE testing
		 *  for element fields
		 */
		boolean hasVocabLayout = false;
		try {
			hasVocabLayout = this.framework.getVocabLayouts().hasVocabLayout(inputField.getNormalizedXPath());
		} catch (Throwable t) {}
		if (hasVocabLayout) {
			prtln("hasVocabLayout: " + inputField.getNormalizedXPath());
		}

		if (inputField.isAttribute()) {
			this.attributeFields.add(inputField);
		} else if (inputField.isAnyType()) {
			this.anyTypeFields.add(inputField);
		} // else if (isMultiValueField(inputField) || inputField.getXPath().startsWith ("/record/general/subjects/"))
		else if (isMultiValueField(inputField) || hasVocabLayout) {
			this.multiValueFields.add(inputField);
		} else if (inputField.isElement()) {
			this.elementFields.add(inputField);
		}
	}


	/**
	 *  Get the url schema paths for this framework configuration.
	 *
	 *@return    A list of xpaths (never null)
	 */
	protected List getUrlSchemaPaths() {
		if (this.urlSchemaPaths == null) {
			this.urlSchemaPaths = new ArrayList();
			if (this.framework != null) {
				this.urlSchemaPaths = this.framework.getUrlPaths();
			}
		}
		return this.urlSchemaPaths;
	}


	/**
	 *  Get the InputField instances managed by this InputManager
	 *
	 *@return    The inputFields value
	 */
	public Collection getInputFields() {
		return fieldsMap.values();
	}


	/**
	 *  Gets the entityErrorFields managed by this InputManager
	 *
	 *@return    The entityErrorFields value
	 */
	public List getEntityErrorFields() {
		if (entityErrorFields == null) {
			entityErrorFields = new ArrayList();
			for (Iterator i = this.getInputFields().iterator(); i.hasNext(); ) {
				InputField field = (InputField) i.next();
				if (field.hasEntityErrors()) {
					entityErrorFields.add(field);
				}
			}
			Collections.sort(entityErrorFields, new SortInputFieldAscending());
		}
		return entityErrorFields;
	}


	/**
	 *  Gets the schemaNode corresponding to the xpath contained in the provided
	 *  paramName.
	 *
	 *@param  paramName  request parameter supplied by the Request object
	 *@return            The schemaNode value
	 */
	public SchemaNode getSchemaNode(String paramName) {
		String xpath = paramNameToNormalizedXPath(paramName);
		return schemaHelper.getSchemaNode(xpath);
	}


	/**
	 *  Gets the schemaHelper attribute of the InputManager object
	 *
	 *@return    The schemaHelper value
	 */
	public SchemaHelper getSchemaHelper() {
		return this.schemaHelper;
	}


	/**
	 *  Validates the value for a given InputField against its dataType.<p>
	 *
	 *  The work is done by {@link org.dlese.dpc.xml.schema.SchemaHelper#checkValidValue(String,String)},
	 *  which is called with information extracted from the InputField parameter.
	 *
	 *@param  inputField     Description of the Parameter
	 *@return                true if the inputField contains a valid value
	 *@exception  Exception  Contains error message if value is not valid
	 */
	public boolean checkValidValue(InputField inputField)
			 throws Exception {
		String value = inputField.getValue();

		SchemaNode schemaNode = inputField.getSchemaNode();
		String typeName = schemaNode.getValidatingType().getName();
		return schemaHelper.checkValidValue(typeName, value);
	}



	/**
	 *  Filters request parameters holding field data from other request
	 *  parameters.
	 *
	 *@param  paramName  paramName in the request that may correspond to a metadata
	 *      field
	 *@return            true if paramName corresponds to an entry in the
	 *      SchemaNodeMap
	 */
	public boolean hasSchemaNode(String paramName) {
		String xpath = paramNameToNormalizedXPath(paramName);
		if (xpath == null) {
			return false;
		} else {
			return schemaHelper.getSchemaNodeMap().containsKey(xpath);
		}
	}


	/**
	 *  Extracts an xpath from a request parameter name, and normalizes it to
	 *  remove all indexing information.<p>
	 *
	 *  For example, the following parameter name of:
	 *  <ul>valueOf(/itemRecord/lifecycle/contributors/contributor_2_/person/nameLast)
	 *
	 *  </ul>
	 *  would return:
	 *  <ul>/itemRecord/lifecycle/contributors/contributor/person/nameLast
	 *  </ul>
	 *
	 *
	 *@param  paramName  Description of the Parameter
	 *@return            A schema-normalized xpath (that can access a schemaNode)
	 */
	public static String paramNameToNormalizedXPath(String paramName) {
		String decodedXPath = paramNameToXPath(paramName);
		if (decodedXPath == null) {
			return null;
		} else {
			return XPathUtils.normalizeXPath(decodedXPath);
		}
	}


	/**
	 *  Creates a parameterName with an index for use in processing multivalue
	 *  fields, which in the request, have several values for a given path.<p>
	 *
	 *  This method creates an individual, indexed path for each value. For
	 *  example, if there were several values for the paramName of
	 *  <ul>enumerationValuesOf(/itemRecord/general/subjects/subject)
	 *  </ul>
	 *  the indexedParamName created for the first value would be
	 *  <ul>enumerationValuesOf(/itemRecord/general/subjects/subject[1])
	 *  </ul>
	 *
	 *
	 *@param  paramName  Description of the Parameter
	 *@param  index      Description of the Parameter
	 *@return            Description of the Return Value
	 */
	public static String getIndexedParamName(String paramName, int index) {
		int closeParen = paramName.lastIndexOf(")");
		if (closeParen == -1) {
			prtln("illegal paramName");
			return "";
		}
		String lastChar = paramName.substring(closeParen - 1, closeParen);
		if (lastChar.equals("]")) {
			prtln("already indexed");
			return paramName;
		}
		String ipnRoot = XPathUtils.decodeXPath(paramName.substring(0, closeParen));
		return ipnRoot + "[" + index + "])";
	}


	/**
	 *  Extracts the xpath portion of a paramName and decodes the xpath by
	 *  converting indexing information from javscript to xpath forms.<p>
	 *
	 *  For example, for a paramName of
	 *  <ul>valueOf(/itemRecord/metaMetadata/contributors/contributor_2_/@date)
	 *
	 *  </ul>
	 *  this method returns
	 *  <ul>itemRecord/metaMetadata/contributors/contributor[2]/@date
	 *  </ul>
	 *
	 *
	 *@param  paramName  Description of the Parameter
	 *@return            Description of the Return Value
	 */
	public static String paramNameToXPath(String paramName) {
		String encodedXPath = InputManager.stripFunctionCall(paramName);
		if (encodedXPath == null) {
			return null;
		}
		String decodedXPath = XPathUtils.decodeXPath(encodedXPath);

		return decodedXPath;
	}


	/**
	 *  Strips the function call from a request parameter name, leaving a xpath.<p>
	 *
	 *  Request parameters corresponding to Input fields are of the form:
	 *  function(xpath). Applying stripFunctionCall to "function(xpath)" yeilds
	 *  xpath.
	 *
	 *@param  paramName  Description of the Parameter
	 *@return            Description of the Return Value
	 */
	public static String stripFunctionCall(String paramName) {
		Pattern p = Pattern.compile("\\(.+?\\)");
		Matcher m = p.matcher(paramName);

		// is this a parameterName that represents an xpath?
		if (!m.find()) {
			return null;
		}

		// xtract the portion of the paramName that will be decoded into an xpath
		return paramName.substring(m.start() + 1, m.end() - 1);
	}


	/**
	 *  Selects the InputFields corresponding to metadata Attributes.
	 *
	 *@return    The attributes value
	 */
	public List getAttributeFields() {
		return this.attributeFields;
	}


	/**
	 *  Gets the anyTypeFields attribute of the InputManager object
	 *
	 *@return    The anyTypeFields value
	 */
	public List getAnyTypeFields() {
		return this.anyTypeFields;
	}


	/**
	 *  Selects the InputFields corresponding to metadata Elements.
	 *
	 *@return    The elements value
	 */
	public List getElementFields() {
		return this.elementFields;
	}


	/**
	 *  Selects the InputFields corresponding to metadata fields that allow for
	 *  multiple values.
	 *
	 *@return    The multiValueFields value
	 */
	public List getMultiValueFields() {
		return this.multiValueFields;
	}



	/**
	 *  Remove specified Anytype fields from the managedFields of this inputManager
	 *  and also from the instanceDocument.<p>
	 *
	 *  Note: updates inputField xpath indexing as appropriate after fields have
	 *  been removed from the instance doc.
	 *
	 *@param  fieldsToRemove  list of InputField instances
	 *@param  docMap          provides access to instanceDocument
	 */
	public void removeAnyTypeFields(List fieldsToRemove, DocMap docMap) {

		if (this.getAnyTypeFields().isEmpty()) {
			return;
		}

		prtln("\nremoving " + this.getAnyTypeFields().size() + " anyTypeFields");

		// make a map of sibling paths to fields
		Map siblingMap = new HashMap();
		for (Iterator i = this.anyTypeFields.iterator(); i.hasNext(); ) {
			AnyTypeInputField field = (AnyTypeInputField) i.next();
			String siblingPath = XPathUtils.getSiblingXPath(field.getXPath());
			List siblingFields = (List) siblingMap.get(siblingPath);
			if (siblingFields == null) {
				siblingFields = new ArrayList();
			}
			siblingFields.add(field);
			siblingMap.put(siblingPath, siblingFields);
		}

		// now remove fields (from corresponding group of siblings).
		for (Iterator i = fieldsToRemove.iterator(); i.hasNext(); ) {
			AnyTypeInputField fieldToRemove = (AnyTypeInputField) i.next();
			List siblings = (List) siblingMap.get(XPathUtils.getSiblingXPath(fieldToRemove.getXPath()));
			siblings.remove(fieldToRemove);
		}

		// Remove ALL anyType fields from the fieldMap (non-empty fields will be readded)
		for (Iterator i = this.anyTypeFields.iterator(); i.hasNext(); ) {
			InputField inputField = (InputField) i.next();
			String fieldKey = inputField.getParamName();
			if (!this.fieldsMap.keySet().contains(fieldKey)) {
				prtln(fieldKey + " not found in fieldsMap!");
			}
			this.fieldsMap.remove(fieldKey);
		}

		// clear lists so they will be reconstructed
		this.anyTypeFields = new ArrayList();
		this.entityErrorFields = null;

		// now reconstruct fields and docMap element from remaining fields
		for (Iterator i = siblingMap.keySet().iterator(); i.hasNext(); ) {
			String siblingPath = (String) i.next();

			Element parent = (Element) docMap.selectSingleNode(XPathUtils.getParentXPath(siblingPath));
			parent.clearContent();
			List remainingFields = (List) siblingMap.get(siblingPath);
			Collections.sort(remainingFields, new SortInputFieldAscending());
			Iterator remainingIterator = remainingFields.iterator();
			while (remainingIterator.hasNext()) {
				AnyTypeInputField field = (AnyTypeInputField) remainingIterator.next();
				try {
					Element child;
					if (field.hasParseError()) {
						child = DocumentHelper.createElement("error");
					} else {
						child = field.getValueElement();
					}
					parent.add(child);
					// update the indexing of the field according to new position in instanceDoc
					int index = parent.elements().size();
					field.updateIndexing(index);
				} catch (Throwable t) {
					prtln(t.getMessage());
				}
			}
		}
	}


	/**
	 *  Returns true of an InputField represents a metadata field that can accept
	 *  mulitple values.<p>
	 *
	 *  NOTE: fields that are associated with a standardsManager are considered
	 *  MultiValue (DO WE NEED THIS ANY MORE, OR WAS IT A TEMPORARY KLUDGE)??
	 *
	 *@param  field  Description of the Parameter
	 *@return        The multiValueField value
	 */
	private boolean isMultiValueField(InputField field) {
		if (schemaHelper.isMultiSelect(field.getSchemaNode())) {
			prtln (field.getXPath() + " is a multivalue field (by schemaHelper.isMultiSelect)");
			return true;
		}
		if (this.framework.getStandardsManager() != null &&
				this.framework.getStandardsManager().getXpath().equals(field.getNormalizedXPath())) {
			prtln (field.getXPath() + " is a multivalue field (by getStandardsManager)");
			return true;
		}
		return false;
	}


	/**
	 *  Identifies entity references and attempts to resolve them
	 *
	 *@param  fieldValue  NOT YET DOCUMENTED
	 *@return             NOT YET DOCUMENTED
	 */
	protected ReferenceResolver.ResolverResults resolveReferences(String fieldValue) {
		try {
			return this.refResolver.resolve(fieldValue);
		} catch (Throwable t) {
			prtln("caught a resolver error!");
		}
		return null;
	}


	// debugging print stuff
	/**
	 *  Displays the InputFields corresponding to metadata fields that are
	 *  Attributes.
	 */
	public void displayAttributeFields() {
		prtln("** Attribute Fields **");
		for (Iterator ai = getAttributeFields().iterator(); ai.hasNext(); ) {
			InputField field = (InputField) ai.next();
			if (schemaHelper.isRequiredAttribute(field.getSchemaNode())) {
				// field.isRequired())
				prtln(field.getXPath() + " (required)");
			} else {
				prtln(field.getXPath());
			}
			prtln("\t" + field.getValue());
		}
		prtln("----------\n");
	}


	/**
	 *  Displays the InputFields corresponding to metadata fields that are
	 *  Elements.
	 */
	public void displayElementFields() {
		prtln("** Element Fields **");
		for (Iterator em = getElementFields().iterator(); em.hasNext(); ) {
			InputField field = (InputField) em.next();
			if (schemaHelper.isRequiredBranch(field.getSchemaNode())) {
				prtln(field.getXPath() + " (required)");
			} else {
				prtln(field.getXPath());
			}
			prtln("\t" + field.getValue());
		}
		prtln("----------\n");
	}


	/**
	 *  Displays the InputFields corresponding to metadata fields that are
	 *  Elements.
	 */
	public void displayAnyTypeFields() {
		prtln("** AnyType Fields **");
		for (Iterator i = this.getAnyTypeFields().iterator(); i.hasNext(); ) {
			InputField field = (InputField) i.next();
			if (schemaHelper.isRequiredBranch(field.getSchemaNode())) {
				prtln(field.getXPath() + " (required)");
			} else {
				prtln(field.getXPath());
			}
			prtln("\t" + field.getValue());
		}
		prtln("----------\n");
	}


	/**
	 *  Displays the InputFields corresponding to metadata fields that are
	 *  Elements.
	 */
	public void displayEntityErrorFields() {
		prtln("** EntityError Fields **");
		for (Iterator i = this.getEntityErrorFields().iterator(); i.hasNext(); ) {
			InputField field = (InputField) i.next();
			int numErrors = field.getEntityErrors().size();
			if (schemaHelper.isRequiredBranch(field.getSchemaNode())) {
				prtln(field.getXPath() + " - " + numErrors + " (required)");
			} else {
				prtln(field.getXPath() + " - " + numErrors);
			}
		}
		prtln("----------\n");
	}


	/**
	 *  Displays the InputFields corresponding to metadata fields that allow for
	 *  multiple values (for debugging).
	 */
	public void displayMultiValueFields() {
		prtln("** MultiValue Fields **");
		for (Iterator em = getMultiValueFields().iterator(); em.hasNext(); ) {
			InputField field = (InputField) em.next();
			if (schemaHelper.isRequiredBranch(field.getSchemaNode())) {
				prtln(field.getXPath() + " (required)");
			} else {
				prtln(field.getXPath());
			}
			prtln("\t" + field.getValue());
		}
		prtln("----------\n");
	}


	/**
	 *  Debugging method to display all request parameters
	 */
	public void displayAllParams() {
		//print out all the query params
		prtln("\nREQUEST PARAMETERS");
		Enumeration rp = request.getParameterNames();
		while (rp.hasMoreElements()) {
			String paramName = (String) rp.nextElement();
			String paramVal = request.getParameter(paramName);
			prtln(paramName + ":  " + paramVal);
		}
		prtln("-----------------");
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 *@return    NOT YET DOCUMENTED
	 */
	public String toString() {
		String s = "\nInputManager fields";
		String NL = "\n\t";
		for (Iterator i = this.fieldsMap.keySet().iterator(); i.hasNext(); ) {
			String key = (String) i.next();
			s += NL + key;
		}
		return s;
	}


	/**
	 *  Description of the Method
	 */
	public void destroy() {
		refResolver = null;

		fieldsMap = null;
		attributeFields = null;
		elementFields = null;
		multiValueFields = null;
		anyTypeFields = null;
	}

	public static void setDebug (boolean bool) {
		debug = bool;
	}
	
	public static boolean getDebug () {
		return debug;
	}

	/**
	 *  Description of the Method
	 *
	 *@param  s  Description of the Parameter
	 */
	private static void prtln(String s) {
		if (debug) {
			SchemEditUtils.prtln(s, "InputManager");
		}
	}
}


