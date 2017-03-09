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
package org.dlese.dpc.oai.harvester.action.form;

import org.dlese.dpc.index.*;
import org.dlese.dpc.index.reader.*;

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
 *  A Struts ActionForm for displaying harvest log reports that are stored in a
 *  SimpleLuceneIndex.
 *
 * @author    John Weatherley
 * @see       org.dlese.dpc.oai.harvester.action.HarvestReportAction
 */
public final class HarvestReportForm extends ActionForm implements Serializable {
	private static boolean debug = false;
	private int numPagingRecords = 10;
	private int start = -1;
	private HttpServletRequest request;

	private String queryString = null;
	private String refineQueryString = null;
	private XMLDocReader docReader = null;
	private String metadata = null;
	private String reportTitle = null;
	private String selectedCollection = null;
	private String[] selectedCollections = null;
	private String[] selectedFormats = null;
	private List collections = null;
	private List collectionLabels = null;
	private List formats = null;
	private String contextURL = null;
	private ResultDoc resultDoc = null;
	private ResultDocList resultDocs = null;


	/**  Constructor for the HarvestReportForm object */
	public HarvestReportForm() { }

	
	/**
	 *  Gets the contextURL attribute of the HarvestReportForm object
	 *
	 * @return    The contextURL value
	 */
	public String getContextURL() {
		return contextURL;
	}



	/**
	 *  Sets the contextURL attribute of the HarvestReportForm object
	 *
	 * @param  contextURL  The new contextURL value
	 */
	public void setContextURL(String contextURL) {
		this.contextURL = contextURL;
	}



	/**
	 *  Sets the collections attribute of the HarvestReportForm object
	 *
	 * @param  collections  The new collections value
	 */
	public void setCollections(List collections) {
		this.collections = collections;
	}


	/**
	 *  Gets the collections attribute of the HarvestReportForm object
	 *
	 * @return    The collections value
	 */
	public List getCollections() {
		if (collections == null)
			return new ArrayList();
		return collections;
	}


	/**
	 *  Sets the collectionLabels attribute of the HarvestReportForm object
	 *
	 * @param  collectionLabels  The new collectionLabels value
	 */
	public void setCollectionLabels(List collectionLabels) {
		prtln("setCollectionLabels() size ");
		this.collectionLabels = collectionLabels;
	}


	/**
	 *  Gets the collectionLabels attribute of the HarvestReportForm object
	 *
	 * @return    The collectionLabels value
	 */
	public List getCollectionLabels() {
		return collectionLabels;
	}



	/**
	 *  Gets the formats attribute of the HarvestReportForm object
	 *
	 * @return    The formats value
	 */
	public List getFormats() {
		return formats;
	}



	/**
	 *  Sets the formats attribute of the HarvestReportForm object
	 *
	 * @param  formats  The new formats value
	 */
	public void setFormats(List formats) {
		prtln("setFormats() size ");
		this.formats = formats;
	}



	/**
	 *  Gets the formatLabels attribute of the HarvestReportForm object
	 *
	 * @return    The formatLabels value
	 */
	public List getFormatLabels() {
		return getFormats();
	}


	/**
	 *  Gets the sfmts attribute of the HarvestReportForm object
	 *
	 * @return    The sfmts value
	 */
	public String[] getSfmts() {
		if (selectedFormats == null) {
			selectedFormats = new String[1];
			if (getFormats() == null || getFormats().size() == 0)
				selectedFormats[0] = "";
			else
				selectedFormats[0] = (String) getCollections().get(0);
		}
		return selectedFormats;
	}



	/**
	 *  Sets the sfmts attribute of the HarvestReportForm object
	 *
	 * @param  selectedFormats  The new sfmts value
	 */
	public void setSfmts(String[] selectedFormats) {
		this.selectedFormats = selectedFormats;
	}



	/**
	 *  Gets the collection that has been selected by the user in the UI via a Select tag.
	 *  For example '0dcc'. For use with a Struts select tag that does not have multiple
	 *  selection enabled.<p>
	 *
	 *  Sample HTML code using Struts:<br>
	 *  <code><br>
	 *  &lt;html:select property="sc" size="1" &gt;<br>
	 *  &nbsp;&nbsp;&lt;html:options name="queryForm" property="collections"
	 *  labelProperty="collectionLabels"/&gt;<br>
	 *  &lt;/html:select&gt; <br>
	 *
	 *
	 * @return    The selected collection.
	 */
	public String getSc() {
		if (selectedCollection == null) {
			if (getCollections() == null || getCollections().size() == 0)
				return "";
			return (String) getCollections().get(0);
		}
		return selectedCollection;
	}


	/**
	 *  Sets the collection that has been selected by the user in the UI via a Select tag.
	 *  For example '0dcc'. For use with a Struts select tag that does not have multiple
	 *  selection enabled.
	 *
	 * @param  selectedCollection  The new sc value
	 */
	public void setSc(String selectedCollection) {
		this.selectedCollection = selectedCollection;
	}


	/**
	 *  Gets the collections that have been selected by the user in the UI. For example
	 *  '0dcc' '0comet'. For use with a Struts select tag that has multiple selection
	 *  enabled.<p>
	 *
	 *  Sample HTML code using Struts:<br>
	 *  <code><br>
	 *  &lt;html:select property="scs" size="5" multiple="t"&gt;<br>
	 *  &nbsp;&nbsp;&lt;html:options name="queryForm" property="collections"
	 *  labelProperty="collectionLabels"/&gt;<br>
	 *  &lt;/html:select&gt; <br>
	 *
	 *
	 * @return    The selected collections.
	 */
	public String[] getScs() {
		if (selectedCollections == null) {
			selectedCollections = new String[1];
			if (getCollections() == null || getCollections().size() == 0)
				selectedCollections[0] = "";
			else
				selectedCollections[0] = (String) getCollections().get(0);
		}
		return selectedCollections;
	}


	/**
	 *  Sets the collection that has been selected by the user in the UI. For example '0dcc'
	 *  '0comet'. For use with a Struts select tag that has multiple selection enabled.
	 *
	 * @param  selectedCollections  The new sc value
	 */
	public void setScs(String[] selectedCollections) {
		this.selectedCollections = selectedCollections;
	}


	/**
	 *  Gets the selected collection(s) parameter to be inserted in the URL that gets the
	 *  next set of results.
	 *
	 * @return    The scparams value.
	 */
	public String getScparams() {
		if (selectedCollection != null)
			return "&sc=" + selectedCollection;
		else if (selectedCollections != null && selectedCollections.length > 0) {
			String sparams = "&scs=" + selectedCollections[0];
			for (int i = 1; i < selectedCollections.length; i++)
				sparams += "&scs=" + selectedCollections[i];
			return sparams;
		}
		return "";
	}


	/*
	 *  public void setDocReader(XMLDocReader docReader)
	 *  {
	 *  this.docReader = docReader;
	 *  }
	 *  public XMLDocReader getDocReader()
	 *  {
	 *  return docReader;
	 *  }
	 */
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
	 *  Sets the search results returned by the {@link
	 *  org.dlese.dpc.index.SimpleLuceneIndex}.
	 *
	 * @param  results  The new results value.
	 */
	public void setResults(ResultDocList results) {
		resultDocs = results;
	}


	/**
	 *  Sets the result attribute of the HarvestReportForm object
	 *
	 * @param  resultDoc  The new result value
	 */
	public void setResult(ResultDoc resultDoc) {
		this.resultDoc = resultDoc;
	}


	/**
	 *  Gets the result attribute of the HarvestReportForm object
	 *
	 * @return    The result value
	 */
	public ResultDoc getResult() {
		return resultDoc;
	}


	/**
	 *  Sets the metadata attribute of the HarvestReportForm object
	 *
	 * @param  metadata  The new metadata value
	 */
	public void setMetadata(String metadata) {
		this.metadata = metadata;
	}


	/**
	 *  Gets the metadata attribute of the HarvestReportForm object
	 *
	 * @return    The metadata value
	 */
	public String getMetadata() {
		/*
		 *  if(metadata == null)
		 *  return "";
		 */
		return metadata;
	}


	/**
	 *  Gets the numResults attribute of the HarvestReportForm object
	 *
	 * @return    The numResults value
	 */
	public String getNumResults() {
		if (resultDocs != null)
			return Integer.toString(resultDocs.size());

		else
			return "0";
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
	 *  Sets the q attribute of the HarvestReportForm object
	 *
	 * @param  queryString  The new q value
	 */
	public void setQ(String queryString) {
		this.queryString = queryString;
	}


	/**
	 *  Gets the refined query string entered by the user, used to search within results.
	 *
	 * @return    The query value.
	 */
	public String getRq() {
		return refineQueryString;
	}


	/**
	 *  Sets the refined query string entered by the user, used to search within results.
	 *
	 * @param  refineQueryString  The new rq value
	 */
	public void setRq(String refineQueryString) {
		this.refineQueryString = refineQueryString;
	}


	/**
	 *  Gets all request parameters except the refined query Rq parameter.
	 *
	 * @return    The nrqParams value.
	 */
	public ArrayList getNrqParams() {
		if (request == null)
			return null;

		Enumeration params = request.getParameterNames();
		String param;
		String vals[];
		ArrayList paramPairs = new ArrayList();
		while (params.hasMoreElements()) {
			param = (String) params.nextElement();
			if (!param.equals("rq") &&
				!param.equals("s")) {
				vals = request.getParameterValues(param);
				for (int i = 0; i < vals.length; i++)
					paramPairs.add(new ParamPair(param, vals[i]));
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


		/**  Constructor for the ParamPair object */
		public ParamPair() { }


		/**
		 *  Constructor for the ParamPair object
		 *
		 * @param  param  The parameter name.
		 * @param  val    The parameter value.
		 */
		public ParamPair(String param, String val) {
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
	 *  Sets the reportTitle attribute of the HarvestReportForm object
	 *
	 * @param  reportTitle  The new reportTitle value
	 */
	public void setReportTitle(String reportTitle) {
		this.reportTitle = reportTitle;
	}


	/**
	 *  Gets the reportTitle attribute of the HarvestReportForm object
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
		if (resultDocs == null || start < 0)
			return null;
		int e = start + numPagingRecords;
		int n = resultDocs.size();
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


	/**
	 *  Sets the number of records to display per paiging request. Defaults to 10.
	 *
	 * @param  numPagingRecords  The new numPagingRecords value
	 */
	public void setNumPagingRecords(int numPagingRecords) {
		this.numPagingRecords = numPagingRecords;
	}


	/**
	 *  Gets the HTTP parameters that should be used to retrieve the next set of results.
	 *
	 * @return    Everything after the ? that should be included in the pager URL.
	 */
	public String getNextResultsUrl() {
		if (resultDocs == null || start < 0)
			return null;

		int e = start + numPagingRecords;
		int n = resultDocs.size();
		if (e >= n)
			return null;
		String end = getEnd();
		if (end == null)
			return null;

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
		if (resultDocs == null || start <= 0)
			return null;

		int p = start - numPagingRecords;
		int prev = p > 0 ? p : 0;

		return "q=" + getQe() +
			"&s=" + prev +
			getNonPaigingParams();
	}


	/**
	 *  Sets the request attribute of the HarvestReportForm object.
	 *
	 * @param  request  The new request value
	 */
	public void setRequest(HttpServletRequest request) {
		this.request = request;
	}


	private String nonPaigingParams = null;


	/**
	 *  Sets the nonPaigingParams attribute of the HarvestReportForm object
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
		/*
		 *  if (request == null)
		 *  return null;
		 *  Enumeration params = request.getParameterNames();
		 *  String param;
		 *  String vals[];
		 *  StringBuffer addParams = new StringBuffer();
		 *  try{
		 *  while (params.hasMoreElements()) {
		 *  param = (String) params.nextElement();
		 *  if (!param.equals("q") &&
		 *  !param.equals("s")) {
		 *  vals = request.getParameterValues(param);
		 *  for (int i = 0; i < vals.length; i++){
		 *  addParams.append("&" + param + "=" + URLEncoder.encode(vals[i],"utf-8"));
		 *  }
		 *  }
		 *  }
		 *  }catch(Exception e){
		 *  addParams.toString();
		 *  }
		 *  return addParams.toString();
		 */
	}


	//================================================================

	/**
	 *  Output a line of text to standard out, with datestamp, if debug is set to true.
	 *
	 * @param  s  The String that will be output.
	 */
	private final void prtln(String s) {
		if (debug) {
			System.out.println(getDateStamp() + " " + s);
		}
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



