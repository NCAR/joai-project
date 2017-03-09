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
package org.dlese.dpc.services.asn.action;

import org.dlese.dpc.services.asn.AsnServiceHelper;
import org.dlese.dpc.services.asn.action.form.AsnServiceForm;
import org.dlese.dpc.standards.asn.AsnStandard;
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
 * @author     Jonathan Ostwald
 * @version    $Id: AsnServiceAction.java,v 1.5 2010/08/31 22:51:44 ostwald Exp $
 */
public class AsnServiceAction extends Action {

	// Define strings to identify the different requests

	/**  The ServiceInfo request verb */
	public static final String SERVICE_INFO = "ServiceInfo";
	/**  The GetStandards request verb */
	public static final String GET_STANDARD_VERB = "GetStandard";
	
	public static final String LIST_STANDARDS_VERB = "ListStandards";
	
	public static final String ASN_PURL_BASE = "http://purl.org/ASN/resources/";

	private static Log log = LogFactory.getLog(AsnServiceAction.class);

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

		AsnServiceForm asnForm = null;
		try {

			asnForm = (AsnServiceForm) form;
			//clear previous values from form bean
			asnForm.clear();
			
			AsnServiceHelper asnServiceHelper =
				(AsnServiceHelper) servlet.getServletContext().getAttribute("asnServiceHelper");
			if (asnServiceHelper == null)
				throw new Exception ("Asn Standards Manager not found");
			asnForm.setAsnHelper(asnServiceHelper);
			
			// Grab the DDS service request verb:
			String verb = request.getParameter("verb");
			if (verb == null) {
				asnForm.setErrorMsg("The verb argument is required. Please indicate the request verb");
				return (mapping.findForward("service.error"));
			}

			else if (verb.equals(SERVICE_INFO)) {
				return doServiceInfo(request, response, asnForm, mapping);
			}

			// Handle check url request:
			else if (verb.equals(GET_STANDARD_VERB)) {
				return doGetStandard(request, response, asnForm, mapping);
			}

			else if (verb.equals(LIST_STANDARDS_VERB)) {
				return doListStandards(request, response, asnForm, mapping);
			}
			
			// The verb is not valid
			else {
				asnForm.setErrorMsg("The verb argument '" + verb + "' is not valid");
				return (mapping.findForward("service.error"));
			}
		} catch (NullPointerException npe) {
			log.error ("AsnServiceAction caught exception. ", npe);
			npe.printStackTrace();
			asnForm.setErrorMsg("There was an internal error by the server: " + npe);
			return (mapping.findForward("service.error"));
		} catch (Throwable e) {
			log.error("AsnServiceAction caught exception. ", e);
			e.printStackTrace();
			asnForm.setErrorMsg("There was an internal error by the server: " + e);
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
	 * @param  asnForm             NOT YET DOCUMENTED
	 * @return                An ActionForward to the JSP page that will handle the
	 *      response
	 * @exception  Exception  If error.
	 */
	protected ActionForward doServiceInfo(
	                                      HttpServletRequest request,
	                                      HttpServletResponse response,
	                                      AsnServiceForm asnForm,
	                                      ActionMapping mapping)
		 throws Exception {

		return mapping.findForward("service.info");
	}


	/**
	 *  Handles a GetStandards request.
	 *
	 *
	 *
	 * @param  request        The HTTP request
	 * @param  response       The HTTP response
	 * @param  asnForm             The bean
	 * @param  mapping        ActionMapping used
	 * @return                An ActionForward to the JSP page that will handle the
	 *      response
	 * @exception  Exception  If error.
	 */
	protected ActionForward doGetStandard(
	                                                HttpServletRequest request,
	                                                HttpServletResponse response,
	                                                AsnServiceForm asnForm,
	                                                ActionMapping mapping)
		 throws Exception {

		log.info ("processing GetStandard request");

		/* test for required params - set error if not found */
		String purl = request.getParameter("id");
		if (purl == null || purl.trim().length() == 0) {
			asnForm.setErrorMsg("The \"id\" argument is required but is missing or empty");
			return (mapping.findForward("service.error"));
		}
		if (purl.indexOf(ASN_PURL_BASE) != 0) {
			asnForm.setErrorMsg("The \"id\" argument must begin with \"" + ASN_PURL_BASE + "\"");
			return (mapping.findForward("service.error"));
		}

		AsnStandard result = getStandard(purl);
		if (result == null) {
			String errMsg = "No standard found for purl: " + purl;
			log.error (errMsg);
			asnForm.setErrorMsg(errMsg);
			return (mapping.findForward("service.error"));
		}

		/* place results (standards wrappers) into form such that they can be extracted by jsp */
		log.debug ("placing standard " + result.getId() + " into form");
		log.debug ("\tid: " + result.getId());
		log.debug ("\ttext" + result.getDescription());
		asnForm.setStandard(result);

		/*	 place service info into some kind of header object
			- timestamp, etc
		*/
		
		/* return action mapping to appropriate jsp via forward */
		return mapping.findForward("get.standard");
	}

	/**
	 *  Handles a ListStandards request.
	 *
	 *
	 *
	 * @param  request        The HTTP request
	 * @param  response       The HTTP response
	 * @param  asnForm             The bean
	 * @param  mapping        ActionMapping used
	 * @return                An ActionForward to the JSP page that will handle the
	 *      response
	 * @exception  Exception  If error.
	 */
	protected ActionForward doListStandards(
	                                                HttpServletRequest request,
	                                                HttpServletResponse response,
	                                                AsnServiceForm asnForm,
	                                                ActionMapping mapping)
		 throws Exception {

		log.info ("processing GetStandard request");

		/* test for required params - set error if not found */
		String [] purls = request.getParameterValues("id");
		if (purls == null || purls.length == 0) {
			asnForm.setErrorMsg("The \"id\" argument is required but is missing or empty");
			return (mapping.findForward("service.error"));
		}
		
		List results = new ArrayList();
		
		for (int i=0;i<purls.length;i++) {
			String purl = purls[i];
		
			if (purl.indexOf(ASN_PURL_BASE) != 0) {
				String errMsg = "The \"id\" argument must begin with \"" + ASN_PURL_BASE + "\"";
				log.error (errMsg);
				continue;
/* 				asnForm.setErrorMsg(errMsg);
				return (mapping.findForward("service.error")); */
			}

			AsnStandard result = getStandard(purl);
			if (result == null) {
				String errMsg = "No standard found for purl: " + purl;
				log.error (errMsg);
				continue;
				/* asnForm.setErrorMsg(errMsg);
				return (mapping.findForward("service.error")); */
			}
			
			results.add (result);

			/* place results (standards wrappers) into form such that they can be extracted by jsp */
			log.debug ("placing standard " + result.getId() + " into form");
			log.debug ("\tid: " + result.getId());
			log.debug ("\ttext" + result.getDescription());
		}
		asnForm.setStandardsList(results);
		
		/* return action mapping to appropriate jsp via forward */
		return mapping.findForward("list.standards");
	}

	
	/**
	 *  Gets a StandardsList from the StandardsSuggestionService if it is
	 *  available, or from "mySuggestStandards" otherwise.<p>
	 *
	 *  StandardsSuggestionService is initialized in AsnServlet, which is
	 *  activated by uncommenting it's declaration in WEB-INF/web.xml.
	 *
	 * @param  searchString       NOT YET DOCUMENTED
	 * @return                    The suggestedStandards value
	 */
 	AsnStandard getStandard(String purl) {
		AsnServiceHelper asnServiceHelper =
			(AsnServiceHelper) servlet.getServletContext().getAttribute("asnServiceHelper");
		AsnStandard result = null;
		try {
			result = asnServiceHelper.getStandard(purl);
			log.info ("result class: " + result.getClass().getName());
			if (result instanceof org.dlese.dpc.standards.asn.ColoradoBenchmark)
				log.info ("COLORADO BENCHMARK");
		} catch (Exception e) {
			log.error ("Exception: ", e);
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

