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
 *  Renders JSP for metadata editing but is "Simple" in the sense that it provides no control for adding new
 *  elements or deleting optional elements. Used as a superClass for other JSP-based renderers, such as {@link
 *  org.dlese.dpc.schemedit.autoform.DleseEditorRenderer}.
 *
 * @author     ostwald<p>
 *
 */
public class MdeModelGroup  extends MdeComplexType {
	/**  Description of the Field */
	private static boolean debug = false;
	
	public MdeModelGroup (RendererImpl renderer) {
		super(renderer);
	}	
	
	/**
	 *  Render children elements of a ModelGroup compositor.
	 *
	 * @param  group  Description of the Parameter
	 */
	public void render (Element group) {
		
		prtln ("\nrenderModelGroup " + xpath);
		prtln ("\t globalDef element:\n" + typeDef.getElement().asXML());
		prtln ("\t group element:\n" + group.asXML());
		
		// we need to find the typeDef for the referenced group
		String ref = group.attributeValue("ref");
		GlobalDef globalDef = typeDef.getSchemaReader().getGlobalDef (ref);
		
		if (globalDef == null || !globalDef.isModelGroup()) {
			prtln ("\t *** groupDef not found!");
		}
		else {
			ModelGroup groupDef = (ModelGroup) globalDef;
			renderSubElements(groupDef.getChildren());
		}
	}
	
 	public static void setDebug(boolean bool) {
		debug = bool;
	}
	
	protected void prtln (String s) {
		String prefix = "MdeModelGroup";
		if (debug) {
			SchemEditUtils.prtln (s, prefix);
		}
	}

}

