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

import java.util.Comparator;
import java.io.Serializable;

/**
 *  Sort Users by userName.
 *
 * @author    Jonathan Ostwald
 */

public class UserNameComparator implements Comparator, Serializable {

	/**
	 *  Compare (case insensitive) by usernames
	 *
	 * @param  o1  NOT YET DOCUMENTED
	 * @param  o2  NOT YET DOCUMENTED
	 * @return     NOT YET DOCUMENTED
	 */
	public int compare(Object o1, Object o2) {
		User u1 = (User) o1;
		User u2 = (User) o2;

		try {
			return u1.getUsername().toUpperCase().compareTo(u2.getUsername().toUpperCase());
		} catch (Throwable t) {
			return 0;
		}
	}
}

