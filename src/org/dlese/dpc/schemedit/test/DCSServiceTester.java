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
package org.dlese.dpc.schemedit.test;

 import org.dlese.dpc.services.dcs.*;

import org.dlese.dpc.serviceclients.webclient.*;
import org.dlese.dpc.xml.*;
import org.dlese.dpc.xml.schema.DocMap;
import org.dlese.dpc.util.strings.*;
import org.dlese.dpc.util.*;

import java.io.*;
import java.util.*;
import java.text.*;
import java.util.regex.*;

import java.net.*;
import org.dom4j.Node;
import org.dom4j.Element;
import org.dom4j.Document;
import org.dom4j.DocumentFactory;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;

import com.sun.msv.datatype.xsd.*;
import org.relaxng.datatype.*;

/**
 *  Description of the Class
 *
 *@author    ostwald
 <p>$Id: DCSServiceTester.java,v 1.4 2009/03/20 23:33:57 jweather Exp $
 */
public class DCSServiceTester {
	private WebServiceClient webServiceClient = null;
	private DocumentFactory df = DocumentFactory.getInstance();

	String baseUrl = "http://tremor.dpc.ucar.edu:8688/schemedit/services/dcsws1-0";
	String verb;


	/**
	 *  Constructor for the DCSServiceTester object
	 *
	 *@param  verb  Description of the Parameter
	 */
	public DCSServiceTester(String verb) {
		this.verb = verb;
		webServiceClient = new WebServiceClient(baseUrl);
	}


	/**
	 *  Invokes PutRecord service directly (via URI) and displays results
	 *
	 *@param  recordXml   Description of the Parameter
	 *@param  xmlFormat   Description of the Parameter
	 *@param  collection  Description of the Parameter
	 *@param  id          Description of the Parameter
	 */
	private void putRecordDirect(String recordXml, String xmlFormat, String collection, String id) {
		String encodedRecord;
		try {
			encodedRecord = URLEncoder.encode(recordXml, "UTF-8");
		} catch (Exception e) {
			prtln("encoding error: " + e.getMessage());
			return;
		}

		String qs = "verb=" + verb;
		qs += "&recordXml=" + encodedRecord.trim();
		// qs += "&xmlFormat=" + xmlFormat.trim();
		// qs += "&collection=" + collection.trim();
		qs += "&id=" + id.trim();

		prtln ("\n ---------------------------------");
		prtln (qs);
		prtln ("\n ---------------------------------");
		
		URL url;
		Document response;
		try {
			url = new URL(baseUrl + "?" + qs);
			response = Dom4jUtils.getXmlDocument(url);
		} catch (MalformedURLException mue) {
			prtln("URL error: " + mue.getCause());
			return;
		} catch (org.dom4j.DocumentException de) {
			prtln(de.getMessage());
			return;
		}

		prtln(Dom4jUtils.prettyPrint(response));
	}


	/**
	 *  Perform GetId by directly issuing request URL
	 *
	 *@param  collection  Description of the Parameter
	 */
	private void getIdDirect(String collection) {
		String qs = "verb=" + verb;
		qs += "&collection=" + collection;
		URL url;
		Document doc;
		try {
			url = new URL(baseUrl + "?" + qs);
			doc = Dom4jUtils.getXmlDocument(url);
		} catch (MalformedURLException mue) {
			prtln("URL error: " + mue.getCause());
			return;
		} catch (org.dom4j.DocumentException de) {
			prtln(de.getMessage());
			return;
		}

		prtln(Dom4jUtils.prettyPrint(doc));
	}


	/**
	 *  Perform GetId via the {@link org.dlese.dpc.schemedit.WebServiceClient#doGetId(String)} helper method
	 *
	 *@param  collection  Description of the Parameter
	 */
	private void getIdViaHelper(String collection) {
		String newId = null;
		try {
			newId = webServiceClient.doGetId(collection);
		} catch (Exception e) {
			prtln("webservice error: " + e.getMessage());
			return;
		}
		prtln("newId: " + newId);
	}

	/**
	* returns xmlRecord with namespace info removed for the purpose of testing error handling
	*/
	private static String getLocalizedDocStr (String recordXml) throws Exception {
		Document doc = Dom4jUtils.getXmlDocument (recordXml);
		String rootElementName = doc.getRootElement().getName();
		String nameSpaceInfo = Dom4jUtils.getNameSpaceInfo(doc, rootElementName);
		doc = Dom4jUtils.localizeXml(doc, rootElementName);
		return doc.asXML();
	}
	

	/**
	* Performs a PutRecord request using file on disk, and invoking the PutRecord service
	* INDIRECTLY (via WebServiceClient doPutRecord).
	*/
	
	public void putADNRecordTest () {
		String collection = "1102611887199";
		String xmlFormat = "adn";
		String xmlRecordPath = "/devel/ostwald/projects/DcsPutRecordTesters/adn/PutRecordTest2.xml";
		File xmlFile = new File(xmlRecordPath);
		if (!xmlFile.exists()) {
			prtln("xmlFile does not exist at " + xmlRecordPath + " quiting ... ");
			return;
		}
		String recordXml;
		try {
			recordXml = Files.readFile(xmlFile).toString();
		} catch (Exception e) {
			prtln("error reading xmlFile: " + e.getMessage());
			return;
		}
		
		// Get an id and stuff it into the document ...
		String id = null;
		try {
			id = webServiceClient.doGetId (collection);
		} catch (Exception e) {
			prtln ("getId error: " + e.getMessage());
			return;
		}
		prtln ("id: " + id);
		
		try {
			recordXml = WebServiceClient.stuffId (recordXml, xmlFormat, "MyCol-000-000-000-025");
		} catch (Exception e) {
			prtln ("stuffId error: " + e.getMessage());
			return;
		}
		
		putRecordDirect (recordXml, xmlFormat, collection, id);
	}
	
	// This works
	public static void putNewsRecordViaHelper () {
		String collection = "nocc";
		String xmlFormat = "news_opps";
		String baseUrl = "";
		String testRecordDir = "/devel/ostwald/projects/DcsPutRecordTesters";
		File formatRecordsDir = new File (testRecordDir, xmlFormat);
		String fileName = "NewsOppsPutRecordTest.xml";
		
		String server = "tremor";
		
		File xmlFile = new File(formatRecordsDir, fileName);
		if (!xmlFile.exists()) {
			prtln("xmlFile does not exist at " + xmlFile.toString() + " quiting ... ");
			return;
		}
		
		if (server.equals("preview")) {
			baseUrl = "http://preview.dlese.org/schemedit/services/dcsws1-0";
		}
		else if (server.equals("tremor")) {
			baseUrl = "http://tremor.dpc.ucar.edu:8688/schemedit/services/dcsws1-0";
		}
		WebServiceClient webServiceClient = new WebServiceClient(baseUrl);

		String status = "Suggested";
		String statusNote = "Kilroy was here";
		
		String recordXml;
		try {
			recordXml = Files.readFile(xmlFile).toString();
		} catch (Exception e) {
			prtln("error reading xmlFile: " + e.getMessage());
			return;
		}
		try {
			// recordXml = getLocalizedDocStr(recordXml);
			String newRecId = webServiceClient.doPutRecord(recordXml, xmlFormat, collection, status, statusNote);
		} catch (WebServiceClientException e) {
			prtln (e.getMessage());
			return;
		} catch (Exception e) {
			prtln (e.getMessage());
			return;
		}
		prtln ("put Record succeeded");
	}		

	public static void putAdnRecordViaHelper () {
		String collection = "1102611887199";
		String xmlFormat = "adn";
		String baseUrl = "";
		String testRecordDir = "/devel/ostwald/projects/DcsPutRecordTesters";
		File formatRecordsDir = new File (testRecordDir, xmlFormat);
		String fileName = "PutRecordTest.xml";
		
		String server = "tremor";
		
		File xmlFile = new File(formatRecordsDir, fileName);
		if (!xmlFile.exists()) {
			prtln("xmlFile does not exist at " + xmlFile.toString() + " quiting ... ");
			return;
		}
		
		if (server.equals("preview")) {
			baseUrl = "http://preview.dlese.org/schemedit/services/dcsws1-0";
		}
		else if (server.equals("tremor")) {
			baseUrl = "http://tremor.dpc.ucar.edu:8688/schemedit/services/dcsws1-0";
		}
		WebServiceClient webServiceClient = new WebServiceClient(baseUrl);

		String status = "Suggested";
		String statusNote = "Kilroy was here";
		
		String recordXml;
		try {
			recordXml = Files.readFile(xmlFile).toString();
		} catch (Exception e) {
			prtln("error reading xmlFile: " + e.getMessage());
			return;
		}
		try {
			// recordXml = getLocalizedDocStr(recordXml);
			String newRecId = webServiceClient.doPutRecord(recordXml, xmlFormat, collection, status, statusNote);
		} catch (WebServiceClientException e) {
			prtln (e.getMessage());
			return;
		} catch (Exception e) {
			prtln (e.getMessage());
			return;
		}
		prtln ("put Record succeeded");
	}		

	
	/**
	 *  The main program for the DCSServiceTester class
	 *
	 *@param  args  The command line arguments
	 */
	public static void main(String[] args) {
		DCSServiceTester t = new DCSServiceTester ("PutRecord");
		// t.putADNRecordTest();
		
		putAdnRecordViaHelper();
	}


	/**
	 *  Description of the Method
	 *
	 *@param  node  Description of the Parameter
	 */
	private static void pp(Node node) {
		prtln(Dom4jUtils.prettyPrint(node));
	}


	/**
	 *  Description of the Method
	 *
	 *@param  s  Description of the Parameter
	 */
	private static void prtln(String s) {
		// System.out.println("DCSServiceTester: " + s);
		System.out.println(s);
	}
	
	
		/**
	* use WebServiceClient.stuffId instead
	*/
/* 	private static String stuffId (String recordXml, String format, String id) throws Exception {
		// create a localized Document from the xmlString
		Document doc = Dom4jUtils.getXmlDocument (recordXml);
		String rootElementName = doc.getRootElement().getName();
		String nameSpaceInfo = Dom4jUtils.getNameSpaceInfo(doc, rootElementName);
		doc = Dom4jUtils.localizeXml(doc, rootElementName);
		String adnIdPath = "/itemRecord/metaMetadata/catalogEntries/catalog/@entry";
		Node idNode = doc.selectSingleNode(adnIdPath);
		if (idNode == null) {
			prtln (Dom4jUtils.prettyPrint(doc));
			throw new Exception ("idNode not found at " + adnIdPath);
		}
		idNode.setText (id);
		doc = Dom4jUtils.delocalizeXml(doc, rootElementName, nameSpaceInfo);
		if (doc == null) 
			throw new Exception ("not able to delocalize xml");
		// prtln (Dom4jUtils.prettyPrint(doc));
		return  doc.asXML();
	} */

}

