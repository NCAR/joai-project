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

import org.dlese.dpc.schemedit.ndr.util.DcStream;
import org.dlese.dpc.ndr.apiproxy.NDRConstants;
import org.dom4j.Element;

public class ModifyAgentRequest extends SignedNdrRequest {
	
	public ModifyAgentRequest() {
		super ("modifyAgent");
		this.setObjectType (NDRConstants.NDRObjectType.AGENT);
	}	

	public ModifyAgentRequest (String agentHandle) {
		this();
		this.handle = agentHandle;
	}

	public void addDcsStreamCmd (String title, String description, String subject) throws Exception {

		DcStream dc_stream = new DcStream(title, description, subject);
		
		this.addDCStreamCmd(dc_stream.asElement());
	}
	
}
