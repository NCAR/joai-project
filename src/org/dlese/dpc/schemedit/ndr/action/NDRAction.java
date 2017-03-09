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
package org.dlese.dpc.schemedit.ndr.action;

import org.dlese.dpc.schemedit.ndr.action.form.NDRForm;
import org.dlese.dpc.schemedit.action.DCSAction;
import org.dlese.dpc.repository.RepositoryManager;
import org.dlese.dpc.repository.SetInfo;
import org.dlese.dpc.index.SimpleFileIndexingObserver;
import org.dlese.dpc.index.reader.XMLDocReader;

import org.dlese.dpc.schemedit.repository.CollectionReaper;
import org.dlese.dpc.schemedit.repository.CollectionIndexingObserver;
import org.dlese.dpc.schemedit.MetaDataFramework;
import org.dlese.dpc.schemedit.SessionBean;
import org.dlese.dpc.schemedit.SchemEditUtils;
import org.dlese.dpc.schemedit.repository.RepositoryService;
import org.dlese.dpc.schemedit.Constants;
import org.dlese.dpc.schemedit.RecordList;
import org.dlese.dpc.schemedit.config.CollectionConfig;
import org.dlese.dpc.schemedit.dcs.DcsDataRecord;
import org.dlese.dpc.ndr.NdrUtils;
import org.dlese.dpc.ndr.apiproxy.NDRConstants;
import org.dlese.dpc.ndr.request.*;
import org.dlese.dpc.ndr.reader.AgentReader;
import org.dlese.dpc.ndr.reader.MetadataProviderReader;

import org.dlese.dpc.schemedit.ndr.SyncService;
import org.dlese.dpc.schemedit.ndr.SyncReport;
import org.dlese.dpc.schemedit.ndr.CollectionImporter;
import org.dlese.dpc.schemedit.ndr.MetaDataWrapperException;
import org.dlese.dpc.schemedit.ndr.writer.MetadataProviderWriter;
import org.dlese.dpc.xml.Dom4jUtils;
import org.dlese.dpc.xml.XMLValidator;
import org.dlese.dpc.util.TimedURLConnection;

import org.dlese.dpc.ndr.apiproxy.InfoXML;
import org.dlese.dpc.ndr.request.NdrRequest;

import java.util.*;
import java.io.*;
import java.net.URL;

import javax.servlet.*;
import javax.servlet.http.*;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionError;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionServlet;

import org.dom4j.Element;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;

import org.json.XML;
import org.json.JSONObject;

/**
 *  A Struts Action controlling interaction between the NDR and DCS.<p>
 *
 *
 * @author     Jonathan Ostwald <p>
 *
 *      $Id: NDRAction.java,v 1.30 2010/03/23 16:26:11 ostwald Exp $
 * @version    $Id: NDRAction.java,v 1.30 2010/03/23 16:26:11 ostwald Exp $
 */
public final class NDRAction extends DCSAction {

	private static boolean debug = true;

	private RepositoryManager rm;
	boolean ndrIsActive = false;
	SyncService syncService = null;

	// --------------------------------------------------------- Public Methods

	/**
	 *  Processes the specified HTTP request and creates the corresponding HTTP
	 *  response by forwarding to a JSP that will create it.
	 *
	 * @param  mapping               The ActionMapping used to select this instance
	 * @param  request               The HTTP request we are processing
	 * @param  response              The HTTP response we are creating
	 * @param  form                  The ActionForm for the given page
	 * @return                       The ActionForward instance describing where
	 *      and how control should be forwarded
	 * @exception  IOException       if an input/output error occurs
	 * @exception  ServletException  if a servlet exception occurs
	 */
	public ActionForward execute(
	                             ActionMapping mapping,
	                             ActionForm form,
	                             HttpServletRequest request,
	                             HttpServletResponse response)
		 throws IOException, ServletException {
		/*
		 *  Design note:
		 *  Only one instance of this class gets created for the app and shared by
		 *  all threads. To be thread-safe, use only local variables, not instance
		 *  variables (the JVM will handle these properly using the stack). Pass
		 *  all variables via method signatures rather than instance vars.
		 */
		ActionErrors errors = initializeFromContext(mapping, request);
		if (!errors.isEmpty()) {
			saveErrors(request, errors);
			return (mapping.findForward("error.page"));
		}
		NDRForm ndrForm = (NDRForm) form;

		String errorMsg = "";

		rm = repositoryManager;

		SchemEditUtils.showRequestParameters(request);
		
		syncService = getSyncService(getSessionBean(request));
		if (syncService != null) {
			// ndrForm.setIsSyncing(syncService.getIsProcessing());
			ndrForm.setSyncService(syncService);
		}
		
		ndrIsActive = (Boolean)servlet.getServletContext().getAttribute ("ndrServiceActive");
		if (ndrIsActive) {
			try {
				ndrForm.setAppAgent (new AgentReader (NDRConstants.getNcsAgent()));
			} catch (Throwable t) {
				prtln ("trouble setting app agent: " + t.getMessage());
			}
		}

		String param = request.getParameter("command");
		if (param == null)
			param = "manage";

		try {

			if (param.equals("sat")) {
				return mapping.findForward("sat.explorer");
			}

			if (param.equals("explore")) {

				return mapping.findForward("ndr.explorer");
			}

			if (param.equals("manage")) {
				if (request.getParameter("activate") != null) {
					servlet.getServletContext().setAttribute ("ndrServiceActive", true);
					errors.add("message", 
						new ActionError("generic.message", "NDR Service has been activated"));
				}
				else if (request.getParameter("deactivate") != null) {
					servlet.getServletContext().setAttribute ("ndrServiceActive", false);
					errors.add("message", 
						new ActionError("generic.message", "NDR Service has been deactivated"));
											
					// return before attempting to interact with NDR
					saveErrors(request, errors);
					return mapping.findForward("ndr.manage");
				}
				
				String verbose = request.getParameter ("verbose");
				if (verbose != null) {
					SimpleNdrRequest.setVerbose ("true".equals(verbose));
					SimpleNdrRequest.setDebug ("true".equals(verbose));
				}
				
				assignAuthorizedSetGroups(request, mapping, form);
				saveErrors(request, errors);
				return mapping.findForward("ndr.manage");
			}
			if (param.equals("browse")) {
				String agentHandle = request.getParameter("agent");
				if (agentHandle == null || agentHandle.trim().length() == 0)
					agentHandle = NDRConstants.getNcsAgent();
				
				AgentReader agentReader = new AgentReader (agentHandle);
				if (agentReader == null)
					throw new Exception ("ERROR: Agent reader not found for " + agentHandle);
				
				String objType = request.getParameter("type");
				objType = (objType == null ? "metadataProvider" : objType);
				if (objType != null) {
					if (objType.equals("metadataProvider")) {
						ndrForm.setBrowserHandles(agentReader.getMetadataProviders());
					}
					else if (objType.equals("aggregator")) {
						ndrForm.setBrowserHandles(agentReader.getAggregators());
					}
/* 					if (objType.equals("agent")) {
						ndrForm.setBrowserHandles (NdrUtils.getAgentHandles());
					} */
				}
				return mapping.findForward("ndr.browse");
			}
			
			if (param.equals ("validate")) {
				try {
					URL url = new URL (request.getParameter("uri"));
					String xml = 
						TimedURLConnection.importURL(url.toString(), NDRConstants.NDR_CONNECTION_TIMEOUT);
					if (xml == null || xml.trim().length() == 0)
						throw new Exception ("empty response from NDR server");
					String vMsg = XMLValidator.validateString(xml);
					prtln ("validation message: " + vMsg);
					ndrForm.setProxyResponse(vMsg == null ? "true" : "false");
					prtln ("proxyResponse: " + ndrForm.getProxyResponse());
					
					// show response xml
					/* Document doc = DocumentHelper.parseText(xml);
					prtln(Dom4jUtils.prettyPrint(doc)); */
					
				} catch (Exception e) {
					ndrForm.setProxyResponse(e.getMessage());
				}
				return mapping.findForward("proxy.response"); 
			}
			
			if (param.equals ("find")) {
				return mapping.findForward("ndr.finder");
			}
				
			if (param.equals("ndrProxy")) {
				return handleNdrProxy(mapping, form, request, response);
			}

			if (param.equals("ndrRecordCount")) {
				String collection = request.getParameter("collection");
				String count = "?";
				if (collection != null) {
					NdrRequest.setVerbose(false);
					try {
						CollectionConfig config = collectionRegistry.getCollectionConfig(collection);
						
						String mdpHandle = config.getMetadataProviderHandle();
						count = String.valueOf (new CountMembersRequest (mdpHandle).getCount());
						
					} catch (Throwable t) {}
					NdrRequest.setVerbose(true);
				}
				// prtln ("\t count: " + count);
				ndrForm.setProxyResponse(count);
				return mapping.findForward("proxy.response");
			}

			if (param.equals ("inactiveMembers")) {
				String collection = request.getParameter("collection");
				if (collection != null) {
					try {
						CollectionConfig config = collectionRegistry.getCollectionConfig(collection);
						NdrRequest.setVerbose(false);
						String mdpHandle = config.getMetadataProviderHandle();
						FindRequest findRequest = new FindRequest ();
						findRequest.setObjectType (NDRConstants.NDRObjectType.METADATA);
						findRequest.addCommand ("relationship", "metadataProvidedBy", mdpHandle);
						findRequest.addQualifiedCommand(NDRConstants.FEDORA_MODEL_NAMESPACE,
														"property",
														"state",
														NDRConstants.ObjectState.INACTIVE.toString());
						InfoXML findResponse = findRequest.submit();
						JSONObject json = XML.toJSONObject(findResponse.getResponse());
						ndrForm.setProxyResponse(json.toString());
					} catch (Throwable t) {
						t.printStackTrace();
					}
				}
				return mapping.findForward("proxy.response");
			}
						
			if (param.equals("proxy")) {
				return handleGeneralProxy (mapping, form, request, response);
			}

 			if (param.equals("import")) {
				// Disabled as of 12/5/07
				boolean importDisabled = true;
				if (importDisabled) {
					errors.add("error", new ActionError("generic.error", "Import function is disabled"));
					saveErrors(request, errors);
					return mapping.findForward("ndr.manage");
				}
				else {

					String mdpHandle = request.getParameter("mdpHandle");
					if (mdpHandle == null || mdpHandle.trim().length() == 0) {
						errorMsg = "MDP handle required to import collection from NDR";
						errors.add("error", new ActionError("generic.error", errorMsg));
						saveErrors(request, errors);
						return mapping.findForward("ndr.manage");
					}
	
					return handleImportNDRCollection(mdpHandle, mapping, form, request);
				}

			}

			if (param.equals("sync")) {
				String collectionPid = request.getParameter("pid");
				if (collectionPid == null || collectionPid.trim().length() == 0) {
					errorMsg = "Collection Pid required to sync collection to NDR";
					errors.add("error", new ActionError("generic.error", errorMsg));
					saveErrors(request, errors);
					return mapping.findForward("ndr.manage");
				}

				return handleSync(collectionPid, mapping, form, request);
			}

			if (param.equals("syncreport")) {
				ndrForm.setSyncReport(this.syncService.getSyncReport());
				return mapping.findForward("ndr.sync.report");
			}
			
			if (param.equals("syncprogress")) {
				ndrForm.setProgress(syncService.getTaskProgress().getProgressReport());
				return mapping.findForward ("ndr.sync.progress");
			}
			
			if (param.equals("writeCollectionInfo")) {
				String collection = request.getParameter("collection");
				if (collection == null || collection.trim().length() == 0) {
					errorMsg = "Collection id required to sync collection to NDR";
					errors.add("error", new ActionError("generic.error", errorMsg));
					saveErrors(request, errors);
					return mapping.findForward("ndr.manage");
				}

				return handleWriteCollectionInfo(collection, mapping, form, request);
			}

			if (param.equals("unregister")) {
				String collection = request.getParameter("collection");
				if (collection == null || collection.trim().length() == 0) {
					errorMsg = "Collection id required to unregister collection from NDR";
					errors.add("error", new ActionError("generic.error", errorMsg));
					saveErrors(request, errors);
					return mapping.findForward("ndr.manage");
				}

				return this.handleUnregisterCollection(collection, mapping, form, request);
			}

			if (param.equalsIgnoreCase("cancel")) {
				return mapping.findForward("ndr.manage");
			}
			errors.add("message",
				new ActionError("generic.message", "Unrecognized command submitted - no action taken"));
		} catch (NullPointerException e) {
			prtlnErr("NDRAction caught exception.");
			e.printStackTrace();
			errors.add("error",
				new ActionError("generic.error", "NDRAction caught exception"));
		} catch (Throwable e) {
			prtlnErr("NDRAction caught exception: " + e);
			errors.add("error",
				new ActionError("generic.error", e.getMessage()));
		}
		saveErrors(request, errors);
		return mapping.findForward("error.page");
	}


	/**
	 *  Intitialize collection-oriented data structures used by NDRForm for the
	 *  collections for which the sessionUser authorized.
	 *  <ul>
	 *    <li> ndrCollections - a list of SetInfos for all authorized collections
	 *    </li>
	 *    <li> mdpHandleMap - a mapping of metadataProvider handles keyed by
	 *    collection</li>
	 *    <li> dcsCollections - a list of SetInfos for DCS-managed collections</li>
	 *
	 *  </ul>
	 *
	 *
	 * @param  mapping  NOT YET DOCUMENTED
	 * @param  form     NOT YET DOCUMENTED
	 */
	private void assignAuthorizedSetGroups(HttpServletRequest request, ActionMapping mapping, ActionForm form) {
		NDRForm ndrForm = (NDRForm) form;
		List ndrSets = new ArrayList();
		Map ndrSetHandles = new HashMap();
		List dcsSets = new ArrayList();
		List authorizedSets =
			repositoryService.getAuthorizedSets(getSessionUser(request), this.requiredRole);
		for (Iterator i = authorizedSets.iterator(); i.hasNext(); ) {
			SetInfo setInfo = (SetInfo) i.next();
			CollectionConfig config = this.collectionRegistry.getCollectionConfig(setInfo.getSetSpec());
			if (config != null && config.isNDRCollection()) {
				ndrSets.add(setInfo);
				ndrSetHandles.put(setInfo.getSetSpec(), config.getMetadataProviderHandle());
			}
			else {
				dcsSets.add(setInfo);
			}
		}
		ndrForm.setNdrCollections(ndrSets);
		ndrForm.setMdpHandleMap(ndrSetHandles);
		ndrForm.setDcsCollections(dcsSets);
	}


	/**
	 * @param  mapping    NOT YET DOCUMENTED
	 * @param  form       NOT YET DOCUMENTED
	 * @param  request    NOT YET DOCUMENTED
	 * @param  mdpHandle  NOT YET DOCUMENTED
	 * @return            NOT YET DOCUMENTED
	 */
 	private ActionForward handleImportNDRCollection(String mdpHandle,
	                                                ActionMapping mapping,
	                                                ActionForm form,
	                                                HttpServletRequest request) {
		ActionErrors errors = new ActionErrors();

		String errorMsg = "";
		CollectionImporter importer = null;
		String collRecId;
		String collectionPid;

		CollectionConfig config = collectionRegistry.findCollectionByHandle(mdpHandle);
		if (config != null) {
			errors.add("error", new ActionError("ndr.import.collection.exists", mdpHandle, config.getId()));
			saveErrors(request, errors);
			return mapping.findForward("ndr.manage");
		}

		try {
			importer = new CollectionImporter(mdpHandle, servlet.getServletContext());

			Map importReport = importer.doImport();
			String collectionId = (String) importReport.get("collectionId");
			String collectionName = (String) importReport.get("collectionName");

			CollectionIndexingObserver observer = new CollectionIndexingObserver (collectionId, collectionRegistry, rm);
			if (repositoryManager.indexCollection(collectionId, observer, true)) {
			// if (rm.indexCollection(collectionId, new SimpleFileIndexingObserver("Collection indexer for '" + collectionName + "'", "Starting indexing"), true)) {
				errors.add("message", new ActionError("generic.message", "Collection '" + collectionName + "' imported from NDR."));
				errors.add("message", new ActionError("generic.message", "Files for collection '" + collectionName + "'are being indexed."));
				errors.add("message", new ActionError("generic.message", "Changes may take several several minutes to appear"));
				errors.add("showIndexMessagingLink", new ActionError("generic.message", ""));
			}
			else {
				errors.add("error", new ActionError("generic.error", "Collection '" + collectionName + "' is not configured in the repository. Unable to index files."));
			}
			saveErrors(request, errors);
			return mapping.findForward("ndr.confirm.load");
		} catch (Exception e) {
			// e.printStackTrace();
			errorMsg = "Import error: " + e.getMessage();
			errors.add("error",
				new ActionError("generic.error", errorMsg));
			saveErrors(request, errors);
			return mapping.findForward("ndr.manage");
		}
	}

	private ActionForward handleGeneralProxy(ActionMapping mapping,
	                                         ActionForm form,
	                                         HttpServletRequest request,
	                                         HttpServletResponse response) throws Exception {
		NDRForm ndrForm = (NDRForm) form;
		prtln("processing general proxy");
		ndrForm.setProxyResponse("");
		String uri = request.getParameter("uri");
		if (uri == null || uri.trim().length() == 0)
			throw new Exception("proxy handler did not receive a URI");

		prtln("\t uri: " + uri);
		String format = request.getParameter("format");
		if (format == null)
			format = "json";
		Document responseDoc = null;
		try {
			responseDoc = Dom4jUtils.getXmlDocument(new URL(uri));
			// prtln("\n RAW RESPONSE");
			// prtln (doc.asXML());
		} catch (Exception e) {
			String errMsg = "proxy response error: " + e.getMessage();
			prtlnErr(errMsg);
			// e.printStackTrace();
			Element error = DocumentHelper.createElement("ProxyErrorReport");
			error.setText(errMsg);
			responseDoc = DocumentHelper.createDocument(error);
		}

		if (format.equals("json")) {
			JSONObject json = XML.toJSONObject(responseDoc.asXML());
			// prtln(json.toString(2));
			ndrForm.setProxyResponse(json.toString());
		}
		else if (format.equals("xml")) {
			// prtln (Dom4jUtils.prettyPrint(responseDoc));
			ndrForm.setProxyResponse(responseDoc.asXML());
		}
		else {
			throw new Exception("unrecognized format: " + format);
		}
		
		return mapping.findForward("proxy.response");
	}
 
	/**
	 *  Execute an ndrRequest (constructed from request parameters) and store the
	 *  results in the Form as an XML document before forwarding to "proxy.response".
	 *
	 * @param  mapping        NOT YET DOCUMENTED
	 * @param  form           NOT YET DOCUMENTED
	 * @param  request        NOT YET DOCUMENTED
	 * @param  response       NOT YET DOCUMENTED
	 * @return                NOT YET DOCUMENTED
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	private ActionForward handleNdrProxy(ActionMapping mapping,
	                                     ActionForm form,
	                                     HttpServletRequest request,
	                                     HttpServletResponse response) throws Exception {
		NDRForm ndrForm = (NDRForm) form;

		String verb = request.getParameter("verb");
		if (verb == null || verb.trim().length() == 0)
			throw new Exception("no verb supplied");

		String handle = request.getParameter("handle");
		if (handle == null || handle.trim().length() == 0) {
			handle = null;
		}

		String inputXML = request.getParameter("inputXML");
		if (inputXML == null || inputXML.trim().length() == 0)
			throw new Exception("no inputXML supplied");

		// make sure the inputXML is well-formed
		Document doc = null;
		try {
			doc = DocumentHelper.parseText(inputXML);
			// prtln("\n===============\nproxyRequest\n" + inputXML);
		} catch (Throwable t) {
			prtlnErr("BAD inputXML: " + inputXML);
			throw new Exception("inputXML was not parsable XML");
		}

		NdrRequest ndrRequest = new NdrRequest();
		ndrRequest.setVerb(verb);
		ndrRequest.setHandle(handle);
		InfoXML proxyResponse = ndrRequest.submit(inputXML);

		doc = DocumentHelper.parseText(proxyResponse.getResponse());
		ndrForm.setProxyResponse(Dom4jUtils.prettyPrint(doc));
		return mapping.findForward("proxy.response");
	}


	/**
	 *  Write the collection info for specified collection to the NDR.
	 *
	 * @param  collection  collection key ("dcc")
	 * @param  mapping     NOT YET DOCUMENTED
	 * @param  form        NOT YET DOCUMENTED
	 * @param  request     NOT YET DOCUMENTED
	 * @return             NOT YET DOCUMENTED
	 */
	private ActionForward handleWriteCollectionInfo(String collection,
	                                                ActionMapping mapping,
	                                                ActionForm form,
	                                                HttpServletRequest request) {
		ActionErrors errors = new ActionErrors();

		NDRForm ndrForm = (NDRForm) form;
		String msg = "";

		// obtain locks on collection record and items
		SetInfo setInfo = SchemEditUtils.getSetInfo(collection, rm);
		if (setInfo == null) {
			errors.add("error", new ActionError("collection.not.found", collection));
			saveErrors(request, errors);
			return mapping.findForward("ndr.manage");
		}
		String id = setInfo.getId();

		if (!getSessionBean(request).getLock(id)) {
			errors.add("error", new ActionError("ndr.export.collection.busy", setInfo.getName(), collection));
			saveErrors(request, errors);
			return mapping.findForward("ndr.manage");
		}

		// write metadata record to NDR
		try {
			// String username = (sessionUser == null ? Constants.UNKNOWN_USER : sessionUser.getUsername());

			CollectionConfig config = collectionRegistry.getCollectionConfig(setInfo.getSetSpec());
			DcsDataRecord dcsDataRecord = dcsDataManager.getDcsDataRecord(setInfo.getId(), repositoryManager);
			MetadataProviderWriter writer = new MetadataProviderWriter(servlet.getServletContext());
			ndrForm.setSyncReport(writer.write(id, config, dcsDataRecord));
			// ndrForm.setCollectionReport(writer.write(id, config, dcsDataRecord));

			config.setAuthority("ndr");
			config.flush();

			/* this is the call we eventually want to make */
			// this.repositoryService.saveCollectionData (setInfo);

			msg = collection + " successfully registered to NDR";
			errors.add("message",
				new ActionError("ndr.export.success", setInfo.getName(), collection));
			saveErrors(request, errors);
			return mapping.findForward("ndr.sync.report");
		} catch (Exception e) {
			msg = "NDR registration error: " + e.getMessage();
			e.printStackTrace();
			errors.add("error",
				new ActionError("ndr.collect.update.error", setInfo.getName(), collection, e.getMessage()));
			saveErrors(request, errors);
			return mapping.findForward("ndr.sync.report");
		}
	}


	/**
	 *  Remove NDR Objects for this collection from the NDR, and remove all
	 *  references to the deleted objects from local item and collection-level
	 *  structures.<p>
	 *
	 *  NOTE: currently does not remove the following NDR objects:
	 *  <ul>
	 *    <li> Collection Resource
	 *    <li> Collection Metadata Object
	 *    <li> Collection Aggregator
	 *  </ul>
	 *
	 *
	 * @param  collection  NOT YET DOCUMENTED
	 * @param  mapping     NOT YET DOCUMENTED
	 * @param  form        NOT YET DOCUMENTED
	 * @param  request     NOT YET DOCUMENTED
	 * @return             NOT YET DOCUMENTED
	 */
	private ActionForward handleUnregisterCollection(String collection,
	                                                 ActionMapping mapping,
	                                                 ActionForm form,
	                                                 HttpServletRequest request) {
		ActionErrors errors = new ActionErrors();

		NDRForm ndrForm = (NDRForm) form;
		String msg = "";

		// obtain locks on collection record and items
		SetInfo setInfo = SchemEditUtils.getSetInfo(collection, rm);
		if (setInfo == null) {
			errors.add("error", new ActionError("collection.not.found", collection));
			saveErrors(request, errors);
			return mapping.findForward("ndr.manage");
		}
		String collectionRecordId = setInfo.getId();
		String collectionName = setInfo.getName();

		if (!getSessionBean(request).getLock(collectionRecordId)) {
			errors.add("error", new ActionError("ndr.export.collection.busy", collectionName, collection));
			saveErrors(request, errors);
			return mapping.findForward("ndr.manage");
		}

		try {

			CollectionConfig config = collectionRegistry.getCollectionConfig(collection);

			// Delete MDP, AGG and MD from NDR
			prtln("deletingNDRCollection");
			try {
				NdrUtils.deleteNDRCollection(config.getMetadataProviderHandle());
			} catch (Throwable t) {
				msg = "could not deleteNDRCollection: " + t.getMessage();
				prtlnErr ("WARNING: " + msg);
				errors.add("error",
					new ActionError("generic.error", msg));
			}

			// Clear NDR info from dcsDataRecord for collection items
			prtln("clearning ndr info from item records");
			RecordList items = repositoryService.getCollectionItemRecords(collection);
			for (Iterator i = items.iterator(); i.hasNext(); ) {
				String recId = (String) i.next();
				try {
					DcsDataRecord dcsDataRecord = this.dcsDataManager.getDcsDataRecord(recId, repositoryManager);
					dcsDataRecord.clearNdrInfo();
					// dcsDataRecord.clearSyncErrors();
					dcsDataRecord.flushToDisk();
				} catch (Throwable t) {
					prtlnErr("WARNING: could not unregister ndr info for " + recId);
				}
			}

			// Clear NDR from collection dcsDataRecord
			prtln("Clearing NDR from collection dcsDataRecord");
			DcsDataRecord collDcsDataRecord = this.dcsDataManager.getDcsDataRecord(collectionRecordId, repositoryManager);
			collDcsDataRecord.clearNdrInfo();
			// collDcsDataRecord.clearSyncErrors();
			collDcsDataRecord.flushToDisk();

			// Clear NDR from collection config
			prtln("Clearing NDR from collection config");
			config.clearNdrInfo();
			config.flush();
			
			CollectionIndexingObserver observer = new CollectionIndexingObserver (collection, collectionRegistry, rm);
			if (repositoryManager.indexCollection(collection, observer, true)) {
			// if (rm.indexCollection(collection, new SimpleFileIndexingObserver("Collection indexer for '" + collectionName + "'", "Starting indexing"), true)) {
				errors.add("message", new ActionError("generic.message", "Collection '" + collectionName + "' removed from NDR."));
				errors.add("message", new ActionError("generic.message", "Files for collection '" + collectionName + "' are being re-indexed."));
				errors.add("message", new ActionError("generic.message", "Changes may take several several minutes to appear"));
				errors.add("showIndexMessagingLink", new ActionError("generic.message", ""));
			}
			else {
				errors.add("error", new ActionError("generic.error", "Collection '" + collectionName + "' is not configured in the repository. Unable to index files."));
			}

			msg = collectionName + " unregistered from NDR";
			errors.add("message",
				new ActionError("generic.message", msg));
			saveErrors(request, errors);
			return mapping.findForward("ndr.manage");
		} catch (Throwable e) {
			msg = "NDR unregistration error: " + e.getMessage();
			e.printStackTrace();
			errors.add("error",
				new ActionError("ndr.unregister.error", collectionName, collection, e.getMessage()));
			saveErrors(request, errors);
			return mapping.findForward("ndr.manage");
		}
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  collection  NOT YET DOCUMENTED
	 * @param  mapping     NOT YET DOCUMENTED
	 * @param  form        NOT YET DOCUMENTED
	 * @param  request     NOT YET DOCUMENTED
	 * @return             NOT YET DOCUMENTED
	 */
	private ActionForward handleSync(String collection,
	                                 ActionMapping mapping,
	                                 ActionForm form,
	                                 HttpServletRequest request) {
		ActionErrors errors = new ActionErrors();

		NDRForm ndrForm = (NDRForm) form;
		String msg = "";

		// obtain locks on collection record and items
		SetInfo setInfo = SchemEditUtils.getSetInfo(collection, rm);
		if (setInfo == null) {
			errors.add("error", new ActionError("collection.not.found", collection));
			saveErrors(request, errors);
			return mapping.findForward("ndr.manage");
		}
		String id = setInfo.getId();
		SessionBean sessionBean = this.getSessionBean(request);
		if (!sessionBean.getLock(id)) {
			errors.add("error", new ActionError("ndr.export.collection.busy", setInfo.getName(), collection));
			saveErrors(request, errors);
			return mapping.findForward("ndr.manage");
		}

		RecordList recordBatch = repositoryService.getCollectionItemRecords(collection);
		if (!sessionBean.getBatchLocks(recordBatch)) {
			errors.add("error", new ActionError("batch.lock.not.obtained", "Export Collection to NDR"));
			saveErrors(request, errors);
			return mapping.findForward("ndr.sync.report");
		}

		// perform sync
		try {
			if (syncService.getIsProcessing())
				throw new Exception ("service is currently busy, try again later");
			syncService.sync(collection);

		} catch (Exception e) {
			msg = "NDR sync error: " + e.getMessage();
			e.printStackTrace();
			errors.add("error",
				// new ActionError("ndr.sync.error", setInfo.getName(), collection, e.getMessage()));
				new ActionError("generic.error", e.getMessage()));
		}
		// sessionBean.releaseAllLocks();
		saveErrors(request, errors);
		// return mapping.findForward("ndr.sync.report");
		return mapping.findForward("ndr.manage");
	}

	private SyncService getSyncService (SessionBean sessionBean) {
		SyncService syncService = sessionBean.getSyncService();
		if (syncService == null) {
			try {
				syncService = new SyncService (sessionBean, this.getServlet().getServletContext());
				sessionBean.setSyncService(syncService);
			} catch (Throwable t) {
				prtlnErr ("could not get SyncService from session: " + t.getMessage());
			}
		}
		return syncService;
	}
	// -------------- Debug ------------------

	/**
	 *  Sets the debug attribute of the NDRAction class
	 *
	 * @param  isDebugOutput  The new debug value
	 */
	public static void setDebug(boolean isDebugOutput) {
		debug = isDebugOutput;
	}


	/**
	 *  Print a line to standard out.
	 *
	 * @param  s  The String to print.
	 */
	private void prtln(String s) {
		if (debug) {
			SchemEditUtils.prtln(s, "NDRAction");
		}
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  s  NOT YET DOCUMENTED
	 */
	private void prtlnErr(String s) {
		SchemEditUtils.prtln(s, "NDRAction");
	}

}

