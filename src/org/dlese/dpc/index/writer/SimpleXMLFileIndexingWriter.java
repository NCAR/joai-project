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
import org.dom4j.*;

import org.dlese.dpc.xml.*;
import org.dlese.dpc.index.*;
import org.dlese.dpc.util.*;
import org.apache.lucene.document.*;
import org.dlese.dpc.repository.*;
import org.dlese.dpc.vocab.*;
import org.dlese.dpc.index.writer.xml.XMLIndexer;

/**
 *  This is the default writer for generic XML formats. Creates a Lucene {@link
 *  org.apache.lucene.document.Document} from any valid XML file by stripping the XML tags to extract and
 *  index the content. The full content of all Elements and Attributes is indexed in the default and
 *  admindefault fields and is stemmed and indexed in the stems field. The reader for this type of Document is
 *  XMLDocReader.
 *
 * @author    John Weatherley
 * @see       org.dlese.dpc.index.FileIndexingService
 * @see       org.dlese.dpc.index.reader.XMLDocReader
 */
public class SimpleXMLFileIndexingWriter extends XMLFileIndexingWriter {
	private String doctype = null;


	/**  Constructor for the SimpleXMLFileIndexingWriter object */
	public SimpleXMLFileIndexingWriter() { }


	/**
	 *  Gets the xml format for this document, for example "oai_dc," "adn," "dlese_ims," or "dlese_anno".
	 *
	 * @return                The docType value
	 * @exception  Exception  If errlr.
	 */
	public String getDocType()
		 throws Exception {
		return (String) getConfigAttributes().get("xmlFormat");
	}


	/**
	 *  Gets the name of the concrete {@link org.dlese.dpc.index.reader.DocReader} class that is used to read
	 *  this type of {@link org.apache.lucene.document.Document}, which is
	 *  "org.dlese.dpc.index.reader.XMLDocReader".
	 *
	 * @return    The STring "org.dlese.dpc.index.reader.XMLDocReader".
	 */
	public String getReaderClass() {
		return "org.dlese.dpc.index.reader.XMLDocReader";
	}


	/**
	 *  This method is called prior to processing and may be used to for any necessary set-up. This method should
	 *  throw and exception with appropriate message if an error occurs.
	 *
	 * @param  sourceFile     The sourceFile being indexed.
	 * @param  existingDoc    An existing Document that exists for this in the index.
	 * @exception  Exception  If error
	 */
	public void init(File sourceFile, org.apache.lucene.document.Document existingDoc)
		 throws Exception {
		//xmlIndexer = new XMLIndexer(getFileContent(), getDocType());
	}


	/**
	 *  Returns the date used to determine "What's new" in the library, which is null (unknown).
	 *
	 * @return                The what's new date for the item
	 * @exception  Exception  This method should throw and Exception with appropriate error message if an error
	 *      occurs.
	 */
	protected Date getWhatsNewDate() throws Exception {
		return null;
	}


	/**
	 *  Returns null (unknown).
	 *
	 * @return                null.
	 * @exception  Exception  This method should throw and Exception with appropriate error message if an error
	 *      occurs.
	 */
	protected String getWhatsNewType() throws Exception {
		return null;
	}


	/**  Does nothing. */
	protected void destroy() { }


	/**
	 *  Gets a report detailing any errors found in the validation of the data, or null if no error was found.
	 *  This method performs schema validation over the XML.
	 *
	 * @return                Null if no data validation errors were found, otherwise a String that details the
	 *      nature of the error.
	 * @exception  Exception  If error in performing the validation.
	 */
	protected String getValidationReport() throws Exception {
		// Do schema validation here...
		if (isMakingDeletedDoc())
			return XMLValidator.validateString(getFileContent());
		else
			return XMLValidator.validateFile(getSourceFile());
	}


	/**
	 *  Returns null to handle by super.
	 *
	 * @return    Null
	 */
	protected String[] _getIds() {
		return null;
	}


	/**
	 *  Gets the urls attribute of the SimpleXMLFileIndexingWriter object
	 *
	 * @return    The urls value
	 */
	public String[] getUrls() {
		return null;
	}


	/**
	 *  Gets the description attribute of the SimpleXMLFileIndexingWriter object
	 *
	 * @return    The description value
	 */
	public String getDescription() {
		return null;
	}


	/**
	 *  Gets the title attribute of the SimpleXMLFileIndexingWriter object
	 *
	 * @return    The title value
	 */
	public String getTitle() {
		return null;
	}


	/**
	 *  Place the entire XML content into the default and stems search field.
	 *
	 * @return    True
	 */
	public boolean indexFullContentInDefaultAndStems() {
		return true;
	}


	/**
	 *  Nothing to do here. All functionality handled by super.
	 *
	 * @param  newDoc         The new {@link org.apache.lucene.document.Document} that is being created for this
	 *      resource
	 * @param  existingDoc    An existing {@link org.apache.lucene.document.Document} that currently resides in
	 *      the index for the given resource, or null if none was previously present
	 * @param  sourceFile     The feature to be added to the CustomFields attribute
	 * @exception  Exception  This method should throw and Exception with appropriate error message if an error
	 *      occurs.
	 */
	protected void addFields(org.apache.lucene.document.Document newDoc, org.apache.lucene.document.Document existingDoc, File sourceFile) throws Exception {

	}

}

