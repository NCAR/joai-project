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
package org.dlese.dpc.schemedit.ndr.action.form;

import org.dlese.dpc.schemedit.*;
import org.dlese.dpc.schemedit.ndr.SyncReport;
import org.dlese.dpc.schemedit.ndr.SyncService;
import org.dlese.dpc.schemedit.dcs.*;
import org.dlese.dpc.ndr.apiproxy.NDRConstants;
import org.dlese.dpc.ndr.reader.AgentReader;
import org.dlese.dpc.repository.*;
import org.dlese.dpc.index.ResultDoc;
import org.dlese.dpc.index.reader.*;
import org.dlese.dpc.xml.schema.*;
import org.dlese.dpc.xml.*;

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
 *  ActionForm bean for handling interactions with the NDR.
 *
 */
public class NDRForm extends ActionForm {

	private boolean debug = true;
	private HttpServletRequest request;
	private SyncReport syncReport = null;
	private String proxyResponse = null;
	private String handle = null;
	private List browserHandles = null;
	private List ndrCollections = null;
	private List dcsCollections = null;
	private Map mdpHandleMap = null;
	private AgentReader appAgent = null;
	private SyncService syncService = null;

	/**
	 *  Constructor
	 */
	public NDRForm() { }
	
	public void setSyncService (SyncService svc) {
		this.syncService = svc;
	}
	
	public SyncService getSyncService () {
		return this.syncService;
	}
	
	public boolean getIsSyncing () {
		return (this.syncService == null ? false : this.syncService.getIsProcessing());
	}
	
	/** Stores results of a Sync operation */
 	public void setSyncReport (SyncReport report) {
		this.syncReport = report;
	}
	
	/** Gets results of a Sync operation */
	public SyncReport getSyncReport () {
		return (this.syncReport);
	}
	
	private String progress = null;
	
	public void setProgress (String progress) {
		this.progress = progress;
	}
	
	public String getProgress () {
		return this.progress;
	}
	
	public void setHandle (String handle) {
		this.handle = handle;
	}
	
	public String getHandle () {
		return this.handle;
	}
	
	public String getNdrApiBaseUrl () {
		return NDRConstants.getNdrApiBaseUrl();
	}

	public String getNcsAgentHandle () {
		return NDRConstants.getNcsAgent ();
	}
	
	public void setAppAgent (AgentReader agentReader) {
		this.appAgent = agentReader;
	}
	
	public String getAppAgentIdentity () {
		if (this.appAgent != null)
			return this.appAgent.getIdentifier();
		else
			return null;
	}
	
	public String getAppAgentIdentityType () {
		if (this.appAgent != null)
			return this.appAgent.getIdentifierType();
		else
			return null;
	}

	/** Stores result of async call to NDR */
	public void setProxyResponse (String response) {
		this.proxyResponse = response;
	}
	
	/** Get result of async call to NDR. Can be json object, xml, or a simple String */
	public String getProxyResponse () {
		return this.proxyResponse;
	}
	
	/** Handles to either aggregator or mdp objects for use in NDR Browser */
 	public void setBrowserHandles (List handles) {
		this.browserHandles = handles;
	}
	
	public List getBrowserHandles () {
		return this.browserHandles;
	} 
	
	/** handleMap associates a setSpec with the corresponding mdpHandle */
	public void setMdpHandleMap (Map handleMap) {
		this.mdpHandleMap = handleMap;
	}
	
	/** Gets Map associating a setSpec with the corresponding mdpHandle */
	public Map getMdpHandleMap () {
		return this.mdpHandleMap;
	}
	
	/** A list of setInfo instances for each collection registered with NDR */
	public void setNdrCollections (List sets) {
		this.ndrCollections = sets;
	}
	
	/** Gets list of setInfo instances for each collection registered with NDR */
	public List getNdrCollections () {
		return this.ndrCollections;
	}
	
	/** A list of setInfo instances for each collection NOT registered with NDR */
	public void setDcsCollections (List sets) {
		this.dcsCollections = sets;
	}
	
	/** Gets setInfo instances for each collection NOT registered with NDR */
	public List getDcsCollections () {
		return this.dcsCollections;
	}
	
	/**
	 *  Output a line of text to standard out, with datestamp, if debug is set to
	 *  true.
	 *
	 *@param  s  The String that will be output.
	 */
	protected final void prtln(String s) {
		if (debug) {
			SchemEditUtils.prtln (s, "NDRForm");
		}
	}

}

