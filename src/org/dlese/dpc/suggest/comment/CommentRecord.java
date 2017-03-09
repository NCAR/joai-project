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
package org.dlese.dpc.suggest.comment;

import org.dlese.dpc.suggest.SuggestionRecord;
import org.dlese.dpc.xml.schema.*;
import org.dlese.dpc.xml.*;

import java.io.*;
import java.util.*;

import org.dom4j.Node;
import org.dom4j.Element;
import org.dom4j.Document;
import org.dom4j.DocumentException;

/**
 *  SuggestionRecord for Suggestors using the "dlese_anno" metadata framework.
 *
 * @author     Jonathan Ostwald
 * @version    $Id: CommentRecord.java,v 1.2 2009/03/20 23:34:00 jweather Exp $
 */
public class CommentRecord extends SuggestionRecord {
	private static boolean debug = true;

	/**
	 *  Creates a new CommentRecord given a LOCALIZED dom4j.Document and
	 *  SchemaHelper, but doesn't assign an id
	 *
	 * @param  doc           {@link org.dom4j.Document}
	 * @param  schemaHelper  NOT YET DOCUMENTED
	 */
	public CommentRecord(Document doc, SchemaHelper schemaHelper) {
		super(doc, schemaHelper);
	}



	/*
	 *  ====XML Element Accessors ==============================================
	 */
	 
	 private String title_path = "/annotationRecord/annotation/title";
	 
	/**
	 *  Gets the title attribute of the CommentRecord object
	 *
	 * @return                The title value
	 */
	public String getTitle() {
		return (String) docMap.get(title_path);
	}


	/**
	 *  Sets the title attribute of the CommentRecord object
	 *
	 * @param  val            The new title value
	 * @exception  Exception  Description of the Exception
	 */
	public void setTitle(String val) throws Exception {
		put(title_path, val);
	}

	private String itemID_path = "/annotationRecord/itemID";

	/**
	 *  Gets the itemID attribute of the CommentRecord object
	 *
	 * @return                The itemID value
	 */
	public String getItemID() {
		return (String) docMap.get(itemID_path);
	}


	/**
	 *  Sets the itemID attribute of the CommentRecord object
	 *
	 * @param  val            The new itemID value
	 * @exception  Exception  Description of the Exception
	 */
	public void setItemID(String val)
		 throws Exception {
		put(itemID_path, val);
	}

	private String description_path = "/annotationRecord/annotation/content/description";

	/**
	 *  Gets the description attribute of the CommentRecord object
	 *
	 * @return                The description value
	 */
	public String getDescription() {
		return get(description_path);
	}


	/**
	 *  Sets the description attribute of the CommentRecord object
	 *
	 * @param  val            The new description value
	 * @exception  Exception  Description of the Exception
	 */
	public void setDescription(String val)
		 throws Exception {
		if (val == null || val.trim().length() == 0)
			val = "putdescription here";
		put(description_path, val);
	}

	// CONTRIBUTOR getters and setters
	
	private String contributor_path = "/annotationRecord/annotation/contributors/contributor";

	/**
	 *  Returns a contributor element having the specified child ("organization" or
	 *  "person", creating the element and specified child if necessary.
	 *
	 * @param  child          NOT YET DOCUMENTED
	 * @return                The contributorElement value
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	private Element getContributorElement(String child) throws Exception {
		if (!("organization".equals(child) || "person".equals(child)))
			throw new Exception("unknown contributor type specified: " + child);

		String childPath = contributor_path + "/" + child;

		// prtln ("childPath: " + childPath);
		Element childElement = (Element) docMap.selectSingleNode(childPath);

		if (childElement == null) {
			// prtln ("childElement does not yet exist");
			String contributors_path = "/annotationRecord/annotation/contributors";
			Element contributors = (Element) docMap.selectSingleNode(contributors_path);
			if (contributors == null)
				contributors = (Element) docMap.createNewNode(contributors_path);

			// prtln ("\nbefore adding contributor element");
			// pp (contributors);

			Element newContributor = contributors.addElement("contributor");

			// prtln ("\nafter adding contributor element");
			// pp (contributors);

			childElement = newContributor.addElement(child);

		}

		return childElement.getParent();
	}


	/**
	 *  Gets the orgContributor attribute of the CommentRecord object
	 *
	 * @return    The orgContributor value
	 */
	private Element getOrgContributor() {
		try {
			return getContributorElement("organization");
		} catch (Throwable t) {
			prtln(t.getMessage());
			return null;
		}
	}


	/**
	 *  Gets the personContributor attribute of the CommentRecord object
	 *
	 * @return    The personContributor value
	 */
	private Element getPersonContributor() {
		try {
			return getContributorElement("person");
		} catch (Throwable t) {
			prtln(t.getMessage());
			return null;
		}
	}

	// person_contributor attributes
	private String person_contributor_path = contributor_path + "/person";
	private String nameFirst_path = person_contributor_path + "/nameFirst";
	private String nameLast_path = person_contributor_path + "/nameLast";
	private String instName_path = person_contributor_path + "/instName";
	private String email_path = person_contributor_path + "/email";


	/**
	 *  There may be more than one contributor element. We are assigning the role
	 *  to the contributor/PERSON tagset.
	 *
	 * @return                The role value
	 */
	public String getRole() {
		if (docMap.nodeExists(person_contributor_path)) {
			try {
				return this.getPersonContributor().attributeValue("role");
			} catch (Throwable t) {}
		}

		return null;
	}


	/**
	 *  Sets the role attribute of the CommentRecord object for the contributor/PERSON
	 *  tagset.
	 *
	 * @param  val            The new role value
	 * @exception  Exception  Description of the Exception
	 */
	public void setRole(String val)
		 throws Exception {

		Element personContributor = getPersonContributor();

		if (personContributor == null)
			prtln("personContributor is NULL");

		personContributor.addAttribute("role", val);

	}


	/**
	 *  Gets the share attribute of the CommentRecord object
	 *
	 * @return                The share value
	 */
	public String getShare() {
		if (docMap.nodeExists(person_contributor_path)) {
			try {
				return this.getPersonContributor().attributeValue("share");
			} catch (Throwable t) {}
		}
		return null;
	}


	/**
	 *  Sets the share attribute of the CommentRecord object
	 *
	 * @param  val            The new share value
	 * @exception  Exception  Description of the Exception
	 */
	public void setShare(String val)
		 throws Exception {

		Element personContributor = getPersonContributor();
		personContributor.addAttribute("share", ("true".equals(val) ? "true" : "false"));
	}

	/**
	 *  Gets the nameFirst attribute of the CommentRecord object
	 *
	 * @return                The nameFirst value
	 */
	public String getNameFirst() {
		return get(nameFirst_path);
	}


	/**
	 *  Sets the nameFirst attribute of the CommentRecord object
	 *
	 * @param  val            The new nameFirst value
	 * @exception  Exception  Description of the Exception
	 */
	public void setNameFirst(String val)
		 throws Exception {
		put(nameFirst_path, val);
	}


	/**
	 *  Gets the nameLast attribute of the CommentRecord object
	 *
	 * @return                The nameLast value
	 */
	public String getNameLast() {
		return get(nameLast_path);
	}


	/**
	 *  Sets the nameLast attribute of the CommentRecord object
	 *
	 * @param  val            The new nameLast value
	 * @exception  Exception  Description of the Exception
	 */
	public void setNameLast(String val)
		 throws Exception {
		put(nameLast_path, val);
	}


	/**
	 *  Gets the email attribute of the CommentRecord object
	 *
	 * @return                The email value
	 */
	public String getEmail() {
		return get(email_path);
	}


	/**
	 *  Sets the email attribute of the CommentRecord object
	 *
	 * @param  val            The new email value
	 * @exception  Exception  Description of the Exception
	 */
	public void setEmail(String val)
		 throws Exception {
		put(email_path, val);
	}


	/**
	 *  Gets the instName attribute of the CommentRecord object
	 *
	 * @return                The instName value
	 */
	public String getInstName() {
		return get(instName_path);
	}


	/**
	 *  Sets the instName attribute of the CommentRecord object
	 *
	 * @param  val            The new instName value
	 * @exception  Exception  Description of the Exception
	 */
	public void setInstName(String val)
		 throws Exception {
		put(instName_path, val);
	}

	private String service_date_created_path = "/annotationRecord/service/date/@created";

	/**
	 *  Gets the creation date for this CommentRecord. 
	 *
	 * @return                The creationDate value (e.g., "2007-09-23")
	 */
	public String getCreationDate() {
		return get(service_date_created_path);
	}


	/**
	 *  Writes a String representaton of the date to various locations in the instance
	 document.<p>
	 *
	 * @param  dateStr        The creationDate value (e.g., "2007-09-23")
	 * @exception  Exception  if the creationDate cannot be set
	 */
	public void setCreationDate(String dateStr) throws Exception {

		put(service_date_created_path, dateStr);

		Element orgContributor = this.getOrgContributor();
		orgContributor.addAttribute("date", dateStr);

		if (docMap.nodeExists(person_contributor_path)) {
			Element personContributor = getPersonContributor();
			personContributor.addAttribute("date", dateStr);
		}
	}


	/**
	 *  Sets the debug attribute of the Emailer object
	 *
	 * @param  db  The new debug value
	 */
	public static void setDebug(boolean db) {
		debug = db;
	}


	/**
	 *  Print a line to standard out.
	 *
	 * @param  s  The String to print.
	 */
	private static void prtln(String s) {
		if (debug) {
			org.dlese.dpc.schemedit.SchemEditUtils.prtln(s, "CommentRecord");
		}
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  node  NOT YET DOCUMENTED
	 */
	private static void pp(Node node) {
		prtln(Dom4jUtils.prettyPrint(node));
	}

		/**
	 *  The main program for the CommentRecord class
	 *
	 * @param  args           The command line arguments
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public static void main(String[] args) throws Exception {
		String path = "C:/Documents and Settings/ostwald/devel/projects/dlese-suggester-project/web/WEB-INF/data/commentTemplate.xml";
		Document doc = Dom4jUtils.getXmlDocument(new File(path));
		String rootElementName = doc.getRootElement().getName();
		doc = Dom4jUtils.localizeXml(doc, rootElementName);

		String schemaLoc = "http://www.dpc.ucar.edu/people/ostwald/Metadata/annotation/1.0.00/annotation.xsd";
		java.net.URL schemaUrl = new java.net.URL(schemaLoc);
		SchemaHelper schemaHelper = new SchemaHelper(schemaUrl, "annotationRecord");

		prtln("\n\nCommentRecord");

		CommentRecord record = new CommentRecord(doc, schemaHelper);

		record.setTitle("myTitle");
		record.setItemID("myItemID");
		record.setDescription("myDescription");
		record.setRole("myRole");
		record.setShare("myShare");
		record.setNameFirst("myNameFirst");
		record.setNameLast("myNameLast");
		record.setInstName("myInstName");
		record.setEmail("myEmail");
		record.setCreationDate("XXX-XX-XX");
		pp(record.doc);
		prtln(record.toString());
	}

	/**
	 *  Print selected fields of this object for debugging purposes
	 *
	 * @return    String representation of this CommentRecord
	 */
	public String toString() {
		try {
			String s = "";
			s += "\n\t title: " + getTitle();
			s += "\n\t description: " + getDescription();
			s += "\n\t role: " + getRole();
			s += "\n\t share: " + getShare();
			s += "\n\t nameFirst: " + getNameFirst();
			s += "\n\t nameLast: " + getNameLast();
			s += "\n\t instName: " + getInstName();
			s += "\n\t email: " + getEmail();
			s += "\n\t creationDate: " + getCreationDate();

			/* 			String email = getEmail();
			if (email == null) {
				email = "unknown";
			}
			s += "\n\t email: " + email; */
			return s;
		} catch (Exception e) {
			System.out.println("CommentRecord.toString() caught an exception: \n" + e);
			return "";
		}
	}

}

