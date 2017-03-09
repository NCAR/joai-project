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

import java.util.*;
import org.dlese.dpc.vocab.*;
import org.dlese.dpc.xml.*;

import org.dom4j.Element;
import org.dom4j.Document;

/**
 *  RemoteSearcher plays a role anaogous to {@link
 *  org.dlese.dpc.index.SimpleLuceneIndex} in the DDS, only it performs searches
 *  for records using a DDS Web Service (version 1.0). RemoteSearcher employs the WebService
 *  client to do the actual search (via the <b>urlCheck</b> method (so this
 *  class can be thought of as a wrapper for the urlCheck Service).<p>
 *
 *  A use case of RemoteSearcher follows:
 *  <ol>
 *    <li> the user submits a query, which at this time is in the form of a URL
 *    (including wild cards)</li>
 *    <li> the query is given to {@link #searchDocs}, which in turn calls the
 *    UrlCheck method of the {@link WebServiceClient}, and then</li>
 *    <li> the result Document is parsed into an array of {@link
 *    RemoteResultDoc} </li>
 *  </ol>
 *
 *
 *@author    ostwald
 */
public class RemoteSearcher {
	private static boolean debug = true;

	WebServiceClient wsc = null;
	MetadataVocab vocab = null;

	/**
	 *  Gets the vocab attribute of the RemoteSearcher object
	 *
	 *@return    The vocab value
	 */
	public MetadataVocab getVocab() {
		return vocab;
	}


	/**
	 *  Constructor for the RemoteSearcher object
	 *
	 *@param  ddsWebServicesBaseUrl  DDS Web Service URL
	 *@param  vocab                  MetadataVocab object used to parse search
	 *      results
	 */
	public RemoteSearcher(String ddsWebServicesBaseUrl, MetadataVocab vocab) {
		try {
			wsc = new WebServiceClient(ddsWebServicesBaseUrl);
			if (wsc == null)
				throw new Exception ("falied to create a WebService.  ddsWebServicesBaseUrl: " + ddsWebServicesBaseUrl);
		} catch (Throwable t) {
			prtln ("initialization error: " + t.getMessage());
		}
		WebServiceClient.setDebug(debug);
		this.vocab = vocab;
	}


	/**
	 *  Gets the webServiceClient attribute of the RemoteSearcher object
	 *
	 *@return    The webServiceClient value
	 *@see       WebServiceClient
	 */
	public WebServiceClient getWebServiceClient() {
		return wsc;
	}


	/**
	 *  analogus to SimpleLuceneIndex.searchDoc, performs a search and returns an
	 *  array of {@link RemoteResultDoc} objects that represent the results of the
	 *  search. <p>
	 *
	 *  Note: the urlCheck WebService returns information about duplicate records
	 *  (that don't match the query, but which refer to the same resource) returned
	 *  as an <b>alsoCatalogedBy</b> element of <b>MatchingRecord</b> . These
	 *  duplicates are treated as search results by searchDocs - they are expanded
	 *  into RemoteResultDoc objects and added to the results returned.
	 *
	 *@param  s  Description of the Parameter
	 *@return    Description of the Return Value
	 */
	public RemoteResultDoc[] searchDocs(String s) {
		RemoteResultDoc[] emptyResults = new RemoteResultDoc[]{};
		if (s == null) {
			prtln("RemoteResultDoc: got null for the search parameter");
			return emptyResults;
		}
		Document webServiceResultDoc = null;
		try {
			prtln("about to call webserviceClient.urlCheck with " + s);
			webServiceResultDoc = wsc.urlCheck(s);
		} catch (WebServiceClientException e) {
			prtln(e.getMessage());
			return emptyResults;
		}
		if (webServiceResultDoc == null) {
			prtln("urlCheck returned null");
			return emptyResults;
		}
		// parse results document
		List matchingRecords = webServiceResultDoc.selectNodes("//results/matchingRecord");
		if (matchingRecords.size() == 0) {
			prtln("urlCheck returned no matches");
			return emptyResults;
		}
		else {
			ArrayList matches = new ArrayList();
			for (Iterator i = matchingRecords.iterator(); i.hasNext(); ) {
				Element matchingRecord = (Element) i.next();
				// prtln ("about to parse matchingRecord:\n" + Dom4jUtils.prettyPrint(matchingRecord));

				RemoteResultDoc m = new RemoteResultDoc(matchingRecord, this);
				matches.add(m);

				// now deal with the alsoCatalogedBys
				List alsoCatalogedBys = matchingRecord.selectNodes("//additionalMetadata[@realm='adn']/alsoCatalogedBy");
				if (alsoCatalogedBys == null) {
					// prtln ("no alsoCatalogedBys found");
				}
				else {
					// prtln (alsoCatalogedBys.size() + " alsoCatalogedBys found");
					for (Iterator n = alsoCatalogedBys.iterator(); n.hasNext(); ) {
						Element ae = (Element) n.next();
						String collection = ae.attributeValue("collectionLabel");
						String id = ae.getText();
						RemoteResultDoc am = new RemoteResultDoc(id, m.getUrl(), collection, this);
						matches.add(am);
					}
				}
			}
			prtln("searchDocs found " + matches.size() + " items");
			return (RemoteResultDoc[]) matches.toArray(emptyResults);
		}
	}


	/**
	 *  Gets the record attribute of the RemoteSearcher object
	 *
	 *@param  id  Description of the Parameter
	 *@return     The record value
	 */
	public GetRecordResponse getRecord(String id) {
		return getDocument(id);
	}


	/**
	 *  Gets the document attribute of the RemoteSearcher object
	 *
	 *@param  id  Description of the Parameter
	 *@return     The document value
	 */
	public GetRecordResponse getDocument(String id) {
		try {
			return wsc.getRecord(id);
		} catch (Exception e) {
			prtln("getDocument() failed to retrieve " + id + "\n\t" + e.getMessage());
			return null;
		}
	}

	public void destroy () {
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
			System.out.println("RemoteSearcher: " + s);
		}
	}


	/**
	 *  RemoteSearcher tester
	 *
	 *@param  args  The command line arguments
	 */
	public static void main(String[] args) {
		String serviceUrl = "http://tremor.dpc.ucar.edu:8688/dds/services/ddsws1-0";
		RemoteSearcher rs = new RemoteSearcher(serviceUrl, null);

		// tester for urlCheck service
		String url = "";
		if (args.length > 0) {
			url = args[0];
		}
		else {
			url = "http://oceanworld.tamu.edu/index.html";
		}
		prtln("url: " + url);
		RemoteResultDoc[] results = rs.searchDocs(url);
		prtln(results.length + " items found");
		for (int i = 0; i < results.length; i++) {
			prtln(results[i].toString());
		}

	}
}

