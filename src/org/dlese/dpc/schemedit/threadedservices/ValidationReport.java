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

public class ValidationReport extends Report {
	
	public ValidationReport (DcsSetInfo dcsSetInfo, String [] statuses) {
		super (dcsSetInfo, statuses);
	}
	
	public String getSummary () {
		String s = "\nValidation Summary";
		s += "\n\t" + recordsProcessed + " records processed";
		if (processingTime > 0)
			s += " in " + Utils.convertMillisecondsToTime (processingTime);
		if (getProp("numValid") != null)
			s += "\n\t" + getProp("numValid") + " were valid";
		else
			prtln ("ValidationReport.getSummary(): numValid property not found");
		s += "\n\t" + getInvalidRecCount() + " were invalid";
		return s;
	}
	
	public String getReport () {
		String s = "Validation Report: " + name;
		s += "\n\tCollection key: " + collection;
		s += getSummary();
		s += details();
		return s;
	}
}
	
