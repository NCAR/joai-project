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
package org.dlese.dpc.index.reader;

import org.apache.lucene.document.*;
import org.dlese.dpc.index.writer.*;
import org.dlese.dpc.index.*;
import org.dlese.dpc.xml.*;
import org.dlese.dpc.webapps.tools.*;
import org.dlese.dpc.util.*;
import org.dlese.dpc.index.document.DateFieldTools;

import javax.servlet.*;
import java.io.*;
import java.text.*;
import java.util.*;
import java.net.InetAddress;

/**
 *  A bean for accessing the data stored in a Lucene {@link org.apache.lucene.document.Document} that logs a
 *  single web request. The index writer that is responsible for creating this type of Lucene {@link
 *  org.apache.lucene.document.Document} is a {@link org.dlese.dpc.index.writer.WebLogWriter}.
 *
 * @author     John Weatherley
 * @see        org.dlese.dpc.index.writer.WebLogWriter
 */
public class WebLogReader extends DocReader {
	private final static String DEFAULT = "(null)";


	/**  Init method does nothing. */
	public void init() { }

	// ----------------- Data access methods ----------------------

	/**
	 *  Gets the requestDate attribute of the WebLogReader object
	 *
	 * @return    The requestDate value
	 */
	public String getRequestDate() {
		String t = doc.get("requestdate");

		if (t == null)
			return DEFAULT;
		long time = -1;
		try {
			time = DateFieldTools.stringToTime(t);
		} catch (ParseException pe) {
			prtlnErr("Error in getRequestDate(): " + pe);
		}			

		return new SimpleDateFormat("EEE MMM d, yyyy h:mm:ss a zzz").format(new Date(time));
	}


	/**
	 *  Gets the requestUrl attribute of the WebLogReader object
	 *
	 * @return    The requestUrl value
	 */
	public String getRequestUrl() {
		String t = doc.get("requesturl");

		if (t == null)
			return DEFAULT;
		return t;
	}


	/**
	 *  Gets the notes attribute of the WebLogReader object
	 *
	 * @return    The notes value
	 */
	public String getNotes() {
		String t = doc.get("notes");

		if (t == null)
			return "";
		return t;
	}


	/**
	 *  Gets the IP address of the requesting remote host.
	 *
	 * @return    The remote host IP, for example 128.123.123.123
	 */
	public String getRemoteHost() {
		String t = doc.get("remotehost");

		if (t == null)
			return DEFAULT;
		return t;
	}


	/**
	 *  Gets the fully qualified domain name the requesting IP. Best effort method, meaning we may not be able to
	 *  return the FQDN depending on the underlying system configuration. If not avaialable, the the IP address
	 *  is returned instead.
	 *
	 * @return    The remote host domain name, for example mysite.org, or IP address if not available
	 */
	public String getRemoteHostName() {
		String t = doc.get("remotehost");

		if (t == null || t.trim().length() == 0)
			return null;
		try {
			InetAddress address = InetAddress.getByName(t);
			return address.getCanonicalHostName();
		} catch (Throwable e) {
			return t;
		}
	}

	// --------------- Set up methods ------------------------

	/**  Constructor for the WebLogReader object */
	public WebLogReader() { }


	/**
	 *  Constructor that may be used programatically to wrap a reader around a Lucene {@link
	 *  org.apache.lucene.document.Document} created by a {@link org.dlese.dpc.index.writer.DocWriter}. Sets the
	 *  score to 0.
	 *
	 * @param  doc  A Lucene {@link org.apache.lucene.document.Document} created by a {@link
	 *      org.dlese.dpc.index.writer.DocWriter}.
	 */
	public WebLogReader(Document doc) {
		super(doc);
	}



	/**
	 *  Gets a String describing the reader type. This may be used in (Struts) beans to determine which type of
	 *  reader is available for a given search result and thus what data is available for display in the UI. The
	 *  reader type implies which getter methods are available.
	 *
	 * @return    The readerType value.
	 */
	public String getReaderType() {
		return "WebLogReader";
	}
	
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
	private final void prtlnErr(String s) {
		System.err.println(getDateStamp() + " WebLogReader error: " + s);
	}	

}


