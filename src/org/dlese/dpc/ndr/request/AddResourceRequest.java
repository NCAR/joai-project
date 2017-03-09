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

import org.dom4j.Element;
import org.dom4j.DocumentHelper;

/**
 *  *  Convenience class for creating NdrRequest to add a Resource
 *
 * @author     Jonathan Ostwald
 * @version    $Id: AddResourceRequest.java,v 1.6 2009/03/20 23:33:53 jweather Exp $
 */
public class AddResourceRequest extends SignedNdrRequest {

	/**  Constructor for the AddResourceRequest object */
	public AddResourceRequest() {
		super("addResource");
		this.setObjectType(NDRObjectType.RESOURCE);
	}


	/**
	 *  Sets the identifier attribute of the AddResourceRequest object
	 *
	 * @param  id  The new identifier value
	 */
	public void setIdentifier(String id) {
		setIdentifier(id, "URL");
	}


	/**
	 *  Sets the identifier attribute of the AddResourceRequest object
	 *
	 * @param  id    The new identifier value
	 * @param  type  The new identifier value
	 */
	public void setIdentifier(String id, String type) {
		Element identifier = DocumentHelper.createElement("identifier");
		identifier.setText(id);
		if (type != null)
			identifier.addAttribute("type", type);
		this.addCommand("property", identifier);
	}


	/**
	 *  Creates "memberOf" relationship to provided aggregatorHandle.
	 *
	 * @param  aggregatorHandle  The new memberOf value
	 */
	public void setMemberOf(String aggregatorHandle) {
		this.addCommand("relationship", "memberOf", aggregatorHandle);
	}

}

