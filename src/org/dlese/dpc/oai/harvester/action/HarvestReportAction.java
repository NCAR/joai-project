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
package org.dlese.dpc.oai.harvester.action;

import org.dlese.dpc.repository.*;
import org.dlese.dpc.index.*;
import org.dlese.dpc.oai.harvester.action.form.*;
import org.dlese.dpc.index.reader.*;
import org.dlese.dpc.xml.*;
import org.apache.lucene.search.*;
import org.dlese.dpc.oai.harvester.*;
import org.dlese.dpc.oai.harvester.structs.*;
import org.dlese.dpc.index.search.DateRangeFilter;
import org.dlese.dpc.index.ResultDocList;

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
 *  A Struts Action for displaying harvest log reports that are stored in a
 *  SimpleLuceneIndex.
 *
 * @author    John Weatherley
 * @see       org.dlese.dpc.oai.harvester.action.form.HarvestReportForm
 */
public final class HarvestReportAction extends Action {

	private static boolean debug = false;
	private String SELECT_ALL = " -- All --";

	// --------------------------------------------------------- Public Methods

	/**
	 *  Processes the specified HTTP request and creates the corresponding HTTP response by
	 *  forwarding to a JSP that will create it.
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
		ServletContext servletContext = getServlet().getServletContext();
		HarvestReportForm hrf = (HarvestReportForm) form;

		// Set up params for paging:
		if (request.getParameter("s") != null) {
			try {
				hrf.setStart(Integer.parseInt(request.getParameter("s").trim()));
			} catch (Throwable e) {}
		}
		hrf.setRequest(request);

		ScheduledHarvestManager shm = 
			(ScheduledHarvestManager) servletContext.getAttribute("scheduledHarvestManager");		
		
		SimpleLuceneIndex harvestLogIndex = (SimpleLuceneIndex) servletContext.getAttribute("harvestLogIndex");
		
		prtln("harvestLogIndex num records: " + harvestLogIndex.getNumDocs());
		
		ActionErrors errors = new ActionErrors();

		if (harvestLogIndex == null) {
			errors.add("error", new ActionError("generic.message", "There was a server error: the web logs index could not be found."));
			saveErrors(request, errors);
			return mapping.findForward("display.report");
		}
		
		String paramsString = request.getQueryString();
		if (paramsString == null)
			paramsString = "";
		else
			paramsString = "?" + paramsString;
		
		String paramVal = "";
		try {
			// Handle queries:
			if (request.getParameter("q") != null) {
				String query = "doctype:0harvestlog AND " + request.getParameter("q");
				String rq = request.getParameter("rq");

				if (rq != null && !rq.trim().equals("*"))
					query += " AND " + formatQuery(rq);

				// Set up a date filter:
				DateRangeFilter dateFilter = DateRangeFilter.After("logdate", 0);
				Sort sort = new Sort( new SortField("logdate", SortField.STRING, true) );
				
				ResultDocList resultDocs = null;
				if (harvestLogIndex != null)
					resultDocs = harvestLogIndex.searchDocs(query, "admindefault", dateFilter, sort);

				if (resultDocs != null) {
					//prtln("query: " + query + ". Num results: " + resultDocs.size());
					//Arrays.sort(resultDocs.toArray(), new LuceneFieldComparator("logdate", LuceneFieldComparator.DESCENDING));
				}
				else
					prtln("query: " + query + " had zero results");

				hrf.setResults(resultDocs);

				setNonPaigingParams(hrf, request);
				// Handle reports queries:
				if (request.getParameter("report") != null) {
					// The title for the report is the content of the parameter "report"
					hrf.setReportTitle(request.getParameter("report"));
					return mapping.findForward("display.report");
				}

				return mapping.findForward("display.report");
			}

			prtln("No qualified params found");
			// If there were no parameters at all:
			return mapping.findForward("display.report");
		} catch (Throwable t) {
			if (hrf != null) {
				errors.add("error", new ActionError("generic.error",
					"There was a server problem: " + t));
				saveErrors(request, errors);
				prtln("There was a server problem: " + t);
			}
			else
				prtln("There was a problem: " + t);

			t.printStackTrace();

			return mapping.findForward("display.report");
		}

	}


	private final String formatQuery(String q) {
		q = q.trim();		
		if(q.startsWith("id:") && !q.matches(".*\\s+.*")){
			q = q.substring(2,q.length());
			q = "id:" + SimpleLuceneIndex.encodeToTerm(q);			
			return q;
		}
		return q;
	}


	private final void setNonPaigingParams(HarvestReportForm queryForm, HttpServletRequest request) {
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





	// -------------- Debug ------------------


	/**
	 *  Sets the debug attribute of the HarvestReportAction class
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
			System.out.println("HarvestReportAction: " + s);
	}
}


