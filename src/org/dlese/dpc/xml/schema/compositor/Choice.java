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

import org.dlese.dpc.xml.schema.ComplexType;
import org.dlese.dpc.xml.schema.SchemaUtils;
import java.util.*;
import org.dom4j.Element;

/**
 *  Class representing the Choice compositor.
 *
 *@author    ostwald
 */
public class Choice extends Compositor {
	
	private static boolean debug = false;

	public Choice (ComplexType parent) {
		super (parent);
	}
	
	public Choice (ComplexType parent, Element e) {
		super (parent, e);
	}
	
	/**
	 * Returns true if there is room in instanceElement any new member within the
	 * constraints of the schema.
	 */
	public boolean acceptsNewMember (Element instanceElement) { 
		ChoiceGuard guard;
		prtln ("\nChoice.acceptsNewMember with \n" + instanceElement.asXML());
		try {
			guard = new ChoiceGuard (this, instanceElement);
		} catch (Exception e) {
			prtln ("ChoiceGard init error: " + e.getMessage());
			return false;
		}
		return guard.acceptsNewMember();
	}

	public boolean acceptsNewMember (Element instanceElement, String memberName, int memberIndex) { 
		ChoiceGuard guard;
		try {
			guard = new ChoiceGuard (this, instanceElement);
		} catch (Exception e) {
			return false;
		}
		return guard.acceptsNewMember(memberName, memberIndex);
	}
	
	/**
	* Returns true if instances of this compositor Must have at least one member. A Choice 
	compositor need not have a member if the compositor has minOccurs of 0 OR if one of the choices
	has minOccurs of 0.<p>
	NOTE: this is much simpler than calculating how many members are ultimately required ....
	*/
	public boolean isRequiredChoice () {
		if (this.getMinOccurs() == 0) {
			return false;
		}
		Iterator members = this.getMembers().iterator();
		while (members.hasNext()) {
			CompositorMember cm = (CompositorMember) members.next();
			if (cm.minOccurs == 0)
				return false;
		}
		return true;
	}
		
	
	/**
	 * Returns the members that could be added to the specified instanceElement
	 * according to schema constraints.
	 */
	public List getAcceptableMembers (Element instanceElement) { 
		prtln ("\ngetAcceptableMembers with instanceElement:");
		prtln (instanceElement.asXML());
		ChoiceGuard guard;
		try {
			guard = new ChoiceGuard (this, instanceElement);
		} catch (Exception e) {
			prtln ("ChoiceGard init error: " + e.getMessage());
			return new ArrayList();
		}
		prtln ("about to call ChoiceGuard getAcceptableMembers()");
		List members = guard.getAcceptableMembers();
		prtln ("acceptable members:");
		printMemberList (members);
		return members;
	}
	
	void printMemberList (List members) {
		if (members == null)
			prtln ("\t * choiceCompositor returned NULL");
		else if (members.isEmpty())
			prtln ("\t There are NO acceptable choices");
		else {
			for (Iterator i = members.iterator();i.hasNext();) {
				prtln ("\t" + (String)i.next());
			}
		}
	}
	
	public int getType () {
		return Compositor.CHOICE;
	}

	
	public List getChoiceNames() {
		return getMemberNames();
	}
	
	/**
	 *  Description of the Method
	 *
	 *@return    Description of the Return Value
	 */
 	 public String toString() {
		 String s = super.toString();
		 // s += "\n\tMY TYPE: " + getType();
		 return s;
	 }

	protected static void prtln (String s) {
		if (debug) {
			SchemaUtils.prtln(s, "Choice");
		}
	}

}

