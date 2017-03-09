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
package org.dlese.dpc.schemedit.standards.asn.plugin;

import org.dlese.dpc.schemedit.standards.asn.AsnSuggestionServiceHelper;
import org.dlese.dpc.util.strings.FindAndReplace;

import java.util.*;
import org.apache.struts.util.LabelValueBean;

import org.dom4j.Element;

/**
 *  CATService FrameworkPlugin providing information specific to the "msp2"
 *  framework.<p>
 *
 *  The msp2 framework has multiple subject fields and a repeating keywords
 *  field.
 *
 * @author    ostwald
 */
public class Msp2FrameworkPlugin extends NcsItemFrameworkPlugin {

	/**
	 *  Gets the optionalCatUIFields attribute of the Msp2FrameworkPlugin object
	 *
	 * @return    The optionalCatUIFields value
	 */
	public List getOptionalCatUIFields() {
		return Arrays.asList("subjects", "keywords");
	}


	/**
	 *  Gets the gradeRangePath of the msp2 framework
	 *
	 * @return    The gradeRangePath value
	 */
	public String getGradeRangePath() {
		return "/record/educational/educationLevel";
	}


	/**
	 *  Gets the collected recordSubjects defined under three xpaths.
	 *
	 * @return    The recordSubjects value
	 */
	public String[] getRecordSubjects() {
		List subjectPaths = Arrays.asList(
			"/record/general/subjects/scienceSubject",
			"/record/general/subjects/mathSubject",
			"/record/general/subjects/educationalSubject"
			);
		try {
			return getRecordSubjects(subjectPaths);
		} catch (Throwable t) {
			prtln("getRecordSubjects ERROR: " + t.getMessage());
			t.printStackTrace();
		}
		return null;
	}


	/**
	 *  Remove semicolons from subject values. <P>
	 *
	 *  NOTE: we may want to tak only the leaf, since the upper levels of the
	 *  hierarchical msp2 subject values can be noise that drowns out the signal
	 *  provided by the leaf.
	 *
	 * @param  value  NOT YET DOCUMENTED
	 * @return        NOT YET DOCUMENTED
	 */
	protected String normalizeSubjectValue(String value) {
		return FindAndReplace.replace(value, ":", " ", true);
	}


	/**
	 *  Gets the keywordPath of the msp2 framework
	 *
	 * @return    The keywordPath value
	 */
	public String getKeywordPath() {
		return "/record/general/keyword";
	}


	/**
	 *  Gets the descriptionPath  of the msp2 framework
	 *
	 * @return    The descriptionPath value
	 */
	public String getDescriptionPath() {
		return "/record/general/description";
	}


	/**
	 *  Gets the subjectPath  of the msp2 framework
	 *
	 * @return    The subjectPath value
	 */
	public String getSubjectPath() {
		return "/record/general/subjects/scienceSubject";
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

