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
public class SetTester {

	private static boolean debug = true;

	/**
	 *  The main program for the SetTester class
	 *
	 *@param  args           The command line arguments
	 *@exception  Exception  Description of the Exception
	 */
	public static void main(String[] args) throws Exception {
		HashMap map = new HashMap ();
		map.put ("one", "1");
		map.put ("two", "2");
		map.put ("three", "3");
		map.put ("zero", "0");
		
		List pruneList = new ArrayList ();
		pruneList.add ("two");
		pruneList.add ("three");
		
		showMap (map);
		
		map = pruneMap (map, pruneList);
		prtln (" --------------------------------");
		showMap (map);

	}

	static HashMap pruneMap (HashMap map, List pruneList) {
		for (Iterator i=pruneList.iterator();i.hasNext();) {
			String key = (String) i.next();
			map.remove(key);
		}
		return map;
	}
	
	static void showMap (HashMap map) {
		Set keys = map.keySet();
		for (Iterator i=keys.iterator();i.hasNext();) {
			String key = (String) i.next();
			String value = (String) map.get(key);
			prtln (key + ": " + value);
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

