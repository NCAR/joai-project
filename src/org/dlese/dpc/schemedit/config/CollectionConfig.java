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

import org.dlese.dpc.repository.RepositoryManager;
import org.dlese.dpc.xml.*;
import org.dlese.dpc.xml.schema.*;
import org.dlese.dpc.schemedit.MetaDataFramework;
import org.dlese.dpc.schemedit.SchemEditUtils;
import org.dlese.dpc.schemedit.dcs.*;

import java.io.*;
import java.util.*;

import org.dom4j.Document;
import org.dom4j.Element;
import java.text.*;

/**
 *  Holds information about a collection that is accessed via a {@link
 *  org.dlese.dpc.schemedit.config.CollectionConfigReader}.<p>
 *
 *  CollectionConfig supports getting and setting of workflow status flags,
 *  default export directory path, etc. Existing collection configuration files
 *  are read at system startup time, but there need not be a config file for
 *  each collection. Config files are created by copying a <b>default.xml</b>
 *  file and modifying it as settings are edited.<p>
 *
 *  CollectionConfig instances are registered in a {@link org.dlese.dpc.schemedit.config.CollectionRegistry}
 *  instance.
 *
 * @author     ostwald<p>
 *
 *      $Id $
 * @version    $Id: CollectionConfig.java,v 1.12 2009/03/20 23:33:56 jweather Exp $
 */
public class CollectionConfig {

	private static boolean debug = false;
	private CollectionConfigReader configReader = null;
	private CollectionRegistry registry = null;
	private IDGenerator idGenerator = null;


	/**
	 *  Constructor for the CollectionConfig object
	 *
	 * @param  configFile          Description of the Parameter
	 * @param  collectionRegistry  NOT YET DOCUMENTED
	 */
	public CollectionConfig(File configFile, CollectionRegistry collectionRegistry) {
		this.registry = collectionRegistry;
		try {
			this.configReader = new CollectionConfigReader(configFile);
		} catch (Exception e) {
			prtln("Initialize ERROR: " + e.getMessage());
		}
	}


	/**
	 *  Constructor for the CollectionConfig object
	 *
	 * @param  configReader  Description of the Parameter
	 */
	public CollectionConfig(CollectionConfigReader configReader) {
		this.configReader = configReader;
	}

	// item record ID code --------------------------

	/**
	 *  Gets the iDGenerator attribute of the CollectionConfig object
	 *
	 * @return    The iDGenerator value
	 */
	public IDGenerator getIDGenerator() {
		if (this.idGenerator == null) {
			String idFilePath = this.registry.getIdFilesPath() + File.separator + this.getId();
			this.idGenerator = new IDGenerator(idFilePath, this.getIdPrefix());
		}
		return this.idGenerator;
	}


	/**
	 *  Get the next ID to be assigned for this collection.
	 *
	 * @return    NOT YET DOCUMENTED
	 */
	public String nextID() {
		return this.getIDGenerator().nextID();
	}


	/**
	 *  Sets the idPrefix for this collection that is used to construct record IDs.
	 *
	 * @param  idPrefix  The new idPrefix value
	 */
	public void setIdPrefix(String idPrefix) {
		this.getIDGenerator().setIdPrefix(idPrefix);
		configReader.setIdPrefix(idPrefix);
	}


	/**
	 *  Gets the idPrefix for this collection.
	 *
	 * @return    The idPrefix value
	 */
	public String getIdPrefix() {
		return configReader.getIdPrefix();
	}


	// ------------------------------------------

	/**
	 *  Gets the configReader attribute of the CollectionConfig object
	 *
	 * @return    The configReader value
	 */
	public CollectionConfigReader getConfigReader() {
		return configReader;
	}


	/**
	 *  Ensure there is an element in the config file for each tuple defined in the
	 *  metaDataFramework for this collection.
	 *
	 * @param  framework      NOT YET DOCUMENTED
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public void updateTuples(MetaDataFramework framework) throws Exception {
		// prtln ("updateTuples() " + getId());
		String xmlFormat = getXmlFormat();
		if (framework == null)
			throw new Exception("framework not found for xmlFormat = " + xmlFormat);
		List collectionConfigPaths = framework.getSchemaPathMap().getCollectionConfigPaths();
		prtln(collectionConfigPaths.size() + " collectionConfigPaths defined for " + xmlFormat);
		for (Iterator j = collectionConfigPaths.iterator(); j.hasNext(); ) {
			SchemaPath schemaPath = (SchemaPath) j.next();
			String pathName = schemaPath.pathName;
			String fieldValue = getTupleValue(pathName);
			setTupleValue(pathName, fieldValue);
		}
		setTupleMap(getTupleMap());

		prtln("updated Tuples for " + getId());
	}


	/**
	 *  Gets the id attribute of the CollectionConfig object
	 *
	 * @return    The id value
	 */
	public String getId() {
		return configReader.getId();
	}


	/**
	 *  Sets the id attribute of the CollectionConfig object
	 *
	 * @param  id  The new id value
	 */
	public void setId(String id) {
		configReader.setId(id);
	}


	/**
	 *  Gets the xmlFormat attribute of the CollectionConfig object
	 *
	 * @return    The xmlFormat value
	 */
	public String getXmlFormat() {
		return configReader.getXmlFormat();
	}


	/**
	 *  Sets the xmlFormat attribute for this collection.
	 *
	 * @param  xmlFormat  The new xmlFormat value
	 */
	public void setXmlFormat(String xmlFormat) {
		prtln("setting xmlFormat to " + xmlFormat);
		configReader.setXmlFormat(xmlFormat);
	}
	
	public boolean getSuggestionsAllowed () {
		return configReader.getAllowSuggestions();
	}
	
	public void setSuggestionsAllowedd (boolean allowed) {
		configReader.setAllowSuggestions(allowed);
	}
	
	/**  
	* Removes all content from the NDR info element.
	*/
	public void clearNdrInfo() {
		configReader.clearNdrInfo();
	}


	/**
	 *  Gets the authority attribute of the CollectionConfig object
	 *
	 * @return    The authority value
	 */
	public String getAuthority() {
		return configReader.getAuthority();
	}


	/**
	 *  Convenience method to determine whether this collection is managed in the NDR.
	 *
	 * @return    true if the collection is managed in the NDR.
	 */
	public boolean isNDRCollection() {
		return ("ndr".equals(this.getAuthority()));
	}


	/**
	 *  Sets the authority attribute for this collection, which is used to determine whether
	 the collection is managed in the NDR.
	 *
	 * @param  authority  The new authority value
	 */
	public void setAuthority(String authority) {
		configReader.setAuthority(authority);
	}


	/**
	 *  Gets the handle of this collections metadataProvider object in the NDR.
	 *
	 * @return    The metadataProviderHandle value
	 */
	public String getMetadataProviderHandle() {
		return configReader.getMetadataProviderHandle();
	}


	/**
	 *  Sets the handle of this collections metadataProvider object in the NDR.
	 *
	 * @param  metadataProviderHandle  The new metadataProviderHandle value
	 */
	public void setMetadataProviderHandle(String metadataProviderHandle) {
		configReader.setMetadataProviderHandle(metadataProviderHandle);
	}


	/**
	 *  Gets the handle of the NDR aggregator object for this collection.
	 *
	 * @return    The aggregatorHandle value
	 */
	public String getAggregatorHandle() {
		return configReader.getAggregatorHandle();
	}


	/**
	 *  Sets the handle of the NDR aggregator object for this collection.
	 *
	 * @param  aggregatorHandle  The new aggregatorHandle value
	 */
	public void setAggregatorHandle(String aggregatorHandle) {
		configReader.setAggregatorHandle(aggregatorHandle);
	}

	/**
	 *  Gets the handle of the NDR agent object for this collection.
	 *
	 * @return    The agentHandle value
	 */
	public String getAgentHandle() {
		return configReader.getAgentHandle();
	}


	/**
	 *  Sets the handle of the NDR agent object for this collection.
	 *
	 * @param  agentHandle  The new agentHandle value
	 */
	public void setAgentHandle(String agentHandle) {
		configReader.setAgentHandle(agentHandle);
	}
	
	
	/**
	 *  Gets the ndrOaiLink for this collection, which is the "itemId" of
	 * Collection Metadata records stored in the NDR, and which is inserted in
	 the InfoStream of item-level metadata records when they are written to the NDR.
	 *
	 * @return    The ndrOaiLink value
	 */
	public String getNdrOaiLink() {
		return configReader.getNdrOaiLink();
	}


	/**
	 *  Sets the ndrOaiLink attribute of the CollectionConfig object
	 *
	 * @param  link  The new ndrOaiLink value
	 */
	public void setNdrOaiLink(String link) {
		configReader.setNdrOaiLink(link);
	}

	public String getNdrServer() {
		return configReader.getNdrServer();
	}


	/**
	 *  Sets the ndrServer attribute of the CollectionConfig object
	 *
	 * @param  link  The new ndrServer value
	 */
	public void setNdrServer(String server) {
		configReader.setNdrServer(server);
	}

	/**
	 *  The StatusMap holds mappings from the StatusFlag values defined by this
	 *  collection to their corresponding StatusFlag instances.
	 *
	 * @return    The statusMap value
	 */
	public Map getStatusMap() {
		return configReader.getStatusMap();
	}


	/**
	 *  Gets the tupleMap for this collection. The tupleMap contains name/value pairs
	 * that are written into new metadata records.
	 *
	 * @return    The tupleMap value
	 */
	public Map getTupleMap() {
		return configReader.getTupleMap();
	}


	/**
	 *  Gets the setInfo for this collection from the RepositoryManager.
	 *
	 * @param  rm  Description of the Parameter
	 * @return     The setInfo value
	 */
	public DcsSetInfo getSetInfo(RepositoryManager rm) {
		return SchemEditUtils.getDcsSetInfo(getId(), rm);
	}


	/**
	 *  Write the contents of this collection's StatusMap to the collection-config
	 *  file.
	 *
	 * @param  statusMap  The new statusMap value
	 */
	public void setStatusMap(Map statusMap) {
		try {
			configReader.setStatusMap(statusMap);
		} catch (Exception e) {
			prtlnErr("setStatusMap error: " + e.getMessage());
		}
	}


	/**
	 *  Sets the tupleMap for this collection, which contains name, value pairs that
	 * are automatically inserted into new metadata records.
	 *
	 * @param  tupleMap  The new tupleMap value
	 */
	public void setTupleMap(Map tupleMap) {
		try {
			configReader.setTupleMap(tupleMap);
		} catch (Exception e) {
			prtlnErr("setTupleMap error: " + e.getMessage());
		}
	}


	/**
	 *  Returns a sorted list of StatusFlag labels defined by this collection.
	 *
	 * @return    The statuses value
	 */
	public List getStatusLabels() {
		List labels = new ArrayList();
		Map statusMap = getStatusMap();
		if (statusMap != null) {
			for (Iterator i = statusMap.values().iterator(); i.hasNext(); ) {
				StatusFlag statusFlag = (StatusFlag) i.next();
				String label = statusFlag.getLabel();
				if (label.trim().length() > 0) {
					labels.add(label);
				}
			}
			Collections.sort(labels);
		}
		return labels;
	}


	/**
	 *  Get the value of the named tuple.
	 *
	 * @param  name  NOT YET DOCUMENTED
	 * @return       The tupleValue value
	 */
	public String getTupleValue(String name) {
		String value = (String) getTupleMap().get(name);
		return value == null ? "" : value;
	}


	/**
	 *  Adds tuple entry to the tupleMap.
	 *
	 * @param  name   The new tupleValue value
	 * @param  value  The new tupleValue value
	 */
	public void setTupleValue(String name, String value) {
		Map tMap = getTupleMap();
		tMap.put(name, value);
		try {
			configReader.setTupleMap(tMap);
		} catch (Exception e) {
			prtlnErr("setTupleValue error: " + e.getMessage());
		}
	}


	/**
	 *  Return the status flag label (the readable version) for the given status
	 *  flag value (which must be decoded if it is a final status flag).
	 *
	 * @param  statusFlagValue  NOT YET DOCUMENTED
	 * @return                  The statusFlagLabel value
	 */
	public String getStatusFlagLabel(String statusFlagValue) {

		return StatusFlags.isFinalStatusValue(statusFlagValue) ?
			this.getFinalStatusLabel() : statusFlagValue;
	}


	/**
	 *  Get sorted list of statusFlags defined in this collection for use in jsp,
	 *  where we need the StatusFlag object, not just the label.
	 *
	 * @return    The statusFlags value
	 */
	public List getStatusFlags() {
		List statusFlags = new ArrayList();
		Map statusMap = getStatusMap();
		if (statusMap != null) {
			List labels = getStatusLabels(); // sorted list of status_labels
			for (Iterator i = labels.iterator(); i.hasNext(); ) {
				String label = (String) i.next();
				if (label.trim().length() > 0) {
					StatusFlag statusFlag = (StatusFlag) statusMap.get(label);
					statusFlags.add(statusFlag);
				}
			}
		}
		statusFlags.add(StatusFlags.getFinalStatusFlag(getFinalStatusLabel(), getId()));
		return statusFlags;
	}


	/**
	 *  Return a list of {@link StatusFlag} beans that describe the statuses that
	 *  can be assigned to a record of this collection. The selectableStatusFlags
	 *  list is composed of the UNKNOWN_status plus those statuses that are defined
	 *  for the collection. The IMPORT status is not selectable, since it is only
	 *  assigned by the system.
	 *
	 * @return    The assignableStatusFlags value
	 */
	public List getAssignableStatusFlags() {
		List flags = new ArrayList();
		flags.add(StatusFlags.UNKNOWN_STATUS_FLAG);
		flags.add(StatusFlags.NEW_STATUS_FLAG);
		// we really should check for dups here ...
		flags.addAll(getStatusFlags());
		return flags;
	}


	/**
	 *  Returns a list of StatusFlags defined by this collection that can be
	 *  selected for searching. The selectable flags includes the assignable flags
	 *  plus the reserved IMPORTED_STATUS_FLAG.
	 *
	 * @return    The selectableStatusFlags value
	 */
	public List getSelectableStatusFlags() {
		List flags = new ArrayList();
		flags.add(StatusFlags.IMPORTED_STATUS_FLAG);
		flags.addAll(getAssignableStatusFlags());
		return flags;
	}


	/**
	 *  Gets the exportDirectory attribute of the CollectionConfig object
	 *
	 * @return    The exportDirectory value
	 */
	public String getExportDirectory() {
		return configReader.getExportDirectory();
	}


	/**
	 *  Sets the exportDirectory attribute of the CollectionConfig object
	 *
	 * @param  s  The new exportDirectory value
	 */
	public void setExportDirectory(String s) {
		configReader.setExportDirectory(s);
	}


	/**
	 *  Convenience method to retrieve the FinalStatusFlagValue for this collection
	 *  from StatusFlags.
	 *
	 * @return    The finalStatusValue value
	 */
	public String getFinalStatusValue() {
		return StatusFlags.getFinalStatusValue(getId());
	}


	/**
	 *  Gets the finalStatusLabel (display string) for this collection.
	 *
	 * @return    The finalStatusLabel value
	 */
	public String getFinalStatusLabel() {
		return configReader.getFinalStatusLabel();
	}


	/**
	 *  Gets the finalStatusFlag for this collection.
	 *
	 * @return    The finalStatusFlag value
	 */
	public StatusFlag getFinalStatusFlag() {
		return StatusFlags.getFinalStatusFlag(getFinalStatusLabel(), getId());
	}


	/**
	 *  Sets the finalStatusLabel (the display label) for this collection's final Status.
	 *
	 * @param  s  The new finalStatusLabel value
	 */
	public void setFinalStatusLabel(String s) {
		configReader.setFinalStatusLabel(s);
	}


	/**
	 *  Writes config file to disk and re-initializes data structures based on config. 
	 *
	 * @return    Description of the Return Value
	 */
	public boolean flush() {
		try {
			this.getConfigReader().flush();
			return true;
		} catch (Exception e) {
			prtlnErr("writeCongFile error: " + e.getMessage());
			return false;
		}
	}


	/** 
	* Causes all values to be re-read from config file on disk.
	*/
	public void refresh() {
		this.idGenerator = null;
		this.getConfigReader().refresh();
	}


	/**
	 *  Description of the Method
	 *
	 * @return    Description of the Return Value
	 */
	public String toString() {
		String ret = "";
		ret += "\nid: " + getId();
		ret += "\nidPrefix: " + getIdPrefix();
		ret += "\nexportDirectory: " + getExportDirectory();
		Map statusMap = getStatusMap();
		for (Iterator i = statusMap.keySet().iterator(); i.hasNext(); ) {
			String collection = (String) i.next();
			StatusFlag statusFlag = (StatusFlag) statusMap.get(collection);
			ret += "\n" + statusFlag.toString();
		}
		ret += "\n Statuses";
		for (Iterator i = getStatusLabels().iterator(); i.hasNext(); ) {
			ret += "\n\t" + (String) i.next();
		}
		ret += "\n SORTED status Flags";
		for (Iterator i = getStatusFlags().iterator(); i.hasNext(); ) {
			ret += "\n\t" + ((StatusFlag) i.next()).toString();
		}
		return ret;
	}


	/**  Description of the Method  */
	public void destroy() {
		prtln("destroying " + getId());
		try {
			configReader.destroy();
			this.idGenerator = null;
		} catch (Throwable e) {
			prtlnErr("destroy error: " + e.getMessage());
		}
	}


	/**
	 *  Description of the Method
	 *
	 * @param  s  Description of the Parameter
	 */
	private static void prtln(String s) {
		if (debug) {
			SchemEditUtils.prtln(s, "CollectionConfig");
		}
	}
	
	private static void prtlnErr(String s) {
		SchemEditUtils.prtln(s, "CollectionConfig");
	}
}

