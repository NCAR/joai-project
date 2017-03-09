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
package org.dlese.dpc.schemedit.input;

import org.dlese.dpc.schemedit.SchemEditUtils;

import org.dlese.dpc.xml.schema.*;
import org.dlese.dpc.xml.*;

import java.util.*;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Attribute;
import org.dom4j.Node;

/**
 *  Prunes empty, non-schema-required elements and attributes from a Document.
 *  Pruning is performed in place (i.e., as a side effect. no value is
 *  returned);
 *
 * @author     ostwald
 * @created    April 22, 2009
 */
public class DocumentPruner {

	private static boolean debug = false;

	/**  the schemaHelper provided schema info */
	private SchemaHelper schemaHelper;

	private DocMap docMap = null;


	/**
	 *  Constructor for the DocumentPruner object
	 *
	 * @param  doc           Description of the Parameter
	 * @param  schemaHelper  Description of the Parameter
	 */
	private DocumentPruner(Document doc, SchemaHelper schemaHelper) {
		this.schemaHelper = schemaHelper;
		this.docMap = new DocMap(doc, schemaHelper);
	}


	/**
	 *  Constructor for the DocumentPruner object
	 *
	 * @param  docMap        Description of the Parameter
	 * @param  schemaHelper  Description of the Parameter
	 */
	private DocumentPruner(DocMap docMap, SchemaHelper schemaHelper) {
		this.docMap = docMap;
		this.schemaHelper = schemaHelper;
	}


	/**
	 *  Description of the Method
	 *
	 * @param  doc           Description of the Parameter
	 * @param  schemaHelper  Description of the Parameter
	 */
	public static void pruneDocument(Document doc, SchemaHelper schemaHelper) {
		DocumentPruner pruner = new DocumentPruner(doc, schemaHelper);
		pruner.pruneInstanceDoc();
	}


	/**  Rid the instanceDocument of any non-required empty fields */
	private void pruneInstanceDoc() {
		Element rootElement = docMap.getDocument().getRootElement();
		pruneTree(rootElement);
	}


	/**
	 *  Recursively delete all non-required, empty fields and elements.<P>
	 *
	 *  ISSUE: whether or not to prune attributes with only whitespace ..
	 *
	 * @param  e  Description of the Parameter
	 */
	private void pruneTree(Element e) {
		prtln("\n pruneTree: " + e.getPath());

		// ATTRIBUTES - remove non-required, empty attributes
		List attributes = e.attributes();
		prtln("... looking at " + attributes.size() + " attributes");
		for (int i = attributes.size() - 1; i > -1; i--) {
			Attribute attr = (Attribute) attributes.get(i);
			String value = attr.getValue();
			String path = getAttributePath(attr);
			prtln(" .. path: " + path + "(qname: " + attr.getQualifiedName() + ")");

			//  2/16/07 - no longer trim values (thereby removing fields with only whitespace)
			/*
			 *  Handling attributes containing *only whitespace*
			 *  - if the attribute value is schema-valid, leave it alone.
			 *  - if the value is NOT schema-valid, prune it
			 */
			if ((value == null) || (value.trim().length() == 0)) {
				SchemaNode schemaNode = schemaHelper.getSchemaNode(path);
				if (schemaNode == null) {
					// why not delete the attribute in this case??
					prtln("\t schemaNode not found for attribute" + path);
					continue;
				}
				else if (schemaNode.isRequired()) {
					prtln("\t empty attribute at " + path + " required and not pruned");
				}
				else {
					/*
					 *  we are processing an OPTIONAL attribute
					 */
					// remove attributes with EMPTY values
					if (value.length() == 0) {
						prtln("\t removing EMPTY optional attribute \"" + attr.getQualifiedName() + "\" at " + path);
						e.remove(attr);
					}
					// remove attributes containing only white space if they are NOT schema-valid
					else {
						try {
							String typeName = schemaNode.getValidatingType().getName();
							schemaHelper.checkValidValue(typeName, value);
						} catch (Exception cv) {
							prtln("\t removing non-valide whitespace attribute " + attr.getQualifiedName() + " from " + path);
							e.remove(attr);
						}
					}

				}
			}
			else {
				// prtln("\t non-empty attribute not pruned: " + attr.getQualifiedName() + " at " + path + ": value=\"" + value + "\"");
			}
		}

		// ELEMENTS - process the child elements
		List children = e.elements();
		prtln("... looking at " + children.size() + " child elements");
		for (int i = children.size() - 1; i > -1; i--) {
			Element child = (Element) children.get(i);
			String path = child.getPath();
			SchemaNode schemaNode = schemaHelper.getSchemaNode(path);
			if (schemaNode == null) {
				// why not delete the element in this case??
				prtln("schemaNode not found for " + path);
				continue;
			}

			// if the element is complex, prune it
			GlobalDef def = schemaHelper.getGlobalDef(schemaNode);
			if ((def != null) && def.isComplexType()) {
				// prtln(def.getName() + " is complex - recursing");
				pruneTree(child);

				prtln("returned from pruneTree (" + path + ")");

				// if the complex child element is now empty, remove it
				if (!schemaNode.isRequired() && complexElementIsEmpty(path, child)) {
					trace("child (" + path + ") has no content - removing", path);
					e.remove(child);
				}
				else {
					trace("isRequired: " + schemaNode.isRequired(), path);
					trace("nodeExists: " + docMap.nodeExists(path), path);
					trace("Dom4jUtils.isEmpty: " + Dom4jUtils.isEmpty(e, true), path);
					trace("child (" + path + ") HAS content", path);
					trace(child.asXML(), path);
				}
			}
			else {
				// simpleType - if there is no text value and the element is not required, then remove it.
				String value = child.getText();
				if ((value == null) || (value.trim().length() == 0)) {
					if (!schemaNode.isRequired()) {
						trace("removing " + schemaNode.getXpath(), path);
						if (!e.remove(child)) {
							trace("failed to remove " + child.getName() + " from " + path, path);
						}
					}
				}
			}
		}
	}


	/**
	 *  Returns true if an element recursively has no attributes with values or
	 *  elements with text value
	 *
	 * @param  path  NOT YET DOCUMENTED
	 * @param  e     NOT YET DOCUMENTED
	 * @return       NOT YET DOCUMENTED
	 */
	private boolean complexElementIsEmpty(String path, Element e) {
		return (docMap.nodeExists(path) && Dom4jUtils.isEmpty(e, true));
	}


	/**
	 *  Gets the attributePath attribute of the DocumentPruner class
	 *
	 * @param  a  NOT YET DOCUMENTED
	 * @return    The attributePath value
	 */
	private static String getAttributePath(Attribute a) {

		Element parent = a.getParent();
		if (parent == null) {
			prtln(" ... parent is null!");
		}
		return parent.getPath() + "/@" + a.getQualifiedName();
	}


	private static boolean traceEnabled = true;
	private String traceString = "contact";


	/**
	 *  Description of the Method
	 *
	 * @param  s     Description of the Parameter
	 * @param  path  Description of the Parameter
	 */
	private void trace(String s, String path) {
		if (traceEnabled) {
			if (traceString == null || traceString.trim().length() == 0) {
				prtln("trace: traceString is not set");
			}
			else {
				if (path.indexOf(traceString) != -1) {
					prtln(s);
				}
			}
		}
	}


	/**
	 *  Print a line to standard out.
	 *
	 * @param  s  The String to print.
	 */
	private static void prtln(String s) {
		if (debug) {
			SchemEditUtils.prtln(s, "DocumentPruner");
		}
	}

}

