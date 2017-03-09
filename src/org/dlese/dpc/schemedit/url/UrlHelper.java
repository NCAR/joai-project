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
package org.dlese.dpc.schemedit.url;

import org.dlese.dpc.schemedit.*;
import org.dlese.dpc.xml.XPathUtils;
import org.dlese.dpc.index.*;
import org.dlese.dpc.serviceclients.remotesearch.*;

import java.util.*;
import java.util.regex.*;
import java.net.URI;
import java.net.URL;
import java.net.URISyntaxException;
import java.net.MalformedURLException;

/**
 *  Utilities for manipulating and comparing URLs
 *
 * @author     ostwald
 * @version    $Id: UrlHelper.java,v 1.3 2009/03/20 23:33:58 jweather Exp $
 */
public class UrlHelper {

	private static boolean debug = true;


	/**  Constructor for the UrlHelper object */
	public UrlHelper() { }


	/**
	 *  Removes trailing slash from a url string
	 *
	 * @param  s  Description of the Parameter
	 * @return    Description of the Return Value
	 */
/* 	static String normalize(String s) {
		if (s.endsWith("/")) {
			// prtln("\nchopping off a slash from " + s);
			s = s.substring(0, s.length() - 1);
		}
		return s;
	} */


	/**
	 *  To be called on form input from metadata editor
	 *
	 * @param  s                       NOT YET DOCUMENTED
	 * @return                         NOT YET DOCUMENTED
	 * @exception  URISyntaxException  NOT YET DOCUMENTED
	 */
	public static String normalize (String s) {
		if (s != null)
			try {
				NormalizedURL toNormalize = new NormalizedURL(s.trim());
				toNormalize.normalize();
				return toNormalize.toString();
			} catch (Exception e) {}
		return s;
	}


	/**
	 *  make sure the given urlStr represents a valid URL, and that the protocol,
	 *  host, and port match those of the baseURL. <p>
	 *
	 *  NOTE: the urls we want to check in DCS will already pass this test because
	 *  they are retrieved by a query string that is built from a legal baseURL and
	 *  include equal protocol, host and ports.
	 *
	 * @param  baseUrl        Description of the Parameter
	 * @param  urlStr         Description of the Parameter
	 * @return                The similar1 value
	 * @exception  Exception  Description of the Exception
	 */
	private static boolean isSimilarSyntactically(URL baseUrl, String urlStr)
		 throws Exception {
		URL url = UrlHelper.getUrl(urlStr);
		if (url == null) {
			throw new Exception("invalid url string: " + urlStr);
		}

		if (!UrlHelper.isValid(url)) {
			throw new Exception("url is not valid (" + urlStr + ")");
		}

		if (!baseUrl.getProtocol().equals(url.getProtocol())) {
			throw new Exception("protocols do not match");
		}

		if (!baseUrl.getHost().equals(url.getHost())) {
			throw new Exception("hosts do not match (" + baseUrl.getHost() + " vs. " + url.getHost());
		}

		if (baseUrl.getPort() != url.getPort()) {
			throw new Exception("ports do not match");
		}

		return true;
	}


	/**
	 *  Checks whether two urls are similar within a variable "distance" that is a
	 *  function of the respective lengths of the two urls.<p>
	 *
	 *  assumes urlStr represents a valid URL, and that the protocol, host, and
	 *  port match those of the baseURL. NOTE: the urls we want to check in DCS
	 *  will already pass this test because they are retrieved by a query string
	 *  that is built from a legal baseURL and include equal protocol, host and
	 *  ports.
	 *
	 * @param  baseStr   A reference URL against which to check another url
	 * @param  urlStr    A url string to be checked for similarity against baseUrl
	 * @param  maxDelta  The maximum difference in url length (the length of the
	 *      value returned by URL.getPath() split by "/")
	 * @return           true if the urlStr is similar to baseUrl, false if the
	 *      urls are not similar, or if either cannot be converted into a URL
	 *      instance.
	 */
	public static boolean isSimilar(String baseStr, String urlStr, int maxDelta) {

		// prtln ("urlHelper.isSimilar() comparing:\n\tbaseStr:" + baseStr + "\n\turlStr:" + urlStr);

		try {
			// if baseUrl cannot be converted to a URL, then abort this check, returning false
			URL baseUrl = UrlHelper.getUrl(baseStr);
			if (baseUrl == null) {
				throw new Exception("baseStr could not be converted into a URL - aborting similar check");
			}

			// only test if the two urls have the same protocol, host and port (if present)
			if (isSimilarSyntactically(baseUrl, urlStr)) {

				URL testUrl = UrlHelper.getUrl(urlStr);
				if (testUrl == null) {
					throw new Exception("could not parse " + urlStr + " as URL");
				}

				// to be similar, the urls must have the same first path element
				// removed this check on 9/22/05 after deciding it is too restrictive
				/* 				if (!UrlHelper.getPathItem(baseUrl, 1).equals(UrlHelper.getPathItem(testUrl, 1))) {
					throw new Exception("first path elements are not equal");
				} */
				int delta = UrlHelper.deltaPath(testUrl, baseUrl);
				// prtln ("   ... delta: " + delta);

				return (delta <= maxDelta);
			}
		} catch (Throwable t) {
			// prtln ("isSimilar msg: " + t.getMessage());
			// prtln (" .. returning false");
		}
		return false;
	}


	/**
	 *  Find the difference between the path lengths of two urls.
	 *
	 * @param  url1  Description of the Parameter
	 * @param  url2  Description of the Parameter
	 * @return       Description of the Return Value
	 */
	public static int deltaPathLen(URL url1, URL url2) {
		return Math.abs(getPathLen(url1) - getPathLen(url2));
	}


	/**
	 *  Compute a "delta" between two URLs that serves as a measure of their
	 *  similarity.<p>
	 *
	 *  First find the portions of the two paths that are identical. Then sum the
	 *  items in each url path items that are not found in the other.
	 *
	 * @param  url1  Description of the Parameter
	 * @param  url2  Description of the Parameter
	 * @return       Description of the Return Value
	 */
	public static int deltaPath(URL url1, URL url2) {
		List items1 = getPathItems(url1);
		List items2 = getPathItems(url2);
		int smaller = (items1.size() < items2.size() ? items1.size() : items2.size());

		int i = 0;
		while (i < smaller) {
			if (items1.get(i).equals(items2.get(i))) {
				i++;
			}
			else {
				break;
			}
		}
		return (items1.size() - i) + (items2.size() - i);
	}


	/**
	 *  Returns the difference in path lengths of two urls represented as strings
	 *
	 * @param  urlStr1  Description of the Parameter
	 * @param  urlStr2  Description of the Parameter
	 * @return          Description of the Return Value
	 */
	public static int deltaPathLen(String urlStr1, String urlStr2) {
		return Math.abs(getPathLen(urlStr1) - getPathLen(urlStr2));
	}


	/**
	 *  return the nth path item, empty string if nth item does not exist
	 *
	 * @param  url    Description of the Parameter
	 * @param  index  Description of the Parameter
	 * @return        The pathItem value
	 */
	public static String getPathItem(URL url, int index) {
		String item = "";
		List pathItems = getPathItems(url);
		if (pathItems.size() > (index)) {
			item = (String) pathItems.get(index);
		}
		return item;
	}


	/**
	 *  Gets the pathItem attribute of the UrlHelper class
	 *
	 * @param  urlStr  Description of the Parameter
	 * @param  index   Description of the Parameter
	 * @return         The pathItem value
	 */
	public static String getPathItem(String urlStr, int index) {
		URL url = getUrl(urlStr);
		if (url != null) {
			return getPathItem(url, index);
		}
		else {
			return "";
		}
	}


	/**
	 *  Returns the segments of a url path split around the path separator ("/")
	 *
	 * @param  url  A URL instance
	 * @return      The pathItems as a List
	 */
	public static List getPathItems(URL url) {
		String path = url.getPath();
		List items = new ArrayList();
		if (path == null) {
			return items;
		}
		String[] splits = path.split("/");
		return Arrays.asList(splits);
	}


	/**
	 *  Returns the length of the path component (URL.getPath()) of a url.
	 *
	 * @param  url  Description of the Parameter
	 * @return      The pathLen value
	 */
	public static int getPathLen(URL url) {
		int pathLen = 0;
		if (isValid(url)) {
			pathLen = getPathItems(url).size() - 1;
		}
		// adjusted for leading slash;
		return pathLen;
	}


	/**
	 *  Returns the length of the path component of a url represented as a string
	 *
	 * @param  s  Description of the Parameter
	 * @return    The path length, or 0 if the provided string could not be
	 *      converted into a URL instance.
	 */
	public static int getPathLen(String s) {
		int pathLen = 0;
		try {
			URL url = new URL(s);
			pathLen = getPathLen(url);
		} catch (Throwable t) {
			prtln("getPathLen error: " + t.getMessage());
		}
		return pathLen;
	}


	/**
	 *  Converts a string into a URL instance, returning null in the case of a
	 *  malformedUrl
	 *
	 * @param  urlStr  Description of the Parameter
	 * @return         A URL instance or null if the urlStr is malformed.
	 */
	public static URL getUrl(String urlStr) {
		URL url = null;
		try {
			url = new URL(urlStr);
		} catch (MalformedURLException e) {
			prtln(e.getMessage());
		}
		return url;
	}


	/**
	 *  A valid URL must have values for protocol and host.
	 *
	 * @param  url  A URL instance
	 * @return      true if the URL has both a protocal and host
	 * @see         java.net.URL
	 */
	public static boolean isValid(URL url) {

		try {
			String protocol = url.getProtocol();
			String host = url.getHost();
			String path = url.getPath();
			boolean ret = ((protocol != null && protocol.trim().length() > 0) &&
				(host != null && host.trim().length() > 0));
			// prtln ("  isValid (" + url.toString() + ") : " + ret);
			return ret;
		} catch (Throwable t) {
			prtln("isValid error: " + t.getMessage());
			return false;
		}
	}


	/**
	 *  Ensure that a urlStr contains a valid protocol and a host. Similar to
	 *  isValid, but returns an exception containing an error msg that can be
	 *  passed back to user.
	 *
	 * @param  urlStr                     String representation of a URL
	 * @return                            true if valid
	 * @exception  MalformedURLException  if not valid
	 */
	public static boolean validateUrl(String urlStr) throws MalformedURLException {
		URL url = null;

		url = new URL(urlStr);
		String protocol = url.getProtocol();
		String host = url.getHost();

		/* 		prtln("protocol: " + protocol);
		prtln("host: " + host); */
		if (host == null || host.trim().length() == 0)
			throw new MalformedURLException("host not found");
		return true;
	}


	/**
	 *  Use wild cards to make a url that will retrieve all "ancestors" of the
	 *  given url up to the specified level.
	 *
	 * @param  urlStr  base url to be "generalized" using wildcard
	 * @param  levels  the number of levels up to generalize urlStr
	 * @return         The similarUrlPath value
	 */
	public static String getSimilarUrlPath(String urlStr, int levels) {
		urlStr = normalize(urlStr);
		String similarUrlPath;
		try {
			similarUrlPath = getAncestor(urlStr, levels).toString();
		} catch (Throwable t) {
			similarUrlPath = urlStr;
		}
		return similarUrlPath + "*";
	}


	/**
	 *  Gets the specified ancestor (using the levels param) of a given url
	 *  (represented as a string).
	 *
	 * @param  urlStr  url represented as a String
	 * @param  levels  specifies how many levels "up" to go.
	 * @return         The ancestor value
	 */
	public static URL getAncestor(String urlStr, int levels) {

		try {
			return getAncestor(new URL(urlStr), levels);
		} catch (Exception e) {
			prtln("getParent error: " + e.getMessage());
			return null;
		}
	}


	/**
	 *  Gets the ancestor of the given URL by calling getParent "level" times.
	 *
	 * @param  url     Description of the Parameter
	 * @param  levels  Description of the Parameter
	 * @return         The ancestor value
	 */
	public static URL getAncestor(URL url, int levels) {
		URL ancestor = url;
		for (int i = 0; i < levels; i++) {
			URL parent = getParent(ancestor);
			if (parent != null && isValid(parent)) {
				ancestor = parent;
			}
			else {
				break;
			}
		}
		return ancestor;
	}


	/**
	 *  Gets the parent attribute of the UrlHelper class
	 *
	 * @param  urlStr  Description of the Parameter
	 * @return         The parent value
	 */
	public static URL getParent(String urlStr) {
		try {
			return getParent(new URL(urlStr));
		} catch (Exception e) {
			prtln("getParent error: " + e.getMessage());
			return null;
		}
	}


	/**
	 *  Gets the parent attribute of the UrlHelper class
	 *
	 * @param  url  Description of the Parameter
	 * @return      The parent value
	 */
	public static URL getParent(URL url) {
		if (!isValid(url)) {
			return null;
		}
		String parentPath = XPathUtils.getParentXPath(url.toString());
		if (parentPath != null) {
			// prtln ("parentPath: " + parentPath);
			try {
				return new URL(parentPath);
			} catch (Throwable t) {
				prtln("getParent error: " + t.getMessage());
				return null;
			}
		}
		else {
			return null;
		}
	}


	/**
	 *  Gets the paramValue attribute of the UrlHelper class
	 *
	 * @param  paramName  Description of the Parameter
	 * @param  url        Description of the Parameter
	 * @return            The paramValue value
	 */
	public static String getParamValue(String paramName, String url) {
		Map queryArgs = getQueryArgs(url);
		return (String) queryArgs.get(paramName);
	}


	/**
	 *  Gets the queryArgs attribute of the UrlHelper class
	 *
	 * @param  url  Description of the Parameter
	 * @return      The queryArgs value
	 */
	public static Map getQueryArgs(String url) {

		String query = "";
		Map queryArgs = new HashMap();

		try {
			query = url.split("\\?")[1];
			// prtln ("query: " + query);
		} catch (ArrayIndexOutOfBoundsException e) {
			prtln("could not get query");
			return queryArgs;
		}

		String[] paramArray = query.split("\\&");
		for (int i = 0; i < paramArray.length; i++) {
			String[] nameValue = paramArray[i].split("\\=");
			;
			try {
				String name = nameValue[0];
				String value = nameValue[1];
				queryArgs.put(name, value);
			} catch (ArrayIndexOutOfBoundsException e) {
				prtln("could not parse \"" + nameValue + "\" as a parameter name and value");
			}
		}
		return queryArgs;
	}


	/**
	 *  The main program for the UrlHelper class
	 *
	 * @param  args  The command line arguments
	 */
	public static void main(String[] args) {
		/* 		String url1 = "http://www.dlese.org/foo/flop/index.html";
		String url2 = "http://www.dlese.org/foo/faa/index.html";
		prtln("checking similarity between\n\t" + url1 + "\n\t" + url2);
		if (isSimilar(url1, url2, 3)) {
			prtln("SIMILAR");
		}
		else {
			prtln("Different");
		} */
		String url;
		if (args.length > 0)
			url = args[0];
		else {
			prtln("no url supplied");
			return;
		}
		try {
			UrlHelper.validateUrl(url);
		} catch (MalformedURLException e) {
			prtln("getMessage(): " + e.getMessage());
			prtln(e.toString());
			return;
		}
		prtln(url + " is VALID");
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

