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
package org.dlese.dpc.webapps.tools;

import java.util.*;
import java.io.*;

import javax.servlet.*;
import javax.servlet.http.*;

/**
 *  This class contains utility methods useful in servlet-based applications.
 *
 * @author     John Weatherley
 * @version    $Id: GeneralServletTools.java,v 1.9 2010/05/03 05:24:03 jweather Exp $
 */
public final class GeneralServletTools {

	/**
	 *  Returns the context path for the webapp, for example '/dds' or '/' for the root context.
	 *
	 * @param  servletContext  The ServletContext object for the webapp
	 * @return                 The context path for the webapp, for example '/dds' or '/', or null if not able to
	 *      determine
	 */
	public static String getContextPath(ServletContext servletContext) {
		try {
			String path = "/";
			String[] pathSegments = servletContext.getResource("/").getPath().split("/");

			if (pathSegments.length > 2)
				path += pathSegments[pathSegments.length - 1];
			return path;
		} catch (Throwable t) {
			return null;
		}
	}


	/**
	 *  Gets the URL that refers to the current server (scheme, hostname and port), for example
	 *  "http://www.dlese.org" or "http://host:8080" or "http://localhost:8080".
	 *
	 * @param  req  The request.
	 * @return      The URL to the server.
	 */
	public static String getServerUrl(HttpServletRequest req) {
		if (req == null)
			return null;

		String port = "";
		if (req.getServerPort() != 80)
			port = ":" + req.getServerPort();

		String contextURL = (req.getScheme()
				 + "://"
				 + req.getServerName()
				 + port).trim();
		return contextURL;
	}


	/**
	 *  Gets the URL that refers to the current server and servlet context, for example
	 *  "http://www.dlese.org/dds" or "http://domain.org:8080/context" or or "http://localhost:8080/context".
	 *
	 * @param  req  The request.
	 * @return      The URL to the context.
	 */
	public static String getContextUrl(HttpServletRequest req) {
		if (req == null)
			return null;

		String port = "";
		if (req.getServerPort() != 80)
			port = ":" + req.getServerPort();

		String contextURL = (req.getScheme()
				 + "://"
				 + req.getServerName()
				 + port
				 + req.getContextPath()).trim();
		return contextURL;
	}


	/**
	 *  Gets the query string supplied in the request, for example "q=ocean&s=0&n=10". This is the same as {@link
	 *  javax.servlet.http.HttpServletRequest#getQueryString()}, but works even if the request has been
	 *  forwarded.
	 *
	 * @param  request  The request
	 * @return          The query string, for example q=ocean&s=0&n=10
	 */
	public static String getQueryString(HttpServletRequest request) {
		if (request == null)
			return null;

		String reqStr = "";
		try {
			boolean isFirst = true;
			Enumeration paramNames = request.getParameterNames();
			if (paramNames.hasMoreElements()) {
				while (paramNames.hasMoreElements()) {
					if (isFirst)
						isFirst = false;
					else
						reqStr += "&";
					String paramName = (String) paramNames.nextElement();
					String[] paramValues = request.getParameterValues(paramName);
					for (int i = 0; i < paramValues.length; i++)
						reqStr += paramName + "=" + paramValues[0];
				}
			}
		} catch (Throwable t) {
			return null;
		}
		return reqStr;
	}



	/**
	 *  Reconstructs the URL the client used to make the request, even if the page has been forwarded for example
	 *  via struts (action.do). The returned URL contains a protocol, server name, port number, and server path,
	 *  but it does not include query string parameters.
	 *
	 * @param  req  The request.
	 * @return      The URL the client used to make the request.
	 */
	public static StringBuffer getRequestURL(HttpServletRequest req) {
		if (req == null)
			return null;

		// Make sure we have the original request Object, not the wrapper, so we get the original requested URI, not the URI it was forwarded to!
		// E.g. return "/dds/admin/admin.do" not "/dds/admin/collections.jsp"
		HttpServletRequest tmpRequest = req;
		while (tmpRequest instanceof HttpServletRequestWrapper)
			tmpRequest = (HttpServletRequest) ((HttpServletRequestWrapper) tmpRequest).getRequest();
		return tmpRequest.getRequestURL();
	}


	/**
	 *  Returns the part of this request's URL from the protocol name up to the query string in the first line of
	 *  the HTTP request. Works properly, even if the page has been forwarded for example via struts (action.do).
	 *
	 * @param  req  The request.
	 * @return      The URI the client used to make the request.
	 */
	public static String getRequestURI(HttpServletRequest req) {
		if (req == null)
			return null;

		// Make sure we have the original request Object, not the wrapper, so we get the original requested URI, not the URI it was forwarded to!
		// E.g. return "/dds/admin/admin.do" not "/dds/admin/collections.jsp"
		HttpServletRequest tmpRequest = req;
		while (tmpRequest instanceof HttpServletRequestWrapper)
			tmpRequest = (HttpServletRequest) ((HttpServletRequestWrapper) tmpRequest).getRequest();
		return tmpRequest.getRequestURI();
	}


	/**
	 *  Gets the absolute path to a given file or directory. Assumes the path passed in is eithr already absolute
	 *  (has leading slash) or is relative to the context root (no leading slash). If the string passed in does
	 *  not begin with a slash ("/"), then the string is converted. For example, an init parameter to a config
	 *  file might be passed in as "WEB-INF/conf/serverParms.conf" and this method will return the corresponding
	 *  absolute path "/export/devel/tomcat/webapps/myApp/WEB-INF/conf/serverParms.conf." <p>
	 *
	 *  If the string that is passed in already begings with "/", nothing is done. <p>
	 *
	 *  Note: the HttpServlet init() (super.init()) method must be called prior to using this method, else a
	 *  ServletException is thrown.
	 *
	 * @param  fname                 An absolute or relative file name or path (relative the the context root).
	 * @param  servletContext        The HttpServletContext of the appliection.
	 * @return                       The absolute path to the given file or path.
	 * @exception  ServletException  An exception related to this servlet
	 */
	public final static String getAbsolutePath(String fname, ServletContext servletContext)
			 throws ServletException {
		String fullname;

		// Note: super.init must be called before getServletContext().getRealPath("/")
		String docRoot = servletContext.getRealPath("/");

		return getAbsolutePath(fname, docRoot);
	}


	/**
	 *  Gets the absolute path to a given file or directory. Assumes the path passed in is eithr already absolute
	 *  (has leading slash) or is relative to the context root (no leading slash). If the string passed in does
	 *  not begin with a slash ("/"), then the string is converted. For example, an init parameter to a config
	 *  file might be passed in as "WEB-INF/conf/serverParms.conf" and this method will return the corresponding
	 *  absolute path "/export/devel/tomcat/webapps/myApp/WEB-INF/conf/serverParms.conf." <p>
	 *
	 *  If the string that is passed in already begings with "/", nothing is done. <p>
	 *
	 *  Note: the super.init() method must be called prior to using this method, else a ServletException is
	 *  thrown.
	 *
	 * @param  fname    An absolute or relative file name or path (relative the the context root).
	 * @param  docRoot  The context document root as obtained by calling getServletContext().getRealPath("/");
	 * @return          The absolute path to the given file or path.
	 */
	public final static String getAbsolutePath(String fname, String docRoot) {
		String fullname;

		if (fname.startsWith("/") ||
				fname.matches("[A-Z]:.+") ||
				fname.startsWith(docRoot) ||
				fname.startsWith("../") ||
				fname.substring(1, 3).equals(":\\")) {
			// Check for Windows paths
			fullname = fname;
		}
		else {
			if (docRoot.endsWith("/"))
				fullname = docRoot + fname;
			else
				fullname = docRoot + "/" + fname;
		}

		return fullname;
	}
}


