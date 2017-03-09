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
package org.dlese.dpc.oai.provider.action.form;

import org.dlese.dpc.propertiesmgr.*;
import org.dlese.dpc.repository.*;
import org.dlese.dpc.webapps.tools.*;

import java.text.SimpleDateFormat;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionError;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionServlet;
import org.apache.struts.util.MessageResources;

import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.io.*;

/**
 *  Form used to support OAI-PMH search page.
 *
 * @author    John Weatherley
 */
public final class OaiPmhSearchForm extends ActionForm
{
	
	private boolean debug = true;
	private String message = null;
	private String baseURL = null;
	private String oaiIdPfx = null;
	private String exampleId = null;
	private List availableFormats = null;
	private List avalableSets = null;
	private String showAdvanced = "false";
	private String exampleFormat = null;	
	private String contextURL = null;
	
	public String getShowAdvanced()
	{
		return showAdvanced;
	}
	
	public void setShowAdvanced(String showAdvanced)
	{
		this.showAdvanced = showAdvanced;
	}



	public String getExampleFormat()
	{
		return exampleFormat;
	}
	
	public void setExampleFormat(String exampleFormat)
	{
		this.exampleFormat = exampleFormat;
	}



	public String getContextURL() {
		return contextURL;
	}



	public void setContextURL(String contextURL) {
		this.contextURL = contextURL;
	}

	
	/**
	 *  Gets the oaiIdPfx attribute of the ProviderBean object
	 *
	 * @return    The oaiIdPfx value
	 */
	public String getOaiIdPfx() {
		if (oaiIdPfx == null)
			return "";
		return oaiIdPfx;
	}


	/**
	 *  Sets the oaiIdPfx attribute of the ProviderBean object
	 *
	 * @param  oaiIdPfx  The new oaiIdPfx value
	 */
	public void setOaiIdPfx(String oaiIdPfx) {
		this.oaiIdPfx = oaiIdPfx;
	}	
	

	public List getAvailableFormats()
	{
		if(availableFormats == null || availableFormats.size() == 0)
		{
			ArrayList tmp = new ArrayList();
			tmp.add("none-available-yet");
			return tmp;
		}
		return availableFormats;
	}
	
	public void setAvailableFormats(List availableFormats) 
	{
		this.availableFormats = availableFormats;
	}	

	
	public List getAvailableSets()
	{	
		return avalableSets;
	}
	
	public void setAvailableSets(List avalableSets)
	{
		this.avalableSets = avalableSets;
	}		

	public void setExampleId(String exampleId){
		this.exampleId = exampleId;
	}

	public String getExampleId(){
		return exampleId;
	}

	/**
	 * A list of UTC dates in the past in the following order: one minute, one hour,
	 * one day, one week, one month, one year.
	 *
	 * @return   A list of UTC dates in the past.
	 */
	public List getUtcDates()
	{
		long curTime = System.currentTimeMillis();
		long min = 1000*60;
		long hour = min*60;
		long day = hour*24;
		long week = day*7;
		long month = day*30;
		long year = day*365;
		
		List dates = new ArrayList();

		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'z'");
		df.setTimeZone(new SimpleTimeZone(SimpleTimeZone.UTC_TIME, "UTC"));
		Date date = new Date(curTime - min);
		dates.add(new DateLabelPair(df.format(date),"one minute ago"));
		date.setTime(curTime - hour);
		dates.add(new DateLabelPair(df.format(date),"one hour ago"));
		date.setTime(curTime - day);
		dates.add(new DateLabelPair(df.format(date),"one day ago"));
		date.setTime(curTime - week);
		dates.add(new DateLabelPair(df.format(date),"one week ago"));
		date.setTime(curTime - month);
		dates.add(new DateLabelPair(df.format(date),"one month ago"));	
		date.setTime(curTime - year);
		dates.add(new DateLabelPair(df.format(date),"one year ago"));	
		return dates;
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

	/**  Constructor for the ProviderAdminForm Bean object */
	public OaiPmhSearchForm() {
		//prtln("OaiPmhSearchForm() ");
	}


	/**
	 *  Sets the message attribute of the ProviderAdminForm object
	 *
	 * @param  message  The new message value
	 */
	public void setMessage(String message) {
		this.message = message;
	}


	/**
	 *  Gets the message attribute of the ProviderAdminForm object
	 *
	 * @return    The message value
	 */
	public String getMessage() {
		return message;
	}




	// ************************ Validation methods ***************************

	/**
	 *  Validate the input. This method is called AFTER the setter method is
	 *  called.
	 *
	 * @param  mapping  The ActionMapping used.
	 * @param  request  The HttpServletRequest for this request.
	 * @return          An ActionError containin any errors that had occured.
	 */
	public ActionErrors validate(ActionMapping mapping, HttpServletRequest request) {
		ActionErrors errors = new ActionErrors();

		return errors;
	}
	
	
	//================================================================

	/**
	 *  Return a string for the current time and date, sutiable for display in log
	 *  files and output to standout:
	 *
	 * @return    The dateStamp value
	 */
	protected static String getDs() {
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
	 *  Output a line of text to standard out, with datestamp, if debug is set to
	 *  true.
	 *
	 * @param  s  The String that will be output.
	 */
	protected final void prtln(String s) {
		if (debug)
			System.out.println(getDs() + " " + s);
	}


	/**
	 *  Sets the debug attribute of the DocumentService object
	 *
	 * @param  db  The new debug value
	 */
	protected final void setDebug(boolean db) {
		debug = db;
	}	

}


