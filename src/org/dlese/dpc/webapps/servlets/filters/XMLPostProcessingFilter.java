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
import org.json.XML;
import org.json.JSONStringer;

import org.dlese.dpc.xml.*;

/**
 *  Performs post-processing of an XML stream and gzips the response if supported by the browser. Supported
 *  post-processing actions include XML validation, XML transform to localized XML, and XML to JSON
 *  serialization.
 *  <ul>
 *    <li> To invoke XML validation, pass HTTP request parameter rt=validate.</li>
 *    <li> To invoke XML transform, pass HTTP request parameter transform=localize (localize only value
 *    currently supported).</li>
 *    <li> To invoke XML to JSON serialization, pass HTTP request parameter output=json and optionally
 *    callback=myCallbackFn to wrap the response in a callback function named by the paramater value.</li>
 *
 *  </ul>
 *  <p>
 *
 *  The XML validation is implemented using a servlet response Filter that checks the content for validity
 *  against an XML schema or DTD, which must be referenced in the content itself. <p>
 *
 *  The JSON serialization is implemented using the json.org XML serializer and appropriate classes must be
 *  included in your classpath. See <a href="http://www.json.org/javadoc/"> http://www.json.org/javadoc/</a> .
 *
 * @author    John Weatherley
 */
public class XMLPostProcessingFilter extends FilterCore {
	private static boolean debug = false;


	/**
	 *  Performs XML post-processing and gzipping of the response.
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
		// prtln("XMLPostProcessingFilter");

		HttpServletResponse res = (HttpServletResponse) response;
		HttpServletRequest req = (HttpServletRequest) request;

		boolean isGzipSupported = isGzipSupported(req);

		String rt = req.getParameter("rt");
		String outputParm = req.getParameter("output");
		String transform = req.getParameter("transform");

		// If asked to do validation, do it...
		if (rt != null && rt.equals("validate")) {
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
			String message = getValidationMessage(responseContent, req);
			
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
			
			// output formatted XML
			ouput.append(xmlToHtml(responseContent));
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

		// Handle JSON serialization, do it...
		else if (outputParm != null && outputParm.equals("json")) {
			res.setHeader("Connection", "close");

			if (isGzipSupported)
				res.setHeader("Content-Encoding", "gzip");

			res.getOutputStream();
			// For some reason, this needs to be called or response will hang
			CharArrayWrapper respWrapper = new CharArrayWrapper(res);

			// Invoke the response, storing output into the wrapper
			chain.doFilter(req, respWrapper);
			if (handleErrorCodes(res, respWrapper))
				return;
			res.setContentType("text/javascript");

			String responseContent = respWrapper.toString();

			// Convert XML to JSON, pretty print:
			try {
				if (transform != null && transform.equals("localize"))
					responseContent = XSLTransformer.localizeXml(responseContent);
				responseContent = XML.toJSONObject(responseContent).toString(3);
			} catch (Throwable t) {
				try {
					responseContent = new JSONStringer()
							.object()
							.key("JSON-ERROR")
							.value(t.getMessage())
							.endObject().toString();
				} catch (Throwable t2) {
					responseContent = "{\"JSON-ERROR\":\"Error converting XML to JSON\"}";
				}
			}

			String callback = req.getParameter("callback");
			if (callback != null && callback.trim().length() > 0)
				responseContent = callback.trim() + "(" + responseContent + ");";

			if (isGzipSupported) {
				//prtln("1 Outputting Gzip format...");
				writeGzipResponse(responseContent.toCharArray(), res);
			}
			else {
				//prtln("1 Outputting regular format...");
				writeRegularResponse(new StringBuffer(responseContent), res);
			}
		}

		// Handle XML Transform, do it...
		else if (transform != null && transform.equals("localize")) {

			if (isGzipSupported)
				res.setHeader("Content-Encoding", "gzip");

			res.getOutputStream();
			// For some reason, this needs to be called or response will hang
			CharArrayWrapper respWrapper = new CharArrayWrapper(res);

			// Invoke the response, storing output into the wrapper
			chain.doFilter(req, respWrapper);
			if (handleErrorCodes(res, respWrapper))
				return;

			String responseContent = respWrapper.toString();

			// Transform the XML:
			try {
				responseContent = XSLTransformer.localizeXml(responseContent);
			} catch (Throwable t) {
				try {
					responseContent = "<DDSWebService><error>Error transforming XML service response: " + t.getMessage() + "</error></DDSWebService>";
				} catch (Throwable t2) {}
			}

			if (isGzipSupported) {
				//prtln("1 Outputting Gzip format...");
				writeGzipResponse(responseContent.toCharArray(), res);
			}
			else {
				//prtln("1 Outputting regular format...");
				writeRegularResponse(new StringBuffer(responseContent), res);
			}
		}

		// If not converting anything...
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
	 *  Validate the provided xml and return a validation message.
	 *
	 * @param  xml  XML string to be validated
	 * @return      The validationMessage value or null if xml is valid
	 */
	protected String getValidationMessage(String xml, ServletRequest request) {
		prtln ("getValidationMessage(): posty");
		return XMLValidator.validateString(xml, true);
	}


	/**
	 *  Get html-displayable version of provided xml
	 *
	 * @param  xml  xml to be displayed as html
	 * @return      html as string
	 */
	protected String xmlToHtml(String xml) {
		return OutputTools.xmlToHtml(xml);
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
	protected void prtln(String s) {
		if (debug)
			System.out.println(getDateStamp() + " XMLPostProcessingFilter: " + s);
	}


	/**
	 *  Sets the debug attribute of the XMLPostProcessingFilter object
	 *
	 * @param  db  The new debug value
	 */
	protected final void setDebugz(boolean db) {
		debug = db;
	}

}

