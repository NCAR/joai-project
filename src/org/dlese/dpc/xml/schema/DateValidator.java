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
package org.dlese.dpc.xml.schema;

import org.dlese.dpc.util.*;
import org.dlese.dpc.util.strings.*;
import java.util.*;
import java.text.*;

/**
 *  Functions for validating date-related schema data types.
 *
 *@author    ostwald
 */
public class DateValidator {

	private static boolean debug = false;
	static String dateFormatString = "yyyy-MM-dd'T'HH:mm:ss'Z'";


	/**
	 *  Gets the field attribute of the DateValidator class
	 *
	 *@param  fieldKey    Description of the Parameter
	 *@param  dateString  Description of the Parameter
	 *@return             The field value
	 */
	static int getField(String fieldKey, String dateString) {

		String template = FindAndReplace.replace(dateFormatString, "\'", "", true);

		int start = template.indexOf(fieldKey);
		int ret = -1;
		if (start == -1) {
			return -1;
		}
		int end = start + fieldKey.length();
		if (dateString.length() > (end - 1)) {
			String fieldStr = dateString.substring(start, end);
			ret = Integer.parseInt(fieldStr);
			return ret;
		}
		else {
			return -1;
		}
	}


	/**
	 *  Description of the Method
	 *
	 *@param  value          Description of the Parameter
	 *@return                Description of the Return Value
	 *@exception  Exception  Description of the Exception
	 */
	public static boolean checkValidXsdDate(String value)
		throws Exception {
			prtln ("checkValidXsdDate with \"" + value + "\"");
		if (value.matches("[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9]")) {
			return checkValidUnionDate(value);
		}
		else {
			throw new Exception("date must be in the form of \'yyyy-MM-dd\'");
		}
	}

	/**
	* First make sure value is valid as a "xsd:gYear" and then make sure the max inclusive value is "-0001"
	*/
	public static boolean checkValidBCType(String value, XSDatatypeManager xsdManager)
		throws Exception {
			try {
				// prtln ("checkValidBCType - checking against xsd:gYear");
				String schemaNSPrefix = xsdManager.getSchemaNSPrefix();
				String gYearType = NamespaceRegistry.makeQualifiedName(schemaNSPrefix, "gYear");
				boolean valid = xsdManager.checkValid(gYearType, value);
				int year = Integer.parseInt(value);
				if (year > -1)
					throw new Exception ();
				return valid;
			} catch (Exception e) {
				throw new Exception ("BCType must be in the form \'-yyyy*\' with a max value of \'-0001\'");
			}
	}

	/**
	 *  We are given a date String. First check to make sure it is formatted
	 *  correctly. Then check to see that the values of each field are leagal. This
	 *  is done by checking the fields of the parsed Date against the original date
	 *  String.
	 *
	 *@param  value               Description of the Parameter
	 *@return                     Description of the Return Value
	 *@exception  Exception       Description of the Exception
	 */
	public static boolean checkValidUnionDate(String value)
		throws Exception {
			
		// prtln ("checkValidUnionDate() with " + value);
			
		
		if (value.equals("Present"))
			return true;
		
		
		SimpleDateFormat sdf = new SimpleDateFormat(dateFormatString);

		Date parsedDate = null;
		try {
			parsedDate = MetadataUtils.parseUnionDateType(value);
		} catch (ParseException pe) {
			String errorMsg = pe.getMessage();
			errorMsg = errorMsg.substring(errorMsg.indexOf(":") + 1).trim() + " or \"Present\"";
			throw new Exception(errorMsg);
		}
		// prtln ("Parsed Date: " + sdf.format(parsedDate));

		Calendar cal = Calendar.getInstance();
		cal.setTime(parsedDate);

		// We know the format is okay because it passed by parseUnionDate. Now we validate the values by
		// Walking the date from right to left, checking for mismatches between
		// the value provided and the parsedDate returned by parseUnionDate.
		// The key here is that parseUnionDate will create a date even if, for example,
		// the months field is "99" buy "carrying" the extra months to the year field
		int provided;

		// We know the format is okay because it passed by parseUnionDate. Now we validate the values by
		// Walking the date from right to left, checking for mismatches between
		// the value provided and the parsedDate returned by parseUnionDate.
		// The key here is that parseUnionDate will create a date even if, for example,
		// the months field is "99" buy "carrying" the extra months to the year field
		int parsed;
		try {
			provided = getField("ss", value);

			// if no seconds are provided, then we don't have to check the hours, seconds or minutes
			//   these are checked against leagal maximums for these fields
			// but if seconds are provided, then we are working with a UTC date
			if (provided > -1) {
				// adjusting the calendar in accordance with timezone offset sincs the calendar date with the
				// parsedDate. We could have instead set the calendar's time zone to UTC, but then the PRINTED
				// dates do not look the same, which makes debugging a little more difficult
				int calOffset = -(cal.get(Calendar.ZONE_OFFSET) + cal.get(Calendar.DST_OFFSET)) / (60 * 1000);
				cal.add(Calendar.MINUTE, calOffset);
				// prtln ("ADJUSTED calendar Date: " + sdf.format(cal.getTime()));

				parsed = cal.get(Calendar.SECOND);
				if (provided > cal.getMaximum(Calendar.SECOND)) {
					throw new Exception("SECOND (" + provided + ") out of range");
				}

				provided = getField("mm", value);
				parsed = cal.get(Calendar.MINUTE);
				if (provided > cal.getMaximum(Calendar.MINUTE)) {
					throw new Exception("MINUTE (" + provided + ") out of range");
				}

				provided = getField("HH", value);
				parsed = cal.get(Calendar.HOUR_OF_DAY);
				if (provided > cal.getMaximum(Calendar.HOUR_OF_DAY)) {
					throw new Exception("HOUR (" + provided + ") out of range");
				}

			}

			// now check the parsed DAY, and MONTH  (YEAR is assumed to be okay if it parsed) against the provided values. if
			// they are different, this is because the "lenient" parsing "carried over" the units
			// from one out of range field to another (if DAY was 32, then MONTH will be incremented)

			provided = getField("dd", value);
			if (provided == -1) {
				provided = 1;
			}
			parsed = cal.get(Calendar.DAY_OF_MONTH);
			if (provided != parsed) {
				throw new Exception("DAY (" + provided + ") out of range");
			}

			provided = getField("MM", value);
			if (provided == -1) {
				provided = 1;
			}
			parsed = cal.get(Calendar.MONTH) + 1;
			// Calendar.JANUARY = 0
			// prtln ("\t Month: provided: " + provided + ", parsed: " + parsed);
			if (provided != parsed) {
				throw new Exception("MONTH (" + provided + ") out of range");
			}

			return true;
		} catch (Exception e) {
			throw new ParseException("Invalid date: " + e.getMessage() + " in \"" + value + "\"", 0);
		}
	}


	static String[] testCases = {
			"",
			" ",
			"2005",
			"2005-01",
			"2005-01-30",
			"2005-01-30T01:01:01Z",
			"2005-01-30T23:01:01z",
			"2005-13",
			"2005-01-33",
			"2005-01-30T00:01:01Z",
			"2005-01-30T24:01:01z",
			"2005-01-30T23:99:01z",
			"2005-01-30T23:59:60z"
			};


	/**
	 *  The main program for the DateValidator class
	 *
	 *@param  args           The command line arguments
	 *@exception  Exception  Description of the Exception
	 */
	public static void main(String[] args)
		throws Exception {
		for (int i = 0; i < testCases.length; i++) {
			String dateString = testCases[i];
			prtln("\n ***\nProvided Date " + dateString);
			try {
				/* boolean isValid = checkValidUnionDate(dateString); */
				boolean isValid = checkValidXsdDate(dateString);
				prtln(dateString + " is a valid unionDate value");
			} catch (Exception e) {
				prtln("ERROR: " + e.getMessage());
			}
		}
	}


	/**
	 *  Description of the Method
	 *
	 *@param  s  Description of the Parameter
	 */
	private static void prtln(String s) {
		if (debug) {
			System.out.println(s);
		}
	}
}

