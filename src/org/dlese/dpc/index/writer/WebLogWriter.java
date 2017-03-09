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
import org.dlese.dpc.index.document.DateFieldTools;

/**
 *  Writes a Lucene {@link org.apache.lucene.document.Document} that holds
 *  information about a client request to an HTTP servlet. <br>
 *  The Lucene {@link org.apache.lucene.document.Document} fields that are
 *  created by this class are: <br>
 *  <br>
 *  <code><b>requestdate</b> </code> - The date of the client request. Stored.
 *  <br>
 *  <code><b>requesturl</b> </code> - The URL and query the client requested.
 *  Stored.<br>
 *  <code><b>remotehost</b> </code> - The requesting client's host name or IP
 *  address. Stored.<br>
 *  <code><b>notes</b> </code> - Free text notes related to this log entry.
 *  Stored.<br>
 *  <code><b>doctype</b> </code> - The document format type, which is 'weblog,'
 *  with '0' appended to support wildcard searching.<br>
 *  <code><b>readerclass</b> </code> - The class which is used to read {@link
 *  org.apache.lucene.document.Document}s created by this writer, which is
 *  'WebLogReader'.<br>
 *  <code><b>admindefaultfield</b> </code> - The default field that holds all content for searching.
 *  'WebLogReader'.<br> 
 *  <br>
 *
 *
 * @author    John Weatherley
 * @see       org.dlese.dpc.index.reader.WebLogReader
 */
public class WebLogWriter implements DocWriter {

	/**
	 *  Gets doctype, which is "weblog".
	 *
	 * @return    The String "weblog".
	 */
	public String getDocType() {
		return "weblog";
	}


	/**
	 *  Gets the name of the concrete {@link org.dlese.dpc.index.reader.DocReader}
	 *  class that is used to read this type of {@link
	 *  org.apache.lucene.document.Document}, which is "WebLogReader".
	 *
	 * @return    The String "WebLogReader".
	 */
	public String getReaderClass() {
		return "org.dlese.dpc.index.reader.WebLogReader";
	}



	/**  Constructor for the WebLogWriter object */
	public WebLogWriter() { }


	/**
	 *  Create a log entry Document.
	 *
	 * @param  request  The HTTP request.
	 * @return          A Document for indexing the request.
	 */
	public final Document log(HttpServletRequest request) {
		return log(request, null);
	}


	/**
	 *  Create a log entry Document, storing and indexing the given notes.
	 *
	 * @param  request  The HTTP request.
	 * @param  notes    Notes about this web log entry.
	 * @return          A Document for indexing the request.
	 */
	public final Document log(HttpServletRequest request, String notes) {
		Document doc = new Document();
		StringBuffer adminDefaultBuffer = new StringBuffer();

		// ------------ Log fields -----------------

		long curTime = System.currentTimeMillis();
		doc.add(new Field("requestdate", DateFieldTools.timeToString(curTime), Field.Store.YES, Field.Index.ANALYZED));
		addToAdminDefaultField(
			new SimpleDateFormat("EEE MMM d, yyyy h:mm:ss a zzz").format(new Date(curTime)), adminDefaultBuffer);
		
		String requestString = request.getRequestURL() +
			(request.getQueryString() == null ? "" : "/" + request.getQueryString());

		doc.add(new Field("requesturl", requestString, Field.Store.YES, Field.Index.NOT_ANALYZED));
		doc.add(new Field("requesturlt", tokenizeURI(requestString), Field.Store.NO, Field.Index.ANALYZED ));
		addToAdminDefaultField(tokenizeURI(requestString).toString(), adminDefaultBuffer);

		String remoteHost = request.getRemoteHost();
		if (remoteHost == null)
			remoteHost = request.getRemoteAddr();
		doc.add(new Field("remotehost", remoteHost, Field.Store.YES, Field.Index.NOT_ANALYZED));
		doc.add(new Field("remotehostt", tokenizeURI(remoteHost), Field.Store.NO, Field.Index.ANALYZED ));
		addToAdminDefaultField(tokenizeURI(remoteHost).toString(), adminDefaultBuffer);

		if (notes != null && notes.length() > 0) {
			doc.add(new Field("notes", notes, Field.Store.YES, Field.Index.ANALYZED));
			addToAdminDefaultField(notes, adminDefaultBuffer);
		}

		// Add the default field:
		doc.add(new Field("admindefault", adminDefaultBuffer.toString(), Field.Store.NO, Field.Index.ANALYZED ));

		// ------------ DocWriter fields -----------------

		// See class JavaDoc for details on this field.
		doc.add(new Field("doctype", '0' + getDocType(), Field.Store.YES, Field.Index.NOT_ANALYZED));

		// See class JavaDoc for details on this field.
		doc.add(new Field("readerclass", getReaderClass(), Field.Store.YES, Field.Index.NOT_ANALYZED));

		return doc;
	}


	/**
	 *  Adds the given String to a text field referenced in the index by the field
	 *  name 'default'. The default field may be used in queries to quickly search
	 *  for text across fields. This method should be called from the
	 *  addCustomFields of implementing classes.
	 *
	 * @param  value          A text string to be added to the indexed field named
	 *      'default.'
	 * @param  adminDefaultBuffer  The feature to be added to the ToDefaultField
	 *      attribute
	 */
	protected final void addToAdminDefaultField(String value, StringBuffer adminDefaultBuffer) {
		adminDefaultBuffer.append(value).append(' ');
	}


	/**
	 *  Tokenizes a URI by replacing the chars /, ?, &, :, = and . with a blank
	 *  space.
	 *
	 * @param  uri  A URL or URI
	 * @return      The tokenized URI
	 */
	private final String tokenizeURI(String uri) {
		return uri.replaceAll("/| |\\?|=|\\.|\\&|:", " ");
	}

}

