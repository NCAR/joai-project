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
package org.dlese.dpc.schemedit.action;

import org.dlese.dpc.repository.*;
import org.dlese.dpc.index.*;
import org.dlese.dpc.schemedit.action.form.*;
import org.dlese.dpc.schemedit.*;
import org.dlese.dpc.schemedit.dcs.*;
import org.dlese.dpc.schemedit.config.*;
import org.dlese.dpc.schemedit.display.SortWidget;
import org.dlese.dpc.schemedit.security.user.User;
import org.dlese.dpc.schemedit.security.user.UserManager;
import org.dlese.dpc.index.reader.*;
import org.dlese.dpc.index.queryParser.FieldExpansionQueryParser;
import org.dlese.dpc.xml.*;
import org.dlese.dpc.dds.action.DDSQueryAction;
import org.apache.lucene.search.*;
import org.apache.lucene.queryParser.QueryParser;
import org.dlese.dpc.oai.*;
import org.dlese.dpc.webapps.tools.GeneralServletTools;

import java.util.*;
import java.io.*;
import java.net.*;
import java.util.regex.*;
import java.util.Hashtable;
import java.util.Locale;
import javax.servlet.*;
import javax.servlet.http.*;
import org.apache.lucene.index.*;
import org.apache.struts.action.Action;
import org.apache.struts.action.ActionError;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionServlet;
import org.apache.struts.util.MessageResources;
import org.apache.struts.util.LabelValueBean;
import java.net.URLEncoder;
import java.net.URLDecoder;
import java.net.URI;
import org.dom4j.Document;

/**
 *  A Struts Action for handling query requests that access a {@link
 *  org.dlese.dpc.index.SimpleLuceneIndex}. This class works in conjunction with
 *  the {@link org.dlese.dpc.schemedit.action.form.DCSQueryForm} Struts form
 *  bean class.
 *
 * @author    Jonathan Ostwald
 */
public final class DCSQueryAction extends DCSAction {

	private static boolean debug = true;
	/**  DESCRIPTION */
	public static String SELECT_ALL = " -- All --";
	/**  DESCRIPTION */
	public static String SELECT_NONE = " -- Select None --";
	/**  DESCRIPTION */
	public static String NO_ERRORS = "Records with no errors";

	long indexLastModified = -1;
	long collectionConfigMod = -1;

	// --------------------------------------------------------- Public Methods

	/**
	 *  Processes the specified HTTP request and creates the corresponding HTTP
	 *  response by forwarding to a JSP that will create it. A {@link
	 *  org.dlese.dpc.index.SimpleLuceneIndex} must be available to this class via
	 *  a ServletContext attribute under the key "index." Returns an {@link
	 *  org.apache.struts.action.ActionForward} instance that maps to the Struts
	 *  forwarding name "browse.query," which must be configured in struts-config.xml
	 *  to forward to the JSP page that will handle the request.
	 *
	 * @param  mapping               The ActionMapping used to select this instance
	 * @param  request               The HTTP request we are processing
	 * @param  response              The HTTP response we are creating
	 * @param  form                  The ActionForm for the given page
	 * @return                       The ActionForward instance describing where
	 *      and how control should be forwarded
	 * @exception  IOException       if an input/output error occurs
	 * @exception  ServletException  if a servlet exception occurs
	 */
	public ActionForward execute(
	                             ActionMapping mapping,
	                             ActionForm form,
	                             HttpServletRequest request,
	                             HttpServletResponse response)
		 throws IOException, ServletException {
		/*
		 *  Design note:
		 *  Only one instance of this class gets created for the app and shared by
		 *  all threads. To be thread-safe, use only local variables, not instance
		 *  variables (the JVM will handle these properly using the stack). Pass
		 *  all variables via method signatures rather than instance vars.
		 */
		DCSQueryForm queryForm = (DCSQueryForm) form;

		ActionErrors errors = initializeFromContext(mapping, request);
		if (!errors.isEmpty()) {
			saveErrors(request, errors);
			return (mapping.findForward("error.page"));
		}

		initializeDisplayParams(mapping, form, request);

		queryForm.setRequest(request);
		ServletContext servletContext = servlet.getServletContext();

		SchemEditUtils.showRequestParameters(request);

		queryForm.setContextURL(GeneralServletTools.getContextUrl(request));

		// SchemEditUtils.showRoleInfo (sessionUser, mapping);
		SessionBean sessionBean = this.getSessionBean(request);
		User sessionUser = this.getSessionUser(sessionBean);
		List sets = repositoryService.getAuthorizedSets(sessionUser, this.requiredRole);
		sessionBean.setSets(sets);

		// handle requests specifying a "command"
		if (request.getParameter("command") != null) {
			String param = request.getParameter("command");

			if (param.equalsIgnoreCase("showQueryOptions")) {
				queryForm.setShowQueryOptions("true");
			}
			else if (param.equalsIgnoreCase("hideQueryOptions")) {
				queryForm.setShowQueryOptions("false");
			}
			else if (param.equalsIgnoreCase("setNumPagingRecords")) {
				String resultsPerPage = request.getParameter("resultsPerPage");
				if (resultsPerPage != null) {
					queryForm.setResultsPerPage(resultsPerPage);
					int pagingRecords = queryForm.getNumPagingRecords();
					if (resultsPerPage.equalsIgnoreCase("all")) {
						pagingRecords = 1000;
					}
					else {
						try {
							pagingRecords = Integer.parseInt(resultsPerPage);
						} catch (Exception e) {
							prtlnErr("pagingRecord parse error: " + e.getMessage());
						}
					}
					queryForm.setNumPagingRecords(pagingRecords);
				}
			}

			return mapping.findForward("browse.query");
		}

		// Handle the sorting requests:
		String sortField = request.getParameter("sortField");
		if (sortField != null && sortField.length() > 0) {
			// prtln ("SORT FIELD: " + sortField);
			queryForm.setSortField(sortField);
			SortWidget currentWidget = queryForm.getCurrentSortWidget();
			String sortOrder = request.getParameter("sortOrder");
			for (Iterator i = queryForm.getSortWidgets().values().iterator(); i.hasNext(); ) {
				SortWidget sw = (SortWidget) i.next();
				if (sortOrder != null && sw == currentWidget) {
					// set the sort order of the current widget to the specified order
					try {
						int order = Integer.parseInt(sortOrder);
						sw.setOrder(order);
					} catch (Exception e) {
						prtlnErr("sortOrder error: " + e.getMessage());
					}
				}
				else {
					// set the sort order of the non-current widget to it's default order
					sw.setOrder(sw.getDefaultOrder());
				}
			}
		}

		// All other requests hanled by the search handler.
		return handleMetadataSearchRequest(sessionBean, mapping, (DCSQueryForm) form, request, response);
	}


	/**
	 *  Sets parameters used in display of search results, including the current
	 *  record and the starting record for the results to be displayed.
	 *
	 * @param  mapping
	 * @param  form
	 * @param  request
	 */
	private void initializeDisplayParams(ActionMapping mapping,
	                                     ActionForm form,
	                                     HttpServletRequest request) {

		DCSQueryForm queryForm = (DCSQueryForm) form;
		SessionBean sessionBean = this.getSessionBean(request);

		// important: initialize searchHelper (results) so that it will be present even for non-query requests.
		SearchHelper searchHelper = sessionBean.getSearchHelper();
		queryForm.setResults(searchHelper);

		// Initialize currentRecId - making sure it is contained in searchList
		String currentRecId = sessionBean.getRecId();
		if (currentRecId != null && currentRecId.trim().length() > 0) {

			// is currentRec still in search results (or has it been changed so
			// that it is no longer a search hit?)
			int indexOfCurrentRec = searchHelper.getIndexOf(currentRecId);
			if (indexOfCurrentRec == -1) {
				// no longer a search hit - use the cachedRecIndex to determine
				// a new currentRec (the rec preceeding the former currentRec)
				int cachedRecIndex = searchHelper.getCachedRecIndex();
				if (searchHelper.getNumHits() > 1 && cachedRecIndex > 0) {
					currentRecId = searchHelper.getRecId(cachedRecIndex - 1);
				}
				else {
					currentRecId = searchHelper.getRecId(cachedRecIndex);
				}
			}

			searchHelper.setCurrentRecId(currentRecId);
			sessionBean.setRecId(currentRecId); // causes this record to be highlighted in result list.
			queryForm.setStart(queryForm.getPaigingParam());
		}
		else {
			// no current rec - set up params for paging:
			if (request.getParameter("s") != null) {
				try {
					queryForm.setStart(Integer.parseInt(request.getParameter("s").trim()));
				} catch (Throwable e) {
					queryForm.setStart(0);
				}
			}
			else {
				queryForm.setStart(0);
			}
		}
	}


	/**
	 *  Collect all the values for "lastEditor" in the index for the collections
	 *  the user is authorized to view.
	 *
	 * @param  sb  NOT YET DOCUMENTED
	 * @return     The editors value
	 */
	private List getEditors(SessionBean sb) {
		String lastEditorFieldName = DcsDataFileIndexingPlugin.FIELD_NS + "lastEditor";
		List names = findTermsInAuthorizedCollections(sb, lastEditorFieldName);
		Collections.sort(names, new LastEditorComparator());
		return names;
	}

	private List getCreators(SessionBean sb) {
		String recordCreatorFieldName = DcsDataFileIndexingPlugin.FIELD_NS + "recordCreator";
		List names = findTermsInAuthorizedCollections(sb, recordCreatorFieldName);
		Collections.sort(names, new LastEditorComparator());
		return names;
	}

	/**
	 *  Return the terms that occur as values for a given indexField within the set
	 *  of collections the current user is authorized to access.<p>
	 *
	 *  For each of the terms that occur in the index for the given indexField,
	 *  perform a query to determine if the term occurs in an authorized
	 *  collection, and if so, add it to the list of terms to be returned.
	 *
	 * @param  sb          NOT YET DOCUMENTED
	 * @param  indexField  NOT YET DOCUMENTED
	 * @return             NOT YET DOCUMENTED
	 */
	private List findTermsInAuthorizedCollections(SessionBean sb, String indexField) {
		// prtln ("findTermsInAuthorizedCollections()");
		SimpleLuceneIndex index = this.repositoryManager.getIndex();
		List allTerms = index.getTerms(indexField);

		// query object that ORs together the authorized collections for sessionUser
		BooleanQuery colQuery = new BooleanQuery();
		// List authorizedCollections = getAuthorizedCollections(sb);
		List authorizedCollections = sb.getAuthorizedCollections();
		for (int i = 0; i < authorizedCollections.size(); i++) {
			// OR the collections
			colQuery.add(new TermQuery(new Term("collection",
				"0" + authorizedCollections.get(i))),
				BooleanClause.Occur.SHOULD);
		}

		List filteredTerms = new ArrayList();
		if (allTerms != null) {
			// prtln (allTerms.size() + " terms found for " + indexField);
			for (Iterator i = allTerms.iterator(); i.hasNext(); ) {
				String term = (String) i.next();
				if (term != null && term.trim().length() == 0)
					continue;

				// create a query that restricts colQuery by requiring "term"
				BooleanQuery bq = new BooleanQuery();
				bq.add(colQuery, BooleanClause.Occur.MUST);
				bq.add(new TermQuery(new Term(indexField, term)), BooleanClause.Occur.MUST);

				ResultDocList resultDocs = index.searchDocs(bq);
				if (resultDocs != null && resultDocs.size() > 0) {
					filteredTerms.add(term);
				}
			}
		}
		return filteredTerms;
	}


	/**
	 *  Handle a request to search over metadata collections and forwared to the
	 *  appropriate jsp page to render the response.
	 *
	 * @param  mapping               The ActionMapping used to select this instance
	 * @param  request               The HTTP request we are processing
	 * @param  response              The HTTP response we are creating
	 * @param  queryForm             DESCRIPTION
	 * @param  sessionBean           NOT YET DOCUMENTED
	 * @return                       The ActionForward instance describing where
	 *      and how control should be forwarded
	 * @exception  IOException       if an input/output error occurs
	 * @exception  ServletException  if a servlet exception occurs
	 */
	public ActionForward handleMetadataSearchRequest(
	                                                 SessionBean sessionBean,
	                                                 ActionMapping mapping,
	                                                 DCSQueryForm queryForm,
	                                                 HttpServletRequest request,
	                                                 HttpServletResponse response)
		 throws IOException, ServletException {

		SimpleLuceneIndex index =
			(SimpleLuceneIndex) servlet.getServletContext().getAttribute("index");

		if (index == null) {
			throw new ServletException("The attribute \"index\" could not be found in the Servlet Context.");
		}

		ActionErrors errors = new ActionErrors();

		// Set up query selector data used in query jsp - why is this done HERE?
		setQuerySelectors(sessionBean);

		// String paramVal = ""; // NEVER USED!
		try {

			// Handle queries:
			if (request.getParameter("q") != null) {
				// ---------- make boolean Query
				BooleanQuery booleanQuery = dcsStandardQuery(
					request,
					this.repositoryManager,
					sessionBean,
					queryForm,
					servlet.getServletContext());

				// prtln("booleanQuery: " + booleanQuery.toString());

				SearchHelper searchHelper = sessionBean.getSearchHelper();
				
				searchHelper.search(booleanQuery, queryForm.getCurrentSortWidget());
				// prtln("Search results: " + searchHelper.getNumHits());


				// stash the query results (encapsulated by searchHelper)
				queryForm.setResults(searchHelper);

				// ------------------------

				setNonPaigingParams(queryForm, request);
				setSortWidgetParams(queryForm, request);

				// Handle reports queries:
				if (request.getParameter("report") != null) {

					// The title for the report is the content of the parameter "report"
					queryForm.setReportTitle(request.getParameter("report"));
					return mapping.findForward("report.query");
				}

				return mapping.findForward("browse.query");
			}

			/* Display the text in the given file (handles viewing source, validating XML for admin
				e.g., Files that could not be indexed
			*/
			else if (request.getParameter("file") != null) {
				ResultDocList resultDocs = null;

				String fileName = request.getParameter("file");

				if (index != null) {
					resultDocs = index.searchDocs("docsource:\"" + fileName + "\"");
				}

				if (resultDocs == null || resultDocs.size() == 0) {
					resultDocs = index.searchDocs("docsource:\"" + OAIUtils.encode(fileName).replaceAll("%2F", "/") + "\"");
				}

				if (resultDocs == null || resultDocs.size() == 0) {
					errors.add("message", new ActionError("generic.message",
						"No data available for File \"" + request.getParameter("file") + "\""));
					saveErrors(request, errors);
					return mapping.findForward("data.display");
				}
				String metadata = ((FileIndexingServiceDocReader) ((ResultDoc)resultDocs.get(0)).getDocReader()).getFullContent();
				if (metadata != null && metadata.length() > 0) {
					// Filter out the XML and DTD declaration if requested
					if (request.getParameter("filterxml") != null) {
						metadata = XMLConversionService.stripXmlDeclaration(new BufferedReader(new StringReader(metadata))).toString();
					}
					queryForm.setMetadata(metadata);
					response.setContentType("text/plain");
					return mapping.findForward("data.display");
				}
			}
			// Display the text in the given file given an item id (handles "view XML" & "Validate Record")
			else if (request.getParameter("fileid") != null) {
				ResultDocList resultDocs = null;

				String id = request.getParameter("fileid");

				if (index != null) {
					resultDocs = index.searchDocs("id:" + SimpleLuceneIndex.encodeToTerm(id));
				}

				if (resultDocs == null || resultDocs.size() == 0) {
					errors.add("message", new ActionError("generic.message",
						"No data available for ID \"" + request.getParameter("fileid") + "\""));
					saveErrors(request, errors);
					return mapping.findForward("data.display");
				}

				String metadataPath = ((FileIndexingServiceDocReader) ((ResultDoc)resultDocs.get(0)).getDocReader()).getDocsource();
				String metadata = SchemEditUtils.getEditableXml(new File(metadataPath));
				if (metadata != null && metadata.length() > 0) {
					// Filter out the XML and DTD declaration if requested
					if (request.getParameter("filterxml") != null) {
						metadata = XMLConversionService.stripXmlDeclaration(new BufferedReader(new StringReader(metadata))).toString();
					}

					queryForm.setMetadata(metadata);
					response.setContentType("text/plain");
					return mapping.findForward("data.display");
				}
				else {
					String file = ((XMLDocReader) ((ResultDoc)resultDocs.get(0)).getDocReader()).getDocsource();
					errors.add("message", new ActionError("generic.message",
						"No data available for ID \"" + request.getParameter("fileid") + "\", file " + file));
					saveErrors(request, errors);
					return mapping.findForward("data.display");
				}
			}

			// No recognizable param existed:
			else if (request.getParameterNames().hasMoreElements()) {
				errors.add("error", new ActionError("generic.error",
					"The request is not valid in this context."));
				saveErrors(request, errors);
				return mapping.findForward("browse.query");
			}

			// If there were no parameters at all:
			return mapping.findForward("browse.query");
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

			return mapping.findForward("browse.query");
		}
	}


	/**
	 *  Return a query object that will find no records.
	 *
	 * @param  rm  NOT YET DOCUMENTED
	 * @return     The emptyQuery value
	 */
	public static BooleanQuery getEmptyQuery(RepositoryManager rm) {

		QueryParser qp = rm.getIndex().getQueryParser();
		BooleanQuery emptyQuery = new BooleanQuery();
		try {
			emptyQuery.add(qp.parse("allrecords:false"), BooleanClause.Occur.MUST);
		} catch (Throwable t) {
			prtlnErr("empty query clause could not be parsed: " + t.getMessage());
		}
		return emptyQuery;
	}


	/**
	 *  Create a query object to search for results specified in request, with
	 *  custom query expansion.<p>
	 *
	 *  Returns empty query if query param cannot be parsed.
	 *
	 * @param  request      the Request
	 * @param  rm           the RepositoryManager
	 * @param  sessionBean  the SessionBean
	 * @param  form         the ActionForm been
	 * @param  context      the servletContext
	 * @return              a query object
	 */
	public static BooleanQuery dcsStandardQuery(
	                                            HttpServletRequest request,
	                                            RepositoryManager rm,
	                                            SessionBean sessionBean,
	                                            ActionForm form,
	                                            ServletContext context) {

		DCSQueryForm queryForm = (DCSQueryForm) form;
		QueryParser qp = rm.getIndex().getQueryParser();

		// The basic paramters used in the search. The remainder
		// are processed in getSelectionParamsQueryString
		String q = request.getParameter("q");
		String s = request.getParameter("s");
		String searchMode = request.getParameter("searchMode"); // may require different processing
		String sortField = request.getParameter("sortField"); // may require different processing

		// Remove question marks from the user's query (conflicts with Lucene syntax)
		if (q != null) {
			q = q.replaceAll("\\?", " ");
			// remove plus signs (messes up handling of q by javascript in UI
			q = q.replaceAll("\\+", " ");
			q = q.trim();

			if (q.length() > 0) {
				// if q can't be parsed, return empty query
				try {
					qp.parse(q);
				} catch (Exception e) {
					prtlnErr("WARNING: query string could not be parsed: " + e.getMessage());
					return getEmptyQuery(rm);
				}
			}
			queryForm.setQ(q);
		}

		Query basicQuery = null;
		Exception serviceException = null;
		boolean sortByRelevance = false;
		if (searchMode != null && !q.matches(".*\\s+.*") && 
			(searchMode.equals("id") || searchMode.equals("url"))) {
			try {
				basicQuery = formatSearchModeQuery(q, searchMode, rm, context);
			} catch (org.apache.lucene.queryParser.ParseException pe) {
				prtln("getSearchModeQuery error: " + pe.getMessage());
				serviceException = pe;
			}
		}
		
		else if (searchMode != null && q.trim().length() > 0 && searchMode.equals("status_note")) {
			try {
				basicQuery = formatSearchModeQuery(q, searchMode, rm, context);
			} catch (org.apache.lucene.queryParser.ParseException pe) {
				prtln("getSearchModeQuery error: " + pe.getMessage());
				serviceException = pe;
			}
		}
		else {
			// Parse the basic query
			try {
				basicQuery = formatQuery(q, rm, context);
				// if there was a query, sort results by Relevance
				if (q != null && q.trim().length() > 0)
					sortByRelevance = true;
			} catch (org.apache.lucene.queryParser.ParseException pe) {
				prtln("formatQuery error: " + pe.getMessage());
				serviceException = pe;
			}
		}

		/* In most cases we want to leave the sortField as it was. But if
		   a searchString was provided, and there wasn't a sort specified
		   by the request, then we make the sort by relevance. Otherwise
		   then we use default ordering
		*/
		if (sortField == null) {
			if (sortByRelevance) {
				queryForm.setSortField(queryForm.RELEVANCE_REC_SORT);
			}
			else {
				// there was not a search string
				queryForm.setSortField(queryForm.DEFAULT_REC_SORT);
			}
		}

		String constraintsQuery = sessionBean.getCollectionsQueryClause();

		// Limit search to the fields indicated by user selections
		constraintsQuery += getSelectionParamsQueryString(request, queryForm, rm, sessionBean);

		BooleanQuery booleanQuery = new BooleanQuery();
		try {
			if (basicQuery != null) {
				booleanQuery.add(basicQuery, BooleanClause.Occur.MUST);
			}
			booleanQuery.add(qp.parse(constraintsQuery), BooleanClause.Occur.MUST);

			// System.out.println("final Query: '" + booleanQuery + "'");
		} catch (Throwable e) {
			prtlnErr("parse error: " + e);
		}

		// if Query.toString() fails, then we have a bad query (probably of the form myfield:"")
		try {
			String foo = booleanQuery.toString();
		} catch (Throwable t) {
			// prtlnErr ("bad query - returning empty query");
			booleanQuery = getEmptyQuery(rm);
		}
		
		return booleanQuery;
	}


	private final static Query formatSearchModeQuery(String q, String queryMode, RepositoryManager rm, ServletContext context)
		 throws org.apache.lucene.queryParser.ParseException {

		prtln ("formatSearchModeQuery()");
		if (queryMode.equals("id")) {
			q = "id:" + SimpleLuceneIndex.encodeToTerm(q, false);
		}
		if (queryMode.equals("url")) {
			q = "urlenc:" + SimpleLuceneIndex.encodeToTerm(q, false);
		}
		if (queryMode.equals("status_note")) {
			// q = "dcsstatusEntryNote:" + SimpleLuceneIndex.encodeToTerm(q, false);
			q = "dcsstatusEntryNote:" + SchemEditUtils.quoteWrap(q);
		}

		QueryParser qp = rm.getIndex().getQueryParser();
		BooleanQuery simpleQuery = new BooleanQuery();
		simpleQuery.add(qp.parse(q), BooleanClause.Occur.MUST);
		return simpleQuery;
	}


	/**
	 *  Formats a given Query q for use with the standard Lucene query parser.
	 *  Assumes documents have been indexed using ItemFileIndexingWriter, etc.
	 *
	 * @param  q                                                 A keyword query
	 *      typed by a user.
	 * @param  rm                                                The
	 *      RepositoryManager
	 * @param  context                                           The servlet
	 *      context
	 * @return                                                   The query
	 *      formatted for boosting title and for searching URLs and IDs.
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
		} catch (Exception e) {
			prtlnErr("formatQuery(): " + e);
			throw new org.apache.lucene.queryParser.ParseException("query could not be processed: " + e.getMessage());
		}

		String fullQuery = null;

		// If we performed a replacement above, use is:
		if (sb.length() > 0) {
			fullQuery = sb.toString();
		}
		else {
			fullQuery = q;
		}

		// Use a expansion query to apply custom boosting, stemming, virtual fields etc.
		Query luceneQuery = null;
		String[] defaultSearchFields = new String[]{"stems", "admindefault"};
		String[] fieldsUsedForBoosting = new String[]{"title", "titlestems", "description", "default"};
		Map boostingValues = new HashMap();
		boostingValues.put("description", new Float("2.0"));
		try {

			luceneQuery = FieldExpansionQueryParser.parse(
				fullQuery,
				rm.getIndex().getAnalyzer(),
				defaultSearchFields,
				fieldsUsedForBoosting,
				boostingValues,
				null,
				rm.getIndex().getLuceneOperator());
		} catch (org.apache.lucene.queryParser.ParseException pe) {
			prtlnErr("query parse: " + pe.getMessage());
		} catch (Throwable t) {
			prtlnErr("Unknown query parse: " + t.getMessage());
		}

		return luceneQuery;
	}


	/**
	 *  Gets the selectionParamsQueryString attribute of the DCSQueryAction class
	 *
	 * @param  request      NOT YET DOCUMENTED
	 * @param  queryForm    NOT YET DOCUMENTED
	 * @param  sessionBean  NOT YET DOCUMENTED
	 * @param  rm           NOT YET DOCUMENTED
	 * @return              The selectionParamsQueryString value
	 */
	public static String getSelectionParamsQueryString(HttpServletRequest request,
	                                                   DCSQueryForm queryForm,
	                                                   RepositoryManager rm,
	                                                   SessionBean sessionBean) {
		String validity = request.getParameter("vld");
		String syncErrors = request.getParameter("syncErrors");
		// String selectedCollection = request.getParameter("sc"); // why have this AND selectedCollections?

		String[] selectedEditors = request.getParameterValues("ses");
		String[] selectedCreators = request.getParameterValues("srcs");
		String[] selectedStatuses = getSelectedStatuses(request, queryForm, sessionBean);
		String[] selectedCollections = request.getParameterValues("scs");
		String[] selectedFormats = request.getParameterValues("sfmts");

		String query = "";

		// Handle collection selection(s):
		if (selectedCollections != null &&
			selectedCollections[0].length() > 0) {
			String collectionsQuery = "(collection:" + selectedCollections[0];
			for (int i = 1; i < selectedCollections.length; i++) {
				collectionsQuery += " OR collection:" + selectedCollections[i];
			}
			collectionsQuery += ")";
			query += (query.length() > 0 ? " AND " : "") + collectionsQuery;
		}

		// handle last editor selection options
		if (selectedEditors != null &&
			selectedEditors.length > 0 &&
			!selectedEditors[0].equals(SELECT_ALL)) {

			String editorsQuery = "(dcslastEditor:" + selectedEditors[0];
			for (int i = 1; i < selectedEditors.length; i++) {
				editorsQuery += " OR dcslastEditor:" + selectedEditors[i];
			}
			editorsQuery += ")";
			query += (query.length() > 0 ? " AND " : "") + editorsQuery;
		}

		// handle record creator selection options
		if (selectedCreators != null &&
			selectedCreators.length > 0 &&
			!selectedCreators[0].equals(SELECT_ALL)) {

			String creatorsQuery = "(dcsrecordCreator:" + selectedCreators[0];
			for (int i = 1; i < selectedCreators.length; i++) {
				creatorsQuery += " OR dcsrecordCreator:" + selectedCreators[i];
			}
			creatorsQuery += ")";
			query += (query.length() > 0 ? " AND " : "") + creatorsQuery;
		}
		
		
		// Handle multiple formats selection option:
		if (selectedFormats != null &&
			selectedFormats.length > 0 &&
			!selectedFormats[0].equals(SELECT_ALL)) {

			String formatsQuery = "(metadatapfx:" + selectedFormats[0];
			for (int i = 1; i < selectedFormats.length; i++) {
				formatsQuery += " OR metadatapfx:" + selectedFormats[i];
			}
			formatsQuery += ")";
			query += (query.length() > 0 ? " AND " : "") + formatsQuery;
		}

		// handle statuses - wrap in quotes to preserve statuses containing spaces
		if (selectedStatuses != null &&
			selectedStatuses.length > 0 &&
			!selectedStatuses[0].equals(SELECT_ALL)) {

			String statusesQuery = "(dcsstatus:" + SchemEditUtils.quoteWrap(selectedStatuses[0]);
			for (int i = 1; i < selectedStatuses.length; i++) {
				statusesQuery += " OR dcsstatus:" + SchemEditUtils.quoteWrap(selectedStatuses[i]);
			}
			statusesQuery += ")";
			query += (query.length() > 0 ? " AND " : "") + statusesQuery;
		}

		// handle validity option
		if (validity != null && validity.length() > 0) {
			if (validity.equals("valid")) {
				query += (query.length() > 0 ? " AND " : "") + "dcsisValid:true";
			}
			if (validity.equals("notvalid")) {
				query += (query.length() > 0 ? " NOT " : "") + "dcsisValid:true";
			}
		}
		else {
			// default validity to all
			queryForm.setVld("");
		}

		// handle syncErrors
		if (syncErrors != null && syncErrors.length() > 0) {
			if (syncErrors.equals("true")) {
				query += (query.length() > 0 ? " AND " : "") + "dcshasSyncError:true";
			}
			if (syncErrors.equals("false")) {
				query += (query.length() > 0 ? " AND " : "") + "dcshasSyncError:false";
			}
		}

		// prtln ("\ngetSelectionParamsQueryString returning ...\n" + query);
		
		return query;
	}


	/**
	 *  Create a list of statusFlag values for querying the index.<p>
	 *
	 *  Expand each selected statusFlag labels into all the statusFlag values that
	 *  share this value, and return the union of all these values.
	 *
	 * @param  request      Description of the Parameter
	 * @param  queryForm    Description of the Parameter
	 * @param  sessionBean  NOT YET DOCUMENTED
	 * @return              The selectedStatuses value
	 */
	private static String[] getSelectedStatuses(HttpServletRequest request, DCSQueryForm queryForm, SessionBean sessionBean) {
		ArrayList statusValues = new ArrayList();
		String[] selectedStatusLabels = request.getParameterValues("sss");
		Map statusMap = sessionBean.getStatuses();

		if (selectedStatusLabels == null) {
			return null;
		}

		for (int i = 0; i < selectedStatusLabels.length; i++) {
			String label = selectedStatusLabels[i];
			List values = (List) statusMap.get(label);
			if (values != null) {
				for (Iterator vi = values.iterator(); vi.hasNext(); ) {
					String value = (String) vi.next();
					statusValues.add(value);
				}
			}
		}
		if (statusValues.size() == 0) {
			/*
			 the selected status labels are no longer in the statusMap. this will happen when:
			 1 - search is preformed for records with a reserved status,
			 2 - only one result is found, and then
			 3 - the status of the lone record is changed.
			 in this case, the statusMap will not contain the searched-for status (since there are
			 no records having this status), and therefore that value is not added to the statusValues list.
			 since we didn't add the value to statusValues, we have effectively removed it from the query,
			 so we must empty Sss so the sarch_user_selections will reflect this
			 */
			queryForm.setSss(new String[]{});
		}

		String[] ret = (String[]) statusValues.toArray(new String[]{});
		return ret;
	}


	/**
	 *  Sets the various querySelectors of the DCSQueryAction object, and stashes
	 *  them in the SessionBean so they are visible to JSP and other controllers.
	 *
	 * @param  sb  The new querySelectors value
	 */
	private void setQuerySelectors(SessionBean sb) {

		SimpleLuceneIndex index = repositoryManager.getIndex();

		if (!sb.isQuerySelectorsInitialized() ||
			index.getLastModifiedCount() > sb.getIndexLastModified() ||
			collectionRegistry.getCollectionConfigMod() > sb.getCollectionConfigMod()) {

			List indexedFormats = new ArrayList();
			List fmts = new ArrayList();
			for (Iterator i = sb.getSets().iterator(); i.hasNext(); ) {
				SetInfo setInfo = (SetInfo) i.next();
				String format = setInfo.getFormat();
				String value = "0" + format;
				if (!fmts.contains(format)) {
					fmts.add(format);
					indexedFormats.add(new LabelValueBean(format, value));
				}
			}

			Collections.sort(indexedFormats, new IndexedFormatComparator());

			Map statuses = getQueryStatusMap(sb);

			sb.setStatuses(statuses);
			sb.setEditors(getEditors(sb));
			sb.setCreators(getCreators(sb));
			sb.setIndexedFormats(indexedFormats);

			sb.setIndexLastModified(index.getLastModifiedCount());
			sb.setCollectionConfigMod(collectionRegistry.getCollectionConfigMod());
			sb.setQuerySelectorsInitialized(true);
		}
	}


	/**
	 *  Create a mapping from unique status labels to a list of all the status
	 *  values that have that label.
	 *
	 * @param  sb  NOT YET DOCUMENTED
	 * @return     A List of statuses
	 */
	public Map getQueryStatusMap(SessionBean sb) {
		// prtln ("getQueryStatusMap()");
		String statusFieldName = DcsDataFileIndexingPlugin.FIELD_NS + "status";
		List statusTerms = findTermsInAuthorizedCollections(sb, statusFieldName);

		// the status terms are status values
		Map statuses = new TreeMap(new CaseInsensitiveComparator());

		if (statusTerms != null) {
			// prtln ("adding indexed status terms to masterStatusList");
			for (Iterator sti = statusTerms.iterator(); sti.hasNext(); ) {
				String statusTerm = (String) sti.next();

				String label = null;
				if (StatusFlags.isFinalStatusValue(statusTerm)) {
					String collection = StatusFlags.getCollection(statusTerm);
					label = collectionRegistry.getFinalStatusLabel(collection);
				}
				else {
					label = statusTerm;
				}

				List values = (List) statuses.get(label);
				if (values == null) {
					values = new ArrayList();
				}
				values.add(statusTerm);
				statuses.put(label, values);
			}
		}
		return statuses;
	}


	/**
	 *  Comparator to sort Status values into alphabetical order.
	 *
	 * @author    ostwald
	 */
	class CaseInsensitiveComparator implements Comparator {

		/**
		 *  Description of the Method
		 *
		 * @param  o1  Description of the Parameter
		 * @param  o2  Description of the Parameter
		 * @return     Description of the Return Value
		 */
		public int compare(Object o1, Object o2) {
			String string1 = ((String) o1).toLowerCase();
			String string2 = ((String) o2).toLowerCase();
			return string1.compareTo(string2);
		}
	}


	/**
	 *  Comparator to sort metadata formats into alphabetical order.
	 *
	 * @author    ostwald
	 */
	class IndexedFormatComparator implements Comparator {

		/**
		 *  Description of the Method
		 *
		 * @param  o1  Description of the Parameter
		 * @param  o2  Description of the Parameter
		 * @return     Description of the Return Value
		 */
		public int compare(Object o1, Object o2) {
			String string1 = ((LabelValueBean) o1).getLabel().toLowerCase();
			String string2 = ((LabelValueBean) o2).getLabel().toLowerCase();
			return string1.compareTo(string2);
		}
	}

	class LastEditorComparator implements Comparator {
		
		UserManager userManager = (UserManager) servlet.getServletContext().getAttribute("userManager");
		
		private String getSortName (String username) {
			try {
				User user = this.userManager.getUser(username);
				return user.getLastName() + user.getFirstName();
			} catch (Throwable t) {}
			return username;
		}
		
		/**
		 *  Description of the Method
		 *
		 * @param  o1  Description of the Parameter
		 * @param  o2  Description of the Parameter
		 * @return     Description of the Return Value
		 */
		public int compare(Object o1, Object o2) {
			String userName1 = getSortName ((String)o1).toLowerCase();
			String userName2 = getSortName ((String)o2).toLowerCase();
			return userName1.compareTo(userName2);
		}
	}

	/**
	 *  Sets the nonPaigingParams attribute of the DCSQueryAction object (all
	 *  request params except those that control the search results page being
	 *  displayed).
	 *
	 * @param  queryForm  The new nonPaigingParams value
	 * @param  request    The new nonPaigingParams value
	 */
	private final void setNonPaigingParams(DCSQueryForm queryForm, HttpServletRequest request) {
		Hashtable paigingParams = new Hashtable(3);
		// paigingParams.put("q", "");
		paigingParams.put("s", "");
		paigingParams.put("resultsPerPage", "");
		queryForm.setNonPaigingParams(getParamsString(paigingParams, request));
	}


	/**
	 *  Sets the sortWidgetParams attribute of the DCSQueryAction object<p>
	 *
	 *  Ultimately used by the sortWidget (see tags/sortWidget.tag) to contstruct a
	 *  URL that will reproduce search but accomodate new sorting, ordering, and
	 *  paiging params.
	 *
	 * @param  queryForm  The new sortWidgetParams value
	 * @param  request    The new sortWidgetParams value
	 */
	private final void setSortWidgetParams(DCSQueryForm queryForm, HttpServletRequest request) {
		Hashtable excludeParams = new Hashtable(5);
		excludeParams.put("q", "");
		excludeParams.put("s", "");
		excludeParams.put("resultsPerPage", "");
		excludeParams.put("sortField", "");
		excludeParams.put("sortOrder", "");
		queryForm.setSortWidgetParams(getParamsString(excludeParams, request));
	}


	/**
	 *  Gets a paramsString of all request params excluding the supplied
	 *  "excludeParams"
	 *
	 * @param  excludeParams  Description of the Parameter
	 * @param  request        Description of the Parameter
	 * @return                The paramsString value
	 */
	private final String getParamsString(Hashtable excludeParams, HttpServletRequest request) {
		if (request == null) {
			return null;
		}
		Enumeration params = request.getParameterNames();
		String param;
		String vals[];
		StringBuffer addParams = new StringBuffer();
		try {
			while (params.hasMoreElements()) {
				param = (String) params.nextElement();
				if (!excludeParams.containsKey(param)) {
					vals = request.getParameterValues(param);
					/* we encode only the query string. all other params (except status) are single-token
					   and status has been quote-wrapped */
					for (int i = 0; i < vals.length; i++) {
						if (param.equals("q")) {
							addParams.append("&" + param + "=" + URLEncoder.encode(vals[i], "utf-8"));
						}
						else {
							addParams.append("&" + param + "=" + vals[i]);
						}
						//addParams.append("&" + param + "=" + URLEncoder.encode(vals[i], "utf-8"));
					}
				}
			}
		} catch (Exception e) {
			addParams.toString();
		}
		return addParams.toString();
	}


	// -------------- Debug ------------------


	/**
	 *  Sets the debug attribute of the DCSQueryAction class
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
			SchemEditUtils.prtln(s, "DCSQueryAction");
		}
	}


	private static void prtlnErr(String s) {
		SchemEditUtils.prtln(s, "DCSQueryAction");
	}

}

