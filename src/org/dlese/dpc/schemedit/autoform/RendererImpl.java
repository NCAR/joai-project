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

import org.dlese.dpc.schemedit.SchemEditUtils;
import org.dlese.dpc.schemedit.autoform.mde.*;
import org.dlese.dpc.schemedit.vocab.FieldInfoReader;
import org.dlese.dpc.schemedit.display.CollapseUtils;

import org.dlese.dpc.xml.*;
import org.dlese.dpc.xml.schema.*;
import org.dlese.dpc.xml.schema.compositor.*;

import java.io.*;
import java.util.*;
import java.util.regex.*;

import org.dom4j.Node;
import org.dom4j.Attribute;
import org.dom4j.Element;
import org.dom4j.Document;

/**
 *  Concrete class for rendering a JSP-based representation of a SchemaNode (an
 *  Element or Attribute defined in an XML Schema). Editor pages are Rendered
 *  via recursive instantiations of Renderers starting with the base element for
 *  that page. <p>
 *
 *  The Renderer is initialized with information from the schema definition for
 *  the particular schemaNode (attribute or element) to be rendered, along with
 *  access to a RenderHelper, which provides utility functions. Rendered nodes
 *  are attached to the <b>parent</b> element (and thus to the Render-tree).
 *
 * @author    ostwald
 */
public class RendererImpl extends Renderer {
	private static boolean debug = true;

	public final static String EDIT_MODE = "edit_mode";
	/**  NOT YET DOCUMENTED */
	public final static String DISPLAY_MODE = "display_mode";
	private static boolean showNSPrefixInLabels = true;
	private String renderMode = EDIT_MODE;

	/**  Constructor for the Renderer object */
	public RendererImpl() {
		setRenderMode (EDIT_MODE);
	}


	/**
	* RenderMode determines whether we are editing a record, or simply viewing its contents.
	*/
	public void setRenderMode (String mode) {
		if (EDIT_MODE.equals(mode))
			this.renderMode = EDIT_MODE;
		else
			this.renderMode = DISPLAY_MODE;
	}
	
	public String getRenderMode() {
		return this.renderMode;
	}
	
	/**
	* Returns true if we are in display mode (not editing)
	*/
	public boolean isDisplayMode() {
		return DISPLAY_MODE.equals(renderMode);
	}
		
	/**
	* Returns true if we are editing field contents (as opposed to simply viewing them).
	*/
	public boolean isEditMode() {
		return EDIT_MODE.equals(renderMode);
	}

	/**
	 *  Sets the showNSPrefixInLabels attribute of the RendererImpl class
	 *
	 * @param  b  The new showNSPrefixInLabels value
	 */
	public static void setShowNSPrefixInLabels(boolean b) {
		showNSPrefixInLabels = b;
	}


	/**
	 *  Gets the showNSPrefixInLabels attribute of the RendererImpl class
	 *
	 * @return    The showNSPrefixInLabels value
	 */
	public static boolean getShowNSPrefixInLabels() {
		return showNSPrefixInLabels;
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  xpath          path to the node to be rendered
	 * @param  parent         element to which the rendered node will be attached
	 * @param  typeDef        schema-defined type definition for the node to be
	 *      rendered
	 * @param  rhelper        Helper class provided utility functions to aid
	 *      rendering
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	protected void doInit(String xpath, Element parent, GlobalDef typeDef, RendererHelper rhelper) throws Exception {
		init(xpath, parent, rhelper);

		// here is where we explicitly set the type for this Renderer
		this.typeDef = typeDef;
		this.typeName = typeDef.getQualifiedName();

	}


	/**
	 *  Gets the xpath of the Node to be rendered.
	 *
	 * @return    The xpath value
	 */
	public String getXpath() {
		return xpath;
	}


	/**  NOT YET DOCUMENTED */
	public void renderSimpleTypeConcrete() {
		if (this.typeDef.isAnyType()) {
			MdeAny anyType = new MdeAny(this);
			anyType.render();
		}
		else {
			MdeSimpleType n = new MdeSimpleType(this);
			n.render();
		}
	}


	/**
	 *  Renders a complexType element by creating an containerBox and then
	 *  populating it with a formattedLabel and the complexType element contents
	 *  (via renderSubElements).
	 */
	public void renderComplexTypeConcrete() {

		MdeComplexType mct = new MdeComplexType(this);
		mct.render();
	}


	/**
	 *  Render a Text-only content model, which is a complexType that uses a <b>
	 *  simpleContent</b> element. SimpleContent elements have base element that is
	 *  "extended" by adding attributes.
	 */
	public void renderDerivedTextOnlyModel() {
		prtln("renderDerivedTextOnlyModel()");
		MdeDerivedTextOnlyModel model = new MdeDerivedTextOnlyModel(this);
		model.render();
	}


	/**
	 *  Render a derived content model (complexType). Currently only "exensions"
	 *  are supported. Derivation of ComplexTypes by "restriction" will be
	 *  supported in a future version.
	 */
	public void renderDerivedContentModel() {
		// prtln ("renderDerivedContentModel()");
		MdeDerivedContentModel model = new MdeDerivedContentModel(this);
		model.render();
	}


	/**
	 *  called with group element, e.g., &lt;xs:group ref="nameGroup"/>
	 *
	 * @param  group  NOT YET DOCUMENTED
	 */
	public void renderModelGroup(Element group) {
		MdeModelGroup m = new MdeModelGroup(this);
		m.render(group);
	}


	/**  Render an Attribute schemaNode */
	public void renderAttribute() {
		// prtln ("renderAttribute()");
		MdeAttribute a = new MdeAttribute(this);
		a.render();
	}


	/**
	 *  Render a choice compository via either MdeChoice, for simple choices, or
	 *  MdeMultiChoice for choice compositors that have more than one occurance
	 *
	 * @param  choiceElement  choice compositor element from the parent node's
	 *      typeDefinition.
	 */
	public void renderChoice(Element choiceElement) {
		// prtln ("\nrenderChoice()");
		MdeChoice c = null;
		if (this.sh.isMultiChoiceElement(this.schemaNode))
			c = new MdeMultiChoice(this);
		else
			c = new MdeChoice(this);
		c.render(choiceElement);

	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  sequence  NOT YET DOCUMENTED
	 */
	public void renderSequence(Element sequence) {
		// prtln ("\nrenderSequence()");
		InlineCompositor comp = new InlineCompositor((ComplexType) typeDef, sequence);

		MdeSequence s = new MdeSequence(this);
		s.render(sequence);
	}


	/**  NOT YET DOCUMENTED */
	public void renderRepeatingSubstitutionGroup() {
		MdeRepeatingSubstitutionGroup rsg = new MdeRepeatingSubstitutionGroup(this);
		rsg.render();
	}


	/**  NOT YET DOCUMENTED */
	public void renderRepeatingElement() {
		// prtln ("\renderRepeatingElement()");

		if (typeDef.isAnyType()) {
			MdeRepeatingAnyType s = new MdeRepeatingAnyType(this);
			s.render();
		}
		else if (typeDef.isSimpleType() || typeDef.isBuiltIn()) {
			MdeRepeatingSimpleType s = new MdeRepeatingSimpleType(this);
			s.render();
		}
		else if (this.schemaNode.isDerivedContentModel()) {
			prtln("repeatingDerivedContentModel()");
			MdeRepeatingDerivedContentModel model = new MdeRepeatingDerivedContentModel(this);
			model.render();
		}
		else if (this.schemaNode.isDerivedTextOnlyModel()) {
			prtln("repeatingDerivedTextOnlyModel()");
			MdeRepeatingDerivedTextOnlyModel model = new MdeRepeatingDerivedTextOnlyModel(this);
			model.render();
		}
		else {
			MdeRepeatingComplexType c = new MdeRepeatingComplexType(this);
			c.render();
		}
	}


	/**
	 *  NOT YET DOCUMENTED
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

		MdeComplexType ct = new MdeComplexType(this);
		ct.renderSubElements(subElements);

	}


	/**  NOT YET DOCUMENTED */
	public void renderSubElements() {
		prtln("\nrenderSubElements()");
		if (typeDef != null && typeDef.isAnyType()) {
			prtln("\t ANY");
			MdeAny any = new MdeAny(this);
			any.render();
		}
		else if (typeDef == null || !typeDef.isComplexType()) {
			prtln("\t simple");
			MdeSimpleType st = new MdeSimpleType(this);
			st.render();
		}
		else {
			MdeComplexType ct = new MdeComplexType(this);
			ct.renderSubElements();
		}

	}

	// -------------- RendererImpl utilities ---------------

	/**
	 *  Render a Best Practices link for the specified xpath
	 *
	 * @param  xpath  NOT YET DOCUMENTED
	 * @return        NOT YET DOCUMENTED
	 */
	public Element bestPracticesLink(String xpath) {
		Element bpLink = df.createElement("span")
			.addAttribute("class", "action-button");

		// String format = rhelper.getFramework().getXmlFormat();
		String pathArg = RendererHelper.normalizeXPath(xpath);
		String fieldName = XPathUtils.getNodeName(pathArg);

		Element tag = df.createElement("st__bestPracticesLink")
			.addAttribute("pathArg", pathArg)
			.addAttribute("fieldName", fieldName);

		bpLink.add(tag);
		return bpLink;
	}


	/**
	 *  Attempts to return a metadata vocab field definition for the input string
	 *  assuming it is an xpath. The field definition is obtained from a {@link
	 *  FieldInfoReader} for the xpath. If a FieldInfoReader cannot be found,
	 *  return the input string (encoded so whether an xpath or a regular string,
	 *  there won't be a javascript error)..
	 *
	 * @param  s  A string that may or may not corrrespond to a field that has
	 *      metadata-vocab definition information
	 * @return    A string to be used as ToolHelp which is the metadata field
	 *      definition if possible.
	 */
	public String getToolHelp(String s) {
		FieldInfoReader fieldInfoReader = rhelper.getFieldInfo(s);
		if (fieldInfoReader != null) {
			try {
				return fieldInfoReader.getDefinition();
			} catch (Exception e) {
				prtln("getToolHelp: unable to obtain definition for " + s);
			}
		}
		return "${sf:decodePath(" + RendererHelper.jspQuotedString(s) + ")}";
	}


	/**
	 *  Creates the JSP element to render an input helper for the specified path
	 *
	 * @param  xpath  NOT YET DOCUMENTED
	 * @return        The inputHelper value
	 */
	public Element getInputHelperElement(String xpath) {
		Element inputHelper = null;
		if (!this.isEditMode())
			return null;
		String inputHelperPath = this.rhelper.getInputHelperFile(xpath);
		if (inputHelperPath != null) {
			String path = "/editor/input_helpers/" + inputHelperPath;
			inputHelper = df.createElement("div")
				.addAttribute("class", "input-helper");
			inputHelper.setText("<%@ include file=\"" + path + "\" %>");
		}
		return inputHelper;
	}

	// -------- input elements --------------
	/**
	 *  Gets the inputElement attribute of the EditorRenderer object
	 *
	 * @param  xpath       NOT YET DOCUMENTED
	 * @param  schemaNode  NOT YET DOCUMENTED
	 * @param  typeDef     NOT YET DOCUMENTED
	 * @return             The inputElement value
	 */
	protected Element renderInputElement(String xpath, SchemaNode schemaNode, GlobalDef typeDef) {
		// prtln ("\n renderInputElement() with " + xpath);

		if (schemaNode.isReadOnly()) {
			return getReadOnlyElement(xpath);
		}

		// xsd:LANGUAGE KLUDGE
		String langageType = NamespaceRegistry.makeQualifiedName(this.getSchemaNamespace(), "language");
		if (schemaNode.getValidatingType().getName().equals(langageType)) {
			return getLanguageInput(xpath);
		}

		// built-ins have no typeDef, and are assumed to use TextInput
		if (typeDef == null || typeDef.isBuiltIn()) {
			return getTextInput(xpath, schemaNode, typeDef);
		}

		if (sh.isEnumerationType(typeDef)) {
			if (sh.isMultiSelect(schemaNode)) {
				return getMultiSelectInput(xpath);
			}
			else {
				return getSelectInput(xpath);
			}
		}
		else if (sh.isComboUnionType(typeDef)) {
			return getComboUnionInput(xpath);
		}
		else {
			return getTextInput(xpath, schemaNode, typeDef);
		}
	}


	/**
	 *  Gets the inputElement attribute of the RendererImpl object
	 *
	 * @param  xpath       NOT YET DOCUMENTED
	 * @param  schemaNode  NOT YET DOCUMENTED
	 * @param  typeDef     NOT YET DOCUMENTED
	 * @return             The inputElement value
	 */
	public Element getInputElement(String xpath, SchemaNode schemaNode, GlobalDef typeDef) {
		Element inputElement = renderInputElement(xpath, schemaNode, typeDef);

		/*
			NOTE: the complexity of this method is a KLUDGE! We should have classes
			responsible for building their input fields. These classes would have attributes
			such as:
			- inputHelper
			- inputElement
			- ...
			And their "render" method would produce the required XML/JSP
		*/

		Element inputHelper = getInputHelperElement(xpath);
		
		Element prompts = df.createElement("st__fieldPrompt")
			.addAttribute("pathArg", xpath);
		
		/* we're about to have to handle the various inputs that are possible:
			- html tags (these require "styleId" param
			- other vanilla input tags (which require "id" param)
			- tags, such as that produced by getInputSingleSelect
			
			look at below and decide: should we test i
			1. test for jsp tag
			2. test for html tag
			3. all others (table being a special case)
			
		*/
		String inputElementName = inputElement.getName();
		// prtln ("\n** Input element name: " + inputElement.getName());
			
		/* if the 'inputElement' is a table, this is a complex element and there is input 
			for this path. in this case we assign the id to the table to support hide/show 
		*/
		if (inputElement.getName().equals("table")) {
			inputElement.addAttribute("id", "${id}");
			List rows = inputElement.elements();
			Element myRow = df.createElement("tr");
			Element myCell = myRow.addElement("td");
			myCell.add(prompts);
			if (inputHelper != null)
				myCell.add(inputHelper);
			rows.add(0, myRow);
		}
		else {
			/*
				If the inputElement is just a single element, then we assume this is
				truly an input (textarea, text, etc). In this case we create a
				wrapper, then add the prompt and helper and the	inputElement inside the wrapper.
				NOTE: the id attribute gets attached to the input. Since the input is rendered
				using an "html" tag, the id attribute name is 'styleId'
			*/
			Element wrapper = df.createElement("div");
			wrapper.add(prompts);
			if (inputHelper != null)
				wrapper.add(inputHelper);
			wrapper.add(inputElement);
			
			if (inputElementName.startsWith("html__")) {
				inputElement.addAttribute("styleId", "${id}");
			}
			else if (inputElementName.startsWith("vl__")) {
			}
			else
				inputElement.addAttribute("styleId", "${id}");
			inputElement = wrapper;
		}
			

		if (schemaNode.isDerivedModel() && !this.sh.isRequiredBranch(schemaNode)) {
			Element nodeExists = rhelper.nodeExistsTest(xpath);
			nodeExists.add(inputElement);
			return nodeExists;
		}
		else
			return inputElement;
	}


	/**
	 *  Gets the readOnlyElement attribute of the RendererImpl object
	 *
	 * @param  xpath  NOT YET DOCUMENTED
	 * @return        The readOnlyElement value
	 */
	protected Element getReadOnlyElement(String xpath) {

		// prtln ("\t READ-ONLY");
		Element readOnlyElement = df.createElement("div");
		this.rhelper.embedDebugInfo(readOnlyElement, "RendererImpl - readOnlyElement");
		Element text = readOnlyElement.addElement("bean__write")
			.addAttribute("name", formBeanName)
			.addAttribute("property", "valueOf(" + xpath + ")");
		// we add a hidden variable so that if no id is assigned, the validator will catch it
		Element hidden = readOnlyElement.addElement("html__hidden")
			.addAttribute("property", "valueOf(" + xpath + ")");
		return readOnlyElement;
	}


	/**
	 *  Gets the textInput attribute of the RendererImpl object
	 *
	 * @param  xpath       NOT YET DOCUMENTED
	 * @param  schemaNode  NOT YET DOCUMENTED
	 * @param  typeDef     NOT YET DOCUMENTED
	 * @return             The textInput value
	 */
	protected Element getTextInput(String xpath, SchemaNode schemaNode, GlobalDef typeDef) {

		// by convention, input for elements named "description" are rendered as textArea
		if (NamespaceRegistry.stripNamespacePrefix(XPathUtils.getLeaf(xpath)).startsWith("description")) {
			return getTextAreaInput(xpath);
		}

		// builtIn boolean typeDefs are rendered using getBooleanInput
		if (typeDef.isBuiltIn() &&
			"any".equals(NamespaceRegistry.stripNamespacePrefix(typeDef.getQualifiedName()))) {
			return getTextAreaInput(xpath);
		}

		// builtIn boolean typeDefs are rendered using getBooleanInput
		if (typeDef.isBuiltIn() &&
			"boolean".equals(NamespaceRegistry.stripNamespacePrefix(typeDef.getQualifiedName()))) {
			return getBooleanInput(xpath);
		}

		int maxLen = rhelper.getMaxLen(xpath);

		Element input = df.createElement("html__text")
			.addAttribute("property", "valueOf(" + xpath + ")")
			.addAttribute("size", String.valueOf(maxLen));

		// enforce maxLen if specified in framework config
		if (maxLen != RendererHelper.DEFAULT_TEXT_INPUT_SIZE) {
			input.addAttribute("maxlength", String.valueOf(maxLen));
		}
		return input;
	}


	/**
	 *  Boolean input elements are rendered with a tag that implements an select
	 *  input with options for true and false.
	 *
	 * @param  xpath  NOT YET DOCUMENTED
	 * @return        The booleanInput value
	 */
	protected Element getBooleanInput(String xpath) {
		Element booleanInput = df.createElement("st__booleanSelect")
			.addAttribute("elementPath", "valueOf(" + xpath + ")");
		return booleanInput;
	}


	/**
	 *  Gets the textAreaInput attribute of the RendererImpl object
	 *
	 * @param  xpath  NOT YET DOCUMENTED
	 * @return        The textAreaInput value
	 */
	public Element getTextAreaInput(String xpath) {
		return getTextAreaInput(xpath, 8);
	}


	/**
	 *  Gets the textAreaInput attribute of the RendererImpl object
	 *
	 * @param  xpath  NOT YET DOCUMENTED
	 * @param  rows   NOT YET DOCUMENTED
	 * @return        The textAreaInput value
	 */
	public Element getTextAreaInput(String xpath, int rows) {
		Element input = df.createElement("html__textarea")
			.addAttribute("property", "valueOf(" + xpath + ")")
			.addAttribute("style", "width__98%")
			.addAttribute("rows", String.valueOf(rows));
		return input;
	}



	/**
	 *  Gets the selectInput attribute of the RendererImpl object
	 *
	 * @param  xpath  NOT YET DOCUMENTED
	 * @return        The selectInput value
	 */
	public Element getSelectInput(String xpath) {
		Element input = df.createElement("html__select")
			.addAttribute("name", formBeanName)
			.addAttribute("property", "valueOf(" + xpath + ")");
		Element options = input.addElement("html__optionsCollection")
			.addAttribute("property", "selectOptions(" + RendererHelper.normalizeXPath(xpath) + ")");
		return input;
	}


	/**
	 *  Gets the languageSelectInput attribute of the RendererImpl object
	 *
	 * @param  xpath  NOT YET DOCUMENTED
	 * @return        The languageSelectInput value
	 */
	public Element getLanguageInput(String xpath) {
		Element select = df.createElement("st__languageSelect")
			.addAttribute("elementPath", xpath);
		return select;
	}


	/*
	    <logic:iterate name=formBeanName indexId="index" property="enumerationOptions(${subjectsPath})" id="item">
	    <html:multiSelect property="enumerationValuesOf(${subjectsPath})">
	    <bean:write name="item" property="value"/>
	    </html:multiSelect>
	    <bean:write  name="item" property="label"/><br>
	    </logic:iterate>
	  */
	/**
	 *  Renders a multiSelect input (a set of of checkboxes) as an Element. The
	 *  multiSelect is represented as a HTML table
	 *
	 * @param  xpath  NOT YET DOCUMENTED
	 * @return        The multiSelectInput as an element
	 */
	public Element getMultiSelectInput(String xpath) {
		// prtln("getMultiSelectInput() with " + xpath);

		Element multiSelectTable = df.createElement("table");

		if (!this.sh.isSingleton(xpath)) {
			// multiSelectTable.addAttribute("info", "multiSelectTable: id=${id}");
			String style = "display__${" + formBeanName + ".collapseBean.displayState};";
			// style +=  "border__red thin dotted;";
			multiSelectTable.addAttribute("style", style);
		}

		multiSelectTable.addAttribute("width", "100%");

		Element row = multiSelectTable.addElement("tr")
			.addAttribute("valign", "top");

		Element multiSelectCell = row.addElement("td");

		/*
			a hidden field is necessary to compensate for the fact that we don't have a reset method in the
			form bean. the hidden field points to the subjectsPath without an index modifier on the leaf..
		  */
		Element hidden = multiSelectCell.addElement("input")
			.addAttribute("type", "hidden")
			.addAttribute("name", "enumerationValuesOf(" + XPathUtils.getSiblingXPath(xpath) + ")")
			.addAttribute("value", "");

		multiSelectCell.add(getMultiBoxInput(xpath));

		return multiSelectTable;
	}



	/**
	 *  Renders a MultiBoxInput (a set of checkboxes) for the current element.
	 *
	 * @param  xpath  NOT YET DOCUMENTED
	 * @return        The getMultiBoxInput value
	 */
	protected Element getMultiBoxInput(String xpath) {
		String normalizedXPath = RendererHelper.normalizeXPath(xpath);
		Element multiBoxInput = df.createElement("logic__iterate")
			.addAttribute("indexId", "index")
			.addAttribute("id", "item")
			.addAttribute("name", formBeanName)
			.addAttribute("property", "enumerationOptions(" + XPathUtils.getSiblingXPath(normalizedXPath) + ")");

		Element labelFormat = multiBoxInput.addElement("div")
			.addAttribute("class", "multiSelect-label");

		String labelId = CollapseUtils.pathToId(xpath) + "_${item.value}_label";

		Element multiSelect = labelFormat.addElement("html__multibox")
			.addAttribute("property", "enumerationValuesOf(" + xpath + ")")
			.addAttribute("styleId", labelId);

		/* "filter=false" addresses problem where controlled vocab contains an apos ("'")
			by supressing bean:write's default behavior of quoting.
			but will this have any side effects?
			and where else do we need to display controlled vocabs this way?
		*/
		Element value = multiSelect.addElement("bean__write")
			.addAttribute("name", "item")
			.addAttribute("property", "value")
			.addAttribute("filter", "false");

		Element label = labelFormat.addElement("label")
			.addAttribute("for", labelId);

		Element labelText = label.addElement("bean__write")
			.addAttribute("name", "item")
			.addAttribute("property", "label");

		return multiBoxInput;
	}


	/**
	 *  comboUnionInput allows user to select from enumerated list (supplied by
	 *  schema) or enter in an arbitray value if desired. This input element is
	 *  rendered by the "comboInput" tag
	 *
	 * @param  xpath  NOT YET DOCUMENTED
	 * @return        The comboUnionInput value
	 */
	protected Element getComboUnionInput(String xpath) {
		Element select = df.createElement("st__comboInput")
			.addAttribute("elementPath", xpath);
		return select;
	}



	// ----------- labels -----------------------

	/**
	 *  Gets the simpleTypeLabel attribute of the RendererImpl object
	 *
	 * @param  xpath  NOT YET DOCUMENTED
	 * @return        The simpleTypeLabel value
	 */
	public SimpleTypeLabel getSimpleTypeLabel(String xpath) {
		return getSimpleTypeLabel(xpath, null, null);
	}


	/**
	 *  Gets the simpleTypeLabel attribute of the RendererImpl object
	 *
	 * @param  xpath        NOT YET DOCUMENTED
	 * @param  siblingPath  NOT YET DOCUMENTED
	 * @param  indexId      NOT YET DOCUMENTED
	 * @return              The simpleTypeLabel value
	 */
	public SimpleTypeLabel getSimpleTypeLabel(String xpath, String siblingPath, String indexId) {
		// prtln ("\ngetSimpleTypeLabel()");
		String normalizedXPath = RendererHelper.normalizeXPath(xpath);
		SchemaNode schemaNode = this.sh.getSchemaNode(normalizedXPath);

		SimpleTypeLabel labelObj = new SimpleTypeLabel();

		if (sh.isRequiredAttribute(schemaNode)) {
			// prtln ("\t isRequiredAttribute");
			labelObj.setRequired();
			labelObj.setFieldType("attribute");
		}
		else if (sh.isRequiredContentElement(schemaNode)) {
			// prtln ("\t isRequiredContentElement");
			labelObj.setRequired();
			labelObj.setFieldType("element");
		}

		else if (schemaNode.isAttribute()) {
			// prtln ("\t isAttribute");
			// labelObj.setLabelClass("element-label optional");
			labelObj.setFieldType("attribute");
		}
		else {
			// prtln ("\t is OTHER");
			// labelObj.setLabelClass("element-label");
			labelObj.setFieldType("element");
		}
		// prtln ("");

		labelObj.setText(getLabelText(xpath, siblingPath, indexId));

		// add best practices if there is field info available
		if (rhelper.hasFieldInfo(xpath)) {
			labelObj.bestPractices = bestPracticesLink(xpath).createCopy();
		}

		return labelObj;
	}


	/**
	 *  Label text is indexed if siblingPath and indexId provided, otherwise based
	 *  on xpath
	 *
	 * @param  xpath        NOT YET DOCUMENTED
	 * @param  siblingPath  NOT YET DOCUMENTED
	 * @param  indexId      NOT YET DOCUMENTED
	 * @return              The labelText value
	 */
	public String getLabelText(String xpath, String siblingPath, String indexId) {
		if (siblingPath != null && indexId != null) {
			String nodeName = XPathUtils.getNodeName(siblingPath);
			return (nodeName + " ${" + indexId + "+1}");
		}
		else {
			String nodeName = XPathUtils.getNodeName(normalizedXPath);
			return nodeName;
		}
	}


	/**
	 *  Gets the multiBoxLabel attribute of the RendererImpl object
	 *
	 * @param  xpath  NOT YET DOCUMENTED
	 * @return        The multiBoxLabel value
	 */
	public Label getMultiBoxLabel(String xpath) {
		Label labelObj = getSimpleTypeLabel(xpath);
		labelObj.setText(labelObj.getText() + " (simple)");
		return labelObj;
	}

	// only show delete controller in edit mode
	/**
	 *  Gets the deleteController attribute of the RendererImpl object
	 *
	 * @param  itemPath     NOT YET DOCUMENTED
	 * @param  elementName  NOT YET DOCUMENTED
	 * @return              The deleteController value
	 */
	public Element getDeleteController(String itemPath, String elementName) {

		Element deleteController = df.createElement("span")
			.addAttribute("class", "action-button");

		String jsCall = null;
		if (this.sh.isAnyTypeElement(itemPath))
			jsCall = "doDeleteAnyElement";
		else
			jsCall = "doDeleteElement";

		Element deleteItemLink = deleteController.addElement("a")
			.addAttribute("href", "javascript:" + jsCall + "(" + RendererHelper.jspQuotedString(itemPath) + ")");

		rhelper.attachToolHelp(deleteItemLink, "delete this " + elementName);
		// deleteItemLink.add (getDelImg());
		deleteItemLink.setText("delete");

		return deleteController;
	}


	/**
	 *  Creates an optionalItemControl Element that allows user to add or delete an
	 *  optional Element to/from the instance document
	 *
	 * @param  xpath  NOT YET DOCUMENTED
	 * @return        The optionalItemControl Element
	 */
	public Element getOptionalItemControl(String xpath) {
		// prtln ("getOptionalItemControl()");
		// test for whether the element exists
		Element baseDiv = df.createElement("div");
		this.rhelper.embedDebugInfo(baseDiv, "RenderImpl - getOptionalItemControl");

		Element elementNotExists = rhelper.nodeNotExistsTest(xpath);
		baseDiv.add(elementNotExists);

		Element newItemAction = elementNotExists.addElement("span")
			.addAttribute("class", "action-button");

		Element newItemLink = newItemAction.addElement("a")
			.addAttribute("href", "javascript:doNewElement(" + RendererHelper.jspQuotedString(xpath) + ")");
		rhelper.attachToolHelp(newItemLink, "Specify the contents of this optional element");
		newItemLink.setText("choose");

		Element elementExists = rhelper.nodeExistsTest(xpath);
		baseDiv.add(elementExists);

		Element delItemAction = elementExists.addElement("span")
			.addAttribute("class", "action-button");

		Element delItemLink = delItemAction.addElement("a")
			.addAttribute("href", "javascript:doDeleteElement(" + RendererHelper.jspQuotedString(xpath) + ")");
		rhelper.attachToolHelp(delItemLink, "remove this optional element");
		delItemLink.setText("remove");
		return baseDiv;
	}


	/**
	 *  Gets the complexTypeLabel attribute of the RendererImpl object
	 *
	 * @param  xpath  NOT YET DOCUMENTED
	 * @return        The complexTypeLabel value
	 */
	public ComplexTypeLabel getComplexTypeLabel(String xpath) {
		return getComplexTypeLabel(xpath, null, null);
	}


	/**
	 *  Gets the complexTypeLabel attribute of the RendererImpl object
	 *
	 * @param  xpath        NOT YET DOCUMENTED
	 * @param  siblingPath  NOT YET DOCUMENTED
	 * @param  indexId      NOT YET DOCUMENTED
	 * @return              The complexTypeLabel value
	 */
	public ComplexTypeLabel getComplexTypeLabel(String xpath, String siblingPath, String indexId) {
		String normalizedXPath = RendererHelper.normalizeXPath(xpath);
		SchemaNode schemaNode = this.sh.getSchemaNode(normalizedXPath);

		ComplexTypeLabel labelObj = new ComplexTypeLabel();

		// apply different style to top-level labels
		if (getLevel(xpath) == 0) {
			labelObj.setLabelClass("main-element-label");
		}
		else {

			/* label an element as required if
				- it is a requiredBranch OR
				- it is requiredContent
				AND
				- the element is not repeating
			this is not perfect, since sometimes (such as the last repeating element) we
			might want to designate as required)
			*/
			if ((sh.isRequiredBranch(schemaNode) ||
				sh.isRequiredContentElement(schemaNode)) &&
				!sh.isRepeatingElement(schemaNode)) {
				labelObj.setRequired();
			}
			else if (sh.isRepeatingElement(schemaNode) && schemaNode.getMinOccurs() == 1) {
				labelObj.setRepeating();
				labelObj.setRequired();
			}
		}

		// label text  inexed if siblingPath and indexId provided, otherwise based on xpath
		labelObj.setText(getLabelText(xpath, siblingPath, indexId));

		// add best practices if there is field info available
		if (rhelper.hasFieldInfo(xpath)) {
			labelObj.bestPractices = bestPracticesLink(xpath).createCopy();
		}
		return labelObj;
	}


	/**
	 *  Hook to allow specialized renderers (in particular DleseEditorRenderer) to
	 *  suppress display of xsd:string elements in certain circumstances by
	 *  overriding this method.
	 *
	 * @param  xpath  The xpath to show
	 * @return        hardcoded to true
	 */
	public boolean showXsdStringElement(String xpath) {
		return true;
	}


	/**
	 *  Gets the renderedField attribute of the RendererImpl object
	 *
	 * @param  xpath  NOT YET DOCUMENTED
	 * @param  label  NOT YET DOCUMENTED
	 * @param  input  NOT YET DOCUMENTED
	 * @return        The renderedField value
	 */
	public Element getRenderedField(String xpath, Label label, Element input) {
		return getRenderedField(xpath, label.getElement(), input);
	}


	/**
	 *  Renders a Labelled input field (for a Simple or Built-in schema dataType.
	 *
	 * @param  label  Description of the Parameter
	 * @param  input  Description of the Parameter
	 * @param  xpath  NOT YET DOCUMENTED
	 * @return        The labelFieldTable value
	 */
	public Element getRenderedField(String xpath, Element label, Element input) {
		// prtln ("getRenderedField()\n\t" + xpath);
		Element table = df.createElement("table")
			.addAttribute("class", "input-field-table");

		Element row = table.addElement("tr")
			.addAttribute("class", "form-row");

		Element labelCell = row.addElement("td")
			.addAttribute("class", "label-cell");
		rhelper.attachToolHelp(labelCell, xpath);
		labelCell.add(label);

		Element inputCell = row.addElement("td")
			.addAttribute("class", "input-cell");
		rhelper.attachMessages(inputCell, xpath);
		inputCell.add(input);

		return table;
	}
	// -------------- end utils ---------------

	/**
	 *  Description of the Method
	 *
	 * @return    Description of the Return Value
	 */
	public String report() {
		String s = "\nRendererImpl Report";
		s += "\n\t xpath: " + xpath;
		s += "\n\t normalizedXPath: " + normalizedXPath;
		if (typeDef == null) {
			s += "\n\t typeDefName is NULL";
		}
		else {
			s += "\n\t typeName: " + typeName;
		}
		if (typeDef == null) {
			s += "\n\t typeDef is NULL";
		}
		if (schemaNode == null) {
			s += "\n\t schemaNode is NULL";
		}
		else {
			if (schemaNode.isAbstract())
				s += "\n\t schemaNode is ABSTRACT";
			if (schemaNode.isHeadElement())
				s += "\n\t schemaNode has SubstitutionGroup";
		}

		s += "\n";
		return s;
	}


	/**
	 *  Sets the debug attribute of the RendererImpl class
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
			SchemEditUtils.prtln(s, "RendererImpl");
		}
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  s  NOT YET DOCUMENTED
	 */
	private final void prtlnErr(String s) {
		System.err.println("RendererImpl: " + s);
	}

}

