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
package org.dlese.dpc.serviceclients.webclient;

import org.dlese.dpc.xml.*;
import org.dlese.dpc.util.*;
import org.dlese.dpc.xml.schema.DocMap;

import java.lang.*;
import java.net.*;
import java.io.*;
import java.util.*;
import java.util.regex.*;

import org.dom4j.Node;
import org.dom4j.Element;
import org.dom4j.Document;
import org.dom4j.DocumentException;

/**
 *  WebServiceClient provides helpers to communicate with webservices via timed connections (time out is adjustable).<p>
 The helper methods do the following:<ol>
<li> accept parameters, 
<li> package the parameters into a webservice request url, 
<li> submit the request URL to the service server
<li> parse the response into either a value or exception, which are returned to the caller.
</ol>
 Currently two DDS and two DCS webservices
 are supported:<p>
 The DDS helpers submit requests to the DDS Search Web Services and returns
 *  responses as {@link org.dom4j.Document}.
 *  <ol>
 *    <li> UrlCheck - finds resources matching a URL (possibly wildcarded), and
 *    </li>
 *    <li> GetRecord - retrieves a given record by ID </li>
 *  </ol><p>
 The DCS helpers support the following repository services:
 <ol>
 <li>PutRecord - inserts a metadata record into a specified collection in a remote DCS repository
 <li>doGetId - returns a unique identifier for a specified collection of a remote DCS.
 </ol>
 *
 *
 *@author    ostwald <p>
 *
 *      $Id: WebServiceClient.java,v 1.23 2009/03/20 23:33:58 jweather Exp $
 */
public class WebServiceClient {

	/**
	 *  Description of the Field
	 */
	protected static boolean debug = true;
	private static int timeOutSecs = 30;
	
	private String baseWebServiceUrl = null;
	private URL requestUrl = null;


	/**
	 *  Constructor for the WebServiceClient object.<p>
	 Example baseWebServiceUrls: <ul>
	 <li>dds search: "http://dcs.dlese.org/roles/services/ddsws1-0"
	 <li>dcs put: "http://dcs.dlese.org/roles/services/dcsws1-0"
	 </ul>
	 *
	 *@param  baseWebServiceUrl  url of Web Service
	 */
	public WebServiceClient(String baseWebServiceUrl) {
		this.baseWebServiceUrl = baseWebServiceUrl;
	}

	public String getBaseUrl () {
		return baseWebServiceUrl;
	}
	
	public void setTimeOutSecs (int i) {
		timeOutSecs = i;
	}
	
	public int getTimeOutSecs () {
		return timeOutSecs;
	}
	
	/**
	 *  Submit a request (query) to the UrlCheck Web service and return the
	 *  response as a {@link org.dom4j.Document}. The UrlCheck service returns
	 *  items that match the query. the query is a url and may contain asterisks as
	 *  wildcards.
	 *
	 *@param  s                              query to be submitted to UrlCheck
	 *      service.
	 *@return                                result from UrlCheck Service as
	 *      Document
	 *@exception  WebServiceClientException  Description of the Exception
	 */
	public Document urlCheck(String s)
		throws WebServiceClientException {
		String verb = "UrlCheck";
		String encoded = "";
		try {
			encoded = URLEncoder.encode(s, "UTF-8");
		} catch (Exception e) {
			String errorMsg = "WebServiceClient.urlCheck failed to encode: " + s;
			throw new WebServiceClientException(errorMsg);
		}
		setRequestUrl(verb, "url=" + encoded);
		Document doc = getResponseDoc();
		// prtln("\n" + doc.asXML());
		prtln(Dom4jUtils.prettyPrint(doc));
		return doc;
	}
	
	public Document doSearch (String queryStr)
		throws WebServiceClientException {
		String verb = "Search";
		String encodedQueryStr = "";
		try {
			encodedQueryStr = URLEncoder.encode(queryStr, "UTF-8");
		} catch (Exception e) {
			String errorMsg = "WebServiceClient.urlCheck failed to encode: " + queryStr;
			throw new WebServiceClientException(errorMsg);
		}
		setRequestUrl(verb, queryStr);
		prtln ("about to sumbit request: " + getRequestUrl());
		Document doc = getResponseDoc();
		// prtln(Dom4jUtils.prettyPrint(doc));
		return doc;
	}
	

	/**
	 *  Place the provided ID into the provided recordXml. recordXml must be
	 *  "delocalized" - it must contain namespace information in the rootElement. Currently supported xmlFormats
	 are "adn" and "news_opps".
	 
	 *
	 *@param  recordXml      Description of the Parameter
	 *@param  id             Description of the Parameter
	 *@return                Description of the Return Value
	 *@exception  Exception  Description of the Exception
	 */
	public static String stuffId(String recordXml, String xmlFormat, String id)
		throws Exception {
		// prtln ("stuffId: \n\trecordXml: " + recordXml + "\n\txmlFormat: " + xmlFormat + "\n\tid: " + id);
		String idPath;
		if (xmlFormat.equals("adn"))
			idPath = "/itemRecord/metaMetadata/catalogEntries/catalog/@entry";
		else if (xmlFormat.equals("news_opps"))
			idPath = "/news-oppsRecord/recordID";
		else if (xmlFormat.equals("dlese_anno"))
			idPath = "/annotationRecord/service/recordID";
		else
			throw new Exception ("unsupported metadataformat: " + xmlFormat);
			
		// create a localized Document from the xmlString
		Document doc = Dom4jUtils.getXmlDocument(recordXml);
		if (doc == null)
			throw new Exception ("could not parse provided recordXML");
		
		Element rootElement = doc.getRootElement();
		if (rootElement == null)
			throw new Exception ("root element not found");
		
		String rootElementName = rootElement.getName();
		if (rootElementName == null || rootElementName.length() == 0)
			throw new Exception ("rootElementName not found");
		
		String nameSpaceInfo = Dom4jUtils.getNameSpaceInfo(doc, rootElementName);
		if (nameSpaceInfo == null || nameSpaceInfo.trim().length() == 0) {
			throw new Exception("recordXml does not contain required name space information in the root element");
		}

		doc = Dom4jUtils.localizeXml (doc, rootElementName);
		if (doc == null)
			throw new Exception ("doc could not be localized - please unsure the record's root element contains namespace information");
		
		DocMap docMap = new DocMap (doc);
		try {
			docMap.smartPut(idPath, id);
		} catch (Exception e) {
			throw new Exception ("Unable to insert ID: " + e.getMessage());
		}	
		
		doc = Dom4jUtils.delocalizeXml(doc, rootElementName, nameSpaceInfo);
		if (doc == null) {
			throw new Exception("not able to delocalize xml");
		}
		// prtln (Dom4jUtils.prettyPrint(doc));
		return doc.asXML();
	}


	/**
	 *  Generate an ID and insert it in the recordXML before calling the PutRecord
	 *  web service
	 *
	 *@param  recordXml                      xml record to be put
	 *@param  xmlFormat                      metadata format of xml record (e.g., "adn")
	 *@param  collection                     destination collection (e.g., "dcc")
	 *@return                                ID of created record
	 *@exception  WebServiceClientException  Description of the Exception
	 */
	public String doPutRecord(String recordXml, String xmlFormat, String collection, String status, String statusNote)
		throws WebServiceClientException {

		String id = null;
		id = doGetId(collection);
		prtln("doPutRecord got new id: " + id + " from collection " + collection);

		// now stuff it into the xml record
		try {
			recordXml = stuffId(recordXml, xmlFormat, id);
		} catch (Exception e) {
			throw new WebServiceClientException("stuffId error: " + e.getMessage());
		}
		return doPutRecord(recordXml, xmlFormat, collection, id, status, statusNote);
	}

	public String doPutRecord(String recordXml, String xmlFormat, String collection)
		throws WebServiceClientException {
			return doPutRecord (recordXml, xmlFormat, collection, null, null);
	}

	/**
	 *  Assumes id is already placed in the xmlRecord. Note - the ID within the recordXml is ultimately
	 used by the indexer, NOT the provided id (see RepositoryManger.putRecord).
	 *
	 *@param  recordXml                      xml record to be put
	 *@param  xmlFormat                      metadata format of xml record (e.g., "adn")
	 *@param  collection                     destination collection (e.g., "dcc")
	 *@param  id                             xml record id
	 *@return                                ID of created record
	 *@exception  WebServiceClientException  Description of the Exception
	 */
	public String doPutRecord(String recordXml, String xmlFormat, String collection, String id, String status, String statusNote)
		throws WebServiceClientException {
		// prtln("doPutRecord()");
		// prtln (recordXml);
		String errorMsg;
		try {
			String encodedRecord = URLEncoder.encode(recordXml, "UTF-8");

			// package up the request URL
			String argString = "recordXml=" + encodedRecord.trim();
			argString += "&xmlFormat=" + xmlFormat.trim();
			argString += "&collection=" + collection.trim();
			argString += "&id=" + id.trim();
			if (status != null)
				argString += "&dcsStatus=" + status;
			if (statusNote != null)
				argString += "&dcsStatusNote=" + URLEncoder.encode(statusNote, "UTF-8");

			// URL url = new URL(baseUrl + "?" + argString);
			// Document response = Dom4jUtils.getXmlDocument(url);

			String logMsg = "doPutRecord() params:";
			logMsg += "\n\t" + "xmlFormat: " + xmlFormat;
			logMsg += "\n\t" + "collection: " + collection;
			logMsg += "\n\t" + "id: " + id;
			logMsg += "\n\t" + "status: " + status;
			logMsg += "\n\t" + "statusNote: " + statusNote;
			prtln (logMsg);
			
			setRequestUrl("PutRecord", argString);
			Document doc = getResponseDoc();

			// now we have to parse the doc looking for errors
			prtln(Dom4jUtils.prettyPrint(doc));
			Node errorNode = doc.selectSingleNode("/DCSWebService/error");
			if (errorNode != null) {
				throw new Exception(errorNode.getText());
			}

		} catch (UnsupportedEncodingException e) {
			errorMsg = "xmlRecord encoding error: " + e.getMessage();
			throw new WebServiceClientException(errorMsg);
		} catch (Throwable t) {
			errorMsg = t.getMessage();
			throw new WebServiceClientException(errorMsg);
		}
		return id;
	}


	/**
	 *  Requests an id from DCS getId web service. Errors are signaled by an
	 *  exception that contains the error message. otherwise, the Id is returned as
	 *  a string
	 *
	 *@param  collection                     Description of the Parameter
	 *@return                                The id 
	 *@exception  WebServiceClientException  If unable to generate an ID
	 */
	public String doGetId(String collection)
		throws WebServiceClientException {
		String verb = "GetId";
		String encodedArg = "";
		try {
			encodedArg = URLEncoder.encode(collection, "UTF-8");
		} catch (Exception e) {
			String errorMsg = "WebServiceClient.getId failed to encode: " + collection;
			throw new WebServiceClientException(errorMsg);
		}
		setRequestUrl(verb, "collection=" + encodedArg);
		Document doc = getResponseDoc();
		// prtln("\n" + doc.asXML());
		// prtln (Dom4jUtils.prettyPrint(doc));

		Node errorNode = doc.selectSingleNode("/DCSWebService/error");
		if (errorNode != null) {
			throw new WebServiceClientException(errorNode.getText());
		}

		Node idNode = doc.selectSingleNode("/DCSWebService/GetId/id");
		if (idNode != null) {
			return idNode.getText();
		}

		String errorMsg = "WebServiceClient.getId response could not be parsed" + Dom4jUtils.prettyPrint(doc);
		throw new WebServiceClientException(errorMsg);
	}


	/**
	 *  Submits a request to the GetRecord DDS Web Service and returns response as
	 *  a {@link org.dom4j.Document}. The GetRecord service returns an ADN record
	 *  wrapped in a XML response.
	 *
	 *@param  id                             id of the record to get
	 *@return                                the response as a Document
	 *@exception  WebServiceClientException  Description of the Exception
	 */
	public GetRecordResponse getRecord(String id)
		throws WebServiceClientException {
		String verb = "GetRecord";
		setRequestUrl(verb, "id=" + id);
		String responseStr = getResponseStr();
		return new GetRecordResponse(responseStr);
	}


	// ------------------------------------------------------------
	// methods to construct a requestURL --  should these be rewritten (and
	// renamed to be static??
	/**
	 *  Sets the requestUrl attribute of the WebServiceClient object
	 *
	 *@param  url  The new requestUrl value
	 */
	public void setRequestUrl(URL url) {
		requestUrl = url;
	}


	/**
	 *  Gets the requestUrl attribute of the WebServiceClient object
	 *
	 *@return    The requestUrl value
	 */
	public URL getRequestUrl() {
		return requestUrl;
	}

	/**
	 *  Sets the requestUrl attribute of the WebServiceClient object
	 *
	 *@param  verb    The new requestUrl value
	 *@param  argStr  The new requestUrl value
	 */
	public void setRequestUrl(String verb, String argStr) {
		String queryStr = "?verb=" + verb;
		queryStr += "&" + argStr;
		try {
			requestUrl = new URL(baseWebServiceUrl + queryStr);
		} catch (Exception e) {
			prtln("setRequestUrl() " + e);
		}
	}


	// ------ Responses as Strings ---------------------
	/**
	 *  Submits a Web Service and returns the result as a string. Throws a
	 *  WebServiceClientException if the response contains an <b>error</b> element.
	 *
	 *@return                                The responseStr value
	 *@exception  WebServiceClientException  Description of the Exception
	 */
	protected String getResponseStr()
		throws WebServiceClientException {
		String response = null;
		try {
			prtln("getResponseStr() with url=" + requestUrl.toString());
			response = getTimedURL(requestUrl);
			// timed
		} catch (Throwable e) {
			throw new WebServiceClientException(e.getMessage());
		}
		String error = getResponseError(response);
		if ((error != null) && (error.length() > 0)) {
			throw new WebServiceClientException(error);
		}
		else {
			return response;
		}
	}


	/**
	 *  Searches the response string for error elements and returns the contents of
	 *  the error if one is found.
	 *
	 *@param  s  Web service response as string
	 *@return    contents of error if found or empty string if not found
	 */
	public static String getResponseError(String s) {
		Pattern p = Pattern.compile("<error>.+?</error>", Pattern.MULTILINE);
		Matcher m = p.matcher(s);
		if (m.find()) {
			return (s.substring(m.start() + "<error>".length(), m.end() - "</error>".length()));
		}
		else {
			return "";
		}
	}


	// ------ Responses as Documents ---------------------

	/**
	 *  retreives the contents of the <b>requestUrl</b> field as a {@link
	 *  org.dom4j.Document}
	 *
	 *@return                                dom4j Document representation of
	 *      response
	 *@exception  WebServiceClientException  if unsuccessful retrieving url or
	 *      parsing doc
	 */
	protected Document getResponseDoc()
		throws WebServiceClientException {
			return getResponseDoc (this.requestUrl);
	}


	/**
	 *  Static version of getResponseDoc. <p>
	 *
	 *@param  url                            Description of the Parameter
	 *@return                                The responseDoc value
	 *@exception  WebServiceClientException  Description of the Exception
	 */
	public static Document getResponseDoc(URL url)
		throws WebServiceClientException {
		Document responseDoc = null;
		try {
			responseDoc = getTimedXmlDocument(url);
		} catch (Throwable e) {
			throw new WebServiceClientException(e.getMessage());
		}
		String error = getResponseError(responseDoc);
		if ((error != null) && (error.length() > 0)) {
			throw new WebServiceClientException(error);
		}
		else {
			return responseDoc;
		}
	}


	/**
	 *  Gets the responseError attribute of the WebServiceClient class
	 *
	 *@param  doc  Description of the Parameter
	 *@return      The responseError value
	 */
	public static String getResponseError(Document doc) {
		try {
			Node errorNode = doc.selectSingleNode("/DDSWebService/error");
			if (errorNode != null) {
				return errorNode.getText().trim();
			}
		} catch (Exception e) {
			prtln("getResponseError() " + e);
		}
		return "";
	}


	/**
	 *  Uses a {@link org.dlese.dpc.util.TimedURLConnection} to get the repsonse
	 *  from the web service (the request is a URL), which is returned as a String.
	 *
	 *@param  url                            Description of the Parameter
	 *@return                                The timedURL value
	 *@exception  WebServiceClientException  Description of the Exception
	 */
	public static String getTimedURL(URL url)
		throws WebServiceClientException {
		String content = "";
		// prtln ("getTimedURL()");
		try {
			content = TimedURLConnection.importURL(url.toString(), "UTF-8", timeOutSecs * 1000);
		} catch (URLConnectionTimedOutException uctoe) {
			throw new WebServiceClientException(uctoe.getMessage());
		} catch (Exception exc) {			
			String msg = "";
			if (exc.getMessage().matches(".*respcode.*")) {
				msg =
					"The request for data resulted in an invalid response from the server." +
					" The baseURL indicated may be incorrect or the service may be unavailable." +
					" HTTP response: " + exc.getMessage();
			}
			else {
				msg =
					"The request for data resulted in an invalid response from the server. Error: " +
					exc.getMessage();
			}
			throw new WebServiceClientException(msg);
		}
		return content;
	}


	/**
	 *  factory for WebServiceClient
	 *
	 *@param  url                            Description of the Parameter
	 *@return                                an instance of WebServiceClient
	 *@exception  WebServiceClientException  Description of the Exception
	 *@exception  DocumentException          Description of the Exception
	 */
	/*
	    public static WebServiceClient getInstance(String baseUrl) {
	    return new WebServiceClient(baseUrl);
	    }
	  */
	/**
	 *  gets the contents of a URL via {@link #getTimedURL(URL)} and then parses
	 *  the contents into a dom4j Document, which is returned
	 *
	 *@param  url                            url to retrieve
	 *@return                                contents of url as dom4j Document, or
	 *      null if unsuccessful
	 *@exception  WebServiceClientException  Description of the Exception
	 *@exception  DocumentException          Description of the Exception
	 */
	public static Document getTimedXmlDocument(URL url)
		throws WebServiceClientException, DocumentException {
		String urlcontent = getTimedURL(url);
		return Dom4jUtils.getXmlDocument(urlcontent);
	}


	/**
	 *  The main program for the WebServiceClient class
	 *
	 *@param  args  The command line arguments
	 */
	public static void main(String[] args) {
		String serviceUrl = "http://tremor.dpc.ucar.edu:8688/dds/services/ddsws1-0";
		String recId = "DLESE-000-000-004-4091";
		GetRecordResponse gr = null;
		WebServiceClient wsc = new WebServiceClient(serviceUrl);
		try {
			gr = wsc.getRecord(recId);
		} catch (WebServiceClientException e) {
			prtln("WebServiceClientException: " + e.getMessage());
			return;
		}
		prtln("Response:\n" + Dom4jUtils.prettyPrint(gr.getDocument()));
		Document doc = null;
		try {
			doc = gr.getItemRecord();
		} catch (WebServiceClientException e) {
			prtln(e.getMessage());
			return;
		}
		prtln("Doc:\n" + Dom4jUtils.prettyPrint(doc));
	}


	/**
	 *  Sets the debug attribute
	 *
	 *@param  db  The new debug value
	 */
	public static void setDebug(boolean db) {
		debug = db;
	}


	/**
	 *  Description of the Method
	 *
	 *@param  s  Description of the Parameter
	 */
	private static void prtln(String s) {
		if (debug) {
			System.out.println("WebServiceClient: " + s);
		}
	}
}

