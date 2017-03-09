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
package org.dlese.dpc.schemedit.autoform.mde;

import org.dlese.dpc.schemedit.autoform.*;
import org.dlese.dpc.schemedit.SchemEditUtils;
import org.dlese.dpc.xml.XPathUtils;

import org.dom4j.Node;
import org.dom4j.Element;
import org.dom4j.DocumentHelper;

/**
 *  Abstract base class for renderning MetadataEditor fields for a repeating
 *  node. Principle contribution is getRepeatingContentBox method, which builds
 *  a box (a "div" element) into which each repetition of the repeating nodes is
 *  placed (by concrete classes).
 *
 * @author    ostwald<p>
 *
 *
 */
public abstract class MdeRepeatingNode extends MdeNode {
	/**  Description of the Field */
	private static boolean debug = true;


	/**
	 *  Constructor for the MdeRepeatingNode object
	 *
	 * @param  renderer  the Renderer instance for this node
	 */
	public MdeRepeatingNode(RendererImpl renderer) {
		super(renderer);
	}


	/**  Render method is supplied by concrete classes */
	public abstract void render();


	/**
	 *  Create a "box" for the contents of each repitition of this node, which is
	 *  filled by the subclasses which build upon this method.
	 *
	 * @param  itemPath  xpath with indexing to support iteration
	 * @return           The repeatingContentBox value
	 */
	protected Element getRepeatingContentBox(String itemPath) {
		Element repeatingContent = DocumentHelper.createElement("div");

		/* 	the following works because "itemPath" has been defined in the jsp
			(see getIteration) prior to the repeating repeatingContent
		*/
		repeatingContent.addAttribute("id", "${sf__pathToId(itemPath)}_box");

		return repeatingContent;
	}


	/**
	 *  Creates a repeatIteration element and attaches it to the parent. The
	 *  iteration element takes care of assigning an indexed path (i.e.,
	 *  *itemPath*) to the repeating items. The iteration construct is used as a
	 *  framework to render the repeating elements (siblings) and their contents.
	 *  <p>
	 *
	 *  jsp example:<code>
	 *  <logic:iterate indexId="index" id="item" name="sef"
	 *       property="repeatingMembersOf(/itemRecord/general/keywords/keyword)">
	 *  <c:set var="itemPath" scope="page" value="/itemRecord/general/keywords/keyword_${keword+1}" />
	 *  </code>
	 *
	 * @param  itemPath     path to individual nodes (including indexing)
	 * @param  siblingPath  The path shared by each of the repeating nodes
	 * @param  indexId      The symbol used to create the index in jsp that
	 *      differentiates the repeating nodes
	 * @return              A jsp iteration construct in which the repeating nodes
	 *      are rendered.
	 */
	protected Element getIteration(String itemPath, String siblingPath, String indexId) {
		Element iteration = DocumentHelper.createElement("logic__iterate")
			.addAttribute("indexId", indexId)
			.addAttribute("id", "item")
			.addAttribute("name", formBeanName)
			.addAttribute("property", "repeatingMembersOf(" + siblingPath + ")");

		Element itemPathAssignElement = iteration.addElement("c__set")
			.addAttribute("var", "itemPath")
			.addAttribute("scope", "page")
			.addAttribute("value", itemPath);
		return iteration;
	}



	/**
	 *  Creates JSP to set the *id* for a particular node, and then to determine
	 *  the display_state of that node from the collapseBean and assign it to a
	 *  javascript variable. These elements all attached to provided baseDiv.
	 *
	 * @param  baseDiv  Element to which created elements are attached.
	 */
	protected void insertRepeatingDisplaySetup(Element baseDiv) {
		// prtln ("insertRepeatingDisplaySetup()\n\t" + xpath);

		// set jsp id to correspond to this node's path (with indexing)
		Element setId = DocumentHelper.createElement("c__set")
			.addAttribute("var", "id")
			.addAttribute("value", "${sf__pathToId(itemPath)}");
		baseDiv.add(setId);

		if (isDisplayMode()) {
			return;
		}

		// initialize collapseBean so we can read the displaystate for this node
		Element setProp = DocumentHelper.createElement("jsp__setProperty")
			.addAttribute("name", "collapseBean")
			.addAttribute("property", "id")
			.addAttribute("value", "${id}");
		baseDiv.add(setProp);

		// create hidden javascript variable to reflect display state from collapseBean
		Element hiddenVar = DocumentHelper.createElement("input")
			.addAttribute("type", "hidden")
			.addAttribute("name", "${id}_displayState")
			.addAttribute("id", "${id}_displayState")
			.addAttribute("value", "${" + formBeanName + ".collapseBean.displayState}");
		baseDiv.add(hiddenVar);
	}


	/**
	 *  Create new item controller that goes at the bottom of a repeating node to
	 *  allow user to create a new element (sibling). <p>
	 *
	 *  This controller is not displayed in the case when the parent element is
	 *  empty. In that case, the new item control is displayed with the parent
	 *  element.
	 *
	 * @return    Control element to add a new sibling to this repeating node.
	 */
	public Element newSiblingController() {

		String siblingPath = XPathUtils.getSiblingXPath(xpath);
		String childName = XPathUtils.getNodeName(siblingPath);
		String indexId = RendererHelper.encodeIndexId(siblingPath);
		String itemPath = siblingPath + "_${" + indexId + "+1}_";

		Element controllerBox = DocumentHelper.createElement("div");
		// new sibling control only displayed when there are already members (i.e., siblings!)
		Element hasMembersTest = rhelper.nodeHasMembersTest(itemPath);
		controllerBox.add(hasMembersTest);

		Element newItemControl = rhelper.acceptsNewSiblingTest(itemPath);

		Element actionBox = newItemControl.addElement("div")
			.addAttribute("style", "width:100px;text-align:center");

		Element style = actionBox.addElement("div")
			.addAttribute("class", "action-button");
		Element newItemLink = style.addElement("a")
			.addAttribute("href", "javascript:doNewElement(" + rhelper.jspQuotedString(itemPath) + ")");
		rhelper.attachToolHelp(newItemLink, "Create a new " + childName);
		newItemLink.setText("add " + childName);

		if (newItemControl != null)
			hasMembersTest.add(newItemControl);

		return controllerBox;
	}


	/**
	 *  Render jsp element for adding a new repeating element that will be shown
	 *  when there are no existing values for this field in the instance document.
	 *
	 * @return    NOT YET DOCUMENTED
	 */
	protected Element emptyRepeatingElement() {

		Element emptyRepeat = rhelper.nodeHasNoMembersTest(xpath);

		String nodeName = XPathUtils.getLeaf(xpath);
		prtln("emptyRepeatingElement() for " + nodeName);

		// build control element
		Element controlElement = df.createElement("div");

		Element newItemAction = controlElement.addElement("div")
			.addAttribute("class", "action-button");
		Element newItemLink = newItemAction.addElement("a")
			.addAttribute("href", "javascript:doNewElement(" + RendererHelper.jspQuotedString(xpath) + ")");
		rhelper.attachToolHelp(newItemLink, "Create a new " + nodeName);
		newItemLink.setText("add " + nodeName);

		prtln("calling renderer.getSimpleTypeLabel(xpath)");
		Label label = renderer.getSimpleTypeLabel(xpath);
		if (!this.schemaNode.isRequired())
			((SimpleTypeLabel)label).setOptional();
		emptyRepeat.add(getRenderedNoInputField(label, controlElement));

		return emptyRepeat;
	}


	/**
	 *  Sets the debug attribute of the MdeRepeatingNode class
	 *
	 * @param  bool  The new debug value
	 */
	public static void setDebug(boolean bool) {
		debug = bool;
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  s  NOT YET DOCUMENTED
	 */
	protected void prtln(String s) {
		String prefix = "MdeRepeatingNode";
		if (debug) {
			SchemEditUtils.prtln(s, prefix);
		}
	}

}

