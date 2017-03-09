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
package org.dlese.dpc.standards.commcore;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.dlese.dpc.webapps.tools.GeneralServletTools;

import java.io.*;
import java.util.*;
import java.text.SimpleDateFormat;

// Enterprise imports
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

/**
 *  Servlet responsible for initializing the StandardsSuggestionService.<p>
 *
 *  CURRENTLY NOT ACTIVE, since we do not actually use StandardsSuggestionService
 *  for Skeletal implementation. To use, uncomment the servlet tag in
 *  WEB-INF/web.xml.
 *
 * @author     Jonathan Ostwald
 */

public final class CommCoreServlet extends HttpServlet {

	private boolean debug = true;

	private static Log log = LogFactory.getLog(CommCoreServlet.class);


	/**
	 *  Intialize the StandardsSuggestionService and place it in the ServletContext
	 *  where it can be found by the Rest Service Clases.
	 *
	 * @param  config                Description of the Parameter
	 * @exception  ServletException  Description of the Exception
	 */
	public void init(ServletConfig config)
		 throws ServletException {
		log.info (getDateStamp() + " CommCoreServlet starting");
		String initErrorMsg = "";
		try {
			super.init(config);
		} catch (Throwable exc) {
			initErrorMsg = "CommCoreServlet Initialization Error:\n  " + exc;
			log.error (initErrorMsg);
		}

		// initialize the AdnStandardsManager
		String commCoreStandardsPath = getAbsolutePath ((String)getServletContext().getInitParameter("commCoreStandardsPath"));
		if (commCoreStandardsPath == null) {
			throw new ServletException("init parameter \"commCoreStandardsPath\" not found in servlet context");
		}
		CommCoreServiceHelper commCoreServiceHelper = null;
		try {
			commCoreServiceHelper = new CommCoreServiceHelper(commCoreStandardsPath); 
		} catch (Exception e) {
			log.error("Failed to initialize StdDocument.", e);
		}

		if (commCoreServiceHelper != null)
			getServletContext().setAttribute("commCoreServiceHelper", commCoreServiceHelper);
		else
			throw new ServletException ("Failed to initialize StdDocument");

		log.info (getDateStamp() + " CommCoreServlet completed.\n");
	}


	/**  Performs shutdown operations. */
	public void destroy() {
		log.info ("destroy() ...");
		System.out.println(getDateStamp() + " CommCoreServlet stopped");
	}


	/**
	 *  Return a string for the current time and date, sutiable for display in log
	 *  files and output to standout:
	 *
	 * @return    The dateStamp value
	 */
	public static String getDateStamp() {
		return
			new SimpleDateFormat("MMM d, yyyy h:mm:ss a zzz").format(new Date());
	}
	
	/**
	 *  Gets the absolute path to a given file or directory. Assumes the path
	 *  passed in is eithr already absolute (has leading slash) or is relative to
	 *  the context root (no leading slash). If the string passed in does not begin
	 *  with a slash ("/"), then the string is converted. For example, an init
	 *  parameter to a config file might be passed in as "WEB-INF/conf/serverParms.conf"
	 *  and this method will return the corresponding absolute path "/export/devel/tomcat/webapps/myApp/WEB-INF/conf/serverParms.conf."
	 *  <p>
	 *
	 *  If the string that is passed in already begings with "/", nothing is done.
	 *  <p>
	 *
	 *  Note: the super.init() method must be called prior to using this method,
	 *  else a ServletException is thrown.
	 *
	 * @param  fname                 An absolute or relative file name or path
	 *      (relative the the context root).
	 * @return                       The absolute path to the given file or path.
	 * @exception  ServletException  An exception related to this servlet
	 */
	private String getAbsolutePath(String fname)
		 throws ServletException {
		if (fname == null) {
			return null;
		}
		return GeneralServletTools.getAbsolutePath(fname, getServletContext());
	}


}

