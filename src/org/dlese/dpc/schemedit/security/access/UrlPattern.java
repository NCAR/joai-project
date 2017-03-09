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

public class UrlPattern {
	
	private static boolean debug = false;
	
	WildcardHelper wch = new WildcardHelper();
	List urlPatterns = null;
	
	UrlPattern () {
		wch = new WildcardHelper();
		urlPatterns = getPatterns();
	}
	
	public List getPatterns () {
		List pp = new ArrayList ();
		pp.add (wch.compilePattern ("/admin/**"));
		pp.add (wch.compilePattern ("/user/*"));
		return pp;
	}
	
	public static List getTestPatterns () {
		List pp = new ArrayList ();
		pp.add ("/"); // legal default mapping
		pp.add ("/*"); // legal default mapping??
		
		pp.add ("/admin/"); // legal path prefix
		pp.add ("/admin/foo/"); // legal path prefix
		pp.add ("/admin/foo/*"); // legal path prefix
		pp.add ("/admin/f*oo/*"); // legal path prefix
		pp.add ("/admin/*"); // legal path prefix
		
		pp.add ("*.foo");  // legal extention
		pp.add ("*foo");  // illegal extension
		pp.add ("/*.foo");  // illegal extension
		pp.add ("/admin/*.foo");  // illegal extension
		
		pp.add ("/a"); // legal explicit
		pp.add ("/a/b.jsp"); // legal explicit
		pp.add ("/a/*.jsp"); // illegal explicit
		pp.add ("/a/b.*"); // illegal explicit
		pp.add ("/a/b"); // legal explicit
		
		return pp;
	}
	
	public boolean match (String path) {
		Map map = new HashMap ();
		Iterator cps = urlPatterns.iterator();
		while (cps.hasNext()) {
			int[] cp = (int [])cps.next();
			if (wch.match (map, path, cp)) {
				prtln ("Got a match!");
				for (Iterator i=map.keySet().iterator();i.hasNext();) {
					String key = (String)i.next();
					prtln ("\t" + key + ": " + (String)map.get(key));
				}
				return true;
			}
		}
		prtln ("NO match found");
		return false;
	}
	
	static int countStars (String s) {
		char[] chars = s.toCharArray();
		int stars = 0;
		for (int i=0;i<chars.length;i++) {
			if (chars[i] == '*')
				stars++;
		}
		return stars;
	}
	
	static void showSplits (String[] splits) {
		if (splits == null) {
			prtln ("\t there are no splits");
			return;
		}
		prtln ("\t there are " + splits.length + " segments");
		for (int i=0;i<splits.length;i++) {
			prtln ("\t\t" + i + " - " + splits[i]);
		}
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
			
	public static String getUrlMapping (String path) {
		String urlMapping = null;
		if (path == null) return path;
		prtln ("\n getPattern with \"" + path + "\"");
		
		if (isDefaultMapping (path)) {
			prtln ("\t IS default mapping");
			return "/**/*";
/* 			if (path.charAt (path.length() -1) != '*')
				return (path + "*");
			else
				return path; */
		}
		
		if (isPathPrefixMapping (path)) {
			prtln ("\t IS path prefix mapping");
			if (path.charAt (path.length() -1) != '*')
				return (path + "**/*");
			else
				return path + "*/*";
		}
		
		if (isExtensionMapping (path)) {
			prtln ("\t IS extension mapping");
			return "/**/" + path;
		}
		
		if (isExplicitMapping (path)) {
			prtln ("\t IS explicit mapping");
			return path;
		}
		
		prtln ("\t illegal path: \"" + path + "\"");
		return null;
		
	}
	
	/**
	* returns null if the path cannot be converted into a pattern
	*/
	public static String getPattern1 (String path) {
		if (path == null) return path;
		prtln ("\n getPattern with \"" + path + "\"");
		
		if (path.equals("/") || path.equals("/*")) {
			prtln ("\t default mapping");
			return "/**";
		}
		
		try {
			// how many *s?
			int stars = countStars (path);
			
			if ( stars > 1) {
				throw new Exception ("\n too many asterisks (" + stars + ")");
			}
			
			if (stars == 0) {
				if (path.charAt (path.length() - 1) == '/') {
					prtln ("\t path prefix mapping");
					return path + "**";
				}
				else {
					prtln ("\t explicit mapping");
					return path;
				}
			}

			String[] splits = path.split("/");
			
			if (splits.length == 0)
				throw new Exception ("splits length == 0 : this should have been caught??");
			String lastSplit = splits[splits.length - 1];
			
			if (lastSplit.indexOf("*") == -1)
				throw new Exception ("expected asterisk in \"" + lastSplit + "\"");
			
			if (splits.length == 1 && lastSplit.startsWith ("*.")) {
				prtln ("\t extension mapping");
				return path;
			}
			
		} catch (Exception e) {
			prtln ("could not create urlMapping: " + e.getMessage());
			return null;
		}
		
		return path;
	}
		
		
	
	public static void main (String [] args) {
		prtln ("\n------------------------------------------------\n");
		System.out.println ("Hello World from UrlPattern ...\n");
		
		String path = "/admin/foo.jsp";
		String urlMapping = null;
		if (args.length > 0) {
			path = args[0];
			urlMapping = getUrlMapping (path);
			if (urlMapping != null)
				prtln (" --> " + urlMapping);
		}
		else {
			for (Iterator i=getTestPatterns().iterator();i.hasNext();) {
				urlMapping = getUrlMapping ( (String)i.next());
				if (urlMapping != null)
					prtln (" --> " + urlMapping);
			}
		}

	}
	
	static void prtln1 (String s) {
	}
		
	static void prtln (String s) {
		if (debug)
			System.out.println(s);
	}
			
}
