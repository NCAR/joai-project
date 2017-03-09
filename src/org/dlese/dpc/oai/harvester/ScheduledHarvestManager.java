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
package org.dlese.dpc.oai.harvester;

import java.io.*;
import java.util.*;
import java.text.*;
import org.dlese.dpc.oai.*;
import org.dlese.dpc.index.*;
import org.dlese.dpc.index.writer.*;
import org.dlese.dpc.index.reader.*;
import org.dlese.dpc.oai.harvester.structs.*;
import org.dlese.dpc.datamgr.*;
import org.apache.lucene.document.*;
import org.dlese.dpc.util.*;
import java.util.zip.*;

/**
 *  Runs, logs and manages ScheduledHarvests.
 *
 * @author     John Weatherley
 * @version    $Id: ScheduledHarvestManager.java,v 1.23.2.3 2013/05/27 04:05:39 jweather Exp $
 * @created    January 13, 2006
 */
public final class ScheduledHarvestManager {
	private static boolean debug = true;

	private File harvestDir = null;
	private SimpleDataStore ds = null;
	private SimpleLuceneIndex harvestLogIndex = null;
	private int timeOutMilliseconds = 240000;


	/**
	 *  Constructor for the ScheduledHarvestManager
	 *
	 * @param  dataStore            The data store that will hold persistent data for this manager.
	 * @param  initialHarvestDir    The initial directory where harvested files will be saved.
	 * @param  harvestLogIndex      The harvest log index used to log harvests.
	 * @param  timeOutMilliseconds  Number of milliseconds the harvester will wait for a response from the data
	 *      provider before timing out
	 */
	public ScheduledHarvestManager(SimpleDataStore dataStore, File initialHarvestDir, SimpleLuceneIndex harvestLogIndex, int timeOutMilliseconds) {
		this.ds = dataStore;
		this.harvestLogIndex = harvestLogIndex;
		this.timeOutMilliseconds = timeOutMilliseconds;

		File dir = (File) ds.get(Keys.SH_HARVESTED_DATA_DIR);
		if (dir == null) {
			this.harvestDir = initialHarvestDir;
		}
		else {
			this.harvestDir = dir;
		}

		removeInProgresstLogEntries();
		Hashtable shs = getScheduledHarvests();
		if (shs != null) {
			Enumeration scheduled = shs.keys();
			while (scheduled.hasMoreElements()) {
				startTimerThread((Long) scheduled.nextElement());
			}
		}
	}


	/**
	 *  Removes log entries showing entrytype status inprogress and replaces them with an error message
	 *  indicateing that the given harvest was terminated by a server shut-down or crash. This method should only
	 *  be called once upon server initialization.
	 */
	public void removeInProgresstLogEntries() {
		//prtln("removeInProgresstLogEntries()");
		if (harvestLogIndex == null) {
			return;
		}

		ResultDocList harvestsInProgress =
			harvestLogIndex.searchDocs("entrytype:inprogress");

		if (harvestsInProgress == null || harvestsInProgress.size() == 0) {
			return;
		}

		Document[] updateDocs = new Document[harvestsInProgress.size()];

		for (int i = 0; i < harvestsInProgress.size(); i++) {

			HarvestLogReader reader = (HarvestLogReader) ((ResultDoc) harvestsInProgress.get(i)).getDocReader();

			HarvestLogWriter writer = new HarvestLogWriter(
				reader.getRepositoryName(),
				reader.getBaseUrl(),
				reader.getSet(),
				reader.getUid());

			updateDocs[i] = writer.logEntry(
				reader.getHarvestUidLong(),
				reader.getStartTimeLong(),
				-1,
				reader.getNumHarvestedRecordsInt(),
				reader.getNumResumptionTokensInt(),
				HarvestLogWriter.COMPLETED_SERIOUS_ERROR,
				reader.getHarvestDir(),
				reader.getZipFilePath(),
				reader.getSupportedGranularity(),
				reader.getDeletedRecordSupport(),
				reader.getOaiErrorCode(),
				"This harvest was terminated prematurely by a server shut-down or crash." +
				" The last message logged was: " + reader.getLogMessage());
		}

		harvestLogIndex.update("entrytype", "inprogress", updateDocs, true);
	}


	/**
	 *  Sets the harvestDir attribute of the ScheduledHarvestManager object
	 *
	 * @param  harvestDir  The new harvestDir value
	 */
	public void setHarvestDir(File harvestDir) {
		this.harvestDir = harvestDir;
		ds.put(Keys.SH_HARVESTED_DATA_DIR, harvestDir);
	}


	/**
	 *  Gets the harvestDir attribute of the ScheduledHarvestManager object
	 *
	 * @return    The harvestDir value
	 */
	public File getHarvestDir() {
		return harvestDir;
	}



	// ------------------ Handle running the harvests ------------------

	/**
	 *  Determines wheter the given ScheduledHarvest is currently running.
	 *
	 * @param  shUid  ScheduledHarvest ID.
	 * @return        True if running.
	 */
	public boolean isRunning(Long shUid) {
		synchronized (this) {
			return runningHarvests.containsKey(shUid);
		}
	}



	/**
	 *  Gets all scheduledHarvests in this this manager.
	 *
	 * @return    The scheduledHarvests
	 */
	public Hashtable getScheduledHarvests() {
		return (Hashtable) ds.get(Keys.SCHEDULED_HARVESTS);
	}



	/**
	 *  Gets the scheduledHarvest, or null if none exists.
	 *
	 * @param  shUid  The ID of the item to retrieve.
	 * @return        The scheduledHarvest value
	 */
	public ScheduledHarvest getScheduledHarvest(Long shUid) {
		Hashtable shs = getScheduledHarvests();
		if (shs == null) {
			return null;
		}
		return (ScheduledHarvest) shs.get(shUid);
	}


	/**
	 *  Adds a ScheduledHarvest to this manager or replaces an existing one with the same ID.
	 *
	 * @param  sh  The ScheduledHarvest to add.
	 */
	public void addScheduledHarvest(ScheduledHarvest sh) {
		Hashtable shs = getScheduledHarvests();
		if (shs == null) {
			shs = new Hashtable();
		}
		shs.put(sh.getUid(), sh);
		ds.put(Keys.SCHEDULED_HARVESTS, shs);
		startTimerThread(sh.getUid());
	}


	/**
	 *  Removes the given ScheduledHarvest.
	 *
	 * @param  shUid        ScheduledHarvest ID to remove
	 * @param  deletefiles  Description of the Parameter
	 */
	public void removeScheduledHarvest(Long shUid, boolean deletefiles) {
		Hashtable shs = getScheduledHarvests();
		if (shs == null) {
			return;
		}
		ScheduledHarvest sh = (ScheduledHarvest) shs.get(shUid);
		if ((sh != null) && (deletefiles == true)) {
			/*
			 *  remove the zip folder for this harvest
			 */
			File zDir = (File) ds.get(Keys.ZIP_DIR);
			String zD = zDir.toString();
			String baseurl = sh.getBaseURL();
			baseurl = (sh.getBaseURL()).substring(7);
			int position = baseurl.indexOf('/');
			if (position != -1) {
				baseurl = baseurl.substring(0, position);
			}
			String dirsname = baseurl + "-" + sh.getMetadataPrefix();
			if (sh.getSetSpec() != "") {
				dirsname += "-" + sh.getSetSpec();
			}
			dirsname = dirsname.replace(':', '-');
			dirsname = dirsname.replace('.', '-');

			String deleteZipDir = zD + File.separator + dirsname;

			File deleteZip = new File(deleteZipDir);
			prtln("deleteZip dir is " + deleteZip.toString());
			boolean success = false;
			try {
				if (deleteZip.exists()) {
					Files.deleteDirectory(deleteZip);
				}
			} catch (Exception e) {
				prtln("Couldnt delete the Zip directory");
			}
		}
		shs.remove(shUid);
		stopTimerThread(shUid);
		ds.put(Keys.SCHEDULED_HARVESTS, shs);
	}


	/**
	 *  Determines whether the given ScheduledHarvest ID is in this manager.
	 *
	 * @param  shUid  ScheduledHarvest ID
	 * @return        True if the given ScheduledHarvest ID exists in this manager.
	 */
	public boolean containsScheduledHarvest(Long shUid) {
		Hashtable shs = getScheduledHarvests();
		if (shs == null) {
			return false;
		}
		return shs.containsKey(shUid);
	}


	/**
	 *  Determine whether the given ScheduledHarvest value is in this manager.
	 *
	 * @param  sh  ScheduledHarvest
	 * @return     True if the ScheduledHarvest exists in this manager, else false.
	 */
	public boolean containsValue(ScheduledHarvest sh) {
		Hashtable shs = getScheduledHarvests();
		if (shs == null) {
			return false;
		}
		return shs.containsValue(sh);
	}


	Hashtable runningHarvests = new Hashtable();
	Hashtable harvestTimers = new Hashtable();


	/**
	 *  Harvests the given ScheduledHarvest immediately, if not already running.
	 *
	 * @param  shUid                        Uid of the ScheduledHarvest.
	 * @param  harvestAll                   True to havest all records, false to havest only records changed
	 *      since the previous harvest.
	 * @param  harvestAllIfNoDeletedRecord  True to harvest all records from scratch if deleted records are not
	 *      supported
	 */
	public void harvestNow(Long shUid, boolean harvestAll, boolean harvestAllIfNoDeletedRecord) {
		Hashtable shs = getScheduledHarvests();
		ScheduledHarvest sh = (ScheduledHarvest) shs.get(shUid);
		if (harvestAll == true) {
			sh.setHarvestAll(true);
		}
		else {
			sh.setHarvestAll(false);
		}

		new HarvestThread(shUid, harvestAll, harvestAllIfNoDeletedRecord, true).start();
	}


	/**
	 *  Performs the harvest.
	 *
	 * @param  shUid                        The UID of the ScheduledHarvest
	 * @param  harvestAll                   True to harvest all records, false to only harvest records that have
	 *      changed since the last harvest.
	 * @param  harvestAllIfNoDeletedRecord  True to harvest all records from scratch if deleted records are not
	 *      supported
	 * @param  override                     True to perform the harvest regardless of status, false to perform
	 *      the harvest only when the status is enabled.
	 */
	private void performHarvest(Long shUid, boolean harvestAll, boolean harvestAllIfNoDeletedRecord, boolean override) {
		Hashtable shs = getScheduledHarvests();
		if (shs == null) {
			return;
		}
		ScheduledHarvest sh = (ScheduledHarvest) shs.get(shUid);
		if (sh == null ||
			(!sh.getEnabledDisabled().equalsIgnoreCase("enabled") && !override)) {
			return;
		}
		Harvester harvester;
		synchronized (this) {
			if (runningHarvests.containsKey(shUid)) {
				return;
			}
			IndexingHarvestMsgHandler handler =
				new IndexingHarvestMsgHandler(
				harvestLogIndex,
				sh.getRepositoryName(),
				sh.getBaseURL(),
				sh.getSetSpec(),
				sh.getUid().toString(),
				50, sh.getHarvestDir());
			OAIChangeListener oaiChangeListener = null;
			harvester = new Harvester(handler, oaiChangeListener, timeOutMilliseconds);
			handler.setHarvester(harvester);
			runningHarvests.put(shUid, harvester);
		}

		prtln("performHarvest() harvestAll is: " + harvestAll);

		Date from;
		if (harvestAll) {
			from = null;
		}
		else {
			from = sh.getLastHarvestTime();
		}

		prtln("performHarvest() from: " + from);

		File zDir = (File) ds.get(Keys.ZIP_DIR);
		String zD = zDir.toString();

		String baseurl = sh.getBaseURL();
		baseurl = (sh.getBaseURL()).substring(7);
		int position = baseurl.indexOf('/');

		if (position != -1) {
			baseurl = baseurl.substring(0, position);
		}

		String filesname = baseurl + "-" + sh.getMetadataPrefix();
		String setspec = sh.getSetSpec();

		if (!(setspec.equals("")) && (sh.getSetSpec() != null)) {
			filesname += "-" + sh.getSetSpec();
		}

		filesname = filesname.replace(':', '-');
		filesname = filesname.replace('.', '-');

		String dirsname = filesname;

		filesname += "-" + getZipFileDateString();

		String zipName = zD + File.separator + dirsname + File.separator + filesname;

		zD += File.separator + dirsname;

		boolean def = sh.getDefaultDir();
		String dirH = (sh.getHarvestDir()).toString();
		if (def == true) {
			dirH += File.separator + dirsname;
		}

		File dH = new File(dirH);

		Date harvestTime = doHarvest(
			harvester,
			sh.getBaseURL(),
			sh.getMetadataPrefix(),
			sh.getSetSpec(),
			from,
			null,
			dH,
			sh.getDoZipResult(),
			zipName,
			zD,
			sh.getSplitBySet(),
			sh,
			harvestAllIfNoDeletedRecord);

		String zipthere = sh.getZipLatest();
		if (zipthere == "") {
			//prtln("SETTING IS ZIP TO FALSE");
			sh.setIsZipPresent(false);
		}
		else {
			//prtln("SETTING IS ZIP TO TRUE");
			sh.setIsZipPresent(true);
		}

		synchronized (this) {
			shs = getScheduledHarvests();
			if (harvestTime != null && shs.containsKey(shUid)) {
				sh.setLastHarvestTime(harvestTime);
				shs.put(shUid, sh);
				ds.put(Keys.SCHEDULED_HARVESTS, shs);
			}
			runningHarvests.remove(shUid);
		}
	}


	private String getZipFileDateString() {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd-'T'-HH'h'-mm'm'-ss's'-SSS'ms'");
		return sdf.format(new Date());
	}


	/**
	 *  Determines whether a given ScheduledHarvest is already running.
	 *
	 * @param  sh  The ScheduledHarvest
	 * @return     True if already running, else false.
	 */
	public boolean isRunningOneTimeHarvest(ScheduledHarvest sh) {
		synchronized (this) {
			return runningOneTimeHarvests.containsKey(sh.toString());
		}
	}


	Hashtable runningOneTimeHarvests = new Hashtable();


	/**
	 *  Executes the one-time-harvest.
	 *
	 * @param  sh                           A ScheduledHarvest with appropriate settings.
	 * @param  from                         From Date
	 * @param  until                        Until Date
	 * @param  harvestDir                   Directory where harvested records will be saved.
	 * @param  harvestAllIfNoDeletedRecord  NOT YET DOCUMENTED
	 */
	private void performOneTimeHarvest(ScheduledHarvest sh, Date from, Date until, File harvestDir,
	                                   boolean harvestAllIfNoDeletedRecord) {
		Harvester harvester;
		synchronized (this) {
			if (runningOneTimeHarvests.containsKey(sh.toString())) {
				return;
			}
			IndexingHarvestMsgHandler handler =
				new IndexingHarvestMsgHandler(
				harvestLogIndex,
				sh.getRepositoryName(),
				sh.getBaseURL(),
				sh.getSetSpec(),
				sh.getUid().toString(),
				50, sh.getHarvestDir());
			OAIChangeListener oaiChangeListener = null;
			harvester = new Harvester(handler, oaiChangeListener, timeOutMilliseconds);
			handler.setHarvester(harvester);
			runningOneTimeHarvests.put(sh.toString(), harvester);
		}

		File zDir = (File) ds.get(Keys.ZIP_DIR);
		String zD = zDir.toString();

		String baseurl = sh.getBaseURL();
		baseurl = (sh.getBaseURL()).substring(7);
		int position = baseurl.indexOf('/');

		if (position != -1) {
			baseurl = baseurl.substring(0, position);
		}
		String filesname = baseurl + "-" + sh.getMetadataPrefix();

		String setspec = sh.getSetSpec();
		if (!(setspec.equals("")) && (sh.getSetSpec() != null)) {
			filesname += "-" + sh.getSetSpec();
		}

		filesname = filesname.replace(':', '-');
		filesname = filesname.replace('.', '-');

		String dirsname = filesname;

		filesname += "-" + getZipFileDateString();

		String zipName = zD + File.separator + dirsname + File.separator + filesname;

		zD += File.separator + dirsname;

		boolean def = sh.getDefaultDir();
		String dirH = (sh.getHarvestDir()).toString();
		if (def == true) {
			dirH += File.separator + dirsname;
		}

		File dH = new File(dirH);

		doHarvest(harvester,
			sh.getBaseURL(),
			sh.getMetadataPrefix(),
			sh.getSetSpec(),
			from,
			until,
			dH,
			sh.getDoZipResult(),
			zipName,
			zD,
			sh.getSplitBySet(),
			sh,
			harvestAllIfNoDeletedRecord);

		boolean isZipDir = new File(zD).exists();
		if (isZipDir) {
			File zdirec = new File(zD);
			int totalzips = Count(zdirec);
			if (totalzips >= 1) {
				//prtln("SETTING IS ZIP TO TRUE");
				sh.setIsZipPresent(true);
			}
			else {
				//prtln("SETTING IS ZIP TO FALSE");
				sh.setIsZipPresent(false);
			}
		}
		else {
			//prtln("SETTING IS ZIP TO FALSE");
			sh.setIsZipPresent(false);
		}

		synchronized (this) {
			runningOneTimeHarvests.remove(sh.toString());
		}
	}


	/**
	 *  Performs a one-time only harvest in the background.
	 *
	 * @param  sh                           A ScheduledHarvest with appropriate settings.
	 * @param  from                         From Date
	 * @param  until                        Until Date
	 * @param  harvestDir                   Directory where harvested records will be saved.
	 * @param  harvestAllIfNoDeletedRecord  True to harvest all records from scratch if deleted records are not
	 *      supported
	 */
	public void oneTimeHarvest(ScheduledHarvest sh, Date from, Date until, File harvestDir, boolean harvestAllIfNoDeletedRecord) {
		new OneTimeHarvestThread(sh, from, until, harvestDir, harvestAllIfNoDeletedRecord).start();
	}


	/**
	 *  Performs the scheduled harvest.
	 *
	 * @param  from                         From time, or null for none.
	 * @param  until                        Until time, or null for none.
	 * @param  harvestDir                   Directory where files are saved.
	 * @param  harvester                    The harvester to use
	 * @param  baseURL                      base URL
	 * @param  metadataPrefix               metadataPrefix
	 * @param  set                          set
	 * @param  doZip                        True to perform zipping 
	 * @param  zipName                      Zip file name
	 * @param  splitBySet                   True to split by set
	 * @param  zDir                         Zip files directory
	 * @param  sh                           The ScheduledHarvest
	 * @param  harvestAllIfNoDeletedRecord  True to harvest all records from scratch if deleted records are not
	 *      supported
	 * @return                              Date of a succeful harvest or null if harvest did not complete
	 *      successfully.
	 */
	private Date doHarvest(
	                       Harvester harvester,
	                       String baseURL,
	                       String metadataPrefix,
	                       String set,
	                       Date from,
	                       Date until,
	                       File harvestDir,
	                       boolean doZip,
	                       String zipName,
	                       String zDir,
	                       boolean splitBySet,
	                       ScheduledHarvest sh,
	                       boolean harvestAllIfNoDeletedRecord) {
		Date harvestTime = new Date(System.currentTimeMillis());

		prtln("doHarvest() from: " + from);
		
		String zipNameUsed = zipName;
		if(!doZip)
			zipNameUsed = null;
		
		try {
			harvester.doHarvest(
				baseURL,
				metadataPrefix,
				set,
				from,
				until,
				harvestDir.getAbsolutePath(),
				splitBySet,
				zipNameUsed,
				zDir,
				false,
				sh.getHarvestAll(),
				harvestAllIfNoDeletedRecord);

			int nH = harvester.getNumRecordsHarvested();
			sh.setNumHarvestedLast(nH);

			// Save the most recent three zip files and delete any older ones:
			File zD = new File(zDir);
			File[] zipFiles = zD.listFiles(new ZipFileFilter());
			File newestfile = null;

			if (zipFiles != null && zipFiles.length > 0) {
				Arrays.sort(zipFiles, new FileModDateComparator(FileModDateComparator.OLDEST_FIRST));
				newestfile = zipFiles[zipFiles.length - 1];

				// Keep only the most three recent:
				for (int i = 0; i < zipFiles.length - 3; i++)
					zipFiles[i].delete();
			}

			// Set the name of the newest zip file, relative to the base zip directory:
			if (newestfile != null) {
				// Move the older zips down
				sh.setBackupThree(sh.getBackupTwo());
				sh.setBackupTwo(sh.getBackupOne());

				String la = zD.getParentFile().getName() + File.separator;
				String newone = newestfile.toString();
				int index = newone.indexOf(la);

				newone = newone.substring(index + la.length());

				// The latest zip and first backup are the same:
				sh.setBackupOne(newone);
				sh.setZipLatest(newone);
			}

			sh.setHarvestAll(false);

			return harvestTime;
		} catch (OAIErrorException oex) {
			// Do nothing... these messages are already passed to the msgHandler
		} catch (Hexception e) {
			//messageHandler.statusMessage("Harvest exception: " + e.getMessage());
		} catch (NullPointerException e) {
			e.printStackTrace();
			//messageHandler.statusMessage("Internal Harvester error. Exception: " + e.getMessage());
		} catch (Throwable e) {
			//messageHandler.statusMessage("Internal Harvester error. Exception: " + e.getMessage());
		}
		return null;
	}



	/**
	 *  Counts the number of files in a directory
	 *
	 * @param  dir  the directory in question
	 * @return      the total number of files in dir
	 */
	private int Count(File dir) {
		if (!dir.isDirectory()) {
			prtln("File " + dir.getName() + " is not a directory.");
			System.exit(1);
		}

		int total = 0;
		String[] files = dir.list();
		String thisDir = dir.getPath();

		// count non-directory files in this directory and recurse for each
		// that IS a directory
		for (int k = 0; k < files.length; k++) {
			File f = new File(thisDir + File.separator + files[k]);
			if (!f.isDirectory()) {
				total++;
			}
			else {
				total += Count(f);
			}
		}
		return total;
	}


	/**
	 *  Start or restarts the timer thread with the given update frequency.
	 *
	 * @param  shUid  The uid of the ScheduledHarvest.
	 */
	private void startTimerThread(Long shUid) {
		Hashtable shs = getScheduledHarvests();
		if (shs == null) {
			return;
		}
		ScheduledHarvest sh = (ScheduledHarvest) shs.get(shUid);

		if (sh == null || !sh.getEnabledDisabled().equalsIgnoreCase("enabled")) {
			return;
		}

		int interval;
		try {
			interval = Integer.parseInt(sh.getHarvestingInterval());
		} catch (NumberFormatException e) {
			prtlnErr("Invalid harvestingInterval." + e.getMessage());
			return;
		}

		long updateFrequency;
		String intervalGranularity = sh.getIntervalGranularity();
		if (intervalGranularity.equalsIgnoreCase("days")) {
			updateFrequency = interval * 1000 * 60 * 60 * 24;
		}
		else if (intervalGranularity.equalsIgnoreCase("hours")) {
			updateFrequency = interval * 1000 * 60 * 60;
		}
		else if (intervalGranularity.equalsIgnoreCase("minutes")) {
			updateFrequency = interval * 1000 * 60;
		}
		else if (intervalGranularity.equalsIgnoreCase("seconds")) {
			updateFrequency = interval * 1000;
		}
		else {
			prtlnErr("Invalid intervalGranularity: " + intervalGranularity);
			return;
		}

		// Set the time to start the harvest, if requested:
		Date runAtDate = null;
		String runAtTime = sh.getRunAtTime();
		if (runAtTime != null && runAtTime.trim().length() > 0) {
			try {
				runAtDate = getHarvestStartDate(runAtTime);
			} catch (Throwable t) {
				prtlnErr("Unable to parse start time: " + t);
			}
		}

		if (updateFrequency == 0) {
			stopTimerThread(shUid);
		}

		Timer timer = (Timer) harvestTimers.get(shUid);
		if (updateFrequency > 0) {
			if (timer != null) {
				timer.cancel();
			}

			// Set daemon to true
			timer = new Timer(true);

			TimerTask harvestTimer = new HarvestTask(shUid);

			// Normalize the update frequency:
			updateFrequency = ((updateFrequency > 0) ? updateFrequency : 60000);

			// Start the timer
			if (runAtDate == null) {
				prtln("\n\nScheduling harvest interval: " + updateFrequency);
				timer.schedule(harvestTimer, 20, updateFrequency);
			}
			else {
				prtln("\n\nScheduling harvest to begin at: " + runAtDate + " interval: " + updateFrequency);
				timer.scheduleAtFixedRate(harvestTimer, runAtDate, updateFrequency);
			}

			harvestTimers.put(shUid, timer);
			prtln("ScheduledHarvest timer started");
		}
	}


	/**  Stops all running harvests gracefully. */
	public void stopAllHarvests() {
		prtln("stopAllHarvests()");

		Hashtable shs = getScheduledHarvests();
		if (shs != null) {
			Enumeration scheduled = shs.keys();
			while (scheduled.hasMoreElements()) {
				stopTimerThread((Long) scheduled.nextElement());
			}
		}

		Harvester har;
		if (runningHarvests != null) {
			Enumeration running = runningHarvests.keys();
			while (running.hasMoreElements()) {
				har = (Harvester) runningHarvests.get(running.nextElement());
				har.kill();
			}
		}

		if (runningOneTimeHarvests != null) {
			Enumeration running = runningOneTimeHarvests.keys();
			while (running.hasMoreElements()) {
				har = (Harvester) runningOneTimeHarvests.get(running.nextElement());
				har.kill();
			}
		}

		if (harvestLogIndex != null) { // stop harvesting log thread when shutting down
			try {
				harvestLogIndex.stopIndexing();
			} catch (Exception e) {
			}
		}
	}


	/**
	 *  Stops the timer
	 *
	 * @param  shUid  The uid of the ScheduledHarvest.
	 */
	public void stopTimerThread(Long shUid) {
		Timer timer = (Timer) harvestTimers.get(shUid);

		if (timer != null) {
			timer.cancel();
			harvestTimers.remove(shUid);
			prtln("ScheduledHarvest timer stopped");
		}
	}


	/**
	 *  Gets the date and time to begin the harvest by parsing a String in 24 hour time format such as 12:24 or
	 *  23:15.
	 *
	 * @param  startTimeString     The hour and minute in 24 hour time, for example 23:15
	 * @return                     The Date the harvest will begin
	 * @exception  ParseException  If unable to parse
	 */
	public static Date getHarvestStartDate(String startTimeString) throws ParseException {
		Date startTimeDate = null;

		Date currentTime = new Date(System.currentTimeMillis());
		//Date oneHourFromNow = new Date(System.currentTimeMillis() + (1000 * 60 * 60));
		Date oneMinuteFromNow = new Date(System.currentTimeMillis() + (1000 * 60));
		// One minute from now instead...
		int dayInYear = Integer.parseInt(Utils.convertDateToString(currentTime, "D"));
		int year = Integer.parseInt(Utils.convertDateToString(currentTime, "yyyy"));

		startTimeDate = Utils.convertStringToDate(year + " " + dayInYear + " " + startTimeString, "yyyy D H:mm");

		// Make sure the timer starts one minute from now or later
		if (startTimeDate.compareTo(oneMinuteFromNow) < 0) {
			if (dayInYear == 365) {
				year++;
				dayInYear = 1;
			}
			else
				dayInYear++;
			startTimeDate = Utils.convertStringToDate(year + " " + dayInYear + " " + startTimeString, "yyyy D H:mm");
		}
		return startTimeDate;
	}



	/**
	 *  Runs the harvest in the background
	 *
	 * @author     John Weatherley
	 * @version    $Id: ScheduledHarvestManager.java,v 1.23.2.3 2013/05/27 04:05:39 jweather Exp $
	 * @created    January 13, 2006
	 */
	private class HarvestThread extends Thread {
		Long shUid;
		boolean harvestAll, override, harvestAllIfNoDeletedRecord;
		ScheduledHarvest sh = null;



		/**
		 *  Constructor for the HarvestThread object
		 *
		 * @param  shUid                        The ID of the ScheduledHarvest to run.
		 * @param  harvestAll                   True to harvest all records, false to only harvest records that have
		 *      changed since the last harvest.
		 * @param  harvestAllIfNoDeletedRecord  True to harvest all records from scratch if deleted records are not
		 *      supported
		 * @param  override                     True to perform the harvest regardless of status, false to perform
		 *      the harvest only if the status is "enabled".
		 */
		public HarvestThread(Long shUid, boolean harvestAll, boolean harvestAllIfNoDeletedRecord, boolean override) {
			this.shUid = shUid;
			this.harvestAll = harvestAll;
			this.harvestAllIfNoDeletedRecord = harvestAllIfNoDeletedRecord;
			this.override = override;
		}


		/**  Main processing method for the HarvestThread object */
		public void run() {
			setPriority(Thread.MIN_PRIORITY);
			performHarvest(shUid, harvestAll, harvestAllIfNoDeletedRecord, override);
		}
	}


	/**
	 *  Runs the harvest in the background at given intervals.
	 *
	 * @author     John Weatherley
	 * @version    $Id: ScheduledHarvestManager.java,v 1.23.2.3 2013/05/27 04:05:39 jweather Exp $
	 * @created    January 13, 2006
	 */
	private class HarvestTask extends TimerTask {
		private Long shUid;


		/**
		 *  Constructor for the HarvestTask object
		 *
		 * @param  shUid  The UID of the ScheduledHarvest.
		 */
		public HarvestTask(Long shUid) {
			this.shUid = shUid;
		}


		/**  Main processing method for the HarvestTask object */
		public void run() {

			boolean harvestAll = false;
			boolean harvestAllIfNoDeletedRecord = true;

			// For scheduled harvests, only harvest all if deletions are not supported:
			new HarvestThread(shUid, harvestAll, harvestAllIfNoDeletedRecord, false).start();

			//new auto harvest setting which would harvest all records every time.
			//new HarvestThread(shUid, true, false, false).start();
		}
	}


	/**
	 *  Runs the harvest in the background
	 *
	 * @author     John Weatherley
	 * @version    $Id: ScheduledHarvestManager.java,v 1.23.2.3 2013/05/27 04:05:39 jweather Exp $
	 * @created    January 13, 2006
	 */
	private class OneTimeHarvestThread extends Thread {
		ScheduledHarvest sh = null;
		Date from = null, until = null;
		File harvestDir;
		boolean harvestAllIfNoDeletedRecord;


		/**
		 *  Constructor for the OneTimeHarvestThread object
		 *
		 * @param  from                         From time, or null for none.
		 * @param  until                        Until time, or null for none.
		 * @param  harvestDir                   Directory where files are saved.
		 * @param  sh                           The ScheduledHarvest to run.
		 * @param  harvestAllIfNoDeletedRecord  True to harvest all records from scratch if deleted records are not
		 *      supported
		 */
		public OneTimeHarvestThread(ScheduledHarvest sh, Date from, Date until, File harvestDir, boolean harvestAllIfNoDeletedRecord) {
			this.from = from;
			this.until = until;
			this.harvestDir = harvestDir;
			this.sh = sh;
			this.harvestAllIfNoDeletedRecord = harvestAllIfNoDeletedRecord;
		}


		/**  Main processing method for the OneTimeHarvestThread object */
		public void run() {
			setPriority(Thread.MIN_PRIORITY);
			performOneTimeHarvest(sh, from, until, harvestDir, harvestAllIfNoDeletedRecord);
		}
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
		System.err.println(getDateStamp() + " ScheduledHarvestManager Error: " + s);
	}



	/**
	 *  Output a line of text to standard out, with datestamp, if debug is set to true.
	 *
	 * @param  s  The String that will be output.
	 */
	private final void prtln(String s) {
		if (debug) {
			System.out.println(getDateStamp() + " ScheduledHarvestManager: " + s);
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

