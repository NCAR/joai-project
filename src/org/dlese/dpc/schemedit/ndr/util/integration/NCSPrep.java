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
package org.dlese.dpc.schemedit.ndr.util.integration;

import org.dlese.dpc.schemedit.ndr.util.NCSCollectReader;
import org.dlese.dpc.schemedit.ndr.util.NCSWebServiceClient;
import org.dlese.dpc.schemedit.dcs.DcsDataRecord;
import org.dlese.dpc.schemedit.MetaDataFramework;
import org.dlese.dpc.schemedit.config.CollectionConfigReader;
import org.dlese.dpc.ndr.apiproxy.*;
import org.dlese.dpc.ndr.NdrUtils;
import org.dlese.dpc.ndr.reader.*;
import org.dlese.dpc.ndr.request.*;
import org.dlese.dpc.xml.Dom4jUtils;
import org.dlese.dpc.xml.XMLFileFilter;
import org.dlese.dpc.util.Files;
import org.dlese.dpc.util.strings.FindAndReplace;
import org.dlese.dpc.index.SimpleLuceneIndex;
import org.dom4j.*;
import java.util.*;
import java.io.File;
import java.net.*;

/**
 *  Utilities for working with NSDL Collection records housed in the NCS
 *  instance.
 *
 * @author    ostwald
 */
public class NCSPrep {
	private static boolean debug = true;

	// String recordsPath = "C:/Documents and Settings/ostwald/devel/dcs-instance-data/ndr/records";
	String recordsPath = CIGlobals.RECORDS_DIR;

	MappingsManager mm = null;

	// String dcs_data_config = "/Users/ostwald/devel/projects/dcs-project/web/WEB-INF/data/configuration/common-frameworks/dcs_data.xml";
	String frameworks = CIGlobals.FRAMEWORK_CONFIG_DIR;
	// "C:/Program Files/Apache Software Foundation/Tomcat 5.5/var/dcs_conf/frameworks/";

	String dcs_data_config = frameworks + "dcs_data.xml";
	String ncs_collect_config = frameworks + "ncs_collect.xml";

	MetaDataFramework dcsDataFramework = null;
	MetaDataFramework ncsCollectFramework = null;

	String collection = "1201216476279";

	File[] dcsDataFiles = null;
	File[] ncsCollectFiles = null;

	Iterator mappings = null;
	int numMappings = -1;


	/**
	 *  Constructor for the NCSPrep object
	 *
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	NCSPrep() throws Exception {

		mm = new MappingsManager();
		mappings = mm.getMappings().iterator();
		numMappings = mm.getMappings().size();

		File dcsDataDir = getDcsDataRecordsDir();
		prtln("dcsDataDir: " + dcsDataDir);
		dcsDataFiles = dcsDataDir.listFiles(new XMLFileFilter());

		File ncsCollectDir = getNcsCollectRecordsDir();
		prtln("ncsCollectDir: " + ncsCollectDir);
		dcsDataFiles = getNcsCollectRecordsDir().listFiles(new XMLFileFilter());
		prtln("");
	}


	/* 	void initMap  () throws Exception {
		idMap = new HashMap();
		int ii = 0;
		for (Iterator i=mm.getMappings().iterator();i.hasNext();) {
			MappingInfo mappingInfo = (MappingInfo)i.next();
			String metadatahandle = mappingInfo.getMetadataHandle();
			String ncsrecordid = mappingInfo.getId();
			idMap.put (ncsrecordid, metadatahandle);
		}
	} */
	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	void updateCollectionMetadataTest() throws Exception {
		String id = "";
		updateCollectionMetadata(id);
	}


	/**
	 *  Return the reader for the id by first finding it's MD object and then the
	 *  MD's aggregator, and then finally using the agg to construct a
	 *  NSDLCollectionReader
	 *
	 * @param  id             NOT YET DOCUMENTED
	 * @return                The nSDLCollectionReader value
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	NSDLCollectionReader getNSDLCollectionReader(String id) throws Exception {
		DcsDataRecord dcsDataRecord = this.getDcsDataRecord(id);
		if (dcsDataRecord == null)
			throw new Exception("couldnt get DcsDataRecord for :" + id);
		String mdHandle = dcsDataRecord.getNdrHandle();
		if (mdHandle == null || mdHandle.trim().length() == 0) {
			prtln ("DcsDataRecord at " + dcsDataRecord.getSource());
			pp (dcsDataRecord.getDocument());
			throw new Exception("couldnt get mdHandle (from dcsDataRecord) for " + id);
		}
		MetadataReader mdReader = new MetadataReader(mdHandle);
		/* 		try {
			String mdpHandle = mdReader.getRelationship ("ncs:collectionMetadataFor");
			if (mdpHandle == null || mdpHandle.trim().length() == 0)
				throw new Exception ("no collectionMetadataFor MDP for: " + mdpHandle);
			MetadataProviderReader mdpReader = new MetadataProviderReader (mdpHandle);
			String aggHandle =  */
		String aggHandle = null;
		for (Iterator i = mdReader.getRelationshipValues("metadataFor").iterator(); i.hasNext(); ) {
			String handle = (String) i.next();
			NdrObjectReader ndrReader = new NdrObjectReader(handle);
			if (ndrReader.getObjectType() == NDRConstants.NDRObjectType.AGGREGATOR) {
				aggHandle = handle;
				break;
			}
		}
		if (aggHandle == null)
			throw new Exception("aggregator not found");
		return new NSDLCollectionReader(aggHandle);
	}


	/**
	 *  updates a collection config record (corresponding with provided collection
	 *  key) to have the aggregator and metadataprovider handles of the NCS
	 *  Collection Record corresponding with the provided id.<p>
	 *
	 *  NOTE: these values come from the CollectionConfigMappings.xml file.<p>
	 *
	 *  NCSRecord is obtained via web services. collection config record is local,
	 *  and then must be installed in NCS.
	 *
	 * @param  ncsrecordid    NOT YET DOCUMENTED
	 * @param  collectionkey  NOT YET DOCUMENTED
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	void updateCollectionConfig(String ncsrecordid, String collectionkey) throws Exception {
		prtln("\nncsrecordid: " + ncsrecordid + ", collectionkey: " + collectionkey);
		NSDLCollectionReader nsdlReader = getNSDLCollectionReader(ncsrecordid);
		String aggHandle = nsdlReader.aggregator.getHandle();
		if (aggHandle == null || aggHandle.trim().length() == 0)
			throw new Exception("aggregator handle not known for " + ncsrecordid);
		String mdpHandle = nsdlReader.mdp.getHandle();
		if (mdpHandle == null || mdpHandle.trim().length() == 0)
			throw new Exception("metadataProvider handle not known for " + ncsrecordid);
		CollectionConfigReader configReader = getCollectionConfigReader(collectionkey);
		if (configReader == null)
			throw new Exception("configReader could not be created for " + collectionkey);
		configReader.setAggregatorHandle(aggHandle);
		configReader.setMetadataProviderHandle(mdpHandle);
		prtln("\t aggHandle: " + aggHandle);
		prtln("\t mdpHandle: " + mdpHandle);
		// pp (configReader.getDocument());
		configReader.flush();
		prtln("wrote configReader to " + configReader.getSource());
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @return                NOT YET DOCUMENTED
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	List updateAllCollectionConfigs() throws Exception {
		String path = CIGlobals.BASE_DIR + "config-mappings/CollectionConfigMappings.xml";
		Document doc = Dom4jUtils.getXmlDocument(new File(path));
		List mappings = doc.selectNodes("/collections/mapping");
		prtln(mappings.size() + " config mappings found");
		List errors = new ArrayList();
		for (Iterator i = mappings.iterator(); i.hasNext(); ) {
			Element m = (Element) i.next();
			String ncsrecordid = MappingsManager.getElementText(m, "ncsrecordid");
			String collectionkey = MappingsManager.getElementText(m, "collectionkey");
			try {
				updateCollectionConfig(ncsrecordid, collectionkey);
			} catch (Exception e) {
				errors.add(e.getMessage());
			}
		}
		if (!errors.isEmpty()) {
			prtln("updateAllCollectionConfigs errors");
			for (Iterator i = errors.iterator(); i.hasNext(); )
				prtln("\t" + (String) i.next());
		}
		return errors;
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  id             NOT YET DOCUMENTED
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	void updateCollectionMetadata(String id) throws Exception {
		MappingInfo mappingInfo = getMappingInfo(id);
		String mdHandle = mappingInfo.getMetadataHandle();
		if (mdHandle == null || mdHandle.trim().length() == 0)
			throw new Exception("metadata handle not known for " + mappingInfo.getId());
		String mdpHandle = mappingInfo.getMetadataProviderHandle();
		if (mdpHandle == null || mdpHandle.trim().length() == 0)
			throw new Exception("metadataProvider handle not known for " + mappingInfo.getId());
		ModifyMetadataRequest request = new ModifyMetadataRequest(mdHandle);
		request.addNcsRelationshipCmd("collectionMetadataFor", mdpHandle, "add");
		request.submit();
		// prtln("updated: collection: " + id + "\n\tmdHandle: " + mdHandle + "\n\tmdp: " + mdpHandle);
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @return    List of errors
	 */
	List updateAllCollectionMetadata() {
		List errors = new ArrayList();
		int count = 0;
		while (mappings.hasNext()) {
			MappingInfo mappingInfo = (MappingInfo) mappings.next();
			String id = mappingInfo.getId();
			prtln(++count + "/" + this.numMappings);
			try {
				updateCollectionMetadata(id);
			} catch (Exception e) {
				errors.add("Metadata update error (" + id + "): " + e.getMessage());
				// e.printStackTrace();
			}
		}
		if (!errors.isEmpty()) {
			prtln("updateAllCollectionMetadata errors");
			for (Iterator i = errors.iterator(); i.hasNext(); )
				prtln("\t" + (String) i.next());
		}
		return errors;
	}

	// --------- DCS DATA STUFF ----------------

	/**
	 *  Gets the dcsDataFramework attribute of the NCSPrep object
	 *
	 * @return    The dcsDataFramework value
	 */
	MetaDataFramework getDcsDataFramework() {
		if (dcsDataFramework == null) {
			try {
				dcsDataFramework = new MetaDataFramework(dcs_data_config, null);
				dcsDataFramework.loadSchemaHelper();
			} catch (Throwable t) {
				prtln("couldnt get DcsDataFramework: " + t.getMessage());
				t.printStackTrace();
			}
		}
		return dcsDataFramework;
	}


	/**
	 *  Gets the dcsDataRecord attribute of the NCSPrep object
	 *
	 * @param  id  NOT YET DOCUMENTED
	 * @return     The dcsDataRecord value
	 */
	DcsDataRecord getDcsDataRecord(String id) {
		DcsDataRecord record = null;
		try {
			String filename = id + ".xml";
			File file = new File(getDcsDataRecordsDir(), filename);
			Document doc = Dom4jUtils.getXmlDocument(file);
			record = new DcsDataRecord(file, getDcsDataFramework(), null, null);
		} catch (Throwable t) {
			prtln("getDcsDataRecord ERROR: " + t.getMessage());
		}
		return record;
	}


	/**
	 *  Gets the collectionConfigReader attribute of the NCSPrep object
	 *
	 * @param  key  NOT YET DOCUMENTED
	 * @return      The collectionConfigReader value
	 */
	CollectionConfigReader getCollectionConfigReader(String key) {
		CollectionConfigReader reader = null;
		try {
			String filename = key + ".xml";
			File file = new File(CIGlobals.COLLECTION_CONFIG_DIR, filename);
			reader = new CollectionConfigReader(file);
		} catch (Throwable t) {
			prtln("getCollectionConfigReader ERROR: " + t.getMessage());
		}
		return reader;
	}

	///Users/ostwald/devel/records/ncsdata-03072008/dcs_data/ncs_collect/1201216476279/
	/**
	 *  Gets the dcsDataRecordsDir attribute of the NCSPrep object
	 *
	 * @return    The dcsDataRecordsDir value
	 */
	File getDcsDataRecordsDir() {
		String path = recordsPath + "/dcs_data/ncs_collect/" + collection;
		return new File(path);
	}


	/**  NOT YET DOCUMENTED */
	void showRecs() {
		prtln("there are " + dcsDataFiles.length + " files");
		for (int i = 0; i < dcsDataFiles.length; i++)
			prtln(dcsDataFiles[i].getName());
	}


	/**
	 *  insert the mdHandle from the MappingsManagerData in the dcsDataRecord for
	 *  id
	 *
	 * @param  id             NOT YET DOCUMENTED
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	void updateDcsDataRecord(String id) throws Exception {
		MappingInfo info = getMappingInfo(id);
		String mdHandle = info.getMetadataHandle();
		if (mdHandle == null || mdHandle.trim().length() == 0)
			throw new Exception("metadata object not found in NDR  for " + id);

		DcsDataRecord dcsDataRecord = getDcsDataRecord(id);
		if (dcsDataRecord == null) {
			throw new Exception("dcsDataRecord not found for " + id);
		}
		dcsDataRecord.setNdrHandle(mdHandle);
		Document doc = dcsDataRecord.getDocument();
		// pp(doc);

		dcsDataRecord.flushToDisk();

		prtln("wrote to " + dcsDataRecord.getSource());
	}


	/**
	 *  Call updateDcsDataRecord for each DcsDataRecord, assigning the collection
	 *  Metadata information to this record
	 *
	 * @return    NOT YET DOCUMENTED
	 */
	List updateAllDcsDataRecords() {
		List errors = new ArrayList();
		while (mappings.hasNext()) {
			MappingInfo info = (MappingInfo) mappings.next();
			String id = "";
			try {
				id = info.getId();
				updateDcsDataRecord(id);
			} catch (Exception upEx) {
				errors.add("Update error (" + id + "): " + upEx.getMessage());
			}
		}
		if (!errors.isEmpty()) {
			prtln("updateAllDcsDataRecords errors");
			for (Iterator i = errors.iterator(); i.hasNext(); )
				prtln("\t" + (String) i.next());
		}
		return errors;
	}

	// --------- NCS COLLECT -------------------------

	/**
	 *  Gets the ncsCollectRecordsDir attribute of the NCSPrep object
	 *
	 * @return    The ncsCollectRecordsDir value
	 */
	File getNcsCollectRecordsDir() {
		String path = recordsPath + "/ncs_collect/" + collection;
		return new File(path);
	}


	/**
	 *  Gets the ncsCollectFramework attribute of the NCSPrep object
	 *
	 * @return    The ncsCollectFramework value
	 */
	MetaDataFramework getNcsCollectFramework() {
		if (ncsCollectFramework == null) {
			try {
				ncsCollectFramework = new MetaDataFramework(ncs_collect_config, null);
				ncsCollectFramework.loadSchemaHelper();
			} catch (Throwable t) {
				prtln("couldnt get ncsCollectFramework: " + t.getMessage());
				t.printStackTrace();
			}
		}
		return ncsCollectFramework;
	}


	/**
	 *  Gets the nCSCollectReader attribute of the NCSPrep object
	 *
	 * @param  id  NOT YET DOCUMENTED
	 * @return     The nCSCollectReader value
	 */
	NCSCollectReader getNCSCollectReader(String id) {
		NCSCollectReader reader = null;
		try {
			String filename = id + ".xml";
			File file = new File(getNcsCollectRecordsDir(), filename);
			Document doc = Dom4jUtils.getXmlDocument(file);
			doc = Dom4jUtils.localizeXml(doc);
			DocumentHelper.makeElement(doc, NCSCollectReader.collectionAgentPath);
			reader = new NCSCollectReader(doc);
		} catch (Throwable t) {
			prtln("getNcsCollectReader ERROR: " + t.getMessage());
		}
		return reader;
	}


	/**
	 *  NOTE: not used for now. things work fine if the collection agent is not
	 *  specified in the ncs_collect record (unless we want to change the
	 *  collection agent).
	 *
	 * @return    List of errors
	 */
	List updateAllNcsCollectRecords() {
		List errors = new ArrayList();
		while (mappings.hasNext()) {
			MappingInfo info = (MappingInfo) mappings.next();
			String id = info.getId();
			try {
				updateNcsCollectRecord(id);
			} catch (Exception upEx) {
				errors.add("Update error (" + id + "): " + upEx.getMessage());
				// upEx.printStackTrace();
			}
		}
		if (!errors.isEmpty()) {
			prtln("updateAllNcsCollectRecords errors");
			for (Iterator i = errors.iterator(); i.hasNext(); )
				prtln("\t" + (String) i.next());
		}
		return errors;
	}


	/**
	 *  Gets the mappingInfo attribute of the NCSPrep object
	 *
	 * @param  id             NOT YET DOCUMENTED
	 * @return                The mappingInfo value
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	private MappingInfo getMappingInfo(String id) throws Exception {
		if (mm == null)
			throw new Exception("MappingsManager not initialzed!");
		MappingInfo info = mm.getMappingInfo(id);
		if (info == null)
			throw new Exception("mapping info not found for " + id);
		return info;
	}


	/**
	 *  insert the collection agent in the ncs_collect for id.<p>
	 *
	 *  NOTE: not used for now. things work fine if the collection agent is not
	 *  specified in the ncs_collect record (unless we want to change the
	 *  collection agent).
	 *
	 * @param  id             NOT YET DOCUMENTED
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	void updateNcsCollectRecord(String id) throws Exception {
		NCSCollectReader reader = getNCSCollectReader(id);
		if (reader == null) {
			throw new Exception("ncsCollectReader not found for " + id);
		}
		MappingInfo info = getMappingInfo(id);
		String agentHandle = info.getAggregatorAgent();
		if (agentHandle == null || agentHandle.trim().length() == 0)
			throw new Exception("agent not found for " + id);

		reader.setCollectionAgent(agentHandle);
		Document doc = reader.getWritableDocument();
		pp(doc);

		String filename = id + ".xml";
		File file = new File(getNcsCollectRecordsDir(), filename);
		// Dom4jUtils.writePrettyDocToFile(doc, file);
		prtln("wrote to " + file);
	}


	/**
	 *  The main program for the NCSPrep class
	 *
	 * @param  args           The command line arguments
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public static void main(String[] args) throws Exception {

		CIGlobals.setup();

		boolean verbosity = false;
		NdrRequest.setDebug(verbosity);
		NdrRequest.setVerbose(verbosity);

		try {
			NCSPrep prepster = new NCSPrep();

			/* Test updateDcsDataRecord with a single record */
			// prepster.updateDcsDataRecord("NSDL-COLLECTION-2669651");  // to test single record

			/* Update ALL DcsDataRecords */
			// prepster.updateAllDcsDataRecords();

			/* Test updateCollectionMetadata with a single collection
				(NOTE: probably want to set verbose to TRUE */
			// prepster.updateCollectionMetadata("NSDL-COLLECTION-3111259");

			/* Update ALL collection metadata records
				(NOTE: probably want to set verbose to FALSE */
			// prepster.updateAllCollectionMetadata();

			/* update a single collectionConfig record using data from CollectionConfigMappings.xml
				params: ncsrecordid, collectionkey
			*/
			// prepster.updateCollectionConfig("NSDL-COLLECTION-000-003-111-903", "1200091746017");

			/* update ALL collection configs as specified in CollectionConfigMappings */
			prepster.updateAllCollectionConfigs();

		} catch (Exception e) {
			prtln("Prepster ERROR: " + e.getMessage());
			e.printStackTrace();
		}
	}



	/**
	 *  Description of the Method
	 *
	 * @param  node  Description of the Parameter
	 */
	private static void pp(Node node) {
		prtln(Dom4jUtils.prettyPrint(node));
	}


	/**
	 *  Description of the Method
	 *
	 * @param  s  Description of the Parameter
	 */
	private static void prtln(String s) {
		String prefix = null;
		if (debug) {
			NdrUtils.prtln(s, prefix);
		}
	}
}

