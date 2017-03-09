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
import org.dlese.dpc.schemedit.standards.asn.NsdlGradeRangeHelper;

import org.dlese.dpc.schemedit.SchemEditUtils;

import org.dom4j.*;

import java.util.*;
import org.apache.struts.util.LabelValueBean;

import java.net.*;

/**
 *  CATService FrameworkPlugin providing information specific to the ncs_item
 *  framework.
 *
 * @author    ostwald
 */
public class NcsItemFrameworkPlugin extends AsnCATPlugin {
	private static boolean debug = true;


	/**
	 *  Initialize a gradeRangeHelper instance for the ncs_item gradeRange Vocab
	 *  values
	 *
	 * @return    a GradeRangeHelper instance
	 */
	protected GradeRangeHelper gradeRangeHelperInit() {
		GradeRangeHelper grh = new NsdlGradeRangeHelper();
		grh.addItem("Pre-Kindergarten", 0);
		grh.addItem("Elementary School", 0, 5);
		grh.addItem("Early Elementary", 0, 2);
		grh.addItem("Kindergarten", 0);
		grh.addItem("Grade 1", 1);
		grh.addItem("Grade 2", 2);
		grh.addItem("Upper Elementary", 3, 5);
		grh.addItem("Grade 3", 3);
		grh.addItem("Grade 4", 4);
		grh.addItem("Grade 5", 5);
		grh.addItem("Middle School", 6, 8);
		grh.addItem("Grade 6", 6);
		grh.addItem("Grade 7", 7);
		grh.addItem("Grade 8", 8);
		grh.addItem("High School", 9, 12);
		grh.addItem("Grade 9", 9);
		grh.addItem("Grade 10", 10);
		grh.addItem("Grade 11", 11);
		grh.addItem("Grade 12", 12);
/* 		grh.addItem("Elementary School Programming", 0, 5);
		grh.addItem("Middle School Programming", 6, 8);
		grh.addItem("High School Programming", 9, 12); */
		return grh;
	}


	/**
	 *  Gets the optionalCatUIFields attribute of the NcsItemFrameworkPlugin object
	 *
	 * @return    The optionalCatUIFields value
	 */
	public List getOptionalCatUIFields() {
		return Arrays.asList("subjects");
	}


	/**
	 *  Gets the gradeRangePath attribute of the NcsItemSuggestionServiceHelper
	 *  object
	 *
	 * @return    The gradeRangePath value
	 */
	public String getGradeRangePath() {
		return "/record/educational/educationLevels/nsdlEdLevel";
	}


	/**
	 *  Gets the keywordPath attribute of the NcsItemSuggestionServiceHelper object
	 *
	 * @return    The keywordPath value
	 */
	public String getKeywordPath() {
		return null;
	}


	/**
	 *  Gets the descriptionPath attribute of the NcsItemFrameworkPlugin object
	 *
	 * @return    The descriptionPath value
	 */
	public String getDescriptionPath() {
		return "/record/general/description";
	}


	/**
	 *  Gets the subjectPath attribute of the NcsItemFrameworkPlugin object
	 *
	 * @return    The subjectPath value
	 */
	public String getSubjectPath() {
		return "/record/general/subject";
	}


	/**
	 *  Gets the gradeRanges corresponding to the grades searchable in the
	 *  Suggestion service (and which can be specified in the control box).<p>
	 *
	 *  NOTE: the "values" should correspond to what the service expects, while the
	 *  "labels" are human-meaningful representations of the values. E.g., one
	 *  member of the gradeRangeOption list might be (value: "1", label: "1st
	 *  Grade")
	 *
	 * @return    The gradeRanges value
	 */
	public List getGradeRangeOptions() {
		List options = new ArrayList();
		// prtln("getGradeRangeOptions()");
		try {
			options.add(new LabelValueBean("Kindergarten", "0"));
			for (int i = 1; i <= 12; i++) {
				String num = String.valueOf(i);
				options.add(new LabelValueBean("Grade " + num, String.valueOf(num)));
			}
		} catch (Throwable t) {
			prtln("getGradeRangeOptions: " + t.getMessage());
			t.printStackTrace();
		}
		return options;
	}



	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  s  NOT YET DOCUMENTED
	 */
	private static void prtln(String s) {
		if (debug) {
			SchemEditUtils.prtln(s, "NcsItemPlugin");
		}
	}
}

