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
package org.dlese.dpc.webapps.servlets.filters;

import org.dlese.dpc.webapps.tools.*;

import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.util.*;
import java.util.zip.*;
import java.text.SimpleDateFormat;

import org.dlese.dpc.xml.*;

/**
 *  Performs gzipping of the response content. This is implemented using servlet response Filter that takes
 *  the reqular ouput of a JSP or servlet and converts it into a GZIP output stream. By encoding the output as
 *  GZIP, the size of thresponse can be reduced by as much as 300%.
 *
 * @author     John Weatherley
 * @version    $Id: GzipFilter.java,v 1.11 2009/03/20 23:34:01 jweather Exp $
 */
public final class GzipFilter extends FilterCore {
	//private static ServletContext context = null;
	private static boolean debug = true;


	/**
	 *  Performs gzipping of the response content.
	 *
	 * @param  request               The request
	 * @param  response              The response
	 * @param  chain                 The chain of filters
	 * @exception  ServletException  If problem occurs
	 * @exception  IOException       If IO problem occurs
	 */
	public final void doFilter(ServletRequest request,
	                           ServletResponse response,
	                           FilterChain chain)
		 throws ServletException, IOException {

		HttpServletResponse res = (HttpServletResponse) response;
		HttpServletRequest req = (HttpServletRequest) request;

		//prtln("Processing request: " + GeneralServletTools.getContextUrl(req) + "?" + GeneralServletTools.getQueryString(req));

		if (!isGzipSupported(req)) {
			//prtln("Outputting regular format...");
			chain.doFilter(req, res);
		}
		else {
			//prtln("Outputting Gzip format...");
			res.getOutputStream();
			CharArrayWrapper respWrapper = new CharArrayWrapper(res);
			res.setHeader("Content-Encoding", "gzip");
			// Invoke the response, storing output into the wrapper
			chain.doFilter(req, respWrapper);
			writeGzipResponse(respWrapper.toCharArray(), res);
			res.getOutputStream().close();
		}
	}



	/**
	 *  Init is called once at application start-up.
	 *
	 * @param  config                The FilterConfig object that holds the ServletContext and init information.
	 * @exception  ServletException  If an error occurs
	 */
	public void init(FilterConfig config) throws ServletException {
		/* if (context == null) {
			try {
				context = config.getServletContext();
				if (((String)context.getInitParameter("debug")).toLowerCase().equals("true")) {
					debug = true;
					//prtln("Outputting debug info");
				}
			} catch (Throwable e) {}
		} */
	}


	/**  Destroy is called at application shut-down time. */
	public void destroy() {

	}



	//================================================================

	/**
	 *  Output a line of text to error out, with datestamp.
	 *
	 * @param  s  The text that will be output to error out.
	 */
	protected final void prtlnErr(String s) {
		System.err.println(getDateStamp() + " " + s);
	}



	/**
	 *  Output a line of text to standard out, with datestamp, if debug is set to true.
	 *
	 * @param  s  The String that will be output.
	 */
	protected final static void prtln(String s) {
		if (debug)
			System.out.println(getDateStamp() + " GzipFilter: " + s);
	}


	/**
	 *  Sets the debug attribute of the GzipFilter object
	 *
	 * @param  db  The new debug value
	 */
	protected final void setDebugzz(boolean db) {
		debug = db;
	}

}

