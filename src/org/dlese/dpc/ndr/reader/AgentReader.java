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
package org.dlese.dpc.ndr.reader;

import org.dlese.dpc.ndr.NdrUtils;
import org.dlese.dpc.ndr.apiproxy.NDRConstants;
import org.dlese.dpc.schemedit.SchemEditUtils;
import org.dlese.dpc.xml.Dom4jUtils;
import org.dlese.dpc.util.Files;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import java.util.*;

/**
 *  Extension of NdrObjectReader for accessing properties, dataStreams, and
 *  relationships of NDR Agent Objects.<p>
 *
 *  More Info:
 *  <li> Agent overview: http://wiki.nsdl.org/index.php/Community:NDR/ObjectTypes#Agent
 *
 *  <li> Agent data model: http://wiki.nsdl.org/index.php/Community:NCore/Model/Objects/Agent
 *
 *  <li> Agent API requests: http://wiki.nsdl.org/index.php/Community:NDR/APIRequestsByObject#Agent_requests
 *
 * @author    ostwald
 */
public class AgentReader extends NdrObjectReader {

	private static boolean debug = true;
	private String pid = null;
	private final static NDRConstants.NDRObjectType MYTYPE = NDRConstants.NDRObjectType.AGENT;

	private String identifier = null;
	private String identifierType = null;


	/**
	 *  Constructor for the AgentReader with provided handle
	 *
	 * @param  handle         handle to Agent object in the NDR
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public AgentReader(String handle) throws Exception {
		super(handle);
		agentInit();
	}


	/**
	 *  Constructor for the AgentReader object with provided response (to ndr "get"
	 *  call)
	 *
	 * @param  response       a dom4j.Document reperesentation of ndr GET response.
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public AgentReader(Document response) throws Exception {
		super(response);
		agentInit();
	}


	/**
	 *  Gets the identifier attribute of the AgentReader object
	 *
	 * @return    The identifier value
	 */
	public String getIdentifier() {
		return this.identifier;
	}


	/**
	 *  Returns one of "HOST", "URL", "OTHER", depending on what form of id is
	 *  present in the object.
	 *
	 * @return    The identifierType value
	 */
	public String getIdentifierType() {
		return this.identifierType;
	}


	protected void agentInit() throws Exception {
		if (getObjectType() != MYTYPE)
			throw new Exception("Provided handle (" + handle +
				") does not refer to an Agent object (" + getObjectType() + ")");

		String[] idForms = {"hasResourceHOST", "hasResourceURL", "hasResourceOTHER"};
		for (int i = 0; i < idForms.length; i++) {
			String idForm = idForms[i];
			String id = this.getProperty(idForm);
			if (id != null) {
				this.identifier = id;
				this.identifierType = idForm.substring("hasResource".length());
				break;
			}
		}
	}


	/**
	 *  Returns true if the configured NCS_AGENT is authorized to change this agent.
	 *
	 * @return    The authorizedToChange value
	 */
	public boolean isAuthorizedToChange() {
		List agents = this.getPropertyValues("authorizedToChange");
		if (agents != null)
			return (agents.contains(NDRConstants.getNcsAgent()));
		else
			return false;
	}


	/**
	 *  Gets the metadataProviders that are related to this Agent via the
	 *  metadataProviderFor relationship.
	 *
	 * @return                The metadataProviders value
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public List getMetadataProviders() throws Exception {
		return NdrUtils.getMDPHandles(this.handle);
	}


	/**
	 *  Gets the aggregators that are related to this Agent via the aggregatorFor
	 *  relationship.
	 *
	 * @return                The aggregators value
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public List getAggregators() throws Exception {
		return NdrUtils.getAggregatorHandles(this.handle);
	}


	protected static void pp(Node node) {
		prtln(Dom4jUtils.prettyPrint(node));
	}


	/**
	 *  Description of the Method
	 *
	 * @param  s  Description of the Parameter
	 */
	private static void prtln(String s) {
		if (debug) {
			SchemEditUtils.prtln(s, "AgentReader");
			// SchemEditUtils.prtln(s, "");
		}
	}

}

