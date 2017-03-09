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
package org.dlese.dpc.schemedit.standards.adn.util;

import org.dom4j.*;

import org.dlese.dpc.schemedit.standards.adn.DleseStandardsDocument;
import org.dlese.dpc.schemedit.standards.adn.DleseStandardsNode;
import org.dlese.dpc.schemedit.standards.adn.AdnStandard;
import org.dlese.dpc.xml.Dom4jUtils;
import org.dlese.dpc.util.strings.FindAndReplace;

import java.util.*;
import java.io.File;

/**
 *  verifies that the asn and adn standards represented in the mappings file correspond to the asn and adn
 *  standards.<p>
 *
 *  adnManager: adn standards read from framework schema asnManager: asn standards read from localized
 *  standardsDocument asn2adnMap: read from mappingsFile
 *
 * @author     Jonathan Ostwald
 * @version    $Id: AsnToAdnVerifier.java,v 1.4 2009/09/30 18:31:41 ostwald Exp $
 */
public class AsnToAdnVerifier {

	private static boolean debug = true;
	private Map asn2adnMap;
	private Map adn2asnMap;
	StandardsMappingInfoMaker smim;
	AsnMappingDocument asnDoc;
	DleseStandardsDocument adnMgr;


	/**  Constructor for the AsnToAdnVerifier object */
	public AsnToAdnVerifier() {
		smim = new StandardsMappingInfoMaker();
		asnDoc = smim.asnDoc;
		adnMgr = smim.adnMgr;
		asn2adnMap = smim.asn2adnMap;
		adn2asnMap = smim.adn2asnMap;
	}


	/**
	 *  The main program for the AsnToAdnVerifier class
	 *
	 * @param  args  The command line arguments
	 */
	public static void main(String[] args) {
		AsnToAdnVerifier t = new AsnToAdnVerifier();
		t.test();
		t.adnMgrTest();
		t.asnDocTest();
	}

	// for every mapping
	/**  A unit test for JUnit */
	void test() {
		prtln("checking the mappings file at " + smim.mappingsFile + " ...");
		for (Iterator i = asn2adnMap.keySet().iterator(); i.hasNext(); ) {
			String asnId = (String) i.next();
			AsnMappingStandard asnStd = (AsnMappingStandard)asnDoc.getStandard(asnId);
			if (asnStd == null)
				prtln("\nasnStd not found for " + asnId);

			String adnId = (String) asn2adnMap.get(asnId);
			DleseStandardsNode adnStd = adnMgr.getStandard(adnId);
			if (adnStd == null)
				prtln("\nadnStd not found for " + adnId);
		}
	}


	/**
	 *  Test whether there is a mapping for each NSES adn standard of level 3 and 4.<p>
	 *
	 *  (NOTE: there won't be mappings for the NCGE standards) .
	 */
	void adnMgrTest() {
		prtln("\n ADN Mgr test");
		Map levelMap = adnMgr.getLevelMap();
		List adnStandards = new ArrayList();
		adnStandards.addAll((List) levelMap.get(new Integer(3)));
		adnStandards.addAll((List) levelMap.get(new Integer(4)));
		for (Iterator i = adnStandards.iterator(); i.hasNext(); ) {
			AdnStandard std = (AdnStandard) i.next();
			if (!adn2asnMap.containsKey(std.getText()))
				prtln("mapping not found for " + std.getText());
		}
	}


	/**  Test whether there is a mapping for each asn standard (of level 3 and 4) */
	void asnDocTest() {
		prtln("\n ASN Mgr test");
		for (Iterator i = asnDoc.getIdentifiers().iterator(); i.hasNext(); ) {
			String id = (String) i.next();
			if (!asn2adnMap.containsKey(id)) {
				AsnMappingStandard std = (AsnMappingStandard) asnDoc.getStandard(id);
				int level = std.getLevel();
				if (level == 3 || level == 4) {
					prtln("\nmapping not found for " + id + " (" + level + ")");
					prtln(std.getMatchKey());
				}
			}
		}
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  s  NOT YET DOCUMENTED
	 */
	private static void prtln(String s) {
		if (debug) {
			while (s.length() > 0 && s.charAt(0) == '\n') {
				System.out.println("");
				s = s.substring(1);
			}

			// System.out.println("AsnToAdnVerifier: " + s);
			System.out.println(s);
		}
	}
}

