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

import java.util.regex.*;

/**
 *  Utilities for testing and manipulating XPaths, represented as String.
 *
 * @author    ostwald
 */
public class XPathUtils {

	private static boolean debug = false;


	/**
	 *  Converts encoded paths of the form used in jsp pages ( "foo_1_") to a XPath
	 *  form ("foo[1]"). (square brackets cannot be used in xpaths contained in jsp
	 *  pages)
	 *
	 * @param  path  NOT YET DOCUMENTED
	 * @return       equivalent XPath
	 */
	public static String decodeXPath(String path) {
		String s = removeELbrackets(path);

		Pattern p = Pattern.compile("_[^//^_]*?[\\d]_");
		Matcher m;
		// replace occurrences one by one
		while (true) {
			m = p.matcher(s);
			if (m.find()) {
				String index = s.substring(m.start() + 1, m.end() - 1);
				String replaceStr = "[" + index.trim() + "]";
				try {
					s = p.matcher(s).replaceFirst(replaceStr);
				} catch (IllegalArgumentException e) {
					return s;
				}
			}
			else
				break;
		}
		return s;
	}


	/**
	 *  Remove jsp expressionLanauge brackets used to support indexing in jsp pages
	 *  (i.e., ${...}) from a path.
	 *
	 * @param  s  path containing el-encoded brackets
	 * @return    NOT YET DOCUMENTED
	 */
	public static String removeELbrackets(String s) {
		Pattern p = Pattern.compile("\\$\\{(.+?)\\}");
		Matcher m;
		// replace occurrences one by one
		while (true) {
			m = p.matcher(s);
			if (m.find()) {
				String stripped = m.group(1);

				try {
					s = p.matcher(s).replaceFirst(stripped);
				} catch (IllegalArgumentException e) {
					return s;
				}
			}
			else
				break;
		}
		return s;
	}


	/**
	 *  Returns the index, if any, for the leaf of the given path.<p>
	 *
	 *  Works for both encoded indexing (e.g., "asdf_1_") and decoded (e.g.,
	 *  "asasdf[1]".
	 *
	 * @param  s  NOT YET DOCUMENTED
	 * @return    The index value
	 */
	public static int getIndex(String s) {
		String xpath = decodeXPath(s);
		String leaf = getLeaf(xpath);
		Pattern p = Pattern.compile("\\[(.+?)\\]");
		Matcher m = p.matcher(leaf);
		if (m.find()) {
			try {
				return Integer.parseInt(m.group(1));
			} catch (NumberFormatException e) {
				prtln ("getIndex could not parse \"" + m.group(1) + "\"as int");
			}
		}
		return 0;
	}


	/**
	 *  Converts indexed paths of the form ("foo[1]") to the form used in jsp pages
	 *  ( "foo_1_")
	 *
	 * @param  s  indexed xpath
	 * @return    equivalent XPath
	 */
	public static String encodeXPath(String s) {
		Pattern p = Pattern.compile("\\[.+?\\]");
		Matcher m;
		// replace occurrences one by one
		while (true) {
			m = p.matcher(s);
			if (m.find()) {
				String index = s.substring(m.start() + 1, m.end() - 1);
				String replaceStr = "_" + index.trim() + "_";
				s = p.matcher(s).replaceFirst(replaceStr);
			}
			else {
				break;
			}
		}
		return s;
	}


	/**
	 *  Removes indexing information from XPath strings.
	 *  <ul>
	 *    <li> before: "/itemRecord/general/foo[1]/@url"</li>
	 *    <li> after: "/itemRecord/general/foo/@url"</li>
	 *  </ul>
	 *
	 *
	 * @param  s  xpath possibly containing index notation
	 * @return    equivalent XPath
	 */
	public static String normalizeXPath(String s) {
		Pattern p = Pattern.compile("\\[.+?\\]");
		Matcher m = p.matcher(s);
		return m.replaceAll("");
	}


	/**
	 *  Returns the name of the element refered to by the given xpath, which is the
	 *  leaf, stripped of "\@" in the case of attribute paths). Accepts jsp-encoded
	 *  xpaths.
	 *
	 * @param  xpath  NOT YET DOCUMENTED
	 * @return        The leafName value, with indexing intact
	 */
	public static String getLeaf(String xpath) {
		String s = xpath;
		int lastSlash = s.lastIndexOf("/");
		if (lastSlash == -1) {
			return s;
		}

		String leaf = s.substring(lastSlash + 1);
		if (leaf.startsWith("@")) {
			return leaf.substring(1);
		}
		return leaf;
	}


	/**
	 *  Returns true if the xpath refers to an attribute.
	 *
	 * @param  xpath  xpath to be tested
	 * @return        The attributePath value
	 */
	public static boolean isAttributePath(String xpath) {
		String s = xpath;
		int lastSlash = s.lastIndexOf("/");
		if (lastSlash == -1) {
			return false;
		}

		String leaf = s.substring(lastSlash + 1);
		return leaf.startsWith("@");
	}


	/**
	 *  Returns the name of the node referred to by the (possibly jsp-encoded)
	 *  xpath, stripped of indexing information.
	 *
	 * @param  xpath  NOT YET DOCUMENTED
	 * @return        The nodeName, stripped of indexing
	 */
	public static String getNodeName(String xpath) {
		return (normalizeXPath(decodeXPath(getLeaf(xpath))));
	}


	/**
	 *  Gets the parentXPath attribute of the XPathUtils class
	 *
	 * @param  xpath  Description of the Parameter
	 * @return        The parentXPath value
	 */
	public static String getParentXPath(String xpath) {
		int lastSlash = xpath.lastIndexOf("/");
		if (lastSlash == -1) {
			return null;
		}
		return xpath.substring(0, lastSlash);
	}


	/**
	 *  Gets the siblingPath (that matches all like-named nodes of the parent
	 *  Element) of a given xpath. Removes indexing information only on the
	 *  terminal element (leaf), NOT on the rest of the path (as {@link
	 *  #normalizeXPath (String)} does).
	 *
	 * @param  xpath  XPath as String
	 * @return        The siblingPath value
	 */
	public static String getSiblingXPath(String xpath) {
		return getParentXPath(xpath) + "/" + getNodeName(xpath);
	}


	/**
	 *  Compares two xpaths for "xpath order", using "natuiral" or alphabetical
	 *  ordering except for when xpath indexing is involved, e.g., "/record/general[3]/foo[5]"
	 *  is "less than" "/record/general[4]/foo[6]".<p>
	 *
	 *  NOTE: result of this comparison is not the same "document order", since XML
	 *  documents are not structured in alpha order!
	 *
	 * @param  path1  first path to be compared
	 * @param  path2  second path to be compared
	 * @return        0 if the two paths are equal, -1 if path1 is "less than" path
	 *      2, 1 otherwise
	 */
	public static int compare(String path1, String path2) {
		// take care of easy cases first
		if (path1.equals(path2))
			return 0;
		if (path1 == null)
			return -1;
		if (path2 == null)
			return 1;
		// must test for empty string before attempting to split
		if ("".equals(path1))
			return -1;
		if ("".equals(path2))
			return 1;
		// convention: relative paths are less than absolute
		if (path1.charAt(0) == '/' && path2.charAt(0) != '/')
			return 1;
		if (path1.charAt(0) != '/' && path2.charAt(0) == '/')
			return -1;

		String[] splits1 = path1.split("/");
		String[] splits2 = path2.split("/");

		if (splits1.length == 1 && splits2.length == 1) {
			// compare path segments. we know they aren't equal, but they might differ only by index
			String sib1 = XPathUtils.getSiblingXPath(path1);
			String sib2 = XPathUtils.getSiblingXPath(path2);

			if (sib1.compareTo(sib2) > 0) {
				return 1;
			}
			// if equal sibling paths, we must compare indexes
			if (sib1.equals(sib2)) {
				return Integer.valueOf(XPathUtils.getIndex(path1)).compareTo(
					Integer.valueOf(XPathUtils.getIndex(path2)));
			}
			return -1;
		}
		else {
			// compare segment by segment
			int lcs = java.lang.Math.min(splits1.length, splits2.length);

			for (int i = 0; i < lcs; i++) {
				int cmp = compare(splits1[i], splits2[i]);
				if (cmp != 0)
					return cmp;
			}

			// we haven't broken the tie yet, so we determine that the shorter path is least
			return Integer.valueOf(splits1.length).compareTo(Integer.valueOf(splits2.length));
		}
	}


	/**
	 *  Description of the Method
	 *
	 * @param  s  Description of the Parameter
	 */
	private static void prtln(String s) {
		if (debug) {
			org.dlese.dpc.xml.schema.SchemaUtils.prtln(s, "");
		}
	}

}

