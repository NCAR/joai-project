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
package org.dlese.dpc.suggest;

import org.dlese.dpc.suggest.resource.urlcheck.UrlValidator;
import org.dlese.dpc.suggest.resource.urlcheck.ValidatorResults;

import org.dlese.dpc.serviceclients.webclient.WebServiceClient;
import org.dlese.dpc.serviceclients.webclient.WebServiceClientException;
import org.dlese.dpc.schemedit.url.UrlHelper;
import org.dlese.dpc.xml.schema.*;
import org.dlese.dpc.xml.*;
import org.dlese.dpc.util.*;

import java.io.*;
import java.util.*;
import java.text.*;

import java.net.URL;

import org.dom4j.Element;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dlese.dpc.util.Files;

/**
 *  Provides functionality to help Suggestor clients collection suggestions and
 *  write them to DCS instances.
 *  <ul>
 *    <li> newRecord - produces a SuggestionRecord for the nativeFormat of this
 *    Suggestor
 *    <li> repositoryService - writes SuggestionRecords to a DCS instance
 *  </ul>
 *  Subclasses may provide additional functionality, such as
 *  <ul>
 *    <li> Dup checking - to make sure a suggestion is received only once
 *    <li> Search Service - to extract information from a record to be annotated
 *
 *  </ul>
 *
 *
 * @author    Jonathan Ostwald
 */
public abstract class SuggestHelper {

	private static boolean debug = true;

	/**
	 *  Helper providing schema-related functionalities for the framework of the
	 *  suggestions
	 */
	protected SchemaHelper schemaHelper = null;
	/**  XML File from which suggestionRecords are created */
	protected File recordTemplate = null;
	/**  DcsStatus given to SuggestionRecords */
	protected String dcsStatus = null;
	/**  dcsStatusNote given to SuggestionRecords */
	protected String dcsStatusNote = null;

	/**  Collection in DCS instance in which suggestions are written */
	private String destCollection = null;
	/**  Web service that writes suggestions to DCS instance */
	private WebServiceClient repositoryServiceClient = null;

	private String mailServer = null;
	private String[] emailTo = null;
	private String emailFrom = null;


	/**
	 *  Gets the native framework of the suggestionRecords produced by this
	 *  Suggestor.
	 *
	 * @return    The xmlFormat value
	 */
	public abstract String getXmlFormat();


	/**
	 *  SuggestHelper Constructor
	 *
	 * @param  recordTemplate  template used to create new SuggestRecord instances
	 * @param  schemaHelper    schemaHelper instance for this SuggestHelper's
	 *      native framework
	 */
	public SuggestHelper(File recordTemplate, SchemaHelper schemaHelper) {
		this.schemaHelper = schemaHelper;
		this.recordTemplate = recordTemplate;

		// verify that the template record file exists
		if (!recordTemplate.exists()) {
			prtlnErr("SuggestHelper(): recordTemplate not found");
		}
	}


	/**
	 *  Gets the schemaHelper attribute of the SuggestHelper object
	 *
	 * @return    The schemaHelper value
	 */
	public SchemaHelper getSchemaHelper() {
		return schemaHelper;
	}


	/**
	 *  The dcsStatus that is given to suggested records.
	 *
	 * @param  status  The new dcsStatus value
	 */
	public void setDcsStatus(String status) {
		this.dcsStatus = status;
	}


	/**
	 *  The dcsStatusNote that is given to suggested records.
	 *
	 * @param  statusNote  The new dcsStatusNote value
	 */
	public void setDcsStatusNote(String statusNote) {
		this.dcsStatusNote = statusNote;
	}


	/**
	 *  The collection of the DCS in which suggestions are placed.
	 *
	 * @param  collection  The new destCollection value
	 */
	public void setDestCollection(String collection) {
		this.destCollection = collection;
	}


	/**
	 *  Gets the destCollection attribute of the SuggestHelper object
	 *
	 * @return    The destCollection value
	 */
	public String getDestCollection() {
		return this.destCollection;
	}


	/**
	 *  Gets the repositoryServiceClient attribute of the SuggestHelper object,
	 *  used to place suggestions in a DCS instance.
	 *
	 * @return    The repositoryServiceClient value
	 */
	public WebServiceClient getRepositoryServiceClient() {
		return this.repositoryServiceClient;
	}


	/**
	 *  The repositoryServiceClient is used to place suggestions in a DCS instance.
	 *
	 * @param  wsc  The new repositoryServiceClient value
	 */
	public void setRepositoryServiceClient(WebServiceClient wsc) {
		this.repositoryServiceClient = wsc;
	}


	/**
	 *  Sets the mailServer attribute of the SuggestHelper object
	 *
	 * @param  mailServer  The new mailServer value
	 */
	public void setMailServer(String mailServer) {
		this.mailServer = mailServer;
	}


	/**
	 *  Gets the mailServer attribute of the SuggestHelper object
	 *
	 * @return    The mailServer value
	 */
	public String getMailServer() {
		return this.mailServer;
	}


	/**
	 *  Sets the emailTo attribute of the SuggestHelper object
	 *
	 * @param  addresses  The new emailTo value
	 */
	public void setEmailTo(String[] addresses) {
		this.emailTo = addresses;
	}


	/**
	 *  Gets the emailTo attribute of the SuggestHelper object
	 *
	 * @return    The emailTo value
	 */
	public String[] getEmailTo() {
		return this.emailTo;
	}


	/**
	 *  Sets the emailFrom attribute of the SuggestHelper object
	 *
	 * @param  address  The new emailFrom value
	 */
	public void setEmailFrom(String address) {
		this.emailFrom = address;
	}


	/**
	 *  Gets the emailFrom attribute of the SuggestHelper object
	 *
	 * @return    The emailFrom value
	 */
	public String getEmailFrom() {
		return this.emailFrom;
	}


	/**
	 *  Creates a new {@link ResourceRecord} instance from the <i>recordTemplate
	 *  </i> file.
	 *
	 * @return                Description of the Return Value
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public abstract SuggestionRecord newRecord() throws Exception;


	/**
	 *  Insert a suggested record into the DCS specified by Suggestor's
	 *  configuration.
	 *
	 * @param  rec            NOT YET DOCUMENTED
	 * @return                ID of record in destination DCS
	 * @exception  Exception  Description of the Exception
	 */
	public abstract String putRecordToDCS(SuggestionRecord rec) throws Exception;


	/**
	 *  Gets the baseUrl of the DCS instance in which suggestions are put. Used to
	 *  construct a URL to view the suggestion in the destination DCS.
	 *
	 * @return    The viewBaseUrl value
	 */
	public String getViewBaseUrl() {
		String repoUrl = null;
		String repositoryServiceBaseUrl = repositoryServiceClient.getBaseUrl();
		prtln("getViewBaseUrl()\n\t repositoryServiceBaseUrl: " + repositoryServiceBaseUrl);
		try {
			URL baseUrl = UrlHelper.getUrl(repositoryServiceBaseUrl);
			// generate instance URL from repositoryBaseServiceUrl
			StringBuffer s = new StringBuffer();
			s.append(baseUrl.getProtocol());
			s.append("://");
			s.append(baseUrl.getHost());
			if (baseUrl.getPort() != -1) {
				s.append(":" + Integer.toString(baseUrl.getPort()));
			}
			s.append("/");
			String instanceName = UrlHelper.getPathItem(baseUrl, 1);
			if (instanceName != null && instanceName.length() > 0) {
				s.append(instanceName);
				s.append("/browse");
			}
			repoUrl = s.toString();
		} catch (Exception e) {
			prtln("getViewBaseUrl error: " + e.getMessage());
		} catch (Throwable t) {
			t.printStackTrace();
		}
		prtln(" ... returning " + repoUrl);
		return repoUrl;
	}


	/**
	 *  Sets the debug attribute of the SuggestHelper object
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
			org.dlese.dpc.schemedit.SchemEditUtils.prtln(s, "SuggestHelper");
		}
	}


	/**
	 *  Return a string for the current time and date, sutiable for display in log
	 *  files and output to standout:
	 *
	 * @return    The dateStamp value
	 */
	protected static String getDateStamp() {
		return
			new SimpleDateFormat("MMM d, yyyy h:mm:ss a zzz").format(new Date());
	}


	/**
	 *  Output a line of text to error out, with datestamp.
	 *
	 * @param  s  The text that will be output to error out.
	 */
	protected final static void prtlnErr(String s) {
		System.err.println(getDateStamp() + " ERROR: " + s);
	}

}

