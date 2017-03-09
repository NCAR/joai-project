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
package org.dlese.dpc.index.reader;

import org.apache.lucene.document.*;
import org.dlese.dpc.index.writer.*;
import org.dlese.dpc.index.*;
import org.dlese.dpc.xml.*;
import org.dlese.dpc.webapps.tools.*;
import org.dlese.dpc.util.*;
import org.dlese.dpc.oai.OAIUtils;
import org.dlese.dpc.index.document.DateFieldTools;

import javax.servlet.*;
import java.io.*;
import java.text.*;
import java.util.*;

/**
 *  A bean for accessing the data stored in a Lucene {@link org.apache.lucene.document.Document} that logs a
 *  single OAI harvest. The index writer that is responsible for creating this type of Lucene {@link
 *  org.apache.lucene.document.Document} is a {@link org.dlese.dpc.index.writer.HarvestLogWriter}.
 *
 * @author     John Weatherley
 * @see        org.dlese.dpc.index.ResultDoc
 * @see        org.dlese.dpc.index.writer.HarvestLogWriter
 */
public class HarvestLogReader extends DocReader {
	private final static String DEFAULT = "(null)";


	/**  Init method does nothing. */
	public void init() { }

	// ----------------- Data access methods ----------------------

	/**
	 *  Gets the logDate attribute of the HarvestLogReader object
	 *
	 * @return    The logDate value
	 */
	public String getLogDate() {
		String t = doc.get("logdate");

		if (t == null)
			return DEFAULT;
		long time = -1;
		try {
			time = DateFieldTools.stringToTime(t);
		} catch (ParseException pe) {
			prtlnErr("Error in getLogDate(): " + pe);
		}			

		return new SimpleDateFormat("h:mm:ss a zzz, EEE MMM d, yyyy").format(new Date(time));
	}

	/**
	 *  Gets the start time as a Date, or null.
	 *
	 * @return    The startTime value
	 */
	public Date getStartDate() {
		String t = doc.get("starttime");

		if (t == null || t == "-1")
			return null;
		try {
			return DateFieldTools.stringToDate(t);
		} catch (ParseException pe) {
			prtlnErr("Error in getStartDate(): " + pe);
			return new Date(0);
		}			
	}
	
	
	/**
	 *  Gets the startTime attribute of the HarvestLogReader object
	 *
	 * @return    The startTime value
	 */
	public String getStartTime() {
		String t = doc.get("starttime");

		if (t == null)
			return DEFAULT;
		
		long time = -1;
		try {
			time = DateFieldTools.stringToTime(t);
		} catch (ParseException pe) {
			prtlnErr("Error in getStartTime(): " + pe);
		}				
		if (time <= 0)
			return "Unknown";
		return new SimpleDateFormat("h:mm:ss a zzz, EEE MMM d, yyyy").format(new Date(time));
	}


	/**
	 *  Gets the start time as a long, or -1 if not available.
	 *
	 * @return    The start time as a long
	 */
	public long getStartTimeLong() {
		String t = doc.get("starttime");

		if (t == null)
			return -1;
		try {
			return DateFieldTools.stringToTime(t);
		} catch (ParseException pe) {
			prtlnErr("Error in getStartTimeLong(): " + pe);
			return -1;
		}			
	}

	/**
	 *  Gets the end time as a Date, or null.
	 *
	 * @return    The startTime value
	 */
	public Date getEndDate() {
		String t = doc.get("endtime");

		if (t == null || t == "-1")
			return null;
		try {
			return DateFieldTools.stringToDate(t);
		} catch (ParseException pe) {
			prtlnErr("Error in getEndDate(): " + pe);
			return new Date(0);
		}		
	}
	
	
	/**
	 *  Gets the endTime attribute of the HarvestLogReader object
	 *
	 * @return    The endTime value
	 */
	public String getEndTime() {
		String t = doc.get("endtime");

		if (t == null)
			return DEFAULT;
		long time = -1;
		try {
			time = DateFieldTools.stringToTime(t);
		} catch (ParseException pe) {
			prtlnErr("Error in getEndTime(): " + pe);
		}			
		if (time <= 0)
			return "Unknown";

		return new SimpleDateFormat("h:mm:ss a zzz, EEE MMM d, yyyy").format(new Date(time));
	}


	/**
	 *  Gets the end time as a long, or -1 if not available.
	 *
	 * @return    The end time or -1
	 */
	public long getEndTimeLong() {
		String t = doc.get("endtime");

		if (t == null)
			return -1;
		try {
			return DateFieldTools.stringToTime(t);
		} catch (ParseException pe) {
			prtlnErr("Error in getEndTimeLong(): " + pe);
			return -1;
		}			
	}


	/**
	 *  Gets the harvest duration as a long, or -1 if not avialble.
	 *
	 * @return    The harvest duration
	 */
	public long getHarvestDurationLong() {
		long start = this.getStartTimeLong();
		long end = this.getEndTimeLong();
		if (start == -1 || end == -1)
			return -1;
		return (end - start);
	}


	/**
	 *  Gets the harvest duration for display, or null if not available.
	 *
	 * @return    The harvest duration
	 */
	public String getHarvestDuration() {
		long duration = getHarvestDurationLong();
		if (duration == -1)
			return null;
		return Utils.convertMillisecondsToTime(duration);
	}


	/**
	 *  Gets the repositoryName attribute of the HarvestLogReader object
	 *
	 * @return    The repositoryName value
	 */
	public String getRepositoryName() {
		String t = doc.get("repositoryname");

		if (t == null)
			return "";
		return t;
	}


	/**
	 *  Gets the baseUrl attribute of the HarvestLogReader object
	 *
	 * @return    The baseUrl value
	 */
	public String getBaseUrl() {
		String t = doc.get("baseurl");

		if (t == null)
			return DEFAULT;
		return t;
	}


	/**
	 *  Gets the set if one was specified, or empty String. Note that the index stores 'noset' for the set if
	 *  none was specified.
	 *
	 * @return    The setSpec or empty String
	 */
	public String getSet() {
		String t = doc.get("set");

		if (t == null || t.equals("noset"))
			return "";
		return t;
	}


	/**
	 *  Gets the from datestamp of the form yyyy-MM-ddTHH:mm:ssZ used for this harvest, or null if none was used.
	 *
	 * @return    The from datestamp or null
	 */
	public String getFromDatestamp() {
		String t = doc.get("from");

		if (t == null)
			return null;
		return t;
	}


	/**
	 *  Gets the from Date used for this harvest, or null if none was used.
	 *
	 * @return    The from Date or null
	 */
	public Date getFromDate() {
		String t = getFromDatestamp();

		if (t == null)
			return null;
		try {
			return OAIUtils.getDateFromDatestamp(t);
		} catch (Throwable te) {
			prtlnErr("getFromDate() error: " + te);
			return null;
		}
	}


	/**
	 *  Gets the until datestamp of the form yyyy-MM-ddTHH:mm:ssZ used for this harvest, or null if none was
	 *  used.
	 *
	 * @return    The until datestamp or null
	 */
	public String getUntilDatestamp() {
		String t = doc.get("until");

		if (t == null)
			return null;
		return t;
	}


	/**
	 *  Gets the until Date used for this harvest, or null if none was used.
	 *
	 * @return    The until Date or null
	 */
	public Date getUntilDate() {
		String t = getUntilDatestamp();

		if (t == null)
			return null;
		try {
			return OAIUtils.getDateFromDatestamp(t);
		} catch (Throwable te) {
			prtlnErr("getUntilDate() error: " + te);
			return null;
		}
	}


	/**
	 *  The UID of the scheduled harvest.
	 *
	 * @return    The uid value
	 */
	public String getUid() {
		String t = doc.get("uid");

		if (t == null)
			return "";
		return t;
	}


	/**
	 *  Gets the uidLong attribute of the HarvestLogReader object
	 *
	 * @return    The uidLong value
	 */
	public long getUidLong() {
		String t = doc.get("uid");

		if (t == null)
			return -1;
		long uid = -1;
		try {
			uid = Long.parseLong(t);
		} catch (NumberFormatException e) {
			return -1;
		}

		return uid;
	}


	/**
	 *  The UID of an individual harvest that was performed.
	 *
	 * @return    The harvestUid value
	 */
	public String getHarvestUid() {
		String t = doc.get("harvestuid");

		if (t == null)
			return "";
		return t;
	}


	/**
	 *  Gets the harvestUidLong attribute of the HarvestLogReader object
	 *
	 * @return    The harvestUidLong value
	 */
	public long getHarvestUidLong() {
		String t = doc.get("harvestuid");

		if (t == null)
			return -1;

		long uid = -1;
		try {
			uid = Long.parseLong(t);
		} catch (NumberFormatException e) {
			return -1;
		}
		return uid;
	}


	/**
	 *  Gets the numHarvestedRecords attribute of the HarvestLogReader object
	 *
	 * @return    The numHarvestedRecords value
	 */
	public String getNumHarvestedRecords() {
		String t = doc.get("numharvestedrecords");

		if (t == null || t.equals("-1"))
			return "Unknown";

		return t;
	}


	/**
	 *  Gets the numHarvestedRecordsInt attribute of the HarvestLogReader object
	 *
	 * @return    The numHarvestedRecordsInt value
	 */
	public int getNumHarvestedRecordsInt() {
		String t = doc.get("numharvestedrecords");

		if (t == null)
			return -1;

		int num = -1;
		try {
			num = Integer.parseInt(t);
		} catch (NumberFormatException e) {
			//System.err.println("Error parsing int: " + t);
			return -1;
		}
		return num;
	}


	/**
	 *  Gets the numResumptionTokens attribute of the HarvestLogReader object
	 *
	 * @return    The numResumptionTokens value
	 */
	public String getNumResumptionTokens() {
		String t = doc.get("numresumptiontokens");

		if (t == null || t.equals("-1"))
			return "Unknown";
		return t;
	}


	/**
	 *  Gets the numResumptionTokensInt attribute of the HarvestLogReader object
	 *
	 * @return    The numResumptionTokensInt value
	 */
	public int getNumResumptionTokensInt() {
		String t = doc.get("numresumptiontokens");

		if (t == null)
			return -1;

		int num = -1;
		try {
			num = Integer.parseInt(t);
		} catch (NumberFormatException e) {
			//System.err.println("Error parsing int: " + t);
			return -1;
		}
		return num;
	}


	/**
	 *  Gets the logMessage attribute of the HarvestLogReader object
	 *
	 * @return    The logMessage value
	 */
	public String getLogMessage() {
		String t = doc.get("message");

		if (t == null)
			return "";
		return t;
	}


	/**
	 *  Gets the harvestDir attribute of the HarvestLogReader object
	 *
	 * @return    The harvestDir value
	 */
	public String getHarvestDir() {
		String t = doc.get("harvestdir");

		if (t == null)
			return "";
		return t;
	}


	/**
	 *  Gets the path to the zip file for this harvest, or null if none was saved.
	 *
	 * @return    The zip file path or null
	 */
	public String getZipFilePath() {
		return doc.get("zipfile");
	}


	/**
	 *  Gets the level of support for deleted records of the data provider, or null if not available.
	 *
	 * @return    One of 'no', 'persistent', 'transient' or null
	 */
	public String getDeletedRecordSupport() {
		return doc.get("deletedrecordsupport");
	}


	/**
	 *  Gets the date granularity supported by the data provider, or null if not available.
	 *
	 * @return    One of 'days', 'seconds'or null
	 */
	public String getSupportedGranularity() {
		return doc.get("supportedgranularity");
	}


	/**
	 *  Gets the OAI error code that was returned by the data provider, for example 'noRecordsMatch', or null if
	 *  not applicable. See <a href="http://www.openarchives.org/OAI/2.0/openarchivesprotocol.htm#ErrorConditions">
	 *  OAI-PMH error codes</a> .
	 *
	 * @return    The OAI error code or null
	 */
	public String getOaiErrorCode() {
		return doc.get("oaierrcode");
	}


	/* 	public String getError(){
		String t = doc.get("error");
		if (t == null)
			return "";
		return t;
	}
	public String getOaiError(){
		String t = doc.get("oaierror");
		if (t == null)
			return "";
		return t;
	}
	public String getOaiErrorMsg(){
		String t = doc.get("oaierrormsg");
		if (t == null)
			return "";
		return t;
	}
	public String getStatusMsg(){
		String t = doc.get("status");
		if (t == null)
			return "";
		return t;
	} */
	/**
	 *  Gets the entryType attribute of the HarvestLogReader object
	 *
	 * @return    The entryType value
	 */
	public String getEntryType() {
		String t = doc.get("entrytype");

		if (t == null)
			return "";
		return t;
	}

	// --------------- Set up methods ------------------------

	/**  Constructor for the HarvestLogReader object */
	public HarvestLogReader() { }


	/**
	 *  Constructor that may be used programatically to wrap a reader around a Lucene {@link
	 *  org.apache.lucene.document.Document} created by a {@link org.dlese.dpc.index.writer.DocWriter}. Sets the
	 *  score to 0.
	 *
	 * @param  doc  A Lucene {@link org.apache.lucene.document.Document} created by a {@link
	 *      org.dlese.dpc.index.writer.DocWriter}.
	 */
	public HarvestLogReader(Document doc) {
		super(doc);
	}



	/**
	 *  Gets a String describing the reader type. This may be used in (Struts) beans to determine which type of
	 *  reader is available for a given search result and thus what data is available for display in the UI. The
	 *  reader type implies which getter methods are available.
	 *
	 * @return    The readerType value.
	 */
	public String getReaderType() {
		return "HarvestLogReader";
	}


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
		System.err.println(getDateStamp() + " HarvestLogReader error: " + s);
	}

}


