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

import java.util.*;
import org.dom4j.Node;
import org.dom4j.Attribute;
import org.dom4j.Element;

/**
 *  Renders Attributes in the Metadata Editor .
 *
 * @author     ostwald<p>
 *
 */
public class MdeAttribute  extends MdeNode {
	/**  Description of the Field */
	private static boolean debug = true;

	public MdeAttribute (RendererImpl renderer) {
		super (renderer);
	}
	
	public void render () {
		if (isEditMode()) {
			render_edit_mode();
		}
		else {
			render_display_mode();
		}
	}
	
	private void render_display_mode () {
		
		
		// we only display an attribute if it's parent exists!
		Element parentExists = this.rhelper.parentNodeExistsTest(xpath);
		parent.add (parentExists);	
		
		attachElementId (parentExists);
		
		Element box = getDiv() // attribute
			.addAttribute("id", "${id}_box");
		parentExists.add (box);
		

		
		
		// first set typeDef to validating TypeDef - WHY IS THIS NECESSARY?
		typeDef = schemaNode.getValidatingType();
		Element fieldElement = getInputElement();

		// create label Element
		Label label = renderer.getSimpleTypeLabel(xpath);

		// join label and field elements as table
		Element renderedField = getRenderedField(label, fieldElement);
		box.add(renderedField);
	}
	
	private void render_edit_mode() {
		
		Element parentExists = rhelper.parentNodeExistsTest(xpath);
		parent.add(parentExists);
		
		attachElementId (parentExists);
	
		// create box
		Element box = null;
		if (this.getLevel() > 1)
			box = getDiv();
		else
			box = df.createElement("div");
		
		box.addAttribute("id", "${id}_box");
		// this.attachElementDebugInfo(box, "attribute box (" + this.getLevel() + ")", "pink");

		// attach box
		parentExists.add(box);
			
		// create fieldElement (containing input element)
		Element fieldElement = df.createElement("div");
		this.embedDebugInfo(fieldElement, "MdeAttribute - fieldElement");
		
		// attachMessages(fieldElement);
		// set typeDef to validating TypeDef - WHY IS THIS NECESSARY?
		typeDef = schemaNode.getValidatingType();
		fieldElement.add(getInputElement());

		// create label Element
		Label label = renderer.getSimpleTypeLabel(xpath);

		// join label and field elements as table
		Element renderedField = getRenderedField(label, fieldElement);
		box.add(renderedField);
		
	}
	
	public static void setDebug (boolean verbose) {
		debug = verbose;
	}
	
	protected void prtln (String s) {
		String prefix = "MdeAttribute";
		if (debug) {
			SchemEditUtils.prtln (s, prefix);
		}
	}

}

