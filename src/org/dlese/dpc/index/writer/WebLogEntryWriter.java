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
import org.apache.lucene.document.*;
import javax.servlet.http.*;
import java.util.regex.*;
import org.dlese.dpc.index.document.DateFieldTools;

import org.dom4j.Element;
import org.dom4j.Attribute;
import org.dom4j.Node;

/**
 *  Writes a Lucene {@link org.apache.lucene.document.Document} for data in a single web
 *  log entry. Uderstands a log file in the format of the 'Combined Log Format' extension
 *  of the Common Log Format (CLF) with additional extensions added by the DLESE query
 *  logger. See the <a href="http://httpd.apache.org/docs/logs.html" target="_blank">
 *  Apache logs docs</a> for info about the Combined Log Format, {@link
 *  org.dlese.dpc.logging.ClfLogger} and {@link org.dlese.dpc.dds.action.DDSQueryAction}
 *  method logQuery() for info about the DLESE query log extensions.
 *
 * @author    John Weatherley
 */
public class WebLogEntryWriter implements DocWriter {
	private static boolean debug = true;

	private static String NS = "webLogEntry";


	/**
	 *  Gets doctype, which is "weblog".
	 *
	 * @return    The String "weblog".
	 */
	public String getDocType() {
		return "webLogEntry";
	}


	/**
	 *  Gets the name of the concrete {@link org.dlese.dpc.index.reader.DocReader} class that
	 *  is used to read this type of {@link org.apache.lucene.document.Document}, which is
	 *  "WebLogEntryReader".
	 *
	 * @return    The String "WebLogEntryReader".
	 */
	public String getReaderClass() {
		return "WebLogEntryReader";
	}



	/**  Constructor for the WebLogEntryWriter object */
	public WebLogEntryWriter() { }



	/**
	 *  Create a log entry Document, storing and indexing the given notes.
	 *
	 * @param  logEntry  A single logEntry line
	 * @return           A Document for indexing the request.
	 */
	public final Document createLogEntryDoc(String logEntry) {
		Document doc = new Document();

		List logEntryRows = getEntryRows(logEntry);

		if (logEntryRows == null || logEntryRows.size() == 0)
			return null;

		StringBuffer defaultFieldStringBuffer = new StringBuffer();

		String val;
		// ------------ Log fields -----------------
		doc.add(new Field(NS + "FullLogEntry", logEntry, Field.Store.YES, Field.Index.NO));

		// Client IP:
		val = (String) logEntryRows.get(0);
		doc.add(new Field(NS + "ClientIp", val, Field.Store.YES, Field.Index.NOT_ANALYZED));
		doc.add(new Field(NS + "ClientIpEnc", SimpleLuceneIndex.encodeToTerm(val), Field.Store.YES, Field.Index.ANALYZED));
		doc.add(new Field(NS + "ClientIpTokens", this.tokenizeString(val), Field.Store.YES, Field.Index.ANALYZED));

		// Request date:
		val = (String) logEntryRows.get(3);
		val = val.replaceAll("\\[|\\]", ""); // Remove the brackets
		prtln("Parsing date: " + val);
		Date date = null;
		try {
			date = Utils.convertStringToDate(val, "dd/MMM/yyyy:HH:mm:ss Z");
		} catch (Throwable pe) {
			prtlnErr("Unable to parse date: " + pe);
		}
		prtln("Date is: " + date);
		if (date != null) {
			doc.add(new Field(NS + "RequestDate", DateFieldTools.dateToString(date), Field.Store.YES, Field.Index.ANALYZED));
			try{
				doc.add(new Field(NS + "RequestDateMonth", Utils.convertDateToString(date,"MMM"), Field.Store.YES, Field.Index.ANALYZED));
				doc.add(new Field(NS + "RequestDateYear", Utils.convertDateToString(date,"yyyy"), Field.Store.YES, Field.Index.ANALYZED));
			} catch (Throwable pe) {
				prtlnErr("Unable to output date: " + pe);
			}					
		}

		// Request string:
		val = (String) logEntryRows.get(4);
		doc.add(new Field(NS + "RequestString", removeHexChars(val), Field.Store.YES, Field.Index.ANALYZED));
		doc.add(new Field(NS + "RequestStringTokens", tokenizeString(removeHexChars(val)), Field.Store.YES, Field.Index.ANALYZED));

		// HTTP status code:
		val = (String) logEntryRows.get(5);
		doc.add(new Field(NS + "HttpStatus", val, Field.Store.YES, Field.Index.ANALYZED));

		// Response size:
		val = (String) logEntryRows.get(6);
		if(!val.equals("-1"))
			doc.add(new Field(NS + "ResponseSize", val, Field.Store.YES, Field.Index.ANALYZED));

		// Referer:
		val = (String) logEntryRows.get(7);
		val = val.replaceAll("\"", ""); // Remove the quotes
		doc.add(new Field(NS + "Referer", removeHexChars(val), Field.Store.YES, Field.Index.ANALYZED));
		doc.add(new Field(NS + "RefererTokens", tokenizeString(removeHexChars(val)), Field.Store.YES, Field.Index.ANALYZED));

		// User agent (browser, crawler, etc):
		val = (String) logEntryRows.get(8);
		val = val.replaceAll("\"", ""); // Remove the quotes
		doc.add(new Field(NS + "UserAgent", removeHexChars(val), Field.Store.YES, Field.Index.ANALYZED));
		doc.add(new Field(NS + "UserAgentTokens", tokenizeString(removeHexChars(val)), Field.Store.YES, Field.Index.ANALYZED));		

		// Name of server that recieved the request:
		val = (String) logEntryRows.get(9);
		doc.add(new Field(NS + "ServerName", val, Field.Store.YES, Field.Index.ANALYZED));

		// Server port that recieved the request:
		val = (String) logEntryRows.get(10);
		doc.add(new Field(NS + "ServerPort", val, Field.Store.YES, Field.Index.ANALYZED));

		// Type of request ("search" "hist"):
		val = (String) logEntryRows.get(11);
		doc.add(new Field(NS + "RequestType", val, Field.Store.YES, Field.Index.ANALYZED));		

		// Number of search results:
		val = (String) logEntryRows.get(12);
		doc.add(new Field(NS + "NumResults", val, Field.Store.YES, Field.Index.ANALYZED));	

		// Total number of records in the library:
		val = (String) logEntryRows.get(13);
		doc.add(new Field(NS + "TotalNumLibraryRecords", val, Field.Store.YES, Field.Index.ANALYZED));		

		// Total number of records in the library:
		val = (String) logEntryRows.get(13);
		doc.add(new Field(NS + "TotalNumLibraryRecords", val, Field.Store.YES, Field.Index.ANALYZED));

		// Results page:
		val = (String) logEntryRows.get(14);
		doc.add(new Field(NS + "PageStartIndex", val, Field.Store.YES, Field.Index.ANALYZED));			

		// DLESE ID:
		val = (String) logEntryRows.get(15);
		val = val.replaceAll("\"", ""); // Remove the quotes
		if(!val.equals("-")){
			doc.add(new Field(NS + "DleseId", val, Field.Store.YES, Field.Index.NOT_ANALYZED));
			doc.add(new Field(NS + "DleseIdEnc", SimpleLuceneIndex.encodeToTerm(val), Field.Store.YES, Field.Index.NOT_ANALYZED));		
			doc.add(new Field(NS + "DleseIdTokens", tokenizeString(val), Field.Store.YES, Field.Index.ANALYZED));		
		}
		
		// Index the query log entry XML
		indexQueryLogEntries(doc,(String)logEntryRows.get(16)); 
		
		// The full log entry:
		doc.add(new Field(NS + "FullLogEntry", removeHexChars(logEntry), Field.Store.YES, Field.Index.NO));
		doc.add(new Field(NS + "FullLogEntryTokens", tokenizeString(removeHexChars(logEntry)), Field.Store.NO, Field.Index.ANALYZED));

		
		// Add the default field:
		doc.add(new Field("default", defaultFieldStringBuffer.toString(), Field.Store.NO, Field.Index.ANALYZED));

		// ------------ DocWriter fields -----------------

		// See class JavaDoc for details on this field.
		doc.add(new Field("doctype", '0' + getDocType(), Field.Store.YES, Field.Index.NOT_ANALYZED));

		// See class JavaDoc for details on this field.
		doc.add(new Field("readerclass", getReaderClass(), Field.Store.YES, Field.Index.ANALYZED));

		return doc;
	}
	
	private void indexQueryLogEntries(Document doc, String queryLogXml){
		if(queryLogXml == null)
			return;
		queryLogXml = queryLogXml.replaceAll("\"", ""); // Remove the quotes
		queryLogXml = queryLogXml.replaceAll("%22", "&quot;"); // Replace the hex encoded quotes with XML entity references
		queryLogXml = queryLogXml.replaceAll("\\\\\'", "'"); // Replace escaped single quotes with single quotes

		queryLogXml = removeHexChars(queryLogXml); // Remove any other hex chars...
		prtln("\n\nqueryLogXml: " + queryLogXml);
		
		org.dom4j.Document xmlDoc = null;
		try{
			xmlDoc = Dom4jUtils.getXmlDocument(queryLogXml);
		}catch(Throwable t){
			prtlnErr("Unable to parse query XML: " + t);
			return;
		}
		
		String val = null;
		
		// The query string entered by the user:
		val = xmlDoc.valueOf("/qle/q");
		if(val != null && val.trim().length() > 0){
			doc.add(new Field(NS + "QueryString", val, Field.Store.YES, Field.Index.ANALYZED));
			doc.add(new Field(NS + "QueryStringKeyword", val, Field.Store.YES, Field.Index.NOT_ANALYZED));
		}

		// The query type (search, hist, web wervice, etc):
		val = xmlDoc.valueOf("/qle/meta/type");
		if(val != null && val.trim().length() > 0){
			doc.add(new Field(NS + "QueryType", val, Field.Store.YES, Field.Index.ANALYZED));
		}		

		// The user session:
		val = xmlDoc.valueOf("/qle/meta/session");
		if(val != null && val.trim().length() > 0){
			doc.add(new Field(NS + "Session", val, Field.Store.YES, Field.Index.ANALYZED));
		}	

		// The Web service client:
		val = xmlDoc.valueOf("/qle/meta/client");
		if(val != null && val.trim().length() > 0){
			doc.add(new Field(NS + "WebServiceClient", val, Field.Store.YES, Field.Index.ANALYZED));
			doc.add(new Field(NS + "WebServiceClientEnc", SimpleLuceneIndex.encodeToTerm(val), Field.Store.YES, Field.Index.ANALYZED));
		}

		// The Web service XML format:
		val = xmlDoc.valueOf("/qle/meta/xmlFormat");
		if(val != null && val.trim().length() > 0){
			doc.add(new Field(NS + "WebServiceXmlFormat", val, Field.Store.YES, Field.Index.ANALYZED));
			doc.add(new Field(NS + "WebServiceXmlFormatEnc", SimpleLuceneIndex.encodeToTerm(val), Field.Store.YES, Field.Index.ANALYZED));			
		}
		
		// The gr value:
		val = xmlDoc.valueOf("/qle/meta/gr");
		if(val != null && val.trim().length() > 0){
			doc.add(new Field(NS + "Gr", tokenizeString(val), Field.Store.YES, Field.Index.ANALYZED));
		}		

		// The re value:
		val = xmlDoc.valueOf("/qle/meta/re");
		if(val != null && val.trim().length() > 0){
			doc.add(new Field(NS + "Re", tokenizeString(val), Field.Store.YES, Field.Index.ANALYZED));
		}

		// The su value:
		val = xmlDoc.valueOf("/qle/meta/su");
		if(val != null && val.trim().length() > 0){
			doc.add(new Field(NS + "Su", tokenizeString(val), Field.Store.YES, Field.Index.ANALYZED));
		}

		// The cs value:
		val = xmlDoc.valueOf("/qle/meta/cs");
		if(val != null && val.trim().length() > 0){
			doc.add(new Field(NS + "Cs", tokenizeString(val), Field.Store.YES, Field.Index.ANALYZED));
		}

		// The ky value:
		val = xmlDoc.valueOf("/qle/meta/ky");
		if(val != null && val.trim().length() > 0){
			doc.add(new Field(NS + "Ky", tokenizeString(val), Field.Store.YES, Field.Index.ANALYZED));
		}
		
	}

	private final List getEntryRows(String logEntry) {
		if (logEntry == null || logEntry.trim().length() == 0)
			return null;

		List rows = new ArrayList(20);

		try {
			/*
				First, replace quotes that are escaped with a forward slash ( \" )
				and replace them with the http hex encoding %22
			*/
			logEntry = logEntry.replaceAll("\\\\\"", "%22");
			System.out.println("\n\nlogEntry: " + logEntry + "\n");

			/*
				Parse the log entry into it's separate parts...
				Three clauses:
					1. Grab the Strings in quotes "" or
					2. Grab the Strings in brackets [] or
					3. Grab the Strings separated by a space (NOT space)
			*/
			Pattern p = Pattern.compile("(\"[^\"]*\"|\\[[^\\[\\]]*\\]|[^ ]+)");

			Matcher m = p.matcher(logEntry);
			StringBuffer sb = new StringBuffer();
			String match;
			while (m.find()) {
				match = m.group(1);
				if (match != null && match.length() > 0) {
					rows.add(match);
				}
				prtln("match: '" + match + "'");
			}
			prtln("match count: " + rows.size());
		} catch (Throwable e) {
			prtlnErr("Could not parse log entry: " + e);
		}
		return rows;
	}


	/**
	 *  Adds the given String to a text field referenced in the index by the field name
	 *  'default'. The default field may be used in queries to quickly search for text across
	 *  fields. This method should be called from the addCustomFields of implementing
	 *  classes.
	 *
	 * @param  value                     A text string to be added to the indexed field named
	 *      'default.'
	 * @param  defaultFieldStringBuffer  The feature to be added to the ToDefaultField
	 *      attribute
	 */
	protected final void addToAdminDefaultField(String value, StringBuffer defaultFieldStringBuffer) {
		defaultFieldStringBuffer.append(value).append(" " + IndexingTools.PHRASE_SEPARATOR);
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


	/**
	 *  Tokenizes a String by removing all the non-letter/number chars.
	 *
	 * @param  string  A String
	 * @return         The tokenized String
	 */
	public final static String tokenizeString(String string) {
		return string.replaceAll("[^a-zA-Z0-9]", " ");
	}


	/**
	 *  Unencodes chars that have been encoded into hex. These include the space ' ' %20, and
	 *  quote '"' %22.
	 *
	 * @param  string  A String
	 * @return         The clean String
	 */
	public final static String removeHexChars(String string) {
		string = string.replaceAll("%22", "\"");
		return string.replaceAll("%20", " ");
	}

	// ---------------- Debugging/utility methods	-----------------------

	/**
	 *  Return a string for the current time and date, sutiable for display in log files and
	 *  output to standout:
	 *
	 * @return    The dateStamp value
	 */
	private final String getDateStamp() {
		return
			new SimpleDateFormat("MMM d, yyyy h:mm:ss a zzz").format(new Date());
	}


	/**
	 *  Output a line of text to error out, with datestamp.
	 *
	 * @param  s  The text that will be output to error out.
	 */
	protected final void prtlnErr(String s) {
		System.err.println(getDateStamp() + " Error: " + s);
	}



	/**
	 *  Output a line of text to standard out, with datestamp, if debug is set to true.
	 *
	 * @param  s  The String that will be output.
	 */
	protected final void prtln(String s) {
		if (debug)
			System.out.println(getDateStamp() + " " + s);
	}


	/**
	 *  Sets the debug attribute of the FileIndexingServiceWriter object
	 *
	 * @param  db  The new debug value
	 */
	public final static void setDebug(boolean db) {
		debug = db;
	}
}

