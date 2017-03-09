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
package org.dlese.dpc.services.commcore.action;

import org.dlese.dpc.services.commcore.action.form.CommCoreForm;
import org.dlese.dpc.standards.commcore.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.*;
import java.text.*;
import java.io.*;
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

/**
 *  An <strong>Action</strong> that handles Web service requests.
 *
 * @author    Jonathan Ostwald
 */
public class CommCoreAction extends Action {

	// Define strings to identify the different requests

	/**  The ServiceInfo request verb */
	public final static String SERVICE_INFO = "ServiceInfo";
	/**  The GetStandards request verb */
	public final static String GET_STANDARD_VERB = "GetStandard";
	public final static String GET_MAPPINGS_VERB = "GetMappings";

	/**  NOT YET DOCUMENTED */
	public final static String ASN_PURL_BASE = "http://purl.org/ASN/resources/";

	private static Log log = LogFactory.getLog(CommCoreAction.class);
	private CommCoreServiceHelper commCoreServiceHelper = null;

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

		CommCoreForm ccForm = null;
		try {

			ccForm = (CommCoreForm) form;
			//clear previous values from form bean
			ccForm.clear();

			commCoreServiceHelper =
				(CommCoreServiceHelper) servlet.getServletContext().getAttribute("commCoreServiceHelper");
			if (commCoreServiceHelper == null)
				throw new Exception("Asn Standards Manager not found");
			ccForm.setCommCoreHelper(commCoreServiceHelper);

			// Grab the DDS service request verb:
			String verb = request.getParameter("verb");
			
			log.info ("verb: " + verb);
			log.info ("getMappings verb: " + GET_MAPPINGS_VERB);
			log.info ("same? " + verb.equals(GET_MAPPINGS_VERB));
			
			if (verb == null) {
				ccForm.setErrorMsg("The verb argument is required. Please indicate the request verb");
				return (mapping.findForward("service.error"));
			}

			else if (verb.equals(SERVICE_INFO)) {
				return doServiceInfo(request, response, ccForm, mapping);
			}

			// Handle check url request:
			else if (verb.equals(GET_STANDARD_VERB)) {
				return doGetStandard(request, response, ccForm, mapping);
			}
			
			else if (verb.equals(GET_MAPPINGS_VERB)) {
				return doGetMappings(request, response, ccForm, mapping);
			}

			// The verb is not valid
			else {
				ccForm.setErrorMsg("The verb argument '" + verb + "' is not valid");
				return (mapping.findForward("service.error"));
			}
		} catch (NullPointerException npe) {
			log.error("CommCoreAction caught exception. ", npe);
			npe.printStackTrace();
			ccForm.setErrorMsg("There was an internal error by the server: " + npe);
			return (mapping.findForward("service.error"));
		} catch (Throwable e) {
			log.error("CommCoreAction caught exception. ", e);
			e.printStackTrace();
			ccForm.setErrorMsg("There was an internal error by the server: " + e);
			return (mapping.findForward("service.error"));
		}
	}


	/**
	 *  Handles a request to get a the service information. <p>
	 *
	 *  Error Exception Conditions: <br>
	 *  badArgument - The request includes illegal arguments.
	 *
	 * @param  request        The HTTP request
	 * @param  response       The HTTP response
	 * @param  mapping        ActionMapping used
	 * @param  ccForm        NOT YET DOCUMENTED
	 * @return                An ActionForward to the JSP page that will handle the
	 *      response
	 * @exception  Exception  If error.
	 */
	protected ActionForward doServiceInfo(
	                                      HttpServletRequest request,
	                                      HttpServletResponse response,
	                                      CommCoreForm ccForm,
	                                      ActionMapping mapping)
		 throws Exception {

		return mapping.findForward("service.info");
	}


	/**
	 *  Handles a GetStandards request. <p>
	 *
	 *
	 *
	 * @param  request        The HTTP request
	 * @param  response       The HTTP response
	 * @param  ccForm        The bean
	 * @param  mapping        ActionMapping used
	 * @return                An ActionForward to the JSP page that will handle the
	 *      response
	 * @exception  Exception  If error.
	 */
	protected ActionForward doGetStandard(
	                                      HttpServletRequest request,
	                                      HttpServletResponse response,
	                                      CommCoreForm ccForm,
	                                      ActionMapping mapping)
		 throws Exception {
		// response.setContentType("text/html; charset=UTF-8");
		request.setCharacterEncoding("UTF-8");
		log.info("processing GetStandard request");

		/* test for required params - set error if not found */
		String id = request.getParameter("id");
		if (id == null || id.trim().length() == 0) {
			ccForm.setErrorMsg("The \"id\" argument is required but is missing or empty");
			return (mapping.findForward("service.error"));
		}

		Standard result = getStandard(id);
		if (result == null) {
			ccForm.setErrorMsg("No standard found for id: " + id);
			return (mapping.findForward("service.error"));
		}

		/* place results (standards wrappers) into form such that they can be extracted by jsp */
		log.info("placing standard " + result.getId() + " into form");
		log.info("\tid: " + result.getId());
		log.info("\ttext" + result.getItemText());
		ccForm.setStandard(result);

		/*	 place service info into some kind of header object
			- timestamp, etc
		*/
		/* return action mapping to appropriate jsp via forward */
		return mapping.findForward("get.standard");
	}

		/**
	 *  Handles a GetMappings request. <p>
	 *
	 *
	 *
	 * @param  request        The HTTP request
	 * @param  response       The HTTP response
	 * @param  ccForm        The bean
	 * @param  mapping        ActionMapping used
	 * @return                An ActionForward to the JSP page that will handle the
	 *      response
	 * @exception  Exception  If error.
	 */
	protected ActionForward doGetMappings(
	                                      HttpServletRequest request,
	                                      HttpServletResponse response,
	                                      CommCoreForm ccForm,
	                                      ActionMapping mapping)
		 throws Exception {

		log.info("processing GetStandard request");

		/* test for required params - set error if not found */
		String docId = request.getParameter("docId");
		if (docId == null || docId.trim().length() == 0) {
			ccForm.setErrorMsg("The \"docId\" argument is required but is missing or empty");
			return (mapping.findForward("service.error"));
		}

		StdDocument stdDoc = commCoreServiceHelper.getStdDocument(docId);
		if (stdDoc == null) {
			ccForm.setErrorMsg("No standard document found for docId: " + docId);
			return (mapping.findForward("service.error"));
		}
		
		Collection standards = stdDoc.getStandards();

		/* place results (standards wrappers) into form such that they can be extracted by jsp */

		ccForm.setDocId(docId);
		ccForm.setStandards(standards);

		/*	 place service info into some kind of header object
			- timestamp, etc
		*/
		/* return action mapping to appropriate jsp via forward */
		return mapping.findForward("get.mappings");
	}


	/**
	 *  Gets a StandardsList from the StandardsSuggestionService if it is
	 *  available, or from "mySuggestStandards" otherwise.<p>
	 *
	 *  StandardsSuggestionService is initialized in AsnServlet, which is activated
	 *  by uncommenting it's declaration in WEB-INF/web.xml.
	 *
	 * @param  id          NOT YET DOCUMENTED
	 * @return               The suggestedStandards value
	 */
	Standard getStandard(String id) {
		Standard result = null;
		try {
			result = commCoreServiceHelper.getStandard(id);
		} catch (Exception e) {
			log.error("Exception: ", e);
		}
		return result;
	}



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

}

