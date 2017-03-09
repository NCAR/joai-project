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
import org.dlese.dpc.schemedit.repository.RepositoryIndexingObserver;
import org.dlese.dpc.schemedit.dcs.*;
import org.dlese.dpc.schemedit.threadedservices.AutoExportTask;

import org.dlese.dpc.schemedit.config.CollectionRegistry;
import org.dlese.dpc.schemedit.action.form.*;
import org.dlese.dpc.repository.*;
import org.dlese.dpc.xml.*;
import org.dlese.dpc.vocab.MetadataVocabServlet;
import org.dlese.dpc.vocab.MetadataVocab;
import org.dlese.dpc.index.*;

import java.util.*;
import java.text.*;
import javax.servlet.ServletContext;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServletResponse;
import org.apache.struts.action.Action;
import org.apache.struts.action.ActionError;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionServlet;
import org.apache.struts.util.MessageResources;

/**
 *  Implementation of <strong>Action</strong> that handles administration of a
 *  metadata repository.
 *
 * @author    John Weatherley, ostwald<p>
 *
 *
 */
public final class DCSAdminAction extends DCSAction {
	private static boolean debug = true;

	// --------------------------------------------------------- Public Methods

	/**
	 *  Process the specified HTTP request, and create the corresponding HTTP
	 *  response (or forward to another web component that will create it). Return
	 *  an <code>ActionForward</code> instance describing where and how control
	 *  should be forwarded, or <code>null</code> if the response has already been
	 *  completed.
	 *
	 * @param  mapping        The ActionMapping used to select this instance
	 * @param  response       The HTTP response we are creating
	 * @param  form           The ActionForm for the given page
	 * @param  req            The HTTP request.
	 * @return                The ActionForward instance describing where and how
	 *      control should be forwarded
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
		ActionErrors errors = initializeFromContext(mapping, req);
		if (!errors.isEmpty()) {
			saveErrors(req, errors);
			return (mapping.findForward("error.page"));
		}
		Locale locale = getLocale(req);
		DCSAdminForm daf = (DCSAdminForm) form;
		ServletContext servletContext = getServlet().getServletContext();

		MetadataVocab vocab =
			(MetadataVocab) servletContext.getAttribute("MetadataVocab");

		String enableNewSets = (String) servletContext.getAttribute("enableNewSets");

		// SchemEditUtils.showRequestParameters(req);

		// Set up the bean with data:
		daf.setOaiIdPfx(repositoryManager.getOaiIdPrefix());
		daf.setNumIdentifiersResults(repositoryManager.getNumIdentifiersResults());
		daf.setNumRecordsResults(repositoryManager.getNumRecordsResults());
		daf.setRepositoryIdentifier(repositoryManager.getRepositoryIdentifier());
		daf.setBaseURL(repositoryManager.getProviderBaseUrl(req));
		daf.setUpdateFrequency(repositoryManager.getUpdateFrequency());
		daf.setIndexingStartTime(repositoryManager.getIndexingStartTime());
		daf.setCollectionRecordsLocation(repositoryManager.getCollectionRecordsLocation());
		daf.setMetadataRecordsLocation(repositoryManager.getMetadataRecordsLocation());

		daf.setSets(repositoryService.getSetInfos());

		daf.setDrcBoostFactor(repositoryManager.getDrcBoostFactor());
		daf.setMultiDocBoostFactor(repositoryManager.getMultiDocBoostFactor());
		daf.setTrustedWsIps(repositoryManager.getTrustedWsIps());

		SimpleLuceneIndex index = repositoryManager.getIndex();

		daf.setNumIndexingErrors(repositoryManager.getNumIndexingErrors());
		if (index != null) {
			ResultDocList indexingErrors = index.searchDocs("error:true");
			if (indexingErrors == null) {
				daf.setNumIndexingErrors(0);
			}
			else {
				daf.setNumIndexingErrors(indexingErrors.size());
			}
			indexingErrors = null;
		}

		try {
			// Handle simple admin page requests that direct to individual settings pages
			if (req.getParameter("page") != null) {
				String pageVal = req.getParameter("page");

				if (pageVal.equalsIgnoreCase("collections")) {
					String sortSetsBy = req.getParameter("sortSetsBy");
					if (sortSetsBy != null) {
						if (sortSetsBy.equals("collection"))
							Collections.sort(daf.getSets());
						else
							Collections.sort(daf.getSets(), DcsSetInfo.getComparator(sortSetsBy));
					}
					return mapping.findForward("collection.settings");
				}

				if (pageVal.equalsIgnoreCase("editors")) {

					Map frameworks = new HashMap();

					// each framework has an editor that can be configured
					for (Iterator i = frameworkRegistry.getAllFormats().iterator(); i.hasNext(); ) {
						String xmlFormat = (String) i.next();
						MetaDataFramework framework = this.getMetaDataFramework(xmlFormat);
						if (framework != null)
							frameworks.put(xmlFormat, framework);
					}

					daf.setFrameworks(frameworks);
					daf.setUnloadedFrameworks(frameworkRegistry.getUnloadedFrameworks());
					return mapping.findForward("editors.settings");
				}

				if (pageVal.equalsIgnoreCase("services")) {
					return mapping.findForward("services.settings");
				}

				if (pageVal.equalsIgnoreCase("index")) {
					return mapping.findForward("index.settings");
				}

				if (pageVal.equalsIgnoreCase("access")) {
					return mapping.findForward("access.settings");
				}

				if (pageVal.equalsIgnoreCase("config")) {
					return mapping.findForward("config.settings");
				}

				errors.add("message", new ActionError("generic.message", "unrecognized page: " + pageVal));
				saveErrors(req, errors);
				return mapping.findForward("edit.repository.settings");
			}

			// Handle software admin actions:
			if (req.getParameter("command") != null) {

				String paramVal = req.getParameter("command");
				prtln("Running command " + paramVal);


				if (paramVal.equalsIgnoreCase("debug")) {
					return handleDebug(mapping, form, req, response);
				}

				// Export all configured collections
				if (paramVal.equalsIgnoreCase("exportAll")) {
					AutoExportTask exportTask = new AutoExportTask(servletContext);
					exportTask.run();
					errors.add ("message", new ActionError("generic.message", "Export is underway as a background process"));
					saveErrors(req, errors);
					return mapping.findForward("config.settings");
				}
				
				// Re-initialize the IDManager - not currently used ...
				if (paramVal.equalsIgnoreCase("reInitIDManager")) {
					collectionRegistry.initializeIDGenerators(index);
					prtln("after reinit ...");
					List cols = index.getTerms("collection");
					prtln(cols.size() + " items found");
					errors.add("message", new ActionError("generic.message", "IDGenerators have been re-initialized"));
					for (Iterator i = collectionRegistry.getIds().iterator(); i.hasNext(); ) {
						String key = (String) i.next();
						String lastId = collectionRegistry.getIDGenerator(key).getLastID();
						errors.add("message", new ActionError("dcsadmin.IDGenerator.status", key, lastId));
					}
					saveErrors(req, errors);
					return mapping.findForward("index.settings");
				}

				// Update the index:
				if (paramVal.equals("Reindex all files")) {
					repositoryManager.indexFiles(new RepositoryIndexingObserver(collectionRegistry, repositoryManager), true);
					errors.add("message", new ActionError("generic.message", "Index is being updated"));
					errors.add("showIndexMessagingLink", new ActionError("generic.message", ""));
					saveErrors(req, errors);
					return mapping.findForward("index.settings");
				}

				// Delete and rebuild the index:
				if (paramVal.equals("rebuildIndex")) {
					prtln ("rebuilding");
					// flush dcs_data_records to disk before rebuilding
					dcsDataManager.flushCache();
					
					repositoryManager.deleteIndex();
					repositoryManager.loadCollectionRecords(true);
					repositoryManager.indexFiles(new RepositoryIndexingObserver(collectionRegistry, repositoryManager), true);
					errors.add("message", new ActionError("generic.message", "Index has been deleted and is now rebuilding."));
					errors.add("showIndexMessagingLink", new ActionError("generic.message", ""));
					saveErrors(req, errors);
					return mapping.findForward("index.settings");
				}
				// Stop indexing:
				if (paramVal.equals("stopIndexing")) {
					repositoryManager.stopIndexing();
					errors.add("message", new ActionError("generic.message", "Indexing has stopped."));
					errors.add("showIndexMessagingLink", new ActionError("generic.message", ""));
					saveErrors(req, errors);
					return mapping.findForward("index.settings");
				}
				// Show indexing status messages:
				if (paramVal.equals("showIndexingMessages")) {
					List indexingMessages = repositoryManager.getIndexingMessages();
/* 					int maxMessagesToShow = 10;
					int totalMessages = indexingMessages.size();
					int lastMessage = (totalMessages >= maxMessagesToShow ?
						totalMessages - maxMessagesToShow : 0);

					for (int i = totalMessages - 1; i >= lastMessage; i--) {
						errors.add("message", new ActionError("generic.message", (String) indexingMessages.get(i)));
					} */
					
					for (int i = indexingMessages.size() - 1; i >= 0; i--) {
						errors.add("message", new ActionError("generic.message", (String) indexingMessages.get(i)));
					}
					
					errors.add("showIndexMessagingLink", new ActionError("generic.message", ""));
					saveErrors(req, errors);
					return mapping.findForward("index.settings");
				}

				// Update the trusted web service IPs
				if (paramVal.equals("setTrustedIps")) {
					String trustedIps = req.getParameter("trustedWsIps");
					if (trustedIps != null) {
						repositoryManager.setTrustedWsIps(trustedIps);
						daf.setTrustedWsIps(repositoryManager.getTrustedWsIps());
						errors.add("message", new ActionError("generic.message", "The trusted web service IPs have been updated."));
						saveErrors(req, errors);
					}
					return mapping.findForward("services.settings");
				}
				// Reload vacabs:
				if (paramVal.equals("Reload vocabulary")) {
					if (vocab != null) {

						MetadataVocabServlet mvs =
							(MetadataVocabServlet) servlet.getServletContext().getAttribute("MetadataVocabServlet");
						try {
							if (mvs == null)
								throw new Exception("MetadataVocabServlet not found in servlet context");
							mvs.loadVocabs();
						} catch (Exception e) {
							String errorMsg = "Vocab reload error: " + e.getMessage();
							errors.add("error",
								new ActionError("generic.error", "Vocabs not found in servlet context"));
						}
						errors.add("message",
							new ActionError("generic.message", "Vocabs reloaded"));
					}
					else {
						errors.add("error", new ActionError("generic.error", "Vocabs not found in servlet context"));
					}
					saveErrors(req, errors);
					return mapping.findForward("index.settings");
				}
				// Update the boost factors:
				if (paramVal.equals("ubf")) {
					double titleBoostFactor = 1;
					double drcBoostFactor = 1;
					double stemmingBoostFactor = 1;
					double multiDocBoostFactor = 1;

					if (req.getParameter("titleBoostFactor") != null) {
						try {
							titleBoostFactor = Double.parseDouble(req.getParameter("titleBoostFactor"));
							if (!(titleBoostFactor >= 0.0)) {
								errors.add("error", new ActionError("generic.error", "Title boost factor must be a number greater than or equal to zero."));
							}
						} catch (Throwable e) {
							errors.add("error", new ActionError("generic.error", "Incorrect value " + e.getMessage() + ". Title boost factor must be a number greater than or equal to zero."));
						}
					}

					if (req.getParameter("drcBoostFactor") != null) {
						try {
							drcBoostFactor = Double.parseDouble(req.getParameter("drcBoostFactor"));
							if (!(drcBoostFactor >= 0.0)) {
								errors.add("error", new ActionError("generic.error", "DRC boost factor must be a number greater than or equal to zero."));
							}
							else {
								repositoryManager.setDrcBoostFactor(drcBoostFactor);
							}
						} catch (Throwable e) {
							errors.add("error", new ActionError("generic.error", "Incorrect value " + e.getMessage() + ". DRC boost factor must be a number greater than or equal to zero."));
						}
					}

					if (req.getParameter("multiDocBoostFactor") != null) {
						try {
							multiDocBoostFactor = Double.parseDouble(req.getParameter("multiDocBoostFactor"));
							if (!(multiDocBoostFactor >= 0.0)) {
								errors.add("error", new ActionError("generic.error", "Multi-record boost factor must be a number greater than or equal to zero."));
							}
							else {
								repositoryManager.setMultiDocBoostFactor(multiDocBoostFactor);
							}
						} catch (Throwable e) {
							errors.add("error", new ActionError("generic.error", "Incorrect value " + e.getMessage() + ".  Multi-record boost factor must be a number greater than or equal to zero."));
						}
					}

					if (req.getParameter("stemmingBoostFactor") != null) {
						try {
							stemmingBoostFactor = Double.parseDouble(req.getParameter("stemmingBoostFactor"));
							if (!(stemmingBoostFactor >= 0.0)) {
								errors.add("error", new ActionError("generic.error", "Stemming boost factor must be a number greater than or equal to zero."));
							}
						} catch (Throwable e) {
							errors.add("error", new ActionError("generic.error", "Incorrect value " + e.getMessage() + ". Stemming boost factor must be a number greater than or equal to zero."));
						}
					}

					if (req.getParameter("resetDefaultBoosting") != null) {
						try {
							repositoryManager.resetBoostingFactorDefaults();
						} catch (Throwable e) {
							errors.add("error", new ActionError("generic.error", "Unable to update boosting values to defaults: " + e.getMessage()));
						}
					}

					if (errors.isEmpty()) {
						daf.setDrcBoostFactor(repositoryManager.getDrcBoostFactor());
						daf.setMultiDocBoostFactor(repositoryManager.getMultiDocBoostFactor());
						errors.add("message", new ActionError("generic.message", "Search boosting factors have been updated."));
					}

					saveErrors(req, errors);
					return mapping.findForward("index.settings");
				}
			}

			// ------ Save data into the repositoryManager ----------

			if (req.getParameter("setStemmingEnabled") != null) {
				if (req.getParameter("setStemmingEnabled").equalsIgnoreCase("true")) {
					daf.setStemmingEnabled("true");
					errors.add("message", new ActionError("generic.message", "Stemming has been enabled."));
				}
				else {
					daf.setStemmingEnabled("false");
					errors.add("message", new ActionError("generic.message", "Stemming has been disabled."));
				}
				saveErrors(req, errors);
				return mapping.findForward("index.settings");
			}

			// Default forwarding:
			saveErrors(req, errors);
			return mapping.findForward("collection.settings");
		} catch (NullPointerException e) {
			prtln("DCSAdminAction caught exception.");
			e.printStackTrace();
			return mapping.findForward("collection.settings");
		} catch (Throwable e) {
			prtln("DCSAdminAction caught exception: " + e);
			e.printStackTrace();
			return mapping.findForward("collection.settings");
		}
	}


	/**
	 *  Checks to see if the given set is valid.
	 *
	 * @param  setName            DESCRIPTION
	 * @param  setSpec            DESCRIPTION
	 * @param  setDescription     DESCRIPTION
	 * @param  daf                DESCRIPTION
	 * @param  req                DESCRIPTION
	 * @param  checkSetSpec       DESCRIPTION
	 * @param  repositoryManager  DESCRIPTION
	 * @return                    The validSet value
	 */
	private final boolean isValidSet(String setName,
	                                 String setSpec,
	                                 String setDescription,
	                                 DCSAdminForm daf,
	                                 HttpServletRequest req,
	                                 boolean checkSetSpec,
	                                 RepositoryManager repositoryManager) {
		ActionErrors errors = new ActionErrors();

		if (checkSetSpec) {
			if (setSpec.indexOf(' ') >= 0 || setSpec.indexOf(':') >= 0 || setSpec.length() == 0) {
				errors.add("currentSetSpec", new ActionError("errors.setSpecSyntax"));
			}
			else if (repositoryManager.isSetConfigured(setSpec)) {
				errors.add("currentSetSpec", new ActionError("errors.setSpecExists"));
			}
		}
		if (setName.length() == 0) {
			errors.add("currentSetName", new ActionError("errors.setName"));
		}

		if (setDescription != null && setDescription.length() > 0) {
			String validationReport = validateXML(setDescription);
			if (validationReport != null) {
				daf.setXmlError(validationReport);
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


	/**
	 *  Hook for executing debug actions. There must be a "command" param equals
	 *  "debug" to flush dcs records, "flush=true"
	 *
	 * @param  mapping        Description of the Parameter
	 * @param  form           Description of the Parameter
	 * @param  req            Description of the Parameter
	 * @param  response       Description of the Parameter
	 * @return                Description of the Return Value
	 * @exception  Exception  Description of the Exception
	 */
	private ActionForward handleDebug(ActionMapping mapping,
	                                  ActionForm form,
	                                  HttpServletRequest req,
	                                  HttpServletResponse response)
		 throws Exception {
		prtln("debug");
		ActionErrors errors = new ActionErrors();
		DCSAdminForm daf = (DCSAdminForm) form;
		String dcsId = req.getParameter("dcsId");
		if (dcsId != null) {
			daf.setDcsId(dcsId);
			DcsDataRecord dcsDataRecord = dcsDataManager.getDcsDataRecord(dcsId, repositoryManager);
			if (dcsDataRecord != null) {
				daf.setDcsXml(Dom4jUtils.prettyPrint(dcsDataRecord.getDocument()));
			}
			else {
				errors.add("error", new ActionError("generic.error", "record not found for " + dcsId));
			}
		}
		String flush = req.getParameter("flush");
		if (flush != null && flush.equals("true")) {
			dcsDataManager.flushCache();
			errors.add("message", new ActionError("generic.message", "dcs cache flushed to disk"));
		}
		String ndrVerbose = req.getParameter("ndrVerbose");
		if (ndrVerbose != null) {
			if (ndrVerbose.equals("true")) {
				org.dlese.dpc.ndr.request.SimpleNdrRequest.setDebug(true);
				org.dlese.dpc.ndr.request.SimpleNdrRequest.setVerbose(true);
				errors.add("message", new ActionError("generic.message", "NDR is set to verbose"));
			}
			else {
				org.dlese.dpc.ndr.request.SimpleNdrRequest.setDebug(false);
				org.dlese.dpc.ndr.request.SimpleNdrRequest.setVerbose(false);
				errors.add("message", new ActionError("generic.message", "NDR is now quiet, mon"));
			}
		}

		saveErrors(req, errors);
		return mapping.findForward("debug");
	}


	/**
	 *  Validate an XML string. The string must contain a schema location that is
	 *  defined in the root element by the attribute <code>schemaLocation,</code>
	 *  which is case-sensitive.
	 *
	 * @param  s  The string to validate
	 * @return    Null iff no validation errors were found, else a String
	 *      containing an appropriate error message.
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
	 *  Gets the index associated with a request parameter of the form
	 *  myParameter[i] where the collection index is indicated in brackets.
	 *
	 * @param  paramName  The request parameter String
	 * @return            The index value
	 */
	private final int getIndex(String paramName) {
		return getIntValue(paramName.substring(paramName.indexOf("[") + 1, paramName.indexOf("]")));
	}


	/**
	 *  Gets the intValue attribute of the DCSAdminAction object
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
	 *  Return a string for the current time and date, sutiable for display in log
	 *  files and output to standout:
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
	 *  Output a line of text to standard out, with datestamp, if debug is set to
	 *  true.
	 *
	 * @param  s  The String that will be output.
	 */
	private final void prtln(String s) {
		if (debug) {
			System.out.println(getDateStamp() + " DCSAdminAction: " + s);
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

