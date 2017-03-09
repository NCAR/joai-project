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

import org.dlese.dpc.index.*;
import org.dlese.dpc.index.reader.*;
import org.dlese.dpc.schemedit.*;
import org.dlese.dpc.schemedit.dcs.*;
import org.dlese.dpc.schemedit.vocab.*;
import org.dlese.dpc.schemedit.config.*;
import org.dlese.dpc.schemedit.action.form.*;
import org.dlese.dpc.schemedit.security.user.User;
import org.dlese.dpc.xml.*;
import org.dlese.dpc.xml.schema.*;
import org.dlese.dpc.util.*;
import org.dlese.dpc.util.strings.*;

import org.dom4j.Document;

import java.util.*;
import java.util.regex.*;
import java.io.*;
import java.text.*;
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
 *  A Struts Action controlling batch record operations, such as batchDelete,
 *  batchStatusUpdate, and batchMove.<p>
 *
 *  Works in conjunction with {@link org.dlese.dpc.schemedit.action.form.BatchOperationsForm}
 *
 *@author    Jonathan Ostwald
 */
public final class BatchOperationsAction extends DCSAction {

	private static boolean debug = false;
	RoleManager roleManager;

	// --------------------------------------------------------- Public Methods

	/**
	 *  Processes the specified HTTP request and creates the corresponding HTTP
	 *  response by forwarding to a JSP that will create it. Returns an {@link
	 *  org.apache.struts.action.ActionForward} instance to forward to the JSP page
	 *  that will handle the request.
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
		ActionErrors errors = initializeFromContext(mapping, request);
		if (!errors.isEmpty()) {
			saveErrors(request, errors);
			return (mapping.findForward("error.page"));
		}

		BatchOperationsForm bof = (BatchOperationsForm) form;
		ServletContext servletContext = servlet.getServletContext();
		SessionBean sessionBean = this.getSessionBean(request);
		User sessionUser = this.getSessionUser(sessionBean);
		String errorMsg = "";

		bof.setRequest(request);

		roleManager = (RoleManager) servletContext.getAttribute("roleManager");
		if (roleManager == null) {
			prtln("WARNING: roleManager not active");
		}

		SimpleLuceneIndex index = repositoryManager.getIndex();

		bof.setSets(
				repositoryService.getAuthorizedSets(sessionUser, this.requiredRole));

		SchemEditUtils.showRequestParameters(request);

		try {
			if (request.getParameter("op") != null) {
				String param = request.getParameter("op");

				if (param.equals("exit")) {
					sessionBean.releaseAllLocks();
					bof.clear();
					return new ActionForward(sessionBean.getQueryUrl(), true);
				}

				if (param.equalsIgnoreCase("batchMove")) {
					return handleBatchMove(request, bof, mapping);
				}

				if (param.equalsIgnoreCase("batchCopyMove")) {
					return handleBatchCopyMove(request, bof, mapping);
				}

				if (param.equalsIgnoreCase("batchDelete")) {
					return handleBatchDelete(request, bof, mapping);
				}

				if (param.equalsIgnoreCase("batchStatus")) {
					return handleBatchStatus(request, bof, mapping);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			errors.add("error", new ActionError("generic.error", "system error: " + e.getMessage()));
			saveErrors(request, errors);
			return mapping.findForward("error.page");
		} catch (Throwable t) {
			errors.add("error", new ActionError("generic.error", "unknown system error"));
			t.printStackTrace();
			saveErrors(request, errors);
			return mapping.findForward("error.page");
		}
		errors.add("message",
				new ActionError("generic.message", "No operation specified - no action taken"));
		saveErrors(request, errors);
		return mapping.findForward("error.page");
	}


	/**
	 *  Control interaction with user to move a set of records to a selected
	 *  destination collection. The set of records to be moved is defined by a
	 *  search, the results of which are accessible via the SessionBean.
	 *
	 *@param  mapping  Description of the Parameter
	 *@param  request  Description of the Parameter
	 *@param  bof      Description of the Parameter
	 *@return          Description of the Return Value
	 */
	protected ActionForward handleBatchMove(HttpServletRequest request, BatchOperationsForm bof, ActionMapping mapping) {
		prtln("handleBatchMove");
		SessionBean sessionBean = sessionRegistry.getSessionBean(request);

		ActionErrors errors = new ActionErrors();
		String errorMsg;

		String command = request.getParameter("command");
		if (command != null && command.equalsIgnoreCase("removeRecords")) {
			return handleRemoveRecords(request, bof, mapping, "batch.move.records");
		}

		if (command != null && command.equalsIgnoreCase("move")) {
			// PERFORM THE BATCH OPEARATION
			String destCollection = bof.getCollection();
			DcsSetInfo setInfo = SchemEditUtils.getDcsSetInfo(destCollection, repositoryManager);
			RecordList recordBatch = bof.getRecordList();
			if (containsMasterCollectionRecord(recordBatch)) {
				errors.add("error", new ActionError("generic.error", "This list contains the master collection record, which cannot be moved."));
				saveErrors(request, errors);
				return mapping.findForward("batch.move.records");
			}
			try {
				if (!sessionBean.getBatchLocks(recordBatch)) {
					errors.add("error", new ActionError("batch.lock.not.obtained", "Batch Move"));
					saveErrors(request, errors);
					return mapping.findForward("batch.move.records");
				}
				RecordList failedRecordList =
						repositoryService.batchMoveRecords(recordBatch, destCollection);
				bof.setRecordList(null);
				bof.setFailedRecordList(failedRecordList);
				int failCount = failedRecordList.size();
				int moveCount = recordBatch.size() - failCount;
				bof.setDcsSetInfo(setInfo);
				errors.add("message",
						new ActionError("generic.message", moveCount + " records moved to " + setInfo.getName()));
				if (failCount > 0) {
					errors.add("error", new ActionError("generic.error", failCount + " records could not be moved"));
				}
			} catch (Exception e) {
				errorMsg = "batch move error: " + e.getMessage();
				errors.add("error", new ActionError("generic.error", errorMsg));
			} finally {
				sessionBean.releaseAllLocks();
			}
			saveErrors(request, errors);
			return mapping.findForward("batch.move.records");
		} else {
			// collect info from user to set up the move
			bof.setRecordList(null);
			// the list of records to move comes from the current result list (results of last search)
			RecordList recordBatch = sessionBean.getRecords();
			if (recordBatch.isEmpty()) {
				errors.add("error", new ActionError("generic.error", "no records selected to move"));
				saveErrors(request, errors);
				return mapping.findForward("batch.move.records");
			}

			// MOVE-SPECIFIC CLAUSE ------------------------------
			// make sure all of the records to move have the same format
			List formats = new ArrayList();
			for (Iterator i = recordBatch.iterator(); i.hasNext(); ) {
				ResultDoc result = recordBatch.getResultDoc((String) i.next(), repositoryManager.getIndex());
				XMLDocReader docReader = (XMLDocReader) result.getDocReader();
				String format = docReader.getNativeFormat();
				if (!formats.contains(format)) {
					formats.add(format);
				}
			}
			if (formats.size() > 1) {
				errors.add("error", new ActionError("generic.error", "not all records have the same format"));
				saveErrors(request, errors);
				return mapping.findForward("batch.move.records");
			}
			// end MOVE-SPECIFIC CLAUSE ------------------------------

			if (containsMasterCollectionRecord(recordBatch)) {
				errors.add("error", new ActionError("generic.error", "This list contains the master collection record, which cannot be moved."));
				saveErrors(request, errors);
				return mapping.findForward("batch.move.records");
			}

			if (!sessionBean.getBatchLocks(recordBatch)) {
				errors.add("error", new ActionError("batch.lock.not.obtained", "Batch Move"));
				saveErrors(request, errors);
				return mapping.findForward("batch.move.records");
			}

			String xmlFormat = (String) formats.get(0);
			bof.setFormatOfRecords(xmlFormat);
			bof.setRecordList(recordBatch);
			bof.setCollection(null);
			bof.setFailedRecordList(null);
			return mapping.findForward("batch.move.records");
		}

	}


	/**
	 *  Description of the Method
	 *
	 *@param  request  Description of the Parameter
	 *@param  bof      Description of the Parameter
	 *@param  mapping  Description of the Parameter
	 *@return          Description of the Return Value
	 */
	protected ActionForward handleBatchCopyMove(HttpServletRequest request, BatchOperationsForm bof, ActionMapping mapping) {
		prtln("handleBatchCopyMove");
		SessionBean sessionBean = sessionRegistry.getSessionBean(request);

		ActionErrors errors = new ActionErrors();
		String errorMsg;

		String command = request.getParameter("command");
		prtln("\t command: " + command);

		if (command != null && command.equalsIgnoreCase("removeRecords")) {
			return handleRemoveRecords(request, bof, mapping, "batch.copy.move.records");
		}

		if (command != null && command.equalsIgnoreCase("copyMove")) {
			// PERFORM THE BATCH OPEARATION
			String destCollection = bof.getCollection();
			DcsSetInfo setInfo = SchemEditUtils.getDcsSetInfo(destCollection, repositoryManager);
			RecordList recordBatch = bof.getRecordList();
			if (containsMasterCollectionRecord(recordBatch)) {
				errors.add("error", new ActionError("generic.error", "This list contains the master collection record, which cannot be moved."));
				saveErrors(request, errors);
				return mapping.findForward("batch.copy.move.records");
			}
			try {
				if (!sessionBean.getBatchLocks(recordBatch)) {
					prtln("uh oh ...");
					errors.add("error", new ActionError("batch.lock.not.obtained", "Batch Copy Move"));
					saveErrors(request, errors);
					return mapping.findForward("batch.copy.move.records");
				}
				prtln("about to call repositoryService.batchCopyMoveRecords");
				RecordList failedRecordList =
						repositoryService.batchCopyMoveRecords(recordBatch, destCollection);
				bof.setRecordList(null);
				bof.setFailedRecordList(failedRecordList);
				int failCount = failedRecordList.size();
				int moveCount = recordBatch.size() - failCount;
				bof.setDcsSetInfo(setInfo);
				errors.add("message",
						new ActionError("generic.message", moveCount + " records copied to " + setInfo.getName()));
				if (failCount > 0) {
					errors.add("error", new ActionError("generic.error", failCount + " records could not be copied"));
				}
			} catch (Exception e) {
				errorMsg = "batch move error: " + e.getMessage();
				errors.add("error", new ActionError("generic.error", errorMsg));
			} finally {
				sessionBean.releaseAllLocks();
			}
			saveErrors(request, errors);
			return mapping.findForward("batch.copy.move.records");
		} else {
			// collect info from user to set up the move
			bof.setRecordList(null);
			// the list of records to move comes from the current result list (results of last search)
			RecordList recordBatch = sessionBean.getRecords();
			if (recordBatch.isEmpty()) {
				errors.add("error", new ActionError("generic.error", "no records selected to copy and move"));
				saveErrors(request, errors);
				return mapping.findForward("batch.copy.move.records");
			}

			// MOVE-SPECIFIC CLAUSE ------------------------------
			// make sure all of the records to move have the same format
			List formats = new ArrayList();
			for (Iterator i = recordBatch.iterator(); i.hasNext(); ) {
				ResultDoc result = recordBatch.getResultDoc((String) i.next(), repositoryManager.getIndex());
				XMLDocReader docReader = (XMLDocReader) result.getDocReader();
				String format = docReader.getNativeFormat();
				if (!formats.contains(format)) {
					formats.add(format);
				}
			}
			if (formats.size() > 1) {
				errors.add("error", new ActionError("generic.error", "not all records have the same format"));
				saveErrors(request, errors);
				return mapping.findForward("batch.copy.move.records");
			}
			// end MOVE-SPECIFIC CLAUSE ------------------------------

			if (containsMasterCollectionRecord(recordBatch)) {
				errors.add("error", new ActionError("generic.error", "This list contains the master collection record, which cannot be moved."));
				saveErrors(request, errors);
				return mapping.findForward("batch.copy.move.records");
			}

			if (!sessionBean.getBatchLocks(recordBatch)) {
				errors.add("error", new ActionError("batch.lock.not.obtained", "Batch Copy Move"));
				saveErrors(request, errors);
				return mapping.findForward("batch.copy.move.records");
			}

			String xmlFormat = (String) formats.get(0);
			bof.setFormatOfRecords(xmlFormat);
			bof.setRecordList(recordBatch);
			bof.setCollection(null);
			bof.setFailedRecordList(null);
			return mapping.findForward("batch.copy.move.records");
		}

	}


	/**
	 *  Returns the distinct collections to which the individual records belong.
	 *  Used to test whether the results are all from the same collection.
	 *
	 *@param  recordList  NOT YET DOCUMENTED
	 *@return             The resultsCollections value
	 */
	List getResultsCollections(RecordList recordList) {

		List collections = new ArrayList();
		SimpleLuceneIndex index = repositoryManager.getIndex();
		for (Iterator i = recordList.iterator(); i.hasNext(); ) {
			String id = (String) i.next();
			ResultDoc result = recordList.getResultDoc(id, index);

			XMLDocReader docReader = (XMLDocReader) result.getDocReader();
			String myCol = docReader.getCollection();
			if (!collections.contains(myCol)) {
				collections.add(myCol);
			}
		}
		return collections;
	}


	/**
	 *  Returns true if the given array of ResultDocs contains a collection format.
	 *
	 *@param  recordBatch  an array of ResultDoc instances
	 *@return              true if ResultDoc[] contains record having xmlFormat of
	 *      "dlese_collect"
	 */
	private boolean containsCollectionRecords(RecordList recordBatch) {
		if (recordBatch == null) {
			return false;
		}
		for (Iterator i = recordBatch.iterator(); i.hasNext(); ) {
			ResultDoc result = recordBatch.getResultDoc((String) i.next(), repositoryManager.getIndex());
			XMLDocReader docReader = (XMLDocReader) result.getDocReader();
			if (docReader.getNativeFormat().equals("dlese_collect")) {
				return true;
			}
		}
		return false;
	}


	/**
	 *  Tests for presense of master collection record in recordBatch
	 *
	 *@param  recordBatch  NOT YET DOCUMENTED
	 *@return              NOT YET DOCUMENTED
	 */
	private boolean containsMasterCollectionRecord(RecordList recordBatch) {
		if (recordBatch == null) {
			return false;
		}
		for (Iterator i = recordBatch.iterator(); i.hasNext(); ) {
			ResultDoc result = recordBatch.getResultDoc((String) i.next(), repositoryManager.getIndex());
			XMLDocReader docReader = (XMLDocReader) result.getDocReader();
			if (docReader.getNativeFormat().equals("dlese_collect") &&
					((DleseCollectionDocReader) docReader).getKey().equals("collect")) {
				return true;
			}
		}
		return false;
	}


	/**
	 *  Control interaction with user to delete a set of records. The set of
	 *  records to be deleted is defined by a search, the results of which are
	 *  accessible via the SessionBean.
	 *
	 *@param  request  Description of the Parameter
	 *@param  bof      Description of the Parameter
	 *@param  mapping  Description of the Parameter
	 *@return          Description of the Return Value
	 */
	protected ActionForward handleBatchDelete(HttpServletRequest request, BatchOperationsForm bof, ActionMapping mapping) {
		prtln("handleBatchDelete");

		ActionErrors errors = new ActionErrors();
		SessionBean sessionBean = this.getSessionBean(request);
		String errorMsg;

		String command = request.getParameter("command");

		if (!roleManager.isAuthorized(RoleManager.BATCH_DELETE, sessionBean)) {
			errors.add("error", new ActionError("generic.error", "You are not authorized to perform a Batch Delete"));
			saveErrors(request, errors);
			return mapping.findForward("browse.query");
		}

		if (command != null && command.equalsIgnoreCase("removeRecords")) {
			return handleRemoveRecords(request, bof, mapping, "batch.delete.records");
		}

		if (command != null && command.equalsIgnoreCase("delete")) {
			// PERFORM THE BATCH OPEARATION
			RecordList recordBatch = bof.getRecordList();
			if (containsMasterCollectionRecord(recordBatch)) {
				errors.add("error", new ActionError("generic.error", "This list contains the master collection record, which cannot be deleted."));
				saveErrors(request, errors);
				return mapping.findForward("batch.delete.records");
			}
			try {
				if (!sessionBean.getBatchLocks(recordBatch)) {
					errors.add("error", new ActionError("batch.lock.not.obtained", "Batch Delete"));
					saveErrors(request, errors);
					return mapping.findForward("batch.delete.records");
				}
				RecordList failedRecordList =
						repositoryService.batchDeleteRecords(recordBatch);
				bof.setRecordList(null);
				bof.setFailedRecordList(failedRecordList);
				int failCount = failedRecordList.size();
				int moveCount = recordBatch.size() - failCount;
				errors.add("message",
						new ActionError("generic.message", moveCount + " records deleted"));
				if (failCount > 0) {
					errors.add("error", new ActionError("generic.error", failCount + " records could not be deleted"));
				}
			} catch (Exception e) {
				errorMsg = "batch delete error: " + e.getMessage();
				errors.add("error", new ActionError("generic.error", errorMsg));
			} finally {
				sessionBean.releaseAllLocks();
			}
			saveErrors(request, errors);
			return mapping.findForward("batch.delete.records");
		} else {
			prtln("set up the batch delete");
			bof.setRecordList(null);
			// the list of records to move comes from the current result list (results of last search)
			RecordList recordBatch = sessionBean.getRecords();
			prtln("record Batch is " + recordBatch.size() + " items");
			if (recordBatch.isEmpty()) {
				errors.add("error", new ActionError("generic.error", "no records selected to delete"));
				saveErrors(request, errors);
				return mapping.findForward("batch.delete.records");
			}

			List collections = this.getResultsCollections(recordBatch);

			if (collections.size() != 1) {
				errors.add("error", new ActionError("generic.error", "not all records are from the same collection"));
				saveErrors(request, errors);
				return mapping.findForward("batch.status.change");
			}

			if (containsMasterCollectionRecord(recordBatch)) {
				errors.add("error", new ActionError("generic.error", "This list contains the master collection record, which cannot be deleted."));
				saveErrors(request, errors);
				return mapping.findForward("batch.delete.records");
			}
			prtln("about to getBatch locks");
			if (!sessionBean.getBatchLocks(recordBatch)) {
				errors.add("error", new ActionError("batch.lock.not.obtained", "Batch Delete"));
				saveErrors(request, errors);
				return mapping.findForward("batch.delete.records");
			}

			bof.setRecordList(recordBatch);
			bof.setFailedRecordList(null);
			return mapping.findForward("batch.delete.records");
		}

	}


	/**
	 *  Remove records from the set of records to operate upon and return a forward
	 *  to the appropriate batch operation page.
	 *
	 *@param  request  Description of the Parameter
	 *@param  bof      Description of the Parameter
	 *@param  mapping  Description of the Parameter
	 *@param  forward  Description of the Parameter
	 *@return          Description of the Return Value
	 */
	private ActionForward handleRemoveRecords(
			HttpServletRequest request,
			BatchOperationsForm bof,
			ActionMapping mapping,
			String forward) {
		ActionErrors errors = new ActionErrors();
		SimpleLuceneIndex index = repositoryManager.getIndex();
		RecordList recsToRemove = new RecordList(request.getParameterValues("rmId"), index);

		if (!recsToRemove.isEmpty()) {
			// RecordList prunedRecs = new RecordList(new ArrayList(), index);
			RecordList prunedRecs = new RecordList(index);
			RecordList records = bof.getRecordList();
			for (Iterator i = records.iterator(); i.hasNext(); ) {
				String id = (String) i.next();
				if (!recsToRemove.contains(id)) {
					prunedRecs.add(id);
				} else {
					this.getSessionBean(request).releaseLock(id);
				}
			}
			bof.setRecordList(prunedRecs);
			errors.add("message", new ActionError("generic.message", recsToRemove.size() + " items removed from list."));

		} else {
			errors.add("error", new ActionError("generic.error", "Record list unchanged - no items were selected to remove"));
		}
		saveErrors(request, errors);
		return mapping.findForward(forward);
	}



	/**
	 *  Control interaction with user to accomplish a Batch Status update. The set
	 *  of records to be deleted is defined by a search, the results of which are
	 *  accessible via the SessionBean.
	 *
	 *@param  request  Description of the Parameter
	 *@param  bof      Description of the Parameter
	 *@param  mapping  Description of the Parameter
	 *@return          Description of the Return Value
	 */
	protected ActionForward handleBatchStatus(HttpServletRequest request,
			BatchOperationsForm bof,
			ActionMapping mapping) {
		prtln("handleBatchStatus");

		ActionErrors errors = new ActionErrors();
		SessionBean sessionBean = this.getSessionBean(request);
		User sessionUser = this.getSessionUser(sessionBean);
		String errorMsg;

		// Exit batch operation
		String command = request.getParameter("command");
		prtln("command is " + command);

		if (command != null && command.equalsIgnoreCase("removeRecords")) {
			return handleRemoveRecords(request, bof, mapping, "batch.status.change");
		}

		if (command != null && command.equalsIgnoreCase("status")) {
			prtln("status - we will update if possible");
			// we have status information from the form
			errors = validateStatusForm(request, bof);
			if (errors.size() > 0) {
				errors.add("error",
						new ActionError("edit.errors.found"));
				saveErrors(request, errors);
				return mapping.findForward("batch.status.change");
			}
			prtln("form validated");
			// create a new status entry
			String entryDate = SchemEditUtils.fullDateString(new Date());
			String userName = this.getSessionUserName(request);
			StatusEntry statusEntry = new StatusEntry(bof.getStatus(),
					bof.getStatusNote(),
					userName,
					entryDate);

			// PERFORM THE BATCH OPEARATION
			prtln("confirmed and about to do batch statusUpdate");
			RecordList recordBatch = bof.getRecordList();
			try {
				if (!sessionBean.getBatchLocks(recordBatch)) {
					errors.add("error", new ActionError("batch.lock.not.obtained", "Batch Status Change"));
					saveErrors(request, errors);
					return mapping.findForward("batch.status.change");
					/*
					 *  throw new Exception("failed to obtain locks on all the records to delete");
					 */
				}
				RecordList failedRecordList =
						repositoryService.batchStatusUpdate(recordBatch, statusEntry);
				bof.setRecordList(null);
				bof.setFailedRecordList(failedRecordList);
				int failCount = failedRecordList.size();
				int moveCount = recordBatch.size() - failCount;
				errors.add("message",
						new ActionError("generic.message", "status changed for " + moveCount + " records"));
				if (failCount > 0) {
					errors.add("error", new ActionError("generic.error", "status could not be update for " + failCount + " records"));
				}
			} catch (Exception e) {
				errorMsg = "batch operation error: " + e.getMessage();
				errors.add("error", new ActionError("generic.error", errorMsg));
			} finally {
				sessionBean.releaseAllLocks();
			}
			saveErrors(request, errors);
			return mapping.findForward("batch.status.change");
		} else {
			// set up to collect the new status information from the user
			bof.setRecordList(null);
			// the list of records to move comes from the current result list (results of last search)
			RecordList recordBatch = sessionBean.getRecords();
			if (recordBatch == null || recordBatch.size() < 1) {
				errors.add("error", new ActionError("generic.error", "no records selected"));
				saveErrors(request, errors);
				return mapping.findForward("batch.status.change");
			}

			// Status-SPECIFIC CLAUSE ------------------------------
			// make sure all of the records to move are from the same collection
			List collections = this.getResultsCollections(recordBatch);

			if (collections.size() != 1) {
				errors.add("error", new ActionError("generic.error", "not all records are from the same collection"));
				saveErrors(request, errors);
				return mapping.findForward("batch.status.change");
			}
			// - end of status-specific part

			String collection = (String) collections.get(0);

			if (!sessionBean.getBatchLocks(recordBatch)) {
				errors.add("error", new ActionError("batch.lock.not.obtained", "Batch Status Change"));
				saveErrors(request, errors);
				return mapping.findForward("batch.status.change");
			}

			prtln("getting status flags for collection (" + collection + ")");
			bof.setStatusFlags(getStatusFlags(collection));
			bof.setRecordList(recordBatch);
			bof.setFailedRecordList(null);
			bof.clearStatusAttributes();
			return mapping.findForward("batch.status.change");
		}

	}


	/**
	 *  Return a list of {@link StatusFlag} beans that describe the statuses that
	 *  can be assigned to a {@link StatusEntry}. This list is composed of the
	 *  UNKNOWN_status plus those statuses that defined for the collection. The
	 *  IMPORT status is not available in this context, since it is only assigned
	 *  by the system.
	 *
	 *@param  collection  Description of the Parameter
	 *@return             The statusFlags value
	 */
	private List getStatusFlags(String collection) {
		CollectionConfig info = collectionRegistry.getCollectionConfig(collection);
		return info.getAssignableStatusFlags();
	}


	/**
	 *  Validate the input from user. Put changed or default values into
	 *  statusForm. After this method returns, statusForm (rather than request) is
	 *  used to process user input
	 *
	 *@param  request  Description of the Parameter
	 *@param  bof      Description of the Parameter
	 *@return          Description of the Return Value
	 */
	private ActionErrors validateStatusForm(HttpServletRequest request, BatchOperationsForm bof) {
		ActionErrors errors = new ActionErrors();

		String status = request.getParameter("status");
		if (status == null) {
			errors.add("status", new ActionError("field.required", "Status"));
		} else if (status.equals(StatusFlags.IMPORTED_STATUS)) {
			errors.add("status", new ActionError("generic.error", "Please assign a status other than \"" + StatusFlags.IMPORTED_STATUS + "\""));
		} else {
			bof.setStatus(status);
		}

		return errors;
	}


	// -------------- Debug ------------------

	/**
	 *  Sets the debug attribute of the BatchOperationsAction class
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
			System.out.println("BatchOperationsAction: " + s);
		}
	}
}

