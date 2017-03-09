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
import org.dlese.dpc.schemedit.display.CollapseBeanInitializer;
import org.dlese.dpc.schemedit.display.CollapseUtils;
import org.dlese.dpc.schemedit.dcs.*;
import org.dlese.dpc.schemedit.action.form.SchemEditForm;


import org.dlese.dpc.repository.RepositoryManager;
import org.dlese.dpc.index.SimpleLuceneIndex;

import org.dlese.dpc.xml.schema.SchemaHelper;
import org.dlese.dpc.xml.*;
import org.dlese.dpc.util.*;

import org.dom4j.Document;
import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import org.dom4j.Node;

import java.util.*;
import java.text.*;
import java.io.File;
import java.io.IOException;
import java.net.URL;

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
 *  Metadata Editor controller for xml records to be created and edited in a
 *  particular disk directory rather than reading and writing to a indexed repository.
 *
 *@author    ostwald
 */
public class StandAloneSchemEditAction extends AbstractSchemEditAction {

	/**
	 *  Description of the Field
	 */
	protected static boolean debug = true;
	protected File recordsDir;

	/**
	 *  Gets the xmlFormat attribute of the StandAloneSchemEditAction object
	 *
	 *@return    The xmlFormat value
	 */

	protected String getXmlFormat() {
		return "";
	}
	

	public ActionForward execute (
			ActionMapping mapping,
			ActionForm form,
			HttpServletRequest request,
			HttpServletResponse response)
		throws IOException, ServletException {

		SchemEditForm sef = (SchemEditForm) form;
		ActionErrors errors = initializeFromContext (mapping, request);
		if (!errors.isEmpty()) {
			saveErrors (request, errors);
			return (mapping.findForward("error.page"));
		}
		
		String errorMsg;
		recordsDir = getRecordsDir();
		if (recordsDir == null) {
			errorMsg = "attribute \"recordsDir\" not found";
			prtln(errorMsg);
			throw new ServletException(errorMsg);
		}
		sef.setRecordsDir(recordsDir);
		
		return super.execute (mapping, form, request, response);
	}
	

	/**
	 *  Gets the recordsDir attribute of the StandAloneSchemEditAction object
	 *
	 *@return    The recordsDir value
	 */
	protected File getRecordsDir() {
		String path = getMetaDataFramework().getRecordsDir();
		File file = null;
		try {
			if (path == null || path.length() == 0) {
				throw new Exception("no records dir registered in the MetadataFramework .. returning null");
			}

			file = new File(getMetaDataFramework().getRecordsDir());
			if (!file.exists()) {
				throw new Exception("got a non-existing path from MetadataFramework: " + path);
			}
		} catch (Exception e) {
			prtln("getRecordsDir() error: " + e.getMessage());
			return null;
		}
		return file;
	}
	/**
	 *  Obtain a record via Web Service.
	 *
	 *@param  recId                 Description of the Parameter
	 *@return                       Description of the Return Value
	 *@exception  ServletException  Description of the Exception
	 */

	protected Document getRemoteRecord(String recId)
		throws ServletException {
		if (true) {
			String errorMsg = "getRemoteRecord not handled by this application";
			throw new ServletException(errorMsg);
		}
		return null;
	}
	
	/**
	 *  Gets the sampleFile attribute of the StandAloneSchemEditAction object
	 *
	 *@return    The sampleFile value
	 */
/* 	protected File getSampleFile() {
		return new File(getMetaDataFramework().getSampleRecordFile());
	} */


	/**
	 *  Requests without a command forward control to the home page for this
	 *  framework.
	 *
	 *@param  mapping               Description of the Parameter
	 *@param  form                  Description of the Parameter
	 *@param  request               Description of the Parameter
	 *@param  response              Description of the Parameter
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

		prtln("no command specified - returning homePage (" + homePage + ")");
		sef.setRecId("");
		sef.setCollection("");
		return mapping.findForward("error.page");
	}


	/**
	 *  Description of the Method
	 *
	 *@param  mapping               Description of the Parameter
	 *@param  form                  Description of the Parameter
	 *@param  request               Description of the Parameter
	 *@param  response              Description of the Parameter
	 *@return                       Description of the Return Value
	 *@exception  ServletException  Description of the Exception
	 */
	protected ActionForward handleOtherCommands(ActionMapping mapping,
			ActionForm form,
			HttpServletRequest request,
			HttpServletResponse response)
		throws ServletException, MissingLockException {
		//----------------------------------------------------------------
		// additional commands we want to handle

		SchemEditForm sef = (SchemEditForm) form;
		MetaDataFramework metadataFramework = getMetaDataFramework();
		SchemaHelper schemaHelper = metadataFramework.getSchemaHelper();
		SchemEditValidator validator = new SchemEditValidator(sef, metadataFramework, mapping, request);
		String command = request.getParameter("command");
		String recId = request.getParameter("recId");
		String errorMsg = "";

		ActionErrors errors = new ActionErrors();

		// now obsolete since editor no longer runs in stand-alone mode
		if (command.equalsIgnoreCase("changeID")) {
			return handleChangeIdRequest(mapping, sef, request, schemaHelper, validator);
		}

		if (command.equalsIgnoreCase("viewxml")) {
			if (recId == null || recId.trim().length() == 0) {
				errorMsg = "viewxml: no recId supplied";
				prtln(errorMsg);
				throw new ServletException(errorMsg);
			}
			File xmlfile = new File(sef.getRecordsDir(), recId + ".xml");
			String xmlVersionString = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
			Document doc = null;

			try {
				doc = Dom4jUtils.getXmlDocument(xmlfile);
			} catch (Exception e) {
				errorMsg = "failed to get document for " + recId + "\n" + e;
				prtln(errorMsg);
				throw new ServletException(errorMsg);
			}
			String metadata = xmlVersionString + Dom4jUtils.prettyPrint(doc);
			sef.setMetadata(metadata);
			response.setContentType("text/plain");
			return mapping.findForward("view.xml");
		}

		// write record and return to index
/* 		if (command.equalsIgnoreCase("standaloneDone")) {
			return handleDoneRequest (mapping, form, request, response, validator);

		} */

		// change ParentDir
		if (command.equalsIgnoreCase("parentDir")) {
			prtln("parentDir with recordsDir = " + sef.getRecordsDir());
			sef.setRecordsDir(sef.getRecordsDir().getParentFile());
			metadataFramework.setRecordsDir(sef.getRecordsDir().toString());
			prtln("recordsDir changed to " + sef.getRecordsDir());
			return mapping.findForward("error.page");
		}

		if (command.equalsIgnoreCase("changeDir")) {
			String pathArg = request.getParameter("pathArg");
			prtln("parentDir with recordsDir = " + sef.getRecordsDir());
			if (pathArg == null || pathArg.trim().length() == 0) {
				errorMsg = "changeDir: pathArg not found in request";
				prtln(errorMsg);
				throw new ServletException(errorMsg);
			}
			else {
				sef.setRecordsDir(new File(sef.getRecordsDir(), pathArg));
				metadataFramework.setRecordsDir(sef.getRecordsDir().toString());
				prtln("recordsDir changed to " + sef.getRecordsDir());
				return mapping.findForward("error.page");
			}
		}

		return super.handleOtherCommands(mapping, form, request, response);
	}

	
/* 	protected ActionForward handleDoneRequest (ActionMapping mapping,
			ActionForm form,
			HttpServletRequest request,
			HttpServletResponse response,
			SchemEditValidator validator)
		throws ServletException, MissingLockException  {
			
		prtln ("handleDoneRequest");
			
		SchemEditForm sef = (SchemEditForm) form;
		MetaDataFramework metadataFramework = getMetaDataFramework();

		ActionErrors errors = saveRecord (mapping, form, request, response, validator);
		
		if (errors.size() > 0) {
			saveErrors(request, errors);
			return getEditorMapping(mapping);
		}
		
		// save is successful
		sessionBean.releaseLock(sef.getRecId());
		errors.add("message",
				new ActionError("save.confirmation"));
		saveErrors(request, errors);
		return mapping.findForward("error.page");
	}
 */
 
 /**
	* Save a record to disk. With the exception of MissingLock errors and Exceptions thrown by putRecord, exceptions are
	caught and returned as ActionErrors.
	*/
	protected ActionErrors saveRecord (ActionMapping mapping,
			ActionForm form,
			HttpServletRequest request,
			HttpServletResponse response,
			SchemEditValidator validator)
		throws MissingLockException {
		
		prtln ("Standalone saveRecord()");
		
		ActionErrors errors = new ActionErrors();
		SchemEditForm sef = (SchemEditForm) form;
		MetaDataFramework metadataFramework = getMetaDataFramework();
		SessionBean sessionBean = this.getSessionBean(request);
		
		if (!sessionBean.ownsLock(sef.getRecId())) {
			throw new MissingLockException();
		}
			
		if (sef.getRecId() == null || sef.getRecId().trim().length() == 0) {
			errors.add("error",
					new ActionError("record.id.notspecified"));
			return errors;
		}

		validator.updateMultiValueFields();
		validator.pruneInstanceDoc();

		try {
			putRecord(mapping, form, request);
		} catch (Exception e) {
			prtln("putRecord error: " + e.getMessage());
			errors.add("pageErrors",
				new ActionError("generic.error", e.getMessage()));
		}
		
		sef.setSavedContent(DocContentMap.getDocContentMap(sef.getDocMap().getDocument(), metadataFramework));
		sef.setGuardedExitPath(null);
		
		return errors;
	}
	/**
	 *  
	 *
	 *@param  mapping                   Description of the Parameter
	 *@param  form                      Description of the Parameter
	 *@param  request                   Description of the Parameter
	 *@param  response                  Description of the Parameter
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
			
		prtln ("Stand allone handleSaveRequest()");
			
		ActionErrors errors = saveRecord (mapping, form, request, response, validator);
			
		if (errors.size() == 0) {
			errors.add("message",
				new ActionError("save.confirmation"));
		}
		
		saveErrors(request, errors);
		return getEditorMapping(mapping);
	}

	
	//----------------------------------------------------------------

	/**
	 *  Gets the fileToEdit attribute of the StandAloneSchemEditAction object
	 *
	 *@param  mapping        Description of the Parameter
	 *@param  form           Description of the Parameter
	 *@param  request        Description of the Parameter
	 *@param  schemaHelper   Description of the Parameter
	 *@return                The fileToEdit value
	 *@exception  Exception  Description of the Exception
	 */
	protected File getFileToEdit(ActionMapping mapping,
			ActionForm form,
			HttpServletRequest request,
			SchemaHelper schemaHelper)
		throws Exception {

		prtln("StandAloneSchemEditAction.getFileToEdit()");

		SchemEditForm sef = (SchemEditForm) form;
		MetaDataFramework metadataFramework = getMetaDataFramework();
		Document record = null;
		ActionErrors errors = new ActionErrors();
		String errorMsg;

		String src = request.getParameter("src");
		String recId = request.getParameter("recId");
		String collection = request.getParameter("collection");
		return new File(sef.getRecordsDir(), recId + ".xml");
	}


	/**
	 *  Description of the Method
	 *
	 *@param  mapping               Description of the Parameter
	 *@param  form                  Description of the Parameter
	 *@param  request               Description of the Parameter
	 *@param  response              Description of the Parameter
	 *@return                       Description of the Return Value
	 *@exception  ServletException  Description of the Exception
	 */
	protected ActionForward handleNewRecordRequest(ActionMapping mapping,
			ActionForm form,
			HttpServletRequest request,
			HttpServletResponse response)
		throws ServletException {

		SchemEditForm sef = (SchemEditForm) form;
		MetaDataFramework metadataFramework = getMetaDataFramework();
		// we don't know the id at this time
		String recId = "";
		String firstPage = metadataFramework.getPageList().getFirstPage();
		Document record = null;
		try {
			record = metadataFramework.makeMinimalRecord(recId);
		} catch (Exception e) {
			throw new ServletException("unable to generate record: " + e.getMessage());
		}
		ActionErrors errors = new ActionErrors();
		String errorMsg;

		String collection = request.getParameter("collection");
		// record = schemaHelper.getMinimalDocument();

		// sef.setNameSpaceInfo(metadataFramework.getNameSpaceInfo()); // !!!
		sef.setRecId(recId);
		sef.setDocMap(record);
		sef.setCurrentPage(firstPage);

		return mapping.findForward("editor");
	}

	protected ActionForward handleCancelRequest(ActionMapping mapping,
			ActionForm form,
			HttpServletRequest request,
			HttpServletResponse response)
		throws ServletException {
			
		return mapping.findForward("error.page");
		}

	/**
	 *  Description of the Method
	 *
	 *@param  mapping               Description of the Parameter
	 *@param  form                  Description of the Parameter
	 *@param  request               Description of the Parameter
	 *@param  response              Description of the Parameter
	 *@return                       Description of the Return Value
	 *@exception  ServletException  Description of the Exception
	 */
	protected ActionForward handleEditRequest(ActionMapping mapping,
			ActionForm form,
			HttpServletRequest request,
			HttpServletResponse response)
		throws ServletException {

		prtln("handleEditRequest()");

		SchemEditForm sef = (SchemEditForm) form;
		MetaDataFramework metadataFramework = getMetaDataFramework();
		SessionBean sessionBean = this.getSessionBean(request);
		Document record = null;
		ActionErrors errors = new ActionErrors();
		String errorMsg;

		String src = request.getParameter("src");
		String recId = request.getParameter("recId");
		String collection = request.getParameter("collection");

		if (src.equals("remote")) {
			if (recId == null || recId.trim().length() == 0) {
				errorMsg = "src is remote but no recId supplied";
				prtln(errorMsg);
				throw new ServletException(errorMsg);
			}
			try {
				record = getRemoteRecord(recId);
			} catch (Exception e) {
				errorMsg = e.getMessage();
				prtln("couldn't get remote document: " + e);
				errors.add("pageErrors",
						new ActionError("remote.record.notfound", recId));
				saveErrors(request, errors);
				return mapping.findForward("error.page");
			}
		}

		// get the file (whether a sample record or from disk)
		else if (src.equals("local") || src.equals("sample") || src.equals("dcs")) {

			try {
				File file = getFileToEdit(mapping, form, request, metadataFramework.getSchemaHelper());

				if (!file.exists()) {
					prtln("file doesn't exist: " + file.toString());
					errors.add("pageErrors",
							new ActionError("file.notfound", file.toString()));
					saveErrors(request, errors);
					return mapping.findForward("error.page");
				}
				else {
					// prtln("file exists");
				}
				record = metadataFramework.getEditableDocument(file.getAbsolutePath());
				
			} catch (Throwable e) {
				prtln(e.getMessage());
				e.printStackTrace();
				String msg = e.getMessage();
				if (msg == null || msg.trim().length() == 0)
					msg = "Please verify that it is of the correct format";
				errors.add("error",
						new ActionError("file.get.error", msg));
				saveErrors(request, errors);
				sef.setRecId("");
				return mapping.findForward("error.page");
			}
		}

		// OBTAIN LOCK For record
		if (!sessionBean.getLock(sef.getRecId())) {
			errors.add("recordLocked",
					new ActionError("lock.not.obtained.error", sef.getRecId()));
			saveErrors(request, errors);
			return mapping.findForward("error.page");
		}

		sef.setSetInfo(SchemEditUtils.getDcsSetInfo(collection, repositoryManager));
		sef.setDocMap(record);
		prtln("  ... saving content of record before editing");
		sef.setSavedContent(DocContentMap.getDocContentMap(record, metadataFramework));
		sef.setGuardedExitPath(null);

		// initialize the collapseBean's state
		// Currently we reset the CollapseBean each time a record is "opened".
		// but if this call is disabled, the state will be retained across documents ...
		new CollapseBeanInitializer(sef.getCollapseBean(), record, metadataFramework).init();

		// if a page parameter is supplied, we open that page directly
		String page = request.getParameter("page");
		if (page != null && page.trim().length() > 0) {
			sef.setCurrentPage(page);
			String pathArg = request.getParameter("pathArg");
			if (pathArg != null && pathArg.trim().length() > 0) {
				prtln("setting hash to " + pathArg);
				// sef.setHash(pathArg);
				sef.setHash(CollapseUtils.pathToId(pathArg));
				sef.exposeNode(XPathUtils.decodeXPath(pathArg));
			}
		}
		else {
			sef.setCurrentPage(firstPage);
		}

		return mapping.findForward("editor");
	}


	/**
	 *  Saves a metadata record either to disk
	 *
	 *@param  mapping        Description of the Parameter
	 *@param  form           Description of the Parameter
	 *@param  request        Description of the Parameter
	 *@exception  Exception  if save operation is unsuccessful
	 */
	protected void putRecord(ActionMapping mapping,
			ActionForm form,
			HttpServletRequest request)
		throws Exception {

		// prtln ("StandAlone.putRecord()");
		SchemEditForm sef = (SchemEditForm) form;
		MetaDataFramework metadataFramework = getMetaDataFramework();
		
		String recordXml = metadataFramework.getWritableRecordXml(sef.getDocMap().getDocument());
		File dest = new File(sef.getRecordsDir(), sef.getRecId() + ".xml");
		try {
			Files.writeFile(recordXml, dest);
			prtln("record saved to disk at " + dest.toString());
			// prtln (recordXml + "\n-----------------------");
			// sef.setRecId("");
		} catch (Exception e) {
			e.printStackTrace();
			String errorMsg = "unable to write document to disk: " + e;
			throw new Exception(errorMsg);
		}
	}


	/**
	 *  Description of the Method
	 *
	 *@param  mapping       Description of the Parameter
	 *@param  form          Description of the Parameter
	 *@param  request       Description of the Parameter
	 *@param  schemaHelper  Description of the Parameter
	 *@param  validator     Description of the Parameter
	 *@return               Description of the Return Value
	 */
	protected ActionForward handleChangeIdRequest(ActionMapping mapping,
			ActionForm form,
			HttpServletRequest request,
			SchemaHelper schemaHelper,
			SchemEditValidator validator) {
		/*
		    we are NOT validating the input fields before changingID. but we have to process the
		    mulitvalue fields to preserve any edits that have been made.
		  */
		SchemEditForm sef = (SchemEditForm) form;
		validator.updateMultiValueFields();

		String pathArg = request.getParameter("pathArg");

		prtln("changeID");
		if ((pathArg == null) || (pathArg.trim().length() == 0)) {
			prtln("no id recieved");
		}
		else {
			String idPath = getMetaDataFramework().getIdPath();
			if (idPath == null) {
				String errorMsg = "handleChangeIdRequest: idPath not found in MetaDataFramework";
				prtln(errorMsg);
			}
			else {
				sef.setValueOf(idPath, pathArg);
				sef.setRecId(pathArg);
			}
		}

		ActionErrors errors = new ActionErrors();
		errors.add("message",
				new ActionError("record.name.changed.confirmation", pathArg));
		saveErrors(request, errors);

		return getEditorMapping(mapping);
	}


	/**
	 *  Print a line to standard out.
	 *
	 *@param  s  The String to print.
	 */
	protected void prtln(String s) {
		if (debug) {
			System.out.println("StandAloneSchemEditAction: " + s);
		}
	}

}

