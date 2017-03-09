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

import org.dlese.dpc.schemedit.*;
import org.dlese.dpc.schemedit.input.SchemEditValidator;
import org.dlese.dpc.schemedit.repository.RepositoryService;
import org.dlese.dpc.schemedit.display.*;
import org.dlese.dpc.schemedit.dcs.DcsDataRecord;
import org.dlese.dpc.schemedit.config.SchemaPath;
import org.dlese.dpc.schemedit.config.CollectionRegistry;
import org.dlese.dpc.schemedit.config.CollectionConfig;
import org.dlese.dpc.schemedit.standards.CATServiceHelper;
import org.dlese.dpc.schemedit.standards.StandardsManager;
import org.dlese.dpc.schemedit.standards.asn.SelectedStandardsBean;
import org.dlese.dpc.schemedit.standards.asn.AsnSuggestionServiceHelper;
import org.dlese.dpc.schemedit.standards.asn.ResQualSuggestionServiceHelper;
import org.dlese.dpc.schemedit.action.form.SchemEditForm;
import org.dlese.dpc.xml.schema.SchemaHelper;
import org.dlese.dpc.xml.schema.DocMap;
import org.dlese.dpc.xml.XPathUtils;
import org.dlese.dpc.xml.Dom4jUtils;
import org.dlese.dpc.webapps.tools.GeneralServletTools;

import org.dlese.dpc.index.ResultDoc;
import org.dlese.dpc.index.reader.XMLDocReader;
import org.dlese.dpc.repository.RepositoryManager;
import org.dlese.dpc.repository.RecordUpdateException;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.Node;

import java.util.*;
import java.text.*;
import java.io.*;
import java.net.URL;
import java.util.Hashtable;
import java.util.Locale;

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
 *  Controller for the Metdata Editor that handles Indexed records rather than
 *  reading and writing records directly to disk.
 *
 *@author     ostwald
 *
 *
 */
public class DCSSchemEditAction extends AbstractSchemEditAction {

	/**
	 *  Description of the Field
	 */
	protected static boolean debug = true;

	/**
	 *  Gets the xmlFormat attribute of the DCSSchemEditAction object
	 *
	 *@return    The xmlFormat value
	 */
	protected String getXmlFormat() {
		return xmlFormat;
	}


	/**
	 *  Description of the Method
	 *
	 *@param  mapping               the ActionMapping
	 *@param  form                  the ActionForm
	 *@param  request               the Request
	 *@param  response              the Response
	 *@return                       Description of the Return Value
	 *@exception  IOException       Description of the Exception
	 *@exception  ServletException  Description of the Exception
	 */
	public ActionForward execute(
			ActionMapping mapping,
			ActionForm form,
			HttpServletRequest request,
			HttpServletResponse response)
			 throws IOException, ServletException {

		ActionErrors errors = initializeFromContext(mapping, request);

		String recId = request.getParameter("recId");
		xmlFormat = request.getParameter("xmlFormat");

		if (errors.isEmpty() && xmlFormat == null && recId != null) {
			// prtln ("\t getting xmlFormat from recId");

			XMLDocReader docReader = null;
			try {
				docReader = RepositoryService.getXMLDocReader(recId, repositoryManager);
				xmlFormat = docReader.getNativeFormat();
			} catch (Exception e) {
				errors.add("error",
						new ActionError("generic.error", "There has been a system error: " + e.getMessage()));
				saveErrors(request, errors);
				return mapping.findForward("error.page");
			}
			if (errors.isEmpty() &&
				!repositoryService.isAuthorizedSet(docReader.getCollection(),
												   this.getSessionUser (request),
												   this.requiredRole)) {
				errors.add("error",
						new ActionError("generic.error", "You are not authorized to edit record " + recId));
			}

			if (!errors.isEmpty()) {
				saveErrors(request, errors);
				return mapping.findForward("error.page");
			}
		}

		return super.execute(mapping, form, request, response);
	}


	/**
	 *  Handles requests containing a "command" parameter that was not handled in
	 *  the superclass.
	 *
	 *@param  mapping                   the ActionMapping
	 *@param  form                      the ActionForm
	 *@param  request                   the Request
	 *@param  response                  the Response
	 *@return                           forward to appropriate JSP page
	 *@exception  ServletException      Description of the Exception
	 *@exception  MissingLockException  if a lock to the record being edited is not
	 *      held.
	 */
	protected ActionForward handleOtherCommands(ActionMapping mapping,
			ActionForm form,
			HttpServletRequest request,
			HttpServletResponse response)
			 throws ServletException, MissingLockException {

		String command = request.getParameter("command");
		ActionErrors errors = new ActionErrors();
		SchemEditForm sef = (SchemEditForm) form;
		SessionBean sessionBean = this.getSessionBean(request);

		// prtln ("\ncommand: " + command);
		
		try {
		
			// Commands for adjusting the display of educationalStandards
			if (command.equals("standardsDisplay")) {
				if (!sessionBean.ownsLock(sef.getRecId())) {
					missingLockInfo (sef);
					throw new MissingLockException();
				}
				return handleStandardsDisplay(mapping, form, request);
			}
	
			if (command.equals("getAssignedStandards")) {
				if (!sessionBean.ownsLock(sef.getRecId())) {
					throw new MissingLockException();
				}
				return getAssignedStandards(mapping, form, request);
			}
	
			if (command.equals("aboutCasaaService")) {
				return mapping.findForward("about.casaa");
				
			} else if (command.equals("suggestStandards") ||
					command.equals("moreLikeThis")) {
				if (!sessionBean.ownsLock(sef.getRecId())) {
					throw new MissingLockException();
				}
				return doUpdateSuggestions(mapping, form, request);

			} else if (command.equals("async")) {
				return this.handleAsyncRequest(mapping, form, request, response);

			} else if (command.equals("resqualBenchmarks")) {
				return this.handleResQualBenchmarksDisplay (mapping, form, request, response);
			}
		} catch (Throwable e) {
			if ("true".equals(request.getParameter("async")) ||
				"async".equals(command)) {
				sef.setAsyncJason(makeJsonError (e.getMessage()));
				return mapping.findForward("async.json");
			}
			throw new ServletException (e);
		}
		return super.handleOtherCommands(mapping, form, request, response);
	}

	private String makeJsonError (String msg) {
		Element err = DocumentHelper.createElement ("error");
		if (msg == null)
			msg = "unknown error";
		err.setText(msg);
		Document responseDoc = DocumentHelper.createDocument(err);
		try {
			org.json.JSONObject json = org.json.XML.toJSONObject(responseDoc.asXML());
			return json.toString();
		} catch (Throwable e) {
			prtlnErr ("WARNING: unable to construct json error structure");
		}
			
		return "error"; 
	}
	
	private void missingLockInfo (SchemEditForm sef) {
		String recId = sef.getRecId();
		prtln ("\nmissingLockInfo()");
		prtln ("recID: " + recId);
		prtln ("is locked? " + this.sessionRegistry.isLocked(recId));
		prtln ("locked records");
		for (Iterator i=this.sessionRegistry.getLockedRecords().keySet().iterator();i.hasNext();) {
			String id = (String)i.next();
			String session = (String)sessionRegistry.getLockedRecords().get(id);
			prtln ("\t" + id + "  -  " + session);
		}
	}
	
	/**
	 *  Requests without a command are an error for the DCSSchemEditAction class
	 *
	 *@param  mapping               the ActionMapping
	 *@param  form                  the ActionForm
	 *@param  request               the Request
	 *@param  response              the Response
	 *@return                       Description of the Return Value
	 *@exception  ServletException  Description of the Exception
	 */
	protected ActionForward handleMissingCommand(
			ActionMapping mapping,
			ActionForm form,
			HttpServletRequest request,
			HttpServletResponse response)
			 throws ServletException {

		SchemEditForm sef = (SchemEditForm) form;

		sef.setRecId("");
		sef.setCollection("");
		String errorMsg = "illegal request received (no command specified)";
		throw new ServletException(errorMsg);
	}


	/**
	 *  Description of the Method
	 *
	 *@param  mapping                   the ActionMapping
	 *@param  form                      the ActionForm
	 *@param  request                   the Request
	 *@param  response                  the Response
	 *@param  validator                 Description of the Parameter
	 *@return                           Description of the Return Value
	 *@exception  ServletException      Description of the Exception
	 *@exception  MissingLockException  Description of the Exception
	 */
	protected ActionForward handleSaveRequest(ActionMapping mapping,
			ActionForm form,
			HttpServletRequest request,
			HttpServletResponse response,
			SchemEditValidator validator)
			 throws ServletException, MissingLockException {
		SchemEditForm sef = (SchemEditForm) form;
		SessionBean sessionBean = this.getSessionBean(request);
		MetaDataFramework metadataFramework = getMetaDataFramework();
		ActionErrors errors = new ActionErrors();

		if (!sessionBean.ownsLock(sef.getRecId())) {
			throw new MissingLockException();
		}

		validator.updateMultiValueFields();

		// is there a uniqueUrl field we should check for?
		String uniqueUrlPath = metadataFramework.getUniqueUrlPath();
		if (uniqueUrlPath != null) {
			/*
			 *  currentUrl is the current value of the uniqueUrlPath element
			 *  if currentUrl is same as validatedUrl, then we don't validate again,
			 *  since this means the user has hit "save" after getting the similarUrl
			 *  message
			 */
			String currentUrl = (String) sef.getDocMap().get(uniqueUrlPath);
			// prtln ("currentUrl: " + currentUrl + "\nvalidatedURL: " + sef.getValidatedUrl());

			if (sef.getValidatedUrl() == null || !currentUrl.equals(sef.getValidatedUrl())) {
				errors = uniqueUrlCheck(mapping, form, request);
			}
			errors.add (this.uniqueValueCheck(mapping, form, request, validator));
		}

		if (errors.size() > 0) {
			saveErrors(request, errors);
			return getEditorMapping(mapping);
		}

		// prepare record
		validator.pruneInstanceDoc();
		// order elements that are configured as such
		metadataFramework.processOrderedElements(sef.getDocMap().getDocument());
		
		// save the record to repository
		try {
			putRecord(mapping, form, request);
		} catch (RecordUpdateException rue) {
			prtln("RecordUpdateException: " + rue.getMessage());

			// report required paths - need to more gracefully handle missing required values!!
			prtln("required paths:");
			List schemaPaths = metadataFramework.getSchemaPathMap().getMinimalRecordPaths();
			for (Iterator i = schemaPaths.iterator(); i.hasNext(); ) {
				SchemaPath schemaPath = (SchemaPath) i.next();
				prtln("\t " + schemaPath.xpath);
			}
			errors.add("error",
					new ActionError("generic.error", "Could not save record: A field required by the indexer is missing"));
			saveErrors(request, errors);
			return getEditorMapping(mapping);
		} catch (Exception e) {
			prtln("putRecord error: " + e.getMessage());
			errors.add("pageErrors",
					new ActionError("generic.error", e.getMessage()));
			saveErrors(request, errors);
			return getEditorMapping(mapping);
		}

		// save is successful
		DocMap docMap = sef.getDocMap();

		sef.setSavedContent(DocContentMap.getDocContentMap(docMap.getDocument(), metadataFramework));
		sef.setGuardedExitPath(null);

		// update resultDoc in form Bean
		ResultDoc resultDoc = repositoryManager.getRecord(sef.getRecId());
		if (resultDoc == null) {
			throw new ServletException("record not found in repository for \"" + sef.getRecId() + "\"");
		}
		sef.setResultDoc(resultDoc);
		
		// update standards display if there is a suggestionService
		try {
			CATServiceHelper helper = this.initSuggestionServiceHelper(sef);
			String displayContent = request.getParameter("displayContent");
			if (displayContent != null)
				helper.updateStandardsDisplay(displayContent);
		} catch (Throwable t) {
			prtlnErr ("WARNING: initSuggestionServiceHelper: " + t.getMessage());
		}

		// display save confirmation message
		String recordIsValid = null;
		String saveMsgKey = "save.confirmation";
		try {
			recordIsValid = this.dcsDataManager.getDcsDataRecord (sef.getRecId(), this.repositoryManager).getIsValid();
			prtln ("recordIsValid: " + recordIsValid);
			saveMsgKey = ("true".equals(recordIsValid) ? "save.confirmation.valid" : "save.confirmation.invalid");
		} catch (Throwable t) {
			prtln ("Error checking validity: " + recordIsValid);
		}

		errors.add("message", new ActionError(saveMsgKey));

		if (ndrServiceEnabled) {
			confirmNdrWrite(resultDoc, sef, errors);
		}

		saveErrors(request, errors);

		return getEditorMapping(mapping);
	}


	/**
	 *  Add confirmation message if this collection is managed in the ndr.
	 *
	 *@param  resultDoc  Description of the Parameter
	 *@param  sef        Description of the Parameter
	 *@param  errors     Description of the Parameter
	 */
	private void confirmNdrWrite(ResultDoc resultDoc, SchemEditForm sef, ActionErrors errors) {
		try {
			XMLDocReader docReader = (XMLDocReader) resultDoc.getDocReader();
			String collection = docReader.getCollection();
			CollectionConfig config = this.collectionRegistry.getCollectionConfig(collection);
			if (config.isNDRCollection()) {
				DcsDataRecord dcsDataRecord =
						dcsDataManager.getDcsDataRecord(sef.getRecId(), repositoryManager);
				if (dcsDataRecord == null) {
					return;
				}
				String handle = dcsDataRecord.getNdrHandle();
				String syncError = dcsDataRecord.getNdrSyncError();

				if (handle != null && handle.trim().length() > 0 &&
						(syncError == null || syncError.trim().length() == 0)) {
					errors.add("message",
							new ActionError("ndr.save.confirmation"));
				}
			}
		} catch (Throwable t) {
			prtln("could not determine ndr save success: " + t.getMessage());
		}
	}

	/**
	* Wrap handleDeleteElement so we can update suggestion service helper in the case
	* that the standards field has been deleted.
	*/
	protected ActionForward handleDeleteElement(ActionMapping mapping,
	                                            ActionForm form,
	                                            HttpServletRequest request,
	                                            SchemEditValidator validator)
		 throws ServletException {
		prtln ("handleDeleteElement()");
		SchemEditForm sef = (SchemEditForm) form;
		ActionErrors errors = new ActionErrors();
		ActionForward forward = super.handleDeleteElement(mapping, form, request, validator);
		if (sef.getSuggestionServiceHelper() != null) {
			try {
				this.initSuggestionServiceHelper(sef);
			} catch (Exception e) {
				prtlnErr ("suggestionServiceHelper error: " + e.getMessage());
			}
		}
		return forward;
	 }

	/**
	 *  NOT YET DOCUMENTED
	 *
	 *@param  mapping               the ActionMapping
	 *@param  form                  the ActionForm
	 *@param  request               the Request
	 *@param  response              the Response
	 *@return                       NOT YET DOCUMENTED
	 *@exception  ServletException  NOT YET DOCUMENTED
	 */
	protected ActionForward handleCancelRequest(ActionMapping mapping,
			ActionForm form,
			HttpServletRequest request,
			HttpServletResponse response)
			 throws ServletException {

		SchemEditForm sef = (SchemEditForm) form;
		String recId = sef.getRecId();
		sef.setRecId("");
		if (sef.getCollection() != null && sef.getCollection().trim().length() > 0) {
			// prtln("about to forward to admin, with collection=" + sef.getCollection());
			return SchemEditUtils.forwardToCaller(request, recId, getSessionBean (request));
		} else {
			throw new ServletException("handleCancelRequest was not supplied a collection");
		}
	}


	/**
	 *  Description of the Method
	 *
	 *@param  mapping               the ActionMapping
	 *@param  form                  the ActionForm
	 *@param  request               the Request
	 *@param  response              the Response
	 *@return                       Description of the Return Value
	 *@exception  ServletException  Description of the Exception
	 */
	protected ActionForward handleEditRequest(ActionMapping mapping,
			ActionForm form,
			HttpServletRequest request,
			HttpServletResponse response)
			 throws ServletException {

		SchemEditForm sef = (SchemEditForm) form;
		MetaDataFramework framework = getMetaDataFramework();
		// prtln ("\t framework: " + framework.getXmlFormat())
		Document record = null;
		ActionErrors errors = new ActionErrors();
		String errorMsg;

		String recId = request.getParameter("recId");
		ResultDoc resultDoc = repositoryManager.getRecord(recId);

		if (resultDoc == null) {
			throw new ServletException("record not found in repository for \"" + recId + "\"");
		}

		sef.setResultDoc(resultDoc);
		XMLDocReader docReader = (XMLDocReader) resultDoc.getDocReader();
		String collection = docReader.getCollection();

		try {
			record = framework.getEditableDocument(docReader.getDocsource());
		} catch (DocumentException e) {
			throw new ServletException(e.getMessage());
		}

		// obtain dcs data record and set it in the bean (IS THIS NECESSARY??)
		DcsDataRecord dcsData = dcsDataManager.getDcsDataRecord(recId, repositoryManager);
		sef.setDcsDataRecord(dcsData);

		// OBTAIN LOCK For record
		if (!getSessionBean(request).getLock(sef.getRecId())) {
			errors.add("recordLocked",
					new ActionError("lock.not.obtained.error", sef.getRecId()));
			saveErrors(request, errors);
			return mapping.findForward("error.page");
		}

		sef.setCollection(collection);
		sef.setSetInfo(SchemEditUtils.getDcsSetInfo(collection, repositoryManager));
		
		record = framework.preprocessEditableDocument(record);
		
		sef.setDocMap(record);
		sef.setSavedContent(DocContentMap.getDocContentMap(record, framework));
		sef.setGuardedExitPath(null);
		sef.setSuggestionServiceHelper(null);

		// initialize the collapseBean's state
		// Currently we reset the CollapseBean each time a record is "opened".
		// but if this call is disabled, the state will be retained across documents ...
		new CollapseBeanInitializer(sef.getCollapseBean(), record, framework).init();

		// initialize standards display and helper

		try {
			CATServiceHelper helper = this.initSuggestionServiceHelper(sef);
			/* NOTE: if the current collection has not been configured to allow suggestions,
			   then the helper will not exist (this is normal).
			*/
			if (helper != null) {
				// we want to start out with a standards doc that has some selections, or the default

				// NOTE: ADN helper has no available docs, so we can't throw exception if this test returns 0
				if (helper.getAvailableDocs().size() > 1) {
					// prtln ("there are available docs - setting current doc");
					AsnSuggestionServiceHelper asnHelper = (AsnSuggestionServiceHelper)helper;
					SelectedStandardsBean ss = asnHelper.getSelectedStandardsBean();
					String currentDocKey = sef.getCurrentStdDocKey();
					Set availableKeys = ss.getDocKeys(); // keys of docs for which there are selections
					if (availableKeys != null && availableKeys.size() > 0
						&& !availableKeys.contains(asnHelper.getDefaultDoc())) {
						String key = (String)availableKeys.iterator().next();
						// prtln ("default doc not available using first available key (" + key + ")");
						asnHelper.setStandardsDocument(key);
						sef.setCurrentStdDocKey(key);
					}
				}

				helper.updateStandardsDisplay("selected");
				helper.setSuggestedStandards(null);
			}

		} catch (Exception e) {
			errorMsg = "CAT Service helper error: " + e.getMessage();
			e.printStackTrace();
			throw new ServletException(errorMsg);
		}

		// if a page parameter is supplied, we open that page directly
		String page = null;
		String pathArg = request.getParameter("pathArg");
		if (pathArg != null && pathArg.trim().length() > 0) {
			try {
				String normalizedPath = XPathUtils.normalizeXPath(pathArg);
				page = normalizedPath.split("/")[framework.getBaseRenderLevel() - 1];
			} catch (Throwable e) {
				prtln("couldn't obtain page attribute");
			}
			sef.setHash(CollapseUtils.pathToId(pathArg));
			sef.exposeNode(XPathUtils.decodeXPath(pathArg));
		}

		sef.setCurrentPage(page == null ? firstPage : page);

		return mapping.findForward("editor");
	}


	/**
	 *  Placeholder method to overide that of AbstractSchemEditAction. Never used
	 *  but necessary to avoid compile error.
	 *
	 *@param  mapping               the ActionMapping
	 *@param  form                  the ActionForm
	 *@param  request               the Request
	 *@param  response              the Response
	 *@return                       Description of the Return Value
	 *@exception  ServletException  Description of the Exception
	 */
	protected ActionForward handleNewRecordRequest(ActionMapping mapping,
			ActionForm form,
			HttpServletRequest request,
			HttpServletResponse response)
			 throws ServletException {

		ActionErrors errors = new ActionErrors();

		errors.add("error",
				new ActionError("generic.error", "bad command name: \"newRecord\""));
		saveErrors(request, errors);
		return mapping.findForward("error.page");
	}


	/**
	 *  Saves a metadata record either to disk in the case of a stand-alone
	 *  metadata editor, or to the repository manager in the case of the integrated
	 *  metadata editor.
	 *
	 *@param  mapping                    Description of the Parameter
	 *@param  form                       Description of the Parameter
	 *@param  request                    Description of the Parameter
	 *@exception  Exception              if save operation is unsuccessful
	 *@exception  RecordUpdateException  Description of the Exception
	 */
	protected void putRecord(ActionMapping mapping,
			ActionForm form,
			HttpServletRequest request)
			 throws Exception, RecordUpdateException {

		// prtln ("DCSSchemEditAction.putRecord()");

		String errorMsg = "";
		SchemEditForm sef = (SchemEditForm) form;

		// MetaDataFramework framework = getMetaDataFramework();
		String recId = sef.getRecId();
		Document doc = sef.getDocMap().getDocument();

		this.repositoryService.saveEditedRecord(recId, doc, getSessionUser(request));

		// Finally, perform framework-specific chores ...
		if (sef.getXmlFormat().equals("dlese_collect")) {
			// reload collection Records
			repositoryManager.loadCollectionRecords(true);
		}
	}


	// --------------------  CAT Suggestor handlers -----------------------

	/**
	 *  Gets the assignedStandards attribute of the DCSSchemEditAction object
	 *
	 *@param  mapping               the ActionMapping
	 *@param  form                  the ActionForm
	 *@param  request               the Request
	 *@return                       The assignedStandards value
	 *@exception  ServletException  Description of the Exception
	 */
	private ActionForward getAssignedStandards(ActionMapping mapping,
			ActionForm form,
			HttpServletRequest request)
			 throws ServletException {

		return mapping.findForward("casaa.assignments");
	}


	/**
	 *  Perform a query on the suggestion server and display results.
	 *
	 *@param  mapping               the ActionMapping
	 *@param  form                  the ActionForm
	 *@param  request               the Request
	 *@return                       NOT YET DOCUMENTED
	 *@exception  ServletException  NOT YET DOCUMENTED
	 */
	private ActionForward doUpdateSuggestions(ActionMapping mapping,
			ActionForm form,
			HttpServletRequest request)
			 throws ServletException {

		prtln("doUpdateSuggestions()");

		SchemEditValidator validator = getValidator(form, mapping, request);
		validator.updateMultiValueFields();

		SchemEditForm sef = (SchemEditForm) form;
		ActionErrors errors = new ActionErrors();
		CATServiceHelper helper = null;
		try {
			helper = this.initSuggestionServiceHelper(sef);
		} catch (Exception e) {
			throw new ServletException("unable to instantiate CAT Service helper: " + e.getMessage());
		}
		if (helper.getFrameworkPlugin() == null) {
			prtln("helper does not have framework plugin");
		}
		try {
			helper.updateDisplayControls(request);
		} catch (Exception e) {
			throw new ServletException("unable to update suggestion controls: " + e.getMessage());
		}

		String pathArg = request.getParameter("pathArg");
		String command = request.getParameter("command");
		try {
			if (command.equals("suggestStandards")) {
				helper.updateSuggestions();
			} else if (command.equals("moreLikeThis")) {
				helper.moreLikeThis();
			}
			
			errors.add("message",
					new ActionError("generic.message",
					helper.getSuggestedStandards().size() + " standards have been suggested"));
					
		} catch (Exception e) {
			prtln(e.getMessage());
			// e.printStackTrace();
			
			if ("true".equals(request.getParameter("async"))) {
				// return error as json so it will be displayed in UI
				String errorMsg = "Unable to communicate with Suggestion server";
				if (e.getMessage() != null)
					errorMsg += ": " + e.getMessage();
				sef.setAsyncJason(makeJsonError (errorMsg));
					return mapping.findForward("async.json");
			}
			else {
				errors.add("error",
					new ActionError("generic.message", "Unable to communicate with Suggestion server"));
			}
		}

		saveErrors(request, errors);
		
		if ("true".equals(request.getParameter("async"))) {
			return mapping.findForward("async.standards.display");
		}
		else {
			sef.setHash(CollapseUtils.pathToId(XPathUtils.getParentXPath(pathArg)));
			return getEditorMapping(mapping);
		}
	}


	/**
	 *  Supports asyncronous request in support of the ServiceHelper (to obtain
	 *  values from the current instance document).
	 *
	 *@param  mapping        the ActionMapping
	 *@param  form           the ActionForm
	 *@param  request        the Request
	 *@param  response       the Response
	 *@return                Description of the Return Value
	 *@exception  Exception  Description of the Exception
	 */
	private ActionForward handleAsyncRequest(ActionMapping mapping,
			ActionForm form,
			HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		SchemEditForm sef = (SchemEditForm) form;
		// update the DocMap so we are working with current data from the instance doc
		SchemEditValidator validator = getValidator(form, mapping, request);
		validator.updateMultiValueFields();

		sef.setAsyncJason("");
		CATServiceHelper helper = this.initSuggestionServiceHelper(sef);

		String field = request.getParameter("updatefield");
		Element asyncResponse = null;
		if (field == null) {
			throw new Exception("Update Field expected");
		} else if (field.equals("gradeRanges")) {
			asyncResponse = DocumentHelper.createElement("gradeRanges");
			
			// new approach - selectedOptionValues
			Element selectedValue = asyncResponse.addElement("startGrade");
			selectedValue.setText(helper.getDerivedCATStartGrade());
			selectedValue = asyncResponse.addElement("endGrade");
			selectedValue.setText(helper.getDerivedCATEndGrade());
		} else if (field.equals("keywords")) {
			// String [] gradeRanges = sef.getEnumerationValuesOf(helper.getKeywordPath());
			String[] keywords = helper.getRecordKeywords();
			asyncResponse = DocumentHelper.createElement("keywords");
			if (keywords != null) {
				for (int i = 0; i < keywords.length; i++) {
					Element keyword = DocumentHelper.createElement("keyword");
					keyword.setText(keywords[i]);
					asyncResponse.add(keyword);
				}
			}
		} else if (field.equals("description")) {
			// String [] gradeRanges = sef.getEnumerationValuesOf(helper.getKeywordPath());
			String description = helper.getRecordDescription();
			asyncResponse = DocumentHelper.createElement("description");
			asyncResponse.setText (description);
		} else {
			throw new Exception("unhandled field: " + field);
		}

		Document responseDoc = DocumentHelper.createDocument(asyncResponse);
		// prtln ("\nAsync response" + Dom4jUtils.prettyPrint(responseDoc));
		org.json.JSONObject json = org.json.XML.toJSONObject(responseDoc.asXML());
		// prtln ("\nJson response");
		// prtln(json.toString(2));
		sef.setAsyncJason(json.toString());
		return mapping.findForward("async.json");
	}


	/**
	 *  Compute the hierarchical display of standards according to "displayContent"
	 *  parameter contained in request.<p>
	 *
	 *  First we close all the hierarchical nodes, and then we open as required to
	 *  show those nodes that conform to the displayContent (i.e, "selected",
	 *  "suggested", or "both").
	 *
	 *@param  mapping               the ActionMapping
	 *@param  form                  the ActionForm
	 *@param  request               the Request
	 *@return                       NOT YET DOCUMENTED
	 *@exception  ServletException  NOT YET DOCUMENTED
	 */
	private ActionForward handleStandardsDisplay(ActionMapping mapping,
			ActionForm form,
			HttpServletRequest request)
			 throws ServletException {

		SchemEditForm sef = (SchemEditForm) form;

		ActionErrors errors = new ActionErrors();
		MetaDataFramework metadataFramework = getMetaDataFramework();
		SchemaHelper schemaHelper = metadataFramework.getSchemaHelper();
		CollapseBean collapseBean = sef.getCollapseBean();
		String errorMsg;

		SchemEditValidator validator = getValidator(form, mapping, request);
		validator.updateMultiValueFields();

		CATServiceHelper helper = null;
		try {
			helper = this.initSuggestionServiceHelper(sef);
		} catch (Exception e) {
			throw new ServletException("unable to instantiate CAT Service helper: " + e.getMessage());
		}

		String priorDisplayMode = helper.getDisplayMode();
		String priorDisplayContent = helper.getDisplayContent();
		try {
			helper.updateDisplayControls(request);
		} catch (Exception e) {
			throw new ServletException("unable to update suggestion controls: " + e.getMessage());
		}

		String pathArg = request.getParameter("pathArg");
		String displayMode = request.getParameter("displayMode");
		String displayContent = request.getParameter("displayContent");
		String currentStdDocKey = request.getParameter("currentStdDocKey");

		// only update hierarchical view of tree if
		// - we are displaying the tree now
		// - and we weren't displaying it before
		// - and the content we're displaying has changed
		
		/*
		prtln ("\n===========================");
		prtln ("Display State");
		prtln ("request.pathArg: " + pathArg);
		prtln ("request.displayMode: " + displayMode);
		prtln ("request.displayContent: " + displayContent);
		prtln ("request.currentStdDocKey: " + currentStdDocKey);
		prtln ("");
		prtln ("helper.displayMode: " + helper.getDisplayMode());
		prtln ("helper.displayContent: " + helper.getDisplayContent());
		prtln ("helper.currentDoc: " + helper.getCurrentDoc());
		prtln ("===========\n");
		*/
		
/* 		boolean updateTreeDisplay1 = helper.TREE_MODE.equals(displayMode) &&
				!helper.getDisplayMode().equals(priorDisplayMode) &&
				!helper.getDisplayContent().equals(priorDisplayContent); */
		
		boolean updateTreeDisplay = true;
		
		try {
			// prtln ("calling updateStandardsDisplay ...");
			helper.updateStandardsDisplay(displayContent);
		} catch (Throwable t) {
			prtln("updateStandardsDisplay error: " + t.getMessage());
			throw new ServletException("unable to updateStandardsDisplay: " + t.getMessage());
		}

		
		saveErrors(request, errors);
		
		if ("true".equals(request.getParameter("async"))) {
			return mapping.findForward("async.standards.display");
		}
		else {
			sef.setHash(CollapseUtils.pathToId(XPathUtils.getParentXPath(pathArg)));
			return getEditorMapping(mapping);
		}

	}

	/**
	 *  Compute the hierarchical display of standards according to "displayContent"
	 *  parameter contained in request.<p>
	 *
	 *  First we close all the hierarchical nodes, and then we open as required to
	 *  show those nodes that conform to the displayContent (i.e, "selected",
	 *  "suggested", or "both").
	 *
	 *@param  mapping               the ActionMapping
	 *@param  form                  the ActionForm
	 *@param  request               the Request
	 *@return                       NOT YET DOCUMENTED
	 *@exception  ServletException  NOT YET DOCUMENTED
	 */
	private ActionForward handleResQualBenchmarksDisplay(ActionMapping mapping,
			ActionForm form,
			HttpServletRequest request,HttpServletResponse response)
			 throws ServletException {

		// prtln ("handleResQualBenchmarksDisplay");
		SchemEditForm sef = (SchemEditForm) form;

		ActionErrors errors = new ActionErrors();
		MetaDataFramework metadataFramework = getMetaDataFramework();
		SchemaHelper schemaHelper = metadataFramework.getSchemaHelper();
		CollapseBean collapseBean = sef.getCollapseBean();
		String errorMsg;

		
		try {

			SchemEditValidator validator = getValidator(form, mapping, request);
			validator.updateMultiValueFields();
	
			CATServiceHelper helper = null;
			try {
				helper = this.initSuggestionServiceHelper(sef);
			} catch (Exception e) {
				throw new ServletException("unable to obtain CAT Service helper: " + e.getMessage());
			}
			
			String helperXpath = helper.getXpath();
			// prtln ("helper xpath: " + helperXpath);
			if (helperXpath.startsWith("/record/contentAlignment/representation"))
				return mapping.findForward("resqual.representation.benchmarks");
			else if (helperXpath.startsWith ("/record/contentAlignment/phenomenon"))
				return mapping.findForward("resqual.phenomenon.benchmarks");
			else
				throw new Exception ("could not process helper xpath (" + helperXpath + ")");
			
		} catch (Throwable t) {
			t.printStackTrace();
			throw new ServletException ("resqual benchmarks error: " + t.getMessage());
		}
	}

	
	/**
	 *  Initializes the suggestionServiceHelper for this framework, or returns
	 *
	 *@param  sef            The ActionForm
	 *@return                The suggestionServiceHelper value, or null if this
	 *      framework does not implement a suggestionService
	 *@exception  Exception  if a SuggestServiceHelper cannot be initialized
	 */
	protected CATServiceHelper initSuggestionServiceHelper(SchemEditForm sef) throws Exception {
		// prtln ("\ninitSuggestionServiceHelper()");
		
		// is there a standards manager for this framework?
		if (sef.getFramework().getStandardsManager() == null) {
			return null;
		}
		
		String collection = sef.getCollection();
		try {
			CollectionConfig config = this.collectionRegistry.getCollectionConfig(collection);
			if (!config.getSuggestionsAllowed()) {
				return null;
			}
		} catch (Throwable t) {
			prtlnErr ("could not ascertain if suggestion service is allowed for \"" + collection +
						"\": " + t.getMessage());
			return null;
		}
		
		CATServiceHelper suggestionServiceHelper = sef.getSuggestionServiceHelper();
		if (suggestionServiceHelper == null) {
			// initialize serviceHelper and set sef.currentStdDocKey to that helper's default
			try {
				suggestionServiceHelper = CATServiceHelper.getInstance(sef);
				// prtln("created new ServiceHelper instance");
				if (suggestionServiceHelper instanceof AsnSuggestionServiceHelper) {
					AsnSuggestionServiceHelper asnServiceHelper = (AsnSuggestionServiceHelper) suggestionServiceHelper;
					String stdDocKey = asnServiceHelper.getDefaultDoc();
					sef.setCurrentStdDocKey(stdDocKey);
					// prtln (" .. set SEF.currentStdDoc to " + stdDocKey);
				}
			} catch (Throwable t) {
				prtlnErr("ERROR: could not instantiate Suggestion Service Helper for " + this.getXmlFormat());
				prtlnErr("(" + t.getMessage() + ")");
				t.printStackTrace();
				return null;
			}
		}
		// prtln ("suggestServiceHelper class: " + suggestionServiceHelper.getClass().getName());
		
		/* handle case where current doc has been changed by user */
		if (suggestionServiceHelper instanceof AsnSuggestionServiceHelper) {
			try {
				String currentStdDocKey = sef.getCurrentStdDocKey();
				AsnSuggestionServiceHelper asnServiceHelper = (AsnSuggestionServiceHelper) suggestionServiceHelper;
				if (!asnServiceHelper.getCurrentDoc().equals(currentStdDocKey)) {
					
/*  					 prtln ("\nasnServiceHelper.currentDocKey: " + asnServiceHelper.getCurrentDoc());
					 prtln ("sef.currentStdDocKey: " + currentStdDocKey);
					 prtln ("SETTING ASN DOC TO: " + currentStdDocKey); */
					 
					try {
						asnServiceHelper.setStandardsDocument(currentStdDocKey);
					} catch (Throwable t) {
						prtlnErr("WARNING: Did NOT set standardsDocument: " + t.getMessage());
					}
				}
				// prtln ("\t calling updateSelectedStandardsBean");
				asnServiceHelper.updateSelectedStandardsBean();
			} catch (Throwable t) {
				t.printStackTrace();
				throw new Exception("caught error updating helper: " + t.getMessage());
			}
		}
		
		sef.setSuggestionServiceHelper(suggestionServiceHelper);
		return suggestionServiceHelper;
	}


	/**
	 *  Print a line to standard out.
	 *
	 *@param  s  The String to print.
	 */
	protected void prtln(String s) {
		if (debug) {
			SchemEditUtils.prtln(s, "DCSSchemEditAction");
		}
	}


	/**
	 *  Description of the Method
	 *
	 *@param  s  Description of the Parameter
	 */
	protected void prtlnErr(String s) {
		SchemEditUtils.prtln(s, "DCSSchemEditAction");
	}

}

