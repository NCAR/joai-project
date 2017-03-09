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
package org.dlese.dpc.suggest.action;

import org.dlese.dpc.suggest.SuggestUtils;
import org.dlese.dpc.suggest.resource.*;
import org.dlese.dpc.suggest.resource.urlcheck.*;
import org.dlese.dpc.suggest.action.form.SuggestResourceForm;
import org.dlese.dpc.xml.schema.*;
import org.dlese.dpc.xml.*;
import org.dlese.dpc.util.strings.FindAndReplace;
import org.dlese.dpc.vocab.MetadataVocab;

import java.util.*;
import java.io.*;
import java.net.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionError;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionServlet;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;
import org.apache.struts.util.MessageResources;

/**
 *  * Action controller for the Suggest a Resource servlet
 *
 * @author     ostwald<p>
 *
 *      $Id $
 * @version    $Id: SuggestResourceAction.java,v 1.7 2009/03/20 23:34:00 jweather Exp $
 */
public final class SuggestResourceAction extends SuggestAction {

	private static boolean debug = true;

	private SuggestResourceHelper suggestHelper = null;


	/**
	 *  Gets the suggestResourceHelper attribute of the SuggestResourceAction
	 *  object
	 *
	 * @return                       The suggestResourceHelper value
	 * @exception  ServletException  NOT YET DOCUMENTED
	 */
	protected SuggestResourceHelper getSuggestHelper() throws ServletException {
		if (suggestHelper == null) {
			try {
				suggestHelper = (SuggestResourceHelper) servlet.getServletContext().getAttribute("SuggestResourceHelper");
				if (suggestHelper == null)
					throw new Exception();
			} catch (Throwable t) {
				throw new ServletException("SuggestResourceHelper is not initialized");
			}
		}
		return suggestHelper;
	}


	// ------------ Command Handlers ------------------------------------


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  mapping               NOT YET DOCUMENTED
	 * @param  form                  NOT YET DOCUMENTED
	 * @param  request               NOT YET DOCUMENTED
	 * @param  response              NOT YET DOCUMENTED
	 * @return                       NOT YET DOCUMENTED
	 * @exception  ServletException  NOT YET DOCUMENTED
	 */
	protected ActionForward initializeSuggestor(
	                                            ActionMapping mapping,
	                                            ActionForm form,
	                                            HttpServletRequest request,
	                                            HttpServletResponse response)
		 throws ServletException {

		SuggestResourceForm srf = (SuggestResourceForm) form;
		ActionErrors errors = validateUrl(form, mapping, request);

		// this suggestor only cares about DuplicatePrimaries (not similars, etc)
		if (errors.isEmpty() && srf.getDupRecord() != null) {
			prtln("getDupRecord found");
			return mapping.findForward("duplicate");
		}

		if (!errors.isEmpty()) {
			saveErrors(request, errors);
			return mapping.findForward("home");
		}

		// if validateUrl has been set in form by validateUrl
		String verifiedUrl = srf.getUrl();
		prtln("verifiedUrl: " + verifiedUrl);

		return mapping.findForward("form");
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  mapping        NOT YET DOCUMENTED
	 * @param  form           NOT YET DOCUMENTED
	 * @param  request        NOT YET DOCUMENTED
	 * @param  response       NOT YET DOCUMENTED
	 * @return                NOT YET DOCUMENTED
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	protected ActionForward handleEditCommand(
	                                          ActionMapping mapping,
	                                          ActionForm form,
	                                          HttpServletRequest request,
	                                          HttpServletResponse response)
		 throws Exception {

		SuggestResourceForm srf = (SuggestResourceForm) form;

		if (srf.getUrl() == null || srf.getUrl().length() == 0) {
			prtln("can't edit without a url ... bailing");
			return handleStaleData(mapping, form, request);
		}

		return mapping.findForward("form");
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  mapping   NOT YET DOCUMENTED
	 * @param  form      NOT YET DOCUMENTED
	 * @param  request   NOT YET DOCUMENTED
	 * @param  response  NOT YET DOCUMENTED
	 * @return           NOT YET DOCUMENTED
	 */
	protected ActionForward handleCancelCommand(
	                                            ActionMapping mapping,
	                                            ActionForm form,
	                                            HttpServletRequest request,
	                                            HttpServletResponse response) {

		SuggestResourceForm srf = (SuggestResourceForm) form;
		ActionErrors errors = new ActionErrors();

		srf.clear();
		srf.setUrl("");
		return mapping.findForward("home");
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  mapping   NOT YET DOCUMENTED
	 * @param  form      NOT YET DOCUMENTED
	 * @param  request   NOT YET DOCUMENTED
	 * @param  response  NOT YET DOCUMENTED
	 * @return           NOT YET DOCUMENTED
	 */
	protected ActionForward handleDoneCommand(
	                                          ActionMapping mapping,
	                                          ActionForm form,
	                                          HttpServletRequest request,
	                                          HttpServletResponse response) {

		SuggestResourceForm srf = (SuggestResourceForm) form;
		ActionErrors errors = new ActionErrors();

		// make sure there is a value for URL. If the user has previously "cancelled" and then
		// used the back buttons to return to an old form that *looks* like it holds data, but really
		// the data has been lost to the app upon cancellation
		if (srf.getUrl() == null || srf.getUrl().length() == 0) {
			prtln("can't get to done without a url ... bailing");
			return handleStaleData(mapping, form, request);
		}

		// Validate
		errors = validateSuggestForm(form, mapping, request);

		// report errors
		if (!errors.isEmpty()) {
			saveErrors(request, errors);
			return mapping.findForward("form");
		}

		srf.setPreserveGradeRanges(true);
		return mapping.findForward("confirm");
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  mapping   NOT YET DOCUMENTED
	 * @param  form      NOT YET DOCUMENTED
	 * @param  request   NOT YET DOCUMENTED
	 * @param  response  NOT YET DOCUMENTED
	 * @return           NOT YET DOCUMENTED
	 */
	protected ActionForward handleSubmitCommand(
	                                            ActionMapping mapping,
	                                            ActionForm form,
	                                            HttpServletRequest request,
	                                            HttpServletResponse response) {

		SuggestResourceForm srf = (SuggestResourceForm) form;

		// make sure there is a value for URL. If the user has previously "cancelled" and then
		// used the back buttons to return to an old form that *looks* like it holds data, but really
		// the data has been lost to the app upon cancellation
		if (srf.getUrl() == null || srf.getUrl().length() == 0) {
			prtln("can't submit without a url ... bailing");
			return handleStaleData(mapping, form, request);
		}

		ActionMessages actionMessages = new ActionMessages();
		ResourceRecord rec = null;
		try {
			rec = createRecord(srf);
		} catch (Exception e) {
			System.out.println("createRecord() failed to set values\n" + e);
			e.printStackTrace();
		}

		// putRecord to DCS
		String newId = null;
		try {
			newId = this.getSuggestHelper().putRecordToDCS(rec, srf.getValidatorResults());
		} catch (Throwable e) {
			prtln("putRecord error: " + e.getMessage());
		}

		boolean notificationSent = false;
		try {
			notificationSent = new ResourceEmailer(newId, this.getSuggestHelper()).sendNotification(srf);
		} catch (Exception e) {
			prtlnErr ("Email error: " + e.getMessage());
		}
		if (!notificationSent) {
			prtln("Notification NOT sent!");
		}
		else {
			prtln("Notification sent");
		}

		srf.clear();
		actionMessages.add(ActionMessages.GLOBAL_MESSAGE,
			new ActionMessage("submit.confirmation"));
		saveMessages(request, actionMessages);
		return mapping.findForward("home");
	}


	/**
	 *  The required fields for suggest-a-url are: url, nameFirst, nameLast,
	 *  emailPrimary, instName
	 *
	 * @param  mapping  Description of the Parameter
	 * @param  request  Description of the Parameter
	 * @param  form     NOT YET DOCUMENTED
	 * @return          Description of the Return Value
	 */
	protected ActionErrors validateSuggestForm(ActionForm form,
	                                           ActionMapping mapping,
	                                           HttpServletRequest request) {

		SuggestResourceForm srf = (SuggestResourceForm) form;
		ActionErrors errors = new ActionErrors();

		String url = srf.getUrl();
		String nameFirst = srf.getNameFirst();
		String nameLast = srf.getNameLast();
		String emailPrimary = srf.getEmailPrimary();
		String instName = srf.getInstName();
		Boolean coppa = srf.getCoppa();

		if ((nameFirst == null) || (nameFirst.trim().equals(""))) {
			errors.add("nameFirst", new ActionError("field.required", "First Name"));
		}

		if ((nameLast == null) || (nameLast.trim().equals(""))) {
			errors.add("nameLast", new ActionError("field.required", "Last Name"));
		}

		if (!coppa) {
			errors.add("coppa", new ActionError("this.field.required"));
		}
		
		if ((emailPrimary == null) || (emailPrimary.trim().equals(""))) {
			errors.add("emailPrimary", new ActionError("field.required.an", "Email Address"));
		}
		else {
			try {
				SuggestUtils.validateEmail(emailPrimary);
			} catch (Exception e) {
				errors.add("emailPrimary",
					new ActionError("generic.error", e.getMessage()));
			}
		}

		return errors;
	}


	/**
	 *  Validate a URL for proper syntax and then process it with the UrlValidator
	 *  to enforce rules for accepting a new suggestion. Validation results are
	 *  stored in the ActionForm, errors are returned to caller as ActionErrors.<p>
	 *
	 *  Verify that a URL is unique within the destination collection using the configured
	 *  searchService stored in the ServiceHelper. If either of these parameters are
	 *  not specified, then urls are validated by default (no errors are returned).
	 *
	 * @param  mapping  Description of the Parameter
	 * @param  request  Description of the Parameter
	 * @param  form     NOT YET DOCUMENTED
	 * @return          errors that tell why a url is not valid, or no errors if
	 *      the url is valid.
	 */
	protected ActionErrors validateUrl(ActionForm form,
	                                   ActionMapping mapping,
	                                   HttpServletRequest request) {

		SuggestResourceForm srf = (SuggestResourceForm) form;
		ActionErrors errors = new ActionErrors();

		String urlStr = srf.getUrl();

		String httpPrefix = "http://";
		if ((urlStr == null) || (urlStr.trim().equals(""))) {
			errors.add("url", new ActionError("field.required", "URL"));
			return errors;
		}

		// remove leading and trailing whitespace from the url (and update form bean)
		urlStr = FindAndReplace.replace(urlStr.trim(), " ", "%20", true);

		// prepend "http://" on to urlStr if it is not there already
		if (!urlStr.toLowerCase().startsWith(httpPrefix)) {
			urlStr = httpPrefix + urlStr;
		}

		srf.setUrl(urlStr);

		URL url = null;
		try {
			url = new URL(srf.getUrl());
		} catch (Exception e) {
			errors.add("url", new ActionError("generic.error", "illegal URL syntax"));
			return errors;
		}

		// Url is well-formed, now see if it is valid according to UrlValidator
		// look for a dup or similar url that is in the reference collection (if one is defined)
		UrlValidator validator = null;
		try {
			validator = this.getSuggestHelper().getUrlValidator();
		} catch (Throwable t) {
			prtlnErr("Could not obtain URL Validator");
		}
		if (validator != null && validator.getReferenceCollection() != null) {
			ValidatorResults vr = validator.validate(srf.getUrl());
			if (vr.hasDuplicate())
				prtln("Duplicate found");
			else
				prtln("NO Duplicate found");
			srf.setValidatorResults(vr);
		}
		else {
			prtln("referenceCollection not specified - no dup or sim check performed");
		}

		return errors;
	}



	/**
	 *  Update the SuggestionRecord (managed by SuggestResourceHelper) with values
	 *  from the form bean
	 *
	 * @param  form           NOT YET DOCUMENTED
	 * @exception  Exception  Description of the Exception
	 */
	protected ResourceRecord createRecord(ActionForm form)
		 throws Exception {

		SuggestResourceForm srf = (SuggestResourceForm) form;

		ResourceRecord rec = getSuggestHelper().newRecord();
		if (rec == null) {
			throw new Exception("createRecord could not get a record from SuggestResourceForm");
		}

		rec.setTitle(srf.getTitle());
		rec.setUrl(srf.getUrl());
		rec.setDescription(srf.getDescription());
		rec.setGradeRanges(srf.getGradeRanges());
		rec.setNameFirst(srf.getNameFirst());
		rec.setNameLast(srf.getNameLast());
		rec.setEmailPrimary(srf.getEmailPrimary());
		rec.setInstName(srf.getInstName());
		rec.setCreationDate(SuggestUtils.getBriefDate());
		
		return rec;
	}


	/**
	 *  Sets the debug attribute of the SuggestResourceAction class
	 *
	 * @param  db  The new debug value
	 */
	public static void setDebug(boolean db) {
		debug = db;
	}


	/**
	 *  Print a line to standard out.
	 *
	 * @param  s  The String to print.
	 */
	private static void prtln(String s) {
		if (debug) {
			org.dlese.dpc.schemedit.SchemEditUtils.prtln(s, "SuggestResourceAction");
		}
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  s  NOT YET DOCUMENTED
	 */
	private static void prtlnErr(String s) {
		org.dlese.dpc.schemedit.SchemEditUtils.prtln(s, "SuggestResourceAction");
	}

}

