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

import org.dlese.dpc.datamgr.*;
import org.dlese.dpc.propertiesmgr.*;
import org.dlese.dpc.index.*;
import org.dlese.dpc.index.writer.*;
import org.dlese.dpc.index.writer.xml.XMLIndexerFieldsConfig;
import org.dlese.dpc.index.reader.*;
import org.dlese.dpc.services.mmd.MmdRec;
import org.dlese.dpc.xml.*;
import org.dlese.dpc.vocab.*;
import org.dlese.dpc.webapps.tools.*;
import org.dlese.dpc.oai.*;
import org.dlese.dpc.util.*;
import org.dlese.dpc.index.analysis.*;
import org.dlese.dpc.index.queryParser.FieldExpansionQueryParser;
import org.dlese.dpc.index.queryParser.XMLQueryParser;
import org.dlese.dpc.repository.action.form.SetDefinitionsForm;
import org.dlese.dpc.index.search.DateRangeFilter;

import org.apache.lucene.search.*;
import org.apache.lucene.document.*;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.index.*;
import org.apache.lucene.index.Term;

import javax.servlet.http.HttpServletRequest;

import org.apache.lucene.analysis.*;
import org.apache.lucene.analysis.standard.StandardAnalyzer;

import org.dom4j.Element;
import org.dom4j.Attribute;
import org.dom4j.Node;
import org.dom4j.Branch;

// JDK imports
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.StringReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.OutputStreamWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.LinkedList;
import java.util.Properties;
import java.text.*;
import java.util.*;
import java.net.URL;

/**
 *  Manages a repository of metadata files for use in discovery, cataloging, OAI and other applications.
 *  Provides and maintains a Lucene index of the metadata, with support for multiple collections and metadata
 *  formats.<p>
 *
 *
 *
 * @author     John Weatherley
 * @version    $Id: RepositoryManager.java,v 1.187.2.1 2012/02/15 23:28:03 jweather Exp $
 */
public class RepositoryManager {
	private final String PROPS_NAME = "RepositoryManager.properties";
	// Time in minutes.
	private int updateFrequency = 0;
	private String repositoryDataDir;
	private File itemIndexerConfigDir = null;
	private SerializedDataManager adminData = null;
	private static String defaultExampleID = null;
	private SimpleLuceneIndex index = null;
	private SimpleLuceneIndex dupItemsIndex = null;
	private FileIndexingService fileIndexingService = null;
	private XMLConversionService xmlConversionService = null;
	private XMLIndexerFieldsConfig xmlIndexerFieldsConfig = null;
	private ArrayList configuredSets = null;
	private ArrayList configuredFormats = null;
	private ArrayList enabledSets = null;
	private HashMap enabledSetsHashMap = null;
	private HashMap disabledSetsHashMap = null;
	private HashMap configuredSetInfosHashMap = null;
	private ArrayList disabledSets = null;
	private String enabledSetsQuery = null;
	private String contextURL = null;
	private String indexLocation = null;
	private String idMapperExclusionFile = null;
	private MetadataVocab metadataVocab = null;
	private boolean removeDocs = true;
	private RecordDataService recordDataService = null;
	private long setConfigLastModified = -1;
	private File collectionRecordsDir = null;
	private String metadataRecordsLocation = null;
	private boolean configureFromCollectionRecords = false;
	private Timer indexingTimer;
	private String indexingStartTime = null;
	private String indexingDaysOfWeek = null;
	private Date indexingStartTimeDate = null;
	private int[] indexingDaysOfWeekArray = null;
	private String drcBoostingQuery = null;
	private String multiDocBoostingQuery = null;
	private String nonDiscoverableStatusQuery = null;
	private PropertiesManager properties = null;
	private XMLFileIndexingWriterFactory xmlFileIndexingWriterFactory = null;
	private HashMap fileIndexingPlugins = new HashMap();
	private File repositoryConfigDir = null;
	private boolean isOaiPmhEnabled = true;
	private File setsConfigFile = null;
	private VirtualSearchFieldMapper virtualSearchFieldMapper = null;
	private Query oaiFilterQuery = null;
	private String dataProviderBaseUrlPathEnding = "/provider";
	private String serverUrl = null;

	// Lock for methods that are part of the public update API
	private Object publicUpdateApiLock = new Object();

	private Object setInfosLock = new Object();

	// Values extracted from the config files
	private String[] defaultSearchFields = null;
	private String[] fieldsUsedForBoosting = null;
	private Map boostingValues = null;

	// Default search boosing logic
	private final static double defaultDrcBoostFactor = 0.1;
	private final static double defaultMultiDocBoostFactor = 0.2;
	private final static double defaultTitleBoostFactor = 1.0;
	private final static double defaultStemmingBoostFactor = 0.1;
	private final static boolean defaultStemmingEnabled = true;
	private final static String ABSOLUTE_DRC_BOOST_QUERY = "(partofdrc:true^95 OR partofdrc:false)";

	/**
	 *  Specifies in the {@link #setFileIndexingPlugin(String xmlFormat, FileIndexingPlugin plugin)} method that
	 *  the given plugin should be used when indexing all XML formats.
	 */
	public final static String PLUGIN_ALL_FORMATS = "PLUGIN_ALL_FORMATS";

	// Caution: ALL class and instance variables are shared.
	// Multiple requests may use this object concurrently.

	private boolean removeInvalidRecords = true;
	private boolean initialized = false;
	private static boolean debug = false;
	private boolean reindexAllFiles = false;
	private int maxFilesToIndex = 500;

	private Hashtable additionalIndexers;
	
	public void setAdditionalIndices(Hashtable additionalIndexers) {
			this.additionalIndexers=additionalIndexers;
	}

	/**
	 *  Init method initializes the data for serving.
	 *
	 * @param  indexCollectionRecords  True to index the collections records when called
	 * @return                         1 iff successful.
	 */
	public int init(boolean indexCollectionRecords) {

		initialized = false;

		//prtln("initializing RepositoryManager...");

		String initErrorMsg = "";

		// Determine whether to configure off of collection records:
		if (collectionRecordsDir != null && metadataRecordsLocation != null)
			configureFromCollectionRecords = true;

		File adminDataDir = new File(repositoryDataDir + "/admin_data");
		if (!adminDataDir.exists()) {
			adminDataDir.mkdirs();
			//prtln("Created directory " + adminDataDir.getAbsolutePath());
		}

		try {
			adminData = new SerializedDataManager(adminDataDir.getAbsolutePath(), true);
		} catch (Exception e) {
			prtlnErr("Error initializing the adminData SerializedDataManager " + e);
			return -1;
		}

		loadDefaults(false);

		try {
			loadNewIndex();
		} catch (Throwable t) {
			prtlnErr("Error loading the index: " + t);
			t.printStackTrace();
			return -1;
		}

		if (recordDataService != null) {
			recordDataService.init(this);
			index.setAttribute("recordDataService", recordDataService);
		}
		xmlFileIndexingWriterFactory = new XMLFileIndexingWriterFactory(recordDataService, getIndex(), xmlIndexerFieldsConfig,additionalIndexers);

		index.setAttribute("repositoryManager", this);

		// Set up file indexer to remove docs altogether or save them as status deleted
		if (removeDocs)
			fileIndexingService = new FileIndexingService(index, 0,
				repositoryDataDir + "/file_indexing_service_data", maxFilesToIndex);
		else
			fileIndexingService = new FileIndexingService(index, 0, true, "id",
				repositoryDataDir + "/file_indexing_service_data", maxFilesToIndex);

		FileIndexingService.setDebug(debug);

		if (getValidateRecords() != null && getValidateRecords().equals("true"))
			fileIndexingService.setValidationEnabled(true);
		else
			fileIndexingService.setValidationEnabled(false);

		// Configure off of the collection-level records, if indicated
		/* try {
			Thread.sleep(500);
		} catch (InterruptedException e) {} */
		loadCollectionRecords(indexCollectionRecords);

		// Init fileIndexingService with the directories of files...
		ArrayList setInfos = getSetInfos();
		if (setInfos != null) {
			SetInfo set;
			ArrayList dirInfos;
			DirInfo dirInfo;
			String setSpec;
			for (int i = 0; i < setInfos.size(); i++) {
				set = (SetInfo) setInfos.get(i);
				putSetInIndex(set);
			}
		}
		// Clean up the set infos if unable to retrieve. This may occur
		// if the SetInfos class has been modified and thus cannot be loaded.
		else {
			try {
				synchronized (setInfosLock) {
					adminData.delete(Keys.SET_INFOS);
				}
			} catch (Throwable e) {}
		}

		// 24 hours, in seconds
		long hours24 = 60 * 60 * 24;

		// Start the indexer timer
		if (indexingStartTime != null) {
			Date startTime = null;
			try {
				Date currentTime = new Date(System.currentTimeMillis());
				//Date oneHourFromNow = new Date(System.currentTimeMillis() + (1000 * 60 * 60));
				Date oneHourFromNow = new Date(System.currentTimeMillis() + (1000 * 60));
				// One minute from now instead...
				int dayInYear = Integer.parseInt(Utils.convertDateToString(currentTime, "D"));
				int year = Integer.parseInt(Utils.convertDateToString(currentTime, "yyyy"));

				startTime = Utils.convertStringToDate(year + " " + dayInYear + " " + indexingStartTime, "yyyy D H:mm");

				// Make sure the timer starts one hour from now or later
				if (startTime.compareTo(oneHourFromNow) < 0) {
					if (dayInYear == 365) {
						year++;
						dayInYear = 1;
					}
					else
						dayInYear++;
					startTime = Utils.convertStringToDate(year + " " + dayInYear + " " + indexingStartTime, "yyyy D H:mm");
				}

				// Parse the days-of-week:
				try {
					indexingDaysOfWeekArray = null;
					if (indexingDaysOfWeek != null && indexingDaysOfWeek.trim().length() > 0) {
						String[] indexingDaysOfWeekStrings = indexingDaysOfWeek.split(",");
						if (indexingDaysOfWeekStrings.length > 0)
							indexingDaysOfWeekArray = new int[indexingDaysOfWeekStrings.length];
						for (int i = 0; i < indexingDaysOfWeekStrings.length; i++) {
							indexingDaysOfWeekArray[i] = Integer.parseInt(indexingDaysOfWeekStrings[i].trim());
							if (indexingDaysOfWeekArray[i] < 1 || indexingDaysOfWeekArray[i] > 7)
								throw new Exception("Value must be an integer from 1 to 7 but found " + indexingDaysOfWeekArray[i]);
						}
					}
				} catch (Throwable t) {
					String msg = "Error parsing indexingDaysOfWeek value: " + t.getMessage();
					indexingDaysOfWeekArray = null;
					throw new Exception(msg);
				}

				startIndexingTimer(hours24, startTime);
			} catch (Exception e) {
				prtlnErr("Syntax error in context parameter 'indexingStartTime' or 'indexingDaysOfWeek'. Indexing timer cron not started: " + e.getMessage());
			}

		}
		else
			startIndexingTimer(updateFrequency * 60, null);

		// Create a new VirtualSearchFieldMapper
		virtualSearchFieldMapper = new VirtualSearchFieldMapper(getIndex().getQueryParser());

		//Set up the OAI sets configuration, if one is saved already on disc
		try {
			loadListSetsConfigFile();
		} catch (Exception e) {
			prtlnErr("Unable to read OAI set configuration: " + e);
			if (e instanceof NullPointerException)
				e.printStackTrace();
		}

		initialized = true;

		// Return 1 on success.
		return 1;
	}


	/**
	 *  Loads a new index at the current location and closes the previous one, if one was open.
	 *
	 * @exception  Exception  Description of the Exception
	 */
	private void loadNewIndex() throws Exception {

		// Set up indexing fields for XML formats, if a config exists:
		try {
			File xmlIndexerConf = new File(getConfigDir(), "xmlIndexerFieldsConfigIndex.xml");
			if (xmlIndexerConf.exists()) {
				xmlIndexerFieldsConfig = new XMLIndexerFieldsConfig(xmlIndexerConf.toURL());
				prtln("Search fields initialized successfully (XMLIndexerFieldsConfig)");
			}
		} catch (Throwable t) {
			prtlnErr("Error initializing the XMLIndexerFieldsConfig (skipping...): " + t);
		}

		// Set up an Analyzer for indexing and searching that handles each field appropriately (based on the config provided)
		PerFieldAnalyzer perFieldAnalyzer = new PerFieldAnalyzer();

		// Add the fields/analyzers from the fields config to the perFieldAnalyzer:
		if (xmlIndexerFieldsConfig != null) {
			Iterator it = xmlIndexerFieldsConfig.getFieldAnalyzers().entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry pairs = (Map.Entry) it.next();
				prtln("Adding field/analyzer from XMLIndexerFieldsConfig: " + pairs.getKey() + "=" + pairs.getValue());
				perFieldAnalyzer.setAnalyzer((String) pairs.getKey(), (String) pairs.getValue());
			}
		}

		// Configure the core Analyzers to use on a per-field basis, overriding any definitions provided above:
		perFieldAnalyzer.addAnalyzersInBundle(Utils.getPropertiesResourceBundle("RepositoryManagerLuceneAnalyzers.properties"));
		perFieldAnalyzer.addAnalyzersInBundle(Utils.getPropertiesResourceBundle("FileIndexingPluginLuceneAnalyzers.properties"));

		prtln("perFieldAnalyzer configured as: " + perFieldAnalyzer);

		if (indexLocation == null)
			indexLocation = repositoryDataDir + "/repository_index";

		SimpleLuceneIndex oldIndex = index;

		SimpleLuceneIndex newIndex = new SimpleLuceneIndex(indexLocation, perFieldAnalyzer);
		newIndex.setOperator(SimpleLuceneIndex.DEFAULT_AND);
		index = newIndex;
		index.setAttribute("repositoryManager", this);

		if (oldIndex != null)
			oldIndex.close();
	}


	/**
	 *  Changes the frequency of reindexing to the new value. Same as {@link #startTimerThread(long
	 *  updateFrequency)}.
	 *
	 * @param  updateFrequency  The number of seconds between index updates.
	 */
	private void changeIndexingTimerFrequency(long updateFrequency) {
		startIndexingTimer(updateFrequency);
	}



	/**
	 *  Start or restarts the indexer thread with the given update frequency. Same as {@link
	 *  #changeUpdateFrequency(long updateFrequency)}.
	 *
	 * @param  updateFrequency  The number of seconds between index updates.
	 */
	private void startIndexingTimer(long updateFrequency) {
		startIndexingTimer(updateFrequency, null);
	}



	/**
	 *  Start or restarts the timer thread with the given update frequency, beginning at the specified time/date.
	 *  Use this method to schedule the timer to run as a nightly cron, beginning at the time you wish the
	 *  indexer to run.
	 *
	 * @param  updateFrequency  The number of seconds between index updates.
	 * @param  startTime        The time at which start the indexing process.
	 */
	private void startIndexingTimer(long updateFrequency, Date startTime) {
		// Make sure the indexing timer is stopped before starting...
		stopIndexingTimer();

		this.indexingStartTimeDate = startTime;

		if (updateFrequency > 0) {
			if (indexingTimer != null)
				indexingTimer.cancel();

			// Set daemon to true
			indexingTimer = new Timer(true);

			// Convert seconds to milliseeconds
			long freq = ((updateFrequency > 0) ? updateFrequency * 1000 : 60000);

			// Start the indexer at regular intervals beginning at the specified Date/Time
			if (startTime != null) {
				try {
					String daysOfWeekMsg = "all days";
					if (indexingDaysOfWeekArray != null) {
						daysOfWeekMsg = "these days of the week: ";
						for (int i = 0; i < indexingDaysOfWeekArray.length; i++)
							daysOfWeekMsg += Utils.getDayOfWeekString(indexingDaysOfWeekArray[i]) + (i == indexingDaysOfWeekArray.length - 1 ? "" : ", ");
					}

					prtln("Indexing timer is scheduled to start " +
						Utils.convertDateToString(startTime, "EEE, MMM d, yyyy h:mm a zzz") +
						", and run every " + Utils.convertMillisecondsToTime(freq) + " on " + daysOfWeekMsg + ".");
				} catch (ParseException e) {}

				indexingTimer.scheduleAtFixedRate(new FileIndexingTask(), startTime, freq);
			}
			// Start the indexer at regular intervals beginning after 6 seconds
			else {
				prtln("Indexing timer scheduled run every " + Utils.convertMillisecondsToTime(freq) + ".");
				indexingTimer.schedule(new FileIndexingTask(), 6000, freq);
			}

			prtln("RepositoryManager indexing timer started");
		}
	}



	/**  Stops the indexing timer thread. */
	private void stopIndexingTimer() {
		if (indexingTimer != null) {
			indexingTimer.cancel();
			prtln("RepositoryManager indexing timer stopped");
		}
	}



	/**  Tear down and clean up by gracefully stopping threads. */
	public void destroy() {
		fileIndexingService.stopTimerThread();
		this.stopIndexingTimer();
		fileIndexingService.stopIndexing();
		if (index != null)
			index.close();
		if (dupItemsIndex != null)
			dupItemsIndex.close();
		prtln("RepositoryManager destroy()");
	}


	/**
	 *  Gets the index attribute of the RepositoryManager object
	 *
	 * @return    The index value
	 */
	public SimpleLuceneIndex getIndex() {
		return index;
	}


	/**  Deletes the repository index and re-initializes a new, empty one in its place. */
	public void deleteIndex() {
		fileIndexingService.stopIndexing();
		if (index != null)
			index.deleteAndReinititlize();
		if (dupItemsIndex != null)
			dupItemsIndex.deleteAndReinititlize();
	}


	/**
	 *  Instructs the indexer to stop indexing wherever its at in the process. This method may take several
	 *  seconds to return.
	 */
	public void stopIndexing() {
		fileIndexingService.stopIndexing();
	}


	/**
	 *  Determins whether indexing is in progress.
	 *
	 * @return    True if indexing is in progress, false if not
	 */
	public boolean isIndexing() {
		return fileIndexingService.isIndexing();
	}


	/**
	 *  Sets the de-duping index to use.
	 *
	 * @param  dupItemsIndex  The new dupItemsIndex value
	 */
	public void setDupItemsIndex(SimpleLuceneIndex dupItemsIndex) {
		this.dupItemsIndex = dupItemsIndex;
	}


	/**
	 *  Gets the version number of the last time the repository index was modified by adding, deleting or
	 *  changing an item. The version number counts the number of times the index was modified.
	 *
	 * @return    The indexLastModifiedCount value
	 */
	public long getIndexLastModifiedCount() {
		return index.getLastModifiedCount();
	}


	/**
	 *  Gets the time the sets configuation status was last modified (enabled, disabled, added, deleted).
	 *
	 * @return    The setStatusModifiedTime value
	 */
	public long getSetStatusModifiedTime() {
		return setConfigLastModified;
	}


	/**
	 *  Gets the indexingMessages attribute of the RepositoryManager object
	 *
	 * @return    The indexingMessages value
	 */
	public ArrayList getIndexingMessages() {
		return fileIndexingService.getIndexingMessages();
	}



	/**
	 *  Constructor for the RepositoryManager. Known uses: OAIProviderServlet.
	 *
	 * @param  repositoryConfigDir  The directory where config files reside (default search/boosting fields,
	 *      other config)
	 * @param  repositoryDataDir    The directory where the repository data and certain configs resides.
	 * @param  updateFrequency      How often to update the index, in minutes.
	 * @param  maxFilesToIndex      Max files to index per block.
	 * @param  removeDocs           True to remove documents completely from the index, false to keep the
	 *      documents in the index but marked as deleted.
	 */
	public RepositoryManager(File repositoryConfigDir, String repositoryDataDir, int updateFrequency, int maxFilesToIndex, boolean removeDocs) {
		//prtln("RepositoryManager()");
		this.repositoryConfigDir = repositoryConfigDir;
		this.updateFrequency = updateFrequency;
		this.repositoryDataDir = repositoryDataDir;
		this.maxFilesToIndex = maxFilesToIndex;
		this.removeDocs = removeDocs;
	}



	/**
	 *  Constructor for the RepositoryManager. Known uses: DDSServlet.
	 *
	 * @param  repositoryConfigDir   The directory where config files reside (default search/boosting fields,
	 *      other config)
	 * @param  itemIndexerConfigDir  The directory where ItemIndexer configs are located
	 * @param  repositoryDataDir     Directory where admin data is stored.
	 * @param  indexLocation         Directory where index is stored.
	 * @param  indexingStartTime     The time of day to start the indexer, in H:mm, for example 0:11 or 23:11.
	 * @param  indexingDaysOfWeek    The days of week to run the indexer as a comma separated list of integers,
	 *      for example 1,3,5 where 1=Sunday, 2=Monday, 7=Saturday etc. or null for all days.
	 * @param  recordDataService     The RecordDataService to use, or null if none is needed.
	 * @param  removeDocs            True to remove documents completely from the index, false to keep the
	 *      documents in the index but marked as deleted.
	 * @param  reindexAllFiles       True to instruct the indexer to delete and reindex each file during each
	 *      indexing pass, false to only reindex those files whoes mod time have changed (more efficient).
	 */
	public RepositoryManager(File repositoryConfigDir,
	                         File itemIndexerConfigDir,
	                         String repositoryDataDir,
	                         String indexLocation,
	                         String indexingStartTime,
	                         String indexingDaysOfWeek,
	                         RecordDataService recordDataService,
	                         boolean removeDocs,
	                         boolean reindexAllFiles) {
		//prtln("RepositoryManager()");
		this.itemIndexerConfigDir = itemIndexerConfigDir;
		this.removeDocs = removeDocs;
		this.updateFrequency = 0;
		this.indexingStartTime = indexingStartTime;
		this.indexingDaysOfWeek = indexingDaysOfWeek;
		this.indexLocation = indexLocation;
		this.repositoryDataDir = repositoryDataDir;
		this.repositoryConfigDir = repositoryConfigDir;
		this.recordDataService = recordDataService;
		this.reindexAllFiles = reindexAllFiles;
		if (recordDataService != null)
			this.metadataVocab = recordDataService.getVocab();
	}


	/**
	 *  Gets the directory where the RepositoryManager config files reside.
	 *
	 * @return    The configDir value
	 */
	public File getConfigDir() {
		return repositoryConfigDir;
	}



	/**
	 *  Gets the path to an XML file the defines IDs that the IDMapper service should exclude from its list of
	 *  duplicates.
	 *
	 * @return    The idMapperExclusionFilePath, or null if none used
	 */
	public String getIdMapperExclusionFilePath() {
		return idMapperExclusionFile;
	}


	/**
	 *  Sets the path to an XML file the defines IDs that the IDMapper service should exclude from its list of
	 *  duplicates. Set to null to use none
	 *
	 * @param  filePath  The new idMapperExclusionFilePath value, or null to specify none
	 */
	public void setIdMapperExclusionFilePath(String filePath) {
		idMapperExclusionFile = filePath;
	}


	/**
	 *  Gets the interval by which the index is updated to check for changes in the meatdata files, in minutes. A
	 *  value of 0 indicates the index is not updated automatically.
	 *
	 * @return    The updateFrequency in minutes.
	 */
	public int getUpdateFrequency() {
		return updateFrequency;
	}


	/**
	 *  Gets the date the indexer scheduled to start.
	 *
	 * @return    The the date the indexer scheduled to start, or null if not available.
	 */
	public Date getIndexingStartTime() {
		return indexingStartTimeDate;
	}


	/**
	 *  Gets the days of the week the indexer will run as an array of Calendar.DAY_OF_WEEK fields, or null to
	 *  indicate all days of the week.
	 *
	 * @return    The indexingDaysOfWeek or null for all days
	 */
	public int[] getIndexingDaysOfWeek() {
		return indexingDaysOfWeekArray;
	}


	/**
	 *  Update the current MetadataVocab with a new one.
	 *
	 * @param  newVocab  The new MetadataVocab object.
	 */
	public void updateVocab(MetadataVocab newVocab) {
		metadataVocab = newVocab;
		if (recordDataService != null)
			recordDataService.updateVocab(newVocab);
	}


	private String vocabAudience = "community";
	private String vocabLanguage = "en-us";


	/**
	 *  Sets the metadataVocabAudienceDefault attribute of the RepositoryManager object
	 *
	 * @param  audience  The new metadataVocabAudienceDefault value
	 */
	public void setMetadataVocabAudienceDefault(String audience) {
		vocabAudience = audience;
	}


	/**
	 *  Gets the metadataVocabAudienceDefault attribute of the RepositoryManager object
	 *
	 * @return    The metadataVocabAudienceDefault value
	 */
	public String getMetadataVocabAudienceDefault() {
		return vocabAudience;
	}


	/**
	 *  Sets the metadataVocabLanguageDefault attribute of the RepositoryManager object
	 *
	 * @param  language  The new metadataVocabLanguageDefault value
	 */
	public void setMetadataVocabLanguageDefault(String language) {
		vocabLanguage = language;
	}


	/**
	 *  Gets the metadataVocabLanguageDefault attribute of the RepositoryManager object
	 *
	 * @return    The metadataVocabLanguageDefault value
	 */
	public String getMetadataVocabLanguageDefault() {
		return vocabLanguage;
	}


	/**
	 *  Loads the default values for user-defined OAI settings.
	 *
	 * @param  override  True to overried all existing settings, false to leave existing values untouched.
	 */
	private void loadDefaults(boolean override) {
		try {
			prtln("loading props file: " + PROPS_NAME);
			properties = new PropertiesManager(PROPS_NAME);
		} catch (IOException ioe) {
			prtln("Error loading RepositoryManager properties: " + ioe);
			return;
		}

		try {
			defaultExampleID = properties.getProperty("exampleid.default");
			if (defaultExampleID == null) {
				defaultExampleID = "abc-1234";
			}

			if (override || getProviderStatus() == null) {
				setProviderStatus(properties.getProperty("providerStatus.default"));
			}
			if (override || getHarvesterStatus() == null) {
				setHarvesterStatus(properties.getProperty("harvesterStatus.default"));
			}
			if (override || getRepositoryIdentifier() == null) {
				setRepositoryIdentifier(properties.getProperty("repositoryIdentifier.default"));
			}
			if (override || getProtocolVersion() == null) {
				setProtocolVersion(properties.getProperty("protocolVersion"));
			}
			if (override || getRepositoryName() == null) {
				setRepositoryName(properties.getProperty("repositoryName.default"));
			}
			if (override || getEarliestDatestamp() == null) {
				setEarliestDatestamp(properties.getProperty("earliestDatestamp.default"));
			}
			if (override || getGranularity() == null) {
				setGranularity(properties.getProperty("granularity.default"));
			}
			if (override || getNumIdentifiersResults() == null) {
				setNumIdentifiersResults(properties.getProperty("numIdentifiersResults.default"));
			}
			if (override || getNumRecordsResults() == null) {
				setNumRecordsResults(properties.getProperty("numRecordsResults.default"));
			}
			if (override || getRemoveInvalidRecords() == null) {
				setRemoveInvalidRecords(properties.getProperty("removeInvalidRecords.default"));
			}
			if (override || getValidateRecords() == null) {
				setValidateRecords(properties.getProperty("validateFiles.default"));
			}
			if (override || getCompressions() == null) {
				removeAdminObject(Keys.COMPRESSIONS);
				addCompression(properties.getProperty("compression.default"));
			}
			if (override || getDescriptions() == null) {
				removeAdminObject(Keys.DESCRIPTIONS);
				addDescription(properties.getProperty("description.default"));
			}
			if (override || getAdminEmails() == null) {
				removeAdminObject(Keys.ADMIN_EMAILS);
				addAdminEmail(properties.getProperty("adminEmail.default"));
			}

			String rir = getRemoveInvalidRecords();
			if (rir != null && rir.equals("false"))
				removeInvalidRecords = false;

		} catch (NullPointerException e) {
			prtln("Error reading properties: " + e);
			e.printStackTrace();
		} catch (Throwable e) {
			prtln("Error reading properties: " + e);
		}
	}


	/**
	 *  Gets the fileIndexingService attribute.
	 *
	 * @return    The fileIndexingService.
	 */
	public FileIndexingService getFileIndexingService() {
		return fileIndexingService;
	}


	/**
	 *  Gets the numIdentifiersResults per resumptionToken to be returned in OAI ListIdentifiers requests.
	 *
	 * @return    The numIdentifiersResults value
	 */
	public String getNumIdentifiersResults() {
		String value = null;
		try {
			value = (String) adminData.get(Keys.NUM_IDENTIFIERS_RESULTS);
		} catch (OIDDoesNotExistException e) {
			//prtln("Could not find data for key: " + Keys.NUM_IDENTIFIERS_RESULTS);
		}
		return value;
	}


	/**
	 *  Sets the numIdentifiersResults per resumptionToken to be returned in OAI ListIdentifiers requests.
	 *
	 * @param  value  The new numIdentifiersResults value
	 */
	public void setNumIdentifiersResults(String value) {
		if (value == null)
			return;
		try {
			if (!adminData.oidExists(Keys.NUM_IDENTIFIERS_RESULTS)) {
				adminData.put(Keys.NUM_IDENTIFIERS_RESULTS, value);
			}
			else {
				adminData.update(Keys.NUM_IDENTIFIERS_RESULTS, value);
			}
		} catch (Exception e) {
			prtlnErr("Error saving serialized NUM_IDENTIFIERS_RESULTS: " + e);
		}
	}



	/**
	 *  Gets the removeInvalidRecords attribute, which is true|false.
	 *
	 * @return    true|false or null if not found.
	 */
	public String getRemoveInvalidRecords() {
		String value = null;
		try {
			value = (String) adminData.get(Keys.REMOVE_INVALID_RECORDS);
		} catch (OIDDoesNotExistException e) {
			//prtln("Could not find data for key: " + Keys.REMOVE_INVALID_RECORDS);
		}
		return value;
	}



	/**
	 *  Sets the removeInvalidRecords attribute. The input must be "true" or "false".
	 *
	 * @param  value  The String "true" or "false".
	 */
	public void setRemoveInvalidRecords(String value) {
		if (value == null)
			return;
		if (!value.equals("true") && !value.equals("false")) {
			prtlnErr("setRemoveInvalidRecords(): Error. Value must be \"true\" or \"false\", not \"" + value + "\"");
			return;
		}

		if (value.equals("true"))
			removeInvalidRecords = true;
		if (value.equals("false"))
			removeInvalidRecords = false;
		try {
			if (!adminData.oidExists(Keys.REMOVE_INVALID_RECORDS)) {
				adminData.put(Keys.REMOVE_INVALID_RECORDS, value);
			}
			else {
				adminData.update(Keys.REMOVE_INVALID_RECORDS, value);
			}
		} catch (Exception e) {
			prtlnErr("Error saving serialized REMOVE_INVALID_RECORDS: " + e);
		}
	}



	/**
	 *  Gets the getValidateRecords attribute, which is true|false.
	 *
	 * @return    true|false or null if not found.
	 */
	public String getValidateRecords() {
		String value = null;
		try {
			value = (String) adminData.get(Keys.VALIDATE_RECORDS);
		} catch (OIDDoesNotExistException e) {
			//prtln("Could not find data for key: " + Keys.VALIDATE_RECORDS);
		}
		return value;
	}



	/**
	 *  Sets the setValidateRecords attribute. The input must be "true" or "false".
	 *
	 * @param  value  The String "true" or "false".
	 */
	public void setValidateRecords(String value) {
		if (value == null)
			return;
		if (!value.equals("true") && !value.equals("false")) {
			prtlnErr("setValidateRecords(): Error. Value must be \"true\" or \"false\", not \"" + value + "\"");
			return;
		}

		if (fileIndexingService != null) {
			if (value.equals("true"))
				fileIndexingService.setValidationEnabled(true);
			else
				fileIndexingService.setValidationEnabled(false);
		}

		try {
			if (!adminData.oidExists(Keys.VALIDATE_RECORDS)) {
				adminData.put(Keys.VALIDATE_RECORDS, value);
			}
			else {
				adminData.update(Keys.VALIDATE_RECORDS, value);
			}
		} catch (Exception e) {
			prtlnErr("Error saving serialized VALIDATE_RECORDS: " + e);
		}
	}



	/**
	 *  Gets the numRecordsResults per resumptionToken to be returned in OAI ListRecords requests.
	 *
	 * @return    The numRecordsResults value
	 */
	public String getNumRecordsResults() {
		String value = null;
		try {
			value = (String) adminData.get(Keys.NUM_RECORDS_RESULTS);
		} catch (OIDDoesNotExistException e) {
			//prtln("Could not find data for key: " + Keys.NUM_RECORDS_RESULTS);
		}
		return value;
	}


	/**
	 *  Sets the numRecordsResults per resumptionToken to be returned in OAI ListRecords requests.
	 *
	 * @param  value  The new numRecordsResults value
	 */
	public void setNumRecordsResults(String value) {
		if (value == null)
			return;
		try {
			if (!adminData.oidExists(Keys.NUM_RECORDS_RESULTS)) {
				adminData.put(Keys.NUM_RECORDS_RESULTS, value);
			}
			else {
				adminData.update(Keys.NUM_RECORDS_RESULTS, value);
			}
		} catch (Exception e) {
			prtlnErr("Error saving serialized NUM_RECORDS_RESULTS: " + e);
		}
	}



	/**
	 *  Removes an object from the adminData datamanager iff it was present, else does nothing.
	 *
	 * @param  key  The object to remove.
	 */
	private void removeAdminObject(String key) {
		try {
			adminData.remove(key);
		} catch (OIDDoesNotExistException odnee) {
			// do nothing
		} catch (LockNotAvailableException lnae) {
			prtlnErr("Error: Unable to remove admin item: " + lnae);
		}
	}


	/**
	 *  Get an example ID that might be disiminated from this repository.
	 *
	 * @return    An example ID from this repository.
	 */
	public String getExampleID() {
		String value = null;
		if (index != null && index.getNumDocs() > 0) {
			ResultDocList docs = index.searchDocs("collection:0*");
			if (docs.size() > 0)
				value = ((XMLDocReader) ((ResultDoc) docs.get(0)).getDocReader()).getId();
			if (value != null && value.length() != 0) {
				return value;
			}
		}

		try {
			value = (String) adminData.get(Keys.EXAMPLE_ID);
			if (value == null || value.length() == 0) {
				return defaultExampleID;
			}
		} catch (OIDDoesNotExistException e) {
			return defaultExampleID;
		}
		return value;
	}


	/**
	 *  Gets the status of the OAI data provider.
	 *
	 * @return    The providerStatus value [ENABLED or DISABLED]
	 */
	public String getProviderStatus() {
		String value = null;
		try {
			value = (String) adminData.get(Keys.PROVIDER_STATUS);
		} catch (OIDDoesNotExistException e) {
		}
		return value;
	}


	/**
	 *  Sets the status of the OAI data provider.
	 *
	 * @param  value          The new providerStatus value [ENABLED or DISABLED]
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public void setProviderStatus(String value) throws Exception {
		if (value == null || (!value.equals("ENABLED") && !value.equals("DISABLED")))
			throw new Exception("Value must be 'ENABLED' or 'DISABLED' but '" + value + "' was found.");
		try {
			if (!adminData.oidExists(Keys.PROVIDER_STATUS)) {
				adminData.put(Keys.PROVIDER_STATUS, value);
			}
			else {
				adminData.update(Keys.PROVIDER_STATUS, value);
			}
		} catch (Exception e) {
			prtlnErr("Error saving serialized PROVIDER_STATUS: " + e);
		}
	}


	/**
	 *  Gets the harvesterStatus attribute of the RepositoryManager object
	 *
	 * @return    The harvesterStatus value [ENABLED or DISABLED]
	 */
	public String getHarvesterStatus() {
		String value = null;
		try {
			value = (String) adminData.get(Keys.HARVESTER_STATUS);
		} catch (OIDDoesNotExistException e) {
		}
		return value;
	}


	/**
	 *  Sets the harvesterStatus attribute of the RepositoryManager object
	 *
	 * @param  value  The new harvesterStatus value [ENABLED or DISABLED]
	 */
	public void setHarvesterStatus(String value) {
		if (value == null)
			return;
		try {
			if (!adminData.oidExists(Keys.HARVESTER_STATUS)) {
				adminData.put(Keys.HARVESTER_STATUS, value);
			}
			else {
				adminData.update(Keys.HARVESTER_STATUS, value);
			}
		} catch (Exception e) {
			prtlnErr("Error saving serialized HARVESTER_STATUS: " + e);
		}
	}


	/**
	 *  Gets the repositoryIdentifier for this provider, for example 'dlese.org'. This equates to the
	 *  namespace-identifier portion of the OAI Identifier Format. An example namespace-identifier is
	 *  'dlese.org'. See <a href="http://www.openarchives.org/OAI/2.0/guidelines-oai-identifier.htm"> OAI
	 *  Identifier Format</a> for information.
	 *
	 * @return    The repositoryIdentifier, which is the namespace-identifier portion of the OAI identifier
	 *      format.
	 */
	public String getRepositoryIdentifier() {
		String value = null;
		try {
			value = (String) adminData.get(Keys.REPOSITORY_IDENTIFIER);
		} catch (OIDDoesNotExistException e) {
		}
		return value;
	}


	/**
	 *  Gets the OAI Identifier Format's scheme and namespace-identifier prefix, for example 'oai:dlese.org:'.
	 *  This prefix should be appended to the beginning of the local-identifier to create the fully-qualified OAI
	 *  Identifier for a given item in the repository. See <a href="http://www.openarchives.org/OAI/2.0/guidelines-oai-identifier.htm">
	 *  OAI Identifier Format</a> for information.
	 *
	 * @return    The OAI Identifier Format's scheme and namespace-identifier prefix, for example
	 *      'oai:dlese.org:'.
	 */
	public String getOaiIdPrefix() {
		String repId = getRepositoryIdentifier();
		if (repId == null || repId.length() == 0)
			return "";
		else
			return ("oai:" + repId + ":");
	}


	/**
	 *  Sets the oaiIdentifier attribute of the RepositoryManager object
	 *
	 * @param  value  The new oaiIdentifier value
	 */
	public void setRepositoryIdentifier(String value) {
		if (value == null)
			return;
		try {
			if (!adminData.oidExists(Keys.REPOSITORY_IDENTIFIER)) {
				adminData.put(Keys.REPOSITORY_IDENTIFIER, value);
			}
			else {
				adminData.update(Keys.REPOSITORY_IDENTIFIER, value);
			}
		} catch (Exception e) {
			prtlnErr("Error saving serialized REPOSITORY_IDENTIFIER: " + e);
		}
	}



	/**
	 *  Gets the protocolVersion attribute of the RepositoryManager object
	 *
	 * @return    The protocolVersion value
	 */
	public String getProtocolVersion() {
		String value = null;
		try {
			value = (String) adminData.get(Keys.PROTOCOL_VERSION);
		} catch (OIDDoesNotExistException e) {
		}
		return value;
	}


	/**
	 *  Sets the protocolVersion attribute of the RepositoryManager object
	 *
	 * @param  value  The new protocolVersion value
	 */
	private void setProtocolVersion(String value) {
		if (value == null)
			return;
		try {
			if (!adminData.oidExists(Keys.PROTOCOL_VERSION)) {
				adminData.put(Keys.PROTOCOL_VERSION, value);
			}
			else {
				adminData.update(Keys.PROTOCOL_VERSION, value);
			}
		} catch (Exception e) {
			prtlnErr("Error saving serialized PROTOCOL_VERSION: " + e);
		}
	}


	/**
	 *  Gets the repositoryName attribute of the RepositoryManager object
	 *
	 * @return    The repositoryName value
	 */
	public String getRepositoryName() {
		String value = null;
		try {
			value = (String) adminData.get(Keys.REPOSITORY_NAME);
		} catch (OIDDoesNotExistException e) {
		}
		return value;
	}


	/**
	 *  Sets the repositoryName attribute of the RepositoryManager object
	 *
	 * @param  value  The new repositoryName value
	 */
	public void setRepositoryName(String value) {
		if (value == null)
			return;
		try {
			if (!adminData.oidExists(Keys.REPOSITORY_NAME)) {
				adminData.put(Keys.REPOSITORY_NAME, value);
			}
			else {
				adminData.update(Keys.REPOSITORY_NAME, value);
			}
		} catch (Exception e) {
			prtlnErr("Error saving serialized REPOSITORY_NAME: " + e);
		}
	}



	/**
	 *  Gets the earliestDatestamp attribute of the RepositoryManager object
	 *
	 * @return    The earliestDatestamp value
	 */
	public String getEarliestDatestamp() {
		String value = null;
		try {
			value = (String) adminData.get(Keys.EARLIEST_DATESTAMP);
		} catch (OIDDoesNotExistException e) {
		}
		return value;
	}


	/**
	 *  Sets the earliestDatestamp attribute of the RepositoryManager object
	 *
	 * @param  value  The new earliestDatestamp value
	 */
	private void setEarliestDatestamp(String value) {
		if (value == null)
			return;
		try {
			if (!adminData.oidExists(Keys.EARLIEST_DATESTAMP)) {
				adminData.put(Keys.EARLIEST_DATESTAMP, value);
			}
			else {
				adminData.update(Keys.EARLIEST_DATESTAMP, value);
			}
		} catch (Exception e) {
			prtlnErr("Error saving serialized EARLIEST_DATESTAMP: " + e);
		}
	}


	/**
	 *  Gets the OAI-PMH deletedRecord support, which depends on whether deleted records are removed from the
	 *  repository index or kept as status deleted. Valid responses include "no" (deletions not keept or
	 *  advertised) or "transient" (deletions are kept and advertised, unless the repository index is reset).
	 *
	 * @return    The String "no" or "transient"
	 */
	public String getDeletedRecord() {
		// If removing deletions from the index, return "no" per OAI-PMH
		if (removeDocs)
			return "no";
		// If keeping deletions in the index, return "transient" per OAI-PMH
		else
			return "transient";
	}


	/**
	 *  Gets the granularity attribute of the RepositoryManager object. Legitimate values are YYYY-MM-DD and
	 *  YYYY-MM-DDThh:mm:ssZ with meanings as defined in ISO8601.
	 *
	 * @return    The granularity value
	 */
	public String getGranularity() {
		String value = null;
		try {
			value = (String) adminData.get(Keys.GRANULARITY);
		} catch (OIDDoesNotExistException e) {
		}
		return value;
	}


	/**
	 *  Sets the granularity attribute of the RepositoryManager object. Legitimate values are YYYY-MM-DD and
	 *  YYYY-MM-DDThh:mm:ssZ with meanings as defined in ISO8601.
	 *
	 * @param  value  The new granularity value
	 */
	public void setGranularity(String value) {
		if (value == null)
			return;
		try {
			if (!adminData.oidExists(Keys.GRANULARITY)) {
				adminData.put(Keys.GRANULARITY, value);
			}
			else {
				adminData.update(Keys.GRANULARITY, value);
			}
		} catch (Exception e) {
			prtlnErr("Error saving serialized GRANULARITY: " + e);
		}
	}


	/**
	 *  Sets the end portion of the OAI baseURL that is being used, for example 'provider'.
	 *
	 * @param  baseUrlEnding  The new providerBaseUrlEnding value
	 */
	public void setProviderBaseUrlEnding(String baseUrlEnding) {
		if (baseUrlEnding.startsWith("/"))
			dataProviderBaseUrlPathEnding = baseUrlEnding;
		else
			dataProviderBaseUrlPathEnding = "/" + baseUrlEnding;
	}


	/**
	 *  Sets the serverUrl (scheme, hostname and port) displayed in the OAI data provider baseUrl and elsewhere.
	 *  Example values: 'http://www.example.com' or 'http://www.example.com:8080'. If set to null (default), then
	 *  the scheme, hostname and port number will be determined automatically by examining the URL that was
	 *  requested by the client.
	 *
	 * @param  serverUrl  The new serverUrl value
	 */
	public void setServerUrl(String serverUrl) {
		this.serverUrl = serverUrl;
	}


	/**
	 *  Gets the Base URL that refers to the current OAI provider, for example
	 *  "http://host:8080/my_oai_context/provider".
	 *
	 * @param  req  The request.
	 * @return      The providerBaseUrl value.
	 */
	public String getProviderBaseUrl(HttpServletRequest req) {
		if (serverUrl == null || serverUrl.length() == 0)
			return GeneralServletTools.getContextUrl(req) + dataProviderBaseUrlPathEnding;
		else
			return serverUrl + req.getContextPath() + dataProviderBaseUrlPathEnding;
	}


	/**
	 *  Gets the compressions attribute of the RepositoryManager object.
	 *
	 * @return    The compressions value
	 */
	public ArrayList getCompressions() {
		return arrayListGet(Keys.COMPRESSIONS, false);
	}


	/**
	 *  Adds a feature to the Compression attribute of the RepositoryManager object. "none" indicates no
	 *  compression is supported. Others: gzip ; compress.
	 *
	 * @param  value  The feature to be added to the Compression attribute
	 */
	public void addCompression(String value) {
		arrayListAddItem(value, Keys.COMPRESSIONS);
	}


	/**
	 *  Removes the given compression value.
	 *
	 * @param  value  The compression value to remove.
	 */
	public void removeCompression(String value) {
		arrayListRemoveItem(value, Keys.COMPRESSIONS);
	}


	/**
	 *  Removes the given compression item.
	 *
	 * @param  i  The compression ArrayList item to remove.
	 */
	public void removeCompression(int i) {
		arrayListRemoveItem(i, Keys.COMPRESSIONS);
	}


	/**
	 *  Gets the descriptions availalable for this repository.
	 *
	 * @return    The descriptions value
	 */
	public ArrayList getDescriptions() {
		return arrayListGet(Keys.DESCRIPTIONS, false);
	}


	/**
	 *  Gets the description attribute of the RepositoryManager object
	 *
	 * @param  i  The index of the description to remove
	 * @return    The description value
	 */
	public String getDescription(int i) {
		return ((String) arrayListGetItem(i, Keys.DESCRIPTIONS, false));
	}


	/**
	 *  Adds a feature to the Description attribute of the RepositoryManager object
	 *
	 * @param  value  The feature to be added to the Description attribute
	 */
	public void addDescription(String value) {
		if (value != null && value.trim().length() > 0)
			arrayListAddItem(value, Keys.DESCRIPTIONS);
	}


	/**
	 *  Add the given admin Description to the repository. The aDescription is used in the Identify request. If
	 *  the given Description already exists, nothing is changed.
	 *
	 * @param  index  The index.
	 * @param  value  The new description
	 */
	public void replaceDescription(int index, String value) {
		arrayListReplaceItem(index, value, Keys.DESCRIPTIONS);
	}


	/**
	 *  Remove the given desctiption from the repository.
	 *
	 * @param  value  The description to remove.
	 */
	public void removeDescription(String value) {
		arrayListRemoveItem(value, Keys.DESCRIPTIONS);
	}


	/**
	 *  Remove the given descrtiption item from the repository.
	 *
	 * @param  i  The description ArrayList index to remove.
	 */
	public void removeDescription(int i) {
		arrayListRemoveItem(i, Keys.DESCRIPTIONS);
	}


	private String[] trustedWsIpsArray = null;


	/**
	 *  Gets an array of IP address regular expressions that are trusted for access to non-discoverable items
	 *  through the web service. The regular expressions may be used directly in the String.matches() method. For
	 *  example:<p>
	 *
	 *  String IP = "128.66.45.88";<br>
	 *  boolean isAuthorized = IP.matches(getTrustedWsIpsArray()[0]);
	 *
	 * @return    array of IP address regular expressions.
	 */
	public String[] getTrustedWsIpsArray() {
		if (trustedWsIpsArray == null) {
			String trustedWsIps = this.getTrustedWsIps();
			if (trustedWsIps == null || trustedWsIps.trim().length() == 0)
				return null;
			trustedWsIpsArray = trustedWsIps.split(",");

			// Convert to an appropriate regular expression (assumes numbers, periods and * wildcard only):
			for (int i = 0; i < trustedWsIpsArray.length; i++) {
				trustedWsIpsArray[i] = trustedWsIpsArray[i].replaceAll("\\.", "\\\\.").replaceAll("\\*", ".*").trim();
			}
		}
		return trustedWsIpsArray;
	}


	/**
	 *  Gets a Comma-separated String of IP address regular expressions that are trusted for access to
	 *  non-discoverable items through the web service.
	 *
	 * @return    Comma-separated String of IP address regular expressions.
	 */
	public String getTrustedWsIps() {
		String value = null;
		try {
			value = (String) adminData.get(Keys.TRUSTED_WS_IPS);
		} catch (OIDDoesNotExistException e) {
			//prtln("Could not find data for key: " + Keys.VALIDATE_RECORDS);
		}
		return value;
	}


	/**
	 *  Sets the trustedWsIps attribute of the RepositoryManager, which should contain a comma-separated list of
	 *  IP addresses for IPs that may access non-discoverable items through the web service. These may contain
	 *  numbers, periods and the * wildcard only, for example 128.166.26.*.
	 *
	 * @param  value  The new trustedWsIps value
	 */
	public void setTrustedWsIps(String value) {
		if (value == null)
			return;
		try {
			if (!adminData.oidExists(Keys.TRUSTED_WS_IPS)) {
				adminData.put(Keys.TRUSTED_WS_IPS, value);
			}
			else {
				adminData.update(Keys.TRUSTED_WS_IPS, value);
			}
		} catch (Exception e) {
			prtlnErr("Error saving serialized TRUSTED_WS_IPS: " + e);
		}
		// Reset so the array will be re-built...
		trustedWsIpsArray = null;
	}


	/**
	 *  Gets a single record from the repository by ID.
	 *
	 * @param  id  The ID for the item
	 * @return     The record or null if not available
	 */
	public ResultDoc getRecord(String id) {
		if (id == null || id.trim().length() == 0)
			return null;
		ResultDocList resultDocs;
		if (removeInvalidRecords)
			resultDocs = index.searchDocs("id:" + SimpleLuceneIndex.encodeToTerm(id) + " AND !valid:false");
		else
			resultDocs = index.searchDocs("id:" + SimpleLuceneIndex.encodeToTerm(id));

		if (resultDocs == null) {
			return null;
		}

		if (resultDocs.size() > 1) {
			prtlnErr("Error: more than one item in index for id '" + id + "'");
		}
		if (resultDocs.size() > 0) {
			return resultDocs.get(0);
		}
		else {
			return null;
		}
	}


	/**
	 *  Gets a Lucene Query for all disabled sets, or null if none.
	 *
	 * @return    The disabledSetsQuery
	 */
	public Query getDisabledSetsQuery() {
		ArrayList setList = getDisabledSets();
		if (setList == null || setList.size() == 0)
			return null;

		BooleanQuery bq = new BooleanQuery();
		for (int i = 0; i < setList.size(); i++) {
			bq.add(new TermQuery(new Term("collection", "0" + setList.get(i))), BooleanClause.Occur.MUST);
		}
		return bq;
	}


	/**
	 *  Gets a single record from the repository by ID, restricted to those records that are
	 *  avaiable/discoverable for OAI.
	 *
	 * @param  id  The ID for the item
	 * @return     The record or null if not available
	 * @see        #setOaiFilterQuery
	 * @see        #getOaiQueryResults(String format,String set,String from,String until)
	 */
	public ResultDoc getRecordOai(String id) {
		if (id == null || id.trim().length() == 0)
			return null;
		ResultDocList resultDocs;
		Query fullQuery;
		try {
			if (removeInvalidRecords)
				fullQuery = getIndex().getQueryParser().parse("id:" + SimpleLuceneIndex.encodeToTerm(id) + " AND !valid:false" + " AND (" + getDiscoverableRecordsQuery() + ")");
			else
				fullQuery = getIndex().getQueryParser().parse("id:" + SimpleLuceneIndex.encodeToTerm(id) + " AND (" + getDiscoverableRecordsQuery() + ")");
		} catch (Throwable t) {
			prtlnErr("Error in getRecord(): " + t);
			return null;
		}

		// Filter out records, if oaiFilterQuery is defined:
		if (oaiFilterQuery != null) {
			BooleanQuery bq = new BooleanQuery();
			bq.add(fullQuery, BooleanClause.Occur.MUST);
			bq.add(oaiFilterQuery, BooleanClause.Occur.MUST_NOT);
			fullQuery = bq;
		}

		// prtln("getRecordOai() query: " + fullQuery);

		resultDocs = getIndex().searchDocs(fullQuery);

		if (resultDocs == null) {
			return null;
		}

		if (resultDocs.size() > 1) {
			prtlnErr("Error: more than one item in index for id '" + id + "'");
		}
		if (resultDocs.size() > 0) {
			return resultDocs.get(0);
		}
		else {
			return null;
		}
	}


	/**
	 *  Gets the results from a standard OAI-PMH ListIdentifiers or ListRecords query or an ODL search request.
	 *
	 * @param  format         The metadata format.
	 * @param  set            The set, or null.
	 * @param  from           The from time, or null.
	 * @param  until          The until time, or null.
	 * @return                The queryResults value.
	 * @exception  Exception  If error occues, exception will contain a message
	 * @see                   #getRecord(String id)
	 */
	public ResultDocList getOaiQueryResults(
	                                      String format,
	                                      String set,
	                                      String from,
	                                      String until)
		 throws Exception {
		//prtln("getOaiQueryResults(): format: " + format + " set: " + set + " from: " + from + " until: " + until);
		return getOdlQueryResults(format, set, from, until, null);
	}


	/**
	 *  Gets the results of an OAI-PMH ListRecords or ListIdentifiers request or an OLD search. Set queryString
	 *  to null to perform a standard OAI-PMH search and non-null for ODL search.
	 *
	 * @param  format         The metadata format.
	 * @param  set            The set to search over, or null.
	 * @param  from           The from time, or null.
	 * @param  until          The until time, or null.
	 * @param  queryString    Must be non-null to indicate ODL search and null to indicate regular OAI-PMH
	 *      request.
	 * @return                The queryResults value.
	 * @exception  Exception  If error occues, exception will contain a message meant for human consumption in
	 *      response error messages.
	 * @see                   #setOaiFilterQuery
	 */
	public ResultDocList getOdlQueryResults(
	                                      String format,
	                                      String set,
	                                      String from,
	                                      String until,
	                                      String queryString)
		 throws Exception {

		//if (queryString != null)
		//prtln("getOdlQueryResults(): format: " + format + " set: " + set + " from: " + from + " until: " + until + " query: " + queryString);

		// Standard OAI-PMH request (indicated by empty query):
		if (queryString == null) {
			// If this repsository is not allowed to support regular OAI-PMH requests, return null
			if (!isOaiPmhEnabled)
				return null;
			// Set the query string to all records, boosting non-deletes to the top:
			else
				queryString = "deleted:false^10 OR deleted:true";
		}
		// If empty ODL search, return null
		else if (queryString.trim().length() == 0)
			return null;
		// Star maps to a query that pulls up all records in the repository
		else if (queryString.trim().equals("*"))
			queryString = "deleted:false";
		// Remove all deleted records from ODL searches:
		else
			queryString = "(" + queryString + ") AND deleted:false";

		// If the requested format is not available, return null
		String convertableFormatsQuery = getConvertableFormatsQuery(format);
		if (convertableFormatsQuery == null)
			return null;

		// Handle date ranges:
		DateRangeFilter dateFilter = null;
		try {
			if (from != null && until == null) {
				dateFilter = DateRangeFilter.After("oaimodtime", OAIUtils.getDateFromDatestamp(from, 0));
			}
			else if (until != null && from == null) {
				dateFilter = DateRangeFilter.Before("oaimodtime", OAIUtils.getDateFromDatestamp(until, 0));
			}
			else if (until != null && from != null) {
				// If the times are the same, increment the 'until' time so that the result will match records with that exact datestamp.
				if (until.toLowerCase().equals(from.toLowerCase()))
					dateFilter = new DateRangeFilter("oaimodtime", OAIUtils.getDateFromDatestamp(from, 0), OAIUtils.getDateFromDatestamp(until, 1));
				else
					dateFilter = new DateRangeFilter("oaimodtime", OAIUtils.getDateFromDatestamp(from, 0), OAIUtils.getDateFromDatestamp(until, 0));
				if (until.length() != from.length())
					throw new OAIErrorException("Invalid date: 'from' and 'until' date arguments must be of the same granularity.", OAICodes.BAD_ARGUMENT);
			}
		} catch (ParseException e) {
			throw new OAIErrorException("Unable to parse date argument. Dates must be of the form 'YYYY-MM-DD' or '" +
				getGranularity() + ".' " + e.getMessage(), OAICodes.BAD_ARGUMENT);
		}

		// Construct the query for this ODL or OAI-PMH ListRecords or ListIdentifiers request
		String query = "(" + queryString + ")"
			 + " AND " + convertableFormatsQuery;
		if (removeInvalidRecords)
			query += " AND !valid:false";

		//prtln("Final Query: " + query);

		// Use the expansion query to apply boosting, stemming, virtual fields etc.
		Query fullQuery = getExpandedSearchQuery(query);

		// Remove any disabled file sets and other non-discoverable records:
		Query discoverableOaiRecordsQuery = getDiscoverableOaiRecordsQuery();
		if (discoverableOaiRecordsQuery != null) {
			BooleanQuery bq = new BooleanQuery();
			bq.add(fullQuery, BooleanClause.Occur.MUST);
			bq.add(discoverableOaiRecordsQuery, BooleanClause.Occur.MUST);
			fullQuery = bq;
		}

		//prtln("getOdlQueryResults(): set is: " + set);

		// Add a restriction for sets if requested
		if (set != null) {
			Query setQuery = null;
			if (virtualSearchFieldMapper != null)
				setQuery = virtualSearchFieldMapper.getQuery("setSpec", set);

			// If the requested set does not exist, nothing should be returned
			if (setQuery == null)
				return null;
			else {
				BooleanQuery bq = new BooleanQuery();
				bq.add(fullQuery, BooleanClause.Occur.MUST);
				bq.add(setQuery, BooleanClause.Occur.MUST);
				fullQuery = bq;
			}
		}

		// Filter out records, if oaiFilterQuery is defined:
		if (oaiFilterQuery != null) {
			BooleanQuery bq = new BooleanQuery();
			bq.add(fullQuery, BooleanClause.Occur.MUST);
			bq.add(oaiFilterQuery, BooleanClause.Occur.MUST_NOT);
			fullQuery = bq;
		}

		ResultDocList results = index.searchDocs(fullQuery, dateFilter);
		int num = 0;
		if (results != null)
			num = results.size();

		//prtln("getOdlQueryResults(): num results: " + num + " query is '" + fullQuery + "'");

		return results;
	}


	/**
	 *  Sets a Lucene query that filters the records served by the OAI data provider. Records that match this
	 *  query will not be served by the OAI data provider. The given filter query persists until this method is
	 *  called again or the RepositoryManager is reinstantiated. Pass null to this method to remove the filter
	 *  query.
	 *
	 * @param  luceneQuery                                       The Lucene query to filter records
	 * @exception  org.apache.lucene.queryParser.ParseException  If query parse error
	 */
	public void setOaiFilterQuery(String luceneQuery)
		 throws org.apache.lucene.queryParser.ParseException {
		if (luceneQuery == null) {
			oaiFilterQuery = null;
			return;
		}
		oaiFilterQuery = getIndex().getQueryParser().parse(luceneQuery);
	}


	private Query getFullOaiFilterQuery(String format) {

		// If the requested format is not available, return null
		String convertableFormatsQuery = getConvertableFormatsQuery(format);
		if (convertableFormatsQuery == null)
			return null;

		String query = getDiscoverableRecordsQuery()
			 + " AND " + convertableFormatsQuery;
		if (removeInvalidRecords)
			query += " AND !valid:false";
		return null;
	}


	private List indexedFormats = null;
	private long indexedFormatsUpdatedTime = -1;


	/**
	 *  Gets all formats that can be converted to the given Format.
	 *
	 * @param  format  The target format.
	 * @return         The formatsThatCanBeConvertedToFormat list.
	 */
	public ArrayList getFormatsThatCanBeConvertedToFormat(String format) {
		ArrayList formats = new ArrayList();
		try {
			List indexedFormats = this.getIndexedFormats();
			if (indexedFormats == null)
				return formats;
			for (int i = 0; i < indexedFormats.size(); i++) {
				String indexedFormat = (String) indexedFormats.get(i);
				// remove the leading '0'
				indexedFormat = indexedFormat.substring(1, indexedFormat.length());
				if (xmlConversionService.canConvert(indexedFormat, format))
					formats.add(indexedFormat);
			}
			return formats;
		} catch (Throwable e) {
			prtlnErr("getFormatsThatCanBeConvertedToFormat() error: " + e);
			e.printStackTrace();
			return formats;
		}
	}


	/**
	 *  Gets all formats that can be converted to the given Formats.
	 *
	 * @param  formats  Formats that we want converted to.
	 * @return          The formatsThatCanBeConvertedToFormats list.
	 */
	public ArrayList getFormatsThatCanBeConvertedToFormats(String[] formats) {
		ArrayList fmts = new ArrayList();
		if (formats == null)
			return fmts;
		List tmp;
		String fmt;
		for (int i = 0; i < formats.length; i++) {
			tmp = getFormatsThatCanBeConvertedToFormat(formats[i]);
			for (int j = 0; j < tmp.size(); j++) {
				fmt = (String) tmp.get(j);
				if (!fmts.contains(fmt))
					fmts.add(fmt);
			}
		}
		return fmts;
	}


	/**
	 *  Gets a query that limits a search to only those xml formats that can be converted to the format
	 *  indicated. The conversion is done using the XmlConversionService.
	 *
	 * @param  toFormat  The xml format to convert to
	 * @return           A query that will return only records that can be converted to the given format, or null
	 *      if no convertable formats are avialable.
	 */
	public final String getConvertableFormatsQuery(String toFormat) {
		//prtln("getConvertableFormatsQuery() foFormat: " + toFormat);
		if (toFormat == null)
			return null;
		ArrayList convertableFormats = getFormatsThatCanBeConvertedToFormat(toFormat);
		if (convertableFormats.size() <= 0)
			return null;
		String query = "(xmlFormat:" + convertableFormats.get(0);
		for (int i = 1; i < convertableFormats.size(); i++)
			query += " OR xmlFormat:" + convertableFormats.get(i);
		query += ")";

		//prtln("getConvertableFormatsQuery() query: " + query);
		return query;
	}


	long indexedSetsUpdateCount = -1;
	ArrayList indexedSets = new ArrayList();


	/**
	 *  Gets the sets that are in the index.
	 *
	 * @return    A list of set setSecs.
	 * @see       #getSetSearchKeys()
	 */
	public ArrayList getIndexedSets() {
		if (index.getLastModifiedCount() > indexedSetsUpdateCount) {
			indexedSetsUpdateCount = index.getLastModifiedCount();

			List cols = index.getTerms("collection");
			indexedSets.clear();
			if (cols != null) {
				String col;
				for (int i = 0; i < cols.size(); i++) {
					col = (String) cols.get(i);
					// remove leading '0'
					indexedSets.add(col.substring(1, col.length()));
				}
			}
		}
		return indexedSets;
	}


	List setSearchKeys = new ArrayList();
	long setSearchKeysLastUpdate = -1;


	/**
	 *  Gets a list of all search keys used to search for sets.
	 *
	 * @return    The setSearchKeys value.
	 * @see       #getIndexedSets()
	 */
	public List getSetSearchKeys() {
		if (index.getLastModifiedCount() > setSearchKeysLastUpdate) {
			setSearchKeysLastUpdate = index.getLastModifiedCount();
			setSearchKeys = index.getTerms("collection");
		}
		return setSearchKeys;
	}


	List indexedAccessionStatus = new ArrayList();
	long indexedAccessionStatusLastUpdate = -1;


	/**
	 *  Gets a list of all accession statusus in the index.
	 *
	 * @return    All accession statusus in the index.
	 */
	public List getIndexedAccessionStatuses() {
		if (index.getLastModifiedCount() > indexedAccessionStatusLastUpdate) {
			indexedAccessionStatusLastUpdate = index.getLastModifiedCount();
			indexedAccessionStatus = index.getTerms("accessionstatus");
		}
		return indexedAccessionStatus;
	}


	List indexedIdMapperErrors = new ArrayList();
	long indexedIdMapperErrorsLastUpdate = -1;


	/**
	 *  Gets a list of all ID mapper errors that have been indexed.
	 *
	 * @return    The available ID mapper errors, as integer strings.
	 */
	public List getIndexedIdMapperErrors() {
		if (index.getLastModifiedCount() > indexedIdMapperErrorsLastUpdate) {
			indexedIdMapperErrorsLastUpdate = index.getLastModifiedCount();
			indexedIdMapperErrors = index.getTerms("idmaperrors");
			if (indexedIdMapperErrors.contains("noerrors") && indexedIdMapperErrors.size() > 1)
				indexedIdMapperErrors.add(0, indexedIdMapperErrors.remove(indexedIdMapperErrors.indexOf("noerrors")));
		}
		return indexedIdMapperErrors;
	}


	/**
	 *  Determines whether the given ID is in the repository. Note that if given document was indexed but is not
	 *  valid, this method MAY return false.
	 *
	 * @param  id  An ID.
	 * @return     True if the given ID is in the repository.
	 */
	public boolean isIdInRepository(String id) {
		List doc = index.listDocs("id", SimpleLuceneIndex.encodeToTerm(id));
		if (doc == null || doc.size() == 0)
			return false;
		if (removeInvalidRecords) {
			XMLDocReader rdr = new XMLDocReader((Document) doc.get(0));
			return rdr.isValid();
		}
		else
			return true;
	}


	/**
	 *  Determines whether the given set is in the repository index. Note that if the set is in the index it may
	 *  also be disabled or all records in the set may have been moved to status deleted, if the set was removed
	 *  from the repository.
	 *
	 * @param  set  A set.
	 * @return      True if the the given set is in the repository index.
	 * @see         #isSetConfigured(String set)
	 */
	public boolean isSetInIndex(String set) {
		List doc = index.listDocs("collection", set);
		return (doc.size() > 0);
	}


	/**
	 *  Determines whether this repository can disseminate the given format.
	 *
	 * @param  format  The format.
	 * @return         True if this repository can dissiminate the format.
	 */
	public boolean canDisseminateFormat(String format) {
		return (getAvailableFormats().containsKey(format));
	}


	Hashtable formats = new Hashtable();
	long formatsLastUpdatedTime = -1;


	/**
	 *  Gets all possible metadata formats that may be disiminated by this RepositoryManager. This includes
	 *  formats that are available by conversion from the native format via the XMLConversionService.
	 *
	 * @return    The metadataFormats available.
	 * @see       #getConfiguredFormats
	 */
	public final Hashtable getAvailableFormats() {
		if (index.getLastModifiedCount() > formatsLastUpdatedTime) {
			formatsLastUpdatedTime = index.getLastModifiedCount();

			formats.clear();
			List indexedFormats = index.getTerms("metadatapfx");
			if (indexedFormats == null)
				return formats;
			String format = null;
			for (int i = 0; i < indexedFormats.size(); i++) {
				format = (String) indexedFormats.get(i);
				// remove the '0' in the doctype
				format = format.substring(1, format.length());
				formats.putAll(getMetadataFormatsConversions(format));
			}
		}
		return formats;
	}



	/**
	 *  Gets all possible metadata formats that are available for a given ID.
	 *
	 * @param  id  The id for the record.
	 * @return     The metadataFormats available.
	 */
	public List getAvailableFormatsList(String id) {
		Hashtable formats = getAvailableFormats(id);
		List fmts = new ArrayList(formats.size());
		Enumeration enumeration = formats.keys();

		while (enumeration.hasMoreElements())
			fmts.add(enumeration.nextElement());
		return fmts;
	}


	/**
	 *  Gets all possible metadata formats that may be disiminated by this RepositoryManager. This includes
	 *  formats that are available via the XMLConversionService.
	 *
	 * @return    The metadataFormats available.
	 */
	public List getAvailableFormatsList() {
		Hashtable formats = getAvailableFormats();
		List fmts = new ArrayList(formats.size());
		Enumeration enumeration = formats.keys();

		while (enumeration.hasMoreElements())
			fmts.add(enumeration.nextElement());
		return fmts;
	}


	/**
	 *  Gets all formats that exist natively in the index. If none exist, and empty list is returned.
	 *
	 * @return    The indexedFormats value or an empty list.
	 */
	public List getIndexedFormats() {
		if (index.getLastModifiedCount() > indexedFormatsUpdatedTime) {
			indexedFormatsUpdatedTime = index.getLastModifiedCount();
			indexedFormats = index.getTerms("metadatapfx");
		}
		if (indexedFormats == null)
			return new ArrayList();
		return indexedFormats;
	}


	/**
	 *  Gets the metadata format conversions that are available for the given format, including the given format.
	 *
	 * @param  fromFormat  The initial format.
	 * @return             All formats that can be derived from the given format.
	 */
	private final Hashtable getMetadataFormatsConversions(String fromFormat) {
		// To do: implement placement of schema and namespace info
		Hashtable formats = new Hashtable();
		if (xmlConversionService == null || fromFormat == null)
			return formats;
		ArrayList availableFormats =
			xmlConversionService.getAvailableFormats(fromFormat);
		for (int j = 0; j < availableFormats.size(); j++) {
			formats.put(availableFormats.get(j),
				new MetadataFormatInfo((String) availableFormats.get(j), "", ""));
		}
		return formats;
	}


	/**
	 *  Gets all possible metadata formats that are available for a given ID.
	 *
	 * @param  id  The id for the record.
	 * @return     The metadataFormats available.
	 */
	public Hashtable getAvailableFormats(String id) {
		ResultDocList resultDocs = index.searchDocs("id:" + SimpleLuceneIndex.encodeToTerm(id));

		if (resultDocs == null)
			return new Hashtable();
		if (resultDocs.size() > 1)
			prtlnErr("Warning: more than one item in index for id '" + id + "'");
		if (resultDocs.size() > 0) {
			String nativeFormat = (String) ((FileIndexingServiceDocReader) resultDocs.get(0).getDocReader()).getDoctype();
			return getMetadataFormatsConversions(nativeFormat);
		}
		return new Hashtable();
	}


	/**
	 *  Determines whether the given set is currently configured in this repository.
	 *
	 * @param  set  The set key, for example 'dcc'
	 * @return      True if the given set is currently configured in this repository.
	 * @see         #isSetInIndex(String set)
	 */
	public boolean isSetConfigured(String set) {
		List cs = getConfiguredSets();
		return (cs != null && set != null && cs.contains(set.trim()));
	}


	/**
	 *  Determines whether the given directory is configured in the repository. Note: Each directory should only
	 *  be configured in the repository once. This method can be used to check prior to adding a directory.
	 *
	 * @param  sourceDirectory  A directory of metadata files.
	 * @return                  True if this directory is configured, false otherwise.
	 */
	public boolean isDirectoryConfigured(File sourceDirectory) {
		return fileIndexingService.isDirectoryConfigured(sourceDirectory);
	}


	/**
	 *  Determines whether the given set is currently enabled in this repository.
	 *
	 * @param  set  The name of the set or, if the vocab mgr is being used, the name of the encoded set key.
	 * @return      True if the given set is currently configured in this repository and is enabled.
	 */
	public boolean isSetEnabled(String set) {
		return getEnabledSetsHashMap().containsKey(set.trim());
	}


	/**
	 *  Determines whether the given set is currently disabled in this repository.
	 *
	 * @param  set  The name of the set or, if the vocab mgr is being used, the name of the encoded set key.
	 * @return      True if the given set is currently configured in this repository and is disabled.
	 */
	public boolean isSetDisabled(String set) {
		return getDisabledSetsHashMap().containsKey(set.trim());
	}


	/**
	 *  Gets a List of file set key Strings such as 'dcc' or 'comet' that are currently configured in this
	 *  repository.
	 *
	 * @return    The configuredSets key values, for example 'dcc', 'comet'
	 */
	public ArrayList getConfiguredSets() {
		if (configuredSets == null) {
			synchronized (setInfosLock) {
				configuredSets = new ArrayList();
				ArrayList setInfos = getSetInfos();
				if (setInfos != null)
					for (int i = 0; i < setInfos.size(); i++)
						configuredSets.add(((SetInfo) setInfos.get(i)).getSetSpec());
			}
		}

		/* String sets = "";
		for(int i = 0; i < configuredSets.size(); i++)
			sets += configuredSets.get(i) + " "; */
		return (ArrayList) configuredSets.clone();
	}


	/**
	 *  Gets a HashMap of SetInfos (directories of files) that are currently configured in this repository keyed
	 *  by their set spec, for example dcc. If non, then empty Map is returned.
	 *
	 * @return    The configuredSetInfos
	 */
	public HashMap getConfiguredSetInfos() {
		if (configuredSetInfosHashMap == null) {
			synchronized (setInfosLock) {
				ArrayList setInfos = getSetInfos();
				if (setInfos != null && setInfos.size() > 0) {
					configuredSetInfosHashMap = new HashMap(setInfos.size());
					for (int i = 0; i < setInfos.size(); i++) {
						SetInfo setInfo = (SetInfo) setInfos.get(i);
						configuredSetInfosHashMap.put(setInfo.getSetSpec(), setInfo);
					}
				}
			}
		}
		if (configuredSetInfosHashMap == null)
			return new HashMap();
		return (HashMap) configuredSetInfosHashMap.clone();
	}


	/**
	 *  Gets a List of native file format Strings that are currently configured in this repository. This does not
	 *  include formats that may be converted from the native formats via the XMLConversionService.
	 *
	 * @return    The file formats in the repository.
	 * @see       #getAvailableFormats
	 */
	public ArrayList getConfiguredFormats() {
		if (configuredFormats == null) {
			synchronized (setInfosLock) {
				configuredFormats = new ArrayList();
				ArrayList setInfos = getSetInfos();
				if (setInfos != null) {
					for (int i = 0; i < setInfos.size(); i++) {
						String format = ((SetInfo) setInfos.get(i)).getFormat();
						if (!configuredFormats.contains(format))
							configuredFormats.add(format);
					}
				}
			}
		}
		return (ArrayList) configuredFormats.clone();
	}


	/**
	 *  Gets the configured sets (directories of files) that are currently enabled in this repository. If the
	 *  vacab manager is being used then the setSpec will be encoded into the vocab key, else it will not.
	 *
	 * @return    The enabledSets value.
	 */
	public ArrayList getEnabledSets() {
		// Refactoring note: can remove the vocab mgr and just use the plan old set spec in all cases...

		if (enabledSets == null) {
			synchronized (setInfosLock) {
				enabledSets = new ArrayList();
				ArrayList setInfos = getSetInfos();
				if (setInfos != null) {
					SetInfo set;
					String setKey = null;
					for (int i = 0; i < setInfos.size(); i++) {
						set = (SetInfo) setInfos.get(i);
						if (set.isEnabled()) {
							setKey = set.getSetSpec();
							if (metadataVocab != null) {
								try {
									setKey = metadataVocab.getTranslatedValue("dlese_collect", "key", set.getSetSpec());
								} catch (Throwable t) {
									//prtlnErr("Error encoding setSpec using vocab mgr: " + t);
									//t.printStackTrace();
								}
							}
							if (setKey != null)
								enabledSets.add(setKey);
						}
					}
				}
			}
		}

		return (ArrayList) enabledSets.clone();
	}


	/**
	 *  Gets the configured sets that are currently enabled in this repository. If the vocab manager is being
	 *  used then the setSpecs are encoded, else they are not. Both the key and the values in the HashMap are the
	 *  same.
	 *
	 * @return    The enabledSets value.
	 */
	public HashMap getEnabledSetsHashMap() {
		if (enabledSetsHashMap == null) {
			synchronized (setInfosLock) {
				enabledSetsHashMap = new HashMap();
				ArrayList es = getEnabledSets();
				if (es != null)
					for (int i = 0; i < es.size(); i++)
						enabledSetsHashMap.put(es.get(i), es.get(i));
			}
		}
		return (HashMap) enabledSetsHashMap.clone();
	}


	/**
	 *  Gets the configured sets that are currently disabled in this repository. If the vocab manager is being
	 *  used then the setSpecs are encoded, else they are not. Both the key and the values in the HashMap are the
	 *  same.
	 *
	 * @return    The disabled sets
	 */
	public HashMap getDisabledSetsHashMap() {
		if (disabledSetsHashMap == null) {
			synchronized (setInfosLock) {
				disabledSetsHashMap = new HashMap();
				ArrayList es = getDisabledSets();
				if (es != null)
					for (int i = 0; i < es.size(); i++)
						disabledSetsHashMap.put(es.get(i), es.get(i));
			}
		}
		return (HashMap) disabledSetsHashMap.clone();
	}


	/**
	 *  Gets the configured sets that are currently enabled in this repository as a String suitable for use in s
	 *  Lucene query, or an empty string if no sets are enabled.
	 *
	 * @return    The enabledSetsQuery value.
	 */
	public final String getEnabledSetsQuery() {
		// Refactoring note: can remove the vocab mgr and just use the plan old set spec in all cases...
		if (enabledSetsQuery == null) {
			synchronized (setInfosLock) {
				String searchField;
				if (metadataVocab == null)
					searchField = "collection";
				else {
					try {
						searchField = metadataVocab.getTranslatedField("dlese_collect", "key");
					} catch (Exception e) {
						searchField = "collection";
					}
				}

				ArrayList es = getEnabledSets();
				String searchVal;
				if (es == null || es.size() == 0)
					enabledSetsQuery = "";
				else {
					enabledSetsQuery = "(";
					for (int i = 0; i < es.size(); i++) {
						if (metadataVocab != null)
							enabledSetsQuery += searchField + ":" + (String) es.get(i) + " OR ";
						else
							enabledSetsQuery += searchField + ":0" + (String) es.get(i) + " OR ";
					}
					enabledSetsQuery = enabledSetsQuery.substring(0, enabledSetsQuery.length() - 4) + ")";
				}
			}
		}
		//prtln("getEnabledSetsQuery() is returnning: " + enabledSetsQuery);
		return enabledSetsQuery;
	}


	/**
	 *  Gets a query that limits a search to only those sets or collections that are not disabled.
	 *
	 * @return    A query that limits a search to only sets that are not disabled, or an empty String if none are
	 *      disabled.
	 */
	/* 	public final String getRemoveDisabledSetsQuery() {
		// 5-26-2004 note: tried this method but not sure it works... needs debug...
		String query = null;
		// Remove disabled sets
		if (getDisabledSets().size() > 0) {
			query = "(collection:0* AND (!collection:0" + getDisabledSets().get(0);
			for (int i = 1; i < getDisabledSets().size(); i++)
				query += " OR !collection:0" + getDisabledSets().get(i);
		}
		else
			return null;
		query += "))";
		return query;
	} */
	/**
	 *  Gets the query string used to limit searches to only those ADN items-level records that should be
	 *  displayed in discovery.
	 *
	 * @return    The query used to get only item-level records that are discoverable. The value returned is
	 *      always non-null.
	 */
	public final String getDiscoverableItemsQuery() {
		String enabledSetsQuery = getEnabledSetsQuery();
		String res = "accessionstatus:"
			 + MmdRec.statusNames[MmdRec.STATUS_ACCESSIONED_DISCOVERABLE];
		if (enabledSetsQuery.length() > 0)
			res += " AND " + getEnabledSetsQuery();
		//prtln("getDiscoverableItemsQuery(): " + res)accessionstatus:"
		return res;
	}


	/**
	 *  Gets the query string used to limit searches to only those records, of any format (item, collection,
	 *  anno, etc.), that should be accessable in discovery or via the web service.
	 *
	 * @return    The query used to get only records that are discoverable. The value returned is always
	 *      non-null.
	 */
	public final String getDiscoverableRecordsQuery() {
		String enabledSetsQuery = getEnabledSetsQuery();
		String res = getDiscoverableStatusQuery();
		if (enabledSetsQuery.length() > 0)
			res += " AND " + enabledSetsQuery;
		//prtln("getDiscoverableRecordsQuery(): " + res);
		return res;
	}


	/**
	 *  Gets the query string used to limit searches to only those records, of any format (item, collection,
	 *  anno, etc.), that should be accessable for OAI including 'enabled' deleted records. Removes all records
	 *  in disabled file directories.<p>
	 *
	 *  Refactoring note: Consider using this for method {@link #getDiscoverableRecordsQuery} instead.
	 *
	 * @return    The Query used to get only records that are not disabled
	 */
	public final Query getDiscoverableOaiRecordsQuery() {
		Query disabledSetsQuery = getDisabledSetsQuery();
		Query discoverableStatusQuery = null;
		try {
			discoverableStatusQuery = getIndex().getQueryParser().parse(getDiscoverableStatusQuery());
		} catch (Throwable t) {
			prtlnErr("getDiscoverableOaiRecordsQuery() error: " + t);
		}

		BooleanQuery bq = new BooleanQuery();
		if (disabledSetsQuery != null)
			bq.add(disabledSetsQuery, BooleanClause.Occur.MUST_NOT);
		if (discoverableStatusQuery != null)
			bq.add(discoverableStatusQuery, BooleanClause.Occur.MUST);
		return bq;
	}


	/**
	 *  Gets the query string used to limit searches to only those items that have an appropriate status for
	 *  discovery. This does not limit by items whoes collection is not discoverable.
	 *
	 * @return    The query string used to limit searches to only those items that have an appropriate status for
	 *      discovery. The value returned is always non-null.
	 */
	private final String getDiscoverableStatusQuery() {
		if (nonDiscoverableStatusQuery == null) {
			nonDiscoverableStatusQuery = "allrecords:true AND !doctype:0errordoc";
			String[] stati = MmdRec.statusNames;
			for (int i = 0; i < stati.length; i++) {
				if (i != MmdRec.STATUS_ACCESSIONED_DISCOVERABLE)
					nonDiscoverableStatusQuery += " AND !accessionstatus:" + stati[i];
			}
		}
		//prtln("getNonDiscoverableStatusQuery(): " + nonDiscoverableStatusQuery);
		return nonDiscoverableStatusQuery;
	}


	private long repositoryLastModCount = -1;
	private long repositorySetsModTime = -1;
	private int numDiscoverableADNResources = 0;
	private int numDiscoverableResources = 0;


	/**
	 *  Gets the number of descrete ADN item-level resources that have been indexed and are currently
	 *  discoverable.
	 *
	 * @return    The number of ADN resources that are currently discoverable.
	 */
	public final int getNumDiscoverableADNResources() {
		initNumDiscoverableCounts();
		return numDiscoverableADNResources;
	}


	/**
	 *  Gets the number of descrete resources of any format (adn, dlese_anno, news_opps, etc) that have been
	 *  indexed and are currently discoverable.
	 *
	 * @return    The number of resources of any format that are currently discoverable.
	 */
	public final int getNumDiscoverableResources() {
		initNumDiscoverableCounts();
		return numDiscoverableResources;
	}


	private final void initNumDiscoverableCounts() {
		long modCount = getIndexLastModifiedCount();
		long setsModTime = getSetStatusModifiedTime();
		if (modCount != repositoryLastModCount || setsModTime != repositorySetsModTime) {
			repositoryLastModCount = modCount;
			repositorySetsModTime = setsModTime;

			// Set the current number of discoverable ADN records
			ResultDocList results = getIndex().searchDocs(getDiscoverableItemsQuery());
			if (results != null) {
				numDiscoverableADNResources = results.size();
			}
			else {
				numDiscoverableADNResources = 0;
			}

			// Set the current number of ALL discoverable records
			results = null;
			results = getIndex().searchDocs(getDiscoverableRecordsQuery());
			if (results != null) {
				numDiscoverableResources = results.size();
			}
			else {
				numDiscoverableResources = 0;
			}
		}
	}


	private final void setDrcBoostingQuery(String boostFactor) {
		drcBoostingQuery = "(partofdrc:true^" + boostFactor + " OR partofdrc:false)";
	}


	private final void setMultiDocBoostingQuery(String boostFactor) {
		multiDocBoostingQuery = "(multirecord:true^" + boostFactor + " OR multirecord:false)";
	}


	/**
	 *  Boost items in the DRC.
	 *
	 * @return    A query String used to boost results that have DRC reviews.
	 */
	public final String getDrcBoostingQuery() {
		if (drcBoostingQuery == null)
			setDrcBoostingQuery(getDrcBoostFactor());
		return drcBoostingQuery;
	}


	/**
	 *  Boost items in the DRC absolutely.
	 *
	 * @return    A query String used to boost results that have DRC reviews above all other criteria.
	 */
	public final String getAbsoluteDrcBoostingQuery() {
		return ABSOLUTE_DRC_BOOST_QUERY;
	}


	/**
	 *  Boost items that have multiple records associated with them.
	 *
	 * @return    A query String used to boost multi-record resources.
	 */
	public final String getMultiDocBoostingQuery() {
		if (multiDocBoostingQuery == null)
			setMultiDocBoostingQuery(getMultiDocBoostFactor());
		return multiDocBoostingQuery;
	}


	/**
	 *  Gets the boosting factor used to rank items in the DRC.
	 *
	 * @return    The boosting factor used to rank items in the DRC.
	 */
	public String getDrcBoostFactor() {
		String value = null;
		try {
			value = (String) adminData.get(Keys.DRC_BOOST_FACTOR);
		} catch (OIDDoesNotExistException e) {
		}
		if (value == null)
			return Double.toString(defaultDrcBoostFactor);
		else
			return value;
	}


	/**
	 *  Gets the boosting factor used to rank resources that are referenced by more than one record.
	 *
	 * @return    The boosting factor used to rank items in multiple records.
	 */
	public String getMultiDocBoostFactor() {
		String value = null;
		try {
			value = (String) adminData.get(Keys.MULTIDOC_BOOST_FACTOR);
		} catch (OIDDoesNotExistException e) {
		}
		if (value == null)
			return Double.toString(defaultMultiDocBoostFactor);
		else
			return value;
	}


	/**
	 *  Sets the boosting factor used to rank resources that are referenced by more than one record.
	 *
	 * @param  boostFactor    The new boosting factor used to rank items in multiple records.
	 * @exception  Exception  If error.
	 */
	public void setMultiDocBoostFactor(double boostFactor)
		 throws Exception {
		if (!(boostFactor >= 0.0))
			throw new Exception("MultiDocBoostFactor must be greater than or equal to zero");

		String value = Double.toString(boostFactor);

		try {
			if (!adminData.oidExists(Keys.MULTIDOC_BOOST_FACTOR)) {
				adminData.put(Keys.MULTIDOC_BOOST_FACTOR, value);
			}
			else {
				adminData.update(Keys.MULTIDOC_BOOST_FACTOR, value);
			}
			setMultiDocBoostingQuery(value);
		} catch (Exception e) {
			prtlnErr("Error saving serialized MULTIDOC_BOOST_FACTOR: " + e);
		}
	}


	/**
	 *  Sets whether OAI-PMH ListRecords and ListIdentifiers functionality is enabled for this repository. Set to
	 *  false to disable regular OAI-PMH ListRecord and ListIdentifier resonses. QDL ListRecord and
	 *  ListIdentifier requests are not effected.
	 *
	 * @param  isEnabled  False to disable regular ListRecord and ListIdentifier OAI-PMH responses
	 */
	public void setIsOaiPmhEnabled(boolean isEnabled) {
		isOaiPmhEnabled = isEnabled;
	}


	/**
	 *  Gets whether OAI-PMH ListRecords and ListIdentifiers functionality is enabled for this repository. False
	 *  means regular ListRecord and ListIdentifier responses are disabled. QDL ListRecord and ListIdentifier are
	 *  always available, regardless.
	 *
	 * @return    False means regular ListRecord and ListIdentifier OAI-PMH responses are disabled
	 */
	public boolean getIsOaiPmhEnabled() {
		return isOaiPmhEnabled;
	}


	/**
	 *  Sets the boosting factor used to rank items in the DRC. Value must be zero or greater.
	 *
	 * @param  boostFactor    The new boosting factor used to rank items in the DRC.
	 * @exception  Exception  If error.
	 */
	public void setDrcBoostFactor(double boostFactor)
		 throws Exception {
		if (!(boostFactor >= 0.0))
			throw new Exception("DrcBoostFactor must be greater than or equal to zero");

		String value = Double.toString(boostFactor);

		try {
			if (!adminData.oidExists(Keys.DRC_BOOST_FACTOR)) {
				adminData.put(Keys.DRC_BOOST_FACTOR, value);
			}
			else {
				adminData.update(Keys.DRC_BOOST_FACTOR, value);
			}
			setDrcBoostingQuery(value);
		} catch (Exception e) {
			prtlnErr("Error saving serialized DRC_BOOST_FACTOR: " + e);
		}
	}


	/**
	 *  Gets the boosting factor used to rank items with matching terms in the title field.
	 *
	 * @return        The boosting factor used to rank items with matching terms in the title field.
	 * @deprecated    As of 12/2004, boosting behavior is controlled by properties set in the RepositoryManager
	 *      config.
	 */
	public String getTitleBoostFactor() {
		String value = null;
		try {
			value = (String) adminData.get(Keys.TITLE_BOOST_FACTOR);
		} catch (OIDDoesNotExistException e) {
		}
		if (value == null)
			return Double.toString(defaultTitleBoostFactor);
		else
			return value;
	}


	/**
	 *  Sets the boosting factor used to rank items with matching terms in the title field. Value must be zero or
	 *  greater.
	 *
	 * @param  boostFactor    The boosting factor used to rank items with matching terms in the title field.
	 * @exception  Exception  If error
	 * @deprecated            As of 12/2004, boosting behavior is controlled by properties set in the
	 *      RepositoryManager config.
	 */
	public void setTitleBoostFactor(double boostFactor) throws Exception {

		if (!(boostFactor >= 0.0))
			throw new Exception("TitleBoostFactor must be greater than or equal to zero");

		String value = Double.toString(boostFactor);

		try {
			if (!adminData.oidExists(Keys.TITLE_BOOST_FACTOR)) {
				adminData.put(Keys.TITLE_BOOST_FACTOR, value);
			}
			else {
				adminData.update(Keys.TITLE_BOOST_FACTOR, value);
			}
		} catch (Exception e) {
			prtlnErr("Error saving serialized TITLE_BOOST_FACTOR: " + e);
		}
	}



	/**
	 *  Resets the boosting factors to the default values.
	 *
	 * @exception  Exception  If error
	 */
	public void resetBoostingFactorDefaults() throws Exception {
		setTitleBoostFactor(defaultTitleBoostFactor);
		setDrcBoostFactor(defaultDrcBoostFactor);
		setMultiDocBoostFactor(defaultMultiDocBoostFactor);
		setStemmingBoostFactor(defaultStemmingBoostFactor);
		setStemmingEnabled(defaultStemmingEnabled);
	}


	/**
	 *  Gets the boosting factor used to rank items with matching stemmed terms.
	 *
	 * @return        The boosting factor used to rank items with matching stemmed terms.
	 * @deprecated    As of 12/2004, stemming behavior is controlled by properties set in the RepositoryManager
	 *      config.
	 */
	public String getStemmingBoostFactor() {
		String value = null;
		try {
			value = (String) adminData.get(Keys.STEMMING_BOOST_FACTOR);
		} catch (OIDDoesNotExistException e) {
		}
		if (value == null)
			return Double.toString(defaultStemmingBoostFactor);
		else
			return value;
	}


	/**
	 *  Sets the boosting factor used to rank items with matching stemmed terms. Value must be zero or greater.
	 *
	 * @param  boostFactor    The boosting factor used to rank items with matching stemmed terms.
	 * @exception  Exception  If error.
	 * @deprecated            As of 12/2004, stemming behavior is controlled by properties set in the
	 *      RepositoryManager config.
	 */
	public void setStemmingBoostFactor(double boostFactor) throws Exception {
		if (!(boostFactor >= 0.0))
			throw new Exception("StemmingBoostFactor must be greater than or equal to zero");

		String value = Double.toString(boostFactor);

		try {
			if (!adminData.oidExists(Keys.STEMMING_BOOST_FACTOR)) {
				adminData.put(Keys.STEMMING_BOOST_FACTOR, value);
			}
			else {
				adminData.update(Keys.STEMMING_BOOST_FACTOR, value);
			}
		} catch (Exception e) {
			prtlnErr("Error saving serialized STEMMING_BOOST_FACTOR: " + e);
		}
	}



	/**
	 *  Indicates whether stemming support is enabled.
	 *
	 * @return        true if stemming is enabled, false otherwise.
	 * @deprecated    As of 12/2004, stemming behavior is controlled by properties set in the RepositoryManager
	 *      config.
	 */
	public boolean isStemmingEnabled() {
		Boolean value = null;
		try {
			value = (Boolean) adminData.get(Keys.STEMMING_ENABLED);
		} catch (OIDDoesNotExistException e) {
		}
		if (value == null)
			return defaultStemmingEnabled;
		else
			return value.booleanValue();
	}


	/**
	 *  Sets whether stemming is enabled.
	 *
	 * @param  stemmingEnabled  true to enable stemming, false to disable it.
	 * @deprecated              As of 12/2004, stemming behavior is controlled by properties set in the
	 *      RepositoryManager config.
	 */
	public void setStemmingEnabled(boolean stemmingEnabled) {
		try {
			if (!adminData.oidExists(Keys.STEMMING_ENABLED)) {
				adminData.put(Keys.STEMMING_ENABLED, new Boolean(stemmingEnabled));
			}
			else {
				adminData.update(Keys.STEMMING_ENABLED, new Boolean(stemmingEnabled));
			}
		} catch (Exception e) {
			prtlnErr("Error saving serialized STEMMING_ENABLED: " + e);
		}
	}


	/**
	 *  Gets a Query for the given Lucene query String that is expanded using the default search fields, boosting
	 *  fields and virtual search field mappings that are configured in this RepositoryManager. The Query may
	 *  then be used to search the index to find matching results.
	 *
	 * @param  queryString                                       The query String
	 * @return                                                   The Query, expanded
	 * @exception  org.apache.lucene.queryParser.ParseException  If error parsing the query String
	 * @see                                                      #getDefaultSearchFields
	 * @see                                                      #getFieldsUsedForBoosting
	 * @see                                                      #getBoostingValues
	 * @see                                                      #getVirtualSearchFieldMapper
	 */
	public Query getExpandedSearchQuery(String queryString) throws
		org.apache.lucene.queryParser.ParseException {
		return FieldExpansionQueryParser.parse(
			queryString,
			getIndex().getAnalyzer(),
			getDefaultSearchFields(),
			getFieldsUsedForBoosting(),
			getBoostingValues(),
			getVirtualSearchFieldMapper(),
			getIndex().getLuceneOperator());
	}


	/**
	 *  Gets the names of the fields that are serched for terms that match a users query. These are defined in
	 *  the file named search_fields.properties found in the repository config directory.
	 *
	 * @return    The defaultSearchFields value
	 * @see       #reloadConfigFiles
	 */
	public String[] getDefaultSearchFields() {
		if (defaultSearchFields == null) {
			Properties fieldsProperties = null;
			//prtln("repositoryConfigDir: " + repositoryConfigDir);
			String conf = null;
			try {
				conf = repositoryConfigDir.getAbsolutePath() + "/search_fields.properties";
				fieldsProperties = new PropertiesManager(conf);
			} catch (Throwable e) {
				System.out.println("RepositoryManager: No search field config file available at '" +
					conf + "'. Using the default search field configuration instead.");
			}

			if (fieldsProperties != null) {
				String temp = null;
				temp = fieldsProperties.getProperty("search.fields");
				if (temp != null && temp.trim().length() > 0)
					defaultSearchFields = temp.split("[\\s|,]+");
				if (defaultSearchFields == null)
					defaultSearchFields = new String[]{};
			}
			if (defaultSearchFields == null)
				defaultSearchFields = new String[]{"stems"};
		}
		return defaultSearchFields;
	}



	/**
	 *  Gets the names of the index fields that are used to boost records that match a users query. These are
	 *  defined in the file named search_fields.properties found in the repository config directory.
	 *
	 * @return    The fieldsUsedForBoosting value
	 * @see       #reloadConfigFiles
	 */
	public String[] getFieldsUsedForBoosting() {
		if (fieldsUsedForBoosting == null) {
			Properties fieldsProperties = null;
			String conf = null;
			try {
				conf = repositoryConfigDir.getAbsolutePath() + "/search_fields.properties";
				fieldsProperties = new PropertiesManager(conf);
			} catch (Throwable e) {
				System.out.println("RepositoryManager: No search boosting config file available at '" +
					conf + "'. Using the default search boosting settings instead.");
			}

			if (fieldsProperties != null) {
				String temp = null;
				temp = fieldsProperties.getProperty("boost.fields");
				if (temp != null && temp.trim().length() > 0)
					fieldsUsedForBoosting = temp.split("[\\s|,]+");
				if (fieldsUsedForBoosting == null)
					fieldsUsedForBoosting = new String[]{};
			}
			if (fieldsUsedForBoosting == null)
				fieldsUsedForBoosting = new String[]{"title", "titlestems", "description", "default"};

		}
		return fieldsUsedForBoosting;
	}


	/**
	 *  Gets the boosting values for fields used to boost search results. These are defined in the file named
	 *  search_fields.properties found in the repository config directory.
	 *
	 * @return    The boostingValues value
	 * @see       #reloadConfigFiles
	 */
	public Map getBoostingValues() {
		Properties fieldsProperties = null;
		if (boostingValues == null) {
			boostingValues = new HashMap();

			String conf = null;
			try {
				conf = repositoryConfigDir.getAbsolutePath() + "/search_fields.properties";
				fieldsProperties = new PropertiesManager(conf);
			} catch (Throwable e) {
				System.out.println("RepositoryManager: No boosting fields config file available at '" +
					conf + "'. Using the default configuration instead.");
			}

			String[] fieldsUsedForBoosting = getFieldsUsedForBoosting();
			Float value = null;
			if (fieldsUsedForBoosting != null && fieldsUsedForBoosting.length > 0) {
				for (int i = 0; i < fieldsUsedForBoosting.length; i++) {
					try {
						value = new Float((String) fieldsProperties.get(fieldsUsedForBoosting[i] + ".boost.value"));
					} catch (Throwable e) {}
					if (value != null)
						boostingValues.put(fieldsUsedForBoosting[i], value);
					value = null;
				}
			}
		}
		return boostingValues;
	}


	/**  Instructs the RepositoryManager to re-read and re-load the values found in it's configuration files. */
	public void reloadConfigFiles() {
		// null variables are reloaded - force re-loading by making them null...
		fieldsUsedForBoosting = null;
		defaultSearchFields = null;
		boostingValues = null;
	}


	/**
	 *  Gets the configured file sets that are currently disabled in this repository.
	 *
	 * @return    The disabledSets value
	 */
	public ArrayList getDisabledSets() {
		if (disabledSets == null) {
			synchronized (setInfosLock) {
				disabledSets = new ArrayList();
				ArrayList setInfos = getSetInfos();
				if (setInfos != null) {
					SetInfo set;
					for (int i = 0; i < setInfos.size(); i++) {
						set = (SetInfo) setInfos.get(i);
						if (!set.isEnabled())
							disabledSets.add(set.getSetSpec());
					}
				}
			}
		}
		return (ArrayList) disabledSets.clone();
	}


	/**  Deletes all SetInfos (directories of files) that are configured. */
	private void deleteSetInfos() {
		try {
			synchronized (setInfosLock) {
				adminData.delete(Keys.SET_INFOS);
			}
		} catch (Throwable e) {}
	}


	/**
	 *  Gets the SetInfos (directories of files) that are currently configured in the repository.
	 *
	 * @return    The setInfos.
	 * @see       SetInfo
	 */
	public ArrayList getSetInfos() {
		synchronized (setInfosLock) {
			ArrayList setInfos = arrayListGet(Keys.SET_INFOS, false);
			if (setInfos == null) {
				try {
					adminData.delete(Keys.SET_INFOS);
				} catch (Throwable e) {}
				return null;
			}
			ArrayList retVals = (ArrayList) setInfos.clone();
			String sets = "";
			for (int i = 0; i < retVals.size(); i++)
				sets += ((SetInfo) retVals.get(i)).getSetSpec() + " ";

			//prtln("getSetInfos(): {" + sets.trim() + "}");
			return retVals;
		}
	}


	/**
	 *  Gets the index in the SetInfos array of the given SetInfo, by UID.
	 *
	 * @param  uid       The UID of the set
	 * @param  setInfos  The SetInfosArray
	 * @return           The the index in the SetInfos array of the given SetInfo.
	 * @see              SetInfo
	 */
	private int getIndexOfSetInfo(String uid, ArrayList setInfos) {
		long id = Long.parseLong(uid);
		if (setInfos == null)
			return -1;
		for (int i = 0; i < setInfos.size(); i++)
			if (id == ((SetInfo) setInfos.get(i)).getUniqueIDLong())
				return i;
		return -1;
	}


	/**
	 *  Enables the given set of files for discovery.
	 *
	 * @param  setUid  The set Uid
	 */
	public void enableSet(String setUid) {
		try {
			ArrayList setInfos = this.getSetInfos();
			int index = getIndexOfSetInfo(setUid, setInfos);
			//prtln("enabling set: " + index);
			((SetInfo) setInfos.get(index)).setEnabled("true");

			synchronized (setInfosLock) {
				adminData.update(Keys.SET_INFOS, setInfos);
				resetSetsData();
			}

		} catch (Throwable e) {
			return;
		}
	}


	/**
	 *  Disabled the given set of files from discovery.
	 *
	 * @param  setUid  The set Uid
	 */
	public void disableSet(String setUid) {

		try {
			ArrayList setInfos = getSetInfos();
			int index = getIndexOfSetInfo(setUid, setInfos);
			//prtln("disabling set: " + index);
			((SetInfo) setInfos.get(index)).setEnabled("false");

			synchronized (setInfosLock) {
				adminData.update(Keys.SET_INFOS, setInfos);
				resetSetsData();
			}

		} catch (Throwable e) {
			return;
		}
	}


	/**
	 *  Gets the SetInfos (directories of files) that are currently configured in the repository as a HashMap
	 *  keyed by setInfo.directory().
	 *
	 * @return    The setInfos HashMap.
	 * @see       SetInfo
	 */
	public HashMap getSetInfosHashMap() {
		ArrayList setInfos = getSetInfos();
		if (setInfos == null)
			return null;
		HashMap map = new HashMap(setInfos.size());
		for (int i = 0; i < setInfos.size(); i++) {
			SetInfo set = (SetInfo) setInfos.get(i);
			map.put(set.getDirectory(), set);
		}
		return map;
	}


	/**
	 *  Gets a copy of the SetInfos (directories of files) that are currently configured in the repository. The
	 *  copy is suitable for modifying without effecting the data in the repository.
	 *
	 * @return    The setInfosCopy value.
	 * @see       SetInfo
	 */
	public ArrayList getSetInfosCopy() {
		synchronized (setInfosLock) {
			return arrayListGet(Keys.SET_INFOS, true);
		}
	}


	/**
	 *  Gets the SetInfo (directory of files) at the give index, suitable for reading but not modifying.
	 *
	 * @param  i  The index into the SetInfo.
	 * @return    The setInfo value.
	 */
	public SetInfo getSetInfo(int i) {
		synchronized (setInfosLock) {
			return ((SetInfo) arrayListGetItem(i, Keys.SET_INFOS, false));
		}
	}


	/**
	 *  Gets the SetInfo (directory of files) by the given set key, for example 'dcc'.
	 *
	 * @param  key  The set key, for example 'dcc'
	 * @return      The setInfo, or null if not available
	 */
	public SetInfo getSetInfo(String key) {
		if (key == null)
			return null;
		try {
			ArrayList setInfos = getSetInfos();
			SetInfo si = null;
			for (int i = 0; i < setInfos.size(); i++) {
				si = (SetInfo) setInfos.get(i);
				if (key.equals(si.getSetSpec()))
					return si;
			}
		} catch (Throwable t) {
			prtlnErr("Unable to get SetInfo \"" + key + "\" reason: " + t);
		}

		return null;
	}


	/**
	 *  Gets the setInfo at the give index, safe for modifying.
	 *
	 * @param  i  The index into the SetInfo.
	 * @return    The setInfo value.
	 */
	public SetInfo getSetInfoCopy(int i) {
		synchronized (setInfosLock) {
			return ((SetInfo) arrayListGetItem(i, Keys.SET_INFOS, true));
		}
	}


	/**
	 *  Adds a new set of files to the repository configuration, but does not index them.
	 *
	 * @param  setInfo        The SetInfo to add to the reposigory configuration.
	 * @exception  Exception  If one of the directories is already configured or none is specified.
	 */
	public void addSetInfo(SetInfo setInfo) throws Exception {
		synchronized (setInfosLock) {
			//prtln("addSetInfo(): " + setInfo);

			// Make sure a directory is indicated
			List dirInfos = setInfo.getDirInfos();
			if (dirInfos == null || dirInfos.size() == 0)
				throw new Exception("The SetInfo does not have any directories defined.");

			// Make sure none of the given directories are already configured.
			for (int i = 0; i < dirInfos.size(); i++) {
				DirInfo dirInfo = (DirInfo) dirInfos.get(i);
				if (fileIndexingService.isDirectoryConfigured(new File(dirInfo.getDirectory()))) {
					throw new Exception("This directory is already configured in the repository: " + dirInfo.getDirectory());
				}
			}

			//prtln("addSetInfo(): " + setInfo);
			arrayListAddItem(setInfo, Keys.SET_INFOS);
			putSetInIndex(setInfo);

			// Reset so that these will get re-generated
			resetSetsData();
		}
	}


	/**
	 *  Remove the given set of files from the repository.
	 *
	 * @param  setInfo  The set info to remove.
	 * @see             SetInfo
	 */
	public void removeSetInfo(SetInfo setInfo) {
		synchronized (setInfosLock) {
			//prtln("removeSetInfo(): " + setInfo);
			arrayListRemoveItem(setInfo, Keys.SET_INFOS);
			removeSetFromIndex(setInfo);

			// Reset so that these will get re-generated
			resetSetsData();
		}
	}


	/**
	 *  Removes the given set of files form the repository.
	 *
	 * @param  i  The index of the SetInfo to remove.
	 * @see       SetInfo
	 */
	public void removeSetInfo(int i) {
		synchronized (setInfosLock) {
			//prtln("removeSetInfo(): index " + i);

			// Get the item from disc to be sure we've got the right one.
			removeSetFromIndex((SetInfo) arrayListGetItem(i, Keys.SET_INFOS, true));
			arrayListRemoveItem(i, Keys.SET_INFOS);

			// Reset so that these will get re-generated
			resetSetsData();
		}
	}


	/**
	 *  Removes the cofiguration (SetInfo) for the given set of files from the repository and deletes the index
	 *  entries for that set of files.
	 *
	 * @param  setSpec  The setSpec, or collection, to remove for example 'dcc'
	 * @return          The removed SetInfo if successful, null if the given set was not found
	 */
	public SetInfo removeSetBySetSpec(String setSpec) {
		List setInfos = getSetInfos();
		if (setInfos == null || setSpec == null)
			return null;
		for (int i = 0; i < setInfos.size(); i++) {
			SetInfo si = (SetInfo) setInfos.get(i);
			if (si.getSetSpec().equals(setSpec.trim())) {
				removeSetInfo(i);
				return si;
			}
		}
		return null;
	}


	/**
	 *  Replace a given SetInfo object with a new one, updating the index as appropriate.
	 *
	 * @param  key         The set key for the set to replace, for example 'dcc'
	 * @param  newSetInfo  The new SetInfo
	 */
	public void replaceSetInfo(String key, SetInfo newSetInfo) {
		if (key == null)
			return;
		try {
			ArrayList setInfos = getSetInfos();
			SetInfo si = null;
			for (int i = 0; i < setInfos.size(); i++) {
				si = (SetInfo) setInfos.get(i);
				if (key.equals(si.getSetSpec())) {
					replaceSetInfo(i, newSetInfo);
					return;
				}
			}
		} catch (Throwable t) {
			prtlnErr("replaceSetInfo(): Unable to get SetInfo \"" + key + "\" reason: " + t);
		}
	}


	/**
	 *  Replace a given SetInfo object with a new one, updating the index as appropriate.
	 *
	 * @param  i           The index into the array of SetInfos to remove
	 * @param  newSetInfo  The new SetInfo
	 */
	public void replaceSetInfo(int i, SetInfo newSetInfo) {
		// Get the item from disc to be sure we've got the right one.
		SetInfo currentSetInfo;
		synchronized (setInfosLock) {
			currentSetInfo = (SetInfo) arrayListGetItem(i, Keys.SET_INFOS, true);

			//prtln("replaceSetInfo() (does a synch) \n replacing: " + currentSetInfo + "\n with: " + newSetInfo);

			// If nothing has changed, return...
			if (currentSetInfo.equals(newSetInfo)) {
				//prtln("replaceSetInfo(): nothing has change, returning...");
				return;
			}

			// Reset so that these will get re-generated
			resetSetsData();
		}

		// ------- Determine whether the index needs updating ---------

		// Update the index if the setSpec or dir has changed.
		if (!currentSetInfo.getSetSpec().equals(newSetInfo.getSetSpec()) ||
			!currentSetInfo.getDirectory().equals(newSetInfo.getDirectory())) {
			//prtln("replaceSetInfo() IS updating the index for the setSpec...");
			SetInfo si;
			synchronized (setInfosLock) {
				si = (SetInfo) arrayListGetItem(i, Keys.SET_INFOS, true);
			}
			removeSetFromIndex(si);
			putSetInIndex(newSetInfo);
			synchronized (setInfosLock) {
				arrayListReplaceItem(i, newSetInfo, Keys.SET_INFOS);
			}
			fileIndexingService.indexFiles(false, null);
			return;
		}

		// If the only thing to change is the name...
		if (newSetInfo.getDirInfos().equals(currentSetInfo.getDirInfos())) {
			//prtln("replaceSetInfo() NOT updating the index for the setSpec... only change is name");
			synchronized (setInfosLock) {
				arrayListReplaceItem(i, newSetInfo, Keys.SET_INFOS);
			}
			return;
		}

		// Add, update or remove a directory from the index, as needed:
		ArrayList dirInfos;

		// Add, update or remove a directory from the index, as needed:
		ArrayList dirInfos2 = null;
		SetInfo comp;
		SetInfo comp2 = null;
		boolean addDir = false;
		boolean removeDir = false;
		boolean replaceDir = false;
		if (newSetInfo.getDirInfos().size() < currentSetInfo.getDirInfos().size()) {
			prtln("replaceSetInfo() opt 1 - remove a directory...");
			dirInfos = currentSetInfo.getDirInfos();
			comp = newSetInfo;
			removeDir = true;
		}
		else {
			dirInfos = newSetInfo.getDirInfos();
			comp = currentSetInfo;
			// Add dir
			if (newSetInfo.getDirInfos().size() > currentSetInfo.getDirInfos().size()) {
				prtln("replaceSetInfo() opt 2 - add a directory...");
				addDir = true;
			}
			else {
				// Replace dir
				dirInfos2 = currentSetInfo.getDirInfos();
				comp2 = newSetInfo;
				prtln("replaceSetInfo() opt 3 - replace a directory...");
				replaceDir = true;
			}
		}

		// Update the index only if directory definition has changed.
		boolean update = false;
		for (int j = 0; j < dirInfos.size(); j++) {
			DirInfo dirInfo = (DirInfo) dirInfos.get(j);
			if (!comp.containsDirInfo(dirInfo)) {
				prtln("replaceSetInfo() IS updating the a dir in the index... \ndirInfo=\n " + dirInfo + "\ncomp=" + comp);
				update = true;
				if (removeDir)
					removeDirFromIndex(dirInfo);
				if (addDir)
					putDirInIndex(dirInfo, currentSetInfo.getSetSpec());
				if (replaceDir) {
					for (int k = 0; k < dirInfos2.size(); k++) {
						DirInfo dirInfo2 = (DirInfo) dirInfos2.get(k);
						if (!comp2.containsDirInfo(dirInfo2))
							removeDirFromIndex(dirInfo2);
					}
					putDirInIndex(dirInfo, currentSetInfo.getSetSpec());
				}
			}
		}
		synchronized (setInfosLock) {
			arrayListReplaceItem(i, newSetInfo, Keys.SET_INFOS);
		}
		if (update)
			fileIndexingService.indexFiles(false, null);
	}


	/**
	 *  Resets the configuredSets, configuredFormats, enabledSets, and disabledSets data structures whenever a
	 *  change is made to one of them.
	 */
	private final void resetSetsData() {
		synchronized (setInfosLock) {
			// Reset so that these will get updated next time they are requested.
			configuredSets = null;
			configuredFormats = null;
			enabledSets = null;
			enabledSetsHashMap = null;
			enabledSetsQuery = null;
			disabledSets = null;
			disabledSetsHashMap = null;
			configuredSetInfosHashMap = null;
			setConfigLastModified = System.currentTimeMillis();
		}
	}


	/**
	 *  Remove the set of files from the index.
	 *
	 * @param  set  The set to remove.
	 */
	private void removeSetFromIndex(SetInfo set) {
		//String setSpec = set.getSetSpec();
		ArrayList dirInfos = set.getDirInfos();
		DirInfo dirInfo;
		for (int j = 0; j < dirInfos.size(); j++) {
			dirInfo = (DirInfo) dirInfos.get(j);
			fileIndexingService.deleteDirectory(dirInfo.getDirectory());
		}
	}


	/**
	 *  Remove the dir from the index.
	 *
	 * @param  dirInfo  The DirInfo to remove.
	 */
	private void removeDirFromIndex(DirInfo dirInfo) {
		prtln("removeDirFromIndex() removing dir " + dirInfo.getDirectory());
		fileIndexingService.deleteDirectory(dirInfo.getDirectory());
	}


	/**
	 *  Gets the number of records that had indexing errors.
	 *
	 * @return    The number of indexing errors
	 */
	public int getNumIndexingErrors() {
		ResultDocList indexingErrors = getIndexingErrorDocs();
		if (indexingErrors == null)
			return 0;
		return indexingErrors.size();
	}


	/**
	 *  Gets the ResultDocs for those records that had errors and could not be indexed, or null if none exist.
	 *
	 * @return    The indexing error ResultDocs, or null if none
	 * @see       org.dlese.dpc.index.reader.ErrorDocReader
	 */
	public ResultDocList getIndexingErrorDocs() {
		SimpleLuceneIndex index = getIndex();
		if (index != null)
			return index.searchDocs("error:true");
		else
			return null;
	}


	/**
	 *  Gets the number of records in the index, excluding records with errors.
	 *
	 * @return    The number of records in the index
	 * @see       #getAllRecordsInIndex
	 */
	public int getNumRecordsInIndex() {
		ResultDocList recs = getAllRecordsInIndex();
		if (recs == null)
			return 0;
		return recs.size();
	}


	/**
	 *  Gets the ResultDocs for all records in the index, excluding records with errors, or null if none exist.
	 *
	 * @return    All records ResultDocs, or null if none
	 * @see       #getNumRecordsInIndex
	 */
	public ResultDocList getAllRecordsInIndex() {
		SimpleLuceneIndex index = getIndex();
		if (index != null)
			return index.searchDocs("allrecords:true AND !error:true");
		else
			return null;
	}


	/**
	 *  Gets the total number of OAI status deleted records. Records are set to status deleted when their source
	 *  file has been removed (if removeDocs has been set to false in the RepositoryManager).
	 *
	 * @return    The number of OAI status deleted records
	 */
	public int getNumDeletedDocs() {
		return index.getNumDocs("deleted:true");
	}


	/**
	 *  Gets the total number of records that are not OAI status deleted. Records are set to status deleted when
	 *  their source file has been removed (if removeDocs has been set to false in the RepositoryManager).
	 *
	 * @return    The number of records not OAI status deleted
	 */
	public int getNumNonDeletedDocs() {
		return index.getNumDocs("deleted:false");
	}


	/**
	 *  Gets the total number of all OAI status deleted records that did not come from any of the existing file
	 *  directories configured in the RepositoryManager.
	 *
	 * @return    Number of deleted documents not from any directory
	 * @see       #getDeletedDocsNotFromAnyDirectory
	 */
	public int getNumDeletedDocsNotFromAnyDirectory() {
		ResultDocList results = getDeletedDocsNotFromAnyDirectory();
		if (results == null)
			return 0;
		else
			return results.size();
	}


	/**
	 *  Gets the ResultDocs for all OAI status deleted records that did not come from any of the existing file
	 *  directories configured in the RepositoryManager.
	 *
	 * @return    Deleted documents not from any directory, or null if none
	 * @see       #getNumDeletedDocsNotFromAnyDirectory
	 */
	public final ResultDocList getDeletedDocsNotFromAnyDirectory() {
		return getIndex().searchDocs(getDeletedDocsNotFromAnyDirectoryQuery());
	}


	/**
	 *  Gets a Query that will return all OAI status deleted records that did not come from any of the existing
	 *  file directories configured in the RepositoryManager.
	 *
	 * @return    A Query for deleted documents not from any directory
	 * @see       #getDeletedDocsNotFromAnyDirectory
	 */
	public final Query getDeletedDocsNotFromAnyDirectoryQuery() {

		BooleanQuery dirsQ = new BooleanQuery();
		dirsQ.add(new TermQuery(new Term("deleted", "true")), BooleanClause.Occur.MUST);

		List setInfos = getSetInfos();
		if (setInfos != null) {
			SetInfo setInfo = null;
			for (int i = 0; i < setInfos.size(); i++) {
				setInfo = (SetInfo) setInfos.get(i);
				dirsQ.add(new TermQuery(new Term("docdir", setInfo.getDirectory().trim())), BooleanClause.Occur.MUST_NOT);
			}
		}
		return dirsQ;
	}


	/**
	 *  Puts the SetInfo into the index and repsository.
	 *
	 * @param  set  The SetInfo to put
	 */
	private void putSetInIndex(SetInfo set) {
		String setSpec = set.getSetSpec();
		ArrayList dirInfos = set.getDirInfos();
		DirInfo dirInfo;
		for (int j = 0; j < dirInfos.size(); j++) {
			//prtln("putSetInIndex(): " + (DirInfo) dirInfos.get(j));
			putDirInIndex((DirInfo) dirInfos.get(j), setSpec);
		}
	}


	/**
	 *  Places the Directory into the FileIndexingService, replacing the existing one if there is one present
	 *  with same File absolute path.
	 *
	 * @param  dirInfo  The dirInfo.
	 * @param  setSpec  The setSpec.
	 */
	private final void putDirInIndex(DirInfo dirInfo, String setSpec) {
		String xmlFormat = dirInfo.getFormat();

		//XMLFileIndexingWriter writer = null;

		/* try {
			writer = xmlFileIndexingWriterFactory.getIndexingWriter(setSpec, xmlFormat);
		} catch (Exception e) {
			prtlnErr("Unable to add indexer for " + setSpec + ": " + e);
			return;
		} */
		// If a FileIndexingPlugin is available for this format or all formats, use it.
		FileIndexingPlugin plugin = (FileIndexingPlugin) fileIndexingPlugins.get(xmlFormat);
		if (plugin == null)
			plugin = (FileIndexingPlugin) fileIndexingPlugins.get(PLUGIN_ALL_FORMATS);

		/* if (plugin != null)
			writer.setFileIndexingPlugin(plugin);
		else {
			plugin = (FileIndexingPlugin) fileIndexingPlugins.get(PLUGIN_ALL_FORMATS);
			if (plugin != null)
				writer.setFileIndexingPlugin(plugin);
		} */
		HashMap writerConfigAttributes = new HashMap(5);
		writerConfigAttributes.put("collection", setSpec);
		writerConfigAttributes.put("recordDataService", recordDataService);
		writerConfigAttributes.put("index", getIndex());
		writerConfigAttributes.put("xmlIndexerFieldsConfig", xmlIndexerFieldsConfig);

//		Class writerClass = (Class) XMLFileIndexingWriterFactory.indexerClasses.get(xmlFormat);
//		if (writerClass == null) {
//			writerClass = (Class) XMLFileIndexingWriterFactory.indexerClasses.get("default_handler_class");
//			writerConfigAttributes.put("xmlFormat", xmlFormat);
//		}
		
		// get XMLFileIndexingWriter class from Factory using appropriate encapsulation
		Class writerClass = xmlFileIndexingWriterFactory.getIndexingWriterClass(xmlFormat);
			writerConfigAttributes.put("xmlFormat", xmlFormat);

		// We want to ensure dlese_collect gets indexed first, then dlese_anno, then all others
		if (xmlFormat.equals("dlese_collect"))
			fileIndexingService.addDirectory(dirInfo.getDirectory(), writerClass, writerConfigAttributes, plugin, 7);
		else if (xmlFormat.equals("dlese_anno"))
			fileIndexingService.addDirectory(dirInfo.getDirectory(), writerClass, writerConfigAttributes, plugin, 6);
		else
			fileIndexingService.addDirectory(dirInfo.getDirectory(), writerClass, writerConfigAttributes, plugin, 5);
	}


	/**
	 *  Sets the fileIndexingPlugin that will be used during indexing. In order to take effect, this method MUST
	 *  be called prior to calling the {@link #init()} method. An individual plugin may be specified for each XML
	 *  format and a global plugin may also be specified for use with all formats. Plugin precedence is
	 *  determined as follows: if a plugin has been set for a specific XML format, use it; else if a plugin has
	 *  been set for all formats, use it; else if a plugin has not been set for either, use none. <p>
	 *
	 *  To configure one or more Lucene Analyzers to use on a per-field basis for your plugins, provide a
	 *  properties file within your application's class path named 'FileIndexingPluginLuceneAnalyzers.properties'.
	 *  See {@link org.dlese.dpc.index.analysis.PerFieldAnalyzer} for details.
	 *
	 * @param  xmlFormat  The XML format for which this plugin is used or {@link #PLUGIN_ALL_FORMATS} to specify
	 *      that the plugin should be used with all formats
	 * @param  plugin     The FileIndexingPlugin that will be used
	 * @see               #PLUGIN_ALL_FORMATS
	 */
	public final void setFileIndexingPlugin(String xmlFormat, FileIndexingPlugin plugin) {
		fileIndexingPlugins.put(xmlFormat, plugin);
	}


	/**
	 *  Puts a record into the repository, replacing the existing record if one exists. If the record format is
	 *  dlese_anno then the annotated record itemID will be reindexed to include the annotation data.
	 *
	 * @param  recordXml                  The record XML as a String
	 * @param  xmlFormat                  The XML format of the record, for example 'adn'
	 * @param  collection                 The collection vocab for the collection or set, for example 'dcc'
	 * @param  id                         A unique identifier for the record, for example DLESE-000-000-000-001.
	 *      This parameter is ignored and may be null if the ID can be derived from the record XML.
	 * @param  saveXmlAsFile              True to save the XML record as a file on disc as well as in the index,
	 *      false to only save it in the index
	 * @return                            The ID of the record as inserted, which may be different than the ID
	 *      requested
	 * @exception  RecordUpdateException  If unable to insert the record for any reason
	 */
	public String putRecord(String recordXml,
	                        String xmlFormat,
	                        String collection,
	                        String id,
	                        boolean saveXmlAsFile)
		 throws RecordUpdateException {
		return doPutRecord(recordXml, xmlFormat, collection, id, null, saveXmlAsFile, true);
	}


	/**
	 *  Puts a record into the repository, replacing the existing record if one exists. If the record format is
	 *  dlese_anno then the annotated record itemID will be reindexed to include the annotation data.
	 *
	 * @param  recordXml                  The record XML as a String
	 * @param  xmlFormat                  The XML format of the record, for example 'adn'
	 * @param  collection                 The collection vocab for the collection or set, for example 'dcc'
	 * @param  id                         A unique identifier for the record, for example DLESE-000-000-000-001.
	 *      This parameter is ignored and may be null if the ID can be derived from the record XML.
	 * @param  indexingPlugin             A plugin to use while indexing, or null to specify none. This plugin
	 *      takes precedence over a previously configured plugin. If none is supplied here, the previously
	 *      configured plugin will be used if available.
	 * @param  saveXmlAsFile              True to save the XML record as a file on disc as well as in the index,
	 *      false to only save it in the index
	 * @return                            The ID of the record as inserted, which may be different than the ID
	 *      requested
	 * @exception  RecordUpdateException  If unable to insert the record for any reason
	 */
	public String putRecord(String recordXml,
	                        String xmlFormat,
	                        String collection,
	                        String id,
	                        FileIndexingPlugin indexingPlugin,
	                        boolean saveXmlAsFile)
		 throws RecordUpdateException {
		return doPutRecord(recordXml, xmlFormat, collection, id, indexingPlugin, saveXmlAsFile, true);
	}


	/**
	 *  Puts a record into the repository, replacing the existing record if one exists. If the record format is
	 *  dlese_anno then the annotated record itemID will be reindexed to include the annotation data.
	 *
	 * @param  recordXml                  The record XML as a String
	 * @param  xmlFormat                  The XML format of the record, for example 'adn'
	 * @param  collection                 The collection vocab for the collection or set, for example 'dcc'
	 * @param  id                         A unique identifier for the record, for example DLESE-000-000-000-001.
	 *      This parameter is ignored and may be null if the ID can be derived from the record XML.
	 * @param  indexingPlugin             A plugin to use while indexing, or null to specify none. This plugin
	 *      takes precedence over a previously configured plugin. If none is supplied here, the previously
	 *      configured plugin will be used if available.
	 * @param  saveXmlAsFile              True to save the XML record as a file on disc as well as in the index,
	 *      false to only save it in the index
	 * @param  indexRelations             True to index the records that this item assigns a relation for
	 * @return                            The ID of the record as inserted, which may be different than the ID
	 *      requested
	 * @exception  RecordUpdateException  If unable to insert the record for any reason
	 */
	private String doPutRecord(String recordXml,
	                           String xmlFormat,
	                           String collection,
	                           String id,
	                           FileIndexingPlugin indexingPlugin,
	                           boolean saveXmlAsFile,
	                           boolean indexRelations)
		 throws RecordUpdateException {
		synchronized (publicUpdateApiLock) {

			if (xmlFormat == null)
				throw new RecordUpdateException("Format specified is null");
			if (collection == null)
				throw new RecordUpdateException("Collection specified is null");
			if (recordXml == null)
				throw new RecordUpdateException("recordXml specified is null");

			SetInfo collectionSetInfo = null;
			if (getConfiguredSetInfos() != null)
				collectionSetInfo = (SetInfo) getConfiguredSetInfos().get(collection);
			if (collectionSetInfo == null)
				throw new RecordUpdateException("Collection '" + collection + "' is not configured in this repository. The collection must exist before records can be put.");
			if (!collectionSetInfo.getFormat().equals(xmlFormat))
				throw new RecordUpdateException("Format '" + xmlFormat + "' was specified, however collection '" + collection +
					"' is configured for format '" + collectionSetInfo.getFormat() + "'");

			File metadataDir = new File(collectionSetInfo.getDirInfo(0).getDirectory());
			if (!fileIndexingService.isDirectoryConfigured(metadataDir))
				throw new RecordUpdateException("The directory '" + metadataDir.getAbsolutePath() + "' is not configured in the FileIndexingService. '");

			Throwable thrown = null;
			File tempFile = new File(System.getProperty("java.io.tmpdir"), "temp_dlese_repository_put_record_file.xml");
			File writeToFile = null;
			File createdFile = null;
			String newRecordId = null;
			try {
				XMLFileIndexingWriterFactory xFactory = new XMLFileIndexingWriterFactory(getIndex(), xmlIndexerFieldsConfig);
				XMLFileIndexingWriter writer = xmlFileIndexingWriterFactory.getIndexingWriter(collection, xmlFormat);
				writer.setFileIndexingPlugin(indexingPlugin);
				writer.setFileIndexingService(fileIndexingService);
				
				prtln("putRecord() verifying record id: " + (newRecordId == null ? "[ID not yet available]" : newRecordId));
				
				Files.writeFile(recordXml, tempFile);

				// Verify that the item can be indexed (throws exception if not...)
				writer.create(tempFile, null, indexingPlugin, null);

				// Grab the ID from the writer. If the ID could not be derived from the XML it will be 'temp_dds_put_record_file'
				newRecordId = writer.getPrimaryId();
				if (newRecordId == null)
					newRecordId = "";

				// If the ID could not be derived from the XML, make sure that one was supplied or throw an exception
				if (newRecordId.equals("temp_dlese_repository_put_record_file")) {
					if (id == null || id.trim().length() == 0)
						throw new RecordUpdateException("ID cannot be determined from the record XML and id param is null");
					newRecordId = id;
				}

				prtln("putRecord() saving record id: " + newRecordId);

				// For refactor, use idvalue instead here, in deleteRecord and elsewhere where IDs are used. Some issues...
				//ResultDoc[] results = index.searchDocs(new TermQuery(new Term("idvalue", newRecordId)));

				// Get the existing record to update, if it exists:
				ResultDocList results = index.searchDocs("id:" + SimpleLuceneIndex.encodeToTerm(newRecordId));
				XMLDocReader xmlDocReader = null;
				if (results != null && results.size() > 0) {
					xmlDocReader = (XMLDocReader) results.get(0).getDocReader();
					String curCollection = xmlDocReader.getCollection();
					if (!curCollection.equals(collection))
						throw new RecordUpdateException("Record ID '" + newRecordId + "' already exists in the repository, but is in collection '" + curCollection + "'");
				}

				metadataDir.mkdirs();
				if (xmlDocReader != null)
					writeToFile = xmlDocReader.getFile();
				else {
					writeToFile = new File(metadataDir, Files.encode(newRecordId) + ".xml");
					createdFile = new File(metadataDir, Files.encode(newRecordId) + ".xml");
				}

				if (!tempFile.renameTo(writeToFile) && !Files.copy(tempFile, writeToFile))
					throw new RecordUpdateException("Unable to create new file at location '" + writeToFile.getAbsolutePath() + ".' Permissions may not allow writing files.");

				fileIndexingService.indexFile(writeToFile, indexingPlugin);
				
				// Handle reindexing records that this item assigns a relationship for and the records related to this:
				if (indexRelations) {
					
					// Reindex myself to get the relations related to me (JW - Is this necessary? there may be another way... pull in the relations in the reader using ID? Check if there are relations to me before reindexing...)
					reindexRecord(newRecordId, null, saveXmlAsFile, false);
					
					// Reindex all records that this item assigns a relationship for...
					List idsToReindex = writer.getRelatedIds();
					if (idsToReindex != null) {
						for (int i = 0; i < idsToReindex.size(); i++) {
							String reindexId = (String) idsToReindex.get(i);
							prtln("putRecord() reindexing related ID: " + reindexId);
							try {
								// To avoid loop, do not reindex relations:
								reindexRecord(reindexId, null, saveXmlAsFile, false);
							} catch (RecordUpdateException t) {
								// Annotated record ID does not exist (probably OK...)
								prtlnErr("putRecord() error reindex related ID '" + reindexId + "': " + t);
							} catch (Throwable t) {
								prtlnErr("putRecord() successfully saved record '" + newRecordId + "' but unable to (re)index the related resource '" + reindexId + "': " + t);
							}
						}
					}

					List urlsToReindex = writer.getRelatedUrls();
					if (urlsToReindex != null && urlsToReindex.size() > 0 ) {
						BooleanQuery urlQ = new BooleanQuery();
						for (int j = 0; j < urlsToReindex.size(); j++) {
							urlQ.add(new TermQuery(new Term("url", (String)urlsToReindex.get(j))), BooleanClause.Occur.SHOULD);
						}
						
						prtln("putRecord() reindexing related URLs query: " + urlQ);
						ResultDocList relatedRecords = getIndex().searchDocs(urlQ);
						if(	relatedRecords != null ) {					
							for (int i = 0; i < relatedRecords.size(); i++) {
								xmlDocReader = (XMLDocReader) relatedRecords.get(i).getDocReader();
								prtln("putRecord() reindexing by related URL ID: " + xmlDocReader.getId());
								reindexRecord(xmlDocReader.getId(), null, saveXmlAsFile, false);
							}
						}
					}
				}

			} catch (Throwable e) {
				thrown = e;
				// Delete the new record file if it has been created:
				try {
					if (createdFile != null)
						createdFile.delete();
				} catch (Throwable t) {}
				e.printStackTrace();
			} finally {
				if (!saveXmlAsFile) {
					// Delete the file, but save the directories so FileIndexingService keeps the records...
					if (writeToFile != null)
						writeToFile.delete();

					/* File formatDir = metadataDir.getParentFile();
				try {
					metadataDir.delete();
				} catch (Throwable t) {
					prtlnErr("Unable to delete '" + metadataDir + "': " + t);
				}
				try {
					formatDir.delete();
				} catch (Throwable t) {
					prtlnErr("Unable to delete '" + formatDir + "': " + t);
				} */
				}
			}
			if (thrown != null)
				throw new RecordUpdateException(thrown.getMessage());

			return newRecordId;
		}
	}


	/**
	 *  Deletes a record from the repository if one exists with the given ID. Returns false if no such record
	 *  exists, else true if the deletion was successful. Reindexes all records that were related to the item,
	 *  such as annotations, to remove related fields from the index.
	 *
	 * @param  id             The unique identifier for the record, for example DLESE-000-000-000-001.
	 * @return                True if the deletion was successful, false if no such record exists.
	 * @exception  Exception  If unable to delete the record for any reason
	 */
	public boolean deleteRecord(String id)
		 throws Exception {
		synchronized (publicUpdateApiLock) {
			ResultDocList results = getIndex().searchDocs("id:" + SimpleLuceneIndex.encodeToTerm(id));
			if (results == null || results.size() == 0)
				return false;

			// Determine if we need to re-index any related documents to remove data from the index:
			XMLDocReader xmlDocReader = (XMLDocReader) results.get(0).getDocReader();			
			List idsToReindex = xmlDocReader.getIdsOfRecordsWithAssignedRelationships();			
			boolean saveXmlAsFile = xmlDocReader.fileExists();		
			
			/* String xmlFormat = xmlDocReader.getDoctype();
			String relatedRecordId = null;
			boolean saveXmlAsFile = true;
			if (xmlFormat.equals("dlese_anno")) {
				relatedRecordId = ((DleseAnnoDocReader) xmlDocReader).getItemId();
				saveXmlAsFile = xmlDocReader.fileExists();
			} */

			// Delete the files, if they exist:
			for (int i = 0; i < results.size(); i++) {
				File delFile = ((FileIndexingServiceDocReader) results.get(i).getDocReader()).getFile();
				if (delFile.exists()) {
					delFile.delete();
					if (delFile.exists())
						throw new Exception("Unable to delete file " + delFile.getAbsolutePath() + " from disk. Most likely cause is access denied.");
				}
			}	
			
			// Delete the record from the index:
			index.removeDocs("id", SimpleLuceneIndex.encodeToTerm(id));

			
			// Handle reindexing recoreds that this item had assigned a relationship for:
			if (idsToReindex != null) {
				for (int i = 0; i < idsToReindex.size(); i++) {
					String reindexId = (String) idsToReindex.get(i);
					prtln("deleteRecord() reindexing (former) related ID: " + reindexId);
					try {
						// To avoid loop, do not reindex relations:
						reindexRecord(reindexId, null, saveXmlAsFile, false);	
					} catch (Throwable t) {
						prtlnErr("deleteRecord() error reindexind related ID '" + reindexId + "': " + t);
					}
				}
			}
			
			// Re-index the related doc (if this is an annotation) to remove related data from the index:
			/* if (relatedRecordId != null) {
				try {
					reindexRecord(relatedRecordId, null, saveXmlAsFile, false);
				} catch (RecordUpdateException t) {
					// Related record ID does not exist (probably OK...)
				} catch (Throwable t) {
					prtlnErr("deleteRecord() successfully deleted record '" + id + "' but unable to (re)index the annotated resource '" + relatedRecordId + "': " + t);
				}
			} */

			return true;
		}
	}


	/**
	 *  Reindexes an existing record in the repository, updating any related data from outside of the record XML
	 *  such as annotations or data from a FileIndexingPlugin, replacing the existing index entry. This is
	 *  equivilent to calling #getRecord to fetch the record XML and then calling #putRecord with the same XML.
	 *
	 * @param  id                         A unique identifier for the record, for example DLESE-000-000-000-001.
	 * @param  indexingPlugin             A plugin to use while indexing, or null to specify none. This plugin
	 *      takes precedence over a previously configured plugin. If none is supplied here, the previously
	 *      configured plugin will be used if available.
	 * @param  saveXmlAsFile              True to save the XML record as a file on disc as well as in the index,
	 *      false to only save it in the index
	 * @param  indexRelations             True to index the records that this item assigns a relation for
	 * @return                            The ID of the record that was reindexed.
	 * @exception  RecordUpdateException  If record ID does not exist or unable to reindex the record for any
	 *      reason
	 */
	public String reindexRecord(String id,
	                            FileIndexingPlugin indexingPlugin,
	                            boolean saveXmlAsFile,
	                            boolean indexRelations)
		 throws RecordUpdateException {
		synchronized (publicUpdateApiLock) {
			ResultDoc resultDoc = getRecord(id);
			if (resultDoc == null)
				throw new RecordUpdateException("Unable to reindex record '" + id + "'. Record not found in the repository.");

			try {
				XMLDocReader xmlDocReader = (XMLDocReader) resultDoc.getDocReader();
				String recordXml = xmlDocReader.getXml();
				String xmlFormat = xmlDocReader.getNativeFormat();
				String recordId = xmlDocReader.getId();
				String collection = xmlDocReader.getCollection();
				doPutRecord(recordXml, xmlFormat, collection, recordId, indexingPlugin, saveXmlAsFile, indexRelations);

				prtln("reindexRecord() successfully re-indexed '" + recordId + "'");
				return recordId;
			} catch (Throwable t) {
				throw new RecordUpdateException("Unable to reindex record '" + id + "': " + t.getMessage());
			}
		}
	}


	/**
	 *  Instructs the repository to index all of it's files. The index will add, update or delete the entries for
	 *  each file in the repository. The indexAll parameter indicates whether to index all files regardless of
	 *  modification or only those files that have been modified, added or deleted since the last index update.
	 *  This method returns immediately and execution occurs in a background thread. Progress may be monitored
	 *  using method {@link #getIndexingMessages}.
	 *
	 * @param  observer  The FileIndexingObserver that will be notified when indexing is complete, or null to use
	 *      none
	 * @param  indexAll  True to index all files, false to index modified files only
	 */
	public final void indexFiles(FileIndexingObserver observer, boolean indexAll) {
		fileIndexingService.indexFiles(indexAll, observer);
	}


	/**
	 *  Instructs the repository to index all of the files for the given set/collection. The index will add,
	 *  update or delete the entries for each file in the collection. The indexAll parameter indicates whether to
	 *  index all files regardless of modification or only those files that have been modified, added or deleted
	 *  since the last index update. This method returns immediately and execution occurs in a background thread.
	 *  Progress may be monitored using method {@link #getIndexingMessages}.
	 *
	 * @param  set       The set key, for example 'dcc'
	 * @param  observer  The FileIndexingObserver that will be notified when indexing is complete, or null to use
	 *      none
	 * @param  indexAll  True to index all files, false to index modified files only
	 * @return           True if the collection exists in the repository, false if the collection does not exist
	 *      in the repository.
	 */
	public final boolean indexCollection(String set, FileIndexingObserver observer, boolean indexAll) {
		SetInfo si = getSetInfo(set);
		if (si == null) {
			try {
				if (observer != null)
					observer.indexingCompleted(FileIndexingObserver.INDEXING_COMPLETED_ERROR, "Collection '" + set + "' is not configured in the repository.");
			} catch (Throwable t) {}
			return false;
		}
		File dir = new File(si.getDirectory());
		fileIndexingService.indexFiles(indexAll, dir, observer);
		return true;
	}


	// ------------------------ Handle collection records ------------------------

	/**
	 *  Sets the absolute path to the collectionRecordsLocation and the metadataRecordsLocation. If supplied, the
	 *  RepositoryManager will index the Collecton Records and they can then be used to configure the collections
	 *  found in this repository. This must be called prior to calling the init() method.
	 *
	 * @param  collectionRecordsLocation  The absolute path to a directory of DLESE collection-level XML records.
	 * @param  metadataRecordsLocation    The absolute path to a directory containing item-level metadata. All
	 *      metadata files must reside in sub-directores by format and collection, for example:
	 *      metadataRecordsLocation + /adn/dcc/DLESE-000-000-000-001.xml.
	 */
	public final void setRecordsLocation(String collectionRecordsLocation, String metadataRecordsLocation) {
		collectionRecordsDir = new File(collectionRecordsLocation);
		//if (!collectionRecordsDir.isDirectory())
		//	prtlnErr("Warning. collectionRecordsDir " + collectionRecordsLocation + " is not a valid directory.");
		this.metadataRecordsLocation = metadataRecordsLocation;
	}


	/**
	 *  Gets the path for the directory of collect-level records this RepositoryManager is using, or empty string
	 *  if none is configured.
	 *
	 * @return    The collectionRecordsLocation value
	 */
	public String getCollectionRecordsLocation() {
		if (collectionRecordsDir != null)
			return collectionRecordsDir.getAbsolutePath();
		else
			return "";
	}


	/**
	 *  Gets the path for the directory of metadata records this RepositoryManager is using, or empty string if
	 *  none is configured.
	 *
	 * @return    The metadataRecordsLocation value
	 */
	public String getMetadataRecordsLocation() {
		if (this.metadataRecordsLocation != null)
			return this.metadataRecordsLocation;
		else
			return "";
	}


	/**
	 *  Put a collection in the repository. The collection can then have items added to it with the putRecord
	 *  method.
	 *
	 * @param  collectionKey               The unique collection key, for example 'dcc'. The key and record ID
	 *      are set to the same value.
	 * @param  xmlFormat                   The xml format of the records in this collection, for example
	 *      'nsdl_dc'
	 * @param  title                       The title
	 * @param  description                 The description
	 * @param  additionalMetadata          A text or XML string to be inserted into the additionalMetadata
	 *      element of the collection record, or null for none
	 * @return                             The collection record ID
	 * @exception  PutCollectionException  If error occurs, indicates the type of error
	 */
	public String putCollection(String collectionKey, String xmlFormat, String title, String description, String additionalMetadata)
		 throws PutCollectionException {
		synchronized (publicUpdateApiLock) {
			// To do: Another putCollection() method that takes a collection records - see DDSServlet
			if (collectionKey == null)
				throw new PutCollectionException("collectionKey cannot be null", PutCollectionException.ERROR_CODE_BAD_KEY);
			if (xmlFormat == null)
				throw new PutCollectionException("xmlFormat cannot be null", PutCollectionException.ERROR_CODE_BAD_FORMAT_SPECIFIER);
			if (title == null)
				throw new PutCollectionException("title cannot be null", PutCollectionException.ERROR_CODE_BAD_TITLE);

			if (!collectionKey.matches("[a-zA-Z0-9_\\-\\.]+"))
				throw new PutCollectionException("collectionKey must match [a-zA-Z0-9_\\\\-\\\\.]+ but found: " + collectionKey, PutCollectionException.ERROR_CODE_BAD_KEY);
			if (!xmlFormat.matches("[a-zA-Z0-9_\\-\\.]+"))
				throw new PutCollectionException("xmlFormat must match [a-zA-Z0-9_\\\\-\\\\.]+ but found: " + xmlFormat, PutCollectionException.ERROR_CODE_BAD_TITLE);

			String collRecordId = null;

			// Set up the collection of collections, if not already configured ----------------------
			try {
				if (!isSetConfigured("collect")) {
					//prtln("Creating collections collection...");

					if (collectionRecordsDir == null)
						throw new Exception("No location for collection records has been set.");

					org.dom4j.Document masterCollectionDoc = Dom4jUtils.getXmlDocument(Files.readFileFromJarClasspath("/org/dlese/dpc/repository/COLLECTION-RECORD-TEMPLATE.xml").toString());

					masterCollectionDoc = updateCollectionRecord(
						masterCollectionDoc,
						"Master DDS Collection of Collections",
						"The master collection contains individual records for each colleciton that resides in the DDS repository.",
						null,
						"dlese_collect",
						"collect",
						"COLLECTIONS-COLLECTION-001",
						new Date());

					String masterCollection = XMLUtils.stripXmlDeclaration(new BufferedReader(new StringReader(masterCollectionDoc.asXML()))).toString();

					collectionRecordsDir.mkdirs();
					try {
						Files.writeFile(
							masterCollection,
							new File(collectionRecordsDir, "COLLECTIONS-COLLECTION-001.xml"));
					} catch (Exception e) {
						throw new PutCollectionException("Failed to write file, possible permission problem: " + e, PutCollectionException.ERROR_CODE_IO_ERROR);
					}
					loadCollectionRecords(true);
				}
			} catch (Exception e) {
				prtlnErr("Error initializing collections: " + e);
				if (e instanceof NullPointerException)
					e.printStackTrace();
				throw new PutCollectionException(e.toString(), PutCollectionException.ERROR_CODE_INTERNAL_ERROR);
			}

			// Add or update the collection record ---------------------
			String collRecordXml = null;
			String collId = null;
			try {
				// Set the ID to be the same as the key:
				collId = collectionKey;

				Query q = new TermQuery(new Term("key", collectionKey));
				ResultDocList results = index.searchDocs(q);

				// Try searching with query parser (sometimes the term key does not work):
				if (results.size() == 0) {
					String qStr = "key:\"" + collectionKey + "\"";
					results = index.searchDocs(qStr);
				}

				Date accessionDate = new Date();

				// If we have an existing collection with this key, update the existing XML record...
				SetInfo existingSet = getSetInfo(collectionKey);
				if (results.size() > 0 && existingSet != null) {
					DleseCollectionDocReader collDoc =
						(DleseCollectionDocReader) results.get(0).getDocReader();

					// Dissallow changing the xmlFormat:
					if (!collDoc.getFormatOfRecords().equals(xmlFormat)) {
						throw new PutCollectionException("Collection '" + collectionKey +
							"' already exists with xmlFormat '" + collDoc.getFormatOfRecords() +
							"'. Cannot change the xmlFormat to '" + xmlFormat + ".' Changing the xmlFormat is not allowed.", PutCollectionException.ERROR_CODE_COLLECTION_EXISTS_IN_ANOTHER_FORMAT);
					}

					org.dom4j.Document xmlDoc = collDoc.getXmlDoc();

					prtln("PutCollection case 1 update existing CollectionRecord. additionalMetadata");
					xmlDoc = updateCollectionRecord(xmlDoc, title, description, additionalMetadata, "DDS_UNCHANGED", "DDS_UNCHANGED", "DDS_UNCHANGED", null);

					collId = xmlDoc.valueOf("/*[local-name()='collectionRecord']/*[local-name()='metaMetadata']/*[local-name()='catalogEntries']/*[local-name()='catalog']/@entry");

					collRecordXml = XMLUtils.stripXmlDeclaration(new BufferedReader(new StringReader(xmlDoc.asXML()))).toString();

					existingSet.setName(title);
					if (description != null && description.trim().length() > 0)
						existingSet.setDescription(description);
					else
						existingSet.setDescription("");

					putRecord(collRecordXml, "dlese_collect", "collect", null, true);
					replaceSetInfo(collectionKey, existingSet);
				}
				// If no previous Collection document exists, create a new one from template:
				else {
					prtln("PutCollection case 2 add new CollectionRecord");

					description = (description == null ? "" : description);

					org.dom4j.Document xmlDoc = Dom4jUtils.getXmlDocument(Files.readFileFromJarClasspath("/org/dlese/dpc/repository/COLLECTION-RECORD-TEMPLATE.xml").toString());

					xmlDoc = updateCollectionRecord(xmlDoc, title, description, additionalMetadata, xmlFormat, collectionKey, collId, accessionDate);

					collRecordXml = XMLUtils.stripXmlDeclaration(new BufferedReader(new StringReader(xmlDoc.asXML()))).toString();

					// Index the collection record:
					collRecordId = putRecord(collRecordXml, "dlese_collect", "collect", null, true);

					// Add the collection (set) to the repository manager:
					String isEnabled = "true";
					String dir = this.metadataRecordsLocation + "/" + xmlFormat + "/" + collectionKey;
					SetInfo set = new SetInfo(
						title,
						collectionKey,
						description,
						isEnabled,
						dir,
						xmlFormat,
						collId);
					set.setAccessionStatus("accessioned");

					prtln("Adding new set: " + set.toString());
					addSetInfo(set);

					//loadCollectionRecords(false);
				}
			} catch (Exception e) {
				if (e instanceof PutCollectionException)
					throw new PutCollectionException(e.getMessage(), ((PutCollectionException) e).getErrorCode());

				prtlnErr("putCollection() error for collection: '" + title + "' collectionKey:" + collectionKey + " xmlFormat:" + xmlFormat + e);

				e.printStackTrace();
				// If new collection but failure, remove record:
				try {
					if (collRecordId != null) {
						prtln("Due to previous errors, calling deleteRecord() for ID: " + collRecordId);
						deleteRecord(collRecordId);
					}
				} catch (Throwable e2) {
					throw new PutCollectionException("There was an exception creating the collection: " + e.toString() + " plus an addition exception when trying to delete the temporary collection record: " + e2.toString(), PutCollectionException.ERROR_CODE_INTERNAL_ERROR);
				}

				throw new PutCollectionException(e.toString(), PutCollectionException.ERROR_CODE_INTERNAL_ERROR);
			}

			return collId;
		}
	}


	/**
	 *  Updates a collection record Document.
	 *
	 * @param  collectionRecordDoc  A collection record Document (may be from the template)
	 * @param  title                Title (must not be null)
	 * @param  description          Description (can be null)
	 * @param  additionalMetadata   Additional metadata (String, XML, or null)
	 * @param  xmlFormat            Format specifier or "DDS_UNCHANGED" to leave unchanged
	 * @param  key                  Collection key or "DDS_UNCHANGED" to leave unchanged
	 * @param  id                   Record ID or "DDS_UNCHANGED" to leave unchanged
	 * @param  accessionDate        Accession date or null to leave unchanged
	 * @return                      An updated Document
	 * @exception  Exception        If error
	 */
	private org.dom4j.Document updateCollectionRecord(
	                                                  org.dom4j.Document collectionRecordDoc,
	                                                  String title,
	                                                  String description,
	                                                  String additionalMetadata,
	                                                  String xmlFormat,
	                                                  String key,
	                                                  String id,
	                                                  Date accessionDate) throws Exception {

		if (!key.equals("DDS_UNCHANGED"))
			collectionRecordDoc.selectSingleNode("/*[local-name()='collectionRecord']/*[local-name()='access']/*[local-name()='key']").setText(key);

		if (!xmlFormat.equals("DDS_UNCHANGED"))
			collectionRecordDoc.selectSingleNode("/*[local-name()='collectionRecord']/*[local-name()='access']/*[local-name()='key']/@libraryFormat").setText(xmlFormat);

		if (!id.equals("DDS_UNCHANGED"))
			collectionRecordDoc.selectSingleNode("/*[local-name()='collectionRecord']/*[local-name()='metaMetadata']/*[local-name()='catalogEntries']/*[local-name()='catalog']/@entry").setText(id);

		if (accessionDate != null)
			collectionRecordDoc.selectSingleNode("/*[local-name()='collectionRecord']/*[local-name()='approval']/*[local-name()='collectionStatuses']/*[local-name()='collectionStatus']/@date").setText(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").format(accessionDate));

		// Update the short title:
		Node shortTitleNode = collectionRecordDoc.selectSingleNode("/*[local-name()='collectionRecord']/*[local-name()='general']/*[local-name()='shortTitle']");
		if (shortTitleNode != null)
			shortTitleNode.setText(title);

		// For previous repositories that are upgraded, remove the fullTitle (legacy)
		Node fullTitleNode = collectionRecordDoc.selectSingleNode("/*[local-name()='collectionRecord']/*[local-name()='general']/*[local-name()='fullTitle']");
		if (fullTitleNode != null)
			fullTitleNode.detach();

		Node descriptionNode = collectionRecordDoc.selectSingleNode("/*[local-name()='collectionRecord']/*[local-name()='general']/*[local-name()='description']");
		if (descriptionNode == null)
			descriptionNode = ((Branch) collectionRecordDoc.selectSingleNode("/*[local-name()='collectionRecord']/*[local-name()='general']")).addElement("description");

		if (description == null || description.trim().length() == 0)
			description = title;
		descriptionNode.setText(description);

		// Delete and then re-create the additionalMetadata node:
		Node additionalMetadataNode = collectionRecordDoc.selectSingleNode("/*[local-name()='collectionRecord']/*[local-name()='additionalMetadata']");
		if (additionalMetadataNode != null)
			additionalMetadataNode.detach();

		if (additionalMetadata != null && additionalMetadata.trim().length() > 0) {
			org.dom4j.Document additionalMetadataDocument;
			try {
				additionalMetadataDocument = Dom4jUtils.getXmlDocument("<additionalMetadata>" + additionalMetadata + "</additionalMetadata>");
			} catch (Throwable t) {
				throw new PutCollectionException("Error processing additionalMetadata argument: " + t.getMessage(), PutCollectionException.ERROR_CODE_BAD_ADDITIONAL_METADATA);
			}
			((Branch) collectionRecordDoc.selectSingleNode("/*[local-name()='collectionRecord']")).add(additionalMetadataDocument.getRootElement());
		}

		return collectionRecordDoc;
	}


	/**
	 *  Delete a collection and all its records from the repository.
	 *
	 * @param  collectionKey  The unique collection key, for example 'dcc'
	 * @return                True if the collection existed and was deleted, false if no such collection exists
	 * @exception  Exception  Description of the Exception
	 */
	public boolean deleteCollection(String collectionKey) throws Exception {
		synchronized (publicUpdateApiLock) {
			if (!isSetConfigured(collectionKey))
				return false;
			if (collectionKey.equals("collect"))
				throw new Exception("The \'collect\' collection is managed internally and cannot be deleted.");

			SetInfo setInfo = getSetInfo(collectionKey);
			File filesDirectory = new File(setInfo.getDirectory());
			if (filesDirectory.isDirectory() && filesDirectory.listFiles() != null && filesDirectory.listFiles().length > 0 && !filesDirectory.canWrite())
				throw new Exception("No write permissions for directory " + filesDirectory.getAbsolutePath());

			// Remove the collection record:
			Query q = new TermQuery(new Term("key", collectionKey));
			ResultDocList results = index.searchDocs(q);

			// Try searching with query parser (sometimes the term key does not work):
			if (results.size() == 0) {
				String qStr = "key:\"" + collectionKey + "\"";
				results = index.searchDocs(qStr);
			}

			if (results.size() > 0) {
				DleseCollectionDocReader collDoc =
					(DleseCollectionDocReader) results.get(0).getDocReader();
				deleteRecord(collDoc.getId());
			}

			// Remove the set/collection configuration:
			removeSetBySetSpec(collectionKey);

			// Delete the record files from disc:
			Exception deleteException = null;
			try {
				Files.deleteDirectory(filesDirectory);
				if (filesDirectory.listFiles() != null && filesDirectory.listFiles().length > 0)
					deleteException = new Exception("Collection was removed from the index and repository, however there was a error deleting files from disk. " +
						filesDirectory.listFiles().length + " files still reside in directory " + filesDirectory.getAbsolutePath() + " - most likely cause is access denied.");
			} catch (Throwable t) {
				deleteException = new Exception("Collection was removed from the index and repository, however there was a problem deleting one or more of the record files from disk: " + t.getMessage());
			}
			if (deleteException != null)
				throw deleteException;

			return true;
		}
	}


	/**
	 *  Loads the collections found in the collection-level records.
	 *
	 * @param  indexCollectionRecs  True to index the collection records, false not to.
	 */
	public void loadCollectionRecords(boolean indexCollectionRecs) {

		// Configure off of the collection-level records, if indicated
		if (!configureFromCollectionRecords)
			return;

		//prtln("loadColletctionRecords() starting... indexCollectionRecs:" + indexCollectionRecs);

		//prtln("\nloadCollectionRecords() loading dir " + collectionRecordsDir.getAbsolutePath() + "\n");
		//prtln("Using collection records located at: " + collectionRecordsDir);
		//prtln("Using metadata records located at: " + metadataRecordsLocation);



		HashMap existingSetInfosHash = getSetInfosHashMap();
		ArrayList colls = getCollectionRecords(new ArrayList(), collectionRecordsDir);
		HashMap newSetInfos = new HashMap(colls.size());
		org.dom4j.Document doc = null;
		SetInfo newSetInfo = null;
		SetInfo existingSetInfo = null;
		ArrayList collectionDirs = new ArrayList();
		String name;
		String setSpec;
		String enabled;
		String format;
		String dir;
		String id;
		String accessionStatus;

		// Start with fresh batch of SetInfos.
		//deleteSetInfos();
		for (int i = 0; i < colls.size(); i++) {
			doc = (org.dom4j.Document) colls.get(i);
			try {
				name = ((Element) (doc.selectNodes("//*[local-name()='general']/*[local-name()='shortTitle']").get(0))).getText();
				setSpec = ((Element) (doc.selectNodes("//*[local-name()='access']/*[local-name()='key']").get(0))).getText();
				id = ((Attribute) (doc.selectNodes("//*[local-name()='metaMetadata']/*[local-name()='catalogEntries']/*[local-name()='catalog']/@entry").get(0))).getText();
				format = ((Attribute) (doc.selectNodes("//*[local-name()='access']/*[local-name()='key']/@libraryFormat").get(0))).getValue();
				dir = this.metadataRecordsLocation + "/" + format + "/" + setSpec;

				enabled = "true";
				accessionStatus = DleseCollectionFileIndexingWriter.getCurrentCollectionStatus(doc).toLowerCase();
				if (!accessionStatus.equals("accessioned"))
					enabled = "false";

				newSetInfo = new SetInfo(name,
					setSpec,
					"",
					enabled,
					dir,
					format,
					id);
				newSetInfo.setAccessionStatus(accessionStatus);

				if (existingSetInfosHash != null)
					existingSetInfo = (SetInfo) existingSetInfosHash.get(newSetInfo.getDirectory());
				else
					existingSetInfo = null;
				if (existingSetInfo != null)
					newSetInfo.setEnabled(existingSetInfo.getEnabled());

				newSetInfos.put(newSetInfo.getDirectory(), newSetInfo);
				if (format.equals("dlese_collect"))
					collectionDirs.add(dir);

				// If we haven't seen this set definition before, add it:
				if (existingSetInfo == null) {
					addSetInfo(newSetInfo);
				}
				// If the set definition has changed, remove and then add it:
				else if (!newSetInfo.equals(existingSetInfo)) {
					removeSetInfo(existingSetInfo);
					addSetInfo(newSetInfo);
				}
			} catch (Throwable e) {
				prtlnErr("Error reading collection record: " + e);
				e.printStackTrace();
			}
		}

		ArrayList existingSetInfos = getSetInfos();
		if (existingSetInfos != null) {

			for (int i = existingSetInfos.size() - 1; i >= 0; i--) {
				existingSetInfo = (SetInfo) existingSetInfos.get(i);
				// Remove if the set is not there anymore
				if (!newSetInfos.containsKey(existingSetInfo.getDirectory())) {
					removeSetInfo(i);
				}
				else {
					// Replace the set definition if it is already there
					newSetInfo = (SetInfo) newSetInfos.get(existingSetInfo.getDirectory());
					if (newSetInfo != null) {
						replaceSetInfo(i, newSetInfo);
					}
				}
			}

		}

		resetSetsData();

		// Index the collection-level records only
		if (indexCollectionRecs) {
			for (int i = 0; i < collectionDirs.size(); i++)
				fileIndexingService.indexFiles(false, new File((String) collectionDirs.get(i)), null);
		}
		//prtln("loadColletctionRecords() finished...");

	}


	/**
	 *  Recursively grab all the collection level records in the given directory and sub-directories.
	 *
	 * @param  collRecords  An empty or partially filled ArrayList of collection record XML Documents.
	 * @param  dir          A directory that contains collection-level records.
	 * @return              An ArrayList of dom4j XML Documents, one for each collection-level record.
	 */
	private ArrayList getCollectionRecords(ArrayList collRecords, File dir) {

		if (dir.isDirectory()) {
			File[] files = dir.listFiles();
			for (int i = 0; i < files.length; i++) {
				if (files[i].isDirectory())
					getCollectionRecords(collRecords, files[i]);
				else if (files[i].getName().endsWith(".xml")) {
					org.dom4j.Document doc = null;
					try {
						doc = Dom4jUtils.getXmlDocument(files[i]);
					} catch (Exception e) {
						prtlnErr("error getting collection record: " + e);
					}
					if (doc != null)
						collRecords.add(doc);
				}
			}
			return collRecords;
		}
		else {
			/*
			if(!dir.exists())
				prtlnErr("getCollectionRecords(): Directory '" + dir + "' does not exist");
			else if(dir.isFile())
				prtlnErr("getCollectionRecords(): Directory '" + dir + "' is a file, not a directory"); */
			return collRecords;
		}
	}


	// ------------------------ End handle collection records ------------------------


	/**
	 *  Gets the recordDataService attribute of the RepositoryManager object
	 *
	 * @return    The recordDataService value
	 */
	public RecordDataService getRecordDataService() {
		return recordDataService;
	}


	/**
	 *  Gets the adminEmails attribute of the RepositoryManager object
	 *
	 * @return    The adminEmails value
	 */
	public ArrayList getAdminEmails() {
		return arrayListGet(Keys.ADMIN_EMAILS, false);
	}


	/**
	 *  Gets the adminEmail attribute of the RepositoryManager object
	 *
	 * @param  i  The index of the e-mail to get
	 * @return    The adminEmails value
	 */
	public String getAdminEmail(int i) {
		return ((String) arrayListGetItem(i, Keys.ADMIN_EMAILS, false));
	}


	/**
	 *  Add the given admin e-mail to the repository. The admin e-mail is used in the Identify request. If the
	 *  given e-mail already exists, nothing is changed.
	 *
	 * @param  email  The admin e-mail to be added.
	 */
	public void addAdminEmail(String email) {
		arrayListAddItem(email, Keys.ADMIN_EMAILS);
	}


	/**
	 *  Add the given admin e-mail to the repository. The admin e-mail is used in the Identify request. If the
	 *  given e-mail already exists, nothing is changed.
	 *
	 * @param  email  The admin e-mail to be added.
	 * @param  index  DESCRIPTION
	 */
	public void replaceAdminEmail(int index, String email) {
		arrayListReplaceItem(index, email, Keys.ADMIN_EMAILS);
	}


	/**
	 *  Removes the given admin e-mail from the repository. The admin e-mail is used in the Identify request. If
	 *  no such e-mail exists, nothing is done.
	 *
	 * @param  email  The admin e-mail to be removed.
	 */
	public void removeAdminEmail(String email) {
		arrayListRemoveItem(email, Keys.ADMIN_EMAILS);
	}


	/**
	 *  Removes the given admin e-mail at the given ArrayList index from the repository. If no such e-mail
	 *  exists, nothing is done.
	 *
	 * @param  i  The admin e-mail ArrayList index to be removed.
	 */
	public void removeAdminEmail(int i) {
		arrayListRemoveItem(i, Keys.ADMIN_EMAILS);
	}


	/**
	 *  Sets the metadata namespace for the given metadataPrefix (XML format). If a metadata namespace is already
	 *  configured it will be replaced with the new value. These are used in the OAI ListMetadataFormats
	 *  response.
	 *
	 * @param  metadataPrefix     The metadata specifier, for example oai_dc, adn, dlese_anno
	 * @param  metadataNamespace  The namspace URI for the given XML format
	 * @return                    True if successful
	 */
	public boolean setMetadataNamespace(String metadataPrefix, String metadataNamespace) {
		return hashMapPutItem(metadataPrefix, metadataNamespace, Keys.METADATA_NAMESPACES);
	}


	/**
	 *  Gets the metadata namespace for the given metadataPrefix (XML format). These are used in the OAI
	 *  ListMetadataFormats response.
	 *
	 * @param  metadataPrefix  The metadata specifier, for example oai_dc, adn, dlese_anno
	 * @return                 The namspace URI for the given XML format, or null if not available
	 */
	public String getMetadataNamespace(String metadataPrefix) {
		return (String) hashMapGetItem(metadataPrefix, Keys.METADATA_NAMESPACES, true);
	}


	/**
	 *  Removes the metadata namespace for the given metadataPrefix (XML format). These are used in the OAI
	 *  ListMetadataFormats response.
	 *
	 * @param  metadataPrefix  The metadata specifier, for example oai_dc, adn, dlese_anno
	 * @return                 True if successful
	 */
	public boolean removeMetadataNamespace(String metadataPrefix) {
		return hashMapRemoveItem(metadataPrefix, Keys.METADATA_NAMESPACES);
	}


	/**
	 *  Gets a Map of metadata namespaces for all metadataPrefixes (XML formats) in the repository, or null if
	 *  none exist. These are used in the OAI ListMetadataFormats response.
	 *
	 * @return    A Map of all metadata namespaces, keyed by metadataPrefix
	 */
	public Map getMetadataNamespaces() {
		try {
			return (Map) adminData.getCopy(Keys.METADATA_NAMESPACES);
		} catch (Throwable t) {
			return null;
		}
	}


	/**
	 *  Sets the metadata schema URL for the given metadataPrefix (XML format). If a schema URL is already
	 *  configured it will be replaced with the new value. These are used in the OAI ListMetadataFormats
	 *  response.
	 *
	 * @param  metadataPrefix  The metadata specifier, for example oai_dc, adn, dlese_anno
	 * @param  metadataSchema  The schema URL for the given XML format
	 * @return                 True if successful
	 */
	public boolean setMetadataSchemaURL(String metadataPrefix, String metadataSchema) {
		return hashMapPutItem(metadataPrefix, metadataSchema, Keys.METADATA_SCHEMAS);
	}


	/**
	 *  Gets the metadata schema URL for the given metadataPrefix (XML format). These are used in the OAI
	 *  ListMetadataFormats response.
	 *
	 * @param  metadataPrefix  The metadata specifier, for example oai_dc, adn, dlese_anno
	 * @return                 The schema URL for the given XML format, or null if not available
	 */
	public String getMetadataSchemaURL(String metadataPrefix) {
		return (String) hashMapGetItem(metadataPrefix, Keys.METADATA_SCHEMAS, true);
	}


	/**
	 *  Removes the metadata schema URL for the given metadataPrefix (XML format). These are used in the OAI
	 *  ListMetadataFormats response.
	 *
	 * @param  metadataPrefix  The metadata specifier, for example oai_dc, adn, dlese_anno
	 * @return                 True if successful
	 */
	public boolean removeMetadataSchemaURL(String metadataPrefix) {
		return hashMapRemoveItem(metadataPrefix, Keys.METADATA_SCHEMAS);
	}


	/**
	 *  Gets a Map of metadata schema URLs for all metadataPrefixes (XML formats) in the repository, or null if
	 *  none exist. These are used in the OAI ListMetadataFormats response.
	 *
	 * @return    A Map of all metadata schema URLs, keyed by metadataPrefix
	 */
	public Map getMetadataSchemaURLs() {
		try {
			return (Map) adminData.getCopy(Keys.METADATA_SCHEMAS);
		} catch (Throwable t) {
			return null;
		}
	}


	/**
	 *  Sets the default schema and namespace for the given XML format, used in the OAI ListMetadataFormats
	 *  response. Value passed in must be of the form [metadataPrefix]|[schema]|[namespace], for example
	 *  'oai_dc|http://www.openarchives.org/OAI/2.0/oai_dc.xsd|http://www.openarchives.org/OAI/2.0/oai_dc/'. If a
	 *  namespace or schema value is already configured for the given metadataPrefix, no change will be made.
	 *  Values may be changed later using #setMetadataSchemaURL and #setMetadataNamespace.
	 *
	 * @param  info           String of the form [metadataPrefix]|[schema]|[namespace]
	 * @exception  Exception  If unable to parse the String properly
	 */
	public void setDefaultXmlFormatInfo(String info) throws Exception {
		if (info == null || info.trim().length() == 0)
			throw new Exception("Invalid value provided. Value was empty.");

		String[] vals = info.split("\\|");
		if (vals.length != 3)
			throw new Exception("Invalid value provided. Value provided was '" + info
				 + "' but must be of the format: [metadataPrefix]|[schema]|[namespace].");

		String metadataPrefix = vals[0].trim();
		String schema = vals[1].trim();
		String namespace = vals[2].trim();

		// Set the default schema and namespace for the given metadataPrefix, only if a value is not present
		if (getMetadataSchemaURL(metadataPrefix) == null)
			setMetadataSchemaURL(metadataPrefix, schema);
		if (getMetadataNamespace(metadataPrefix) == null)
			setMetadataNamespace(metadataPrefix, namespace);
	}


	/**
	 *  Sets the XMLConversionService used by this RepositoryManager and puts it into the index as an attribute
	 *  under the key xmlConversionService for use in the XMLDocReaders.
	 *
	 * @param  cs  The new XMLConversionService.
	 */
	public void setXMLConversionService(XMLConversionService cs) {
		xmlConversionService = cs;
		index.setAttribute("xmlConversionService", cs);
	}


	/**
	 *  Gets the XMLConversionService used by this RepositoryManager, or null if none available.
	 *
	 * @return    The XMLConversionService or null
	 */
	public XMLConversionService getXMLConversionService() {
		return xmlConversionService;
	}


	/**
	 *  Get the directory where the repository persistent data and certain configs resides, including the
	 *  collections configs, specified by the init param repositoryData.
	 *
	 * @return                The directory where the repository data and certain configs resides.
	 * @exception  Exception  If error
	 */
	public File getRepositoryDataDir() throws Exception {
		return new File(repositoryDataDir);
	}


	/**
	 *  Get the directory where the repository ItemIndexer configs are located.
	 *
	 * @return                The directory where the repository ItemIndexer configs are located
	 * @exception  Exception  If error
	 */
	public File getItemIndexerConfigDir() throws Exception {
		return itemIndexerConfigDir;
	}


	/**
	 *  Gets the OAI sets configuration XML for this repository, used to generate the ListSets OAI response. See
	 *  <a href="../../../../javadoc-includes/ListSets-config-sample.xml">sample ListSets XML config file</a> .
	 *
	 * @return    The listSetsConfigXml String, or null if none are configured
	 */
	public String getListSetsConfigXml() {
		if (setsConfigFile == null || !setsConfigFile.exists())
			return null;
		try {
			return Files.readFile(setsConfigFile).toString();
		} catch (Throwable e) {
			//prtlnErr("Unable to read ListSets config: " + e);
			return null;
		}
	}


	/**
	 *  Loads the OAI sets configuration from file and configures the corresponding set mappings in this
	 *  repository. See <a href="../../../../javadoc-includes/ListSets-config-sample.xml">sample ListSets XML
	 *  config file</a> . If not file exists, nothing is done.
	 *
	 * @exception  Exception  If error parsing or loading the config
	 */
	public void loadListSetsConfigFile() throws Exception {
		setsConfigFile = new File(repositoryDataDir + "/ListSets-config.xml");
		if (setsConfigFile.exists()) {
			String setsXml = Files.readFile(setsConfigFile).toString();
			setListSetsConfigXml(setsXml);
		}
	}


	/**
	 *  Sets the OAI sets configuration for this repository and saves the XML to file, replacing all previous
	 *  definitions. See <a href="../../../../javadoc-includes/ListSets-config-sample.xml">sample ListSets XML
	 *  config file</a> . The virtual field 'setSpec' is used to define the OAI sets in the repository.
	 *
	 * @param  xml            The new listSetsConfigXml
	 * @exception  Exception  If unable to parse
	 */
	private void setListSetsConfigXml(String xml) throws Exception {
		// If there is an Exception when parsing, throw it without modifying the current settings
		VirtualSearchFieldMapper tmpVirtualSearchFieldMapper =
			new VirtualSearchFieldMapper(getIndex().getQueryParser());
		tmpVirtualSearchFieldMapper.addVirtualFieldConfiguration(xml);

		// If all is well, store and apply the settings:
		Files.writeFile(xml, setsConfigFile);
		virtualSearchFieldMapper.remove("setSpec");
		virtualSearchFieldMapper.addVirtualFieldConfiguration(xml);

		prtln("OAI sets mappings set to:\n  " + virtualSearchFieldMapper);
	}


	/**
	 *  Removes the given OAI set definition from repository configuration.
	 *
	 * @param  setSpec  The set to remove
	 * @return          True if the set was removed
	 */
	public boolean removeOAISetSpecDefinition(String setSpec) {
		try {
			OAISetsXMLConfigManager.removeOAISetSpecDefinition(setsConfigFile, setSpec);

			// Register the sets configuration with the repsository:
			if (setsConfigFile != null && setsConfigFile.exists()) {
				String setsXml = Files.readFile(setsConfigFile).toString();
				// This writes the XML file, so no need to do it here
				setListSetsConfigXml(setsXml);
			}
		} catch (Throwable t) {
			prtlnErr("Error removing set definition: " + t);
			return false;
		}
		return true;
	}


	/**
	 *  Sets the definition for a given OAI set, initializing it in the repository and writing the ListSets
	 *  config XML to persistent file. If the given set already exists, it will be re-defined, if not, a new set
	 *  will be added to the existsing sets in the config XML file.
	 *
	 * @param  setDefinitionsForm  A bean holding the set definiton info
	 * @exception  Exception       If error
	 */
	public void setOAISetSpecDefinition(SetDefinitionsForm setDefinitionsForm) throws Exception {
		//prtln("setOAISetSpecDefinition()");

		OAISetsXMLConfigManager.setOAISetSpecDefinition(setsConfigFile, setDefinitionsForm);

		// Register the sets configuration with the repsository:
		try {
			String setsXml = Files.readFile(setsConfigFile).toString();
			// This writes the XML file, so no need to do it here
			setListSetsConfigXml(setsXml);
		} catch (Exception e) {
			prtlnErr("Unable to read OAI set configuration: " + e);
			if (e instanceof NullPointerException)
				e.printStackTrace();
			throw e;
		}
	}


	/**
	 *  Determines whether one or more OAI sets are configured for this repository.
	 *
	 * @return    True if one or more OAI sets are configured for this repository
	 */
	public boolean getHasOaiSetsConfigured() {
		return (virtualSearchFieldMapper.getNumTermsConfiguredForField("setSpec") > 0);
	}


	/**
	 *  Determines whether the given OAI set is configured in this repository.
	 *
	 * @param  setSpec  The OAI setSpec
	 * @return          True if the OAI set is configured in this repository
	 */
	public boolean getHasOaiSetConfigured(String setSpec) {
		if (setSpec == null || virtualSearchFieldMapper == null)
			return false;
		return virtualSearchFieldMapper.getIsTermConfiguredForField("setSpec", setSpec);
	}


	/**
	 *  Gets the OAI sets configured in this repository.
	 *
	 * @return    The OAI sets configured in this repository, or null
	 */
	public String[] getOaiSets() {
		return virtualSearchFieldMapper.getVirtualTerms("setSpec");
	}


	/**
	 *  Gets the Lucene Query for the given setSpec, or null if none exists.
	 *
	 * @param  setSpec  The OAI setSpec
	 * @return          The Query for the given setSpec or null
	 */
	public Query getOaiSetQuery(String setSpec) {
		return virtualSearchFieldMapper.getQuery("setSpec", setSpec);
	}


	/**
	 *  Gets the OAI sets associated with this record ID as a List of Strings, for example 'mySet' or null.
	 *
	 * @param  id  The record ID (without OAI prefix)
	 * @return     A List of OAI set Strings, or null if none
	 */
	public List getOaiSetsForId(String id) {
		if (virtualSearchFieldMapper == null)
			return null;

		String[] setSpecs = getOaiSets();

		if (setSpecs == null || setSpecs.length == 0)
			return null;

		TermQuery idQuery = new TermQuery(new Term("id", SimpleLuceneIndex.encodeToTerm(id)));
		BooleanQuery bq = null;
		ArrayList setList = new ArrayList();
		for (int i = 0; i < setSpecs.length; i++) {
			Query setQuery = virtualSearchFieldMapper.getQuery("setSpec", setSpecs[i]);
			bq = new BooleanQuery();
			bq.add(idQuery, BooleanClause.Occur.MUST);
			bq.add(setQuery, BooleanClause.Occur.MUST);
			ResultDocList results = getIndex().searchDocs(bq);
			if (results != null && results.size() > 0)
				setList.add(setSpecs[i]);
		}

		return setList;
	}


	/**
	 *  Gets the number of records that are in the given OAI set excluding deletions, or -1 if no such set is
	 *  configured.
	 *
	 * @param  setSpec  The OAI setSpec
	 * @return          The number of non-deleted records in the set or -1 if the given set does not exist
	 * @see             #getNumDeletedRecordsInSet
	 */
	public int getNumRecordsInSet(String setSpec) {
		Query query = virtualSearchFieldMapper.getQuery("setSpec", setSpec);
		if (query != null) {
			BooleanQuery bq = new BooleanQuery();
			bq.add(new TermQuery(new Term("deleted", "false")), BooleanClause.Occur.MUST);
			bq.add(query, BooleanClause.Occur.MUST);
			ResultDocList results = index.searchDocs(bq);
			if (results == null)
				return 0;
			else
				return results.size();
		}
		return -1;
	}


	/**
	 *  Gets the number of deleted records that are in the given OAI, or -1 if no such set is configured.
	 *
	 * @param  setSpec  The OAI setSpec
	 * @return          The number of deleted records in the set or -1 if the given set does not exist
	 * @see             #getNumRecordsInSet
	 */
	public int getNumDeletedRecordsInSet(String setSpec) {
		Query query = virtualSearchFieldMapper.getQuery("setSpec", setSpec);
		if (query != null) {
			BooleanQuery bq = new BooleanQuery();
			bq.add(new TermQuery(new Term("deleted", "true")), BooleanClause.Occur.MUST);
			bq.add(query, BooleanClause.Occur.MUST);
			ResultDocList results = index.searchDocs(bq);
			if (results == null)
				return 0;
			else
				return results.size();
		}
		return -1;
	}


	/**
	 *  Gets the VirtualSearchFieldMapper that defines the virtual field/term definitions for this
	 *  RepositoryManager. Dev note: may want to add RM methods to update/add stuff to the
	 *  VirtualSearchFieldMapper, but for now this method can be used to get and then modify it.
	 *
	 * @return    The virtualSearchFieldMapper or null if none
	 */
	public VirtualSearchFieldMapper getVirtualSearchFieldMapper() {
		return virtualSearchFieldMapper;
	}


	// ************* Thread for handling indexing ************

	/**
	 *  Runs the indexer at regular intervals.
	 *
	 * @author    John Weatherley
	 */
	private class FileIndexingTask extends TimerTask {
		/**  Main processing method for this thread. */
		public void run() {
			//prtln("FileIndexingTask.run()...");

			String msg = "FileIndexingTask cron timer: Begin indexing all collections...";

			boolean runIndexer = false;

			// Run every day if no DaysOfWeek indicated:
			if (indexingDaysOfWeekArray == null)
				runIndexer = true;
			// Run today, if indicated:
			else {
				Calendar now = new GregorianCalendar();
				int today = now.get(Calendar.DAY_OF_WEEK);
				for (int i = 0; i < indexingDaysOfWeekArray.length; i++) {
					if (indexingDaysOfWeekArray[i] == today) {
						runIndexer = true;
						break;
					}
				}
			}

			if (!runIndexer) {
				//prtln("FileIndexingTask NOT running today...");
				return;
			}
			else
				prtln(msg);

			//prtln("RepositoryManager.FileIndexingTask() just before loadCollectionRecords()");
			loadCollectionRecords(false);
			if (fileIndexingService != null) {
				SimpleFileIndexingObserver observer = null;

				if (debug)
					observer = new SimpleFileIndexingObserver("Background indexing timer task", "Beginning to index files...");

				fileIndexingService.indexFiles(reindexAllFiles, observer);
			}
		}
	}


	// ************* Wrapper methods for the SerializedDataManager ************

	private final boolean arrayListAddItem(Object o, String key) {
		if (o == null || key == null) {
			return false;
		}

		ArrayList list = null;
		boolean newList = false;
		try {
			list = (ArrayList) adminData.get(key);
		} catch (OIDDoesNotExistException e) {
			list = new ArrayList();
			// Create a new list if none exists.
			newList = true;
		}

		// Only add if the item is not currently in the list:
		if (list.contains(o)) {
			return false;
		}

		list.add(o);

		try {
			if (newList) {
				adminData.put(key, list);
			}
			else {
				adminData.update(key, list);
			}
		} catch (Throwable e) {
			//prtlnErr("Error adding item \"" + o + "\" to ArrayList \"" + key + ":\" " + e);
			return false;
		}

		return true;
	}



	private final Object arrayListGetItem(int i, String key, boolean asCopy) {
		if (key == null) {
			return null;
		}

		ArrayList list = null;
		try {
			if (asCopy)
				list = (ArrayList) adminData.getCopy(key);
			else
				list = (ArrayList) adminData.get(key);
			if (list != null) {
				return list.get(i);
			}
		} catch (Throwable e) {
			//prtlnErr("Error getting index \"" + i + "\" from ArrayList \"" + key + ":\" " + e);
		}
		return null;
	}



	private final boolean arrayListRemoveItem(Object o, String key) {
		if (o == null || key == null) {
			return false;
		}

		ArrayList list = null;
		try {
			list = (ArrayList) adminData.get(key);
			if (list != null) {
				list.remove(o);
				adminData.update(key, list);
			}
			else {
				return false;
			}
		} catch (Throwable e) {
			//prtlnErr("Error removing item \"" + o + "\" from ArrayList \"" + key + ":\" " + e);
			return false;
		}
		return true;
	}



	private final boolean arrayListRemoveItem(int i, String key) {
		if (key == null) {
			return false;
		}

		ArrayList list = null;
		try {
			list = (ArrayList) adminData.get(key);
			if (list != null) {
				list.remove(i);
				adminData.update(key, list);
			}
			else {
				return false;
			}
		} catch (Throwable e) {
			//prtlnErr("Error removing index \"" + i + "\" from ArrayList \"" + key + ":\" " + e);
			return false;
		}
		return true;
	}



	private final boolean arrayListReplaceItem(int index, Object o, String key) {
		if (o == null || key == null) {
			return false;
		}

		ArrayList list = null;
		try {
			list = (ArrayList) adminData.get(key);
			if (list != null) {
				list.set(index, o);

				adminData.update(key, list);
			}
			else {
				return false;
			}
		} catch (Throwable e) {
			//prtlnErr("Error removing index \"" + index + "\" from ArrayList \"" + key + ":\" " + e);
			return false;
		}
		return true;
	}


	private final ArrayList arrayListGet(String key, boolean asCopy) {
		if (key == null) {
			return null;
		}

		try {
			Object o;
			if (asCopy)
				o = adminData.getCopy(key);
			else
				o = adminData.get(key);

			if (o instanceof ArrayList)
				return (ArrayList) o;
			else {
				//prtlnErr("arrayListGet(): the object retrieved was not an ArrayList");
				return null;
			}
		} catch (OIDDoesNotExistException e) {
			//prtln("arrayListGet() OID does not exist: " + e);
			return null;
		} catch (Throwable e) {
			//prtlnErr("arrayListGet() caught exception: " + e);
			return null;
		}
	}



	private final boolean hashMapPutItem(String key, Object o, String hashMapPersistanceKey) {
		if (o == null || key == null || hashMapPersistanceKey == null) {
			return false;
		}

		HashMap hashMap = null;
		boolean newMap = false;
		try {
			hashMap = (HashMap) adminData.get(hashMapPersistanceKey);
		} catch (OIDDoesNotExistException e) {
			hashMap = new HashMap();
			// Create a new list if none exists.
			newMap = true;
		}

		hashMap.put(key, o);

		try {
			if (newMap) {

				adminData.put(hashMapPersistanceKey, hashMap);
			}
			else {
				adminData.update(hashMapPersistanceKey, hashMap);
			}
		} catch (Throwable e) {
			//prtlnErr("Error setting item \"" + key + "\" to HashMap \"" + hashMapPersistanceKey + ":\" " + e);
			return false;
		}

		return true;
	}


	private final Object hashMapGetItem(String key, String hashMapPersistanceKey, boolean asCopy) {
		if (key == null || hashMapPersistanceKey == null) {
			return null;
		}

		HashMap hashMap = null;
		try {
			if (asCopy)
				hashMap = (HashMap) adminData.getCopy(hashMapPersistanceKey);
			else
				hashMap = (HashMap) adminData.get(hashMapPersistanceKey);

			if (hashMap != null)
				return hashMap.get(key);
		} catch (Throwable e) {
			//prtlnErr("Error getting item \"" + key + "\" from HashMap \"" + hashMapPersistanceKey + ":\" " + e);
		}
		return null;
	}



	private final boolean hashMapRemoveItem(String key, String hashMapPersistanceKey) {
		if (hashMapPersistanceKey == null || key == null) {
			return false;
		}

		HashMap hashMap = null;
		try {
			hashMap = (HashMap) adminData.get(hashMapPersistanceKey);
			if (hashMap != null) {
				hashMap.remove(key);
				adminData.update(hashMapPersistanceKey, hashMap);
			}
			else {
				return false;
			}
		} catch (Throwable e) {
			//prtlnErr("Error removing item \"" + key + "\" from ArrayList \"" + hashMapPersistanceKey + ":\" " + e);
			return false;
		}
		return true;
	}



	//================================================================

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
	protected final void prtlnErr(String s) {
		System.err.println(getDateStamp() + " RepositoryManager ERROR: " + s);
	}



	/**
	 *  Output a line of text to standard out, with datestamp, if debug is set to true.
	 *
	 * @param  s  The String that will be output.
	 */
	protected final void prtln(String s) {
		if (debug) {
			System.out.println(getDateStamp() + " RepositoryManager: " + s);
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

