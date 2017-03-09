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
package org.dlese.dpc.action;

import org.dlese.dpc.repository.*;
import org.dlese.dpc.index.*;
import org.dlese.dpc.action.form.*;
import org.dlese.dpc.index.reader.*;
import org.dlese.dpc.xml.*;
import org.apache.lucene.search.*;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.index.Term;
import org.dlese.dpc.oai.*;
import org.dlese.dpc.webapps.tools.GeneralServletTools;
import org.dlese.dpc.index.search.DateRangeFilter;

import java.util.*;
import java.io.*;
import java.util.Hashtable;
import java.util.Locale;
import javax.servlet.*;
import javax.servlet.http.*;
import org.apache.struts.action.Action;
import org.apache.struts.action.ActionError;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionServlet;
import org.apache.struts.util.MessageResources;
import java.net.URLEncoder;

/**
 *  A Struts Action for handling query requests that access a {@link org.dlese.dpc.index.SimpleLuceneIndex}.
 *  This class works in conjunction with the {@link org.dlese.dpc.action.form.SimpleQueryForm} Struts form
 *  bean class.<p>
 *
 *  This class works by processing the HTTP request and outputting the corresponding HTTP response by
 *  forwarding to a JSP that will create it. Returns an {@link org.apache.struts.action.ActionForward}
 *  instance that maps to the Struts forwarding name "simple.query," "report.query," "weblog.query," or
 *  "weblogreport.query" which must be configured in struts-config.xml to indicate the JSP page that will
 *  handle and render the request.<p>
 *
 *  An application that uses this class must set a {@link javax.servlet.ServletContext} attribute containing a
 *  {@link org.dlese.dpc.index.SimpleLuceneIndex} under the key "index." This index will be used to perform
 *  the query. Optionally there may also be a web log index uder the key "webLogIndex".
 *
 * @author     John Weatherley
 * @version    $Id: SimpleQueryAction.java,v 1.44 2010/07/14 00:18:48 jweather Exp $
 */
public final class SimpleQueryAction extends Action {

	private static boolean debug = false;
	/**  String to display for selecting all */
	public static String SELECT_ALL = " -- All --";
	List formats = new ArrayList();
	List collections = new ArrayList();
	List collectionLabels = new ArrayList();
	long indexLastModified = -1;

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
		/*
		 *  Design note:
		 *  Only one instance of this class gets created for the app and shared by
		 *  all threads. To be thread-safe, use only local variables, not instance
		 *  variables (the JVM will handle these properly using the stack). Pass
		 *  all variables via method signatures rather than instance vars.
		 */
		SimpleQueryForm sqf = (SimpleQueryForm) form;

		// Set up params for paging:
		if (request.getParameter("s") != null) {
			try {
				sqf.setStart(Integer.parseInt(request.getParameter("s").trim()));
			} catch (Throwable e) {}
		}
		sqf.setRequest(request);

		RepositoryManager rm =
			(RepositoryManager) servlet.getServletContext().getAttribute("repositoryManager");
		if (rm != null) {
			sqf.setContextURL(GeneralServletTools.getContextUrl(request));
		}

		// Handle the various requests:
		if (request.getParameter("searchOver") != null && request.getParameter("searchOver").equals("webLogs")) {
			return handleWebLogSearchRequest(mapping, (SimpleQueryForm) form, request, response);
		}

		// All other requests hanled by the search handler.
		return handleMetadataSearchRequest(mapping, (SimpleQueryForm) form, request, response, rm);
	}


	/**
	 *  Handle a request to search over metadata collections and forwared to the appropriate jsp page to render
	 *  the response.
	 *
	 * @param  mapping               The ActionMapping used to select this instance
	 * @param  request               The HTTP request we are processing
	 * @param  response              The HTTP response we are creating
	 * @param  queryForm             The ActionForm for the given page
	 * @param  rm                    The RepositoryManager
	 * @return                       The ActionForward instance describing where and how control should be
	 *      forwarded
	 * @exception  IOException       if an input/output error occurs
	 * @exception  ServletException  if a servlet exception occurs
	 */
	public ActionForward handleMetadataSearchRequest(
	                                                 ActionMapping mapping,
	                                                 SimpleQueryForm queryForm,
	                                                 HttpServletRequest request,
	                                                 HttpServletResponse response,
	                                                 RepositoryManager rm)
		 throws IOException, ServletException {

		SimpleLuceneIndex index =
			(SimpleLuceneIndex) servlet.getServletContext().getAttribute("index");

		if (index == null)
			throw new ServletException("The attribute \"index\" could not be found in the Servlet Context.");

		ActionErrors errors = new ActionErrors();

		// Set up some data used in beans:
		setCollectionsAndFormats(index, queryForm, rm);

		String paramVal = "";
		try {

			// Handle queries:
			if (request.getParameter("q") != null) {
				String query = formatQuery(request.getParameter("q"));
				
				if (query.equals("") || query.equals("*"))
					query = "(allrecords:true)";

				if (request.getParameter("rq") != null) {
					query += " AND " + formatQuery(request.getParameter("rq"));
				}
				prtln("formatQuery to: " + query);
				
				// Handle single collection selection option:
				String selectedCollection = request.getParameter("sc");
				if (selectedCollection != null &&
					selectedCollection.length() > 0 &&
					!selectedCollection.equals(SELECT_ALL)) {
					query = "(" + query + ") AND collection:" + selectedCollection;
				}

				// Handle multiple collections selection option:
				String[] selectedCollections = request.getParameterValues("scs");
				if (selectedCollections != null &&
					selectedCollections.length > 0
					 && !selectedCollections[0].equals(SELECT_ALL)) {
					query = "(" + query + ") AND (collection:" + selectedCollections[0];
					for (int i = 1; i < selectedCollections.length; i++)
						query += " OR collection:" + selectedCollections[i];
					query += ")";
				}

				// Handle statuses, such as OAI deleted, etc.:
				BooleanQuery statusQuery = null;				
				String [] selStatus = queryForm.getSelStatus();
				if (selStatus != null &&
					selStatus.length > 0
					 && !selStatus[0].equals(SELECT_ALL)) {
					statusQuery = new BooleanQuery();
					for (int i = 0; i < selStatus.length; i++) {
						if(selStatus[i].equals("deletedNotInDir"))
							statusQuery.add(rm.getDeletedDocsNotFromAnyDirectoryQuery(),BooleanClause.Occur.SHOULD);
						else
							statusQuery.add(new TermQuery(new Term("deleted",selStatus[i])),BooleanClause.Occur.SHOULD);
					}
				}				
				
				// Handle multiple OAI Sets selection:
				BooleanQuery setSpecQuery = null;
				String[] selectedSetSpecs = request.getParameterValues("selSetSpecs");
				if (selectedSetSpecs != null
					 && selectedSetSpecs.length > 0
					 && !selectedSetSpecs[0].equals(SELECT_ALL)) {
					
					for (int i = 0; i < selectedSetSpecs.length; i++) {
						Query lucQuery = rm.getOaiSetQuery(selectedSetSpecs[i]);
						if(lucQuery != null){
							if(setSpecQuery == null)
								setSpecQuery = new BooleanQuery();
							setSpecQuery.add(lucQuery,BooleanClause.Occur.SHOULD);
							prtln("search setSpec '" + selectedSetSpecs[i] + "'");
						}
					}
				}

				// Handle multiple formats selection option:
				String[] selectedFormats = request.getParameterValues("sfmts");
				ArrayList searchFormats = rm.getFormatsThatCanBeConvertedToFormats(selectedFormats);
				if (searchFormats != null &&
					searchFormats.size() > 0
					 && !((String) searchFormats.get(0)).equals(SELECT_ALL)) {
					query = "(" + query + ") AND (metadatapfx:0" + searchFormats.get(0);
					for (int i = 1; i < searchFormats.size(); i++)
						query += " OR metadatapfx:0" + searchFormats.get(i);
					query += ")";
				}

				// Handle Grade Range
				String[] gradeRanges = request.getParameterValues("gr");
				if (gradeRanges != null && gradeRanges.length > 0) {
					query = "(" + query + ") AND (gr:" + gradeRanges[0];
					for (int i = 1; i < gradeRanges.length; i++)
						query += " OR gr:" + gradeRanges[i];
					query += ")";
				}

				// Handle Resource Type
				String[] resourceTypes = request.getParameterValues("re");
				if (resourceTypes != null && resourceTypes.length > 0) {
					query = "(" + query + ") AND (re:" + resourceTypes[0];
					for (int i = 1; i < resourceTypes.length; i++)
						query += " OR re:" + resourceTypes[i];
					query += ")";
				}

				// Handle Standards
				String[] contentStandards = request.getParameterValues("cs");
				if (contentStandards != null && contentStandards.length > 0) {
					query = "(" + query + ") AND (cs:" + contentStandards[0];
					for (int i = 1; i < contentStandards.length; i++)
						query += " OR cs:" + contentStandards[i];
					query += ")";
				}

				ResultDocList resultDocs = null;
				//prtln("Default operator is: " + index.getOperatorString());
				if (index != null){
					Query userQuery = index.getQueryParser().parse(query);
					BooleanQuery finalQuery = new BooleanQuery();
					finalQuery.add(userQuery,BooleanClause.Occur.MUST);
					if(statusQuery != null)
						finalQuery.add(statusQuery,BooleanClause.Occur.MUST);
					if(setSpecQuery != null)
						finalQuery.add(setSpecQuery,BooleanClause.Occur.MUST);
					prtln("search query: " + finalQuery.toString());
					resultDocs = index.searchDocs(finalQuery);
				}
				//resultDocs = index.searchDocs(query, "admindefault");

				if (resultDocs != null)
					prtln("query: " + query + ". Num results: " + resultDocs.size());
				else
					prtln("query: " + query + " had zero results");

				queryForm.setResults(resultDocs);
				StringBuffer str;

				prtln("setResults done");

				setNonPaigingParams(queryForm, request);

				// Handle reports queries:
				if (request.getParameter("report") != null) {

					// The title for the report is the content of the parameter "report"
					queryForm.setReportTitle(request.getParameter("report"));
					return mapping.findForward("report.query");
				}

				return mapping.findForward("simple.query");
			}

			// Display a blank report page that lists the possible reports (defined above).
			else if (request.getParameter("report") != null && request.getParameter("report").equals("noreport")) {
				queryForm.setReportTitle("Select a report");
				return mapping.findForward("report.query");
			}

			// Display the record full view:
			else if (request.getParameter("fullview") != null) {
				String id = request.getParameter("fullview");
				if (id.length() == 0) {
					errors.add("message", new ActionError("generic.message",
						"No ID indicated. Please supply an ID number."));
					saveErrors(request, errors);
					return mapping.findForward("fullview.display");
				}

				ResultDocList resultDocs = null;

				if (index != null)
					resultDocs = index.searchDocs("id:" + SimpleLuceneIndex.encodeToTerm(id));

				if (resultDocs == null || resultDocs.size() == 0) {
					errors.add("message", new ActionError("generic.message",
						"No record was found in the index for ID \"" + id + "\""));
					saveErrors(request, errors);
					return mapping.findForward("fullview.display");
				}

				queryForm.setResult(resultDocs.get(0));
				return mapping.findForward("fullview.display");
			}

			// Display the record in the given format. Record id and metadataFormat should be in the same request:
			else if (request.getParameter("id") != null) {
				ResultDocList resultDocs = null;
				String id = request.getParameter("id");
				prtln("Getting record for id: " + id);
				if (index != null)
					resultDocs = index.searchDocs("id:" + SimpleLuceneIndex.encodeToTerm(id));

				if (resultDocs == null || resultDocs.size() == 0) {
					errors.add("message", new ActionError("generic.message",
						"No data available for record ID \"" + request.getParameter("id") + "\""));
					saveErrors(request, errors);
					return mapping.findForward("data.display");
				}

				// Grab the metadata XML:
				paramVal = request.getParameter("metadataFormat");
				String metadata = ((XMLDocReader) resultDocs.get(0).getDocReader()).getXmlFormat(paramVal, false);
				if (metadata != null && metadata.length() > 0) {
					// Ensure the XML declaration is present:
					// (?s) sets the DOTALL mode, making . match line endings as well as all other chars.
					if (!metadata.substring(0, 80).toLowerCase().matches("(?s).*<\\?xml.*version.*\\?>.*"))
						metadata = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" + metadata;
					queryForm.setMetadata(metadata);
					response.setContentType("text/plain");
					return mapping.findForward("data.display");
				}
				else {
					errors.add("message", new ActionError("generic.message",
						"Unable to display data in format \"" + paramVal + "\""));
					saveErrors(request, errors);
					return mapping.findForward("data.display");
				}
			}

			// Display the text in the given file:
			else if (request.getParameter("file") != null) {
				ResultDocList resultDocs = null;

				String fileName = request.getParameter("file");

				if (index != null)
					resultDocs = index.searchDocs("docsource:\"" + fileName + "\"");

				if (resultDocs == null || resultDocs.size() == 0)
					resultDocs = index.searchDocs("docsource:\"" + OAIUtils.encode(fileName).replaceAll("%2F", "/") + "\"");

				if (resultDocs == null || resultDocs.size() == 0) {
					errors.add("message", new ActionError("generic.message",
						"No data available for File \"" + request.getParameter("file") + "\""));
					saveErrors(request, errors);
					return mapping.findForward("data.display");
				}
				String metadata = ((FileIndexingServiceDocReader) resultDocs.get(0).getDocReader()).getFullContent();
				if (metadata != null && metadata.length() > 0) {
					// Filter out the XML and DTD declaration if requested
					if (request.getParameter("filterxml") != null)
						metadata = XMLConversionService.stripXmlDeclaration(new BufferedReader(new StringReader(metadata))).toString();
					queryForm.setMetadata(metadata);
					response.setContentType("text/plain");
					return mapping.findForward("data.display");
				}
			}
			// Display the text in the given file given an item id:
			else if (request.getParameter("fileid") != null) {
				ResultDocList resultDocs = null;

				String id = request.getParameter("fileid");

				if (index != null)
					resultDocs = index.searchDocs("id:" + SimpleLuceneIndex.encodeToTerm(id));

				if (resultDocs == null || resultDocs.size() == 0) {
					errors.add("message", new ActionError("generic.message",
						"No data available for ID \"" + request.getParameter("fileid") + "\""));
					saveErrors(request, errors);
					return mapping.findForward("data.display");
				}
				String metadata = ((XMLDocReader) resultDocs.get(0).getDocReader()).getFullContent();
				if (metadata != null && metadata.length() > 0) {
					// Filter out the XML and DTD declaration if requested
					if (request.getParameter("filterxml") != null)
						metadata = XMLConversionService.stripXmlDeclaration(new BufferedReader(new StringReader(metadata))).toString();
					queryForm.setMetadata(metadata);
					response.setContentType("text/plain");
					return mapping.findForward("data.display");
				}
				else {
					String file = ((XMLDocReader) resultDocs.get(0).getDocReader()).getDocsource();
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
				return mapping.findForward("simple.query");
			}

			// If there were no parameters at all:
			return mapping.findForward("simple.query");
		} catch (Throwable t) {
			if (queryForm != null) {
				errors.add("error", new ActionError("generic.error",
					"There was a server problem: " + t));
				saveErrors(request, errors);
				prtln("There was a server problem: " + t);
			}
			else
				prtln("There was a problem: " + t);

			t.printStackTrace();

			return mapping.findForward("simple.query");
		}
	}



	/**
	 *  Handle a request to search over web logs and forwared to the appropriate jsp page to render the response.
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
	public ActionForward handleWebLogSearchRequest(
	                                               ActionMapping mapping,
	                                               SimpleQueryForm form,
	                                               HttpServletRequest request,
	                                               HttpServletResponse response)
		 throws IOException, ServletException {

		SimpleLuceneIndex webLogIndex =
			(SimpleLuceneIndex) servlet.getServletContext().getAttribute("webLogIndex");

		if (webLogIndex == null)
			throw new ServletException("The attribute \"webLogIndex\" could not be found in the Servlet Context.");

		ActionErrors errors = new ActionErrors();

		// Grab the form bean:
		SimpleQueryForm queryForm = (SimpleQueryForm) form;

		String paramVal = "";
		try {

			// Handle queries:
			if (request.getParameter("q") != null) {
				String query = "doctype:0weblog AND " + request.getParameter("q");
				String rq = request.getParameter("rq");

				if (rq != null && !rq.trim().equals("*"))
					query += " AND " + formatQuery(rq);

				// Set up a date filter:
				DateRangeFilter dateFilter = DateRangeFilter.After("requestdate", 0);
				Sort sort = new Sort( new SortField("requestdate", SortField.STRING, true) );

				ResultDocList resultDocs = null;
				if (webLogIndex != null) {
					resultDocs = webLogIndex.searchDocs(query, "admindefault", dateFilter, sort);
				}

				if (resultDocs != null) {
					prtln("query: " + query + ". Num results: " + resultDocs.size());
				}
				else
					prtln("query: " + query + " had zero results");

				queryForm.setResults(resultDocs);

				setNonPaigingParams(queryForm, request);
				// Handle reports queries:
				if (request.getParameter("report") != null) {
					// The title for the report is the content of the parameter "report"
					queryForm.setReportTitle(request.getParameter("report"));
					return mapping.findForward("weblogreport.query");
				}

				return mapping.findForward("weblog.query");
			}

			// No recognizable param existed:
			else if (request.getParameterNames().hasMoreElements()) {
				errors.add("message", new ActionError("generic.message",
					"The web loq request is not valid in this context."));
				saveErrors(request, errors);
				return mapping.findForward("weblog.query");
			}

			prtln("No qualified params found");
			// If there were no parameters at all:
			return mapping.findForward("weblog.query");
		} catch (Throwable t) {
			if (queryForm != null) {
				errors.add("error", new ActionError("generic.error",
					"There was a server problem: " + t));
				saveErrors(request, errors);
				prtln("There was a server problem: " + t);
			}
			else
				prtln("There was a problem: " + t);

			t.printStackTrace();

			return mapping.findForward("weblog.query");
		}
	}


	private final String formatQuery(String q) {
		q = q.trim();
		if (q.startsWith("id:") && !q.matches(".*\\s+.*")) {
			q = q.substring(3, q.length());
			q = "id:" + SimpleLuceneIndex.encodeToTerm(q, false);
			return q;
		}
		if (q.startsWith("url:") && !q.matches(".*\\s+.*")) {
			q = q.substring(4, q.length());
			q = "urlenc:" + SimpleLuceneIndex.encodeToTerm(q, false);
			return q;
		}
		return q;
	}


	private final void setCollectionsAndFormats(
	                                            SimpleLuceneIndex index,
	                                            SimpleQueryForm queryForm,
	                                            RepositoryManager rm) {
		if (index.getLastModifiedCount() > indexLastModified) {
			indexLastModified = index.getLastModifiedCount();

			// Get collections in the index:
			List cols = index.getTerms("collection");
			collections = new ArrayList();
			collections.add(SELECT_ALL);
			if (cols != null)
				collections.addAll(cols);

			collectionLabels = new ArrayList();
			collectionLabels.add(SELECT_ALL);
			if (cols != null) {
				for (int i = 0; i < cols.size(); i++) {
					String col = (String) cols.get(i);
					collectionLabels.add(col.substring(1, col.length()));
				}
			}

			if (rm != null) {
				formats = new ArrayList();
				formats.add(SELECT_ALL);
				formats.addAll(rm.getAvailableFormatsList());
			}
		}
		prtln("adding collections and formats");
		queryForm.setCollections(collections);
		queryForm.setCollectionLabels(collectionLabels);
		queryForm.setFormats(formats);
	}


	private final void setNonPaigingParams(SimpleQueryForm queryForm, HttpServletRequest request) {
		Hashtable paigingParams = new Hashtable(2);
		paigingParams.put("q", "");
		paigingParams.put("s", "");
		queryForm.setNonPaigingParams(getParamsString(paigingParams, request));
	}


	private final String getParamsString(Hashtable excludeParams, HttpServletRequest request) {
		if (request == null)
			return null;

		Enumeration params = request.getParameterNames();
		String param;
		String vals[];
		StringBuffer addParams = new StringBuffer();
		try {
			while (params.hasMoreElements()) {
				param = (String) params.nextElement();
				if (!excludeParams.containsKey(param)) {
					vals = request.getParameterValues(param);
					for (int i = 0; i < vals.length; i++) {
						addParams.append("&" + param + "=" + URLEncoder.encode(vals[i], "utf-8"));
					}
				}
			}
		} catch (Exception e) {
			addParams.toString();
		}
		return addParams.toString();
	}



	/*
	 *  Gets the HTTP parameters that should be used to retrieve the next set of
	 *  results.
	 *
	 * @return    Everything after the ? that should be included in the pager URL.
	 */
	/* public String getNextResultsUrl() {
		if (resultDocs == null || start < 0)
			return null;
		int e = start + numPagingRecords;
		int n = resultDocs.length;
		if (e >= n)
			return null;
		String end = getEnd();
		if (end == null)
			return null;
		return "q=" + getQe() +
			"&s=" + end +
			getNonPaigingParams();
	} */
	/*
	 *  Gets the HTTP parameters that should be used to retrieve the previous set
	 *  of results.
	 *
	 * @return    Everything after the ? that should be included in the pager URL.
	 */
	/* public String getPrevResultsUrl() {
		if (resultDocs == null || start <= 0)
			return null;
		int p = start - numPagingRecords;
		int prev = p > 0 ? p : 0;
		return "q=" + getQe() +
			"&s=" + prev +
			getNonPaigingParams();
	} */
	// -------------- Debug ------------------


	/**
	 *  Sets the debug attribute of the SimpleQueryAction class
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
	private void prtln(String s) {
		if (debug)
			System.out.println("SimpleQueryAction: " + s);
	}
}


