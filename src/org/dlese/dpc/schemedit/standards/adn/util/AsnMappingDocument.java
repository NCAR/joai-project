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

import org.dlese.dpc.standards.asn.AsnDocument;
import org.dlese.dpc.schemedit.SchemEditUtils;
import org.dlese.dpc.schemedit.standards.adn.*;
import org.dom4j.*;
import org.dlese.dpc.xml.Dom4jUtils;
import org.dlese.dpc.xml.schema.SchemaHelper;
import java.util.*;
import java.io.File;

public class AsnMappingDocument extends AsnDocument {
	private static boolean debug = true;
	
	public AsnMappingDocument (String path) throws Exception {
		super (new File(path));
	}

	public static void main (String[] args) throws Exception {
		// String path = "/home/ostwald/python-lib/asn/standards-documents/1995-NSES-v1.2.5-localized.xml";
		String dir = "H:/Documents//ASN/standards-documents/source/";
		String filename = "2007-Colorado-Science-Model Content Standards Science.xml";
		AsnMappingDocument asfMgr = new AsnMappingDocument(dir + filename);
		
		// asfMgr.compareOne ();
	}
	
	public void compareOne () {
		DleseStandardsDocument sm = getDleseStandardsDocument();
		String adnKey = "NSES:5-8:Content Standard D Earth and Space Science Standards:Structure of the earth system:Land forms are the result of a combination of constructive and destructive forces. Constructive forces include crustal deformation, volcanic eruption, and deposition of sediment, while destructive forces include weathering and erosion.";
		DleseStandardsNode adnStd = sm.getStandard(adnKey);
		// DleseStandardsNode adnStd = sm.getStandard("NSES:K-4:Content Standard F Science in Personal and Social Perspectives Standards:Science and Technology in Local Challenges:Science and technology have greatly improved food quality and quantity, transportation, health, sanitation, and communication. These benefits of science and technology are not available to all of the people in the world.");
		String id = "553690e3-86b6-44d2-a9da-5df959084d88";
		AsnMappingStandard asfStd = (AsnMappingStandard)getStandard(id);
		
		if (adnStd == null || asfStd == null) {
			if (adnStd == null)
				prtln ("ADN standard not found");
			if (asfStd == null)
				prtln ("ASF standard not found for id: " + id);
		}
		else {
			prtln ("adnStd:\n" + adnStd.getFullText() + "\n");
			prtln ("asfStd:\n" + asfStd.getAdnText() + "\n");
			if (adnStd.getFullText().equalsIgnoreCase(asfStd.getAdnText()))
				prtln ("\n\n\t==> EQUAL");
			else
				prtln ("\n\n\t==> NOT Equal");
		}
	}
	
	public static DleseStandardsDocument getDleseStandardsDocument () {
		String schemaURLStr = "http://dev.dlese.org:7080/Metadata/adn-item/0.7.00/record.xsd";
		String xpath = "/itemRecord/educational/contentStandards/contentStandard";
		SchemaHelper sh = MappingUtils.getSchemaHelper (schemaURLStr, "itemRecord");
		List dataTypeNames = new ArrayList();
		dataTypeNames.add ("NCGEgeographyContentStandardsType");
		dataTypeNames.add ("NSESscienceContentStandardsAllType");
		List adnStandards = new ArrayList();
		
		DleseStandardsDocument sm = null;
		try {
			// t.setMaxNodes (101);
			sm = new DleseStandardsDocument(sh, dataTypeNames);
		} catch (Exception e) {
			prtln ("getDleseStandardsDocument initialization error: " + e.getMessage());
			e.printStackTrace(); 
			return null;
		}
		return sm;
	}
	
	private static void prtln(String s) {
		if (debug) {
			// System.out.println("AsnMappingDocument: " + s);
			System.out.println(s);
		}
	}
}
