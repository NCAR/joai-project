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
package org.dlese.dpc.schemedit.dcs;

import org.apache.lucene.document.*;
import org.dlese.dpc.index.reader.*;
import org.dlese.dpc.index.*;
import org.dlese.dpc.repository.*;
import org.dlese.dpc.index.document.DateFieldTools;

import javax.servlet.*;
import java.util.*;

/**
 *  Reader to extract DcsData (aka "workflow status") information from a LuceneDoc
 *  created by {@link org.dlese.dpc.schemedit.dcs.DcsDataFileIndexingPlugin}.
 *
 *
 *@author    Jonathan Ostwald
 */
public class DcsDataDocReader extends XMLDocReader {
	private final String DEFAULT = "(null)";


	/**
	 *  Constructor for the DcsDataDocReader object
	 */
	public DcsDataDocReader() { }


	/**
	 *  Constructor that may be used programatically to wrap a reader around a
	 *  Lucene {@link org.apache.lucene.document.Document} created by a {@link
	 *  org.dlese.dpc.index.writer.DleseCollectionFileIndexingWriter}.
	 *
	 *@param  doc    A Lucene {@link org.apache.lucene.document.Document} created
	 *      by a {@link org.dlese.dpc.index.writer.DleseCollectionFileIndexingWriter}.
	 *@param  index  The index being used
	 */
	public DcsDataDocReader(Document doc, SimpleLuceneIndex index) {
		super(doc);
		if (index != null) {
			recordDataService = (RecordDataService) index.getAttribute("recordDataService");
			metadataVocab = recordDataService.getVocab();
		}
	}


	/**
	 *  Gets the String 'DcsDataDocReader,' which is the key that describes this
	 *  reader type. This may be used in (Struts) beans to determine which type of
	 *  reader is available for a given search result and thus what data is
	 *  available for display in the UI. The reader type determines which getter
	 *  methods are available.
	 *
	 *@return    The String 'DcsDataDocReader'.
	 */
	public String getReaderType() {
		return "DcsDataDocReader";
	}


	/**
	 *  Gets the title of the new-opps item.
	 *
	 *@return    The title
	 */
	public String getLastEditor() {
		return doc.get(DcsDataFileIndexingPlugin.FIELD_NS + "lastEditor");
	}

	public String hasNdrSyncError () {
		return doc.get(DcsDataFileIndexingPlugin.FIELD_NS + "hasSyncError");
	}

	public String getNdrHandle () {
		return doc.get(DcsDataFileIndexingPlugin.FIELD_NS + "ndrHandle");
	}
	
	/**
	 *  Gets the status attribute of the DcsDataDocReader object
	 *
	 *@return    The status value
	 */
	public String getStatus() {
		return doc.get(DcsDataFileIndexingPlugin.FIELD_NS + "status");
	}


	/**
	 *  Gets the description of the new-opps item.
	 *
	 *@return    The description
	 */
	public String getStatusNote() {
		return doc.get(DcsDataFileIndexingPlugin.FIELD_NS + "statusNote");
	}


	/**
	 *  Gets the isValid attribute of the DcsDataDocReader object
	 *
	 *@return    The isValid value
	 */
	public String getIsValid() {
		return doc.get(DcsDataFileIndexingPlugin.FIELD_NS + "isValid");
	}
	
	public String getIsFinalStatus() {
		return doc.get(DcsDataFileIndexingPlugin.FIELD_NS + "isFinalStatus");
	}

	/**
	 *  Gets the eventStartDate attribute of the DcsDataDocReader object
	 *
	 *@return    The eventStartDate value
	 */
	public Date getLastTouchDate() {
		return getDate(DcsDataFileIndexingPlugin.FIELD_NS + "lastTouchDate");
	}

	public Date getRecordCreationDate() {
		return getDate (DcsDataFileIndexingPlugin.FIELD_NS + "recordCreationDate");
	}
	
	public String getRecordCreator () {
		return doc.get(DcsDataFileIndexingPlugin.FIELD_NS + "recordCreator");
	}

	/**
	 *  Gets the date attribute of the DcsDataDocReader object
	 *
	 *@param  dateField  Description of the Parameter
	 *@return            The date value
	 */
	private Date getDate(String dateField) {
		String t = doc.get(dateField);

		if (t == null) {
			return null;
		}

		try {
			return new Date(DateFieldTools.stringToTime(t));
		} catch (Throwable e) {
			return null;
		}
	}

}

