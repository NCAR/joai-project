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

import java.util.*;

import org.apache.struts.action.ActionError;
import org.apache.struts.action.ActionMapping;
import javax.servlet.http.HttpServletRequest;

/**
 *  Extension of SchemEditValidator that implements special (non-schema)
 *  validation for OSM framework.
 *
 * @author    ostwald
 */
public class OsmEditValidator extends SchemEditValidator {

	private static boolean debug = false;
	final String PUBNAME_PATH = "/record/general/pubName";


	/**
	 *  Constructor for the OsmEditValidator object
	 *
	 * @param  sef        Description of the Parameter
	 * @param  framework  Description of the Parameter
	 * @param  request    Description of the Parameter
	 * @param  mapping    NOT YET DOCUMENTED
	 */
	public OsmEditValidator(SchemEditForm sef,
	                        MetaDataFramework framework,
	                        ActionMapping mapping,
	                        HttpServletRequest request) {
		super(sef, framework, mapping, request);
	}


	/**
	 *  Suppress validation of the pubName field, since it is handled in
	 *  validateForm
	 *
	 * @param  inputField  Description of the Parameter
	 * @return             Description of the Return Value
	 */
	protected boolean skipFieldValidation(InputField inputField) {
		return inputField.getNormalizedXPath().equals(PUBNAME_PATH);
	}


	/**
	 *  Custom validator for pubName field that throws a validation error when this
	 *  field is empty, even though the schema allows it to be empty.
	 *
	 * @return    Description of the Return Value
	 */
	public SchemEditActionErrors validateForm() {
		prtln("validateForm()  currentPage: " + this.sef.getCurrentPage());
		SchemEditActionErrors errors = new SchemEditActionErrors(schemaHelper);
		boolean doValidate = false;

		if (this.sef.getCurrentPage().equals("general")) {
			prtln("beginning custom validation");

			// find cases where a url (optional) is defined but the corresponding name (required) is not.
			for (Iterator i = this.im.getInputFields().iterator(); i.hasNext(); ) {
				InputField field = (InputField) i.next();
				prtln(" .. " + field.getXPath() + "\n\t (" + field.getNormalizedXPath() + ")");
				if (field.getNormalizedXPath().equals(PUBNAME_PATH)) {
					prtln("processing: " + field.getXPath());

					String value = field.getValue().trim();
					String xpath = field.getXPath();

					if ((value == null) || (value.length() == 0)) {
						SchemEditErrors.addError(errors, field, "field.required");
						exposeField(field);
					}
					else {
						// there is a value -  validate against schema
						try {
							im.checkValidValue(field);
						} catch (Exception cv) {
							SchemEditErrors.addXSDdatatypeError(errors, field, "invalid.value", cv.getMessage());
							exposeField(field);
						}
					}
				}
			}

		}

		// now call normal validation methods
		errors.add(super.validateForm());

		return errors;
	}


	/**
	 *  Print a line to standard out.
	 *
	 * @param  s  The String to print.
	 */
	private static void prtln(String s) {
		if (debug) {
			SchemEditUtils.prtln(s, "OsmEditValidator");
		}
	}

}

