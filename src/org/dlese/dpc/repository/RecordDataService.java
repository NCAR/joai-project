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
package org.dlese.dpc.repository;

import java.io.*;
import java.util.*;
import java.text.*;

import org.dlese.dpc.xml.*;
import org.dlese.dpc.index.*;
import org.dlese.dpc.util.*;
import org.apache.lucene.document.*;
import org.dlese.dpc.index.reader.*;
import org.dlese.dpc.services.mmd.MmdException;
import org.dlese.dpc.services.mmd.MmdRec;
import org.dlese.dpc.services.mmd.MmdWarning;
import org.dlese.dpc.services.mmd.Query;
import org.dlese.dpc.vocab.*;
import org.dlese.dpc.dds.action.*;
import org.dlese.dpc.index.analysis.*;

import java.net.URL;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;
import org.dom4j.*;

/**
 *  Provides data related to a given collection-level record such as its associated IDs and annotations. This
 *  class is used by class {@link org.dlese.dpc.index.writer.ItemFileIndexingWriter} to insert data into the
 *  index for each record at index creation time.
 *
 * @author     John Weatherley
 * @version    $Id: RecordDataService.java,v 1.58 2010/07/14 00:18:49 jweather Exp $
 * @see        RecordData
 * @see        org.dlese.dpc.services.mmd.Query
 */
public class RecordDataService {
	private static boolean debug = false;

	/**  Use to query within a other collection */
	public static int QUERY_OTHER = Query.QUERY_OTHER;
	/**  Use to query within a the same collection */
	public static int QUERY_SAME = Query.QUERY_SAME;
	/**  Use to query within both other and the same collection */
	public static int QUERY_BOTH = Query.QUERY_BOTH;

	private boolean IDMAPPER_DISABLED = false;

	private MetadataVocab vocab = null;
	private String dbUrl = null;
	private Query localIdMapperQueryObject = null;
	private String collBaseDir = null;
	private RepositoryManager rm = null;
	private String annotationPathwaysSchemaUrl = null;
	private String collectCollectionID = null;


	/**
	 *  Constructor for a RecordDataService that has access to an MmdRec Query service.
	 *
	 * @param  dbUrl                        URL of the database, or use the String "useRandomIds" to generate
	 *      random associated IDs, or null if not using the Query API.
	 * @param  vocab                        The MetadataVocab to use, or null if not needed.
	 * @param  collBaseDir                  Description of the Parameter
	 * @param  annotationPathwaysSchemaUrl  URL to the annotation pathways schema
	 * @see                                 org.dlese.dpc.services.mmd.Query
	 */
	public RecordDataService(
	                         String dbUrl,
	                         MetadataVocab vocab,
	                         String collBaseDir,
	                         String annotationPathwaysSchemaUrl) {
		this.collBaseDir = collBaseDir;
		this.dbUrl = dbUrl;
		this.vocab = vocab;
		this.annotationPathwaysSchemaUrl = annotationPathwaysSchemaUrl;
		if (dbUrl.equals("id_mapper_not_used"))
			this.IDMAPPER_DISABLED = true;
	}


	/**
	 *  Updates the MetadataVocab being used by this service.
	 *
	 * @param  newVocab  The new MetadataVocab that will be used.
	 */
	public void updateVocab(MetadataVocab newVocab) {
		vocab = newVocab;
	}


	/**
	 *  Initialize the RecordDataService.
	 *
	 * @param  rm  The RepositoryManager
	 */
	public void init(RepositoryManager rm) {
		this.rm = rm;
	}


	/**
	 *  Gets the index used by this RecordData service to retrieve data.
	 *
	 * @return    The index.
	 */
	public SimpleLuceneIndex getIndex() {
		return rm.getIndex();
	}


	/**
	 *  Returns the value of collectCollectionID.
	 *
	 * @return    The collectCollectionID value
	 */
	public String getCollectCollectionID() {
		return collectCollectionID;
	}


	/**
	 *  Sets the value of collectCollectionID.
	 *
	 * @param  collectCollectionID  The value to assign collectCollectionID.
	 */
	public void setCollectCollectionID(String collectCollectionID) {
		this.collectCollectionID = collectCollectionID;
	}
	
	// ------ ID Mapper service ----------------


	/**
	 *  Gets a IDMapper Query Object to use in {@link #getMmdRec(String, String, Query)} or {@link
	 *  #getAssociatedMMDRecs(int, String, String, Query)}. The Query may be used multiple times. After use, the
	 *  caller must be sure to close the Query using the {@link #closeIdMapperQueryObject(Query)}.
	 *
	 * @return    The a Query Object, or null if unable to obtain
	 */
	public Query getIdMapperQueryObject() {
		if (IDMAPPER_DISABLED)
			return null;
		try {
			//prtln("getIdMapperQueryObject()");
			return new Query(0, dbUrl);
		} catch (Throwable t) {
			prtlnErr("Error opening idmapper database connection: " + t);
		}
		return null;
	}



	/**
	 *  Closes an IDMapper Query object, releasing its resources.
	 *
	 * @param  queryObject  The Query Object to close
	 */
	public void closeIdMapperQueryObject(Query queryObject) {
		if (queryObject == null)
			return;
		try {
			queryObject.closeDb();
			//prtln("closeIdMapperQueryObject()");
		} catch (Throwable e) {
			prtlnErr("Error closing idmapper database connection: " + e);
		}
		queryObject = null;
	}


	/**  Initializes the local Query Object. This must be called prior to use over a collection of files. */
	public void initIdMapper() {
		//prtln("initIdMapper()");
		if (dbUrl == null ||
			dbUrl.trim().toLowerCase().equals("userandomids") ||
			IDMAPPER_DISABLED) {
			return;
		}
		localIdMapperQueryObject = getIdMapperQueryObject();
	}


	/**
	 *  Closes the local Query Object, releasing it's resources. This should be called after finished using the
	 *  idmapper for a collection of files.
	 */
	public void closeIdMapper() {
		//prtln("closeIdMapper()");
		closeIdMapperQueryObject(localIdMapperQueryObject);
		localIdMapperQueryObject = null;
	}


	/**
	 *  Gets the idMapperDisabled attribute of the RecordDataService object
	 *
	 * @return    The idMapperDisabled [true | false]
	 */
	public boolean isIdMapperDisabled() {
		return IDMAPPER_DISABLED;
	}


	/**
	 *  Gets the filePathForId attribute of the RecordDataService object
	 *
	 * @param  rec  Description of the Parameter
	 * @return      The filePathForId value
	 */
	public String getFilePathForId(MmdRec rec) {
		if (rec == null) {
			//prtln("getFilePathForId() Error: ID map record is null");
			return "";
		}
		String pathToRecord = collBaseDir + "/"
			 + rec.metastyleNames[rec.getMetastyle()]
			 + "/" + rec.getCollKey() + "/" + rec.getFileName();
		return pathToRecord;
	}


	/**
	 *  Gets the fileForId attribute of the RecordDataService object
	 *
	 * @param  rec  Description of the Parameter
	 * @return      The fileForId value
	 */
	public File getFileForId(MmdRec rec) {
		return new File(getFilePathForId(rec));
	}


	/**
	 *  Gets the IDs for records that catalog the same resource, not including the ID that is given.
	 *
	 * @param  id                 The id to the item-level record - will be ommitted from the list of
	 *      associatedIDs
	 * @param  associatedMmdRecs  An array of MmdRecs
	 * @return                    The associatedIDs value
	 */
	public String[] getAssociatedIDs(String id, MmdRec[] associatedMmdRecs) {
		if (associatedMmdRecs == null || associatedMmdRecs.length == 0)
			return null;

		String[] associatedIds = new String[associatedMmdRecs.length];
		for (int i = 0; i < associatedMmdRecs.length; i++) {
			if (!associatedMmdRecs[i].getId().equalsIgnoreCase(id)) {
				associatedIds[i] = associatedMmdRecs[i].getId();
			}
		}
		return associatedIds;
	}



	/**
	 *  Gets all errors identified by the ID mapper service for the given records, or null if none exist.
	 *
	 * @param  mmdRecs  An array of MmdRecords
	 * @return          A String array of IDMapper error codes
	 */
	public String[] getIdentifiedErrors(MmdRec[] mmdRecs) {
		if (mmdRecs == null || mmdRecs.length == 0)
			return null;

		ArrayList errList = new ArrayList();
		for (int i = 0; i < mmdRecs.length; i++) {
			MmdRec myRecordData = mmdRecs[i];
			if (myRecordData != null) {
				Object tmpErr;
				ArrayList tmpErrors = getErrorCodeStrings(myRecordData);
				for (int j = 0; j < tmpErrors.size(); j++) {
					tmpErr = tmpErrors.get(j);
					if (!errList.contains(tmpErr)) {
						errList.add(tmpErr);
					}
				}
			}
		}

		if (errList.size() == 0) {
			return null;
		}
		return (String[]) errList.toArray(new String[]{});
	}


	private ArrayList getErrorCodeStrings(MmdRec rec) {
		ArrayList errList = new ArrayList();
		if (rec == null)
			return errList;
		MmdWarning[] errs = rec.getWarnings();
		if (errs != null) {
			for (int i = 0; i < errs.length; i++) {
				errList.add(Integer.toString(errs[i].getMsgType()));
			}
		}
		return errList;
	}


	/**
	 *  Gets the associated MMD records for the given IDs, records that catalog the same resource, or null if
	 *  none exist, using the given Query Object. Splits the IDs and collectionKeys by spaces and uses the first
	 *  one.
	 *
	 * @param  IDs             An ID(s) to an item-level resource, separated by spaces - the first is used
	 * @param  collectionKeys  The collection keys for the collection of files, for example "dcc" or "comet",
	 *      separated by spaces - the first is used
	 * @param  queryType       Specify to look in other or the same collection
	 * @param  qo              The IDMapper Query Object to use
	 * @return                 The associated MmdRecs
	 */
	public MmdRec[] getAssociatedMMDRecs(int queryType, String IDs, String collectionKeys, Query qo) {
		if (qo == null)
			return null;

		//prtln("getAssociatedMMDRecs(): ids: " + IDs + " colls: " + collectionKeys);
		// The first ID and collection key are the primary ones.
		String ID = IDs.trim().split(" ")[0];
		String collectionKey = collectionKeys.trim().split(" ")[0];

		MmdRec[] associatedRecordsData = null;
		try {
			associatedRecordsData = qo.findDups(queryType, collectionKey, ID, rm.getIdMapperExclusionFilePath());

			/* String msg = "";
			if(associatedRecordsData == null || associatedRecordsData.length == 0)
				msg = " had 0 matches";
			else{
					msg = " returned";
					for(int i = 0; i < associatedRecordsData.length; i++)
						msg+= " " + associatedRecordsData[i].getId();
			}

			prtln("getAssociatedMMDRecs() for ID: " + ID + " type: " + (queryType == Query.QUERY_SAME ? "same" : "other") + msg); */
		} catch (Throwable e) {
			prtlnErr("Idmapper error while retrieving associatedIds for item " + ID + ": " + e);
			return null;
		}
		return associatedRecordsData;
	}


	/**
	 *  Gets the associated MMD records for the given IDs, records that catalog the same resource, or null if
	 *  none exist, using the local Query Object. Splits the IDs and collectionKeys by spaces and uses the first
	 *  one.
	 *
	 * @param  IDs             An ID(s) to an item-level resource, separated by spaces.
	 * @param  collectionKeys  The collection keys for the collection of files, for example "dcc" or "comet",
	 *      separated by spaces.
	 * @param  queryType       Specify to look in other or the same collection
	 * @return                 The associated MmdRecs
	 */
	public MmdRec[] getAssociatedMMDRecs(int queryType, String IDs, String collectionKeys) {
		return getAssociatedMMDRecs(queryType, IDs, collectionKeys, localIdMapperQueryObject);
	}


	/**
	 *  Gets the ID mapper record for the given ID using the given Query Object.
	 *
	 * @param  id             The ID of a given record, for example DLESE-000-000-000-001.
	 * @param  collectionKey  The collection in which the given record resides, for example dcc.
	 * @param  qo             The Query Object to use
	 * @return                The ID mapper mmdRecord.
	 */
	public MmdRec getMmdRec(String id, String collectionKey, Query qo) {
		//prtln("getMmdRec(): id:" + id + " coll: " + collectionKey + (qo == null? "qo null": "qo not null"));
		if (qo == null)
			return null;

		MmdRec mmdRec = null;
		try {
			mmdRec = qo.getMmdRec(collectionKey, id);
		} catch (Throwable t) {
			prtln("getMmdRec() threw exception: " + t);
		}
		return mmdRec;
	}


	/**
	 *  Gets the ID mapper record for the given ID using the local Query Object.
	 *
	 * @param  id             The ID of a given record, for example DLESE-000-000-000-001.
	 * @param  collectionKey  The collection in which the given record resides, for example dcc.
	 * @return                The ID mapper mmdRecord.
	 */
	public MmdRec getMmdRec(String id, String collectionKey) {
		return getMmdRec(id, collectionKey, localIdMapperQueryObject);
	}


	/**
	 *  Gets random test associatedIDs for the given ID (records that reference the same resource) or null if
	 *  none exist. *** For testing purposes only ***
	 *
	 * @param  ID  An ID to an item-level resource.
	 * @return     The associatedIDs value
	 */
	private String[] getRandomAssociatedIDs(String ID) {
		// Note: should these associations be stored in a separate index altogether?

		if (getIndex() == null || ID == null) {
			return null;
		}

		String query = "collection:0* AND !valid:false AND readerclass:ItemDocReader";

		ResultDocList resultDocs = null;
		if (getIndex() != null) {
			resultDocs = getIndex().searchDocs(query);
		}
		if (resultDocs == null || resultDocs.size() == 0) {
			return null;
		}

		int random_num_ids = Utils.getRandomIntBetween(0, 4);
		if (random_num_ids == 0) {
			return null;
		}
		String[] ids = new String[random_num_ids];
		// Add 0 to 2 random ids:
		for (int i = 0; i < random_num_ids; i++) {
			ids[i] = (((ItemDocReader) resultDocs.get(Utils.getRandomIntBetween(0, resultDocs.size())).getDocReader()).getId());
		}

		return ids;
	}


	private String[] getRandomIdMapperErrors() {

		int random_num = Utils.getRandomIntBetween(0, 4);
		if (random_num == 0 || random_num == 1) {
			return null;
		}
		else if (random_num == 2) {
			return new String[]{"41040", "41110"};
		}
		else {
			return new String[]{"41030"};
		}
	}


	// ------ Index services ----------------


	/**
	 *  Gets the accession status of the given records or, if multiple resources get all statuses separated by
	 *  spaces.
	 *
	 * @param  mmdRecs  The MmdRecords
	 * @return          The status value
	 */
	public String getAccessionStatus(MmdRec[] mmdRecs) {
		// The first ID and collection key are the primary ones.
		if (mmdRecs == null || mmdRecs.length == 0)
			return null;

		HashMap tmpMap = new HashMap();
		String status = "";
		String tmp;
		for (int i = 0; i < mmdRecs.length; i++) {
			MmdRec myRecordData = mmdRecs[i];
			if (myRecordData == null)
				tmp = MmdRec.statusNames[MmdRec.STATUS_ACCESSIONED_DISCOVERABLE];
			else
				tmp = myRecordData.statusNames[myRecordData.getStatus()];

			if (!tmpMap.containsKey(tmp)) {
				status += tmp + " ";
				tmpMap.put(tmp, tmp);
			}
		}
		return status.trim();
	}


	/**
	 *  Gets the ResultDoc for the given item level metata record id but only if it should be displayed in
	 *  discovery. Returns null if none exists.
	 *
	 * @param  itemId  The ID to a given item-level record.
	 * @return         The ResultDoc for that item or null.
	 */
	public ResultDoc getDisplayableItemResultDoc(String itemId) {
		return doGetItemResultDoc(itemId, true);
	}



	/**
	 *  Gets the item result docs for each of the ids listed regardless, returning only those that should be
	 *  displayed in discovery.
	 *
	 * @param  itemIds  An array of IDs.
	 * @return          The items that matched or null if none.
	 */
	public ResultDocList getDisplayableItemResultDocs(String[] itemIds) {
		return doGetItemResultDocs(itemIds, true);
	}



	/**
	 *  Gets the ResultDoc for the given item-level metata record id regardless of the records status, or null if
	 *  none exists.
	 *
	 * @param  itemId  The ID to a given item-level record.
	 * @return         The ResultDoc for that item.
	 */
	public ResultDoc getItemResultDoc(String itemId) {
		return doGetItemResultDoc(itemId, false);
	}



	/**
	 *  Gets the item result docs for each of the ids listed regardless of their status.
	 *
	 * @param  itemIds  An array of IDs.
	 * @return          The items that matched or null if none.
	 */
	public ResultDocList getItemResultDocs(String[] itemIds) {
		return doGetItemResultDocs(itemIds, false);
	}



	/**
	 *  Gets the ResultDoc for the given XML record id or null if none exists.
	 *
	 * @param  itemId       The ID to a given XML record.
	 * @param  displayable  True to return discovery displayable items, false to return all items.
	 * @return              The ResultDoc for that item.
	 */
	private final ResultDoc doGetItemResultDoc(String itemId, boolean displayable) {
		if (itemId == null || getIndex() == null) {
			return null;
		}
		try {
			String query = "id:" + SimpleLuceneIndex.encodeToTerm(itemId);
			if (displayable)
				query += " AND " + rm.getDiscoverableItemsQuery();
			ResultDocList results =
				getIndex().searchDocs(query);

			if (results == null || results.size() == 0) {
				return null;
			}
			else {
				String id = ((XMLDocReader) results.get(0).getDocReader()).getId();

				if (itemId.equalsIgnoreCase(id)) {
					return results.get(0);
				}
				else {
					return null;
				}
			}
		} catch (Throwable e) {
			prtlnErr("RecordDataService.getItemResultDoc() error: " + e);
			e.printStackTrace();
			return null;
		}
	}



	/**
	 *  Gets the item result docs for the given IDs in the same order as requested.
	 *
	 * @param  itemIds      The ids of the items.
	 * @param  displayable  Whether to return only those IDs that are currently displayable in discovery or not.
	 * @return              The itemResultDocs value
	 */
	private final ResultDocList doGetItemResultDocs(String[] itemIds, boolean displayable) {
		if (itemIds == null || itemIds.length == 0 || getIndex() == null) {
			return null;
		}

		//HashMap idsSeen = new HashMap();
		ArrayList resultDocs = new ArrayList();
		ResultDoc doc = null;
		for (int i = 0; i < itemIds.length; i++) {
			doc = doGetItemResultDoc(itemIds[i], displayable);
			if (doc != null) {
				resultDocs.add(doc);
			}
		}
		if (resultDocs.size() > 0) {
			return (ResultDocList) new ResultDocList((ResultDoc[])resultDocs.toArray(new ResultDoc[]{}));
		}
		else {
			return null;
		}
	}



	/**
	 *  Gets the DLESEAnno docs that annotate the given ids, or null if none.
	 *
	 * @param  ids  Ids for records in the repository.
	 * @return      The matching DLESE anno records, or null if none.
	 */
	public ResultDocList getDleseAnnoResultDocs(String[] ids) {
		if (ids == null || ids.length == 0 || getIndex() == null) {
			return null;
		}
		try {
			String idQ = "(annoitemid:\"" + ids[0] + "\"";
			for (int i = 1; i < ids.length; i++) {
				idQ += " OR annoitemid:\"" + ids[i] + "\"";
			}
			idQ += ")";
			ResultDocList results =
				getIndex().searchDocs(idQ, new KeywordAnalyzer());
			if (results == null || results.size() == 0) {
				//prtln("getDleseAnnoResultDocs(): " + idQ + " num: 0");
				return null;
			}
			else {
				//prtln("getDleseAnnoResultDocs(): " + idQ + " num: " + results.length);
				return results;
			}
		} catch (Throwable e) {
			System.err.println("RecordDataService.getDleseAnnoResultDocs() error: " + e);
			//e.printStackTrace();
			return null;
		}
	}


	/**
	 *  Gets all annotation types in the given set of DleseAnnoDocReaders, or null if none exist.
	 *
	 * @param  annoResultDocs  An array of ResultDocs
	 * @return                 The annoTypes value or null.
	 */
	public ArrayList getAnnoTypesFromResultDocs(ResultDocList annoResultDocs) {
		if (annoResultDocs == null || annoResultDocs.size() == 0) {
			return null;
		}

		ArrayList annoTypes = new ArrayList(annoResultDocs.size());
		String type;
		for (int i = 0; i < annoResultDocs.size(); i++) {
			type = ((DleseAnnoDocReader) annoResultDocs.get(i).getDocReader()).getType();
			if (!annoTypes.contains(type)) {
				annoTypes.add(type);
			}
		}
		return annoTypes;
	}


	/**
	 *  Gets the annoStatusFromReaders attribute of the RecordDataService object
	 *
	 * @param  annoResultDocs  An array of ResultDocs
	 * @return                 The annoStatusFromReaders value
	 */
	public ArrayList getAnnoStatusFromResultDocs(ResultDocList annoResultDocs) {
		if (annoResultDocs == null || annoResultDocs.size() == 0) {
			return null;
		}

		ArrayList annoStatus = new ArrayList(annoResultDocs.size());
		String type;
		for (int i = 0; i < annoResultDocs.size(); i++) {
			type = ((DleseAnnoDocReader) annoResultDocs.get(i).getDocReader()).getStatus();
			if (!annoStatus.contains(type)) {
				annoStatus.add(type);
			}
		}
		return annoStatus;
	}


	/**
	 *  Gets the annotation formats from the anno result docs.
	 *
	 * @param  annoResultDocs  An array of ResultDocs
	 * @return                 The anno formats
	 */
	public ArrayList getAnnoFormatsFromResultDocs(ResultDocList annoResultDocs) {
		if (annoResultDocs == null || annoResultDocs.size() == 0) {
			return null;
		}

		ArrayList annoFormats = new ArrayList(annoResultDocs.size());
		String val;
		for (int i = 0; i < annoResultDocs.size(); i++) {
			val = ((DleseAnnoDocReader) annoResultDocs.get(i).getDocReader()).getFormat();
			if (!annoFormats.contains(val)) {
				annoFormats.add(val);
			}
		}
		return annoFormats;
	}


	/**
	 *  Gets a list of keys, for example {06, 04}, for each of the annotation collections that have at least one
	 *  status completed record.
	 *
	 * @param  annoResultDocs  An array of ResultDocs of annotation records
	 * @return                 A list of annotation collection keys, or null
	 */
	public ArrayList getCompletedAnnoCollectionKeysFromResultDocs(ResultDocList annoResultDocs) {
		if (annoResultDocs == null || annoResultDocs.size() == 0) {
			return null;
		}

		ArrayList completedAnnoKeys = new ArrayList(annoResultDocs.size());
		String status;
		DleseAnnoDocReader annoDocReader = null;
		//prtln("num anno docs: " + annoResultDocs.length);
		for (int i = 0; i < annoResultDocs.size(); i++) {
			annoDocReader = (DleseAnnoDocReader) annoResultDocs.get(i).getDocReader();
			status = annoDocReader.getStatus();
			//prtln("completedAnnoKey status: " + status);
			if (status != null && status.matches(".*completed")) {
				String key = annoDocReader.getCollectionKey();
				if (!completedAnnoKeys.contains(key))
					completedAnnoKeys.add(key);
			}
		}
		//prtln("completedAnnoKeys list size: " + completedAnnoKeys.size());

		return completedAnnoKeys;
	}


	/**
	 *  Gets all annotation pathways in the given set of DleseAnnoDocReaders, or null if none exist. Currently,
	 *  this is not used!
	 *
	 * @param  annoResultDocs  An array of ResultDocs
	 * @return                 The annoPathways value or null.
	 */
	public ArrayList getAnnoPathwaysFromResultDocs(ResultDocList annoResultDocs) {
		if (annoResultDocs == null || annoResultDocs.size() == 0) {
			return null;
		}

		ArrayList annoPathways = new ArrayList(annoResultDocs.size());
		String pathway;
		for (int i = 0; i < annoResultDocs.size(); i++) {
			pathway = ((DleseAnnoDocReader) annoResultDocs.get(i).getDocReader()).getPathway();
			if (!annoPathways.contains(pathway)) {
				annoPathways.add(pathway);
			}
		}
		return annoPathways;
	}


	/**
	 *  Return true if there is an annotation present that is part of the DRC. The definition of DRC is obtains
	 *  from the current pathways.xsd schema at the DPC.
	 *
	 * @param  annoResultDocs  Annotation records.
	 * @return                 True if one or more of the annotations is part of the DRC.
	 */
	public static boolean hasDRCAnnotation(ResultDocList annoResultDocs) {
		if (annoResultDocs == null || annoResultDocs.size() == 0)
			return false;

		for (int i = 0; i < annoResultDocs.size(); i++)
			if (((DleseAnnoDocReader) annoResultDocs.get(i).getDocReader()).isPartOfDrc())
				return true;

		return false;
	}


	/**
	 *  Return true if there is an item present that is part of the DRC as determined by the items collection.
	 *
	 * @param  itemResultDocs  Array of Item result docs.
	 * @return                 True if one or more of the items is part of the DRC.
	 */
	public static boolean hasDRCItem(ResultDocList itemResultDocs) {
		if (itemResultDocs == null || itemResultDocs.size() == 0)
			return false;

		for (int i = 0; i < itemResultDocs.size(); i++)
			if (((ItemDocReader) itemResultDocs.get(i).getDocReader()).getMyCollectionDoc().isPartOfDRC())
				return true;

		return false;
	}



	/**
	 *  Return a HasMap of valid DRC pathways from the pathways.xsd schema. Both the keys and values cantain the
	 *  exact Strings of the valid pathways.
	 *
	 * @return    HashMap of valid DRC pathways.
	 */
	private HashMap getValidDrcPathways() {

		if (annotationPathwaysSchemaUrl == null)
			return null;

		HashMap pathways = new HashMap();

		try {
			SAXReader reader = new SAXReader();
			Document document = reader.read(new URL(annotationPathwaysSchemaUrl));
			List nodes = document.selectNodes("//xsd:simpleType[@name='pathwayType']/xsd:restriction/xsd:enumeration");
			for (Iterator iter = nodes.iterator(); iter.hasNext(); ) {
				Node node = (Node) iter.next();
				pathways.put(node.valueOf("@value"), node.valueOf("@value"));
			}
		} catch (Throwable e) {
			prtlnErr("Error getValidDrcPathways(): " + e);
		}

		return pathways;
	}



	/**
	 *  Gets all collection keys encoded by Vocab Mgr, for example {06, 08}, for the given records.
	 *
	 * @param  collectionResults  ResultDocs of records
	 * @return                    The collection keys or null.
	 */
	public ArrayList getCollectionKeysFromResultDocs(ResultDocList collectionResults) {
		if (collectionResults == null || collectionResults.size() == 0) {
			return null;
		}

		ArrayList collections = new ArrayList(collectionResults.size() + 4);
		String collection;
		for (int i = 0; i < collectionResults.size(); i++) {
			collection = ((XMLDocReader) collectionResults.get(i).getDocReader()).getCollectionKey();
			if (collection != null && !collections.contains(collection)) {
				collections.add(collection);
			}
		}
		return collections;
	}


	// --------------- Vocab ----------------

	/**
	 *  Gets the vocab attribute of the RecordDataService object
	 *
	 * @return    The vocab value
	 */
	public MetadataVocab getVocab() {
		return vocab;
	}


	// ---------------- Debug methods --------------------

	/**
	 *  Return a string for the current time and date, sutiable for display in log files and output to standout:
	 *
	 * @return    The dateStamp value
	 */
	protected final static String getDateStamp() {
		return
			new SimpleDateFormat("MMM d, yyyy h:mm:ss a zzz").format(new Date());
	}


	/**
	 *  Output a line of text to error out, with datestamp.
	 *
	 * @param  s  The text that will be output to error out.
	 */
	protected final static void prtlnErr(String s) {
		System.err.println(getDateStamp() + " " + s);
	}



	/**
	 *  Output a line of text to standard out, with datestamp, if debug is set to true.
	 *
	 * @param  s  The String that will be output.
	 */
	protected final static void prtln(String s) {
		if (debug) {
			System.out.println(getDateStamp() + " " + s);
		}
	}


	/**
	 *  Sets the debug attribute of the object
	 *
	 * @param  db  The new debug value
	 */
	public static void setDebug(boolean db) {
		debug = db;
	}
}

