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
import org.dlese.dpc.index.reader.*;
import org.dlese.dpc.services.mmd.MmdRec;
import org.dlese.dpc.util.*;
import org.apache.lucene.document.*;
import org.dlese.dpc.repository.*;
import org.dlese.dpc.vocab.*;
import org.dlese.dpc.index.document.DateFieldTools;

import org.dom4j.Element;
import org.dom4j.Attribute;
import org.dom4j.Node;

/**
 *  Used to write a Lucene {@link org.apache.lucene.document.Document} for a NCS Collection XML record.
 *
 * @author    John Weatherley
 * @see       org.dlese.dpc.index.reader.XMLDocReader
 * @see       org.dlese.dpc.repository.RecordDataService
 * @see       org.dlese.dpc.index.writer.FileIndexingServiceWriter
 */
public class NCSCollectionFileIndexingWriter extends XMLFileIndexingWriter {

	private String metadataFormat = "ncs_collect";


	/**  Create a NCSCollectionFileIndexingWriter. */
	public NCSCollectionFileIndexingWriter() { }


	/**
	 *  Returns the title for the collection.
	 *
	 * @return                The title value
	 * @exception  Exception  If error reading XML.
	 */
	public String getTitle() throws Exception {
		List nodes = getDom4jDoc().selectNodes("/record/general/title");
		if (nodes.size() == 0)
			return "";
		Element element = (Element) nodes.get(0);
		return element.getText();
	}


	/**
	 *  The description for the collection.
	 *
	 * @return                The description String
	 * @exception  Exception  If error reading XML.
	 */
	public String getDescription() throws Exception {
		List nodes = getDom4jDoc().selectNodes("/record/general/description");
		if (nodes.size() == 0)
			return "";
		Element element = (Element) nodes.get(0);
		return element.getText();
	}


	/**
	 *  Gets the URL to the collection.
	 *
	 * @return                The collectionUrl value
	 * @exception  Exception  If error
	 */
	public String[] getUrls() throws Exception {
		List nodes = getDom4jDoc().selectNodes("/record/general/url");
		if (nodes.size() == 0)
			return null;
		Element element = (Element) nodes.get(0);
		return new String[]{element.getText()};
	}



	// --------------- Methods from FileIndexingServiceWriter --------------------

	/**
	 *  Gets the ID of this collection record.
	 *
	 * @return                The ID
	 * @exception  Exception  If error
	 */
	protected String[] _getIds() throws Exception {
		List nodes = getDom4jDoc().selectNodes("/record/general/recordID");
		if (nodes.size() != 1)
			throw new Exception("Unable to get ID. Wrong number of elements: " + nodes.size());
		Element element = (Element) nodes.get(0);
		return new String[]{element.getText()};
	}


	/**
	 *  Gets the docType attribute of the NCSCollectionFileIndexingWriter, which is 'ncs_collect.'
	 *
	 * @return    The docType, which is 'ncs_collect.'
	 */
	public String getDocType() {
		return metadataFormat;
	}


	/**
	 *  Gets the name of the concrete {@link org.dlese.dpc.index.reader.DocReader} class that is used to read
	 *  this type of {@link org.apache.lucene.document.Document}, which is "XMLDocReader".
	 *
	 * @return    The String "org.dlese.dpc.index.reader.XMLDocReader".
	 */
	public String getReaderClass() {
		return "org.dlese.dpc.index.reader.XMLDocReader";
	}



	/**
	 *  Returns the date used to determine "What's new" in the library. Just returns the file mod date.
	 *
	 * @return                The what's new date for the item
	 * @exception  Exception  This method should throw and Exception with appropriate error message if an error
	 *      occurs.
	 */
	protected Date getWhatsNewDate() throws Exception {
		if (isMakingDeletedDoc())
			return new Date();
		else
			return new Date(getSourceFile().lastModified());
	}


	/**
	 *  Returns 'collection'.
	 *
	 * @return    The string 'collection'.
	 */
	protected String getWhatsNewType() {
		return "collection";
	}


	/**
	 *  Nothing needed
	 *
	 * @param  source         The source file being indexed
	 * @param  existingDoc    An existing Document that currently resides in the index for the given resource, or
	 *      null if none was previously present
	 * @exception  Exception  If an error occured during set-up.
	 */
	public void init(File source, Document existingDoc) throws Exception {
	}


	/**  This method is called at the conclusion of processing and may be used for tear-down. */
	protected void destroy() {

	}


	/**
	 *  Gets a report detailing any errors found in the XML validation of the collection record, or null if no
	 *  error was found.
	 *
	 * @return                Null if no data validation errors were found, otherwise a String that details the
	 *      nature of the error.
	 * @exception  Exception  If error in performing the validation.
	 */
	protected String getValidationReport() throws Exception {
		if (isMakingDeletedDoc())
			return XMLValidator.validateString(getFileContent());
		else
			return XMLValidator.validateFile(getSourceFile());
	}


	// --------------- Inherited methods --------------------


	/**
	 *  Place the entire XML content into the default and stems search field.
	 *
	 * @return    True
	 */
	public boolean indexFullContentInDefaultAndStems() {
		return true;
	}


	/**
	 *  Adds fields to the index that are part of the collection-level Document.
	 *
	 * @param  newDoc         The new Document that is being created for this resource
	 * @param  existingDoc    An existing Document that currently resides in the index for the given resource, or
	 *      null if none was previously present
	 * @param  sourceFile     The sourceFile that is being indexed.
	 * @exception  Exception  If an error occurs
	 */
	protected final void addFields(Document newDoc, Document existingDoc, File sourceFile)
		 throws Exception {

		String value;
		List nodes;

		nodes = getDom4jDoc().selectNodes("/record/general/subject");
		for (int i = 0; i < nodes.size(); i++) {
			Element element = (Element) nodes.get(i);
			newDoc.add(new Field("ncsCollectSubject", element.getText(), Field.Store.YES, Field.Index.ANALYZED));
		}

		nodes = getDom4jDoc().selectNodes("/record/educational/educationLevels/nsdlEdLevel");
		for (int i = 0; i < nodes.size(); i++) {
			Element element = (Element) nodes.get(i);
			newDoc.add(new Field("ncsCollectEdLevel", element.getText(), Field.Store.YES, Field.Index.ANALYZED));
		}

		nodes = getDom4jDoc().selectNodes("/record/educational/educationLevels/otherEdLevel");
		for (int i = 0; i < nodes.size(); i++) {
			Element element = (Element) nodes.get(i);
			newDoc.add(new Field("ncsCollectEdLevel", element.getText(), Field.Store.YES, Field.Index.ANALYZED));
		}

		nodes = getDom4jDoc().selectNodes("/record/educational/audiences/nsdlAudience");
		for (int i = 0; i < nodes.size(); i++) {
			Element element = (Element) nodes.get(i);
			newDoc.add(new Field("ncsCollectAudience", element.getText(), Field.Store.YES, Field.Index.ANALYZED));
		}

		nodes = getDom4jDoc().selectNodes("/record/educational/audiences/otherAudience");
		for (int i = 0; i < nodes.size(); i++) {
			Element element = (Element) nodes.get(i);
			newDoc.add(new Field("ncsCollectAudience", element.getText(), Field.Store.YES, Field.Index.ANALYZED));
		}

		value = getDom4jDoc().valueOf("/record/collection/ingest/oai/@baseURL");
		if (value != null && value.trim().length() > 0) {
			newDoc.add(new Field("ncsCollectHasOai", "true", Field.Store.YES, Field.Index.NOT_ANALYZED));
			newDoc.add(new Field("ncsCollectOaiBaseUrl", value, Field.Store.YES, Field.Index.NOT_ANALYZED));

			value = getDom4jDoc().valueOf("/record/collection/ingest/oai/@frequency");
			if (value != null && value.trim().length() > 0)
				newDoc.add(new Field("ncsCollectOaiFrequency", value, Field.Store.YES, Field.Index.NOT_ANALYZED));
		}
		else {
			newDoc.add(new Field("ncsCollectHasOai", "false", Field.Store.YES, Field.Index.NOT_ANALYZED));
		}

		value = getDom4jDoc().valueOf("/record/collection/OAIvisibility");
		if (value != null && value.trim().length() > 0)
			newDoc.add(new Field("ncsCollectOaiVisibility", value, Field.Store.YES, Field.Index.ANALYZED));

		value = getDom4jDoc().valueOf("/record/collection/pathway");
		if (value == null || value.trim().length() == 0)
			value = "empty";
		newDoc.add(new Field("ncsCollectIsPathway", value, Field.Store.YES, Field.Index.ANALYZED));

		nodes = getDom4jDoc().selectNodes("/record/collection/pathways/name");
		if (nodes.size() == 0) {
			value = "empty";
			newDoc.add(new Field("ncsCollectPathwayName", value, Field.Store.YES, Field.Index.ANALYZED));
		}
		else {
			for (int i = 0; i < nodes.size(); i++) {
				Element element = (Element) nodes.get(i);
				newDoc.add(new Field("ncsCollectPathwayName", element.getText(), Field.Store.YES, Field.Index.ANALYZED));
			}
		}

		nodes = getDom4jDoc().selectNodes("/record/collection/collectionSubjects/collectionSubject");
		for (int i = 0; i < nodes.size(); i++) {
			Element element = (Element) nodes.get(i);
			newDoc.add(new Field("ncsCollectCollectionSubject", element.getText(), Field.Store.YES, Field.Index.ANALYZED));
		}

		nodes = getDom4jDoc().selectNodes("/record/collection/collectionPurposes/collectionPurpose");
		for (int i = 0; i < nodes.size(); i++) {
			Element element = (Element) nodes.get(i);
			newDoc.add(new Field("ncsCollectCollectionPurpose", element.getText(), Field.Store.YES, Field.Index.ANALYZED));
		}

	}

	// --------------- Overridden methods from the super class ---------------

	// None...

	// --------------- Concrete methods ---------------

	// None...

}

