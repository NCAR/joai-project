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
package org.dlese.dpc.oai.harvester;

import org.dlese.dpc.repository.*;
import org.dlese.dpc.oai.harvester.action.*;
import org.dlese.dpc.oai.harvester.action.form.*;
import org.dlese.dpc.index.reader.*;
import org.dlese.dpc.index.writer.*;
import org.dlese.dpc.index.*;
import org.dlese.dpc.webapps.tools.*;
import org.dlese.dpc.xml.*;
import org.dlese.dpc.action.*;
import org.dlese.dpc.datamgr.*;
import org.dlese.dpc.util.*;
import org.dlese.dpc.oai.harvester.structs.*;

// JDK imports
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.OutputStreamWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.LinkedList;
import java.util.Properties;

// Enterprise imports
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;


/**
 *  A servlet used to manage harvesting metadata via OAI-PMH. 
 *
 * @author    John Weatherley
 */
public final class OAIHarvesterServlet extends HttpServlet {

	// Caution: ALL class and instance variables are shared.
	// Multiple requests may use this object concurrently.

	private boolean initialized = false;
	private boolean debug = true;

	//private String docRoot = null;

	/**
	 *  Init method called by the web application server upon startup
	 *
	 * @param  conf                  Servlet configuration
	 * @exception  ServletException  If error
	 */
	public void init(ServletConfig conf)
		 throws ServletException {

		
		System.out.println(getDateStamp() + " OAIHarvesterServlet starting");
		String initErrorMsg = "";

		try {
			// super.init must be called before getServletContext().getRealPath("/");
			super.init(conf);
		} catch (Throwable exc) {
			initErrorMsg = "OAIHarvesterServlet Initialization Error:\n  " + exc;
			prtlnErr(initErrorMsg);
		}

		ServletContext context = getServletContext();		
		
		// Grab the document root for this servlet.
		// Note: this cannot be grabbed before super.init():
		//docRoot = getServletContext().getRealPath("/");

		// Check to see if init has been called previously. If yes, do nothing
		// (This happens when the app is running in a Tomcat environment configured
		// with multiple JVM's using the same Tomcat code base ala Tom Boyd).
		if (initialized){
			prtlnErr("OAIHarvesterServlet already initialized. Will not initialize twice.");
			return;		
		}
			
		
		initialized = true;
				
		String bugs = context.getInitParameter("debug");
		if (bugs != null && bugs.equalsIgnoreCase("true")) {
			debug = true;
			prtln("Outputting debug info");
		}else{
			debug = false;		
			prtln("Debug info disabled");
		}
		
		// Set all debugging:
		HarvesterAdminAction.setDebug(debug);
		IndexingHarvestMsgHandler.setDebug(debug);
		HarvestReportAction.setDebug(debug);
		HarvesterAdminForm.setDebug(debug);
		ScheduledHarvestManager.setDebug(debug);
		ScheduledHarvest.setDebug(debug);
		Harvester.setDebug(debug);
		
		/* int updateFrequency = 8;		
		String fileSyncronizationFrequency = (String) context.getInitParameter("updateFrequency");
		try {
			if (fileSyncronizationFrequency != null) {
				updateFrequency = Integer.parseInt(fileSyncronizationFrequency);
			}
		} catch (Throwable nfe) {
			prtlnErr("Error parsing updateFrequency: " + nfe);
		}	 */	
		
	
		String harvesterData = context.getInitParameter("harvesterData");
		if (harvesterData == null || harvesterData.length() == 0) {
			initErrorMsg = "OAIHarvesterServlet init parameter \"harvesterData\" is missing";
			prtlnErr(initErrorMsg);
			throw new ServletException(initErrorMsg);
		}		

		
		File harvesterDataDir = new File(GeneralServletTools.getAbsolutePath(harvesterData,context));
		if (!harvesterDataDir.exists()) {
			prtln("Created directory " + harvesterDataDir.getAbsolutePath());
			harvesterDataDir.mkdir();
		}

	
		// Create a Harvest Log Index
		SimpleLuceneIndex harvestLogIndex =
			new SimpleLuceneIndex(harvesterDataDir.getAbsolutePath() + "/harvest_log_index");
		harvestLogIndex.setOperator(SimpleLuceneIndex.DEFAULT_AND);
		//harvestLogIndex.setDebug(debug);


		
		// Set-up a DataStore
		SimpleDataStore harvesterSettings = null;	
		try{
			File f = new File(harvesterDataDir.getAbsolutePath() + "/harvesterSettings");
			f.mkdir();
			harvesterSettings = 
				new SimpleDataStore(f.getAbsolutePath(),true);
		}catch(Throwable e){
			initErrorMsg = "Unable to initialize harvesterSettings: " + e;
			prtlnErr(initErrorMsg);
			throw new ServletException(initErrorMsg);			
		}
		
		if(harvesterSettings == null)
			prtlnErr("harvesterSettings is null!");
		
		// Launch timer threads on each scheduled harvest:
		Hashtable shs = (Hashtable) harvesterSettings.get(Keys.SCHEDULED_HARVESTS);
		if(shs != null){
			ScheduledHarvest [] scheduled = (ScheduledHarvest [])shs.values().toArray( new ScheduledHarvest[]{});	
			prtln("There are " + scheduled.length + " scheduled harvests...");
			/* for(int i = 0; i < scheduled.length; i++)
				scheduled[i].init(new SimpleHarvestMessageHandler(),scheduled[i].getHarvestDir()); */
		}
		
		int harvestTimeOutMilliseconds = Integer.parseInt(context.getInitParameter("harvestTimeOutMilliseconds"));

		ScheduledHarvestManager scheduledHarvestManager = new ScheduledHarvestManager(harvesterSettings,
					new File(GeneralServletTools.getAbsolutePath("WEB-INF/harvested_records", context)),
					harvestLogIndex,harvestTimeOutMilliseconds);
		
		// Make context scope objects available::
		context.setAttribute("harvestLogIndex", harvestLogIndex);
		context.setAttribute("harvesterData", harvesterData);
		context.setAttribute("harvesterSettings", harvesterSettings);
		context.setAttribute("scheduledHarvestManager", scheduledHarvestManager);

		System.out.println(getDateStamp() + " OAIHarvesterServlet initialized.");
	}


	private void addXslConverter(String paramVal, XMLConversionService xmlConversionService)
		 throws ServletException {
		String[] vals = paramVal.split("\\|");
		if (vals.length != 3) {
			prtlnErr("addXslConverter() error: could not parse parameter '" + paramVal + "'");
			return;
		}

		prtln("addXslConverter() adding converter " + vals[0] + " " + vals[1] + " " + vals[2]);

		xmlConversionService.addXslStylesheet(vals[1], vals[2],
			GeneralServletTools.getAbsolutePath(
			"WEB-INF/xsl_files/" + vals[0], getServletContext()));

	}


	private void addJavaConverter(String paramVal, XMLConversionService xmlConversionService)
		 throws ServletException {
		prtln("addJavaConverter() adding converter " + paramVal);

		String[] vals = paramVal.split("\\|");
		if (vals.length != 3) {
			prtlnErr("addJavaConverter() error: could not parse parameter '" + paramVal + "'");
			return;
		}

		prtln("addJavaConverterClass() adding converter " + vals[0] + " " + vals[1] + " " + vals[2]);

		xmlConversionService.addJavaConverterClass(vals[1], vals[2], vals[0],getServletContext());
	}



	/**  Performs shutdown operations.  */
	public void destroy() {

		ScheduledHarvestManager scheduledHarvestManager =
			(ScheduledHarvestManager) getServletContext().getAttribute("scheduledHarvestManager");
		if(scheduledHarvestManager != null)
			scheduledHarvestManager.stopAllHarvests();
		
		System.out.println(getDateStamp() + " OAIHarvesterServlet stopped");
	}


	//================================================================

	/**
	 *  Handle POST requests.
	 *
	 * @param  req                   DESCRIPTION
	 * @param  resp                  DESCRIPTION
	 * @exception  ServletException  DESCRIPTION
	 * @exception  IOException       DESCRIPTION
	 */
	public void doPost(
	                   HttpServletRequest req,
	                   HttpServletResponse resp)
		 throws ServletException, IOException {
		doGet(req, resp);
	}


	//================================================================

	/**
	 *  Handle PUT requests.
	 *
	 * @param  req                   Input request.
	 * @param  resp                  Resulting response.
	 * @exception  IOException       I/O error
	 * @exception  ServletException  servlet error
	 */
	public void doPut(
	                  HttpServletRequest req,
	                  HttpServletResponse resp)
		 throws ServletException, IOException {
		//badreq( req, resp);
	}


	//================================================================

	/**
	 *  Handle DELETE requests.
	 *
	 * @param  req                   Input request.
	 * @param  resp                  Resulting response.
	 * @exception  IOException       I/O error
	 * @exception  ServletException  servlet error
	 */
	public void doDelete(
	                     HttpServletRequest req,
	                     HttpServletResponse resp)
		 throws ServletException, IOException {
		badreq(req, resp);
	}


	//================================================================

	/**
	 *  Handle GET requests.
	 *
	 * @param  req                   Input request.
	 * @param  resp                  Resulting response.
	 * @exception  IOException       I/O error
	 * @exception  ServletException  servlet error
	 */
	public void doGet(
	                  HttpServletRequest req,
	                  HttpServletResponse resp)
		 throws IOException, ServletException {

		// Set up output writer.
		PrintWriter respwtr = resp.getWriter();
		respwtr.print(" doGet() ");
		respwtr.close();

	}


	// end doGet

	/**
	 *  Override the standard servlet logging to use our logger
	 *
	 * @param  msg  DESCRIPTION
	 */
	public final void log(String msg) {
		prtln(msg);
	}



	/**
	 *  Handle illegal requests.
	 *
	 * @param  req                   Input request.
	 * @param  resp                  Resulting response.
	 * @exception  IOException       I/O error
	 * @exception  ServletException  servlet error
	 */
	private void badreq(
	                    HttpServletRequest req,
	                    HttpServletResponse resp)
		 throws ServletException, IOException {

	}



	/**
	 *  DESCRIPTION
	 *
	 * @param  date  DESCRIPTION
	 * @return       DESCRIPTION
	 */
	private String mkOaiResponseDate(Date date) {
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
		String datestg = df.format(date);
		return datestg;
	}


	//================================================================

	/**
	 *  DESCRIPTION
	 *
	 * @param  date  DESCRIPTION
	 * @return       DESCRIPTION
	 */
	private String mkOaiDatestamp(Date date) {
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
		String datestg = df.format(date);
		return datestg;
	}


	//================================================================


	/**
	 *  Return a string for the current time and date, sutiable for display in log files and
	 *  output to standout:
	 *
	 * @return    The dateStamp value
	 */
	public static String getDateStamp() {
		return
			new SimpleDateFormat("MMM d, yyyy h:mm:ss a zzz").format(new Date());
	}


	/**
	 *  Output a line of text to error out, with datestamp.
	 *
	 * @param  s  The text that will be output to error out.
	 */
	private final void prtlnErr(String s) {
		System.err.println(getDateStamp() + " Harvester - " + s);
	}



	/**
	 *  Output a line of text to standard out, with datestamp, if debug is set to true.
	 *
	 * @param  s  The String that will be output.
	 */
	private final void prtln(String s) {
		if (debug)
			System.out.println(getDateStamp() + " Harvester - " + s);
	}


	/**
	 *  Sets the debug attribute.
	 *
	 * @param  db  The new debug value
	 */
	public final void setDebug(boolean db) {
		debug = db;
	}
}

