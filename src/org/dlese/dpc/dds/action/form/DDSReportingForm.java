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

import org.dlese.dpc.propertiesmgr.*;
import org.dlese.dpc.webapps.tools.*;
import org.dlese.dpc.index.*;
import org.dlese.dpc.oai.*;
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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.ServletContext;
import java.util.*;
import java.io.*;
import java.text.*;
import java.net.URLEncoder;

/**
 *  A bean that holds data for DDS reporting.
 *
 * @author     John Weatherley
 * @see        org.dlese.dpc.dds.action.DDSReportingAction
 */
public class DDSReportingForm extends ActionForm implements Serializable {

	private static boolean debug = true;
	private String errorMsg = null;
	private ResultDocList results = null;
	private SimpleLuceneIndex index = null;
	private String termCountFields = "";
	private String indexToUse = "ddsIndex";
	private String id = null;
	private String collection = null;

	// Bean properties:

	/**  Constructor for the RepositoryForm object */
	public DDSReportingForm() { }


	/**
	 *  Gets the index attribute of the DDSReportingForm object
	 *
	 * @return    The index value
	 */
	public SimpleLuceneIndex getIndex() {
		return index;
		/* try{
			RepositoryManager rm =
				(RepositoryManager) getServlet().getServletContext().getAttribute("repositoryManager");
			return rm.getIndex();
		}catch(Throwable e){
			prtlnErr("Error getting index object");
			return null;
		} */
	}


	/**
	 *  Sets the index attribute of the DDSReportingForm object
	 *
	 * @param  newIndex  The new index value
	 */
	public void setIndex(SimpleLuceneIndex newIndex) {
		index = newIndex;
	}


	/**
	 *  Sets the id attribute of the DDSReportingForm object
	 *
	 * @param  id  The new id value
	 */
	public void setId(String id) {
		this.id = id;
	}


	/**
	 *  Gets the id attribute of the DDSReportingForm object
	 *
	 * @return    The id value
	 */
	public String getId() {
		return id;
	}


	/**
	 *  Sets the collection attribute of the DDSReportingForm object
	 *
	 * @param  collection  The new collection value
	 */
	public void setCollection(String collection) {
		this.collection = collection;
	}


	/**
	 *  Gets the collection attribute of the DDSReportingForm object
	 *
	 * @return    The collection value
	 */
	public String getCollection() {
		return collection;
	}


	/**
	 *  Gets the mmdRecsDupsSame attribute of the DDSReportingForm object
	 *
	 * @return    The mmdRecsDupsSame value
	 */
	public MmdRec[] getMmdRecsDupsSame() {
		if (id == null || collection == null || id.length() == 0 || collection.length() == 0)
			return null;

		RepositoryManager rm =
			(RepositoryManager) getServlet().getServletContext().getAttribute("repositoryManager");
		RecordDataService rds = rm.getRecordDataService();
		Query qo = rds.getIdMapperQueryObject();
		MmdRec[] myRecs = rds.getAssociatedMMDRecs(Query.QUERY_SAME, id, collection, qo);
		rds.closeIdMapperQueryObject(qo);
		return myRecs;
	}


	/**
	 *  Gets the myMmdRec attribute of the DDSReportingForm object
	 *
	 * @return    The myMmdRec value
	 */
	public MmdRec getMyMmdRec() {
		if (id == null || collection == null || id.length() == 0 || collection.length() == 0)
			return null;

		RepositoryManager rm =
			(RepositoryManager) getServlet().getServletContext().getAttribute("repositoryManager");
		RecordDataService rds = rm.getRecordDataService();
		Query qo = rds.getIdMapperQueryObject();
		MmdRec myRec = rds.getMmdRec(id, collection, qo);
		rds.closeIdMapperQueryObject(qo);
		return myRec;
	}


	/**
	 *  Gets the mmdRecsDupsOther attribute of the DDSReportingForm object
	 *
	 * @return    The mmdRecsDupsOther value
	 */
	public MmdRec[] getMmdRecsDupsOther() {
		if (id == null || collection == null || id.length() == 0 || collection.length() == 0)
			return null;

		RepositoryManager rm =
			(RepositoryManager) getServlet().getServletContext().getAttribute("repositoryManager");
		RecordDataService rds = rm.getRecordDataService();
		Query qo = rds.getIdMapperQueryObject();
		MmdRec[] myRecs = rds.getAssociatedMMDRecs(Query.QUERY_OTHER, id, collection, qo);
		rds.closeIdMapperQueryObject(qo);
		return myRecs;
	}


	private String lastTermCountFields = "";
	private long lastIndexMod = -1;
	private Map termCountMap = null;


	/**
	 *  Sets the fields to generate term counts for, separated by spaces.
	 *
	 * @param  s  The new calculateCountsForFields value
	 */
	public void setCalculateCountsForFields(String s) {
		if(s == null || s.trim().length() == 0)
			setCalculateCountsForFieldsAsArray(new String [] {});
		setCalculateCountsForFieldsAsArray(s.split("\\s+"));
	}

	
	/**
	 *  Sets the fields to generate term counts for, or null for none.
	 *
	 * @param  fields  The new calculateCountsForFields value
	 */
	public void setCalculateCountsForFieldsAsArray(String [] fields) {
		if(fields == null || fields.length == 0) {
			termCountMap = null;
			return;
		}
		
		for(int i = 0; i < fields.length; i++) {
			if(i != 0)
				termCountFields += " ";	
			termCountFields += fields[i];
		}

		if (!termCountFields.equals(lastTermCountFields) || getIndex().getLastModifiedCount() != lastIndexMod) {
			prtln("Generating term count map for fields: " + termCountFields);
			termCountMap = getIndex().getTermAndDocCounts(fields);
			lastIndexMod = getIndex().getLastModifiedCount();
			lastTermCountFields = termCountFields;
			prtln("Done generating term count map for fields: " + termCountFields);
		}
		else
			prtln("Using cached term count map for fields: " + termCountFields);
	}	

	/**
	 *  Gets the termCountFields attribute of the DDSReportingForm object
	 *
	 * @return    The termCountFields value
	 */
	public String getTermCountFields() {
		return termCountFields;
	}


	/**
	 *  Gets the numTerms attribute of the DDSReportingForm object
	 *
	 * @return    The numTerms value
	 */
	public int getNumTerms() {
		if (termCountMap == null)
			return 0;
		return termCountMap.size();
	}


	private String[] stemWords = null;
	private String[] originalWords = null;


	/**
	 *  Sets the stemWords attribute of the DDSReportingForm object
	 *
	 * @param  s  The new stemWords value
	 */
	public void setStemWords(String s) {
		// Split on non-word chars
		originalWords = s.split("[^a-zA-Z0-9]+");
		stemWords = Stemmer.stemWordsInString(s).split(" ");
	}


	/**
	 *  Gets the originalWords attribute of the DDSReportingForm object
	 *
	 * @return    The originalWords value
	 */
	public String[] getOriginalWords() {
		return originalWords;
	}


	/**
	 *  Gets the stems attribute of the DDSReportingForm object
	 *
	 * @return    The stems value
	 */
	public String[] getStems() {
		return stemWords;
	}


	/**
	 *  Gets the termCountMap attribute of the DDSReportingForm object
	 *
	 * @return    The termCountMap value
	 */
	public Map getTermCountMap() {
		return termCountMap;
	}


	/**
	 *  Sets the indexToUse attribute of the DDSReportingForm object
	 *
	 * @param  value  The new indexToUse value
	 */
	public void setIndexToUse(String value) {
		indexToUse = value;
	}


	/**
	 *  Gets the indexToUse attribute of the DDSReportingForm object
	 *
	 * @return    The indexToUse value
	 */
	public String getIndexToUse() {
		return indexToUse;
	}


	/**
	 *  Sets the errorMsg attribute of the DDSReportingForm object
	 *
	 * @param  errorMsg  The new errorMsg value
	 */
	public void setErrorMsg(String errorMsg) {
		this.errorMsg = errorMsg;
	}



	/**
	 *  Gets the errorMsg attribute of the DDSReportingForm object
	 *
	 * @return    The errorMsg value
	 */
	public String getErrorMsg() {
		return errorMsg;
	}


	/**
	 *  Gets the results attribute of the DDSReportingForm object
	 *
	 * @return    The results value
	 */
	public ResultDocList getResults() {
		return results;
	}


	/**
	 *  Sets the results attribute of the DDSReportingForm object
	 *
	 * @param  results  The new results value
	 */
	public void setResults(ResultDocList results) {
		this.results = results;
	}


	/**
	 *  Gets the numResults attribute of the DDSReportingForm object
	 *
	 * @return    The numResults value
	 */
	public String getNumResults() {
		if (results == null)
			return "0";
		return Integer.toString(results.size());
	}


	/**
	 *  Gets a list of encoded IDs for index field 'id' that show being in more than one record in the index.
	 *
	 * @return    A list of encoded IDs
	 */
	public List getIdsInMultipleRecords() {
		ArrayList dupIds = new ArrayList();

		try {
			// Set up a term count map for the id field
			Map termCountMap = getIndex().getTermAndDocCounts(new String[]{"id"});

			String[] ids = (String[]) termCountMap.keySet().toArray(new String[]{});
			for (int i = 0; i < ids.length; i++) {
				TermDocCount docCount = (TermDocCount) termCountMap.get(ids[i]);
				if (docCount.getDocCount() > 1)
					dupIds.add(ids[i]);
			}
		} catch (Throwable t) {
			prtlnErr("Error in getRecordsWithMultipleIds(): " + t);
		}

		return dupIds;
	}


	//================================================================


	/**
	 *  Return a string for the current time and date, sutiable for display in log files and output to standout:
	 *
	 * @return    The dateStamp value
	 */
	protected final static String getDs() {
		return
			new SimpleDateFormat("MMM d, yyyy h:mm:ss a zzz").format(new Date());
	}


	/**
	 *  Output a line of text to error out, with datestamp.
	 *
	 * @param  s  The text that will be output to error out.
	 */
	protected final void prtlnErr(String s) {
		System.err.println(getDs() + " " + s);
	}



	/**
	 *  Output a line of text to standard out, with datestamp, if debug is set to true.
	 *
	 * @param  s  The String that will be output.
	 */
	protected final void prtln(String s) {
		if (debug)
			System.out.println(getDs() + " " + s);
	}


	/**
	 *  Sets the debug attribute
	 *
	 * @param  isDebugOuput  The new debug value
	 */
	public static void setDebug(boolean isDebugOuput) {
		debug = isDebugOuput;
	}
}



