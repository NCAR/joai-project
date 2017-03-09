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
package org.dlese.dpc.ndr.apiproxy;

import java.util.*;
import org.dlese.dpc.xml.Dom4jUtils;
import org.dom4j.*;

/**
 *  Class representing the payload for most {@link
 *  org.dlese.dpc.ndr.request.NdrRequest}s. See <a
 *  href="http://ndr.comm.nsdl.org/cgi-bin/wiki.pl?APIBasics">NDR APIBasics</a>
 *  for more information about the InputXML parameter.
 *
 * @author    ostwald<p>
 *
 *
 */
public class InputXML {

	private static boolean debug = true;

	private NDRConstants.NDRObjectType objectType = null;
	private InputXMLComponent properties = null;
	private DataComponent data = null;
	private InputXMLComponent relationships = null;


	/**
	 *  Constructor for the InputXML object for the specified NDR object type.
	 *
	 * @param  objectType  NOT YET DOCUMENTED
	 */
	public InputXML(NDRConstants.NDRObjectType objectType) {
		this.objectType = objectType;
	}


	// -----------  PROPERTIES ----------

	/**
	 *  Gets the properties component of the InputXML object.
	 *
	 * @return    The properties component.
	 */
	public InputXMLComponent getProperties() {
		if (this.properties == null) {
			this.properties = new InputXMLComponent("properties");
		}
		return this.properties;
	}


	// -----------  RELATIONSHIPS ----------

	/**
	 *  Gets the relationships component of the InputXML object
	 *
	 * @return    the Relationships component.
	 */
	public InputXMLComponent getRelationships() {
		if (this.relationships == null) {
			this.relationships = new InputXMLComponent("relationships");
		}
		return this.relationships;
	}


	/**
	 *  Gets the data component of the InputXML object. This is where the data
	 *  streams & serviceDescriptions are located.
	 *
	 * @return    The data component.
	 */
	public DataComponent getData() {
		if (this.data == null) {
			this.data = new DataComponent();
		}
		return this.data;
	}


	/**
	 *  xsi:schemaLocation="http://ns.nsdl.org/ndr/request_v1.00/
	 *  http://ns.nsdl.org/schemas/ndr/request_v1.00.xsd" schemaVersion="1.00.000"
	 *
	 * @return    The rootElement value
	 */
	private Element getRootElement() {
		Element root = DocumentHelper.createElement("InputXML");
		String defaultNsUri = "http://ns.nsdl.org/ndr/request_v1.00/";
		String schemaUri = "http://ns.nsdl.org/schemas/ndr/request_v1.00.xsd";
		root.addAttribute("xmlns", defaultNsUri);

		String xsiUri = "http://www.w3.org/2001/XMLSchema-instance";
		Namespace xsiNs = DocumentHelper.createNamespace("xsi", xsiUri);
		root.add(xsiNs);

		root.addAttribute("xsi:schemaLocation", defaultNsUri + " " + schemaUri);
		root.addAttribute("schemaVersion", "1.00.000");
		return root;
	}


	/**
	 *  Get an org.dom4j.Document representation of the inputXML.
	 *
	 * @return    NOT YET DOCUMENTED
	 */
	public Document asDocument() {
		Element root = this.getRootElement();
		Element obj = root.addElement(this.objectType.getTag());

		if (this.properties != null) {
			obj.add(this.properties.asElement());
		}
		if (this.relationships != null) {
			obj.add(this.relationships.asElement());
		}
		if (this.data != null) {
			obj.add(this.data.asElement());
		}

		return DocumentHelper.createDocument(root);
	}


	/**
	 *  Returns a String representation of the InputXML instance for use as a
	 *  NdrRequest parameter.
	 *
	 * @return    inputXML as XML string
	 */
	public String asXML() {
		return Dom4jUtils.prettyPrint(this.asDocument());
	}


	/**
	 *  The main program for the InputXML class
	 *
	 * @param  args           The command line arguments
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public static void main(String[] args) throws Exception {
		prtln("yo");
		InputXML input = new InputXML(NDRConstants.NDRObjectType.AGGREGATOR);
		InputXMLComponent properties = input.getProperties();
		properties.addCommand("metadataFor", "2200/asdfjiaserlT", "delete");
		properties.addCommand("metadataFor", "2200/blamasdre", "delete");
		properties.addCommand("metadataFor", "2200/blamasdre");

		DataComponent data = input.getData();
		data.addDataStreamCmd("ncs_item", makeDataStream());
		data.addDataStreamCmd("ncs_item", makeDataStream(), "delete");

		pp(input.asDocument());
	}


	/**
	 *  Creates a fake data stream record for testing purposes.
	 *
	 * @return    DataStream element.
	 */
	private static Element makeDataStream() {
		Element ds = DocumentHelper.createElement("DataStreamRecord");
		ds.addElement("RecordId").setText("ID-000-000-001");
		return ds;
	}


	/**
	 *  Gets String representation of Namespace for debugging purposes.
	 *
	 * @param  ns  NOT YET DOCUMENTED
	 * @return     NOT YET DOCUMENTED
	 */
	static String nsToString(Namespace ns) {
		return ns.getPrefix() + " : " + ns.getURI();
	}


	/**
	 *  Pretty prints a DOM object for debugging purposes.
	 *
	 * @param  n  NOT YET DOCUMENTED
	 */
	private static void pp(Node n) {
		prtln(Dom4jUtils.prettyPrint(n));
	}


	/**
	 *  Description of the Method
	 *
	 * @param  s  Description of the Parameter
	 */
	private static void prtln(String s) {
		if (debug) {
			// System.out.println("InputXML: " + s);
			System.out.println(s);
		}
	}

}

