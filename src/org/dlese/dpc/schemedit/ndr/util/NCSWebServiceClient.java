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
package org.dlese.dpc.schemedit.ndr.util;

import org.dlese.dpc.ndr.apiproxy.NDRConstants;
import org.dlese.dpc.serviceclients.webclient.WebServiceClient;
import org.dlese.dpc.serviceclients.webclient.WebServiceClientException;

import org.dlese.dpc.ndr.reader.NSDLCollectionReader;
import org.dlese.dpc.index.SimpleLuceneIndex;
import org.dlese.dpc.ndr.NdrUtils;
import org.dlese.dpc.xml.Dom4jUtils;
import org.dlese.dpc.xml.XPathUtils;
import org.dom4j.*;
import java.util.*;
import java.io.File;
import java.net.*;

/**
 *  Class to extract information from the NCS via webServices.
 *
 * @author    Jonathan Ostwald
 */
public class NCSWebServiceClient extends WebServiceClient {

	private static boolean debug = true;

	static String NCS_NSDL_COLLECTION_KEY = "1201216476279"; // key for ncs_collect collection in NCS
	private static String defaultServiceUrl = "http://ncs.nsdl.org/mgr/services/";


	/**  Constructor for the NCSWebServiceClient object */
	public NCSWebServiceClient() {
		this(defaultServiceUrl);
	}


	/**
	 *  Constructor for the NCSWebServiceClient object
	 *
	 * @param  baseWebServiceUrl  NOT YET DOCUMENTED
	 */
	public NCSWebServiceClient(String baseWebServiceUrl) {
		super(baseWebServiceUrl);
	}


	/**
	 *  Gets an NCSCollectReader instance for a given resourceUrl.<p>
	 *
	 *  Uses web service to find a metadata record for the resourceUrl and then
	 *  creates a reader object from the response.
	 *
	 * @param  resourceUrl  Description of the Parameter
	 * @return              The nCSRecord value
	 */
	public NCSCollectReader getNCSRecord(URL resourceUrl) {
		Document response = null;
		try {
			// response = webServiceSearch(makeServiceUrl(resourceUrl.toString()));
			URL url = new URL(makeServiceUrl(resourceUrl.toString()));
			response = getResponseDoc(url);
		} catch (MalformedURLException e) {
			prtln("Bad URL: " + e);
		} catch (WebServiceClientException e) {
			prtln("getNCSCollectionID Error: " + e.getMessage());
		}
		if (response == null)
			return null;

		String metadataPath = "/DDSWebService/Search/results/record/metadata";
		Element metadata =
			(Element) response.selectSingleNode(metadataPath);
		if (metadata == null) {
			// throw new Exception ("metadataPath element not found at: " + metadataPath);
			prtln("metadataPath element not found at: " + metadataPath);
			return null;
		}
		Element root = metadata.element("record").createCopy();
		return new NCSCollectReader(DocumentHelper.createDocument(root));
	}


	/**
	 *  Returns NCSCollectReader instance for NCS Collect record having provided
	 *  id.
	 *
	 * @param  recId          Description of the Parameter
	 * @return                The nCSRecord value
	 * @exception  Exception  Description of the Exception
	 */
	public NCSCollectReader getNCSRecord(String recId) throws Exception {
		// prtln ("getNCSRecord() for " + recId);

		String serviceUrl = getBaseUrl() + "ddsws1-1?verb=GetRecord";
		serviceUrl += "&id=" + recId;
		serviceUrl += "&ky=" + NCS_NSDL_COLLECTION_KEY;

		// Document doc = webServiceSearch(serviceUrl);
		URL url = new URL(serviceUrl);
		Document doc = getResponseDoc(url);
		String metadataPath = "/DDSWebService/GetRecord/record/metadata";
		Element metadata =
			(Element) doc.selectSingleNode(metadataPath);
		if (metadataPath == null) {
			throw new Exception("metadataPath element not found at: " + metadataPath);
		}
		Element root = metadata.element("record").createCopy();
		return new NCSCollectReader(DocumentHelper.createDocument(root));
	}


	/**
	 *  Gets the nCSRecordByTitle attribute of the NCSWebServiceClient object
	 *
	 * @param  title          NOT YET DOCUMENTED
	 * @return                The nCSRecordByTitle value
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public NCSCollectReader getNCSRecordByTitle(String title) throws Exception {

		String serviceUrl = getBaseUrl() + "ddsws1-1?verb=Search&s=0&n=10";
		serviceUrl += "&q=" + URLEncoder.encode(title);
		serviceUrl += "&ky=" + NCS_NSDL_COLLECTION_KEY;

		URL url = new URL(serviceUrl);
		Document response = getResponseDoc(url);
		Node node = response.selectSingleNode("/DDSWebService/Search/resultInfo/numReturned");
		int numReturned = Integer.parseInt(node.getText().trim());
		if (numReturned > 1) {
			throw new Exception(numReturned + " records found");
		}

		// prtln (numReturned + " results returned");
		String metadataPath = "/DDSWebService/Search/results/record/metadata";
		Element metadata =
			(Element) response.selectSingleNode(metadataPath);
		if (metadata == null) {
			throw new Exception("metadataPath element not found at: " + metadataPath);
		}
		Element root = metadata.element("record").createCopy();
		return new NCSCollectReader(DocumentHelper.createDocument(root));
	}


	/**
	 *  Finds the collection id for the provided collection resource url.<p>
	 *
	 *
	 *
	 * @param  resourceUrl  Description of the Parameter
	 * @return              The nCSCollectionID value
	 */
	public String getNCSCollectionID(String resourceUrl) {
		String id = "";
		Document response = null;
		try {
			// response = webServiceSearch(makeServiceUrl(resourceUrl));
			URL url = new URL(makeServiceUrl(resourceUrl));
			prtln(url.toString());
			response = getResponseDoc(url);
			pp(response);
		} catch (MalformedURLException e) {
			prtln("Bad URL: " + e);
		} catch (WebServiceClientException e) {
			prtln("getNCSCollectionID Error: " + e.getMessage());
		}
		if (response != null) {
			Node node = response.selectSingleNode("/DDSWebService/Search/results/record/head/id");
			if (node != null) {
				id = node.getText();
			}
		}
		return (id != null ? id : "");
	}

	/**
	 *  Gets a list of record ids from the NCSL Collections collection.
	 *
	 * @return                The nCSCollectionIDs value
	 * @exception  Exception  Description of the Exception
	 */
	public List getNCSCollectionRecordIDs () throws Exception {
		return getRecordIDs (NCS_NSDL_COLLECTION_KEY);
	}
	
	

	/**
	 *  Gets a list of record ids from the specified collection.
	 *
	 * @return                The nCSCollectionIDs value
	 * @exception  Exception  Description of the Exception
	 */
	public List getRecordIDs(String collectionKey) throws Exception {

		Document response = null;
		String baseUrl = this.getBaseUrl() + "ddsws1-1?verb=Search&q=";
		baseUrl += "&ky=" + collectionKey;
		int start = 0;
		int numToGet = 50;
		int totalNumResults = 1;
		int numResults = 0;
		List ids = new ArrayList();
		// step through the records for this collection by grabbing subsequent chunks
		while (numResults < totalNumResults) {
			String urlStr = baseUrl + "&s=" + start + "&n=" + numToGet;
			URL url = new URL(urlStr);

			// get the next numToGet records
			response = getResponseDoc(url);

			Node totalNRnode = response.selectSingleNode("/DDSWebService/Search/resultInfo/totalNumResults");
			totalNumResults = Integer.parseInt(totalNRnode.getText());
			List idNodes = response.selectNodes("/DDSWebService/Search/results/record/head/id");
			for (Iterator i = idNodes.iterator(); i.hasNext(); ) {
				String id = ((Node) i.next()).getText();
				if (ids.contains(id)) {
					throw new Exception("dup id found: " + id);
				}
				ids.add(id);
			}
			start = start + numToGet;
			numResults = ids.size();
		}
		return ids;
	}


	/**
	 *  Gets the IDS of the Collections of the NCS
	 *  http://ncs.nsdl.org/mgr/services/ddsws1-1?verb=ListCollections
	 * @return                The nCSCollectionIDs value
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public List getNCSCollectionIDs() throws Exception {

		Document response = null;
		String urlStr = this.getBaseUrl() + "ddsws1-1?verb=ListCollections";
		URL url = new URL(urlStr);
		response = getResponseDoc(url);
		List ids = new ArrayList();
		// step through the records for this collection by grabbing subsequent chunks

		List idNodes = 
			response.selectNodes("/DDSWebService/ListCollections/collections/collection/searchKey");
		for (Iterator i = idNodes.iterator(); i.hasNext(); ) {
			String id = ((Node) i.next()).getText().trim();
			if (ids.contains(id)) {
				throw new Exception("dup id found: " + id);
			}
			ids.add(id);

		}
		return ids;
	}


	/**
	 *  Convenience method for {@link makeServiceUrl(String, String)}.
	 *
	 * @param  resourceUrl  Description of the Parameter
	 * @return              Description of the Return Value
	 */
	String makeServiceUrl(String resourceUrl) {
		return makeServiceUrl(resourceUrl, NCS_NSDL_COLLECTION_KEY);
	}


	/**
	 *  Creates a url for use in a webservice call to find records having provided
	 *  rsourceUrl and collection key.
	 *
	 * @param  resourceUrl  Description of the Parameter
	 * @param  colKey       Description of the Parameter
	 * @return              Description of the Return Value
	 */
	String makeServiceUrl(String resourceUrl, String colKey) {
		String baseUrl = getBaseUrl() + "ddsws1-1?verb=Search&s=0&n=10";
		baseUrl += "&q=urlenc:" + SimpleLuceneIndex.encodeToTerm(resourceUrl);
		baseUrl += "&ky=" + colKey;
		return baseUrl;
	}


	/**  A unit test for JUnit */
	void tester1() {
		// String resourceUrl = "http://dlcenter.larc.nasa.gov/"; // works with no collection
		// String resourceUrl = "http://www.alexandria.ucsb.edu/"; // works WITH collection
		String resourceUrl = "http://www.atomicarchive.com/"; // does not work?
		// String resourceUrl = "http://medicine.plosjournals.org/perlserv/?request=index-html&issn=1549-1676"; // does not work
		String id = getNCSCollectionID(resourceUrl);
		prtln("id: " + id);
	}


	/**
	 *  A unit test for JUnit
	 *
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	void tester2() throws Exception {
		URL url = new URL("http://www.atomicarchive.com/");
		NCSCollectReader reader = getNCSRecord(url);
		prtln(reader.getRecordID());
	}


	/**
	 *  A unit test for JUnit
	 *
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	void tester3() throws Exception {
		List ids = getNCSCollectionRecordIDs();
		for (Iterator i = ids.iterator(); i.hasNext(); )
			prtln((String) i.next());
	}


	/**
	 *  A unit test for JUnit
	 *
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	void tester4() throws Exception {
		// String title = "TeachEngineering: Resources for K-12";
		String title = "Atomic Archive";
		NCSCollectReader reader = getNCSRecordByTitle(title);
		prtln(reader.getRecordID());
	}


	/**
	 *  A unit test for JUnit
	 *
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	void tester5() throws Exception {
		String id = "NSDL-COLLECTION-920106";
		NCSCollectReader reader = getNCSRecord(id);
		prtln(reader.getTitle());
	}


	/**
	 *  The main program for the NCSWebServiceClient class
	 *
	 * @param  args  The command line arguments
	 */
	public static void main(String[] args) {
		File propFile = null; // propFile must be assigned!
		NdrUtils.setup (propFile);
		try {
			NCSWebServiceClient client = new NCSWebServiceClient();
			client.tester5();
		} catch (Throwable t) {
			prtln(t.getMessage());
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
}

