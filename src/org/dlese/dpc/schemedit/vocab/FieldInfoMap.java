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
package org.dlese.dpc.schemedit.vocab;

import java.io.*;
import java.util.*;
import java.text.*;
import java.net.*;

import org.dom4j.*;

import org.dlese.dpc.schemedit.SchemEditUtils;
import org.dlese.dpc.xml.Dom4jUtils;

/**
 *  Data structure mapping xpaths to {@link
 *  org.dlese.dpc.schemedit.vocab.FieldInfoReader} instances. The FieldInfoMap is instantiated by the
 {@link org.dlese.dpc.schemedit.config.FrameworkConfigReader} and accessed via the 
 {@link org.dlese.dpc.schemedit.MetaDataFramework}.
 *
 *@author    ostwald
 *
 */
public abstract class FieldInfoMap {
	private static boolean debug = true;
	protected HashMap map = null;
	protected String directoryUri = null;


	/**
	 *  Constructor for the FieldInfoMap object
	 */
	public FieldInfoMap() {
		map = new HashMap();
	}

	/**
	 *  Read a listing of URIs Fields files from directoryUri and then loads each
	 *  of the listed files as FieldInfoReader objects, which are stored in a map.
	 *
	 *@exception  Exception  Description of the Exception
	 */
	public abstract void init() throws Exception;

	/**
	 *  Sets the debug attribute of the FieldInfoMap class
	 *
	 *@param  d  The new debug value
	 */
	public static void setDebug(boolean d) {
		debug = d;
	}


	/**
	 *  Constructor for the FieldInfoMap object
	 *
	 *@param  uri  Description of the Parameter
	 */
	public FieldInfoMap(String uri) {
		this();
		directoryUri = uri;
	}


	/**
	 *  Add a FieldInfoReader to the map.
	 *
	 *@param  xpath   Description of the Parameter
	 *@param  reader  Description of the Parameter
	 */
	public void putFieldInfo(String xpath, FieldInfoReader reader) {
		map.put(xpath, reader);
	}


	/**
	 *  Gets the fieldInfo for given xpath.
	 *
	 *@param  xpath  Description of the Parameter
	 *@return        The fieldInfo value or null if not found.
	 */
	public FieldInfoReader getFieldInfo(String xpath) {
		return (FieldInfoReader) map.get(xpath);
	}


	/**
	 *  Description of the Method
	 *
	 *@param  xpath  Description of the Parameter
	 */
	public void removeFieldInfo(String xpath) {
		map.remove(xpath);
	}


	/**
	 *  Gets the keySet attribute of the FieldInfoMap object
	 *
	 *@return    The keySet value
	 */
	public Set getKeySet() {
		if (map == null) {
			map = new HashMap();
		}
		return map.keySet();
	}


	/**
	 *  Reload all the FieldInfoReaders in the map
	 *
	 *@exception  Exception  Description of the Exception
	 */
	public void reload()
		throws Exception {
		init();
	}


	/**
	 *  Gets all the FieldInfoReaders as a list.
	 *
	 *@return    a List of FieldInfoReaders
	 */
	public List getAllFieldInfo() {
		ArrayList list = new ArrayList();
		list.addAll(map.values());
		return list;
	}


	/**
	 *  Gets all the fields (xpaths) having FieldInfoReaders
	 *
	 *@return    a list of xpaths to Fields
	 */
	public List getFields() {
		ArrayList keys = new ArrayList();
		for (Iterator i = getKeySet().iterator(); i.hasNext(); ) {
			keys.add((String) i.next());
		}
		return keys;
	}


	/**
	 *  Returns true if there is a FieldInfoReader present for the specified xpath.
	 *
	 *@param  xpath  Description of the Parameter
	 *@return        Description of the Return Value
	 */
	public boolean hasFieldInfo(String xpath) {
		return getFields().contains(xpath);
	}


	/**
	 *  Description of the Method
	 *
	 *@return    Description of the Return Value
	 */
	public String toString() {
		String ret = "";
		for (Iterator i = map.keySet().iterator(); i.hasNext(); ) {
			String key = (String) i.next();
			ret += key + "\n";
		}
		return ret;
	}


	/**
	 *  Output a line of text to standard out, with datestamp, if debug is set to
	 *  true.
	 *
	 *@param  s  The String that will be output.
	 */
	protected static void prtln(String s) {
		if (debug) {
			System.out.println("FieldInfoMap: " + s);
		}
	}
}

