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
package org.dlese.dpc.schemedit.sif;

import org.dlese.dpc.schemedit.SchemEditUtils;
import org.dlese.dpc.standards.asn.NameSpaceXMLDocReader;
import org.dlese.dpc.util.Files;
import org.dom4j.Element;
import org.dom4j.DocumentHelper;

public class CurriculumStructureReader extends  SIFDocReader {
	private static boolean debug = true;
	
	public CurriculumStructureReader (String xml) throws Exception {
		super (xml);
	}
	
	public String getHeaderText () {
		return getTitle();
	}
	
	public String getDescriptiveText() {
		return getDescription();
	}
	
	public String getXmlFormat () {
		return "sif_curriculum";
	}
	
	public String getFormatName () {
		return "CurriculumStructure";
	}		
	
	public String getTitle () {
		return getNodeText ("/sif:CurriculumStructure/sif:Titles/sif:Title");
	}
	
	public String getDescription () {
		return getNodeText ("/sif:CurriculumStructure/sif:Description");
	}
	
 	public static void main (String [] args) throws Exception {
		prtln ("howdy");
		String records = "C:/Documents and Settings/ostwald/devel/dcs-instance-data/ccs/records";
		String path = records + "/sif_curriculum/1210372130672/CUR-000-000-000-001.xml";
		
		String xml = Files.readFile(path).toString();
		
		CurriculumStructureReader reader = new CurriculumStructureReader (xml);
		prtln ("xmlFormat: " + reader.getXmlFormat());
		prtln ("formatName: " + reader.getFormatName());
		prtln ("refId: " + reader.getRefId());
		prtln ("title: " + reader.getTitle());
		prtln ("description: " + reader.getDescription());
	}
	
	private static void prtln(String s) {
		if (debug) {
			SchemEditUtils.prtln(s, "CurriculumStructureReader: ");
		}
	}
}
