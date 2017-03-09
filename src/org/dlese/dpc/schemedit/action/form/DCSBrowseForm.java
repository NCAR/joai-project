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

import org.dlese.dpc.index.ResultDoc;
import org.dlese.dpc.schemedit.*;
import org.dlese.dpc.schemedit.action.*;
import org.dlese.dpc.schemedit.dcs.DcsSetInfo;
import org.dlese.dpc.schemedit.config.ErrorLog;
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

/**
 *  Supports the collection browser of the DCS. This class works in conjunction
 *  with {@link org.dlese.dpc.schemedit.action.DCSBrowseAction}.
 *
 *@author    Jonathan Ostwald
 */
public class DCSBrowseForm extends ActionForm implements Serializable {
	private static boolean debug = true;

	private HttpServletRequest request;
 	private int numDocs = 0;
	private List sets = null;
	private Map userRoles = null;
	private String contextURL = null;
	private String sortSpec = "collection";

	/**
	 *  Constructor for the DCSBrowseForm object
	 */
	public DCSBrowseForm() { }

	public int getNumDocs () {
		return numDocs;
	}
	
	public void setNumDocs (int num) {
		numDocs = num;
	}
	
	/**
	 *  Gets the contextURL attribute of the DCSBrowseForm object
	 *
	 *@return    The contextURL value
	 */
	public String getContextURL() {
		return contextURL;
	}


	/**
	 *  Sets the contextURL attribute of the DCSBrowseForm object
	 *
	 *@param  contextURL  The new contextURL value
	 */
	public void setContextURL(String contextURL) {
		this.contextURL = contextURL;
	}

	/**
	 *  Gets the sets configured in the RepositoryManager. Overloaded method from
	 *  RepositoryForm.
	 *
	 *@return    The sets value
	 */
	public List getSets() {
		if (sets == null) {
			return new ArrayList();
		}
		if (sortSpec == null || sortSpec.equals("collection")) {
			Collections.sort(sets);
		}
		else {
			Collections.sort(sets, DcsSetInfo.getComparator(sortSpec));
		}		
		return sets;
	}
	
	/**
	 *  Sets the sets attribute of the RepositoryAdminForm object. The set items
	 are instances of {@link org.dlese.dpc.schemedit.dcs.DcsSetInfo}.
	 *
	 *@param  setInfoSets  The new sets value
	 */
	public void setSets(List setInfoSets) {
		if (setInfoSets == null) {
			sets = new ArrayList();
		}
		else
			sets = setInfoSets;
	}

	public Map getUserRoles () {
		return this.userRoles;
	}
	
	public void setUserRoles (Map roleMap) {
		this.userRoles = roleMap;
	}
	
	/**
	 *  Gets the sortSpec attribute of the RepositoryAdminForm object
	 *
	 *@return    The sortSpec value
	 */
	public String getSortSpec() {
		return sortSpec;
	}


	/**
	 *  Sets the sortSpec attribute of the RepositoryAdminForm object
	 *
	 *@param  sortSpec  The new sortSpec value
	 */
	public void setSortSpec(String sortSpec) {
		this.sortSpec = sortSpec;
	}

	/**
	 *  DESCRIPTION
	 *
	 *@param  mapping  DESCRIPTION
	 *@param  request  DESCRIPTION
	 */
	public void reset(ActionMapping mapping, javax.servlet.http.HttpServletRequest request) {
/* 		//selectedIdMapperErrors = null;
		selectedEditors = new String[]{};
		selectedStatuses = new String[]{}; */
	}

	/**
	 *  Sets the request attribute of the DCSBrowseForm object.
	 *
	 *@param  request  The new request value
	 */
	public void setRequest(HttpServletRequest request) {
		this.request = request;
	}

	//================================================================

	/**
	 *  Output a line of text to standard out, with datestamp, if debug is set to
	 *  true.
	 *
	 *@param  s  The String that will be output.
	 */
	private final void prtln(String s) {
		if (debug) {
			System.out.println("DCSBrowseForm: " + s);
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


