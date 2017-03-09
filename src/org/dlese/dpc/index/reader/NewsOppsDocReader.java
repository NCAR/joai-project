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
import org.dlese.dpc.repository.*;
import org.dlese.dpc.xml.*;
import org.dlese.dpc.webapps.tools.*;
import org.dlese.dpc.util.*;
import org.dlese.dpc.vocab.*;
import org.dlese.dpc.index.document.DateFieldTools;

import javax.servlet.*;
import java.io.*;
import java.text.*;
import java.util.*;

/**
 *  A bean for accessing the data stored in a Lucene {@link org.apache.lucene.document.Document} that was
 *  indexed from a DLESE news and opportunities metadata record. The index writer that is responsible for
 *  creating this type of Lucene {@link org.apache.lucene.document.Document} is a {@link
 *  org.dlese.dpc.index.writer.NewsOppsFileIndexingWriter}.
 *
 * @author     John Weatherley
 * @see        org.dlese.dpc.index.writer.NewsOppsFileIndexingWriter
 */
public class NewsOppsDocReader extends XMLDocReader {
	private final String DEFAULT = "(null)";


	/**  Constructor for the NewsOppsDocReader object */
	public NewsOppsDocReader() { }


	/**
	 *  Constructor that may be used programatically to wrap a reader around a Lucene {@link
	 *  org.apache.lucene.document.Document} created by a {@link org.dlese.dpc.index.writer.DleseCollectionFileIndexingWriter}.
	 *
	 * @param  doc    A Lucene {@link org.apache.lucene.document.Document} created by a {@link
	 *      org.dlese.dpc.index.writer.DleseCollectionFileIndexingWriter}.
	 * @param  index  The index being used
	 */
	public NewsOppsDocReader(Document doc, SimpleLuceneIndex index) {
		super(doc);
		if (index != null) {
			recordDataService = (RecordDataService) index.getAttribute("recordDataService");
			metadataVocab = recordDataService.getVocab();
		}
	}



	/**
	 *  Gets the String 'NewsOppsDocReader,' which is the key that describes this reader type. This may be used
	 *  in (Struts) beans to determine which type of reader is available for a given search result and thus what
	 *  data is available for display in the UI. The reader type determines which getter methods are available.
	 *
	 * @return    The String 'NewsOppsDocReader'.
	 */
	public String getReaderType() {
		return "NewsOppsDocReader";
	}



	/**
	 *  Gets the title of the new-opps item.
	 *
	 * @return    The title
	 */
	public String getTitle() {
		return doc.get(NewsOppsFileIndexingWriter.FIELD_NS + "title");
	}


	/**
	 *  Gets the description of the new-opps item.
	 *
	 * @return    The description
	 */
	public String getDescription() {
		return doc.get(NewsOppsFileIndexingWriter.FIELD_NS + "description");
	}


	/**
	 *  Gets the announcement Url. Same as {@link #getUrl}.
	 *
	 * @return    The announcementUrl value
	 */
	public String getAnnouncementUrl() {
		return doc.get(NewsOppsFileIndexingWriter.FIELD_NS + "announcementURL");
	}


	/**
	 *  The primary URL, which for news opps is the same as {@link #getAnnouncementUrl}.
	 *
	 * @return    Same as {@link #getAnnouncementUrl}
	 */
	public String getUrl() {
		return getAnnouncementUrl();
	}


	/**
	 *  Gets the announcements attribute of the NewsOppsDocReader object
	 *
	 * @return    The announcements value
	 */
	public String[] getAnnouncements() {
		return IndexingTools.extractSeparatePhrasesFromString(doc.get(NewsOppsFileIndexingWriter.FIELD_NS + "announcement"));
	}


	/**
	 *  Gets the topics attribute of the NewsOppsDocReader object
	 *
	 * @return    The topics value
	 */
	public String[] getTopics() {
		return IndexingTools.extractSeparatePhrasesFromString(doc.get(NewsOppsFileIndexingWriter.FIELD_NS + "topic"));
	}


	/**
	 *  Gets the keywords attribute of the NewsOppsDocReader object
	 *
	 * @return    The keywords value
	 */
	public String[] getKeywords() {
		return IndexingTools.extractSeparatePhrasesFromString(doc.get(NewsOppsFileIndexingWriter.FIELD_NS + "keyword"));
	}


	/**
	 *  Gets the audiences attribute of the NewsOppsDocReader object
	 *
	 * @return    The audiences value
	 */
	public String[] getAudiences() {
		return IndexingTools.extractSeparatePhrasesFromString(doc.get(NewsOppsFileIndexingWriter.FIELD_NS + "audience"));
	}


	/**
	 *  Gets the diversities attribute of the NewsOppsDocReader object
	 *
	 * @return    The diversities value
	 */
	public String[] getDiversities() {
		return IndexingTools.extractSeparatePhrasesFromString(doc.get(NewsOppsFileIndexingWriter.FIELD_NS + "diversity"));
	}


	/**
	 *  Gets the locations, which are two character state or country codes, for example 'CO'.
	 *
	 * @return    The locations value
	 */
	public String[] getLocations() {
		return IndexingTools.extractStringsFromString(doc.get(NewsOppsFileIndexingWriter.FIELD_NS + "location"));
	}


	/**
	 *  Gets an array of city state strings for display. These are comprised of a city, if provided, followed by
	 *  a comma followed by the state abbreviation. For example 'Boulder, CO'.
	 *
	 * @return    An array of city-state strings, for example 'Boulder, CO'
	 */
	public String[] getCityStates() {
		return IndexingTools.extractSeparatePhrasesFromString(doc.get(NewsOppsFileIndexingWriter.FIELD_NS + "cityStates"));
	}


	/**
	 *  Gets the sponsors attribute of the NewsOppsDocReader object
	 *
	 * @return    The sponsors value
	 */
	public String[] getSponsors() {
		return IndexingTools.extractSeparatePhrasesFromString(doc.get(NewsOppsFileIndexingWriter.FIELD_NS + "sponsor"));
	}


	/**
	 *  Gets the eventStartDate attribute of the NewsOppsDocReader object
	 *
	 * @return    The eventStartDate value
	 */
	public Date getEventStartDate() {
		return getDate(NewsOppsFileIndexingWriter.FIELD_NS + "eventStart" + "date");
	}


	/**
	 *  Gets the eventStopDate attribute of the NewsOppsDocReader object
	 *
	 * @return    The eventStopDate value
	 */
	public Date getEventStopDate() {
		return getDate(NewsOppsFileIndexingWriter.FIELD_NS + "eventStop" + "date");
	}


	/**
	 *  Gets the archiveDate attribute of the NewsOppsDocReader object
	 *
	 * @return    The archiveDate value
	 */
	public Date getArchiveDate() {
		return getDate(NewsOppsFileIndexingWriter.FIELD_NS + "archive" + "date");
	}


	/**
	 *  Gets the postDate attribute of the NewsOppsDocReader object
	 *
	 * @return    The postDate value
	 */
	public Date getPostDate() {
		return getDate(NewsOppsFileIndexingWriter.FIELD_NS + "post" + "date");
	}


	/**
	 *  Gets the recordCreationtDate attribute of the NewsOppsDocReader object
	 *
	 * @return    The recordCreationtDate value
	 */
	public Date getRecordCreationtDate() {
		return getDate(NewsOppsFileIndexingWriter.FIELD_NS + "recordCreation" + "date");
	}


	/**
	 *  Gets the dueDate attribute of the NewsOppsDocReader object
	 *
	 * @return    The dueDate value
	 */
	public Date getDueDate() {
		return getDate(NewsOppsFileIndexingWriter.FIELD_NS + "due" + "date");
	}


	/**
	 *  Gets the applyByDate attribute of the NewsOppsDocReader object
	 *
	 * @return    The applyByDate value
	 */
	public Date getApplyByDate() {
		return getDate(NewsOppsFileIndexingWriter.FIELD_NS + "applyBy" + "date");
	}


	private Date getDate(String dateField) {
		String t = doc.get(dateField);

		if (t == null)
			return null;

		try {
			return new Date(DateFieldTools.stringToTime(t));
		} catch (Throwable e) {
			return null;
		}
	}

}


