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
package org.dlese.dpc.index.writer;

import java.io.*;
import java.util.*;
import java.text.*;

import org.dlese.dpc.xml.*;
import org.dlese.dpc.index.*;
import org.dlese.dpc.util.*;
import org.apache.lucene.document.*;
import org.dlese.dpc.vocab.*;
import org.dlese.dpc.repository.*;
import org.dlese.dpc.services.mmd.MmdException;
import org.dlese.dpc.services.mmd.MmdRec;
import org.dlese.dpc.index.writer.xml.*;
import edu.ucsb.adl.LuceneGeospatialQueryConverter;

/**
 *  Creates a Lucene {@link org.apache.lucene.document.Document} from an ADN-item metadata source file.<p>
 *
 *  <b>The Lucene {@link org.apache.lucene.document.Document} fields that are created by this class are (in
 *  addition the the ones listed for {@link org.dlese.dpc.index.writer.FileIndexingServiceWriter}):</b> <br>
 *  <br>
 *  <code><b>doctype</b> </code> - Set to 'adn'. Stored. Note: the actual indexing of this field happens in
 *  the superclass {@link org.dlese.dpc.index.writer.FileIndexingServiceWriter}.<br>
 *  <code><b>additional fields</b> </code> - A number of additional fields are defined. See the Java code for
 *  method {@link #addFrameworkFields(Document, Document)} for details. <br>
 *  <br>
 *
 *
 * @author    John Weatherley, Ryan Deardorff
 */
public class ADNFileIndexingWriter extends ItemFileIndexingWriter {

	private XMLDoc adnXmlDoc[] = null;
	private XMLDoc myXmlDoc = null;
	private String myID = null;
	private String myContent = null;
	private String myContentType = null;
	private boolean singleRecord = false;
	private boolean isDupDoc = false;
	private boolean indexTheMultiDoc = true;
	private ArrayList additionalCollections = new ArrayList();
	private ArrayList additionalIds = new ArrayList();
	private ArrayList additionalIdsFiles = new ArrayList();
	private MmdRec[] associatedMmdRecs = null;
	private MmdRec[] allMmdRecs = null;
	private MmdRec myMmdRec = null;
	private boolean hasGotMmdRec = false;
	private String metadataFormat = "adn";

	private static long num_instances = 0;


	/**  Create a ADNFileIndexingWriter that indexes the given collection in field collection. */
	public ADNFileIndexingWriter() {
		num_instances++;
		//prtln("ADNFileIndexingWriter(): NumInstances: " + num_instances);
	}


	/**
	 *  Create a ADNFileIndexingWriter that indexes the given collection in field collection.
	 *
	 * @param  isDupDoc  False to force this to be processed as a non-dup
	 */
	public ADNFileIndexingWriter(boolean isDupDoc) {
		//prtln("ADNFileIndexingWriter() isDupDoc: " + isDupDoc + " NumInstances: " + num_instances);
		num_instances++;
		this.isDupDoc = isDupDoc;
	}


	/**
	 *  Perform finalization... closing resources, etc.
	 *
	 * @exception  Throwable  If error
	 */
	protected void finalize() throws Throwable {
		try {
			num_instances--;
		} finally {
			super.finalize();
		}
	}


	/**
	 *  Gets the numInstances attribute of the ADNFileIndexingWriter class
	 *
	 * @return    The numInstances value
	 */
	public static long getNumInstances() {
		return num_instances;
	}


	/**
	 *  Initialize the XML map, MmdRecs and other data prior to processing
	 *
	 * @param  source         The source file being indexed.
	 * @param  existingDoc    A Document that previously existed in the index for this item, if present
	 * @exception  Exception  Thrown if error reading the XML map
	 */
	public void initItem(File source, Document existingDoc)
		 throws Exception {

		try {

			//accessionStatus = null;
			hasGotMmdRec = false;
			associatedMmdRecs = null;
			allMmdRecs = null;
			myMmdRec = null;
			additionalCollections.clear();
			additionalIds.clear();
			additionalIdsFiles.clear();

			// Design note: If making a deleted doc, get content from getFileContent(), not the file:
			if (isMakingDeletedDoc()) {
				myXmlDoc = new XMLDoc();
				myXmlDoc.useXmlString(getFileContent(),
					this.isValidationEnabled(),
					true,
					true);
			}
			else {
				myXmlDoc = new XMLDoc("file:///" + source.getAbsolutePath(),
					this.isValidationEnabled(),
					true,
					true);
			}

			FileIndexingService fileIndexingService = getFileIndexingService();
			if (fileIndexingService != null) {
				// If the indexer has not been configured to de-dup the records, then don't use the de-duper
				// Note: This attribute is set in DDSSevlet from the context param filterDups
				String tmp = (String) fileIndexingService.getAttribute("filterDups");
				if (tmp == null || tmp.equals("false"))
					singleRecord = true;
			}

			myID = myXmlDoc.getXmlString("metaMetadata/catalogEntries/catalog@entry");

			// The Below is for for diagnostics...
			/* if(isDupDoc)
			prtln("indexing dup ID: " + myID + " singleRecord:" + singleRecord + " isDupDoc:" + isDupDoc);
		else
			prtln("indexing ID: " + myID + " singleRecord:" + singleRecord + " isDupDoc:" + isDupDoc); */
			//if(recordDataService != null)
			//addDocToRemove("id", SimpleLuceneIndex.encodeToTerm(myID));
			boolean hasIdMapperData = true;

			RecordDataService recordDataService = getRecordDataService();

			// Check to see that the ID mapper has an entry for this record and if not...
			if (recordDataService != null && !recordDataService.isIdMapperDisabled()) {
				MmdRec tmpMyMmdRec = getMyMmdRec();
				if (tmpMyMmdRec == null) {
					hasIdMapperData = false;

					//throw new MmdException("Id mapper service has no entry for ID:" + myID + " in collection:" + collection + ".");
				}
				// If we have an ID Mapper record, get the content for indexing
				else {
					myContent = tmpMyMmdRec.getPrimaryContent();
					myContentType = tmpMyMmdRec.getPrimaryContentType();
				}
			}

			// Index a single doc.
			if (recordDataService == null || singleRecord || !hasIdMapperData) {

				// The Below is for for diagnostics...
				/* if(recordDataService == null)
				prtln("indexing as single doc because recordDataService is null");
			else
				prtln("indexing as single doc because singleRecord is true"); */
				adnXmlDoc = new XMLDoc[]{myXmlDoc};
			}
			// Index a potential multi-doc.
			else {

				// Get dups in other collections
				MmdRec[] qualifiedAssociatedMmdRecs = getAssociatedMmdRecs();

				// Make sure not to include dups if the other collection has multiple dups!
				if (qualifiedAssociatedMmdRecs != null && qualifiedAssociatedMmdRecs.length > 0) {
					// Make sure there are no dups of me before proceeding
					addDocToRemove("id", SimpleLuceneIndex.encodeToTerm(myID));

					HashMap dupsFromASingleCollection = new HashMap();
					ArrayList collectionsWithMultiDups = new ArrayList();
					for (int i = 0; i < qualifiedAssociatedMmdRecs.length; i++) {
						String collKey = qualifiedAssociatedMmdRecs[i].getCollKey();
						if (dupsFromASingleCollection.containsKey(collKey))
							collectionsWithMultiDups.add(collKey);
						dupsFromASingleCollection.put(collKey, collKey);
					}
					ArrayList mmdRecsList = new ArrayList();
					for (int i = 0; i < qualifiedAssociatedMmdRecs.length; i++) {
						String collKey = qualifiedAssociatedMmdRecs[i].getCollKey();
						if (!collectionsWithMultiDups.contains(collKey)) {
							//prtln(myID + " has a legit dup: " + qualifiedAssociatedMmdRecs[i].getId() + " from " + qualifiedAssociatedMmdRecs[i].getCollKey());
							mmdRecsList.add(qualifiedAssociatedMmdRecs[i]);
						}
						//else
						//prtln(myID + " has a NON-legit dup: " + qualifiedAssociatedMmdRecs[i].getId() + " from " + qualifiedAssociatedMmdRecs[i].getCollKey());
					}
					qualifiedAssociatedMmdRecs = (MmdRec[]) mmdRecsList.toArray(new MmdRec[]{});
				}

				// The Below is for for diagnostics...
				/* if(qualifiedAssociatedMmdRecs == null)
				prtln("No dup docs were found by IDMapper: qualifiedAssociatedMmdRecs = null");
			else if(qualifiedAssociatedMmdRecs.length == 0)
				prtln("No dup docs were found by IDMapper: qualifiedAssociatedMmdRecs.length = 0");
			else
				prtln( "Dup docs were found by IDMapper. qualifiedAssociatedMmdRecs.length = " + qualifiedAssociatedMmdRecs.length); */
				// Check for dups in the same collection and throw an error
				if (true) {
					/*
				Issue: if we index more than one item in a given collection that is identified as a
				dup, other collections that have a record that is also a dup for the record
				will index ALL of them in one big multi-doc. The totals for a given collection
				will not be correct.
				*/
					MmdRec[] dupRecsInMyCollection = recordDataService.getAssociatedMMDRecs(RecordDataService.QUERY_SAME, myID, super.getCollections()[0]);
					if (dupRecsInMyCollection != null && dupRecsInMyCollection.length > 0) {
						Arrays.sort(dupRecsInMyCollection);
						if (!dupRecsInMyCollection[0].getId().equals(myID)) {
							String m = dupRecsInMyCollection[0].getId();
							for (int i = 1; i < dupRecsInMyCollection.length; i++)
								m += ", " + dupRecsInMyCollection[i].getId();

							String message = "Dup records were found in collection " + super.getCollections()[0] +
								". IDs " + m + " appear to catalog the same resource. " +
								"Record ID " + dupRecsInMyCollection[0].getId() +
								" is being indexed and not this one.";
							//prtln(message);
							throw new Exception(message);
						}
					}
				}

				// Handle duplicates found in other collections...
				if (qualifiedAssociatedMmdRecs == null || qualifiedAssociatedMmdRecs.length == 0)
					adnXmlDoc = new XMLDoc[]{myXmlDoc};
				else {
					ArrayList xmlDocs = new ArrayList(qualifiedAssociatedMmdRecs.length + 1);
					xmlDocs.add(myXmlDoc);
					for (int i = 0; i < qualifiedAssociatedMmdRecs.length; i++) {
						//prtln("Found associated dup record for id " + myID + ": " + qualifiedAssociatedMmdRecs[i].getId() + " " + qualifiedAssociatedMmdRecs[i].getCollKey());
						try {
							xmlDocs.add(new XMLDoc(
								"file:///" + recordDataService.getFilePathForId(qualifiedAssociatedMmdRecs[i]),
								false,
								true,
								true));
							additionalIds.add(qualifiedAssociatedMmdRecs[i].getId());
							additionalCollections.add(qualifiedAssociatedMmdRecs[i].getCollKey());
							additionalIdsFiles.add(recordDataService.getFileForId(qualifiedAssociatedMmdRecs[i]));
						} catch (NullPointerException npe) {
							prtlnErr("NullPointerException in ADNFileIndexingWriter.init().");
							npe.printStackTrace();
						} catch (Throwable e) {
							// FileNotFoundException, etc...
							// Note: One way this can be thrown is if the IDMapper is pointing to a copy of
							// the file that has a different file name than the one the indexer is using.
							//prtlnErr("Exception in ADNFileIndexingWriter.init(): " + e);
							//e.printStackTrace();
						}
					}
					adnXmlDoc = (XMLDoc[]) xmlDocs.toArray(new XMLDoc[]{});

					// Determine whether to multi-doc index this record - it should only be
					// multi-doc indexed once for all items that reference the same resource...
					if (adnXmlDoc.length > 1) {
						indexIndividualDupDoc(myID, super.getCollections()[0], getSourceFile());
						for (int i = 0; i < additionalIds.size(); i++) {
							File dupDocFile = (File) additionalIdsFiles.get(i);
							indexIndividualDupDoc(
								(String) additionalIds.get(i),
								(String) additionalCollections.get(i),
								dupDocFile);
						}
					}
					//prtln("A total of " + adnXmlDoc.length + " existing associated records were found for id " + myID);
				}
			}

		} catch (Exception e) {
			if (e instanceof NullPointerException) {
				prtlnErr("caught exception: " + e);
				e.printStackTrace();
			}
			throw e;
		}
	}


	/**  Release map resources for GC after processing. */
	protected void destroy() {
		adnXmlDoc = null;
		additionalCollections.clear();
		additionalIds.clear();
		additionalIdsFiles.clear();
		associatedMmdRecs = null;
		allMmdRecs = null;
		myMmdRec = null;
		hasGotMmdRec = false;
		prtln("ADNFileIndexingWriter.destroy()");
	}


	/**
	 *  Returns unique collection keys for the item being indexed, separated by spaces. For example 'dcc,'
	 *  'comet' or 'dwel'. Since this may be a multi-doc, it may have multiple collections, so overridding the
	 *  default getCollection() method.
	 *
	 * @return                The collection keys
	 * @exception  Exception  If error
	 */
	public String[] getCollections() throws Exception {
		RecordDataService recordDataService = getRecordDataService();
		if (recordDataService == null || singleRecord) {
			return super.getCollections();
		}
		else {
			List myColects = new ArrayList();
			
			myColects.add(super.getCollections()[0]);
			for (int i = 0; i < additionalCollections.size(); i++)
				myColects.add((String) additionalCollections.get(i));
			return (String[])myColects.toArray(new String[]{});
		}
	}


	/**
	 *  Returns the accession status of this record, for example 'accessioned'. The String is tokenized, stored
	 *  and indexed under the field key 'accessionstatus'.
	 *
	 * @return                The accession status.
	 * @exception  Exception  This method should throw and Exception with appropriate error message if an error
	 *      occurs.
	 */
	protected String getAccessionStatus() throws Exception {
		RecordDataService recordDataService = getRecordDataService();
		if (recordDataService == null)
			return MmdRec.statusNames[MmdRec.STATUS_ACCESSIONED_DISCOVERABLE];
		else
			return recordDataService.getAccessionStatus(getAllMmdRecs());
	}



	/**
	 *  Returns true if the item has one or more related resource, false otherwise.
	 *
	 * @return                True if the item has one or more related resource, false otherwise.
	 * @exception  Exception  This method should throw and Exception with appropriate error message if an error
	 *      occurs.
	 */
	protected boolean getHasRelatedResource() throws Exception {
		for (int i = 0; i < adnXmlDoc.length; i++)
			if (adnXmlDoc[i].getXmlField("relations/relation/idEntry@entry").length() > 0 ||
				adnXmlDoc[i].getXmlField("relations/relation/urlEntry@url").length() > 0)
				return true;

		return false;
	}


	/**
	 *  Returns the IDs of related resources that are cataloged by ID, or null if none are present
	 *
	 * @return                Related resource IDs, or null if none are available
	 * @exception  Exception  This method should throw and Exception with appropriate error message if an error
	 *      occurs.
	 */
	protected String[] getRelatedResourceIds() throws Exception {
		ArrayList strings = new ArrayList();
		for (int i = 0; i < adnXmlDoc.length; i++) {
			String[] values = adnXmlDoc[i].getXmlFields("relations/relation/idEntry@entry");
			for (int j = 0; j < values.length; j++)
				strings.add(values[j]);
		}

		if (strings.size() > 0)
			return (String[]) strings.toArray(new String[]{});
		else
			return null;
	}


	/**
	 *  Returns the URLs of related resources that are cataloged by URL, or null if none are present
	 *
	 * @return                Related resource URLs, or null if none are available
	 * @exception  Exception  This method should throw and Exception with appropriate error message if an error
	 *      occurs.
	 */
	protected String[] getRelatedResourceUrls() throws Exception {
		ArrayList strings = new ArrayList();
		for (int i = 0; i < adnXmlDoc.length; i++) {
			String[] values = adnXmlDoc[i].getXmlFields("relations/relation/urlEntry@url");
			for (int j = 0; j < values.length; j++)
				strings.add(values[j]);
		}

		if (strings.size() > 0)
			return (String[]) strings.toArray(new String[]{});
		else
			return null;
	}


	/**
	 *  Returns the accession date for the item, or null if this item is not accessioned. If this is a multi-doc,
	 *  returns the oldest accession date of the bunch, corresponding to the first time this resource appeared in
	 *  the library.
	 *
	 * @return                The accession date for the item, or null if this item is not accessioned.
	 * @exception  Exception  This method should throw and Exception with appropriate error message if an error
	 *      occurs.
	 */
	protected Date getAccessionDate() throws Exception {
		try {
			Date[] accessionDates = new Date[adnXmlDoc.length];
			for (int i = 0; i < adnXmlDoc.length; i++)
				accessionDates[i] =
					MetadataUtils.parseUnionDateType(adnXmlDoc[i].getXmlString("metaMetadata/dateInfo@accessioned"));

			// Return the oldest date out of the full bunch
			Arrays.sort(accessionDates);
			return accessionDates[0];
		} catch (Throwable e) {
			//prtlnErr("Unable to parse accession date: " + e);
			return null;
		}
	}


	/**
	 *  Returns the date this item was first created, or null if not available.
	 *
	 * @return                The item creation date or null
	 * @exception  Exception  This method should throw and Exception with appropriate error message if an error
	 *      occurs.
	 */
	protected Date getCreationDate() throws Exception {
		try {
			Date[] creationDates = new Date[adnXmlDoc.length];
			for (int i = 0; i < adnXmlDoc.length; i++)
				creationDates[i] =
					MetadataUtils.parseUnionDateType(adnXmlDoc[i].getXmlString("metaMetadata/dateInfo@created"));

			// Return the most recent date out of the full bunch
			Arrays.sort(creationDates);

			return creationDates[0];
		} catch (Throwable e) {
			//prtlnErr("Unable to parse creation date: " + e);
			return null;
		}
	}



	/**
	 *  Gets the name of the concrete {@link org.dlese.dpc.index.reader.DocReader} class that is used to read
	 *  this type of {@link org.apache.lucene.document.Document}, which is "ItemDocReader".
	 *
	 * @return    The String "org.dlese.dpc.index.reader.ItemDocReader".
	 */
	public String getReaderClass() {
		return "org.dlese.dpc.index.reader.ItemDocReader";
	}


	/**
	 *  Default and stems fields handled here, so do not index full content.
	 *
	 * @return    False
	 */
	public boolean indexFullContentInDefaultAndStems() {
		return false;
	}


	/**
	 *  Returns the MmdRecs for records in other collections that catalog the same resource, not including
	 *  myMmdRec.
	 *
	 * @return    The associated MmdRecs, or null if none
	 */
	protected MmdRec[] getAssociatedMmdRecs() {
		RecordDataService recordDataService = getRecordDataService();
		if (recordDataService != null && associatedMmdRecs == null) {
			try {
				associatedMmdRecs = recordDataService.getAssociatedMMDRecs(RecordDataService.QUERY_OTHER, myID, super.getCollections()[0]);
			} catch (Exception e) {
				prtlnErr("getAssociatedMmdRecs() error: " + e);
			}
			if (associatedMmdRecs == null)
				associatedMmdRecs = new MmdRec[]{};
		}

		return associatedMmdRecs;
	}


	/**
	 *  Returns the MmdRecs for all records that catalog this resouce, including myMmdRec.
	 *
	 * @return    All MmdRecs for this resource, null or empty if none
	 */
	protected MmdRec[] getAllMmdRecs() {
		if (allMmdRecs == null) {
			MmdRec[] assocMmds = getAssociatedMmdRecs();
			ArrayList allMmds = new ArrayList();
			if (assocMmds != null)
				for (int i = 0; i < assocMmds.length; i++)
					allMmds.add(assocMmds[i]);
			MmdRec myMmd = getMyMmdRec();
			if (myMmd != null)
				allMmds.add(myMmd);

			allMmdRecs = (MmdRec[]) allMmds.toArray(new MmdRec[]{});
		}
		return allMmdRecs;
	}


	/**
	 *  Returns the MmdRec for this record only.
	 *
	 * @return    The MmdRec for this record, or null
	 */
	protected MmdRec getMyMmdRec() {
		RecordDataService recordDataService = getRecordDataService();
		if (recordDataService != null && !hasGotMmdRec) {
			try {
				myMmdRec = recordDataService.getMmdRec(myID, super.getCollections()[0]);
			} catch (Exception e) {
				prtlnErr("getMyMmdRec() error: " + e);
			}
			hasGotMmdRec = true;
		}
		return myMmdRec;
	}


	/**
	 *  Gets a report detailing any errors found in the validation of the data, or null if no error was found.
	 *
	 * @return                Null if no data validation errors were found, otherwise a String that details the
	 *      nature of the error.
	 * @exception  Exception  If error in performing the validation.
	 */
	protected String getValidationReport()
		 throws Exception {
		String valError = "";
		for (int i = 0; i < adnXmlDoc.length; i++)
			if (adnXmlDoc[i].hasErrors())
				valError += " " + adnXmlDoc[i].getErrors().toString();

		if (valError.length() == 0)
			return null;
		else
			return valError;
	}


	/**
	 *  Gets the docType attribute of the ADNFileIndexingWriter, which is 'adn.'
	 *
	 * @return    The docType, which is 'adn.'
	 */
	public final String getDocType() {
		return metadataFormat;
	}


	/**
	 *  Gets the id(s) for this item. If multiple IDs exists, the first one is the primary.
	 *
	 * @return                The id value
	 * @exception  Exception  If an error occurs
	 */
	protected String[] _getIds() throws Exception {
		List ids = new ArrayList();
		for (int i = 0; i < adnXmlDoc.length; i++)
			ids.add(adnXmlDoc[i].getXmlString("metaMetadata/catalogEntries/catalog@entry").trim());
		return (String[])ids.toArray(new String[]{});
	}


	/**
	 *  Gets the title attribute of the ADNFileIndexingWriter object
	 *
	 * @return                The title value
	 * @exception  Exception  If an error occurs
	 */
	public final String getTitle() throws Exception {
		String val = "";
		for (int i = 0; i < adnXmlDoc.length; i++)
			val += " " + adnXmlDoc[i].getXmlString("general/title");	
		return val.trim();
	}


	/**
	 *  Gets the description attribute of the ADNFileIndexingWriter object
	 *
	 * @return                The description value
	 * @exception  Exception  If an error occurs
	 */
	public final String getDescription() throws Exception {
		String val = "";
		for (int i = 0; i < adnXmlDoc.length; i++) {
			try {
				val += " " + adnXmlDoc[i].getXmlString("general/description");
			} catch (XMLException xe) {
				// Allow empty description, even though is't required metadata...
			}
		}
		return val.trim();
	}


	/**
	 *  Gets the url(s) from the ADN record(s).
	 *
	 * @return                The urls value
	 * @exception  Exception  If an error occurs
	 */
	public final String[] getUrls() throws Exception {
		List urls = new ArrayList();
		for (int i = 0; i < adnXmlDoc.length; i++)
			urls.add(adnXmlDoc[i].getXmlString("technical/online/primaryURL"));
		return (String[])urls.toArray(new String[]{});
	}


	/**
	 *  Returns the item's keywords sorted and separated by the '+' symbol. An empty String or null is
	 *  acceptable. The String is tokenized, stored and indexed under the field key 'keywords' and is also
	 *  indexed in the 'default' field.
	 *
	 * @return                The keywords String
	 * @exception  Exception  This method should throw and Exception with appropriate error message if an error
	 *      occurs.
	 */
	protected String getKeywords() throws Exception {
		ArrayList kw = new ArrayList();
		HashMap kwMap = new HashMap();
		String keyword;
		String stdKeyword;
		for (int i = 0; i < adnXmlDoc.length; i++) {
			String[] keywords = adnXmlDoc[i].getXmlFields("general/keywords/keyword");
			for (int j = 0; j < keywords.length; j++) {
				stdKeyword = keywords[j].trim().toLowerCase();
				if (!kwMap.containsKey(stdKeyword)) {
					kw.add(keywords[j].trim());
					kwMap.put(stdKeyword, stdKeyword);
				}
			}
		}

		Collections.sort(kw);
		String val = "";
		if (kw.size() > 0)
			val = (String) kw.get(0);
		for (int j = 1; j < kw.size(); j++)
			val += "+" + (String) kw.get(j);

		kw.clear();
		kwMap.clear();
		//prtln("keywords: " + val);
		return val.trim();
	}


	/**
	 *  Returns the items creator's last name. An empty String or null is acceptable. The String is tokenized,
	 *  stored and indexed under the field the 'default' field only.
	 *
	 * @return                The creator's last name String
	 * @exception  Exception  This method should throw and Exception with appropriate error message if an error
	 *      occurs.
	 */
	protected String getCreatorLastName() throws Exception {
		String val = "";
		for (int i = 0; i < adnXmlDoc.length; i++) {
			String[] lastNames = adnXmlDoc[i].getXmlFields("lifecycle/contributors/contributor/person/nameLast");
			for (int j = 0; j < lastNames.length; j++)
				val += " " + lastNames[j];
		}
		//prtln("creatorlastname: " + val);
		return val.trim();
	}


	/**
	 *  Returns the items creator's full name. An empty String or null is acceptable. The String is tokenized,
	 *  stored and indexed under the field key 'creator'.
	 *
	 * @return                Creator's full name
	 * @exception  Exception  This method should throw and Exception with appropriate error message if an error
	 *      occurs.
	 */
	protected String getCreator() throws Exception {
		String val = "";
		for (int i = 0; i < adnXmlDoc.length; i++) {
			String[] firstNames = adnXmlDoc[i].getXmlFields("lifecycle/contributors/contributor/person/nameFirst");
			String[] middleNames = adnXmlDoc[i].getXmlFields("lifecycle/contributors/contributor/person/nameMiddle");
			String[] lastNames = adnXmlDoc[i].getXmlFields("lifecycle/contributors/contributor/person/nameLast");
			for (int j = 0; j < firstNames.length; j++)
				val += " " + firstNames[j];
			for (int j = 0; j < middleNames.length; j++)
				val += " " + middleNames[j];
			for (int j = 0; j < lastNames.length; j++)
				val += " " + lastNames[j];
		}
		//prtln("creators: " + val);
		return val.trim();
	}


	/**
	 *  Returns the item's cost. The String is stored and indexed under the field key 'cost'.
	 *
	 * @return                Resource cost
	 * @exception  Exception  This method should throw and Exception with appropriate error message if an error
	 *      occurs.
	 */
	protected String getCost() throws Exception {
		String val = "";
		for (int i = 0; i < adnXmlDoc.length; i++) {
			String[] cost = adnXmlDoc[i].getXmlFields("rights/cost");
			for (int j = 0; j < cost.length; j++)
				val += " " + cost[j].replaceAll(":", "").replaceAll(" ", "");
		}
		//prtln("creators: " + val);
		return val.trim();
	}


	/**
	 *  Gets the boundingBox attribute of the ADNFileIndexingWriter object
	 *
	 * @return    The boundingBox value
	 */
	protected BoundingBox getBoundingBox() throws Exception {
		// Return the first BB found in any of the records, or null...
		for (int i = 0; i < adnXmlDoc.length; i++) {
			String n = adnXmlDoc[i].getXmlField("geospatialCoverages/geospatialCoverage/boundBox/northCoord");
			String s = adnXmlDoc[i].getXmlField("geospatialCoverages/geospatialCoverage/boundBox/southCoord");
			String e = adnXmlDoc[i].getXmlField("geospatialCoverages/geospatialCoverage/boundBox/eastCoord");
			String w = adnXmlDoc[i].getXmlField("geospatialCoverages/geospatialCoverage/boundBox/westCoord");
			if (n.length() > 0 && s.length() > 0 && e.length() > 0 && w.length() > 0) {
				double dn = 0;
				double ds = 0;
				double de = 0;
				double dw = 0;
				try {
					dn = Double.parseDouble(n);
					ds = Double.parseDouble(s);
					de = Double.parseDouble(e);
					dw = Double.parseDouble(w);
				} catch (Throwable t) {
					continue;
				}
				return new BoundingBox(dn, ds, de, dw);
			}
		}
		return null;
	}


	/**
	 *  Returns the content of the item this record catalogs, or null if not available. For example the full HTML
	 *  text of the Web page.
	 *
	 * @return    The content of the item, or null
	 */
	protected String getContent() {
		return myContent;
	}


	/**
	 *  Returns the content type of the item this record catalogs, or null if not available. For example
	 *  "text/html" or "html".
	 *
	 * @return    The content type of the item, or null
	 */
	protected String getContentType() {
		return myContentType;
	}


	/**
	 *  Adds custom fields to the index that are unique to this framework.
	 *
	 * @param  newDoc         The feature to be added to the FrameworkFields attribute
	 * @param  existingDoc    The feature to be added to the FrameworkFields attribute
	 * @exception  Exception  If an error occurs
	 */
	protected final void addFrameworkFields(Document newDoc, Document existingDoc) throws Exception {
		
		// Add basic text to the default fields:
		String title = getTitle();
		addToDefaultField(title);
		addToAdminDefaultField(title);	
		String description = getDescription();
		addToDefaultField(description);
		addToAdminDefaultField(description);	
		
		String terms;

		terms = getFieldContent(getGradeRange(), "gradeRange", metadataFormat);
		if (terms != null && terms.trim().length() > 0)
			newDoc.add(new Field(getFieldName("gradeRange", metadataFormat), terms, Field.Store.YES, Field.Index.ANALYZED));

		terms = getFieldContent(getGradeRange(), null, metadataFormat);
		if (terms != null && terms.trim().length() > 0)
			newDoc.add(new Field("gradeRange", terms, Field.Store.YES, Field.Index.ANALYZED));

		terms = getFieldContent(getResourceTypes(), "resourceType", metadataFormat);
		if (terms != null && terms.trim().length() > 0)
			newDoc.add(new Field(getFieldName("resourceType", metadataFormat), terms, Field.Store.YES, Field.Index.ANALYZED));
		terms = getFieldContent(getResourceTypes(), null, metadataFormat);
		if (terms != null && terms.trim().length() > 0)
			newDoc.add(new Field("resourceType", terms, Field.Store.YES, Field.Index.ANALYZED));
		terms = getTermStringFromStringArray(getResourceTypes());
		if (terms != null && terms.trim().length() > 0)
			addToDefaultField(terms);

		terms = getFieldContent(getSubjects(), "subject", metadataFormat);
		if (terms != null && terms.trim().length() > 0)
			newDoc.add(new Field(getFieldName("subject", metadataFormat), terms, Field.Store.YES, Field.Index.ANALYZED));
		terms = getFieldContent(getSubjects(), null, metadataFormat);
		if (terms != null && terms.trim().length() > 0)
			newDoc.add(new Field("subject", terms, Field.Store.YES, Field.Index.ANALYZED));
		terms = getTermStringFromStringArray(getSubjects());
		if (terms != null && terms.trim().length() > 0)
			addToDefaultField(terms);

		terms = getFieldContent(getContentStandards(), "contentStandard", metadataFormat);
		if (terms != null && terms.trim().length() > 0)
			newDoc.add(new Field(getFieldName("contentStandard", metadataFormat), terms, Field.Store.YES, Field.Index.ANALYZED));
		terms = getFieldContent(getContentStandards(), null, metadataFormat);
		if (terms != null && terms.trim().length() > 0)
			newDoc.add(new Field("contentStandard", terms, Field.Store.YES, Field.Index.ANALYZED));

		terms = getPlaceNames();
		if (terms != null && terms.trim().length() > 0) {
			newDoc.add(new Field("placeNames", terms, Field.Store.YES, Field.Index.ANALYZED));
			addToDefaultField(terms);
		}

		terms = getEventNames();
		if (terms != null && terms.trim().length() > 0) {
			newDoc.add(new Field("eventNames", terms, Field.Store.YES, Field.Index.ANALYZED));
			addToDefaultField(terms);
		}

		terms = getTemporalCoverageNames();
		if (terms != null && terms.trim().length() > 0) {
			newDoc.add(new Field("temporalCoverageNames", terms, Field.Store.YES, Field.Index.ANALYZED));
			addToDefaultField(terms);
		}

		terms = getOrganizationInstName();
		if (terms != null && terms.trim().length() > 0) {
			newDoc.add(new Field("organizationInstName", terms, Field.Store.YES, Field.Index.ANALYZED));
			//addToDefaultField(terms);
		}

		terms = getOrganizationInstDepartment();
		if (terms != null && terms.trim().length() > 0) {
			newDoc.add(new Field("organizationInstDepartment", terms, Field.Store.YES, Field.Index.ANALYZED));
			//addToDefaultField(terms);
		}

		terms = getPersonInstName();
		if (terms != null && terms.trim().length() > 0) {
			newDoc.add(new Field("personInstName", terms, Field.Store.YES, Field.Index.ANALYZED));
			//addToDefaultField(terms);
		}

		terms = getPersonInstDepartment();
		if (terms != null && terms.trim().length() > 0) {
			newDoc.add(new Field("personInstDepartment", terms, Field.Store.YES, Field.Index.ANALYZED));
			//addToDefaultField(terms);
		}

		terms = getCreatorEmailPrimary();
		if (terms != null && terms.trim().length() > 0)
			newDoc.add(new Field("emailPrimary", terms, Field.Store.YES, Field.Index.ANALYZED));

		terms = getCreatorEmailAlt();
		if (terms != null && terms.trim().length() > 0)
			newDoc.add(new Field("emailAlt", terms, Field.Store.YES, Field.Index.ANALYZED));

		terms = getOrganizationEmail();
		if (terms != null && terms.trim().length() > 0)
			newDoc.add(new Field("emailOrganization", terms, Field.Store.YES, Field.Index.ANALYZED));

		terms = getCost();
		if (terms != null && terms.trim().length() > 0)
			newDoc.add(new Field("cost", terms, Field.Store.YES, Field.Index.ANALYZED));

		terms = getAudienceToolFor();
		if (terms != null && terms.trim().length() > 0)
			newDoc.add(new Field("itemAudienceToolFor", terms, Field.Store.YES, Field.Index.ANALYZED));

		terms = getAudienceBeneficiary();
		if (terms != null && terms.trim().length() > 0)
			newDoc.add(new Field("itemAudienceBeneficiary", terms, Field.Store.YES, Field.Index.ANALYZED));

		terms = getAudienceInstructionalGoal();
		if (terms != null && terms.trim().length() > 0)
			newDoc.add(new Field("itemAudienceInstructionalGoal", terms, Field.Store.YES, Field.Index.ANALYZED));

		terms = getAudienceTeachingMethod();
		if (terms != null && terms.trim().length() > 0)
			newDoc.add(new Field("itemAudienceTeachingMethod", terms, Field.Store.YES, Field.Index.ANALYZED));

		terms = getAudienceTypicalAgeRange();
		if (terms != null && terms.trim().length() > 0)
			newDoc.add(new Field("itemAudienceTypicalAgeRange", terms, Field.Store.YES, Field.Index.ANALYZED));

		terms = getUrlMirrors();
		if (terms != null && terms.trim().length() > 0)
			newDoc.add(new Field("urlMirrorsEncoded", terms, Field.Store.NO, Field.Index.ANALYZED));

		if (adnXmlDoc.length > 1)
			newDoc.add(new Field("multirecord", "true", Field.Store.YES, Field.Index.ANALYZED));
		else
			newDoc.add(new Field("multirecord", "false", Field.Store.YES, Field.Index.ANALYZED));

	}


	/**
	 *  Sets the whether this writer should write a single record doc rather than a multi-item doc.
	 *
	 * @param  isSingleDoc  The new isSingleDoc value
	 */
	public void setIsSingleDoc(boolean isSingleDoc) {
		singleRecord = isSingleDoc;
	}


	private void indexIndividualDupDoc(String id, String coll, File sourceFile) throws Exception {
		SimpleLuceneIndex dupItemsIndex =
			(SimpleLuceneIndex) getFileIndexingService().getAttribute("dupItemsIndex");

		ResultDocList thisDoc = dupItemsIndex.searchDocs("id:" + SimpleLuceneIndex.encodeToTerm(id));
		Document myExistingDoc = null;
		if (thisDoc != null && thisDoc.size() > 0)
			myExistingDoc = ((ResultDoc)thisDoc.get(0)).getDocument();

		ADNFileIndexingWriter adnWriter = new ADNFileIndexingWriter(true);

		// Create a new config for the de-dup writer and set the dup colletcion to it...
		// Shallow copy so we don't change the original config...
		HashMap newConfig = (HashMap) getConfigAttributes().clone();
		newConfig.put("collection", coll);
		adnWriter.setConfigAttributes(newConfig);
		adnWriter.setIsSingleDoc(true);

		Document doc = null;
		FileIndexingServiceData newData = null;

		try {
			newData = adnWriter.create(sourceFile, myExistingDoc, getFileIndexingPlugin(), null);
			doc = newData.getDoc();
		} catch (Throwable e) {
			e.printStackTrace();
			throw new Exception("Error indexing multi-record item " + id + ": " + e.getMessage());
		}

		/*
		 *  prtln("ID terms:\n");
		 *  List idTerms = dupItemsIndex.getTerms("id");
		 *  for(int i = 0; i< idTerms.size();i++)
		 *  prtln("   " + (String)idTerms.get(i));
		 */
		//prtln("dup doc updateting id " + id);

		if (doc != null) {
			//prtln("removing doc id: " + id);
			addDocToRemove("id", SimpleLuceneIndex.encodeToTerm(id));
			dupItemsIndex.update("id", SimpleLuceneIndex.encodeToTerm(id), doc, true);
		}
	}



	// -------------- Essential controlled vocabs (These should be moved to itemFileInexingWriter... ----------------

	/**
	 *  Gets the gradeRange attribute of the ADNFileIndexingWriter object
	 *
	 * @return    The gradeRange value
	 */
	protected String[] getGradeRange() {
		ArrayList vals = new ArrayList();

		for (int i = 0; i < adnXmlDoc.length; i++) {
			String[] tmp = adnXmlDoc[i].getXmlFields("educational/audiences/audience/gradeRange");
			if (tmp != null)
				for (int j = 0; j < tmp.length; j++)
					if (!vals.contains(tmp[j]))
						vals.add(tmp[j]);
		}
		return (String[]) vals.toArray(new String[]{});
	}


	/**
	 *  Gets the resourceTypes attribute of the ADNFileIndexingWriter object
	 *
	 * @return    The resourceTypes value
	 */
	protected String[] getResourceTypes() {
		ArrayList vals = new ArrayList();

		for (int i = 0; i < adnXmlDoc.length; i++) {
			String[] tmp = adnXmlDoc[i].getXmlFields("educational/resourceTypes/resourceType");
			if (tmp != null)
				for (int j = 0; j < tmp.length; j++)
					if (!vals.contains(tmp[j]))
						vals.add(tmp[j]);
		}
		return (String[]) vals.toArray(new String[]{});
	}


	/**
	 *  Gets the contentStandards attribute of the ADNFileIndexingWriter object
	 *
	 * @return    The contentStandards value
	 */
	protected String[] getContentStandards() {
		ArrayList vals = new ArrayList();

		for (int i = 0; i < adnXmlDoc.length; i++) {
			String[] tmp = adnXmlDoc[i].getXmlFields("educational/contentStandards/contentStandard");
			if (tmp != null) {
				for (int j = 0; j < tmp.length; j++) {
					String std = tmp[j];
					if (!vals.contains(std))
						vals.add(std);
					// Check for 4th level standards and index them as 3rd for searching...
					if (std.startsWith("NSES") && std.matches(".*:.*:.*:.*:.*")) {
						std = std.substring(0, std.lastIndexOf(":"));
						if (!vals.contains(std))
							vals.add(std);
					}
				}
			}
		}
		return (String[]) vals.toArray(new String[]{});
	}


	/**
	 *  Gets the subjects attribute of the ADNFileIndexingWriter object
	 *
	 * @return    The subjects value
	 */
	protected String[] getSubjects() {
		ArrayList vals = new ArrayList();

		for (int i = 0; i < adnXmlDoc.length; i++) {
			String[] tmp = adnXmlDoc[i].getXmlFields("general/subjects/subject");
			if (tmp != null)
				for (int j = 0; j < tmp.length; j++)
					if (!vals.contains(tmp[j]))
						vals.add(tmp[j]);
		}
		return (String[]) vals.toArray(new String[]{});
	}

	// -------------- end Essential controlled vocabs ----------------

	// -------------- ADN specific fields ----------------

	/**
	 *  Gets the creator's primary email.
	 *
	 * @return                The creator's primary email.
	 * @exception  Exception  This method should throw and Exception with appropriate error message if an error
	 *      occurs.
	 */
	protected String getCreatorEmailPrimary() throws Exception {
		String val = "";
		for (int i = 0; i < adnXmlDoc.length; i++) {
			String[] emails = adnXmlDoc[i].getXmlFields("lifecycle/contributors/contributor/person/emailPrimary");

			for (int j = 0; j < emails.length; j++)
				if (val.indexOf(emails[j].trim()) == -1)
					val += " " + emails[j].trim();
		}
		//prtln("creators emailPrimary: " + val);
		return val.trim();
	}


	/**
	 *  Gets the creator's alternate email.
	 *
	 * @return                The creator's alternate email.
	 * @exception  Exception  This method should throw and Exception with appropriate error message if an error
	 *      occurs.
	 */
	protected String getCreatorEmailAlt() throws Exception {
		String val = "";
		for (int i = 0; i < adnXmlDoc.length; i++) {
			String[] emails = adnXmlDoc[i].getXmlFields("lifecycle/contributors/contributor/person/emailAlt");

			for (int j = 0; j < emails.length; j++)
				if (val.indexOf(emails[j].trim()) == -1)
					val += " " + emails[j].trim();
		}
		//prtln("creators emailAlt: " + val);
		return val.trim();
	}


	/**
	 *  Gets the oraganization email.
	 *
	 * @return                The oraganization email.
	 * @exception  Exception  This method should throw and Exception with appropriate error message if an error
	 *      occurs.
	 */
	protected String getOrganizationEmail() throws Exception {
		String val = "";
		for (int i = 0; i < adnXmlDoc.length; i++) {
			String[] emails = adnXmlDoc[i].getXmlFields("lifecycle/contributors/contributor/organization/instEmail");

			for (int j = 0; j < emails.length; j++)
				if (val.indexOf(emails[j].trim()) == -1)
					val += " " + emails[j].trim();
		}
		//prtln("organization emailAlt: " + val);
		return val.trim();
	}


	/**
	 *  Gets the oraganizations institution name. ADN xPath lifecycle/contributors/contributor/organization/instName
	 *
	 * @return                The oraganization name.
	 * @exception  Exception  This method should throw and Exception with appropriate error message if an error
	 *      occurs.
	 */
	protected String getOrganizationInstName() throws Exception {
		String val = "";
		for (int i = 0; i < adnXmlDoc.length; i++) {
			String[] vals = adnXmlDoc[i].getXmlFields("lifecycle/contributors/contributor/organization/instName");

			for (int j = 0; j < vals.length; j++)
				if (val.indexOf(vals[j].trim()) == -1)
					val += " " + vals[j].trim();
		}
		//prtln("organization instName: " + val);
		return val.trim();
	}


	/**
	 *  Gets the oraganizations institution department name. ADN xPath
	 *  lifecycle/contributors/contributor/organization/instDept
	 *
	 * @return                The oraganizations institution department name.
	 * @exception  Exception  This method should throw and Exception with appropriate error message if an error
	 *      occurs.
	 */
	protected String getOrganizationInstDepartment() throws Exception {
		String val = "";
		for (int i = 0; i < adnXmlDoc.length; i++) {
			String[] vals = adnXmlDoc[i].getXmlFields("lifecycle/contributors/contributor/organization/instDept");

			for (int j = 0; j < vals.length; j++)
				if (val.indexOf(vals[j].trim()) == -1)
					val += " " + vals[j].trim();
		}
		//prtln("organization instDept: " + val);
		return val.trim();
	}



	/**
	 *  Gets the persons institution name. ADN xPath lifecycle/contributors/contributor/person/instName
	 *
	 * @return                The institution name.
	 * @exception  Exception  This method should throw and Exception with appropriate error message if an error
	 *      occurs.
	 */
	protected String getPersonInstName() throws Exception {
		String val = "";
		for (int i = 0; i < adnXmlDoc.length; i++) {
			String[] vals = adnXmlDoc[i].getXmlFields("lifecycle/contributors/contributor/person/instName");

			for (int j = 0; j < vals.length; j++)
				if (val.indexOf(vals[j].trim()) == -1)
					val += " " + vals[j].trim();
		}
		//prtln("organization instName: " + val);
		return val.trim();
	}


	/**
	 *  Gets the persons institution department name. ADN xPath lifecycle/contributors/contributor/person/instDept
	 *
	 * @return                The institution department name.
	 * @exception  Exception  This method should throw and Exception with appropriate error message if an error
	 *      occurs.
	 */
	protected String getPersonInstDepartment() throws Exception {
		String val = "";
		for (int i = 0; i < adnXmlDoc.length; i++) {
			String[] vals = adnXmlDoc[i].getXmlFields("lifecycle/contributors/contributor/person/instDept");

			for (int j = 0; j < vals.length; j++)
				if (val.indexOf(vals[j].trim()) == -1)
					val += " " + vals[j].trim();
		}
		//prtln("organization instDept: " + val);
		return val.trim();
	}


	/**
	 *  Gets the mirror URLs encoded as terms, if any.
	 *
	 * @return                The URL mirrors encoded as terms, or empty string.
	 * @exception  Exception  This method should throw and Exception with appropriate error message if an error
	 *      occurs.
	 */
	protected String getUrlMirrors() throws Exception {
		String val = "";
		for (int i = 0; i < adnXmlDoc.length; i++) {
			String[] mirrorUrl = adnXmlDoc[i].getXmlFields("technical/online/mirrorURLs/mirrorURL");

			for (int j = 0; j < mirrorUrl.length; j++)
				if (val.indexOf(mirrorUrl[j].trim()) == -1)
					val += " " + SimpleLuceneIndex.encodeToTerm(mirrorUrl[j].trim());
		}
		return val.trim();
	}


	/**
	 *  The audience tool for.
	 *
	 * @return                The audience tool for.
	 * @exception  Exception  This method should throw and Exception with appropriate error message if an error
	 *      occurs.
	 */
	protected String getAudienceToolFor() throws Exception {
		return doMakePhrasesFromPath("educational/audiences/audience/toolFor");
	}


	/**
	 *  The audience beneficiary.
	 *
	 * @return                The audience beneficiary.
	 * @exception  Exception  This method should throw and Exception with appropriate error message if an error
	 *      occurs.
	 */
	protected String getAudienceBeneficiary() throws Exception {
		return doMakePhrasesFromPath("educational/audiences/audience/beneficiary");
	}


	/**
	 *  The audience typical age range.
	 *
	 * @return                The audience typical age range.
	 * @exception  Exception  This method should throw and Exception with appropriate error message if an error
	 *      occurs.
	 */
	protected String getAudienceTypicalAgeRange() throws Exception {
		return doMakePhrasesFromPath("educational/audiences/audience/typicalAgeRange");
	}


	/**
	 *  The audience instructionalGoal.
	 *
	 * @return                The audience instructionalGoal.
	 * @exception  Exception  This method should throw and Exception with appropriate error message if an error
	 *      occurs.
	 */
	protected String getAudienceInstructionalGoal() throws Exception {
		return doMakePhrasesFromPath("educational/audiences/audience/instructionalGoal");
	}


	/**
	 *  The audience teachingMethod.
	 *
	 * @return                The audience teachingMethod.
	 * @exception  Exception  This method should throw and Exception with appropriate error message if an error
	 *      occurs.
	 */
	protected String getAudienceTeachingMethod() throws Exception {
		return doMakePhrasesFromPath("educational/audiences/audience/teachingMethods/teachingMethod");
	}


	/**
	 *  Takes the XPath provided and extracts each node at that level from each of the multi-docs and converts it
	 *  to an indexable phrase.
	 *
	 * @param  xPath  The XPath
	 * @return        The indexable phrase
	 */
	private String doMakePhrasesFromPath(String xPath) {
		List vals = new ArrayList();
		for (int i = 0; i < adnXmlDoc.length; i++) {
			String[] phrase = adnXmlDoc[i].getXmlFields(xPath);
			for (int j = 0; j < phrase.length; j++) {
				if (!vals.contains(phrase[j]))
					vals.add(phrase[j]);
			}
		}
		String result = IndexingTools.makeSeparatePhrasesFromStrings(vals);
		return result;
	}


	/**
	 *  Gets all place names as text. Place names are extracted from the following XPaths: <nobr>
	 *  general/simplePlacesAndEvents/placeAndEvent/place,</nobr> <nobr>
	 *  geospatialCoverages/geospatialCoverage/boundBox/bbPlaces/place/name</nobr> and <nobr>
	 *  geospatialCoverages/geospatialCoverage/detGeos/detGeo/detPlaces/place/name.</nobr>
	 *
	 * @return    All place names as text.
	 */
	protected String getPlaceNames() {
		ArrayList vals = new ArrayList();

		for (int i = 0; i < adnXmlDoc.length; i++) {
			String[] tmp = adnXmlDoc[i].getXmlFields("general/simplePlacesAndEvents/placeAndEvent/place");
			if (tmp != null)
				for (int j = 0; j < tmp.length; j++)
					if (!vals.contains(tmp[j]))
						vals.add(tmp[j]);

			tmp = adnXmlDoc[i].getXmlFields("geospatialCoverages/geospatialCoverage/boundBox/bbPlaces/place/name");
			if (tmp != null)
				for (int j = 0; j < tmp.length; j++)
					if (!vals.contains(tmp[j]))
						vals.add(tmp[j]);

			tmp = adnXmlDoc[i].getXmlFields("geospatialCoverages/geospatialCoverage/detGeos/detGeo/detPlaces/place/name");
			if (tmp != null)
				for (int j = 0; j < tmp.length; j++)
					if (!vals.contains(tmp[j]))
						vals.add(tmp[j]);
		}

		String out = "";
		for (int i = 0; i < vals.size(); i++)
			out += vals.get(i) + " ";

		return out;
	}


	/**
	 *  Gets all event names as text. Event names are extracted from the following XPaths: <nobr>
	 *  general/simplePlacesAndEvents/placeAndEvent/event,</nobr> <nobr>
	 *  geospatialCoverages/geospatialCoverage/boundBox/bbEvents/event/name</nobr> and <nobr>
	 *  geospatialCoverages/geospatialCoverage/detGeos/detGeo/detEvents/event/name.</nobr>
	 *
	 * @return    All event names as text.
	 */
	protected String getEventNames() {
		ArrayList vals = new ArrayList();

		for (int i = 0; i < adnXmlDoc.length; i++) {
			String[] tmp = adnXmlDoc[i].getXmlFields("general/simplePlacesAndEvents/placeAndEvent/event");
			if (tmp != null)
				for (int j = 0; j < tmp.length; j++)
					if (!vals.contains(tmp[j]))
						vals.add(tmp[j]);

			tmp = adnXmlDoc[i].getXmlFields("geospatialCoverages/geospatialCoverage/boundBox/bbEvents/event/name");
			if (tmp != null)
				for (int j = 0; j < tmp.length; j++)
					if (!vals.contains(tmp[j]))
						vals.add(tmp[j]);

			tmp = adnXmlDoc[i].getXmlFields("geospatialCoverages/geospatialCoverage/detGeos/detGeo/detEvents/event/name");
			if (tmp != null)
				for (int j = 0; j < tmp.length; j++)
					if (!vals.contains(tmp[j]))
						vals.add(tmp[j]);
		}

		String out = "";
		for (int i = 0; i < vals.size(); i++)
			out += vals.get(i) + " ";

		return out;
	}


	/**
	 *  Gets all temporal coverage names as text. Temporal coverage names are extracted from the following
	 *  XPaths: <nobr>general/simpleTemporalCoverages/description,</nobr> and <nobr>
	 *  temporalCoverages/timeAndPeriod/periods/period/name.</nobr>
	 *
	 * @return    All temporal coverage names as text.
	 */
	protected String getTemporalCoverageNames() {
		ArrayList vals = new ArrayList();

		for (int i = 0; i < adnXmlDoc.length; i++) {
			String[] tmp = adnXmlDoc[i].getXmlFields("general/simpleTemporalCoverages/description");
			if (tmp != null)
				for (int j = 0; j < tmp.length; j++)
					if (!vals.contains(tmp[j]))
						vals.add(tmp[j]);

			tmp = adnXmlDoc[i].getXmlFields("temporalCoverages/timeAndPeriod/periods/period/name");
			if (tmp != null)
				for (int j = 0; j < tmp.length; j++)
					if (!vals.contains(tmp[j]))
						vals.add(tmp[j]);

		}

		String out = "";
		for (int i = 0; i < vals.size(); i++)
			out += vals.get(i) + " ";

		return out;
	}
}

