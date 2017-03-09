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
package org.dlese.dpc.dds;

import java.util.Comparator;
import org.dlese.dpc.index.*;
import org.apache.lucene.document.*;
import org.apache.lucene.document.Document;

/**
 *  Description of the Class
 *
 * @author    ryandear
 */
public class SortDocsAlphabetically implements Comparator
{

	/**
	 *  Provide comparison for sorting Lucene documents alphabetically by "title"
	 *
	 * @param  o1  document 1
	 * @param  o2  document 2
	 * @return     DESCRIPTION
	 */
	public int compare(Object o1,
			Object o2) {
		String s1 = ((Document) o1).get("title");
		String s2 = ((Document) o2).get("title");
		if (s1.compareTo(s2) > 0) {
			return 1;
		}
		if (s1.equals(s2)) {
			return 0;
		}
		return -1;
	}
}

