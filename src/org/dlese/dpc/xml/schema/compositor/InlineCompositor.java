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

import org.dlese.dpc.xml.schema.*;
import java.util.*;
import org.dom4j.Element;

/**
 *  InlineCompositor class specifies methods for accessing and validating the "members" of a InlineCompositor Element (i.e., 
 All, Sequence and Choice), as well as the acceptsNewMember method. The members are a list of CompositorMember instances which
 represent the child elements of the InlineCompositor element.

 *
 *@author    ostwald
 */
public class InlineCompositor extends Compositor {
	
	Compositor parentCompositor;

	public InlineCompositor (ComplexType parentTypeDef, Element compositorElement) {
		this (parentTypeDef, compositorElement, null);
	}
	
	public InlineCompositor (ComplexType parentTypeDef, Element compositorElement, Compositor parentCompositor) {
		super (parentTypeDef, compositorElement);
		this.parentCompositor = parentCompositor;
	}
	
	/**
	 *  Returns an integer contant that specifies whether this InlineCompositor is Sequence, Choice, All.
	 *
	 *@return    The dataType value
	 */
	 public  int getType() {
		 String name = element.getName();
		 if (name.equals("all"))
			 return Compositor.ALL;
		 if (name.equals("sequence"))
			 return Compositor.SEQUENCE;
		 if (name.equals("choice"))
			 return Compositor.CHOICE;	 
		 return Compositor.UNKNOWN;
	 }

	 public Compositor getParentCompositor () {
		 return parentCompositor;
	 }

	/**
	 *  Description of the Method
	 *
	 *@return    Description of the Return Value
	 */
	 public String toString() {
		 String s = "InlineCompositor: " + getName();
		 s += "\n\t" + "type: " + getType();
		 s += "\n\t" + "maxOccurs: " + maxOccurs;
		 s += "\n\t" + "minOccurs: " + minOccurs;
		 // s += "\n\t" + "element: \n\t" + getElement().asXML();
		 s += "\n\t" + "member names:";
		 for (Iterator i=getMembers().iterator();i.hasNext();) {
			 CompositorMember member = (CompositorMember)i.next();
			 try {
				 s += "\n\t\t -- " + member.getInstanceQualifiedName();
			 } catch (Throwable t) {
				 s += "\n\t\t -- " + "WARNING: could not obtain member name: " + member.getElement();
			 }
		 }
		 return s;
	 }


	/**
	 *  Returns true if a given instance document element can accept a new member according to schema-defined 
	 constraints for this InlineCompositor.
	
	 *
	 *@return    The simpleType value
	 */
	 public boolean acceptsNewMember(Element instanceDocElement) {
		 return false;
	 }

	 public boolean acceptsNewMember (Element instanceElement, String memberName, int memberIndex) { 
		System.out.println ("WARNING: acceptsNewMember (instanceElement, MemberName, memberIndex) not implemented!");
		return (acceptsNewMember(instanceElement));
	}
	 
}

