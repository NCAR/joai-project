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
import org.dlese.dpc.xml.XSLTransformer;
import org.dlese.dpc.xml.Dom4jUtils;
import org.dlese.dpc.xml.XPathUtils;
import org.dlese.dpc.xml.schema.*;
import org.dlese.dpc.util.Files;
import org.dom4j.*;
import org.dom4j.io.*;
import org.apache.struts.util.LabelValueBean;

/**
 *  Class for testing dom manipulation with help from {@link org.dlese.dpc.xml.schema.SchemaHelper}
 *
 * @author     ostwald<p>
 *
 *      $Id $
 * @version    $Id: SGTester.java,v 1.3 2009/03/20 23:33:58 jweather Exp $
 */
public class SGTester {

	private static boolean debug = true;

	Document doc = null;
	DocMap docMap = null;
	String path = null;
	SchemaHelper schemaHelper = null;

	/**
	 *  Constructor for the SGTester object
	 *
	 * @param  path  NOT YET DOCUMENTED
	 */
 	SGTester(String path, URL schemaUrl) throws Exception {
		schemaHelper = new SchemaHelper(schemaUrl);
		this.path = path;
		prtln ("\nSGTester");
		prtln ("reading from\n\t" + path);
		doc = parseWithSAX(new File(path));
		docMap = new DocMap(doc, schemaHelper);
	}
	
	/**
	 *  The main program for the SGTester class
	 *
	 * @param  args           The command line arguments
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public static void main(String[] args) throws Exception {
		String path = "/devel/ostwald/SchemEdit/NameSpaces/tester-sg.xml";
		String attachPath = "/cd:cd/tp:artist";
		// attachPath = "/cd:cd/cd:artist";
		if (args.length > 0)
			attachPath = args[0];
		SGTester t = null;
		URL schemaUrl = new URL ("http://www.dpc.ucar.edu/people/ostwald/Metadata/NameSpacesPlay/cd.xsd");
		try {
			t = new SGTester (path, schemaUrl);
		} catch (Exception e) {
			prtln ("ERROR: " + e.getMessage());
			return;
		}

		prtln ("\n===============================================\n");
		
		String xpath = "/cd:cd/cd:abstractElement";
		// t.showSGoptions (xpath);
		
		// t.getSubStitutionGroupMembersOf(xpath);
		
		t.showSubstitutionElement (attachPath);

		t.doCdNodeProbes();
		
		t.attachNewElement (attachPath);
		
		t.doCdNodeProbes();
	}
		
	void doCdNodeProbes () {
		prtln ("\n---------------------");
		nodeProbe ("/cd:cd");
		nodeProbe ("/cd:cd/@nameTitle");
		nodeProbe ("/cd:cd/@tp:rating");
		nodeProbe ("/cd:cd/cd:extnTest");
		nodeProbe ("/cd:cd/cd:extnTest/@testAttribute");
		nodeProbe ("/cd:cd/cd:artist");
		nodeProbe ("/cd:cd/tp:artist");
		nodeProbe ("/cd:cd/cd:id");
		nodeProbe ("/cd:cd/cd:info");
		nodeProbe ("/cd:cd/cd:info/sh:author");

	}
	void nodeProbe (String path) {
		// Document doc = sh.getInstanceDocument();
		Node n = docMap.selectSingleNode (path);
		if (n == null)
			prtln("node NOT found at " + path);
		else
			prtln ("node FOUND at " + path);
	}
	
	void attachNewElement(String pathArg)
		throws Exception {
			prtln ("\n---------------------");
		prtln ("attachNewElement() attachPath: " + pathArg);
		
		prtln ("document as read from file");
		pp (docMap.getDocument());
		
		List l = docMap.selectNodes(pathArg);
		prtln ("\n\t" + l.size() + " nodes found for " + pathArg);
		
 		Element newElement = schemaHelper.getNewElement(pathArg);
		if (newElement == null) {
			throw new Exception("getNewElement failed");
		}
		else {
 			prtln ("\n ... calling docMap.addElement()");
			if (!docMap.addElement(newElement, pathArg)) {
				throw new Exception("docMap.addElement failed");
			}
		}
		prtln ("after adding new element");
		pp (docMap.getDocument());
		
		l = docMap.selectNodes(pathArg);
		prtln ("\n\t" + l.size() + " nodes found for " + pathArg);
		
	}
	
	void getNewElementTest (String path) {
		prtln ("\n---------------------");
		Element newElement = schemaHelper.getNewElement(path);
		prtln ("newElement at " + path);
		pp (newElement);
	}
	
	void showSGoptions (String path) {
		prtln ("\n---------------------");
		prtln ("Substitution Group options for " + path);
		LabelValueBean [] options = getSubstitutionGroupOptions (path);
		prtln ("    " + options.length + " options found");
		for (int i=0;i<options.length;i++) {
			prtln ("\t" + i + ": " + options[i].getValue());
		}
	}
	
	void showSubstitutionElement (String path) {
		prtln ("\n---------------------");
		SchemaNode schemaNode = schemaHelper.getSchemaNode(path);
		if (schemaNode.isSubstitutionGroupMember())
			prtln ("sub element" + schemaNode.getSubstitutionElement().asXML());
		else
			prtln ("no sub element found for " + path);
	}
	
	/**
	 * select all substitutionGroup elements by building up complex xpath ORing together
	 * the xpaths for the individual members
	 */
	public List getSubStitutionGroupMembersOf(String encodedPath) {
		prtln ("\n---------------------");
		// List memberPaths = new ArrayList();
		String selectionPath = "";
		String xpath = XPathUtils.decodeXPath(encodedPath);
		SchemaNode schemaNode = schemaHelper.getSchemaNode(xpath);
		if (schemaNode == null || !schemaNode.isHeadElement())
			return new ArrayList();
		
		if (!schemaNode.isAbstract()) {
			// memberPaths.add (xpath);
			selectionPath = xpath;
		}
		String parentXPath = XPathUtils.getParentXPath(xpath);
		Iterator groupMembers=schemaNode.getSubstitutionGroup().iterator();
		while (groupMembers.hasNext()) {
			GlobalElement member = (GlobalElement)groupMembers.next();
			if (selectionPath.length() > 0)
				selectionPath += " | ";
			String memberPath = parentXPath + "/" + member.getQualifiedInstanceName();
			// memberPaths.add (memberPath);
			selectionPath += memberPath;
		}
		prtln ("selectionPath: " + selectionPath);
		List nodes = docMap.selectNodes(selectionPath);
		prtln ("    " + nodes.size() + " elements found");
		return nodes;
	}

		
	/**
	 * given the path to a schemaNode that hasSubstitutionGroup(), return an array of LabelValueBean objects representing,
	 the substitutionGroup, where both the label and the value are the group memebers qualifiedName
	 */
	public LabelValueBean[] getSubstitutionGroupOptions(String encodedPath) {
		// prtln ("getSubstitutionGroupOptions() with encodedPath = " + encodedPath);
		// normalize path since we are after schema information

		LabelValueBean[] emptyArray = new LabelValueBean[]{};
		String xpath = SchemaHelper.toSchemaPath(encodedPath);
		SchemaNode schemaNode = schemaHelper.getSchemaNode (xpath);
		if (!schemaNode.isHeadElement())
			return emptyArray;
		
		List values = new ArrayList ();
		if (!schemaNode.isAbstract())
			values.add (XPathUtils.getNodeName(xpath));
		for (Iterator members=schemaNode.getSubstitutionGroup().iterator();members.hasNext();) {
			GlobalElement ge = (GlobalElement)members.next();
			values.add (ge.getQualifiedInstanceName());
		}
		
		if (values.isEmpty())
			return emptyArray;

		LabelValueBean[] options = new LabelValueBean[values.size()];
		for (int i = 0; i < values.size(); i++) {
			String value = (String) values.get(i);
			String label = (String) values.get(i);
			options[i] = new LabelValueBean(label, value);
		}
		prtln("getSubstitutionGroupOptions() returning " + options.length + " LabelValueBeans");
		return options;
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
			// System.out.println("SGTester: " + s);
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


	
}

