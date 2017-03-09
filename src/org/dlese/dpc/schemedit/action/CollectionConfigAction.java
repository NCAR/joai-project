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

import java.io.*;
import java.util.List;
import org.dlese.dpc.schemedit.Constants;
import org.dlese.dpc.schemedit.SchemEditUtils;
import org.dlese.dpc.schemedit.MissingLockException;
import org.dlese.dpc.schemedit.dcs.DcsDataManager;
import org.dlese.dpc.schemedit.dcs.DcsDataRecord;
import org.dlese.dpc.schemedit.dcs.DcsSetInfo;
import org.dlese.dpc.schemedit.input.CollectionConfigValidator;
import org.dlese.dpc.schemedit.input.SchemEditValidator;
import org.dlese.dpc.schemedit.action.form.SchemEditForm;
import org.dlese.dpc.schemedit.MetaDataFramework;
import org.dlese.dpc.schemedit.config.CollectionConfig;
import org.dlese.dpc.schemedit.ndr.NDRSync;
import org.dlese.dpc.xml.schema.*;

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
 *  Controller for the Collection Settings editor. Automatically reloads saved
 *  config file so that changes take effect immediately.
 *
 * @author    ostwald
 *
 *
 */
public final class CollectionConfigAction extends StandAloneSchemEditAction {

	private static boolean debug = false;
	private static boolean forceValidation = true;


	/**
	 *  Gets the validator attribute of the CollectionConfigAction object
	 *
	 * @param  form     Description of the Parameter
	 * @param  mapping  Description of the Parameter
	 * @param  request  Description of the Parameter
	 * @return          The validator value
	 */
	protected SchemEditValidator getValidator(ActionForm form, ActionMapping mapping, HttpServletRequest request) {
		SchemEditForm sef = (SchemEditForm) form;
		return new CollectionConfigValidator(this.collectionRegistry, sef, getMetaDataFramework(), mapping, request);
	}


	/**
	 *  Gets the recordsDir attribute of the CollectionConfigAction object
	 *
	 * @return    The recordsDir value
	 */
	protected File getRecordsDir() {
		return (File) servlet.getServletContext().getAttribute("collectionConfigDir");
	}


	/**
	 *  Gets the xmlFormat attribute of the CollectionConfigAction object
	 *
	 * @return    The xmlFormat value
	 */
	protected String getXmlFormat() {
		return "collection_config";
	}


	/**
	 *  Write the saved config file to disk, and update NDR if this collection is
	 *  registered with the NDR.
	 *
	 * @param  mapping                   the ActionMapping
	 * @param  form                      the ActionForm
	 * @param  request                   the Request
	 * @param  response                  the Response
	 * @param  validator                 the special validator for configs
	 * @return                           Description of the Return Value
	 * @exception  ServletException      Description of the Exception
	 * @exception  MissingLockException  if this config is currently being edited
	 *      by someone else
	 */
	protected ActionForward handleSaveRequest(ActionMapping mapping,
	                                          ActionForm form,
	                                          HttpServletRequest request,
	                                          HttpServletResponse response,
	                                          SchemEditValidator validator)
		 throws ServletException, MissingLockException {

		SchemEditForm sef = (SchemEditForm) form;
		MetaDataFramework metadataFramework = getMetaDataFramework();

		// BEFORE SAVING RECORD, we compare new and cashed values of the mdp: just in case this collection is
		// registered with the NDR and the mdp has changed, then we need to adjust item records - see below
		boolean updateNDRItems = mdpHasChanged(sef);
		boolean updateIDGenerator = idPrefixHasChanged (sef);

		ActionErrors errors = validator.validateForm();
		if (errors.size() > 0) {
			errors.add("error",
				new ActionError("generic.error",
				"Your input contains errors. This record cannot be saved until there are no errors"));
			saveErrors(request, errors);
			return getEditorMapping(mapping);
		}

		errors = saveRecord(mapping, form, request, response, validator);
		if (errors.size() == 0) {
			errors.add("message",
				new ActionError("save.confirmation"));
		}
		else {
			saveErrors(request, errors);
			return getEditorMapping(mapping);
		}

		String collection = sef.getCollection();
		CollectionConfig config = collectionRegistry.getCollectionConfig(collection);
		try {
			/*
				no need to save record again, but we do need to alert the
				registry to rebuild the data structures that compile info from
				the individual collections, such as workflow statuses,
				idPrefixes, ...
			*/
			config.refresh();
			collectionRegistry.updateMasterStatusList();
			
			if (updateIDGenerator) {
				collectionRegistry.initializeIDGenerator (config, this.repositoryManager.getIndex());
			}
			dcsDataManager.normalizeStatuses(collection);
			prtln("collection Info is updated");
			errors.add("message",
				new ActionError("generic.message", "Collection info is updated"));
		} catch (Exception e) {
			prtln("update collection error: " + e.getMessage());
			errors.add("pageErrors",
				new ActionError("generic.error", e.getMessage()));
		}

		if (config.isNDRCollection()) {
			boolean ndrServiceIsActive = false;
			try {
				ndrServiceIsActive = (Boolean) servlet.getServletContext().getAttribute("ndrServiceActive");
			} catch (Throwable t) {}

			if (ndrServiceIsActive) {
				try {
					DcsSetInfo setInfo = config.getSetInfo(this.repositoryManager);
					
					String userName = this.getSessionUserName(request);
					NDRSync ndrSync = new NDRSync(collection, userName, servlet.getServletContext());
					ndrSync.writeCollectionInfo(setInfo);

					errors.add("message",
						new ActionError("generic.message", "Collection Info synced to NDR"));

					if (updateNDRItems) {
						// update all metadata objects in NDR to point to new MDP
						ndrSync.syncItems();
					}
				} catch (Throwable t) {
					prtlnErr("Trouble writing collection config to NDR");
					t.printStackTrace();
				}
			}
			else {
				String errMsg = "Collection Info saved locally but not written to the NDR";
				errMsg += " (the NDR has been deactivated)";
				errors.add("message",
					new ActionError("generic.error", errMsg));
			}
		}

		saveErrors(request, errors);
		return getEditorMapping(mapping);
	}


	/**
	 *  Returns true if the metadataProvider value has changed (and is not empty),
	 *  meaning that NDR metadata objects must be updated to point to the new
	 *  metadataProvider.
	 *
	 * @param  sef  the Form
	 * @return      true if mdp has changed and is not empty
	 */
	boolean mdpHasChanged(SchemEditForm sef) {
		String mdpPath = "/collectionConfigRecord/ndrInfo/metadataProvider/handle";
		return updateActionRequired(sef, mdpPath);
	}


	boolean idPrefixHasChanged (SchemEditForm sef) {
		String xpath = "/collectionConfigRecord/idPrefix";
		return updateActionRequired(sef, xpath);
	}

	/**
	 *  Returns true if the value of the field specified by xpath has changed (and
	 *  is not empty), indicating that a follow-on action is required as a
	 *  consequence of the change.
	 *
	 * @param  sef    the Form
	 * @param  xpath  xpath of field to test
	 * @return        true if value at xpath has changed
	 */
	boolean updateActionRequired(SchemEditForm sef, String xpath) {
		String currentValue = (String) sef.getDocMap().get(xpath);
		String cachedValue = this.getCachedValue(xpath, sef);

		if (currentValue == null && cachedValue == null)
			return false;

		return !currentValue.equals(cachedValue);
	}


	/**
	 *  handle commands specific to this controller, call the superclasses
	 *  handleOtherCommands method if a command is not recognized
	 *
	 * @param  mapping                   Description of the Parameter
	 * @param  form                      Description of the Parameter
	 * @param  request                   Description of the Parameter
	 * @param  response                  Description of the Parameter
	 * @return                           Description of the Return Value
	 * @exception  ServletException      Description of the Exception
	 * @exception  MissingLockException  Description of the Exception
	 */
	protected ActionForward handleOtherCommands(ActionMapping mapping,
	                                            ActionForm form,
	                                            HttpServletRequest request,
	                                            HttpServletResponse response)
		 throws ServletException, MissingLockException {
		//----------------------------------------------------------------
		// additional commands we want to handle

		SchemEditForm sef = (SchemEditForm) form;
		SchemEditValidator validator = new SchemEditValidator(sef, getMetaDataFramework(), mapping, request);
		String command = request.getParameter("command");
		String recId = request.getParameter("recId");
		String errorMsg = "";

		ActionErrors errors = new ActionErrors();

		String collection = sef.getCollection();
		if (collection == null || collection.trim().length() == 0) {
			throw new ServletException("collection not defined in formBean");
		}
		sef.setSetInfo(SchemEditUtils.getDcsSetInfo(collection, repositoryManager));

		return super.handleOtherCommands(mapping, form, request, response);
	}


	/**
	 *  Gets the fileToEdit attribute of the CollectionConfigAction object
	 *
	 * @param  mapping        Description of the Parameter
	 * @param  form           Description of the Parameter
	 * @param  request        Description of the Parameter
	 * @param  schemaHelper   Description of the Parameter
	 * @return                The fileToEdit value
	 * @exception  Exception  Description of the Exception
	 */
	protected File getFileToEdit(ActionMapping mapping,
	                             ActionForm form,
	                             HttpServletRequest request,
	                             SchemaHelper schemaHelper)
		 throws Exception {

		prtln("getFileToEdit()");

		SchemEditForm sef = (SchemEditForm) form;
		ActionErrors errors = new ActionErrors();
		String errorMsg;

		String collection = request.getParameter("collection");
		String recId = request.getParameter("recId");

		prtln("collection is " + collection);

		CollectionConfig config = collectionRegistry.getCollectionConfig(collection);

		DcsSetInfo setInfo = SchemEditUtils.getDcsSetInfo(collection, repositoryManager);
		sef.setSetInfo(setInfo);
		config.setXmlFormat(setInfo.getFormat());

		MetaDataFramework framework = getMetaDataFramework(setInfo.getFormat());
		if (framework == null)
			throw new Exception("The metadata framework for " + setInfo.getFormat() + " is not loaded");

		config.updateTuples(framework);

		config.getConfigReader().flush();
		File file = config.getConfigReader().getSource();
		if (!file.exists()) {
			throw new Exception("could not create collection config file for " + collection);
		}

		return file;
	}


	/**
	 *  Print a line to standard out.
	 *
	 * @param  s  The String to print.
	 */
	protected void prtln(String s) {
		if (debug) {
			System.out.println("CollectionConfigAction: " + s);
		}
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  s  NOT YET DOCUMENTED
	 */
	protected void prtlnErr(String s) {
		System.out.println("CollectionConfigAction: " + s);
	}

}

