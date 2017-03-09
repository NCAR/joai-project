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

import org.dlese.dpc.schemedit.dcs.*;
import org.dlese.dpc.schemedit.config.CollectionConfig;

import org.dlese.dpc.repository.RepositoryManager;
import org.dlese.dpc.schemedit.MetaDataFramework;
import org.dlese.dpc.schemedit.FrameworkRegistry;
import org.dlese.dpc.schemedit.SchemEditUtils;

import org.dlese.dpc.xml.schema.*;
import org.dlese.dpc.xml.*;
import org.dlese.dpc.index.*;
import org.dlese.dpc.index.reader.*;

import org.dlese.dpc.util.*;
import org.dlese.dpc.repository.*;

import java.util.*;
import java.text.*;

import javax.servlet.ServletContext;

import org.dom4j.Document;

/**
 *  The class includes methods to create, copy, and put Records to the main Repository, and
 *  RepositoryWriterPlugins if so configured.
 *
 * @author    ostwald <p>
 *
 *
 */
public class RepositoryWriter {

	private static boolean debug = false;
	private ServletContext servletContext = null;
	private FrameworkRegistry frameworkRegistry = null;

	private List plugins = null;


	/**
	 *  Constructor for the RepositoryWriter object
	 *
	 * @param  servletContext  NOT YET DOCUMENTED
	 */
	protected RepositoryWriter(ServletContext servletContext) {
		this.servletContext = servletContext;
	}


	/**
	 *  Register a RepositoryWriterPlugin.
	 *
	 * @param  plugin  the RepositoryWriterPlugin
	 */
	public void addPlugin(RepositoryWriterPlugin plugin) {
		if (this.plugins == null) {
			this.plugins = new ArrayList();
		}
		if (!this.plugins.contains(plugin))
			this.plugins.add(plugin);
	}


	/**
	 *  Unregister a RepositoryWriterPlugin.
	 *
	 * @param  plugin  the RepositoryWriterPlugin
	 */
	public void removePlugin(RepositoryWriterPlugin plugin) {
		if (this.plugins != null) {
			this.plugins.remove(plugin);
		}
	}


	/**
	 *  Gets the registered RepositoryWriterPlugins
	 *
	 * @return    The plugins value
	 */
	public List getPlugins() {
		if (this.plugins == null)
			this.plugins = new ArrayList();
		return this.plugins;
	}



	/**
	 *  Convenience method for RepositoryServices calls that have the docReader
	 *  handy, calls writeRecord after computing required params.
	 *
	 * @param  recId                      metadata record id
	 * @param  recordXml                  metadata record as xml string
	 * @param  docReader                  docReader for metadata
	 * @param  dcsDataRecord              dcsDataRecord for metadata
	 * @exception  RecordUpdateException  if record cannot be written to index
	 * @exception  Exception              NOT YET DOCUMENTED
	 */
	protected void writeRecord(String recId,
	                           String recordXml,
	                           XMLDocReader docReader,
	                           DcsDataRecord dcsDataRecord) throws RecordUpdateException, Exception {
		String xmlFormat = docReader.getNativeFormat();
		String collection = docReader.getCollection();

		this.writeRecord(recordXml, xmlFormat, collection, recId, dcsDataRecord);

	}


	/**
	 *  Writes provided metadata to the index and to other registered repositories,
	 *  such as the NDR.<p>
	 *
	 *  First try to write to the external repository. This attempt may fail before
	 *  the record has been indexed the first time, if a RepositoryWriterPlugin
	 *  required the index (as the NDR plugin does). In this case, a syncError is
	 *  registered in the index.
	 *
	 * @param  recordXml                  metadata record as xml string
	 * @param  xmlFormat                  format of metadata
	 * @param  collection                 collection key of metadata record (e.g.,
	 *      "dcc")
	 * @param  id                         metadata record id
	 * @param  dcsDataRecord              dcsDataRecord for metadata
	 * @exception  RecordUpdateException  if record cannot be written to index
	 * @exception  Exception              NOT YET DOCUMENTED
	 */
	protected void writeRecord(
	                           String recordXml,
	                           String xmlFormat,
	                           String collection,
	                           String id,
	                           DcsDataRecord dcsDataRecord)
		 throws RecordUpdateException, Exception {

		prtln("\nwriteRecord() id: " + id);

		RepositoryManager rm = getRepositoryManager();
		if (rm == null)
			throw new Exception("RepositoryManager not found");

		List plugins = this.getPlugins();
		if (!plugins.isEmpty()) {

			// if the record to be written is not known to the repository, then the call
			// to plugin will fail, so we must first write to the repository ...
			if (this.getXMLDocReader(id) == null) {
				prtln("docReader not found for " + id + " indexing now ..");
				rm.putRecord(recordXml, xmlFormat, collection, id, true);
			}

			dcsDataRecord.clearSyncErrors();
			for (Iterator i = plugins.iterator(); i.hasNext(); ) {
				prtln("calling plugin.putRecord");
				RepositoryWriterPlugin plugin = (RepositoryWriterPlugin) i.next();
				try {
					// results of plugin.putRecord are inserted by plugin into dcsDataRecord
					// e.g., in case of ndrPlugin, the ndrHandle is inserted in dcsDataRecord
					plugin.putRecord(id, recordXml, xmlFormat, dcsDataRecord);
				} catch (RepositoryWriterPluginException e) {
					// prtln("\nWARNING: " + e.getPluginName() + ": " + e.getMessage());

					prtln("------\n" + e + "\n-------");

					// e.printStackTrace();
					dcsDataRecord.setNdrSyncError(e.getSyncMessage());
				}
			}
		}

		// make the write to the index
		try {
			rm.putRecord(recordXml, xmlFormat, collection, id, true);
		} catch (RecordUpdateException e) {
			prtlnErr("\nRecordUpdateException: " + e.getMessage());
			if (plugins != null) {
				// if we weren't able to update the index, then don't attempt to update metadata in NDR
				try {
					// WHY WHOULD WE DELETE THE RECORD IN THE NDR??
					// plugin.deleteRecord(id, dcsDataRecord);
				} catch (Throwable t) {}
			}
			prtln(" ... throwing " + e.getClass().getName());
			throw e;
		}

	}


	/**
	 *  Delete a record from the repository.<p>
	 *
	 *  Current plugin exception policy: If there is a pluginError, the record is
	 *  NOT deleted, but instead it is re-indexed as a sync-error.
	 *
	 * @param  recId          NOT YET DOCUMENTED
	 * @param  dcsDataRecord  NOT YET DOCUMENTED
	 * @return                NOT YET DOCUMENTED
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	protected boolean deleteRecord(String recId, DcsDataRecord dcsDataRecord) throws Exception {
		XMLDocReader docReader = this.getXMLDocReader(recId);
		/*
			We call putRecord to index failure.
			TODO - Keep track of if ANY plug failed, and if so, write record
			  (plugins are responsible for inserting appropriate error into DcsDataRecord)
		*/
		if (!this.getPlugins().isEmpty()) {
			dcsDataRecord.clearSyncErrors();
			for (Iterator i = this.getPlugins().iterator(); i.hasNext(); ) {

				RepositoryWriterPlugin plugin = (RepositoryWriterPlugin) i.next();
				try {
					plugin.deleteRecord(recId, dcsDataRecord);
				} catch (RepositoryWriterPluginException e) {
					e.printStackTrace();
					dcsDataRecord.setNdrSyncError(e.getSyncMessage());
				}
			}

			if (dcsDataRecord.hasSyncError()) {
				/*
					ISSUE: what is appropriate action if there was a plugin error?
					FOR NOW - do not delete from index: save record to index failure and then throw
					exception
				*/
				getRepositoryManager().putRecord(docReader.getXml(),
					docReader.getNativeFormat(),
					docReader.getCollection(),
					recId,
					true);

				throw new Exception("There were plugin errors - record " + recId + " not deleted");
			}
		}

		return getRepositoryManager().deleteRecord(recId);
	}


	/**
	 *  Delete a collection from the local repository. Called by {@link
	 *  org.dlese.dpc.schemedit.repository.RepositoryService}
	 *
	 * @param  config         NOT YET DOCUMENTED
	 * @return                NOT YET DOCUMENTED
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	protected boolean deleteCollection(CollectionConfig config) throws Exception {
		// Extract needed info from config before blowing it away ...
		String collection = config.getId();
		CollectionReaper reaper = new CollectionReaper(collection, servletContext);
		reaper.reap();
		return true;
	}


	/**
	 *  Gets the repositoryManager attribute of the RepositoryWriter object
	 *
	 * @return                The repositoryManager value
	 * @exception  Exception  If the repositoryManager is not found in the
	 *      ServletContext
	 */
	private RepositoryManager getRepositoryManager() throws Exception {
		return (RepositoryManager) getContextAttributeValue("repositoryManager");
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	protected void init() throws Exception {
		prtln("init: RepositoryWriter.init()");

		if (this.servletContext == null)
			prtln("SERVLET CONTEXT is NULL");

		// we should test these to make sure they aren't null
		// and do something reasonable if they are
		try {

			frameworkRegistry =
				(FrameworkRegistry) getContextAttributeValue("frameworkRegistry");

			/* 			collectionRegistry =
				(CollectionRegistry) getContextAttributeValue("collectionRegistry"); */
			/* 			dcsDataManager =
				(DcsDataManager) getContextAttributeValue("dcsDataManager"); */
		} catch (Throwable t) {
			prtln("RepositoryWriter init error: " + t);
			t.printStackTrace();
			throw new Exception(t);
		}

		prtln("RepositoryWriter initialized");
	}


	/**
	 *  Gets the contextAttributeValue attribute of the RepositoryWriter object
	 *
	 * @param  attrName       NOT YET DOCUMENTED
	 * @return                The contextAttributeValue value
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	private Object getContextAttributeValue(String attrName) throws Exception {
		Object attrValue = servletContext.getAttribute(attrName);
		if (attrValue == null)
			throw new Exception("servletContext attribute \"" + attrName + "\" not found");
		return attrValue;
	}


	/**
	 *  Gets the metaDataFramework attribute of the RepositoryWriter object
	 *
	 * @param  xmlFormat  NOT YET DOCUMENTED
	 * @return            The metaDataFramework value
	 */
	private MetaDataFramework getMetaDataFramework(String xmlFormat) {
		return frameworkRegistry.getFramework(xmlFormat);
	}


	/**
	 *  Gets the XMLDocReader associated with an id
	 *
	 * @param  id  Record ID
	 * @return     XMLDocReader or null if not found
	 */
	private XMLDocReader getXMLDocReader(String id) {
		RepositoryManager repositoryManager = null;
		try {
			repositoryManager = getRepositoryManager();
		} catch (Exception e) {
			prtln(e.getMessage());
			return null;
		}
		ResultDoc record = repositoryManager.getRecord(id);
		if (record == null) {
			prtln("indexed record not found for id: " + id);
			return null;
		}
		try {
			return (XMLDocReader) record.getDocReader();
		} catch (Exception e) {
			prtln("getDcsDataRecord found unexpected docReader class " + e.getMessage());
			return null;
		}
	}


	/**
	 *  NOT CURRENTLY USED
	 *
	 * @return                            The dateString value
	 */
	/* 	protected void writeCollectionData (CollectionConfig collectionConfig, DcsDataRecord collDcsDataRecord) {
		collDcsDataRecord.clearSyncErrors();
		RepositoryWriterPlugin plugin = getRepositoryWriterPlugin (collectionConfig.getId());
		if (plugin != null) {
			try {
				plugin.putCollectionData (collectionConfig, collDcsDataRecord);
			} catch (RepositoryWriterPluginException e) {
				collDcsDataRecord.setNdrSyncError(e.getSyncMessage());
				collDcsDataRecord.flushToDisk();
			}
		}
	} */
	/**
	 *  Gets the dateString attribute of the RepositoryWriter class
	 *
	 * @return    The dateString value
	 */
	private static String getDateString() {
		return SchemEditUtils.fullDateString(new Date());
	}


	/**
	 *  Print a line to standard out.
	 *
	 * @param  s  The String to print.
	 */
	private static void prtln(String s) {
		if (debug) {
			SchemEditUtils.prtln(s, "RepositoryWriter");
		}
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  s  NOT YET DOCUMENTED
	 */
	private static void prtlnErr(String s) {
		SchemEditUtils.prtln(s, "RepositoryWriter");
	}
}

