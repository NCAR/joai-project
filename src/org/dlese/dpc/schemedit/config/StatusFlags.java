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
package org.dlese.dpc.schemedit.config;

import org.dlese.dpc.schemedit.dcs.DcsDataRecord;
import java.util.*;
import java.io.Serializable;
import java.util.regex.*;
import org.dlese.dpc.util.strings.FindAndReplace;

/**
 *  Hold a tuple of status value and accompanying decription.
 *
 *@author    ostwald<p>
 *
 *      $Id $
 */
public class StatusFlags implements Serializable {

	private static boolean debug = true;

	public final static String DCS_SPECIAL = "DCS Special";
	
	/**
	 *  Description of the Field
	 */
	public final static String UNKNOWN_STATUS = "Unknown";
	final static String UNKNOWN_STATUS_DESC = "The record has not yet been assigned a status.";

	/**
	 *  Description of the Field
	 */
	public final static String NEW_STATUS = "New";
	final static String NEW_STATUS_DESC = "The metadata record has been added to catalog a resource.";

	/**
	 *  Description of the Field
	 */
	public final static String IMPORTED_STATUS = "Imported";
	final static String IMPORTED_STATUS_DESC = "The metadata record has been added and needs to have a status assigned.";

	public final static String NDR_IMPORTED_STATUS = "Imported from NDR";
	public final static String NDR_EXPORTED_STATUS = "Exported to NDR";

	public final static String RECOMMENDED_STATUS = "Recommended";
	final static String RECOMMENDED_STATUS_DESC = "The metadata record has been recommended for inclusion.";
	/**
	 *  Description of the Field
	 */
	public final static String DEFAULT_FINAL_STATUS = "Final Status";
	private final static String FINAL_STATUS_DESC = "The metadata record is complete. ";

	private final static String FINAL_STATUS_VALUE_TEMPLATE = "_|-final--|_";
	private final static Pattern FINAL_STATUS_PATTERN = Pattern.compile("_\\|-final-([^\\s]+)-\\|_");


	/**
	 *  Description of the Method
	 *
	 *@return    Description of the Return Value
	 */
	public final static List reservedStatusLabels() {
		List list = new ArrayList();
		list.add(UNKNOWN_STATUS);
		list.add(NEW_STATUS);
		list.add(IMPORTED_STATUS);
		list.add (NDR_IMPORTED_STATUS);
		list.add (NDR_EXPORTED_STATUS);
		list.add(RECOMMENDED_STATUS);
		list.add(DEFAULT_FINAL_STATUS);
		return list;
	}


	/**
	 *  Description of the Field
	 */
	public final static StatusFlag UNKNOWN_STATUS_FLAG =
			new StatusFlag(UNKNOWN_STATUS, UNKNOWN_STATUS, UNKNOWN_STATUS_DESC);

	/**
	 *  Description of the Field
	 */
	public final static StatusFlag NEW_STATUS_FLAG =
			new StatusFlag(NEW_STATUS, NEW_STATUS, NEW_STATUS_DESC);

	/**
	 *  Description of the Field
	 */
	public final static StatusFlag IMPORTED_STATUS_FLAG =
			new StatusFlag(IMPORTED_STATUS, IMPORTED_STATUS, IMPORTED_STATUS_DESC);

	public final static StatusFlag RECOMMENDED_STATUS_FLAG =
			new StatusFlag(RECOMMENDED_STATUS, RECOMMENDED_STATUS, RECOMMENDED_STATUS_DESC);
			
	/**
	 *  Creates a final status value string based on given collection key.<p>
	 *
	 *  The status value is created by inserting the collection key into the
	 *  FINAL_STATUS_VALUE_TEMPLATE. For example, for the collection key, "dcc",
	 *  the final status value would be "_|-final-dcc-|_" "
	 *
	 *@param  collection  Description of the Parameter
	 *@return             The finalStatusValue value
	 */
	public final static String getFinalStatusValue(String collection) {
		return FindAndReplace.replace(FINAL_STATUS_VALUE_TEMPLATE, "--", "-" + collection + "-", false);
	}


	/**
	 *  Gets the finalStatusFlag attribute of the StatusFlags class
	 *
	 *@param  label       Description of the Parameter
	 *@param  collection  Description of the Parameter
	 *@return             The finalStatusFlag value
	 */
	public final static StatusFlag getFinalStatusFlag(String label, String collection) {
		String value = getFinalStatusValue(collection);
		return new StatusFlag(label, value, FINAL_STATUS_DESC);
	}


	/**
	 *  Derives a collection key from a final status value string.
	 *
	 *@param  statusValue  Description of the Parameter
	 *@return              The collection value
	 */
	public final static String getCollection(String statusValue) {
		Matcher m = FINAL_STATUS_PATTERN.matcher(statusValue);
		if (m.find()) {
			return m.group(1);
		}
		else {
			return "";
		}
	}


	/**
	 *  Determines whether the given string is a final status value, and therefore provides
	 a means of identifying "final status flags"
	 *
	 *@param  s  Description of the Parameter
	 *@return    true if a collection key can be derived from the given string.
	 */
	public final static boolean isFinalStatusValue(String s) {
		String collection = getCollection(s);
		// prtln ("  " + s + " --> *" + collection + "*");
		return (collection != null && collection.trim().length() > 0);
	}


	/**
	 *  The main program for the StatusFlags class
	 *
	 *@param  args  The command line arguments
	 */
	public static void main(String[] args) {
		// String s = "_|-final-|_";
		String s = "dcc";
		if (args.length > 0) {
			s = args[0];
		}

		String key = "dcc";
		prtln("final status value for \'" + key + "\' is " + getFinalStatusValue(key));

		String sv = getFinalStatusValue(s);
		if (!isFinalStatusValue(sv)) {
			prtln(sv + " is not a final status value??");
		}
		prtln("collection: " + getCollection(sv));

		String bog = "_|-finalfoo-|_";
		if (isFinalStatusValue(bog)) {
			prtln(bog + " is a final status value??");
		}
	}


	/**
	 *  Description of the Method
	 *
	 *@param  s  Description of the Parameter
	 */
	private static void prtln(String s) {
		System.out.println(s);
	}

}

