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

import org.dom4j.Element;

import org.dlese.dpc.xml.Dom4jUtils;
import org.dlese.dpc.xml.XPathUtils;
import org.dlese.dpc.util.strings.FindAndReplace;
import java.util.regex.*;

import java.util.*;

/**
 *  Extends AsnStatement to capture document-level information from the
 *  "asn:StandardDocument" statement of a ASN resolver response or a ASN
 *  standards document file.
 *
 * @author    Jonathan Ostwald
 */
public class AsnDocStatement extends AsnStatement {

	private static boolean debug = true;
	private String title;
	private String fileCreated;
	private String created;
	private String exportVersion;
	private String jurisdiction;


	/**
	 *  Constructor for the AsnDocStatement object
	 *
	 * @param  e  NOT YET DOCUMENTED
	 */
	public AsnDocStatement(Element e) {
		super(e);
		this.title = getSubElementText(e, "title");
		this.fileCreated = getSubElementText("fileCreated");
		this.created = getSubElementText("created");
		this.exportVersion = getSubElementText("exportVersion");
		this.jurisdiction = getSubElementResource("jurisdiction");
	}


	/**
	 *  Gets the title attribute of the AsnDocStatement object
	 *
	 * @return    The title value
	 */
	public String getTitle() {
		return this.title;
	}


	/**
	 *  Gets the fileCreated attribute of the AsnDocStatement object
	 *
	 * @return    The fileCreated value
	 */
	public String getFileCreated() {
		return this.fileCreated;
	}


	/**
	 *  Gets the created attribute of the AsnDocStatement object
	 *
	 * @return    The created value
	 */
	public String getCreated() {
		return this.created;
	}


	/**
	 *  Gets the exportVersion attribute of the AsnDocStatement object
	 *
	 * @return    The exportVersion value
	 */
	public String getExportVersion() {
		return this.exportVersion;
	}

	public String getJurisdiction() {
		return this.jurisdiction;
	}
	
	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @return    NOT YET DOCUMENTED
	 */
	public String toString() {
		String s = "\ntitle: " + this.getTitle();
		s += "\n\t" + "fileCreated: " + this.getFileCreated();
		s += "\n\t" + "created: " + this.getCreated();
		s += "\n\t" + "exportVersion: " + this.getExportVersion();
		s += "\n\t" + "jurisdiction: " + this.getJurisdiction();

		s += "\n\n" + "Statement info";
		s += super.toString();

		return s;
	}


	private static void prtln(String s) {
		if (debug) {
			System.out.println("AsnDocStatement: " + s);
		}
	}

}

