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
package org.dlese.dpc.standards.asn;

import org.dom4j.Element;

import org.dlese.dpc.xml.Dom4jUtils;
import org.dlese.dpc.xml.XPathUtils;
import org.dlese.dpc.util.strings.FindAndReplace;
import java.util.regex.*;

import java.util.*;

/**
 *  Extends AsnStandard to provide custom rendering of display text for AAAS standards.
 *
 * @author    Jonathan Ostwald
 */
public class AAASBenchmark extends AsnStandard {

	private static boolean debug = false;


	/**
	 *  Constructor for the AAASBenchmark object
	 *
	 * @param  e       NOT YET DOCUMENTED
	 * @param  asnDoc  NOT YET DOCUMENTED
	 */
	public AAASBenchmark(AsnStatement asnStmnt, AsnDocument asnDoc) {
		super(asnStmnt, asnDoc);
	}


	/**
	 *  Gets the text attribute of the AsnStandard object
	 *
	 * @return    The text value
	 */
	public String getDescription() {
		String description = super.getDescription();
		if (this.isLeaf())
			return stripLeadingInfo (description);
		else
			return description;
	}


	/**
	 *  Remove unwanted stuff from beginning of standard text (only for leaves!)<P>
	 *
	 *  before: "1A (3-5) #1 Results of similar ... "<br/>
	 *  after: "Results of similar ... "
	 *
	 * @param  s  NOT YET DOCUMENTED
	 * @return    NOT YET DOCUMENTED
	 */
	String stripLeadingInfo(String s) {
 		if (!this.isLeaf())
			return s;
		Pattern p = Pattern.compile(".*#[0-9] (.*)");
		Matcher m = p.matcher(s);
		if (m.find()) {
			// prtln ("Found");
			return m.group(1).trim();
		}
		return s;
	}


	/**
	 *  walk the ancestor list, adding text from each
	 *
	 * @return    The displayText value
	 */
	public String getDisplayText() {

		String s = "";
		List aList = getAncestors();
		for (int i = 0; i < aList.size(); i++) {
			AsnStandard std = (AsnStandard) aList.get(i);
			// prtln("std: " + std.getId() + "(" + std.isLeaf() + ")");
			s += std.getDescription();
			s += ": ";
		}
		s += this.getDescription();

		s = FindAndReplace.replace(s, "<br>", "\n", true);
		return removeEntityRefs(s);
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  s  NOT YET DOCUMENTED
	 */
	private static void prtln(String s) {
		if (debug) {
			System.out.println("AsnStandard: " + s);
		}
	}
}

