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

/*
Eventually we will have to account for the parent element (after all this is necessary in determining
whether a child choice can be added). But for now we concentrate on determining what choices have already been made.
1 - determine the choices that have already been made
we need to test an existing XML element against a type definition (ComplexType instance)
- a ComplexType instance can be read from a file containing
- XML element to be tested can be obtained in the same way
*/


/**

 * @author     Jonathan Ostwald
 */
 public class ChoiceTester extends CompositorTester {

	Choice choiceCompositor;
	Element instanceElement;

	public ChoiceTester (String schemaName, String instanceDocPath, String xpath) throws Exception {
		super (schemaName, xpath);
		this.setInstanceDocument(instanceDocPath);
		this.instanceElement = getInstanceElement(xpath);
		try {
			this.choiceCompositor = (Choice) compositor;
		} catch (Exception e) {
			throw new Exception ("ChoiceCompositor not obtained: " + e.getMessage());
		}
	}
	
	/**
	 *  The main program for the ChoiceTester class
	 *
	 * @param  args           The command line arguments
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public static void main(String[] args) throws Exception {
		prtln("ChoiceTester XXXXXXXXXXXXX");
		
 		String xmlFormat = "dlese_anno";
		String instanceDocPath = "/Users/ostwald/devel/lab-records/dlese_anno/1151575667866/ANNO-000-000-000-007.xml";
		String xpath = "/annotationRecord/annotation/content"; 
		
		ChoiceTester tester = new ChoiceTester (xmlFormat, instanceDocPath, xpath);   
	
		
		tester.showOccurrences ();
		
		tester.acceptsNewChoiceMember ();
		
		tester.printAcceptableChoices ();
	}
	
	void showOccurrences () {
		ChoiceGuard guard = (ChoiceGuard) CompositorGuard.getInstance(choiceCompositor, instanceElement);

		guard.printOccurrences();
		
/* 		showOccurrences (guard, "description");
		showOccurrences (guard, "rating");
		showOccurrences (guard, "url"); */
		
	}
	
	void showOccurrences (ChoiceGuard guard, String name) {
		prtln ("ocurrences of " + name + ": " + guard.getOccurrencesCount(name));
	}
	
	/**
	 * If the instanceElement can accept a new member, then the acceptable choices should
	 * not be empty.
	 */
	boolean acceptsNewChoiceMember () {
		prtln ("\n-------------------------");
		prtln ("Does the instance Element accept any new choice member?");
		boolean ret = choiceCompositor.acceptsNewMember(instanceElement);
		if (ret)
			prtln ("compositor DOES accept new member");
		else
			prtln ("compositor does NOT accept new member");
		return ret;
	}
	
	void printAcceptableChoices () {
		prtln ("\n-------------------------");
		prtln ("AcceptableChoices for instance Element:");
		List choices = choiceCompositor.getAcceptableMembers(instanceElement);
		if (choices == null)
			prtln ("\t * choiceCompositor returned NULL");
		else if (choices.isEmpty())
			prtln ("\t There are NO acceptable choices");
		else {
			for (Iterator i = choices.iterator();i.hasNext();) {
				prtln ("\t" + (String)i.next());
			}
		}
	}
	
	void testChoiceCompositor () {
		prtln ("\n---------\ntesting Choice Compositor");
		int maxOccurs = choiceCompositor.getMaxOccurs();
		prtln ("\t max choices allowed: " + maxOccurs);
		
		List leafMembers = choiceCompositor.getLeafMembers();
		prtln ("choiceCompositor leaf members (" + leafMembers.size() + ")");
		for (Iterator i=leafMembers.iterator();i.hasNext();) {
			CompositorMember m = (CompositorMember)i.next();
			prtln ("\t" + m.getInstanceQualifiedName() + "  maxOccurs: " + m.maxOccurs + "  minOccurs: " + m.minOccurs);
		}
		
 		List choices = choiceCompositor.getAcceptableMembers(instanceElement);
		prtln ("acceptable members (" + choices.size() + ")");
		for (Iterator i=choices.iterator();i.hasNext();) {
			prtln ("\t" + (String) i.next());
		}
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


