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
package org.dlese.dpc.suggest.action.form;

import org.dlese.dpc.suggest.resource.urlcheck.ValidatorResults;
import org.dlese.dpc.schemedit.url.DupSim;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionError;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionServlet;
import org.apache.struts.util.MessageResources;
import org.apache.struts.util.LabelValueBean;

import javax.servlet.http.HttpServletRequest;
import java.util.*;



/**
 *  ActionForm bean for handling requests that access a {@link org.dlese.dpc.suggest.SuggestionRecord}
 *  the QA DataBase and Records.
 *
 * @author     ostwald
 * @version    $Id: SuggestResourceForm.java,v 1.3 2009/03/20 23:34:00 jweather Exp $
 */
public class SuggestResourceForm extends SuggestForm {

	private static boolean debug = true;

	/**  NOT YET DOCUMENTED */
	protected ValidatorResults validatorResults = null;
	
	private String url = null;
	private String title = null;
	private String description = null;
	private String[] gradeRanges = new String[0];
	private String nameFirst = null;
	private String nameLast = null;
	private String emailPrimary = null;
	private String instName = null;
	private String creationDate = null;
	private String[] gradeRangeChoices = new String[0];
	private String[] gradeRangeLeafChoices = new String[0];
	private LabelValueBean[] gradeRangeOptions = new LabelValueBean[]{};
	private boolean preserveGradeRanges = false;


	/**  Constructor  */
	public SuggestResourceForm() {
		prtln("SuggestResourceForm()");
	}


	/**
	 *  We need this method to allow gradeRange to be zero'd out as a multibox. But
	 *  we also need the preserveGradeRanges to protect from a reset to support the
	 *  edit/display/re-edit cycle. preserveGradeRanges is set in SuggestResourceAction.
	 *
	 * @param  mapping  Description of the Parameter
	 * @param  request  Description of the Parameter
	 */
	public void reset(ActionMapping mapping, HttpServletRequest request) {
		//
		super.reset(mapping, request);
		if (!preserveGradeRanges) {
			gradeRanges = new String[]{};
		}
		preserveGradeRanges = false;
	}


	/**  resets the bean's key attributes  */
	public void clear() {
		super.clear();
		
		validatorResults = null;
		
		url = null;
		title = null;
		description = null;
		gradeRanges = new String[]{};
		nameFirst = null;
		nameLast = null;
		emailPrimary = null;
		instName = null;
		creationDate = null;
		preserveGradeRanges = false;
		validatorResults = null;
		prtln("form cleared");
	}

	/**
	 *  Gets the validatorResults attribute of the SuggestForm object
	 *
	 * @return    The validatorResults value
	 */
	public ValidatorResults getValidatorResults() {
		return validatorResults;
	}


	/**
	 *  Sets the validatorResults attribute of the SuggestForm object
	 *
	 * @param  results  The new validatorResults value
	 */
	public void setValidatorResults(ValidatorResults results) {
		validatorResults = results;
	}
	
	/**
	 *  Gets the dupRecord attribute of the SuggestResourceForm object
	 *
	 * @return    The dupRecord value
	 */
	public DupSim getDupRecord() {
		if (validatorResults == null)
			return null;
		return validatorResults.getDuplicate();
	}


	/**
	 *  Gets the dupRecordId attribute of the SuggestResourceForm object
	 *
	 * @return    The dupRecordId value
	 */
	public String getDupRecordId() {
		try {
			return getDupRecord().getId();
		} catch (Throwable t) {
			// prtln ("failed to get dupRecordId");
			return null;
		}
	}


	/**
	 *  Gets the title attribute of the SuggestResourceForm object
	 *
	 * @return    The title value
	 */
	public String getTitle() {
		if (title == null) {
			return "";
		}
		else {
			return title;
		}
	}


	/**
	 *  Sets the title attribute of the SuggestResourceForm object
	 *
	 * @param  val  The new title value
	 */
	public void setTitle(String val) {
		this.title = val;
	}


	/**
	 *  Gets the url attribute of the SuggestResourceForm object
	 *
	 * @return    The url value
	 */
	public String getUrl() {
		return url;
	}


	/**
	 *  Sets the url attribute of the SuggestResourceForm object
	 *
	 * @param  val  The new url value
	 */
	public void setUrl(String val) {
		this.url = val;
	}


	/**
	 *  Gets the description attribute of the SuggestResourceForm object
	 *
	 * @return    The description value
	 */
	public String getDescription() {
		if (description == null) {
			return "";
		}
		else {
			return description;
		}
	}


	/**
	 *  Sets the description attribute of the SuggestResourceForm object
	 *
	 * @param  val  The new description value
	 */
	public void setDescription(String val) {
		this.description = val;
	}


	/**
	 *  Sets the preserveGradeRanges attribute of the SuggestResourceForm object
	 *
	 * @param  val  The new preserveGradeRanges value
	 */
	public void setPreserveGradeRanges(boolean val) {
		this.preserveGradeRanges = val;
	}


	/**
	 *  Gets the preserveGradeRanges attribute of the SuggestResourceForm object
	 *
	 * @return    The preserveGradeRanges value
	 */
	public boolean getPreserveGradeRanges() {
		return preserveGradeRanges;
	}


	/**
	 *  Gets a displayable version of the selected gradeRanges
	 *
	 * @return                The gradeRangesDisplay value
	 * @exception  Exception  Description of the Exception
	 */
	public String getGradeRangesDisplay()
		 throws Exception {
		String[] items = getGradeRangesKeys();
		String s = "";
		if ((items == null) || (items.length == 0)) {
			return "";
		}
		for (int i = 0; i < items.length; i++) {
			String val = items[i];
			String prefix = "DLESE:";
			if (val.startsWith(prefix)) {
				val = val.substring(prefix.length());
			}
			s += val;
			if (i < (items.length - 1)) {
				s += ", ";
			}
		}
		return s;
	}


	/**
	 *  Gets the gradeRanges attribute of the SuggestResourceForm object
	 *
	 * @return    The gradeRanges value
	 */
	public String[] getGradeRanges() {
		if ((gradeRanges == null) || (gradeRanges.length == 0)) {
			//
			return new String[]{};
		}
		else {
			return gradeRanges;
		}
	}


	/**
	 *  Gets the gradeRangesKeys attribute of the SuggestResourceForm object
	 *
	 * @return    The gradeRangesKeys value
	 */
	public String[] getGradeRangesKeys() {
		List gr = Arrays.asList(getGradeRanges());
		ArrayList keys = new ArrayList();
		for (Iterator i = gr.iterator(); i.hasNext(); ) {
			String value = (String) i.next();
			String key = "";
			try {
				// key = vocab.getFieldValueSystemId("gradeRange", value);
				key = vocab.getTranslatedValue("adn", "gradeRange", value);
			} catch (Exception e) {
				prtln("getGradeRangesKeys: " + e.getMessage());
			}
			keys.add(key);
		}
		return (String[]) keys.toArray(new String[]{});
	}


	/**
	 *  Sets the gradeRanges attribute of the SuggestResourceForm object
	 *
	 * @param  vals  The new gradeRanges value
	 */
	public void setGradeRanges(String[] vals) {
		this.gradeRanges = vals;
	}


	/**
	 *  Gets the nameFirst attribute of the SuggestResourceForm object
	 *
	 * @return    The nameFirst value
	 */
	public String getNameFirst() {
		if (nameFirst == null) {
			return "";
		}
		else {
			return nameFirst;
		}
	}


	/**
	 *  Sets the nameFirst attribute of the SuggestResourceForm object
	 *
	 * @param  val  The new nameFirst value
	 */
	public void setNameFirst(String val) {
		this.nameFirst = val;
	}


	/**
	 *  Gets the nameLast attribute of the SuggestResourceForm object
	 *
	 * @return    The nameLast value
	 */
	public String getNameLast() {
		if (nameLast == null) {
			return "";
		}
		else {
			return nameLast;
		}
	}


	/**
	 *  Sets the nameLast attribute of the SuggestResourceForm object
	 *
	 * @param  val  The new nameLast value
	 */
	public void setNameLast(String val) {
		this.nameLast = val;
	}


	/**
	 *  Gets the emailPrimary attribute of the SuggestResourceForm object
	 *
	 * @return    The emailPrimary value
	 */
	public String getEmailPrimary() {
		if (emailPrimary == null) {
			return "";
		}
		else {
			return emailPrimary;
		}
	}


	/**
	 *  Sets the emailPrimary attribute of the SuggestResourceForm object
	 *
	 * @param  val  The new emailPrimary value
	 */
	public void setEmailPrimary(String val) {
		this.emailPrimary = val;
	}


	/**
	 *  Gets the instName attribute of the SuggestResourceForm object
	 *
	 * @return    The instName value
	 */
	public String getInstName() {
		if (instName == null) {
			return "";
		}
		else {
			return instName;
		}
	}


	/**
	 *  Sets the instName attribute of the SuggestResourceForm object
	 *
	 * @param  val  The new instName value
	 */
	public void setInstName(String val) {
		this.instName = val;
	}


	/**
	 *  Create a LabelValueBean array holding the gradeRangeChoices and Labels
	 *
	 * @return    The gradeRangeOptions value
	 */
	public LabelValueBean[] getGradeRangeOptions() {
		// List values = schemaHelper.getEnumerationValues("DLESEgradeRangeType", false);
		List values = Arrays.asList(getGradeRangeChoices());
		String[] labelsArray = getGradeRangeChoicesSystemKeys();
		List labels = Arrays.asList(labelsArray);

		if ((labels == null) || (labels.size() != values.size())) {
			prtln("didn't create labels, assigning labels to values instead");
			labels = values;
		}
		LabelValueBean[] options = new LabelValueBean[values.size()];
		for (int i = 0; i < values.size(); i++) {
			String value = (String) values.get(i);
			String label = (String) labels.get(i);
			options[i] = new LabelValueBean(label, value);
		}
		// prtln("getEnumerationOptions() returning " + options.length + " LabelValueBeans");
		return options;
	}


	/**
	 *  Grabs the collection keys from the DPC keys schema. Why couldn't we simply
	 *  specify the DataType spec and have the values by retrieved automatically?
	 *
	 * @return    A list of valid colleciton keys.
	 */
	public String[] getGradeRangeChoices() {
		if (gradeRangeChoices.length == 0) {
			String typeName = "DLESEgradeRangeType";
			List choiceList = schemaHelper.getEnumerationValues(typeName, false);
			gradeRangeChoices = (String[]) choiceList.toArray(new String[]{});
		}
		return gradeRangeChoices;
	}


	/**
	 *  Gets the gradeRangeChoicesSystemKeys attribute of the SuggestResourceForm
	 *  object
	 *
	 * @return    The gradeRangeChoicesSystemKeys value
	 */
	public String[] getGradeRangeChoicesSystemKeys() {
		List gr = Arrays.asList(getGradeRangeChoices());
		ArrayList keys = new ArrayList();
		for (Iterator i = gr.iterator(); i.hasNext(); ) {
			String value = (String) i.next();
			String key = "";
			try {
				key = vocab.getTranslatedValue("adn", "gradeRange", value);
			} catch (Exception e) {
				prtln("getGradeRangeChoicesSystemKeys: " + e.getMessage());
			}
			keys.add(key);
		}
		return (String[]) keys.toArray(new String[]{});
	}


	/**
	 *  Print selected fields of this object for debugging purposes
	 *
	 * @return    String
	 */
	public String toString() {
		String s = "SuggestResourceForm contents:";
		if (url != null) {
			s += "\n\turl: " + url;
		}
		if (emailPrimary != null) {
			s += "\n\temailPrimary: " + emailPrimary;
		}
		return s;
	}


	/**
	 *  Sets the debug attribute of the SuggestResourceForm object
	 *
	 * @param  db  The new debug value
	 */
	public static void setDebug(boolean db) {
		debug = db;
	}


	/**
	 *  Output a line of text to standard out, with datestamp, if debug is set to
	 *  true.
	 *
	 * @param  s  The String that will be output.
	 */
	protected final void prtln(String s) {
		if (debug) {
			System.out.println(s);
		}
	}

}

