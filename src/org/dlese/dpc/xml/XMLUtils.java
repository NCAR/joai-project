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

import org.dlese.dpc.xml.XMLConversionService;
import java.io.*;
import org.json.XML;
import org.apache.commons.lang.StringEscapeUtils;

/**
 *  This class holds static methods for use in XML processing.
 *
 * @author    John Weatherley
 */
public class XMLUtils {

	/**
	 *  Removes all XML comments from a String.
	 *
	 * @param  input  XML String
	 * @return        XML with all comments removed
	 */
	public static String removeXMLComments(String input) {
		return input.replaceAll("(?s)<!--.*?-->", "");
	}


	/**
	 *  Strips the XML declaration and DTD declaration from the given XML. The resulting content is sutable for
	 *  insertion inside an existing XML element.
	 *
	 * @param  rdr              A BufferedReader containing XML.
	 * @return                  Content with the XML and DTD declarations stipped out.
	 * @exception  IOException  If error
	 */
	public final static StringBuffer stripXmlDeclaration(BufferedReader rdr) throws IOException {
		return XMLConversionService.stripXmlDeclaration(rdr);
	}


	/**
	 *  Convert XML to JSON.
	 *
	 * @param  xml  An XML String.
	 * @return      JSON serialization of the XML or empty string if error.
	 */
	public final static String xml2json(String xml) {
		try {
			return XML.toJSONObject(xml).toString(3);
		} catch (Throwable e) {
			System.err.println("Error converting XML to JSON: " + e);
			return "";
		}
	}


	/**
	 *  Escapes the characters in a String using XML entities.
	 *
	 * @param  xml  The String to escape, may be null
	 * @return      A new escaped String, null if null string input
	 */
	public final static String escapeXml(String xml) {
		return StringEscapeUtils.escapeXml(xml);
	}


	private static void prtln(String s) {
		System.out.println(s);
	}

}


