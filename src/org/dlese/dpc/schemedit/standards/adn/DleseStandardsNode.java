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
package org.dlese.dpc.schemedit.standards.adn;
import org.dlese.dpc.schemedit.standards.StandardsNode;

import java.util.*;

/**
 *  Node in a hierarchical structure of content standards, determined by the
 *  colon-separated reperesentation of individual standards.
 *
 * @author    ostwald<p>
 *
 */
public class DleseStandardsNode implements StandardsNode {
	private boolean debug = true;
	private String text = "";
	private int level = 0;
	private String name;
	private List subList = new ArrayList();
	private Map nameMap = new HashMap();
	private boolean isLastInSubList = false;
	private DleseStandardsNode parent = null;
	private String fullText;
	private String id;


	/**
	 *  Constructor for the DleseStandardsNode object given a ResultDoc instance
	 *
	 * @param  name    NOT YET DOCUMENTED
	 * @param  parent  NOT YET DOCUMENTED
	 * @param  id      NOT YET DOCUMENTED
	 */
	public DleseStandardsNode(String name, DleseStandardsNode parent, String id) {
		this.id = id;
		this.name = name;
		this.parent = parent;
		if (parent == null)
			level = 0;
		else
			level = parent.getLevel() + 1;
		fullText = "";
	}


	/**
	 *  Gets the matchKey attribute of the DleseStandardsNode object
	 *
	 * @return    The matchKey value
	 */
	public String getMatchKey() {
		return getName().toLowerCase();
	}


	/**
	 *  Gets the fullText attribute of the DleseStandardsNode object
	 *
	 * @return    The fullText value
	 */
	public String getFullText() {
		return fullText;
	}


	/**
	 *  Returns fullText attribute - used for label/value objects in jsp
	 *
	 * @return    The value value
	 */
	public String getValue() {
		return fullText;
	}


	/**
	 *  Gets the id attribute of the DleseStandardsNode object
	 *
	 * @return    The id value
	 */
	public String getId() {
		return id;
	}


	/**
	 *  GradeRange for this standard (the first field of the ":"-delimited id)
	 *
	 * @return    The gradeRange value
	 */
	public String getGradeRange() {
		String[] splits = this.getId().split(":");
		return splits[1];
	}


	/**
	 *  Gets the parent attribute of the DleseStandardsNode object
	 *
	 * @return    The parent value
	 */
	public DleseStandardsNode getParent() {
		return parent;
	}


	/**
	 *  Gets the ancestors attribute of the DleseStandardsNode object
	 *
	 * @return    The ancestors value
	 */
	public List getAncestors() {
		List ancestors = new ArrayList();
		DleseStandardsNode myParent = parent;
		while (myParent != null && myParent.getLevel() > 0) {
			ancestors.add(myParent);
			myParent = myParent.getParent();
		}
		return ancestors;
	}


	/**
	 *  Sets the fullText attribute of the DleseStandardsNode object
	 *
	 * @param  s  The new fullText value
	 */
	public void setFullText(String s) {
		fullText = s;
	}


	/**
	 *  Gets the lastInSubList attribute of the DleseStandardsNode object
	 *
	 * @return    The lastInSubList value
	 */
	public boolean isLastInSubList() {
		if (parent == null || parent.getSubList() == null || parent.getSubList().size() == 0)
			return false;
		return (parent.getSubList().indexOf(this) == parent.getSubList().size() - 1);
	}


	/**
	 *  Gets the isLastInSubList attribute of the DleseStandardsNode object
	 *
	 * @return    The isLastInSubList value
	 */
	public boolean getIsLastInSubList() {
		return isLastInSubList();
	}


	/**
	 *  Sets the isLastInSubList attribute of the DleseStandardsNode object
	 *
	 * @param  isLast  The new isLastInSubList value
	 */
	public void setIsLastInSubList(boolean isLast) {
		isLastInSubList = isLast;
	}


	/**
	 *  Gets the name attribute of the DleseStandardsNode object
	 *
	 * @return    The name value
	 */
	public String getName() {
		return name;
	}


	/**
	 *  Gets the noDisplay attribute of the DleseStandardsNode object
	 *
	 * @return    The noDisplay value
	 */
	public boolean getNoDisplay() {
		return false;
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  name  NOT YET DOCUMENTED
	 * @return       NOT YET DOCUMENTED
	 */
	public boolean hasChild(String name) {
		return nameMap.containsKey(name);
	}


	/**
	 *  Gets the child attribute of the DleseStandardsNode object
	 *
	 * @param  name  NOT YET DOCUMENTED
	 * @return       The child value
	 */
	public DleseStandardsNode getChild(String name) {
		return (DleseStandardsNode) nameMap.get(name);
	}


	/**
	 *  Sets the name attribute of the DleseStandardsNode object
	 *
	 * @param  name  The new name value
	 */
	public void setName(String name) {
		this.name = name;
	}


	/**
	 *  Gets the level attribute of the DleseStandardsNode object
	 *
	 * @return    The level value
	 */
	public int getLevel() {
		return level;
	}


	/**
	 *  Sets the level attribute of the DleseStandardsNode object
	 *
	 * @param  level  The new level value
	 */
	public void setLevel(int level) {
		this.level = level;
	}


	/**
	 *  Gets the subList attribute of the DleseStandardsNode object
	 *
	 * @return    The subList value
	 */
	public List getSubList() {
		return subList;
	}


	/**
	 *  Sets the subList attribute of the DleseStandardsNode object
	 *
	 * @param  list  The new subList value
	 */
	public void setSubList(List list) {
		subList = list;
		nameMap = new HashMap();
		for (Iterator i = subList.iterator(); i.hasNext(); ) {
			DleseStandardsNode node = (DleseStandardsNode) i.next();
			nameMap.put(node.getName(), node);
		}
	}


	/**
	 *  Gets the hasSubList attribute of the DleseStandardsNode object
	 *
	 * @return    The hasSubList value
	 */
	public boolean getHasSubList() {
		return (subList != null && subList.size() > 0);
	}


	/**
	 *  Gets the isLeafNode attribute of the DleseStandardsNode object
	 *
	 * @return    The isLeafNode value
	 */
	public boolean getIsLeafNode() {
		return !getHasSubList();
	}


	/**
	 *  Adds a feature to the SubNode attribute of the DleseStandardsNode object
	 *
	 * @param  node  The feature to be added to the SubNode attribute
	 */
	public void addSubNode(StandardsNode node) {
		subList.add(node);
		nameMap.put(((DleseStandardsNode) node).getName(), node);
	}


	/**
	 *  Gets the definition attribute of the DleseStandardsNode object
	 *
	 * @return    The definition value
	 */
	public String getDefinition() {
		return null;
	}


	/**
	 *  Gets the wrap attribute of the DleseStandardsNode object
	 *
	 * @return    The wrap value
	 */
	public boolean getWrap() {
		return false;
	}


	/**
	 *  Returns name attribute - used for label/value objects in jsp.
	 *
	 * @return    The label value
	 */
	public String getLabel() {
		return name;
	}


	/**
	 *  Description of the Method
	 *
	 * @param  s  Description of the Parameter
	 */
	protected void prtln(String s) {
		if (debug) {
			System.out.println("DleseStandardsNode: " + s);
		}
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @return    NOT YET DOCUMENTED
	 */
	public String toString() {
		String NL = "\n\t";
		String s = "id: " + getId();
		s += NL + "value: " + getValue();
		s += NL + "definition: " + getDefinition();
		s += NL + "label: " + getLabel();
		return s;
	}
}

