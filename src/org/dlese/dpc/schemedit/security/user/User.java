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
package org.dlese.dpc.schemedit.security.user;

import java.io.Serializable;
import java.util.*;
import java.io.File;

import org.dlese.dpc.schemedit.SchemEditUtils;
import org.dlese.dpc.schemedit.config.AbstractConfigReader;
import org.dlese.dpc.schemedit.security.auth.AuthUtils;
import org.dlese.dpc.schemedit.security.access.Roles;
import org.dlese.dpc.schemedit.security.access.Roles.Role;
import org.dlese.dpc.schemedit.RoleManager;
import org.dlese.dpc.xml.Dom4jUtils;
import org.dlese.dpc.util.Files;

import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.DocumentHelper;

/**
 *  Class that represents a registered DCS User, including attributes, roles and
 *  preferences. <p>
 *
 *  Note: passwords are not stored with the User objects.
 *
 *@author    ostwald
 */

public class User {

	private static boolean debug = false;
	private Map roleMap = null;
	private Map prefMap = null;
	private UserConfigReader reader = null;


	/**
	 *  Constructor for the User object
	 *
	 *@param  source         User data file
	 *@exception  Exception  if the user cannot be instantiated
	 */
	public User(File source) throws Exception {
		try {
			this.reader = new UserConfigReader(source);
		} catch (Exception e) {
			prtln("unable to read file at " + source + " : " + e.getMessage());
			return;
		}
		getRoleMap();
		getPrefMap();
	}


	/**
	 *  No-argument Constructor for the User object
	 *
	 *@exception  Exception  Description of the Exception
	 */
	public User() throws Exception {
		String userRecordTemplate =
				Files.readFileFromJarClasspath("/org/dlese/dpc/schemedit/security/user/USER-RECORD-TEMPLATE.xml").toString();
		prtln("userRecordTemplate: " + userRecordTemplate);
		try {
			this.reader = new UserConfigReader(userRecordTemplate);
		} catch (Exception e) {
			prtln("unable to initialize from User record template");
			e.printStackTrace();
			return;
		}
		this.roleMap = this.getRoleMap();
	}


	/**
	 *  Gets the file for this user object
	 *
	 *@return    The source value
	 */
	public File getSource() {
		return reader.getSource();
	}


	/**
	 *  Sets the file path for this object (where it is flushed).
	 *
	 *@param  file  The new source value
	 */
	public void setSource(File file) {
		this.reader.setSource(file);
	}




	/**
	 *  Description of the Method
	 *
	 *@param  node  Description of the Parameter
	 */
	private static void pp(Node node) {
		prtln(Dom4jUtils.prettyPrint(node));
	}


	/**
	 *  The username (must be unique).
	 */
	private String username_path = "/record/username";


	/**
	 *  Return the username.
	 *
	 *@return    The username value
	 */
	public String getUsername() {
		if (this.reader == null) {
			prtln("READER is NULL");
		}
		return this.getNodeText(username_path);
	}


	/**
	 *  Set the username.
	 *
	 *@param  username  The new username
	 */
	public void setUsername(String username) {
		this.setNodeText(username_path, username);
	}


	/**
	 *  The EMAIL address from which messages are sent.
	 */
	private String email_path = "/record/general/email";


	/**
	 *  Return the from address.
	 *
	 *@return    The email value
	 */
	public String getEmail() {
		return nonNullValue(this.getNodeText(email_path));
	}


	/**
	 *  Set the from address.
	 *
	 *@param  email  The new from address
	 */
	public void setEmail(String email) {
		this.setNodeText(email_path, email);
	}


	private String firstName_path = "/record/general/firstname";


	/**
	 *  Gets the firstName attribute of the User object
	 *
	 *@return    The firstName value
	 */
	public String getFirstName() {
		return nonNullValue(this.getNodeText(firstName_path));
	}


	/**
	 *  Sets the firstName attribute of the User object
	 *
	 *@param  firstName  The new firstName value
	 */
	public void setFirstName(String firstName) {
		this.setNodeText(firstName_path, firstName);
	}


	private String lastName_path = "/record/general/lastname";


	/**
	 *  Gets the lastName attribute of the User object
	 *
	 *@return    The lastName value
	 */
	public String getLastName() {
		return nonNullValue(this.getNodeText(lastName_path));
	}


	/**
	 *  Sets the lastName attribute of the User object
	 *
	 *@param  lastName  The new lastName value
	 */
	public void setLastName(String lastName) {
		this.setNodeText(lastName_path, lastName);
	}


	/**
	 *  Gets the fullName attribute of the User object
	 *
	 *@return    The fullName value
	 */
	public String getFullName() {
		return getFirstName() + " " + getLastName();
	}


	private String institution_path = "/record/general/institution";


	/**
	 *  Gets the institution attribute of the User object
	 *
	 *@return    The institution value
	 */
	public String getInstitution() {
		return nonNullValue(this.getNodeText(institution_path));
	}


	/**
	 *  Sets the institution attribute of the User object
	 *
	 *@param  institution  The new institution value
	 */
	public void setInstitution(String institution) {
		this.setNodeText(institution_path, institution);
	}


	private String department_path = "/record/general/department";


	/**
	 *  Gets the department attribute of the User object
	 *
	 *@return    The department value
	 */
	public String getDepartment() {
		return nonNullValue(this.getNodeText(department_path));
	}


	/**
	 *  Sets the department attribute of the User object
	 *
	 *@param  department  The new department value
	 */
	public void setDepartment(String department) {
		this.setNodeText(department_path, department);
	}


	/**
	 *  Gets the roleMap attribute of the User object
	 *
	 *@return    The prefMap value
	 */
	public Map getPrefMap() {
		if (this.prefMap == null) {
			this.prefMap = new HashMap();
			List preferences = this.getNodes("/record/preferences/pref");
			if (preferences != null) {
				// prtln (prefs.size() + " prefs found");
				for (Iterator r = preferences.iterator(); r.hasNext(); ) {
					Element prefElement = (Element) r.next();
					try {
						String name = prefElement.element("prefname").getTextTrim();
						String val = prefElement.element("prefvalue").getTextTrim();
						this.setPref(name, val);
					} catch (Exception e) {
						prtln("Failed to set pref for " + Dom4jUtils.prettyPrint(prefElement)
								 + "\n Message: " + e.getMessage());
					}
				}
			} else {
				prtln("no prefs found");
			}
		}
		return prefMap;
	}


	/**
	 *  Gets the pref attribute of the User object
	 *
	 *@param  prefname  Description of the Parameter
	 *@return           The pref value
	 */
	public String getPref(String prefname) {
		return (String) prefMap.get(prefname);
	}


	/**
	 *  Set the User's rref for specified collection.<p>
	 *
	 *  If the rref to set is the same as the User's default rref, then delete the
	 *  rref for the specified collection (effectively setting it, since calls to
	 *  getPref for that collection will return the default rref).
	 *
	 *@param  name  The new pref value
	 *@param  val   The new pref value
	 */
	public void setPref(String name, String val) {
		prefMap.put(name, val);
	}


	/**
	 *  Delete a rref (if the collection is not the default collection)
	 *
	 *@param  name  Description of the Parameter
	 */
	public void deletePref(String name) {
		prefMap.remove(name);
	}


	/**
	 *  Description of the Method
	 *
	 *@return    Description of the Return Value
	 */
	private Element prefMapToElement() {
		Element prefrencesElement = DocumentHelper.createElement("preferences");
		for (Iterator i = this.getPrefMap().keySet().iterator(); i.hasNext(); ) {
			String prefname = (String) i.next();
			Element prefElement = prefrencesElement.addElement("pref");
			prefElement.addElement("prefname").setText(prefname);
			prefElement.addElement("prefvalue").setText(this.getPref(prefname));
		}
		return prefrencesElement;
	}

	// ROLES stuff

	private Role maxRole = null;


	/**
	 *  Gets the maxRole attribute of the User object
	 *
	 *@return    The maxRole value
	 */
	public Role getMaxRole() {
		if (maxRole == null) {
			if (this.isAdminUser()) {
				maxRole = Roles.ADMIN_ROLE;
				return maxRole;
			}

			// this MUST be executed before iteration, because it calles setRole, which
			// reinitializes maxRole .... (UGGH)
			Map roleMap = this.getRoleMap();

			maxRole = Roles.NO_ROLE;
			for (Iterator i = roleMap.values().iterator(); i.hasNext(); ) {
				Role thisRole = (Role) i.next();
				if (thisRole.compareTo(maxRole) >= 0) {
					maxRole = thisRole;
				}
			}
		}
		return maxRole;
	}


	/**
	 *  Return the role that has been explicitly assigned to the specified
	 *  collection. Returns null if there is no assignment.
	 *
	 *@param  collection  NOT YET DOCUMENTED
	 *@return             The assignedRole value
	 */
	public Role getAssignedRole(String collection) {
		return (Role) this.getRoleMap().get(collection);
	}


	/**
	 *  Gets the effective role for this collection, meaning if there is no
	 *  explicit role assigned, use the default.
	 *
	 *@param  collection  the collection
	 *@return             The role value
	 */
	public Role getRole(String collection) {
		if (this.isAdminUser()) {
			return Roles.ADMIN_ROLE;
		} else {
			Role role = (Role) this.getRoleMap().get(collection);
			return (role == null ? Roles.NO_ROLE : role);
		}
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 *@param  roleStr  NOT YET DOCUMENTED
	 *@return          NOT YET DOCUMENTED
	 */
	public boolean hasRole(String roleStr) {
		return hasRole(Roles.toRole(roleStr));
	}


	/**
	 *  Returns true if this User has a role statisfying provided Role in the specified
	 *  collection.
	 *
	 *@param  roleStr     specified role (as string)
	 *@param  collection  the collection
	 *@return             true if the user has permission for this collection
	 */
	public boolean hasRole(String roleStr, String collection) {
		return hasRole(Roles.toRole(roleStr), collection);
	}


	/**
	 *  Returns true if this User has a role statisfying provided Role in any
	 *  collection.
	 *
	 *@param  role  specified role
	 *@return       true if this User has a role statisfying provided Role in any collection
	 */
	public boolean hasRole(Role role) {
		return (this.isAdminUser() ? true : this.getMaxRole().satisfies(role));
	}


	/**
	 *  Returns true if this User has at least the specified role in the specified collection
	 *
	 *@param  role        the role
	 *@param  collection  the collection
	 *@return             NOT YET DOCUMENTED
	 */
	public boolean hasRole(Role role, String collection) {
		if (this.isAdminUser()) {
			return true;
		} else {
			return (collection == null ? hasRole(role) : getRole(collection).satisfies(role));
		}
	}


	private String isAdminUser_path = "/record/isAdminUser";


	/**
	 *  Sets the adminUser attribute of the User object
	 *
	 *@param  isAdmin  The new adminUser value
	 */
	public void setAdminUser(boolean isAdmin) {
		maxRole = null;
		this.setNodeText(isAdminUser_path, (isAdmin ? "true" : "false"));
	}


	/**
	 *  Returns true of this user is an admin
	 *
	 *@return    The adminUser value
	 */
	public boolean isAdminUser() {
		return "true".equals(this.getNodeText(isAdminUser_path));
	}


	/**
	 *  Returns true of this user is an admin
	 *
	 *@return    The isAdminUser value
	 */
	public boolean getIsAdminUser() {
		return this.isAdminUser();
	}


	/**
	 *  Does this user have a role higher than the provided for the specified collection
	 *
	 *@param  role        NOT YET DOCUMENTED
	 *@param  collection  NOT YET DOCUMENTED
	 *@return             NOT YET DOCUMENTED
	 */
	public boolean controls(Role role, String collection) {
		return this.getRole(collection).controls(role);
	}



	/**
	 *  Set the User's role for specified collection.<p>
	 *
	 *  If the role to set is the same as the User's default role, then delete the
	 *  role for the specified collection (effectively setting it, since calls to
	 *  getRole for that collection will return the default role).
	 *
	 *@param  collection  The new role value
	 *@param  role        The new role value
	 */
	public void setRole(String collection, Roles.Role role) {
		prtln("setRole   " + collection + ", " + role.toString());
		this.getRoleMap().put(collection, role);
		maxRole = null;
	}


	/**
	 *  Delete a role (if the collection is not the default collection)
	 *
	 *@param  collection  NOT YET DOCUMENTED
	 */
	public void deleteRole(String collection) {
		this.getRoleMap().remove(collection);
		maxRole = null;
	}


	/**
	 *  Gets the roleMap attribute of the User object
	 *
	 *@return    The roleMap value
	 */
	public Map getRoleMap() {
		if (this.roleMap == null) {
			this.roleMap = new HashMap();
			List roles = this.getNodes("/record/roles/role");
			if (roles != null) {
				// prtln (roles.size() + " roles found");
				for (Iterator r = roles.iterator(); r.hasNext(); ) {
					Element roleElement = (Element) r.next();
					try {
						String collection = roleElement.element("collection").getTextTrim();
						String roleStr = roleElement.element("rolename").getTextTrim();
						this.setRole(collection, Roles.toRole(roleStr));
					} catch (Exception e) {
						prtln("Failed to set role for " + Dom4jUtils.prettyPrint(roleElement)
								 + "\n Message: " + e.getMessage());
						e.printStackTrace();
					}
				}
			} else {
				prtln("no roles found");
			}
		}
		return roleMap;
	}


	/**
	 *  Description of the Method
	 *
	 *@return    Description of the Return Value
	 */
	private Element rolesMapToElement() {
		Element rolesElement = DocumentHelper.createElement("roles");
		if (!this.isAdminUser()) {
			for (Iterator i = this.getRoleMap().keySet().iterator(); i.hasNext(); ) {
				String collection = (String) i.next();

				// TODO - eliminate "default" collection. this can be removed after user files
				// for each User instance are written.

				Roles.Role role = this.getAssignedRole(collection);
				if (role != Roles.NO_ROLE) {
					Element roleElement = rolesElement.addElement("role");
					roleElement.addElement("rolename").setText(role.toString());
					roleElement.addElement("collection").setText(collection);

				}
			}
		}
		return rolesElement;
	}

	// READER calls

	/**
	 *  Gets the nodeText attribute of the User object
	 *
	 *@param  path  Description of the Parameter
	 *@return       The nodeText value
	 */
	private String getNodeText(String path) {
		return reader.getNodeText(path);
	}


	/**
	 *  Sets the nodeText for this path
	 *
	 *@param  path   The new nodeText value
	 *@param  value  The new nodeText value
	 */
	private void setNodeText(String path, String value) {
		reader.setNodeText(path, value);
	}


	/**
	 *  Gets the nodes for specified path
	 *
	 *@param  path  Description of the Parameter
	 *@return       The nodes value
	 */
	private List getNodes(String path) {
		return reader.getNodes(path);
	}


	/**
	 *  Gets the node for specified path
	 *
	 *@param  path  Description of the Parameter
	 *@return       The node value
	 */
	private Node getNode(String path) {
		return reader.getNode(path);
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 *@param  s  NOT YET DOCUMENTED
	 *@return    NOT YET DOCUMENTED
	 */
	public String nonNullValue(String s) {
		return (s == null ? "" : s);
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 *@return    NOT YET DOCUMENTED
	 */
	public String toString() {
		List s = new ArrayList();
		s.add("User: " + this.getUsername());
		s.add("isAdminUser: " + this.getIsAdminUser());
		s.add("fullname: " + this.getFullName());
		s.add("firstname: " + this.getFirstName());
		s.add("lastname: " + this.getLastName());
		s.add("institution: " + this.getInstitution());
		s.add("email: " + this.getEmail());
		s.add("roles:");
		Map roleMap = this.getRoleMap();
		if (roleMap == null || roleMap.isEmpty()) {
			s.add("\tnone");
		} else {
			for (Iterator i = roleMap.keySet().iterator(); i.hasNext(); ) {
				String collection = (String) i.next();
				s.add("\tcollection: " + collection + "  role: " + getRole(collection));
			}
		}

		// Prefs
		s.add("prefs:");
		Map prefMap = this.getPrefMap();
		if (prefMap == null || prefMap.isEmpty()) {
			s.add("\tnone");
		} else {
			for (Iterator i = prefMap.keySet().iterator(); i.hasNext(); ) {
				String pref = (String) i.next();
				s.add("\tpref: " + pref + "  val: " + getPref(pref));
			}
		}
		s.add("maxRole: " + this.getMaxRole().toString());
		return AuthUtils.joinTokens(s, "\n\t");
	}


	/**
	 *  Write this User to disk and reset data structures so they will be
	 *  reloaded from disk.
	 *
	 *@exception  Exception  Description of the Exception
	 */
	public void flush() throws Exception {
		prtln("\n flushing " + this.getUsername());
		// we have to update the prefs and roles structures!

		// UPDATE the document so it will be correctly written
		Element root = (Element) getNode("/record");
		String roles_path = ("/record/roles");
		String prefs_path = ("/record/preferences");
		// delete "roles" and "preferences", and then add again from their maps

		Element rolesElement = (Element) getNode(roles_path);
		if (rolesElement != null && !root.remove(rolesElement)) {
			prtln("WARNING: Roles not removed");
		}
		Element prefsElement = (Element) getNode(prefs_path);
		if (prefsElement != null && !root.remove(prefsElement)) {
			prtln("WARNING: Prefs not removed");
		}

		// insert new elements
		rolesElement = this.rolesMapToElement();
		prefsElement = this.prefMapToElement();

		// add elements constructed from the maps
		if (rolesElement != null)
			root.add(rolesElement);
		if (prefsElement != null)
			root.add(prefsElement);	

		this.reader.flush();
		this.roleMap = null;
		this.prefMap = null;

	}


	/**
	 *  Destroy the datastructures for this User object
	 */
	public void destroy() {
		if (roleMap != null) {
			this.roleMap.clear();
		}
		if (prefMap != null) {
			this.prefMap.clear();
		}
		if (reader != null) {
			this.reader.destroy();
		}
	}
	
	
	/**
	 *  The main program for the User class
	 *
	 *@param  args           The command line arguments
	 *@exception  Exception  Description of the Exception
	 */
	public static void main(String[] args) throws Exception {
		org.dlese.dpc.schemedit.test.TesterUtils.setSystemProps();
		System.out.println("hello dolly");
		String filepath = "/Users/ostwald/tmp/ncs_user_records/jonathan.xml";
		// String filepath = "/Users/ostwald/devel/dcs-records/2009_06_10-records/ncs_user/1246745355106/NCS_USER-000-000-000-006.xml";
		User user = new User(new File(filepath));

		prtln("BEFORE");
		prtln(user.toString());

		user.setRole("fooberry", Roles.CATALOGER_ROLE);
		user.setPref("my pref", "pref value");

		// user.deleteRole("fooberry");
		// user.deletePref("my pref");
		user.flush();

		prtln("AFTER");
		prtln(user.toString());
		/*
		 *  pp (user.prefMapToElement());
		 *  pp (user.rolesMapToElement());
		 */
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 *@param  s  NOT YET DOCUMENTED
	 */
	protected static void prtln(String s) {
		if (debug) {
			SchemEditUtils.prtln(s, "User");
		}
	}


	/**
	 *  Description of the Class
	 *
	 *@author    ostwald
	 */
	class UserConfigReader extends AbstractConfigReader {

		/**
		 *  Constructor for the UserConfigReader object
		 *
		 *@param  source         Description of the Parameter
		 *@exception  Exception  Description of the Exception
		 */
		UserConfigReader(File source) throws Exception {
			super(source);
		}


		/**
		 *  Constructor for the UserConfigReader object
		 *
		 *@param  xmlSource      Description of the Parameter
		 *@exception  Exception  Description of the Exception
		 */
		UserConfigReader(String xmlSource) throws Exception {
			super(xmlSource);
		}


		/**
		 *  Sets the nodeText attribute of the UserConfigReader object
		 *
		 *@param  path  The new nodeText value
		 *@param  val   The new nodeText value
		 */
		protected void setNodeText(String path, String val) {
			super.setNodeText(path, val);
		}


		/**
		 *  Sets the source attribute of the UserConfigReader object
		 *
		 *@param  file  The new source value
		 */
		protected void setSource(File file) {
			super.setSource(file);
		}

	}

}

