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
import org.dlese.dpc.dds.action.form.*;
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
 *  An <strong>Action</strong> that handles DDS Web service requests. This class handles DDSWS service version
 *  1.0.
 *
 * @author     John Weatherley
 * @version    $Id: DDSServicesAction.java,v 1.57 2010/07/23 01:23:12 jweather Exp $
 * @see        org.dlese.dpc.services.dds.action.form.DDSServicesForm
 */
public class DDSServicesAction extends Action {

	private static int MAX_SEARCH_RESULTS = 1000;

	/**  The UrlCheck request verb */
	public static String URL_CHECK_VERB = "UrlCheck";
	/**  The GetRecord request verb */
	public static String GET_RECORD_VERB = "GetRecord";
	/**  The search request verb */
	public static String SEARCH_VERB = "Search";
	/**  The seach all request verb */
	public static String USER_SEARCH_VERB = "UserSearch";
	/**  The ServiceInfo request verb */
	public static String SERVICE_INFO = "ServiceInfo";
	/**  The list xml formats request verb */
	public static String LIST_XML_FORMATS = "ListXmlFormats";
	/**  The list collections request verb */
	public static String LIST_COLLECTIONS = "ListCollections";
	/**  The list grade ranges request verb */
	public static String LIST_GRADE_RANGES = "ListGradeRanges";
	/**  The list subjects request verb */
	public static String LIST_SUBJECTS = "ListSubjects";
	/**  The list content standards request verb */
	public static String LIST_CONTENT_STANDARDS = "ListContentStandards";
	/**  The list resource types request verb */
	public static String LIST_RESOURCE_TYPES = "ListResourceTypes";
	/**  The list fields request verb */
	public static String LIST_FIELDS = "ListFields";	
	/**  The list terms request verb */
	public static String LIST_TERMS = "ListTerms";	

	private static boolean debug = true;

	private static long repositoryLastModCount = -1;
	private static long repositorySetsModTime = -1;
	private static int numRecordsIndexed = -1;


	/**
	 *  Gets the key used for caching the ListCollections response in the application scope. Should be
	 *  overwritten by the subclasses and updated in the ListCollections.jsp page when a new version is created.
	 *
	 * @return    The listCollectionsCacheKey value
	 */
	protected String getListCollectionsCacheKey() {
		return "ListCollectionsResponse10";
	}

	// --------------------------------------------------------- Public Methods --------

	/**
	 *  Processes the DDS web service request by forwarding to the appropriate corresponding JSP page for
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
			mySes.setAttribute("sessionCreator", "ddsws10");
		} catch (Throwable t) {}

		DDSServicesForm df = null;
		try {

			df = (DDSServicesForm) form;
			df.setVocab((MetadataVocab) servlet.getServletContext().getAttribute("MetadataVocab"));

			RepositoryManager rm =
				(RepositoryManager) servlet.getServletContext().getAttribute("repositoryManager");

			// Grab the DDS service request verb:
			String verb = request.getParameter("verb");
			if (verb == null) {
				df.setErrorMsg("The verb argument is required. Please indicate the request verb");
				df.setErrorCode(DDSServicesForm.ERROR_CODE_BADVERB);
				return (mapping.findForward("ddsservices.error"));
			}
			// Handle disabled status:
			/* else if (rm.getProviderStatus().toLowerCase().equals("disabled")) {
			 *return (mapping.findForward("ddsservices.disabled"));
			 *}  */
			// Handle check url request:
			else if (verb.equals(GET_RECORD_VERB)) {
				return doGetRecord(request, response, rm, df, mapping);
			}
			else if (verb.equals(SEARCH_VERB)) {
				return doSearch(request, response, rm, df, mapping);
			}
			else if (verb.equals(USER_SEARCH_VERB)) {
				return doUserSearch(request, response, rm, df, mapping);
			}
			// Handle check url request:
			else if (verb.equals(URL_CHECK_VERB)) {
				return doUrlCheck(request, response, rm, df, mapping);
			}
			// Handle check Service Info request:
			else if (verb.equals(SERVICE_INFO)) {
				return doServiceInfo(request, response, rm, df, mapping);
			}
			// Handle xml formats:
			else if (verb.equals(LIST_XML_FORMATS)) {
				return doListXmlFormats(request, response, rm, df, mapping);
			}
			// Handle collections:
			else if (verb.equals(LIST_COLLECTIONS)) {
				return doListCollections(request, response, rm, df, mapping);
			}
			// Handle list fields:
			else if (verb.equals(LIST_FIELDS)) {
				return doListFields(request, response, rm, df, mapping);
			}				
			// Handle list terms:
			else if (verb.equals(LIST_TERMS)) {
				return doListTerms(request, response, rm, df, mapping);
			}		
			// Handle vocab-ralated requests:
			else if (verb.equals(LIST_GRADE_RANGES) ||
				verb.equals(LIST_SUBJECTS) ||
				verb.equals(LIST_RESOURCE_TYPES) ||
				verb.equals(LIST_CONTENT_STANDARDS)) {
				return doListVocabEntries(request, response, rm, df, mapping, verb);
			}
			// The verb is not valid for the DDS web service
			else {
				df.setErrorMsg("The verb argument '" + verb + "' is not valid");
				df.setErrorCode(DDSServicesForm.ERROR_CODE_BADVERB);
				return (mapping.findForward("ddsservices.error"));
			}
		} catch (NullPointerException npe) {
			prtln("DDSServicesAction caught exception. " + npe);
			npe.printStackTrace();
			df.setErrorMsg("There was an internal error by the server: " + npe);
			df.setErrorCode(DDSServicesForm.ERROR_CODE_INTERNALSERVERERROR);
			return (mapping.findForward("ddsservices.error"));
		} catch (Throwable e) {
			prtln("DDSServicesAction caught exception. " + e);
			e.printStackTrace();
			df.setErrorMsg("There was an internal error by the server: " + e);
			df.setErrorCode(DDSServicesForm.ERROR_CODE_INTERNALSERVERERROR);
			return (mapping.findForward("ddsservices.error"));
		}
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
	 * @param  df             The bean
	 * @param  mapping        ActionMapping used
	 * @return                An ActionForward to the JSP page that will handle the response
	 * @exception  Exception  If error.
	 */
	protected ActionForward doGetRecord(
	                                    HttpServletRequest request,
	                                    HttpServletResponse response,
	                                    RepositoryManager rm,
	                                    DDSServicesForm df,
	                                    ActionMapping mapping)
		 throws Exception {

		String idParam = request.getParameter("id");
		if (idParam == null || idParam.length() == 0) {
			df.setErrorMsg("The id argument is required but is missing or empty");
			df.setErrorCode(DDSServicesForm.ERROR_CODE_BADARGUMENT);
			return mapping.findForward("ddsservices.error");
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
		else if (so.equalsIgnoreCase("allRecords")) {
			// Return only discoverable records, unless authorized...
			if (isAuthorized(request, "trustedIp", rm)) {
				query = "id:" + id;
				df.setAuthorizedFor("trustedUser");
			}
			else {
				df.setErrorMsg("You are not authorized to search over all records");
				df.setErrorCode(DDSServicesForm.ERROR_CODE_NOTAUTHORIZED);
				return mapping.findForward("ddsservices.error");
			}
		}
		else {
			df.setErrorMsg("The argument 'so' must contain only 'allRecords' or 'discoverableRecords'");
			df.setErrorCode(DDSServicesForm.ERROR_CODE_BADARGUMENT);
			return mapping.findForward("ddsservices.error");
		}

		if (index != null) {
			resultDocs = index.searchDocs(query);
		}

		if (resultDocs == null || resultDocs.size() == 0) {
			if (index != null) {
				resultDocs = index.searchDocs("id:\"" + id + "\"");
			}
			if (resultDocs != null && resultDocs.size() > 0) {
				df.setErrorMsg("ID '" + idParam + "' is in the repository but is not discoverable");
				df.setErrorCode(DDSServicesForm.ERROR_CODE_IDDOESNOTEXIST);
			}
			else {
				df.setErrorMsg("ID '" + idParam + "' does not exist in the repository");
				df.setErrorCode(DDSServicesForm.ERROR_CODE_IDDOESNOTEXIST);
			}
			return mapping.findForward("ddsservices.error");
		}

		// If no format specified, return in the native stripped or localized format
		if (xmlFormat == null) {
			// Method overridden by sub-classes
			setRecordXml(df, (ResultDoc)resultDocs.get(0));
		}
		else {
			String xml = ((XMLDocReader) ((ResultDoc)resultDocs.get(0)).getDocReader()).getXmlFormat(xmlFormat, true);
			if (xml == null || xml.length() == 0) {
				df.setErrorMsg("Record " + idParam + " exists in the repository but is not available in xmlFormat " + xmlFormat);
				df.setErrorCode(DDSServicesForm.ERROR_CODE_CANNOTDISSEMINATEFORMAT);
				return mapping.findForward("ddsservices.error");
			}
			df.setRecordXml(xml);
			df.setRecordFormat(xmlFormat);
		}

		df.setResults(resultDocs);
		return mapping.findForward("ddsservices.GetRecord");
	}


	/**
	 *  Sets the record XML in the form bean using the localized version of XML. This method is overidden in at
	 *  least one sub-class.
	 *
	 * @param  df         The form
	 * @param  resultDoc  The resultDoc
	 */
	protected void setRecordXml(DDSServicesForm df, ResultDoc resultDoc) {
		df.setRecordXml(((XMLDocReader) resultDoc.getDocReader()).getXmlLocalized());
	}


	/**
	 *  Handles a request to perform a search over item-level records using the User Query Language. This request
	 *  exposes the same search options that users experience when performing a search for educational resources
	 *  in the DDS.<p>
	 *
	 *  Arguments: identifier, metadataPrefix.<p>
	 *
	 *  Error Exception Conditions: <br>
	 *  badArgument - The request includes illegal arguments.
	 *
	 * @param  request        The HTTP request
	 * @param  response       The HTTP response
	 * @param  rm             The RepositoryManager used
	 * @param  df             The bean
	 * @param  mapping        ActionMapping used
	 * @return                An ActionForward to the JSP page that will handle the response
	 * @exception  Exception  If error.
	 */
	protected ActionForward doUserSearch(
	                                     HttpServletRequest request,
	                                     HttpServletResponse response,
	                                     RepositoryManager rm,
	                                     DDSServicesForm df,
	                                     ActionMapping mapping)
		 throws Exception {

		//String ql = request.getParameter("ql");

		// Enforce compliance with the specification:
		String s = request.getParameter("s");
		String n = request.getParameter("n");
		if (n == null || s == null) {
			String msg = "";
			if (n == null) {
				msg += "The argument 'n' is required. n specifies the maximum number of results to return. ";
			}
			if (s == null) {
				msg += "The argument 's' is required. s specifies the start position of the results to return.";
			}
			df.setErrorMsg(msg.trim());
			df.setErrorCode(DDSServicesForm.ERROR_CODE_BADARGUMENT);
			return mapping.findForward("ddsservices.error");
		}
		else {
			try {
				df.setS(Integer.parseInt(s));
				if (df.getS() < 0) {
					df.setErrorMsg("The argument 's' must be greater than or equal to 0. s specifies the start position of the results to return.");
					df.setErrorCode(DDSServicesForm.ERROR_CODE_BADARGUMENT);
					return mapping.findForward("ddsservices.error");
				}
			} catch (NumberFormatException nfe) {
				df.setErrorMsg("The argument 's' must be an integer. s specifies the start position of the results to return.");
				df.setErrorCode(DDSServicesForm.ERROR_CODE_BADARGUMENT);
				return mapping.findForward("ddsservices.error");
			}
			try {
				df.setN(Integer.parseInt(n));
				if (df.getN() < 1 || df.getN() > MAX_SEARCH_RESULTS) {
					df.setErrorMsg("The argument 'n' must be a number from 1 to " + MAX_SEARCH_RESULTS + ". n specifies the maximum number of results to return.");
					df.setErrorCode(DDSServicesForm.ERROR_CODE_BADARGUMENT);
					return mapping.findForward("ddsservices.error");
				}
			} catch (NumberFormatException nfe) {
				df.setErrorMsg("The argument 'n' must be in integer. n specifies the maximum number of results to return.");
				df.setErrorCode(DDSServicesForm.ERROR_CODE_BADARGUMENT);
				return mapping.findForward("ddsservices.error");
			}
		}

		// Enforce maximum records
		if (df.getN() > MAX_SEARCH_RESULTS) {
			df.setErrorMsg("The maximum allowable value for the 'n' argument is " + MAX_SEARCH_RESULTS);
			df.setErrorCode(DDSServicesForm.ERROR_CODE_BADARGUMENT);
			return mapping.findForward("ddsservices.error");
		}

		// Handle User Query Language requests over item-level records

		// Enforce compliance with the User Search query option:
		String xmlFormat = request.getParameter("xmlFormat");
		String so = request.getParameter("so");
		if (xmlFormat != null && !xmlFormat.startsWith("adn")) {
			df.setErrorMsg("The UserSearch request operates over the ADN format only. Try using the Search request to search over the xml format '" + xmlFormat + "'");
			df.setErrorCode(DDSServicesForm.ERROR_CODE_BADARGUMENT);
			return mapping.findForward("ddsservices.error");
		}
		else if (so != null) {
			df.setErrorMsg("The so (search over) argument may not be used with the UserSearch request. Try using the Search request to search over discoverable and non-discoverable records");
			df.setErrorCode(DDSServicesForm.ERROR_CODE_BADARGUMENT);
			return mapping.findForward("ddsservices.error");
		}

		// Perform the search...
		DDSStandardSearchResult standardSearchResult =
			DDSQueryAction.ddsStandardQuery(
			request,
			null,
			rm,
			(MetadataVocab) servlet.getServletContext().getAttribute("MetadataVocab"),
			servlet.getServletContext(),
			DDSQueryAction.SEARCHTYPE_DDSWS_USER_SEARCH);

		//org.apache.lucene.queryParser.ParseException pe = standardSearchResult.getParseException();
		Exception ex = standardSearchResult.getException();
		if (ex != null) {

			// Check if there was a Lucene syntax error, and report it back to client:
			if (ex instanceof org.apache.lucene.queryParser.ParseException) {
				// Grab a ParseException message from just the user-supplied query string
				String q = request.getParameter("q");
				try {
					rm.getIndex().getQueryParser().parse(q);
				} catch (org.apache.lucene.queryParser.ParseException pe2) {
					ex = pe2;
				}

				df.setErrorMsg(ex.getMessage());
				df.setErrorCode(DDSServicesForm.ERROR_CODE_BADQUERY);
				return mapping.findForward("ddsservices.error");
			}
			else {
				df.setErrorMsg("There was an error processing the request: " + ex.getMessage());
				df.setErrorCode(DDSServicesForm.ERROR_CODE_BADARGUMENT);
				return mapping.findForward("ddsservices.error");
			}
		}

		ResultDocList resultDocs = standardSearchResult.getResults();

		//prtln("doUserSearch() search for: '" + q + "' had " + (resultDocs == null ? -1 : resultDocs.length) + " results");

		if (resultDocs == null || resultDocs.size() == 0) {
			String q = request.getParameter("q");
			if (q == null || q.trim().length() == 0) {
				df.setErrorMsg("Your search had no matching records");
			}
			else {
				df.setErrorMsg("Your search for '" + request.getParameter("q") + "' had no matching records");
			}
			df.setErrorCode(DDSServicesForm.ERROR_CODE_NORECORDSMATCH);
			return mapping.findForward("ddsservices.error");
		}

		if (resultDocs == null || resultDocs.size() == 0) {
			df.setResults(null);
		}
		else {
			df.setResults(resultDocs);
		}

		return mapping.findForward("ddsservices.Search");
	}


	/**
	 *  Handles a request to perform a search over all records using the Lucene Query Language. <p>
	 *
	 *  Arguments: identifier, metadataPrefix.<p>
	 *
	 *  Error Exception Conditions: <br>
	 *  badArgument - The request includes illegal arguments.
	 *
	 * @param  request        The HTTP request
	 * @param  response       The HTTP response
	 * @param  rm             The RepositoryManager used
	 * @param  df             The bean
	 * @param  mapping        ActionMapping used
	 * @return                An ActionForward to the JSP page that will handle the response
	 * @exception  Exception  If error.
	 */
	protected ActionForward doSearch(
	                                 HttpServletRequest request,
	                                 HttpServletResponse response,
	                                 RepositoryManager rm,
	                                 DDSServicesForm df,
	                                 ActionMapping mapping)
		 throws Exception {

		// Enforce compliance with the specification:
		String s = request.getParameter("s");
		String n = request.getParameter("n");
		if (n == null || s == null) {
			String msg = "";
			if (n == null) {
				msg += "The argument 'n' is required. n specifies the maximum number of results to return. ";
			}
			if (s == null) {
				msg += "The argument 's' is required. s specifies the start position of the results to return.";
			}
			df.setErrorMsg(msg.trim());
			df.setErrorCode(DDSServicesForm.ERROR_CODE_BADARGUMENT);
			return mapping.findForward("ddsservices.error");
		}
		else {
			try {
				df.setS(Integer.parseInt(s));
				if (df.getS() < 0) {
					df.setErrorMsg("The argument 's' must be greater than or equal to 0. s specifies the start position of the results to return.");
					df.setErrorCode(DDSServicesForm.ERROR_CODE_BADARGUMENT);
					return mapping.findForward("ddsservices.error");
				}
			} catch (NumberFormatException nfe) {
				df.setErrorMsg("The argument 's' must be an integer. s specifies the start position of the results to return.");
				df.setErrorCode(DDSServicesForm.ERROR_CODE_BADARGUMENT);
				return mapping.findForward("ddsservices.error");
			}
			try {
				df.setN(Integer.parseInt(n));
				if (df.getN() < 1 || df.getN() > MAX_SEARCH_RESULTS) {
					df.setErrorMsg("The argument 'n' must be a number from 1 to " + MAX_SEARCH_RESULTS + ". n specifies the maximum number of results to return.");
					df.setErrorCode(DDSServicesForm.ERROR_CODE_BADARGUMENT);
					return mapping.findForward("ddsservices.error");
				}
			} catch (NumberFormatException nfe) {
				df.setErrorMsg("The argument 'n' must be in integer. n specifies the maximum number of results to return.");
				df.setErrorCode(DDSServicesForm.ERROR_CODE_BADARGUMENT);
				return mapping.findForward("ddsservices.error");
			}
		}

		// The possible params (plus: su, re, cs, ky, gr, dcsStatus)
		String userQuery = request.getParameter("q");
		String dateField = request.getParameter("dateField");
		String fromDate = request.getParameter("fromDate");
		String toDate = request.getParameter("toDate");
		String[] sortAscendingBy = request.getParameterValues("sortAscendingBy");
		String[] sortDescendingBy = request.getParameterValues("sortDescendingBy");
		String so = request.getParameter("so");
		String[] xmlFormat = request.getParameterValues("xmlFormat");

		//prtln("doSearch() userQuery: '" + userQuery + "'");

		HashMap docReaderAttributes = new HashMap();

		// Error check the xmlFormat argument
		if (xmlFormat != null && xmlFormat.length != 1) {
			df.setErrorMsg("Only one 'xmlFormat' argument may be specified");
			df.setErrorCode(DDSServicesForm.ERROR_CODE_BADARGUMENT);
			return mapping.findForward("ddsservices.error");
		}

		// Error check the sortBy arguments
		if ((sortAscendingBy != null && sortDescendingBy != null) ||
			(sortAscendingBy != null && sortAscendingBy.length > 1) ||
			(sortDescendingBy != null && sortDescendingBy.length > 1)) {
			df.setErrorMsg("Only one of the arguments 'sortAscendingby' or 'sortDescendingBy' may be specified");
			df.setErrorCode(DDSServicesForm.ERROR_CODE_BADARGUMENT);
			return mapping.findForward("ddsservices.error");
		}

		// Get a query that pulls formats that may can be converted to the requested format
		String xmlFormatsQuery = null;
		if (xmlFormat != null) {
			xmlFormatsQuery = rm.getConvertableFormatsQuery(xmlFormat[0]);
		}

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
				df.getS(),
				DDSQueryAction.SEARCHTYPE_DDSWS_SEARCH);

			df.setErrorMsg("There are no records available in xml format '" + xmlFormat[0] + "'");
			df.setErrorCode(DDSServicesForm.ERROR_CODE_NORECORDSMATCH);
			return mapping.findForward("ddsservices.error");
		}

		// Handle date field searches
		if (dateField != null && fromDate == null && toDate == null) {
			df.setErrorMsg("The 'fromDate' and/or 'toDate' argument must be specified when the 'dateField' argument is indicated");
			df.setErrorCode(DDSServicesForm.ERROR_CODE_BADARGUMENT);
			return mapping.findForward("ddsservices.error");
		}
		DateRangeFilter dateFilter = null;
		if (fromDate != null || toDate != null) {

			//prtln("Date search from: " + fromDate + " to: " + toDate);

			if (dateField == null) {
				df.setErrorMsg("The 'dateField' argument must be specified when either the 'fromDate' or 'toDate' argument is indicated");
				df.setErrorCode(DDSServicesForm.ERROR_CODE_BADARGUMENT);
				return mapping.findForward("ddsservices.error");
			}
			// Map common field names to their indexed names
			if (dateField.equalsIgnoreCase("fileLastModified")) {
				dateField = "modtime";
			}
			if (dateField.equalsIgnoreCase("whatsNewDate")) {
				dateField = "wndate";
			}

			try {
				if (fromDate != null && toDate != null) {
					dateFilter = new DateRangeFilter(dateField, MetadataUtils.parseDate(fromDate), MetadataUtils.parseDate(toDate));
				}
				else if (fromDate != null) {
					dateFilter = DateRangeFilter.After(dateField, MetadataUtils.parseDate(fromDate));
				}
				else if (toDate != null) {
					dateFilter = DateRangeFilter.Before(dateField, MetadataUtils.parseDate(toDate));
				}
			} catch (ParseException pe) {
				df.setErrorMsg("One or more dates indicated is incorrect: " + pe.getMessage() + ". Dates must be of the form YYYY-MM-DD, YYYY-MM, YYYY or yyyy-MM-ddTHH:mm:ssZ");
				df.setErrorCode(DDSServicesForm.ERROR_CODE_BADARGUMENT);
				return mapping.findForward("ddsservices.error");
			}
		}

		SimpleLuceneIndex index = rm.getIndex();
		ResultDocList resultDocs = null;

		// Return only discoverable items, unless authorized...
		boolean isAuthorized = isAuthorized(request, "trustedIp", rm);

		/* System.out.println("");
		 *prtln("UserQuery was: '" + userQuery + "'");
		 *String query = DDSQueryAction.formatFieldsInQuery(userQuery);
		 *prtln("UserQuery is: '" + query + "'");
		 *prtln("Query was the same? " + userQuery.equals(query) );
		 *System.out.println("");  */
		String query = userQuery;
		if (so == null || so.equalsIgnoreCase("discoverableRecords") || so.length() == 0) {
			if (query == null || query.trim().length() == 0) {
				query = rm.getDiscoverableRecordsQuery();
			}
			else {
				query = "(" + query + ") AND " + rm.getDiscoverableRecordsQuery();
			}

			if (xmlFormatsQuery != null) {
				query += " AND " + xmlFormatsQuery;
			}
		}
		else if (so.equalsIgnoreCase("allRecords")) {
			// Return only discoverable records, unless authorized...
			if (isAuthorized) {
				if (query == null || query.length() == 0) {
					query = "allrecords:true AND !doctype:0errordoc";
				}
				else {
					query = "(" + query + ") AND !doctype:0errordoc";
				}

				if (xmlFormatsQuery != null) {
					query += " AND " + xmlFormatsQuery;
				}
				df.setAuthorizedFor("trustedUser");
			}
			else {
				df.setErrorMsg("You are not authorized to search over all records");
				df.setErrorCode(DDSServicesForm.ERROR_CODE_NOTAUTHORIZED);
				return mapping.findForward("ddsservices.error");
			}
		}
		else {
			df.setErrorMsg("The argument 'so' must contain only 'allRecords' or 'discoverableRecords'");
			df.setErrorCode(DDSServicesForm.ERROR_CODE_BADARGUMENT);
			return mapping.findForward("ddsservices.error");
		}

		MetadataVocab vocab = (MetadataVocab) servlet.getServletContext().getAttribute("MetadataVocab");
		// Limit search to the fields indicated (ky,gr,su,re,cs)
		String vocabQuery = DDSQueryAction.getVocabParamsQueryString(request, vocab, docReaderAttributes);
		query += vocabQuery;

		// Limit the search by DCS status, if requested (not currently in protocol ddsws v1.0)
		String[] status = request.getParameterValues("dcsStatus");
		if (status != null) {
			SchemEditServlet sev = (SchemEditServlet) servlet.getServletContext().getAttribute("schemEditServlet");
			if (sev != null) {
				String statusQuery = sev.getStatusQuery(status);
				if (statusQuery != null && statusQuery.length() > 0) {
					query = "(" + query + ") AND (" + statusQuery + ")";
				}
			}
		}

		// Get the Geospatial Query, or null if not requested:
		Query geospatialQuery = null;
		try {
			geospatialQuery = DDSQueryAction.getGeospatialQuery(request);
		} catch (Exception geoEx) {
			df.setErrorMsg(geoEx.getMessage());
			df.setErrorCode(DDSServicesForm.ERROR_CODE_BADARGUMENT);
			return mapping.findForward("ddsservices.error");
		}

		// If empty search, do nothing...
		boolean isEmptySearch = false;
		if ((userQuery == null || userQuery.length() == 0) &&
			(xmlFormat == null || xmlFormat[0].length() == 0) &&
			vocabQuery.length() == 0 &&
			dateFilter == null &&
			geospatialQuery == null) {
			isEmptySearch = true;
		}

		// Perform the search...
		if (index != null && !isEmptySearch) {

			// Set-up sorting
			if (sortAscendingBy != null) {
				if (sortAscendingBy[0].equalsIgnoreCase("fileLastModified")) {
					sortAscendingBy[0] = "modtime";
				}
				if (sortAscendingBy[0].equalsIgnoreCase("whatsNewDate")) {
					sortAscendingBy[0] = "wndate";
				}
				docReaderAttributes.put("sortAscendingByField", sortAscendingBy[0]);
			}
			else if (sortDescendingBy != null) {
				if (sortDescendingBy[0].equalsIgnoreCase("fileLastModified")) {
					sortDescendingBy[0] = "modtime";
				}
				if (sortDescendingBy[0].equalsIgnoreCase("whatsNewDate")) {
					sortDescendingBy[0] = "wndate";
				}
				docReaderAttributes.put("sortDescendingByField", sortDescendingBy[0]);
			}

			QueryParser qp = index.getQueryParser();
			Query pQuery = null;
			try {
				//prtln("doSearch() query is: '" + query + "'");
				pQuery = qp.parse(query);
				//prtln("doSearch() PARSED query is: '" + pQuery.toString() + "'");
			} catch (org.apache.lucene.queryParser.ParseException pe) {

				// Grab a ParseException message from just the user-supplied query string
				String q = request.getParameter("q");
				try {
					index.getQueryParser().parse(q);
				} catch (org.apache.lucene.queryParser.ParseException pe2) {
					pe = pe2;
				}

				df.setErrorMsg(pe.getMessage());
				df.setErrorCode(DDSServicesForm.ERROR_CODE_BADQUERY);
				return mapping.findForward("ddsservices.error");
			}

			BooleanQuery booleanQuery = new BooleanQuery();
			booleanQuery.add(pQuery, BooleanClause.Occur.MUST);

			if (geospatialQuery != null) {
				String geoClause = request.getParameter("geoClause");
				if (geoClause != null && geoClause.toLowerCase().equals("should"))
					booleanQuery.add(geospatialQuery, BooleanClause.Occur.SHOULD);
				else
					booleanQuery.add(geospatialQuery, BooleanClause.Occur.MUST);
			}
			
			// Sort results?
			Sort sort = null;
			if (sortAscendingBy != null) {
				sort = new Sort( new SortField(sortAscendingBy[0], SortField.STRING, false) );
				//prtln("sortAscendingBy:" + sortAscendingBy[0]);
			}
			else if (sortDescendingBy != null) {
				sort = new Sort( new SortField(sortDescendingBy[0], SortField.STRING, true) );
				//prtln("sortDescendingBy:" + sortDescendingBy[0]);
			}			
			
			resultDocs = index.searchDocs(booleanQuery, dateFilter, sort, docReaderAttributes);
			
			//prtln("doSearch() query for: '" + booleanQuery + "' had " + resultDocs.size() + " results");
			//prtln("doSearch() query for: '" + query + "' had " + resultDocs.size() + " results");
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
				df.getS(),
				DDSQueryAction.SEARCHTYPE_DDSWS_SEARCH);

			String q = request.getParameter("q");
			if (q == null || q.trim().length() == 0) {
				df.setErrorMsg("Your search had no matching records");
			}
			else {
				df.setErrorMsg("Your search for '" + request.getParameter("q") + "' had no matching records");
			}
			df.setErrorCode(DDSServicesForm.ERROR_CODE_NORECORDSMATCH);

			return mapping.findForward("ddsservices.error");
		}
		
		// Pre-fetch the docs to ensure they are available for read and if not send error 
		// (can fail if indexer re-loads the index reader between search time and read time):
		try {
			int end = df.getN() + df.getS();
			for( int i = df.getS(); i < resultDocs.size() && i < end; i++)
				resultDocs.get(i).getDocReader().getDocument();			
		} catch (Throwable t) {
			prtlnErr(t.toString());
			df.setErrorCode(DDSServicesForm.ERROR_CODE_INTERNALSERVERERROR);
			df.setErrorMsg(t.getMessage());
			return mapping.findForward("ddsservices.error");			
		}
		
		df.setResults(resultDocs);

		// Log the query that the client requested:
		DDSQueryAction.logQuery(userQuery,
			servlet.getServletContext(),
			request,
			200,
			-1,
			resultDocs.size(),
			getNumRecords(rm),
			df.getS(),
			DDSQueryAction.SEARCHTYPE_DDSWS_SEARCH);		
			
		return mapping.findForward("ddsservices.Search");
	}


	/**
	 *  Handles a request to check the repository for the existence of a given URL. <p>
	 *
	 *  Arguments: url (one or more} <p>
	 *
	 *  Error Exception Conditions: <br>
	 *  badArgument - The request includes illegal arguments.
	 *
	 * @param  request   The HTTP request
	 * @param  response  The HTTP response
	 * @param  rm        The RepositoryManager used
	 * @param  df        The bean
	 * @param  mapping   ActionMapping used
	 * @return           An ActionForward to the JSP page that will handle the response
	 */
	protected ActionForward doUrlCheck(
	                                   HttpServletRequest request,
	                                   HttpServletResponse response,
	                                   RepositoryManager rm,
	                                   DDSServicesForm df,
	                                   ActionMapping mapping) {

		String[] urls = request.getParameterValues("url");
		if (urls == null || urls.length == 0) {
			df.setErrorMsg("You must supply one or more url parameters for the UrlCheck request.");
			df.setErrorCode(DDSServicesForm.ERROR_CODE_BADARGUMENT);
			return mapping.findForward("ddsservices.error");
		}

		String q = "urlenc:" + SimpleLuceneIndex.encodeToTerm(urls[0].trim(), false);
		for (int i = 1; i < urls.length; i++) {
			q += " OR urlenc:" + SimpleLuceneIndex.encodeToTerm(urls[i].trim(), false);
		}

		SimpleLuceneIndex index = rm.getIndex();
		ResultDocList resultDocs = index.searchDocs(q);

		//prtln("doUrlCheck() search for: '" + q + "' had " + (resultDocs == null ? -1 : resultDocs.length) + " resultDocs");

		if (resultDocs == null || resultDocs.size() == 0) {
			df.setResults(null);
		}
		else {
			df.setResults(resultDocs);
		}

		return (mapping.findForward("ddsservices.UrlCheck"));
	}


	/**
	 *  Handles a request to get a the available XML formats. <p>
	 *
	 *  Error Exception Conditions: <br>
	 *  badArgument - The request includes illegal arguments.
	 *
	 * @param  request        The HTTP request
	 * @param  response       The HTTP response
	 * @param  rm             The RepositoryManager used
	 * @param  df             The bean
	 * @param  mapping        ActionMapping used
	 * @return                An ActionForward to the JSP page that will handle the response
	 * @exception  Exception  If error.
	 */
	protected ActionForward doListXmlFormats(
	                                         HttpServletRequest request,
	                                         HttpServletResponse response,
	                                         RepositoryManager rm,
	                                         DDSServicesForm df,
	                                         ActionMapping mapping)
		 throws Exception {

		String id = request.getParameter("id");
		List xmlFormats = null;
		if (id == null) {
			xmlFormats = rm.getAvailableFormatsList();
		}
		else {
			xmlFormats = rm.getAvailableFormatsList(id);
		}

		if (xmlFormats == null || xmlFormats.size() == 0) {
			String errMsg;
			if (id == null) {
				errMsg = "No xml formats are currently available from this service";
			}
			else {
				ResultDocList results = rm.getIndex().searchDocs("id:\"" + id + "\"");
				if (results == null || results.size() == 0) {
					errMsg = "ID '" + id + "' is not in the repository";
				}
				else {
					errMsg = "No xml formats are available for ID '" + id + "'";
				}
			}

			df.setErrorMsg(errMsg);
			df.setErrorCode(DDSServicesForm.ERROR_CODE_NORECORDSMATCH);
			return mapping.findForward("ddsservices.error");
		}

		df.setXmlFormats(xmlFormats);
		return mapping.findForward("ddsservices.ListXmlFormats");
	}


	private long indexLastModifiedCount = 0, setStatusModifiedTime = 0;


	/**
	 *  Handles a request to get a the available collections. <p>
	 *
	 *  Error Exception Conditions: <br>
	 *  badArgument - The request includes illegal arguments.
	 *
	 * @param  request        The HTTP request
	 * @param  response       The HTTP response
	 * @param  rm             The RepositoryManager used
	 * @param  df             The bean
	 * @param  mapping        ActionMapping used
	 * @return                An ActionForward to the JSP page that will handle the response
	 * @exception  Exception  If error.
	 */
	protected ActionForward doListCollections(
	                                          HttpServletRequest request,
	                                          HttpServletResponse response,
	                                          RepositoryManager rm,
	                                          DDSServicesForm df,
	                                          ActionMapping mapping)
		 throws Exception {

		ServletContext context = servlet.getServletContext();

		// If a different vocabInterface is requested, return it instead of the cache
		String vocabInterface = request.getParameter("vocabInterface");
		if (vocabInterface != null) {
			if (!vocabInterface.equals("dds.descr.en-us")) {
				df.setVocabInterface(vocabInterface);
				context.removeAttribute(getListCollectionsCacheKey());
			}
		}

		// Return the cached response if nothing has changed, otherwise update the cache
		if (rm.getSetStatusModifiedTime() > setStatusModifiedTime || rm.getIndexLastModifiedCount() > indexLastModifiedCount) {
			context.removeAttribute(getListCollectionsCacheKey());
			setStatusModifiedTime = rm.getSetStatusModifiedTime();
			prtln("removing coll cache key: " + getListCollectionsCacheKey());
			indexLastModifiedCount = rm.getIndexLastModifiedCount();
		}
		else if (context.getAttribute(getListCollectionsCacheKey()) != null) {
			// If nothing has changed and we've already got a cache, return it...
			return mapping.findForward("ddsservices.ListCollections");
		}

		// Records in the 'collect' collection are those that the Repository configures as collections:
		String query = "collection:0collect";

		// Sort by short title:
		Sort sortBy = new Sort( new SortField("/key//collectionRecord/general/shortTitle", SortField.STRING, false) );	
		
		ResultDocList results = rm.getIndex().searchDocs(query,sortBy);
		
		/* if (results != null) {
			Collections.sort(results, new LuceneFieldComparator("shorttitle", LuceneFieldComparator.ASCENDING));
		} */

		df.setResults(results);

		// Set up vocab
		//System.out.println("setting collection field...");
		df.setField("dlese_collect", "key");

		return mapping.findForward("ddsservices.ListCollections");
	}


	/**
	 *  Handles a request to get a list of the fields in the index. <p>
	 *
	 *  Error Exception Conditions: <br>
	 *  badArgument - The request includes illegal arguments.
	 *
	 * @param  request        The HTTP request
	 * @param  response       The HTTP response
	 * @param  rm             The RepositoryManager used
	 * @param  df             The bean
	 * @param  mapping        ActionMapping used
	 * @return                An ActionForward to the JSP page that will handle the response
	 * @exception  Exception  If error.
	 */
	protected ActionForward doListFields(
	                                          HttpServletRequest request,
	                                          HttpServletResponse response,
	                                          RepositoryManager rm,
	                                          DDSServicesForm df,
	                                          ActionMapping mapping)
		 throws Exception {

		request.setAttribute("index",rm.getIndex());

		return mapping.findForward("ddsservices.ListFields");
	}

	/**
	 *  Handles a request to get a list of terms for given field(s) in the index. <p>
	 *
	 *  Error Exception Conditions: <br>
	 *  badArgument - The request includes illegal arguments.
	 *
	 * @param  request        The HTTP request
	 * @param  response       The HTTP response
	 * @param  rm             The RepositoryManager used
	 * @param  df             The bean
	 * @param  mapping        ActionMapping used
	 * @return                An ActionForward to the JSP page that will handle the response
	 * @exception  Exception  If error.
	 */
	protected ActionForward doListTerms(
	                                          HttpServletRequest request,
	                                          HttpServletResponse response,
	                                          RepositoryManager rm,
	                                          DDSServicesForm df,
	                                          ActionMapping mapping)
		 throws Exception {
		
		String [] fields = request.getParameterValues("field");
		
		// Require one or more fields args:
		if(fields == null || fields.length == 0) {
			df.setErrorMsg("The 'field' argument is required for request ListTerms. Please indicate one or more fields.");
			df.setErrorCode(DDSServicesForm.ERROR_CODE_BADARGUMENT);
			return mapping.findForward("ddsservices.error");
		}
		
		// Require the fields arg(s) are not empty:
		boolean isEmptyFields = true;
		for(int i = 0; i < fields.length; i++) {
			if(fields[i].trim().length() > 0) {
				isEmptyFields = false;
				break;
			}
		}
		if(isEmptyFields) {
			df.setErrorMsg("The 'field' argument is required for request ListTerms, but is empty. Please indicate one or more fields.");
			df.setErrorCode(DDSServicesForm.ERROR_CODE_BADARGUMENT);
			return mapping.findForward("ddsservices.error");
		}		
		
		/* DDSReportingForm drf = new DDSReportingForm();
		drf.setIndex(rm.getIndex());
		drf.setCalculateCountsForFieldsAsArray(fields); */
		
		Map termAndDocCountsMap = rm.getIndex().getTermAndDocCounts(fields);
		request.setAttribute("termAndDocCountsMap",termAndDocCountsMap);
		request.setAttribute("numTerms",termAndDocCountsMap.size());
		request.setAttribute("index",rm.getIndex());
		
		return mapping.findForward("ddsservices.ListTerms");
	}	
	
	
	/**
	 *  Handles a request to list a vocab and it's values including GradeRanges, Subjects, ResourceTypes and
	 *  ContentStandards. <p>
	 *
	 *  Error Exception Conditions: <br>
	 *  badArgument - The request includes illegal arguments.
	 *
	 * @param  request        The HTTP request
	 * @param  response       The HTTP response
	 * @param  rm             The RepositoryManager used
	 * @param  df             The bean
	 * @param  mapping        ActionMapping used
	 * @param  verb           The verb requested
	 * @return                An ActionForward to the JSP page that will handle the response
	 * @exception  Exception  If error.
	 */
	protected ActionForward doListVocabEntries(
	                                           HttpServletRequest request,
	                                           HttpServletResponse response,
	                                           RepositoryManager rm,
	                                           DDSServicesForm df,
	                                           ActionMapping mapping,
	                                           String verb)
		 throws Exception {

		if (verb.equals(LIST_GRADE_RANGES)) {
			df.setField("adn", "gradeRange");
		}
		else if (verb.equals(LIST_RESOURCE_TYPES)) {
			df.setField("adn", "resourceType");
		}
		else if (verb.equals(LIST_CONTENT_STANDARDS)) {
			df.setField("adn", "contentStandard");
		}
		else if (verb.equals(LIST_SUBJECTS)) {
			df.setField("adn", "subject");
		}

		return mapping.findForward("ddsservices.ListVocabEntries");
	}


	/**
	 *  Handles a request to get a the service information. <p>
	 *
	 *  Error Exception Conditions: <br>
	 *  badArgument - The request includes illegal arguments.
	 *
	 * @param  request        The HTTP request
	 * @param  response       The HTTP response
	 * @param  rm             The RepositoryManager used
	 * @param  df             The bean
	 * @param  mapping        ActionMapping used
	 * @return                An ActionForward to the JSP page that will handle the response
	 * @exception  Exception  If error.
	 */
	protected ActionForward doServiceInfo(
	                                      HttpServletRequest request,
	                                      HttpServletResponse response,
	                                      RepositoryManager rm,
	                                      DDSServicesForm df,
	                                      ActionMapping mapping)
		 throws Exception {

		return mapping.findForward("ddsservices.ServiceInfo");
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
			if (results != null) {
				numRecordsIndexed = results.size();
			}
			else {
				numRecordsIndexed = 0;
			}
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
			if (trustedIps == null) {
				return false;
			}

			String IP = request.getRemoteAddr();
			for (int i = 0; i < trustedIps.length; i++) {
				if (IP.matches(trustedIps[i])) {
					return true;
				}
			}
		}
		return false;
	}


	/**
	 *  Sets the maximum number of search results that can be returned by the service.
	 *
	 * @param  maxResults  The new maxSearchResults value
	 */
	public static void setMaxSearchResults(int maxResults) {
		MAX_SEARCH_RESULTS = maxResults;
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
		System.err.println(getDateStamp() + " " + s);
	}


	/**
	 *  Output a line of text to standard out, with datestamp, if debug is set to true.
	 *
	 * @param  s  The String that will be output.
	 */
	protected final void prtln(String s) {
		if (debug) {
			System.out.println(getDateStamp() + " " + s);
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


