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
package org.dlese.dpc.ndr.toolkit;

import org.dlese.dpc.schemedit.SchemEditUtils;
import org.dlese.dpc.schemedit.test.TesterUtils;

import org.dlese.dpc.util.Files;

import org.dlese.dpc.xml.Dom4jUtils;
import org.dom4j.*;

import java.util.*;
import java.io.*;

/**
 *  Singleton utility class to determine the mime-type of a file based on its
 *  extension.<p>
 *
 *  Usage:
 *  <ul>
 *    <li> MimeTypes mt = MimeTypes.getInstance();
 *    <li> String mimeType = mt.getMimeType("foo.tif");
 *  </ul>
 *
 *
 * @author    Jonathan Ostwald
 */
public class MimeTypes {

	private static boolean debug = true;

	Document doc = null;
	Map map = null;
	private static MimeTypes instance = null;


	/**
	 *  Gets the instance attribute of the MimeTypes class
	 *
	 * @return    The instance value
	 */
	public static MimeTypes getInstance() {
		if (instance == null) {
			try {
				instance = new MimeTypes();
			} catch (Exception t) {
				prtlnErr("MimeTypes could not be instantiated: " + t.getMessage());
			}
		}
		return instance;
	}


	/**
	 *  Constructor for the MimeTypes object
	 *
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	private MimeTypes() throws Exception {

		try {
			String mappingsXML = Files.readFileFromJarClasspath("/org/dlese/dpc/ndr/toolkit/MIMETYPE-MAPPINGS.xml").toString();
			this.doc = Dom4jUtils.localizeXml(Dom4jUtils.getXmlDocument(mappingsXML));
		} catch (Exception e) {
			throw new Exception("unable to process local MIMETYPE-MAPPINGS.xml file");
		}

		String xpath = "/mime-mappings/mime-mapping";
		List mappings = doc.selectNodes(xpath);
		prtln(mappings.size() + " mime mappings found");
		map = new HashMap();
		for (Iterator i = mappings.iterator(); i.hasNext(); ) {
			Element mapping = (Element) i.next();
			String ext = mapping.elementTextTrim("extension");
			String mime = mapping.elementTextTrim("mime-type");
			map.put(ext, mime);
			// prtln(ext + ": " + mime);
		}
	}


	/**
	 *  Gets the mimeType for the provided filename.
	 *
	 * @param  filename  a string having an extention ("name.ext").
	 * @return           The mimeType value or null if either one is not found or
	 *      if the provided filename does not contain a dot (".").
	 */
	public String getMimeType(String filename) {
		if (filename == null || filename.indexOf(".") == -1)
			return null;
		String[] splits = filename.split("\\.");
		// prtln("splits length: " + splits.length);
		String ext = splits[splits.length - 1].toLowerCase();
		return (String) map.get(ext);
	}


	/**
	 *  Gets the mimeType associated with the provided file (based on the file's
	 *  name).
	 *
	 * @param  file  file named with a dot extension.
	 * @return       The mimeType value (e.g., "image/gif");
	 */
	public String getMimeType(File file) {
		if (file == null)
			return null;
		return getMimeType(file.getName());
	}


	/**
	 *  The main program for the MimeTypes class
	 *
	 * @param  args           The command line arguments
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public static void main(String[] args) throws Exception {
		MimeTypes mt = MimeTypes.getInstance();
		String in = ".tiff";
		if (args.length > 0)
			in = args[0];
		String out = mt.getMimeType(in);
		prtln("in: " + in);
		prtln("out: " + out);
		
		prtln (mt.getMimeType("foo.pdf"));
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  node  NOT YET DOCUMENTED
	 */
	private static void pp(Node node) {
		prtln(Dom4jUtils.prettyPrint(node));
	}


	/**
	 *  Description of the Method
	 *
	 * @param  s  Description of the Parameter
	 */
	private static void prtln(String s) {
		if (debug) {
			SchemEditUtils.prtln(s, "");
		}
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  s  NOT YET DOCUMENTED
	 */
	private static void prtlnErr(String s) {
		SchemEditUtils.prtln(s, "MimeTypes");
	}
}

