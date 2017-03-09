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
package org.dlese.dpc.repository;

import java.io.*;
import java.util.*;
import java.text.*;

import org.dlese.dpc.xml.*;
import org.dlese.dpc.index.*;
import org.dlese.dpc.util.*;
import org.apache.lucene.document.*;
import org.dlese.dpc.index.reader.*;

/**
 *  Holds data related to a single item-level record such as it's annotations and records
 *  that refer to the same resource. This class is used by class {@link
 *  org.dlese.dpc.index.writer.ItemFileIndexingWriter} and {@link
 *  org.dlese.dpc.index.reader.ItemDocReader}.
 *
 * @author    John Weatherley
 * @see       org.dlese.dpc.index.writer.ItemFileIndexingWriter
 * @see       org.dlese.dpc.index.reader.ItemDocReader
 * @see       RecordDataService
 */
public class RecordData {

	private static boolean debug = true;
	private DleseAnnoDocReader[] annoDocs = null;
	private ItemDocReader[] associatedItemsDocs = null;
	private String[] associatedIds = null;
	private ArrayList annoPathways = null;
	private ArrayList annoTypes = null;
	private String accessionStatus = null;


	/**
	 *  Contruct the record data for an item-level record. Created by {@link
	 *  RecordDataService}.
	 *
	 * @param  annoDocs             DleseAnnoDocReaders for records that annotate this item.
	 * @param  associatedItemsDocs  ItemDocReaders for items that refer to the same
	 *      resource.
	 * @param  accessionStatus      The accession status.
	 * @see                         RecordDataService
	 */
	protected RecordData(
	                     DleseAnnoDocReader[] annoDocs,
	                     ItemDocReader[] associatedItemsDocs,
	                     String accessionStatus) {
		this.annoDocs = annoDocs;
		this.associatedItemsDocs = associatedItemsDocs;
		this.accessionStatus = accessionStatus;
	}


	/**
	 *  Gets the accessionStatus attribute of the RecordData object
	 *
	 * @return    The accessionStatus value
	 */
	public String getAccessionStatus() {
		return accessionStatus;
	}


	/**
	 *  Gets DocReaders for all item-level records that refer to the same resource, or null
	 *  if none exist.
	 *
	 * @return    The asociatedItemsDocs value or null.
	 */
	public ItemDocReader[] getAsociatedItemsDocs() {
		return associatedItemsDocs;
	}


	/**
	 *  Gets DocReaders for all annotation records that reference this resource, or null if
	 *  none exist.
	 *
	 * @return    The annotationDocs value or null.
	 */
	public DleseAnnoDocReader[] getAnnotationDocs() {
		return annoDocs;
	}


	/**
	 *  Gets all annotataion types in the index that refer to this record, or null if none
	 *  exist.
	 *
	 * @return    The annoTypes value or null.
	 */
	/* public ArrayList getAnnoTypeszzz() {
		if (annoDocs == null || annoDocs.length == 0)
			return null;

		if (annoTypes != null)
			return annoTypes;

		annoTypes = new ArrayList(annoDocs.length);
		String type;
		for (int i = 0; i < annoDocs.length; i++) {
			type = annoDocs[i].getType();
			if (!annoTypes.contains(type))
				annoTypes.add(type);
		}
		return annoTypes;
	} */



	/**
	 *  Gets all annotataion pathways in the index that refer to this record, or null if none
	 *  exist.
	 *
	 * @return    The annoPathways value or null.
	 */
	/* public ArrayList getAnnoPathwayszzz() {
		if (annoDocs == null || annoDocs.length == 0)
			return null;

		if (annoPathways != null)
			return annoPathways;

		annoPathways = new ArrayList(annoDocs.length);
		String pathway;
		for (int i = 0; i < annoDocs.length; i++) {
			pathway = annoDocs[i].getPathway();
			if (!annoPathways.contains(pathway))
				annoPathways.add(pathway);
		}
		return annoPathways;
	} */


	/**
	 *  Gets the associatedIDs for this record, records that catalog the same resource.
	 *  Returns only IDs for those records that are currently in the index.
	 *
	 * @return    The associatedIDs value.
	 */
	public String[] getAssociatedIds() {
		if (associatedItemsDocs == null || associatedItemsDocs.length == 0)
			return null;
		if (associatedIds != null)
			return associatedIds;

		associatedIds = new String[associatedItemsDocs.length];
		for (int i = 0; i < associatedItemsDocs.length; i++)
			associatedIds[i] = associatedItemsDocs[i].getId();

		return associatedIds;
	}



	// ---------------- Debug methods --------------------

	/**
	 *  Return a string for the current time and date, sutiable for display in log files and
	 *  output to standout:
	 *
	 * @return    The dateStamp value
	 */
	protected final static String getDateStamp() {
		return
			new SimpleDateFormat("MMM d, yyyy h:mm:ss a zzz").format(new Date());
	}


	/**
	 *  Output a line of text to error out, with datestamp.
	 *
	 * @param  s  The text that will be output to error out.
	 */
	protected final void prtlnErr(String s) {
		System.err.println(getDateStamp() + " " + s);
	}



	/**
	 *  Output a line of text to standard out, with datestamp, if debug is set to true.
	 *
	 * @param  s  The String that will be output.
	 */
	protected final void prtln(String s) {
		if (debug) {
			System.out.println(getDateStamp() + " " + s);
		}
	}


	/**
	 *  Sets the debug attribute of the object
	 *
	 * @param  db  The new debug value
	 */
	public static void setDebug(boolean db) {
		debug = db;
	}
}

