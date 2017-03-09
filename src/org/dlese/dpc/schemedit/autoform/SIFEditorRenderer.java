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

import org.dlese.dpc.schemedit.sif.SIFRefIdManager;
import org.dlese.dpc.schemedit.sif.SIFRefIdMap;
import org.dlese.dpc.schemedit.autoform.mde.*;

import org.dlese.dpc.xml.*;
import org.dlese.dpc.xml.schema.NamespaceRegistry;
import org.dlese.dpc.xml.schema.GlobalDef;
import org.dlese.dpc.xml.schema.SchemaNode;

import org.dom4j.*;

import java.io.*;
import java.util.*;

/**
 *  Renders JSP for SIF (Schools Interoperability Framework) metadata
 *  frameworks, adding functionality for selecting from SIF objects, and other
 *  sif-specific input objects.
 *
 * @author    ostwald<p>
 *
 *
 */
public class SIFEditorRenderer extends DleseEditorRenderer {
	/**  Description of the Field */
	private static boolean debug = true;
	private SIFRefIdManager refIdMgr = SIFRefIdManager.getInstance();


	/**
	 *  Suppress rendering of SIF_ExtendedElements field, which includes "any"
	 *  construct and does not (at this point) contain any fields we need to
	 *  supply.
	 */
	public void renderNode() {
		// if (xpath.startsWith ("/sif:Activity/sif:SIF_ExtendedElements")) {
		if (XPathUtils.getNodeName(xpath).equals("sif:SIF_ExtendedElements")) {
			prtln("Refusing to render " + xpath);
			return;
		}
		else
			super.renderNode();
	}


	/**
	 *  Removes namespace prefix from label text
	 *
	 * @param  xpath        path to element to be rendered
	 * @param  siblingPath  sibling path to support indexing
	 * @param  indexId      indexId to support indexing
	 * @return              The labelText value
	 */
	public String getLabelText(String xpath, String siblingPath, String indexId) {
		if (siblingPath != null && indexId != null) {
			String nodeName =
				NamespaceRegistry.stripNamespacePrefix(XPathUtils.getNodeName(siblingPath));
			return (nodeName + " ${" + indexId + "+1}");
		}
		else {
			String nodeName =
				NamespaceRegistry.stripNamespacePrefix(XPathUtils.getNodeName(normalizedXPath));
			return nodeName;
		}
	}


	/**
	 *  Renders a textInput element using the sifRefId tag, which supports hooks to
	 *  the SIF Object finder and creators to aid user in suppling a sifRefId
	 *  (Reference ID to existing SIF object).
	 *
	 * @param  xpath       xpath of node to be rendered
	 * @param  schemaNode  schemaNode of node to be rendered
	 * @param  typeDef     typeDef
	 * @return             The textInput value
	 */
	protected Element getTextInput(String xpath, SchemaNode schemaNode, GlobalDef typeDef) {
		prtln ("getTextInput(): " + xpath);
		if (refIdMgr != null) {
			SIFRefIdMap refIdMap = refIdMgr.getRefIdMap(this.getXmlFormat());
			if (refIdMap != null && refIdMap.hasPath(this.normalizedXPath)) {
				prtln ("\t***");
				Element refIdTag = df.createElement("st__sifRefId")
					.addAttribute("property", "valueOf(" + xpath + ")")
					.addAttribute("sifType", refIdMap.getTypes(this.normalizedXPath));
				return refIdTag;
			}
		}

		return super.getTextInput(xpath, schemaNode, typeDef);
	}


	/**
	 *  Sets the debug attribute of the SIFEditorRenderer class
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
			System.out.println("SIFEditorRenderer: " + s);
		}
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  s  NOT YET DOCUMENTED
	 */
	private final void prtlnErr(String s) {
		System.err.println("SIFEditorRenderer: " + s);
	}

}

