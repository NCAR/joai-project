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
import java.util.*;
import java.util.Locale;

import org.dom4j.Document;
import org.json.XML;
import org.json.JSONObject;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionError;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.util.MessageResources;
import org.apache.struts.util.LabelValueBean;

import org.dlese.dpc.schemedit.SchemEditUtils;
import org.dlese.dpc.schemedit.action.DCSAction;
import org.dlese.dpc.xml.Dom4jUtils;

import org.dlese.dpc.schemedit.security.action.form.UserInfoForm;
import org.dlese.dpc.schemedit.security.access.AccessManager;
import org.dlese.dpc.schemedit.security.access.Roles;
import org.dlese.dpc.schemedit.security.user.User;
import org.dlese.dpc.schemedit.security.user.UserManager;
import org.dlese.dpc.schemedit.security.login.PasswordHelper;
import org.dlese.dpc.schemedit.security.auth.AuthUtils;

/**
 *  Controller for creating and editing user information.
 *
 * @author    Jonathan Ostwald
 */

public final class UserInfoAction extends DCSAction {

	private static boolean debug = false;

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

		ActionErrors errors = initializeFromContext(mapping, request);
		if (!errors.isEmpty()) {
			saveErrors(request, errors);
			return (mapping.findForward("error.page"));
		}
		UserInfoForm userForm = (UserInfoForm) form;

		// Extract attributes we will need
		Locale locale = getLocale(request);
		MessageResources messages = getResources(request);

		userForm.setUsers(userManager.getUsers());
		User sessionUser = getSessionUser(request);
		
		// Query Args
		String command = request.getParameter("command");

		
		SchemEditUtils.showRequestParameters(request, new String[]{"pass"});

		try {
			if (isCancelled(request)) {
				// prtln("cancelled!");
			}

			if (command == null) {
				userForm.setUsername(null);
				errors.add("error",
					new ActionError("generic.error", "command required!"));
				saveErrors(request, errors);
				return (mapping.findForward("edit.user"));
			}

			if ("popupUserInfo".equals(command)) {
				// prtln("ldapUserInfo");
				String username = request.getParameter("username");

				User user = userManager.getUser(username);
				if (user == null) {
					prtln ("user not found for " + username);
				}
				populateUserForm(user, sessionUser, userForm);
				return (mapping.findForward("popup.user.info"));
			}
			
			if ("ldapUserInfo".equals(command)) {
				// prtln("ldapUserInfo");
				return getLdapUserInfo(mapping, form, request, response);
			}

			if ("ucasUserInfo".equals(command)) {
				// prtln("ucasUserInfo");
				return getUcasUserInfo(mapping, form, request, response);
			}

			if ("edit".equals(command)) {
				String username = request.getParameter("username");

				User user = userManager.getUser(username);
				if (user == null) {
					userForm.setUsername(null);
					prtln("User \"" + username + "\" not found in database!");
					errors.add("error",
						new ActionError("error.username.notfound", username));
					saveErrors(request, errors);
					return (mapping.findForward("edit.user"));
				}

 				if (userForm.getFileLoginEnabled()) {
					try {
						String password = PasswordHelper.getInstance().getPassword(username);
						userForm.setPass(password);
						userForm.setPass2(password);
					} catch (Exception e) {
						if (userForm.getPasswordRequired())
							throw e;
					}
				}

				populateUserForm(user, sessionUser, userForm);

				userForm.setReferer(getReferer(request));

				// showHeaders(request);
				prtln("referer: " + userForm.getReferer());
			}

			if ("save".equals(command)) {
				// UserInfoForm.validate ensures there is a username, and password is okay
				prtln("handling save command");
				String username = userForm.getUsername();
				prtln("username is " + username);
				if (userForm.isNewUser())
					prtln("NEW USER");
				else
					prtln("NOT new user");

				User user = null;
				if (userForm.isNewUser()) {
					user = new User();
					user.setUsername(username);
				}
				else {
					user = userManager.getUser(username);
					if (user == null) {
						userForm.setUsername(null);
						throw new Exception("User not found in database for \"" + username + "\"");
					}
				}

				prtln("updating user object");
				user.setFirstName(userForm.getFirstname());
				user.setLastName(userForm.getLastname());
				user.setInstitution(userForm.getInstitution());
				user.setDepartment(userForm.getDepartment());
				user.setEmail(userForm.getEmail());

				String userIsAdmin = request.getParameter("userIsAdmin");
				if (userIsAdmin != null) {
					if (!sessionUser.hasRole(Roles.ADMIN_ROLE)) {
						errors.add("error", new ActionError("generic.error",
							"you must be an administrator to assign administrator role"));
						saveErrors(request, errors);
					}
					else {
						prtln("about to set admin to true for " + user.getFirstName());
						user.setAdminUser(userIsAdmin.equals("true"));
					}
				}

 				if (userForm.getFileLoginEnabled()) {
					// if password is optional and there is no value, then delete password entry for this user
					if (userForm.getPasswordOptional() && 
						(userForm.getPass() == null || userForm.getPass().trim().length() == 0)) {
						try {
							PasswordHelper.getInstance().remove(username);
						} catch (Exception e) {
							e.printStackTrace();
							throw new Exception ("PasswordHelper could not remove password: " + e.getMessage());
						}
					} else {
						// update password entry for this user
						try {
							PasswordHelper.getInstance().update(username, userForm.getPass());
						} catch (Exception e) {
							e.printStackTrace();
							throw new Exception("passwordHelper error: " + e.getMessage());
						}
					}
				}

				try {
					prtln("saving data");
					userManager.saveUser(user);
				} catch (Throwable e) {
					prtln("userManager.saveUser Failed!");
					e.printStackTrace();
					throw new Exception("accessManager flush ERROR: " + e.getMessage());
				}
				userForm.setNewUser(false);
				populateUserForm(user, sessionUser, userForm);
				errors.add("message",
					new ActionError("generic.message", "User info saved for \"" + username + "\""));
				saveErrors(request, errors);
			}

			if ("create".equals(command)) {
				User user = new User();
				userForm.setNewUser(true);
				populateUserForm(user, sessionUser, userForm);
				userForm.setReferer(getReferer(request));
			}
		} catch (Throwable t) {
			t.printStackTrace();
			errors.add("error",
				new ActionError("generic.error", "System Error: " + t.getMessage()));
			saveErrors(request, errors);
		}

		// Forward control to the specified success URI
		return (mapping.findForward("edit.user"));
	}
	
	/**
	 *  Epected Request Params:
	 *  <li> command : 'ldapUserInfo',
	 *  <li> searchString : may include asterisks for wildcarding,
	 *  <li> ldapField - either 'uid' or 'cn'<p>
	 *
	 *  Returns a list of matches (LDAP Entries) as JSON
	 *
	 * @param  mapping   NOT YET DOCUMENTED
	 * @param  form      NOT YET DOCUMENTED
	 * @param  request   NOT YET DOCUMENTED
	 * @param  response  NOT YET DOCUMENTED
	 * @return           The ldapUserInfo value
	 */
	private ActionForward getLdapUserInfo(ActionMapping mapping,
	                                      ActionForm form,
	                                      HttpServletRequest request,
	                                      HttpServletResponse response) {

		prtln("\ngetLdapUserInfo()");

		String searchString = request.getParameter("searchString");
		String ldapField = request.getParameter("ldapField");
		UserInfoForm userForm = (UserInfoForm) form;
		Document responseDoc = null;
		if (searchString == null || searchString.trim().length() == 0)
			return null;
		if (ldapField == null || ldapField.trim().length() == 0)
			return null;
		try {
			responseDoc = AuthUtils.getLdapUserInfo(searchString, ldapField);
			// prtln("responseDoc: " + Dom4jUtils.prettyPrint(responseDoc));
			JSONObject json = XML.toJSONObject(responseDoc.asXML());
			// prtln(json.toString(2));
			String ldapUserInfo = json.toString();
			// prtln ("ldapUserInfo: " + ldapUserInfo);
			userForm.setLdapUserInfo(ldapUserInfo);
		} catch (Exception e) {
			prtln("getLdapUserInfo error: " + e.getMessage());
			e.printStackTrace();
			userForm.setLdapUserInfo("ERROR: " + e.getMessage());
		}
		return mapping.findForward("ldap.user.info");
	}


	/**
	 *  Queries the UCAS People DB via the Rest API and returns results as json.
	 *
	 * @param  mapping   the mapping
	 * @param  form      the form
	 * @param  request   the request
	 * @param  response  the response
	 * @return           forward to jsp that accesses json from ActionForm.
	 */
	private ActionForward getUcasUserInfo(ActionMapping mapping,
	                                      ActionForm form,
	                                      HttpServletRequest request,
	                                      HttpServletResponse response) {

		prtln("\ngetUcasUserInfo()");

		String firstName = request.getParameter("firstName");
		String lastName = request.getParameter("lastName");
		UserInfoForm userForm = (UserInfoForm) form;
		Document responseDoc = null;

		try {
			String url = "https://api.ucar.edu/people/internalPersons?";
			if (firstName != null) {
				url += "firstName=" + firstName;
				if (lastName != null)
					url += "&";
			}
			if (lastName != null) {
				url += "lastName=" + lastName;
			}
			String json = org.dlese.dpc.util.TimedURLConnection.importURL(url, 5000);
			prtln("json: " + json);
			userForm.setLdapUserInfo(json);
		} catch (Exception e) {
			prtln("getUcasUserInfo error: " + e.getMessage());
			e.printStackTrace();
			userForm.setLdapUserInfo("ERROR: " + e.getMessage());
		}
		return mapping.findForward("ldap.user.info");
	}


	/**
	 *  Gets the request referer, attempting to overcome IE but that referer is not
	 *  sent in the header from event handlers. First looks for a request param
	 *  named "referer" and uses this if found, otherwise uses the "referer" from
	 *  the header.
	 *
	 * @param  request  NOT YET DOCUMENTED
	 * @return          The referer value
	 */
	private String getReferer(HttpServletRequest request) {
		String referer = request.getParameter("referer");
		return (referer != null) ? referer : request.getHeader("referer");
	}


	/**
	 *  Debugging 
	 *
	 * @param  request  the request
	 */
	private void showHeaders(HttpServletRequest request) {
		prtln("\n REQUEST HEADERS");
		Enumeration headerNames = request.getHeaderNames();
		while (headerNames.hasMoreElements()) {
			String name = (String) headerNames.nextElement();
			prtln(name + ": " + request.getHeader(name));
		}
		prtln("-----------\n");
	}

	/**
	 *  Initialize form bean with values to be edited
	 *
	 * @param  user         user object to be edited
	 * @param  userForm     the form
	 * @param  sessionUser  user object for session user
	 */
	void populateUserForm(User user, User sessionUser, UserInfoForm userForm) {
		userForm.setUsername(user.getUsername());
		userForm.setFirstname(user.getFirstName());
		userForm.setLastname(user.getLastName());
		userForm.setInstitution(user.getInstitution());
		userForm.setDepartment(user.getDepartment());
		userForm.setEmail(user.getEmail());

		boolean authenticationEnabled =
			(Boolean) servlet.getServletContext().getAttribute("authenticationEnabled");
		userForm.setCanAssignCollectionAccess(!authenticationEnabled ||
			sessionUser.hasRole(Roles.MANAGER_ROLE));
		userForm.setUserHasAdminRole(user.hasRole(Roles.ADMIN_ROLE));
/* 		prtln("populateUserForm() isAdmin? " + user.isAdminUser() + "  userForm.getUserHasAdminRole: " +
			userForm.getUserHasAdminRole()); */
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  s  NOT YET DOCUMENTED
	 */
	static void prtln(String s) {
		if (debug)
			SchemEditUtils.prtln(s, "UserInfoAction");
	}

}

