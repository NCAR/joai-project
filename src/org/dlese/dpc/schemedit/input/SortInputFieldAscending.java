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
package org.dlese.dpc.schemedit.input;

import java.util.Comparator;
import org.dlese.dpc.xml.XPathUtils;

/**
 *  Comparator to order input fields in "natural order" of their xpath
 *  attribute.
 *
 * @author    ostwald<p>
 *
 *
 */
public class SortInputFieldAscending implements Comparator {

	/**
	 *  Compare InputField objects by their XPath fields (in "natural order"), only
	 *  taking indexing into account (e.g., "/record/general[1] comes before
	 *  "record/general[10]").
	 *
	 * @param  o1  InputField 1
	 * @param  o2  InputField 2
	 * @return     comparison of "xpath order"
	 */
	public int compare(Object o1, Object o2) {

		String s1 = ((InputField) o1).getXPath();
		String s2 = ((InputField) o2).getXPath();

		return XPathUtils.compare(s1, s2);
	}
}

