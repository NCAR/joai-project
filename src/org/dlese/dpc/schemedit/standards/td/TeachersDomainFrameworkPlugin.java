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
package org.dlese.dpc.schemedit.standards.td;

import org.dlese.dpc.schemedit.standards.CATServiceHelper;
import org.dlese.dpc.schemedit.standards.asn.plugin.NcsItemFrameworkPlugin;
import org.dlese.dpc.schemedit.standards.asn.AsnSuggestionServiceHelper;
import org.dlese.dpc.util.strings.FindAndReplace;

import java.util.*;
import org.apache.struts.util.LabelValueBean;

import org.dom4j.Element;

/**
 *  CATService FrameworkPlugin providing information specific to the "comm_core"
 *  framework.<p>
 *
 *  The msp2 framework has multiple subject fields and a repeating keywords
 *  field.
 *
 * @author    ostwald
 */
public class TeachersDomainFrameworkPlugin extends NcsItemFrameworkPlugin {

	public void init(CATServiceHelper helper) {
		super.init (helper);
		// this.getCatHelper().setServiceIsActive(false);
	}
	
	/**
	 *  Gets the optionalCatUIFields attribute of the TeachersDomainFrameworkPlugin object
	 *
	 * @return    The optionalCatUIFields value
	 */
	public List getOptionalCatUIFields() {
		return new ArrayList();
	}


	/**
	 *  Gets the gradeRangePath of the msp2 framework
	 *
	 * @return    The gradeRangePath value
	 */
	public String getGradeRangePath() {
		return "";
	}


	/**
	 *  Gets the collected recordSubjects defined under three xpaths.
	 *
	 * @return    The recordSubjects value
	 */
	public String[] getRecordSubjects() {
		return null;
	}


	/**
	 *  Gets the keywordPath of the msp2 framework
	 *
	 * @return    The keywordPath value
	 */
	public String getKeywordPath() {
		return "";
	}


	/**
	 *  Gets the descriptionPath  of the msp2 framework
	 *
	 * @return    The descriptionPath value
	 */
	public String getDescriptionPath() {
		return "";
	}


	/**
	 *  Gets the subjectPath  of the msp2 framework
	 *
	 * @return    The subjectPath value
	 */
	public String getSubjectPath() {
		return "";
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

