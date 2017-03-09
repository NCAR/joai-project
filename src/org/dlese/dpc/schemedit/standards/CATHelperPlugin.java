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

import java.util.List;

/**
 *  Interface for framework-specific plugins to the CATServiceHelper
 *
 * @author    ostwald
 */
public interface CATHelperPlugin {

	/**
	 *  Initialize the plugin with a CATServiceHelper instance
	 *
	 * @param  helper  NOT YET DOCUMENTED
	 */
	public void init(CATServiceHelper helper);


	/**
	 *  Gets a list specifying what optional fields (e.g., "subject", "keyword") is
	 *  exported by this framework. The UI controls for these fields will be
	 *  active.
	 *
	 * @return    The optionalCatUIFields value
	 */
	public List getOptionalCatUIFields();


	/* -------- INSTANCE DOC values for use in UI ------------
		these methods get the values to be used in the CAT UI. They don't necessarily
		have to correspond to single fields in the instance doc. For example, the
		getRecordKeywords() method for the msp2 framework actually grabs the subject
		values as well as the keyword values from the instance doc
	*/
	/**
	 *  Gets the recordDescription for the current record
	 *
	 * @return    The recordDescription value
	 */
	public String getRecordDescription();


	/**
	 *  Gets the recordKeywords for the current record
	 *
	 * @return    The recordKeywords value
	 */
	public String[] getRecordKeywords();


	/**
	 *  Gets the recordGradeRanges for the current record
	 *
	 * @return    The recordGradeRanges value
	 */
	public String[] getRecordGradeRanges();


	/**
	 *  Gets the gradeRangeOptionValue corresponding to the lowest selected
	 *  gradeRange in the current instance document.<p>
	 *
	 *  NOTE: this requires converting from possible gradeRange metadata values to
	 *  the values supplied for gradeRangeOptions.
	 *
	 * @return    The startGrade value
	 */
	public String getDerivedCATStartGrade();


	/**
	 *  Gets the gradeRangeOptionValue corresponding to the highest selected
	 *  gradeRange in the current instance document.<p>
	 *
	 *  NOTE: this requires converting from possible gradeRange metadata values to
	 *  the values supplied for gradeRangeOptions.
	 *
	 * @return    The endGrade value
	 */
	public String getDerivedCATEndGrade();


	/**
	 *  Gets the recordSubjects for the current record
	 *
	 * @return    The recordSubjects value
	 */
	public String[] getRecordSubjects();

	//--------------GRADE STUFF for CAT UI-------------


	/**
	 *  Gets the gradeRangeOptions for this framework
	 *
	 * @return    The gradeRangeOptions value
	 */
	public List getGradeRangeOptions();
	
	public int getSelectedCATEndGrade(String [] gradeConstraints);
	
	public int getSelectedCATStartGrade(String [] gradeConstraints);

}

