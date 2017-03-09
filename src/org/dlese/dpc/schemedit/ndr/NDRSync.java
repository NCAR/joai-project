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

import java.util.*;
import org.dom4j.*;

import org.dlese.dpc.xml.Dom4jUtils;
import org.dlese.dpc.schemedit.SchemEditUtils;
import org.dlese.dpc.schemedit.MetaDataFramework;
import org.dlese.dpc.schemedit.FrameworkRegistry;
import org.dlese.dpc.schemedit.config.CollectionRegistry;
import org.dlese.dpc.schemedit.config.CollectionConfig;
import org.dlese.dpc.schemedit.dcs.DcsSetInfo;
import org.dlese.dpc.schemedit.dcs.DcsDataManager;
import org.dlese.dpc.schemedit.dcs.DcsDataRecord;
import org.dlese.dpc.schemedit.ndr.writer.MetadataWriter;
import org.dlese.dpc.schemedit.ndr.writer.NSDLCollectionWriter;
import org.dlese.dpc.schemedit.ndr.writer.MetadataProviderWriter;
import org.dlese.dpc.ndr.reader.MetadataProviderReader;
import org.dlese.dpc.ndr.NdrUtils;
import org.dlese.dpc.repository.RepositoryManager;
import org.dlese.dpc.index.ResultDoc;
import org.dlese.dpc.index.ResultDocList;
import org.dlese.dpc.index.reader.XMLDocReader;

import javax.servlet.ServletContext;

/**
 *  NOW Obsolete??
 *
 * @author     Jonathan Ostwald
 */
public class NDRSync {

	private static boolean debug = true;

	DocumentFactory df = DocumentFactory.getInstance();
	ServletContext servletContext;
	String collectionKey;
	String sessionUserName;
	CollectionConfig collectionConfig;
	DcsDataManager dcsDataManager;
	RepositoryManager rm;
	String urlPath = null;
	MetaDataFramework itemFramework = null;

	String aggregatorHandle = null;
	String metadataProviderHandle = null;

	SyncReport syncReport = null;

	boolean deleteUnmatchedNDRRecords = false; // when true, removes unmatched records from the NDR


	/**
	 *  Constructor for the NDRSync object
	 *
	 * @param  collectionKey    NOT YET DOCUMENTED
	 * @param  sessionUserName  NOT YET DOCUMENTED
	 * @param  servletContext   NOT YET DOCUMENTED
	 * @exception  Exception    NOT YET DOCUMENTED
	 */
	public NDRSync(String collectionKey, String sessionUserName, ServletContext servletContext) throws Exception {
		this.servletContext = servletContext;
		this.collectionKey = collectionKey;
		this.sessionUserName = sessionUserName;
		try {
			rm = (RepositoryManager) getServletContextAttribute("repositoryManager");
			dcsDataManager = (DcsDataManager) getServletContextAttribute("dcsDataManager");
			collectionConfig = getCollectionConfig(collectionKey);
			FrameworkRegistry frameworkRegistry =
				(FrameworkRegistry) getServletContextAttribute("frameworkRegistry");
			itemFramework = frameworkRegistry.getFramework(collectionConfig.getXmlFormat());
			if (itemFramework == null) {
				throw new Exception("collection cannot be synced because \"" +
					collectionConfig.getXmlFormat() + "\" is not loaded");
			}
			urlPath = itemFramework.getUrlPath();
			if (urlPath == null) {
				String msg = "item framework (" + itemFramework.getName() + ") ";
				msg += "does not define a URL path and therefore cannot be synced wtih the NDR";
				throw new Exception(msg);
			}
		} catch (Throwable e) {
			e.printStackTrace();
			throw new Exception(e.getMessage());
		}
	}


	/**
	 *  A record is out of sync if the metadata has been changed, or the status has
	 *  been touched since it was last written (synced) to the NDR.
	 *
	 * @param  dcsDataRecord  NOT YET DOCUMENTED
	 * @return                NOT YET DOCUMENTED
	 */
	private boolean recordOutOfSync(DcsDataRecord dcsDataRecord) {
		Date thresholdDate = dcsDataRecord.getLastSyncDateDate();
		Date changeDate = dcsDataRecord.getChangeDateDate();
		Date lastTouchDate = dcsDataRecord.getLastTouchDateDate();
		prtln("\nlastSyncDate: " + SchemEditUtils.fullDateString(thresholdDate));
		prtln("\t changeDate: " + SchemEditUtils.fullDateString(changeDate));
		prtln("\t lastTouch: " + SchemEditUtils.fullDateString(lastTouchDate));

		// HERE we decide whether this record has changed or not ...
		return (changeDate.after(thresholdDate) || lastTouchDate.after(thresholdDate));
	}


	/**
	 *  Write collection-level info (metadataProvider and Aggregator) for the
	 *  specified collection (set) to the NDR.
	 *
	 * @param  setInfo        Data structure holding collection information
	 * @return                NOT YET DOCUMENTED
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public SyncReport writeCollectionInfo(DcsSetInfo setInfo) throws Exception {
		DcsDataRecord dcsDataRecord = dcsDataManager.getDcsDataRecord(setInfo.getId(), rm);
		prtln("\nwriteCollectionInfo() setInfo.getId: " + setInfo.getId());
		MetadataProviderWriter writer = new MetadataProviderWriter(servletContext);
		return writer.write(setInfo.getId(), collectionConfig, dcsDataRecord);
	}


	/**
	 *  Gets the metadataWriter attribute of the NDRSync object
	 *
	 * @return                The metadataWriter value
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	private MetadataWriter getMetadataWriter() throws Exception {
		if (this.getXmlFormat().equals("ncs_collect")) {
			return new NSDLCollectionWriter(this.servletContext);
		}
		else {
			return new MetadataWriter(this.servletContext);
		}
	}


	/**
	 *  Sync a collection with the NDR by first updating collection-level
	 *  (metadataProvider, aggregator) information in the NDR and then updating
	 *  outOfSync metadata with the NDR.
	 *
	 * @return                NOT YET DOCUMENTED
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public SyncReport sync() throws Exception {
		prtln("\n======  SYNC() ===============");

		DcsSetInfo setInfo = this.collectionConfig.getSetInfo(rm);

		syncReport = writeCollectionInfo(setInfo);

		syncItems(syncReport, false);

		return syncReport;
	}

	public void syncItems() throws Exception {
		SyncReport report = new SyncReport(this.collectionConfig, this.getCollectionName());
		syncItems (report, true);
	}

	/**
	 *  Sync the item-level records with the NDR.<p>
	 *
	 *  First, write all the indexed records that are out of sync to the NDR. Then,
	 *  (optionally - depending on the deleteUnmatchedNDRRecords attribute) delete
	 *  all the objects in the NDR associated with this collection that are NOT
	 *  indexed.
	 *
	 * @param  syncReport  NOT YET DOCUMENTED
	 */
	private void syncItems(SyncReport syncReport, boolean syncAll) throws Exception {

		MetadataWriter metadataWriter = null;
		try {
			metadataWriter = getMetadataWriter();
		} catch (Exception e) {
			throw new Exception("Unable to initialize metadata writer: " + e.getMessage());
		}

		List idList = getIdList(); // this collection's items from index (local)
		List indexedHandles = new ArrayList();

		for (Iterator i = idList.iterator(); i.hasNext(); ) {
			String id = (String) i.next();
			prtln("\n" + id);
			DcsDataRecord dcsDataRecord = dcsDataManager.getDcsDataRecord(id, rm);
			indexedHandles.add(dcsDataRecord.getNdrHandle());
			// HERE we decide whether this record has changed or not ...
			if (recordOutOfSync(dcsDataRecord) || syncAll) {
				prtln("\t ... syncing");
				try {
					dcsDataRecord.clearSyncErrors();
					SyncReportEntry entry = metadataWriter.write(id, dcsDataRecord);
					this.syncReport.addEntry(entry);
					if (entry.getCommand().startsWith("add"))
						indexedHandles.add(entry.getMetadataHandle());
				} catch (Exception e) {
					syncReport.addEntry(new SyncReportEntry(id, e.getMessage()));
					dcsDataRecord.setNdrSyncError(e.getMessage());
					dcsDataRecord.flushToDisk();
				}
			}
			else {
				prtln("\t ... NOT syncing");
			}
		}

		// DELETE records in NDR that are NOT in NCS

		if (deleteUnmatchedNDRRecords) {
			List ndrHandles = getNdrHandles();
			for (Iterator i = ndrHandles.iterator(); i.hasNext(); ) {
				String handle = (String) i.next();
				if (!indexedHandles.contains(handle)) {
					prtln("about to delete " + handle + " from NDR");
					try {
						syncReport.addEntry(new SyncReportEntry(handle, "delete", "", NdrUtils.deleteNDRObject(handle)));
					} catch (Exception e) {
						syncReport.addEntry(new SyncReportEntry(handle, e.getMessage()));
					}
				}
			}
		}
	}


	/**
	 *  Gets the handles of Metadata Objects in the NDR for this collection.
	 *
	 * @return    The ndrHandles value
	 */
	private List getNdrHandles() {
		try {
			String mdpHandle = collectionConfig.getMetadataProviderHandle();
			MetadataProviderReader mdpReader = new MetadataProviderReader(mdpHandle);
			return mdpReader.getItemHandles();
		} catch (Throwable t) {
			prtln("getNdrHandles error: " + t);
		}
		return new ArrayList();
	}


	/**
	 *  Return a query string that will find records for the specified collection.
	 *
	 * @param  collection  NOT YET DOCUMENTED
	 * @param  statuses    NOT YET DOCUMENTED
	 * @return             NOT YET DOCUMENTED
	 */
	private String buildQuery(String collection, String[] statuses) {
		String query = "(collection:0*) AND collection:0" + collection;

		/* handle status selection options */
		if (statuses != null &&
			statuses.length > 0) {
			query = "(" + query + ") AND (dcsstatus:" + SchemEditUtils.quoteWrap(statuses[0]);
			for (int i = 1; i < statuses.length; i++) {
				query += " OR dcsstatus:" + SchemEditUtils.quoteWrap(statuses[i]);
			}
			query += ")";
		}

		prtln("query: " + query);
		return query;
	}


	/**
	 *  Obtain list of itemRecord ids from the index for collection to be exported.
	 *
	 * @return    A list of record Ids
	 */
	private List getIdList() {
		return getIdList(null);
	}


	/**
	 *  Obtain list of itemRecord ids for records having specified status from the
	 *  index for collection to be exported.
	 *
	 * @param  statuses  Statuses that will be searched over
	 * @return           A list of record Ids
	 */
	private List getIdList(String[] statuses) {
		List idList = new ArrayList();
		String query = buildQuery(collectionKey, statuses);
		ResultDocList results = rm.getIndex().searchDocs(query);
		if (results != null && results.size() > 0) {
			for (int i = 0; i < results.size(); i++) {
				XMLDocReader docReader = (XMLDocReader) ((ResultDoc)results.get(i)).getDocReader();
				idList.add(docReader.getId());
			}
		}
		prtln("getIdList returning " + idList.size() + " items");
		return idList;
	}


	/**
	 *  Gets the collectionConfig instance for the collection to be exported. Also
	 *  ensures that the collection config's "authority" field is set to "ndr".
	 *
	 * @param  collectionKey  NOT YET DOCUMENTED
	 * @return                CollectionConfig instance
	 * @exception  Exception  If collectionConfig or setInfo cannot be found for
	 *      collectionKey
	 */
	private CollectionConfig getCollectionConfig(String collectionKey) throws Exception {
		CollectionRegistry collectionRegistry = (CollectionRegistry) getServletContextAttribute("collectionRegistry");
		// do NOT create new collection info if it doesn't already exist
		CollectionConfig config = collectionRegistry.getCollectionConfig(collectionKey, false);
		if (config == null)
			throw new Exception("collection not found for \"" + collectionKey + "\"");

		// ensure that the repository manager knows about this collection
		if (config.getSetInfo(rm) == null)
			throw new Exception("set config not found for  for \"" + collectionKey + "\"");
		config.setAuthority("ndr");
		return config;
	}


	/**
	 *  Convenience method to obtain a servletContext attribute.
	 *
	 * @param  att            attribute name
	 * @return                named object from ServletContext
	 * @exception  Exception  if not found in ServletContext
	 */
	private Object getServletContextAttribute(String att) throws Exception {
		Object o = servletContext.getAttribute(att);
		if (o == null)
			throw new Exception(att + " not found in servletContext");
		return o;
	}


	/**
	 *  Gets the xmlFormat attribute of the collection to be exported.
	 *
	 * @return    The xmlFormat value
	 */
	String getXmlFormat() {
		return this.collectionConfig.getXmlFormat();
	}


	/**
	 *  Gets the name (i.e., "short title") attribute of the collection to be
	 *  exported.
	 *
	 * @return    The collectionName value
	 */
	String getCollectionName() {
		return this.collectionConfig.getSetInfo(rm).getName();
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
			SchemEditUtils.prtln(s, "NDRSync");
	}
}

