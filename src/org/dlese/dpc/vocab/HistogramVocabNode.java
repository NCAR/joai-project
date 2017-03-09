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
package org.dlese.dpc.vocab;
import org.dlese.dpc.repository.*;
import org.dlese.dpc.index.reader.*;
import org.dlese.dpc.index.*;
import org.dlese.dpc.dds.action.DDSQueryAction;
import org.dlese.dpc.dds.action.form.HistogramForm;
import java.util.*;

/**
 *  Wrapper for a plain VocabNode that ties to a Lucene index for generating
 *  vocab-based totals
 *
 * @author    Ryan Deardorff
 */
public class HistogramVocabNode {

	private HistogramForm histogramForm;
	private VocabNode vocabNode;
	private String libraryTotal = "";                              // Total resources matching this vocab
	private int largestTotalInThisGroup = 0;                       // Histogram bars are sized relative to this
	private boolean isCollection;                                  // Is this histogram a specific collection?
	private String collection = null;                              // ...if so, which one?
	private boolean hasSubtotalsGreaterThanZero = false;
	private boolean isLastInSublist = false;
	private int PERCENT_ADJUST = 80;
	static HashMap indexTotals = new HashMap();                    // cache to reduce # of index queries generated

	/**
	 *  Sets the collection attribute of the HistogramVocabNode object
	 *
	 * @param  collection  The new collection value
	 */
	public void setCollection( String collection ) {
		this.collection = collection;
	}

	/**
	 *  Gets the histogramQuery attribute of the HistogramVocabNode object
	 *
	 * @return    The histogramQuery value
	 */
	public String getHistogramQuery() {
		String addCollection = "";
		if ( collection != null && !collection.equals( "0*" ) ) {
			addCollection = "ky_" + collection + "_";
		}
		return "/library/browse_" + addCollection + vocabNode.getFieldId() + "_" + vocabNode.getId() + ".htm";
	}

	/**
	 *  Sets the largestTotalInThisGroup attribute of the HistogramVocabNode object
	 *
	 * @param  total  The new largestTotalInThisGroup value
	 */
	public void setLargestTotalInThisGroup( String total ) {
		largestTotalInThisGroup = Integer.parseInt( total );
	}

	/**
	 *  Percentages are adjusted to account for extra UI space (in the browse case,
	 *  the total number displayed next to the percentage image bar). This can be
	 *  set to 100 to eliminate that behavior, or adjusted for UIs that may need
	 *  more space.
	 *
	 * @param  perc  Percentage multiplier (1-100)
	 */
	public void setPercentAdjust( int perc ) {
		PERCENT_ADJUST = perc;
	}

	/**
	 *  Gets the vocabTotalBar attribute of the HistogramVocabNode object
	 *
	 * @return    The vocabTotalBar value
	 */
	public String getVocabTotalBarPercent() {
		float percent = (float)( (float)Integer.parseInt( libraryTotal ) / (float)largestTotalInThisGroup ) *
			PERCENT_ADJUST;
		String perc = Float.toString( percent );
		int ind = perc.indexOf( "." );
		if ( ind > -1 ) {
			perc = perc.substring( 0, ind );
		}
		return perc;
	}

	/**
	 *  Constructor for the HistogramVocabNode object
	 *
	 * @param  vocabNode
	 * @param  histogramForm  Description of the Parameter
	 * @param  isCollection   Description of the Parameter
	 */
	public HistogramVocabNode( VocabNode vocabNode, HistogramForm histogramForm, boolean isCollection ) {
		this.vocabNode = vocabNode;
		this.histogramForm = histogramForm;
		this.isCollection = isCollection;
	}

	/**
	 *  Set the total for this vocab value, with added query (for things like
	 *  collection)
	 *
	 * @param  index     Lucene index
	 * @param  rm        repository manager
	 * @param  addQuery  added to query that gets the total for this node
	 * @return
	 */
	public int setLibraryTotal( SimpleLuceneIndex index, RepositoryManager rm, String addQuery ) {
		int ret = 0;
		if ( vocabNode.getList().item.size() == 0 ) {                 // Sub-headers are not currently tallied
			String addedQuery = "";
			if ( vocabNode.getFieldId().equals( "ky" ) ) {
				addedQuery = addQuery + " AND " + DDSQueryAction.getCollectionQueryTerm( vocabNode.getId() );
			}
			else if ( vocabNode.getId() != null ) {
				addedQuery = addQuery + " AND " + vocabNode.getFieldId() + ":" + vocabNode.getId();
			}
			if ( vocabNode.getDivider() ) {
				ret = 0;
			}
			else {
				if ( indexTotals.get( addedQuery ) != null ) {
					ret = ( (Integer)indexTotals.get( addedQuery ) ).intValue();
				}
				else {
					ret = index.getNumDocs( rm.getDiscoverableItemsQuery() + addedQuery );
					indexTotals.put( addedQuery, new Integer( ret ) );
				}
			}
			libraryTotal = new Integer( ret ).toString();
		}
		return ret;
	}

	/**
	 *  Clear the cache that holds the totals for histogram nodes
	 */
	public static void clearCache() {
		indexTotals.clear();
	}

	/**
	 *  Sets the libraryTotal attribute of the HistogramVocabNode object
	 *
	 * @param  total  The new libraryTotal value
	 */
	public void setLibraryTotal( int total ) {
		libraryTotal = new Integer( total ).toString();
	}

	/**
	 *  Gets the libraryTotal attribute of the HistogramVocabNode object
	 *
	 * @return    The libraryTotal value
	 */
	public String getLibraryTotal() {
		if ( isCollection ) {
			// This is a collections node, so let the histogram form know which collection is
			// being inspected (so it can add it to other vocab criteria):
			histogramForm.setCollection( vocabNode.getId() );
		}
		if ( vocabNode.getList().item.size() > 0 ) {                  // Sub-headers are not currently tallied
			return "-1";
		}
		return libraryTotal;
	}

	/**
	 *  If this node is a sub-header, does it contain sub-nodes that have a total
	 *  number of resources > 0?
	 *
	 * @return
	 */
	public boolean hasSubtotalsGreaterThanZero() {
		return hasSubtotalsGreaterThanZero;
	}

	/**
	 *  Sets the hasSubtotalsGreaterThanZero attribute of the HistogramVocabNode
	 *  object
	 *
	 * @param  hasSubtotalsGreaterThanZero  The new hasSubtotalsGreaterThanZero
	 *      value
	 */
	public void setHasSubtotalsGreaterThanZero( boolean hasSubtotalsGreaterThanZero ) {
		this.hasSubtotalsGreaterThanZero = hasSubtotalsGreaterThanZero;
	}

	/**
	 *  Gets the hasSubtotalsGreaterThanZero attribute of the HistogramVocabNode
	 *  object
	 *
	 * @return    The hasSubtotalsGreaterThanZero value
	 */
	public String getHasSubtotalsGreaterThanZero() {
		if ( hasSubtotalsGreaterThanZero ) {
			return "true";
		}
		return "false";
	}

	/**
	 *  Gets the lastInSublist attribute of the HistogramVocabNode object
	 *
	 * @return    The lastInSublist value
	 */
	public boolean isLastInSublist() {
		return isLastInSublist;
	}

	/**
	 *  Sets the isLastInSublist attribute of the HistogramVocabNode object
	 *
	 * @param  isLastInSublist  The new isLastInSublist value
	 */
	public void setIsLastInSublist( boolean isLastInSublist ) {
		this.isLastInSublist = isLastInSublist;
	}

	/**
	 *  Gets the isLastInSublist attribute of the HistogramVocabNode object
	 *
	 * @return    The isLastInSublist value
	 */
	public String getIsLastInSublist() {
		if ( isLastInSublist ) {
			return "true";
		}
		return "false";
	}

	/**
	 *  Gets the vocabNode attribute of the HistogramVocabNode object
	 *
	 * @return    The vocabNode value
	 */
	public VocabNode getVocabNode() {
		return vocabNode;
	}
}

