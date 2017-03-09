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
package org.dlese.dpc.dds;

import org.dlese.dpc.repository.*;
import org.dlese.dpc.repository.indexing.*;
import org.dlese.dpc.repository.action.*;
import org.dlese.dpc.datamgr.*;
import org.dlese.dpc.repository.action.form.*;
import org.dlese.dpc.action.*;
import org.dlese.dpc.xml.*;
import org.dlese.dpc.dds.ndr.*;
import org.dlese.dpc.util.Files;
import org.dlese.dpc.ndr.request.*;
import org.dlese.dpc.services.dds.action.*;

import java.io.*;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;

import org.dlese.dpc.index.*;
import org.dlese.dpc.index.writer.*;
import org.dlese.dpc.vocab.*;
import java.text.*;
import org.dlese.dpc.webapps.tools.*;
import org.dlese.dpc.dds.action.*;
import org.dlese.dpc.propertiesmgr.*;

/**
 *  Provided as an administrative and intialization servlet for the Digital Discovery System (DDS).
 *
 * @author    John Weatherley, Dave Deniman, Ryan Deardorff
 */
public class DDSServlet extends HttpServlet {

	final static int VALID_REQUEST = 0;
	final static int REQUEST_EXCEPTION = -1;
	final static int UNRECOGNIZED_REQUEST = 1;
	final static int NO_REQUEST_PARAMS = 2;
	final static int INITIALIZING = 3;
	final static int NOT_INITIALIZED = 4;
	final static int INVALID_CONTEXT = 5;
	final static String DIRECTORY_DATA_DIR = "file_monitor_metadata";
	private String repositoryConfigDirPath = null;
	private static boolean isInitialized = false;
	private SimpleLuceneIndex index;
	private RepositoryManager rm;
	private static String docRoot = null;

	// Possible values: fileSystem, external
	private String recordDataSource = "fileSystem";

	private boolean debug = true;


	/**  Constructor for the DDSServlet object */
	public DDSServlet() { }


	/**
	 *  The standard <code>HttpServlet</code> init method, called only when the servlet is first loaded.
	 *
	 * @param  config
	 * @exception  ServletException
	 */
	public void init(ServletConfig config) throws ServletException {
		super.init(config);

		try {

			if (isInitialized) {
				prtlnErr("DDS has already been initialized. Call to DDSServlet.init() aborted...");
				return;
			}

			isInitialized = true;

			ServletContext servletContext = getServletContext();

			//String analysisType = (String) config.getInitParameter( "analysisType" );
			String defaultField = (String) config.getInitParameter("defaultField");
			String collectionKey = (String) config.getInitParameter("collectionKey");

			// Context init params set in the context definition in server.xml or web.xml:
			String queryLogFile = getAbsolutePath((String) servletContext.getInitParameter("queryLogFile"));
			String repositoryData = getAbsolutePath((String) servletContext.getInitParameter("repositoryData"));
			String indexLocation = getAbsolutePath((String) servletContext.getInitParameter("indexLocation"));
			String dbURL = servletContext.getInitParameter("dbURL");
			String idMapperExclusionFile = servletContext.getInitParameter("idMapperExclusionFile");
			String collBaseDir = getAbsolutePath((String) servletContext.getInitParameter("collBaseDir"));
			String collectionRecordsLocation = (String) servletContext.getInitParameter("collectionRecordsLocation");
			String maxNumResultsDDSWS = (String) servletContext.getInitParameter("maxNumResultsDDSWS");
			String adminEmailDDSWS = (String) servletContext.getInitParameter("adminEmailDDSWS");

			// Set the max number of service results allowed in DDSWS:
			int maxDDSWSResults = 1000;
			try {
				maxDDSWSResults = Integer.parseInt(maxNumResultsDDSWS);
			} catch (Throwable t) {}
			DDSServicesAction.setMaxSearchResults(maxDDSWSResults);
			servletContext.setAttribute("maxNumResultsDDSWS", Integer.toString(maxDDSWSResults));

			// Set the admin e-mail address advertised for DDSWS:
			if (adminEmailDDSWS == null)
				adminEmailDDSWS = "";
			servletContext.setAttribute("adminEmailDDSWS", adminEmailDDSWS);

			boolean indexCollectionRecords = true;

			// Set up the data source(s). Possible values are: fileSystem (default), or list of ItemIndexers:
			recordDataSource = (String) servletContext.getInitParameter("recordDataSource");
			if (recordDataSource == null)
				recordDataSource = "fileSystem";
			servletContext.setAttribute("recordDataSource", recordDataSource);

			// Setings for when IndexinManager is being used:
			if (!recordDataSource.equals("fileSystem")) {
				indexCollectionRecords = false;
			}

			// By default, only use records located in the ../dlese_collect/collect directory to configure
			// the collections for this repository:
			if (collectionRecordsLocation == null) {
				collectionRecordsLocation = getAbsolutePath(collBaseDir + "/dlese_collect/collect");
			}
			else {
				collectionRecordsLocation = getAbsolutePath(collectionRecordsLocation);
			}
			String enableNewSets = servletContext.getInitParameter("enableNewSets");
			String filterDups = servletContext.getInitParameter("filterDups");
			String resourceResultLinkRedirectURL = (String) servletContext.getInitParameter("resourceResultLinkRedirectURL");
			if ((resourceResultLinkRedirectURL == null) || (resourceResultLinkRedirectURL.equals("none"))) {
				resourceResultLinkRedirectURL = "";
			}
			else if (!resourceResultLinkRedirectURL.endsWith("/"))
				resourceResultLinkRedirectURL = resourceResultLinkRedirectURL + "/";

			// Indexing start time in 24hour time, for example 0:35 or 23:35
			String indexingStartTime = (String) servletContext.getInitParameter("indexingStartTime");
			if (indexingStartTime == null || indexingStartTime.equalsIgnoreCase("disabled"))
				indexingStartTime = null;

			// Indexing days of the week as a comma separated list of integers, for example 1,3,5 where 1=Sunday, 2=Monday, 7=Saturday etc.
			String indexingDaysOfWeek = (String) servletContext.getInitParameter("indexingDaysOfWeek");
			if (indexingDaysOfWeek == null || indexingDaysOfWeek.equalsIgnoreCase("all"))
				indexingDaysOfWeek = null;

			String debugMemory = servletContext.getInitParameter("debugMemory");
			if (((String) servletContext.getInitParameter("debug")).toLowerCase().equals("true")) {
				debug = true;
				prtln("Outputting debug info");
			}
			else {
				debug = false;
				prtln("Debug info disabled");
			}

			// Set all debugging:
			RepositoryManager.setDebug(debug);
			FileIndexingService.setDebug(debug);
			FileIndexingServiceWriter.setDebug(debug);
			//SimpleLuceneIndex.setDebug( debug );
			SimpleQueryAction.setDebug(debug);
			RepositoryForm.setDebug(debug);
			SerializedDataManager.setDebug(debug);
			RepositoryAdminAction.setDebug(debug);
			RepositoryAction.setDebug(debug);
			RecordDataService.setDebug(debug);
			DDSAdminQueryAction.setDebug(debug);
			DDSQueryAction.setDebug(debug);
			SimpleNdrRequest.setDebug(debug);
			CollectionIndexer.setDebug(debug);


			// Remove any Lucene locks that may have persisted from a previous dirty shut-down:
			String tempDirPath = System.getProperty("java.io.tmpdir");
			if (tempDirPath != null) {
				File tempDir = new File(tempDirPath);

				FilenameFilter luceneLockFilter =
					new FilenameFilter() {
						public boolean accept(File dir, String name) {
							return (name.startsWith("lucene") && name.endsWith(".lock"));
						}
					};

				File[] tempFiles = tempDir.listFiles(luceneLockFilter);

				if (tempFiles != null) {
					for (int i = 0; i < tempFiles.length; i++) {
						prtlnErr("DDSServlet startup: Removing lucene lock file: " + tempFiles[i].getAbsolutePath());
						tempFiles[i].delete();
					}
				}
			}

			prtln("Using collection-level metadata files located at " + collectionRecordsLocation);
			prtln("Using metadata files located at " + collBaseDir);
			prtln("Using index located at " + indexLocation);

			// Load metadata controlled vocabularies:
			String vocabConfigDir = getAbsolutePath((String) servletContext.getInitParameter("vocabConfigDir"));
			String sqlDriver = (String) servletContext.getInitParameter("sqlDriver");
			String sqlURL = (String) servletContext.getInitParameter("sqlURL");
			String sqlUser = (String) servletContext.getInitParameter("sqlUser");
			String sqlPassword = (String) servletContext.getInitParameter("sqlPassword");
			String annotationPathwaysSchemaUrl = (String) servletContext.getInitParameter("annotationPathwaysSchemaUrl");
			String vocabTextFile = (String) servletContext.getInitParameter("vocabTextFile");

			//String idmapProps = (String) context.getInitParameter("idmapProps");

			// ORIGINAL vocabs based on terms/groups files - stores in conext under "MetadataVocab"
			/* new LoadMetadataVocabs( vocabConfigDir,
		 *"DDS_Vocabulary.xml",
		 *"org.apache.xerces.parsers.SAXParser",
		 *(ServletContext)getServletContext(),
		 *sqlDriver, sqlURL, sqlUser, sqlPassword, vocabTextFile );        */
			// The MetadataVocabServlet puts the MetadataVocab object in the context under "MetadataVocab"
			MetadataVocab metadataVocab = (MetadataVocab) servletContext.getAttribute("MetadataVocab");

			// Launch and init the RepositoryManager.
			File repositoryDataDir = new File(getAbsolutePath(repositoryData));
			if (!repositoryDataDir.exists()) {
				prtln("Created directory " + repositoryDataDir.getAbsolutePath());
				repositoryDataDir.mkdir();
			}

			// Get the config directory.
			File repositoryConfigDir = null;
			try {
				repositoryConfigDir = new File(getAbsolutePath((String) servletContext.getInitParameter("repositoryConfigDir")));
				repositoryConfigDirPath = repositoryConfigDir.getAbsolutePath();
			} catch (Throwable e) {
				prtlnErr("Error getting repositoryConfigDir: " + e);
			}

			// Get the ItemIndexer config directory.
			File itemIndexerConfigDir = null;
			try {
				itemIndexerConfigDir = new File(getAbsolutePath((String) servletContext.getInitParameter("itemIndexerConfigDir")));
			} catch (Throwable e) {
				prtlnErr("Error getting itemIndexerConfigDir: " + e);
			}

			// If an IndexingManager is being used, don't run the RepositoryManager file indexing timer.
			String indexingStartTimeFileSystem = indexingStartTime;
			if (!recordDataSource.equals("fileSystem"))
				indexingStartTimeFileSystem = null;

			prtln("Starting up RepositoryManager");
			rm = new RepositoryManager(
				repositoryConfigDir,
				itemIndexerConfigDir,
				repositoryDataDir.getAbsolutePath(),
				indexLocation,
				indexingStartTimeFileSystem,
				indexingDaysOfWeek,
				new RecordDataService(dbURL, metadataVocab, collBaseDir, annotationPathwaysSchemaUrl),
				true, true);

			// Set up the default metadata audience and language used by the indexer and search resultDocs for display:
			String metadataVocabAudience = (String) servletContext.getInitParameter("metadataVocabAudience");
			if (metadataVocabAudience == null) {
				metadataVocabAudience = "community";
			}
			String metadataVocabLanguage = (String) servletContext.getInitParameter("metadataVocabLanguage");
			if (metadataVocabLanguage == null) {
				metadataVocabLanguage = "en-us";
			}

			rm.setMetadataVocabAudienceDefault(metadataVocabAudience);
			rm.setMetadataVocabLanguageDefault(metadataVocabLanguage);

			// Use collection-level records to configre the collections
			rm.setRecordsLocation(collectionRecordsLocation, collBaseDir);

			// Disable regular OAI-PMH responses to ListRecords and ListIdentifiers requests (accept ODL requests only)
			String oaiPmhEnabled = (String) servletContext.getInitParameter("oaiPmhEnabled");
			if (oaiPmhEnabled != null && oaiPmhEnabled.equals("false"))
				rm.setIsOaiPmhEnabled(false);

			// Set the location of the file used to exclude IDs in the IDMapper service
			if (idMapperExclusionFile != null && idMapperExclusionFile.length() > 0 && !idMapperExclusionFile.equals("none")) {
				prtln("Using IDMapper ID exclusion file located at: " + idMapperExclusionFile);
				rm.setIdMapperExclusionFilePath(idMapperExclusionFile);
			}

			// Set up a dir for the record meta-metadata
			File recordMetaMetadataDir = new File(repositoryDataDir, "record_meta_metadata");
			recordMetaMetadataDir.mkdir();
			servletContext.setAttribute("recordMetaMetadataDir", recordMetaMetadataDir);

			// Set up the configured FileIndexingPlugins prior to calling rm.init()
			setFileIndexingPlugins(rm);

			// Make the RepositoryManager available to the beans that use it:
			if (rm.init(indexCollectionRecords) == 1) {
				prtln("rm.init() successful...");
				servletContext.setAttribute("repositoryManager", rm);
			}
			else {
				String initErrorMsg = "DDSServlet:  error initializing the repositoryManager";
				prtlnErr(initErrorMsg);
				throw new ServletException(initErrorMsg);
			}

			// Load DDS-related properties from file
			Properties ddsConfigProperties = null;
			try {
				ddsConfigProperties = new PropertiesManager(repositoryConfigDirPath + "/dds_config.properties");
				servletContext.setAttribute("ddsConfigProperties", ddsConfigProperties);
			} catch (Throwable t) {
				prtlnErr("Error loading DDS config properties: " + t);
			}

			FileIndexingService fileIndexingService = rm.getFileIndexingService();

			// Grab the repository index.
			index = rm.getIndex();
			index.setOperator(SimpleLuceneIndex.DEFAULT_AND);

			// Set up the dup items index.
			SimpleLuceneIndex dupItemsIndex =
				new SimpleLuceneIndex(index.getIndexLocation() + "/dup_items_index", index.getAnalyzer());
			dupItemsIndex.setOperator(SimpleLuceneIndex.DEFAULT_AND);
			dupItemsIndex.setAttribute("repositoryManager", rm);
			fileIndexingService.setAttribute("dupItemsIndex", dupItemsIndex);
			fileIndexingService.setAttribute("filterDups", filterDups);
			index.setAttribute("dupItemsIndex", dupItemsIndex);
			index.setAttribute("repositoryManager", rm);
			if (debugMemory != null) {
				index.setAttribute("debugMemory", debugMemory);
			}
			rm.setDupItemsIndex(dupItemsIndex);

			if (collBaseDir != null && !collBaseDir.equalsIgnoreCase("null")) {
				index.setAttribute("collBaseDir", getAbsolutePath(collBaseDir));
				dupItemsIndex.setAttribute("collBaseDir", getAbsolutePath(collBaseDir));
				fileIndexingService.setAttribute("collBaseDir", getAbsolutePath(collBaseDir));
			}

			// Set up an XMLConversionService for use in DDS web service, OAI and elsewhere:
			if (rm.getXMLConversionService() == null) {
				File xslFilesDirecoryPath = new File(GeneralServletTools.getAbsolutePath(
					"WEB-INF/xsl_files/", getServletContext()));
				File xmlCachDir = new File(GeneralServletTools.getAbsolutePath(
					repositoryData + File.separator + "converted_xml_cache", getServletContext()));

				try {
					XMLConversionService xmlConversionService =
						XMLConversionService.xmlConversionServiceFactoryForServlets(getServletContext(), xslFilesDirecoryPath, xmlCachDir, true);
					rm.setXMLConversionService(xmlConversionService);
				} catch (Throwable t) {
					prtlnErr("ERROR: Unable to initialize xmlConversionService: " + t);
				}
			}

			// Add default xmlformat schema and namespaces for OAI:
			Enumeration enumeration = getInitParameterNames();
			String param;
			while (enumeration.hasMoreElements()) {
				param = (String) enumeration.nextElement();
				if (param.toLowerCase().startsWith("xmlformatinfo")) {
					try {
						rm.setDefaultXmlFormatInfo(getInitParameter(param));
					} catch (Throwable t) {
						String initErrorMsg = "Error reading init param for xmlformatinfo: " + t;
						prtlnErr(initErrorMsg);
					}
				}
			}

			servletContext.setAttribute("enableNewSets", enableNewSets);
			servletContext.setAttribute("index", index);
			servletContext.setAttribute("queryLogFile", queryLogFile);
			servletContext.setAttribute("resourceResultLinkRedirectURL", resourceResultLinkRedirectURL);

			setCollectionsVocabDisplay(metadataVocab, rm);
			System.gc();
			System.runFinalization();

			// Set the startup date and log the time:
			servletContext.setAttribute("ddsStartUpDate", new Date());

			System.out.println("\n\n" + getDateStamp() + " DDSServlet started." + "\n\n");

			// ------ Index collecitons from external data source, if indicated -------

			if (!recordDataSource.equals("fileSystem") && !recordDataSource.equals("dcs")) {
				IndexingManager indexingManager = new IndexingManager(rm);

				servletContext.setAttribute("indexingManager", indexingManager);

				recordDataSource = recordDataSource.replaceAll("\\s+", "");
				String[] classes = recordDataSource.split(",");
				for (int i = 0; i < classes.length; i++) {
					try {
						prtln("Adding ItemIndexer '" + classes[i] + "'");
						indexingManager.addIndexingEventHandler(classes[i]);
						prtln("Done Adding ItemIndexer '" + classes[i] + "'");
					} catch (Throwable t) {
						prtlnErr("Error initializing data source Java class for IndexingManager: " + t);
					}
				}

				try {
					indexingManager.fireIndexerReadyEvent(null);
					indexingManager.fireUpdateCollectionsEvent(null);
				} catch (Throwable t) {
					prtlnErr("Error occured with an ItemIndexer: " + t);
					t.printStackTrace();
				}

				try {
					if (indexingStartTime != null)
						indexingManager.startIndexingTimer(indexingStartTime, indexingDaysOfWeek);
				} catch (Throwable t) {
					prtlnErr("Error starting the indexing timer: " + t);
				}
			}
		} catch (Throwable t) {
			prtlnErr("DDSServlet.init() error: " + t);
			t.printStackTrace();
			throw new ServletException(t);
		}
	}


	/**
	 *  Sets the fileIndexingPlugins attribute of the DDSServlet object
	 *
	 * @param  rm  The new fileIndexingPlugins value
	 */
	private void setFileIndexingPlugins(RepositoryManager rm) {
		try {
			// Add configured FileIndexingPlugins:
			Enumeration enumeration = getInitParameterNames();
			String param;
			while (enumeration.hasMoreElements()) {
				param = (String) enumeration.nextElement();

				if (param.toLowerCase().startsWith("fileindexingplugin")) {
					String paramVal = getInitParameter(param);
					String[] vals = paramVal.split("\\|");
					if (vals.length != 2 && vals.length != 1) {
						prtlnErr("Error: setFileIndexingPlugins(): could not parse parameter '" + paramVal + "'");
						continue;
					}

					try {
						Class pluginClass = Class.forName(vals[0].trim());
						FileIndexingPlugin plugin = (FileIndexingPlugin) pluginClass.newInstance();
						String format = vals.length == 2 ? vals[1].trim() : RepositoryManager.PLUGIN_ALL_FORMATS;

						// Make the ServletContext available to all ServletContextFileIndexingPlugins
						if (plugin instanceof ServletContextFileIndexingPlugin) {
							((ServletContextFileIndexingPlugin) plugin).setServletContext(getServletContext());
						}

						//System.out.println("Adding plugin: " + plugin.getClass().getName() + " for format " + format);
						rm.setFileIndexingPlugin(format, plugin);
					} catch (Throwable e) {
						prtlnErr("Error: setFileIndexingPlugins(): could not instantiate class '" + vals[0].trim() + "'. " + e);
						continue;
					}
				}
			}
		} catch (Throwable e) {
			String initErrorMsg = "Error: setFileIndexingPlugins(): " + e;
			prtlnErr(initErrorMsg);
		}

	}


	/**  Shut down sequence. */
	public void destroy() {
		IndexingManager indexingManager = (IndexingManager) getServletContext().getAttribute("indexingManager");

		try {
			if (indexingManager != null)
				indexingManager.destroy();
		} catch (Throwable t) {
			prtlnErr("Problem shutting down indexingManager: " + t);
		}

		rm.destroy();
		System.out.println("\n\n" + getDateStamp() + " DDSServlet stopped." + "\n\n");
	}


	/**
	 *  Standard doPost method forwards to doGet
	 *
	 * @param  request
	 * @param  response
	 * @exception  ServletException
	 * @exception  IOException
	 */
	public void doPost(HttpServletRequest request, HttpServletResponse response)
		 throws ServletException, IOException {
		doGet(request, response);
	}


	/**
	 *  The standard required servlet method, just parses the request header for known parameters. The <code>doPost</code>
	 *  method just calls this one. See {@link HttpServlet} for details.
	 *
	 * @param  request
	 * @param  response
	 * @exception  ServletException
	 * @exception  IOException
	 */
	public void doGet(HttpServletRequest request, HttpServletResponse response)
		 throws ServletException, IOException {
		PrintWriter out = response.getWriter();

		int result = handleRequest(request, response, out);
		switch (result) {

						case VALID_REQUEST:
							response.setContentType("text/html");
							// processed a request okay
							return;
						case UNRECOGNIZED_REQUEST:
							// no recognized parameters
							response.setContentType("text/html");
							out.println("Called with unrecognized parameter(s)...");
							return;
						case NO_REQUEST_PARAMS:
							// no paramters
							response.setContentType("text/html");
							out.println("Request did not contain a parameter...");
							return;
						case INITIALIZING:
							response.setContentType("text/html");
							out.println("System is initializing...");
							out.println(" ... initializtion may take less than a second or several minutes.");
							out.println(" ... please try request again.");
							return;
						case NOT_INITIALIZED:
							out.println("System is not initialized...");
							out.println(" ... the server may need to be restarted,");
							out.println(" ... or there is a problem with configuration.");
							out.println("");
							out.println("Please inform support@your.org.");
							out.println("");
							out.println("Thank You");
							return;
						case INVALID_CONTEXT:
							response.setContentType("text/html");
							out.println("A request was recieved, but the context can not be identified...");
							out.println(" ... either  unable to initialize the catalog context," +
								" or the servlet container is in an invalid state.");
							return;
						default:
							// an exception occurred
							response.setContentType("text/html");
							out.println("An unexpected exception occurred processing request...");
							return;
		}
	}


	/**
	 *  Used to provide explicit command parameter processing.
	 *
	 * @param  request
	 * @param  response
	 * @param  out       DESCRIPTION
	 * @return
	 */
	private int handleRequest(HttpServletRequest request, HttpServletResponse response, PrintWriter out) {

		try {
			//if ( catalog != null && monitor != null ) {
			//if (catalog.ready()) {
			Enumeration paramNames = request.getParameterNames();
			if (paramNames.hasMoreElements()) {
				while (paramNames.hasMoreElements()) {
					String paramName = (String) paramNames.nextElement();
					String[] paramValues = request.getParameterValues(paramName);
					// this next section can use an interface and hashmap -
					// see pg 228 of JavaServerPages
					if (paramValues.length == 1) {
						if (paramName.equals("command")) {
							if (paramValues[0].equals("stop")) {
								//fileIndexingService.stopTester();
							}
							return VALID_REQUEST;
							//if(paramValues[0].equals("start"))
							//	fileIndexingService.startTester();

						}
//  								if (paramName.equals("query")) {
//  									//catalog.reinit();
//  									//response.setContentType("text/html");
//  									//PrintWriter out = response.getWriter();
//  									//out.println("CatalogAdmin called Catalog.reinit() ");
//  									//out.println("See Catalog Activity Log for messages.");
//  									return VALID_REQUEST;
//  								}
//  								if (paramName.equals("unlock")) {
//  									//releaseLock(paramValues[0], request, response);
//  									return VALID_REQUEST;
//  								}
					}
				}
				return UNRECOGNIZED_REQUEST;
			}
			return NO_REQUEST_PARAMS;
			//}
			//else if (catalog.initializing())
			//	return CATALOG_INITIALIZING;
			//else
			//	return CATALOG_NOT_INITIALIZED;
			//}
			//return INVALID_CONTEXT;
		} catch (Throwable t) {
			return REQUEST_EXCEPTION;
		}
	}


	/**
	 *  Sets the "noDisplay" property of collection vocab nodes according the results of the repository manager's
	 *  getEnabledSetsHashMap()
	 *
	 * @param  vocab  The new collectionsVocabDisplay value
	 * @param  rm     The new collectionsVocabDisplay value
	 */
	public static void setCollectionsVocabDisplay(MetadataVocab vocab, RepositoryManager rm) {
		Set vi = vocab.getVocabSystemInterfaces();
		Iterator i = vi.iterator();
		while (i.hasNext()) {
			doSetCollectionsVocabDisplay(vocab, rm, (String) i.next());
		}
	}


	/**
	 *  Description of the Method
	 *
	 * @param  vocab            Description of the Parameter
	 * @param  rm               Description of the Parameter
	 * @param  systemInterface  Description of the Parameter
	 */
	private static void doSetCollectionsVocabDisplay(MetadataVocab vocab,
	                                                 RepositoryManager rm,
	                                                 String systemInterface) {
		HashMap sets = rm.getEnabledSetsHashMap();
		ArrayList nodes = vocab.getVocabNodes(systemInterface + "/key", "");
		for (int i = 0; i < nodes.size(); i++) {
			if (sets.get(((VocabNode) nodes.get(i)).getId()) == null) {
				((VocabNode) nodes.get(i)).setNoDisplay(true);
			}
			// If the UI groups file said not to display (dwelanno for example), then don't override that:
			else if (!((VocabNode) nodes.get(i)).getNoDisplayOriginal()) {
				((VocabNode) nodes.get(i)).setNoDisplay(false);
			}
		}
	}


	/**
	 *  Gets the absolute path to a given file or directory. Assumes the path passed in is eithr already absolute
	 *  (has leading slash) or is relative to the context root (no leading slash). If the string passed in does
	 *  not begin with a slash ("/"), then the string is converted. For example, an init parameter to a config
	 *  file might be passed in as "WEB-INF/conf/serverParms.conf" and this method will return the corresponding
	 *  absolute path "/export/devel/tomcat/webapps/myApp/WEB-INF/conf/serverParms.conf." <p>
	 *
	 *  If the string that is passed in already begings with "/", nothing is done. <p>
	 *
	 *  Note: the super.init() method must be called prior to using this method, else a ServletException is
	 *  thrown.
	 *
	 * @param  fname                 An absolute or relative file name or path (relative the the context root).
	 * @return                       The absolute path to the given file or path.
	 * @exception  ServletException  An exception related to this servlet
	 */
	private String getAbsolutePath(String fname)
		 throws ServletException {
		return GeneralServletTools.getAbsolutePath(fname, getServletContext());
	}


	/**
	 *  Gets the absolute path to a given file or directory. Assumes the path passed in is eithr already absolute
	 *  (has leading slash) or is relative to the context root (no leading slash). If the string passed in does
	 *  not begin with a slash ("/"), then the string is converted. For example, an init parameter to a config
	 *  file might be passed in as "WEB-INF/conf/serverParms.conf" and this method will return the corresponding
	 *  absolute path "/export/devel/tomcat/webapps/myApp/WEB-INF/conf/serverParms.conf." <p>
	 *
	 *  If the string that is passed in already begings with "/", nothing is done. <p>
	 *
	 *  Note: the super.init() method must be called prior to using this method, else a ServletException is
	 *  thrown.
	 *
	 * @param  fname    An absolute or relative file name or path (relative the the context root).
	 * @param  docRoot  The context document root as obtained by calling getServletContext().getRealPath("/");
	 * @return          The absolute path to the given file or path.
	 */
	private String getAbsolutePath(String fname, String docRoot) {
		return GeneralServletTools.getAbsolutePath(fname, docRoot);
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
		System.err.println(getDateStamp() + " " + s);
	}


	/**
	 *  Output a line of text to standard out, with datestamp, if debug is set to true.
	 *
	 * @param  s  The String that will be output.
	 */
	private final void prtln(String s) {
		if (debug) {
			System.out.println(getDateStamp() + " " + s);
		}
	}


	/**
	 *  Sets the debug attribute of the DDSServlet object
	 *
	 * @param  db  The new debug value
	 */
	public final void setDebug(boolean db) {
		debug = db;
	}
}

