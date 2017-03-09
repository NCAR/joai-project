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
package org.dlese.dpc.services.dcs.action.form;

import org.dlese.dpc.propertiesmgr.*;
import org.dlese.dpc.webapps.tools.*;
import org.dlese.dpc.index.*;
import org.dlese.dpc.index.reader.*;
import org.dlese.dpc.oai.*;
import org.dlese.dpc.repository.*;
import org.dlese.dpc.dds.action.form.VocabForm;

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
 *  A ActionForm bean that holds data for DDS web services and has access to vocab info.
 *
 * @author    John Weatherley
 * @see       org.dlese.dpc.services.dds.action.DDSServicesAction
 */
public class RecommenderForm extends ActionForm implements Serializable {

	private static boolean debug = true;
	private String errorMsg = null;
	private ResultDocList results = null;
	private String authorizedFor = null;
	private String recordXml = null;
	private String recordFormat = null;
	private int s = 0, n = 10;
	private List xmlFormats = null;
	private String requestElementLabel = null;
	private String id = null;
	private String collection = null;
	private String xmlFormat = null;
	private List errorList = null;
	private String [] statuses = null;
	private List statusLabels = null;

	// Bean properties:

	/**  Constructor for the RepositoryForm object */
	public RecommenderForm() { }


	public String getId () {
		return id;
	}
	
	public void setId (String id) {
		this.id = id;
	}
	
	public String getCollection () {
		return collection;
	}
	
	public void setCollection (String collection) {
		this.collection = collection;
	}
	
	public String getXmlFormat () {
		return xmlFormat;
	}
	
	public void setXmlFormat (String xmlFormat) {
		this.xmlFormat = xmlFormat;
	}	
	
	public String [] getStatuses () {
		return statuses;
	}
	
	public void setStatuses (String [] statusValues) {
		statuses = statusValues;
	}
	
	public List getStatusLabels () {
		return statusLabels;
	}
	
	public void setStatusLabels (List labels) {
		statusLabels = labels;
	}
	
	public void setErrorList (List errorList) {
		this.errorList = errorList;
	}	
	
	public List getErrorList(){
		return errorList;
	}	
	
	public List getXmlFormats(){
		return xmlFormats;
	}
	
	public void setXmlFormats(List var){
		xmlFormats = var;
		if(xmlFormats != null)
			Collections.sort(xmlFormats);
	}
	
	/**
	 *  Gets the localizedRecordXml attribute of the RecommenderForm object
	 *
	 * @return    The localizedRecordXml value
	 */
	public String getLocalizedRecordXml() {
		String xml = recordXml.replaceAll("xmlns.*=\".*\"|xsi:schemaLocation.*=\".*\"", "");
		if (recordFormat == null)
			return xml;
		else if (recordFormat.equals("oai_dc"))
			return xml.replaceAll("oai_dc:dc", "oai_dc").replaceAll("<dc:", "<").replaceAll("</dc:", "</");
		else if (recordFormat.equals("nsdl_dc"))
			return xml.replaceAll("nsdl_dc:nsdl_dc", "ndsl_dc").replaceAll("<dc:", "<").replaceAll("<dct:", "<").replaceAll("</dc:", "</").replaceAll("</dct:", "</");
		return xml;
	}

		
	/**
	 *  Gets the recordXml attribute of the RecommenderForm object
	 *
	 * @return    The recordXml value
	 */
	public String getRecordXml() {
		return recordXml;
	}	

	/**
	 *  Sets the recordXml attribute of the RecommenderForm object
	 *
	 * @param  val  The new recordXml value
	 */
	public void setRecordXml(String val) {
		recordXml = val;
	}


	/**
	 *  Sets the recordFormat attribute of the RecommenderForm object
	 *
	 * @param  val  The new recordFormat value
	 */
	public void setRecordFormat(String val) {
		recordFormat = val;
	}


	/**
	 *  Gets the recordFormat attribute of the RecommenderForm object
	 *
	 * @return    The recordFormat value
	 */
	public String getRecordFormat() {
		return recordFormat;
	}


	/**
	 *  Gets the s attribute of the RecommenderForm object
	 *
	 * @return    The s value
	 */
	public int getS() {
		return s;
	}


	/**
	 *  Sets the s attribute of the RecommenderForm object
	 *
	 * @param  val  The new s value
	 */
	public void setS(int val) {
		s = val;
	}


	/**
	 *  Gets the n attribute of the RecommenderForm object
	 *
	 * @return    The n value
	 */
	public int getN() {
		return n;
	}


	/**
	 *  Sets the n attribute of the RecommenderForm object
	 *
	 * @param  val  The new n value
	 */
	public void setN(int val) {
		n = val;
	}


	/**
	 *  Gets the role name for which this user is authorized
	 *
	 * @return    The authorizedFor value
	 */
	public String getAuthorizedFor() {
		return authorizedFor;
	}



	/**
	 *  Sets the role name for which this user is authorized
	 *
	 * @param  val  The new authorizedFor value
	 */
	public void setAuthorizedFor(String val) {
		authorizedFor = val;
	}


	/**
	 *  Sets the errorMsg attribute of the RecommenderForm object
	 *
	 * @param  errorMsg  The new errorMsg value
	 */
	public void setErrorMsg(String errorMsg) {
		prtln ("errorMsg: " + errorMsg);
		this.errorMsg = errorMsg;
	}


	/**
	 *  Gets the errorMsg attribute of the RecommenderForm object
	 *
	 * @return    The errorMsg value
	 */
	public String getErrorMsg() {
		return errorMsg;
	}


	/**
	 *  Gets the results attribute of the RecommenderForm object
	 *
	 * @return    The results value
	 */
	public ResultDocList getResults() {
		return results;
	}


	/**
	 *  Sets the results attribute of the RecommenderForm object
	 *
	 * @param  results  The new results value
	 */
	public void setResults(ResultDocList results) {
		this.results = results;
	}


	/**
	 *  Gets the number of matching results.
	 *
	 * @return    The numResults value
	 */
	public int getNumResults() {
		if (results == null)
			return 0;
		return results.size();
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


