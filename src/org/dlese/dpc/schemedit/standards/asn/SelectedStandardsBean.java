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
package org.dlese.dpc.schemedit.standards.asn;

import org.dlese.dpc.schemedit.standards.StandardsManager;
import org.dlese.dpc.schemedit.standards.StandardsRegistry;

import org.dlese.dpc.schemedit.SchemEditUtils;

import java.io.*;
import java.util.*;

/**
 *  Helper class to provide information about the currently selected standards
 *  in the UI, which may span several standards documents. The "docKey"
 *  attribute is settable by the UI, which enables information about a specific
 *  document (and its selected standards) to be queried.
 *
 * @author    ostwald
 */

public class SelectedStandardsBean {
	private static boolean debug = false;

	private StandardsRegistry standardsRegistry = null;
	private Map docMap = null;
	private Map nodeMap = null;
	private String currentDocKey = null;
	private String docKey = null;
	private Map otherSelectedStandards = null;
	private List selectedStandards = null;


	/**
	 *  Constructor for the SelectedStandardsBean object
	 *
	 * @param  selectedStandards  standards currently selected in the UI
	 * @param  currentDocKey      key (e.g., "NSES.Science.1995.D10001D0") of standards document currently active in UI
	 */
	public SelectedStandardsBean(List selectedStandards, String currentDocKey) {

		this.standardsRegistry = StandardsRegistry.getInstance();
		this.selectedStandards = new ArrayList();
		update(selectedStandards, currentDocKey);
	}


	/**
	 *  Update SelectedStandardBean with information from UI
	 *
	 * @param  selectedStandards  list of stds selected in UI
	 * @param  currentDocKey      docKey (e.g., "NSES.Science.1995.D10001D0")
	 */
	public void update(List selectedStandards, String currentDocKey) {
		if (!currentDocKey.equals(this.currentDocKey))
			this.otherSelectedStandards = null;

		this.currentDocKey = currentDocKey;

		// we update nodeMap if nodeMap is currently null or if selectedStandards has changed

		if (this.nodeMap == null)
			this.nodeMap = new HashMap();

		Collections.sort(selectedStandards);

		// calls to standards registry can be expensive so we try to reuse what
		// is available
		if (!this.selectedStandards.equals(selectedStandards)) {

			this.selectedStandards = selectedStandards;
			Map oldNodeMap = this.nodeMap;
			this.nodeMap = new HashMap();
			for (Iterator i = selectedStandards.iterator(); i.hasNext(); ) {
				String id = (String) i.next();
				if (oldNodeMap.keySet().contains(id)) {
					this.nodeMap.put(id, oldNodeMap.get(id));
				}
				else {
					AsnStandardsNode node = standardsRegistry.getStandardsNode(id);
					if (node == null) {
						prtln("WARNING: standardsRegistry did not find node for " + id);
						continue;
					}
					this.nodeMap.put(id, node);
				}
			}
			this.otherSelectedStandards = null; // need this if standards can be selected from not-current doc
			this.docMap = null;
		}
	}


	/**
	 *  Creates a mapping from document keys to a list of AsnStandardsNodes
	 *  (representing the selectedStandards for that doc). The docMap supports
	 *  {@link #getOtherSelectedStandards()}.
	 *
	 * @return    mapping from doc key to list of selectedStandards for that doc
	 */
	private Map getDocMap() {
		if (this.docMap == null) {
			this.docMap = new HashMap();

			for (Iterator i = this.nodeMap.keySet().iterator(); i.hasNext(); ) {
				String id = (String) i.next();
				AsnStandardsNode node = (AsnStandardsNode) this.nodeMap.get(id);
				if (node == null) {
					prtln("THIS SHOULDNT HAPPEN: nodeMap did not contain node for " + id);
					continue;
				}
				String docId = node.getDocId();
				String key = standardsRegistry.getKey(docId);
				if (!this.docMap.containsKey(key)) {
					this.docMap.put(key, new ArrayList());
				}
				List nodeList = (List) this.docMap.get(key);
				nodeList.add(node);
				this.docMap.put(key, nodeList);
			}
		}
		return this.docMap;
	}


	/**
	 *  The standards document key set by the UI, enabling it to query for selected
	 *  standards from a specific document.
	 *
	 * @return    The docKey value (e.g., "NSES.Science.1995.D10001D0")
	 */
	public String getDocKey() {
		return this.docKey;
	}


	/**
	 *  Doc key settable by UI.
	 *
	 * @param  key  The new docKey value
	 */
	public void setDocKey(String key) {
		this.docKey = key;
	}


	/**
	 *  All the doc keys for which there are selected standards.
	 *
	 * @return    The docKeys value
	 */
	public Set getDocKeys() {
		return this.getDocMap().keySet();
	}



	/**
	 *  Gets the docInfo for specified standards document
	 *
	 * @param  docKey  document key
	 * @return         The docInfo value
	 */
	public AsnDocInfo getDocInfo(String docKey) {
		return (AsnDocInfo) standardsRegistry.getDocInfo(docKey);
	}


	/**
	 *  Gets the docInfo for the current document
	 *
	 * @return    The docInfo value
	 */
	public AsnDocInfo getDocInfo() {
		return (AsnDocInfo) standardsRegistry.getDocInfo(this.docKey);
	}


	/**
	 *  Gets the numSelected attribute of the SelectedStandardsBean object
	 *
	 * @return    The numSelected value
	 */
	public int getNumSelected() {
		return getNumSelected(this.docKey);
	}


	/**
	 *  Gets the numSelected attribute of the SelectedStandardsBean object
	 *
	 * @param  docKey  NOT YET DOCUMENTED
	 * @return         The numSelected value
	 */
	public int getNumSelected(String docKey) {
		List selected = (List) this.getDocMap().get(docKey);
		if (selected == null)
			return 0;
		return selected.size();
	}


	/**
	 *  Gets the number of selected standards that are NOT from the current
	 *  document
	 *
	 * @return    The numOtherSelected value
	 */
	public int getNumOtherSelected() {
		int numSelected = 0;
		if (this.selectedStandards != null)
			numSelected = this.selectedStandards.size();

		int ret = numSelected - this.getNumSelected(this.currentDocKey);
		prtln(" ... returning => " + ret);
		return ret;
	}


	/**
	 *  Gets the standardsTree for the standards doc specified by provided key.
	 *
	 * @param  key  Description of the Parameter
	 * @return      The standardsTree value
	 */
	public AsnStandardsDocument getStandardsDocument(String key) {
		return this.standardsRegistry.getStandardsDocument(key);
	}


	/**
	 *  Gets the selected standards that are not in the active standards document
	 *  (which is determined by currentDocKey.
	 *
	 * @return    The otherSelectedStandards value
	 */
	public Map getOtherSelectedStandards() {
		if (otherSelectedStandards == null) {
			// prtln("computing oherSelectedStandards() " + currentDocKey);

			Map docMap = getDocMap();
			otherSelectedStandards = new HashMap();
			for (Iterator i = docMap.keySet().iterator(); i.hasNext(); ) {
				String key = (String) i.next();

				if (!key.equals(currentDocKey)) {
					// prtln("\t key: " + key);
					List idList = (List) docMap.get(key);
					otherSelectedStandards.put(key, idList);
				}
			}
		}
		return otherSelectedStandards;
	}


	static String[] debugIds = {
		"http://purl.org/ASN/resources/S102DBAE",
		"http://purl.org/ASN/resources/S103E0D7",
		"http://purl.org/ASN/resources/S103EC5A",
		"http://purl.org/ASN/resources/S103EC5B",
		"http://purl.org/ASN/resources/S1027384",
		"http://purl.org/ASN/resources/S102747E"
		};

	static String[] sampleIds = {
		"http://purl.org/ASN/resources/S102DBAE",
		"http://purl.org/ASN/resources/S103E0D7",
		"http://purl.org/ASN/resources/S103EC5A",
		"http://purl.org/ASN/resources/S103EC5B",
		"http://purl.org/ASN/resources/S1027384",
		"http://purl.org/ASN/resources/S102747E",
		"http://purl.org/ASN/resources/S103E0D7",
		"http://purl.org/ASN/resources/S103EC5A",
		"http://purl.org/ASN/resources/S103EC5B",
		"http://purl.org/ASN/resources/S1027384",
		"http://purl.org/ASN/resources/S102747E"
		};


	/**
	 *  The main program for the SelectedStandardsBean class
	 *
	 * @param  args           The command line arguments
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public static void main(String[] args) throws Exception {

		String dir = "/Documents/Work/DLS/ASN/mast-docs/";
		StandardsRegistry reg = StandardsRegistry.getInstance();
		List docs = reg.load(dir);
		reg.report();

		String[] idSet = debugIds;
		List ids = new ArrayList();
		for (int i = 0; i < idSet.length; i++) {
			ids.add(idSet[i]);
		}

		String currentDocKey = "NSES.Science.1995";
		SelectedStandardsBean bean = new SelectedStandardsBean(ids, currentDocKey);

		bean.report();

		Map others = bean.getOtherSelectedStandards();
		prtln("\nOther selected standards");
		for (Iterator i = others.keySet().iterator(); i.hasNext(); ) {
			String key = (String) i.next();
			prtln(key);
			List items = (List) others.get(key);
			for (Iterator n = items.iterator(); n.hasNext(); ) {
				AsnStandardsNode node = (AsnStandardsNode) n.next();
				prtln("\t" + node.getId());
			}
		}
	}


	/**  Description of the Method */
	public void destroy() { }


	/**  Description of the Method */
	public void report() {
		prtln("\n----------------------");
		prtln("selected standards bean report");
		for (Iterator i = this.getDocMap().keySet().iterator(); i.hasNext(); ) {
			String key = (String) i.next();
			String docId = getStandardsDocument(key).getId();
			prtln("\n" + key + " (" + docId + ")");
			List nodeList = (List) this.getDocMap().get(key);
			for (Iterator n = nodeList.iterator(); n.hasNext(); ) {
				AsnStandardsNode node = (AsnStandardsNode) n.next();
				prtln("\t" + node.getId());
			}
		}
		prtln("----------------------\n");
	}


	/**
	 *  Description of the Method
	 *
	 * @param  others  Description of the Parameter
	 */
	public void showOthers(Map others) {
		for (Iterator i = others.keySet().iterator(); i.hasNext(); ) {
			String key = (String) i.next();
			prtln(key);
			List items = (List) others.get(key);
			for (Iterator n = items.iterator(); n.hasNext(); ) {
				AsnStandardsNode node = (AsnStandardsNode) n.next();
				prtln("\t" + node.getId());
			}
			prtln("---------");
		}
	}


	/**
	 *  Description of the Method
	 *
	 * @param  s  Description of the Parameter
	 */
	private static void prtln(String s) {
		if (debug)
			SchemEditUtils.prtln(s, "SelectedStandardsBean");
	}
}

