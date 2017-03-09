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
 *  Renders JSP for metadata editing but is "Simple" in the sense that it
 *  provides no control for adding new elements or deleting optional elements.
 *  Used as a superClass for other JSP-based renderers, such as {@link
 *  org.dlese.dpc.schemedit.autoform.DleseEditorRenderer}.
 *
 * @author     ostwald
 *
 */
public class MdeComplexType extends MdeNode {
	/**  Description of the Field */
	private static boolean debug = true;

	String repeatingComplexSingletonChildPath = null;
	ComplexType complexTypeDef;
	
	/**
	 *  Constructor for the MdeComplexType object
	 *
	 * @param  renderer  NOT YET DOCUMENTED
	 */
	public MdeComplexType(RendererImpl renderer) {
		super(renderer);
		complexTypeDef = (ComplexType) typeDef;
		
		if (complexTypeDef == null) {
			prtln("\t complexTypeDef is NULL ... bailing");
			prtln("\n Here's typeDef:" + typeDef.toString());
			return;
		}
	}


	/**  Render editing field for this complex element */
	private void render_edit_mode() {
		prtln ("reder_edit_mode with: " + xpath);
		if (getLevel() != 0) {
			insertDisplaySetup(parent);
		}
		else {
			Element elementPath = df.createElement("c__set")
				.addAttribute("var", "elementPath")
				.addAttribute("value", xpath);
			parent.add(elementPath);

			Element setId = df.createElement("c__set")
				.addAttribute("var", "id")
				.addAttribute("value", "${sf__pathToId(elementPath)}");
			parent.add(setId);
		}
		
		Label label = renderer.getComplexTypeLabel(xpath);
		Element controlElement = null;

		Element containerBox = getComplexTypeBox();
		containerBox.addAttribute("id", "${id}_box");
		parent.add(containerBox);
		
		// test for the existence of a parent, unless we are at the 0th level of the page
		Element parentExists = null;
		if (getLevel() == 0) {
			parentExists = df.createElement("div");
			attachElementId (parentExists);
		}
		else {
			parentExists = rhelper.parentNodeExistsTest(xpath);
		}
		containerBox.add(parentExists);


		controlElement = df.createElement("div");
		attachControl(controlElement);

/* 		prtln ("---------- Control Element --------------");
		prtln (Dom4jUtils.prettyPrint(controlElement)); */
		
		parent = parentExists;
		
		Element inputHelper = this.renderer.getInputHelperElement(this.xpath);
		parent.add(getRenderedNoInputField(label, controlElement, inputHelper));
		
		// make collapsible subElementBox
		Element subElementsBox = parent.addElement("div");
		// this.attachElementDebugInfo(subElementsBox, "MdeComplexType - subElementsBox - ${id}");

		// add id to subElement Box so it can be collapsed
		subElementsBox.addAttribute("id", "${id}");
		if (getLevel() > 0 && isEditMode()) {
			subElementsBox.addAttribute("style", "display:${" + formBeanName + ".collapseBean.displayState};");
		}
		
		// new attach point becomes subElementsBox
		parent = subElementsBox;
		
		renderSubElements();

	}


	/**  Render display for this element */
	private void render_display_mode() {

		Label label = renderer.getComplexTypeLabel(xpath);
		Element controlElement = null;

		parent.add(getRenderedNoInputField(label, controlElement));

		Element subElementsBox = parent.addElement("div");

		// new attach point becomes subElementsBox
		parent = subElementsBox;
		renderSubElements();
	}


	/**  Render this complex type element */
	public void render() {

/* 		Label label = renderer.getComplexTypeLabel(xpath);
		Element controlElement = null; */

		if (isEditMode()) {
			render_edit_mode();
		}
		else {
			render_display_mode();
		}
	}


	/**
	 *  Attach a specific control (e.g., "add element") the the controlElement
	 of this node
	 *
	 * @param  controlElement  NOT YET DOCUMENTED
	 */
	private void attachControl(Element controlElement) {
		// build CONTROL ELEMENT
		
		prtln ("attach control");

 		if (hasRepeatingComplexSingletonChild()) {
			prtln ("  hasRepeatingComplexSingletonChild");
			
			if (sh.isChoiceElement(schemaNode)) {
				// if this repeatingComp... is also a choice, we need to provide a delete button
				prtln ("isChoiceElement()");
				
				String elementName = XPathUtils.getNodeName(normalizedXPath);
				Element choiceController = getChoiceDeleteController(xpath, elementName);
				controlElement.add(choiceController);
			}
			
			controlElement.add(getRepeatComplexSingletonControl());
			
		}
		
		else if (sh.isChoiceElement(schemaNode)) {
			prtln ("isChoiceElement()");
			
			String elementName = XPathUtils.getNodeName(normalizedXPath);
			Element choiceController = getChoiceDeleteController(xpath, elementName);
			controlElement.add(choiceController);
		}
		
		else if (!sh.isRequiredBranch(schemaNode)) {
			prtln ("optionalBranch()");
			// attachLabelDebugInfo(labelObj, "optional-element-label");
			controlElement.add(renderer.getOptionalItemControl(xpath));
		}
		else if (sh.hasMultiSelect(schemaNode)) {
			prtln ("  hasMultiSelect");
			if (getLevel() != 0) {
				/*
					add "choose" controller for the case where empty requiredMultiSelect is closed
					NOTE: this is needed only for collapsible editors, since when the parent is closed
					we need to indicate when a choice needs to be made. In non-collapsible editors
					the choices are always visible.
				*/
				controlElement.add(getRequiredMultiSelectControl());
			}
		}
		else {
			prtln ("  NO CONTROL ATTACHED");
		}
	}


	/**
	 *  Render the subitems of the current ComplexType element by walking the
	 *  children elements of the type definition. <p>
	 *
	 *  This method called by renderComplexTypeConcrete.
	 */
	public void renderSubElements() {
		renderSubElements(complexTypeDef.getChildren());
	}


	/**
	 *  Render the subelements of specified proxyTypeDef, used to render derived models.
	 *
	 * @param  proxyTypeDef  NOT YET DOCUMENTED
	 */
	public void renderSubElements(GlobalDef proxyTypeDef) {
		GlobalDef savedTypeDef = typeDef;
		typeDef = proxyTypeDef;
		renderSubElements();
		typeDef = savedTypeDef;
	}


	/**
	 *  Render the given subElements (or those of the current ComplexType if no
	 *  subElements are provided). This method called by renderComplexTypeConcrete.
	 *
	 * @param  subElements  A list of elements to be rendered.
	 */
	public void renderSubElements(List subElements) {
		// prtln ("\n renderSubElements() xpath: " + xpath);

		if (this.schemaNode.isRecursive()) {
			prtln ("...RECURSIVE - bailing");
			return;
		}
		
		List children = (subElements == null) ? complexTypeDef.getChildren() : subElements;

		children = orderSubElements(children);

		for (Iterator i = children.iterator(); i.hasNext(); ) {
			Element child = (Element) i.next();
			String childType = child.getName();
			String childName = child.attributeValue("name");

			// if we don't have a childName, check to see if the element is a reference, and
			// treat a reference the same as we would a named element
			if (childName == null) {
				childName = child.attributeValue("ref");
			}

			prtln("\nSubElements child\n\tchildType: " + childType + "\n\tchildName: " + childName);

			try {
				if (childType.equals("attribute")) {
					String attPath = xpath + "/@" + getQualifiedAttributeName(childName, child, complexTypeDef);
					newRenderer(attPath, parent).renderAttribute();
				}
	
				else if (childType.equals ("attributeGroup")) {
					prtln ("Attribute Group");
					GlobalDef globalDef = typeDef.getSchemaReader().getGlobalDef (childName);
					AttributeGroup attrGroup = (AttributeGroup) globalDef;
					for (Iterator a=attrGroup.getAttributes().iterator();a.hasNext();) {
						Element attrElement = (Element)a.next();
						String attrType = attrElement.getName();
						String attrName = attrElement.attributeValue("name");
			
						// if we don't have a childName, check to see if the element is a reference, and
						// treat a reference the same as we would a named element
						if (attrName == null) {
							attrName = attrElement.attributeValue("ref");
						}
						String attPath = xpath + "/@" + getQualifiedAttributeName(attrName, attrElement, complexTypeDef);
						newRenderer(attPath, parent).renderAttribute();
					}
				}
					
				
				else if (childType.equals("simpleContent")) {
	
					Element subElementsBox = getDiv(); // subElementsBox
					parent.add(subElementsBox);
					newRenderer(xpath, subElementsBox).renderDerivedTextOnlyModel();
				}
	
				else if (childType.equals("complexContent")) {
					Element subElementsBox = getDiv(); // subElementsBox
					parent.add(subElementsBox);
					newRenderer(xpath, subElementsBox).renderDerivedContentModel();
	
				}
	
				else if (childType.equals("all") ||
					childType.equals("sequence") ||
					childType.equals("choice")) {
	
 					Element subElementsBox = getDiv(); // subElementsBox
					parent.add(subElementsBox);
	
					if (childType.equals("choice")) {
						newRenderer(xpath, subElementsBox).renderChoice(child);
					}
					else {
						newRenderer(xpath, subElementsBox).renderSequence(child);
					}
				}
	
				else if (childType.equals("group")) {
					newRenderer(xpath, parent).renderModelGroup(child);
				}
	
				else if (childType.equals("element")) {
					String childPath = xpath + "/" + getQualifiedElementName(childName, complexTypeDef);
					newRenderer(childPath, parent).renderNode();
				}
	
				else if (childType.equals("any")) {
					String prefix = this.sh.getSchemaNamespace().getPrefix();
					
					String childPath = xpath + "/" + 
						NamespaceRegistry.makeQualifiedName(prefix,"any");
					// prtln ("\t childPath: " + childPath);
					newRenderer(childPath, parent).renderNode();
				}
				
				else {
					// experimental - now throwing an exception
					// throw new Exception ("render SubElements got unexpected childType: (" + childType + ")");
					prtln("\n***\nrender SubElements got unexpected childType?? (" + childType + ")");
					if (childName == null) {
						prtln("childName is null for " + xpath);
						return;
					}
				}
			} catch (Exception e) {
				prtln ("could not render child (\""+childName+"\") at " + xpath + ": " + e.getMessage());
				prtln (" .. bailing from renderSubElements()");
				e.printStackTrace();
				return;
			}
		}
	}


	/**
	 *  Debugging utility
	 *
	 * @param  elements  NOT YET DOCUMENTED
	 */
	private void showSubElements(List elements) {
		for (Iterator i = elements.iterator(); i.hasNext(); ) {
			Element e = (Element) i.next();
			prtln("\t" + e.asXML());
		}
	}


	/**
	 *  Order a list of elements so that the attributes are first. This is used by
	 *  renderSubElements so that the attributes are shown first (before
	 *  subelements) in the form
	 *
	 * @param  children  a list of elements, some of which may be attributes
	 * @return           list re-ordered to put attributes at top of list
	 */
	protected List orderSubElements(List children) {

/* 		prtln ("\n orderSubElements");
		prtln ("\n Before Sort");
		showSubElements (children); 
*/

		List sortedChildren = new ArrayList();
		for (Iterator i = children.iterator(); i.hasNext(); ) {
			Element e = (Element) i.next();
			if (e.getName().equals("attribute")) {
				sortedChildren.add(e);
			}
		}
		for (Iterator i = children.iterator(); i.hasNext(); ) {
			Element e = (Element) i.next();
			if (!e.getName().equals("attribute")) {
				sortedChildren.add(e);
			}
		}

/* 		prtln ("\n After Sort");
		showSubElements (sortedChildren); 
*/
		return sortedChildren;
	}


	/**
	 *  Create "choose" controller for the case where an empty requiredMultiSelect
	 *  node is closed. Called from renderComplexTypeConcrete, this controller
	 *  provides a prompt for user when the required multiselect boxes are empty
	 *  and not visible
	 *
	 * @return    The requiredMultiSelectControl value
	 */
	public Element getRequiredMultiSelectControl() {

		Element actionElement = df.createElement("span")
			.addAttribute("class", "action-button");

		// test to see if child node (the multiSelect) is empty
		Element isEmptyTest = actionElement.addElement("logic__equal")
			.addAttribute("name", formBeanName)
			.addAttribute("property", "nodeIsEmpty(" + xpath + ")")
			.addAttribute("value", "true");

		// test to see if the node is closed
		Element isOpenTest = isEmptyTest.addElement("c__if")
			.addAttribute("test", "${not " + formBeanName + ".collapseBean.isOpen}");

		//  "choose" controller link that toogles DisplayState of node
		Element chooseController = isOpenTest.addElement("a")
			.addAttribute("href", "javascript__toggleDisplayState(" + RendererHelper.jspQuotedString("${id}") + ")");
		chooseController.setText("choose");

		return actionElement;
	}

	/**
	 *  Create a controller for deleting a repeating item.
	 *
	 * @param  itemPath     Description of the Parameter
	 * @param  elementName  Description of the Parameter
	 * @return              The repeatingItemController value
	 */
	protected Element getChoiceDeleteController(String itemPath, String elementName) {

		Element deleteItemAction = df.createElement("span")
			.addAttribute("class", "action-button");
		Element deleteItemLink = deleteItemAction.addElement("a")
			.addAttribute("href", "javascript:doDeleteChoiceElement(" + RendererHelper.jspQuotedString(itemPath) + ")");
		rhelper.attachToolHelp(deleteItemLink, "delete this " + elementName + " and its subfields");

		deleteItemLink.setText("delete");

		return deleteItemAction;
	}

	/**
	 *  Produces "new item" controller that is attached to the field label of a
	 *  repeating element, and appears only when there are NO children. When there
	 *  ARE children, then the "new item" controller is presented at the bottom of
	 *  the repeating children.
	 *
	 * @return    The firstItemControl value
	 */
	protected Element getRepeatComplexSingletonControl() {
		if (!hasRepeatingComplexSingletonChild()) {
			prtln("getRepeatComplexSingletonControl() called for element without repeating complex singleton child");
			return null;
		}

		String childName = XPathUtils.getLeaf(getRepeatingComplexSingletonChildPath());

		// run-time test determines whether instance document can accept a new child
		// NOTE: (document why!) the test here must use:
		// "elementCount < 1", rather than "nodeIsEmpty"
		Element newItemControl = df.createElement("logic__lessThan")
			.addAttribute("name", formBeanName)
			.addAttribute("property", "childElementCountOf(" + xpath + ")")
			.addAttribute("value", "1");

		Element newItemAction = newItemControl.addElement("div")
			.addAttribute("class", "action-button");

		Element newItemLink = newItemAction.addElement("a")
			.addAttribute("href", "javascript:doNewElement(" + RendererHelper.jspQuotedString(getRepeatingComplexSingletonChildPath()) + ")");
		rhelper.attachToolHelp(newItemLink, "Create a new " + childName);

		newItemLink.setText("add " + childName);
		// attachElementDebugInfo(newItemAction, "rcsc", "red");

		return newItemControl;
	}


	/**
	 *  Returns true if the current node contains a single repeating child element
	 *  that is not an enumeration.
	 *
	 * @return    Description of the Return Value
	 */
	protected boolean hasRepeatingComplexSingletonChild() {
		String path = getRepeatingComplexSingletonChildPath();
		return (path.trim().length() != 0);
	}


	/**
	 *  Finds the xpath to a repeating child element if the current node contains
	 *  one. If not, return an empty string.
	 *
	 * @return    The repeatingComplexSingletonChildPath value
	 */
	protected String getRepeatingComplexSingletonChildPath() {
		if (this.repeatingComplexSingletonChildPath == null) {

			String childName = sh.getRepeatingComplexSingletonChildName(RendererHelper.normalizeXPath(xpath));
			// schemaHelper.getRepeatingComplexSingletonChildName returns an empty string if the schemaNode at
			// the indicated path does not contain a repeatingComplexSingletonChild element
			if (childName != null && childName.trim().length() > 0)
				this.repeatingComplexSingletonChildPath = xpath + "/" + childName;
			else
				this.repeatingComplexSingletonChildPath = "";
		}
		return this.repeatingComplexSingletonChildPath;
	}


	/**
	 *  Creates a "box" (a decorated Div element) containing this element's id and display state.
	 *
	 * @return    The complexTypeBox value
	 */
	protected Element getComplexTypeBox() {
		Element box = df.createElement("div");

		SchemaNode parentNode = sh.getParentSchemaNode(schemaNode);
		if (parentNode == null || getLevel() < 2) {
			return box;
		}

		/* 	make a collapsible box except for those with parents
		 	that are simpleOrComplex types with a requiredContentElements
		*/
		if (!sh.isRequiredContentElement(parentNode)) {
			box.addAttribute("id", "${id}");
			box.addAttribute("style", "display:${" + formBeanName + ".collapseBean.displayState};");
		}

		return box;
	}


	/**
	 *  Sets the debug attribute of the MdeComplexType class
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
		String prefix = "MdeComplexType";
		if (debug) {
			SchemEditUtils.prtln(s, prefix);
		}
	}

}

