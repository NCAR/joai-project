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
package org.dlese.dpc.oai.harvester.action.form;



import org.dlese.dpc.propertiesmgr.*;

import org.dlese.dpc.webapps.tools.*;

import org.dlese.dpc.oai.harvester.structs.*;

import org.dlese.dpc.oai.harvester.*;



import org.apache.struts.action.Action;

import org.apache.struts.action.ActionError;

import org.apache.struts.action.ActionErrors;

import org.apache.struts.action.ActionForm;

import org.apache.struts.action.ActionForward;

import org.apache.struts.action.ActionMapping;

import org.apache.struts.action.ActionServlet;

import org.apache.struts.util.MessageResources;

import org.apache.struts.validator.ValidatorForm;



import javax.servlet.http.HttpServletRequest;

import java.util.*;

import java.text.*;

import java.io.*;



/**

 *  Presentation bean used by the Harvester action controller and jsp pages.

 *

 * @author     John Weatherley

 * @version    $Id: HarvesterAdminForm.java,v 1.20.2.3 2012/10/05 23:48:57 jweather Exp $

 * @created    January 11, 2006

 */

public final class HarvesterAdminForm extends ActionForm {

	private static boolean debug = true;



	private String message = null;

	private String mySes = null;

	private String harvesterStatus = null;

	private Boolean validateRecords = null;

	private String baseURL = null;

	private String metadataPrefix = null;

	private String setSpec = null;

	private String from = null;

	private String until = null;

	private String harvestedDataDir = null;

	private ScheduledHarvest lastOneTimeHarvest = null;

	private ScheduledHarvest lastRunHarvest = null;

	private Hashtable scheduledHarvests = null;

	private Object[] scheduledHarvestsSorted = null;



	private String shRepositoryName = null;

	private String shSetSpec = null;

	private String shBaseURL = null;

	private String shMetadataPrefix = null;

	private String shHarvestingInterval = null;

	private String shIntervalGranularity = null;

	private String shRunAtTime = null;

	private String shEnabledDisabled = null;

	private String shUid = null;

	private String shHarvestedDataDir = null;

	private String shHarvestDir = null;

	private String shSet = null;

	private String shDir = null;
	
	private boolean shDontZipFiles = true;

	private String defDir = null;

	private static String[] commonDirs = null;

	private final int MAX_DIRS = 5;

	private String allowDupDir = "dontallow";





	/**

	 *  Reset bean properties to their default state, as needed. This method is called before the properties are

	 *  repopulated by the controller. <p>

	 *

	 *  The default implementation does nothing. In practice, the only properties that need to be reset are those

	 *  which represent checkboxes on a session-scoped form. Otherwise, properties can be given initial values

	 *  where the field is declared. *

	 *

	 * @param  mapping  ActionMapping

	 * @param  request  HttpServletRequest

	 */

	public void reset(ActionMapping mapping, HttpServletRequest request) {

		setShEnabledDisabled("disabled");

	}


	/**
	 * Returns the value of shDontZipFiles.
	 */
	public boolean getShDontZipFiles()
	{
		return shDontZipFiles;
	}

	/**
	 * Sets the value of shDontZipFiles.
	 * @param shDontZipFiles The value to assign shDontZipFiles.
	 */
	public void setShDontZipFiles(boolean shDontZipFiles)
	{
		this.shDontZipFiles = shDontZipFiles;
	}


	/**

	 *  Gets the shSet attribute of the HarvesterAdminForm object

	 *

	 * @return    The shSet value

	 */

	public String getShSet() {

		return this.shSet;

	}





	/**

	 *  Gets the allowDupDir attribute of the HarvesterAdminForm object

	 *

	 * @return    The allowDupDir value

	 */

	public String getAllowDupDir() {

		return allowDupDir;

	}





	/**

	 *  Sets the allowDupDir attribute of the HarvesterAdminForm object

	 *

	 * @param  v  The new allowDupDir value

	 */

	public void setAllowDupDir(String v) {

		allowDupDir = v;

	}





	/**

	 *  Sets the runAtTime attribute of the HarvesterAdminForm object

	 *

	 * @param  v  The new runAtTime value

	 */

	public void setShRunAtTime(String v) {

		shRunAtTime = v;

	}





	/**

	 *  Gets the runAtTime attribute of the HarvesterAdminForm object

	 *

	 * @return    The runAtTime value

	 */

	public String getShRunAtTime() {

		return shRunAtTime;

	}





	/**

	 *  Gets the mAX_DIRS attribute of the HarvesterAdminForm object

	 *

	 * @return    The mAX_DIRS value

	 */

	public int getMAX_DIRS() {

		return MAX_DIRS;

	}





	/**  Sets the mAX_DIRS attribute of the HarvesterAdminForm object */

	public void setMAX_DIRS() {



	}





	/**

	 *  Gets the commonDirs attribute of the HarvesterAdminForm object

	 *

	 * @return    The commonDirs value

	 */

	public String[] getCommonDirs() {



		return commonDirs;

	}





	/**

	 *  Gets the defDir attribute of the HarvesterAdminForm object

	 *

	 * @return    The defDir value

	 */

	public String getDefDir() {

		//String newDefDir = defDir.replaceAll("\\\\", "\\\\\\\\");

		return defDir;

	}





	/**

	 *  Sets the defDir attribute of the HarvesterAdminForm object

	 *

	 * @param  d  The new defDir value

	 */

	public void setDefDir(String d) {

		defDir = d;

	}





	/**

	 *  Sets the commonDirs attribute of the HarvesterAdminForm object

	 *

	 * @param  dirs  The new commonDirs value

	 */

	public void setCommonDirs(String[] dirs) {



		/*

		 *  for (int i = 0; (i < dirs.length) && (i < commonDirs.length); i++) {

		 *  commonDirs[i] = dirs[i];

		 *  }

		 */

		int n = getMAX_DIRS();

		commonDirs = new String[n];



		for (int i = 0; i < n; i++) {

			commonDirs[i] = dirs[i];

		}



	}





	/**

	 *  Gets the shDir attribute of the HarvesterAdminForm object

	 *

	 * @return    The shDir value

	 */

	public String getShDir() {

		return this.shDir;

	}





	/**

	 *  Sets the shSet attribute of the HarvesterAdminForm object

	 *

	 * @param  s  The new shSet value

	 */

	public void setShSet(String s) {

		this.shSet = s;

	}





	/**

	 *  Sets the shDir attribute of the HarvesterAdminForm object

	 *

	 * @param  s  The new shDir value

	 */

	public void setShDir(String s) {

		this.shDir = s;

	}





	// Scheduled harvest vars:



	/**

	 *  Gets the shRepositoryName attribute of the HarvesterAdminForm object

	 *

	 * @return    The shRepositoryName value

	 */

	public String getShRepositoryName() {

		return shRepositoryName;

	}





	/**

	 *  Sets the shRepositoryName attribute of the HarvesterAdminForm object

	 *

	 * @param  val  The new shRepositoryName value

	 */

	public void setShRepositoryName(String val) {

		shRepositoryName = val;

	}







	/**

	 *  Gets the shSetSpec attribute of the HarvesterAdminForm object

	 *

	 * @return    The shSetSpec value

	 */

	public String getShSetSpec() {

		return shSetSpec;

	}





	/**

	 *  Sets the shSetSpec attribute of the HarvesterAdminForm object

	 *

	 * @param  val  The new shSetSpec value

	 */

	public void setShSetSpec(String val) {

		shSetSpec = val;

	}





	/**

	 *  Sets the lastOneTimeHarvest attribute of the HarvesterAdminForm object

	 *

	 * @param  sh  The new lastOneTimeHarvest value

	 */

	public void setLastOneTimeHarvest(ScheduledHarvest sh) {

		this.lastOneTimeHarvest = sh;

	}





	/**

	 *  Gets the lastOneTimeHarvest attribute of the HarvesterAdminForm object

	 *

	 * @return    The lastOneTimeHarvest value

	 */

	public ScheduledHarvest getLastOneTimeHarvest() {

		return lastOneTimeHarvest;

	}





	/**

	 *  Sets the lastRunHarvest attribute of the HarvesterAdminForm object

	 *

	 * @param  sh  The new lastRunHarvest value

	 */

	public void setLastRunHarvest(ScheduledHarvest sh) {

		this.lastRunHarvest = sh;

	}





	/**

	 *  Gets the lastRunHarvest attribute of the HarvesterAdminForm object

	 *

	 * @return    The lastRunHarvest value

	 */

	public ScheduledHarvest getLastRunHarvest() {

		return lastRunHarvest;

	}





	/**

	 *  Gets the shBaseURL attribute of the HarvesterAdminForm object

	 *

	 * @return    The shBaseURL value

	 */

	public String getShBaseURL() {

		return shBaseURL;

	}





	/**

	 *  Sets the shBaseURL attribute of the HarvesterAdminForm object

	 *

	 * @param  val  The new shBaseURL value

	 */

	public void setShBaseURL(String val) {

		shBaseURL = val;

	}





	/**

	 *  Gets the shMetadataPrefix attribute of the HarvesterAdminForm object

	 *

	 * @return    The shMetadataPrefix value

	 */

	public String getShMetadataPrefix() {

		return shMetadataPrefix;

	}





	/**

	 *  Sets the shMetadataPrefix attribute of the HarvesterAdminForm object

	 *

	 * @param  val  The new shMetadataPrefix value

	 */

	public void setShMetadataPrefix(String val) {

		shMetadataPrefix = val;

	}





	/**

	 *  Gets the shHarvestingInterval attribute of the HarvesterAdminForm object

	 *

	 * @return    The shHarvestingInterval value

	 */

	public String getShHarvestingInterval() {

		return shHarvestingInterval;

	}





	/**

	 *  Sets the shHarvestingInterval attribute of the HarvesterAdminForm object

	 *

	 * @param  val  The new shHarvestingInterval value

	 */

	public void setShHarvestingInterval(String val) {

		shHarvestingInterval = val;

	}





	/**

	 *  Gets the shIntervalGranularity attribute of the HarvesterAdminForm object

	 *

	 * @return    The shIntervalGranularity value

	 */

	public String getShIntervalGranularity() {

		return shIntervalGranularity;

	}





	/**

	 *  Gets the shIntervalGranularityLabel attribute of the HarvesterAdminForm object

	 *

	 * @return    The shIntervalGranularityLabel value

	 */

	public String getShIntervalGranularityLabel() {

		if (shHarvestingInterval != null && shIntervalGranularity != null && shHarvestingInterval.equals("1")) {

			return shIntervalGranularity.substring(0, shIntervalGranularity.length() - 1);

		}

		return shIntervalGranularity;

	}





	/**

	 *  Sets the shIntervalGranularity attribute of the HarvesterAdminForm object

	 *

	 * @param  val  The new shIntervalGranularity value

	 */

	public void setShIntervalGranularity(String val) {

		shIntervalGranularity = val;

	}







	/**

	 *  Gets the shEnabledDisabled attribute of the HarvesterAdminForm object

	 *

	 * @return    The shEnabledDisabled value

	 */

	public String getShEnabledDisabled() {

		return shEnabledDisabled;

	}





	/**

	 *  Sets the shEnabledDisabled attribute of the HarvesterAdminForm object

	 *

	 * @param  val  The new shEnabledDisabled value

	 */

	public void setShEnabledDisabled(String val) {

		shEnabledDisabled = val;

	}







	/**

	 *  Gets the shUid attribute of the HarvesterAdminForm object

	 *

	 * @return    The shUid value

	 */

	public String getShUid() {

		return shUid;

	}





	/**

	 *  Sets the shUid attribute of the HarvesterAdminForm object

	 *

	 * @param  val  The new shUid value

	 */

	public void setShUid(String val) {

		shUid = val;

	}





	/**

	 *  Sets the shHarvestedDataDir attribute of the HarvesterAdminForm object

	 *

	 * @param  val  The new shHarvestedDataDir value

	 */

	public void setShHarvestedDataDir(String val) {

		this.shHarvestedDataDir = val;

	}





	/**

	 *  Gets the shHarvestedDataDir attribute of the HarvesterAdminForm object

	 *

	 * @return    The shHarvestedDataDir value

	 */

	public String getShHarvestedDataDir() {

		return shHarvestedDataDir;

	}





	/*

	 *  public void setShHarvesterStatus(String val)

	 *  {

	 *  = val;

	 *  }

	 */

	/**

	 *  Gets the shEnabledDisabledList attribute of the HarvesterAdminForm object

	 *

	 * @return    The shEnabledDisabledList value

	 */

	public String[] getShEnabledDisabledList() {

		return new String[]{"disabled", "enabled"};

	}





	/**

	 *  Gets the shIntervalGranularityList attribute of the HarvesterAdminForm object

	 *

	 * @return    The shIntervalGranularityList value

	 */

	public String[] getShIntervalGranularityList() {

		return new String[]{"days", "hours", "minutes", "seconds"};

	}





	/**

	 *  Gets the shIntervalGranularityListLabels attribute of the HarvesterAdminForm object

	 *

	 * @return    The shIntervalGranularityListLabels value

	 */

	public String[] getShIntervalGranularityListLabels() {

		return new String[]{"day(s)", "hour(s)", "minute(s)", "second(s)"};

	}





	/**

	 *  Sets the shHarvestDir attribute of the HarvesterAdminForm object

	 *

	 * @param  val  The new shHarvestDir value

	 */

	public void setShHarvestDir(String val) {

		shHarvestDir = val;



		// add to the commonDirs object

		add_to_common_dirs(val);

	}





	/**

	 *  Gets the shHarvestDir attribute of the HarvesterAdminForm object

	 *

	 * @return    The shHarvestDir value

	 */

	public String getShHarvestDir() {

		return shHarvestDir;

	}





	// Individual harvest vars:



	/**

	 *  Sets the baseURL attribute of the HarvesterAdminForm object

	 *

	 * @param  val  The new baseURL value

	 */

	public void setBaseURL(String val) {

		baseURL = val;

	}





	/**

	 *  Gets the baseURL attribute of the HarvesterAdminForm object

	 *

	 * @return    The baseURL value

	 */

	public String getBaseURL() {

		return baseURL;

	}





	/**

	 *  Sets the metadataPrefix attribute of the HarvesterAdminForm object

	 *

	 * @param  val  The new metadataPrefix value

	 */

	public void setMetadataPrefix(String val) {

		metadataPrefix = val;

	}





	/**

	 *  Gets the metadataPrefix attribute of the HarvesterAdminForm object

	 *

	 * @return    The metadataPrefix value

	 */

	public String getMetadataPrefix() {

		return metadataPrefix;

	}





	/**

	 *  Sets the setSpec attribute of the HarvesterAdminForm object

	 *

	 * @param  val  The new setSpec value

	 */

	public void setSetSpec(String val) {

		setSpec = val;

	}





	/**

	 *  Gets the setSpec attribute of the HarvesterAdminForm object

	 *

	 * @return    The setSpec value

	 */

	public String getSetSpec() {

		return setSpec;

	}





	/**

	 *  Sets the from attribute of the HarvesterAdminForm object

	 *

	 * @param  val  The new from value

	 */

	public void setFrom(String val) {

		from = val;

	}





	/**

	 *  Gets the from attribute of the HarvesterAdminForm object

	 *

	 * @return    The from value

	 */

	public String getFrom() {

		return from;

	}





	/**

	 *  Sets the until attribute of the HarvesterAdminForm object

	 *

	 * @param  val  The new until value

	 */

	public void setUntil(String val) {

		until = val;

	}





	/**

	 *  Gets the until attribute of the HarvesterAdminForm object

	 *

	 * @return    The until value

	 */

	public String getUntil() {

		return until;

	}





	/**

	 *  Gets the harvestedDataDir attribute of the HarvesterAdminForm object

	 *

	 * @return    The harvestedDataDir value

	 */

	public String getHarvestedDataDir() {

		return harvestedDataDir;

	}





	/**

	 *  Sets the harvestedDataDir attribute of the HarvesterAdminForm object

	 *

	 * @param  val  The new harvestedDataDir value

	 */

	public void setHarvestedDataDir(String val) {

		this.harvestedDataDir = val;

	}





	/**

	 *  Sets the harvesterStatus attribute of the HarvesterAdminForm object

	 *

	 * @param  val  The new harvesterStatus value

	 */

	public void setHarvesterStatus(String val) {

		harvesterStatus = val;

	}





	/**

	 *  Gets the harvesterStatus attribute of the HarvesterAdminForm object

	 *

	 * @return    The harvesterStatus value

	 */

	public String getHarvesterStatus() {

		if (harvesterStatus == null) {

			harvesterStatus = "ENABLED";

		}

		return harvesterStatus;

	}





	/**

	 *  Sets the scheduledHarvests attribute of the HarvesterAdminForm object

	 *

	 * @param  scheduledHarvests  The new scheduledHarvests value

	 */

	public void setScheduledHarvests(Hashtable scheduledHarvests) {

		this.scheduledHarvests = scheduledHarvests;

		scheduledHarvestsSorted = null;

	}





	/**

	 *  Gets the scheduledHarvests attribute of the HarvesterAdminForm object

	 *

	 * @return    The scheduledHarvests value

	 */

	public Object[] getScheduledHarvests() {

		if (scheduledHarvestsSorted == null) {

			if (scheduledHarvests == null || scheduledHarvests.size() == 0) {

				return null;

			}

			scheduledHarvestsSorted = scheduledHarvests.values().toArray();

			Arrays.sort(scheduledHarvestsSorted);

		}

		return scheduledHarvestsSorted;

	}





	/**

	 *  Sets the validateRecords attribute of the HarvesterAdminForm object

	 *

	 * @param  val  The new validateRecords value

	 */

	public void setValidateRecords(Boolean val) {

		validateRecords = val;

	}





	/**

	 *  Gets the validateRecords attribute of the HarvesterAdminForm object

	 *

	 * @return    The validateRecords value

	 */

	public String getValidateRecords() {

		if (validateRecords == null) {

			return "false";

		}

		return validateRecords.toString();

	}







	/**

	 *  Sets the mySes attribute of the HarvesterAdminForm object

	 *

	 * @param  val  The new mySes value

	 */

	public void setMySes(String val) {

		mySes = val;

	}





	/**

	 *  Gets the mySes attribute of the HarvesterAdminForm object

	 *

	 * @return    The mySes value

	 */

	public String getMySes() {

		if (mySes == null) {

			return "";

		}

		return mySes;

	}







	/**  Constructor for the HarvesterAdminForm Bean object */

	public HarvesterAdminForm() {

		setShSet("split");

		prtln("HarvesterAdminForm() ");

	}





	/**

	 *  Sets the message attribute of the HarvesterAdminForm object

	 *

	 * @param  message  The new message value

	 */

	public void setMessage(String message) {

		this.message = message;

	}





	/**

	 *  Gets the message attribute of the HarvesterAdminForm object

	 *

	 * @return    The message value

	 */

	public String getMessage() {

		return message;

	}







	// ************************ Validation methods ***************************



	/**

	 *  Validate the input. This method is called AFTER the setter method is called.

	 *

	 * @param  mapping  The ActionMapping used.

	 * @param  request  The HttpServletRequest for this request.

	 * @return          An ActionError containin any errors that had occured.

	 */

	public ActionErrors validate(ActionMapping mapping, HttpServletRequest request) {

		ActionErrors errors = new ActionErrors();

		//prtln("\n\n\nvalidate()");



		try {



			// Harvested Data Dir

			String param = request.getParameter("editHarvestedDataDir");

			if (param != null && param.equals("save")) {

				if (harvestedDataDir == null || !(new File(harvestedDataDir.trim()).isDirectory())) {

					errors.add("harvestedDataDir", new ActionError("errors.harvestedDataDir"));

				}

			}

			param = request.getParameter("editShHarvestedDataDir");

			if (param != null && param.equals("save")) {

				if (shHarvestedDataDir == null || !(new File(shHarvestedDataDir.trim()).isDirectory())) {

					errors.add("shHarvestedDataDir", new ActionError("errors.harvestedDataDir"));

				}

			}



			// ScheduledHarvest validation

			param = request.getParameter("scheduledHarvest");

			if (param != null && param.equals("save")) {



				if (shMetadataPrefix.trim().length() == 0) {

					errors.add("shMetadataPrefix", new ActionError("errors.metadataPrefix"));

				}



				if (getShEnabledDisabled().equalsIgnoreCase("enabled")) {



					if (shHarvestingInterval.trim().length() == 0) {

						errors.add("shHarvestingInterval", new ActionError("generic.message",

							"Please indicate a harvesting interval."));

					}

					else {

						try {

							int interval = Integer.parseInt(shHarvestingInterval);

							if (interval <= 0) {

								errors.add("shHarvestingInterval", new ActionError("generic.message",

									"Value must be greater than 0."));

							}

						} catch (NumberFormatException e) {

							errors.add("shHarvestingInterval", new ActionError("generic.message",

								"The harvesting interval must be an integer."));

						}

					}



					// Check on time of harvest for days:

					if (shIntervalGranularity.equals("days") && (shRunAtTime == null || shRunAtTime.trim().length() == 0)) {

						errors.add("shRunAtTime", new ActionError("generic.message",

							"Please indicate the time for this harvest to begin"));

					}

					if (shRunAtTime != null) {

						if (shRunAtTime.trim().length() > 0) {

							// Make sure we can parse the scheduled harvest time properly:

							try {

								if (shRunAtTime.matches(".*[a-zA-Z].*")) {

									errors.add("shRunAtTime", new ActionError("generic.message",

										"Inproper time (contains letters). Please specify hours and minutes using 24 hour time, for example 23:15"));

								}

								else

									ScheduledHarvestManager.getHarvestStartDate(shRunAtTime);

							} catch (ParseException pe) {

								errors.add("shRunAtTime", new ActionError("generic.message",

									"Inproper time. Please specify hours and minutes using 24 hour time, for example 23:15"));

							} catch (Throwable t) {

								errors.add("shRunAtTime", new ActionError("generic.message",

									"Error reading the time: " + t.getMessage()));

							}

						}

					}

				}

				if (shBaseURL.trim().length() == 0) {

					errors.add("shBaseURL", new ActionError("generic.message",

						"Please provide a base URL."));

				}

				else if (!(shBaseURL.startsWith("http://") || shBaseURL.startsWith("https://"))) {

					errors.add("shBaseURL", new ActionError("generic.message",

						"The baseURL must begin with http:// or https://"));

				}

				/*

				 *  if (shHarvestDir.trim().length() == 0) {

				 *  errors.add("shHarvestDir", new ActionError("generic.message",

				 *  "Please provide the path to a valid directory where the records for this harvest will be saved."));

				 *  }

				 */

			}

		} catch (NullPointerException e) {

			prtlnErr("validate() error: " + e);

			e.printStackTrace();

		} catch (Throwable e) {

			prtlnErr("validate() error: " + e);

		}

		if (!errors.isEmpty()) {

			//prtln("validate() returning errors... ");

		}



		//prtln("\n\n\n");

		return errors;

	}





	/**

	 *  Validates the format of an e-mail address.

	 *

	 * @param  email  The e-mail address to validate.

	 * @return        True iff this e-mail has a valid format.

	 */

	private final boolean emailIsValid(String email) {

		if (email == null || email.trim().length() == 0) {

			return true;

		}

		return FormValidationTools.isValidEmail(email);

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





	/**

	 *  Description of the Method

	 *

	 * @param  val  Description of the Parameter

	 */

	private final void add_to_common_dirs(String val) {



		if (val == null) {

			return;

		}

		if (val == "") {

			return;

		}



		if (commonDirs == null) {

			commonDirs = new String[MAX_DIRS];

			for (int i = 0; i < MAX_DIRS; i++) {

				commonDirs[i] = "";

			}

			commonDirs[0] = val;



		}

		else {



			/*

			 *  check for duplicates

			 */

			for (int i = 0; i < MAX_DIRS; i++) {

				if (commonDirs[i].equalsIgnoreCase(val)) {

					return;

				}

			}

			for (int i = MAX_DIRS - 1; i > 0; i--) {

				commonDirs[i] = commonDirs[i - 1];

			}

			commonDirs[0] = val;



		}



	}



}





