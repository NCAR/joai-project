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

import org.dlese.dpc.ndr.apiproxy.InfoXML;

import java.util.*; 

/**
 *  NOT YET DOCUMENTED
 *
 * @author     Jonathan Ostwald
 * @version    $Id: CountMembersRequest.java,v 1.2 2009/03/20 23:33:53 jweather Exp $
 */
public class CountMembersRequest extends NdrRequest {

	/**  Constructor for the CountMembersRequest object */
	public CountMembersRequest() {
		this.verb = "countMembers";
	}


	/**
	 *  Constructor for the CountMembersRequest object given a handle for a
	 Aggregator or MetadataProvider object.
	 *
	 * @param  objectHandle  handle for Aggregator or MetadataProvider
	 */
	public CountMembersRequest(String objectHandle) {
		this();
		this.setHandle(objectHandle);
	}

	/**
	 *  Gets the resultHandle attribute of the CountMembersRequest object
	 *
	 * @return                The resultHandle value
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public int getCount() throws Exception {
		InfoXML response = this.submit();
		if (response.hasErrors())
			throw new Exception(response.getError());
		else
			return response.getCount();
	}

}


