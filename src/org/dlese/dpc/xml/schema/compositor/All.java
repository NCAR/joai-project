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
import org.dlese.dpc.xml.schema.GlobalDef;
import org.dlese.dpc.xml.schema.GlobalElement;
import java.util.*;
import org.dom4j.Element;

/**
 *  Class representing the All compositor.
 *
 *@author    ostwald
 */
public class All extends Compositor {
	

	public All (ComplexType parent) {
		super (parent);
	}
	
	public All (ComplexType parent, Element e) {
		super (parent, e);
	}
	
	public boolean acceptsNewMember (Element instanceElement) { 
		AllGuard guard;
		try {
			guard = new AllGuard (this, instanceElement);
		} catch (Exception e) {
			return false;
		}
		return guard.acceptsNewMember();
	}

	public boolean acceptsNewMember (Element instanceElement, String memberName, int memberIndex) { 
		System.out.println ("WARNING: acceptsNewMember (instanceElement, MemberName, memberIndex) not implemented!");
		return (acceptsNewMember(instanceElement));
	}
	
	public int getType () {
		return Compositor.ALL;
	}

	/**
	 * not currently called. NOTE: "ref" elements are not handled correctly - this
	 * must be debugged if it is to be used
	 */
	public List getExpandedMemberNames () {
		List names = new ArrayList();
		for (Iterator i = element.elementIterator(); i.hasNext(); ) {
			Element e = (Element) i.next();
			String e_name = e.getName();
			String ref = e.attributeValue("ref", null);
			if (e_name.equals("element")) {
				if (ref != null) {
					// this is a reference to a GlobalElement - we need to check if it can be expanded
					GlobalDef typeDef = parent.getSchemaReader().getGlobalDef (ref);
					GlobalElement referredElement;
					try {
						referredElement = (GlobalElement)typeDef;
					} catch (Throwable t) {
						prtln ("WARNING: referred element was not a GlobalElement (found type: " + typeDef.getType() + ")");
						continue;
					}
					// 
					if (referredElement.hasSubstitutionGroup()) {
						names.addAll (getSubstitionGroupNames(referredElement));
						// unless the globalElement is abstract, add the original element
						// if (!ge.isAbstract()) names.add (ref);
						continue;
					}
				}
				else {
					names.add(e.attributeValue("name"));
				}
			}
			else {
				prtln ("WARNING: all compositor encountered an illegal member: " + e_name);
			}
		}
		return names;
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


}

