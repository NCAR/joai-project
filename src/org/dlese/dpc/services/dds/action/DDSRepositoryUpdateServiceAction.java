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
package org.dlese.dpc.services.dds.action;

import org.dlese.dpc.services.dds.action.form.DDSRepositoryUpdateServiceForm;
import org.dlese.dpc.repository.RepositoryManager;
import org.dlese.dpc.repository.RecordUpdateException;
import org.dlese.dpc.repository.PutCollectionException;
import org.dlese.dpc.index.reader.*;
import org.dlese.dpc.index.SimpleLuceneIndex;
import org.dlese.dpc.index.ResultDoc;
import org.dom4j.*;

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



/**
 *  An Action that implements a RESTful Web service for performing updates to a DDS repository.
 *
 * @author    John Weatherley
 */
public final class DDSRepositoryUpdateServiceAction extends Action {
	private static boolean debug = true;


	/**  PutRecord */
	public final static String PUT_RECORD_VERB = "PutRecord";	
	/**  DeleteRecord */
	public final static String DELETE_RECORD_VERB = "DeleteRecord";	
	/**  PutCollection */
	public final static String PUT_COLLECTION_VERB = "PutCollection";
	/**  Delete Collection */
	public final static String DELETE_COLLECTION_VERB = "DeleteCollection";	


	// --------------------------------------------------------- Public Methods

	/**
	 *  Processes the Web service request by forwarding to the appropriate corresponding JSP page for
	 *  rendering.
	 *
	 * @param  mapping        The ActionMapping used to select this instance
	 * @param  request        The HTTP request we are processing
	 * @param  response       The HTTP response we are creating
	 * @param  form           The ActionForm for the given page
	 * @return                The ActionForward instance describing where and how control should be forwarded
	 * @exception  Exception  If error.
	 */
	public ActionForward execute(
	                             ActionMapping mapping,
	                             ActionForm form,
	                             HttpServletRequest request,
	                             HttpServletResponse response)
		 throws Exception {
		
		DDSRepositoryUpdateServiceForm ddsusf = null;
		try {
			ddsusf = (DDSRepositoryUpdateServiceForm) form;
			
			// Disallow any action if the service is not enabled:
			String enableService = (String) servlet.getServletContext().getInitParameter("enableRepositoryUpdateWebService");	 
			if(enableService == null || !enableService.equals("true")) {
				ddsusf.setErrorMsg("The service is disabled.");
				ddsusf.setErrorCode(DDSRepositoryUpdateServiceForm.ERROR_CODE_SERVICE_DISABLED);
				return mapping.findForward("ddsupdateservice.error");			
			}
			// Disallow any action if the service is not configured from the file system data source:
			String recordDataSource = (String) servlet.getServletContext().getInitParameter("recordDataSource");	 
			if(recordDataSource == null || !recordDataSource.equals("fileSystem")) {
				ddsusf.setErrorMsg("The service is disabled. The data source " + recordDataSource + " is not supported.");
				ddsusf.setErrorCode(DDSRepositoryUpdateServiceForm.ERROR_CODE_SERVICE_DISABLED);
				return mapping.findForward("ddsupdateservice.error");			
			}			
			
			RepositoryManager repositoryManager =
				(RepositoryManager) servlet.getServletContext().getAttribute("repositoryManager");

			// only trusted ips may perform these operations
			boolean doAuthorize = true;
			if (doAuthorize) {
				if (isAuthorized(request, "trustedIp", repositoryManager)) {
					ddsusf.setAuthorizedFor("trustedUser");
				}
				else {
					ddsusf.setErrorMsg("You are not authorized to use this service");
					ddsusf.setErrorCode(DDSRepositoryUpdateServiceForm.ERROR_CODE_NOTAUTHORIZED);
					return mapping.findForward("ddsupdateservice.error");
				}
			}			
				
			// Grab the DDS update service request verb:
			String verb = request.getParameter("verb");
			if (verb == null) {
				ddsusf.setErrorMsg("The verb argument is required. Please indicate the request verb");
				ddsusf.setErrorCode(DDSRepositoryUpdateServiceForm.ERROR_CODE_BADVERB);
				return (mapping.findForward("ddsupdateservice.error"));
			}
			// Handle service requests:
			else if (verb.equals(PUT_RECORD_VERB)) {
				return doPutRecord(request, response, repositoryManager, ddsusf, mapping);
			}
			else if (verb.equals(DELETE_RECORD_VERB)) {
				return doDeleteRecord(request, response, repositoryManager, ddsusf, mapping);
			}			
			else if (verb.equals(PUT_COLLECTION_VERB)) {
				return doPutCollection(request, response, repositoryManager, ddsusf, mapping);
			}
			else if (verb.equals(DELETE_COLLECTION_VERB)) {
				return doDeleteCollection(request, response, repositoryManager, ddsusf, mapping);
			}			
			// The verb is not valid for the DDS update web service
			else {
				ddsusf.setErrorMsg("The verb argument '" + verb + "' is not valid");
				ddsusf.setErrorCode(DDSRepositoryUpdateServiceForm.ERROR_CODE_BADVERB);
				return (mapping.findForward("ddsupdateservice.error"));
			}
		} catch (NullPointerException npe) {
			prtln("DDSRepositoryUpdateServiceAction caught exception. " + npe);
			npe.printStackTrace();
			ddsusf.setErrorMsg("There was an internal error by the server: " + npe);
			ddsusf.setErrorCode(DDSRepositoryUpdateServiceForm.ERROR_CODE_INTERNALSERVERERROR);
			return (mapping.findForward("ddsupdateservice.error"));
		} catch (Throwable e) {
			prtln("DDSRepositoryUpdateServiceAction caught exception. " + e);
			e.printStackTrace();
			ddsusf.setErrorMsg("There was an internal error by the server: " + e);
			ddsusf.setErrorCode(DDSRepositoryUpdateServiceForm.ERROR_CODE_INTERNALSERVERERROR);
			return (mapping.findForward("ddsupdateservice.error"));
		}
	}


	/**
	 *  Handles a request to put a collection into a DDS repository.
	 *
	 *
	 * @param  request        The HTTP request
	 * @param  response       The HTTP response
	 * @param  rm             The RepositoryManager used
	 * @param  ddsusf          The Form bean
	 * @param  mapping        ActionMapping used
	 * @return                An ActionForward to the JSP page that will handle the response
	 * @exception  Exception  If error.
	 */
	protected ActionForward doPutCollection(
	                                    HttpServletRequest request,
	                                    HttpServletResponse response,
	                                    RepositoryManager repositoryManager,
	                                    DDSRepositoryUpdateServiceForm ddsusf,
	                                    ActionMapping mapping)
		 throws Exception {

		prtln("doPutCollection()");

		String errorMsg = "";

		// validate presence of required arguments
		String nameParam = request.getParameter("name");
		if (nameParam == null || nameParam.length() == 0) {
			errorMsg += "The \'name\' argument is required but is missing or empty. ";
		}	
		String xmlFormatParam = request.getParameter("xmlFormat");
		if (xmlFormatParam == null || xmlFormatParam.length() == 0) {
			errorMsg += "The \'xmlFormat\' argument is required but is missing or empty. ";
		}
		String collectionKeyParam = request.getParameter("collectionKey");
		if (collectionKeyParam == null || collectionKeyParam.length() == 0) {
			errorMsg += "The \'collectionKey\' argument is required but is missing or empty. ";
		}

		if (errorMsg.length() > 0) {
			ddsusf.setErrorMsg(errorMsg);
			ddsusf.setErrorCode(DDSRepositoryUpdateServiceForm.ERROR_CODE_BADARGUMENT);
			return mapping.findForward("ddsupdateservice.error");
		}

		String descriptionParam = request.getParameter("description");
		String additionalMetadata = request.getParameter("additionalMetadata");
		
		prtln("params validated");

		try { 
			String collId = repositoryManager.putCollection(collectionKeyParam,xmlFormatParam,nameParam,descriptionParam,additionalMetadata);
			ddsusf.setXmlFormat(xmlFormatParam);
			ddsusf.setCollectionKey(collectionKeyParam);
			ddsusf.setId(collId);
			ddsusf.setResultCode(DDSRepositoryUpdateServiceForm.RESULT_CODE_SUCCESS);
			
			prtln("putCollection has succeeded and is returning");
			return mapping.findForward("ddsupdateservice.PutCollection");
		} catch (Exception e) {
			if(e instanceof NullPointerException) {
				e.printStackTrace();
				ddsusf.setErrorMsg("The server encountered a problem: Null pointer exception.");
				ddsusf.setErrorCode(DDSRepositoryUpdateServiceForm.ERROR_CODE_INTERNALSERVERERROR);				
			}
			else if(e instanceof PutCollectionException) {
				PutCollectionException pe = (PutCollectionException)e;
				
				// Throw badArgument if appropriate:
				ddsusf.setErrorMsg(e.getMessage());
				if(	pe.getErrorCode().equals(PutCollectionException.ERROR_CODE_BAD_ADDITIONAL_METADATA) || 
					pe.getErrorCode().equals(PutCollectionException.ERROR_CODE_BAD_FORMAT_SPECIFIER) || 
					pe.getErrorCode().equals(PutCollectionException.ERROR_CODE_BAD_KEY) || 
					pe.getErrorCode().equals(PutCollectionException.ERROR_CODE_BAD_TITLE)) {
					ddsusf.setErrorCode(DDSRepositoryUpdateServiceForm.ERROR_CODE_BADARGUMENT);	
				}
				else
					ddsusf.setErrorCode(DDSRepositoryUpdateServiceForm.ERROR_CODE_ILLEGAL_OPERATION);			
			}
			else {
				ddsusf.setErrorMsg(e.getMessage());
				ddsusf.setErrorCode(DDSRepositoryUpdateServiceForm.ERROR_CODE_ILLEGAL_OPERATION);
			}
			return mapping.findForward("ddsupdateservice.error");
		}
	}

	/**
	 *  Handles a request to delete a collection from a DDS repository.
	 *
	 *
	 * @param  request        The HTTP request
	 * @param  response       The HTTP response
	 * @param  rm             The RepositoryManager used
	 * @param  ddsusf          The Form bean
	 * @param  mapping        ActionMapping used
	 * @return                An ActionForward to the JSP page that will handle the response
	 * @exception  Exception  If error.
	 */
	protected ActionForward doDeleteCollection(
	                                    HttpServletRequest request,
	                                    HttpServletResponse response,
	                                    RepositoryManager repositoryManager,
	                                    DDSRepositoryUpdateServiceForm ddsusf,
	                                    ActionMapping mapping)
		 throws Exception {

		prtln("doDeleteCollection()");

		String errorMsg = "";

		// validate presence of required arguments
		String collectionKeyParam = request.getParameter("collectionKey");
		if (collectionKeyParam == null || collectionKeyParam.length() == 0) {
			errorMsg += "The \'collectionKey\' argument is required but is missing or empty. ";
		}

		if (errorMsg.length() > 0) {
			ddsusf.setErrorMsg(errorMsg);
			ddsusf.setErrorCode(DDSRepositoryUpdateServiceForm.ERROR_CODE_BADARGUMENT);
			return mapping.findForward("ddsupdateservice.error");
		}
		
		
		prtln("params validated");

		try { 
			boolean collectionExisted = repositoryManager.deleteCollection(collectionKeyParam);
			//ddsusf.setXmlFormat(xmlFormatParam);
			ddsusf.setCollectionKey(collectionKeyParam);
			if(collectionExisted)
				ddsusf.setResultCode(DDSRepositoryUpdateServiceForm.RESULT_CODE_SUCCESS);
			else
				ddsusf.setResultCode(DDSRepositoryUpdateServiceForm.RESULT_CODE_NO_SUCH_COLLECTION);
			
			prtln("doDeleteCollection has succeeded and is returning");
			return mapping.findForward("ddsupdateservice.DeleteCollection");
		} catch (Exception e) {
			if(e instanceof NullPointerException) {
				e.printStackTrace();
				ddsusf.setErrorMsg("The server encountered a problem: Null pointer exception.");
				ddsusf.setErrorCode(DDSRepositoryUpdateServiceForm.ERROR_CODE_INTERNALSERVERERROR);				
			}
			else {
				ddsusf.setErrorMsg(e.getMessage());
				ddsusf.setErrorCode(DDSRepositoryUpdateServiceForm.ERROR_CODE_ILLEGAL_OPERATION);
			}
			return mapping.findForward("ddsupdateservice.error");
		}
	}	
	
	/**
	 *  Handles a request to put a metadata record into a DDS repository. Wraps {@link
	 *  org.dlese.dpc.repository.RepositoryManager}.putRecord and therefore requires the same arguments.<p>
	 *
	 *
	 * @param  request        The HTTP request
	 * @param  response       The HTTP response
	 * @param  rm             The RepositoryManager used
	 * @param  ddsusf          The Form bean
	 * @param  mapping        ActionMapping used
	 * @return                An ActionForward to the JSP page that will handle the response
	 * @exception  Exception  If error.
	 */
	protected ActionForward doPutRecord(
	                                    HttpServletRequest request,
	                                    HttpServletResponse response,
	                                    RepositoryManager repositoryManager,
	                                    DDSRepositoryUpdateServiceForm ddsusf,
	                                    ActionMapping mapping)
		 throws Exception {

		//prtln("doPutRecord()");

		String errorMsg = "";

		// validate presence of required arguments
		String recordXmlParam = request.getParameter("recordXml");
		if (recordXmlParam == null || recordXmlParam.length() == 0) {
			errorMsg += "The \'recordXml\' argument is required but is missing or empty. ";
		}
		String xmlFormatParam = request.getParameter("xmlFormat");
		if (xmlFormatParam == null || xmlFormatParam.length() == 0) {
			errorMsg += "The \'xmlFormat\' argument is required but is missing or empty. ";
		}
		String collectionKeyParam = request.getParameter("collectionKey");
		if (collectionKeyParam == null || collectionKeyParam.length() == 0) {
			errorMsg += "The \'collectionKey\' argument is required but is missing or empty. ";
		}
		String idParam = request.getParameter("id");
		if (idParam == null || idParam.length() == 0) {
			errorMsg += "The \'id\' argument is required but is missing or empty. ";
		}

		if (errorMsg.length() > 0) {
			ddsusf.setErrorMsg(errorMsg);
			ddsusf.setErrorCode(DDSRepositoryUpdateServiceForm.ERROR_CODE_BADARGUMENT);
			return mapping.findForward("ddsupdateservice.error");
		}

		//prtln("params validated");

		try {
			
			prtln("doPutRecord() id: " + idParam);
			String actualRecordID = repositoryManager.putRecord(recordXmlParam, xmlFormatParam, collectionKeyParam, idParam, true);
			ddsusf.setXmlFormat(xmlFormatParam);
			ddsusf.setCollectionKey(collectionKeyParam);
			ddsusf.setId(actualRecordID);
			ddsusf.setResultCode(DDSRepositoryUpdateServiceForm.RESULT_CODE_SUCCESS);

			/*
				ISSUE - do we want to connect annotations to the records they
				annotate at this point? if we are saving an annotation record,
				then we have to call putRecord for the annotated records that
				are found in the current DCS index.
				THE FOLLOWING HAS NOT BEEN TESTED
			*/
			/* if (xmlFormatParam.equals("dlese_anno")) {
				prtln("indexing annotated record ...");
				try {
					RepositoryService repositoryService =
						(RepositoryService) servlet.getServletContext().getAttribute("repositoryService");
					if (repositoryService == null)
						throw new Exception("repositoryService not found in servlet context");
					Document doc = DocumentHelper.parseText(recordXmlParam);
					doc = Dom4jUtils.localizeXml(doc);
					repositoryService.indexAnnotatedRecord(new DocMap(doc));
				} catch (Exception e) {
					prtlnErr("error indexing annotated record: " + e.getMessage());
				}
			} */

			prtln("putRecord has succeeded and is returning");
			return mapping.findForward("ddsupdateservice.PutRecord");
		} catch (Exception e) {
			if(e instanceof NullPointerException) {
				e.printStackTrace();
				ddsusf.setErrorMsg("The server encountered a problem: Null pointer exception.");
				ddsusf.setErrorCode(DDSRepositoryUpdateServiceForm.ERROR_CODE_INTERNALSERVERERROR);				
			}
			else {
				ddsusf.setErrorMsg(e.getMessage());
				ddsusf.setErrorCode(DDSRepositoryUpdateServiceForm.ERROR_CODE_ILLEGAL_OPERATION);
			}
			return mapping.findForward("ddsupdateservice.error");
		}
	}
	

	/**
	 *  Handles a request to delete a metadata record into a DDS repository.<p>
	 *
	 *
	 * @param  request        The HTTP request
	 * @param  response       The HTTP response
	 * @param  rm             The RepositoryManager used
	 * @param  ddsusf          The Form bean
	 * @param  mapping        ActionMapping used
	 * @return                An ActionForward to the JSP page that will handle the response
	 * @exception  Exception  If error.
	 */
	protected ActionForward doDeleteRecord(
	                                    HttpServletRequest request,
	                                    HttpServletResponse response,
	                                    RepositoryManager repositoryManager,
	                                    DDSRepositoryUpdateServiceForm ddsusf,
	                                    ActionMapping mapping)
		 throws Exception {

		prtln("doDeleteRecord()");

		String errorMsg = "";

		// validate presence of required arguments
		String xmlFormatParam = request.getParameter("xmlFormat");
		String idParam = request.getParameter("id");
		if (idParam == null || idParam.length() == 0) {
			errorMsg += "The \'id\' argument is required but is missing or empty. ";
		}

		if (errorMsg.length() > 0) {
			ddsusf.setErrorMsg(errorMsg);
			ddsusf.setErrorCode(DDSRepositoryUpdateServiceForm.ERROR_CODE_BADARGUMENT);
			return mapping.findForward("ddsupdateservice.error");
		}

		prtln("params validated");

		try { 
			boolean recordExisted = repositoryManager.deleteRecord(idParam);
			ddsusf.setId(idParam);
			if(recordExisted)
				ddsusf.setResultCode(DDSRepositoryUpdateServiceForm.RESULT_CODE_SUCCESS);
			else
				ddsusf.setResultCode(DDSRepositoryUpdateServiceForm.RESULT_CODE_NO_SUCH_RECORD);

			/*
				ISSUE - do we want to update annotations to the records they
				annotate at this point? */

			prtln("doDeleteRecord has succeeded and is returning");
			return mapping.findForward("ddsupdateservice.DeleteRecord");
		} catch (Exception e) {
			if(e instanceof NullPointerException) {
				e.printStackTrace();
				ddsusf.setErrorMsg("The server encountered a problem: Null pointer exception.");
				ddsusf.setErrorCode(DDSRepositoryUpdateServiceForm.ERROR_CODE_INTERNALSERVERERROR);				
			}
			else {
				ddsusf.setErrorMsg(e.getMessage());
				ddsusf.setErrorCode(DDSRepositoryUpdateServiceForm.ERROR_CODE_ILLEGAL_OPERATION);
			}
			return mapping.findForward("ddsupdateservice.error");
		}
	}
	
	
	/**
	 *  Checks for IP authorization
	 *
	 * @param  request       HTTP request
	 * @param  securityRole  Security role
	 * @param  rm            RepositoryManager
	 * @return               True if authorized, false otherwise.
	 */
	private boolean isAuthorized(HttpServletRequest request, String securityRole, RepositoryManager repositoryManager) {
		if (securityRole.equals("trustedIp")) {
			String[] trustedIps = repositoryManager.getTrustedWsIpsArray();
			if (trustedIps == null)
				return false;

			String IP = request.getRemoteAddr();
			for (int i = 0; i < trustedIps.length; i++) {
				if (IP.matches(trustedIps[i]))
					return true;
			}
			prtln("unauthorized IP: " + IP);
		}
		return false;
	}


	// --------------- Debug output ------------------

	/**
	 *  Return a string for the current time and date, sutiable for display in log files and output to standout:
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
		System.err.println(getDateStamp() + "DDSRepositoryUpdateServiceAction Error: " + s);
	}



	/**
	 *  Output a line of text to standard out, with datestamp, if debug is set to true.
	 *
	 * @param  s  The String that will be output.
	 */
	protected final void prtln(String s) {
		if (debug) {
			System.out.println("DDSRepositoryUpdateServiceAction: " + s);
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

