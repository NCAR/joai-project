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

import java.io.File;
import java.util.*;

import org.dom4j.Element;
import org.dom4j.Document;
import org.dom4j.DocumentFactory;

import org.dlese.dpc.xml.Dom4jUtils;
import org.dlese.dpc.xml.schema.DocMap;
import org.dlese.dpc.xml.XMLFileFilter;
import org.dlese.dpc.schemedit.config.AbstractConfigReader;

import org.dlese.dpc.schemedit.SchemEditUtils;
import org.dlese.dpc.schemedit.security.access.Roles;

/**
 *  Manages {@link User} instances and provides information about users
 *  inlcuding roles, attributes, preferences, etc.<p>
 *
 *  Reads user data from disk as XML Files, provides run-time services to suport
 *  UI, and authentication
 *
 * @author    Jonathan Ostwald
 */
public class UserManager {

	static boolean debug = false;

	Map userMap = null;
	File userDataDir = null;


	/**
	 *  Constructor for the UserManager object
	 *
	 * @param  userDataDir    directory containing XML files of user data
	 * @exception  Exception  if the directory cannot be processed
	 */
	public UserManager(File userDataDir) throws Exception {
		this.userDataDir = userDataDir;

		load();
		prtln("User Manager intitialized with " + userMap.size() + " entries");
	}


	/**  Reads the data dir, constructs user instances, and registers them */
	public void load() {
		userMap = new TreeMap();
		if (userDataDir == null || !userDataDir.exists()) {
			String msg = "user directory does not exist at " + userDataDir;
			// throw new Exception (msg);
			prtln("\n\n *** ERROR: " + msg + " ***\n\n");
			return;
		}

		prtln("\nLoading User data from " + userDataDir);

		File[] userFiles = userDataDir.listFiles(new XMLFileFilter());
		prtln("about to process " + userFiles.length + " user files");
		for (int i = 0; i < userFiles.length; i++) {
			prtln("\nProcessing user file (" + (i + 1) + " of " + userFiles.length + ") : " + userFiles[i].getName());
			try {
				User user = new User(userFiles[i]);
				register(user);
			} catch (Exception e) {
				String errorMsg = userFiles[i].getName() + " user NOT registered: " + e.getMessage();
				prtln("ERROR: " + errorMsg + "\n");
				// loadErrors.add(xmlFormat, e.getMessage());
			}
		}
	}


	/**
	 *  Gets the User instance having supplied username, or null if user cannot be
	 *  found.
	 *
	 * @param  username  the username
	 * @return           The user value
	 */
	public User getUser(String username) {
		return (username == null) ? null : (User) userMap.get(username);
	}


	/**
	 *  Create a new User instance for provided user name.
	 *
	 * @param  username       the username
	 * @return                a User instance
	 * @exception  Exception  if user for provided username exists.
	 */
	public User createUser(String username) throws Exception {
		if (this.userMap.containsKey(username))
			throw new Exception("User already exists with username: " + username);

		User newUser = new User();
		newUser.setUsername(username);
		File newFile = new File(this.userDataDir, username + ".xml");
		newUser.setSource(newFile);
		newUser.flush();
		this.register(newUser);
		return newUser;
	}


	/**
	 *  Remove user associated with provided username from registry
	 *
	 * @param  username  username of user to delete
	 */
	public void deleteUser(String username) {
		User user = getUser(username);
		if (user == null)
			return;
		this.unregister(user);
		File deletedUser = new File(user.getSource().getParentFile(),
			username + ".deleted");
		user.getSource().renameTo(deletedUser);
		user.destroy();
	}


	/**
	 *  Writes data for specified user to disk as XML file
	 *
	 * @param  user           user to be saved
	 * @exception  Exception  if provided user does not have a username
	 */
	public void saveUser(User user) throws Exception {
		if (user.getSource() == null) {
			String username = user.getUsername();
			if (username.trim().length() == 0)
				throw new Exception("cannot save user because no username is defined for it");
			else {
				File newFile = new File(this.userDataDir, username + ".xml");
				user.setSource(newFile);
			}
		}
		user.flush();
		this.register(user);
	}


	/**
	 *  Returns a list of all user instances managed by this UserManager.
	 *
	 * @return    The users value
	 */
	public List getUsers() {
		return getUsers(Roles.NO_ROLE);
	}


	/**
	 *  Returns all users having role equal to or below maxRole.
	 *
	 * @param  maxRole  NOT YET DOCUMENTED
	 * @return          The users value
	 */
	public List getUsers(Roles.Role maxRole) {
		List users = new ArrayList();
		for (Iterator i = userMap.keySet().iterator(); i.hasNext(); ) {
			String username = (String) i.next();
			User user = getUser(username);

			if (user.hasRole(maxRole)) {
				users.add(getUser(username));
			}
		}
		Collections.sort(users, new UserNameComparator());
		return users;
	}


	/**
	 *  Adds provided User instance to the managed users.
	 *
	 * @param  user  The feature to be added to the User attribute
	 */
	public synchronized void register(User user) {
		userMap.put(user.getUsername(), user);
		prtln("registered: " + user.getUsername());
	}


	/**
	 *  Removes provided user from the managed users.
	 *
	 * @param  user  User to unregister
	 */
	public synchronized void unregister(User user) {
		userMap.remove(user.getUsername());
		// destroy reader
		// destroy file
	}


	/**
	 *  Write user data to disk.<p>
	 *
	 *  First creates Document containing all user data, then reinitializes docMap
	 *  with the Document so it will be written to disk.
	 *
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public synchronized void flush() throws Exception {

		for (Iterator i = getUsers().iterator(); i.hasNext(); ) {
			User user = (User) i.next();
			user.flush();
		}
	}


	/**
	 *  The main program for the UserManager class
	 *
	 * @param  args           The command line arguments
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public static void main(String[] args) throws Exception {
		org.dlese.dpc.schemedit.test.TesterUtils.setSystemProps();
		System.out.println("Hello World");
		// String path = "C:/tmp/users.xml";
		String dir = "/Users/ostwald/tmp/ncs_user_records";

		UserManager um = new UserManager(new File(dir));

		try {
			um.createUser("fooberry");
		} catch (Exception e) {
			prtln("WARNING: " + e.getMessage());
		}
		um.deleteUser("fooberry");
		um.showUsers();

		// prtln (Dom4jUtils.prettyPrint (um.getDocMap().getDocument()));

		/*
		 *  for (Iterator i=um.readUsers().iterator();i.hasNext();) {
		 *  / prtln ( ((User)i.next()).toString());
		 *  User user = (User)i.next();
		 *  prtln ("\n" + user.getUsername());
		 *  for (Iterator ii=Roles.roles.iterator();ii.hasNext(); ) {
		 *  Roles.Role r = (Roles.Role)ii.next();
		 *  prtln ("\t has role " + r + ": " + user.hasRole (r));
		 *  }
		 *  }
		 */
	}


	/**
	 *  Return a listing of users including username and fullNames.
	 *
	 * @return    The userDisplayNames value
	 */
	public List getUserDisplayNames() {
		List users = getUsers();
		List names = new ArrayList();
		for (Iterator i = users.iterator(); i.hasNext(); ) {
			User user = (User) i.next();
			names.add("<b>" + user.getUsername() + "</b> (" + user.getFullName() + ")");
		}
		return names;
	}


	/**  Debugging method to print string representation of all managed users */
	public void showUsers() {
		List users = getUsers();
		prtln("UserManager: " + users.size() + " users");
		for (Iterator i = users.iterator(); i.hasNext(); ) {
			User user = (User) i.next();
			prtln("\n" + user.toString());
		}
	}


	/**  Destroy this Usermanager and all the managed users. */
	public void destroy() {
		prtln("destroy not yet implemented");
		prtln("detroying registered user instances");
		for (Iterator i = getUsers().iterator(); i.hasNext(); ) {
			User user = (User) i.next();
			user.destroy();
		}
	}

	public static void setDebug (boolean bool) {
		debug = bool;
	}

	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  s  NOT YET DOCUMENTED
	 */
	protected static void prtln(String s) {
		if (debug) {
			SchemEditUtils.prtln(s, "UserManager");
		}
	}

}

