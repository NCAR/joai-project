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
package org.dlese.dpc.oai;

import java.util.*;
import java.text.*;

/**
 *  Contains utility methods used in OAI.
 *
 * @author     John Weatherley
 * @version    $Id: OAIUtils.java,v 1.11 2009/03/20 23:33:53 jweather Exp $
 */
public final class OAIUtils {

	/**
	 *  Gets an ISO8601 UTC datestamp string of the form yyyy-MM-ddTHH:mm:ssZ from a Date.
	 *
	 * @param  date  The Date
	 * @return       The corresponding datestamp String in UTC timezone
	 * @see          #getDateFromDatestamp
	 */
	public final static String getDatestampFromDate(Date date) {
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
		df.setTimeZone(new SimpleTimeZone(SimpleTimeZone.UTC_TIME, "UTC"));
		String datestg = df.format(date);
		return datestg;
	}


	/**
	 *  Converts an ISO8601 UTC datestamp String of the form yyyy-MM-ddTHH:mm:ssZ or the short form yyyy-MM-dd to a Java
	 *  Date. See <a href="http://www.w3.org/TR/NOTE-datetime">ISO8601 </a> and <a
	 *  href="http://www.openarchives.org/OAI/2.0/openarchivesprotocol.htm#Dates"> OAI date info</a> for more
	 *  info. If the short form yyyy-MM-dd is given, this method adds the String t01:00:00z to it to produce an
	 *  ISO8601 compliant date time.
	 *
	 * @param  datestamp           A datestamp in UTC format.
	 * @return                     The dateFromDatestamp value
	 * @exception  ParseException  If unable to interpret the datestamp.
	 */
	public final static Date getDateFromDatestamp(String datestamp)
		 throws ParseException {
		return getDateFromDatestamp(datestamp, 0);
	}


	/**
	 *  Converts an ISO8601 UTC datastamp String of the form yyyy-MM-ddTHH:mm:ssZ or the short form yyyy-MM-dd to a Java
	 *  Date. See <a href="http://www.w3.org/TR/NOTE-datetime">ISO8601 </a> and <a
	 *  href="http://www.openarchives.org/OAI/2.0/openarchivesprotocol.htm#Dates"> OAI date info</a> for more
	 *  info. If the short form yyyy-MM-dd is given, this method adds the String t01:00:00z to it to produce an
	 *  ISO8601 compliant date time.
	 *
	 * @param  datestamp           A datestamp in UTC format.
	 * @param  increment           Number of seconds to increment the date, positive or negative, or 0 to leave
	 *      unchanged.
	 * @return                     The dateFromDatestamp value
	 * @exception  ParseException  If unable to interpret the datestamp.
	 */
	public final static Date getDateFromDatestamp(String datestamp, long increment)
		 throws ParseException {

		// Since we're using a Date parser, time begins at January 1, 1970, 00:00:00 GMT,
		// We want to return no matches rather than badArgument, so convert the year to 1970.
		int year = 0;
		try {
			year = Integer.parseInt(datestamp.substring(0, 4));
		} catch (Throwable e) {
			throw new ParseException("Year is malformed", 3);
		}
		if (year < 1970)
			datestamp = "1970" + datestamp.substring(4, datestamp.length());

		if (datestamp.length() == 10) {
			datestamp += "t01:00:00z";
		}
		else {
			datestamp = datestamp.toLowerCase();
		}

		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd't'HH:mm:ss'z'");
		df.setTimeZone(new SimpleTimeZone(SimpleTimeZone.UTC_TIME, "UTC"));
		Date date = df.parse(datestamp);
		if (increment != 0)
			date.setTime((date.getTime() + increment * 1000));
		//prtln("getDateFromDatestamp() returning: " + date.toString());
		return date;
	}


	/**
	 *  Converts an ISO8601 UTC datastamp String of the form yyyy-MM-ddTHH:mm:ssZ to a long. See <a
	 *  href="http://www.w3.org/TR/NOTE-datetime">ISO8601</a> and <a
	 *  href="http://www.openarchives.org/OAI/2.0/openarchivesprotocol.htm#Dates"> OAI date info</a> for more
	 *  info.
	 *
	 * @param  datestamp           A datestamp in UTC format.
	 * @return                     The longFromDatestamp value
	 * @exception  ParseException  If unable to interpret the datestamp.
	 */
	public final static long getLongFromDatestamp(String datestamp)
		 throws ParseException {
		return getDateFromDatestamp(datestamp, 0).getTime();
	}


	/**
	 *  Converts an ISO8601 UTC datastamp String of the form yyyy-MM-ddTHH:mm:ssZ to a long. See <a
	 *  href="http://www.w3.org/TR/NOTE-datetime">ISO8601</a> and <a
	 *  href="http://www.openarchives.org/OAI/2.0/openarchivesprotocol.htm#Dates"> OAI date info</a> for more
	 *  info.
	 *
	 * @param  datestamp           A datestamp in UTC format.
	 * @param  increment           Number of seconds to increment the date, positive or negative, or 0 to leave
	 *      unchanged.
	 * @return                     The longFromDatestamp value
	 * @exception  ParseException  If unable to interpret the datestamp.
	 */
	public final static long getLongFromDatestamp(String datestamp, long increment)
		 throws ParseException {
		return getDateFromDatestamp(datestamp, increment).getTime();
	}


	/**
	 *  Creates an appropriate directory path where harvested file(s) are saved similar to wget paths based on
	 *  the URI.
	 *
	 * @param  basePath  The base output directory.
	 * @param  baseURL   The baseURL of the OAI data provider.
	 * @return           The path where the harvested files are saved.
	 */
	public static String getHarvestedDirBaseURLPath(String basePath, String baseURL) {
		return basePath + "/" + baseURL.replaceFirst("http://", "").replaceAll(":", "/");
	}


	/**
	 *  Creates an appropriate directory path where harvested file(s) are saved based on the baseURL, format and
	 *  set. The set name is encoded for file system compliance.
	 *
	 * @param  basePath  The base output directory.
	 * @param  setname   The OAI setSpec harvested, or null if none.
	 * @param  prefix    The metadataPrefix (format) harvested.
	 * @param  baseURL   The baseURL of the OAI data provider.
	 * @return           The path where the harvested files are saved.
	 */
	public static String getHarvestedDirPath(String basePath, String setname, String prefix, String baseURL) {
		String path;

		String baseURLPath = getHarvestedDirBaseURLPath(basePath, baseURL);
		//prtln("filePath baseURLPath: " + baseURLPath);

		if (setname == null || setname.equals(""))
			path = baseURLPath + "/" + prefix;
		else {
			try {
				path = baseURLPath + "/" + encode(setname) + "/" + prefix;
			} catch (Exception e) {
				path = baseURLPath + "/" + setname + "/" + prefix;
			}
		}

		return path;
	}


	/**
	 *  Translations used in encoding identifiers and resumption tokens. Also can be used to encode file names
	 *  but not directory paths.<p>
	 *
	 *  See section 3.1.1.3 in http://www.openarchives.org/OAI/2.0/openarchivesprotocol.htm <p>
	 *
	 *  See also http://www.ietf.org/rfc/rfc2396.txt
	 *  <ul>
	 *    <li> unreserved = alphanum | mark
	 *    <li> mark = "-" | "_" | "." | "!" | "~" | "*" | "'" | "(" | ")"
	 *  </ul>
	 *
	 */
	private final static String[][] encodings = {
		{"%", "%25"},  // this one must be first
		{"/", "%2F"},
		{"?", "%3F"},
		{"#", "%23"},
		{"=", "%3D"},
		{"&", "%26"},
		{":", "%3A"},
		{";", "%3B"},
		{" ", "%20"},
		{"+", "%2B"}
		};
	
	private static HashMap encodingsMap = null;	
	static {
		encodingsMap = new HashMap(encodings.length);
		for( int i = 0; i < encodings.length; i++)
			encodingsMap.put(encodings[i][0],encodings[i][1]);	
	}

	/**
	 *  Encode an identifier or resumption token. Can also be used to encode chars for use in file names.<p>
	 *
	 *  See section 3.1.1.3 in http://www.openarchives.org/OAI/2.0/openarchivesprotocol.htm
	 *
	 * @param  msg            A String to encode.
	 * @return                Encoded String
	 * @exception  Exception  If error
	 */
	public static String encode(String msg)
		 throws Exception {
			 
		StringBuffer result = new StringBuffer(msg.length() + 10);	
		for (int i = 0; i < msg.length(); i++) {
			Object curChar = msg.substring(i,i+1);
			if(encodingsMap.containsKey(curChar))
				curChar = encodingsMap.get(curChar);
			result.append(curChar);
		}
		return result.toString();
	}


	/**
	 *  Decode an identifier or resumption token. <p>
	 *
	 *  See section 3.1.1.3 in http://www.openarchives.org/OAI/2.0/openarchivesprotocol.htm
	 *
	 * @param  msg            Message to decode
	 * @return                Decoded String
	 * @exception  Exception  If unable to decode.
	 */
	public static String decode(String msg)
		 throws Exception {
		int ii;
		String res = msg;
		for (ii = encodings.length - 1; ii >= 0; ii--) { // reverse order
			String key; // reverse order
			String value;
			key = encodings[ii][1]; // reverse order
			value = encodings[ii][0];
			if (res.indexOf(value) >= 0)
				throw new Exception("cannot decode stg: \"" + msg + "\"");
			res = replaceall(res, key, value);
		}
		return res;
	}



	/**
	 *  Replaces all occurances in stg of key with value. Cannot use Java's string.replaceAll since it chokes on
	 *  the characters used in regular expressions.
	 *
	 * @param  stg    String in which to replace
	 * @param  key    Key to replace
	 * @param  value  Value that will replace key
	 * @return        Resulting String.
	 */
	private final static String replaceall(
	                                       String stg,
	                                       String key,
	                                       String value) {
		int ix;
		int previx;

		String res = "";
		previx = 0;
		while (true) {
			ix = stg.indexOf(key, previx);
			if (ix < 0) {
				res += stg.substring(previx);
				break;
			}
			res += stg.substring(previx, ix);
			res += value;
			ix += key.length();
			previx = ix;
		}
		return res;
	}

}


