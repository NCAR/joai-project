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
import org.dlese.dpc.index.reader.*;
import org.dlese.dpc.services.mmd.MmdRec;
import org.dlese.dpc.util.*;
import org.apache.lucene.document.*;
import org.dlese.dpc.repository.*;
import org.dlese.dpc.vocab.*;
import org.dlese.dpc.index.document.DateFieldTools;

import org.dlese.dpc.util.HTMLParser;

/**
 *  Abstract class for writing a Lucene {@link org.apache.lucene.document.Document} for a collection of
 *  item-level metadata records of a specific format (DLESE IMS, ADN-Item, ADN-Collection, etc). The reader
 *  for this type of {@link org.apache.lucene.document.Document} is {@link
 *  org.dlese.dpc.index.reader.XMLDocReader} or {@link org.dlese.dpc.index.reader.ItemDocReader}. <p>
 *
 *  <br>
 *  <b>The Lucene {@link org.apache.lucene.document.Document} fields that are created by this class are (in
 *  addition the the ones listed for {@link org.dlese.dpc.index.writer.FileIndexingServiceWriter}):</b> <br>
 *  <br>
 *  <code><b>title</b> </code> - The tile for the resource. Stored.<br>
 *  <code><b>description</b> </code> - The description for the resource. Stored. <br>
 *  <code><b>url</b> </code> - The url to the resoruce. Stored.<br>
 *  Stored. Appended with a '0' at the beginning to support wildcard searching. <br>
 *  <code><b>metadatapfx</b> </code> - The metadata prefix (format) for this record, for example 'adn' or
 *  'oai_dc'. Stored. Appended with a '0' at the beginning to support wildcard searching.<br>
 *  <code><b>accessionstatus</b> </code> - The accession status for this record. Stored. Appended with a '0'
 *  at the beginning to support wildcard searching. <br>
 *  <code><b>annotypes</b> </code> - Annotataion types that are refer to this record. Keyword. <br>
 *  <code><b>annopathways</b> </code> - Annotataion pathways that are refer to this record. Keyword. <br>
 *  <code><b>associatedids</b> </code> - A list of record IDs that refer to the same resource. Keyword. <br>
 *  <code><b>valid</b> </code> - Indicates whether the record is valid [true | false]. Not stored.<br>
 *  <code><b>validationreport</b> </code> - Text describing an error in the validation of the data for this
 *  record. Stored. Only indexed if there was a validation error indicated by the valid field containing
 *  false. <br>
 *  <br>
 *
 *
 * @author    John Weatherley
 * @see       org.dlese.dpc.index.reader.ItemDocReader
 * @see       org.dlese.dpc.index.reader.XMLDocReader
 * @see       org.dlese.dpc.repository.RecordDataService
 * @see       org.dlese.dpc.index.writer.FileIndexingServiceWriter
 */
public abstract class ItemFileIndexingWriter extends XMLFileIndexingWriter {

	// My id's include this record's ID at the first position. If this is a multi-doc
	// then additional IDs exist after that, if single-doc, only one appears.
	private String[] myIDs = null;

	// Get other items that reference the same resource
	private String[] associatedIds = null;
	private String[] identifiedErrors = null;
	private ResultDocList assocItemResultDocs = null;

	// All Ids contains the ID for this item plus all associated IDs.
	private String[] allIds = null;

	// Get annotation for this record only. If I am a multi-doc, these include all annos for all records
	private ResultDocList _myAnnoResultDocs = null;

	// Get ALL annotations for this resource from all records associated with the resource
	// regardless of whether I am a multi-doc
	private ResultDocList allAnnoResultDocs = null;

	private Date wnDate = null;
	private String wnType = null;

	// --------------- Abstract methods for subclasses --------------------

	/**
	 *  Returns the item's keywords sorted and separated by the '+' symbol. An empty String or null is
	 *  acceptable. The String is tokenized, stored and indexed under the field key 'keywords' and is also
	 *  indexed in the 'default' field.
	 *
	 * @return                The keywords String
	 * @exception  Exception  This method should throw and Exception with appropriate error message if an error
	 *      occurs.
	 */
	protected abstract String getKeywords() throws Exception;


	/**
	 *  Returns the items creator's last name. An empty String or null is acceptable. The String is tokenized,
	 *  stored and indexed under the field the 'default' field only.
	 *
	 * @return                The creator's last name String
	 * @exception  Exception  This method should throw and Exception with appropriate error message if an error
	 *      occurs.
	 */
	protected abstract String getCreatorLastName() throws Exception;


	/**
	 *  Returns the items creator's full name. An empty String or null is acceptable. The String is tokenized,
	 *  stored and indexed under the field key 'creator'.
	 *
	 * @return                Creator's full name
	 * @exception  Exception  This method should throw and Exception with appropriate error message if an error
	 *      occurs.
	 */
	protected abstract String getCreator() throws Exception;


	/**
	 *  Returns the accession status of this record, for example 'accessioned'. The String is tokenized, stored
	 *  and indexed under the field key 'accessionstatus'.
	 *
	 * @return                The accession status.
	 * @exception  Exception  This method should throw and Exception with appropriate error message if an error
	 *      occurs.
	 */
	protected abstract String getAccessionStatus() throws Exception;


	/**
	 *  Returns the accession date for the item, or null if this item is not accessioned.
	 *
	 * @return                The accession date for the item, or null if this item is not accessioned.
	 * @exception  Exception  This method should throw and Exception with appropriate error message if an error
	 *      occurs.
	 */
	protected abstract Date getAccessionDate() throws Exception;


	/**
	 *  Returns the date this item was first created, or null if not available.
	 *
	 * @return                The item creation date or null
	 * @exception  Exception  This method should throw and Exception with appropriate error message if an error
	 *      occurs.
	 */
	protected abstract Date getCreationDate() throws Exception;


	/**
	 *  Returns the content of the item this record catalogs, or null if not available. For example the full HTML
	 *  text of the Web page.
	 *
	 * @return    The content of the item, or null
	 */
	protected abstract String getContent();


	/**
	 *  Returns the MmdRecs for records in other collections that catalog the same resource. Does not include
	 *  myMmdRec.
	 *
	 * @return    The associated MmdRecs, null or empty if none
	 */
	protected abstract MmdRec[] getAssociatedMmdRecs();


	/**
	 *  Returns the MmdRecs for all records associated with this resouce, including myMmdRec.
	 *
	 * @return    All MmdRecs for this resource, null or empty if none
	 */
	protected abstract MmdRec[] getAllMmdRecs();


	/**
	 *  Returns the MmdRec for this record only.
	 *
	 * @return    The MmdRec for this record, or null
	 */
	protected abstract MmdRec getMyMmdRec();


	/**
	 *  Returns the content type of the item this record catalogs, or null if not available. For example
	 *  "text/html" or "html".
	 *
	 * @return    The content type of the item, or null
	 */
	protected abstract String getContentType();


	/**
	 *  Returns true if the item has one or more related resource, false otherwise.
	 *
	 * @return                True if the item has one or more related resource, false otherwise.
	 * @exception  Exception  This method should throw and Exception with appropriate error message if an error
	 *      occurs.
	 */
	protected abstract boolean getHasRelatedResource() throws Exception;


	/**
	 *  Returns the IDs of related resources that are cataloged by ID, or null if none are present
	 *
	 * @return                Related resource IDs, or null if none are available
	 * @exception  Exception  This method should throw and Exception with appropriate error message if an error
	 *      occurs.
	 */
	protected abstract String[] getRelatedResourceIds() throws Exception;


	/**
	 *  Returns the URLs of related resources that are cataloged by URL, or null if none are present
	 *
	 * @return                Related resource URLs, or null if none are available
	 * @exception  Exception  This method should throw and Exception with appropriate error message if an error
	 *      occurs.
	 */
	protected abstract String[] getRelatedResourceUrls() throws Exception;


	/**
	 *  Adds fields to the index that are unique to the given framework.<p>
	 *
	 *  Example code:<br>
	 *  <code>protected void addFrameworkFields(Document newDoc, Document existingDoc) throws Exception {</code>
	 *  <br>
	 *  &nbsp;<code> String customContent = "Some content";</code><br>
	 *  &nbsp;<code> newDoc.add(new Field("mycustomefield", customContent));</code> <br>
	 *  <code>}</code>
	 *
	 * @param  newDoc         The new {@link org.apache.lucene.document.Document} that is being created for this
	 *      resource
	 * @param  existingDoc    An existing {@link org.apache.lucene.document.Document} that currently resides in
	 *      the index for the given resource, or null if none was previously present
	 * @exception  Exception  This method should throw and Exception with appropriate error message if an error
	 *      occurs.
	 */
	protected abstract void addFrameworkFields(Document newDoc, Document existingDoc) throws Exception;


	// --------------- Abstract methods reconstituted from FileIndexingServiceWriter --------------------

	/**
	 *  Returns a unique document type key for this kind of record, corresponding to the format type. For example
	 *  "adn," "dlese_ims," or "dlese_anno". The string is parsed using the Lucene {@link
	 *  org.apache.lucene.analysis.standard.StandardAnalyzer} so it must be lowercase and should not contain any
	 *  stop words.
	 *
	 * @return                The docType String
	 * @exception  Exception  This method should throw and Exception with appropriate error message if an error
	 *      occurs.
	 */
	public abstract String getDocType() throws Exception;


	/**
	 *  Gets the fully qualified name of the concrete {@link org.dlese.dpc.index.reader.DocReader} class that is
	 *  used to read this type of {@link org.apache.lucene.document.Document}, for example
	 *  "org.dlese.dpc.index.reader.ItemDocReader".
	 *
	 * @return    The name of the {@link org.dlese.dpc.index.reader.DocReader}.
	 */
	public abstract String getReaderClass();


	/**
	 *  This method is called prior to processing and may be used to for any necessary set-up. This method should
	 *  throw and exception with appropriate message if an error occurs.
	 *
	 * @param  source         The source file being indexed
	 * @param  existingDoc    An existing Document that currently resides in the index for the given resource, or
	 *      null if none was previously present
	 * @exception  Exception  If an error occured during set-up.
	 */
	public abstract void initItem(File source, Document existingDoc) throws Exception;


	/**  This method is called at the conclusion of processing and may be used for tear-down. */
	protected abstract void destroy();


	/**
	 *  Gets a report detailing any errors found in the validation of the data, or null if no error was found.
	 *  This could be implemented by simply performing XML schema validation on the file, or can involve more
	 *  customized validation of the data if necessary. This method is called after all other methods that access
	 *  the data ({@link #getTitle()}, {@link #addFrameworkFields(Document, Document)}, etc.) so that data
	 *  verification can be done during those calls, if needed.
	 *
	 * @return                Null if no data validation errors were found, otherwise a String that details the
	 *      nature of the error.
	 * @exception  Exception  If error in performing the validation.
	 */
	protected abstract String getValidationReport() throws Exception;


	// --------------- Inherited methods --------------------


	/**
	 *  Initialize the subclasses and record data service data.
	 *
	 * @param  source         The source file being indexed.
	 * @param  existingDoc    A Document that previously existed in the index for this item, if present
	 * @exception  Exception  Thrown if error reading the XML map
	 */
	public void init(File source, Document existingDoc)
		 throws Exception {

		// Initialize my subclasses
		initItem(source, existingDoc);

		// My id's include this record's ID at the first position. If this is a multi-doc
		// then additional IDs exist after that, if single-doc, only one appears.
		myIDs = _getIds();

		RecordDataService recordDataService = getRecordDataService();

		if (recordDataService != null) {

			// Get other items that reference the same resource
			associatedIds = recordDataService.getAssociatedIDs(myIDs[0], getAssociatedMmdRecs());
			identifiedErrors = recordDataService.getIdentifiedErrors(getAllMmdRecs());
			assocItemResultDocs = recordDataService.getItemResultDocs(associatedIds);

			ArrayList tmpAllIds = new ArrayList();
			for (int i = 0; i < myIDs.length; i++)
				tmpAllIds.add(myIDs[i]);
			if (associatedIds != null) {
				for (int i = 0; i < associatedIds.length; i++)
					if (!tmpAllIds.contains(associatedIds[i]))
						tmpAllIds.add(associatedIds[i]);
			}
			// All Ids contains the ID for this item plus all associated IDs.
			allIds = (String[]) tmpAllIds.toArray(new String[]{});

			// Get ALL annotations for this resource from all records associated with the resource
			// regardless of whether I am a multi-doc
			allAnnoResultDocs = recordDataService.getDleseAnnoResultDocs(allIds);

			// If not recordDataService, clear the data
		}
		else {
			associatedIds = null;
			identifiedErrors = null;
			assocItemResultDocs = null;
			allIds = null;
			allAnnoResultDocs = null;
		}

		// Determine the What's new date and type:
		DleseCollectionDocReader myCollectionDoc = getMyCollectionDoc();
		if (myCollectionDoc != null && myCollectionDoc.isPartOfDRC())
			wnType = "itemannocomplete";
		else
			wnType = "itemnew";

		// If we have not been accessioned yet, use the creation date...
		wnDate = getAccessionDate();
		if (wnDate == null)
			wnDate = getCreationDate();

		// Use the later of my accession date and my collection's accession date
		if (myCollectionDoc != null) {
			Date myCollectionAccessionDate = myCollectionDoc.getAccessionDateDate();
			if (wnDate != null && myCollectionAccessionDate != null && myCollectionAccessionDate.compareTo(wnDate) == 1)
				wnDate = myCollectionAccessionDate;
		}

		ResultDocList myAnnoResultDocs = getMyAnnoResultDocs();

		// Use the later of my accession date and the wnDates of my annotations
		if (wnDate != null && myAnnoResultDocs != null && myAnnoResultDocs.size() > 0) {
			Date annoWnDate;
			String annoWnType;
			for (int i = 0; i < myAnnoResultDocs.size(); i++) {
				DleseAnnoDocReader annoDocReader = (DleseAnnoDocReader) myAnnoResultDocs.get(i).getDocReader();
				annoWnDate = annoDocReader.getWhatsNewDateDate();

				// If the annotatation's whats' new date is later and is part of DRC, use it
				if (annoWnDate != null && annoWnDate.compareTo(wnDate) == 1) {
					annoWnType = annoDocReader.getWhatsNewType();
					if (annoWnType.equals("drcannocomplete")) {
						wnType = "itemannocomplete";
						wnDate = annoWnDate;
					}
					else if (annoWnType.equals("drcannoinprogress")) {
						wnType = "itemannoinprogress";
						wnDate = annoWnDate;
					}
				}
			}
		}
	}


	/**
	 *  Gets the annotations for this record, null or zero length if none available. Overrides method in
	 *  XMLFileIndexingWriter because IDs need initializing.
	 *
	 * @return                The myAnnoResultDocs value
	 * @exception  Exception  If error
	 */
	protected ResultDocList getMyAnnoResultDocs() throws Exception {
		if (_myAnnoResultDocs == null) {
			RecordDataService recordDataService = getRecordDataService();
			if (recordDataService != null) {
				// Get annotation for this record only. If I am a multi-doc, these (should) include all annos for all records
				_myAnnoResultDocs = recordDataService.getDleseAnnoResultDocs(_getIds());
			}
		}
		return _myAnnoResultDocs;
	}


	/**
	 *  Adds fields to the index that are common to all item-level documents. These include the title,
	 *  description, id and url as well as collection, accession status, annotation references, and
	 *  collection(s).
	 *
	 * @param  newDoc         The new Document that is being created for this resource
	 * @param  existingDoc    An existing Document that currently resides in the index for the given resource, or
	 *      null if none was previously present
	 * @param  sourceFile     The sourceFile that is being indexed.
	 * @exception  Exception  If an error occurs
	 */
	protected final void addFields(Document newDoc, Document existingDoc, File sourceFile)
		 throws Exception {

		RecordDataService recordDataService = getRecordDataService();

		// Add fields only available if the RecordDataService is avail:
		if (recordDataService != null) {

			String fieldContent;
			List fieldList;

			/*
			// Flag anno
			if (myAnnoResultDocs != null && myAnnoResultDocs.length > 0)
				newDoc.add(new Field("itemhasanno", "true", Field.Store.YES, Field.Index.ANALYZED));
			else
				newDoc.add(new Field("itemhasanno", "false", Field.Store.YES, Field.Index.ANALYZED));
			// Anno types
			fieldContent = "";
			fieldList = recordDataService.getAnnoTypesFromResultDocs(myAnnoResultDocs);
			if (fieldList != null && fieldList.size() > 0) {
				for (int i = 0; i < fieldList.size(); i++)
					fieldContent += ((String) fieldList.get(i)).replaceAll(" ", "+") + " ";
				newDoc.add(new Field("itemannotypes", fieldContent, Field.Store.YES, Field.Index.ANALYZED));
			}
			// Anno pathways
			fieldContent = "";
			fieldList = recordDataService.getAnnoPathwaysFromResultDocs(myAnnoResultDocs);
			if (fieldList != null && fieldList.size() > 0) {
				for (int i = 0; i < fieldList.size(); i++)
					fieldContent += ((String) fieldList.get(i)).replaceAll(" ", "+") + " ";
				newDoc.add(new Field("itemannopathways", fieldContent, Field.Store.YES, Field.Index.ANALYZED));
			}
			// Anno collection keys, e.g. {06, 09}
			fieldContent = "";
			fieldList = recordDataService.getCollectionKeysFromResultDocs(myAnnoResultDocs);
			if (fieldList != null && fieldList.size() > 0) {
				for (int i = 0; i < fieldList.size(); i++) {
					fieldContent += (String) fieldList.get(i);
					if (i < (fieldList.size() - 1))
						fieldContent += "+";
				}
				//prtln("itemannocollectionkeys for " + this.getId() + " is: " + fieldContent);
				newDoc.add(new Field("itemannocollectionkeys", fieldContent, Field.Store.YES, Field.Index.ANALYZED));
			}
			// Anno collection keys e.g. {06, 09} for those with status completed only
			fieldContent = "";
			ArrayList completedAnnoCollectionKeys
				 = recordDataService.getCompletedAnnoCollectionKeysFromResultDocs(myAnnoResultDocs);
			if (completedAnnoCollectionKeys != null && completedAnnoCollectionKeys.size() > 0) {
				for (int i = 0; i < completedAnnoCollectionKeys.size(); i++) {
					fieldContent += (String) completedAnnoCollectionKeys.get(i);
					if (i < (completedAnnoCollectionKeys.size() - 1))
						fieldContent += "+";
				}
				//prtln("itemannocompletedcollectionkeys for " + this.getId() + " is: " + fieldContent);
				newDoc.add(new Field("itemannocompletedcollectionkeys", fieldContent, Field.Store.YES, Field.Index.ANALYZED));
			}
			// Anno status
			fieldContent = "";
			fieldList = recordDataService.getAnnoStatusFromResultDocs(myAnnoResultDocs);
			if (fieldList != null && fieldList.size() > 0) {
				for (int i = 0; i < fieldList.size(); i++)
					fieldContent += ((String) fieldList.get(i)).replaceAll(" ", "+") + " ";
				newDoc.add(new Field("itemannostatus", fieldContent, Field.Store.YES, Field.Index.ANALYZED));
			}

			// Anno formats
			fieldContent = "";
			fieldList = recordDataService.getAnnoFormatsFromResultDocs(myAnnoResultDocs);
			if (fieldList != null && fieldList.size() > 0) {
				for (int i = 0; i < fieldList.size(); i++)
					fieldContent += ((String) fieldList.get(i)) + " ";
				newDoc.add(new Field("itemannoformats", fieldContent, Field.Store.YES, Field.Index.ANALYZED));
			}

			// Anno rating information and statistics
			indexAnnoRatings(myAnnoResultDocs, newDoc); */
			// Part of DRC?
			/*
			The resourcs in the in DRC if either A or B are true:
			A. One or more ADN records that catalog the resource comes from a DRC
			collection (as indicated by it's collection record)

			B. The logic for determining whether an annotated resources is in the
			DRC is the following:
			1. The anno's collection record shows it is part of DRC AND
			2. The anno's status is /annotationRecord/item/statusOf@status 'xxx
			completed' (or is empty) AND
			3. The anno's pathway (/annotationRecord/item/pathway) is not empty
			*/
			DleseCollectionDocReader collDoc = getMyCollectionDoc();
			String partOfDrc;
			if (collDoc == null)
				throw new FileIndexingServiceException("Unable to get collection-level record for collection key:" + getCollections()[0]);
			if (collDoc.isPartOfDRC() ||
				recordDataService.hasDRCItem(assocItemResultDocs) ||
				recordDataService.hasDRCAnnotation(getMyAnnoResultDocs()))
				partOfDrc = "true";
			else
				partOfDrc = "false";
			newDoc.add(new Field("partofdrc", partOfDrc, Field.Store.YES, Field.Index.ANALYZED));

			// -------------- Collections that refer to same resource -----------------

			// My annotation colleciton keys (vocab mgr encoded) that are part of DRC
			ArrayList drcAnnoKeys = new ArrayList();
			if (partOfDrc.equals("true"))
				drcAnnoKeys.add(getFieldContent("drc", "key", "dlese_collect"));
			DleseAnnoDocReader annoDoc;
			DleseCollectionDocReader annoCollectionDoc;
			String drcAnnoKey;
			for (int i = 0; allAnnoResultDocs != null && i < allAnnoResultDocs.size(); i++) {
				annoCollectionDoc = (DleseCollectionDocReader) ((DleseAnnoDocReader) allAnnoResultDocs.get(i).getDocReader()).getMyCollectionDoc();
				annoDoc = (DleseAnnoDocReader) allAnnoResultDocs.get(i).getDocReader();

				if (annoCollectionDoc != null && annoCollectionDoc.isPartOfDRC()) {
					drcAnnoKey = annoDoc.getCollectionKey();
					if (!drcAnnoKeys.contains(drcAnnoKey))
						drcAnnoKeys.add(annoDoc.getCollectionKey());
				}
			}

			// Additional collections keys (vocab mgr encoded) that refer to the same resource, including anno collections:
			String myCollectionKey = getCollections()[0];
			myCollectionKey = getFieldContent(myCollectionKey, "key", "dlese_collect");
			ArrayList completedAnnoCollectionKeys
				 = recordDataService.getCompletedAnnoCollectionKeysFromResultDocs(getMyAnnoResultDocs());
			String associatedcollectionkeys = "";
			fieldList = recordDataService.getCollectionKeysFromResultDocs(assocItemResultDocs);
			if (fieldList != null) {
				if (completedAnnoCollectionKeys != null)
					fieldList.addAll(completedAnnoCollectionKeys);
			}
			else
				fieldList = completedAnnoCollectionKeys;

			if (partOfDrc.equals("true")) {
				if (fieldList == null)
					fieldList = new ArrayList(1);
				fieldList.add(getFieldContent("drc", "key", "dlese_collect"));
			}

			if (fieldList != null && fieldList.size() > 0) {
				for (int i = 0; i < fieldList.size(); i++) {
					if (myCollectionKey.equals((String) fieldList.get(i))) {
						prtlnErr("Warning: ID " + getIds() +
							" indicates items in the same collection that refer to the same resource.");
					}
					else {
						associatedcollectionkeys += (String) fieldList.get(i);
						if (i < (fieldList.size() - 1))
							associatedcollectionkeys += "+";
					}
				}
				newDoc.add(new Field("associatedcollectionkeys", associatedcollectionkeys, Field.Store.YES, Field.Index.ANALYZED));
			}

			// -------------- End collections that refer to same resource -----------------

			// Flag Associated IDs
			if (associatedIds != null && associatedIds.length > 0)
				newDoc.add(new Field("hasassociatedids", "true", Field.Store.YES, Field.Index.ANALYZED));
			else
				newDoc.add(new Field("hasassociatedids", "false", Field.Store.YES, Field.Index.ANALYZED));

			// Associated IDs.
			fieldContent = "";
			if (associatedIds != null && associatedIds.length > 0) {
				for (int i = 0; i < associatedIds.length; i++)
					fieldContent += associatedIds[i] + " ";
				//prtln("adding associated IDs: " + fieldContent);
				newDoc.add(new Field("associatedids", fieldContent, Field.Store.YES, Field.Index.ANALYZED));
			}

			// Errors.
			fieldContent = "";
			if (identifiedErrors != null && identifiedErrors.length > 0) {
				for (int i = 0; i < identifiedErrors.length; i++)
					fieldContent += identifiedErrors[i] + " ";
				newDoc.add(new Field("idmaperrors", fieldContent, Field.Store.YES, Field.Index.ANALYZED));
			}
			else
				newDoc.add(new Field("idmaperrors", "noerrors", Field.Store.YES, Field.Index.ANALYZED));

		}
		// If no record data service, mark the item as having associated ids
		else {
			/* newDoc.add(new Field("itemhasanno", "false", Field.Store.YES, Field.Index.ANALYZED)); */
			newDoc.add(new Field("hasassociatedids", "false", Field.Store.YES, Field.Index.ANALYZED));
			DleseCollectionDocReader myCollectionDoc = getMyCollectionDoc();
			if (myCollectionDoc != null && myCollectionDoc.isPartOfDRC())
				newDoc.add(new Field("partofdrc", "true", Field.Store.YES, Field.Index.ANALYZED));
			else
				newDoc.add(new Field("partofdrc", "false", Field.Store.YES, Field.Index.ANALYZED));
		}

		// Accesssion status
		String accessionStatus = getAccessionStatus();
		if (accessionStatus == null || accessionStatus.length() == 0)
			accessionStatus = MmdRec.statusNames[MmdRec.STATUS_ACCESSIONED_DISCOVERABLE];
		newDoc.add(new Field("accessionstatus", accessionStatus, Field.Store.YES, Field.Index.ANALYZED));

		// Accesssion date
		Date accessionDate = getAccessionDate();
		if (accessionDate != null)
			newDoc.add(new Field("accessiondate", DateFieldTools.dateToString(accessionDate), Field.Store.YES, Field.Index.NOT_ANALYZED));

		// Creation date
		Date creationDate = getCreationDate();
		if (creationDate != null)
			newDoc.add(new Field("creationdate", DateFieldTools.dateToString(creationDate), Field.Store.YES, Field.Index.NOT_ANALYZED));

		String keywords = getKeywords();
		if (keywords != null && keywords.trim().length() > 0)
			newDoc.add(new Field("keyword", keywords, Field.Store.YES, Field.Index.ANALYZED));
		addToDefaultField(keywords);
		addToAdminDefaultField(keywords);

		String creatorLastName = getCreatorLastName();
		if (creatorLastName != null) {
			addToDefaultField(creatorLastName);
			addToAdminDefaultField(creatorLastName);
		}

		String creator = getCreator();
		if (creator == null)
			creator = "";
		newDoc.add(new Field("creator", creator, Field.Store.NO, Field.Index.ANALYZED));
		addToAdminDefaultField(creator);

		if (getHasRelatedResource())
			newDoc.add(new Field("itemhasrelatedresource", "true", Field.Store.YES, Field.Index.ANALYZED));
		else
			newDoc.add(new Field("itemhasrelatedresource", "false", Field.Store.YES, Field.Index.ANALYZED));

		String value;
		value = IndexingTools.makeSeparatePhrasesFromStrings(getRelatedResourceIds());
		if (value != null)
			newDoc.add(new Field("itemrelatedresourceids", value, Field.Store.YES, Field.Index.NOT_ANALYZED));

		value = IndexingTools.makeSeparatePhrasesFromStrings(getRelatedResourceUrls());
		if (value != null)
			newDoc.add(new Field("itemrelatedresourceurls", value, Field.Store.YES, Field.Index.NOT_ANALYZED));

		// Index the content of the item
		indexContent(newDoc, existingDoc);

		addFrameworkFields(newDoc, existingDoc);
	}


	/**
	 *  Indexes the annotation rating information and tabulated statistics.
	 *
	 * @param  annoResultDocs  An array of anno ResultDocs
	 * @param  newDoc          The Document to add the fields to
	 */
	private void indexAnnoRatings(ResultDocList annoResultDocs, Document newDoc) {

		float numRatings = 0;
		float totalRating = 0;
		String ratings = null;
		if (annoResultDocs != null && annoResultDocs.size() > 0) {
			ratings = "";
			for (int i = 0; i < annoResultDocs.size(); i++) {
				String rating = ((DleseAnnoDocReader) annoResultDocs.get(i).getDocReader()).getRating();
				if (rating != null && rating.length() > 0) {
					try {
						totalRating += Float.parseFloat(rating);
						numRatings++;
						ratings += rating + " ";
					} catch (Exception nfe) {}
				}
			}
		}

		// The total number of ratings assigned to this resource
		newDoc.add(new Field("itemannonumratings", new DecimalFormat("00000").format(numRatings), Field.Store.YES, Field.Index.NOT_ANALYZED));

		// A String of all the ratings assigned to this resource, as numbers (e.g. '1 1 2 4 2 3')
		if (ratings != null && ratings.length() > 0)
			newDoc.add(new Field("itemannoratingvalues", ratings, Field.Store.YES, Field.Index.ANALYZED));

		// The average rating for this resource
		if (numRatings > 0 || totalRating > 0) {
			float aveRating = (totalRating / numRatings);

			NumberFormat formatter = new DecimalFormat("0.000");
			//prtln("ave rating: " + aveRating + " string: " + formatter.format(aveRating));
			newDoc.add(new Field("itemannoaveragerating", formatter.format(aveRating), Field.Store.YES, Field.Index.NOT_ANALYZED));
		}
	}


	/**
	 *  Indexes the content of the item, for example the HTML.
	 *
	 * @param  newDoc       The new Document that is being created.
	 * @param  existingDoc  The existing Document for this item, if available.
	 */
	private void indexContent(Document newDoc, Document existingDoc) {
		String content = getContent();
		String contentType = getContentType();

		String contentIndexed = "";
		if (content != null && content.trim().length() != 0 && contentType != null && contentType.trim().length() != 0) {
			String value = null;
			// Index HTML content
			if (contentType.toLowerCase().indexOf("html") != -1) {
				HTMLParser hp = null;

				// Parse the HTML:
				try {
					hp = new HTMLParser(content, "UTF-8");
				} catch (Throwable t) {
					//prtlnErr("Unable to parse HTML: " + t);
					if (contentIndexed.trim().length() == 0)
						contentIndexed = "noContentIndexedUnparsableHtml";
					return;
				}

				// Index the full text of the HTML document:
				try {
					value = hp.getWholeText();
					if (value != null && value.trim().length() > 0) {
						newDoc.add(new Field("itemContent", value, Field.Store.YES, Field.Index.ANALYZED));
						contentIndexed += "htmlFullContent";
					}
				} catch (Throwable t) {
					//prtlnErr("Unable to parse HTMLParser.getWholeText(): " + t);
				}

				// Index the HTML title and headers
				try {
					value = hp.getTitleText();
					if (value != null && value.trim().length() > 0) {
						newDoc.add(new Field("itemContentTitle", value, Field.Store.YES, Field.Index.ANALYZED));
						contentIndexed += " htmlTitle";
					}
				} catch (Throwable t) {
					//prtlnErr("Unable to parse HTMLParser.getTitleText(): " + t);
				}

				// Index the HTML headers
				try {
					value = hp.getHeaderText();
					if (value != null && value.trim().length() > 0) {
						newDoc.add(new Field("itemContentHeaders", value, Field.Store.YES, Field.Index.ANALYZED));
						contentIndexed += " htmlHeaders";
					}
				} catch (Throwable t) {
					//prtlnErr("Unable to parse HTMLParser.getHeaderText(): " + t);
				}
			}
			// Index pdf, which is stored in the DB as text
			else if (contentType.toLowerCase().indexOf("pdf") != -1) {
				newDoc.add(new Field("itemContent", content, Field.Store.YES, Field.Index.ANALYZED));
				contentIndexed += " pdfFullContent";
			}

			if (contentIndexed.trim().length() == 0)
				contentIndexed = "noContentIndexedUnsupportedContentType";
		}

		// Store the content type:
		if (contentType != null)
			newDoc.add(new Field("itemContentType", contentType, Field.Store.YES, Field.Index.ANALYZED));

		// Indicate the fields that were indexed as contents
		newDoc.add(new Field("itemContentIndexedFields", (contentIndexed.trim().length() == 0 ? "noContentIndexedEmpty" : contentIndexed), Field.Store.YES, Field.Index.ANALYZED));

	}


	/**
	 *  Returns the date used to determine "What's new" in the library, which is the item's accession date.
	 *
	 * @return                The what's new date for the item
	 * @exception  Exception  This method should throw and Exception with appropriate error message if an error
	 *      occurs.
	 */
	protected Date getWhatsNewDate() throws Exception {
		return wnDate;
	}


	/**
	 *  Returns 'itemnew' or 'itemannoinprogress' or 'itemannocomplete' whichever came most recelntly.
	 *
	 * @return                The string 'itemnew' or 'itemannoinprogress' or 'itemannocomplete'.
	 * @exception  Exception  If error getting whats new type.
	 */
	protected String getWhatsNewType() throws Exception {
		return wnType;
	}

}

