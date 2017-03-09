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

import org.dlese.dpc.xml.Dom4jUtils;
import org.dlese.dpc.xml.XPathUtils;
import org.dom4j.*;
import java.util.*;

/**
 *  See http://ndr.comm.nsdl.org/cgi-bin/wiki.pl?addMetadata for documentation
 *  of the InfoStream in MetadataObjects.<p>
 *
 *  NOTE: OAI Harvest info is not relevant to NCS collections, but is required
 *  in the info stream, so we use values that will at lease schema-validate for
 *  harvest-related elements.
 *
 * @author     Jonathan Ostwald
 * @version    $Id: InfoStream.java,v 1.3 2009/03/20 23:33:56 jweather Exp $
 */
public class InfoStream {

	private static boolean debug = true;

	private Document doc = null;

	private String nsdlAboutCategory = null;
	private String repositoryPrimaryIdentifier = null;
	private String link = null;
	private String metadataNamespace = null;

	// OAI-harvest elements
	private String harvestDate = "1970-01-01T00:00:00Z";
	private String harvestHarvestType = "other";
	private String harvestDatasourcePublic = "false";
	private String harvestDatasource = "http://fake.baseurl/metadataNotHarvested";
	private String harvestIdentifier = null;
	private String harvestRecordDatestamp = "1970-01-01T00:00:00Z";


	/**  Constructor for the InfoStream object */
	public InfoStream() { }


	/**
	 *  Constructor for the InfoStream object
	 *
	 * @param  nsdlAboutCategory            either "item" or "collection"
	 * @param  repositoryPrimaryIdentifier  metadata identifier (resourceUrl)
	 * @param  link                         itemId property from collection metadata object
	 * @param  metadataNamespace            XML namespace URI of this format's metadata.
	 * @param  harvestIdentifier            the metadata provider's unique ID for this metadata. (e.g. the metadata provider's OAI identifier). This should be the same as the value of the uniqueID property.
	 */
	public InfoStream(String harvestIdentifier,
	                  String nsdlAboutCategory,
	                  String repositoryPrimaryIdentifier,
	                  String link,
	                  String metadataNamespace) {
		this.setHarvestIdentifier(harvestIdentifier);
		this.setNsdlAboutCategory(nsdlAboutCategory);
		this.setRepositoryPrimaryIdentifier(repositoryPrimaryIdentifier);
		this.setLink(link);
		this.setMetadataNamespace(metadataNamespace);
	}
	
	public InfoStream (Element element) {
		this.setNsdlAboutCategory(getChildText (element, "nsdlAboutCategory"));
		this.setRepositoryPrimaryIdentifier(getChildText (element, "repositoryPrimaryIdentifier"));
		this.setLink(getChildText (element, "link"));
		
		
		this.setHarvestDate(getChildText (element, "harvestDate"));
		this.setHarvestHarvestType(getChildText (element, "harvestHarvestType"));
		this.setHarvestDatasourcePublic(getChildText (element, "harvestDatasourcePublic"));
		this.setHarvestDatasource(getChildText (element, "harvestDatasource"));
		this.setHarvestIdentifier(getChildText (element, "harvestIdentifier"));
		this.setHarvestRecordDatestamp(getChildText (element, "harvestRecordDatestamp"));
		
		
		this.setMetadataNamespace(getChildText (element, "metadataNamespace"));
	}


	/**
	 *  Sets the nsdlAboutCategory attribute of the InfoStream object
	 *
	 * @param  nsdlAboutCategory  The new nsdlAboutCategory value
	 */
	public void setNsdlAboutCategory(String nsdlAboutCategory) {
		this.nsdlAboutCategory = nsdlAboutCategory;
	}


	/**
	 *  Sets the repositoryPrimaryIdentifier attribute of the InfoStream object
	 *
	 * @param  repositoryPrimaryIdentifier  The new repositoryPrimaryIdentifier
	 *      value
	 */
	public void setRepositoryPrimaryIdentifier(String repositoryPrimaryIdentifier) {
		this.repositoryPrimaryIdentifier = repositoryPrimaryIdentifier;
	}


	/**
	 *  Sets the link attribute of the InfoStream object
	 *
	 * @param  link  The new link value
	 */
	public void setLink(String link) {
		this.link = link;
	}


	/**
	 *  Sets the harvestDate attribute of the InfoStream object
	 *
	 * @param  harvestDate  The new harvestDate value
	 */
	public void setHarvestDate(String harvestDate) {
		this.harvestDate = harvestDate;
	}


	/**
	 *  Sets the harvestHarvestType attribute of the InfoStream object
	 *
	 * @param  harvestHarvestType  The new harvestHarvestType value
	 */
	public void setHarvestHarvestType(String harvestHarvestType) {
		this.harvestHarvestType = harvestHarvestType;
	}


	/**
	 *  Sets the harvestDatasourcePublic attribute of the InfoStream object
	 *
	 * @param  harvestDatasourcePublic  The new harvestDatasourcePublic value
	 */
	public void setHarvestDatasourcePublic(String harvestDatasourcePublic) {
		this.harvestDatasourcePublic = harvestDatasourcePublic;
	}


	/**
	 *  Sets the harvestDatasource attribute of the InfoStream object
	 *
	 * @param  harvestDatasource  The new harvestDatasource value
	 */
	public void setHarvestDatasource(String harvestDatasource) {
		this.harvestDatasource = harvestDatasource;
	}


	/**
	 *  Sets the harvestIdentifier attribute of the InfoStream object
	 *
	 * @param  harvestIdentifier  The new harvestIdentifier value
	 */
	public void setHarvestIdentifier(String harvestIdentifier) {
		this.harvestIdentifier = harvestIdentifier;
	}


	/**
	 *  Sets the harvestRecordDatestamp attribute of the InfoStream object
	 *
	 * @param  harvestRecordDatestamp  The new harvestRecordDatestamp value
	 */
	public void setHarvestRecordDatestamp(String harvestRecordDatestamp) {
		this.harvestRecordDatestamp = harvestRecordDatestamp;
	}


	/**
	 *  Sets the metadataNamespace attribute of the InfoStream object
	 *
	 * @param  metadataNamespace  The new metadataNamespace value
	 */
	public void setMetadataNamespace(String metadataNamespace) {
		this.metadataNamespace = metadataNamespace;
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @return    NOT YET DOCUMENTED
	 */
	public Element asElement() {
		Element root = getRootElement();
		// doc is necessary for setNoteText calls
		this.doc = DocumentHelper.createDocument(root);
		if (nsdlAboutCategory != null)
			setNodeText("/info/nsdlAboutCategory", nsdlAboutCategory);
		if (repositoryPrimaryIdentifier != null)
			setNodeText("/info/repositoryPrimaryIdentifier", repositoryPrimaryIdentifier);
		if (link != null)
			setNodeText("/info/link", link);
		if (harvestDate != null)
			setNodeText("/info/harvestDate", harvestDate);
		if (harvestHarvestType != null)
			setNodeText("/info/harvestHarvestType", harvestHarvestType);
		if (harvestDatasourcePublic != null)
			setNodeText("/info/harvestDatasourcePublic", harvestDatasourcePublic);
		if (harvestDatasource != null)
			setNodeText("/info/harvestDatasource", harvestDatasource);
		if (harvestIdentifier != null)
			setNodeText("/info/harvestIdentifier", harvestIdentifier);
		if (harvestRecordDatestamp != null)
			setNodeText("/info/harvestRecordDatestamp", harvestRecordDatestamp);
		if (metadataNamespace != null)
			setNodeText("/info/metadataNamespace", metadataNamespace);

		return root.createCopy();
	}


	/**
	 *  Gets the rootElement attribute of the InfoStream object
	 *
	 * @return    The rootElement value
	 */
	private Element getRootElement() {
		Element root = DocumentHelper.createElement("info");
		// root.add(DocumentHelper.createNamespace("dc", "http://purl.org/dc/elements/1.1/"));

		return root;
	}


	/**
	 *  The main program for the InfoStream class
	 *
	 * @param  args           The command line arguments
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public static void main(String[] args) throws Exception {
		InfoStream is = getFakeInfoStream();
		pp(is.asElement());
	}


	/**
	 *  Gets the fakeInfoStream attribute of the InfoStream class
	 *
	 * @return    The fakeInfoStream value
	 */
	public static InfoStream getFakeInfoStream() {
		InfoStream is = new InfoStream();
		is.setRepositoryPrimaryIdentifier("I am a FAKE info stream");
		is.setHarvestDate("harvest date");
		is.setHarvestDatasourcePublic("i'm a data source!");
		is.setHarvestHarvestType("content");
		return is;
	}


	/**
	 *  Gets the node attribute of the InfoStream object
	 *
	 * @param  xpath  NOT YET DOCUMENTED
	 * @return        The node value
	 */
	private Node getNode(String xpath) {
		return this.doc.selectSingleNode(xpath);
	}

	private String getChildText (Element parent, String tag) {
		try {
			return parent.element(tag).getTextTrim();
		} catch (Exception e) {}
		return "";
	}

	/**
	 *  Sets the nodeText attribute of the InfoStream object
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

