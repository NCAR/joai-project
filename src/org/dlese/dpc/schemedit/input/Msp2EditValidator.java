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
Extension of SchemEditValidator that is only concerned with a few msp2-specific fields.
 *
 * @author    ostwald
 *
 *
 */
public class Msp2EditValidator extends SchemEditValidator {

	private static boolean debug = false;
	private final String SUBJECTS_PATH = "/record/general/subjects";

	/**
	 *  Constructor for the Msp2EditValidator object
	 *
	 * @param  sef        Description of the Parameter
	 * @param  framework  Description of the Parameter
	 * @param  request    Description of the Parameter
	 * @param  mapping    NOT YET DOCUMENTED
	 */
	public Msp2EditValidator(SchemEditForm sef,
	                          MetaDataFramework framework,
	                          ActionMapping mapping,
	                          HttpServletRequest request) {
		super (sef, framework, mapping, request);

	}

	public SchemEditActionErrors validateForm() {
		prtln("validateForm()  currentPage: " + this.sef.getCurrentPage());
		SchemEditActionErrors errors = new SchemEditActionErrors(schemaHelper);
		
		// validation: we flag error at the SUBJECTS_PATH level if none of it's children have a value
		if (this.sef.getCurrentPage().equals ("general")) {
			boolean hasValue = false;
			
			prtln ("beginning custom validation");
			// iterate over all children of SUBJECTS_PATH, looking for a non-empty value
			for (Iterator i=this.im.getInputFields().iterator();i.hasNext();) {
				InputField field = (InputField)i.next();
				if (field.getNormalizedXPath().startsWith(SUBJECTS_PATH + "/")) {
					prtln ("looking at " + field.getXPath());
					if (field.getValue().trim().length() > 0) {
						prtln (" .. hasValue! (" + field.getValue() + ")");
						hasValue = true;
						break;
					}
				}
			}
			if (!hasValue) {
				prtln ("adding error!");
				String msg = "At least one child field requires a value";
				SchemEditErrors.addGenericError(errors, SUBJECTS_PATH, msg);
				// exposeField(subjectsField);  / no inputField for this element - should we create one?
			}
		}
		
		errors.add(super.validateForm());

		return errors;
	}
	
	/*
	* Suppress validation of all fields under SUBJECTS_PATH
	*/
	protected boolean skipFieldValidation (InputField inputField) {
		return inputField.getNormalizedXPath().startsWith (SUBJECTS_PATH);
	}
	
	/**
	 *  Print a line to standard out.
	 *
	 * @param  s  The String to print.
	 */
	private static void prtln(String s) {
		if (debug) {
			SchemEditUtils.prtln(s, "Msp2EditValidator");
		}
	}

}

