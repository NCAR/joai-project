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
package org.dlese.dpc.util.strings;

/**
 *  Description of the Class
 *
 * @author    ryandear
 */
public final class FindAndReplace
{

	/**
	 *  Given an input string, replace all occurences of a string sequence (reg.
	 *  ex.) with a given replacement string.
	 *
	 * @param  in               input string
	 * @param  find             regular expression to match against
	 * @param  replace          string to replace matches with
	 * @param  caseInsensitive  should matching be case insensitive?
	 * @return                  the newly altered string public static String
	 */
	public static String replace(String in,
			String find,
			String replace,
			boolean caseInsensitive) {
		String match;
		if (caseInsensitive) {
			match = in.toLowerCase();
			find = find.toLowerCase();
		}
		else {
			match = in;
		}
		StringBuffer ret = new StringBuffer();
		int ind1 = 0;
		int ind2 = match.indexOf(find);
		int findLength = find.length();
		while (ind2 > -1) {
			ret.append(in.substring(ind1, ind2))
					.append(replace);
			ind1 = ind2 + findLength;
			ind2 = match.indexOf(find, ind1);
		}
		ret.append(in.substring(ind1));
		return ret.toString();
	}
}

