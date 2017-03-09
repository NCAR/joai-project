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
import org.dlese.dpc.services.dcs.action.form.RecommenderForm;
import org.dlese.dpc.dds.*;
import org.dlese.dpc.dds.action.*;
import org.dlese.dpc.xml.*;
import org.dlese.dpc.xml.schema.DocMap;
import org.dlese.dpc.repository.*;
import org.dlese.dpc.index.reader.*;
import org.dlese.dpc.index.*;
import org.dlese.dpc.util.*;
import org.dlese.dpc.index.writer.*;
import org.dlese.dpc.webapps.servlets.filters.GzipFilter;
import org.dlese.dpc.vocab.MetadataVocab;
import org.apache.lucene.search.*;

import org.dlese.dpc.schemedit.*;
import org.dlese.dpc.schemedit.action.DCSAction;
import org.dlese.dpc.schemedit.repository.RepositoryService;
import org.dlese.dpc.schemedit.config.IDGenerator;
import org.dlese.dpc.schemedit.threadedservices.ExportingService;
import org.dlese.dpc.schemedit.dcs.*;
import org.dlese.dpc.schemedit.config.*;

import java.util.*;
import java.text.*;
import java.io.*;
import java.net.URL;
import java.util.Hashtable;
import java.util.Locale;
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
 * @see       org.dlese.dpc.services.dcs.action.form.RecommenderForm
 */
public final class RecommenderAction extends DCSAction {
	private static boolean debug = true;

	public final static String RECOMMEND_RESOURCE = "RecommendResource";
	public final static String RECOMMEND_COLLECTION = "RecommendCollection";


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
		RecommenderForm recForm = null;
		try {

			recForm = (RecommenderForm) form;
			
			// how to notify of configuration errrors??
			ActionErrors errors = initializeFromContext (mapping, request);
			if (!errors.isEmpty()) {
				List errorList = new ArrayList ();
				for (Iterator i=errors.get();i.hasNext();) {
					ActionError err = (ActionError)i.next();
					prtln ("\t" + err.toString());
					errorList.add (err.toString());
				}
				recForm.setErrorList(errorList);
				prtln ("initializeFromContext errors (" + errors.size() + ")");
				logErrors (errorList);
				return  mapping.findForward("dcsservices.error");
			}
			
			// Grab the DDS service request verb:
			String verb = request.getParameter("verb");
			if (verb == null) {
				recForm.setErrorMsg("The verb argument is required. Please indicate the request verb");
				return (mapping.findForward("dcsservices.error"));
			}

			// Handle put record request:
			else if (verb.equals(RECOMMEND_RESOURCE)) {
				return doRecommendResource(request, response, recForm, mapping);
			}	
			// Handle export request:
 			else if (verb.equals(RECOMMEND_COLLECTION)) {
				return doRecommendCollection(request, response, recForm, mapping);
			}
			// The verb is not valid for the DDS web service
			else {
				recForm.setErrorMsg("The verb argument '" + verb + "' is not valid");
				return (mapping.findForward("dcsservices.error"));
			}
		} catch (NullPointerException npe) {
			prtln("RecommenderAction caught exception. " + npe);
			npe.printStackTrace();
			recForm.setErrorMsg("There was an internal error by the server: " + npe);
			return (mapping.findForward("dcsservices.error"));
		} catch (Throwable e) {
			prtln("RecommenderAction caught exception. " + e);
			e.printStackTrace();
			recForm.setErrorMsg("There was an internal error by the server: " + e);
			return (mapping.findForward("dcsservices.error"));
		}
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
	 * @param  recForm             The Form bean
	 * @param  mapping        ActionMapping used
	 * @return                An ActionForward to the JSP page that will handle the
	 *      response
	 * @exception  Exception  If error.
	 */
	protected ActionForward doRecommendResource(
			HttpServletRequest request,
			HttpServletResponse response,
			RecommenderForm recForm,
			ActionMapping mapping)
			 throws Exception {

		prtln ("doRecommendResource()");				 
		boolean doAuthorize = false;
		
		// only trusted ips may perform put record
		prtln ("authorizing");
		if (doAuthorize) {
			if (isAuthorized(request, "trustedIp")) {
				recForm.setAuthorizedFor("trustedUser");
			}
			else {
				recForm.setErrorMsg("You are not authorized to use the DCS Recommender service");
				return mapping.findForward("dcsservices.error");
			}
		}

		List errorList = new ArrayList ();
		prtln ("validating required arguments");
		
		// validate presence of required arguments
		String recordXmlParam = request.getParameter("recordXml");
		if (recordXmlParam == null || recordXmlParam.length() == 0) {
			errorList.add ("The \'recordXml\' argument is required but is missing or empty");
		}

		CollectionConfig collectionConfig = null;
		String collectionParam = request.getParameter("collection");
		if (collectionParam == null || collectionParam.length() == 0) {
			errorList.add ("The \'collection\' argument is required but is missing or empty");
		}
		else {
			collectionConfig = this.collectionRegistry.getCollectionConfig(collectionParam);
			if (collectionConfig == null) {
				errorList.add ("The specified collection, \"" + collectionParam + "\" does not exist");
			}
		}		

		if (errorList.size() > 0) {
			recForm.setErrorList(errorList);
			logErrors (errorList);
			return  mapping.findForward("dcsservices.error");
		}
		
		prtln ("required request params validated");
		
		String itemRecord = null;
		String recId = collectionConfig.nextID();
		String xmlFormat = collectionConfig.getXmlFormat();
		try {
			itemRecord = makeItemRecord (recordXmlParam, recId, collectionConfig);
		} catch (Exception e) {
			errorList.add (e.getMessage());			
			recForm.setErrorList(errorList);
			logErrors (errorList);
			return  mapping.findForward("dcsservices.error");
		}
		
		try {
			repositoryManager.putRecord(itemRecord, xmlFormat, collectionParam, recId, true);
		} catch (RecordUpdateException e) {
			recForm.setErrorMsg(e.getMessage());
			return mapping.findForward("dcsservices.error");
		}
		
		recForm.setId(recId);
		
		/* DcsDataFileIndexingPlugin plugin; */
		try {
			// create a status entry
			DcsDataRecord dcsDataRecord = dcsDataManager.getDcsDataRecord(recId, repositoryManager);
			
			prtln ("deleting entry ... ");
			
			String changeDate = dcsDataRecord.getChangeDate();
			dcsDataRecord.deleteStatusEntry (changeDate);
			// prtln (Dom4jUtils.prettyPrint (dcsDataRecord.getDocument()));

			String dcsStatus = StatusFlags.RECOMMENDED_STATUS;
			String dcsStatusNote = "recommended via Recommender service";
			this.repositoryService.validateRecord(itemRecord, dcsDataRecord, xmlFormat);

					
			dcsDataRecord.updateStatus (dcsStatus, dcsStatusNote, Constants.UNKNOWN_EDITOR);
			
			// prtln ("status record for recommended record");
			// prtln (Dom4jUtils.prettyPrint (dcsDataRecord.getDocument()));
			repositoryService.updateRecord(recId);
			dcsDataRecord.flushToDisk();
		} catch (Throwable e) {
			prtln ("WARNING: error updating DcsDataRecord: " + e.getMessage());
			return mapping.findForward("dcsservices.error");
		}
		return mapping.findForward("dcsservices.RecommendResource");
	}

	protected ActionForward doRecommendCollection (
			HttpServletRequest request,
			HttpServletResponse response,
			RecommenderForm recForm,
			ActionMapping mapping)
			 throws Exception {

		prtln ("doRecommendCollection()");				 
		boolean doAuthorize = false;
		
		// only trusted ips may perform put record
		prtln ("authorizing");
		if (doAuthorize) {
			if (isAuthorized(request, "trustedIp")) {
				recForm.setAuthorizedFor("trustedUser");
			}
			else {
				recForm.setErrorMsg("You are not authorized to use the DCS Recommender service");
				return mapping.findForward("dcsservices.error");
			}
		}

		// validate presence of required arguments
		prtln ("\tvalidating required arguments");

		List errorList = new ArrayList();
		
		CollectionConfig collectionConfig = null;
		String collectionParam = request.getParameter("collection");
		if (collectionParam == null || collectionParam.length() == 0) {
			errorList.add ("The \'collection\' argument is required but is missing or empty");
		}
		else {
			collectionConfig = this.collectionRegistry.getCollectionConfig(collectionParam);
			if (collectionConfig == null) {
				errorList.add ("The specified collection, \"" + collectionParam + "\" does not exist");
			}
			if (!collectionConfig.getXmlFormat().equals("ncs_collect")) {
				errorList.add ("Specified collection must be of 'nsc_collect' format");
			}
		}

		String recordXmlParam = request.getParameter("recordXml");
		if (recordXmlParam == null || recordXmlParam.length() == 0) {
			errorList.add ("The \'recordXml\' argument is required but is missing or empty");
		}
		
		if (errorList.size() > 0) {
			recForm.setErrorList(errorList);
			logErrors (errorList);
			return mapping.findForward("dcsservices.error");
		}
		
		prtln ("required request params validated");
		
		String collectRecord = null;
		String recId = collectionConfig.nextID();
		try {
			collectRecord = makeItemRecord (recordXmlParam, recId, collectionConfig);
		} catch (Exception e) {
			errorList.add (e.getMessage());			
			recForm.setErrorList(errorList);
			logErrors (errorList);
			return  mapping.findForward("dcsservices.error");
		} catch (Throwable t) {
			errorList.add ("Recommender service caught unknown error");
			recForm.setErrorList(errorList);
			logErrors (errorList);
			return mapping.findForward("dcsservices.error");
		}
		
		try {
			repositoryManager.putRecord(collectRecord, collectionConfig.getXmlFormat(), collectionParam, recId, true);
		} catch (RecordUpdateException e) {
			recForm.setErrorMsg(e.getMessage());
			return mapping.findForward("dcsservices.error");
		}
		
		recForm.setId(recId);
		
		// HERE IS WHERE WE MAKE THE SERVICE DESCRIPTION AND POKE IT IN THE DCS_DATA_RECORD CONFIG
		
		/* DcsDataFileIndexingPlugin plugin; */
		try {
			// create a status entry
			DcsDataRecord dcsDataRecord = dcsDataManager.getDcsDataRecord(recId, repositoryManager);
			dcsDataRecord.deleteStatusEntry (dcsDataRecord.getChangeDate());
			String dcsStatus = StatusFlags.RECOMMENDED_STATUS;
			String dcsStatusNote = "recommended via Recommender service";
			this.repositoryService.validateRecord(collectRecord, dcsDataRecord, "ncs_collect");
			dcsDataRecord.updateStatus (dcsStatus, dcsStatusNote, Constants.UNKNOWN_EDITOR);
			
			prtln ("status record for recommended collect record");
			prtln (Dom4jUtils.prettyPrint (dcsDataRecord.getDocument()));
			repositoryService.updateRecord(recId);
			dcsDataRecord.flushToDisk();
		} catch (Throwable e) {
			prtln ("WARNING: error updating DcsDataRecord: " + e.getMessage());
			return mapping.findForward("dcsservices.error");
		}
		return mapping.findForward("dcsservices.RecommendCollection");
	}		
	
	private void logErrors (List errorList) {
		if (errorList.isEmpty())
			return;
		prtln ("Recommender service request errors: ");
		for (Iterator i=errorList.iterator();i.hasNext();)
			prtln ("\t" + (String)i.next());
	}
			
	
	private String makeItemRecord (String recordXml, 
									 String recId, 
									 CollectionConfig collectionConfig) throws Exception {
		Document itemRecord = null;
		try {
			itemRecord = DocumentHelper.parseText(recordXml);
		} catch (Exception e) {
			throw new Exception ("Provided recordXML is not well formed");
		}
		String xmlFormat = collectionConfig.getXmlFormat();
		String idPath = null;
		MetaDataFramework framework = null;
		try {
			framework = this.frameworkRegistry.getFramework(xmlFormat);
			if (framework  == null) 
				throw new Exception ("MetadataFramework not loaded for \"" + xmlFormat +"\"");
			
			if (!framework.getSchemaHelper().getNamespaceEnabled()) {
				itemRecord = Dom4jUtils.localizeXml(itemRecord);
			}
			
			idPath = framework.getIdPath();
			if (idPath == null || idPath.trim().length() == 0)
				throw new Exception ("id path not configured for \"" + xmlFormat + "\" framework");
			
		} catch (Exception e) {
			throw new Exception ("Server Configuration error: " + e.getMessage());
		}
		
		try {
			DocMap docMap = new DocMap (itemRecord);
			docMap.smartPut (idPath, recId);
			
			if (xmlFormat.equals ("ncs_collect")) {
				
				String brandURL = "http://nsdl.org/images/brands/" + recId + ".jpg";
				String brandURLPath = "/record/collection/brandURL";
				docMap.smartPut (brandURLPath, brandURL);
				
			}
		} catch (Exception e) {
			throw new Exception ("Server error: unable to assign id to record: " + e);
		}
		
		return framework.getWritableRecordXml(itemRecord);
	}
	
	
	/**
	 *  Checks for IP authorization
	 *
	 * @param  request       HTTP request
	 * @param  securityRole  Security role
	 * @param  rm            RepositoryManager
	 * @return               True if authorized, false otherwise.
	 */
	private boolean isAuthorized(HttpServletRequest request, String securityRole) {
		prtln ("ip of requester: " + request.getRemoteAddr());
		if (securityRole.equals("trustedIp")) {
			String[] trustedIps = repositoryManager.getTrustedWsIpsArray();
			if (trustedIps == null)
				return false;

			String IP = request.getRemoteAddr();
			for (int i = 0; i < trustedIps.length; i++)
				if (IP.matches(trustedIps[i]))
					return true;
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
			System.out.println("RecommenderAction: " + s);
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

