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
package org.dlese.dpc.oai.harvester.action;

import org.dlese.dpc.oai.harvester.*;
import org.dlese.dpc.oai.harvester.structs.*;
import org.dlese.dpc.oai.harvester.action.form.*;
import org.dlese.dpc.xml.XMLValidator;
import org.dlese.dpc.vocab.*;
import org.dlese.dpc.util.*;
import org.dlese.dpc.vocab.MetadataVocab;
import org.dlese.dpc.oai.*;
import org.dlese.dpc.oai.harvester.*;
import org.dlese.dpc.webapps.tools.*;
import org.dlese.dpc.datamgr.*;

import java.util.*;
import java.lang.*;
import java.io.*;
import java.text.*;
import java.util.Hashtable;
import java.util.Locale;
import org.apache.struts.action.Action;
import org.apache.struts.action.ActionError;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionServlet;
import org.apache.struts.util.MessageResources;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;
import javax.servlet.*;
import javax.servlet.http.*;

/**
 *  Action controller that handles administration of the Harvester.
 *
 * @author     John Weatherley
 */
public final class HarvesterAdminAction extends Action {
	private static boolean debug = false;
	private static Hashtable mySessions = new Hashtable();


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

		ServletContext servletContext = getServlet().getServletContext();

		// Ensure harvests only occur once even if link is clicked multiple times.
		Long mySes = new Long(Utils.getUniqueID());
		mySessions.put(mySes, mySes);

		// Extract attributes we will need
		Locale locale = getLocale(req);
		HarvesterAdminForm haf = (HarvesterAdminForm) form;
		ActionErrors errors = new ActionErrors();

		SimpleDataStore ds =
				(SimpleDataStore) servletContext.getAttribute("harvesterSettings");

		ScheduledHarvestManager shm =
				(ScheduledHarvestManager) servletContext.getAttribute("scheduledHarvestManager");

		// Set default values for stuff that has not yet been initialized
		setDefaults(ds, servletContext);

		// Set up the bean with data:
		haf.setHarvesterStatus((String) ds.get(Keys.HARVESTER_STATUS));
		haf.setValidateRecords((Boolean) ds.get(Keys.VALIDATE_RECORDS));
		haf.setHarvestedDataDir(((File) ds.get(Keys.HARVESTED_DATA_DIR)).getAbsolutePath());
		haf.setShHarvestedDataDir(shm.getHarvestDir().getAbsolutePath());
		haf.setScheduledHarvests(shm.getScheduledHarvests());
		haf.setMySes(mySes.toString());

		if (haf.getCommonDirs() == null) {
			Hashtable shhash = shm.getScheduledHarvests();
			if (shhash != null) {
				Enumeration enumm = shhash.keys();

				int num = haf.getMAX_DIRS();
				String[] dirs;
				dirs = new String[num];
				for (int f = 0; f < num; f++) {
					dirs[f] = "";
				}
				int i = 0;
				while (enumm.hasMoreElements()) {
					Long key = (Long) enumm.nextElement();
					ScheduledHarvest sh = shm.getScheduledHarvest(key);
					String dir = (sh.getHarvestDir()).toString();
					boolean def = sh.getDefaultDir();
					if ((def == false) && (dir != null)) {
						/*
						 *  make sure no duplicates
						 */
						boolean duplicate = false;
						for (int f = 0; f < num; f++) {
							if ((dirs[f]).equalsIgnoreCase(dir)) {
								duplicate = true;
								break;
							}
						}
						if ((duplicate == false) && (i < num)) {
							dirs[i] = dir;
							i++;
						}
					}
				}

				/*
				 *  for (int f = 0; f < i; f++) {
				 *  prtln(dirs[f]);
				 *  }
				 */
				haf.setCommonDirs(dirs);

			}
		}

		try {

			// Handle harvester admin actions:
			if (req.getParameter("command") != null) {
				prtln("command");
				String paramVal = req.getParameter("command");
				prtln("Running command " + paramVal);

			}

			// Harvest an individual collection
			if (req.getParameter("doHarvest") != null) {

				// Ensure harvests only occur once even if link is clicked multiple times.
				Long ses = new Long(req.getParameter("mySes"));
				if (mySessions.containsKey(ses)) {
					mySessions.remove(ses);
				} else {
					return mapping.findForward("display.harvester.settings");
				}

				String baseURL = req.getParameter("baseURL").trim();
				String metadataPrefix = req.getParameter("metadataPrefix").trim();
				String setSpec = req.getParameter("setSpec").trim();
				String from = req.getParameter("from").trim();
				String until = req.getParameter("until").trim();

				if (metadataPrefix.length() == 0) {
					errors.add("metadataPrefix", new ActionError("errors.metadataPrefix"));
				}

				if (baseURL.length() == 0) {
					errors.add("baseURL", new ActionError("generic.message",
							"Please provide a base URL."));
				} else if (!(baseURL.startsWith("http://") || baseURL.startsWith("https://"))) {
					errors.add("baseURL", new ActionError("generic.message",
							"The baseURL must begin with http:// or https://"));
				}

				Date fromDate = null;
				Date untilDate = null;
				if (from != null && from.length() > 0) {
					try {
						fromDate = OAIUtils.getDateFromDatestamp(from);
					} catch (ParseException e) {
						errors.add("from", new ActionError("generic.message",
								"From date must be of the form YYYY-MM-DDThh:mm:ssZ or YYYY-MM-DD. " + e.getMessage()));
					}
				}
				if (until != null && until.length() > 0) {
					try {
						untilDate = OAIUtils.getDateFromDatestamp(until);
					} catch (ParseException e) {
						errors.add("until", new ActionError("generic.message",
								"Until date must be of the form YYYY-MM-DDThh:mm:ssZ or YYYY-MM-DD. " + e.getMessage()));
					}
				}
				if (!errors.isEmpty()) {
					errors.add("validationError", new ActionError("generic.message", ""));
				}
				
				boolean doZipFiles = true;
				
				if (errors.isEmpty()) {
					ScheduledHarvest sh = new ScheduledHarvest(
							"One-time harvest",
							setSpec,
							baseURL,
							metadataPrefix,
							"0",
							"days",
							null,
							"disabled", null, true, false, true, doZipFiles, "");

					if (shm.isRunningOneTimeHarvest(sh)) {
						String set = sh.getSetSpec();
						if (set == null || set.length() == 0) {
							set = ", set (none)";
						} else {
							set = ", set " + set;
						}
						errors.add("harvestErr", new ActionError("generic.message", "A harvest of " +
								sh.getBaseURL() + set + " is already in progress."));
						saveErrors(req, errors);
						return mapping.findForward("display.harvester.settings");
					}
					
					boolean harvestAllIfNoDeletedRecord = false;
					
					shm.oneTimeHarvest(sh, fromDate, untilDate, (File) ds.get(Keys.HARVESTED_DATA_DIR), harvestAllIfNoDeletedRecord);
					haf.setLastOneTimeHarvest(sh);

					String set = sh.getSetSpec();
					if (set == null || set.length() == 0) {
						set = ", set (none)";
					} else {
						set = ", set " + set;
					}
					errors.add("runOneTimeHarvest", new ActionError("generic.message", "A harvest of " +
							sh.getBaseURL() + set + " has been started."));

				}

				saveErrors(req, errors);
				return mapping.findForward("display.harvester.settings");
			}

			if (req.getParameter("statusButton") != null) {
				String paramVal = req.getParameter("statusButton");
				if (paramVal.toLowerCase().matches(".*disable.*")) {
					haf.setHarvesterStatus("DISABLED");
					ds.put(Keys.HARVESTER_STATUS, "DISABLED");
				} else {
					haf.setHarvesterStatus("ENABLED");
					ds.put(Keys.HARVESTER_STATUS, "ENABLED");
				}
			}
			if (req.getParameter("validateRecordsRecordsButton") != null) {
				String paramVal = req.getParameter("validateRecords");
				Boolean validate = new Boolean(paramVal);
				haf.setValidateRecords(validate);
				ds.put(Keys.VALIDATE_RECORDS, validate);
			}
			if (req.getParameter("editHarvestedDataDir") != null) {
				String paramVal = req.getParameter("editHarvestedDataDir");
				if (paramVal.equals("edit")) {
					return mapping.findForward("edit.harvester.settings");
				}

				if (paramVal.equals("save")) {
					String newVal = req.getParameter("harvestedDataDir");
					File f = new File(newVal);
					ds.put(Keys.HARVESTED_DATA_DIR, f);
					haf.setHarvestedDataDir(f.getAbsolutePath());
					return mapping.findForward("display.harvester.settings");
				}
			}
			if (req.getParameter("editShHarvestedDataDir") != null) {
				String paramVal = req.getParameter("editShHarvestedDataDir");
				if (paramVal.equals("edit")) {
					return mapping.findForward("edit.harvester.settings");
				}

				if (paramVal.equals("save")) {
					String newVal = req.getParameter("shHarvestedDataDir");
					File f = new File(newVal);
					shm.setHarvestDir(f);
					haf.setShHarvestDir(f.getAbsolutePath());
					return mapping.findForward("display.harvester.settings");
				}
			}

			// Add/edit/save/delete scheduled harvest
			if (req.getParameter("scheduledHarvest") != null) {
				String paramVal = req.getParameter("scheduledHarvest");

				// Add a new scheduled harvest:
				if (paramVal.equals("add")) {
					haf.setShUid("0");
					haf.setShBaseURL("");
					haf.setShEnabledDisabled("disabled");
					haf.setShHarvestingInterval("");
					haf.setShIntervalGranularity("");
					haf.setShMetadataPrefix("");
					haf.setShRepositoryName("");
					haf.setShSetSpec("");
					haf.setAllowDupDir("dontallow");
					haf.setShDontZipFiles(true);

					File defaultHDir = (File) ds.get(Keys.HARVESTED_DATA_DIR);
					String defaultDir = defaultHDir.toString();
					haf.setDefDir(defaultDir);
					haf.setShHarvestDir("");
					haf.setShSet("dontsplit");
					haf.setShDir("default");
					return mapping.findForward("edit.harvester.settings");
				
				// Edit an existing scheduled harvest:
				} else if (paramVal.equals("edit")) {

					Long shUid = new Long(req.getParameter("shUid"));
					ScheduledHarvest sh = shm.getScheduledHarvest(shUid);
					String reg = sh.getEnabledDisabled();
					if (!(reg.equalsIgnoreCase("enabled"))) {
						reg = "disabled";
					}
					haf.setShEnabledDisabled(reg);

					haf.setShUid(sh.getUid().toString());
					haf.setShBaseURL(sh.getBaseURL());
					haf.setShHarvestingInterval(sh.getHarvestingInterval());
					haf.setShIntervalGranularity(sh.getIntervalGranularity());
					haf.setShRunAtTime(sh.getRunAtTime());
					haf.setShMetadataPrefix(sh.getMetadataPrefix());
					haf.setShRepositoryName(sh.getRepositoryName());
					haf.setShSetSpec(sh.getSetSpec());
					haf.setAllowDupDir(sh.getAllowDupDir());
					haf.setShDontZipFiles(!sh.getDoZipResult());

					File defaultHDir = (File) ds.get(Keys.HARVESTED_DATA_DIR);
					String defaultDir = defaultHDir.toString();
					haf.setDefDir(defaultDir);
					String dir = sh.getHarvestDir().toString();
					if (dir.equals(defaultDir)) {
						haf.setShDir("default");
						haf.setShHarvestDir("");
					} else {
						haf.setShDir("custom");
						haf.setShHarvestDir(dir);
					}

					boolean spl = sh.getSplitBySet();
					if (spl == true) {
						haf.setShSet("split");
					} else {
						haf.setShSet("dontsplit");
					}
					return mapping.findForward("edit.harvester.settings");
				
				// Add a new harvest...
				} else if (paramVal.equals("save")) {
					Long shUid = new Long(req.getParameter("shUid"));
					ScheduledHarvest sh;
					String choice = req.getParameter("shDir");
					String allowD;
					if (req.getParameter("allowDupDir") == null) {
						allowD = "dontallow";
					} else {
						allowD = req.getParameter("allowDupDir");
					}

					if (choice.equalsIgnoreCase("custom")) {
						if ((allowD == null) || (!(allowD.equalsIgnoreCase("allow")))) {
							haf.setAllowDupDir("dontallow");

							String d = req.getParameter("shHarvestDir");
							String myId = req.getParameter("shUid");
							Long mId = Long.valueOf(myId);
							boolean foundsame = false;
							Hashtable shhash = shm.getScheduledHarvests();
							if (shhash != null) {
								Enumeration enumm = shhash.keys();

								while (enumm.hasMoreElements()) {
									Long values = (Long) enumm.nextElement();
									ScheduledHarvest sharvest = (ScheduledHarvest) shhash.get(values);
									Long id = sharvest.getUid();
									if (id.longValue() != mId.longValue()) {
										String dir = (sharvest.getHarvestDir()).toString();
										if (d.equalsIgnoreCase(dir)) {
											foundsame = true;
											break;
										}
									}
								}
								if (foundsame == true) {
									errors.add("shHarvestDir", new ActionError("generic.message",
											"You have chosen to save this harvest to a location which is already in use."));
									saveErrors(req, errors);
									return mapping.findForward("edit.harvester.settings");
								}
							}
						}
					}

					if (shUid.intValue() != 0 && !shm.containsScheduledHarvest(shUid)) {
						return mapping.findForward("display.harvester.settings");
					}

					String sp = req.getParameter("shSet");
					boolean split = false;
					if (sp.equalsIgnoreCase("split")) {
						split = true;
					} else {
						split = false;
					}

					boolean def = false;
					String direc = "";
					if (choice.equals("custom")) {
						def = false;
						direc = req.getParameter("shHarvestDir");
					}

					if ((choice.equals("default")) || (direc.equals(""))) {
						def = true;
						File defaultHDir = (File) ds.get(Keys.HARVESTED_DATA_DIR);
						String defaultDir = defaultHDir.toString();
						direc = defaultDir;
					}

					String reg = req.getParameter("shEnabledDisabled");
					if (reg == null) {
						reg = "disabled";
					}
					
					boolean doZipFiles = true;
					String doZipFilesStr = req.getParameter("shDontZipFiles");
					if (doZipFilesStr != null && doZipFilesStr.equals("true")) {
						doZipFiles = false;
					}					
					
					sh = new ScheduledHarvest(
							req.getParameter("shRepositoryName"),
							req.getParameter("shSetSpec"),
							req.getParameter("shBaseURL"),
							req.getParameter("shMetadataPrefix"),
							req.getParameter("shHarvestingInterval"),
							req.getParameter("shIntervalGranularity"),
							req.getParameter("shRunAtTime"),
							reg,
							new File(direc),
							split, false, def, doZipFiles, allowD
							);

					/*
					 *  if (shm.containsValue(sh))
					 *  {
					 *  prtln("contains value.");
					 *  return mapping.findForward("display.harvester.settings");
					 *  }
					 */
					ScheduledHarvest prevSh = shm.getScheduledHarvest(shUid);
					if (prevSh != null) {
						sh.setUid(prevSh.getUid());
						sh.setSplitBySet(split);

						boolean isZip = prevSh.getIsZipPresent();
						sh.setIsZipPresent(isZip);

						// Copy data that is not set/edited by the form:
						if (sh.getBaseURL().equals(prevSh.getBaseURL()) &&
								sh.getSetSpec().equals(prevSh.getSetSpec()) &&
								sh.getMetadataPrefix().equals(prevSh.getMetadataPrefix())) {
							sh.setLastHarvestTime(prevSh.getLastHarvestTime());
							sh.setZipLatest(prevSh.getZipLatest());
							sh.setBackupOne(prevSh.getBackupOne());
							sh.setBackupTwo(prevSh.getBackupTwo());
							sh.setBackupThree(prevSh.getBackupThree());
							sh.setIsZipPresent(prevSh.getIsZipPresent());
							sh.setNumHarvestedLast(prevSh.getNumHarvestedLast());
						}
					}

					shm.removeScheduledHarvest(shUid, false);

					shm.addScheduledHarvest(sh);

					haf.setScheduledHarvests(shm.getScheduledHarvests());

					return mapping.findForward("display.harvester.settings");
				} else if (paramVal.equals("delete")) {
					Long shUid = new Long(req.getParameter("shUid"));
					prtln("Deleting scheduledHarvest uid " + shUid);

					shm.removeScheduledHarvest(shUid, true);
					haf.setScheduledHarvests(shm.getScheduledHarvests());
					return mapping.findForward("display.harvester.settings");
				} else if (paramVal.equals("runHarvest")) {

					// Ensure harvests only occur once even if link is clicked multiple times.
					/*
					 *  Long ses = new Long(req.getParameter("mySes"));
					 *  if (mySessions.containsKey(ses))
					 *  mySessions.remove(ses);
					 *  else
					 *  return mapping.findForward("display.harvester.settings");
					 */
					Long shUid = new Long(req.getParameter("shUid"));
					prtln("runHarvest for " + shUid);
					ScheduledHarvest prevSh = shm.getScheduledHarvest(shUid);
					prtln(prevSh.getHarvestDir().toString());

					if (!shm.containsScheduledHarvest(shUid)) {
						errors.add("harvestErr", new ActionError("generic.message", "The requested harvest is no longer configured."));
						saveErrors(req, errors);
						return mapping.findForward("display.harvester.settings");
					}

					if (shm.isRunning(shUid)) {
						errors.add("harvestErr", new ActionError("generic.message", "A harvest for " +
								shm.getScheduledHarvest(shUid).getRepositoryName() + " is already in progress."));
						saveErrors(req, errors);
						return mapping.findForward("display.harvester.settings");
					}
					
					boolean harvestAllIfNoDeletedRecord = false;
					
					if (req.getParameter("doAll") != null) {
						shm.harvestNow(shUid, true, harvestAllIfNoDeletedRecord);
					} else {
						shm.harvestNow(shUid, false, harvestAllIfNoDeletedRecord);
					}

					haf.setLastRunHarvest(shm.getScheduledHarvest(shUid));

					errors.add("runHarvest", new ActionError("generic.message", "The harvest for " +
							shm.getScheduledHarvest(shUid).getRepositoryName() + " has been started."));

					saveErrors(req, errors);
					return mapping.findForward("display.harvester.settings");
				}
			}

			Enumeration params = req.getParameterNames();

			// Set the appropriate forwarding:
			String paramName = null;
			String[] paramValues = null;
			while (params.hasMoreElements()) {
				paramName = (String) params.nextElement();
				paramValues = req.getParameterValues(paramName);

				if (paramName.startsWith("edit") || paramName.startsWith("add")) {
					if (paramValues[0].startsWith("Edit") || paramValues[0].startsWith("Add")) {
						return mapping.findForward("edit.harvester.settings");
					}
				}
			}

			// Default forwarding:
			return mapping.findForward("display.harvester.settings");
		} catch (NullPointerException e) {
			prtln("HarvesterAdminAction caught exception.");
			e.printStackTrace();
			return mapping.findForward("display.harvester.settings");
		} catch (Throwable e) {
			prtln("HarvesterAdminAction caught exception: " + e);
			return mapping.findForward("display.harvester.settings");
		}
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
		} else {
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
	 *  Gets the intValue attribute of the HarvesterAdminAction object
	 *
	 * @param  isInt  String as an integer
	 * @return        The intValue value
	 */
	private final int getIntValue(String isInt) {
		try {
			return Integer.parseInt(isInt);
		} catch (Throwable e) {
			return -1;
		}
	}


	/**
	 *  Sets the defaults attribute of the HarvesterAdminAction object
	 *
	 * @param  ds              The new defaults value
	 * @param  servletContext  The new defaults value
	 * @return                 DESCRIPTION
	 * @exception  Exception   DESCRIPTION
	 */
	private boolean setDefaults(SimpleDataStore ds, ServletContext servletContext)
			 throws Exception {

		// Grab settings:
		File harvestedDataDir = (File) ds.get(Keys.HARVESTED_DATA_DIR);
		if (harvestedDataDir == null) {
			harvestedDataDir =
					new File(GeneralServletTools.getAbsolutePath("WEB-INF" + File.separator + "harvested_records", servletContext));
			ds.put(Keys.HARVESTED_DATA_DIR, harvestedDataDir);
		}
		
		String harvesterData = servletContext.getInitParameter("zippedHarvestsDirectory");
		if(harvesterData == null || harvesterData.trim().length() == 0){
			prtlnErr("Warning: Init parameter 'zippedHarvestsDirectory' is missing");
			harvesterData = "admin/zipped_harvests";
		}

		File zipDir = new File(GeneralServletTools.getAbsolutePath(harvesterData, servletContext));
		ds.put(Keys.ZIP_DIR, zipDir);

		if (!harvestedDataDir.exists()) {
			harvestedDataDir.mkdir();
		}

		if (!zipDir.exists()) {
			zipDir.mkdir();
		}


		if (ds.get(Keys.HARVESTER_STATUS) == null) {
			ds.put(Keys.HARVESTER_STATUS, "ENABLED");
		}
		if (ds.get(Keys.VALIDATE_RECORDS) == null) {
			ds.put(Keys.VALIDATE_RECORDS, new Boolean(false));
		}

		return true;
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

