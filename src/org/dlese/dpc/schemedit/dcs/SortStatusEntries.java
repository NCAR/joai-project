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
package org.dlese.dpc.schemedit.dcs;

import java.util.Comparator;
import java.util.Date;
import org.dlese.dpc.util.MetadataUtils;
import java.text.*;

/**
 *  
 Comparator to sort {@link org.dlese.dpc.schemedit.dcs.StatusEntry}
	  elements in reverse order of their "dateChanged" property. <p>

 * @author    Jonathan Ostwald
  $Id: SortStatusEntries.java,v 1.2 2009/03/20 23:33:56 jweather Exp $
 */
public class SortStatusEntries implements Comparator {
	
	/**
	 *  Provide comparison for sorting Sugest a URL Records by  "lastModified" property
	 *
	 * @param  o1  document 1
	 * @param  o2  document 2
	 * @return     DESCRIPTION
	 */
	public int compare(Object o1, Object o2) {
		Date dateOne;
		Date dateTwo;
		try {
			dateOne = ((StatusEntry) o1).getDate();
			dateTwo = ((StatusEntry) o2).getDate();
		} catch (Exception e) {
			prtlnErr("Error: unable to find last modified date: " + e.getMessage());
			return 0;
		}
		return dateTwo.compareTo (dateOne);
	}
	
	private static void prtln(String s) {
		System.out.println(s);
	}


	private static void prtlnErr(String s) {
		System.err.println(s);
	}
	
}

