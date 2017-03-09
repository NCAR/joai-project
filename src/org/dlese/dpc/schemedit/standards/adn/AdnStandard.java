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
package org.dlese.dpc.schemedit.standards.adn;

import java.util.Comparator;

/**
 *  Represents an controlled vocabulary item from the ADN educational metadata standard. ADN represents
 *  standards as strings delimited by semi-colons, which serve to break the text up into "levels".
 *  This class provides access to the string representation for each level.
 *
 * @author     ostwald<p>
 *
 */
public class AdnStandard {
	private boolean debug = true;
	private String text = "";
	String[] splits = null;


	/**
	 *  Constructor for the AdnStandard object given a ResultDoc instance
	 *
	 * @param  text    NOT YET DOCUMENTED
	 */
	public AdnStandard(String text) {
		this.text = text;
		splits = text.split(":");
	}

	public String getGradeRange () {
		return getSplit (1);
	}

	/**
	 *  Gets the matchKey attribute of the AdnStandard object
	 *
	 * @return    The matchKey value
	 */
	public String getMatchKey() {
		// return getLeaf().toLowerCase();
		String text = getLeaf().toLowerCase();
		int i = text.indexOf(".");
		if (i > -1)
			text = text.substring(0, i);		
		return text + "(" + this.getGradeRange() + ")";
	}


	/**
	 *  Gets the text attribute of the AdnStandard object, that is the entire string
	 used to define this vocab item. The text attribute is colon-delimited to express
	 hierarchical relations.
	 *
	 * @return    The text value
	 */
	public String getText() {
		return text;
	}


	/**
	 *  Gets the specified segment of the colon-delimited text field.
	 *
	 * @param  level  NOT YET DOCUMENTED
	 * @return        The split value
	 */
	public String getSplit(int level) {
		return splits[level];
	}


	/**
	 *  Gets the levels attribute of the AdnStandard object
	 *
	 * @return    The levels value
	 */
	public int getLevels() {
		return splits.length;
	}


	/**
	 *  Gets the last segment of the text attribute.
	 *
	 * @return    The leaf value
	 */
	public String getLeaf() {
		return splits[getLevels() - 1];
	}


	/**
	 *  Description of the Method
	 *
	 * @return    Description of the Return Value
	 */
	public String toString() {
		String s = "";
		String sep = "\n";
		for (int i = 0; i < splits.length; i++) {
			s += sep + splits[i];
			sep += "\t";
		}
		return s;
	}


	/**
	 *  Gets the comparator attribute of the AdnStandard object
	 *
	 * @return    The comparator value
	 */
	public StandardComparator getComparator() {
		return new StandardComparator();
	}


	/**
	 *  Description of the Class
	 *
	 * @author     ostwald
	 */
	public class StandardComparator implements Comparator {

		/**
		 *  Description of the Method
		 *
		 * @param  o1  Description of the Parameter
		 * @param  o2  Description of the Parameter
		 * @return     Description of the Return Value
		 */
		public int compare(Object o1, Object o2) {
			/* 			System.out.println ("hello");
			String string1 = ((AdnStandard) o1).getId().toLowerCase();
			String string2 = ((AdnStandard) o2).getId().toLowerCase();
			return string2.compareTo(string1); */
			return 0;
		}
	}


	/**
	 *  Description of the Method
	 *
	 * @param  s  Description of the Parameter
	 */
	protected void prtln(String s) {
		if (debug) {
			System.out.println("AdnStandard: " + s);
		}
	}
}

