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
package org.dlese.dpc.dds.action;

import org.dlese.dpc.repository.*;
import org.dlese.dpc.index.*;
import org.dlese.dpc.dds.action.form.*;
import org.dlese.dpc.index.reader.*;
import org.dlese.dpc.index.writer.*;
import org.dlese.dpc.index.queryParser.*;
import org.dlese.dpc.xml.*;
import org.dlese.dpc.util.*;
import org.dlese.dpc.webapps.tools.GeneralServletTools;
import org.dlese.dpc.propertiesmgr.PropertiesManager;
import org.apache.lucene.search.*;
import org.dlese.dpc.vocab.MetadataVocab;
import org.dlese.dpc.logging.*;
import java.util.logging.Level;
import java.util.regex.*;
import org.dlese.dpc.dds.*;
import org.dlese.dpc.schemedit.SchemEditServlet;
import org.dlese.dpc.index.search.DateRangeFilter;
import edu.ucsb.adl.LuceneGeospatialQueryConverter;

import java.util.*;
import java.text.*;
import java.io.*;
import java.net.*;
import java.util.Hashtable;
import java.util.Locale;
import javax.servlet.ServletException;
import javax.servlet.ServletContext;
import javax.servlet.http.*;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionError;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionServlet;
import org.apache.struts.util.MessageResources;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.queryParser.MultiFieldQueryParser;

/**
 *  A Struts Action for handling display of resource record descriptions, and their various collection info.
 *
 * @author    John Weatherley, Ryan Deardorff
 */
public final class DDSQueryAction extends Action {

	/**
	 *  Indicates use of the AND boolean operator in the {@link #getBooleanQuery(String[] terms, int operator,
	 *  boolean useStemming)} method.
	 */
	public final static int OPERATOR_AND = 0;

	/**
	 *  Indicates use of the OR boolean operator in the {@link #getBooleanQuery(String[] terms, int operator,
	 *  boolean useStemming)} method.
	 */
	public final static int OPERATOR_OR = 1;

	// Search types:

	/**  Indicates textual search by user */
	public final static int SEARCHTYPE_SEARCH = 0;
	/**  Indicates histogram search by user */
	public final static int SEARCHTYPE_HISTOGRAM = 1;
	/**  Indicates whats new search by user */
	public final static int SEARCHTYPE_WHATSNEW = 2;
	/**  Indicates web service search by client using user search request */
	public final static int SEARCHTYPE_DDSWS_USER_SEARCH = 3;
	/**  Indicates web service search by client using general search request */
	public final static int SEARCHTYPE_DDSWS_SEARCH = 4;
	/**  Indicates web service search by client using the ODL request */
	public final static int SEARCHTYPE_ODL_SEARCH = 5;
	/**  Indicates an RSS general request */
	public final static int SEARCHTYPE_GENERAL_RSS = 6;
	/**  Indicates an RSS whats new request */
	public final static int SEARCHTYPE_WHATSNEW_RSS = 7;
	/**  Indicates a JSHTML service search */
	public final static int SEARCHTYPE_JSHTML_SEARCH = 8;
	/**  Identifiers that get printed in the query/search logs */
	public final static String[] SEARCH_TYPE_NAMES = {
		"search",
		"histogram",
		"whatsnew",
		"ddsws-user-search",
		"ddsws-search",
		"odl-search",
		"rss-general",
		"rss-whatsnew",
		"jshtml-search"};

	/**  A list of all fields that are indexed in encoded form via SimpleLuceneIndex.encodeToTerm() */
	public final static String[] ENCODED_FIELDS = {
		"id",
		"urlenc"
		};

	/**  A list of aliases that map user-typed field names to the field name that exists in the index. */
	public final static String[][] FIELD_ALIASES = {
		{"url", "urlenc"}
		};

	private static boolean debug = false;
	private String SELECT_ALL = " -- All --";
	private static ClfLogger logger = null;
	private static StandardAnalyzer standardAnalyzer = new StandardAnalyzer(SimpleLuceneIndex.getLuceneVersion());

	// --------------------------------------------------------- Public Methods

	/**
	 *  Processes the specified HTTP request and creates the corresponding HTTP response by forwarding to a JSP
	 *  that will create it. A {@link org.dlese.dpc.index.SimpleLuceneIndex} must be available to this class via
	 *  a ServletContext attribute under the key "index." Returns an {@link
	 *  org.apache.struts.action.ActionForward} instance that maps to the Struts forwarding name "simple.query,"
	 *  which must be configured in struts-config.xml to forward to the JSP page that will handle the request.
	 *
	 * @param  mapping               The ActionMapping used to select this instance
	 * @param  request               The HTTP request we are processing
	 * @param  response              The HTTP response we are creating
	 * @param  form                  The ActionForm for the given page
	 * @return                       The ActionForward instance describing where and how control should be
	 *      forwarded
	 * @exception  IOException       if an input/output error occurs
	 * @exception  ServletException  if a servlet exception occurs
	 */
	public ActionForward execute(
	                             ActionMapping mapping,
	                             ActionForm form,
	                             HttpServletRequest request,
	                             HttpServletResponse response)
		 throws IOException, ServletException {

		DDSQueryForm sqf = (DDSQueryForm) form;
		sqf.setError(null);

		try {

			/*
			 *Design note:
			 *Only one instance of this class gets created for the app and shared by
			 *all threads. To be thread-safe, use only local variables, not instance
			 *variables (the JVM will handle these properly using the stack). Pass
			 *all variables via method signatures rather than instance vars.
			 */
			// Set up params for paging:
			if (request.getParameter("s") != null) {
				try {
					sqf.setStart(Integer.parseInt(request.getParameter("s").trim()));
				} catch (Throwable e) {}
			}
			sqf.setRequest(request);

			RepositoryManager rm =
				(RepositoryManager) servlet.getServletContext().getAttribute("repositoryManager");

			sqf.setContextURL(GeneralServletTools.getContextUrl(request));

			// All other requests hanled by the search handler.
			return handleMetadataSearchRequest(mapping, (DDSQueryForm) form, request, response, rm);
		} catch (Throwable t) {
			//prtlnErr("Caught exception: " + t);
			//t.printStackTrace();
			sqf.setError("Server encountered a severe problem. Message: " + t);
			return mapping.findForward("simple.query");
		}
	}


	/**
	 *  Handle a request to search over metadata collections and forwared to the appropriate jsp page to render
	 *  the response.
	 *
	 * @param  mapping               The ActionMapping used to select this instance
	 * @param  request               The HTTP request we are processing
	 * @param  response              The HTTP response we are creating
	 * @param  rm
	 * @param  queryForm
	 * @return                       The ActionForward instance describing where and how control should be
	 *      forwarded
	 * @exception  IOException       if an input/output error occurs
	 * @exception  ServletException  if a servlet exception occurs
	 */
	public ActionForward handleMetadataSearchRequest(ActionMapping mapping,
	                                                 DDSQueryForm queryForm,
	                                                 HttpServletRequest request,
	                                                 HttpServletResponse response,
	                                                 RepositoryManager rm)
		 throws IOException, ServletException {

		SimpleLuceneIndex index =
			(SimpleLuceneIndex) servlet.getServletContext().getAttribute("index");

		if (index == null) {
			throw new ServletException("The attribute \"index\" could not be found in the Servlet Context.");
		}

		// "Find a resource" links look like query.do?q= and nothing more,
		// and truly empty searches ("You did not define a search") have &s=0:
		if (request.getQueryString().equals("q=") ||
			request.getQueryString().equals("q=&s=0")) {
			queryForm.setIsEmptySearch(true);
		}
		else {
			queryForm.setIsEmptySearch(false);
		}
		queryForm.setTotalNumResources(rm.getNumDiscoverableADNResources());
		queryForm.setResourceResultLinkRedirectURL((String) servlet.getServletContext().getAttribute("resourceResultLinkRedirectURL"));

		String metadataVocabInstanceAttributeName = (String) servlet.getServletContext().getInitParameter("metadataVocabInstanceAttributeName");
		MetadataVocab vocab = (MetadataVocab) servlet.getServletContext().getAttribute(metadataVocabInstanceAttributeName);

		ActionErrors errors = new ActionErrors();

		String view = request.getParameter("view");

		queryForm.setStart(0);

		queryForm.resetPagingLinks();
		// Pass vocab to the form bean:
		queryForm.setVocab(vocab);
		// Set vocab input state using info. in request (and vocab system dds.descr.en-us)
		queryForm.getVocabInputState().setState(request, "dds.descr.en-us");
		request.getSession().setAttribute("MetadataVocabInputState", queryForm.getVocabInputState());
		queryForm.clearVocabCache();
		String paramVal = "";
		try {

			String searchOver = request.getParameter("over");
			if (searchOver != null && (searchOver.length() > 0)) {
				if (searchOver.equals("2")) {
					return mapping.findForward("site.query1");
				}
				if (searchOver.equals("3")) {
					return mapping.findForward("site.query2");
				}
				if (searchOver.equals("4")) {
					return mapping.findForward("site.query3");
				}
			}

			// Set up bean values:
			if ((request.getParameter("hist") != null) &&
				(request.getParameter("hist").equals("true"))) {
				queryForm.setSearchType("hist");
			}
			else if (request.getParameter("searchType") != null) {
				queryForm.setSearchType(request.getParameter("searchType"));
			}
			else {
				queryForm.setSearchType("search");
			}

			// Handle textual and field-based queries:
			if (request.getParameter("q") != null) {

				// Determine the search type for query logging:
				int searchType = SEARCHTYPE_SEARCH;
				if (queryForm.getSearchType().equals("hist")) {
					searchType = SEARCHTYPE_HISTOGRAM;
				}
				else if (request.getParameter("wnfrom") != null || request.getParameter("wnto") != null) {
					searchType = SEARCHTYPE_WHATSNEW;
				}

				// Perform the standard search and get results.
				DDSStandardSearchResult ddsSearchResult = ddsStandardQuery(
					request,
					null,
					rm,
					vocab,
					servlet.getServletContext(),
					searchType);

				//System.out.println( "SEARCH TYPE: " + searchType );

				// Set up the bean for paging:
				if (request.getParameter("s") != null) {
					try {
						queryForm.setStart(Integer.parseInt(request.getParameter("s")));
					} catch (Throwable e) {
						queryForm.setStart(0);
					}
				}
				else {
					queryForm.setStart(0);
				}

				// Set up the bean to handle whats new queries
				if (ddsSearchResult.getForwardName().equals("whats.new.query")) {
					queryForm.setSearchType("whatsNew");
					queryForm.setWnfrom(request.getParameter("wnfrom"));

					// Construct list of past dates for the "What's new" search drop-down:
					int monthCount = 0;
					Calendar calCompare = Calendar.getInstance();
					calCompare.set(2003, 7, 1);
					queryForm.clearDateStrings();
					Calendar cal = Calendar.getInstance(TimeZone.getDefault());
					cal.set(Calendar.DAY_OF_MONTH, 1);
					cal.add(Calendar.MONTH, -1);
					SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
					queryForm.addDateString(sdf.format(cal.getTime()));
					sdf = new SimpleDateFormat("MMMMM yyyy");
					queryForm.addDateStringForUI(sdf.format(cal.getTime()));
					monthCount++;
					while (cal.after(calCompare) && (monthCount < 6)) {
						cal.add(Calendar.MONTH, -1);
						sdf = new SimpleDateFormat("yyyy-MM-dd");
						queryForm.addDateString(sdf.format(cal.getTime()));
						sdf = new SimpleDateFormat("MMMMM yyyy");
						queryForm.addDateStringForUI(sdf.format(cal.getTime()));
						monthCount++;
					}
				}
				queryForm.setResults(ddsSearchResult.getResults());
				//System.out.println( "RETURN: " + ddsSearchResult.getForwardName() );
				return mapping.findForward(ddsSearchResult.getForwardName());
			}
			//System.out.println( "RETURN: simple.query" );
			return mapping.findForward("simple.query");
		} catch (Throwable t) {
			if (queryForm != null) {
				errors.add("error", new ActionError("generic.error",
					"There was a server problem: " + t));
				saveErrors(request, errors);
				prtlnErr("There was a server problem: " + t);
			}
			else {
				prtlnErr("There was a problem: " + t);
			}
			t.printStackTrace();
			return mapping.findForward("simple.query");
		}
	}


	/**
	 *  Performs textual and field-based searches limited to discoverable items only and using pre-defined search
	 *  logic. Used by DDS search and DDSWebServices search.
	 *
	 * @param  request                      The HTTP request
	 * @param  rm                           The RepositoryManager
	 * @param  vocab                        The MetadataVocab
	 * @param  context                      The ServletContext
	 * @param  searchType                   The searchType that gets logged
	 * @param  additionalQueryOrConstraint  If the q parameter is empty, this value will be used as the search
	 *      query, if q is not empty this value will be ANDed with it
	 * @return                              A DDSStandardSearchResult that contains the search results and a
	 *      String indicating which page to forward to for presentataion.
	 */
	public static DDSStandardSearchResult ddsStandardQuery(
	                                                       HttpServletRequest request,
	                                                       String additionalQueryOrConstraint,
	                                                       RepositoryManager rm,
	                                                       MetadataVocab vocab,
	                                                       ServletContext context,
	                                                       int searchType) {

		// All of the parameters that may be used in the search
		String[] selectedCollections = request.getParameterValues("ky");
		String[] gradeRanges = request.getParameterValues("gr");
		String[] resourceTypes = request.getParameterValues("re");
		String[] contentStandards = request.getParameterValues("cs");
		String[] subjects = request.getParameterValues("su");
		String wntype = request.getParameter("wntype");
		String sortBy = request.getParameter("sortby");
		String wnfrom = request.getParameter("wnfrom");
		String wnto = request.getParameter("wnto");
		String s = request.getParameter("s");
		String cq = request.getParameter("cq");
		String bq = request.getParameter("bq");
		String qh = request.getParameter("qh");
		String q = request.getParameter("q");
		String[] dcsStatus = request.getParameterValues("dcsStatus");
		String slc = request.getParameter("slc");

		// Remove question marks from the user's query (conflicts with Lucene syntax)
		if (q != null)
			q = q.replaceAll("\\?", " ");

		if (qh != null) {
			if (slc != null && slc.equals("t")) {
				if (q != null && q.trim().length() > 0) {
					q = "(" + q + ") AND (" + qh + ")";
				}
				else {
					q = qh;
				}
			}
			else if (q == null) {
				q = qh;
			}
		}

		Query basicQuery = null;
		Exception serviceException = null;

		Query geospatialQuery = null;
		// Get the Geospatial Query, or null if not requested:
		try {
			geospatialQuery = getGeospatialQuery(request);
		} catch (Exception geoEx) {
			serviceException = geoEx;
		}

		// Add or use the additional queryOrConstraint to the query
		if (additionalQueryOrConstraint != null) {
			if (q == null || q.trim().length() == 0) {
				q = additionalQueryOrConstraint;
			}
			else {
				q = "(" + q + ") AND (" + additionalQueryOrConstraint + ")";
			}
		}

		// Add global search constraints, if indicated by cq param
		if (cq != null && cq.trim().length() > 0) {
			if (q == null || q.trim().length() == 0) {
				q = cq;
			}
			else {
				q = "(" + q + ") AND (" + cq + ")";
			}
		}

		// Limit the search by DCS status, if requested by dcsStatus param
		if (dcsStatus != null) {
			SchemEditServlet sev = (SchemEditServlet) context.getAttribute("schemEditServlet");
			if (sev != null) {
				String statusQuery = sev.getStatusQuery(dcsStatus);
				if (statusQuery != null && statusQuery.length() > 0) {
					if (q == null || q.trim().length() == 0) {
						q = statusQuery;
					}
					else {
						q = "(" + q + ") AND (" + statusQuery + ")";
					}
				}
			}
		}

		// Add global search boosting, if indicated by the bq parameter
		if (bq != null && bq.trim().length() > 0) {
			String boosterQuery = "(allrecords:true OR (" + bq + "))";
			if (q == null || q.trim().length() == 0) {
				q = boosterQuery;
			}
			else {
				q = "(" + q + ") AND (" + boosterQuery + ")";
			}
		}

		// Parse the basic query
		try {
			//prtln("q: " + q);
			basicQuery = formatQuery(q, rm, context);
		} catch (org.apache.lucene.queryParser.ParseException pe) {
			serviceException = pe;
		}

		String constraintsQuery = "";
		HashMap docReaderAttributes = new HashMap();
		String discoverableItemsQuery = rm.getDiscoverableItemsQuery();
		String reviewedBoostingQuery;
		if (q == null || q.length() == 0) {
			reviewedBoostingQuery = rm.getAbsoluteDrcBoostingQuery();
		}
		else {
			reviewedBoostingQuery = rm.getDrcBoostingQuery();
		}

		// Handle empty keyword query with non-empty criteria:
		if (basicQuery == null &&
			(geospatialQuery != null ||
			(selectedCollections != null && selectedCollections[0].length() > 0) ||
			(gradeRanges != null && gradeRanges[0].length() > 0) ||
			(wnto != null && wnto.length() > 0) ||
			(wnfrom != null && wnfrom.length() > 0) ||
			(resourceTypes != null && resourceTypes[0].length() > 0) ||
			(subjects != null && subjects[0].length() > 0) ||
			(contentStandards != null && contentStandards[0].length() > 0))) {
			constraintsQuery = "(allrecords:true)";
		}
		else if (basicQuery == null) {
			// If the query is null and we have no other selections, issue an empty query.
			basicQuery = new BooleanQuery();
		}
		else {
			if (basicQuery != null) {
				docReaderAttributes.put("formattedUserQuery", basicQuery.toString());
			}
		}

		// Limit search to the fields indicated (ky,gr,su,re,cs)
		constraintsQuery += getVocabParamsQueryString(request, vocab, docReaderAttributes);

		// Handle What's New date searching
		Filter whatsNewFilter = null;
		try {
			whatsNewFilter = getWhatsNewDateFilter(wnfrom, wnto);
		} catch (ParseException pe) {
			prtlnErr("Invalid What's New date: " + pe);
		}

		// Handle What's new resource type:
		if (wntype != null) {
			constraintsQuery += (constraintsQuery.length() > 0 ? " AND " : "") + "(wntype:" + wntype + ")";
		}

		// Populate the result doc readers with data for de-duping and display:
		docReaderAttributes.put("discoverableItemsQuery", discoverableItemsQuery);
		docReaderAttributes.put("reviewedBoostingQuery", reviewedBoostingQuery);

		constraintsQuery += (constraintsQuery.length() > 0 ? " AND " : "") + reviewedBoostingQuery + " AND " + rm.getMultiDocBoostingQuery() +
			" AND " + discoverableItemsQuery;

		ResultDocList resultDocs = null;
		SimpleLuceneIndex index = rm.getIndex();

		BooleanQuery booleanQuery = new BooleanQuery();

		QueryParser qp = index.getQueryParser();

		try {
			//System.out.println("constrints Query: '" + constraintsQuery + "'");
			if (basicQuery != null) {
				booleanQuery.add(basicQuery, BooleanClause.Occur.MUST);
			}
			if (geospatialQuery != null) {
				String geoClause = request.getParameter("geoClause");
				if (geoClause != null && geoClause.toLowerCase().equals("should"))
					booleanQuery.add(geospatialQuery, BooleanClause.Occur.SHOULD);
				else
					booleanQuery.add(geospatialQuery, BooleanClause.Occur.MUST);
			}
			booleanQuery.add(qp.parse(constraintsQuery), BooleanClause.Occur.MUST);

			//System.out.println("final Query: '" + booleanQuery + "'");
		} catch (Throwable e) {
			prtlnErr("parse error: " + e);
		}

		if (index != null) {
			// Set up sorting for dup docs
			if (sortBy != null) {
				// Use mostrecent is an alias for accessiondate
				if (sortBy.equalsIgnoreCase("mostrecent")) {
					sortBy = "accessiondate";
				}
				// Date fields sort descending for most recnet first
				if (sortBy.indexOf("date") >= 0) {
					docReaderAttributes.put("sortDescendingByField", sortBy);
				}
				// Text fields sort ascending for alphabetical
				else {
					docReaderAttributes.put("sortAscendingByField", sortBy);
				}
			}
			
			Sort sort = null;
			
			// Sort results in descending order
			if (sortBy != null) {
				// Date fields sort descending
				if (sortBy.indexOf("date") >= 0) {
					sort = new Sort( new SortField(sortBy, SortField.STRING, false) );
					//Collections.sort(resultDocs, new LuceneFieldComparator(sortBy, LuceneFieldComparator.DESCENDING));
					prtln( "Sort descending by date field: " + sortBy );
				}
				// Text fields sort ascending
				else {
					sort = new Sort( new SortField(sortBy, SortField.STRING, true) );
					//Collections.sort(resultDocs, new LuceneFieldComparator(sortBy, LuceneFieldComparator.ASCENDING));
					prtln( "Sort ascending by date field: " + sortBy );
				}
			}

			//System.out.println( "WHATS NEW QUERY: " + booleanQuery + "/" + whatsNewFilter );
			resultDocs = index.searchDocs(booleanQuery, whatsNewFilter, sort, docReaderAttributes);

		}
		int numSearchResults = (resultDocs == null) ? 0 : resultDocs.size();

		// Determine the start position for displaying results (used here for logging only)
		int startPosition = -1;
		if (s != null) {
			try {
				startPosition = Integer.parseInt(s);
			} catch (Throwable e) {}
		}

		// Log the query that the user requested:
		logQuery(q, context,
			request,
			200,
			-1,
			numSearchResults,
			rm.getNumDiscoverableADNResources(),
			startPosition,
			searchType);

		if (whatsNewFilter != null) {
			return new DDSStandardSearchResult(resultDocs, serviceException, "whats.new.query");
		}
		else {
			return new DDSStandardSearchResult(resultDocs, serviceException, "simple.query");
		}
	}


	/**
	 *  Adds a feature to the ConstraintToQuery attribute of the DDSQueryAction class
	 *
	 * @param  query            The feature to be added to the ConstraintToQuery attribute
	 * @param  constraintQuery  The feature to be added to the ConstraintToQuery attribute
	 * @return                  The resulting query
	 */
	public final static String addConstraintToQuery(String query, String constraintQuery) {
		if (query != null && query.trim().length() > 0) {
			return "(" + query + ") AND (" + constraintQuery + ")";
		}
		else {
			return constraintQuery;
		}
	}


	/**
	 *  Generates a geospatial bounding box Lucene Query from the necessary parameteres in an http request, which
	 *  are geoPredicate, geoBBNorth, geoBBWest, geoBBEast, geoBBSouth. If the query crosses the 180/-180
	 *  longitude it is split into two query regions, one on each side, joined by boolean clause.
	 *
	 * @param  request        An http request that may contain geospatial parameters
	 * @return                A geospatial Query, or null if no no geospatial parameters were supplied
	 * @exception  Exception  If there is an error with one or more of the geospatial parameters
	 */
	public static Query getGeospatialQuery(HttpServletRequest request) throws Exception {
		String predicate = request.getParameter("geoPredicate");
		String north = request.getParameter("geoBBNorth");
		String west = request.getParameter("geoBBWest");
		String east = request.getParameter("geoBBEast");
		String south = request.getParameter("geoBBSouth");

		// If no geo parameters were indicated, return null:
		if (predicate == null && north == null && west == null && east == null && south == null)
			return null;

		double westCoord = 0d;
		double eastCoord = 0d;
		try {
			westCoord = Double.parseDouble(west);
			eastCoord = Double.parseDouble(east);
		} catch (Throwable t) {}

		try {
			// If the query crosses the 180/-180 longitude, split the query into two regions on either side:
			if (westCoord > eastCoord) {
				BooleanQuery booleanQuery = new BooleanQuery();
				Query geoQ1 = LuceneGeospatialQueryConverter.convertQuery(predicate, north, south, east, "-179.999999");
				Query geoQ2 = LuceneGeospatialQueryConverter.convertQuery(predicate, north, south, "179.999999", west);
				booleanQuery.add(geoQ1, BooleanClause.Occur.SHOULD);
				booleanQuery.add(geoQ2, BooleanClause.Occur.SHOULD);
				return booleanQuery;
			}

			// Return the Query, throws an exception if parameteres are invalid. Encodes BB fields as "northCoord", "southCoord", "eastCoord", "westCoord":
			return LuceneGeospatialQueryConverter.convertQuery(predicate, north, south, east, west);
		} catch (Exception ex) {
			throw new Exception("Geospatial query error: " + ex.getMessage());
		}
	}


	/**
	 *  Gets a query that limits a search to vocab-managed fields such as gradeRange (gr), resourceType (re),
	 *  subject (su), contentStandard (cs) and collection (ky). The query returned begins with AND. If no sucy
	 *  parameters exist in the request, returns an empty String.
	 *
	 * @param  request              The HTTP request
	 * @param  vocab                The metadata vocab used
	 * @param  docReaderAttributes  The doc reader attributes
	 * @return                      Gets a query that limits a search to vocab-managed fields, or an empty String
	 *      if no such field parameters exist in the request.
	 */
	public final static String getVocabParamsQueryString(
	                                                     HttpServletRequest request,
	                                                     MetadataVocab vocab,
	                                                     HashMap docReaderAttributes) {
		String[] selectedCollections = request.getParameterValues("ky");
		String[] gradeRanges = request.getParameterValues("gr");
		String[] resourceTypes = request.getParameterValues("re");
		String[] contentStandards = request.getParameterValues("cs");
		String[] subjects = request.getParameterValues("su");

		String query = "";
		// Handle collection selection(s):
		if (selectedCollections != null &&
			selectedCollections[0].length() > 0) {
			String collectionsQuery = "(" +
				getCollectionQueryTerm(selectedCollections[0]);
			for (int i = 1; i < selectedCollections.length; i++) {
				collectionsQuery += " OR "
					 + getCollectionQueryTerm(selectedCollections[i]);
			}
			collectionsQuery += ")";
			query += (query.length() > 0 ? " AND " : "") + collectionsQuery;
			docReaderAttributes.put("collectionsQuery", collectionsQuery);
		}

		// Handle Grade Range
		if (gradeRanges != null && gradeRanges[0].length() > 0) {
			query = query + (query.length() > 0 ? " AND" : "") + " (gr:" + gradeRanges[0];
			for (int i = 1; i < gradeRanges.length; i++) {
				query += " OR gr:" + gradeRanges[i];
			}
			query += ")";
		}

		// Handle Resource Type
		if (resourceTypes != null && resourceTypes[0].length() > 0) {
			query = query + (query.length() > 0 ? " AND" : "") + " (re:" + resourceTypes[0];
			for (int i = 1; i < resourceTypes.length; i++) {
				query += " OR re:" + resourceTypes[i];
			}
			query += ")";
		}

		// Handle Standards
		if (contentStandards != null && contentStandards[0].length() > 0) {
			query = query + (query.length() > 0 ? " AND" : "") + " (cs:" + contentStandards[0];
			for (int i = 1; i < contentStandards.length; i++) {
				query += " OR cs:" + contentStandards[i];
			}
			query += ")";
		}

		// Handle subjects
		if (subjects != null && subjects[0].length() > 0) {
			query = query + (query.length() > 0 ? " AND" : "") + " (su:" + subjects[0];
			for (int i = 1; i < subjects.length; i++) {
				query += " OR su:" + subjects[i];
			}
			query += ")";
		}
		return query;
	}


	/**
	 *  Gets the whatsNewDateFilter attribute of the DDSQueryAction class
	 *
	 * @param  wnfrom              The from date
	 * @param  wnto                The to date
	 * @return                     The whatsNewDateFilter value
	 * @exception  ParseException  If error parsing
	 */
	public final static Filter getWhatsNewDateFilter(String wnfrom, String wnto)
		 throws ParseException {
		// Handle What's New date searching
		DateRangeFilter whatsNewFilter = null;

		if (wnfrom != null && wnto == null &&
			(wnfrom.equals("recent") || wnfrom.equals("rss"))) {
			Calendar cal = Calendar.getInstance(TimeZone.getDefault());
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
			sdf.setTimeZone(TimeZone.getDefault());
			int RECENT_WEEKS = 6;
			final String START_FROM_DATE = "2003-07-01";
			cal.add(Calendar.WEEK_OF_MONTH, -RECENT_WEEKS);
			wnfrom = sdf.format(cal.getTime());
			whatsNewFilter = DateRangeFilter.After("wndate", MetadataUtils.parseUnionDateType(wnfrom));
		}
		else if (wnfrom != null && wnto == null) {
			// Set wnto to be the end of wnfrom month
			Calendar cal = Calendar.getInstance(TimeZone.getDefault());
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
			sdf.setTimeZone(TimeZone.getDefault());
			int ind = wnfrom.indexOf("-");
			// Avoid year 10000 bug!
			String year = wnfrom.substring(0, ind);
			String month = wnfrom.substring(ind + 1, ind + 3);
			String day = wnfrom.substring(ind + 4, ind + 6);
			cal.set(Integer.parseInt(year),
				Integer.parseInt(month) - 1,
				Integer.parseInt(day));
			cal.add(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH) - 1);
			wnto = sdf.format(cal.getTime());
			//System.out.println( "WHATS NEW 2..." );
			whatsNewFilter = new DateRangeFilter("wndate", MetadataUtils.parseUnionDateType(wnfrom), MetadataUtils.parseUnionDateType(wnto));
		}
		else if (wnfrom == null && wnto != null) {
			whatsNewFilter = DateRangeFilter.Before("wndate", MetadataUtils.parseUnionDateType(wnto));
		}
		else if (wnfrom != null && wnto != null) {
			//System.out.println( "WHATS NEW 3..." );
			whatsNewFilter = new DateRangeFilter("wndate", MetadataUtils.parseUnionDateType(wnfrom), MetadataUtils.parseUnionDateType(wnto));
		}

		return whatsNewFilter;
	}


	/**
	 *  Gets the Lucene query string necessary to pull out records for a given collection. This method maps the
	 *  query to the set of records that belong to the collection. This value may be ANDed with others to produce
	 *  a compound boolean query.
	 *
	 * @param  ky  The vocab key, for example 06
	 * @return     The query String
	 */
	public static String getCollectionQueryTerm(String ky) {

		String ret = null;

		// If this is the DRC collection (ky:0c), use special query:
		if (ky.equals("0c") || ky.equals("drc")) {
			ret = "partofdrc:true";
		}

		/*
		 *The default query searches by collection key OR by the item's annotation collection key
		 *(mapped to those items that have one or more annos with status=completed). Because a
		 *search on the collection key and the anno collection key are guaranteed to return non-overlapping
		 *sets of ADN resources, it works to search on both in all cases.
		 *For the web servcice, this means a search by collection will return both formats, if
		 *available. For example a search on the CRS collection will return both dlese_anno and adn
		 *XML records.
		 */
		else {
			ret = "(ky:" + ky + " OR itemannocompletedcollectionkeys:" + ky + ")";
		}

		//System.out.println("getCollectionQueryTerm: " + ky + " is returning '" + ret + "'");

		return ret;
	}


	private static HashMap stopWordMap = null;


	/**
	 *  Gets the stopWordMap attribute of the DDSQueryAction class
	 *
	 * @return    The stopWordMap value
	 */
	public static HashMap getStopWordMap() {
		if (stopWordMap == null) {
			String[] stopWords = (String[])StandardAnalyzer.STOP_WORDS_SET.toArray();
			stopWordMap = new HashMap(stopWords.length);
			for (int i = 0; i < stopWords.length; i++) {
				stopWordMap.put(stopWords[i], new Object());
			}
		}
		return stopWordMap;
	}


	/**
	 *  Formats a given Query q for use with the standard Lucene query parser. Assumes documents have been
	 *  indexed using ItemFileIndexingWriter, etc.
	 *
	 * @param  q                                                 A keyword query typed by a user.
	 * @param  rm                                                The RepositoryManager
	 * @param  context                                           The servlet context
	 * @return                                                   The query formatted for boosting title and for
	 *      searching URLs and IDs.
	 * @exception  org.apache.lucene.queryParser.ParseException  If error parsing
	 */
	private final static Query formatQuery(String q, RepositoryManager rm, ServletContext context)
		 throws org.apache.lucene.queryParser.ParseException {
		if (q == null) {
			return null;
		}
		q = q.trim();
		if (q.length() == 0) {
			return null;
		}

		// --- First: format common queries that appear at the beginning of the user's query ---

		// Include the field, then a term that does not contain ( or ), then a space or end of input, then anything else
		Pattern p = Pattern.compile("\\A(id:|url:|site:|http:)([\\S&&[^\\(\\)]]+)(\\s+|\\z)(.*)");
		Matcher m = p.matcher(q);
		StringBuffer sb = new StringBuffer();
		try {
			while (m.find()) {
				String field = m.group(1);
				String term = m.group(2);
				String space = m.group(3);
				String rest = m.group(4);

				//System.out.println("field: '" + field + "' term: '" + term + "' space: '" + space + "' rest: '" + rest + "'");
				if (field != null && term != null) {

					if (field.equals("url:")) {
						field = "urlenc:";
						term = SimpleLuceneIndex.encodeToTerm(term, false);
					}
					else if (field.equals("http:")) {
						field = "urlenc:";
						term = SimpleLuceneIndex.encodeToTerm("http:" + term, false);
					}
					else if (field.equals("site:")) {
						String tmp;
						if (!term.startsWith("http://")) {
							tmp = "http://" + term;
						}
						else {
							tmp = term;
						}

						URL url = null;
						try {
							url = new URL(tmp);
						} catch (MalformedURLException e) {}
						if (url != null) {
							field = "urlenc:";
							term = SimpleLuceneIndex.encodeToTerm("http*" + url.getHost().replaceFirst("www\\.", ""), false) + "*";
						}
					}
					else if (field.equals("id:")) {
						field = "idvalue:";
						term = SimpleLuceneIndex.escape(term, "*");
					}

					//System.out.println("field: '" + field + "' escaped-term: '" + term + "' rest: '" + rest + "'");
					sb.append(field).append(term).append(space).append(rest);
				}
			}
		} catch (Throwable e) {
			prtlnErr("formatQuery(): " + e);
		}

		String fullQuery = null;

		// If we performed a replacement above, use is:
		if (sb.length() > 0) {
			fullQuery = sb.toString();
		}
		else {
			fullQuery = q;
		}

		Query luceneQuery = null;

		// Use the expansion query to apply boosting, stemming, virtual fields etc.
		luceneQuery = rm.getExpandedSearchQuery("(" + fullQuery + ")");

		//System.out.println("user's Query: '" + qObject + "'");
		//System.out.println("query: '" + luceneQuery + "'");

		return luceneQuery;
	}


	/**
	 *  Formats the fields/terms in the query string by replacing the aliased field names and encoding terms that
	 *  are in fields that have been indexed encoded.
	 *
	 * @param  q  The raw query by user
	 * @return    Formatted query
	 */
	public final static String formatFieldsInQuery(String q) {
		q = replaceAliasedFieldNames(q);
		return formatEncodedFields(q).toString();
	}


	private static String encFieldsRegEx;

	// Creates a reqular expression of the form id:|urlenc:|emailPrimaryEnc:
	static {
		encFieldsRegEx = "";
		for (int i = 0; i < ENCODED_FIELDS.length; i++) {
			encFieldsRegEx += ENCODED_FIELDS[i] + ":";
			if (i != ENCODED_FIELDS.length - 1) {
				encFieldsRegEx += "|";
			}
		}
	}


	/**
	 *  Encodes a query string such that each term that is part of a field that has been encoded in the index
	 *  using {@link org.dlese.dpc.index.SimpleLuceneIndex}.encodeToTerm() is replaced with the encoded form for
	 *  searching. May be used in conjuction with {@link #replaceAliasedFieldNames(String)}.<p>
	 *
	 *  Examples:<br>
	 *  <blockquote> <code>
	 *'id:DLESE-000-000-000-001' becomes 'id:dlesex45000x45000x45000x45001'<br>
	 *  <br>
	 *  'ocean AND urlenc:(http://*.noaa.gov* OR http://*.nasa.gov*)' becomes 'ocean AND
	 *  urlenc:(httpx58x47x47*x46noaax46gov* OR httpx58x47x47*x46nasax46gov*)'<br>
	 *  </code></blockquote>
	 *
	 * @param  q  A query string
	 * @return    An query string with appropriate terms encoded
	 */
	public final static StringBuffer formatEncodedFields(String q) {
		if (q == null) {
			return null;
		}

		//if(true)
		//return new StringBuffer(q);


		Pattern p = Pattern.compile("(" + encFieldsRegEx + ")((\\(|\")([\\S &&[^\"\\^\\)\\(]]+)(\"|\\))|([\\S&&[^\\^]]+)(\\B))");
		Matcher m = p.matcher(q);
		StringBuffer sb = new StringBuffer();
		while (m.find()) {
			// Group 4 will contain the terms inside quotes or parens, e.g. url:(http://my.com http://your.com)
			String term = m.group(4);
			if (term != null) {
				String terms[] = term.split("\\s+");
				term = "";
				// Check for AND|OR and don't encode them
				for (int i = 0; i < terms.length; i++) {
					if (terms[i].equals("AND") || terms[i].equals("OR")) {
						term += terms[i];
					}
					else {
						String quot = m.group(3);
						// If we are inside quotes, DO encode the *
						if (quot != null && quot.equals("\"")) {
							term += SimpleLuceneIndex.encodeToTerm(terms[i], true);
						}
						// If we are not inside quotes, DO NOT encode the *
						else {
							term += SimpleLuceneIndex.encodeToTerm(terms[i], false);
						}
					}
					if (i != terms.length - 1) {
						term += " ";
					}
				}
			}
			// Group 6 will contain the single term after the field, e.g. url:http://my.com
			String term2 = SimpleLuceneIndex.encodeToTerm(m.group(6), false);

			m.appendReplacement(sb, "$1$3" + (term == null ? "" : term) + "$5" + (term2 == null ? "" : term2) + "$7");
		}
		m.appendTail(sb);
		//System.out.println("encoded: '" + sb.toString() + "'");

		return sb;
	}


	/**
	 *  Substitutues the searchable field names for the common field names that user's may use in their query
	 *  string. May be used in conjuction with {@link #formatEncodedFields(String)}.<p>
	 *
	 *  Examples:<br>
	 *  <blockquote> <code>
	 *  'url:http://*.noaa.gov*' becomes 'urlenc:http://*.noaa.gov*'<br>
	 *  </code> </blockquote>
	 *
	 * @param  q  A query from a user
	 * @return    The query with the aliased field names replaced
	 */
	public final static String replaceAliasedFieldNames(String q) {
		//if(true)
		//return q;

		if (q == null) {
			return null;
		}

		for (int i = 0; i < FIELD_ALIASES.length; i++) {
			q = q.replaceAll(FIELD_ALIASES[i][0] + ":", FIELD_ALIASES[i][1] + ":");
		}
		return q;
	}


	/**
	 *  Creates a boolean query string for use in the standard Lucene query parser.
	 *
	 * @param  terms        The terms used in the query.
	 * @param  operator     The operator to use (AND|OR).
	 * @param  useStemming  True to stem the tersm, false to leave unchanged.
	 * @return              A formatted boolean query or null if no terms were present.
	 */
	public final static String getBooleanQuery(String[] terms, int operator, boolean useStemming) {
		if (terms == null || terms.length < 1) {
			return null;
		}

		String opr;
		if (operator == 0) {
			opr = " AND ";
		}
		else {
			opr = " OR ";
		}
		String q = "";

		// Make a boolean query
		boolean insideQuotes = false;
		for (int i = 0; i < terms.length - 1; i++) {
			//System.out.println(" term: " + terms[i]);
			if (numQuotes(terms[i].toCharArray()) == 1) {
				insideQuotes = insideQuotes ? false : true;
			}

			// Non-quoted terms...
			if (!insideQuotes) {
				if (useStemming) {
					if (!terms[i].matches("AND|OR|.*\\\".*")) {
						q += Stemmer.getStem(terms[i]);
					}
					else {
						q += terms[i];
					}
				}
				else {
					q += terms[i];
				}

				// Append the operator only if appropriate
				if (!terms[i].matches("AND|OR") && !terms[i + 1].matches("AND|OR")) {
					q += opr;
				}
				else {
					q += " ";
				}
			}
			// Quoted terms...
			else {
				q += terms[i] + " ";
			}
		}

		// Handle the final term
		if (useStemming) {
			if (terms[terms.length - 1].indexOf('\"') < 0) {
				q += Stemmer.getStem(terms[terms.length - 1]);
			}
			else {
				q += terms[terms.length - 1];
			}
		}
		else {
			q += terms[terms.length - 1];
		}
		return "(" + q + ")";
	}


	/**
	 *  Examines the term for the presence of the string url: http: or site: and then encodes it appropriately
	 *  for searching in the urlenc Lucene field, or returns term if no encoding is needed.
	 *
	 * @param  term  The term to encode
	 * @return       The encoded fieldTerm value, or term if no encoding needed.
	 */
	private final static String getFieldTerm(String term) {
		if (term.startsWith("http://")) {
			term = "urlenc:" + SimpleLuceneIndex.encodeToTerm(term, false);
			return term;
		}
		else if (term.startsWith("url:")) {
			term = term.substring(4, term.length());
			term = "urlenc:" + SimpleLuceneIndex.encodeToTerm(term, false);
			return term;
		}
		else if (term.startsWith("site:")) {
			term = term.substring(5, term.length());
			if (term.startsWith("http://")) {
				term = term.substring(7, term.length());
			}

			URL url;
			try {
				url = new URL("http://" + term);
			} catch (MalformedURLException e) {
				return term;
			}
			term = "(urlenc:" + SimpleLuceneIndex.encodeToTerm("http://" + url.getHost(), false) +
				"* OR urlenc:" + SimpleLuceneIndex.encodeToTerm("http://www." + url.getHost(), false) + "*)";
			return term;
		}
		/*
		 *else if (term.startsWith("id:")) {
		 *term = term.substring(3, term.length());
		 *term = "id:" + SimpleLuceneIndex.encodeToTerm(term, false);
		 *return term;
		 *}
		 */
		else {
			return term;
		}
	}


	/**
	 *  Description of the Method
	 *
	 * @param  chars
	 * @return
	 */
	private final static int numQuotes(char[] chars) {
		int numQuotes = 0;
		for (int i = 0; i < chars.length; i++) {
			if (chars[i] == '"') {
				numQuotes++;
			}
		}
		return numQuotes;
	}


	/**
	 *  This method must be called at just after the user's query has been processed by Lucene. It is used to log
	 *  user queries.
	 *
	 * @param  query             The text query entered by the user or submitted by the web service client, or
	 *      null if none was submitted with the request
	 * @param  statusCode        The http status code
	 * @param  contentLen        The length of the data returned, in bytes
	 * @param  numSearchResults  The number of search results returned by the query
	 * @param  totalRecords      The total number of records in the system at the time of the query
	 * @param  rank              The rank of the item(s) retunred on the page. If this is a results page, then
	 *      this is rank of the first item on the page.
	 * @param  servletContext    The servletContext
	 * @param  request           The HTTP request
	 * @param  searchType        The search type (search, histogram, what's new, ws-uql, ws-lql, etc)
	 * @return                   True iff successful, else false
	 */
	public final static synchronized boolean logQuery(String query,
	                                                  ServletContext servletContext,
	                                                  HttpServletRequest request,
	                                                  int statusCode,
	                                                  int contentLen,
	                                                  int numSearchResults,
	                                                  int totalRecords,
	                                                  int rank,
	                                                  int searchType) {

		if (logger == null) {
			String logfile =
				(String) servletContext.getAttribute("queryLogFile");

			// If no log file location has been configured, do nothing:
			if (logfile == null) {
				return false;
			}

			// LogManager will create the file if needs to but the surrounding directory must already exist.
			try {
				logger = DleseLogManager.getClfLogger(
					Level.FINEST,
				// minimum level this logger will accept
					true,
				// append
					logfile);
				// output log file
			} catch (LogException dle) {
				System.err.println("Error: Cannot init ClfLogger: " + dle);
				return false;
			}
		}

		// Log the search:
		logger.log(
		// message importance
			Level.SEVERE,
		// the incoming request
			request,
		// returned http status code
			statusCode,
		// returned content len, bytes
			contentLen,
		// type: search, full, or other
			"search",
		// num search results
			numSearchResults,
		// total num records in the database
			totalRecords,
		// rankNum: if search: rank of first rec in page
		// if full: rank of this rec
			rank,
		// dleseId: if full: dlese ID of this record
			null,
		// xmlStg.  Will appear in double quotes.
		// Internal backslashes, quotes, double quotes,
		// newlines, etc. will be escaped.
			getQueryLogEntry(request, query, searchType).toString());

		return true;
	}


	/**
	 *  Get a string for the free-text portion of the query log entry. The text contains the keywords entered by
	 *  the user as well as all the metadata criteria selected in the interface at the time of the search.
	 *
	 * @param  req         The HTTP request.
	 * @param  query       DESCRIPTION
	 * @param  searchType  DESCRIPTION
	 * @return             A string suitable for output in the free-text portion of a query log entry.
	 */
	private final static StringBuffer getQueryLogEntry(HttpServletRequest req, String query, int searchType) {
		StringBuffer out = new StringBuffer();

		String sessionId = null;
		HttpSession httpSession = req.getSession(false);
		if (httpSession == null) {
			sessionId = "";
		}
		else {
			sessionId = httpSession.getId();
		}

		String xmlFormat = null;
		String client = null;

		// Some params used by the web service interfaces
		if (searchType > 2) {
			client = req.getParameter("client");
			if (searchType == SEARCHTYPE_ODL_SEARCH) {
				xmlFormat = req.getParameter("metadataPrefix");
			}
			else {
				xmlFormat = req.getParameter("xmlFormat");
			}
		}

		out.append("<qle>");
		out.append("<q>" + (query == null ? "" : query) + "</q>");
		out.append("<meta>");
		out.append("<gr>" + getParameterValues("gr", req) + "</gr>");
		out.append("<re>" + getParameterValues("re", req) + "</re>");
		out.append("<su>" + getParameterValues("su", req) + "</su>");
		out.append("<cs>" + getParameterValues("cs", req) + "</cs>");
		out.append("<ky>" + getParameterValues("ky", req) + "</ky>");
		out.append("<type>" + SEARCH_TYPE_NAMES[searchType] + "</type>");
		if (xmlFormat != null) {
			out.append("<xmlFormat>" + xmlFormat + "</xmlFormat>");
		}
		if (client != null) {
			out.append("<client>" + client + "</client>");
		}
		out.append("<session>" + sessionId + "</session>");
		out.append("</meta></qle>");
		return out;
	}


	/**
	 *  Gets the parameterValues attribute of the DDSQueryAction object
	 *
	 * @param  param
	 * @param  request
	 * @return          The parameterValues value
	 */
	private static StringBuffer getParameterValues(String param, HttpServletRequest request) {
		String[] vals = request.getParameterValues(param);
		if (vals == null || vals.length == 0) {
			return new StringBuffer();
		}
		StringBuffer tmp = new StringBuffer();
		tmp.append(vals[0]);
		for (int i = 1; i < vals.length; i++) {
			tmp.append("," + vals[i]);
		}
		return tmp;
	}


	/**
	 *  Gets the metadata vocab criteria selected at the time of the query formatted in a mannor suitable to be
	 *  output in the free-text portion of the query log entry.
	 *
	 * @param  metadataVocabString  The metadata input criteria as returned by the current
	 *      MetadataVocabInputState object
	 * @return                      The metadata vocab criteria selected at the time of the query formatted in a
	 *      mannor suitable to be output in the free-text portion of the query log entry.
	 */
	private String getVocabLogString(String metadataVocabString) {
		StringBuffer out = new StringBuffer();
		out.append("<lc>" + getVocabStripped("learningcontext", metadataVocabString) + "</lc>");
		out.append("<rt>" + getVocabStripped("resourcetype", metadataVocabString) + "</rt>");
		return out.toString();
	}


	/**
	 *  Creates a comma-separated string of all user-selected terms for a particular vocab category. Assumens
	 *  that the string passed out is in the format output by the MetadataVocabInputState object.
	 *
	 * @param  vocabCategory        The category of vocab terms that should be extracted, for example
	 *      'learningcontext' or 'resourcetype'
	 * @param  metadataVocabString  The MetadataVocabInputState object's output string containing the current
	 *      vocabs selected.
	 * @return                      A comma-separated list of terms of the given type.
	 */
	private String getVocabStripped(String vocabCategory, String metadataVocabString) {
		//System.out.println("### the string to parse: " + metadataVocabString + " ###");

		Pattern pattern = Pattern.compile(vocabCategory + ":\"[^\"]*\"");
		Matcher matcher = pattern.matcher(metadataVocabString);
		String stripped = "";
		while (matcher.find()) {
			stripped += matcher.group();
		}
		//System.out.println("\n\nRegex: " + stripped + "\n\n");
		pattern = Pattern.compile(vocabCategory + ":");
		matcher = pattern.matcher(stripped);
		stripped = matcher.replaceAll("");
		//System.out.println("\n\nRegex2: " + stripped + "\n\n");

		pattern = Pattern.compile("\"\"");
		matcher = pattern.matcher(stripped);
		stripped = matcher.replaceAll(",");
		//System.out.println("\n\nRegex3: " + stripped + "\n\n");

		pattern = Pattern.compile("\"");
		matcher = pattern.matcher(stripped);
		stripped = matcher.replaceAll("");
		//System.out.println("\n\nRegex4: " + stripped + "\n\n");

		return stripped;
	}


	// -------------- Debug ------------------

	/**
	 *  Sets the debug attribute of the DDSQueryAction class
	 *
	 * @param  isDebugOutput  The new debug value
	 */
	public static void setDebug(boolean isDebugOutput) {
		debug = isDebugOutput;
	}


	/**
	 *  Print a line to standard out.
	 *
	 * @param  s  The String to print.
	 */
	private static void prtln(String s) {
		if (debug) {
			System.out.println("DDSQueryAction: " + s);
		}
	}


	/**
	 *  Print a line to error out.
	 *
	 * @param  s  The String to print.
	 */
	private static void prtlnErr(String s) {
		System.err.println("DDSQueryAction error: " + s);
	}
}


