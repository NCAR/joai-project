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
package org.dlese.dpc.util;

import java.util.Comparator;
import java.io.File;

/**
 *  Compares two Files based on their modification date.
 *
 * @author     John Weatherley
 * @version    $Id: FileModDateComparator.java,v 1.2 2009/03/20 23:34:00 jweather Exp $
 */
public class FileModDateComparator implements Comparator {
	/**  Sort by newest first. */
	public final static int NEWEST_FIRST = 0;
	/**  Sort by oldest first. */
	public final static int OLDEST_FIRST = 1;

	int order = NEWEST_FIRST;


	/**
	 *  Constructor that allows setting the sort order by newest or oldest first.
	 *
	 * @param  sortOrder  The order for sorting (NEWEST_FIRST or OLDEST_FIRST)
	 * @see               #NEWEST_FIRST
	 * @see               #OLDEST_FIRST
	 */
	public FileModDateComparator(int sortOrder) {
		order = sortOrder;
	}


	/**  Default constructor that sorts by newest first. */
	public FileModDateComparator() { }


	/**
	 *  Compares two Files for sorting by their modification date.<p>
	 *
	 *  Compares its two arguments for order. Returns a negative integer, zero, or a positive integer as the
	 *  first argument is less than, equal to, or greater than the second.
	 *
	 * @param  o1  The first Object.
	 * @param  o2  The second Object.
	 * @return     An int indicating sort order.
	 */
	public int compare(Object o1, Object o2) {
		File file1;
		File file2;

		if (order == NEWEST_FIRST) {
			file1 = (File) o1;
			file2 = (File) o2;
		}
		else {
			file1 = (File) o2;
			file2 = (File) o1;
		}

		if (file1.lastModified() < file2.lastModified())
			return 1;
		else if (file1.lastModified() > file2.lastModified())
			return -1;
		else
			return 0;
	}
}

