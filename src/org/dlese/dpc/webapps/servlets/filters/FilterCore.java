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
 *  Abstract class that contains core methods common to Servlet Filters. Implementation note: Servlet Filters
 *  only get instatiated once and live for the lifetime of the application.
 *
 * @author     John Weatherley
 * @version    $Id: FilterCore.java,v 1.2 2009/03/20 23:34:01 jweather Exp $
 */
public abstract class FilterCore implements Filter {
	private static boolean debug = true;


	/**
	 *  Writes the response content to the client using gzip compression. Before using this method, make sure the
	 *  client can accespt gzip content via the {@link #isGzipSupported} method. <p>
	 *
	 *  Note: you must set the content encoding type to gzip using the method <br>
	 *  <code>response.setHeader("Content-Encoding", "gzip");</code> prior to calling <code>chain.doFilter()</code>
	 *  method.
	 *
	 * @param  content          The content to sent to the client in gzip format
	 * @param  res              The response object
	 * @exception  IOException  If IO Error occurs
	 */
	protected final void writeGzipResponse(char[] content, HttpServletResponse res)
		 throws IOException {

		ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
		GZIPOutputStream zipOut = new GZIPOutputStream(byteStream);
		OutputStreamWriter tempOut = new OutputStreamWriter(zipOut, res.getCharacterEncoding());

		// Compress content, store into byteStream array
		tempOut.write(content);
		tempOut.close();
		zipOut.close();

		int contentLength = byteStream.size();
		// Set the content length header - not sure this does anything, since the headers are already committed.
		res.setContentLength(contentLength);

		// Send compressed content over the wire...
		byteStream.writeTo(res.getOutputStream());
		byteStream.close();
		res.getOutputStream().close();
		//prtlnCore("Gziped response len: " + contentLength);
	}


	/**
	 *  Writes the response to the OutputStream without gzipping it, for use if the client can not accept a gzip
	 *  response.
	 *
	 * @param  content          The content to setd
	 * @param  res              The response
	 * @exception  IOException  If error
	 */
	protected final void writeRegularResponse(StringBuffer content, HttpServletResponse res)
		 throws IOException {
		// Set the content length header - not sure this does anything, since the headers are already committed.
		res.setContentLength(content.length());

		// Send uncompressed content over the wire...
		OutputStreamWriter tempOut = new OutputStreamWriter(res.getOutputStream(), res.getCharacterEncoding());
		tempOut.write(content.toString());
		tempOut.close();
		res.getOutputStream().close();
		//prtlnCore("Regular response len: " + content.length());
	}


	/**
	 *  Determines whether the client that made the request can accept a gzip response.
	 *
	 * @param  req  The request
	 * @return      True if the client can accept gzip content
	 */
	public final static boolean isGzipSupported(HttpServletRequest req) {
		String enc = req.getHeader("Accept-Encoding");
		return ((enc != null) && (enc.indexOf("gzip") != -1));
	}


	/**
	 *  Handles http errors (not yet implemented).
	 *
	 * @param  res              The response
	 * @param  wrapper          The wrapper
	 * @return                  True if an http error occured and is being handled here
	 * @exception  IOException  If error processing
	 */
	protected final boolean handleErrorCodes(HttpServletResponse res, CharArrayWrapper wrapper)
		 throws IOException {
		int sc = wrapper.getStatus();
		if (sc != 0 && sc != 200) {
			prtlnCore("Error code: " + sc + " error msg: " + wrapper.getErrorMsg());
			//res.reset();
			//res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,"john here");
			// It appears that if there is an error, the OutputStream gets closed by the caller,
			// and writing to it again throws an exception.


			/* StringBuffer out = new StringBuffer();
			out.append("<html><head><title>Error status " + sc + "</title></head><body bgcolor='ffffff'><br>\n\n");
			out.append("Error processing request. <p>\n\n");
			out.append("Status code " + sc);
			if (sc == 404)
				out.append(" - file not found");
			out.append("\n\n");
			out.append("</body></html>\n");
			writeRegularResponse(out, res); */
			return true;
		}
		return false;
	}



	//================================================================

	/**
	 *  Return a string for the current time and date, sutiable for display in log files and output to standout:
	 *
	 * @return    The dateStamp value
	 */
	protected final static String getDateStamp() {
		return
			new SimpleDateFormat("MMM d, yyyy h:mm:ss a zzz").format(new Date());
	}



	/**
	 *  Output a line of text to standard out, with datestamp, if debug is set to true.
	 *
	 * @param  s  The String that will be output.
	 */
	protected final void prtlnCore(String s) {
		if (debug)
			System.out.println(getDateStamp() + " FilterCore: " + s);
	}

}

