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
package org.dlese.dpc.standards.commcore;

import org.dom4j.Element;

import org.dlese.dpc.xml.Dom4jUtils;
import org.dlese.dpc.xml.XPathUtils;
import org.dlese.dpc.util.strings.FindAndReplace;
import java.util.regex.*;

import java.util.*;

/**
 *  Extends StdElement to include hierarchy information, such as "children" and
 *  "ancestors"
 *
 * @author    Jonathan Ostwald
 */
public class Standard extends StdElement {

	private static boolean debug = false;

	protected StdDocument stdDoc;
	protected List children = null;
	protected List ancestors = null;


	/**
	 *  Constructor for the Standard object given an XML Element and the
	 *  containing StdDocument instance;
	 *
	 * @param  e       the element defining the Standard
	 * @param  stdDoc  the Document containing this standard
	 */
	public Standard(Element e, StdDocument stdDoc) {
		super(e);
		this.stdDoc = stdDoc;
	}


	/**
	 *  Gets the documentIdentifier attribute of the Standard object
	 *
	 * @return    The documentIdentifier value
	 */
	public String getDocumentIdentifier() {
		return this.stdDoc.getIdentifier();
	}


	/**
	 *  Gets the std attribute of the Standard object
	 *
	 * @param  id  Description of the Parameter
	 * @return     The std value
	 */
	protected Standard getStd(String id) {
		return stdDoc.getStandard(id);
	}


	/**
	 *  Gets the author attribute of the Standard object
	 *
	 * @return    The author value
	 */
	public String getAuthor() {
		return this.stdDoc.getAuthor();
	}


	/**
	 *  Gets the topic attribute of the Standard object
	 *
	 * @return    The topic value
	 */
	public String getTopic() {
		return this.stdDoc.getTopic();
	}


	/**
	 *  Returns true if the Standard object is a leaf
	 *
	 * @return    The leaf value
	 */
	public boolean isLeaf() {
		return getChildren().isEmpty();
	}


	/**
	 *  Returns children as Standard instances in same order as the XML Element
	 *  defining this Standard
	 *
	 * @return    The children value
	 */
	public List getChildren() {
		if (this.children == null) {
			this.children = new ArrayList();
			try {
				Element hasChild = element.element("children");
				if (hasChild == null) {
					// prtln ("Leaf node: " + this.getId());
					return this.children;
				}
				List childElements = hasChild.elements("child");
				for (Iterator i = childElements.iterator(); i.hasNext(); ) {
					Element childElement = (Element) i.next();
					String childId = childElement.getTextTrim();
					// prtln ("childId: " + childId);
					Standard std = this.getStd(childId);
					if (std == null) {
						prtln("WARNING: ASN Document error: std not found for " + childId);
						continue;
					}
					this.addChild(std);
				}
			} catch (Throwable t) {
				prtln("trouble getting children for " + this.getId() + ": " + t.getMessage());
				t.printStackTrace();
				System.exit(1);
			}
		}
		return children;
	}


	/**
	 *  Adds a Child to the Standard object
	 *
	 * @param  std  The feature to be added to the Child attribute
	 */
	protected void addChild(Standard std) {
		if (std == null) {
			return;
		}
		if (!children.contains(std)) {
			children.add(std);
		}
	}


	/**
	 *  Gets the parentStandard attribute of the Standard object
	 *
	 * @return    The parentStandard value
	 */
	public Standard getParentStandard() {
		try {
			return this.stdDoc.getStandard(getParentId());
		} catch (Exception e) {
			prtln("parent standard: " + e.getMessage());
		}
		return null;
	}


	/**
	 *  Removes entityRefs from the provided string
	 *
	 * @param  in  input string
	 * @return     string with entity refs removed
	 */
	public static String removeEntityRefs(String in) {
		// in = SchemEditUtils.contractAmpersands (in);
		Pattern p = Pattern.compile("&[0-9a-zA-Z#]+?;");
		StringBuffer ret = new StringBuffer();
		int ind1 = 0;
		Matcher m = p.matcher(in);

		while (m.find()) {
			int ind2 = m.start();

			ret.append(in.substring(ind1, ind2));

			in = ";" + in.substring(ind2 + m.group().length());
			ind1 = 0;
			m = p.matcher(in);
		}
		ret.append(in.substring(ind1));
		return ret.toString();
	}


	/**
	 *  Walk the ancestor list, adding text from each node
	 *
	 * @return    The displayText value
	 */
	public String getDisplayText() {

		String s = "";
		List aList = getAncestors();
		for (int i = 0; i < aList.size(); i++) {
			Standard std = (Standard) aList.get(i);
			s += std.getItemText();
			s += ": ";
		}
		s += this.getItemText();

		s = FindAndReplace.replace(s, "<br>", "\n", true);
		return removeEntityRefs(s);
	}


	/**
	 *  Gets an ordered list of ancestors from root to this standard's parent
	 *
	 * @return    The ancestors value
	 */
	public List getAncestors() {
		if (ancestors == null) {
			ancestors = new ArrayList();
			Standard marker = getStd(getParentId());
			while (marker != null) {
				ancestors.add(marker);
				marker = getStd(marker.getParentId());
			}
			Collections.reverse(ancestors);
		}
		return ancestors;
	}


	/**
	 *  Gets the level attribute of the Standard object
	 *
	 * @return    The level value
	 */
	public int getLevel() {
		return getAncestors().size() + 1;
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @return    NOT YET DOCUMENTED
	 */
	public String toString() {
		// return Dom4jUtils.prettyPrint (element);
		String s = "\n" + getId();
		s += "\n\t" + "level: " + getLevel();
		s += "\n\t" + "parent: " + getParentId();
		s += "\n\t" + "itemText: " + getItemText();
		s += "\n\n\t" + "displayText: " + getDisplayText();
		if (!this.getAncestors().isEmpty()) {
			s += "\n\t" + "Ancestors";
			for (Iterator i = this.getAncestors().iterator(); i.hasNext(); ) {
				Standard anc = (Standard) i.next();
				s += "\n\t\t" + anc.getId();
			}
		}
		if (!this.getChildren().isEmpty()) {
			s += "\n\t" + "Children";
			for (Iterator i = this.getChildren().iterator(); i.hasNext(); ) {
				Standard anc = (Standard) i.next();
				s += "\n\t\t" + anc.getId();
			}
		}
		return s;
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  s  NOT YET DOCUMENTED
	 */
	private static void prtln(String s) {
		if (debug) {
			System.out.println("Standard: " + s);
		}
	}
}

