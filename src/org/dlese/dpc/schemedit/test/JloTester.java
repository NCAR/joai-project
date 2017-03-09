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

import java.util.*;
import java.util.regex.*;
import java.net.*;
import java.io.*;
import org.dlese.dpc.schemedit.*;
import org.dlese.dpc.schemedit.input.InputManager;
import org.dlese.dpc.xml.Dom4jUtils;
import org.dlese.dpc.xml.XPathUtils;
import org.dlese.dpc.xml.schema.*;
import org.dlese.dpc.util.Files;
import org.dom4j.*;
import org.dom4j.tree.*;
import org.dom4j.io.*;
import org.jaxen.SimpleNamespaceContext;
import java.util.regex.*;

import org.dlese.dpc.util.strings.FindAndReplace;


/**
 *  Class for testing dom manipulation with help from {@link org.dlese.dpc.xml.schema.SchemaHelper}
 * I am different
 * @author     ostwald<p>
 *
 */
public class JloTester {

	private static boolean debug = true;

	Document doc = null;
	DocMap docMap = null;
	DocumentFactory df = null;
	NamespaceRegistry namespaces = null;
	String T = "\t";
	String NL = "\n";
	
	/**
	 *  Constructor for the JloTester object
	 *
	 * @param  path  NOT YET DOCUMENTED
	 */
	JloTester(String path) throws Exception {
		try {
			doc = Dom4jUtils.getXmlDocument (new File (path));
			docMap = new DocMap (doc);
		} catch (Exception e) {
			throw new Exception ("init error: " + e.getMessage());
		} catch (Throwable t) {
			throw new Exception ("unknown error!");
		}
	}

	JloTester(URL url) throws Exception {
		try {
			doc = Dom4jUtils.getXmlDocument (url);
			docMap = new DocMap (doc);
		} catch (Exception e) {
			throw new Exception ("init error: " + e.getMessage());
		} catch (Throwable t) {
			throw new Exception ("unknown error!");
		}
	}
	
		
	
	/**
	 *  The main program for the JloTester class
	 *
	 * @param  args           The command line arguments
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public static void main(String[] args) throws Exception {

		
/* 		String path = "/editor/editorHTMLIncludes.jsp";
		Element e = DocumentHelper.createElement ("foo");
		e.setText ("<%@ include file=\"" + path + "\" %>");
		pp (e);
		
		String s = Dom4jUtils.prettyPrint(e);
		
		String out = replaceDirectives(s);
		prtln ("out: " + out); */
		
		int total = 99;
		int done = 43;
		float percentComplete = (float)done/total;
		prtln ("percentComplete: " + Float.toString(percentComplete));
	}
	
	static String replaceDirectives (String s) {
		// pattern to detect page directives (e.g., includes) that were inserted
		// into Document as text and therefore have the tags escaped.
		Pattern p = Pattern.compile("&lt;(%@.*?)%&gt;");
		Matcher m = null;
		while (true) {
			m = p.matcher(s);
			if (m.find()) {
				prtln ("MATCH: \"" + m.group(1) + "\"");
				String repl = "<" + m.group(1) + ">";
				s = m.replaceFirst (repl);
			}
			else
				break;
		}
		return s;
	}
	
	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  n  NOT YET DOCUMENTED
	 */
	private static void pp(Node n) {
		prtln(Dom4jUtils.prettyPrint(n));
	}


	/**
	 *  Description of the Method
	 *
	 * @param  s  Description of the Parameter
	 */
	private static void prtln(String s) {
		if (debug) {
			// System.out.println("JloTester: " + s);
			System.out.println(s);
		}
	}

}

