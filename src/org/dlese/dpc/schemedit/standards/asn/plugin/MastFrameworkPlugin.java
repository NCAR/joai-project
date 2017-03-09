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
import org.dlese.dpc.schemedit.standards.asn.AsnCATPlugin;
import org.dlese.dpc.schemedit.standards.asn.GradeRangeHelper;

import org.dlese.dpc.schemedit.SchemEditUtils;

import java.util.*;
import org.apache.struts.util.LabelValueBean;

import org.dom4j.Element;

/**
 *  CATService FrameworkPlugin providing information specific to the mast
 *  framework.
 *
 * @author    ostwald
 */
public class MastFrameworkPlugin extends AsnCATPlugin {

	AsnSuggestionServiceHelper helper = null;


	/**
	 *  NOTE: values below come from vocabLayoutFile (http://meta.usu.edu/frameworks/mast/1.0/groups/gemEdLevel-mast-groups-cataloger-en-us.xml)
	 *  - There are no values defined in schema!
	 *
	 * @return    NOT YET DOCUMENTED
	 */
	protected GradeRangeHelper gradeRangeHelperInit() {
		GradeRangeHelper grh = new GradeRangeHelper();
		grh.addItem("Kindergarten", 0);
		grh.addItem("1", 1);
		grh.addItem("2", 2);
		grh.addItem("3", 3);
		grh.addItem("4", 4);
		grh.addItem("5", 5);
		grh.addItem("6", 6);
		grh.addItem("7", 7);
		grh.addItem("8", 8);
		grh.addItem("9", 9);
		grh.addItem("10", 10);
		grh.addItem("11", 11);
		grh.addItem("12", 12);
		grh.addItem("All", 0, 0);
		return grh;
	}


	/**
	 *  Gets the optionalCatUIFields attribute of the MastFrameworkPlugin object
	 *
	 * @return    The optionalCatUIFields value
	 */
	public List getOptionalCatUIFields() {
		return new ArrayList();
	}


	/**
	 *  Gets the gradeRangePath of the mast framework
	 *
	 * @return    The gradeRangePath value
	 */
	public String getGradeRangePath() {
		return "/record/educational/audiences/gemEdLevel";
	}


	/**
	 *  Gets the keywordPath of the mast framework
	 *
	 * @return    The keywordPath value
	 */
	public String getKeywordPath() {
		return "/itemRecord/general/keywords/keyword";
	}


	/**
	 *  Gets the descriptionPath of the mast framework
	 *
	 * @return    The descriptionPath value
	 */
	public String getDescriptionPath() {
		return "/record/general/description";
	}


	/**
	 *  Gets the subjectPath of the mast framework
	 *
	 * @return    The subjectPath value
	 */
	public String getSubjectPath() {
		return "/record/general/subjects/gemSubject";
	}


	/**
	 *  Gets the GradeRanges corresponding to the grades searchable in the
	 *  Suggestion service (and which can be specified in the control box).<p>
	 *
	 *  NOTE: the "values" should correspond to what the service expects, while the
	 *  "labels" should correspond to the selectable values from the
	 *  metaDataFramework.
	 *
	 * @return    The gradeRanges value
	 */
	public List getGradeRangeOptions() {
		List options = new ArrayList();
		try {
			options.add(new LabelValueBean("Kindergarten", "0"));
			for (int i = 1; i <= 12; i++) {
				String num = String.valueOf(i);
				options.add(new LabelValueBean(num, num));
			}
		} catch (Throwable t) {
			prtlnErr("getGradeRangeOptions ERROR: " + t.getMessage());
			// t.printStackTrace();
		}
		return options;
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  s  NOT YET DOCUMENTED
	 */
	private static void prtln(String s) {
		SchemEditUtils.prtln(s, "MastPlugin");
	}

}

