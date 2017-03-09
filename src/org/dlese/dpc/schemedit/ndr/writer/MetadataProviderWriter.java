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
package org.dlese.dpc.schemedit.ndr.writer;

import org.dlese.dpc.xml.Dom4jUtils;
import org.dlese.dpc.schemedit.ndr.SyncReport;
import org.dlese.dpc.schemedit.ndr.ReportEntry;
import org.dlese.dpc.schemedit.ndr.util.ServiceDescription;
import org.dlese.dpc.schemedit.SchemEditUtils;
import org.dlese.dpc.schemedit.repository.RepositoryService;
import org.dlese.dpc.schemedit.config.CollectionConfig;

import org.dlese.dpc.schemedit.dcs.DcsDataManager;
import org.dlese.dpc.schemedit.dcs.DcsDataRecord;

import org.dlese.dpc.repository.RepositoryManager;
import org.dlese.dpc.index.reader.DleseCollectionDocReader;

import org.dlese.dpc.ndr.NdrUtils;
import org.dlese.dpc.ndr.apiproxy.InfoXML;
import org.dlese.dpc.ndr.apiproxy.NDRConstants;
import org.dlese.dpc.ndr.request.*;
import org.dlese.dpc.ndr.reader.MetadataProviderReader;

import javax.servlet.ServletContext;

import java.util.*;
import org.dom4j.*;
/**
 *  The MetadataProviderWriter syncs collection-level information between the
 *  NCS and the NDR. It updates (and creates, if necessary) both an aggregator
 *  and a metadataProvider object in the NDR, and then records the handles for
 *  these NDR objects in the collection-configuration record in the NCS. <p>
 *
 *  The MetadataProvider object will have the following data streams:
 *  <ul>
 *    <li> dcs_data - holding the contents of the collection's DcsDataRecord
 *    </li>
 *    <li> collection_config - holding the collections' configuration record
 *    </li>
 *    <li> dlese_collect - holding the contents of the collection record</li>
 *
 *  </ul>
 *  And the following properties:
 *  <ul>
 *    <li> dlese:nativeFormat - the format of the item level records for this
 *    collection</li>
 *    <li> dlese:collectionName</li>
 *    <li> dlese:collectionId - the collection key</li>
 *  </ul>
 *
 *
 * @author    Jonathan Ostwald
 */
public class MetadataProviderWriter {

	private static boolean debug = true;

	ServletContext servletContext;

	CollectionConfig collectionConfig;
	DcsDataRecord collDcsDataRecord;
	RepositoryManager rm;


	/**
	 *  Constructor for the MetadataProviderWriter object
	 *
	 * @param  servletContext  NOT YET DOCUMENTED
	 * @exception  Exception   NOT YET DOCUMENTED
	 */
	public MetadataProviderWriter(ServletContext servletContext) throws Exception {
		this.servletContext = servletContext;
	}


	/**
	 *  Constructor for the MetadataProviderWriter object
	 *
	 * @param  collectionRecordId  NOT YET DOCUMENTED
	 * @param  collectionConfig    NOT YET DOCUMENTED
	 * @param  collDcsDataRecord   NOT YET DOCUMENTED
	 * @return                     NOT YET DOCUMENTED
	 * @exception  Exception       NOT YET DOCUMENTED
	 */
	public SyncReport write(String collectionRecordId, CollectionConfig collectionConfig, DcsDataRecord collDcsDataRecord) throws Exception {
		this.collDcsDataRecord = collDcsDataRecord;
		this.rm = (RepositoryManager) getServletContextAttribute("repositoryManager");
		this.collectionConfig = collectionConfig;

		prtln("\nwrite() collectionRecordId: " + collectionRecordId);

		DleseCollectionDocReader docReader =
			(DleseCollectionDocReader) RepositoryService.getXMLDocReader(collectionRecordId, rm);
		if (docReader == null)
			throw new Exception("collectionDocReader not found in index");

		String collectionName = docReader.getShortTitle();
		SyncReport report = new SyncReport(this.collectionConfig, collectionName);

		String collection = collectionConfig.getId();

		String aggHandle = null;
		try {
			aggHandle = getAggregatorHandle(docReader, report);
		} catch (Exception e) {
			throw new Exception("aggregator could not be found or created: " + e);
		}

		/* Initialize the MDP add/modify request with information that has to be written
			each time, since it depends on editable info in the collection config and
			collection record
		*/
		NdrRequest request = new SignedNdrRequest();
		request.setObjectType(NDRConstants.NDRObjectType.METADATAPROVIDER);

		// properties
		request.addNcsPropertyCmd("collectionName", collectionName);

		/* CollectionID
		(this is an unfortunately-named property, since it is really the KEY)
		we write the collectionID each time to ensure that it is present when the
		mdpHandle is changed (e.g., when a ncs collection is ingested into the NDR)
		*/
		request.addNcsPropertyCmd("collectionId", collection);

		// relationships
		request.addCommand("relationship", "aggregatedBy", aggHandle);

		// datastreams
		request.addDataStreamCmd("dcs_data", getDcsDataStream());
		request.addDataStreamCmd("collection_config", getCollectionConfigStream());
		request.addDataStreamCmd("dlese_collect", getCollectionRecordStream(docReader));
		request.addNcsPropertyCmd("nativeFormat", collectionConfig.getXmlFormat());

		String metadataProviderHandle = collectionConfig.getMetadataProviderHandle();
		String cmd = null;
		String setSpec = "ncs-" + collection;
		if (metadataProviderHandle == null || metadataProviderHandle.trim().length() == 0) {
			cmd = "add";

			/*  create a Collection MDP -
				the following are written ONLY at MDP creation time - they don't
				change over the life of the collection, or if the collection is
				later associated with a NDR Collection (in which case the MDP and
				AGG are managed by the NCS Collect record
			*/
			request.setVerb("addMetadataProvider");
			// NDR / OAI properties: setSpec and setName
			request.addCommand("property", "setSpec", setSpec);
			request.addCommand("property", "setName", collectionName);

			// DO WE CARE ABOUT THE AGENT? (DOES IT HURT TO KEEP IN AGENT CODE?)
			String agentHandle = collectionConfig.getAgentHandle();
			if (agentHandle == null || agentHandle.trim().length() == 0)
				agentHandle = NDRConstants.getNcsAgent();
			request.addCommand("relationship", "metadataProviderFor", agentHandle);

			/*	this is only a place-holder service description, it has no meaning to the NDR
				and will be overwritten by NCS Collection Manager if this collection becomes an
				NDR Collection.
			*/
			ServiceDescription sd =
				ServiceDescription.makeServiceDescription(docReader, NDRConstants.NDRObjectType.METADATAPROVIDER);
			request.addServiceDescriptionCmd(sd.asElement());
		}
		else {
			cmd = "modify";
			request.setVerb("modifyMetadataProvider");
			request.setHandle(metadataProviderHandle);
		}

		InfoXML response = request.submit();
		ReportEntry reportEntry = new ReportEntry("MetadataProvider", cmd, response);

		if (reportEntry.isError()) {
			throw new Exception("MetadataProvider not written: " + reportEntry.getErrorMsg());
		}
		else
			prtln ("MetadataProvider object written with \"" + cmd + "\" command");

		//update collection DcsDataRecord
		metadataProviderHandle = reportEntry.getHandle();
		collDcsDataRecord.setNdrHandle(metadataProviderHandle); // MDP handle in DcsDataRecord?????
		this.collDcsDataRecord.setLastSyncDate(new Date());
		this.collDcsDataRecord.setSetSpec(setSpec);
		this.collDcsDataRecord.flushToDisk();

		//update collection configuration
		this.collectionConfig.setAuthority("ndr");
		collectionConfig.setMetadataProviderHandle(metadataProviderHandle);
		collectionConfig.flush();
		report.addEntry(reportEntry);

		// prtln("returning from write()");
		// prtln("report has " + report.getEntries().size() + " entries");

		return report;
	}


	/**
	 *  Gets the dcsDataStream attribute of the MetadataProviderWriter object
	 *
	 * @return                The dcsDataStream value
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	private Element getDcsDataStream() throws Exception {
		try {
			Document doc = Dom4jUtils.localizeXml(collDcsDataRecord.getDocument());
			return doc.getRootElement().createCopy();
		} catch (Exception e) {
			throw new Exception("could not obtain dcsDataStream: " + e.getMessage());
		}
	}


	/**
	 *  Get the Collection Aggregator Object from the NDR, creating one if
	 *  necessary.<p>
	 *
	 *  Updates the collection configuration, and returns the collection
	 *  aggregator's handle.<p>
	 *
	 *  Aggregator components:
	 *  <ul>
	 *    <li> datastream - Aggregator Service Description - created from
	 *    ncs_collect record</li>
	 *    <li> relationships:
	 *    <ul>
	 *      <li> assocatedWith: resourceHandle
	 *      <li> aggregatorFor: NCS Agent (obtained from servletContext)
	 *    </ul>
	 *
	 *  </ul>
	 *  <P>
	 *
	 *
	 *
	 * @param  docReader      NOT YET DOCUMENTED
	 * @param  report         NOT YET DOCUMENTED
	 * @return                The resourceHandle value
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	private String getAggregatorHandle(DleseCollectionDocReader docReader,
	                                   SyncReport report) throws Exception {
		prtln("getAggregatorHandle ()");

		/*
			We must also test to see if this metadataProvider has a
			"aggregatedBy" relationship!
			when a collection is defined by NSDLCollectionWriter, the
			aggregator IS defined, but if the MDP value is simply plugged into
			the collection config, it won't be known by the simple
			collectionConfig.getAggregatorHandle() test ...
		*/
		// normally, aggHandle is stored in collectionConfig
		String aggHandle = collectionConfig.getAggregatorHandle();

		/*
			if mdpHandle has been manually entered in the collectionConfig (see above)
			it may not yet be known to collectionConfig ...
		*/
		if (aggHandle == null || aggHandle.trim().length() == 0) {
			String mdpHandle = collectionConfig.getMetadataProviderHandle();
			if (mdpHandle != null && mdpHandle.trim().length() > 0) {
				try {
					MetadataProviderReader mdp = new MetadataProviderReader(mdpHandle);
					aggHandle = mdp.getAggregatedBy();
					this.collectionConfig.setAggregatorHandle(aggHandle);
				} catch (Throwable t) {
					prtln("Trouble finding aggHandle for mdp: " + mdpHandle);
				}
			}
		}

		/*
			if the aggHandle is still not found, we create one.
			ISSUE: should we update the aggregator Object when it already exists?
			 - working decision: NO - since the agg info provided by the ncs_collect
			   is authoritative, we let the collection management side control this via
			   the collection record (ncs_collect).
		*/
		if (aggHandle == null || aggHandle.trim().length() == 0) {

			AddAggregatorRequest request = new AddAggregatorRequest();

			request.setAggregatorFor(NDRConstants.getNcsAgent());

			// create an aggregator ServiceDescription
			ServiceDescription sd =
				ServiceDescription.makeServiceDescription(docReader,
				NDRConstants.NDRObjectType.AGGREGATOR);

			Element sdElement = sd.asElement();
			request.addServiceDescriptionCmd(sdElement);

			/* request.addServiceDescriptionCmd(getServiceDescription(collectionName, "aggregator")); */
			InfoXML response = request.submit();
			ReportEntry reportEntry = new ReportEntry("Aggregator", "add", response);
			if (reportEntry.isError())
				throw new Exception(reportEntry.getErrorMsg());

			aggHandle = response.getHandle();
			this.collectionConfig.setAggregatorHandle(aggHandle);
			report.addEntry(reportEntry);
		}

		return aggHandle;
	}


	/**
	 *  Convenience method to obtain a servletContext attribute.
	 *
	 * @param  att            name of attribute to obtain
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
	 *  Gets the collectionRecordStream attribute of the MetadataProviderWriter
	 *  object
	 *
	 * @param  docReader  NOT YET DOCUMENTED
	 * @return            The collectionRecordStream value
	 */
	private Element getCollectionRecordStream(DleseCollectionDocReader docReader) {
		return docReader.getXmlDoc().getRootElement().createCopy();
	}


	/**
	 *  Returns a copy of the collectionConfig document's root element.
	 *
	 * @return    The collectionConfigStream value
	 */
	Element getCollectionConfigStream() {
		return collectionConfig.getConfigReader().getDocument().getRootElement().createCopy();
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
			SchemEditUtils.prtln(s, "MetadataProviderWriter");
	}

}

