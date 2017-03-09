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
package org.dlese.dpc.schemedit.standards.adn;

import org.dlese.dpc.schemedit.standards.StandardsDocument;
import org.dlese.dpc.schemedit.standards.adn.util.MappingUtils;

import org.dlese.dpc.xml.schema.*;
import org.dlese.dpc.schemedit.*;

import java.io.*;
import java.util.*;

import java.net.*;

/**
 *  StandardsDocument for the ADN Framework. Manages standards represented as
 *  ":"-delimited strings.
 *
 * @author    ostwald
 */
public class DleseStandardsDocument implements StandardsDocument {
	private static boolean debug = true;

	private SchemaHelper schemaHelper;
	String version;

	List standards = null;
	Map standardsMap = new HashMap();
	List dataTypes = new ArrayList();
	DleseStandardsNode rootNode = null;
	List nodeList = new ArrayList();
	int maxNodes = Integer.MAX_VALUE;
	String xpath = "";


	/**
	 *  Constructor for the DleseStandardsDocument object
	 *
	 * @param  schemaHelper   NOT YET DOCUMENTED
	 * @param  xpath          NOT YET DOCUMENTED
	 * @param  dataTypeName   NOT YET DOCUMENTED
	 * @exception  Exception  NOT YET DOCUMENTED
	 */

	public DleseStandardsDocument(SchemaHelper schemaHelper, String dataTypeName)
		 throws Exception {
		this.schemaHelper = schemaHelper;
		List dataTypeNames = new ArrayList();
		dataTypeNames.add(dataTypeName);
		dataTypes = makeDataTypesList(dataTypeNames);
		init();
	}


	/**
	 *  Constructor for the DleseStandardsDocument object
	 *
	 * @param  schemaHelper   NOT YET DOCUMENTED
	 * @param  xpath          NOT YET DOCUMENTED
	 * @param  dataTypeNames  NOT YET DOCUMENTED
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public DleseStandardsDocument(SchemaHelper schemaHelper, List dataTypeNames)
		 throws Exception {
		this.schemaHelper = schemaHelper;
		dataTypes = makeDataTypesList(dataTypeNames);
		init();
	}
	
	/**
	 *  Returns "National Science Education Standards (NSES)"
	 *
	 * @return    The author value
	 */
	 public String getAuthor() {
		return "National Science Education Standards (NSES)";
	}


	/**
	 *  Returns "Science"
	 *
	 * @return    The topic value
	 */
 	public String getTopic() {
		return "Science";
	}

	
	/**
	 *  Get a StandardNode by id
	 *
	 * @param  id  NOT YET DOCUMENTED
	 * @return     The standard value
	 */
	public DleseStandardsNode getStandard(String id) {
		return (DleseStandardsNode) standardsMap.get(id);
	}

	public DleseStandardsNode getRootNode () {
		return rootNode;
	}
	
	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @return    NOT YET DOCUMENTED
	 */
	public int size() {
		return getStandards().size();
	}
	
	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  dataTypeNames  NOT YET DOCUMENTED
	 * @return                NOT YET DOCUMENTED
	 */
	private List makeDataTypesList(List dataTypeNames) {
		List types = new ArrayList();
		for (Iterator i = dataTypeNames.iterator(); i.hasNext(); ) {
			String typeName = (String) i.next();
			GlobalDef typeDef = schemaHelper.getGlobalDef(typeName);
			types.add(typeDef);
		}
		return types;
	}


	/**
	 *  Initialize the DleseStandardsDocument by populating the standardsMap and tree
	 *
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	private void init() throws Exception {
		if (schemaHelper == null)
			throw new Exception("SchemaHelper not initialized");

		version = schemaHelper.getVersion();

		standards = getStandards();
		rootNode = makeStandardsDocument();
		if (rootNode != null)
			nodeList = getNodeList(rootNode.getSubList());
		prtln("DleseStandardsDocument instantiated");
		prtln("\tDataTypes:");
		for (Iterator i = dataTypes.iterator(); i.hasNext(); ) {
			GlobalDef def = (GlobalDef) i.next();
			prtln("\t\t" + def.getName());
		}
	}


	/*
	* Limits the size of the standards to be processed, mostly useful for debugging
	*/
	/**
	 *  Sets the maxNodes attribute of the DleseStandardsDocument object
	 *
	 * @param  max  The new maxNodes value
	 */
	public void setMaxNodes(int max) {
		maxNodes = max;
	}


	/**
	 *  Gets the maxNodes attribute of the DleseStandardsDocument object
	 *
	 * @return    The maxNodes value
	 */
	public int getMaxNodes() {
		return maxNodes;
	}

	/**
	 *  Create a hierarchical tree of DleseStandardsNodes by splitting up the
	 *  Standards and populating the tree one standard at a time.
	 *
	 * @return    Description of the Return Value
	 */
	private DleseStandardsNode makeStandardsDocument() {
		DleseStandardsNode root = new DleseStandardsNode("root educational standards node", null, "root");
		for (Iterator i = getStandards().iterator(); i.hasNext(); ) {
			AdnStandard std = (AdnStandard) i.next();
			DleseStandardsNode parent = root;
			String id = "";
			for (int lev = 0; lev < std.getLevels(); lev++) {
				String name = std.getSplit(lev);
				if (lev > 0)
					id += ":";
				id += name;
				DleseStandardsNode child = parent.getChild(name);
				if (child == null) {
					child = new DleseStandardsNode(name, parent, id);
					standardsMap.put(id, child);
					if (lev == std.getLevels() - 1)
						child.setFullText(std.getText());
					parent.addSubNode(child);
					// prtln ("added \n\tchild: " + child.getName() + "\n\tto parent: " + parent.getName());
				}
				parent = child;
			}
		}
		return root;
	}



	/**
	 *  Walk down a hierarchical tree of DleseStandardsNodes and print an indented
	 *  display
	 */
	public void printStandardsDocument() {
		prtln("STANDARD STREE ---------------------");
		printSubList(rootNode, "");
	}


	/**
	 *  Utility method to display the items of a subList.
	 *
	 * @param  node    Description of the Parameter
	 * @param  indent  Description of the Parameter
	 */
	void printSubList(DleseStandardsNode node, String indent) {
		// prtln ("printSubList with " + node.getName());
		prtln(indent + node.getName());
		// prtln ("node has " + node.getSubList().size() + " children");
		if (node.getHasSubList()) {
			for (Iterator i = node.getSubList().iterator(); i.hasNext(); ) {
				printSubList((DleseStandardsNode) i.next(), indent + "-");
			}
		}
	}


	/**
	 *  Returns a flat list containing all DleseStandardsNodes in the standardsTree.
	 *
	 * @return    The nodeList value
	 */
	public List getNodeList() {
		return nodeList;
	}


	/**
	 *  Flattens the hierarchy under the given DleseStandardsDocument list, and
	 *  returns it as a flat arrayList.
	 *
	 * @param  nodes     NOT YET DOCUMENTED
	 * @return           The vocabList value
	 */
	private ArrayList getNodeList(List nodes) {
		ArrayList ret = new ArrayList();
		for (int i = 0; i < nodes.size(); i++) {
			DleseStandardsNode addNode = (DleseStandardsNode) nodes.get(i);
			List sublist = addNode.getSubList();
			ret.add(addNode);
			if (sublist.size() > 0) {
				ret.addAll(getNodeList(sublist));
			}
		}
		return ret;
	}

	/**  Description of the Method  */
	public void printNodeList() {
		prtln("NODE LIST ---------------------");
		for (int i = 0; i < nodeList.size() && i < 1000; i++) {
			DleseStandardsNode node = (DleseStandardsNode) nodeList.get(i);
			String lastIndicator = "";
			if (node.getIsLastInSubList()) {
				lastIndicator = "   _|";
			}
			String leafIndicator = "";
			if (node.getFullText().length() > 0) {
				leafIndicator = " * ";
			}
			prtln("(" + node.getLevel() + ") " + leafIndicator + node.getName() + lastIndicator);

		}
	}
	




	/**  Description of the Method  */
	void showStandards() {
		List standards = getStandards();
		for (int i = 0; i < standards.size() && i < 50; i++) {
			AdnStandard std = (AdnStandard) standards.get(i);
			prtln("(" + std.getLevels() + ")  " + std.getLeaf());
		}
	}


	/**
	 *  Debugging method maps standards to their "level" (the number of
	 *  colon-delimited fields in the vocabItem's textual representation.
	 *
	 * @return    The levelMap value
	 */
	public Map getLevelMap() {
		Map levelMap = new HashMap();
		List standards = getStandards();
		for (int i = 0; i < standards.size(); i++) {
			AdnStandard std = (AdnStandard) standards.get(i);
			Integer levels = new Integer(std.getLevels());
			List list = new ArrayList();
			if (levelMap.containsKey(levels)) {
				list = (List) levelMap.get(levels);
			}
			list.add(std);
			levelMap.put(levels, list);
		}
		return levelMap;
	}


	/**
	 *  Gets the standards attribute of the DleseStandardsDocument object
	 *
	 * @return    The standards value
	 */
	private List getStandards() {
		return getStandards(maxNodes);
	}


	/**
	 *  Gets the standards attribute of the DleseStandardsDocument object
	 *
	 * @param  maxSize  Description of the Parameter
	 * @return          The standards value
	 */
	private List getStandards(int maxSize) {
		if (standards == null) {
			// String typeName = "union.contentStandardType";
			List vocabItems = new ArrayList();
			for (Iterator i = dataTypes.iterator(); i.hasNext(); ) {
				GlobalDef globalDef = (GlobalDef) i.next();
				if (globalDef.isSimpleType())
					vocabItems.addAll(((SimpleType) globalDef).getEnumerationValues(false));
			}
			standards = new ArrayList();
			for (Iterator i = vocabItems.iterator(); i.hasNext(); ) {
				String vocab = (String) i.next();
				AdnStandard std = new AdnStandard(vocab);
				standards.add(std);
				if (standards.size() > maxSize) {
					break;
				}
			}
		}
		return standards;
	}

	
	/**
	 *  The main program for the DleseStandardsDocument class
	 *
	 * @param  args  The command line arguments
	 */
	public static void main(String[] args) {

		String schemaURLStr = "http://www.dlese.org/Metadata/adn-item/0.7.00/record.xsd";
		String xpath = "/itemRecord/educational/contentStandards/contentStandard";
		SchemaHelper sh = MappingUtils.getSchemaHelper(schemaURLStr, "itemRecord");
		List dataTypeNames = new ArrayList();
		dataTypeNames.add("NCGEgeographyContentStandardsType");
		dataTypeNames.add("NSESscienceContentStandardsAllType");

		DleseStandardsDocument t = null;
		try {
			// t.setMaxNodes (101);
			t = new DleseStandardsDocument(sh, dataTypeNames);
		} catch (Exception e) {
			prtln("initialization error: " + e.getMessage());
			e.printStackTrace();
			return;
		}

		// t.printNodeList();
		t.printStandardsDocument();

		// String item = "NSES:K-4:Unifying Concepts and Processes Standards:Systems, order, and organization";
		// String item = "NSES:5-8:Content Standard D Earth and Space Science Standards:Structure of the earth system:Land forms are the result of a combination of constructive and destructive forces. Constructive forces include crustal deformation, volcanic eruption, and deposition of sediment, while destructive forces include weathering and erosion.";
		String item = "NSES:5-8:Content Standard D Earth and Space Science Standards:Earth's history:The earth processes we see today, including erosion, movement of lithospheric plates, and changes in atmospheric composition, are similar to those that occurred in the past. Earth history is also influenced by occasional catastrophes, such as the impact of an asteroid or comet.";

		DleseStandardsNode n = t.getStandard(item);

		// prtln("DleseStandardsNode");
		// prtln("\tfullText: " + n.getFullText());
		// prtln (n.toString());

		/* 		List ancestors = n.getAncestors();
		prtln (ancestors.size() + " ancestors found for \t" + item);
		for (Iterator i=ancestors.iterator();i.hasNext();) {
			DleseStandardsNode a = (DleseStandardsNode)i.next();
			prtln ("\t" + a.getId() + " (" + a.getLevel() + ")");
		} */
	}
	
	/**
	* Set all data structures for this DleseStandardsDocument to null
	*/
	public void destroy () {
		this.standards = null;
		this.standardsMap = null;
		this.nodeList = null;
	}

	/**
	 *  Description of the Method
	 *
	 * @param  s  Description of the Parameter
	 */
	private static void prtln(String s) {
		if (debug)
			SchemEditUtils.prtln(s, "DleseStandardsDocument");
	}
}

