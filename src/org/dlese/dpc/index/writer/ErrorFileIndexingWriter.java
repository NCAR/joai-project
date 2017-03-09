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

/**
 *  Writes a Lucene {@link org.apache.lucene.document.Document} that represents an error that has occured in
 *  in indexing a File. This writer is used by {@link org.dlese.dpc.index.FileIndexingService}. The {@link
 *  org.dlese.dpc.index.writer.FileIndexingServiceWriter} may throw a {@link ErrorDocException}, which can be
 *  used to specific specific fields to index with the exception.<p>
 *
 *  <br>
 *  <b>The Lucene {@link org.apache.lucene.document.Document} fields that are created by this class are (in
 *  addition the the ones listed for {@link org.dlese.dpc.index.writer.FileIndexingServiceWriter}) all fields
 *  specified in the {@link ErrorDocException} are stored plus:</b> <br>
 *  <br>
 *  <code><b>error</b> </code> - Set to 'true' for all documents indexed by this writer. Not stored.<br>
 *  <code><b>errormsg</b> </code> - The description of the error, for display. Stored. <br>
 *  <code><b>errordoctype</b> </code> - The type of error thrown as specified by {@link ErrorDocException}.
 *  Defaults to 'generic'. Stored. <br>
 *  <code><b>exception</b> </code> - The name of the Java exception that was thrown. Stored. <br>
 *  <br>
 *
 *
 * @author     John Weatherley
 * @see        org.dlese.dpc.index.writer.FileIndexingServiceWriter
 * @see        org.dlese.dpc.index.FileIndexingService
 * @see        ErrorDocException
 */
public class ErrorFileIndexingWriter extends FileIndexingServiceWriter {
	private Throwable exception = null;


	/**
	 *  Gets the docType, which is 'errordoc.'
	 *
	 * @return                Returns 'errordoc.'
	 * @exception  Exception  If error.
	 */
	public String getDocType() throws Exception {
		return "errordoc";
	}


	/**
	 *  Gets the specifier associated with this group of files or null if no group association exists.
	 *
	 * @return    The docGroup specifier
	 */
	public String getDocGroup() {
		return null;
	}


	/**
	 *  Gets the name of the concrete {@link org.dlese.dpc.index.reader.DocReader} class that is used to read
	 *  this type of {@link org.apache.lucene.document.Document}, which is "ErrorDocReader".
	 *
	 * @return    The STring "ErrorDocReader".
	 */
	public String getReaderClass() {
		return "org.dlese.dpc.index.reader.ErrorDocReader";
	}


	/**
	 *  Constructor for the ErrorFileIndexingWriter object
	 *
	 * @param  exception  The exception that was thrown when the error occured.
	 */
	public ErrorFileIndexingWriter(Throwable exception) {
		prtln("ErrorFileIndexingWriter exeption: " + exception);
		exception.printStackTrace();
		this.exception = exception;
	}


	/**
	 *  Adds the error message to the Lucene document.
	 *
	 * @param  newDoc         The new Document that is being created for this resource
	 * @param  existingDoc    An existing Document that currently resides in the index for the given resource, or
	 *      null if none was previously present
	 * @param  sourceFile     The sourceFile that is being indexed.
	 * @exception  Exception  If an error occurs
	 */
	protected final void addCustomFields(Document newDoc, Document existingDoc, File sourceFile) throws Exception {
		String errMsg = "";
		String errStackTrack = "";
		String exceptionName = "";
		String errorDocType = null;
		
		if (exception != null) {
			errMsg = exception.getMessage();
			exceptionName = exception.getClass().getName();
			
			//prtln("ErrorDoc: exception message 1 is: " + errMsg);
			StackTraceElement[] stack = exception.getStackTrace();
			for (int ii = 0; ii < stack.length; ii++)
				errStackTrack += " " + stack[ii].toString();
			
			if (errMsg == null || errMsg.length() == 0)
				errMsg = exception.getClass().getName() + ": " + errStackTrack;
			//prtln("ErrorDoc: exception message 3 is: " + errMsg);	

			// Index the fields indicated for the given error doc
			if (exception instanceof ErrorDocException) {
				ErrorDocException errorDocException = (ErrorDocException) exception;
				
				errorDocType = errorDocException.getErrorDocType();
				
				String[] keywordFieldNames = errorDocException.getKeywordFieldNames();
				if (keywordFieldNames != null)
					for (int i = 0; i < keywordFieldNames.length; i++)
						newDoc.add(new Field(keywordFieldNames[i], errorDocException.getKeywordFieldValue(keywordFieldNames[i]), Field.Store.YES, Field.Index.NOT_ANALYZED));

				String[] textFieldNames = errorDocException.getTextFieldNames();
				if (textFieldNames != null)
					for (int i = 0; i < textFieldNames.length; i++)
						newDoc.add(new Field(textFieldNames[i], errorDocException.getTextFieldValue(textFieldNames[i]), Field.Store.YES, Field.Index.ANALYZED));
			}
		}
		else
			prtln("ErrorDoc: exception is null!");

		// Add the doc dir encoded for searching
		newDoc.add(new Field("docdirenc",SimpleLuceneIndex.encodeToTerm(getSourceDir().getAbsolutePath()), Field.Store.NO, Field.Index.ANALYZED));
		
		if (errorDocType == null)
			errorDocType = "generic";
		newDoc.add(new Field("errordoctype", errorDocType, Field.Store.YES, Field.Index.ANALYZED));		
		newDoc.add(new Field("errormsg", errMsg, Field.Store.YES, Field.Index.ANALYZED));
		newDoc.add(new Field("exception", exceptionName, Field.Store.YES, Field.Index.ANALYZED));
		newDoc.add(new Field("stacktrace", errStackTrack, Field.Store.YES, Field.Index.ANALYZED));
		newDoc.add(new Field("error", "true", Field.Store.NO, Field.Index.ANALYZED));
		addToAdminDefaultField(errMsg);
		addToAdminDefaultField(exceptionName);
	}


	/**
	 *  Does nothing.
	 *
	 * @param  source         The source file being indexed
	 * @param  existingDoc    An existing Document that currently resides in the index for the given resource, or
	 *      null if none was previously present
	 * @exception  Exception  If an error occured during set-up.
	 */
	public void init(File source, Document existingDoc) throws Exception { }


	/**  Does nothing. */
	protected void destroy() { }

}

