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

import org.dlese.dpc.ndr.apiproxy.NDRConstants;
import org.dlese.dpc.ndr.NdrUtils;
import org.dlese.dpc.xml.Dom4jUtils;
import org.dlese.dpc.xml.XPathUtils;
import org.dom4j.*;
import java.util.*;
import java.io.File;

/**
 *  Class to extract information from a ncs_collect metadata record represented
 *  as a dom4j.Document.
 *
 * @author     Jonathan Ostwald
 */
public class NCSCollectReader {

	private static boolean debug = true;

	/**  NOT YET DOCUMENTED */
	public Document doc = null;


	/**
	 *  Constructor for the NCSCollectReader object given a dom4j.Document in
	 *  ncs_collect format.
	 *
	 * @param  doc  NOT YET DOCUMENTED
	 */
	public NCSCollectReader(Document doc) {
		this.doc = Dom4jUtils.localizeXml(doc);
	}


	/**
	 *  Gets the oaiBaseUrl attribute of the NCSCollectReader object
	 *
	 * @return    The oaiBaseUrl value
	 */
	public String getOaiBaseUrl() {
		return getNodeText("record/collection/ingest/oai/@baseURL");
	}


	/**
	 *  Gets the oaiVisibility attribute of the NCSCollectReader object
	 *
	 * @return    The oaiVisibility value
	 */
	public NDRConstants.OAIVisibilty getOaiVisibility() {
		String s = getNodeText("/record/collection/OAIvisibility");
		try {
			return NDRConstants.OAIVisibilty.getVisibility(s);
		} catch (Throwable t) {
			prtln("couldn't make a visibility enum from \"" + s + "\"");
		}
		return NDRConstants.OAIVisibilty.PRIVATE;
	}


	/**
	 *  Gets the writableDocument attribute of the NCSCollectReader object
	 *
	 * @return    The writableDocument value
	 */
	public Document getWritableDocument() {
		Element root = doc.getRootElement().createCopy();

		root.addAttribute("xmlns", "http://ns.nsdl.org/ncs");

		root.add(DocumentHelper.createNamespace("xsi", "http://www.w3.org/2001/XMLSchema-instance"));
		root.addAttribute("schemaLocation",
			"http://ns.nsdl.org/ncs http://ns.nsdl.org/ncs/ncs_collect/1.02/schemas/ncs-collect.xsd");
		return DocumentHelper.createDocument(root);
	}


	/**
	 *  Gets the oaiIngest attribute of the NCSCollectReader object
	 *
	 * @return    The oaiIngest value
	 */
	public boolean isOaiIngest() {
		return (getNode("/record/collection/ingest/oai") != null);
	}

	public String getOaiFormat() {
		return (getNodeText("/record/collection/ingest/oai/@format"));
	}

	/**
	 *  Gets the title attribute of the NCSCollectReader object
	 *
	 * @return    The title value
	 */
	public String getTitle() {
		return getNodeText("record/general/title");
	}


	/**
	 *  Gets the url attribute of the NCSCollectReader object
	 *
	 * @return    The url value
	 */
	public String getUrl() {
		return getNodeText("record/general/url");
	}


	/**
	 *  Gets the recordID attribute of the NCSCollectReader object
	 *
	 * @return    The recordID value
	 */
	public String getRecordID() {
		return getNodeText("record/general/recordID");
	}


	/**
	 *  Gets the description attribute of the NCSCollectReader object
	 *
	 * @return    The description value
	 */
	public String getDescription() {
		return getNodeText("record/general/description");
	}


	private String brandURLPath = "record/collection/brandURL";


	/**
	 *  Gets the brandURL attribute of the NCSCollectReader object
	 *
	 * @return    The brandURL value
	 */
	public String getBrandURL() {
		return getNodeText("record/collection/brandURL");
	}


	/**
	 *  Sets the brandURL attribute of the NCSCollectReader object
	 *
	 * @param  url  The new brandURL value
	 */
	public void setBrandURL(String url) {
		setNodeText(brandURLPath, url);
	}


	private String imageWidthPath = "record/collection/imageWidth";


	/**
	 *  Gets the imageWidth attribute of the NCSCollectReader object
	 *
	 * @return    The imageWidth value
	 */
	public String getImageWidth() {
		return getNodeText("record/collection/imageWidth");
	}


	/**
	 *  Sets the imageWidth attribute of the NCSCollectReader object
	 *
	 * @param  width  The new imageWidth value
	 */
	public void setImageWidth(String width) {
		setNodeText(imageWidthPath, width);
	}


	private String imageHeightPath = "record/collection/imageHeight";


	/**
	 *  Gets the imageHeight attribute of the NCSCollectReader object
	 *
	 * @return    The imageHeight value
	 */
	public String getImageHeight() {
		return getNodeText(imageHeightPath);
	}


	/**
	 *  Sets the imageHeight attribute of the NCSCollectReader object
	 *
	 * @param  height  The new imageHeight value
	 */
	public void setImageHeight(String height) {
		setNodeText(imageHeightPath, height);
	}


	/**
	 *  Gets the sets attribute of the NCSCollectReader object
	 *
	 * @return    The sets value
	 */
	public List getSets() {
		return getValues("/record/collection/ingest/oai/set");
	}


	private String altTextPath = "record/collection/altText";


	/**
	 *  Gets the altText attribute of the NCSCollectReader object
	 *
	 * @return    The altText value
	 */
	public String getAltText() {
		return getNodeText("record/collection/altText");
	}


	/**
	 *  Sets the altText attribute of the NCSCollectReader object
	 *
	 * @param  width  The new altText value
	 */
	public void setAltText(String width) {
		setNodeText(altTextPath, width);
	}


	/**
	 *  Gets the contacts attribute of the NCSCollectReader object
	 *
	 * @return    The contacts value
	 */
	public List getContacts() {
		List contacts = new ArrayList();
		List nodes = this.doc.selectNodes("/record/collection/contacts/contact");
		for (Iterator i = nodes.iterator(); i.hasNext(); ) {
			Element element = (Element) i.next();
			Contact contact = new Contact(element);
			contacts.add(contact);
		}
		return contacts;
	}


	/**
	 *  Sets the contacts attribute of the NCSCollectReader object
	 *
	 * @param  contacts  The new contacts value
	 */
	public void setContacts(List contacts) {
		Element contactsEl = DocumentHelper.makeElement(doc, "/record/collection/contacts");
		contactsEl.clearContent();
		for (Iterator i = contacts.iterator(); i.hasNext(); ) {
			Contact contact = (Contact) i.next();
			contactsEl.add(contact.asElement());
		}
	}

	/**  NOT YET DOCUMENTED */
	public static String collectionAgentPath = "/record/collection/collectionAgent/handle";


	/**
	 *  Gets the collectionAgent attribute of the NCSCollectReader object
	 *
	 * @return    The collectionAgent value
	 */
	public String getCollectionAgent() {
		return getNodeText(collectionAgentPath);
	}


	/**
	 *  Sets the collectionAgent attribute of the NCSCollectReader object
	 *
	 * @param  agentHandle  The new collectionAgent value
	 */
	public void setCollectionAgent(String agentHandle) {
		setNodeText(collectionAgentPath, agentHandle);
	}


	/**
	 *  Gets the applicationAgent attribute of the NCSCollectReader object
	 *
	 * @return    The applicationAgent value
	 */
	public String getApplicationAgent() {
		return getNodeText("/record/collection/ingest/applicationAgent/handle");
	}

	/**
	 *  The main program for the NCSCollectReader class
	 *
	 * @param  args           The command line arguments
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public static void main(String[] args) throws Exception {
		org.dlese.dpc.schemedit.test.TesterUtils.setSystemProps();
		boolean readFromFile = true;
		NCSCollectReader reader = null;
		
		if (readFromFile) {
			// String path = "H:/python-lib/dcsTools/recommender/dcs_collect.xml";
			File dir = new File("C:/Documents and Settings/ostwald/devel/dcs-instance-data/local-ndr/records/ncs_collect/1256687231238");
			String filename = "NCS-COL-000-000-000-002.xml";
			Document ncs_collect = Dom4jUtils.getXmlDocument(new File(dir, filename));
			reader = new NCSCollectReader(ncs_collect);
		}
		else {
			// String ncsRecId = "NSDL-COLLECTION-4743";
			String ncsRecId = "NSDL-COLLECTION-27007";

			// Get a ncsCollect record using webservice
			reader = org.dlese.dpc.schemedit.ndr.util.integration.NSDLCollectionUtils.getNCSRecord(ncsRecId);
			if (reader == null)
				throw new Exception("did not find NCS Record for " + ncsRecId);
		}

		pp(reader.doc);

		prtln("title: " + reader.getTitle());
		prtln("collection agent: " + reader.getCollectionAgent());
		prtln("is OAI ingest? " + reader.isOaiIngest());
		prtln("OAI format: " + reader.getOaiFormat());
		prtln(reader.getSets().size() + " sets found");
		/* prtln ("metadataProvidedBy: " + reader.getMetadataProvidedBy()); */
	}


	/**  NOT YET DOCUMENTED */
	public void serviceDescriptionTest() {
		ServiceDescription sd =
			ServiceDescription.makeServiceDescription(this, NDRConstants.NDRObjectType.AGGREGATOR);
		pp(sd.asElement());
	}


	/**
	 *  Utility to gets a node at the specified xpath.
	 *
	 * @param  xpath  NOT YET DOCUMENTED
	 * @return        The node value
	 */
	private Node getNode(String xpath) {
		return this.doc.selectSingleNode(xpath);
	}


	/**
	 *  Gets the values attribute of the NCSCollectReader object
	 *
	 * @param  xpath  NOT YET DOCUMENTED
	 * @return        The values value
	 */
	private List getValues(String xpath) {
		List nodes = doc.selectNodes(xpath);
		List values = new ArrayList();
		for (Iterator i = nodes.iterator(); i.hasNext(); ) {
			values.add(((Node) i.next()).getText());
		}
		return values;
	}


	/**
	 *  requires at least the parent exists. fails silently.
	 *
	 * @param  xpath  The new nodeText value
	 * @param  text   The new nodeText value
	 */
	private void setNodeText(String xpath, String text) {
		Node node = getNode(xpath);
		if (node == null) {
			Element parent = (Element) getNode(XPathUtils.getParentXPath(xpath));
			if (parent == null) {
				prtln("Neither node or parent found for " + xpath);
				return;
			}
			node = parent.addElement(XPathUtils.getLeaf(xpath));
		}
		node.setText(text);
	}


	/**
	 *  Gets the text of the node at the specified xpath, or null if no node
	 *  exists.
	 *
	 * @param  xpath  NOT YET DOCUMENTED
	 * @return        The nodeText value
	 */
	private String getNodeText(String xpath) {
		Node node = getNode(xpath);
		if (node == null) {
			return "";
		}
		return node.getText();
	}


	/**
	 *  NOT YET DOCUMENTED
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
			System.out.println(s);
		}
	}
}

