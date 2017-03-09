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
package org.dlese.dpc.repository.action;

import org.dlese.dpc.repository.action.form.*;
import org.dlese.dpc.repository.*;
import org.dlese.dpc.repository.indexing.*;
import org.dlese.dpc.xml.XMLValidator;
import org.dlese.dpc.vocab.*;
import org.dlese.dpc.vocab.MetadataVocab;
import org.dlese.dpc.oai.harvester.Harvester;
import org.dlese.dpc.index.*;
import org.dlese.dpc.util.Files;
import org.dlese.dpc.xml.Dom4jUtils;
import org.dlese.dpc.dds.DDSServlet;
import org.dlese.dpc.propertiesmgr.*;
import org.dlese.dpc.dds.ndr.*;
import javax.servlet.*;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.io.SAXReader;
import org.dom4j.Element;
import org.dom4j.Attribute;
import org.dom4j.Node;

import java.util.*;
import java.lang.*;
import java.io.*;
import java.text.*;
import java.util.Hashtable;
import java.util.Locale;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServletResponse;
import org.apache.struts.action.Action;
import org.apache.struts.action.ActionError;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.DynaActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionServlet;
import org.apache.struts.util.MessageResources;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;

/**
 *  Action that handles administration of an OAI metadata repository. <p>
 *
 *  Related documentation:<p>
 *
 *  See <a href="../../../../../javadoc-includes/ListSets-config-sample.xml">sample ListSets XML config file
 *  </a> .
 *
 * @author     John Weatherley
 * @version    $Id: RepositoryAdminAction.java,v 1.77 2009/12/22 01:54:56 jweather Exp $
 */
public final class RepositoryAdminAction extends Action {
	private static boolean debug = false;


	// --------------------------------------------------------- Public Methods

	/**
	 *  Process the specified HTTP request, and create the corresponding HTTP response (or forward to another web
	 *  component that will create it). Return an <code>ActionForward</code> instance describing where and how
	 *  control should be forwarded, or <code>null</code> if the response has already been completed.
	 *
	 * @param  mapping        The ActionMapping used to select this instance
	 * @param  response       The HTTP response we are creating
	 * @param  form           The ActionForm for the given page
	 * @param  req            The HTTP request.
	 * @return                The ActionForward instance describing where and how control should be forwarded
	 * @exception  Exception  If error.
	 */
	public ActionForward execute(
	                             ActionMapping mapping,
	                             ActionForm form,
	                             HttpServletRequest req,
	                             HttpServletResponse response)
		 throws Exception {

		/*
		 *  Design note:
		 *  Only one instance of this class gets created for the app and shared by
		 *  all threads. To be thread-safe, use only local variables, not instance
		 *  variables (the JVM will handle these properly using the stack). Pass
		 *  all variables via method signatures rather than instance vars.
		 */
		 

		// Extract attributes we will need
		Locale locale = getLocale(req);
		RepositoryAdminForm raf = (RepositoryAdminForm) form;
		ActionErrors errors = new ActionErrors();

		ServletContext servletContext = getServlet().getServletContext();

		RepositoryManager rm =
			(RepositoryManager) servletContext.getAttribute("repositoryManager");
			
		MetadataVocab vocab = (MetadataVocab) servletContext.getAttribute("MetadataVocab");
		
		// The record data source: can be 'fileSystem' an external indexer
		String recordDataSource = (String)servletContext.getAttribute("recordDataSource");

		// The IndexingManager is used if an external data source is managing the index rather than the internal FileIndexingService
		IndexingManager indexingManager = (IndexingManager) servletContext.getAttribute("indexingManager");	
				
		// Are we using the IndexingManager for external data source instead of files?
		boolean isIndexedByIndexingManager = false;
		if(recordDataSource != null && indexingManager != null && !recordDataSource.equals("fileSystem"))
			isIndexedByIndexingManager = true;
		
		String enableNewSets = (String) servletContext.getAttribute("enableNewSets");

		try {			
			// Set up the bean with data:
			raf.setOaiIdPfx(rm.getOaiIdPrefix());
			raf.setNumIdentifiersResults(rm.getNumIdentifiersResults());
			raf.setNumRecordsResults(rm.getNumRecordsResults());			
			raf.setRepositoryIdentifier(rm.getRepositoryIdentifier());
			raf.setBaseURL(rm.getProviderBaseUrl(req));
			raf.setUpdateFrequency(rm.getUpdateFrequency());
			raf.setIndexingStartTime(rm.getIndexingStartTime());
			raf.setIdexingDaysOfWeek(rm.getIndexingDaysOfWeek());
			raf.setCollectionRecordsLocation(rm.getCollectionRecordsLocation());
			raf.setMetadataRecordsLocation(rm.getMetadataRecordsLocation());
			raf.setSets(rm.getSetInfos());
			raf.setDrcBoostFactor(rm.getDrcBoostFactor());
			raf.setMultiDocBoostFactor(rm.getMultiDocBoostFactor());
			raf.setTitleBoostFactor(rm.getTitleBoostFactor());
			raf.setStemmingEnabled(Boolean.toString(rm.isStemmingEnabled()));
			raf.setTrustedWsIps(rm.getTrustedWsIps());
			raf.setNumIndexingErrors(rm.getNumIndexingErrors());
			if(isIndexedByIndexingManager) {
				raf.setIndexingStartTime(indexingManager.getIndexingStartTime());
				raf.setIdexingDaysOfWeek(indexingManager.getIndexingDaysOfWeek());
			}
		} catch (Throwable t) {
			prtlnErr("Error: " + t);
			if (t instanceof java.lang.NullPointerException)
				t.printStackTrace();
		}
		
		// If the user clicked the "Cancel" button, do nothing:
		if (isCancelled(req)) {
			//errors.add("message", new ActionError("generic.message", "Canceled - nothing done"));
			//saveErrors(req, errors);
			return mapping.findForward("display.repository.settings");
		}
		
		
		try {
			
			String command = req.getParameter("command");
			
			// ---- Dispatch actions that come in as Form Beans: ----
			
			// Add an OAI set definition (Note: This sets forms config is a good action/form forwrding scheme to follow):
			SetDefinitionsForm setDefinitionsForm = (SetDefinitionsForm) req.getAttribute("setDefinitionsForm");
			if (setDefinitionsForm != null) {
				return doAddSetDefinition(errors, mapping, rm, req, raf, setDefinitionsForm);
			}			

			// Add/edit repository info:
			RepositoryInfoForm repositoryInfoForm = (RepositoryInfoForm) req.getAttribute("repositoryInfoForm");
			if (repositoryInfoForm != null) {
				return doSetRepositoryInfo(errors, mapping, rm, req, raf, repositoryInfoForm);
			}				
			
			// ---- Handle admin actions indicated by the "command" parameter: ----
			if (command != null) {

				prtln("Running command " + command);
				
				// Enable / Disable the data provider:
				if (command.equals("changeProviderStatus")) {
					String status = req.getParameter("providerStatus");
					try {
						rm.setProviderStatus(status);
					} catch (Throwable t) {
						errors.add("error", new ActionError("generic.error", "Could not change provider status: " + t));
						saveErrors(req, errors);
						return mapping.findForward("display.repository.settings");				
					}
					errors.add("message", new ActionError("generic.message", "The data provider has been " + status.toLowerCase() + "."));
					saveErrors(req, errors);
					return mapping.findForward("display.repository.settings");
				}
		
				// Delete an OAI set definition (Note: This sets forms config is a good action/form forwrding scheme to follow)
				if (command.equals("deleteSetDefinition")) {
					String setSpec = req.getParameter("setSpec");
					String setName = req.getParameter("setName");
					prtln("Running command " + command + " for setSpec: " + setSpec);

					try {
						rm.removeOAISetSpecDefinition(setSpec);
					} catch (Throwable t) {
						errors.add("error", new ActionError("generic.error", "An error occured while trying to delete set '" + setName + "'. Message: " + t));
						saveErrors(req, errors);
						return mapping.findForward("display.repository.settings");
					}

					errors.add("message", new ActionError("generic.message", "The set '" + setName + "' has been deleted"));
					saveErrors(req, errors);

					return mapping.findForward("display.repository.settings");
				}
				// Update an OAI set definition
				if (command.equals("updateSetDefinition")) {
					String setSpec = req.getParameter("edit");
					prtln("Running command " + command + " for setSpec: " + setSpec);
					SetDefinitionsForm updateSetDefinitionsForm = null;

					// Populate the SetDefinitionsForm with the previous values:
					try {
						updateSetDefinitionsForm = OAISetsXMLConfigManager.getSetDefinitionsForm(rm.getListSetsConfigXml(), setSpec);
					} catch (Throwable t) {
						errors.add("error", new ActionError("generic.error", "An error occured while trying to edit a set. Message: " + t));
						saveErrors(req, errors);
						return mapping.findForward("display.repository.settings");
					}
					if (updateSetDefinitionsForm == null) {
						errors.add("error", new ActionError("generic.error", "Can not edit set '" + setSpec + "' because it does not exist"));
						saveErrors(req, errors);
						return mapping.findForward("display.repository.settings");
					}
					updateSetDefinitionsForm.setServlet(getServlet()); 
					req.setAttribute("setDefinitionsForm", updateSetDefinitionsForm);

					return mapping.findForward("update.set.definition");
				}
				
				// Update repository information
				if (command.equals("updateRepositoryInfo")) {
					prtln("Running command " + command);

					// Populate the RepositoryInfoForm with the previous values:
					try {
						repositoryInfoForm = new RepositoryInfoForm();
						repositoryInfoForm.setNamespaceIdentifier(rm.getRepositoryIdentifier());
						repositoryInfoForm.setRepositoryName(rm.getRepositoryName());
						repositoryInfoForm.setRepositoryDescription(rm.getDescription(0));
						repositoryInfoForm.setAdminEmail(rm.getAdminEmail(0));
					} catch (Throwable t) {
						errors.add("error", new ActionError("generic.error", "An error occured while trying to update the repository info. Message: " + t));
						saveErrors(req, errors);
						return mapping.findForward("display.repository.settings");
					}
					req.setAttribute("repositoryInfoForm", repositoryInfoForm);

					return mapping.findForward("update.repository.info");
				}

				// Update repository information index using external indexer
				if (command.equals("indexingManagerAction")) {
					String imCommand = req.getParameter("imCommand");
					prtln("Running Indexing Manager command " + imCommand);
					
					if (indexingManager == null) {
						errors.add("error", new ActionError("generic.error", "Can not index from external source - no IndexingManager is configured in the system"));
						saveErrors(req, errors);
						return mapping.findForward("display.repository.settings");
					}
						
					if(imCommand.equals("indexAllCollections")) {
						try {
							indexingManager.fireIndexAllCollectionsEvent(null);
							errors.add("message", new ActionError("generic.message", "IndexingManager: Indexing all collections has begun."));
						} catch (Throwable t) {
							errors.add("error", new ActionError("generic.error", "There was an error reported from the IndexingManager: " + t));
							saveErrors(req, errors);
							if(true || t instanceof java.lang.NullPointerException)
								t.printStackTrace();
						}
					}
					
					if(imCommand.equals("abortIndexing")) {
						try {
							indexingManager.fireAbortIndexingEvent(null);
							errors.add("message", new ActionError("generic.message", "IndexingManager: Indexing requested to stop..."));
						} catch (Throwable t) {
							errors.add("error", new ActionError("generic.error", "There was an error reported from the IndexingManager: " + t));
							if(true || t instanceof java.lang.NullPointerException)
								t.printStackTrace();
						}
					}					
					
					saveErrors(req, errors);
					return mapping.findForward("display.repository.settings");
				}					
				
				// Delete a set/collection from the repository config and index (this is the file dirs):
				if (command.equals("removeSetBySetSpec")) {
					String setSpec = req.getParameter("setSpec");
					if (setSpec == null || setSpec.length() == 0) {
						errors.add("error", new ActionError("generic.error", "Can not remove set - no setSpec specified"));
						saveErrors(req, errors);
						return mapping.findForward("display.repository.settings");
					}

					SetInfo si = rm.removeSetBySetSpec(req.getParameter("setSpec"));
					if (si != null)
						errors.add("message", new ActionError("generic.message", "'" + si.getName() + "' was removed from the respository"));
					else
						errors.add("message", new ActionError("generic.message", setSpec + " was not configured and so was not removed"));
					saveErrors(req, errors);

					// Put the sets in the bean for display...
					raf.setSets(rm.getSetInfos());
					return mapping.findForward("display.repository.settings");
				}
				
				// Add metadata dir
				if (command.equals("addMetadataDir")) {
					// Add new directory of metadata files to the repository:
					MetadataDirectoryInfoForm metadataDirsForm = (MetadataDirectoryInfoForm) req.getAttribute("metadataDirsForm");
					if (metadataDirsForm != null) {
						return doAddNewMetadataFileDir(errors, mapping, rm, req, raf, metadataDirsForm);
					}				
				}
				
				// Update metadata dir info
				if (command.equals("updateMetadataDir")) {
					// Edit settings for existing directory of metadata files to the repository:
					MetadataDirectoryInfoForm metadataDirsForm = (MetadataDirectoryInfoForm) req.getAttribute("metadataDirsForm");
					if (metadataDirsForm != null) {
						return doEditMetadataFileDir(errors, mapping, rm, req, raf, metadataDirsForm);
					}					
				}
				
				// Update the index:
				if (command.equals("Update modified files")) {
					rm.indexFiles(new SimpleFileIndexingObserver("Update modified files action", "Starting indexing"), false);
					errors.add("message", new ActionError("generic.message", "Index is being updated"));
					errors.add("showIndexMessagingLink", new ActionError("generic.message", ""));
					saveErrors(req, errors);
					return mapping.findForward("display.repository.settings");
				}
				// Re-index all files in the index:
				if (command.equals("Reindex all files")) {
					boolean indexAll = true;
					String doIndexAll = req.getParameter("indexAll");
					if(doIndexAll != null && doIndexAll.trim().toLowerCase().equals("false"))
						indexAll = false;
					rm.indexFiles(new SimpleFileIndexingObserver("Reindex action", "Starting indexing"), indexAll);
					errors.add("message", new ActionError("generic.message", "All files are being re-indexed."));
					errors.add("message", new ActionError("generic.message", "Changes may take several several minutes to appear"));
					errors.add("showIndexMessagingLink", new ActionError("generic.message", ""));
					saveErrors(req, errors);
					return mapping.findForward("display.repository.settings");
				}

				// Index all files in the the given collection/directory:
				if (command.equals("reindexCollection")) {
					String key = req.getParameter("key");
					String displayName = req.getParameter("displayName");
					if (displayName == null)
						displayName = key;					
					
					// Handle indexing from IndexingManager:
					if(isIndexedByIndexingManager) {
						try{
							indexingManager.fireIndexCollectionEvent(key,null);
							errors.add("message", new ActionError("generic.message", "Records for '" + displayName + "' are being indexed."));
						} catch (Throwable t) {
							errors.add("error", new ActionError("generic.error", "Records for '" + displayName + "' could not being indexed. There was an error: " + t.getMessage()));
						}
					}
					// Handle indexing from FileIndexingService:
					else {
	
						boolean indexAll = true;
						String ia = req.getParameter("indexAll");
						if (ia != null && ia.equalsIgnoreCase("false"))
							indexAll = false;
												
						if (rm.indexCollection(key, new SimpleFileIndexingObserver("Collection indexer for '" + displayName + "'", "Starting indexing"), indexAll)) {
							errors.add("message", new ActionError("generic.message", "Files for '" + displayName + "' are being indexed."));
							errors.add("message", new ActionError("generic.message", "Changes may take several several minutes to appear"));
							errors.add("showIndexMessagingLink", new ActionError("generic.message", ""));
						}
						else {
							errors.add("error", new ActionError("generic.error", "'" + displayName + "' is not configured in the repository. Unable to index files."));
						}
					}
					saveErrors(req, errors);
					return mapping.findForward("display.repository.settings");
				}

				// Delete and rebuild the index:
				if (command.equals("rebuildIndex")) {
					String msg = "Index is being deleted.";
					if(isIndexedByIndexingManager) {
						try {
							indexingManager.fireAbortIndexingEvent(null);
							Thread.sleep(2000);
						} catch (Throwable t) {
							errors.add("error", new ActionError("generic.error", "There was an error reported from the IndexingManager: " + t));
							if(true || t instanceof java.lang.NullPointerException)
								t.printStackTrace();
						}
					}
					rm.deleteIndex();
					rm.indexFiles(new SimpleFileIndexingObserver("Delete/rebuild index", "Starting indexing"), true);
					errors.add("message", new ActionError("generic.message", msg));
					errors.add("showIndexMessagingLink", new ActionError("generic.message", ""));
					saveErrors(req, errors);
					return mapping.findForward("display.repository.settings");
				}
				// Stop indexing:
				if (command.equals("stopIndexing")) {
					rm.stopIndexing();
					errors.add("message", new ActionError("generic.message", "Indexing has stopped."));
					errors.add("showIndexMessagingLink", new ActionError("generic.message", ""));
					saveErrors(req, errors);
					return mapping.findForward("display.repository.settings");
				}
				// Show indexing status messages:
				if (command.equals("showIndexingMessages")) {
					List indexingMessages = null;
					if(isIndexedByIndexingManager) {
						indexingMessages = indexingManager.getIndexingMessages();	
					}
					else {
						indexingMessages = rm.getIndexingMessages();
					}
					for (int i = indexingMessages.size() - 1; i >= 0; i--) {
						errors.add("message", new ActionError("generic.message", (String) indexingMessages.get(i)));
					}
										
					errors.add("showIndexMessagingLink", new ActionError("generic.message", ""));
					saveErrors(req, errors);
					return mapping.findForward("display.repository.settings");
				}
				// Load/reload the OAI sets configuration
				if (command.equals("loadOAISetsConfig")) {
					try {
						rm.loadListSetsConfigFile();
					} catch (Throwable t) {
						errors.add("error", new ActionError("generic.error", "Error loading OAI sets config: " + t));
						if (t instanceof NullPointerException)
							t.printStackTrace();
						saveErrors(req, errors);
						return mapping.findForward("display.repository.settings");
					}
					String sets = rm.getVirtualSearchFieldMapper().toString();
					errors.add("message", new ActionError("generic.message", "OAI List Sets configuration has been loaded: " + sets));
					saveErrors(req, errors);
					return mapping.findForward("display.repository.settings");
				}
				// Reload the RepositoryManager and DDS config files
				if (command.equals("reloadRepositoryConfig")) {
					rm.reloadConfigFiles();
					try {
						PropertiesManager ddsConfigProperties = (PropertiesManager) servletContext.getAttribute("ddsConfigProperties");
						ddsConfigProperties.loadPropertiesFile();
					} catch (Throwable t) {
						errors.add("error", new ActionError("generic.error", "Error loading DDS properties: " + t));
						saveErrors(req, errors);
						return mapping.findForward("display.repository.settings");
					}
					errors.add("message", new ActionError("generic.message", "Configuration files have been re-loaded."));
					saveErrors(req, errors);
					return mapping.findForward("display.repository.settings");
				}
				// Update the trusted web service IPs
				if (command.equals("setTrustedIps")) {
					String trustedIps = req.getParameter("trustedWsIps");
					if (trustedIps != null) {
						rm.setTrustedWsIps(trustedIps);
						raf.setTrustedWsIps(rm.getTrustedWsIps());
						errors.add("message", new ActionError("generic.message", "The trusted web service IPs have been updated."));
						saveErrors(req, errors);
					}
					return mapping.findForward("display.repository.settings");
				}
				// Reload vacabs:
				if (command.equals("Reload vocabulary")) {
					if (vocab != null) {
						LoadMetadataVocabs loadVocabs =
							(LoadMetadataVocabs) servlet.getServletContext().getAttribute("LoadMetadataVocabs");
						if (loadVocabs != null) {
							loadVocabs.load();
						}
						ArrayList msgs = vocab.getMessages();
						if (msgs != null) {
							for (int i = 0; i < msgs.size(); i++) {
								errors.add("message", new ActionError("generic.message", msgs.get(i)));
							}
						}

						ArrayList err = vocab.getErrors();
						if (err != null) {
							for (int i = 0; i < err.size(); i++) {
								errors.add("error", new ActionError("generic.error", err.get(i)));
							}
						}
					}
					saveErrors(req, errors);
					return mapping.findForward("display.repository.settings");
				}
				// Update the boost factors:
				if (command.equals("ubf")) {
					double titleBoostFactor = 1;
					double drcBoostFactor = 1;
					double stemmingBoostFactor = 1;
					double multiDocBoostFactor = 1;

					if (req.getParameter("titleBoostFactor") != null) {
						try {
							titleBoostFactor = Double.parseDouble(req.getParameter("titleBoostFactor"));
							if (!(titleBoostFactor >= 0.0))
								errors.add("error", new ActionError("generic.error", "Title boost factor must be a number greater than or equal to zero."));
							else
								rm.setTitleBoostFactor(titleBoostFactor);
						} catch (Throwable e) {
							errors.add("error", new ActionError("generic.error", "Incorrect value " + e.getMessage() + ". Title boost factor must be a number greater than or equal to zero."));
						}
					}

					if (req.getParameter("drcBoostFactor") != null) {
						try {
							drcBoostFactor = Double.parseDouble(req.getParameter("drcBoostFactor"));
							if (!(drcBoostFactor >= 0.0))
								errors.add("error", new ActionError("generic.error", "DRC boost factor must be a number greater than or equal to zero."));
							else
								rm.setDrcBoostFactor(drcBoostFactor);
						} catch (Throwable e) {
							errors.add("error", new ActionError("generic.error", "Incorrect value " + e.getMessage() + ". DRC boost factor must be a number greater than or equal to zero."));
						}
					}

					if (req.getParameter("multiDocBoostFactor") != null) {
						try {
							multiDocBoostFactor = Double.parseDouble(req.getParameter("multiDocBoostFactor"));
							if (!(multiDocBoostFactor >= 0.0))
								errors.add("error", new ActionError("generic.error", "Multi-record boost factor must be a number greater than or equal to zero."));
							else
								rm.setMultiDocBoostFactor(multiDocBoostFactor);
						} catch (Throwable e) {
							errors.add("error", new ActionError("generic.error", "Incorrect value " + e.getMessage() + ".  Multi-record boost factor must be a number greater than or equal to zero."));
						}
					}

					if (req.getParameter("stemmingBoostFactor") != null) {
						try {
							stemmingBoostFactor = Double.parseDouble(req.getParameter("stemmingBoostFactor"));
							if (!(stemmingBoostFactor >= 0.0))
								errors.add("error", new ActionError("generic.error", "Stemming boost factor must be a number greater than or equal to zero."));
							rm.setStemmingBoostFactor(stemmingBoostFactor);
						} catch (Throwable e) {
							errors.add("error", new ActionError("generic.error", "Incorrect value " + e.getMessage() + ". Stemming boost factor must be a number greater than or equal to zero."));
						}
					}

					if (req.getParameter("resetDefaultBoosting") != null) {
						try {
							rm.resetBoostingFactorDefaults();
						} catch (Throwable e) {
							errors.add("error", new ActionError("generic.error", "Unable to update boosting values to defaults: " + e.getMessage()));
						}
					}

					if (errors.isEmpty()) {
						raf.setDrcBoostFactor(rm.getDrcBoostFactor());
						raf.setMultiDocBoostFactor(rm.getMultiDocBoostFactor());
						raf.setTitleBoostFactor(rm.getTitleBoostFactor());
						raf.setStemmingEnabled(Boolean.toString(rm.isStemmingEnabled()));
						errors.add("message", new ActionError("generic.message", "Search boosting factors have been updated."));
					}

					saveErrors(req, errors);
					return mapping.findForward("display.repository.settings");
				}
			}

			// ------ Save data into the rm ----------

			if (req.getParameter("setStemmingEnabled") != null) {
				if (req.getParameter("setStemmingEnabled").equalsIgnoreCase("true")) {
					rm.setStemmingEnabled(true);
					raf.setStemmingEnabled("true");
					errors.add("message", new ActionError("generic.message", "Stemming has been enabled."));
				}
				else {
					rm.setStemmingEnabled(false);
					raf.setStemmingEnabled("false");
					errors.add("message", new ActionError("generic.message", "Stemming has been disabled."));
				}
			}

			if (req.getParameter("removeInvalidRecords") != null) {
				rm.setRemoveInvalidRecords(req.getParameter("removeInvalidRecords").trim());
			}

			if (req.getParameter("validateRecords") != null) {
				rm.setValidateRecords(req.getParameter("validateRecords").trim());
			}

			if (req.getParameter("numIdentifiersResults") != null) {
				rm.setNumIdentifiersResults(req.getParameter("numIdentifiersResults").trim());
				raf.setNumIdentifiersResults(rm.getNumIdentifiersResults());
			}

			if (req.getParameter("numRecordsResults") != null) {
				rm.setNumRecordsResults(req.getParameter("numRecordsResults").trim());
				raf.setNumRecordsResults(rm.getNumRecordsResults());
			}

			if (req.getParameter("repositoryName") != null) {
				rm.setRepositoryName(req.getParameter("repositoryName").trim());
			}

			if (req.getParameter("repositoryIdentifier") != null) {
				if (req.getParameter("remove") != null) {
					rm.setRepositoryIdentifier("");
					raf.setRepositoryIdentifier("");
				}
				else {
					rm.setRepositoryIdentifier(req.getParameter("repositoryIdentifier").trim());
					raf.setRepositoryIdentifier(rm.getRepositoryIdentifier());
				}
			}

			// Handle admin-emails
			if (req.getParameter("currentAdminEmail") != null) {
				prtln("Handling adminEmail updates...");

				// Add an admin-email
				if (req.getParameter("add") != null) {
					if (!req.getParameter("currentAdminEmail").equals("")) {
						rm.addAdminEmail(req.getParameter("currentAdminEmail"));
					}
				}
				else {
					int i = getIntValue(req.getParameter("currentIndex"));

					// Remove an admin-email
					if (req.getParameter("remove") != null) {
						String rmAdminEmail = rm.getAdminEmail(i);
						if (rmAdminEmail != null && rmAdminEmail.equals(req.getParameter("currentAdminEmail"))) {
							rm.removeAdminEmail(i);
						}
					}
					// Update the admin-email:
					else {
						rm.replaceAdminEmail(i, req.getParameter("currentAdminEmail"));
					}
				}
			}

			// Handle descriptions
			if (req.getParameter("currentDescription") != null) {
				prtln("Handling currentDescription updates...");
				String validationReport = null;

				// Add a description
				if (req.getParameter("add") != null) {
					if (!req.getParameter("currentDescription").equals("")) {
						validationReport = validateXML(req.getParameter("currentDescription"));
						if (validationReport != null) {
							raf.setXmlError(validationReport);
							return mapping.findForward("edit.repository.settings");
						}
						else {
							rm.addDescription(req.getParameter("currentDescription"));
						}
					}
				}
				else {
					int i = getIntValue(req.getParameter("currentIndex"));

					// Remove a description
					if (req.getParameter("remove") != null) {
						String rmDescription = rm.getDescription(i);
						if (rmDescription != null && rmDescription.equals(req.getParameter("currentDescription"))) {
							rm.removeDescription(i);
						}
					}
					// Update the description:
					else {
						validationReport = validateXML(req.getParameter("currentDescription"));
						if (validationReport != null) {
							raf.setXmlError(validationReport);
							return mapping.findForward("edit.repository.settings");
						}
						else {
							rm.replaceDescription(i, req.getParameter("currentDescription"));
						}
					}
				}
			}

			// ---------- Handle all auto set/collection configuration from collection-level records ----------------------

			// Load collection-level records
			if (req.getParameter("collectionRecordsAdmin") != null) {

				String paramVal = req.getParameter("collectionRecordsAdmin");

				// Load the collection-level records
				if (paramVal.equals("loadCollectionRecords")) {
					// Signal the UI that collection menu's need to be re-loaded:
					servletContext.setAttribute("reload_admin_collection_menus", "true");
					if(isIndexedByIndexingManager) {
						indexingManager.fireUpdateCollectionsEvent(null);
					} else {
						// Load the collections:
						rm.loadCollectionRecords(true);							
					}
					errors.add("message", new ActionError("generic.message", "Loaded collection records."));
					saveErrors(req, errors);
					return mapping.findForward("display.repository.settings");
				}
			}

			// ---------- Handle all manual set/collection updates (Old method) ----------------------

			if (req.getParameter("currentSetSpec") != null) {
				prtln("Handling currentSet updates...");

				// Add a set
				if (req.getParameter("add") != null) {
					if (!req.getParameter("currentSetSpec").equals("")) {
						String currentSetDescription =
							req.getParameter("currentSetDescription");
						if (currentSetDescription == null) {
							currentSetDescription = "";
						}
						else {
							currentSetDescription = req.getParameter("currentSetDescription").trim();
						}

						if (!isValidSet(req.getParameter("currentSetName").trim(),
							req.getParameter("currentSetSpec").trim(),
							currentSetDescription, raf, req, true, rm)) {
							return mapping.findForward("edit.repository.settings");
						}

						if (enableNewSets == null || !enableNewSets.equalsIgnoreCase("false")) {
							enableNewSets = "true";
						}

						SetInfo set = new SetInfo(
							req.getParameter("currentSetName").trim(),
							req.getParameter("currentSetSpec").trim(),
							currentSetDescription,
							enableNewSets,
							null, null, null);

						prtln("Adding new set: " + set.toString());
						rm.addSetInfo(set);
					}
				}
				else {
					int i = getIntValue(req.getParameter("currentIndex"));

					// Enable / Disable sets/collections
					if (req.getParameter("disableSet") != null || req.getParameter("enableSet") != null) {

						// Use UID method
						if (req.getParameter("setUid") != null) {

							if (req.getParameter("enableSet") != null) {
								rm.enableSet(req.getParameter("setUid"));
								prtln("enabling " + req.getParameter("setUid"));
							}
							else {
								rm.disableSet(req.getParameter("setUid"));
								prtln("disabling " + req.getParameter("setUid"));
							}
							raf.setSets(rm.getSetInfos());
						}

						// Use old method
						else {
							SetInfo set = rm.getSetInfoCopy(i);
							// Make sure we have the right set!
							if (set != null &&
								set.getSetSpec().equals(req.getParameter("currentSetSpec").trim()) &&
								set.getName().equals(req.getParameter("currentSetName").trim())) {

								if (req.getParameter("disableSet") != null)
									set.setEnabled("false");
								else
									set.setEnabled("true");

								prtln("Replacing set: " + set.toString());
								rm.replaceSetInfo(i, set);
							}
						}
						
						// Re-load metadata vocab to re-set the menus, etc.
						MetadataVocabServlet vocabsServlet =
							(MetadataVocabServlet)servlet.getServletContext().getAttribute("MetadataVocabServlet");
						if ( vocabsServlet != null )
						   vocabsServlet.loadVocabs();
					}

					// Remove a set
					else if (req.getParameter("remove") != null) {
						SetInfo rmSet = rm.getSetInfoCopy(i);
						// Make sure we have the right set!
						if (rmSet != null &&
							rmSet.getName().equals(req.getParameter("currentSetName").trim())
							 && rmSet.getSetSpec().equals(req.getParameter("currentSetSpec").trim())
							 && rmSet.getDescription().equals(req.getParameter("currentSetDescription").trim())) {
							rm.removeSetInfo(i);
						}
					}
					// Update the set:
					else if (req.getParameter("edit") == null) {
						SetInfo set = rm.getSetInfoCopy(i);
						if (set != null) {
							String setSpec = req.getParameter("currentSetSpec");
							String setName = req.getParameter("currentSetName");
							String setDescription = req.getParameter("currentSetDescription");
							setSpec = setSpec == null ? "" : setSpec.trim();
							setName = setName == null ? "" : setName.trim();
							setDescription = setDescription == null ? "" : setDescription.trim();

							boolean isNewSetSpec =
								(setSpec != null && set.getSetSpec().equals(setSpec) ? false : true);
							if (!isValidSet(setName,
								setSpec,
								setDescription, raf, req, isNewSetSpec, rm)) {
								return mapping.findForward("edit.repository.settings");
							}

							set.setName(setName);
							set.setSetSpec(setSpec);
							set.setDescription(setDescription);

							prtln("Replacing set: " + set.toString());
							rm.replaceSetInfo(i, set);
						}
					}
				}
				raf.setSets(rm.getSetInfos());
				if (vocab != null &&
					servletContext.getAttribute ("suppressSetCollectionsVocabDisplay") == null) {
					DDSServlet.setCollectionsVocabDisplay(vocab, rm);
				}
			}
			// end set/collection configuration

			// Handle adding/deleting directories of files to the repository:
			if (req.getParameter("dirConf") != null) {
				String dirConf = req.getParameter("dirConf");

				// Add a directory to the repository:
				if (dirConf.equals("add")) {
					String dir = req.getParameter("dir");
					if (dir == null) {
						errors.add("message", new ActionError("generic.error", "Error: no directory was specified."));
						saveErrors(req, errors);
						return mapping.findForward("display.repository.settings");
					}
					File f = new File(dir);
					if (!f.isDirectory()) {
						errors.add("message", new ActionError("generic.error", "Error: '" + dir + "' is not a valid directory on the server. Please ensure the directory exists and try again."));
						saveErrors(req, errors);
						return mapping.findForward("display.repository.settings");
					}
				}
				else if (dirConf.equals("delete")) {

				}
			}

			// Handle directory/formats for sets/collections
			if (req.getParameter("currentSetFormat") != null
				 && req.getParameter("currentSetDirectory").trim().length() != 0) {
				prtln("Handling currentSetFormat updates...");

				int setIndex = getIntValue(req.getParameter("currentIndex"));
				int setDirInfoIndex = getIntValue(req.getParameter("currentDirInfoIndex"));

				SetInfo tmp = rm.getSetInfoCopy(setIndex);

				// Add a directory/format for this set
				if (req.getParameter("add") != null) {
					if (setDirInfoIndex == -1) {
						File setDirectory = new File(req.getParameter("currentSetDirectory").trim());
						prtln("Checking Add dir: " + setDirectory.getAbsolutePath());
						if (rm.isDirectoryConfigured(setDirectory)) {
							errors.add("error", new ActionError("generic.message", "The directory indicated is already taken. Each directory may only be configured once."));
							saveErrors(req, errors);
							return mapping.findForward("edit.repository.settings");
						}
						tmp.addDirInfo(req.getParameter("currentSetFormat").trim(), req.getParameter("currentSetDirectory").trim());
						rm.replaceSetInfo(setIndex, tmp);
						errors.add("message", new ActionError("generic.message", "New files are now being indexed."));
						errors.add("message", new ActionError("generic.message", "Changes may take several several minutes to appear"));
						errors.add("showIndexMessagingLink", new ActionError("generic.message", ""));
						saveErrors(req, errors);
					}
				}
				else {
					// Remove directoery for the set
					if (req.getParameter("remove") != null) {
						prtln("removing dir: ");
						//prtln("getting setIndex: " + setIndex + " setDirInfoIndex: " + setDirInfoIndex);
						SetInfo currentSet = rm.getSetInfoCopy(setIndex);
						DirInfo di = new DirInfo(req.getParameter("currentSetDirectory").trim(), req.getParameter("currentSetFormat").trim());
						prtln("removing dir: " + di);
						if (currentSet.getDirInfo(setDirInfoIndex).equals(di)) {
							prtln("removingDirInfo: " + di.toString());
							currentSet.removeDirInfo(setDirInfoIndex);
							rm.replaceSetInfo(setIndex, currentSet);
							errors.add("message", new ActionError("generic.message", "The directory of files has been removed from the index."));
							saveErrors(req, errors);
						}
						else {
							prtln("NOT removingDirInfo: " + di.toString());
						}
					}

					// Update a direcotry/format tuple for this set:
					else if (setDirInfoIndex != -1 && req.getParameter("edit") == null) {

						prtln("Update SetInfo");
						SetInfo modSet = rm.getSetInfoCopy(setIndex);

						DirInfo modDirInfo = modSet.getDirInfo(setDirInfoIndex);

						File setDirectory = new File(req.getParameter("currentSetDirectory").trim());
						prtln("Checking update dir: " + setDirectory.getAbsolutePath());
						if (rm.isDirectoryConfigured(setDirectory) && !modDirInfo.hasDirectory(setDirectory)) {
							errors.add("error", new ActionError("generic.message", "The directory indicated is already taken. Each directory may only be configured once."));
							saveErrors(req, errors);
							return mapping.findForward("edit.repository.settings");
						}

						modSet.setDirInfo(
							setDirInfoIndex,
							req.getParameter("currentSetDirectory"),
							req.getParameter("currentSetFormat"));

						rm.replaceSetInfo(setIndex, modSet);
						errors.add("message", new ActionError("generic.message", "New files are now being indexed."));
						errors.add("message", new ActionError("generic.message", "Changes may take several several minutes to appear"));
						errors.add("showIndexMessagingLink", new ActionError("generic.message", ""));
						saveErrors(req, errors);
					}
				}
				raf.setSets(rm.getSetInfos());
			}

			Enumeration params = req.getParameterNames();

			// Set the appropriate forwarding:
			//String adminEmail = req.getParameter("adminEmail");
			String paramName = null;
			String[] paramValues = null;
			while (params.hasMoreElements()) {
				paramName = (String) params.nextElement();
				paramValues = req.getParameterValues(paramName);

				if (paramName.startsWith("edit") || paramName.startsWith("add")) {
					if (paramValues[0].startsWith("Edit") || paramValues[0].startsWith("Add")) {
						prtln("case 1");
						return mapping.findForward("edit.repository.settings");
					}
				}
			}

			// Default forwarding:
			saveErrors(req, errors);
			return mapping.findForward("display.repository.settings");
		} catch (NullPointerException e) {
			prtln("RepositoryAminAction caught exception.");
			e.printStackTrace();
			return mapping.findForward("display.repository.settings");
		} catch (Throwable e) {
			prtln("RepositoryAminAction caught exception: " + e);
			return mapping.findForward("display.repository.settings");
		}
	}



	/* Checks to see if the given set is valid. */
	private final boolean isValidSet(String setName,
	                                 String setSpec,
	                                 String setDescription,
	                                 RepositoryAdminForm raf,
	                                 HttpServletRequest req,
	                                 boolean checkSetSpec,
	                                 RepositoryManager rm) {
		ActionErrors errors = new ActionErrors();

		if (checkSetSpec) {
			if (setSpec.indexOf(' ') >= 0 || setSpec.indexOf(':') >= 0 || setSpec.length() == 0) {
				errors.add("currentSetSpec", new ActionError("errors.setSpecSyntax"));
			}
			else if (rm.isSetConfigured(setSpec)) {
				errors.add("currentSetSpec", new ActionError("errors.setSpecExists"));
			}
		}
		if (setName.length() == 0) {
			errors.add("currentSetName", new ActionError("errors.setName"));
		}

		if (setDescription != null && setDescription.length() > 0) {
			String validationReport = validateXML(setDescription);
			if (validationReport != null) {
				raf.setXmlError(validationReport);
				return false;
			}
		}

		/*
		 *  if (set.getFormat().indexOf(' ') >= 0 || set.getFormat().length() == 0)
		 *  errors.add("currentSetFormat", new ActionError("errors.setFormat"));
		 *  File f = new File(set.getDirectory());
		 *  prtln("directory: " + f.getAbsolutePath());
		 *  if (!f.isDirectory())
		 *  errors.add("currentSetDirectory", new ActionError("errors.setDirectory"));
		 */
		if (errors.isEmpty()) {
			prtln("\n\n\nsetIsValid() returning no errors...\n\n\n");
			return true;
		}
		else {
			prtln("\n\n\nsetIsValid() had errors...\n\n\n");
			saveErrors(req, errors);
			return false;
		}
	}



	private ActionForward doAddNewMetadataFileDir(ActionErrors errors,
	                                              ActionMapping mapping,
	                                              RepositoryManager rm,
	                                              HttpServletRequest req,
	                                              RepositoryAdminForm raf,
	                                              MetadataDirectoryInfoForm addMetadataDirsForm) {

		prtln("name is: " + addMetadataDirsForm.getDirPath());

		String dirPath = (String) addMetadataDirsForm.getDirPath();
		dirPath = dirPath.trim();
		String dirMetadataFormat = (String) addMetadataDirsForm.getDirMetadataFormat();
		String dirNickname = (String) addMetadataDirsForm.getDirNickname();
		String metadataNamespace = (String) addMetadataDirsForm.getMetadataNamespace();
		String metadataSchema = (String) addMetadataDirsForm.getMetadataSchema();
		
		if(dirPath == null || dirPath.length() == 0)
			errors.add("error", new ActionError("generic.error", "The directory was not specified"));
		
		if(errors.size() == 0) {		
			prtln("Adding metadata file dir: " + dirNickname + " " + dirMetadataFormat + " " + dirPath + " " + metadataNamespace + " " + metadataSchema);	
			try {
	
				// Make an arbitray set spec to use as a placeholder
				String setSpec = Long.toString(System.currentTimeMillis());
	
				SetInfo newSet = new SetInfo(dirNickname,
					setSpec,
					"",
					"true",
					dirPath,
					dirMetadataFormat,
					setSpec);
	
				// Add the set
				rm.addSetInfo(newSet);
	
				// Put the sets in the bean for display...
				raf.setSets(rm.getSetInfos());
	
				// Set the metadata schema and namespace info
				setNamespaceAndSchemaDefs(rm, dirMetadataFormat, metadataNamespace, metadataSchema);
	
				// Begin the iindexer
				rm.indexCollection(setSpec, new SimpleFileIndexingObserver("Indexer for '" + dirNickname + "'", "Starting indexing"), true);
				errors.add("showIndexMessagingLink", new ActionError("generic.message", ""));
				errors.add("message", new ActionError("generic.message", "Directory '" + newSet.getName() + "' was added and is being indexed"));
			} catch (Throwable t) {
				errors.add("error", new ActionError("generic.error", "There was an error adding the directory: " + t.getMessage()));
	
				if (t instanceof NullPointerException)
					t.printStackTrace();
			}
		}
		saveErrors(req, errors);
		return mapping.findForward("display.repository.settings");
	}



	private ActionForward doEditMetadataFileDir(ActionErrors errors,
	                                            ActionMapping mapping,
	                                            RepositoryManager rm,
	                                            HttpServletRequest req,
	                                            RepositoryAdminForm raf,
	                                            MetadataDirectoryInfoForm editMetadataDirsForm) {


		prtln("name is: " + editMetadataDirsForm.getDirPath());

		String dirPath = (String) editMetadataDirsForm.getDirPath();
		dirPath = dirPath.trim();
		String dirMetadataFormat = (String) editMetadataDirsForm.getDirMetadataFormat();
		String dirNickname = (String) editMetadataDirsForm.getDirNickname();
		String metadataNamespace = (String) editMetadataDirsForm.getMetadataNamespace();
		String metadataSchema = (String) editMetadataDirsForm.getMetadataSchema();

		String data = dirNickname + " " + dirPath + " " + metadataNamespace + " " + metadataSchema;
		prtln("Editing metadata file dir: " + data);

		try {

			// Get the previous setSpec
			String setSpec = req.getParameter("edit");

			SetInfo prevSetInfo = rm.getSetInfo(setSpec);
			if (prevSetInfo == null) {
				errors.add("error", new ActionError("generic.error", "Unable to find previous settings to edit."));
				saveErrors(req, errors);
				return mapping.findForward("display.repository.settings");
			}

			if (dirMetadataFormat == null || dirMetadataFormat.trim().length() == 0) {
				errors.add("error", new ActionError("generic.error", "No XML format was specfied"));
				saveErrors(req, errors);
				return mapping.findForward("display.repository.settings");
			}

			if (dirPath == null || dirPath.trim().length() == 0) {
				errors.add("error", new ActionError("generic.error", "No directory was specfied"));
				saveErrors(req, errors);
				return mapping.findForward("display.repository.settings");
			}
			
			// Remove the old records from the index if the dir or format has changed:
			/* if(!prevSetInfo.getDirectory().equals(dirPath) || !prevSetInfo.getFormat().equals(dirMetadataFormat)){
				boolean wasRemoved = rm.removeSetBySetSpec(prevSetInfo.getSetSpec());
				if (wasRemoved)
					errors.add("message", new ActionError("generic.message", setSpec + " was removed from the respository"));
				else
					errors.add("message", new ActionError("generic.message", setSpec + " was not configured and so was not removed"));
			} */
			
			SetInfo updatedSetInfo = new SetInfo(dirNickname,
				setSpec,
				prevSetInfo.getDescription(),
				prevSetInfo.getEnabled(),
				dirPath,
				dirMetadataFormat,
				setSpec);

			// Replace the setInfo
			rm.replaceSetInfo(setSpec, updatedSetInfo);

			// Put the sets in the bean for display...
			raf.setSets(rm.getSetInfos());

			// Set the metadata schema and namespace info
			setNamespaceAndSchemaDefs(rm, dirMetadataFormat, metadataNamespace, metadataSchema);

			// Begin the indexer
			//rm.indexCollection(setSpec,new SimpleFileIndexingObserver("Indexer for '" + dirNickname + "'", "Starting indexing"),true);
			
			errors.add("showIndexMessagingLink", new ActionError("generic.message", ""));
			errors.add("message", new ActionError("generic.message", "Information and configuration was updated for directory '" + updatedSetInfo.getName() + "'"));
		} catch (Throwable t) {
			errors.add("error", new ActionError("generic.error", "There was an error adding the directory: " + t.getMessage()));

			if (t instanceof NullPointerException)
				t.printStackTrace();
		}
		saveErrors(req, errors);
		return mapping.findForward("display.repository.settings");
	}



	private ActionForward doAddSetDefinition(ActionErrors errors,
	                                         ActionMapping mapping,
	                                         RepositoryManager rm,
	                                         HttpServletRequest req,
	                                         RepositoryAdminForm raf,
	                                         SetDefinitionsForm setDefinitionsForm) {

		prtln("doAddSetDefinition()");
	
		// Save the new set definition
		try {		
			rm.setOAISetSpecDefinition(setDefinitionsForm);
		} catch (Throwable t) {
			errors.add("error", new ActionError("generic.error", "Error saving sets: " + t.getMessage()));
			if (t instanceof NullPointerException)
				t.printStackTrace();
			saveErrors(req, errors);
			return mapping.findForward("display.repository.settings");
		}

		// Remove the previous set difinition, if no errors so far...
		String prevSetSpec = req.getParameter("edit");
		if (prevSetSpec != null && !prevSetSpec.equals(setDefinitionsForm.getSetSpec())) {
			prtln("deleting previeous set definition for '" + prevSetSpec + "'");
			try {
				rm.removeOAISetSpecDefinition(prevSetSpec);
			} catch (Throwable t) {
				errors.add("error", new ActionError("generic.error", "An error occured while trying to delete set '" + setDefinitionsForm.getSetName() + "'. Message: " + t));
				saveErrors(req, errors);
				return mapping.findForward("display.repository.settings");
			}
		}
		if (prevSetSpec != null)
			prtln("updated set definition for '" + prevSetSpec + "'");

		errors.add("message", new ActionError("generic.message", "The set '" + setDefinitionsForm.getSetName() + "' was added successfully."));
		saveErrors(req, errors);
		return mapping.findForward("display.repository.settings");
	}

	
	private ActionForward doSetRepositoryInfo(ActionErrors errors,
	                                         ActionMapping mapping,
	                                         RepositoryManager rm,
	                                         HttpServletRequest req,
	                                         RepositoryAdminForm raf,
	                                         RepositoryInfoForm repositoryInfoForm) {

		prtln("doSetRepositoryInfo()");

		// Save the new set definition
		try {
			rm.setRepositoryName(repositoryInfoForm.getRepositoryName());
			rm.setRepositoryIdentifier(repositoryInfoForm.getNamespaceIdentifier());
			
			// For now, only one description is used (but could have more later):
			rm.removeDescription(0);
			rm.addDescription(repositoryInfoForm.getRepositoryDescription());
			
			// For now, only one e-mail is used (but could have more later):
			rm.removeAdminEmail(0);
			rm.addAdminEmail(repositoryInfoForm.getAdminEmail());
		} catch (Throwable t) {
			errors.add("error", new ActionError("generic.error", "Error saving repository information: " + t.getMessage()));
			if (t instanceof NullPointerException)
				t.printStackTrace();
			saveErrors(req, errors);
			return mapping.findForward("display.repository.settings");
		}

		errors.add("message", new ActionError("generic.message", "The repository information has been updated."));
		saveErrors(req, errors);
		return mapping.findForward("display.repository.settings");
	}	
	

	/**
	 *  Handle updating the namespaces and schemas defined for a given XML Format.
	 *
	 * @param  rm         The new namespaceAndSchemaDefs value
	 * @param  xmlFormat  The new namespaceAndSchemaDefs value
	 * @param  namespace  The new namespaceAndSchemaDefs value
	 * @param  schema     The new namespaceAndSchemaDefs value
	 */
	private void setNamespaceAndSchemaDefs(RepositoryManager rm, String xmlFormat, String namespace, String schema) {
		if (rm == null || xmlFormat == null)
			return;

		if (namespace != null) {
			if (namespace.trim().length() == 0)
				rm.removeMetadataNamespace(xmlFormat);
			else
				rm.setMetadataNamespace(xmlFormat, namespace);
		}

		if (schema != null) {
			if (namespace.trim().length() == 0)
				rm.removeMetadataSchemaURL(xmlFormat);
			else
				rm.setMetadataSchemaURL(xmlFormat, schema);
		}
	}


	/**
	 *  Validate an XML string. The string must contain a schema location that is defined in the root element by
	 *  the attribute <code>schemaLocation,</code> which is case-sensitive.
	 *
	 * @param  s  The string to validate
	 * @return    Null iff no validation errors were found, else a String containing an appropriate error
	 *      message.
	 */
	private final String validateXML(String s) {
		if (s == null) {
			return null;
		}

		if (s.indexOf("schemaLocation") == -1) {
			return
				"SCHEMA NOT PRESENT: The schema location must be defined in the " +
				"root element by the schemaLocation attribute, which is case-sensitive.";
		}
		else {
			return XMLValidator.validateString(s);
		}
	}


	/**
	 *  Gets the index associated with a request parameter of the form myParameter[i] where the collection index
	 *  is indicated in brackets.
	 *
	 * @param  paramName  The request parameter String
	 * @return            The index value
	 */
	private final int getIndex(String paramName) {
		return getIntValue(paramName.substring(paramName.indexOf("[") + 1, paramName.indexOf("]")));
	}


	/**
	 *  Gets the intValue attribute of the RepositoryAdminAction object
	 *
	 * @param  isInt  Description of the Parameter
	 * @return        The intValue value
	 */
	private final int getIntValue(String isInt) {
		try {
			return Integer.parseInt(isInt);
		} catch (Throwable e) {
			return -1;
		}
	}



	// ---------------------- Debug info --------------------

	/**
	 *  Return a string for the current time and date, sutiable for display in log files and output to standout:
	 *
	 * @return    The dateStamp value
	 */
	protected final static String getDateStamp() {
		return
			new SimpleDateFormat("MMM d, yyyy h:mm:ss a zzz").format(new Date());
	}


	/**
	 *  Output a line of text to error out, with datestamp.
	 *
	 * @param  s  The text that will be output to error out.
	 */
	private final void prtlnErr(String s) {
		System.err.println(getDateStamp() + " " + s);
	}


	/**
	 *  Output a line of text to standard out, with datestamp, if debug is set to true.
	 *
	 * @param  s  The String that will be output.
	 */
	private final void prtln(String s) {
		if (debug) {
			System.out.println(getDateStamp() + " " + s);
		}
	}


	/**
	 *  Sets the debug attribute of the object
	 *
	 * @param  db  The new debug value
	 */
	public static void setDebug(boolean db) {
		debug = db;
	}
}

