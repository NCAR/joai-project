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
package org.dlese.dpc.schemedit.action.form;

import org.dlese.dpc.schemedit.input.SchemEditActionErrors;
import org.dlese.dpc.index.*;
import org.dlese.dpc.repository.*;
import org.dlese.dpc.schemedit.*;
import org.dlese.dpc.schemedit.dcs.*;
import org.dlese.dpc.schemedit.action.*;
import org.dlese.dpc.index.reader.*;
import org.dlese.dpc.util.*;

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

 *
 * @author    Jonathan Ostwald
 */
public final class DCSViewForm extends SchemEditForm implements Serializable {
	private static boolean debug = false;
	private int start = -1;
	private int numPagingRecords = 10;
	private HttpServletRequest request;
	private String prevId = null;
	private String nextId = null;

	private String queryString = null;
	private String refineQueryString = null;
	private XMLDocReader docReader = null;
	private String reportTitle = null;
	private String contextURL = null;
	
	// following are defined in SchemEditForm
	private DcsSetInfo dcsSetInfo = null;
	private ResultDoc resultDoc = null;
	
	private int resultIndex = -1;
	private SearchHelper searchHelper = null;
	
	private String frameworkConfigFormat = null;
	
	boolean collectionFrameworkLoaded = false;

	/**  Constructor for the DCSViewForm object */
	public DCSViewForm() { }
	
	public DcsSetInfo getDcsSetInfo () {
		return dcsSetInfo;
	}
	
	public void setDcsSetInfo (DcsSetInfo dcsSetInfo) {
		this.dcsSetInfo = dcsSetInfo;
	}
	
	public String getPrevId () {
		return prevId;
	}
	
	public void setPrevId (String id) {
		prevId = id;
	}
	
	public String getNextId () {
		return nextId;
	}
	
	public void setNextId (String id) {
		nextId = id;
	}
	
	
	/**
	 *  Gets the contextURL attribute of the DCSViewForm object
	 *
	 * @return    The contextURL value
	 */
	public String getContextURL() {
		return contextURL;
	}



	/**
	 *  Sets the contextURL attribute of the DCSViewForm object
	 *
	 * @param  contextURL  The new contextURL value
	 */
	public void setContextURL(String contextURL) {
		this.contextURL = contextURL;
	}


	/**
	 *  Gets the search results returned by the {@link
	 *  org.dlese.dpc.index.SimpleLuceneIndex}.
	 *
	 * @return    The results value
	 */

	public SearchHelper getResults() {
		return searchHelper;
	}

	public void setCollectionFrameworkLoaded (boolean bool) {
		this.collectionFrameworkLoaded = bool;
	}
	
	public boolean getCollectionFrameworkLoaded () {
		return this.collectionFrameworkLoaded;
	}
	
	/**
	 *  Sets the search results.
	 */
	public void setResults(SearchHelper results) {
		searchHelper = results;
	}

	/**
	 *  Sets the result attribute of the DCSViewForm object
	 *
	 * @param  resultDoc  The new result value
	 */
	public void setResult(ResultDoc resultDoc) {
		this.resultDoc = resultDoc;
	}


	/**
	 *  Gets the result attribute of the DCSViewForm object
	 *
	 * @return    The result value
	 */
	public ResultDoc getResult() {
		return resultDoc;
	}
	
 	public void setResultIndex (int i) {
		resultIndex = i;
	}
	
	/**
	* Get the index of the current result within the results. Returns -1 if 
	result not found.
	*/
	public int getResultIndex() {
		return resultIndex;
	}
	
	/**
	* Returns relative path to the jsp frag page containing the record view for the
	* format of the result record.
	*/
	public String getRecordViewPage () {
		return getXmlFormat() + "_record.jsp";
	}
	
	/**
	 *  Gets the result attribute of the DCSViewForm object
	 *
	 * @return    The result value
	 */
	public DocReader getDocReader() {
		if (resultDoc == null)
			return null;
		return resultDoc.getDocReader();
	}

	/**
	 *  Gets the numResults attribute of the DCSViewForm object
	 *
	 * @return    The numResults value
	 */
	public String getNumResults() {
		return Integer.toString(searchHelper.getNumHits());
	}

	/**
	 *  Sets the reportTitle attribute of the DCSViewForm object
	 *
	 * @param  reportTitle  The new reportTitle value
	 */
	public void setReportTitle(String reportTitle) {
		this.reportTitle = reportTitle;
	}


	/**
	 *  Gets the reportTitle attribute of the DCSViewForm object
	 *
	 * @return    The reportTitle value
	 */
	public String getReportTitle() {
		return reportTitle;
	}



	// ---------------- Pager --------------------

	/**
	 *  Sets the starting index for the records to display.
	 *
	 * @param  start  The new start value
	 */
	public void setStart(int start) {
		this.start = start;
	}


	/**
	 *  Gets the starting index for the records that will be displayed.
	 *
	 * @return    The start value
	 */
	public String getStart() {
		// For display in the UI, add 1
		return Integer.toString(start + 1);
	}


	/**
	 *  Gets the ending index for the records that will be displayed.
	 *
	 * @return    The end value
	 */
	public String getEnd() {
		if (!searchHelper.isEmpty() || start < 0) {
			return null;
		}
		int e = start + numPagingRecords;
		int n = searchHelper.getNumHits();
		return Integer.toString(e < n ? e : n);
	}


	/**
	 *  Gets the offset into the results array to begin iterating.
	 *
	 * @return    The offset value
	 */
	public String getOffset() {
		return Integer.toString(start);
	}


	/**
	 *  Gets the length of iterations to loop over the results array.
	 *
	 * @return    The length value
	 */
	public String getLength() {
		return Integer.toString(numPagingRecords);
	}

	public int getNumPagingRecords () {
		return numPagingRecords;
	}

	/**
	 *  Sets the number of records to display per paiging request. Defaults to 10.
	 *
	 * @param  numPagingRecords  The new numPagingRecords value
	 */
	public void setNumPagingRecords(int numPagingRecords) {
		this.numPagingRecords = numPagingRecords;
	}

	/**
	 *  Gets the query string entered by the user, encoded for use in a URL string.
	 *
	 * @return    The query value ncoded for use in a URL string.
	 */
	public String getQe() {
		try {
			return URLEncoder.encode(queryString, "utf-8");
		} catch (UnsupportedEncodingException e) {
			prtln("getQe(): " + e);
			return "";
		}
	}

	/**
	 *  Gets the HTTP parameters that should be used to retrieve the next set of results.
	 *
	 * @return    Everything after the ? that should be included in the pager URL.
	 */
	public String getNextResultsUrl() {
		if (!searchHelper.isEmpty() || start < 0) {
			return null;
		}

		int e = start + numPagingRecords;
		int n = searchHelper.getNumHits();
		if (e >= n) {
			return null;
		}
		String end = getEnd();
		if (end == null) {
			return null;
		}

		return "q=" + getQe() +
			"&s=" + end +
			getNonPaigingParams();
	}


	/**
	 *  Gets the HTTP parameters that should be used to retrieve the previous set of results.
	 *
	 * @return    Everything after the ? that should be included in the pager URL.
	 */
	public String getPrevResultsUrl() {
		if (!searchHelper.isEmpty() || start <= 0) {
			return null;
		}

		int p = start - numPagingRecords;
		int prev = p > 0 ? p : 0;

		return "q=" + getQe() +
			"&s=" + prev +
			getNonPaigingParams();
	}


	/**
	 *  Sets the request attribute of the DCSViewForm object.
	 *
	 * @param  request  The new request value
	 */
	public void setRequest(HttpServletRequest request) {
		this.request = request;
	}

	public String getFrameworkConfigFormat () {
		return this.frameworkConfigFormat;
	}
	
	public void setFrameworkConfigFormat (String format) {
		this.frameworkConfigFormat = format;
	}

	private String nonPaigingParams = null;


	/**
	 *  Sets the nonPaigingParams attribute of the DCSViewForm object
	 *
	 * @param  nonPaigingParams  The new nonPaigingParams value
	 */
	public void setNonPaigingParams(String nonPaigingParams) {
		this.nonPaigingParams = nonPaigingParams;
	}


	/**
	 *  Gets all the parameters that existed in the request other than those used for
	 *  paiging.
	 *
	 * @return    The NonPaigingParams returned as an HTTP query string.
	 */
	public final String getNonPaigingParams() {
		return nonPaigingParams;
	}

	public SchemEditActionErrors validate(ActionMapping mapping,
                                 HttpServletRequest request) {
        SchemEditActionErrors errors = new SchemEditActionErrors();
		prtln ("validate()");
        return errors;
    }

	//================================================================

	/**
	 *  Output a line of text to standard out, with datestamp, if debug is set to true.
	 *
	 * @param  s  The String that will be output.
	 */
	protected static void prtln(String s) {
		if (debug) {
			System.out.println("DCSViewForm: " + s);
		}
	}

	protected static void prtlnError(String s) {
		System.out.println("DCSViewForm: " + s);
	}

	/**
	 *  Return a string for the current time and date, sutiable for display in log files and
	 *  output to standout:
	 *
	 * @return    The dateStamp value
	 */
	private final static String getDateStamp() {
		return
			new SimpleDateFormat("MMM d, yyyy h:mm:ss a zzz").format(new Date());
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


