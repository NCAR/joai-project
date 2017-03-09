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
package org.dlese.dpc.schemedit.security.login;

import java.util.Map;
import java.io.*;
import java.util.*;
import java.security.Principal;
import javax.security.auth.*;
import javax.security.auth.callback.*;
import javax.security.auth.login.*;
import javax.security.auth.spi.*;

import org.dlese.dpc.schemedit.SchemEditUtils;
import org.dlese.dpc.schemedit.security.auth.AuthPrincipal;

/**
* Login Module that authenticates against a password stored in a password file.
*/
public class FileLogin extends SimpleLogin
{
	private static boolean debug = false;
	
	private String              pwdFile;
	private long                lastModified    = 0;
	private Hashtable           users           = null;
	private PasswordHelper		passwordHelper  = null;

	/**
	* Read user data from pwd file
	*/
	private void load(File f) throws Exception
	{
		lastModified = f.lastModified();
		BufferedReader r = new BufferedReader(new FileReader(f));
		if (passwordHelper == null) {
			throw new Exception ("passwordHelper is not initialized");
		}
		users = passwordHelper.load();
	}

	/**
	* Load user data from pwdFile if necessary
	*/
	private void reload() throws Exception
	{
		File f = new File(pwdFile);
		if (users == null || f.lastModified() != lastModified)
		   load(f);
	}

	/**
	* loads pwdFile before validating User
	*/
	protected synchronized Vector validateUser(String username, char password[]) throws LoginException
	{
		prtln ("validateUser() username: " + username + "  password: " + new String (password));
		
		try {
			reload();
		} catch (Exception e) {
			throw new LoginException("Error reading " + pwdFile + " (" + e.getMessage() + ")");
		}

		if (users == null || !users.containsKey(username))
		   throw new AccountExpiredException("Unknown user");
		
		LoginUser u = (LoginUser) users.get(username);
		char pwd[];
		try {
			// pwd = Utils.cryptPassword(password);
			pwd = password;
		} catch (Exception e) {
			throw new LoginException("Error encoding password (" + e.getMessage() + ")");
		}
		int c;
		for (c = 0; c < pwd.length && c < u.password.length; c++)
			if (pwd[c] != u.password[c])
			   break;
		if (c != pwd.length || c != u.password.length) {
			prtln ("fileLogin could not verify password");
		   throw new FailedLoginException("Bad password");
		}
		prtln ("\n validateUser() SUCCEEDED!\n");
		u.principals.add (new AuthPrincipal(this.getLoginModuleName()));
		return u.principals;
	}
	
	/**
	* Initialize this login module using password file obtained from login config
	*/
	public void initialize(Subject subject, CallbackHandler callbackHandler, Map sharedState, Map options)
	{
		prtln ("\ninitialize");
		super.initialize(subject, callbackHandler, sharedState, options);

		pwdFile = getOption("pwdFile", null);
		if (null == pwdFile)
		   throw new Error("A password file must be named (pwdFile=?)");
		passwordHelper = PasswordHelper.getInstance(pwdFile);
	}
	
	static void prtln (String s) {
		if (debug)
			SchemEditUtils.prtln (s, "FileLogin");
	}
}
