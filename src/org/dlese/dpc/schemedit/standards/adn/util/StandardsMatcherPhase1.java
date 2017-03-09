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

import org.dlese.dpc.schemedit.SchemEditUtils;
import org.dlese.dpc.schemedit.standards.adn.*;
import org.dom4j.*;
import org.dlese.dpc.xml.Dom4jUtils;
import org.dlese.dpc.xml.schema.SchemaHelper;
import java.util.*;
import java.io.File;

/**
 *  Tool to help match asf and adn standards, and to identify standards that cannot be matched. The output of
 *  reports generated here can be manipulated into tab-delimited form and then imported into spreadsheets with
 *  the goal of creating a table in which the mappings between asfId and adn records is made.
 *
 * @author     Jonathan Ostwald
 */
public class StandardsMatcherPhase1 {
	private static boolean debug = true;
	private DleseStandardsDocument adnStandards;
	private AsnMappingDocument asnDoc;
	List adnNthLevelStandards;
	int level;
	Map idMap;
	Map matchKeyIdMap;

	List unmatchedASN, matched;
	Map unmatchedADN;


	/**  Constructor for the StandardsMatcherPhase1 object - builds two data structures that are then used to
	generate reports:
	1 - "idMap" -- which is initialized to hold an empty entry for each ASN id (the entry will later be filled)
	2 - "matchKeyIdMap" -- which holds an entry (an ASN ID) for each match key (when ASN and ADN stds have same match
	keys).	
	*/
	public StandardsMatcherPhase1(String standardsDocPath, int level) {
		this.level = level;
		
		String schemaURLStr;
		try {	
			
			if (level == 3) {
				schemaURLStr = "http://www.dlese.org/Metadata/adn-item/0.6.50/record.xsd";
				SchemaHelper sh = MappingUtils.getSchemaHelper(schemaURLStr, "itemRecord");
				String xpath = "/itemRecord/educational/contentStandards/contentStandard";
				adnStandards = new DleseStandardsDocument (sh, "NSESscienceContentStandardsType");
			}
			else if (level == 4) {
				schemaURLStr = "http://www.dlese.org/Metadata/adn-item/0.7.00/record.xsd";
				SchemaHelper sh = MappingUtils.getSchemaHelper(schemaURLStr, "itemRecord");
				adnStandards = MappingUtils.getDleseStandardsDocument(sh);
			}

			if (adnStandards == null)
				throw new Exception("unable to initialize ADN standards manager");

			adnNthLevelStandards = (List) adnStandards.getLevelMap().get(new Integer(level+1));
			
			if (adnNthLevelStandards == null)
				throw new Exception ("no adn standards found for level: " + level);
			
			prtln(adnNthLevelStandards.size() + " " + level + "th level ADN standards to match");

			// initialize ASN standards manager
			asnDoc = new AsnMappingDocument(standardsDocPath);

			// idMap is keyed by asf identifier, contains entries for standards at specified level
			idMap = new HashMap();
			matchKeyIdMap = new HashMap();
			for (Iterator i = asnDoc.getStandards().iterator(); i.hasNext(); ) {
				AsnMappingStandard std = (AsnMappingStandard) i.next();
				// if (std.isLeaf()) {
				if (std.getLevel() == this.level) {
					idMap.put(std.getId(), "");

					String matchKey = std.getMatchKey();
					if (matchKeyIdMap.containsKey(matchKey))
						prtln("... matchKey collision with \"" + matchKey + "\"");
					matchKeyIdMap.put(matchKey, std.getId());
				}
			}
			prtln("idMap initialized to hold " + idMap.size() + " items");
			prtln("matchKeyIdMap holds " + matchKeyIdMap.size() + " items");
		} catch (Throwable e) {
			prtln("StandardsMatcherPhase1 error: " + e.getMessage());
			e.printStackTrace();
		}
	}

	void showASNMatchKeys () {
/* 		prtln ("ASN Match Keys");
		for (Iterator i=matchKeyIdMap.keySet().iterator();i.hasNext();) {
			String matchKey = (String)i.next();
			String id = (String) matchKeyIdMap.get(matchKey);
			AsnStandard std = asnDoc.getStandard(id);
			// prtln ("\n" + std.toString());
			prtln (std.getDisplayText() + "@@@" + matchKey);
		} */
		
		prtln ("ASN third level standards");
		for (Iterator i=asnDoc.getStandardsAtLevel(3).iterator();i.hasNext();) {
			AsnMappingStandard std = (AsnMappingStandard)i.next();
			String matchKey = std.getMatchKey();
			// prtln ("\n" + std.toString());
			// prtln (std.getDisplayText() + "@@@" + matchKey);
			prtln (std.getDescription() + "@@@" + matchKey);
		}
		
	}
	
	/**
	 *  Try to find an ASN standard for each ADN 4th level standard. Populates following data structures:
	 *  <li> unmatchedADN - the adn standards for which no asf standard could be found. a mapping from
	 *  adnMatchKey to adnText
	 *  <li> idMap - (previously populated by creating an entry for each leaf ASN node) - mapping from asfId.
	 *  When a match is made, the idMap entry is set to the adn text.
	 *  <li> matched - list of matched keys (the values can be found in idMap)
	 *  <li> unmatchedASN - list of asfKeys that were not matched. These structures are printed out in "report"
	 */
	public void matchStandards() {

		unmatchedADN = new HashMap();
		unmatchedASN = new ArrayList();
		matched = new ArrayList();

		prtln("\nMatching standards ...");

		for (int i = 0; i < adnNthLevelStandards.size(); i++) {
			AdnStandard adnStandard = (AdnStandard) adnNthLevelStandards.get(i);
			String adnMatchKey = adnStandard.getMatchKey();

			String asnIdentifier = (String) matchKeyIdMap.get(adnMatchKey);

			// did the adnMatchKey equal an asnIdentifier?
			if (asnIdentifier == null) {
				unmatchedADN.put(adnMatchKey, adnStandard.getText());
			}
			else {
				idMap.put(asnIdentifier, adnStandard.getText());
			}
		}

		for (Iterator i = idMap.keySet().iterator(); i.hasNext(); ) {
			String key = (String) i.next();
			String matchedStd = (String) idMap.get(key);
			// if a standard was matched then it has content ...
			if (matchedStd == null || matchedStd.trim().length() == 0) {
				unmatchedASN.add(key);
			}
			else {
				matched.add(key);
			}
		}
	}


	/**  NOT YET DOCUMENTED */
	void report() {
		reportMatches();
		reportUnmatchedADN();
		reportUnmatchedASN();

		prtln("\n----------------------------------------------------------------------");
		prtln(adnNthLevelStandards.size() + " standards tried -- " + matched.size() + " found, " + unmatchedADN.size() + " not found");

	}


	/**  report the matches - we've stashed the adn standard text in the idMap */
	void reportMatches() {
		prtln("\n----------------------------------------------------------------------");
		prtln(matched.size() + " Matches");
		int count = 0;
		for (Iterator i = matched.iterator(); i.hasNext(); ) {
			count++;
			String key = (String) i.next();
			// AsnStandard std = asnDoc.getStandard(key);
			prtln("\n" + count + " -- " + key + "\n" + (String) idMap.get(key) + "\n");
		}
	}


	/**  NOT YET DOCUMENTED */
	void reportUnmatchedADN() {
		prtln("\n----------------------------------------------------------------------");
		prtln(unmatchedADN.size() + " unmatched ADN standards");
		int count = 0;
		for (Iterator i = unmatchedADN.keySet().iterator(); i.hasNext(); ) {
			String key = (String) i.next();
			String adnText = (String) unmatchedADN.get(key);
			count++;
			prtln("\n" + count + " -- " + key + "\n" + adnText + "\n");
		}
	}


	/**  NOT YET DOCUMENTED */
	void reportUnmatchedASN() {
		prtln("\n----------------------------------------------------------------------");
		prtln(unmatchedASN.size() + " unmatched ASN standards");
		int count = 0;
		for (Iterator i = unmatchedASN.iterator(); i.hasNext(); ) {
			count++;
			String key = (String) i.next();
			AsnMappingStandard std = (AsnMappingStandard) asnDoc.getStandard(key);
			if (std != null)
				prtln("\n" + count + " -- " + std.getId() + "\n" + std.getMatchKey() + "\n");
			else
				prtln("\n" + count + " -- ASN standard NOT found for " + std.getId() + "\n");
		}
	}


	/**
	 *  The main program for the StandardsMatcherPhase1 class
	 *
	 * @param  args  The command line arguments
	 */
	public static void main(String[] args) {
		org.dlese.dpc.xml.schema.SchemaReader.setDebug(false);
		// System.setProperty( "javax.xml.transform.TransformerFactory", 
			// "com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl" );
		boolean debugging = false;

		
		String host = "pc";
		String filename = "1995-NSES-v1.2.5-012007.xml";  // June 2007 version
		// String filename = "1995-NSES-v1.2.5-06012007.xml";  // June 2007 version
		
		String dir;
		if (host.equals("pc"))
			dir = "H:/python-lib/asn/standards-documents/localized";
		else
			dir = "/home/ostwald/python-lib/asn/standards-documents/localized";

		String standardsDocPath = dir + "/" + filename;
		
		
		int level = 4;
		StandardsMatcherPhase1 matcher = new StandardsMatcherPhase1(standardsDocPath, level);

		if (debugging) {
			matcher.showASNMatchKeys();
		}
		else {		
			matcher.matchStandards();
			matcher.report();
		}
		// asfMap.compareOne ();
	}



	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  s  NOT YET DOCUMENTED
	 */
	private static void prtln(String s) {
		if (debug) {
			// System.out.println("StandardsMatcherPhase1: " + s);
			System.out.println(s);
		}
	}
}
