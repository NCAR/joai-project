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
package org.dlese.dpc.suggest.comment;

import org.dlese.dpc.suggest.SuggestUtils;
import org.dlese.dpc.suggest.SuggestHelper;
import org.dlese.dpc.suggest.SuggestionRecord;

import org.dlese.dpc.serviceclients.webclient.WebServiceClient;
import org.dlese.dpc.serviceclients.webclient.GetRecordResponse;
import org.dlese.dpc.serviceclients.webclient.WebServiceClientException;

import org.dlese.dpc.xml.schema.SchemaHelper;
import org.dlese.dpc.xml.Dom4jUtils;

import java.io.File;
import java.util.*;

import org.dom4j.Node;
import org.dom4j.Element;
import org.dom4j.Document;
import org.dom4j.DocumentException;

/**
 *  Provides services for SuggestComment Client.
 *
 * @author    Jonathan Ostwald
 */
public class SuggestCommentHelper extends SuggestHelper {

	private static boolean debug = true;

	/**  NOT YET DOCUMENTED */
	protected String searchServiceUrl = null;
	/**  NOT YET DOCUMENTED */
	protected WebServiceClient searchServiceClient = null;


	/**
	 *  Gets the xmlFormat attribute of the SuggestCommentHelper object
	 *
	 * @return    The xmlFormat value
	 */
	public String getXmlFormat() {
		return "dlese_anno";
	}


	/**
	 *  SuggestCommentHelper Constructor. The repositoryDir param points to a
	 *  directory that contains a default record (used for making new records), and
	 *  a records directory (where suggested urls are stored). This stuff is all
	 *  hard coded for now, but properties can be used to supply defaults (see
	 *  {@link org.dlese.dpc.suggest.SuggestCommentHelper}).
	 *
	 * @param  recordTemplate  NOT YET DOCUMENTED
	 * @param  schemaHelper    NOT YET DOCUMENTED
	 */
	public SuggestCommentHelper(File recordTemplate, SchemaHelper schemaHelper) {
		super(recordTemplate, schemaHelper);
	}


	/**
	 *  Creates a new {@link ResourceRecord} instance by reading from the
	 *  recordTemplate. We don't call readRecord (file) because we don't want the
	 *  id to be set for the new record
	 *
	 * @return                Description of the Return Value
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public CommentRecord newRecord() throws Exception {

		Document rawDoc = Dom4jUtils.getXmlDocument(recordTemplate);
		String rootElementName = rawDoc.getRootElement().getName();
		Document doc = Dom4jUtils.localizeXml(rawDoc, rootElementName);
		return new CommentRecord(doc, schemaHelper);
	}


	/**
	 *  Gets the itemRecordProps attribute of the SuggestCommentHelper object
	 *
	 * @param  id             NOT YET DOCUMENTED
	 * @return                The itemRecordProps value
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public Map getItemRecordProps(String id) throws Exception {
		Map props = new HashMap();
		Document itemRecord = null;
		try {
			GetRecordResponse response = this.searchServiceClient.getRecord(id);
			itemRecord = response.getItemRecord();
			if (itemRecord == null)
				throw new Exception("unable to retrieve itemRecord");
		} catch (Throwable t) {
			prtln(t.getMessage());
			// t.printStackTrace();
			throw new Exception("Record not found for " + id);
		}

		String title_path = "/itemRecord/general/title";
		try {
			props.put("title", ((Element) itemRecord.selectSingleNode(title_path)).getText());
		} catch (Throwable t) {}

		String id_path = "/itemRecord/metaMetadata/catalogEntries/catalog/@entry";
		try {
			props.put("id", ((Node) itemRecord.selectSingleNode(id_path)).getText());
		} catch (Throwable t) {}

		String url_path = "/itemRecord/technical/online/primaryURL";
		try {
			props.put("url", ((Element) itemRecord.selectSingleNode(url_path)).getText());
		} catch (Throwable t) {}

		prtln("title: " + (String) props.get("title"));
		prtln("url: " + (String) props.get("url"));
		prtln("id: " + (String) props.get("id"));

		return props;
	}


	/**
	 *  Insert a suggested record into the DCS specified by client configuration.
	 *
	 * @param  rec            NOT YET DOCUMENTED
	 * @return                ID of record in destination DCS
	 * @exception  Exception  Description of the Exception
	 */
	public String putRecordToDCS(SuggestionRecord rec) throws Exception {

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
			throw new Exception("doc could not be obtained from SuggestCommentHelper");
		}

		// insert namespace info
		String nameSpaceInfo = SuggestUtils.getNameSpaceInfo(schemaHelper);
		String rootElementName = doc.getRootElement().getName();
		Document annoDoc = Dom4jUtils.delocalizeXml(doc, rootElementName, nameSpaceInfo);

		String newRecId = null;
		try {
			newRecId = getRepositoryServiceClient().doPutRecord(annoDoc.asXML(),
				"dlese_anno",
				getDestCollection(),
				dcsStatus,
				dcsStatusNote);
		} catch (WebServiceClientException e) {
			throw new Exception("WebService error: " + e.getMessage());
		}

		return newRecId;
	}


	/**
	 *  Gets the searchServiceClient attribute of the SuggestCommentHelper object
	 *
	 * @return    The searchServiceClient value
	 */
	public WebServiceClient getSearchServiceClient() {
		return this.searchServiceClient;
	}


	/**
	 *  Sets the searchServiceClient attribute of the SuggestCommentHelper object
	 *
	 * @param  wsc  The new searchServiceClient value
	 */
	public void setSearchServiceClient(WebServiceClient wsc) {
		this.searchServiceClient = wsc;
	}


	/**
	 *  Sets the debug attribute of the SuggestCommentHelper object
	 *
	 * @param  db  The new debug value
	 */
	public static void setDebug(boolean db) {
		debug = db;
	}


	/**
	 *  Print the string with trailing newline to std output
	 *
	 * @param  s  string to print
	 */
	private static void prtln(String s) {
		if (debug) {
			org.dlese.dpc.schemedit.SchemEditUtils.prtln(s, "SuggestCommentHelper");
		}
	}

}

