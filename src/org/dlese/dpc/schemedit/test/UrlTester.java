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

import org.dlese.dpc.xml.XPathUtils;
import org.dlese.dpc.schemedit.url.UrlHelper;
import java.util.*;
import java.util.regex.*;
import java.net.*;

/**
 *  Utilities for manipulating XPaths, represented as String
 *
 * @author     ostwald
 * @version    $Id: UrlTester.java,v 1.5 2009/03/20 23:33:58 jweather Exp $
 */
public class UrlTester {

	private static boolean debug = true;


	/**
	 *  Gets the validUrl attribute of the UrlTester class
	 *
	 * @param  s  Description of the Parameter
	 * @return    The validUrl value
	 */
	public static boolean isValidUrl(String s) {
		prtln("isValidUrl with " + s);
		URI uri = null;
		try {
			uri = new URI(s);
		} catch (URISyntaxException se) {
			prtln(se.getMessage());
			return false;
		}
		String scheme = uri.getScheme();
		if (scheme == null) {
			return false;
		}
		else {
			prtln("scheme: " + scheme);
			return true;
		}
	}


	/**
	 *  Split given URL into pieces and print them out
	 *
	 * @param  url  Description of the Parameter
	 */
	public static void chopUrl(URL url) {
		prtln("\nchopUrl with " + url.toString());

		String protocol = url.getProtocol();
		String host = url.getHost();
		String path = url.getPath();
		int pathLen = path.split("/").length;
		String file = url.getFile();
		String query = url.getQuery();
		prtln("host: " + host);
		prtln("protocal: " + protocol);

		String hostPrefix = "notfound";
		int firstDot = host.indexOf(".");
		if (firstDot != -1) {
			hostPrefix = host.substring(0, firstDot);
		}
		prtln("hostPrefix: " + hostPrefix);

		String parent = "not found";
		if (file != null && file.length() > 0) {
			parent = XPathUtils.getParentXPath(url.toString());
		}
		prtln("path: " + path + " (" + pathLen + ")");
		prtln("file: " + file);
		prtln("query: " + query);
		prtln("parent: " + parent);
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  name   NOT YET DOCUMENTED
	 * @param  array  NOT YET DOCUMENTED
	 */
	public static void prtArray(String name, String[] array) {
		prtln("array: " + name + " (" + array.length + ")");
		for (int i = 0; i < array.length; i++)
			prtln("\t" + array[i]);
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  s  NOT YET DOCUMENTED
	 */
	public static void validateUrl(String s) {
		String urlStr = s.trim();
		prtln("valaidateUrl(): with \"" + urlStr + "\"");
		// QUESTION: do we WANT to chop off the trailing slash? maybe we want to add a trailing slash
		// to urls that should have one but don't ..
		// the issue is whether removing the slash messes up the "parent" relationship ..

		if (s.endsWith("/")) {
			prtln("\nchopping off a slash from " + s);
			s = s.substring(0, s.length() - 1);
		}

		URL url = null;
		try {
			url = new URL(s);

			String protocol = url.getProtocol();
			if (protocol == null || protocol.length() == 0)
				throw new Exception("missing protocol");
			else
				prtln("protocol: " + protocol);

			String host = url.getHost();
			if (host == null || host.length() == 0)
				throw new Exception("missing host");
			else
				prtln("host: " + host);

			String path = url.getPath();
			int pathLen = 0;
			if (path == null || path.length() == 0) {
				// throw new Exception ("missing path");
				prtln("missing path");
			}
			else {
				prtln("path: " + path);
				pathLen = path.split("/").length;
				prtArray("path splits", path.split("/"));
			}

			String file = url.getFile();
			if (file == null || file.length() == 0) {
				// throw new Exception ("missing file");
				prtln("missing file");
			}
			else
				prtln("file: " + file);

			String similarQueryPath = null;
			if (pathLen > 1) {
				String parentPath = XPathUtils.getParentXPath(url.toString());
				if (parentPath != null) {
					similarQueryPath = parentPath + "/*";
					prtln("similarQueryPath: " + similarQueryPath);
				}
			}
			if (similarQueryPath == null)
				prtln("no simiilarQueryPath path computed");

		} catch (MalformedURLException urlex) {
			prtln("URL parse error: " + urlex.getMessage());
		} catch (Exception e) {
			prtln("Error: " + e.getMessage());
		}
	}


	static String[] poolUrls = {
		"http:////www.google.com/",
		"http://www.google.com/foo/index.html",
		"http://www.google.com/foo/index.htm",
		"http://www.google.com/index.htm",
		"ftp://ftp.google.com",
		"ftp://ftp.google.com/files/",
		"ftp:/ftp.google.com/files/index.html",
		"ftp:/ftp.google.com/files/header.html",
		};


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  urls  NOT YET DOCUMENTED
	 * @return       NOT YET DOCUMENTED
	 */
	static ArrayList makePool(String[] urls) {
		ArrayList pool = new ArrayList();
		for (int i = 0; i < urls.length; i++) {
			pool.add(urls[i]);
		}
		return pool;
	}


	/**
	 *  A unit test for JUnit
	 *
	 * @param  args  NOT YET DOCUMENTED
	 */
	static void testPool(String[] args) {
		prtln("--------------------------------------------------");
		List pool = new ArrayList();
		// prtln ("args length: " + args.length);
		if (args.length > 0)
			pool.add(args[0]);
		else
			pool = makePool(poolUrls);

		for (Iterator i = pool.iterator(); i.hasNext(); ) {
			String u = (String) i.next();
			/* 			u = u.trim();

			// QUESTION: do we WANT to chop off the trailing slash? maybe we want to add a trailing slash
			// to urls that should have one but don't ..
			// the issue is whether removing the slash messes up the "parent" relationship ..

			if (u.endsWith("/")) {
				prtln ("\nchopping off a slash from " + u);
				u = u.substring(0, u.length()-1);
			}
			chopUrl(new URL (u));

			*/
			validateUrl(u);

		}
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  urlStr                     NOT YET DOCUMENTED
	 * @return                            NOT YET DOCUMENTED
	 * @exception  MalformedURLException  NOT YET DOCUMENTED
	 */
	static boolean validate(String urlStr) throws MalformedURLException {
		URL url = null;

		url = new URL(urlStr);
		String protocol = url.getProtocol();
		String host = url.getHost();
		String path = url.getPath();
		String file = url.getFile();
		String query = url.getQuery();
		
		prtln("protocol: " + protocol);
		prtln("host: " + host);
/* 		prtln("path: " + path);
		prtln("file: " + file);		
		prtln("query: " + query); */

		if (host == null || host.trim().length() == 0)
			throw new MalformedURLException ("malformed URL");
		return true;
	}


	/**
	 *  The main program for the UrlTester class
	 *
	 * @param  args           The command line arguments
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public static void main(String[] args) throws Exception {
		// testPool (args);
		String urlStr = "http://foo.com/ncs-test/index.html?foo=farb";
		if (args.length > 0)
			urlStr = args[0];

		
		prtln("normalizing: " + urlStr);
		String normalized = UrlHelper.normalize(urlStr);
		prtln ("normalized: " + normalized);

		
/* 		try {
			chopUrl (new URL (urlStr));
			validate(urlStr);
			prtln("VALID");
		} catch (MalformedURLException e) {
			prtln("Invalid: " + e.getMessage());
		} */

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

