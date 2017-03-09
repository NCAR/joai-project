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

/**
 *  NOT YET DOCUMENTED
 *
 * @author     Jonathan Ostwald
 * @version    $Id: AddAggregatorRequest.java,v 1.7 2009/03/20 23:33:53 jweather Exp $
 */
public class AddAggregatorRequest extends SignedNdrRequest {

	/**  Constructor for the AddAggregatorRequest object */
	public AddAggregatorRequest() {
		super("addAggregator");
		this.setObjectType(NDRObjectType.AGGREGATOR);
	}


	/**
	 *  Convenience constructor that initializes the aggregatorFor releationship with provided agentHandle.
	 *
	 * @param  agentHandle  NOT YET DOCUMENTED
	 */
	public AddAggregatorRequest(String agentHandle) {
		this();
		setAggregatorFor(agentHandle);
	}


	/**
	 *  Sets the aggregatorFor attribute of the AddAggregatorRequest object
	 *
	 * @param  agentHandle  The new aggregatorFor value
	 */
	public void setAggregatorFor(String agentHandle) {
		this.addCommand("relationship", "aggregatorFor", agentHandle);
	}


	/**
	 *  Addes "associatedWith" relationship to provided resourceHandle.<p>
	 This relationship is used to connect a Collection Aggregator with the Collection Resource.
	 *
	 * @param  resourceHandle  NOT YET DOCUMENTED
	 */
	public void associateWith(String resourceHandle) {
		this.addCommand("relationship", "associatedWith", resourceHandle);
	}

}

