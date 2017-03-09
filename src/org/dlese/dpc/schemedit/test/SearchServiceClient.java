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

import org.dlese.dpc.serviceclients.webclient.WebServiceClient;
import org.dlese.dpc.serviceclients.webclient.WebServiceClientException;

import org.dlese.dpc.index.SimpleLuceneIndex;
import org.dlese.dpc.xml.Dom4jUtils;
import org.dlese.dpc.xml.XPathUtils;
import org.dlese.dpc.util.Files;
import org.dom4j.*;
import java.util.*;
import java.io.File;
import java.net.*;

/**
 *  Class to extract information from the NCS via webServices.
 *
 * @author    Jonathan Ostwald
 */
public class SearchServiceClient extends WebServiceClient {

	private static boolean debug = true;

	static String GLOBE_COLLECTION_KEY = "00o"; // key for ncs_collect collection in NCS
	private static String defaultServiceUrl = "http://dcs.dlese.org/schemedit/services/";


	/**  Constructor for the SearchServiceClient object */
	public SearchServiceClient() {
		this(defaultServiceUrl);
	}


	/**
	 *  Constructor for the SearchServiceClient object
	 *
	 * @param  baseWebServiceUrl  NOT YET DOCUMENTED
	 */
	public SearchServiceClient(String baseWebServiceUrl) {
		super(baseWebServiceUrl);
	}


	/**
	 *  Gets the nCSRecordByTitle attribute of the SearchServiceClient object
	 *
	 * @param  title          NOT YET DOCUMENTED
	 * @return                The nCSRecordByTitle value
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public List adnSearch () throws Exception {

		String serviceUrl = getBaseUrl() + "ddsws1-1?verb=Search&s=0&n=200";
		serviceUrl += "&ky=" + GLOBE_COLLECTION_KEY;

		URL url = new URL(serviceUrl);
		Document response = getResponseDoc(url);
		response = Dom4jUtils.localizeXml(response);
		// pp (response);
		Node node = response.selectSingleNode("/DDSWebService/Search/resultInfo/numReturned");
		int numReturned = Integer.parseInt(node.getText().trim());
		// prtln (numReturned + " records found");

		String metadataPath = "/DDSWebService/Search/results/record/metadata/itemRecord";
		List resultElements =  response.selectNodes(metadataPath);
		List results = new ArrayList();
		for (Iterator i=resultElements.iterator();i.hasNext();) {
			Element element = (Element)i.next();
			results.add (DocumentHelper.createDocument(element.createCopy()));
		}
		return results;
	}

	String report (List adnDocs) {
		String s = "";
		String NL = "\n";
		List items = new ArrayList ();
		for (Iterator i=adnDocs.iterator();i.hasNext();) {
			Document doc = (Document)i.next();
			Node idNode = doc.selectSingleNode ("/itemRecord/metaMetadata/catalogEntries/catalog/@entry");
			String id = idNode.getText();
			Node titleNode = doc.selectSingleNode ("/itemRecord/general/title");
			String title = titleNode.getText();
			items.add (title + "::" + id);
		}
	
		Collections.sort (items, new TitleComparator());
		
		for (Iterator i=items.iterator();i.hasNext();) {
			String item = (String)i.next();
			String[] splits = item.split("::");
			String title = splits[0].trim();
			String id = splits[1].trim();
			s += NL + title + "\t" + id;
		}
		return s;
	}
			
			
	/**
	 *  The main program for the SearchServiceClient class
	 *
	 * @param  args  The command line arguments
	 */
	public static void main(String[] args) {
		TesterUtils.setSystemProps();
		prtln ("SearchServiceClient");
		SearchServiceClient client = null;
		List results = null;
		try {
			client = new SearchServiceClient();
			results = client.adnSearch();

		} catch (Throwable t) {
			prtln(t.getMessage());
			return;
		}
		prtln (results.size() + " results found");
		String report = client.report (results);
		String path = "/Users/ostwald/devel/tmp/GlobeRecords.txt";
		try {
			Files.writeFile (report, new File (path));
			prtln ("wrote to " + path);
		} catch (Exception e) {
			prtln ("Couldn't write: " + e.getMessage());
			return;
		}
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  n  NOT YET DOCUMENTED
	 */
	private static void pp(Node n) {
		prtln(Dom4jUtils.prettyPrint(n));
	}


	/**
	 *  Description of the Method
	 *
	 * @param  s  Description of the Parameter
	 */
	private static void prtln(String s) {
		if (debug) {
			System.out.println(s);
		}
	}
	
	public class TitleComparator implements Comparator {
	
		public int compare(Object o1, Object o2) {
			
			String string1 = (String) o1;
			String string2 = (String) o2;
			
			return string1.toLowerCase().compareTo(string2.toLowerCase());
		}
	}
}

