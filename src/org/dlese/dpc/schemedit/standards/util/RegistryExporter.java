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
package org.dlese.dpc.schemedit.standards.util;

import org.dlese.dpc.schemedit.standards.StandardsRegistry;
import org.dlese.dpc.schemedit.standards.asn.*;
import org.dlese.dpc.schemedit.SchemEditUtils;
import org.dlese.dpc.standards.asn.*;
import org.dlese.dpc.xml.XMLFileFilter;
import org.dlese.dpc.util.Files;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.*;
import java.io.File;
import java.io.FileFilter;

/**
 *  Dumps the contents of a {@link StandardsRegistry} contents to file in
 *  tab-delimited form (one row per standards doc).<p>
 *
 *  The tab-delimited file can be loaded into a spreadsheet to visualize the
 *  attributes (such as title, topic, author, year) of the documents. The recprd
 *  for each standards doc contains a "selected" column, which can be marked up
 *  to indicate what documents are to be used. <p>
 *
 *  After saving the file (again as tab-delimited) the file can be processed by
 *  the python script, standardsFilesTool.py, to copy selected standards
 *  documents into a specified destination directory, which can then be used to
 *  load the StandardsRegistry (and therefore the standardsSuggestionService)
 *  with the selected files.
 *
 *@author     Jonathan Ostwald
 *@created    June 25, 2009
 */
public class RegistryExporter {
	private static boolean debug = true;
	private StandardsRegistry reg = null;
	private String standardsDir = null;


	RegistryExporter(File dirFile) throws Exception {
		this (dirFile.getAbsolutePath());
	}
	
	/**
	 *  Constructor for the RegistryExporter object
	 *
	 *@param  dir            Points to a directory containing standards files.
	 *@exception  Exception  NOT YET DOCUMENTED
	 */
	RegistryExporter(String dir) throws Exception {
		this.standardsDir = dir;
		this.reg = StandardsRegistry.getInstance();
		this.reg.load(this.standardsDir);

	}



	/**
	 *  Join members into a string, separaed by the specified delimiter
	 *
	 *@param  list       list to be joined
	 *@param  delimiter  string to insert between memebers
	 *@return            a delimited string
	 */
	private String join(List list, String delimiter) {
		String s = "";
		int n = list.size();
		for (int i = 0; i < list.size(); i++) {
			s += (String) list.get(i);
			if (i < list.size() - 1) {
				s += delimiter;
			}
		}
		return s;
	}


	/**
	 *  Write a tab-delimited file containing a record for each standards document.
	 *
	 *@param  file  the file in which to export the standards docs contained in the
	 *      StandardsRegistry
	 */
	public void export(File file) {
		if (this.reg == null) {
			prtln("REG IS NULL");
		}
		Set keys = this.reg.getKeys();
		prtln(keys.size() + " documents registered");
		List rows = new ArrayList();

		List headerItems = new ArrayList();
		headerItems.add("selected");
		headerItems.add("topic");
		headerItems.add("author");
		headerItems.add("year");
		headerItems.add("title");
		headerItems.add("filePath");

		rows.add(join(headerItems, "\t"));

		// Collections.sort (keys);
		for (Iterator i = keys.iterator(); i.hasNext(); ) {
			String key = (String) i.next();
			AsnDocInfo docInfo = this.reg.getDocInfo(key);
			List rowItems = new ArrayList();
			rowItems.add("X");// selected by default
			rowItems.add(docInfo.getTopic());
			rowItems.add(docInfo.getAuthor());
			rowItems.add(docInfo.getCreated());
			rowItems.add(docInfo.getTitle());
			rowItems.add(docInfo.getSource().getAbsolutePath());
			rows.add(join(rowItems, "\t"));
		}
		String tabDelimited = join(rows, "\n");
		prtln(tabDelimited);
		try {
			Files.writeFile(tabDelimited, file);
			prtln("registry exported to " + file);
		} catch (Throwable t) {
			prtln("ERROR: unable to export to " + file + ": " + t.getMessage());
		}
	}


	/**
	 *  The main program for the RegistryExporter class
	 *
	 *@param  args           The command line arguments
	 *@exception  Exception  NOT YET DOCUMENTED
	 */
	public static void main(String[] args) throws Exception {
		prtln("\n------------------------------------\n");
		
		// asn2Export();
		mastDcsExport();
	
	}

	static void mastDcsExport () throws Exception {
		String standardsDir = "L:/ostwald/MAST/standardsFiles.v.1.4";
		RegistryExporter exporter = new RegistryExporter(standardsDir);
		String exportBasePath = "C:/tmp/ASN/";
		exporter.export(new File(exportBasePath, "Mast-v1.4"));
	}
	
	static void asn2Export () throws Exception {
		String standardsDir = "H:/Documents/IMLS/ASN-v2.0";
		RegistryExporter exporter = new RegistryExporter(standardsDir);
		String exportBasePath = "C:/tmp/ASN/";
		exporter.export(new File(exportBasePath, "ASN-v2.0"));
	}
	/**
	 *  NOT YET DOCUMENTED
	 *
	 *@param  s  NOT YET DOCUMENTED
	 */
	private static void prtln(String s) {
		if (debug) {
			// System.out.println("RegistryExporter: " + s);
			SchemEditUtils.prtln(s, "Registry");
		}
	}


}

