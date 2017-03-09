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
package org.dlese.dpc.ndr;

import org.dlese.dpc.ndr.request.NdrRequest;
import org.dlese.dpc.ndr.request.SimpleNdrRequest;

import org.dlese.dpc.ndr.apiproxy.NDRAPIProxy;
import org.dlese.dpc.ndr.apiproxy.NDRConstants;
import org.dlese.dpc.ndr.toolkit.MimeTypes;
import org.dom4j.Document;

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
 *  Servlet responsible for initializing the NDR proxy and placing it in the servlet Context. 
 *
 *@author    Jonathan Ostwald <p>
 *
 *      $Id: NDRServlet.java,v 1.20 2010/05/26 21:13:20 ostwald Exp $
 */

public final class NDRServlet extends HttpServlet {

	private boolean debug = true;

	/**
	 *  Make sure the repository exists and there is a master collection record before the repository manager is
	 initilized in the DDSServlet.
	 *
	 *@param  config                Description of the Parameter
	 *@exception  ServletException  Description of the Exception
	 */
	public void init(ServletConfig config)
		throws ServletException {
		System.out.println(getDateStamp() + " NDRServlet starting");
		String initErrorMsg = "";
		try {
			super.init(config);
		} catch (Throwable exc) {
			initErrorMsg = "NDRServlet Initialization Error:\n  " + exc;
			prtlnErr(initErrorMsg);
		}
		
		ServletContext servletContext = getServletContext();

		// showContextParams (); 
		
		try {
		
			// if ndrServiceEnabled init param is not "true", then exit
			String ndrServiceEnabled = (String) servletContext.getInitParameter("ndrServiceEnabled");
			if (ndrServiceEnabled == null || !ndrServiceEnabled.equals("true")) {
				throw new Exception ("ndrServiceEnabled was not configured as \"true\"");
			}
			
			prtln ("SimpleNdrRequest.verbose is " + SimpleNdrRequest.getVerbose());
			
			// initialize ndrApiBaseUrl
			String ndrApiBaseUrl = (String) servletContext.getInitParameter("ndrApiBaseUrl");
			if (ndrApiBaseUrl == null) {
				throw new Exception ("ndrApiBaseUrl init parameter not found");
			}
			/* 
			servletContext.setAttribute ("ndrServer", ndrServer); 
			prtln ("ndrServer set as context attribute - " + ndrServer);
			*/
			NDRConstants.setNdrApiBaseUrl (ndrApiBaseUrl);
			prtln ("ndrApiBaseUrl assigned in NDRConstants: " + ndrApiBaseUrl);
			
			// initialize ncsAgentHandle
			String ncsAgentHandle = (String) servletContext.getInitParameter("ncsAgentHandle");
			if (ncsAgentHandle == null)
				throw new Exception ("ncsAgentHandle init parameter not found");
			NDRConstants.setNcsAgent (ncsAgentHandle);
			prtln ("ncsAgentHandle assigned in NDRConstants: " + NDRConstants.getNcsAgent());
			
			// initialize ncsAgentPrivateKey
			String ncsAgentPrivateKey = getAbsolutePath((String) servletContext.getInitParameter("ncsAgentPrivateKey"));
			if (ncsAgentPrivateKey == null) {
				throw new Exception ("ncsAgentPrivateKey init param not found!");
			}
			else {
				File keyFile = new File (ncsAgentPrivateKey);
				if (!keyFile.exists() || !keyFile.isFile())  {
					throw new Exception ("ncsAgentPrivateKey file does not exist at " + ncsAgentPrivateKey);
				}
				else {
					NDRConstants.setPrivateKeyFile(keyFile);
					prtln ("keyFile assigned in NDRConstants: " + NDRConstants.getPrivateKeyFile());
				}
			}
			
			NDRConstants.setMasterAgent (
				(String) servletContext.getInitParameter("ndrMasterAgent"));
			NDRConstants.setMasterCollection (
				(String) servletContext.getInitParameter("ndrMasterCollection"));			
			
			servletContext.setAttribute ("ndrServiceEnabled", true);
			servletContext.setAttribute ("ndrServiceActive", true);
			System.out.println(getDateStamp() + " NDRServlet initialized.\n");
		} catch (Throwable t) {
			// there has been a fatal error and NDRService is not initilized
			// prtlnErr ("WARNING: NDR service is not enabled");
			NDRConstants.setNdrApiBaseUrl (null);
			servletContext.setAttribute ("ndrServiceEnabled", false);
			String errorMsg = "";
			if (t instanceof Exception) {
				errorMsg = t.getMessage();
			}
			else {
				t.printStackTrace();
				errorMsg = "reason unknown";
			}
			// throw new ServletException ("NDRServlet not enabled: " + errorMsg);
			prtlnErr ("NDRServlet not enabled: " + errorMsg);
		}
			
	}

	/**
	 *  Performs shutdown operations.
	 */
	public void destroy() {
		prtln("destroy() ...");
		System.out.println(getDateStamp() + " NDRServlet stopped");
	}

	/**
	 *  Return a string for the current time and date, sutiable for display in log
	 *  files and output to standout:
	 *
	 *@return    The dateStamp value
	 */
	public static String getDateStamp() {
		return
			new SimpleDateFormat("MMM d, yyyy h:mm:ss a zzz").format(new Date());
	}


	/**
	 *  Output a line of text to error out, with datestamp.
	 *
	 *@param  s  The text that will be output to error out.
	 */
	private final void prtlnErr(String s) {
		System.err.println(getDateStamp() + " NDRServlet: " + s);
	}


	/**
	 *  Output a line of text to standard out, with datestamp, if debug is set to
	 *  true.
	 *
	 *@param  s  The String that will be output.
	 */
	private final void prtln(String s) {
		if (debug) {
			System.out.println(getDateStamp() + " NDRServlet: " + s);
		}
	}

	/**
	 *  Gets the absolute path to a given file or directory. Assumes the path
	 *  passed in is eithr already absolute (has leading slash) or is relative to
	 *  the context root (no leading slash). If the string passed in does not begin
	 *  with a slash ("/"), then the string is converted. For example, an init
	 *  parameter to a config file might be passed in as
	 *  "WEB-INF/conf/serverParms.conf" and this method will return the
	 *  corresponding absolute path "/export/devel/tomcat/webapps/myApp/WEB-INF/conf/serverParms.conf."

	 *  Note: the super.init() method must be called prior to using this method,
	 *  else a ServletException is thrown.
	 *
	 *@param  fname                 An absolute or relative file name or path
	 *      (relative the the context root).
	 *@return                       The absolute path to the given file or path.
	 *@exception  ServletException  An exception related to this servlet
	 */
	private String getAbsolutePath(String fname)
		throws ServletException {
		if (fname == null) {
			return null;
		}
		
		String docRoot = getServletContext().getRealPath("/");
		String fullname;

		if (fname.startsWith("/") ||
			fname.matches("[A-Z]:.+") ||
			fname.startsWith(docRoot) ||
			fname.startsWith("../") ||
			fname.substring(1, 3).equals(":\\")) {
			// Check for Windows paths
			fullname = fname;
		}
		else {
			if (docRoot.endsWith("/"))
				fullname = docRoot + fname;
			else
				fullname = docRoot + "/" + fname;
		}

		return fullname;
	}
	
	/**
	* debugging method to show config and context params
	*/
	private void showContextParams () {
		
		prtln ("\ninit parameters from CONFIG");
		Enumeration e1 = this.getServletConfig().getInitParameterNames();
		while (e1.hasMoreElements()) {
			prtln ("\t" + (String)e1.nextElement());
		}
		
		prtln ("\ninit parameters from CONTEXT");
		Enumeration e2 = this.getServletContext().getInitParameterNames();
		while (e2.hasMoreElements()) {
			prtln ("\t" + (String)e2.nextElement());
		} 
	}

	
}

