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

import java.io.*;
import java.util.*;
import org.dlese.dpc.schemedit.security.auth.TypedPrincipal;

/**
 *  Class to represent a User for purposes of password (file) based
 *  authentication.
 *
 * @author    Jonathan Ostwald
 * @see       FileLogin
 */
public class LoginUser {
	/**  place holder for password */
	public char password[];
	/**  place holder for principals */
	public Vector principals;


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @return    NOT YET DOCUMENTED
	 */
	public String toPasswdFileEntry() {
		TypedPrincipal p = (TypedPrincipal) principals.get(0);
		return (p.getName() + ":" + new String(password));
	}
}

