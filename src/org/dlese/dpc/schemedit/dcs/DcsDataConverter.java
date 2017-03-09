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
package org.dlese.dpc.schemedit.dcs;

// import org.dlese.dpc.xml.schema.*;
import org.dlese.dpc.xml.*;
import org.dlese.dpc.schemedit.*;
import java.io.*;
import java.util.*;

import org.dom4j.*;

/**
 *  Converts old style DcsData records (that only mantained a
 *  single-entry) into history-aware records.
 *
 *@author    ostwald<p>
 $Id $
 */
public class DcsDataConverter {

	private static boolean debug = true;

	private File dcsDataRecords = null;
	private DocumentFactory df = DocumentFactory.getInstance();
	private int converted = 0;
	private int alreadyConverted = 0;
	private int errors = 0;
	private MetaDataFramework framework = null;


	/**
	 *  DcsDataConverter Constructor. The dcsDataRecords param points to 
	 the root directory containing dcs_data records. the subdirectories are named for the
	 metadata formats of the directories they contain, which in turn hold directories for each
	 collection of that format.
	 
	 *
	 *@param  path  directory holding default record and records directory
	 */
	public DcsDataConverter(String path, MetaDataFramework framework) {

		this.framework = framework;
		dcsDataRecords = new File(path);
		if (!dcsDataRecords.exists()) {
			prtln("root dcs_data record directory doesn't exist at " + path);
			return;
		}
	}


	/**
	 *  Convert a directory of dcs_data files into new format by calling <b>convertFile
	 *  </b> on each file. Idempotent: will only convert old-style records and
	 *  leaves new-style records unchanged.
	 */
	public void convert() {
		File[] formatDirs = dcsDataRecords.listFiles();
		for (int i = 0; i < formatDirs.length; i++) {
			File formatDir = formatDirs[i];
			String xmlFormat = formatDir.getName();
			if (xmlFormat.startsWith("."))
				continue;
			prtln ("converting " + xmlFormat);
			File[] collections = formatDir.listFiles();
			for (int j=0;j<collections.length;j++) {
				File collection = collections[j];
				convertDirectory (collection);
			}
		}
		
		prtln ("** Conversion Summary **");
		prtln("\n\t" + converted + " files converted\n\t" + alreadyConverted + " already converted\n\t" + errors + " errors");
	}

	
	public void convertDirectory (File directory) {
		prtln ("\n ** converting directory: " + directory.getName());
		File [] files = directory.listFiles(new XMLFileFilter());
		int myConverted = 0;
		int myAlreadyConverted = 0;
		int myErrors = 0;
		for (int i=0;i<files.length;i++) {
			
			File file = files[i];
			String fileName = file.getName();
			try {
				if (convertFile(file)) {
					converted++;
					myConverted++;
				}
				else {
					alreadyConverted++;
					myAlreadyConverted++;
				}
				// prtln (fileName + " converted");
			} catch (Exception e) {
				prtln ("error converting " + fileName + ": " + e.getMessage());
				errors++;
				myErrors++;
			}
		}
		prtln("\n\t" + myConverted + " files converted\n\t" + myAlreadyConverted + " already converted\n\t" + myErrors + " errors");
	}

	
	/**
	 *  Convert file to new format if necessary
	 *
	 *@param  file           file to be converted
	 *@return                true if converted
	 *@exception  Exception  thrown if file cannot be converted or written to disk after conversion
	 */
	public boolean convertFile(File file)
		throws Exception {
		
		if (!file.exists())
			throw new Exception ("file to convert does not exist at " + file.toString());
		
		Document doc = Dom4jUtils.getXmlDocument(file);
		Element root = doc.getRootElement();

		if (root.element("statusEntries") != null) {
			// throw new Exception (file.getName() + " has already been converted");
			return false;
		}

		// Get the components of this file
		String recordId = root.element("recordID").getText();
		String lastEditor = root.element("lastEditor").getText();
		String status = root.element("status").getText();
		String statusNote = root.element("statusNote").getText();
		String lastTouchDate = root.element("lastTouchDate").getText();
		String isValid = "unknown";
		try {
			isValid = root.element("isValid").getText();
		} catch (Exception e) {}
		
		DcsDataRecord tmpRec = new DcsDataRecord (new File (dcsDataRecords, "tmp.xml"), framework, null, null);
		tmpRec.setId(recordId);
		
/* 		tmpRec.setLastEditor(lastEditor);
		tmpRec.setStatus(status);
		tmpRec.setStatusNote (statusNote); */
		
		tmpRec.updateStatus(status, statusNote, lastEditor);
		
		tmpRec.setLastTouchDate (lastTouchDate);
		// tmpRec.setIsValid(isValid);
		
		tmpRec.setSource(file);

		// show (tmpRec.getDocument());
		
		// prtln ("\t" + file.getName());
		tmpRec.flushToDisk();
		return true;
	}


	/**
	 *  debugging
	 *
	 *@param  args  The command line arguments
	 */
	public static void main(String[] args) {
		String path = "/devel/ostwald/projects/tmp/Suggestor-tmp/manage";
		if (args.length > 0) {
			path = args[0];
		}
/* 		DcsDataConverter c = new DcsDataConverter(path);
		c.convert(); */
		/* File file = new File ("/devel/ostwald/projects/tmp/Suggestor-tmp/rec1.xml");
		try {
			c.convertFile (file);
		} catch (Exception e) {
			prtln (e.getMessage());
		} */
	}


	/**
	 *  Sets the debug attribute of the DcsDataConverter object
	 *
	 *@param  db  The new debug value
	 */
	public static void setDebug(boolean db) {
		debug = db;
	}


	/**
	 *  Utility to show XML in pretty form
	 *
	 *@param  node  Description of the Parameter
	 */
	public static void show(Node node) {
		prtln(Dom4jUtils.prettyPrint(node));
	}


	/**
	 *  Print the string with trailing newline to std output
	 *
	 *@param  s  string to print
	 */
	private static void prtln(String s) {
		if (debug) {
			System.out.println("DcsDataConverter: " + s);
		}
	}
	
	
	public class DirectoryFilter implements FileFilter {
	/**
	 *  A FileFilter for xml files. Filters for files that end in '.xml' or '.XML'.
	 *
	 * @param  file  The file in question.
	 * @return       True if the file ends in '.xml' or '.XML'.
	 */
	public boolean accept(File file) {
		return (!file.getName().startsWith("."));
	}
}
}

