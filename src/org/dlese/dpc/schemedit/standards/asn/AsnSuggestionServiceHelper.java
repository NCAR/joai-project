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

import org.dlese.dpc.schemedit.standards.adn.AsnToAdnMapper;
import org.dlese.dpc.schemedit.standards.CATServiceHelper;
import org.dlese.dpc.schemedit.standards.CATHelperPlugin;
import org.dlese.dpc.schemedit.standards.StandardsRegistry;
import org.dlese.dpc.schemedit.standards.StandardsManager;
import org.dlese.dpc.standards.asn.AsnDocument;
import org.dlese.dpc.schemedit.standards.asn.AsnStandardsDocument;
import org.dlese.dpc.schemedit.MetaDataFramework;
import org.dlese.dpc.schemedit.action.form.SchemEditForm;
import org.dlese.dpc.xml.Dom4jUtils;
import org.dlese.dpc.xml.schema.*;
import org.dlese.dpc.schemedit.*;

import org.dlese.dpc.serviceclients.cat.CATStandard;

import org.dom4j.*;

import java.io.*;
import java.util.*;
import org.apache.struts.util.LabelValueBean;

import java.net.*;

/**
 *  SuggestionsServiceHelper for the CAT REST standards suggestion service,
 *  operating over ASN Standards.
 *
 * @author    ostwald
 */
public class AsnSuggestionServiceHelper extends CATServiceHelper {
	private static boolean debug = false;

	SelectedStandardsBean selectedStandardsBean = null;
	AsnStandardsDocument standardsDocument = null;


	/**
	 *  Constructor for the AsnSuggestionServiceHelper object
	 *
	 * @param  sef              NOT YET DOCUMENTED
	 * @param  frameworkPlugin  NOT YET DOCUMENTED
	 */
	public AsnSuggestionServiceHelper(SchemEditForm sef, CATHelperPlugin frameworkPlugin) {
		super(sef, frameworkPlugin);
		try {
			this.setStandardsDocument(this.getDefaultDoc());
		} catch (Throwable e) {
			prtlnErr("ERROR: could not set document: " + e.getMessage());
			e.printStackTrace();
		}
	}


	/**
	 *  Gets the current standardsDocument if one has been assigned, or fetches the
	 *  "defaultDoc" from the StandardsRegistry.
	 *
	 * @return    The standardsDocument value
	 */
	public AsnStandardsDocument getStandardsDocument() {
		if (this.standardsDocument == null) {
			prtln("getStandardsDocument: this.standardsDocument is null, obtaining from StandardsRegistry");
			try {
				this.standardsDocument =
					StandardsRegistry.getInstance().getStandardsDocument(this.getDefaultDoc());
			} catch (Throwable t) {
				prtlnErr("could not instantiate AsnStandardsDocument: " + t.getMessage());
			}
		}
		return this.standardsDocument;
	}


	/**
	 *  Gets the standardsFormat attribute of the AsnSuggestionServiceHelper object
	 *  (hardcoded to "asn").
	 *
	 * @return    The standardsFormat value
	 */
	public String getStandardsFormat() {
		return "asn";
	}


	/**
	 *  Gets the standardsManager attribute of the AsnSuggestionServiceHelper
	 *  object
	 *
	 * @return    The standardsManager value
	 */
	public AsnStandardsManager getStandardsManager() {
		StandardsManager mgr = super.getStandardsManager();
		if (mgr instanceof DynaStandardsManager) {
			return (DynaStandardsManager) mgr;
		}
		else {
			return (AsnStandardsManager) mgr;
		}
	}


	//----------------- from AsnStandardsManager --------------------

	/**
	 *  Gets the otherSelectedStandards attribute of the AsnSuggestionServiceHelper
	 *  object
	 *
	 * @return    The otherSelectedStandards value
	 */
	public Map getOtherSelectedStandards() {
		if (this.selectedStandardsBean != null) {
			return this.selectedStandardsBean.getOtherSelectedStandards();
		}
		return null;
	}


	/**
	 *  Write the list of selected standards, as well as the currentStandards Doc
	 *  to the SelectedStandardsBean
	 */
	public void updateSelectedStandardsBean() {
		if (this.selectedStandardsBean == null)
			this.selectedStandardsBean = 
				new SelectedStandardsBean(this.getSelectedStandards(), this.getCurrentDoc());
		else
			this.selectedStandardsBean.update(this.getSelectedStandards(), this.getCurrentDoc());
	}


	/**
	 *  Gets the selectedStandardsBean attribute of the AsnSuggestionServiceHelper
	 *  object
	 *
	 * @return    The selectedStandardsBean value
	 */
	public SelectedStandardsBean getSelectedStandardsBean() {
		return this.selectedStandardsBean;
	}


	/**
	 *  Gets the availableDocs attribute of the AsnSuggestionServiceHelper object
	 *
	 * @return    The availableDocs value
	 */
	public List getAvailableDocs() {
		return getStandardsManager().getAvailableDocs();
	}


	/**
	 *  Gets the currentDoc attribute of the AsnSuggestionServiceHelper object
	 *
	 * @return    The currentDoc value
	 */
	public String getCurrentDoc() {
		if (getStandardsDocument() == null) {
			prtln("getCurrentDoc() - standardsDocument is null, returning empty string!");
			return "";
		}
		return getStandardsDocument().getDocKey();
	}


	/**
	 *  Gets the defaultDoc attribute of the AsnSuggestionServiceHelper object
	 *
	 * @return    The defaultDoc value
	 */
	public String getDefaultDoc() {
		return getStandardsManager().getDefaultDocKey();
	}


	/**
	 *  Sets the asnDocument attribute of the AsnSuggestionServiceHelper object
	 *
	 * @param  key            The new asnDocument value
	 * @exception  Exception  Description of the Exception
	 */
	public void setStandardsDocument(String key) throws Exception {
		prtln("setAsnDocument()  = " + key);
		try {
			AsnStandardsDocument doc = StandardsRegistry.getInstance().getStandardsDocument(key);
			if (doc == null)
				throw new Exception("Standards document not found for " + key);
			this.standardsDocument = doc;
			this.setSuggestedStandards(null);
		} catch (Throwable t) {
			prtlnErr("WARNING: could not set asnDocument: " + t.getMessage());
		}

		/* 		prtln("\nAfter setting AsnDocument");
		prtln("\t helper author is " + this.getAuthor());
		prtln("\t mgr author is " + this.getAuthor()); */
	}


	/**
	 *  Gets the idFromCATStandard attribute of the AsnSuggestionServiceHelper
	 *  object
	 *
	 * @param  std  NOT YET DOCUMENTED
	 * @return      The idFromCATStandard value
	 */
	protected String getIdFromCATStandard(CATStandard std) {
		return std.getIdentifier();
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  s  NOT YET DOCUMENTED
	 */
	private static void prtln(String s) {
		if (debug) {
			SchemEditUtils.prtln(s, "AsnSuggestionServiceHelper");
		}
	}


	/**
	 *  Description of the Method
	 *
	 * @param  s  Description of the Parameter
	 */
	private static void prtlnErr(String s) {
		if (debug) {
			SchemEditUtils.prtln(s, "AsnSuggestionServiceHelper");
		}
	}


	/**
	 *  Debugging
	 *
	 * @param  standardsList  A list of StandardsWrapper instances to display.
	 */
	public void displaySuggestions(List standardsList) {
		prtln(standardsList.size() + " items returned by suggestion service ...");
		for (int i = 0; i < standardsList.size(); i++) {
			CATStandard std = (CATStandard) standardsList.get(i);
			String id = std.getIdentifier();
			String text = std.getText();
			prtln("\n" + i + "\n" + id + "\n" + text + "\n");
		}
	}

}

