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

import org.dlese.dpc.schemedit.MetaDataFramework;
import org.dlese.dpc.schemedit.SchemEditUtils;
import org.dlese.dpc.schemedit.vocab.*;
import org.dlese.dpc.schemedit.vocab.layout.VocabLayoutConfig;
import org.dlese.dpc.schemedit.config.SchemaPath;
import java.util.*;
import java.util.regex.*;
import org.dlese.dpc.xml.schema.*;
import org.dlese.dpc.util.strings.FindAndReplace;

import org.dlese.dpc.xml.XPathUtils;
import org.dom4j.Node;
import org.dom4j.Attribute;
import org.dom4j.Element;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;

/**
 *  Manages global rhelper information for a Renderer Instance as well as
 *  providing a renderer factory for instantiating new renderer objects.<p>
 *
 *  See {@link org.dlese.dpc.schemedit.autoform.Renderer} and it's concrete
 *  subclasses.
 *
 * @author    ostwald <p>
 *
 *
 */
public class RendererHelper {
	private static boolean debug = true;

	public final static int DEFAULT_TEXT_INPUT_SIZE = 50;
	private int baseLevel = 0;
	private Element root = null;
	private SchemaHelper schemaHelper = null;
	private MetaDataFramework framework = null;
	private String rendererClassName = null;
	private String formBeanName = "sef";
	private List renderers = new ArrayList();

	/**
	 *  Causes debugging information about each element to be shown in editor
	 */
	public boolean showElementDebugInfo = false;
	/**
	 *  Causes debugging information about the element labels to be shown in editor
	 */
	public boolean showLabelDebugInfo = false;

	/**
	 *  Keeps track of number of Renderer instantiations to guard against infinite
	 *  loops
	 */
	public int counter = 0;


	/**
	 *  Constructor for the RendererHelper object with renderer class name
	 *  explicitly specified. Used by View Renders, such as {@link EditorViewerRenderer},
	 *  for which the renderer cannot be obtained from the MetaDataFramework.
	 *
	 * @param  root          Description of the Parameter
	 * @param  framework     Description of the Parameter
	 * @param  formBeanName  Description of the Parameter
	 * @param  renderer      Description of the Parameter
	 */
	public RendererHelper(Element root,
	                      MetaDataFramework framework,
	                      String formBeanName,
	                      String renderer) {
		this(root, framework, formBeanName);
		this.rendererClassName = renderer;
		prtln("rendererClassName: " + rendererClassName);
	}


	/**
	 *  Constructor for the RendererHelper object
	 *
	 * @param  root          root {@link org.dom4j.Element} to be rendered
	 * @param  framework     MetaDataFramework representing the XML Schema to be
	 *      used
	 * @param  formBeanName  Description of the Parameter
	 */
	public RendererHelper(Element root, MetaDataFramework framework, String formBeanName) {
		this.root = root;
		this.formBeanName = formBeanName;
		this.rendererClassName = framework.getRenderer();
		this.framework = framework;
		this.baseLevel = framework.getBaseRenderLevel();
		this.schemaHelper = framework.getSchemaHelper();
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  target       NOT YET DOCUMENTED
	 * @param  displayText  NOT YET DOCUMENTED
	 * @param  borderColor  NOT YET DOCUMENTED
	 */
	public void attachElementDebugInfo(Element target, String displayText, String borderColor) {
		if (showElementDebugInfo) {
			Element info = DocumentHelper.createElement("div")
				.addAttribute("class", "element-debug-info");
			// s + "  (" + getLevel() + ")";
			info.addText(displayText);
			target.add(info);
			if (borderColor != null)
				target.addAttribute("style", "border__thin dashed " + borderColor);
		}
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  e  NOT YET DOCUMENTED
	 * @param  s  NOT YET DOCUMENTED
	 */
	public void embedDebugInfo(Element e, String s) {
		if (showElementDebugInfo) {
			e.addAttribute("debug-info", s);
		}
	}


	/**
	 *  Renderer factory returns a Renderer instance for the schemaNode at xpath.
	 *
	 * @param  xpath   xpath for node to be rendered
	 * @param  parent  parent element to which to attach rendered node
	 * @return         The renderer value
	 */
	public final RendererImpl getRenderer(String xpath, Element parent) {
		return getRenderer(xpath, parent, null);
	}


	/**
	 *  Renderer factory returns a Renderer instance for the schemaNode at xpath,
	 *  possibly with a "proxy dataType" (if provided) that will override the
	 *  dataType associated with the schemaNode.
	 *
	 * @param  xpath    xpath for node to be rendered
	 * @param  parent   parent element to which to attach rendered node
	 * @param  typeDef  NOT YET DOCUMENTED
	 * @return          The renderer value
	 */
	public final RendererImpl getRenderer(String xpath, Element parent, GlobalDef typeDef) {

		String className = this.rendererClassName;

		// A 'factory' that dynamically loads the appropriate DocReader...
		try {
			Class rendererClass = Class.forName("org.dlese.dpc.schemedit.autoform." + className);
			RendererImpl renderer = (RendererImpl) rendererClass.newInstance();

			if (typeDef != null) {
				renderer.doInit(xpath, parent, typeDef, this);
			}
			else {
				renderer.doInit(xpath, parent, this);
			}
			renderers.add(renderer);
			return renderer;
		} catch (Throwable e) {
			System.err.println("Error loading renderer class '" + className + "'. " + e);
			// e.printStackTrace();
			return null;
		}

	}


	/**
	 *  Removes any and all indexing information from an XPath. In particular,
	 *  cleans up any JSP-encoding, including curly bracketed indexing (e.g.,
	 *  ${indexId+1}). This form of XPath is necessary for accessing schemaNodes,
	 *  which contain no indexing.
	 *
	 * @param  encodedXPpath  Description of the Parameter
	 * @return                Xpath duitable for finding a SchemaNode
	 */
	public static String normalizeXPath(String encodedXPpath) {
		// first get rid of any curly brackets and anything between them
		Pattern p = Pattern.compile("\\$\\{.+?\\}");
		Matcher m = p.matcher(encodedXPpath);
		String xpath = m.replaceAll("1");

		String d = XPathUtils.decodeXPath(xpath);
		String ret = XPathUtils.normalizeXPath(d);

		return ret;
	}


	/**
	 *  Gets the rendererClassName attribute of the RendererHelper object
	 *
	 * @return    The rendererClassName value
	 */
	public String getRendererClassName() {
		return rendererClassName;
	}


	/**
	 *  Gets the maxLen attribute of the RendererHelper object
	 *
	 * @param  xpath  NOT YET DOCUMENTED
	 * @return        The maxLen value
	 */
	public int getMaxLen(String xpath) {
		SchemaPath configPath = framework.getSchemaPathMap().getPathByPath(normalizeXPath(xpath));
		if (configPath != null && configPath.maxLen > 0)
			return configPath.maxLen;
		else
			return DEFAULT_TEXT_INPUT_SIZE;
	}


	/**
	 *  Gets the formBeanName attribute of the RendererHelper object
	 *
	 * @return    The formBeanName value
	 */
	public String getFormBeanName() {
		return formBeanName;
	}


	/**
	 *  Sets the formBeanName attribute of the RendererHelper object
	 *
	 * @param  beanName  The new formBeanName value
	 */
	public void setFormBeanName(String beanName) {
		formBeanName = beanName;
	}


	/**
	 *  Gets the schemaHelper attribute of the RendererHelper object
	 *
	 * @return    The schemaHelper value
	 */
	public SchemaHelper getSchemaHelper() {
		return schemaHelper;
	}


	/**
	 *  Gets the framework attribute of the RendererHelper object
	 *
	 * @return    The framework value
	 */
	public MetaDataFramework getFramework() {
		return framework;
	}


	/**
	 *  Gets the root attribute of the RendererHelper object
	 *
	 * @return    The root value
	 */
	public Element getRoot() {
		return root;
	}


	/**
	 *  Return if there is Mui Grouping Information available for this field. Mui
	 *  Group Information is specified in the groups files of the MetadataVocab
	 *  object, and stored in the MetaDataFramework object.
	 *
	 * @param  xpath  NOT YET DOCUMENTED
	 * @return        NOT YET DOCUMENTED
	 */
	public boolean hasMuiGroupInfo(String xpath) {
		// have to first get rid of curly braces and then we can decode, etc
		String key = RendererHelper.normalizeXPath(xpath);

		return framework.getMuiGroups().contains(key);
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  xpath  NOT YET DOCUMENTED
	 * @return        NOT YET DOCUMENTED
	 */
	public boolean hasVocabLayout(String xpath) {
		String key = RendererHelper.normalizeXPath(xpath);
		VocabLayoutConfig vocabLayouts = framework.getVocabLayouts();
		if (vocabLayouts != null)
			return vocabLayouts.hasVocabLayout(key);
		return false;
	}


	/**
	 *  Description of the Method
	 *
	 * @param  xpath  Description of the Parameter
	 * @return        Description of the Return Value
	 */
	public boolean hasFieldInfo(String xpath) {
		String key = RendererHelper.normalizeXPath(xpath);
		FieldInfoMap fieldInfoMap = this.framework.getFieldInfoMap();
		return fieldInfoMap.hasFieldInfo(key);
	}


	/**
	 *  returns the FieldInfoReader for the given xpath, or null if one does not
	 *  exist
	 *
	 * @param  xpath  Description of the Parameter
	 * @return        The fieldInfo value
	 */
	public FieldInfoReader getFieldInfo(String xpath) {
		String key = RendererHelper.normalizeXPath(xpath);
		FieldInfoMap fieldInfoMap = this.framework.getFieldInfoMap();
		// prtln ("  ... getFieldInfo looking for key: " + key);
		return fieldInfoMap.getFieldInfo(key);
	}


	/**
	 *  Returns true if the provided path is configured with an inputHelper
	 *
	 * @param  xpath  NOT YET DOCUMENTED
	 * @return        NOT YET DOCUMENTED
	 */
	public boolean hasInputHelper(String xpath) {
		return (getInputHelperFile(xpath) != null);
	}


	/**
	 *  Returns the configured inputHelper value for the provided xpath
	 *  (inputHelper value is relative to the /editor/input_helper directory in the
	 *  deployed app
	 *
	 * @param  xpath  NOT YET DOCUMENTED
	 * @return        The inputHelperFile value
	 */
	public String getInputHelperFile(String xpath) {
		// prtln("getInputHelperFile() with " + xpath);
		if (xpath != null) {
			String normXpath = RendererHelper.normalizeXPath(xpath);
			SchemaPath schemaPath = this.getFramework().getSchemaPathMap().getPathByPath(normXpath);
			if (schemaPath != null) {
				String inputHelperFile = schemaPath.inputHelper;
				if (inputHelperFile != null && inputHelperFile.trim().length() > 0)
					return inputHelperFile;
			}
		}
		return null;
	}


	/**
	 *  Gets the baseLevel attribute of the RendererHelper object. The baseLevel
	 *  determines at which level in the schema we are rendering for the individual
	 *  pages.
	 *
	 * @return    The baseLevel value
	 */
	public int getBaseLevel() {
		return baseLevel;
	}

	// ------------- static utilities ----------

	/**
	 *  Given a "siblingPath" (xpath with no indexing on leaf node), create a
	 *  javascript variable to use for indexed elements. Replace colons in index id
	 *  with underscore to avoid javascript problems with qualified names. Then add
	 *  a string ("Index") and indexId index, if required, to avoid name
	 *  collisions.
	 *
	 * @param  xpath  NOT YET DOCUMENTED
	 * @return        NOT YET DOCUMENTED
	 */
	public final static String encodeIndexId(String xpath) {
		String leaf = XPathUtils.getNodeName(xpath);
		String root = FindAndReplace.replace(leaf, ":", "", false) + "Index";
		Pattern p = Pattern.compile(leaf + "_\\$\\{" + root + "([0-9]*)");
		boolean found = false;
		Matcher m = p.matcher(xpath);
		int lastIndex = 0;
		while (m.find()) {
			found = true;
			try {
				lastIndex = Integer.parseInt(m.group(1));
			} catch (java.lang.NumberFormatException e) {}
		}
		return root + (found ? String.valueOf(lastIndex + 1) : "");
	}



	/**
	 *  returns the input string surrounded by a string that is replaced with a
	 *  quotation mark when the rendered Element is writen to disk. This is
	 *  necessary because dom4J turns quotation marks and appostrophies into
	 *  entities, which then cause the JSP processer to barf when the jsp page is
	 *  rendered
	 *
	 * @param  s  Description of the Parameter
	 * @return    Description of the Return Value
	 */
	public final static String jspQuotedString(String s) {
		return "^v^" + s + "^v^";
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  page  NOT YET DOCUMENTED
	 * @return       NOT YET DOCUMENTED
	 */
	public static Element jspInclude(String page) {
		return DocumentHelper.createElement("jsp__include")
			.addAttribute("page", page);
	}

	// ---------- predicates

	/**
	 *  Creates a test for the existance of the parentNode of the node being
	 *  rendered
	 *
	 * @param  xpath  NOT YET DOCUMENTED
	 * @return        An Element representing the test for the existance of a
	 *      parent node
	 */
	public Element parentNodeExistsTest(String xpath) {
		Element tester = DocumentHelper.createElement("logic__equal")
			.addAttribute("name", formBeanName)
			.addAttribute("property", "parentNodeExists(" + xpath + ")")
			.addAttribute("value", "true");
		return tester;
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  xpath  NOT YET DOCUMENTED
	 * @return        NOT YET DOCUMENTED
	 */
	public Element parentNodeNotExistsTest(String xpath) {
		Element tester = DocumentHelper.createElement("logic__equal")
			.addAttribute("name", formBeanName)
			.addAttribute("property", "parentNodeExists(" + xpath + ")")
			.addAttribute("value", "false");
		return tester;
	}


	/**
	 *  Creates a test for the existence of a node in the instance document at the
	 *  given path
	 *
	 * @param  path  Description of the Parameter
	 * @return       Element that performs test when embedded in JSP
	 */
	public Element nodeExistsTest(String path) {
		Element tester = DocumentHelper.createElement("logic__equal")
			.addAttribute("name", formBeanName)
			.addAttribute("property", "nodeExists(" + path + ")")
			.addAttribute("value", "true");
		return tester;
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  path  NOT YET DOCUMENTED
	 * @return       NOT YET DOCUMENTED
	 */
	public Element nodeHasChildrenTest(String path) {
		Element tester = DocumentHelper.createElement("logic__greaterThan")
			.addAttribute("name", formBeanName)
			.addAttribute("property", "childElementCountOf(" + path + ")")
			.addAttribute("value", "0");
		return tester;
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  path  NOT YET DOCUMENTED
	 * @return       NOT YET DOCUMENTED
	 */
	public Element nodeHasMembersTest(String path) {
		Element tester = DocumentHelper.createElement("logic__greaterThan")
			.addAttribute("name", formBeanName)
			.addAttribute("property", "memberCountOf(" + path + ")")
			.addAttribute("value", "0");
		return tester;
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  path  NOT YET DOCUMENTED
	 * @return       NOT YET DOCUMENTED
	 */
	public Element nodeHasNoMembersTest(String path) {
		Element tester = DocumentHelper.createElement("logic__equal")
			.addAttribute("name", formBeanName)
			.addAttribute("property", "memberCountOf(" + path + ")")
			.addAttribute("value", "0");
		return tester;
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  path  NOT YET DOCUMENTED
	 * @return       NOT YET DOCUMENTED
	 */
	public Element acceptsNewSiblingTest(String path) {
		Element tester = DocumentHelper.createElement("logic__equal")
			.addAttribute("name", formBeanName)
			.addAttribute("property", "acceptsNewSibling(" + path + ")")
			.addAttribute("value", "true");
		return tester;
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  path  NOT YET DOCUMENTED
	 * @return       NOT YET DOCUMENTED
	 */
	public Element acceptsNewChioceTest(String path) {
		Element tester = DocumentHelper.createElement("logic__equal")
			.addAttribute("name", formBeanName)
			.addAttribute("property", "acceptsNewChoice(" + path + ")")
			.addAttribute("value", "true");
		return tester;
	}


	/**
	 *  Creates an element that tests for the NON-existence of an instance document
	 *  node at the given path
	 *
	 * @param  path  Description of the Parameter
	 * @return       Element that performs test when embedded in JSP
	 */
	public Element nodeNotExistsTest(String path) {
		Element tester = DocumentHelper.createElement("logic__notEqual")
			.addAttribute("name", formBeanName)
			.addAttribute("property", "nodeExists(" + path + ")")
			.addAttribute("value", "true");
		return tester;
	}


	/**
	 *  Used by the viewing classes (not the editing classes) to test that a node
	 *  exists and has content.<p>
	 *
	 *  NOTE: should this method be ABSTRACT here, and only instantiated in DcsView
	 *  utils?? (it must be at least declared here, since it is referenced from the
	 *  renderer, which has obtained utils as RenderUtils.
	 *
	 * @param  xpath  NOT YET DOCUMENTED
	 * @return        NOT YET DOCUMENTED
	 */
	public Element viewNodeTest(String xpath) {
		// test to see if child node (the multiSelect) is empty

		Element test = DocumentHelper.createElement("logic__equal")
			.addAttribute("name", formBeanName)
			.addAttribute("property", "viewNode(" + xpath + ")")
			.addAttribute("value", "true");
		return test;
	}


	// -------- end preds

	// -------------- Controls -------------------

	/**
	 *  Description of the Method
	 *
	 * @param  container  Description of the Parameter
	 * @param  xpath      NOT YET DOCUMENTED
	 */
	public void attachMessages(Element container, String xpath) {
		/*
		    propertyId is determined by the type of input element this xpath is associated with.
		    most elements will use "valueOf", but multiSelectes require "enumerationValuesOf"
		  */
		SchemaNode schemaNode = schemaHelper.getSchemaNode(normalizeXPath(xpath));
		if (schemaNode == null) {
			prtln("WARNING: attache messages did not find schemaNode for " + xpath);
			return;
		}
		String propertyIdPrefix = "valueOf";
		if (schemaHelper.isMultiSelect(schemaNode)) {
			propertyIdPrefix = "enumerationValuesOf";
		}
		else if (schemaNode.getTypeDef().isAnyType()) {
			propertyIdPrefix = "anyTypeValueOf";
		}
		String propertyId = propertyIdPrefix + "(" + xpath + ")";

		Element elementMessagesTag = container.addElement("st__elementMessages")
			.addAttribute("propertyId", propertyId);
	}


	/**
	 *  Attach a pop-up tool help message to an element
	 *
	 * @param  e        The element to recieve the tool help
	 * @param  helpMsg  Description of the Parameter
	 */
	public void attachToolHelp(Element e, String helpMsg) {
		if (helpMsg != null && helpMsg.trim().length() > 0) {
			e.addAttribute("title", getToolHelp(helpMsg.trim()));
		}
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
		FieldInfoReader fieldInfoReader = getFieldInfo(s);
		if (fieldInfoReader != null) {
			try {
				return fieldInfoReader.getDefinition();
			} catch (Exception e) {
				prtln("getToolHelp: unable to obtain definition for " + s);
			}
		}
		return "${sf:decodePath(" + jspQuotedString(s) + ")}";
	}


	/**
	 *  Sets the logging attribute of the RendererHelper class
	 *
	 * @param  verbose  The new logging value
	 */
	public static void setLogging(boolean verbose) {
		try {
			RendererHelper.setDebug(verbose);
			org.dlese.dpc.schemedit.autoform.mde.MdeNode.setDebug(verbose);
			org.dlese.dpc.schemedit.autoform.mde.MdeAny.setDebug(verbose);
			org.dlese.dpc.schemedit.autoform.mde.MdeAttribute.setDebug(verbose);
			org.dlese.dpc.schemedit.autoform.mde.MdeSimpleType.setDebug(verbose);
			org.dlese.dpc.schemedit.autoform.mde.MdeComplexType.setDebug(verbose);
			org.dlese.dpc.schemedit.autoform.mde.MdeRepeatingComplexType.setDebug(verbose);
			org.dlese.dpc.schemedit.autoform.mde.MdeRepeatingSimpleType.setDebug(verbose);
			org.dlese.dpc.schemedit.autoform.mde.MdeRepeatingAnyType.setDebug(verbose);
			org.dlese.dpc.schemedit.autoform.mde.MdeSequence.setDebug(verbose);
			org.dlese.dpc.schemedit.autoform.mde.MdeChoice.setDebug(verbose);
			org.dlese.dpc.schemedit.autoform.mde.MdeMultiChoice.setDebug(verbose);
			org.dlese.dpc.schemedit.autoform.mde.MdeDerivedTextOnlyModel.setDebug(verbose);
			org.dlese.dpc.schemedit.autoform.mde.MdeDerivedContentModel.setDebug(verbose);
			org.dlese.dpc.schemedit.autoform.mde.MdeRepeatingSubstitutionGroup.setDebug(verbose);
			org.dlese.dpc.schemedit.autoform.mde.MdeStdsNode.setDebug(verbose);
			org.dlese.dpc.schemedit.autoform.mde.MdeModelGroup.setDebug(verbose);

			org.dlese.dpc.schemedit.autoform.RendererHelper.setDebug(verbose);
			org.dlese.dpc.schemedit.autoform.Renderer.setDebug(verbose);
			org.dlese.dpc.schemedit.autoform.RendererImpl.setDebug(verbose);
			org.dlese.dpc.schemedit.autoform.ViewerRenderer.setDebug(verbose);
			org.dlese.dpc.schemedit.autoform.EditorRenderer.setDebug(verbose);
			org.dlese.dpc.schemedit.autoform.DleseEditorRenderer.setDebug(verbose);
			org.dlese.dpc.schemedit.autoform.SIFEditorRenderer.setDebug(verbose);
		} catch (Throwable t) {
			prtln("setLogging ERROR: " + t.getMessage());
		}
		prtln("logging set to " + verbose);
	}


	/**  NOT YET DOCUMENTED */
	public void destroy() {
		prtln("destroying " + this.framework.getXmlFormat() + " renderers ...");
		for (Iterator i = renderers.iterator(); i.hasNext(); ) {
			RendererImpl renderer = (RendererImpl) i.next();
			String xpath = renderer.getXpath();
			renderer = null;
			// prtln ("\t" + xpath);
		}
	}


	/**
	 *  Sets the debug attribute of the RendererHelper class
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
			// System.out.println(s);
			SchemEditUtils.prtln(s, "RendererHelper");
		}
	}
}

