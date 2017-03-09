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
package org.dlese.dpc.xml.schema;

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
 * @version    $Id: SchemaNamespaceConverter.java,v 1.3 2009/03/20 23:34:01 jweather Exp $
 */
public class SchemaNamespaceConverter {

	private static boolean debug = false;

	public static final String SCHEMA_NAMESPACE_URI = "http://www.w3.org/2001/XMLSchema";
	
	Document doc = null;
	DocumentFactory df = null;
	NamespaceRegistry namespaces = null;
	Namespace schemaNS = null;
	String schemaNSPrefix = null;
	
	/**
	 *  Constructor for the SchemaNamespaceConverter object
	 *
	 * @param  path  NOT YET DOCUMENTED
	 */
	SchemaNamespaceConverter() {
		df = DocumentFactory.getInstance();
	}

	 Document convert (Document doc, String nsPrefix) {
		prtln ("convert");
		this.doc = doc;
		pp (doc);
		schemaNSPrefix = nsPrefix;
		schemaNS = new Namespace (schemaNSPrefix, SCHEMA_NAMESPACE_URI);
		Element root = doc.getRootElement();
		if (!schemaNSisDefault (root))
			return doc;
		root.add (schemaNS);
		root.setQName (new QName (root.getName(), schemaNS));
		prtln (root.getQualifiedName() + " - " + root.getName());
		convert (doc.getRootElement(), 2);
		return doc;
	}
	
	void convert (Element e, int level) {
		for (Iterator i=e.elementIterator();i.hasNext();) {
			Element child = (Element)i.next();
			Namespace ns = child.getNamespace();
			if (!SCHEMA_NAMESPACE_URI.equals(ns.getURI())) {
				prtln ("child namespace does not match schemaNamespaceURI for " + child.getQualifiedName());
				return;
			}
			String s = "";
			child.setQName (new QName (child.getName(), schemaNS));
			for (int j=0;j<level;j++)
				s += "  ";
			s += child.getQualifiedName();
			prtln (s);
			convert (child, level+1);
		}
	}
	
	/**
	* Test that the original schema namespace is indeed the default namespace - namely, it has no prefix and
	the namespaceURI matches SCHEMA_NAMESPACE_URI.
	*/
	private boolean schemaNSisDefault (Element root) {
		Namespace ns = root.getNamespace();
		prtln ("original namespace of root element: " + NamespaceRegistry.nsToString(ns));
		if (ns.getPrefix() != null && ns.getPrefix().trim().length() > 0) {
			prtln ("\t there is an original namespace prefix (" + ns.getPrefix() + ") returning false");
			return false;
		}
		if (!SCHEMA_NAMESPACE_URI.equals(ns.getURI())) {
			prtln ("\t schemaURI (" + ns.getURI() + ") does not match SCHEMA_NAMESPACE_URI - returning false");
			return false;
		}
		prtln ("\t schemaNSisDefault: root namespace (" + NamespaceRegistry.nsToString(ns) + ")");
		return true;
	}
	
	/**
	 *  The main program for the SchemaNamespaceConverter class
	 *
	 * @param  args           The command line arguments
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public static void main(String[] args) throws Exception {

		// String path = "/Library/WebServer/Documents/metadata-frameworks/nsdl-oai/oai_dc.xsd";
		String path = "/devel/ostwald/metadata-frameworks/NSDL-OAI/oai_dc_local.xsd";
		if (args.length > 0)
			path = args[0];
		
		prtln ("schema path: " + path);
		Document doc = Dom4jUtils.getXmlDocument (new File (path));
		String nsPrefix = "xsi";
		SchemaNamespaceConverter converter = new SchemaNamespaceConverter ();
		doc = converter.convert(doc, nsPrefix);
		pp (doc);
		converter.doNodeProbes();
		
	}
	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  n  NOT YET DOCUMENTED
	 */
	private static void pp(Node n) {
		prtln(Dom4jUtils.prettyPrint(n));
	}

	void doNodeProbes () {
		nodeProbe ("/xsi:schema");
		nodeProbe ("/xsi:schema/xsi:annotation");
		nodeProbe ("/xsi:schema/xsi:import");
		nodeProbe ("/xsi:schema/xsi:complexType/xsi:choice");
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
			// System.out.println("SchemaNamespaceConverter: " + s);
			System.out.println(s);
		}
	}	
}

