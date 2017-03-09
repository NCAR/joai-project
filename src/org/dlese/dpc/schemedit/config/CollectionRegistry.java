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
package org.dlese.dpc.schemedit.config;

import java.io.*;
import java.util.*;

import org.dlese.dpc.xml.*;
import org.dlese.dpc.index.SimpleLuceneIndex;
import org.dlese.dpc.index.ResultDoc;
import org.dlese.dpc.index.ResultDocList;
import org.dlese.dpc.index.reader.DocReader;
import org.dlese.dpc.index.reader.XMLDocReader;
import org.dlese.dpc.schemedit.*;

/**
 *  Holds {@link org.dlese.dpc.schemedit.config.CollectionConfig} instances in a
 *  map structure, keyed by collection id (e.g., "dcc"); The registry is
 *  initialized during system startup by reading from a configDir where the
 *  collection config files are located.
 *
 * @author     ostwald <p>
 *
 *      $Id: CollectionRegistry.java,v 1.31 2010/07/14 00:18:49 jweather Exp $
 * @version    $Id: CollectionRegistry.java,v 1.31 2010/07/14 00:18:49 jweather Exp $
 */
public class CollectionRegistry {
	private static boolean debug = true;
	
	private HashMap collections = null;
	private File configDir = null;
	
	private List masterStatusList = null;
	private HashMap masterStatusLabelMap = null;

	private long collectionConfigMod = 0;

	private String idFilesPath = ""; 
	private File defaultConfig = null;

	/**  no-argument Constructor for the CollectionRegistry object  */
	public CollectionRegistry() {
		// prtln ("instantiating registry");
		collections = new HashMap();
	}

	/**
	 *  Register collections from configuration directory
	 *
	 * @param  configDir      NOT YET DOCUMENTED
	 */
	public CollectionRegistry(File configDir, String idFilesPath, String defaultCollConfigPath) {
		this();
		
		this.idFilesPath = idFilesPath;
		this.defaultConfig = new File (defaultCollConfigPath);
		
		this.configDir = configDir;
		if (!configDir.exists()) {
			prtlnErr("configDir does not exist at " + configDir.getAbsolutePath());
			return;
		}

		File[] configFiles = configDir.listFiles(new XMLFileFilter());
		prtln("\n\n Initializing Collection Registry ... \n\tabout to read " + configFiles.length + " files from " + configDir.getAbsolutePath());

		for (int i = 0; i < configFiles.length; i++) {
			File configFile = configFiles[i];
			String configFileName = configFile.getName();
			CollectionConfig config = null;
			try {
				prtln("about to create CollectionConfig with " + configFileName);
				config = new CollectionConfig(configFile, this);
				if (config == null) {
					prtlnErr("failed to initialize CollectionConfig for " + configFile.getAbsolutePath());
					continue;
				}
			} catch (Throwable t) {
				prtlnErr("ERROR: processing: " + configFile);
				t.printStackTrace();
				continue;
			}

			// really should clear the "default.xml" files from collections (in dcs_conf) ...
			if (configFileName.equals("default.xml")) {
				prtlnErr ("WARNING: OLD CONFIG FILE ENCOUNTERED");
				continue;
			}
			else {
				try {
					register(config);
				} catch (Exception e) {
					prtlnErr ("WARNING: collection (" + config.getId() + ") could not be registered: " + e.getMessage());
				}
			}
		}
		updateMasterStatusList();
	}

	public boolean isNDRCollection (String collection) {
		CollectionConfig config = this.getCollectionConfig(collection);
		return (config != null && config.isNDRCollection());
	}
	
	public boolean isRegistered (String collectionKey) {
		return this.getIds().contains (collectionKey);
	}

	public CollectionConfig getMasterCollectionConfig () {
		return this.getCollectionConfig("collect", false);
	}
	
	public String getIdFilesPath () {
		return this.idFilesPath;
	}

	/**
	 *  find collectionConfig having provided handle as its MetadataProviderHandle.
	 *
	 * @param  handle  NOT YET DOCUMENTED
	 * @return         null if provided handle is empty or if matching config not found.
	 */
	public CollectionConfig findCollectionByHandle(String handle) {
		if (handle == null || handle.trim().length() == 0)
			return null;
		for (Iterator i = this.collections.values().iterator(); i.hasNext(); ) {
			CollectionConfig config = (CollectionConfig) i.next();
			if (handle.equals(config.getMetadataProviderHandle())) {
				return config;
			}
		}
		return null;
	}

	/**
	 *  Gets the collectionConfig attribute of the CollectionRegistry object
	 *
	 * @param  collection  collectionKey (e.g., "dcc")
	 * @return             The collectionConfig value
	 */
	public CollectionConfig getCollectionConfig(String collection) {
		return getCollectionConfig(collection, true);
	}
	
	/**
	 *  Gets a {@link org.dlese.dpc.schemedit.config.CollectionConfig} instance
	 *  corresponding to the provided collection.
	 *
	 * @param  collection        Description of the Parameter
	 * @param  createIfNotFound  NOT YET DOCUMENTED
	 * @return                   The collectionConfig value
	 */
	public CollectionConfig getCollectionConfig(String collection, boolean createIfNotFound) {
		CollectionConfig config = (CollectionConfig) collections.get(collection);
		if (config == null) {
			// prtln("collectionConfig for \"" + collection + "\" not found");
			
			// first look for an existing config file for this collection
			File configFile = new File(configDir, collection + ".xml");
			if (configFile.exists()) {
				// prtln(" ... config file found - loading");
				config = new CollectionConfig(configFile, this);
			}
			else if (createIfNotFound) {
				// prtln(" ... config file NOT found, using default to build new one");
				// File defaultFile = new File(configDir, DEFAULT_CONFIG_FILE);
				// config = new CollectionConfig(defaultFile, this);
				config = new CollectionConfig(defaultConfig, this);
				config.getConfigReader().setSource(new File(configDir, collection + ".xml"));
				config.setId(collection);
				if (config.getFinalStatusLabel() == null || 
					config.getFinalStatusLabel().trim().length() == 0) {
					config.setFinalStatusLabel(StatusFlags.DEFAULT_FINAL_STATUS);
				}
				config.flush();
			}
			// prtln ("new collectionConfig instance created: " + config.toString());
			if (config != null) {
				try {
					register(config);
				} catch (Exception e) {
					prtlnErr ("WARNING: collection (" + config.getId() + ") could not be registered: " + e.getMessage());
				}
			}
		}
		return config;
	}

	// ------------ ID Machinery and Convenience methods ---------------
	
 	private HashMap getIdPrefixMap () {
		HashMap idPrefixMap = new HashMap();
		for (Iterator i=this.getIds().iterator();i.hasNext();) {
			String key = (String)i.next();
			idPrefixMap.put (key, this.getIdPrefix(key));
		}
		return idPrefixMap;
	}
	
	public boolean isDuplicateIdPrefix (String idPrefix) {
		return isDuplicateIdPrefix (null, idPrefix);
	}
	
	public boolean isDuplicateIdPrefix (String collectionKey, String idPrefix) {
		// prtln ("isDuplicatePrefix() key: " + collectionKey + "  prefix: " + idPrefix);
		for (Iterator i=getIdPrefixMap().entrySet().iterator();i.hasNext();) {
			Map.Entry entry = (Map.Entry)i.next();
			String aPrefix = (String) entry.getValue();
			String aKey = (String) entry.getKey();
			if (aPrefix != null && aPrefix.trim().toUpperCase().equals(idPrefix.trim().toUpperCase()) &&
				aKey != null && !aKey.equals(collectionKey)) {
				return true;
			}
		}
		return false;
	}
	
	/**
	* Convenience method to obtain a new record ID from specified collection (which must exist)
	*/
	public String getNextID (String collectionKey) {
		try {
			return this.getCollectionConfig(collectionKey, false).nextID();
		} catch (Throwable e) {
			prtlnErr ("nextID WARNING: could not get new ID for " + collectionKey + " (" + e.getMessage() + ")");
		}
		return null;
	}
	
	/*
	* Convenience method to obtain IdPrefix from specified collection (which must exist)
	*/
	public String getIdPrefix (String collectionKey) {
		try {
			return this.getCollectionConfig(collectionKey, false).getIdPrefix();
		} catch (Throwable e) {
			prtlnErr ("getIdPrefix WARNING: could not get prefix for " + collectionKey + " (" + e.getMessage() + ")");
		}
		return null;
	}
	
	/**
	* Obtain IDGenerator instance for specified collection (which must be registered).
	*/
 	public IDGenerator getIDGenerator (String collectionKey) {
		if (this.getIds().contains(collectionKey))
			return this.getCollectionConfig(collectionKey, false).getIDGenerator();
		return null;
	}
	
	/**
	 *  Initialize the IDGenerator for each collection in the index so that the next ID to be
	 assigned will increment the last assigned ID for that collection.
	 */
	public void initializeIDGenerators(SimpleLuceneIndex index) {
		prtln("\ninitializing idGenerators");
		
		// is there any reason to re-create the idPrefixMap now that it is under CollectionRegistry control??
		/* idPrefixMap = new IDPrefixMap(idPrefixesFile); */
		
		if (index == null) {
			return;
		}
		List cols = index.getTerms("collection");
		// collection

		if (cols == null || cols.size() == 0) {
			prtln("  ... initializeIDGenerators() no collections found, returning");
			return;
		}

		prtln(cols.size() + " collections found in index");
		for (Iterator i = cols.iterator(); i.hasNext(); ) {
			// the collection term is "0"+collectionKey
			String collectionKey = ((String) i.next()).substring(1);
			CollectionConfig collectionConfig = this.getCollectionConfig(collectionKey, false);
			if (collectionConfig == null) {
				prtln ("initializeIDGenerators WARNING: collectionConfig not found for " + collectionKey);
				continue;
			}
			try {
				initializeIDGenerator (collectionConfig, index);
			} catch (Throwable e) {
				prtlnErr ("failed to initialize IDGenerator for " + collectionKey);
			}
		}
	}
	
	public void initializeIDGenerator (CollectionConfig collectionConfig, SimpleLuceneIndex index) throws Exception {
		
		if (collectionConfig == null)
			throw new Exception ("collectionConfig not found");
		
		String collectionKey = collectionConfig.getId();
		
		// make id list for this collection
		String query = "(collection:0*)  AND collection:0" + collectionKey;
		ResultDocList results = index.searchDocs(query);
		// prtln (results.length + " results found in index for " + collectionKey);
		ArrayList idList = new ArrayList();
		for (int j = 0; j < results.size(); j++) {
			ResultDoc result = (ResultDoc) results.get(j);
			DocReader docReader = result.getDocReader();
			if (docReader instanceof XMLDocReader) {
				String id = ((XMLDocReader) docReader).getId();
				idList.add(id);
			}
		}
		collectionConfig.getIDGenerator().init (idList);
		collectionConfig.getIDGenerator().report();
	}

	
	// ----------------------------------------------
	
	/**
	 *  Gets the collectionConfigMod attribute of the CollectionRegistry object
	 *
	 * @return    The collectionConfigMod value
	 */
	public long getCollectionConfigMod() {
		if (collectionConfigMod == 0)
			setCollectionConfigMod();
		return collectionConfigMod;
	}


	/**  Sets the collectionConfigMod attribute of the CollectionRegistry object */
	public void setCollectionConfigMod() {
		collectionConfigMod = new Date().getTime();
	}


	/**
	 *  Gets the finalStatusLabel attribute of the CollectionRegistry object
	 *
	 * @param  collection  NOT YET DOCUMENTED
	 * @return             The finalStatusLabel value
	 */
	public String getFinalStatusLabel(String collection) {
		CollectionConfig config = getCollectionConfig(collection);
		if (config == null) {
			prtln("getFinalStatusLabel: cannot find collectionConfig for " + collection);
			return StatusFlags.DEFAULT_FINAL_STATUS;
		}
		return config.getFinalStatusLabel();
	}


	/**
	 *  Gets a list of the finalStatusFlags defined by each collection. 
	 *
	 * @return    The finalStatusFlags value
	 */
	public List getFinalStatusFlags() {
		List list = new ArrayList();
		for (Iterator i = getIds().iterator(); i.hasNext(); ) {
			String collection = (String) i.next();
			CollectionConfig collectionConfig = getCollectionConfig(collection);
			list.add(collectionConfig.getFinalStatusFlag());
		}
		return list;
	}

	/**
	 *  Returns a list of all status flags defined by all registered collections.
	 *
	 * @return    The masterStatusList value
	 */
	public List getMasterStatusList() {
		if (masterStatusList == null) {
			updateMasterStatusList();
		}
		return masterStatusList;
	}


	/**  NOT YET DOCUMENTED */
	public void updateMasterStatusList() {
		// prtln ("updateMasterStatusList()");
		masterStatusList = new ArrayList();

		// build list of all known CollectionConfig instances
		ArrayList allInfos = new ArrayList();
		allInfos.addAll(collections.values());

		// Collect the unique status uniqueLabels
		// prtln ("about to iterate over " + allInfos.size() + " collectionConfigs");
		for (Iterator i = allInfos.iterator(); i.hasNext(); ) {
			CollectionConfig collectionConfig = (CollectionConfig) i.next();
			// prtln (" collection: " + collectionConfig.getId());
			List statusFlags = collectionConfig.getStatusFlags();
			for (Iterator j = statusFlags.iterator(); j.hasNext(); ) {
				StatusFlag flag = (StatusFlag) j.next();
				// prtln ("\t"+ flag.getValue());
				masterStatusList.add(flag);
			}
		}
		masterStatusLabelMap = null;
		setCollectionConfigMod();
	}


	/**
	 *  Gets the masterStatusLabelMap attribute of the CollectionRegistry object
	 *
	 * @return    The masterStatusLabelMap value
	 */
	public Map getMasterStatusLabelMap() {
		if (masterStatusLabelMap == null) {
			masterStatusLabelMap = new HashMap();
			for (Iterator i = getMasterStatusList().iterator(); i.hasNext(); ) {
				try {
					StatusFlag flag = (StatusFlag) i.next();
					String label = flag.getLabel();
					if (label != null && label.length() > 0) {
						List flags;
						if (masterStatusLabelMap.containsKey(label)) {
							flags = (List) masterStatusLabelMap.get(label);
						}
						else {
							flags = new ArrayList();
						}
						flags.add(flag);
						masterStatusLabelMap.put(label, flags);
					}
				} catch (Exception e) {
					prtln("getMasterStatusLabelMap error: " + e.getMessage());
					e.printStackTrace();
					break;
				}
			}
		}
		return masterStatusLabelMap;
	}


	/**
	 *  Register a collection by placing it's CollectionConfig instance in the map.
	 *
	 * @param  config  Description of the Parameter
	 */
	public void register(CollectionConfig config) throws Exception {
		String id = config.getId();
		collections.put(id, config);
		updateMasterStatusList();
		prtln("registered " + id + " (prefix: " + (String)this.getIdPrefixMap().get (id) + ")\n");
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  id  NOT YET DOCUMENTED
	 */
	public void unregister(String id) {
		CollectionConfig config = getCollectionConfig (id, false);
		collections.remove(id);
		if (config != null)
			config.destroy();
		updateMasterStatusList();
		prtln("unregistered " + id + "\n");
	}


	/**
	 *  Remove the CollectionConfig from the registry.
	 *
	 * @param  config  Description of the Parameter
	 */
	public void unregister(CollectionConfig config) {
		unregister(config.getId());
	}


	/**
	 *  Unregister a collection and deletes it's configuration file.
	 *
	 * @param  collection  Description of the Parameter
	 */
	public void deleteCollection(String collection) {
		// CollectionConfig config = this.getCollectionConfig(collection);
		CollectionConfig config = (CollectionConfig) collections.get(collection);
		if (config == null) {
			prtln("WARNING: deleteCollection(): config not found for " + collection);
			String s = "Registered collection keys:";
			for (Iterator i = getIds().iterator(); i.hasNext(); ) {
				String key = (String) i.next();
				s += "\n\t" + key;
			}
			prtln(s);
			return;
		}
		deleteCollection(config);
	}


	/**
	 *  Unregister a collection and delete it's configuration file.
	 *
	 * @param  config  Description of the Parameter
	 */
	public void deleteCollection(CollectionConfig config) {
		unregister(config);
		CollectionConfigReader configReader = config.getConfigReader();
		File sourceFile = configReader.getSource();
		// sourceFile.delete();
		try {
			String deletedFileName = config.getId() + ".deleted";
			File deletedFile = new File(sourceFile.getParentFile(), deletedFileName);
			// unlike unix, windows apparently refuses to rename over existing file
			if (deletedFile.exists() && !deletedFile.delete()) {
				prtln("WARNING: existing file at " + deletedFile + " could not be deleted");
			}
			if (!sourceFile.renameTo(deletedFile))
				throw new Exception("Could not rename " + sourceFile.getAbsolutePath() + " to " + deletedFileName);
		} catch (Throwable t) {
			prtln("WARNING: deleteCollection error: " + t.getMessage());
		}
		config.destroy();
		setCollectionConfigMod(); // force update of status and editor select lists in query interface
	}


	/**
	 *  Print the items of the mdvMap for debugging purposes.
	 *
	 * @return    Description of the Return Value
	 */
	public String toString() {
		String s = "Collection Registry";
		for (Iterator i = getIds().iterator(); i.hasNext(); ) {
			String key = (String) i.next();
			s += "\n\t" + key;
			/* s += "\n **" + key + " **";
			CollectionConfig config = (CollectionConfig) collections.get(key);
			s += config.toString(); */
		}
		return s;
	}


	/**
	 *  Returns the keys for the registered collections.
	 *
	 * @return    The ids value
	 */
	public Set getIds() {
		return collections.keySet();
	}


	/**
	 *  The number of registered collections
	 *
	 * @return    NOT YET DOCUMENTED
	 */
	public int size() {
		return collections.size();
	}


	/**  Description of the Method  */
	public void destroy() {
		prtln("detroying registered collections");
		for (Iterator i = collections.values().iterator(); i.hasNext(); ) {
			CollectionConfig config = (CollectionConfig) i.next();
			config.destroy();
		}
	}


	/**
	 *  Description of the Method
	 *
	 * @param  s  Description of the Parameter
	 */
	private static void prtln(String s) {
		if (debug) {
			SchemEditUtils.prtln (s, "CollectionRegistry");
		}
	}

	private static void prtlnErr (String s) {
		SchemEditUtils.prtln (s, "CollectionRegistry");
	}
	
}

