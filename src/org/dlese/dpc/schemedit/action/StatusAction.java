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

import org.dlese.dpc.repository.RepositoryManager;
import org.dlese.dpc.index.reader.*;
import org.dlese.dpc.schemedit.*;
import org.dlese.dpc.schemedit.repository.RepositoryService;
import org.dlese.dpc.schemedit.dcs.*;
import org.dlese.dpc.schemedit.config.CollectionConfig;
import org.dlese.dpc.schemedit.config.CollectionRegistry;
import org.dlese.dpc.schemedit.action.form.StatusForm;
import org.dlese.dpc.xml.*;
import org.dlese.dpc.xml.schema.*;
import org.dlese.dpc.util.*;
import org.dlese.dpc.util.strings.*;
import org.dlese.dpc.webapps.tools.GeneralServletTools;

import org.dlese.dpc.schemedit.security.user.User;

import org.dom4j.Document;

import java.util.*;
import java.io.*;
import java.net.*;
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
 *  A Struts Action controlling interaction during editing of workflow status information asociated with
 *  individual metadata records.
 *
 *
 *
 * @author     Jonathan Ostwald
 *
 */
public final class StatusAction extends DCSAction {

	private static boolean debug = true;


	/**
	 *  Gets the metaDataFramework attribute of the StatusAction object
	 *
	 * @return    The metaDataFramework value
	 */
	protected MetaDataFramework getMetaDataFramework() {
		return getMetaDataFramework ("dcs_data");
	}

	// --------------------------- Public Methods ------------------------------

	/**
	 *  Processes the specified HTTP request and creates the corresponding HTTP response by forwarding to a JSP
	 *  that will create it. A {@link org.dlese.dpc.repository.RepositoryManager} must be available to this class
	 *  via a ServletContext attribute under the key "repositoryManager." Returns an {@link
	 *  org.apache.struts.action.ActionForward} instance which must be configured in struts-config.xml to forward
	 *  to the JSP page that will handle the request.
	 *
	 * @param  mapping               The ActionMapping used to select this instance
	 * @param  request               The HTTP request we are processing
	 * @param  response              The HTTP response we are creating
	 * @param  form                  The ActionForm for the given page
	 * @return                       The ActionForward instance describing where and how control should be
	 *      forwarded
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
		ActionErrors errors = initializeFromContext (mapping, request);
		if (!errors.isEmpty()) {
			saveErrors (request, errors);
			return (mapping.findForward("error.page"));
		} 
		StatusForm statusForm = (StatusForm) form;
		String errorMsg = "";

		MetaDataFramework dcsdataFramework = getMetaDataFramework();
		SessionBean sessionBean = this.getSessionBean(request);
		SchemaHelper schemaHelper = dcsdataFramework.getSchemaHelper();
		
		SchemEditUtils.showRequestParameters(request);
		
		try {
			if (request.getParameter("command") != null) {
				String param = request.getParameter("command");

				if (param.equalsIgnoreCase("exit")) {
					String recId = statusForm.getRecId();
					sessionBean.releaseLock(recId);
					statusForm.clear();
					return forwardToCaller(request, statusForm);
				}

				if (param.equalsIgnoreCase("edit")) {
					return handleEditRecord(mapping, form, request, response);
				}

				// The rest of these commands require that a lock is held
				if (!sessionBean.ownsLock(statusForm.getRecId()))
					throw new MissingLockException();

				if (param.equalsIgnoreCase("deleteEntry")) {
					return handleDeleteEntry(mapping, form, request, response);
				}

				if (param.equalsIgnoreCase("updateEntry")) {
					return handleUpdateEntry(mapping, form, request, response);
				}

				if (param.equalsIgnoreCase("editEntry")) {
					return handleEditEntry(mapping, form, request, response);
				}

				if (param.equalsIgnoreCase("cancelEditEntry")) {
					if (!sessionBean.ownsLock(statusForm.getRecId()))
						throw new MissingLockException();
					statusForm.clear();
					errors.add("message", new ActionError("generic.message", "entry edit canceled"));
					saveErrors(request, errors);
					return mapping.findForward("edit.status");
				}

				if (param.equalsIgnoreCase("updateStatus")) {
					String recId = statusForm.getRecId();

					if (errors.size() > 0) {
						errors.add("error",
							new ActionError("edit.errors.found"));
						saveErrors(request, errors);
						return mapping.findForward("edit.status");
					}
					else {
						return handleSaveRecord(mapping, form, request, response);
					}
				}
			}
			statusForm.clear();
			errors.add("error",
				new ActionError("generic.error", "No command submitted - no action taken"));
			saveErrors(request, errors);
			return mapping.findForward("edit.status");
		} catch (MissingLockException mle) {
			errors.add("missingLock", new ActionError("missing.lock"));
			saveErrors(request, errors);
			return mapping.findForward("error.page");
		} catch (Throwable e) {
			prtln("System Error: " + e);
			String msg = e.getMessage();
			if (msg != null && msg.trim().length() > 0)
				msg = "A system error has occurred (" + msg + ")";
			else
				msg = "An unknown system error has occurred";
			
			if (e instanceof NullPointerException)
				e.printStackTrace();
			errors.add("actionSetupError", 
				new ActionError("generic.error", msg));
					
			saveErrors(request, errors);
			return mapping.findForward("error.page");
			// return forwardToCaller(request, statusForm);
		}
	}


	/**
	 *  Return a list of {@link StatusFlag} beans that describe the statuses that can be assigned to a {@link
	 *  StatusEntry}. This list is composed of the UNKNOWN_status plus those statuses that defined for the
	 *  collection. The IMPORT status is not available in this context, since it is only assigned by the system.
	 *
	 * @param  collection  collection key (e.g., 'dcc')
	 * @return             status flags that may be assigned for this collection
	 */
	private List getStatusFlags(String collection) {
		CollectionConfig info = collectionRegistry.getCollectionConfig(collection);
		return info.getAssignableStatusFlags();
	}


	/**
	 *  Remove the StatusEntry corresponding to "entryKey" from the DcsDataRecord.
	 *
	 * @param  mapping        NOT YET DOCUMENTED
	 * @param  form           NOT YET DOCUMENTED
	 * @param  request        NOT YET DOCUMENTED
	 * @param  response       NOT YET DOCUMENTED
	 * @return                NOT YET DOCUMENTED
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	private ActionForward handleDeleteEntry(ActionMapping mapping,
	                                        ActionForm form,
	                                        HttpServletRequest request,
	                                        HttpServletResponse response) throws Exception {
		ActionErrors errors = new ActionErrors();
		StatusForm statusForm = (StatusForm) form;
		String id = statusForm.getRecId();

		DcsDataRecord dcsDataRecord = statusForm.getDcsDataRecord();
		String entryKey = statusForm.getEntryKey();

		dcsDataRecord.deleteStatusEntry(entryKey);
		repositoryService.updateRecord(id);
		dcsDataRecord.flushToDisk();
		statusForm.clear();
		errors.add("message", new ActionError("generic.message", "status entry deleted"));
		saveErrors(request, errors);

		return mapping.findForward("edit.status");
	}


	/**
	 *  Update the edited values for the StatusEntry corresponding to "entryKey"
	 *
	 * @param  mapping        NOT YET DOCUMENTED
	 * @param  form           NOT YET DOCUMENTED
	 * @param  request        NOT YET DOCUMENTED
	 * @param  response       NOT YET DOCUMENTED
	 * @return                NOT YET DOCUMENTED
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	private ActionForward handleUpdateEntry(ActionMapping mapping,
	                                        ActionForm form,
	                                        HttpServletRequest request,
	                                        HttpServletResponse response) throws Exception {
		ActionErrors errors = new ActionErrors();
		StatusForm statusForm = (StatusForm) form;
		String entryKey = statusForm.getEntryKey();
		String id = statusForm.getRecId();

		DcsDataRecord dcsDataRecord = statusForm.getDcsDataRecord();
		StatusEntry statusEntry = dcsDataRecord.getStatusEntry(entryKey);
		if (statusEntry == null)
			throw new Exception("status entry not found for " + entryKey);
		statusEntry.setStatusNote(statusForm.getStatusNote());
		dcsDataRecord.replaceStatusEntry(entryKey, statusEntry);
		repositoryService.updateRecord(id);
		dcsDataRecord.flushToDisk();
		statusForm.setHash(entryKey);
		statusForm.clear();
		statusForm.setEntryKey("");
		errors.add("message", new ActionError("generic.message", "status entry updated"));
		saveErrors(request, errors);
		return mapping.findForward("edit.status");
	}


	/**
	 *  Set up to edit a history item (corresponding to "entryKey" of the current status record
	 *
	 * @param  mapping        NOT YET DOCUMENTED
	 * @param  form           NOT YET DOCUMENTED
	 * @param  request        NOT YET DOCUMENTED
	 * @param  response       NOT YET DOCUMENTED
	 * @return                NOT YET DOCUMENTED
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	private ActionForward handleEditEntry(ActionMapping mapping,
	                                      ActionForm form,
	                                      HttpServletRequest request,
	                                      HttpServletResponse response) throws Exception {
		StatusForm statusForm = (StatusForm) form;
		DcsDataRecord dcsDataRecord = statusForm.getDcsDataRecord();
		String entryKey = statusForm.getEntryKey();
		StatusEntry statusEntry = dcsDataRecord.getStatusEntry(entryKey);
		if (statusEntry == null)
			throw new Exception("status entry not found for " + entryKey);
		statusForm.setStatusNote(statusEntry.getStatusNote());
		statusForm.setHash(entryKey);
		prtln ("statusForm.ndrHandle set to " + statusForm.getNdrHandle());
		return mapping.findForward("edit.status");
	}


	/**
	 *  Set up to edit a new statusEntry for this record.
	 *
	 * @param  mapping        NOT YET DOCUMENTED
	 * @param  form           NOT YET DOCUMENTED
	 * @param  request        NOT YET DOCUMENTED
	 * @param  response       NOT YET DOCUMENTED
	 * @return                NOT YET DOCUMENTED
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	private ActionForward handleEditRecord(ActionMapping mapping,
	                                       ActionForm form,
	                                       HttpServletRequest request,
	                                       HttpServletResponse response) throws Exception {
		ActionErrors errors = new ActionErrors();
		StatusForm statusForm = (StatusForm) form;
		String id = request.getParameter("recId");
		if (id == null || id.trim().length() == 0) {
			errors.add("error",
				new ActionError("generic.error", "a record id is required to edit status"));
			saveErrors(request, errors);
			return forwardToCaller(request, statusForm);
		}

		// OBTAIN LOCK For record
		SessionBean sessionBean = this.getSessionBean(request);
		if (!sessionBean.getLock(id)) {
			errors.add("recordLocked",
				new ActionError("lock.not.obtained.error", id));
			saveErrors(request, errors);
			return mapping.findForward("error.page");
		}

		// prepare for case in which this record is no longer on search results
		// after status is changed.
		SearchHelper searchHelper = sessionBean.getSearchHelper();
		searchHelper.setCachedRecIndex (searchHelper.getIndexOf(id));
		
		statusForm.clear();
		statusForm.setReferer(request.getHeader("referer"));
		statusForm.setRecId(id);
		statusForm.setEntryKey("");

		XMLDocReader docReader = RepositoryService.getXMLDocReader(id, repositoryManager);
		String collection = docReader.getCollection();

		statusForm.setStatusFlags(getStatusFlags(collection));

		statusForm.setCollection(collection);
		statusForm.setCollectionName(docReader.getMyCollectionDoc().getShortTitle());

		// existing dcsData Record
		DcsDataRecord dcsData = dcsDataManager.getDcsDataRecord(id, repositoryManager);
		if (dcsData == null) {
			throw new Exception("Could not find dcsDataRecord for " + id);
		}
		statusForm.setDcsDataRecord(dcsData);

		// sessionBean remembers the editor for this session
		// statusForm.setLastEditor(sessionBean.getEditor());
		statusForm.setStatus(dcsData.getStatus());
		statusForm.setStatusNote("");

		if (request.getParameter("modal") != null) {
			return mapping.findForward("edit.status.modal");
		}
		else
			return mapping.findForward("edit.status");
	}


	/**
	 *  Save the new status entry for this record.
	 *
	 * @param  mapping   NOT YET DOCUMENTED
	 * @param  form      NOT YET DOCUMENTED
	 * @param  request   NOT YET DOCUMENTED
	 * @param  response  NOT YET DOCUMENTED
	 * @return           NOT YET DOCUMENTED
	 */
	private ActionForward handleSaveRecord(ActionMapping mapping,
	                                       ActionForm form,
	                                       HttpServletRequest request,
	                                       HttpServletResponse response) {
		ActionErrors errors = new ActionErrors();
		StatusForm statusForm = (StatusForm) form;
		String changeDate = SchemEditUtils.fullDateString(new Date());
		String username = this.getSessionUserName(request);
		StatusEntry statusEntry =
			new StatusEntry(statusForm.getStatus(), statusForm.getStatusNote(), username, changeDate);
		String recId = statusForm.getRecId();
		String entryKey = statusForm.getEntryKey();

		try {
			repositoryService.updateRecordStatus (recId, statusEntry);
			SessionBean sessionBean = this.getSessionBean(request);
			statusForm.clear();
			
			// don't forward anywhere if we are modal (we risk landing on a page that releases locks)
			if (request.getParameter("modal") != null) {
				return null;
			}
			
			sessionBean.releaseLock(recId);
			return SchemEditUtils.forwardToCaller(request, recId, sessionBean);
		} catch (Throwable t) {
			// reload the DcsDataRecord - it has been reverted when updateStatus fails
			statusForm.setDcsDataRecord(dcsDataManager.getDcsDataRecord(recId, repositoryManager));
			String errorMsg = "UpdateStatus Error: handleSaveRecord error: " + t.getMessage();
			errors.add("message",
				new ActionError("generic.message", errorMsg));
			saveErrors(request, errors);
			return mapping.findForward("edit.status");
		}
	}

	/**
	 *  Generate an ActionForward that returns control to the calling page if possible.
	 *
	 * @param  request     NOT YET DOCUMENTED
	 * @param  statusForm  NOT YET DOCUMENTED
	 * @return             NOT YET DOCUMENTED
	 */
	private ActionForward forwardToCaller(HttpServletRequest request, StatusForm statusForm) {
		
		// if all else fails, we return to the search page for the current collection
		String DEFAULT_FORWARD = "/admin/query.do?s=0&q=&sc=0" + statusForm.getCollection();

		String referer = statusForm.getReferer();
		statusForm.setReferer(null);

		// prtln ("\t referer: " + referer);
		
		try {
			// set the recId in the sessionBean so it is available to upcoming jsp page
			this.getSessionBean(request).setRecId(statusForm.getRecId());

			if (referer == null)
				throw new Exception("forwardToCaller() referer is null");

			String forwardPath = new URL(referer).getFile();
			
			
			if (forwardPath == null || forwardPath.trim().length() == 0)
				throw new Exception("forwarding to " + DEFAULT_FORWARD + " because referer was not supplied");

			// we expect the forwardPath at this point to start with the contextPath
			String contextUrl = GeneralServletTools.getContextUrl(request);
			String contextPath = "";

			contextPath = new URL(contextUrl).getPath().trim();

			// strip the context path from forward so it conforms to the syntax of the action forward path
			if (contextPath.length() > 0 && forwardPath.startsWith(contextPath))
				forwardPath = forwardPath.substring(contextPath.length());
			else
				throw new Exception("forwardToCaller: expected context path \"" + contextPath + 
							        "\" not found in referer");

			// we have a legal forward path.
			if (forwardPath != null && forwardPath.trim().length() > 0) {
				return new ActionForward(forwardPath);
			}
			
			return new ActionForward(DEFAULT_FORWARD);
		} catch (Throwable t) {
			// prtln ("\t caught error (" + t.getMessage() + ") returning DEFAULT_FORWARD");
			return new ActionForward(DEFAULT_FORWARD);
		}
	}


	// -------------- Debug ------------------


	/**
	 *  Sets the debug attribute of the StatusAction class
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
		if (debug)
			System.out.println("StatusAction: " + s);
	}
}

