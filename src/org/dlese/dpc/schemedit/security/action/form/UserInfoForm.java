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

import javax.servlet.http.HttpServletRequest;
import org.apache.struts.action.ActionError;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;
import org.dlese.dpc.schemedit.security.user.UserManager;
import org.dlese.dpc.schemedit.security.auth.AuthUtils;

/**

 */

public final class UserInfoForm extends ActionForm {

	private static boolean debug = true;
	
	public void reset (ActionMapping mapping, HttpServletRequest request) {}
	
	private String referer = null;
	
	
	public String getReferer() {
		return referer;
	}
	/**
	 *  Sets the referer attribute of the StatusForm object
	 *
	 *@param  referer  The new referer value
	 */
	public void setReferer(String referer) {
		this.referer = referer;
	}

	
	private String command = null;
	
	public String getCommand() {
		return command;
	}
	
	public void setCommand( String cmd) {
		command = cmd;
	}
	
	private List users;
	
	public List getUsers () {
		return users;
	}
	
	public void setUsers (List users) {
		this.users = users;
	}
	
	private boolean newUser = false;
	
	public boolean isNewUser () {
		return newUser;
	}
	
	public void setNewUser (boolean newUser) {
		this.newUser = newUser;
	}

	private String username;
	
	public String getUsername () {
		return username;
	}
	
	public void setUsername (String username) {
		this.username = username;
	}
	
	private String firstname;
	
	public String getFirstname () {
		return firstname;
	}
	
	public void setFirstname (String firstname) {
		this.firstname = firstname;
	}
	
	private String lastname;
	
	public String getLastname () {
		return lastname;
	}
	
	public void setLastname (String lastname) {
		this.lastname = lastname;
	}
	
	private boolean canAssignCollectionAccess = false;
	
	public boolean getCanAssignCollectionAccess () {
		return canAssignCollectionAccess;
	}
	
	public void setCanAssignCollectionAccess (boolean can) {
		canAssignCollectionAccess = can;
	}
	
	public boolean userHasAdminRole = false;
	
	public boolean getUserHasAdminRole () {
		return userHasAdminRole;
	}
	
	public void setUserHasAdminRole (boolean has) {
		this.userHasAdminRole = has;
	}
	
	public String getFullname () {
		String fn = (firstname == null || firstname.trim().length() == 0) ? "???" : firstname;
		String ln = (lastname == null || lastname.trim().length() == 0) ? "???" : lastname;
		return fn + " " + ln;
	}	
	
	private String institution;
	
	public String getInstitution () {
		return institution;
	}
	
	public void setInstitution (String institution) {
		this.institution = institution;
	}
	
	private String department = null;

	public String getDepartment() {
		return (this.department);
	}

	public void setDepartment(String department) {
		this.department = department;
	}
	
	private String email;
	
	public String getEmail () {
		return email;
	}
	
	public void setEmail (String email) {
		this.email = email;
	}
	
	private String pass;
	
	public String getPass () {
		return pass;
	}
	
	public void setPass (String pass) {
		this.pass = pass;
	}	
	
	private String pass2;
	
	public String getPass2 () {
		return pass2;
	}
	
	public void setPass2 (String pass2) {
		this.pass2 = pass2;
	}	
	
	public List getUserDisplayNames () {
		UserManager userManager = 
			(UserManager) servlet.getServletContext().getAttribute("userManager");
		return userManager.getUserDisplayNames();
	}
	
	private String dupUsername;;
	
	public String getDupUsername () {
		return dupUsername;
	}
	
	public void setDupUsername (String dup) {
		dupUsername = dup;
	}
	
	
	private String ldapUserInfo = null;
	
	public String getLdapUserInfo () {
		return ldapUserInfo;
	}
	
	public void setLdapUserInfo (String s) {
		this.ldapUserInfo = s;
	}
	
	public boolean getPasswordRequired () {
		return (this.getFileLoginEnabled() && AuthUtils.getConfiguredLoginModules().size() == 1);
	}
	
	public boolean getFileLoginEnabled () {
		return AuthUtils.loginModuleEnabled("org.dlese.dpc.schemedit.security.login.FileLogin");
	}
	
	public boolean getPasswordOptional () {
		return (this.getFileLoginEnabled() && AuthUtils.getConfiguredLoginModules().size() > 1);
	}
	
	/**
	* Ensure that values are present for required fields before passing control to UserInfoAction.
	*/
	public ActionErrors validate(ActionMapping mapping,
                                 HttpServletRequest request) {
        ActionErrors errors = new ActionErrors();

		UserManager userManager = 
			(UserManager) servlet.getServletContext().getAttribute("userManager");
			
		// bypass for async calls for directory info
		if ("ldapUserInfo".equals(command) || "ucasUserInfo".equals(command)) {
			return errors;
		}
			
		// username is always required
		if (!"create".equals(command) && (username == null || username.trim().length() == 0)) {
			errors.add("error",
				new ActionError("generic.error", "username required"));
		}
		
		if ("save".equals(command)) {

			if (isNewUser() && userManager.getUser (username) != null) {
				// prtln (userManager.getUser(username).toString());
				errors.add("error",
					new ActionError("generic.error", "username \"" + username + "\" is already taken"));
				setDupUsername (username);
				setUsername (null);
			}
			else {
				setDupUsername ("");
			}

			if (firstname.trim().length() == 0)
				errors.add ("error", new ActionError ("generic.error", "fistname is required"));
			
			if (lastname.trim().length() == 0)
				errors.add ("error", new ActionError ("generic.error", "lastname is required"));	
			
 			if (this.getPasswordRequired() || 
				(pass != null && pass.trim().length() > 0) || 
				(pass2 != null && pass2.trim().length() > 0 )) {
				if (pass.trim().length()==0)
					errors.add ("error", new ActionError ("generic.error", "must supply a password"));
				else if (!pass.equals (pass2)) 
					errors.add ("error", new ActionError ("generic.error", "passwords do not match"));
			}
		}
		
		setUsers(userManager.getUsers());
		
        return errors;
    }	
	
	static void prtln(String s) {
		while (s.length() > 0 && s.charAt(0) == '\n') {
			System.out.println ("");
			s = s.substring(1);
		}
		System.out.println("UserInfoForm: " + s);
	}
	

	
}
