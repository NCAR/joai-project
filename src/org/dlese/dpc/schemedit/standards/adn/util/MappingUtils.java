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

import org.dlese.dpc.xml.schema.SchemaHelper;
import java.util.*;
import java.net.*;

public class MappingUtils {
	private static boolean debug = true;
	
	public static SchemaHelper getSchemaHelper(String schemaURLStr, String rootElementName) {

		URL schemaURL = null;
		SchemaHelper sh = null;
		try {
			schemaURL = new URL(schemaURLStr);
			sh = new SchemaHelper(schemaURL, rootElementName);
		} catch (MalformedURLException urlExc) {
			prtln("unable to create URL from " + schemaURLStr);
			return null;
		} catch (Exception e) {
			prtln("failed to instantiate SchemaHelper: " + e);
			return null;
		}
		return sh;
	}
	
	public static DleseStandardsDocument getDleseStandardsDocument () {
		String schemaURLStr = "http://dev.dlese.org:7080/Metadata/adn-item/0.7.00/record.xsd";
		SchemaHelper sh = getSchemaHelper (schemaURLStr, "itemRecord");
		return getDleseStandardsDocument (sh);
	}
		
	public static DleseStandardsDocument getDleseStandardsDocument (SchemaHelper sh) {
		String xpath = "/itemRecord/educational/contentStandards/contentStandard";
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
			// System.out.println("MappingUtils: " + s);
			System.out.println(s);
		}
	}
}
