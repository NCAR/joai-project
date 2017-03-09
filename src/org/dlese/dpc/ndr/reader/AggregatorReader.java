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
import org.dlese.dpc.ndr.request.FindRequest;
import org.dlese.dpc.ndr.request.CountMembersRequest;
import org.dlese.dpc.schemedit.SchemEditUtils;
import org.dlese.dpc.xml.Dom4jUtils;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import java.util.*;

/**
 *  Extension of NdrObjectReader for accessing properties, dataStreams, and
 *  relationships of NDR Aggregator Objects.<p>
 *
 *  More Info:
 *  <li> Aggregator overview: http://wiki.nsdl.org/index.php/Community:NDR/ObjectTypes#Aggregator
 *
 *  <li> Aggregator data model: http://wiki.nsdl.org/index.php/Community:NCore/Model/Objects/Aggregator
 *
 *  <li> Aggregator API requests: http://wiki.nsdl.org/index.php/Community:NDR/APIRequestsByObject#Aggregator_requests
 *
 * @author    ostwald
 */
public class AggregatorReader extends GroupingObjectReader {

	private static boolean debug = false;
	private final static NDRObjectType MYTYPE = NDRObjectType.AGGREGATOR;
	private String mdpHandle = null;


	/**
	 *  Gets the childToParentRelationship attribute of the AggregatorReader object
	 *
	 * @return    The childToParentRelationship value
	 */
	public String getChildToParentRelationship() {
		return "memberOf";
	}


	/**
	 *  Constructor for the AggregatorReader object at the specified NDR handle.
	 *
	 * @param  handle         NOT YET DOCUMENTED
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public AggregatorReader(String handle) throws Exception {
		super(handle);
		if (getObjectType() != MYTYPE)
			throw new Exception("Provided handle (" + handle +
				") does not refer to a aggregator object (" + getObjectType() + ")");
	}


	/**
	 *  Constructor for the AggregatorReader object for the provided Document
	 *  representing a NDR "get" call for a Aggregator object.
	 *
	 * @param  ndrResponse    NOT YET DOCUMENTED
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public AggregatorReader(Document ndrResponse) throws Exception {
		super(ndrResponse);
		if (getObjectType() != MYTYPE)
			throw new Exception("Provided document does not refer to a metadataProvider object (" + getObjectType() + ")");
	}


	/**
	 *  Gets the mdpHandle attribute of the AggregatorReader object
	 *
	 * @return    The mdpHandle value
	 */
	public String getMdpHandle() {
		if (mdpHandle == null) {
			try {
				FindRequest request = new FindRequest(NDRConstants.NDRObjectType.METADATAPROVIDER);
				request.addCommand("relationship", "aggregatedBy", this.getHandle());
				return request.getResultHandle();
			} catch (Throwable t) {
				prtln("could not obtain mdpHandle: " + t);
			}
			mdpHandle = "";
		}
		return mdpHandle;
	}


	/**
	 *  Gets the nativeFormat for the ITEM-level metadata of this collection.
	 *
	 * @return    The nativeFormat value
	 */
	public String getNativeFormat() {
		return "nsdl_dc";
	}


	/**
	 *  Gets the aggregatorFor attribute of the AggregatorReader object
	 *
	 * @return    The aggregatorFor value
	 */
	public String getAggregatorFor() {
		return this.getRelationship("aggregatorFor");
	}


	/**
	 *  Gets the collectionResource attribute of the AggregatorReader object
	 *
	 * @return    The collectionResource value
	 */
	public String getCollectionResource() {
		return this.getRelationship("associatedWith");
	}


	/**
	 *  Description of the Method
	 *
	 * @param  s  Description of the Parameter
	 */
	private static void prtln(String s) {
		if (debug) {
			// SchemEditUtils.prtln(s, "AggregatorReader");
			SchemEditUtils.prtln(s, "");
		}
	}

}

