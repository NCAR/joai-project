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
import org.dlese.dpc.ndr.apiproxy.NDRConstants;

/**
*  Convenience class for creating NdrRequests to delete an NDR Object.
 *
 * @author     Jonathan Ostwald
 * @version    $Id: DeleteRequest.java,v 1.7 2009/03/20 23:33:53 jweather Exp $
 */
public class DeleteRequest extends SignedNdrRequest {

	private boolean cascade = false;


	/**  Constructor for the DeleteRequest object */
	public DeleteRequest() {
		super("delete");
	}


	/**
	 *  Constructor for the DeleteRequest object with provided handle to ndrObject.
	 *
	 * @param  handle  NOT YET DOCUMENTED
	 */
	public DeleteRequest(String handle) {
		this();
		this.handle = handle;
	}


	/**
	 *  Constructor for the DeleteRequest object
	 *
	 * @param  handle   NOT YET DOCUMENTED
	 * @param  cascade  NOT YET DOCUMENTED
	 */
	public DeleteRequest(String handle, boolean cascade) {
		this(handle);
		this.cascade = cascade;
	}

	protected String makePath () throws Exception {
		String path = super.makePath();
		if (cascade)
			path = path + "?cascade=true";
		return path;
	}

}

