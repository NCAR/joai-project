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
package org.dlese.dpc.schemedit.security.action.form;

import java.util.*;
import java.io.Serializable;
import javax.servlet.http.HttpServletRequest;
import org.apache.struts.action.ActionError;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.util.LabelValueBean;

import org.dlese.dpc.repository.RepositoryManager;
import org.dlese.dpc.schemedit.dcs.DcsSetInfo;
import org.dlese.dpc.schemedit.config.CollectionRegistry;
import org.dlese.dpc.schemedit.config.CollectionConfig;
import org.dlese.dpc.schemedit.security.user.User;
import org.dlese.dpc.schemedit.security.user.UserManager;

/**
*  ActionForm supporting the {@link org.dlese.dpc.schemedit.security.action.UserManagerAction}.
 *
 * @author    Jonathan Ostwald
 */
public final class UserManagerForm extends ActionForm {

	private static boolean debug = false;


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  mapping  NOT YET DOCUMENTED
	 * @param  request  NOT YET DOCUMENTED
	 */
	public void reset(ActionMapping mapping, HttpServletRequest request) { }


	private Map userRoleMap = null;


	/**
	 *  Gets the userRoleMap attribute of the UserManagerForm object
	 *
	 * @return    The userRoleMap value
	 */
	public Map getUserRoleMap() {
		return userRoleMap;
	}


	/**
	 *  Sets the userRoleMap attribute of the UserManagerForm object
	 *
	 * @param  map  The new userRoleMap value
	 */
	public void setUserRoleMap(Map map) {
		userRoleMap = map;
	}


	private List users;


	/**
	 *  Gets the users attribute of the UserManagerForm object
	 *
	 * @return    The users value
	 */
	public List getUsers() {
		return users;
	}


	/**
	 *  Sets the users attribute of the UserManagerForm object
	 *
	 * @param  users  The new users value
	 */
	public void setUsers(List users) {
		this.users = users;
	}


	private String username;


	/**
	 *  Gets the username attribute of the UserManagerForm object
	 *
	 * @return    The username value
	 */
	public String getUsername() {
		return username;
	}


	/**
	 *  Sets the username attribute of the UserManagerForm object
	 *
	 * @param  username  The new username value
	 */
	public void setUsername(String username) {
		this.username = username;
	}


	private User user = null;


	/**
	 *  Gets the user attribute of the UserManagerForm object
	 *
	 * @return    The user value
	 */
	public User getUser() {
		return user;
	}


	/**
	 *  Sets the user attribute of the UserManagerForm object
	 *
	 * @param  user  The new user value
	 */
	public void setUser(User user) {
		this.user = user;
	}


	private boolean newRole = false;


	/**
	 *  Gets the newRole attribute of the UserManagerForm object
	 *
	 * @return    The newRole value
	 */
	public boolean getNewRole() {
		return newRole;
	}


	/**
	 *  Sets the newRole attribute of the UserManagerForm object
	 *
	 * @param  newRole  The new newRole value
	 */
	public void setNewRole(boolean newRole) {
		this.newRole = newRole;
	}


	private String collection;


	/**
	 *  Gets the collection attribute of the UserManagerForm object
	 *
	 * @return    The collection value
	 */
	public String getCollection() {
		return collection;
	}


	/**
	 *  Sets the collection attribute of the UserManagerForm object
	 *
	 * @param  collection  The new collection value
	 */
	public void setCollection(String collection) {
		this.collection = collection;
	}


	private List collectionOptions;


	/**
	 *  Gets the collectionOptions attribute of the UserManagerForm object
	 *
	 * @return    The collectionOptions value
	 */
	public List getCollectionOptions() {
		return collectionOptions;
	}


	/**
	 *  Sets the collectionOptions attribute of the UserManagerForm object
	 *
	 * @param  options  The new collectionOptions value
	 */
	public void setCollectionOptions(List options) {
		prtln("setting " + options.size() + " options");
		collectionOptions = options;
	}


	/**  NOT YET DOCUMENTED */
	public List managableUsers = null;


	/**
	 *  Gets the managableUsers attribute of the UserManagerForm object
	 *
	 * @return    The managableUsers value
	 */
	public List getManagableUsers() {
		return managableUsers;
	}


	/**
	 *  Sets the managableUsers attribute of the UserManagerForm object
	 *
	 * @param  userList  The new managableUsers value
	 */
	public void setManagableUsers(List userList) {
		managableUsers = userList;
	}


	/* 	private List collections;

	public List getCollections () {
		return collections;
	}

	public void setCollections (List options) {
		prtln ("setting " + options.size() + " collections");
		collections = options;
	} */
	private String role;


	/**
	 *  Gets the role attribute of the UserManagerForm object
	 *
	 * @return    The role value
	 */
	public String getRole() {
		return role;
	}


	/**
	 *  Sets the role attribute of the UserManagerForm object
	 *
	 * @param  role  The new role value
	 */
	public void setRole(String role) {
		this.role = role;
	}


	private Map roles;


	/**
	 *  Gets the roles attribute of the UserManagerForm object
	 *
	 * @return    The roles value
	 */
	public Map getRoles() {
		if (roles == null)
			roles = new TreeMap();
		return roles;
	}


	/**
	 *  Sets the roles attribute of the UserManagerForm object
	 *
	 * @param  roles  The new roles value
	 */
	public void setRoles(Map roles) {
		this.roles = roles;
	}


	private List roleOptions;


	/**
	 *  Gets the roleOptions attribute of the UserManagerForm object
	 *
	 * @return    The roleOptions value
	 */
	public List getRoleOptions() {
		return roleOptions;
	}


	/**
	 *  Sets the roleOptions attribute of the UserManagerForm object
	 *
	 * @param  options  The new roleOptions value
	 */
	public void setRoleOptions(List options) {
		this.roleOptions = options;
	}


	private String command = null;


	/**
	 *  Gets the command attribute of the UserManagerForm object
	 *
	 * @return    The command value
	 */
	public String getCommand() {
		return command;
	}


	/**
	 *  Sets the command attribute of the UserManagerForm object
	 *
	 * @param  cmd  The new command value
	 */
	public void setCommand(String cmd) {
		command = cmd;
	}


	private String[] scs = null;


	/**
	 *  Gets the scs attribute of the UserManagerForm object
	 *
	 * @return    The scs value
	 */
	public String[] getScs() {
		if (scs == null)
			scs = new String[]{};
		return scs;
	}


	/**
	 *  Sets the scs attribute of the UserManagerForm object
	 *
	 * @param  scs  The new scs value
	 */
	public void setScs(String[] scs) {
		this.scs = scs;
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  mapping  NOT YET DOCUMENTED
	 * @param  request  NOT YET DOCUMENTED
	 * @return          NOT YET DOCUMENTED
	 */
	public ActionErrors validate(ActionMapping mapping,
	                             HttpServletRequest request) {
		ActionErrors errors = new ActionErrors();
		// prtln ("validate()");

		if (command != null && (command.equals("edit") || command.equals("save"))) {
			UserManager userManager =
				(UserManager) getServlet().getServletContext().getAttribute("userManager");
			if (username == null) {
				errors.add(ActionErrors.GLOBAL_ERROR,
					new ActionError("error.username.required"));
			}

			else {
				user = userManager.getUser(username);
				if (user == null) {
					prtln("User \"" + username + "\" not found in database!");
					errors.add(ActionErrors.GLOBAL_ERROR,
						new ActionError("error.username.notfound", username));
				}
			}
		}

		return errors;
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @author    Jonathan Ostwald
	 */
	public class RoleBean {
		private String collection;
		private String role;


		/**
		 *  Constructor for the RoleBean object
		 *
		 * @param  collection  NOT YET DOCUMENTED
		 * @param  role        NOT YET DOCUMENTED
		 */
		public RoleBean(String collection, String role) {
			this.collection = collection;
			this.role = role;
		}


		/**
		 *  Gets the collection attribute of the RoleBean object
		 *
		 * @return    The collection value
		 */
		public String getCollection() {
			return collection;
		}


		/**
		 *  Gets the role attribute of the RoleBean object
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
	 * @param  s  NOT YET DOCUMENTED
	 */
	static void prtln(String s) {
		while (s.length() > 0 && s.charAt(0) == '\n') {
			System.out.println("");
			s = s.substring(1);
		}
		System.out.println("UserManagerForm: " + s);
	}

}

