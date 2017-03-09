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
package org.dlese.dpc.ndr.reader;

import org.dlese.dpc.ndr.apiproxy.NDRConstants;
import org.dlese.dpc.ndr.apiproxy.NDRConstants.NDRObjectType;
import org.dlese.dpc.schemedit.SchemEditUtils;
import org.dlese.dpc.xml.Dom4jUtils;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import java.net.URL;
import java.util.*;

/**
/**
 *  Extension of NdrObjectReader for accessing properties, dataStreams, and
 *  relationships of NDR MetadataProvider Objects.<p>
 *
 *  More Info:
 *  <li> MetadataProvider overview: http://wiki.nsdl.org/index.php/Community:NDR/ObjectTypes#MetadataProvider
 *
 *  <li> MetadataProvider data model: http://wiki.nsdl.org/index.php/Community:NCore/Model/Objects/MetadataProvider
 *
 *  <li> MetadataProvider API requests: http://wiki.nsdl.org/index.php/Community:NDR/APIRequestsByObject#MetadataProvider_requests
 *
 * @author    ostwald
 */
public class MetadataProviderReader extends GroupingObjectReader {

	private static boolean debug = false;
	private final static NDRObjectType MYTYPE = NDRObjectType.METADATAPROVIDER;
	private String collectionId = null;
	private String collectionName = null;
	private String nativeFormat = null;
	private HarvestInfoReader harvestInfo = null;


	public String getChildToParentRelationship() {
		return "metadataProvidedBy";
	}
	
	/**
	 *  Constructor for the MetadataProviderReader object at the specified NDR
	 *  handle.
	 *
	 * @param  handle         NOT YET DOCUMENTED
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public MetadataProviderReader(String handle) throws Exception {
		super(handle, "dlese_collect");
		if (getObjectType() != MYTYPE)
			throw new Exception("Provided handle (" + handle +
				") does not refer to a metadataProvider object (" + getObjectType() + ")");
	}


	/**
	 *  Constructor for the MetadataProviderReader object for the provided Document
	 *  representing a NDR "get" call for a MetadataProvider object.
	 *
	 * @param  ndrResponse    NOT YET DOCUMENTED
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public MetadataProviderReader(Document ndrResponse) throws Exception {
		super(ndrResponse, "dlese_collect");
		if (getObjectType() != MYTYPE)
			throw new Exception("Provided document does not refer to a metadataProvider object (" + getObjectType() + ")");
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @return    NOT YET DOCUMENTED
	 */
/* 	protected List dataStreamsToExtract() {
		if (dsFormats == null) {
			dsFormats = super.dataStreamsToExtract();
			dsFormats.add("collection_config");
		}
		return dsFormats;
	} */


	/**
	 *  Gets the collectionId attribute of the MetadataProviderReader object
	 *  (corresponding to the "setSpec" attribute of the collection's SetInfo
	 *  object).
	 *
	 * @return    The collectionId value
	 */
	public String getCollectionId() {
		return getProperty("ncs:collectionId");
	}


	/**
	 *  Gets the collectionName attribute of the MetadataProviderReader object
	 *  (corresponding to the "name" attribute of the collection's SetInfo object).
	 *
	 * @return    The collectionName value
	 */
	public String getCollectionName() {
		return getProperty("ncs:collectionName");
	}


	/**
	 *  Gets the collectionRecord (dlese_collect format) of the MetadataProviderReader object
	 *
	 * @return    The collectionRecord value
	 */
	public Element getCollectionRecord() {
		return this.getDataStream("dlese_collect");
	}

	/**
	 *  Gets the nativeFormat for the ITEM-level metadata of this collection.
	 *
	 * @return    The nativeFormat value
	 */
	public String getNativeFormat() {
		if (nativeFormat == null) {

			nativeFormat = this.getProperty("ncs:nativeFormat");
			if (nativeFormat == null || nativeFormat.trim().length() == 0)
				nativeFormat = "nsdl_dc";
		}
		return nativeFormat;
	}


	/**
	 *  Gets the aggregatedBy attribute of the MetadataProviderReader object
	 *
	 * @return    The aggregatedBy value
	 */
	public String getAggregatedBy() {
		return this.getRelationship("aggregatedBy");
	}


	/**
	 *  Gets the metadataProviderFor attribute (an agent handle) of the MetadataProviderReader object
	 *
	 * @return    The metadataProviderFor value
	 */
	public String getMetadataProviderFor() {
		return this.getRelationship("metadataProviderFor");
	}

	/**
	 *  Gets the setSpec attribute of the MetadataProviderReader object
	 *
	 * @return    The setSpec value
	 */
	public String getSetSpec() {
		return this.getProperty("setSpec");
	}


	/**
	 *  Gets the setName attribute of the MetadataProviderReader object
	 *
	 * @return    The setName value
	 */
	public String getSetName() {
		return this.getProperty("setName");
	}

		/**
	 *  Gets the serviceDescription attribute of the MetadataProviderReader object
	 *
	 * @return    The serviceDescription value
	 */
	public HarvestInfoReader getHarvestInfo() {
		if (harvestInfo == null) {
			String xpath = "/ndr:NSDLDataRepository/ndr:NDRObject/ndr:data/ndr:harvestInfo";
			String uriStr = getNodeText(xpath);
			try {
				URL url = new URL(uriStr);
/* 				Document doc = NdrUtils.getNDRObjectDoc(url);
				harvestInfo = doc.getRootElement().asXML(); */
				harvestInfo = new HarvestInfoReader (url);
			} catch (Exception e) {
				prtln("getHarvestInfo error: " + e.getMessage());
				harvestInfo = null;
			}
		}
		return harvestInfo;
	}

	/**
	 *  Gets the metadata object handles for the MetadataProviderReader object
	 *
	 * @return                The itemHandles value
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
 	public List getItemHandles() throws Exception {
		return getMemberHandles();
	}

	
	
	/**
	 *  Gets the handles of the inactive metadata objects of the MetadataProviderReader object
	 *
	 * @return                The inactiveItemHandles value
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public List getInactiveItemHandles() throws Exception {
/* 		if (inactiveItemHandles == null) {
			FindRequest request = new FindRequest();
			request.setObjectType(NDRConstants.NDRObjectType.METADATA);
			request.addCommand("relationship", "metadataProvidedBy", this.getHandle());
			inactiveItemHandles = request.getResultHandles();
		}
		return inactiveItemHandles; */
		return getInactiveMemberHandles();
	}


	/**
	 *  Description of the Method
	 *
	 * @param  s  Description of the Parameter
	 */
	private static void prtln(String s) {
		if (debug) {
			// SchemEditUtils.prtln(s, "MetadataProviderReader");
			SchemEditUtils.prtln(s, "");
		}
	}

}

