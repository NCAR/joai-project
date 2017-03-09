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
import java.net.URLEncoder;

/**
 *  An abstract bean for accessing the data stored in a Lucene {@link org.apache.lucene.document.Document}
 *  that was created by a {@link org.dlese.dpc.index.writer.FileIndexingServiceWriter}. This class may be
 *  extended for each {@link org.apache.lucene.document.Document} type that might be returned in a search.
 *
 * @author     John Weatherley
 */
public abstract class FileIndexingServiceDocReader extends DocReader implements Serializable {
	private static boolean debug = true;
	private final String DEFAULT = "(null)";

	/**  The File handle for the underlying file. */
	private File myFile = null;



	/**
	 *  Constructor that may be used programatically to wrap a reader around a Lucene {@link
	 *  org.apache.lucene.document.Document} that was created by a {@link org.dlese.dpc.index.writer.DocWriter}.
	 *
	 * @param  doc  A Lucene {@link org.apache.lucene.document.Document}.
	 * @see         org.dlese.dpc.index.writer.DocWriter
	 */
	protected FileIndexingServiceDocReader(Document doc) {
		super(doc);
	}


	/**  Constructor that initializes an empty DocReader. */
	protected FileIndexingServiceDocReader() {
		super();
	}

	// ----------------- Concrete methods ---------------------

	/**
	 *  Gets the full content of the file that was used to index the {@link org.apache.lucene.document.Document}.
	 *  This includes all XML or HTML tags, etc.
	 *
	 * @return    The full content as text, or empty string if unable to process.
	 */
	public final String getFullContent() {
		try {
			String content = doc.get("filecontent");
			if(content == null)
				content = Files.readFile(getFile()).toString();
			return content;	
		} catch (Exception e) {
			prtlnErr("Error reading file ( FileIndexingServiceDocReader.getFullContent() )" + e);
			return "";
		}
	}


	/**
	 *  Gets the full content of the file that was used to index the {@link org.apache.lucene.document.Document},
	 *  returned in the given character encoding, for example UTF-8.
	 *
	 * @param  characterEncoding  The character encoding to return, for example 'UTF-8'
	 * @return                    The full content as text, or empty string if unable to process.
	 */
	public final String getFullContentEncodedAs(String characterEncoding) {
		try {
			String content = doc.get("filecontent");
			if(content == null)
				content = Files.readFileToEncoding(getFile(),characterEncoding).toString();
			return content;			
		} catch (Exception e) {
			prtlnErr("Error reading file ( FileIndexingServiceDocReader.getFullContentEncodedAs() )" + e);
			return "";
		}
	}


	/**
	 *  Gets doctype associated with the {@link org.apache.lucene.document.Document}, for example 'dlese_ims,'
	 *  'adn,' or 'html'. Note that to support wildcard searching, the doctype is indexed with a leading '0'
	 *  appened to the beginning. This method strips the leading zero prior to returning.
	 *
	 * @return    The doctype value.
	 */
	public String getDoctype() {
		String doctype = doc.get("doctype");
		if (doctype == null)
			return "";
		else
			// remove the '0'
			return doctype.substring(1, doctype.length());
	}


	/**
	 *  Determine whether the status of this {@link org.apache.lucene.document.Document} is deleted, indicated by
	 *  a return value of "true". This does not necessarily mean the file has been deleted.
	 *
	 * @return    The String "true" if the status is deleted, else "false".
	 */
	public String getDeleted() {
		if (isDeleted())
			return "true";
		else
			return "false";
	}


	/**
	 *  Determine whether the status of this {@link org.apache.lucene.document.Document} is deleted. This does
	 *  not necessarily mean the file has been deleted. <p>
	 *
	 *  Field: status [true]
	 *
	 * @return    True if the status is deleted.
	 */
	public boolean isDeleted() {
		String deleted = doc.get("deleted");
		if (deleted == null || !deleted.equals("true"))
			return false;
		else
			return true;
	}



	/**
	 *  Determine whether the file associated with this {@link org.apache.lucene.document.Document} exists,
	 *  indicated by a return value of "true".
	 *
	 * @return    The String "true" if the file exists, else "false".
	 */
	public String getFileExists() {
		if (fileExists())
			return "true";
		else
			return "false";
	}


	/**
	 *  Determine whether the file associated with this {@link org.apache.lucene.document.Document} exists.
	 *
	 * @return    True if the file exists, else false.
	 */
	public boolean fileExists() {
		return getFile().exists();
	}


	/**
	 *  Gets the date and time this record was indexed, as a String.
	 *
	 * @return    The date and time this record was indexed
	 */
	public String getDateFileWasIndexedString() {
		String t = doc.get("fileindexeddate");

		if (t == null)
			return DEFAULT;
		long modTime = -1;
		try{
			modTime = DateFieldTools.stringToTime(t);
		} catch (ParseException pe) {
			prtlnErr("Error in getDateFileWasIndexedString(): " + pe); 
		}
		return new SimpleDateFormat("MMM d, yyyy h:mm:ss a zzz").format(new Date(modTime));
	}


	/**
	 *  Gets the date this record was indexed.
	 *
	 * @return    The date this record was indexed
	 */
	public Date getDateFileWasIndexed() {
		String t = doc.get("fileindexeddate");

		if (t == null)
			return null;
		
		try {
			return DateFieldTools.stringToDate(t);
		} catch (ParseException pe) {
			prtlnErr("Error in getDateFileWasIndexed(): " + pe);
			return new Date(0);
		}			
		
	}


	/**
	 *  Gets a String representataion of the File modification time of the File used to index the {@link
	 *  org.apache.lucene.document.Document}. Note that while this represents the File modification time, this
	 *  date stamp does not get updated until the File is re-indexed by the indexer.
	 *
	 * @return    The File modification time.
	 */
	public String getLastModifiedString() {
		String t = doc.get("modtime");

		if (t == null)
			return DEFAULT;
		try{
			return new SimpleDateFormat("MMM d, yyyy h:mm:ss a zzz").format(DateFieldTools.stringToDate(t));
		} catch (ParseException pe) {
			prtlnErr("Error in getLastModifiedString(): " + pe);
			return new SimpleDateFormat("MMM d, yyyy h:mm:ss a zzz").format(new Date(0));
		}
	}


	/**
	 *  Gets the file modification date in UTC format for the given record.
	 *
	 * @return    The file modification date value.
	 */
	public String getLastModifiedAsUTC() {
		String t = doc.get("modtime");

		if (t == null)
			return DEFAULT;

		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
		df.setTimeZone(new SimpleTimeZone(SimpleTimeZone.UTC_TIME, "UTC"));
		try {
			return df.format(DateFieldTools.stringToDate(t));
		} catch (ParseException pe) {
			prtlnErr("Error in getLastModifiedAsUTC(): " + pe);
			return df.format(new Date(0));
		}			
	}


	/**
	 *  Gets the File modification time of the File used to index the {@link
	 *  org.apache.lucene.document.Document}. Note that while this represents the File modification time, this
	 *  date stamp does not get updated until the File is re-indexed by the indexer.
	 *
	 * @return    The File modification time.
	 */
	public long getLastModified() {
		String t = doc.get("modtime");

		if (t == null)
			return 0;
		
		try {
			return DateFieldTools.stringToTime(t);
		} catch (ParseException pe) {
			prtlnErr("Error in getLastModified(): " + pe);
			return -1;
		}			
	}


	/**
	 *  Gets the File that was used to index the {@link org.apache.lucene.document.Document}.
	 *
	 * @return    The source File.
	 */
	public File getFile() {
		if (myFile == null)
			myFile = new File(getDocsource());
		return myFile;
	}


	/**
	 *  Gets the name of the File that was used to index the {@link org.apache.lucene.document.Document}.
	 *
	 * @return    The source File name.
	 */
	public String getFileName() {
		String t = doc.get("filename");

		if (t == null)
			return DEFAULT;
		else
			return t;
	}


	/**
	 *  Gets the absolute path of the file that was used to index the {@link
	 *  org.apache.lucene.document.Document}.
	 *
	 * @return    The absolute path the the underlying file.
	 */
	public String getDocsource() {
		String t = doc.get("docsource");

		if (t == null)
			return DEFAULT;
		else
			return t;
	}


	/**
	 *  Gets the absolute path of the file that was used to index the {@link
	 *  org.apache.lucene.document.Document}, encoded.
	 *
	 * @return    The absolute path the the underlying file.
	 */
	public String getDocsourceEncoded() {
		String t = doc.get("docsource");

		if (t == null)
			return DEFAULT;

		try {
			return URLEncoder.encode(t, "utf-8");
		} catch (Exception e) {
			return t;
		}
	}


	/**
	 *  Gets the absolute path of the directory that contained the File used to index the {@link
	 *  org.apache.lucene.document.Document}.
	 *
	 * @return    The docDir value.
	 */
	public String getDocDir() {
		String t = doc.get("docdir");

		if (t == null)
			return DEFAULT;
		else
			return t;
	}



	// -----------------------------------------------------------------------------

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
	 *  Sets the debug attribute.
	 *
	 * @param  db  The new debug value
	 */
	protected final static void setDebug(boolean db) {
		debug = db;
	}


	/**
	 *  Output a line of text to error out, with datestamp.
	 *
	 * @param  s  The text that will be output to error out.
	 */
	protected static void prtlnErr(String s) {
		System.err.println(getDateStamp() + " " + s);
	}



	/**
	 *  Output a line of text to standard out, with datestamp, if debug is set to true.
	 *
	 * @param  s  The String that will be output.
	 */
	protected static void prtln(String s) {
		if (debug)
			System.out.println(getDateStamp() + " " + s);
	}
}


