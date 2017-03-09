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
import org.dom4j.Namespace;

/**
 *  Represents a *Member* of the Compositor (e.g., All, Sequence, or Choice)
 *  specified in ComplexType definition.<p>
 *
 *  Compositor Members are created at schema-processing time, but used to
 *  provide run-time services. For example, at run time, the compositor is used,
 *  along with the CompositorGuard instance, to determine if a child element may
 *  be added to an existing instance document. In order to serve run-time
 *  purposes, the Compositor members must be accessible via qualified element
 *  names as they appear in the instance document. The instance-level namespace
 *  prefix might be different from that associated with the compositor members
 *  in the schema file in which they were defined.
 *
 *@author     Jonathan Ostwald
 *@created    July 19, 2006
 */
public class CompositorMember {

	public final static int ELEMENT = 0;
	public final static int ELEMENT_REF = 1;
	public final static int GROUP_REF = 2;
	public final static int COMPOSITOR = 3;
	public final static int ANY = 4;
	public final static int UNKNOWN = -1;

	private static boolean debug = false;

	public int minOccurs;
	public int maxOccurs;

	private String name = null;

	private String qualifiedName = null;
	private Element element;
	private String instanceQualifiedName = null;
	private GlobalElement globalElement = null;

	private Compositor parentCompositor;
	private InlineCompositor subCompositor = null;
	private int maxInstanceElements = -1;

	private List substitutionGroupMembers = null;
	private List substitutionGroupMemberNames = null;

	private SchemaReader schemaReader = null;

	private int cmType = UNKNOWN;
	private CompositorGuard parentCompositorGuard = null;


	/**
	 *  Constructor for the Member object
	 *
	 *@param  element     Schema Element representing this member
	 *@param  compositor  the compositor instance of which this is a member
	 */

	public CompositorMember(Element element, Compositor compositor) {
		this.element = element;
		this.parentCompositor = compositor;

		minOccurs = SchemaHelper.getMinOccurs(element);
		maxOccurs = SchemaHelper.getMaxOccurs(element);
		
		String errMsg = "";
		ComplexType parentTypeDef = null;
		try {
			parentTypeDef = compositor.getParent();
			if (parentTypeDef == null) {
				throw new Exception("compositor parent not found");
			}
			schemaReader = compositor.getParent().getSchemaReader();
		} catch (Throwable t) {
			prtln("WARNING: schemaReader could not be found: " + t.getMessage());
		}

		// set the name tp be that of the element if simple,
		// to the compositor if it's complex
		String name_attr = element.attributeValue("name", null);
		String memberElementName = element.getName();
		String ref_attr = element.attributeValue("ref", null);
		
		String memberElementQName = element.getQualifiedName();
		String prefix = NamespaceRegistry.getNamespacePrefix(memberElementQName);
		
		// prefix should always correspond to the schemaNamespace
		if (!prefix.equals(schemaReader.getNamespaces().getSchemaNamespace().getPrefix()))
			prtln ("**** bogus prefix! ****");
		
		if (memberElementName.equals("element")) {
			if (name_attr != null) {
				name = name_attr;
				cmType = ELEMENT;
			} else if (ref_attr != null) {
				name = ref_attr;
				cmType = ELEMENT_REF;
			} else {
				errMsg = "WARNING: cmElement had neither \"name\" or \"ref\" attributes";
				prtln(errMsg + "\n" + element.asXML());
			}

		} else if (memberElementName.equals("any")) {
				name = NamespaceRegistry.makeQualifiedName(
					schemaReader.getNamespaces().getSchemaNamespace(),
					"any");

/* 				// debugging
				prtln ("\n*** this CMType is any (" + name + ")");
				prtln ("element: " + this.element.asXML());
				prtln ("--------\n"); 
*/
				cmType = ANY;
			
		} else if (memberElementName.equals("group")) {
			if (ref_attr != null) {
				name = schemaReader.getInstanceQualifiedName(ref_attr);
				cmType = GROUP_REF;
			} else {
				errMsg = "WARNING: \"group\" cmElement had no \"ref\" attribute";
				prtln(errMsg + "\n" + element.asXML());
			}
		} else if (memberElementName.equals("all") ||
				memberElementName.equals("choice") ||
				memberElementName.equals("sequence")) {
			name = memberElementName;
			cmType = COMPOSITOR;
			subCompositor = new InlineCompositor(parentTypeDef, element, parentCompositor);
		} else {
			prtln("WARNING: CM could not resolve element: " + element.asXML());
		}

	}


	/**
	 *  Construct a CompositorMember given a GlobalElement.<p>
	 *
	 *  This constructor is used to create CompositorMembers from the
	 *  GlobalElements stored as substitutionGroup of a headElement (see
	 *  Compositor.getSubstitionGroupMembers).<p>
	 *
	 *  <p>
	 *
	 *  Global elements may not have occurrance information. The the qualifiedName
	 *  should already be in the top-level namespace context, but for now leave
	 *  getInstanceQualifiedName alone, rather than assign it here as we do with
	 *  qualifiedName.
	 *
	 *@param  globalElement  Description of the Parameter
	 */
	public CompositorMember(GlobalElement globalElement) {
		this.globalElement = globalElement;
		schemaReader = globalElement.getSchemaReader();
		name = globalElement.getName();
		qualifiedName = globalElement.getQualifiedName();
		minOccurs = 1;
		maxOccurs = 1;

		if (globalElement.isHeadElement()) {
			substitutionGroupMembers = globalElement.getSubstitutionGroup();
		}

		prtln("\t element\n" + globalElement.getElement());
	}


	/**
	 *  return the maximum leaf nodes this member could have
	 *
	 *@return    The maxInstanceElements value
	 */
	public int getMaxInstanceElements() {

		if (maxInstanceElements == -1) {
			if (!hasSubCompositor()) {
				maxInstanceElements = this.maxOccurs;
			} else {
				int max = -1;
				Iterator cms = this.getSubCompositor().getMembers().iterator();
				while (cms.hasNext()) {
					CompositorMember child = (CompositorMember) cms.next();
					int childMax = child.getMaxInstanceElements();
					if (childMax == Integer.MAX_VALUE) {
						max = childMax;
						break;
					} else {
						max = max + childMax;
					}
				}
				maxInstanceElements = max;
			}
		}
		return maxInstanceElements;
	}


	/**
	 *  Gets the element attribute of the CompositorMember object
	 *
	 *@return    The element value
	 */
	public Element getElement() {
		return element;
	}


	/**
	 *  Description of the Method
	 *
	 *@return    Description of the Return Value
	 */
	public boolean hasSubCompositor() {
		return (subCompositor != null);
	}


	/**
	 *  If this CompositorMember represents a HeadElement (a globalElement having a
	 *  substitutionGroup, then return the substitutionGroup as a list of
	 *  GlobalElements. Otherwise return an empty List.<p>
	 *
	 *  NOTE: evaluation of substitutionGroupMembers is lazy - it must be performed
	 *  AFTER all the GlobalDefs in the schema have been created, otherwise there
	 *  is a chance that the globalElement, if one exists, will not be found in the
	 *  GlobalDefMap.
	 *
	 *@return    The substitutionGroupMembers value
	 */
	public List getSubstitutionGroupMembers() {
		if (substitutionGroupMembers == null) {
			// prtln ("\n getSubstitutionGroupMembers for " + this.getInstanceQualifiedName());
			substitutionGroupMembers = new ArrayList();
			// obtain globalElement if one exists for this CM
			// If globalElement cannot be found, return emptyList
			if (globalElement == null) {
				// find global element ..

				// prtln ("\n CM about to get globalDef with " + name);
				GlobalDef globalDef = schemaReader.getGlobalDef(name);

				/*
				 *  if (globalDef == null)
				 *  prtln ("\t globalDef not found for " + name);
				 *  else
				 *  prtln ("\t globalDef found\n" + globalDef.toString());
				 */
				if (globalDef != null && globalDef.isGlobalElement()) {
					globalElement = (GlobalElement) globalDef;
				}
			}
			/*
			 *  if (globalElement != null) {
			 *  prtln ("found globalElement\n" + globalElement.toString());
			 *  }
			 */
			if (globalElement != null && globalElement.isHeadElement()) {
				substitutionGroupMembers = globalElement.getSubstitutionGroup();
			}
		}
		return substitutionGroupMembers;
	}


	/**
	 *  Gets the instanceQualifiedNames of the substitutionGroupMembers, if
	 *  this Member is a headElement.
	 *
	 *@return    InstanceQualifiedNames of this Members substitutionGroupMembers
	 */
	public List getSubstitutionGroupMemberNames() {
		// prtln ("getSubstitutionGroupMemberNames for " + this.getInstanceQualifiedName());
		if (substitutionGroupMemberNames == null) {
			substitutionGroupMemberNames = new ArrayList();
			if (isHeadElement()) {
				Iterator sgm = getSubstitutionGroupMembers().iterator();
				while (sgm.hasNext()) {
					GlobalElement ge = (GlobalElement) sgm.next();
					substitutionGroupMemberNames.add(ge.getQualifiedInstanceName());
				}
			}
			// prtln ("\tfound " + substitutionGroupMemberNames.size() + " names");
		}
		return substitutionGroupMemberNames;
	}


	/**
	 *  Is this member a headElement (having a substitutionGroup that
	 * specifies element names that can be subsituted for this member.
	 *
	 *@return    The headElement value
	 */
	public boolean isHeadElement() {
		return (!getSubstitutionGroupMembers().isEmpty());
	}


	/**
	 *  Returns this Member's inline compositor (Choice, Sequence, All) if there is one.
	 *
	 *@return    The subCompositor value
	 */
	public InlineCompositor getSubCompositor() {
		return subCompositor;
	}


	/**
	 *  Gets the parentCompositor attribute of the CompositorMember object
	 *
	 *@return    The parentCompositor value
	 */
	public Compositor getParentCompositor() {
		return parentCompositor;
	}


	/**
	 *  Gets the parentCompositorGuard attribute of the CompositorMember object
	 *
	 *@return    The parentCompositorGuard value
	 */
	public CompositorGuard getParentCompositorGuard() {
		prtln("\n getParentCompositorGuard()");
		if (parentCompositorGuard == null) {

			/*
			 *  prtln ("\t parentCompositorGuard not found");
			 *  prtln ("\t compositorGuard element: " + getElement().asXML());
			 *  / construct parentCompositorGuard - the compositorGuard in the
			 *  / parentCompositor that manages the instanceList elements.
			 */
		}
		return parentCompositorGuard;
	}


	/*
	 *  Get the qualified name of this member as it appears in an instance document, i.e., having the proper
	 *  namespace prefix according to the namespaces as defined at the top level of the schema
	 */
	/**
	 *  Gets the instanceQualifiedName attribute of the CompositorMember object
	 *
	 *@return    The instanceQualifiedName value
	 */
	public String getInstanceQualifiedName() {
		if (instanceQualifiedName == null) {
			instanceQualifiedName = this.schemaReader.getInstanceQualifiedName(name);
		}
		if (instanceQualifiedName == null) {
			prtln("WARNING: CompositorMember.getInstanceQualifiedName() returning null for \"" + name + "\"");
		}
		return instanceQualifiedName;
	}


	/**
	 *  Gets the qualifiedName attribute of the CompositorMember object
	 *
	 *@return    The qualifiedName value
	 */
	public String getQualifiedName() {
		if (qualifiedName == null) {
			if (!NamespaceRegistry.isQualified(name)) {
				Namespace ns = parentCompositor.getParent().getNamespace();
				qualifiedName = NamespaceRegistry.makeQualifiedName(ns, name);
			} else {
				qualifiedName = name;
			}
		}
		return qualifiedName;
	}


	/**
	 *  Gets the label attribute of the CompositorMember object
	 *
	 *@return    The label value
	 */
	public String getLabel() {
		return this.name;
	}

	/**
	 *  Gets the cMtype attribute of the CompositorMember object
	 *
	 *@return    The cMtype value
	 */
	public int getCMtype() {
		return this.cmType;
	}

	public String toString () {
		String s = "Compositor Member";
		String NLT = "\n\t";
		s += NLT + "type: " + this.getCMtype();
		s += NLT + "label: " + getLabel();
		s += NLT + "qualifiedName: " + this.getQualifiedName();
		s += NLT + "instancequalifiedName: " + this.getInstanceQualifiedName();
		s += NLT + "element: " + this.getElement().asXML();
		return s;
	}
	
	/**
	 *  NOT YET DOCUMENTED
	 *
	 *@param  s  NOT YET DOCUMENTED
	 */
	public static void prtln(String s) {
		if (debug) {
			SchemaUtils.prtln(s, "CM");
		}
	}
}


