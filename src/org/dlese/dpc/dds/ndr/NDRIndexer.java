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
package org.dlese.dpc.dds.ndr;

import org.dlese.dpc.repository.indexing.*;

import java.io.*;
import java.util.*;
import java.net.URL;

import org.dlese.dpc.index.*;
import org.dlese.dpc.index.writer.*;
import java.text.*;

import org.dlese.dpc.ndr.*;
import org.dlese.dpc.ndr.reader.*;
import org.dlese.dpc.ndr.request.*;
import org.dlese.dpc.ndr.apiproxy.*;
import org.dlese.dpc.ndr.apiproxy.InfoXML;
import org.dlese.dpc.propertiesmgr.PropertiesManager;
import org.dlese.dpc.util.TimedURLConnection;
import org.dlese.dpc.repository.PutCollectionException;

import org.dom4j.*;
import org.dlese.dpc.xml.Dom4jUtils;
import org.apache.lucene.document.Field;
import java.net.URLEncoder;

/**
 *  Indexes and configures collections from the NSDL Data Repository (NDR).
 *
 * @author    John Weatherley
 */
public class NDRIndexer implements ItemIndexer {
	private static boolean debug = false;

	int MAX_NUM_COLLECTIONS = 1000; // The max number of collections DDS will suck in from NDR...

	private CollectionIndexer collectionIndexer = null;
	private boolean abortIndexing = false;
	private boolean indexingIsActive = false;
	private File configFile = null;
	private Document configXmlDoc = null;


	/**  Constructor for the NDRIndexer object */
	public NDRIndexer() { }


	/**
	 *  Sets the configDirectory attribute of the NDRIndexer object
	 *
	 * @param  configDir      The new configDirectory value
	 * @exception  Exception  If error
	 */
	public void setConfigDirectory(File configDir) throws Exception {
		if (configDir == null)
			throw new Exception("The configDir is null");
		this.configFile = new File(configDir, "org.dlese.dpc.dds.ndr.NDRIndexer.NDR_indexer_collections.xml");
	}


	/**
	 *  Initialize and configure. This gets called once when the class is first created and is called by
	 *  updateCollections() when re-loading the list of collections.
	 *
	 * @exception  Exception  If error
	 */
	private void configureAndInitialize() throws Exception {
		synchronized (this) {

			try {
				prtln("reading config file located at: " + configFile);
				configXmlDoc = Dom4jUtils.getXmlDocument(configFile);
			} catch (Throwable t) {
				String msg = "Unable to read the NDR configuration file '" + configFile + "'. Message: " + t.getMessage();
				prtlnErr(msg);
				throw new Exception(msg);
			}

			// Set up the NDRIndexer to index NDR collections:
			String ndrApiBaseUrl = configXmlDoc.valueOf("/NDR_Collections/ndrApiBaseUrl");
			if (ndrApiBaseUrl == null || ndrApiBaseUrl.length() == 0) {
				String msg = "Unable to configure NDR collections. Missing required config element: 'ndrApiBaseUrl'";
				printStatusMessage(msg);
				throw new Exception(msg);
			}

			// For read-only operations, no agent handle or private key file is necessary (pass in null):
			NDRConstants.init(ndrApiBaseUrl, null, null);
		}
	}



	/**
	 *  Perform indexing when requested
	 *
	 * @param  indexingEvent  The event fired
	 * @exception  Exception  If error
	 */
	public void indexingActionRequested(IndexingEvent indexingEvent)
		 throws Exception {
		collectionIndexer = indexingEvent.getCollectionIndexer();
		switch (indexingEvent.getType()) {

						case IndexingEvent.BEGIN_INDEXING_ALL_COLLECTIONS:
						{
							//prtln("NDRIndexer started BEGIN_INDEXING_ALL_COLLECTIONS." + "\n\n");
							indexCollections(null);
							break;
						}

						case IndexingEvent.INDEXER_READY:
						{
							//prtln("NDRIndexer started INDEXER_READY!" + "\n\n");
							return;
						}

						case IndexingEvent.BEGIN_INDEXING_COLLECTION:
						{
							//prtln("NDRIndexer started BEGIN_INDEXING_COLLECTION!" + "\n\n");
							String key = indexingEvent.getCollectionKey();
							if (key != null && key.trim().length() > 0)
								indexCollections(key);
							return;
						}

						case IndexingEvent.ABORT_INDEXING:
						{
							//prtln("NDRIndexer ABORT_INDEXING!" + "\n\n");
							printStatusMessage("Stopping indexing...");
							abortIndexing = true;
							return;
						}

						case IndexingEvent.UPDATE_COLLECTIONS:
						{
							prtln("NDRIndexer started UPDATE_COLLECTIONS!" + "\n\n");
							updateCollections();
							return;
						}
						case IndexingEvent.CONFIGURE_AND_INITIALIZE:
						{
							prtln("NDRIndexer updating configuration" + "\n\n");
							configureAndInitialize();
							return;
						}
		}
	}


	/**
	 *  Index a collection or all collections.
	 *
	 * @param  collectionKey  The collection key/id, or null for all
	 * @exception  Exception  If error
	 */
	public void indexCollections(String collectionKey) throws Exception {
		// If collectionKey is null, index all collections, if not, index only that collection...

		synchronized (this) {
			if (indexingIsActive) {
				throw new Exception("NDRIndexer is already running.");
			}
			abortIndexing = false;
			indexingIsActive = true;
		}

		prtln("NDRIndexer started." + "\n\n");
		printStatusMessage("Indexing started.");

		// Initialize required NDR attributes and Sets TransformerFactory to a XSL 1.0 version:
		//NdrUtils.ndrTestSetup();


		// Configure all collections defined in the NCS:
		List ndrCollectionInfos = new ArrayList();
		try {
			ndrCollectionInfos = getCollectionsFromRemoteNCS();

			// Format type 'canonical_nsdl_dc' or 'native'
			String formatType = configXmlDoc.valueOf("/NDR_Collections/ncsManagedCollections/formatType");
			if (formatType == null || formatType.trim().length() == 0 || !(formatType.equalsIgnoreCase("canonical_nsdl_dc") || formatType.equalsIgnoreCase("native")))
				formatType = "canonical_nsdl_dc";

			if (!abortIndexing) {
				for (int i = 0; i < ndrCollectionInfos.size(); i++) {
					CollectionInfo collectionInfo = (CollectionInfo) ndrCollectionInfos.get(i);
					MetadataProviderReader mdpReader = null;
					try {
						mdpReader = getNdrMetadataProvider(collectionInfo.getHandle());
					} catch (Exception e) {
						String msg = "Error retrieving NDR MetadataProvider. Skipping collection '" + collectionInfo.getHandle() + "'. Message: " + e.getMessage();
						prtlnErr(msg);
						collectionIndexer.printStatusMessage(msg);
						continue;
					}

					String currentCollection = mdpReader.getSetSpec();
					String format = "canonical_nsdl_dc";
					if (formatType.equalsIgnoreCase("native"))
						format = mdpReader.getNativeFormat();

					if (collectionKey == null)
						indexCollection(currentCollection, mdpReader, format, collectionInfo.getNcsRecordXml());
					else if (collectionKey.equals(currentCollection)) {
						indexCollection(currentCollection, mdpReader, format, collectionInfo.getNcsRecordXml());
						break;
					}

					//indexNdrCollection( currentCollection, collectionKey );

					if (abortIndexing)
						ndrCollectionInfos.clear();
				}
			}
		} catch (Throwable t) {
			String msg = "Error getting NDR collections from NCS: " + t.getMessage();
			prtlnErr(msg);
			collectionIndexer.printStatusMessage(msg);
		}

		synchronized (this) {
			indexingIsActive = false;
		}

		printStatusMessage("Indexing completed.");

		prtln("NDRIndexer completed." + "\n\n");
	}



	/**
	 *  Update the collections that are configured in DDS.
	 *
	 * @exception  Exception  If error
	 */
	public void updateCollections() throws Exception {

		// Reload the config files:
		configureAndInitialize();
		
		// Start status message
		printStatusMessage("Begin adding/updating NDR collections...");		
		
		// Add collections that I know about:
		//List ncsCollections = getNcsCollections();
		//prtln("getNcsCollections() returned: " + Arrays.toString(ncsCollections.toArray()));

		boolean ndrErrorHasOccured = false;
		List ndrCollections = new ArrayList();

		// Configure collections defined in the NCS:
		List ndrCollectionInfos = new ArrayList();
		try {
			ndrCollectionInfos = getCollectionsFromRemoteNCS();

			// Format type 'canonical_nsdl_dc' or 'native'
			String formatType = configXmlDoc.valueOf("/NDR_Collections/ncsManagedCollections/formatType");
			if (formatType == null || formatType.trim().length() == 0 || !(formatType.equalsIgnoreCase("canonical_nsdl_dc") || formatType.equalsIgnoreCase("native")))
				formatType = "canonical_nsdl_dc";

			if (ndrErrorHasOccured == false) {
				ndrCollections = new ArrayList();
				for (int i = 0; i < ndrCollectionInfos.size(); i++) {
					CollectionInfo collectionInfo = (CollectionInfo) ndrCollectionInfos.get(i);
					MetadataProviderReader mdpReader = null;
					try {
						mdpReader = getNdrMetadataProvider(collectionInfo.getHandle());
					} catch (Exception e) {
						ndrErrorHasOccured = true;
						String msg = "Error retrieving NDR MetadataProvider. Skipping collection '" + collectionInfo.getHandle() + "'. Message: " + e.getMessage();
						prtlnErr(msg);
						collectionIndexer.printStatusMessage(msg);
						continue;
					}

					String format = "canonical_nsdl_dc";
					if (formatType.equalsIgnoreCase("native"))
						format = mdpReader.getNativeFormat();

					String collectionKey = mdpReader.getSetSpec();
					if (collectionKey == null) {
						prtlnErr("MetadataProviderReader returned null collectionKey/setSpec. Skipping collection for MDP " + mdpReader.getMetadataProviderFor());
					}
					else {
						ndrCollections.add(collectionKey);
						putCollection(mdpReader, collectionKey, format, collectionInfo.getNcsRecordXml());
					}
				}
			}
		} catch (Throwable t) {
			ndrErrorHasOccured = true;
			String msg = "Error getting NDR collections from NCS: " + t.getMessage();
			prtlnErr(msg);
			collectionIndexer.printStatusMessage(msg);
		}

		// Do not delete/remove collections if there has been an error collecting the list of collections.
		if (ndrErrorHasOccured) {
			prtln("updateCollections() ndrErrorHasOccured... ");
			collectionIndexer.printStatusMessage("One or more errors occured while updating the collections. No collections will be deleted...");
			return;
		}

		// Remove all collections in the repository that I don't know about:
		List currentConfiguredCollections = collectionIndexer.getConfiguredCollections();
		for (int i = 0; i < currentConfiguredCollections.size(); i++) {
			String collKey = (String) currentConfiguredCollections.get(i);
			if (!ndrCollections.contains(collKey) && !ndrCollectionInfos.contains(collKey)) {
				printStatusMessage("Removing collection '" + collKey + "'");
				collectionIndexer.deleteCollection((String) collKey);
			}
		}
		
		// Final status message
		printStatusMessage("Finished adding/updating NDR collections.");
	}


	private void putCollection(MetadataProviderReader mdpReader, String collectionKey, String format, String additionalMetadata) throws Exception {

		String collectionFormat = format;
		if (format.equalsIgnoreCase("canonical_nsdl_dc"))
			collectionFormat = "nsdl_dc";
		String collectionName = mdpReader.getSetName();
		String collectionDescription = "This collection resides in the NSDL Data Repository (NDR). It's NDR setSpec is " + mdpReader.getSetSpec();
				
		prtln("putCollection() collectionKey:" + collectionKey + " collectionFormat: " + collectionFormat);
		//if(!collectionIndexer.isCollectionConfigured(collectionKey))
		try {
			collectionIndexer.putCollection(collectionKey, collectionFormat, collectionName, collectionDescription, additionalMetadata);
			//collectionIndexer.putCollection(collectionKey, collectionFormat, collectionName, collectionDescription, additionalMetadata);
		} catch (PutCollectionException pce) {
			if (pce.getErrorCode().equals(PutCollectionException.ERROR_CODE_COLLECTION_EXISTS_IN_ANOTHER_FORMAT)) {
				collectionIndexer.printStatusMessage("Collection '" + collectionName + "' is in the repository but in a different format. Deleting...");
				collectionIndexer.deleteCollection(collectionKey);
				collectionIndexer.printStatusMessage("Adding collection '" + collectionName + " with the new format (key:" + collectionKey + " new format:" + collectionFormat + ")");
				collectionIndexer.putCollection(collectionKey, collectionFormat, collectionName, collectionDescription, additionalMetadata);
			}
		}
	}


	/* 	private List getNcsCollections() throws Exception {
		return doGetCollections("ncs");
	}
	private List getNdrCollectionHandles() throws Exception {
		return doGetCollections("ndr");
	}
	private List doGetCollections(String type) throws Exception {
		// Type must be one of "ncs" or "ndr"
		ArrayList collectionInfos = new ArrayList();
		if (configXmlDoc == null)
			throw new Exception("Unable to configure NDR collections. No config file found at " + configFile);
		List nodes = configXmlDoc.selectNodes("/NDR_Collections/collection[@type='" + type + "']");
		if (nodes == null)
			return collectionInfos;
		// Get the handle or key:
		for (int i = 0; i < nodes.size(); i++) {
			String handle = (String) ((Node) nodes.get(i)).valueOf("handle");
			if (handle == null || handle.length() == 0)
				handle = (String) ((Node) nodes.get(i)).valueOf("key");
			if (handle != null && handle.length() > 0)
				collectionInfos.add(handle);
		}
		return collectionInfos;
	} */

	/* 	private MetadataProviderReader getNcsMetadataProvider(String collectionKey) throws Exception {
		MetadataProviderReader mdpReader = null;
		// Grab the collection handle from the NDR:
		mdpReader = getMetadataProvider(collectionKey);
		prtln("MetadataProvider for " + collectionKey);
		prtln("\t" + "collectionId: " + mdpReader.getCollectionId());
		prtln("\t" + "collectionName: " + mdpReader.getCollectionName());
		prtln("\t" + "nativeFormat: " + mdpReader.getNativeFormat());
		return mdpReader;
	}*/

	private MetadataProviderReader getNdrMetadataProvider(String mdpHandle) throws Exception {
		MetadataProviderReader mdpReader = null;
		mdpReader = new MetadataProviderReader(mdpHandle);
		prtln("MetadataProvider handle " + mdpHandle);
		prtln("\t" + "setName: " + mdpReader.getSetName());
		prtln("\t" + "setSpec: " + mdpReader.getSetSpec());
		prtln("\t" + "nativeFormat: " + mdpReader.getNativeFormat());
		return mdpReader;
	}


	private boolean printOutput = true;


	private void indexCollection(String collectionKey, MetadataProviderReader mdpReader, String format, String additionalMetadata) throws Exception {
		if (abortIndexing)
			return;

		/* if (printOutput) {
			prtln("-----------------\nCollection Record");
			NdrUtils.pp(mdpReader.getCollectionRecord());
		} */
		// Ensure the collection record is configured
		String collectionFormat = format;
		if (collectionFormat.equals("canonical_nsdl_dc"))
			collectionFormat = "nsdl_dc";
		String collectionName = mdpReader.getSetName();
		String collectionDescription = collectionName;
		
		if (!collectionIndexer.isCollectionConfigured(collectionKey))
			collectionIndexer.putCollection(collectionKey, collectionFormat, collectionName, collectionDescription, additionalMetadata);

		if (abortIndexing)
			return;

		// Index the items:
		indexItemRecords(mdpReader, collectionKey, format);
	}


	/**
	 *  Gets the itemRecords for a given collection starting from the MetadataProvider.
	 *
	 * @param  mdp            MetadataProviderReader
	 * @param  collectionKey  The collection key
	 * @param  format         The format to index for this collection, "canonical_nsdl_dc" for canonical
	 */
	public void indexItemRecords(MetadataProviderReader mdp, String collectionKey, String format) {
		if (abortIndexing)
			return;

		CollectionIndexingSession collectionIndexingSession = collectionIndexer.getNewCollectionIndexingSession(collectionKey);

		printStatusMessage("Fetching list of record handles for collection '" + mdp.getSetName() + "'");

		List mdHandles = new ArrayList();
		String xmlFormat = format;
		if (xmlFormat.equals("canonical_nsdl_dc"))
			xmlFormat = "nsdl_dc";
		try {
			mdHandles = mdp.getItemHandles();
		} catch (Exception e) {
			prtln("could not get itemHandles for " + mdp.getHandle());
		}

		printStatusMessage("Indexing " + mdHandles.size() + " records for collection '" + mdp.getSetName() + "'...");

		/*
			iterate through metadata handles, grabbing itemRecords (from the datastream
			associated with the collection's "nativeFormat".
		*/
		int numHandles = (mdHandles == null ? 0 : mdHandles.size());
		prtln("\n------------\nthere are " + numHandles + " itemRecords");

		int messagesFrequency = 100; // How often to display status message of progress (num records)

		int numSuccess = 0;
		int numErrors = 0;
		int numProcessed = 0;

		Throwable errorThrown = null;

		int count = 0;
		for (Iterator i = mdHandles.iterator(); i.hasNext() && !abortIndexing; ) {
			String mdHandle = (String) i.next();
			try {
				prtln("Fetching record from mdReader handle:" + mdHandle + " format:" + xmlFormat);

				MetadataReader mdReader = new MetadataReader(mdHandle, xmlFormat);

				Document itemRecord;
				if (format.equals("canonical_nsdl_dc"))
					itemRecord = mdReader.getCanonicalNsdlDcItemRecord();
				else
					itemRecord = mdReader.getItemRecord();

				prtln("ID: " + mdReader.getRecordId() + " status: " + mdReader.getStatus() + " isValid: " + mdReader.getIsValid());

				String use_id = mdReader.getRecordId();
				if (use_id == null)
					use_id = mdHandle;

				// Index NCS native item: Make sure we're grabbing final, valid records only for NCS collections:
				if (!mdReader.isNcsMetadata() || (mdReader.getIsFinal() && mdReader.getIsValid())) {
					prtln("Put record for collection:" + collectionKey + " format:" + xmlFormat + " no: " + (++count) + " id:" + use_id);
					collectionIndexer.putRecord(itemRecord.asXML(), collectionKey, use_id, collectionIndexingSession);
					numSuccess++;
					numProcessed++;
				}
				else {
					prtln("Error... record fails NCS condition (mdReader.getIsFinal() && mdReader.getIsValid())");
				}
			} catch (Exception e) {
				errorThrown = e;
				prtln("could not get itemRecord for " + mdHandle + ": " + e);
				numErrors++;
				numProcessed++;
			}

			if (numProcessed > 0 && numProcessed % messagesFrequency == 0) {
				String msg = "Processed " + numProcessed +
					" of " + mdHandles.size() +
					" records for collection '" + mdp.getSetName() +
					" thus far (" + numSuccess + " successful, " + numErrors + " errors)";
				if (errorThrown != null)
					msg += ". One or more records could not be indexed because of errors. Most recent error message: " + errorThrown;
				printStatusMessage(msg);
			}
		}

		String msg = "Finished indexing collection '" + mdp.getSetName() + "'. Processed " + numProcessed + " records (" + numSuccess + " successful, " + numErrors + " errors)";
		if (errorThrown != null)
			msg += ". One or more records could not be indexed because of errors. Most recent error message: " + errorThrown;
		printStatusMessage(msg);

		if (abortIndexing)
			return;
		collectionIndexer.deletePreviousSessionRecords(collectionIndexingSession);
	}



	/**
	 *  Fetches collections from the NCS to determine which ones to index.
	 *
	 * @return                List of CollectionInfo Objects for each collection.
	 * @exception  Exception  If error
	 */
	private List getCollectionsFromRemoteNCS() throws Exception {
		int timoutMs = 6000;
		int s = 0;
		int n = 1000;

		prtln("updating collections from NCS");

		ArrayList collectionInfos = new ArrayList();
		if (configXmlDoc == null)
			throw new Exception("Unable to configure NDR collections. No config file found at " + configFile);

		String ncsSearchApiBaseUrl = configXmlDoc.valueOf("/NDR_Collections/ncsManagedCollections/ncsSearchApiBaseUrl");
		if (ncsSearchApiBaseUrl == null || ncsSearchApiBaseUrl.trim().length() == 0)
			return collectionInfos;

		List nodes = configXmlDoc.selectNodes("/NDR_Collections/ncsManagedCollections/ncsCollectionStatusFilter/ncsCollectionStatus");
		String nscStatusParams = "";
		// Construct status flag:
		for (int i = 0; i < nodes.size(); i++) {
			try {
				String status = (String) ((Node) nodes.get(i)).getText();
				if (status != null && status.length() > 0)
					nscStatusParams += "&dcsStatus=" + URLEncoder.encode(status, "utf-8");
			} catch (UnsupportedEncodingException e) {
				prtlnErr("Error encoding dcsStatus: " + e);
			}
		}

		String searchQueryConstraint = configXmlDoc.valueOf("/NDR_Collections/ncsManagedCollections/searchQueryConstraint");
		try {
			if (searchQueryConstraint != null && searchQueryConstraint.trim().length() > 0)
				searchQueryConstraint = "&q=" + URLEncoder.encode(searchQueryConstraint, "utf-8");
			else
				searchQueryConstraint = "";
		} catch (UnsupportedEncodingException e) {
			prtlnErr("Error encoding searchQueryConstraint: " + e);
		}

		// Loop until all collection records have been fetched from the NCS Web service, and build a DOM:
		String serviceRequestUrl = null;
		for (int i = 0; i < 200; i++) {
			serviceRequestUrl = ncsSearchApiBaseUrl + "?verb=Search&xmlFormat=ncs_collect" + nscStatusParams + "&n=" + n + "&s=" + s + searchQueryConstraint;

			prtln("Fetching collections from NCS. Request: " + serviceRequestUrl);
			Document fetched = Dom4jUtils.getXmlDocument(Dom4jUtils.localizeXml(TimedURLConnection.importURL(serviceRequestUrl, timoutMs)));
			String numResults = fetched.valueOf("/DDSWebService/Search/resultInfo/totalNumResults");
			String numReturned = fetched.valueOf("/DDSWebService/Search/resultInfo/numReturned");
			String offset = fetched.valueOf("/DDSWebService/Search/resultInfo/offset");
			String ddsErrorCode = fetched.valueOf("/DDSWebService/error/@code");

			if (ddsErrorCode != null && ddsErrorCode.equals("noRecordsMatch")) {
				prtln("No matching collections found in the NCS (noRecordsMatch). Request made was: " + serviceRequestUrl);
				break;
			}

			if (numResults == null || numResults.trim().length() == 0 || numResults.equals("0"))
				throw new Exception("No collections records available from NCS (numResults element was missing or empty... must be an error). Request made was: " + serviceRequestUrl);

			else {
				//List metadataProviderHandles = fetched.selectNodes("/DDSWebService/Search/results/record/head/additionalMetadata/ndr_info/metadataProviderHandle");
				//prtln("Fetch num recordNodes:" + recordNodes.size());

				List ncsCollectRecords = fetched.selectNodes("/DDSWebService/Search/results/record");

				if (ncsCollectRecords == null || ncsCollectRecords.size() == 0)
					break;

				// Add all metadataProviders (Collections):
				for (int j = 0; j < ncsCollectRecords.size() && collectionInfos.size() < MAX_NUM_COLLECTIONS; j++) {
					String handle = ((Node) ncsCollectRecords.get(j)).valueOf("head/additionalMetadata/ndr_info/metadataProviderHandle");
					prtln("NCS OAI collection handle: " + handle);

					String ncsRecordXml = ((Node) ncsCollectRecords.get(j)).selectSingleNode("metadata/record").asXML();
					//ncsRecordXml =  null;

					if (handle != null && handle.trim().length() > 0)
						collectionInfos.add(new CollectionInfo(handle.trim(), ncsRecordXml));
				}
			}

			/*
				NDR explorer:
					http://ncs.nsdl.org/mgr/ndr/ndr.do?command=explore&handle=2200/20061002130324510T
				Some example handles for dev:
					Math forum MP:
						2200/20061002124657491T
					Math Forum aggregator:
						2200/20061002124656295T
					Math Forum metadata:
						2200/20091103184302321T
						2200/20091103184717170T
					MSP2 MP:
						2200/20061002130512109T
					MSP2 metadata:
						2200/20091209135045391T
						2200/20091209135100332T
			*/
			//prtln("Fetch numReturned:" + numReturned + " offset:" + offset + " s:" + s);
			if (numReturned == null || numReturned.equals("0"))
				break;
			s += n;
		}

		//prtln("getCollectionsFromRemoteNCS() returning: " + Arrays.toString(collectionInfos.toArray()));
		return collectionInfos;
	}



	/**
	 *  Gets a metadataProviderReader given a collection key.<p>
	 *
	 *
	 *
	 * @param  collection     collection key
	 * @return                The metadataProvider value
	 * @exception  Exception  If error
	 */
	private MetadataProviderReader getMetadataProvider(String collection) throws Exception {
		return NdrUtils.getMetadataProvider(collection);
	}


	private PropertiesManager getProperties(File configDir) throws Exception {
		// First look in the configuration directory...
		if (configDir != null && configDir.isDirectory())
			return new PropertiesManager(new File(configDir, "org.dlese.dpc.dds.ndr.NDRIndexer.properties").toString());
		// Then look in the classpath...
		else
			return new PropertiesManager("org.dlese.dpc.dds.ndr.NDRIndexer.properties");
	}


	private void printStatusMessage(String msg) {
		collectionIndexer.printStatusMessage("NRDIndexer: " + msg);
	}


	private class CollectionInfo {
		private String _ndr_handle = null;
		private String _ncsRecordXml = null;


		private CollectionInfo(String handle, String ncsRecordXml) {
			_ndr_handle = handle;
			_ncsRecordXml = ncsRecordXml;
		}


		private String getHandle() {
			return _ndr_handle;
		}


		private String getNcsRecordXml() {
			return _ncsRecordXml;
		}


		/**
		 *  Equal if the handles are not null and are the same.
		 *
		 * @param  obj  The other Object
		 * @return      True if the handles are the same
		 */
		public boolean equals(Object obj) {
			if (obj == null)
				return false;
			CollectionInfo other = (CollectionInfo) obj;
			if (this.getHandle() == null || other.getHandle() == null)
				return false;
			return this.getHandle().equals(other.getHandle());
		}
	}



	/* ---------------------- Debug methods ----------------------- */
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
	private final static void prtlnErr(String s) {
		System.err.println(getDateStamp() + " NDRIndexer Error: " + s);
	}


	/**
	 *  Output a line of text to standard out, with datestamp, if debug is set to true.
	 *
	 * @param  s  The String that will be output.
	 */
	private final static void prtln(String s) {
		if (debug) {
			System.out.println(getDateStamp() + " NDRIndexer: " + s);
		}
	}


	/**
	 *  Sets the debug attribute of the NDRIndexer object
	 *
	 * @param  db  The new debug value
	 */
	public final void setDebug(boolean db) {
		debug = db;
	}
}

