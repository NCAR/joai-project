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

import org.dlese.dpc.schemedit.SchemEditUtils;

import org.dlese.dpc.util.Files;
import org.dlese.dpc.xml.Dom4jUtils;
import org.dlese.dpc.xml.XPathUtils;
import org.dlese.dpc.xml.XMLFileFilter;


import org.dom4j.*;

import java.io.File;

public class AsnUtils {
	private static boolean debug = true;
	
	public static void beautify (String path) {
		beautify (new File (path));
	}
	
	public static void beautify (File file) {
		Document doc = null;
		try {
			doc = Dom4jUtils.getXmlDocument (file);
		} catch (Exception e) {
			prtln (e.getMessage());
		}
		String xml = Dom4jUtils.prettyPrint(doc);
		try {
			// prtln (xml);
			Files.writeFile(xml, file);
			prtln ("wrote to " + file);
		} catch (Exception e) {
			prtln ("could not beautify (" + file + "): " + e.getMessage());
		}
	}
	
	public static void beautifyDir (String path) throws Exception {
		File dir = new File (path);
		if (!dir.isDirectory())
			throw new Exception ("Directory required (" + path + ")");
		File [] files = dir.listFiles(new XMLFileFilter());
		for (int i=0;i<files.length;i++) {
			try {
				beautify (files[i]);
			} catch (Exception e) {
				prtln ("Beautify error: " + e.getMessage());
			}
		}
	}
			
	
	public static void main (String [] args) throws Exception {
		prtln ("Utils");
		// String dir = "/Documents/Work/DLS/ASN/mast-docs";
		String dir = "/Documents/Work/DLS/ASN/standards-documents/v1.4.0/math";
/* 		String filename = "1995-National Science Education Standards (NSES)-Science-National Science Education Standard.xml";
		beautify (new File (dir, filename)); */
		beautifyDir (dir);

	}
	
	private static void prtln(String s) {
		if (debug) {
			SchemEditUtils.prtln(s, "CATServiceHelper");
		}
	}
}
