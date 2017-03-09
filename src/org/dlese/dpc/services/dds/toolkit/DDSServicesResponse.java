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
package org.dlese.dpc.services.dds.toolkit;

import org.dom4j.*;
import java.util.*;
import java.text.*;

/**
 *  Encapsulates a response from a DDSWS or DDSUpdateWS service request.
 *
 * @author    John Weatherley
 */
public final class DDSServicesResponse {
	private static boolean debug = true;

	private Document responseDocument = null;
	private String requestMade = null;


	/**
	 *  Constructor for the DDSServicesResponse object
	 *
	 * @param  requestMade       The request made to the service
	 * @param  responseDocument  The response 
	 */
	protected DDSServicesResponse(String requestMade, Document responseDocument) {
		this.responseDocument = responseDocument;
		this.requestMade = requestMade;
	}


	/**
	 *  Gets the service response.
	 *
	 * @return    The responseDocument value
	 */
	public Document getResponseDocument() {
		return responseDocument;
	}


	/**
	 *  Gets the full request that was made to the service.
	 *
	 * @return    The requestString value
	 */
	public String getRequestString() {
		return requestMade;
	}
	
	public String toString() {
		if(responseDocument == null && requestMade == null)
			return "null";
		return "Request: " + requestMade + "\n\nResponse:\n\n" + responseDocument.asXML();
	}

	// ---------------------- Debug info --------------------

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
	 *  Output a line of text to error out, with datestamp.
	 *
	 * @param  s  The text that will be output to error out.
	 */
	private final static void prtlnErr(String s) {
		System.err.println(getDateStamp() + " DDSServicesResponse Error: " + s);
	}

	/**
	 *  Output a line of text to standard out, with datestamp, if debug is set to true.
	 *
	 * @param  s  The String that will be output.
	 */
	private final static void prtln(String s) {
		if (debug) {
			System.out.println(getDateStamp() + " DDSServicesResponse: " + s);
		}
	}


	/**
	 *  Sets the debug attribute of the object
	 *
	 * @param  db  The new debug value
	 */
	public static void setDebug(boolean db) {
		debug = db;
	}
}

