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
package org.dlese.dpc.schemedit.security.access;

import java.util.*;
import java.io.IOException;
import java.net.URLEncoder;
import javax.servlet.*;
import javax.servlet.http.*;
import org.apache.struts.Globals;
import org.apache.struts.action.*;
import org.apache.struts.util.RequestUtils;


/**
 *  NOT YET DOCUMENTED
 *
 * @author     Jonathan Ostwald
 * @version    $Id: WebServicesFilter.java,v 1.6 2009/03/20 23:33:57 jweather Exp $
 */
public class WebServicesFilter implements Filter {
	
	private static boolean debug = false;

	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  filterConfig          NOT YET DOCUMENTED
	 * @exception  ServletException  NOT YET DOCUMENTED
	 */
	public void init(FilterConfig filterConfig)
		 throws ServletException {
		prtln ("init()");
	}

	/**
	 *  This filter gets every webservices request, and simply passes it on without authorizing.
	 *
	 * @param  request               NOT YET DOCUMENTED
	 * @param  response              NOT YET DOCUMENTED
	 * @param  chain                 NOT YET DOCUMENTED
	 * @exception  IOException       NOT YET DOCUMENTED
	 * @exception  ServletException  NOT YET DOCUMENTED
	 */
	public void doFilter(ServletRequest request,
	                     ServletResponse response,
	                     FilterChain chain)
		 throws IOException, ServletException {
			 
		// prtln ("\n---------------------\ndoFilter()");
		HttpServletRequest req = (HttpServletRequest) request;
		HttpServletResponse res = (HttpServletResponse) response;
		HttpSession session = req.getSession();
		
		ActionErrors errors = new ActionErrors();

		
		showRequestInfo(req);
		
		req.setAttribute ("webserviceRequest", "true");
		
		chain.doFilter(request, response);
	
	}

	private void showRequestInfo (HttpServletRequest request) {
		String s = "\n Request Info:";
		s += "\n" + "\t requestURI: " + request.getRequestURI();
		s += "\n" + "\t queryString: " + request.getQueryString();
		s += "\n" + "\t servletPath: " + request.getServletPath();
		prtln (s + "\n");
	}
	
	/**  NOT YET DOCUMENTED */
	public void destroy() {
	}
	
	private void showRequestInfoVerbose (HttpServletRequest request) {
		prtln ("\n Request Info:");
		prtln ("\t requestURI: " + request.getRequestURI());
		prtln ("\t queryString: " + request.getQueryString());
		prtln ("\t requestURL: " + request.getRequestURL().toString());
		prtln ("\t servletPath: " + request.getServletPath());
		prtln ("\t contextPath: " + request.getContextPath());
		prtln ("\t pathTranslated: " + request.getPathTranslated());
		prtln ("\t pathInfo: " + request.getPathInfo());
		prtln ("\nrequest attributes");
		for (Enumeration e=request.getAttributeNames();e.hasMoreElements();) {
			String name = (String)e.nextElement();
			Object attribute = (Object)request.getAttribute(name);
			prtln ("\t name: " + name + "  class: " + attribute.getClass().getName());
		}
		prtln ("------------------------------\n");
	}

	
	static void prtln(String s) {
		if (debug) {
			while (s.length() > 0 && s.charAt(0) == '\n') {
				System.out.println ("");
				s = s.substring(1);
			}
			System.out.println("WebServicesFilter: " + s);
		}
	}
	
}

