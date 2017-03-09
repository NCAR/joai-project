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
import org.dlese.dpc.schemedit.threadedservices.*;
import org.dlese.dpc.repository.*;
import org.dlese.dpc.index.ResultDoc;
import org.dlese.dpc.index.reader.*;
import org.dlese.dpc.xml.schema.*;
import org.dlese.dpc.xml.*;
import org.dlese.dpc.serviceclients.remotesearch.RemoteResultDoc;
import org.dlese.dpc.serviceclients.remotesearch.reader.ADNItemDocReader;

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
 *  ActionForm bean for handling requests to support Schemedit. Most methods
 *  acesss the {@link DocMap} attribute, which wraps the XML Document that is
 *  being edited.
 *
 *@author    ostwald $Id: RecordOperationsForm.java,v 1.8 2005/08/31 08:09:59
 *      ostwald Exp $
 */
public class RecordOperationsForm extends ActionForm {

	private boolean debug = true;
	private HttpServletRequest request;

	private String recId = null;
	private DcsSetInfo dcsSetInfo = null;
	private String collection;
	private XMLDocReader docReader = null;
	private String editRec = "";
	// used to forward after move
	private ResultDoc resultDoc = null;
	private List sets = null;


	/**
	 *  Constructor
	 */
	public RecordOperationsForm() { }



	/**
	 *  Gets the resultDoc attribute of the RecordOperationsForm object
	 *
	 *@return    The resultDoc value
	 */
	public ResultDoc getResultDoc() {
		return resultDoc;
	}


	/**
	 *  Sets the resultDoc attribute of the RecordOperationsForm object
	 *
	 *@param  resultDoc  The new resultDoc value
	 */
	public void setResultDoc(ResultDoc resultDoc) {
		this.resultDoc = resultDoc;
	}


	/**
	 *  Gets the docReader attribute of the RecordOperationsForm object
	 *
	 *@return    The docReader value
	 */
	public XMLDocReader getDocReader() {
		if (resultDoc != null) {
			return (XMLDocReader) resultDoc.getDocReader();
		}
		return null;
	}


	/**
	 *  Sets the docReader attribute of the RecordOperationsForm object
	 *
	 *@param  docReader  The new docReader value
	 */
	public void setDocReader(XMLDocReader docReader) {
		this.docReader = docReader;
	}

	/**
	 *  editRec parameter is used by handleMoveRecord to specify whether control is
	 *  forwarded back to editor. Seems there should be an easier way ...
	 *
	 *@return    The editRec value
	 */
	public String getEditRec() {
		return editRec;
	}


	/**
	 *  Sets the editRec attribute of the RecordOperationsForm object
	 *
	 *@param  s  The new editRec value
	 */
	public void setEditRec(String s) {
		editRec = s;
	}

	
	/**
	 *  Gets the sets attribute of the CollectionServicesForm object
	 *
	 *@return    The sets value
	 */
	public List getSets() {
		return sets;
	}

	/**
	 *  Sets the sets attribute of the CollectionServicesForm object
	 *
	 *@param  sets  The new sets value
	 */
	public void setSets(List sets) {
		this.sets = sets;
	}

	
	/**
	 *  Gets the dcsSetInfo attribute of the RecordOperationsForm object
	 *
	 *@return    The dcsSetInfo value
	 */
	public DcsSetInfo getDcsSetInfo() {
		return dcsSetInfo;
	}


	/**
	 *  Sets the dcsSetInfo attribute of the RecordOperationsForm object
	 *
	 *@param  info  The new dcsSetInfo value
	 */
	public void setDcsSetInfo(DcsSetInfo info) {
		dcsSetInfo = info;
	}


	/**
	 *  Gets the collection attribute of the RecordOperationsForm object
	 *
	 *@return    The collection value
	 */
	public String getCollection() {
		return collection;
	}


	/**
	 *  Sets the collection attribute of the RecordOperationsForm object
	 *
	 *@param  collection  The new collection value
	 */
	public void setCollection(String collection) {
		this.collection = collection;
	}



	/**
	 *  Gets the recId attribute of the RecordOperationsForm object
	 *
	 *@return    The recId value
	 */
	public String getRecId() {
		return recId;
	}


	/**
	 *  Sets the recId attribute of the RecordOperationsForm object
	 *
	 *@param  id  The new recId value
	 */
	public void setRecId(String id) {
		recId = id;
	}

	/**
	 *  Output a line of text to standard out, with datestamp, if debug is set to
	 *  true.
	 *
	 *@param  s  The String that will be output.
	 */
	protected final void prtln(String s) {
		if (debug) {
			System.out.println("RecordOperationsForm: " + s);
		}
	}

}

