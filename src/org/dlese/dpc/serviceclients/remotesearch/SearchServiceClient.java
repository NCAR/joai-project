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
package org.dlese.dpc.serviceclients.remotesearch;

import org.dlese.dpc.serviceclients.webclient.*;
import org.dlese.dpc.serviceclients.remotesearch.reader.ADNItemDocReader;

import java.util.*;
import org.dlese.dpc.xml.*;
import org.dlese.dpc.index.*;
import java.net.*;

import org.dom4j.*;

/**
 *  SearchServiceClient performs searches for records using a DDS Web Service
 *  (version 1.0), returning results as ADNItemDocReader instances.<p>
 *
 *  SearchServiceClient employs the WebServiceClient to do the actual search
 *  (via the <b>doSearch</b> method (so this class can be thought of as a
 *  wrapper for the Search Web Service).
 *
 *@author    ostwald
 */
public class SearchServiceClient {
	private static boolean debug = true;

	WebServiceClient wsc = null;


	/**
	 *  Constructor for the SearchServiceClient object
	 *
	 *@param  searchServiceBaseUrl  DDS Web Service URL
	 */
	public SearchServiceClient(String searchServiceBaseUrl) {
		try {
			wsc = new WebServiceClient(searchServiceBaseUrl);
			if (wsc == null) {
				throw new Exception("falied to create a WebServiceClient.  searchServiceBaseUrl: " + searchServiceBaseUrl);
			}
		} catch (Throwable t) {
			prtln("initialization error: " + t.getMessage());
		}
	}


	/**
	 *  Gets the webServiceClient attribute of the SearchServiceClient object
	 *
	 *@return    The webServiceClient value
	 *@see       WebServiceClient
	 */
	public WebServiceClient getWebServiceClient() {
		return wsc;
	}


	/**
	 *  Performs a Web Service Search (using the {@link
	 *  org.dlese.dpc.serviceclients.remotesearch.WebServiceClient}) and returns
	 *  results as a List of {@link org.dlese.dpc.serviceclients.remotesearch.reader.ADNItemDocReader}
	 *  instances.
	 *
	 *@param  s  Description of the Parameter
	 *@return    found records as ADNItemDocReader instances.
	 */
	public List searchDocs(String s) {
		List results = new ArrayList();
		Document webServiceResultDoc = null;
		try {
			prtln("about to call webserviceClient.doSearch with " + s);
			webServiceResultDoc = Dom4jUtils.localizeXml(wsc.doSearch(s));
		} catch (WebServiceClientException e) {
			prtln("Error: " + e.getMessage());
			return results;
		}
		if (webServiceResultDoc == null) {
			prtln("doSearch returned null");
			return results;
		}
		
		List resultRecords = webServiceResultDoc.selectNodes("/DDSWebService/Search/results/record");
		if (resultRecords.size() == 0) {
			return results;
		}

		for (Iterator i = resultRecords.iterator(); i.hasNext(); ) {
			Element resultRec = (Element) i.next();
			Element idElement = (Element) resultRec.selectSingleNode("head/id");
			String id = idElement.getText();
			Element collectionElement = (Element) resultRec.selectSingleNode("head/collection");
			String collection = collectionElement.getText();

			// clone the root and create a new document with it
			Element itemRoot = (Element) resultRec.selectSingleNode("metadata/itemRecord");

			Element rootClone = (Element) itemRoot.clone();
			Document itemRecordDoc = DocumentFactory.getInstance().createDocument(rootClone);
			results.add(new ADNItemDocReader(id, collection, itemRecordDoc, null));
		}

		return results;
	}


	/**
	 *  Description of the Method
	 */
	public void destroy() {
		wsc = null;
	}


	/**
	 *  Sets the debug attribute
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
	private static void prtln(String s) {
		if (debug) {
			// System.out.println("SearchServiceClient: " + s);
			System.out.println(s);
		}
	}


	/**
	 *  Constructs a query string to find records in a specified collection having a specified url in either the
	 primaryUrl or mirrorUrl fields
	 *
	 *@param  url         url to search for
	 *@param  collection  collection to search for
	 *@return             query string to submit to searchDocs method
	 */
	static String makeUrlQueryStr(String url, String collection) {
		String urlEnc = SimpleLuceneIndex.encodeToTerm(url, false);
		String q = "(url:" + urlEnc;
		q += "+OR+urlMirrorsEncoded:" + urlEnc;
		q += ")+AND+collection:0" + collection;
		return "q=" + q + "&s=0&n=500";
	}


	/**
	 *  Given a url and a collection, performs remote search and returns results.
	 *
	 *@param  url         Description of the Parameter
	 *@param  collection  Description of the Parameter
	 *@return             Description of the Return Value
	 */
	List urlSearch(String url, String collection) {
		String queryStr = makeUrlQueryStr(url, collection);
		return searchDocs(queryStr);
	}


	/**
	 *  SearchServiceClient tester
	 *
	 *@param  args           The command line arguments
	 *@exception  Exception  Description of the Exception
	 */
	public static void main(String[] args)
		throws Exception {
		String serviceUrl = "http://localhost/schemedit/services/ddsws1-0";
		SearchServiceClient rs = new SearchServiceClient(serviceUrl);

		String queryStr;

		String url = "h*fooberry*";
		if (args.length > 0) {
			url = args[0];
		}
		String collection = "adnselect";
		List results = rs.urlSearch(url, collection);

		prtln("\nResults (" + results.size() + ")");
		for (Iterator i = results.iterator(); i.hasNext(); ) {
			ADNItemDocReader reader = (ADNItemDocReader) i.next();
			prtln("\n" + reader.getId());
			prtln("\tcollection: " + reader.getCollection());
			prtln("\tprimaryUrl: " + reader.getUrl());
			prtln("\tmirrorUrls:");
			for (Iterator m = reader.getMirrorUrls().iterator(); m.hasNext(); ) {
				prtln("\t\t" + (String) m.next());
			}
		}
		prtln("\nResults (" + results.size() + ")");
	}

}

