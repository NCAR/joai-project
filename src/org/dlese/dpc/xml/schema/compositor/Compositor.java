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
import org.dlese.dpc.xml.Dom4jUtils;
import java.util.*;
import org.dom4j.Element;

/**
 *  Compositor class specifies methods for accessing and validating the
 *  "members" of a Compositor Element (i.e., All, Sequence and Choice), as well
 *  as the acceptsNewMember method. The members are a list of CompositorMember
 *  instances which represent the child elements of the compositor element.
 *
 * @author     ostwald
 */
public abstract class Compositor {
	/**  NOT YET DOCUMENTED */
	protected static boolean debug = false;

	/**  NOT YET DOCUMENTED */
	public final static int UNKNOWN = -1;
	/**  NOT YET DOCUMENTED */
	public final static int SEQUENCE = 0;
	/**  NOT YET DOCUMENTED */
	public final static int CHOICE = 1;
	/**  NOT YET DOCUMENTED */
	public final static int ALL = 2;

	/**  NOT YET DOCUMENTED */
	protected int minOccurs;
	/**  NOT YET DOCUMENTED */
	protected int maxOccurs;
	private int maxInstanceElements = -1;

	/**  NOT YET DOCUMENTED */
	protected ComplexType parent = null;
	/**  NOT YET DOCUMENTED */
	protected Element element = null;
	/**  NOT YET DOCUMENTED */
	protected String name = "";
	/**  NOT YET DOCUMENTED */
	protected List members = null;
	/**  NOT YET DOCUMENTED */
	protected List leafMembers = null;

	/**  NOT YET DOCUMENTED */
	private List leafMemberNames = null;

	/**  NOT YET DOCUMENTED */
	protected NamespaceRegistry localNamespaces = null;
	/**  NOT YET DOCUMENTED */
	protected NamespaceRegistry instanceNamespaces = null;
	
	protected CompositorMember anyTypeMember = null;


	/**
	 *  Constructor for the Compositor object. Used to construct InlineCompositor
	 *  instances
	 *
	 * @param  parent  NOT YET DOCUMENTED
	 * @param  e       NOT YET DOCUMENTED
	 */
	public Compositor(ComplexType parent, Element e) {
		this.parent = parent;

		SchemaReader schemaReader = parent.getSchemaReader();
		localNamespaces = schemaReader.getNamespaces();
		instanceNamespaces = schemaReader.getInstanceNamespaces();
		element = e;
		init();
	}


	/**
	 *  Constructor for the Compositor object
	 *
	 * @param  parent  NOT YET DOCUMENTED
	 */
	public Compositor(ComplexType parent) {
		this.parent = parent;

		SchemaReader schemaReader = parent.getSchemaReader();
		localNamespaces = schemaReader.getNamespaces();
		instanceNamespaces = schemaReader.getInstanceNamespaces();
		element = parent.getFirstChild();
		init();
	}

	/**  Initialize this compositor with occurance information and instantiate compositor members */
	protected void init() {
		if (element == null) {
			prtln("Compositor got null as element!");
			return;
		}
		minOccurs = SchemaHelper.getMinOccurs(element);
		maxOccurs = SchemaHelper.getMaxOccurs(element);

		// initialize members
		members = new ArrayList();
		for (Iterator i = element.elementIterator(); i.hasNext(); ) {
			Element e = (Element) i.next();
			CompositorMember cm = new CompositorMember (e, this);
			/* WARNING: this.anyTypeMember implies that there can only be ONE anyType member per
			   compositor, but this is NOT THE CASE!
			*/
			if (cm.getCMtype() == CompositorMember.ANY)
				this.anyTypeMember = cm;
			members.add(cm);
		}
	}

	public boolean hasAnyTypeMember () {
		return this.anyTypeMember != null;
	}
	
	public CompositorMember getAnyTypeMember () {
		return this.anyTypeMember;
	}

	/**
	 *  Expands a headElement (a GlobaElement having a substitutionGroup) into the
	 *  instanceQualified names of it's substitutionGroup members.
	 *
	 * @param  headElement  NOT YET DOCUMENTED
	 * @return              The substitionGroupNames value
	 */
	protected List getSubstitionGroupNames(GlobalElement headElement) {
		// prtln ("\ngetSubstitionGroupNames with this globalElement\n" + headElement.toString());
		List names = new ArrayList();
		for (Iterator i = headElement.getSubstitutionGroup().iterator(); i.hasNext(); ) {
			GlobalElement sgm = (GlobalElement) i.next();
			names.add(sgm.getQualifiedInstanceName());
		}
		// unless the headElement is abstract, add its name
		if (!headElement.isAbstract())
			names.add(headElement.getQualifiedInstanceName());
		// prtln ("returning " + names.toString());
		return names;
	}


	/**
	 *  Return a list of CompositoMembers made from the substitionGroup for the
	 *  given headElement.
	 *
	 * @param  headElement  NOT YET DOCUMENTED
	 * @return              The substitionGroupMembers value
	 */
	protected List getSubstitionGroupMembers(GlobalElement headElement) {
		List members = new ArrayList();
		for (Iterator i = headElement.getSubstitutionGroup().iterator(); i.hasNext(); ) {
			GlobalElement sgm = (GlobalElement) i.next();
			members.add(new CompositorMember(sgm));
		}
		// unless the headElement is abstract, add its name
		if (!headElement.isAbstract())
			members.add(new CompositorMember(headElement));
		return members;
	}


	/**
	 *  Recursively traverses the element Compositor's elements, collecting
	 *  CompositorMembers for the leaf elements (those members that can no longer
	 *  be expanded. LeafMembers are important because only they are present in an
	 *  instanceDocument.<p>
	 *
	 *  Members that are expanded include Groups and inline Compositors (Sequence,
	 *  All, Choice).<p>
	 *
	 *  NOTE: Does this algorithm take derivedContentModels into account?? I.e.,
	 *  how are complexContent (extensions) handled??
	 *
	 * @return    The leafMembers value
	 */
	public List getLeafMembers() {

		// only calculate if we haven't done so already
		if (leafMembers == null) {

			leafMembers = new ArrayList();
			if (getType() == Compositor.UNKNOWN) {
				prtln("ERROR: unknown compositor type");
				return leafMembers;
			}
			for (Iterator i = element.elementIterator(); i.hasNext(); ) {
				Element e = (Element) i.next();
				String e_name = e.getName();
				String ref = e.attributeValue("ref", null);
				// prtln ("e_name: " + e_name + "  ref: " + ref);

				if (e_name.equals("element")) {
					if (ref != null) {
						// this is a reference to a GlobalElement - we need to check if it can be expanded
						GlobalDef typeDef = parent.getSchemaReader().getGlobalDef(ref);
						GlobalElement referredElement;
						try {
							referredElement = (GlobalElement) typeDef;
						} catch (Throwable t) {
							prtln("WARNING: referred element was not a GlobalElement (found type: " + typeDef.getType() + ")");
							continue;
						}
						if (referredElement.hasSubstitutionGroup()) {
							leafMembers.addAll(getSubstitionGroupMembers(referredElement));
							continue;
						}
						else {
							SchemaHelper.box("handling an element with ref: " + ref, "Compositor");
							String qualifiedRef = parent.getSchemaReader().getInstanceQualifiedName(ref);
							prtln("\t resolved ref (" + ref + ") -> " + qualifiedRef);
							leafMembers.add(qualifiedRef);
						}
					}
					else {
						leafMembers.add(new CompositorMember(e, this));
					}
				}
				else if (e_name.equals("group")) {
					if (ref == null) {
						prtln("ERROR: group elements must have a reference!");
						continue;
					}
					// now we have to find the typeDef for the referred-to group
					ModelGroup groupDef = null;
					try {
						groupDef = (ModelGroup) parent.getSchemaReader().getGlobalDef(ref);
					} catch (Throwable t) {
						prtln("WARNING: groupDef was not a ModelGroup (found type: " + groupDef.getType() + ")");
						continue;
					}
					leafMembers.addAll(groupDef.getCompositor().getLeafMembers());
				}
				else if (e_name.equals("choice") || e_name.equals("sequence") || e_name.equals("all")) {
					// choices do not have "name" or "ref" attributes. Use a InlineCompositor
					InlineCompositor inlineCompositor = new InlineCompositor(parent, e, this);
					leafMembers.addAll(inlineCompositor.getLeafMembers());
				}
				else {
					prtln("WARNING: " + getName() + " compositor encountered an illegal member: " + e_name);
				}
			}
		}
		return leafMembers;
	}


	/**
	 *  Recursively expand the compositor elements to resolve each into one or more
	 *  membersNames - returning a list collecting all possible member Names that
	 *  this compositor could contain.
	 *
	 * @return    The leafMemberNames value
	 */
	public synchronized List getLeafMemberNames() {

		// only calculate if we haven't done so already
		if (this.leafMemberNames == null) {

			List names = new ArrayList();

			// sanity check for known compositor type
			if (getType() == Compositor.UNKNOWN) {
				prtln("ERROR: unknown compositor type");
				this.leafMemberNames = names;
				return names;
			}

			// expand each compositor element
			for (Iterator i = element.elementIterator(); i.hasNext(); ) {
				Element e = (Element) i.next();
				String e_name = e.getName();
				String ref = e.attributeValue("ref", null);
				String name_attr = e.attributeValue("name", null);
				// prtln ("\t e_name: " + e_name + ", name: " + name_attr + ", ref: " + ref);

				if (e_name.equals("element")) {
					if (ref != null) {
						// this is a reference to a GlobalElement - we need to check if it can be expanded
						GlobalDef typeDef = parent.getSchemaReader().getGlobalDef(ref);
						GlobalElement referredElement;
						try {
							referredElement = (GlobalElement) typeDef;
						} catch (Throwable t) {
							prtln("WARNING: referred element was not a GlobalElement (found type: " + typeDef.getType() + ")");
							continue;
						}
						//
						if (referredElement.hasSubstitutionGroup()) {
							// prtln ("\t hasSubGroup");
							names.addAll(getSubstitionGroupNames(referredElement));
							continue;
						}
						else {
							// prtln ("\t handling an element with ref: " + ref);
							String instanceName = parent.getSchemaReader().getInstanceQualifiedName(ref);
							// prtln ("\t resolved ref (" + ref + ") -> " + instanceName);
							names.add(instanceName);
						}

					}
					else {
						String instanceName =
							parent.getSchemaReader().getInstanceQualifiedName(e.attributeValue("name"));
						names.add(instanceName);
					}
				}
				else if (e_name.equals("group")) {
					if (ref == null) {
						prtln("ERROR: group elements must have a reference!");
						continue;
					}
					// now we have to find the typeDef for the referred-to group
					ModelGroup groupDef = null;
					try {
						groupDef = (ModelGroup) parent.getSchemaReader().getGlobalDef(ref);
					} catch (Throwable t) {
						prtln("WARNING: groupDef was not a ModelGroup (found type: " + groupDef.getType() + ")");
						continue;
					}
					names.addAll(groupDef.getCompositor().getLeafMemberNames());
				}
				else if (e_name.equals("choice") || e_name.equals("sequence") || e_name.equals("all")) {
					// compositors do not have "name" or "ref" attributes. Use a InlineCompositor
					InlineCompositor inline = new InlineCompositor(parent, e, this);
					names.addAll(inline.getLeafMemberNames());
				}
				else {
					prtln("WARNING: " + getName() + " compositor encountered an illegal member: " + e_name);
				}
			}
			this.leafMemberNames = names;
		}
		return this.leafMemberNames;
	}


	/**  NOT YET DOCUMENTED */
	public void printInstanceNames() {
		prtln("\n Compositor Instance Names");
		for (Iterator n = getInstanceNames().iterator(); n.hasNext(); ) {
			prtln("\t" + (String) n.next());
		}
	}


	/**  NOT YET DOCUMENTED */
	public void printLeafMemberNames() {
		prtln("\n Compositor Leaf Member Names");
		for (Iterator n = getLeafMemberNames().iterator(); n.hasNext(); ) {
			prtln("\t" + (String) n.next());
		}
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  compositor  NOT YET DOCUMENTED
	 */
	static void printMemberNames(Compositor compositor) {
		List members = compositor.getMembers();
		prtln(members.size() + " Compositor Members:");
		for (Iterator i = members.iterator(); i.hasNext(); ) {
			CompositorMember member = (CompositorMember) i.next();
			prtln("\t" + member.getInstanceQualifiedName());
		}
	}


	/**
	 *  Returns an integer contant that specifies whether this Compositor is
	 *  Sequence, Choice, All.
	 *
	 * @return    The dataType value
	 */
	public int getType() {
		return Compositor.UNKNOWN;
	}


	/**
	 *  Returns string representation of the Compositor's type (e.g., "compositor",
	 *  "sequence", "all").
	 *
	 * @return    The type value
	 */
	public String getName() {
		int type = getType();
		if (type == Compositor.SEQUENCE)
			return "sequence";
		else if (type == Compositor.CHOICE)
			return "choice";
		else if (type == Compositor.ALL)
			return "all";
		return "???";
	}


	/**
	 *  Returns the ComplexType instance in which this Compositor is defined.
	 *
	 * @return    The location value
	 */
	public final ComplexType getParent() {
		return this.parent;
	}


	/**
	 *  Returns the instanceNames of the child members of this Compositor
	 *
	 * @return    The memberNames value
	 */
	public final List getMemberNames() {
		List names = new ArrayList();
		for (Iterator i = getMembers().iterator(); i.hasNext(); ) {
			CompositorMember member = (CompositorMember) i.next();
			names.add(member.getInstanceQualifiedName());
		}
		return names;
	}


	/**
	 *  InstanceNames are the instance-qualified names of the members that that
	 *  cannot be further split into subCompositors.<p>
	 *
	 *  For headElement members, must we include the substitutionGroupMembers?
	 *
	 * @return    The instanceNames value
	 */
	public final List getInstanceNames() {
		List names = new ArrayList();
		for (Iterator i = getMembers().iterator(); i.hasNext(); ) {
			CompositorMember member = (CompositorMember) i.next();
			if (!member.hasSubCompositor()) {
				String instanceName = member.getInstanceQualifiedName();
				// is this member a headElement??
				GlobalDef typeDef = parent.getSchemaReader().getGlobalDef(instanceName);
				if (typeDef != null &&
					typeDef.isGlobalElement() &&
					((GlobalElement) typeDef).isHeadElement()) {
					Iterator sg = ((GlobalElement) typeDef).getSubstitutionGroup().iterator();
					while (sg.hasNext()) {
						GlobalElement sgm = (GlobalElement) sg.next();
						names.add(sgm.getQualifiedInstanceName());
					}
					if (!((GlobalElement) typeDef).isAbstract())
						names.add(instanceName);
				}
				else {
					names.add(instanceName);
				}
			}
		}
		return names;
	}


	/**
	 *  Gets the element attribute of the Compositor object
	 *
	 * @return    The element value
	 */
	public final Element getElement() {
		return this.element;
	}


	/**
	 *  Gets the indexOfMember attribute of the Compositor object
	 *
	 * @param  cm  NOT YET DOCUMENTED
	 * @return     The indexOfMember value
	 */
	public int getIndexOfMember(CompositorMember cm) {
		for (int i = 0; i < members.size(); i++) {
			if (((CompositorMember) members.get(i)) == cm)
				return i;
		}
		return -1;
	}


	/**
	 *  Gets the indexOfMember attribute of the Compositor object
	 *
	 * @param  memberName  NOT YET DOCUMENTED
	 * @return             The indexOfMember value
	 */
	public int getIndexOfMember(String memberName) {
		prtln("getIndexOfMember() lookingn for " + memberName);
		for (int i = 0; i < members.size(); i++) {
			CompositorMember cm = (CompositorMember) members.get(i);
			prtln(" .. " + cm.getInstanceQualifiedName());
			if (cm.getInstanceQualifiedName().equals(memberName))
				return i;
		}
		return -1;
	}


	/**
	 *  Gets the memberAt attribute of the Compositor object
	 *
	 * @param  index  NOT YET DOCUMENTED
	 * @return        The memberAt value
	 */
	public CompositorMember getMemberAt(int index) {
		if (index > members.size())
			return null;
		return (CompositorMember) members.get(index);
	}


	/**
	 *  Description of the Method
	 *
	 * @return    Description of the Return Value
	 */
	public String toString() {
		String s = "Compositor: " + getName() + " (" + Integer.toHexString(hashCode()) + ")";
		s += "\n\t" + "class: " + getClass().getName();
		s += "\n\t" + "type: " + getType();
		s += "\n\t" + "maxOccurs: " + maxOccurs;
		s += "\n\t" + "minOccurs: " + minOccurs;
		// s += "\n\t" + "element: \n\t" + getElement().asXML();
		s += "\n\t" + "member names:";
		for (Iterator i = getMembers().iterator(); i.hasNext(); ) {
			CompositorMember member = (CompositorMember) i.next();
			s += "\n\t\t -- " + member.getInstanceQualifiedName();
			s += " maxOccurs: " + member.maxOccurs + "  minOccurs: " + member.minOccurs;
		}
		if (this.leafMemberNames != null) {
			s += "\n\t" + "leaf member names:";
			for (Iterator i = this.leafMemberNames.iterator(); i.hasNext(); ) {
				String member = (String) i.next();
				s += "\n\t\t -- " + member;
			}
		}
		else
			s += "\n\t" + "leafMember Names have not yet been computed";
		return s;
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @return    NOT YET DOCUMENTED
	 */
	public String occursInfo() {
		String s = getName();
		String max = (maxOccurs > 200) ? "unbounded" : String.valueOf(maxOccurs);
		String min = String.valueOf(minOccurs);
		return "minOccurs=\"" + min + "\"  maxOccurs=\"" + max + "\"";
	}


	/**
	 *  Returns true if a given instance document element can accept a new member
	 *  according to schema-defined constraints for this compositor.
	 *
	 * @param  instanceDocElement  NOT YET DOCUMENTED
	 * @return                     The simpleType value
	 */
	public abstract boolean acceptsNewMember(Element instanceDocElement);


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  instanceElement  NOT YET DOCUMENTED
	 * @param  memberName       NOT YET DOCUMENTED
	 * @param  memberIndex      NOT YET DOCUMENTED
	 * @return                  NOT YET DOCUMENTED
	 */
	public abstract boolean acceptsNewMember(Element instanceElement, String memberName, int memberIndex);


	/**
	 *  Gets the minOccurs attribute of the Compositor object
	 *
	 * @return    The minOccurs value
	 */
	public final int getMinOccurs() {
		return minOccurs;
	}


	/**
	 *  Gets the maxOccurs attribute of the Compositor object
	 *
	 * @return    The maxOccurs value
	 */
	public final int getMaxOccurs() {
		return maxOccurs;
	}


	/**
	 *  Return the maximum leaf nodes this member could have.<p>
	 *
	 *  NOTE: this might be okay for Sequence and All, but it doesn't work for
	 *  CHOICE compositors, since only one member can contribute PER OCCURRANCE!
	 *
	 * @return    The maxInstanceElements value
	 */
	public int getMaxInstanceElements() {
		if (maxInstanceElements == -1) {
			int max = 0;
			Iterator members = getMembers().iterator();
			while (members.hasNext()) {
				CompositorMember cm = (CompositorMember) members.next();
				int cmMax = cm.getMaxInstanceElements();
				if (cmMax == Integer.MAX_VALUE) {
					max = cmMax;
					break;
				}
				else
					max = max + cmMax;
			}
			maxInstanceElements = max;
		}
		return maxInstanceElements;
	}



	/**
	 *  Returns a list of Member instances - one for each element in the choice
	 *  compositor
	 *
	 * @return    The members value
	 */
	public List getMembers() {
		return members;
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  name  NOT YET DOCUMENTED
	 * @return       NOT YET DOCUMENTED
	 */
	public boolean hasMember(String name) {
		return getMemberNames().contains(name);
	}


	/**
	 *  Finds a particular Member from the members list
	 *
	 * @param  name  NOT YET DOCUMENTED
	 * @return       The member value
	 */
	public CompositorMember getMember(String name) {
		for (Iterator i = members.iterator(); i.hasNext(); ) {
			CompositorMember member = (CompositorMember) i.next();
			if (name.equals(member.getInstanceQualifiedName()))
				return member;
			if (member.getSubstitutionGroupMemberNames().contains(name))
				return member;
		}
		return null;
	}


	/**
	 *  Gets the leafMember attribute of the Compositor object
	 *
	 * @param  name  NOT YET DOCUMENTED
	 * @return       The leafMember value
	 */
	public CompositorMember getLeafMember(String name) {
		for (Iterator i = getLeafMembers().iterator(); i.hasNext(); ) {
			CompositorMember member = (CompositorMember) i.next();
			if (name.equals(member.getInstanceQualifiedName()))
				return member;
		}
		return null;
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  s  NOT YET DOCUMENTED
	 */
	protected static void prtln(String s) {
		if (debug) {
			/* 			while (s.length() > 0 && s.charAt(0) == '\n') {
				System.out.println("");
				s = s.substring(1);
			}
			System.out.println("Compositor: " + s); */
			SchemaUtils.prtln(s, "Compositor");
		}
	}
}

