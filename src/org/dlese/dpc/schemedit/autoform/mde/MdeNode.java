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
import org.dlese.dpc.schemedit.vocab.FieldInfoReader;
import org.dlese.dpc.xml.*;
import org.dlese.dpc.xml.schema.*;

import java.util.*;

import org.dom4j.Node;
import org.dom4j.Attribute;
import org.dom4j.Element;
import org.dom4j.Namespace;
import org.dom4j.DocumentFactory;

/**
 *  Base class for rendering the document structure ("nodes") in the Metadata
 *  Editor. Subclasses provide concrete <b>render</b> methods for two *modes*:
 *
 *  <ol>
 *    <li> edit_mode - in which input is accepted, and
 *    <li> display_mode - in which field values are simply displayed
 *  </ol>
 *  Subclasses implement the logic to render the various schema types (e.g.,
 *  SimpleType, ComplexType, Choice, Sequence, etc) that define each metadata
 *  editor node. <p>
 *
 *  MdeNode instances provide access to the following classes:
 *  <ul>
 *    <li> Renderers - implement the recursive process of implenting
 *    MetdataEditor Nodes, including methods to render input fields,
 *    <li> RendererHelper - provides state information for the rendering
 *    process, as well as a Factory for obtaining new renderers, and
 *    <li> SchemaHelper - provides utility methods for obtaining schema-based
 *    information.
 *  </ul>
 *
 *
 * @author    ostwald
 *
 *
 */
public abstract class MdeNode {
	/**  Description of the Field */
	private static boolean debug = true;

	/**  NOT YET DOCUMENTED */
	protected SchemaHelper sh;
	/**  NOT YET DOCUMENTED */
	protected String xpath;
	/**  NOT YET DOCUMENTED */
	protected Element parent;
	/**  NOT YET DOCUMENTED */
	protected SchemaNode schemaNode;
	/**  NOT YET DOCUMENTED */
	protected GlobalDef typeDef;
	/**  NOT YET DOCUMENTED */
	protected DocumentFactory df = DocumentFactory.getInstance();
	/**  NOT YET DOCUMENTED */
	protected RendererHelper rhelper = null;
	/**  NOT YET DOCUMENTED */
	protected String formBeanName = null;

	/**  NOT YET DOCUMENTED */
	protected String normalizedXPath;
	/**  NOT YET DOCUMENTED */
	public RendererImpl renderer;
	protected String inputHelperFile;


	/**  NOT YET DOCUMENTED */
	public abstract void render();


	
	/**
	 *  Constructor for the MdeNode object
	 *
	 * @param  renderer  NOT YET DOCUMENTED
	 */
	public MdeNode(RendererImpl renderer) {

		this.xpath = renderer.xpath;
		this.parent = renderer.parent;
		this.typeDef = renderer.typeDef;
		this.rhelper = renderer.rhelper;
		this.inputHelperFile = this.rhelper.getInputHelperFile(this.xpath);
		this.renderer = renderer;
		this.sh = rhelper.getSchemaHelper();
		this.formBeanName = rhelper.getFormBeanName();

		normalizedXPath = RendererHelper.normalizeXPath(xpath);
		schemaNode = sh.getSchemaNode(normalizedXPath);

		if (this.typeDef == null) {
			prtln("typeDef is null");
			this.typeDef = schemaNode.getTypeDef();
		}
		
/* 		if (this.inputHelperFile != null) {
			prtln ("inputHelperFile: " + this.inputHelperFile);
		} */
	}


	/**
	 *  Gets a new renderer for the specified xpath and parent element.
	 *
	 * @param  xpath   xpath for new renderer
	 * @param  parent  parent element for new renderer
	 * @return         A new renderer instance.
	 */
	public Renderer newRenderer(String xpath, Element parent) {
		return rhelper.getRenderer(xpath, parent);
	}


	/**
	 *  Gets a new renderer for the specified xpath and parent element and typeDef.
	 *
	 * @param  xpath    xpath for new renderer
	 * @param  parent   parent element for new renderer
	 * @param  typeDef  Explicitly specified typeDef for new renderer
	 * @return          A new renderer instance.
	 */
	public Renderer newRenderer(String xpath, Element parent, GlobalDef typeDef) {
		return rhelper.getRenderer(xpath, parent, typeDef);
	}

	public String getInputHelperFile () {
		return this.inputHelperFile;
	}

	/**
	 *  Gets the editMode attribute of the MdeNode object
	 *
	 * @return    The editMode value
	 */
	protected boolean isEditMode() {
		return this.renderer.isEditMode();
	}

	/**
	 *  Gets the displayMode attribute of the MdeNode object (KEEP)
	 *
	 * @return    The displayMode value
	 */
	protected boolean isDisplayMode() {
		return this.renderer.isDisplayMode();
	}

	/**
	 *  Gets the mode attribute of the MdeNode object - used for Debugging
	 *
	 * @return    The mode value
	 */
	public String getMode() {
		return this.renderer.getRenderMode();
	}


	/**
	 *  Gets a rendered inputElement for this node from the Renderer instance.
	 *
	 * @return    The inputElement value
	 */
	protected Element getInputElement() {
		return renderer.getInputElement(xpath, schemaNode, typeDef);
	}


	/**
	 *  Return a Best Practices link represented as an Element.<p>
	 *
	 *  NOTE: apparently css class ("action-button") must be applyed to both the
	 *  link and the text within to get consistent results in both IE and FireFox
	 *  ...
	 *
	 * @param  xpath  Description of the Parameter
	 * @return        Description of the Return Value
	 */
	protected Element bestPracticesLink(String xpath) {
		return renderer.bestPracticesLink(xpath);
	}

	/**
	* Gets the schema namespace (associated with "http://www.w3.org/2001/XMLSchema") 
	* for the root schema for the metadata framework for this Renderer.
	*/
	protected Namespace getInstanceSchemaNamespace() {
		return this.sh.getSchemaNamespace();
	}

	/**
	 *  Returns the instance-level prefix assigned to the given namespace. If
	 *  namesapaces are not enabled for this schema, return an empty prefix.
	 *
	 * @param  ns       NOT YET DOCUMENTED
	 * @param  typeDef  NOT YET DOCUMENTED
	 * @return          The instancePrefix value
	 */
	private String getInstancePrefix(Namespace ns, GlobalDef typeDef) {
		if (!sh.getNamespaceEnabled())
			return null;
		NamespaceRegistry namespaces = sh.getDefinitionMiner().getGlobalDefMap().getNamespaces();
		String instancePrefix = namespaces.getPrefixforUri(ns.getURI());
		if (instancePrefix.equals(""))
			instancePrefix = namespaces.getNamedDefaultNamespace().getPrefix();
		return instancePrefix;
	}


	/**
	 *  Resolve the prefix of the qualified name using the typeDef as context.<p>
	 *
	 *  Namespace prefixes in type definitions are meaningful within the local
	 *  context of the typeDef, but must be resolved to be meaningful at the
	 *  top-level of the schema (i.e., the level of the instance document). This
	 *  method determines the namespace for a locally defined prefix and returns
	 *  the prefix corresponding to that namespace at the instance document level.
	 *
	 * @param  name     NOT YET DOCUMENTED
	 * @param  typeDef  NOT YET DOCUMENTED
	 * @return          NOT YET DOCUMENTED
	 */
	private String resolveQualifiedName(String name, GlobalDef typeDef) {
		String localPrefix = NamespaceRegistry.getNamespacePrefix(name);
		Namespace localPrefixNS = typeDef.getSchemaReader().getNamespaces().getNSforPrefix(localPrefix);
		String topLevelPrefix = getInstancePrefix(localPrefixNS, typeDef);
		return NamespaceRegistry.makeQualifiedName(topLevelPrefix, NamespaceRegistry.stripNamespacePrefix(name));
	}


	/**
	 *  Gets the qualifiedName attribute of the Renderer object
	 *
	 * @param  name     NOT YET DOCUMENTED
	 * @param  typeDef  NOT YET DOCUMENTED
	 * @return          The qualifiedName value
	 */
	protected String getQualifiedElementName(String name, GlobalDef typeDef) {
		String qualifiedName = name;
		if (!NamespaceRegistry.isQualified(name)) {
			String prefix = getInstancePrefix(typeDef.getNamespace(), typeDef);
			qualifiedName = NamespaceRegistry.makeQualifiedName(prefix, name);
		}
		else {
			/*
				IMPORTANT -- don't resolve built-ins (for now), since doing so will confuse the schemaReader
				such that it won't create a BuiltIn (if the schemaPrefix local to schemaReader is not that
				of global schemaPrefix).
				This is pretty ugly, and i BELIEVE that if BuiltIns were available via the globalRefMap under
				the schemaNamespaceUri, this would no longer be a problem ... but for now, just test that
				qualifiedName doesn't refer to a built in ....
			*/
			if (!typeDef.getSchemaReader().isBuiltIn(name))
				qualifiedName = resolveQualifiedName(name, typeDef);
		}
		return qualifiedName;
	}


	/**
	 *  Gets the qualifiedAttributeName attribute of the Renderer object.<p>
	 *
	 *  NOTE: i don't think this method is required at all. if the instance
	 *  document is constructed correctly, the attributes are already qualified as
	 *  needed and there is no need to mess with it any further ..
	 *
	 * @param  name     NOT YET DOCUMENTED
	 * @param  typeDef  NOT YET DOCUMENTED
	 * @param  element  NOT YET DOCUMENTED
	 * @return          The qualifiedAttributeName value
	 */
	protected String getQualifiedAttributeName(String name, Element element, GlobalDef typeDef) {
		Namespace typeDefNamespace = typeDef.getNamespace();
		String typeDefNSPrefix = typeDefNamespace.getPrefix();
		String qualifiedName = name;

		// if the name is qualified, remove namespace prefix if it is the same as
		// the typeDef's prefix
		if (NamespaceRegistry.isQualified(name)) {

			// get the namespace corresponding to the name's prefix - in the context of provided typeDef
			qualifiedName = resolveQualifiedName(name, typeDef);

		}
		else { // no prefix supplied
			String schemaPath = SchemaHelper.toSchemaPath(xpath);
			SchemaNode parentNode = sh.getSchemaNode(schemaPath);

			// when there is no prefix, don't qualify the attribute
			qualifiedName = name;
		}

		// prtln(" ... returning " + qualifiedName);
		return qualifiedName;
	}


	/**
	 *  Renders a Labelled input field (for a Simple or Built-in schema dataType.
	 *
	 * @param  label       Description of the Parameter
	 * @param  inputField  Description of the Parameter
	 * @return             The labelFieldTable value
	 */
	protected Element getRenderedField(Label label, Element inputField) {
		return renderer.getRenderedField(xpath, label, inputField);
	}


	/**
	 *  Displaying labels for elements that have no input field
	 *
	 * @param  label  Description of the Parameter
	 * @return        The labelActionTable value
	 */
	protected Element getRenderedNoInputField(Label label) {
		return getRenderedNoInputField(label, null);
	}

	protected Element getRenderedNoInputField(Label label, Element action) {
		return getRenderedNoInputField(label, action, null);
	}
	
	/**
	 *  Creates Element for displaying labels, action controllers and
	 *  collapseWidgets for elements that have no input field. Action controllers
	 *  enable users to perform actions such as deleting a node or adding a new
	 *  one.
	 *
	 * @param  label   Description of the Parameter
	 * @param  action  Description of the Parameter
	 * @return         The formattedLabel value
	 */
	protected Element getRenderedNoInputField(Label label, Element action, Element inputHelper) {

		Element table = df.createElement("table")
			.addAttribute("class", "no-input-field-table");

		Element row = table.addElement("tr")
			.addAttribute("class", "form-row");

		Element labelCell = row.addElement("td");
		rhelper.attachToolHelp(labelCell, xpath);

		if (getLevel() > 0) {
			labelCell.addAttribute("class", "label-cell");
		}

		labelCell.add(label.getElement());

		if (action != null) {
			Element controllerCell = row.addElement("td")
				.addAttribute("class", "action-box");
			controllerCell.add(action);
		}
		
		Element prompts = df.createElement("st__fieldPrompt")
			.addAttribute("pathArg", xpath);
		
		Element cell = row.addElement("td")
			.addAttribute("class", "action-box");
		if (this.isEditMode())
			cell.add(prompts);
		if (inputHelper != null)
			cell.add(inputHelper);
		
		if (this.schemaNode.isRecursive())
			attachElementDebugInfo(table, "RECURSIVE", "blue");
		
		return table;
	}


	/**
	 *  Gets the level of the current node relative to the <b>baseLevel</b> . the
	 *  level is a measure of the node's depth in the schema hierarchy.
	 *
	 * @return    The level value
	 */
	final int getLevel() {
		return renderer.getLevel(xpath);
	}


	/**
	 *  Gets the div attribute of the MdeNode object
	 *
	 * @return    The div value
	 */
	protected Element getDiv() {
		int level = getLevel();
		return df.createElement("div").addAttribute("class", "level-" + Integer.toString(level));
	}


	/**
	 *  Gets the div attribute of the MdeNode object
	 *
	 * @param  level  Description of the Parameter
	 * @return        The div value
	 */
	protected Element getDiv(int level) {
		return df.createElement("div").addAttribute("class", "level-" + Integer.toString(level));
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
	protected String getToolHelp(String s) {
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
	 *  Embed a debugging string in the provided element that can be viewed in HTML
	 *  source.
	 *
	 * @param  e  NOT YET DOCUMENTED
	 * @param  s  NOT YET DOCUMENTED
	 */
	protected void embedDebugInfo(Element e, String s) {
		rhelper.embedDebugInfo(e, s);
	}


	/**
	 *  Attach a debugging message to this element that can be viewed in the
	 *  editing form.
	 *
	 * @param  e  NOT YET DOCUMENTED
	 * @param  s  NOT YET DOCUMENTED
	 */
	protected void attachElementDebugInfo(Element e, String s) {
		attachElementDebugInfo(e, s, null);
	}


	/**
	 *  Attaches debugging info to a rendered element in the editor, optionally
	 *  including a border to outline the element. The borderSpec is a string that
	 *  specifies the border in css: e.g., "thin blue solid"
	 *
	 * @param  target       The Element to which the debugging element is attached
	 * @param  displayText  The debugging message
	 * @param  borderColor  NOT YET DOCUMENTED
	 */
	protected void attachElementDebugInfo(Element target, String displayText, String borderColor) {
		if (rhelper.showElementDebugInfo)
			rhelper.attachElementDebugInfo(target, displayText, borderColor);
	}


	/**
	 *  Description of the Method
	 *
	 * @param  e           Description of the Parameter
	 * @param  s           Description of the Parameter
	 * @param  borderSpec  NOT YET DOCUMENTED
	 */
	protected void attachLabelDebugInfo(Element e, String s, String borderSpec) {
		if (rhelper.showLabelDebugInfo) {
			Element info = df.createElement("div")
				.addAttribute("class", "label-debug-info");
			String debugInfo = s;
			info.addText(debugInfo);
			e.add(info);
			if (borderSpec != null)
				e.addAttribute("style", "border__" + borderSpec);
		}
	}


	/**
	 *  Description of the Method
	 *
	 * @param  e  Description of the Parameter
	 * @param  s  Description of the Parameter
	 */
	protected void attachLabelDebugInfo(Element e, String s) {
		attachLabelDebugInfo(e, s, null);
	}


	/**
	 *  Insert an HTML comment into the html source.
	 *
	 * @param  e  NOT YET DOCUMENTED
	 * @param  s  NOT YET DOCUMENTED
	 */
	protected void insertHtmlComment(Element e, String s) {
		prtln("\ninsertHtmlComment()");
		Element comment = df.createElement("st__htmlComment");
		comment.setText(s);
		Element parent = e.getParent();

		if (parent != null) {
			List children = parent.elements();
			int index = children.indexOf(e);
			children.add(index, comment);
		}
		else {
			// prtlnErr("PARENT NOT FOUND");
		}
	}


	/**
	 *  Adds a feature to the HtmlComment attribute of the MdeNode object
	 *
	 * @param  e  The feature to be added to the HtmlComment attribute
	 * @param  s  The feature to be added to the HtmlComment attribute
	 */
	protected void addHtmlComment(Element e, String s) {
		prtln("\ninsertHtmlComment()");
		Element comment = df.createElement("st__htmlComment");
		comment.setText(s);
		e.add(comment);
	}


	/**
	 *  attach debuging information to a label object that is displayed in the
	 *  editor
	 *
	 * @param  labelObj  NOT YET DOCUMENTED
	 * @param  s         NOT YET DOCUMENTED
	 */
	protected void attachLabelDebugInfo(Label labelObj, String s) {
		if (rhelper.showLabelDebugInfo) {
			labelObj.setDebugInfo(s);
		}
	}


	/**
	 *  Add an "id" attribute to the provided element, with a value derived from
	 *  this node's "xpath".
	 *
	 * @param  e  NOT YET DOCUMENTED
	 */
	protected void attachElementId(Element e) {
		// define javascript id for this node based on xpath
		// ISSUE: is this ever actually used??
		e.addElement("c__set")
			.addAttribute("var", "elementPath")
			.addAttribute("value", xpath);
		e.addElement("c__set")
			.addAttribute("var", "id")
			.addAttribute("value", "${sf__pathToId(elementPath)}");
	}


	/**
	 *  Creates element representing a mousable field label that will open or close
	 *  its contents. Used to expand and collapse the hierarchical strucuture of
	 *  the xml document
	 *
	 * @param  baseDiv  Description of the Parameter
	 */
	/*
	<%-- define variable to hold the key/id for this element --%>
	<c:set var="id" value="${sf:pathToId('/news-oppsRecord/announcements')}" scope="page" />
	<%-- use jsp:setProperty to set the id prop of collapseBean prior to accessing rhelper --%>
	<jsp:setProperty name="collapseBean" property="id" value="${id}" />
	<%-- set hidden var to currentState --%>
	<input type="hidden" name="${id}_displayState" value="${"+formBeanName+".collapseBean.displayState}" />
	*/
	protected void insertDisplaySetup(Element baseDiv) {
		if (isEditMode()) {
			Element elementPath = df.createElement("c__set")
				.addAttribute("var", "elementPath")
				.addAttribute("value", xpath);
			baseDiv.add(elementPath);

			Element setId = df.createElement("c__set")
				.addAttribute("var", "id")
				.addAttribute("value", "${sf__pathToId(elementPath)}");
			baseDiv.add(setId);

			Element setProp = df.createElement("jsp__setProperty")
				.addAttribute("name", "collapseBean")
				.addAttribute("property", "id")
				.addAttribute("value", "${id}");
			baseDiv.add(setProp);

			Element hiddenVar = df.createElement("input")
				.addAttribute("type", "hidden")
				.addAttribute("id", "${id}_displayState")
				.addAttribute("name", "${id}_displayState")
				.addAttribute("value", "${" + formBeanName + ".collapseBean.displayState}");
			baseDiv.add(hiddenVar);
		}
	}


	/**
	 *  Description of the Method
	 *
	 * @param  s  Description of the Parameter
	 */
	protected void prtln(String s) {
		if (debug)
			SchemEditUtils.prtln(s, "MdeNode");
	}


	/**
	 *  Sets the debug attribute of the MdeNode class
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
	private final void prtlnErr(String s) {
		System.err.println("MdeNode: " + s);
	}

}

