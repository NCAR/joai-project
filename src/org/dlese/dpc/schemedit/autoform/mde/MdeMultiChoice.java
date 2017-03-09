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
import org.dlese.dpc.schemedit.display.CollapseUtils;
import org.dlese.dpc.xml.*;
import org.dlese.dpc.xml.schema.*;
import org.dlese.dpc.xml.schema.compositor.InlineCompositor;
import org.dlese.dpc.xml.schema.compositor.Choice;
import org.dlese.dpc.util.Files;
import org.dlese.dpc.util.strings.FindAndReplace;

import java.util.*;
import org.dom4j.Node;
import org.dom4j.Attribute;
import org.dom4j.Element;
import org.dom4j.DocumentHelper;

/**
 *  Renders editing fields for a Choice compositor with multiple occurances.
 *
 * @author    ostwald<p>
 *
 *
 */
public class MdeMultiChoice extends MdeChoice {
	/**  Description of the Field */
	private static boolean debug = true;


	/**
	 *  Constructor for the MdeMultiChoice object
	 *
	 * @param  renderer  NOT YET DOCUMENTED
	 */
	public MdeMultiChoice(RendererImpl renderer) {
		super(renderer);
	}


	/**
	 *  Render children elements of a choice compositor. The JSP has to test for
	 *  which of the choices actually exists in the instance document before an
	 *  input field can be displayed for it. So renderChoice creates an iterator
	 *  that will construct the paths to the possible choices, and when one of the
	 *  paths actually exists in the document, then that path is used to construct
	 *  an input element. If there are no children elements, then display
	 *
	 * @param  choiceElement  NOT YET DOCUMENTED
	 */
	public void render(Element choiceElement) {
		// prtln ("Multichoice rendering with xpath = " + xpath);

		/* test for each of the possible choices (i.e., children of the choise compositor),
			do an existence check to see if
		   that choice is present in the document. Then render the node
		   */
		InlineCompositor vc = new InlineCompositor(complexTypeDef, choiceElement);

		List leafMemberNames = vc.getLeafMemberNames();
		for (Iterator i = leafMemberNames.iterator(); i.hasNext(); ) {
			String memberName = (String) i.next();

			// we have to accomodate either element names or references!
			// prtln("\t\t memberName: " + memberName);

			if (sh.getNamespaceEnabled() && !NamespaceRegistry.isQualified(memberName)) {
				String prefix = this.sh.getDefinitionMiner().getNamespaces().getNamedDefaultNamespace().getPrefix();
				memberName = NamespaceRegistry.makeQualifiedName(prefix, memberName);
			}

			String path = xpath + "/" + memberName;
			Element choiceTestElement = rhelper.nodeExistsTest(path);
			parent.add(choiceTestElement);
			newRenderer(path, choiceTestElement).renderRepeatingElement();
		}
		if (isEditMode()) {
			/*
				If the choice node exists but a choice has not been made,
				render the select input by which the user makes a choice.
				This select input is presented as a "required choice", not in the sense that
				the element is schema-required, but more in the sense that we want the user
				to know there is a choice to be made.
				If the choice element is schema-optional, the user can remove it via
				the remove action.
			*/
			// Does the choice node exist?
			Element nodeExistsTest = rhelper.nodeExistsTest(xpath);
			parent.add(nodeExistsTest);

			nodeExistsTest.add(firstChoiceSelect());

			parent.add(getChoiceSiblingController());
		}
		else {
			prtln("mode is NOT edit (" + getMode() + ")");
		}
	}


	/**
	 *  Renders input that is presented when a choice has not yet been made for
	 *  this element.
	 *
	 * @return    jsp element for firstChoiceSelect input
	 */
	Element firstChoiceSelect() {
		// check for an empty choice (does the parent (aka xpath) have no memebers)?
		Element newChoiceTest = DocumentHelper.createElement("logic__empty")
			.addAttribute("name", formBeanName)
			.addAttribute("property", "membersOf(" + xpath + "/*)");

		Element newChoiceDiv = DocumentHelper.createElement("div");
		newChoiceTest.add(newChoiceDiv);

		/*
				Add a hidden element here so that the validator
				will detect non-made choices.
			*/
		Element hidden = newChoiceDiv.addElement("input")
			.addAttribute("type", "hidden")
			.addAttribute("name", "valueOf(" + xpath + ")")
			.addAttribute("value", "");

		Element label = DocumentHelper.createElement("div")
			.addAttribute("class", "field-label");

		Element textElement = label.addElement("div");

		if (this.choiceCompositor.isRequiredChoice()) {
			textElement.addAttribute("class", "required-choice");
			textElement.setText("required choice");
		}
		else {
			textElement.addAttribute("class", "optional-choice");
			textElement.setText("optional choice");
		}

		Element input = getChoiceSelect();

		Element choiceField =
			this.renderer.getRenderedField(xpath, label, input);

		// add label and value elements as a table to newChoiceDiv
		newChoiceDiv.add(choiceField);
		return newChoiceTest;
	}


	/**
	 *  Presents aa controller to create a new choice. This controller is only
	 *  displayed if the element has children. if there are no children then
	 *  "firstChoiceSelect" is displayed
	 *
	 * @return    The choiceSiblingController value
	 */
	Element getChoiceSiblingController() {

		Element hasChildrenTest = rhelper.nodeHasChildrenTest(xpath);

		Element newItemControl = rhelper.acceptsNewChioceTest(xpath);
		hasChildrenTest.add(newItemControl);

		/* jsp to create an id for the "add child controller" that is based on the indexed path
		   (see MdeNode.insertDisplaySetup for pattern)
		   the id is named "controlId" and is used in two places:
		   1 - "add new child" link (which makes a "toggleVisibility" javascript call)
		   2 - the id for the "choice select" element, which is opened by the "add new child" link
		*/
		Element childPath = df.createElement("c__set")
			.addAttribute("var", "childPath")
			.addAttribute("value", xpath);
		newItemControl.add(childPath);

		Element setId = df.createElement("c__set")
			.addAttribute("var", "controlId")
			.addAttribute("value", "${sf__pathToId(childPath)}_add_child_controller");
		newItemControl.add(setId);

		Element actionBox = newItemControl.addElement("div")
			.addAttribute("style", "width:100px;text-align:center");

		Element style = actionBox.addElement("div")
			.addAttribute("class", "action-button");

		Element newItemLink = style.addElement("a")
			.addAttribute("href", "javascript:toggleVisibility(" +
			RendererHelper.jspQuotedString("${controlId}") + ")");
		newItemLink.setText("add new child");
		rhelper.attachToolHelp(newItemLink, "Add a child for " + xpath);

		// wrap prompt and select in div
		Element choiceSelect = getChoiceSelect()
			.addAttribute("id", "${controlId}")
			.addAttribute("style", "display:none;");

		newItemControl.add(choiceSelect);
		return hasChildrenTest;
	}


	/**
	 *  Sets the debug attribute of the MdeMultiChoice class
	 *
	 * @param  verbose  The new debug value
	 */
	public static void setDebug(boolean verbose) {
		debug = verbose;
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  s  NOT YET DOCUMENTED
	 */
	protected void prtln(String s) {
		String prefix = "MdeMultiChoice";
		if (debug) {
			SchemEditUtils.prtln(s, prefix);
		}
	}

}

