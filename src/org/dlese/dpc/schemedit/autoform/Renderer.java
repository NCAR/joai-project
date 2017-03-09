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

import org.dlese.dpc.xml.schema.GlobalDef;
import org.dlese.dpc.xml.schema.ComplexType;
import org.dlese.dpc.xml.schema.SchemaNode;
import org.dlese.dpc.xml.schema.SchemaHelper;

import java.util.*;

import org.dom4j.Node;
import org.dom4j.Attribute;
import org.dom4j.Element;
import org.dom4j.Namespace;
import org.dom4j.DocumentFactory;


/**
 *  Abstract class for rendering an editor for a SchemaNode (an Element or Attribute defined in an XML
 *  Schema). Editor pages are Rendered via recursive instantiations of Renderers starting with the base
 *  element for that page. <p>
 *
 *  The product is a <b>RenderTree</b> is a {@link org.dom4j.Element} structure that expresses a metadata
 *  editor interface. The RenderTree is almost leagal jsp but must still be slightly massaged before it can be
 *  processed as jsp (see AutoForm).<p>
 *
 *  All concrete classes render JSP-based editors, but in the future we may want to support other editors.
 *  This class contains no JSP-specific methods, but it does enforce a particular style of rendering in which
 *  individual schema nodes are rendered and attached to a <b>parent</b> element. The Renderer is called
 *  recursively as it traverses the schemaStructure, starting at the <b> root</b> Element of the schema (a
 *  {@link SchemaNode}) and building the RenderTree as it traverses the schema structure.
 *
 * @author     ostwald
 *
 */
public abstract class Renderer {
	private static boolean debug = true;

	/**
	 *  A {@link SchemaHelper} instance that provides schema-related information and services to the Renderer.
	 */
	public SchemaHelper sh;
	/**  JSP - encoded xpath to the element being rendered. */
	public String xpath;
	/**  A {@link org.dom4j.Element} representing the parent of the node being rendered. */
	public Element parent;
	/**
	 *  The {@link SchemaNode} which wraps the schema element (which may represent an Element Attribute) being
	 *  rendered
	 */
	public SchemaNode schemaNode;
	/**
	 *  The XPath corresponding to the SchemaNode. This path contains no indexing or jsp-encoding since it points
	 *  to a the schema's instanceDocument.
	 */

	public GlobalDef typeDef;
	/**  A {@link org.dom4j.DocumentFactory} for creating dom Nodes. */
	 
	/**  Data structure containing global information required by individual Renderer instances. */
	public RendererHelper rhelper = null;
	 
	protected String normalizedXPath;
	/**
	 *  Name of the element-to-be-rendered's DataType. This field is required because in some cases the element
	 *  is rendered as a different type from which it is defined.
	 */
	protected String typeName;
	/**  The DataType of the element to be rendered. */

	protected DocumentFactory df;

	/**  The root element to be Rendered. Stored in the RendererHelper instance. */
	protected Element root;
	/**
	 *  Place holder for a path to a repeating child. This field is null for elements that do not contain a
	 *  repeating child.
	 */
	protected String repeatingComplexSingletonChildPath = null;

	/**  Description of the Field */
	protected String formBeanName = null;
	
	// protected RendererUtils utils;


	/**  Constructor for the Renderer object */
	public Renderer() { }


	/**
	 *  Constructor for the Renderer object
	 *
	 * @param  xpath          xpath to the Node to be rendered
	 * @param  parent         The element to which the rendered Node is attached as a child
	 * @param  rhelper          Pointer to the RendererHelper
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	protected void doInit(String xpath, Element parent, RendererHelper rhelper) throws Exception {
		init(xpath, parent, rhelper);

		if (schemaNode == null) {
			typeName = "";
			typeDef = null;
			throw new Exception("Renderer can't find schemaNode for " + normalizedXPath);
		}
		else {
			typeDef = schemaNode.getTypeDef();
			typeName = typeDef.getQualifiedName();
		}
		if (schemaNode.isHeadElement())
			prtln(report() + "\n");
		
	}

	/**
	 *  Constructor for the Renderer object in which the dataType to render is specified (rather than being
	 *  determined by the schemaNode at <b>xpath </b> . This constructor is used to render schema nodes that have
	 *  a derivedModel, which must be rendered using the dataType they extend.
	 *
	 * @param  xpath          xpath to the Node to be rendered
	 * @param  parent         The element to which the rendered Node is attached as a child
	 * @param  dataTypeName   The name of the dataType as which the node is rendered
	 * @param  rhelper          Pointer to the RendererHelper
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	protected void doInit(String xpath, Element parent, GlobalDef typeDef, RendererHelper rhelper) throws Exception {
		init(xpath, parent, rhelper);

		// here is where we explicitly set the type for this Renderer
		this.typeDef = typeDef;
		this.typeName = typeDef.getQualifiedName();
	}
	
	/**
	 *  Initialize Renderer attributes
	 *
	 * @param  xpath   The jsp-encoded xpath to the element to be rendered.
	 * @param  parent  The {@link SchemaNode} which wraps the schema element (which may represent an Element
	 *      Attribute) being rendered
	 * @param  rhelper   Data structure containing global information required by individual Renderer instances
	 */
	protected void init(String xpath, Element parent, RendererHelper rhelper) {
		this.xpath = xpath;
		this.parent = parent;
		this.rhelper = rhelper;
		this.sh = rhelper.getSchemaHelper();
		this.root = rhelper.getRoot();
		this.formBeanName = rhelper.getFormBeanName();

		df = DocumentFactory.getInstance();
		normalizedXPath = RendererHelper.normalizeXPath(xpath);
		schemaNode = sh.getSchemaNode(normalizedXPath);

		// guard against infinite loops
		// prtln("*** State.counter is " + rhelper.counter + ") ***");
		int maxCount = 2000; // 2000
		if (++rhelper.counter > maxCount) {
			System.out.println("\n*** Renderer forcing Sytem Exit (counter is " + rhelper.counter + ") ***");
			System.exit(rhelper.counter);
		}
	}
	
	public abstract boolean isDisplayMode();
	public abstract boolean isEditMode();
	public abstract void setRenderMode(String mode);
	public abstract String getRenderMode();
	
	/**
	 *  Render an editor for the schemaNode (i.e., a Node of the schema's instanceDocument) specified by the <b>
	 *  xpath</b> field.
	 */
	public void renderNode() {
		if (getLevel(xpath) < 3) {
			prtln ("\n-----------------------------------------------");
			prtln("renderNode() with " + xpath);
			prtln  ("\t level: " + getLevel(xpath));
			prtln ("\t typeName: " + typeName);
		}
		else
			prtln ("\n");
		
 		if (schemaNode.isRecursive()) {
			/* 
			IMPORTANT to set the text of the parent node to empty string so that
			the jsp rendered is <div></div>, rather than <div/>, which is NOT processed
			as a closed element by browsers and leads to nasty layout problems.
			*/
			parent.setText ("");
			return;
		}

		if (sh.isRepeatingElement(schemaNode, typeDef)) {
			prtln("\t isRepeatingElement");
			if (schemaNode.isHeadElement())
				renderRepeatingSubstitutionGroup();
			else {
				prtln ("calling renderRepeatingElement");
				renderRepeatingElement();
			}
			return;
		}
		
		if (typeDef.isBuiltIn() || typeDef.isSimpleType()) {
			prtln ("\t isSimpleType");
			renderSimpleType();
			return;
		}
		if (typeDef.isComplexType()) {
			prtln ("\t isComplexType");
			renderComplexType();
			return;
		}

		prtlnErr("renderNode() unknown dataType for " + xpath + " (" + typeName + ")");
		return;
	}


	/**  Render an editor for a schemaNode defined as {@link SimpleType} */
	public void renderSimpleType() {

		if (schemaNode.isAttribute()) {
			renderAttribute();
			return;
		}

		renderSimpleTypeConcrete();
	}


	/**  Implementation-specific method to render an editor for a schemaNode defined as {@link SimpleType} */
	public abstract void renderSimpleTypeConcrete();


	/**  Render an editor for a schemaNode defined as {@link ComplexType} */
	public void renderComplexType() {
		// prtln ("renderComplexType()  level: " + getLevel(xpath));
		ComplexType cType = (ComplexType) typeDef;
		
		// derivedModels (simpleContent and complexContent) are rendered by their own methods
		if (cType.hasSimpleContent()) {
			// prtln ("\t hasSimpleContent");
			renderDerivedTextOnlyModel();
			return;
		}
		if (cType.hasComplexContent()) {
			// prtln ("\t hasComplexContent");
			renderDerivedContentModel();
			return;
		}

		// prtln ("\t calling renderComplexTypeConcrete");
		renderComplexTypeConcrete();
	}


	/**
	 *  Implementation-specific method to render an editor for a schemaNode defined as {@link ComplexType}
	 */
	protected abstract void renderComplexTypeConcrete();


	/**
	 *  Render a derived Text-Only content model (ComplexType having SimpleContent element)<p>
	 *
	 *  An example text-only model definition:<pre>
	 *  <xsd:complexType>
	 *    <xsd:simpleContent>
	 *      <xsd:extension base="xsd:string">
	 *        <xsd:attribute name="URL" type="stringTextType" use="required"/>
	 *      </xsd:extension> </xsd:simpleContent> </xsd:complexType> </pre>
	 */
	public abstract void renderDerivedTextOnlyModel();


	/**  Render a Derived Content Model (ComplexType having ComplexContent element) */
	public abstract void renderDerivedContentModel();

	public abstract void renderModelGroup(Element group);

	/**  Render an Attribute Node */
	public abstract void renderAttribute();
	
	/**
	 *  Render a choice node. Each of the possible choices is tested for existance, and finally a test for an
	 *  empty choice is included to account for the situation in which a new parent element has been added but
	 *  the choice has not yet been made.
	 *
	 * @param  choice  The choice element of the type definition (see {@link ComplexType})
	 */
	public abstract void renderChoice(Element choice);
	
		/**
	 *  Render the subelements of a sequence compositor
	 *
	 * @param  sequence  the sequence compositor {@link org.dom4j.Element}
	 */
	public abstract void renderSequence(Element sequence);

	/**  Render an element having an unbounded sequence of subelements */
	public abstract void renderRepeatingElement();
	
	public abstract void renderRepeatingSubstitutionGroup();
	
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
	 *  Render the subitems of the current ComplexType element by walking the children elements of the type
	 *  definition. <p>
	 *
	 *  This method called by renderComplexTypeConcrete.
	 */
	public abstract void renderSubElements();


	/**
	 *  Render the given subElements (or those of the current ComplexType if no subElements are provided). This
	 *  method called by renderComplexTypeConcrete.
	 *
	 * @param  subElements  A list of elements to be rendered.
	 */
	public abstract void renderSubElements(List subElements); 

	// -------------- Renderer utilities ---------------

	/**
	* Gets the format (e.g., "adn") of the metadata framework for this Renderer.
	*/
	public final String getXmlFormat() {
		return this.rhelper.getFramework().getXmlFormat();
	}
	
	/**
	* Gets the prefix of the schemaNamespace as defined by the root schema for the
	* metadata framework for this Renderer.
	*/
	public final String getSchemaNSPrefix () {
		return this.sh.getSchemaNamespace().getPrefix();
	}
	
	/**
	* Gets the schema namespace (associated with "http://www.w3.org/2001/XMLSchema") 
	for the root schema for the metadata framework for this Renderer.
	*/
	public final Namespace getSchemaNamespace () {
		return this.sh.getSchemaNamespace();
	}
	
	/**
	 *  Gets the level of the current node relative to the <b>baseLevel</b> . the level is a measure of the
	 *  node's depth in the schema hierarchy.
	 *
	 * @return    The level value
	 */
 	final int getLevel() {
		String[] splits = xpath.split("/");
		return splits.length - rhelper.getBaseLevel();
	}

	public final int getLevel(String xpath) {
		String[] splits = xpath.split("/");
		return splits.length - rhelper.getBaseLevel();
	}
	
	/**
	 *  Gets a DIV element styled for the current level.
	 *
	 * @return    The div value
	 */
	protected Element getDiv() {
		int level = getLevel();
		return df.createElement("div").addAttribute("class", "level-" + Integer.toString(level));
	}


	/**
	*  Gets a DIV element styled for a particular level.
	 *
	 * @param  level  Description of the Parameter
	 * @return        The div value
	 */
	protected Element getDiv(int level) {
		return df.createElement("div").addAttribute("class", "level-" + Integer.toString(level));
	}
	
	/**
	 *  returns the input string surrounded by a string that is replaced with a quotation mark when the rendered
	 *  Element is writen to disk. This is necessary because dom4J turns quotation marks and appostrophies into
	 *  entities, which then cause the JSP processer to barf when the jsp page is rendered
	 *
	 * @param  s  Description of the Parameter
	 * @return    Description of the Return Value
	 */
	public static String jspQuotedString(String s) {
		return "^v^" + s + "^v^";
	}
	
	public abstract Element bestPracticesLink(String xpath);
	
	public abstract Element getInputElement(String xpath, SchemaNode schemaNode, GlobalDef typeDef);
	
	public abstract SimpleTypeLabel getSimpleTypeLabel (String xpath);
	public abstract SimpleTypeLabel getSimpleTypeLabel (String xpath, String siblingPath, String indexId);
	
	public abstract ComplexTypeLabel getComplexTypeLabel (String xpath);
	public abstract ComplexTypeLabel getComplexTypeLabel (String xpath, String siblingPath, String indexId);
	
	public abstract boolean showXsdStringElement (String xpath);
	
	public abstract Element getRenderedField(String xpath, Element label, Element inputField);

	public abstract Element getDeleteController(String itemPath, String elementName);

	public abstract Element getOptionalItemControl(String xpath);
	
	protected abstract Element getMultiBoxInput(String xpath);
	
	/**
	 *  Description of the Method
	 *
	 * @return    Description of the Return Value
	 */
	public String report() {
		String s = "\nRenderer Report";
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
				s+= "\n\t schemaNode is ABSTRACT";
			if (schemaNode.isHeadElement())
				s+= "\n\t schemaNode has SubstitutionGroup";
		}
		
		s += "\n";
		return s;
	}


	/**
	 *  Sets the debug attribute of the Renderer object
	 *
	 * @param  debug  The new debug value
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
			SchemEditUtils.prtln (s, "Renderer");
		}
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  s  NOT YET DOCUMENTED
	 */
	private final void prtlnErr(String s) {
		System.err.println("Renderer: " + s);
	}

}

