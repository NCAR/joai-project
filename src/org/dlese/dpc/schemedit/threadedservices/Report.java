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
package org.dlese.dpc.schemedit.threadedservices;

import java.util.*;
import org.dlese.dpc.schemedit.*;
import org.dlese.dpc.schemedit.dcs.*;
import org.dlese.dpc.schemedit.config.StatusFlags;
import org.dlese.dpc.util.Utils;
import org.dlese.dpc.util.strings.FindAndReplace;
import org.dom4j.*;

public class Report implements java.io.Serializable {
	
	protected static boolean debug = true;
	
	String collection = "";
	String name = "";
	Date timeStamp;
	public long processingTime = 0;
	public int recordsProcessed = 0;
	String summary = "";
	String [] statuses;
	Map props;
	List reportEntries = new ArrayList ();
	
	public Report (DcsSetInfo dcsSetInfo, String [] statuses) {
		this.name = dcsSetInfo.getName();
		this.collection = dcsSetInfo.getSetSpec();
		this.timeStamp = new Date();
		this.props = new HashMap();
		this.statuses = statuses;
	}

	public Report (String name, String collection) {
	    this.name = name;
	    this.collection = collection;
	    this.timeStamp = new Date();
		this.props = new HashMap();
		this.statuses = null;
	}
			
	public Map getProps () {
		return props;
	}
	
	public String getProp (String propName) {
		String ret = (String) props.get (propName);
		return ret == null ? "" : ret;
	}
	
	public void setProp (String propName, String propValue) {
		props.put(propName, propValue);
	}
	
	public String [] getStatuses () {
		return statuses;
	}
	
	public String getDisplayStatuses () {
		if (statuses == null || statuses.length == 0)
			return "All Statuses";
		
		String ret = "";
		for (int i=0;i<statuses.length;i++) {
			String statusValue = statuses[i];
/* 			String statusFlag;
			if (StatusFlags.isFinalStatusValue (statusValue))
				statusFlag = StatusFlags. */
			ret += statusValue;;
			if (i == (statuses.length - 2) && statuses.length > 1)
				ret += " and ";
			else if (i < (statuses.length - 2) && statuses.length > 2)
				ret += ", ";
		}
		return ret;
	}
	
	public List getEntries () {
		Collections.sort (reportEntries, new EntryComparator());
		return reportEntries;
	}
	
	public String getCollection () {
		return collection;
	}
	
	public String getName () {
		return name;
	}
	
	public Date getTimeStamp () {
		return timeStamp;
	}
	
	public String getProcessingTime () {
		return Utils.convertMillisecondsToTime (processingTime);
	}
	
	public void addEntry (DcsDataRecord dcsDataRecord) {
		reportEntries.add (new ReportEntry (dcsDataRecord));
	}
	
	public void addEntry (String id, String report) {
		reportEntries.add (new ReportEntry (id, report));
	}
	
	public int getInvalidRecCount () {
		int count = 0;
		for (Iterator i = reportEntries.iterator();i.hasNext();) {
			ReportEntry entry = (ReportEntry)i.next();
			if (entry.getIsInvalid())
				count++;
		}
		return count;
	}

	
	public String getSummary () {
		String s = "\nSummary";
		s += "\n\t" + recordsProcessed + " records processed";
		if (processingTime > 0)
			s += " in " + Utils.convertMillisecondsToTime (processingTime);
		s += "\n\t" + getInvalidRecCount() + " were invalid";
		return s;
	}
	
	public String details () {
		String s =  "\n\nDetailed Report";
		for (Iterator i = reportEntries.iterator();i.hasNext();) {
			ReportEntry entry = (ReportEntry)i.next();
			// s += "\n" + entry.id;
		}
		return s;
	}
	
	public String getReport () {
		String s = "Report: " + name;
		s += "\n\tCollection key: " + collection;
		s += getSummary();
		s += details();
		return s;
	}
	
	public class ReportEntry implements java.io.Serializable {
		
		String validationReport;
		String id;
		
		
		ReportEntry (String id, String validationReport) {
			this.id = id;
			this.validationReport = validationReport;
		}
		
		ReportEntry (DcsDataRecord dcsDataRecord) {
			this.id = dcsDataRecord.getId();
			validationReport = dcsDataRecord.getValidationReport();
		}
		
		public boolean getIsInvalid () {
			return (validationReport.trim().length() > 0);
		}
		
		public String getId () {
			return this.id;
		}
		
		/**
		* Format the validation report for browser display
		*/
		public String getValidationReport () {
			String rpt = FindAndReplace.replace (validationReport, "\n", "<br/>", false);
			String pattern = "NOT VALID:";
			if (rpt.indexOf(pattern) == 0) {
				rpt = "<b>" + pattern +"</b>" + rpt.substring(pattern.length());
				rpt = FindAndReplace.replace (rpt, "Error:", "<br/>Error:", false);
			}
			return rpt;
		}
		
		public String toString () {
			String s = "\n" + id;

			if (getIsInvalid()) {
				s += "\n\n" + validationReport;
			}
				
			return s;
		}
	}
	
	class EntryComparator implements Comparator {
	
		/**
		 *  Provide comparison for sorting Sugest a URL Records by  "lastModified" property
		 *
		 * @param  o1  document 1
		 * @param  o2  document 2
		 * @return     DESCRIPTION
		 */
		public int compare(Object o1, Object o2) {

			String id1 = ((ReportEntry) o1).getId();
			String id2 = ((ReportEntry) o2).getId();
			return id1.compareTo (id2);
		}

	}
	protected static void prtln (String s) {
		if (debug)
			System.out.println (s);
	}
}
	
