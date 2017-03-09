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
package org.dlese.dpc.xml;

import java.util.Comparator;
import org.dom4j.Node;

/**
 *  Compares two dom4j nodes using an xPath. Note that a dom4j Node List can be sorted using the native
 *  method: <code>List sortedList = document.selectNodes("/ListSets/set","setName");</code>
 *
 * @author     John Weatherley
 * @version    $Id: Dom4jNodeListComparator.java,v 1.2 2009/03/20 23:34:01 jweather Exp $
 */
public class Dom4jNodeListComparator implements Comparator {
	/**  Sort ascending. */
	public final static int ASCENDING = 0;
	/**  Sort descending. */
	public final static int DESCENDING = 1;

	int order = DESCENDING;
	String xPath = null;


	/**
	 *  Constructor that allows setting the sort order by ascending or descending.
	 *
	 * @param  sortOrder  The order for sorting (ASCENDING or DESCENDING)
	 * @param  xPath      An xPath relative to the node root
	 * @see               #ASCENDING
	 * @see               #DESCENDING
	 */
	public Dom4jNodeListComparator(String xPath, int sortOrder) {
		order = sortOrder;
		this.xPath = xPath;
	}


	/**
	 *  Default constructor that sorts descending.
	 *
	 * @param  xPath  An xPath relative to the node root
	 */
	public Dom4jNodeListComparator(String xPath) {
		this.xPath = xPath;

	}


	/**
	 *  Compares two dom4j Nodes for sorting by an xPath.<p>
	 *
	 *  Compares its two arguments for order. Returns a negative integer, zero, or a positive integer as the
	 *  first argument is less than, equal to, or greater than the second.
	 *
	 * @param  o1  The first Object.
	 * @param  o2  The second Object.
	 * @return     An int indicating sort order.
	 */
	public int compare(Object o1, Object o2) {
		Node node1;
		Node node2;

		if (order == DESCENDING) {
			node1 = (Node) o1;
			node2 = (Node) o2;
		}
		else {
			node1 = (Node) o2;
			node2 = (Node) o1;
		}

		Object oo1 = node1.valueOf(xPath);
		Object oo2 = node2.valueOf(xPath);

		return oo1.toString().compareTo(oo2.toString());
	}
}

