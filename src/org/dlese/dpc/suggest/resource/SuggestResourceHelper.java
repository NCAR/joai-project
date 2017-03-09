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
package org.dlese.dpc.suggest.resource;

import org.dlese.dpc.suggest.SuggestUtils;
import org.dlese.dpc.suggest.SuggestHelper;
import org.dlese.dpc.suggest.SuggestionRecord;

import org.dlese.dpc.suggest.resource.urlcheck.UrlValidator;
import org.dlese.dpc.suggest.resource.urlcheck.ValidatorResults;

import org.dlese.dpc.serviceclients.webclient.WebServiceClient;
import org.dlese.dpc.serviceclients.webclient.WebServiceClientException;

import org.dlese.dpc.xml.schema.SchemaHelper;
import org.dlese.dpc.xml.Dom4jUtils;

import java.io.File;
import java.util.*;

import org.dom4j.Element;
import org.dom4j.Document;
import org.dom4j.DocumentException;

/**
 *  SuggestUrlManger - provides services for suggestor Client.
 *  
 *
 *@author    Jonathan Ostwald
 */
public class SuggestResourceHelper extends SuggestHelper {

	private static boolean debug = true;
	
	private UrlValidator urlValidator = null;

	public String getXmlFormat() {
		return "adn";
	}
	
	/**
	 *  SuggestResourceHelper Constructor. The repositoryDir param points to a
	 *  directory that contains a default record (used for making new records), and
	 *  a records directory (where suggested urls are stored). This stuff is all
	 *  hard coded for now, but properties can be used to supply defaults (see
	 *  {@link org.dlese.dpc.suggest.SuggestResourceHelper}).
	 *
	 *@param  repositoryDir  directory holding default record and records directory
	 */
	public SuggestResourceHelper(File recordTemplate, SchemaHelper schemaHelper) {
		super (recordTemplate, schemaHelper);
	}
	
	public UrlValidator getUrlValidator () {
		return this.urlValidator;
	}
	
	public void setUrlValidator (UrlValidator validator) {
		this.urlValidator = validator;
	}
	
	/**
	 *  Creates a new {@link ResourceRecord} instance by reading from the
	 *  recordTemplate. We don't call readRecord (file) because we don't want the
	 *  id to be set for the new record
	 *
	 *@return    Description of the Return Value
	 */
	public ResourceRecord newRecord() throws Exception {

		Document rawDoc = Dom4jUtils.getXmlDocument(recordTemplate);
		String rootElementName = rawDoc.getRootElement().getName();
		Document doc = Dom4jUtils.localizeXml(rawDoc, rootElementName);
		return new ResourceRecord(doc, schemaHelper);

	}

	public String putRecordToDCS(SuggestionRecord rec) throws Exception {
		throw new Exception ("putRecordToDCS requires a ValidatorResults instance");
	}
	
	/**
	 *  Insert a suggested record into the DCS specified by client configuration.
	 *
	 *@param  sm             Description of the Parameter
	 *@return                ID of record in destination DCS
	 *@exception  Exception  Description of the Exception
	 */
	public String putRecordToDCS(SuggestionRecord rec, ValidatorResults validatorResults)
		throws Exception {

		if (validatorResults.hasSimilarUrls()) {
			dcsStatusNote += validatorResults.similarUrlReportForDcsStatusNote();
		}
				
		prtln("putRecordToDCS(): dcsStatus=" + dcsStatus + ", dcsStatusNote=" + dcsStatusNote);

		String destCollection = this.getDestCollection();
		if (destCollection == null || destCollection.trim().length() == 0) {
			prtln("destCollection not specified, putRecordToDCS exiting ... ");
			return null;
		}
		
		if (rec == null) {
			throw new Exception("SuggestionRecord is null");
		}
		Document doc = rec.getDoc();
		if (doc == null) {
			throw new Exception("doc could not be obtained from SuggestResourceHelper");
		}

		// insert namespace info
		String nameSpaceInfo = SuggestUtils.getNameSpaceInfo(schemaHelper);
		String rootElementName = doc.getRootElement().getName();
		Document adnDoc = Dom4jUtils.delocalizeXml(doc, rootElementName, nameSpaceInfo);

		String newRecId = null;
		try {
			newRecId = getRepositoryServiceClient().doPutRecord(adnDoc.asXML(), 
														   "adn", 
														   getDestCollection(), 
														   dcsStatus, 
														   dcsStatusNote);
		} catch (WebServiceClientException e) {
			throw new Exception("WebService error: " + e.getMessage());
		}

		return newRecId;
	}

	/**
	 *  Sets the debug attribute of the SuggestResourceHelper object
	 *
	 *@param  db  The new debug value
	 */
	public static void setDebug(boolean db) {
		debug = db;
	}


	/**
	 *  Print the string with trailing newline to std output
	 *
	 *@param  s  string to print
	 */
	private static void prtln(String s) {
		if (debug) {
			org.dlese.dpc.schemedit.SchemEditUtils.prtln (s, "SuggestResourceHelper");
		}
	}

}

