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
package org.dlese.dpc.schemedit.ndr.util.integration;

import org.dlese.dpc.schemedit.ndr.util.*;
import org.dlese.dpc.ndr.apiproxy.*;
import org.dlese.dpc.ndr.NdrUtils;
import org.dlese.dpc.ndr.reader.*;
import org.dlese.dpc.ndr.request.*;
import org.dlese.dpc.xml.Dom4jUtils;
import org.dlese.dpc.util.Files;
import org.dlese.dpc.util.strings.FindAndReplace;
import org.dlese.dpc.index.SimpleLuceneIndex;
import org.dom4j.*;
import java.util.*;
import java.io.File;
import java.net.*;

/**
 *  Reads spreadsheet data (xml file created from spreadsheet) with data
 *  supplied by NSDL but augmented from NCS Collect records, with the purpose of
 *  determining overlaps and gaps between the collection management info in both
 *  models.
 *
 * @author    Jonathan Ostwald
 */
public class Collection {
	private static boolean debug = true;
	
	public NSDLCollectionReader nsdlColl = null;
	public NCSCollectReader ncsRec = null;
	public InfoStream infoStream = null;
	public ServiceDescription serviceDescription = null;
	
	
	/**
	 *  Constructor for the Collection object
	 *
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public Collection(String ncsRecId, String aggHandle) throws Exception {
		this (ncsRecId, aggHandle, null);
	}
		
	public Collection(String ncsRecId, String aggHandle, String webServiceBaseUrl) throws Exception {
		try {
			NSDLCollectionUtils.setBaseServiceUrl(webServiceBaseUrl);
			ncsRec = NSDLCollectionUtils.getNCSRecord(ncsRecId);
		} catch (Exception e) {
			throw new Exception ("could not get NCSRecord: " + e.getMessage());
		}
		try {
			nsdlColl = new NSDLCollectionReader (aggHandle);
		} catch (Exception e) {
			throw new Exception ("could not get NSDL Collection Reader: " + e.getMessage());
		}		
	}
	
	public String getCollectionMetadataHandle () {
		return this.nsdlColl.getResourceUrl();
	}
	
	public InfoStream getInfoStream () {
		if (infoStream == null)
			infoStream = new InfoStream (this.nsdlColl.metadata.getDataStream("nsdl_dc_info"));
		return infoStream;
	}
	
	public ServiceDescription getServiceDescription () {
		if (serviceDescription == null) {
			ServiceDescriptionReader reader = this.nsdlColl.aggregator.getServiceDescription();
			if (reader != null) {
				Element sdElement = reader.getDocument().getRootElement().createCopy();
				serviceDescription = new ServiceDescription (sdElement);
			}
		}
		return serviceDescription;
	}
	
	ServiceDescription getTestServiceDescription () {
		NCSCollectReader reader = integrateServiceDescription ();
		return ServiceDescription.makeServiceDescription(reader, NDRConstants.NDRObjectType.AGGREGATOR);
	}
	
	NCSCollectReader integrateServiceDescription () {
		ServiceDescription sd = getServiceDescription ();
		Element root = ncsRec.doc.getRootElement().createCopy();
		NCSCollectReader newReader = new NCSCollectReader (DocumentHelper.createDocument(root));

		newReader.setBrandURL(sd.getImage().getBrandURL());

		newReader.setImageHeight(sd.getImage().getHeight());
		newReader.setImageWidth(sd.getImage().getWidth());
		
		newReader.setContacts (sd.getContacts());
		
		return newReader;
	}
	
	public static void tester () {
		try {
			// NdrUtils.ndrTestSetup();
			NdrUtils.setup("http://ndr.nsdl.org/api");
			prtln ("\nCollection ...\n");
	
			// Collection integrator = new Collection();
			// integrator.compareCollections();
	
			String aggHandle = "2200/20061002124859565T";
			String ncsRecId = "NSDL-COLLECTION-4743";
			Collection collection = new Collection (ncsRecId, aggHandle);
			// collection.nsdlColl.report();
			
			prtln ("metadataHandle: " + collection.getCollectionMetadataHandle());
			
			prtln ("\ninfo stream");
			pp (collection.getInfoStream().asElement());
			
			prtln ("\nserviceDescription");
			pp (collection.getServiceDescription().asElement());
			
			prtln ("\nTEST serviceDescription");
			pp (collection.getTestServiceDescription().asElement());
			
	/* 		NCSCollectReader modified = collection.integrateServiceDescription();
			pp  (modified.getWritableDocument());
			List contacts = modified.getContacts();
			prtln ("there are " + contacts.size() + " contacts");
			for (Iterator i=contacts.iterator();i.hasNext();) {
				Contact c = (Contact)i.next();
				pp (c.asElement());
			} */
		} catch (Throwable t) {
			prtln ("tester ERROR: " + t.getMessage());
		}
	}
			
	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  args           NOT YET DOCUMENTED
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public static void main(String[] args) throws Exception {
		File propFile = null; // propFile must be assigned!
		NdrUtils.setup (propFile);
		
		// tester();
		setInfoStream();
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
	 *  NOT YET DOCUMENTED
	 *
	 * @param  s  NOT YET DOCUMENTED
	 */
	private static void prtln(String s) {
		String prefix = null;
		if (debug) {
			NdrUtils.prtln(s, prefix);
		}
	}

	public static String getElementText(Element e, String tag) {
		String text = "";
		try {
			text = e.element(tag).getTextTrim();
		} catch (Throwable t) {}
		return text;
	}
	
	/**
	* Supply values for these params to communicate with specific ndr server as specified agant
	*/
	static void ndrSetup () {
		String ndrApiBaseUrl = "";
		String ncsAgent = "";
		String keyFile = "";

		NdrUtils.setup(ndrApiBaseUrl, ncsAgent, keyFile);
	}
	
	/**
	* we want to put an arbitray info stream into the Metadata Object for this collection
	*/
	static void setInfoStream () throws Exception {
		prtln ("\nCollection ...\n");

		// Collection integrator = new Collection();
		// integrator.compareCollections();

		String aggHandle = "2200/test.20080227142005210T";
		String ncsRecId = "NCS7-COLL-000-000-000-002";
		String webServiceBaseUrl = "http://localhost/schemedit/services/";
		Collection collection = new Collection (ncsRecId, aggHandle, webServiceBaseUrl);
		
		
		String path = "C:/tmp/info_stream.xml";
		Document doc = Dom4jUtils.getXmlDocument(new File (path));
		InfoStream myInfoStream = new InfoStream (doc.getRootElement().createCopy());
		myInfoStream.setLink ("yaba");
		
		MetadataReader mdReader = collection.nsdlColl.metadata;
		ModifyMetadataRequest request = new ModifyMetadataRequest (mdReader.getHandle());
		
		NdrRequest.setDebug(true);
		NdrRequest.setVerbose(true);
		
		Element nsdl_dc = mdReader.getDataStream("nsdl_dc");
		pp (nsdl_dc);
		request.setDataStream("nsdl_dc", nsdl_dc);
		request.setDataInfoStream("nsdl_dc", myInfoStream.asElement());
		request.submit();
		
		pp (myInfoStream.asElement());
	}
		
}

