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
package org.dlese.dpc.schemedit.vocab.layout;

import java.io.*;
import java.util.*;
import java.text.*;
import java.net.*;

import org.dom4j.*;

import org.dlese.dpc.standards.asn.NameSpaceXMLDocReader;
import org.dlese.dpc.schemedit.SchemEditUtils;
import org.dlese.dpc.xml.Dom4jUtils;

/**
 *  A node in a VocabLayout tree as defined by a vocabLayout (historically
 *  called "groups") file.
 *
 * @author    Jonathan Ostwald
 */
public class LayoutNode {
	private static boolean debug = true;

	VocabLayout vocabLayout = null;
	List subList = null;
	/**  NOT YET DOCUMENTED */
	public Element element;
	String vocab;
	String type;
	String text;
	String deftn;
	boolean wrap;
	boolean collapsible;
	boolean noDisplay;
	boolean isLastInSubList;
	LayoutNode parent;
	int groupLevel;
	int columns = -1;


	/**
	 *  Constructor for the LayoutNode object
	 *
	 * @param  element      element defining this LayoutNode in the vocabLayout
	 *      file
	 * @param  vocabLayout  the vocabLayout instance of which this node is a member
	 */
	public LayoutNode(Element element, VocabLayout vocabLayout) {
		this(element, vocabLayout, null);
	}


	/**
	 *  Constructor for the LayoutNode object containing the parent node.
	 *
	 * @param  element      element defining this LayoutNode in the vocabLayout
	 *      file
	 * @param  vocabLayout  the vocabLayout instance of which this node is a member
	 * @param  parent       the parent LayoutNode instance
	 */
	public LayoutNode(Element element, VocabLayout vocabLayout, LayoutNode parent) {
		this.element = element;
		this.vocabLayout = vocabLayout;
		this.vocab = element.attributeValue("vocab");
		this.type = element.attributeValue("type");
		this.text = element.attributeValue("text");
		this.deftn = element.attributeValue("deftn");
		this.wrap = element.attributeValue("wrap", "").equals("true");
		this.collapsible = element.attributeValue("collapsible", "").equals("true");
		this.noDisplay = element.attributeValue("display", "").equals("false");
		this.parent = parent;
		if (this.parent == null) {
			this.groupLevel = 0;
			this.isLastInSubList = false;
		}
		else {
			this.groupLevel = this.parent.getGroupLevel() + 1;
			if (parent.element.elements().get(parent.element.elements().size() - 1) == this.element) {
				isLastInSubList = true;
			}
		}
		// prtln (this.text + " - " + this.groupLevel);
		this.subList = this.getSubList();
	}


	/**
	 *  Gets the parent (LayoutNode) of the LayoutNode object
	 *
	 * @return    The parent value
	 */
	public LayoutNode getParent() {
		return this.parent;
	}


	/**
	 *  Returns the vocab attribute value.
	 *
	 * @return    The name value
	 */
	public String getName() {
		return this.vocab;
	}


	/**
	 *  Returns the text attribute value if present, and the "name" attribute
	 *  valueotherwise.
	 *
	 * @return    The label value
	 */
	public String getLabel() {
		if (this.text != null && this.text.trim().length() > 0)
			return this.text;
		return this.getName();
	}


	/**
	 *  Gets the definition attribute value of the LayoutNode object
	 *
	 * @return    The definition value
	 */
	public String getDefinition() {
		return this.deftn;
	}


	/**
	 *  Gets the wrap attribute of the LayoutNode object, used to create a new
	 *  column in the display.
	 *
	 * @return    The wrap value
	 */
	public boolean getWrap() {
		return (this.wrap);
	}


	/**
	 *  Gets the noDisplay attribute of the LayoutNode object, suppresses display of this LayoutNode.
	 *
	 * @return    The noDisplay value
	 */
	public boolean getNoDisplay() {
		return (this.noDisplay);
	}


	/**
	 *  Gets the type attribute of the LayoutNode object
	 *
	 * @return    The type value
	 */
	public String getType() {
		return this.type;
	}


	/**
	 *  Gets the collapsible attribute of the LayoutNode object
	 *
	 * @return    The collapsible value
	 */
	public boolean getCollapsible() {
		return (this.collapsible);
	}


	/**
	 *  Gets the subList attribute of the LayoutNode object
	 *
	 * @return    The subList value
	 */
	public List getSubList() {
		if (subList == null) {
			subList = new ArrayList();
			for (Iterator i = this.element.elementIterator(); i.hasNext(); ) {
				subList.add(new LayoutNode((Element) i.next(), this.vocabLayout, this));
			}
		}
		return subList;
	}


	/**
	 *  Does this LayoutNode have children
	 *
	 * @return    true if this node has children
	 */
	public boolean hasSubList() {
		return (!this.getSubList().isEmpty());
	}


	/**
	 *  Gets the hasSubList attribute of the LayoutNode object
	 *
	 * @return    The hasSubList value
	 */
	public boolean getHasSubList() {
		return this.hasSubList();
	}


	/**
	 *  Returns true if this LayoutNode is the last in its list.
	 *
	 * @return    The isLastInSubList value
	 */
	public boolean getIsLastInSubList() {
		return this.isLastInSubList;
	}


	/**
	 *  Gets the sibling LayoutNode (including this one) of this LayoutNode
	 *  instance.
	 *
	 * @return    The siblings value
	 */
	private List getSiblings() {
		if (this.parent == null)
			return this.vocabLayout.getLayoutNodes();
		else
			return this.parent.getSubList();
	}


	/**
	 *  Gets the number of columns required to render the sublist of which this
	 *  LayoutNode is a member.
	 *
	 * @return    The numColumns value
	 */
	public int getNumColumns() {
		if (this.columns == -1) {
			this.columns = 1;
			for (Iterator i = this.getSiblings().iterator(); i.hasNext(); ) {
				LayoutNode node = (LayoutNode) i.next();
				if (!node.noDisplay && node.getWrap())
					this.columns++;
			}
		}
		return this.columns;
	}


	/**
	 *  Gets the groupLevel attribute of the LayoutNode object, which specifies the depth of this
	 LayoutNode within the VocabLayout tree.
	 *
	 * @return    The groupLevel value
	 */
	public int getGroupLevel() {
		return this.groupLevel;
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  num  NOT YET DOCUMENTED
	 * @return      NOT YET DOCUMENTED
	 */
	public String spacer(int num) {
		String s = "";
		for (int i = 0; i < num; i++)
			s += "\t";
		return s;
	}


	/**  NOT YET DOCUMENTED */
	public void report() {
		String NL = "\n" + spacer(this.getGroupLevel());
		String T = "\t" + spacer(this.getGroupLevel());
		prtln(NL + "name: " + this.getName());
		prtln(T + "label: " + this.getLabel());
		prtln(T + "deftn: " + this.getDefinition());
		prtln(T + "wrap: " + this.getWrap());
		prtln(T + "isLastInSubList: " + this.getIsLastInSubList());
		prtln(T + "collapsible: " + this.getCollapsible());
		if (this.hasSubList()) {
			prtln(T + "subList: " + this.getSubList().size());
			for (Iterator i = this.getSubList().iterator(); i.hasNext(); ) {
				LayoutNode sub = (LayoutNode) i.next();
				sub.report();
			}
		}
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  node  NOT YET DOCUMENTED
	 */
	private static void pp(Node node) {
		prtln(Dom4jUtils.prettyPrint(node));
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  s  NOT YET DOCUMENTED
	 */
	private static void prtln(String s) {
		if (debug) {
			// SchemEditUtils.prtln(s, "LayoutNode");
			SchemEditUtils.prtln(s, "");
		}
	}
}

