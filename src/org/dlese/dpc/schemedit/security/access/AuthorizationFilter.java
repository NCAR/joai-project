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
import java.text.SimpleDateFormat;
import java.io.IOException;
import java.net.URLEncoder;
import java.net.URL;
import javax.servlet.*;
import javax.servlet.http.*;

import javax.security.auth.Subject;
import org.apache.struts.Globals;
import org.apache.struts.action.*;
import org.apache.struts.util.RequestUtils;

import org.dlese.dpc.schemedit.Constants;
import org.dlese.dpc.schemedit.SchemEditUtils;
import org.dlese.dpc.schemedit.security.login.PasswordHelper;
import org.dlese.dpc.schemedit.security.user.User;
import org.dlese.dpc.schemedit.security.user.UserManager;
import org.dlese.dpc.schemedit.security.auth.Auth;
import org.dlese.dpc.schemedit.SessionBean;
import org.dlese.dpc.schemedit.SessionRegistry;

// import javax.servlet.ServletContext;

/**
 *  NOT YET DOCUMENTED
 *
 * @author     Jonathan Ostwald
 * @version    $Id: AuthorizationFilter.java,v 1.18 2010/08/30 19:04:47 ostwald Exp $
 */
public class AuthorizationFilter implements Filter {
	
	private static boolean debug = true;
	private String onErrorUrl;
	private AccessManager accessManager = null;
	private ServletContext servletContext = null;
	private SessionRegistry sessionRegistry = null;
	private boolean authenticationEnabled = true;
	
	/**
	 *  Sets {@link org.dlese.dpc.schemedit.SessionRegistry} attribute from servletContext. NOTE: SessionRegistry is 
	 * instantianted before filters are initialized only because it is registered as a Listener. Otherwise, the SessionRegistry
	 * would not yet be available in the servletContext.
	 *
	 * @param  filterConfig          NOT YET DOCUMENTED
	 * @exception  ServletException  NOT YET DOCUMENTED 
	 */
	public void init(FilterConfig filterConfig)
		 throws ServletException {
		prtln ("init()");
		
		
		servletContext = filterConfig.getServletContext();
		sessionRegistry = (SessionRegistry) servletContext.getAttribute("sessionRegistry");
		if (sessionRegistry == null)
			throw new ServletException ("Authorization Filter could not find \"sessionRegistry\" in servlet context");
		
		onErrorUrl = filterConfig.getInitParameter("onError");
		if (onErrorUrl == null || "".equals(onErrorUrl)) {
			onErrorUrl = "/index.jsp";
		}
		
		String authEnabledParam = (String) servletContext.getInitParameter("authenticationEnabled");
		authenticationEnabled = new Boolean (authEnabledParam);
		servletContext.setAttribute("authenticationEnabled", authenticationEnabled);
		if (!authenticationEnabled)
			prtln ("WARNING: authentication system is NOT enabled");
		else
			prtln ("authentication is enabled");
	}
	
	/**
	 *  This filter gets every page request, and must decide how to handle each one.
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
		boolean autoGuestLoginEnabled = false;
		FilterInfo filterInfo = new FilterInfo (req);

		// we don't want to process webservice requests
		if (isWebserviceRequest (req)) {
			// prtln ("webserviceRequest passed through");
			chain.doFilter(request, response);
			return;
		}
		
		/* bypass when authorization is turned off */
		if (!authenticationEnabled) {
			System.out.println("\n" + getDateStamp() + " AuthorizationFilter: " + req.getRequestURL());
			
			// Debugging
			if (!isBypassRequest (req.getServletPath())) {
				prtln (filterInfo.toString());
			}
			
			chain.doFilter(request, response);
			return;
		}
		

		/*	user is null when either:
			- a user has not yet logged in OR
			- they have just logged out OR
			- the PasswordHelper has not yet been initialized
			autoLogon if autoGuestLoginEnabled is enabled
		*/
			
		if (session.getAttribute("user") == null && autoGuestLoginEnabled) {
			// prtln ("\t auto logging in");
			try {
				autoLogon (session);
			} catch (Exception e) {
				e.printStackTrace();
				throw new ServletException ("Unable to AutoLogin as guest: " + e.getMessage());
			}
		}
		
		User user = (User) session.getAttribute("user");
		ActionErrors errors = new ActionErrors();
		accessManager = AccessManager.getInstance();
		
		if (accessManager == null)
			throw new ServletException ("AuthorizationFilter could not obtain accessManager - check system configuration!");
		
		// pass through requests for selected resources
		if (isBypassRequest (req.getServletPath())) {				
			// prtln ("passing through");
			// req.getRequestDispatcher(req.getServletPath()).forward(req, res);
			chain.doFilter(request, response);
			return;
		}
		
		// log this request
		System.out.println("\n" + getDateStamp() + " AuthorizationFilter: " + req.getRequestURL());
		
		
		// creates session bean if one did not exist.
		SessionBean sessionBean = sessionRegistry.getSessionBean(req);
		Roles.Role requiredRole = null;
		
		GuardedPath gp = accessManager.matchGuardedPath(req.getServletPath());
		if (gp != null) {
			prtln ("guarded path: " + gp.getPath() + "  requiredRole: " + gp.getRole());
			requiredRole = gp.getRole();
		}
		else {
		    prtln ("guarded path NOT found for servletPath: " + req.getServletPath());
		}
		
		if (requiredRole == Roles.NO_ROLE) {
			filterInfo.put ("", "no requiredRole to check: passing on ...");
			prtln (filterInfo.toString());
			chain.doFilter(request, response);
			return;
		}

		// showRequestInfoVerbose (req);
			
		if (user != null) {
		    prtln ("USER is " + user.getUsername() + " (session: " + session.getId() + ")");
			if (user.hasRole( requiredRole )) {
				
				// debugging info
				/* 				
				filterInfo.put ("user", user.getUsername());
				filterInfo.put ("hasRole", requiredRole.toString());
				prtln (filterInfo.toString()); 
				*/
				
				chain.doFilter(request, response);
				return;
			}
		}
		else {
		    prtln ("USER is NULL (session: " + session.getId() + ")");
		}

		errors.add ("error", new ActionError ("error.authentication.required"));
		req.setAttribute(Globals.ERROR_KEY, errors);
		String dispatchUrl = onErrorUrl;
		try {

		    // use just the PATH component of requestURL for dest base
		    String requestUrl = req.getRequestURL().toString();
		    String dest = new URL (requestUrl).getPath();
		    if (dest.indexOf (req.getContextPath()) != 0) {
			throw new Exception ("requestUrl path (" + dest + ") does not begin with \"" + 
					     req.getContextPath());
		    }
		    dest = dest.substring (req.getContextPath().length());
		    // prtln ("dest: " + dest);
			if (req.getQueryString() != null)
				dest += "?" + req.getQueryString();
			dispatchUrl = onErrorUrl + "?dest=" + dest + "&requiredRole=" + requiredRole;
		} catch (Exception e) {
			prtlnErr ("could not compute destination: " + e.getMessage());
		}
		
		// prtln ("contextPath: " + req.getContextPath());
		String redirectUrl = req.getContextPath() + dispatchUrl;
		prtln ("\nREDIRECTING TO " + redirectUrl);
		res.sendRedirect(redirectUrl);

	}

	private boolean isWebserviceRequest (HttpServletRequest request) {
		return (request.getAttribute("webserviceRequest") != null);
	}
	
	/**
	* Checks servlet path for paths that are not subject to authentication.
	*/
	private boolean isBypassRequest (String servletPath) {
		return (servletPath.endsWith (".jpg") ||
				servletPath.endsWith (".gif") ||
				servletPath.endsWith (".js") ||
				servletPath.endsWith (".css"));
	}
	
	private static boolean isActionUri (String uri) {
		return (uri.endsWith (".do"));
	}
	
	private static String getActionUri (String uri) {
		if (!isActionUri (uri))
			return null;
		return uri.substring(0, uri.length()-3);
	}

	private void autoLogon (HttpSession session) throws Exception {
		prtln ("autoLogin");
		UserManager userManager = (UserManager) servletContext.getAttribute("userManager");
		User user = null;
		
		if (userManager == null)
			throw new Exception ("userManager not found in servletContext");

/* 		String username = "guest";
		String passwd = "guest";
		Auth fa = new org.dlese.dpc.schemedit.security.auth.FileAuth(username, passwd);
		if (fa.authenticate()) {
			user = (User) userManager.getUser(username);
			Subject subject = fa.getSubject();
			// setUserRoles (subject, user);
			session.setAttribute(Auth.SUBJECT_SESSION_KEY, subject);
		} */
		
		user = (User) userManager.getUser("guest");
		
		if (user == null) {
			throw new Exception ("Guest user not found in database!");
		}
		
		// Save our logged-in user in the session
		session.setAttribute(Constants.USER_KEY, user);
		
		prtln("LogonAction: User '" + user.getUsername() +
			"' logged on in session " + session.getId());
	}

	
	/**  NOT YET DOCUMENTED */
	public void destroy() {
	}

/* 	private String requestInfo (HttpServletRequest request) {
		String s = "\n Request Info:";
		s += "\n" + "\t requestURI: " + request.getRequestURI();
		s += "\n" + "\t queryString: " + request.getQueryString();
		s += "\n" + "\t servletPath: " + request.getServletPath();
		return (s + "\n");
	} */
	

	private void showRequestInfoVerbose (HttpServletRequest request) {
		prtln ("\n Request Info:");
		prtln ("\t requestURI: " + request.getRequestURI());
		prtln ("\t queryString: " + request.getQueryString());
		prtln ("\t requestURL: " + request.getRequestURL().toString());
		prtln ("\t servletPath: " + request.getServletPath());
		prtln ("\t contextPath: " + request.getContextPath());
		prtln ("\t pathTranslated: " + request.getPathTranslated());
		prtln ("\t pathInfo: " + request.getPathInfo());
		HttpSession session = request.getSession();
		if (session == null)
		    prtln ("Session: NULL");
		else
		    prtln ("Session: " + session.getId());
		prtln ("\nrequest attributes");
		for (Enumeration e=request.getAttributeNames();e.hasMoreElements();) {
			String name = (String)e.nextElement();
			Object attribute = (Object)request.getAttribute(name);
			prtln ("\t name: " + name + "  class: " + attribute.getClass().getName());
		}
		prtln ("------------------------------\n");
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


	static void prtln(String s) {
		if (debug) {
		    SchemEditUtils.prtln (s, "AuthorizationFilter");
		}
	}
	
	static void prtlnErr(String s) {
		System.out.println("AuthorizationFilter: " + s);
	}
	
	class FilterInfo {
		Map map;
		
		FilterInfo (HttpServletRequest request) {
			map = new HashMap();
			this.put ("requestURI", request.getRequestURI());
			this.put ("queryString", request.getQueryString());
			this.put ("servletPath", request.getServletPath());
		}
		
		public void put (String prop, String val) {
			this.map.put(prop, val);
		}
		
		public String get (String prop) {
			return (String)this.map.get (prop);
		}
		
		public String toString () {
			String s = "Filter Info:";
			for (Iterator i=this.map.keySet().iterator();i.hasNext();) {
				String key = (String)i.next();
				if (key.equals("queryString"))
					s += "\n\t" + key + ": " + this.get(key);
			}
			return s;
		}
		
	}
	
}

