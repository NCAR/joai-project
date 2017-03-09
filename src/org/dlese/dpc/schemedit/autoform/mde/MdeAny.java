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

import java.util.*;
import org.dom4j.Node;
import org.dom4j.Attribute;
import org.dom4j.Element;

/**
 *  Renders an ANY node.<P>
 *
 *  NOTE: this class is not used and should be considered as a stub only - it
 *  may not even represent the correct approach to handling "any" nodes.
 *
 * @author    ostwald
 *
 *
 */
public class MdeAny extends MdeSimpleType {
	/**  Description of the Field */
	private static boolean debug = false;


	/**
	 *  Constructor for the MdeAny object
	 *
	 * @param  renderer  NOT YET DOCUMENTED
	 */
	public MdeAny(RendererImpl renderer) {
		super(renderer);
	}
	
	protected void render_edit_mode() {
		super.render_edit_mode();
	}
	
	// <html:textarea property="elementAt(/annotationRecord/moreInfo/*)" style="width:98%" rows="8" styleId="${id}"/>
	protected Element getInputElement() {
		if (isEditMode()) {
			int rows = 8;
			Element input = df.createElement("html__textarea")
				.addAttribute("property", "anyTypeValueOf(" + xpath + ")")
				.addAttribute("style", "width__98%")
				.addAttribute("rows", String.valueOf(rows));
			return input;
		}
		else {
			Element valueDiv = df.createElement("div")
				.addAttribute("class", "static-value");
			// Must filter xml element!
			Element valueElement = valueDiv.addElement("bean__write")
				.addAttribute("name", formBeanName)
				.addAttribute("property", "anyTypeValueOf(" + xpath + ")")
				.addAttribute("filter", "true");
			return valueDiv;
		}
	}
		
	

	/**
	 *  Sets the debug attribute of the MdeAny class
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
		String prefix = "MdeAny";
		if (debug) {
			SchemEditUtils.prtln(s, prefix);
		}
	}

}

