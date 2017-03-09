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
import java.io.Serializable;
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
import org.dlese.dpc.schemedit.struts.MyModuleConfig;
import org.dlese.dpc.schemedit.security.access.AccessManager;
import org.dlese.dpc.schemedit.security.access.ActionPath;
import org.dlese.dpc.schemedit.security.util.CollectionLabelValueSorter;
import org.dlese.dpc.schemedit.security.util.AccessUtils;
import org.dlese.dpc.schemedit.security.login.PasswordHelper;
import org.dlese.dpc.schemedit.security.user.*;

import org.dlese.dpc.schemedit.security.access.Roles;
import org.dlese.dpc.schemedit.security.action.form.UserManagerForm;

import org.dlese.dpc.schemedit.config.CollectionRegistry;
import org.dlese.dpc.schemedit.config.CollectionConfig;

/**
 *  Controller for Manage Users page, which displays users and their roles for
 *  each collection, and allows for editing these roles, as well as deleting
 *  users.
 *
 * @author    Jonathan Ostwald
 */

public final class UserManagerAction extends DCSAction {

	private static boolean debug = false;

	private Comparator userComparator = new FullNameComparator();

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
		ActionErrors errors = initializeFromContext(mapping, request);
		if (!errors.isEmpty()) {
			saveErrors(request, errors);
			return (mapping.findForward("error.page"));
		}

		ServletContext servletContext = servlet.getServletContext();
		boolean authenticationEnabled = (Boolean) servletContext.getAttribute("authenticationEnabled");
		if (!authenticationEnabled) {
			errors.add("auth-not-enabled",
				new ActionError("authentication.not.enabled"));
			saveErrors(request, errors);
			return (mapping.findForward("manage.users"));
		}

		UserManagerForm umForm = (UserManagerForm) form;

		Locale locale = getLocale(request);
		MessageResources messages = getResources(request);

		// Query Args
		String command = request.getParameter("command");

		// show request params excluding collapse bean and form input
		// SchemEditUtils.showRequestParameters(request);

		if ("edit".equals(command)) {
			String userName = request.getParameter("username");
			umForm.setUsername(userName);
			User user = userManager.getUser(userName);
			umForm.setUser(user);
			umForm.setUserRoleMap(getUserRoleMap(user, getSessionUser(request)));
			if (request.getParameter("add") != null) {
				errors.add("message",
					new ActionError("generic.message",
					"Assign roles to " + user.getFullName() + " to grant access to collections"));
			}
			saveErrors(request, errors);
			return (mapping.findForward("user.access"));
		}

		else if ("save".equals(command)) {
			prtln("handling SAVE command");
			errors.add(handleSaveCommand(mapping, form, request, response));
		}

		else if ("delete".equals(command)) {
			prtln("handling DELETE command");
			errors.add(handleDeleteCommand(mapping, form, request, response));
		}

		if (command == null || isCancelled(request)) {
			String userName = request.getParameter("username");
			umForm.setUsername(userName);
			umForm.setUser(userManager.getUser(userName));
		}

		else {
			errors.add(ActionErrors.GLOBAL_ERROR,
				new ActionError("generic.error", "Unrecognized command " + command));
		}

		User sessionUser = getSessionUser(request);
		List authorizedSets =
			repositoryService.getAuthorizedSets(sessionUser, this.requiredRole);
		List managedUsers = AccessUtils.getManagedUsers(sessionUser, authorizedSets, userManager, userComparator);
		List managableUsers = AccessUtils.getManagableUsers(sessionUser, managedUsers, userManager, userComparator);
		umForm.setUsers(managedUsers);
		umForm.setManagableUsers(managableUsers);
		umForm.setUserRoleMap(getUserRoleMap(managedUsers, sessionUser));

		saveErrors(request, errors);
		return (mapping.findForward("manage.users"));
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  mapping   NOT YET DOCUMENTED
	 * @param  form      NOT YET DOCUMENTED
	 * @param  request   NOT YET DOCUMENTED
	 * @param  response  NOT YET DOCUMENTED
	 * @return           NOT YET DOCUMENTED
	 */
	private ActionErrors handleSaveCommand(ActionMapping mapping,
	                                       ActionForm form,
	                                       HttpServletRequest request,
	                                       HttpServletResponse response) {
		ActionErrors errors = new ActionErrors();
		UserManagerForm umForm = (UserManagerForm) form;
		String username = umForm.getUsername();
		User user = umForm.getUser();

		Enumeration paramNames = request.getParameterNames();
		while (paramNames.hasMoreElements()) {
			String paramName = (String) paramNames.nextElement();
			if (paramName.startsWith("role_")) {
				String collection = paramName.substring("role_".length());
				String roleStr = request.getParameter(paramName);
				prtln("collection: " + collection + "  role: " + roleStr);

				// this is where we should verify that the sessionUuser actually has control
				// over this collection
				user.setRole(collection, Roles.toRole(roleStr));
			}
		}

		prtln(" ... done iterating");
		try {
			prtln("\nsaving data");
			userManager.flush();
			prtln("  .. done saving data");
		} catch (Exception e) {
			prtln("userManager save data ERROR: " + e.getMessage());
		}
		prtln("\nupdatingUserSessionBean");
		updateUserSessionBean(user);
		prtln("   ... done updatingUserSessionBean");

		return errors;
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  mapping   NOT YET DOCUMENTED
	 * @param  form      NOT YET DOCUMENTED
	 * @param  request   NOT YET DOCUMENTED
	 * @param  response  NOT YET DOCUMENTED
	 * @return           NOT YET DOCUMENTED
	 */
	private ActionErrors handleDeleteCommand(ActionMapping mapping,
	                                         ActionForm form,
	                                         HttpServletRequest request,
	                                         HttpServletResponse response) {
		ActionErrors errors = new ActionErrors();
		UserManagerForm umForm = (UserManagerForm) form;
		String username = umForm.getUsername();
		User user = this.userManager.getUser(username);

		if (user == null) {
			errors.add("error",
				new ActionError("generic.error",
				"Could not delete \"" + username + "\" - user not found"));
			return errors;
		}

		/*
			Cannot delete user unless sessionUser "controls" all collections in which the user
			participates.
			for all user collections
				sessionUser.role (collection) must control user.role (collection)
		*/
		if (errors.isEmpty()) {
			// users can't delete themselves!
			User sessionUser = getSessionUser(request);
			if (user == sessionUser) {
				errors.add("error",
					new ActionError("You may not delete yourself"));
			}

			// admins can delete any user except root or themselves
			else if (sessionUser.isAdminUser()) {
				User rootUser = this.userManager.getUser("root");
				if (sessionUser == rootUser) {
					errors.add("error",
						new ActionError("Root user may not be deleted"));
				}
			}
			else {

				Iterator userCollections = user.getRoleMap().keySet().iterator();
				while (userCollections.hasNext()) {
					String collection = (String) userCollections.next();
					if (!user.hasRole(Roles.CATALOGER_ROLE, collection))
						continue;
					prtln("collection: " + collection);
					prtln("\t user role: " + user.getRole(collection).toString());
					prtln("\t sessionUser role: " + sessionUser.getRole(collection).toString());
					if (!sessionUser.getRole(collection).controls(user.getRole(collection))) {
						errors.add("error",
							new ActionError("generic.error",
							"Could not delete \"" + username + "\" - this user participates in collections " +
							"that you do not control. Consider removing access for this user " +
							"from your collections, rather than deleting the user."));
						break;
					}
				}
			}
		}

		if (errors.isEmpty()) {
			// userManager.unregister(user);
			
			prtln("\nupdatingUserSessionBean");
			updateUserSessionBean(user);
			
			userManager.deleteUser(username);
			try {
				prtln("\nsaving data");
				userManager.flush();
/* 				if ("FileLogin".equals(this.authScheme)) {
					PasswordHelper.getInstance().remove(username);
				} */
				prtln("  .. done saving data");
			} catch (Exception e) {
				prtln("userManager save data ERROR: " + e.getMessage());
			}

			prtln("   ... done updatingUserSessionBean");
			errors.add("message",
				new ActionError("generic.message",
				"User \"" + username + "\" - has been successfully deleted"));
		}

		return errors;
	}



	/**
	 *  Reset the setQuerySelectorsInitialized flag in all sessions this user is
	 *  logged onto so that searching query selectors will be updated in the search
	 *  pages.
	 *
	 * @param  user  the user
	 */
	private void updateUserSessionBean(User user) {
		Iterator i = sessionRegistry.getUserSessionBeans(user).iterator();
		while (i.hasNext()) {
			SessionBean sb = (SessionBean) i.next();
			sb.setQuerySelectorsInitialized(false);
		}
	}


	/**
	 *  Return a list representing the collections for which this user has no role
	 *  explicitly assigned
	 *
	 * @param  user  the user
	 * @return       The collectionOptions value
	 */
	private List getCollectionOptions(User user) {

		List options = new ArrayList();

		if (user == null) {
			prtln("WARNING getCollectionOptions got a null user");
			return options;
		}

		prtln("getCollectionOptions for " + user.getUsername());

		for (Iterator i = collectionRegistry.getIds().iterator(); i.hasNext(); ) {
			String collection = (String) i.next();
			prtln("\t getting info for " + collection);
			try {
				CollectionConfig info = collectionRegistry.getCollectionConfig(collection);
				// only add collection if user does not already have a role assigned for it
				if (info != null && user.getAssignedRole(collection) == null)
					options.add(new LabelValueBean(info.getSetInfo(repositoryManager).getName(), collection));
			} catch (Throwable t) {
				prtln("getCollectionOptions error: " + t.getMessage());
			}
		}
		Collections.sort(options, new CollectionLabelValueSorter());
		return options;
	}


	/**
	 *  Gets the userRoleMap attribute of the UserManagerAction object
	 *
	 * @param  user         NOT YET DOCUMENTED
	 * @param  sessionUser  NOT YET DOCUMENTED
	 * @return              The userRoleMap value
	 */
	public Map getUserRoleMap(User user, User sessionUser) {
		List list = new ArrayList();
		list.add(user);
		return getUserRoleMap(list, sessionUser);
	}


	/**
	 *  Create a mapping from username to List of CollectionRoleBeans, which are
	 *  sorted by collection. Don't worry about sorting users, this will be done on
	 *  a separate data structure.
	 *
	 * @param  managedUsers  NOT YET DOCUMENTED
	 * @param  sessionUser   NOT YET DOCUMENTED
	 * @return               The userRoleMap value
	 */
	public Map getUserRoleMap(Collection managedUsers, User sessionUser) {
		Map map = new HashMap();
		Comparator sorter = new CollectionRoleBeanSorter();
		List collectionBeans = getCollections(sessionUser);
		for (Iterator i = managedUsers.iterator(); i.hasNext(); ) {
			User user = (User) i.next();
			List collectionRoles = new ArrayList();
			for (Iterator j = collectionBeans.iterator(); j.hasNext(); ) {
				LabelValueBean lvBean = (LabelValueBean) j.next();
				String collectionName = lvBean.getLabel();
				String collectionKey = lvBean.getValue();
				String role = Roles.toString(user.getRole(collectionKey));
				collectionRoles.add(new CollectionRoleBean(collectionName, collectionKey, role));
			}
			Collections.sort(collectionRoles, sorter);
			map.put(user.getUsername(), collectionRoles);
		}
		// Debugging
/* 		prtln (" ... done - getUserRoleMap has " + map.size() + " entries");
		for (Iterator i=map.keySet().iterator();i.hasNext();)
			prtln ("\t" + (String)i.next()); */
		
		return map;
	}


	/**
	 *  Gets the collections attribute of the UserManagerAction object
	 *
	 * @param  sessionUser  NOT YET DOCUMENTED
	 * @return              The collections value
	 */
	private List getCollections(User sessionUser) {
		List collections = new ArrayList();
		Iterator sets =
			repositoryService.getAuthorizedSets(sessionUser,
			Roles.MANAGER_ROLE).iterator();
		while (sets.hasNext()) {
			SetInfo setInfo = (SetInfo) sets.next();
			collections.add(new LabelValueBean(setInfo.getName(), setInfo.getSetSpec()));
		}
		return collections;
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @author    Jonathan Ostwald
	 */
	public class CollectionRoleBean {
		private String collectionName;
		private String collectionKey;
		private String role;


		/**
		 *  Constructor for the CollectionRoleBean object
		 *
		 * @param  collectionName  NOT YET DOCUMENTED
		 * @param  collectionKey   NOT YET DOCUMENTED
		 * @param  role            NOT YET DOCUMENTED
		 */
		public CollectionRoleBean(String collectionName, String collectionKey, String role) {
			this.collectionName = collectionName;
			this.collectionKey = collectionKey;
			this.role = role;
		}


		/**
		 *  Gets the collectionName attribute of the CollectionRoleBean object
		 *
		 * @return    The collectionName value
		 */
		public String getCollectionName() {
			return collectionName;
		}


		/**
		 *  Gets the collectionKey attribute of the CollectionRoleBean object
		 *
		 * @return    The collectionKey value
		 */
		public String getCollectionKey() {
			return collectionKey;
		}


		/**
		 *  Gets the role attribute of the CollectionRoleBean object
		 *
		 * @return    The role value
		 */
		public String getRole() {
			return role;
		}
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @author    Jonathan Ostwald
	 */
	public class CollectionRoleBeanSorter implements Comparator, Serializable {

		/**
		 *  NOT YET DOCUMENTED
		 *
		 * @param  o1  NOT YET DOCUMENTED
		 * @param  o2  NOT YET DOCUMENTED
		 * @return     NOT YET DOCUMENTED
		 */
		public int compare(Object o1, Object o2) {

			CollectionRoleBean crBean1 = (CollectionRoleBean) o1;
			CollectionRoleBean crBean2 = (CollectionRoleBean) o2;

			String s1 = crBean1.getCollectionName();
			String s2 = crBean2.getCollectionName();

			if (s1 == null)
				s1 = "";
			if (s2 == null)
				s2 = "";

			return s1.toUpperCase().compareTo(s2.toUpperCase());
		}
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  s  NOT YET DOCUMENTED
	 */
	static void prtln(String s) {
		if (debug) {
			SchemEditUtils.prtln(s, "UserManagerAction");
		}
	}

}

