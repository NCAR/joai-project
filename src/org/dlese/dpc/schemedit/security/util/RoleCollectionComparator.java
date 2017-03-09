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
package org.dlese.dpc.schemedit.security.util;

import java.util.Comparator;
import java.io.Serializable;
import org.dlese.dpc.schemedit.security.access.Roles;

/**
 *  Sort the names of role-associated collections. Used to sort the UserRole map in 
 {@link org.dlese.dpc.schemedit.security.action.form.CollectionAccessForm} by collection name.
 *
 * @author    Jonathan Ostwald
 */

public class RoleCollectionComparator implements Comparator, Serializable {

	/**
	 *  A lexical comparison
	 *
	 * @param  o1  NOT YET DOCUMENTED
	 * @param  o2  NOT YET DOCUMENTED
	 * @return     NOT YET DOCUMENTED
	 */
	public int compare(Object o1, Object o2) {
		String s1 = (String) o1;
		String s2 = (String) o2;

		if (s1 == null)
			s1 = "";
		if (s2 == null)
			s2 = "";

		if (s1.equals(s2))
			return 0;

		return s1.compareTo(s2);
	}
}

