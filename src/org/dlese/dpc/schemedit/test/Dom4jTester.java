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
 * @version    $Id: Dom4jTester.java,v 1.3 2009/03/20 23:33:58 jweather Exp $
 */
public class Dom4jTester {

	private static boolean debug = true;

	Document doc = null;
	DocumentFactory df = null;
	NamespaceRegistry namespaces = null;
	MyCache namespaceCache = null;
	MyStack stack = null;
	
	/**
	 *  Constructor for the Dom4jTester object
	 *
	 * @param  path  NOT YET DOCUMENTED
	 */
	Dom4jTester(URL url) throws Exception {
		df = DocumentFactory.getInstance();
		stack = new MyStack (df);
		namespaces = new NamespaceRegistry ();
		namespaceCache = new MyCache();
		namespaceCache.getCache().clear();
		
 		doc = parseWithSAX (url);
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
	 *  The main program for the Dom4jTester class
	 *
	 * @param  args           The command line arguments
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public static void main(String[] args) throws Exception {

		URL url = new URL ("http://www.dpc.ucar.edu/people/ostwald/Metadata/NameSpacesPlay/instance.xml");
		Dom4jTester t = new Dom4jTester (url);

		cacheTest (t);
		
		MyStack s = t.stack;
		prtln ("\nInitial: " + s.toString());
		for (Iterator i=t.namespaces.getNamespaces().iterator();i.hasNext();) {
			s.push ((Namespace)i.next());
		}
		s.push (s.makeNamespace ("foo", "http://www.tape.com"));
		prtln ("\n" + s.toString());
		
		Namespace dsn = s.getDefaultNamespace();
		prtln ("default: " + nsToString(dsn));
		
		s.pop ("");
		dsn = s.getDefaultNamespace();
		if (dsn != null)
			prtln ("default: " + nsToString(dsn));
		
		Map nsCache = s.getCache ();
		showMap (nsCache, "\t");
		
	}
	
	static String nsToString (Namespace ns) {
		return ns.getPrefix() + " : " + ns.getURI();
	}
	
	static void cacheTest (Dom4jTester t) throws Exception {
			
		// URL url2 = new URL ("file:/devel/ostwald/metadata-frameworks/collection-v1.0.00/collection.xsd");
		parseWithSAX (new URL ("http://www.dpc.ucar.edu/people/ostwald/Metadata/NameSpacesPlay/tape.xsd"));
		parseWithSAX (new URL ("http://www.dpc.ucar.edu/people/ostwald/Metadata/NameSpacesPlay/track.xsd"));

		t.namespaceCache.get ("foo", "http://foo.com");
		Namespace gotNS = t.namespaceCache.get ("http://foo.com");
		if (gotNS != null) {
			prtln ("gotNS: " + gotNS.getPrefix() + " : " + gotNS.getURI());
		}
		else
			prtln ("gotNS is null");
			
		Map cache = t.namespaceCache.getCache();
		prtln ("Cache");
		showCache (cache, "\t");
		
		Map uriCache = t.namespaceCache.getURICache("http://www.newInstance.com/cd");
		prtln ("URI Cache");
		showCache (uriCache, "\t");
	}
	
	static void showCache (Map cache, String indent) {
		prtln ("Cache has " + cache.size() + " entries");
		for (Iterator i=cache.keySet().iterator();i.hasNext();) {
			String uri = (String)i.next();
			Map subCache = (Map) cache.get (uri);
			prtln (indent + uri + "(" + subCache.size() + ")");
			for (Iterator ii=subCache.keySet().iterator();ii.hasNext();) {
				String prefix = (String)ii.next();
				Namespace ns = (Namespace)subCache.get (prefix);
				prtln (indent + indent + prefix + " -- " + nsToString (ns));
			}
		}
	}
			
	
	static void showMap (Map map, String indent) {
		prtln (indent + "map has " + map.size() + " entries");
		for (Iterator i=map.keySet().iterator();i.hasNext();) {
			String uri = (String)i.next();
			prtln ("\t" + uri);
			Object o = map.get (uri);
			// prtln (indent + uri + " : " + o.getClass().getName());
			if (o instanceof Namespace)
				prtln (indent + uri + " : " + nsToString( (Namespace)o));
			if (o instanceof Map) 
				showMap ((Map) o, indent+indent);
		}
	}
	
	private Map getUrisFromScratch () {
		Map uris = new HashMap ();
		Namespace namedDefaultNamespace = namespaces.getNamedDefaultNamespace();
		uris.put (namedDefaultNamespace.getPrefix(), namedDefaultNamespace.getURI());
		return uris;
	}
	
	private Map getUrisFromNR () {
		Map uris = new HashMap ();
		for (Iterator i=namespaces.getPrefixMap().keySet().iterator();i.hasNext();) {
			String prefix = (String)i.next();
			Namespace ns = namespaces.getNSforPrefix(prefix);
/* 			if (prefix.trim().length() > 0)
				uris.put(prefix, ns.getURI()); */
			uris.put(prefix, ns.getURI());
		}
		return uris;
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
			// System.out.println("Dom4jTester: " + s);
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

