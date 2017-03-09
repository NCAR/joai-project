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

import org.dlese.dpc.schemedit.standards.CATServiceHelper;
import org.dlese.dpc.schemedit.standards.CATHelperPlugin;

import org.dlese.dpc.schemedit.standards.StandardsDocument;
import org.dlese.dpc.schemedit.standards.StandardsNode;
import org.dlese.dpc.schemedit.action.form.SchemEditForm;
import org.dlese.dpc.xml.Dom4jUtils;
import org.dlese.dpc.xml.XPathUtils;
import org.dlese.dpc.xml.schema.DocMap;
import org.dlese.dpc.schemedit.*;

import org.dlese.dpc.schemedit.display.CollapseBean;
import org.dlese.dpc.schemedit.display.CollapseUtils;

import javax.servlet.http.HttpServletRequest;

import org.dom4j.*;

import java.io.*;
import java.util.*;
import org.apache.struts.util.LabelValueBean;

import java.net.*;

/**
 *  SuggestionsServiceHelper for the CAT REST standards suggestion service,
 *  operating over res_qual framework, which presents special considerations ...
 *
 * @author    ostwald
 */
public class ResQualSuggestionServiceHelper extends AsnSuggestionServiceHelper {
	private static boolean debug = false;

	private String basePath = null;


	/**
	 *  Constructor for the ResQualSuggestionServiceHelper object
	 *
	 * @param  sef              the SchemEditForm instance
	 * @param  frameworkPlugin  the framework plugin for the res_qual framework
	 */
	public ResQualSuggestionServiceHelper(SchemEditForm sef, CATHelperPlugin frameworkPlugin) {
		super(sef, frameworkPlugin);
		// display selected standards by default
		this.setDisplayContent(this.SELECTED_CONTENT);
		this.setDisplayMode(this.LIST_MODE);
		this.initializeXpath();
		prtln("instantiated");
	}


	private void initializeXpath() {
		// we don't know which xpath is relevant until we query the instance Doc!
		// String basePath = null;
		SchemEditForm sef = getActionForm();
		if (sef.getDocMap().nodeExists("/record/contentAlignment/representation"))
			basePath = "/record/contentAlignment/representation";
		else if (sef.getDocMap().nodeExists("/record/contentAlignment/phenomenon"))
			basePath = "/record/contentAlignment/phenomenon";

		if (basePath != null)
			this.setXpath(basePath + "/learningGoal/content");
		else
			prtln("WARNING: basePath not determined for resqual helper");
	}


	/**
	 *  Gets the standardsManager attribute of the ResQualSuggestionServiceHelper
	 *  object
	 *
	 * @return    The standardsManager value
	 */
	public ResQualStandardsManager getStandardsManager() {
		AsnStandardsManager mgr = super.getStandardsManager();
		return (ResQualStandardsManager) mgr;
	}


	/**
	 *  Sets the xpath attribute of the ResQualSuggestionServiceHelper object
	 *
	 * @param  path  The new xpath value
	 */
	public void setXpath(String path) {
		this.getStandardsManager().setXpath(path);
		if (path.startsWith("/record/contentAlignment/representation"))
			basePath = "/record/contentAlignment/representation";
		if (path.startsWith("/record/contentAlignment/phenomenon"))
			basePath = "/record/contentAlignment/phenomenon";
	}


	/**
	 *  Gets the CAT standards that have been selected in the CAT UI, which are
	 *  different from the benchmarks that have been cataloged for the res_qual
	 *  instanceDoc
	 *
	 * @param  request  NOT YET DOCUMENTED
	 * @return          The selectedCATStandards value
	 */
	private List getSelectedCATStandards(HttpServletRequest request) {
		List selectedStds = new ArrayList();

		String catParamName = "enumerationValuesOf(" + this.getXpath() + ")";
		String[] selectedCATStds = request.getParameterValues(catParamName);
		if (selectedCATStds == null || selectedCATStds.length == 0) {
			prtln("\tno selected standards found in request");
			return selectedStds;
		}
		prtln("\tselected standards in CAT interface (" + selectedCATStds.length + ")");
		for (int i = 0; i < selectedCATStds.length; i++) {
			if (selectedCATStds[i].trim().length() > 0) {
				prtln("\t" + selectedCATStds[i]);
				selectedStds.add(selectedCATStds[i]);
			}
		}
		return selectedStds;
	}


	/**
	 *  Update instanceDocument to make the benchmark elements in the instance doc
	 *  correspond to the selected standards in the CAT UI.<p>
	 *
	 *  Called by SchemEditForm.validate() to pre-process the instanceDoc.
	 *
	 * @param  request        the Request
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public void rectifyInstanceDoc(HttpServletRequest request) throws Exception {
		prtln("\n------------------------\nrectifyInstanceDoc");

		SchemEditForm sef = this.getActionForm();
		List selectedStds = this.getSelectedCATStandards(request);

		if (selectedStds.isEmpty()) {
			prtln("no selectedStds found - bailing");
			return;
		}

		String benchmarksXpath = XPathUtils.getParentXPath(this.getXpath());

		DocMap docMap = sef.getDocMap();

		// benchMarkContainerXPath will be /record/contentAlignment/{phenomenon | representation}
		String benchMarkContainerXPath = XPathUtils.getParentXPath(benchmarksXpath);

		Element benchMarkContainer = (Element) docMap.selectSingleNode(benchMarkContainerXPath);
		if (benchMarkContainer == null)
			throw new Exception("benchMarkContainer element not found");

		Map assignedBenchmarks = new HashMap(); // mapping of asnID to benchmark element

		List benchmarkElements = benchMarkContainer.elements(); // benchmark elements existing in instanceDoc
		if (benchmarkElements.isEmpty()) {
			prtln("no assigned benchmarks found");
		}
		else {
			// prtln(benchmarkElements.size() + " benchmarkElements found");

			for (Iterator i = benchmarkElements.iterator(); i.hasNext(); ) {
				Element benchmarkEl = (Element) i.next();
				if (benchmarkEl == null)
					throw new Exception("benchmarkEl not found");
				String benchmark = benchmarkEl.element("content").getTextTrim();
				if (benchmark.length() > 0 && selectedStds.contains(benchmark)) {
					assignedBenchmarks.put(benchmark, benchmarkEl.createCopy());
				}
			}
		}

		/* add a new benchMark element for each newly selected standard */
		for (Iterator i = selectedStds.iterator(); i.hasNext(); ) {
			String selectedStd = (String) i.next();
			if (!assignedBenchmarks.containsKey(selectedStd)) {

				int index = docMap.selectNodes(benchmarksXpath).size();
				String bmPath = benchmarksXpath;
				if (index > 0)
					bmPath += "[" + (index + 1) + "]";

				Element newBenchmark = (Element) docMap.createNewNode(bmPath);
				newBenchmark.element("content").setText(selectedStd);
				assignedBenchmarks.put(selectedStd, newBenchmark.createCopy());
			}
		}

		/* rebuild benchmarks from assignedBenchmarks map */
		docMap.removeSiblings(benchmarksXpath);

		// order in standards Doc order using node list
		for (Iterator i = this.getStandardsNodes().iterator(); i.hasNext(); ) {
			String id = ((StandardsNode) i.next()).getId();
			if (selectedStds.contains(id))
				benchMarkContainer.add((Element) assignedBenchmarks.get(id));
		}

	}


	/**
	 *  Initialize the collapse bean to show selected and suggested standards nodes
	 *  in the display specified by "displayContent".
	 *
	 * @param  displayContent  specifies what type of standards display is to be
	 *      updated.
	 * @exception  Exception   NOT YET DOCUMENTED
	 */
	public void updateStandardsDisplay(String displayContent) throws Exception {
		prtln("\n +++++++++++++++++++++");
		prtln("updateStandardsDisplay()");
		prtln("  displayContent: " + displayContent);
		SchemEditForm sef = this.getActionForm();

		CollapseBean cb = sef.getCollapseBean();
		StandardsDocument standardsTree = this.getStandardsDocument();

		if (standardsTree == null)
			throw new Exception("standardsTree not found");

		// close all before opening desired (this may not be what we want to do ...)
		for (Iterator i = standardsTree.getNodeList().iterator(); i.hasNext(); ) {
			StandardsNode node = (StandardsNode) i.next();
			if (node.getHasSubList()) {
				String key = CollapseUtils.pairToId(this.getXpath(), node.getId());
				cb.closeElement(key);
			}
		}

		// expose selected nodes
		if (displayContent.equalsIgnoreCase("selected") || displayContent.equalsIgnoreCase("both")) {
			// prtln ("\nexposing selected nodes");
			String[] selected = sef.getEnumerationValuesOf(this.getXpath());
			for (int i = 0; i < selected.length; i++) {
				String id = selected[i];
				StandardsNode node = standardsTree.getStandard(id);
				// prtln ("id: " + id);
				if (node == null) {
					prtln("WARNING: selected node not found for \"" + id +
						"\" in standardsTree for " + this.getCurrentDoc());
					continue;
				}
				List ancestors = node.getAncestors();
				for (Iterator a = ancestors.iterator(); a.hasNext(); ) {
					StandardsNode ancestor = (StandardsNode) a.next();
					String key = CollapseUtils.pairToId(this.getXpath(), ancestor.getId());
					cb.openElement(key);
				}
			}
			if (sef.getDocMap().nodeExists(XPathUtils.decodeXPath(this.getXpath())))
				sef.exposeNode(XPathUtils.decodeXPath(this.getXpath()));
		}

		// expose suggested nodes
		if (displayContent.equalsIgnoreCase("suggested") || displayContent.equalsIgnoreCase("both")) {
			// prtln ("\nexposing suggested nodes");
			List suggested = getSuggestedStandards();
			for (Iterator s = suggested.iterator(); s.hasNext(); ) {
				String id = (String) s.next();
				// prtln ("id: " + id);
				StandardsNode node = standardsTree.getStandard(id);
				if (node == null) {
					// prtln("WARNING: node not found for \"" + id + "\"");
					continue;
				}
				List ancestors = node.getAncestors();
				for (Iterator a = ancestors.iterator(); a.hasNext(); ) {
					StandardsNode ancestor = (StandardsNode) a.next();
					String key = CollapseUtils.pairToId(this.getXpath(), ancestor.getId());
					cb.openElement(key);
				}
			}
			// sef.exposeNode(XPathUtils.decodeXPath(this.getXpath()));
			if (sef.getDocMap().nodeExists(XPathUtils.decodeXPath(this.getXpath())))
				sef.exposeNode(XPathUtils.decodeXPath(this.getXpath()));
		}

		prtln("\nbasePath: " + basePath + " - exists: " + sef.getDocMap().nodeExists(basePath));

		if (this.basePath != null && sef.getDocMap().nodeExists(basePath)) {
			prtln("opening basePath element (" + basePath + ")");
			cb.openElement(CollapseUtils.pathToId(basePath));
		}

		prtln("   ... done with updateStandardsDisplay()");
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  s  NOT YET DOCUMENTED
	 */
	private static void prtln(String s) {
		if (debug) {
			SchemEditUtils.prtln(s, "ResQualHelper");
		}
	}


	/**
	 *  Description of the Method
	 *
	 * @param  s  Description of the Parameter
	 */
	private static void prtlnErr(String s) {
		if (debug) {
			SchemEditUtils.prtln(s, "ResQualSuggestionServiceHelper");
		}
	}

}

