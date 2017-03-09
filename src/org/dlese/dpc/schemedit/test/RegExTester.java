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
package org.dlese.dpc.schemedit.test;

import java.util.regex.*;
import java.util.*;
import java.net.*;

import org.dom4j.*;
import org.dlese.dpc.util.strings.FindAndReplace;
import org.dlese.dpc.xml.XPathUtils;

/**
 *  Class for testing pattern matching and regular expressinos
 *
 *@author     ostwald
 */
public class RegExTester {

	private static boolean debug = true;
		
	public static String FINAL_STATUS_TEMPLATE = "_|-final--|_";
	public static Pattern FINAL_STATUS_PATTERN = Pattern.compile("_\\|-final-([^\\s]+)-\\|_");

	public static String makeFinalStatusValue (String collection) {
		return FindAndReplace.replace (FINAL_STATUS_TEMPLATE, "--", "-" + collection + "-", false);
	}
	
	static String makeString () {
		String s = "<xs:complexType name=\"cdType\">";
		s += "\n" + "<xs:sequence>";
		s += "\n" + "<xs:element name=\"extnTest\" type=\"sh:extnTestType\"/>";
		s += "\n" + "<!-- <xs:element name=\"artist\" type=\"xs:string\"/> -->";
		s += "\n" + "<xs:element ref=\"cd:abstractElement\" minOccurs=\"2\" maxOccurs=\"2\"/>";
		s += "\n" + "<xs:element ref=\"cd:simpleAbstract\" minOccurs=\"2\" maxOccurs=\"2\"/>";
		
		s += "\n" + "<xs:element name=\"id\" type=\"sh:relationEntryType\"/>";
		s += "\n" + "<xs:element name=\"info\" type=\"sh:infoType\"/>";
		s += "\n" + "<xs:element name=\"media\" type=\"cd:mediaType\"/>";

		s += "\n" + "</xs:sequence>";
		
		s += "\n" + "<!--";  		
		s += "\n" + "<xs:attribute name=\"title\" type=\"xs:string\"/>";
		s += "\n" + "<xs:attribute ref=\"tp:rating\"/>";
		s += "\n" + "<xs:attribute name=\"nameTitle\" type=\"sh:nameTitleType\"/>";
		s += "\n" + "<xs:attribute name=\"salutation\" type=\"cd:union.salutationType\"/> -->";
		s += "\n" + "<!-- reference to global attribute defined in other namespace -->";
		s += "\n" + "</xs:complexType>";
		
		return s;
	}
	
	static void stipCommentsTester () {
		String in = makeString();
	
		prtln ("------------------------------------------------\n");
		prtln ("\n in: \n " + in);
		String out = stripComments (in);	
		prtln ("\n out: \n " + out);
	}
	
	public static final String encodeIndexId (String xpath) {
		// String leaf = XPathUtils.getLeaf(xpath);
		String leaf = XPathUtils.getNodeName(xpath);
		prtln ("leaf: " + leaf);
		String root = FindAndReplace.replace (leaf, ":", "", false)+"Index";
		prtln ("root: " + root);
		Pattern p = Pattern.compile(leaf+"_\\$\\{"+root+"([0-9]*)");
		boolean found = false;
		Matcher m = p.matcher(xpath);
		int lastIndex = 0;
		while (m.find()) {
			found = true;
			prtln ("FOUND: " + m.end());
			prtln ("\t group: " + m.group(1));
			try {
				lastIndex = Integer.parseInt(m.group(1));
			} catch (java.lang.NumberFormatException e) {}
		}

		return root + (found ? String.valueOf(lastIndex+1) : "");
	}
		
	
	public static void main (String [] args) {
		
		String xpath = "/concept/relations/hierarchy_${hierarchyIndex0+1}_/hierarchy_${hierarchyIndex2+1}_/hierarchy";
		// String xpath = "/concept/relations/hierarchy_${hierarchyIndex0+1}_/hierarchy_${hierarchyIndex2+1}_/hierarch";
		// String xpath = "/concept_${conceptIndex+1}_/concept";
		// String xpath = "/concept";
		prtln (encodeIndexId (xpath));
		// stipCommentsTester();
		
		//  /content_${contentIndex+1}_/text
		
/* 		Pattern p = Pattern.compile(".*?/content/text");
		String s = "/as/asdf/content/text"; */
		
		// Pattern p = Pattern.compile("hierarchy_\\$\\{hierarchyIndex[\d]*.*?\\}");
/* 		Pattern p = Pattern.compile("hierarchy_\\$\\{hierarchyIndex([0-9]*)");
		String s = "/concept/relations/hierarchy_${hierarchyIndex0+1}_/hierarchy_${hierarchyIndex1+1}_hierarchy";
		boolean found = false;
		Matcher m = p.matcher(s);
		while (m.find()) {
			found = true;
			prtln ("FOUND: " + m.end() + ", " + s.length());
			prtln ("\t group: " + m.group(1));
		}
		if (!found)
			prtln ("NOT found"); */

	}

	static String stripComments (String s) {
		Pattern p;
		Matcher m;
		int index;

		// remove comments
		p = Pattern.compile("<!--.+?-->", Pattern.DOTALL);
		index = 0;
		while (true) {
			m = p.matcher(s);

			// replace occurrences one by one
			if (m.find(index)) {
				// prtln("\n comment group: " + m.group());
				String content = s.substring(m.start(), m.end());
				// prtln ("content: " + content + "(" + content.length() + ")");
				
				String replaceStr = "";
				s = s.substring(0, m.start()) + s.substring(m.end());
				index = m.start();
				// prtln ("\n after removing comment:\n" + s);
			}
			else {
				break;
			}
		}
		return s;
	}

	
	
	/**
	 *  Description of the Method
	 *
	 *@param  s  Description of the Parameter
	 */
	private static void prtln(String s) {
		if (debug) {
			// System.out.println("RegExTester: " + s);
			
			while (s.length() > 0 && s.charAt(0) == '\n') {
				System.out.println("");
				s = s.substring(1);
			}
			
			System.out.println(s);
		}
	}

}

