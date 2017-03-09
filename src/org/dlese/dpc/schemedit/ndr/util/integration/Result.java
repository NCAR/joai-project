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

import org.dlese.dpc.xml.Dom4jUtils;

import org.dom4j.*;
import java.util.*;

class Result {
	public String resourceHandle;
	public String resourceUrl;
	
	String nonNullValue (String s) {
		return (s == null ? "" : s);
	}
	
	Result (String resourceUrl, String resourceHandle) {
		this.resourceHandle = nonNullValue(resourceHandle);
		this.resourceUrl = nonNullValue(resourceUrl);
	}
	
	Result (Element e) {
		this.resourceHandle = CollectionIntegrator.getElementText(e, "resourcehandle");
		this.resourceUrl = CollectionIntegrator.getElementText(e, "resourceurl");
	}
	
	public String toString() {
		return "ncsUrl: " + resourceUrl +   "  handle: " + resourceHandle;
	}
	
	public Element asElement (String tag) {
		Element e = DocumentHelper.createElement (tag);
		Element url = e.addElement ("resourceurl");
		url.setText(resourceUrl);
		Element handle = e.addElement ("resourcehandle");
		handle.setText(resourceHandle);
		return e;
	}
}

