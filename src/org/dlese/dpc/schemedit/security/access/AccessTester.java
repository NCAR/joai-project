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

public class AccessTester {
	
	WildcardHelper wch = new WildcardHelper();
	List urlPatternMatchers = null;
	
	AccessTester () {
		wch = new WildcardHelper();
		urlPatternMatchers = getUrlPatternMatchers();
/* 		prtln ("\ncompiled URL patterns");
		for (Iterator i=urlPatternMatchers.iterator();i.hasNext();) {
			UrlPatternMatcher pm = (UrlPatternMatcher)i.next();
			prtln (pm.toString());
		} */
		showPatterns ();
	}
	
	public static List getPatternsList () {
		List pp = new ArrayList ();
		pp.add ("/"); // legal default mapping
		pp.add ("/*"); // legal default mapping
		
		pp.add ("/a/"); 
		/* pp.add ("/a/b/"); */
		pp.add ("/a/b/*"); 
		pp.add ("/a/b/c/"); 
		pp.add ("/a/b/c/foo.jsp");
		pp.add ("*.jsp");
		
		return pp;
	}
	
	boolean isMatch (String path, String pattern) {
		Map map = new HashMap();
		if (wch.match (map, path, wch.compilePattern(pattern))) {
/* 			prtln ("Got a match!");
			for (Iterator i=map.keySet().iterator();i.hasNext();) {
				String key = (String)i.next();
				prtln ("\t" + key + ": " + (String)map.get(key));
			} */
			return true;
		}
		return false;
	}
		
	
	public List getUrlPatternMatchers () {
		List pms = new ArrayList ();
		for (Iterator i=getPatternsList().iterator();i.hasNext();) {
			UrlPatternMatcher matcher = new UrlPatternMatcher ((String)i.next());
			if (!matcher.matchers.isEmpty()) {
				// prtln (" --> " + urlMapping);
				pms.add (matcher);
			}
		}
		return pms;
	}
	
	public String getMatch (String path) {
		prtln ("\nmatch with \"" + path + "\"");
		List matches = new ArrayList ();
		Iterator pms = urlPatternMatchers.iterator();
		while (pms.hasNext()) {
			UrlPatternMatcher pm = (UrlPatternMatcher)pms.next();
			
			// return exact matches
			if (pm.urlPattern.equals(path))
				return pm.urlPattern;
			
			// prtln ("matching against " + pm.urlPattern);
			if (pm.match (path)) {
				// prtln ("\t YEP!");
				matches.add (pm.urlPattern);
			}
			else {
				// prtln ("\t nope");
			}
		}
		prtln (" found " + matches.size() + " matches");
		return getBestMatch (matches);
	}
	
	String getBestMatch (List matches) {
		prtln ("\nbestMatch");
		int longest = -1;
		String bestMatch = null;
		for (Iterator i=matches.iterator();i.hasNext();) {
			String match = (String)i.next();
			if ("/*".equals(match)) {
				prtln ("shaving " + match);
				match = "/";
			}
			int length = match.split("/").length;
			prtln ("\t" + match + "(" + match.split("/").length + ")");
			if (length > longest) {
				bestMatch = match;
				longest = length;
			}
		}
		
		return bestMatch;
	}
	
	public void showPatterns () {
		prtln ("Patterns");
		Iterator pms = urlPatternMatchers.iterator();
		while (pms.hasNext()) {
			UrlPatternMatcher pm = (UrlPatternMatcher)pms.next();
			prtln ("\t" + pm.urlPattern );
		}
	}
	
	public static void main (String [] args) {
		System.out.println ("Hello World from AccessTester");
		
		AccessTester t = new AccessTester();
		String path = "/admin/foo.jsp";
		if (args.length > 0)
			path = args[0];
		String match = t.getMatch (path);
		prtln ("path: \"" + path + "\"");
		prtln ("match: \"" + match + "\"");

	}
	
		
	static void prtln (String s) {
		while (s.length() > 0 && s.charAt(0) == '\n') {
			System.out.println ("");
			s = s.substring(1);
		}
		System.out.println(s);
	}
			
}
