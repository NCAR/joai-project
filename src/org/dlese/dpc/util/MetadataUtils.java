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
package org.dlese.dpc.util;



import java.util.Date;

import java.util.Random;

import java.util.Enumeration;

import java.util.*;

import java.text.*;



import org.dom4j.*;



import org.dlese.dpc.util.Utils;

import org.dlese.dpc.oai.OAIUtils;



/**

 *  This class holds handy static methods for working with DLESE metadata.

 *

 * @author    John Weatherley

 */

public class MetadataUtils {



	/**

	 *  Converts a String in a DLESE union.dateType format into a Java Date object. The possible formats are

	 *  YYYY-mm-dd, YYYY-mm or YYYY. Uses MST time to interpret the dates.

	 *

	 * @param  dateString          A String that in one of the following four formats: YYYY-mm-dd, YYYY-mm, YYYY.

	 * @return                     The Date object.

	 * @exception  ParseException  If unable to interpret the date String using the given format.

	 */

	public final static Date parseUnionDateType(String dateString)

		 throws ParseException {

		if (dateString.matches("[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9]"))

			return Utils.convertStringToDate(dateString + "MST", "yyyy-MM-ddz");

		else if (dateString.matches("[0-9][0-9][0-9][0-9]-[0-9][0-9]"))

			return Utils.convertStringToDate(dateString + "-01MST", "yyyy-MM-ddz");

		else if (dateString.matches("[0-9][0-9][0-9][0-9]"))

			return Utils.convertStringToDate(dateString + "-01-01MST", "yyyy-MM-ddz");

		else if (dateString.matches("[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9][Tt][0-9][0-9]:[0-9][0-9]:[0-9][0-9][Zz]"))

			return OAIUtils.getDateFromDatestamp(dateString);

		else

			throw new ParseException("Unable to parse union date type: date must be one of yyyy-MM-dd, yyyy-MM, yyyy, yyyy-MM-ddtHH:mm:ssz but found " + dateString, 0);

	}





	/**

	 *  Converts an ISO8601 UTC datastamp of the form yyyy-MM-ddTHH:mm:ssZ or the short form yyyy-MM-dd to a Java

	 *  Date. See <a href="http://www.w3.org/TR/NOTE-datetime">ISO8601 </a> and <a

	 *  href="http://www.openarchives.org/OAI/2.0/openarchivesprotocol.htm#Dates"> OAI date info</a> for more

	 *  info. If the short form yyyy-MM-dd is given, this method adds the String t01:00:00z to it to produce an

	 *  ISO8601 compliant date time.

	 *

	 * @param  datestamp           A datestamp in UTC format.

	 * @return                     A Java Date that corresponds to the date String.

	 * @exception  ParseException  If unable to interpret the datestamp.

	 */

	public final static Date parseISO8601DateStamp(String datestamp)

		 throws ParseException {

		return OAIUtils.getDateFromDatestamp(datestamp, 0);

	}





	/**

	 *  Converts a String of the union.dateType or ISO8601 UTC datastamp form to a Date. The possible forms

	 *  include YYYY-mm-dd, YYYY-mm, YYYY or the ISO8601 form yyyy-MM-ddTHH:mm:ssZ.

	 *

	 * @param  dateString          A date String

	 * @return                     A Date Object

	 * @exception  ParseException  If unable to parse the date

	 */

	public final static Date parseDate(String dateString)

		 throws ParseException {



		try {

			return parseUnionDateType(dateString);

		} catch (ParseException pe) {}



		return parseISO8601DateStamp(dateString);

	}







	private static void prtln(String s) {

		System.out.println(s);

	}





	private static void prtlnErr(String s) {

		System.err.println(s);

	}



}



