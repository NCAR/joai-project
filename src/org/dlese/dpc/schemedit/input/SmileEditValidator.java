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

import org.dlese.dpc.schemedit.action.form.SchemEditForm;
import org.dlese.dpc.schemedit.MetaDataFramework;
import org.dlese.dpc.schemedit.SchemEditUtils;
import org.dlese.dpc.xml.XPathUtils;

import java.util.*;

import org.apache.struts.action.ActionError;
import org.apache.struts.action.ActionMapping;
import javax.servlet.http.HttpServletRequest;


/**
 *  Extension of SchemEditValidator that is only concerned with a few
 *  msp2-specific fields.
 *
 *@author     ostwald
 *@created    June 25, 2009
 */
public class SmileEditValidator extends SchemEditValidator {

	private static boolean debug = false;
	private Map pathMap = null;
	final String MATERIALS_LIST_PATH = "/smileItem/costTimeMaterials/materialsList";


	/**
	 *  Constructor for the SmileEditValidator object
	 *
	 *@param  sef        Description of the Parameter
	 *@param  framework  Description of the Parameter
	 *@param  request    Description of the Parameter
	 *@param  mapping    NOT YET DOCUMENTED
	 */
	public SmileEditValidator(SchemEditForm sef,
			MetaDataFramework framework,
			ActionMapping mapping,
			HttpServletRequest request) {
		super(sef, framework, mapping, request);

		this.pathMap = new HashMap();
		for (Iterator i = this.im.getElementFields().iterator(); i.hasNext(); ) {
			InputField field = (InputField) i.next();
			this.pathMap.put(field.getXPath(), field);
		}
	}


	/**
	 *  Suppress validation of all fields under MATERIALS_LIST_PATH
	 *
	 *@param  inputField  Description of the Parameter
	 *@return             Description of the Return Value
	 */
	protected boolean skipFieldValidation(InputField inputField) {
		return inputField.getNormalizedXPath().startsWith(MATERIALS_LIST_PATH);
	}


	/**
	 *  Custom validator for MATERIALS_LIST_PATH
	 *
	 *@return    Description of the Return Value
	 */
	public SchemEditActionErrors validateForm() {
		prtln("validateForm()  currentPage: " + this.sef.getCurrentPage());
		SchemEditActionErrors errors = new SchemEditActionErrors(schemaHelper);
		boolean doValidate = false;

		if (this.sef.getCurrentPage().equals("costTimeMaterials")) {
			prtln("beginning custom validation");

			// find cases where a url (optional) is defined but the corresponding name (required) is not.
			String normalizedNamePath = MATERIALS_LIST_PATH + "/materialsListItem/materialsListItemName";
			prtln("\nLooking for fields with a normalized path = " + normalizedNamePath);
			for (Iterator i = this.im.getInputFields().iterator(); i.hasNext(); ) {
				InputField field = (InputField) i.next();
				prtln(" .. " + field.getXPath() + "\n\t (" + field.getNormalizedXPath() + ")");
				if (field.getNormalizedXPath().equals(normalizedNamePath)) {
					prtln("processing: " + field.getXPath());
					// process the children

					InputField nameField = (InputField) this.pathMap.get(field.getXPath());
					String urlPath = XPathUtils.getParentXPath(nameField.getXPath()) + "/materialsListItemUrl";
					InputField urlField = (InputField) this.pathMap.get(urlPath);

					String url = urlField.getValue().trim();
					String name = nameField.getValue().trim();
					prtln("\tname: " + name + "  url: " + url);

					// validate the urlField against it's schemaType
					if (url.length() > 0) {
						try {
							im.checkValidValue(urlField);
						} catch (Exception cv) {
							SchemEditErrors.addXSDdatatypeError(errors, urlField, "invalid.value", cv.getMessage());
							exposeField(urlField);
						}

						// now flag cases where url is present but name is not
						if (name.length() == 0) {
							prtln("adding error!");
							SchemEditErrors.addError(errors, nameField, "field.required");
							exposeField(nameField);
						}
					}
				}
			}
		}

		errors.add(super.validateForm());

		return errors;
	}


	/**
	 *  Print a line to standard out.
	 *
	 *@param  s  The String to print.
	 */
	private static void prtln(String s) {
		if (debug) {
			SchemEditUtils.prtln(s, "SmileEditValidator");
		}
	}

}

