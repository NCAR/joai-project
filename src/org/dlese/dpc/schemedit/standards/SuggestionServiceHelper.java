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

import org.dlese.dpc.schemedit.MetaDataFramework;
import org.dlese.dpc.schemedit.action.form.SchemEditForm;
import org.dlese.dpc.xml.schema.*;
import org.dlese.dpc.schemedit.*;

import javax.servlet.http.HttpServletRequest;

import org.dom4j.*;

import java.io.*;
import java.util.*;
import org.apache.struts.util.LabelValueBean;

import java.net.*;

/**
 *  Run-time support for handling interaction with user in the context of
 *  content standard assignment aided by a suggestionService (e.g., CAT).<p>
 *
 *  Has access to the the current instanceDocument (in the metadata editor), and
 *  retrieves suggestions from the suggestion service.
 *
 * @author     ostwald
 * @see        StandardsManager
 * @created    December 10, 2008
 */
public interface SuggestionServiceHelper {

	/**
	 *  Gets the serviceIsActive attribute of the SuggestionServiceHelper object
	 *
	 * @return    true if the SuggestionService is available
	 */
	public boolean getServiceIsActive();


	/**
	 *  Gets the standardsManager attribute of the SuggestionServiceHelper object
	 *
	 * @return    The standardsManager value
	 */
	public StandardsManager getStandardsManager();


	/**
	 *  Gets the xpath of the metadata element containing the managedStandards
	 *
	 * @return    The xpath value
	 */
	public String getXpath();


	/**
	 *  Gets the xmlFormat attribute of the SuggestionServiceHelper object
	 *
	 * @return    The xmlFormat value
	 */
	public String getXmlFormat();



	/**
	 *  Gets the suggested Standards represented in ADN format.
	 *
	 * @return    The suggestedStandards value
	 */
	public List getSuggestedStandards();


	/**
	 *  Gets the numSelectedStandards attribute of the SuggestionServiceHelper
	 *  object
	 *
	 * @return    The numSelectedStandards value
	 */
	public int getNumSelectedStandards();


	/**
	 *  Gets the standards that are selected in the instanceDoc
	 *
	 * @return    The selectedStandards value
	 */
	public List getSelectedStandards();


	/**
	 *  Sets the suggestedStandards attribute of the SuggestionServiceHelper object
	 *
	 * @param  stds  The new suggestedStandards value
	 */
	public void setSuggestedStandards(List stds);


	/**
	 *  Determines whether standards are displayed as a heirarchical tree or flat
	 *  list.
	 *
	 * @return    e.g., LIST_MODE for list mode, TREE_MODE for tree mode.
	 */
	public String getDisplayMode();


	/**
	 *  Determines what standards to display (e.g., SUGGESTED_CONTENT,
	 *  STANDARDS_CONTENT, BOTH, ALL)
	 *
	 * @return    The displayContent value
	 */
	public String getDisplayContent();


	/**
	 *  Gets the currentDoc attribute of the SuggestionServiceHelper object
	 *
	 * @return    The currentDoc value
	 */
	public String getCurrentDoc();


	/**
	 *  Update suggestedStandards by performing a query on the SuggestionService
	 *  using current constraints.
	 *
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public void updateSuggestions() throws Exception;


	/**
	 *  Gets the url attribute of the SuggestionServiceHelper object
	 *
	 * @return    The url value
	 */
	public String getUrl();


	/**
	 *  Gets the url of the instance document
	 *
	 * @return    The recordUrl value
	 */
	public String getRecordUrl();


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
	 *  Hides and exposes nodes in the hierarchical standards display.
	 *
	 * @param  displayContent  Description of the Parameter
	 * @exception  Exception   NOT YET DOCUMENTED
	 */
	public void updateStandardsDisplay(String displayContent) throws Exception;
}

