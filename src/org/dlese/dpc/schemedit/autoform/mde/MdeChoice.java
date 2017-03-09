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
import org.dlese.dpc.xml.*;
import org.dlese.dpc.xml.schema.*;
import org.dlese.dpc.xml.schema.compositor.InlineCompositor;
import org.dlese.dpc.xml.schema.compositor.Choice;

import java.util.*;

import org.dom4j.Node;
import org.dom4j.Attribute;
import org.dom4j.Element;

/**
 *  Render a simple Choice compositor.
 *
 * @author    ostwald
 *
 *
 */
public class MdeChoice extends MdeComplexType {
	/**  Description of the Field */
	private static boolean debug = false;
	protected Choice choiceCompositor = null;


	/**
	 *  Constructor for the MdeChoice object
	 *
	 * @param  renderer  NOT YET DOCUMENTED
	 */
	public MdeChoice(RendererImpl renderer) {
		super(renderer);
		this.choiceCompositor = (Choice) this.complexTypeDef.getCompositor();
	}


	/**
	 *  Render children elements of a choice compositor. The JSP has to test for
	 *  which of the choices actually exists in the instance document before an
	 *  input can be displayed for it. So renderChoice creates an iterator that
	 *  will construct the paths to the possible choices, and when one of the paths
	 *  actually exists in the document, then that path is used to construct an
	 *  input element.
	 *
	 * @param  choiceElement  NOT YET DOCUMENTED
	 */
	public void render(Element choiceElement) {
		prtln("\nrendering with xpath = " + xpath);

		/* 	test for each of the possible choices (i.e., children of the choise
			compositor), do an existence check to see if that choice is present in
			the document. Then render the node 
		*/

		// create an InlineCompositor to supply leafMember names
		InlineCompositor vc = new InlineCompositor(complexTypeDef, choiceElement);
		List leafMemberNames = vc.getLeafMemberNames();
		for (Iterator i = leafMemberNames.iterator(); i.hasNext(); ) {
			String memberName = (String) i.next();

			/* 	for frameworks employing multiple namespaces, we have to assign an explicit
				namespace to member names in the default namespace.
			*/
			if (sh.getNamespaceEnabled() && !NamespaceRegistry.isQualified(memberName)) {
				String prefix = this.sh.getDefinitionMiner().getNamespaces()
					.getNamedDefaultNamespace().getPrefix();
				memberName = NamespaceRegistry.makeQualifiedName(prefix, memberName);
			}

			String path = xpath + "/" + memberName;
			Element choiceTestElement = rhelper.nodeExistsTest(path);
			parent.add(choiceTestElement);
			newRenderer(path, choiceTestElement).renderNode();
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

			// check for an empty choice (does the parent (aka xpath) have no memebers)?
			Element newChoiceTest = nodeExistsTest.addElement("logic__empty")
				.addAttribute("name", formBeanName)
				.addAttribute("property", "membersOf(" + xpath + "/*)");

			Element newChoiceDiv = df.createElement("div");

			newChoiceTest.add(newChoiceDiv);

			/*
				Add a hidden element here so that the validator
				will detect non-made choices.
			*/
			Element hidden = newChoiceDiv.addElement("input")
				.addAttribute("type", "hidden")
				.addAttribute("name", "valueOf(" + xpath + ")")
				.addAttribute("value", "");

			Element label = df.createElement("div")
				.addAttribute("class", "field-label");
				
			Element textElement = label.addElement("div");
			if (this.choiceCompositor.isRequiredChoice()) {
				textElement.addAttribute("class", "required-choice");
				textElement.setText("required choice");	
			}
			else {
				textElement.addAttribute("class", "optional-choice");
				textElement.setText ("optional choice");
			}

			Element input = getChoiceSelect();

			Element choiceField = 
				this.renderer.getRenderedField(xpath, label, input);

			// add label and value elements as a table to newChoiceDiv
			newChoiceDiv.add(choiceField);

		}
		else {
			prtln("mode is NOT edit (" + getMode() + ")");
		}
	}

	protected Element getChoiceSelect () {
		// --- CHOICE SELECT ----
		// tmpArg holds choice. It is initialized to "" so the "-- choice --" option is selected,
		// rather than one of the choices
		
		Element wrapper = df.createElement ("div");
		this.embedDebugInfo(wrapper, "MdeChoice - getChoiceSelect");
		
		Element tmpArgInit = wrapper.addElement("jsp__setProperty")
			.addAttribute("name", formBeanName)
			.addAttribute("property", "tmpArg")
			.addAttribute("value", "");

		// the select element for making a choice
		Element newChoiceSelect = wrapper.addElement("html__select")
			.addAttribute("name", formBeanName)
			.addAttribute("property", "tmpArg")
			.addAttribute("onchange", "return (doChoice(this.value))");

		// the permissible choices for this element
		Element newChoiceOptions = newChoiceSelect.addElement("html__optionsCollection")
			.addAttribute("property", "choiceOptions(" + xpath + ")");
			
		rhelper.attachToolHelp(newChoiceSelect, "Choose from drop down list");
		
		return wrapper;
	}


	/**
	 *  Sets the debug attribute of the MdeChoice class
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
		String prefix = "MdeChoice";
		if (debug) {
			SchemEditUtils.prtln(s, prefix);
		}
	}

}

