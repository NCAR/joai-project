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
package org.dlese.dpc.schemedit.standards.td;
import org.dlese.dpc.schemedit.standards.StandardsNode;
import org.dlese.dpc.standards.commcore.Standard;
import org.dlese.dpc.util.strings.FindAndReplace;

import java.util.*;

/**
 *  Implements the StandardsNode interface using information from a Commcore
 *  instance.
 *
 *@author    ostwald
 *@see       Standard
 */
public class TeachersDomainStandardsNode implements StandardsNode {
	private boolean debug = true;

	private Map childMap = new HashMap();
	private List subList = new ArrayList();
	private String itemText = null;


	/**
	 *  Constructor for the TeachersDomainStandardsNode object given a ResultDoc instance
	 *
	 *@param  std     NOT YET DOCUMENTED
	 *@param  parent  Description of the Parameter
	 */
	public TeachersDomainStandardsNode(Standard std, TeachersDomainStandardsNode parent) {
		this.parent = parent;
		this.gradeRange = std.getGradeRange();
		this.id = std.getId();
		this.itemText = std.getItemText();
		this.level = std.getLevel();
		this.fullText = this.initFullText(std);
		this.docId = std.getDocumentIdentifier();
	}


	private String gradeRange = null;


	/**
	 *  Gets the gradeRange attribute of the TeachersDomainStandardsNode object
	 *
	 *@return    The gradeRange value
	 */
	public String getGradeRange() {
		return this.gradeRange;
	}


	private String id = null;


	/**
	 *  Gets the id attribute of the StandardsNode object
	 *
	 *@return    The id value
	 */
	public String getId() {
		return this.id;
	}


	private String docId;


	/**
	 *  Gets the docId attribute of the TeachersDomainStandardsNode object
	 *
	 *@return    The docId value
	 */
	public String getDocId() {
		return this.docId;
	}


	private TeachersDomainStandardsNode parent = null;


	/**
	 *  Gets the parent attribute of the TeachersDomainStandardsNode object
	 *
	 *@return    The parent value
	 */
	public TeachersDomainStandardsNode getParent() {
		return parent;
	}


	private List ancestors = null;


	/**
	 *  Gets the ancestors attribute of the TeachersDomainStandardsNode object
	 *
	 *@return    The ancestors value
	 */
	public List getAncestors() {
		if (this.ancestors == null) {
			ancestors = new ArrayList();
			TeachersDomainStandardsNode myParent = parent;
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
	 *@return    The isLastInSubList value
	 */
	public boolean isLastInSubList() {
		if (parent == null || parent.getSubList() == null || parent.getSubList().size() == 0) {
			return false;
		}
		return (parent.getSubList().indexOf(this) == parent.getSubList().size() - 1);
	}


	/**
	 *  Gets the isLastInSubList attribute of the TeachersDomainStandardsNode object
	 *
	 *@return    The isLastInSubList value
	 */
	public boolean getIsLastInSubList() {
		return isLastInSubList();
	}


	/**
	 *  Gets the noDisplay attribute of the StandardsNode object
	 *
	 *@return    The noDisplay value
	 */
	public boolean getNoDisplay() {
		return false;
	}


	/**
	 *  Gets the child attribute of the TeachersDomainStandardsNode object
	 *
	 *@param  id  asnId
	 *@return     The child Node (or null if there is no child with specified id)
	 */
	public TeachersDomainStandardsNode getChild(String id) {
		return (TeachersDomainStandardsNode) this.childMap.get(id);
	}


	/**
	 *  Gets the specified child of this Node
	 *
	 *@param  name  asnId
	 *@return       The child value
	 */
	public boolean hasChild(String id) {
		return childMap.containsKey(id);
	}


	private int level = -1;


	/**
	 *  Gets the level attribute of the TeachersDomainStandardsNode object
	 *
	 *@return    The level value
	 */
	public int getLevel() {
		return this.level;
	}


	/**
	 *  Gets a list of TeachersDomainStandardsNode that have this node as a parent.
	 *
	 *@return    The subList value
	 */
	public List getSubList() {
		return subList;
	}


	/**
	 *  Gets the hasSubList attribute of the TeachersDomainStandardsNode object
	 *
	 *@return    The hasSubList value
	 */
	public boolean getHasSubList() {
		return (subList != null && !subList.isEmpty());
	}


	/**
	 *  Gets the isLeafNode attribute of the TeachersDomainStandardsNode object
	 *
	 *@return    The isLeafNode value
	 */
	public boolean getIsLeafNode() {
		return !getHasSubList();
	}


	/**
	 *  Adds a childNodet to the TeachersDomainStandardsNode
	 *
	 *@param  node  The feature to be added to the SubNode attribute
	 */
	public void addSubNode(StandardsNode node) {
		subList.add(node);
		childMap.put(((TeachersDomainStandardsNode) node).getId(), node);
	}


	/**
	 *  Returns null (here only to satisfy the interface)
	 *
	 *@return    The definition value
	 */
	public String getDefinition() {
		return null;
	}


	/**
	 *  Gets the wrap attribute of the StandardsNode object
	 *
	 *@return    The wrap value
	 */
	public boolean getWrap() {
		return false;
	}


	private String label = null;


	/**
	 *  Gets node's itemText with a gradeRange attached to leaf nodes.
	 *
	 *@return    The label value
	 */
	public String getLabel() {
		// just return item text until we get the grade levels straighted out for comm_core
		return this.itemText;
/* 		if (this.label == null) {
			if (this.getLevel() == 1) {
				this.label = 	this.itemText;
			} else if (this.getIsLeafNode()) {
				this.label = "[" + this.getGradeRange() + "] " + this.itemText;
			} else {
				this.label = this.itemText + "  [" + this.getGradeRange() + "]";
			}
		}
		return this.label; */
	}


	/**
	 *  The text of the standard as defined by ASN source document.
	 *
	 *@return    The itemText value
	 */
	public String getItemText() {
		return this.itemText;
	}


	private String fullText = null;


	/**
	 *  Gets the concatenated text of this standard and its ancestors, prepended with a gradeRange indicator
	 *
	 *@return    The fullText value
	 */
	public String getFullText() {
		return this.fullText;
	}


	/**
	 *  Initialize the fullText attribute with information from the Standard
	 *
	 *@param  std  Description of the Parameter
	 *@return      Description of the Return Value
	 */
	private String initFullText(Standard std) {
		return "[" + this.getGradeRange() + "] " + std.getDisplayText();
	}


	private List lineage = null;


	/**
	 *  Gets a list containing the text of this node and it's ancestors
	 *
	 *@return    The lineage value
	 */
	public List getLineage() {
		if (this.lineage == null) {
			this.lineage = new ArrayList();
			for (Iterator i = this.getAncestors().iterator(); i.hasNext(); ) {
				TeachersDomainStandardsNode node = (TeachersDomainStandardsNode) i.next();
				this.lineage.add(0, node.getItemText());
			}
			this.lineage.add(this.getLabel());
		}
		return this.lineage;
	}


	/**
	 *  Description of the Method
	 */
	public void showLineage() {
		for (Iterator i = this.getLineage().iterator(); i.hasNext(); ) {
			prtln((String) i.next());
		}
	}


	/**
	 *  Description of the Method
	 *
	 *@param  s  Description of the Parameter
	 */
	protected void prtln(String s) {
		if (debug) {
			System.out.println("ccStandardsNode: " + s);
		}
	}


	/**
	 *  Description of the Method
	 *
	 *@return    Description of the Return Value
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

