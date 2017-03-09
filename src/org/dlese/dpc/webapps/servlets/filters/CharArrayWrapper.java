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

import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;

/**
 *  A response wrapper that takes the servlet's or JSP's response and saves it
 *  into a char[] for use in Filters. The array can then be acted upon using a
 *  Filter to do post-processing on the respose content prior to it being sent
 *  out. See More Servlets and JavaServer Pages, chapter 9.
 *
 * @author    John Weatherley
 */
public class CharArrayWrapper extends HttpServletResponseWrapper
{
	private CharArrayWriter charWriter;
	private int contentLength;
	private String contentType;
	private int status_code;
	private String error_msg = null;


	/**
	 *  Constructor for the CharArrayWrapper object
	 *
	 * @param  response  The HttpServletResponse.
	 */
	public CharArrayWrapper(HttpServletResponse response) {
		super(response);
		charWriter = new CharArrayWriter();
	}


	/**
	 *  Replace the normal writer with a writer that ouputs our char[]. This char[]
	 *  will then be used when ouputting the response instead of the default
	 *  writer.
	 *
	 * @return    The writer value
	 */
	public PrintWriter getWriter() {
		return new PrintWriter(charWriter, true);
	}


	/**
	 *  Convert to String.
	 *
	 * @return    A String representataion of the char[] content.
	 */
	public String toString() {
		return charWriter.toString();
	}


	/**
	 *  Convert to char[].
	 *
	 * @return    A char[] of the CharArray content.
	 */
	public char[] toCharArray() {
		return charWriter.toCharArray();
	}



	/**
	 *  DESCRIPTION
	 *
	 * @param  sc               DESCRIPTION
	 * @exception  IOException  DESCRIPTION
	 */
	public void sendError(int sc)
			 throws IOException {
		this.status_code = sc;
		super.sendError(sc);
	}


	/**
	 *  DESCRIPTION
	 *
	 * @param  sc               DESCRIPTION
	 * @param  msg              DESCRIPTION
	 * @exception  IOException  DESCRIPTION
	 */
	public void sendError(int sc, String msg)
			 throws IOException {
		this.error_msg = msg;
		this.status_code = sc;
		super.sendError(sc, msg);
	}


	/**
	 *  Sets the status attribute of the CharArrayWrapper object
	 *
	 * @param  sc  The new status value
	 */
	public void setStatus(int sc) {
		this.status_code = sc;
		super.setStatus(sc);
	}


	/**
	 *  Gets the status attribute of the CharArrayWrapper object
	 *
	 * @return    The status value
	 */
	public int getStatus() {
		return status_code;
	}


	/**
	 *  Gets the errorMsg attribute of the CharArrayWrapper object
	 *
	 * @return    The errorMsg value
	 */
	public String getErrorMsg() {
		return error_msg;
	}


	/**
	 *  Sets the contentLength attribute of the GenericResponseWrapper object
	 *
	 * @param  length  The new contentLength value
	 */
	public void setContentLength(int length) {
		this.contentLength = length;
		super.setContentLength(length);
	}


	/**
	 *  Gets the contentLength attribute of the GenericResponseWrapper object
	 *
	 * @return    The contentLength value
	 */
	public int getContentLength() {
		return contentLength;
	}


	/**
	 *  Sets the contentType attribute of the GenericResponseWrapper object
	 *
	 * @param  type  The new contentType value
	 */
	public void setContentType(String type) {
		this.contentType = type;
		super.setContentType(type);
	}


	/**
	 *  Gets the contentType attribute of the GenericResponseWrapper object
	 *
	 * @return    The contentType value
	 */
	public String getContentType() {
		return contentType;
	}

}

