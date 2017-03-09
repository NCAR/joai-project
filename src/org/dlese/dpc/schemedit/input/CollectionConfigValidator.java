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
import org.dlese.dpc.schemedit.config.*;
import org.dlese.dpc.schemedit.action.form.SchemEditForm;

import org.dlese.dpc.xml.schema.*;

import java.util.*;

import org.apache.struts.action.ActionError;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionMapping;
import javax.servlet.http.HttpServletRequest;


/**
 *  Extends SchemEditValidator to provide validation services for collection configuration
 records. Specifically, ensures that status flags are not duplicated,
 *  nor do they redefine reserved flags<p>
 *
 *
 *
 *@author    ostwald <p>
 *
 *      $Id: CollectionConfigValidator.java,v 1.10 2005/06/02 18:45:40 ostwald
 *      Exp $
 */
public class CollectionConfigValidator extends SchemEditValidator {

	private static boolean debug = false;
	private CollectionRegistry collectionRegistry = null;


	/**
	 *  Constructor for the CollectionConfigValidator object
	 *
	 *@param  sef                 Description of the Parameter
	 *@param  framework           Description of the Parameter
	 *@param  mapping             Description of the Parameter
	 *@param  request             Description of the Parameter
	 *@param  collectionRegistry  Description of the Parameter
	 */
	public CollectionConfigValidator(CollectionRegistry collectionRegistry,
			SchemEditForm sef,
			MetaDataFramework framework,
			ActionMapping mapping,
			HttpServletRequest request) {
		super(sef, framework, mapping, request);
		this.collectionRegistry = collectionRegistry;
	}


	/**
	 *  In addition to validating against the schema, check statusFlags for duplicates and
	 create SchemEditErrors if dups found.
	 *
	 *@return    Description of the Return Value
	 */
	public SchemEditActionErrors validateForm() {
		prtln("validateForm");
		// im.displayAttributeFields();
		// im.displayElementFields();

		SchemEditActionErrors errors = super.validateForm();

		// make a list of reserved status flag labels for this collection
		String collection = sef.getRecId();
		prtln("collection: " + collection);
		CollectionConfig collectionConfig = collectionRegistry.getCollectionConfig(collection);
		if (collectionConfig == null) {
			prtln("collectionConfig not found");
		}

		List unavailableFlags = new ArrayList();
		for (Iterator i = StatusFlags.reservedStatusLabels().iterator(); i.hasNext(); ) {
			String label = (String) i.next();
			unavailableFlags.add(label.toLowerCase());
		}

/* 		prtln("\n reserved status labels");
		for (Iterator i = unavailableFlags.iterator(); i.hasNext(); ) {
			prtln("\t" + (String) i.next());
		} */

		// ensure the idPrefix is not already assigned
		InputField idPrefixField = this.getIdPrefixField();
		
		// Debugging
		if (idPrefixField != null) {
			prtln ("\nidPrefixField.getValue(): " + idPrefixField.getValue());
			prtln ("dup? " + collectionRegistry.isDuplicateIdPrefix(collection, idPrefixField.getValue()));
		}
		else
			prtln ("idPrefixField is NULL");
		
		// don't flag dup error if the idPrefix is null or empty - it will be caught as an empty required field.
		if (idPrefixField != null &&
			idPrefixField.getValue().trim().length() > 0 &&
			collectionRegistry.isDuplicateIdPrefix(collection, idPrefixField.getValue())) {
				
			SchemEditErrors.addError(errors, idPrefixField, "dup.idPrefix.error");
			exposeField(idPrefixField);
		}

		
		// check the finalStatusLabel
		// if it's okay, then add it to flaglist
		// if not, create error
		InputField finalStatusField = getFinalStatusFlagField();
		if (finalStatusField == null) {
			// prtln(" .... not found");
		}
		else {
			String finalStatusLabel = finalStatusField.getValue().toLowerCase();
			// prtln("finalStatusLabel: " + finalStatusLabel);
			if (!finalStatusLabel.equals(StatusFlags.DEFAULT_FINAL_STATUS.toLowerCase()) &&
					unavailableFlags.contains(finalStatusLabel)) {
				SchemEditErrors.addError(errors, finalStatusField, "status.flag.error");
				exposeField(finalStatusField);
			}
			else {
				unavailableFlags.add(finalStatusLabel);
			}
		}

		// check each statusFlag element and if it is on the list, create error
		// prtln("\n *** status flags ***");
		for (Iterator i = getStatusFlagFields().iterator(); i.hasNext(); ) {
			InputField field = (InputField) i.next();
			String statusLabel = field.getValue().toLowerCase();
			// prtln(statusLabel);
			if (unavailableFlags.contains(statusLabel)) {
				// prtln("  ... is illegal");
				SchemEditErrors.addError(errors, field, "status.flag.error");
				exposeField(field);
			}
			else {
				unavailableFlags.add(statusLabel);
			}
		}
		return errors;
	}

	private InputField getInputField (String xpath) {
		for (Iterator i = im.getInputFields().iterator(); i.hasNext(); ) {
			InputField field = (InputField) i.next();
			if (field.getXPath().equals(xpath)) {
				return field;
			}
		}
		return null;
	}		
	
	/**
	 *  Gets the finalStatusFlag InputField from the InputManager
	 *
	 *@return    The finalStatusFlagField value
	 */
	private InputField getFinalStatusFlagField() {
		String finalStatusLabelPath = "/collectionConfigRecord/statusFlags/@finalStatusLabel";
/* 		for (Iterator i = im.getInputFields().iterator(); i.hasNext(); ) {
			InputField field = (InputField) i.next();
			// prtln (field.toString());
			if (field.getXPath().equals(finalStatusLabelPath)) {
				return field;
			}
		}
		return null; */
		return this.getInputField(finalStatusLabelPath);
	}

	private InputField getIdPrefixField () {
		String idPrefixPath = "/collectionConfigRecord/idPrefix";
		return this.getInputField(idPrefixPath);
	}

	/**
	 *  Gets the statusFlag InputFields from the InputManager
	 *
	 *@return    The statusFlagFields value
	 */
	private List getStatusFlagFields() {
		List list = new ArrayList();
		String statusFlagPath = "/collectionConfigRecord/statusFlags/statusFlag/status";
		for (Iterator i = im.getInputFields().iterator(); i.hasNext(); ) {
			InputField field = (InputField) i.next();
			// prtln (field.toString());
			if (field.getNormalizedXPath().startsWith(statusFlagPath)) {
				list.add(field);
			}
		}
		return list;
	}


	/**
	 *  Print a line to standard out.
	 *
	 *@param  s  The String to print.
	 */
	private static void prtln(String s) {
		if (debug) {
			// System.out.println("CollectionConfigValidator: " + s);
			System.out.println(s);
		}
	}

}

