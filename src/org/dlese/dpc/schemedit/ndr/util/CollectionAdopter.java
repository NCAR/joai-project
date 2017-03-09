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
package org.dlese.dpc.schemedit.ndr.util;

import org.dlese.dpc.ndr.NdrUtils;
import org.dlese.dpc.ndr.reader.*;
import org.dlese.dpc.ndr.request.*;
import org.dlese.dpc.ndr.apiproxy.InfoXML;
import org.dlese.dpc.ndr.apiproxy.NDRConstants;
import org.dlese.dpc.ndr.apiproxy.NDRConstants.NDRObjectType;
import org.dlese.dpc.schemedit.SchemEditUtils;
import org.dlese.dpc.schemedit.test.TesterUtils;
import org.dlese.dpc.schemedit.config.CollectionConfigReader;
import org.dlese.dpc.xml.Dom4jUtils;
import org.dlese.dpc.xml.XSLTransformer;
import org.dlese.dpc.util.Files;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.XPath;
import java.io.File;
import java.net.URL;
import java.util.*;
import java.text.*;
import javax.xml.transform.Transformer;

/**
 *  Use Case: there is a collection in the ndr that is not represented by a NSDL
 *  collection record in the NCS. We want to this collection in the NCS,
 *  requiring that not only we create a collection record (and hook it up with
 *  the appropriate objects in the NDR, but we also want to create an ncs_item
 *  collection for managing it's items. so we need to create ncs_item records
 *  (based on nsdl_dc streams in ndr items), and also dcs_data records
 *  (containing the ndr handle of the corresponding items).
 *
 * @author     ostwald<p>
 *
 *
 */
public class CollectionAdopter {

	private static boolean debug = true;
	String xslPath = "C:/Documents and Settings/ostwald/devel/projects/dcs-project/web/WEB-INF/xsl_files/nsdl-dc-v1.02.020-to-ncs-item-v1.02.xsl";
	int counter = 0;
	File output = new File("C:/tmp/CollectionAdopter/ndrMetadataInfoRecords");


	/**
	 *  Constructor for the CollectionAdopter object
	 *
	 * @param  mdpHandle      NOT YET DOCUMENTED
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public CollectionAdopter(String mdpHandle) throws Exception {
		SimpleNdrRequest.setDebug(true);
		SimpleNdrRequest.setVerbose(false);
		MetadataProviderReader mdp = new MetadataProviderReader(mdpHandle);
		List itemHandles = mdp.getItemHandles();
		prtln("There are " + itemHandles.size() + " records to retrieve");
		for (Iterator i = itemHandles.iterator(); i.hasNext(); ) {
			String itemHandle = (String) i.next();
			MetadataReader mdReader = new MetadataReader(itemHandle);
			// prtln ("\n" + mdReader.getHandle());
			Element nsdl_dc_stream = (mdReader.getDataStream("nsdl_dc"));
			// pp (nsdl_dc_stream);
			// prtln ("----------");
			Element ncs_item_stream = this.getNcsItemStream(nsdl_dc_stream);
			// pp (ncs_item_stream);

			Element root = DocumentHelper.createElement("ndrMetadataInfo");

			Element ndrHandle_element = root.addElement("ndrHandle");
			ndrHandle_element.setText(itemHandle);
			Element nsdl_dc_element = root.addElement("nsdl_dc");
			if (nsdl_dc_stream != null)
				nsdl_dc_element.add(nsdl_dc_stream);
			Element ncs_item_element = root.addElement("ncs_item");
			if (ncs_item_stream != null)
				ncs_item_element.add(ncs_item_stream);
			Document doc = DocumentHelper.createDocument(root);
			// pp (doc);
			this.writeDoc(doc);
		}
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  doc  NOT YET DOCUMENTED
	 */
	void writeDoc(Document doc) {
		String filename = String.valueOf(++this.counter) + ".xml";
		File dest = new File(this.output, filename);
		try {
			Dom4jUtils.writeDocToFile(doc, dest);
			prtln("wrote to " + dest);
		} catch (Exception e) {
			prtln("couldn't write to " + dest + ": " + e.getMessage());
		}
	}

	/**
	 *  The main program for the CollectionAdopter class
	 *
	 * @param  args           The command line arguments
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public static void main(String[] args) throws Exception {
		TesterUtils.setSystemProps();
		File propFile = null; // propFile must be assigned!
		NdrUtils.setup (propFile);

		String mdpHandle = "2200/20061002124915923T";
		CollectionAdopter ca = new CollectionAdopter(mdpHandle);
	}


	/**
	 *  A unit test for JUnit
	 *
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	static void tester() throws Exception {
		String ndrServer = "ndrtest.nsdl.org";
		String ncsAgent = "2200/test.20060829130238941T"; // ncs.nsdl.org
		String keyFile = "C:/mykeys/ndrtest_private";
		NdrUtils.setup(ndrServer, ncsAgent, keyFile);

		String mdpHandle = "2200/test.20090211175403264T";
		CollectionAdopter ca = new CollectionAdopter(mdpHandle);
	}


	/**
	 *  Gets the ncsItemStream attribute of the CollectionAdopter object
	 *
	 * @param  nsdl_dc_stream  NOT YET DOCUMENTED
	 * @return                 The ncsItemStream value
	 * @exception  Exception   NOT YET DOCUMENTED
	 */
	public Element getNcsItemStream(Element nsdl_dc_stream) throws Exception {
		// return XSLTransformer.transformString (nsdl_dc, this.xslPath);
		String className = "net.sf.saxon.TransformerFactoryImpl";
		if (nsdl_dc_stream == null)
			return null;
		String nsdl_dc = nsdl_dc_stream.asXML();
		try {
			Transformer transformer =
				XSLTransformer.getTransformer(this.xslPath, className);
			String xml = XSLTransformer.transformString(nsdl_dc, transformer);
			if (xml != null && xml.trim().length() > 0)
				return DocumentHelper.parseText(xml).getRootElement();
		} catch (Throwable t) {
			prtln("transform problem: " + t.getMessage());
		}
		return null;
	}


	/**
	 *  Doesn't work for some strange reason ...
	 *
	 * @param  mdHandle       NOT YET DOCUMENTED
	 * @param  fmt            NOT YET DOCUMENTED
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	void deleteDataStream(String mdHandle, String fmt) throws Exception {
		ModifyMetadataRequest request = new ModifyMetadataRequest(mdHandle);
		request.deleteDataStream(fmt);
		request.submit();
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  node  NOT YET DOCUMENTED
	 */
	private static void pp(Node node) {
		prtln(Dom4jUtils.prettyPrint(node));
	}


	/**
	 *  Sets the debug attribute of the CollectionAdopter class
	 *
	 * @param  bool  The new debug value
	 */
	public static void setDebug(boolean bool) {
		debug = bool;
	}


	/**
	 *  Description of the Method
	 *
	 * @param  s  Description of the Parameter
	 */
	private static void prtln(String s) {
		if (debug) {
			// SchemEditUtils.prtln(s, "CollectionAdopter");
			SchemEditUtils.prtln(s, "");
		}
	}

}

