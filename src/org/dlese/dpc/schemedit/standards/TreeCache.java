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
package org.dlese.dpc.schemedit.standards;

import org.dlese.dpc.schemedit.standards.asn.AsnStandardsDocument;
import org.dlese.dpc.schemedit.standards.asn.AsnStandardsNode;

import org.dlese.dpc.schemedit.SchemEditUtils;
import org.dlese.dpc.standards.asn.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.*;
import java.io.File;

/**
*  Cashe of AsnStandardsDocuments. Works with a {@link StandardsRegistry} instance to support
 *  the "getStandardsDocument" call. Maintains a tree map of specified size.
 *  When a call to getStandardsDocument is called: - Most recently used list is
 *  updated to put that tree first. - if requested tree is not in the tree map,
 *  the least recently used tree is destroyed and the requested tree is read
 *  (using path obtained from the Registry).
 *
 * @author     Jonathan Ostwald
 * @created    December 31, 2008
 */
public class TreeCache {
	private static Log log = LogFactory.getLog(TreeCache.class);
	private static boolean debug = true;
	private int capacity = 20;
	// private List mru = null; // most Recently Used List
	// private static TreeCache instance = null;
	private Map treeMap = null;
	private NodeMap nodeMap = null;
	private UsageQueue usageQueue = null;
	private StandardsRegistry standardsRegistry = null;


	/**
	 *  Constructor for the TreeCache object
	 *
	 * @param  standardsRegistry  NOT YET DOCUMENTED
	 * @exception  Exception      NOT YET DOCUMENTED
	 */
	public TreeCache(StandardsRegistry standardsRegistry) throws Exception {
		this.standardsRegistry = standardsRegistry;
		this.treeMap = new HashMap();
		this.nodeMap = new NodeMap();
		this.usageQueue = new UsageQueue(this.capacity);
	}


	/**
	 *  Description of the Method
	 *
	 * @param  key   Document key of the form (<author>.<topic>.<year>.<uid>)
	 * @param  tree  Description of the Parameter
	 */
	public void addTree(String key, AsnStandardsDocument tree) {
		this.treeMap.put(key, tree);
		this.nodeMap.addNodes(key, tree);
		this.usageQueue.addItem(key);
		if (this.isFull())
			this.removeTree();
	}


	/**
	 *  Gets the full attribute of the TreeCache object
	 *
	 * @return    The full value
	 */
	public boolean isFull() {
		return this.treeMap.size() > this.capacity;
	}


	/**  Remove least recently used tree from the cache */
	public void removeTree() {
		String key = this.usageQueue.getLRU();
		this.removeTree(key);
	}


	/**
	 *  Remove specified tree from the cache
	 *
	 * @param  key  key of asnStandardsDocument
	 */
	public void removeTree(String key) {
		// prtln ("removeTree: " + key);
		AsnStandardsDocument tree = (AsnStandardsDocument) treeMap.get(key);
		tree.destroy();
		this.treeMap.remove(key);
		this.usageQueue.removeItem(key);
	}


	/**
	 *  Get the StandardsTree specified by provided key. <pre>
	 * if the treeMap.containsKey(key)
	 * - usageQueue.touch(key)
	 * otherwise
	 * - tree = new AsnStandardsDocument (key)
	 * - addTree (key, tree)
	 * return  treeMap.get(key)
	 *</pre>
	 *
	 * @param  key  document key (e.g., "AAAS.Science.1993.D1000152")
	 * @return      The the standards document for specified key.
	 */
	public AsnStandardsDocument getTree(String key) {
		AsnStandardsDocument tree = null;
		if (this.treeMap.containsKey(key)) {
			this.usageQueue.touch(key);
			tree = (AsnStandardsDocument) treeMap.get(key);
		}
		else {
			try {
				prtln("Loading Standards Tree for " + key);
				tree = standardsRegistry.instantiateStandardsDocument(key);
				this.addTree(key, tree);
			} catch (Exception e) {
				prtln("getTree error: " + e.getMessage());
			}
		}
		return (AsnStandardsDocument) this.treeMap.get(key);
	}


	/**
	 *  Gets the standardsNode attribute of the TreeCache object
	 *
	 * @param  asnId  Description of the Parameter
	 * @return        The standardsNode value
	 */
	public AsnStandardsNode getStandardsNodeOLD(String asnId) {
		AsnStandardsNode std = null;
		for (Iterator i = this.treeMap.values().iterator(); i.hasNext(); ) {
			AsnStandardsDocument tree = (AsnStandardsDocument) i.next();
			std = tree.getStandard(asnId);
			if (std != null) {
				return std;
			}
		}
		prtln("Standard not found: " + asnId);
		return null;
	}


	/**
	 *  Gets the standardsNode attribute of the TreeCache object
	 *
	 * @param  asnId          NOT YET DOCUMENTED
	 * @return                The standardsNode value
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public AsnStandardsNode getStandardsNode(String asnId) throws Exception {
		String docKey = this.nodeMap.getDocKey(asnId);
		if (docKey == null)
			throw new Exception("docKey not found for " + asnId);
		AsnStandardsDocument tree = this.getTree(docKey);
		if (tree == null)
			throw new Exception("tree not found for " + docKey);
		return tree.getStandard(asnId);
	}


	/**  NOT YET DOCUMENTED */
	public void report() {
		prtln("\nTreeCache Report");
		this.usageQueue.report();
		prtln("NodeMap has " + this.nodeMap.size() + " items");
	}


	/**
	 *  The main program for the TreeCache class
	 *
	 * @param  args           The command line arguments
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public static void main(String[] args) throws Exception {
		prtln("\n------------------------------------\n");
	}


	/**
	 *  Queue supporting LeastRecentlyUsed functions. Least recently used item is
	 *  first, Most recently used is last.
	 *
	 * @author     ostwald
	 * @created    December 31, 2008
	 */
	private class UsageQueue {
		List items = null;
		private int capacity = -1;


		/**
		 *  Constructor for the UsageQueue object
		 *
		 * @param  capacity  NOT YET DOCUMENTED
		 */
		UsageQueue(int capacity) {
			this.capacity = capacity;
			this.items = new ArrayList();
		}


		/**
		 *  Make the supplied key the most recently used, and return the least
		 *  recently used key if there are more than capacity items.
		 *
		 * @param  key  Description of the Parameter
		 */
		private synchronized void touch(String key) {
			if (items.contains(key)) {
				items.remove(key);
				items.add(key);
			}
		}


		/**
		 *  Gets the lRU attribute of the UsageQueue object
		 *
		 * @return    The lRU value
		 */
		public String getLRU() {
			return (String) items.get(0);
		}


		/**
		 *  NOT YET DOCUMENTED
		 *
		 * @param  key  NOT YET DOCUMENTED
		 */
		public void removeItem(String key) {
			items.remove(key);
		}


		/**
		 *  NOT YET DOCUMENTED
		 *
		 * @param  index  NOT YET DOCUMENTED
		 */
		public void removeItem(int index) {
			items.remove(index);
		}


		/**
		 *  Adds a feature to the Item attribute of the UsageQueue object
		 *
		 * @param  key  The feature to be added to the Item attribute
		 */
		public void addItem(String key) {
			items.add(key);
		}


		/**  NOT YET DOCUMENTED */
		public void report() {
			prtln("UsageQueue - Least Recently Used first");
			for (Iterator i = this.items.iterator(); i.hasNext(); )
				prtln("\t" + (String) i.next());
			prtln("--------------");
		}

	}


	/**
	 *  Map supporting quick look up of standards nodes by their asn id
	 *
	 * @author    Jonathan Ostwald
	 */
	private class NodeMap {
		Map nodeMap = null;
		List docKeys = null;


		/**  Constructor for the NodeMap object */
		NodeMap() {
			this.nodeMap = new HashMap();
			this.docKeys = new ArrayList();
		}


		/**
		 *  Adds all nodes of the provided StandardsDocument to the nodeMap for
		 *  convenient lookup.
		 *
		 * @param  key   key identifying this tree
		 * @param  tree  standards tree containing nodes to be added
		 */
		void addNodes(String key, AsnStandardsDocument tree) {
			if (!this.docKeys.contains(key)) {
				this.docKeys.add(key);
				for (Iterator i = tree.getNodeList().iterator(); i.hasNext(); ) {
					AsnStandardsNode node = (AsnStandardsNode) i.next();
					this.nodeMap.put(node.getId(), key);
				}
			}
		}


		/**
		 *  Gets the docKey attribute of the NodeMap object
		 *
		 * @param  asnId  NOT YET DOCUMENTED
		 * @return        The docKey value
		 */
		String getDocKey(String asnId) {
			return (String) this.nodeMap.get(asnId);
		}


		/**
		 *  NOT YET DOCUMENTED
		 *
		 * @return    NOT YET DOCUMENTED
		 */
		int size() {
			return this.nodeMap.size();
		}
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  s  NOT YET DOCUMENTED
	 */
	private static void prtln(String s) {
		if (debug) {
			SchemEditUtils.prtln(s, "TreeCache");
		}
	}
}

