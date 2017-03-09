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
 *  Extends AsnStandard to provide custom "description" for Colorado Benchmarks.
 *
 * @author    Jonathan Ostwald
 */
public class ColoradoBenchmark extends AsnStandard {

	private static boolean debug = false;


	/**
	 *  Constructor for the ColoradoBenchmark object
	 *
	 * @param  e       NOT YET DOCUMENTED
	 * @param  asnDoc  NOT YET DOCUMENTED
	 */
	public ColoradoBenchmark(AsnStatement asnStmnt, AsnDocument asnDoc) {
		super(asnStmnt, asnDoc);
	}


	/**
	 *  tester program for the ColoradoBenchmark class
	 *
	 * @param  args           The command line arguments
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public static void main(String[] args) throws Exception {
		String in = "Standard 44: ";
		// String in = "2. ";
		// prtln (getNumber (in, 1));
		// prtln (getNumber (in, 2));

		in = "2. the theory of plate tectonics he";
		prtln("in: " + in);
		prtln("non number: \"" + getNonNumberText(in, 1) + "\"");
		prtln("non number: \"" + getNonNumberText(in, 2) + "\"");

		in = "Standard 32: The rain in spain ... ";
		prtln("in: " + in);
		prtln("non number: \"" + getNonNumberText(in, 1) + "\"");
		prtln("non number: \"" + getNonNumberText(in, 2) + "\"");

	}

	/* patterns for working with colorado benchmark numbering scheme */
	static String level1Pattern = "Standard ([\\d]+):";
	static String level2Pattern = "([\\d]+).";
	
	/**
	* Returns a pattern depending on the specified level
	*/
	static Pattern getPattern(int level) {
		if (level == 1)
			return Pattern.compile(level1Pattern);
		if (level == 2)
			return Pattern.compile(level2Pattern);
		return null;
	}


	/**
	 *  Gets the description attribute of the ColoradoBenchmark object
	 *
	 * @return    The description value
	 */
	public String getDescription() {
		int level = this.getLevel();
		String baseText = this.getAsnStatement().getDescription();
		if (level != 2)
			return baseText;

		AsnStandard parent = this.getParentStandard();
		String parentNum = this.getNumber(parent);
		String myNum = this.getNumber(this);
		if (parentNum != null && myNum != null) {
			return parentNum + "." + myNum + ". " +
				getNonNumberText(baseText, level);
		}
		return baseText;
	}


	/**
	* Extracts the number for this standard, using a regular expression determined by
	* the standard's level
	*/
	static String getNumber(AsnStandard std) {
		return getNumber(std.getAsnStatement().getDescription(), std.getLevel());
	}


	/**
	 *  using regular expression like this is not all that solid (what if there are
	 *  other occurances)?
	 *
	 * @param  input  NOT YET DOCUMENTED
	 * @param  level  NOT YET DOCUMENTED
	 * @return        The number value
	 */
	static String getNumber(String input, int level) {
		try {
			return getNumber(input, getPattern(level));
		} catch (Exception e) {
			prtln("getNumber: " + e.getMessage());
		}
		return null;
	}


	static String getNonNumberText(String input, int level) {
		try {
			return getNonNumberText(input, getPattern(level));
		} catch (Exception e) {
			prtln("getNumber: " + e.getMessage());
		}
		return null;
	}


	/**
	* Removes number, matched by the pattern, form the provided input
	*/
	static String getNonNumberText(String input, Pattern p) {
		Matcher m = p.matcher(input);
		if (m.find() && m.start() == 0) {
			return input.substring(m.end()).trim();
		}
		else {
			prtln("NOPE");
		}
		return null;
	}


	/**
	* Use supplied pattern to extract a number from the provided input string.
	*/
	static String getNumber(String input, Pattern p) {
		Matcher m = p.matcher(input);
		if (m.find() && m.start() == 0) {
			// prtln ("found: \"" + m.group(1));
			return m.group(1);
		}
		else {
			// prtln ("NOPE");
		}
		return null;
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

