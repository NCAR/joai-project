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

import org.dlese.dpc.suggest.*;
import org.dlese.dpc.suggest.action.form.SuggestForm;
import org.dlese.dpc.xml.schema.*;
import org.dlese.dpc.xml.*;
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
 *  Abstract controller for a Suggestor Client. Implements the following flow of
 *  control:
 *  <ol>
 *    <li> Presents form for user input
 *    <li> Validates input. if there are errors returns user to form, otherwise
 *    presents confirmation page.
 *    <li> User can elect to re-edit the form, or "submit" it.
 *    <li> confirmation page is displayed upon submission
 *  </ol>
 *
 *
 * @author     ostwald<p>
 *
 *      $Id $
 * @version    $Id: SuggestAction.java,v 1.2 2009/03/20 23:34:00 jweather Exp $
 */
public abstract class SuggestAction extends Action {

	private static boolean debug = true;


	/**
	 *  Gets the suggestHelper attribute of the SuggestAction object
	 *
	 * @return                       The suggestHelper value
	 * @exception  ServletException  NOT YET DOCUMENTED
	 */
	protected abstract SuggestHelper getSuggestHelper() throws ServletException;


	/**
	 *  Gets the schemaHelper attribute of the SuggestAction object
	 *
	 * @return    The schemaHelper value
	 */
	protected SchemaHelper getSchemaHelper() {
		try {
			return getSuggestHelper().getSchemaHelper();
		} catch (Exception e) {}
		return null;
	}

	// --------------------------------------------------------- Public Methods

	/**
	 *  Processes the specified HTTP request and creates the corresponding HTTP
	 *  response by forwarding to a JSP that will create it. Returns an {@link
	 *  org.apache.struts.action.ActionForward} instance that maps to the Struts
	 *  forwarding name "xxx.xxx," which must be configured in struts-config.xml to
	 *  forward to the JSP page that will handle the request.
	 *
	 * @param  mapping               Description of the Parameter
	 * @param  form                  Description of the Parameter
	 * @param  request               Description of the Parameter
	 * @param  response              Description of the Parameter
	 * @return                       Description of the Return Value
	 * @exception  IOException       Description of the Exception
	 * @exception  ServletException  Description of the Exception
	 */
	public ActionForward execute(
	                             ActionMapping mapping,
	                             ActionForm form,
	                             HttpServletRequest request,
	                             HttpServletResponse response)
		 throws IOException, ServletException {
		/*
		    Design note:
		    Only one instance of this class gets created for the app and shared by
		    all threads. To be thread-safe, use only local variables, not instance
		    variables (the JVM will handle these properly using the stack). Pass
		    all variables via method signatures rather than instance vars.
		  */
		SuggestForm sform = (SuggestForm) form;

		SchemaHelper schemaHelper = getSchemaHelper();

		sform.setSchemaHelper(schemaHelper);

		MetadataVocab vocab = (MetadataVocab) servlet.getServletContext().getAttribute("MetadataVocab");
		sform.setVocab(vocab);

		ActionErrors errors = new ActionErrors();
		ActionMessages actionMessages = new ActionMessages();

		// Query Args
		String command = request.getParameter("command");
		// prtln("\ncommand: " + command + "\n");
		
		org.dlese.dpc.schemedit.SchemEditUtils.showRequestParameters(request); 

		// HANDLE COMMAND
		try {
			if (command == null) {
				// return handleCancelCommand(mapping, form, request, response);
				return initializeSuggestor(mapping, form, request, response);
			}

			// NEW - present simple form to get (and validate) URL
			if (command.equalsIgnoreCase("cancel")) {
				return handleCancelCommand(mapping, form, request, response);
			}

			// EDIT - read record and present form for editing
			if (command.equalsIgnoreCase("edit")) {
				return handleEditCommand(mapping, form, request, response);
			}

			// SUBMIT - user has confirmed data - now write it to disk
			// data is valid at this point and the user has hit the submit button
			if (command.equalsIgnoreCase("submit")) {
				return handleSubmitCommand(mapping, form, request, response);
			}

			// DONE - validate data and present confirmation page to user
			if (command.equalsIgnoreCase("done")) {
				return handleDoneCommand(mapping, form, request, response);
			}

			return handleOtherCommands(mapping, form, request, response);
		} catch (Exception e) {
			prtlnErr("ERROR: " + e.getMessage());
			e.printStackTrace();
			actionMessages.add(ActionMessages.GLOBAL_MESSAGE,
				new ActionMessage("generic.error", "Server Error: " + e.getMessage()));
		} catch (Throwable t) {
			prtlnErr("UNKNOWN ERROR: " + t.getMessage());
			actionMessages.add(ActionMessages.GLOBAL_MESSAGE,
				new ActionMessage("generic.error", "Unknown Server Error"));
		}
		saveMessages(request, actionMessages);
		return mapping.findForward("home");
	}


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
	protected abstract ActionForward initializeSuggestor(
	                                                     ActionMapping mapping,
	                                                     ActionForm form,
	                                                     HttpServletRequest request,
	                                                     HttpServletResponse response) throws ServletException;


	/**
	 *  Hook for subclasses to handle commands that are outside the ones defined in
	 *  this class.
	 *
	 * @param  mapping               NOT YET DOCUMENTED
	 * @param  form                  NOT YET DOCUMENTED
	 * @param  request               NOT YET DOCUMENTED
	 * @param  response              NOT YET DOCUMENTED
	 * @return                       NOT YET DOCUMENTED
	 * @exception  ServletException  NOT YET DOCUMENTED
	 */
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

		String command = request.getParameter("command");
		prtln("command: " + command);

		throw new ServletException("unsupported command: " + command);
	}


	/**
	 *  Populate SuggestionForm, and forward user to edit-form.
	 *
	 * @param  mapping        NOT YET DOCUMENTED
	 * @param  form           NOT YET DOCUMENTED
	 * @param  request        NOT YET DOCUMENTED
	 * @param  response       NOT YET DOCUMENTED
	 * @return                NOT YET DOCUMENTED
	 * @exception  Exception  if SuggestionForm cannot be populated with required
	 *      info.
	 */
	protected abstract ActionForward handleEditCommand(
	                                                   ActionMapping mapping,
	                                                   ActionForm form,
	                                                   HttpServletRequest request,
	                                                   HttpServletResponse response)
		 throws Exception;


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  mapping   NOT YET DOCUMENTED
	 * @param  form      NOT YET DOCUMENTED
	 * @param  request   NOT YET DOCUMENTED
	 * @param  response  NOT YET DOCUMENTED
	 * @return           NOT YET DOCUMENTED
	 */
	protected abstract ActionForward handleCancelCommand(
	                                                     ActionMapping mapping,
	                                                     ActionForm form,
	                                                     HttpServletRequest request,
	                                                     HttpServletResponse response);


	/**
	 *  Validate information supplied by user, return to edit form if there are
	 *  errors, or display confirmation page if there are no errors.
	 *
	 * @param  mapping   NOT YET DOCUMENTED
	 * @param  form      NOT YET DOCUMENTED
	 * @param  request   NOT YET DOCUMENTED
	 * @param  response  NOT YET DOCUMENTED
	 * @return           NOT YET DOCUMENTED
	 */
	protected abstract ActionForward handleDoneCommand(
	                                                   ActionMapping mapping,
	                                                   ActionForm form,
	                                                   HttpServletRequest request,
	                                                   HttpServletResponse response);


	/**
	 *  Attempt to write the suggestion to a DCS instance, forward user to
	 *  confirmation page.
	 *
	 * @param  mapping   NOT YET DOCUMENTED
	 * @param  form      NOT YET DOCUMENTED
	 * @param  request   NOT YET DOCUMENTED
	 * @param  response  NOT YET DOCUMENTED
	 * @return           NOT YET DOCUMENTED
	 */
	protected abstract ActionForward handleSubmitCommand(
	                                                     ActionMapping mapping,
	                                                     ActionForm form,
	                                                     HttpServletRequest request,
	                                                     HttpServletResponse response);


	/**
	 *  Return user to suggestor front page and show message explaining that they
	 *  were apparently trying to edit or submit data from a cancelled form
	 *
	 * @param  mapping  Description of the Parameter
	 * @param  request  Description of the Parameter
	 * @param  form     NOT YET DOCUMENTED
	 * @return          Description of the Return Value
	 */
	protected ActionForward handleStaleData(
	                                        ActionMapping mapping,
	                                        ActionForm form,
	                                        HttpServletRequest request) {
		SuggestForm sForm = (SuggestForm) form;
		sForm.clear();

		prtln("handleStaleData");
		ActionMessages actionMessages = new ActionMessages();
		actionMessages.add(ActionMessages.GLOBAL_MESSAGE,
			new ActionMessage("stale.data"));
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
	protected abstract ActionErrors validateSuggestForm(ActionForm form,
	                                                    ActionMapping mapping,
	                                                    HttpServletRequest request);


	/**
	 *  Update the SuggestionRecord (managed by SuggestResourceHelper) with values
	 *  from the form bean
	 *
	 * @param  form           NOT YET DOCUMENTED
	 * @exception  Exception  Description of the Exception
	 */
	protected abstract SuggestionRecord createRecord(ActionForm form) throws Exception;


	/**
	 *  Sets the debug attribute of the SuggestAction class
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
			org.dlese.dpc.schemedit.SchemEditUtils.prtln(s, "SuggestAction");
		}
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  s  NOT YET DOCUMENTED
	 */
	private static void prtlnErr(String s) {
		org.dlese.dpc.schemedit.SchemEditUtils.prtln(s, "SuggestAction");
	}

}

