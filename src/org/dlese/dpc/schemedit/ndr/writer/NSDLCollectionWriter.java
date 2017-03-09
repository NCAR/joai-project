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

import org.dlese.dpc.schemedit.ndr.*;
import org.dlese.dpc.schemedit.ndr.util.ServiceDescription;
import org.dlese.dpc.schemedit.ndr.util.NsdlDcWriter;
import org.dlese.dpc.schemedit.ndr.util.NCSCollectReader;
import org.dlese.dpc.schemedit.SchemEditUtils;
import org.dlese.dpc.schemedit.MetaDataFramework;
import org.dlese.dpc.schemedit.dcs.DcsDataRecord;

import org.dlese.dpc.ndr.NdrUtils;
import org.dlese.dpc.ndr.apiproxy.InfoXML;
import org.dlese.dpc.ndr.apiproxy.NDRConstants;
import org.dlese.dpc.ndr.request.*;
import org.dlese.dpc.ndr.reader.*;

import org.dom4j.*;

import java.util.*;
import java.net.*;

import javax.servlet.ServletContext;

/**
 *  Creates or updates the NDR Objects necessary to create a NSDL collection in
 *  the NDR, namely the collection Metadata Record, Collection Resource,
 *  Collection Aggregator, and Collection MetadataProvider.<p>
 *
 *  NOTE: A NSDL collection is different from the representation of a DLESE/NCS
 *  collection in the NDR. A NCS collection consists of only a MetadataProvider,
 *  Aggregator and item-level Metadata objects. If we are creating an NSDL
 *  collection for an EXISTING NCS collection we need to do some additional
 *  operations, which are NOT YET IMPLEMENTED:
 *  <ul>
 *    <li> connect the existing MetadataProvider object to the newly created
 *    Collection Aggregator</li>
 *    <li> add a "memberOf" relation to each resource object that points to the
 *    Collection Aggregator.</li>
 *  </ul>
 *
 *
 * @author    Jonathan Ostwald
 */
public class NSDLCollectionWriter extends MetadataWriter {

	private static boolean debug = true;

	private String collectionMetadata = null;
	private String collectionResource = null;
	private String collectionAggregator = null;
	private String collectionMetadataProvider = null;
	private String collectionAgent = null;
	private String ingestAppAgent = null; // an agent that is authorized to write metadata
	private String setSpec = null;
	private MetadataProviderReader mdpReader = null;
	private NCSCollectReader ncsCollectReader = null;


	/**
	 *  Constructor for the NSDLCollectionWriter object
	 *
	 * @param  servletContext  NOT YET DOCUMENTED
	 * @exception  Exception   NOT YET DOCUMENTED
	 */
	public NSDLCollectionWriter(ServletContext servletContext) throws Exception {
		super(servletContext);
		prtln("NSDLCollectionWriter instantiated");
	}


	/**
	 *  Convenience write method that doesn't require ncs_collect metadata to be
	 *  supplied (this will be obtained from index in the callee).
	 *
	 * @param  recId          NOT YET DOCUMENTED
	 * @param  dcsDataRecord  NOT YET DOCUMENTED
	 * @return                NOT YET DOCUMENTED
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public SyncReportEntry write(String recId, DcsDataRecord dcsDataRecord) throws Exception {
		return write(recId, null, dcsDataRecord);
	}


	/**
	 *  Creates or updates collection-level NDR Objects necessary to define a NSDL
	 *  collection in the NDR, including collection-level metadata object,
	 *  collection resource, and collection aggregator.<p>
	 *
	 *  NOTE: if the NCSL collection already exists, then there is a possibility
	 *  that the collection's resource URL has been modified. For this reason, we
	 *  have to locate the collection definition objects in the NDR by other means
	 *  than the resource URL ...
	 *
	 * @param  recId          record id, used to obtain collection metadata from
	 *      index if necessary
	 * @param  recordXml      collection metadata as String (optionally provided)
	 * @param  dcsDataRecord  auxillary information about the collection metadata
	 *      record
	 * @return                NOT YET DOCUMENTED
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public SyncReportEntry write(String recId, String recordXml, DcsDataRecord dcsDataRecord) throws Exception {

		if (this.servletContext == null)
			throw new Exception("servletContext not found");

		prtln("writing " + recId);
		this.recId = recId;
		this.dcsDataRecord = dcsDataRecord;
		this.finalAndValid = (dcsDataRecord.isFinalStatus() && dcsDataRecord.isValid());

		try {
			init(recordXml);
		} catch (Exception e) {
			throw new Exception("initialization error" + e.getMessage(), e);
		}

		String resourceUrl = null;
		try {
			resourceUrl = getResourceUrl(itemRecord);
		} catch (Exception e) {
			throw new Exception("ItemRecord Resource URL error", e);
		}

		this.ncsCollectReader = new NCSCollectReader(itemRecord);

		try {
			this.collectionAgent = getValidCollectionAgent();
			prtln("collection agent: " + this.collectionAgent);
		} catch (Exception e) {
			prtln(e.getMessage());
		}

		try {
			this.ingestAppAgent = getValidIngestAppAgent();
			prtln("ingest app agent: " + this.ingestAppAgent);
		} catch (Exception e) {
			prtln(e.getMessage());
		}

		/*
		    as of 2/13/2008 we don't act upon the application agent
		    String collectionAppAgent = this.getCollectionAppAgent(ncsCollectReader);
		    prtln("collection APP agent: " + collectionAppAgent);
		  */
		// Does there exist a collection metadata object?
		this.collectionMetadata = dcsDataRecord.getNdrHandle();
		prtln("collectionMetadata obtained from dcsDataRecord: " + collectionMetadata);

		this.collectionResource = getResourceHandle(resourceUrl);
		prtln("collection resource: " + collectionResource);

		this.collectionAggregator = null;
		this.collectionMetadataProvider = null;

		/*
		    if there is a collectionMetadata object, we use it to obtain the
		    collectionAggregator and collectionMP objects.
		    otherwise we have to create them (and this requires a collectionResource)
		  */
		if (getValueOrNull(collectionMetadata) != null) {
			prtln("collection Metadata exists (" + collectionMetadata + ")\n  - finding collection MDP and AGG ....");

			mdReader = getMetadataReader();
			collectionMetadataProvider = mdReader.getRelationship("ncs:collectionMetadataFor");

			prtln("\t existing mdp: " + collectionMetadataProvider);

			/*
			    the first time through, there won't be a mdp yet, but otherwise we can obtain a mdp reader
			  */
			if (getValueOrNull(collectionMetadataProvider) != null) {
				collectionAggregator = this.getMetadataProviderReader().getRelationship("aggregatedBy");
				prtln("\t existing agg: " + collectionAggregator);
			}
		}

		/*
		    we have to update aggregator and metadataprovider because the serviceDescription stream
		    of these objects is created from information in the ncs_collect record.
		    if the record is not valid and final, then we need to set the visibility of the MDP to
		    "private" (hiding it from OAI and Search service).
		    if the record IS valid and final, then we need to ensure that the visibility is set to
		    the value of the OAIProvider field in the ncs_collect record.
		  */
		collectionAggregator = updateAggregator();
		prtln("\t updated agg: " + collectionAggregator);

		collectionMetadataProvider = updateMetadataProvider();
		prtln("\t updated mdp: " + collectionMetadataProvider);

		NdrRequest writeRequest = getWriteRequest(resourceUrl, collectionResource);

		InfoXML response = writeRequest.submit();
		SyncReportEntry report = new SyncReportEntry(recId, writeRequest.getVerb(), collectionResource, response);

		if (report.isError()) {
			throw new Exception(report.getErrorMsg());
		}

		// always write the handle into DcsDataRecord, just to ensure that we are synced
		dcsDataRecord.setNdrHandle(report.getMetadataHandle());
		dcsDataRecord.setMetadataProviderHandle(collectionMetadataProvider);
		dcsDataRecord.setNsdlItemId(this.nsdlItemId);
		dcsDataRecord.setSetSpec(this.setSpec);

		dcsDataRecord.setLastSyncDate(new Date());
		dcsDataRecord.flushToDisk();

		return report;
	}


	/**
	 *  Validate the Collection Agent (if any) specificied in the NCSCollectionReader.
	 *  <p>
	 *
	 *  This test is important to do before any changes are made to the other
	 *  collection objects in the NDR, since there is no roll-back
	 *
	 * @return                A valid agent handle or null
	 * @exception  Exception  if an invalid agent handle is specified in the
	 *      NCSCollectionReader
	 */
	private String getValidCollectionAgent() throws Exception {
		String collectionAgent = this.ncsCollectReader.getCollectionAgent();
		if (collectionAgent == null || collectionAgent.trim().length() == 0)
			collectionAgent = null;
		else {
			NdrObjectReader reader = new NdrObjectReader(collectionAgent);
			if (reader == null)
				prtln("NDR Object not found for " + collectionAgent);
			if (reader.getObjectType() != NDRConstants.NDRObjectType.AGENT)
				throw new Exception("Specified Collection Agent (" + collectionAgent +
					") either does not exist or is not an Agent");
		}
		return collectionAgent;
	}


	private String getValidIngestAppAgent() throws Exception {
		String ingestAppAgent = this.ncsCollectReader.getApplicationAgent();
		if (ingestAppAgent == null || ingestAppAgent.trim().length() == 0)
			ingestAppAgent = null;
		else {
			NdrObjectReader reader = new NdrObjectReader(ingestAppAgent);
			if (reader == null)
				prtln("NDR Object not found for " + ingestAppAgent);
			if (reader.getObjectType() != NDRConstants.NDRObjectType.AGENT)
				throw new Exception("Specified Collection Agent (" + ingestAppAgent +
					") either does not exist or is not an Agent");
		}
		return ingestAppAgent;
	}


	/**
	 *  Gets the metadataForRelationsToAdd attribute of the NSDLCollectionWriter
	 *  object. To the "metadataFor" relation created by MetadataWriter we add a
	 *  "meatadataFor" relation to the collection resource.
	 *
	 * @param  resourceHandle  Description of the Parameter
	 * @return                 The metadataForRelationsToAdd value
	 */
	protected List getMetadataForRelationsToAdd(String resourceHandle) {
		List toAdd = super.getMetadataForRelationsToAdd(resourceHandle);
		toAdd.add(this.collectionAggregator);
		return toAdd;
	}


	/**
	 *  Adds a feature to the MetadataRequest attribute of the NSDLCollectionWriter
	 *  object
	 *
	 * @param  ndrRequest     The feature to be added to the MetadataRequest
	 *      attribute
	 * @param  resHandle      The feature to be added to the MetadataRequest
	 *      attribute
	 * @return                Description of the Return Value
	 * @exception  Exception  Description of the Exception
	 */
	protected NdrRequest addMetadataRequest(NdrRequest ndrRequest,
	                                        String resHandle) throws Exception {
		ndrRequest = super.addMetadataRequest(ndrRequest,
			resHandle);
		if (collectionAggregator != null) {
			ndrRequest.addCommand("relationship", "metadataFor", this.collectionAggregator);
		}
		if (collectionMetadataProvider != null) {
			ndrRequest.addNcsRelationshipCmd("collectionMetadataFor", collectionMetadataProvider);
		}
		return ndrRequest;
	}


	/**
	 *  Description of the Method
	 *
	 * @param  ndrRequest      Description of the Parameter
	 * @param  resHandle       Description of the Parameter
	 * @param  nsdl_dc_stream  Description of the Parameter
	 * @return                 Description of the Return Value
	 * @exception  Exception   Description of the Exception
	 */
	protected NdrRequest modifyMetadataRequest(NdrRequest ndrRequest,
	                                           String resHandle,
	                                           Element nsdl_dc_stream) throws Exception {
		NdrRequest modifyRequest = super.modifyMetadataRequest(ndrRequest,
			resHandle,
			nsdl_dc_stream);

		if (mdReader != null) {
			String currentMdp = mdReader.getRelationship("ncs:collectionMetadataFor");
			if (currentMdp == null) {
				ndrRequest.addNcsRelationshipCmd("collectionMetadataFor", collectionMetadataProvider, "add");
			}
			else if (!currentMdp.equals(collectionMetadataProvider)) {
				ndrRequest.addNcsRelationshipCmd("collectionMetadataFor", collectionMetadataProvider, "add");
				ndrRequest.addNcsRelationshipCmd("collectionMetadataFor", currentMdp, "delete");
			}
		}
		return modifyRequest;
	}


	/**
	 *  Gets the nsdlAboutCategory attribute of the NSDLCollectionWriter object
	 *
	 * @return    The nsdlAboutCategory value
	 */
	String getNsdlAboutCategory() {
		return "collect";
	}


	/**
	 *  Get the Collection Aggregator Object from the NDR, creating one if
	 *  necessary.<p>
	 *
	 *  Even if the aggregator already exists, we want to update it in case the
	 *  collection metadata has changed.<p>
	 *
	 *  Aggregator components:
	 *  <ul>
	 *    <li> datastream - Aggregator Service Description - created from
	 *    ncs_collect record</li>
	 *    <li> relationships:
	 *    <ul>
	 *      <li> assocatedWith: resourceHandle
	 *      <li> aggregatorFor: NCS Agent (obtained from servletContext)
	 *      <li> memberOf: 2200/NSDL_Collection_of_Collections_Aggregator
	 *    </ul>
	 *
	 *  </ul>
	 *
	 *
	 * @return                The resourceHandle value
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	private String updateAggregator()
		 throws Exception {

		/*
		    The aggregator object requires:
		    - relationships
		    - "memberOf" 2200/NSDL_Collection_of_Collections_Aggregator
		    - "associatedWith" collectionResource
		    - datastreams
		    - "serviceDescription"
		  */
		// Build the request to either create or modify the collection aggregator
		SignedNdrRequest request = new SignedNdrRequest();
		request.setObjectType(NDRConstants.NDRObjectType.AGGREGATOR);

		/*
		    For records that will get items through OAI harvests:
			Both MetadataProvider and Aggregator objects need to have
			authorized-to-change properties allowing the Harvest/Ingest process to update
			them.
		  */
		if (collectionAggregator == null) {
			request.setVerb("addAggregator");

			// aggregatorFor - use the collectionAgent if one exists,
			// otherwise the NCS agent
			String aggregatorFor =
				(this.collectionAgent != null ? this.collectionAgent : NDRConstants.getNcsAgent());
			request.addCommand("relationship", "aggregatorFor", aggregatorFor);

			request.authorizeToChange(NDRConstants.getNcsAgent());
			if (NDRConstants.getMasterAgent() != null)
				request.authorizeToChange(NDRConstants.getMasterAgent());
			if (NDRConstants.getMasterCollection() != null)
				request.addCommand("relationship", "memberOf",
					NDRConstants.getMasterCollection());
			if (this.ingestAppAgent != null)
				request.authorizeToChange(this.ingestAppAgent);
			request.authorizeToChange(NDRConstants.FEED_EATER_AGENT);
		}
		else {
			// the collectionAggregator exists
			request.setVerb("modifyAggregator");

			// aggregatorFor - only modify this relationship if there is a value in the ncs_collect record
			if (this.collectionAgent != null)
				request.addCommand("relationship", "aggregatorFor", this.collectionAgent);

			request.setHandle(collectionAggregator);
			request.authorizeToChange(NDRConstants.getNcsAgent(), "add");
			if (NDRConstants.getMasterAgent() != null)
				request.authorizeToChange(NDRConstants.getMasterAgent(), "add");
			if (this.ingestAppAgent != null)
				request.authorizeToChange(this.ingestAppAgent, "add");
			request.authorizeToChange(NDRConstants.FEED_EATER_AGENT, "add");
		}

		// always set the associatedWith relationship to the collection resource,
		// since it may have changed in the ncs_collect record
		request.addCommand("relationship", "associatedWith", collectionResource);

		// always update aggregator ServiceDescription
		ServiceDescription sd =
			ServiceDescription.makeServiceDescription(
			ncsCollectReader, NDRConstants.NDRObjectType.AGGREGATOR);
		request.addServiceDescriptionCmd(sd.asElement());

		InfoXML response = request.submit();
		if (response.hasErrors()) {
			throw new Exception(response.getError());
		}

		return response.getHandle();
	}


	/**
	 *  Gets the metadataReader attribute of the NSDLCollectionWriter object
	 *
	 * @return                The metadataReader value
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	protected MetadataReader getMetadataReader() throws Exception {
		if (this.mdReader == null)
			this.mdReader = new MetadataReader(dcsDataRecord.getNdrHandle(), "ncs_collect");
		return this.mdReader;
	}


	/**
	 *  Returns mdpReader for the collectionMetadataProvider (CMDP), if the CMDP
	 *  has a value.<p>
	 *
	 *  The CMDP is initially not assigned, and in the case of a new collection,
	 *  will not be assigned until {@link #updateMetadataProvider} returns. So this
	 *  method must always test for the existence of CMDP before attempting to
	 *  create the mdpReader. Callers of this method must test for null before
	 *  proceeding.
	 *
	 * @return                The mdpReader value or null if collectionMetadataProvider
	 *      has not been assigned
	 * @exception  Exception  if collectionMetadataProvider has a value but a MDP
	 *      reader cannot be created
	 */
	private MetadataProviderReader getMetadataProviderReader() throws Exception {
		// collectionMetadataProvider is the authoritative representation. if
		// it does not have a value, then mdpReader should not exist
		if (this.collectionMetadataProvider == null) {
			this.mdpReader = null;
			return null;
		}
		// if mdpReader exists but is pointed at an object other than
		// collectionMetadataProvider, then we have to destroy it and create
		// a new mdpReader that corresponds to collectionMetadataProvider
		if (this.mdpReader != null &&
			!this.mdpReader.getHandle().equals(this.collectionMetadataProvider)) {
			prtln("mdpReader exists, but with stale handle!");
			this.mdpReader = null;
		}
		if (this.mdpReader == null) {
			try {
				this.mdpReader = new MetadataProviderReader(this.collectionMetadataProvider);
				if (this.mdpReader == null)
					throw new Exception("contructor returned null");
			} catch (Exception e) {
				throw new Exception("could not create MDP reader: " + e.getMessage());
			}
		}
		return this.mdpReader;
	}


	/**
	 *  Returns the collection_config data_stream from the collection MDP if
	 *  possible. Used to test whether the NSDL Collection governs a collection
	 *  whose items are managed in the NCS.
	 *
	 * @return    The the collection_config data stream from the collection
	 *      mdpReader
	 */
	protected Element getNCSCollectionConfig() {
		try {
			return this.getMetadataProviderReader().getDataStream("collection_config");
		} catch (Exception e) {
			String errMsg = "WARNING: could not obtain mdpReader for " + collectionMetadataProvider;
			errMsg += ": " + e.getMessage();
			prtlnErr(errMsg);
		}
		return null;
	}


	/**
	 *  For Collections whose items are managed by this NCS instance, we can find
	 *  the itemFormat by inspecting the "collection_config" stream of the
	 *  Collection MetadataProvider object.
	 *
	 * @return    The nCSCollectionNativeFormat value
	 */
	protected String getNCSCollectionNativeFormat() {
		try {
			Element mdpColconfig = this.getNCSCollectionConfig();
			if (mdpColconfig == null)
				throw new Exception("WARNING - mdpColconfig not found for " + collectionMetadataProvider);
			return mdpColconfig.selectSingleNode("xmlFormat").getText();
		} catch (Throwable t) {
			this.prtlnErr("getNCSCollectionNativeFormat: " + t.getMessage());
		}
		return null;
	}



	/**
	 *  Gets the collectionMDP attribute of the NSDLCollectionWriter object
	 *
	 * @return                handle for collection metadataProvider object
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	private String updateMetadataProvider() throws Exception {

		/*
		    The mdp requires
		    - relations
		    - "aggregatedBy" collectionAggregator
		    - datastreams
		    - "serviceDescription"
		  */
		SignedNdrRequest request = new SignedNdrRequest();
		request.setObjectType(NDRConstants.NDRObjectType.METADATAPROVIDER);

		// if the MDP does not exist or does not contain a setSpec, assign it
		// based on recordID
		try {
			this.setSpec = this.getMetadataProviderReader().getSetSpec();
		} catch (Throwable t) {
			this.setSpec = "ncs-" + ncsCollectReader.getRecordID();
		}
		
		if (collectionMetadataProvider == null) {

			request.setVerb("addMetadataProvider");

			// metadataProviderFor - use the collectionAgent if one exists,
			// otherwise the NCS agent
			String metadataProviderFor =
				(this.collectionAgent != null ? this.collectionAgent : NDRConstants.getNcsAgent());
			request.addCommand("relationship", "metadataProviderFor", metadataProviderFor);

			request.addCommand("relationship", "aggregatedBy", collectionAggregator);
			request.addCommand("property", "setSpec", this.setSpec);

			request.authorizeToChange(NDRConstants.getNcsAgent());
			if (NDRConstants.getMasterAgent() != null)
				request.authorizeToChange(NDRConstants.getMasterAgent());
			if (this.ingestAppAgent != null)
				request.authorizeToChange(this.ingestAppAgent);
		}
		else {

			// the metadataprovider exists
			request.setVerb("modifyMetadataProvider");

			// metadataProviderFor - only modify this relationship if there is a collectionAgent
			// specified in the ncs_collect record
			if (this.collectionAgent != null)
				request.addCommand("relationship", "metadataProviderFor", this.collectionAgent);

			request.setHandle(collectionMetadataProvider);

			request.authorizeToChange(NDRConstants.getNcsAgent(), "add");
			if (NDRConstants.getMasterAgent() != null)
				request.authorizeToChange(NDRConstants.getMasterAgent(), "add");
			if (this.ingestAppAgent != null)
				request.authorizeToChange(this.ingestAppAgent, "add");
		}

		// always update an aggregator ServiceDescription (based on collection_info of ncs_collect)
		ServiceDescription sd =
			ServiceDescription.makeServiceDescription(
			ncsCollectReader, NDRConstants.NDRObjectType.METADATAPROVIDER);
		request.addServiceDescriptionCmd(sd.asElement());

		// always update oai:visibility (because this is controlled by ncs_collect)
		// use value from ncs_collect if record is final and valid, PRIVATE otherwise
		if (this.finalAndValid) {
			request.addOaiVisibilityCmd(ncsCollectReader.getOaiVisibility());
		}
		else {
			request.addOaiVisibilityCmd(NDRConstants.OAIVisibilty.PRIVATE);
		}

		// always update the nativeFormat property from the ncs_collect record IF this
		// collection is OAI ingested
		if (this.ncsCollectReader.isOaiIngest()) {
			request.addNcsPropertyCmd("nativeFormat", this.ncsCollectReader.getOaiFormat());
		}
		else if (this.getNCSCollectionConfig() != null) {
			// if this collection is managed in the NCS, we obtain the xmlFormat using
			// getNCSCollectionNativeFormat()
			String ncsCollectionNativeFormat = this.getNCSCollectionNativeFormat();
			request.addNcsPropertyCmd("nativeFormat", ncsCollectionNativeFormat);
		}

		// always update setName (based on title of ncs_collect)
		request.addCommand("property", "setName", ncsCollectReader.getTitle());

		InfoXML response = request.submit();
		if (response.hasErrors()) {
			throw new Exception(response.getError());
		}

		return response.getHandle();
	}


	/**
	 *  Print a line to standard out.
	 *
	 * @param  s  The String to print.
	 */
	private static void prtln(String s) {
		if (debug) {
			SchemEditUtils.prtln(s, "NSDLCollectionWriter");
		}
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  s  NOT YET DOCUMENTED
	 */
	private static void prtlnErr(String s) {
		SchemEditUtils.prtln(s, "NSDLCollectionWriter");
	}
}

