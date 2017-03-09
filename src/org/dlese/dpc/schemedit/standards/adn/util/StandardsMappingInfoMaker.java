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

public class StandardsMappingInfoMaker {
	private static boolean debug = true;
	public DleseStandardsDocument adnMgr;
	public AsnMappingDocument asnDoc;
	// private AsnToAdnHelper asnToAdnHelper;
	public Map asn2adnMap;
	public Map adn2asnMap;
	public String mappingsFile;
	
	DocumentFactory df;
	
	// the file that is written as output by the matcher
	String mappingInfoPath = "/devel/ostwald/projects/schemedit-project/web/WEB-INF/data/Adn-to-Asn-v1.2.5-info.xml";
	
	public StandardsMappingInfoMaker () {
		
		df = DocumentFactory.getInstance();
		
		// initialize ADN standards manager
		String schemaURLStr = "http://www.dlese.org/Metadata/adn-item/0.7.00/record.xsd";
		try {
			SchemaHelper sh = MappingUtils.getSchemaHelper (schemaURLStr, "itemRecord");
			if (sh == null)
				throw new Exception ("could not initialize schemaHelper with " + schemaURLStr);
			
			prtln ("\n initializing ADN standards manager ...");
			adnMgr = MappingUtils.getDleseStandardsDocument(sh);
			if (adnMgr == null)
				throw new Exception ("unable to initialize ADN standards manager");
			prtln (" ... " + adnMgr.size() + " ADN standards found");
			

			// initialize ASN standards manager
			prtln ("\n initializing ASN standards manager ...");
			String standardsDocPath = "/home/ostwald/python-lib/asn/standards-documents/localized/1995-NSES-v1.2.5-072006-localized.xml";
			asnDoc = new AsnMappingDocument (standardsDocPath);
			
			// initialize AsnToAdnHelper
			// String mapperPath = "/devel/ostwald/projects/schemedit-project/web/WEB-INF/data/Adn-to-Asf-mappings.xml";
			mappingsFile = "/devel/ostwald/projects/schemedit-project/web/WEB-INF/data/ADN-ASN-v1.2.5-mappings.xml";
			// asnToAdnHelper = new AsnToAdnHelper (mapperPath);
			
			prtln ("\n initializing initAsn2adnMap ...");
			initTranslationMaps (mappingsFile);
			
		} catch (Throwable e) {
			prtln ("\nStandardsMappingInfoMaker error: " + e.getMessage());
		}
	}
	

	

	
	void initTranslationMaps (String mappingsDocPath) {
		Document doc = null;
		try {
			doc = Dom4jUtils.getXmlDocument(new File(mappingsDocPath));
		} catch (Exception e) {
			prtln("Couldn't read mappings doc: " + e.getMessage());
			return;
		}

		// prtln (Dom4jUtils.prettyPrint(doc));
		asn2adnMap = new HashMap();
		adn2asnMap = new HashMap();
		List mappings = doc.selectNodes("/Adn-to-Asn-mappings/mapping");
		prtln(mappings.size() + " mappings found");
		for (Iterator i = mappings.iterator(); i.hasNext(); ) {
			Element e = (Element) i.next();
			String id = e.attributeValue("asnIdentifier");
			String text = e.getText();
			
			if (asn2adnMap.get(id) != null) {
				prtln ("\t ASN collision with " + id);
			}
			else
				asn2adnMap.put(id, text);
			
			if (adn2asnMap.get(text) != null) {
				prtln ("\t ADN collision with " + text);
			}
			else
				adn2asnMap.put(text, id);
		}
		prtln("asn2adnMap initialized with " + asn2adnMap.size() + " items");
		prtln("adn2asnMap initialized with " + adn2asnMap.size() + " items");
	}
	
	public static void main (String[] args) {
		StandardsMappingInfoMaker matcher = new StandardsMappingInfoMaker();
		Document doc = null;
		File out = new File (matcher.mappingInfoPath);
		try {
			doc = matcher.makeMappingInfoDoc();
			Dom4jUtils.writeDocToFile(doc, out);
		} catch (Exception e) {
			prtln (e.getMessage());
			return;
		}
		// prtln (Dom4jUtils.prettyPrint(doc));
		if (out.exists())
			prtln ("new file exists at " + out.getAbsolutePath());
		else
			prtln ("new file DOES NOT EXIST!");
	}
		
	
	/**
	* Create an xml document containing an element for each 4th level standard. The standards elements
	* have the following attributes:<ul>
	<li>id (asnID)
	<li>adnText (text of the ADN vocab)
	<li>asnText (text derived from ASN node's "itemDescription" plus that of it's ancestors). this resembles the adnText, 
	but is not the same string
	</ul>
	*/	
	Document makeMappingInfoDoc () throws Exception {
		Element root = df.createElement("Adn-to-Asn-info");
		for (Iterator i = asn2adnMap.keySet().iterator();i.hasNext();) {
			String id = (String)i.next();
			AsnMappingStandard asnStd = (AsnMappingStandard) asnDoc.getStandard(id);
			if (asnStd == null) {
				throw new Exception ("could not retrieve ASN standard for " + id);
			}
			String asnText = asnStd.getDisplayText();
			// String adnText = asnToAdnHelper.getAdnText(id);
			String adnText = (String) asn2adnMap.get(id);
			Element stdElement = root.addElement("standard");
			stdElement.addAttribute("id", id);
			stdElement.addElement("adnText").setText (adnText);
			stdElement.addElement("asnText").setText (asnText);
		}
		return df.createDocument(root);
	}
		
			
			
	
	private static void prtln(String s) {
		if (debug) {
			while (s.length() > 0 && s.charAt(0) == '\n') {
				System.out.println ("");
				s = s.substring(1);
			}
			// System.out.println("StandardsMappingInfoMaker: " + s);
			System.out.println(s);
		}
	}
}
