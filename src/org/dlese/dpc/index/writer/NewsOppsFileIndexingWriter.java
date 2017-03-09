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
 *  Used to write a Lucene {@link org.apache.lucene.document.Document} for a DLESE news-opps XML record. The
 *  reader for this type of {@link org.apache.lucene.document.Document} is {@link
 *  org.dlese.dpc.index.reader.NewsOppsDocReader}. <p>
 *
 *
 *
 * @author     John Weatherley
 * @see        org.dlese.dpc.index.reader.XMLDocReader
 * @see        org.dlese.dpc.repository.RecordDataService
 * @see        org.dlese.dpc.index.writer.FileIndexingServiceWriter
 */
public class NewsOppsFileIndexingWriter extends XMLFileIndexingWriter {

	/**  The News Opps NS */
	public final static String FIELD_NS = "newsOpps";



	// --------------- Methods from FileIndexingServiceWriter --------------------

	/**
	 *  Gets the ID of this collection record.
	 *
	 * @return                The ID
	 * @exception  Exception  If error
	 */
	protected String[] _getIds() throws Exception {
		return new String[]{getDom4jDoc().selectSingleNode("/news-oppsRecord/recordID").getText()};
	}


	public String[] getUrls() throws Exception {
		// Announcement url
		Node node = getDom4jDoc().selectSingleNode("/news-oppsRecord/announcementURL");
		if (node == null)
			return null;
		return new String[]{node.getText()};
	}
	
	public String getDescription() throws Exception {
		// Description
		Node node = getDom4jDoc().selectSingleNode("/news-oppsRecord/description");
		if (node != null)
			return node.getText();
		return null;
	}
	
	public String getTitle() throws Exception {
		Node node = getDom4jDoc().selectSingleNode("/news-oppsRecord/title");
		if (node != null)
			return node.getText();
		return null;
	}
	
	/**
	 *  Gets the docType attribute of the NewsOppsFileIndexingWriter, which is 'news_opps.'
	 *
	 * @return    The docType, which is 'news_opps.'
	 */
	public String getDocType() {
		return "news_opps";
	}


	/**
	 *  Gets the name of the concrete {@link org.dlese.dpc.index.reader.DocReader} class that is used to read
	 *  this type of {@link org.apache.lucene.document.Document}, which is "NewsOppsDocReader".
	 *
	 * @return    The String "org.dlese.dpc.index.reader.NewsOppsDocReader".
	 */
	public String getReaderClass() {
		return "org.dlese.dpc.index.reader.NewsOppsDocReader";
	}


	private Date getPostDate() throws Exception {
		Date date = null;
		Node node = getDom4jDoc().selectSingleNode("/news-oppsRecord/postDate");
		if (node != null)
			date = MetadataUtils.parseUnionDateType(node.getText());
		return date;
	}


	/**
	 *  Returns the date used to determine "What's new" in the library, which is the post date.
	 *
	 * @return                The what's new date for the item
	 * @exception  Exception  This method should throw and Exception with appropriate error message if an error
	 *      occurs.
	 */
	protected Date getWhatsNewDate() throws Exception {
		Date date = getPostDate();
		if (date == null) {
			if (isMakingDeletedDoc())
				return new Date();
			else
				return new Date(getSourceFile().lastModified());
		}

		return date;
	}


	/**
	 *  Returns 'newsopps'.
	 *
	 * @return    The string 'newsopps'.
	 */
	protected String getWhatsNewType() {
		return "newsopps";
	}


	/**
	 *  Nothing needed.
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
	 *  Gets a report detailing any errors found in the XML validation of the news-opps record, or null if no
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
	 *  Default and stems fields handled here, so do not index full content.
	 *
	 * @return    False
	 */
	public boolean indexFullContentInDefaultAndStems() {
		return false;
	}	
	
	/**
	 *  Adds fields to the index that are part of the news-opps Document.
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
		Node node;
		List nodes;
		Element element;
		Attribute attribute;
		Date date;

		// Archive date:
		node = getDom4jDoc().selectSingleNode("/news-oppsRecord/archiveDate");
		if (node != null) {
			date = MetadataUtils.parseUnionDateType(node.getText());
			newDoc.add(
				new Field(FIELD_NS + "archivedate",
				DateFieldTools.dateToString(date), Field.Store.YES, Field.Index.NOT_ANALYZED));
		}

		// Record creation date date:
		node = getDom4jDoc().selectSingleNode("/news-oppsRecord/recordCreationDate");
		if (node != null) {
			date = MetadataUtils.parseUnionDateType(node.getText());
			newDoc.add(
				new Field(FIELD_NS + "recordCreationdate",
				DateFieldTools.dateToString(date), Field.Store.YES, Field.Index.NOT_ANALYZED));
		}

		// Post date:
		date = getPostDate();
		if (date != null)
			newDoc.add(new Field(FIELD_NS + "postdate",
				DateFieldTools.dateToString(date), Field.Store.YES, Field.Index.NOT_ANALYZED));

		// The new path to dates (as of 12/1/04):
		nodes = getDom4jDoc().selectNodes("/news-oppsRecord/otherDates/otherDate");
		for (int i = 0; i < nodes.size(); i++) {
			element = ((Element) nodes.get(i));
			date = MetadataUtils.parseUnionDateType(element.getText());
			// Use the date type as the date field name
			newDoc.add(
				new Field(FIELD_NS + element.attributeValue("type") + "date",
				DateFieldTools.dateToString(date), Field.Store.YES, Field.Index.NOT_ANALYZED));
		}

		// Old date path (can be removed after records have been updated):
		nodes = getDom4jDoc().selectNodes("/news-oppsRecord/dates/date");
		for (int i = 0; i < nodes.size(); i++) {
			element = ((Element) nodes.get(i));
			date = MetadataUtils.parseUnionDateType(element.getText());
			// Use the date type as the date field name
			newDoc.add(
				new Field(FIELD_NS + element.attributeValue("type") + "date",
				DateFieldTools.dateToString(date), Field.Store.YES, Field.Index.NOT_ANALYZED));
		}

		// Title
		String title = getTitle();
		if(title != null)
			addToDefaultField(title);		
		
		// Description
		String description = getDescription();
		if(description != null)
			addToDefaultField(description);

		// Announcement url
		node = getDom4jDoc().selectSingleNode("/news-oppsRecord/announcementURL");
		if (node != null) {
			value = node.getText();
			newDoc.add(new Field(FIELD_NS + "announcementURL", value, Field.Store.YES, Field.Index.NOT_ANALYZED));
		}

		// Announcements
		value = IndexingTools.makeSeparatePhrasesFromNodes(getDom4jDoc().selectNodes("/news-oppsRecord/announcements/announcement"));
		if (value != null) {
			newDoc.add(new Field(FIELD_NS + "announcement", value, Field.Store.YES, Field.Index.ANALYZED));
			addToDefaultField(value);
			//addToStemsField(value);
		}

		// Topics
		value = IndexingTools.makeSeparatePhrasesFromNodes(getDom4jDoc().selectNodes("/news-oppsRecord/topics/topic"));
		if (value != null) {
			newDoc.add(new Field(FIELD_NS + "topic", value, Field.Store.YES, Field.Index.ANALYZED));
			addToDefaultField(value);
			//addToStemsField(value);
		}

		// Keywords
		value = IndexingTools.makeSeparatePhrasesFromNodes(getDom4jDoc().selectNodes("/news-oppsRecord/keywords/keyword"));
		if (value != null) {
			newDoc.add(new Field(FIELD_NS + "keyword", value, Field.Store.YES, Field.Index.ANALYZED));
			addToDefaultField(value);
			//addToStemsField(value);
		}

		// Audiences
		value = IndexingTools.makeSeparatePhrasesFromNodes(getDom4jDoc().selectNodes("/news-oppsRecord/audiences/audience"));
		if (value != null) {
			newDoc.add(new Field(FIELD_NS + "audience", value, Field.Store.YES, Field.Index.ANALYZED));
			addToDefaultField(value);
			//addToStemsField(value);
		}

		// Diversities
		value = IndexingTools.makeSeparatePhrasesFromNodes(getDom4jDoc().selectNodes("/news-oppsRecord/diversities/diversity"));
		if (value != null) {
			newDoc.add(new Field(FIELD_NS + "diversity", value, Field.Store.YES, Field.Index.ANALYZED));
			addToDefaultField(value);
		}

		// City, state strings for display
		nodes = getDom4jDoc().selectNodes("/news-oppsRecord/locations/location");
		if (nodes != null && nodes.size() > 0) {
			ArrayList cityStates = new ArrayList(nodes.size());
			for (int i = 0; i < nodes.size(); i++) {
				node = ((Node) nodes.get(i)).selectSingleNode("@city");
				if (node == null)
					cityStates.add(((Node) nodes.get(i)).getText());
				else
					cityStates.add(node.getText().trim() + ", " + ((Node) nodes.get(i)).getText());
			}
			value = IndexingTools.makeSeparatePhrasesFromStrings(cityStates);
			newDoc.add(new Field(FIELD_NS + "cityStates", value, Field.Store.YES, Field.Index.NO));
		}

		// States, countries...
		value = IndexingTools.makeStringFromNodes(getDom4jDoc().selectNodes("/news-oppsRecord/locations/location"));
		if (value != null) {
			newDoc.add(new Field(FIELD_NS + "location", value, Field.Store.YES, Field.Index.ANALYZED));
			addToDefaultField(value);
		}

		// Cities
		value = IndexingTools.makeSeparatePhrasesFromNodes(getDom4jDoc().selectNodes("/news-oppsRecord/locations/location/@city"));
		if (value != null) {
			newDoc.add(new Field(FIELD_NS + "city", value, Field.Store.YES, Field.Index.ANALYZED));
			addToDefaultField(value);
		}

		// Sponsor
		value = IndexingTools.makeSeparatePhrasesFromNodes(getDom4jDoc().selectNodes("/news-oppsRecord/contributors/contributor[@role='Sponsor']/organization/instName"));
		if (value != null) {
			newDoc.add(new Field(FIELD_NS + "sponsor", value, Field.Store.YES, Field.Index.ANALYZED));
			addToDefaultField(value);
		}
	}



	// --------------- Overridden methods from the super class ---------------

	// --------------- Concrete methods ---------------

	/**  Create a NewsOppsFileIndexingWriter. */
	public NewsOppsFileIndexingWriter() { }

}

