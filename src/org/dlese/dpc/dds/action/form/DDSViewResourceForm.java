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

import org.dlese.dpc.index.reader.*;
import org.dlese.dpc.repository.*;
import org.dlese.dpc.index.*;
import org.dlese.dpc.vocab.MetadataVocab;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.io.*;
import java.text.*;
import java.net.URLEncoder;
import org.dlese.dpc.util.GetURL;

/**
 *  A Struts Form bean for handling DDS view resource requests
 *
 * @author    Ryan Deardorff
 */
public class DDSViewResourceForm extends ActionForm implements Serializable {
	private String recordId;
	private String recordFilename;
	private String[] recordCollections;
	private String[] recordIds;
	private SimpleLuceneIndex index = null;
	private ResultDocList resultDocs = null;
	private ResultDoc primaryResultDoc = null;
	private String primaryResultDocCollectionKey = null;
	private String pathwayUrl = "";
	private String error = null;
	private String resourceResultLinkRedirectURL = "";
	private ItemDocReader itemDocReader = null;
	private String primaryResultDocId = "";
	private Map vocabCache = null;
	private String vocabCacheGroup = "";
	private String system = "";
	private MetadataVocab vocab = null;
	private String relDisplayed = null;
	private String collectionKey = null;
	private String contextUrl = "";
	private StringBuffer vocabCacheFeedbackString = new StringBuffer( "" );

	private String forwardUrl;

	/**
	 *  Sets the forwardUrl attribute of the DDSViewResourceForm object
	 *
	 * @param  forwardUrl  The new forwardUrl value
	 */
	public void setForwardUrl( String forwardUrl ) {
		this.forwardUrl = forwardUrl;
	}

	/**
	 *  Gets the forwardUrl attribute of the DDSViewResourceForm object
	 *
	 * @return    The forwardUrl value
	 */
	public String getForwardUrl() {
		return forwardUrl;
	}

	/**
	 *  Gets the vocabCacheFeedbackString attribute of the DDSViewResourceForm
	 *  object
	 *
	 * @return    The vocabCacheFeedbackString value
	 */
	public String getVocabCacheFeedbackString() {
		if ( vocabCacheFeedbackString.length() > 1 ) {
			vocabCacheFeedbackString.setLength( vocabCacheFeedbackString.length() - 2 );
		}
		return vocabCacheFeedbackString.toString();
	}

	/**
	 *  Sets the vocabCacheFeedbackString attribute of the DDSViewResourceForm
	 *  object
	 *
	 * @param  append  The new vocabCacheFeedbackString value
	 */
	public void setVocabCacheFeedbackString( String append ) {
		vocabCacheFeedbackString.append( append + ", " );
	}

	/**
	 *  Constructor for the DDSViewResourceForm object
	 */
	public DDSViewResourceForm() {
		vocabCache = new HashMap();
	}

	/**
	 *  Sets the vocab attribute of the DDSViewResourceForm object
	 *
	 * @param  vocab  The new vocab value
	 */
	public void setVocab( MetadataVocab vocab ) {
		this.vocab = vocab;
	}

	/**
	 *  Gets the vocab attribute of the DDSViewResourceForm object
	 *
	 * @return    The vocab value
	 */
	public MetadataVocab getVocab() {
		return vocab;
	}

	/**
	 *  Sets the system attribute of the DDSViewResourceForm object
	 *
	 * @param  system  The new system value
	 */
	public void setSystem( String system ) {
		this.system = system;
	}

	/**
	 *  Description of the Method
	 */
	public void clearVocabCache() {
		vocabCacheFeedbackString.setLength( 0 );
		vocabCache.clear();
	}

	/**
	 *  Description of the Method
	 *
	 * @param  vocabValue
	 */
	public void setVocabCacheValue( String vocabValue ) {
		vocabCache.put( vocabValue, new Boolean( true ) );
	}

	/**
	 *  Sets the vocabCacheGroup attribute of the DDSViewResourceForm object
	 *
	 * @param  vocabCacheGroup  The new vocabCacheGroup value
	 */
	public void setVocabCacheGroup( String vocabCacheGroup ) {
		this.vocabCacheGroup = vocabCacheGroup;
	}

	/**
	 *  Gets the cachedVocabValuesInOrder attribute of the DDSViewResourceForm
	 *  object
	 *
	 * @return    The cachedVocabValuesInOrder value
	 */
	public ArrayList getCachedVocabValuesInOrder() {
		return vocab.getCacheValuesInOrder( system, vocabCacheGroup, vocabCache );
	}

	/**
	 *  Sets the resourceResultLinkRedirectURL attribute of the DDSViewResourceForm
	 *  object
	 *
	 * @param  str  The new resourceResultLinkRedirectURL value
	 */
	public void setResourceResultLinkRedirectURL( String str ) {
		resourceResultLinkRedirectURL = str;
	}

	/**
	 *  Gets the resourceResultLinkRedirectURL attribute of the DDSQueryForm object
	 *
	 * @return    The resourceResultLinkRedirectURL value
	 */
	public String getResourceResultLinkRedirectURL() {
		return resourceResultLinkRedirectURL;
	}

	/**
	 *  Sets the primaryResultDocId attribute of the DDSViewResourceForm object
	 *
	 * @param  id  The new primaryResultDocId value
	 */
	public void setPrimaryResultDocId( String id ) {
		this.primaryResultDocId = id;
	}

	/**
	 *  Gets the primaryResultDocId attribute of the DDSViewResourceForm object
	 *
	 * @return    The primaryResultDocId value
	 */
	public String getPrimaryResultDocId() {
		return primaryResultDocId;
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
	 *  Sets the primaryResultDoc attribute of the DDSViewResourceForm object
	 *
	 * @param  primaryResultDoc  The new primaryResultDoc value
	 */
	public void setPrimaryResultDoc( ResultDoc primaryResultDoc ) {
		this.primaryResultDoc = primaryResultDoc;
		itemDocReader = (ItemDocReader)primaryResultDoc.getDocReader();
	}

	/**
	 *  Gets the primaryResultDocReader attribute of the DDSViewResourceForm object
	 *
	 * @return    The primaryResultDoc value
	 */
	public ResultDoc getPrimaryResultDoc() {
		return primaryResultDoc;
	}

	/**
	 *  Gets the itemDocReader attribute of the DDSViewResourceForm object
	 *
	 * @return    The itemDocReader value
	 */
	public ItemDocReader getItemDocReader() {
		return itemDocReader;
	}

	/**
	 *  Sets the primaryResultDocCollectionId attribute of the DDSViewResourceForm
	 *  object
	 *
	 * @param  primaryResultDocCollectionKey  The new primaryResultDocCollectionKey
	 *      value
	 */
	public void setPrimaryResultDocCollectionKey( String primaryResultDocCollectionKey ) {
		this.primaryResultDocCollectionKey = primaryResultDocCollectionKey;
	}

	/**
	 *  Gets the primaryResultDocCollectionId attribute of the DDSViewResourceForm
	 *  object
	 *
	 * @return    The primaryRequestedCollectionKey value
	 */
	public String getPrimaryRequestedCollectionKey() {
		return primaryResultDocCollectionKey;
	}

	/**
	 *  Gets the hasReviews attribute of the DDSViewResourceForm object
	 *
	 * @return    The hasReviews value
	 */
	public String getHasReviews() {
		ResultDocList annoResults = ( (ItemDocReader)primaryResultDoc.getDocReader() ).getAnnotationResultDocs();

		if ( annoResults != null ) {
			for ( int i = 0; i < annoResults.size(); i++ ) {
				DleseAnnoDocReader annoReader = (DleseAnnoDocReader)annoResults.get(i).getDocReader();
				if ( ( annoReader != null ) &&
					( annoReader.getType().equals( "Review" ) ||
					annoReader.getType().equals( "Average scores of aggregated indices" ) ||
					annoReader.getType().equals( "Editor's summary" ) ) &&
					!annoReader.getStatus().matches( ".*progress" ) ) {
					return "true";
				}
			}
		}
		return "false";
	}

	/**
	 *  Gets the hasDrcReviews attribute of the DDSViewResourceForm object
	 *
	 * @return    The hasDrcReviews value
	 */
	public String getHasDrcReviews() {
		ResultDocList annoResults = ( (ItemDocReader)primaryResultDoc.getDocReader() ).getAnnotationResultDocs();
		if ( annoResults != null ) {
			for ( int i = 0; i < annoResults.size(); i++ ) {
				DleseAnnoDocReader annoReader = (DleseAnnoDocReader)annoResults.get(i).getDocReader();
				if ( ( annoReader != null ) &&
					( annoReader.getType().equals( "Review" ) ||
					annoReader.getType().equals( "Average scores of aggregated indices" ) ||
					annoReader.getType().equals( "Editor's summary" ) ) &&
					( annoReader.getPathway().length() > 0 ) &&
					!annoReader.getStatus().matches( ".*progress" ) ) {
					return "true";
				}
			}
		}
		return "false";
	}

	/**
	 *  Gets the isDrcReview attribute of the DDSViewResourceForm object
	 *
	 * @param  index
	 * @return        The isDrcReview value
	 */
	public String getIsDrcReview( int index ) {
		ResultDocList annoResults = ( (ItemDocReader)primaryResultDoc.getDocReader() ).getAnnotationResultDocs();
		if ( annoResults != null ) {
			DleseAnnoDocReader annoReader = (DleseAnnoDocReader)annoResults.get(index).getDocReader();
			if ( ( annoReader != null )
				 && ( annoReader.getType().equals( "Review" ) ||
				annoReader.getType().equals( "Average scores of aggregated indices" ) ||
				annoReader.getType().equals( "Editor's summary" ) ) &&
				( annoReader.getPathway().length() > 0 ) ) {
				return "true";
			}
		}
		return "false";
	}

	/**
	 *  Gets the isDrcReviewInProgress attribute of the DDSViewResourceForm object
	 *
	 * @return    The isDrcReviewInProgress value
	 */
	public String getIsDrcReviewInProgress() {
		ResultDocList annoResults = ( (ItemDocReader)primaryResultDoc.getDocReader() ).getAnnotationResultDocs();
		if ( annoResults != null ) {
			for ( int i = 0; i < annoResults.size(); i++ ) {
				DleseAnnoDocReader annoReader = (DleseAnnoDocReader)annoResults.get(i).getDocReader();
				if ( ( annoReader != null ) &&
					( annoReader.getType().equals( "Review" ) ||
					annoReader.getType().equals( "Average scores of aggregated indices" ) ||
					annoReader.getType().equals( "Editor's summary" ) ) &&
					( annoReader.getPathway().length() > 0 ) &&
					annoReader.getStatus().matches( ".*progress" ) ) {
					pathwayUrl = annoReader.getUrl();
					return "true";
				}
			}
		}
		return "false";
	}

	/**
	 *  Gets the pathwayUrl attribute of the DDSViewResourceForm object
	 *
	 * @return    The pathwayUrl value
	 */
	public String getPathwayUrl() {
		return pathwayUrl;
	}


	/**
	 *  Gets the hasOtherReviews attribute of the DDSViewResourceForm object
	 *
	 * @return    The hasOtherReviews value
	 */
	public String getHasOtherReviews() {
		ResultDocList annoResults = ( (ItemDocReader)primaryResultDoc.getDocReader() ).getAnnotationResultDocs();
		if ( annoResults != null ) {
			for ( int i = 0; i < annoResults.size(); i++ ) {
				DleseAnnoDocReader annoReader = (DleseAnnoDocReader)annoResults.get(i).getDocReader();
				if ( ( annoReader != null )
					 && annoReader.getType().equals( "Review" ) &&
					annoReader.getPathway().length() == 0 ) {
					return "true";
				}
			}
		}
		return "false";
	}

	/**
	 *  Gets the teachingTips attribute of the DDSViewResourceForm object
	 *
	 * @return    The hasTeachingTips value
	 */
	public String getHasTeachingTips() {
		ResultDocList annoResults = ( (ItemDocReader)primaryResultDoc.getDocReader() ).getAnnotationResultDocs();
		if(annoResults ==  null)
			return "false";
		for ( int i = 0; i < annoResults.size(); i++ ) {
			if ( ( (DleseAnnoDocReader)annoResults.get(i).getDocReader() ).getType().equals( "Teaching tip" ) ) {
				return "true";
			}
		}
		return "false";
	}

	/**
	 *  Gets the hasIdeasForUse attribute of the DDSViewResourceForm object
	 *
	 * @return    The hasIdeasForUse value
	 */
	public String getHasIdeasForUse() {
		ResultDocList annoResults = ( (ItemDocReader)primaryResultDoc.getDocReader() ).getAnnotationResultDocs();
		for ( int i = 0; i < annoResults.size(); i++ ) {
			if ( ( (DleseAnnoDocReader)annoResults.get(i).getDocReader() ).getType().equals( "See also" ) ) {
				return "true";
			}
		}
		return "false";
	}

	/**
	 *  Gets the hasChallengingLearningContexts attribute of the
	 *  DDSViewResourceForm object
	 *
	 * @return    The hasChallengingLearningContexts value
	 */
	public String getHasChallengingLearningContexts() {
		ResultDocList annoResults = ( (ItemDocReader)primaryResultDoc.getDocReader() ).getAnnotationResultDocs();
		for ( int i = 0; i < annoResults.size(); i++ ) {
			if ( ( (DleseAnnoDocReader)annoResults.get(i).getDocReader() ).getType().equals( "Information on challenging teaching and learning situations" ) ) {
				return "true";
			}
		}
		return "false";
	}

	/**
	 *  Sets the idSearch attribute of the DDSViewResourceForm object
	 *
	 * @param  id  The new idSearch value
	 */
	public void setIdSearch( String id ) {
		RepositoryManager rm =
			(RepositoryManager)getServlet().getServletContext().getAttribute( "repositoryManager" );
		String discoverableItemsQuery = "";
		if ( rm != null ) {
			discoverableItemsQuery = rm.getDiscoverableItemsQuery();
		}

		String q = "id:" + SimpleLuceneIndex.encodeToTerm( id );
		if ( discoverableItemsQuery.length() > 0 ) {
			q += " AND " + discoverableItemsQuery;
		}

		resultDocs = index.searchDocs( q );
	}

	/**
	 *  Sets the relDisplayed attribute of the DDSViewResourceForm object
	 *
	 * @param  val  The new relDisplayed value
	 */
	public void setRelDisplayed( String val ) {
		relDisplayed = val;
	}

	/**
	 *  Gets the relDisplayed attribute of the DDSViewResourceForm object
	 *
	 * @return    The relDisplayed value.
	 */
	public String getRelDisplayed() {
		return relDisplayed;
	}

	/**
	 *  Gets the idSearchTitle attribute of the DDSViewResourceForm object
	 *
	 * @return    The idSearchTitle value
	 */
	public String getIdSearchTitle() {
		if ( resultDocs == null || resultDocs.size() == 0 ) {
			return null;
		}

		return ( (ItemDocReader)resultDocs.get(0).getDocReader() ).getTitle();
	}

	/**
	 *  Gets the idSearchUrl attribute of the DDSViewResourceForm object
	 *
	 * @return    The idSearchUrl value
	 */
	public String getIdSearchUrl() {
		if ( resultDocs == null || resultDocs.size() == 0 ) {
			return null;
		}
		return ( (ItemDocReader)resultDocs.get(0).getDocReader() ).getUrl();
	}


	/**
	 *  Gets the resourceTitle attribute of the DDSViewResourceForm object
	 *
	 * @return    The resourceTitle value
	 */
	public String getResourceTitle() {
		return ( (ItemDocReader)primaryResultDoc.getDocReader() ).getTitle();
	}

	/**
	 *  Gets the resourceUrl attribute of the DDSViewResourceForm object
	 *
	 * @return    The resourceUrl value
	 */
	public String getResourceUrl() {
		return ( (ItemDocReader)primaryResultDoc.getDocReader() ).getUrl();
	}

	/**
	 *  Sets the recordFilename attribute of the DDSViewResourceForm object
	 *
	 * @param  recordFilename  The new recordFilename value
	 */
	public void setRecordFilename( String recordFilename ) {
		this.recordFilename = recordFilename;
	}

	/**
	 *  Gets the recordFilename attribute of the DDSViewResourceForm object
	 *
	 * @return    The recordFilename value
	 */
	public String getRecordFilename() {
		return recordFilename;
	}

	/**
	 *  Gets the recordCollection attribute of the DDSViewResourceForm object
	 *
	 * @return    The recordCollections value
	 */
	public String[] getRecordCollections() {
		return recordCollections;
	}

	/**
	 *  Gets the recordCollection attribute of the DDSViewResourceForm object
	 *
	 * @param  index
	 * @return        The recordCollection value
	 */
	public String getRecordCollection( int index ) {
		return recordCollections[index];
	}

	/**
	 *  Gets the recordIds attribute of the DDSViewResourceForm object
	 *
	 * @return    The recordIds value
	 */
	public String[] getRecordIds() {
		return recordIds;
	}

	/**
	 *  Gets the recordIds attribute of the DDSViewResourceForm object
	 *
	 * @param  index
	 * @return        The recordIds value
	 */
	public String getRecordIds( int index ) {
		return recordIds[index];
	}

	/**
	 *  Gets the numRecordCollections attribute of the DDSViewResourceForm object
	 *
	 * @return    The numRecordCollections value
	 */
	public int getNumRecordCollections() {
		String[] colls = ( (ItemDocReader)primaryResultDoc.getDocReader() ).getCollections();
		return colls.length;
	}

	/**
	 *  Sets the resourceId attribute of the DDSViewResourceForm object
	 *
	 * @param  recordId  The new recordId value
	 */
	public void setRecordId( String recordId ) {
		this.recordId = recordId;
		System.out.println( "setRecordId = " + recordId );
	}

	/**
	 *  Gets the resourceId attribute of the DDSViewResourceForm object
	 *
	 * @return    The recordId value
	 */
	public String getRecordId() {
		System.out.println( "getRecordId = " + recordId );
		return recordId;
	}

	/**
	 *  Sets the searcher attribute of the DDSViewResourceForm object
	 *
	 * @param  index  The new searcher value
	 */
	public void setSearcher( SimpleLuceneIndex index ) {
		this.index = index;
	}

	// The following are used by the "collections that contain" page:
	/**
	 *  Sets the collectionKey attribute of the DDSViewResourceForm object
	 *
	 * @param  collectionKey  The new collectionKey value
	 */
	public void setCollectionKey( String collectionKey ) {
		this.collectionKey = collectionKey;
	}

	/**
	 *  Sets the contextUrl attribute of the DDSViewResourceForm object
	 *
	 * @param  contextUrl  The new contextUrl value
	 */
	public void setContextUrl( String contextUrl ) {
		this.contextUrl = contextUrl;
	}

	/**
	 *  Gets the collectionDescription attribute of the DDSViewResourceForm object
	 *
	 * @return    The collectionDescription value
	 */
	public String getCollectionDescription() {
		return GetURL.getURL( contextUrl + "/collection.do?ky=" + collectionKey + "&noHead=true", false );
	}
}


