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

import org.dlese.dpc.schemedit.config.CollectionConfig;
import org.dlese.dpc.schemedit.config.CollectionRegistry;
import org.dlese.dpc.schemedit.SchemEditUtils;
import org.dlese.dpc.schemedit.RecordList;
import org.dlese.dpc.schemedit.repository.RepositoryService;
import org.dlese.dpc.schemedit.dcs.DcsDataManager;

import java.io.*;
import java.util.*;
import org.dlese.dpc.repository.*;
import org.dlese.dpc.index.*;
import org.dlese.dpc.index.reader.*;
import org.dlese.dpc.util.Files;

import javax.servlet.ServletContext;

/**
 *  Deletes a collections by moving metadata records to a "baseTrashDir"
 *  directory.<p>
 *
 *  The Collection record, item records, and status records are all removed from
 *  their respective places in the repository file structure and moved to
 *  corresponding positions within the baseTrashDir directory.
 *
 * @author     ostwald<p>
 *
 *      $Id $
 * @version    $Id: CollectionReaper.java,v 1.4 2009/03/20 23:33:57 jweather Exp $
 */
public class CollectionReaper {

	private static boolean debug = true;
	private boolean initialized = false;

	private File baseTrashDir = null;
	private File repository = null;
	private File masterCollectionRecords = null;
	private String collection = null;
	private String parent = null;
	private String xmlFormat = null;
	private String id = null;
	private CollectionRegistry collectionRegistry = null;
	private RepositoryService repositoryService = null;
	private RepositoryManager rm = null;
	private DcsDataManager dcsDataManager = null;


	/**
	 *  Constructor for the debugging the CollectionReaper
	 *
	 * @param  metadataRecordLocation     Description of the Parameter
	 * @param  collection                 Description of the Parameter
	 * @param  xmlFormat                  Description of the Parameter
	 * @param  id                         Description of the Parameter
	 */
	public CollectionReaper(String metadataRecordsLocation,
	                        String collection,
	                        String xmlFormat,
	                        String id) {
		repository = new File(metadataRecordsLocation);
		masterCollectionRecords = new File(metadataRecordsLocation, "dlese_collect/collect");
		baseTrashDir = new File(repository, "trash");
		this.collection = collection;
		this.xmlFormat = xmlFormat;
		this.id = id;
	}


	/**
	 *  constructor for use from Schemedit app
	 *
	 * @param  collection      Description of the Parameter
	 * @param  servletContext  Description of the Parameter
	 */
	public CollectionReaper(String collection, ServletContext servletContext) {

		rm = (RepositoryManager) servletContext.getAttribute("repositoryManager");
		repositoryService = (RepositoryService) servletContext.getAttribute("repositoryService");
		// idManager = (IDManager) servletContext.getAttribute("idManager");
		collectionRegistry = (CollectionRegistry) servletContext.getAttribute("collectionRegistry");
		dcsDataManager = (DcsDataManager) servletContext.getAttribute("dcsDataManager");

		if (dcsDataManager != null) {
			dcsDataManager.flushCache();
		}

		repository = new File(rm.getMetadataRecordsLocation());
		masterCollectionRecords = new File(rm.getCollectionRecordsLocation());
		baseTrashDir = new File(repository, "trash");
		this.collection = collection;
		SetInfo setInfo = SchemEditUtils.getSetInfo(collection, rm);
		this.xmlFormat = setInfo.getFormat();
		this.id = setInfo.getId();
		try {
			parent = RepositoryService.getXMLDocReader(id, rm).getCollection();
			prtln("parent for " + id + " is " + parent);
		} catch (Exception e) {
			prtln("error getting parent: " + e.getMessage());
			parent = "collect";
		}

		try {
			this.init();
		} catch (Exception e) {
			prtln("CollectionReaper init failed: " + e.getMessage());
			return;
		}
		initialized = true;
	}


	/**
	 *  Make sure the repository, baseTrashDir and collections directories exist.
	 *
	 * @exception  Exception  Description of the Exception
	 */
	public void init()
		 throws Exception {

		if (!repository.exists()) {
			throw new Exception("recordsDir does not exist at " + repository.getAbsolutePath());
		}

		if (baseTrashDir == null) {
			throw new Exception("baseTrashDir directory not initialized");
		}

		if (!baseTrashDir.exists()) {
			if (!baseTrashDir.mkdir()) {
				throw new Exception("baseTrashDir directory does not exist and could not be created at " + baseTrashDir.getAbsolutePath());
			}
		}

		if (!masterCollectionRecords.exists()) {
			throw new Exception("collection Records directory does not exist at " + masterCollectionRecords.getAbsolutePath());
		}

		prtln("CollectionReaper initialized:");
		prtln("  .. collection: " + collection);
		prtln("  .. parent: " + parent);
		prtln("  .. collection record id: " + id);
		prtln("  .. repository path: " + repository.getAbsolutePath());
		prtln("  .. baseTrashDir path: " + baseTrashDir.getAbsolutePath());
		prtln("  .. masterCollections path: " + masterCollectionRecords.getAbsolutePath());

		initialized = true;
	}


	/**
	 *  move all files from item record Directory to the itemTrash directory for
	 *  given format and collection (baseTrashDir/xmlFormat/collection).
	 *
	 * @exception  Exception  Description of the Exception
	 */
	private void trashItems()
		 throws Exception {
		prtln("trashItems()");
		String path = xmlFormat + Files.getFileSeparatorStr() + collection;

		File sourceDir = new File(repository, path);
		if (!sourceDir.exists()) {
			// throw new Exception ("sourceDir does not exist at " + sourceDir.getAbsolutePath());
			prtln("WARNING: item level record directory does not exist at " + sourceDir.getAbsolutePath());
			return;
		}

		File destDir = getUniqueDir(baseTrashDir, path);
		prtln("     moving files\n\tfrom: " + sourceDir + "\n\tto: " + destDir);
		moveFiles(sourceDir, destDir);

		if (!sourceDir.delete()) {
			throw new Exception("source directory could not be deleted at " + sourceDir.getAbsolutePath());
		}

		prtln(" ... item records trashed to " + destDir);
	}


	/**
	 *  Description of the Method
	 *
	 * @exception  Exception  Description of the Exception
	 */
	public void reap()
		 throws Exception {
		if (!initialized) {
			throw new Exception("CollectionReaper not initialized");
		}

		prtln("reap with collection = " + collection + " xmlFormat (item format) = " + xmlFormat);

		if (collection.equals("collect"))
			throw new Exception("CollectionReaper cannot reap the master collection!");

		try {
			// for collections containing collection records, unregister them and remove vocab entries
			// for item records, which have no sub-items
			if (xmlFormat.equals("dlese_collect")) {
				// we only reap subcollections if this is a "master collection"
				if (parent.equals("collect")) {
					// take care of subCollections
					prtln("reaping subcollections of " + collection);
					unhookSubCollections();
				}
				else {
					// we don't reap items or unhook from slave collections, since these have no
					// items and are not registered as collections, per se.
					prtln("not reaping items or unhooking subs - parent is: " + parent);
				}
			}

			prtln("trashing item and status files");
			trashItems(); // these deletes the item-level files
			trashStatusFiles();
			trashCollectionStatusFile();
			trashCollectionFile();
			unhookCollection(collection);

		} catch (Exception e) {
			throw new Exception("Reap error: " + e.getMessage());
		}
	}


	/**
	 *  Remove all data structures referring to a collection, including:
	 *  <ul>
	 *    <li> vocabulary entries (collectionKey)
	 *    <li> prefix
	 *    <li> idGenerator
	 *    <li> collectionConfiguration
	 *  </ul>
	 *
	 *
	 * @param  key            NOT YET DOCUMENTED
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public void unhookCollection(String key) throws Exception {
		prtln("unhooking: " + key);
		collectionRegistry.deleteCollection(key);
	}

	
	/**
	 *  Unhook the subcollections of a collection containing collection records. It
	 *  is assumed that the collection records are not associated with item
	 *  records.
	 *
	 * @exception  Exception  Description of the Exception
	 */
	public void unhookSubCollections()  throws Exception {

		if (repositoryService == null)
			throw new Exception ("repositoryService is not initialized"); 
		RecordList results = repositoryService.getCollectionItemRecords(collection);

		if (!results.isEmpty()) {
			prtln("unhookSubCollections: " + results.size() + " found");
			for (Iterator i = results.iterator(); i.hasNext(); ) {
				try {
					ResultDoc result = results.getResultDoc((String) i.next(), rm.getIndex());
					DleseCollectionDocReader docReader = (DleseCollectionDocReader) result.getDocReader();
					String key = docReader.getKey();
					prtln("  ..key: " + key);
					unhookCollection(key);

				} catch (Exception e) {
					prtln("unhookSubCollections error: " + e.getMessage());
					e.printStackTrace();
				}
			}
		}
		else {
			prtln("unhookSubCollections - none found");
		}
	}


	/**
	 *  Description of the Method
	 *
	 * @param  sourceDir      Description of the Parameter
	 * @param  destDir        Description of the Parameter
	 * @exception  Exception  Description of the Exception
	 */
	private void moveFiles(File sourceDir, File destDir)
		 throws Exception {
		File[] records = sourceDir.listFiles();
		for (int i = 0; i < records.length; i++) {
			File sourceFile = records[i];
			String fileName = sourceFile.getName();
			File destFile = new File(destDir, fileName);
			sourceFile.renameTo(destFile);
		}
	}


	/**
	 *  Get a unique Directory for the given baseDir and path. For Example, if the
	 *  directory at (baseDir + path) does not exist, return it. Otherwise,
	 *  repeatedly try (baseDir + path + "_i") until the resulting directory does
	 *  not exist.
	 *
	 * @param  baseDir        NOT YET DOCUMENTED
	 * @param  path           NOT YET DOCUMENTED
	 * @return                The uniqueDir value
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	File getUniqueDir(File baseDir, String path) throws Exception {
		File uniqueDir = new File(baseDir, path);
		if (uniqueDir.exists()) {
			for (int i = 1; i < 100; i++) {
				uniqueDir = new File(baseDir, path + "_" + Integer.toString(i));
				if (!uniqueDir.exists())
					break;
			}
		}
		if (uniqueDir.exists() || !uniqueDir.mkdirs()) {
			throw new Exception("could not create unique directory at " + uniqueDir.getAbsolutePath());
		}
		return uniqueDir;
	}


	/**
	 *  move all dcsDataRecord files from source Directory to trash directory
	 *  (baseTrashDir/dcs_data/xmlFormat/collection)
	 *
	 * @exception  Exception  Description of the Exception
	 */
	private void trashStatusFiles()
		 throws Exception {
		// prtln ("trashStatusFiles()");
		File dcsTrash = new File(baseTrashDir, "dcs_data");
		File dcsSource = new File(repository, "dcs_data");
		String path = xmlFormat + Files.getFileSeparatorStr() + collection;
		File sourceDir = new File(dcsSource, path);
		if (!sourceDir.exists()) {
			prtln("dcs_data sourceDir does not exist at " + sourceDir.getAbsolutePath());
			return;
		}

		File destDir = getUniqueDir(dcsTrash, path);

		prtln("     moving files\n\tfrom: " + sourceDir + "\n\tto: " + destDir);
		moveFiles(sourceDir, destDir);

		if (!sourceDir.delete()) {
			throw new Exception("WARNING: status file directory could not be deleted at " + sourceDir.getAbsolutePath());
		}

		prtln(" ... status files trashed");
	}


	/**
	 *  Move the file containing the collection record for the reaped collection to
	 *  the trash directory.
	 *
	 * @exception  Exception  Description of the Exception
	 */
	private void trashCollectionFile()
		 throws Exception {
		prtln("\ntrashCollectionFile()");
		String collectionFileName = id + ".xml";
		String sep = Files.getFileSeparatorStr();
		String path = "dlese_collect" + sep + parent;

		// File collectionRecord = new File(collectionRecords, parent + sep + collectionFileName);
		File collectionRecord = new File(this.masterCollectionRecords, collectionFileName);
		if (!collectionRecord.exists()) {
			// throw new Exception ("collection record does not exist at " + collectionRecord.getAbsolutePath());
			prtln("WARNING: collection record to be trashed does not exist at " + collectionRecord.getAbsolutePath());
			return;
		}

		File destDir = new File(baseTrashDir, path);
		prtln(" ... destDir for this collection is: " + destDir.getAbsolutePath());
		if (!destDir.exists()) {
			if (!destDir.mkdirs()) {
				throw new Exception("destDir directory could not be created at " + destDir.getAbsolutePath());
			}
		}

		File dest = new File(destDir, collectionFileName);
		prtln("\n\tdestFile is: " + dest.getAbsolutePath());
		// unlike unix, windows apparently refuses to rename over existing file
		if (dest.exists() && !dest.delete()) {
			prtln("WARNING: exting trash file at " + dest + " could not be deleted");
		}
		if (!collectionRecord.renameTo(dest)) {
			prtln("WARNING: collection record at " + collectionRecord.getAbsolutePath() + " could not be renamed as " + dest);
		}

	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	private void trashCollectionStatusFile()
		 throws Exception {
		prtln("\ntrashCollectionStatusFile()");
		String sep = Files.getFileSeparatorStr();

		File dcsTrashDir = new File(baseTrashDir, "dcs_data");
		File dcsDataSource = new File(repository, "dcs_data");
		String collectionFileName = id + ".xml";
		String path = "dlese_collect" + sep + parent + sep + collectionFileName;

		File collectionStatusRecord = new File(dcsDataSource, path);
		if (!collectionStatusRecord.exists()) {
			// throw new Exception ("collection record does not exist at " + collectionStatusRecord.getAbsolutePath());
			prtln("WARNING: collection STATUS record to be trashed does not exist at " + collectionStatusRecord.getAbsolutePath());
			return;
		}

		File dest = new File(dcsTrashDir, path);
		File destDir = dest.getParentFile();
		prtln(" ... dest for this collection Status record is: " + destDir.getAbsolutePath());
		if (!destDir.exists()) {
			if (!destDir.mkdirs()) {
				throw new Exception("destDir directory could not be created at " + destDir.getAbsolutePath());
			}
		}

		// unlike unix, windows apparently refuses to rename over existing file
		if (dest.exists() && !dest.delete()) {
			prtln("WARNING: exting trash file at " + dest + " could not be deleted");
		}
		if (!collectionStatusRecord.renameTo(dest)) {
			prtln("collection status record at " + collectionStatusRecord.getAbsolutePath() + " could not be renamed");
		}
		prtln(" ... collection STATUS File trashed");
	}



	/**
	 *  The main program for the CollectionReaper class
	 *
	 * @param  args  The command line arguments
	 */
	public static void main(String[] args) {
		prtln("hello from the CollectionReaper");
		String recordsDir = "/devel/ostwald/tmp/sample_records";
		String collectionDir = "/devel/ostwald/tmp/sample_records/dlese_collect/collect";
		String collection = "sercnagt";
		String xmlFormat = "adn";
		String id = "DLESE-COLLECTION-000-000-000-010";

		CollectionReaper r = new CollectionReaper(recordsDir, collection, xmlFormat, id);
		try {
			r.init();
			r.reap();
		} catch (Exception e) {
			prtln(e.getMessage());
		}
	}


	/**
	 *  Description of the Method
	 *
	 * @param  s  Description of the Parameter
	 */
	private static void prtln(String s) {
		if (debug) {
			// System.out.println(s);
			SchemEditUtils.prtln(s, "Reaper");
		}
	}
}

