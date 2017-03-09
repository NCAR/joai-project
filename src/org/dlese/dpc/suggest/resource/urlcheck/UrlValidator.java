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
package org.dlese.dpc.suggest.resource.urlcheck;

import org.dlese.dpc.serviceclients.remotesearch.SearchServiceClient;
import org.dlese.dpc.serviceclients.remotesearch.reader.ADNItemDocReader;
import org.dlese.dpc.schemedit.url.UrlHelper;
import org.dlese.dpc.schemedit.url.DupSim;
import org.dlese.dpc.index.SimpleLuceneIndex;
import java.util.*;

/**
 *  Uses the Search Web Service (DDSWS v1.0) to search a repository (or a
 *  collection within a repository) for ADN records having duplicate and/or similar
 *  urls in it's primary or mirrorUrl fields.<p>
 *
 *  The primary method is validate, which takes a referenceUrl, and returns a
 *  ValidatorResults instance that stores information about records that contain
 *  duplicate and similar urls in the PrimaryURL and MirrorUrl fields.
 *
 *
 * @author     ostwald<p>
 *
 *      $Id $
 * @version    $Id: UrlValidator.java,v 1.3 2009/03/20 23:34:00 jweather Exp $
 */
public class UrlValidator {

	private static boolean debug = true;

	private String referenceCollection = null;
	private SearchServiceClient searchServiceClient = null;
	private DupSim duplicate = null;
	private List similarPrimaryUrls = null;
	static int SEARCH_LEVELS = 3;
	static int MAX_DELTA = 3;


	/**
	 *  Constructor for the UrlValidator object
	 *
	 * @param  searchServiceBaseUrl  base url of the Search Service used to
	 *      validate a url
	 * @param  referenceCollection   NOT YET DOCUMENTED
	 */
	public UrlValidator(String searchServiceBaseUrl, String referenceCollection) {
		this(new SearchServiceClient(searchServiceBaseUrl), referenceCollection);
	}


	/**
	 *  Constructor for the UrlValidator object
	 *
	 * @param  searchServiceClient  base url of the Search Service used to validate
	 *      a url
	 * @param  referenceCollection  NOT YET DOCUMENTED
	 */
	public UrlValidator(SearchServiceClient searchServiceClient, String referenceCollection) {
		this.referenceCollection = referenceCollection;
		SearchServiceClient.setDebug(false);
		if (searchServiceClient == null) {
			prtln("ERROR: failed to initialize searchServiceClient");
			return;
		}
		this.searchServiceClient = searchServiceClient;
	}


	/**
	 *  Gets the referenceCollection attribute of the UrlValidator object
	 *
	 * @return    The referenceCollection value
	 */
	public String getReferenceCollection() {
		return this.referenceCollection;
	}


	/**
	 *  Construct query string to be used by the Search Service to search for
	 *  records containing matches to the provided url.
	 *
	 * @param  url  Description of the Parameter
	 * @return      Description of the Return Value
	 */
	private static String makeUrlQueryStr(String url) {
		return makeUrlQueryStr(url, null);
	}


	/**
	 *  Construct query string to search for records (in the specified collection)
	 *  containing urls that match the provided url in either the PrimaryUrl or
	 *  MirrorUrls fields. If a collection is not specified, the entire repository
	 *  is searched.
	 *
	 * @param  url         referenceUrl
	 * @param  collection  collection to be searched.
	 * @return             Description of the Return Value
	 */
	private static String makeUrlQueryStr(String url, String collection) {
		String urlEnc = SimpleLuceneIndex.encodeToTerm(url, false);

		// construct query string
		String q = "urlenc:" + urlEnc;
		q += "+OR+urlMirrorsEncoded:" + urlEnc;

		// add collection (if present) to query
		if (collection != null) {
			q = "(" + q + ")+AND+collection:0" + collection;
		}

		return "q=" + q + "&s=0&n=500";
	}


	/**
	 *  Create the query string to search for records containing similar urls to
	 *  the provided url.
	 *
	 * @param  url  Description of the Parameter
	 * @return      Description of the Return Value
	 */
	private String makeSimilarUrlQueryStr(String url) {
		String similarUrlPath = UrlHelper.getSimilarUrlPath(url, SEARCH_LEVELS);
		prtln("similarUrlPath: " + similarUrlPath);
		return makeUrlQueryStr(similarUrlPath, this.referenceCollection);
	}


	/**
	 *  Return a ValidatorResults instance containing the results of a query to
	 *  find duplicate and similiar urls in the primaryUrl and mirrorUrl fields of
	 *  repository records.
	 *
	 * @param  referenceUrl  The url to be validated
	 * @return               Description of the Return Value
	 */
	public ValidatorResults validate(String referenceUrl) {
		ValidatorResults vr = new ValidatorResults();

		// return empty results if there is no referenceCollection Specified
		if (this.referenceCollection == null)
			return vr;

		String queryStr = makeSimilarUrlQueryStr(referenceUrl);
		// prtln ("query string: " + queryStr + "\n");
		prtln("referenceUrl: " + referenceUrl + "\n");

		List searchResults = searchServiceClient.searchDocs(queryStr);
		prtln(searchResults.size() + " search results found");

		for (Iterator i = searchResults.iterator(); i.hasNext(); ) {
			ADNItemDocReader reader = (ADNItemDocReader) i.next();
			String primaryUrl = reader.getUrl();

			// is it a duplicate?
			if (primaryUrl.equals(referenceUrl)) {
				vr.setDuplicate(new DupSim(reader, "primary"));
				return vr;
			}
			if (UrlHelper.isSimilar(referenceUrl, primaryUrl, MAX_DELTA)) {
				vr.addSimilarPrimaryUrl(new DupSim(reader, "primary"));
			}

			for (Iterator m = reader.getMirrorUrls().iterator(); m.hasNext(); ) {
				String mirrorUrl = (String) m.next();
				if (mirrorUrl.equals(referenceUrl)) {
					String id = reader.getId();
					DupSim dup = new DupSim(reader.getId(), mirrorUrl, "dup", "mirror", "adn");
					vr.setDuplicate(dup);
					return vr;
				}
				if (UrlHelper.isSimilar(referenceUrl, mirrorUrl, MAX_DELTA)) {
					DupSim dup = new DupSim(reader.getId(), mirrorUrl, "sim", "mirror", "adn");
					vr.addSimilarMirrorUrl(dup);
				}
			}
		}
		return vr;
	}


	/**
	 *  The main program for the UrlValidator class
	 *
	 * @param  args           The command line arguments
	 * @exception  Exception  Description of the Exception
	 */
	public static void main(String[] args)
		 throws Exception {
		String url = "http://www.fooberry.com";
		if (args.length > 0) {
			url = args[0];
		}

		String collection = null;
		if (args.length > 1) {
			collection = args[1];
		}

		prtln("reference Url: " + url);
		String serviceUrl = "http://128.117.126.8:8688/schemedit/services/ddsws1-0";

		UrlValidator v = new UrlValidator(serviceUrl, collection);
		ValidatorResults vr = v.validate(url);
		prtln(vr.toString());
		prtln("======================================");

	}


	/**
	 *  Sets the debug attribute of the UrlValidator class
	 *
	 * @param  bool  The new debug value
	 */
	public static void setDebug(boolean bool) {
		debug = bool;
	}


	/**
	 *  Description of the Method
	 *
	 * @param  s  Description of the Parameter
	 */
	private static void prtln(String s) {
		if (debug) {
			System.out.println("UrlValidator: " + s);
		}
	}
}

