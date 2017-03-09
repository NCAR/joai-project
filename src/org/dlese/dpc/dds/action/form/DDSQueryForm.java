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

import org.dlese.dpc.index.*;
import org.dlese.dpc.index.reader.*;
import org.dlese.dpc.vocab.*;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionError;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionServlet;
import org.apache.struts.util.MessageResources;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.io.*;
import java.text.*;
import java.net.URLEncoder;

/**
 *  A Struts Form bean for handling DDS query requests that access a {@link
 *  org.dlese.dpc.index.SimpleLuceneIndex}. This class works in conjuction with
 *  the DDSQueryAction Struts Action class.
 *
 * @author    John Weatherley, Ryan Deardorff
 * @see       org.dlese.dpc.dds.action.DDSQueryAction
 */
public final class DDSQueryForm extends DDSViewResourceForm implements Serializable {

	private static boolean debug = false;
	private int numPagingRecords = 10;
	private int start = 0;
	private HttpServletRequest request;
	private String queryString = null;
	private String refineQueryString = null;
	private XMLDocReader docReader = null;
	private String metadata = null;
	private String contextURL = null;
	private String gradeLevel = null;
	private String resourceType = null;
	private String contentStandard = null;
	private String subject = null;
	private String collection = null;
	private String searchType = null;
	private String view = null;
	private String error = null;
	ResultDocList resultDocs = null;
	//private MetadataVocab vocab = null;                            // Vocab structure (servlet scope)
	private MetadataVocabInputState vocabInputState = null;        // Vocabs selected by user (session scope)
	ResultDoc currentResultDoc;
	private int totalNumResources = -1;
	private String resourceResultLinkRedirectURL = null;
	private ArrayList dateStrings = new ArrayList();
	private ArrayList dateStringsForUI = new ArrayList();
	private String wnfrom = "";
	private int dateStringIndex = 0;
	private StringBuffer pagingLinks = new StringBuffer( "" );
	private boolean isEmptySearch = false;

	/**
	 *  Constructor for the DDSQueryForm object
	 */
	public DDSQueryForm() { }

	/**
	 *  Description of the Method
	 */
	public void resetPagingLinks() {
		pagingLinks.setLength( 0 );
	}

	/**
	 *  view indicates which flavor of UI display is rendered (RSS for example)
	 *
	 * @param  view  The new view value
	 */
	public void setView( String view ) {
		this.view = view;
	}

	/**
	 *  view indicates which flavor of UI display is rendered (RSS for example)
	 *
	 * @return    The viewType value
	 */
	public String getView() {
		return view;
	}

	/**
	 *  Sets the wnfrom attribute of the DDSQueryForm object
	 *
	 * @param  wnfrom  The new wnfrom value
	 */
	public void setWnfrom( String wnfrom ) {
		this.wnfrom = wnfrom;
	}

	/**
	 *  Gets the wnfrom attribute of the DDSQueryForm object
	 *
	 * @return    The wnfrom value
	 */
	public String getWnfrom() {
		return wnfrom;
	}

	/**
	 *  Gets the dateStrings attribute of the DDSQueryForm object
	 *
	 * @return    The dateStrings value
	 */
	public ArrayList getDateStrings() {
		return dateStrings;
	}

	/**
	 *  Description of the Method
	 */
	public void clearDateStrings() {
		dateStringIndex = 0;
		dateStrings.clear();
	}

	/**
	 *  Adds a feature to the DateString attribute of the DDSQueryForm object
	 *
	 * @param  add  The feature to be added to the DateString attribute
	 */
	public void addDateString( String add ) {
		dateStrings.add( add );
	}

	/**
	 *  Adds a feature to the DateStringForUI attribute of the DDSQueryForm object
	 *
	 * @param  add  The feature to be added to the DateStringForUI attribute
	 */
	public void addDateStringForUI( String add ) {
		dateStringsForUI.add( add );
	}

	/**
	 *  Gets the dateStringsForUI attribute of the DDSQueryForm object
	 *
	 * @return    The dateStringsForUI value
	 */
	public String getDateStringsForUI() {
		return (String)dateStringsForUI.get( dateStringIndex++ );
	}

	/**
	 *  Sets the searchType attribute of the DDSQueryForm object
	 *
	 * @param  str  The new searchType value
	 */
	public void setSearchType( String str ) {
		searchType = str;
	}

	/**
	 *  Gets the searchType attribute of the DDSQueryForm object
	 *
	 * @return    The searchType value
	 */
	public String getSearchType() {
		return searchType;
	}

	/**
	 *  Sets the vocabInputState attribute of the DDSQueryForm object
	 *
	 * @param  vocabInputState  The new vocabInputState value
	 */
	public void setVocabInputState( MetadataVocabInputState vocabInputState ) {
		this.vocabInputState = vocabInputState;
	}

	/**
	 *  Gets the vocab attribute of the DDSQueryForm object
	 *
	 * @return    The vocab value
	 */
	public MetadataVocabInputState getVocabInputState() {
		if ( vocabInputState == null ) {
			vocabInputState = new MetadataVocabInputState();
		}
		return vocabInputState;
	}


	/**
	 *  Sets the error attribute of the DDSQueryForm object
	 *
	 * @param  error  The new error value
	 */
	public void setError( String error ) {
		this.error = error;
	}


	/**
	 *  Gets the error attribute of the DDSQueryForm object
	 *
	 * @return    The error value
	 */
	public String getError() {
		return error;
	}

	/**
	 *  Sets the resourceResultLinkRedirectURL attribute of the DDSQueryForm object
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
	 *  Gets the contextURL attribute of the SimpleQueryForm object
	 *
	 * @return    The contextURL value
	 */
	public String getContextURL() {
		return contextURL;
	}


	/**
	 *  Sets the contextURL attribute of the SimpleQueryForm object
	 *
	 * @param  contextURL  The new contextURL value
	 */
	public void setContextURL( String contextURL ) {
		this.contextURL = contextURL;
	}


	/**
	 *  Gets the search results returned by the {@link
	 *  org.dlese.dpc.index.SimpleLuceneIndex}.
	 *
	 * @return    The results value
	 */
	public ResultDocList getResults() {
		return resultDocs;
	}

	/**
	 *  Gets the search results returned by the {@link
	 *  org.dlese.dpc.index.SimpleLuceneIndex} as a List (compatible with c:forEach
	 *  iteration)
	 *
	 * @return    The resultsList value
	 */
	public List getResultsList() {
		return resultDocs;
	}


	/**
	 *  Sets the search results returned by the {@link
	 *  org.dlese.dpc.index.SimpleLuceneIndex}.
	 *
	 * @param  results  The new results value.
	 */
	public void setResults( ResultDocList results ) {
		resultDocs = results;
	}


	/**
	 *  Is the resource in the DRC?
	 *
	 * @param  index
	 * @return        The isPartOfDRC value
	 */
	public String getIsPartOfDRC( int index ) {
		ItemDocReader reader = (ItemDocReader)resultDocs.get(index).getDocReader();
		return reader.getPartOfDRC();
	}


	/**
	 *  Gets the hasViewableReview attribute of the DDSQueryForm object
	 *
	 * @param  index
	 * @return        The hasViewableReview value
	 */
	public String getHasViewableReview( int index ) {
		String ret = "false";
		ItemDocReader reader = (ItemDocReader)resultDocs.get(index).getDocReader();
		String[] annoTypes = reader.getAnnoTypes();
		if ( ( annoTypes != null ) && ( annoTypes.length > 0 ) ) {
			ret = "true";
		}
		String[] annoStatus = (String[])reader.getAnnoStatus().toArray( new String[]{} );
		if ( annoStatus != null ) {
			for ( int i = 0; i < annoStatus.length; i++ ) {
				if ( annoStatus[i].equals( "Review in progress" ) ||
					annoStatus[i].equals( "Review stopped" ) ) {
					ret = "false";
				}
			}
		}
		return ret;
	}


	/**
	 *  Sets the metadata attribute of the SimpleQueryForm object
	 *
	 * @param  metadata  The new metadata value
	 */
	public void setMetadata( String metadata ) {
		this.metadata = metadata;
	}


	/**
	 *  Gets the metadata attribute of the SimpleQueryForm object
	 *
	 * @return    The metadata value
	 */
	public String getMetadata() {
		/*
		 *if(metadata == null)
		 *return "";
		 */
		return metadata;
	}


	/**
	 *  Gets the numResults attribute of the SimpleQueryForm object
	 *
	 * @return    The numResults value
	 */
	public String getNumResults() {
		if ( resultDocs != null ) {
			return Integer.toString( resultDocs.size() );
		}

		else {
			return "0";
		}
	}


	/**
	 *  Gets the total number of resources that are currently discoverable.
	 *
	 * @return    The number of resource currently discoverable.
	 */
	public String getTotalNumResources() {
		return Integer.toString( totalNumResources );
	}


	/**
	 *  Sets the totalNumResources that are currently discoverable.
	 *
	 * @param  numResources  The totalNumResources that are currently discoverable.
	 */
	public void setTotalNumResources( int numResources ) {
		totalNumResources = numResources;
	}


	/**
	 *  Gets the query string entered by the user.
	 *
	 * @return    The query value.
	 */
	public String getQ() {
		return queryString;
	}


	/**
	 *  Gets the gr attribute of the DDSQueryForm object
	 *
	 * @return    The gr value
	 */
	public String getGr() {
		return gradeLevel;
	}


	/**
	 *  Sets the gr attribute of the DDSQueryForm object
	 *
	 * @param  gradeLevel  The new gr value
	 */
	public void setGr( String gradeLevel ) {
		this.gradeLevel = gradeLevel;
	}


	/**
	 *  Gets the re attribute of the DDSQueryForm object
	 *
	 * @return    The re value
	 */
	public String getRe() {
		return resourceType;
	}


	/**
	 *  Sets the re attribute of the DDSQueryForm object
	 *
	 * @param  resourceType  The new re value
	 */
	public void setRe( String resourceType ) {
		this.resourceType = resourceType;
	}


	/**
	 *  Gets the cs attribute of the DDSQueryForm object
	 *
	 * @return    The cs value
	 */
	public String getCs() {
		return contentStandard;
	}


	/**
	 *  Sets the cs attribute of the DDSQueryForm object
	 *
	 * @param  contentStandard  The new cs value
	 */
	public void setCs( String contentStandard ) {
		this.contentStandard = contentStandard;
	}


	/**
	 *  Gets the su attribute of the DDSQueryForm object
	 *
	 * @return    The su value
	 */
	public String getSu() {
		return subject;
	}


	/**
	 *  Sets the su attribute of the DDSQueryForm object
	 *
	 * @param  subject  The new su value
	 */
	public void setSu( String subject ) {
		this.subject = subject;
	}


	/**
	 *  Gets the ky attribute of the DDSQueryForm object
	 *
	 * @return    The ky value
	 */
	public String getKy() {
		return collection;
	}


	/**
	 *  Sets the ky attribute of the DDSQueryForm object
	 *
	 * @param  collection  The new ky value
	 */
	public void setKy( String collection ) {
		this.collection = collection;
	}


	/**
	 *  Gets the query string entered by the user, encoded for use in a URL string.
	 *
	 * @return    The query value ncoded for use in a URL string.
	 */
	public String getQe() {
		try {
			return URLEncoder.encode( queryString, "utf-8" );
		}
		catch ( UnsupportedEncodingException e ) {
			prtln( "getQe(): " + e );
			return "";
		}
	}


	/**
	 *  Gets the user keywords, truncated for feedback display
	 *
	 * @return    The keywordsTruncated value
	 */
	public String getKeywordsTruncated() {
		String ret = queryString;
		if ( ret.length() > 30 ) {
			int ind = ret.indexOf( " ", 30 );
			if ( ( ind > -1 ) && ( ind < 40 ) ) {
				ret = ret.substring( 0, ind ) + "...";
			}
			else if ( ret.length() > 34 ) {
				ret = ret.substring( 0, 30 ) + "...";
			}
		}
		return ret.trim();
	}


	/**
	 *  Gets the keywords attribute of the DDSQueryForm object
	 *
	 * @return    The keywords value
	 */
	public String getKeywords() {
		return queryString;
	}


	/**
	 *  Sets the q attribute of the SimpleQueryForm object
	 *
	 * @param  queryString  The new q value
	 */
	public void setQ( String queryString ) {
		this.queryString = queryString;
	}


	/**
	 *  Gets the refined query string entered by the user, used to search within
	 *  results.
	 *
	 * @return    The query value.
	 */
	public String getRq() {
		return refineQueryString;
	}


	/**
	 *  Sets the refined query string entered by the user, used to search within
	 *  results.
	 *
	 * @param  refineQueryString  The new rq value
	 */
	public void setRq( String refineQueryString ) {
		this.refineQueryString = refineQueryString;
	}


	/**
	 *  If no keyword entered and no vocabs have been selected, return "true",
	 *  otherwise return "false"
	 *
	 * @return    The isEmptySearch value
	 */
	public String getIsEmptySearch() {
		if ( isEmptySearch ) {
			return "true";
		}
		return "false";
	}

	/**
	 *  Sets the isEmptySearch attribute of the DDSQueryForm object
	 *
	 * @param  isEmptySearch  The new isEmptySearch value
	 */
	public void setIsEmptySearch( boolean isEmptySearch ) {
		this.isEmptySearch = isEmptySearch;
	}

	/**
	 *  Gets all request parameters except the refined query Rq parameter.
	 *
	 * @return    The nrqParams value.
	 */
	public ArrayList getNrqParams() {
		if ( request == null ) {
			return null;
		}

		Enumeration params = request.getParameterNames();
		String param;
		String vals[];
		ArrayList paramPairs = new ArrayList();
		while ( params.hasMoreElements() ) {
			param = (String)params.nextElement();
			if ( !param.equals( "rq" ) &&
				!param.equals( "s" ) ) {
				vals = request.getParameterValues( param );
				for ( int i = 0; i < vals.length; i++ ) {
					paramPairs.add( new ParamPair( param, vals[i] ) );
				}
			}
		}
		return paramPairs;
	}


	/**
	 *  Holds paramter, value pairs.
	 *
	 * @author    John Weatherley
	 */
	public class ParamPair implements Serializable {
		private String param, val;


		/**
		 *  Constructor for the ParamPair object
		 */
		public ParamPair() { }


		/**
		 *  Constructor for the ParamPair object
		 *
		 * @param  param  The parameter name.
		 * @param  val    The parameter value.
		 */
		public ParamPair( String param, String val ) {
			this.param = param;
			this.val = val;
		}


		/**
		 *  Gets the parameter name.
		 *
		 * @return    The parameter name.
		 */
		public String getName() {
			return param;
		}


		/**
		 *  Gets the parameter value.
		 *
		 * @return    The parameter value.
		 */
		public String getVal() {
			return val;
		}
	}


	/**
	 *  Sets the starting index for the records to display.
	 *
	 * @param  start  The new start value
	 */
	public void setStart( int start ) {
		this.start = start;
	}


	/**
	 *  Gets the offset into the results array to begin iterating.
	 *
	 * @return    The offset value
	 */
	public String getOffset() {
		return Integer.toString( start );
	}


	/**
	 *  Gets the length of iterations to loop over the results array.
	 *
	 * @return    The length value
	 */
	public String getLength() {
		return Integer.toString( numPagingRecords );
	}


	/**
	 *  Gets the pagingLinks attribute of the DDSQueryForm object
	 *
	 * @return    The pagingLinks value
	 */
	public String getPagingLinks() {
		if ( ( resultDocs != null ) && ( resultDocs.size() > 10 ) ) {
			if ( pagingLinks.length() == 0 ) {
				String searchType = getSearchType();
				int first = start;
				int last = start + numPagingRecords;
				int end = Math.min( last, resultDocs.size() );
				String PAGING_URL;
				if ( searchType != null && searchType.equals( "hist" ) ) {
					PAGING_URL = "browse" + getPagingCriteriaParams( searchType );
				}
				else {
					PAGING_URL = "query.do?q=" + getQe() + getPagingCriteriaParams( searchType );
				}
				final int RESULTS_FROM_CURRENT = 4;                         // show previous & next # of links
				int fcStart = 0;
				int fcCount = 0;
				int outCount = 0;
				int dif = last - first;
				String startStr = null;
				String[] outStr = new String[3 + ( RESULTS_FROM_CURRENT * 2 )];
				fcStart = first - ( RESULTS_FROM_CURRENT * dif );
				if ( fcStart > 0 ) {
					fcCount = Math.round( fcStart / dif );
				}
				// Previous result page:
				if ( first > 0 ) {
					startStr = getPagingStartStr( Integer.toString( first - dif ), searchType );
					outStr[outCount++] = " <a href='" + PAGING_URL + startStr + "' class='pagingLink' title='Previous page of resources'>&lt;&lt;</a>&nbsp;";
				}
				// Previous N result pages:
				for ( int i = 0; i < RESULTS_FROM_CURRENT; i++ ) {
					if ( fcStart >= 0 ) {
						startStr = getPagingStartStr( Integer.toString( fcStart ), searchType );
						outStr[outCount++] = "<a href='" + PAGING_URL + startStr + "' class='pagingLink'>" + Integer.toString( ++fcCount ) + "</a>";
					}
					fcStart += dif;
				}
				String currentPage = Integer.toString( ++fcCount );
				// Current page:
				outStr[outCount++] = "<span class='pagingThisPage'>" + currentPage + "</span>";
				// Next N result pages:
				fcStart += dif;
				for ( int i = 0; i < RESULTS_FROM_CURRENT; i++ ) {
					if ( fcStart < resultDocs.size() ) {
						startStr = getPagingStartStr( Integer.toString( fcStart ), searchType );
						outStr[outCount++] = "<a href='" + PAGING_URL + startStr + "' class='pagingLink'>" + Integer.toString( ++fcCount ) + "</a>";
					}
					fcStart += dif;
				}
				// Next result page:
				if ( last < resultDocs.size() ) {
					startStr = getPagingStartStr( Integer.toString( last ), searchType );
					outStr[outCount++] = "&nbsp;<a href='" + PAGING_URL + startStr
						 + "' class='pagingLink' title='Next page of resources'>&gt;&gt;</a>";
				}
				// Get total number of pages (Math.round() doesn't do the trick,
				// because you never know if it's rounding UP or DOWN...)
				int totalPages = 0;
				int totalCountDown = resultDocs.size();
				while ( totalCountDown > 0 ) {
					totalCountDown -= 10;
					totalPages++;
				}

				// Print the links:
				for ( int i = 0; i < outCount; i++ ) {
					pagingLinks.append( outStr[i] );
				}
				pagingLinks.append( "</nobr>" );
			}
			return pagingLinks.toString();
		}
		return "";
	}

	/**
	 *  Gets the pagingStartStr attribute of the DDSQueryForm object
	 *
	 * @param  start       Description of the Parameter
	 * @param  searchType  Description of the Parameter
	 * @return             The pagingStartStr value
	 */
	private String getPagingStartStr( String start, String searchType ) {
		if ( searchType != null && searchType.equals( "hist" ) ) {
			if ( Integer.parseInt( start ) > 0 ) {
				return "-" + start + ".htm";
			}
			return ".htm";
		}
		return "&s=" + start;
	}

	/**
	 *  Gets the criteriaParams attribute of the DDSQueryForm object
	 *
	 * @param  searchType  Description of the Parameter
	 * @return             The criteriaParams value
	 */
	private String getPagingCriteriaParams( String searchType ) {
		StringBuffer ret = new StringBuffer();
		Enumeration e = request.getParameterNames();
		while ( e.hasMoreElements() ) {
			String param = (String)e.nextElement();
			if ( !param.equals( "q" ) && !param.equals( "s" ) ) {
				String[] values = request.getParameterValues( param );
				if ( values != null && values[0].length() > 0 ) {
					for ( int i = 0; i < values.length; i++ ) {
						if ( searchType != null && searchType.equals( "hist" ) ) {
							if ( !param.equals( "hist" ) ) {
								ret.append( "_" + param + "_" + values[i] );
							}
						}
						else {
							ret.append( "&" + param + "=" + values[i] );
						}
					}
				}
			}
		}
		return ret.toString();
	}

	/**
	 *  Gets the starting index for the records that will be displayed.
	 *
	 * @return    The start value
	 */
	public String getStart() {
		// For display in the UI, add 1
		return Integer.toString( start + 1 );
	}


	/**
	 *  Gets the ending index for the records that will be displayed.
	 *
	 * @return    The end value
	 */
	public String getEnd() {
		if ( resultDocs == null || start < 0 ) {
			return null;
		}
		int e = start + numPagingRecords;
		int n = resultDocs.size();
		return Integer.toString( e < n ? e : n );
	}


	/**
	 *  Sets the request attribute of the SimpleQueryForm object.
	 *
	 * @param  request  The new request value
	 */
	public void setRequest( HttpServletRequest request ) {
		this.request = request;
	}


	/**
	 *  Gets all the parameters that existed in the request other than those used
	 *  for paiging.
	 *
	 * @return    The additionalParams returned as an HTTP query string.
	 */
	private final String getAdditionalParams() {
		if ( request == null ) {
			return null;
		}

		Enumeration params = request.getParameterNames();
		String param;
		String vals[];
		StringBuffer addParams = new StringBuffer();
		while ( params.hasMoreElements() ) {
			param = (String)params.nextElement();
			if ( !param.equals( "q" ) &&
				!param.equals( "s" ) ) {
				vals = request.getParameterValues( param );
				for ( int i = 0; i < vals.length; i++ ) {
					addParams.append( "&" + param + "=" + vals[i] );
				}
			}
		}
		return addParams.toString();
	}


	//================================================================

	/**
	 *  Output a line of text to standard out, with datestamp, if debug is set to
	 *  true.
	 *
	 * @param  s  The String that will be output.
	 */
	private final void prtln( String s ) {
		if ( debug ) {
			System.out.println( getDateStamp() + " " + s );
		}
	}


	/**
	 *  Return a string for the current time and date, sutiable for display in log
	 *  files and output to standout:
	 *
	 * @return    The dateStamp value
	 */
	private final static String getDateStamp() {
		return
			new SimpleDateFormat( "MMM d, yyyy h:mm:ss a zzz" ).format( new Date() );
	}


	/**
	 *  Sets the debug attribute of the object
	 *
	 * @param  db  The new debug value
	 */
	public static void setDebug( boolean db ) {
		debug = db;
	}
}



