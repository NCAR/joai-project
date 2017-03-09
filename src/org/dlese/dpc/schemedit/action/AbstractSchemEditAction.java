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
import org.dlese.dpc.schemedit.action.form.SchemEditForm;

import org.dlese.dpc.schemedit.url.UrlHelper;
import org.dlese.dpc.schemedit.url.DupSim;
import org.dlese.dpc.schemedit.input.SchemEditValidator;
import org.dlese.dpc.schemedit.input.Msp2EditValidator;
import org.dlese.dpc.schemedit.input.SmileEditValidator;
import org.dlese.dpc.schemedit.input.OsmEditValidator;
import org.dlese.dpc.schemedit.input.NsdlAnnoValidator;
import org.dlese.dpc.schemedit.input.SchemEditErrors;
import org.dlese.dpc.schemedit.input.SchemEditActionErrors;
import org.dlese.dpc.schemedit.input.UniqueValueChecker;
import org.dlese.dpc.schemedit.input.InputField;
import org.dlese.dpc.schemedit.config.SchemaPath;
import org.dlese.dpc.schemedit.display.CollapseUtils;
import org.dlese.dpc.xml.schema.SchemaHelper;
import org.dlese.dpc.xml.XPathUtils;
import org.dlese.dpc.xml.Dom4jUtils;
import org.dlese.dpc.vocab.MetadataVocab;
import org.dlese.dpc.index.reader.XMLDocReader;
import org.dlese.dpc.webapps.tools.GeneralServletTools;

import org.dom4j.Document;
import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import org.dom4j.Node;

import java.util.*;
import java.io.IOException;

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
 *  Abstract controller for Metadata Editors.
 *
 * @author    ostwald
 */
public abstract class AbstractSchemEditAction extends DCSAction {

	/**  Description of the Field */
	protected static boolean debug = true;
	/**  where to go when the user clicks "edit" */
	protected String firstPage;
	/**  Description of the Field */
	protected String homePage;

	/**  NOT YET DOCUMENTED */
	protected String xmlFormat = null;


	// --------  Abstract Methods---------

	/**
	 *  Gets the xmlFormat attribute of the AbstractSchemEditAction object, which
	 *  identifies the MetaDataFramework of the record being edited.
	 *
	 * @return    The xmlFormat value
	 */
	protected abstract String getXmlFormat();


	/**
	 *  Gets the metaDataFramework attribute of the record being edited.
	 *
	 * @return    The metaDataFramework value
	 */
	protected MetaDataFramework getMetaDataFramework() {
		return getMetaDataFramework(getXmlFormat());
	}


	/**
	 *  Gets the validator attribute of the AbstractSchemEditAction object
	 *
	 * @param  form     NOT YET DOCUMENTED
	 * @param  mapping  NOT YET DOCUMENTED
	 * @param  request  NOT YET DOCUMENTED
	 * @return          The validator value
	 */
	protected SchemEditValidator getValidator(ActionForm form, ActionMapping mapping, HttpServletRequest request) {
		//prtln ("getValidator(): xmlFormat: " + this.getMetaDataFramework().getXmlFormat());
		SchemEditForm sef = (SchemEditForm) form;
		MetaDataFramework framework = sef.getFramework();
		String xmlFormat = framework.getXmlFormat();

		if (xmlFormat.equals("msp2") || xmlFormat.equals("comm_core") || xmlFormat.equals("math_path"))
			return new Msp2EditValidator(sef, framework, mapping, request);
		else if (xmlFormat.startsWith("smile_item"))
			return new SmileEditValidator(sef, framework, mapping, request);
		else if (xmlFormat.equals("osm"))
			return new OsmEditValidator(sef, framework, mapping, request);
		else if (xmlFormat.equals("nsdl_anno"))
			return new NsdlAnnoValidator(sef, framework, mapping, request);
		else
			return new SchemEditValidator(sef, framework, mapping, request);
	}


	/**
	 *  Abstract call to save record.
	 *
	 * @param  mapping        Description of the Parameter
	 * @param  form           Description of the Parameter
	 * @param  request        Description of the Parameter
	 * @exception  Exception  Description of the Exception
	 */
	protected abstract void putRecord(ActionMapping mapping, ActionForm form,
	                                  HttpServletRequest request)
		 throws Exception;


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
		String errorMsg = null;
		/*
		    Design note:
		    Only one instance of this class gets created for the app and shared by
		    all threads. To be thread-safe, use only local variables, not instance
		    variables (the JVM will handle these properly using the stack). Pass
		    all variables via method signatures rather than instance vars.
		  */
		SchemEditForm sef = (SchemEditForm) form;
		ActionErrors errors = initializeFromContext(mapping, request);
		if (!errors.isEmpty()) {
			saveErrors(request, errors);
			return (mapping.findForward("error.page"));
		}
		sef.setContextURL(GeneralServletTools.getContextUrl(request));

		MetadataVocab vocab = (MetadataVocab) servlet.getServletContext().getAttribute("MetadataVocab");
		sef.setVocab(vocab);

		sef.clearMultiValuesCache();
		sef.setHash("");

		// Query Args
		String command = request.getParameter("command");
		String currentPage = request.getParameter("currentPage");
		String nextPage = request.getParameter("nextPage");
		String pathArg = request.getParameter("pathArg");
		String recId = request.getParameter("recId");

		// show request params excluding collapse info and form input
		SchemEditUtils.showRequestParameters(request, new String[]{"(", "_^_"});

		// show request params including form input
		// SchemEditUtils.showRequestParameters(request, new String[]{"_^_"}); // excluding collapse info


		// ----- framework-dependent -------------
		MetaDataFramework metadataFramework = getMetaDataFramework();
		if (metadataFramework == null) {

			// experimental (9/18/08) most of the time xmlFormat is null is after a timeout experienced
			// from the editor. in this case, just go to the home page
			if (this.getXmlFormat() == null) {
				prtln("xmlFormat is NULL: bailing!");
				return mapping.findForward("browse.home");
			}

			errorMsg = "The \"" + this.getXmlFormat() + "\" metadata framework is not loaded";
			errors.add("actionSetupError", new ActionError("generic.error", errorMsg));
			saveErrors(request, errors);
			return mapping.findForward("error.page");
		}
		sef.setFramework(metadataFramework);

		PageList pageList = metadataFramework.getPageList();
		sef.setPageList(pageList);
		firstPage = pageList.getFirstPage();
		homePage = pageList.getHomePage();
		
		SchemaHelper schemaHelper = metadataFramework.getSchemaHelper();
		SessionBean sessionBean = this.getSessionBean(request);
		SchemEditValidator validator = getValidator(form, mapping, request);

		sef.getCollapseBean().update(request);

		try {

			if (command == null || command.trim().length() == 0) {
				return handleMissingCommand(mapping, form, request, response);
			}

			if (command.equalsIgnoreCase("dirtyDocCheck")) {
				if (!sessionBean.ownsLock(recId)) {
					throw new MissingLockException();
				}
				return isDocDirty(mapping, form, request, validator);
			}

			if (command.equalsIgnoreCase("exit")) {
				if (!sessionBean.ownsLock(recId)) {
					throw new MissingLockException();
				}
				return handleExitCommand(mapping, form, request, validator);
			}

			// guardedExit tries to keep user from exiting without saving changes
			if (command.equalsIgnoreCase("guardedExit")) {
				if (!sessionBean.ownsLock(recId)) {
					throw new MissingLockException();
				}
				return handleGuardedExit(mapping, form, request, validator);
			}
			else {
				// we are not dealing with a guardedExit. clear the guardedExit path
				// so it does not affect the next page
				// prtln ("clearing guardedExitPath");
				sef.setGuardedExitPath(null);
			}

			// commands that do NOT REQUIRE INTERACTION WITH REPOSITORY OR RECORD
			// Best Practices
			// ** FRAMEWORK must be set in form **
			if (command.equalsIgnoreCase("bestpractices")) {
				if (pathArg == null || pathArg.trim().length() == 0) {
					errorMsg = "best practices did not recieve a pathArg!";
					throw new ServletException(errorMsg);
				}
				sef.setFieldInfoReader(sef.getFieldInfo(pathArg));
				return mapping.findForward("best.practices");
			}

			// Commands that DO NOT REQUIRE REPOSITORY - SHOULD ALREADY HAVE LOCK
			// change editor form (the input page)
			else if (command.equalsIgnoreCase("changeForm")) {

				if (!sessionBean.ownsLock(recId)) {
					throw new MissingLockException();
				}

				if (currentPage.equals("fullView")) {
					SchemEditActionErrors sErrors = validator.validateDocument();
					if (sErrors.size() > 0) {
						saveErrors(request, sErrors);
						return getEditorMapping(mapping);
					}
				}

				if (sef.getForceValidation()) {
					prtln("validating form");
					SchemEditActionErrors sErrors = validator.validateForm();
					if (sErrors.size() > 0) {
						saveErrors(request, sErrors);
						return getEditorMapping(mapping);
					}
				}
				else {
					validator.updateMultiValueFields();
				}

				errors = uniqueUrlCheck(mapping, form, request);
				errors.add(uniqueValueCheck(mapping, form, request, validator));

				if (errors.size() > 0) {
					saveErrors(request, errors);
					return getEditorMapping(mapping);
				}

				// if a pathArg is present, expose that element and set the hash so the exposed element
				// will be viewed at top of page
				if (pathArg != null && pathArg.trim().length() > 0) {
					try {
						sef.exposeNode(XPathUtils.decodeXPath(pathArg));
					} catch (org.dom4j.InvalidXPathException e) {
						prtln("exposeNode error: " + e.getMessage());
					}
					sef.setHash(CollapseUtils.pathToId(pathArg));
				}
				sef.setPreviousPage(currentPage);
				sef.setCurrentPage(nextPage);

				return getEditorMapping(mapping);
			}

			// newElement - add element to the instanceDocument
			else if (command.equalsIgnoreCase("newElement")) {

				if (!sessionBean.ownsLock(recId)) {
					throw new MissingLockException();
				}

				// we are NOT validating the input fields before adding a new element.
				validator.updateMultiValueFields();

				try {
					attachNewElement(pathArg, sef, schemaHelper);
				} catch (Exception e) {
					prtln(e.getMessage());
				}

				/*
					pathArg refers to the element that has just been added. if the element is
					a multiselect, then the hash is simply this path. otherwise, the element is indexed
					and the hash must point to the element just added
				  */
				String hash;

				// create decoded, normalized path to query schemaHelper
				String schemaPath = XPathUtils.normalizeXPath(XPathUtils.decodeXPath(pathArg));

				if (schemaHelper.isRepeatingElement(schemaPath)) {
					int hashIndex = XPathUtils.getIndex(pathArg) + 1;
					String elementName = XPathUtils.getNodeName(pathArg);
					hash = XPathUtils.getParentXPath(pathArg) + "/" + elementName + "_" + hashIndex + "_";
				}

				else if (schemaHelper.isChoiceElement(schemaPath)) {
					hash = XPathUtils.getParentXPath(pathArg);
				}

				else {
					hash = pathArg;
				}
				String id = CollapseUtils.pathToId(hash);

				sef.setHash(id);
				sef.getCollapseBean().openElement(id);
				sef.exposeNode(XPathUtils.decodeXPath(hash));

				return getEditorMapping(mapping);
			}

			//  CHIOCE
			else if (command.equalsIgnoreCase("choice")) {
				if (!sessionBean.ownsLock(recId)) {
					throw new MissingLockException();
				}

				if ((currentPage == null) || (currentPage.trim().length() == 0)) {
					errorMsg = "choice: currentPage not found in request";
					prtln(errorMsg);
					throw new ServletException(errorMsg);
				}
				
				//  we are NOT validating the input fields before adding a new element.
				validator.updateMultiValueFields();
				
				// guard against adding an illegal choice caused by refreshing the page after adding a choice
				if (sef.getAcceptsNewChoice(XPathUtils.getParentXPath(pathArg)).equals(sef.FALSE)) {
					errors.add("error",
						new ActionError("generic.error", "schema does not allow new element at " + pathArg));
					saveErrors(request, errors);
					return getEditorMapping(mapping);
				}

				try {
					/* 	we strip the leaf index from the path because with choices, we want the new choice
						to go at the end of the element, and the provided index does not make sense to
						docMap/targetedInsert, which expects the target to exist...
					*/
					attachNewElement(XPathUtils.getSiblingXPath(pathArg), sef, schemaHelper);
				} catch (Exception e) {
					prtln(e.getMessage());
				}

				// we want to hash to the chosen (new) element
				prtln("pathArg: " + pathArg);
				String id = CollapseUtils.pathToId(pathArg);

				sef.setHash(CollapseUtils.pathToId(id));
				sef.getCollapseBean().openElement(id);
				sef.exposeNode(XPathUtils.decodeXPath(pathArg));
				return getEditorMapping(mapping);
			}

			//  DELETE an element
			else if (command.equalsIgnoreCase("deleteElement")) {
				if (!sessionBean.ownsLock(recId)) {
					throw new MissingLockException();
				}

				return handleDeleteElement(mapping, form, request, validator);
			}
			/*
				VALIDATE
			  */
			else if (command.equalsIgnoreCase("validate")) {
				// Validate
				if (!sessionBean.ownsLock(recId)) {
					throw new MissingLockException();
				}

				SchemEditActionErrors sErrors = validator.validateForm();
				sErrors.add(uniqueUrlCheck(mapping, form, request));
				sErrors.add(uniqueValueCheck(mapping, form, request, validator));

				if (sErrors.size() > 0) {
					saveErrors(request, sErrors);
				}
				else {
					errors.add("message",
						new ActionError("validation.confirmation"));
					saveErrors(request, errors);
				}
				return getEditorMapping(mapping);
			}

			// commands that REQUIRE ACCESSING REPOSITORY - MUST OBTAIN LOCK

			else if (command.equalsIgnoreCase("edit")) {
				try {
					return handleEditRequest(mapping, (SchemEditForm) form, request, response);
				} catch (Exception e) {
					errors.add("error",
						new ActionError("generic.error", "The specified record cannot be edited:\n" + e.getMessage()));
					saveErrors(request, errors);
					return mapping.findForward("error.page");
				}
			}

			else if (command.equalsIgnoreCase("newRecord")) {
				return handleNewRecordRequest(mapping, form, request, response);
			}

			// CANCEL - exit from editor
			else if (command.equalsIgnoreCase("cancel")) {
				return handleCancelRequest(mapping, form, request, response);
				// release lock on record
			}

			// SAVE - save document and return to editor
			else if (command.equalsIgnoreCase("save")) {
				return handleSaveRequest(mapping, form, request, response, validator);
			}

			return handleOtherCommands(mapping, form, request, response);
		} catch (StackOverflowError soe) {
			prtln("Stack Overflow!");
			// soe.printStackTrace();
			errors.add("error",
				new ActionError("generic.error", "The system is unable to process a very long element value"));
			saveErrors(request, errors);
			return mapping.findForward("error.page");
		} catch (MissingLockException mle) {
			return handleMissingLockException(mle, mapping, form, request, response);
		} catch (Throwable e) {
			prtln("Exception: " + e);
			e.printStackTrace();
			errors.add("error",
				new ActionError("generic.error", "There has been a system error: " + e.getMessage()));
			saveErrors(request, errors);
			return mapping.findForward("error.page");
		}

	}


	/**
	 *  Checks each field in the instance document associated with a configured
	 *  "uniqueValue" path for duplicate values in the same field in other records
	 *  in the collection.
	 *
	 * @param  mapping    the actionMapping
	 * @param  form       the actionForm
	 * @param  request    the request
	 * @param  validator  the validator for this form
	 * @return            errors resulting from uniqueValueCheck
	 */
	protected ActionErrors uniqueValueCheck(ActionMapping mapping,
	                                        ActionForm form,
	                                        HttpServletRequest request,
	                                        SchemEditValidator validator) {

		SchemEditActionErrors errors = new SchemEditActionErrors();
		SchemEditForm sef = (SchemEditForm) form;
		MetaDataFramework framework = getMetaDataFramework();
		UniqueValueChecker uniqueValueChecker = null;
		try {
			uniqueValueChecker = new UniqueValueChecker(getServlet().getServletContext());
		} catch (Exception e) {
			prtlnErr("Could not instantiate ValueChecker: " + e.getMessage());
			e.printStackTrace();
			return errors;
		}

		String recId = sef.getRecId();
		XMLDocReader docReader = null;
		try {
			docReader = this.repositoryService.getXMLDocReader(sef.getRecId());
		} catch (Exception e) {
			prtln("docReader not found for " + sef.getRecId() + ": " + e.getMessage());
			return errors;
		}

		try {
			List uniqueValuePaths = framework.getSchemaPathMap().getSchemaPathsByValueType("uniqueValue");
			if (uniqueValuePaths.isEmpty()) {
				return errors;
			}
			// prtln("\t" + uniqueValuePaths.size() + " uniqueValuePaths found");
			for (Iterator i = uniqueValuePaths.iterator(); i.hasNext(); ) {

				SchemaPath schemaPath = (SchemaPath) i.next();
				String path = schemaPath.xpath;
				// prtln("\nuniqueValuePath: " + path);
				/*
				   we have a normalized path,
				   - now test all the values for that path for uniqueness
				   - Iterate through the InputFields to find the values (and xpaths)
				     for the "uniqueValue" field.
					 The (indexed) xpath allows us to associated feedback (in the UI) 
					 with the offending input element.
				*/
				Iterator fieldIter = validator.getInputManager().getInputFields().iterator();
				while (fieldIter.hasNext()) {
					InputField inputField = (InputField) fieldIter.next();
					if (inputField.getNormalizedXPath().equals(path)) {
						String fieldXPath = inputField.getXPath();
						String fieldValue = inputField.getValue();
						List dups = uniqueValueChecker.getDupValues(fieldValue, docReader, path);

						for (Iterator dupsIter = dups.iterator(); dupsIter.hasNext(); ) {
							String dupRecId = (String) dupsIter.next();
							SchemEditErrors.addDuplicateValueError(errors, inputField, dupRecId);
							sef.exposeNode(fieldXPath);
						}
					}
				}
			}
		} catch (Throwable t) {
			prtlnErr("uniqueValueCheck Error: " + t.getMessage());
			// t.printStackTrace();
		}
		return errors;
	}


	/**
	 *  Check values of schemapaths designated as "uniqueUrl"s for duplicate or
	 *  similiar values within given collection. Called before executing "save" and
	 *  "changeForm" commands
	 *
	 * @param  mapping  the actionMapping
	 * @param  form     the actionForm
	 * @param  request  the request
	 * @return          errors resulting from uniqueUrlCheck
	 */
	protected ActionErrors uniqueUrlCheck(ActionMapping mapping,
	                                      ActionForm form,
	                                      HttpServletRequest request) {

		SchemEditActionErrors errors = new SchemEditActionErrors();
		SchemEditForm sef = (SchemEditForm) form;
		MetaDataFramework framework = getMetaDataFramework();

		boolean doSimilarCheck = true;
		boolean doDupCheck = true;
		sef.setValidatedUrl(null);
		sef.setSimilarUrlRecs(null);
		sef.setDuplicateUrlRecs(null);
		try {
			String uniqueUrlPath = framework.getUniqueUrlPath();
			if (uniqueUrlPath == null) {
				return errors;
			}

			// do we need to check the url?
			// - if it null or empty we don't bother checking (this assumes empty urls are okay)
			// - otherwise we check against cached url

			String currentUrl = (String) sef.getDocMap().get(uniqueUrlPath);
			if (currentUrl == null || currentUrl.length() == 0) {
				return errors;
			}

			// get cached Url from the savedContent map, in which values are stored in Lists that
			// are mapped from xpaths.
			// So we pluck the first value (if there is one)
			String cachedUrl = this.getCachedValue(uniqueUrlPath, sef);
			if (currentUrl.equals(cachedUrl)) {
				// prtln("\t url has not been changed ... returning");
				return errors;
			}

			// no element found at uniqueUrlPath in savedContent - check for dups unless dupCheck is disabled
			if (!doDupCheck) {
				prtln("UniqueUrlCheck: dupCheck is disabled, returning ...");
				return errors;
			}

			// if the url is a duplicate, then create errors and return
			List dups = repositoryService.getDups(currentUrl, sef.getCollection());
			if (dups != null && dups.size() > 0) {
				sef.setDups(dups);
				SchemEditErrors.addDuplicateUrlError(errors, uniqueUrlPath, "duplicate.url.error");
				return errors;
			}

			// no duplicates found, now check for similar urls (if enabled)
			if (!doSimilarCheck) {
				return errors;
			}

			// the currentUrl is valid (it has no dups although there may be similars in primary or mirror fields)
			sef.setValidatedUrl(currentUrl);

			List sims = repositoryService.getSims(currentUrl, sef.getCollection());
			// remove self from sims
			for (int i = sims.size() - 1; i > -1; i--) {
				DupSim dupSim = (DupSim) sims.get(i);
				if (dupSim.getId().equals(sef.getRecId())) {
					sims.remove(i);
				}
			}

			// prtln (sims.size() + " similar urls found");
			sef.setSims(sims);
			if (sims.size() > 0) {
				SchemEditErrors.addSimilarUrlError(errors, uniqueUrlPath, "unique.url.required");
				return errors;
			}
		} catch (Throwable t) {
			prtln(t.getMessage());
			t.printStackTrace();
		}
		return errors;
	}


	/**
	 *  Gets the cachedValue from the savedContentMap<p>
	 *
	 *  The savedContentMap stores a List of values for each xpath, so we simply
	 *  pluck the first.
	 *
	 * @param  xpath  path at which to retrieve value
	 * @param  sef    NOT YET DOCUMENTED
	 * @return        The cachedValue value
	 */
	String getCachedValue(String xpath, SchemEditForm sef) {
		List valueList = (List) sef.getSavedContent().get(xpath);

		// we can only check against the savedContent if there is any
		if (valueList != null && valueList.size() != 0) {
			return (String) valueList.get(0);
		}
		return null;
	}


	/**
	 *  Check to see if the document has been modified in editor. If so, return
	 *  control to editor and prompt user to save.
	 *
	 * @param  mapping  the actionMapping
	 * @param  form     the actionForm
	 * @param  request  the request
	 * @param  validator             Description of the Parameter
	 * @return                       Description of the Return Value
	 * @exception  ServletException  NOT YET DOCUMENTED
	 */
	protected ActionForward isDocDirty(ActionMapping mapping,
	                                   ActionForm form,
	                                   HttpServletRequest request,
	                                   SchemEditValidator validator) throws ServletException {

		// prtln("checking for dirty doc");
		SchemEditForm sef = (SchemEditForm) form;
		ActionErrors errors = new ActionErrors();
		validator.updateMultiValueFields();

		Map currentContent = DocContentMap.getDocContentMap(sef.getDocMap().getDocument(),
			getMetaDataFramework());

		boolean docIsDirty = !currentContent.equals(sef.getSavedContent());
		// prtln("  DIRTY? " + docIsDirty);
		// debugging - show diff - DISABLED
		if (false && docIsDirty) {
			DocContentMap.cmpMaps(currentContent, sef.getSavedContent());
		}

		Element asyncResponse = org.dom4j.DocumentHelper.createElement("docIsDirty");
		asyncResponse.setText(docIsDirty ? sef.TRUE : sef.FALSE);

		Document responseDoc = org.dom4j.DocumentHelper.createDocument(asyncResponse);
		// prtln ("\nAsync response" + Dom4jUtils.prettyPrint(responseDoc));
		try {
			org.json.JSONObject json = org.json.XML.toJSONObject(responseDoc.asXML());
			// prtln ("\nJson response");
			// prtln(json.toString(2));
			sef.setAsyncJason(json.toString());
		} catch (Throwable t) {
			throw new ServletException(t);
		}
		return mapping.findForward("async.json");
	}


	/**
	 *  Check to see if the document has been modified in editor. If so, return
	 *  control to editor and prompt user to save.
	 *
	 * @param  mapping  the actionMapping
	 * @param  form     the actionForm
	 * @param  request  the request
	 * @param  validator             Description of the Parameter
	 * @return                       Description of the Return Value
	 * @exception  ServletException  NOT YET DOCUMENTED
	 */
	protected ActionForward handleGuardedExit(ActionMapping mapping,
	                                          ActionForm form,
	                                          HttpServletRequest request,
	                                          SchemEditValidator validator) throws ServletException {
		SchemEditForm sef = (SchemEditForm) form;
		ActionErrors errors = new ActionErrors();
		validator.updateMultiValueFields();

		String forwardPath = request.getParameter("pathArg");

		String confirmedExitPath = request.getParameter("confirmedExit");
		if (confirmedExitPath == null)
			throw new ServletException("Required parameter \'confirmedExit\' was not found");
		boolean confirmedExit = (request.getParameter("confirmedExit").equals("true"));

		Map currentContent = DocContentMap.getDocContentMap(sef.getDocMap().getDocument(),
			getMetaDataFramework());

		boolean docIsDirty = !currentContent.equals(sef.getSavedContent());

		// debugging - show diff - DISABLED
		if (false && docIsDirty) {
			DocContentMap.cmpMaps(currentContent, sef.getSavedContent());
		}

		if (docIsDirty && !confirmedExit) {
			sef.setGuardedExitPath(forwardPath);
			// here we should check to see if the record exists in the index. if NOT, then it
			// is new and must be explicitly saved or it will be lost altogether!
			// NOTE: DCSSchemEditAction works on the index, while StandAloneSchemEditAction does not,
			// so we have to check first that we are dealing with an indexed record
			if ((this instanceof DCSSchemEditAction) && repositoryManager.getRecord(sef.getRecId()) == null) {
				errors.add("error",
					new ActionError("generic.message", "Record has not been saved and therefore will be lost"));
			}
			else {
				errors.add("error",
					new ActionError("generic.message", "Record contains unsaved changes"));
			}
			saveErrors(request, errors);
			return getEditorMapping(mapping);
		}
		else {
			String recId = sef.getRecId();

			// RELEASE LOCK
			SessionBean sessionBean = this.getSessionBean(request);
			sessionBean.releaseLock(recId);
			sef.clear();
			sef.setGuardedExitPath(null);
			if (forwardPath == null || forwardPath.trim().length() == 0 || forwardPath.equals("forwardToCaller")) {
				// prtln("no forward path supplied .. forward to Caller");
				return SchemEditUtils.forwardToCaller(request, recId, sessionBean);
			}
			else {
				return new ActionForward(forwardPath);
			}
		}
	}


	protected ActionForward handleExitCommand(ActionMapping mapping,
	                                          ActionForm form,
	                                          HttpServletRequest request,
	                                          SchemEditValidator validator) throws ServletException {
		SchemEditForm sef = (SchemEditForm) form;
		String forwardPath = request.getParameter("pathArg");
		String recId = sef.getRecId();

		// RELEASE LOCK
		SessionBean sessionBean = this.getSessionBean(request);
		sessionBean.releaseLock(recId);
		sef.clear();
		if (forwardPath == null || forwardPath.trim().length() == 0 || forwardPath.equals("forwardToCaller")) {
			// prtln("no forward path supplied .. forward to Caller");
			return SchemEditUtils.forwardToCaller(request, recId, sessionBean);
		}
		else {
			return new ActionForward(forwardPath);
		}
	}



	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  mapping  the actionMapping
	 * @param  form     the actionForm
	 * @param  request  the request
	 * @return                       NOT YET DOCUMENTED
	 * @exception  ServletException  NOT YET DOCUMENTED
	 */
	protected ActionForward handleTimedOutGuardedExit(ActionMapping mapping,
	                                                  ActionForm form,
	                                                  HttpServletRequest request) throws ServletException {
		SchemEditForm sef = (SchemEditForm) form;
		ActionErrors errors = new ActionErrors();

		String forwardPath = request.getParameter("pathArg");
		if (forwardPath == null || forwardPath.trim().length() == 0) {
			prtln("ERROR: forwardPath was not supplied");
		}
		else {
			// prtln ("forwardPath: " + forwardPath);
		}

		String recId = "";
		sef.setRecId(recId);
		// clear exitPath
		sef.setGuardedExitPath(null);
		if (forwardPath == null || forwardPath.trim().length() == 0 || forwardPath.equals("forwardToCaller")) {
			// prtln("no forward path supplied .. forward to Caller");
			return SchemEditUtils.forwardToCaller(request, recId, getSessionBean(request));
		}
		else {
			// prtln ("forwarding to " + forwardPath);
			return new ActionForward(forwardPath);
		}
	}


	/**
	 *  Determines the appropriate editor mapping depending on whether the editor
	 *  is configured to use a frame-based display.
	 *
	 * @param  mapping  Description of the Parameter
	 * @return          The editorMapping value
	 */
	protected ActionForward getEditorMapping(ActionMapping mapping) {
		return mapping.findForward("editor");
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  mle                   the missingLockException we are handling
	 * @param  mapping  the actionMapping
	 * @param  form     the actionForm
	 * @param  request  the request
	 * @param  response              NOT YET DOCUMENTED
	 * @return                       NOT YET DOCUMENTED
	 * @exception  ServletException  NOT YET DOCUMENTED
	 */
	protected ActionForward handleMissingLockException(MissingLockException mle,
	                                                   ActionMapping mapping,
	                                                   ActionForm form,
	                                                   HttpServletRequest request,
	                                                   HttpServletResponse response)
		 throws ServletException {

		String command = request.getParameter("command");
		ActionErrors errors = new ActionErrors();
		boolean isTimedOut = false;

		SessionBean sessionBean = this.getSessionBean(request);
		if (sessionBean == null) {
			prtln("WARNING: handleMissingLockException() cannot find sessionBean");
			isTimedOut = true;
		}
		else if (sessionBean.getNumSecsToTimeout() < 0 || sessionBean.isNew()) {
			prtln("seession has timed out");
			isTimedOut = true;
			sessionRegistry.unregisterSessionBean(sessionBean.getId());
		}
		else {
			prtln("session has not timed out (" + sessionBean.getNumSecsToTimeout() + " secs to go)");
		}

		prtln("handleMissingLockException() . . .");
		prtln(". . . command: " + command);
		prtln(". . . message: " + mle.getMessage());
		prtln("isTimedOut: " + isTimedOut);

		if (command.equals("exit")) {
			/*
				for guardedExit, we want to let the navigational requests (browse, search, view) go through,
				but others (copyRecord, new, delete) we put up a session time out sign if possible and if not
				we put up general-purpose (missingLock) message.
				i.e., if exitCommand contains {"new", "copyRecord", ...) - then show message,
					otherwise, simply forward to location specified by pathArg
			*/
			String[] illegalExitArgs = {"new", "copyRecord", "move", "delete"};
			List illegalExitCommands = Arrays.asList(illegalExitArgs);

			String pathArg = request.getParameter("pathArg");
			String exitCommand = null;
			if (pathArg != null && pathArg.trim().length() > 0) {
				exitCommand = UrlHelper.getParamValue("command", pathArg);
				prtln("exitCommand: " + exitCommand);
				if (!illegalExitCommands.contains(exitCommand)) {
					prtln("safe exit granted");
					// return handleTimedOutGuardedExit(mapping, form, request);
					return handleExitCommand(mapping, form, request, null);
				}
			}
		}

		if (isTimedOut) {
			// give timed-out message - send back to collections home
			prtln("timed-out: give timed-out message");
			errors.add("missingLock",
				new ActionError("session.timeout.msg", command));
		}
		else {
			prtln("not timed out: assume navigation error");
			errors.add("missingLock",
				new ActionError("editor.re.entry.msg", command));
		}

		saveErrors(request, errors);
		return mapping.findForward("error.page");
	}


	/**
	 *  A hook for extensions of AbstractSchemEditAction to handle misc
	 *  unanticipated requests.
	 *
	 * @param  mapping  the actionMapping
	 * @param  form     the actionForm
	 * @param  request  the request
	 * @param  validator             NOT YET DOCUMENTED
	 * @return                       Description of the Return Value
	 * @exception  ServletException  Description of the Exception
	 */
	protected ActionForward handleDeleteElement(ActionMapping mapping,
	                                            ActionForm form,
	                                            HttpServletRequest request,
	                                            SchemEditValidator validator)
		 throws ServletException {

		SchemEditForm sef = (SchemEditForm) form;
		ActionErrors errors = new ActionErrors();
		MetaDataFramework metadataFramework = getMetaDataFramework();
		SchemaHelper schemaHelper = metadataFramework.getSchemaHelper();
		String errorMsg;

		String currentPage = request.getParameter("currentPage");
		String pathArg = request.getParameter("pathArg");

		if ((currentPage == null) || (currentPage.trim().length() == 0)) {
			errorMsg = "delete: currentPage not found in request";
			prtln(errorMsg);
			throw new ServletException(errorMsg);
		}

		/*
			we are NOT validating the input fields before deletion. but we have to process the
			MultiValue fields to preserve any edits that have been made.
		  */
		validator.updateMultiValueFields();

		/*
			if patharg refers to the last of a required repeating element
				add an empty element
				give user a message to this effect
				collapse parent of deleted Element
				zoom to top
			else
				parentPath
				give user message
				zoom to element
		*/
		boolean nodeIsAnyType = schemaHelper.isAnyTypeElement(pathArg);
		if (nodeIsAnyType) {
			pathArg = schemaHelper.encodeAnyTypeXpath(XPathUtils.decodeXPath(pathArg));
		}

		if (validator.isLastRequiredRepeatingElement(pathArg)) {
			// prtln("isLastRequiredRepeatingElement() returned true");
			sef.getDocMap().removeElement(pathArg);
			try {
				// attachNewElement(pathArg, sef, schemaHelper);
				attachNewElement(XPathUtils.getSiblingXPath(pathArg), sef, schemaHelper);
			} catch (Exception e) {
				errorMsg = "failed to attach new element after deleting last required repeating element at " + pathArg;
				throw new ServletException(errorMsg);
			}
			String parentPath = XPathUtils.getParentXPath(pathArg);
			sef.getCollapseBean().closeElement(CollapseUtils.pathToId(pathArg));
			errors.add("message",
				new ActionError("required.repeating.element.confirmation",
				XPathUtils.decodeXPath(pathArg),
				XPathUtils.getNodeName(pathArg)));
		}
		else {
			// normal case
			sef.getDocMap().removeElement(pathArg);
			errors.add("message",
				new ActionError("element.deleted.confirmation", XPathUtils.decodeXPath(pathArg)));
			/*	set hash so user is taken to context of delete operation
				for multi-select objects, pathArg refers to the PARENT of the item deleted, and therefore
				hash can use this path. For indexed items, however, pathArg refers to the item deleted and
				hash must refer to the PARENT of the deleted item
			  */
			String hashPath = null;
			if (nodeIsAnyType) {
				pathArg = schemaHelper.decodeAnyTypeXpath(pathArg);
			}

			int index = XPathUtils.getIndex(pathArg);
			// getIndex returns 0 when there is no indexing
			if (index > 0) {
				String siblingPath = XPathUtils.getSiblingXPath(pathArg);
				int n = sef.getDocMap().selectNodes(siblingPath).size();
				if (n == 0) {
					hashPath = XPathUtils.getParentXPath(pathArg);
				}
				else if (index > 1) {
					hashPath = siblingPath + "_" + (index - 1) + "_";
				}
				else {
					// effectively moving down, but since we just deleted
					//  index, the one below is now index.
					hashPath = siblingPath + "_" + (index) + "_";
				}
			}
			else {
				hashPath = XPathUtils.getParentXPath(pathArg);
			}
			sef.setHash(CollapseUtils.pathToId(hashPath));
		}

		saveErrors(request, errors);
		return getEditorMapping(mapping);
	}


	/**
	 *  A hook for extensions of AbstractSchemEditAction to handle misc
	 *  unanticipated requests.
	 *
	 * @param  mapping  the actionMapping
	 * @param  form     the actionForm
	 * @param  request  the request
	 * @param  response                  Description of the Parameter
	 * @return                           Description of the Return Value
	 * @exception  ServletException      Description of the Exception
	 * @exception  MissingLockException  NOT YET DOCUMENTED
	 */
	protected ActionForward handleOtherCommands(ActionMapping mapping,
	                                            ActionForm form,
	                                            HttpServletRequest request,
	                                            HttpServletResponse response)
		 throws ServletException, MissingLockException {

		String command = request.getParameter("command");
		ActionErrors errors = new ActionErrors();

		prtln("unknown command");
		errors.add("message",
			new ActionError("unrecognized.command", command));
		saveErrors(request, errors);

		return getEditorMapping(mapping);
	}



	/**
	 *  Handle the case where the request contains a command that is not processed
	 *  by this Class by calling the "handleMissingCommand" method of a subclass.
	 *
	 * @param  mapping               the ActionMapping
	 * @param  form                  the ActionForm
	 * @param  request               the Request
	 * @param  response              the Response
	 * @return                       Description of the Return Value
	 * @exception  ServletException  If the subclass does not handle the command
	 */
	protected abstract ActionForward handleMissingCommand(
	                                                      ActionMapping mapping,
	                                                      ActionForm form,
	                                                      HttpServletRequest request,
	                                                      HttpServletResponse response)
		 throws ServletException;


	/**
	 *  Loads a record into the MetaDataEditor
	 *
	 * @param  mapping               the ActionMapping
	 * @param  form                  the ActionForm
	 * @param  request               the Request
	 * @param  response              the Response
	 * @return                       Description of the Return Value
	 * @exception  ServletException  Description of the Exception
	 */
	protected abstract ActionForward handleEditRequest(ActionMapping mapping,
	                                                   ActionForm form,
	                                                   HttpServletRequest request,
	                                                   HttpServletResponse response)
		 throws ServletException;


	/**
	 *  Cancels editing of current metadata record.
	 *
	 * @param  mapping               the ActionMapping
	 * @param  form                  the ActionForm
	 * @param  request               the Request
	 * @param  response              the Response
	 * @return                       NOT YET DOCUMENTED
	 * @exception  ServletException  NOT YET DOCUMENTED
	 */
	protected abstract ActionForward handleCancelRequest(ActionMapping mapping,
	                                                     ActionForm form,
	                                                     HttpServletRequest request,
	                                                     HttpServletResponse response)
		 throws ServletException;


	/**
	 *  Description of the Method
	 *
	 * @param  mapping               the ActionMapping
	 * @param  form                  the ActionForm
	 * @param  request               the Request
	 * @param  response              the Response
	 * @return                       Description of the Return Value
	 * @exception  ServletException  Description of the Exception
	 */

	protected abstract ActionForward handleNewRecordRequest(ActionMapping mapping,
	                                                        ActionForm form,
	                                                        HttpServletRequest request,
	                                                        HttpServletResponse response)
		 throws ServletException;


	/**
	 *  Description of the Method
	 *
	 * @param  mapping                   the ActionMapping
	 * @param  form                      the ActionForm
	 * @param  request                   the Request
	 * @param  response                  the Response
	 * @param  validator                 Description of the Parameter
	 * @return                           Description of the Return Value
	 * @exception  ServletException      Description of the Exception
	 * @exception  MissingLockException  Description of the Exception
	 */
	protected abstract ActionForward handleSaveRequest(ActionMapping mapping,
	                                                   ActionForm form,
	                                                   HttpServletRequest request,
	                                                   HttpServletResponse response,
	                                                   SchemEditValidator validator)
		 throws ServletException, MissingLockException;


	/**
	 *  Create a new element and attach it to the instance document
	 *
	 * @param  pathArg        path of element to be attached
	 * @param  sef            the form bean
	 * @param  schemaHelper   the schemaHelper
	 * @exception  Exception  if new element could not be created and attached
	 */
	protected void attachNewElement(String pathArg, SchemEditForm sef, SchemaHelper schemaHelper)
		 throws Exception {
		// prtln ("attachNewElement() patharg: " + pathArg);

		Element newElement = schemaHelper.getNewElement(pathArg);
		if (newElement == null) {
			throw new Exception("getNewElement failed");
		}
		else {
			if (!sef.getDocMap().addElement(newElement, pathArg)) {
				throw new Exception("docMap.addElement failed");
			}
		}
	}


	/**
	 *  Print a line to standard out.
	 *
	 * @param  s  The String to print.
	 */
	protected void prtln(String s) {
		if (debug) {
			System.out.println("AbstractSchemEditAction: " + s);
		}
	}


	protected void prtlnErr(String s) {
		System.out.println("AbstractSchemEditAction: " + s);
	}

}

