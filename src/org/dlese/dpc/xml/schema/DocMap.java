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

import java.util.*;
import java.io.StringWriter;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.DocumentFactory;
import org.dom4j.Namespace;
import org.dom4j.QName;

import org.dlese.dpc.xml.*;

/**
 *  DocMap wraps a dom4j Document providing get and put methods for use with a
 *  Map-Backed Action form. Also supports document operations such as creating,
 *  inserting and removing nodes.
 *
 * @author    ostwald
 */
public class DocMap {
	private boolean debug = false;

	Document doc;
	XMLWriter writer;
	SchemaHelper schemaHelper = null;
	private DocumentFactory df = new DocumentFactory();


	/**
	 *  Constructor for the DocMap object
	 *
	 * @param  doc  Description of the Parameter
	 */
	public DocMap(Document doc) {
		this(doc, null);
	}


	/**
	 *  Constructor for the DocMap object
	 *
	 * @param  doc           NOT YET DOCUMENTED
	 * @param  schemaHelper  NOT YET DOCUMENTED
	 */
	public DocMap(Document doc, SchemaHelper schemaHelper) {
		this.doc = doc;
		this.schemaHelper = schemaHelper;
	}


	/**
	 *  Gets the document attribute of the DocMap object
	 *
	 * @return    The document value
	 */
	public Document getDocument() {
		return this.doc;
	}


	/**
	 *  Get list of all nodes selected by provided xpath
	 *
	 * @param  xpath  the xpath
	 * @return        list of nodes
	 */
	public List selectNodes(String xpath) {
		if (xpath == null || xpath.trim().length() == 0)
			return new ArrayList();
		return doc.selectNodes(xpath);
	}


	/**
	 *  Gets first node at proviced xpath
	 *
	 * @param  xpath  Description of the Parameter
	 * @return        Description of the Return Value
	 */
	public Node selectSingleNode(String xpath) {
		if (xpath == null || xpath.trim().length() == 0)
			return null;
		return doc.selectSingleNode(xpath);
	}


	/**
	 *  Returns true if there is a node at specified xpath
	 *
	 * @param  xpath  xpath to node
	 * @return        true if node exists at xpath
	 */
	public boolean nodeExists(String xpath) {
		if (xpath == null || xpath.trim().length() == 0)
			return false;
		return (selectSingleNode(xpath) != null);
	}



	/**
	 *  Given an encoded xpath, return the value of the referred to node
	 *
	 * @param  key  xpath encoded as necessary for use in jsp
	 * @return      text of the node referred to by the xpath or an empty string if
	 *      the node cannot be found
	 */
	public Object get(String key) {
		String xpath = XPathUtils.decodeXPath(key);
		Node node = doc.selectSingleNode(xpath);
		String val = "";
		if (node == null) {
			// prtln("DocMap.get(): couldn't find node for " + xpath);
		}
		else {
			try {
				val = node.getText();
			} catch (Exception e) {
				prtlnErr("DocMap.get(): unable to get Text for " + xpath);
			}
		}
		return val;
	}


	/**
	 *  Updates the Document by setting the Text of the Node at the specified
	 *  xpath. If there is no node at the specified path, a new one is created.<p>
	 *
	 *  ISSUE: what if there is no value (one way this happens is when there is a
	 *  field in the form but no value is supplied by user. In this case we
	 *  shouldn't bother creating a new node because there is no value to insert.
	 *  But what if there was formerly a value and the user has deleted it? if the
	 *  node is an Element, then should that element be deleted? (The user should
	 *  be warned first!) if the node is an attribute, then should that attribute
	 *  be deleted? (the user won't be warned, since this does not have the
	 *  "ripple" effect that deleting an element can have. (maybe the user should
	 *  be warned only if the element has children with values).
	 *
	 * @param  key  unencoded xpath
	 * @param  val  value to assign to node at xpath
	 */
	public void put(Object key, Object val) {
		String xpath = XPathUtils.decodeXPath((String) key);

		Node node = doc.selectSingleNode(xpath);
		if (node == null) {
			// prtln("DocMap.put(): creating new node for " + xpath);
			try {
				node = createNewNode(xpath);
			} catch (Exception e) {
				prtlnErr("DocMap.put(): couldn't create new node at \"" + xpath + "\": " + e);
				return;
			}
		}
		// 2/28/07 no longer trim values!
		/* 		String trimmed_val = "";
		if (val != null)
			trimmed_val = ((String) val).trim();
		node.setText(trimmed_val);*/
		node.setText(val != null ? (String) val : "");
	}


	/**
	 *  Tests xpath against provided schema (via SchemaHelper) before putting
	 *  value, creating a new Node if one is not found at xpath.
	 *
	 * @param  xpath          NOT YET DOCUMENTED
	 * @param  value          NOT YET DOCUMENTED
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public void smartPut(String xpath, String value) throws Exception {
		if (schemaHelper != null && schemaHelper.getSchemaNode(xpath) == null) {
			throw new Exception("stuffValue got an illegal xpath: " + xpath);
		}

		if (value == null)
			throw new Exception("stuffValue got a null value (which is illegal) for " + xpath);

		Node node = selectSingleNode(xpath);
		if (node == null)
			node = createNewNode(xpath);
		if (node == null)
			throw new Exception("smartPut failed to find or create node: " + xpath);
		else {
			// 2/18/07 no longer trim values!
			// node.setText(value.trim());
			node.setText(value);
		}
	}


	/**
	 *  Return true if the specified node is an Element and has either a
	 *  sub-element, or an attribute (even if they are empty), OR content.
	 *
	 * @param  xpath  xpath to the node to be evaluated for children
	 * @return        true if sub-elements, or attributes, false otherwise or if
	 *      node is not an Element
	 */
	public boolean hasChildren(String xpath) {
		// prtln ("\nhasChildren: " + xpath);
		if (xpath == null || xpath.trim().length() == 0)
			return false;
		Node node = doc.selectSingleNode(xpath);
		if (node == null) {
			prtlnErr("\thasChildren() could not find node: (" + xpath + ")");
			return false;
		}

		if (node.getNodeType() != Node.ELEMENT_NODE) {
			prtlnErr("hasChildern() called with an non-Element - returning false");
			return false;
		}

		Element e = (Element) node;

		// we used to check for "hasText" but why would we want to do that here???
		/*
			We DO want to check in the case of an element that can contain text which ALSO
			has attributes. So we can do this check IF the typeDef is the right kind ...
		*/
		boolean hasText = (e.getTextTrim() != null && e.getTextTrim().length() > 0);
		if (hasText)
			return true;

		return (e.elements().size() > 0 ||
			e.attributeCount() > 0);
	}


	/**
	 *  Returns true if an element (recursively) has no textual content, no
	 *  children, and no attributes with values.<p>
	 *
	 *  Note: returns FALSE if no node exists at the given path.
	 *
	 * @param  xpath  Description of the Parameter
	 * @return        true if empty, false if any errors are encountered
	 */
	public boolean isEmpty(String xpath) {
		Node node = doc.selectSingleNode(xpath);
		String msg = "";

		// return FALSE if a node is not found (this is kind of a wierd convention?)
		if (node == null) {
			msg = " ... couldn't find node at " + xpath + " returning FALSE";
			// prtlnErr(msg);
			return false;
		}

		if (node.getNodeType() == Node.ATTRIBUTE_NODE) {
			String content = node.getText();
			// return (content == null || content.trim().length() == 0);

			// 2/28/07 - no longer ignore whitespace!
			return (content == null || content.length() == 0);
		}

		if (node.getNodeType() != Node.ELEMENT_NODE) {
			msg = "  ...  called with an unknown type of node - returning false";
			// prtlnErr(msg);
			return false;
		}

		boolean ret = Dom4jUtils.isEmpty((Element) node);
		return ret;
	}


	/**
	 *  removes a node from the dom4j Document.
	 *
	 * @param  xpath  Description of the Parameter
	 */
	public void remove(String xpath) {
		Node node = doc.selectSingleNode(xpath);
		if (node == null) {
			// prtln("remove could not find node to delete (" + xpath + ")");
			return;
		}

		Element parent = node.getParent();
		if (parent == null) {
			// prtln("remove could not find parent of node to delete (" + xpath + ")");
			return;
		}
		if (!parent.remove(node)) {
			prtlnErr("failed to remove node at " + xpath);
		}
	}


	/**
	 *  Remove a node and all it's siblings (having the same element name) from the
	 *  parent node. One way to do this would be to clear the contents of the
	 *  parent node, but i'm not sure this is safe. so instead we will find all
	 *  nodes matching the "deindexed" xpath and then remove them individually.
	 *
	 * @param  xpath          xpath to an enumeration node
	 * @return                true if successful
	 * @exception  Exception  if unable to remove siblings
	 */
	public boolean removeSiblings(String xpath)
		 throws Exception {
		Node node = doc.selectSingleNode(xpath);
		if (node == null) {
			String msg = "removeSiblings couldn't find node at " + xpath;
			throw new Exception(msg);
		}

		Element parent = node.getParent();
		if (parent == null) {
			String msg = "removeSiblings() couldn't find parent node for :" + xpath;
			throw new Exception(msg);
		}

		// why not just use parent.clearContent()??
		List elements = parent.elements(node.getName());
		for (int i = elements.size() - 1; i > -1; i--) {
			Element e = (Element) elements.get(i);
			if (!parent.remove(e)) {
				throw new Exception("removeSiblings() failed to remove element");
			}
		}
		return true;
	}


	/**
	 *  finds the like-named siblings of the node specified by xpath.
	 *
	 * @param  xpath  an xpath to a specific node
	 * @return        the total number of siblings with the same element name
	 *      (including the node specified by the xpath)
	 */
	public int getSiblingCount(String xpath) {
		Node node = doc.selectSingleNode(xpath);
		if (node == null) {
			// prtlnErr("getSiblingCount() could not find node (" + xpath + ")");
			return 0;
		}

		String siblingXPath = XPathUtils.getSiblingXPath(xpath);
		List siblings = doc.selectNodes(siblingXPath);
		return siblings.size();
	}


	/**
	 *  Create a new element at the location specified by jsp-encoded xpath.
	 *
	 * @param  encodedXPath  Description of the Parameter
	 * @return               the new element
	 */
	private Element newElement(String encodedXPath) {
		String xpath = XPathUtils.decodeXPath(encodedXPath);
		String elementName = XPathUtils.getNodeName(xpath);
		String parentPath = XPathUtils.getParentXPath(xpath);
		Element parent = (Element) selectSingleNode(parentPath);
		if (parent == null) {
			// prtln("newElement() failed to find parent node: " + parentPath + " ... creating ... ");
			parent = newElement(parentPath);
			if (parent == null) {
				prtlnErr("parent could not be created at " + parentPath);
				return null;
			}
		}

		Element newChild = df.createElement(elementName);
		try {
			insertElement(newChild, parent, xpath);
		} catch (Exception e) {
			prtlnErr("unable to insert new element into dom: " + e.getMessage());
			return null;
		}

		return newChild;
	}


	/**
	 *  Called from metadata controller to add an element at a specific place in
	 *  the document. The xpath specified by encodedReferencePath provides an
	 *  insertion point for the new element. Namely, we want to insert the new
	 *  element immdediately following the encodedReferencePath.
	 *
	 * @param  e                     The feature to be added to the Element
	 *      attribute
	 * @param  encodedReferencePath  The feature to be added to the Element
	 *      attribute
	 * @return                       NOT YET DOCUMENTED
	 */
	public boolean addElement(Element e, String encodedReferencePath) {
		String xpath = XPathUtils.decodeXPath(encodedReferencePath);
		// prtln("addElement with reference path: " + xpath);
		String parentPath = XPathUtils.getParentXPath(xpath);
		Element parent = (Element) selectSingleNode(parentPath);
		if (parent == null) {
			// prtlnErr ("addElement() failed to find parent node: " + parentPath + " ... creating ... ");
			parent = newElement(parentPath);
			if (parent == null) {
				prtlnErr("addElement: parent could not be created at " + parentPath);
				return false;
			}
		}
		/* we have to give insertElement the targetPath for the element, which is
		constructed by incrementing the index of the given xpath
		*/
		try {
			int pathIndex = XPathUtils.getIndex(xpath);
			String targetPath = XPathUtils.getSiblingXPath(xpath) + "[" + Integer.toString(pathIndex + 1) + "]";
			insertElement(e, parent, targetPath);

		} catch (Exception ex) {
			prtlnErr("addElement failed to add child: " + ex.getMessage());
			// ex.printStackTrace();
			return false;
		}

		return true;
	}



	/**
	 *  Insert the given element into parent using targetPath for placement
	 *  information (insertion point is immediatlely following the node identified
	 *  by targetPath).
	 *
	 * @param  element        element to be inserted
	 * @param  parent         parent in which to insert element
	 * @param  targetPath     specifies insertion point
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public void insertElement(Element element, Element parent, String targetPath) throws Exception {
		// prtln("insertElement() child: " + element.getName() + "  parent: " + parent.getName() + "\n\ttargetPath: " + targetPath);

		int targetIndex = XPathUtils.getIndex(targetPath);

		// if index is 0, or if the parent is empty, then we have to find insertion point using the compositor!
		if (Dom4jUtils.isEmpty(parent) || targetIndex == 0) {
			untargetedInsert(element, parent);
		}
		else {
			targetedInsert(element, parent, targetIndex);
		}
	}


	/**
	 *  If the child is an substitution element, then return the Head Element's
	 *  qualified name, otherwise just use the child's qualified name
	 *
	 * @param  parentPath  NOT YET DOCUMENTED
	 * @param  element     NOT YET DOCUMENTED
	 * @return             NOT YET DOCUMENTED
	 */
	private String resolveElementName(String parentPath, Element element) {

		String qualifiedName = element.getQualifiedName();
		if (schemaHelper == null)
			return qualifiedName;

		String xpath = parentPath + "/" + qualifiedName;
		SchemaNode schemaNode = schemaHelper.getSchemaNode(xpath);
		if (schemaNode == null) {
			// prtln ("\t schemaNode not found: returning " + qualifiedName);
			return qualifiedName;
		}

		if (schemaNode.getHeadElementName().length() > 0) {
			// prtln ("\t this HAS a head element" + Dom4jUtils.prettyPrint (element));
			return schemaNode.getHeadElementName();
		}
		else {
			// prtln ("this did not have a head element");
			return qualifiedName;
		}
	}


	/**
	 *  Insert an element in it's parent element using compositor. The compositor
	 *  can provide information that allows us to place the new element at the
	 *  correct place in the parent element.
	 *
	 * @param  element        NOT YET DOCUMENTED
	 * @param  parent         NOT YET DOCUMENTED
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	private void untargetedInsert(Element element, Element parent) throws Exception {
		// prtln("\n untargetedInsert()");
		// prtln("\telement name: " + element.getQualifiedName());
		// prtln ("\telement to insert: " + Dom4jUtils.prettyPrint(element) + "\n");
		String parentPath = parent.getPath();

		if (schemaHelper == null || !schemaHelper.hasSequenceCompositor(parentPath)) {

			// prtln ("no help from schemaHelper - inserting element at end of parent");
			if (schemaHelper == null)
				prtln("SchemaHelper is NULL");
			parent.add(element);
			return;
		}
		else {
			Sequence compositor = (Sequence) schemaHelper.getCompositor(parentPath);
			String compositorMemberName = resolveElementName(parentPath, element);
			int memberIndex = compositor.getIndexOfMember(compositorMemberName);

			/*
			 insert element immediately following other like-named elements, or just ahead
			 of the following member if no like-named elements exist.
			 when the childMemberIndex is greater than the (target) memberIndex, then it is
			 time to insert.
			*/
			List elements = parent.elements();
			for (int i = 0; i < elements.size(); i++) {
				Element child = (Element) elements.get(i);
				String childCompositorMemberName = resolveElementName(parentPath, child);
				int childMemberIndex = compositor.getIndexOfMember(childCompositorMemberName);

				if (childMemberIndex > memberIndex) {
					elements.add(i, element);
					return;
				}
			}
			parent.add(element);
		}
	}


	/**
	 *  Insert an element into the parent immediately following the targetIndex.
	 *  For example, if a parent element had three children elements, and the
	 *  targetIndex were 2, then the new element would be inserted after the second
	 *  element and before the third. NOTE: throws exception if targetIndex is
	 *  greater than one and an element corresponding to targetIndex does not
	 *  exist.
	 *
	 * @param  element        element to be inserted
	 * @param  parent         parent in which to be inserted
	 * @param  targetIndex    index after which to be inserted
	 * @exception  Exception  if targetIndex is greater than one and an element
	 *      corresponding to targetIndex does not exist.
	 */
	private void targetedInsert(Element element, Element parent, int targetIndex) throws Exception {
		prtln("\ntargetedInsert()");
		// prtln ("\telement to insert: " + Dom4jUtils.prettyPrint(element));
		String parentPath = parent.getPath();
		String targetName = resolveElementName(parentPath, element);
		prtln("  ... target info -- name: " + targetName + "  targetIndex: " + targetIndex);
		int n = 0;

		List elements = parent.elements();
		for (int i = 0; i < elements.size(); i++) {
			Element child = (Element) elements.get(i);
			String childName = resolveElementName(parentPath, child);
			if (childName.equals(targetName)) {
				// increment index, and look for target
				n++;
				if (n == targetIndex - 1) {
					// insert element after elements[i]
					if (i + 1 < elements.size())
						elements.add(i + 1, element);
					else
						elements.add(element);
					return;
				}
			}
		}

		// when the editor needs to create a new repeating node for an empty parent, it supplies an index of 1 ...
		if (targetIndex == 1) {
			untargetedInsert(element, parent);
		}
		else {
			throw new Exception("targetedInsert failed to find target");
		}

	}


	/**
	 *  Put sequence elements in the order specified by the sequence compositor.
	 *
	 * @param  parent  NOT YET DOCUMENTED
	 */
	public void orderSequenceElements(Element parent) {
		String xpath = parent.getPath();
		List order = schemaHelper.getChildrenOrder(xpath);

		// just as a safeguard, don't reorder if there is only one child
		// for the schemaNode, since parent is probably a repeating element
		// and reordering would wipe out all but one child
		if (order.size() < 2)
			return;

		// map element name to a list of elements to accomodate repeating elements
		Map elementMap = new HashMap();
		List elements = parent.elements();
		for (int i = elements.size() - 1; i > -1; i--) {
			Element e = (Element) elements.get(i);
			String tagName = e.getName();
			List items = (List) elementMap.get(tagName);
			if (items == null)
				items = new ArrayList();
			items.add(0, e.detach()); // add to beginning to maintain doc ordering
			elementMap.put(tagName, items);
		}

		for (Iterator i = order.iterator(); i.hasNext(); ) {
			String name = (String) i.next();
			elements = (List) elementMap.get(name);
			if (elements != null) {
				for (Iterator ii = elements.iterator(); ii.hasNext(); ) {
					parent.add((Element) ii.next());
				}
			}
		}
	}


	/**
	 *  Removes the element at specified path
	 *
	 * @param  encodedXPath  xpath encoded by jsp
	 */
	public void removeElement(String encodedXPath) {
		prtln("removeElement: " + encodedXPath);
		String xpath = XPathUtils.decodeXPath(encodedXPath);
		remove(xpath);
	}


	/**
	 *  Creates new node that is a sibling of the node at provided xpath
	 *
	 * @param  xpath          xpath to existing node
	 * @return                newly created sibling node
	 * @exception  Exception  if a new node cannot be created
	 */
	public Node createNewSiblingNode(String xpath) throws Exception {
		String siblingPath = XPathUtils.getSiblingXPath(xpath);
		int siblingCount = getSiblingCount(siblingPath);
		String indexedPath = siblingPath + "[" + Integer.toString(siblingCount + 1) + "]";
		return createNewNode(indexedPath);
	}


	/**
	 *  Creates a new node at the location specified by the xpath, creating all the
	 *  necessary nodes along the path in the process.<p>
	 *
	 *  When schemaHelper is present and the path specifies an Element, then a new
	 *  element is created by SchemaHelper.getNewElement. Otherwise, the newly
	 *  created Element (or Attribute) is empty.
	 *
	 * @param  xpath          location of new node to be created
	 * @return                dom4j Node (either an Attribute or an Element)
	 * @exception  Exception  if unable to create a new node at xpath
	 */
	public Node createNewNode(String xpath)
		 throws Exception {
		prtln("\ncreateNewNode with " + xpath);

		if (nodeExists(xpath)) {
			String msg = "createNewNode(): node at " + xpath + " already exists!";
			throw new Exception(msg);
		}

		String parentPath = XPathUtils.getParentXPath(xpath);

		// does parent exist?
		Node parentNode = doc.selectSingleNode(parentPath);
		if (parentNode == null) {
			prtln("parentNode does not exist at " + parentPath + " ... creating new one ...");
			try {
				parentNode = createNewNode(parentPath);
			} catch (Exception e) {
				String msg = "failed to create new parent: " + e;
				throw new Exception(msg);
			}
		}

		// at this point we have to test again to make sure the node wasn't created
		// in the process of creating a parent (this could happen if the newly created
		// element was a TREE that included the node we were to create
		if (doc.selectSingleNode(xpath) != null)
			return doc.selectSingleNode(xpath);

		// sanity check: parent must be an element (cause attributes can't have children)
		if (parentNode.getNodeType() != Node.ELEMENT_NODE) {
			String msg = "createNewNode(): parent Node at " + parentPath + " is not an element";
			throw new Exception(msg);
		}
		Element parent = (Element) parentNode;

		// are we creating an attribute?
		if (XPathUtils.isAttributePath(xpath)) {
			String attName = XPathUtils.getNodeName(xpath);

			if (NamespaceRegistry.isQualified(attName)) {
				if (schemaHelper == null)
					throw new Exception("SchemaHelper required to create qualified attribute");

				String prefix = NamespaceRegistry.getNamespacePrefix(attName);
				Namespace ns = schemaHelper.getGlobalDefMap().getNamespaces().getNSforPrefix(prefix);
				QName qname = df.createQName(attName, ns.getURI());
				parent.addAttribute(qname, "");
				return parent.attribute(qname);
			}
			else {
				parent.addAttribute(attName, "");
				return parent.attribute(attName);
			}
		}
		else {
			// we are creating an element
			// getNodeName removes the indexing "e.g., [2]" from the leaf
			String elementName = XPathUtils.getNodeName(xpath);

			// prtln("creating new element: " + elementName);
			Element newElement = null;
			if (schemaHelper == null) {
				// prtln ("schemaHelper not found - performing simple addElement");
				newElement = df.createElement(elementName);
			}
			else {
				// prtln ("schemaHelper found - adding new element and children");
				newElement = schemaHelper.getNewElement(xpath);
			}

			// if the path contains an index, ensure the previous siblings exist
			int index = XPathUtils.getIndex(xpath);
			if (index > 1) {
				for (int i = 1; i < index; i++) {
					String siblingPath = parentPath + "/" + elementName + "[" + i + "]";
					if (!nodeExists(siblingPath)) {
						insertElement(newElement.createCopy(), parent, siblingPath);
					}
				}
			}

			insertElement(newElement, parent, xpath);

			return newElement;
		}
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  s  NOT YET DOCUMENTED
	 */
	protected final void prtlnErr(String s) {
		System.err.println("DocMap: " + s);
	}


	/**
	 *  Description of the Method
	 *
	 * @param  s  Description of the Parameter
	 */
	protected final void prtln(String s) {
		if (debug) {

			while (s.length() > 0 && s.charAt(0) == '\n') {
				System.out.println("");
				s = s.substring(1);
			}

			System.out.println("DocMap: " + s);
		}
	}

}

