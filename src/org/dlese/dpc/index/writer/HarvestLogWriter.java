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
package org.dlese.dpc.index.writer;

import java.io.*;
import java.util.*;
import java.text.*;

import org.dlese.dpc.xml.*;
import org.dlese.dpc.index.*;
import org.dlese.dpc.util.*;
import org.dlese.dpc.oai.OAIUtils;
import org.apache.lucene.document.*;
import javax.servlet.http.*;
import org.dlese.dpc.index.document.DateFieldTools;

/**
 *  Writes a Lucene Document that holds information about a harvest. <br>
 *  The Lucene {@link org.apache.lucene.document.Document} fields that are created by this class are: <br>
 *  <br>
 *  <code><b>requestdate</b> </code> - The date of the client request. Stored. <br>
 *  <code><b>requesturl</b> </code> - The URL and query the client requested. Stored.<br>
 *  <code><b>remotehost</b> </code> - The requesting client's host name or IP address. Stored.<br>
 *  <code><b>notes</b> </code> - Free text notes related to this log entry. Stored.<br>
 *  <code><b>doctype</b> </code> - The document format type, which is 'weblog,' with '0' appended to support
 *  wildcard searching.<br>
 *  <code><b>readerclass</b> </code> - The class which is used to read {@link
 *  org.apache.lucene.document.Document}s created by this writer, which is 'WebLogReader'.<br>
 *  <code><b>admindefaultfield</b> </code> - The default field that holds all content for searching.
 *  'WebLogReader'.<br>
 *  <br>
 *
 *
 * @author     John Weatherley
 * @see        org.dlese.dpc.index.reader.HarvestLogReader
 */
public class HarvestLogWriter implements DocWriter {
	/**  NOT YET DOCUMENTED */
	public static int HARVEST_IN_PROGRESS = 0;
	/**  NOT YET DOCUMENTED */
	public static int COMPLETED_SUCCESSFUL = 1;
	/**  NOT YET DOCUMENTED */
	public static int COMPLETED_OAI_ERROR = 2;
	/**  NOT YET DOCUMENTED */
	public static int COMPLETED_SERIOUS_ERROR = 3;

	private String baseURL = null;
	private String set = null;
	private Date from = null;
	private Date until = null;
	private String shUid = null;
	private String repositoryName = null;



	/**
	 *  Gets doctype, which is "harvestlog".
	 *
	 * @return    The String "harvestlog".
	 */
	public String getDocType() {
		return "harvestlog";
	}


	/**
	 *  Gets the name of the concrete {@link org.dlese.dpc.index.reader.DocReader} class that is used to read
	 *  this type of {@link org.apache.lucene.document.Document}, which is "HarvestLogReader".
	 *
	 * @return    The String "HarvestLogReader".
	 */
	public String getReaderClass() {
		return "org.dlese.dpc.index.reader.HarvestLogReader";
	}


	/**
	 *  Constructor for the HarvestLogWriter object
	 *
	 * @param  repositoryName  Repos name
	 * @param  baseURL         baseUrl
	 * @param  set             set, or null if none
	 * @param  shUid           The harvest Uid
	 */
	public HarvestLogWriter(String repositoryName,
	                        String baseURL,
	                        String set,
	                        String shUid) {
		this.repositoryName = repositoryName;
		this.baseURL = baseURL;
		this.set = set;
		this.shUid = shUid;
	}


	/**
	 *  Sets the harvest attributes for this harvest.
	 *
	 * @param  from   The from date or null if none used
	 * @param  until  The until date or null if none used
	 */
	public final void setHarvestAttributes(Date from, Date until) {
		this.from = from;
		this.until = until;
	}


	/**
	 *  Logs an entry in the index.
	 *
	 * @param  harvestUid            Harvest Uid
	 * @param  startTime             Start time
	 * @param  endTime               End time
	 * @param  recordCount           Num records
	 * @param  resumptionCount       Num resumption tokens issued
	 * @param  messageType           The message type
	 * @param  harvestedRecordsDir   Dir where harvested records are saved
	 * @param  zipFilePathName       Zip file
	 * @param  supportedGranularity  Granularity
	 * @param  deletedRecordSupport  Deleted record support
	 * @param  oaiErrCode            OAI error code, or null
	 * @param  message               Message to be logged
	 * @return                       Document to be inserted into the index
	 */
	public final Document logEntry(
	                               long harvestUid,
	                               long startTime,
	                               long endTime,
	                               int recordCount,
	                               int resumptionCount,
	                               int messageType,
	                               String harvestedRecordsDir,
	                               String zipFilePathName,
	                               String supportedGranularity,
	                               String deletedRecordSupport,
	                               String oaiErrCode,
	                               String message) {

		Document doc = new Document();
		StringBuffer adminDefaultBuffer = new StringBuffer();

		try {
			doc.add(new Field("harvestdir", harvestedRecordsDir, Field.Store.YES, Field.Index.NOT_ANALYZED));
			doc.add(new Field("harvestuid", Long.toString(harvestUid), Field.Store.YES, Field.Index.ANALYZED));
			doc.add(new Field("numharvestedrecords", Integer.toString(recordCount), Field.Store.YES, Field.Index.ANALYZED));
			doc.add(new Field("numresumptiontokens", Integer.toString(resumptionCount), Field.Store.YES, Field.Index.ANALYZED));
			try {
				doc.add(new Field("starttime", DateFieldTools.timeToString(startTime), Field.Store.YES, Field.Index.ANALYZED));
			} catch (Throwable e) {
				doc.add(new Field("starttime", "-1", Field.Store.YES, Field.Index.ANALYZED));
			}
			try {
				doc.add(new Field("endtime", DateFieldTools.timeToString(endTime), Field.Store.YES, Field.Index.ANALYZED));
			} catch (Throwable e) {
				doc.add(new Field("endtime", "-1", Field.Store.YES, Field.Index.ANALYZED));
			}
			if (zipFilePathName != null)
				doc.add(new Field("zipfile", zipFilePathName, Field.Store.YES, Field.Index.NOT_ANALYZED));

			if (supportedGranularity != null)
				doc.add(new Field("supportedgranularity", supportedGranularity, Field.Store.YES, Field.Index.ANALYZED));
			if (deletedRecordSupport != null)
				doc.add(new Field("deletedrecordsupport", deletedRecordSupport, Field.Store.YES, Field.Index.ANALYZED));

			if (message != null)
				doc.add(new Field("message", message, Field.Store.YES, Field.Index.ANALYZED));
			if (oaiErrCode != null)
				doc.add(new Field("oaierrcode", oaiErrCode, Field.Store.YES, Field.Index.ANALYZED));

		} catch (Throwable e) {
			System.err.println("Error creating harvest logEntry(): " + e);
		}


		String entryType = "";
		if (messageType == HARVEST_IN_PROGRESS)
			entryType = "inprogress";
		else if (messageType == COMPLETED_SUCCESSFUL)
			entryType = "completedsuccessful";
		else if (messageType == COMPLETED_OAI_ERROR)
			entryType = "completederroroai";
		else if (messageType == COMPLETED_SERIOUS_ERROR)
			entryType = "completederrorserious";

		doc.add(new Field("entrytype", entryType, Field.Store.YES, Field.Index.ANALYZED));
		return addRequiredFields(doc, adminDefaultBuffer);
	}


	private Document addRequiredFields(Document doc, StringBuffer adminDefaultBuffer) {
		long curTime = System.currentTimeMillis();
		doc.add(new Field("logdate", DateFieldTools.timeToString(curTime), Field.Store.YES, Field.Index.ANALYZED));
		addToAdminDefaultField(
			new SimpleDateFormat("EEE MMM d, yyyy h:mm:ss a zzz").format(new Date(curTime)), adminDefaultBuffer);

		doc.add(new Field("baseurl", baseURL, Field.Store.YES, Field.Index.NOT_ANALYZED));
		addToAdminDefaultField(tokenizeURI(baseURL), adminDefaultBuffer);
		if (set == null || set.length() == 0)
			set = "noset";
		doc.add(new Field("set", set, Field.Store.YES, Field.Index.ANALYZED));
		doc.add(new Field("repositoryname", repositoryName, Field.Store.YES, Field.Index.ANALYZED));
		doc.add(new Field("uid", shUid, Field.Store.YES, Field.Index.ANALYZED));
		if (from != null)
			doc.add(new Field("from", OAIUtils.getDatestampFromDate(from), Field.Store.YES, Field.Index.NOT_ANALYZED));
		if (until != null)
			doc.add(new Field("until", OAIUtils.getDatestampFromDate(from), Field.Store.YES, Field.Index.NOT_ANALYZED));

		// Add the default field:
		doc.add(new Field("admindefault", adminDefaultBuffer.toString(), Field.Store.NO, Field.Index.ANALYZED));

		// ------------ DocWriter fields -----------------

		// See class JavaDoc for details on this field.
		doc.add(new Field("doctype", '0' + getDocType(), Field.Store.YES, Field.Index.NOT_ANALYZED));

		// See class JavaDoc for details on this field.
		doc.add(new Field("readerclass", getReaderClass(), Field.Store.YES, Field.Index.NOT_ANALYZED));

		return doc;
	}


	/**
	 *  Adds the given String to a text field referenced in the index by the field name 'default'. The default
	 *  field may be used in queries to quickly search for text across fields. This method should be called from
	 *  the addCustomFields of implementing classes.
	 *
	 * @param  value               A text string to be added to the indexed field named 'default.'
	 * @param  adminDefaultBuffer  The feature to be added to the ToDefaultField attribute
	 */
	protected final void addToAdminDefaultField(String value, StringBuffer adminDefaultBuffer) {
		adminDefaultBuffer.append(value).append(' ');
	}


	/**
	 *  Tokenizes a URI by replacing the chars /, ?, &, :, = and . with a blank space.
	 *
	 * @param  uri  A URL or URI
	 * @return      The tokenized URI
	 */
	private final String tokenizeURI(String uri) {
		return uri.replaceAll("/| |\\?|=|\\.|\\&|:", " ");
	}

}

