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

import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.queryParser.QueryParser;

import java.util.*;

/**
 *  Class to perform searches and cache the results as well as the query and
 *  sort information. Used to provide access to search results from different
 *  JSP pages (e.g., search, view).
 *
 *@author    Jonathan Ostwald
 */
public class SearchHelper {
	private static boolean debug = false;
	private ResultDocList searchResults = null;
	private SimpleLuceneIndex index;

	private String currentRecId;
	private int currentRecIndex = -1;
	private int cachedRecIndex = -1;

	private long lastIndexMod = -1;

	private SortSpec cachedSort = null;
	private Query cachedQuery = null;

	private final ResultDocList EMPTY_SEARCH_RESULTS = new ResultDocList();


	/**
	 *  Constructor for the SearchHelper object
	 *
	 *@param  index  NOT YET DOCUMENTED
	 */
	public SearchHelper(SimpleLuceneIndex index) {
		this.index = index;
		searchResults = EMPTY_SEARCH_RESULTS;
	}


	/**
	 *  Returns the current searchResults, doing a fresh search and sort if the
	 *  index has changed since the searchResults were calculated.<p>
	 *
	 *  When do we FORCE a new search?? - during sort (where the sort may have
	 *  changed, but the index has not, so search would not have otherwise have
	 *  been performed. NOTE: we could also force search by reseting the
	 *  lastIndexMod to -1!<p>
	 *
	 *  RETHINK this logic. why cache anything? why not just return the
	 *  searchResults or an empty list??
	 *
	 *@return    The results value
	 */
	public ResultDocList getResults() {
		if (this.isNewIndex()) {
			if (this.cachedQuery == null) {
				searchResults = EMPTY_SEARCH_RESULTS;
			} else {
				// search will wipe out cashedSort, so we have to save it and then re-sort.
				prtln("\t cached query: " + this.cachedQuery);

				// THE CACHEDSORT SHOULD BE A LUCENEQUERY (INCLUDING SORT)

				searchResults = this.search(this.cachedQuery, this.cachedSort);
			}
		}

		if (searchResults == null) {
			searchResults = EMPTY_SEARCH_RESULTS;
		}
		return searchResults;
	}

	/**
	 *  Converts sortSpec (containing sort field and sort order) into a Lucene Sort
	 *  object.
	 *
	 *@param  sortSpec  the SortSpec instance to be converted
	 *@return           Description of the Return Value
	 */
	Sort sortSpecToSortObj(SortSpec sortSpec) {
		if (sortSpec == null) {
			return null;
		}
		boolean reverse = sortSpec.order == Constants.DESCENDING;
		return new Sort(new SortField(sortSpec.field, SortField.STRING, reverse));
	}

	/**
	 *  Return the results of search with the provided query, performing new search
	 *  only if a new query string is provided or if the index has changed since
	 *  the last search.<p>
	 *
	 *
	 *
	 *@param  query  the lucene Query
	 *@return        search results
	 */
	public ResultDocList search(Query query) {
		return search(query, null);
	}


	/*
	 *  sort obj can be either a SortSpec or SortWidget instance
	 */
	/**
	 *  Description of the Method
	 *
	 *@param  query    Description of the Parameter
	 *@param  sortObj  Description of the Parameter
	 *@return          Description of the Return Value
	 */
	public ResultDocList search(Query query, Object sortObj) {
		prtln("\nSEARCH");
		SortSpec sortSpec = null;
		if (sortObj == null) {
			sortSpec = null;
		} else if (sortObj instanceof SortSpec) {
			sortSpec = (SortSpec) sortObj;
		} else if (sortObj instanceof SortWidget) {
			sortSpec = new SortSpec((SortWidget) sortObj);
		} else {
			prtln("WARNING: unrecognized sortObj class: " + sortObj.getClass().getName());
		}

		if (sortSpec == null) {
			prtln("  no sort spec provided");
		} else {
			prtln("   sortSpec: field: " + sortSpec.field + ", order: " + sortSpec.order);
		}

		/*
		 *  prtln("\nSEARCH");
		 *  prtln ("\t new Query ? " + isNewQuery(query));
		 *  prtln ("\t new Index ? " + isNewIndex());
		 */
		long start = new Date().getTime();

		if (query == null) {
			// prtln("no query provided");
			this.searchResults = this.EMPTY_SEARCH_RESULTS;
		} else {
			Sort sort = sortSpecToSortObj(sortSpec);
			// prtln("\t doing NEW search");
			this.searchResults = index.searchDocs(query, null, sort, null);
		}
		this.lastIndexMod = index.getLastModifiedCount();
		this.cachedQuery = query;
		currentRecIndex = -1;
		if (sortSpec != null) {
			this.cachedSort = sortSpec;
		} else {
			this.cachedSort = null;
		}

		String msg = "  search took " + (new Date().getTime() - start) + " millis";
		if (debug)
			prtlnBox(msg);

		// this.showSearchResultsHits();

		return this.getResults();
	}


	/**
	 *  Returns all the searchResults
	 *
	 *@return    The hits value
	 */
	public List getHits() {
		return getHits(0, getNumHits());
	}


	/**
	 *  Returns a range of searchResults
	 *
	 *@param  start   beginning index
	 *@param  length  length of range
	 *@return         List of records from specified starting index
	 */
	public List getHits(int start, int length) {

		int end = Math.min(this.getNumHits(), start + length);
		return this.getResults().subList(start, end);
	}


	/**
	 *  gets the index in the searchResults for the record having provided id.
	 *
	 *@param  recId  a record id
	 *@return        the index, or -1 if no record is found
	 */
	public int getIndexOf(String recId) {
		ResultDocList results = this.getResults();
		for (int i = 0; i < results.size(); i++) {
			if (getIdFromResultDoc(results.get(i)).equals(recId)) {
				return i;
			}
		}
		return -1;
	}



	/**
	 *  Returns true if there are no searchResults
	 *
	 *@return    The empty value
	 */
	public boolean isEmpty() {
		return this.getNumHits() == 0;
	}


	/**
	 *  Convienence caller of isEmpty for jsp pages.
	 *
	 *@return    returns true if there are no search results.
	 */
	public boolean getIsEmpty() {
		return this.isEmpty();
	}


	/**
	 *  Gets the number of searchResult items
	 *
	 *@return    The numHits value
	 */
	public int getNumHits() {
		return this.getResults().size();
	}



	/**
	 *  gets the Cached current record id
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
		currentRecIndex = -1;// force currentRecIndex to be recomputed
		prtln("\nCurrentRecId set to " + id);
	}


	/**
	 *  Sets the cachedRecInex attribute of the SearchHelper object
	 *
	 *@param  i  The new cachedRecInex value
	 */
	private void setCachedRecInex(int i) {
		this.cachedRecIndex = i;
		prtln("\n cachedRecId set to " + i);
	}


	/**
	 *  Gets the id of the result at specifiec index of search results.
	 *
	 *@param  recIndex  index into search results
	 *@return           the id or null if not found
	 */
	public String getRecId(int recIndex) {
		if (recIndex > -1 && this.getNumHits() >= (recIndex + 1)) {
			return getIdFromResultDoc(this.getResults().get(recIndex));
		} else {
			prtlnErr("getRecId error: requested index: " + recIndex + " size: " + this.getNumHits());
			return null;
		}
	}


	/**
	 *  Return the index of the current record in the searchResults
	 *
	 *@return    The currentRecIndex value
	 */
	public int getCurrentRecIndex() {

		if (currentRecIndex == -1) {

			if (currentRecId == null) {
				// prtln ("..... getCurrentRecIndex currentRecId is null, returning -1");
				return currentRecIndex;
			}

			prtln("\n ** getCurrentRecIndex searching for currentRecIndex");
			long start = new Date().getTime();

			// sort will perform a new search && sort if the index has changed
			ResultDocList results = this.getResults();
			prtln("\t searching over " + results.size());
			for (int i = 0; i < results.size(); i++) {
				// prtln ("\t (" + i + " id: " + getIdFromResultDoc(results[i]));
				if (getIdFromResultDoc(results.get(i)).equals(currentRecId)) {
					currentRecIndex = i;
					currentRecId = this.getRecId(i);
					prtln("set currentRecIndex to: " + currentRecIndex);
					prtln("set currentRecId to: " + currentRecId);
					break;
				}
			}

			// if we haven't found the currentRec, should we set the currentRecId to null??
			if (currentRecIndex == -1) {
				currentRecId = null;
			} else {
				this.setCachedRecInex(currentRecIndex);
			}

			prtln("   ... search for currentRecIndex took " + (new Date().getTime() - start) + " millis");
		}

		return currentRecIndex;
	}


	/**
	 *  Gets the cachedRecIndex attribute of the SearchHelper object
	 *
	 *@return    The cachedRecIndex value
	 */
	public int getCachedRecIndex() {
		return cachedRecIndex;
	}


	/**
	 *  Sets the cachedRecIndex attribute of the SearchHelper object
	 *
	 *@param  i  The new cachedRecIndex value
	 */
	public void setCachedRecIndex(int i) {
		cachedRecIndex = i;
	}


	/**
	 *  Sets the results to the single resultDoc provided
	 *
	 *@param  resultDoc  The new results value
	 */
	public void setResults(ResultDoc resultDoc) {
		this.searchResults = new ResultDocList(new ResultDoc[]{resultDoc});
		this.lastIndexMod = index.getLastModifiedCount();
		currentRecIndex = -1;
	}


	/**
	 *  Gets the ResultDoc for given record id from the index, returning null if a
	 *  result is not found.<p>
	 *
	 *  If more than one result is found (this should not happen), print a message
	 *  and return the first result.
	 *
	 *@param  id  record ID
	 *@return     The resultDoc value or null if resultDoc is not found.
	 */
	public ResultDoc getResultDoc(String id) {
		if (id == null || id.trim().length() == 0) {
			return null;
		}
		ResultDoc[] resultDocs =
				index.searchDocs("id:" + SimpleLuceneIndex.encodeToTerm(id)).toArray();

		if (resultDocs == null) {
			prtln("\tgetResultDoc() - nothing found for id: " + id);
			return null;
		}

		if (resultDocs.length > 1) {
			prtlnErr("Error: more than one item in index for id '" + id + "'");
		}
		if (resultDocs.length > 0) {
			return resultDocs[0];
		} else {
			return null;
		}
	}

	// ---------- predicates ---------------------

	/**
	 *  Compares provided sortWidget to the cached sortSpec.
	 *
	 *@param  sortWidget  SortWidget to compare to cached sort
	 *@return             true if cashedSort is not null and field or order of
	 *      cashed sort differs from that of provided SortWidget.
	 */
	private boolean isNewSort(SortWidget sortWidget) {
		return (this.cachedSort == null || this.cachedSort.isNewSort(sortWidget));
	}


	/**
	 *  Gets the newSortField attribute of the SearchHelper object
	 *
	 *@param  sortWidget  NOT YET DOCUMENTED
	 *@return             The newSortField value
	 */
	private boolean isNewSortField(SortWidget sortWidget) {
		return (this.cachedSort == null || this.cachedSort.isNewSortField(sortWidget));
	}


	/**
	 *  Compares given query string to cached query string.
	 *
	 *@param  query  Description of the Parameter
	 *@return        Returns true if provided query is different than current
	 *      query.
	 */
	private boolean isNewQuery(Query query) {
		if (query == null) {
			return this.cachedQuery != query;
		}
		return !query.equals(this.cachedQuery);
	}


	/**
	 *  Determines whether the index has changed since the last search operation
	 *  was performed.
	 *
	 *@return    true if the index has changed.
	 */
	private boolean isNewIndex() {
		return (this.lastIndexMod < index.getLastModifiedCount());
	}


	// -------- utility methods ------------

	/**
	 *  Gets the id of the record held by the provided ResultDoc.
	 *
	 *@param  resultDoc  the resultDoc
	 *@return            id of the metadata held in the resultDoc
	 */
	private String getIdFromResultDoc(ResultDoc resultDoc) {
		XMLDocReader docReader = (XMLDocReader) resultDoc.getDocReader();
		return docReader.getId();
	}


	/**
	 *  Debugging
	 */
	private void showSearchResultsHits() {
		int n = 50;
		prtln("SEARCH RESULT LIST");
		if (this.isEmpty()) {
			prtln("\t EMPTY");
			return;
		}
		for (Iterator i = getHits(0, n).iterator(); i.hasNext(); ) {
			ResultDoc resultDoc = (ResultDoc) i.next();
			prtln("\t" + getIdFromResultDoc(resultDoc));
		}
	}


	/**
	 *  Print a line to standard out.
	 *
	 *@param  s  The String to print.
	 */
	private static void prtln(String s) {
		if (debug) {
			// SchemEditUtils.prtln(s, "SearchHelper");
			SchemEditUtils.prtln(s, "");
		}
	}



	/**
	 *  NOT YET DOCUMENTED
	 *
	 *@param  s  NOT YET DOCUMENTED
	 */
	private static void prtlnErr(String s) {
		SchemEditUtils.prtln(s, "SearchHelper");
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 *@param  s  NOT YET DOCUMENTED
	 */
	private void prtlnBox(String s) {
		SchemEditUtils.box(s);
	}


	/**
	 *  Stores information necessary to specify a sort (i.e., field and order),
	 *  used to cache information so a query can be re-executed if necessary (e.g.,
	 *  if the index has changed we want to re-execute a query so the user sees the
	 *  most current information.
	 *
	 *@author    ostwald
	 */
	private class SortSpec {
		private SortWidget widget = null;
		/**
		 *  either 0 or 1
		 */
		public int order;
		/**
		 *  must be the name of an indexed field
		 */
		public String field;


		/**
		 *  Constructor for the SortSpec object
		 *
		 *@param  field  sort field
		 *@param  order  sort order
		 */
		public SortSpec(String field, int order) {
			this.field = field;
			this.order = order;
		}


		/**
		 *  Constructor for the SortSpec object from a sortWidget.
		 *
		 *@param  widget  the sortWidget
		 */
		public SortSpec(SortWidget widget) {
			this.widget = widget;
			this.order = widget.getOrder();
			this.field = widget.getFieldName();
		}


		/**
		 *  Compares the order and field attributes of the provided sortWidget to the
		 *  corresponding fields of this sortSpec.
		 *
		 *@param  widget  the sortWidget
		 *@return         true if the provided sortWidget specifies a sort other than
		 *      this sortSpec
		 */
		public boolean isNewSort(SortWidget widget) {
			if (widget == null) {
				return false;
			}
			return (widget.getOrder() != this.order ||
					!widget.getFieldName().equals(this.field));
		}


		/**
		 *  Returns true if the provided widget specifies a different sort field than
		 *  the sort field of this SortSpec.
		 *
		 *@param  widget  provided SortWidget
		 *@return         true if provided sortWidget is not null and sort fields do
		 *      not match
		 */
		public boolean isNewSortField(SortWidget widget) {
			if (widget == null) {
				return false;
			}
			return (!widget.getFieldName().equals(this.field));
		}


		/**
		 *  Gets the widget attribute of the SortSpec object
		 *
		 *@return    The widget value
		 */
		public SortWidget getWidget() {
			return this.widget;
		}
	}
}


