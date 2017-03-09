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
import org.dlese.dpc.schemedit.action.form.RecordOperationsForm;
import org.dlese.dpc.schemedit.*;
import org.dlese.dpc.schemedit.repository.RepositoryService;
import org.dlese.dpc.schemedit.config.CollectionRegistry;
import org.dlese.dpc.schemedit.dcs.DcsSetInfo;
import org.dlese.dpc.schemedit.dcs.DcsDataRecord;
import org.dlese.dpc.schemedit.security.user.User;
import org.dlese.dpc.index.reader.*;
import org.dlese.dpc.xml.*;
import org.apache.lucene.search.*;
import org.dlese.dpc.webapps.tools.GeneralServletTools;

import java.util.*;
import java.io.*;
import java.util.Hashtable;
import java.util.Locale;
import javax.servlet.*;
import javax.servlet.http.*;

import org.dom4j.Document;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionError;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionServlet;
import org.apache.struts.util.MessageResources;

/**
 *  A Struts Action for handling operations on single records, such as Copy, Delete,
   Move, and New.
 *
 *
 *
 *@author    Jonathan Ostwald
 *
 */
public final class RecordOperationsAction extends DCSAction {

	private static boolean debug = false;

	private RepositoryManager rm;

	// --------------------------------------------------------- Public Methods

	/**
	 *  Processes the specified HTTP request and creates the corresponding HTTP
	 *  response by forwarding to a JSP that will create it. A {@link
	 *  org.dlese.dpc.index.SimpleLuceneIndex} must be available to this class via
	 *  a ServletContext attribute under the key "index." Returns an {@link
	 *  org.apache.struts.action.ActionForward} instance that maps to the Struts
	 *  forwarding name "search," which must be configured in
	 *  struts-config.xml to forward to the JSP page that will handle the request.
	 *
	 *@param  mapping               The ActionMapping used to select this instance
	 *@param  request               The HTTP request we are processing
	 *@param  response              The HTTP response we are creating
	 *@param  form                  The ActionForm for the given page
	 *@return                       The ActionForward instance describing where and
	 *      how control should be forwarded
	 *@exception  IOException       if an input/output error occurs
	 *@exception  ServletException  if a servlet exception occurs
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

		ActionErrors errors = initializeFromContext (mapping, request);
		if (!errors.isEmpty()) {
			saveErrors (request, errors);
			return (mapping.findForward("error.page"));
		}
		RecordOperationsForm rof = (RecordOperationsForm) form;
		rm = repositoryManager;
		
		ServletContext servletContext = servlet.getServletContext();

		SchemEditUtils.showRequestParameters(request);

		rof.setSets(
			repositoryService.getAuthorizedSets(getSessionUser(request), this.requiredRole));
		
		// handle requests specifying a "command"
		if (request.getParameter("command") != null) {
			String command = request.getParameter("command");

			if (command.equalsIgnoreCase("copyRecord")) {
				return handleCopyRecord(mapping, form, request);
			}
			if (command.equalsIgnoreCase("deleteRecord")) {
				return handleDeleteRecord(mapping, form, request, response);
			}
			
			if (command.equalsIgnoreCase("move")) {
				return handleMoveRecord(mapping, form, request);
			}
			
			if (command.equalsIgnoreCase("copymove")) {
				return handleCopyMoveRecord(mapping, form, request);
			}
			
			if (command.equals("newRecord")) {
				try {
					return handleNewRecord(mapping, form, request, response);
				} catch (Throwable t) {
					prtln ("newRecord error: " + t.getMessage());
					// t.printStackTrace();
				}
			}
			
			errors.add("error", new ActionError("unrecognized.command", command));
			saveErrors(request, errors);
			return mapping.findForward("error.page");
		}
		
		errors.add("recordLocked", new ActionError("generic.error", "No command specified"));
		saveErrors(request, errors);
		return mapping.findForward("error.page");
	}

	private ActionForward handleMoveRecord( ActionMapping mapping, ActionForm form, HttpServletRequest request) {
		RecordOperationsForm rof = (RecordOperationsForm) form;
		SessionBean sessionBean = this.getSessionBean(request);
		ActionErrors errors = new ActionErrors();
		String errorMsg = "";
		
		// recId is the id of the record we are going to move
		String recId = request.getParameter("recId");
		if (recId == null || recId.length() == 0) {
			errors.add ("error", new ActionError ("parameter.required", "recId"));
			saveErrors (request, errors);
			return mapping.findForward ("move.record"); 
		}
		
		// exit -- signifies that the user is leaving (cancelling) the move operation
		// editRec -- signifies that the user is in the editor (rather than viewing or searching)
		String exit = request.getParameter("exit");
		if (exit != null) {
			// WARNING: kludge follows: we need a way to handle moving from within the editor
			// we could copy the code from this method to schemeEdit, OR we can stick an editRec
			// control param in to help us ... editRec is flag that tells us if we are in the editor.
			// if we are in the editor, then we don't use forwardToCaller, but rather we invoke the
			// editor for the newly moved record.
			
			// we have already moved the record, and now we have to forward the user to the
			// editor with the moved record
			
			// NOTE: we don't have anything to release here, since we have just completed the move
			
			String editRec = rof.getEditRec();
			if (editRec == null || editRec.length() == 0)   {
				return SchemEditUtils.forwardToCaller(request, recId, sessionBean);
			}
			else {
				// send the user back to the editor with the 
				XMLDocReader docReader = null;
				try {
					docReader = RepositoryService.getXMLDocReader(recId, rm);
				} catch (Exception e) {
					errorMsg =  "system could not find indexed record for " + recId;
					errors.add ("error", new ActionError ("record.op.failure", "moved", errorMsg));
					saveErrors (request, errors);
					return mapping.findForward ("move.record"); 
				}
				String xmlFormat =  docReader.getNativeFormat();
				String collection = docReader.getCollection();
				String forwardPath = "/editor/edit.do?";
				forwardPath += 	"src=dcs&command=edit&collection=" + collection + "&recId=" + recId;
				rof.setEditRec ("");
				return new ActionForward(forwardPath);
			}
		}
		
		// obtain lock on record
		if (!sessionBean.getLock(recId)) {
			errors.add ("recordLocked", new ActionError ("lock.not.obtained.error", recId));
			saveErrors (request, errors);
			return mapping.findForward ("error.page");
		}
		
		String collection = request.getParameter ("collection");
		if (collection == null || collection.length() == 0) {
			
			// we don't have a destination collection yet, so get it from user
			String srcCollection = SchemEditUtils.getCollectionOfIndexedRecord (recId, rm);
			DcsSetInfo dcsSetInfo = SchemEditUtils.getDcsSetInfo(srcCollection, rm);
			if (dcsSetInfo == null) {
				sessionBean.releaseLock(recId);
				errorMsg =  "source collection not found for " + recId;
				errors.add ("error", new ActionError ("record.op.failure", "moved", errorMsg));
				saveErrors (request, errors);
				return mapping.findForward ("move.record");
			}

			rof.setResultDoc(rm.getRecord(recId));
			rof.setDcsSetInfo(dcsSetInfo);
			rof.setCollection (null);
			return mapping.findForward("move.record");
		}
		else {
			// we have a collection and a recID - DO THE MOVE
			DcsSetInfo setInfo = SchemEditUtils.getDcsSetInfo(collection, rm);
			
			try {
				String newID = repositoryService.moveRecord (recId, collection);
				rof.setResultDoc(null);
				rof.setRecId(newID);
				rof.setDcsSetInfo(setInfo);
				errors.add ("message", 
					new ActionError ("generic.message", "record moved to " + setInfo.getName()));
			} catch (Exception e) {
				errors.add ("error", new ActionError ("record.op.failure", "moved", e.getMessage()));
			} finally {
				sessionBean.releaseLock(recId);
			}
			saveErrors (request, errors);
			return mapping.findForward ("move.record");
		}
	}
	
	private ActionForward handleCopyMoveRecord( ActionMapping mapping, ActionForm form, HttpServletRequest request) {
		RecordOperationsForm rof = (RecordOperationsForm) form;
		SessionBean sessionBean = this.getSessionBean(request);
		ActionErrors errors = new ActionErrors();
		String errorMsg = "";
		
		// recId is the id of the record we are going to move
		String recId = request.getParameter("recId");
		if (recId == null || recId.length() == 0) {
			errors.add ("error", new ActionError ("parameter.required", "recId"));
			saveErrors (request, errors);
			return mapping.findForward ("copy.move.record"); 
		}
		
		// exit -- signifies that the user is leaving (cancelling) the move operation
		// editRec -- signifies that the user is in the editor (rather than viewing or searching)
		String exit = request.getParameter("exit");
		if (exit != null) {
			// WARNING: kludge follows: we need a way to handle moving from within the editor
			// we could copy the code from this method to schemeEdit, OR we can stick an editRec
			// control param in to help us ... editRec is flag that tells us if we are in the editor.
			// if we are in the editor, then we don't use forwardToCaller, but rather we invoke the
			// editor for the newly moved record.
			
			// we have already moved the record, and now we have to forward the user to the
			// editor with the moved record
			
			// NOTE: we don't have anything to release here, since we have just completed the move
			
			String editRec = rof.getEditRec();
			if (editRec == null || editRec.length() == 0)   {
				return SchemEditUtils.forwardToCaller(request, recId, sessionBean);
			}
			else {
				// send the user back to the editor with the 
				XMLDocReader docReader = null;
				try {
					docReader = RepositoryService.getXMLDocReader(recId, rm);
				} catch (Exception e) {
					errorMsg =  "system could not find indexed record for " + recId;
					errors.add ("error", new ActionError ("record.op.failure", "moved", errorMsg));
					saveErrors (request, errors);
					return mapping.findForward ("copy.move.record"); 
				}
				String xmlFormat =  docReader.getNativeFormat();
				String collection = docReader.getCollection();
				String forwardPath = "/editor/edit.do?";
				forwardPath += 	"src=dcs&command=edit&collection=" + collection + "&recId=" + recId;
				rof.setEditRec ("");
				return new ActionForward(forwardPath);
			}
		}
		
		// obtain lock on record
		if (!sessionBean.getLock(recId)) {
			errors.add ("recordLocked", new ActionError ("lock.not.obtained.error", recId));
			saveErrors (request, errors);
			return mapping.findForward ("error.page");
		}
		
		String collection = request.getParameter ("collection");
		if (collection == null || collection.length() == 0) {
			
			// we don't have a destination collection yet, so get it from user
			String srcCollection = SchemEditUtils.getCollectionOfIndexedRecord (recId, rm);
			DcsSetInfo dcsSetInfo = SchemEditUtils.getDcsSetInfo(srcCollection, rm);
			if (dcsSetInfo == null) {
				sessionBean.releaseLock(recId);
				errorMsg =  "source collection not found for " + recId;
				errors.add ("error", new ActionError ("record.op.failure", "moved", errorMsg));
				saveErrors (request, errors);
				return mapping.findForward ("copy.move.record");
			}

			rof.setResultDoc(rm.getRecord(recId));
			rof.setDcsSetInfo(dcsSetInfo);
			rof.setCollection (null);
			return mapping.findForward("copy.move.record");
		}
		else {
			// we have a collection and a recID - DO THE MOVE
			DcsSetInfo setInfo = SchemEditUtils.getDcsSetInfo(collection, rm);
			
			try {
				String newID = repositoryService.copyMoveRecord (recId, collection);
				rof.setResultDoc(null);
				rof.setRecId(newID);
				rof.setDcsSetInfo(setInfo);
				errors.add ("message", 
					new ActionError ("generic.message", "record copied and moved to " + setInfo.getName()));
			} catch (Exception e) {
				errors.add ("error", new ActionError ("record.op.failure", "moved", e.getMessage()));
			} finally {
				sessionBean.releaseLock(recId);
			}
			saveErrors (request, errors);
			return mapping.findForward ("copy.move.record");
		}
	}
	

	/**
	 *  Description of the Method
	 *
	 *@param  mapping               Description of the Parameter
	 *@param  form                  Description of the Parameter
	 *@param  request               Description of the Parameter
	 *@return                       Description of the Return Value
	 *@exception  ServletException  Description of the Exception
	 */
	protected ActionForward handleCopyRecord(ActionMapping mapping,
			ActionForm form,
			HttpServletRequest request)
		throws ServletException {
		
		ActionErrors errors = new ActionErrors();
		String errorMsg;

		String originalId = request.getParameter("id");
		User sessionUser = this.getSessionUser(request);
		try {
			XMLDocReader docReader = repositoryService.copyRecord(originalId, sessionUser);

			// obtain info necessary for creating forwardPath from docReader for copied record
			String collection = docReader.getCollection();
			String id = docReader.getId();
			String xmlFormat = docReader.getNativeFormat();
			MetaDataFramework framework = this.getMetaDataFramework(xmlFormat);

			String forwardPath = "/editor/edit.do?command=edit&recId=" + id;
			return new ActionForward(forwardPath);
		} catch (Exception e) {
			errors.add("error",
					new ActionError("record.op.failure", "copied", e.getMessage()));
			saveErrors(request, errors);
			return mapping.findForward("browse.query");
		} catch (Throwable t) {
			t.printStackTrace();
			errors.add("error",
					new ActionError("record.op.failure", "copied", "server error"));
			saveErrors(request, errors);
			return mapping.findForward("browse.query");
		}
	}

	/**
	* Redirect to the metadata editor loaded with specified record.
	*/
	protected ActionForward getEditRecordForward (String id, String collection) {
			String forwardPath = "/editor/edit.do?command=edit&collection=" + collection + "&recId=" + id;
			return new ActionForward(forwardPath, true);
	}
	
	protected ActionForward handleNewRecord(ActionMapping mapping,
	                                               ActionForm form,
	                                               HttpServletRequest request,
	                                               HttpServletResponse response)
		throws ServletException {
		
		ActionErrors errors = new ActionErrors();
		String errorMsg;

		String collection = request.getParameter("collection");
		String xmlFormat = request.getParameter ("xmlFormat");

		if (collection == null) {
			errors.add ("error", new ActionError ("parameter.required", "collection"));
		}
		
		if (xmlFormat == null) {
			errors.add ("error", new ActionError ("parameter.required", "xmlFormat"));
		}
		
		if (!errors.isEmpty()) {
			saveErrors(request, errors);
			return mapping.findForward("error.page");
		}
		
		String id;			
		try {		
			MetaDataFramework framework = this.getMetaDataFramework(xmlFormat);
			if (framework == null) {
				prtln ("Framework is NULL?!?");
				prtln (frameworkRegistry.toString());
				throw new Exception ("framework not found for \"" + xmlFormat + "\"");
			}
	
			id = collectionRegistry.getNextID(collection);
			Document record = framework.makeMinimalRecord(id, 
														  collectionRegistry.getCollectionConfig(collection),
														  this.getSessionUser (request));
			
			if (!framework.getSchemaHelper().getNamespaceEnabled()) {
				// now prepare document to write to file by inserting namespace information
				record = framework.getWritableRecord(record);
			}
			
			String username = this.getSessionUserName (request);
			repositoryService.saveNewRecord(id, record.asXML(), collection, username);

		} catch (Exception e) {
			e.printStackTrace();
			errors.add("error",
					new ActionError("record.op.failure", "created", e.getMessage()));
			saveErrors(request, errors);
			return mapping.findForward("error.page");
		}

		return getEditRecordForward (id, collection);
	}

	

	/**
	 *  Description of the Method
	 *
	 *@param  mapping    Description of the Parameter
	 *@param  request    Description of the Parameter
	 *@param  response   Description of the Parameter
	 *@param  queryForm  Description of the Parameter
	 *@return            Description of the Return Value
	 */
	private ActionForward handleDeleteRecord(
			ActionMapping mapping,
			ActionForm form,
			HttpServletRequest request,
			HttpServletResponse response) {
		prtln("handleDeleteRecord");
		ActionErrors errors = new ActionErrors();
		SessionBean sessionBean = sessionRegistry.getSessionBean(request);
		ServletContext servletContext = servlet.getServletContext();

		// recId is the ID of the record to delete
		String recId = request.getParameter("recId");
		if (!sessionBean.getLock(recId)) {
			errors.add("recordLocked", new ActionError("lock.not.obtained.error", recId));
			saveErrors(request, errors);
			return mapping.findForward("error.page");
		}

		// delete record with help of RepositoryService
		try {
			repositoryService.deleteRecord(recId);
			errors.add("deleteConfirmation", new ActionError("generic.message", "Record " + recId + " deleted"));
			sessionRegistry.getSessionBean(request).setRecId(null);
		} catch (Exception e) {
			errors.add("error", 
				new ActionError("record.op.failure", "deleted: ", e.getMessage()));
		} finally {
			sessionBean.releaseLock(recId);
		}

		saveErrors(request, errors);
		return mapping.findForward("browse.query");
	}



	// -------------- Debug ------------------


	/**
	 *  Sets the debug attribute of the RecordOperationsAction class
	 *
	 *@param  isDebugOutput  The new debug value
	 */
	public static void setDebug(boolean isDebugOutput) {
		debug = isDebugOutput;
	}



	/**
	 *  Print a line to standard out.
	 *
	 *@param  s  The String to print.
	 */
	private void prtln(String s) {
		if (debug) {
			System.out.println("RecordOperationsAction: " + s);
		}
	}
}

