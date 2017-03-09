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
package org.dlese.dpc.schemedit.standards.asn;
import org.dlese.dpc.schemedit.standards.StandardsNode;
import org.dlese.dpc.standards.asn.AsnStandard;
import org.dlese.dpc.util.strings.FindAndReplace;

import java.util.*;

/**
 *  Wraps an AsnStandard object and implements the StandardsNode interface.
 *
 * @author    ostwald
 * @see       AsnStandard
 */
public class AsnStandardsNode implements StandardsNode {
	private boolean debug = true;

	private Map childMap = new HashMap();
	private List subList = new ArrayList();
	private AsnStandardsNode parent = null;
	private List ancestors = null;
	private AsnStandard std;


	/**
	 *  Constructor for the AsnStandardsNode object given an AsnStandard instance
	 *  and the parent Node.
	 *
	 * @param  std     an asnStandard
	 * @param  parent  this node's parent in the standard tree
	 */
	public AsnStandardsNode(AsnStandard std, AsnStandardsNode parent) {
		this.std = std;
		this.parent = parent;
		this.fullText = this.initFullText(std);
	}


	/**
	 *  Gets the asnStandard attribute of the AsnStandardsNode object
	 *
	 * @return    The asnStandard value
	 */
	public AsnStandard getAsnStandard() {
		return this.std;
	}


	/**
	 *  Gets the gradeRange attribute of the AsnStandardsNode object
	 *
	 * @return    The gradeRange value
	 */
	public String getGradeRange() {
		return std.getGradeRange();
	}


	/**
	 *  Gets the id attribute of the StandardsNode object
	 *
	 * @return    The id value
	 */
	public String getId() {
		return std.getId();
	}


	/**
	 *  Gets the docId attribute of the AsnStandardsNode object
	 *
	 * @return    The docId value
	 */
	public String getDocId() {
		return std.getDocumentIdentifier();
	}


	/**
	 *  Gets the parent attribute of the AsnStandardsNode object
	 *
	 * @return    The parent value
	 */
	public AsnStandardsNode getParent() {
		return parent;
	}


	/**
	 *  Gets the ancestors attribute of the AsnStandardsNode object
	 *
	 * @return    The ancestors value
	 */
	public List getAncestors() {
		if (this.ancestors == null) {
			ancestors = new ArrayList();
			AsnStandardsNode myParent = parent;
			while (myParent != null && myParent.getLevel() > 0) {
				ancestors.add(myParent);
				myParent = myParent.getParent();
			}
		}
		return ancestors;
	}



	/**
	 *  Returns true if this StandardsNode is the last of its siblings
	 *
	 * @return    The isLastInSubList value
	 */
	public boolean isLastInSubList() {
		if (parent == null || parent.getSubList() == null || parent.getSubList().size() == 0) {
			return false;
		}
		return (parent.getSubList().indexOf(this) == parent.getSubList().size() - 1);
	}


	/**
	 *  Gets the isLastInSubList attribute of the AsnStandardsNode object
	 *
	 * @return    The isLastInSubList value
	 */
	public boolean getIsLastInSubList() {
		return isLastInSubList();
	}


	/**
	 *  Gets the noDisplay attribute of the StandardsNode object
	 *
	 * @return    The noDisplay value
	 */
	public boolean getNoDisplay() {
		return false;
	}


	/**
	 *  Gets the child attribute of the AsnStandardsNode object
	 *
	 * @param  id  asnId
	 * @return     The child Node (or null if there is no child with specified id)
	 */
	public AsnStandardsNode getChild(String id) {
		return (AsnStandardsNode) this.childMap.get(id);
	}


	/**
	 *  Gets the specified child of this Node
	 *
	 * @param  id    NOT YET DOCUMENTED
	 * @return       The child value
	 */
	public boolean hasChild(String id) {
		return childMap.containsKey(id);
	}


	/**
	 *  Gets the level attribute of the AsnStandardsNode object
	 *
	 * @return    The level value
	 */
	public int getLevel() {
		return std.getLevel();
	}


	/**
	 *  Gets a list of AsnStandardsNode that have this node as a parent.
	 *
	 * @return    The subList value
	 */
	public List getSubList() {
		return subList;
	}


	/**
	 *  Gets the hasSubList attribute of the AsnStandardsNode object
	 *
	 * @return    The hasSubList value
	 */
	public boolean getHasSubList() {
		return (subList != null && !subList.isEmpty());
	}


	/**
	 *  Gets the isLeafNode attribute of the AsnStandardsNode object
	 *
	 * @return    The isLeafNode value
	 */
	public boolean getIsLeafNode() {
		return !getHasSubList();
	}


	/**
	 *  Adds a childNodet to the AsnStandardsNode
	 *
	 * @param  node  The feature to be added to the SubNode attribute
	 */
	public void addSubNode(StandardsNode node) {
		subList.add(node);
		childMap.put(((AsnStandardsNode) node).getId(), node);
	}


	/**
	 *  Returns null (here only to satisfy the interface)
	 *
	 * @return    The definition value
	 */
	public String getDefinition() {
		return null;
	}


	/**
	 *  Gets the wrap attribute of the StandardsNode object
	 *
	 * @return    The wrap value
	 */
	public boolean getWrap() {
		return false;
	}


	private String label = null;


	/**
	 *  Gets node's itemText with a gradeRange attached to leaf nodes.
	 *
	 * @return    The label value
	 */
	public String getLabel() {
		if (this.label == null) {
			if (this.getLevel() == 1) {
				this.label = this.getItemText();
			}
			else if (this.getIsLeafNode()) {
				this.label = "[" + this.getGradeRange() + "] " + this.getItemText();
			}
			else {
				this.label = this.getItemText() + "  [" + this.getGradeRange() + "]";
			}
		}
		return this.label;
	}


	/**
	 *  The text of the standard as defined by ASN source document.
	 *
	 * @return    The itemText value
	 */
	public String getItemText() {
		return std.getDescription();
	}


	private String fullText = null;


	/**
	 *  Gets the concatenated text of this standard and its ancestors, prepended
	 *  with a gradeRange indicator
	 *
	 * @return    The fullText value
	 */
	public String getFullText() {
		return this.fullText;
	}


	/**
	 *  Initialize the fullText attribute with information from the AsnStandard
	 *
	 * @param  std  Description of the Parameter
	 * @return      Description of the Return Value
	 */
	private String initFullText(AsnStandard std) {
		return "[" + this.getGradeRange() + "] " + std.getDisplayText();
	}


	private List lineage = null;


	/**
	 *  Gets a list containing the text of this node and it's ancestors
	 *
	 * @return    The lineage value
	 */
	public List getLineage() {
		if (this.lineage == null) {
			this.lineage = new ArrayList();
			for (Iterator i = this.getAncestors().iterator(); i.hasNext(); ) {
				AsnStandardsNode node = (AsnStandardsNode) i.next();
				this.lineage.add(0, node.getItemText());
			}
			this.lineage.add(this.getLabel());
		}
		return this.lineage;
	}


	/**  Description of the Method  */
	public void showLineage() {
		for (Iterator i = this.getLineage().iterator(); i.hasNext(); ) {
			prtln((String) i.next());
		}
	}


	/**
	 *  Description of the Method
	 *
	 * @param  s  Description of the Parameter
	 */
	protected void prtln(String s) {
		if (debug) {
			System.out.println("AsnStandardsNode: " + s);
		}
	}


	/**
	 *  Description of the Method
	 *
	 * @return    Description of the Return Value
	 */
	public String toString() {
		String NL = "\n\t";
		String s = "id: " + getId();
		s += NL + "definition: " + getDefinition();
		s += NL + "label: " + getLabel();
		s += NL + "fullText: " + getFullText();
		s += NL + "itemText: " + getItemText();
		s += NL + "isLeafNode: " + getIsLeafNode();
		return s;
	}

}

