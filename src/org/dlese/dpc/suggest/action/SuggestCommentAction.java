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
import org.dlese.dpc.suggest.comment.*;
import org.dlese.dpc.suggest.resource.urlcheck.*;
import org.dlese.dpc.suggest.action.form.SuggestCommentForm;
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
 *  * Action controller for the Suggest a Comment servlet.
 *
 * @author     ostwald<p>
 *
 *      $Id $
 * @version    $Id: SuggestCommentAction.java,v 1.9 2009/03/20 23:34:00 jweather Exp $
 */
public final class SuggestCommentAction extends SuggestAction {

	private static boolean debug = true;

	private SuggestCommentHelper suggestHelper = null;

	/**
	 *  Gets the SuggestCommentHelper attribute of the SuggestCommentAction
	 *  object
	 *
	 * @return                       The SuggestCommentHelper value
	 * @exception  ServletException  NOT YET DOCUMENTED
	 */
	protected SuggestCommentHelper getSuggestHelper () throws ServletException {
		if (suggestHelper == null) {
			try {
				suggestHelper = (SuggestCommentHelper) servlet.getServletContext().getAttribute("SuggestCommentHelper");
				if (suggestHelper == null)
					throw new Exception ();
			} catch (Throwable t) {
				throw new ServletException ("SuggestCommentHelper is not initialized");
			}
		}
		return suggestHelper;
	}

	protected ActionForward initializeSuggestor(
	                                            ActionMapping mapping,
	                                            ActionForm form,
	                                            HttpServletRequest request,
	                                            HttpServletResponse response)
		 throws ServletException {
			 
		SuggestCommentForm scf = (SuggestCommentForm) form;
		ActionErrors errors = new ActionErrors();
			 
		String id = request.getParameter("id");
		if (id == null || id.trim().length() == 0) {
			errors.add("error",
				new ActionError("comment.id.required"));
			saveErrors(request, errors);
			return mapping.findForward("home");
		}
		
		scf.clear();
		
		scf.setPopup("p".equals( request.getParameter("view")));
		scf.setItemID(id);
		
		// ensure the provided record id (itemID) corresponds to an existing record
		String itemTitle = null;
		String itemURL = null;
		try {
			Map itemRecordProps = this.getSuggestHelper().getItemRecordProps(id);
			scf.setItemURL ((String)itemRecordProps.get ("url"));
			scf.setItemTitle((String)itemRecordProps.get ("title"));
		} catch (Exception e) {
			errors.add("error",
				new ActionError("comment.resource.not.found", id));
			saveErrors(request, errors);
			return mapping.findForward("home");
		}
		
		saveMessages(request, errors);
		return mapping.findForward("form");
		
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
	protected ActionForward handleOtherCommands(
	                                            ActionMapping mapping,
	                                            ActionForm form,
	                                            HttpServletRequest request,
	                                            HttpServletResponse response) throws ServletException {

		SuggestCommentForm scf = (SuggestCommentForm) form;
		ActionErrors errors = new ActionErrors();

		String command = request.getParameter("command");
		prtln("command: " + command);

		throw new ServletException("unsupported command: " + command);
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
	protected ActionForward handleEditCommand(
	                                          ActionMapping mapping,
	                                          ActionForm form,
	                                          HttpServletRequest request,
	                                          HttpServletResponse response)
											  throws Exception {

												  
		SuggestCommentForm scf = (SuggestCommentForm) form;
		
		if (scf.getItemID() == null || scf.getItemID().length() == 0 ) {			
			prtln("can't edit without a id ... bailing");
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

		SuggestCommentForm scf = (SuggestCommentForm) form;
		ActionErrors errors = new ActionErrors();

		scf.clear();
		scf.setItemID("");
		
		errors.add(ActionMessages.GLOBAL_MESSAGE,
			new ActionMessage("comment.cancel"));
		saveMessages(request, errors);
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

		SuggestCommentForm scf = (SuggestCommentForm) form;
		ActionErrors errors = new ActionErrors();

		// make sure there is a value for URL. If the user has previously "cancelled" and then
		// used the back buttons to return to an old form that *looks* like it holds data, but really
		// the data has been lost to the app upon cancellation
		if (scf.getItemID() == null || scf.getItemID().length() == 0) {
			prtln("can't get to done without a itemID ... bailing");
			return handleStaleData(mapping, form, request);
		}

		// Validate
		errors = validateSuggestForm(form, mapping, request);

		// report errors
		if (!errors.isEmpty()) {
			saveErrors(request, errors);
			return mapping.findForward("form");
		}

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

		SuggestCommentForm scf = (SuggestCommentForm) form;

		// make sure there is a value for URL. If the user has previously "cancelled" and then
		// used the back buttons to return to an old form that *looks* like it holds data, but really
		// the data has been lost to the app upon cancellation
		if (scf.getItemID() == null || scf.getItemID().length() == 0) {
			prtln("can't submit without a url ... bailing");
			return handleStaleData(mapping, form, request);
		}

		ActionMessages actionMessages = new ActionMessages();
		CommentRecord rec = null;
		try {
			rec = createRecord(scf);
		} catch (Exception e) {
			System.out.println("createRecord() failed to set values\n" + e);
			e.printStackTrace();
		}

		// putRecord to DCS
		String newId = null;
		try {
			newId = this.getSuggestHelper().putRecordToDCS(rec);
		} catch (Throwable e) {
			prtln("putRecord error: " + e.getMessage());
		}

		boolean notificationSent = false;
		try {
			new CommentEmailer(newId, this.getSuggestHelper()).sendNotification(scf);
		} catch (Exception e) {
			prtlnErr ("Email error: " + e.getMessage());
		}
		if (!notificationSent) {
			prtln("Notification NOT sent!");
		}
		else {
			prtln("Notification sent");
		}

		scf.clear();
		actionMessages.add(ActionMessages.GLOBAL_MESSAGE,
			new ActionMessage("comment.confirmation"));
		saveMessages(request, actionMessages);
		return mapping.findForward("home");
	}


	/**
	 *  The required fields for suggest-a-url are: url, nameFirst, nameLast,
	 *  emailPrimary, instName
	 *
	 * @param  mapping       Description of the Parameter
	 * @param  request       Description of the Parameter
	 * @param  form          NOT YET DOCUMENTED
	 * @return               Description of the Return Value
	 */
	protected ActionErrors validateSuggestForm(ActionForm form,
	                                           ActionMapping mapping,
	                                           HttpServletRequest request) {

		SuggestCommentForm scf = (SuggestCommentForm) form;
		ActionErrors errors = new ActionErrors();

		String description = scf.getDescription();
		String role = scf.getRole();
		String share = scf.getShare();
		String nameFirst = scf.getNameFirst();
		String nameLast = scf.getNameLast();
		String email = scf.getEmail();
		String instName = scf.getInstName();
		Boolean coppa = scf.getCoppa();

		if ((description == null) || (description.trim().equals(""))) {
			errors.add("description", new ActionError("field.required", "Comment"));
		}
		
		if ((role == null) || (role.trim().equals(""))) {
			errors.add("role", new ActionError("field.required", "Role"));
		}
		
 		if ((share == null) || (share.trim().equals(""))) {
			errors.add("share", new ActionError("this.field.required", "Share"));
		}	
		
		if ((nameFirst == null) || (nameFirst.trim().equals(""))) {
			errors.add("nameFirst", new ActionError("field.required", "First Name"));
		}

		if ((nameLast == null) || (nameLast.trim().equals(""))) {
			errors.add("nameLast", new ActionError("field.required", "Last Name"));
		}

		if (!coppa) {
			errors.add("coppa", new ActionError("this.field.required"));
		}
		
		if ((instName == null) || (instName.trim().equals(""))) {
			errors.add("instName", new ActionError("field.required.an", "Institution or Affiliation"));;
		}
		
		if ((email == null) || (email.trim().equals(""))) {
			errors.add("email", new ActionError("field.required.an", "Email Address"));
		}
		else {
			try {
				SuggestUtils.validateEmail (email);
			} catch (Exception e) {
				errors.add("email",
					new ActionError("generic.error", e.getMessage()));
			}
		}

		return errors;
	}


	/**
	 *  Update the SuggestionRecord (managed by SuggestCommentHelper) with values
	 *  from the form bean.<P>
	 *
	 * @param  form           NOT YET DOCUMENTED
	 * @exception  Exception  Description of the Exception
	 */
	protected CommentRecord createRecord(ActionForm form)
		 throws Exception {

		SuggestCommentForm scf = (SuggestCommentForm) form;
		CommentRecord rec = this.getSuggestHelper().newRecord();
		
		if (rec == null) {
			throw new Exception("createRecord could not get a record from SuggestCommentForm");
		}

		rec.setTitle("Comment on " + scf.getItemTitle());
		rec.setItemID(scf.getItemID());
		rec.setDescription(scf.getDescription());
		rec.setRole(scf.getRole());
		rec.setShare(scf.getShare());
		rec.setNameFirst(scf.getNameFirst());
		rec.setNameLast(scf.getNameLast());
		rec.setEmail(scf.getEmail());
		rec.setInstName(scf.getInstName());
		rec.setCreationDate(SuggestUtils.getBriefDate());
		
		return rec;
	}


	/**
	 *  Sets the debug attribute of the SuggestCommentAction class
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
			org.dlese.dpc.schemedit.SchemEditUtils.prtln(s, "SuggestCommentAction");
		}
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  s  NOT YET DOCUMENTED
	 */
	private static void prtlnErr(String s) {
		org.dlese.dpc.schemedit.SchemEditUtils.prtln(s, "SuggestCommentAction");
	}

}

