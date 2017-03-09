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
 *  Renders JSP for DLESE metadata frameworks, adding functionality for choosing
 *  from suggested standards, and other dlese-specific input objects, including:
 *
 *  <ul>
 *    <li> Rendering multiBox elements using either Fields File, MUI groups, or
 *    StandardsManager (for suggestion service)
 *    <li> Rendering repeating elements as MdeStdNode if element has been
 *    configured to use standards manager
 *    <li> Rendering of textInput elements as textAreas for configured elements
 *    of "concepts" and "fields_files" frameworks
 *    <li> idiosyncratic rendering of xsd:string elements for certain fields in
 *    "adn" and "dlese_anno" frameworks
 *  </ul>
 *
 *
 *@author    ostwald<p>
 *
 *
 */
public class DleseEditorRenderer extends EditorRenderer {
	/**
	 *  Description of the Field
	 */
	private static boolean debug = true;

	/**
	 *  mdvMultiBoxTag specifies which kind of metaDataVocab mulitbox will be
	 *  rendered:
	 *  <li> mdvMultiBoxStateful - collapsible hierarchies that remember their
	 *  rhelper
	 *  <li> mdvMultiBoxStateless - collapsible hierarchies that DON'T remember
	 *  their rhelper
	 *  <li> mdvMultiBoxStatic - static hierarchies
	 *  <li> fieldInfoMultiBox - static hierarchical display of multiboxes
	 *  <li> muiMultiBox - MUI-driven display that handles collapsible nodes (as
	 *  defined in the mui-groups files)
	 */
	String mdvMultiBox = "muiMultiBox";


	/**
	 *  Create the multiBoxTag corresonsponding to the value of the mdvMultiBox
	 *  value
	 *
	 *@return    The multiBoxTag value
	 */
	private String getMultiBoxTag() {
		return "st__" + mdvMultiBox;
	}


	/**
	 *  Renders a multibox input (a set of of checkboxes) as an Element using one
	 *  of several methods of rendering a multiBoxInput, depending on what
	 *  information is available for the current element.<p>
	 *
	 *
	 *  <ul>
	 *    <li> If a MetadataVocab Mapping is available, the multibox input will be
	 *    rendered by the tag file returned by getMultiBoxTag</li>
	 *    <li> If the current element has fieldInfo, then the multibox is rendered
	 *    by the "fieldInfoMultiBox" tag</li>
	 *    <li> If neither of these are available, then the mulitbox is rendered by
	 *    a superclass, such as {@link org.dlese.dpc.schemedit.autoform.SimpleJspRenderer#getMultiBoxInput()}
	 *    </li>
	 *  </ul>
	 *
	 *
	 *@param  xpath  xpath of element to be rendered as multiBoxInput
	 *@return        The multiBoxInput as an element
	 */
	public Element getMultiBoxInput(String xpath) {
		String normalizedXPath = RendererHelper.normalizeXPath(xpath);
		prtln("getMultiBoxInput() with " + xpath);

		Element multiBoxInput = null;

		// does this field have a vocabLayout??
		if (rhelper.hasVocabLayout(xpath)) {
			prtln("\t has LAYOUT");
			Element layoutMultiBox = df.createElement("vl__vocabLayoutMultiBox")
					.addAttribute("elementPath", "enumerationValuesOf(" + XPathUtils.getSiblingXPath(xpath) + ")");
			multiBoxInput = layoutMultiBox;
		} // does this xpath have a MUI group?
		else if (rhelper.hasMuiGroupInfo(xpath)) {
			prtln("\t hasMuiGroupInfo");
			Element mdvMultiBox = df.createElement(getMultiBoxTag())
					.addAttribute("elementPath", "enumerationValuesOf(" + XPathUtils.getSiblingXPath(xpath) + ")");
			multiBoxInput = mdvMultiBox;
		} // 5/23/08 check not only that there is a fields file, but also that there is a termlist (vocabs) defined
		else if (rhelper.hasFieldInfo(xpath) &&
				!rhelper.getFieldInfo(xpath).getTermList().isEmpty()) {
			prtln("\t hasFieldInfo");
			Element fieldInfoMultiBox = df.createElement("st__fieldInfoMultiBox")
					.addAttribute("elementPath", XPathUtils.getSiblingXPath(xpath));
			multiBoxInput = fieldInfoMultiBox;
		} else {
			prtln("\t rendering generic multiBoxInput");
			multiBoxInput = super.getMultiBoxInput(xpath);
		}

		// does this xpath have a standardsManager?
		StandardsManager sm = rhelper.getFramework().getStandardsManager();
		if (sm != null && normalizedXPath.equals(sm.getXpath())) {

			Element helperChoice = df.createElement("c__choose");
			Element helperPresent = df.createElement("c__when")
					.addAttribute("test", "${not empty " + this.formBeanName + ".suggestionServiceHelper}");
			Element otherwise = df.createElement("c__otherwise");
			otherwise.add(multiBoxInput);
			helperChoice.add(helperPresent);
			helperChoice.add(otherwise);

			try {
				prtln("rendering as managedStandards_MultiBox (" + normalizedXPath + ")");
				Element standardsTag = df.createElement("std__" + sm.getRendererTag())
						.addAttribute("elementPath", "enumerationValuesOf(" + XPathUtils.getSiblingXPath(xpath) + ")")
						.addAttribute("pathArg", XPathUtils.getSiblingXPath(xpath));
				helperPresent.add(standardsTag);
			} catch (Throwable t) {
				prtln("WARNING: getMultiBoxInput() standards input error: " + t.getMessage());
			}
			return helperChoice;
		} else {
			return multiBoxInput;
		}
	}


	/**
	 *  If there is a vocabLayout (groups file) for this xpath, use it to layout
	 *  the element.<p>
	 *
	 *  NOTES<br/>
	 *  1 - the path passed to the "vl__vocabLayoutSingleSelect" is not a
	 *  "siblingPath" as is the case with multi-selects, but the actual xpath,
	 *  since here we are collecting a single value<br/>
	 *  2 - the "elementPath" attribute (missed-named) determines which method is
	 *  used to determine an input field's values. since this is a single-value
	 *  select, we use "valueOf".
	 *
	 *@param  xpath  path to the element to be rendered as a select input
	 *@return        The selectInput value
	 */
	public Element getSelectInput(String xpath) {
		// does this field have a vocabLayout??
		String normalizedXPath = RendererHelper.normalizeXPath(xpath);
		if (rhelper.hasVocabLayout(normalizedXPath)) {
			Element layoutSingleSelect = df.createElement("vl__vocabLayoutSingleSelect")
					.addAttribute("elementPath", "valueOf(" + xpath + ")");
			return layoutSingleSelect;
		}

		return super.getSelectInput(xpath);
	}


	/**
	 *  If this element has been configured to use suggestion service, render using
	 *  MdeStdsNode, otherwise render as usual.
	 */
	public void renderRepeatingElement() {
		String normalizedXPath = RendererHelper.normalizeXPath(xpath);

		StandardsManager sm = rhelper.getFramework().getStandardsManager();
		if (sm != null && normalizedXPath.equals(sm.getXpath())) {
			MdeStdsNode mdeStdsNode = new MdeStdsNode(this, sm);
			mdeStdsNode.render();
		} else {
			super.renderRepeatingElement();
		}
	}

	// pattern to find concept_{index}_/text xpaths in CONCEPTS records
	static Pattern content_text_pattern = Pattern.compile(".*?/content_\\$\\{.*?\\}_/text");
	static Pattern relation_text_pattern = Pattern.compile(".*?/relation_\\$\\{.*?\\}_/text");
	static Pattern hierarchy_label_pattern = Pattern.compile(".*?/hierarchy_\\$\\{.*?\\}_/@label");


	/**
	 *  Render configured paths as textArea inputs instead of regular text inputs.
	 *
	 *@param  xpath       xpath of input
	 *@param  schemaNode  schemaNode for this xpath
	 *@param  typeDef     the typeDefinion for this node
	 *@return             The textInput value
	 */
	protected Element getTextInput(String xpath, SchemaNode schemaNode, GlobalDef typeDef) {

		String xmlFormat = this.getXmlFormat();

		prtln("getTextInput: " + xmlFormat);

		// concepts hack: is this an element from the concepts framework that ends in content_text_pattern?
		if (xmlFormat.equals("concepts")) {
			Matcher m = content_text_pattern.matcher(xpath);
			if (m.matches() && m.end() == xpath.length()) {
				prtln("content_text: " + xpath);
				return getTextAreaInput(xpath, 4);
			}
			m = relation_text_pattern.matcher(xpath);
			if (m.matches() && m.end() == xpath.length()) {
				prtln("relation_text: " + xpath);
				return getTextAreaInput(xpath, 4);
			}
			m = hierarchy_label_pattern.matcher(xpath);
			if (m.matches() && m.end() == xpath.length()) {
				prtln("relation_text: " + xpath);
				return getTextAreaInput(xpath, 4);
			}
		}

		// fields_file hack: show certain nodes as text area
		if (xmlFormat.equals("fields_file")) {
			String nodeName = XPathUtils.getNodeName(this.normalizedXPath);
			prtln("NodeName: " + nodeName);
			if (nodeName.equals("practice") ||
					nodeName.equals("definition") ||
					nodeName.equals("termAndDeftn")) {
				return getTextAreaInput(xpath, 3);
			}
		}

		// library_dc hack: show rights as textarea
		if (xmlFormat.equals("library_dc")) {
			String nodeName = XPathUtils.getNodeName(this.normalizedXPath);
			prtln("NodeName: " + nodeName);
			if (nodeName.equals("dc:rights")) {
				return getTextAreaInput(xpath, 3);
			}
		}

		if (xmlFormat.equals("osm") || xmlFormat.equals("osm_next")) {
			if (this.normalizedXPath.equals("/record/general/abstract")) {
				return getTextAreaInput(xpath, 5);
			}
		}

		if (schemaNode.isDerivedModel()) {

			// because it is derived, the typeName for this element has been forced to that of the
			// extension element (see e.g., SimpleJspRenderer.renderDerivedXXXX)
			// String extensionTypeName = typeName;
			String extensionTypeName = typeDef.getQualifiedName();
			String stringType = NamespaceRegistry.makeQualifiedName(this.getSchemaNamespace(), "string");
			if (extensionTypeName.equals(stringType)) {
				if (showXsdStringInput(xpath)) {
					return super.getTextInput(xpath, schemaNode, typeDef);
				} else {
					return df.createElement("div");
				}
			} else {
				return super.getTextInput(xpath, schemaNode, typeDef);
			}
		} else {
			return super.getTextInput(xpath, schemaNode, typeDef);
		}
	}

	// ---------- xsd:string INPUTS to hide -------------------------
	List xsdStringInputsToHide = null;


	/**
	 *  Returns list of ADN xsd:string elements for which we do not display input
	 *  elements.<p>
	 *
	 *  These items are used only in showXsdStringInput.
	 *
	 *@return    The xsdStringInputsToHide value
	 */
	private List getXsdStringInputsToHide() {
		if (xsdStringInputsToHide == null) {
			xsdStringInputsToHide = new ArrayList();
			xsdStringInputsToHide.add("/itemRecord/geospatialCoverages/geospatialCoverage/detGeos/detGeo/longLats/longLat");
			xsdStringInputsToHide.add("/itemRecord/metaMetadata/dateInfo");
			xsdStringInputsToHide.add("/itemRecord/temporalCoverages/timeAndPeriod/timeInfo/timeAD/begin");
			xsdStringInputsToHide.add("/itemRecord/temporalCoverages/timeAndPeriod/timeInfo/timeAD/end");
		}
		return xsdStringInputsToHide;
	}


	/**
	 *  Control whether we display an input element for xsd:string inputs that are
	 *  used as the extention type for a derivedTextOnly Model schematype. <p>
	 *
	 *  Bottom Line, by DLESE convention, we almost NEVER show xsd:string elements
	 *  that are the base extension of a derivedTextOnlyModel. This method only
	 *  makes exceptions for the ADN framework. In all other frameworks, the
	 *  xsd:string extension element is NOT shown.
	 *
	 *@param  xpath  xpath of element to test
	 *@return        true if an input element should be shown for this path
	 */
	private boolean showXsdStringInput(String xpath) {
		String normalizedXPath = RendererHelper.normalizeXPath(xpath);
		String xmlFormat = rhelper.getFramework().getXmlFormat();

		// we only show xsd:string extensions in a few cases of ADN
		//   these are defined by the inverse of xsdStringInputsToHide!
		if (xmlFormat.equals("adn")) {
			return (!getXsdStringInputsToHide().contains(normalizedXPath));
		} else {
			return false;
		}
	}


	/**
	 *  Sets the debug attribute of the DleseEditorRenderer class
	 *
	 *@param  bool  The new debug value
	 */
	public static void setDebug(boolean bool) {
		debug = bool;
	}


	/**
	 *  Description of the Method
	 *
	 *@param  s  Description of the Parameter
	 */
	private static void prtln(String s) {
		if (debug) {
			while (s.length() > 0 && s.charAt(0) == '\n') {
				System.out.println("");
				s = s.substring(1);
			}
			System.out.println("DleseEditorRenderer: " + s);
		}
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 *@param  s  NOT YET DOCUMENTED
	 */
	private final void prtlnErr(String s) {
		System.err.println("DleseEditorRenderer: " + s);
	}

}

