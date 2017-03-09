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

import org.dlese.dpc.repository.RepositoryManager;
import org.dlese.dpc.repository.SetInfo;

import org.dlese.dpc.schemedit.SchemEditUtils;
import org.dlese.dpc.schemedit.SessionRegistry;
import org.dlese.dpc.schemedit.SessionBean;
import org.dlese.dpc.schemedit.action.DCSAction;
import org.dlese.dpc.schemedit.security.access.AccessManager;
import org.dlese.dpc.schemedit.security.access.Roles;
import org.dlese.dpc.schemedit.security.util.*;
import org.dlese.dpc.schemedit.security.user.User;
import org.dlese.dpc.schemedit.security.user.UserManager;
import org.dlese.dpc.schemedit.security.action.form.CollectionAccessForm;

import org.dlese.dpc.schemedit.config.CollectionRegistry;
import org.dlese.dpc.schemedit.config.CollectionConfig;

/**
 *  Controller for viewing and editing user access (roles) for each collection
 *  for which the sessionUser has managerial permissions.
 *
 * @author    Jonathan Ostwald
 */

public final class CollectionAccessAction extends DCSAction {

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
	 * @param  form                  the ActionForm
	 * @return                       appropriate ActionForward
	 * @exception  IOException       if an input/output error occurs
	 * @exception  ServletException  if a servlet exception occurs
	 */
	public ActionForward execute(ActionMapping mapping,
	                             ActionForm form,
	                             HttpServletRequest request,
	                             HttpServletResponse response)
		 throws IOException, ServletException {

		// Initialize from servlet context attributes
		ActionErrors errors = initializeFromContext(mapping, request);
		if (!errors.isEmpty()) {
			saveErrors(request, errors);
			return (mapping.findForward("error.page"));
		}

		// ensure authentication is enabled
		ServletContext servletContext = servlet.getServletContext();
		boolean authenticationEnabled = (Boolean) servletContext.getAttribute("authenticationEnabled");
		if (!authenticationEnabled) {
			errors.add("auth-not-enabled",
				new ActionError("authentication.not.enabled"));
			saveErrors(request, errors);
			return (mapping.findForward("collection.access"));
		}

		CollectionAccessForm caForm = (CollectionAccessForm) form;

		Locale locale = getLocale(request);
		MessageResources messages = getResources(request);

		User sessionUser = getSessionUser(request);
		List authorizedSets =
			repositoryService.getAuthorizedSets(sessionUser, this.requiredRole);
		List managedUsers = AccessUtils.getManagedUsers(sessionUser, authorizedSets, userManager);
		List managableUsers = AccessUtils.getManagableUsers(sessionUser, managedUsers, userManager);
		caForm.setSets(authorizedSets);

		caForm.setRoleOptions(getRoleOptions(accessManager.getRoles(Roles.MANAGER_ROLE)));

		// show request params excluding collapse bean and form input
		SchemEditUtils.showRequestParameters(request);

		// Query Args
		String command = request.getParameter("command");

		if (command == null || isCancelled(request)) {
			caForm.setCollection(caForm.getCollection());
			caForm.setCollectionRoles(null);
			caForm.setManagableUsers(AccessUtils.getManagableUsers(sessionUser, managedUsers, userManager));
			caForm.setCollectionAccessMap(this.getCollectionAccessMap(authorizedSets, managedUsers));
			// caForm.setUsers(userManager.getUsers());
			// caForm.set
			return (mapping.findForward("collection.access"));
		}

		else if ("edit".equals(command)) {
			// prtln ("\n handling edit");
			String collection = request.getParameter("collection");
			SetInfo setInfo = null;
			try {
				// setInfo = this.getAuthorizedSetInfo(collection, authorizedSets);
				setInfo = AccessUtils.getAuthorizedSetInfo(collection, authorizedSets);
			} catch (Exception e) {
				prtln("caught error: " + e.getMessage());
				errors.add("error", new ActionError("generic.error", e.getMessage()));
			}

			if (!errors.isEmpty()) {
				saveErrors(request, errors);
				return (mapping.findForward("collection.access"));
			}

			String username = request.getParameter("username");
			if (username != null) {
				User user = this.userManager.getUser(username);
				if (!managedUsers.contains(user))
					managedUsers.add(user);
				caForm.setUsername(username);
				if (user.getRole(setInfo.getSetSpec()) == Roles.NO_ROLE) {
					errors.add("message",
						new ActionError("assign.user.access.to.collection",
						user.getFullName(), setInfo.getName()));
				}
			}

			caForm.setSet(setInfo);
			caForm.setCollection(collection);
			caForm.setCollectionRoles(getCollectionRoles(collection, managedUsers));
			caForm.setManagableUsers(AccessUtils.getManagableUsers(sessionUser, managedUsers, userManager));
			saveErrors(request, errors);
			return mapping.findForward("edit.collection.access");
		}

		else if ("save".equals(command)) {
			String collection = caForm.getCollection();
			SetInfo setInfo = null;
			try {
				setInfo = AccessUtils.getAuthorizedSetInfo(collection, authorizedSets);
			} catch (Exception e) {
				errors.add("error", new ActionError("generic.error", e.getMessage()));
			}

			if (!errors.isEmpty()) {
				saveErrors(request, errors);
				return (mapping.findForward("collection.access"));
			}
			Enumeration paramNames = request.getParameterNames();
			while (paramNames.hasMoreElements()) {
				String paramName = (String) paramNames.nextElement();
				if (paramName.startsWith("role_")) {
					String username = paramName.substring("role_".length());
					String roleStr = request.getParameter(paramName);
					// prtln ("username: " + username + "  role: " + roleStr);

					User user = userManager.getUser(username);
					try {
						user.setRole(collection, Roles.toRole(roleStr));
						updateUserSessionBean(user);
						user.flush();
					} catch (Throwable t) {
						prtln("could not assign role: " + t.getMessage());
						if (user == null)
							prtln("\t User not found for \"" + username + "\"");
					}
				}
			}

			managedUsers = AccessUtils.getManagedUsers(sessionUser, authorizedSets, userManager);
			caForm.setManagableUsers(AccessUtils.getManagableUsers(sessionUser, managedUsers, userManager));
			caForm.setCollectionAccessMap(this.getCollectionAccessMap(authorizedSets, managedUsers));
		}
		else {
			errors.add("error",
				new ActionError("generic.error", "Unrecognized command " + command));
			saveErrors(request, errors);
		}

		// Forward control to the specified success URI
		return (mapping.findForward("collection.access"));
	}


	/**
	 *  Reset the setQuerySelectorsInitialized flag in all sessions this user is
	 *  logged onto so that searching query selectors will be updated in the search
	 *  pages.
	 *
	 * @param  user  the User
	 */
	private void updateUserSessionBean(User user) {
		Iterator i = sessionRegistry.getUserSessionBeans(user).iterator();
		while (i.hasNext()) {
			SessionBean sb = (SessionBean) i.next();
			sb.setQuerySelectorsInitialized(false);
		}
	}


	/**
	 *  Gets the roleOptions attribute of the CollectionAccessAction object
	 *
	 * @param  roles  NOT YET DOCUMENTED
	 * @return        The roleOptions value
	 */
	private List getRoleOptions(Collection roles) {
		// prtln ("getRoleOptions");
		List options = new ArrayList();
		options.add(new LabelValueBean("- none -", ""));
		for (Iterator i = roles.iterator(); i.hasNext(); ) {
			String role = Roles.toString((Roles.Role) i.next());
			// prtln ("adding options for " + role);
			options.add(new LabelValueBean(role, role));
		}
		return options;
	}


	/**
	 *  Gets the collectionAccessMap attribute of the CollectionAccessAction object
	 *
	 * @param  authorizedSets  NOT YET DOCUMENTED
	 * @param  users           NOT YET DOCUMENTED
	 * @return                 The collectionAccessMap value
	 */
	private Map getCollectionAccessMap(List authorizedSets, Collection users) {
		// prtln ("getCollectionAccessMap");
		List collections = getCollections(authorizedSets);
		Map accessMap = new LinkedHashMap();
		for (Iterator i = collections.iterator(); i.hasNext(); ) {
			LabelValueBean cb = (LabelValueBean) i.next();
			String collection = cb.getValue();
			accessMap.put(collection, getCollectionRoles(collection, users));
		}
		return accessMap;
	}


	/**
	 *  Gets the collections attribute of the CollectionAccessAction object
	 *
	 * @param  authorizedSets  NOT YET DOCUMENTED
	 * @return                 The collections value
	 */
	private List getCollections(List authorizedSets) {
		List options = new ArrayList();

		if (authorizedSets == null)
			return options;

		for (Iterator i = authorizedSets.iterator(); i.hasNext(); ) {
			SetInfo setInfo = (SetInfo) i.next();
			// prtln ("\t getting info for " + setInfo.getSetSpec());
			try {
				options.add(new LabelValueBean(setInfo.getName(), setInfo.getSetSpec()));
			} catch (Throwable t) {
				prtln("getCollections error: " + t.getMessage());
			}
		}
		Collections.sort(options, new CollectionLabelValueSorter());
		return options;
	}


	/**
	 *  Returns a list of UserRoleBeans, sorted by user fullname (lastname,
	 *  firstname).
	 *
	 * @param  collection  collection key
	 * @param  users       registered system users
	 * @return             The collectionRoles value
	 */
	private List getCollectionRoles(String collection, Collection users) {
		List list = new ArrayList();
		for (Iterator i = users.iterator(); i.hasNext(); ) {
			User user = (User) i.next();
			Roles.Role role = user.getRole(collection);
			list.add(new UserRoleBean(user, Roles.toString(role)));
		}
		Collections.sort(list, new UserRoleBeanComparator());
		return list;
	}


	/**
	 *  Bean storing User object with a role value.
	 *
	 * @author    Jonathan Ostwald
	 */
	public class UserRoleBean {
		private User user;
		private String role;


		/**
		 *  Constructor for the UserRoleBean object
		 *
		 * @param  user  the User object
		 * @param  role  a role value
		 */
		public UserRoleBean(User user, String role) {
			this.user = user;
			this.role = role;
		}


		/**
		 *  Gets the user attribute of the UserRoleBean object
		 *
		 * @return    The user value
		 */
		public User getUser() {
			return user;
		}


		/**
		 *  Returns full name (lastname, firstname) of Beans's user attribute (or the
		 *  username if fullname can't be computed).
		 *
		 * @return    The fullName value
		 */
		public String getFullName() {
			try {
				return user.getLastName().toUpperCase() + user.getFirstName().toUpperCase();
			} catch (Throwable t) {}
			return user.getUsername().toUpperCase();
		}


		/**
		 *  Gets the role attribute of the UserRoleBean object
		 *
		 * @return    The role value
		 */
		public String getRole() {
			return role;
		}
	}


	/**
	 *  Comparator for ordering UserRoleBeans by full name
	 *
	 * @author    Jonathan Ostwald
	 */
	class UserRoleBeanComparator implements Comparator, java.io.Serializable {

		/**
		 *  Compare two UserRoleBean objects
		 *
		 * @param  o1  userRoleBean1
		 * @param  o2  userRoleBean2
		 * @return     comparison
		 */
		public int compare(Object o1, Object o2) {
			UserRoleBean u1 = (UserRoleBean) o1;
			UserRoleBean u2 = (UserRoleBean) o2;

			return u1.getFullName().compareTo(u2.getFullName());
		}
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  s  NOT YET DOCUMENTED
	 */
	static void prtln(String s) {
		while (s.length() > 0 && s.charAt(0) == '\n') {
			System.out.println("");
			s = s.substring(1);
		}
		System.out.println("CollectionAccessAction: " + s);
	}

}

