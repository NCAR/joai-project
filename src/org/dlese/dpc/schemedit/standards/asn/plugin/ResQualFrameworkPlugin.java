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
import org.dlese.dpc.schemedit.SchemEditUtils;

import org.dom4j.*;
import java.util.*;
import org.apache.struts.util.LabelValueBean;
import java.net.*;

/**
 *  CATService FrameworkPlugin providing information specific to the res_qual
 *  framework.
 *
 * @author    ostwald
 */
public class ResQualFrameworkPlugin extends NcsItemFrameworkPlugin {
	private static boolean debug = false;


	/**
	 *  Gets the optionalCatUIFields attribute of the ResQualFrameworkPlugin object
	 *
	 * @return    The optionalCatUIFields value
	 */
	public List getOptionalCatUIFields() {
		return Arrays.asList("keywords");
	}

	/**
	 *  Gets the gradeRangePath attribute of the NcsItemSuggestionServiceHelper
	 *  object
	 *
	 * @return    The gradeRangePath value
	 */
	public String getGradeRangePath() {
		return "/record/educational/educationLevel";
	}


	/**
	 *  Gets the keywordPath attribute of the NcsItemSuggestionServiceHelper object
	 *
	 * @return    The keywordPath value
	 */
	public String getKeywordPath() {
		return "/record/general/keyword";
	}


	/**
	 *  Gets the descriptionPath attribute of the ResQualFrameworkPlugin object
	 *
	 * @return    The descriptionPath value
	 */
	public String getDescriptionPath() {
		return "/record/general/description";
	}


	/**
	 *  Gets the subjectPath attribute of the ResQualFrameworkPlugin object
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
		if (debug) {
			SchemEditUtils.prtln(s, "ResQualPlugin");
		}
	}
}

