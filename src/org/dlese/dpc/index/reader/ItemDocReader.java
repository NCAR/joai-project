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
import org.apache.lucene.queryParser.*;
import org.apache.lucene.search.*;
import org.dlese.dpc.index.writer.*;
import org.dlese.dpc.index.*;
import org.dlese.dpc.repository.*;
import org.dlese.dpc.xml.*;
import org.dlese.dpc.webapps.tools.*;
import org.dlese.dpc.util.*;
import org.dlese.dpc.vocab.*;
import org.dlese.dpc.index.document.DateFieldTools;

import org.dom4j.Element;

import javax.servlet.*;
import java.io.*;
import java.text.*;
import java.util.*;

/**
 *  A bean for accessing the data stored in a Lucene {@link org.apache.lucene.document.Document} that was
 *  indexed from a DLESE item-level metadata record, such as ADN. The index writer that is responsible for
 *  creating this type of Lucene {@link org.apache.lucene.document.Document} is a {@link
 *  org.dlese.dpc.index.writer.ItemFileIndexingWriter}.
 *
 * @author     John Weatherley
 * @see        org.dlese.dpc.index.writer.ItemFileIndexingWriter
 */
public class ItemDocReader extends XMLDocReader {
	private final String DEFAULT = "(null)";
	private ResultDocList associatedItemResultDocs = null;
	private ResultDocList displayableAssociatedItemResultDocs = null;
	private ResultDocList allItemResultDocs = null;
	private ResultDocList annotationResultDocs = null;
	private ResultDocList deDupedResultDocs = null;
	private boolean associatedItemsInitialized = false;
	private boolean displayableAssociatedItemsInitialized = false;
	private boolean allItemsInitialized = false;
	private boolean annotationDocReadersInitialized = false;
	private ArrayList missingAssociatedItemIds = null;
	private boolean multiRecordStatus = false;
	private Document multiDoc = null;
	private RepositoryManager rm = null;
	private HashMap completedAnnosByType = null;
	private ArrayList completedAnnos = null;
	private HashMap inProgressAnnosByFormat = null;
	private int numCompletedAnnos = 0;
	private int numInProgressAnnos = 0;
	private String[] relatedResourceIds = null;
	private String[] relatedResourceUrls = null;
	private String[] allIds = null;


	/**  Constructor for the ItemDocReader object */
	public ItemDocReader() { }


	/**  Initializes the ItemDocReader at search time. */
	public void init() {
		super.init();

		// Determine my multirecord status (true|false)
		String status = doc.get("multirecord");
		multiRecordStatus = (status != null && !status.equals("false"));

		multiDoc = doc;
		if (isMultiRecord()) {
			SimpleLuceneIndex dupItemsIndex =
				(SimpleLuceneIndex) conf.index.getAttribute("dupItemsIndex");

			// De-duping algorithm - choose the best record possible:
			if (dupItemsIndex != null) {
				ResultDocList deDupDocs = null;
				String q = null;

				// If sorting has been requested, use the first sorted record
				if (conf.attributes != null) {
					if (conf.attributes.get("sortAscendingByField") != null || conf.attributes.get("sortDescendingByField") != null) {
						deDupDocs = doGetDeDupedResultDocs(true, true);
						if (deDupDocs != null && deDupDocs.size() > 0) {
							//printQuery("De-dup is using sort query 0\n");

							setDoc(deDupDocs.get(0).getDocument());
							return;
						}
					}
				}

				// Grab the IDs for this multi-record
				String[] allIds = getAllIds();

				// Format idQuery using + syntax
				String idQuery = "+(id:" + SimpleLuceneIndex.encodeToTerm(allIds[0]);
				for (int i = 1; i < allIds.length; i++)
					idQuery += " id:" + SimpleLuceneIndex.encodeToTerm(allIds[i]);
				idQuery += ") ";

				// First query  ------------------------------------------------------

				// Since we're using the generated + syntax, we must use default OR query parser to search
				q = idQuery + removeMultiRecordQueryField(conf.query);
				QueryParser qp = dupItemsIndex.getQueryParser();
				qp.setDefaultOperator(QueryParser.Operator.OR);
				Query lq = null;

				try {
					lq = qp.parse(q);
				} catch (Exception pe) {
					prtlnErr("De-dup query 1 had a ParseException: " + pe);
				}

				deDupDocs = dupItemsIndex.searchDocs(lq, conf.filter);
				//printQuery("\nDe-dup query 1: " + q + " had " + (deDupDocs == null ? 0 : deDupDocs.size()) + " results.\n");
				if (deDupDocs != null && deDupDocs.size() > 0) {
					//printQuery("De-dup is using query 1\n");
					setDoc(deDupDocs.get(0).getDocument());
					return;
				}

				// Grab the query fields that were entered
				String formattedUserQuery = null;
				String collectionsQuery = null;
				String discoverableItemsQuery = null;
				String reviewedBoostingQuery = null;
				if (conf.attributes != null) {
					formattedUserQuery = (String) conf.attributes.get("formattedUserQuery");
					formattedUserQuery = (formattedUserQuery != null) ? removeMultiRecordQueryField(formattedUserQuery) : formattedUserQuery;
					collectionsQuery = (String) conf.attributes.get("collectionsQuery");
					discoverableItemsQuery = (String) conf.attributes.get("discoverableItemsQuery");
					reviewedBoostingQuery = (String) conf.attributes.get("reviewedBoostingQuery");
				}

				// Second query ------------------------------------------------------

				// Re-format the idQuery to use AND syntax
				idQuery = "(id:" + SimpleLuceneIndex.encodeToTerm(allIds[0]);
				for (int i = 1; i < allIds.length; i++)
					idQuery += " OR id:" + SimpleLuceneIndex.encodeToTerm(allIds[i]);
				idQuery += ")";

				if (formattedUserQuery == null)
					formattedUserQuery = getNonFieldedQueryTerms(conf.query).trim();

				if (formattedUserQuery.length() > 0)
					q = "(" + formattedUserQuery + ") AND " + idQuery;
				else
					q = idQuery;

				q = (collectionsQuery != null) ? (q += " AND " + collectionsQuery) : q;
				q = (reviewedBoostingQuery != null) ? (q += " AND " + reviewedBoostingQuery) : q;
				q = (discoverableItemsQuery != null) ? (q += " AND " + discoverableItemsQuery) : q;

				deDupDocs = dupItemsIndex.searchDocs(q, conf.filter);
				//printQuery("\nDe-dup query 2: " + q + " had " + (deDupDocs == null ? 0 : deDupDocs.length) + " results.\n");
				if (deDupDocs != null && deDupDocs.size() > 0) {
					//printQuery("De-dup is using query 2\n");
					setDoc(deDupDocs.get(0).getDocument());
					return;
				}

				// Third query ------------------------------------------------------
				// search only by IDs, collection, review bossting and discoverable status...

				q = idQuery;
				q = (collectionsQuery != null) ? (q += " AND " + collectionsQuery) : q;
				q = (reviewedBoostingQuery != null) ? (q += " AND " + reviewedBoostingQuery) : q;
				q = (discoverableItemsQuery != null) ? (q += " AND " + discoverableItemsQuery) : q;

				deDupDocs = dupItemsIndex.searchDocs(q, conf.filter);

				//printQuery("\nDe-dup query 3: " + q + " had " + (deDupDocs == null ? 0 : deDupDocs.length) + " results.\n");
				if (deDupDocs != null && deDupDocs.size() > 0) {
					//printQuery("De-dup is using query 3\n");
					setDoc(deDupDocs.get(0).getDocument());
					return;
				}

				// Fourth query ------------------------------------------------------
				// Search only by IDs, with filter, return the first one

				deDupDocs = doGetDeDupedResultDocs(true, false);

				if (deDupDocs != null && deDupDocs.size() > 0) {
					//printQuery("De-dup is using query 4\n");
					setDoc(deDupDocs.get(0).getDocument());
					return;
				}

				// Fifth query ------------------------------------------------------
				// Search only by IDs, with no filter - return the first one - should always work

				deDupDocs = doGetDeDupedResultDocs(false, false);

				if (deDupDocs != null && deDupDocs.size() > 0) {
					//printQuery("De-dup is using query 5\n");
					setDoc(deDupDocs.get(0).getDocument());
					return;
				}
			}
		}
	}

	protected Document getMultiDoc() {
		return multiDoc;	
	}
	
	private final void printQuery(String s) {
		if (true)
			System.out.println(s);
	}

	private String removeMultiRecordQueryField(String query) {
		//printQuery("removeMultiRecordQueryField() query: '" + query + "'");
		String result = query.replaceAll("multirecord:true", "multirecord:false");
		//String result = query.replaceAll("\\(multirecord:true\\) AND |multirecord:true AND |\\(multirecord:true\\)", "");
		//printQuery("removeMultiRecordQueryField() removed query: '" + result + "'");
		return result;
	}


	private String getNonFieldedQueryTerms(String query) {
		query = query.replaceAll("\\(|\\)|\\\"", "");
		String[] terms = query.split("\\s+");
		if (terms == null) {
			return query;
		}
		StringBuffer newQuery = new StringBuffer();
		boolean prevIsTerm = false;
		for (int i = 0; i < terms.length; i++) {
			//prtln("term: " + terms[i]);

			if (terms[i].matches("[^:]*")) {
				//prtln("matching term: " + terms[i]);
				if (terms[i].matches("AND|OR")) {
					if (prevIsTerm) {
						newQuery.append(terms[i]);
						newQuery.append(" ");
						prevIsTerm = false;
					}
				}
				else {
					newQuery.append(terms[i]);
					newQuery.append(" ");
					//prtln("good term: " + terms[i]);
					prevIsTerm = true;
				}
			}
			else {
				prevIsTerm = false;
			}
		}

		String q = newQuery.toString();
		if (q.endsWith(" OR ")) {
			return q.substring(0, q.length() - 4);
		}
		if (q.endsWith(" AND ")) {
			return q.substring(0, q.length() - 5);
		}
		return q;
	}


	private String getFieldedQueryTerms(String query) {
		String[] terms = query.split("\\s+");
		if (terms == null) {
			return query;
		}
		StringBuffer newQuery = new StringBuffer();
		boolean hasRealTerm = false;
		for (int i = 0; i < terms.length; i++) {
			//prtln("term: " + terms[i]);

			if (terms[i].matches(".*\\(.*:.*|.*\\).*:.*|.*:.*")) {
				newQuery.append(terms[i]);
				newQuery.append(" ");
				hasRealTerm = true;
			}
			else if (hasRealTerm && terms[i].matches(".*AND.*|.*OR.*")) {
				newQuery.append(terms[i]);
				newQuery.append(" ");
			}
		}
		return newQuery.toString();
	}


	/**
	 *  Constructor that may be used programatically to wrap a reader around a Lucene {@link
	 *  org.apache.lucene.document.Document} created by a {@link org.dlese.dpc.index.writer.ItemFileIndexingWriter}.
	 *
	 * @param  doc    A Lucene {@link org.apache.lucene.document.Document} created by a {@link
	 *      org.dlese.dpc.index.writer.ItemFileIndexingWriter}.
	 * @param  index  The SimpleLuceneIndex in use
	 */
	public ItemDocReader(Document doc, SimpleLuceneIndex index) {
		super(doc);
		if (index != null) {
			recordDataService = (RecordDataService) index.getAttribute("recordDataService");
			metadataVocab = recordDataService.getVocab();
		}
	}



	/**
	 *  Gets the String 'ItemDocReader,' which is the key that describes this reader type. This may be used in
	 *  (Struts) beans to determine which type of reader is available for a given search result and thus what
	 *  data is available for display in the UI. The reader type determines which getter methods are available.
	 *
	 * @return    The String 'ItemDocReader'.
	 */
	public String getReaderType() {
		return "ItemDocReader";
	}


	/**
	 *  Gets the title of the item.
	 *
	 * @return    The title value
	 */
	public String getTitle() {
		String t = doc.get("title");

		if (t == null) {
			return "";
		}
		else {
			return t;
		}
	}


	/**
	 *  Gets the keywords as a sorted array. Duplicate keywords are ommitted.
	 *
	 * @return    The keywords
	 */
	public String[] getKeywords() {
		String t = doc.get("keyword");

		// Already sorted at index time...
		if (t == null || t.trim().length() == 0)
			return null;
		else
			return t.trim().split("\\+");
	}


	/**
	 *  Gets the keywords for all associated records as a sorted array. Duplicate keywords are ommitted.
	 *
	 * @return    The keywords
	 */
	public String[] getMultiKeywords() {
		String t = multiDoc.get("keyword");

		// Already sorted at index time...
		if (t == null || t.trim().length() == 0)
			return null;
		else
			return t.trim().split("\\+");
	}


	/**
	 *  Gets the keywords as a sorted comma separated list terminated with a period suitable for display to
	 *  users. For example: ocean, rain, sea.
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
	 *  Gets the concatinated title of all associated records for this item.
	 *
	 * @return    The concatinated title text.
	 */
	public String getMultiTitle() {
		String t = multiDoc.get("title");

		if (t == null) {
			return "";
		}
		else {
			return t;
		}
	}



	/**
	 *  Gets the description for this item.
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
	 *  Gets the concatinated descriptions of all associated records for this item.
	 *
	 * @return    The multiDescription value
	 */
	public String getMultiDescription() {
		if (multiDoc == null) {
			return "";
		}

		String t = multiDoc.get("description");

		if (t == null) {
			return "";
		}
		else {
			return t;
		}
	}


	/**
	 *  Gets the the concatinated IDs of all associated records for this item.
	 *
	 * @return    The multiIds value
	 */
	public String getMultiIds() {
		if (multiDoc == null)
			return "";

		String val = "";
		String vals[] = multiDoc.getValues("idvalue");
		for(int i = 0; i < vals.length; i++)
			val += vals[i] + (vals.length-1 == i ? "" : " ");
		
		return val;		
	}



	/**
	 *  Gets the part-of-DRC status (true or false).
	 *
	 * @return    The String "true" or "false".
	 */
	public String getPartOfDRC() {
		String t = multiDoc.get("partofdrc");

		if (t == null) {
			return "false";
		}
		else {
			return t;
		}
	}


	/**
	 *  Gets the URL for this resource.
	 *
	 * @return    The url value
	 */
	public String getUrl() {
		String t = doc.get("url");

		if (t == null) {
			return DEFAULT;
		}
		else {
			return t;
		}
	}


	/**
	 *  Gets the URL for this resource.
	 *
	 * @return    The url value
	 */
	public String getUrlEncoded() {
		String t = doc.get("urlenc");

		if (t == null) {
			return DEFAULT;
		}
		else {
			return t;
		}
	}


	/**
	 *  Gets the URL for this resource, truncated if it is very long
	 *
	 * @return    The url value
	 */
	public String getUrlTruncated() {
		final int CUTOFF = 80;
		String t = doc.get("url");

		if (t == null) {
			return DEFAULT;
		}
		else {
			if (t.length() > CUTOFF + 10) {
				int ind = t.indexOf("/", CUTOFF);
				if (ind > -1 && ind < CUTOFF + 10)
					t = t.substring(0, ind + 1) + "...";
				else
					t = t.substring(0, CUTOFF) + "...";
			}
			return t;
		}
	}


	/**
	 *  Gets the collections associated with this record as a single String. Same as Same as {@link
	 *  #getSetString()}.
	 *
	 * @return    The collections.
	 */
	public String getCollectionsString() {
		return getSetString();
	}


	/**
	 *  Gets the Whats New type for the multi-record (if it exists), which is one of 'itemnew,'
	 *  'itemannocomplete,' 'itemannoinprogress,' 'annocomplete,' 'annoinprogress,' 'drcannocomplete,'
	 *  'drcannoinprogress,' 'collection'.
	 *
	 * @return    The What's New type or empty String.
	 */
	public String getMultiWhatsNewType() {
		String t = multiDoc.get("wntype");

		if (t == null)
			return "";
		return t;
	}


	/**
	 *  Gets the Whats New date for the multi-record as a String, which is the whats new date for this resource
	 *  across all records that catalog it. Note that this is the appropriate method to use when displaying this
	 *  value to an end user, instead of {@link #getWhatsNewDate()}.
	 *
	 * @return    The What's New date or empty String.
	 */
	public String getMultiWhatsNewDate() {
		String t = multiDoc.get("wndate");

		if (t == null)
			return "";

		try {
			long modTime = DateFieldTools.stringToTime(t);
			SimpleDateFormat df = new SimpleDateFormat("MMM' 'dd', 'yyyy");
			return df.format(new Date(modTime));
		} catch (Throwable e) {
			prtlnErr("Error getWhatsNewDate(): " + e);
			return "";
		}
	}


	/**
	 *  Gets the Whats New date for the multi-record (if it exists) as a Date, which is the whats new date for
	 *  this resource across all records that catalog it. Note that this is the appropriate method to use when
	 *  displaying this value to an end user, instead of {@link #getWhatsNewDateDate()}.
	 *
	 * @return    The What's New date or null.
	 */
	public Date getMultiWhatsNewDateDate() {
		String t = multiDoc.get("wndate");

		if (t == null)
			return null;

		try {
			return new Date(DateFieldTools.stringToTime(t));
		} catch (Throwable e) {
			prtlnErr("Error getWhatsNewDateDate(): " + e);
			return null;
		}
	}


	/**
	 *  Gets the created date as a String. ADN XPath metaMetadata/dateInfo@created.
	 *
	 * @return    The created date or empty String.
	 */
	public String getCreationDate() {
		String t = doc.get("creationdate");

		if (t == null)
			return "";

		try {
			long modTime = DateFieldTools.stringToTime(t);
			SimpleDateFormat df = new SimpleDateFormat("MMM' 'dd', 'yyyy");
			return df.format(new Date(modTime));
		} catch (Throwable e) {
			prtlnErr("Error getCreationDate(): " + e);
			return "";
		}
	}


	/**
	 *  Gets the created date as a Java Date ADN XPath metaMetadata/dateInfo@created.
	 *
	 * @return    The created Date or null.
	 */
	public Date getCreationDateDate() {
		String t = doc.get("creationdate");

		if (t == null)
			return null;

		try {
			return new Date(DateFieldTools.stringToTime(t));
		} catch (Throwable e) {
			prtlnErr("Error getCreationDateDate(): " + e);
			return null;
		}
	}


	/**
	 *  Gets the accession date for this muti-doc, as a date String, which is the date the resource first
	 *  appeared in the library among all records that catalog it. Note that this is the appropriate method to
	 *  use when displaying this value to an end user, instead of {@link #getAccessionDate()}.
	 *
	 * @return    The accession date or empty String.
	 */
	public String getMultiAccessionDate() {
		String t = multiDoc.get("accessiondate");

		if (t == null)
			return "";

		try {
			long modTime = DateFieldTools.stringToTime(t);
			SimpleDateFormat df = new SimpleDateFormat("MMM' 'dd', 'yyyy");
			return df.format(new Date(modTime));
		} catch (Throwable e) {
			prtlnErr("Error getAccessionDate(): " + e);
			return "";
		}
	}


	/**
	 *  Gets the accession date for this multi-doc as a Java Date, which is the date the resource first appeared
	 *  in the library among all records that catalog it. Note that this is the appropriate method to use when
	 *  displaying this value to an end user, instead of {@link #getAccessionDateDate()}.
	 *
	 * @return    The accession Date or null.
	 */
	public Date getMultiAccessionDateDate() {
		String t = multiDoc.get("accessiondate");

		if (t == null)
			return null;

		try {
			return new Date(DateFieldTools.stringToTime(t));
		} catch (Throwable e) {
			prtlnErr("Error getAccessionDateDate(): " + e);
			return null;
		}
	}


	/**
	 *  Gets the accession date as a String. Note that the appropriate method to use when displaying this value
	 *  to an end user is {@link #getMultiAccessionDate()}.
	 *
	 * @return    The accession date or empty String.
	 */
	public String getAccessionDate() {
		String t = doc.get("accessiondate");

		if (t == null)
			return "";

		try {
			long modTime = DateFieldTools.stringToTime(t);
			SimpleDateFormat df = new SimpleDateFormat("MMM' 'dd', 'yyyy");
			return df.format(new Date(modTime));
		} catch (Throwable e) {
			prtlnErr("Error getAccessionDate(): " + e);
			return "";
		}
	}


	/**
	 *  Gets the accession date as a Java Date. Note that the appropriate method to use when displaying this
	 *  value to an end user is {@link #getMultiAccessionDateDate()}.
	 *
	 * @return    The accession Date or null.
	 */
	public Date getAccessionDateDate() {
		String t = doc.get("accessiondate");

		if (t == null)
			return null;

		try {
			return new Date(DateFieldTools.stringToTime(t));
		} catch (Throwable e) {
			prtlnErr("Error getAccessionDateDate(): " + e);
			return null;
		}
	}


	/**
	 *  Gets the AccessionStatus for this record.
	 *
	 * @return    The status value
	 */
	public String getAccessionStatus() {
		String t = doc.get("accessionstatus");

		if (t == null) {
			return DEFAULT;
		}
		else {
			return t;
		}
	}


	/**
	 *  Gets the the concatinated accession statuses of all associated records for this item.
	 *
	 * @return    The status value
	 */
	public String getMultiAccessionStatus() {
		String t = multiDoc.get("accessionstatus");

		if (t == null) {
			return DEFAULT;
		}
		else {
			return t;
		}
	}



	/**
	 *  Gets the beneficiary attribute of the ItemDocReader object
	 *
	 * @return    The beneficiary value
	 */
	public String[] getBeneficiary() {
		String t = doc.get("itemAudienceBeneficiary");

		if (t == null)
			return null;
		return IndexingTools.extractSeparatePhrasesFromString(t);
	}


	/**
	 *  Gets the toolFor attribute of the ItemDocReader object
	 *
	 * @return    The toolFor value
	 */
	public String[] getToolFor() {
		String t = doc.get("itemAudienceToolFor");

		if (t == null)
			return null;
		return IndexingTools.extractSeparatePhrasesFromString(t);
	}


	/**
	 *  Gets the instructionalGoal attribute of the ItemDocReader object
	 *
	 * @return    The instructionalGoal value
	 */
	public String[] getInstructionalGoal() {
		String t = doc.get("itemAudienceInstructionalGoal");

		if (t == null)
			return null;
		return IndexingTools.extractSeparatePhrasesFromString(t);
	}


	/**
	 *  Gets the teachingMethod attribute of the ItemDocReader object
	 *
	 * @return    The teachingMethod value
	 */
	public String[] getTeachingMethod() {
		String t = doc.get("itemAudienceTeachingMethod");

		if (t == null)
			return null;
		return IndexingTools.extractSeparatePhrasesFromString(t);
	}


	/**
	 *  Gets the typicalAgeRange attribute of the ItemDocReader object
	 *
	 * @return    The typicalAgeRange value
	 */
	public String[] getTypicalAgeRange() {
		String t = doc.get("itemAudienceTypicalAgeRange");

		if (t == null)
			return null;
		return IndexingTools.extractSeparatePhrasesFromString(t);
	}


	/**
	 *  Gets the grade ranges for this record, for example 07, or 'DLESE:Primary elementary' if no vocab manager
	 *  is available.
	 *
	 * @return    The grade ranges
	 */
	public String[] getGradeRanges() {
		try {
			String t = doc.get(getFieldId("gradeRange", getDoctype()));
			if (t == null || t.length() == 0) {
				return null;
			}
			return t.trim().split("\\+");
		} catch (Throwable e) {
			e.printStackTrace();
			return null;
		}
	}


	/**
	 *  Gets the grade ranges for this record and all others that catalog the same resource, for example 07, or
	 *  'DLESE:Primary elementary' if no vocab manager is available.
	 *
	 * @return    The grade ranges as cataloged by all records for this resource
	 */
	public String[] getMultiGradeRanges() {
		if (multiDoc == null)
			return getGradeRanges();
		String t = multiDoc.get(getFieldId("gradeRange", getDoctype())).trim();
		String[] vals;
		if (t == null || t.length() == 0)
			return null;
		return t.split("\\+");
	}


	/**
	 *  Gets the grade range UI labels from the vocab manager for this record, for example 'Primary (K-2)', or
	 *  'DLESE:Primary elementary' if the vocab manager is not available. To specify the vocab interface to use,
	 *  first call {@link #setVocabInterface(String)}, otherwise the default will be used, which is
	 *  'dds.descr.en-us'.<p>
	 *
	 *  An example application using JSTL might look like:<br>
	 *  <code>
	 *  <pre>
	 *   &lt;%-- The following line is optional --%&gt;
	 *   &lt;c:set property=&quot;vocabInterface&quot; target=&quot;${docReader}&quot; value=&quot;dds.descr.en-us&quot;/&gt;
	 *   &lt;c:forEach items=&quot;${docReader.gradeRangeLabels}&quot; var=&quot;myVocabLabel&quot;&gt;
	 *        ${myVocabLabel}
	 *   &lt;/c:forEach&gt;
	 *  </pre> </code>
	 *
	 * @return    A Collection of grade range UI labels as cataloged for this record
	 */
	public Collection getGradeRangeLabels() {
		return getUiLabelsFromVocabIds("gr", getGradeRanges(), getDoctype());
	}


	/**
	 *  Gets the grade range UI labels from the vocab manager for this record and all others that catalog the
	 *  same resource, for example 'Primary (K-2)', or 'DLESE:Primary elementary' if the vocab manager is not
	 *  available. To specify the vocab interface to use, first call {@link #setVocabInterface(String)},
	 *  otherwise the default will be used, which is 'dds.descr.en-us'.<p>
	 *
	 *  An example application using JSTL might look like:<br>
	 *  <code>
	 *  <pre>
	 *   &lt;%-- The following line is optional --%&gt;
	 *   &lt;c:set property=&quot;vocabInterface&quot; target=&quot;${docReader}&quot; value=&quot;dds.descr.en-us&quot;/&gt;
	 *   &lt;c:forEach items=&quot;${docReader.multiGradeRangeLabels}&quot; var=&quot;myVocabLabel&quot;&gt;
	 *        ${myVocabLabel}
	 *   &lt;/c:forEach&gt;
	 *  </pre> </code>
	 *
	 * @return    A Collection of grade range UI labels as cataloged by all records for this resource
	 */
	public Collection getMultiGradeRangeLabels() {
		return getUiLabelsFromVocabIds("gr", getMultiGradeRanges(), getDoctype());
	}


	/**
	 *  Gets the subjects for this record, for example 03, or 'DLESE:Biology' if no vocab manager is available.
	 *
	 * @return    The subjects
	 */
	public String[] getSubjects() {
		String t = doc.get(getFieldId("subject", getDoctype()));
		if (t == null || t.length() == 0) {
			return null;
		}
		return t.trim().split("\\+");
	}


	/**
	 *  Gets the subjects for this item and all additional items associated via the ID mapper. Duplicats are
	 *  ommitted.
	 *
	 * @return    The subjects value
	 */
	public String[] getMultiSubjects() {
		if (multiDoc == null) {
			return getSubjects();
		}
		String t = multiDoc.get(getFieldId("subject", getDoctype()));
		if (t == null || t.length() == 0) {
			return null;
		}
		return t.trim().split("\\+");
	}


	/**
	 *  Gets the subject UI labels from the vocab manager for this record, for example 'Biology', or
	 *  'DLESE:Biology' if the vocab manager is not available, sorted alphabetically. To specify the vocab
	 *  interface to use, first call {@link #setVocabInterface(String)}, otherwise the default will be used,
	 *  which is 'dds.descr.en-us'.<p>
	 *
	 *  An example application using JSTL might look like:<br>
	 *  <code>
	 *  <pre>
	 *   &lt;%-- The following line is optional --%&gt;
	 *   &lt;c:set property=&quot;vocabInterface&quot; target=&quot;${docReader}&quot; value=&quot;dds.descr.en-us&quot;/&gt;
	 *   &lt;c:forEach items=&quot;${docReader.subjectLabels}&quot; var=&quot;myVocabLabel&quot;&gt;
	 *        ${myVocabLabel}
	 *   &lt;/c:forEach&gt;
	 *  </pre> </code>
	 *
	 * @return    An array of subject UI label strings as cataloged for this record, sorted alphabetically
	 */
	public Object[] getSubjectLabels() {
		Collection collection = getUiLabelsFromVocabIds("su", getSubjects(), getDoctype());
		if (collection == null)
			return null;
		Object[] array = collection.toArray();
		Arrays.sort(array);
		return array;
	}


	/**
	 *  Gets the subject UI labels from the vocab manager for this record and all others that catalog the same
	 *  resource, for example 'Biology', or 'DLESE:Biology' if the vocab manager is not available, sorted
	 *  alphabetically. To specify the vocab interface to use, first call {@link #setVocabInterface(String)},
	 *  otherwise the default will be used, which is 'dds.descr.en-us'.<p>
	 *
	 *  An example application using JSTL might look like:<br>
	 *  <code>
	 *  <pre>
	 *   &lt;%-- The following line is optional --%&gt;
	 *   &lt;c:set property=&quot;vocabInterface&quot; target=&quot;${docReader}&quot; value=&quot;dds.descr.en-us&quot;/&gt;
	 *   &lt;c:forEach items=&quot;${docReader.multiSubjectLabels}&quot; var=&quot;myVocabLabel&quot;&gt;
	 *        ${myVocabLabel}
	 *   &lt;/c:forEach&gt;
	 *  </pre> </code>
	 *
	 * @return    An array of subject UI label Strings as cataloged by all records for this resource, sorted
	 *      alphabetically
	 */
	public Object[] getMultiSubjectLabels() {
		Collection collection = getUiLabelsFromVocabIds("su", getMultiSubjects(), getDoctype());
		if (collection == null)
			return null;
		Object[] array = collection.toArray();
		Arrays.sort(array);
		return array;
	}


	/**
	 *  Gets the content standards for this record, for example 01, or 'NSES:K-4:Unifying Concepts and Processes
	 *  Standards:Change, constancy, and measurement' if no vocab manager is available.
	 *
	 * @return    The content standards
	 */
	public String[] getContentStandards() {
		String t = doc.get(getFieldId("contentStandard", getDoctype()));
		if (t == null || t.length() == 0) {
			return null;
		}
		return t.trim().split("\\+");
	}


	/**
	 *  Gets the content standards for this record and all others that catalog the same resource, for example 01,
	 *  or 'NSES:K-4:Unifying Concepts and Processes Standards:Change, constancy, and measurement' if no vocab
	 *  manager is available.
	 *
	 * @return    The content standards
	 */
	public String[] getMultiContentStandards() {
		if (multiDoc == null) {
			return getContentStandards();
		}
		String t = multiDoc.get(getFieldId("contentStandard", getDoctype()));
		if (t == null || t.length() == 0) {
			return null;
		}
		return t.trim().split("\\+");
	}


	/**
	 *  Gets the content standard UI labels from the vocab manager for this record, for example 'Change,
	 *  constancy, and measurement', or 'NSES:K-4:Unifying Concepts and Processes Standards:Change, constancy,
	 *  and measurement' if the vocab manager is not available. To specify the vocab interface to use, first call
	 *  {@link #setVocabInterface(String)}, otherwise the default will be used, which is 'dds.descr.en-us'.<p>
	 *
	 *  An example application using JSTL might look like:<br>
	 *  <code>
	 *  <pre>
	 *   &lt;%-- The following line is optional --%&gt;
	 *   &lt;c:set property=&quot;vocabInterface&quot; target=&quot;${docReader}&quot; value=&quot;dds.descr.en-us&quot;/&gt;
	 *   &lt;c:forEach items=&quot;${docReader.contentStandardLabels}&quot; var=&quot;myVocabLabel&quot;&gt;
	 *        ${myVocabLabel}
	 *   &lt;/c:forEach&gt;
	 *  </pre> </code>
	 *
	 * @return    A Collection of content standard UI labels as cataloged for this record
	 */
	public Collection getContentStandardLabels() {
		return getUiLabelsFromVocabIds("cs", getContentStandards(), getDoctype());
	}


	/**
	 *  Gets the content standard UI labels from the vocab manager for this record and all others that catalog
	 *  this resource, for example 'Change, constancy, and measurement', or 'NSES:K-4:Unifying Concepts and
	 *  Processes Standards:Change, constancy, and measurement' if the vocab manager is not available. To specify
	 *  the vocab interface to use, first call {@link #setVocabInterface(String)}, otherwise the default will be
	 *  used, which is 'dds.descr.en-us'.<p>
	 *
	 *  An example application using JSTL might look like:<br>
	 *  <code>
	 *  <pre>
	 *   &lt;%-- The following line is optional --%&gt;
	 *   &lt;c:set property=&quot;vocabInterface&quot; target=&quot;${docReader}&quot; value=&quot;dds.descr.en-us&quot;/&gt;
	 *   &lt;c:forEach items=&quot;${docReader.multiContentStandardLabels}&quot; var=&quot;myVocabLabel&quot;&gt;
	 *        ${myVocabLabel}
	 *   &lt;/c:forEach&gt;
	 *  </pre> </code>
	 *
	 * @return    A Collection of content standard UI labels as cataloged by all records for this resource
	 */
	public Collection getMultiContentStandardLabels() {
		return getUiLabelsFromVocabIds("cs", getMultiContentStandards(), getDoctype());
	}


	/**
	 *  Gets the resource types for this record, for example 0c, or 'DLESE:Learning materials:Classroom activity'
	 *  if no vocab manager is available.
	 *
	 * @return    The resource types
	 */
	public String[] getResourceTypes() {
		String t = doc.get(getFieldId("resourceType", getDoctype()));
		if (t == null || t.length() == 0) {
			return null;
		}
		return t.trim().split("\\+");
	}


	/**
	 *  Gets the resource types for this record and all others that catalog the same resource, for example 0c, or
	 *  'DLESE:Learning materials:Classroom activity' if no vocab manager is available.
	 *
	 * @return    The resource types
	 */
	public String[] getMultiResourceTypes() {
		if (multiDoc == null) {
			return getResourceTypes();
		}
		String t = multiDoc.get(getFieldId("resourceType", getDoctype()));
		if (t == null || t.length() == 0) {
			return null;
		}
		return t.trim().split("\\+");
	}


	/**
	 *  Gets the resource type UI labels from the vocab manager for this record, for example 'Classroom
	 *  activity', or 'DLESE:Learning materials:Classroom activity' if the vocab manager is not available, sorted
	 *  alphabetically. To specify the vocab interface to use, first call {@link #setVocabInterface(String)},
	 *  otherwise the default will be used, which is 'dds.descr.en-us'.<p>
	 *
	 *  An example application using JSTL might look like:<br>
	 *  <code>
	 *  <pre>
	 *   &lt;%-- The following line is optional --%&gt;
	 *   &lt;c:set property=&quot;vocabInterface&quot; target=&quot;${docReader}&quot; value=&quot;dds.descr.en-us&quot;/&gt;
	 *   &lt;c:forEach items=&quot;${docReader.resourceTypeLabels}&quot; var=&quot;myVocabLabel&quot;&gt;
	 *        ${myVocabLabel}
	 *   &lt;/c:forEach&gt;
	 *  </pre></code>
	 *
	 * @return    An array of resource type UI label Strings as cataloged by this record, sorted alphabetically.
	 */
	public Object[] getResourceTypeLabels() {
		Collection collection = getUiLabelsFromVocabIds("re", getResourceTypes(), getDoctype());
		if (collection == null)
			return null;
		Object[] array = collection.toArray();
		Arrays.sort(array);
		return array;
	}


	/**
	 *  Gets the resource type UI labels from the vocab manager for this record and all others that catalog the
	 *  same resource, for example 'Classroom activity', or 'DLESE:Learning materials:Classroom activity' if the
	 *  vocab manager is not available, sorted alphabetically. To specify the vocab interface to use, first call
	 *  {@link #setVocabInterface(String)}, otherwise the default will be used, which is 'dds.descr.en-us'.<p>
	 *
	 *  An example application using JSTL might look like:<br>
	 *  <code>
	 *  <pre>
	 *   &lt;%-- The following line is optional --%&gt;
	 *   &lt;c:set property=&quot;vocabInterface&quot; target=&quot;${docReader}&quot; value=&quot;dds.descr.en-us&quot;/&gt;
	 *   &lt;c:forEach items=&quot;${docReader.multiResourceTypeLabels}&quot; var=&quot;myVocabLabel&quot;&gt;
	 *        ${myVocabLabel}
	 *   &lt;/c:forEach&gt;
	 *  </pre> </code>
	 *
	 * @return    An array of resource type UI label Strings as cataloged by all records for this resource,
	 *      sorted alphabetically
	 */
	public Object[] getMultiResourceTypeLabels() {
		Collection collection = getUiLabelsFromVocabIds("re", getMultiResourceTypes(), getDoctype());
		if (collection == null)
			return null;
		Object[] array = collection.toArray();
		Arrays.sort(array);
		return array;
	}


	/**
	 *  Gets the value 'true' or 'false' depending on whether this item does or does not have multiple records
	 *  associated with it.
	 *
	 * @return    'true' if there are multiple records, 'false' if only one.
	 */
	public String getMultiRecordStatus() {
		if (isMultiRecord())
			return "true";
		else
			return "false";
	}


	/**
	 *  Determines whether this item does or does not have multiple records associated with it.
	 *
	 * @return    True if there are multiple records, false if only one.
	 */
	public boolean isMultiRecord() {
		return multiRecordStatus;
	}


	/**
	 *  Determines whether this item has annotations.
	 *
	 * @return    True if annotations are present, false otherwise.
	 */
	public boolean hasAnnotations() {
		String t = multiDoc.get("itemannotypes");
		return (t != null && t.trim().length() != 0);
	}


	/**
	 *  Gets the anno types that are associated with this record.
	 *
	 * @return    The anno types value.
	 */
	public String[] getAnnoTypes() {
		String t = multiDoc.get("itemannotypes");
		if (t == null || t.length() == 0) {
			return null;
		}
		else {
			String[] tmp = t.split("\\s+");
			for (int i = 0; i < tmp.length; i++) {
				tmp[i] = tmp[i].replaceAll("\\+", " ");
			}
			return tmp;
		}
	}


	/**
	 *  Gets the anno pathways that are associated with this record.
	 *
	 * @return    The list of anno pathway Strings
	 */
	public ArrayList getAnnoPathways() {
		String t = multiDoc.get("itemannopathways");
		//System.out.println("getAnnoPathways() String: " + t );
		if (t == null || t.length() == 0) {
			return null;
		}
		else {
			String[] tmp = t.split("\\s+");
			ArrayList list = new ArrayList(tmp.length);
			for (int i = 0; i < tmp.length; i++)
				list.add(tmp[i].replaceAll("\\+", " "));
			return list;
		}
	}


	/**
	 *  Gets the annotation statuses that are associated with this item.
	 *
	 * @return    A list of anno status Strings
	 */
	public ArrayList getAnnoStatus() {
		String t = multiDoc.get("itemannostatus");
		//System.out.println("getAnnoStatus() String: " + t );
		if (t == null || t.trim().length() == 0) {
			return null;
		}
		else {
			String[] tmp = t.split("\\s+");
			ArrayList list = new ArrayList(tmp.length);
			for (int i = 0; i < tmp.length; i++)
				list.add(tmp[i].replaceAll("\\+", " "));
			return list;
		}
	}


	/**
	 *  Gets the annotation formats that are associated with this item.
	 *
	 * @return    A list of anno formats Strings
	 */
	public ArrayList getAnnoFormats() {
		String t = multiDoc.get("itemannoformats");
		if (t == null || t.trim().length() == 0) {
			return null;
		}
		else {
			String[] tmp = t.split("\\s+");
			ArrayList list = new ArrayList(tmp.length);
			for (int i = 0; i < tmp.length; i++)
				list.add(tmp[i].replaceAll("\\+", " "));
			return list;
		}
	}


	/**
	 *  Gets a String array of all annotation star ratings for this item in numerical form from 1 to 5, or null
	 *  if none. Each rating represents a single annotation's star rating for this item.
	 *
	 * @return    An String array of numbers 1, 2, 3, 4, 5, or null
	 */
	public String[] getAnnoRatings() {
		String t = multiDoc.get("itemannoratingvalues");
		//System.out.println("getAnnoRatings() String: " + t );
		if (t == null || t.trim().length() == 0)
			return null;
		else
			return t.split("\\s+");
	}


	/**
	 *  Gets a the average of all star ratings for this item as a String, or null if none. The rating is shown to
	 *  three decimal points, for example '4.333' or '3.000'.
	 *
	 * @return    The average star rating to three decimal points as a String
	 */
	public String getAverageAnnoRating() {
		String t = multiDoc.get("itemannoaveragerating");
		if (t == null || t.trim().length() == 0)
			return null;
		else
			return t;
	}


	/**
	 *  Gets a the average of all star ratings for this item as a float, or -1 if none.
	 *
	 * @return    The average star rating or -1 if none
	 */
	public float getAverageAnnoRatingFloat() {
		try {
			String t = getAverageAnnoRating();
			if (t != null)
				return Float.parseFloat(t);
		} catch (Throwable t) {
			prtlnErr("Error in getAverageAnnoRatingFloat(): " + t);
		}
		return -1;
	}


	/**
	 *  Gets a the total number of star ratings for this item as a String. The number is displayed to five
	 *  digits, for example '00002' or '00000'.
	 *
	 * @return    The number of star ratings as a String
	 */
	public String getNumAnnoRatings() {
		String t = multiDoc.get("itemannonumratings");
		if (t == null || t.trim().length() == 0)
			return null;
		else
			return t;
	}


	/**
	 *  Gets a the total number of star ratings for this item as a int.
	 *
	 * @return    The number of star ratings for this item
	 */
	public int getNumAnnoRatingsInt() {
		try {
			String t = getNumAnnoRatings();
			if (t != null)
				return Integer.parseInt(t);
		} catch (Throwable t) {
			prtlnErr("Error in getNumAnnoRatingsInt(): " + t);
		}
		return 0;
	}


	/**
	 *  Gets the annotataion collection keys, for example {06, 08}, for all collections that annotate this item.
	 *
	 * @return    The annoCollectionKeys value
	 */
	public String[] getAnnoCollectionKeys() {
		String t = multiDoc.get("itemannocollectionkeys");
		//System.out.println("getAnnoCollectionKeys() String: " + t );
		if (t == null || t.trim().length() == 0) {
			return null;
		}
		else {
			return t.split("\\+");
		}
	}


	/**
	 *  Gets the annotataion collection keys, for example {06, 08}, for all collections that annotate this item
	 *  with one or more status completed annotations.
	 *
	 * @return    The completedAnnoCollectionKeys value
	 */
	public String[] getCompletedAnnoCollectionKeys() {
		String t = multiDoc.get("itemannocompletedcollectionkeys");
		//System.out.println("getAnnoCollectionKeys() String: " + t );
		if (t == null || t.trim().length() == 0) {
			return null;
		}
		else {
			return t.split("\\+");
		}
	}


	/**
	 *  Gets the collection keys for all enabled collections that are associated with this resource. Associated
	 *  annotations such as CRS and JESSE that are identified as collections are included. Disabled collections
	 *  are not included.
	 *
	 * @return    A list of associatedCollectionKeys Strings
	 */
	public ArrayList getAssociatedCollectionKeys() {
		String t = doc.get("associatedcollectionkeys");
		if (t == null || t.trim().length() == 0) {
			return null;
		}
		else {
			if (rm == null) {
				rm = (RepositoryManager) getIndex().getAttribute("repositoryManager");
			}
			HashMap enabledColls = rm.getEnabledSetsHashMap();
			String[] keys = t.split("\\+");
			ArrayList list = new ArrayList(keys.length);
			for (int i = 0; i < keys.length; i++) {
				if (enabledColls.containsKey(keys[i]))
					list.add(keys[i]);
			}
			return list;
		}
	}


	/**
	 *  Gets the collection keys for all collections that are associated with this resource including collections
	 *  that are not enabled.
	 *
	 * @return    The associatedCollectionKeys value
	 */
	public String[] getAllAssociatedCollectionKeys() {
		String t = doc.get("associatedcollectionkeys");
		//System.out.println("getAssociatedCollectionKeys() String: " + t );
		if (t == null || t.trim().length() == 0) {
			return null;
		}
		else {
			return t.split("\\+");
		}
	}


	/**
	 *  Gets the IDs of records that refer to the same resource, not including this record's ID.
	 *
	 * @return    The associated IDs value.
	 */
	public String[] getAssociatedIds() {
		String t = doc.get("associatedids");
		if (t == null || t.trim().length() == 0) {
			return null;
		}
		else {
			return t.split("\\s+");
		}
	}


	/**
	 *  Gets all the IDs associated with this resource, including this record's ID.
	 *
	 * @return    The allIds value
	 */
	public String[] getAllIds() {
		if (allIds == null)
			allIds = getId().split("\\s+");
		return allIds;
	}


	/**
	 *  Gets the errors types (codes) identified by the ID mapper for this records. Returns integers as strings.
	 *
	 * @return    The ID mapper error types (codes).
	 */
	public String[] getErrorTypes() {
		String t = doc.get("idmaperrors");
		if (t == null || t.trim().length() == 0 || t.equals("noerrors")) {
			return null;
		}
		else {
			return t.split("\\s+");
		}
	}


	/**
	 *  Gets the errors identified by the ID mapper for this records.
	 *
	 * @return    The ID mapper errors.
	 */
	public String[] getErrorStrings() {
		String[] types = getErrorTypes();
		if (types == null || types.length == 0) {
			return null;
		}
		String[] strings = new String[types.length];
		for (int i = 0; i < types.length; i++) {
			strings[i] = DpcErrors.getMessage(Integer.parseInt(types[i]));
		}
		return strings;
	}


	/**
	 *  Gets the ItemDocReaders for all records that refer to this resource. The first record is this item. This
	 *  returns the full ResultDoc for each item, not the de-duped ResultDocs.
	 *
	 * @return    All records that refer to this resource.
	 * @link      #getDeDupedResultDocs.
	 */
	public ResultDocList getAllItemResultDocs() {
		if (allItemsInitialized) {
			return allItemResultDocs;
		}
		allItemsInitialized = true;

		if (recordDataService == null) {
			return null;
		}

		String[] associatedIds = getAssociatedIds();
		String[] allIds;

		if (associatedIds == null || associatedIds.length == 0) {
			allIds = new String[]{getId()};
		}
		else {
			allIds = new String[associatedIds.length + 1];
			allIds[0] = getId();
			for (int i = 0; i < associatedIds.length; i++) {
				allIds[i + 1] = associatedIds[i];
			}
		}
		allItemResultDocs = recordDataService.getItemResultDocs(allIds);

		//for(int i = 0; i < allItemResultDocs.length; i++)
		//prtln("allItemResultDocs contains id: " + ((ItemDocReader)allItemResultDocs[i].getDocReader()).getId());

		return allItemResultDocs;
	}



	/**
	 *  Gets the de-duped ResultDocs for all records that refer to this resource from the dup items index, or
	 *  null if this is not a duped record. These are sorted if a sort was specified in the ResultDoc attributes
	 *  via key 'sortAscendingByField' or 'sortDescendingByField'. See also {@link #getAllItemResultDocs}.
	 *
	 * @return    All de-duped ResultDocs for this resource.
	 * @see       #getAllItemResultDocs
	 */
	public ResultDocList getDeDupedResultDocs() {
		if (deDupedResultDocs == null)
			deDupedResultDocs = doGetDeDupedResultDocs(false, true);

		return deDupedResultDocs;
	}


	/**
	 *  Gets the de-duped ResultDocs for all records that refer to this resource, including this record, from the
	 *  dup items index, or null if this is not a duped record. See also {@link #getAllItemResultDocs}.
	 *
	 * @param  useFilter  True to use the filter, if one exists in the conf
	 * @param  useSort    True to use sorting, if indicated in the conf
	 * @return            All de-duped ResultDocs for this resource.
	 */
	private ResultDocList doGetDeDupedResultDocs(boolean useFilter, boolean useSort) {
		if (!isMultiRecord())
			return null;

		// Grab the IDs for this multi-record
		String[] allIds = getAllIds();
		String idQuery = "(id:" + SimpleLuceneIndex.encodeToTerm(allIds[0]);
		for (int i = 1; i < allIds.length; i++)
			idQuery += " OR id:" + SimpleLuceneIndex.encodeToTerm(allIds[i]);
		idQuery += ")";

		SimpleLuceneIndex dupItemsIndex =
			(SimpleLuceneIndex) conf.index.getAttribute("dupItemsIndex");

		org.apache.lucene.search.Filter filter = null;
		if (useFilter)
			filter = conf.filter;

		ResultDocList myDocs = null;
		Sort sort = null;

		String sortBy = "none";

		// If sorting has been requested, sort
		if (useSort && conf.attributes != null) {
			String sortAscendingByField = (String) conf.attributes.get("sortAscendingByField");
			String sortDescendingByField = (String) conf.attributes.get("sortDescendingByField");

			// Special case: since accessiondate is sorted ascending for it's multi-doc value, it must also be here regardless
			// E.g. the value in the multi-doc is the value sorted ascending
			if (sortDescendingByField != null && sortDescendingByField.equalsIgnoreCase("accessiondate")) {
				sortAscendingByField = sortDescendingByField;
				sortDescendingByField = null;
			}

			if (sortAscendingByField != null || sortDescendingByField != null) {
				if (sortAscendingByField != null) {
					sortBy = "sortAscendingByField:" + sortAscendingByField;
					//Collections.sort(myDocs, new LuceneFieldComparator(sortAscendingByField, LuceneFieldComparator.ASCENDING));
					sort = new Sort( new SortField(sortAscendingByField, SortField.STRING, false) );
				}
				else {
					sortBy = "sortDescendingByField:" + sortDescendingByField;
					//Collections.sort(myDocs, new LuceneFieldComparator(sortDescendingByField, LuceneFieldComparator.DESCENDING));
					sort = new Sort( new SortField(sortDescendingByField, SortField.STRING, true) );
				}
			}
		}
		
		myDocs = dupItemsIndex.searchDocs(idQuery, (String)null, filter, sort);

		printQuery("\ndoGetDeDupedResultDocs() query: '" + idQuery + "' filter: '" + filter + " " + sortBy + " had " + myDocs.size() + " results.\n");

		// If no filter was used, go ahead and set the deDupedDocs
		if (filter == null)
			deDupedResultDocs = myDocs;
		
		return myDocs;
	}


	/**
	 *  Gets the ItemDocReaders for all associated items for this item (refer to same resource).
	 *
	 * @return    The associatedItemDocReaders value
	 */
	public ResultDocList getAssociatedItemResultDocs() {
		if (associatedItemsInitialized) {
			return associatedItemResultDocs;
		}
		associatedItemsInitialized = true;

		if (recordDataService == null) {
			return null;
		}

		String[] associatedIds = getAssociatedIds();
		if (associatedIds == null || associatedIds.length == 0) {
			return null;
		}

		associatedItemResultDocs = recordDataService.getItemResultDocs(associatedIds);

		// Check to see if some of the associated IDs arn't in the index:
		try {
			if (associatedItemResultDocs == null) {
				setMissingAssociatedItemIds(new ArrayList(Arrays.asList(associatedIds)));
			}
			else if (associatedIds.length > associatedItemResultDocs.size()) {
				ArrayList missingItems = new ArrayList(Arrays.asList(associatedIds));
				for (int j = 0; j < associatedItemResultDocs.size(); j++) {
					int loc = missingItems.indexOf(((ItemDocReader) associatedItemResultDocs.get(j).getDocReader()).getId());
					if (loc >= 0) {
						missingItems.remove(loc);
					}
				}
				if (missingItems.size() > 0) {
					setMissingAssociatedItemIds(missingItems);
				}
			}
		} catch (Throwable e) {
			prtlnErr("getAssociatedItemDocReaders error: " + e);
			e.printStackTrace();
		}

		return associatedItemResultDocs;
	}


	/**
	 *  Gets the ItemDocReaders for all associated items (refer to same resource) for this item that have an
	 *  appropriate status for display in discovery.
	 *
	 * @return    The displayableAssociatedItemDocReaders value
	 */
	public ResultDocList getDisplayableAssociatedItemResultDocs() {
		if (displayableAssociatedItemsInitialized) {
			return displayableAssociatedItemResultDocs;
		}
		displayableAssociatedItemsInitialized = true;

		if (recordDataService == null) {
			return null;
		}

		String[] associatedIds = getAssociatedIds();
		if (associatedIds == null || associatedIds.length == 0) {
			return null;
		}

		//prtln("getDisplayableAssociatedItemResultDocs() index query");

		displayableAssociatedItemResultDocs =
			recordDataService.getDisplayableItemResultDocs(associatedIds);

		return displayableAssociatedItemResultDocs;
	}



	/**
	 *  Gets the missingAssociatedItemIds for associated items that are not in the index.
	 *
	 * @return    A list of missingAssociatedItemIds Strings
	 */
	public ArrayList getMissingAssociatedItemIds() {
		// Initialize the ArrayList, then return
		getAssociatedItemResultDocs();
		return missingAssociatedItemIds;
	}


	private void setMissingAssociatedItemIds(ArrayList missingAssociatedItemIds) {
		this.missingAssociatedItemIds = missingAssociatedItemIds;
	}


	/**
	 *  Determines whether this item has one or more related resources of any type. If the related resource is an
	 *  ID, checks to see that the ID is currently available in the repository.
	 *
	 * @return    True if this item has one or more related resources of any type, else false.
	 */
	public boolean hasRelatedResource() {
		return doHasRelatedResource(doc);
	}


	/**
	 *  Determines whether this item has one or more related resources of any type from any records that catalogs
	 *  this resource. If the related resource is an ID, checks to see that the ID is currently available in the
	 *  repository.
	 *
	 * @return    True if this item has one or more related resources of any type, else false.
	 */
	public boolean hasMultiRelatedResource() {
		return doHasRelatedResource(multiDoc);
	}


	private boolean doHasRelatedResource(Document theDoc) {
		String t = theDoc.get("itemhasrelatedresource");
		if (t == null || t.equals("false"))
			return false;
		else {
			try {

				String[] urls = null;

				if (theDoc == multiDoc)
					urls = getMultiRelatedResourceUrls();
				else
					urls = getRelatedResourceUrls();

				// If we have a related resource by URL, return true
				if (urls != null && urls.length > 0)
					return true;

				ResultDocList results = null;
				if (theDoc == multiDoc)
					results = getMultiRelatedResourceByIdDocs();
				else
					results = getRelatedResourceByIdDocs();

				if (results == null || results.size() == 0)
					return false;
			} catch (Throwable e) {
				e.printStackTrace();
				return false;
			}

			return true;
		}
	}


	/**
	 *  Determines whether this item has one or more related resources of any type.
	 *
	 * @return    'true' if this item has one or more related resources of any type, else 'false'.
	 */
	public String getHasRelatedResource() {
		if (hasRelatedResource())
			return "true";
		else
			return "false";
	}


	/**
	 *  Determines whether this item has one or more related resources of any type from any records that catalogs
	 *  this resource.
	 *
	 * @return    'true' if this item has one or more related resources of any type, else 'false'.
	 */
	public String getMultiHasRelatedResource() {
		if (hasMultiRelatedResource())
			return "true";
		else
			return "false";
	}


	/**
	 *  Gets the IDs of all related resources that were cataloged by ID, or null if none were present
	 *
	 * @return    The relatedResourceIds
	 */
	public String[] getRelatedResourceIds() {
		if (relatedResourceIds == null)
			relatedResourceIds = IndexingTools.extractSeparatePhrasesFromString(doc.get("itemrelatedresourceids"));
		return relatedResourceIds;
	}



	/**
	 *  Gets the IDs of all related resources that were cataloged by ID from all records that catalog this
	 *  resource, or null if none were present.
	 *
	 * @return    The relatedResourceIds
	 */
	public String[] getMultiRelatedResourceIds() {
		if (relatedResourceIds == null)
			relatedResourceIds = IndexingTools.extractSeparatePhrasesFromString(multiDoc.get("itemrelatedresourceids"));
		return relatedResourceIds;
	}


	/**
	 *  Gets the relatedResourceByIdDocs attribute of the ItemDocReader object
	 *
	 * @return    The relatedResourceByIdDocs value
	 */
	public ResultDocList getRelatedResourceByIdDocs() {
		return doGetRelatedResourceByIdDocs(getRelatedResourceIds());
	}


	/**
	 *  Gets the multiRelatedResourceByIdDocs attribute of the ItemDocReader object
	 *
	 * @return    The multiRelatedResourceByIdDocs value
	 */
	public ResultDocList getMultiRelatedResourceByIdDocs() {
		return doGetRelatedResourceByIdDocs(getMultiRelatedResourceIds());
	}


	private final ResultDocList doGetRelatedResourceByIdDocs(String[] ids) {
		// If the related resource is not an ID, return true
		if (ids != null && ids.length > 0) {
			if (rm == null)
				rm = (RepositoryManager) getIndex().getAttribute("repositoryManager");

			String query = "(id:" + SimpleLuceneIndex.encodeToTerm(ids[0]);
			for (int i = 1; i < ids.length; i++)
				query += " OR id:" + SimpleLuceneIndex.encodeToTerm(ids[i]);
			query += ") AND " + rm.getDiscoverableItemsQuery();

			return rm.getIndex().searchDocs(query);
			//System.out.println("hasRelatedResource() query for " + query + " had " + (resultDocs == null? 0 : resultDocs.length) + " results");
		}
		else
			return null;
	}


	private List multiRelatedResources = null;
	private List multiDisplayableRelatedResources = null;


	/**
	 *  Gets a List of RelatedResource Objects for each related resource from each of the records that catalog
	 *  this resource, or null if none.
	 *
	 * @return    A List of RelatedResource Objects, or null
	 */
	public List getMultiRelatedResources() {
		if (multiRelatedResources == null && hasMultiRelatedResource())
			multiRelatedResources = doGetMultiRelatedResources(false);

		return multiRelatedResources;
	}


	/**
	 *  Gets a List of RelatedResource Objects for each related resource from each of the records that catalog
	 *  this resource that have an appropriate status for display in discovery, or null if none.
	 *
	 * @return    A List of RelatedResource Objects, or null
	 */
	public List getMultiDisplayableRelatedResources() {
		if (multiDisplayableRelatedResources == null && hasMultiRelatedResource())
			multiDisplayableRelatedResources = doGetMultiRelatedResources(true);

		return multiDisplayableRelatedResources;
	}


	private List doGetMultiRelatedResources(boolean displayable) {

		ArrayList items = new ArrayList();
		items.add(this);

		ResultDocList results = null;
		if (displayable)
			results = getDisplayableAssociatedItemResultDocs();
		else
			results = getAssociatedItemResultDocs();
		if (results != null) {
			for (int i = 0; i < results.size(); i++)
				items.add(results.get(i).getDocReader());
		}

		//prtln("getMultiRelatedResources(): items size: " + items.size() );

		ArrayList theRelatedResources = new ArrayList();

		for (int i = 0; i < items.size(); i++) {
			ItemDocReader xmlDocReader = (ItemDocReader) items.get(i);
			String specifiedById = xmlDocReader.getId();
			List relationElms = xmlDocReader.getXmlDoc().selectNodes("//*[local-name()='itemRecord']/*[local-name()='relations']//*[local-name()='relation']/*");
			if (relationElms != null) {
				for (int j = 0; j < relationElms.size(); j++) {
					try {
						Element relationElm = (Element) relationElms.get(j);
						String id = null;
						String title = null;
						String url = null;
						String kind = relationElm.valueOf("@kind");
						// Handle idEntry
						if (relationElm.getName().equals("idEntry")) {
							id = relationElm.valueOf("@entry");
							if(id == null || id.trim().length() == 0)
								continue;
							
							ResultDoc relatedDoc = null;
							if (displayable)
								relatedDoc = recordDataService.getDisplayableItemResultDoc(id);
							else
								relatedDoc = recordDataService.getItemResultDoc(id);
							if (relatedDoc != null) {
								ItemDocReader itemDocReader = (ItemDocReader) relatedDoc.getDocReader();
								title = itemDocReader.getTitle();
								url = itemDocReader.getUrl();
							}
							else
								continue;
						}
						// Handle urlEntry
						else {
							title = relationElm.valueOf("@title");
							url = relationElm.valueOf("@url");
						}
						RelatedResource relatedResource = new RelatedResource(id, title, url, kind, specifiedById);
						//prtln(relatedResource.toString());
						if (!theRelatedResources.contains(relatedResource))
							theRelatedResources.add(relatedResource);
					} catch (Throwable t) {
						prtlnErr("error getting related resource: " + t);
					}
				}
			}
		}
		return theRelatedResources;
	}


	/**
	 *  Data for a related resource.
	 *
	 * @author     John Weatherley
	 */
	public class RelatedResource {
		private String _id = null;
		private String _title = null;
		private String _url = null;
		private String _kind = null;
		private String _specifiedById = null;


		/**
		 *  String representation
		 *
		 * @return    String representation
		 */
		public String toString() {
			return "RelatedResource: id:'" + _id + "' title:'" + _title + "' url:'" + _url + "' kind:'" + _kind + "' specified by:'" + _specifiedById + "'";
		}


		/**
		 *  True if the two RelatedResource URLs and kind are the same.
		 *
		 * @param  o  A RelatedResource
		 * @return    True if the two RelatedResource URLs and kind are the same
		 */
		public boolean equals(Object o) {
			try {
				RelatedResource rr = (RelatedResource) o;
				if (rr.getUrl() != null && _url != null && rr.getKind() != null && _kind != null)
					return (rr.getUrl().equals(_url) && rr.getKind().equals(_kind));
			} catch (Throwable t) {
				return false;
			}
			return false;
		}


		/**
		 * @param  id             The ID
		 * @param  title          The title
		 * @param  url            The url
		 * @param  kind           The kind
		 * @param  specifiedById  The ID of the record that specified this related resource
		 */
		public RelatedResource(
		                       String id,
		                       String title,
		                       String url,
		                       String kind,
		                       String specifiedById) {
			_id = id;
			_title = title;
			_url = url;
			_kind = kind;
			_specifiedById = specifiedById;
		}


		/**
		 *  Gets the id attribute of the RelatedResource object
		 *
		 * @return    The id value
		 */
		public String getId() {
			return _id;
		}


		/**
		 *  Gets the title attribute of the RelatedResource object
		 *
		 * @return    The title value
		 */
		public String getTitle() {
			return _title;
		}


		/**
		 *  Gets the url attribute of the RelatedResource object
		 *
		 * @return    The url value
		 */
		public String getUrl() {
			return _url;
		}


		/**
		 *  Gets the relation attribute of the RelatedResource object
		 *
		 * @return    The relation value
		 */
		public String getKind() {
			return _kind;
		}


		/**
		 *  Gets the specifiedById attribute of the RelatedResource object
		 *
		 * @return    The specifiedById value
		 */
		public String getSpecifiedById() {
			return _specifiedById;
		}
	}


	/**
	 *  Gets the URLs of all related resources that were cataloged by URL, or null if none were present
	 *
	 * @return    The relatedResourceUrls
	 */
	public String[] getRelatedResourceUrls() {
		if (relatedResourceUrls == null)
			relatedResourceUrls = IndexingTools.extractSeparatePhrasesFromString(doc.get("itemrelatedresourceurls"));
		return relatedResourceUrls;
	}


	/**
	 *  Gets the URLs of all related resources that were cataloged by URL from all records that catalog this
	 *  resource, or null if none were present
	 *
	 * @return    The relatedResourceUrls
	 */
	public String[] getMultiRelatedResourceUrls() {
		if (relatedResourceUrls == null)
			relatedResourceUrls = IndexingTools.extractSeparatePhrasesFromString(multiDoc.get("itemrelatedresourceurls"));
		return relatedResourceUrls;
	}

	// ---------------- Annotations display, etc. --------------------------

	/**
	 *  Gets the ResultDocs for all annotations that refer to this resource.
	 *
	 * @return    The ResultDocs value
	 */
	public ResultDocList getAnnotationResultDocs() {
		try {
			if (annotationDocReadersInitialized) {
				return annotationResultDocs;
			}
			annotationDocReadersInitialized = true;

			if (recordDataService == null || !hasAnnotations()) {
				return null;
			}

			String[] ids = getAllIds();
			ArrayList allIds = new ArrayList();
			for (int i = 0; i < ids.length; i++) {
				allIds.add(ids[i]);
			}

			ids = getAssociatedIds();
			if (ids != null) {
				for (int i = 0; i < ids.length; i++) {
					allIds.add(ids[i]);
				}
			}

			//prtln("getAnnotationResultDocs() index query");
			annotationResultDocs = recordDataService.getDleseAnnoResultDocs((String[]) allIds.toArray(new String[]{}));
			
			return annotationResultDocs;
		} catch (Throwable e) {
			e.printStackTrace();
			return null;
		}
	}


	/**
	 *  Determines whether this item has at least one completed annotation.
	 *
	 * @return    True if this item has one or more completed annotation, false otherwise.
	 */
	public boolean hasCompletedAnno() {
		initAnnosByType();
		return !completedAnnos.isEmpty();
	}


	/**
	 *  Gets the hasCompletedAnno attribute of the ItemDocReader object
	 *
	 * @return    The hasCompletedAnno value
	 */
	public String getHasCompletedAnno() {
		if (hasCompletedAnno()) {
			return "true";
		}
		else {
			return "false";
		}
	}


	/**
	 *  Gets the numCompletedAnnos attribute of the ItemDocReader object
	 *
	 * @return    The numCompletedAnnos value
	 */
	public String getNumCompletedAnnos() {
		initAnnosByType();
		return Integer.toString(numCompletedAnnos);
	}


	/**
	 *  Gets the numInProgressAnnos attribute of the ItemDocReader object
	 *
	 * @return    The numInProgressAnnos value
	 */
	public String getNumInProgressAnnos() {
		initAnnosByType();
		return Integer.toString(numInProgressAnnos);
	}


	/**
	 *  Gets the numTextAnnosInProgress attribute of the ItemDocReader object
	 *
	 * @return    The numTextAnnosInProgress value
	 */
	public String getNumTextAnnosInProgress() {
		initAnnosByType();
		return Integer.toString(numTextInProgress);
	}


	/**
	 *  Gets the numAudioAnnosInProgress attribute of the ItemDocReader object
	 *
	 * @return    The numAudioAnnosInProgress value
	 */
	public String getNumAudioAnnosInProgress() {
		initAnnosByType();
		return Integer.toString(numAudioInProgress);
	}


	/**
	 *  Gets the numGraphicalAnnosInProgress attribute of the ItemDocReader object
	 *
	 * @return    The numGraphicalAnnosInProgress value
	 */
	public String getNumGraphicalAnnosInProgress() {
		initAnnosByType();
		return Integer.toString(numGraphicalInProgress);
	}


	/**
	 *  Gets the number of video format annotations in progress for this item.
	 *
	 * @return    The number of video format annotations
	 */
	public String getNumVideoAnnosInProgress() {
		initAnnosByType();
		return Integer.toString(numVideoInProgress);
	}


	/**
	 *  Determines whether this item has a completed annotataion of the given type, for example 'Review',
	 *  'Comment', 'Educational standard', etc.
	 *
	 * @param  type  The annotation type
	 * @return       True if this item has a completed annotataion of the given type.
	 */
	public boolean hasCompletedAnnoOfType(String type) {
		initAnnosByType();
		return completedAnnosByType.containsKey(type);
	}


	/**
	 *  Gets a list of all completed annotataions for this item of the given type.
	 *
	 * @param  type  The annotation type, for example 'Review', 'Teaching tip', etc.
	 * @return       A list of {@link DleseAnnoDocReader}s for all completed annotataions for this item of the
	 *      given type, or an empty list.
	 */
	public ArrayList getCompletedAnnosOfType(String type) {
		initAnnosByType();
		ArrayList list = (ArrayList) completedAnnosByType.get(type);
		if (list == null) {
			list = new ArrayList();
		}
		return list;
	}


	/**
	 *  Determines whether the item has an annotation in progress.
	 *
	 * @return    'true' if the item has an annotation in progress, otherwise 'false'.
	 */
	public String getHasInProgressAnno() {
		if (hasInProgressAnno()) {
			return "true";
		}
		else {
			return "false";
		}
	}


	/**
	 *  Determines whether the item has an annotation in progress.
	 *
	 * @return    True if the item has an annotation in progress, otherwise false.
	 */
	public boolean hasInProgressAnno() {
		initAnnosByType();
		return !inProgressAnnosByFormat.isEmpty();
	}


	/**
	 *  Determines whether the item has an annotation in progress of the given format, which is one of 'text',
	 *  'audio', 'graphical', or 'video'.
	 *
	 * @param  format  Annotation format
	 * @return         True if the item has an annotation in progress of the given format, otherwise false.
	 */
	public boolean hasInProgressAnnoOfFormat(String format) {
		initAnnosByType();
		return inProgressAnnosByFormat.containsKey(format);
	}


	/**
	 *  Gets all in-progress annotations for this item that have the given format, which is one of 'text',
	 *  'audio', 'graphical', or 'video'.
	 *
	 * @param  format  Annotation format
	 * @return         A list of {@link DleseAnnoDocReader}s
	 */
	public ArrayList getInProgressAnnosOfFormat(String format) {
		initAnnosByType();
		ArrayList list = (ArrayList) inProgressAnnosByFormat.get(format);
		if (list == null)
			list = new ArrayList();
		return list;
	}


	private int numAudioInProgress = 0;
	private int numGraphicalInProgress = 0;
	private int numTextInProgress = 0;
	private int numVideoInProgress = 0;


	private void initAnnosByType() {
		// If already initialized, return
		if (completedAnnosByType != null)
			return;

		completedAnnosByType = new HashMap();
		inProgressAnnosByFormat = new HashMap();
		completedAnnos = new ArrayList();
		ResultDocList annoResults = getAnnotationResultDocs();
		if (annoResults == null)
			return;
		ArrayList annoList = null;
		boolean isCompleted;
		boolean isInProgress;
		for (int i = 0; i < annoResults.size(); i++) {
			DleseAnnoDocReader anno = (DleseAnnoDocReader) annoResults.get(i).getDocReader();
			String annoType = anno.getType();
			String annoStatus = anno.getStatus();
			String annoFormat = anno.getFormat();
			isCompleted = anno.getIsCompleted();
			isInProgress = anno.getIsInProgress();

			if (isCompleted) {
				numCompletedAnnos++;
				annoList = (ArrayList) completedAnnosByType.get(annoType);
				completedAnnos.add(anno);
			}
			else if (isInProgress) {
				if (annoFormat.equals("text"))
					numTextInProgress++;
				else if (annoFormat.equals("audio"))
					numAudioInProgress++;
				else if (annoFormat.equals("graphical"))
					numGraphicalInProgress++;
				else if (annoFormat.equals("video"))
					numVideoInProgress++;
				numInProgressAnnos++;
				annoList = (ArrayList) inProgressAnnosByFormat.get(annoFormat);
			}

			if (annoList == null)
				annoList = new ArrayList();

			annoList.add(anno);
			if (isCompleted)
				completedAnnosByType.put(annoType, annoList);
			else if (isInProgress)
				inProgressAnnosByFormat.put(annoFormat, annoList);
		}
	}


	/**
	 *  Gets a list of {@link DleseAnnoDocReader}s containing each of the in-progress type 'text' annotations for
	 *  this resource.
	 *
	 * @return    A list of {@link DleseAnnoDocReader}s or empty list
	 */
	public ArrayList getTextAnnosInProgress() {
		return getInProgressAnnosOfFormat("text");
	}


	/**
	 *  Gets a list of {@link DleseAnnoDocReader}s containing each of the in-progress type 'audio' annotations
	 *  for this resource.
	 *
	 * @return    A list of {@link DleseAnnoDocReader}s
	 */
	public ArrayList getAudioAnnosInProgress() {
		return getInProgressAnnosOfFormat("audio");
	}


	/**
	 *  Gets a list of {@link DleseAnnoDocReader}s containing each of the in-progress type 'graphical'
	 *  annotations for this resource.
	 *
	 * @return    A list of {@link DleseAnnoDocReader}s or empty list
	 */
	public ArrayList getGraphicalAnnosInProgress() {
		return getInProgressAnnosOfFormat("graphical");
	}


	/**
	 *  Gets a list of {@link DleseAnnoDocReader}s containing each of the in-progress type 'video' annotations
	 *  for this resource.
	 *
	 * @return    A list of {@link DleseAnnoDocReader}s or empty list
	 */
	public ArrayList getVideoAnnosInProgress() {
		return getInProgressAnnosOfFormat("video");
	}


	/**
	 *  Gets a list of {@link DleseAnnoDocReader}s containing each of the completed annotations, regardless of
	 *  type, for this resource.
	 *
	 * @return    A list of {@link DleseAnnoDocReader}s or empty list
	 */
	public ArrayList getCompletedAnnos() {
		initAnnosByType();
		return completedAnnos;
	}


	/**
	 *  Gets a list of {@link DleseAnnoDocReader}s containing each of the completed type 'review' annotations for
	 *  this resource.
	 *
	 * @return    A list of {@link DleseAnnoDocReader}s or empty list
	 */
	public ArrayList getCompletedReviews() {
		return getCompletedAnnosOfType("Review");
	}


	/**
	 *  Gets a list of {@link DleseAnnoDocReader}s containing each of the completed type 'teaching tip'
	 *  annotations for this resource.
	 *
	 * @return    A list of {@link DleseAnnoDocReader}s or empty list
	 */
	public ArrayList getCompletedTeachingTips() {
		return getCompletedAnnosOfType("Teaching tip");
	}


	/**
	 *  Gets a list of {@link DleseAnnoDocReader}s containing each of the completed type 'editors summary'
	 *  annotations for this resource.
	 *
	 * @return    A list of {@link DleseAnnoDocReader}s or empty list
	 */
	public ArrayList getCompletedEditorSummaries() {
		return getCompletedAnnosOfType("Editor's summary");
	}


	/**
	 *  Gets a list of {@link DleseAnnoDocReader}s containing each of the completed type 'challenging situation'
	 *  annotations for this resource.
	 *
	 * @return    A list of {@link DleseAnnoDocReader}s or empty list
	 */
	public ArrayList getCompletedChallengingSituations() {
		return getCompletedAnnosOfType("Information on challenging teaching and learning situations");
	}


	/**
	 *  Gets a list of {@link DleseAnnoDocReader}s containing each of the completed type 'average scores'
	 *  annotations for this resource.
	 *
	 * @return    A list of {@link DleseAnnoDocReader}s or empty list
	 */
	public ArrayList getCompletedAverageScores() {
		return getCompletedAnnosOfType("Average scores of aggregated indices");
	}


	/**
	 *  Gets a list of {@link DleseAnnoDocReader}s containing each of the completed type 'advice' annotations for
	 *  this resource.
	 *
	 * @return    A list of {@link DleseAnnoDocReader}s or empty list
	 */
	public ArrayList getCompletedAdvice() {
		return getCompletedAnnosOfType("Advice");
	}


	/**
	 *  Gets a list of {@link DleseAnnoDocReader}s containing each of the completed type 'annotation' annotations
	 *  for this resource.
	 *
	 * @return    A list of {@link DleseAnnoDocReader}s or empty list
	 */
	public ArrayList getCompletedAnnotation() {
		return getCompletedAnnosOfType("Annotation");
	}


	/**
	 *  Gets a list of {@link DleseAnnoDocReader}s containing each of the completed type 'bias' annotations for
	 *  this resource.
	 *
	 * @return    A list of {@link DleseAnnoDocReader}s or empty list
	 */
	public ArrayList getCompletedBias() {
		return getCompletedAnnosOfType("Bias");
	}


	/**
	 *  Gets a list of {@link DleseAnnoDocReader}s containing each of the completed type 'change' annotations for
	 *  this resource.
	 *
	 * @return    A list of {@link DleseAnnoDocReader}s or empty list
	 */
	public ArrayList getCompletedChange() {
		return getCompletedAnnosOfType("Change");
	}


	/**
	 *  Gets a list of {@link DleseAnnoDocReader}s containing each of the completed type 'comment' annotations
	 *  for this resource.
	 *
	 * @return    A list of {@link DleseAnnoDocReader}s or empty list
	 */
	public ArrayList getCompletedComment() {
		return getCompletedAnnosOfType("Comment");
	}


	/**
	 *  Gets a list of {@link DleseAnnoDocReader}s containing each of the completed type 'educational standard'
	 *  annotations for this resource.
	 *
	 * @return    A list of {@link DleseAnnoDocReader}s or empty list
	 */
	public ArrayList getCompletedEducationalStandard() {
		return getCompletedAnnosOfType("Educational standard");
	}


	/**
	 *  Gets a list of {@link DleseAnnoDocReader}s containing each of the completed type 'example' annotations
	 *  for this resource.
	 *
	 * @return    A list of {@link DleseAnnoDocReader}s or empty list
	 */
	public ArrayList getCompletedExample() {
		return getCompletedAnnosOfType("Example");
	}


	/**
	 *  Gets a list of {@link DleseAnnoDocReader}s containing each of the completed type 'explanation'
	 *  annotations for this resource.
	 *
	 * @return    A list of {@link DleseAnnoDocReader}s or empty list
	 */
	public ArrayList getCompletedExplanation() {
		return getCompletedAnnosOfType("Explanation");
	}


	/**
	 *  Gets a list of {@link DleseAnnoDocReader}s containing each of the completed type 'question' annotations
	 *  for this resource.
	 *
	 * @return    A list of {@link DleseAnnoDocReader}s or empty list
	 */
	public ArrayList getCompletedQuestion() {
		return getCompletedAnnosOfType("Question");
	}


	/**
	 *  Gets a list of {@link DleseAnnoDocReader}s containing each of the completed type 'see also' annotations
	 *  for this resource.
	 *
	 * @return    A list of {@link DleseAnnoDocReader}s or empty list
	 */
	public ArrayList getCompletedSeeAlso() {
		return getCompletedAnnosOfType("See also");
	}
	
	
	// ------------ Debug / logging ---------------
	
	/**
	 *  Output a line of text to error out, with datestamp.
	 *
	 * @param  s  The text that will be output to error out.
	 */
	protected static void prtlnErr(String s) {
		System.err.println(getDateStamp() + " ItemDocReader Error: " + s);
	}


	/**
	 *  Output a line of text to standard out, with datestamp, if debug is set to true.
	 *
	 * @param  s  The String that will be output.
	 */
	protected static void prtln(String s) {
		System.out.println(getDateStamp() + " ItemDocReader: " + s);
	}	
	
}


