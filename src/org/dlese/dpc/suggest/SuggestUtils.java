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
package org.dlese.dpc.suggest;

import org.dlese.dpc.util.*;
import org.dlese.dpc.xml.schema.SchemaHelper;

import java.util.*;
import java.io.*;
import java.text.*;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.Attribute;

/**
 *  Utilities supporting the Suggestor clients.
 *
 * @author     ostwald
 * @version    $Id: SuggestUtils.java,v 1.10 2009/03/20 23:34:00 jweather Exp $
 */
public class SuggestUtils {

	private static boolean debug = false;


	/**
	 *  Simple email validation, throwing Exception containing error if not valid.
	 *  <p>
	 *
	 *  Validates eemail address against two rules:
	 *  <ol>
	 *    <li> MUST have one and only one "@"
	 *    <li> must not end in aPeriod
	 *  </ol>
	 *
	 *
	 * @param  email          NOT YET DOCUMENTED
	 * @return                NOT YET DOCUMENTED
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public static boolean validateEmail(String email) throws Exception {

		int ampCount = 0;
		char[] chars = email.toCharArray();
		int len = chars.length;
		for (int i = 0; i < len; i++) {
			if (String.valueOf(chars[i]).equals("@")) {
				ampCount++;
			}
		}
		if (ampCount != 1) {
			throw new Exception("email address must contain one and only one \"@\"");
		}
		else if (email.endsWith(".")) {
			throw new Exception("email address must not end with a period (\".\")");
		}
		return true;
	}


	/**
	 *  Gets the enumeratedValues of the element of doc at xpath. EnumeratedValues
	 *  are the values of the "value" attribute of children of the target element.
	 *  This method assumes that the target element has children elements all
	 *  having a "value" attribute.
	 *
	 * @param  doc    an {@link org.dom4j.Document}
	 * @param  xpath  xpath to a specific {@link org.dom4j.Element}
	 * @return        a list of the value attribute value for all children of the
	 *      target element.
	 */
	public static ArrayList getEnumeratedValues(Document doc, String xpath) {
		ArrayList values = new ArrayList();
		List list = doc.selectNodes(xpath);
		for (Iterator i = list.iterator(); i.hasNext(); ) {
			Element e = (Element) i.next();
			String v = e.attributeValue("value");
			values.add(v);
		}
		return values;
	}


	/**
	 *  USED ONLY WITHIN SUGGEST.RECORD - not necessary if records are first
	 *  localized ... XPaths require a little tweaking for use with the DLESE
	 *  schemas due to the way that namespaces are used. for each element in an
	 *  XPath, there must be a <tt>local-name()=</tt> before the actual element
	 *  name. e.g., instead of <tt>/<font color="red">title</font> </tt> we have to
	 *  use <tt> /&star;[local-name()='<font color="red">title</font> ']</tt> this
	 *  method converts simple xpaths to this new form Note: this algorithm chops a
	 *  trailing slash from a given xpath, which is okay because a trailing slash
	 *  will cause an exception in dom4j
	 *
	 * @param  schemaHelper  NOT YET DOCUMENTED
	 * @return               Description of the Return Value
	 */
	/* 	public static String dleseXPath(String xpath) {
		// figure out what the first path delimiter is, if any
		// it could be /, //, or none
		String d = "";
		int x = 0;
		if (xpath.startsWith("//")) {
			d = "//";
			x = 2;
		}
		else if (xpath.startsWith("/")) {
			d = "/";
			x = 1;
		}
		// remove the first path delimiter (if any) from the xpath
		// so the split will not detect an initial delimiter
		String tmppath = xpath.substring(x);
		// prtln ("tmppath: *" + tmppath + "*");
		String[] pathElements = tmppath.split("/");
		String localpath = d;
		for (int i = 0; i < pathElements.length; i++) {
			if (i > 0) {
				localpath += "/";
			}
			String seg = pathElements[i];
			if (!isLegalSegment(seg)) {
				prtln("dleseXPath() caught an illegal character in '" + seg + "'");
				return "";
			}
			localpath += "*[local-name()='" + pathElements[i] + "']";
		}
		return localpath;
	}
 */
	/**
	 *  Calculates the nameSpaceInfo attribute for item records. nameSpaceInfo is
	 *  the stuff that goes in the root element of an Instance Document for this
	 *  framework. The namespace information is stripped from instance documents
	 *  when they are read into the editor because it makes working with the xml
	 *  VERY much easier. But it must be added to the document again when it is
	 *  written to disk, so we keep it here. Eventually the program will be smart
	 *  enough to manage this process by itself, but for now we give it a crutch .
	 *  . . This attribute is specified by the properties file.
	 *
	 * @param  schemaHelper  NOT YET DOCUMENTED
	 * @return               The nameSpaceInfo value
	 */
	public static String getNameSpaceInfo(SchemaHelper schemaHelper) {
		String targetNs = schemaHelper.getTargetNamespace();
		String s = "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" ";
		String schemaLocation = schemaHelper.getSchemaLocation();
		if (schemaLocation == null)
			schemaLocation = "";
		if (targetNs != null && targetNs.trim().length() > 0) {
			s += "xmlns=\"" + targetNs + "\" ";
			s += "xsi:schemaLocation=\"" + targetNs + " " + schemaLocation + "\"";
		}
		else {
			s += "xsi:noNamespaceSchemaLocation=\"" + schemaLocation + "\"";
		}
		return s;
	}


	/**
	 *  defines a set of characters that, if present in a xpath segment given to
	 *  dleseXPath, will throw an error. {@link dleseXPath} helper and therfore
	 *  deprecieated. this method is EXPERIMENTAL, since i really don't know what
	 *  would happen if these characters were left in the path, or if the current
	 *  set of bad characters is complete of course, i doubt this algorithm is very
	 *  efficient, but at least it works!
	 *
	 * @param  segment  Description of the Parameter
	 * @return          The legalSegment value
	 */
	/* 	private static boolean isLegalSegment(String segment) {
		char[] badchars = {'[', '*'};
		for (int i = 0; i < badchars.length; i++) {
			if (segment.indexOf(badchars[i]) != -1) {
				return false;
			}
		}
		return true;
	} */
	/**
	 *  Converts a String in a DLESE union.dateType format into a Java Date object.
	 *  The possible formats are YYYY-mm-dd, YYYY-mm or YYYY. Uses MST time to
	 *  interpret the dates.
	 *
	 * @param  dateString          A String that in one of the following four
	 *      formats: YYYY-mm-dd, YYYY-mm, YYYY.
	 * @return                     The Date object.
	 * @exception  ParseException  If unable to interpret the date String using the
	 *      given format.
	 */
	/* 	public final static Date parseDateString(String dateString)
		 throws ParseException {
		if (dateString.matches("[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9]"))
			return Utils.convertStringToDate(dateString, "yyyy-MM-ddz");
		else if (dateString.matches("[0-9][0-9][0-9][0-9]-[0-9][0-9]"))
			return Utils.convertStringToDate(dateString + "-01", "yyyy-MM-ddz");
		else if (dateString.matches("[0-9][0-9][0-9][0-9]-[0-9][0-9]"))
			return Utils.convertStringToDate(dateString + "-01-01", "yyyy-MM-ddz");
		else if (dateString.matches("[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9][T][0-9][0-9]:[0-9][0-9]:[0-9][0-9][Z]"))
			return Utils.convertStringToDate(dateString, "yyyy-MM-dd'T'HH:mm:ss'Z'");
		else
			throw new ParseException("Unable to parse union date type: date must be one of yyyy-MM-dd, yyyy-MM, yyyy, yyyy-MM-ddtHH:mm:ssz", 0);
	} */
	/**  NOT YET DOCUMENTED */
	public static String FullDateFormatString = "yyyy-MM-dd'T'HH:mm:ss'Z'";


	/**
	 *  returns current time in "yyyy-MM-dd'T'HH:mm:ss'Z'" format
	 *
	 * @return    The fullDate value
	 */
	public static String getFullDate() {
		Date date = new Date();
		SimpleDateFormat sdf = new SimpleDateFormat(SuggestUtils.FullDateFormatString);
		return sdf.format(date);
	}


	/**  NOT YET DOCUMENTED */
	public static String BriefDateFormatString = "yyyy-MM-dd";


	/**
	 *  Returns current time in yyyy-MM-dd format
	 *
	 * @return    The briefDate value
	 */
	public static String getBriefDate() {
		Date date = new Date();
		SimpleDateFormat sdf = new SimpleDateFormat(SuggestUtils.BriefDateFormatString);
		return sdf.format(date);
	}


	/**
	 *  Converts a comma-delimited string into a String array. Used to parse
	 *  "emailTo" init parameters.
	 *
	 * @param  s  comma-delimted string
	 * @return    array of Strings
	 */
	public static String[] commaDelimitedToArray(String s) {
		if (s == null)
			return new String[]{};
		System.out.println("commaDelimitedToArray: " + s);
		String[] splits = s.split(",");
		List items = new ArrayList();
		for (int i = 0; i < splits.length; i++) {
			String val = splits[i].trim();
			if (val.length() > 0)
				items.add(val);
		}
		return (String[]) items.toArray(new String[]{});
	}


	/**
	 *  Sets the debug attribute
	 *
	 * @param  db  The new debug value
	 */
	public static void setDebug(boolean db) {
		debug = db;
	}


	/**
	 *  Description of the Method
	 *
	 * @param  s  Description of the Parameter
	 */
	private static void prtln(String s) {
		if (debug) {
			System.out.println(s);
		}
	}
}

