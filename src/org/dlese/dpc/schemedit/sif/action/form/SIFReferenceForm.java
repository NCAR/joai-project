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
package org.dlese.dpc.schemedit.sif.action.form;

// import org.dlese.dpc.schemedit.*;
import org.dlese.dpc.schemedit.display.CollapseUtils;
import org.dlese.dpc.repository.*;
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
// import java.io.*;
// import java.text.*;
// import java.net.*;
// import java.util.regex.*;

/**
 *
 *@author    ostwald 
 */
public class SIFReferenceForm extends ActionForm {

	private boolean debug = true;
	private String command = null;
	private String recId = null;
	private String newRecId = null;
	private String elementId = null;
	private String[] sifTypes = null;
	private String searchString = null;
	private Map objectMap = null;
	private HttpServletRequest request;
	private String selectedType = null;
	private List typeOptions = null;
	private String description = null;
	private String title = null;
	private Map setMap = null;
	private String collection = null;


	/* private SetInfo setInfo = null; */
	// input params
	/**
	 *  Constructor
	 */
	public SIFReferenceForm() { }


	/**
	 *  Description of the Method
	 */
	public void clear() {
		objectMap = null;
		command = null;
		recId = null;
		searchString = null;
		sifTypes = null;
		collection = null;
		selectedType = null;
		title = null;
	}

	public Map getObjectMap () {
		return this.objectMap;
	}
	
	public void setObjectMap (Map map) {
		this.objectMap = map;
	}
	
	public String getCommand () {
		return command;
	}
	
	public void setCommand (String cmd) {
		command = cmd;
	}
	
	/**
	 *  Gets the recId attribute of the SIFReferenceForm object
	 *
	 *@return    The recId value
	 */
	public String getRecId() {
		return recId;
	}

	/**
	 *  Sets the recId attribute of the SIFReferenceForm object
	 *
	 *@param  id  The new recId value
	 */
	public void setRecId(String id) {
		recId = id;
	}
	
	/**
	 *  Gets the newRecId attribute of the SIFReferenceForm object
	 *
	 *@return    The newRecId value
	 */
	public String getNewRecId() {
		return newRecId;
	}

	/**
	 *  Sets the newRecId attribute of the SIFReferenceForm object
	 *
	 *@param  id  The new newRecId value
	 */
	public void setNewRecId(String id) {
		newRecId = id;
	}

	public String getElementId () {
		return this.elementId;
	}
	
	public void setElementId (String id) {
		this.elementId = id;
	}
	
	public String getElementPath () {
		prtln ("getElementsPath: " + CollapseUtils.idToPath(this.elementId));
		return CollapseUtils.idToPath(this.elementId);
	}
	
	public String getRefTypeSelectId () {
		prtln ("getRefTypeSelectId: " + CollapseUtils.pathToId(this.getElementPath() + "/@SIF_RefObject"));
		return CollapseUtils.pathToId(this.getElementPath() + "/@SIF_RefObject");
	}
	
	public String [] getSifTypes () {
		return this.sifTypes;
	}
	
	public void setSifTypes (String [] types) {
		this.sifTypes = types;
	}

	public String getSearchString () {
		return this.searchString;
	}
	
	public void setSearchString (String s) {
		this.searchString = s;
	}
	
	public String getSelectedType () {
		return selectedType;
	}
	
	public void setSelectedType (String type) {
		selectedType = type;
	}
	
	public List getTypeOptions () {
		return this.typeOptions;
	}
	
	public void setTypeOptions (List options) {
		this.typeOptions = options;
	}
	
	public String getDescription () {
		return this.description;
	}
	
	public void setDescription (String s) {
		this.description = s;
	}
	
	public String getTitle () {
		return this.title;
	}
	
	public void setTitle (String s) {
		this.title = s;
	}
	
	public Map getSetMap () {
		return setMap;
	}
	
	public void setSetMap (Map map) {
		this.setMap = map;
	}
	
	public String getCollection () {
		return this.collection;
	}
	
	public void setCollection (String col) {
		this.collection = col;
	}
	
	/**
	 *  Output a line of text to standard out, with datestamp, if debug is set to
	 *  true.
	 *
	 *@param  s  The String that will be output.
	 */
	protected final void prtln(String s) {
		if (debug) {
			System.out.println("SIFReferenceForm: " + s);
		}
	}

}

