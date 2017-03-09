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

import org.dlese.dpc.index.reader.DleseCollectionDocReader;
import org.dlese.dpc.ndr.apiproxy.NDRConstants;
import org.dlese.dpc.ndr.NdrUtils;
import org.dlese.dpc.xml.Dom4jUtils;
import org.dlese.dpc.xml.XPathUtils;
import org.dom4j.*;
import java.util.*;
import java.io.File;

/**
 *  Class to construct serviceDescription elements to be used in NDR Objects (i.e., MetadataProvider and Aggregator),
 as described at http://ndr.comm.nsdl.org/cgi-bin/wiki.pl?addMetadataProvider.
 *
 * @author     Jonathan Ostwald
 * @version    $Id: ServiceDescription.java,v 1.6 2009/08/19 18:25:45 ostwald Exp $
 */
public class ServiceDescription {

	private static boolean debug = true;

	private Document doc = null;
	private List contacts = new ArrayList();
	private Image image = null;
	private String title = null;
	private String description = null;
	private String type = null;
	private String identifier = null;


	/**  Constructor for the ServiceDescription object */
	public ServiceDescription() {
		this.contacts = new ArrayList();
	}


	/**
	 *  Constructor for the ServiceDescription object, given title, description and type values.
	 *
	 * @param  title        NOT YET DOCUMENTED
	 * @param  description  NOT YET DOCUMENTED
	 * @param  type         NOT YET DOCUMENTED
	 */
	public ServiceDescription(String title, String description, String identifier) {
		this();
		setTitle(title);
		setDescription(description);
		setIdentifier(identifier);
		// setType(type);
	}
	
	public ServiceDescription (Element element) {
		this();
		Document doc = DocumentHelper.createDocument (element);
		doc = Dom4jUtils.localizeXml(doc);
		
		this.setTitle(getChildText (element, "title"));
		this.setDescription(getChildText (element, "description"));
		this.setType (getChildText (element, "type"));
		
		Element imageElement = element.element ("image");
		this.setImage(new Image (imageElement));
		
		List contactNodes = doc.selectNodes ("/serviceDescription/contacts/contact");
		if (!contactNodes.isEmpty()) {
			for (Iterator i=contactNodes.iterator();i.hasNext();) {
				Element contactElement = (Element)i.next();
				this.addContact (Contact.getInstance (contactElement));
			}
		}
	}


	/**
	 *  Creates a Contact instance and adds it to the serviceDescription.
	 *
	 * @param  name   contact name
	 * @param  email  contact email
	 * @param  info   contact info
	 */
	public void addContact(String name, String email, String info) {
		Contact contact = new Contact(name, email, info);
		contacts.add(contact);
	}


	/**
	 *  Adds a contact instance to the serviceDescription.
	 *
	 * @param  contact  The Contact to be added
	 */
	public void addContact(Contact contact) {
		contacts.add(contact);
	}
	
	public List getContacts () {
		return this.contacts;
	}


	/**
	 *  Creates an image instance and adds it to the ServiceDescription object
	 *
	 * @param  brandUrl  image brandUrl
	 * @param  title     image title
	 * @param  width     image width
	 * @param  height    image height
	 * @param  alttext   image alttext
	 */
	public void setImage(String brandUrl, String title, String width, String height, String alttext) {
		this.image = new Image(brandUrl, title, width, height, alttext);
	}


	/**
	 *  Sets the image attribute of the ServiceDescription object
	 *
	 * @param  image  The new image value
	 */
	public void setImage(Image image) {
		this.image = image;
	}

	public Image getImage () {
		return this.image;
	}

	/**
	 *  Sets the title attribute of the ServiceDescription object
	 *
	 * @param  title  The new title value
	 */
	public void setTitle(String title) {
		this.title = title;
	}


	/**
	 *  Sets the description attribute of the ServiceDescription object
	 *
	 * @param  description  The new description value
	 */
	public void setDescription(String description) {
		this.description = description;
	}


	/**
	 *  Sets the type attribute of the ServiceDescription object
	 *
	 * @param  type  The new type value
	 */
	public void setType(String type) {
		this.type = type;
	}


	/**
	 *  Sets the identifier attribute of the ServiceDescription object
	 *
	 * @param  type  The new identifier value
	 */
	public void setIdentifier(String type) {
		this.identifier = identifier;
	}


	/**
	 *  Utility to test if a string is not null and has non-whitespace conten.
	 *
	 * @param  s  string to be tested
	 * @return    true if string has content
	 */
	private boolean notEmpty(String s) {
		return (s != null && s.trim().length() > 0);
	}


	/**
	 *  Returns serviceDescription as an dom4j.Element.
	 *
	 * @return    NOT YET DOCUMENTED
	 */
	public Element asElement() {
		Element root = getRootElement();
		// doc is necessary for setNoteText calls
		this.doc = DocumentHelper.createDocument(root);
		if (notEmpty(title))
			setNodeText("/serviceDescription/dc:title", title);
		if (notEmpty(description))
			setNodeText("/serviceDescription/dc:description", description);
		if (notEmpty(type))
			setNodeText("/serviceDescription/dc:type", type);
		if (notEmpty(identifier))
			setNodeText("/serviceDescription/dc:identifier", identifier);
		if (image != null)
			root.add(image.asElement());
		if (!contacts.isEmpty()) {
			Element e = root.addElement("contacts");
			for (Iterator i = contacts.iterator(); i.hasNext(); ) {
				e.add(((Contact) i.next()).asElement());
			}
		}
		return root;
	}


	/**
	 *  Creates the root element for the serviceDescription in required namespaces.
	 *
	 * @return    The rootElement value
	 */
	private Element getRootElement() {
		Element root = DocumentHelper.createElement("serviceDescription");
		/* 		root.add(DocumentHelper.createNamespace("dct", "http://purl.org/dc/terms/"));
		root.add(DocumentHelper.createNamespace("ieee", "http://purl.org/ieee/phony_namespace")); */
		root.add(DocumentHelper.createNamespace("dc", "http://purl.org/dc/elements/1.1/"));

		return root;
	}

	/**
	 *  The main program for the ServiceDescription class
	 *
	 * @param  args           The command line arguments
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public static void main(String[] args) throws Exception {
		File propFile = null; // propFile must be assigned!
		NdrUtils.setup (propFile);
		
		NDRConstants.setPrivateKeyFile(new java.io.File ("/Users/ostwald/devel/misc/keys/ncsPrivateKey"));
		String recId = "NSDL-COLLECTION-920106";
		NCSCollectReader reader = 
			org.dlese.dpc.schemedit.ndr.util.integration.NSDLCollectionUtils.getNCSRecord(recId);
		
		ServiceDescription sd = ServiceDescription.makeServiceDescription(reader, NDRConstants.NDRObjectType.AGGREGATOR);

		pp (sd.asElement());
	}


	/**
	 *  Creates a service description instance based on information provided as NCSCollectReader.
	 *
	 * @param  ncsDocReader  reader for a ncs_collect record.
	 * @return               serviceDescription instance
	 */
	public static ServiceDescription makeServiceDescription(NCSCollectReader ncsDocReader, 
															NDRConstants.NDRObjectType ndrObjType) {
		ServiceDescription sd = new ServiceDescription();
		String type = ndrObjType.getNdrResponseType();
		sd.setTitle(type + " for " + ncsDocReader.getTitle());
		// sd.setDescription(ncsDocReader.getDescription());
		if (ndrObjType == NDRConstants.NDRObjectType.METADATAPROVIDER)
			sd.setDescription("Provides " + ncsDocReader.getTitle() + " records");
		
		else if (ndrObjType == NDRConstants.NDRObjectType.AGGREGATOR)
			sd.setDescription("Collection of " + ncsDocReader.getTitle() + " items");
		
		sd.setType(type);
		if (ncsDocReader.getBrandURL() != null && ncsDocReader.getBrandURL().trim().length() > 0) {
			sd.setImage(ncsDocReader.getBrandURL(), ncsDocReader.getTitle(), 
						ncsDocReader.getImageWidth(), ncsDocReader.getImageHeight(), 
						ncsDocReader.getAltText());
		}
		sd.setIdentifier(ncsDocReader.getOaiBaseUrl());
		for (Iterator i=ncsDocReader.getContacts().iterator();i.hasNext();) {
			Contact contact = (Contact)i.next();
			// prtln (Dom4jUtils.prettyPrint(contact.asElement()));
			sd.addContact (contact);
		}
		return sd;
	}
	
	/**
	 *  Creates a service description instance based on information provided as DleseCollectionDocReader.
	 *
	 * @param  reader  reader representing a dlese_collect record
	 * @return         serviceDescription instance
	 */
	public static ServiceDescription makeServiceDescription(DleseCollectionDocReader reader, 
															NDRConstants.NDRObjectType ndrObjType) {
		ServiceDescription sd = new ServiceDescription();
		String type = ndrObjType.getNdrResponseType();
		sd.setTitle(type + " for " + reader.getShortTitle());
		sd.setDescription(reader.getDescription());
		sd.setType(type);
		sd.setIdentifier(reader.getCollectionUrl());
		return sd;
	}

	/**
	 *  Gets the fakeServiceDescription attribute of the ServiceDescription class
	 *
	 * @return    The fakeServiceDescription value
	 */
	public static ServiceDescription getFakeServiceDescription() {
		ServiceDescription sd = new ServiceDescription("flim", "flam", "floo");
		sd.setImage("imgbrandURL", "imgtitle", "imgwidth", "imgheight", "alttext");
		sd.addContact("ostwald", "ostwald@ucar.edu", "i'm nuts");
		sd.addContact("joanthan", "ostwald@comcast.edu", "sue me");
		return sd;
	}


	/**
	 *  Gets the node attribute of the ServiceDescription object
	 *
	 * @param  xpath  NOT YET DOCUMENTED
	 * @return        The node value
	 */
	private Node getNode(String xpath) {
		return this.doc.selectSingleNode(xpath);
	}


	/**
	 *  Sets the text of the node specified by xpath.
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

	private static String getChildText (Element parent, String tag) {
		try {
			return parent.element(tag).getTextTrim();
		} catch (Exception e) {}
		return "";
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


	/**
	 *  Class representing an Image attribute of the serviceDescription.
	 *
	 * @author     Jonathan Ostwald
	 * @version    $Id: ServiceDescription.java,v 1.6 2009/08/19 18:25:45 ostwald Exp $
	 */
	 public class Image {
		String brandURL = null;
		String title = null;
		String width = null;
		String height = null;
		String alttext = null;


		/**
		 *  Constructor for the Image object
		 *
		 * @param  brandURL  NOT YET DOCUMENTED
		 * @param  title     NOT YET DOCUMENTED
		 * @param  width     NOT YET DOCUMENTED
		 * @param  height    NOT YET DOCUMENTED
		 * @param  alttext   NOT YET DOCUMENTED
		 */
		public Image(String brandURL, String title, String width, String height, String alttext) {
			this.brandURL = brandURL;
			this.title = title;
			this.width = width;
			this.height = height;
			this.alttext = alttext;
		}
		
		public Image (Element element) {
			this.brandURL = getChildText (element, "brandURL");
			this.title = getChildText (element, "title");
			this.width = getChildText (element, "width");
			this.height = getChildText (element, "height");
			this.alttext = getChildText (element, "alttext");
		}

		/**
		 *  NOT YET DOCUMENTED
		 *
		 * @return    NOT YET DOCUMENTED
		 */
		public Element asElement() {
			Element image = DocumentHelper.createElement("image");
			if (brandURL != null && brandURL.trim().length() > 0)
				image.addElement("brandURL").setText(brandURL);
			if (title != null && brandURL.trim().length() > 0)
				image.addElement("title").setText(title);
			if (width != null && brandURL.trim().length() > 0)
				image.addElement("width").setText(width);
			if (height != null && brandURL.trim().length() > 0)
				image.addElement("height").setText(height);
			if (alttext != null && brandURL.trim().length() > 0)
				image.addElement("alttext").setText(alttext);
			return image;
		}
		
		public String getBrandURL () {
			return this.brandURL;
		}
		
		public String getTitle () {
			return this.title;
		}
		
		public String getWidth () {
			return this.width;
		}
		
		public String getHeight () {
			return this.height;
		}

		public String getAlttext () {
			return this.alttext;
		}		
	}

}

