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
import org.dlese.dpc.util.Utils;
import org.dom4j.*;

public class ExportReport extends Report {
	
	protected static boolean debug = true;
	// private HashMap props;
	
	public ExportReport (DcsSetInfo dcsSetInfo, String [] statuses) {
		super (dcsSetInfo, statuses);
	}

	public ExportReport (String name, String collection) {
	    super (name, collection);
	}

	public String getNumToExport () {
		return getProp ("numToExport");
	}
	
	public String getNumExported () {
		return getProp ("numExported");
	}
	
	public String getNumNotExported () {
		return getProp ("numNotExported");
	}
	
	public String getNumDeleted () {
		return getProp ("numDeleted");
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

}
	
