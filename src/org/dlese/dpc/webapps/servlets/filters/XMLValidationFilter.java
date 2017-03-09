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
import java.text.SimpleDateFormat;

import org.dlese.dpc.xml.*;

/**
 *  Performs XML validation and gzipping of the pre-compiled HTTP response content. A caller wishing to invoke
 *  this filter should use an HTTP request parameter 'rt' set to 'validate.' <p>
 *
 *  This is implemented using a servlet response Filter that checks the content for validity against an XML
 *  schema or DTD, which must be referenced in the content itself. This filter also perform gzip compression
 *  of all output, if supported by the browser. See More Servlets and JavaServer Pages, chapter 9 for more
 *  information on Filters.
 *
 * @author        John Weatherley
 * @deprecated    Use XMLPostProcessingFilter instead
 * @see           XMLPostProcessingFilter
 */
public final class XMLValidationFilter extends FilterCore {
	private static boolean debug = true;


	/**
	 *  Performs XML validation and gzipping of the HTTP response content. A caller wishing to envoke this filter
	 *  should use an HTTP request parameter 'rt' set to 'validate.'
	 *
	 * @param  request               The request
	 * @param  response              The response
	 * @param  chain                 The chain of Filters
	 * @exception  ServletException  Iff error
	 * @exception  IOException       Iff IO error
	 */
	public final void doFilter(ServletRequest request,
	                           ServletResponse response,
	                           FilterChain chain)
		 throws ServletException, IOException {
		//prtln("XMLValidationFilter");

		HttpServletResponse res = (HttpServletResponse) response;
		HttpServletRequest req = (HttpServletRequest) request;

		boolean isGzipSupported = isGzipSupported(req);

		String rt = req.getParameter("rt");

		// If asked to do validation, do it...
		if (rt != null && rt.equals("validate")) {
			String verb = req.getParameter("verb");
			if (isGzipSupported)
				res.setHeader("Content-Encoding", "gzip");

			res.getOutputStream();
			// For some reason, this needs to be called or response will hang
			CharArrayWrapper respWrapper = new CharArrayWrapper(res);

			// Invoke the response, storing output into the wrapper
			chain.doFilter(req, respWrapper);
			if (handleErrorCodes(res, respWrapper))
				return;
			res.setContentType("text/html");

			String responseContent = respWrapper.toString();

			// Run XML validation over the content
			String message = XMLValidator.validateString(responseContent, true);

			StringBuffer ouput = new StringBuffer();
			ouput.append("<html><head><title>Validation report</title></head><body bgcolor='ffffff'>\n");
			if (message != null) {
				ouput.append("<font color='red'>");
				ouput.append("ERROR: This XML is NOT VALID:</font><br><br>\n" +
					OutputTools.htmlEncode(message) +
					"<br>");
			}
			else {
				ouput.append("<font color='green'>");
				ouput.append("This XML is VALID.</font><br>\n");
			}

			ouput.append("<br><hr noshade><br>\n");
			ouput.append(OutputTools.xmlToHtml(responseContent));
			ouput.append("</body></html>\n");
			responseContent = null;
			// gc

			if (isGzipSupported) {
				//prtln("1 Outputting Gzip format...");
				writeGzipResponse(ouput.toString().toCharArray(), res);
			}
			else {
				//prtln("1 Outputting regular format...");
				writeRegularResponse(ouput, res);
			}
		}

		// If not validating...
		else {
			if (!isGzipSupported) {
				//prtln("2 Outputting regular format...");
				chain.doFilter(req, res);
			}
			else {
				//prtln("2 Outputting Gzip format...");
				res.getOutputStream();
				CharArrayWrapper respWrapper = new CharArrayWrapper(res);
				res.setHeader("Content-Encoding", "gzip");

				// Invoke the response, storing output into the wrapper
				chain.doFilter(req, respWrapper);
				if (handleErrorCodes(res, respWrapper))
					return;

				writeGzipResponse(respWrapper.toCharArray(), res);
			}
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
				if (((String) context.getInitParameter("debug")).toLowerCase().equals("true")) {
					debug = true;
					//prtln("Outputing debug info");
				}
			} catch (Throwable e) {}
		} */
	}


	/**  Destroy is called at application shut-down time. */
	public void destroy() { }



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
	protected final void prtln(String s) {
		if (debug)
			System.out.println(getDateStamp() + " XMLValidationFilter: " + s);
	}


	/**
	 *  Sets the debug attribute of the XMLValidationFilter object
	 *
	 * @param  db  The new debug value
	 */
	protected final void setDebugzz(boolean db) {
		debug = db;
	}

}

