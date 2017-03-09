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
package org.dlese.dpc.schemedit.config;

import java.util.Comparator;

/**
 *  
 Comparator to sort {@link org.dlese.dpc.schemedit.config.StatusFlag}
	  instances natural order of their "label" property. <p>

 * @author    Jonathan Ostwald
  $Id: SortStatusFlags.java,v 1.3 2009/03/20 23:33:56 jweather Exp $
 */
public class SortStatusFlags implements Comparator {
	
	/**
	 *  Provide comparison for sorting StatusFlag by their label property
	 *
	 * @param  o1  StatusFlag 1
	 * @param  o2  StatusFlag 2
	 * @return     DESCRIPTION
	 */
	public int compare(Object o1, Object o2) {
			String string1 = ((StatusFlag) o1).getLabel().toLowerCase();
			String string2 = ((StatusFlag) o2).getLabel().toLowerCase();
		return string1.compareTo (string2);
	}

}

