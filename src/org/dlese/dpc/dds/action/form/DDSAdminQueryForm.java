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
package org.dlese.dpc.dds.action.form;

import org.dlese.dpc.index.*;
import org.dlese.dpc.dds.action.*;
import org.dlese.dpc.index.reader.*;
import org.dlese.dpc.util.*;
import org.dlese.dpc.vocab.*;
import org.dlese.dpc.repository.*;
import org.dlese.dpc.services.mmd.MmdException;
import org.dlese.dpc.services.mmd.MmdRec;
import org.dlese.dpc.services.mmd.MmdWarning;
import org.dlese.dpc.services.mmd.Query;

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
 *  A Struts Form bean for handling query requests that access a {@link
 *  org.dlese.dpc.index.SimpleLuceneIndex}. This class works in conjuction with the {@link
 *  org.dlese.dpc.dds.action.DDSAdminQueryAction} Struts Action class.
 *
 * @author     John Weatherley
 */
public final class DDSAdminQueryForm extends VocabForm implements Serializable {
	private static boolean debug = true;
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
	private String[] ky = null;
	private String[] selectedFormats = null;
	private String[] selectedIndexedFormats = null;
	private List collections = null;
	private List idMapperErrors = null;
	private List idMapperErrorLabels = null;
	private List collectionLabels = null;
	private List formats = null, indexedFormats = null;
	private List indexedFormatsLabels = null;
	private List indexedAccessionStatus = null;
	private String contextURL = null;
	private ResultDoc resultDoc = null;
	private ResultDocList resultDocs = null;
	private String showQueryOptions = "false";
	private MetadataVocabInputState vocabInputState = null;


	/**  Constructor for the DDSAdminQueryForm object */
	public DDSAdminQueryForm() { }


	/**
	 *  Sets the vocabInputState attribute of the DDSAdminQueryForm object
	 *
	 * @param  vocabInputState  The new vocabInputState value
	 */
	public void setVocabInputState(MetadataVocabInputState vocabInputState) {
		this.vocabInputState = vocabInputState;
	}


	/**
	 *  Gets the vocabInputState attribute of the DDSAdminQueryForm object
	 *
	 * @return    The vocabInputState value
	 */
	public MetadataVocabInputState getVocabInputState() {
		if (vocabInputState == null) {
			vocabInputState = new MetadataVocabInputState();
		}
		return vocabInputState;
	}


	/**
	 *  Gets the showQueryOptions attribute of the DDSAdminQueryForm object
	 *
	 * @return    The showQueryOptions value
	 */
	public String getShowQueryOptions() {
		return showQueryOptions;
	}


	/**
	 *  Sets the showQueryOptions attribute of the DDSAdminQueryForm object
	 *
	 * @param  showQueryOptions  The new showQueryOptions value
	 */
	public void setShowQueryOptions(String showQueryOptions) {
		this.showQueryOptions = showQueryOptions;
	}


	/**
	 *  Gets the contextURL attribute of the DDSAdminQueryForm object
	 *
	 * @return    The contextURL value
	 */
	public String getContextURL() {
		return contextURL;
	}



	/**
	 *  Sets the contextURL attribute of the DDSAdminQueryForm object
	 *
	 * @param  contextURL  The new contextURL value
	 */
	public void setContextURL(String contextURL) {
		this.contextURL = contextURL;
	}


	/**
	 *  Sets the id mapper errors that are available in the index.
	 *
	 * @return    The idMapperErrors value
	 */
	/* public void setIdMapperErrors(List errors) {
		idMapperErrors = errors;
		idMapperErrorLabels = null;
		if (idMapperErrors != null && idMapperErrors.size() > 0) {
			idMapperErrorLabels = new ArrayList();
			String tmp;
			for (int i = 0; i < idMapperErrors.size(); i++) {
				tmp = (String) idMapperErrors.get(i);
				if (tmp.equals(DDSAdminQueryAction.SELECT_NONE))
					idMapperErrorLabels.add(DDSAdminQueryAction.SELECT_NONE);
				else if (tmp.equals("noerrors"))
					idMapperErrorLabels.add(DDSAdminQueryAction.NO_ERRORS);
				else
					idMapperErrorLabels.add(DpcErrors.getMessage(Integer.parseInt(tmp)));
			}
		}
	} */
	/**
	 *  Gets the idMapperErrors that are in the index, as integer strings.
	 *
	 * @return    The idMapperErrors value
	 */
	public List getIdMapperErrors() {
		if (idMapperErrors == null) {
			RepositoryManager rm =
				(RepositoryManager) servlet.getServletContext().getAttribute("repositoryManager");
			idMapperErrors = rm.getIndexedIdMapperErrors();
		}
		return idMapperErrors;
	}


	/**
	 *  Gets the idMapperErrorLabels, as strings.
	 *
	 * @return    The idMapperErrorLabels value
	 */
	public List getIdMapperErrorLabels() {
		List errorCodes = getIdMapperErrors();
		if (idMapperErrorLabels == null) {
			if (errorCodes != null && errorCodes.size() > 0) {
				idMapperErrorLabels = new ArrayList();
				String tmp;
				for (int i = 0; i < errorCodes.size(); i++) {
					tmp = (String) errorCodes.get(i);
					if (tmp.equals(DDSAdminQueryAction.SELECT_NONE))
						idMapperErrorLabels.add(DDSAdminQueryAction.SELECT_NONE);
					else if (tmp.equals("noerrors"))
						idMapperErrorLabels.add(DDSAdminQueryAction.NO_ERRORS);
					else
						idMapperErrorLabels.add(DpcErrors.getMessage(Integer.parseInt(tmp)));
				}
			}
		}
		return idMapperErrorLabels;
	}



	/**
	 *  Gets the indexedAccessionStatuses attribute of the DDSAdminQueryForm object
	 *
	 * @return    The indexedAccessionStatuses value
	 */
	public List getIndexedAccessionStatuses() {
		RepositoryManager rm =
			(RepositoryManager) servlet.getServletContext().getAttribute("repositoryManager");
		return rm.getIndexedAccessionStatuses();
	}



	/**
	 *  Gets the collections that are configured in the repository, sorted.
	 *
	 * @return    The collections value
	 */
	public List getCollections() {
		try {
			if (getServlet().getServletContext().getAttribute("reload_admin_collection_menus") != null)
				collections = null;
			if (collections == null) {
				RepositoryManager rm =
					(RepositoryManager) servlet.getServletContext().getAttribute("repositoryManager");
				collections = rm.getConfiguredSets();
				// Sort the collection list by their label...
				if (collections != null) {
					ArrayList temp = new ArrayList(collections.size());
					setField("dlese_collect","key");
					String coll = null;
					for (int i = 0; i < collections.size(); i++) {
						coll = (String) collections.get(i);
						setValue(coll);
						try {
							String label = null;
							if(!getIsVocabTermAvailable())
								label = ((SetInfo)rm.getConfiguredSetInfos().get( coll )).getName();
							else
								label = getVocabTerm().getLabel();
														
							// Construct a String used only for sorting by name:
							temp.add(label.toLowerCase() + "24separator42" + coll);
						} catch (Throwable t) {
							prtlnErr("Warning: Unable to get vocab key for collection '" + coll + "'. Reason: " + t);
						}
					}
					Collections.sort(temp);
					collections = new ArrayList(temp.size());
					for (int i = 0; i < temp.size(); i++) {
						String key = ((String) temp.get(i)).replaceFirst(".*24separator42", "");
						collections.add(key);
					}
				}
			}
		} catch (Throwable t) {
			prtlnErr("Error: getCollections() Message: " + t);
			if(t instanceof NullPointerException)
				t.printStackTrace();
		}
		return collections;
	}


	/**
	 *  Sets the collectionLabels attribute of the DDSAdminQueryForm object
	 *
	 * @return    The formats value
	 */
	/* public void setCollectionLabels(List collectionLabels) {
		prtln("setCollectionLabels() size ");
		this.collectionLabels = collectionLabels;
	} */
	/**
	 *  Gets the collectionLabels attribute of the DDSAdminQueryForm object
	 *
	 * @return    The collectionLabels value
	 */
	/* public List getCollectionLabels() {
		if(collectionLabels == null){
			List cols = getCollections();
			if(cols != null){
				collectionLabels = new
			}
		}
		return collectionLabels;
	} */
	/**
	 *  Gets all possible metadata formats that may be disiminated by the RepositoryManager. This includes
	 *  formats that are available via the XMLConversionService.
	 *
	 * @return    The formats value
	 */
	public List getFormats() {
		return formats;
	}



	/**
	 *  Sets the formats attribute of the DDSAdminQueryForm object
	 *
	 * @param  formats  The new formats value
	 */
	public void setFormats(List formats) {
		//prtln("setFormats() size ");
		this.formats = formats;
	}



	/**
	 *  Gets all formats that exist natively in the index.
	 *
	 * @return    The indexedFormats value
	 */
	public List getIndexedFormats() {
		RepositoryManager rm =
			(RepositoryManager) servlet.getServletContext().getAttribute("repositoryManager");
		return rm.getIndexedFormats();
	}



	/**
	 *  Gets all formats that exist natively in the index.
	 *
	 * @param  formats  The new indexedFormats value
	 */
	public void setIndexedFormats(List formats) {
		indexedFormats = formats;
	}



	/**
	 *  Gets all formats that exist natively in the index.
	 *
	 * @return    The indexedFormatsLabels value
	 */
	public List getIndexedFormatsLabels() {
		return indexedFormatsLabels;
	}



	/**
	 *  Gets all formats that exist natively in the index.
	 *
	 * @param  formatsLabels  The new indexedFormatsLabels value
	 */
	public void setIndexedFormatsLabels(List formatsLabels) {
		indexedFormatsLabels = formatsLabels;
	}


	/**
	 *  Gets the selected indexed formats.
	 *
	 * @return    The sfmts value
	 */
	public String[] getSifmts() {
		/* if (selectedIndexedFormats == null) {
			selectedIndexedFormats = new String[1];
			if (getIndexedFormats() == null || getFormats().size() == 0) {
				selectedIndexedFormats[0] = "";
			}
			else {
				selectedIndexedFormats[0] = (String) getIndexedFormats().get(0);
			}
		} */
		return selectedIndexedFormats;
	}



	/**
	 *  Sets the selected indexed formats.
	 *
	 * @param  selectedIndexedFormats  The new sifmts value
	 */
	public void setSifmts(String[] selectedIndexedFormats) {
		this.selectedIndexedFormats = selectedIndexedFormats;
	}



	/**
	 *  Gets the formatLabels attribute of the DDSAdminQueryForm object
	 *
	 * @return    The formatLabels value
	 */
	public List getFormatLabels() {
		return getFormats();
	}


	/**
	 *  Gets the selected formats. These include formats that are available via the XMLConversionService.
	 *
	 * @return    The sfmts value
	 */
	public String[] getSfmts() {
		return selectedFormats;
	}



	/**
	 *  Sets the selected formats. These include formats that are available via the XMLConversionService.
	 *
	 * @param  selectedFormats  The new sfmts value
	 */
	public void setSfmts(String[] selectedFormats) {
		this.selectedFormats = selectedFormats;
	}



	/**
	 *  Gets the collection that has been selected by the user in the UI via a Select tag. For example '0dcc'.
	 *  For use with a Struts select tag that does not have multiple selection enabled.<p>
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
			if (getCollections() == null || getCollections().size() == 0) {
				return "";
			}
			return (String) getCollections().get(0);
		}
		return selectedCollection;
	}


	/**
	 *  Sets the collection that has been selected by the user in the UI via a Select tag. For example '0dcc'.
	 *  For use with a Struts select tag that does not have multiple selection enabled.
	 *
	 * @param  selectedCollection  The new sc value
	 */
	public void setSc(String selectedCollection) {
		this.selectedCollection = selectedCollection;
	}


	/**
	 *  Gets the collections that have been selected by the user in the UI. For example '0dcc' '0comet'. For use
	 *  with a Struts select tag that has multiple selection enabled.<p>
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
			if (getCollections() == null || getCollections().size() == 0) {
				selectedCollections[0] = "";
			}
			else {
				selectedCollections[0] = (String) getCollections().get(0);
			}
		}
		return selectedCollections;
	}

	/**
	 *  Gets the collections that have been selected by the user in the UI. For example '06' '006'. For use
	 *  with a Struts select tag that has multiple selection enabled.<p>
	 *
	 *
	 *
	 * @return    The selected collection keys
	 */
	public String[] getKy() {
		return ky;
	}
	
	
	String[] selectedIdMapperErrors = null;


	/**
	 *  Gets the selectedIdMapperErrors attribute of the DDSAdminQueryForm object
	 *
	 * @return    The selectedIdMapperErrors value
	 */
	public String[] getSelectedIdMapperErrors() {
		if (selectedIdMapperErrors == null)
			selectedIdMapperErrors = new String[]{DDSAdminQueryAction.SELECT_NONE};
		return selectedIdMapperErrors;
	}


	String[] selectedAccessionStatuses = null;


	/**
	 *  DESCRIPTION
	 *
	 * @return    DESCRIPTION
	 */
	public String[] getselectedAccessionStatuses() {
		if (selectedAccessionStatuses == null)
			selectedAccessionStatuses = new String[]{DDSAdminQueryAction.SELECT_ALL};
		return selectedAccessionStatuses;
	}


	/**
	 *  DESCRIPTION
	 *
	 * @param  selectedAccessionStatuses  DESCRIPTION
	 */
	public void setselectedAccessionStatuses(String[] selectedAccessionStatuses) {
		this.selectedAccessionStatuses = selectedAccessionStatuses;
	}


	/**
	 *  Sets the selectedIdMapperErrors attribute of the DDSAdminQueryForm object
	 *
	 * @param  selectedIdMapperErrors  The new selectedIdMapperErrors value
	 */
	public void setSelectedIdMapperErrors(String[] selectedIdMapperErrors) {
		this.selectedIdMapperErrors = selectedIdMapperErrors;
	}


	/**
	 *  DESCRIPTION
	 *
	 * @param  mapping  DESCRIPTION
	 * @param  request  DESCRIPTION
	 */
	public void reset(ActionMapping mapping, javax.servlet.http.HttpServletRequest request) {
		//selectedIdMapperErrors = null;
	}


	/**
	 *  Sets the collection that has been selected by the user in the UI. For example '0dcc' '0comet'. For use
	 *  with a Struts select tag that has multiple selection enabled.
	 *
	 * @param  selectedCollections  The new sc value
	 */
	public void setScs(String[] selectedCollections) {
		this.selectedCollections = selectedCollections;
	}

	/**
	 *  Sets the collection that has been selected by the user in the UI. For example '06' '008'. For use
	 *  with a Struts select tag that has multiple selection enabled.
	 *
	 * @param  ky  The new sc value
	 */
	public void setKy(String[] ky) {
		this.ky = ky;
	}	
	

	/**
	 *  Gets the selected collection(s) parameter to be inserted in the URL that gets the next set of results.
	 *
	 * @return    The scparams value.
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
	 *  Gets the search results returned by the {@link org.dlese.dpc.index.SimpleLuceneIndex}.
	 *
	 * @return    The results value
	 */
	public ResultDocList getResults() {
		return resultDocs;
	}



	/**
	 *  Sets the search results returned by the {@link org.dlese.dpc.index.SimpleLuceneIndex}.
	 *
	 * @param  results  The new results value.
	 */
	public void setResults(ResultDocList results) {
		resultDocs = results;
	}


	/**
	 *  Sets the result attribute of the DDSAdminQueryForm object
	 *
	 * @param  resultDoc  The new result value
	 */
	public void setResult(ResultDoc resultDoc) {
		this.resultDoc = resultDoc;
	}


	/**
	 *  Gets the result attribute of the DDSAdminQueryForm object
	 *
	 * @return    The result value
	 */
	public ResultDoc getResult() {
		return resultDoc;
	}

	/**
	 *  Gets the DocumentMap for the search result.
	 *
	 * @return    The DocMap value
	 */
	public DocumentMap getDocMap() {
		return resultDoc.getDocMap();
	}	

	/**
	 *  Gets the LazyDocumentMap for the search result.
	 *
	 * @return    The LazyDocumentMap value
	 */
	public LazyDocumentMap getLazyDocMap() {
		return resultDoc.getLazyDocMap();
	}	
	
	/**
	 *  Gets the result attribute of the DDSAdminQueryForm object
	 *
	 * @return    The result value
	 */
	public DocReader getDocReader() {
		if (resultDoc == null)
			return null;
		try {
			return resultDoc.getDocReader();
		} catch (Throwable t) {
			return null;
		}
	}


	/**
	 *  Gets the IDMapper Mmd record for this ADN resource, otherwise null. If the given record is not in ADN,
	 *  null will be returned.
	 *
	 * @return    The Mmd Record, or null
	 */
	public MmdRec getMmdRec() {
		try {
			XMLDocReader dr = (XMLDocReader) getDocReader();
			RecordDataService rdc = getRepositoryManager().getRecordDataService();
			Query idMapperQueryObject = rdc.getIdMapperQueryObject();
			MmdRec rec = rdc.getMmdRec(dr.getId(), dr.getCollection(), idMapperQueryObject);
			rdc.closeIdMapperQueryObject(idMapperQueryObject);
			return rec;
		} catch (Throwable t) {
			prtln("Error: Could not get IDMapper data: " + t);
			return null;
		}
	}


	/**
	 *  Gets the IDMapper Mmd records for dupulicate resources in other collections, or null.
	 *
	 * @return    The duplicate Mmd Records, or null
	 */
	public MmdRec[] getMmdDupesInOtherCollections() {
		try {
			XMLDocReader dr = (XMLDocReader) getDocReader();
			RecordDataService rdc = getRepositoryManager().getRecordDataService();
			Query idMapperQueryObject = rdc.getIdMapperQueryObject();
			MmdRec[] recs = rdc.getAssociatedMMDRecs(RecordDataService.QUERY_OTHER, dr.getId(), dr.getCollection(), idMapperQueryObject);
			rdc.closeIdMapperQueryObject(idMapperQueryObject);
			return recs;
		} catch (Throwable t) {
			prtln("Error: Could not get IDMapper data: " + t);
			return null;
		}
	}


	/**
	 *  Gets the IDMapper Mmd records for dupulicate resources in the same as this resource collection, or null.
	 *
	 * @return    The duplicate Mmd Records, or null
	 */
	public MmdRec[] getMmdDupesInSameCollection() {
		try {
			XMLDocReader dr = (XMLDocReader) getDocReader();
			RecordDataService rdc = getRepositoryManager().getRecordDataService();
			Query idMapperQueryObject = rdc.getIdMapperQueryObject();
			MmdRec[] recs = rdc.getAssociatedMMDRecs(RecordDataService.QUERY_SAME, dr.getId(), dr.getCollection(), idMapperQueryObject);
			rdc.closeIdMapperQueryObject(idMapperQueryObject);
			return recs;
		} catch (Throwable t) {
			prtln("Error: Could not get IDMapper data: " + t);
			return null;
		}
	}


	/**
	 *  Gets the RepositoryManager.
	 *
	 * @return    The RepositoryManager
	 */
	private RepositoryManager getRepositoryManager() {
		return (RepositoryManager) this.getServlet().getServletContext().getAttribute("repositoryManager");
	}


	/**
	 *  Sets the metadata attribute of the DDSAdminQueryForm object
	 *
	 * @param  metadata  The new metadata value
	 */
	public void setMetadata(String metadata) {
		this.metadata = metadata;
	}


	/**
	 *  Gets the metadata attribute of the DDSAdminQueryForm object
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
	 *  Gets the numResults attribute of the DDSAdminQueryForm object
	 *
	 * @return    The numResults value
	 */
	public String getNumResults() {
		if (resultDocs != null) {
			return Integer.toString(resultDocs.size());
		}
		else {
			return "0";
		}
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
			prtln("Error: getQe(): " + e);
			return "";
		}
	}


	/**
	 *  Sets the q attribute of the DDSAdminQueryForm object
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
	 * @author     John Weatherley
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
	 *  Sets the reportTitle attribute of the DDSAdminQueryForm object
	 *
	 * @param  reportTitle  The new reportTitle value
	 */
	public void setReportTitle(String reportTitle) {
		this.reportTitle = reportTitle;
	}


	/**
	 *  Gets the reportTitle attribute of the DDSAdminQueryForm object
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
		if (resultDocs == null || start < 0) {
			return null;
		}
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
	 *  Gets the s attribute of the DDSAdminQueryForm object
	 *
	 * @return    The s value
	 */
	public int getS() {
		return start;
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
		if (resultDocs == null || start < 0) {
			return null;
		}

		int e = start + numPagingRecords;
		int n = resultDocs.size();
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
		if (resultDocs == null || start <= 0) {
			return null;
		}

		int p = start - numPagingRecords;
		int prev = p > 0 ? p : 0;

		return "q=" + getQe() +
			"&s=" + prev +
			getNonPaigingParams();
	}


	/**
	 *  Sets the request attribute of the DDSAdminQueryForm object.
	 *
	 * @param  request  The new request value
	 */
	public void setRequest(HttpServletRequest request) {
		this.request = request;
	}


	private String nonPaigingParams = null;


	/**
	 *  Sets the nonPaigingParams attribute of the DDSAdminQueryForm object
	 *
	 * @param  nonPaigingParams  The new nonPaigingParams value
	 */
	public void setNonPaigingParams(String nonPaigingParams) {
		this.nonPaigingParams = nonPaigingParams;
	}


	/**
	 *  Gets all the parameters that existed in the request other than those used for paiging.
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
	 *  Output a line of text to standard err, with datestamp.
	 *
	 * @param  s  The String that will be output.
	 */
	private final void prtlnErr(String s) {
		System.err.println(getDateStamp() + " " + s);
	}


	/**
	 *  Return a string for the current time and date, sutiable for display in log files and output to standout:
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



