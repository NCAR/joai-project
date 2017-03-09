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
package org.dlese.dpc.serviceclients.cat;

import org.dlese.dpc.schemedit.SchemEditUtils;
import org.dlese.dpc.xml.Dom4jUtils;
import org.dlese.dpc.util.TimedURLConnection;
import org.dlese.dpc.util.URLConnectionTimedOutException;
import org.dom4j.*;
import java.util.*;
import java.net.*;

/**
 * @author    Jonathan Ostwald
 */
public abstract class CATWebService {

	private static boolean debug = true;
	// protected final String baseUrl = "http://cat.nsdl.org:8280/casaa/service.do";
	protected String baseUrl;
	protected String username;
	protected String password;
	private static int CONNECTION_TIMEOUT = 60000;
	
	public CATWebService() {
		this ("nsdl", "digi!lib", "http://grace.syr.edu:8080/casaa/service.do");
	}
	
	/**
	 *  Constructor for the CATWebService object
	 *
	 * @param  username  username for authenticating to the CAT Web Service
	 * @param  password  password for authenticating to the CAT Web Service
	 */
	public CATWebService(String username, String password, String baseUrl) {
		this.username = username;
		this.password = password;
		this.baseUrl = baseUrl;
	}


	/**
	 *  Gets the method attribute of the CATWebService object
	 *
	 * @return    The method value
	 */
	protected abstract String getMethod();


	private void displayConstraints(CATRequestConstraints constraints) {
		prtln("----------------------");
		prtln("baseUrl: " + this.baseUrl);
		prtln("username: " + username);
		prtln("password: " + password);
		prtln("method: " + this.getMethod());
		prtln(constraints.toString());
		prtln("----------------------\n");
	}


	/**
	 *  Submit a request to the CAT service and return the response as a Document
	 *
	 * @param  constraints    constraints submitted to CAT Web Service
	 * @return                The response value
	 * @exception  Exception  if a response could not be obtained, or if the
	 *      response contains an error from the server.
	 */
	public Document getResponse(CATRequestConstraints constraints) throws Exception {
		// displayConstraints (constraints);
		String url = this.baseUrl + "?method=" + this.getMethod();
		String queryString = constraints.toQueryString();
		url += "&username=" + username;
		url += "&password=" + password;
		if (queryString.trim().length() > 0)
			url = url + "&" + queryString;

		// prtln ("URL: " + url);
		
		int millis = this.CONNECTION_TIMEOUT;
		String content = null;
		try {
			content = TimedURLConnection.importURL(url, millis);
		} catch (URLConnectionTimedOutException e) {
			throw new Exception("Connection to " + url + " timed out after " + (millis / 1000) + " seconds");
		}
		// prtln (content);
		Document doc = null;
		try {
			doc = Dom4jUtils.getXmlDocument(content);
		} catch (Exception e) {
			prtln("\nRESPONSE PARSE ERROR: " + e.getMessage());
			prtln("\n--------------------------------------\nRESPONSE");
			prtln(content);
			prtln("---------- END RESPONSE --------------\n");
			throw e;
		}
		Element error = (Element) doc.selectSingleNode("/CATWebService/Error");
		if (error != null) {

			/* 			prtln ("url: " + url);*/
			prtln("\nERROR RESPONSE");
			pp(doc);
			prtln("");
			throw new Exception(getErrorMsg(error));
		}
		return doc;
	}


	/**
	 *  Gets the response attribute of the CATWebService object
	 *
	 * @param  argMap         query parameters as key/value pairs
	 * @return                The response value
	 * @exception  Exception  if a response could not be obtained, or if the
	 *      response contains an error from the server.
	 */
	public Document getResponse(Map argMap) throws Exception {

		String query = "";
		for (Iterator i = argMap.keySet().iterator(); i.hasNext(); ) {
			String param = (String) i.next();
			String value = (String) argMap.get(param);
			query += param + "=" + URLEncoder.encode(value);
			if (i.hasNext())
				query += "&";
		}

		URL url = null;
		try {
			url = new URL(this.baseUrl + "?" + query);
			prtln("URL: " + url.toString());
		} catch (Throwable t) {
			throw new Exception("webservice error: " + t);
		}

		int millis = this.CONNECTION_TIMEOUT;
		String content = null;
		try {
			content = TimedURLConnection.importURL(url.toString(), millis);
		} catch (URLConnectionTimedOutException e) {
			throw new Exception("Connection to " + url + " timed out after " + (millis / 1000) + " seconds");
		}
		Document doc = Dom4jUtils.getXmlDocument(content);

		Element error = (Element) doc.selectSingleNode("/CATWebService/Error");
		if (error != null) {
			// pp (doc);
			throw new Exception(getErrorMsg(error));
		}
		return doc;
	}


	/**
	 *  Gets the errorMsg attribute of the CATWebService object
	 *
	 * @param  errorElement  error element from a CAT Service response
	 * @return               The errorMsg value
	 */
	protected String getErrorMsg(Element errorElement) {
		String errorMsg = "Unspecified CATWebService Error";
		try {
			String statusMsg = errorElement.element("StatusMessage").getTextTrim();
			if (statusMsg.length() > 0)
				errorMsg = statusMsg;
		} catch (Throwable t) {}

		try {
			String returnCode = errorElement.element("ReturnCode").getTextTrim();
			if (returnCode.length() > 0)
				errorMsg += " (returnCode: " + returnCode + ")";
		} catch (Throwable t) {}

		try {
			String helpMessage = errorElement.element("HelpMessage").getTextTrim();
			if (helpMessage.length() > 0)
				errorMsg += " HelpMessage: " + helpMessage;
		} catch (Throwable t) {}

		return errorMsg;
	}



	/**
	 *  Description of the Method
	 *
	 * @param  node  Description of the Parameter
	 */
	protected static void pp(Node node) {
		prtln(Dom4jUtils.prettyPrint(node));
	}


	/**
	 *  Description of the Method
	 *
	 * @param  s  Description of the Parameter
	 */
	private static void prtln(String s) {
		String prefix = "CATWebService";
		if (debug) {
			SchemEditUtils.prtln(s, prefix);
		}
	}

}

