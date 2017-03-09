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

/**
 *  Class for testing pattern matching and regular expressinos
 *
 *@author     ostwald
 */
public class PatternTester {

	private static boolean debug = true;


	public static String getElementText(Element e) {
		String link = e.attributeValue ("link");
		String url = e.attributeValue ("url");
		String text = e.getText();
		if (link != null && url != null && text != null) {
			DocumentFactory df = DocumentFactory.getInstance();
			Element hyperlink = df.createElement("a").addAttribute("href", url);
			hyperlink.setText (link);
			return FindAndReplace.replace (text, link, hyperlink.asXML(), false);
		}
		return e.getText();
	}
	

	public static boolean isValidUrl (String s) {
		prtln ("isValidUrl with " + s);
		URI uri = null;
		try {
			uri = new URI (s);
		} catch (URISyntaxException se) {
			prtln (se.getMessage());
			return false;
		}
		String scheme = uri.getScheme();
		if (scheme == null)
			return false;
		else {
			prtln ("scheme: " + scheme);
			return true;
		}
	}
		
	
	public static String FINAL_STATUS_TEMPLATE = "_|-final--|_";
	public static Pattern FINAL_STATUS_PATTERN = Pattern.compile("_\\|-final-([^\\s]+)-\\|_");

	public static String makeFinalStatusValue (String collection) {
		return FindAndReplace.replace (FINAL_STATUS_TEMPLATE, "--", "-" + collection + "-", false);
	}
	
	public static String getCollection (String statusValue) {
		Matcher m = FINAL_STATUS_PATTERN.matcher(statusValue);
		if (m.find())
			return m.group(1);
		else
			return "";
	}
	
	public static boolean isFinalStatusValue (String s) {
		String collection = getCollection (s);
		prtln ("  " + s + " --> *" + collection + "*");
		return (collection != null && collection.trim().length() > 0);
	}
	
	/**
	* validate emails for the suggestor app.
	* MUST have on and only one "@"
	* must not end in Period
	*/
	public static boolean isValidEmail (String s) {
		prtln ("isValidEmail with " + s);
		int ampCount = 0;
		char [] chars = s.toCharArray();
		int len = chars.length;
		for (int i=0;i<len;i++) {
			if (String.valueOf(chars[i]).equals("@"))
				ampCount++;
		}
		return (ampCount == 1 && !s.endsWith("."));
	}
	
	public static void main (String [] args) {
		String firstpart = "http://128.117.126.8:8688/schemedit/adn/adn.do";
        String secondpart = "src=dcs&command=edit&crollection=1102611887199&recId=MyCol-000-000-000-065";
		String url = firstpart + "?" + secondpart;
		
		
/*		Map queryArgs = getQueryArgs (url);
 		for (Iterator i = queryArgs.keySet().iterator();i.hasNext();) {
			String name = (String)i.next();
			String value = (String) queryArgs.get(name);
			prtln (name + ": " + value);
		} */
			
		String val = getParamValue (url, "collection");
		prtln ("collection: " + val);
	}

	static String getParamValue (String url, String paramName) {
		Map queryArgs = getQueryArgs (url);
		return (String)queryArgs.get (paramName);
	}
	
	static Map getQueryArgs (String url) {
		
		String query = "";
		Map queryArgs = new HashMap();
		
		try {
			query = url.split("\\?")[1];
			// prtln ("query: " + query);
		} catch (ArrayIndexOutOfBoundsException e) {
			prtln ("could not get query");
			return queryArgs;
		}
		
		String [] paramArray = query.split ("\\&");
		for (int i=0;i<paramArray.length;i++) {
			String [] nameValue = paramArray[i].split("\\=");;
			try {
				String name = nameValue[0];
				String value = nameValue[1];
				queryArgs.put (name, value);
			} catch (ArrayIndexOutOfBoundsException e) {
				prtln ("could not parse \"" + nameValue + "\" as a parameter name and value");
			}
		}
		return queryArgs;
	}
	
	/**
	 *  Description of the Method
	 *
	 *@param  s  Description of the Parameter
	 */
	private static void prtln(String s) {
		if (debug) {
			System.out.println("PatternTester: " + s);
		}
	}

}

