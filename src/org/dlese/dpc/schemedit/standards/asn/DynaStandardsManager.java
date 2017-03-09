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

import org.dlese.dpc.schemedit.standards.StandardsRegistry;
import org.dlese.dpc.schemedit.standards.adn.util.MappingUtils;

import org.dlese.dpc.standards.asn.AsnDocument;

import org.dlese.dpc.xml.schema.*;
import org.dlese.dpc.schemedit.*;

import java.io.*;
import java.util.*;

import java.net.*;

/**
 *  Manages a set of Standards Documents (e.g. all standards files within a
 *  specified directory) as AsnStandardsDocuments instances and provides access
 *  to Documents and Standards Nodes within the current Standards document.
 *
 * @author    ostwald
 */

public class DynaStandardsManager extends AsnStandardsManager {
	private static boolean debug = false;
	private String standardsDirectory = null;


	/**
	 *  Constructor for the DynaStandardsManager object, which reads all files within
	 *  (recursively) the specified "standardsDirectory".
	 *
	 * @param  xpath               xpath of instanceDoc to which standards are
	 *      assigned
	 * @param  xmlFormat           format of framework for this helper
	 * @param  standardsDirectory  path to directory containing standardsDocs
	 * @param  defaultDocKey       as configured for this helper
	 * @exception  Exception       if AsnDocument files cannot be processed
	 */
	public DynaStandardsManager(String xmlFormat, String xpath, File standardsDirectory, String defaultDocKey)
		 throws Exception {
		super(xmlFormat, xpath, standardsDirectory);

		prtln("\nDynaStandardsManager() defaultDocKey: " + defaultDocKey);
		/* defaultDocKey is not necessarily a complete key (i.e., "author.topic." vs "author.topic.year")
			- try to match defaultDocKey and if there is no match simply pick the first available standards
			doc configured for this helper.
		*/
		try {
			String docKey = this.getStandardsRegistry().matchKey(defaultDocKey);
			prtln("matchKey retured: " + docKey);
			if (docKey == null) {
				// couldn't get default, so grab first available
				prtln("couldnt get default, grabbing first available");
				AsnDocInfo docInfo = (AsnDocInfo) this.availableDocs.get(0);
				docKey = docInfo.getKey();
			}
			this.setDefaultDocKey(docKey);
			prtln("  ... default set to: " + this.getDefaultDocKey());
		} catch (Throwable e) {
			throw new Exception("standards manager could not establish defaultDocKey for " + xmlFormat);
		}
	}


	/**
	 *  Initialize the standardsRegistry and availableDocs attributes for this
	 *  Manager.
	 *
	 * @param  standardsDirectory  directory holding standard docs
	 * @exception  Exception       if standards docs cannot be loaded into StandardsRegistry
	 */
	public void init(File standardsDirectory) throws Exception {
		StandardsRegistry registry = this.getStandardsRegistry();
		this.setAvailableDocs(registry.load(standardsDirectory.getAbsolutePath()));
		prtln(this.availableDocs.size() + " standards Documents loaded");
	}


	/**
	 *  Gets the standardsDirectory attribute of the DynaStandardsManager object
	 *
	 * @return    The standardsDirectory value
	 */
	public String getStandardsDirectory() {
		return this.standardsDirectory;
	}



	/**
	 *  The main program for the DynaStandardsManager class
	 *
	 * @param  args           The command line arguments
	 * @exception  Exception  
	 */
	public static void main(String[] args) throws Exception {

		String xpath = "/record/educational/standards/asnID";
		String xmlFormat = "ncs_item";

		// String dir = "L:/common/asn/v1.4.1/ASN-by-subject-2008/Science";
		String dir = "L:/ostwald/MAST/devel-standards";

		File source = new File(dir);
		String defaultDocKey = "..2005";

		DynaStandardsManager mgr = new DynaStandardsManager(xmlFormat, xpath, source, defaultDocKey);
		mgr.report();
	}


	/**  Debugging */
	public void report() {
		super.report();
	}


	/**
	 *  Description of the Method
	 *
	 * @param  s  Description of the Parameter
	 */
	private static void prtln(String s) {
		if (debug)
			SchemEditUtils.prtln(s, "DynaStandardsManager");
	}
}

