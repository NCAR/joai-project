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

import java.lang.*;
import java.util.HashMap;

/**
 *  Exception that may be used to pass text and keywords to be written to the index by the
 *  ErrorFileIndexingWriter if an error occurs during indexing.
 *
 * @author     John Weatherley
 * @see        ErrorFileIndexingWriter
 */
public class ErrorDocException extends Exception {
	private HashMap keywordFields = new HashMap();
	private HashMap textFields = new HashMap();
	private String errorDocType = null;


	/**
	 *  Constructs an <code>Exception</code> with the specified detail message.
	 *
	 * @param  message  the detailed message.
	 */
	public ErrorDocException(String message, String errorDocType) {
		super(message);
		this.errorDocType = errorDocType;
	}
	
	
	public String getErrorDocType() {
		return errorDocType;	
	}

	/**
	 *  Adds a field to be indexed as a keyword.
	 *
	 * @param  fieldName  The field name
	 * @param  keyword    The field content
	 */
	public void putKeywordField(String fieldName, String keyword) {
		keywordFields.put(fieldName, keyword);
	}


	/**
	 *  Gets the names of the fields to be indexed as keywords.
	 *
	 * @return    The keywordFieldNames value
	 */
	public String[] getKeywordFieldNames() {
		return (String[]) keywordFields.keySet().toArray(new String[]{});
	}


	/**
	 *  Gets the value of the field to be indexed as a keyword.
	 *
	 * @param  fieldName  The field name
	 * @return            The value, or null
	 */
	public String getKeywordFieldValue(String fieldName) {
		return (String) keywordFields.get(fieldName);
	}


	/**
	 *  Adds a field to be indexed as text.
	 *
	 * @param  fieldName  The field name
	 * @param  text       The field value
	 */
	public void putTextField(String fieldName, String text) {
		textFields.put(fieldName, text);
	}


	/**
	 *  Gets the names of the fields to be indexed as text.
	 *
	 * @return    The field names
	 */
	public String[] getTextFieldNames() {
		return (String[]) textFields.keySet().toArray(new String[]{});
	}


	/**
	 *  Gets the value of the field to be indexed as text.
	 *
	 * @param  fieldName  The field name
	 * @return            The value
	 */
	public String getTextFieldValue(String fieldName) {
		return (String) textFields.get(fieldName);
	}

}

