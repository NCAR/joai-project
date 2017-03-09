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
import org.dlese.dpc.util.Files;
import org.dlese.dpc.util.strings.FindAndReplace;

import java.util.*;
import org.dom4j.Node;
import org.dom4j.Attribute;
import org.dom4j.Element;

/**
 *  Render MetadataEditor inputs for all complexType elements at a given xpath,
 *  as well as a controller for adding a sibling.
 *
 * @author     ostwald
 */
public class MdeRepeatingDerivedTextOnlyModel extends MdeRepeatingNode {
	/**  Description of the Field */
	private static boolean debug = false;
	String indexId;
	String siblingPath;
	String itemPath;
	ComplexType complexTypeDef;
	boolean indexRepeatingContentLabels = true;


	/**
	 *  Constructor for the MdeRepeatingDerivedTextOnlyModel object
	 *
	 * @param  renderer  NOT YET DOCUMENTED
	 */
	public MdeRepeatingDerivedTextOnlyModel(RendererImpl renderer) {
		super(renderer);
		// prtln ("initialized MdeRepeatingDerivedTextOnlyModel (" + xpath + ")");
		siblingPath = XPathUtils.getSiblingXPath(xpath);
		indexId = RendererHelper.encodeIndexId(siblingPath);
		itemPath = siblingPath + "_${" + indexId + "+1}_";
		complexTypeDef = (ComplexType) typeDef;
	}


	/**
	 *  Identical to SimpleJspRenderer.renderRepeatingElement, but adds a
	 *  controller for deleting items`
	 */
	public void render() {
		prtln("\nrender() with " + xpath);
		// prtln ("mode: " + getMode());

		Element repeatingItemsBox = df.createElement("div");
		this.embedDebugInfo(repeatingItemsBox, "MdeRepeatingDerivedTextOnlyModel - repeatingItemsBox");

		Element iteration = null;
		// attachElementDebugInfo(repeatingItemsBox, "repeating Derived Text Only Box (" + xpath + ")", "blue");

		if (isEditMode()) {

			// we don't want to show anything here if the parent doesn't exist!
			Element parentExists = rhelper.parentNodeExistsTest(xpath);
			parent.add(parentExists);

			parentExists.add(repeatingItemsBox);

			if (!sh.isRepeatingComplexSingleton(normalizedXPath)) {
				repeatingItemsBox.add(emptyRepeatingElement());
			}

			Element hasMemberTest = rhelper.nodeHasMembersTest(xpath);
			
			repeatingItemsBox.add(hasMemberTest);
			iteration = getIteration(itemPath, siblingPath, indexId);
			insertRepeatingDisplaySetup(iteration);
			hasMemberTest.add(iteration);
			Element siblingController = newSiblingController();
			repeatingItemsBox.add(siblingController);
		}
		else {
			parent.add(repeatingItemsBox);
			iteration = getIteration(itemPath, siblingPath, indexId);
			repeatingItemsBox.add(iteration);
		}

		// attach repeatingContent as child of the iteration, so each item is displayed
		Element repeatingContent = getRepeatingContent();

		if (repeatingContent != null)
			iteration.add(repeatingContent);

	}


	/**
	 *  Renders the contents of this node, which are in turn rendered inside an iteration element.
	 *
	 * @return           The repeatingContent value
	 */
	protected Element getRepeatingContent() {
		// prtln ("getRepeatingContent() with itemPath: " + itemPath);
		Element repeatingContent = getRepeatingContentBox(itemPath);
		this.attachElementDebugInfo(repeatingContent, "getRepeatingContentBox", "pink");

		Label label;
		if (indexRepeatingContentLabels) {
			label = renderer.getComplexTypeLabel(xpath, itemPath, indexId);
		}
		else {
			label = renderer.getComplexTypeLabel(xpath);
		}

		String elementName = XPathUtils.getNodeName(this.siblingPath);
		Element controller = renderer.getDeleteController(itemPath, elementName);
		repeatingContent.add(getRenderedNoInputField(label, controller));

		// Element simpleContentBox = repeatingContent.addElement("div");
		Element simpleContentBox = this.getDiv();
		repeatingContent.add(simpleContentBox);
		simpleContentBox.addAttribute("id", "${id}");
		if (getLevel() > 0 && isEditMode()) {
			simpleContentBox.addAttribute("style", "display:${" + formBeanName + ".collapseBean.displayState};");
			this.attachElementDebugInfo(simpleContentBox, "simpleContentBox: id=${id}", "green");
		}

		parent = simpleContentBox;
		renderTextExtensionElement();

		// render extensionElement children - all of which must be attributes
		Iterator extnIterator = complexTypeDef.getExtensionElement().elementIterator();
		while (extnIterator.hasNext()) {
			Element attElement = (Element) extnIterator.next();
			String attElementName = attElement.getName();
			// make sure attElement defines an attribute!

			if (attElementName.equals("attribute")) {
				String attName = attElement.attributeValue("name");

				// if we don't have a attName, check to see if the element is a reference, and
				// treat a reference the same as we would a named element
				if (attName == null) {
					attName = attElement.attributeValue("ref");
				}
				String attPath = itemPath + "/@" + getQualifiedAttributeName(attName, attElement, complexTypeDef);
				newRenderer(attPath, parent).renderAttribute();
			}

			else if (attElementName.equals("attributeGroup")) {
				prtln("Attribute Group");
				String attName = attElement.attributeValue("name");

				// if we don't have a attName, check to see if the element is a reference, and
				// treat a reference the same as we would a named element
				if (attName == null) {
					attName = attElement.attributeValue("ref");
				}
				GlobalDef globalDef = typeDef.getSchemaReader().getGlobalDef(attName);
				AttributeGroup attrGroup = (AttributeGroup) globalDef;
				for (Iterator a = attrGroup.getAttributes().iterator(); a.hasNext(); ) {
					Element attrElement = (Element) a.next();
					String attrType = attrElement.getName();
					String attrName = attrElement.attributeValue("name");

					// if we don't have a childName, check to see if the element is a reference, and
					// treat a reference the same as we would a named element
					if (attrName == null) {
						attrName = attrElement.attributeValue("ref");
					}
					String attPath = itemPath + "/@" + getQualifiedAttributeName(attrName, attrElement, complexTypeDef);
					newRenderer(attPath, parent).renderAttribute();
				}
			}

			else {
				prtln("WARNING: renderSimpleContent() expected attribute or attributeGroup but found: " + attElementName);
				continue;
			}
		}

		return repeatingContent;
	}



	/**  Render the extension element of this derivedTextOnlyModel element */
	protected void renderTextExtensionElement() {

		prtln("renderTextExtensionElement \n\t (" + itemPath + ") \n\t level: " + getLevel());
		String extensionTypeName = getQualifiedElementName(complexTypeDef.getExtensionBase(), complexTypeDef);
		// prtln(" ... extension Base Type: " + extensionTypeName);

		String extnTypeName = getQualifiedElementName(complexTypeDef.getExtensionBase(), complexTypeDef);
		GlobalDef extnType = complexTypeDef.getSchemaReader().getGlobalDef(extnTypeName);
		newRenderer(itemPath, parent, extnType).renderNode();
	}


	/**
	 *  Sets the debug attribute of the MdeRepeatingDerivedTextOnlyModel class
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
		String prefix = "MdeRepeatingDerivedTextOnlyModel";
		if (debug) {
			SchemEditUtils.prtln(s, prefix);
		}
	}

}

