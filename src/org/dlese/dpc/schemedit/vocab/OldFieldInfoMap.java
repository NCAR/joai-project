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
*  Populates  a {@line FieldInfoMap} by reading from a file specified in Framework config file (with name ending
 *  in "filename-list.xml"). The filename-list file contained:
 *  <ul>
 *    <li> a directoryURL - pointing to a directory where the individual fields files would be located
 *    <li> a list of file names that were appended to the directoryURL to access the individual fields files.
 *
 *  </ul>
 *  An example filename-list file can be found at: http://www.dlese.org/Metadata/adn-item/0.6.50/build/filename-list.xml
 *
 * @author     ostwald
 *
 */
public class OldFieldInfoMap extends FieldInfoMap {
	private static boolean debug = true;


	/**
	 *  Constructor for the OldFieldInfoMap object
	 *
	 * @param  uri  Description of the Parameter
	 */
	public OldFieldInfoMap(String uri) {
		this();
		directoryUri = uri;
	}


	/**  Constructor for the OldFieldInfoMap object */
	public OldFieldInfoMap() {
		map = new HashMap();
	}


	/**
	 *  Read a listing of URIs Fields files from directoryUri and then loads each of the listed files as
	 *  FieldInfoReader objects, which are stored in a map.
	 *
	 * @exception  Exception  Description of the Exception
	 */
	public void init()
		 throws Exception {
		map.clear();
		URI uri = new URI(directoryUri);
		Document fieldsFileListing = SchemEditUtils.getLocalizedXmlDocument(uri);

		// print xml file for debugging
		prtln("FieldsDirectory at : " + uri.toString());
		// prtln (Dom4jUtils.prettyPrint(fieldsFileListing));


		Node filesNode = fieldsFileListing.selectSingleNode("metadataFieldsInfo/files");
		if (filesNode == null) {
			prtln("no filesNode found");
			return;
		}

		Element filesElement = (Element) filesNode;
		String baseUri = filesElement.attributeValue("uri");

		// load each of the files in the fields file listing
		for (Iterator i = filesElement.elementIterator(); i.hasNext(); ) {
			Node fileNode = (Node) i.next();
			String fileName = fileNode.getText();
			URI myUri = new URI(baseUri + fileName);
			try {
				FieldInfoReader reader = new FieldInfoReader(myUri);
				putFieldInfo(reader.getPath(), reader);
				// prtln("path: " + reader.getPath());
			} catch (Exception e) {
				prtln(e.getMessage());
			}
		}
		prtln("OldFieldInfoMap initialized - " + map.size() + " fields files read");
	}


	/**
	 *  Read a set of fields files
	 *
	 * @param  args           The command line arguments
	 * @exception  Exception  Description of the Exception
	 */
	public static void main(String[] args)
		 throws Exception {
		setDebug(true);
		prtln("OldFieldInfoMap");

		String devel_adn = "file:/devel/ostwald/metadata-frameworks/metadata-ui/adn/filename-list.xml";

		String dlese_adn_0_6_50 = "http://www.dlese.org/Metadata/adn-item/0.6.50/build/filename-list.xml";
		String dlese_news_opps_1_0_00 = "http://www.dlese.org/Metadata/news-opps/1.0.00/build/filename-list.xml";
		String dlese_dlese_anno_0_1_01 = "http://www.dlese.org/Metadata/annotation/0.1.01/build/filename-list.xml";
		String dlese_dlese_anno_1_0_0 = "http://www.dlese.org/Metadata/annotation/1.0.00/build/filename-list.xml";

		String uri = dlese_dlese_anno_1_0_0;

		OldFieldInfoMap fim = new OldFieldInfoMap(uri);
		try {
			fim.init();
		} catch (Exception e) {
			prtln("init error: " + e.getMessage());
			return;
		}
		// fim.downLoadFieldsFiles();
	}


	/**
	 *  Utility to download fields files to local disk.
	 *
	 * @exception  Exception  Description of the Exception
	 */
	void downLoadFieldsFiles()
		 throws Exception {
		prtln("hello world");
		String format = "dlese_anno";
		File dir = new File("/devel/ostwald/metadata-frameworks/metadata-ui/" + format + "/fields-files/");
		if (!dir.exists() && !dir.mkdirs()) {
			throw new Exception("could not make directory at " + dir.getAbsolutePath());
		}
		List fieldInfoReaders = getAllFieldInfo();
		prtln("there are " + fieldInfoReaders.size() + " readers");
		for (Iterator i = getAllFieldInfo().iterator(); i.hasNext(); ) {
			FieldInfoReader fir = (FieldInfoReader) i.next();
			Document doc = SchemEditUtils.getLocalizedXmlDocument(fir.uri);
			String path = fir.uri.getPath();
			String[] splits = path.split("/");
			String fileName = null;
			if (splits != null && splits.length > 0) {
				fileName = splits[splits.length - 1];
			}
			if (fileName == null) {
				throw new Exception("could not get filename from " + path);
			}
			File dest = new File(dir, fileName);
			Dom4jUtils.writePrettyDocToFile(doc, dest);
			prtln("wrote " + dest.toString());
		}
	}


	/**
	 *  Output a line of text to standard out, with datestamp, if debug is set to true.
	 *
	 * @param  s  The String that will be output.
	 */
	protected static void prtln(String s) {
		if (debug) {
			System.out.println("OldFieldInfoMap: " + s);
		}
	}
}

