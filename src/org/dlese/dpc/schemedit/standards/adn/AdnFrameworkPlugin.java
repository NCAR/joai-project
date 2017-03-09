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
package org.dlese.dpc.schemedit.standards.adn;

import org.dlese.dpc.schemedit.standards.AbstractCATHelperPlugin;
import org.dlese.dpc.schemedit.standards.CATServiceHelper;
import org.dlese.dpc.serviceclients.cat.CATRequestConstraints;
import org.apache.struts.util.LabelValueBean;
import java.util.*;

/**
 *  CATHelperPlugin for the ADN framework
 *
 * @author    ostwald
 */
public class AdnFrameworkPlugin extends AbstractCATHelperPlugin {

	/**
	 *  Gets the catHelper attribute of the AdnFrameworkPlugin object
	 *
	 * @return    The catHelper value
	 */
	protected DleseSuggestionServiceHelper getCatHelper() {
		return (DleseSuggestionServiceHelper) super.getCatHelper();
	}


	/**
	 *  Gets the optionalCatUIFields attribute of the AdnFrameworkPlugin object
	 *
	 * @return    The optionalCatUIFields value
	 */
	public List getOptionalCatUIFields() {
		// return Arrays.asList ("subjects", "keywords");
		return Arrays.asList("keywords");
	}


	/**
	 *  Gets the gradeRangePath attribute of the DleseSuggestionServiceHelper
	 *  object
	 *
	 * @return    The gradeRangePath value
	 */
	public String getGradeRangePath() {
		return "/itemRecord/educational/audiences/audience/gradeRange";
	}


	/**
	 *  Gets the descriptionPath attribute of the AdnFrameworkPlugin object
	 *
	 * @return    The descriptionPath value
	 */
	public String getDescriptionPath() {
		return "/itemRecord/general/description";
	}


	/**
	 *  Gets the keywordPath attribute of the DleseSuggestionServiceHelper object
	 *
	 * @return    The keywordPath value
	 */
	public String getKeywordPath() {
		return "/itemRecord/general/keywords/keyword";
	}


	/**
	 *  Gets the recordGradeRange vocab values from the instance document (i.e.,
	 *  the selected gradeRange vocab values)
	 *
	 * @return    The recordGradeRanges value
	 */
	public String[] getRecordGradeRanges() {
		String path = this.getGradeRangePath();
		if (path != null && path.trim().length() > 0) {
			return this.getCatHelper().getActionForm().getEnumerationValuesOf(path);
		}
		else {
			return null;
		}
	}


	/**
	 *  Gets the subjectPath attribute of the AdnFrameworkPlugin object
	 *
	 * @return    The subjectPath value
	 */
	public String getSubjectPath() {
		return "/itemRecord/general/subjects/subject";
	}


	/**
	 *  Compute a startGrade value based on currently selected CAT grade Level
	 *  controls
	 *
	 * @param  gradeConstraints  NOT YET DOCUMENTED
	 * @return                   The endGrade value
	 */

	/**
	 *  Compute a CAT API value for the startGrade (lowest grade level) of the
	 *  selected grade ranges.<p>
	 *
	 *  The gradeConstraints for the adn framework are strings, such as
	 *  "DLESE:Primary elementary", which represent a gradeRange (e.g., 0-2). This
	 *  method determines the lowest bound of the supplied gradeConstraints.
	 *
	 * @param  gradeConstraints  NOT YET DOCUMENTED
	 * @return                   The startGrade value
	 */
	public int getSelectedCATStartGrade(String[] gradeConstraints) {
		int min = Integer.MAX_VALUE;
		if (gradeConstraints == null || !this.getCatHelper().getUseGradeRanges()) {
			prtln("getEndGrade: selectedGradeRanges is null");
			return CATRequestConstraints.ANY_GRADE;
		}
		for (int i = 0; i < gradeConstraints.length; i++) {
			min = Math.min(min, this.getCatHelper().getStandardsMapper().getStartGrade(gradeConstraints[i]));
		}
		if (min == Integer.MAX_VALUE)
			min = CATRequestConstraints.ANY_GRADE;
		return min;
	}


	/**
	 *  Compute a CAT API value for the endGrade (highest grade level) of the
	 *  selected grade ranges.<p>
	 *
	 *  The gradeConstraints for the adn framework are strings, such as
	 *  "DLESE:Primary elementary", which represent a gradeRange (e.g., 0-2). This
	 *  method determines the highest bound of the supplied gradeConstraints.
	 *
	 * @param  gradeConstraints  NOT YET DOCUMENTED
	 * @return                   The endGrade value
	 */
	public int getSelectedCATEndGrade(String[] gradeConstraints) {
		int max = -1;
		if (gradeConstraints == null || !this.getCatHelper().getUseGradeRanges()) {
			prtln("getEndGrade: gradeConstraints is null");
			return CATRequestConstraints.ANY_GRADE;
		}
		for (int i = 0; i < gradeConstraints.length; i++) {
			max = Math.max(max, this.getCatHelper().getStandardsMapper().getEndGrade(gradeConstraints[i]));
		}
		return max;
	}


	/**
	 *  Gets the gradeRanges options corresponding to the grades searchable in the
	 *  ADN framework service.<p>
	 *
	 *  The returned items contain two fields
	 *  <ul>
	 *    <li> value - corresponding to a grade-level vocab defined for the ADN
	 *    framework (e.g., "DLESE:Primary elementary")
	 *    <li> label - that indicates grade-levels in a human-readable form (e.g.,
	 *    "Primary elementary (0-2)")
	 *  </ul>
	 *
	 *
	 * @return    A list of LabelValueBeans used to populate the GradeRange widget
	 *      in the CAT UI
	 */
	public List getGradeRangeOptions() {
		List options = new ArrayList();
		AsnToAdnMapper standardsMapper = this.getCatHelper().getStandardsMapper();
		try {
			Collection gradeRangeValues = standardsMapper.getGradeRanges();
			for (Iterator i = gradeRangeValues.iterator(); i.hasNext(); ) {
				String value = (String) i.next();
				String startGrade = Integer.toString(standardsMapper.getStartGrade(value));
				String endGrade = Integer.toString(standardsMapper.getEndGrade(value));
				String label = standardsMapper.getGradeRangeLabel(value) + " (" + startGrade + "-" + endGrade + ")";
				options.add(new LabelValueBean(label, value));
			}
		} catch (Throwable t) {
			prtlnErr("getGradeRangeOptions: " + t.getMessage());
			// t.printStackTrace();
		}
		return options;
	}


	/**
	 *  Gets the gradeRangeOptionValue corresponding to the lowest selected
	 *  gradeRange vocab in the current instance document.<p>
	 *
	 *  NOTE: in the case of ADN, gradeRange vocab values are same as
	 *  gradeRangeOption values, so there is no need to convert, as there is for
	 *  frameworks in which this isn't the case!
	 *
	 * @return    The startGradeOptionValue value
	 */
	public String getDerivedCATStartGrade() {
		AsnToAdnMapper standardsMapper = this.getCatHelper().getStandardsMapper();
		int startGrade = Integer.MAX_VALUE;
		String startOptionValue = "";
		String[] selected = this.getRecordGradeRanges();

		// walk down the selected gradeRangeValues, find the option value corresponding
		// to the selected value with the lowest startGrade
		for (int i = 0; i < selected.length; i++) {
			String myVocab = selected[i];
			int myStartGrade = standardsMapper.getStartGrade(myVocab);
			if (myStartGrade < startGrade) {
				startGrade = myStartGrade;
				startOptionValue = myVocab;
			}
		}
		return startOptionValue;
	}


	/**
	 *  Gets the gradeRangeOptionValue corresponding to the highest selected
	 *  gradeRange in the current instance document.
	 *
	 * @return    The endGrade value
	 */
	public String getDerivedCATEndGrade() {
		AsnToAdnMapper standardsMapper = this.getCatHelper().getStandardsMapper();
		int endGrade = -1;
		String endOptionValue = "";
		String[] selected = this.getRecordGradeRanges();

		// walk down the selected gradeRangeValues, find the option value corresponding
		// to the selected value with the lowest endGrade
		for (int i = 0; i < selected.length; i++) {
			String myVocab = selected[i];
			int myEndGrade = standardsMapper.getEndGrade(myVocab);
			if (myEndGrade > endGrade) {
				endGrade = myEndGrade;
				endOptionValue = myVocab;
			}
		}
		return endOptionValue;
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  s  NOT YET DOCUMENTED
	 */
	private static void prtln(String s) {
		System.out.println(s);
	}

}

