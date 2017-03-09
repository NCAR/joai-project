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
import org.dlese.dpc.serviceclients.webclient.*;
import org.dlese.dpc.xml.Dom4jUtils;
import org.dlese.dpc.xml.schema.*;
import org.dlese.dpc.util.*;
import org.dom4j.*;
import org.dom4j.tree.*;
import org.dom4j.io.*;


/**
 *  Class for testing dom manipulation with help from {@link org.dlese.dpc.xml.schema.SchemaHelper}
 *
 * @author     ostwald<p>
 *
 *      $Id $
 * @version    $Id: DocReader.java,v 1.3 2009/03/20 23:33:58 jweather Exp $
 */
public class DocReader {

	private static boolean debug = true;

	Document doc = null;
	NamespaceStack stack = null;
	DocumentFactory df = null;
	NamespaceRegistry namespaces = null;
	
	/**
	 *  Constructor for the DocReader object
	 *
	 * @param  path  NOT YET DOCUMENTED
	 */
	DocReader(URL url) throws Exception {
		df = DocumentFactory.getInstance();
		stack = new NamespaceStack (df);
		namespaces = new NamespaceRegistry ();
		
		int timeOutSecs = 20;
		String content = TimedURLConnection.importURL(url.toString(), "UTF-8", timeOutSecs * 1000);
		prtln ("content:\n" + content);
		
		// doc = parseWithSAX (url);
		// doc = Dom4jUtils.getXmlDocument(url);
		// doc = WebServiceClient.getTimedXmlDocument(url);
		doc = Dom4jUtils.getXmlDocument(content);
		pp (doc);
		
		namespaces.registerNamespaces(doc);
		prtln (namespaces.toString());
	}

	private void showStack () {
		String s = "\nNamespace Stack";
		for (int i=0;i<stack.size();i++) {
			Namespace ns = stack.getNamespace (i);
			s += "\n\t" + i + ":  " + ns.getPrefix() + ": " + ns.getURI();
		}
		prtln (s);
	}
	
	/**
	 *  The main program for the DocReader class
	 *
	 * @param  args           The command line arguments
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public static void main(String[] args) throws Exception {

		URL url = new URL ("http://ns.nsdl.org/schemas/ed_type/ed_type_v1.00.xsd");
		DocReader t = new DocReader (url);
	


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
			// System.out.println("DocReader: " + s);
			System.out.println(s);
		}
	}

	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  aFile                  NOT YET DOCUMENTED
	 * @return                        NOT YET DOCUMENTED
	 * @exception  DocumentException  NOT YET DOCUMENTED
	 */
	public static Document parseWithSAX(File aFile) throws Exception {
		SAXReader xmlReader = new SAXReader();
		return xmlReader.read(aFile);
	}
	public static Document parseWithSAX(URL url) throws Exception {
		SAXReader xmlReader = new SAXReader();
		return xmlReader.read(url);
	}	
}

