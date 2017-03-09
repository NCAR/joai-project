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
import org.dlese.dpc.xml.schema.*;


import java.util.*;
import org.dom4j.Node;
import org.dom4j.Attribute;
import org.dom4j.Element;

/**
 *  Render MetadataEditor inputs for all complexType elements at a given xpath, as well as a controller for 
 adding a sibling.
 *
 * @author     ostwald
 *
 */
public class MdeRepeatingComplexType  extends MdeRepeatingNode {
	/**  Description of the Field */
	private static boolean debug = true;
	String indexId;
	String siblingPath;
	String itemPath;
	boolean indexRepeatingContentLabels = true;

	public MdeRepeatingComplexType (RendererImpl renderer) {
		super(renderer);
		siblingPath = XPathUtils.getSiblingXPath(xpath);
		indexId = RendererHelper.encodeIndexId(siblingPath);
		itemPath = siblingPath + "_${" + indexId + "+1}_";
	}
	
	/**
	 *  Identical to SimpleJspRenderer.renderRepeatingElement, but adds a controller
	 *  for deleting items`
	 */
	public void render() {

		prtln ("level: " + getLevel());
		
		Element repeatingItemsBox = df.createElement("div");
		this.embedDebugInfo (repeatingItemsBox, "MdeRepeatingComplexType - repeatingItemsBox");
		
		Element iteration = null;
		
		if (isEditMode()) {
			
			// we don't want to show anything here if the parent doesn't exist!
			Element parentExists = rhelper.parentNodeExistsTest(xpath);
			parent.add (parentExists);
			
			parentExists.add (repeatingItemsBox);
			
			/* PROBLEM - /nsdl_anno/structuredOutline/header/outline is
			being evaluated as a isRepeatingComplexSingleton, but we want the
			emptyRepeatingElement to be added! 
			
			WHY don't we add emptyRepeating conrole here??
			Assumption: we DON'T want this control for "wrapped repeating singltons", e.g,
			- SimplePlacesAndEvents (this is where the add control is when there are no children)
			  - placeAndEvent (repeat) - don't want here (cause its above)
			*/
			Element addItemControl = emptyRepeatingElement();
 			if (!sh.isRepeatingComplexSingleton(normalizedXPath)) {
				repeatingItemsBox.add (addItemControl);
			}
			else {
				/* we only put addItemControl on OPTIONAL repeatingComplexSingletons,
				Since if the element does not exist, it won't be displayed without
				the addItemControl! */

				if (!this.schemaNode.isRequired()) {
					repeatingItemsBox.add (addItemControl);					
				}
			}
			
			// if this element has members, display them in the repeatingItemsBox
			Element hasMemberTest = rhelper.nodeHasMembersTest (xpath);
			repeatingItemsBox.add (hasMemberTest);
			
			iteration = getIteration(itemPath, siblingPath, indexId);
			insertRepeatingDisplaySetup(iteration);
			hasMemberTest.add (iteration);
			
			Element siblingController = newSiblingController();
			repeatingItemsBox.add (siblingController);
		}
		else {
			parent.add (repeatingItemsBox);
			iteration = getIteration(itemPath, siblingPath, indexId);
			repeatingItemsBox.add (iteration);
		}

		// attach repeatingContent as child of the iteration, so each item is displayed
		Element repeatingContent = getRepeatingContent(itemPath);
		iteration.add(repeatingContent);
		
	}	
	
	/**
	 *  Renders the contents of this node, which are in turn rendered inside an iteration element.
	 *
	 * @param  itemPath  the xpath for this element, with indexing to support iteration
	 * @return           The repeatingContent value
	 */
	protected Element getRepeatingContent(String itemPath) {
		// prtln ("getRepeatingContent() with itemPath: " + itemPath);
		Element repeatingContent = getRepeatingContentBox (itemPath);
		
		Label label;
		if (indexRepeatingContentLabels) {
			label = renderer.getComplexTypeLabel(xpath, itemPath, indexId);
		}
		else {
			label = renderer.getComplexTypeLabel(xpath);
		}
			
		String elementName = XPathUtils.getNodeName(this.siblingPath);
		Element controller = renderer.getDeleteController(itemPath, elementName);		
		
		Element inputHelper = this.renderer.getInputHelperElement(this.xpath);
		
		repeatingContent.add(getRenderedNoInputField(label, controller, inputHelper));
	
		Element subElementsBox = repeatingContent.addElement("div");
		subElementsBox.addAttribute("id", "${id}");
		if (getLevel() > 0 && isEditMode()) {
			subElementsBox.addAttribute("style", "display:${"+formBeanName+".collapseBean.displayState};");
			/* this.attachElementDebugInfo(subElementsBox, "subElementsBox: id=${id}", "green"); */
		}
		
		newRenderer(itemPath, subElementsBox).renderSubElements();	
		
		return repeatingContent;
	}

 	public static void setDebug(boolean bool) {
		debug = bool;
	}
	
	protected void prtln (String s) {
		String prefix = "MdeRepeatingComplexType";
		if (debug) {
			SchemEditUtils.prtln (s, prefix);
		}
	}

}

