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
package org.dlese.dpc.schemedit.standards;

import org.dlese.dpc.schemedit.SchemEditUtils;
import org.dlese.dpc.util.strings.FindAndReplace;
import java.util.*;
import org.dom4j.Element;

/**
 *  Abstract class for framework-specific plugins to the CATServiceHelper
 *
 * @author    ostwald
 */
public abstract class AbstractCATHelperPlugin implements CATHelperPlugin {

	private CATServiceHelper helper;

	/**
	 *  Gets the catHelper attribute of the AbstractCATHelperPlugin object
	 *
	 * @return    The catHelper value
	 */
	protected CATServiceHelper getCatHelper() {
		return this.helper;
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  helper  NOT YET DOCUMENTED
	 */
	public void init(CATServiceHelper helper) {
		this.helper = helper;
	}


	/**
	 *  Gets the fields besides url, description and graderanges that a particular
	 *  framework wants to control via the CAT UI.
	 *
	 * @return    The optionalCatUIFields value
	 */
	public List getOptionalCatUIFields() {
		return new ArrayList();
	}



	/* -------- INSTANCE DOC values for use in UI ------------
		these methods get the values to be used in the CAT UI. They don't necessarily
		have to correspond to single fields in the instance doc. For example, the
		getRecordKeywords() method for the msp2 framework actually grabs the subject
		values as well as the keyword values from the instance doc
	*/
	/**
	 *  Gets the recordSubjects from the current instance doc using the supplied
	 *  subjectPaths.
	 *
	 * @param  subjectPaths  a list of xpaths containing subject values
	 * @return               The recordSubjects value
	 */
	public String[] getRecordSubjects(List subjectPaths) {

		List allSubjects = new ArrayList();

		if (subjectPaths != null) {

			for (Iterator sp = subjectPaths.iterator(); sp.hasNext(); ) {
				String path = (String) sp.next();
				try {
					String[] values = this.getCatHelper().getActionForm().getEnumerationValuesOf(path);
					for (int i = 0; i < values.length; i++) {
						allSubjects.add(this.normalizeSubjectValue(values[i]));
					}
				} catch (Throwable t) {
					prtlnErr("getRecordsSubjects ERROR: " + t.getMessage());
				}
			}
		}
		return (String[]) allSubjects.toArray(new String[]{});
	}


	/**
	 *  Hook to preprocess subject values before submitting them to the CAT API
	 *
	 * @param  value  NOT YET DOCUMENTED
	 * @return        NOT YET DOCUMENTED
	 */
	protected String normalizeSubjectValue(String value) {
		return value;
	}


	/**
	 *  Gets the recordSubjects from the current instance doc using the subjectPath
	 *  defined in the concrete plugin instance.
	 *
	 * @return    The recordSubjects value
	 */
	public String[] getRecordSubjects() {

		String path = this.getSubjectPath();

		try {
			if (path != null && path.trim().length() > 0) {
				String[] values = this.helper.getActionForm().getEnumerationValuesOf(path);
				for (int i = 0; i < values.length; i++)
					values[i] = normalizeSubjectValue(values[i]);
				return values;
			}
		} catch (Throwable t) {
			prtlnErr("getRecordsSubjects error: " + t.getMessage());
		}
		return null;
	}


	/**
	 *  Gets the value of the description field of the current metadata record,
	 *  concatenating multiple description field values in the case where the
	 *  description field is repeating.
	 *
	 * @return    The description
	 */
	public String getRecordDescription() {
		String description = "";
		List dElements = this.helper.getActionForm().getDocMap().selectNodes(getDescriptionPath());
		for (Iterator i = dElements.iterator(); i.hasNext(); ) {
			try {
				description = description + " " + ((Element) i.next()).getText();
			} catch (Throwable t) {
				prtlnErr("getRecordDescription error: " + t.getMessage());
			}
		}
		return description;
	}


	/**
	 *  Gets the keyword values currently defined in the record.
	 *
	 * @return    The recordKeywords value
	 */
	public String[] getRecordKeywords() {
		String path = this.getKeywordPath();
		if (path != null && path.trim().length() > 0) {
			return this.helper.getActionForm().getEnumerationValuesOf(path);
		}
		else {
			return null;
		}
	}


	/**
	 *  Returns the gradeRange vocab values currently selected in the record
	 *
	 * @return    The recordGradeRanges value
	 */
	public String[] getRecordGradeRanges() {
		String path = this.getGradeRangePath();
		if (path != null && path.trim().length() > 0) {
			return this.helper.getActionForm().getEnumerationValuesOf(path);
		}
		else {
			return null;
		}
	}


	/**
	 *  Gets the startGradeContraintOptionValue corresponsing to the lowest
	  gradeRange selected in the current record.<p>
	  
	 *
	 * @return    The startGradeOptionValue value
	 */
	public abstract String getDerivedCATStartGrade();


	/**
	 *  Gets the gradeRangeOptionValue corresponding to the highest selected
	 *  gradeRange in the current instance document.<p>
	 *
	 *  NOTE: this requires converting from possible gradeRange metadata values to
	 *  the values supplied for gradeRangeOptions.
	 *
	 * @return    The endGrade value
	 */
	public abstract String getDerivedCATEndGrade();


	// ----------- PATHS --------------------

	/**
	 *  Gets the gradeRangePath attribute of the SuggestionServiceHelper object
	 *
	 * @return    The gradeRangePath value
	 */
	public abstract String getGradeRangePath();


	/**
	 *  Gets the keywordPath attribute of the SuggestionServiceHelper object
	 *
	 * @return    The keywordPath value
	 */
	public abstract String getKeywordPath();


	/**
	 *  Gets the descriptionPath attribute of the AbstractCATHelperPlugin object
	 *
	 * @return    The descriptionPath value
	 */
	public abstract String getDescriptionPath();


	/**
	 *  Gets the subjectPath attribute of the AbstractCATHelperPlugin object
	 *
	 * @return    The subjectPath value
	 */
	public abstract String getSubjectPath();

	// public String getUrlPath();

	//--------------GRADE STUFF -------------


	/**
	 *  Compute a startGrade value based on currently selected CAT grade Level
	 *  controls
	 *
	 * @return    The startGrade value
	 */
	public abstract int getSelectedCATStartGrade(String [] gradeConstraints);


	/**
	 *  Compute a end Grade value based on currently selected CAT grade Level
	 *  controls
	 *
	 * @return    The endGrade value
	 */
	public abstract int getSelectedCATEndGrade(String [] gradeConstraints);


	/**
	 *  Gets the gradeRangeOptions attribute of the AbstractCATHelperPlugin object
	 *
	 * @return    The gradeRangeOptions value
	 */
	public abstract List getGradeRangeOptions();


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  s  NOT YET DOCUMENTED
	 */
	private static void prtln(String s) {
		SchemEditUtils.prtln(s, "");
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  s  NOT YET DOCUMENTED
	 */
	public static void prtlnErr(String s) {
		SchemEditUtils.prtln(s, "");
	}

}

