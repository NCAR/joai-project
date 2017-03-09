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
package org.dlese.dpc.schemedit.standards.asn;

import org.dlese.dpc.serviceclients.cat.CATRequestConstraints;
import org.dlese.dpc.schemedit.standards.AbstractCATHelperPlugin;
import org.dlese.dpc.schemedit.standards.CATServiceHelper;
import org.dlese.dpc.schemedit.SchemEditUtils;
import org.dlese.dpc.util.strings.FindAndReplace;
import java.util.*;
import org.dom4j.Element;

/**
 *  Abstract class for CATHelperPlugins whose frameworks store ASN identifiers.
 *  <p>
 *
 *  These plugins make use of a {@link GradeRangeHelper} to map GradeRange
 *  vocabs defined in the schema to corresponding CAT API constraints.
 *
 * @author    ostwald
 */
public abstract class AsnCATPlugin extends AbstractCATHelperPlugin {

	private static boolean debug = false;
	private GradeRangeHelper gradeRangeHelper = null;


	/**
	 *  Gets the catHelper attribute of the AsnCATPlugin object
	 *
	 * @return    The catHelper value
	 */
	protected CATServiceHelper getCatHelper() {
		return (AsnSuggestionServiceHelper) super.getCatHelper();
	}


	/**
	 *  Gets the gradeRangeHelper attribute of the AsnCATPlugin object
	 *
	 * @return    The gradeRangeHelper value
	 */
	protected GradeRangeHelper getGradeRangeHelper() {
		if (this.gradeRangeHelper == null) {
			this.gradeRangeHelper = gradeRangeHelperInit();
		}
		return this.gradeRangeHelper;
	}


	/**
	 *  Initialize a gradeRangeHelper instance for the gradeRange Vocab values of a
	 *  specific framework.
	 *
	 * @return    a gradeRange helper for the gradeRange vocabs
	 * @see       GradeRangeHelper
	 */
	protected abstract GradeRangeHelper gradeRangeHelperInit();


	/**
	 *  Specifies the fields besides url, description and gradeRanges that a
	 *  particular framework wants to control via the CAT UI.<p>
	 *
	 *  For example, some frameworks want to provide controls for the "subject"
	 *  field. In this case the list returned would contain "subjects"
	 *
	 * @return    The optionalCatUIFields value
	 */
	public abstract List getOptionalCatUIFields();


	/**
	 *  Convert the provided CAT API gradeLevel to the corresponding option value
	 *
	 * @param  i  a string representation of the provided int that can be used as a
	 *      value in the CAT UI
	 * @return    The gradeLevelOptionValue, or -1 if the provided value could not
	 *      be processed as a CAT UI value.
	 */
	protected String getGradeLevelOptionValue(int i) {
		if (i < 13 && i > -1)
			return String.valueOf(i);
		return "-1";
	}


	/**
	 *  Gets the gradeRangeOptionValue corresponding to the lowest selected
	 *  gradeRange in the current instance document.<p>
	 *
	 *  NOTE: We have to convert from the lowest gradeRangeVocab to it's
	 *  gradeRangeOption value
	 *
	 * @return    The startGradeOptionValue value
	 */
	public String getDerivedCATStartGrade() {
		int startGrade = this.getGradeRangeHelper().getStartGrade(this.getRecordGradeRanges());
		return this.getGradeLevelOptionValue(startGrade);
	}


	/**
	 *  Gets the gradeRangeOptionValue corresponding to the highest selected
	 *  gradeRange in the current instance document.<p>
	 *
	 *  NOTE: We return an gradeRangeOptionValue, so we have to convert from the
	 *  lowest gradeRangeVocab to it's gradeRangeOption value
	 *
	 * @return    The endGrade value
	 */
	public String getDerivedCATEndGrade() {
		int endGrade = this.getGradeRangeHelper().getEndGrade(this.getRecordGradeRanges());
		return this.getGradeLevelOptionValue(endGrade);
	}

	// ----------- PATHS --------------------

	/**
	 *  Gets the gradeRangePath attribute of the SuggestionServiceHelper object
	 *
	 * @return    The gradeRangePath value
	 */
	public abstract String getGradeRangePath();


	/**
	 *  Gets the keywordPath for the plugin's framework
	 *
	 * @return    The keywordPath value
	 */
	public abstract String getKeywordPath();


	/**
	 *  Gets the descriptionPath for the plugin's framework
	 *
	 * @return    The descriptionPath value
	 */
	public abstract String getDescriptionPath();


	/**
	 *  Gets the subjectPath for the plugin's framework
	 *
	 * @return    The subjectPath value
	 */
	public abstract String getSubjectPath();

	//--------------GRADE STUFF -------------

	/**
	 *  Returns the greater of the selected gradeLevel constraints selected in the
	 *  CAT UI (as an integer).<p>
	 *
	 *  NOTE: Assumes CAT UI specifies two constraints, and that both can be parsed
	 *  as integers.
	 *
	 * @param  gradeConstraints  NOT YET DOCUMENTED
	 * @return                   The startGrade value
	 */
	public int getSelectedCATStartGrade(String[] gradeConstraints) {
		if (gradeConstraints == null || !this.getCatHelper().getUseGradeRanges()) {
			return CATRequestConstraints.ANY_GRADE;
		}

		int min = CATRequestConstraints.ANY_GRADE;
		try {
			min = Math.min(Integer.parseInt(gradeConstraints[0]), Integer.parseInt(gradeConstraints[1]));
		} catch (Throwable t) {
			prtln("WARNING: getSelectedCATStartGrade problem: " + t.getMessage());
		}
		return min;
	}


	/**
	 *  Returns the lesser of the selected gradeLevel constraints selected in the
	 *  CAT UI (as an integer).<p>
	 *
	 *  NOTE: Assumes CAT UI specifies two constraints, and that both can be parsed
	 *  as integers.
	 *
	 * @param  gradeConstraints  NOT YET DOCUMENTED
	 * @return                   The endGrade value
	 */
	public int getSelectedCATEndGrade(String[] gradeConstraints) {
		if (gradeConstraints == null || !this.getCatHelper().getUseGradeRanges()) {
			return CATRequestConstraints.ANY_GRADE;
		}

		int max = CATRequestConstraints.ANY_GRADE;
		try {
			max = Math.max(Integer.parseInt(gradeConstraints[0]), Integer.parseInt(gradeConstraints[1]));
		} catch (Throwable t) {
			prtln("WARNING: getSelectedCATEndGrade problem: " + t.getMessage());
		}
		return max;
	}


	/**
	 *  Gets the gradeRanges corresponding to the grades searchable in the CAT
	 *  service (and which can be specified in the control box).<p>
	 *
	 *  NOTE: the "values" should correspond to what the service expects, while the
	 *  "labels" are human-meaningful representations of the values. E.g., one
	 *  member of the gradeRangeOption list might be (value: "1", label: "1st
	 *  Grade")
	 *
	 * @return    The gradeRanges value
	 */
	public abstract List getGradeRangeOptions();


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  s  NOT YET DOCUMENTED
	 */
	private static void prtln(String s) {
		if (debug)
			SchemEditUtils.prtln(s, "AsnCATPlugin");
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  s  NOT YET DOCUMENTED
	 */
	public static void prtlnErr(String s) {
		SchemEditUtils.prtln(s, "AsnCATPlugin");
	}

}

