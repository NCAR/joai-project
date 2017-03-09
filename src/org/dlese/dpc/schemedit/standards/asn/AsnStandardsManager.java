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
package org.dlese.dpc.schemedit.standards.asn;

import org.dlese.dpc.schemedit.standards.StandardsManager;
import org.dlese.dpc.schemedit.standards.StandardsRegistry;
import org.dlese.dpc.schemedit.standards.adn.util.MappingUtils;

import org.dlese.dpc.standards.asn.AsnDocument;

import org.dlese.dpc.xml.schema.*;
import org.dlese.dpc.schemedit.*;

import java.io.*;
import java.util.*;

import java.net.*;

/**
 *  Provides acess to a single ASN Standards Document (and individual standards
 *  contained within) via the AsnDocument and AsnNode classes. Provides lists of
 *  AsnStandardsNodes for use in UI JSP.
 *
 * @author    ostwald
 */

public class AsnStandardsManager implements StandardsManager {
	String xmlFormat;
	String xpath;
	List availableDocs = null;
	private StandardsRegistry standardsRegistry = null;
	private String defaultDocKey = null;


	/**
	 *  Constructor for the AsnStandardsManager object
	 *
	 * @param  xmlFormat      format of framework for this standardsManager
	 * @param  xpath          field for which standards are managed
	 * @param  source         AsnDocument file
	 * @exception  Exception  if AsnDocument file cannot be processed
	 */
	public AsnStandardsManager(String xmlFormat, String xpath, File source) throws Exception {
		this.xmlFormat = xmlFormat;
		this.xpath = xpath;
		this.standardsRegistry = StandardsRegistry.getInstance();

		try {

		} catch (Exception e) {
			throw new Exception("AsnDocument could not be initialized: " + e.getMessage());
		}
		init(source);
	}


	/**
	 *  Initialize the AsnStandardsManager by populating the standardsMap and tree from the provided AsnDocument file.
	 *
	 * @param  source         AsnDocument file
	 * @exception  Exception  if the source file cannot be processed
	 */
	public void init(File source) throws Exception {
		AsnDocInfo docInfo = this.standardsRegistry.register(source.getAbsolutePath());
		this.setDefaultDocKey(docInfo.key);
		this.availableDocs = new ArrayList();
		this.availableDocs.add(docInfo);
	}


	/**
	 *  Gets the standardsRegistry attribute of the AsnStandardsManager object
	 *
	 * @return    The standardsRegistry value
	 */
	public StandardsRegistry getStandardsRegistry() {
		return this.standardsRegistry;
	}


	/**
	 *  Gets the defaultDocKey attribute of the AsnStandardsManager object
	 *
	 * @return    The defaultDocKey value
	 */
	public String getDefaultDocKey() {
		return this.defaultDocKey;
	}


	/**
	 *  Sets the defaultDocKey attribute of the AsnStandardsManager object
	 *
	 * @param  docKey  The new defaultDocKey value
	 */
	public void setDefaultDocKey(String docKey) {
		this.defaultDocKey = docKey;
	}


	/**
	 *  Gets the availableDocs (avaliable ASN Standards Documents) attribute of the AsnStandardsManager object
	 *
	 * @return    The availableDocs value
	 */
	public List getAvailableDocs() {
		return this.availableDocs;
	}


	/**
	 *  Sets the availableDocs attribute of the AsnStandardsManager object
	 *
	 * @param  docs  The new availableDocs value
	 */
	public void setAvailableDocs(List docs) {
		this.availableDocs = docs;
	}


	/**
	 *  Gets the xpath attribute of the AsnStandardsManager object
	 *
	 * @return    The xpath value
	 */
	public String getXmlFormat() {
		return xmlFormat;
	}


	/**
	 *  Gets the xpath attribute of the AsnStandardsManager object
	 *
	 * @return    The xpath value
	 */
	public String getXpath() {
		return xpath;
	}


	protected void setXpath(String xpath) {
		this.xpath = xpath;
	}


	/**
	 *  Gets the rendererTag attribute of the AsnStandardsManager object
	 *
	 * @return    The rendererTag value
	 */
	public String getRendererTag() {
		return "standards_MultiBox";
	}


	/**  prints debugging information about this AsnStandardsManager */
	public void report() {
		prtln("\n----------------------");
		prtln("xmlFormat: " + this.getXmlFormat());
		prtln("xpath: " + this.getXpath());
		prtln("rendererTag: " + this.getRendererTag());
	}


	/**
	 *  Description of the Method
	 *
	 * @param  s  Description of the Parameter
	 */
	private static void prtln(String s) {
		SchemEditUtils.prtln(s, "AsnStandardsManager");
		// System.out.println(s);
	}
}

