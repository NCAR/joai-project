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
package org.dlese.dpc.schemedit;

import org.dlese.dpc.index.SimpleLuceneIndex;
import org.dlese.dpc.index.ResultDoc;
import org.dlese.dpc.index.ResultDocList;
import org.dlese.dpc.index.reader.XMLDocReader;
import org.dlese.dpc.schemedit.display.SortWidget;

import java.util.*;

/**
 *  Class to manage a list of records (e.g. search results) as a list of RECORD
 *  IDS, rather than as ResultDoc arrays. We don't want to keep sets of
 *  ResultDocs around because they take up memory and also because they can
 *  become stale over time.
 *
 *@author    ostwald
 */
public class RecordList {

	private static boolean debug = false;
	private List items;
	private String query;
	private SimpleLuceneIndex index;
	private String currentRecId;


	/**
	 *  Constructor for the RecordList object
	 */
	public RecordList() {
		items = new ArrayList();
		index = null;
		query = null;
	}


	/**
	 *  Constructor for the RecordList object
	 *
	 *@param  index  the index
	 */
	public RecordList(SimpleLuceneIndex index) {
		this();
		this.index = index;
	}


	/**
	 *  Contructor for the RecordList object given a query string and a lucene
	 *  index
	 *
	 *@param  query  the query
	 *@param  index  a lucene index
	 */
	public RecordList(String query, SimpleLuceneIndex index) {
		this(index);
		this.query = query;
		this.items = this.getIdList(query, index);
	}


	/**
	 *  Constructor for the RecordList object given a ResultDocList and a
	 *  SimpleLuceneIndex. The record IDs are extracted from ResultDocList items
	 *  and stored.
	 *
	 *@param  resultDocs  the resultDocList
	 *@param  index       the index
	 */
	public RecordList(ResultDocList resultDocs, SimpleLuceneIndex index) {
		this(index);
		items = resultDocsToIdList(resultDocs);
		prtln("RecordList contructed from a ResultDocList");
	}


	/**
	 *  Constructor for the RecordList object given an array of ids<p>
	 *
	 *  BatchOperations.handleRemoveRecords() uses this method for the values
	 *  returned by request.getParameterValues().
	 *
	 *@param  ids    An array of RecordIds
	 *@param  index  the index
	 */

	public RecordList(String[] ids, SimpleLuceneIndex index) {
		this(index);

		if (ids != null && ids.length > 0) {
			for (int i = 0; i < ids.length; i++) {
				add(ids[i]);
			}
		}
		prtln("RecordList contructed from an array of ids");
	}


	/**
	 *  The number of items
	 *
	 *@return    the number of ids (items) managed by this RecordList
	 */
	public int size() {
		return getItems().size();
	}


	/**
	 *  Gets the size attribute of the RecordList object
	 *
	 *@return    The size value
	 */
	public int getSize() {
		return getItems().size();
	}


	/**
	 *  Returns an iterator for the current record list.
	 *
	 *@return    an iterator over this recordList's ids
	 */
	public Iterator iterator() {
		return getItems().iterator();
	}


	/**
	 *  returns true of the provided id is managed by this RecordList
	 *
	 *@param  id  id to check against items
	 *@return     true if this RecordList's items contains specified id
	 */
	public boolean contains(String id) {
		return getItems().contains(id);
	}


	/**
	 *  Add the id to this RecordLists's items
	 *
	 *@param  id  id to add
	 */
	public void add(String id) {
		items.add(id);
	}


	/**
	 *  Gets the empty attribute of the RecordList object
	 *
	 *@return    The empty value
	 */
	public boolean isEmpty() {
		return getItems().isEmpty();
	}


	/**
	 *  Gets the isEmpty attribute of the RecordList object
	 *
	 *@return    The isEmpty value
	 */
	public boolean getIsEmpty() {
		return isEmpty();
	}


	/**
	 *  Returns the index of the specified id, or -1 if it is not managed by this
	 *  RecordList
	 *
	 *@param  id  a record id to find the index of in this ResultList
	 *@return     The indexOf value
	 */
	public int getIndexOf(String id) {
		return getItems().indexOf(id);
	}


	/**
	 *  List of record ids returned by the last query.
	 *
	 *@return    The items value
	 */
	public List getItems() {
		if (items == null) {
			items = new ArrayList();
		}
		return items;
	}


	/**
	 *  Sets the index attribute of the RecordList object
	 *
	 *@param  index  The new index value
	 */
	public void setIndex(SimpleLuceneIndex index) {
		this.index = index;
	}


	/**
	 *  Returns the records as a list of ResultDocs
	 *
	 *@return    The hits value
	 */
	public List getHits() {
		return getHits(0, size());
	}


	/**
	 *  Returns a list of recordDocs for the specified range.
	 *
	 *@param  start   beginning index
	 *@param  length  length of range
	 *@return         The hits value
	 */
	public List getHits(int start, int length) {
		// prtln ("\n ** getHits () start: " + start + " , length: " + length);
		List hits = new ArrayList();
		for (int i = start; i < (start + length) && i < this.size(); i++) {
			String id = getRecId(i);
			if (id != null) {
				hits.add(getResultDoc(id));
			}
		}
		return hits;
	}


	/**
	 *  Id of the current record.
	 *
	 *@return    The currentRecId value
	 */
	public String getCurrentRecId() {
		return currentRecId;
	}


	/**
	 *  Sets the currentRecId attribute of the RecordList object
	 *
	 *@param  id  The new currentRecId value
	 */
	public void setCurrentRecId(String id) {
		currentRecId = id;
	}


	/**
	 *  Index of the current record in the result list.
	 *
	 *@return    The currentRecIndex value
	 */
	public int getCurrentRecIndex() {
		if (currentRecId == null) {
			return -1;
		}
		return getItems().indexOf(currentRecId);
	}


	/**
	 *  Gets the current record as a ResultDoc instance.
	 *
	 *@return    The currentResult value
	 */
	public ResultDoc getCurrentResult() {
		return getResultDoc(currentRecId);
	}


	/**
	 *  Gets the ResultDoc given a record id via the index.
	 *
	 *@param  id  the id for which to obtain a ResultDoc
	 *@return     The resultDoc value
	 */
	public ResultDoc getResultDoc(String id) {
		return getResultDoc(id, index);
	}


	/**
	 *  Gets the resultDoc for a particular record by searching the index.
	 *
	 *@param  id     record id
	 *@param  index  the index
	 *@return        The resultDoc value
	 */
	public ResultDoc getResultDoc(String id, SimpleLuceneIndex index) {
		prtln("getResultDoc() with id = " + id);
		if (index != null && id != null) {
			ResultDocList resultDocs = index.searchDocs("id:" + SimpleLuceneIndex.encodeToTerm(id));
			if (resultDocs != null && resultDocs.size() > 0) {
				prtln("\t" + resultDocs.size() + " docs found");
				return resultDocs.get(0);
			}
		}
		prtln("..  returning NULL");
		return null;
	}


	/**
	 *  Returns a list containing the ids of the provided ResultDocList
	 *
	 *@param  resultDocs  search results
	 *@return             a list of ids
	 */
	private List resultDocsToIdList(ResultDocList resultDocs) {
		List idList = new ArrayList();
		String msg = "RecordList.resultDocsToIdList\n  building ID list from " + resultDocs.size() +
				" resultDocs ..\n";

		long start = new Date().getTime();
		if (resultDocs != null && resultDocs.size() > 0) {
			for (int i = 0; i < resultDocs.size(); i++) {
				XMLDocReader docReader = (XMLDocReader) resultDocs.get(i).getDocReader();
				idList.add(docReader.getId());
			}
		}
		msg += "  " + (new Date().getTime() - start) + " milliseconds";
		if (debug)
			SchemEditUtils.box(msg);

		return idList;
	}


	/**
	 *  Returns a list of ids that is constructed by querying the index and then
	 *  extracting only the record id from the results.
	 *
	 *@param  query  search query
	 *@param  index  the index to be searched
	 *@return        a list containing search result ids
	 */
	private List getIdList(String query, SimpleLuceneIndex index) {
		if (index == null) {
			prtln ("WARNING: getIdList called without an index to search");
			return new ArrayList();
		}
		return resultDocsToIdList(index.searchDocs(query));
	}


	/**
	 *  Gets the recId attribute of the RecordList object
	 *
	 *@param  recIndex  NOT YET DOCUMENTED
	 *@return           The recId value
	 */
	public String getRecId(int recIndex) {
		try {
			return (String) getItems().get(recIndex);
		} catch (IndexOutOfBoundsException e) {
			prtln("WARNING: getRecId got IndexOutOfBoundsException for \"" + recIndex + "\"");
			e.printStackTrace();
			return null;
		}
	}


	/**
	 *  Print a line to standard out.
	 *
	 *@param  s  The String to print.
	 */
	private void prtln(String s) {
		if (debug) {
			SchemEditUtils.prtln(s, "RecordList");
		}
	}
}

