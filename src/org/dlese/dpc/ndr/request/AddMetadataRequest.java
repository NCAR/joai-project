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
package org.dlese.dpc.ndr.request;

import org.dlese.dpc.ndr.apiproxy.NDRConstants.NDRObjectType;

import org.dlese.dpc.xml.Dom4jUtils;
import org.dom4j.*;

/**
 *  Convenience class for creating NdrRequests to add metadata
 *
 * @author     Jonathan Ostwald
 * @version    $Id: AddMetadataRequest.java,v 1.6 2009/03/20 23:33:53 jweather Exp $
 */
public class AddMetadataRequest extends SignedNdrRequest {

	/**  Constructor for the AddMetadataRequest object */
	public AddMetadataRequest() {
		super("addMetadata");
		this.setObjectType (NDRObjectType.METADATA);
	}


	/**
	 *  Constructor that sets "metadataFor" relationship with provided resourceHandle;
	 *
	 * @param  resourceHandle  NOT YET DOCUMENTED
	 */
	public AddMetadataRequest(String resourceHandle) {
		this();
		setMetadataFor(resourceHandle);
	}


	/**
	 *  Sets the uniqueId attribute of the AddMetadataRequest object
	 *
	 * @param  id  The new uniqueId value
	 */
	public void setUniqueId(String id) {
		this.addCommand("property", "uniqueID", id);
	}


	/**
	 *  Sets the metadataFor attribute of the AddMetadataRequest object
	 *
	 * @param  resourceHandle  The new metadataFor value
	 */
	public void setMetadataFor(String resourceHandle) {
		this.addCommand("relationship", "metadataFor", resourceHandle);
	}


	/**
	 *  Sets the dataStream attribute of the AddMetadataRequest object
	 *
	 * @param  format         The new dataStream value
	 * @param  element        The new dataStream value
	 * @exception  Exception  if inputXML is null, or if element is null
	 */
	public void setDataStream(String format, Element element) throws Exception {
		this.addDataStreamCmd(format, element);
	}


	/**
	 *  Sets the metadataProvidedBy attribute of the AddMetadataRequest object
	 *
	 * @param  mdpHandle  The new metadataProvidedBy value
	 */
	public void setMetadataProvidedBy(String mdpHandle) {
		this.addCommand("relationship", "metadataProvidedBy", mdpHandle);
	}


}

