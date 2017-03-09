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
package org.dlese.dpc.schemedit;

import java.io.*;
import java.text.*;
import java.util.*;

// Enterprise imports
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

import org.apache.struts.config.ModuleConfig;
import org.apache.struts.config.ActionConfig;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.Globals;

import org.dlese.dpc.schemedit.security.access.AccessManager;
import org.dlese.dpc.schemedit.security.user.UserManager;
import org.dlese.dpc.schemedit.security.user.User;
import org.dlese.dpc.schemedit.security.user.UserdataConverter;
import org.dlese.dpc.webapps.tools.GeneralServletTools;

/**
 *  ActionServlet 
 
 *
 *@author    Jonathan Ostwald <p>
 *
 *      $Id: ActionServlet.java,v 1.8 2009/07/07 02:59:59 ostwald Exp $
 */

public final class ActionServlet extends org.apache.struts.action.ActionServlet {

	private boolean debug = true;

	public void init() throws ServletException {
		super.init();

		String dcsConfig = getAbsolutePath((String) getServletContext().getInitParameter("dcsConfig"));
		
		AccessManager accessManager = null;
		try {
			File security_config = new File (dcsConfig, "auth/access.xml");
			prtln ("configuring AccessManager from " + security_config);
			accessManager = AccessManager.getInstance (security_config, getActionMappings());
		} catch (Exception e) {
			prtln ("Unable to instantiate AccessManager: " + e.getMessage());
		}
		getServletContext().setAttribute("accessManager", accessManager);
		prtln ("accessManager placed in servlet context");
		
		UserManager userManager = null;
		try {
			File oldUserData = new File (dcsConfig, "auth/users.xml");
			if (oldUserData.exists())
				UserdataConverter.convert (oldUserData);
			userManager = new UserManager (new File (dcsConfig, "users"));
		} catch (Throwable e) {
			prtln ("Unable to instantiate UserManager: " + e.getMessage());
			e.printStackTrace();
		}
		getServletContext().setAttribute("userManager", userManager);
		prtln ("userManager placed in servlet context");
		
		User guestUser = userManager.getUser("guest");
		if (guestUser != null) {
			getServletContext().setAttribute("guestUser", guestUser);
			prtln ("guest user set in servletContext");
		}
		
		
		prtln ("\nActionServlet initialized\n");
	}
	
	
	public List getActionMappings () {
		List mappings = new ArrayList();
		ModuleConfig moduleConfig = (ModuleConfig)
                getServletContext().getAttribute(Globals.MODULE_KEY);
        ActionConfig[] actionConfigs = moduleConfig.findActionConfigs();
		for (int i=0;i<actionConfigs.length;i++)
			mappings.add ((ActionMapping)actionConfigs[i]);
        return mappings;
	}
	
	protected void process (HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// prtln ("PROCESS");
		super.process (request, response);
	}
	
	/**
	 *  Standard doPost method forwards to doGet
	 *
	 * @param  request
	 * @param  response
	 * @exception  ServletException
	 * @exception  IOException
	 */
	public void doPost(HttpServletRequest request, HttpServletResponse response)
		 throws ServletException, IOException {
		doGet(request, response);
	}


	/**
	 *  The standard required servlet method, just parses the request header for known parameters. The <code>doPost</code>
	 *  method just calls this one. See {@link HttpServlet} for details.
	 *
	 * @param  request
	 * @param  response
	 * @exception  ServletException
	 * @exception  IOException
	 */
	public void doGet(HttpServletRequest request, HttpServletResponse response)
		 throws ServletException, IOException {
			 // prtln ("doGet!");
			 super.doGet(request, response);
	}


	//================================================================
	// stuff swiped from OAIProviderServlet


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

	private String getAbsolutePath(String fname)
		throws ServletException {
		if (fname == null) {
			return null;
		}
		return GeneralServletTools.getAbsolutePath(fname, getServletContext());
	}
	
	/**
	 *  Output a line of text to error out, with datestamp.
	 *
	 *@param  s  The text that will be output to error out.
	 */
	private final void prtlnErr(String s) {
		System.err.println(getDateStamp() + " ActionServlet: " + s);
	}


	/**
	 *  Output a line of text to standard out, with datestamp, if debug is set to
	 *  true.
	 *
	 *@param  s  The String that will be output.
	 */
	private final static void prtln(String s) {
		while (s.length() > 0 && s.charAt(0) == '\n') {
			System.out.println ("");
			s = s.substring(1);
		}
		System.out.println("ActionServlet: " + s);
	}

}

