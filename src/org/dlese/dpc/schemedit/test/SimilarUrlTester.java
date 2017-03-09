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

import org.dlese.dpc.schemedit.url.UrlHelper;
import org.dlese.dpc.xml.XPathUtils;

import java.util.*;
import java.util.regex.*;
import java.net.*;

/**
 *  Utilities for manipulating XPaths, represented as String
 *
 *@author    ostwald
 */
public class SimilarUrlTester {

	private static boolean debug = true;
	ArrayList pool = null;

	String[] poolUrls = {
			"http://www.google.com/one",
			"http://www.google.com/one/index.html",
			"http://www.google.com/first/index.html",
			"http://www.google.com/index.htm",
			"http://www.google.com/one/two/index.htm",
			"http://www.google.com/one/three/index.htm",
			"http://www.google.com/one/three/two/index.htm",
			};


	/**
	 *  Description of the Method
	 *
	 *@return    Description of the Return Value
	 */
	ArrayList makePool() {
		ArrayList pool = new ArrayList();
		for (int i = 0; i < poolUrls.length; i++) {
			pool.add(poolUrls[i]);
		}
		return pool;
	}


	/**
	 *  Constructor for the SimilarUrlTester object
	 */
	SimilarUrlTester() {
		pool = makePool();
	}


	/**
	 *  make sure the given urlStr represents a valid URL, and that the protocol,
	 *  host, and port match those of the baseURL. NOTE: the urls we want to check in DCS
	 * will already pass this test because they are retrieved by a query string that is built
	 * from a legal baseURL and include equal protocol, host and ports.
	 *
	 *@param  baseUrl  Description of the Parameter
	 *@param  urlStr   Description of the Parameter
	 *@return          The similar1 value
	 */
	boolean isSimilar1 (URL baseUrl, String urlStr) throws Exception {
		URL url = UrlHelper.getUrl(urlStr);
		if (url == null) {
			throw new Exception ("invalid url string: " + urlStr);
		}

		if (!UrlHelper.isValid(url)) {
			throw new Exception ("url is not valid (" + urlStr + ")");
		}

		if (!baseUrl.getProtocol().equals(url.getProtocol())) {
			throw new Exception ("protocols do not match");
		}

		if (!baseUrl.getHost().equals(url.getHost())) {
			throw new Exception ("hosts do not match (" + baseUrl.getHost() + " vs. " + url.getHost());
		}

		if (baseUrl.getPort() != url.getPort()) {
			throw new Exception ("ports do not match");
		}

		return true;
	}


	/**
	 *  Gets the similar attribute of the SimilarUrlTester object
	 *
	 *@param  baseUrl  Description of the Parameter
	 *@param  urlStr   Description of the Parameter
	 *@return          The similar value
	 */
	boolean isSimilar(String baseStr, String urlStr) throws Exception {
		// if domains do not match, then return false

		prtln ("\n----------------------");
		prtln ("comparing:\n\t" + baseStr + "\n\t" + urlStr);
		
		URL baseUrl = UrlHelper.getUrl (baseStr);
		if (baseUrl == null) {
			throw new Exception ("baseStr could not be converted into a URL - aborting similar check");
		}
		
		if (isSimilar1(baseUrl, urlStr)) {

			URL testUrl = UrlHelper.getUrl (urlStr);
			if (testUrl == null) {
				throw new Exception ("could not parse " + urlStr + " as URL");
			}
			
			// to be similar, the urls must have the same first path element
			if (!UrlHelper.getPathItem(baseUrl, 1).equals(UrlHelper.getPathItem(testUrl, 1))) {
				throw new Exception ("first path elements are not equal");
			}
			
			int delta = UrlHelper.deltaPath(testUrl, baseUrl);
			prtln ("delta: " + delta);
			
			return true;
		}
		return false;
	}

	/**
	 *  Gets the similarUrls attribute of the SimilarUrlTester object
	 *
	 *@exception  Exception  Description of the Exception
	 */
	void poolCompare()
		throws Exception {
			
		prtln ("URL pool");
		for (Iterator i=pool.iterator();i.hasNext();) {
			prtln ("\t" + (String)i.next());
		}
			
		while (true) {
			System.out.print("\n=============================\nURL: ");
			byte[] buffer = new byte[1024];
			System.in.read(buffer);
			String url = new String(buffer).trim();
			if (url.length() == 0) {
				prtln("Bye");
				return;
			}
			else {
				prtln("similar urls");
				for (Iterator i = pool.iterator(); i.hasNext(); ) {
					try {
						if (isSimilar ( (String)i.next(), url))
							prtln ("SIMILAR");
						else
							prtln ("NOT similar");
					} catch (Exception e) {
						prtln ("NOT similar: " + e.getMessage());
					}
				}
			}
		}
	}

	void lenTester () throws Exception {
		while (true) {
			System.out.print("\nURL: ");
			byte[] buffer = new byte[1024];
			System.in.read(buffer);
			String urlStr = new String(buffer).trim();
			if (urlStr.length() == 0) {
				prtln("Bye");
				return;
			}
			else {
				prtln ("path length " + UrlHelper.getPathLen(urlStr));
				prtln ("items:");
				for (int i=1;i<UrlHelper.getPathLen(urlStr)+1;i++)
					prtln ("\t" + i + ": " + UrlHelper.getPathItem(urlStr, i));
				}
		}
	}
	/**
	 *  The main program for the SimilarUrlTester class
	 *
	 *@param  args           The command line arguments
	 *@exception  Exception  Description of the Exception
	 */
	public static void main(String[] args)
		throws Exception {
		SimilarUrlTester tester = new SimilarUrlTester();
		tester.poolCompare();
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

