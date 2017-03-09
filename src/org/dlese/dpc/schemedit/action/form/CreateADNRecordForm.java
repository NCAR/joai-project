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

import org.dlese.dpc.schemedit.*;
import org.dlese.dpc.schemedit.dcs.*;
import org.dlese.dpc.xml.schema.*;
import org.dlese.dpc.xml.*;
import org.dlese.dpc.index.*;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Node;

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
import java.net.*;
import java.util.regex.*;

/**
 *  ActionForm bean for handling requests to create ADN Records
 *
 *@author    ostwald
 */
public class CreateADNRecordForm extends ActionForm {

	private boolean debug = true;
	private HttpServletRequest request;
	
	private DcsDataRecord dcsDataRecord = null;
	private String editRecordLink = "";
	
	private String recId = null;
	
	// input params
	private String title = null;
	private String description = "description goes here";
	private String primaryUrl = null;
	private String validatedUrl = null;
	
	private String collection = null;
	private String collectionName = null;
	
	private List dups = null;
	private List sims = null;
	private ResultDoc [] similarUrlRecs = null;
	private ResultDoc [] duplicateUrlRecs = null;

	/**
	 *  Constructor
	 */
	public CreateADNRecordForm() {
	}
	
	public void clear () {
		recId = null;
		title = null;
		primaryUrl = null;
		collection = null;
		collectionName = null;
		validatedUrl = null;
		bogusUrl = null;
		sims = null;
		dups = null;
	}
	
	/**
	 *  Gets the dcsDataRecord attribute of the ADNRecordForm object
	 *
	 *@return    The dcsDataRecord value
	 */
	public DcsDataRecord getDcsDataRecord() {
		return dcsDataRecord;
	}


	/**
	 *  Sets the dcsDataRecord attribute of the ADNRecordForm object
	 *
	 *@param  dataRec  The new dcsDataRecord value
	 */
	public void setDcsDataRecord(DcsDataRecord dataRec) {
		dcsDataRecord = dataRec;
	}

	public ResultDoc [] getSimilarUrlRecs () {
		return similarUrlRecs;
	}
	
	public void setSimilarUrlRecs (ResultDoc [] results) {
		similarUrlRecs = results;
	}
	
	public ResultDoc [] getDuplicateUrlRecs () {
		return duplicateUrlRecs;
	}
	
	public void setDuplicateUrlRecs (ResultDoc [] results) {
		duplicateUrlRecs = results;
	}
	
		public List getDups() {
		if (dups == null)
			dups = new ArrayList();
		return dups;
	}

	public void setDups (List simDupList) {
		dups = simDupList;
	}
	
	public List getSims() {
		if (sims == null)
			sims = new ArrayList();
		return sims;
	}

	public void setSims (List simDupList) {
		sims = simDupList;
	}

	
		/**
	 *  Gets the editRecordLink attribute of the ADNRecordForm object
	 *
	 *@return    The editRecordLink value
	 */
	public String getEditRecordLink() {
		return editRecordLink;
	}


	/**
	 *  Sets the editRecordLink attribute of the ADNRecordForm object
	 *
	 *@param  s  The new editRecordLink value
	 */
	public void setEditRecordLink(String s) {
		editRecordLink = s;
	}


	
	/**
	 *  Gets the recId attribute of the ADNRecordForm object
	 *
	 *@return    The recId value
	 */
	public String getRecId() {
		return recId;
	}


	/**
	 *  Sets the recId attribute of the ADNRecordForm object
	 *
	 *@param  id  The new recId value
	 */
	public void setRecId(String id) {
		recId = id;
	}

	public String getXmlFormat() {
		return "adn";
	}
	
	public String getTitle() {
		return title;
	}
	
	public void setTitle(String title) {
		this.title = title;
	}

	public String getDescription() {
		return description;
	}
	
	public void setDescription(String description) {
		this.description = description;
	}

	public String getPrimaryUrl() {
		return primaryUrl;
	}

	public void setPrimaryUrl(String primaryUrl) {
		this.primaryUrl = primaryUrl;
	}
	
	public String getValidatedUrl() {
		return validatedUrl;
	}

	public void setValidatedUrl(String validatedUrl) {
		this.validatedUrl = validatedUrl;
	}
	
	private String bogusUrl = null;
	
	public void setBogusUrl (String url) {
		this.bogusUrl = url;
	}
	
	public String getBogusUrl () {
		return this.bogusUrl;
	}
	
	public String getCollection() {
		return collection;
	}
	
	public void setCollection(String collection) {
		this.collection = collection;
	}

	public String getCollectionName() {
		return collectionName;
	}
	
	public void setCollectionName(String collectionName) {
		this.collectionName = collectionName;
	}
	
	/**
	 *  Sets the request attribute of the DCSBrowseForm object.
	 *
	 * @param  request  The new request value
	 */
 	public void setRequest(HttpServletRequest request) {
		this.request = request;
	}


	/**
	 *  Output a line of text to standard out, with datestamp, if debug is set to
	 *  true.
	 *
	 *@param  s  The String that will be output.
	 */
	protected final void prtln(String s) {
		if (debug) {
			System.out.println("CreateADNRecordForm: " + s);
		}
	}

}

