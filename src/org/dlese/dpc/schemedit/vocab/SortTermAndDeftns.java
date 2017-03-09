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
package org.dlese.dpc.schemedit.vocab;

import java.util.Comparator;

/**
 *  Comparator to sort Records in reverse order of their creation date
 *
 * @author    Jonathan Ostwald
 */
public class SortTermAndDeftns implements Comparator {
	
	/**
	 *  Provide comparison for sorting Sugest a URL Records by reverse "creation Date"
	 *
	 * @param  o1  document 1
	 * @param  o2  document 2
	 * @return     DESCRIPTION
	 */
	public int compare(Object o1, Object o2) {
		String s1;
		String s2;
		try {
			s1 = ((TermAndDeftn) o1).getTerm();
			s2 = ((TermAndDeftn) o2).getTerm();
			return s1.compareTo(s2);
		} catch (Exception e) {
			System.out.println ("SortTermAndDeftns error: " + e.getMessage());
			return 0;
		}
	}
}

