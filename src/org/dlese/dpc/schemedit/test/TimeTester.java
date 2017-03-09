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
package org.dlese.dpc.schemedit.test;

import org.dlese.dpc.schemedit.*;
import org.dlese.dpc.xml.XPathUtils;
import org.dlese.dpc.util.*;
import org.dlese.dpc.util.strings.*;
import org.dlese.dpc.oai.OAIUtils;

import java.util.*;
import java.util.regex.*;
import java.net.*;
import java.text.*;

/**
 *  Utilities for manipulating XPaths, represented as String
 *
 *@author    ostwald
 */
public class TimeTester {

	private static boolean debug = true;

	static String dateFormatString = SchemEditUtils.utcDateFormatString;

	// static String myDateFormat = "yyyy-MM-dd";
	static String myDateFormat = "yyyy-MM-dd HH:mm:ss";
	static SimpleDateFormat sdf = new SimpleDateFormat(myDateFormat);
	
	static String getUtcTimeString () {
		Date date = SchemEditUtils.getUtcTime();
		return SchemEditUtils.utcDateString (date);
	}

	static String toUtcTime (String dateStr) throws Exception {
		Date date = sdf.parse(dateStr);
		return sdf.format (SchemEditUtils.getUtcTime(date));
	}
	
	/**
	 *  The main program for the TimeTester class
	 *
	 *@param  args           The command line arguments
	 *@exception  Exception  Description of the Exception
	 */
	public static void main(String[] args) throws Exception {

		prtln ("go");
		long start = new Date().getTime();
		Thread thread = Thread.currentThread();
		thread.sleep(2000);
		long elapsed = new Date().getTime() - start;
		SchemEditUtils.box ("i slept for " + elapsed + " milliseconds!");
		
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


