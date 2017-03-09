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

public class FindResourceRequest extends SimpleNdrRequest {
	
	private String resourceUrl = null;;
	
	public FindResourceRequest () {
		this.verb = "findResource";
	}
	
	public FindResourceRequest (String resourceUrl) {
		this();
		this.resourceUrl = resourceUrl;
	}
	
	public String getResultHandle() throws Exception {

		InfoXML response = this.submit();

		if (response.hasErrors())
			throw new Exception(response.getError());
		else
			return response.getHandle();
	}
	
	protected String makePath () throws Exception {
		if (resourceUrl == null && handle == null)
			throw new Exception ("findResource request requires either \"resourceUrl\" " +
								 "or \"handle\"");
		
		String path = NDRConstants.getNdrApiBaseUrl() + "/findResource?";
		if (resourceUrl != null)
			path += "url="+java.net.URLEncoder.encode (resourceUrl);
		else if (handle != null)
			path += "handle="+handle;
		return path;
	}
}

