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
package org.dlese.dpc.schemedit.security.action;

import java.io.IOException;
import java.util.Hashtable;
import java.util.Locale;
import java.util.Set;
import java.util.Iterator;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionError;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionServlet;
import org.apache.struts.util.MessageResources;

import javax.security.auth.Subject;

import org.dlese.dpc.schemedit.SchemEditUtils;

import org.dlese.dpc.schemedit.Constants;
import org.dlese.dpc.schemedit.security.user.User;
import org.dlese.dpc.schemedit.security.access.Roles;
import org.dlese.dpc.schemedit.security.auth.SchemEditAuth;
import org.dlese.dpc.schemedit.security.auth.AuthUtils;
import org.dlese.dpc.schemedit.security.user.UserManager;

import org.dlese.dpc.schemedit.security.action.form.LogonForm;

/**
 *  Implementation of <strong>Action</strong> that validates a user logon.
 *
 * @author    Jonathan Ostwald
 */

public final class LogonAction extends Action {

	private static boolean debug = true;

	// --------------------------------------------------------- Public Methods

	/**
	 *  Process the specified HTTP request, and create the corresponding HTTP
	 *  response (or forward to another web component that will create it). Return
	 *  an <code>ActionForward</code> instance describing where and how control
	 *  should be forwarded, or <code>null</code> if the response has already been
	 *  completed.
	 *
	 * @param  mapping               The ActionMapping used to select this instance
	 * @param  request               The HTTP request we are processing
	 * @param  response              The HTTP response we are creating
	 * @param  form                  NOT YET DOCUMENTED
	 * @return                       NOT YET DOCUMENTED
	 * @exception  IOException       if an input/output error occurs
	 * @exception  ServletException  if a servlet exception occurs
	 */
	public ActionForward execute(ActionMapping mapping,
	                             ActionForm form,
	                             HttpServletRequest request,
	                             HttpServletResponse response)
		 throws IOException, ServletException {

		// Extract attributes we will need
		Locale locale = getLocale(request);
		MessageResources messages = getResources(request);
		User user = null;

		// Validate the request parameters specified by the user
		ActionErrors errors = new ActionErrors();
		String username = ((LogonForm) form).getUsername();
		String password = ((LogonForm) form).getPassword();
		String dest = ((LogonForm) form).getDest();

		// NOTE: don't show request params in output!!
		// SchemEditUtils.showRequestParameters(request);

		try {
		
			UserManager userManager =
				(UserManager) servlet.getServletContext().getAttribute("userManager");
			if (userManager == null) {
				errors.add("error",
					new ActionError("error.database.missing"));
			}
			else if ("guest".equals(username) && "guest".equals(password)) {
				// GUEST user does not have to be authorized
				user = (User) userManager.getUser("guest");
				if (user == null) {
					errors.add("error",
						new ActionError("generic.error",
						"Guest user account is not activated for this app"));
				}
			}
			else {

				SchemEditAuth auth = new SchemEditAuth(username, password);
				if (auth.authenticate()) {
					// prtln ("authenticated");
					user = (User) userManager.getUser(username);
					Subject subject = auth.getSubject();
					
					HttpSession sess = request.getSession();
					sess.setAttribute(SchemEditAuth.SUBJECT_SESSION_KEY, subject);
					AuthUtils.showSubject(subject, "authenticated subject");
				}
	
				if (user == null) {
					prtln("Could not authenticate user for username: \"" + username + "\"");
					errors.add("error",
						new ActionError("error.password.mismatch"));
				}
			}
	
			// Report any errors we have discovered back to the original form
			if (!errors.isEmpty()) {
				saveErrors(request, errors);
				return mapping.getInputForward();
			}
	
			// Save our logged-in user in the session
			HttpSession session = request.getSession();
			session.setAttribute(Constants.USER_KEY, user);
	
			prtln("User '" + user.getUsername() +
				"' logged on in session " + session.getId());
	
			// Remove the obsolete form bean
			if (mapping.getAttribute() != null) {
				if ("request".equals(mapping.getScope()))
					request.removeAttribute(mapping.getAttribute());
				else
					session.removeAttribute(mapping.getAttribute());
			}
	
			// Forward control to the specified success URI
			// redirect so browser knows we aren't in auth anymore
			if (dest != null && dest.trim().length() > 0) {
				// prtln ("redirecting to " + dest);
				return new ActionForward(dest, true);
			}
			else {
				// prtln ("forwarding to dcs.collections");
				return (mapping.findForward("dcs.collections"));
			}
		} catch (Throwable t) {
			prtln ("ERROR: " + t.getMessage());
			t.printStackTrace();
			throw new ServletException (t);
		}
	}


	static void prtln(String s) {
		while (s.length() > 0 && s.charAt(0) == '\n') {
			System.out.println("");
			s = s.substring(1);
		}
		System.out.println("LogonAction: " + s);
	}

}

