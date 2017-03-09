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
import java.net.URLEncoder;
import java.io.*;

// The ValueSortedMap is implemented in DLESETools, using the Apache Commons Collections packages
import org.apache.commons.collections.map.ValueSortedMap;

import org.dlese.dpc.index.document.DateFieldTools;
import org.dlese.dpc.index.*;

/**
 *  This class holds a number of handy static methods for generating unique ids, random numbers within a given
 *  range, parsing dates, executing command-line processes, and others.
 *
 * @author    John Weatherley
 */
public class Utils {
	final static Random randgen = new Random(new Date().getTime());


	/**
	 *  Converts a String that contains a recognizable date/time to a Java Date object. See {@link
	 *  java.text.SimpleDateFormat} for syntax information for the format String. For example a valid date String
	 *  might look like '2003-04-27MST' with a date format specifier of 'yyyy-MM-ddz' where yyyy indicates the
	 *  year, MM the month, dd the day and z the general time zone (GMT, MST, PST, etc).
	 *
	 * @param  dateString          A String that contains a recognizable date/time in it, for example
	 *      '2003-04-27MST'.
	 * @param  dateFormat          The format of the date String as specified in {@link
	 *      java.text.SimpleDateFormat}, for example 'yyyy-MM-ddz'.
	 * @return                     The Date object.
	 * @exception  ParseException  If unable to interpret the date String using the given format specifier.
	 */
	public final static Date convertStringToDate(String dateString, String dateFormat)
		 throws ParseException {
		return new SimpleDateFormat(dateFormat).parse(dateString);
	}


	/**
	 *  Converts a Java Date that into a formatted String. See {@link java.text.SimpleDateFormat} for syntax
	 *  information for the format String. For example a valid date format specifier might be '"MMM' 'dd', 'yyyy'
	 *  'z"' where yyyy indicates the year, MMM the month, dd the day and z the time zone (GMT, MST, PST, etc).
	 *
	 * @param  date                A Java Date object.
	 * @param  dateFormat          The format of the date String to be output as specified in {@link
	 *      java.text.SimpleDateFormat}, for example 'yyyy-MM-ddz'.
	 * @return                     The formatted String.
	 * @exception  ParseException  If unable to interpret the date Date using the given format specifier.
	 */
	public final static String convertDateToString(Date date, String dateFormat)
		 throws ParseException {
		SimpleDateFormat df = new SimpleDateFormat(dateFormat);
		return df.format(date);
	}


	/**
	 *  Converts a long representation of time to a Date.
	 *
	 * @param  milliseconds  Time in milliseconds
	 * @return               A Date object
	 */
	public final static Date convertLongToDate(long milliseconds) {
		return new Date(milliseconds);
	}


	/**
	 *  Print the elapsed time that occured beween two points of time as recorded in java Date objects.
	 *
	 * @param  start  The start Date.
	 * @param  end    The end Date.
	 * @param  msg    A message inserted in front of the elapsed time string.
	 */
	public static void printElapsedTime(String msg, Date start, Date end) {
		long ms = (end.getTime() - start.getTime()) % 1000;
		long sec1 = (long) Math.floor((end.getTime() - start.getTime()) / 1000);
		long min = (long) Math.floor(sec1 / 60);
		long sec = sec1 - 60 * min;
		long tms = end.getTime() - start.getTime();

		prtln(msg + min + " minutes and " + sec + "." + ms + " seconds");
		prtln("(" + tms + " total milliseconds)");
	}


	/**
	 *  Converts a long that represents time in milliseconds to a String that displays the time in minutes and
	 *  seconds.
	 *
	 * @param  milliseconds  Time in millisoconds.
	 * @return               A String that displays the time in minutes and seconds.
	 */
	public static String convertMillisecondsToTime(long milliseconds) {
		long ms = milliseconds % 1000;
		long sec1 = (long) Math.floor(milliseconds / 1000);
		long min = (long) Math.floor(sec1 / 60);
		long sec = sec1 - 60 * min;

		if (min == 0) {
			if (sec < 10)
				return (sec + "." + ms + " seconds");
			else
				return (sec + " seconds");
		}
		else if (min == 1)
			return (min + " minute, " + sec + " seconds");
		else
			return (min + " minutes, " + sec + " seconds");
	}


	/**
	 *  Generates a random alpha string of the given length.
	 *
	 * @param  length  The length of the String to generate
	 * @return         The randomAlphaString value
	 */
	public static String getRandomAlphaString(int length) {
		StringBuffer ret = new StringBuffer(length);
		int i;
		for (int j = 0; j < length; ) {
			i = ((int) (Math.abs(randgen.nextLong() % 256)));
			if (i >= 97 && i <= 122) {
				ret.append((char) i);
				j++;
			}
		}
		return ret.toString();
	}


	/**
	 *  Generates a random integer greater-than or equal to low and less-than high.
	 *
	 * @param  low   Smallest possible value
	 * @param  high  Highest possible value
	 * @return       A random number greater-than or equal to low and less-than high
	 */
	public static int getRandomIntBetween(int low, int high) {
		int total = high - low;
		return low + ((int) (Math.abs(randgen.nextLong() % total)));
	}


	/**
	 *  Generates a random string containing extended chars of the given length
	 *
	 * @param  length  The length of the String
	 * @return         The randomCharsString value
	 */
	public static String getRandomCharsString(int length) {
		StringBuffer ret = new StringBuffer(length);
		char c;
		for (int j = 0; j < length; ) {
			c = (char) ((int) (Math.abs(randgen.nextLong() % 256)));
			if (!Character.isISOControl(c) && !Character.isWhitespace(c)) {
				ret.append(c);
				j++;
			}
		}
		return ret.toString();
	}


	/**
	 *  Returns true if the object in the first parameter contains the Object in the second parameter according
	 *  to the Objects equals method. The first parameter can be an Array, Collection (List, Set, etc.), Map or
	 *  String. If the Object in the first parameter is a String, returns true if the Object in the second
	 *  parameter is a subString of the first. If the Object in the first parameter is a Map, returns true if one
	 *  of it's keys is equal to the Object in the second parameter. <p>
	 *
	 *  Implements a more versitile version of the regular JSTL contains function.
	 *
	 * @param  target   The target Object
	 * @param  subject  The subject Object for comparison
	 * @return          True if the target Object contains the subject Object
	 */
	public final static boolean contains(Object target, Object subject) {
		if (target == null || subject == null)
			return false;

		try {
			if (target instanceof String) {
				if (!(subject instanceof String))
					return false;
				return (((String) target).indexOf((String) subject) != -1);
			}
			else if (target instanceof Object[]) {
				Object[] myArray = (Object[]) target;
				for (int i = 0; i < myArray.length; i++) {
					if (myArray[i].equals(subject))
						return true;
				}
			}
			else if (target instanceof Collection) {
				return (((Collection) target).contains(subject));
			}
			else if (target instanceof Map) {
				return (((Map) target).containsKey(subject));
			}
		} catch (ClassCastException ce) {

		} catch (Throwable t) {
			prtlnErr("Problem in contains(): " + t);
		}
		return false;
	}


	/**
	 *  Puts items in a Map, creating a new TreeMap if null is passed in for the myMap argument, otherwise
	 *  updating the Map with the key/value pair. TreeMap keeps its keys sorted lexagraphically.
	 *
	 * @param  myMap  A Map or null
	 * @param  key    A key for insertion in the Map
	 * @param  value  A value for insertion in the Map, or null to insert empty String
	 * @return        The existing Map or a new TreeMap with the key/value pair inserted
	 */
	public static Map map(Map myMap, String key, String value) {
		if (myMap == null)
			myMap = new TreeMap();
		if (key == null)
			return myMap;
		if (value == null)
			value = "";
		myMap.put(key, value);
		return myMap;
	}


	/**
	 *  Puts items in a Map, creating a new Map if null is passed in for the myMap argument, otherwise updating
	 *  the Map with the key/value pair. Keys remain sorted by the *values* in the Map.
	 *
	 * @param  myMap      A Map or null
	 * @param  key        A key for insertion in the Map
	 * @param  value      A value for insertion in the Map, or null to insert empty String
	 * @param  ascending  'ascending' or 'descending' (defaults to ascending if other value is passed)
	 * @return            The existing Map or a new TreeMap with the key/value pair inserted
	 */
	public static Map mapSortByValue(Map myMap, String key, String value, String ascending) {
		boolean isAscending = (ascending == null || !ascending.equals("descending"));
		if (myMap == null)
			myMap = new ValueSortedMap(isAscending);
		if (key == null)
			return myMap;
		if (value == null)
			value = "";
		myMap.put(key, value);
		return myMap;
	}


	/**
	 *  Gets the Map from a String in the Java properties format of the form property=value one per line.
	 *
	 * @param  propertiesString  A Java Properties String
	 * @return                   A Map of properties, or null if unable to parse
	 */
	public static Map getPropertiesMap(String propertiesString) {
		try {
			Properties properties = new Properties();
			// Fix the string to make sure the line ending string is actually a line ending char:
			properties.load(new ByteArrayInputStream(propertiesString.replaceAll("\\\\n", "\n").getBytes()));
			return properties;
		} catch (Throwable t) {

		}
		return null;
	}


	/**
	 *  Runs the given command-line command.
	 *
	 * @param  command         The full command string including arguments
	 * @param  errorOutput     A StringBuffer that will be pupulated with the normal output after execution, if
	 *      any, or null not to use it
	 * @param  standardOutput  A StringBuffer that will be pupulated with the error output after execution, if
	 *      any, or null not to use it
	 * @return                 0 if normal completion, non-zero if abnormal
	 */
	public static int runCommand(String command, StringBuffer standardOutput, StringBuffer errorOutput) {
		try {
			if (command == null)
				return -1;

			//prtln("\nExecuting \"" + command + "\"\n");

			Runtime rt = Runtime.getRuntime();
			Process proc = rt.exec(command);

			BufferedReader stdout =
				new BufferedReader(new InputStreamReader(proc.getInputStream()));

			BufferedReader error =
				new BufferedReader(new InputStreamReader(proc.getErrorStream()));

			// Wait for the process to exit
			proc.waitFor();

			if (standardOutput != null) {
				// Output it's message:
				String line = null;
				while ((line = stdout.readLine()) != null) {
					standardOutput.append(line);
				}
				stdout.close();
			}

			if (errorOutput != null) {
				// Output errors:
				String line = null;
				while ((line = error.readLine()) != null) {
					errorOutput.append(line);
				}
				error.close();
			}

			return proc.exitValue();
		} catch (Throwable e) {
			if (errorOutput != null)
				errorOutput.append("Error executing command: " + e);
			return -1;
		}
	}


	/**  Prints the char values of all chars in range 0 to 256. */
	public static void print_char_values() {
		StringBuffer ret = new StringBuffer();
		char c;
		for (int j = 0; j < 256; j++) {
			c = (char) j;
			prtln("index: " + j + " is: " + c);
		}
	}


	/**
	 *  Encodes a String for use in a URL using UTF-8 character encoding.
	 *
	 * @param  text                              Unencoded text
	 * @return                                   Text that is encoded for use in a URL
	 * @exception  UnsupportedEncodingException  If unable to encode using UTF-8.
	 */
	public final static String URLEncoder(String text)
		 throws UnsupportedEncodingException {
		if (text == null)
			return null;
		try {
			return URLEncoder.encode(text, "utf-8");
		} catch (Throwable e) {
			return text;
		}
	}


	/**
	 *  Encodes a string used in the URL sent to the redirect server and ensures it does not contain problematic
	 *  characters for the Apache 1 mod_redirect rules (the character sequence %2F is replaced with /).
	 *
	 * @param  text                              Unencoded text
	 * @return                                   Text that is encoded for use in a URL
	 * @exception  UnsupportedEncodingException  If unable to encode using UTF-8.
	 */
	public final static String RedirectEncoder(String text)
		 throws UnsupportedEncodingException {
		if (text == null)
			return "";
		return URLEncoder(text).replaceAll("%2F", "/");
	}


	/**
	 *  Inspects an objects type and methods to standard out.
	 *
	 * @param  o  The Object to inspect
	 * @return    The objects class name
	 */
	public final static String ObjectInspector(Object o) {
		if (o == null)
			return "null";

		Class c = o.getClass();

		prtln("ObjectInspector: " + c.getName());

		return c.getName();
	}


	/**
	 *  Tells whether or not the source string matches the given regular expression.
	 *
	 * @param  source  The source String
	 * @param  regEx   A regular expression
	 * @return         True if the source String matches the regular expression
	 * @see            java.lang.String#matches(String regEx)
	 */
	public final static boolean matches(String source, String regEx) {
		try {
			return source.matches(regEx);
		} catch (Throwable e) {
			return false;
		}
	}


	/**
	 *  Replaces each substring of this string that matches the given regular expression with the given
	 *  replacement. If replacement is empty or null, all matches are removed from the source String.
	 *
	 * @param  source       The source String
	 * @param  regEx        A regular expression
	 * @param  replacement  The replacement String
	 * @return              The resulting String
	 * @see                 java.lang.String#replaceAll(String regex, String replacement)
	 */
	public final static String replaceAll(String source, String regEx, String replacement) {
		try {
			if (replacement == null)
				replacement = "";
			return source.replaceAll(regEx, replacement);
		} catch (Throwable e) {
			return source;
		}
	}


	/**
	 *  Replaces the first substring of this string that matches the given regular expression with the given
	 *  replacement. If replacement is empty or null, all matches are removed from the source String.
	 *
	 * @param  source       The source String
	 * @param  regEx        A regular expression
	 * @param  replacement  The replacement String
	 * @return              The resulting String
	 * @see                 java.lang.String#replaceFirst(String regex, String replacement)
	 */
	public final static String replaceFirst(String source, String regEx, String replacement) {
		try {
			if (replacement == null)
				replacement = "";
			return source.replaceFirst(regEx, replacement);
		} catch (Throwable e) {
			return source;
		}
	}


	/**
	 *  Converts a Lucene String-encoded date to a Date Object. If the String can not be converted, returns null.
	 *
	 * @param  dateString  A Lucene String-encoded date
	 * @return             A Date Object, or null
	 */
	public final static Date luceneStringToDate(String dateString) {
		try {
			return DateFieldTools.stringToDate(dateString);
		} catch (Throwable t) {
			return null;
		}
	}


	/**
	 *  Converts a date String of the form YYYY-mm-dd, YYYY-mm, YYYY or yyyy-MM-ddTHH:mm:ssZ to a searchable
	 *  Lucene (v2.x) lexical date String of the form 'yyyyMMddHHmmss', or null if unable to parse the date
	 *  String.
	 *
	 * @param  dateString  A date String
	 * @return             The corresponding lexicalDateString value
	 */
	public final static String getLexicalDateString(String dateString) {
		try {
			return DateFieldTools.dateToString(MetadataUtils.parseDate(dateString));
		} catch (Throwable t) {
			return null;
		}
	}


	/**
	 *  Gets a ResourceBundle from a properties file that is in a Jar file or class path within this
	 *  application's runtime environment.
	 *
	 * @param  propsFileName  The name of the properties file
	 * @return                The ResourceBundle, or null if not found
	 */
	public final static ResourceBundle getPropertiesResourceBundle(String propsFileName) {
		Utils tmp = new Utils();
		InputStream input = tmp.getClass().getResourceAsStream(propsFileName);
		ResourceBundle resourceBundle = null;
		if (input == null) {
			input = tmp.getClass().getResourceAsStream("/" + propsFileName);
		}
		if (input != null) {
			try {
				resourceBundle = new PropertyResourceBundle(input);
			} catch (IOException ioe) {} finally {
				try {
					input.close();
				} catch (IOException ioe) {}
			}
		}
		else {
			//prtlnErr( "Could not load properties \"" + propsFileName + "\"");
		}
		return resourceBundle;
	}


	/**
	 *  Gets a String representation of the Calendar.DAY_OF_WEEK field.
	 *
	 * @param  dayOfWeek                           A Calenday.DAY_OF_WEEK field value
	 * @return                                     A String representation of the day
	 * @exception  ArrayIndexOutOfBoundsException  If the specified field is out of range (field < 0 || field >=
	 *      7)
	 */
	public static String getDayOfWeekString(int dayOfWeek) throws ArrayIndexOutOfBoundsException {
		switch (dayOfWeek) {
						case 1:
							return "Sunday";
						case 2:
							return "Monday";
						case 3:
							return "Tuesday";
						case 4:
							return "Wednesday";
						case 5:
							return "Thursday";
						case 6:
							return "Friday";
						case 7:
							return "Saturday";
						default:
							throw new ArrayIndexOutOfBoundsException("Day of week must be >= 1 and <= 7");
		}
	}


	/**
	 *  Splits this string around matches of the given regular expression.
	 *
	 * @param  source  The source String
	 * @param  regEx   A regular expression
	 * @return         The resulting String tokens
	 * @see            java.lang.String#split(String regex)
	 */
	public final static String[] split(String source, String regEx) {
		try {
			return source.split(regEx);
		} catch (Throwable e) {
			return null;
		}
	}


	/**
	 *  Encodes a String to a single term for searching over fields that have been indexed encoded. Encodes
	 *  spaces but leaves the wild card '*' un-encoded for searching.
	 *
	 * @param  string  A String to encode
	 * @return         An encoded String that may be used for searches
	 * @see            org.dlese.dpc.index.SimpleLuceneIndex#encodeToTerm(String, boolean, boolean)
	 */
	public final static String encodeToSearchTerm(String string) {
		return SimpleLuceneIndex.encodeToTerm(string, false, true);
	}


	/**
	 *  Encodes a String to a String for searching over fields that have been indexed encoded. Preserves the wild
	 *  card '*' and spaces ' '.
	 *
	 * @param  string  The String to encode
	 * @return         An encoded String that may be used for searches
	 * @see            org.dlese.dpc.index.SimpleLuceneIndex#encodeToTerm(String, boolean, boolean)
	 */
	public final static String encodeToSearchTerms(String string) {
		return SimpleLuceneIndex.encodeToTerm(string, false, false);
	}


	/**
	 *  Sends the string to System-err-println.
	 *
	 * @param  s  String to ouptut.
	 */
	public final static void printToSystemErr(String s) {
		System.err.println(s);
	}


	/**
	 *  * Sends the string to System-out-println.
	 *
	 * @param  s  String to ouptut.
	 */
	public final static void printToSystemOut(String s) {
		System.out.println(s);
	}


	private static long current = System.currentTimeMillis();


	/**
	 *  Gets a global system unique ID. This algorithm is reasonably guaranteed to be correct within a single
	 *  running JVM.
	 *
	 * @return    A number guaranteed to be unique throughout this JVM.
	 */
	public static synchronized long getUniqueID() {
		return current++;
	}


	private final static void prtlnErr(String s) {
		System.err.println("Error: " + s);
	}


	private final static void prtln(String s) {
		System.out.println(s);
	}

}


