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

import org.dlese.dpc.util.*;

import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.util.*;
import java.nio.CharBuffer;
import java.text.SimpleDateFormat;

/**
 *  Converts JSP, servlet, HTML or other output into a JavaScript writeln statement or places it into a
 *  JavaScript variable. Activated by specifying a query parameter - if none is supplied, the output is left
 *  unchanged (except for Gziping). This Filter may be used to create a JavaScript proxy for conetnt. Gzip
 *  compresses the response if the browser supports it.<p>
 *
 *  To use, configure the filter to be activated for the pages you want in your web.xml configuration. Then
 *  send a request to the page with the one or more of the following query parameters:<p>
 *
 *  rt=jswl - Instructions the filter to output the content as a document.write( content ); statement<br>
 *  rt=jsvar - Instructs the filter to place the content into a JavaScript variable named 'jsvar'.<br>
 *  jsvarname=myvarname - (optional) When jsvar is used, this instructs the filter to place the ouput into the
 *  a variable by the name provided. If not supplied, defaults to 'jsvar'.<p>
 *
 *  Examples:<p>
 *
 *  <code>
 *&lt;script type='text/javascript' src='http://example.org/myPage.jsp?rt=jswl'&gt;&lt;/script&gt; <br>
 *  &lt;script type='text/javascript' src='http://example.org/myPage.jsp?rt=jsvar&jsvarname=myXml'>
 *  &lt;/script&gt; </code>
 *
 * @author     John Weatherley
 * @version    $Id: JavaScriptWritelnFilter.java,v 1.9 2009/03/20 23:34:01 jweather Exp $
 */
public final class JavaScriptWritelnFilter extends FilterCore {
	//private static ServletContext context = null;
	private static boolean debug = true;


	/**
	 *  Converts the http response into a JavaScript output if parameter rt=jsvar or rt=jswl. Gzip compresses the
	 *  response regardless.
	 *
	 * @param  request               The request
	 * @param  response              The response
	 * @param  chain                 The chain of Filters
	 * @exception  ServletException  If error
	 * @exception  IOException       If IO error
	 */
	public final void doFilter(ServletRequest request,
	                           ServletResponse response,
	                           FilterChain chain)
		 throws ServletException, IOException {

		HttpServletResponse res = (HttpServletResponse) response;
		HttpServletRequest req = (HttpServletRequest) request;

		//prtln("Filtering " + req.getRequestURL());

		boolean isGzipSupported = isGzipSupported(req);

		String rt = req.getParameter("rt");

		// If asked to output as JavaScript, do it...
		if (rt != null && (rt.equals("jswl") || rt.equals("jsvar"))) {

			if (isGzipSupported)
				res.setHeader("Content-Encoding", "gzip");

			// For some reason, this needs to be called or response will hang
			res.getOutputStream();

			CharArrayWrapper respWrapper = new CharArrayWrapper(res);

			// Invoke the response, storing output into the wrapper
			chain.doFilter(req, respWrapper);

			// Set the content type to javascript
			res.setContentType("text/javascript");

			//prtln("JavaScriptWritelnFilter 2 - " + res.isCommitted());

			StringBuffer ouput = new StringBuffer();

			// Output as a JavaScript writln statement
			if (rt.equals("jswl")) {
				ouput.append("document.write(\"");
				ouput.append(HTMLTools.javaScriptEncode(respWrapper.toCharArray()));
				ouput.append("\");");

				//prtln("Outputting as JS write statement");
			}

			// Output as a JavaScript variable
			else if (rt.equals("jsvar")) {

				String varName = req.getParameter("jsvarname");
				if (varName == null || varName.length() == 0)
					varName = "jsvar";

				ouput.append("\n var " + varName + " = '");
				ouput.append(HTMLTools.javaScriptEncode(respWrapper.toCharArray()));
				ouput.append("'; \n");

				//prtln("Outputting as JS variable '" + varName + "'");
			}

			// Gzip if possible...
			if (isGzipSupported) {
				//prtln("1 Outputting Gzip format...");
				writeGzipResponse(ouput.toString().toCharArray(), res);
			}
			else {
				//prtln("1 Outputting regular format...");
				writeRegularResponse(ouput, res);
			}
		}

		// If NOT asked to output in JavaScript, just GZIP...
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
		System.err.println(getDateStamp() + " JavaScriptWritelnFilter ERROR: " + s);
	}



	/**
	 *  Output a line of text to standard out, with datestamp, if debug is set to true.
	 *
	 * @param  s  The String that will be output.
	 */
	protected final void prtln(String s) {
		if (debug)
			System.out.println(getDateStamp() + " JavaScriptWritelnFilter: " + s);
	}


	/**
	 *  Sets the debug attribute of the JavaScriptWritelnFilter object
	 *
	 * @param  db  The new debug value
	 */
	protected final void setDebugzz(boolean db) {
		debug = db;
	}

}

