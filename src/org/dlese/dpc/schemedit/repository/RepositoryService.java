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
package org.dlese.dpc.schemedit.repository;

import org.dlese.dpc.schemedit.Constants;
import org.dlese.dpc.schemedit.FrameworkRegistry;
import org.dlese.dpc.schemedit.MetaDataFramework;
import org.dlese.dpc.schemedit.RecordList;
import org.dlese.dpc.schemedit.SchemEditUtils;
import org.dlese.dpc.schemedit.url.DupSimUrlChecker;
import org.dlese.dpc.schemedit.repository.CollectionReaper;
import org.dlese.dpc.schemedit.security.user.User;
import org.dlese.dpc.schemedit.security.access.Roles;
import org.dlese.dpc.schemedit.dcs.*;
import org.dlese.dpc.schemedit.config.*;
import org.dlese.dpc.schemedit.repository.RepositoryWriter;
import org.dlese.dpc.schemedit.threadedservices.AutoExportTask;
import org.dlese.dpc.xml.schema.*;
import org.dlese.dpc.xml.*;
import org.dlese.dpc.index.*;
import org.dlese.dpc.index.reader.*;

import org.dlese.dpc.util.*;
import org.dlese.dpc.util.strings.FindAndReplace;
import org.dlese.dpc.repository.*;

import java.util.*;
import java.text.*;
import java.io.*;
import java.net.*;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

import org.dom4j.Document;

/**
 *  Methods to wrap the {@link org.dlese.dpc.repository.RepositoryManager} class
 *  and provide other methods and services over the repository.
 *
 * @author    ostwald <p>
 *
 *
 */
public class RepositoryService {

	static boolean debug = false;
	RepositoryWriter repositoryWriter;
	ServletContext servletContext;

	private int autoExportFrequency = 0;
	private Timer autoExportTimer;
	private String autoExportStartTime = null;
	private Date autoExportStartTimeDate = null;
	private List listeners;


	/**
	 *  Constructor for the RepositoryService object, requiring ServletContext.
	 *
	 * @param  servletContext
	 * @exception  Exception   if repositoryWriter or autoExport service cannot be
	 *      initialized.
	 */
	public RepositoryService(ServletContext servletContext) throws Exception {
		this.servletContext = servletContext;
		this.listeners = new ArrayList();
		repositoryWriter = new RepositoryWriter(servletContext);
		repositoryWriter.init();
		autoExportInit();
	}


	/**
	 *  Gets the repositoryWriter attribute of the RepositoryService object
	 *
	 * @return    The repositoryWriter value
	 */
	public RepositoryWriter getRepositoryWriter() {
		return this.repositoryWriter;
	}


	/**
	 *  Gets the dups attribute of the RepositoryService object
	 *
	 * @param  url         NOT YET DOCUMENTED
	 * @param  collection  NOT YET DOCUMENTED
	 * @return             The dups value
	 */
	public List getDups(String url, String collection) {
		DupSimUrlChecker urlChecker = null;
		List dups = new ArrayList();
		try {
			urlChecker = new DupSimUrlChecker(url, collection, servletContext);
			dups = urlChecker.getDups();
		} catch (Exception e) {
			prtlnErr("DupSimUrlChecker error: " + e.getMessage());
		}
		return dups;
	}


	/**
	 *  Gets the sims attribute of the RepositoryService object
	 *
	 * @param  url         NOT YET DOCUMENTED
	 * @param  collection  NOT YET DOCUMENTED
	 * @return             The sims value
	 */
	public List getSims(String url, String collection) {
		DupSimUrlChecker urlChecker = null;
		List sims = new ArrayList();
		try {
			urlChecker = new DupSimUrlChecker(url, collection, servletContext);
			sims = urlChecker.getSims();
		} catch (Exception e) {
			prtlnErr("DupSimUrlChecker error: " + e.getMessage());
		}
		return sims;
	}


	/**
	 *  Initialize the autoExport service with init params from servletContext.
	 *
	 */
	private void autoExportInit() {
		String autoExportStartTime = (String) servletContext.getInitParameter("autoExportStartTime");
		if (autoExportStartTime == null || autoExportStartTime.equalsIgnoreCase("disabled")) {
			autoExportStartTime = null;
			prtln("AutoExport task is not scheduled");
			return;
		}

		long hours24 = 60 * 60 * 24;

		// Start the indexer timer
		if (autoExportStartTime != null) {
			Date startTime = null;
			try {
				Date currentTime = new Date(System.currentTimeMillis());
				//Date oneHourFromNow = new Date(System.currentTimeMillis() + (1000 * 60 * 60));
				Date oneHourFromNow = new Date(System.currentTimeMillis() + (1000 * 60));
				// One minute from now instead...
				int dayInYear = Integer.parseInt(Utils.convertDateToString(currentTime, "D"));
				int year = Integer.parseInt(Utils.convertDateToString(currentTime, "yyyy"));

				startTime = Utils.convertStringToDate(year + " " + dayInYear + " " + autoExportStartTime, "yyyy D H:mm");

				// Make sure the timer starts one hour from now or later
				if (startTime.compareTo(oneHourFromNow) < 0) {
					if (dayInYear == 365) {
						year++;
						dayInYear = 1;
					}
					else
						dayInYear++;
					startTime = Utils.convertStringToDate(year + " " + dayInYear + " " + autoExportStartTime, "yyyy D H:mm");
				}
				startAutoExportTimer(hours24, startTime);
			} catch (ParseException e) {
				prtlnErr("Syntax error in context parameter 'autoExportStartTime.' AutoExport timer not started: " + e.getMessage());
			}
		}
		else
			startAutoExportTimer(autoExportFrequency * 60, null);
	}


	/**
	 *  Start or restarts the indexer thread with the given update frequency. Same
	 *  as {@link #changeautoExportFrequency(long autoExportFrequency)}.
	 *
	 * @param  autoExportFrequency  The number of seconds between index updates.
	 */
	private void startAutoExportTimer(long autoExportFrequency) {
		startAutoExportTimer(autoExportFrequency, null);
	}


	/**
	 *  Start or restarts the timer thread with the given update frequency,
	 *  beginning at the specified time/date. Use this method to schedule the timer
	 *  to run as a nightly cron, beginning at the time you wish the indexer to
	 *  run.
	 *
	 * @param  autoExportFrequency  The number of seconds between index updates.
	 * @param  startTime            The time at which start the indexing process.
	 */
	private void startAutoExportTimer(long autoExportFrequency, Date startTime) {
		// Make sure the indexing timer is stopped before starting...
		stopAutoExportTimer();

		prtln("startAutoExportTimer");

		this.autoExportStartTimeDate = startTime;

		if (autoExportFrequency > 0) {
			if (autoExportTimer != null)
				autoExportTimer.cancel();

			// Set daemon to true
			autoExportTimer = new Timer(true);

			// Convert seconds to milliseeconds
			long freq = ((autoExportFrequency > 0) ? autoExportFrequency * 1000 : 60000);

			// Start the indexer at regular intervals beginning at the specified Date/Time
			if (startTime != null) {
				try {
					prtln("autoExport timer is scheduled to start " +
						Utils.convertDateToString(startTime, "EEE, MMM d, yyyy h:mm a zzz") +
						", and run every " + Utils.convertMillisecondsToTime(freq) + ".");
				} catch (ParseException e) {}

				autoExportTimer.scheduleAtFixedRate(new AutoExportTask(servletContext), startTime, freq);
			}
			// Start the indexer at regular intervals beginning after 6 seconds
			else {
				prtln("autoExport timer scheduled run every " + Utils.convertMillisecondsToTime(freq) + ".");
				autoExportTimer.schedule(new AutoExportTask(servletContext), 6000, freq);
			}
		}
	}


	/**  Stops the indexing timer thread. */
	private void stopAutoExportTimer() {
		if (autoExportTimer != null) {
			autoExportTimer.cancel();
			prtln("RepositoryManager indexing timer stopped");
		}
	}


	/**  NOT YET DOCUMENTED */
	private void destroy() {
		this.stopAutoExportTimer();
	}


	/**
	 *  Gets the repositoryManager attribute of the RepositoryService class
	 *
	 * @return                The repositoryManager value
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	private RepositoryManager getRepositoryManager() throws Exception {
		RepositoryManager rm =
			(RepositoryManager) servletContext.getAttribute("repositoryManager");
		if (rm == null) {
			throw new Exception("RepositoryManger not found");
		}
		return rm;
	}



	/**
	 *  Gets the collectionRegistry attribute of the RepositoryService class
	 *
	 * @return                The collectionRegistry value
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	private CollectionRegistry getCollectionRegistry() throws Exception {
		CollectionRegistry cr =
			(CollectionRegistry) servletContext.getAttribute("collectionRegistry");
		if (cr == null) {
			throw new Exception("CollectionRegistry not found");
		}
		return cr;
	}


	/**
	 *  Gets the frameworkRegistry attribute of the RepositoryService class
	 *
	 * @return    The frameworkRegistry value
	 */
	private FrameworkRegistry getFrameworkRegistry() {
		FrameworkRegistry fr =
			(FrameworkRegistry) servletContext.getAttribute("frameworkRegistry");
		if (fr == null) {
			prtln("getFrameworkRegistry returning null");
		}
		return fr;
	}


	/**
	 *  Gets the metaDataFramework associated with given xmlFormat.
	 *
	 * @param  xmlFormat  for example, "adn"
	 * @return            The metaDataFramework value
	 */
	private MetaDataFramework getMetaDataFramework(String xmlFormat) {
		FrameworkRegistry fr = getFrameworkRegistry();
		if (fr == null) {
			prtlnErr("WARNING: framework registry not found in servlet context");
			return null;
		}
		return fr.getFramework(xmlFormat);
	}


	/**
	 *  Gets the dcsDataManager attribute of the RepositoryService class
	 *
	 * @return    The dcsDataManager value
	 */
	private DcsDataManager getDcsDataManager() {
		DcsDataManager dcsDataManager = (DcsDataManager) servletContext.getAttribute("dcsDataManager");
		if (dcsDataManager == null) {
			prtlnErr("getDcsDataManager returning null");
		}
		return dcsDataManager;
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  id             NOT YET DOCUMENTED
	 * @return                NOT YET DOCUMENTED
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public boolean indexedRecordIsStale(String id) throws Exception {
		XMLDocReader docReader = this.getXMLDocReader(id);
		if (docReader == null)
			throw new Exception("Record not found for " + id);
		File sourceFile = docReader.getFile();
		return indexedRecordIsStale(sourceFile, docReader);
	}


	/**
	 *  Test to see if record has been modified outside of DCS.
	 *
	 * @param  sourceFile  file on disk
	 * @param  docReader   the docReader
	 * @return             true if file has changed on disk
	 */
	public static boolean indexedRecordIsStale(File sourceFile, XMLDocReader docReader) {
		long granularity = 2000;
		// prtln ("\nvalidationIsStale()");

		// long diff = Math.abs(sourceFile.lastModified() - docReader.getLastModified());
		long diff = sourceFile.lastModified() - docReader.getLastModified();
		/* 		if (diff > 0) {
			prtln ("\tsourceFile.lastModified(): " + sourceFile.lastModified());
			prtln ("\tdocReader.getLastModified(): " + docReader.getLastModified());
			prtln ("\t --> diff: " + diff);
		} */
		return diff > granularity;
	}


	/**
	 *  Reindexes the annotated item record of an annotation record so the
	 *  annotated item record is linked to its annotation.
	 *
	 * @param  annoDocMap     NOT YET DOCUMENTED
	 * @exception  Exception  Description of the Exception
	 */
	public void indexAnnotatedRecord(DocMap annoDocMap)
		 throws Exception {
		prtln("indexAnnotatedRecord()");

		MetaDataFramework annoFramework = getMetaDataFramework("dlese_anno");
		if (annoFramework == null)
			throw new Exception("dlese_anno framework not found");

		RepositoryManager repositoryManager = this.getRepositoryManager();
		if (repositoryManager == null)
			throw new Exception("repository manager not found");

		String version = annoFramework.getVersion();
		String itemIDpath = "/annotationRecord/item/itemID";
		prtln("anno framework version: " + version);
		if (annoFramework.getVersion().equals("1.0.00"))
			itemIDpath = "/annotationRecord/itemID";

		String itemID = null;
		try {
			itemID = annoDocMap.selectSingleNode(itemIDpath).getText();
		} catch (Throwable t) {
			prtlnErr("indexAnnotatedRecord unable to obtain itemID: " + t.getMessage());
		}

		if (itemID != null && itemID.length() > 0 &&
			repositoryManager.getRecord(itemID) != null)
			try {
				this.updateRecord(itemID);
			} catch (Exception e) {
				String errorMsg = "failed to index annotated record: " + e.getMessage();
				e.printStackTrace();
				throw new Exception(errorMsg);
			}
	}


	/**
	 *  Update the status of a metadata record with provided statusEntry instance.
	 *
	 * @param  recId          id of record to update
	 * @param  statusEntry    new status for record
	 * @exception  Exception  if record cannot be saved and indexed with provided
	 *      status
	 */
	public void updateRecordStatus(String recId, StatusEntry statusEntry)
		 throws Exception {

		prtln("\nupdateRecordStatus() : recId is " + recId);
		// prtln (statusEntry.getElement().asXML());

		RepositoryManager repositoryManager = getRepositoryManager();
		DcsDataManager dcsDataManager = getDcsDataManager();
		// write dcsDataRecord to disk so we can revert if necessary
		DcsDataRecord dcsDataRecord = dcsDataManager.getDcsDataRecord(recId, repositoryManager);
		dcsDataRecord.flushToDisk();

		// update the status
		dcsDataRecord.updateStatus(statusEntry);
		dcsDataRecord.setLastTouchDate(getDateString());
		dcsDataRecord.setLastEditor(statusEntry.getEditor());

		// update the recordXml
		ResultDoc resultDoc = repositoryManager.getRecord(recId);
		XMLDocReader docReader = (XMLDocReader) resultDoc.getDocReader();
		String recordXml = docReader.getXml();

		try {
			repositoryWriter.writeRecord(recId, recordXml, docReader, dcsDataRecord);
		} catch (Exception e) {
			dcsDataManager.revertToSaved(recId);
			/* throw new Exception("Could not save record: " + e.getMessage()); */
			throw e;
		}
		dcsDataRecord.flushToDisk();
	}


	/**
	 *  Save an edited record to disk and update the index accordingly.
	 *
	 * @param  recId          id of metadata record to be saved
	 * @param  doc            metadata record as dom4j.Document
	 * @param  user           NOT YET DOCUMENTED
	 * @exception  Exception  if unable to successfully update index
	 */
	public void saveEditedRecord(String recId, Document doc, User user) throws Exception {

		XMLDocReader docReader = this.getXMLDocReader(recId, this.getRepositoryManager());
		if (docReader == null)
			throw new Exception("saveEditedRecord could not find " + recId + " in index");
		String xmlFormat = docReader.getNativeFormat();
		DcsDataManager dcsDataManager = this.getDcsDataManager();
		if (dcsDataManager == null)
			throw new Exception("DcsDataManager not found in servlet context");
		MetaDataFramework framework = this.getMetaDataFramework(xmlFormat);
		String recordXml = framework.getWritableRecordXml(doc);

		// update the isValid and lastTouchDate of the DcsDataRecord
		DcsDataRecord dcsDataRecord =
			dcsDataManager.getDcsDataRecord(recId, getRepositoryManager());

		// save the dcsDataRecord in case we need to roll back
		dcsDataRecord.flushToDisk();

		this.validateRecord(recordXml, dcsDataRecord, xmlFormat);
		String userName = (user != null ? user.getUsername() : Constants.UNKNOWN_EDITOR);

		dcsDataRecord.setLastTouchDate(getDateString());
		// prtln ("dcsDataRecord.setLastEditor: " + userName);
		dcsDataRecord.setLastEditor(userName);

		// normalize status by setting status to NEW if it was previously UNKNOWN
		if (dcsDataRecord.getStatus().equals(StatusFlags.UNKNOWN_STATUS) &&
			dcsDataRecord.getEntryList().size() == 1) {

			String statusNote = "Auto-changing status from \"" +
				StatusFlags.UNKNOWN_STATUS + "\" to \"" + StatusFlags.NEW_STATUS + "\"";
			dcsDataRecord.updateStatus(StatusFlags.NEW_STATUS, statusNote, userName);
		}

		Exception thrown = null;
		try {
			repositoryWriter.writeRecord(recId, recordXml, docReader, dcsDataRecord);
		} catch (RecordUpdateException e) {
			thrown = e;
		} catch (Exception e) {
			thrown = e;
		}
		if (thrown != null) {
			dcsDataManager.revertToSaved(recId);
			/* throw new Exception("Could not save record: " + e.getMessage()); */
			prtln("throwing " + thrown.getClass().getName());
			throw thrown;
		}
		dcsDataRecord.flushToDisk();

	}


	/**
	 *  Saves and indexes a newly created record. New records cannot be found in
	 *  the index, so recId,recordXml, and collection must be passed as parameters,
	 *  rather than derived from a docReader. <p>
	 *
	 *  Callers of this method are controllers who collect key information from
	 *  user and create an item-level metadata record outside of the metadata
	 *  editor (currently only CreateADNRecordAction.handleNewRecordRequest does
	 *  this).
	 *
	 * @param  recId          record id
	 * @param  recordXml      record content as delocalized xml string
	 * @param  collection     collection key (e.g., "dcc")
	 * @param  username       NOT YET DOCUMENTED
	 * @exception  Exception  if unable to save new record
	 */
	public void saveNewRecord(String recId,
	                          String recordXml,
	                          String collection,
	                          String username)
		 throws Exception {

		prtln("\nsaveNewRecord()");

		RepositoryManager rm = getRepositoryManager();
		DcsDataManager dcsDataManager = getDcsDataManager();

		DcsSetInfo setInfo = SchemEditUtils.getDcsSetInfo(collection, rm);
		String xmlFormat = setInfo.getFormat();

		String fileName = recId + ".xml";

		DcsDataRecord dcsDataRecord = dcsDataManager.getDcsDataRecord(collection, xmlFormat, fileName, recId);
		validateRecord(recordXml, dcsDataRecord, xmlFormat);

		// update status
		dcsDataRecord.setLastTouchDate(getDateString());
		dcsDataRecord.updateStatus(StatusFlags.NEW_STATUS, "Record Created", username);

		try {
			repositoryWriter.writeRecord(recordXml, xmlFormat, collection, recId, dcsDataRecord);
		} catch (Exception e) {
			dcsDataManager.revertToSaved(recId);
			throw new Exception("Could not save record: " + e.getMessage());
		}
		dcsDataRecord.flushToDisk();
	}


	/**
	 *  NOT CURRENTLY USED - still have to work out the mechanics of saving
	 *  collection data ...
	 *
	 * @param  recId          NOT YET DOCUMENTED
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	/* 	public void saveCollectionData (SetInfo setInfo) throws Exception {
		DcsDataRecord dcsDataRecord = null;
		CollectionConfig config = null;
		try {
			RepositoryManager rm = getRepositoryManager();
			dcsDataRecord = getDcsDataManager().getDcsDataRecord (setInfo.getId(), rm);
			config =
				getCollectionRegistry().getCollectionConfig(setInfo.getSetSpec());
		} catch (Throwable t) {
			throw new Exception ("saveCollectionData setup error: " + t);
		}
		this.repositoryWriter.writeCollectionData (config, dcsDataRecord);
	} */

	/**
	 *  Updates the record by writing it to the repository. NOTE: Does NOT affect
	 *  the metadata but updates the indexed record, which includes DcsDataRecord
	 *  information. Therefore, this method is appropriate for indexing updated
	 *  DcsDataRecord information, but it is not appropriate for updating changes
	 *  to metadata content.<p>
	 *
	 *  Called by DCSSchemEditAction.indexAnnotatedRecord() and ThreadedServices.validate().
	 *
	 * @param  recId          Description of the Parameter
	 * @exception  Exception  Description of the Exception
	 */
	public void updateRecord(String recId)
		 throws Exception {

		prtln("\nupdateRecord()");

		RepositoryManager rm = getRepositoryManager();
		ResultDoc resultDoc = rm.getRecord(recId);
		XMLDocReader docReader = (XMLDocReader) resultDoc.getDocReader();
		String recordXml = docReader.getXml();
		String xmlFormat = docReader.getNativeFormat();
		String collection = docReader.getCollection();

		DcsDataRecord dcsDataRecord = getDcsDataManager().getDcsDataRecord(recId, rm);
		repositoryWriter.writeRecord(recordXml, xmlFormat, collection, recId, dcsDataRecord);
	}


	/**
	 *  Validate the record and update the DcsDataRecord. NOTE: DcsDataRecord is
	 *  not saved here - it must be saved (flushed to disk) by the caller.
	 *
	 * @param  dcsData    status record corresponding to the xml record.
	 * @param  record     An xml record, represented either as String or File
	 * @param  xmlFormat  NOT YET DOCUMENTED
	 */
	public void validateRecord(Object record, DcsDataRecord dcsData, String xmlFormat) {

		if (dcsData == null) {
			prtln("WARNING: dcsDataRecord is NULL");
			return;
		}

		MetaDataFramework framework = this.getMetaDataFramework(xmlFormat);
		if (framework == null) {
			prtlnErr("validateRecord ERROR: framework not found for " + xmlFormat);
			// throw new Exception ("validateRecord ERROR: framework not found for " + xmlFormat);
		}
		String validationReport =
			(framework != null ? framework.validateRecord(record) : Constants.UNKNOWN_VALIDITY);
		dcsData.setValidationReport(validationReport);
	}


	/**
	 *  check for dup url in this collection
	 *
	 * @param  collection     Description of the Parameter
	 * @param  records        NOT YET DOCUMENTED
	 * @return                Description of the Return Value
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	/* 	public boolean collectionContainsUrlX(String url, String collection) {
		return (getDups(url, collection).size() > 0);
	} */

	/**
	 *  Moves a batch of records into the specified collection.
	 *
	 * @param  records        A list of records to move to collection
	 * @param  collection     collection key of the destination collection
	 * @return                returns list of records that could not be moved, if
	 *      any
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public RecordList batchMoveRecords(RecordList records, String collection) throws Exception {

		prtln("\nbatchMoveRecords()");

		RecordList failures = new RecordList(getRepositoryManager().getIndex());
		if (records.isEmpty()) {
			prtln("batchMoveRecords no records to move");
		}
		else {
			for (Iterator i = records.iterator(); i.hasNext(); ) {
				String id = (String) i.next();
				String newId = "";
				try {
					newId = moveRecord(id, collection);
				} catch (Exception e) {
					prtlnErr("batchMoveRecords failed to move " + id);
					failures.add(id);
				}
			}
		}
		return failures;
	}

	/**
	 *  Moves a batch of records into the specified collection.
	 *
	 * @param  records        A list of records to move to collection
	 * @param  collection     collection key of the destination collection
	 * @return                returns list of records that could not be moved, if
	 *      any
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public RecordList batchCopyMoveRecords(RecordList records, String collection) throws Exception {

		prtln("\nbatchCopyMoveRecords()");

		RecordList failures = new RecordList(getRepositoryManager().getIndex());
		if (records.isEmpty()) {
			prtln("batchCopyMoveRecords no records to move");
		}
		else {
			for (Iterator i = records.iterator(); i.hasNext(); ) {
				String id = (String) i.next();
				String newId = "";
				try {
					newId = copyMoveRecord(id, collection);
				} catch (Exception e) {
					prtlnErr("batchCopyMoveRecords failed to move " + id);
					failures.add(id);
				}
			}
		}
		return failures;
	}
	
	
	/**
	 *  Updates the status of a set of records (but does not validate or update the
	 *  lastTouchDate.
	 *
	 * @param  records        an array of records represented as ResultDocs
	 * @param  statusEntry    contains status, statusNote and editor information to
	 *      be added to each record
	 * @return                a list of records that could not be updated
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public RecordList batchStatusUpdate(RecordList records, StatusEntry statusEntry) throws Exception {

		prtln("\nbatchStatusUpdate()");

		RecordList failures = new RecordList(getRepositoryManager().getIndex());
		RepositoryManager rm = getRepositoryManager();

		if (records.isEmpty()) {
			prtln("batchStatusUpdate no records to move");
		}
		else {
			for (Iterator i = records.iterator(); i.hasNext(); ) {
				String id = (String) i.next();

				try {
					this.updateRecordStatus(id, statusEntry);
					// updateStatus(id, statusEntry, servletContext, true);
				} catch (Exception e) {
					prtlnErr("batchStatusUpdate failed to update status for " + id +
						": " + e.getMessage());
					failures.add(id);
				}
			}
		}
		return failures;
	}


	/**
	 *  Move record to the destination collection, returning the ID of the new
	 *  record. Status and lastTouchDate are updated in the DcsDataRecord, but the
	 *  metadata is not re-validated.<p>
	 *
	 *  Ensures that source and destination collections are different before making
	 *  move, so batch moves don't have to make this check.<p>
	 *
	 *
	 *
	 * @param  recId          id of the record to be moved
	 * @param  collection     Destination collection for the record to be moved
	 * @return                id of the moved record
	 * @exception  Exception  Description of the Exception
	 */

	 public String moveRecord(String recId, String collection)
		 throws Exception {

		prtln("\nmoveRecord()");
		RepositoryManager rm = getRepositoryManager();
		DcsDataManager dcsDataManager = getDcsDataManager();

		// extract info from the record to be moved
		XMLDocReader docReader = RepositoryService.getXMLDocReader(recId, rm);
		String xmlFormat = docReader.getNativeFormat();
		String formerCollection = docReader.getCollection();
		String formerCollectionName = formerCollection;

		// don't bother moving is source and destination collections are the same
		if (collection.equals(formerCollection)) {
			prtlnErr("source and destination records are the same: " + collection);
			return recId;
		}

		try {
			formerCollectionName = docReader.getMyCollectionDoc().getShortTitle();
		} catch (Exception e) {}

		MetaDataFramework framework = getFrameworkRegistry().getFramework(xmlFormat);
		if (framework == null)
			throw new Exception("\"" + xmlFormat + "\" framework is not loaded");

		// transplant a new ID in the metadata record
		CollectionConfig newCollectionConfig = null;
		try {
			newCollectionConfig = getCollectionRegistry().getCollectionConfig(collection, false);
		} catch (Throwable t) {
			throw new Exception("collection config not found for " + collection);
		}
		// String newId = getCollectionRegistry().nextID(collection);
		String newId = newCollectionConfig.nextID();
		// prtln("new id: " + newId);
		String newRecordXml = SchemEditUtils.stuffId(docReader.getXml(), newId, framework);

		// flush the DcsDataRecord - necessary to support rollback
		DcsDataRecord dcsDataRecord = dcsDataManager.getDcsDataRecord(recId, rm);
		dcsDataRecord.flushToDisk();
		// remove old record from cache - NECESSARY because we are about to change it to
		// represent moved record!
		dcsDataManager.removeFromCache(recId);

		// now convert dcsDataRecord to reflect move by changing id, sourceFile, etc.
		// when the dcsDataRecors is now flushed, it is written to the new location.
		String newFileName = newId + ".xml";
		File collectionDir = dcsDataManager.getCollectionDir(collection, xmlFormat);
		File newSource = new File(collectionDir, newFileName);
		dcsDataRecord.setSource(newSource);
		dcsDataRecord.setCollectionConfig(newCollectionConfig);
		dcsDataRecord.setId(newId);
		dcsDataRecord.clearNdrInfo();

		/*
		    update status to reflect move - set the status of the dcsDataRecord
		    to "Imported". NOTE: we assume the fileName will be of the form
		    newId+".xml" because that is the default behaviour of the
		    RepositoryManager and this is a NEW record so we know the default
		    behaviour will be used
		*/
		String status = StatusFlags.IMPORTED_STATUS;
		String statusNote = "Moved from " + formerCollectionName + " collection (was " + recId + ")";
		String editor = "";
		dcsDataRecord.updateStatus(status, statusNote, editor);
		dcsDataRecord.setLastTouchDate(getDateString());

		// put the modified dcsDataRecord in the cache where it will be found by the indexer
		// note: it is only written to disk if the move operation succeeds
		dcsDataManager.cacheRecord(dcsDataRecord);

		// make dataMap for repositoryEventListeners
		Map dataMap = new HashMap();
		dataMap.put ("srcId", recId);
		dataMap.put ("srcCol", formerCollection);
		dataMap.put ("dstId", newId);
		dataMap.put ("dstCol", collection);
		
		
		try {
			repositoryWriter.writeRecord(newRecordXml, xmlFormat, collection, newId, dcsDataRecord);
			this.notifyListeners("moveRecord", dataMap);
		} catch (Exception e) {
			// move has failed, remove dcsDataRecord from cache
			dcsDataManager.removeFromCache(newId);
			prtlnErr("moveRecord ERROR: " + e.getMessage());
			e.printStackTrace();
			throw new Exception("Server Error: " + e.getMessage());
		}
		// move operation has succeeded
		dcsDataRecord.flushToDisk();
		deleteRecord(recId); // delete the source record at recId
		return newId;
	}


	/**
	 *  returns list of records that could not be deleted
	 *
	 * @param  records        Description of the Parameter
	 * @return                Description of the Return Value
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public RecordList batchDeleteRecords(RecordList records) throws Exception {

		prtln("\nbatchDeleteRecords()");

		RecordList failures = new RecordList(getRepositoryManager().getIndex());
		if (records.isEmpty()) {
			prtln("batchDeleteRecords no records to delete");
		}
		else {
			for (Iterator i = records.iterator(); i.hasNext(); ) {
				String id = (String) i.next();
				try {
					deleteRecord(id);
				} catch (Exception e) {
					prtlnErr("batchDeleteRecords failed to delete " + id);
					failures.add(id);
				}
			}
		}
		return failures;
	}


	/**
	 *  Delete a collection from the repository
	 *
	 * @param  collection     key of collection to be deleted
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public void deleteCollection(String collection) throws Exception {

		prtln("\ndeleteCollection()");

		CollectionConfig config = this.getCollectionRegistry().getCollectionConfig(collection);
		if (config == null)
			throw new Exception("collection does not exist for \"" + collection + "\"");
		repositoryWriter.deleteCollection(config);
		getRepositoryManager().loadCollectionRecords(true);
	}


	/**
	 *  Delete a record from the repository
	 *
	 * @param  recId          Description of the Parameter
	 * @exception  Exception  Description of the Exception
	 */
	public void deleteRecord(String recId) throws Exception {

		prtln("\ndeleteRecord() " + recId);

		RepositoryManager rm = getRepositoryManager();
		DcsDataManager dcsDataManager = getDcsDataManager();

		try {
			if (recId == null || recId.trim().length() == 0) {
				throw new Exception("No Record ID provided");
			}

			// existing dcsData Record
			DcsDataRecord dcsDataRecord = dcsDataManager.getDcsDataRecord(recId, rm);

			// make dataMap for repositoryEventListeners
			Map dataMap = new HashMap();
			dataMap.put ("srcId", recId);
			dataMap.put ("srcCol", this.getXMLDocReader (recId).getCollection());
			
			if (!repositoryWriter.deleteRecord(recId, dcsDataRecord)) {
				throw new Exception("record not found in repository");
			}

			if (dcsDataRecord == null) {
				prtlnErr("WARNING: dcsDataManager failed to find dcsDataRecord for " + recId);
			}
			else if (!dcsDataRecord.delete()) {
				prtlnErr("WARNING: Did not delete dcsDataRecord with id = " + recId);
			}
			
			this.notifyListeners("deleteRecord", dataMap);
			
		} catch (Throwable e) {
			throw new Exception("deleteRecord error: " + e.getMessage());
		}
	}


	/**
	 *  Create a new record within the same collection as the original. A new
	 *  DcsDataRecord is created with a status of "Uknown" and statusNote
	 *  signifying it is copied. Validation is not performed - the new record gets
	 *  the isValid attribute of the original.
	 *
	 * @param  originalId     id of the record to be copied
	 * @return                XMLDocReader for the new record
	 * @exception  Exception  Description of the Exception
	 */
	public XMLDocReader copyRecord(String originalId, User user)
		 throws Exception {

		prtln("\ncopyRecord() originalId: " + originalId);

		RepositoryManager rm = getRepositoryManager();

		if (originalId == null || originalId.trim().length() == 0) {
			throw new Exception("did not receive id paramter");
		}

		XMLDocReader docReader = RepositoryService.getXMLDocReader(originalId, rm);

		String collection = docReader.getCollection();
		String originalXml = docReader.getXml();
		String xmlFormat = docReader.getNativeFormat();
		File originalFile = SchemEditUtils.getFileFromIndex(originalId, rm);
		if (originalFile == null) {
			throw new Exception("No record was found in the index for ID \"" + originalId + "\"");
		}

		FrameworkRegistry frameworkRegistry = getFrameworkRegistry();
		MetaDataFramework framework = frameworkRegistry.getFramework(xmlFormat);
		if (framework == null) {
			throw new Exception("\"" + xmlFormat + "\" framework is not loaded");
		}

		CollectionConfig collectionConfig = null;
		CollectionRegistry cr = getCollectionRegistry();
		if (cr != null) {
			collectionConfig = cr.getCollectionConfig(collection);
		}

		String newId = collectionConfig.nextID();
		String recordXml = framework.copyRecord(originalXml, newId, collectionConfig, user).asXML();

		// now PUT the record!!
		String fileName = newId + ".xml";
		// prtln("writing new " + xmlFormat + " record to " + fileName);

		// new dcsData Record - keep the isValid but update dates and status
		DcsDataManager dcsDataManager = getDcsDataManager();
		DcsDataRecord originalDcsData = dcsDataManager.getDcsDataRecord(originalId, rm);
		DcsDataRecord newDcsData = dcsDataManager.getDcsDataRecord(collection, xmlFormat, fileName, newId);

		newDcsData.setValidationReport(originalDcsData.getValidationReport());
		newDcsData.setLastTouchDate(getDateString());

		String statusNote = "Copied from " + originalId;
		String editor = (user != null ? user.getUsername() : Constants.UNKNOWN_EDITOR);
		newDcsData.updateStatus(StatusFlags.NEW_STATUS, statusNote, editor);

		// make dataMap for repositoryEventListeners
		Map dataMap = new HashMap();
		dataMap.put ("srcId", originalId);
		dataMap.put ("srcCol", collection);
		dataMap.put ("dstId", newId);
		dataMap.put ("dstCol", collection);
		
		prtln("about to call repositoryWriter.writeRecord");
		try {

			repositoryWriter.writeRecord(recordXml, xmlFormat, collection, newId, newDcsData);
			this.notifyListeners("copyRecord", dataMap);
		} catch (RecordUpdateException e) {
			dcsDataManager.revertToSaved(newId);
			prtlnErr("Record Copy error: " + e.getMessage());
			throw new Exception(e);
		}
		newDcsData.flushToDisk();
		return RepositoryService.getXMLDocReader(newId, rm);
	}


	/**
	 *  Write a copied version of a metadata record into a destination collectioni
	 *
	 * @param  originalId      NOT YET DOCUMENTED
	 * @param  destCollection  NOT YET DOCUMENTED
	 * @return                 NOT YET DOCUMENTED
	 * @exception  Exception   NOT YET DOCUMENTED
	 */
	public String copyMoveRecord(String srcRecordId, String destCollection)
		 throws Exception {

		prtln("\ncopyMoveRecord()");
		RepositoryManager rm = getRepositoryManager();
		DcsDataManager dcsDataManager = getDcsDataManager();

		// extract info from the record to be moved
		XMLDocReader docReader = RepositoryService.getXMLDocReader(srcRecordId, rm);
		String xmlFormat = docReader.getNativeFormat();
		String srcCollection = docReader.getCollection();
		
		// framework must be loaded
		MetaDataFramework framework = getFrameworkRegistry().getFramework(xmlFormat);
		if (framework == null)
			throw new Exception("\"" + xmlFormat + "\" framework is not loaded");

		// don't bother moving is source and destination collections are the same
		if (destCollection.equals(srcCollection)) {
			prtlnErr("source and destination records are the same: " + destCollection);
			return srcRecordId;
		}

		String srcCollectionName = srcCollection;
		try {
			srcCollectionName = docReader.getMyCollectionDoc().getShortTitle();
		} catch (Exception e) {}

		// verify that the destination collection exists and is proper format
		CollectionConfig destCollectionConfig = null;
		try {
			destCollectionConfig = getCollectionRegistry().getCollectionConfig(destCollection, false);
		} catch (Throwable t) {
			throw new Exception("collection config not found for " + destCollection);
		}
		
		if (!destCollectionConfig.getXmlFormat().equals(xmlFormat))
			throw new Exception ("source and destination collections have different formats");
		
		// transplant a new ID in the metadata record
		String destId = destCollectionConfig.nextID();
		String destRecordXml = SchemEditUtils.stuffId(docReader.getXml(), destId, framework);

		// ---- set up DcsDataRecord - this should look like "copy" -----
		// new dcsData Record - keep the isValid but update dates and status
		DcsDataRecord srcDcsData = dcsDataManager.getDcsDataRecord(srcRecordId, rm);
		String destFileName = destId + ".xml";
		DcsDataRecord destDcsData = dcsDataManager.getDcsDataRecord(destCollection, xmlFormat, destFileName, destId);

		destDcsData.setValidationReport(srcDcsData.getValidationReport());
		destDcsData.setLastTouchDate(getDateString());

		String statusNote = "Copied from " + srcCollectionName + " collection (was " + srcRecordId + ")";
		destDcsData.updateStatus(StatusFlags.IMPORTED_STATUS, statusNote, Constants.UNKNOWN_EDITOR);

		// make dataMap for repositoryEventListeners
		Map dataMap = new HashMap();
		dataMap.put ("srcId", srcRecordId);
		dataMap.put ("srcCol", srcCollection);
		dataMap.put ("dstId", destId);
		dataMap.put ("dstCol", destCollection);
		
		prtln("about to call repositoryWriter.writeRecord");
		try {
			repositoryWriter.writeRecord(destRecordXml, xmlFormat, destCollection, destId, destDcsData);
			this.notifyListeners ("copyMoveRecord", dataMap);
		} catch (RecordUpdateException e) {
			dcsDataManager.revertToSaved(destId);
			prtlnErr("Record Copy error: " + e.getMessage());
			throw new Exception(e);
		}
		destDcsData.flushToDisk();
		return destId;

	}

	/**
	 *  Gets the dcsDataRecord attribute of the RepositoryService object
	 *
	 * @param  recId          NOT YET DOCUMENTED
	 * @return                The dcsDataRecord value
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public DcsDataRecord getDcsDataRecord(String recId) throws Exception {
		DcsDataManager dcsDataManager = this.getDcsDataManager();
		if (dcsDataManager == null)
			throw new Exception("DcsDataManager not found");
		return dcsDataManager.getDcsDataRecord(recId, this.getRepositoryManager());
	}


	/**
	 *  Gets the xMLDocReader attribute of the RepositoryService object
	 *
	 * @param  id             NOT YET DOCUMENTED
	 * @return                The xMLDocReader value
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public XMLDocReader getXMLDocReader(String id) throws Exception {
		RepositoryManager rm = null;
		try {
			rm = getRepositoryManager();
		} catch (Exception e) {
			prtlnErr("WARNING: repositoryManager not found by RepositoryService");
			return null;
		}
		return getXMLDocReader(id, rm);
	}


	/**
	 *  Gets the XMLDocReader associated with an id
	 *
	 * @param  id             Record ID
	 * @param  rm             RepositoryManager
	 * @return                XMLDocReader or null if not found
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public static XMLDocReader getXMLDocReader(String id, RepositoryManager rm) throws Exception {
		ResultDoc record = rm.getRecord(id);
		if (record == null) {
			throw new Exception("getXMLDocReader() indexed record not found for id: " + id);
			//return null;
		}
		try {
			return (XMLDocReader) record.getDocReader();
		} catch (Exception e) {
			throw new Exception("getXMLDocReader found unexpected docReader class " + e.getMessage());
			// return null;
		}
	}


	/**
	 *  Gets the recordFormat attribute of the RepositoryService class
	 *
	 * @param  id             NOT YET DOCUMENTED
	 * @param  rm             NOT YET DOCUMENTED
	 * @return                The recordFormat value
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public static String getRecordFormat(String id, RepositoryManager rm) throws Exception {
		XMLDocReader reader = getXMLDocReader(id, rm);
		if (reader == null)
			throw new Exception("Record (" + id + ") not found in the index");
		return reader.getNativeFormat();
	}


	/**
	 *  Update the list of sets.
	 *
	 * @return    The sets value
	 */
	public ArrayList getSetInfos() {
		ArrayList sets = new ArrayList();

		RepositoryManager rm = null;
		try {
			rm = this.getRepositoryManager();
		} catch (Exception e) {
			prtlnErr(e.getMessage());
			return sets;
		}

		try {
			List setInfos = rm.getSetInfos();
			if (setInfos != null) {
				for (int i = 0; i < setInfos.size(); i++) {
					SetInfo setInfo = (SetInfo) setInfos.get(i);
					setInfo.setSetInfoData(rm);

					String setSpec = setInfo.getSetSpec();

					// we don't provide access to "collect" collection (master Collection records)
					if (setSpec.equals("collect")) {
						continue;
					}

					DcsSetInfo dcsSetInfo = new DcsSetInfo(setInfo);
					dcsSetInfo.setSetInfoData(rm);

					try {
						if (getCollectionRegistry() == null)
							throw new Exception("getCollectionRegistry() == null");
						CollectionConfig config = getCollectionRegistry().getCollectionConfig(setSpec);
						if (config == null)
							throw new Exception("config is null");
						dcsSetInfo.setAuthority(config.getAuthority());
						dcsSetInfo.setIdPrefix(config.getIdPrefix());
						dcsSetInfo.setFinalStatusFlag(config.getFinalStatusFlag());
						// prtln ("set authority to: " + config.getAuthority());
					} catch (Throwable t) {
						prtlnErr("trouble configuring dcsSetInfo for \"" + setInfo.getName() + "\": " + t.getMessage());
						t.printStackTrace();
					}

					sets.add(dcsSetInfo);

				}
				Collections.sort(sets);
			}
		} catch (Throwable e) {
			prtlnErr("getSets() error: " + e);
			sets = new ArrayList();
		}
		return sets;
	}


	/**
	 *  Returns true if the specified user is authorized for the specified
	 *  collection (set).
	 *
	 * @param  collection    NOT YET DOCUMENTED
	 * @param  user          NOT YET DOCUMENTED
	 * @param  requiredRole  NOT YET DOCUMENTED
	 * @return               The authorizedSet value
	 */
	public boolean isAuthorizedSet(String collection, User user, Roles.Role requiredRole) {
		/*
			the following logic required to enable admins to edit collection records without having
			the collection of collection records to be considered an "authorizedSet".
		*/
		boolean authenticationEnabled = (Boolean) servletContext.getAttribute("authenticationEnabled");
		if (!authenticationEnabled)
			return true;
		if (user != null && user.hasRole(Roles.ADMIN_ROLE))
			return true;

		List authorizedSets = getAuthorizedSets(user, requiredRole);
		if (authorizedSets == null)
			return false;
		for (Iterator i = authorizedSets.iterator(); i.hasNext(); ) {
			SetInfo setInfo = (SetInfo) i.next();
			if (setInfo.getSetSpec().equals(collection))
				return true;
		}
		return false;
	}


	/*
	* Gets list of sets (collections) for which the specified user is authorized.
	*/
	/**
	 *  Gets the authorizedSets attribute of the RepositoryService object
	 *
	 * @param  user          NOT YET DOCUMENTED
	 * @param  requiredRole  NOT YET DOCUMENTED
	 * @return               The authorizedSets value
	 */
	public List getAuthorizedSets(User user, Roles.Role requiredRole) {
		List sets = this.getSetInfos();
		// prtln ("getAuthorizedSets() requiredRole: " + requiredRole);

		boolean authenticationEnabled = (Boolean) servletContext.getAttribute("authenticationEnabled");

		if (requiredRole == Roles.NO_ROLE ||
			!authenticationEnabled) {
			// prtln ("Role not provided -- returning all sets");
			return sets;
		}

		if (user == null) {
			// prtln ("User not provided -- returning no sets");
			return new ArrayList();
		}

		List authorizedSets = new ArrayList();

		for (Iterator i = sets.iterator(); i.hasNext(); ) {
			SetInfo setInfo = (SetInfo) i.next();
			String name = setInfo.getName();
			// prtln ("\t name: " + name + "  collection: " + setInfo.getSetSpec());
			if (user.hasRole(requiredRole, setInfo.getSetSpec())) {
				authorizedSets.add(setInfo);
				// prtln ("\t authorized for " + setInfo.getName());
			}
			else {
				// prtln ("\t denied for " + setInfo.getName());
			}
		}
		return authorizedSets;
	}


	/**
	 *  Gets all item records for the specified collection by performing index
	 *  query.
	 *
	 * @param  collection  collection key
	 * @return             RecordList containing IDs of item records
	 */
	public RecordList getCollectionItemRecords(String collection) {
		RepositoryManager rm = null;
		try {
			rm = this.getRepositoryManager();
		} catch (Exception e) {
			prtlnErr(e.getMessage());
			return null;
		}

		String query = " ((collection:0*)) AND (collection:0" + collection + ")";
		return new RecordList(query, rm.getIndex());
	}

	// ----------listener management --------------------------
	/**
	 *  Description of the Method
	 *
	 * @param  dcsDataRecord  NOT YET DOCUMENTED
	 */
	void notifyListeners(String eventName, Map eventData) {
		RepositoryEvent event = new RepositoryEvent(eventName, eventData);

		for (int i = 0; i < listeners.size(); i++) {
			try {
				((RepositoryEventListener) listeners.get(i)).handleEvent(event);
			} catch (Throwable t) {
				prtln("WARNING: Unexpected exception occurred while notifying listeners..." + t.getMessage());
				t.printStackTrace();
			}
		}
	}


	/**
	 *  Adds a feature to the Listener attribute of the DcsDataRecord object
	 *
	 * @param  listener  The feature to be added to the Listener attribute
	 */
	public void addListener(RepositoryEventListener listener) {
		prtln ("addListerner with " + listener.getClass().getName());
		if (listener != null) {
			if (listeners == null) {
				listeners = new ArrayList();
			}
			else if (listeners.contains(listener)) {
				return;
			}

			listeners.add(listener);
		}
	}


	/**
	 *  Description of the Method
	 *
	 * @param  listener  Description of the Parameter
	 */
	public void removeListener(RepositoryEventListener listener) {
		if (listener != null) {
			int index = listeners.indexOf(listener);
			if (index > -1) {
				try {
					listeners.remove(index);
				} catch (IndexOutOfBoundsException ioobe) {
					return;
				}
			}
		}
	}
	
	// ----------------- util ---------------------

	/**
	 *  Gets the dateString attribute of the RepositoryService class
	 *
	 * @return    The dateString value
	 */
	public static String getDateString() {
		return SchemEditUtils.fullDateString(new Date());
	}


	/**
	 *  Print a line to standard out.
	 *
	 * @param  s  The String to print.
	 */
	static void prtln(String s) {
		if (debug) {
			SchemEditUtils.prtln(s, "RepositoryService");
		}
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  s  NOT YET DOCUMENTED
	 */
	static void prtlnErr(String s) {
		SchemEditUtils.prtln(s, "RepositoryService");
	}
}

