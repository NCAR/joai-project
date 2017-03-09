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

import org.dlese.dpc.schemedit.*;
import org.dlese.dpc.schemedit.test.*;
import org.dlese.dpc.xml.*;
import org.dlese.dpc.util.*;
import org.dlese.dpc.util.strings.*;
import org.dlese.dpc.oai.OAIUtils;

import org.dom4j.*;

import java.util.*;
import java.io.*;
import java.util.regex.*;
import java.net.*;
import java.text.*;

/**
 *  Utilities for manipulating XPaths, represented as String
 *
 *@author    ostwald
 */
public class CollectionFileConverter {

	private static boolean debug = true;
	public MetaDataFramework framework;
	
	
	public CollectionFileConverter (MetaDataFramework framework) {
		this.framework = framework;
		if (framework == null) {
			prtln ("Initialization ERROR: metadataFramework is missing!");
		}
	}
	
	public static String showArray (String [] items) {
		String ret = "";
		if (items != null)
			for (int i=0;i<items.length;i++) {
				ret += items[i] ;
				if (i == (items.length - 2) && items.length > 1)
					ret += " and ";
				else if (i < (items.length - 2) && items.length > 2)
					ret += ", ";
			}
		return ret;
	}
	
	public void convert (String path) {
		try {
			convertDir (new File (path));
		} catch (Throwable t) {
			System.out.println ("WARNING: convert error: " + t.getMessage());
		}
	}
	
	private void convertFile (File file) {
		prtln ("convertFile " + file.toString());
		try {
			if (this.framework == null) {
				throw new Exception ("Initialization ERROR: metadataFramework is missing!");
			}
			
			Document doc = Dom4jUtils.getXmlDocument(file);
			if (doc == null) {
				throw new Exception("could not parse provided recordXML");
			}
			
			String rootElementName = framework.getRootElementName();
	
			doc = Dom4jUtils.localizeXml(doc, rootElementName);
			if (doc == null) {
				throw new Exception("doc could not be localized - please unsure the record's root element contains namespace information");
			}
			
			String idPath = framework.getIdPath();
			prtln ("idPath: " + idPath);
			Node idNode = doc.selectSingleNode(idPath);
			if (idNode == null)
				throw new Exception ("idNode not found");
			
			String id = idNode.getText();
			String newId = "DCS-COLLECTION" + id.substring ("DLESE-COLLECTION".length());
			prtln ("id: " + id + ", new id: " + newId);
			
			idNode.setText(newId);

			doc = framework.getWritableRecord(doc);
			// prtln (Dom4jUtils.prettyPrint (doc));
			
			Dom4jUtils.writeDocToFile (doc, file);
			file.renameTo(new File (file.getParentFile(), newId+".xml"));
			
		} catch (Throwable t) {
			prtln ("convertFile error: " + t.getMessage());
			t.printStackTrace();
		}

	}
		
	
	private void convertDir (File dir) {
		prtln ("convertDir: " + dir.toString());
		File [] files = dir.listFiles ();
		XMLFileFilter xmlFileFilter = new XMLFileFilter ();
		for (int i=0;i<files.length;i++) {
			File file = files[i];
			if (file.isDirectory())
				convertDir (file);
			else if (xmlFileFilter.accept(file)) {
				String fileName = file.getName();
				if (fileName.startsWith("DLESE-COLLECTION")) {
					convertFile (file);
				}
			}
		}
	}
	
	/**
	 *  The main program for the CollectionFileConverter class
	 *
	 *@param  args           The command line arguments
	 *@exception  Exception  Description of the Exception
	 */
	public static void main(String[] args) throws Exception {
		String xmlFormat = "dcs_data";
		String collectionRecordsLocation = null;
		FrameworkTester ft = new FrameworkTester (xmlFormat);
		MetaDataFramework framework = ft.getFramework();
		CollectionFileConverter tester = new CollectionFileConverter (framework);
		if (xmlFormat.equals("dlese_collect")) {
			collectionRecordsLocation = "/devel/ostwald/records/dlese_collect";
		}
		else if (xmlFormat.equals("dcs_data")) {
			collectionRecordsLocation = "/devel/ostwald/records-convert-test/dcs_data/dlese_collect";
		}
		else {
			prtln ("unrecognized xmlFormat: " + xmlFormat);
			return;
		}
			
		File collectDir = new File (collectionRecordsLocation);
		tester.convertDir (collectDir);

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

