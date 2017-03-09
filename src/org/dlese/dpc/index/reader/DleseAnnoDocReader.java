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

import org.dlese.dpc.index.*;
import org.dlese.dpc.index.writer.*;
import org.dlese.dpc.index.*;
import org.dlese.dpc.repository.*;
import org.apache.lucene.document.*;
import org.dlese.dpc.index.document.DateFieldTools;

import java.util.*;
import java.text.*;

/**
 *  A bean for accessing the data stored in a Lucene {@link org.apache.lucene.document.Document} that was
 *  indexed from a DLESE annotation-level metadata record. The index writer that is responsible for creating
 *  this type of Lucene {@link org.apache.lucene.document.Document} is a {@link
 *  org.dlese.dpc.index.writer.DleseAnnoFileIndexingServiceWriter}.
 *
 * @author     John Weatherley
 * @see        org.dlese.dpc.index.ResultDoc
 * @see        org.dlese.dpc.index.writer.DleseAnnoFileIndexingServiceWriter
 */
public class DleseAnnoDocReader extends XMLDocReader {

	private ResultDoc itemResultDoc = null;


	/**
	 *  Gets the String 'DleseAnnoDocReader,' which is the key that describes this reader type. This may be used
	 *  in (Struts) beans to determine which type of reader is available for a given search result and thus what
	 *  data is available for display in the UI. The reader type determines which getter methods are available.
	 *
	 * @return    The String 'DleseAnnoDocReader'.
	 */
	public String getReaderType() {
		return "DleseAnnoDocReader";
	}


	/**
	 *  Gets the ResultDoc of the item this annotates, or null if not available.
	 *
	 * @return    The athe ResultDoc of the item this annotates.
	 */
	public ResultDoc getAnnotatedItemResultDoc() {
		if (itemResultDoc != null)
			return itemResultDoc;
		if (recordDataService != null)
			itemResultDoc = recordDataService.getItemResultDoc(getItemId());
		return itemResultDoc;
	}



	/**
	 *  Gets the serviceName attribute of the DleseAnnoDocReader object
	 *
	 * @return    The serviceName value
	 */
	public String getServiceName() {
		String t = doc.get("annoservicename");

		if (t == null)
			return "";
		else
			return t;
	}


	/**
	 *  Gets the ID of the item this annotates.
	 *
	 * @return    The ID of the annotated item
	 */
	public String getItemId() {
		String t = doc.get("annoitemid");

		if (t == null)
			return "";
		else
			return t;
	}


	/**
	 *  Gets the title of the annotation.
	 *
	 * @return    The title or empty if none
	 */
	public String getTitle() {
		String t = doc.get("annotitle");

		if (t == null)
			return "";
		else
			return t;
	}


	/**
	 *  Gets the content of the annotation itself, or empty if none.
	 *
	 * @return    The content or empty
	 * @see       #getUrl
	 */
	public String getDescription() {
		String t = doc.get("annodescription");

		if (t == null)
			return "";
		else
			return t;
	}


	/**
	 *  Gets the url containing the content of the annotation, or empty if none.
	 *
	 * @return    The url or empty
	 * @see       #getDescription
	 */
	public String getUrl() {
		String t = doc.get("annourl");

		if (t == null)
			return "";
		else
			return t;
	}


	/**
	 *  Gets the annotation pathway, for example 'CRS (Community Review System)'.
	 *
	 * @return    The pathway value or empty
	 */
	public String getPathway() {
		String t = doc.get("annopathway");

		if (t == null)
			return "";
		else
			return t;
	}


	/**
	 *  Gets the star rating as a number from 1 to 5 or empty if none assigned. Available since anno framwork
	 *  v1.0.
	 *
	 * @return    The start rating or empty String if none
	 */
	public String getRating() {
		String t = doc.get("annorating");

		if (t == null)
			return "";
		else
			return t;
	}


	/**
	 *  Gets the status of the annotation, which is one of 'completed', 'in progress' or 'retired'. Note that
	 *  status used to also contain the format string but there is now a separate field for format - see {@link
	 *  #getFormat}. The status String here is the same regardless of whether the underlying record is in
	 *  framework version 1.0 or 0.1.
	 *
	 * @return    The status value
	 */
	public String getStatus() {
		String t = doc.get("annostatus");

		if (t == null)
			return "";
		else
			return t;
	}


	/**
	 *  Gets the format of the annotation, which is one of 'audio', 'graphical', 'text', or 'video'. The format
	 *  String here is the same regardless of whether the underlying record is in framework version 1.0 or 0.1.
	 *  Note that format used to be part of the status in v0.1 - see {@link #getStatus}.
	 *
	 * @return    The format value
	 */
	public String getFormat() {
		String t = doc.get("annoformat");

		if (t == null)
			return "";
		else
			return t;
	}


	/**
	 *  Gets the type of annotation, for example 'Review', 'Comment', 'Educational standard', etc.
	 *
	 * @return    The type value
	 */
	public String getType() {
		String t = doc.get("annotype");

		if (t == null)
			return "";
		else
			return t;
	}


	/**
	 *  Gets the annotation date as a String, available only for annos in verion 0.1.
	 *
	 * @return        The date String, or empty if none available.
	 * @deprecated    As of annotation framework v1.0, this field no longer exists.
	 */
	public String getDate() {
		String t = doc.get("annodate");

		if (t == null)
			return "";

		try {
			SimpleDateFormat df = new SimpleDateFormat("MMM' 'dd', 'yyyy");
			return df.format(DateFieldTools.stringToDate(t));
		} catch (Throwable e) {
			prtlnErr("Error getDate(): " + e);
			return "";
		}
	}


	/**
	 *  Gets the annotation date as a Date object, available only for annos in verion 0.1.
	 *
	 * @return        The Date object or null if not available
	 * @deprecated    As of annotation framework v1.0, this field no longer exists.
	 */
	public Date getDateDate() {
		String t = doc.get("annodate");

		if (t == null)
			return null;

		try {
			return DateFieldTools.stringToDate(t);
		} catch (Throwable e) {
			prtlnErr("Error getDateDate(): " + e);
			return null;
		}
	}


	/**
	 *  True if the annotation that is part of the DRC, false otherwise.
	 *
	 * @return    The String 'true' or 'false'.
	 */
	public String getIsPartOfDrc() {
		if (isPartOfDrc())
			return "true";
		else
			return "false";
	}


	/**
	 *  True if the annotation that is part of the DRC, false otherwise.
	 *
	 * @return    The partOfDrc value
	 */
	public boolean isPartOfDrc() {
		String t = doc.get("annoispartofdrc");

		if (t == null || t.equals("false"))
			return false;
		else
			return true;
	}


	/**
	 *  Determines whether the annotation status is 'completed'.
	 *
	 * @return    True if status is 'completed'
	 */
	public boolean getIsCompleted() {
		return (getStatus().indexOf("completed") != -1);
	}


	/**
	 *  Determines whether the annotation status is 'in progress'.
	 *
	 * @return    True if status is 'in progress'
	 */
	public boolean getIsInProgress() {
		return (getStatus().indexOf("in progress") != -1);
	}


	/**
	 *  Determines whether the annotation status is 'retired'.
	 *
	 * @return    True if status is 'retired'
	 */
	public boolean getIsRetired() {
		return (getStatus().indexOf("retired") != -1);
	}



	/**  Constructor for the DleseAnnoDocReader object */
	public DleseAnnoDocReader() { }


	/**
	 *  Constructor that may be used programatically to wrap a reader around a Lucene {@link
	 *  org.apache.lucene.document.Document} that was created by a {@link
	 *  org.dlese.dpc.index.writer.FileIndexingServiceWriter}.
	 *
	 * @param  doc  A Lucene {@link org.apache.lucene.document.Document} created by a {@link
	 *      org.dlese.dpc.index.writer.ItemFileIndexingWriter}.
	 */
	public DleseAnnoDocReader(Document doc) {
		super(doc);
	}

}


