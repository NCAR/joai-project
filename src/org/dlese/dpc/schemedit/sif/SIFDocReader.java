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

public abstract class SIFDocReader extends NameSpaceXMLDocReader {
	private static boolean debug = true;
	
	public SIFDocReader (String xml) throws Exception {
		super (xml);
	}
	
	public String getRefId () {
		Element root = this.getRootElement();
		return root.attributeValue("RefId");
	}
	
	public static SIFDocReader getReader (String xmlFormat, String xml) throws Exception {
		if ("sif_curriculum".equals (xmlFormat))
			return new CurriculumStructureReader (xml);
		if ("sif_lesson".equals (xmlFormat))
			return new LessonReader (xml);
		if ("sif_activity".equals(xmlFormat))
			return new ActivityReader(xml);
		throw new Exception ("XmlFormat not supported: " + xmlFormat);
	}
		
	public abstract String getHeaderText();
	
	public abstract String getDescriptiveText();
	
	public abstract String getXmlFormat ();
	
	public abstract String getFormatName ();
	
/* 	public static void main (String [] args) throws Exception {
		prtln ("howdy");
		String records = "C:/Documents and Settings/ostwald/devel/dcs-instance-data/ccs/records";
		String path = records + "/sif_curriculum/1210372130672/CUR-000-000-000-001.xml";
		
		String xml = Files.readFile(path).toString();
		
		SIFDocReader reader = new SIFDocReader (xml);
		prtln ("refId: " + reader.getRefId());
		prtln ("title: " + reader.getTitle());
	} */
	
	private static void prtln(String s) {
		if (debug) {
			SchemEditUtils.prtln(s, "SIFDocReader: ");
		}
	}
}
