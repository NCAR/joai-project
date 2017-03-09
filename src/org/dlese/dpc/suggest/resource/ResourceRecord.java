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
package org.dlese.dpc.suggest.resource;

import org.dlese.dpc.suggest.SuggestionRecord;
import org.dlese.dpc.xml.schema.*;
import org.dlese.dpc.xml.*;

import java.io.*;
import java.util.*;

import org.dom4j.Element;
import org.dom4j.Document;
import org.dom4j.DocumentException;

/**
 *  Wrapper for a suggested resource record, which is in ADN item-level format
 *  and represented as a {@link org.dom4j.Document}.
 *
 *@author    Jonathan Ostwald
 */
public class ResourceRecord extends SuggestionRecord {
	private static boolean debug = true;

	// xpaths to access ResourceRecord elements
/* 	private String contribXPath = null;
	private String urlXPath = null;
	private String titleXPath = null;
	private String descriptionXPath = null;
	private String gradeRangeXPath = null;
	private String nameFirstXPath = null;
	private String nameLastXPath = null;
	private String emailPrimaryXPath = null;
	private String instNameXPath = null;
	private String dateInfoXPath = null; */
		
	private String urlXPath = "/itemRecord/technical/online/primaryURL";
	private String titleXPath = "/itemRecord/general/title";
	private String descriptionXPath = "/itemRecord/general/description";
	private String gradeRangeXPath = "/itemRecord/educational/audiences/audience/gradeRange";
	private String dateInfoXPath = "/itemRecord/metaMetadata/dateInfo";
	private String contribXPath = "/itemRecord/metaMetadata/contributors/contributor";
	private String nameFirstXPath = contribXPath + "/person/nameFirst";
	private String nameLastXPath = contribXPath + "/person/nameLast";
	private String emailPrimaryXPath = contribXPath + "/person/emailPrimary";
	private String instNameXPath = contribXPath + "/person/instName";

	/**
	 *  Constructor - creates a new ResourceRecord but doesn't assign an id
	 *
	 *@param  doc  {@link org.dom4j.Document}
	 */
 	public ResourceRecord(Document doc, SchemaHelper schemaHelper) {
		super(doc, schemaHelper);
	}

	/*
	 *  ====XML Element Accessors ==============================================
	 */
	/**
	 *  Gets the title attribute of the ResourceRecord object
	 *
	 *@return                The title value
	 *@exception  Exception  Description of the Exception
	 */
	public String getTitle() {
		return (String)docMap.get(titleXPath);
	}


	/**
	 *  Sets the title attribute of the ResourceRecord object
	 *
	 *@param  val            The new title value
	 *@exception  Exception  Description of the Exception
	 */
	public void setTitle(String val)  throws Exception {
		if (val == null || val.trim().length() == 0)
			val = "puttitle here";
		put(titleXPath, val);
	}


	/**
	 *  Gets the url attribute of the ResourceRecord object
	 *
	 *@return                The url value
	 *@exception  Exception  Description of the Exception
	 */
	public String getUrl() {
		return (String)docMap.get(urlXPath);
	}

	/**
	 *  Sets the url attribute of the ResourceRecord object
	 *
	 *@param  val            The new url value
	 *@exception  Exception  Description of the Exception
	 */
	public void setUrl(String val)
 		throws Exception {
			put(urlXPath, val);
	}


	/**
	 *  Gets the description attribute of the ResourceRecord object
	 *
	 *@return                The description value
	 *@exception  Exception  Description of the Exception
	 */
	public String getDescription() {
		return get(descriptionXPath);
	}


	/**
	 *  Sets the description attribute of the ResourceRecord object
	 *
	 *@param  val            The new description value
	 *@exception  Exception  Description of the Exception
	 */
	public void setDescription(String val)
		throws Exception {
		if (val == null || val.trim().length() == 0)
			val = "putdescription here";
		put(descriptionXPath, val);
	}

	/**
	 *  we want to collect all the gradeRange Values and return them as an Array
	 *
	 *@return                The gradeRanges value
	 *@exception  Exception  Description of the Exception
	 */
	public String[] getGradeRanges()
		throws Exception {
		List list = docMap.selectNodes(gradeRangeXPath);

		// create a normalized list, gr
		ArrayList gr = new ArrayList();
		for (Iterator i = list.iterator(); i.hasNext(); ) {
			Element e = (Element) i.next();
			String val = e.getText();
			if (!val.trim().equals("")) {
				gr.add(val);
			}
		}
		// now return a string array
		return (String[]) gr.toArray(new String[]{});
	}


	/**
	 *  return a pretty-printed list of gradeRanges as a string
	 *
	 *@return                The gradeRangesDisplay value
	 *@exception  Exception  Description of the Exception
	 */
	public String getGradeRangesDisplay()
		throws Exception {
		String[] items = getGradeRanges();
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
	 *  GradeRange xpath is //educational/audiences/audience/gradeRange to set a
	 *  gradeRange we have to be concerned with the entire AUDIENCE element at this
	 *  point (Suggest-a-url), none of the other fields have meaningful values, so
	 *  we can simply wipe out the existing audiences and add new ones
	 *
	 *@param  vals           The new gradeRanges value
	 *@exception  Exception  Description of the Exception
	 */
	public void setGradeRanges(String[] vals)
		throws Exception {
		
		String audiencesXPath = "/itemRecord/educational/audiences";
		Element audiencesElement = (Element)docMap.selectSingleNode(audiencesXPath);
		if (audiencesElement == null)
			audiencesElement = (Element) docMap.createNewNode(audiencesXPath);
		
		audiencesElement.clearContent();

		if (vals.length == 0) {
			vals = new String[]{"DLESE:To be supplied"};
		}

		for (int i = 0; i < vals.length; i++) {
			String gradeRange = vals[i];
			if (!gradeRange.trim().equals("")) {
				Element audienceElement = audiencesElement.addElement("audience");
				Element gradeRangeElement = audienceElement.addElement("gradeRange");
				gradeRangeElement.addText(gradeRange);
			}
		}
	}


	// CONTRIBUTOR getters and setters
	/**
	 *  Sets the creation date for this ResourceRecord. MetaMetadata/dateInfo and
	 *  contributor elements should have same date value (since they are set
	 *  together - see <b>{@link ResourceRecord#setCreationDate}</b> we use
	 *  metaMetadata/dateInfo here
	 *
	 *@return                The creationDate value
	 *@exception  Exception  Description of the Exception
	 */
	public String getCreationDate() {
		return get(dateInfoXPath + "/@created");
	}


	/**
	 *  writes a String representaton of the date to both contributor and
	 *  metaMetadata/dateInfo nodes.<p>
	 *
	 *  issue: is it okay to use the same string representation for these nodes
	 *  (they have different type definitions)
	 *
	 *@param  dateStr        The new creationDate value
	 *@exception  Exception  Description of the Exception
	 */
	public void setCreationDate(String dateStr)
		throws Exception {
			
		put(contribXPath + "/@date", dateStr);
		put(dateInfoXPath + "/@created", dateStr);
	}


	/**
	 *  Gets the lastModified attribute of the ResourceRecord object
	 *
	 *@return                The lastModified value
	 *@exception  Exception  Description of the Exception
	 */
	public String getLastModified() {
		return (String)docMap.get(dateInfoXPath + "/@lastModified");
	}


	/**
	 *  Sets the lastModified attribute of the ResourceRecord object
	 *
	 *@param  dateStr        The new lastModified value
	 *@exception  Exception  Description of the Exception
	 */
	public void setLastModified(String dateStr)
		throws Exception {
			put(dateInfoXPath + "/@lastModified", dateStr);
	}


	/**
	 *  Gets the nameFirst attribute of the ResourceRecord object
	 *
	 *@return                The nameFirst value
	 *@exception  Exception  Description of the Exception
	 */
	public String getNameFirst() {
		return get(nameFirstXPath);
	}


	/**
	 *  Sets the nameFirst attribute of the ResourceRecord object
	 *
	 *@param  val            The new nameFirst value
	 *@exception  Exception  Description of the Exception
	 */
	public void setNameFirst(String val)
		throws Exception {
			put(nameFirstXPath, val);
	}


	/**
	 *  Gets the nameLast attribute of the ResourceRecord object
	 *
	 *@return                The nameLast value
	 *@exception  Exception  Description of the Exception
	 */
	public String getNameLast() {
		return get(nameLastXPath);
	}


	/**
	 *  Sets the nameLast attribute of the ResourceRecord object
	 *
	 *@param  val            The new nameLast value
	 *@exception  Exception  Description of the Exception
	 */
	public void setNameLast(String val)
		throws Exception {
			put(nameLastXPath, val);
	}


	/**
	 *  Gets the emailPrimary attribute of the ResourceRecord object
	 *
	 *@return                The emailPrimary value
	 *@exception  Exception  Description of the Exception
	 */
	public String getEmailPrimary() {
		return get(emailPrimaryXPath);
	}


	/**
	 *  Sets the emailPrimary attribute of the ResourceRecord object
	 *
	 *@param  val            The new emailPrimary value
	 *@exception  Exception  Description of the Exception
	 */
	public void setEmailPrimary(String val)
		throws Exception {
			put(emailPrimaryXPath, val);
	}


	/**
	 *  Gets the instName attribute of the ResourceRecord object
	 *
	 *@return                The instName value
	 *@exception  Exception  Description of the Exception
	 */
	public String getInstName() {
		return get(instNameXPath);
	}


	/**
	 *  Sets the instName attribute of the ResourceRecord object
	 *
	 *@param  val            The new instName value
	 *@exception  Exception  Description of the Exception
	 */
	public void setInstName(String val)
		throws Exception {
			put(instNameXPath, val);
	}


	/**
	 *  Sets the debug attribute of the Emailer object
	 *
	 *@param  db  The new debug value
	 */
	public static void setDebug(boolean db) {
		debug = db;
	}


	/**
	 *  Print a line to standard out.
	 *
	 *@param  s  The String to print.
	 */
	private void prtln(String s) {
		if (debug) {
			org.dlese.dpc.schemedit.SchemEditUtils.prtln(s, "ResourceRecord");
		}
	}
	
	/**
	 *  Print selected fields of this object for debugging purposes
	 *
	 *@return    String representation of this ResourceRecord
	 */
	public String toString() {
		try {
			String s = "";

			String emailPrimary = getEmailPrimary();
			if (emailPrimary == null) {
				emailPrimary = "unknown";
			}
			s += "\n\temailPrimary: " + emailPrimary;
			return s;
		} catch (Exception e) {
			System.out.println("ResourceRecord.toString() caught an exception: \n" + e);
			return "";
		}
	}
}

