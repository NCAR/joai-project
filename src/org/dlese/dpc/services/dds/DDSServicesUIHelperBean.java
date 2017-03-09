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
package org.dlese.dpc.services.dds;

import org.dlese.dpc.propertiesmgr.*;
import org.dlese.dpc.webapps.tools.*;
import org.dlese.dpc.index.*;
import org.dlese.dpc.index.reader.*;
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
 *  A bean that performs functions helpful in the UI. Meant to be an applicateion scoped
 *  bean for uses in the JSP pages.
 *
 * @author    John Weatherley
 */
public class DDSServicesUIHelperBean implements Serializable {

	private static boolean debug = true;

	// Bean properties:

	/**  Constructor for the DDSServicesUIHelperBean object */
	public DDSServicesUIHelperBean() { }


	/**
	 *  A list of UTC dates in the past in the following order: one minute, one hour, one
	 *  day, one week, one month, one year.
	 *
	 * @return    A list of UTC dates in the past.
	 */
	public List getUtcDates() {
		long curTime = System.currentTimeMillis();
		long min = 1000 * 60;
		long hour = min * 60;
		long day = hour * 24;
		long week = day * 7;
		long month = day * 30;
		long year = day * 365;

		List dates = new ArrayList();

		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'z'");
		df.setTimeZone(new SimpleTimeZone(SimpleTimeZone.UTC_TIME, "UTC"));
		Date date = new Date(curTime - min);
		dates.add(new DateLabelPair(df.format(date), "one minute ago"));
		date.setTime(curTime - hour);
		dates.add(new DateLabelPair(df.format(date), "one hour ago"));
		date.setTime(curTime - day);
		dates.add(new DateLabelPair(df.format(date), "one day ago"));
		date.setTime(curTime - week);
		dates.add(new DateLabelPair(df.format(date), "one week ago"));
		date.setTime(curTime - month);
		dates.add(new DateLabelPair(df.format(date), "one month ago"));
		date.setTime(curTime - year);
		dates.add(new DateLabelPair(df.format(date), "one year ago"));
		return dates;
	}


	/**
	 *  Object used to store Dates and labels for the dates
	 *
	 * @author    John Weatherley
	 */
	public class DateLabelPair {
		private String date, label;


		/**
		 *  Constructor for the DateLabelPair object
		 *
		 * @param  date   The date String
		 * @param  label  The label String
		 */
		public DateLabelPair(String date, String label) {
			this.date = date;
			this.label = label;
		}


		/**
		 *  Gets the date attribute of the DateLabelPair object
		 *
		 * @return    The date value
		 */
		public String getDate() {
			return date;
		}


		/**
		 *  Gets the label attribute of the DateLabelPair object
		 *
		 * @return    The label value
		 */
		public String getLabel() {
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


