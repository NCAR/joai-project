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
import org.dlese.dpc.xml.schema.compositor.InlineCompositor;

import org.dom4j.Node;
import org.dom4j.Attribute;
import org.dom4j.Element;

/**
 *  Renders JSP for editing and displaying repeating Sequences in the metadata editor.
 *
 * @author     ostwald<p>
 *
 */
public class MdeRepeatingSequence extends MdeSequence {
	/**  Description of the Field */
	private static boolean debug = true;

	boolean indexRepeatingContentLabels = true;

	public MdeRepeatingSequence (RendererImpl renderer) {
		super(renderer);
	}
	
	public void render(Element sequenceElement) {
		
		InlineCompositor comp = new InlineCompositor (complexTypeDef, sequenceElement); 
		
		// IT appears the following block is strictly for debugging!?
		if (comp.getMaxOccurs() > 1) {
			Element box = df.createElement ("div");
			this.embedDebugInfo(box, "MdeRepeatingSequence - render box");
			// attachElementDebugInfo(box, "repeating sequence: " + comp.occursInfo(), "green");
			parent.add (box);
			parent = box;
		}
		
		renderSubElements(sequenceElement.elements());
	}

 	public static void setDebug(boolean bool) {
		debug = bool;
	}
	
	protected void prtln (String s) {
		String prefix = "MdeRepeatingSequence";
		if (debug) {
			SchemEditUtils.prtln (s, prefix);
		}
	}

}

