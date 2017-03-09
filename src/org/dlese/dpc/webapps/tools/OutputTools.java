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
package org.dlese.dpc.webapps.tools;

import java.util.*;
import java.io.*;
import org.dlese.dpc.xml.*;

/**
 *  This class contains tools for formatting ouput, such as converting XML text
 *  to HTML, that are useful in many servlet/JSP based applications.
 *
 * @author    John Weatherley
 */
public final class OutputTools {
	/**
	 *  Provides HTML encoding for XML resulting in text that looks good in a Web
	 *  browser. Uses the indentation that existed in the original text. Use this
	 *  method to encode text that will be displayed in a web browser (e.g. JSP or
	 *  HTML page).
	 *
	 * @param  stg  The string to convert.
	 * @return      A string suitable for display as HTML
	 */
	public final static String xmlToHtml(String stg) {
		String res;
		if (stg == null)
			res = "";
		else {
			StringBuffer bufa = new StringBuffer();
			char c1;
			for (int i = 0; i < stg.length(); i++) {
				c1 = stg.charAt(i);
				if (c1 == '\"')
					bufa.append("&quot;");
				else if (c1 == '&')
					bufa.append("&amp;");
				else if (c1 == '\n')
					bufa.append("<br>");
				else if (c1 == ' ')
					bufa.append("&nbsp; ");
				else if (c1 == '\t')
					bufa.append("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; ");
				else if (c1 == '<')
					bufa.append("<font color='blue'>&lt;");
				else if (c1 == '>')
					bufa.append("&gt;</font>");
				//else if (c1 == '#')
				//bufa.append("%23");
				else
					bufa.append(c1);
			}
			res = new String(bufa);
		}
		return res;
	}


	/**
	 *  Gets the content from XML by stripping all XML tags. The input XML should
	 *  be valid prior to calling this method. This method produces exactly the
	 *  same output as {@link org.dlese.dpc.xml.XMLConversionService#getContentFromXML(String)}.
	 *
	 * @param  stg  An XML String.
	 * @return      The contentFromXML.
	 */
	public final static String getContentFromXML(String stg) {
		return XMLConversionService.getContentFromXML(stg);
	}


	/**
	 *  Provides HTML encoding for XML resulting in text that looks good in a Web
	 *  browser. Attempts to indent based on the XML structure rather than using
	 *  the indentation in the text itself. Use this method to encode text that
	 *  will be displayed in a web browser (e.g. JSP or HTML page). <p>
	 *
	 *  Note: This method still needs some work.
	 *
	 * @param  stg  The string to convert.
	 * @return      A string suitable for display as HTML
	 */
	public final static String xmlToHtmlIndent(String stg) {
		int indent = 0;
		String res;
		if (stg == null)
			res = "";
		else {
			StringBuffer bufa = new StringBuffer();
			char c1;
			char c2;
			for (int i = 0; i < stg.length(); i++) {
				c1 = stg.charAt(i);
				if (i + 1 < stg.length())
					c2 = stg.charAt(i + 1);
				else
					c2 = ' ';

				if (c1 == '\"')
					bufa.append("&quot;");
				else if (c1 == '&')
					bufa.append("&amp;");
				else if (c1 == '\n') {
					bufa.append("<br>");
					for (int j = 0; j < indent; j++)
						bufa.append("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;");
				}
				//else if (c1 == ' ') 	bufa.append("&nbsp;");
				//else if (c1 == '\t') 	bufa.append("&nbsp;&nbsp;&nbsp;");
				else if (c1 == '<') {
					if (c2 != '/') {
						for (int j = 0; j < indent; j++)
							bufa.append("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;");
						indent++;
					}
					else
						indent--;
					bufa.append("<font color='gray'><b>&lt;");
				}
				else if (c1 == '>')
					bufa.append("&gt;</b></font>");
				//else if (c1 == '#')
				//bufa.append("%23");
				else
					bufa.append(c1);
			}
			res = new String(bufa);
		}
		return res;
	}


	/**
	 *  Convert a plain text string into HTML. The resulting String is sutable for
	 *  display in a web browser.
	 *
	 * @param  text  The original text.
	 * @return       The text encoded into HTML
	 */
	public final static String htmlEncode(String text) {
		String res;
		if (text == null)
			res = "";
		else {
			StringBuffer bufa = new StringBuffer();
			char c1;
			for (int i = 0; i < text.length(); i++) {
				c1 = text.charAt(i);
				if (c1 == '\"')
					bufa.append("&quot;");
				else if (c1 == '&')
					bufa.append("&amp;");
				else if (c1 == '\n')
					bufa.append("<br>");
				else if (c1 == '\t')
					bufa.append("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;");
				else if (c1 == '<')
					bufa.append("&lt;");
				else if (c1 == '>')
					bufa.append("&gt;");
				else
					bufa.append(c1);
			}
			res = new String(bufa);
		}
		return res;
	}

}


