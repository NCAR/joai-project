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
 *  A bean that holds data for editing records.
 *
 * @author    John Weatherley
 * @see       org.dlese.dpc.dds.action.DDSReportingAction
 */
public class DDSEditRecordForm extends ActionForm implements Serializable {

	private static boolean debug = true;

	// Bean properties:

	/**  Constructor for the RepositoryForm object */
	public DDSEditRecordForm() { }



	
	// --------------- Methods for putRecord, delete record... -------------------
	
	private String recordXml = null;
	private String collection = null;
	private String xmlFormat = null;
	private String recordId = null;
	private String deleteRecord = null;
	private String id = null;
	
	public String getId(){
		return this.id;	
	}
	
	public void setId(String value){
		this.id = value;	
	}		
	
	public String getRecordXml(){
		return this.recordXml;	
	}
	
	public void setRecordXml(String value){
		this.recordXml = value;	
	}	

	public String getCollection(){
		return this.collection;	
	}
	
	public void setCollection(String value){
		this.collection = value;	
	}	

	public String getXmlFormat(){
		return this.xmlFormat;	
	}
	
	public void setXmlFormat(String value){
		this.xmlFormat = value;	
	}	

	public String getRecordId(){
		return this.recordId;	
	}
	
	public void setRecordId(String value){
		this.recordId = value;	
	}	

	public String getDeleteRecord(){
		return this.deleteRecord;	
	}
	
	public void setDeleteRecord(String value){
		this.deleteRecord = value;	
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



