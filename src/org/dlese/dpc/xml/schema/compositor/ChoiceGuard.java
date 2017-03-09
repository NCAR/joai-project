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
package org.dlese.dpc.xml.schema.compositor;

import java.util.*;
import java.lang.Math;
import org.dlese.dpc.xml.Dom4jUtils;
import org.dlese.dpc.xml.schema.SchemaUtils;

import org.dom4j.Element;
import org.dom4j.Node;

/**
 *  CompositorGuard for the Choice compositor.
 *
 * @author     ostwald
 */
public class ChoiceGuard extends CompositorGuard {

	private static boolean debug = false;
	private boolean mutuallyExclusive = false;
	
	/**
	 *  Constructor for the ChoiceGuard object
	 *
	 * @param  compositor       NOT YET DOCUMENTED
	 * @param  instanceElement  NOT YET DOCUMENTED
	 */
	public ChoiceGuard(Compositor compositor, Element instanceElement) {
		this (compositor, instanceElement.elements());
	}

	public ChoiceGuard(Compositor compositor, List instanceList) {
		super(compositor, instanceList);
	}

	/**
	 *  Returns a list of element names representing the choices that can be legally made.
	 *
	 * @return    The acceptableMembers value
	 */
	public List getAcceptableMembers() {
		prtln ("getAcceptableMembers()");
		List accepts = new ArrayList();
		List legalMemberNames = compositor.getMemberNames();
		prtln ("about to check " + legalMemberNames.size() + " names");
		for (Iterator i = legalMemberNames.iterator(); i.hasNext(); ) {
			String name = (String) i.next();
			prtln ("looking at " + name);
			if (acceptsNewMember(name)) {
				prtln (" ... accepts " + name);
				accepts.add(name);
			}
			else {
				prtln (" ... does NOT accept " + name);
			}
		}
		return accepts;
	}

	/**
	 *  Returns a list of occurrence instances that can be used to determine
	 *  whether an instance document element satisfies the occurrence constraints
	 *  of the schema.<p>
	 *  Walk down the buckets, when there is no room to add the current member,
	 *  start an new occurrance. NOTE: the order in which we add members is 
	 *  aribitrary - we just can't overfill any single member.<p>
	 
	 NOTE: this does not account for many of the children choice compositors can have:
	 - choice
	 - sequence
	 - any
	 - element
	 - group
	 *
	 *@return                The occurrences value
	 *@exception  Exception  NOT YET DOCUMENTED
	 */
	protected List getOccurrences() throws Exception {

		List occurrences = new ArrayList();
		Occurrence o = null;

		Iterator iMembers = this.instanceMembers.iterator();
		/* compositor.getLeafMemberNames(); */
		
		CompositorMember lastCM = null;
		
		while (iMembers.hasNext()) {
			Element memberElement = (Element) iMembers.next();
			String leafName = memberElement.getQualifiedName();
			
						
			// sanity check - must find a member for this child
			if (!compositor.getLeafMemberNames().contains(leafName)) {
				throw new Exception("Unexpected child element: \"" + leafName + "\"");
			}
			
			CompositorMember cm = compositor.getMember (leafName);
			if (cm == null)
				throw new Exception("Could not find CM for: \"" + leafName + "\"");
			
			
			if (cm != lastCM || o.getMemberCount (cm) == cm.maxOccurs) {
				// start a new occurrence
				o = new Occurrence(compositor);
				occurrences.add (o);
			}
			
			o.add (memberElement);
			lastCM = cm;
		}

		return occurrences;
	}


	/**
	 *  NOTE: if any of the choice compositor members has a minOccurs of 0, then there need not be any occurrances!
	 *
	 * @return                NOT YET DOCUMENTED
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public boolean checkValid() throws Exception {
		if (parsingError != null)
			throw new Exception(parsingError);

		int occurs = getOccurrencesCount();
		int minOccurs = compositor.getMinOccurs();
		int maxOccurs = compositor.getMaxOccurs();
		if (minMemberOccurs() == 0)
			minOccurs = 0;
		if (occurs < minOccurs)
			throw new Exception(compositor.getName() + " must occur at least " + minOccurs + " times");
		if (occurs > maxOccurs)
			throw new Exception(compositor.getName() + " may not occur more than " + maxOccurs + " times");
		return true;
	}

	/**
	* Returns the smallest of the minOccurs attributes of the choice members.
	*/
	int minMemberOccurs () {
		int minOccurs = Integer.MAX_VALUE;
		for (Iterator i=compositor.getMembers().iterator();i.hasNext();) {
			CompositorMember member = (CompositorMember)i.next();
			minOccurs = Math.min(minOccurs, member.minOccurs);
		}
		return minOccurs;
	}

	/**
	 *  Returns true if a new member may be added. <P>Takes into account whether there is a legal choice
	 to be made, as well as the maxOccurs constraint of the compositor.
	 *
	 * @return    NOT YET DOCUMENTED
	 */
	public boolean acceptsNewMember() {
		prtln ("choiceGuard.acceptsNewMember()");
		prtln ("\t super.acceptsNewMember(): " + super.acceptsNewMember());
		prtln ("\t getAcceptableMembers().size(): " + getAcceptableMembers().size());
		return (super.acceptsNewMember() && getAcceptableMembers().size() > 0);
	}
	
	/**
	 *  returns if the instanceElement can accept 
	 *
	 * @param  name  NOT YET DOCUMENTED
	 * @return       NOT YET DOCUMENTED
	 */
	private boolean acceptsNewMember(String name) {
		prtln ("acceptsNewMember with " + name);
		CompositorMember member = compositor.getMember(name);
		if (member == null) {
			prtln("acceptsNewMember could not find compositor member for \"" + name + "\"");
			return false;
		}

		// int occurrences = occurrenceCounter.getCount(name);
		int occurCnt = getOccurrencesCount(name);

		// mutuallyExclusive does NOT inforce the semantics of the choice compositor, but
		// it does enforce the intent of the multi-choice in the dlese_anno 1.0.00 framework
		
		boolean ret = false;
		
		if (mutuallyExclusive) {
			// this test does not allow choices to be made more than one time, reguardless of schema semantics
			prtln ("\t occurrences of " + name + ": " + occurCnt);
			prtln ("\t total occurrences: " + getOccurrencesCount());
			prtln ("\t compositor maxOccurrs: " + compositor.getMaxOccurs());
			
			ret = (getOccurrencesCount() < compositor.getMaxOccurs() &&
				occurCnt < 1);
		}
		else {
			
			int memberCount = member.getMaxInstanceElements();
			prtln ("\t occurrences of " + name + ": " + occurCnt);
			prtln ("max members for " + name + ": " + memberCount);
			
			// this test DOES enforce the choice compositor semantics, but it is not yet
			// supported in the Renderers (requires indexing of children) ...
			ret = getOccurrencesCount() < compositor.getMaxOccurs();
		}
		prtln ("  --> returning " + ret);
		return ret;
	}


	/**
	 *  Returns a list of occurrence instances that can be used to determine whether an instance document element
	 *  satisfies the occurrence constraints of the schema.
	 *
	 * @return                The occurrences value
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
/* 	protected List getOccurrences() throws Exception {
		prtln ("getOccurrences()");
		occurrenceCounter = new OccurrenceCounter();
		occurrences = new ArrayList();
		List groups = getElementGroups();

		// debugging
		prtln ("\t " + groups.size() + " groups found");
		// printElementGroups();

		// prtln (" iterating through groups ...");
		for (Iterator i = groups.iterator(); i.hasNext(); ) {
			ElementGroup group = (ElementGroup) i.next();
			String name = group.name;
			// prtln ("\t " + name);
			try {
				CompositorMember member = compositor.getLeafMember(name);
				if (member == null) {
					throw new Exception("couldnt find member for " + name);
				}
				Occurrence occurrence = new Occurrence(member);
				occurrence.elements = group.elements;
				occurrenceCounter.inc (name);
				occurrences.add(occurrence);

			} catch (Exception e) {
				throw new Exception("error with \"" + name + "\": " + e.getMessage());
			}
		}
		return occurrences;
	} */


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
	protected static void prtln (String s) {
 		if (debug) {
/* 			while (s.length() > 0 && s.charAt(0) == '\n') {
				System.out.println ("");
				s = s.substring(1);
			}
			
			System.out.println("ChoiceGuard: " + s); */
			SchemaUtils.prtln(s, "ChoiceGuard");
		}
	}

}

