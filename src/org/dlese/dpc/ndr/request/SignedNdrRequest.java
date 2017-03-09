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

import org.dlese.dpc.ndr.apiproxy.*;
import org.dlese.dpc.ndr.connection.NDRConnection;
import org.dlese.dpc.xml.Dom4jUtils;
import org.dom4j.*;
import java.util.*;

/**
 *  Class to communiate directly with NDR via {@link org.dlese.dpc.ndr.connection.NDRConnection}.
 *  Builds the inputXML parameter that is sent as part a POST request.
 *
 * @author     Jonathan Ostwald
 * @version    $Id: SignedNdrRequest.java,v 1.4 2009/03/20 23:33:53 jweather Exp $
 */
public class SignedNdrRequest extends NdrRequest {

	/**  NOT YET DOCUMENTED */
	protected InputXML inputXML = null;
	/**  NOT YET DOCUMENTED */
	protected NDRConstants.NDRObjectType objectType = null;
	private String payload = null;


	/**  Constructor for the SignedNdrRequest object */
	public SignedNdrRequest() {
		super();
	}


	/**
	 *  Constructor for the SignedNdrRequest object with specified verb.
	 *
	 * @param  verb  NOT YET DOCUMENTED
	 */
	public SignedNdrRequest(String verb) {
		this();
		this.verb = verb;
	}


	/**
	 *  Constructor for the SignedNdrRequest object with specified verb and handle.
	 *
	 * @param  verb    NOT YET DOCUMENTED
	 * @param  handle  NOT YET DOCUMENTED
	 */
	public SignedNdrRequest(String verb, String handle) {
		this(verb);
		this.handle = handle;
	}

	public void authorizeToChange (String agentHandle) {
		this.authorizeToChange(agentHandle, null);
	}

	public void authorizeToChange (String agentHandle, String action) {
		this.addQualifiedCommand(NDRConstants.AUTH_NAMESPACE, 
								 "relationship", 
								 "authorizedToChange", 
								 agentHandle, 
								 action);
	}
	
	/**
	 *  Creates connection and adds payload in the form of inputXML parameter.<p>
	 *
	 *  Payload is the request objects's inputXML attribute, which is overidden by
	 *  the inputXMLStr parameter if present. This allows a caller to create an
	 *  inputXMLStr external to the request, which is helpful in debugging.
	 *
	 * @param  path           NOT YET DOCUMENTED
	 * @param  inputXMLStr    NOT YET DOCUMENTED
	 * @return                The nDRConnection value
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	protected NDRConnection getNDRConnection(String path, String inputXMLStr) throws Exception {
		NDRConnection connection = super.getNDRConnection(path, inputXMLStr);

		connection.setKeyFile(NDRConstants.getPrivateKeyFile());
		connection.setAgentHandle(this.getRequestAgent());
		connection.setCanonicalHeader(true);
		
		return connection;
	}

	/**  NOT YET DOCUMENTED */
	public void report(String path) {
		prtln("SignedNdrRequest: submit");
		prtln("\t path: " + path);
		prtln("\t requestAgent: " + this.requestAgent);
		prtln("\t verbose: " + getVerbose());

	}

}

