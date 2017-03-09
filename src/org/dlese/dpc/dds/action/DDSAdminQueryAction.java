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
import org.dlese.dpc.index.search.DateRangeFilter;
import org.dlese.dpc.xml.*;
import org.apache.lucene.search.*;
import org.dlese.dpc.oai.*;
import org.dlese.dpc.vocab.*;
import org.dlese.dpc.services.mmd.Query;
import org.dlese.dpc.util.Files;

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
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import org.dlese.dpc.webapps.tools.GeneralServletTools;

/**
 *  A Struts Action for handling query requests that access a {@link
 *  org.dlese.dpc.index.SimpleLuceneIndex}. This class works in conjunction with the
 *  {@link org.dlese.dpc.dds.action.form.DDSAdminQueryForm} Struts form bean class.<p>
 *
 *
 *
 * @author    John Weatherley
 */
public final class DDSAdminQueryAction extends Action {

	private static boolean debug = false;
	/**  DESCRIPTION */
	public static String SELECT_ALL = " -- All --";
	/**  DESCRIPTION */
	public static String SELECT_NONE = " -- Select None --";
	/**  DESCRIPTION */
	public static String NO_ERRORS = "Records showing no errors";
	List formats = new ArrayList();
	List collections = new ArrayList();
	List collectionLabels = new ArrayList();
	List indexedFormats = new ArrayList();
	List indexedFormatsLabels = new ArrayList();
	long indexLastModified = -1;

	// --------------------------------------------------------- Public Methods

	/**
	 *  Processes the specified HTTP request and creates the corresponding HTTP response by
	 *  forwarding to a JSP that will create it. A {@link
	 *  org.dlese.dpc.index.SimpleLuceneIndex} must be available to this class via a
	 *  ServletContext attribute under the key "index." Returns an {@link
	 *  org.apache.struts.action.ActionForward} instance that maps to the Struts forwarding
	 *  name "simple.query," which must be configured in struts-config.xml to forward to the
	 *  JSP page that will handle the request.
	 *
	 * @param  mapping               The ActionMapping used to select this instance
	 * @param  request               The HTTP request we are processing
	 * @param  response              The HTTP response we are creating
	 * @param  form                  The ActionForm for the given page
	 * @return                       The ActionForward instance describing where and how
	 *      control should be forwarded
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
				
		DDSAdminQueryForm sqf = (DDSAdminQueryForm) form;
		// Set up the vocabs
		MetadataVocabInputState inputState = sqf.getVocabInputState();
		inputState.setState(request, "dds.descr.en-us");
		request.getSession().setAttribute("MetadataVocabInputState", inputState);
		
		String metadataVocabInstanceAttributeName = (String) servlet.getServletContext().getInitParameter("metadataVocabInstanceAttributeName");
		MetadataVocab vocab = (MetadataVocab) servlet.getServletContext().getAttribute(metadataVocabInstanceAttributeName);
		sqf.setVocab(vocab);		
		
		// Reset the results if the page request is empty...
		//if (request.getParameterMap() == null || request.getParameterMap().size() == 0)
			//sqf.setResults(null);
		
		// If a new search was requested, make sure other params are reset in the session bean:
		if (request.getParameter("q") != null) {
			if(request.getParameterValues("ky") == null)
				sqf.setKy(new String [] {});
			if(request.getParameterValues("sifmts") == null)
				sqf.setSifmts(new String [] {});				
		}			
			
		// Set up params for paging:
		if (request.getParameter("s") != null) {
			try {
				sqf.setStart(Integer.parseInt(request.getParameter("s").trim()));
			} catch (Throwable e) {
				sqf.setStart(0);
			}
		}
		//else
			//sqf.setStart(0);

		sqf.setRequest(request);

		RepositoryManager rm =
			(RepositoryManager) servlet.getServletContext().getAttribute("repositoryManager");

		sqf.setContextURL(GeneralServletTools.getContextUrl(request));

		if (request.getParameter("command") != null) {
			String param = request.getParameter("command");

			if (param.equalsIgnoreCase("showQueryOptions"))
				sqf.setShowQueryOptions("true");
			else if (param.equalsIgnoreCase("hideQueryOptions"))
				sqf.setShowQueryOptions("false");
			return mapping.findForward("simple.query");
		}

		// Handle the various requests:
		if (request.getParameter("searchOver") != null && request.getParameter("searchOver").equals("webLogs")) {
			return handleWebLogSearchRequest(mapping, (DDSAdminQueryForm) form, request, response);
		}

		// All other requests hanled by the search handler.
		return handleMetadataSearchRequest(mapping, (DDSAdminQueryForm) form, request, response, rm);
	}


	/**
	 *  Handle a request to search over metadata collections and forwared to the appropriate
	 *  jsp page to render the response.
	 *
	 * @param  mapping               The ActionMapping used to select this instance
	 * @param  request               The HTTP request we are processing
	 * @param  response              The HTTP response we are creating
	 * @param  queryForm             DESCRIPTION
	 * @param  rm                    DESCRIPTION
	 * @return                       The ActionForward instance describing where and how
	 *      control should be forwarded
	 * @exception  IOException       if an input/output error occurs
	 * @exception  ServletException  if a servlet exception occurs
	 */
	public ActionForward handleMetadataSearchRequest(
	                                                 ActionMapping mapping,
	                                                 DDSAdminQueryForm queryForm,
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
		//setCollectionsAndFormats(index, queryForm, rm);

		/* ArrayList mapperErrors = new ArrayList();
		mapperErrors.add(SELECT_NONE);
		List tmp = rm.getIndexedIdMapperErrors();
		if (tmp != null)
			mapperErrors.addAll(tmp);
		queryForm.setIdMapperErrors(mapperErrors); */
		/* ArrayList accessionStatus = new ArrayList();
		accessionStatus.add(SELECT_ALL);
		tmp = rm.getIndexedAccessionStatuses();
		if (tmp != null)
			accessionStatus.addAll(tmp);
		queryForm.setIndexedAccessionStatuses(accessionStatus); */
		String paramVal = "";
		try {

			// Handle user search queries:
			if (request.getParameter("q") != null) {
				String query = formatQuery(request.getParameter("q"));
				prtln("formatQuery to: " + query);
				if (query.equals("*"))
					query = "(allrecords:true)";

				if (request.getParameter("rq") != null) {
					if(query.trim().length() > 0)
						query += " AND ";
					query += formatQuery(request.getParameter("rq"));
				}

				// Handle single collection selection option:
				/* String selectedCollection = request.getParameter("ky");
				if (selectedCollection != null &&
					selectedCollection.length() > 0 &&
					!selectedCollection.equals(SELECT_ALL)) {
					query = "(" + query + ") AND ky:" + selectedCollection;
				} */
				// Handle idmapper errors selection option:
				String[] selectedIdMapperErrors = request.getParameterValues("selectedIdMapperErrors");
				if (selectedIdMapperErrors != null && selectedIdMapperErrors.length > 0
					 && !selectedIdMapperErrors[0].equals(SELECT_NONE)) {
					if(query.trim().length() > 0)
						query = "(" + query + ") AND ";						 
					query += "(idmaperrors:" + selectedIdMapperErrors[0];
					for (int i = 1; i < selectedIdMapperErrors.length; i++)
						query += " OR idmaperrors:" + selectedIdMapperErrors[i];
					query += ")";
				}

				// Handle accession status query option:
				String[] selectedAccessionStatuses = request.getParameterValues("indexedAccessionStatuses");
				if (selectedAccessionStatuses != null
					 && selectedAccessionStatuses.length > 0
					 && !selectedAccessionStatuses[0].equals(SELECT_ALL)) {
					if(query.trim().length() > 0)
						query = "(" + query + ") AND ";						 
					query += "(accessionstatus:" + selectedAccessionStatuses[0];
					for (int i = 1; i < selectedAccessionStatuses.length; i++)
						query += " OR accessionstatus:" + selectedAccessionStatuses[i];
					query += ")";
				}

				// Handle multiple collections selection option:
				String[] selectedCollections = request.getParameterValues("ky");
				if (selectedCollections != null &&
					selectedCollections.length > 0
					 && !selectedCollections[0].equals(SELECT_ALL)) {
					if(query.trim().length() > 0)
						query = "(" + query + ") AND ";						 
					query += "(ky:" + selectedCollections[0];
					for (int i = 1; i < selectedCollections.length; i++)
						query += " OR ky:" + selectedCollections[i];
					query += ")";
				}

				// Handle multiple formats selection option:
				String[] selectedFormats = request.getParameterValues("sfmts");
				ArrayList searchFormats = rm.getFormatsThatCanBeConvertedToFormats(selectedFormats);
				if (searchFormats != null &&
					searchFormats.size() > 0
					 && !((String) searchFormats.get(0)).equals(SELECT_ALL)) {
					if(query.trim().length() > 0)
						query = "(" + query + ") AND ";						 
					query += "(metadatapfx:0" + searchFormats.get(0);
					for (int i = 1; i < searchFormats.size(); i++)
						query += " OR metadatapfx:0" + searchFormats.get(0);
					query += ")";
				}

				// Handle multiple indexed formats selection option:
				String[] selectedIndexedFormats = request.getParameterValues("sifmts");
				if (selectedIndexedFormats != null
					 && selectedIndexedFormats.length > 0
					 && !selectedIndexedFormats[0].equals(SELECT_ALL)) {
					if(query.trim().length() > 0)
						query = "(" + query + ") AND ";						 
					query += "(metadatapfx:" + selectedIndexedFormats[0];
					for (int i = 1; i < selectedIndexedFormats.length; i++)
						query += " OR metadatapfx:" + selectedIndexedFormats[i];
					query += ")";
				}

				// Handle Grade Range
				String[] gradeRanges = request.getParameterValues("gr");
				if (gradeRanges != null && gradeRanges.length > 0) {
					if(query.trim().length() > 0)
						query = "(" + query + ") AND ";						 
					query += "(gr:" + gradeRanges[0];
					for (int i = 1; i < gradeRanges.length; i++)
						query += " OR gr:" + gradeRanges[i];
					query += ")";
				}

				// Handle Resource Type
				String[] resourceTypes = request.getParameterValues("re");
				if (resourceTypes != null && resourceTypes.length > 0) {
					if(query.trim().length() > 0)
						query = "(" + query + ") AND ";						 
					query += "(re:" + resourceTypes[0];
					for (int i = 1; i < resourceTypes.length; i++)
						query += " OR re:" + resourceTypes[i];
					query += ")";
				}

				// Handle subjects
				String[] subjects = request.getParameterValues("su");
				if (subjects != null && subjects.length > 0) {
					if(query.trim().length() > 0)
						query = "(" + query + ") AND ";						 
					query += "(su:" + subjects[0];
					for (int i = 1; i < subjects.length; i++)
						query += " OR su:" + subjects[i];
					query += ")";
				}

				// Handle Standards
				String[] contentStandards = request.getParameterValues("cs");
				if (contentStandards != null && contentStandards.length > 0) {
					if(query.trim().length() > 0)
						query = "(" + query + ") AND ";						 
					query += "(cs:" + contentStandards[0];
					for (int i = 1; i < contentStandards.length; i++)
						query += " OR cs:" + contentStandards[i];
					query += ")";
				}

				ResultDocList resultDocs = null;
				//prtln("Default operator is: " + index.getOperatorString());
				if (index != null)
					resultDocs = index.searchDocs(query);
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
					queryForm.setResult(null);
					errors.add("message", new ActionError("generic.message",
						"No record was found in the index for ID \"" + id + "\""));
					saveErrors(request, errors);
					return mapping.findForward("fullview.display");
				}

				Query.reloadIdExclusionDocument();
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
				String metadata = ((FileIndexingServiceDocReader) resultDocs.get(0).getDocReader()).getFullContentEncodedAs("UTF-8");
				//String metadata = Files.readFileToEncoding( ((XMLDocReader) resultDocs[0].getDocReader()).getFile(), "UTF-8").toString();
				if (metadata != null && metadata.length() > 0) {
					//prtln("METADATA 1: " + metadata);
					
					// Filter out the XML and DTD declaration if requested
					if (request.getParameter("filterxml") != null)
						metadata = XMLConversionService.stripXmlDeclaration(new BufferedReader(new StringReader(metadata))).toString();
					queryForm.setMetadata(metadata);
					
					//String metaUTF8 = new String(metadata.getBytes(), "UTF-8");
					//queryForm.setMetadata(metaUTF8);
					//prtln("METADATA 2: " + metaUTF8 );
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
	 *  Handle a request to search over web logs and forwared to the appropriate jsp page to
	 *  render the response.
	 *
	 * @param  mapping               The ActionMapping used to select this instance
	 * @param  request               The HTTP request we are processing
	 * @param  response              The HTTP response we are creating
	 * @param  form                  The ActionForm for the given page
	 * @return                       The ActionForward instance describing where and how
	 *      control should be forwarded
	 * @exception  IOException       if an input/output error occurs
	 * @exception  ServletException  if a servlet exception occurs
	 */
	public ActionForward handleWebLogSearchRequest(
	                                               ActionMapping mapping,
	                                               DDSAdminQueryForm form,
	                                               HttpServletRequest request,
	                                               HttpServletResponse response)
		 throws IOException, ServletException {

		SimpleLuceneIndex webLogIndex =
			(SimpleLuceneIndex) servlet.getServletContext().getAttribute("webLogIndex");

		if (webLogIndex == null)
			throw new ServletException("The attribute \"webLogIndex\" could not be found in the Servlet Context.");

		ActionErrors errors = new ActionErrors();

		// Grab the form bean:
		DDSAdminQueryForm queryForm = (DDSAdminQueryForm) form;

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
				if (webLogIndex != null)
					resultDocs = webLogIndex.searchDocs(query, "admindefault", dateFilter, sort);

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


	/**
	 *  Formats the fields/terms in the query string by replacing the aliased field names and
	 *  encoding terms that are in fields that have been indexed encoded.
	 *
	 * @param  q  The raw query by user
	 * @return    Formatted query
	 */
	private final String formatQuery(String q) {
		return DDSQueryAction.formatFieldsInQuery(q);
	}


	/* private final void setCollectionsAndFormats(
	                                            SimpleLuceneIndex index,
	                                            DDSAdminQueryForm queryForm,
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
				List tmp = rm.getIndexedFormats();
				indexedFormats = new ArrayList();
				indexedFormats.add(SELECT_ALL);
				indexedFormats.addAll(tmp);
				indexedFormatsLabels = new ArrayList();
				indexedFormatsLabels.add(SELECT_ALL);
				for (int i = 0; i < tmp.size(); i++) {
					String fmt = (String) tmp.get(i);
					indexedFormatsLabels.add(fmt.substring(1, fmt.length()));
				}
			}
		}
		prtln("adding collections and formats");
		//queryForm.setCollections(collections);
		//queryForm.setCollectionLabels(collectionLabels);
		queryForm.setFormats(formats);
		queryForm.setIndexedFormats(indexedFormats);
		queryForm.setIndexedFormatsLabels(indexedFormatsLabels);
	} */

	private final void setNonPaigingParams(DDSAdminQueryForm queryForm, HttpServletRequest request) {
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



	/**
	 *  Gets the HTTP parameters that should be used to retrieve the next set of results.
	 *
	 * @param  isDebugOutput  The new debug value
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
	/**
	 *  Gets the HTTP parameters that should be used to retrieve the previous set of results.
	 *
	 * @param  isDebugOutput  The new debug value
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
			System.out.println("DDSAdminQueryAction: " + s);
	}
}


