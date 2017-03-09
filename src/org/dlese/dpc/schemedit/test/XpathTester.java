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
import org.dlese.dpc.xml.Dom4jUtils;
import org.dlese.dpc.xml.schema.*;
import org.dlese.dpc.util.Files;
import org.dom4j.*;
import org.dom4j.tree.*;
import org.dom4j.io.*;


/**
 *  Class for testing dom manipulation with help from {@link org.dlese.dpc.xml.schema.SchemaHelper}
 *
 * @author     ostwald<p>
 *
 *      $Id $
 * @version    $Id: XpathTester.java,v 1.4 2009/07/07 03:29:38 ostwald Exp $
 */
public class XpathTester {

	private static boolean debug = true;

	Document doc = null;
	DocumentFactory df = null;
	NamespaceRegistry namespaces = null;
	MyCache namespaceCache = null;
	MyStack stack = null;
	
	/**
	 *  Constructor for the XpathTester object
	 *
	 * @param  path  NOT YET DOCUMENTED
	 */
	XpathTester(String path) throws Exception {
		df = DocumentFactory.getInstance();
		doc = Dom4jUtils.getXmlDocument (new File (path));
	}

	/**
	 *  The main program for the XpathTester class
	 *
	 * @param  args           The command line arguments
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public static void main(String[] args) throws Exception {

		// nodeProbeTester();
		
		pathSortTester();
		
	}
	
	static void pathSortTester () {
		List paths = getXPaths();
		Collections.sort (paths);
		for (Iterator i=paths.iterator();i.hasNext();) {
			prtln ((String)i.next());
		}
	}
	
	static List getXPaths () {
		List paths = new ArrayList();
		paths.add ("/record/a");
		paths.add ("/record/b[1]");
		paths.add ("/record/b[12]");
		paths.add ("/record/b[2]");
		return paths;
	}
	
	
	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  n  NOT YET DOCUMENTED
	 */
	private static void pp(Node n) {
		prtln(Dom4jUtils.prettyPrint(n));
	}

	static void nodeProbeTester () throws Exception {
		String path = "/home/ostwald/XML/Untitled3.xml";
		XpathTester t = new XpathTester (path);
		t.doNodeProbes();
	}
	
	void doNodeProbes () {
		nodeProbe ("/cd:cd");
		nodeProbe ("/cd:cd/@nameTitle");
		nodeProbe ("/cd:cd/@tp:rating");
		nodeProbe ("/cd:cd/cd:id");
		nodeProbe ("/cd:cd/sh:album");
		nodeProbe ("/cd:cd/sh:album/@key");
		nodeProbe ("/cd:cd/cd:info");
		nodeProbe ("/cd:cd/cd:tape/@newAttr");
		nodeProbe ("/cd:cd/cd:tape/@tp:newAttr");
	}
	void nodeProbe (String path) {
		Node n = doc.selectSingleNode (path);
		if (n == null)
			prtln("node NOT found at " + path);
		else
			prtln ("node FOUND at " + path);
	}
	

	/**
	 *  Description of the Method
	 *
	 * @param  s  Description of the Parameter
	 */
	private static void prtln(String s) {
		if (debug) {
			// System.out.println("XpathTester: " + s);
			System.out.println(s);
		}
	}

	class MyCache extends NamespaceCache {
		public MyCache () {
			super();
		}
		
		protected Map getURICache (String uri) {
			return super.getURICache(uri);
		}
		
		Map getCache () {
			return super.cache;
		}
	}
	
	class MyStack extends NamespaceStack {
		public MyStack () {
			super();
		}
		
		public Namespace makeNamespace(String prefix, String uri) {
			return super.createNamespace(prefix, uri);
		}
		
		public MyStack (DocumentFactory df) {
			super(df);
		}
		
		public void push (Namespace ns) {
			super.push(ns);
			prtln ("  ... stack (" + size() + ") PUSHED namespace: "  + ns.getPrefix() + ": " + ns.getURI());
		}
	
		public Namespace pop () {
			Namespace ns = super.pop();
			prtln ("  ... stack (" + size() + ") POPPED namespace: "  + ns.getPrefix() + ": " + ns.getURI());
			return ns;
		}
			
		public String toString () {
			String s = "\nNamespace Stack";
			for (int i=0;i<size();i++) {
				Namespace ns = getNamespace (i);
				s += "\n\t" + i + ":  " + ns.getPrefix() + ": " + ns.getURI();
			}
			return s;
		}
		
		Map getCache () {
			return getNamespaceCache();
		}
	}
		
	
}

