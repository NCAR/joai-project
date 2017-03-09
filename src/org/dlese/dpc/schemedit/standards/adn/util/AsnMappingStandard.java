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
package org.dlese.dpc.schemedit.standards.adn.util;

import org.dlese.dpc.standards.asn.AsnStandard;
import org.dlese.dpc.standards.asn.AsnStatement;
import org.dlese.dpc.xml.Dom4jUtils;
import org.dlese.dpc.xml.XPathUtils;
import org.dlese.dpc.util.strings.FindAndReplace;
import java.util.regex.*;

import org.dom4j.Element;
import java.util.*;

/**
 *  Extends AsnStandard with methods to aid the task of mapping asn standards to ADN standards.
 *
 * @author     Jonathan Ostwald
 */
public class AsnMappingStandard extends AsnStandard {

	private static boolean debug = false;
	
	/**
	 *  Constructor for the AsnMappingStandard object
	 *
	 * @param  e    NOT YET DOCUMENTED
	 * @param  map  NOT YET DOCUMENTED
	 */
	public AsnMappingStandard(AsnStatement asnStmnt, AsnMappingDocument asnDoc) {
		super (asnStmnt, asnDoc);
	}
	
	public String getMatchKey () {
		String text = getDescription().toLowerCase();
		int i = text.indexOf(".");
		if (i > -1)
			text = text.substring(0, i);
		
		return text + "(" + this.getGradeRange() + ")";
		
	}

	/**
	 *  Gets the displayText attribute of the AsnMappingStandard object
	 *
	 * @param  level  NOT YET DOCUMENTED
	 * @return        The displayText value
	 */
	private String getDisplayText(int level) {
		String text = getDescription();
		if (level == 1) {
			// String pat = "all students should develop";
			String pat = "all students should develop understanding of";
			int x = text.indexOf(pat);
			if (x != -1)
				return text.substring(0, x + pat.length());
			else {
				pat = "all students should develop";
				x = text.indexOf(pat);
					if (x != -1)
						return text.substring(0, x + pat.length());
					else
						return "**  pattern not found **";
			}
		}
		return text;
	}

	/**
	 *  walk the ancestor list, adding text from each
	 *
	 * @return    The displayText value
	 */
	public String getDisplayText() {
		// from the level 1 ancestor, get everything up to "students should develop"

		String s = "";
		List aList = getAncestors();
		for (int i = 0; i < aList.size(); i++) {
			AsnMappingStandard std = (AsnMappingStandard) aList.get(i);
			s += std.getDisplayText(std.getLevel());
			s += ":";
		}
		s += this.getDisplayText(this.getLevel());

		s = FindAndReplace.replace(s, "<br>", "\n", true);
		return removeEntityRefs (s);
	}

	public String getAdnText () {
		try {
			if (!isLeaf())
				throw new Exception ("standard must be a leaf");
			
			if (getLevel() < 2)
				throw new Exception ("level must be 3 or greater");
			
			List segments = new ArrayList ();
			segments.add ("NSES");
			segments.add (getGradeRange());
			
			AsnMappingStandard std = (AsnMappingStandard) getAncestors().get(0);
			String text = std.getDescription();
			prtln ("level 0: " + text);
			String [] splits = text.split("<br>");
			if (splits.length >= 2) {
			
				// stdLetter will be of this form: "Content Standard E:"
				String stdLetter = splits[1].trim();
				// remove trailing semi colon
				if (stdLetter.charAt(stdLetter.length()-1) == ':')
					stdLetter = stdLetter.substring(0, stdLetter.length()-1);
				
				// stdTopic will be something like: "Science and Technology"
				String stdTopic = splits[0].trim();
				
				prtln ("\n  stdLetter: " + stdLetter);
				prtln ("\n  stdTopic: " + stdTopic);
				segments.add (stdLetter + " " + stdTopic + " Standards");
			}
			else
				throw new Exception ("could not split level 0 text (" + text + ")");
			
			std = (AsnMappingStandard) getAncestors().get(1);
			text = std.getDescription(); 
			segments.add (text);
			segments.add (getDescription());
			
			String s = "";
			for (int i=0;i<segments.size();i++) {
				s += (String)segments.get(i);
				if (i < segments.size() - 1)
					s += ":";
			}
			return s;
		} catch (Exception e) {
			prtln ("getAdnText error: " + e.getMessage());
			return "";
		}
	}

	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @return    NOT YET DOCUMENTED
	 */
	public String toString() {
		// return Dom4jUtils.prettyPrint (element);
		String s = "\n" + getId();
		s += "\n\t" + "level: " + getLevel();
		s += "\n\t" + "gradeLevel: " + getStartGradeLevel() + " - " + getEndGradeLevel();
		s += "\n\t" + "isChildOf: " + getParentId();
		s += "\n\t" + "itemText: " + getDescription();
		s += "\n\n\t" + getDisplayText();
		return s;
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  s  NOT YET DOCUMENTED
	 */
	private static void prtln(String s) {
		if (debug) {
			System.out.println("AsnMappingStandard: " + s);
		}
	}
}
