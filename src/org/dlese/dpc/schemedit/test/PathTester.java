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
import java.io.*;
import java.text.*;

/**
 *  Utilities for manipulating XPaths, represented as String
 *
 *@author    ostwald
 */
public class PathTester {

	private static boolean debug = true;

	
	/**
	 *  The main program for the PathTester class
	 *
	 *@param  args           The command line arguments
	 *@exception  Exception  Description of the Exception
	 */
	public static void main(String[] args) throws Exception {

		String path1 = "/dpc/tremor/devel/ostwald/records-bog/dlese_collect/collect/..";
		String path2 = "/devel/ostwald/records-bog/dlese_collect";
		
		String bogpath = "/devel/ostwald/folseir";
		if (! (new File (bogpath).exists()))
			prtln ("bog file doesnt exist");
		
		File file1 = new File (path1);
		File file2 = new File (path2);
		
		prtln ("Absolute Paths");
		prtln ("1 " + file1.getAbsolutePath());
		prtln ("2 " + file2.getAbsolutePath());
		
		prtln ("canonicalPaths");
		prtln ("1 " + file1.getCanonicalPath());
		prtln ("2 " + file2.getCanonicalPath());
		
		prtln ("file comparison");
		if (file1.equals(file2))
			prtln ("the files are equal");
		else
			prtln ("the files are DIFFERENT");
		
		prtln ("Canonical file comparison");
		if (file1.getCanonicalFile().equals(file2.getCanonicalFile()))
			prtln ("the files are equal");
		else
			prtln ("the files are DIFFERENT");

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

