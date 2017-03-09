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

import java.util.*;
import java.io.*;

import org.dlese.dpc.schemedit.SchemEditUtils;
import org.dlese.dpc.schemedit.autoform.RendererHelper;
import org.dlese.dpc.schemedit.display.CollapseUtils;
import org.dlese.dpc.xml.schema.SchemaHelper;
import org.dlese.dpc.xml.schema.SchemaNodeMap;
import org.dlese.dpc.xml.XPathUtils;

import org.apache.struts.action.ActionError;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import javax.servlet.http.HttpServletRequest;

/**
 *  Subclasses ActionErrors to maintain certain error-types in document order to
 *  improve readibility of error displays in UI.
 *
 * @author    Jonathan Ostwald
 */
public class SchemEditActionErrors extends ActionErrors {

	private static boolean debug = false;
	private SchemaNodeMap schemaNodeMap;
	private Comparator schemaOrder;
	private List sortedProperties;


	/**  No-argument Constructor for the SchemEditActionErrors object */
	public SchemEditActionErrors() {
		this(null);
	}


	/**
	 *  Constructor for the SchemEditActionErrors object with SchemaHelper
	 *  argument, which is needed to maintain errors in document order.
	 *
	 * @param  schemaHelper  the schemaHelper
	 */
	public SchemEditActionErrors(SchemaHelper schemaHelper) {
		super();
		if (schemaHelper != null) {
			setSchemaHelper(schemaHelper);
		}
		sortedProperties = new ArrayList();
		sortedProperties.add("pageErrors");
		sortedProperties.add("entityErrors");
	}


	/**
	 *  Sets the schemaHelper attribute of the SchemEditActionErrors object
	 *
	 * @param  schemaHelper  The new schemaHelper value
	 */
	public void setSchemaHelper(SchemaHelper schemaHelper) {
		if (schemaHelper != null) {
			schemaNodeMap = schemaHelper.getSchemaNodeMap();
			schemaOrder = schemaNodeMap.new DocOrderComparator();
		}
	}


	/**
	 *  Add an error, and sort into document order if schemaHelper is present and
	 *  if specified propery is contained in sortedProperties list.
	 *
	 * @param  property  error property
	 * @param  message   error object
	 */
	public void add(String property, ActionMessage message) {
		ActionMessageItem item = (ActionMessageItem) messages.get(property);
		List list = null;

		if (item == null) {
			list = new ArrayList();
			item = new ActionMessageItem(list, iCount++, property);

			messages.put(property, item);
		}
		else {
			list = item.getList();
		}

		list.add(message);
		if (property != null && this.sortedProperties.contains(property)) {
			Collections.sort(list, new DocOrderComparator());
		}
	}



	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @return    NOT YET DOCUMENTED
	 */
	public Iterator get_Off() {
		// prtln("\nget()");
		this.accessed = true;

		if (messages.isEmpty()) {
			return Collections.EMPTY_LIST.iterator();
		}

		ArrayList results = new ArrayList();
		ArrayList actionItems = new ArrayList();

		// prtln("\n looping through messages");
		for (Iterator i = messages.values().iterator(); i.hasNext(); ) {
			// prtln("\t" + ((ActionMessage) i.next()).getKey());
			actionItems.add(i.next());
		}

		// Sort ActionMessageItems based on the initial order the
		// property/key was added to ActionMessages.
		Collections.sort(actionItems, new DocOrderComparator());

		for (Iterator i = actionItems.iterator(); i.hasNext(); ) {
			ActionMessageItem ami = (ActionMessageItem) i.next();

			for (Iterator messages = ami.getList().iterator(); messages.hasNext(); ) {
				results.add(messages.next());
			}
		}

		return results.iterator();
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  property  NOT YET DOCUMENTED
	 * @return           NOT YET DOCUMENTED
	 */
	public Iterator get_Off(String property) {
		// prtln("\nget(" + property + ")");
		this.accessed = true;

		ActionMessageItem item = (ActionMessageItem) messages.get(property);

		if (item == null) {
			return (Collections.EMPTY_LIST.iterator());
		}
		else {
			if (property == null || !property.equals("pageErrors")) {
				return (item.getList().iterator());
			}
			else {
				// memebers of sortedList are org.apache.struts.action.ActionMessage (ActionError))
				// pageErrors have the xpath as the second value in the "values" list
				List sortedList = item.getList();
				prtln("pageErrors items (" + sortedList.size() + ")");
				for (Iterator i = sortedList.iterator(); i.hasNext(); ) {
					ActionMessage msg = (ActionMessage) i.next();
					prtln(msg.toString());
				}
				Collections.sort(sortedList, new DocOrderComparator());
				return sortedList.iterator();
			}
		}

	}


	/**
	 *  Compares two "page error" actionMessages wrt to instance document order so
	 *  that editing errors can be presented in the same order in which they occur
	 *  in the document being edited.<p>
	 *
	 *  "Page error" ActionMessages contain their xpath as the second item of their
	 *  "values". NOTE: This is dependent on the message resource used to format the
	 *  errors (see SchemEditValidator & SchemEditErrors).<p>
	 *
	 *  Algorithm: First compare segment by segment: - if there is indexing info at
	 *  the given segment, then compare index numbers - if one elment runs out of
	 *  indexing info before tie is broken, compare by schemaPaths <p>
	 *
	 *  The SchemaNodeMap.DocOrderComparator can determine <i>schema order</i> of
	 *  paths, which can be used to order instance document paths, as long as there
	 *  are no indexes (since indexing information is removed from the schema
	 *  paths). <p>
	 *
	 *  If two paths are judged equal by schema order, then they can be simply
	 *  compared as strings to bring the indexing information in and resolve the
	 *  tie.
	 *
	 * @author    Jonathan Ostwald
	 */
	public class DocOrderComparator implements Comparator {

		/**
		 *  Gets the xPath attribute of the DocOrderComparator object
		 *
		 * @param  msg  NOT YET DOCUMENTED
		 * @return      The xPath value
		 */
		private String getXPath(ActionMessage msg) {
			if (msg.getValues().length > 1) {
				String id = (String) msg.getValues()[1];
				return XPathUtils.decodeXPath(CollapseUtils.idToPath(id));
			}
			else {
				// what should we return here??
				return "ZZ";
			}
		}


		/**
		 *  Compare two paths segment by segment. - if a segment only differs by index
		 *  (e.g., [1] vs [2], then make comparison based on the index - if segments
		 *  are different leafNames, then return a 0 so the paths will be compared by
		 *  schemaPath.
		 *
		 * @param  p1  NOT YET DOCUMENTED
		 * @param  p2  NOT YET DOCUMENTED
		 * @return     NOT YET DOCUMENTED
		 */
		private int pathCmp(String p1, String p2) {
			String[] s1 = p1.split("/");
			String[] s2 = p2.split("/");
			int cmp;
			for (int i = 0; i < s1.length && i < s2.length; i++) {
				String seg1 = s1[i];
				String seg2 = s2[i];
				// prtln ("\tseg1: " + seg1);
				// prtln ("\tseg2: " + seg2);
				String name1 = XPathUtils.getNodeName(seg1);
				String name2 = XPathUtils.getNodeName(seg2);
				if (name1.equals(name2)) {
					int i1 = XPathUtils.getIndex(seg1);
					int i2 = XPathUtils.getIndex(seg2);
					if (i1 < i2) {
						// prtln ("\t\t" + i1 + " < " + i2);
						return -1;
					}
					if (i1 > i2) {
						// prtln ("\t\t" + i1 + " > " + i2);
						return 1;
					}
					// prtln ("\t\t ... indexes are the same");
				}
				else {
					// the segment names were different, let schemaOrder compare
					return 0;
				}
			}
			// we haven't resolved order, let schemaOrder compare
			return 0;
		}



		/**
		 *  sorts by order in which paths are processed by StructureWalker (and
		 *  therefore are added to the SchemaNodeMap)
		 *
		 * @param  o1  NOT YET DOCUMENTED
		 * @param  o2  NOT YET DOCUMENTED
		 * @return     NOT YET DOCUMENTED
		 */
		public int compare(Object o1, Object o2) {

			String path1 = getXPath((ActionMessage) o1);
			String path2 = getXPath((ActionMessage) o2);
			prtln("paths: \n\t 1 - " + path1 + "\n\t 2 - " + path2);

			int cmp = pathCmp(path1, path2);
			prtln("\t pathCmp returned " + cmp);
			if (cmp != 0) {
				return cmp;
			}

			if (schemaOrder != null) {
				cmp = schemaOrder.compare(getSchemaCmpPath(path1),
					getSchemaCmpPath(path2));
			}
			else {
				cmp = path1.compareTo(path2);
			}

			prtln("\t ... schemaOrder.compare: " + cmp);

			return cmp;
		}


		/**
		 *  Attributes do not have the proper key number in schemaNodeMap, so just use
		 *  the parent element path instead for attributes.
		 *
		 * @param  path  NOT YET DOCUMENTED
		 * @return       The schemaCmpPath value
		 */
		private String getSchemaCmpPath(String path) {
			String schemaPath = SchemaHelper.toSchemaPath(path);
			if (XPathUtils.isAttributePath(schemaPath))
				return XPathUtils.getParentXPath(schemaPath);
			return schemaPath;
		}

	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  s  NOT YET DOCUMENTED
	 */
	private static void prtln(String s) {
		if (debug) {
			SchemEditUtils.prtln(s, "seActionErrors");
		}
	}
}


