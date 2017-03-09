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
package org.dlese.dpc.services.dds.action;

import org.dlese.dpc.services.dds.action.form.*;
import org.dlese.dpc.dds.*;
import org.dlese.dpc.dds.action.*;
import org.dlese.dpc.xml.*;
import org.dlese.dpc.repository.*;
import org.dlese.dpc.index.reader.*;
import org.dlese.dpc.index.*;
import org.dlese.dpc.index.analysis.*;
import org.apache.lucene.queryParser.QueryParser;
import org.dlese.dpc.util.*;
import org.dlese.dpc.index.writer.*;
import org.dlese.dpc.webapps.servlets.filters.GzipFilter;
import org.dlese.dpc.vocab.MetadataVocab;
import org.dlese.dpc.schemedit.SchemEditServlet;
import org.dlese.dpc.index.search.DateRangeFilter;

import org.apache.lucene.search.*;
import org.apache.lucene.analysis.*;
import org.apache.lucene.analysis.standard.StandardAnalyzer;

import java.util.*;
import java.text.*;
import java.io.*;
import java.util.Hashtable;
import java.util.Locale;
import javax.servlet.ServletContext;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServletResponse;
import org.apache.struts.action.Action;
import org.apache.struts.action.ActionError;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionServlet;
import org.apache.struts.util.MessageResources;

/**
 *  An <strong>Action</strong> controller that handles requests for the JavaScript HTML search service. This
 *  class handles JSHTML search service verions 1.1. Note that web_service_connection.jsp in the context acts
 *  partially as a controller as well.
 *
 * @author     John Weatherley
 * @version    $Id: JSHTMLAction_1_1.java,v 1.15 2010/07/14 00:19:27 jweather Exp $
 * @see        org.dlese.dpc.services.dds.action.form.JSHTMLForm_1_1
 */
public final class JSHTMLAction_1_1 extends Action {
	private final static int MAX_SEARCH_RESULTS = 100;

	private static boolean debug = true;

	private static long repositoryLastModCount = -1;
	private static long repositorySetsModTime = -1;
	private static int numRecordsIndexed = -1;


	// --------------------------------------------------------- Public Methods

	/**
	 *  Processes the JavaScript service request by forwarding to the appropriate corresponding JSP page for
	 *  rendering.
	 *
	 * @param  mapping        The ActionMapping used to select this instance
	 * @param  request        The HTTP request we are processing
	 * @param  response       The HTTP response we are creating
	 * @param  form           The ActionForm for the given page
	 * @return                The ActionForward instance describing where and how control should be forwarded
	 * @exception  Exception  If error.
	 */
	public ActionForward execute(
	                             ActionMapping mapping,
	                             ActionForm form,
	                             HttpServletRequest request,
	                             HttpServletResponse response)
		 throws Exception {

		/*
		 *  Design note:
		 *  Only one instance of this class gets created for the app and shared by
		 *  all threads. To be thread-safe, use only local variables, not instance
		 *  variables (the JVM will handle these properly using the stack). Pass
		 *  all variables via method signatures rather than instance vars.
		 */
		try {
			// Track where the session was created, for bookeeping in DCS
			HttpSession mySes = request.getSession(true);
			mySes.setAttribute("sessionCreator", "jshtml-v1-1");
		} catch (Throwable t) {}

		JSHTMLForm_1_1 jsform = null;
		try {

			jsform = (JSHTMLForm_1_1) form;

			RepositoryManager rm =
				(RepositoryManager) servlet.getServletContext().getAttribute("repositoryManager");

			HashMap customSearchMappings = configureCustomMenus(request, jsform);
			
			boolean hasMenuItemSelected = hasMenuItemSelected(request,jsform.getSmartLinkParameterNames());
			jsform.setHasMenuItemSelected(hasMenuItemSelected);

			// Determine the action to take:
			if (request.getParameter("fullDescription") != null || request.getParameter("collectionDescription") != null)
				return doGetRecord(request, response, rm, jsform, mapping);
			else if (request.getParameter("q") != null || request.getParameter("qh") != null || hasMenuItemSelected) {
				return doUserSearch(request, response, rm, jsform, customSearchMappings, mapping);
			}
			else
				return mapping.findForward("jshtml.default");
		} catch (NullPointerException npe) {
			prtln("JSHTMLAction_1_1 caught exception. " + npe);
			npe.printStackTrace();
			jsform.setErrorMsg("There was an internal error by the server: " + npe);
			return (mapping.findForward("jshtml.default"));
		} catch (Throwable e) {
			prtln("JSHTMLAction_1_1 caught exception. " + e);
			e.printStackTrace();
			jsform.setErrorMsg("There was an internal error by the server: " + e);
			return (mapping.findForward("jshtml.default"));
		}
	}


	private boolean hasMenuItemSelected(HttpServletRequest request, ArrayList smartLinkParameterNames) {
		if (request.getParameter("gr") != null)
			return true;
		if (request.getParameter("su") != null)
			return true;
		if (request.getParameter("re") != null)
			return true;
		if (request.getParameter("ky") != null)
			return true;
		if (request.getParameter("cs") != null)
			return true;
		
		if(smartLinkParameterNames != null)
			for(int i = 0; i < smartLinkParameterNames.size(); i++)
				if (request.getParameter((String)smartLinkParameterNames.get(i)) != null)
					return true;
		
		return false;
	}


	private HashMap configureCustomMenus(HttpServletRequest request, JSHTMLForm_1_1 form) {
		String[] menus = request.getParameterValues("menu");
		if (menus == null || menus.length == 0)
			return null;

		HashMap searchMappings = new HashMap(menus.length);
		HashMap menuItemsMap = new HashMap(menus.length);
		for (int i = 0; i < menus.length; i++) {
			//prtln("menu: " + menus[i]);
			String[] vals = menus[i].split("\\|");
			if (vals == null || vals.length != 3)
				continue;

			// Form: menuName | itemName | query
			Object position = form.addMenuItem(vals[0], vals[1]);
			String key = "slm" + position + "-" + (form.getNumMenuItems(vals[0]) -1 );
			searchMappings.put(key, vals[2]);
			menuItemsMap.put(key, vals[1]);
			//prtln("menu key: " + key + " menu: " + vals[0] + " item: " + vals[1]);
		}
		form.setMenuItemsMap(menuItemsMap);
		return searchMappings;
	}


	/**
	 *  Handles a request to get a given record from the repository. <p>
	 *
	 *  Arguments: identifier, metadataPrefix.<p>
	 *
	 *  Error Exception Conditions: <br>
	 *  badArgument - The request includes illegal arguments.
	 *
	 * @param  request        The HTTP request
	 * @param  response       The HTTP response
	 * @param  rm             The RepositoryManager used
	 * @param  mapping        ActionMapping used
	 * @param  jsform         The Form Bean
	 * @return                An ActionForward to the JSP page that will handle the response
	 * @exception  Exception  If error.
	 */
	protected ActionForward doGetRecord(
	                                    HttpServletRequest request,
	                                    HttpServletResponse response,
	                                    RepositoryManager rm,
	                                    JSHTMLForm_1_1 jsform,
	                                    ActionMapping mapping)
		 throws Exception {

		String idParam = request.getParameter("fullDescription");
		if (idParam == null || idParam.length() == 0)
			idParam = request.getParameter("collectionDescription");
		if (idParam == null || idParam.length() == 0) {
			jsform.setErrorMsg("The fullDescription or collectionDescription parameter is required but is missing or empty");
			jsform.setResults(null);
			return mapping.findForward("jshtml.default");
		}

		ResultDocList resultDocs = null;
		SimpleLuceneIndex index = rm.getIndex();

		String id = SimpleLuceneIndex.encodeToTerm(idParam);
		String so = request.getParameter("so");
		String xmlFormat = request.getParameter("xmlFormat");

		String query;
		if (so == null || so.equalsIgnoreCase("discoverableRecords") || so.length() == 0) {
			query = "id:" + id + " AND " + rm.getDiscoverableRecordsQuery();
		}
		// Note that the following two cases are not currently used, but could be in the future...
		else if (so.equalsIgnoreCase("allRecords")) {
			// Return only discoverable records, unless authorized...
			if (isAuthorized(request, "trustedIp", rm)) {
				query = "id:" + id;
				jsform.setAuthorizedFor("trustedUser");
			}
			else {
				jsform.setErrorMsg("You are not authorized to search over all records");
				return mapping.findForward("jshtml.default");
			}
		}
		else {
			jsform.setErrorMsg("The parameter so must contain only 'allRecords' or 'discoverableRecords'");
			return mapping.findForward("jshtml.default");
		}

		if (index != null)
			resultDocs = index.searchDocs(query);

		if (resultDocs == null || resultDocs.size() == 0) {
			if (index != null)
				resultDocs = index.searchDocs("id:\"" + id + "\"");
			if (resultDocs != null && resultDocs.size() > 0)
				jsform.setErrorMsg("ID '" + idParam + "' is in the repository but is not discoverable");
			else
				jsform.setErrorMsg("ID '" + idParam + "' does not exist in the repository");
			jsform.setResults(null);
			return mapping.findForward("jshtml.default");
		}

		jsform.setResults(resultDocs);
		return mapping.findForward("jshtml.default");
	}


	/**
	 *  Handles a request to perform a search over item-level records using the User Query Language. This request
	 *  exposes the same search options that users experience when performing a search for educational resources
	 *  in the DDS.<p>
	 *
	 *  Arguments: identifier, metadataPrefix.<p>
	 *
	 *  Error Exception Conditions: <br>
	 *  badArgument - The request includes illegal parameters.
	 *
	 * @param  request               The HTTP request
	 * @param  response              The HTTP response
	 * @param  rm                    The RepositoryManager used
	 * @param  mapping               ActionMapping used
	 * @param  jsform                The Form
	 * @param  customSearchMappings  A Map of search queries keyed by menu-item ID
	 * @return                       An ActionForward to the JSP page that will handle the response
	 * @exception  Exception         If error.
	 */
	protected ActionForward doUserSearch(
	                                     HttpServletRequest request,
	                                     HttpServletResponse response,
	                                     RepositoryManager rm,
	                                     JSHTMLForm_1_1 jsform,
	                                     Map customSearchMappings,
	                                     ActionMapping mapping)
		 throws Exception {

		String s = request.getParameter("s");
		String n = request.getParameter("n");
		try {
			if (s == null)
				jsform.setS(0);
			else
				jsform.setS(Integer.parseInt(s));
			if (jsform.getS() < 0) {
				jsform.setErrorMsg("The parameter 's' must be greater than or equal to 0. s specifies the start position within the search results to display.");
				return mapping.findForward("jshtml.default");
			}
		} catch (NumberFormatException nfe) {
			jsform.setErrorMsg("The parameter 's' must be a number. s specifies the start position within the search results to display.");
			return mapping.findForward("jshtml.default");
		}
		try {
			if (n == null)
				jsform.setN(10);
			else
				jsform.setN(Integer.parseInt(n));
			if (jsform.getN() < 1 || jsform.getN() > MAX_SEARCH_RESULTS) {
				jsform.setErrorMsg("The command 'MaxResultsPerPage' must be a number from 1 to " + MAX_SEARCH_RESULTS + ". MaxResultsPerPage specifies the maximum number of search results to display.");
				return mapping.findForward("jshtml.default");
			}
		} catch (NumberFormatException nfe) {
			jsform.setErrorMsg("The command 'MaxResultsPerPage' must be a number. MaxResultsPerPage specifies the maximum number of search results to display.");
			return mapping.findForward("jshtml.default");
		}

		// Enforce maximum records
		if (jsform.getN() > MAX_SEARCH_RESULTS) {
			jsform.setErrorMsg("The maximum allowable value for the 'n' parameter is " + MAX_SEARCH_RESULTS);
			return mapping.findForward("jshtml.default");
		}

		// Handle User Query Language requests over item-level records

		// Enforce compliance with the User Search query option:
		String xmlFormat = request.getParameter("xmlFormat");
		String so = request.getParameter("so");
		if (xmlFormat != null && !xmlFormat.startsWith("adn")) {
			jsform.setErrorMsg("The UserSearch request operates over the ADN format only. Try using the Search request to search over the xml format '" + xmlFormat + "'");
			return mapping.findForward("jshtml.default");
		}
		else if (so != null) {
			jsform.setErrorMsg("The so (search over) parameter may not be used with the UserSearch request. Try using the Search request to search over discoverable and non-discoverable records");
			return mapping.findForward("jshtml.default");
		}

		// Add smart link search constraints / query
		String smartLinkQuery = null;
		if(customSearchMappings != null){
			Enumeration pNames = request.getParameterNames();
			while (pNames.hasMoreElements()) {
				String menuQuery = null;
				String pName = (String)pNames.nextElement();
				if(pName.startsWith("slm")){
					String [] pVals = request.getParameterValues(pName);
					for(int i = 0; i < pVals.length; i++){
						String slQuery = (String)customSearchMappings.get( pName + "-" + pVals[i]);
						if(slQuery != null && slQuery.length() > 0){
							if(menuQuery == null)
								menuQuery = "(" + slQuery + ")";
							else
								menuQuery = "(" + slQuery + ") OR " + menuQuery;
						}
					}
				}
				if(menuQuery != null){
					if(smartLinkQuery == null)
						smartLinkQuery = "(" + menuQuery + ")";
					else
						smartLinkQuery = "(" + menuQuery + ") AND " + smartLinkQuery;	
				}
			}
			if(smartLinkQuery != null)
				smartLinkQuery = "(" + smartLinkQuery + ")";
		}
		
		// Perform the search...
		DDSStandardSearchResult standardSearchResult =
			DDSQueryAction.ddsStandardQuery(
			request,
			smartLinkQuery,
			rm,
			(MetadataVocab) servlet.getServletContext().getAttribute("MetadataVocab"),
			servlet.getServletContext(),
			DDSQueryAction.SEARCHTYPE_JSHTML_SEARCH);

		//org.apache.lucene.queryParser.ParseException pe = standardSearchResult.getParseException();
		Exception ex = standardSearchResult.getException();
		if (ex != null) {
			
			// Check if there was a Lucene syntax error, and report it back to client:
			if(ex instanceof org.apache.lucene.queryParser.ParseException) {
				// Grab a ParseException message from just the user-supplied query string
				String q = request.getParameter("q");
				try {
					// If we have a user query, try it to see if it's the source of the parse error
					if(q != null)
						rm.getIndex().getQueryParser().parse(q);
					
					// If the error did NOT reside in the user's query, send an error message to the developer
					jsform.setErrorMsg("There was an error parsing the search query. Possible reason: " + ex.getMessage());
					return mapping.findForward("jshtml.default");					
				} catch (org.apache.lucene.queryParser.ParseException pe2) {			
					ex = pe2;
					// To do: if user's query was the problem, could send a message to the user...
				}
			}
			else {
				jsform.setErrorMsg("There was an error processing the request: " + ex.getMessage());
				return mapping.findForward("jshtml.default");					
			}
		}

		ResultDocList resultDocs = standardSearchResult.getResults();

		//prtln("doUserSearch() search for: '" + request.getParameter("q") + "' had " + (resultDocs == null ? -1 : resultDocs.length) + " results");

		if (resultDocs == null || resultDocs.size() == 0)
			jsform.setResults(null);
		else
			jsform.setResults(resultDocs);

		return mapping.findForward("jshtml.default");
	}


	/**
	 *  Handles a request to perform a search over all records using the Lucene Query Language. <p>
	 *
	 *
	 *
	 * @param  request        The HTTP request
	 * @param  response       The HTTP response
	 * @param  rm             The RepositoryManager used
	 * @param  mapping        ActionMapping used
	 * @param  jsform         The form bean
	 * @return                An ActionForward to the JSP page that will handle the response
	 * @exception  Exception  If error.
	 */
	protected ActionForward doSearch(
	                                 HttpServletRequest request,
	                                 HttpServletResponse response,
	                                 RepositoryManager rm,
	                                 JSHTMLForm_1_1 jsform,
	                                 ActionMapping mapping)
		 throws Exception {

		String s = request.getParameter("s");
		String n = request.getParameter("n");
		try {
			if (s == null)
				jsform.setS(0);
			else
				jsform.setS(Integer.parseInt(s));
			if (jsform.getS() < 0) {
				jsform.setErrorMsg("The parameter 's' must be greater than or equal to 0. s specifies the start position within the search results to display.");
				return mapping.findForward("jshtml.default");
			}
		} catch (NumberFormatException nfe) {
			jsform.setErrorMsg("The parameter 's' must be a number. s specifies the start position within the search results to display.");
			return mapping.findForward("jshtml.default");
		}
		try {
			if (n == null)
				jsform.setN(10);
			else
				jsform.setN(Integer.parseInt(n));
			if (jsform.getN() < 1 || jsform.getN() > MAX_SEARCH_RESULTS) {
				jsform.setErrorMsg("The parameter 'n' must be a number from 1 to " + MAX_SEARCH_RESULTS + ". n specifies the maximum number of search results to display.");
				return mapping.findForward("jshtml.default");
			}
		} catch (NumberFormatException nfe) {
			jsform.setErrorMsg("The parameter 'n' must be a number. n specifies the maximum number of search results to display.");
			return mapping.findForward("jshtml.default");
		}

		// The possible params (excluding vocab-managed params including su, re, cs, ky, gr)
		String userQuery = request.getParameter("q");
		String dateField = request.getParameter("dateField");
		String fromDate = request.getParameter("fromDate");
		String toDate = request.getParameter("toDate");
		String[] sortAscendingBy = request.getParameterValues("sortAscendingBy");
		String[] sortDescendingBy = request.getParameterValues("sortDescendingBy");
		String so = request.getParameter("so");
		String[] xmlFormat = request.getParameterValues("xmlFormat");

		HashMap docReaderAttributes = new HashMap();

		// Error check the xmlFormat argument
		if (xmlFormat != null && xmlFormat.length != 1) {
			jsform.setErrorMsg("Only one 'xmlFormat' parameter may be specified");
			return mapping.findForward("jshtml.default");
		}

		// Error check the sortBy arguments
		if ((sortAscendingBy != null && sortDescendingBy != null) ||
			(sortAscendingBy != null && sortAscendingBy.length > 1) ||
			(sortDescendingBy != null && sortDescendingBy.length > 1)) {
			jsform.setErrorMsg("Only one of the parameters 'sortAscendingby' or 'sortDescendingBy' may be specified");
			return mapping.findForward("jshtml.default");
		}

		// Get a query that pulls formats that may can be converted to the requested format
		String xmlFormatsQuery = null;
		if (xmlFormat != null)
			xmlFormatsQuery = rm.getConvertableFormatsQuery(xmlFormat[0]);

		// If a format was requested but there is no available query for that format, return empty
		if (xmlFormat != null && xmlFormatsQuery == null) {
			// Log the query that the client requested:
			DDSQueryAction.logQuery(userQuery,
				servlet.getServletContext(),
				request,
				200,
				-1,
				0,
				getNumRecords(rm),
				jsform.getS(),
				DDSQueryAction.SEARCHTYPE_JSHTML_SEARCH);

			jsform.setErrorMsg("There are no records available in xml format '" + xmlFormat[0] + "'");
			return mapping.findForward("jshtml.default");
		}

		// Handle date field searches
		if (dateField != null && fromDate == null && toDate == null) {
			jsform.setErrorMsg("The 'fromDate' and/or 'toDate' parameter must be specified when the 'dateField' argument is indicated");
			return mapping.findForward("jshtml.default");
		}
		DateRangeFilter dateFilter = null;
		if (fromDate != null || toDate != null) {
			if (dateField == null) {
				jsform.setErrorMsg("The 'dateField' parameter must be specified when either the 'fromDate' or 'toDate' argument is indicated");
				return mapping.findForward("jshtml.default");
			}
			// Map common field names to their indexed names
			if (dateField.equalsIgnoreCase("fileLastModified"))
				dateField = "modtime";
			if (dateField.equalsIgnoreCase("whatsNewDate"))
				dateField = "wndate";

			try {
				if (fromDate != null && toDate != null)
					dateFilter = new DateRangeFilter(dateField, MetadataUtils.parseDate(fromDate), MetadataUtils.parseDate(toDate));
				else if (fromDate != null)
					dateFilter = DateRangeFilter.After(dateField, MetadataUtils.parseDate(fromDate));
				else if (toDate != null)
					dateFilter = DateRangeFilter.Before(dateField, MetadataUtils.parseDate(toDate));
			} catch (ParseException pe) {
				jsform.setErrorMsg("One or more dates indicated is incorrect: " + pe.getMessage() + ". Dates must be of the form YYYY-MM-DD, YYYY-MM, YYYY or yyyy-MM-ddTHH:mm:ssZ");
				return mapping.findForward("jshtml.default");
			}
		}

		SimpleLuceneIndex index = rm.getIndex();
		ResultDocList resultDocs = null;

		// Return only discoverable items, unless authorized...
		boolean isAuthorized = isAuthorized(request, "trustedIp", rm);

		/* System.out.println("");
		prtln("UserQuery was: '" + userQuery + "'");
		String query = DDSQueryAction.formatFieldsInQuery(userQuery);
		prtln("UserQuery is: '" + query + "'");
		prtln("Query was the same? " + userQuery.equals(query) );
		System.out.println(""); */
		String query = userQuery;
		if (so == null || so.equalsIgnoreCase("discoverableRecords") || so.length() == 0) {
			if (query == null || query.trim().length() == 0)
				query = rm.getDiscoverableRecordsQuery();
			else
				query = "(" + query + ") AND " + rm.getDiscoverableRecordsQuery();

			if (xmlFormatsQuery != null)
				query += " AND " + xmlFormatsQuery;
		}
		else if (so.equalsIgnoreCase("allRecords")) {
			// Return only discoverable records, unless authorized...
			if (isAuthorized) {
				if (query == null || query.length() == 0)
					query = "allrecords:true AND !doctype:0errordoc";
				else
					query = "(" + query + ") AND !doctype:0errordoc";

				if (xmlFormatsQuery != null)
					query += " AND " + xmlFormatsQuery;
				jsform.setAuthorizedFor("trustedUser");
			}
			else {
				jsform.setErrorMsg("You are not authorized to search over all records");
				return mapping.findForward("jshtml.default");
			}
		}
		else {
			jsform.setErrorMsg("The parameter so must contain only 'allRecords' or 'discoverableRecords'");
			return mapping.findForward("jshtml.default");
		}

		MetadataVocab vocab = (MetadataVocab) servlet.getServletContext().getAttribute("MetadataVocab");
		// Limit search to the fields indicated (ky,gr,su,re,cs)
		String vocabQuery = DDSQueryAction.getVocabParamsQueryString(request, vocab, docReaderAttributes);
		query += vocabQuery;

		// Limit the search by DCS status, if requested (not currently in protocol ddsws v1.0)
		String [] status = request.getParameterValues("dcsStatus");
		if(status != null) {
			SchemEditServlet sev = (SchemEditServlet) servlet.getServletContext().getAttribute("schemEditServlet");
			if(sev != null) {
				String statusQuery = sev.getStatusQuery(status);
				if(statusQuery != null && statusQuery.length() > 0)
					query = "(" + query + ") AND (" + statusQuery + ")";
			}
		}		
		
		// If empty search, do nothing...
		boolean isEmptySearch = false;
		if ((userQuery == null || userQuery.length() == 0) &&
			(xmlFormat == null || xmlFormat[0].length() == 0) &&
			vocabQuery.length() == 0 &&
			dateFilter == null) {
			isEmptySearch = true;
		}

		// Perform the search...
		if (index != null && !isEmptySearch) {

			// Set-up sorting
			if (sortAscendingBy != null) {
				if (sortAscendingBy[0].equalsIgnoreCase("fileLastModified"))
					sortAscendingBy[0] = "modtime";
				if (sortAscendingBy[0].equalsIgnoreCase("whatsNewDate"))
					sortAscendingBy[0] = "wndate";
				docReaderAttributes.put("sortAscendingByField", sortAscendingBy[0]);
			}
			else if (sortDescendingBy != null) {
				if (sortDescendingBy[0].equalsIgnoreCase("fileLastModified"))
					sortDescendingBy[0] = "modtime";
				if (sortDescendingBy[0].equalsIgnoreCase("whatsNewDate"))
					sortDescendingBy[0] = "wndate";
				docReaderAttributes.put("sortDescendingByField", sortDescendingBy[0]);
			}

			QueryParser qp = index.getQueryParser();
			Query pQuery = null;
			try {
				pQuery = qp.parse(query);
			} catch (org.apache.lucene.queryParser.ParseException pe) {

				// Grab a ParseException message from just the user-supplied query string
				String q = request.getParameter("q");
				try {
					index.getQueryParser().parse(q);
				} catch (org.apache.lucene.queryParser.ParseException pe2) {
					pe = pe2;
				}

				jsform.setErrorMsg("Error parsing the q parameter ' " + userQuery + "'. Possible reason: " + pe.getMessage());
				return mapping.findForward("jshtml.default");
			}
			
			Sort sort = null;
			
			// Sort results
			if (sortAscendingBy != null)
				sort = new Sort( new SortField(sortAscendingBy[0], SortField.STRING, false) );
			else if (sortDescendingBy != null)
				sort = new Sort( new SortField(sortDescendingBy[0], SortField.STRING, false) );

			resultDocs = index.searchDocs(pQuery, dateFilter, sort, docReaderAttributes);

			//prtln("doSearch() query for: '" + query + "' had " + (resultDocs == null ? -1 : resultDocs.length) + " results");
		}

		// Send a message if no matches were made:
		if (resultDocs == null || resultDocs.size() == 0) {

			// Log the query that the client requested:
			DDSQueryAction.logQuery(userQuery,
				servlet.getServletContext(),
				request,
				200,
				-1,
				0,
				getNumRecords(rm),
				jsform.getS(),
				DDSQueryAction.SEARCHTYPE_JSHTML_SEARCH);

			/* String q = request.getParameter("q");
			if (q == null || q.trim().length() == 0)
				jsform.setErrorMsg("Your search had no matching records");
			else
				jsform.setErrorMsg("Your search for '" + request.getParameter("q") + "' had no matching records"); */
			return mapping.findForward("jshtml.default");
		}

		jsform.setResults(resultDocs);

		// Log the query that the client requested:
		DDSQueryAction.logQuery(userQuery,
			servlet.getServletContext(),
			request,
			200,
			-1,
			resultDocs.size(),
			getNumRecords(rm),
			jsform.getS(),
			DDSQueryAction.SEARCHTYPE_JSHTML_SEARCH);

		return mapping.findForward("jshtml.default");
	}



	/**
	 *  Gets the number of records that have been indexed inclucing adn, collection, anno and other metadata
	 *  types regardless of status. Specifically, number of index entries that are not errordocs.
	 *
	 * @param  rm  The RepositoryManager being used.
	 * @return     The number of records that are currently in the index.
	 */
	public final static int getNumRecords(RepositoryManager rm) {
		long modCount = rm.getIndexLastModifiedCount();
		long setsModTime = rm.getSetStatusModifiedTime();
		if (modCount != repositoryLastModCount || setsModTime != repositorySetsModTime) {
			repositoryLastModCount = modCount;
			repositorySetsModTime = setsModTime;
			ResultDocList results = rm.getIndex().searchDocs("allrecords:true AND !doctype:0errordoc");
			if (results != null)
				numRecordsIndexed = results.size();
			else
				numRecordsIndexed = 0;
		}
		return numRecordsIndexed;
	}


	/**
	 *  Checks for IP authorization
	 *
	 * @param  request       HTTP request
	 * @param  securityRole  Security role
	 * @param  rm            RepositoryManager
	 * @return               True if authorized, false otherwise.
	 */
	private boolean isAuthorized(HttpServletRequest request, String securityRole, RepositoryManager rm) {
		if (securityRole.equals("trustedIp")) {
			String[] trustedIps = rm.getTrustedWsIpsArray();
			if (trustedIps == null)
				return false;

			String IP = request.getRemoteAddr();
			for (int i = 0; i < trustedIps.length; i++)
				if (IP.matches(trustedIps[i]))
					return true;
		}
		return false;
	}



	// --------------- Debug output ------------------

	/**
	 *  Return a string for the current time and date, sutiable for display in log files and output to standout:
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
		System.err.println(getDateStamp() + " JSHTMLAction_1_1 Error:" + s);
	}



	/**
	 *  Output a line of text to standard out, with datestamp, if debug is set to true.
	 *
	 * @param  s  The String that will be output.
	 */
	protected final void prtln(String s) {
		if (debug) {
			System.out.println(getDateStamp() + " JSHTMLAction_1_1: " + s);
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


