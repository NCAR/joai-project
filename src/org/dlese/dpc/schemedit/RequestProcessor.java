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

import org.dlese.dpc.webapps.tools.GeneralServletTools;

import java.io.*;
import java.util.*;
import java.text.SimpleDateFormat;
import java.net.URL;

// Enterprise imports
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;

/**
 *  My first cut at a RequestProcessor.
 *
 * @author     Jonathan Ostwald <p>
 *
 *      $Id: RequestProcessor.java,v 1.16 2010/03/03 17:15:43 ostwald Exp $
 * @version    $Id: RequestProcessor.java,v 1.16 2010/03/03 17:15:43 ostwald Exp $
 */

public final class RequestProcessor extends org.apache.struts.action.RequestProcessor {

	private boolean debug = true;


	/**
	 *  Override process method to catch the infamous BeanUtils.populate exception, which is thrown when a
	 *  session has timed out. The problem is that we can't return a forward from here, so we probably have to
	 *  create a new exception and catch it elsewhere (but where?).
	 *
	 * @param  request               Description of the Parameter
	 * @param  response              Description of the Parameter
	 * @exception  ServletException  Description of the Exception
	 * @exception  IOException       Description of the Exception
	 */
	public void process(HttpServletRequest request, HttpServletResponse response)
		 throws ServletException, IOException {
		// prtln("PROCESS");

		/*
			here is the place (before the request is acted upon by any other class)
			to explicitly set the encoding of the request. The is necessary ONLY if
			the content-type/charset of the page containing a form has been set to UTF:
				<%@ page contentType="text/html; charset=UTF-8" %>
			In this case, the input from the form (i.e., the request params) are garbled
			if the request encoding is not also set to UTF-8 as below.
			NOTE: if the content-type/charset is NOT explicitly set, then setting the request
			encoding to UTF-8 as below will also garble the input!!
		*/
		// set request encoding to UTF-8
		try {
			request.setCharacterEncoding("UTF-8");
		} catch (Throwable t) {
			t.printStackTrace();
		}
		// prtln ("request encoding: " + request.getCharacterEncoding());

		try {
			super.process(request, response);
		} catch (ServletException e) {
			prtln("process caught exception: " + e.getMessage());
			if ("BeanUtils.populate".equals(e.getMessage())) {
				handleBeanUtilsPopulateError(request, response);
			}
			else {
				e.printStackTrace();
				throw e;
			}
		}
	}


	/**
	 *  Gets the requestUrl attribute of the RequestProcessor object
	 *
	 * @param  request  NOT YET DOCUMENTED
	 * @return          The requestUrl value
	 */
	private String getRequestUrl(HttpServletRequest request) {
		StringBuffer requestURL = request.getRequestURL();
		if (requestURL != null)
			return requestURL.toString();
		else
			return "";
	}


	/**
	 *  Handle bean population errors that are not crucial to flow of control. For example, if we have a
	 *  beanUtils.populate exception when the user is trying to navigate somewhere outside of the editor, then
	 *  let the navication happen without throwing an exception. Otherwise test for timeout and present message,
	 *  else, assume its a "back button" problem and show message.
	 *
	 * @param  request               NOT YET DOCUMENTED
	 * @param  response              NOT YET DOCUMENTED
	 * @exception  ServletException  NOT YET DOCUMENTED
	 * @exception  IOException       NOT YET DOCUMENTED
	 */
	private void handleBeanUtilsPopulateError(HttpServletRequest request, HttpServletResponse response)
		 throws ServletException, IOException {
		prtln("handling BeanUtils Populate Error");

		SessionRegistry sessionRegistry =
			(SessionRegistry) servlet.getServletContext().getAttribute("sessionRegistry");
		SessionBean sessionBean = (sessionRegistry != null) ? sessionRegistry.getSessionBean(request) : null;

		String requestUrl = getRequestUrl(request);
		String contextPath = request.getContextPath();

		// default destination
		String destination = response.encodeURL(contextPath + "/populate_error.jsp");

		// has the session timed out?
		if (sessionBean != null && sessionBean.getNumSecsToTimeout() < 1) {
			destination = response.encodeURL(contextPath + "/session_timed_out.jsp");
		}
		else {
			// prtln("NOT TIMED OUT!");
			/*
 			prtln("request parameters");
			for (Enumeration i = request.getParameterNames(); i.hasMoreElements(); ) {
				prtln("\t" + (String) i.nextElement());
			}
			*/
			String command = request.getParameter("command");

			if (command != null && command.equals("exit")) {
				String pathArg = request.getParameter("pathArg");
				prtln("processing a \"guardedExit\" command");
				prtln("\t command: " + command);
				prtln("\t pathArg: " + pathArg);
				prtln ("\t contextPath: " + contextPath);
				if (pathArg != null && pathArg.equals("forwardToCaller")) {
					ActionForward forward = SchemEditUtils.forwardToCaller(request, request.getParameter("recId"), sessionBean);
					destination = contextPath + forward.getPath();
				}
				else {
					if (pathArg.startsWith("http://"))
						destination = response.encodeURL(pathArg);
					else
						destination = response.encodeURL(contextPath + pathArg);
				}
			}
		}
		prtln("destination: " + destination);
		response.sendRedirect(destination);
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  request               NOT YET DOCUMENTED
	 * @param  response              NOT YET DOCUMENTED
	 * @param  form                  NOT YET DOCUMENTED
	 * @param  mapping               NOT YET DOCUMENTED
	 * @exception  ServletException  NOT YET DOCUMENTED
	 */
	protected void processPopulate(HttpServletRequest request, HttpServletResponse response, ActionForm form, ActionMapping mapping)
		 throws ServletException {
		// can we trap BeanUtils.populate exceptions here?
		// but is there any advantage to worrying about it here as opposed to in process()?

		boolean throwExceptionUponPopulateError = true;
		try {
			super.processPopulate(request, response, form, mapping);
		} catch (Exception e) {
			prtln("processPopulate ERROR: " + e.getMessage());
			if (throwExceptionUponPopulateError) {
				prtln("Throwing ServletException ...");
				throw new ServletException("BeanUtils.populate");
			}
			else {
				prtln("Not throwing exception for now but instead printing stack trace...");
				e.printStackTrace();
			}

		}
	}

	/**
	* Since we do our roles processing in the AuthenticationFilter, the only job to do here is to 
	* create a sessionBean when necessary.
	*/
	protected boolean processRoles (HttpServletRequest request, HttpServletResponse response, ActionMapping mapping)
		 throws ServletException, IOException {
		// prtln("PROCESS ROLES");

		String logMsg = "";
		
/* 		SessionRegistry sessionRegistry =
			(SessionRegistry) servlet.getServletContext().getAttribute("sessionRegistry");
		// creates session bean if one did not exist.
		SessionBean sessionBean = sessionRegistry.getSessionBean(request); */

		return true;
	}

	// print out the roles known to "mapping"
	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  mapping  NOT YET DOCUMENTED
	 */
	private void showRoleNames(ActionMapping mapping) {
		String roles[] = mapping.getRoleNames();
		if (roles != null && roles.length > 0) {
			prtln("Roles");
			for (int i = 0; i < roles.length; i++) {
				prtln("\t" + roles[i]);
			}
		}
		else
			prtln("no roles defined");
	}


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
	 *  Output a line of text to standard out, with datestamp, if debug is set to true.
	 *
	 * @param  s  The String that will be output.
	 */
	private final void prtln(String s) {
		if (debug) {
			System.out.println(getDateStamp() + " DCS RequestProcessor: " + s);
		}
	}

	/**
	 *  Description of the Method
	 *
	 * @param  request               Description of the Parameter
	 * @param  response              Description of the Parameter
	 * @param  mapping               Description of the Parameter
	 * @return                       Description of the Return Value
	 * @exception  ServletException  Description of the Exception
	 * @exception  IOException       Description of the Exception
	 */
	
}

