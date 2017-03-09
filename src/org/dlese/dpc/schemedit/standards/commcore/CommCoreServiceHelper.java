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
package org.dlese.dpc.schemedit.standards.commcore;

import org.dlese.dpc.schemedit.MetaDataFramework;
import org.dlese.dpc.schemedit.action.form.SchemEditForm;
import org.dlese.dpc.xml.schema.*;
import org.dlese.dpc.xml.XPathUtils;
import org.dlese.dpc.schemedit.*;
import org.dlese.dpc.schemedit.display.CollapseBean;
import org.dlese.dpc.schemedit.display.CollapseUtils;

import org.dlese.dpc.schemedit.standards.CATServiceHelper;
import org.dlese.dpc.schemedit.standards.CATHelperPlugin;
import org.dlese.dpc.schemedit.standards.StandardsDocument;
import org.dlese.dpc.schemedit.standards.StandardsNode;
import org.dlese.dpc.schemedit.standards.config.SuggestionServiceConfig;
import org.dlese.dpc.schemedit.standards.config.SuggestionServiceManager;
import org.dlese.dpc.schemedit.standards.adn.DleseSuggestionServiceHelper;
import org.dlese.dpc.schemedit.standards.asn.AsnSuggestionServiceHelper;
import org.dlese.dpc.schemedit.standards.asn.ResQualSuggestionServiceHelper;

import org.dlese.dpc.serviceclients.cat.CATStandard;

import javax.servlet.http.HttpServletRequest;

import org.dom4j.*;

import java.io.*;
import java.util.*;
import org.apache.struts.util.LabelValueBean;

import java.net.*;

/**
 *  Run-time support for CAT suggestion service, which acts as intermediary
 *  between CAT Service client and Form bean/JSP pages.<p>
 *
 *  The CAT service UI involves extraction of several values from the item
 *  record being edited for each framework, such as selected keywords, selected
 *  graderanges, etc. The functionality to extract these values is delegated to
 *  the framework-specific plug-in, which implments {@link CATHelperPlugin}.
 *
 * @author    ostwald
 */
public class CommCoreServiceHelper extends CATServiceHelper {
	private static boolean debug = true;


	/**
	 *  Constructor for the CommCoreServiceHelper object
	 *
	 * @param  sef              Description of the Parameter
	 * @param  frameworkPlugin  NOT YET DOCUMENTED
	 */
	// public CommCoreServiceHelper(SchemEditForm sef, CATHelperPlugin frameworkPlugin) {
	public CommCoreServiceHelper(SchemEditForm sef, CATHelperPlugin frameworkPlugin) {
		super (sef, frameworkPlugin);
		this.setServiceIsActive(false);
	}


	/**
	 *  Gets the standardsDocument attribute of the CommCoreServiceHelper object
	 *
	 * @return    The standardsDocument value
	 */
	 public CommCoreStandardsDocument getStandardsDocument() {
		 return this.getStandardsManager().getStandardsDocument();
	 }


	/**
	 *  Gets the standardsFormat attribute of the CommCoreServiceHelper object
	 *
	 * @return    The standardsFormat value
	 */
	 public String getStandardsFormat() {
		 return "comm_core";
	 }


	/**
	 *  Gets the standardsManager attribute of the CommCoreServiceHelper object
	 *
	 * @return    The standardsManager value
	 */
	public CommCoreStandardsManager getStandardsManager() {
		if (this.getActionForm() == null)
			prtlnErr("getStandardsManager: actionForm is unavailable");
		else if (this.getActionForm().getFramework() == null)
			prtlnErr("getStandardsManager: framework is unavailable");
		return (CommCoreStandardsManager) this.getActionForm().getFramework().getStandardsManager();
	}


	/**
	 *  Gets the rootStandardNode attribute of the CommCoreServiceHelper object
	 *
	 * @return    The rootStandardNode value
	 */
	public StandardsNode getRootStandardNode() {
		return this.getStandardsDocument().getRootNode();
	}

	/**
	 *  Resolves author from the asnDocument (which it gets from the
	 *  StandardsDocument)
	 *
	 * @return    The author value
	 */
	public String getAuthor() {
		return this.getStandardsDocument().getAuthor();
	}

	protected String getIdFromCATStandard(CATStandard std) {
		return std.getIdentifier();
	}

	public List getAvailableDocs() {
		List docs = new ArrayList();
		docs.add (this.getStandardsDocument());
		return docs;
	}

	/**
	 *  Print a line to standard out.
	 *
	 * @param  s  The String to print.
	 */
	private static void prtln(String s) {
		if (debug) {
			SchemEditUtils.prtln(s, "CommCoreServiceHelper");
		}
	}


	private static void prtlnErr(String s) {
		SchemEditUtils.prtln(s, "CommCoreServiceHelper");
	}

}

