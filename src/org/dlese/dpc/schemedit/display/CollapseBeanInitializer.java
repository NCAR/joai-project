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
package org.dlese.dpc.schemedit.display;

import org.dlese.dpc.schemedit.MetaDataFramework;
import org.dlese.dpc.schemedit.SchemEditUtils;

import org.dlese.dpc.xml.schema.SchemaHelper;
import org.dlese.dpc.xml.schema.SchemaNode;
import org.dlese.dpc.xml.schema.compositor.Compositor;
import org.dlese.dpc.xml.schema.GlobalDef;
import org.dlese.dpc.xml.XPathUtils;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Node;
import java.util.*;

/**
 *  Class to initialize a {@link CollapseBean} which controls the visibility of
 *  fields in the metadata editor. All fields that are deeper than a threshold
 *  level are opened, while those shallower are closed.
 *
 *@author    Jonathan Ostwald
 */
public class CollapseBeanInitializer {
	private boolean debug = false;

	CollapseBean collapseBean = null;
	Document doc = null;
	SchemaHelper sh = null;
	MetaDataFramework framework = null;
	int baseLevel;
	int THRESHOLD_LEVEL = 1;
	List initializedNodes;


	/**
	 *  Constructor for the CollapseBeanInitializer object
	 *
	 *@param  collapseBean  collapseBean to be initialized
	 *@param  doc           instance document
	 *@param  framework     framework of instance document
	 */
	public CollapseBeanInitializer(CollapseBean collapseBean, Document doc, MetaDataFramework framework) {
		this(collapseBean, doc, framework.getBaseRenderLevel(), framework);
	}


	/**
	 *  Constructor for the CollapseBeanInitializer object
	 *
	 *@param  collapseBean  collapseBean to be initialized
	 *@param  doc           instance document
	 *@param  baseLevel     Configured baseLevel for the framework - used to
	 *      compute relative "level"
	 *@param  framework     Description of the Parameter
	 */
	public CollapseBeanInitializer(CollapseBean collapseBean, Document doc, int baseLevel, MetaDataFramework framework) {
		this.collapseBean = collapseBean;
		this.doc = doc;
		this.baseLevel = baseLevel;
		this.framework = framework;
		this.sh = framework.getSchemaHelper();
		this.initializedNodes = new ArrayList();
	}


	/**
	 *  Returns the level of a path relative to the baseLevel, which is specified
	 *  by the metadataFramework.
	 *
	 *@param  xpath      NOT YET DOCUMENTED
	 *@param  baseLevel  NOT YET DOCUMENTED
	 *@return            The level value
	 */
	private int getLevel(String xpath, int baseLevel) {
		String[] splits = xpath.split("/");
		return splits.length - baseLevel;
	}


	/**
	 *  Initializes visiblity of elements in the metadata editor. Currently,
	 *  expands all nodes except those at a level below THRESHOLD_LEVEL.
	 */
	public void init() {
		collapseBean.clear();
		Element root = doc.getRootElement();
		initializePath(root.getPath());
	}


	/**
	 *  Sets displayState of specified element in collapseBean to open
	 *
	 *@param  encodedXPath  encoded xpath specifying element to be opened
	 */
	private void openElement(String encodedXPath) {
		String id = CollapseUtils.pathToId(encodedXPath);
		collapseBean.openElement(id);
	}


	/**
	 *  Sets displayState of specified element in collapseBean to closed
	 *
	 *@param  encodedXPath  encoded xpath specifying element to be closed
	 */
	private void closeElement(String encodedXPath) {
		String id = CollapseUtils.pathToId(encodedXPath);
		collapseBean.closeElement(id);
	}


	/**
	 *  Set the displayState of the collapseBean node specified by "path"<p>
	 *
	 *  Queries MetaDataFramework for configured custom initial display states (we
	 *  can now specify whether a field is initially open or not in the
	 *  framework-config using the "initialFieldCollapse" parameter.<p>
	 *
	 *  As of 7/2/209 we are initializing top-level multibox displays as "opened".
	 *
	 *@param  path  xpath specifying a node of the collapseBean
	 */
	private void initializeNode(String path) {
		
		if (this.initializedNodes.contains(path)) {
			// prtln ("already initialized - bailing");
			return;
		}
		else {
			prtln ("initializeNode: " + path);
			this.initializedNodes.add (path);
		}
		
		if (path.startsWith("_^_nsdl_anno_^_structuredOutline_1__^_outline"))
			prtln ("ya baby");
		String encodedXPath = XPathUtils.encodeXPath(path);

		boolean isRepeatingElement = sh.isRepeatingElement(encodedXPath);

		int level = getLevel(path, baseLevel);

		SchemaNode schemaNode = sh.getSchemaNode(path);
		String customInitialState = this.framework.getInitialFieldCollapse(path);

		if (customInitialState != null) {
			if ("open".equals(customInitialState)) {
				openElement(encodedXPath);
				this.collapseBean.exposeElement(encodedXPath);
			} else if ("closed".equals(customInitialState)) {
				closeElement(encodedXPath);
			}
		} // open top-level multiSelects by default
		else if (level == 1 && schemaNode != null && 
			(sh.isMultiSelect(schemaNode) || sh.isChoiceElement(schemaNode) || sh.isMultiChoiceElement(schemaNode))) {
			openElement(encodedXPath);
		} // Default initialization state
		else if (level > THRESHOLD_LEVEL) {
			openElement(encodedXPath);
		} else {
			closeElement(encodedXPath);
		}
	}


	/**
	 *  Set the displayState for the given path, and then attempt to set it for the
	 *  children recursively. All paths that are deeper than a threshold level are
	 *  opened, while those shallower are closed.
	 *
	 *@param  path  specifies node of collapseBean
	 */
	private void initializePath(String path) {
		prtln ("\ninitializePath() " + path);
		Element element = (Element) doc.selectSingleNode(path);
		if (element == null) {
			// node won't be found for AnyType elements (which is okay
			// since they aren't collapsible, anyway
			prtlnErr("Node not found for " + path);
			return;
		}

		try {
			this.initializeNode(path);
			this.initializeSiblings(path);
			this.initializeChildren(path, element);
		} catch (Throwable e) {
			prtlnErr("initializePath error: " + e.getMessage());
			e.printStackTrace();
		}

	}


	/**
	 *  Initialize children of the element specified by path - note: we must
	 *  manually add indexing for repeating elements
	 *
	 *@param  path     xpath to element whose children are to be processed
	 *@param  element  instance document element at path
	 */
	private void initializeChildren(String path, Element element) {
		prtln ("initializeChildren() " + path);
		SchemaNode parentNode = sh.getSchemaNode(path);

		/*
		 *  otherChildrenNames used to collect the member names that are NOT
		 *  present in the instance document
		 */
		List otherChildrenNames = new ArrayList();

		if (parentNode != null && parentNode.hasCompositor()) {
			otherChildrenNames.addAll(parentNode.getCompositor().getLeafMemberNames());
		}

		Iterator elementIter = element.elementIterator();
		String lastChildName = "";
		int index = -1;
		while (elementIter.hasNext()) {
			Element child = (Element) elementIter.next();
			String childName = child.getQualifiedName();
			String childPath = path + "/" + childName;

			if (!lastChildName.equals(childName)) {
				lastChildName = childName;
				index = 1;
			}

			otherChildrenNames.remove(childName);

			SchemaNode schemaNode = sh.getSchemaNode(childPath);
			if (schemaNode != null) {
				boolean doInitialize = true;
				GlobalDef typeDef = sh.getGlobalDef(schemaNode);
				if (typeDef == null) {
					doInitialize = true;
				} else if (typeDef.isComplexType()) {
					doInitialize = true;
				} else if (sh.isMultiSelect(schemaNode)) {
					doInitialize = true;
				}

				// duh - it is ALWAYS true!
				if (!doInitialize) {
					continue;
				}

				// for repeating items we have to add indexing to the path
				if (sh.isRepeatingElement(schemaNode)) {
					childPath += "[" + index++ + "]";
				}
			} else {
				prtlnErr("schemaNode not found at " + childPath);
			}
			// recurse with childPath
			initializePath(childPath);
		}

		// set displayState of otherChildren to open so their "add" controls will be available
		if (!otherChildrenNames.isEmpty()) {
			for (Iterator j = otherChildrenNames.iterator(); j.hasNext(); ) {
				String encodedPath = XPathUtils.encodeXPath(path + "/" + (String) j.next());
				this.openElement(encodedPath);
			}
		}
	}


	/**
	 *  Initialize SIBLINGS that are not in the instance document. necessary to
	 *  ensure that the sibling controls are available so their elements can be
	 *  created if desired. we need only open siblings that do not have a value in
	 *  the instance doc, since elements with values are initialized as part of the
	 *  recursive walk of the instance document.
	 *
	 *@param  path  NOT YET DOCUMENTED
	 */
	private void initializeSiblings(String path) {
		prtln ("initializeSiblings: " + path);
		SchemaNode schemaNode = sh.getSchemaNode(path);
		if (schemaNode == null) {
			return;
		}
		String myLeafName = XPathUtils.getNodeName(path);
		SchemaNode parentSchemaNode = schemaNode.getParent();
		if (parentSchemaNode != null && parentSchemaNode.hasCompositor()) {
			Compositor compositor = parentSchemaNode.getCompositor();
			Iterator leafMemberNames = compositor.getLeafMemberNames().iterator();
			while (leafMemberNames.hasNext()) {
				String leafName = (String) leafMemberNames.next();
				if (myLeafName.equals(leafName)) {
					continue;
				}
				String siblingPath = XPathUtils.getParentXPath(path) + "/" + leafName;
				if (doc.selectSingleNode(siblingPath) == null) {
					this.initializeNode(siblingPath);
				}
			}
		}
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 *@param  s  NOT YET DOCUMENTED
	 */
	private final void prtlnErr(String s) {
		System.err.println("cbInitializer: " + s);
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 *@param  s  NOT YET DOCUMENTED
	 */
	protected final void prtln(String s) {
		if (debug) {
			SchemEditUtils.prtln(s, "cbInitializer");
			// System.out.println(s);
		}
	}
}

