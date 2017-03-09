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
package org.dlese.dpc.suggest.resource.urlcheck;

import org.dlese.dpc.schemedit.url.DupSim;
import java.util.Comparator;


/**
 *  Comparator to sort DupSim instances by order of their IDs.
 *
 *@author    ostwald<p>
 $Id $
 */
public class SimSorter implements Comparator {

	public static boolean debug = true;
	/**
	 *  Description of the Method
	 *
	 *@param  o1  Description of the Parameter
	 *@param  o2  Description of the Parameter
	 *@return     Description of the Return Value
	 */
	public int compare(Object o1, Object o2) {
		String string1 = ((DupSim) o1).getId().toLowerCase();
		String string2 = ((DupSim) o2).getId().toLowerCase();
		return string1.compareTo(string2);
	}
}

