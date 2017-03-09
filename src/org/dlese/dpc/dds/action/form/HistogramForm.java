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
package org.dlese.dpc.dds.action.form;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionError;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionServlet;
import org.apache.struts.util.MessageResources;

import org.dlese.dpc.dds.action.form.DDSViewResourceForm;
import org.dlese.dpc.repository.*;
import org.dlese.dpc.vocab.*;
import org.dlese.dpc.index.reader.*;
import org.dlese.dpc.index.*;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.io.*;
import java.text.*;
import java.net.URLEncoder;
import org.dlese.dpc.dds.action.DDSQueryAction;

/**
 *  A Struts Form bean for handling DDS histogram requests
 *
 * @author    Ryan Deardorff
 */
public final class HistogramForm extends ActionForm implements Serializable {

	protected String error = null;
	protected String hasCollectionSpecified = "false";
	protected String primaryRecordCollectionDescriptionPage = "";
	protected String totalQuery = "";
	protected SimpleLuceneIndex index = null;
	protected RepositoryManager rm = null;
	protected String collection = null;
	protected String collectionMetaName = "";
	protected DleseCollectionDocReader collectionDocReader = null;
	protected int largestTotal = 0;
	protected ServletContext servletContext;
	protected String currentVocabName;                             // vocab FIELD name
	protected String currentVocabGroup = "";                       // vocab UI group (top-level outline groupings in FIELD OPML)
	protected String currentVocabFramework;
	protected MetadataVocab vocab = null;

	// keep track of which index version is represented by HistogramVocabNode caching:
	protected static long histogramIndexModified = -1;

	/**
	 *  Constructor for the HistogramForm object
	 */
	public HistogramForm() { }

	/**
	 *  Sets the vocab attribute of the VocabCachingActionForm object
	 *
	 * @param  vocab  The new vocab value
	 */
	public void setVocab( MetadataVocab vocab ) {
		this.vocab = vocab;
	}

	/**
	 *  Sets the servletContext attribute of the HistogramForm object
	 *
	 * @param  servletContext  The new servletContext value
	 */
	public void setServletContext( ServletContext servletContext ) {
		this.servletContext = servletContext;
	}

	/**
	 *  Sets the currentVocab attribute of the HistogramForm object
	 *
	 * @param  currentVocabName  The new currentVocabName value
	 */
	public void setCurrentVocabName( String currentVocabName ) {
		this.currentVocabName = currentVocabName;
	}

	/**
	 *  Gets the currentVocabName attribute of the HistogramForm object
	 *
	 * @return    The currentVocabName value
	 */
	public String getCurrentVocabName() {
		return currentVocabName;
	}

	/**
	 *  Sets the currentVocabGroup attribute of the HistogramForm object
	 *
	 * @param  currentVocabGroup  The new currentVocabGroup value
	 */
	public void setCurrentVocabGroup( String currentVocabGroup ) {
		this.currentVocabGroup = currentVocabGroup;
	}

	/**
	 *  Gets the currentVocabGroup attribute of the HistogramForm object
	 *
	 * @return    The currentVocabGroup value
	 */
	public String getCurrentVocabGroup() {
		return currentVocabGroup;
	}

	/**
	 *  Sets the currentVocabFramework attribute of the HistogramForm object
	 *
	 * @param  currentVocabFramework  The new currentVocabFramework value
	 */
	public void setCurrentVocabFramework( String currentVocabFramework ) {
		this.currentVocabFramework = currentVocabFramework;
	}

	/**
	 *  Gets the largestTotal attribute of the HistogramForm object
	 *
	 * @return    The largestTotal value
	 */
	public String getLargestTotal() {
		return Integer.toString( largestTotal );
	}

	/**
	 *  Description of the Method
	 */
	public void resetLargestTotal() {
		largestTotal = 0;
	}

	/**
	 *  Sets the collection attribute of the HistogramForm object
	 *
	 * @param  collection  The new collection value
	 */
	public void setCollection( String collection ) {
		this.collection = collection;
	}

	/**
	 *  Sets the collectionMetaName attribute of the HistogramForm object
	 *
	 * @param  collectionMetaName  The new collectionMetaName value
	 */
	public void setCollectionMetaName( String collectionMetaName ) {
		this.collectionMetaName = collectionMetaName;
	}

	/**
	 *  Gets the collectionTitle attribute of the HistogramForm object
	 *
	 * @return    The collectionTitle value
	 */
	public String getCollectionTitle() {
		return vocab.getUiValueLabel( "dlese_collect",
			servletContext.getInitParameter( "metadataVocabAudience" ),
			servletContext.getInitParameter( "metadataVocabLanguage" ), "key", collection, false );
	}

	/**
	 *  Sets the hasCollectionSpecified attribute of the HistogramForm object
	 *
	 * @param  hasCollectionSpecified  The new hasCollectionSpecified value
	 */
	public void setHasCollectionSpecified( String hasCollectionSpecified ) {
		this.hasCollectionSpecified = hasCollectionSpecified;
	}

	/**
	 *  Gets the hasCollectionSpecified attribute of the HistogramForm object
	 *
	 * @return    The hasCollectionSpecified value
	 */
	public String getHasCollectionSpecified() {
		return hasCollectionSpecified;
	}

	/**
	 *  Sets the error attribute of the DDSViewResourceForm object
	 *
	 * @param  error  The new error value
	 */
	public void setError( String error ) {
		this.error = error;
	}

	/**
	 *  Gets the error attribute of the DDSViewResourceForm object
	 *
	 * @return    The error value
	 */
	public String getError() {
		return error;
	}

	/**
	 *  Sets the collection attribute of the HistogramForm object
	 *
	 * @param  collectionDocReader  The new collectionResultDoc value
	 */
	public void setCollectionDocReader( DocReader collectionDocReader ) {
		try {
			this.collectionDocReader = (DleseCollectionDocReader)collectionDocReader;
		}
		catch ( Exception e ) {
			System.out.println( "EXCEPTION: " );
			e.printStackTrace();
		}
	}

	/**
	 *  Gets the collectionDocReader attribute of the HistogramForm object
	 *
	 * @return    The collectionDocReader value
	 */
	public DleseCollectionDocReader getCollectionDocReader() {
		return collectionDocReader;
	}

	/**
	 *  Gets the collection attribute of the HistogramForm object
	 *
	 * @return    The collection value
	 */
	public ArrayList getCollections() {
		ArrayList ret = new ArrayList();
		ArrayList vocList = vocab.getVocabNodes( "dlese_collect",
			servletContext.getInitParameter( "metadataVocabAudience" ),
			servletContext.getInitParameter( "metadataVocabLanguage" ), "key" );
		VocabNode node = new VocabNodeOPML( "AllCollections" );
		node.setLabel( "Entire library" );
		node.setFieldId( "ky" );
		node.setId( "0*" );
		HistogramVocabNode newNode = new HistogramVocabNode( node, this, true );
		newNode.setLibraryTotal( index, rm, "" );
		ret.add( newNode );
		for ( int i = 0; i < vocList.size(); i++ ) {
			newNode = new HistogramVocabNode( (VocabNode)vocList.get( i ), this, true );
			newNode.setLibraryTotal( index, rm, "" );
			ret.add( newNode );
		}
		return ret;
	}

	/**
	 *  Gets the vocabList attribute of the HistogramForm object
	 *
	 * @return    The vocabList value
	 */
	public ArrayList getVocabList() {
		ArrayList vocList;
		if ( !currentVocabGroup.equals( "" ) ) {
			vocList = vocab.getVocabNodes( currentVocabFramework,
				servletContext.getInitParameter( "metadataVocabAudience" ),
				servletContext.getInitParameter( "metadataVocabLanguage" ),
				currentVocabName, currentVocabGroup );
		}
		else {
			vocList = vocab.getVocabNodes( currentVocabFramework,
				servletContext.getInitParameter( "metadataVocabAudience" ),
				servletContext.getInitParameter( "metadataVocabLanguage" ),
				currentVocabName );
		}
		return getVocabList( vocList );
	}

	/**
	 *  Gets the vocabList attribute of the HistogramForm object
	 *
	 * @param  vocList
	 * @return          The vocabList value
	 */
	private ArrayList getVocabList( ArrayList vocList ) {
		return getVocabList( vocList, true );
	}

	/**
	 *  Gets the vocabList attribute of the HistogramForm object
	 *
	 * @param  vocList
	 * @param  isTop
	 * @return          The vocabList value
	 */
	private ArrayList getVocabList( ArrayList vocList, boolean isTop ) {
		ArrayList ret = new ArrayList();
		for ( int i = 0; i < vocList.size(); i++ ) {
			HistogramVocabNode newNode = new HistogramVocabNode( (VocabNode)vocList.get( i ), this, false );
			ArrayList sublist = newNode.getVocabNode().getSubList();
			if ( sublist.size() > 0 ) {
				newNode.setLibraryTotal( 0 );
				ret.add( newNode );
				ret.addAll( getVocabList( sublist, false ) );
			}
			else {
				int total = 0;
				if ( collection != null && ( collection.length() > 0 ) ) {
					total = newNode.setLibraryTotal( index, rm, DDSQueryAction.getCollectionQueryTerm( collection ) );
				}
				else {
					total = newNode.setLibraryTotal( index, rm, "" );
				}
				if ( total > largestTotal ) {
					largestTotal = total;
				}
				if ( collection != null ) {
					newNode.setCollection( collection );
				}
				ret.add( newNode );
			}
		}
		if ( isTop ) {
			ret = hasSubtotalsGreaterThanZero( ret );
		}
		return ret;
	}

	/**
	 *  Description of the Method
	 *
	 * @param  ret
	 * @return
	 */
	public ArrayList hasSubtotalsGreaterThanZero( ArrayList ret ) {
		// Set hasSubtotalsGreaterThanZero property in sub-headers, and isLastInSublist for all nodes:
		for ( int i = 0; i < ret.size(); i++ ) {
			HistogramVocabNode node = (HistogramVocabNode)ret.get( i );
			node.setHasSubtotalsGreaterThanZero( false );
			int size = node.getVocabNode().getSubList().size();
			boolean hasPositiveSubtotals = false;
			for ( int j = i + 1; j < ( i + 1 + size ); j++ ) {
				HistogramVocabNode subNode = (HistogramVocabNode)ret.get( j );
				if ( Integer.parseInt( subNode.getLibraryTotal() ) > 0 ) {
					node.setHasSubtotalsGreaterThanZero( true );
					hasPositiveSubtotals = true;
				}
				if ( hasPositiveSubtotals && ( j == ( i + size ) ) ) {
					subNode.setIsLastInSublist( true );
				}
			}
		}
		return ret;
	}

	/**
	 *  Gets the total attribute of the HistogramForm object
	 *
	 * @return    The total value
	 */
	public String getTotal() {
		return Integer.toString( index.getNumDocs( rm.getDiscoverableItemsQuery() + totalQuery ) );
	}

	/**
	 *  This is the OLD way, used by DDS v2.0
	 *
	 * @return    The collectionsVocab value
	 */
	public ArrayList getCollectionsVocab() {
		return vocab.getVocabNodes( "dlese_collect",
			servletContext.getInitParameter( "metadataVocabAudience" ),
			servletContext.getInitParameter( "metadataVocabLanguage" ), "key" );
	}

	/**
	 *  Sets the repositoryManager attribute of the HistogramForm object
	 *
	 * @param  rm  The new repositoryManager value
	 */
	public void setRepositoryManager( RepositoryManager rm ) {
		this.rm = rm;
		currentVocabGroup = "";
		if ( !rm.isIndexing() ) {
			long currentIndexModified = rm.getIndexLastModifiedCount();
			if ( currentIndexModified != histogramIndexModified ) {
				HistogramVocabNode.clearCache();
				histogramIndexModified = currentIndexModified;
			}
		}
	}

	/**
	 *  Sets the index attribute of the HistogramForm object
	 *
	 * @param  index  The new index value
	 */
	public void setIndex( SimpleLuceneIndex index ) {
		this.index = index;
	}

	/**
	 *  Gets the collectionTotal attribute of the HistogramForm object
	 *
	 * @return    The collectionTotal value
	 */
	public String getCollectionTotal() {
		return Integer.toString( index.getNumDocs() );
	}
}


