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
package org.dlese.dpc.standards.asn;

import org.dlese.dpc.schemedit.SchemEditUtils;
import org.dlese.dpc.schemedit.standards.*;
import org.dom4j.*;
import org.dlese.dpc.xml.Dom4jUtils;
import org.dlese.dpc.xml.schema.SchemaHelper;
import java.util.*;
import java.io.File;

public class AsnTester {
	private static boolean debug = true;
	AsnDocument asnDoc;
	AsnStandard root;
	public AsnTester (String path) {
			
		try {
			asnDoc = new AsnDocument(new File (path));
		} catch (Exception e) {
			prtln ("Could not read from " + path + ": " + e.getMessage());
			return;
		}
		
		root = asnDoc.getRootStandard();
		// prtln ("root: " + root.toString());
		prtln ("root: " + root.getDescription() + "  (" + root.getLevel() + ")");
	}
	
	void showChildren (AsnStandard parent) {
		for (Iterator i=parent.getChildren().iterator();i.hasNext();) {
			AsnStandard std = (AsnStandard)i.next();
			prtln ("\t" + std.getDescription());
		}
	}
	
	void showStandardsAtLevel (int level) {
		boolean verbose = true;
		List stds = asnDoc.getStandardsAtLevel(level);
		prtln (stds.size() + " standards found at level " + level);
		for (Iterator i=stds.iterator();i.hasNext();) {
			AsnStandard std = (AsnStandard)i.next();
			if (verbose) {
				// prtln ("\t - " + std.getDescription().substring (0, 40));
				showStandard (std);
			}
		}
	}
	
	public static void main (String[] args) {
		// AAASTester();
		ColoTester();
	}

	static void ColoTester () {
		String path = "C:/tmp/ASN/Colo.xml";
		AsnTester tester = new AsnTester (path);

		String id = "http://purl.org/ASN/resources/S103ECDB";
	
		tester.showSingleStandard(id);
	}
	
	static void AAASTester () {
		String path = "C:/tmp/ASN/AAAS.xml";
		AsnTester tester = new AsnTester (path);
		// tester.showChildren (tester.root);
		// tester.showStandard("http://purl.org/ASN/resources/S103ECD6");
		prtln ("authorPurl: " + tester.asnDoc.getAuthorPurl());
		prtln ("version: " + tester.asnDoc.getVersion());

		
/* 		tester.showStandardsAtLevel(1);
		
		prtln ("");
		tester.showStandardsAtLevel(2); */

		String id = "http://purl.org/ASN/resources/S100D239";
	
		tester.showSingleStandard(id);
	}
	
	void showStandard (String purlId) {
		AsnStandard std = asnDoc.getStandard(purlId);
		if (std == null)
			prtln ("standard not found for " + purlId);
		else
			showStandard (std);
	}
	
	public void showSingleStandard (String purlId) {
		prtln (purlId);
		showStandard (purlId);
	}
	
	public void showStandard (AsnStandard std) {
/* 		prtln ("\t - " + truncate (std.getDescription(), 40) + " -- " +
				ColoradoStandardDisplay.getNumber (std)); */
		if (std instanceof AAASBenchmark)
			prtln (((AAASBenchmark)std).getDisplayText());
		else
			prtln (std.getDisplayText());
	}
		
	static String truncate (String s, int max) {
		if (s == null)
			return "";
		String in = s.trim();
		if (in.length() > max)
			return in.substring (0, max) + " ...";
		else
			return in;
	}
	
	private static void prtln(String s) {
		if (debug) {
			// System.out.println("AsnDocument: " + s);
			System.out.println(s);
		}
	}
}
