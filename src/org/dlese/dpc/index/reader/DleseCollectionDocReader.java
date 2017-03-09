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
package org.dlese.dpc.index.reader;

import org.apache.lucene.document.*;
import org.dlese.dpc.index.writer.*;
import org.dlese.dpc.index.*;
import org.dlese.dpc.repository.*;
import org.dlese.dpc.xml.*;
import org.dlese.dpc.webapps.tools.*;
import org.dlese.dpc.util.*;
import org.dlese.dpc.vocab.*;
import org.dlese.dpc.index.document.DateFieldTools;

import javax.servlet.*;
import java.io.*;
import java.text.*;
import java.util.*;

/**
 *  A bean for accessing the data stored in a Lucene {@link org.apache.lucene.document.Document} that was
 *  indexed from a DLESE collection-level metadata record. The index writer that is responsible for creating
 *  this type of Lucene {@link org.apache.lucene.document.Document} is a {@link
 *  org.dlese.dpc.index.writer.DleseCollectionFileIndexingWriter}.
 *
 * @author    John Weatherley
 * @see       org.dlese.dpc.index.writer.DleseCollectionFileIndexingWriter
 */
public class DleseCollectionDocReader extends XMLDocReader {
	private final String DEFAULT = "(null)";
	private final String metadataFormat = "dlese_collect";


	/**  Constructor for the DleseCollectionDocReader object */
	public DleseCollectionDocReader() { }


	/**
	 *  Constructor that may be used programatically to wrap a reader around a Lucene {@link
	 *  org.apache.lucene.document.Document} created by a {@link org.dlese.dpc.index.writer.DleseCollectionFileIndexingWriter}.
	 *
	 * @param  doc    A Lucene {@link org.apache.lucene.document.Document} created by a {@link
	 *      org.dlese.dpc.index.writer.DleseCollectionFileIndexingWriter}.
	 * @param  index  The index being used
	 */
	public DleseCollectionDocReader(Document doc, SimpleLuceneIndex index) {
		super(doc);
		if (index != null) {
			recordDataService = (RecordDataService) index.getAttribute("recordDataService");
			metadataVocab = recordDataService.getVocab();
		}
	}



	/**
	 *  Gets the String 'DleseCollectionDocReader,' which is the key that describes this reader type. This may be
	 *  used in (Struts) beans to determine which type of reader is available for a given search result and thus
	 *  what data is available for display in the UI. The reader type determines which getter methods are
	 *  available.
	 *
	 * @return    The String 'DleseCollectionDocReader'.
	 */
	public String getReaderType() {
		return "DleseCollectionDocReader";
	}


	/**
	 *  Gets the additional metadata for this collection that was indicated in {@link
	 *  org.dlese.dpc.repository.RepositoryManager.putRecord} when the collection was created, or null.
	 *
	 * @return    The additional metadata as an String (may be an XML String), or null if none.
	 */
	public String getAdditionalMetadata() {
		String t = doc.get("collectionAdditionalMetadata");

		if (t == null || t.trim().length() == 0)
			return null;

		return t;
	}

	/**
	 *  Determines whether this collection is currently enabled for viewing.
	 *
	 * @return    True if enabled, otherwise false.
	 */
	public boolean getIsEnabled() {
		String useKey = null;
		try {
			useKey = metadataVocab.getTranslatedValue("dlese_collect", "key", getKey());
		} catch (Throwable e) {
			useKey = getKey();
		}

		SimpleLuceneIndex index = getIndex();
		if (index == null)
			return true;
		RepositoryManager rm = (RepositoryManager) index.getAttribute("repositoryManager");
		if (rm == null)
			return true;
		else
			return rm.isSetEnabled(useKey);
	}


	/**
	 *  Gets the title of the collection, for example 'DLESE Community Collection (DCC)', as indicated by the the
	 *  vocab manager. If the vocab manger is not available, returns the same as {@link #getShortTitle}.
	 *
	 * @return    The vocab manager title for this collection
	 */
	public String getTitle() {
		try {
			return getUiLabelFromVocabName("key", getKey(), metadataFormat);
		} catch (Throwable t) {
			return getShortTitle();
		}
	}


	/**
	 *  Gets the full title of the collection from the XML record.
	 *
	 * @return    The fullTitle value
	 */
	public String getFullTitle() {
		String t = doc.get("fulltitle");

		if (t == null) {
			return "";
		}
		else {
			return t;
		}
	}


	/**
	 *  Gets the short title of the collection directly from the XML record.
	 *
	 * @return    The shortTitle value
	 */
	public String getShortTitle() {
		String t = doc.get("shorttitle");

		if (t == null) {
			return "";
		}
		else {
			return t;
		}
	}



	/**
	 *  Gets the description of the collection.
	 *
	 * @return    The description value
	 */
	public String getDescription() {
		String t = doc.get("description");

		if (t == null) {
			return "";
		}
		else {
			return t;
		}
	}


	/**
	 *  Gets the format of the records in this collection, for example 'adn'.
	 *
	 * @return    The format string, for example, adn.
	 */
	public String getFormatOfRecords() {
		String t = doc.get("formatofrecords");

		if (t == null) {
			return "";
		}
		else {
			return t;
		}
	}


	/**
	 *  Gets the collection key used to identify the items in the collection this record refers to. For example,
	 *  dcc or comet. This is NOT the key to the collection this record belogs to.
	 *
	 * @return    The Key value
	 */
	public String getKey() {
		String t = doc.get("key");

		if (t == null) {
			return "";
		}
		else {
			return t;
		}
	}


	/**
	 *  Gets the most recent accession status of the collection.
	 *
	 * @return    The most recent accession status of the collection.
	 */
	public String getAccessionStatus() {
		String t = doc.get("collaccessionstatus");

		if (t == null) {
			return "";
		}
		else {
			return t;
		}
	}


	/**
	 *  Gets the accession date of this collection, or null if this collection is currently not accessioned.
	 *
	 * @return    The accession date or null if currently not accessioned
	 */
	public Date getAccessionDateDate() {
		String t = doc.get("collaccessiondate");

		if (t == null)
			return null;

		try {
			// Stored in the form 20090207080305
			return new Date(DateFieldTools.stringToTime(t));
		} catch (Throwable e) {
			return null;
		}
	}


	/**
	 *  Gets part of DRC status [true or false].
	 *
	 * @return    The String "true" or "false".
	 */
	public String getPartOfDRC() {
		if (isPartOfDRC())
			return "true";
		else
			return "false";
	}


	/**
	 *  Gets part of DRC status [true or false].
	 *
	 * @return    True if this collection is part of DRC, false otherwise.
	 */
	public boolean isPartOfDRC() {
		String t = doc.get("partofdrc");

		if (t == null || t.equals("false"))
			return false;
		else
			return true;
	}


	/**
	 *  Gets the collection URL or empty String if none is supplied.
	 *
	 * @return    The url value
	 */
	public String getCollectionUrl() {
		String t = doc.get("url");

		if (t == null) {
			return "";
		}
		else {
			return t;
		}
	}


	/**
	 *  Gets the url to the collection's scope statement.
	 *
	 * @return    The scope URL or empty String.
	 */
	public String getScopeUrl() {
		String t = doc.get("scopeurl");

		if (t == null) {
			return "";
		}
		else {
			return t;
		}
	}


	/**
	 *  Gets the URL to the collection's review process statement.
	 *
	 * @return    The URL to the collection's review process statement, or empty String if not available.
	 */
	public String getReviewProcessUrl() {
		String t = doc.get("reviewprocessurl");

		if (t == null) {
			return "";
		}
		else {
			return t;
		}
	}


	/**
	 *  Gets the collection's review process statement.
	 *
	 * @return    The collection's review process statement, or empty String if not available.
	 */
	public String getReviewProcess() {
		String t = doc.get("reviewprocess");

		if (t == null) {
			return "";
		}
		else {
			return t;
		}
	}


	/**
	 *  Gets the collection's cost.
	 *
	 * @return    The cost of the collection.
	 */
	public String getCost() {
		String t = doc.get("cost");

		if (t == null) {
			return "";
		}
		else {
			return t;
		}
	}


	/**
	 *  Gets the collection this record is part of, as a string.
	 *
	 * @return    The collection.
	 */
	public String getCollectionsString() {
		return getSetString();
	}


	/**
	 *  Gets the GradeRanges associated with this collection, encoded by the vocab manager if it's available.
	 *
	 * @return    The gradeRanges value
	 */
	public String[] getGradeRanges() {
		String t = doc.get(getFieldId("gradeRange", metadataFormat)).trim();
		String[] vals;
		if (t == null || t.length() == 0) {
			return null;
		}
		return t.split("\\+");
	}


	/**
	 *  Gets the subjects associated with this collection, encoded by the vocab manager if it's available.
	 *
	 * @return    The subjects value
	 */
	public String[] getSubjects() {
		String t = doc.get(getFieldId("subject", metadataFormat)).trim();
		String[] vals;
		if (t == null || t.length() == 0) {
			return null;
		}
		return t.split("\\+");
	}


	/**
	 *  Gets the keywords as an array of Strings. Note that some keywords are actually phrases comprised of
	 *  multiple words.
	 *
	 * @return    The keywords
	 */
	public String[] getKeywords() {
		String t = doc.get("keyword");

		if (t == null || t.trim().length() == 0) {
			return null;
		}
		else {
			return t.trim().split("\\+");
		}
	}


	/**
	 *  Gets the keywords a comma separated list terminated with a period suitable for display to users. For
	 *  example: ocean, sea, rain.
	 *
	 * @return    The keywords displayed suitable for users.
	 */
	public String getKeywordsDisplay() {
		String[] keywords = getKeywords();
		if (keywords == null || keywords.length == 0) {
			return null;
		}

		String disp = "";
		for (int i = 0; i < keywords.length - 1; i++) {
			disp += keywords[i] + ", ";
		}
		disp += keywords[keywords.length - 1] + ".";
		return disp;
	}


	/**
	 *  Gets the location of the files for this collection on the server.
	 *
	 * @return    The locationOfFiles value
	 */
	public String getLocationOfFiles() {
		try {
			File f = new File(getRepositoryManager().getMetadataRecordsLocation() + "/" + getFormatOfRecords() + "/" + getKey());
			return f.getAbsolutePath();
		} catch (Throwable e) {
			prtlnErr("Unable to get location of files: " + e);
			return "";
		}
	}


	/**
	 *  Gets the number of files for this collection.
	 *
	 * @return    The numFiles value
	 */
	public String getNumFiles() {
		try {
			File dir = new File(getLocationOfFiles());
			return Long.toString(dir.listFiles(new XMLFileFilter()).length);
		} catch (Throwable e) {
			return "0";
		}
	}


	/**
	 *  Gets the number of items indexed for this collection.
	 *
	 * @return    The numIndexed value
	 */
	public String getNumIndexed() {
		return Integer.toString(getIndex().getNumDocs("collection:0" + getKey()));
	}


	/**
	 *  Gets the number of indexing errors that were found for this collection.
	 *
	 * @return    The numIndexingErrors value
	 */
	public String getNumIndexingErrors() {
		return Integer.toString(getIndex().getNumDocs("error:true AND docdir:\"" +
			SimpleLuceneIndex.escape(new File(getLocationOfFiles()).getAbsolutePath()) + "\""));
	}


	/**
	 *  Gets the contact contributor bean with that contains methods for accessing contributor information (Not
	 *  implemented). Note: This is not yet implemented (returns null)!!!
	 *
	 * @return    The contact Contributor bean.
	 */
	public Contributor getContact() {
		return null;
		//org.dom4j.Document xmlDoc = getXmlDoc();
		//return getContributor();
	}


	/**
	 *  Gets the responsibleParty contributor bean with that contains methods for accessing contributor
	 *  information. Note: This is not yet implemented (returns null)!!!
	 *
	 * @return    The responsibleParty Contributor bean.
	 */
	public Contributor getResponsibleParty() {
		return null;
		//org.dom4j.Document xmlDoc = getXmlDoc();
		//return getContributor(...);
	}


	private Contributor getContributor() {
		// Needs implementation!

		org.dom4j.Document xmlDoc = getXmlDoc();
		String nameTitle = null;
		String nameFirst = null;
		String nameLast = null;
		String instName = null;
		String instDept = null;
		String emailPrimary = null;
		String emailAlt = null;
		String contactID = null;

		return new PersonContributor(
			nameTitle,
			nameFirst,
			nameLast,
			instName,
			instDept,
			emailPrimary,
			emailAlt,
			contactID);
	}


	/**
	 *  A bean that holds information about a DLESE contributor that is a person.
	 *
	 * @author    John Weatherley
	 */
	public class PersonContributor implements Contributor {
		String nameTitle, nameFirst, nameLast, instName, instDept, emailPrimary, emailAlt, contactID;


		/**
		 *  Constructor for the PersonContributor object
		 *
		 * @param  nameTitle     Title
		 * @param  nameFirst     First name
		 * @param  nameLast      Last Name
		 * @param  instName      Institution Name
		 * @param  instDept      Institution Dept
		 * @param  emailPrimary  Primary email
		 * @param  emailAlt      Alternate email
		 * @param  contactID     Contact ID
		 */
		public PersonContributor(
		                         String nameTitle,
		                         String nameFirst,
		                         String nameLast,
		                         String instName,
		                         String instDept,
		                         String emailPrimary,
		                         String emailAlt,
		                         String contactID) {
			this.nameFirst = nameFirst;
			this.nameTitle = nameTitle;
			this.nameLast = nameLast;
			this.instName = instName;
			this.instDept = instDept;
			this.emailPrimary = emailPrimary;
			this.emailAlt = emailAlt;
		}


		/**
		 *  Gets the type, which is 'person'.
		 *
		 * @return    The String 'person'.
		 */
		public String getType() {
			return "person";
		}


		/**
		 *  Gets the nameTitle attribute of the PersonContributor object
		 *
		 * @return    The nameTitle value
		 */
		public String getNameTitle() {
			return nameTitle;
		}


		/**
		 *  Gets the nameFirst attribute of the PersonContributor object
		 *
		 * @return    The nameFirst value
		 */
		public String getNameFirst() {
			return nameFirst;
		}


		/**
		 *  Gets the nameLast attribute of the PersonContributor object
		 *
		 * @return    The nameLast value
		 */
		public String getNameLast() {
			return nameLast;
		}


		/**
		 *  Gets the instName attribute of the PersonContributor object
		 *
		 * @return    The instName value
		 */
		public String getInstName() {
			return instName;
		}


		/**
		 *  Gets the instDept attribute of the PersonContributor object
		 *
		 * @return    The instDept value
		 */
		public String getInstDept() {
			return instDept;
		}


		/**
		 *  Gets the emailPrimary attribute of the PersonContributor object
		 *
		 * @return    The emailPrimary value
		 */
		public String getEmailPrimary() {
			return emailPrimary;
		}


		/**
		 *  Gets the emailAlt attribute of the PersonContributor object
		 *
		 * @return    The emailAlt value
		 */
		public String getEmailAlt() {
			return emailAlt;
		}


		/**
		 *  Gets the contactID attribute of the PersonContributor object
		 *
		 * @return    The contactID value
		 */
		public String getContactID() {
			return contactID;
		}

	}


	/**
	 *  A bean that holds information about a DLESE contributor that is an organization.
	 *
	 * @author    John Weatherley
	 */
	public class OrganizationContributor implements Contributor {
		/**
		 *  Gets the type, which is 'organization'.
		 *
		 * @return    The String 'organization'.
		 */
		public String getType() {
			return "organization";
		}
	}


	/**
	 *  A bean that holds information about a DLESE contributor.
	 *
	 * @author    John Weatherley
	 */
	public interface Contributor {
		/**
		 *  Gets the type attribute of the Contributor object
		 *
		 * @return    The type value
		 */
		public String getType();
	}

}


