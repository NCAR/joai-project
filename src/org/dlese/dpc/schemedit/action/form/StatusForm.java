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
import org.dlese.dpc.schemedit.config.StatusFlags;
import org.dlese.dpc.schemedit.security.access.ActionPath;
import org.dlese.dpc.schemedit.security.access.Roles;
import org.dlese.dpc.schemedit.dcs.*;
import org.dlese.dpc.repository.*;
import org.dlese.dpc.xml.schema.*;
import org.dlese.dpc.xml.*;

/*
import org.dlese.dpc.serviceclients.remotesearch.RemoteResultDoc;
import org.dlese.dpc.serviceclients.remotesearch.reader.ADNItemDocReader;
import org.dlese.dpc.vocab.*;
*/

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
 *  ActionForm bean for editing the status information associated with each
 *  metadata record
 *
 *@author    ostwald 
 
 */
public class StatusForm extends ActionForm {

	private boolean debug = false;
	private HttpServletRequest request;

	private DcsDataRecord dcsDataRecord = null;
	private String referer = null;
	private String command = null;
	private String collection = null;
	private String collectionName = null;

	// here are the attributes needed by edit-status
	private String recId = null;
	// private String lastEditor = null;
	private String status = null;
	private String statusNote = null;
	private List statusFlags = null;
	private String entryKey = null;
	private String hash = "";
	private String ndrHandle = "";


	/* private SetInfo setInfo = null; */
	// input params
	/**
	 *  Constructor
	 */
	public StatusForm() { }


	/**
	 *  Description of the Method
	 */
	public void clear() {
		entryKey = null;
		hash = "";
		statusNote = "";
	}

	public String getCommand () {
		return command;
	}
	
	public void setCommand (String cmd) {
		command = cmd;
	}
	
	/**
	 *  Gets the recId attribute of the StatusForm object
	 *
	 *@return    The recId value
	 */
	public String getRecId() {
		return recId;
	}


	/**
	 *  Sets the recId attribute of the StatusForm object
	 *
	 *@param  id  The new recId value
	 */
	public void setRecId(String id) {
		recId = id;
	}

	public String getNdrHandle () {
		return ndrHandle;
	}
	
	public void setNdrHandle (String handle) {
		this.ndrHandle = handle;
	}
	
	public String getHash () {
		return hash;
	}
	
	public void setHash (String h) {
		hash = h;
	}
	
	public String getEntryKey() {
		return entryKey;
	}
	
	public void setEntryKey (String s) {
		entryKey = s;
	}

	/**
	 *  Gets the dcsDataRecord attribute of the StatusForm object
	 *
	 *@return    The dcsDataRecord value
	 */
	public DcsDataRecord getDcsDataRecord() {
		return dcsDataRecord;
	}


	/**
	 *  Sets the dcsDataRecord attribute of the StatusForm object
	 *
	 *@param  dataRec  The new dcsDataRecord value
	 */
	public void setDcsDataRecord(DcsDataRecord dataRec) {
		dcsDataRecord = dataRec;
	}


	/**
	 *  Gets the dcsLastEditor attribute of the StatusForm object
	 *
	 *@return    The dcsLastEditor value
	 */
/* 	public String getLastEditor() {
		return lastEditor;
	}
 */

	/**
	 *  Sets the dcsLastEditor attribute of the StatusForm object
	 *
	 *@param  lastEditor  The new dcsLastEditor value
	 */
/* 	public void setLastEditor(String lastEditor) {
		this.lastEditor = lastEditor;
	} */


	/**
	 *  Gets the dcsStatus attribute of the StatusForm object
	 *
	 *@return    The dcsStatus value
	 */
	public String getStatus() {
		return status;
	}


	/**
	 *  Sets the status attribute of the StatusForm object
	 *
	 *@param  status  The new status value
	 */
	public void setStatus(String status) {
		this.status = status;
	}


	/**
	 *  Gets the dcsStatusFlags attribute of the StatusForm object
	 *
	 *@return    The dcsStatusFlags value
	 */
	public List getStatusFlags() {
		return statusFlags;
	}

	public void setStatusFlags (List flags) {
		statusFlags = flags;
	}

	/**
	 *  Gets the dcsStatusNote attribute of the StatusForm object
	 *
	 *@return    The dcsStatusNote value
	 */
	public String getStatusNote() {
		return statusNote;
	}


	/**
	 *  Sets the dcsStatusNote attribute of the StatusForm object
	 *
	 *@param  statusNote  The new dcsStatusNote value
	 */
	public void setStatusNote(String statusNote) {
		this.statusNote = statusNote;
	}


	/**
	 *  Sets the referer attribute of the StatusForm object
	 *
	 *@param  referer  The new referer value
	 */
	public void setReferer(String referer) {
		this.referer = referer;
	}


	/**
	 *  Gets the referer attribute of the StatusForm object
	 *
	 *@return    The referer value
	 */
	public String getReferer() {
		return referer;
	}


	/**
	 *  Sets the collection attribute of the StatusForm object
	 *
	 *@param  s  The new collection value
	 */
	public void setCollection(String s) {
		this.collection = s;
	}


	/**
	 *  Gets the collection attribute of the StatusForm object
	 *
	 *@return    The collection value
	 */
	public String getCollection() {
		return collection;
	}


	/**
	 *  Sets the collectionName attribute of the StatusForm object
	 *
	 *@param  s  The new collectionName value
	 */
	public void setCollectionName(String s) {
		this.collectionName = s;
	}


	/**
	 *  Gets the collectionName attribute of the StatusForm object
	 *
	 *@return    The collectionName value
	 */
	public String getCollectionName() {
		return collectionName;
	}

	
	public ActionErrors validate(ActionMapping mapping, HttpServletRequest request) {
		// prtln ("validate()");
		ActionErrors errors = new ActionErrors();
		SessionRegistry sessionRegistry =
			(SessionRegistry) servlet.getServletContext().getAttribute("sessionRegistry");
		SessionBean sb = sessionRegistry.getSessionBean(request);
		
		boolean authenticationEnabled = 
			(Boolean)servlet.getServletContext().getAttribute ("authenticationEnabled");
		
		if (command == null) {
			errors.add ("error", new ActionError ("generic.error", "command required"));
		}
			
		else {
		
			if (collection != null && collection.trim().length() > 0) {
				Roles.Role role = ActionPath.getRole (mapping);
				if (authenticationEnabled &&
					!sb.isAuthorizedCollection(role, collection))
					errors.add ("error", new ActionError ("error.unauthorized.collection", collection));
				return errors;
			}
			
			if (command.equals ("save")) {
				if (status == null) {
					errors.add("status", new ActionError("field.required", "Status"));
				}
				else if (status.trim().equals(StatusFlags.IMPORTED_STATUS)) {
					errors.add("status", new ActionError("generic.error", "Please assign a status other than \"" + StatusFlags.IMPORTED_STATUS + "\""));
				}
			}
		}
		return errors;
	}


	/**
	 *  Output a line of text to standard out, with datestamp, if debug is set to
	 *  true.
	 *
	 *@param  s  The String that will be output.
	 */
	protected final void prtln(String s) {
		if (debug) {
			System.out.println("StatusForm: " + s);
		}
	}

}

