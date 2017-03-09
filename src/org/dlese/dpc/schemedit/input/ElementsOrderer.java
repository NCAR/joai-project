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

import org.dlese.dpc.schemedit.MetaDataFramework;
import org.dlese.dpc.schemedit.SchemEditUtils;

import org.dlese.dpc.xml.schema.*;
import org.dlese.dpc.xml.*;
import org.dlese.dpc.util.Files;

import java.util.*;
import java.io.*;
import java.text.ParseException;
import java.net.MalformedURLException;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Attribute;
import org.dom4j.Node;

/**
 *  Orders instance doc elements based an "ordering" attribute. For example, the
 *  following elements
 *  <li> &lt;foo order="2" key="blue" /&gt;
 *  <li> &lt;foo order="1" key="red" /&gt; would be ordered as:
 *  <li> &lt;foo order="1" key="red" /&gt;
 *  <li> &lt;foo order="2" key="blue" /&gt; based on an orderering attribute of
 *  "order".<p>
 *
 *
 *
 * @author    ostwald
 */
public class ElementsOrderer {

	private static boolean debug = false;

	/**  the schemaHelper provided schema info */
	private String orderSpec;
	private String ordererNodeName;
	private String elementPathToOrder;
	private String elementNameToOrder;
	private Document doc = null;


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  doc        NOT YET DOCUMENTED
	 * @param  orderSpec  NOT YET DOCUMENTED
	 */
	public static void orderElements(Document doc, String orderSpec) {
		ElementsOrderer orderer = new ElementsOrderer(doc, orderSpec);
		try {
			orderer.processDoc();
		} catch (Exception e) {
			prtlnErr("ElementsOrderer processing Error: " + e);
		}
	}


	/**
	 *  Constructor for the ElementsOrderer object given an instance Document and
	 *  an orderSpec.<p>
	 *
	 *  The orderSpec is an xpath with an attribute as the leaf, and is interpreted
	 *  as follows:
	 *  <li> The attribute name of the xpath specifieds the orderingAttribute,
	 *  e.g., "//farb/@foo", the "foo" attribute is used to compute an order.
	 *  <li> The parent of the orderSpec xpath determines which nodes are to be
	 *  sorted. If the nodeName is a wildcard, then all the elements at that level
	 *  are sorted. If the nodeName is a string, then only elements of that name
	 *  will be sorted.<p>
	 *
	 *  The actual sorting is performed by {@link ElementOrderComparator}.
	 *
	 * @param  doc        Description of the Parameter
	 * @param  orderSpec  an expath that specified the ordering attribute as well
	 *      as the elements to be ordered.
	 */
	private ElementsOrderer(Document doc, String orderSpec) {
		this.orderSpec = orderSpec;
		this.ordererNodeName = XPathUtils.getNodeName(orderSpec);
		this.elementPathToOrder = XPathUtils.getParentXPath(orderSpec);
		this.elementNameToOrder = XPathUtils.getNodeName(elementPathToOrder);
		this.doc = doc;
		prtln("orderSpec: " + orderSpec);
		prtln("ordererNodeName: " + ordererNodeName);
		prtln("elementPathToOrder: " + elementPathToOrder);
		prtln("elementNameToOrder: " + elementNameToOrder);
	}


	/**
	 *  Finds all elements that the orderSpec can apply to, determines the ordering
	 *  operations required, and orders each one.<p>
	 *
	 *  For example, if the orderSpec is "//contents/@order" there may be several
	 *  tag sets containing "contents" selected by the xpath, and each must be
	 *  sorted individually.
	 *
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	private void processDoc() throws Exception {
		prtln("\nprocessDoc with " + orderSpec);

		// Find all the elements throughout the document that might be reordered
		List orderNodes = null;
		if ("*".equals(elementNameToOrder)) {
			// we take all nodes
			orderNodes = doc.selectNodes(elementPathToOrder);
		}
		else {
			orderNodes = doc.selectNodes(orderSpec);
		}

		prtln(orderNodes.size() + " order nodes found");
		if (orderNodes.size() == 0) {
			return;
		}

		/*
			orderedElementPaths - identify the separate groups that will be
			ordered (e.g., the orderNodes might contain members from different
			parent elements, which we will want to sort separately).
		*/
		List orderedElementPaths = new ArrayList();
		for (Iterator i = orderNodes.iterator(); i.hasNext(); ) {
			Node orderNode = (Node) i.next();
			String elPath = null;
			if ("*".equals(elementNameToOrder))
				elPath = XPathUtils.getParentXPath(orderNode.getPath()) + "/*";
			else {
				// we selected by attribute and therefore must take the parent
				elPath = XPathUtils.getParentXPath(orderNode.getPath());
			}
			// prtln ("elPath: " + elPath);
			if (!orderedElementPaths.contains(elPath))
				orderedElementPaths.add(elPath);
		}
		prtln(orderedElementPaths.size() + " orderedElementPaths found");

		for (Iterator i = orderedElementPaths.iterator(); i.hasNext(); ) {
			String path = (String) i.next();
			orderElements(path);
		}
	}


	/**
	 *  Reorder the specified elements within the parent specified by
	 *  orderedElementPath.<p>
	 *
	 *  Reordering is accomplished by building a list of ordered nodes, and then
	 *  replacing the elements to be ordered in the instance doc with the elements
	 *  from the ordered list.
	 *
	 * @param  orderedElementPath  an xpath to an element containing elements to be
	 *      ordered
	 */
	private void orderElements(String orderedElementPath) {

		prtln("\norderElements: " + orderedElementPath);
		List orderedNodes = getOrderedElements(orderedElementPath);
		prtln(orderedNodes.size() + " nodes found");
		if (orderedNodes.size() < 2)
			return;

		prtln("orderedElementPath: " + orderedElementPath);
		Element parent = (Element) doc.selectSingleNode(XPathUtils.getParentXPath(orderedElementPath));
		if (parent == null) {
			prtln("WARNING: parent node not found");
			return;
		}
		List children = null;
		if ("*".equals(elementNameToOrder))
			children = parent.elements(); // does "*" get all children?
		else
			children = parent.elements(elementNameToOrder); // does "*" get all children?
		replaceNamedNodes(orderedNodes, children, elementNameToOrder);

	}


	/**
	 *  Gets the orderedElements attribute of the ElementsOrderer object
	 *
	 * @param  xpath  NOT YET DOCUMENTED
	 * @return        The orderedElements value
	 */
	private List getOrderedElements(String xpath) {
		prtln("orderAllElements: " + xpath);
		List orderedElements = doc.selectNodes(xpath);
		prtln(orderedElements.size() + " nodes found");
		if (orderedElements.size() > 1)
			Collections.sort(orderedElements, new ElementOrderComparator(ordererNodeName));
		return orderedElements;
	}


	/**
	 *  Reordering is accomplished by building a list of ordered nodes, and then
	 *  replacing the elements to be ordered in the instance doc with the elements
	 *  from the ordered list.<p>
	 *
	 *  The "replaceNodeName" parameter specifies whether ALL elements are to be
	 *  ordered, or only those with specific nodeName
	 *
	 * @param  orderedNodes     list of elements in order
	 * @param  children         list of unordered instance doc elements
	 * @param  replaceNodeName  either "*" or a nodeName
	 */
	private void replaceNamedNodes(List orderedNodes, List children, String replaceNodeName) {
		// String nodeName = XPathUtils.getNodeName(this.orderedElementPath);

		prtln("\nreplaceNamedNodes()");
		prtln("  orderedNodes: " + orderedNodes.size());
		prtln("  children: " + children.size());
		prtln("  replaceNodeName: " + replaceNodeName);

		Iterator ordered = orderedNodes.iterator();
		int replaceIndex = 0;
		for (int i = 0; i < children.size(); i++) {
			Element child = (Element) children.get(i);
			if ("*".equals(replaceNodeName) || replaceNodeName.equals(child.getName())) {
				String c_order = child.attributeValue(ordererNodeName, "N/A");
				prtln(i + " looking at " + child.attributeValue("title", "N/A") + " (" + c_order + ")");
				children.remove(i);
				Element replacement = (Element) ordered.next();
				String r_order = replacement.attributeValue(ordererNodeName, "N/A");
				prtln("\t replacing with " + replacement.attributeValue("title", "N/A")
					 + " (" + r_order + ")");
				if (children.size() == i)
					children.add(replacement.createCopy());
				else
					children.add(i, replacement.createCopy());
			}
		}
	}


	static void osmTest() throws Exception {
		String xmlPath = "C:/tmp/osm-tester.xml";
		String xml = Files.readFile(xmlPath).toString();
		Document record = Dom4jUtils.localizeXml(Dom4jUtils.getXmlDocument(xml));

		ElementsOrderer.orderElements(record, "/record/general/asset/@order");
		ElementsOrderer.orderElements(record, "/record/contributors/*/@order");

		prtln("\n ORDERER");
		prtln(Dom4jUtils.prettyPrint(record));
	}


	static void conceptTest() throws Exception {
		String xmlPath = "C:/tmp/concept-tester.xml";
		String xml = Files.readFile(xmlPath).toString();
		Document record = Dom4jUtils.localizeXml(Dom4jUtils.getXmlDocument(xml));
		ElementsOrderer orderer = new ElementsOrderer(record, "//contents/*/@num");
		orderer.processDoc();
		orderer = new ElementsOrderer(record, "//hierarchy/*/@num");
		orderer.processDoc();
		orderer = new ElementsOrderer(record, "//relations/*/@num");
		orderer.processDoc();
		prtln("\n ORDERER");
		prtln(Dom4jUtils.prettyPrint(orderer.doc));
	}


	/**
	 *  The main program for the ElementsOrderer class
	 *
	 * @param  args           The command line arguments
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public static void main(String[] args) throws Exception {
		org.dlese.dpc.schemedit.test.TesterUtils.setSystemProps();

		osmTest();
		// conceptTest();
	}


	/**
	 *  Print a line to standard out.
	 *
	 * @param  s  The String to print.
	 */
	private static void prtln(String s) {
		if (debug) {
			// SchemEditUtils.prtln(s, "ElementsOrderer");
			SchemEditUtils.prtln(s, "");
		}
	}


	private static void prtlnErr(String s) {
		SchemEditUtils.prtln(s, "ElementsOrderer");
	}


	/**
	 *  Comparator to order elements, using a specified "order" attribute.
	 *
	 * @author    ostwald
	 */
	public class ElementOrderComparator implements Comparator {

		String orderAttrName;


		/**
		 *  Constructor for the ElementOrderComparator object
		 *
		 * @param  orderAttrName  NOT YET DOCUMENTED
		 */
		public ElementOrderComparator(String orderAttrName) {
			this.orderAttrName = orderAttrName;
		}


		/**
		 *  Get order num based on the named attribute, returning a BIG num if there
		 *  is no attribute, or if the value cannot be parsed as an integet greater
		 *  than 0 (1 is the smallest meaningful order number).
		 *
		 * @param  o  NOT YET DOCUMENTED
		 * @return    The order value
		 */
		private Integer getOrderNum(Object o) {
			String orderAttrPath = ((Element) o).attributeValue(orderAttrName, null);
			try {
				int order = Integer.valueOf(orderAttrPath);
				if (order < 1)
					throw new Exception("order is less than 1");
				return order;
			} catch (Exception e) {
				//System.out.println ("getOrder error for \"" + orderAttrPath + "\": " + e.getMessage());
			}
			return Integer.MAX_VALUE;
		}


		/**
		 *  Compare two asset Elements which may or not have an "order" attribute
		 *
		 * @param  o1  element 1
		 * @param  o2  element 2
		 * @return     comparison of "order"
		 */
		public int compare(Object o1, Object o2) {

			return getOrderNum(o1).compareTo(getOrderNum(o2));
		}
	}

}

