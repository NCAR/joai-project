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

import org.dlese.dpc.schemedit.url.UrlHelper;
import org.dlese.dpc.schemedit.action.form.SchemEditForm;
import org.dlese.dpc.schemedit.MetaDataFramework;
import org.dlese.dpc.schemedit.SchemEditUtils;

import org.dlese.dpc.xml.schema.*;
import org.dlese.dpc.xml.schema.compositor.Choice;
import org.dlese.dpc.xml.*;

import java.util.*;
import java.io.*;
import java.text.ParseException;
import java.net.MalformedURLException;

import org.apache.struts.action.ActionError;
import org.apache.struts.action.ActionMapping;
import javax.servlet.http.HttpServletRequest;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Attribute;
import org.dom4j.Node;

/**
 *  Validates the metadata fields present in a http request.<p>
 *
 *  The fields to be validated are filtered from the request, and the fields of
 *  like "type" (Attribute, Element, MulitValue) are validated as a set. The
 *  validation methods return {@link org.apache.struts.action.ActionErrors}
 *  instances that have been populated by the {@link org.dlese.dpc.schemedit.SchemEditErrors}.
 *
 * @author    ostwald
 *
 *
 */
public class SchemEditValidator {

	private static boolean debug = false;

	/**  the ActionForm providing fields to validate */
	protected SchemEditForm sef;
	/**  the schemaHelper provided schema info */
	protected SchemaHelper schemaHelper;
	/**  NOT YET DOCUMENTED */
	protected MetaDataFramework framework;
	/**  NOT YET DOCUMENTED */
	protected HttpServletRequest request;
	/**  NOT YET DOCUMENTED */
	protected InputManager im;
	/**  NOT YET DOCUMENTED */
	protected DocMap docMap = null;

	List multiValueFields = null;


	/**
	 *  Constructor for the SchemEditValidator object
	 *
	 * @param  sef        Description of the Parameter
	 * @param  framework  Description of the Parameter
	 * @param  request    Description of the Parameter
	 * @param  mapping    NOT YET DOCUMENTED
	 */
	public SchemEditValidator(SchemEditForm sef,
	                          MetaDataFramework framework,
	                          ActionMapping mapping,
	                          HttpServletRequest request) {
		this.sef = sef;
		this.docMap = sef.getDocMap();
		this.framework = framework;
		this.schemaHelper = framework.getSchemaHelper();
		this.request = request;
		// used inputManager cached in ActionForm if possible
		if (this.sef.getInputManager() != null) {
			this.im = sef.getInputManager();
		}
		else {
			this.im = new InputManager(request, framework);
		}
	}


	/**
	 *  Gets the inputManager attribute of the SchemEditValidator object
	 *
	 * @return    The inputManager value
	 */
	public InputManager getInputManager() {
		return this.im;
	}

	/*
	* Hook to allow subclasses to suppress validation of specific fields.
	*/
	protected boolean skipFieldValidation (InputField inputField) {
		return false;
	}

	/**
	 *  Validates the metadata fields contained in a request by calling <code>validateMultiValueFields</code>
	 *  , then <code>validateAttributeFields</code>, and finally <code>validateElementFields</code>
	 *  .
	 *
	 * @return    validation errors found during the process of validation
	 */
	public SchemEditActionErrors validateForm() {
		// prtln("validateForm");
		SchemEditActionErrors errors = new SchemEditActionErrors(schemaHelper);

		// pruneRepeatingFields();  // DISABLED 5/11/04 see method
		errors.add(validateMultiValueFields());
		errors.add(validateAttributeFields());
		errors.add(validateElementFields());

		return errors;
	}


	/**
	 *  Predicate to identify if the input contains entities that could not be resolved.
	 *
	 * @return    NOT YET DOCUMENTED
	 */
	public boolean hasEntityErrors() {
		return !getInputManager().getEntityErrorFields().isEmpty();
	}


	/**
	 *  Gets the entityErrors attribute of the SchemEditValidator object
	 *
	 * @return    The entityErrors value
	 */
	public SchemEditActionErrors getEntityErrors() {
		SchemEditActionErrors errors = new SchemEditActionErrors(schemaHelper);
		List errorFields = getInputManager().getEntityErrorFields();
		for (Iterator f = errorFields.iterator(); f.hasNext(); ) {
			InputField field = (InputField) f.next();
			List entityErrors = field.getEntityErrors();
			String errorMsg = "";
			for (Iterator e = entityErrors.iterator(); e.hasNext(); ) {
				ParseException entityError = (ParseException) e.next();
				String ref = "&amp;" + entityError.getMessage().substring(1);
				errorMsg += "<br/>&nbsp;-&nbsp;" + ref + " could not be resolved (at offset " + entityError.getErrorOffset() + ")";
			}
			SchemEditErrors.addEntityError(errors, field, errorMsg);
			docMap.put(field.getXPath(), field.getValue());
			exposeField(field);
		}
		return errors;
	}


	/**
	 *  Description of the Method
	 *
	 * @return    Description of the Return Value
	 */
	public SchemEditActionErrors validateDocument() {
		prtln("validateDocument");
		SchemEditActionErrors errors = new SchemEditActionErrors(schemaHelper);

		prtln(" ... not yet implemented");

		return errors;
	}


	/**
	 *  Removes empty elements of repeating fields.<p>
	 *
	 *  DISABLED 5/11/04 - the repository will contain non-valid elements, so we
	 *  don't worry so much about getting rid of the empty elements (many of which
	 *  will be invalid due to the "stringTextType" convention) Removes empty child
	 *  elements from each repeating field in the toPrune list. Children are pruned
	 *  if they are empty and their occurance attribute allows them to be deleted.
	 */
	protected void pruneRepeatingFields() {
		prtln ("\nPruneRepeatingFields()");
		List toPruneList = sef.getRepeatingFieldsToPrune();

		// for each repeating field on the toPrune list
		for (Iterator p = toPruneList.iterator(); p.hasNext(); ) {
			String parentPath = (String) p.next();
			prtln ("\t parent: " + parentPath);
			Element parent = (Element) docMap.selectSingleNode(parentPath);
			if (parent == null) {
				prtln("pruneRepeatingFields(): Element not found for " + parentPath);
				continue;
			}
			List children = parent.elements();
			SchemaNode schemaNode = schemaHelper.getSchemaNode(XPathUtils.normalizeXPath(parentPath));
			if (schemaNode == null) {
				prtln("pruneRepeatingFields() couldn't find SchemaNode");
				continue;
			}

			for (int i = children.size() - 1; i > -1; i--) {
				Element child = (Element) children.get(i);
				String childName = child.getName();
				prtln ("\t     childName: " + childName);
				if (!Dom4jUtils.isEmpty(child)) {
					continue;
				}
				if (parent.elements().size() >= schemaNode.getMinOccurs()) {
					String childPath = child.getPath();
					parent.remove(child);
				}
			}
			parent.setText("");
		}
		sef.clearRepeatingFieldsToPrune();
	}


	/**
	 *  Validate the input fields corresponding to elements in the DocMap. NOTE: do
	 *  not bother validating elements that are not present in the DocMap!.
	 *
	 * @return    Description of the Return Value
	 */
	protected SchemEditActionErrors validateElementFields() {
		prtln ("\nvalidateElementFields()");
		SchemEditActionErrors errors = new SchemEditActionErrors(schemaHelper);

		// step through the element fields
		List fieldsToValidate = new ArrayList();
		fieldsToValidate.addAll(im.getElementFields());

		/*
			For now, we do not attempt to validate anyType fields.
			This probably has to be done as a seperate method, since
			there are tricky interactions with SchemEditForm.validate()
		*/
		fieldsToValidate.addAll(im.getAnyTypeFields());
		Iterator fields = fieldsToValidate.iterator();
		while (fields.hasNext()) {
			InputField field = (InputField) fields.next();
			
			if (skipFieldValidation (field)) continue;
			
			String value = field.getValue();
			String xpath = field.getXPath();
			SchemaNode schemaNode = field.getSchemaNode();

			prtln ("\t" + xpath);
			
			// don't bother validating if node does not exist in DocMap
			if (!docMap.nodeExists(xpath) && !field.isAnyType()) {
				prtln("validateElementFields: node not in DocMap: " + xpath);
				continue;
			}

			// if there is no value, AND
			//   the element is (a choice element OR (requiredContent AND NOT nillable)), warn user
			// 2/16/07 - don't trim input. whitespace facet handled in InputField
			// if ((value == null) || (value.trim().length() == 0)) {
			if ((value == null) || (value.length() == 0)) {

				
				// trap requiredChoices
				if (schemaNode.hasChoiceCompositor()) {

					Choice choice = (Choice) schemaNode.getCompositor();
					if (choice.isRequiredChoice()) {
						SchemEditErrors.addError(errors, field, "choice.required");
						exposeField(field);
					}
				}
				else if (schemaHelper.isRequiredContentElement(schemaNode)) {
					// field.isRequired()
					if (field.isNillable()) {
						// prtln("nillable element: okay to be empty");
					}
					else {
						SchemEditErrors.addError(errors, field, "field.required");
						exposeField(field);
					}
				}

				/*
				    disabling the following check: 4/9/04 - because sometimes
				    it's okay to have an empty parent field with populated children
				    (see Collection.relations.relation)!?
				  */
				else if (!docMap.isEmpty(xpath)) {
					// prtln("element with children missing value: " + field.getParamName());
					// SchemEditErrors.addEmptyWithChildError (errors, field, "empty.element.with.children");
				}
				else {
					/*
					    we have an empty element. we used to delete such empty elements but
					    now are leaving them for the user to delete 5/11/04
					    prtln ("deleting empty element: " + field.getParamName());
					    docMap.remove(xpath);
					  */
					//prtln ("validateElementFields NOT removing empty element: " + xpath);
				}
			}
			else {
				// there is a value
				try {
					im.checkValidValue(field);
				} catch (Exception cv) {
					SchemEditErrors.addXSDdatatypeError(errors, field, "invalid.value", cv.getMessage());
					exposeField(field);
				}

				// validate fields whose paths are defined in the frameworkConfig as "url type"
				List urlPaths = framework.getUrlPaths();

				if (urlPaths != null && urlPaths.contains(field.getNormalizedXPath())) {
					try {
						UrlHelper.validateUrl(field.getValue());
					} catch (MalformedURLException e) {
						String errorMsg = "";
						if (e.getMessage() != null)
							errorMsg = e.getMessage();
						prtln("MalformedURLException!\n\t" + errorMsg);
						SchemEditErrors.addInvalidUrlError(errors, field.getXPath(), errorMsg);
						exposeField(field);
						return errors;
					}
				}
			}
		}

		return errors;
	}


	/**
	 *  Checks all required attribute fields for a value, and all populated
	 *  attributes for schema-type compliance.
	 *
	 * @return    Description of the Return Value
	 */
	protected SchemEditActionErrors validateAttributeFields() {
		// im.displayAttributeFields();

		SchemEditActionErrors errors = new SchemEditActionErrors(schemaHelper);
		// step through the attribute fields
		for (Iterator ai = im.getAttributeFields().iterator(); ai.hasNext(); ) {
			InputField field = (InputField) ai.next();
			
			if (skipFieldValidation (field)) continue;
			
			String value = field.getValue();
			String xpath = field.getXPath();

			try {
				if (!docMap.nodeExists(xpath)) {
					/*
						why wasn't a node for this attribute created already??
							we could simply create the attribute (as long as it exists in the schemaNodeMap),
							but i think it should have already been created ...
					// prtln ("docMap node does not exist for " + xpath);
					// continue;
					/* docMap
					*/
					throw new Exception("docMap node does not exist for " + xpath);
				}
			} catch (Exception e) {
				e.printStackTrace();
				continue;
			}

			// if there is no value, AND the attribute is required, warn user
			if ((value == null) || (value.length() == 0)) {
				if (schemaHelper.isRequiredAttribute(field.getSchemaNode())) {
					// prtln ("required attribute missing: " + field.getParamName());

					SchemEditErrors.addError(errors, field, "field.required");
					exposeField(field);
				}
				else {
					// prtln("deleting empty attribute: " + field.getParamName());
					docMap.remove(field.getXPath());
				}
			}
			else {
				// there is a value - validate it
				try {
					im.checkValidValue(field);
				} catch (Exception cv) {
					SchemEditErrors.addXSDdatatypeError(errors, field, "invalid.value", cv.getMessage());
					exposeField(field);

				}
			}
		}
		return errors;
	}


	/**
	 *  Validate the multivalue parameters managed by the input manager. For each
	 *  different group of multivalue elements found, remove all existing elements,
	 *  then add the NON-EMPTY params from the input, and finally validate,
	 *  returning errors.
	 */
	public void updateMultiValueFields() {
		prtln ("\nupdateMultiValueFields()");
		// im.displayMultiValueFields();

		String currentElementPath = "";
		List mulitValueGroups = new ArrayList();

		// traverse all multivalue fields in the InputManager
		for (Iterator i = im.getMultiValueFields().iterator(); i.hasNext(); ) {
			InputField field = (InputField) i.next();
			// prtln("processing field: " + field.getXPath());

			/*
				we are only concerned with fields that are represented as multiboxes. fields
				that are not unbounded are represented as select objects and therefore will
				always have a value and do not need to be processed here
			*/
			boolean hasVocabLayout = false;
			try {
				hasVocabLayout = this.framework.getVocabLayouts().hasVocabLayout(field.getNormalizedXPath());
			} catch (Throwable t) {}
			if (hasVocabLayout) {
				// prtln ("\t .. hasVocabLayout: " + field.getNormalizedXPath());
			}
		
			if (!field.getSchemaNode().isUnbounded() && !hasVocabLayout) {
				// prtln("  .. not unbounded: continuing");
				continue;
			}
			String siblingXPath = XPathUtils.getSiblingXPath(field.getXPath());

			// is this a new group? groups are identified by the siblingXPath (which identifies all members)
			if (!siblingXPath.equals(currentElementPath)) {
				currentElementPath = siblingXPath;
				mulitValueGroups.add(field);
				// prtln("   added new group: " + field.getFieldName() + "(" + siblingXPath + ")");

				// delete all siblings at the currentElementPath
				try {
					docMap.removeSiblings(currentElementPath);
				} catch (Exception e) {
					// this is not always an error ...
					// prtln("error removing siblings for multivalue: " + e.getMessage());
				}
			}

			// add the value of the current field.
			String value = field.getValue();
			if ((value != null) && (value.trim().length() > 0)) {
				try {
					// prtln("  ... about to create a new node at " + field.getXPath());
					Node newNode = docMap.createNewSiblingNode(field.getXPath());
					newNode.setText(value);
				} catch (Throwable t) {
					prtln("updateMultiValueFields ERROR: " + t.getMessage());
					// t.printStackTrace();
				}
			}
		}
		multiValueFields = mulitValueGroups;
	}


	/**
	 *  Ensures that all fields expecting 1 or more values have at least one value;
	 *
	 * @return    Description of the Return Value
	 */
	protected SchemEditActionErrors validateMultiValueFields() {
		prtln ("\nvalidateMultiValueFields()");
		// im.displayMultiValueFields();
		updateMultiValueFields();
		SchemEditActionErrors errors = new SchemEditActionErrors(schemaHelper);

		// validate each mulitvalueGroup: make sure required fields have at least
		// one node in the DocMap
		for (Iterator i = multiValueFields.iterator(); i.hasNext(); ) {
			InputField group = (InputField) i.next();
			
			if (skipFieldValidation (group)) continue;
			
			String groupPath = group.getNormalizedXPath();
			prtln("validateMultiValueFields(): group: " + groupPath);


			if (group.getSchemaNode().isRequired()) {
				List list = docMap.selectNodes(groupPath);
				if (list.size() == 0) {
					SchemEditErrors.addError(errors, group, "field.required");
					exposeField(group);
				}
			}
		}
		return errors;
	}


	/**
	 *  Guards against deletion of the last required element, causing a new empty
	 *  element to be automatically inserted after the last one is deleted. But
	 *  this guard is relaxed for choice and substitutionGroup elements, since we
	 *  can't know which element to replace the last one with.
	 *
	 * @param  pathArg  NOT YET DOCUMENTED
	 * @return          The lastRequiredRepeatingElement value
	 */
	public boolean isLastRequiredRepeatingElement(String pathArg) {
		prtln("isLastRequiredRepeatingElement() ... " + pathArg);
		String xpath = XPathUtils.decodeXPath(pathArg);
		String siblingPath = XPathUtils.getSiblingXPath(xpath);
		List siblingList = docMap.selectNodes(siblingPath);

		prtln("\tfound " + siblingList.size() + " siblings");

		if (siblingList.size() == 1) {
			SchemaNode schemaNode = schemaHelper.getSchemaNode(xpath);
			if (schemaNode == null) {
				prtln("isLastRequiredRepeatingElement():schemaNode not found for " + xpath);
				return false;
			}

			prtln("schemaNode: \n" + schemaNode.toString());

			// allow to delete last choice or substitutionGroup elements
			if (schemaNode.getMinOccurs() == 1 &&
				!schemaHelper.isChoiceElement(schemaNode) &&
				!schemaNode.isSubstitutionGroupMember()) {

				// check that parent also has minOccurs of 1
				String parentPath = XPathUtils.getParentXPath(xpath);
				SchemaNode parentSchemaNode = schemaHelper.getSchemaNode(parentPath);
				if (parentSchemaNode.getMinOccurs() == 1) {
					return true;
				}
				else {
					prtln("minOccurs not equal to one - returning false");
				}
			}
		}
		return false;
	}


	/**
	 *  Guard against deletion of last member of a required repeating sequence.
	 *
	 * @param  pathArg  Description of the Parameter
	 * @return          Description of the Return Value
	 */
	public SchemEditActionErrors validateDeletion(String pathArg) {
		SchemEditActionErrors errors = new SchemEditActionErrors(schemaHelper);
		String xpath = XPathUtils.decodeXPath(pathArg);
		String siblingPath = XPathUtils.getSiblingXPath(xpath);
		List siblingList = docMap.selectNodes(siblingPath);

		// prtln("\tfound " + siblingList.size() + " siblings");

		if (siblingList.size() == 1) {
			SchemaNode schemaNode = schemaHelper.getSchemaNode(xpath);
			if (schemaNode == null) {
				prtln("\tschemaNode not found for " + xpath);
			}
			Element element = (Element) docMap.selectSingleNode(xpath);
			if (element == null) {
				prtln("delete: node not found at " + xpath);
				return errors;
			}

			// allow to delete choice elements, even if they are required
			if (schemaNode.getMinOccurs() == 1 &&
				!schemaHelper.isChoiceElement(schemaNode)) {

				// check that parent also has minOccurs of "1"
				String parentPath = XPathUtils.getParentXPath(xpath);
				SchemaNode parentSchemaNode = schemaHelper.getSchemaNode(parentPath);
				if (parentSchemaNode.getMinOccurs() == 1) {
					String paramName = "valueOf(" + pathArg + ")";
					String elementName = XPathUtils.getLeaf(siblingPath);
					SchemEditErrors.addError(errors, paramName, "cant.delete", elementName, pathArg);
				}
			}
		}
		return errors;
	}


	/**
	 *  Description of the Method
	 *
	 * @param  field  Description of the Parameter
	 */
	protected void exposeField(InputField field) {
		// prtln ("\n\nexposeField with " + field.getParamName());
		String xpath = field.getXPath();
		if (xpath != null && xpath.length() > 0) {
			sef.exposeNode(xpath);
		}
	}


	/**  Rid the instanceDocument of any non-required empty fields */
	public void pruneInstanceDoc() {
		prtln ("\npruneInstanceDoc");
		DocumentPruner.pruneDocument(docMap.getDocument(), this.schemaHelper);
	}

	/**
	 *  Print a line to standard out.
	 *
	 * @param  s  The String to print.
	 */
	private static void prtln(String s) {
		if (debug) {
			SchemEditUtils.prtln(s, "SchemEditValidator");
		}
	}

}

