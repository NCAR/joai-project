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
package org.dlese.dpc.schemedit.ndr.util.integration;

import org.dlese.dpc.ndr.apiproxy.*;
import org.dlese.dpc.ndr.NdrUtils;
import org.dlese.dpc.ndr.reader.*;
import org.dlese.dpc.ndr.request.*;
import org.dlese.dpc.xml.Dom4jUtils;
import org.dlese.dpc.util.Files;
import org.dlese.dpc.util.strings.FindAndReplace;
import org.dlese.dpc.index.SimpleLuceneIndex;
import org.dom4j.*;
import java.util.*;
import java.io.File;
import java.net.*;

/**
 *  Reads spreadsheet data (xml file created from spreadsheet) with data
 *  supplied by NSDL but augmented from NCS Collect records, with the purpose of
 *  determining overlaps and gaps between the collection management info in both
 *  models.
 *
 * @author    Jonathan Ostwald
 */
public class MisMatches {
	private static boolean debug = true;
	public static String dataFile = null;

	public List mismatches = null;

	/**
	 *  Constructor for the MisMatches object
	 *
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public MisMatches() throws Exception {
		Document doc = Dom4jUtils.getXmlDocument(new File (dataFile));
		mismatches = doc.selectNodes ("/mismatches/mismatch");
	}
	
	List getMisMatchingUrls () {
		List list = new ArrayList();
		prtln ("Mismatching URLS");
		for (Iterator i=mismatches.iterator();i.hasNext();) {
			MisMatch mm = new MisMatch ((Element)i.next());
			String ncsUrl = mm.ncsResult.resourceUrl;
			String nsdlUrl = mm.nsdlResult.resourceUrl;
			if (!ncsUrl.equals(nsdlUrl)) {
				list.add (mm);
				prtln ("\n" + mm.id);
				prtln ("\t ncs: " + ncsUrl);
				prtln ("\t nsdl: " + nsdlUrl);
			}
		}
		return null;
	}
	
	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @return    NOT YET DOCUMENTED
	 */
	public String toString() {
		String s = ("MisMatches values:\n");

		return s;
	}

	public static void setup () {
		CollectionIntegrator.setup();
		// dataFile = "/Users/ostwald/Desktop/Working/CollectionsData-02282008.xml";
		dataFile = "H:/Documents/NDR/NSDLCollections/MisMatches-02282008-2.xml";
	}
	
	public static void taosSetup () {
		String ndrApiBaseUrl = "http://ndr.nsdl.org/api";
		String ncsAgent = null;
		String keyFile = null;
		NdrUtils.setup(ndrApiBaseUrl, ncsAgent, keyFile);
		NdrRequest.setVerbose(false);
		NdrRequest.setDebug(false);
	}

	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  args           NOT YET DOCUMENTED
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public static void main(String[] args) throws Exception {
		
		setup();
		prtln ("\nMisMatches ...\n");

		MisMatches mm = new MisMatches();
		prtln (mm.mismatches.size() + " mismatches found");
		mm.getMisMatchingUrls ();
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  node  NOT YET DOCUMENTED
	 */
	private static void pp(Node node) {
		prtln(Dom4jUtils.prettyPrint(node));
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  s  NOT YET DOCUMENTED
	 */
	private static void prtln(String s) {
		String prefix = null;
		if (debug) {
			NdrUtils.prtln(s, prefix);
		}
	}

}

