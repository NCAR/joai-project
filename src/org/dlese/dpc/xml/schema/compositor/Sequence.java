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
import java.util.*;
import org.dom4j.Element;

/**
 *  Description of the Interface
 *
 *@author    ostwald
 */
public class Sequence extends Compositor {
	

	public Sequence (ComplexType parent) {
		super (parent);
	}
	
	public Sequence (ComplexType parent, Element e) {
		super (parent, e);
	}
	
		
	/**
	* Does the the given instanceElement accept ANY new member?
	
	NOTE: this method does not make sense! What we are interested in, is
	which of the CompositorMembers can accept another occurrence!
	*/
	public boolean acceptsNewMember (Element instanceElement) { 
		SequenceGuard guard;
		try {
			guard = new SequenceGuard (this, instanceElement);
		} catch (Exception e) {
			return false;
		}
		return guard.acceptsNewMember();
	}

	/**
	* Does the the given instanceElement accept a specifical new member at a specific
	location?
	
	WHEN would this be called? Compositors only care whether they can accept another
	OCCURRENCE!
	*/	
	public boolean acceptsNewMember (Element instanceElement, String memberName, int memberIndex) { 
		SequenceGuard guard;
		try {
			guard = new SequenceGuard (this, instanceElement);
		} catch (Exception e) {
			return false;
		}
		return guard.acceptsNewMember(memberName, memberIndex);
	}
	
	public int getType () {
		return Compositor.SEQUENCE;
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

