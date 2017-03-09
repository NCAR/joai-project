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
package org.dlese.dpc.services.commcore.action.form;

import org.dlese.dpc.standards.commcore.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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
 *  ActionForm bean that holds data for AsnService.
 *

 */
public class CommCoreForm extends ActionForm implements Serializable {

	private String errorMsg = null;
	private Collection standards = null;
	private Standard standard = null;
	private String baseStd = null;
	private CommCoreServiceHelper commCoreHelper = null;
	String stdId = null;
	String docId = null;
	
	private static Log log = LogFactory.getLog(CommCoreForm.class);

	// Bean properties:

	/**  Constructor for the RepositoryForm object */
	public CommCoreForm() { }

	public void clear() {
		errorMsg = null;
		standards = null;
	}
		
	public void setCommCoreHelper (CommCoreServiceHelper helper) {
		this.commCoreHelper = helper;
	}
	
	public String getBaseStd () {
		return this.baseStd;
	}
	
	public void setBaseStd (String std) {
		this.baseStd = std;
	}

	public void setStandard (Standard standard) {
		this.standard = standard;
	}
	
	public Standard getStandard () {
		return this.standard;
	}
	
	public String getStdId () {
		return this.stdId;
	}
	
	public void setStdId (String id) {
		this.stdId = id;
	}
	
	public String getDocId () {
		return this.docId;
	}
	
	public void setDocId (String id) {
		this.docId = id;
	}
	
	/**
	* Returns the std corresponding to this.stdId from the standards document
	* containing this.standard.
	*/
	public Standard getStandardById () {
		if (stdId == null || commCoreHelper == null || this.standard == null) {
			log.error ("returning null");
			return null;
		}
		Standard std = commCoreHelper.getStandard(this.standard.getDocumentIdentifier(), stdId);
		if (std == null)
			log.error ("std not found");
		return std;
	}
	
	public void setStandards (Collection stdWrappers) {
		standards = stdWrappers;
	}
	
	public Collection getStandards () {
		return standards;
	}
	
	/**
	 *  Sets the errorMsg attribute of the DDSServicesForm object
	 *
	 * @param  errorMsg  The new errorMsg value
	 */
	public void setErrorMsg(String errorMsg) {
		this.errorMsg = errorMsg;
	}


	/**
	 *  Gets the errorMsg attribute of the DDSServicesForm object
	 *
	 * @return    The errorMsg value
	 */
	public String getErrorMsg() {
		return errorMsg;
	}
	
	
	public class DateLabelPair
	{
		private String date,label;
		public DateLabelPair(String date,String label)
		{	
			this.date = date;
			this.label = label;
		}
		
		public String getDate()
		{
			return date;
		}
		public String getLabel()
		{
			return label;
		}
	}	
	
	//================================================================


	/**
	 *  Return a string for the current time and date, sutiable for display in log files and
	 *  output to standout:
	 *
	 * @return    The dateStamp value
	 */
	protected final static String getDs() {
		return
			new SimpleDateFormat("MMM d, yyyy h:mm:ss a zzz").format(new Date());
	}

}


