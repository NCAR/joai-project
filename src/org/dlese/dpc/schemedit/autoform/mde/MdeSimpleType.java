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
 *  Renders JSP for SimpleType schema elements in the metadata editor. SimpleType elements have an input
 and no children elements or attributes.
 *
 * @author     ostwald<p>
 *
 */
public class MdeSimpleType  extends MdeNode {
	/**  Description of the Field */
	private static boolean debug = false;
	
	public MdeSimpleType (RendererImpl renderer) {
		super(renderer);
	}
	
	public void render() {
		if (schemaNode.isHeadElement()) {
			// renderSimpleTypeSubstitutionGroup();
			prtln ("WARNING: SimpleTypeSubstitution group not yet implemented!");
			return;
		}
		
		if (isEditMode()) {
			render_edit_mode();
		}
		else {
			render_display_mode();
		}
	}
	
	private boolean isMultiSelect () {
		return (typeDef != null && 
			this.sh.isEnumerationType(typeDef) && 
			this.sh.isMultiSelect(this.schemaNode));
	}
	
	protected void render_edit_mode() {
		
		Element parentExists = rhelper.parentNodeExistsTest(xpath);
		parent.add(parentExists);		
		insertDisplaySetup (parentExists);
		
		// LABEL - if we have a multiSelect, then use a complex label so we can make it
		// collapsible. Otherwise, use simpleTypeLabel
		Label label = null;
		
		if (this.isMultiSelect() && !this.sh.isSingleton(this.schemaNode)) {
			label = renderer.getMultiBoxLabel (xpath);
		}
		else {
			label = renderer.getSimpleTypeLabel(xpath);
		}
		
		// DELETE CONTROLLER
		// add a controller to delete this element if it is a repeating element BUT
		// not if it is a repeatingComplexSingleton, since repeatingComplexSingltons
		// have a separate label/controller for doing deletes.
		if (sh.isRepeatingElement (normalizedXPath) &&
			!sh.isRepeatingComplexSingleton (normalizedXPath)) {
			Element deleteController = renderer.getDeleteController(xpath, "field");
			((SimpleTypeLabel)label).control = deleteController;
		}
		
		// create box decorated with id attribute
		Element box = df.createElement("div")
			.addAttribute("id", "${id}_box");
		embedDebugInfo(box, "simpleType");
	
		// attach box
		parentExists.add(box);

		// create fieldElement (containing input element)
		Element fieldElement = df.createElement("div");
			
		// attachMessages(fieldElement);
		fieldElement.add(getInputElement());

		box.add(getRenderedField(label, fieldElement));
	}

	protected void render_display_mode() {
		
		attachElementId (parent);
		
		// LABEL
		SimpleTypeLabel label = renderer.getSimpleTypeLabel(xpath);
				
 		// create fieldElement (containing input element)
		Element fieldElement = df.createElement("div");
		fieldElement.add(getInputElement());

		parent.add(getRenderedField(label, fieldElement)); 
	}

	
 	public static void setDebug(boolean bool) {
		debug = bool;
	}
	
	protected void prtln (String s) {
		String prefix = "MdeSimpleType";
		if (debug) {
			SchemEditUtils.prtln (s, prefix);
		}
	}

}

