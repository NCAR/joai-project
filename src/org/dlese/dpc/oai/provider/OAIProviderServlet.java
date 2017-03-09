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
package org.dlese.dpc.oai.provider;

import org.dlese.dpc.repository.*;
import org.dlese.dpc.repository.action.*;
import org.dlese.dpc.repository.action.form.*;
import org.dlese.dpc.index.reader.*;
import org.dlese.dpc.index.writer.*;
import org.dlese.dpc.index.*;
import org.dlese.dpc.webapps.tools.*;
import org.dlese.dpc.xml.*;
import org.dlese.dpc.action.*;
import org.dlese.dpc.datamgr.*;


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
 *  A servlet used to configure and manage a library repository. The repository may be used for OAI, for
 *  discovery or for cataloging.
 *
 * @author     John Weatherley
 * @version    $Id: OAIProviderServlet.java,v 1.35.2.1 2012/02/15 23:28:03 jweather Exp $
 */
public final class OAIProviderServlet extends HttpServlet {
	private RepositoryManager rm;

	// Caution: ALL class and instance variables are shared.
	// Multiple requests may use this object concurrently.

	private boolean initialized = false;
	private boolean debug = true;

	//private String docRoot = null;

	/**
	 *  Init method called by the web application server upon startup
	 *
	 * @param  conf                  The config
	 * @exception  ServletException  If error
	 */
	public void init(ServletConfig conf)
		 throws ServletException {

		System.out.println(getDateStamp() + " OAIProviderServlet starting");
		String initErrorMsg = "";		
		
		try {
			// super.init must be called before getServletContext().getRealPath("/");
			super.init(conf);
		} catch (Throwable exc) {
			initErrorMsg = "OAIProviderServlet Initialization Error:\n  " + exc;
			prtlnErr(initErrorMsg);
		}

		ServletContext servletContext = getServletContext();

		// Grab the document root for this servlet.
		// Note: this cannot be grabbed before super.init():
		//docRoot = getServletContext().getRealPath("/");

		// Check to see if init has been called previously. If yes, do nothing
		// (This happens when the app is running in a Tomcat environment configured
		// with multiple JVM's using the same Tomcat code base ala Tom Boyd).
		if (initialized) {
			prtlnErr("OAIProviderServlet already initialized. Will not initialize twice.");
			return;
		}

		initialized = true;
		
		
		// Remove any Lucene locks that may have persisted from a previous dirty shut-down:
		String tempDirPath = System.getProperty("java.io.tmpdir");
		if(tempDirPath != null) {
			File tempDir = new File(tempDirPath);
			
			FilenameFilter luceneLockFilter = new FilenameFilter() {
				public boolean accept(File dir, String name) {
					return (name.startsWith("lucene") && name.endsWith(".lock"));
				}
			};
			
			File [] tempFiles = tempDir.listFiles(luceneLockFilter);
			
			if(tempFiles != null) {
				for(int i = 0; i < tempFiles.length; i++) {
					prtlnErr("Removing lucene lock file: " + tempFiles[i].getAbsolutePath());
					tempFiles[i].delete();
				}
			}
		}			
		
		
		String bugs = servletContext.getInitParameter("debug");
		if (bugs != null && bugs.equalsIgnoreCase("true")) {
			debug = true;
			prtln("Outputting debug info");
		}
		else {
			debug = false;
			prtln("Debug info disabled");
		}

		int updateFrequency = 0;
		String fileSyncronizationFrequency = (String) servletContext.getInitParameter("updateFrequency");
		try {
			if (fileSyncronizationFrequency != null) {
				updateFrequency = Integer.parseInt(fileSyncronizationFrequency);
			}
		} catch (Throwable nfe) {
			prtlnErr("Error parsing updateFrequency: " + nfe);
		}

		int maxNumFilesToIndex = 100;
		String val = (String) servletContext.getInitParameter("maxNumFilesToIndex");
		try {
			maxNumFilesToIndex = Integer.parseInt(val);
		} catch (Throwable nfe) {
			prtlnErr("Error reading context parameter maxNumFilesToIndex: " + nfe.getMessage());
		}

		// Set all debugging:
		RepositoryManager.setDebug(debug);
		FileIndexingService.setDebug(debug);
		FileIndexingServiceWriter.setDebug(debug);
		SimpleLuceneIndex.setDebug(debug);
		SimpleQueryAction.setDebug(debug);
		RepositoryForm.setDebug(debug);
		SerializedDataManager.setDebug(debug);
		RepositoryAdminAction.setDebug(debug);
		RepositoryAction.setDebug(debug);
		XMLConversionService.setDebug(debug);
		OAISetsXMLConfigManager.setDebug(debug);
		SimpleFileIndexingObserver.setDebug(debug);

		String repositoryData = servletContext.getInitParameter("repositoryData");
		if (repositoryData == null || repositoryData.length() == 0) {
			initErrorMsg = "OAIProviderServlet context parameter \"repositoryData\" is missing";
			prtlnErr(initErrorMsg);
			throw new ServletException(initErrorMsg);
		}

		// Get the config directory.
		File repositoryConfigDir = null;
		try {
			repositoryConfigDir = new File(GeneralServletTools.getAbsolutePath((String) servletContext.getInitParameter("repositoryConfigDir"), servletContext));
		} catch (Throwable e) {
			prtlnErr("Error getting repositoryConfigDir: " + e);
		}		
		
		File repositoryDataDir = new File(GeneralServletTools.getAbsolutePath(repositoryData, servletContext));
		if (!repositoryDataDir.exists()) {
			prtln("Created directory " + repositoryDataDir.getAbsolutePath());
			repositoryDataDir.mkdir();
		}

		// Launch and init the RepositoryManager, if it has not already been defined by DDSServlet or elsewhere:
		rm = (RepositoryManager) servletContext.getAttribute("repositoryManager");
		if (rm == null) {
			rm =
				new RepositoryManager(repositoryConfigDir, repositoryDataDir.getAbsolutePath(), updateFrequency, maxNumFilesToIndex, false);

			// get additional indexing classes from the servlet config.
			rm.setAdditionalIndices(getFileIndexingClasses()); 

			// Make the RepositoryManager available to the beans that use it:
			if (rm.init(true) == 1) {
				prtln("init() rm");
				servletContext.setAttribute("repositoryManager", rm);
			}
			else {
				initErrorMsg = "OAIProviderServlet:  error initializing the repositoryManager";
				prtlnErr(initErrorMsg);
				rm = null;
				throw new ServletException(initErrorMsg);
			}
		}
		
		// Set the server URL that is used to display the baseURL in OAI responses and elsewhere
		String serverUrl = servletContext.getInitParameter("serverUrl");
		if (serverUrl != null && !serverUrl.equals("[determine-from-client]"))
			rm.setServerUrl(serverUrl);		
		
		// Set up the ending portion of the baseUrl for the data provider:
		String dataProviderBaseUrlPathEnding = servletContext.getInitParameter("dataProviderBaseUrlPathEnding");
		if(dataProviderBaseUrlPathEnding != null)
			rm.setProviderBaseUrlEnding(dataProviderBaseUrlPathEnding);

		// Create a WebLog Index
		SimpleLuceneIndex webLogIndex =
			new SimpleLuceneIndex(repositoryDataDir.getAbsolutePath() + "/web_log_index");
		webLogIndex.setOperator(SimpleLuceneIndex.DEFAULT_AND);
		//webLogIndex.setDebug(debug);

		// Set up an XMLConversionService for use in OAI and elsewhere:
		if (rm.getXMLConversionService() == null) {
			File xslFilesDirecoryPath = new File(GeneralServletTools.getAbsolutePath(
				"WEB-INF/xsl_files/", getServletContext()));
			File xmlCachDir = new File(GeneralServletTools.getAbsolutePath(
				repositoryData + File.separator + "converted_xml_cache", getServletContext()));

			try {
				XMLConversionService xmlConversionService =
					XMLConversionService.xmlConversionServiceFactoryForServlets(getServletContext(), xslFilesDirecoryPath, xmlCachDir, true);
				rm.setXMLConversionService(xmlConversionService);
			} catch (Throwable t) {
				prtlnErr("ERROR: Unable to initialize xmlConversionService: " + t);
			}
		}

		// Add default xmlformat schema and namespaces:
		Enumeration enumeration = getInitParameterNames();
		String param;
		while (enumeration.hasMoreElements()) {
			param = (String) enumeration.nextElement();
			if (param.toLowerCase().startsWith("xmlformatinfo")) {
				try {
					rm.setDefaultXmlFormatInfo(getInitParameter(param));
				} catch (Throwable t) {
					initErrorMsg = "Error reading init param for xmlformatinfo: " + t;
					prtlnErr(initErrorMsg);
				}
			}
		}

		// Make context scope objects available to our beans:
		getServletContext().setAttribute("index", rm.getIndex());
		getServletContext().setAttribute("webLogIndex", webLogIndex);
		
		
		// For testing:
		try{
			//rm.setOaiFilterQuery("ocean OR title:(ocean) OR stems:(ocean)");
		} catch (Exception e){}
		
		System.out.println(getDateStamp() + " OAIProviderServlet initialized.");
	}


	/**  Performs shutdown operations. */
	public void destroy() {
		rm.destroy();
		// shutdown webLogIndex Thread to avoid zombies		
		SimpleLuceneIndex webLogIndex = (SimpleLuceneIndex)getServletContext().getAttribute("webLogIndex");
		if(webLogIndex!= null) {
			try {
				webLogIndex.stopIndexing();
			} catch (Exception e) {
			}
		}


		System.out.println(getDateStamp() + " OAIProviderServlet stopped");
	}

	/**
	 *  Returns the fileIndexingClasses found in the servlet config.
	 * @return
	 */
	private Hashtable getFileIndexingClasses() {

		Hashtable ht = new Hashtable();

		try {
			// Add configured FileIndexingPlugins:
			Enumeration enumeration = getInitParameterNames();
			String param;
			while (enumeration.hasMoreElements()) {
				param = (String) enumeration.nextElement();

				if (param.toLowerCase().startsWith("fileindexingclass")) {
					String paramVal = getInitParameter(param);
					String[] vals = paramVal.split("\\|");
					if (vals.length != 2 && vals.length != 1) {
						prtlnErr("Error: setFileIndexingClasses(): could not parse parameter '" + paramVal + "'");
						continue;
					}
					String format = vals[0].trim();
					try {
						Class myclass = Class.forName(vals[1].trim());
						prtln("custom indexing class "+myclass.getName()+" created for "+format);
						ht.put(format, myclass);
					} catch (Throwable e) {
						prtlnErr("Error: setFileIndexingClasses(): could not find class '" + vals[1].trim() + "'. for format "+format+ " " + e);
						e.printStackTrace();
						continue;
					}
				}
			}
		} catch (Throwable e) {
			String initErrorMsg = "Error: setFileIndexingClasses(): " + e;
			prtlnErr(initErrorMsg);
		}

		return ht;

	}

	//================================================================

	/**
	 *  Handle POST requests.
	 *
	 * @param  req                   Request
	 * @param  resp                  Response
	 * @exception  ServletException  If error
	 * @exception  IOException       If error
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
	 *  Return a string for the current time and date, sutiable for display in log files and output to standout:
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
		System.err.println(getDateStamp() + " OAIProviderServlet: " + s);
	}



	/**
	 *  Output a line of text to standard out, with datestamp, if debug is set to true.
	 *
	 * @param  s  The String that will be output.
	 */
	private final void prtln(String s) {
		if (debug)
			System.out.println(getDateStamp() + " OAIProviderServlet: " + s);
	}


	/**
	 *  Sets the debug attribute of the DocumentService object
	 *
	 * @param  db  The new debug value
	 */
	public final void setDebug(boolean db) {
		debug = db;
	}
}

