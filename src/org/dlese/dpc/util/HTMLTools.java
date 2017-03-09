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
package org.dlese.dpc.util;

import org.dlese.dpc.xml.XMLUtils;

/**
 *  This class holds a number of handy static methods for use in HTML and XML processing.
 *
 * @author     John Weatherley
 * @version    $Id: HTMLTools.java,v 1.9 2009/03/20 23:34:00 jweather Exp $
 */
public class HTMLTools {

	/**
	 *  Removes all HTML comments from a String.
	 *
	 * @param  input  HTML String
	 * @return        HTML with all comments removed
	 */
	public static String removeHTMLComments(String input) {
		return XMLUtils.removeXMLComments(input);
	}


	/**
	 *  Encodes a regular String with caracter entity references, for example the ampersand character &amp;
	 *  becomes &amp;amp;. See <a href="http://www.w3schools.com/html/html_entitiesref.asp"> Entities Reference
	 *  </a> for a nice table of character entity references. Ampersands that are already encoded will not be
	 *  touched. If the input string is an SGML variant like HTML or XML then all occurances of the characters
	 *  &lt;, &gt; and &quot; are left alone.
	 *
	 * @param  input   A String.
	 * @param  isSGML  True if the input string is a an SGML variant such as HTML or XML, false if not.
	 * @return         A String that is encoded with character entity references.
	 */
	public static String encodeCharacterEntityReferences(String input, boolean isSGML) {
		if (input == null)
			return null;

		// Note: some chars are represented using the ISO hex value

		StringBuffer bufa = new StringBuffer();
		for (int i = 0; i < input.length(); i++) {
			char tchr = input.charAt(i);
			if (tchr == '\"' && !isSGML)
				bufa.append("&quot;");
			else if (tchr == '<' && !isSGML)
				bufa.append("&lt;");
			else if (tchr == '>' && !isSGML)
				bufa.append("&gt;");
			else if (tchr == '&' &&
				input.length() <= (i + 4))
				bufa.append("&amp;");
			else if (tchr == '&' &&
				!(input.charAt(i + 1) == 'a' &&
				input.charAt(i + 2) == 'm' &&
				input.charAt(i + 3) == 'p' &&
				input.charAt(i + 4) == ';'))
				bufa.append("&amp;");
			// Copyright symbol (c)
			else if (tchr == '\u00A9')
				bufa.append("&#169;");
			/* // en dash
			else if (tchr == '\u2013')
				bufa.append("&#8211;");
			// em dash
			else if (tchr == '\u2014')
				bufa.append("&#8212;");
			// left single quotation mark
			else if (tchr == '\u2018')
				bufa.append("&#8216;");
			// right single quotation mark
			else if (tchr == '\u2019')
				bufa.append("&#8217;");
			// left double quotation mark
			else if (tchr == '\u201C')
				bufa.append("&#8220;");
			// right double quotation mark
			else if (tchr == '\u201D')
				bufa.append("&#8221;"); */
			else
				bufa.append(tchr);
		}

		return bufa.toString();
	}


	// ------------------------- Methods useful for JavaScript ------------------


	/**
	 *  Encodes an Array of chars so that they will be valid inside JavaScript quotes.
	 *
	 * @param  chars  An array of chars
	 * @return        The resulting, encoded StringBuffer
	 */
	public final static StringBuffer javaScriptEncode(char[] chars) {
		StringBuffer buf = new StringBuffer();
		if (chars == null)
			return buf;
		for (int i = 0; i < chars.length; i++) {
			// Replace a double quote with \"
			if (chars[i] == '\"')
				buf.append("\\\"");
			// Replace a single quote with \'
			else if (chars[i] == '\'')
				buf.append("\\\'");
			// Replace CR with \r;
			else if (chars[i] == '\r')
				buf.append("\\r");
			// Replace LF with \n;
			else if (chars[i] == '\n')
				buf.append("\\n");
			// Replace backslash with \\
			else if (chars[i] == '\\')
				buf.append("\\\\");
			// All else, output unchanged
			else
				buf.append(chars[i]);
		}
		return buf;
	}


	/**
	 *  Encodes a String so that it will be valid inside JavaScript quotes.
	 *
	 * @param  string  A String
	 * @return         The resulting, encoded StringBuffer
	 */
	public final static StringBuffer javaScriptEncode(String string) {
		if (string == null)
			return null;
		else
			return javaScriptEncode(string.toCharArray());
	}


	/**
	 *  Same behavior as {@link #javaScriptEncode}.
	 *
	 * @param  string  A String
	 * @return         The resulting, encoded String
	 */
	public final static String javaScriptEncodeToStr(String string) {
		if (string == null)
			return null;
		StringBuffer buf = new StringBuffer();
		char[] chars = string.toCharArray();
		for (int i = 0; i < chars.length; i++) {
			// Replace a double quote with \"
			if (chars[i] == '\"')
				buf.append("\\\"");
			// Replace a single quote with \'
			else if (chars[i] == '\'')
				buf.append("\\\'");
			// Replace CR with \r;
			else if (chars[i] == '\r')
				buf.append("\\r");
			// Replace LF with \n;
			else if (chars[i] == '\n')
				buf.append("\\n");
			// Replace backslash with \\
			else if (chars[i] == '\\')
				buf.append("\\\\");
			// All else, output unchanged
			else
				buf.append(chars[i]);
		}
		return buf.toString();
	}



	private static void prtln(String s) {
		System.out.println(s);
	}

}


