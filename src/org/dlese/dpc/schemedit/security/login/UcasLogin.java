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

import org.dlese.dpc.schemedit.security.auth.AuthPrincipal;
import org.dlese.dpc.schemedit.security.auth.UserPrincipal;
import org.dlese.dpc.schemedit.security.auth.ucas.MyUcasClient;

import edu.ucar.cisl.authenticator.client.AuthClientException;
import edu.ucar.cisl.authenticator.domain.Authentication;

/**
 *  LoginModule that uses UCAS auth service to Authenticate.
 *
 * @author    Jonathan Ostwald
 */
public class UcasLogin extends SimpleLogin {
	private static boolean debug = false;

	/**
	* Validate the user via MyUcasClient (which uses the information in it's propsFile
	* to authenticate itself to the UCAS Auth service).
	*/	
	protected synchronized Vector validateUser(String username, char password[]) throws LoginException {
		Authentication auth = null;
		try {
			prtln("calling MyUcasClient.getAuthentication");
			auth = MyUcasClient.getAuthentication("password", username, new String(password));
		} catch (AuthClientException ex) {
			System.out.println(ex.getMessage());
			throw new LoginException(ex.getMessage());
		}

		prtln ("result is " + auth.isValid());
		if (auth.isValid()) {
			prtln ("First Name is " + auth.getFirstName());
			prtln ("Last Name is " + auth.getLastName());
		}
		else {
			throw new FailedLoginException("Bad password");
		}

		Vector principals = new Vector();
		// principals.add(new UserPrincipal(username, TypedPrincipal.USER));
		principals.add(new UserPrincipal(username));
		principals.add(new AuthPrincipal(this.getLoginModuleName()));
		return principals;
	}


	/**
	 *  Intialize the UcasLogin using the propsFile obtained from the login config.
	 *
	 * @param  subject          NOT YET DOCUMENTED
	 * @param  callbackHandler  NOT YET DOCUMENTED
	 * @param  sharedState      NOT YET DOCUMENTED
	 * @param  options          NOT YET DOCUMENTED
	 */
	public void initialize(Subject subject, CallbackHandler callbackHandler, Map sharedState, Map options) {
		prtln("initialize");
		super.initialize(subject, callbackHandler, sharedState, options);
		String propsFile = getOption("propsFile", null);
		if (propsFile == null)
			throw new Error("UcasLogin config error: propsFile not supplied");
		prtln("propsFile: " + propsFile);
		MyUcasClient.setPropsPath(propsFile);

	}


	static void prtln(String s) {
		if (debug)
			System.out.println("UcasLogin: " + s);
	}
}

