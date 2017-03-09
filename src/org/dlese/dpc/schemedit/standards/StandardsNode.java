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
package org.dlese.dpc.schemedit.standards;

import java.util.*;

/**
 *  Node in a hierarchical structure of content standards, used to display
 *  standards using mui-oriented commands and therefore modeled after the {@link
 *  org.dlese.dpc.vocab.VocabNode} interface.
 *
 * @author    ostwald<p>
 *
 *      $Id $
 */
public interface StandardsNode {


	/**
	 *  Gets the id attribute of the StandardsNode object
	 *
	 * @return    The id value
	 */
	public String getId();


	/**
	 *  Gets a list of ancestors as StandardsNodes from this to stdDocument root;
	 *
	 * @return    The ancestors value
	 */
	public List getAncestors();


	/**
	 *  Gets the isLastInSubList attribute of the StandardsNode object
	 *
	 * @return    The isLastInSubList value
	 */
	public boolean isLastInSubList();


	/**
	 *  Gets the noDisplay attribute of the StandardsNode object
	 *
	 * @return    The noDisplay value
	 */
	public boolean getNoDisplay();


	/**
	 *  Gets the specified child of this Node
	 *
	 * @param  childId  NOT YET DOCUMENTED
	 * @return          The child value
	 */
	public StandardsNode getChild(String childId);


	/**
	 *  Gets the level attribute of the StandardsNode object
	 *
	 * @return    The level value
	 */
	public int getLevel();


	/**
	 *  Gets a list of StandardsNodes that have this node as a parent.
	 *
	 * @return    The subList value
	 */
	public List getSubList();


	/**
	 *  Gets the gradeRange attribute of the StandardsNode object
	 *
	 * @return    String of form "K-4"
	 */
	public String getGradeRange();


	/**
	 *  Gets the hasSubList attribute of the StandardsNode object
	 *
	 * @return    The hasSubList value
	 */
	public boolean getHasSubList();


	/**
	 *  Gets the isLeafNode attribute of the StandardsNode object
	 *
	 * @return    The isLeafNode value
	 */
	public boolean getIsLeafNode();


	/**
	 *  Adds a feature to the SubNode attribute of the StandardsNode object
	 *
	 * @param  node  The feature to be added to the SubNode attribute
	 */
	public void addSubNode(StandardsNode node);


	/**
	 *  A definition of the vocab choice (defined for vocabNode, not clear how
	 *  relevant for standardsNode). If non-null, used as mouse-over title in JSP.
	 *
	 * @return    The definition value
	 */
	public String getDefinition();


	/**
	 *  Gets the wrap attribute of the StandardsNode object
	 *
	 * @return    The wrap value
	 */
	public boolean getWrap();


	/**
	 *  Gets the label attribute of the StandardsNode object
	 *
	 * @return    The label value
	 */
	public String getLabel();


	/**
	 *  Gets the fullText attribute of the StandardsNode object
	 *
	 * @return    The fullText value
	 */
	public String getFullText();

}

