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
package org.dlese.dpc.schemedit.autoform;

import org.dlese.dpc.schemedit.autoform.mde.*;
import org.dlese.dpc.schemedit.standards.StandardsManager;
import org.dlese.dpc.xml.*;
import org.dlese.dpc.xml.schema.*;

import java.util.*;
import java.util.regex.*;

import org.dom4j.Node;
import org.dom4j.Attribute;
import org.dom4j.Element;

/**
 *  Includes res_qual-specific kludges ...
 *
 * @author    ostwald
 */
public class ResQualEditorRenderer extends DleseEditorRenderer {
	private static boolean debug = false;

	/**
	 *  Render configured paths as textArea inputs instead of regular text inputs.
	 *
	 * @param  xpath       xpath of input
	 * @param  schemaNode  schemaNode for this xpath
	 * @param  typeDef     the typeDefinion for this node
	 * @return             The textInput value
	 */
	protected Element getTextInput(String xpath, SchemaNode schemaNode, GlobalDef typeDef) {

		String xmlFormat = this.getXmlFormat();
		prtln("getTextInput: " + xmlFormat);

		if (xmlFormat.equals("res_qual") && this.normalizedXPath.startsWith("/record/contentAlignment")) {
			String nodeName = XPathUtils.getNodeName(this.normalizedXPath);
			// prtln("NodeName: " + nodeName);

			if (nodeName.equals("comment") || nodeName.equals("learnGoalPart"))
				return getTextAreaInput(xpath, 2);
		}
		return super.getTextInput(xpath, schemaNode, typeDef);
	}


	/**
	 *  Sets the debug attribute of the ResQualEditorRenderer class
	 *
	 * @param  bool  The new debug value
	 */
	public static void setDebug(boolean bool) {
		debug = bool;
	}


	/**
	 *  Description of the Method
	 *
	 * @param  s  Description of the Parameter
	 */
	private static void prtln(String s) {
		if (debug) {
			while (s.length() > 0 && s.charAt(0) == '\n') {
				System.out.println("");
				s = s.substring(1);
			}
			System.out.println("ResQualEditorRenderer: " + s);
		}
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  s  NOT YET DOCUMENTED
	 */
	private final void prtlnErr(String s) {
		System.err.println("ResQualEditorRenderer: " + s);
	}

}

