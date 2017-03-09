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
package org.dlese.dpc.xml.schema;
import org.dlese.dpc.xml.schema.compositor.*;
import org.dlese.dpc.xml.Dom4jUtils;

import org.dlese.dpc.util.*;

import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.QName;
import org.dom4j.Namespace;

import java.util.*;
import java.io.*;

/**
 *  Wrapper for ComplexType definitions in XML Schemas.
 *
 * @author    ostwald
 */
public class ComplexType extends SimpleType {

	private static boolean debug = false;

	private Compositor compositor = null;

	// contentModel holds the "name" of the first element in the ComplexType definition
	// it can be a compositor, a derivation (simpleContent, complexContent),
	private String contentModel = "unknown";


	/**
	 *  Constructor for the ComplexType object
	 *
	 * @param  element       Schema element defining type
	 * @param  location      File in which type definition exists
	 * @param  namespace     NOT YET DOCUMENTED
	 * @param  schemaReader  reader instance that processed this definition
	 */
	public ComplexType(Element element, String location, Namespace namespace, SchemaReader schemaReader) {

		super(element, location, namespace, schemaReader);

		Element contentModelElement = getFirstChild();
		try {
			contentModel = getFirstChild().getName();
		} catch (Throwable t) {
			prtln("contentModel could not be determined: " + this.element.asXML());
			// return;
		}

		if (this.isDerivedContentModel()) {
			prtln("\n==============\nderiving content model for " + this.getName());
			contentModelElement = deriveModelElement();
			contentModel = contentModelElement.getName();
		}

		try {

			// prtln ("ComplexType() contentModel: " + contentModel);
			if (contentModel.equals("choice")) {
				this.compositor = new Choice(this, contentModelElement);
			}
			else if (contentModel.equals("sequence")) {
				this.compositor = new Sequence(this, contentModelElement);
			}
			else if (contentModel.equals("all")) {
				this.compositor = new All(this, contentModelElement);
			}
		} catch (Throwable t) {
			prtln("ComplexType could not instantiate compositor: " + t.getMessage());
			t.printStackTrace();
		}
		if (compositor != null && compositor.getType() == Compositor.UNKNOWN) {
			prtln("no compositor found");
		}
	}


	/**
	 *  Recursively expand derived content models, producing a compositor with no
	 *  extensions or restrictions.<p>
	 *
	 *  NOTE: all element names and types will have to use instanceDoc namespaces
	 *  ...
	 *
	 * @return    NOT YET DOCUMENTED
	 */
	Element deriveModelElement() {
		prtln("deriveModelElement()");
		if (this.getRestrictionBase() != null)
			return this.deriveRestrictionModelElement();
		else if (this.getExtensionBase() != null)
			return this.deriveExtensionModelElement();
		else {
			prtln("WARNING: deriveModelElement called without derived type (" + this.getName() + ")");
			return null;
		}
	}
	
	
	Element deriveRestrictionModelElement() {
		prtln("deriveRestrictionModelElement()");
		Element restrictionElement = this.getRestrictionElement();
		// prtln ("\n restriction element:" + pp (restrictionElement));
		Element ret = ((Element) restrictionElement.elementIterator().next()).createCopy();
		// prtln ("\t returning: " +pp (ret));
		return ret;
	}

	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @return    NOT YET DOCUMENTED
	 */
	Element deriveExtensionModelElement() {
		prtln("\nderiveExtensionModelElement() with " + this.getName());
		try {
			Stack derivationStack = new Stack();
			derivationStack.push(this);
			// prtln ("\t pushed " + this.getName());
			ComplexType baseType = (ComplexType) getExtensionType();
			while (baseType != null) {
				derivationStack.push(baseType);
				prtln("\t pushed " + baseType.getName());
				prtln("\t\t ns: " + NamespaceRegistry.nsToString(baseType.getNamespace()));
				// prtln ("\n ---\n" + pp (baseType.getExtensionElement()) + " --");
				baseType = (ComplexType) baseType.getExtensionType();
			}

			if (derivationStack.empty()) {
				Element restrictionElement = this.getRestrictionElement();
				if (restrictionElement == null)
					throw new Exception("not a restriction or an extension?");
				prtln("\n restriction element:" + pp(restrictionElement));
				return (Element) restrictionElement.elementIterator().next();
			}

			ComplexType root = (ComplexType) derivationStack.pop();
			// prtln ("\t popped " + root.getName());
			Element modelElement = root.getFirstChild().createCopy();
			prtln("\nroot model Element (" + NamespaceRegistry.nsToString(root.getNamespace()) + "): " + pp(modelElement));

			while (true) {
				if (derivationStack.empty())
					break;
				ComplexType derivedType = (ComplexType) derivationStack.pop();
				prtln("\t popped " + derivedType.getName());
				Element extnElement = derivedType.getExtensionElement();
				// prtln ("\nextnElement: " + pp(extnElement));

				if (extnElement.elements().isEmpty()) {
					prtln("\t extnElements empty for " + derivedType.getName());
					continue;
				}

				Element model = (Element) extnElement.elementIterator().next();
				for (Iterator i = model.elementIterator(); i.hasNext(); ) {
					Element member = (Element) i.next();
					// This is where we would change name and type to instanceDoc namespace??
					prtln("\n--\nadding member: " + member.asXML());

					modelElement.add(member.createCopy());
				}
			}
			return modelElement;
		} catch (Throwable t) {
			prtln("deriveExtensionModelElement error: " + t.getMessage());
			t.printStackTrace();
			// throw new Exception ("deriveModelElement error");
		}
		return null;
	}



	/**
	 *  Gets the dataType attribute of the ComplexType object
	 *
	 * @return    COMPLEX_TYPE - The dataType value as an int
	 */
	public int getDataType() {
		return COMPLEX_TYPE;
	}

	// USE COMPOSITOR!?
	/**
	 *  Gets the contentModel attribute of the ComplexType object
	 *
	 * @return    The contentModel value
	 */
	public String getContentModel() {
		return contentModel;
	}


	/**
	 *  Gets the compositor attribute of the ComplexType object. ASSUMPTION:
	 *  compositor element is first child (not including annotation element)
	 *
	 * @return    The compositor value
	 */
	public Compositor getCompositor() {
		if (getCompositorType() != Compositor.UNKNOWN)
			return compositor;
		else
			return null;
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @return    NOT YET DOCUMENTED
	 */
	public boolean hasInlineCompositor() {
		Compositor compositor = getCompositor();
		if (compositor == null)
			return false;
		return (compositor instanceof InlineCompositor);
	}


	/**
	 *  Gets the abstract attribute of the ComplexType object
	 *
	 * @return    The abstract value
	 */
	public boolean isAbstract() {
		String abstractValue = getElement().attributeValue("abstract", null);
		return (abstractValue != null && abstractValue.equals("true"));
	}


	/**
	 *  Gets the compositorType attribute of the ComplexType object
	 *
	 * @return    The compositorType value
	 */
	public int getCompositorType() {
		if (compositor != null)
			return compositor.getType();
		else
			return Compositor.UNKNOWN;
	}


	/**
	 *  Returns a list of choice elements if a choise compositor is found, and an
	 *  empty list otherwise
	 *
	 * @return    The choices value
	 */
	public List getChoices() {
		if (getCompositorType() == Compositor.CHOICE)
			return compositor.getMemberNames();
		else
			return new ArrayList();
	}


	/**
	 *  Gets the derivedType attribute of the ComplexType object
	 *
	 * @return    The derivedType value
	 */
	public boolean isDerivedType() {
		return (hasSimpleContent() || hasComplexContent());
	}

	// --------- Derived Content Model --------------

	/**
	 *  Returns true if this complexType defines a derived content model (i.e., it
	 *  has a "complexContent" element as first child.
	 *
	 * @return    The derivedContentModel value
	 */
	public boolean isDerivedContentModel() {
		return hasComplexContent();
	}


	/**
	 *  Tests for presence of a simpleContent element
	 *
	 * @return    true if a simpleContent element is present
	 */
	public boolean hasComplexContent() {
		// Element e = getFirstChild();
		// return contentModel.equals("complexContent");
		return getFirstChild().getName().equals("complexContent");
	}


	/**
	 *  Gets the complexContent element of the ComplexType object.
	 *
	 * @return    The complexContent value or null if not found.
	 */
	public Element getComplexContent() {
		return (Element) getElement().selectSingleNode(NamespaceRegistry.makeQualifiedName(xsdPrefix, "complexContent"));
	}

	// --------- Text Only Content Model --------------
	/**
	 *  Returns true if this complexType defines a derived text only model (i.e.,
	 *  it has a "simpleContent" element as first child.
	 *
	 * @return    The derivedTextOnlyModel value
	 */
	public boolean isDerivedTextOnlyModel() {
		return hasSimpleContent();
	}


	/**
	 *  Tests for presence of a simpleContent element
	 *
	 * @return    true if a simpleContent element is present
	 */
	public boolean hasSimpleContent() {
		return contentModel.equals("simpleContent");
	}


	/**
	 *  Gets the simpleContent attribute of the ComplexType object
	 *
	 * @return    The simpleContent value
	 */
	public Element getSimpleContent() {
		return (Element) getElement().selectSingleNode(NamespaceRegistry.makeQualifiedName(xsdPrefix, "simpleContent"));
	}


	/**
	 *  Gets the base extension type of the simpleContent element of the
	 *  ComplexType.
	 *
	 * @return    The base extension type name if this ComplexType has
	 *      simpleContent, null otherwise.
	 */
	public String getSimpleContentType() {
		if (!hasSimpleContent()) {
			return null;
		}
		Element e = getElement();
		Node node = e.selectSingleNode(NamespaceRegistry.makeQualifiedName(xsdPrefix, "simpleContent") + "/" +
			NamespaceRegistry.makeQualifiedName(xsdPrefix, "extension"));
		if (node == null) {
			prtln("extension node not found for " + getName());
			return null;
		}
		return ((Element) node).attributeValue("base");
	}



	// --------- Extension stuff (for both derived content and text only --------------

	/**
	 *  Gets the derivedCompositorType attribute of the ComplexType object
	 *
	 * @return    The derivedCompositorType value
	 */
	public int getDerivedCompositorType() {
		ComplexType derivationRoot = null;
		if (this.getRestrictionBase() != null)
			derivationRoot = this.getRestrictionRootType();
		else if (this.getExtensionBase() != null)
			derivationRoot = this.getExtensionRootType();

		if (derivationRoot != null)
			return derivationRoot.getCompositorType();
		else
			return Compositor.UNKNOWN;
	}


	/**
	 *  Gets the extensionElement ComplexType object
	 *
	 * @return    The extensionElement or null if not found.
	 */
	public Element getExtensionElement() {
		if (isDerivedContentModel())
			return (Element) getElement()
				.selectSingleNode(NamespaceRegistry.makeQualifiedName(xsdPrefix, "complexContent") + "/" +
				NamespaceRegistry.makeQualifiedName(xsdPrefix, "extension"));
		if (isDerivedTextOnlyModel())
			return (Element) getElement()
				.selectSingleNode(NamespaceRegistry.makeQualifiedName(xsdPrefix, "simpleContent") + "/" +
				NamespaceRegistry.makeQualifiedName(xsdPrefix, "extension"));
		return null;
	}


	/**
	 *  Gets the restrictionElement ComplexType object.
	 *
	 * @return    The restrictionElement or null if not found.
	 */
	public Element getRestrictionElement() {
		if (isDerivedContentModel())
			return (Element) getElement()
				.selectSingleNode(NamespaceRegistry.makeQualifiedName(xsdPrefix, "complexContent") +
				"/" + NamespaceRegistry.makeQualifiedName(xsdPrefix, "restriction"));
		if (isDerivedTextOnlyModel())
			return (Element) getElement()
				.selectSingleNode(NamespaceRegistry.makeQualifiedName(xsdPrefix, "simpleContent") +
				"/" + NamespaceRegistry.makeQualifiedName(xsdPrefix, "restriction"));
		return null;
	}


	/**
	 *  Gets the name of the extension base type for complex types that derive by
	 *  extension.
	 *
	 * @return    The extensionBase value or null if not found.
	 */
	public String getExtensionBase() {
		Element extn = getExtensionElement();
		return (extn == null ? null : extn.attributeValue("base"));
	}


	/**
	 *  Gets the extensionType attribute of the ComplexType object
	 *
	 * @return    The extensionType value
	 */
	public GlobalDef getExtensionType() {
		String base = getExtensionBase();
		return (base == null ? null : getSchemaReader().getGlobalDef(base));
	}


	/**
	 *  Recursively traverse the derived Model definitions until a base type
	 *  definition is found that is not derived from another (via an extension or a
	 *  restriction).
	 *
	 * @return    The extensionRootType value
	 */
	public ComplexType getExtensionRootType() {
		ComplexType rootType = null;
		ComplexType baseType = (ComplexType) getExtensionType();
		while (baseType != null) {
			rootType = baseType;
			baseType = (ComplexType) rootType.getExtensionType();
		}
		return rootType;
	}


	/**
	 *  Gets the name of the restriction base type for complex types that derive by
	 *  restriction.
	 *
	 * @return    The restrictionBase value or null if not found.
	 */
	public String getRestrictionBase() {
		Element extn = getRestrictionElement();
		return (extn == null ? null : extn.attributeValue("base"));
	}


	/**
	 *  Gets the restrictionType attribute of the ComplexType object
	 *
	 * @return    The restrictionType value
	 */
	public GlobalDef getRestrictionType() {
		String base = getRestrictionBase();
		return (base == null ? null : getSchemaReader().getGlobalDef(base));
	}


	/**
	 *  Gets the restrictionRootType attribute of the ComplexType object
	 *
	 * @return    The restrictionRootType value
	 */
	public ComplexType getRestrictionRootType() {
		ComplexType rootType = null;
		ComplexType baseType = (ComplexType) getRestrictionType();
		while (baseType != null) {
			rootType = baseType;
			baseType = (ComplexType) rootType.getRestrictionType();
		}
		return rootType;
	}


	/**
	 *  Gets the derivationBase for derived ComplexTypes.<p>
	 *
	 *  Returns the base type for restriction or extention elements of either
	 *  derivedContentModels or derivedTestOnlyModels.
	 *
	 * @return    The derivationBase value or null if not found.
	 */
	public String getDerivationBase() {
		if (getExtensionBase() != null)
			return getExtensionBase();
		if (getRestrictionBase() != null)
			return getRestrictionBase();
		return null;
	}


	/**
	 *  Gets the derivationType attribute of the ComplexType object
	 *
	 * @return    The derivationType value
	 */
	public GlobalDef getDerivationType() {
		String base = getDerivationBase();
		return (base == null ? null : getSchemaReader().getGlobalDef(base));
	}


	/**
	 *  String representation of the ComplexType object
	 *
	 * @return    a String representation of the ComplexType object
	 */
	public String toString() {
		String s = "ComplexType: " + getName();
		String nl = "\n\t";
		s += nl + "type: " + getType();
		s += nl + "location: " + getLocation();
		s += nl + "contentmodel: " + getContentModel();
		if (getNamespace() != Namespace.NO_NAMESPACE)
			s += nl + "namespace: " + getNamespace().getPrefix() + ": " + getNamespace().getURI();
		else
			s += nl + "namespace: null";
		//		s += nl + "modelGroup: " + modelGroup;
		//		s += nl + "there are " + getChildren().size() + " content elements";
		/*
			 *  if (modelGroup == null)
			 *  s += "\n" + element.asXML();
			 */
		s += nl + "schemaNamespace: " + this.getSchemaReader().getNamespaces().getSchemaNamespace().getPrefix();
		s += Dom4jUtils.prettyPrint(getElement());
		return s;
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  node  NOT YET DOCUMENTED
	 * @return       NOT YET DOCUMENTED
	 */
	private static String pp(Node node) {
		return Dom4jUtils.prettyPrint(node);
	}


	/**
	 *  print a string to std out
	 *
	 * @param  s  Description of the Parameter
	 */
	protected static void prtln(String s) {
		if (debug) {
			while (s.length() > 0 && s.charAt(0) == '\n') {
				System.out.println("");
				s = s.substring(1);
			}
			System.out.println("ComplexType: " + s);
		}
	}
}

