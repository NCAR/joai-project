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
 *  Class responsible for rendering a Text-only content model, which is a
 *  complexType that uses a <b> simpleContent</b> element. SimpleContent
 *  elements have base element that is "extended" by adding attributes.<p>
 *
 *  An example text-only model definition:<pre>
 *  <xsd:complexType>
 *    <xsd:simpleContent>
 *      <xsd:extension base="xsd:string">
 *        <xsd:attribute name="URL" type="stringTextType" use="required"/>
 *      </xsd:extension> </xsd:simpleContent> </xsd:complexType> </pre>
 *
 * @author    ostwald<p>
 *
 *
 */
public class MdeDerivedTextOnlyModel extends MdeComplexType {
	/**  Description of the Field */
	private static boolean debug = true;


	/**
	 *  Constructor for the MdeDerivedTextOnlyModel object
	 *
	 * @param  renderer  NOT YET DOCUMENTED
	 */
	public MdeDerivedTextOnlyModel(RendererImpl renderer) {
		super(renderer);
		prtln ("\ninitialized \n\t" + xpath + "\n\t" + typeDef.getQualifiedInstanceName());
	}


	/**
	 *  The base element is rendered as the base type before rendering the
	 *  children. NOTE: we can't assume the base element is a SimpleType!<
	 */
	public void render() {

		// make sure we are looking at an extension - restriction is not yet supported!
		if (complexTypeDef.getExtensionElement() == null) {
			if (complexTypeDef.getRestrictionElement() != null) {
				prtln("WARNING: renderDerivedTextOnlyModel recieved a restriction element - NOT SUPPORTED!");
			}
			else {
				prtln("WARNING: renderDerivedTextOnlyModel could not find an extension element");
			}
			return;
		}

		Element box = df.createElement("div");
		embedDebugInfo(box, "derived text only model");
		// attachElementDebugInfo (box, "DerivedTextOnlyModel", "yellow");
		parent.add(box);
		parent = box;

		// render extension element first
		renderTextExtensionElement();

		// render extensionElement children - all of which must be attributes
		Iterator extnIterator = complexTypeDef.getExtensionElement().elementIterator();
		while (extnIterator.hasNext()) {
			Element attElement = (Element) extnIterator.next();
			String attElementName = attElement.getName();
			// make sure attElement defines an attribute!
			
			if (attElementName.equals ("attribute")) {
				String attName = attElement.attributeValue("name");
	
				// if we don't have a attName, check to see if the element is a reference, and
				// treat a reference the same as we would a named element
				if (attName == null) {
					attName = attElement.attributeValue("ref");
				}
				String attPath = xpath + "/@" + getQualifiedAttributeName(attName, attElement, complexTypeDef);
				newRenderer(attPath, parent).renderAttribute();
			}
			
			else if (attElementName.equals ("attributeGroup")) {
				prtln ("Attribute Group");
				String attName = attElement.attributeValue("name");
	
				// if we don't have a attName, check to see if the element is a reference, and
				// treat a reference the same as we would a named element
				if (attName == null) {
					attName = attElement.attributeValue("ref");
				}
				GlobalDef globalDef = typeDef.getSchemaReader().getGlobalDef (attName);
				AttributeGroup attrGroup = (AttributeGroup) globalDef;
				for (Iterator a=attrGroup.getAttributes().iterator();a.hasNext();) {
					Element attrElement = (Element)a.next();
					String attrType = attrElement.getName();
					String attrName = attrElement.attributeValue("name");
		
					// if we don't have a childName, check to see if the element is a reference, and
					// treat a reference the same as we would a named element
					if (attrName == null) {
						attrName = attrElement.attributeValue("ref");
					}
					String attPath = xpath + "/@" + getQualifiedAttributeName(attrName, attrElement, complexTypeDef);
					newRenderer(attPath, parent).renderAttribute();
				}
			}
			
			else {
				prtln("WARNING: renderSimpleContent() expected attribute or attributeGroup but found: " + attElementName);
				continue;
			}
			
		}
	}


	/**
	 *  Render the extension element of a Derived Text-Only Model.<p>
	 *
	 *  This method implemented so it can be overridden in {@link org.dlese.dpc.schemedit.autoform.DleseEditorRenderer}
	 *  to suppress display under certain circumstances.
	 */
	protected void renderTextExtensionElement() {

		prtln ("renderTextExtensionElement \n\t (" + xpath + ") \n\t level: " + getLevel());
		String extensionTypeName = getQualifiedElementName(complexTypeDef.getExtensionBase(), complexTypeDef);
		// prtln(" ... extension Base Type: " + extensionTypeName);

		String extnTypeName = getQualifiedElementName(complexTypeDef.getExtensionBase(), complexTypeDef);
		GlobalDef extnType = complexTypeDef.getSchemaReader().getGlobalDef(extnTypeName);
		newRenderer(xpath, parent, extnType).renderNode();
	}


	/**
	 *  Sets the debug attribute of the MdeDerivedTextOnlyModel class
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
	protected void prtln(String s) {
		String prefix = "MdeDerivedTextOnlyModel";
		if (debug) {
			SchemEditUtils.prtln(s, prefix);
		}
	}

}

