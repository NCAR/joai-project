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

import javax.servlet.*;
import java.io.*;
import java.text.*;
import java.util.*;

/**
 *  A bean for accessing the data stored in a Lucene {@link org.apache.lucene.document.Document} that was
 *  indexed by a {@link org.dlese.dpc.index.writer.ErrorFileIndexingWriter}, which occurs when there is an
 *  error while indexing a given file.
 *
 * @author     John Weatherley
 * @see        org.dlese.dpc.index.writer.ErrorFileIndexingWriter
 */
public class ErrorDocReader extends FileIndexingServiceDocReader {

	/**  Constructor for the ErrorDocReader object */
	public ErrorDocReader() { }


	/**  Init method does nothing. */
	public void init() { }


	/**
	 *  Constructor that may be used programatically to wrap a reader around a Lucene {@link
	 *  org.apache.lucene.document.Document} created by a {@link org.dlese.dpc.index.writer.FileIndexingServiceWriter}.
	 *
	 * @param  doc  A Lucene {@link org.apache.lucene.document.Document} created by a {@link
	 *      org.dlese.dpc.index.writer.ItemFileIndexingWriter}.
	 */
	public ErrorDocReader(Document doc) {
		super(doc);
	}


	/**
	 *  Gets the String 'ErrorDocReader,' which is the key that describes this reader type. This may be used in
	 *  (Struts) beans to determine which type of reader is available for a given search result and thus what
	 *  data is available for display in the UI. The reader type determines which getter methods are available.
	 *
	 * @return    The String 'ErrorDocReader'.
	 */
	public String getReaderType() {
		return "ErrorDocReader";
	}


	/**
	 *  Gets the error message.
	 *
	 * @return    The error message.
	 */
	public String getErrorMsg() {
		String t = doc.get("errormsg");

		if (t == null)
			return "";
		else
			return t;
	}


	/**
	 *  Gets the name of the Exception that was thrown to when the error occured.
	 *
	 * @return    The Exception name.
	 */
	public String getExceptionName() {
		String t = doc.get("exception");

		if (t == null)
			return "";
		else
			return t;
	}

	
	/**
	 *  Gets the stack trace that was indicated to when the error occured.
	 *
	 * @return    The stack trace as a flat String.
	 */
	public String getStackTrace() {
		String t = doc.get("stacktrace");

		if (t == null)
			return "";
		else
			return t;
	}	


	/**
	 *  Gets the errorDocType attribute, which defaults to 'generic'.
	 *
	 * @return    The errorDocType value
	 */
	public String getErrorDocType() {
		String value = doc.get("errordoctype");
		if(value == null || value.length() == 0)
			return "generic";
		else 
			return value;
	}

	
	// ---------------- Fields available for ErrorDocType = 'dupIdError': ------------------

	/**
	 *  Gets the duplicateId attribute of the ErrorDocReader object
	 *
	 * @return    The duplicateId value
	 */
	public String getDuplicateId() {
		return doc.get("duplicateIdValue");
	}


	/**
	 *  Gets the duplicateIdSourceFilePath attribute of the ErrorDocReader object
	 *
	 * @return    The duplicateIdSourceFilePath value
	 */
	public String getDuplicateIdSourceFilePath() {
		return doc.get("duplicateIdDocsource");
	}


	/**
	 *  Gets the duplicateIdSourceFile attribute of the ErrorDocReader object
	 *
	 * @return    The duplicateIdSourceFile value
	 */
	public File getDuplicateIdSourceFile() {
		String path = getDuplicateIdSourceFilePath();
		if (path != null)
			return new File(path);
		return null;
	}

}


