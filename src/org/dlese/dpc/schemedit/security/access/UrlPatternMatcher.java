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
package org.dlese.dpc.schemedit.security.access;

import org.apache.struts.util.WildcardHelper;
import java.util.*;
import java.util.regex.*;

/*
	URL patterns:
		(note: in all cases, there can be at most only 1 asterisk, and this must occur in the last segment
	
		Explicit Mapping - no conversion required
		
		path prefix mapping (e.g., /admin/ or /admin/*)
			rule 1 - in paths where there is no * and it ends in a /, append "**"
			rule 2 - in paths ending in /*, append another *
			
		extension mapping (*. followed by a prefix)
			- path becomes "/ * * / *.prefix"
			
		default mapping (/ - matches all urls)
			- path becomes "/ * *"
	**/

public class UrlPatternMatcher {
	
	private static boolean debug = false;
	
	WildcardHelper wch = new WildcardHelper();
	public String urlPattern = null;
	String patternType = "illegal mapping";
	public List matchers;
	public List compiledPatterns;
	
	public UrlPatternMatcher (String urlPattern) {
		this.urlPattern = urlPattern;
		this.matchers = getMatchers();
		this.compiledPatterns = new ArrayList ();
		for (Iterator i=matchers.iterator();i.hasNext();) {
			String matcher = (String)i.next();
			compiledPatterns.add (wch.compilePattern (matcher));
		}
		
		prtln (this.toString());
		
	}
	
	public boolean isEmpty () {
		return matchers.isEmpty();
	}
	
	public boolean matchOLD (String path) {
		Map map = new HashMap ();
		Iterator i = matchers.iterator();
		while (i.hasNext()) {
			String matcher = (String)i.next();
			if (wch.match (map, path, wch.compilePattern (matcher))) {
				// prtln ("Got a match!");
				for (Iterator j=map.keySet().iterator();j.hasNext();) {
					String key = (String)j.next();
					// prtln ("\t" + key + ": " + (String)map.get(key));
				}
				return true;
			}
		}
		// prtln ("does not match");
		return false;
	}	
	
 	public boolean match (String path) {
		Map map = new HashMap ();
		Iterator cps = compiledPatterns.iterator();
		while (cps.hasNext()) {
			int[] cp = (int [])cps.next();
			if (wch.match (map, path, cp)) {
				return true;
			}
		}
		return false;
	}
		
	static boolean isDefaultMapping (String s) {
		Pattern pattern = Pattern.compile("/[*]?");
		Matcher matcher = pattern.matcher (s);
		if (matcher.find()) {
			// showMatch (matcher);
			return (matcher.group().length() == s.length());
		}
		return false;
	}
	
	static boolean isPathPrefixMapping (String s) {
		Pattern pattern = Pattern.compile("/[^*]+/[*]?");
		Matcher matcher = pattern.matcher (s);
		if (matcher.find()) {
			// showMatch (matcher);
			return (matcher.group().length() == s.length());
		}
		return false;
	}
	
	static boolean isExtensionMapping (String s) {
		Pattern pattern = Pattern.compile("\\*\\.[\\S]+");
		Matcher matcher = pattern.matcher (s);
		if (matcher.find()) {
			// showMatch (matcher);
			return (matcher.group().length() == s.length());
		}
		return false;
	}
	
	static boolean isExplicitMapping (String s) {
		Pattern pattern = Pattern.compile("/[^*]*[^/^*]{1}");
		Matcher matcher = pattern.matcher (s);
		if (matcher.find()) {
			// showMatch (matcher);
			return (matcher.group().length() == s.length());
		}
		return false;
	}
	
	static void showMatch (Matcher matcher) {
		prtln ("I found the text \"" + matcher.group() + "\" starting at " +
			"index " + matcher.start() + " and ending at index " + matcher.end());
	}
	
	public List getMatchers () {
		prtln ("\n getMatchers with \"" + urlPattern + "\"");
		List matchers = new ArrayList();
		
		if (urlPattern == null) {
			return matchers;
		}
		
		if (isDefaultMapping (urlPattern)) {
			prtln ("\t IS default mapping");
			patternType = "default mapping";
			matchers.add ("/**/*");
			matchers.add ("/*");
			return matchers;
		}
		
		if (isPathPrefixMapping (urlPattern)) {
			prtln ("\t IS path prefix mapping");
			patternType = "path prefix mapping";
			if (urlPattern.charAt (urlPattern.length() -1) == '*') {
				urlPattern = urlPattern.substring(0, urlPattern.length()-2);
			}
			matchers.add (urlPattern + "**/*");
			matchers.add (urlPattern + "*");
			return matchers;
		}
		
		if (isExtensionMapping (urlPattern)) {
			prtln ("\t IS extension mapping");
			patternType = "extension mapping";
			matchers.add ("/**/" + urlPattern);
			matchers.add ("/" + urlPattern);
			return matchers;
		}
		
		if (isExplicitMapping (urlPattern)) {
			prtln ("\t IS explicit mapping");
			patternType = "explicit mapping";
			matchers.add (urlPattern);
			return matchers;
		}
		
		prtln ("\t illegal urlPattern: \"" + urlPattern + "\"");
		return matchers;
		
	}

	public String toString () {
		String s = "UrlPatternMatcher for " + urlPattern;
		s += "\n\t patternType: " + patternType;
		s += "\n\t matchers";
		if (matchers.isEmpty())
			s += "\n\t\t - none -";
		else
			for (Iterator i=matchers.iterator();i.hasNext();)
				s += "\n\t\t" + (String)i.next();
		return s;
	}
	
	public static void main (String [] args) {
		prtln ("\n------------------------------------------------\n");
		System.out.println ("Hello World from UrlPatternMatcher ...\n");
		
		String pattern = "*.jsp";
		if (args.length > 0) {
			pattern = args[0];
		}

		UrlPatternMatcher pm = new UrlPatternMatcher (pattern);
		
		String path = "/foo.jsp";
		if (pm.match(path))
			prtln ("MATCH");
		else
			prtln ("NOPE");
	}
		
	static void prtln (String s) {
		if (debug)
			System.out.println(s);
	}
			
}
