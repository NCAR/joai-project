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
import org.dlese.dpc.schemedit.SchemEditUtils;
import org.dlese.dpc.schemedit.MetaDataFramework;
import org.dlese.dpc.schemedit.FrameworkRegistry;
import org.dlese.dpc.schemedit.repository.RepositoryService;
import org.dlese.dpc.schemedit.config.CollectionRegistry;
import org.dlese.dpc.schemedit.config.CollectionConfig;
import org.dlese.dpc.schemedit.dcs.DcsSetInfo;
import org.dlese.dpc.schemedit.dcs.DcsDataManager;
import org.dlese.dpc.schemedit.dcs.DcsDataRecord;

import org.dlese.dpc.ndr.NdrUtils;
import org.dlese.dpc.ndr.apiproxy.InfoXML;
import org.dlese.dpc.ndr.apiproxy.NDRConstants;
import org.dlese.dpc.ndr.reader.MetadataReader;
import org.dlese.dpc.ndr.request.*;

import org.dlese.dpc.index.reader.XMLDocReader;
import org.dlese.dpc.repository.RepositoryManager;
import org.dlese.dpc.xml.Dom4jUtils;
import org.dlese.dpc.xml.XMLConversionService;

import org.dom4j.*;
import java.util.*;
import java.net.*;

import javax.servlet.ServletContext;

/**
 *  Class responsible for writing Metadata records to the NDR as Metadata
 *  Objects.
 *
 * @author    Jonathan Ostwald
 */
public class MetadataWriter {

	private static boolean debug = true;
	/**  NOT YET DOCUMENTED */
	protected ServletContext servletContext = null;

	/**  NOT YET DOCUMENTED */
	protected MetadataReader mdReader = null;

	/**  NOT YET DOCUMENTED */
	protected RepositoryManager rm;
	/**  NOT YET DOCUMENTED */
	protected String recId;
	/**  NOT YET DOCUMENTED */
	protected XMLDocReader docReader;
	/**  NOT YET DOCUMENTED */
	protected DcsDataRecord dcsDataRecord;
	/**  NOT YET DOCUMENTED */
	protected CollectionConfig collectionConfig;
	/**  NOT YET DOCUMENTED */
	protected XMLConversionService xmlConversionService;

	/**  NOT YET DOCUMENTED */
	protected Document itemRecord = null;
	/**  Description of the Field */
	protected boolean finalAndValid = false;
	
	protected String nsdlItemId = null;


	/**  Constructor for the MetadataWriter object */
	public MetadataWriter() { }


	/**
	 *  Constructor for the MetadataWriter object
	 *
	 * @param  servletContext  NOT YET DOCUMENTED
	 * @exception  Exception   NOT YET DOCUMENTED
	 */
	public MetadataWriter(ServletContext servletContext) throws Exception {
		this.servletContext = servletContext;
	}


	/**
	 *  Sets the servletContext attribute of the MetadataWriter object
	 *
	 * @param  servletContext  The new servletContext value
	 */
	public void setServletContext(ServletContext servletContext) {
		this.servletContext = servletContext;
	}


	/**
	 *  Initialize required values and services for the MetadataWriter.<p>
	 *
	 *  RepositoryManager, docReader, itemRecord, xmlConversionService,
	 *  collectionConfig.
	 *
	 * @param  recordXml      metadata, can be null.
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	protected void init(String recordXml) throws Exception {

		RepositoryManager rm = (RepositoryManager) getServletContextAttribute("repositoryManager");
		this.docReader = RepositoryService.getXMLDocReader(recId, rm);
		if (docReader == null) {
			throw new Exception("itemRecord not found in repository (" + recId + ")");
		}
		
		this.mdReader = null;

		try {
			this.itemRecord = getItemRecord(recordXml);
		} catch (Exception e) {
			throw new Exception("could not construct item record", e);
		}

		xmlConversionService = null;
		try {
			xmlConversionService =
				(XMLConversionService) rm.getIndex().getAttribute("xmlConversionService");
		} catch (Throwable t) {}
		if (xmlConversionService == null) {
			throw new Exception("unable to obtain xmlConversionService");
		}

		collectionConfig = this.getCollectionConfig(docReader.getCollection());
		if (collectionConfig == null) {
			throw new Exception("collection config not found for " + docReader.getCollection());
		}
	}


	/**
	 *  Convenience method to write the metadata record identified by recId along
	 *  with dcsDataRecord, to the NDR.<p>
	 This method is called from OUTSIDE the metadata editor (e.g., Sync), when 
	 there is not access to the actual XML of the record.
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
	 *  Writes metadata record to NDR using provided "recordXml" metadata if
	 *  provided, or metadata obtained from from the index if recordXml is null.<p>
	 *
	 *
	 *
	 * @param  recId          recordId, used to obtain metadata from index if
	 *      necessary
	 * @param  recordXml      metadata as String (optionally provided)
	 * @param  dcsDataRecord  auxillary information about the metadata record
	 * @return                NOT YET DOCUMENTED
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public SyncReportEntry write(String recId, String recordXml, DcsDataRecord dcsDataRecord) throws Exception {
		if (this.servletContext == null)
			throw new Exception("servletContext not found");

		prtln("\nwrite() recId: " + recId);
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

		// prtln("\t resourceUrl: " + resourceUrl);
		String resHandle = getResourceHandle(resourceUrl);
		// prtln("resource Handle otained from NDR: " + resHandle);

		NdrRequest request = getWriteRequest(resourceUrl, resHandle);
		InfoXML response = request.submit();
		SyncReportEntry report = new SyncReportEntry(recId, request.getVerb(), resHandle, response);

		if (report.isError()) {
			throw new Exception(report.getErrorMsg());
		}
		else {
			// prtln("\n ***** mdHandle: " + report.getMetadataHandle());

			// always write the handle into DcsDataRecord, just to ensure that we are synced
			dcsDataRecord.setNdrHandle(report.getMetadataHandle());
			dcsDataRecord.setNsdlItemId(this.nsdlItemId);
			dcsDataRecord.setLastSyncDate(new Date());
			dcsDataRecord.flushToDisk();
		}
		return report;
	}


	/**
	 *  Returns NdrRequest to write metadata to NDR, either as a new metadata
	 *  object or by updating an existing object.
	 *
	 * @param  resHandle      resource object handle
	 * @param  resourceUrl    NOT YET DOCUMENTED
	 * @return                report instance indicating success or failure
	 * @exception  Exception  if unable to build ndr request
	 */
	protected NdrRequest getWriteRequest(String resourceUrl,
	                                     String resHandle) throws Exception {
		NdrRequest ndrRequest = new SignedNdrRequest();
		ndrRequest.setObjectType(NDRConstants.NDRObjectType.METADATA);

		ndrRequest.addNcsPropertyCmd("status", this.getNormalizedStatus());
		ndrRequest.addNcsPropertyCmd("isValid", this.getIsValid());

		// We set the metadataProvider each time in case the record has moved or
		// the mdp has been reassigned.
		String metadataProviderHandle = collectionConfig.getMetadataProviderHandle();
		ndrRequest.addCommand("relationship", "metadataProvidedBy", metadataProviderHandle);

		// dcs_data stream
		Element dcs_data_stream = dcsDataRecord.getDocument().getRootElement();
		ndrRequest.addDataStreamCmd("dcs_data", dcs_data_stream);

		// primary metadata stream
		String xmlFormat = docReader.getNativeFormat();
		Element native_data_stream = getNativeDataStream(itemRecord, xmlFormat);
		ndrRequest.addNativeDataStreamCmd(xmlFormat, native_data_stream);

		// set OAI visibility in accordance with finalAndValid
		if (this.finalAndValid) {
			ndrRequest.addOaiVisibilityCmd(NDRConstants.OAIVisibilty.PUBLIC);
		}
		else {
			ndrRequest.addOaiVisibilityCmd(NDRConstants.OAIVisibilty.PRIVATE);
		}
		
		// add nsdl_dc data stream (regardless of status or validity
		Element nsdl_dc = getNSDL_DC(itemRecord);
		if (nsdl_dc != null) {
			ndrRequest.addDataStreamCmd("nsdl_dc", nsdl_dc);
		}

		/*
		    based on whether there is a NdrHandle stored in the DcsDataRecord,
		    determine whether we are
		    - creating an NEW metadata Object OR
		    - modifying an existing one
		  */
		String mdHandle = dcsDataRecord.getNdrHandle();
		boolean metadataExists = false;
		// verify that the metadata object exists!
		if (mdHandle != null && mdHandle.trim().length() > 0) {
			try {
				metadataExists = (this.getMetadataReader() != null);
			} catch (Exception e) {
				prtln("metadata handle in dcsDataRecord is bogus!");
			}
		}

		// add new or modify existing metadata object by completing the request and submitting
		if (metadataExists) {
			return modifyMetadataRequest(ndrRequest, resHandle, nsdl_dc);
		}
		else {
			return addMetadataRequest(ndrRequest, resHandle);
		}
	}


	/**
	 *  Augment provided NdrRequest to form an "addMetadataRequest" request to
	 *  create a new NDR object.
	 *
	 * @param  resHandle      resource object handle
	 * @param  ndrRequest     The feature to be added to the Metadata attribute
	 * @return                report instance indicating success or failure
	 * @exception  Exception  if unable to build ndr request
	 */
	protected NdrRequest addMetadataRequest(NdrRequest ndrRequest,
	                                        String resHandle) throws Exception {
		prtln("addMetadataRequest");

		ndrRequest.setVerb("addMetadata");

		/*
		    Set Static properties - the won't change over lifetime of NDR Object.
		    - uniqueId (required) - id that is unique for all Metadata objects supplied by
		    a single MetadataProvider
		  */
		ndrRequest.addNcsPropertyCmd("recordId", recId);
		ndrRequest.addCommand("property", "uniqueID", recId);
		this.nsdlItemId = "oai:nsdl.org:ncs:" + recId;
		ndrRequest.addCommand("property", "itemId", nsdlItemId);

		ndrRequest.addCommand("relationship", "metadataFor", resHandle);
		return ndrRequest;
	}



	/**
	 *  Augment provided NdrRequest to form a "modifyMetadataRequest" request for
	 *  updating an existing metadata object in the NDR.<p>
	 *
	 *  Collection-level objects (AggHandle and MdpHandle) are non-null only if the
	 *  metadata is a NSDL collection record (i.e., ncs_collect format).
	 *
	 * @param  resHandle       resource object handle
	 * @param  ndrRequest      NOT YET DOCUMENTED
	 * @param  nsdl_dc_stream  NOT YET DOCUMENTED
	 * @return                 report instance indicating success or failure
	 * @exception  Exception   if unable to build ndr request
	 */
	protected NdrRequest modifyMetadataRequest(NdrRequest ndrRequest,
	                                           String resHandle,
	                                           Element nsdl_dc_stream) throws Exception {

		ndrRequest.setVerb("modifyMetadata");
		ndrRequest.setHandle(dcsDataRecord.getNdrHandle());

		// if the nsdl_dc data stream is null, we delete it from the metadata record
		// We never remove an nsdl_dc_stream per 4/30/2008 discussion with CI
		// issue: now nsdl_dc can be out of sync with primary metadata ...
/* 		if (nsdl_dc_stream == null) {
			ndrRequest.addDataStreamCmd("nsdl_dc", null, "delete");
		} */

		this.nsdlItemId = this.getMetadataReader().getProperty("itemId");
		setMetadataForRelationships(ndrRequest, resHandle);

		return ndrRequest;
	}


	/**
	 *  Gets the metadataReader attribute of the MetadataWriter object
	 *
	 * @return                The metadataReader value
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	protected MetadataReader getMetadataReader() throws Exception {
		if (this.mdReader == null) {
			this.mdReader = new MetadataReader(dcsDataRecord.getNdrHandle(), docReader.getNativeFormat());
		}
		return this.mdReader;
	}


	/**
	 *  Gets the metadataForRelationsToAdd attribute of the MetadataWriter object
	 *
	 * @param  resourceHandle  Description of the Parameter
	 * @return                 The metadataForRelationsToAdd value
	 */
	List getMetadataForRelationsToAdd(String resourceHandle) {
		List toAdd = new ArrayList();
		toAdd.add(resourceHandle);
		return toAdd;
	}


	/**
	 *  Gets the nsdlAboutCategory attribute of the MetadataWriter object.<p>
	 *
	 *  "item" except when framework is ncs_collect, and then it is "collect"
	 *  nsdlAboutCategory is "item" except when framework is ncs_collect, and then
	 *  it is "collect"
	 *
	 * @return    The nsdlAboutCategory value
	 */
	String getNsdlAboutCategory() {
		return "item";
	}


	/**
	 *  Agument the provided NdrRequest object with computed "metedataFor"
	 *  relationships.<p>
	 *
	 *  Item metadata will have one metadataFor relationship (enforcing the DLESE
	 *  Collection model.
	 *
	 * @param  ndrRequest      The new metadataForRelationships value
	 * @param  resourceHandle  The new metadataForRelationships value
	 * @exception  Exception   NOT YET DOCUMENTED
	 */
	void setMetadataForRelationships(NdrRequest ndrRequest, String resourceHandle) throws Exception {
		MetadataReader reader = getMetadataReader();
		List currentMetadataForHandles = reader.getRelationshipValues("metadataFor");

		List toDelete = new ArrayList();
		List toAdd = getMetadataForRelationsToAdd(resourceHandle);

		for (Iterator i = currentMetadataForHandles.iterator(); i.hasNext(); ) {
			String handle = (String) i.next();
			if (toAdd.contains(handle)) {
				toAdd.remove(handle);
			}
			else {
				toDelete.add(handle);
			}
		}

		for (Iterator i = toAdd.iterator(); i.hasNext(); ) {
			ndrRequest.addCommand("relationship", "metadataFor", (String) i.next(), "add");
		}
		for (Iterator i = toDelete.iterator(); i.hasNext(); ) {
			ndrRequest.addCommand("relationship", "metadataFor", (String) i.next(), "delete");
		}
	}

	/**
	 *  Obtain a resource handle from the NDR for the provided resourceURL. A new
	 *  resource is created if one does not already exist in the NDR. In either
	 *  case, a memberOf relationship is created between the resource and the
	 *  collection's aggregator.
	 *
	 * @param  resourceUrl    url for the resorce to be found or created
	 * @return                The resourceHandle value
	 * @exception  Exception  if a collection aggregator is not found, or if the
	 *      resource cannot be created or modified.
	 */
	protected String getResourceHandle(String resourceUrl) throws Exception {

		SignedNdrRequest request = null;

		/*
		    All Collections managed by the NCS will have an aggregator, which
		    must be related to the resource by the "memberOf" relationship.
		  */
		String aggHandle = collectionConfig.getAggregatorHandle();
		if (aggHandle == null || aggHandle.trim().length() == 0) {
			throw new Exception("Aggregator not found");
		}

		String resourceHandle = NdrUtils.findResource(resourceUrl);
		if (resourceHandle != null) {
			//prtln("resource found: " + resourceHandle + "\n");
			request = new ModifyResourceRequest(resourceHandle);
			// provide explicit "add" command since we are modifying
			request.addCommand("relationship", "memberOf", aggHandle, "add");
		}
		else {
			// prtln("resource NOT found, creating new one\n");
			request = new AddResourceRequest();
			((AddResourceRequest) request).setIdentifier(resourceUrl);
			request.addCommand("relationship", "memberOf", aggHandle);
		}

		InfoXML response = request.submit();
		if (response.hasErrors()) {
			throw new Exception(response.getError());
		}

		return response.getHandle();
	}


	/**
	 *  Gets the metadata record as a dom4j.Document, converting from recordXml if
	 *  it is provided, or obtaining from the index (via docReader) otherwise.
	 *
	 * @param  recordXml      metadata record as XML String
	 * @return                The itemRecord value
	 * @exception  Exception  if provided recordXml is not well-formed.
	 */
	protected Document getItemRecord(String recordXml) throws Exception {

		return (recordXml != null ?
			DocumentHelper.parseText(recordXml) :
			docReader.getXmlDoc());
	}


	/**
	 *  Returns metadata record transformed to nsdl_dc format.
	 *
	 * @param  itemRecord  metadata record as Document
	 * @return             The transformed record, or null if unable to transform
	 */
	private Element getNSDL_DC(Document itemRecord) {
		String fromFormat = collectionConfig.getXmlFormat();
		String toFormat = "nsdl_dc";
		Element nsdl_dc = null;

		if (xmlConversionService == null || !xmlConversionService.canConvert(fromFormat, toFormat)) {
			prtln("conversionService could not convert from \"" + fromFormat +
				"\" to \"" + toFormat + "\" - likely no transform is registered");
			return null;
		}
		else {
			try {
				String transformedXml = xmlConversionService.convertXml(fromFormat, toFormat, itemRecord.asXML());

				if (transformedXml == null || transformedXml.trim().length() == 0) {
					prtln("apparently unable to transform ...");
					nsdl_dc = null;
				}
				else {
					nsdl_dc = DocumentHelper.parseText(transformedXml).getRootElement();
				}
			} catch (Exception e) {
				prtlnErr("error transforming to nsdl_dc" + e.getMessage());
			}
		}

		return nsdl_dc;
	}


	/**
	 *  Gets the normalizedStatus attribute of the MetadataWriter object
	 *
	 * @return    The normalizedStatus value
	 */
	private String getNormalizedStatus() {
		return (dcsDataRecord.isFinalStatus() ? NDRConstants.NCS_FINAL_STATUS : dcsDataRecord.getStatus());
	}


	/**
	 *  Gets the isValid attribute of the MetadataWriter object
	 *
	 * @return    The isValid value
	 */
	private String getIsValid() {

		return (dcsDataRecord.isValid() ? "true" : "false");
	}


	/**
	 *  Gets the valueOrNull attribute of the MetadataWriter object
	 *
	 * @param  s  Description of the Parameter
	 * @return    The valueOrNull value
	 */
	protected String getValueOrNull(String s) {
		if (s == null || s.trim().length() == 0) {
			return null;
		}
		return s;
	}


	/**
	 *  Gets data stream for metadata in native xml format, which is the root
	 *  element of the provided Document. If the native xml format is "oai_dc" we
	 *  perform a (LKLUDGE) tranformation of the namespaces to convert from
	 *  dlese_oai to oai_dc.
	 *
	 * @param  itemRecord  NOT YET DOCUMENTED
	 * @param  xmlFormat   NOT YET DOCUMENTED
	 * @return             The primaryDataStream value
	 */
	private Element getNativeDataStream(Document itemRecord, String xmlFormat) {
		Element itemRoot = itemRecord.getRootElement().createCopy();

		// oai_dc KLUDGE - swap namespace for DCS oai_dc to ndsl's oai_dc
		// NOTE!! this should be done with a javaFormatConverter??
		if (xmlFormat.equals("oai_dc")) {
			QName schemaLocation = DocumentHelper.createQName("schemaLocation",
				DocumentHelper.createNamespace("xsi", "http://www.w3.org/2001/XMLSchema-instance"));
			itemRoot.addAttribute(schemaLocation,
				"http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd");
		}
		return itemRoot;
	}


	/**
	 *  Get and validate the URL using the urlPath configured for this framework
	 *  (NOTE: url path must be configured for the framework of the record to be
	 *  written). For single-namespace frameworks, url must be obtained from
	 *  localized record.
	 *
	 * @param  itemRecord     NOT YET DOCUMENTED
	 * @return                The resourceUrl value
	 * @exception  Exception  if resourceUrl cannot be obtained
	 */
	protected String getResourceUrl(Document itemRecord) throws Exception {

		// Document doc = docReader.getXmlDoc();
		String xmlFormat = docReader.getNativeFormat();
		MetaDataFramework itemFramework = getMetaDataFramework(xmlFormat);

		if (itemFramework == null) {
			throw new Exception("\"" + xmlFormat + "\" framework was not found");
		}

		if (xmlFormat.equals("concepts")) {
			return "http://acorn.dls.ucar.edu:7248/dps";
		}
		
		String resourceUrl = null;
		try {
			resourceUrl = itemFramework.getRecordUrl(itemRecord);
			if (resourceUrl == null) {
				// throw new Exception("URL required to write metadata to the NDR");
				throw new Exception("URL not found in record");
			}
			URL test = new URL(resourceUrl);
		} catch (MalformedURLException urlEx) {
			throw new Exception("malformed URL");
		}
		return resourceUrl;
	}


	/**
	 *  Gets the requiredContextAttributeValue attribute of the MetadataWriterPlugin
	 *  object
	 *
	 * @param  attrName       NOT YET DOCUMENTED
	 * @return                The requiredContextAttributeValue value
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	protected Object getServletContextAttribute(String attrName) throws Exception {
		Object attrValue = servletContext.getAttribute(attrName);
		if (attrValue == null) {
			throw new Exception("\"" + attrName + "\" not found in servletContext");
		}
		return attrValue;
	}


	/**
	 *  Gets the collectionConfig attribute of the RepositoryWriterPlugin object
	 *
	 * @param  collection     NOT YET DOCUMENTED
	 * @return                The collectionConfig value
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	protected CollectionConfig getCollectionConfig(String collection) throws Exception {
		CollectionRegistry reg =
			(CollectionRegistry) getServletContextAttribute("collectionRegistry");
		return reg.getCollectionConfig(collection);
	}


	/**
	 *  Gets the metaDataFramework attribute of the MetadataWriter object
	 *
	 * @param  xmlFormat      NOT YET DOCUMENTED
	 * @return                The metaDataFramework value
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	protected MetaDataFramework getMetaDataFramework(String xmlFormat) throws Exception {
		FrameworkRegistry reg =
			(FrameworkRegistry) getServletContextAttribute("frameworkRegistry");
		return reg.getFramework(xmlFormat);
	}


	/**
	 *  Print a line to standard out.
	 *
	 * @param  s  The String to print.
	 */
	private static void prtln(String s) {
		if (debug) {
			SchemEditUtils.prtln(s, "MetadataWriter");
		}
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  s  NOT YET DOCUMENTED
	 */
	private static void prtlnErr(String s) {
		SchemEditUtils.prtln(s, "MetadataWriter");
	}
}

