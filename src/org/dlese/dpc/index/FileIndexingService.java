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
package org.dlese.dpc.index;
import java.io.*;
import java.util.*;
import org.dlese.dpc.index.*;
import org.dlese.dpc.xml.*;
import org.dlese.dpc.datamgr.*;
import org.dlese.dpc.index.writer.*;
import org.dlese.dpc.index.reader.*;
import org.dlese.dpc.util.*;
import org.apache.lucene.document.*;
import java.text.*;
import javax.servlet.*;
import org.dlese.dpc.webapps.tools.*;
import org.dlese.dpc.repository.*;
import org.dlese.dpc.services.mmd.*;
import org.dlese.dpc.index.document.DateFieldTools;
/*
 *  To Do: Add functionality to update disable the discovery of a collection in the
 *  query parser. Create a class that returns the discoverable (or NOT discoverable) collections
 *  and filter them at query time.
 */
/*
 *  To Do: Add functionality to update individual record's status with regards to discoverablilty.
 */
/*
 *  To Do: Add a method updateRecords(String field, List recordsToUpdate) that updates each
 *  record found in recordsToUpdate that match when searched in the given field. So, for example
 *  a list of IDs could be passed in that will be updated.
 */
/**
 *  Indexes files into a {@link org.dlese.dpc.index.SimpleLuceneIndex} and automatically updates the index
 *  whenever changes to the files are made. This class uses a {@link
 *  org.dlese.dpc.index.writer.FileIndexingServiceWriter} to create the Lucene {@link
 *  org.apache.lucene.document.Document}s that are placed in the {@link
 *  org.dlese.dpc.index.SimpleLuceneIndex}. This class looks for changes made to items in a directory of files
 *  and updates the index automatically by adding, updating or deleting items as appropriate. The frequency
 *  for update checkes is configurable. There should be only one instance of this class for each {@link
 *  org.dlese.dpc.index.SimpleLuceneIndex} that is being populated with this class.
 *
 * @author    John Weatherley
 */
public final class FileIndexingService {
	/**  Indicates that indexing completed normally */
	public final static int INDEXING_SUCCESS = 1;
	/**  Indicates that indexing was aborted by request */
	public final static int INDEXING_ABORTED = 2;
	/**  Indicates that indexing completed with a severe error */
	public final static int INDEXING_ERROR = 3;
	/**  Indicates that indexing completed successfully, but one or more item was indexed with errors */
	public final static int INDEXING_ITEM_ERROR = 4;
	/**  Indicates that indexing directory does not exist */
	public final static int INDEXING_DIR_DOES_NOT_EXIST = 5;
	/**  Indicates a read error on the directory */
	public final static int INDEXING_DIR_READ_ERROR = 6;
	private final static int NUM_INDEXING_MESSAGES = 25;
	// Frequency of file-to-index sychronization, in seconds
	private long updateFrequency = 0;
	private long lastSyncTime = 0;
	private Timer timer;
	private long startTime, endTime, startTime2, endTime2;
	private SimpleLuceneIndex index;
	private HashMap sourceDirs = new HashMap();
	private HashMap sourceDirsHistory = new HashMap();
	private SimpleDataStore dataStore = null;
	//private Hashtable unindexableFiles = null;
	private HashMap attributes = new HashMap();
	// Used to track how many threads are waiting to perform some sort of indexing
	private int numIndexingQue = 0;
	private int num_remove = 0;
	private int num_add = 0;
	private int num_replace = 0;

	// Objects used to synchronize the indexing process...
	private final Object _stopSync = new Object();
	private final Object _indexingSync = new Object();

	private boolean indexErrors = true;
	private boolean validateFiles = true;
	private boolean saveDeletes = false;
	private static boolean debug = true;
	private String idFieldToRemove = null;
	private boolean isIndexing = false;
	private boolean stopIndexing = false;
	// Max num files to index at one time
	int maxNumFilesToIndex = 500;
	// For testing only...
	FileMoveTester tester = null;


	/**
	 *  Indexes files to the given {@link org.dlese.dpc.index.SimpleLuceneIndex}, checking for changes in the
	 *  files and reindexing them at the given update frequency. Validation of files is enabled by default.
	 *
	 * @param  index                       The {@link org.dlese.dpc.index.SimpleLuceneIndex} that will be
	 *      populated and updated with Documents created from files
	 * @param  updateFrequency             The frequency by which files are checked for updates, in seconds. Zero
	 *      or less indicates no updates should be performed.
	 * @param  fileIndexingServiceDataDir  The directory where serialized data will be stored
	 * @param  maxNumFilesToIndex          Max number of files to index per iteration
	 * @see                                #setValidationEnabled(boolean validateFiles)
	 */
	public FileIndexingService(SimpleLuceneIndex index,
	                           long updateFrequency,
	                           String fileIndexingServiceDataDir,
	                           int maxNumFilesToIndex) {
		this(index, updateFrequency, false, null, fileIndexingServiceDataDir, maxNumFilesToIndex);
	}


	/**
	 *  Indexes files to the given {@link org.dlese.dpc.index.SimpleLuceneIndex}, checking for changes in the
	 *  files and reindexing them at the given update frequency. Validation of files is enabled by default.
	 *
	 * @param  index                       The {@link org.dlese.dpc.index.SimpleLuceneIndex} that will be
	 *      populated and updated with Documents created from files
	 * @param  updateFrequency             The frequency by which files are checked for updates, in seconds. Zero
	 *      or less indicates no updates should be performed.
	 * @param  saveDeletes                 True to save removed documents in the index and mark them deleted,
	 *      else they will be removed from the index.
	 * @param  idFieldToRemove             An ID field whoes docs should be removed if found in duplicate.
	 * @param  fileIndexingServiceDataDir  Dir where persistent data files will be stored
	 * @param  maxNumFilesToIndex          The number of files to index per iteration
	 * @see                                #setValidationEnabled(boolean validateFiles)
	 */
	public FileIndexingService(
	                           SimpleLuceneIndex index,
	                           long updateFrequency,
	                           boolean saveDeletes,
	                           String idFieldToRemove,
	                           String fileIndexingServiceDataDir,
	                           int maxNumFilesToIndex) {
		this.index = index;
		this.saveDeletes = saveDeletes;
		this.updateFrequency = updateFrequency;
		this.idFieldToRemove = idFieldToRemove;
		this.maxNumFilesToIndex = maxNumFilesToIndex;
		if (index == null) {
			return;
		}
		try {
			File f = new File(fileIndexingServiceDataDir);
			f.mkdir();
			dataStore = new SimpleDataStore(f.getAbsolutePath(), true);
		} catch (Exception e) {
			prtlnErr("Error initializing SimpleDataStore: " + e);
		}
		startTimerThread(updateFrequency);
	}


	/**
	 *  Sets an attribute Object that will be available for access here and from the FileIndexingServiceWriters.
	 *
	 * @param  key        The key used to reference the attribute.
	 * @param  attribute  Any Java Object.
	 * @see               org.dlese.dpc.index.writer.FileIndexingServiceWriter
	 */
	public void setAttribute(String key, Object attribute) {
		attributes.put(key, attribute);
	}


	/**
	 *  Stops the indexing process if it is currently taking place. This method may take a few seconds to
	 *  complete.
	 */
	public void stopIndexing() {
		stopIndexing = true; // Signal threads to stop indexing
		index.stopIndexing(); // Stop the indexer
		while (numIndexingQue > 0) {
			Thread.yield();
			try {
				Thread.sleep(10);
			} catch (Throwable e) {}
		}
		stopIndexing = false;
		//prtln("Indexing has stopped...");
	}


	/**
	 *  Determines whether the indexing process is waiting to stop. This should be called by indexing threads in
	 *  this class prior to indexing a block of files. If true, they should stop their indexing and decrement
	 *  numIndexingQue;
	 *
	 * @return    The indexingWaitingToStop value
	 */
	private boolean isIndexingWaitingToStop() {
		synchronized (_stopSync) {
			return (stopIndexing && numIndexingQue != 0);
		}
	}


	/**
	 *  Gets an attribute Object from this FileIndexingService.
	 *
	 * @param  key  The key used to reference the attribute.
	 * @return      The Java Object that is stored under the given key or null if none exists.
	 * @see         org.dlese.dpc.index.writer.FileIndexingServiceWriter
	 */
	public Object getAttribute(String key) {
		return attributes.get(key);
	}


	/**
	 *  Changes the frequency of reindexing to the new value. Same as {@link #startTimerThread(long
	 *  updateFrequency)}.
	 *
	 * @param  updateFrequency  The frequency by which files are checked for changes and reindexed.
	 */
	public void changeUpdateFrequency(long updateFrequency) {
		startTimerThread(updateFrequency);
	}


	/**
	 *  Start or restarts the timer thread with the given update frequency. Same as {@link
	 *  #changeUpdateFrequency(long updateFrequency)}.
	 *
	 * @param  updateFrequency  The number of seconds between index updates.
	 */
	public void startTimerThread(long updateFrequency) {
		if (updateFrequency == 0) {
			stopTimerThread();
		}
		if (updateFrequency > 0) {
			if (timer != null) {
				timer.cancel();
			}
			// Set daemon to true
			timer = new Timer(true);
			// Start monitoring the metadata sourceFile directory for updates and changes.
			timer.schedule(new FileSyncTask(),
				6000,
				((updateFrequency > 0) ? updateFrequency * 1000 : 60000));
			addIndexingMessage("Auto-indexing timer started");
			//prtln("FileIndexingService timer started");
		}
	}


	/**  Stops the indexing timer thread. */
	public void stopTimerThread() {
		if (timer != null) {
			timer.cancel();
			addIndexingMessage("Auto-indexing timer stopped");
			//prtln("FileIndexingService timer stopped");
		}
	}


	/**
	 *  Sets whether or not to validate the files being indexed and create a validation report, which is indexed.
	 *  If set to true, the files will be validated, otherwise they will not. Default is true.
	 *
	 * @param  validateFiles  True to validate, else false.
	 * @see                   org.dlese.dpc.index.writer.FileIndexingServiceWriter#getValidationReport()
	 */
	public void setValidationEnabled(boolean validateFiles) {
		this.validateFiles = validateFiles;
	}


	/**
	 *  Adds a directory of files to be monitored for changes, or replaces the current one if one exists with the
	 *  same absolute path.
	 *
	 * @param  sourceFileDirectory             The file direcory that will be monitored for updates.
	 * @param  indexingPriority                The feature to be added to the Directory attribute
	 * @param  documentWriterClass             The feature to be added to the Directory attribute
	 * @param  documentWriterConfigAttributes  The feature to be added to the Directory attribute
	 * @param  plugin                          The feature to be added to the Directory attribute
	 * @return                                 True if the directory was added successfully.
	 */
	public boolean addDirectory(
	                            String sourceFileDirectory,
	                            Class documentWriterClass,
	                            HashMap documentWriterConfigAttributes,
	                            FileIndexingPlugin plugin,
	                            int indexingPriority) {
		if (sourceFileDirectory == null) {
			return false;
		}
		File srcDir = null;
		try {
			srcDir = new File(sourceFileDirectory);
		} catch (Throwable e) {
			prtlnErr("Unable to add directory: " + e);
			return false;
		}
		// Make sure index is in the config since it's used by XMLFileIndexingWriter and others
		if (documentWriterConfigAttributes == null)
			documentWriterConfigAttributes = new HashMap(1);
		if (index != null && documentWriterConfigAttributes.get("index") == null)
			documentWriterConfigAttributes.put("index", index);
		return addDirectory(srcDir, documentWriterClass, documentWriterConfigAttributes, plugin, indexingPriority);
	}


	/**
	 *  Adds a directory of files to be monitored for changes, or replaces the current one if one exists with the
	 *  same absolute path.
	 *
	 * @param  srcDir                          The file direcory that will be monitored for updates.
	 * @param  indexingPriority                The feature to be added to the Directory attribute
	 * @param  documentWriterClass             The feature to be added to the Directory attribute
	 * @param  documentWriterConfigAttributes  The feature to be added to the Directory attribute
	 * @param  plugin                          The feature to be added to the Directory attribute
	 * @return                                 True if the directory was added successfully.
	 */
	public boolean addDirectory(
	                            File srcDir,
	                            Class documentWriterClass,
	                            HashMap documentWriterConfigAttributes,
	                            FileIndexingPlugin plugin,
	                            int indexingPriority) {
		//prtln("addDirectory(): " + srcDir);
		if (srcDir == null) {
			return false;
		}
		// Make sure index is in the config since it's used by XMLFileIndexingWriter and others
		if (documentWriterConfigAttributes == null)
			documentWriterConfigAttributes = new HashMap(1);
		if (index != null && documentWriterConfigAttributes.get("index") == null)
			documentWriterConfigAttributes.put("index", index);
		//prtln("addDirectory(): " + srcDir.getAbsolutePath());
		// If this dir is not already being monotored, clear it from the index (to
		//if(!sourceDirs.containsKey(srcDir.getAbsolutePath()))
		//	index.removeDocs("docdir",srcDir.getAbsolutePath());
		// To do: if a dir was deleted, then added back, need to notify index to re-index those files...
		synchronized (sourceDirs) {
			sourceDirs.put(srcDir.getAbsolutePath(), new FISDirInfo(srcDir, documentWriterClass, documentWriterConfigAttributes, plugin, this, indexingPriority));
		}
		synchronized (sourceDirsHistory) {
			sourceDirsHistory.put(srcDir.getAbsolutePath(), new FISDirInfo(srcDir, documentWriterClass, documentWriterConfigAttributes, plugin, this, indexingPriority));
		}
		return true;
	}


	/**
	 *  Determins whether indexing is in progress.
	 *
	 * @return    True if indexing is in progress, false if not
	 */
	public boolean isIndexing() {
		return (numIndexingQue > 0);
	}


	/**
	 *  Determines whether the given directory is configured for indexing.
	 *
	 * @param  srcDir  A directory of indexable files.
	 * @return         True if this directory is already configured for indexing, false otherwise.
	 */
	public boolean isDirectoryConfigured(File srcDir) {
		if (srcDir == null) {
			return false;
		}
		synchronized (sourceDirs) {
			return sourceDirs.containsKey(srcDir.getAbsolutePath());
		}
	}


	/**
	 *  Gets a HashMap of all directories that are configured in this FileIndexingService, keyed by absolute
	 *  path.
	 *
	 * @return    The configuredDirectories value
	 */
	public HashMap getConfiguredDirectories() {
		synchronized (sourceDirs) {
			return (HashMap) sourceDirs.clone();
		}
	}


	/**
	 *  Gets a new fileIndexingServiceWriter that is configured for the given source directory, or null if none
	 *  exists.
	 *
	 * @param  srcDir  Absolute path to the src directory for the file
	 * @return         The fileIndexingServiceWriter or null
	 */
	private FileIndexingServiceWriter getNewFileIndexingServiceWriter(String srcDir) {
		FISDirInfo dirInfo = null;
		synchronized (sourceDirs) {
			if (sourceDirs == null)
				return null;
			dirInfo = (FISDirInfo) sourceDirs.get(srcDir);
		}
		if (dirInfo == null) {
			return null;
		}
		else {
			return dirInfo.getDocWriter();
		}
	}


	/**
	 *  Deletes the files in the given directory from the index and removes it from the configuration. Assumes
	 *  the directory was previously added to the index using the {@link #addDirectory} method.
	 *
	 * @param  sourceFileDirectory  The directory of files needing to be removed from the index.
	 * @return                      True if the directory of files exsited in the index and was removed.
	 */
	public boolean deleteDirectory(String sourceFileDirectory) {
		if (sourceFileDirectory == null) {
			return false;
		}
		File srcDir = null;
		try {
			srcDir = new File(sourceFileDirectory);
		} catch (Throwable e) {
			prtlnErr("Unable to remove directory: " + e);
			return false;
		}
		return deleteDirectory(srcDir);
	}


	/**
	 *  Deletes the files in the given directory from the index and removes it from the configuration. Assumes
	 *  the directory was previously added to the index using the {@link #addDirectory} method.
	 *
	 * @param  srcDir  The directory of files needing to be removed from the index.
	 * @return         True if the directory of files exsited in the index and was removed.
	 */
	public boolean deleteDirectory(File srcDir) {
		//prtln("deleteDirectory()");
		Object result = null;
		if (srcDir == null) {
			return false;
		}
		synchronized (sourceDirs) {
			result = sourceDirs.remove(srcDir.getAbsolutePath());
		}
		if (result == null) {
			return false;
		}
		else {
			// Remove the records from the index itself.
			removeDeletedDirs();
			return true;
		}
	}


	/**
	 *  Gets the updateFrequency attribute of the FileIndexingService object
	 *
	 * @return    The updateFrequency value
	 */
	public long getUpdateFrequency() {
		return updateFrequency;
	}


	/**
	 *  Gets the lastSyncTime attribute of the FileIndexingService object
	 *
	 * @return    The lastSyncTime value
	 */
	public long getLastSyncTime() {
		return lastSyncTime;
	}


	/**
	 *  Gets the numRecordsToDelete attribute of the FileIndexingService object
	 *
	 * @return    The numRecordsToDelete value
	 */
	public int getNumRecordsToDelete() {
		return num_remove;
	}


	/**
	 *  Gets the numRecordsToAdd attribute of the FileIndexingService object
	 *
	 * @return    The numRecordsToAdd value
	 */
	public int getNumRecordsToAdd() {
		return num_add;
	}


	/**
	 *  Gets the numRecordsToReplace attribute of the FileIndexingService object
	 *
	 * @return    The numRecordsToReplace value
	 */
	public int getNumRecordsToReplace() {
		return num_replace;
	}


	/**
	 *  Updates the index to reflect the files in the directories this service is monitoring, with the option to
	 *  run the update in the background. Any new, deleted or modified files that appear in the directories will
	 *  be reflected in the index.
	 *
	 * @param  reindexAll  True to reindex all files regardless of file mod date, False to reindex only those
	 *      files that have changed since the last indexing.
	 * @param  observer    The FileIndexingObserver that will be notified when indexing is complete, or null to
	 *      use none
	 */
	public void indexFiles(boolean reindexAll, FileIndexingObserver observer) {
		//prtln("indexFiles(2)");
		new FileSyncThread(reindexAll, observer).start();
	}


	/**
	 *  Updates the index to reflect the files in the directory indicated, which must have been previously added
	 *  to this FileIndexingService using {@link #addDirectory}. Any new, deleted or modified files that appear
	 *  in the directory will be reflected in the index.
	 *
	 * @param  reindexAll  True to reindex all files regardless of file mod date, False to reindex only those
	 *      files that have changed since the last indexing.
	 * @param  directory   The directory to index.
	 * @param  observer    The FileIndexingObserver that will be notified when indexing is complete, or null to
	 *      use none
	 */
	public void indexFiles(boolean reindexAll, File directory, FileIndexingObserver observer) {
		//prtln("indexFiles(3) directory: " + directory);
		new FileSyncThread(reindexAll, directory, observer).start();
	}


	/**
	 *  Indexes a single file. The operaion is executed serially to completion.
	 *
	 * @param  fileToIndex                       The file to index.
	 * @param  plugin                            A FileIndexingPlugin or null.
	 * @exception  FileIndexingServiceException  If unable to index
	 */
	public void indexFile(File fileToIndex, FileIndexingPlugin plugin)
		 throws FileIndexingServiceException {
		//prtln("indexFile...");
		synchronized (_indexingSync) {
			numIndexingQue++;
			Throwable thrown = null;
			RecordDataService recordDataService = null;
			try {
				if (isIndexingWaitingToStop())
					throw new FileIndexingServiceException("Indexing stop signal has been requested. File index file: " + fileToIndex.getAbsolutePath() + " will not be indexed.");
				if (fileToIndex == null)
					throw new FileIndexingServiceException("File is null");
				if (!fileToIndex.canRead())
					throw new FileIndexingServiceException("Unable to read file");
				if (fileToIndex.isDirectory())
					throw new FileIndexingServiceException("File '" + fileToIndex.getAbsolutePath() + "' is a directory, not a file");
				FileIndexingServiceWriter myDocWriter = null;
				myDocWriter = getNewFileIndexingServiceWriter(fileToIndex.getParent());
				if (myDocWriter == null)
					throw new FileIndexingServiceException("The directory '" + fileToIndex.getParentFile() +
						"' is not configured for indexing by the FileIndexingService.");
				myDocWriter.setValidationEnabled(false);
				recordDataService = (RecordDataService) index.getAttribute("recordDataService");
				// Initialize the idMapper:
				if (recordDataService != null)
					recordDataService.initIdMapper();
				String ab = fileToIndex.getAbsolutePath();
				Document existingLuceneDoc = null;
				List existingDocs = index.listDocs("docsource", ab);
				if (existingDocs != null && existingDocs.size() > 0)
					existingLuceneDoc = (Document) existingDocs.get(0);
				FileIndexingServiceData newDocData = myDocWriter.create(fileToIndex, existingLuceneDoc, plugin, null);
				if (newDocData != null && newDocData.getDoc() != null) {
					// Remove any documents as requested by the DocWriter:
					HashMap docsToRemove = new HashMap();
					addRemoveFields(newDocData, docsToRemove);
					removeDocsFromIndex(index, docsToRemove);
					// Add the new document:
					index.update("docsource", ab, newDocData.getDoc(), SimpleLuceneIndex.BLOCK);
				}
				else
					throw new FileIndexingServiceException("Unable to create index entry for file");
			} catch (Throwable e) {
				thrown = e;
			} finally {
				if (recordDataService != null)
					recordDataService.closeIdMapper();
				numIndexingQue--;
			}
			if (thrown != null)
				throw new FileIndexingServiceException(thrown.toString());
		}
	}


	/**
	 *  Updates the index for just the single directory of files indicated. Any new, deleted or modified files
	 *  that appear in the directory will be reflected in the index. This process may take several minutes to
	 *  return depending on the number of files needing to be updated. If this FileIndexingService has not been
	 *  configured for this directory, no indexing takes place.
	 *
	 * @param  reindexAll  True to reindex all records regardless of file mod date.
	 * @param  directory   The directory to index
	 * @param  observer    The FileIndexingObserver that will be notified when indexing is complete, or null to
	 *      use none
	 */
	private void synchIndexWithFiles(boolean reindexAll, File directory, FileIndexingObserver observer) {
		synchronized (_indexingSync) {
			//prtln("synchIndexWithFiles(3)");
			numIndexingQue++;
			String message = "";
			if (isIndexingWaitingToStop()) {
				message = "Indexing stop signal has been requested. Directory: " + directory.getAbsolutePath() + " will not be indexed.";
				//prtln(message);
				try {
					if (observer != null)
						observer.indexingCompleted(FileIndexingObserver.INDEXING_COMPLETED_ABORTED, message);
				} catch (Throwable t) {}
				numIndexingQue--;
				return;
			}
			startTime2 = System.currentTimeMillis();
			// Remove records for dirs we no longer know about.
			removeDeletedDirs();
			if (directory == null) {
				message = "Error indexing files. The directory specified is null.";
				addIndexingMessage(message);
				prtlnErr(message);
				try {
					if (observer != null)
						observer.indexingCompleted(FileIndexingObserver.INDEXING_COMPLETED_ERROR, message);
				} catch (Throwable t) {}
				numIndexingQue--;
				return;
			}

			boolean dirIsConfigured = false;
			synchronized (sourceDirs) {
				dirIsConfigured = sourceDirs.containsKey(directory.getAbsolutePath());
			}
			if (!dirIsConfigured) {
				message = "Error indexing files. The directory " + directory.getAbsolutePath() + " is not configured for indexing.";
				addIndexingMessage(message);
				prtlnErr(message);
				try {
					if (observer != null)
						observer.indexingCompleted(FileIndexingObserver.INDEXING_COMPLETED_ERROR, message);
				} catch (Throwable t) {}
				numIndexingQue--;
				return;
			}
			int result = INDEXING_ERROR;
			try {
				result = synchDirectory(directory, reindexAll, index);
			} catch (FileIndexingServiceException e) {
				addIndexingMessage(e.getMessage());
				prtlnErr("Error while indexing: " + e);
				//e.printStackTrace();
			}
			Thread.yield();
			endTime2 = System.currentTimeMillis();
			String elapsedTime = Utils.convertMillisecondsToTime(endTime2 - startTime2);
			//lastSyncTime = System.currentTimeMillis();
			if (result == INDEXING_ERROR) {
				message = "Errors occured while indexing directory '" + directory.getName() + "'. Some or all items did not " +
					"get indexed or re-indexed. Total number of items in index: " + index.getNumDocs();
				addIndexingMessage(message);
			}
			else if (result == INDEXING_ITEM_ERROR) {
				message = "Completed indexing directory '" + directory.getName() +
					"' successfully, however one or more items was indexed with errors. Total time "
					 + elapsedTime + ". Total number of items in index: " + index.getNumDocs();
				addIndexingMessage(message);
			}
			else if (result == INDEXING_ABORTED) {
				message = "Completed indexing directory '" + directory.getName() +
					"' successfully, however indexing was aborted and some files may not have been indexed. Total time " +
					elapsedTime + ". Total number of items in index: " + index.getNumDocs();
				addIndexingMessage(message);
			}
			else {
				message = "Completed indexing directory '" + directory.getName() +
					"' successfully. Total time " + elapsedTime + ". Total number of items in index: " + index.getNumDocs();
				addIndexingMessage(message);
			}
			// Notify the observer
			try {
				if (observer != null)
					observer.indexingCompleted(result, message);
			} catch (Throwable t) {}
			numIndexingQue--;
		}
	}


	/**
	 *  Updates the index to reflect the files in the directories this service is monitoring. Any new, deleted or
	 *  modified files that appear in the directories will be reflected in the index. This process may take
	 *  several minutes to return depending on the number of files needing to be updated.
	 *
	 * @param  reindexAll  True to reindex all records regardless of file mod date.
	 * @param  observer    The FileIndexingObserver that will be notified when indexing is complete, or null to
	 *      use none
	 */
	private void synchIndexWithFiles(boolean reindexAll, FileIndexingObserver observer) {
		synchronized (_indexingSync) {
			//prtln("synchIndexWithFiles(2)");
			numIndexingQue++;
			String message = "";
			if (isIndexingWaitingToStop()) {
				message = "Indexing stop signal has been requested before indexing began. No files were indexed.";
				//prtln(message);
				try {
					if (observer != null)
						observer.indexingCompleted(FileIndexingObserver.INDEXING_COMPLETED_ABORTED, message);
				} catch (Throwable t) {}
				numIndexingQue--;
				return;
			}
			startTime = System.currentTimeMillis();
			// Remove records for dirs we no longer know about.
			removeDeletedDirs();
			FISDirInfo[] dirs = null;
			synchronized (sourceDirs) {
				dirs = (FISDirInfo[]) (sourceDirs.values()).toArray(new FISDirInfo[]{});

				addIndexingMessage("Beginning indexing of " + sourceDirs.size() +
					" directories. Total number of items in index: " + index.getNumDocs());
			}
			// Sort by indexing priority
			Arrays.sort(dirs);
			int result = INDEXING_ERROR;
			try {
				for (int i = 0; i < dirs.length; i++) {
					result = synchDirectory(dirs[i].srcDir, reindexAll, index);
					Thread.yield();
				}
			} catch (FileIndexingServiceException e) {
				addIndexingMessage(e.getMessage());
				prtlnErr("Error while indexing: " + e);
				//e.printStackTrace();
			}
			endTime = System.currentTimeMillis();
			String elapsedTime = Utils.convertMillisecondsToTime(endTime - startTime);
			lastSyncTime = System.currentTimeMillis();
			if (result == INDEXING_ERROR) {
				synchronized (sourceDirs) {
					message = "Errors occured while indexing one or more of " + sourceDirs.size() +
						" directories. Some or all items did not get indexed or re-indexed. Total time " +
						elapsedTime + ". Total number of items in index: " + index.getNumDocs();
				}
				addIndexingMessage(message);
			}
			else if (result == INDEXING_ITEM_ERROR) {
				synchronized (sourceDirs) {
					message = "Completed indexing " + sourceDirs.size() +
						" directories successfully, however one or more items was indexed with errors. Total time "
						 + elapsedTime + ". Total number of items in index: " + index.getNumDocs();
				}
				addIndexingMessage(message);
			}
			else if (result == INDEXING_ABORTED) {
				synchronized (sourceDirs) {
					message = "Completed indexing " + sourceDirs.size() +
						" directories successfully, however indexing was requested to stop. Some or all directories may not have been indexed. "
						 + "Total time " + elapsedTime + ". Total number of items in index: " + index.getNumDocs();
				}
				addIndexingMessage(message);
			}
			else {
				synchronized (sourceDirs) {
					message = "Completed indexing " + sourceDirs.size() +
						" directories successfully. Total time " + elapsedTime + ". Total number of items in index: " + index.getNumDocs();
				}
				addIndexingMessage(message);
			}
			// Notify the observer
			try {
				if (observer != null)
					observer.indexingCompleted(result, message);
			} catch (Throwable t) {}
			numIndexingQue--;
		}
	}


	/**
	 *  Removes any documents in the index that belong to a directory that is configured in this
	 *  FileIndexingService but does not exist or to a directory that was once configured but was subsequently
	 *  deleted using the deleteDirectory() method. Nothing is done if the the directory exists and is configured
	 *  for indexing in this FileIndexingService.
	 */
	private final void removeDeletedDirs() {
		try {
			//prtln("removeDeletedDirs()");
			// Grab all the dirs that each doc in the index has been indexed under:
			List indexedDirs = index.getTerms("docdir_remove");
			String indexedDir;
			File dirFile;
			if (indexedDirs != null) {
				for (int i = 0; i < indexedDirs.size(); i++) {
					indexedDir = (String) indexedDirs.get(i);
					removeDirFromIndex(new File(indexedDir));
				}
			}
		} catch (NullPointerException npe) {
			prtlnErr("Error removeDeletedDirs(): " + npe);
			//npe.printStackTrace();
		} catch (Throwable e) {
			prtlnErr("Error removeDeletedDirs(): " + e);
		}
	}


	/**
	 *  Removes any documents in the index that belong to a directory that is configured in this
	 *  FileIndexingService but does not exist or to a directory that was once configured but was subsequently
	 *  deleted using the deleteDirectory() method. Nothing is done if the the directory exists and is configured
	 *  for indexing in this FileIndexingService. This method does not effect whether the direcory is configured
	 *  for indexing. Use method deleteDirectory() to remove a directory for indexing from this
	 *  FileIndexingService.
	 *
	 * @param  dirToRemove  A direcory whoes indexed files will be removed from the index if appropriate.
	 */
	private final void removeDirFromIndex(File dirToRemove) {
		try {
			boolean dirIsConfigured = false;
			synchronized (sourceDirs) {
				dirIsConfigured = sourceDirs.containsKey(dirToRemove.getAbsolutePath());
			}
			//prtln("removeDirFromIndex(): " + dirToRemove + " exists:" + dirToRemove.exists() + " isConfigured:" + dirIsConfigured);

			if (!dirIsConfigured || !dirToRemove.exists()) {
				FISDirInfo dirInfo = null;
				synchronized (sourceDirsHistory) {
					dirInfo = (FISDirInfo) sourceDirsHistory.get(dirToRemove.getAbsolutePath());
				}
				if (dirInfo != null) {
					addIndexingMessage("Removing index entries for directory " + dirToRemove.getAbsolutePath());
					removeDocs("docdir_remove", dirToRemove.getAbsolutePath(), dirInfo.getDocWriter());
				}
				else {
					//prtlnErr("removeDirFromIndex(): could not find dirInfo for dir " + dirToRemove.getAbsolutePath());
					addIndexingMessage("Removing index entries for directory " + dirToRemove.getAbsolutePath());
					removeDocs("docdir_remove", dirToRemove.getAbsolutePath(), null);
				}
			}
		} catch (NullPointerException npe) {
			prtlnErr("Error removeDirFromIndex(): " + npe);
			//npe.printStackTrace();
		} catch (Throwable e) {
			prtlnErr("Error removeDirFromIndex(): " + e);
		}
	}


	/**
	 *  Removes all documents that match the given term within the given field. Removed documents will either be
	 *  saved in the index and marked as deleted (indicated by the Lucene field "deleted" being indexed as
	 *  "true"), or removed from the index altogether as determined by the parameter passed in at the
	 *  constructor. This is useful for removing a single document that is indexed with a unique ID field, or to
	 *  remove a group of documents mathcing the same term for a given field. For example you might pass in an ID
	 *  of a record that needs to be removed along with the ID field that it is indexed under, or the file path
	 *  corresponding to a record along with the field "docsource." Note this is the same as {@link
	 *  SimpleLuceneIndex#removeDocs(String,String)} but is synchronized with other operations occuring in this
	 *  FileIndexinService and handles deletes accordingly.
	 *
	 * @param  field      The field that is searched.
	 * @param  term       The term that is matched for removal.
	 * @param  docWriter  The FileIndexingServiceWriter to use
	 */
	public final void removeDocs(String field, String term, FileIndexingServiceWriter docWriter) {
		removeDocs(field, term, docWriter, this.saveDeletes);
	}


	/**
	 *  Removes all documents that match the given terms within the given field. Removed documents will either be
	 *  saved in the index and marked as deleted (indicated by the Lucene field "deleted" being indexed as
	 *  "true"), or removed from the index altogether as determined by the parameter passed in at the
	 *  constructor. This is useful for removing multiple documents that are indexed with a unique ID field. For
	 *  example you might pass in an array of IDs needing to be removed. Note this is the same as {@link
	 *  SimpleLuceneIndex#removeDocs(String,String[])} but is synchronized with other operations occuring in this
	 *  FileIndexinService and handles deletes accordingly.
	 *
	 * @param  field      The field that is searched.
	 * @param  terms      The terms that are matched for removal.
	 * @param  docWriter  The FileIndexingServiceWriter to use
	 */
	public final void removeDocs(String field, String[] terms, FileIndexingServiceWriter docWriter) {
		removeDocs(field, terms, docWriter, this.saveDeletes);
	}


	/**
	 *  Removes all documents that match the given term within the given field. Removed documents will either be
	 *  saved in the index and marked as deleted (indicated by the Lucene field "deleted" being indexed as
	 *  "true"), or removed from the index altogether as determined by the parameter passed in to this method.
	 *  This is useful for removing a single document that is indexed with a unique ID field, or to remove a
	 *  group of documents mathcing the same term for a given field. For example you might pass in an ID of a
	 *  record that needs to be removed along with the ID field that it is indexed under, or the file path
	 *  corresponding to a record along with the field "docsource." Note this is the same as {@link
	 *  SimpleLuceneIndex#removeDocs(String,String)} but is synchronized with other operations occuring in this
	 *  FileIndexinService and handles deletes accordingly.
	 *
	 * @param  field               The field that is searched.
	 * @param  term                The term that is matched for removal.
	 * @param  saveDeletedRecords  True to save the removed documents in the index and mark them deleted, else
	 *      they will be removed from the index.
	 * @param  docWriter           The FileIndexingServiceWriter to use
	 */
	public final void removeDocs(
	                             String field,
	                             String term,
	                             FileIndexingServiceWriter docWriter,
	                             boolean saveDeletedRecords) {
		// Note: Method refactored on 8/29/06 to call the following instead of full redundent implementaion here
		removeDocs(field, new String[]{term}, docWriter, saveDeletedRecords);
	}


	/**
	 *  Removes all documents that match the given terms within the given field. Removed documents will either be
	 *  saved in the index and marked as deleted (indicated by the Lucene field "deleted" being indexed as
	 *  "true"), or removed from the index altogether as determined by the parameter passed in to this method.
	 *  This is useful for removing multiple documents that are indexed with a unique ID field. For example you
	 *  might pass in an array of IDs needing to be removed. Note this is the same as {@link
	 *  SimpleLuceneIndex#removeDocs(String,String[])} but is synchronized with other operations occuring in this
	 *  FileIndexinService and handles deletes accordingly.
	 *
	 * @param  field        The field that is searched.
	 * @param  terms        The terms that are matched for removal.
	 * @param  saveDeletes  True to save the removed documents in the index and mark them deleted, else they will
	 *      be removed from the index.
	 * @param  docWriter    Writer used to perform the delete.
	 */
	public final void removeDocs(
	                             String field,
	                             String[] terms,
	                             FileIndexingServiceWriter docWriter,
	                             boolean saveDeletes) {
		synchronized (_indexingSync) {
			numIndexingQue++;
			if (isIndexingWaitingToStop()) {
				//prtln("Indexing has been requested to stop. Documents will not be removed.");
				numIndexingQue--;
				return;
			}
			if (saveDeletes) {

				//				primaryIndex.deleteDocs(docWriter,field,terms);


				/**
				 * This class implements the functionality of deleteDocs
				 * The index updates are performed with chunks of chunksize, set to maxNumFilesToIndex
				 * @author Timo Proescholdt <timo@proescholdt.de>
				 *
				 */
				class RemoveDocCallback implements Callback {

					ArrayList deletedDocs;
					int chunksize;
					String field;
					String[] terms;
					SimpleLuceneIndex idx;

					public RemoveDocCallback(SimpleLuceneIndex idx, int chunksize, String field, String[] terms) {

						deletedDocs = new ArrayList(chunksize);
						this.chunksize=chunksize;
						this.terms=terms;
						this.field=field;
						this.idx=idx;
				}


					public void doWithDocument(Document dd) throws Exception {

						try {
							if (dd != null) {
								deletedDocs.add(dd);
							}
							/* if (e instanceof NullPointerException)
								e.printStackTrace(); */
						} catch( Throwable e) {
							addIndexingMessage("Error: Unable to remove documents from index. "+e);
							prtlnErr("Error removeDocs(): "+e);
						}

						if (deletedDocs.size()>= chunksize ) {
							idx.update(field, terms, (Document[]) deletedDocs.toArray(new Document[]{}), true);
							deletedDocs.clear();
						}


					}

					public void mycleanup() {
						try {
							idx.update(field, terms, (Document[]) deletedDocs.toArray(new Document[]{}), true);
							deletedDocs.clear();
						} catch (Exception e) {
							prtlnErr("cannot updateIndex "+e);
						}
					}

				}

				//RemoveDocCallback c = new RemoveDocCallback(primaryIndex, maxNumFilesToIndex, field, terms);
				//primaryIndex.doWithDocument(c, field, terms);
				RemoveDocCallback c = new RemoveDocCallback(index, maxNumFilesToIndex, field, terms);
				index.doWithDocument(c, field, terms);

				c.mycleanup();


			}
			else {
				//prtln("removeDocs2(): removing docs completely from the index");
				index.removeDocs(field, terms);
			}
			numIndexingQue--;
		}
	}


	/**
	 *  Re-indexes all documents that match the given term within the given field. Requires that the file for the
	 *  given document is still in it's original location. If the file is not in it's original location then the
	 *  index will remove the document without updating and it will not be marked as deleted. This is useful for
	 *  updating a single document that is indexed with a unique ID field, or to update a group of documents
	 *  mathcing the same term for a given field. For example you might pass in an ID of a record that needs
	 *  updating along with the ID field that it is indexed under, or the file path corresponding to a record
	 *  that needs updating along with the field "docsource."
	 *
	 * @param  field       The field that is searched.
	 * @param  term        The term that is matched for updates.
	 * @param  reindexAll  True to reindex all matching results, false to reindex only those matches whoes files
	 *      have been modified since the last update.
	 * @return             The number of matching documents to be updated.
	 */
	public int reindexDocs(String field, String term, boolean reindexAll) {
		List updateDocs = index.listDocs(field, term);
		new UpdateThread((Document[]) updateDocs.toArray(new Document[]{}), reindexAll).start();
		return updateDocs.size();
	}


	/**
	 *  Re-indexes all documents that match the given terms within the given field. This is useful for updating
	 *  multiple documents that are indexed with a unique ID field. For example you might pass in an array of IDs
	 *  needing to be updated along with the ID field that it is indexed under, or an array of file paths
	 *  corresponding to records that need updating along with the field "docsource."
	 *
	 * @param  field       The field that is searched.
	 * @param  terms       The terms that are matched for updates.
	 * @param  reindexAll  True to reindex all matching results, false to reindex only those matches whoes files
	 *      have been modified since the last update.
	 * @return             The number of matching documents to be updated.
	 */
	public int reindexDocs(String field, String[] terms, boolean reindexAll) {
		List updateDocs = index.listDocs(field, terms);
		new UpdateThread((Document[]) updateDocs.toArray(new Document[]{}), reindexAll).start();
		return updateDocs.size();
	}


	/**
	 *  Reindexes Documents managed by this FileIndexingService that match the given Lucene query.
	 *
	 * @param  query       A Lucene search query.
	 * @param  reindexAll  True to reindex all matching results, false to reindex only those matches whoes files
	 *      have been modified since the last update.
	 * @return             The number of matching documents to be updated.
	 */
	public int reindexDocs(String query, boolean reindexAll) {
		ResultDocList results = index.searchDocs(query);
		new UpdateThread(results, reindexAll).start();
		return results.size();
	}


	/**
	 *  Reindexes the given Documents.
	 *
	 * @param  docs        Lucene Documents from the same index that is managed by this FileIndexingService.
	 * @param  reindexAll  True to reindex all matching results, false to reindex only those matches whoes files
	 *      have been modified since the last update.
	 */
	public void reindexDocs(Document[] docs, boolean reindexAll) {
		new UpdateThread(docs, reindexAll).start();
	}


	/**
	 *  Reindexes the Documents in the given ResultDocs.
	 *
	 * @param  docs        Lucene ResultDocs from the same index that is managed by this FileIndexingService.
	 * @param  reindexAll  True to reindex all matching results, false to reindex only those matches whoes files
	 *      have been modified since the last update.
	 */
	public void reindexDocs(ResultDocList docs, boolean reindexAll) {
		new UpdateThread(docs, reindexAll).start();
	}


	/**
	 *  Syncronizes the files in the backgound.
	 *
	 * @author    jweather
	 */
	private class FileSyncThread extends Thread {
		private boolean reindexAll = false;
		private File directory = null;
		private FileIndexingObserver observer = null;


		/**
		 *  Constructor for the FileSyncThread object
		 *
		 * @param  reindexAll  true to index all
		 * @param  o           The FileIndexingObserver that will be notified when indexing is complete, or null to
		 *      use none
		 */
		public FileSyncThread(boolean reindexAll, FileIndexingObserver o) {
			this.reindexAll = reindexAll;
			observer = o;
			setDaemon(true);
		}


		/**
		 *  Constructor for the FileSyncThread object
		 *
		 * @param  reindexAll  DESCRIPTION
		 * @param  directory   DESCRIPTION
		 * @param  o           The FileIndexingObserver that will be notified when indexing is complete, or null to
		 *      use none
		 */
		public FileSyncThread(boolean reindexAll, File directory, FileIndexingObserver o) {
			this.reindexAll = reindexAll;
			this.directory = directory;
			observer = o;
			setDaemon(true);
		}


		/**  Main processing method for the FileSyncThread object */
		public void run() {
			if (directory != null) {
				synchIndexWithFiles(reindexAll, directory, observer);
			}
			else {
				synchIndexWithFiles(reindexAll, observer);
			}
		}
	}


	private class UpdateThread extends Thread {
		Object updateObjects;
		boolean reindexAll = false;
		File sourceDir = null;


		/**
		 *  Constructor for the UpdateThread object
		 *
		 * @param  filesToUpdate  DESCRIPTION
		 * @param  sourceDir      DESCRIPTION
		 * @param  reindexAll     DESCRIPTION
		 */
		public UpdateThread(File[] filesToUpdate, File sourceDir, boolean reindexAll) {
			this.updateObjects = filesToUpdate;
			this.reindexAll = reindexAll;
			this.sourceDir = sourceDir;
			setDaemon(true);
		}


		/**
		 *  Constructor for the UpdateThread object
		 *
		 * @param  docsToUpdate  DESCRIPTION
		 * @param  reindexAll    DESCRIPTION
		 */
		public UpdateThread(Document[] docsToUpdate, boolean reindexAll) {
			this.updateObjects = docsToUpdate;
			this.reindexAll = reindexAll;
			setDaemon(true);
		}


		/**
		 *  Constructor for the UpdateThread object
		 *
		 * @param  resultsToUpdate  DESCRIPTION
		 * @param  reindexAll       DESCRIPTION
		 */
		public UpdateThread(ResultDocList resultsToUpdate, boolean reindexAll) {
			this.updateObjects = resultsToUpdate;
			this.reindexAll = reindexAll;
			setDaemon(true);
		}


		/**  Main processing method for the UpdateThread object */
		public void run() {
			try {
				doIndexObjects(updateObjects, sourceDir, reindexAll, index);
			} catch (FileIndexingServiceException e) {
				addIndexingMessage(e.getMessage());
				prtlnErr("Error updating documents: " + e);
			}
		}
	}


	/**
	 *  Synchronizes the files with the index by adding, deleting or updating the appropriate entries in the
	 *  index when files change.
	 *
	 * @param  index                             The index to store the entries
	 * @param  srcDir                            The source file directory.
	 * @param  reindexAll                        True to force re-indexing of all files.
	 * @return                                   The final indexing status
	 * @exception  FileIndexingServiceException  If error
	 */
	private int synchDirectory(
	                           File srcDir,
	                           boolean reindexAll,
	                           SimpleLuceneIndex index)
		 throws FileIndexingServiceException {
		//prtln("synchDirectory()");
		if (srcDir == null) {
			addIndexingMessage("Directory is null. Unable to index.");
			return INDEXING_ERROR;
		}
		if (!srcDir.exists()) {
			addIndexingMessage("Warning: Directory " + srcDir.getAbsolutePath() + " does not exist.");
			// Make sure the index is cleared of any entries for this directory...
			removeDirFromIndex(srcDir);
			return INDEXING_DIR_DOES_NOT_EXIST;
		}
		if (!srcDir.canRead()) {
			addIndexingMessage("Warning: Unable to read directory " + srcDir.getAbsolutePath());
			return INDEXING_DIR_READ_ERROR;
		}
		boolean debugMemory = false;
		try {
			String dm = (String) index.getAttribute("debugMemory");
			if (dm != null && dm.equals("true"))
				debugMemory = true;
		} catch (Throwable t) {}
		//prtln("indexing dir: " + srcDir.getAbsolutePath());
		String message = srcDir.getAbsolutePath() + "-start--numADN:" + ADNFileIndexingWriter.getNumInstances()
			 + "-numCOLL:" + DleseCollectionFileIndexingWriter.getNumInstances()
			 + "-numANNO:" + DleseAnnoFileIndexingServiceWriter.getNumInstances();
		if (debugMemory)
			Utils.runCommand("/home/jweather/bin/log_process_memory.sh " + message, null, null);
		// Perform the indexing operation...
		int status = doIndexObjects(srcDir.listFiles(new XMLFileFilter()), srcDir, reindexAll, index);
		message = srcDir.getAbsolutePath() + "-beforGC--numADN:" + ADNFileIndexingWriter.getNumInstances()
			 + "-numCOLL:" + DleseCollectionFileIndexingWriter.getNumInstances()
			 + "-numANNO:" + DleseAnnoFileIndexingServiceWriter.getNumInstances();
		if (debugMemory)
			Utils.runCommand("/home/jweather/bin/log_process_memory.sh " + message, null, null);
		// Run garbage collection...
		MemoryCheck.runGC();
		message = srcDir.getAbsolutePath() + "-end--numADN:" + ADNFileIndexingWriter.getNumInstances()
			 + "-numCOLL:" + DleseCollectionFileIndexingWriter.getNumInstances()
			 + "-numANNO:" + DleseAnnoFileIndexingServiceWriter.getNumInstances();
		if (debugMemory)
			Utils.runCommand("/home/jweather/bin/log_process_memory.sh " + message, null, null);
		return status;
	}


	/**
	 *  Indexes or re-indexes the files or Documents indicated. Supported Objects are File, Document and
	 *  ResultDoc.
	 *
	 * @param  docsToUpdate                      Must be of type File[], Document[] or ResultDocList.
	 * @param  srcDir                            The source file directory or null. Must be supplied if
	 *      docsToUpdate is of type File[], else it's not used.
	 * @param  reindexAll                        True to reindex all documents even if a document alredy exists
	 *      for the item, else false to only index those items whoes file mod time has changed since last update.
	 * @param  index                             The index to store the entries.
	 * @return                                   The final indexing status
	 * @exception  FileIndexingServiceException  If error.
	 */
	private int doIndexObjects(
	                           Object docsToUpdateObj,
	                           File srcDir,
	                           boolean reindexAll,
	                           SimpleLuceneIndex index)
		 throws FileIndexingServiceException {
		
		//prtln("doIndexObjects() srcDir:" + srcDir);
		synchronized (_indexingSync) {
			List docsToUpdateList = null;
			
			// Make sure the IDMapper exclusion file is freshly loaded...
			Query.reloadIdExclusionDocument();
			numIndexingQue++;
			if (isIndexingWaitingToStop()) {
				numIndexingQue--;
				//prtln("Indexing has been requested to stop. Method indexObjects() is stopping...");
				return INDEXING_ABORTED;
			}
			final int FILE_ARRAY = 0;
			final int DOCUMENT_ARRAY = 1;
			final int RESULTDOC_ARRAY = 2;
			int object_type = -1;
			boolean errorOccured = false;
			if (docsToUpdateObj instanceof File[]) {
				object_type = FILE_ARRAY;
				docsToUpdateList = Arrays.asList((Object[])docsToUpdateObj);
			}
			else if (docsToUpdateObj instanceof Document[]) {
				object_type = DOCUMENT_ARRAY;
				docsToUpdateList = Arrays.asList((Object[])docsToUpdateObj);
			}
			else if (docsToUpdateObj instanceof ResultDocList) {
				object_type = RESULTDOC_ARRAY;
				docsToUpdateList = (ResultDocList)docsToUpdateObj;
			}
			else {
				numIndexingQue--;
				throw new FileIndexingServiceException("The docsToUpdate Object must be of type File[], Document[] or ResultDocList");
			}
			
			String msg = "Beginning to index " + docsToUpdateList.size() + " items";
			if (srcDir != null) {
				msg += " from " + srcDir.getAbsolutePath();
			}
			addIndexingMessage(msg);			
			
			RecordDataService recordDataService = null;
			FileIndexingServiceException thrown = null;
			try {
				int MAX_SIZE = maxNumFilesToIndex;
				if (validateFiles && MAX_SIZE > 100)
					MAX_SIZE = 100;
				// Make sure the index is up-to-date before proceeding
				while (index.isIndexing()) {
					Thread.sleep(10);
				}
				//MemoryCheck.runGC();
				num_add = 0;
				num_remove = 0;
				num_replace = 0;
				ArrayList removeFilesByPath = new ArrayList(MAX_SIZE);
				ArrayList removeFilesById = new ArrayList(MAX_SIZE);
				HashMap indexingSessionAttributes = new HashMap();
				HashMap removeItemsHashMap = new HashMap();
				ArrayList addDocs = new ArrayList(MAX_SIZE);
				recordDataService = (RecordDataService) index.getAttribute("recordDataService");
				Hashtable filesHash = new Hashtable(docsToUpdateList.size());
				FileIndexingServiceWriter myDocWriter = null;
				List thisDoc;
				FileIndexingServiceData newDocData;
				Document doc;
				//Document newDoc;
				Document dupIdDoc;
				List dupsToRemove = null;
				String newID;
				String rmAb;
				String ab;
				String deletedStr;
				String errorDocStr;
				String doc_mtime;
				File fileToUpdate;
				SimpleFileIndexingServiceDocReader tmpReader;
				// Initialize the idMapper ToDo: refactor so this does not need to occur each time!
				if (recordDataService != null)
					recordDataService.initIdMapper();
				Thread myThread = Thread.currentThread();
				myThread.setPriority(Thread.MIN_PRIORITY);
				for (int i = 0; i < docsToUpdateList.size() && !stopIndexing; i++) {
					//prtln("doIndexObjects() loop start i=" + i);
					// Handle File []
					if (object_type == FILE_ARRAY) {
						//prtln("doIndexObjects() FILE_ARRAY");
						myDocWriter = getNewFileIndexingServiceWriter(srcDir.getAbsolutePath());
						myDocWriter.setValidationEnabled(validateFiles);
						fileToUpdate = (File) docsToUpdateList.get(i);
						ab = fileToUpdate.getAbsolutePath();
						thisDoc = index.listDocs("docsource", ab);
					}
					// Handle Document [] or ResultDocList
					else {
						if (object_type == DOCUMENT_ARRAY) {
							//prtln("doIndexObjects() DOCUMENT_ARRAY");
							tmpReader = new SimpleFileIndexingServiceDocReader((Document) docsToUpdateList.get(i));
						}
						else {
							//prtln("doIndexObjects() RESULTDOC_ARRAY");
							tmpReader = new SimpleFileIndexingServiceDocReader(((ResultDoc) docsToUpdateList.get(i)).getDocument());
						}
						thisDoc = new ArrayList(1);
						thisDoc.add(docsToUpdateList.get(i));
						fileToUpdate = tmpReader.getFile();
						ab = fileToUpdate.getAbsolutePath();
						srcDir = new File(tmpReader.getDocDir());
						myDocWriter = getNewFileIndexingServiceWriter(srcDir.getAbsolutePath());
						myDocWriter.setValidationEnabled(validateFiles);
						//System.out.println("just got a new DocWriter Not File " + (myDocWriter == null ? " is null" : " not null"));
					}
					Thread.yield();
					// Replace if the file is in index but mod times are different,
					// or it's status had been deleted.
					if (thisDoc != null && thisDoc.size() == 1) {
						doc = (Document) thisDoc.get(0);
						doc_mtime = doc.get("modtime");
						errorDocStr = doc.get("doctype");
						boolean isErrorDoc = (errorDocStr != null && errorDocStr.equals("0errordoc"));
						deletedStr = doc.get("deleted");
						boolean isDeleted = (deletedStr != null && deletedStr.equals("true"));
						// Check if modtime is different, or if the record had previously been deleted.
						
						// Convert file mod time using DateFieldTools.timeToString to ensure same granularity: 
						String file_mtime = DateFieldTools.timeToString(fileToUpdate.lastModified());
												
						if (reindexAll ||
							!doc_mtime.equals(file_mtime) ||
							isErrorDoc ||
							isDeleted) {
							// To replace, add the file to both the remove and add lists.
							try {
								//prtln("doIndexDocs() case 1");
								newDocData = myDocWriter.create(fileToUpdate, doc, null, indexingSessionAttributes);
								if (newDocData != null && newDocData.getDoc() != null) {
									addDocs.add(newDocData.getDoc());
									removeFilesByPath.add(ab);
									num_replace++;
								}
								addRemoveFields(newDocData, removeItemsHashMap);
							} catch (Throwable e) {
								errorOccured = true;
								if (indexErrors) {
									//prtln("Indexing an error: " + e);
									//e.printStackTrace();
									ErrorFileIndexingWriter errDoc =
										new ErrorFileIndexingWriter(e);
									newDocData = errDoc.create(fileToUpdate, doc, null, indexingSessionAttributes);
									if (newDocData != null && newDocData.getDoc() != null) {
										//prtln("Adding an error: " + e);
										addDocs.add(newDocData.getDoc());
										removeFilesByPath.add(ab);
									}
									addRemoveFields(newDocData, removeItemsHashMap);
								}
								else {
									prtlnErr("Error: " + e);
									//e.printStackTrace();
								}
							}
						}
					}
					// Add the file if it's not found in the index
					else if (thisDoc == null || thisDoc.size() == 0) {
						try {
							//prtln("doIndexDocs() case 2");
							newDocData = myDocWriter.create(fileToUpdate, null, null, indexingSessionAttributes);
							if (newDocData != null && newDocData.getDoc() != null) {
								addDocs.add(newDocData.getDoc());
								num_add++;
							}
							addRemoveFields(newDocData, removeItemsHashMap);
							// Remove other Docs that have the same given ID:
							try {
								if (idFieldToRemove != null && newDocData != null && newDocData.getDoc() != null) {
									newID = newDocData.getDoc().get(idFieldToRemove);
									dupsToRemove = index.listDocs(idFieldToRemove, newID);
									if (dupsToRemove != null) {
										for (int p = 0; p < dupsToRemove.size(); p++) {
											rmAb = ((Document) dupsToRemove.get(p)).get("docsource");
											if (rmAb != null) {
												removeFilesByPath.add(rmAb);
												num_remove++;
											}
										}
									}
								}
							} catch (Throwable e) {
								prtlnErr("Error removing dup ID: " + e);
							}
						} catch (Throwable e) {
							errorOccured = true;
							if (indexErrors) {
								prtln("Indexing an error: " + e);
								e.printStackTrace();
								ErrorFileIndexingWriter errDoc =
									new ErrorFileIndexingWriter(e);
								newDocData = errDoc.create(fileToUpdate, null, null, indexingSessionAttributes);
								if (newDocData != null && newDocData.getDoc() != null) {
									addDocs.add(newDocData.getDoc());
								}
								addRemoveFields(newDocData, removeItemsHashMap);
							}
							else {
								prtlnErr("Error: " + e);
								//e.printStackTrace();
							}
						}
					}
					else if (thisDoc != null) {
						// This should never happen
						prtlnErr("Error: index contains multiple entries for file: " + ab);
					}
					filesHash.put(ab, new Object());
					fileToUpdate = null;
					// Limit any single update to MAX_SIZE additions.
					if (addDocs.size() >= MAX_SIZE) {
						/* prtln("synchDirectory2 adding " +
							addDocs.size() +
							" files and removing " +
							removeFilesByPath.size() +
							" files"); */
						// Remove docs that the writers have requested to be removed and update the index
						removeDocsFromIndex(index, removeItemsHashMap);
						index.update("docsource", removeFilesByPath, addDocs);
						removeItemsHashMap.clear();
						addDocs.clear();
						removeFilesByPath.clear();
						indexingSessionAttributes.clear();
					}
					if (thisDoc != null) {
						thisDoc.clear();
					}
					thisDoc = null;
					doc = null;
					ab = null;
					if (i % 50 == 0 && i != 0) {
						msg = "Indexed " + i + " of " + docsToUpdateList.size() + " items";
						if (srcDir != null) {
							msg += " from " + srcDir.getAbsolutePath();
						}
						msg += ". Progressing normally...";
						addIndexingMessage(msg);
					}
					//prtln("doIndexObjects() loop end i=" + i);
				}
				// ---- Remove files that are in the index but not in the file directory ---
				
				// Note: The below listDocs line creates a spike in memory (jconsole debugging). Refactor!
				//List dirDocs = index.listDocs("docdir_remove", srcDir.getAbsolutePath());





					if (myDocWriter == null && object_type == FILE_ARRAY) {
						myDocWriter = getNewFileIndexingServiceWriter(srcDir.getAbsolutePath());
						myDocWriter.setValidationEnabled(validateFiles);
					}


				/**
				 * This class implements the functionality of listDocs and the former loop over its result
				 * It removes files that are in the index but not in the directory
				 * It avoids the previous memory spike, by treating each document separately and only storing filenames 
				 * of documents that need to be removed 
				 * 
				 * @author Timo Proescholdt <timo@proescholdt.de>
				 *
				 */
				class CleanIndexCallback implements Callback {

					FileIndexingServiceWriter myDocWriter;
					boolean saveDeletes;
					Hashtable filesHash;
					ArrayList removeFilesByPath;
					ArrayList addDocs;
					int num_remove = 0;


					public CleanIndexCallback(FileIndexingServiceWriter myDocWriter,
							boolean saveDeletes, Hashtable filesHash,
							ArrayList removeFilesByPath,ArrayList addDocs) {

						this.myDocWriter=myDocWriter;
						this.saveDeletes=saveDeletes;
						this.filesHash=filesHash;
						this.removeFilesByPath=removeFilesByPath;
						this.addDocs=addDocs;
					}

					public void doWithDocument(Document doc) {

						Document rDoc = null;
						Document dd;

						String ab;
						String deletedStr;

						Thread.yield();
						rDoc = doc;

						ab = rDoc.get("docsource");
						deletedStr = rDoc.get("deleted");
						if (!filesHash.containsKey(ab)) {

							//prtln("doIndexDocs() case 3");
							// If tracking deletes, re-index as deleted
							if (saveDeletes) {
								if (deletedStr == null || !deletedStr.equals("true")) {
									try {
										removeFilesByPath.add(ab);
										dd = myDocWriter.getDeletedDoc(rDoc);
										if (dd != null) {
											addDocs.add(dd);
											num_remove++;
											prtln("synchDirectory(): saving a deleted file");
										}
									} catch (Throwable e) {
											prtlnErr("Problem creating deleted record entry: " + e);
										e.printStackTrace();
									}

								}
							}
							else {
								removeFilesByPath.add(ab);
								num_remove++;
							}

						}


					}

				}


				CleanIndexCallback c = new CleanIndexCallback(myDocWriter, saveDeletes, filesHash, removeFilesByPath, addDocs);

				index.doWithDocument(c, "docdir_remove", srcDir.getAbsolutePath());

				num_remove = c.num_remove;


				/* prtln("synchDirectory adding " +
					addDocs.size() +
					" files and removing " +
					removeFilesByPath.size() +
					" files"); */
				// Remove docs that the writers have requested to be removed and update the index
				removeDocsFromIndex(index, removeItemsHashMap);
				index.update("docsource", removeFilesByPath, addDocs);
				// Garbage collect
				removeItemsHashMap.clear();
				addDocs.clear();
				removeFilesByPath.clear();
				indexingSessionAttributes.clear();
				if (stopIndexing)
					addIndexingMessage("Indexing was requested to stop...");
			} catch (NullPointerException npe) {
				npe.printStackTrace();
				msg = "Error: Unable to perform indexing of " + docsToUpdateList.size() + " items";
				if (srcDir != null) {
					msg += " from directory " + srcDir.getAbsolutePath();
				}
				thrown = new FileIndexingServiceException(msg + ". Reason: NullPointerException");
			} catch (MmdException e) {
				msg = "Error: Unable to perform indexing of " + docsToUpdateList.size() + " items";
				if (srcDir != null) {
					msg += " from directory " + srcDir.getAbsolutePath();
				}
				thrown = new FileIndexingServiceException(msg + ". ID mapper error:" + e.getMessage());
			} catch (Throwable e) {
				msg = "Error: Unable to perform indexing of " + docsToUpdateList.size() + " items";
				if (srcDir != null) {
					msg += " from directory " + srcDir.getAbsolutePath();
				}
				thrown = new FileIndexingServiceException(msg + ". Reason: " + e.toString());
			} finally {
				if (recordDataService != null)
					recordDataService.closeIdMapper();
				numIndexingQue--;
			}
			// If an exception occured, throw it!
			if (thrown != null)
				throw thrown;
			//MemoryCheck.runGC();
			msg = "Successfully indexed " + docsToUpdateList.size() + " items";
			if (srcDir != null) {
				msg += " from " + srcDir.getAbsolutePath();
			}
			addIndexingMessage(msg);
			prtln("end indexObjects(). Number of items in index: " + index.getNumDocs());
			if (errorOccured)
				return INDEXING_ITEM_ERROR;
			else if (stopIndexing)
				return INDEXING_ABORTED;
			else
				return INDEXING_SUCCESS;
		}
	}


	private void addRemoveFields(FileIndexingServiceData data, HashMap docsToRemove) {
		if (data == null || docsToRemove == null) {
			return;
		}
		HashMap newDocsToRemove = data.getDocsToRemove();
		if (newDocsToRemove == null) {
			return;
		}
		Object[] newFields = newDocsToRemove.keySet().toArray();
		for (int i = 0; i < newFields.length; i++) {
			ArrayList values = (ArrayList) docsToRemove.get(newFields[i]);
			if (values == null) {
				values = new ArrayList();
			}
			values.addAll((ArrayList) newDocsToRemove.get(newFields[i]));
			docsToRemove.put(newFields[i], values);
		}
	}


	private void removeDocsFromIndex(SimpleLuceneIndex index, HashMap docsToRemove) {
		Object[] removeFields = docsToRemove.keySet().toArray();
		ArrayList values;
		for (int i = 0; i < removeFields.length; i++) {
			values = (ArrayList) docsToRemove.get(removeFields[i]);
			//prtln("removeDocsFromIndex(): removing " + values.size() + " from field " + removeFields[i]);
			index.removeDocs((String) removeFields[i], (String[]) values.toArray(new String[]{}));
		}
	}


	/**
	 *  Data structure that holds information about a directory of metadata files and factory for creating the
	 *  appropriate FileIndexingServiceWriter for handling this directory. The indexing priority may also be
	 *  indicated (PRIORITY_0 to PRIORITY_9). If none is specified the default priority is set to PRIORITY_5.
	 *  High-numbered priorities will get indexed before low-numbered priorities by the {@link
	 *  org.dlese.dpc.index.FileIndexingService}.
	 *
	 * @author    John Weatherley
	 */
	class FISDirInfo implements Serializable, Comparable {
		/**  The source directory */
		public File srcDir;
		/**  The indexing priority */
		public int indexingPriority = 5;
		private HashMap docWriterConfigAttributes;
		private Class docWriterClass;
		private FileIndexingService fis;
		private FileIndexingPlugin plugin;


		/**
		 *  Constructor for the DirInfo object
		 *
		 * @param  srcDir                     The source directory for the files
		 * @param  docWriterClass             The {@link FileIndexingServiceWriter} class that handles indexing this
		 *      type of file
		 * @param  docWriterConfigAttributes  Config attributes passed into the {@link FileIndexingServiceWriter}
		 * @param  plugin                     {@link FileIndexingPlugin} passed into the {@link
		 *      FileIndexingServiceWriter}
		 * @param  fis                        The FileIndexingService, which is passed in to the {@link
		 *      FileIndexingServiceWriter}
		 * @param  indexingPriority           The indexing priority
		 */
		public FISDirInfo(File srcDir, Class docWriterClass, HashMap docWriterConfigAttributes, FileIndexingPlugin plugin, FileIndexingService fis, int indexingPriority) {
			this.srcDir = srcDir;
			this.docWriterConfigAttributes = docWriterConfigAttributes;
			this.docWriterClass = docWriterClass;
			this.indexingPriority = indexingPriority;
			this.fis = fis;
			this.plugin = plugin;
		}


		/**
		 *  A factory method that instantiates a new FileIndexingServiceWriter for handling this type of file and
		 *  initializing it for writing.
		 *
		 * @return    A new FileIndexingServiceWriter
		 */
		public FileIndexingServiceWriter getDocWriter() {
			if (docWriterClass == null) {
				// Later: implement to return a default writer (perhaps self-configures based on file extension, etc...).
				return null;
			}
			else {
				try {
					// Instatiate and initialize the writer...
					FileIndexingServiceWriter fw = (FileIndexingServiceWriter) docWriterClass.newInstance();
					fw.setConfigAttributes(docWriterConfigAttributes);
					fw.setFileIndexingService(fis);
					if (plugin != null)
						fw.setFileIndexingPlugin(plugin);
					return fw;
				} catch (Throwable t) {
					prtlnErr("Error instantiating writer '" + docWriterClass + "': " + t);
					return null;
				}
			}
		}


		/**  Constructor for the FISDirInfo object */
		public FISDirInfo() { }


		/**
		 *  Compares two DirInfos by the index priority. Collections.sort() or Arrays.sort() can thus be used to
		 *  sort a list of DirInfos by priority.
		 *
		 * @param  o                       The DirInfos to compare
		 * @return                         Returns a negative integer, zero, or a positive integer as this object is
		 *      less than, equal to, or greater than the specified object.
		 * @exception  ClassCastException  If the object passed in is not a DirInfos.
		 */
		public int compareTo(Object o)
			 throws ClassCastException {
			FISDirInfo other = (FISDirInfo) o;
			if (this.indexingPriority < other.indexingPriority) {
				return 1;
			}
			else if (this.indexingPriority > other.indexingPriority) {
				return -1;
			}
			else {
				return 0;
			}
		}
	}


	/**
	 *  Synchronizes new, deleted or modified files with a Lucene index at a regular interval.
	 *
	 * @author    John Weatherley
	 */
	private class FileSyncTask extends TimerTask {
		/**  Main processing method for the FileSyncTask object */
		public void run() {
			//prtln("FileSyncTask.run()");
			SimpleFileIndexingObserver observer = null;
			if (debug)
				observer = new SimpleFileIndexingObserver("Background indexing timer task", "Beginning to index files...");
			synchIndexWithFiles(false, observer);
		}
	}


	private void addIndexingMessage(String msg) {
		ArrayList indexingMessages =
			(ArrayList) dataStore.get("INDEXING_STATUS_MESSAGES");
		if (indexingMessages == null) {
			indexingMessages = new ArrayList(NUM_INDEXING_MESSAGES);
		}
		indexingMessages.add(getSimpleDateStamp() + " " + msg);
		if (indexingMessages.size() > NUM_INDEXING_MESSAGES) {
			indexingMessages.remove(0);
		}
		dataStore.put("INDEXING_STATUS_MESSAGES", indexingMessages);
	}


	/**
	 *  Gets the last 10 indexing status messages.
	 *
	 * @return    The indexingMessages.
	 */
	public ArrayList getIndexingMessages() {
		ArrayList indexingMessages =
			(ArrayList) dataStore.get("INDEXING_STATUS_MESSAGES");
		if (indexingMessages == null) {
			indexingMessages = new ArrayList();
			indexingMessages.add("No indexing messages logged yet...");
		}
		return indexingMessages;
	}

	// -------------------- Testing methods -------------------
	/**
	 *  Starts a FileMoveTester iff one is not already initialized. The FileMoveTester simulate moving files in
	 *  and out of the sourceFile directory, for testing purposes only. Warning: FileMoveTester moves
	 *  metadatafiles. Only use with test records!)
	 *
	 * @param  docRoot              The context document root as obtainied by calling
	 *      getServletContext().getRealPath("/");
	 * @param  sourceFileDirectory  DESCRIPTION
	 */
	public void startTester(String docRoot, String sourceFileDirectory) {
		if (tester == null) {
			tester = new FileMoveTester(sourceFileDirectory,
				GeneralServletTools.getAbsolutePath(sourceFileDirectory + "/collectionTwo", docRoot));
		}
	}


	/**  Stops the FileMoveTester */
	public void stopTester() {
		if (tester != null) {
			tester.stop();
			tester = null;
		}
	}

	// -------------------- Utility methods -------------------
	/**
	 *  Return a string for the current time and date, sutiable for display in log files and output to standout:
	 *
	 * @return    The dateStamp value
	 */
	public static String getSimpleDateStamp() {
		try {
			return
				Utils.convertDateToString(new Date(), "EEE, MMM d h:mm:ss a");
		} catch (ParseException e) {
			return "";
		}
	}


	/**
	 *  Return a string for the current time and date, sutiable for display in log files and output to standout:
	 *
	 * @return    The dateStamp value
	 */
	public static String getDateStamp() {
		return
			new SimpleDateFormat("MMM d, yyyy h:mm:ss a zzz").format(new Date());
	}


	/**
	 *  Output a line of text to error out, with datestamp.
	 *
	 * @param  s  The text that will be output to error out.
	 */
	private final void prtlnErr(String s) {
		System.err.println(getDateStamp() + " FileIndexingService ERROR: " + s);
	}


	/**
	 *  Output a line of text to standard out, with datestamp, if debug is set to true.
	 *
	 * @param  s  The String that will be output.
	 */
	private final void prtln(String s) {
		if (debug) {
			System.out.println(getDateStamp() + " FileIndexingService: " + s);
		}
	}


	/**
	 *  Sets the debug attribute object
	 *
	 * @param  db  The new debug value
	 */
	public static void setDebug(boolean db) {
		debug = db;
	}
}

