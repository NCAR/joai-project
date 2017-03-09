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
package org.dlese.dpc.schemedit.dcs;
import org.dlese.dpc.schemedit.*;
import org.dlese.dpc.schemedit.config.*;

import java.io.*;
import java.util.*;
import java.text.*;

import org.dlese.dpc.index.*;
import org.dlese.dpc.repository.RepositoryManager;
import org.dlese.dpc.schemedit.repository.RepositoryService;
import org.dlese.dpc.index.reader.*;
import org.dlese.dpc.xml.*;
import org.dlese.dpc.util.*;

/**
 *  Provides access to, and caching of {@link org.dlese.dpc.schemedit.dcs.DcsDataRecord}
 *  instances that house workflow status associated with each metadata record
 *  managed by the DCS. <p>
 *
 *  Dcs-data is stored on disk as xml files and read into {@link DcsDataRecord}
 *  instances which are subsequently accessed via the cache.<p>
 *
 *  DcsDataRecords are removed from cache via the {@link #revertToSaved(String)}
 *  method, which forces the DcsDataRecord to be read from disk the next time it
 *  is requested.
 *
 * @author    ostwald $Id: DcsDataManager.java,v 1.4 2004/12/08 17:05:37 ostwald
 *      Exp $
 */
public class DcsDataManager {

	static boolean debug = false;
	File dcsDataDir = null;
	MetaDataFramework dcsDataFramework = null;
	CollectionRegistry collectionRegistry = null;
	HashMap cache = new HashMap();
	private boolean cachingEnabled = true;
	private ArrayList listeners;


	/**
	 *  Create a DcsDataManager.
	 *
	 * @param  rm                  Description of the Parameter
	 * @param  dcsDataFramework    MetaDataFramework for the dcs_data format
	 * @param  collectionRegistry  NOT YET DOCUMENTED
	 */
	public DcsDataManager(RepositoryManager rm, MetaDataFramework dcsDataFramework, CollectionRegistry collectionRegistry) {
		this.dcsDataDir = getDcsDataDir(rm);
		this.collectionRegistry = collectionRegistry;
		this.dcsDataFramework = dcsDataFramework;
	}


	/**
	 *  Gets the dcsDataDir attribute of the DcsDataManager object
	 *
	 * @param  rm  Description of the Parameter
	 * @return     The dcsDataDir value
	 */
	public static File getDcsDataDir(RepositoryManager rm) {

		String metadataRecordsLocation = rm.getMetadataRecordsLocation();
		return getDcsDataDir(metadataRecordsLocation);
		/* 		File dcsDataDir = new File(metadataRecordsLocation, "dcs_data");
		if (!dcsDataDir.exists()) {
			dcsDataDir.mkdir();
			prtln("dcsDataDir created");
		}
		return dcsDataDir; */
	}


	/**
	 *  Gets the dcsDataDir attribute of the DcsDataManager class
	 *
	 * @param  metadataRecordsLocation  NOT YET DOCUMENTED
	 * @return                          The dcsDataDir value
	 */
	public static File getDcsDataDir(String metadataRecordsLocation) {
		File dcsDataDir = new File(metadataRecordsLocation, "dcs_data");
		if (!dcsDataDir.exists()) {
			dcsDataDir.mkdir();
			prtln("dcsDataDir created");
		}
		return dcsDataDir;
	}


	/**
	 *  Get the directory in which dcs_data records are stored for the given
	 *  collection and metadataFormat. Creates necessary directories.
	 *
	 * @param  collection  Description of the Parameter
	 * @param  xmlFormat   Description of the Parameter
	 * @return             The collectionDir value
	 */
	public File getCollectionDir(String collection, String xmlFormat) {
		File formatDir = new File(dcsDataDir, xmlFormat);
		if (!formatDir.exists()) {
			prtln("creating formatDir: " + formatDir.toString());
			formatDir.mkdir();
		}

		File collectDir = new File(formatDir, collection);
		if (!collectDir.exists()) {
			prtln("creating collectDir: " + collectDir.toString());
			collectDir.mkdir();
		}
		return collectDir;
	}


	/**
	 *  Gets the dcsStatusOptions attribute of the DcsDataManager object
	 *
	 * @return    The dcsStatusOptions value
	 */
	public List getDcsStatusOptions() {
		List statusOptions = new ArrayList();
		try {
			statusOptions = dcsDataFramework.getSchemaHelper().getEnumerationValues("dcsStatusType", false);
		} catch (Exception e) {
			prtln("getStatusOptions error: " + e.getMessage());
		}
		// prtln ("getStatusOptions returning " + statusOptions);
		return statusOptions;
	}


	/**  NOT YET DOCUMENTED */
	public synchronized void normalizeStatuses() {
		prtln("normaliseStatuses for all collections");
		for (Iterator i = cache.values().iterator(); i.hasNext(); ) {
			DcsDataRecord rec = (DcsDataRecord) i.next();
			rec.normalizeStatus();
		}
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  collection  NOT YET DOCUMENTED
	 */
	public synchronized void normalizeStatuses(String collection) {
		prtln("normaliseStatuses for " + collection);
		for (Iterator i = cache.values().iterator(); i.hasNext(); ) {
			DcsDataRecord rec = (DcsDataRecord) i.next();
			if (rec.getCollection().equals(collection))
				rec.normalizeStatus();
		}
	}


	/**
	 *  Retrieve the DcsDataRecord correponding to the given record ID
	 *
	 * @param  id  Description of the Parameter
	 * @param  rm  Description of the Parameter
	 * @return     The dcsDataRecord value
	 */
	public DcsDataRecord getDcsDataRecord(String id, RepositoryManager rm) {
		// prtln ("getDcsDataRecord with id = " + id);

		DcsDataRecord dcsDataRecord = null;

		if (cachingEnabled) {
			dcsDataRecord = (DcsDataRecord) cache.get(id);
		}

		if (dcsDataRecord == null) {

			XMLDocReader docReader = null;
			try {
				docReader = RepositoryService.getXMLDocReader(id, rm);
			} catch (Exception e) {
				prtln("couldn't get docReader: " + e.getMessage());
				return null;
			}

			String xmlFormat = docReader.getMetadataPrefix();
			String collection = docReader.getCollection();
			String fileName = docReader.getFileName();

			dcsDataRecord = getDcsDataRecord(collection, xmlFormat, fileName, id);
		}
		if (dcsDataRecord == null)
			prtln("\t WARNING: returning null record");
		return dcsDataRecord;
	}


	/**
	 *  Gets a dcsDataRecord without specifying an id. Necessary to support
	 *  indexing, {@link org.dlese.dpc.schemedit.dcs.DcsDataFileIndexingPlugin}
	 *  where the ID is not known or easily obtainable.
	 *
	 * @param  collection  Description of the Parameter
	 * @param  xmlFormat   Description of the Parameter
	 * @param  fileName    Description of the Parameter
	 * @return             The dcsDataRecord value
	 */
	public DcsDataRecord getDcsDataRecord(String collection, String xmlFormat, String fileName) {
		// prtln("getDcsDataRecord called without an ID");
		return getDcsDataRecord(collection, xmlFormat, fileName, null);
	}


	/**
	 *  Returns an empty dcsDataRecord corresponding to an indexed source record.
	 *  The DcsDataRecord is a place holder for dcs-data, and is asociated with a
	 *  file that will hold the dcs-data record for the source record.
	 *
	 * @param  collection  The collection of the source record
	 * @param  xmlFormat   Description of the Parameter
	 * @param  fileName    file name of the metadata file
	 * @param  id          Description of the Parameter
	 * @return             The dcsDataRecord value
	 */
	public DcsDataRecord getDcsDataRecord(String collection, String xmlFormat, String fileName, String id) {

		DcsDataRecord dcsDataRecord = null;

		if (cachingEnabled && id != null) {
			dcsDataRecord = (DcsDataRecord) cache.get(id);
		}
		else {
			prtln("getDcsDataRecord with id == null - record will not be added to cache");
		}

		if (dcsDataRecord == null) {

			if (collection == null || xmlFormat == null || fileName == null) {
				prtln("getDcsDataRecord received at least one null parameter .. returning null");
				return null;
			}

			// prtln ("***** creating dcsDataRecord for " + id);

			CollectionConfig collectionConfig = null;
			try {
				collectionConfig = collectionRegistry.getCollectionConfig(collection);
			} catch (Throwable t) {
				prtln("getDcsDataRecord failed to find collectionConfig for " + collection);
			}
			File collectionDir = getCollectionDir(collection, xmlFormat);
			File dcsDataFile = new File(collectionDir, fileName);
			dcsDataRecord = new DcsDataRecord(dcsDataFile, dcsDataFramework, collectionConfig, this);

			// add dcsDataRecord to cache if we know the id
			if (cachingEnabled && id != null && id.length() > 0) {
				dcsDataRecord.setId(id);
				cache.put(id, dcsDataRecord);
			}
		}
		return dcsDataRecord;
	}


	/**
	 *  Remove the cashed DcsDataRecord, in effect reverting to the last saved
	 *  version of the record, since it will be read from disk next time it is
	 *  accessed.
	 *
	 * @param  id  Description of the Parameter
	 * @return     Description of the Return Value
	 */
	public synchronized boolean revertToSaved(String id) {
		return removeFromCache(id);
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  id  NOT YET DOCUMENTED
	 * @return     NOT YET DOCUMENTED
	 */
	public synchronized boolean removeFromCache(String id) {
		return (cache.remove(id) != null);
	}


	/**
	 *  Description of the Method
	 *
	 * @param  dcsDataRecord  Description of the Parameter
	 */
	public synchronized void cacheRecord(DcsDataRecord dcsDataRecord) {
		String id = dcsDataRecord.getId();
		if (id != null && id.length() > 0) {
			cache.put(id, dcsDataRecord);
		}
		else {
			prtln("unable to cache dcsDataRecord");
		}
	}


	/**  Write all cached DcsDataRecords to disk and then clear the cache  */
	public synchronized void flushCache() {
		prtln("flushing " + cache.size() + " DcsDataRecords to disk");
		for (Iterator i = cache.keySet().iterator(); i.hasNext(); ) {
			String key = (String) i.next();
			if (cachingEnabled) {
				DcsDataRecord rec = (DcsDataRecord) cache.get(key);
				try {
					rec.flushToDisk();
				} catch (OutOfMemoryError me) {
					prtln(me.getMessage());
					prtln("Out of memory - aborting cache flush");
					return;
				} catch (Exception e) {
					prtln(" ... flushToDisk failed for " + rec.getId());
				}
			}
		}
		cache.clear();
	}


	/**
	 *  This method is called at the conclusion of processing and may be used for
	 *  tear-down.<p>
	 *
	 *  We don't need to flush on destroy if we are writing dcsDataRecords to disk
	 *  after each change.
	 */
	public void destroy() {
		prtln("destroy()");
		boolean flushOnDestroy = false;

		if (flushOnDestroy)
			flushCache();
		else
			prtln("  ... clearing " + cache.size() + " DcsDataRecords from cache");
	}


	/**
	 *  Print a line to standard out.
	 *
	 * @param  s  The String to print.
	 */
	static void prtln(String s) {
		if (debug) {
			System.out.println("DcsDataManager: " + s);
		}
	}


	/**
	 *  Description of the Method
	 *
	 * @param  dcsDataRecord  NOT YET DOCUMENTED
	 */
	void notifyListeners(DcsDataRecord dcsDataRecord) {
		// make LuceneIndexChangedEvent with a new reader file using long data
		StatusEvent event = new StatusEvent(dcsDataRecord);

		for (int i = 0; i < listeners.size(); i++) {
			try {
				((WorkFlowServices) listeners.get(i)).statusChanged(event);
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
	public void addListener(WorkFlowServices listener) {
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
	public void removeListener(WorkFlowServices listener) {
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
}

