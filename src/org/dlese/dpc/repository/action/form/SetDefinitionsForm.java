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
package org.dlese.dpc.repository.action.form;

import org.dlese.dpc.repository.*;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionError;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.validator.ValidatorForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionServlet;
import org.apache.struts.util.MessageResources;

import javax.servlet.http.HttpServletRequest;

import java.io.Serializable;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

/**
 *  Bean for values used to add/edit OAI set definitions.
 *
 * @author     John Weatherley
 * @version    $Id: SetDefinitionsForm.java,v 1.17 2009/03/20 23:33:54 jweather Exp $
 */
public final class SetDefinitionsForm extends ValidatorForm implements Serializable {

	private String setName = null;
	private String setSpec = null;
	private String setDescription = null;
	private String setURL = null;

	private String[] includedDirs = new String[0];
	private String includedTerms = null;
	private String includedQuery = null;
	private String includedFormat = null;

	private String[] excludedDirs = new String[0];
	private String excludedTerms = null;
	private String excludedQuery = null;


	/**  Constructor for the SetDefinitionsForm Bean object */
	public SetDefinitionsForm() { }


	/**  Resets the data in the form to default values. Needed by Struts if form is stored in session scope. */
	public void reset() {
		setName = null;
		setSpec = null;
		setDescription = null;
		setURL = null;

		includedDirs = new String[0];
		includedTerms = null;
		includedQuery = null;
		includedFormat = null;

		excludedDirs = new String[0];
		excludedTerms = null;
		excludedQuery = null;
	}


	/**
	 *  Validates the form data.
	 *
	 * @param  mapping  The ActionMapping
	 * @param  request  The HttpServletRequest request
	 * @return          The ActionErrors
	 */
	public ActionErrors validate(ActionMapping mapping, HttpServletRequest request) {
		ActionErrors errors = super.validate(mapping, request);
		if (errors == null)
			errors = new ActionErrors();

		RepositoryManager rm = getRepositoryManager();

		// First, clear out the form based on which radio buttons were checked
		try {
			// Set to include all records:
			if (request.getParameter("include_radio").equalsIgnoreCase("include_radio_1")) {
				includedFormat = null;
				for (int i = 0; i < includedDirs.length; i++)
					includedDirs[i] = null;
			}
		} catch (Throwable t) {}
		try {
			// Set to include records of a given xmlFormat:
			if (request.getParameter("include_radio").equalsIgnoreCase("include_radio_2")) {
				for (int i = 0; i < includedDirs.length; i++)
					includedDirs[i] = null;
			}
		} catch (Throwable t) {}
		try {
			// Set to include records in the given directories:
			if (request.getParameter("include_radio").equalsIgnoreCase("include_radio_3")) {
				includedFormat = null;
			}
		} catch (Throwable t) {}

		try {
			// Set to have no limiting query clauses:
			if (request.getParameter("limit_radio").equalsIgnoreCase("limit_radio_1")) {
				includedTerms = null;
				includedQuery = null;
			}
		} catch (Throwable t) {}

		try {
			// Set to have no excludion query clauses:
			if (request.getParameter("exclude_radio").equalsIgnoreCase("exclude_radio_1")) {
				excludedTerms = null;
				excludedQuery = null;
				for (int i = 0; i < excludedDirs.length; i++)
					excludedDirs[i] = null;
			}
		} catch (Throwable t) {}

		// Verify the setSpec is not already being used:
		if (rm != null && setSpec != null && setSpec.trim().length() > 0) {
			String editSetSpec = request.getParameter("edit");

			// If editing an existing setSpec, verify that the new setSpec does not conflict with another existing setSpec
			if (editSetSpec != null && !editSetSpec.equals(setSpec) && rm.getHasOaiSetConfigured(setSpec)) {
				errors.add("setSpecAlreadyInUse", new ActionError("errors.setSpecAlreadyInUse"));
			}
			// If creating a new setSpec, make sure it does not already exist:
			else if (editSetSpec == null && rm.getHasOaiSetConfigured(setSpec)) {
				errors.add("setSpecAlreadyInUse", new ActionError("errors.setSpecAlreadyInUse"));
			}
		}

		// Verify that the setSpec meets the protocol specifications for URI unreserved characters:
		if (setSpec != null) {
			if (!setSpec.matches("([A-Za-z0-9\\-_\\.!~\\*'\\(\\)])+(:[A-Za-z0-9\\-_\\.!~\\*'\\(\\)]+)*")) {
				errors.add("setSpecSyntax", new ActionError("errors.setSpecSyntax"));
			}
		}

		// Make sure at least some meaningful definition has been provided:
		if (getIsDefinitionEmpty()) {
			errors.add("noDefinitionProvided", new ActionError("errors.noDefinitionProvided"));
		}

		// Make sure the dirs are either empty or valid dirs on the server
		for (int i = 0; i < includedDirs.length; i++) {
			String dir = includedDirs[i];
			if (dir != null && dir.trim().length() > 0) {
				File d = new File(dir);
				if (!d.isDirectory())
					errors.add("includedDirs[" + i + "]", new ActionError("errors.isdirectory"));
			}
		}

		return errors;
	}


	private RepositoryManager getRepositoryManager() {
		try {
			return (RepositoryManager) getServlet().getServletContext().getAttribute("repositoryManager");
		} catch (Throwable t) {
			//t.printStackTrace();
		}
		return null;
	}


	/**
	 *  Sets the setName attribute of the SetDefinitionsForm object
	 *
	 * @param  value  The new setName value
	 */
	public void setSetName(String value) {
		setName = (value == null ? null : value.trim());
	}


	/**
	 *  Sets the setSpec attribute of the SetDefinitionsForm object
	 *
	 * @param  value  The new setSpec value
	 */
	public void setSetSpec(String value) {
		setSpec = (value == null ? null : value.trim());
	}


	/**
	 *  Sets the setDescription attribute of the SetDefinitionsForm object
	 *
	 * @param  value  The new setDescription value
	 */
	public void setSetDescription(String value) {
		setDescription = (value == null ? null : value.trim());
	}


	/**
	 *  Sets the setURL attribute of the SetDefinitionsForm object
	 *
	 * @param  value  The new setURL value
	 */
	public void setSetURL(String value) {
		setURL = (value == null ? null : value.trim());
	}



	/**
	 *  Gets the setName attribute of the SetDefinitionsForm object
	 *
	 * @return    The setName value
	 */
	public String getSetName() {
		return setName;
	}


	/**
	 *  Gets the setSpec attribute of the SetDefinitionsForm object
	 *
	 * @return    The setSpec value
	 */
	public String getSetSpec() {
		return setSpec;
	}


	/**
	 *  Gets the setDescription attribute of the SetDefinitionsForm object
	 *
	 * @return    The setDescription value
	 */
	public String getSetDescription() {
		return setDescription;
	}


	/**
	 *  Gets the setURL attribute of the SetDefinitionsForm object
	 *
	 * @return    The setURL value
	 */
	public String getSetURL() {
		return setURL;
	}


	/**
	 *  Sets the includedDirs attribute of the SetDefinitionsForm object
	 *
	 * @param  value  The new includedDirs value
	 */
	public void setIncludedDirs(String[] value) {
		includedDirs = value;
	}


	/**
	 *  Gets the includedDirs attribute of the SetDefinitionsForm object
	 *
	 * @return    The includedDirs value
	 */
	public String[] getIncludedDirs() {
		return includedDirs;
	}


	/**
	 *  Gets the includedDirs that are defined for the set but that do not exist in the repository.
	 *
	 * @return    The includedDirsNotInRepository value
	 */
	public ArrayList getIncludedDirsNotInRepository() {
		return getConfiguredDirsNotInRepository(getIncludedDirs());
	}


	private final ArrayList getConfiguredDirsNotInRepository(String[] configuredDirs) {
		if (configuredDirs == null || configuredDirs.length == 0)
			return null;

		RepositoryManager rm = getRepositoryManager();
		if (rm == null)
			return null;
		ArrayList notIn = new ArrayList();
		for (int i = 0; i < configuredDirs.length; i++) {
			try {
				if (!rm.isDirectoryConfigured(new File(configuredDirs[i])))
					notIn.add(configuredDirs[i]);
			} catch (Throwable t) {
				// t.printStackTrace();
			}
		}

		return notIn;
	}




	/**
	 *  Gets the setInfos sorted by name, or null if none available.
	 *
	 * @return    The setInfos value
	 */
	public final ArrayList getSetInfos() {
		RepositoryManager rm = getRepositoryManager();
		if (rm == null)
			return null;
		ArrayList setInfos = rm.getSetInfosCopy();
		if (setInfos != null)
			Collections.sort(setInfos, SetInfo.getComparator("name"));
		return setInfos;
	}




	/**
	 *  Gets the includedFormat attribute of the SetDefinitionsForm object
	 *
	 * @return    The includedFormat value
	 */
	public String getIncludedFormat() {
		return includedFormat;
	}


	/**
	 *  True if the repository does not have the included format configured.
	 *
	 * @return    True if the repository does not have the included format configured.
	 */
	public boolean getIncludedFormatNotAvailable() {
		if (includedFormat == null || includedFormat.length() == 0)
			return false;

		RepositoryManager rm = getRepositoryManager();
		if (rm == null)
			return false;

		ArrayList configuredFormats = rm.getConfiguredFormats();
		if (configuredFormats == null)
			return true;
		return !configuredFormats.contains(includedFormat);
	}


	/**
	 *  Gets the includedTerms attribute of the SetDefinitionsForm object
	 *
	 * @return    The includedTerms value
	 */
	public String getIncludedTerms() {
		return includedTerms;
	}


	/**
	 *  Gets the includedQuery attribute of the SetDefinitionsForm object
	 *
	 * @return    The includedQuery value
	 */
	public String getIncludedQuery() {
		return includedQuery;
	}


	/**
	 *  Gets the excludedDirs attribute of the SetDefinitionsForm object
	 *
	 * @return    The excludedDirs value
	 */
	public String[] getExcludedDirs() {
		return excludedDirs;
	}


	/**
	 *  Gets the Excluded Dirs that are defined for the set but that do not exist in the repository.
	 *
	 * @return    The includedDirsNotInRepository value
	 */
	public ArrayList getExcludedDirsNotInRepository() {
		return getConfiguredDirsNotInRepository(getExcludedDirs());
	}


	/**
	 *  Gets the excludedTerms attribute of the SetDefinitionsForm object
	 *
	 * @return    The excludedTerms value
	 */
	public String getExcludedTerms() {
		return excludedTerms;
	}


	/**
	 *  Gets the excludedQuery attribute of the SetDefinitionsForm object
	 *
	 * @return    The excludedQuery value
	 */
	public String getExcludedQuery() {
		return excludedQuery;
	}


	/**
	 *  Sets the includedFormat attribute of the SetDefinitionsForm object
	 *
	 * @param  value  The new includedFormat value
	 */
	public void setIncludedFormat(String value) {
		includedFormat = (value == null ? null : value.trim());
	}


	/**
	 *  Sets the includedTerms attribute of the SetDefinitionsForm object
	 *
	 * @param  value  The new includedTerms value
	 */
	public void setIncludedTerms(String value) {
		includedTerms = (value == null ? null : value.trim());
	}


	/**
	 *  Sets the includedQuery attribute of the SetDefinitionsForm object
	 *
	 * @param  value  The new includedQuery value
	 */
	public void setIncludedQuery(String value) {
		includedQuery = (value == null ? null : value.trim());
	}


	/**
	 *  Sets the excludedDirs attribute of the SetDefinitionsForm object
	 *
	 * @param  value  The new excludedDirs value
	 */
	public void setExcludedDirs(String[] value) {
		excludedDirs = value;
	}


	/**
	 *  Sets the excludedTerms attribute of the SetDefinitionsForm object
	 *
	 * @param  value  The new excludedTerms value
	 */
	public void setExcludedTerms(String value) {
		excludedTerms = (value == null ? null : value.trim());
	}


	/**
	 *  Sets the excludedQuery attribute of the SetDefinitionsForm object
	 *
	 * @param  value  The new excludedQuery value
	 */
	public void setExcludedQuery(String value) {
		excludedQuery = (value == null ? null : value.trim());
	}


	/**
	 *  True if the set definition has nothing set.
	 *
	 * @return    True if the definition is empty.
	 */
	public boolean getIsDefinitionEmpty() {
		// Determines whether some meaningful definition has been provided:
		return (
			isEmpty(includedDirs) &&
			isEmpty(includedQuery) &&
			isEmpty(includedFormat) &&
			isEmpty(includedTerms) &&
			isEmpty(excludedDirs) &&
			isEmpty(excludedQuery) &&
			isEmpty(excludedTerms));
	}


	/* Determine whether a given String array is empty */
	private boolean isEmpty(String[] stringArray) {
		if (stringArray == null)
			return true;
		for (int i = 0; i < stringArray.length; i++)
			if (stringArray[i] != null && stringArray[i].trim().length() != 0)
				return false;
		return true;
	}


	/* Determine whether a given String is empty */
	private boolean isEmpty(String string) {
		if (string != null && string.trim().length() > 0)
			return false;
		return true;
	}

}


