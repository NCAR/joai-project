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
package org.dlese.dpc.schemedit.ndr;

import org.dlese.dpc.ndr.reader.*;
import org.dlese.dpc.schemedit.ndr.util.*;

import org.dlese.dpc.util.Files;
import org.dlese.dpc.util.EnvReader;
import org.dom4j.*;
import org.dom4j.tree.*;
import org.dom4j.io.*;

import java.io.File;
import java.util.*;

import javax.servlet.ServletContext;

import org.dlese.dpc.schemedit.Constants;
import org.dlese.dpc.schemedit.SchemEditUtils;
import org.dlese.dpc.schemedit.MetaDataFramework;
import org.dlese.dpc.schemedit.MetaDataHelper;
import org.dlese.dpc.schemedit.SessionRegistry;
import org.dlese.dpc.schemedit.SessionBean;
import org.dlese.dpc.schemedit.FrameworkRegistry;
import org.dlese.dpc.schemedit.config.CollectionRegistry;
import org.dlese.dpc.schemedit.config.CollectionConfig;
import org.dlese.dpc.schemedit.config.StatusFlags;
import org.dlese.dpc.schemedit.dcs.DcsDataManager;
import org.dlese.dpc.schemedit.dcs.DcsDataRecord;
import org.dlese.dpc.schemedit.dcs.StatusEntry;
import org.dlese.dpc.schemedit.test.TesterUtils;
import org.dlese.dpc.schemedit.security.user.User;
import org.dlese.dpc.repository.RepositoryManager;

import org.dlese.dpc.xml.schema.*;
import org.dlese.dpc.xml.Dom4jUtils;
import org.dlese.dpc.xml.XMLConversionService;

/**
 *  Extracts data from a Collection Metadata object and loads it into the NCS
 *  data structures to create a collection, and then creates item-level metadata
 *  from MetadataObjects in the NDR (transforming to native format if possible).
 *  <p>
 *
 *  ISSUE: can we import arbitrary collections, or is it REQUIRED that NCS-level
 *  information (e.g., format) is present.
 *
 * @author    Jonathan Ostwald
 */
public class CollectionImporter {

	private static boolean debug = true;
	private static boolean writeToDisk = true;

	static boolean importOnlyAuthorizedCollections = true;

	ServletContext servletContext = null;
	FrameworkRegistry frameworkRegistry = null;
	CollectionRegistry collectionRegistry = null;
	DcsDataManager dcsDataManager = null;
	RepositoryManager repositoryManager = null;
	XMLConversionService xmlConversionService;

	File repositoryDir = null;
	File collectionConfigDir = null;

	MetadataProviderReader mdp = null;

	File itemRecordDir = null;
	File dcsRecordDir = null;
	File collectionRecordDir = null;
	Document collectionDoc = null;

	Element collectionHistory = null;

	String collectionId = null;
	String collectionRecordID = null;
	String collectionName = null;
	String nativeFormat = null;
	String mdpHandle = null;


	/**
	 *  Constructor for the CollectionImporter object (with access to
	 *  ServletContext)
	 *
	 * @param  servletContext  NOT YET DOCUMENTED
	 * @param  mdpHandle       NOT YET DOCUMENTED
	 * @exception  Exception   NOT YET DOCUMENTED
	 */
	public CollectionImporter(String mdpHandle,
	                          ServletContext servletContext) throws Exception {
		prtln("\n=========================================================");
		prtln("CollectionImporter()  mdpHandle: " + mdpHandle);
		/*
			make sure all of the required services are available in the ServletContest
			before preceeding ...
		*/
		try {
			this.servletContext = servletContext;

			collectionConfigDir = (File) getRequiredContextAttribute("collectionConfigDir");

			frameworkRegistry =
				(FrameworkRegistry) getRequiredContextAttribute("frameworkRegistry");

			collectionRegistry =
				(CollectionRegistry) getRequiredContextAttribute("collectionRegistry");

			dcsDataManager =
				(DcsDataManager) getRequiredContextAttribute("dcsDataManager");

			repositoryManager =
				(RepositoryManager) getRequiredContextAttribute("repositoryManager");

			xmlConversionService = null;
			try {
				xmlConversionService =
					(XMLConversionService) repositoryManager.getIndex().getAttribute("xmlConversionService");
			} catch (Throwable t) {}
			if (xmlConversionService == null)
				throw new Exception("unable to obtain xmlConversionService");

			repositoryDir = new File(repositoryManager.getMetadataRecordsLocation());

		} catch (Exception e) {
			throw new Exception("CollectionImporter could not be constructed: " + e.getMessage());
		}

		try {
			init(mdpHandle);
		} catch (Exception e) {
			e.printStackTrace();
			throw new Exception("CollectionImporter could not be initialized: " + e.getMessage());
		}
		prtln(".. initialized");
	}


	/**
	 *  Constructor for development and debugging purposes (servletContext is not
	 *  available).
	 *
	 * @param  recordsPath    NOT YET DOCUMENTED
	 * @param  configPath     NOT YET DOCUMENTED
	 * @param  mdpHandle      NOT YET DOCUMENTED
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public CollectionImporter(String mdpHandle,
	                          String recordsPath,
	                          String configPath) throws Exception {

		repositoryDir = new File(recordsPath);
		collectionConfigDir = new File(configPath);

		try {
			init(mdpHandle);
		} catch (Exception e) {
			e.printStackTrace();
			throw new Exception("init error: " + e.getMessage());
		}
	}



	/**
	 *  Gets the requiredServletContextAttribute attribute of the CollectionImporter
	 *  object
	 *
	 * @param  attributeName  NOT YET DOCUMENTED
	 * @return                The requiredServletContextAttribute value
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	private Object getRequiredContextAttribute(String attributeName) throws Exception {
		Object object = servletContext.getAttribute(attributeName);
		if (object == null) {
			throw new Exception("Required servletContext attribute (" + attributeName + ") not found");
		}
		return object;
	}


	/**
	 *  Initialize the import by extracting collection information from the
	 *  metadateProvider, such as collectionId, format, etc.
	 *
	 * @param  mdpHandle      collection metadataProvider
	 * @exception  Exception  if mdpHandle does not yield a metadataProvider
	 *      object, if the nativeFormat is unknown, or if the required file
	 *      structures cannot be created.
	 */
	private void init(String mdpHandle) throws Exception {
		prtln("\n- init()");
		this.collectionRecordID = collectionRegistry.getMasterCollectionConfig().nextID();

		this.mdp = new MetadataProviderReader(mdpHandle);
		if (mdp == null) {
			throw new Exception("MetadataProvider not found for " + mdpHandle);
		}

		if (importOnlyAuthorizedCollections && !this.mdp.isAuthorizedToChange())
			throw new Exception("This Application is not authorized to manage requested NDR collection");

		this.collectionId = getCollectionId();
		this.collectionName = getCollectionName();
		this.nativeFormat = getNativeFormat();

		prtln("\t collectionId: " + collectionId);
		prtln("\t collectionName: " + collectionName);
		prtln("\t nativeFormat: " + nativeFormat);

		// ensure metadata Framework exists for native framework
		this.getMetaDataFramework(nativeFormat);

		// Establish the directories for the files we will write
		String sep = Files.getFileSeparatorStr();
		if (!getRecordsDir().exists())
			throw new Exception("Records directory does not exist at " + getRecordsDir().toString());

		this.itemRecordDir = new File(repositoryDir, nativeFormat + sep + collectionId);
		if (!this.itemRecordDir.exists())
			this.itemRecordDir.mkdirs();

		this.dcsRecordDir = new File(repositoryDir, "dcs_data" + sep + nativeFormat + sep + collectionId);
		if (!this.dcsRecordDir.exists())
			this.dcsRecordDir.mkdirs();

		this.collectionRecordDir = new File(repositoryDir, "dlese_collect/collect");
		if (!this.collectionRecordDir.exists())
			this.collectionRecordDir.mkdirs();
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @return                NOT YET DOCUMENTED
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public Map doImport() throws Exception {
		prtln("\n doImport()");
		if (this.servletContext == null)
			throw new Exception("doImport called without a servletContext!!");

		try {
			loadCollectionLevelMetadata();

			// Register Collection with CollectionRegistry
			prtln("\t about to call loadCollectionConfig()");
			CollectionConfig collectionConfig = loadCollectionConfig();

			prtln("\t about to call loadCollectionDcsData()");
			loadCollectionDcsData();

			// Register Collection with repositoryManager
			repositoryManager.loadCollectionRecords(true);

			// write item-level metadata and dcs_data to disk
			prtln("\t about to call loadItemLevelMetadata()");
			loadItemLevelMetadata(collectionConfig);

			Map importReport = new HashMap();
			importReport.put("collectionId", collectionId);
			importReport.put("collectionName", collectionName);

			return importReport;
		} catch (Throwable t) {
			String errMsg = "doImport ERROR: " + t.getMessage();
			t.printStackTrace();
			throw new Exception(errMsg);
		}
	}


	/**
	 *  Gets the metaDataFramework attribute of the AbstractSchemEditAction object
	 *
	 * @return                The metaDataFramework value
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	protected MetaDataFramework getCollectionFramework() throws Exception {
		return getMetaDataFramework("dlese_collect");
	}


	/**
	 *  Gets the collectionConfigFramework attribute of the CollectionImporter
	 *  object
	 *
	 * @return                The collectionConfigFramework value
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	protected MetaDataFramework getCollectionConfigFramework() throws Exception {
		return getMetaDataFramework("collection_config");
	}


	/**
	 *  Returns MetaDataFramework for specified format.<p>
	 *
	 *  Uses TesterUtils to obtain framework in command line context, when
	 *  frameworkRegistry is not available.
	 *
	 * @param  xmlFormat      NOT YET DOCUMENTED
	 * @return                The metaDataFramework value
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	MetaDataFramework getMetaDataFramework(String xmlFormat) throws Exception {
		MetaDataFramework framework = null;
		try {
			return frameworkRegistry.getFramework(xmlFormat);
		} catch (NullPointerException e) {}
		if (framework == null)
			throw new Exception("Metadata Framework not found for format: \"" + xmlFormat + "\"");
		return framework;
	}


	/**
	 *  Gets the statusEntries element from the metadataProvider's dsc_data
	 *  datastream, which reflect the collection's history, including status
	 *  changes, imports and exports.
	 *
	 * @return    The statusEntries element if one exists, or an "empty_history"
	 *      element otherwise
	 */
	public Element getCollectionHistory() {

		Element collectionHistory = null;
		Element dcs_data = mdp.getDataStream("dcs_data");
		if (dcs_data != null)
			collectionHistory = (Element) dcs_data.selectSingleNode("dcsDataRecord/statusEntries");
		return (collectionHistory != null) ? collectionHistory : null;
	}


	/**
	 *  Gets the collectionDoc attribute of the CollectionImporter object
	 *
	 * @return                The collectionDoc value
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	Document getCollectionDoc() throws Exception {
		if (collectionDoc == null) {
			Map pvm = new HashMap();
			pvm.put("/collectionRecord/access/key/@libraryFormat", nativeFormat);
			pvm.put("/collectionRecord/general/shortTitle", collectionName);
			pvm.put("/collectionRecord/metaMetadata/catalogEntries/catalog/@entry", collectionRecordID);
			pvm.put("/collectionRecord/access/key", collectionId);

			collectionDoc = MetaDataHelper.makeCollectionDoc(collectionRecordID, pvm, getCollectionFramework());
			collectionDoc = getCollectionFramework().getWritableRecord(collectionDoc);
		}
		return collectionDoc;
	}


	/**
	 *  Write Collection-level metadata record to disk (NOT currently used).
	 *
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public void loadCollectionLevelMetadata() throws Exception {
		Document doc = getCollectionDoc();
		File dest = new File(collectionRecordDir, collectionRecordID + ".xml");
		writeRecord(dest, doc.getRootElement());
	}


	/**
	 *  Gets the collectionMetadata attribute of the CollectionImporter object
	 *
	 * @return    The collectionMetadata value
	 */
	/* 	private CollectionMetadata getCollectionMetadata(String mdpHandle) throws Exception {
		cm = new CollectionMetadata();
		MetadataProviderReader mdp = new MetadataProviderReader(mdpHandle);
		// populate HEADER
		CollectionMetadata.Header header = cm.getHeader();
		header.setCollectionId(mdp.collectionId);
		header.setCollectionName(mdp.getCollectionName());
		header.setNativeFormat(mdp.getNativeFormat());
		header.setDataStream("dcs_data", mdp.getDataStream("dcs_data"));
		header.setDataStream("collection_config", mdp.getDataStream("collection_config"));
		header.setMetadataProviderDescription(mdp.getServiceDescription());
		header.setMetadataProviderHandle(mdp.getHandle());
		// get METADATA
		List mdHandles = mdp.getItemHandles();
		String nativeFormat = header.getNativeFormat();
		prtln("about to fetch " + mdHandles.size() + " metadata items");
		for (Iterator i = mdHandles.iterator(); i.hasNext(); ) {
			String mdHandle = (String) i.next();
			MetadataReader reader = new MetadataReader(mdHandle, nativeFormat);
			MetaDataWrapper wrapper = new MetaDataWrapper();
			wrapper.setHandle(reader.getHandle());
			wrapper.setPid(reader.getPid());
			try {
				wrapper.setDataStream(nativeFormat, reader.getDataStream(nativeFormat));
			} catch (Exception e) {
				prtln("failed to set \"" + nativeFormat + "\" datastream for " + mdHandle + " : " + e.getMessage());
			}
			try {
				wrapper.setDataStream("dcs_data", reader.getDataStream("dcs_data"));
			} catch (Exception e) {
				prtln("failed to set dcs_data datastream for " + mdHandle + " : " + e.getMessage());
			}
			cm.addMetadataWrapper(wrapper);
		}
		return cm;
	} */
	/**
	 *  Determines a collectionId for the collection to be imported.<P>
	 *
	 *  Tries the following until a value is found:
	 *  <ul>
	 *    <li> collectionId property of mdpReader
	 *    <li> setSpec property of mdpReader
	 *    <li> creates a uniqueID number
	 *  </ul>
	 *
	 *
	 * @return    The collectionId value
	 */
	public String getCollectionId() {
		if (collectionId == null) {
			collectionId = mdp.getCollectionId();
			if (!hasValue(collectionId)) {
				collectionId = mdp.getProperty("setSpec");
			}
			if (!hasValue(collectionId)) {
				collectionId = SchemEditUtils.getUniqueId();
			}
		}
		return collectionId;
	}


	/**
	 *  Gets the sessionUserName attribute of the CollectionImporter object
	 *
	 * @return    The sessionUserName value
	 */
	private String getSessionUserName() {
		User sessionUser = null;
		/* 		try {
			SessionRegistry sessionRegistry =
				(SessionRegistry) getRequiredContextAttribute("sessionRegistry");
			SessionBean sessionBean = sessionRegistry.getSessionBean(request);
			if (sessionBean == null) {
				throw new Exception("SessionBean not found in SessionRegistry");
			}
			else {
				sessionUser = sessionBean.getUser();
			}
		} catch (Throwable t) {}
		return (sessionUser == null ? Constants.UNKNOWN_USER : sessionUser.getUsername()); */
		return Constants.UNKNOWN_USER;
	}


	/**
	 *  Determines a collectionName for the collection to be imported.<P>
	 *
	 *  Tries the following until a value is found:
	 *  <ul>
	 *    <li> collectionName property of mdpReader
	 *    <li> setName property of mdpReader
	 *    <li> title field of the serviceDescription
	 *    <li> the string "Unnamed Collection"
	 *  </ul>
	 *
	 *
	 * @return    NOT YET DOCUMENTED
	 */
	private String getCollectionName() {
		if (collectionName == null) {
			collectionName = mdp.getCollectionName();

			if (!hasValue(collectionName)) {
				collectionName = mdp.getProperty("setName");
			}

			if (!hasValue(collectionName)) {
				ServiceDescriptionReader sd = mdp.getServiceDescription();
				if (sd != null) {
					try {
						collectionName = sd.getTitle();
					} catch (Throwable t) {}
				}
			}

			if (!hasValue(collectionName)) {
				collectionName = "Unnamed Collection";
			}
		}
		return collectionName;
	}


	/**
	 *  Gets the nativeFormat for the collection to be imported.<p>
	 *
	 *  If the MetadataProvider object does not specify a nativeFormat, we use
	 *  "ncs_item", so that the item records will be editable in the DCS.
	 *
	 * @return    The nativeFormat value
	 */
	private String getNativeFormat() {
		if (nativeFormat == null) {
			nativeFormat = mdp.getNativeFormat();

			if (!hasValue(nativeFormat) || nativeFormat.equals("nsdl_dc"))
				nativeFormat = "ncs_item";
		}
		return nativeFormat;
	}


	/**
	 *  The location of the metadata records.<p>
	 *
	 *  NOTE: The recordsDir is obtained va RepositoryManager.getMetadataRecordsLocation()
	 *  when we are running as a webapp, and is provided explicitly when running
	 *  from command line.<p>
	 *
	 *
	 *
	 * @return    The recordsDir value
	 */
	File getRecordsDir() {
		return repositoryDir;
	}


	/**
	 *  Gets the collectionConfigFile attribute of the CollectionImporter object
	 *
	 * @return    The collectionConfigFile value
	 */
	public File getCollectionConfigFile() {
		String fileName = collectionId + ".xml";
		return new File(collectionConfigDir, fileName);
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	private void loadCollectionDcsData() throws Exception {

		DcsDataRecord dcsDataRecord =
			dcsDataManager.getDcsDataRecord("collect", "dlese_collect", collectionRecordID + ".xml", collectionRecordID);

		Element collectionHistory = getCollectionHistory();
		if (collectionHistory != null) {
			for (Iterator i = collectionHistory.elementIterator("statusEntry"); i.hasNext(); ) {
				Element e = (Element) i.next();
				dcsDataRecord.updateStatus(new StatusEntry(e), true);
			}
		}

		String username = getSessionUserName();
		dcsDataRecord.updateStatus(StatusFlags.NDR_IMPORTED_STATUS, "", username);
		dcsDataRecord.flushToDisk();
	}


	/**
	 *  Loads Extracts a collection_config stream from the metadataProvider object
	 *  in the NDR and loads it into the NCS
	 *
	 * @return                NOT YET DOCUMENTED
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	private CollectionConfig loadCollectionConfig() throws Exception {
		prtln("\n- loadCollectionConfig()");
		boolean configProvided = false;
		Element collection_configDS = this.mdp.getDataStream("collection_config");
		// String idPrefix =
		if (collection_configDS != null) {
			writeCollectionConfigStream(collection_configDS.asXML());
			configProvided = true;
		}
		else {
			prtln("\t collection_config stream not found in mdp");
		}

		// update collection config to reflect import
		/*
			NOTE: if we are importing a collection from NDR for the first time, the
			collection_config will NOT contain the following (which should be inserted
			at this time:
			- metadataProviderHandle, aggregatorHandle, agentHandle
			- collectionPrefix
			...
		*/
		// Register Collection - this will create and register config if it doesn't already exist
		CollectionConfig config = collectionRegistry.getCollectionConfig(collectionId, true);
		if (!configProvided) {
			config.setIdPrefix(collectionId);
		}
		config.setAuthority("ndr");
		String mdpHandle = mdp.getHandle();
		if (hasValue(mdpHandle))
			config.setMetadataProviderHandle(mdpHandle);

		String agentHandle = mdp.getMetadataProviderFor();
		if (hasValue(agentHandle)) {
			prtln("\t agentHandle: " + agentHandle);
			config.setAgentHandle(agentHandle);
			prtln("\t ... config.getAgentHandle(): " + config.getAgentHandle());
		}

		String aggregatorHandle = mdp.getAggregatedBy();
		if (hasValue(aggregatorHandle))
			config.setAggregatorHandle(aggregatorHandle);

		config.flush();

		prtln("\t After loading collection Configuration");
		prtln("\t ... id: " + config.getId());
		prtln("\t ... xmlFormat: " + config.getXmlFormat());
		prtln("\t ... authority is: " + config.getAuthority());
		prtln("\t ... metadataProviderHandle: " + config.getMetadataProviderHandle());
		prtln("\t ... agentHandle: " + config.getAgentHandle());
		prtln("\t ... aggregatorHandle: " + config.getAggregatorHandle());
		prtln("\t ... prefix: " + config.getIdPrefix());

		return config;
	}


	/**
	 *  Write the CollectionConfig datastream to disk as a collection_config file.
	 *
	 * @param  configStream   NOT YET DOCUMENTED
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	private void writeCollectionConfigStream(String configStream) throws Exception {
		prtln("writeCollectionConfigStream()");

		Document localizedDoc = Dom4jUtils.getXmlDocument(configStream);
		prtln("\nlocalized version of collection_config stream");
		pp(localizedDoc);
		prtln("--------------------");
		String xml = getCollectionConfigFramework().getWritableRecordXml(localizedDoc);
		File dest = getCollectionConfigFile();
		if (this.writeToDisk) {
			Files.writeFile(xml, dest);
			prtln("collection Config record written to " + dest.getAbsolutePath());
		}
		else {
			prtln("\ncollection  Config record NOT written to disk");
			prtln(xml);
		}
	}


	/**
	 *  Extract the ItemRecord from provided meta Element and write to disk at
	 *  location determined by provided pid.
	 *
	 * @param  pid            itemRecord id
	 * @param  meta           Element containing itemRecor record as sole child
	 *      element
	 * @exception  Exception  if meta contains other than 1 child, or if there is
	 *      trouble writing to disk
	 */
	void writeItemRecord(String pid, Element meta) throws Exception {
		File dest = new File(itemRecordDir, pid + ".xml");
		writeRecord(dest, meta);
	}


	/**
	 *  Extract DcsDataRecord from provided dcsData Element and write to disk # at
	 *  location determined by provided id.
	 *
	 * @param  id                DcsDataRecord id
	 * @param  md                NOT YET DOCUMENTED
	 * @param  collectionConfig  NOT YET DOCUMENTED
	 * @exception  Exception     if dcsData contains other than 1 child, or if
	 *      there is trouble writing to disk
	 */
	void writeDcsDataRecord(String id, MetadataReader md, CollectionConfig collectionConfig) throws Exception {
		// set mdHandle in dcs_data record.
		Element dcsData = md.getDataStream("dcs_data");
		String dcsDataRecordFileName = id + ".xml";
		if (dcsData != null) {
			Element handleElement = DocumentHelper.makeElement(dcsData, "ndrInfo/ndrHandle");
			handleElement.setText(md.getHandle());
			File dest = new File(dcsRecordDir, dcsDataRecordFileName);
			writeRecord(dest, dcsData);
		}
		else {
			DcsDataRecord dcsDataRecord = this.dcsDataManager.getDcsDataRecord(
				collectionConfig.getId(),
				nativeFormat,
				dcsDataRecordFileName,
				id);
			dcsDataRecord.setNdrHandle(md.getHandle());
			dcsDataRecord.flushToDisk();
		}
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  rootElement    NOT YET DOCUMENTED
	 * @return                NOT YET DOCUMENTED
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	String makeDocXml(Element rootElement) throws Exception {
		if (rootElement == null)
			throw new Exception("makeDocXml received null rootElement");
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
		// xml += rootElement.asXML();
		xml += Dom4jUtils.prettyPrint(rootElement);
		return xml;
	}


	/**
	 *  Write an XML Document (having provided Element as root) to the specified
	 *  File.
	 *
	 * @param  dest           NOT YET DOCUMENTED
	 * @param  record         NOT YET DOCUMENTED
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	void writeRecord(File dest, Element record) throws Exception {
		String xml = makeDocXml(record);
		if (this.writeToDisk) {
			Files.writeFile(xml, dest);
			prtln(record.getQualifiedName() + " written to " + dest.getAbsolutePath());
		}
		else {
			prtln("\n" + record.getQualifiedName() + " NOT written to disk");
			prtln(xml);
		}
	}


	/**
	 *  Process MetadataWrappers in NDR response.<p>
	 *
	 *  Extract the following from each MetaDataWrapper and write to file system:
	 *
	 *  <ul>
	 *    <li> pid
	 *    <li> itemRecord
	 *    <li> dcsDataRecord
	 *  </ul>
	 *
	 *
	 * @param  collectionConfig  NOT YET DOCUMENTED
	 * @exception  Exception     NOT YET DOCUMENTED
	 */
	public void loadItemLevelMetadata(CollectionConfig collectionConfig) throws Exception {
		prtln("\nImporting Item Level Metadata ... (" + this.nativeFormat + ")");

		List mdHandles = mdp.getItemHandles();
		if (mdHandles == null)
			return;

		for (Iterator i = mdHandles.iterator(); i.hasNext(); ) {
			String mdHandle = (String) i.next();
			MetadataReader mdReader = null;
			try {
				mdReader = new MetadataReader(mdHandle, this.getNativeFormat());
			} catch (Exception e) {
				prtln("unable to get MetadataObject at " + mdHandle + ": " + e.getMessage());
				continue;
			}
			String id = mdReader.getUniqueID();
			if (!hasValue(id)) {
				try {
					id = collectionConfig.nextID();
				} catch (Throwable t) {
					throw new Exception("unable to generate ID for metadata record");
				}
			}
			// write to repository
			// NOTE!!!  TRANSFORM nsdl_dc to ncs_item!!

			prtln("\n processing " + id);
			try {
				Element metadata = getMetadataRecord(mdReader, collectionConfig);
				if (metadata == null) {
					throw new Exception("metadata record was not obtained from " + mdReader.getHandle());
				}
				writeItemRecord(id, metadata);
			} catch (Exception e) {
				prtln("unable to write item metadata (" + id + "): " + e.getMessage());
				continue;
			}
			try {
				writeDcsDataRecord(id, mdReader, collectionConfig);
			} catch (Exception e) {
				prtln("unable to write item dcs_data (" + id + "): " + e.getMessage());
			}
		}
	}


	/**
	 *  Get the primary datastream as a dom4j.Element.<p>
	 *
	 *  If a nativeFormat is unknown assume there is a "nsdl_dc" and transform it
	 *  to "ncs_item".
	 *
	 * @param  md      NOT YET DOCUMENTED
	 * @param  config  NOT YET DOCUMENTED
	 * @return         The metadataRecord value
	 */
	private Element getMetadataRecord(MetadataReader md, CollectionConfig config) {
		prtln("\ngetMetadataRecord() - nativeFormat: " + nativeFormat);
		Element metadata = md.getDataStream(nativeFormat);
		if (metadata == null)
			prtln("\t metdata is NULL");

		if (this.nativeFormat.equals("ncs_item") && metadata == null) {
			prtln("entering transform track");

			Element nsdl_dc_stream = md.getDataStream("nsdl_dc");
			if (nsdl_dc_stream == null) {
				prtln("nsdl_dc stream not found in metadata object at " + md.getHandle());
				return null;
			}
			prtln("nsdl_dc_stream: " + nsdl_dc_stream.asXML());
			String nsdl_dc = nsdl_dc_stream.asXML();
			String fromFormat = "nsdl_dc";
			String toFormat = "ncs_item";
			if (xmlConversionService == null || !xmlConversionService.canConvert(fromFormat, toFormat)) {
				prtln("conversionService could not convert from \"" + fromFormat +
					"\" to \"" + toFormat + "\"");
				return null;
			}
			else {
				try {
					prtln("about to transform");

					org.dlese.dpc.xml.XSLTransformer.setDebug(true);
					String transformedXml = xmlConversionService.convertXml(fromFormat, toFormat, nsdl_dc);

					if (transformedXml == null || transformedXml.trim().length() == 0) {
						prtln("apparently unable to transform ...");
						metadata = null;
					}
					else {
						metadata = DocumentHelper.parseText(transformedXml).getRootElement();
					}
				} catch (Exception e) {
					prtlnErr("error transforming to nsdl_dc" + e.getMessage());
				}
			}
		}

		else {
			if (this.nativeFormat.equals("oai_dc")) {
				metadata = normalizeOaiDcRecord(metadata);
			}
		}

		return metadata;
	}


	/**
	 *  Convert NSDL_OAI record to the form used internally by DCS by changing the
	 *  "schemaLocation" from
	 *
	 * @param  oai_dc  NOT YET DOCUMENTED
	 * @return         NOT YET DOCUMENTED
	 */
	private Element normalizeOaiDcRecord(Element oai_dc) {
		Namespace xsi = DocumentHelper.createNamespace("xsi", "http://www.w3.org/2001/XMLSchema-instance");
		QName schemaLocation = DocumentHelper.createQName("schemaLocation", xsi);
		oai_dc.addAttribute(schemaLocation,
			"http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.dlese.org/Metadata/oai_dc/2.0/oai_dc-DCvocab-nolang.xsd");
		return oai_dc;
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  args           NOT YET DOCUMENTED
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public static void main(String args[]) throws Exception {
		prtln("\n+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
		prtln("CollectionImporter");
		String mdpHandle = "2200/test.20070403144245218T";

		String recordsPath = TesterUtils.getRecordsPath();
		String configPath = TesterUtils.getCollectionConfigDir();

		CollectionImporter importer = null;
		try {
			importer = new CollectionImporter(mdpHandle, recordsPath, configPath);
		} catch (Exception e) {
			prtln("Loader not initialized due to FATAL ERROR: " + e.getMessage());
			return;
		}

		try {
			importer.loadCollectionLevelMetadata();

			importer.loadItemLevelMetadata(null);
			prtln("Collection Imported!! To Check:");
			prtln("\t collection_config document in " + importer.getCollectionConfigFile());
			// pp (collection_doc);

		} catch (Exception e) {
			prtln("Import error: " + e.getMessage());
		}
	}


	/**  NOT YET DOCUMENTED */
	void report() {
		String NL = "\n\t";
		String s = "CollectionImporter report";
		prtln(s);
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  s  NOT YET DOCUMENTED
	 * @return    NOT YET DOCUMENTED
	 */
	private boolean hasValue(String s) {
		return (s != null && s.trim().length() > 0);
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  node  NOT YET DOCUMENTED
	 */
	private static void pp(Node node) {
		System.out.println(Dom4jUtils.prettyPrint(node));
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  s  NOT YET DOCUMENTED
	 */
	private static void prtln(String s) {
		if (debug)
			SchemEditUtils.prtln(s, "CollectionImporter");
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  s  NOT YET DOCUMENTED
	 */
	private static void prtlnErr(String s) {
		SchemEditUtils.prtln(s, "CollectionImporter Error: ");
	}
}

