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
public class IntegrationUtils {
	private static boolean debug = true;
	
	public Collection collection = null;

	
	/**
	 *  Constructor for the Collection object
	 *
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public IntegrationUtils(String ncsRecId, String aggHandle) throws Exception {
		this (ncsRecId, aggHandle, null);
	}
		
	public IntegrationUtils(String ncsRecId, String aggHandle, String webServiceBaseUrl) throws Exception {
		try {
			collection = new Collection (ncsRecId, aggHandle, webServiceBaseUrl);
		} catch (Exception e) {
			throw new Exception ("could not get collection: " + e.getMessage());
		}	
	}

	ServiceDescription getTestServiceDescription () {
		NCSCollectReader reader = integrateServiceDescription ();
		return ServiceDescription.makeServiceDescription(reader, NDRConstants.NDRObjectType.AGGREGATOR);
	}
	
	NCSCollectReader integrateServiceDescription () {
		ServiceDescription sd = this.collection.getServiceDescription ();
		Element root = this.collection.ncsRec.doc.getRootElement().createCopy();
		NCSCollectReader newReader = new NCSCollectReader (DocumentHelper.createDocument(root));

		newReader.setBrandURL(sd.getImage().getBrandURL());

		newReader.setImageHeight(sd.getImage().getHeight());
		newReader.setImageWidth(sd.getImage().getWidth());
		
		newReader.setContacts (sd.getContacts());
		
		return newReader;
	}
	
	static Collection getCollection () throws Exception {
		File propFile = null; // propFile must be assigned!
		NdrUtils.setup (propFile);
		
		prtln ("\nCollection ...\n");

		String aggHandle = "2200/test.20080227142005210T";
		String ncsRecId = "NCS7-COLL-000-000-000-002";
		String webServiceBaseUrl = "http://localhost/schemedit/services/";
		IntegrationUtils iUtils = new IntegrationUtils (ncsRecId, aggHandle, webServiceBaseUrl);
		return iUtils.collection;
	}
	
	/**
	* we want to put an arbitray info stream into the Metadata Object for this collection for testing purposes.
	*/
	static void setInfoStream () throws Exception {

		Collection collection = getCollection();
		
		String path = "C:/tmp/info_stream.xml";
		Document doc = Dom4jUtils.getXmlDocument(new File (path));
		InfoStream myInfoStream = new InfoStream (doc.getRootElement().createCopy());
		myInfoStream.setLink ("yaba");
		
		MetadataReader mdReader = collection.nsdlColl.metadata;
		ModifyMetadataRequest request = new ModifyMetadataRequest (mdReader.getHandle());
		
		Element nsdl_dc = mdReader.getDataStream("nsdl_dc");
		pp (nsdl_dc);
		request.setDataStream("nsdl_dc", nsdl_dc);
		request.setDataInfoStream("nsdl_dc", myInfoStream.asElement());
		request.submit();
		
		pp (myInfoStream.asElement());
	}
	
	static void getInfoStream () throws Exception {

		Collection collection = getCollection();
		MetadataReader mdReader = collection.nsdlColl.metadata;
		Element info_stream_element = mdReader.getDataStream("nsdl_dc_info");
		pp (info_stream_element);
		InfoStream infoStream = new InfoStream (info_stream_element);
		pp (infoStream.asElement());
	}
	
	/**
	* we want to put an arbitray info stream into the Metadata Object for this collection
	*/
	static void setServiceDescriptions () throws Exception {

		Collection collection = getCollection();
		
		String path = "C:/tmp/serviceDescription.xml";
		Document doc = Dom4jUtils.getXmlDocument(new File (path));
		ServiceDescription mySD = mySD = new ServiceDescription (doc.getRootElement().createCopy());
		mySD.setImage("http://foo.brand.com/image.jpg", "my bogus title", "100", "25", "my bogus alttext");
		
		String aggHandle = collection.nsdlColl.aggregator.getHandle();
		ModifyAggregatorRequest aggRequest = new ModifyAggregatorRequest (aggHandle);
		mySD.setType (NDRConstants.NDRObjectType.AGGREGATOR.toString());
		aggRequest.addServiceDescriptionCmd(mySD.asElement());
		aggRequest.submit();
		
		String mdpHandle = collection.nsdlColl.mdp.getHandle();
		ModifyMetadataProviderRequest mdpRequest = new ModifyMetadataProviderRequest (mdpHandle);
		mySD.setType (NDRConstants.NDRObjectType.METADATAPROVIDER.toString());
		mdpRequest.addServiceDescriptionCmd(mySD.asElement());
		mdpRequest.submit();
		
/* 		MetadataReader mdReader = collection.nsdlColl.metadata;
		ModifyMetadataRequest request = new ModifyMetadataRequest (mdReader.getHandle());
		
		Element nsdl_dc = mdReader.getDataStream("nsdl_dc");
		pp (nsdl_dc);
		request.setDataStream("nsdl_dc", nsdl_dc);
		request.setDataInfoStream("nsdl_dc", myInfoStream.asElement());
		request.submit(); */
		
		pp (mySD.asElement());
	}	
			
	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  args           NOT YET DOCUMENTED
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public static void main(String[] args) throws Exception {

		// setInfoStream();
		setServiceDescriptions();
		// getInfoStream();
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
	
		
}

