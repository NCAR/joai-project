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
package org.dlese.dpc.schemedit.config;

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
public class FrameworkConfigConverter {

	private static boolean debug = true;

	public File originalsDir = null;
	public File convertedDir = null;
	private DocumentFactory df = DocumentFactory.getInstance();
	private int converted = 0;
	private int alreadyConverted = 0;
	private int errors = 0;
	private MetaDataFramework framework = null;


	/**
	 *  FrameworkConfigConverter Constructor. The dcsDataRecords param points to 
	 the root directory containing dcs_data records. the subdirectories are named for the
	 metadata formats of the directories they contain, which in turn hold directories for each
	 collection of that format.
	 
	 *
	 *@param  path  directory holding default record and records directory
	 */
	public FrameworkConfigConverter(String workingDirPath) throws Exception {

		File workingDir = new File(workingDirPath);
		if (!workingDir.exists())
			throw new Exception ("working Dir does not exist at " + workingDirPath);
		
		originalsDir = new File (workingDir, "originals");
		convertedDir = new File (workingDir, "converted");
		
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
					myConverted++;
				}
				else {
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
	* validates document in file, throwing error if not valid
	*/
	private Document getDocument (File file) throws Exception {
		if (!file.exists())
			throw new Exception ("file to convert does not exist at " + file.toString());
		
		Document doc = Dom4jUtils.getXmlDocument(file);
		Element root = doc.getRootElement();

		List pathNodes = doc.selectNodes ("/frameworkConfigRecord/schemaInfo/paths/path");
		if (pathNodes == null || pathNodes.size() == 0) {
			throw new Exception (file.getName() + " cannot be processed");
		} 
		
		List pathSpecNodes = doc.selectNodes ("/frameworkConfigRecord/schemaInfo/paths/path/pathSpec");
 		if ( pathSpecNodes != null && pathSpecNodes.size() > 0) {
			throw new Exception (file.getName() + " has already been converted");
		}
		return doc;
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
		
		Document doc = getDocument(file);
		Element schemaInfo = (Element) doc.selectSingleNode ("/frameworkConfigRecord/schemaInfo");
		// Node paths = doc.selectSingleNode ("/frameworkConfigRecord/schemaInfo/paths");
		Element paths = schemaInfo.element("paths");
		Element oldPaths = (Element) paths.detach();
		Element newPaths = schemaInfo.addElement ("paths");
		
		for (Iterator i=oldPaths.elementIterator();i.hasNext();) {
			Element oldPath = (Element)i.next();
			Element newPath = newPaths.addElement ("path");
			Element pathSpec = newPath.addElement("pathSpec");
			pathSpec.setText(oldPath.getText());
			for (Iterator a=oldPath.attributeIterator();a.hasNext();) {
				Attribute att = (Attribute)a.next();
				String attName = att.getName();
				String attValue = att.getText();
				pathSpec.addAttribute (attName, attValue);
			}
		}
		
		show(doc);
		prtln ("---");
		show (oldPaths);

		File convertedFile = new File (convertedDir, file.getName());
		Dom4jUtils.writeDocToFile(doc, convertedFile);
		return true;
	}


	/**
	 *  debugging
	 *
	 *@param  args  The command line arguments
	 */
	public static void main(String[] args) throws Exception {
		prtln ("starting");
		String workingDir = "/devel/ostwald/tmp/framework-config-convert";
		FrameworkConfigConverter converter = new FrameworkConfigConverter (workingDir);
		File file = new File (converter.originalsDir, "adn.xml");
		converter.convertFile(file);
		prtln ("ending");
	}


	/**
	 *  Sets the debug attribute of the FrameworkConfigConverter object
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
			System.out.println("FrameworkConfigConverter: " + s);
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

