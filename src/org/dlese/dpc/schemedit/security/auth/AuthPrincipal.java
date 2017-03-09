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

import java.io.*;
import java.security.Principal;

/**
	A principle for denoting which login module authenticated.
 */
public class AuthPrincipal extends TypedPrincipal implements Principal, Serializable
{
	public AuthPrincipal(String name, int type) {
		super (name, type);
	}

	/**
	 * Create a AuthPrincipal with a name.
	 *
	 * <p>
	 *
	 * @param name the name for this Principal.
	 * @exception NullPointerException if the <code>name</code>
	 * 			is <code>null</code>.
	 */
	 public AuthPrincipal(String name) {
		this(name, AUTH);
	}


	/**
	 * Create a AuthPrincipal with a blank name.
	 *
	 * <p>
	 *
	 */
	public AuthPrincipal()
	{
		this("", AUTH);
	}

}
