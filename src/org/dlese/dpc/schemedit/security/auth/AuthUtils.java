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
package org.dlese.dpc.schemedit.security.auth;

import org.dlese.dpc.schemedit.SchemEditUtils;
import org.dlese.dpc.util.Files;

import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.security.*;
import javax.security.auth.*;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.AppConfigurationEntry;
import java.util.*;

import org.dlese.dpc.schemedit.security.auth.nsdl.NSDLLdapClient;
import org.dlese.dpc.ldap.LdapException;
import org.dlese.dpc.ldap.LdapEntry;

import org.dom4j.*;

/**
 * @author    Jonathan Ostwald
 */

public class AuthUtils {

	private static boolean debug = false;
	/**  NOT YET DOCUMENTED */
	public static NSDLLdapClient ldapClient = null;


	/**
	 *  Debugging - prints a string representation of provided Subject
	 *
	 * @param  _mySubject  the subject to display
	 */
	public static void showSubject(Subject _mySubject) {
		showSubject(_mySubject, null);
	}


	/**
	 *  Debugging - prints a string representation of provided Subjec
	 *
	 * @param  _mySubject  the subject to display
	 * @param  msg         a message to display along with subject
	 */
	public static void showSubject(Subject _mySubject, String msg) {
		System.out.println("\n(" + msg + ")");
		if (_mySubject != null)
			System.out.println(" ..." + _mySubject.toString() + "  (" +
				_mySubject.getPrincipals().size() + " principals)");
		else
			System.out.println("  ... no subject found in login Context");
		System.out.println("------------\n");
	}


	/**
	 *  Gets the configuredLoginModules as list of loginModule class names
	 *
	 * @return    The configuredLoginModules value
	 */
	public static List getConfiguredLoginModules() {
		return getConfiguredLoginModules(false);
	}


	/**
	 *  Gets (and prints) the configuredLoginModules as list of loginModule class
	 *  names
	 *
	 */
	public static void reportConfiguredLoginModules() {
		getConfiguredLoginModules(true);
	}


	/**
	 *  Gets the configuredLoginModules as list of loginModule class names,
	 *  optionally printing if "verbose" is true.
	 *
	 * @param  verbose  to display configured modules
	 * @return          The configuredLoginModules value
	 */
	public static List getConfiguredLoginModules(boolean verbose) {
		String report = "\nConfigured Login Modules";
		List loginModules = new ArrayList();
		try {
			Configuration config = Configuration.getConfiguration();
			if (config == null)
				throw new Exception("No configuration found");
			AppConfigurationEntry[] entries = config.getAppConfigurationEntry("NCS");
			if (entries == null)
				throw new Exception("No app config entries found");
			for (int i = 0; i < entries.length; i++) {
				AppConfigurationEntry entry = entries[i];
				String name = entry.getLoginModuleName();
				loginModules.add(name);
				String controlFlag = entry.getControlFlag().toString();
				report += "\n\t" + name + " (" + controlFlag + ")";
			}
		} catch (Throwable t) {
			String msg = "Error showing login modules: " + t.getMessage();
			prtlnErr(msg);
			report += "\n" + msg;
		}
		if (verbose)
			System.out.println(report + "\n");
		return loginModules;
	}


	/**
	 *  Gets the the specified AppConfigurationEntry from the login configuration
	 *
	 * @param  loginModuleClass  class name of loginModule to get
	 * @return                   The configuredLoginModule value
	 */
	public static AppConfigurationEntry getConfiguredLoginModule(String loginModuleClass) {
		Configuration config = Configuration.getConfiguration();
		try {
			if (loginModuleClass == null || loginModuleClass.trim().length() == 0)
				throw new Exception("no loginModuleClass provided");
			if (config == null)
				throw new Exception("No configuration found");
			AppConfigurationEntry[] entries = config.getAppConfigurationEntry("NCS");
			if (entries == null)
				throw new Exception("No app config entries found");
			for (int i = 0; i < entries.length; i++) {
				AppConfigurationEntry entry = entries[i];
				String name = entry.getLoginModuleName();
				if (loginModuleClass.equals(name))
					return entry;
			}
		} catch (Throwable e) {
			prtlnErr("getConfiguredLoginModule WARNING: " + e.getMessage());
		}
		return null;
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  loginModuleClass  NOT YET DOCUMENTED
	 * @return                   NOT YET DOCUMENTED
	 */
	public static boolean loginModuleEnabled(String loginModuleClass) {
		return (getConfiguredLoginModule(loginModuleClass) != null);
	}


	/**
	 *  Gets the passwordFile attribute of the AuthUtils class
	 *
	 * @return    The passwordFile value
	 */
	public static String getPasswordFile() {
		AppConfigurationEntry entry =
			getConfiguredLoginModule("org.dlese.dpc.schemedit.security.login.FileLogin");
		if (entry == null)
			return null;
		String path = null;
		try {
			path = ((String) entry.getOptions().get("pwdFile"));
		} catch (Throwable t) {
			prtlnErr("getPasswordFile could not get pwdFile option: " + t.getMessage());
		}
		return path;
	}


	/**
	 *  Searches an LDAP directory and returns results in the form of a
	 *  dom4j.Document. Used by UserInfoAction.<p>
	 *
	 *  NOTE: currently supports NSDL ldap only!
	 *
	 * @param  searchString   the search string
	 * @param  ldapField      either "cn" or "uid"
	 * @return                The ldapUserInfo value
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public static Document getLdapUserInfo(String searchString, String ldapField) throws Exception {
		prtln("getLdapUserInfo() searchString: " + searchString + "  ldapField: " + ldapField);
		if (ldapClient == null) {
			// throw Exception ("ldapClient not available");
			String path = "C:/mykeys/NsdlLdap.properties";
			ldapClient = new NSDLLdapClient(path);
		}

		String filter = null;
		if ("uid".equals(ldapField))
			filter = "uid=" + searchString;
		else if ("cn".equals(ldapField))
			filter = "cn=" + searchString;
		else
			throw new Exception("unrecognized ldapField: " + ldapField);

		LdapEntry[] entries = ldapClient.search(
			"ou=DefaultOrigin,dc=nsdl,dc=org",  // base = null: start at top of DB tree
		filter,  // filter
		new String[]{"cn", "mail"},  // attrNames = null: return all attributes
		0);

		if (entries == null)
			prtln("entries is null");
		else
			prtln(entries.length + " entries found");

		Element root = DocumentHelper.createElement("ldapInfo");
		if (entries != null && entries.length > 0) {
			for (int i = 0; i < entries.length; i++) {
				LdapEntry entry = entries[i];
				Element entryEl = root.addElement("entry");

				String uid = ldapClient.getUid(entry.getDn());
				if (uid == null) {
					prtln("got null uid for \"" + entry.getDn() + "\"");
					continue;
				}

				String name = getSingleAttrValue("cn", entry);
				String email = getSingleAttrValue("mail", entry);

				Element userEl = entryEl.addElement("uid");
				userEl.setText(uid);
				Element nameEl = entryEl.addElement("name");
				nameEl.setText(name != null ? name : "");
				Element mailEl = entryEl.addElement("email");
				mailEl.setText(email != null ? email : "");
			}
		}
		return DocumentHelper.createDocument(root);
	}


	static String getSingleAttrValue(String attrName, LdapEntry entry) {
		String[] vals = entry.getAttrStrings(attrName);
		if (vals != null && vals.length > 0)
			return vals[0];
		return null;
	}


	/**
	 *  Splits a string into tokens around ','
	 *
	 * @param  s  the string to split
	 * @return    a list of tokens
	 */
	public static List getTokens(String s) {
		return getTokens(s, ",");
	}


	/**
	 *  Splits a string into tokens around the provided delimiter
	 *
	 * @param  s          the string to split
	 * @param  delimiter  string to split by
	 * @return            a list of tokens
	 */
	public static List getTokens(String s, String delimiter) {
		List ret = new ArrayList();
		if (s == null)
			return ret;
		String[] tokens = s.split(delimiter);

		for (int i = 0; i < tokens.length; i++) {
			if (tokens[i].trim().length() > 0)
				ret.add(tokens[i].trim());
		}
		return ret;
	}


	/**
	 *  Joins a list of tokens into a comma-delimited string
	 *
	 * @param  tokens  list to be joined
	 * @return         string of joined values
	 */
	public static String joinTokens(List tokens) {
		return joinTokens(tokens, ",");
	}


	/**
	 *  Joins a list of tokens using provided delimiter.
	 *
	 * @param  tokens   list to be joined
	 * @param  joinStr  string to be inserted between tokens
	 * @return          string of joined tokens
	 */
	public static String joinTokens(List tokens, String joinStr) {
		String ret = "";
		if (tokens == null)
			return ret;
		for (Iterator i = tokens.iterator(); i.hasNext(); ) {
			ret += (String) i.next();
			if (i.hasNext())
				ret += joinStr;
		}
		return ret;
	}


	/**
	 *  NOT USED - returns true if provided subject has provided permission
	 *
	 * @param  subj  the subject
	 * @param  p     the permission
	 * @return       true if permitted
	 */
	public static boolean permitted(Subject subj, final Permission p) {
		if ((p == null)) {
			System.err.println("subj or perm is null");
			return false;
		}
		if (subj == null) {
			subj = new Subject();
		}
		final SecurityManager sm;
		if (System.getSecurityManager() == null) {
			sm = new SecurityManager();
		}
		else {
			sm = System.getSecurityManager();
		}
		System.err.println("trying to auth " + subj + " with permission " + p);
		try {
			Subject.doAsPrivileged(subj,
				new PrivilegedExceptionAction() {
					public Object run() {
						System.err.println("sm: " + sm);
						sm.checkPermission(p);
						return null;
					}
				}, null);
			return true;
		} catch (AccessControlException ace) {
			System.err.println("exception caught: " + ace);
			return false;
		} catch (PrivilegedActionException pae) {
			if (pae.getException() instanceof SecurityException) {
				System.err.println("exception caught: " + pae);
			}
			else {
				System.err.println("what the hell is this: " + pae);
			}
			return false;
		}

	}


	private static void prtln(String s) {
		if (debug)
			SchemEditUtils.prtln(s, "AuthUtils");
	}


	private static void prtlnErr(String s) {
		SchemEditUtils.prtln(s, "AuthUtils");
	}
}

