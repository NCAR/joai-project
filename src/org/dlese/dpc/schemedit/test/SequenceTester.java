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

import org.dlese.dpc.xml.*;
import org.dlese.dpc.xml.schema.*;
import org.dlese.dpc.xml.schema.compositor.*;
import org.dlese.dpc.schemedit.*;
import org.dlese.dpc.util.strings.*;

import java.io.*;
import java.util.*;
import java.text.*;
import java.util.regex.*;

import java.net.*;
import org.dom4j.Node;
import org.dom4j.Element;
import org.dom4j.Document;
import org.dom4j.DocumentFactory;



/**

 * @author     Jonathan Ostwald
 */
 public class SequenceTester  extends CompositorTester {

	Sequence sequenceCompositor;
	Element instanceElement;

	public SequenceTester (String schemaName, String instanceDocPath, String xpath) throws Exception {
		super (schemaName, xpath);
		this.setInstanceDocument(instanceDocPath);
		this.instanceElement = getInstanceElement(xpath);
		try {
			this.sequenceCompositor = (Sequence) compositor;
		} catch (Exception e) {
			throw new Exception ("SequenceCompositor not obtained: " + e.getMessage());
		}
	}
	
	/**
	 *  The main program for the SequenceTester class
	 *
	 * @param  args           The command line arguments
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public static void main(String[] args) throws Exception {
		prtln("SequenceTester");
		
		String instanceDocPath = null;
		String xpath = null;
		
		String xmlFormat = "dlese_anno_TEST";
		if (args.length > 0)
			xmlFormat = args[0];
		
		if (xmlFormat.equals("dlese_anno_TEST")) {
			instanceDocPath = "C:/tmp/dlese_anno_record.xml";
			xpath = "/annotationRecord/moreInfo";
		}
		else if (xmlFormat.equals ("mets")) {
			instanceDocPath = "C:/tmp/mets_record.xml";
			xpath = "/this:mets/this:fileSec/this:fileGrp/this:file/this:FContent/this:xmlData";
		}
		else {
			throw new Exception ("xmlFormat (" + xmlFormat + ") not handled");
		}
		 
		SequenceTester tester = new SequenceTester (xmlFormat, instanceDocPath, xpath);   
		
		tester.showOccurrences ();
		
		tester.acceptsNewMember ();
		
		// but where would we ever use this??
		for (int i=0;i<4;i++) {
			// tester.acceptsNewMember ("cd:simpleAbstract", i);
		}
		
		// tester.acceptsNewSequenceMember ();
		// tester.printAcceptableChoices ();
		
		/* 	NOTE: we shouldn't be hardcoding "xsd" here! but i'm not positive where to 
			grab the schemaNamespace. Here i'm grabbing it from the compositor, but
			this has not been tested ...
		*/
		String schemaNSPrefix = tester.sequenceCompositor.getParent().getXsdPrefix();
		String anyTypeMemberName = NamespaceRegistry.makeQualifiedName(schemaNSPrefix, "any");
		CompositorMember cm = tester.sequenceCompositor.getMember(anyTypeMemberName);
		if (cm != null)
			prtln (cm.toString());
		else
			prtln ("CM for \"" + anyTypeMemberName + "\" not found");
	}
	

	
	void acceptsNewMember () {
		prtln ("\n-------------------------");
		prtln ("acceptsNewMember: Does the instance Element accept any new sequence member?");
		boolean ret = sequenceCompositor.acceptsNewMember(instanceElement);
		prtln (" ... acceptsNewMember returning " + ret);
	}
	
	void acceptsNewMember (String name, int index) {
		prtln ("\n-------------------------");
		prtln ("acceptsNewMember() name: " + name + " index: " + index);
		prtln ("\t Does the instance Element this member?");
		boolean ret =sequenceCompositor.acceptsNewMember (instanceElement, name, index);
		prtln (" ... acceptsNewMember (" + name + ", " + index + ") returning " + ret);
	}
	
	void showOccurrences () {
		SequenceGuard guard = (SequenceGuard) CompositorGuard.getInstance(sequenceCompositor, instanceElement);
		guard.printOccurrences();
		
	}
	
	void showOccurrences (SequenceGuard guard, String name) {
		prtln ("ocurrences of " + name + ": " + guard.getOccurrencesCount(name));
	}
	
	/**
	 * If the instanceElement can accept a new member, then the acceptable sequences should
	 * not be empty.
	 */
	boolean acceptsNewSequenceMember () {
		prtln ("\n-------------------------");
		prtln ("Does the instance Element accept any new sequence member?");
		boolean ret = sequenceCompositor.acceptsNewMember(instanceElement);
		if (ret)
			prtln ("compositor DOES accept new member");
		else
			prtln ("compositor does NOT accept new member");
		return ret;
	}




		/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  node  NOT YET DOCUMENTED
	 * @return       NOT YET DOCUMENTED
	 */
	static String pp(Node node) {
		try {
			return (Dom4jUtils.prettyPrint(node));
		} catch (Exception e) {
			return (e.getMessage());
		}
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  s  NOT YET DOCUMENTED
	 */
	static void prtln(String s) {
		while (s.charAt(0) == '\n') {
			System.out.println ("");
			s = s.substring(1);
		}
		System.out.println(s);
	}
	
}


