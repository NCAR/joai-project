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
package org.dlese.dpc.services.dcs.action;

import org.dlese.dpc.services.dcs.PutRecordData;
import org.dlese.dpc.services.dcs.action.form.DCSServicesForm;
import org.dlese.dpc.xml.*;
import org.dlese.dpc.xml.schema.DocMap;
import org.dlese.dpc.repository.RepositoryManager;
import org.dlese.dpc.repository.RecordUpdateException;
import org.dlese.dpc.index.reader.*;
import org.dlese.dpc.index.SimpleLuceneIndex;
import org.dlese.dpc.index.ResultDoc;
import org.dlese.dpc.index.ResultDocList;
import org.dlese.dpc.vocab.MetadataVocab;

import org.dlese.dpc.schemedit.*;
import org.dlese.dpc.schemedit.repository.RepositoryService;
import org.dlese.dpc.schemedit.config.IDGenerator;
import org.dlese.dpc.schemedit.threadedservices.ExportingService;
import org.dlese.dpc.schemedit.dcs.*;
import org.dlese.dpc.schemedit.config.*;

import java.util.*;
import java.text.*;
import java.io.*;

import javax.servlet.ServletContext;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServletResponse;
import org.apache.struts.action.Action;
import org.apache.struts.action.ActionError;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionServlet;
import org.apache.struts.util.MessageResources;

import org.dom4j.*;

/**
 *  An <strong>Action</strong> that handles DCS related web service requests.
 *
 * @author    Jonathan Ostwald
 */
public final class DCSServicesAction extends Action {
	private static boolean debug = false;

	private final static int MAX_SEARCH_RESULTS = 500;

	public final static String GET_ID_VERB = "GetId";
	public final static String PUT_RECORD_VERB = "PutRecord";
	public final static String UPDATE_STATUS_VERB = "UpdateStatus";
	public final static String EXPORT_COLLECTION_VERB = "ExportCollection";
	public final static String URL_CHECK_VERB = "UrlCheck";

	private FrameworkRegistry frameworkRegistry = null;
	private CollectionRegistry collectionRegistry = null;
	private RepositoryService repositoryService = null;


	// --------------------------------------------------------- Public Methods

	/**
	 *  Processes the DDS web service request by forwarding to the appropriate
	 *  corresponding JSP page for rendering.
	 *
	 * @param  mapping        The ActionMapping used to select this instance
	 * @param  request        The HTTP request we are processing
	 * @param  response       The HTTP response we are creating
	 * @param  form           The ActionForm for the given page
	 * @return                The ActionForward instance describing where and how
	 *      control should be forwarded
	 * @exception  Exception  If error.
	 */
	public ActionForward execute(
			ActionMapping mapping,
			ActionForm form,
			HttpServletRequest request,
			HttpServletResponse response)
			 throws Exception {
		/*
		 *  Design note:
		 *  Only one instance of this class gets created for the app and shared by
		 *  all threads. To be thread-safe, use only local variables, not instance
		 *  variables (the JVM will handle these properly using the stack). Pass
		 *  all variables via method signatures rather than instance vars.
		 */
		DCSServicesForm dcssf = null;
		try {

			dcssf = (DCSServicesForm) form;
			dcssf.setVocab( (MetadataVocab) servlet.getServletContext().getAttribute("MetadataVocab") );

			RepositoryManager rm =
					(RepositoryManager) servlet.getServletContext().getAttribute("repositoryManager");
					
			frameworkRegistry = (FrameworkRegistry) servlet.getServletContext().getAttribute("frameworkRegistry");
			if (frameworkRegistry == null)
				throw new ServletException ("attribute \"frameworkRegistry\" not found in servlet context");
					
			collectionRegistry = (CollectionRegistry) servlet.getServletContext().getAttribute("collectionRegistry");
			if (collectionRegistry == null)
				throw new ServletException ("attribute \"collectionRegistry\" not found in servlet context");
			
			repositoryService = (RepositoryService) servlet.getServletContext().getAttribute("repositoryService");
			if (repositoryService == null)
				throw new Exception ("attribute \"collectionRegistry\" not found in servlet context");

			
			// Grab the DDS service request verb:
			String verb = request.getParameter("verb");
			if (verb == null) {
				dcssf.setErrorMsg("The verb argument is required. Please indicate the request verb");
				return (mapping.findForward("dcsservices.error"));
			}

			// Handle get id request:
			else if (verb.equals(GET_ID_VERB)) {
				return doGetId(request, response, rm, dcssf, mapping);
			}
			// Handle put record request:
			else if (verb.equals(PUT_RECORD_VERB)) {
				return doPutRecord(request, response, rm, dcssf, mapping);
			}	
			// Handle update status request:
			else if (verb.equals(UPDATE_STATUS_VERB)) {
				return doUpdateStatus(request, response, rm, dcssf, mapping);
			}	
			// Handle export request:
			else if (verb.equals(EXPORT_COLLECTION_VERB)) {
				return doExportCollection(request, response, rm, dcssf, mapping);
			}
			// Handle export request:
			else if (verb.equals(URL_CHECK_VERB)) {
				return doUrlCheck(request, response, rm, dcssf, mapping);
			}
			// The verb is not valid for the DDS web service
			else {
				dcssf.setErrorMsg("The verb argument '" + verb + "' is not valid");
				return (mapping.findForward("dcsservices.error"));
			}
		} catch (NullPointerException npe) {
			prtlnErr("DCSServicesAction caught exception. " + npe);
			npe.printStackTrace();
			dcssf.setErrorMsg("There was an internal error by the server: " + npe);
			return (mapping.findForward("dcsservices.error"));
		} catch (Throwable e) {
			prtlnErr("DCSServicesAction caught exception. " + e);
			e.printStackTrace();
			dcssf.setErrorMsg("There was an internal error by the server: " + e);
			return (mapping.findForward("dcsservices.error"));
		}
	}



	/**
	 *  Handles a request to generate an id for a specified collection. <p>
	 *
	 *  Arguments: collection key.<p>
	 *
	 *  Error Exception Conditions: <br>
	 *  badArgument - The request includes illegal arguments.
	 *
	 * @param  request        The HTTP request
	 * @param  response       The HTTP response
	 * @param  rm             The RepositoryManager used
	 * @param  dcssf             The bean
	 * @param  mapping        ActionMapping used
	 * @return                An ActionForward to the JSP page that will handle the
	 *      response
	 * @exception  Exception  If error.
	 */
	protected ActionForward doGetId(
			HttpServletRequest request,
			HttpServletResponse response,
			RepositoryManager rm,
			DCSServicesForm dcssf,
			ActionMapping mapping)
			 throws Exception {

		boolean doAuthorize = false;
		String collectionParam = request.getParameter("collection");
		if (collectionParam == null || collectionParam.length() == 0) {
			dcssf.setErrorMsg("The collection argument is required but is missing or empty");
			return mapping.findForward("dcsservices.error");
		}

		if (doAuthorize) {
			if (isAuthorized(request, "trustedIp", rm)) {
				dcssf.setAuthorizedFor("trustedUser");
			}
			else {
				dcssf.setErrorMsg("You are not authorized to search over all records");
				return mapping.findForward("dcsservices.error");
			}
		}
		
		CollectionConfig collectionConfig = collectionRegistry.getCollectionConfig(collectionParam, false);
		if (collectionConfig == null) {
			dcssf.setErrorMsg("collection '" + collectionParam + "' not recognized");
			return mapping.findForward("dcsservices.error");
		}
		
		String id;
		try {
			id = collectionConfig.nextID();
		} catch (Throwable t) {
			if (t.getMessage() != null)
				dcssf.setErrorMsg("Unable to generate ID: " + t.getMessage());
			else
				dcssf.setErrorMsg("Unable to generate ID");
			return mapping.findForward("dcsservices.error");
		}
		

		dcssf.setId(id);
		return mapping.findForward("dcsservices.GetId");
	}

	protected ActionForward doUrlCheck(
			HttpServletRequest request,
			HttpServletResponse response,
			RepositoryManager rm,
			DCSServicesForm dcssf,
			ActionMapping mapping)
			 throws Exception {

		boolean doAuthorize = false;
		
		String url = request.getParameter("url");
		if (url == null || url.trim().length() == 0) {
			dcssf.setErrorMsg("You must supply a url parameter for the UrlCheck request.");
			return mapping.findForward("dcsservices.error");
		}
		url = url.trim();
		dcssf.setUrl(url);
	
		String [] collections = request.getParameterValues("collection");
		dcssf.setCollections (collections);
	
		// search for the url in either the dcsurlenc or urlenc fields
 		String q = "(dcsurlenc:" + SimpleLuceneIndex.encodeToTerm(url, false);
		q += " OR urlenc:" + SimpleLuceneIndex.encodeToTerm(url, false) + ")";
		
		if (collections != null && collections.length > 0) {
			q += " AND (collection:0"+collections[0];
			for (int i=1;i<collections.length;i++) {
				q += " OR collection:0" + collections[i];
			}
			q += ")";
		}

		SimpleLuceneIndex index = rm.getIndex();
		ResultDocList resultDocs = index.searchDocs(q);
		
		prtln("doUrlCheck() search for: '" + q + "' had " + (resultDocs == null ? -1 : resultDocs.size()) + " resultDocs");

		if (resultDocs == null || resultDocs.size() == 0) {
			dcssf.setResults(null);
		}
		else {
			dcssf.setResults(resultDocs);
		}

		return (mapping.findForward("dcsservices.UrlCheck"));

	}
		
	/**
	 *  Handles a request to put a metadata record into the repository. Wraps
	 {@link org.dlese.dpc.repository.RepositoryManager}.putRecord and therefore requires
	 the same arguments.<p>
	 Currently allows existing records to be overwritten. <p>
	 *
	 *  Arguments: recordXml, xmlFormat, collection, and id.<p>
	 *
	 *  Error Exception Conditions: <br>
	 *  badArgument - The request includes illegal arguments.
	 *
	 * @param  request        The HTTP request
	 * @param  response       The HTTP response
	 * @param  rm             The RepositoryManager used
	 * @param  dcssf             The Form bean
	 * @param  mapping        ActionMapping used
	 * @return                An ActionForward to the JSP page that will handle the
	 *      response
	 * @exception  Exception  If error.
	 */
	protected ActionForward doPutRecord(
			HttpServletRequest request,
			HttpServletResponse response,
			RepositoryManager rm,
			DCSServicesForm dcssf,
			ActionMapping mapping)
			 throws Exception {

		prtln ("doPutRecord()");				 
		boolean doAuthorize = true;
		
		// only trusted ips may perform put record
		if (doAuthorize) {
			if (isAuthorized(request, "trustedIp", rm)) {
				dcssf.setAuthorizedFor("trustedUser");
			}
			else {
				dcssf.setErrorMsg("You are not authorized to use the DCS PutRecord service");
				return mapping.findForward("dcsservices.error");
			}
		}

		List errorList = new ArrayList ();
		
		// validate presence of required arguments
		String recordXmlParam = request.getParameter("recordXml");
		if (recordXmlParam == null || recordXmlParam.length() == 0) {
			errorList.add ("The \'recordXml\' argument is required but is missing or empty");
		}
		String xmlFormatParam = request.getParameter("xmlFormat");
		if (xmlFormatParam == null || xmlFormatParam.length() == 0) {
			errorList.add ("The \'xmlFormat\' argument is required but is missing or empty");
		}
		String collectionParam = request.getParameter("collection");
		if (collectionParam == null || collectionParam.length() == 0) {
			errorList.add ("The \'collection\' argument is required but is missing or empty");
		}		
		String idParam = request.getParameter("id");
		if (idParam == null || idParam.length() == 0) {
			errorList.add ("The \'id\' argument is required but is missing or empty");
		}

		String dcsStatus = StatusFlags.IMPORTED_STATUS;
		String statusParam = request.getParameter("dcsStatus");
		if (statusParam != null && statusParam.length() > 0) {
			dcsStatus = statusParam;
		}
		
		String dcsStatusNote = "imported via DCS PutRecord Service";
		String statusNoteParam = request.getParameter("dcsStatusNote");
		if (statusNoteParam != null && statusNoteParam.length() > 0) {
			dcsStatusNote = statusNoteParam;
		}
		
		
		if (errorList.size() > 0) {
			dcssf.setErrorList(errorList);
			return  mapping.findForward("dcsservices.error");
		}
		
		prtln ("params validated");
		
		PutRecordData prData = new PutRecordData();
		prData.init(recordXmlParam, xmlFormatParam, collectionParam, frameworkRegistry);
		
		// validate against the requirements of the index - NOT the metadata schema!
		errorList = validateRecordXml (prData);
		if (errorList.size() > 0) {
			dcssf.setErrorList(errorList);
			return  mapping.findForward("dcsservices.error");
		}
		
		// check to see if this record already exists. (Currently no action is taken).
		String fileName = prData.getId() + ".xml";
		try {
			XMLDocReader docReader = RepositoryService.getXMLDocReader(prData.getId(), rm);
			if (docReader != null) {
				fileName = docReader.getFileName();
				prtln ("file exists for this record: " + fileName);
			}
		} catch (Throwable t) {}
		
		
		// set up the dcsData record
		// validate the xml against the schema
		String isValid = "false";
		String vRpt = XMLValidator.validateString(recordXmlParam);
		prtln ("validation Report: " + vRpt);
		if (vRpt == null || vRpt.trim().length() == 0) {
			isValid = "true";
		}
		
		// prtln ("xml validated: isValid=" + isValid);
		
		/* DcsDataFileIndexingPlugin plugin; */
		DcsDataRecord dcsDataRecord;
		try {
			// set up a file indexing plugin
			DcsDataManager dcsDataManager = (DcsDataManager)servlet.getServletContext().getAttribute("dcsDataManager");
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
			String timeStamp = sdf.format(new Date());
			
			// WARNING: might not be safe to assume the file name will be same as id!!
			
			// 8/18 - adding id to the param list. why wasn't this done originally?
			dcsDataRecord = dcsDataManager.getDcsDataRecord(collectionParam, xmlFormatParam, fileName, prData.getId());
			dcsDataRecord.setLastTouchDate (timeStamp);
			dcsDataRecord.setValidationReport(vRpt);
			
			// create a status entry
			dcsDataRecord.updateStatus (dcsStatus, dcsStatusNote, Constants.UNKNOWN_EDITOR);
			
			prtln ("status record for imported record");
			prtln (Dom4jUtils.prettyPrint (dcsDataRecord.getDocument()));
			
			dcsDataRecord.flushToDisk();
		} catch (Exception e) {
			dcssf.setErrorMsg("error creating DcsDataRecord: " + e.getMessage());
			return mapping.findForward("dcsservices.error");
		}
			
		try {
			rm.putRecord(recordXmlParam, xmlFormatParam, collectionParam, idParam, true);
			dcssf.setXmlFormat(xmlFormatParam);
			dcssf.setCollection(collectionParam);
			dcssf.setId(idParam);
			
			/*
				ISSUE - do we want to connect annotations to the records they
				annotate at this point? if we are saving an annotation record,
				then we have to call putRecord for the annotated records that
				are found in the current DCS index.
				THE FOLLOWING HAS NOT BEEN TESTED
			*/
			
			if (xmlFormatParam.equals("dlese_anno")) {
				prtln ("indexing annotated record ...");
				try {
					RepositoryService repositoryService = 
						(RepositoryService) servlet.getServletContext().getAttribute("repositoryService");
					if (repositoryService == null)
						throw new Exception ("repositoryService not found in servlet context");
					Document doc = DocumentHelper.parseText(recordXmlParam);
					doc = Dom4jUtils.localizeXml(doc);
					repositoryService.indexAnnotatedRecord(new DocMap (doc));
				} catch (Exception e) {
					prtlnErr ("doPutRecord error indexing annotated record: " + e.getMessage());
				}
			}
			
			
			prtln ("putRecord has succeeded and is returning");
			return mapping.findForward("dcsservices.PutRecord");
		} catch (RecordUpdateException e) {
			dcssf.setErrorMsg(e.getMessage());
			return mapping.findForward("dcsservices.error");
		}
	}

	/**
	 *  Handles a request to put a metadata record into the repository. Wraps
	 {@link org.dlese.dpc.repository.RepositoryManager}.putRecord and therefore requires
	 the same arguments.<p>
	 Currently allows existing records to be overwritten. <p>
	 *
	 *  Arguments: id, dcsStatus, dcsStatusNode (optional), and dcsStatusEditor(optional).<p>
	 *
	 *  Error Exception Conditions: <br>
	 *  badArgument - The request includes illegal or missing arguments.
	 *
	 * @param  request        The HTTP request
	 * @param  response       The HTTP response
	 * @param  rm             The RepositoryManager used
	 * @param  dcssf             The Form bean
	 * @param  mapping        ActionMapping used
	 * @return                An ActionForward to the JSP page that will handle the
	 *      response
	 * @exception  Exception  If error.
	 */
	protected ActionForward doUpdateStatus(
			HttpServletRequest request,
			HttpServletResponse response,
			RepositoryManager rm,
			DCSServicesForm dcssf,
			ActionMapping mapping)
			 throws Exception {

		prtln ("doUpdateStatus()");				 
		boolean doAuthorize = true;
		
		// only trusted ips may perform put record
		if (doAuthorize) {
			if (isAuthorized(request, "trustedIp", rm)) {
				dcssf.setAuthorizedFor("trustedUser");
			}
			else {
				dcssf.setErrorMsg("You are not authorized to use the DCS PutRecord service");
				return mapping.findForward("dcsservices.error");
			}
		}

		List errorList = new ArrayList ();
		
		// validate presence of required arguments
		
		String idParam = request.getParameter("id");
		if (idParam == null || idParam.length() == 0) {
			errorList.add ("The \'id\' argument is required but is missing or empty");
		}
		
		// check to see that the record exists
		try {
			repositoryService.getXMLDocReader(idParam);
		} catch (Exception e) {
			errorList.add ("Record not found for id=\'" + idParam + "\'");
		}

		String dcsStatus = null;
		String statusParam = request.getParameter("dcsStatus");
		if (statusParam != null && statusParam.trim().length() > 0) {
			dcsStatus = statusParam.trim();
		}
		else {
			errorList.add ("The \'dcsStatus\' argument is required but is missing or empty");
		}
		
		// statusNote is optional
		String dcsStatusNote = "";
		String statusNoteParam = request.getParameter("dcsStatusNote");
		if (statusNoteParam != null && statusNoteParam.length() > 0) {
			dcsStatusNote = statusNoteParam;
		}
	
		// statusEditor is optional
		String dcsStatusEditor = Constants.UNKNOWN_EDITOR;
		String statusEditorParam = request.getParameter("dcsStatusEditor");
		if (statusEditorParam != null && statusEditorParam.length() > 0) {
			dcsStatusEditor = statusEditorParam;
		}
		
		if (errorList.size() > 0) {
			dcssf.setErrorList(errorList);
			return  mapping.findForward("dcsservices.error");
		}
		
		prtln ("params validated");
		
		// make a status entry
		StatusEntry statusEntry = new StatusEntry (dcsStatus, dcsStatusNote, dcsStatusEditor);
		
		try {
			repositoryService.updateRecordStatus(idParam, statusEntry);
			dcssf.setStatusEntry(statusEntry.getElement().asXML());
			prtln ("doUpdateStatus has succeeded and is returning");
			return mapping.findForward("dcsservices.UpdateStatus");
		} catch (RecordUpdateException e) {
			dcssf.setErrorMsg(e.getMessage());
			return mapping.findForward("dcsservices.error");
		}
	}

	
	protected ActionForward doExportCollection(
			HttpServletRequest request,
			HttpServletResponse response,
			RepositoryManager rm,
			DCSServicesForm dcssf,
			ActionMapping mapping)
			 throws Exception {

		prtln ("doExportCollection()");				 
		boolean doAuthorize = true;
		
		// only trusted ips may perform put record
		if (doAuthorize) {
			if (isAuthorized(request, "trustedIp", rm)) {
				dcssf.setAuthorizedFor("trustedUser");
			}
			else {
				dcssf.setErrorMsg("You are not authorized to use the DCS ExportCollection service");
				return mapping.findForward("dcsservices.error");
			}
		}

		CollectionRegistry collectionRegistry = 
			(CollectionRegistry) servlet.getServletContext().getAttribute("collectionRegistry");
		if (collectionRegistry == null) {
			dcssf.setErrorMsg("Server Error: \"collectionRegistry\" not found in servlet context");
			return mapping.findForward("dcsservices.error");
		}	
			
		ExportingService exportingService = (ExportingService) servlet.getServletContext().getAttribute ("exportingService");
		if (exportingService == null) {
			dcssf.setErrorMsg("Server Error: \"exportingService\" not found in servlet context");
			return mapping.findForward("dcsservices.error");
		}
		
		if (exportingService.getIsProcessing()) {
			String id = exportingService.getSessionId();
			DcsSetInfo set = exportingService.getExportingSetInfo();
			dcssf.setErrorMsg("Another session is currently performing an export. Please try again later.");
			return mapping.findForward("dcsservices.error");
		}
		
		
		List errorList = new ArrayList ();
		CollectionConfig collectionConfig = null;
		File destDir = null;
		
		// validate presence of required arguments
		String collectionParam = request.getParameter("collection");
		if (collectionParam == null || collectionParam.length() == 0) {
			errorList.add ("The \'collection\' argument is required but is missing or empty");
		}
		else {
			collectionConfig = collectionRegistry.getCollectionConfig(collectionParam, false);
			if (collectionConfig == null) {
				errorList.add ("No collection found for \"" + collectionParam + "\"");
			}
		}
		
		if (collectionConfig != null) {
			// exportDir is not required. it defaults to configured exportDir for this collection
			String relativeExportDest = collectionConfig.getExportDirectory();
			
			String exportDirParam = request.getParameter("exportDir");
			if (exportDirParam != null && exportDirParam.length() > 0) {
				prtln ("exportDirParam provided (" + exportDirParam + ") overriding default (" + relativeExportDest + ")");
				relativeExportDest = exportDirParam;
			}

			try {
				String exportBaseDir = exportingService.getExportBaseDir();
				destDir = ExportingService.validateExportDestination (exportBaseDir, relativeExportDest);
			} catch (Exception e) {
				errorList.add(e.getMessage());
			}
		}
		
		if (errorList.size() > 0) {
			dcssf.setErrorList(errorList);
			return  mapping.findForward("dcsservices.error");
		}
		
		prtln ("params validated");

		// status values are not required. The default is __final__
		String [] statusParams = request.getParameterValues("statuses");
		String [] statusValues = getStatusValues (statusParams, collectionConfig);
		
		DcsSetInfo setInfo = SchemEditUtils.getDcsSetInfo(collectionConfig.getId(), rm);

		prtln ("about to call exportingService.exportRecords()");
		prtln ("\t" + "destDir: " + destDir);
		prtln ("\t" + "statusValues");
		for (int i=0;i<statusValues.length;i++)
			prtln ("\t\t" + statusValues[i]);
		if (setInfo == null)
			prtln ("\t" + "setInfo is NULL!");
		
		dcssf.setExportDir (destDir.getAbsolutePath());
		dcssf.setStatusLabels(getStatusLabels (statusParams, collectionConfig));
		dcssf.setStatuses(statusParams);
		
		try {

			exportingService.exportRecords (destDir, setInfo, statusValues, null);
			
		} catch (Throwable t) {
			String errorMsg = "exportingService.exportRecords error: " + t.getMessage();
			prtlnErr (errorMsg);
			t.printStackTrace();
			throw new Exception (errorMsg);
		}

		return mapping.findForward("dcsservices.ExportCollection");
	}
	
	private List getStatusLabels (String [] statusParams, CollectionConfig collectionConfig) {
		List labels = new ArrayList ();
		String finalStatusValue = collectionConfig.getFinalStatusValue();
		if (statusParams == null)
			return labels;
		for (int i=0;i<statusParams.length;i++) {
			String value = statusParams[i];
			if (value.equals(finalStatusValue))
				value = collectionConfig.getFinalStatusLabel();
			if (!labels.contains(value))
				labels.add (value);
		}
		return labels;
	}
			
	private String [] getStatusValues (String[] statusParams, CollectionConfig collectionConfig) {
		String finalStatusValue = collectionConfig.getFinalStatusValue();
		if (statusParams == null) {
			String [] ret = {finalStatusValue};
			return ret;
		}
		
		String [] values = new String [statusParams.length];
		String finalStatusLabel = collectionConfig.getFinalStatusLabel();
		if (statusParams == null)
			return values;
		for (int i=0;i<statusParams.length;i++) {
			String label = statusParams[i];
			String value = label;
			if (label.equals(finalStatusLabel))
				value = collectionConfig.getFinalStatusValue();
			values[i] = value;
		}
		return values;
	}
	
	/**
	* Ensure that the record will not choke the index. Currently only suported for "adn" format.<p>
	Notes:
	<li>This level of validation may not be neccesary since repository manager performs the same checks
	<li>If we do want to perform this level of validation, we should be using required paths as defined in 
	framework-config.
	*/
	private List validateRecordXml (PutRecordData prData) throws Exception {
		// prtln ("validateRecordXml");
		List errorList = new ArrayList ();
		String id = prData.getId();
		String format = prData.getFormat();
		String prefix = collectionRegistry.getIdPrefix(prData.getCollection());
		// prtln ("id: " + id + ", prefix: " + prefix);
		if (!id.startsWith(prefix))
			errorList.add ("record ID (" + id + ") does not have the correct prefix (\"" + prefix + "\" is required)");
		
		// validate against individual frameworks - not yet implemented
		if (format.equals("adn"))
			errorList.addAll (validateAdn (prData));
		/* 
		else if (format.equals("dlese_collect"))
			errorList.addAll (validateDleseCollect (prData));
		else if (format.equals("news_opps"))
			errorList.addAll (validateNewsOpps(prData));
		else if (format.equals("dlese_anno"))
			errorList.addAll (validateDleseAnno (prData));
		else
			return errorList; */
		return errorList;
	}
	
	private List validateAdn (PutRecordData prData) throws Exception {
		// prtln ("validateAdn()");
		List errorList = new ArrayList ();

		return errorList;
	}
		
	
	/**
	 *  Checks for IP authorization
	 *
	 * @param  request       HTTP request
	 * @param  securityRole  Security role
	 * @param  rm            RepositoryManager
	 * @return               True if authorized, false otherwise.
	 */
	private boolean isAuthorized(HttpServletRequest request, String securityRole, RepositoryManager rm) {
		if (securityRole.equals("trustedIp")) {
			String[] trustedIps = rm.getTrustedWsIpsArray();
			if (trustedIps == null)
				return false;

			String IP = request.getRemoteAddr();
			for (int i = 0; i < trustedIps.length; i++) {
				if (IP.matches(trustedIps[i]))
					return true;
			}
			prtlnErr ("unauthorized IP: " + IP);
		}
		return false;
	}


			
	// --------------- Debug output ------------------

	/**
	 *  Return a string for the current time and date, sutiable for display in log
	 *  files and output to standout:
	 *
	 * @return    The dateStamp value
	 */
	protected final static String getDateStamp() {
		return
				new SimpleDateFormat("MMM d, yyyy h:mm:ss a zzz").format(new Date());
	}


	/**
	 *  Output a line of text to error out, with datestamp.
	 *
	 * @param  s  The text that will be output to error out.
	 */
	protected final void prtlnErr(String s) {
		System.err.println(getDateStamp() + " " + s);
	}



	/**
	 *  Output a line of text to standard out, with datestamp, if debug is set to
	 *  true.
	 *
	 * @param  s  The String that will be output.
	 */
	protected final void prtln(String s) {
		if (debug) {
			System.out.println("DCSServicesAction: " + s);
		}
	}


	/**
	 *  Sets the debug attribute of the object
	 *
	 * @param  db  The new debug value
	 */
	public static void setDebug(boolean db) {
		debug = db;
	}
}

