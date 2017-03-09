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

import org.dlese.dpc.index.*;
import org.dlese.dpc.repository.*;
import org.dlese.dpc.schemedit.*;
import org.dlese.dpc.schemedit.display.SortWidget;
import org.dlese.dpc.schemedit.dcs.*;
import org.dlese.dpc.schemedit.action.*;
import org.dlese.dpc.index.reader.*;
import org.dlese.dpc.util.*;
import org.dlese.dpc.vocab.*;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionError;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionServlet;
import org.apache.struts.util.MessageResources;
import org.apache.struts.util.LabelValueBean;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.io.*;
import java.text.*;
import java.net.URLEncoder;
import org.apache.lucene.document.*;
import org.dlese.dpc.index.document.*;

/**
 *  A Struts Form bean for handling query requests that access a {@link
 *  org.dlese.dpc.index.SimpleLuceneIndex}. This class works in conjuction with
 *  the {@link org.dlese.dpc.schemedit.action.DCSQueryAction} Struts Action
 *  class.
 *
 *@author    Jonathan Ostwald
 *
 */
public final class DCSQueryForm extends ActionForm implements Serializable {
	private static boolean debug = true;
	public static String DEFAULT_REC_SORT = "idvalue";
	public static String RELEVANCE_REC_SORT = "relevance";
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
	private List idMapperErrors = null;
	private List idMapperErrorLabels = null;
	private List formats = null, indexedFormats = null;
	private List indexedFormatsLabels = null;
	private List indexedAccessionStatus = null;
	private String contextURL = null;
	
	private SearchHelper searchHelper = null;
	
	private String showQueryOptions = "false";

	private List editors = null;
	private Map statuses = null;
	private String[] selectedEditors = null;
	private String[] selectedCreators = null;
	private String[] selectedStatuses = null;
	private List collectionLabelValues = null;

	private List sets = null;
	private String searchMode = "raw";
	private String lastSearchString = null;

	private String[] resultsPerPageOptions = {"All", "10", "20", "50", "100"};
	private String resultsPerPage = null;
	private String sortField = null;
	private String validity = null;
	
	private Map sortWidgets = null;


	/**
	 *  Constructor for the DCSQueryForm object
	 */
	public DCSQueryForm() { }


	/**
	 *  Gets the resultsPerPageOptions attribute of the DCSQueryForm object
	 *
	 *@return    The resultsPerPageOptions value
	 */
	public String[] getResultsPerPageOptions() {
		return resultsPerPageOptions;
	}


	/**
	 *  Sets the sortField attribute of the DCSQueryForm object
	 *
	 *@param  sortField  The new sortField value
	 */
	 public void setSortField(String sortField) {
		this.sortField = sortField;
	}


	/**
	 *  Gets the sortField attribute of the DCSQueryForm object
	 *
	 *@return    The sortField value
	 */
	public String getSortField() {
		if (sortField == null || sortField.length() == 0) {
			sortField = DEFAULT_REC_SORT;
		}
		return sortField;
	}
	/**
	* The sortField of the widgets must be indexed!
	* The sortWidgets key must match the sortField for each widget.
	*/
	public Map getSortWidgets() {
		if (sortWidgets == null || sortWidgets.size() == 0) {
			// prtln ("creating new set of sortWidgets");
			sortWidgets = new HashMap();
			sortWidgets.put("idvalue", new SortWidget("idvalue", "Record ID", Constants.DESCENDING));
			sortWidgets.put("dcslastEditorName", new SortWidget("dcslastEditorName", "Last Editor", Constants.ASCENDING));
			sortWidgets.put("dcsstatusLabel", new SortWidget("dcsstatusLabel", "Status", Constants.ASCENDING));
			sortWidgets.put("dcslastTouchDate", new SortWidget("dcslastTouchDate", "Last Touch", Constants.DESCENDING));
		}
		return sortWidgets;
	}
	
	public SortWidget getCurrentSortWidget () {
		return (SortWidget)getSortWidgets().get(getSortField());
	}
	
	public SortWidget getRecordIdWidget () {
		return (SortWidget)getSortWidgets().get("idvalue");
	}
	
	public SortWidget getLastEditorWidget () {
		return (SortWidget)getSortWidgets().get("dcslastEditorName");
	}

	public SortWidget getStatusWidget () {
		return (SortWidget)getSortWidgets().get("dcsstatusLabel");
	}
	
	public SortWidget getLastTouchDateWidget () {
		return (SortWidget)getSortWidgets().get("dcslastTouchDate");
	}	
	
	/**
	 *  A string representation of the numPagingRecords attribute
	 *
	 *@return    The resultsPerPage value
	 */
	public String getResultsPerPage() {
		if (resultsPerPage == null) {
			resultsPerPage = "10";
		}
		return resultsPerPage;
	}


	/**
	 *  Sets the resultsPerPage attribute of the DCSQueryForm object
	 *
	 *@param  s  The new resultsPerPage value
	 */
	public void setResultsPerPage(String s) {
		resultsPerPage = s;
	}


	/**
	 *  Gets the searchMode attribute of the DCSQueryForm object
	 *
	 *@return    The searchMode value
	 */
	public String getSearchMode() {
		return searchMode;
	}


	/**
	 *  Sets the searchMode attribute of the DCSQueryForm object
	 *
	 *@param  s  The new searchMode value
	 */
	public void setSearchMode(String s) {
		searchMode = s;
	}

	/**
	 *  Gets the showQueryOptions attribute of the DCSQueryForm object
	 *
	 *@return    The showQueryOptions value
	 */
	public String getShowQueryOptions() {
		return showQueryOptions;
	}


	/**
	 *  Sets the showQueryOptions attribute of the DCSQueryForm object
	 *
	 *@param  showQueryOptions  The new showQueryOptions value
	 */
	public void setShowQueryOptions(String showQueryOptions) {
		this.showQueryOptions = showQueryOptions;
	}


	/**
	 *  Gets the contextURL attribute of the DCSQueryForm object
	 *
	 *@return    The contextURL value
	 */
	public String getContextURL() {
		return contextURL;
	}



	/**
	 *  Sets the contextURL attribute of the DCSQueryForm object
	 *
	 *@param  contextURL  The new contextURL value
	 */
	public void setContextURL(String contextURL) {
		this.contextURL = contextURL;
	}


	/**
	 *  return the list of Editors for the currently selected collection; we don't
	 *  use "selectedCollection" but rather getSelectedSet().getSetSpec() because
	 *  the former does not seem to be properly initialized after system restart?
	 *
	 *@return    The editors value
	 */
	public List getEditors() {
		return editors;
	}


	/**
	 *  Sets the editors attribute of the DCSBrowseForm object
	 *
	 *@param  editors  The new editors value
	 */
	public void setEditors(List editors) {
		// prtln ("setEditors() size: " + editors.size());
		this.editors = editors;
	}


	/**
	 *  Gets the vld attribute of the DCSQueryForm object
	 *
	 *@return    The vld value
	 */
	public String getVld() {
		return validity;
	}


	/**
	 *  Sets the vld attribute of the DCSQueryForm object
	 *
	 *@param  v  The new vld value
	 */
	public void setVld(String v) {
		validity = v;
	}


	/**
	 *  Gets the ses (Selected Creators) attribute of the DCSBrowseForm object
	 *
	 *@return    The ses value
	 */
	public String[] getSrcs() {
		if (selectedCreators == null) {
			selectedCreators = new String[]{};
		}
		return selectedCreators;
	}


	/**
	 *  Sets the collection that has been selected by the user in the UI. For
	 *  example 'jones'. For use with a Struts select tag that has multiple
	 *  selection enabled.
	 *
	 *@param  selectedCreators  The new ses value
	 */
	public void setSrcs(String[] selectedCreators) {
		this.selectedCreators = selectedCreators;
	}
	
	/**
	 *  Gets the ses (Selected Editors) attribute of the DCSBrowseForm object
	 *
	 *@return    The ses value
	 */
	public String[] getSes() {
		if (selectedEditors == null) {
			selectedEditors = new String[]{};
		}
		return selectedEditors;
	}


	/**
	 *  Sets the collection that has been selected by the user in the UI. For
	 *  example '0dcc' '0comet'. For use with a Struts select tag that has multiple
	 *  selection enabled.
	 *
	 *@param  selectedEditors  The new ses value
	 */
	public void setSes(String[] selectedEditors) {
		this.selectedEditors = selectedEditors;
	}

	/**
	 *  return the list of Statuses for the currently selected collection; we don't
	 *  use "selectedCollection" but rather getSelectedSet().getSetSpec() because
	 *  the former does not seem to be properly initialized after system restart?
	 *
	 *@return    The statuses value
	 */
	public Map getStatuses() {
		return statuses;
	}


	/**
	 *  Sets the statuses attribute of the DCSBrowseForm object
	 *
	 *@param  statuses  The new statuses value
	 */
	public void setStatuses(Map statuses) {
		// prtln ("setStatuses() size: " + statuses.size());
		this.statuses = statuses;
	}


	/**
	 *  Gets the sss (selectedStatuses) attribute of the DCSBrowseForm object
	 *
	 *@return    The sss value
	 */
	public String[] getSss() {
		if (selectedStatuses == null) {
			selectedStatuses = new String[]{};
		}
		return selectedStatuses;
	}


	/**
	 *  Sets the sss (selectedStatuses) attribute of the DCSBrowseForm object
	 *
	 *@param  selectedStatuses  The new sss value
	 */
	public void setSss(String[] selectedStatuses) {
		this.selectedStatuses = selectedStatuses;
	}


	/**
	 *  Sets the id mapper errors that are available in the index.
	 *
	 *@param  errors  The ID mapper errors, as a list of integers.
	 */
	public void setIdMapperErrors(List errors) {
		idMapperErrors = errors;
		idMapperErrorLabels = null;
		if (idMapperErrors != null && idMapperErrors.size() > 0) {
			idMapperErrorLabels = new ArrayList();
			String tmp;
			for (int i = 0; i < idMapperErrors.size(); i++) {
				tmp = (String) idMapperErrors.get(i);
				if (tmp.equals(DCSQueryAction.SELECT_NONE)) {
					idMapperErrorLabels.add(DCSQueryAction.SELECT_NONE);
				}
				else if (tmp.equals("noerrors")) {
					idMapperErrorLabels.add(DCSQueryAction.NO_ERRORS);
				}
				else {
					idMapperErrorLabels.add(DpcErrors.getMessage(Integer.parseInt(tmp)));
				}
			}
		}
	}


	/**
	 *  Gets the idMapperErrors that are in the index, as integer strings.
	 *
	 *@return    The idMapperErrors value
	 */
	public List getIdMapperErrors() {
		return idMapperErrors;
	}


	/**
	 *  Gets the idMapperErrorLabels, as strings.
	 *
	 *@return    The idMapperErrorLabels value
	 */
	public List getIdMapperErrorLabels() {
		return idMapperErrorLabels;
	}


	/**
	 *  Sets the indexedAccessionStatuses attribute of the DCSQueryForm object
	 *
	 *@param  statusus  The new indexedAccessionStatuses value
	 */
	public void setIndexedAccessionStatuses(List statusus) {
		indexedAccessionStatus = statusus;
	}


	/**
	 *  Gets the indexedAccessionStatuses attribute of the DCSQueryForm object
	 *
	 *@return    The indexedAccessionStatuses value
	 */
	public List getIndexedAccessionStatuses() {
		return indexedAccessionStatus;
	}

	/**
	 *  Gets all possible metadata formats that may be disiminated by the
	 *  RepositoryManager. This includes formats that are available via the
	 *  XMLConversionService.
	 *
	 *@return    The formats value
	 */
	public List getFormats() {
		return formats;
	}



	/**
	 *  Sets the formats attribute of the DCSQueryForm object
	 *
	 *@param  formats  The new formats value
	 */
	public void setFormats(List formats) {
		// prtln("setFormats() size ");
		this.formats = formats;
	}



	/**
	 *  Gets all formats that exist natively in the index.
	 *
	 *@return    The indexedFormats value
	 */
	public List getIndexedFormats() {
		return indexedFormats;
	}



	/**
	 *  Gets all formats that exist natively in the index.
	 *
	 *@param  formats  The new indexedFormats value
	 */
	public void setIndexedFormats(List formats) {
		indexedFormats = formats;
	}



	/**
	 *  Gets all formats that exist natively in the index.
	 *
	 *@return    The indexedFormatsLabels value
	 */
	public List getIndexedFormatsLabels() {
		return indexedFormatsLabels;
	}



	/**
	 *  Gets all formats that exist natively in the index.
	 *
	 *@param  formatsLabels  The new indexedFormatsLabels value
	 */
	public void setIndexedFormatsLabels(List formatsLabels) {
		indexedFormatsLabels = formatsLabels;
	}

	/**
	 *  Gets the formatLabels attribute of the DCSQueryForm object
	 *
	 *@return    The formatLabels value
	 */
	public List getFormatLabels() {
		return getFormats();
	}


	/**
	 *  Gets the selected formats. These include formats that are available via the
	 *  XMLConversionService.
	 *
	 *@return    The sfmts value
	 */
	public String[] getSfmts() {
		if (selectedFormats == null) {
			selectedFormats = new String[1];
			if (getFormats() == null || getFormats().size() == 0) {
				selectedFormats[0] = "";
			}
		}
		return selectedFormats;
	}



	/**
	 *  Sets the selected formats. These include formats that are available via the
	 *  XMLConversionService.
	 *
	 *@param  selectedFormats  The new sfmts value
	 */
	public void setSfmts(String[] selectedFormats) {
		this.selectedFormats = selectedFormats;
	}



	/**
	 *  Gets the collection that has been selected by the user in the UI via a
	 *  Select tag. For example '0dcc'. For use with a Struts select tag that does
	 *  not have multiple selection enabled.<p>
	 *
	 *  Sample HTML code using Struts:<br>
	 *  <code><br>
	 *  &lt;html:select property="sc" size="1" &gt;<br>
	 *  &nbsp;&nbsp;&lt;html:options name="queryForm" property="collections"
	 *  labelProperty="collectionLabels"/&gt;<br>
	 *  &lt;/html:select&gt; <br>
	 *
	 *
	 *@return    The selected collection.
	 */
	public String getSc() {
		if (selectedCollection == null) {
			return "";
		}
		return selectedCollection;
	}


	/**
	 *  Sets the collection that has been selected by the user in the UI via a
	 *  Select tag. For example '0dcc'. For use with a Struts select tag that does
	 *  not have multiple selection enabled.
	 *
	 *@param  selectedCollection  The new sc value
	 */
	public void setSc(String selectedCollection) {

		this.selectedCollection = selectedCollection;
	}


	/**
	 *  Gets the collections that have been selected by the user in the UI. For
	 *  example '0dcc' '0comet'. For use with a Struts select tag that has multiple
	 *  selection enabled.<p>
	 *
	 *  Sample HTML code using Struts:<br>
	 *  <code><br>
	 *  &lt;html:select property="scs" size="5" multiple="t"&gt;<br>
	 *  &nbsp;&nbsp;&lt;html:options name="queryForm" property="collections"
	 *  labelProperty="collectionLabels"/&gt;<br>
	 *  &lt;/html:select&gt; <br>
	 *
	 *
	 *@return    The selected collections.
	 */
	public String[] getScs() {
		if (selectedCollections == null) {
			selectedCollections = new String[]{};
		}
		return selectedCollections;
	}


	String[] selectedIdMapperErrors = null;


	/**
	 *  Gets the selectedIdMapperErrors attribute of the DCSQueryForm object
	 *
	 *@return    The selectedIdMapperErrors value
	 */
	public String[] getSelectedIdMapperErrors() {
		if (selectedIdMapperErrors == null) {
			selectedIdMapperErrors = new String[]{DCSQueryAction.SELECT_NONE};
		}
		return selectedIdMapperErrors;
	}

	String[] selectedAccessionStatuses = null;


	/**
	 *  DESCRIPTION
	 *
	 *@return    DESCRIPTION
	 */
	public String[] getselectedAccessionStatuses() {
		if (selectedAccessionStatuses == null) {
			selectedAccessionStatuses = new String[]{DCSQueryAction.SELECT_ALL};
		}
		return selectedAccessionStatuses;
	}


	/**
	 *  DESCRIPTION
	 *
	 *@param  selectedAccessionStatuses  DESCRIPTION
	 */
	public void setselectedAccessionStatuses(String[] selectedAccessionStatuses) {
		this.selectedAccessionStatuses = selectedAccessionStatuses;
	}


	/**
	 *  Sets the selectedIdMapperErrors attribute of the DCSQueryForm object
	 *
	 *@param  selectedIdMapperErrors  The new selectedIdMapperErrors value
	 */
	public void setSelectedIdMapperErrors(String[] selectedIdMapperErrors) {
		this.selectedIdMapperErrors = selectedIdMapperErrors;
	}


	/**
	 *  DESCRIPTION
	 *
	 *@param  mapping  DESCRIPTION
	 *@param  request  DESCRIPTION
	 */
	public void reset(ActionMapping mapping, javax.servlet.http.HttpServletRequest request) {
		//selectedIdMapperErrors = null;
		selectedCollections = null;
		selectedFormats = null;
		selectedEditors = null;
		selectedCreators = null;
		selectedStatuses = null;
	}


	/**
	 *  Sets the collection that has been selected by the user in the UI. For
	 *  example '0dcc' '0comet'. For use with a Struts select tag that has multiple
	 *  selection enabled.
	 *
	 *@param  selectedCollections  The new sc value
	 */
	public void setScs(String[] selectedCollections) {
		this.selectedCollections = selectedCollections;
	}


	/**
	 *  Gets the selected collection(s) parameter to be inserted in the URL that
	 *  gets the next set of results.
	 *
	 *@return    The scparams value.
	 */
	public String getScparams() {
		if (selectedCollection != null) {
			return "&sc=" + selectedCollection;
		}
		else if (selectedCollections != null && selectedCollections.length > 0) {
			String sparams = "&scs=" + selectedCollections[0];
			for (int i = 1; i < selectedCollections.length; i++) {
				sparams += "&scs=" + selectedCollections[i];
			}
			return sparams;
		}
		return "";
	}

	/**
	* for sort debugging
	*/
	private void printResults (ResultDocList results) {
		prtln ("-- Results --");
		if (results == null){
			prtln ("no results provided");
			return;
		}
		int limiter=20;
		for (int i=0;i<results.size() && i<limiter;i++) {
			ResultDoc result = results.get(i);
			DocumentMap docMap = result.getDocMap();
			XMLDocReader docReader = (XMLDocReader)result.getDocReader();
			/* String id = docReader.getId(); */
			String id = (String)docMap.get("id");
			String editor = (String)docMap.get("dcslastEditor");
			String idvalue = (String)docMap.get("idvalue");
			String status = (String)docMap.get("dcsstatus");
			
			String t = (String)docMap.get ("dcslastTouchDate");
			long modTime = -1;
			try {
				modTime = DateFieldTools.stringToTime(t);
			} catch (ParseException pe) {
				System.err.println("Error in printResults(): " + pe);
			}			
			SimpleDateFormat df = new SimpleDateFormat("MMM' 'dd', 'yyyy");
			String lastTouchDate = df.format(new Date(modTime));
			
			// prtln (i+"\t"+idvalue+"\t"+editor+"\t"+status+"\t"+lastTouchDate+"  ("+t+")");
			prtln (i+"\t"+idvalue+"\t\t"+editor+"\t\t"+status);
		}
	}

	/**
	 *  Sets the search results returned by the {@link
	 *  org.dlese.dpc.index.SimpleLuceneIndex} after sorting them. <p>
	 *
	 *  we DON'T want to sort results if:
	 *  <li> this is a NEW sort (if searchString is different from
	 *  lastSearchString)
	 *  <li> there is no sortField value to sort it by (note: we explicitly set
	 *  sortField to null in the case of a new search)
	 *
	 *@param  results  The new results value.
	 */
/* 	 public void setResults(ResultDoc[] results) {
		resultDocs = results;
	} */

	public void setResults(SearchHelper results) {
		searchHelper = results;
	}
	
	/**
	 *  Sets the metadata attribute of the DCSQueryForm object
	 *
	 *@param  metadata  The new metadata value
	 */
	public void setMetadata(String metadata) {
		this.metadata = metadata;
	}


	/**
	 *  Gets the metadata attribute of the DCSQueryForm object
	 *
	 *@return    The metadata value
	 */
	public String getMetadata() {
		return metadata;
	}

	public List getHits () {
		return searchHelper.getHits(start, numPagingRecords);
	}
		

	/**
	 *  Gets the numResults attribute of the DCSQueryForm object
	 *
	 *@return    The numResults value
	 */
	public String getNumResults() {

		if (searchHelper == null) {
			prtln ("WARNING: searchHelper is NULL");
			return "0";
		}
		
		return Integer.toString (searchHelper.getNumHits());
	}



	/**
	 *  Gets the query string entered by the user.
	 *
	 *@return    The query value.
	 */
	public String getQ() {
		return queryString;
	}


	/**
	 *  Gets the query string entered by the user, encoded for use in a URL string.
	 *
	 *@return    The query value ncoded for use in a URL string.
	 */
	public String getQe() {
		if (queryString == null)
			return "";
		
		try {
			return URLEncoder.encode(queryString, "utf-8");
		} catch (UnsupportedEncodingException e) {
			prtln("getQe(): " + e);
			return "";
		}
	}


	/**
	 *  Sets the q attribute of the DCSQueryForm object
	 *
	 *@param  queryString  The new q value
	 */
	public void setQ(String queryString) {
		this.queryString = queryString;
	}


	/**
	 *  Gets the refined query string entered by the user, used to search within
	 *  results.
	 *
	 *@return    The query value.
	 */
	public String getRq() {
		return refineQueryString;
	}


	/**
	 *  Sets the refined query string entered by the user, used to search within
	 *  results.
	 *
	 *@param  refineQueryString  The new rq value
	 */
	public void setRq(String refineQueryString) {
		this.refineQueryString = refineQueryString;
	}


	/**
	 *  Gets all request parameters except the refined query Rq parameter.
	 *
	 *@return    The nrqParams value.
	 */
	public ArrayList getNrqParams() {
		if (request == null) {
			return null;
		}

		Enumeration params = request.getParameterNames();
		String param;
		String vals[];
		ArrayList paramPairs = new ArrayList();
		while (params.hasMoreElements()) {
			param = (String) params.nextElement();
			if (!param.equals("rq") &&
					!param.equals("s")) {
				vals = request.getParameterValues(param);
				for (int i = 0; i < vals.length; i++) {
					paramPairs.add(new ParamPair(param, vals[i]));
				}
			}
		}
		return paramPairs;
	}


	/**
	 *  Holds paramter, value pairs.
	 *
	 *@author    John Weatherley
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
		 *@param  param  The parameter name.
		 *@param  val    The parameter value.
		 */
		public ParamPair(String param, String val) {
			this.param = param;
			this.val = val;
		}


		/**
		 *  Gets the parameter name.
		 *
		 *@return    The parameter name.
		 */
		public String getName() {
			return param;
		}


		/**
		 *  Gets the parameter value.
		 *
		 *@return    The parameter value.
		 */
		public String getVal() {
			return val;
		}
	}


	/**
	 *  Sets the reportTitle attribute of the DCSQueryForm object
	 *
	 *@param  reportTitle  The new reportTitle value
	 */
	public void setReportTitle(String reportTitle) {
		this.reportTitle = reportTitle;
	}


	/**
	 *  Gets the reportTitle attribute of the DCSQueryForm object
	 *
	 *@return    The reportTitle value
	 */
	public String getReportTitle() {
		return reportTitle;
	}



	// ---------------- Pager --------------------

	/**
	 *  Sets the starting index for the records to display.
	 *
	 *@param  start  The new start value
	 */
	public void setStart(int start) {
		this.start = start;
	}


	/**
	 *  Gets the starting index for the records that will be displayed.
	 *
	 *@return    The start value
	 */
	public String getStart() {
		// For display in the UI, add 1
		return Integer.toString(start + 1);
	}


	/**
	 *  Gets the ending index for the records that will be displayed.
	 *
	 *@return    The end value
	 */
	public String getEnd() {
		// if (resultDocs == null || start < 0) {
		if (searchHelper.isEmpty() || start < 0) {
			return null;
		}
		int e = start + numPagingRecords;
		// int n = resultDocs.length;
		int n = searchHelper.getNumHits();
		return Integer.toString(e < n ? e : n);
	}


	/**
	 *  Gets the offset into the results array to begin iterating.
	 *
	 *@return    The offset value
	 */
	public String getOffset() {
		return Integer.toString(start);
	}


	/**
	 *  Gets the length of iterations to loop over the results array.
	 *
	 *@return    The length value
	 */
	public String getLength() {
		return Integer.toString(numPagingRecords);
	}


	/**
	 *  Gets the numPagingRecords attribute of the DCSQueryForm object
	 *
	 *@return    The numPagingRecords value
	 */
	public int getNumPagingRecords() {
		return numPagingRecords;
	}


	/**
	 *  Sets the number of records to display per paiging request. Defaults to 10.
	 *
	 *@param  numPagingRecords  The new numPagingRecords value
	 */
	public void setNumPagingRecords(int numPagingRecords) {
		this.numPagingRecords = numPagingRecords;
	}


	/**
	 *  Gets the HTTP parameters that should be used to retrieve the next set of
	 *  results.
	 *
	 *@return    Everything after the ? that should be included in the pager URL.
	 */
	public String getNextResultsUrl() {
		// if (resultDocs == null || start < 0) {
		if (searchHelper.isEmpty() || start < 0) {
			return null;
		}

		int e = start + numPagingRecords;
		// int n = resultDocs.length;
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
	 *  Gets the HTTP parameters that should be used to retrieve the previous set
	 *  of results.
	 *
	 *@return    Everything after the ? that should be included in the pager URL.
	 */
	public String getPrevResultsUrl() {
		// if (resultDocs == null || start <= 0) {
		if (searchHelper.isEmpty() || start <= 0) {
			return null;
		}

		int p = start - numPagingRecords;
		int prev = p > 0 ? p : 0;

		return "q=" + getQe() +
				"&s=" + prev +
				getNonPaigingParams();
	}


	/**
	 *  Sets the request attribute of the DCSQueryForm object.
	 *
	 *@param  request  The new request value
	 */
	public void setRequest(HttpServletRequest request) {
		this.request = request;
	}


	private String nonPaigingParams = null;


	/**
	 *  Sets the nonPaigingParams attribute of the DCSQueryForm object
	 *
	 *@param  nonPaigingParams  The new nonPaigingParams value
	 */
	public void setNonPaigingParams(String nonPaigingParams) {
		this.nonPaigingParams = nonPaigingParams;
	}


	/**
	 *  Gets all the parameters that existed in the request other than those used
	 *  for paiging.
	 *
	 *@return    The NonPaigingParams returned as an HTTP query string.
	 */
	public final String getNonPaigingParams() {
		return nonPaigingParams;
	}

	private int paigingParam = 0;
	
	/**
	* the current result page.
	*/
	public int getPaigingParam () {
		if (this.searchHelper == null) {
			return 0;
		}
		int recIndex = this.searchHelper.getCurrentRecIndex();
		if (recIndex == -1)
			recIndex = start;
		if (numPagingRecords > 0) {
			return (recIndex / numPagingRecords) * numPagingRecords;
		}
		else {
			return 0;
		}
	}
	
	public int getPaigingParam (String id) {
		if (this.searchHelper == null) 
			return 0;
		int recIndex = this.searchHelper.getIndexOf(id);
		if (numPagingRecords > 0) {
			return (recIndex / numPagingRecords) * numPagingRecords;
		}
		else {
			return 0;
		}
	}
	
	private String sortWidgetParams = null;


	/**
	 *  Sets the sortWidgetParams attribute of the DCSQueryForm object.<p>
	 *  Used by the sortWidget (see tags/sortWidget.tag) to contstruct a URL that
	 *  will reproduce search but accomodate new sorting, ordering, and paiging params.
	 *
	 *@param  sortWidgetParams  The new sortWidgetParams value
	 */
	public void setSortWidgetParams(String sortWidgetParams) {
		this.sortWidgetParams = sortWidgetParams;
	}


	/**
	 *  Gets all the parameters that existed in the request other than those used
	 *  for paiging or sorting.<P>
	 *
	 *@return    The SortWidgetParams returned as an HTTP query string.
	 */
	public final String getSortWidgetParams() {
		return sortWidgetParams;
	}

	//================================================================

	/**
	 *  Output a line of text to standard out
	 *
	 *@param  s  The String that will be output.
	 */
	private final void prtln(String s) {
		if (debug) {
			// System.out.println(getDateStamp() + " " + s);
			SchemEditUtils.prtln (s, "queryForm");
		}
	}


	/**
	 *  Return a string for the current time and date, sutiable for display in log
	 *  files and output to standout:
	 *
	 *@return    The dateStamp value
	 */
	private final static String getDateStamp() {
		return
				new SimpleDateFormat("MMM d, yyyy h:mm:ss a zzz").format(new Date());
	}


	/**
	 *  Sets the debug attribute of the object
	 *
	 *@param  db  The new debug value
	 */
	public static void setDebug(boolean db) {
		debug = db;
	}
}


