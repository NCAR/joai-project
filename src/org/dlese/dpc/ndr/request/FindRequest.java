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
import org.dlese.dpc.ndr.apiproxy.NDRConstants.NDRObjectType;

import java.util.List;

/**
 *  Convenience method for constructing "find" NdrRequest.<p>
 *
 *  See <a href="http://ndr.comm.nsdl.org/cgi-bin/wiki.pl?find">NDR API
 *  documentation</>.
 *
 * @author     Jonathan Ostwald
 * @version    $Id: FindRequest.java,v 1.8 2009/03/20 23:33:53 jweather Exp $
 */
public class FindRequest extends NdrRequest {

	/**  Constructor for the FindRequest object */
	public FindRequest() {
		this.verb = "find";
	}


	/**
	 *  Constructor for the FindRequest to restrict hits to objects of specified type.
	 *
	 * @param  objectType  NOT YET DOCUMENTED
	 */
	public FindRequest(NDRObjectType objectType) {
		this();
		this.setObjectType(objectType);
	}


	/**
	 *  Gets the first handle (of potentially many) found.
	 *
	 * @return                The resultHandle value
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public String getResultHandle() throws Exception {

		InfoXML response = this.submit();

		if (response.hasErrors())
			throw new Exception(response.getError());
		else
			return response.getHandle();
	}
	
	/**
	 *  Gets the resultHandle attribute of the ListMembersRequest object
	 *
	 * @return                The resultHandle value
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public List getResultHandles() throws Exception {
		InfoXML response = this.submit();
		if (response.hasErrors())
			throw new Exception(response.getError());
		else
			return response.getHandleList();
	}

}

