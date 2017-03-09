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
package org.dlese.dpc.schemedit.action;

import org.dlese.dpc.repository.*;
import org.dlese.dpc.index.*;
import org.dlese.dpc.schemedit.*;
import org.dlese.dpc.schemedit.dcs.*;
import org.dlese.dpc.schemedit.config.*;
import org.dlese.dpc.schemedit.security.user.User;
import org.dlese.dpc.schemedit.action.form.*;
import org.dlese.dpc.schemedit.input.DocumentPruner;
import org.dlese.dpc.xml.*;
import org.dlese.dpc.xml.schema.*;
import org.dlese.dpc.util.strings.*;

import org.dom4j.Document;

import java.util.*;
import java.net.*;
import java.io.*;
import java.text.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionError;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionServlet;
import org.apache.struts.util.MessageResources;

/**
 *  A Struts Action controlling interaction during creation of metadata records
 *  for frameworks that require an initial data-entry screen before entering the
 *  metadata editor. For example, in the ADN framework, a url must first be
 *  checked for uniqueness before a record is created.
 *
 * @author    Jonathan Ostwald
 */
public abstract class CreateRecordAction extends DCSAction {

	private static boolean debug = false;


	/**
	 *  Gets the xmlFormat attribute of the CreateRecordAction object
	 *
	 * @return    The xmlFormat value
	 */
	protected abstract String getXmlFormat();


	/**
	 *  Gets the createForward attribute of the CreateRecordAction object
	 *
	 * @param  mapping  NOT YET DOCUMENTED
	 * @return          The createForward value
	 */
	protected abstract ActionForward getCreateForward(ActionMapping mapping);


	/**
	 *  Gets the confirmForward attribute of the CreateRecordAction object
	 *
	 * @param  mapping   NOT YET DOCUMENTED
	 * @param  carForm   NOT YET DOCUMENTED
	 * @param  request   NOT YET DOCUMENTED
	 * @param  response  NOT YET DOCUMENTED
	 * @return           The confirmForward value
	 */
	protected abstract ActionForward getConfirmForward(ActionMapping mapping,
	                                                   CreateADNRecordForm carForm,
	                                                   HttpServletRequest request,
	                                                   HttpServletResponse response
	                                                   );

	// --------------------------------------------------------- Public Methods

	/**
	 *  Processes the specified HTTP request and creates the corresponding HTTP
	 *  response by forwarding to a JSP that will create it. Returns an {@link
	 *  org.apache.struts.action.ActionForward} instance that maps to a Struts
	 *  forwarding name, which must be configured in struts-config.xml to forward
	 *  to the JSP page that will handle the request.
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

		CreateADNRecordForm carForm = (CreateADNRecordForm) form;
		String errorMsg = "";

		carForm.setRequest(request);

		SimpleLuceneIndex index = this.repositoryManager.getIndex();
		if (index == null) {
			errorMsg = "attribute \"index\" not found in servlet context";
			throw new ServletException(errorMsg);
		}

		MetaDataFramework framework = this.getMetaDataFramework(getXmlFormat());
		if (framework == null) {
			errorMsg = "The \"" + getXmlFormat() + "\" metadata framework can not be found";
			errors.add("error", new ActionError("generic.error", errorMsg));
			saveErrors(request, errors);
			return mapping.findForward("error.page");
		}

		SchemEditUtils.showRequestParameters(request);

		try {
			if (request.getParameter("command") != null) {
				String param = request.getParameter("command");

				if (param.equalsIgnoreCase("new")) {
					carForm.clear();
					String collection = request.getParameter("collection");
					carForm.setCollection(collection);
					SetInfo setInfo = SchemEditUtils.getSetInfo(collection, repositoryManager);
					if (setInfo != null) {
						carForm.setCollectionName(setInfo.getName());
					}
					return this.getCreateForward(mapping);
				}

				if (param.equalsIgnoreCase("cancel")) {
					carForm.clear();
					return mapping.findForward("browse.home");
				}

				if (param.equalsIgnoreCase("submit")) {
					return this.handleSubmit(carForm, framework, mapping, request, response);
				}
			}
			carForm.clear();
			errors.add("message",
				new ActionError("generic.message", "No command submitted - no action taken"));
			saveErrors(request, errors);
			return this.getCreateForward(mapping);
		} catch (Throwable e) {
			prtlnErr("Exception: " + e);
			e.printStackTrace();
			errors.add("error",
				new ActionError("generic.error", "There has been a system error: " + e.getMessage()));
			saveErrors(request, errors);
			return mapping.findForward("error.page");
		}
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  carForm    NOT YET DOCUMENTED
	 * @param  framework  NOT YET DOCUMENTED
	 * @param  mapping    NOT YET DOCUMENTED
	 * @param  request    NOT YET DOCUMENTED
	 * @param  response   NOT YET DOCUMENTED
	 * @return            NOT YET DOCUMENTED
	 */
	protected ActionForward handleSubmit(CreateADNRecordForm carForm,
	                                     MetaDataFramework framework,
	                                     ActionMapping mapping,
	                                     HttpServletRequest request,
	                                     HttpServletResponse response) {
		prtln("submit\n\tprimaryUrl: " + carForm.getPrimaryUrl() +
			"\n\tvalidatedUrl: " + carForm.getValidatedUrl());
		ActionErrors errors = validateForm(request, carForm, framework);
		prtln("validation found " + errors.size() + " errors");
		if (errors.size() > 0) {
			carForm.setValidatedUrl(null);
			errors.add("error",
				new ActionError("edit.errors.found"));
			saveErrors(request, errors);
			return this.getCreateForward(mapping);
		}

		// are we concerned with testing for similar and duplicate values?
		if (framework.getUniqueUrlPath() != null) {
			/*
			  1 - if we have not seen this url (primaryUrl) before we need to check for sims.
				validatedUrl is set to preserve a url that is valid, but which also has sims
				  - note: validatedUrl is set below, so it was set in the previous call to handleSubmit)
				case A) if validatedUrl is null, then we have not tested for sims and must do so now. if sim(s)
				are found, then we will preserve the primaryUrl value as "validatedUrl" and present a
				confirmation message to user (this has sims, do you really want to save?).
				case B) if primaryUrl is not equal to validatedUrl, then the user has changed the
					previous url and we have to evaluate again for sims (setting validatedUrl
					if sims are found, etc)
			  2 - if validatedUrl and primaryUrl are the same, then user has confirmed that the
			  	primaryUrl is to be used (even though it is similar), and we can perform the submit
				operation
			*/
			if (carForm.getValidatedUrl() == null ||
				!carForm.getPrimaryUrl().equals(carForm.getValidatedUrl())) {
				prtln("checking sims");
				List sims = repositoryService.getSims(carForm.getPrimaryUrl(), carForm.getCollection());
				if (sims.size() > 0) {
					prtln(sims.size() + " similar urls found");
					carForm.setValidatedUrl(carForm.getPrimaryUrl());
					carForm.setSims(sims);
					errors.add("message",
						new ActionError("similar.urls.found", Integer.toString(sims.size())));
					saveErrors(request, errors);
					return this.getCreateForward(mapping);
				}
			}
		}

		return handleNewRecordRequest(carForm, framework, mapping, request, response);
	}


	/**
	 *  Description of the Method
	 *
	 * @param  carForm    Description of the Parameter
	 * @param  framework  Description of the Parameter
	 * @param  mapping    Description of the Parameter
	 * @param  request    Description of the Parameter
	 * @param  response   NOT YET DOCUMENTED
	 * @return            Description of the Return Value
	 */
	private ActionForward handleNewRecordRequest(CreateADNRecordForm carForm,
	                                             MetaDataFramework framework,
	                                             ActionMapping mapping,
	                                             HttpServletRequest request,
	                                             HttpServletResponse response) {
		prtln("handleNewRecordRequest");
		ActionErrors errors = new ActionErrors();
		try {
			String collection = carForm.getCollection();
			String recId = collectionRegistry.getNextID(collection);
			carForm.setRecId(recId);

			// prepare record Document
			String xmlFormat = carForm.getXmlFormat();
			Document recordDoc = makeRecordDoc(carForm, framework, mapping, request, response);

			DocumentPruner.pruneDocument(recordDoc, framework.getSchemaHelper());

			String username = this.getSessionUserName(request);
			repositoryService.saveNewRecord(recId, recordDoc.asXML(), collection, username);			
		} catch (Exception e) {
			errors.add("error",
				new ActionError("generic.error", e.getMessage()));
			saveErrors(request, errors);
			return this.getCreateForward(mapping);
		} catch (Throwable t) {
			t.printStackTrace();
			errors.add("error",
				new ActionError("generic.error", "unknown system error"));
			saveErrors(request, errors);
			return this.getCreateForward(mapping);
		}
		// prtln (Dom4jUtils.prettyPrint(collectionDoc));
		errors.add("message",
			new ActionError("adn.creation.confirmation"));
		saveErrors(request, errors);

		prtln("contextPath: " + request.getContextPath());
		prtln("servletPath: " + request.getServletPath());

		String editRecordLink = request.getContextPath() + "/editor/edit.do?command=edit&recId=" + carForm.getRecId();
		carForm.setEditRecordLink(editRecordLink);
		return this.getConfirmForward(mapping, carForm, request, response);
	}


	/**
	 *  Create an empty collection metadata document and populate from ActionForm
	 *  (carForm). Passes any error information back as an ActionError -used, for
	 *  example, when a webservice response contains an error that we want to
	 *  propagate back up to the user.
	 *
	 * @param  carForm        the ActionForm
	 * @param  framework      the MetaDataFramework of the record being created
	 * @return                Description of the Return Value
	 * @exception  Exception  Description of the Exception
	 */
	protected abstract Document makeRecordDoc(CreateADNRecordForm carForm,
											  MetaDataFramework framework,
											  ActionMapping mapping,
											  HttpServletRequest request,
											  HttpServletResponse response)
		 throws Exception;


	/**
	 *  Validate the input from user. Put changed or default values into carForm.
	 *  After this method returns carForm (rather than request) is used to process
	 *  user input.
	 *
	 * @param  request    the Request
	 * @param  carForm    the ActionForm
	 * @param  framework  the MetaDataFramework of the record being created
	 * @return            Description of the Return Value
	 */
	protected abstract ActionErrors validateForm(HttpServletRequest request,
	                                             CreateADNRecordForm carForm,
	                                             MetaDataFramework framework);


	/**
	 *  Sets the debug attribute of the CreateRecordAction class
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
			SchemEditUtils.prtln(s, "CreateRecordAction");
		}
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  s  NOT YET DOCUMENTED
	 */
	private void prtlnErr(String s) {
		SchemEditUtils.prtln(s, "CreateRecordAction");
	}
}

