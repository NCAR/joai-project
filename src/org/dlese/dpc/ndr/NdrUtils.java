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
package org.dlese.dpc.ndr;

import org.dlese.dpc.ndr.reader.*;
import org.dlese.dpc.ndr.request.*;
import org.dlese.dpc.ndr.apiproxy.InfoXML;
import org.dlese.dpc.ndr.apiproxy.NDRConstants;
import org.dlese.dpc.ndr.apiproxy.NDRConstants.NDRObjectType;
import org.dlese.dpc.util.TimedURLConnection;
import org.dlese.dpc.util.URLConnectionTimedOutException;
import java.io.IOException;
import org.dlese.dpc.propertiesmgr.PropertiesManager;

import org.dlese.dpc.xml.Dom4jUtils;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import java.io.File;
import java.net.URL;
import java.util.*;
import java.text.*;

/**
 *  Collection of static methods for setting up NDR proxy and performing
 *  operations on the NDR. <p>
 *
 *  NOTE: many NDR proxy functions require a ndrPrivateKey. Edit this file to
 *  set the "ndrPrivateKeyFile" attribute to a key file accessible to your
 *  filesystem to enable these functions.
 *
 * @author    ostwald<p>
 *
 *
 */
public class NdrUtils {

	private static boolean debug = true;


	/**
	 *  Stand in for the official NDR normalizing routine. Ensures a trailing slash
	 *  if the url is simply a domain (with no path or query parts)
	 *
	 * @param  urlStr  NOT YET DOCUMENTED
	 * @return         NOT YET DOCUMENTED
	 */
	public static String normalizeUrl(String urlStr) {
		prtln("normalizing: " + urlStr);
		URL url = null;
		try {
			url = new URL(urlStr);
		} catch (Exception e) {
			prtln("couldn't form url");
			return urlStr;
		}
		String path = url.getPath();
		String query = url.getQuery();
		if ((path == null || "".equals(path)) && (query == null || "".equals(query)))
			return urlStr + "/";
		else
			return urlStr;
	}


	/**
	 *  Configures system to communicate with specified ndrServer (on read-only
	 *  basis)
	 *
	 * @param  ndrServer  the ndr server api url (e.g., "http://ndrtest.nsdl.org/api")
	 */
	public static void setup(String ndrServer) {
		try {
			NdrUtils.initProp();
		} catch (Throwable t) {
			prtln("initProp error: " + t.getMessage());
		}

		NDRConstants.init(ndrServer, null, null);
	}


	/**
	 *  Sets configuration parameters from provided properties file to communicate
	 *  with an NDR instance<p>
	 *
	 *  Properties:
	 *  <ul>
	 *    <li> ndr.api.base.url (e.g., "http://ndrtest.nsdl.org/api") - required
	 *
	 *    <li> ncs.agent.handle - required for signed operations
	 *    <li> ndr.private.key - (path to privatekey file for ncs.agent.handle)
	 *    required for signed operations
	 *  </ul>
	 *
	 *
	 * @param  propsFile  the properties file
	 */
	public static void setup(File propsFile) {
		String ndrServer = null;
		String ncsAgentHandle = null;
		String keyPath = null;
		try {
			PropertiesManager props = new PropertiesManager(propsFile.getAbsolutePath());
			ndrServer = props.getProp("ndr.api.base.url", "");
			ncsAgentHandle = props.getProp("ncs.agent.handle", "");
			keyPath = props.getProp("ndr.private.key", "");

			NdrUtils.initProp();
		} catch (Throwable t) {
			prtln("initProp error: " + t.getMessage());
		}

		NDRConstants.init(ndrServer, ncsAgentHandle, keyPath);
	}


	/**
	 *  Intialize NDR Constants to enable interaction with NDR.
	 *
	 * @param  ndrServer       NOT YET DOCUMENTED
	 * @param  ncsAgentHandle  NOT YET DOCUMENTED
	 * @param  keyPath         NOT YET DOCUMENTED
	 */
	public static void setup(String ndrServer, String ncsAgentHandle, String keyPath) {
		try {
			NdrUtils.initProp();
		} catch (Throwable t) {
			prtln("initProp error: " + t.getMessage());
		}

		NDRConstants.init(ndrServer, ncsAgentHandle, keyPath);

	}


	/**
	 *  Returns null if the string is null or empty, or the original string
	 *  otherwise. Used to ensure a value is either null or has value.
	 *
	 * @param  s  NOT YET DOCUMENTED
	 * @return    The nonEmpyStringOrNull value
	 */
	public final static String getNonEmpyStringOrNull(String s) {
		return (s == null || s.trim().length() == 0 ? null : s);
	}


	/**
	 *  Returns true if the provided handle is for a MetadataProvider that is part
	 *  of a NDR Collection Definition (as opposed to a MDP that is used to group
	 *  metadata for an NCS collection that is not seen by the NDR).<p>
	 *
	 *  NOTE: This is not a general solution, but instead depends upon an NCS
	 *  relationship. See isNDRCollectionMetadataProvider for a general solution.
	 *
	 * @param  mdpHandle      NOT YET DOCUMENTED
	 * @return                The nDRCollectionMDP1 value
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public static boolean isNDRCollectionMDP1(String mdpHandle) throws Exception {

		FindRequest request = new FindRequest(NDRConstants.NDRObjectType.METADATA);
		request.addNcsRelationshipCmd("collectionMetadataFor", mdpHandle);
		return (request.getResultHandle() != null);
	}


	/**
	 *  Returns true if the provided handle is for a MetadataProvider that is part
	 *  of a NDR Collection Definition (as opposed to a MDP that is used to group
	 *  metadata for an NCS collection that is not seen by the NDR).<p>
	 *
	 *  Algorithm:
	 *  <ol>
	 *    <li> obtain the aggregatorFor the provided MDP
	 *    <li> find the resource that is "associatedWith" the aggregator
	 *    <li> if there is a metadata object that is "metadataFor" BOTH the
	 *    aggregator and the resource, then the mdp was an NDR Collection
	 *    MetadataProvider.
	 *  </ol>
	 *
	 *
	 * @param  mdpHandle  NOT YET DOCUMENTED
	 * @return            The nDRCollectionMetadataProvider value
	 */
	public static boolean isNDRCollectionMetadataProvider(String mdpHandle) {
		try {
			MetadataProviderReader mdpReader = new MetadataProviderReader(mdpHandle);
			String aggHandle = mdpReader.getRelationship("aggregatedBy");
			AggregatorReader aggReader = new AggregatorReader(aggHandle);
			String resHandle = aggReader.getRelationship("associatedWith");

			FindRequest request = new FindRequest(NDRConstants.NDRObjectType.METADATA);
			request.addCommand("relationship", "metadataFor", aggHandle);
			request.addCommand("relationship", "metadataFor", resHandle);

			return (request.getResultHandle() != null);
		} catch (Throwable t) {
			// t.printStackTrace();
		}
		return false;
	}


	/**
	 *  Retrieve list of metadata objects provided by specified metadataProvider
	 *  whose metadata is valid and whose status is Final.
	 *
	 * @param  mdpHandle      handle of metadataProvider owning desired metadata
	 * @return                list of handles for final and valid metadata
	 * @exception  Exception  when ndrRequest results in error from NDR
	 */
	public static List findFinalValidResources(String mdpHandle) throws Exception {
		FindRequest request = new FindRequest();
		request.setObjectType(NDRObjectType.METADATA);
		request.addCommand("relationship", "metadataProvidedBy", mdpHandle);
		request.addNcsPropertyCmd("status", NDRConstants.NCS_FINAL_STATUS);

		// ??? why should this be commented out ???
		// request.addNcsPropertyCmd("isValid", "true");

		InfoXML response = request.submit();
		if (response.hasErrors())
			throw new Exception(response.getError());
		return new NdrResponseReader(response).getHandleList();
	}


	/**
	 *  Finds the handle of the resource associated with provided url.
	 *
	 * @param  url  resource url to find
	 * @return      resource handle or null if resource is not found
	 */
	public static String findResourceOld(String url) {
		FindResourceRequest request = new FindResourceRequest(url);
		InfoXML response;
		try {
			response = request.submit();
		} catch (Exception e) {
			prtln("findResource error: " + e.getMessage());
			return null;
		}
		String handle = response.getHandle();

		/* when deleting collections (MDP, MD, AGG) and then recreating them, we are
		running into a strange situation in which the findResource request returns BOTH
		a handle and an error element. NOTE: the handle is returned as a child of the root,
		unlike in the normal situation assumed by response.getHandle(). This condition usually
		indicates the resource is still related to a deleted metadata object. I have discovered
		that this condition still allows us to reuse the resource in a new metadata relation.
		*/
		if (handle == null) {
			try {
				Document doc = DocumentHelper.parseText(response.getResponse());
				doc = Dom4jUtils.localizeXml(doc);
				Node handleNode = doc.selectSingleNode("/NSDLDataRepository/handle");
				if (handleNode != null)
					handle = handleNode.getText();
			} catch (Throwable e) {}
		}
		return handle;
	}


	/**
	 *  Set state to "Inactive" for NDR Object corresponding to handle.<p>
	 *
	 *
	 *
	 * @param  handle         handle of object to activate
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public static void deactivateObject(String handle) throws Exception {
		NdrObjectReader reader = new NdrObjectReader(handle);
		if (reader == null)
			throw new Exception("object not found for " + handle);
		setObjectState(reader, NDRConstants.ObjectState.INACTIVE);
	}


	/**
	 *  Set state to "Active" for NDR Object corresponding to handle.<p>
	 *
	 *  NOTE: will not activate an object with an NCS status that is not final.
	 *
	 * @param  handle         handle of object to activate
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public static void activateObject(String handle) throws Exception {
		NdrObjectReader reader = new NdrObjectReader(handle);
		if (reader == null)
			throw new Exception("object not found for " + handle);
		String ncsStatus = getNonEmpyStringOrNull(reader.getProperty("status"));
		// if there is a status, we can only activate if it is final
		if (ncsStatus == null || ncsStatus.equals(NDRConstants.NCS_FINAL_STATUS))
			setObjectState(reader, NDRConstants.ObjectState.ACTIVE);
	}


	/**
	 *  Sets the state of the NDR Object corresponding to provided handle.
	 *
	 * @param  handle         handle for NDR Object
	 * @param  state          The new objectState value
	 * @exception  Exception  if NDR Object not found for handle
	 */
	public static void setObjectState(String handle, NDRConstants.ObjectState state) throws Exception {
		NdrObjectReader reader = new NdrObjectReader(handle);
		if (reader == null)
			throw new Exception("object not found for " + handle);
		setObjectState(reader, state);
	}


	/**
	 *  Sets the objectState attribute of the NdrUtils class
	 *
	 * @param  reader         NDR reader for object for which state will be set
	 * @param  state          The new objectState value
	 * @exception  Exception
	 */
	public static void setObjectState(NdrObjectReader reader, NDRConstants.ObjectState state) throws Exception {
		NDRConstants.NDRObjectType type = reader.getObjectType();
		String verb = "modify" + type.getNdrResponseType();
		NdrRequest request = new SignedNdrRequest(verb, reader.getHandle());
		request.setObjectType(type);
		request.addQualifiedCommand(NDRConstants.FEDORA_MODEL_NAMESPACE, "property", "state", state.toString());
		// request.report();
		request.submit();

	}


	/**
	 *  Gets the "link" value that goes into the nsdl_dc_info stream for metadata
	 *  records
	 *
	 * @param  mdHandle       NOT YET DOCUMENTED
	 * @return                The infoLink value
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public static String getInfoLink(String mdHandle) throws Exception {

		MetadataReader md = new MetadataReader(mdHandle);
		String mdpHandle = md.getRelationship("metadataProvidedBy");

		MetadataProviderReader mdp = new MetadataProviderReader(mdpHandle);
		String aggHandle = mdp.getRelationship("aggregatedBy");

		FindRequest request = new FindRequest(NDRConstants.NDRObjectType.METADATA);
		request.addCommand("relationship", "metadataFor", aggHandle);
		String aggMetadataHandle = request.getResultHandle();
		prtln("Super Collection Metadata handle: " + aggMetadataHandle);
		MetadataReader aggMetadata = new MetadataReader(aggMetadataHandle);

		// finally, grab the itemId from the aggregator's MD
		return aggMetadata.getProperty("itemId");
	}


	/**
	 *  Return the handle of the resource for the given url if one is found, null
	 *  otherwise.
	 *
	 * @param  url  url for which object will be found
	 * @return      NOT YET DOCUMENTED
	 */
	public static String findResource(String url) {
		FindResourceRequest request = new FindResourceRequest(url);
		try {
			return request.getResultHandle();
		} catch (Exception e) {
			prtln("findResource error: " + e.getMessage());
		}
		return null;
	}


	/**
	 *  Returns deleted metadata objects for specified metadataProvider.
	 *
	 * @param  mdpHandle  handle for a metadataProvider object
	 * @return            list of metadata object handles
	 */
	public static List findDeletedMetadataObjects(String mdpHandle) {
		FindRequest request = new FindRequest(NDRConstants.NDRObjectType.METADATA);

		// specify a metadataProvider
		request.addCommand("relationship", "metadataProvidedBy", mdpHandle);

		// Specify DELETED State
		request.addQualifiedCommand(NDRConstants.FEDORA_MODEL_NAMESPACE, "property", "state", "Deleted");
		List records = new ArrayList();
		try {
			return request.getResultHandles();
		} catch (Exception e) {
			prtln("WARNING: findDeletedMetadataObjects unable to complete for " + mdpHandle);
			prtln(e.getMessage());
		}
		return records;
	}


	/**
	 *  Gets the metadataProvider stored in the NDR associated with given
	 *  collection key.
	 *
	 * @param  collection     collection key
	 * @return                metadataProviderReader instance for given collection
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public static MetadataProviderReader getMetadataProvider(String collection) throws Exception {
		FindRequest request = new FindRequest(NDRObjectType.METADATAPROVIDER);
		// request.setRequestAgent(NDRConstants.getNcsAgent());
		request.addNcsPropertyCmd("collectionId", collection);

		// request.addCommand("relationship", "metadataProviderFor", NDRConstants.getNcsAgent());
		InfoXML response = request.submit();
		if (response.hasErrors())
			throw new Exception(response.getError());
		String mdpHandle = response.getHandle();
		if (mdpHandle == null)
			throw new Exception("MetadataProvider not found for collectionID: \"" + collection + "\"");

		MetadataProviderReader reader = new MetadataProviderReader(mdpHandle);
		if (reader == null)
			throw new Exception("unable to obtain MetadataProviderReader");
		return reader;
	}


	/**
	 *  Retrieves the CollectionRecord stored in the NDR for the given collection
	 *  key.
	 *
	 * @param  collection     collectionKey associated with a collection stored in
	 *      the NDR.
	 * @return                CollectionRecord (dlese_collect format)
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public static Document getCollectionRecord(String collection) throws Exception {
		MetadataProviderReader reader = getMetadataProvider(collection);
		Element collectionRecordRoot = reader.getCollectionRecord();
		if (collectionRecordRoot == null)
			throw new Exception("unable to obtain collection Record element from metadataProvider");
		return DocumentHelper.createDocument(collectionRecordRoot);
	}


	/**
	 *  Performs a "get" request on the NDR with the provided object handle and
	 *  returns the result as a dom4j.Document.
	 *
	 * @param  handle         handle to an ndrObject
	 * @return                NDR response representing object for handle
	 * @exception  Exception  if handle is not provided, or url does not resolve to
	 *      well-formed xml.
	 */
	public static Document getNDRObjectDoc(String handle) throws Exception {
		if (handle == null || handle.trim().length() == 0)
			throw new Exception("getNDRObjectDoc requires handle");
		String command = "get";
		URL url = new URL(NDRConstants.getNdrApiBaseUrl() + "/" + command + "/" + handle);
		Document doc = getNDRObjectDoc(url);
		return doc;
	}


	/**
	 *  Retrieves the provided "handleUrl" (a complete URL including ndrApi and a
	 *  handle to a specific object) as a dom4j.Document.<p>
	 *
	 *
	 *
	 * @param  handleUrl      NOT YET DOCUMENTED
	 * @return                Response from NDR
	 * @exception  Exception  If url does not resolve to well-formed xml.
	 */
	public static Document getNDRObjectDoc(URL handleUrl) throws Exception {

		// timed connection
		int millis = NDRConstants.NDR_CONNECTION_TIMEOUT;
		String content = null;
		try {
			content = TimedURLConnection.importURL(handleUrl.toString(), millis);
		} catch (URLConnectionTimedOutException e) {
			prtln("connection timed out: " + e);
			throw new Exception("Connection to " + handleUrl + " timed out after " + (millis / 1000) + " seconds");
		}
		catch (IOException t) {
			throw new Exception ("TimedURLConnection error: " + t.getMessage());
		}
		return Dom4jUtils.getXmlDocument(content);
	}


	/**
	 *  Gets all MetadataProvider handles associated with the DleseAgent
	 *
	 * @return                The mDPHandles value
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public static List getMDPHandles() throws Exception {
		return getMDPHandles(NDRConstants.getNcsAgent());
	}


	/**
	 *  Gets the handles for all MetadataProvider objects associated with the
	 *  aggent associated with specified agentHandle (which defaults to the
	 *  dleseAgent).<p>
	 *
	 *  Relies on "Find" request, which requires authentication and therefore uses
	 *  NDRAPIProxy.
	 *
	 * @param  agentHandle    agent for which we are retrieving mdpHandles
	 * @return                The mDPHandles associated with this agent
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public static List getMDPHandles(String agentHandle) throws Exception {

		FindRequest request = new FindRequest();
		request.setObjectType(NDRConstants.NDRObjectType.METADATAPROVIDER);
		request.addCommand("relationship", "metadataProviderFor", agentHandle);
		return request.getResultHandles();
	}


	/**
	 *  Gets the aggregatorHandles attribute of the NdrUtils class
	 *
	 * @return                The aggregatorHandles value
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public static List getAggregatorHandles() throws Exception {
		return getAggregatorHandles(NDRConstants.getNcsAgent());
	}


	/**
	 *  Gets the aggregatorHandles attribute of the NdrUtils class
	 *
	 * @param  agentHandle    NOT YET DOCUMENTED
	 * @return                The aggregatorHandles value
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public static List getAggregatorHandles(String agentHandle) throws Exception {
		FindRequest request = new FindRequest();
		request.setObjectType(NDRConstants.NDRObjectType.AGGREGATOR);
		request.addCommand("relationship", "aggregatorFor", agentHandle);
		return request.getResultHandles();
	}



	/**
	 *  Sets TransformerFactory to a XSL 1.0 version so the localizers don't
	 *  complain.
	 *
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public static void initProp() throws Exception {
		// System.setProperty("javax.xml.transform.TransformerFactory",
		// "com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl");
	}


	/**
	 *  The main program for the NdrUtils class
	 *
	 * @param  args           The command line arguments
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public static void main(String[] args) throws Exception {

		SimpleNdrRequest.setVerbose(true);
	 	SimpleNdrRequest.setDebug (true);		
		
		// configure to communicate with NDR
		String path = "C:/Documents and Settings/ostwald/devel/ndrServerProps/ndr.test.properties";
		NdrUtils.setup(new File(path));
		prtln("ndrApiBaseUrl = " + NDRConstants.getNdrApiBaseUrl());

		// do something ...
	}


	/**
	 *  Gets List of handles for MetadataProviders having no items.
	 *
	 * @return                The emtpyMDPHandles value
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public static List getEmtpyMDPHandles() throws Exception {
		List handles = getMDPHandles();
		List emptyHandles = new ArrayList();
		for (Iterator i = handles.iterator(); i.hasNext(); ) {
			String mdpHandle = (String) i.next();
			// prtln ("proccessing " + mdpHandle);
			System.out.print(".");
			try {
				MetadataProviderReader mdp = new MetadataProviderReader(mdpHandle);
				List items = mdp.getItemHandles();
				// prtln ("\t" + items.size() + " items");
				if (items.size() == 0)
					emptyHandles.add(mdpHandle);
			} catch (Exception re) {
				prtln("reader error: " + re.getMessage());
			}
		}
		return emptyHandles;
	}


	/**
	 *  Gets handles of MetadataProvider Objects in the NDR that have been modified
	 *  since specified threshold date.
	 *
	 * @param  thresholdDate  NOT YET DOCUMENTED
	 * @return                The recentMDPHandles value
	 */
	public static List getRecentMDPHandles(Date thresholdDate) {
		List mdpList = new ArrayList();
		try {

			List handles = getMDPHandles();

			prtln(handles.size() + " handles found");
			int counter = 0;
			int max = 1000;
			prtln("threshold date: " + formattedDate(thresholdDate));
			for (Iterator i = handles.iterator(); i.hasNext(); ) {
				String mdpHandle = (String) i.next();
				try {
					MetadataProviderReader mdp = new MetadataProviderReader(mdpHandle);
					prtln("\t lastModified: " + formattedDate(mdp.getLastModified()));
					if (mdp.getLastModified().after(thresholdDate))
						mdpList.add(mdp);
				} catch (Exception re) {
					prtln("reader error: " + re.getMessage());
				}
				if (counter++ > max)
					break;
			}
		} catch (Exception e) {
			prtln("ERROR getting recent MDP handles: " + e.getMessage());
		}

		prtln(mdpList.size() + " metadataproviders found");
		return mdpList;
	}



	/**
	 *  Gets List of MDP handles that have not been modified since provided
	 *  threshold Date.
	 *
	 * @param  thresholdDate  NOT YET DOCUMENTED
	 * @return                The staleMDPHandles value
	 */
	public static List getStaleMDPHandles(Date thresholdDate) {
		List mdpList = new ArrayList();
		try {

			List handles = getMDPHandles();

			prtln(handles.size() + " handles found");
			int counter = 0;
			int max = 1000;
			prtln("\nGetting stale MDP handles");
			prtln("threshold date: " + formattedDate(thresholdDate));
			for (Iterator i = handles.iterator(); i.hasNext(); ) {
				String mdpHandle = (String) i.next();
				try {
					MetadataProviderReader mdp = new MetadataProviderReader(mdpHandle);
					System.out.print(".");
					// prtln ("\t lastModified: " + formattedDate (mdp.getLastModified()));
					if (mdp.getLastModified().before(thresholdDate))
						mdpList.add(mdp);
				} catch (Exception re) {
					prtln("reader error: " + re.getMessage());
				}
				if (counter++ > max)
					break;
			}
		} catch (Exception e) {
			prtln("ERROR getting recent MDP handles: " + e.getMessage());
		}

		prtln(mdpList.size() + " metadataproviders found");
		return mdpList;
	}


	/**
	 *  Marks the object corresponding to the provided handle as "deleted" in the
	 *  NDR.
	 *
	 * @param  handle         handle of Object to be deleted.
	 * @return                response object as InfoXML instance
	 * @exception  Exception  if NDR object cannot be deleted
	 */
	public static InfoXML deleteNDRObject(String handle) throws Exception {
		return deleteNDRObject(handle, false);
	}


	/**
	 *  Marks the object corresponding to the provided handle, as well as all
	 *  subordinate objects (when cascade is true), as "deleted" in the NDR.
	 *
	 * @param  handle         handle of Object to be deleted
	 * @param  cascade        flag to delete all dependent objects (not working in
	 *      NDR)
	 * @return                response object as InfoXML instance
	 * @exception  Exception  if NDR object cannot be deleted
	 */
	public static InfoXML deleteNDRObject(String handle, boolean cascade) throws Exception {
		DeleteRequest request = new DeleteRequest(handle, cascade);
		InfoXML proxyResponse = request.submit();
		if (proxyResponse.hasErrors())
			throw new Exception(proxyResponse.getError());
		return proxyResponse;
	}


	/**
	 *  Removes the metadataProvider and all Metadata Objects associated with given
	 *  key from the NDR.
	 *
	 * @param  collection     collectionKey
	 * @exception  Exception  if NDR objects cannot be deleted
	 */
	public static void deleteCollection(String collection) throws Exception {
		MetadataProviderReader mdpReader = getMetadataProvider(collection);
		deleteNDRCollection(mdpReader.getHandle());
	}


	/**
	 *  Removes the metadataProvider, all Metadata Objects, and the aggregator
	 *  associated with the given mdpHandle from the NDR.
	 *
	 * @param  mdpHandle      MetadataProvider object handle.
	 * @exception  Exception  if NDR objects cannot be deleted
	 */
	public static void deleteNDRCollection(String mdpHandle) throws Exception {
		MetadataProviderReader mdp = new MetadataProviderReader(mdpHandle);

		try {
			if (mdp.getObjectType() != NDRObjectType.METADATAPROVIDER)
				throw new Exception("not a metadataProvider handle: " + mdpHandle);

			// String aggregatorHandle = mdp.getRelationship("aggregatorFor");
			String aggregatorHandle = mdp.getAggregatedBy();
			/* 			if (aggregatorHandle != null && aggregatorHandle.trim().length() > 0)
				deleteNDRObject(aggregatorHandle); */
			if (aggregatorHandle != null) {
				String errorMsg = null;
				InfoXML response = null;
				try {
					prtln("about to delete aggregator");
					response = deleteNDRObject(aggregatorHandle);
				} catch (Exception e) {
					errorMsg = "trouble deleting aggregator (" + aggregatorHandle + "): " + e.getMessage();
				}
				if (response != null && response.hasErrors()) {
					errorMsg = "trouble deleting aggregator (" + aggregatorHandle + "): " + response.getError();
				}
				if (errorMsg != null)
					throw new Exception(errorMsg);
			}

			/*
			// NOTE: cascade does not appear to be working!
			prtln ("deleting mdp with cascase = true");
			return deleteNDRObject (mdpHandle, true);
			*/
			// delete all metadata records
			for (Iterator i = mdp.getItemHandles().iterator(); i.hasNext(); ) {
				String mdHandle = (String) i.next();
				deleteNDRObject(mdHandle);
			}

			deleteNDRObject(mdpHandle);

			// delete the mdp
		} catch (Exception e) {
			throw new Exception("Collection could not be deleted: " + e.getMessage());
		}
	}


	/**  "yyyy-MM-dd'T'HH:mm:ss'Z'" */
	public static String ndrDateFormatString = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
	/**  Formats and parses dates according to utcDateFormatString. */
	public static SimpleDateFormat ndrDateFormat = new SimpleDateFormat(ndrDateFormatString);

	/**  NOT YET DOCUMENTED */
	public static String simpleDateFormatString = "yyyy-MM-dd";
	/**  NOT YET DOCUMENTED */
	public static SimpleDateFormat simpleDateFormat = new SimpleDateFormat(simpleDateFormatString);


	/**
	 *  Formats given date string using given dateFormat.
	 *
	 * @param  dateStr     Date in string form to be converted to a Date Object
	 * @param  dateFormat  NOT YET DOCUMENTED
	 * @return             Date object for dateStr, or Date corresponding to 0
	 *      milliseconds if the dateStr cannot be parsed.
	 */
	public static Date parseDateString(String dateStr, SimpleDateFormat dateFormat) {
		try {
			return dateFormat.parse(dateStr);
		} catch (ParseException pe) {
			prtln("could not parse date: " + pe.getMessage());
		}
		return new Date(0);
	}


	/**
	 *  Converts String of form used by NDR ("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'") into a
	 *  Date object.
	 *
	 * @param  dateStr
	 * @return          NOT YET DOCUMENTED
	 */
	public static Date parseNdrDateString(String dateStr) {
		return parseDateString(dateStr, ndrDateFormat);
	}


	/**
	 *  Converts date string of form "yyyy-MM-dd" to Date object.
	 *
	 * @param  dateStr  NOT YET DOCUMENTED
	 * @return          NOT YET DOCUMENTED
	 */
	public static Date parseSimpleDateString(String dateStr) {
		return parseDateString(dateStr, simpleDateFormat);
	}


	/**
	 *  Converts a Date object to String of form "yyyy-MM-dd".
	 *
	 * @param  date  NOT YET DOCUMENTED
	 * @return       NOT YET DOCUMENTED
	 */
	public static String formattedDate(Date date) {
		return simpleDateFormat.format(date);
	}


	/**
	 *  Pretty-prints a dom4j.Node.
	 *
	 * @param  node  NOT YET DOCUMENTED
	 */
	public static void pp(Node node) {
		prtln(Dom4jUtils.prettyPrint(node));
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  xml  NOT YET DOCUMENTED
	 */
	public static void pp(String xml) {
		if (xml == null || "".equals(xml)) {
			prtln("prettyPrint: no input supplied");
		}

		try {
			pp(DocumentHelper.parseText(xml));
		} catch (Throwable t) {
			prtln("ERROR: unable to process xml ..." + t.getMessage());
			prtln(xml);
		}
	}


	/**
	 *  Sets the debug attribute of the NdrUtils class
	 *
	 * @param  bool  The new debug value
	 */
	public static void setDebug(boolean bool) {
		debug = bool;
	}


	/**
	 *  Description of the Method
	 *
	 * @param  s  Description of the Parameter
	 */
	private static void prtln(String s) {
		if (debug) {
			System.out.println("NdrUtils: " + s);
		}
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  s       NOT YET DOCUMENTED
	 * @param  prefix  NOT YET DOCUMENTED
	 */
	public static void prtln(String s, String prefix) {

		while (s.length() > 0 && s.charAt(0) == '\n') {
			System.out.println("");
			s = s.substring(1);
		}

		if (prefix == null || prefix.trim().length() == 0)
			System.out.println(s);
		else
			System.out.println(prefix + ": " + s);
	}

}

