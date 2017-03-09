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
package org.dlese.dpc.schemedit;

import org.dlese.dpc.xml.schema.*;
import org.dlese.dpc.xml.*;

import java.io.*;
import java.util.*;
import java.text.*;
import java.net.URL;

import org.dom4j.Element;
import org.dom4j.Document;
import org.dom4j.DocumentException;

/**
*  Bean for displaying an index of metadata records in the StandAlone metadata editor 
	(see {@link org.dlese.dpc.schemedit.action.StandAloneSchemEditAction}).
 *
 *@author    ostwald<p>
 $Id $
 */
public class Record {
	private static boolean debug = true;

	private String id = null;
	private File file = null;
	private long lastModified = 0;


	/**
	 *  Constructor - creates a new Record containing given Document and assigns it
	 *  given id.
	 *
	 *@param  file  Description of the Parameter
	 */
	public Record(File file) {
		try {
			this.file = file;
			lastModified = file.lastModified();
		} catch (Exception e) {
			System.out.println(e);
		}
	}


	/**
	 *  Gets the path attribute of the Record object
	 *
	 *@return    The path value
	 */
	public String getPath() {
		return file.toString();
	}


	/**
	 *  Gets the lastModified attribute of the Record object
	 *
	 *@return    The lastModified value
	 */
	public long getLastModified() {
		return lastModified;
	}


	/**
	 *  Gets the lastMod attribute of the Record object
	 *
	 *@return    The lastMod value
	 */
	public String getLastMod() {
		Date date = new Date(lastModified);
		SimpleDateFormat df = new SimpleDateFormat("M/d/yyyy h:mm:ss a");
		return df.format(date);
	}


	/**
	 *  Gets the lastModDate attribute of the Record object
	 *
	 *@return    The lastModDate value
	 */
	public String getLastModDate() {
		Date date = new Date(lastModified);
		SimpleDateFormat df = new SimpleDateFormat("M/d/yyyy");
		return df.format(date);
	}


	/**
	 *  Gets the lastModTime attribute of the Record object
	 *
	 *@return    The lastModTime value
	 */
	public String getLastModTime() {
		Date date = new Date(lastModified);
		SimpleDateFormat df = new SimpleDateFormat("h:mm:ss a");
		return df.format(date);
	}


	/**
	 *  Gets the id attribute of the Record object
	 *
	 *@return    The id value
	 */
	public String getId() {
		String s = file.getName();
		return getId(s);
	}


	/**
	 *  Gets the id attribute of the Record class
	 *
	 *@param  fileName  Description of the Parameter
	 *@return           The id value
	 */
	public static String getId(String fileName) {
		int lastDot = fileName.lastIndexOf(".");
		if (lastDot == -1) {
			return fileName;
		}
		else {
			return fileName.substring(0, lastDot);
		}
	}


	/**
	 *  Sets the debug attribute of the Emailer object
	 *
	 *@param  db  The new debug value
	 */
	public static void setDebug(boolean db) {
		debug = db;
	}


	/**
	 *  Print a line to standard out.
	 *
	 *@param  s  The String to print.
	 */
	private void prtln(String s) {
		if (debug) {
			System.out.println("Record: " + s);
		}
	}
}

