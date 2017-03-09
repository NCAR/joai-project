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
package org.dlese.dpc.oai.harvester.structs;

import java.io.*;
import java.util.*;
import java.text.*;
import org.dlese.dpc.oai.*;
import org.dlese.dpc.oai.harvester.*;

/**
 *  Data structure that holds the info needed to configure a regular-occuring harvest of a particular
 *  repository.
 *
 * @author     John Weatherley
 * @version    $Id: ScheduledHarvest.java,v 1.20.2.1 2012/09/28 23:53:50 jweather Exp $
 * @created    January 13, 2006
 */
public final class ScheduledHarvest implements Serializable, Comparable {
	private static boolean debug = true;

	private static long next = 0;
	private static int num = 4;

	private String repositoryName = "";
	private String setSpec = "";
	private String baseURL = "";
	private String metadataPrefix = "";
	private String harvestingInterval = "";
	private String intervalGranularity = "";
	private String enabledDisabled = "";
	private Long uid = null;
	private Date lastHarvestTime = null;
	private File harvestDir = null;
	private boolean splitBySet = false;
	private boolean doZip = true;
	private boolean isZipPresent = false;
	private String backupOne = "";
	private String backupTwo = "";
	private String backupThree = "";
	private String zipLatest = "";
	private boolean defaultDir = true;
	private int numHarvestedLast = -1;
	private String allowDupDir = "";
	private final static int maxR = 30000;
	private boolean warnR = false;
	private boolean harvestAll = false;
	private String runAtTime = null;



	/**  Constructor for the ScheduledHarvest object */
	public ScheduledHarvest() {
		prtln("ScheduledHarvest() empty constructor");
	}


	/**
	 *  Constructor for the ScheduledHarvest object
	 *
	 * @param  repositoryName       Name of the repository.
	 * @param  setSpec              setSpec to be harvested, or null for none.
	 * @param  baseURL              BaseURL.
	 * @param  metadataPrefix       MetadataPrefix.
	 * @param  harvestingInterval   Interval for harvesting.
	 * @param  intervalGranularity  Interval granularity.
	 * @param  runAtTime            The time of day to begin the harvest in 24 hour time for example 23:15, or
	 *      null to schedule immediately
	 * @param  enabledDisabled      One of exactly enabled or disabled.
	 * @param  harvestDir           Description of the Parameter
	 * @param  spl                  Description of the Parameter
	 * @param  isZip                Description of the Parameter
	 * @param  def                  Description of the Parameter
	 * @param  dupAllow             Description of the Parameter
	 * @param  doZip                True to zip the resulting harvest
	 */
	public ScheduledHarvest(
	                        String repositoryName,
	                        String setSpec,
	                        String baseURL,
	                        String metadataPrefix,
	                        String harvestingInterval,
	                        String intervalGranularity,
	                        String runAtTime,
	                        String enabledDisabled,
	                        File harvestDir,
	                        boolean spl,
	                        boolean isZip,
	                        boolean def,
	                        boolean doZip,
	                        String dupAllow) {
		prtln("ScheduledHarvest() long constructor");
		this.runAtTime = runAtTime;
		this.repositoryName = repositoryName.trim();
		this.setSpec = setSpec.trim();
		this.baseURL = baseURL.trim();
		this.metadataPrefix = metadataPrefix.trim();
		this.harvestingInterval = harvestingInterval;
		this.intervalGranularity = intervalGranularity;
		this.enabledDisabled = enabledDisabled;
		this.lastHarvestTime = null;
		this.harvestDir = harvestDir;
		this.splitBySet = spl;
		this.isZipPresent = isZip;
		this.defaultDir = def;
		this.doZip = doZip;
		this.allowDupDir = dupAllow;

		prtln("runAtTime: " + getRunAtTime());

		uid = new Long(System.currentTimeMillis() + next++);
	}



	// ---- Getters ----

	/**
	 *  Gets the uid attribute of the ScheduledHarvest object
	 *
	 * @return    The uid value
	 */
	public Long getUid() {
		return uid;
	}


	/**
	 *  Gets the warnR attribute of the ScheduledHarvest object
	 *
	 * @return    The warnR value
	 */
	public boolean getWarnR() {
		return warnR;
	}



	/**
	 *  Gets the allowDupDir attribute of the ScheduledHarvest object
	 *
	 * @return    The allowDupDir value
	 */
	public String getAllowDupDir() {
		return allowDupDir;
	}


	/**
	 *  Gets the defaultDir attribute of the ScheduledHarvest object
	 *
	 * @return    The defaultDir value
	 */
	public boolean getDefaultDir() {
		return defaultDir;
	}


	/**
	 *  Gets the numHarvestedLast attribute of the ScheduledHarvest object
	 *
	 * @return    The numHarvestedLast value
	 */
	public int getNumHarvestedLast() {
		return numHarvestedLast;
	}


	/**
	 *  Gets the backupOne attribute of the ScheduledHarvest object
	 *
	 * @return    The backupOne value
	 */
	public String getBackupOne() {
		prtln("getBackupOne(): " + backupOne);
		return backupOne;
	}


	/**
	 *  Gets the zipLatest attribute of the ScheduledHarvest object
	 *
	 * @return    The zipLatest value
	 */
	public String getZipLatest() {
		prtln("getZipLatest(): " + zipLatest);
		return zipLatest;
	}


	/**
	 *  Gets the backupTwo attribute of the ScheduledHarvest object
	 *
	 * @return    The backupTwo value
	 */
	public String getBackupTwo() {
		return backupTwo;
	}


	/**
	 *  Gets the backupThree attribute of the ScheduledHarvest object
	 *
	 * @return    The backupThree value
	 */
	public String getBackupThree() {
		return backupThree;
	}



	/**
	 *  Gets the isZipPresent attribute of the ScheduledHarvest object
	 *
	 * @return    The isZipPresent value
	 */
	public boolean getIsZipPresent() {
		return isZipPresent;
	}


	/**
	 *  Gets the repositoryName attribute of the ScheduledHarvest object
	 *
	 * @return    The repositoryName value
	 */
	public String getRepositoryName() {
		return repositoryName;
	}


	/**
	 *  RepositoryName with quotes escaped for use in JavaScript literals.
	 *
	 * @return    The repositoryNameEscaped value
	 */
	public String getRepositoryNameEscaped() {
		if (repositoryName == null) {
			return null;
		}
		return repositoryName.replaceAll("\\\'", "\\\\'");
	}


	/**
	 *  Gets the harvestDir attribute of the ScheduledHarvest object
	 *
	 * @return    The harvestDir value
	 */
	public File getHarvestDir() {
		return harvestDir;
	}


	/**
	 *  Gets the splitBySet attribute of the ScheduledHarvest object
	 *
	 * @return    The splitBySet value
	 */
	public boolean getSplitBySet() {
		return this.splitBySet;
	}


	/**
	 *  Gets the baseURL attribute of the ScheduledHarvest object
	 *
	 * @return    The baseURL value
	 */
	public String getBaseURL() {
		return baseURL;
	}
	
	/**
	 *  Determine if the harvested files should be zipped
	 *
	 * @return    True if the harvested files should be zipped
	 */
	public boolean getDoZipResult() {
		return doZip;
	}	


	/**
	 *  Gets the setSpec attribute of the ScheduledHarvest object
	 *
	 * @return    The setSpec value
	 */
	public String getSetSpec() {
		return setSpec;
	}


	/**
	 *  Gets the setSpecHtml attribute of the ScheduledHarvest object
	 *
	 * @return    The setSpecHtml value
	 */
	public String getSetSpecHtml() {
		if (setSpec == null || setSpec.length() == 0) {
			return "&nbsp;";
		}
		return setSpec;
	}


	/**
	 *  Gets the metadataPrefix attribute of the ScheduledHarvest object
	 *
	 * @return    The metadataPrefix value
	 */
	public String getMetadataPrefix() {
		return metadataPrefix;
	}


	/**
	 *  Gets the harvestingInterval attribute of the ScheduledHarvest object
	 *
	 * @return    The harvestingInterval value
	 */
	public String getHarvestingInterval() {
		return harvestingInterval;
	}


	/**
	 *  Gets the intervalGranularity attribute of the ScheduledHarvest object
	 *
	 * @return    The intervalGranularity value
	 */
	public String getIntervalGranularity() {
		return intervalGranularity;
	}


	/**
	 *  Gets the intervalGranularityLabel attribute of the ScheduledHarvest object
	 *
	 * @return    The intervalGranularityLabel value
	 */
	public String getIntervalGranularityLabel() {
		if (harvestingInterval != null && intervalGranularity != null && harvestingInterval.equals("1")) {
			return intervalGranularity.substring(0, intervalGranularity.length() - 1);
		}
		else {
			return intervalGranularity;
		}
	}


	/**
	 *  Gets the enabledDisabled attribute of the ScheduledHarvest object
	 *
	 * @return    The enabledDisabled value
	 */
	public String getEnabledDisabled() {
		return enabledDisabled;
	}


	/**
	 *  Gets the lastHarvestTime attribute of the ScheduledHarvest object
	 *
	 * @return    The lastHarvestTime value
	 */
	public Date getLastHarvestTime() {
		if (lastHarvestTime == null)
			prtln("getLastHarvestTime(): null");
		else
			prtln("getLastHarvestTime(): " + lastHarvestTime);
		return lastHarvestTime;
	}


	/**
	 *  Gets the harvestAll attribute of the ScheduledHarvest object
	 *
	 * @return    The harvestAll value
	 */
	public boolean getHarvestAll() {
		return harvestAll;
	}


	/**
	 *  Gets the time of day this scheduled harvest is set to run, for display, for example 1:00 PM.
	 *
	 * @return    The time for display, or null if not set
	 */
	public String getRunAtTimeDisplay() {
		try {
			if (runAtTime == null || runAtTime.trim().length() == 0)
				return null;
			else
				return new SimpleDateFormat("h:mm a zzz").format(ScheduledHarvestManager.getHarvestStartDate(runAtTime));
		} catch (Throwable t) {
			prtlnErr("getRunAtTimeDisplay(): " + t);
			return null;
		}
	}


	/**
	 *  Gets the time of day to begin the regular harvests in 24 hour time, for example 23:15.
	 *
	 * @return    The hour and minute in 24 hour time, for example 23:15, or null to schedule immediately
	 */
	public String getRunAtTime() {
		if (runAtTime != null && runAtTime.trim().length() == 0)
			return null;
		else
			return runAtTime;
	}

	// ---- Setters ----

	/**
	 *  Sets the time of day to begin the regular harvests, in 24 hour time for example 23:15.
	 *
	 * @param  runAtTime  The hour and minute in 24 hour time, for example 23:15
	 */
	public void setRunAtTime(String runAtTime) {
		prtln("setRunAtTime(): " + runAtTime);
		this.runAtTime = runAtTime;
	}


	/**
	 *  Sets the harvestAll attribute of the ScheduledHarvest object
	 *
	 * @param  h  The new harvestAll value
	 */
	public void setHarvestAll(boolean h) {
		prtln("setHarvestAll(): " + h);
		harvestAll = h;
	}


	/**
	 *  Sets the allowDupDir attribute of the ScheduledHarvest object
	 *
	 * @param  v  The new allowDupDir value
	 */
	public void setAllowDupDir(String v) {
		allowDupDir = v;
	}


	/**
	 *  Sets the numHarvestedLast attribute of the ScheduledHarvest object
	 *
	 * @param  n  The new numHarvestedLast value
	 */
	public void setNumHarvestedLast(int n) {
		this.numHarvestedLast = n;

		prtln("numHarvested Last : " + numHarvestedLast);
		prtln("maxR : " + maxR);
		if (numHarvestedLast >= maxR) {
			setWarnR(true);
		}
		else {
			setWarnR(false);
		}
	}


	/**
	 *  Sets the uid attribute of the ScheduledHarvest object
	 *
	 * @param  val  The new uid value
	 */
	public void setUid(Long val) {
		this.uid = val;
	}



	/**
	 *  Sets the defaultDir attribute of the ScheduledHarvest object
	 *
	 * @param  def  The new defaultDir value
	 */
	public void setDefaultDir(boolean def) {
		this.defaultDir = def;
	}


	/**
	 *  Sets the backupOne attribute of the ScheduledHarvest object
	 *
	 * @param  bOne  The new backupOne value
	 */
	public void setBackupOne(String bOne) {
		this.backupOne = bOne;
	}



	/**
	 *  Sets the zipLatest attribute of the ScheduledHarvest object
	 *
	 * @param  z  The new zipLatest value
	 */
	public void setZipLatest(String z) {
		this.zipLatest = z;
	}


	/**
	 *  Sets the backupTwo attribute of the ScheduledHarvest object
	 *
	 * @param  bTwo  The new backupTwo value
	 */
	public void setBackupTwo(String bTwo) {
		this.backupTwo = bTwo;
	}


	/**
	 *  Sets the backupThree attribute of the ScheduledHarvest object
	 *
	 * @param  bThree  The new backupThree value
	 */
	public void setBackupThree(String bThree) {
		this.backupThree = bThree;
	}



	/**
	 *  Sets the isZipPresent attribute of the ScheduledHarvest object
	 *
	 * @param  isZip  The new isZipPresent value
	 */
	public void setIsZipPresent(boolean isZip) {
		this.isZipPresent = isZip;
	}


	/**
	 *  Sets the warnR attribute of the ScheduledHarvest object
	 *
	 * @param  w  The new warnR value
	 */
	public void setWarnR(boolean w) {
		warnR = w;
	}


	/**
	 *  Sets the harvestDir attribute of the ScheduledHarvest object
	 *
	 * @param  hd  The new harvestDir value
	 */
	public void setHarvestDir(File hd) {

		this.harvestDir = hd;
	}


	/**
	 *  Sets the splitBySet attribute of the ScheduledHarvest object
	 *
	 * @param  spl  The new splitBySet value
	 */
	public void setSplitBySet(boolean spl) {
		this.splitBySet = spl;
	}


	/**
	 *  Sets the repositoryName attribute of the ScheduledHarvest object
	 *
	 * @param  val  The new repositoryName value
	 */
	public void setRepositoryName(String val) {
		this.repositoryName = val;
	}


	/**
	 *  Sets the baseURL attribute of the ScheduledHarvest object
	 *
	 * @param  val  The new baseURL value
	 */
	public void setBaseURL(String val) {
		this.baseURL = val;
	}


	/**
	 *  Sets the setSpec attribute of the ScheduledHarvest object
	 *
	 * @param  val  The new setSpec value
	 */
	public void setSetSpec(String val) {
		this.setSpec = val;
	}


	/**
	 *  Sets the metadataPrefix attribute of the ScheduledHarvest object
	 *
	 * @param  val  The new metadataPrefix value
	 */
	public void setMetadataPrefix(String val) {
		this.metadataPrefix = val;
	}


	/**
	 *  Sets the harvestingInterval attribute of the ScheduledHarvest object
	 *
	 * @param  val  The new harvestingInterval value
	 */
	public void setHarvestingInterval(String val) {
		this.harvestingInterval = val;
	}


	/**
	 *  Sets the intervalGranularity attribute of the ScheduledHarvest object
	 *
	 * @param  val  The new intervalGranularity value
	 */
	public void setIntervalGranularity(String val) {
		this.intervalGranularity = val;
	}


	/**
	 *  Sets the enabledDisabled attribute of the ScheduledHarvest object
	 *
	 * @param  val  The new enabledDisabled value
	 */
	public void setEnabledDisabled(String val) {
		this.enabledDisabled = val;
	}


	/**
	 *  Sets the lastHarvestTime attribute of the ScheduledHarvest object
	 *
	 * @param  val  The new lastHarvestTime value
	 */
	public void setLastHarvestTime(Date val) {
		prtln("setLastHarvestTime(): " + val);
		lastHarvestTime = val;
	}


	// ------------------ Comparator and toString methods:	------------------

	/**
	 *  Provides a String representataion for this ScheduledHarvest. This method may be used for debugging to see
	 *  what is in the ScheduledHarvest. This method is also used it the {@link #equals(Object)} method.
	 *
	 * @return    String describing all data in the SetInfo.
	 */
	public String toString() {
		StringBuffer ret =
			new StringBuffer(
			"\n Repository name: " + repositoryName +
			"\n setSpec: " + setSpec +
			"\n baseURL: " + baseURL +
			"\n metadataPrefix: " + metadataPrefix +
			"\n enabled status: " + enabledDisabled +
			"\n harvestingInterval: " + harvestingInterval +
			"\n intervalGranularity: " + intervalGranularity
			);
		return ret.toString();
	}


	/**
	 *  Checks equality of two ScheduledHarvest objects.
	 *
	 * @param  o  The ScheduledHarvest to compare to this
	 * @return    True iff the compared object is equal
	 */
	public boolean equals(Object o) {
		if (o == null || !(o instanceof ScheduledHarvest)) {
			return false;
		}
		try {
			return this.toString().equals(o.toString());
		} catch (Throwable e) {
			// Catch null pointer...
			return false;
		}
	}


	/**
	 *  Compare for sorting purposes
	 *
	 * @param  o  The object to compare to.
	 * @return    -1, 0 or 1.
	 */
	public int compareTo(Object o) {
		return this.toString().toLowerCase().compareTo(o.toString().toLowerCase());
	}


	// -------------------- Utility methods -------------------

	/**
	 *  Return a string for the current time and date, sutiable for display in log files and output to standout:
	 *
	 * @return    The dateStamp value
	 */
	public static String getDateStamp() {
		return
			new SimpleDateFormat("MMM d, yyyy h:mm:ss a zzz").format(new Date());
	}


	/**
	 *  Output a line of text to error out, with datestamp.
	 *
	 * @param  s  The text that will be output to error out.
	 */
	private final void prtlnErr(String s) {
		System.err.println(getDateStamp() + " ScheduledHarvest " + s);
	}



	/**
	 *  Output a line of text to standard out, with datestamp, if debug is set to true.
	 *
	 * @param  s  The String that will be output.
	 */
	private final void prtln(String s) {
		if (debug) {
			System.out.println(getDateStamp() + " ScheduledHarvest " + s);
		}
	}


	/**
	 *  Sets the debug attribute object
	 *
	 * @param  db  The new debug value
	 */
	public static void setDebug(boolean db) {
		debug = db;
	}

}

