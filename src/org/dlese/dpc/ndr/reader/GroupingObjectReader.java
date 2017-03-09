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

import org.dlese.dpc.ndr.apiproxy.NDRConstants;
import org.dlese.dpc.ndr.request.ListMembersRequest;
import org.dlese.dpc.ndr.request.CountMembersRequest;
import org.dlese.dpc.ndr.request.FindRequest;
import org.dlese.dpc.schemedit.SchemEditUtils;
import org.dlese.dpc.xml.Dom4jUtils;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import java.net.URL;
import java.util.*;

/**
 *  Base class for MetadataProvider and Aggregator Readers with support for
 *  listing members, authorization, and service descriptions.
 *
 * @author    ostwald
 */
public abstract class GroupingObjectReader extends NdrObjectReader {

	private static boolean debug = false;
	private ServiceDescriptionReader serviceDescription = null;
	private List memberHandles = null;
	private List inactiveMemberHandles = null;
	private int inactiveMemberCount = -1;
	private int memberCount = -1;


	/**
	 *  Gets the childToParentRelationship attribute of the GroupingObjectReader
	 *  object
	 *
	 * @return    The childToParentRelationship value
	 */
	public abstract String getChildToParentRelationship();


	/**
	 *  Constructor for the GroupingObjectReader object at the specified NDR
	 *  handle.
	 *
	 * @param  handle         NOT YET DOCUMENTED
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public GroupingObjectReader(String handle) throws Exception {
		super(handle);
	}


	/**
	 *  Constructor for the GroupingObjectReader object
	 *
	 * @param  handle         NOT YET DOCUMENTED
	 * @param  format         NOT YET DOCUMENTED
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public GroupingObjectReader(String handle, String format) throws Exception {
		super(handle, format);
	}


	/**
	 *  Constructor for the GroupingObjectReader object for the provided Document
	 *  representing a NDR "get" call for a MetadataProvider object.
	 *
	 * @param  ndrResponse    NOT YET DOCUMENTED
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public GroupingObjectReader(Document ndrResponse) throws Exception {
		super(ndrResponse);
	}


	/**
	 *  Constructor for the GroupingObjectReader object
	 *
	 * @param  ndrResponse    NOT YET DOCUMENTED
	 * @param  format         NOT YET DOCUMENTED
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public GroupingObjectReader(Document ndrResponse, String format) throws Exception {
		super(ndrResponse, format);
	}


	/**
	 *  Gets the serviceDescription attribute of the GroupingObjectReader object
	 *
	 * @return    The serviceDescription value
	 */
	public ServiceDescriptionReader getServiceDescription() {
		if (serviceDescription == null) {
			String xpath = "/ndr:NSDLDataRepository/ndr:NDRObject/ndr:data/ndr:serviceDescription";
			String uriStr = getNodeText(xpath);
			try {
				URL url = new URL(uriStr);
				serviceDescription = new ServiceDescriptionReader(url);
			} catch (Exception e) {
				prtln("serviceDescription error: " + e.getMessage());
				serviceDescription = null;
			}
		}
		return serviceDescription;
	}


	/**
	 *  Gets the metadata object handles for the GroupingObjectReader object
	 *
	 * @return                The memberHandles value
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public List getMemberHandles() throws Exception {
		if (memberHandles == null) {
			memberHandles = new ListMembersRequest(this.handle).getResultHandles();
		}
		return memberHandles;
	}


	/**
	 *  Gets the number of visible metadata records of the GroupingObjectReader
	 *  object
	 *
	 * @return                The number of members for this MDP
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public int getMemberCount() throws Exception {
		if (memberCount == -1) {
			memberCount = new CountMembersRequest(this.handle).getCount();
		}
		return memberCount;
	}


	/**
	 *  Gets the handles of the inactive metadata objects of the GroupingObjectReader
	 *  object
	 *
	 * @return                The inactiveMemberHandles value
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public List getInactiveMemberHandles() throws Exception {
		if (inactiveMemberHandles == null) {
			FindRequest request = new FindRequest();
			request.setObjectType(NDRConstants.NDRObjectType.METADATA);
			request.addCommand("relationship", getChildToParentRelationship(), this.getHandle());
			request.addStateCmd(NDRConstants.ObjectState.INACTIVE);
			inactiveMemberHandles = request.getResultHandles();
		}
		return inactiveMemberHandles;
	}


	/**
	 *  Gets the number of inactive Members of the GroupingObjectReader object
	 *
	 * @return                The inactiveMemberCount value
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public int getInactiveMemberCount() throws Exception {
		if (inactiveMemberCount == -1) {
			inactiveMemberCount = getInactiveMemberHandles().size();
		}
		return inactiveMemberCount;
	}


	/**
	 *  Returns true if the configured NCS_AGENT is authorized to change this
	 *  object
	 *
	 * @return    The authorizedToChange value
	 */
	public boolean isAuthorizedToChange() {
		return isAuthorizedToChange(NDRConstants.getNcsAgent());
	}


	/**
	 *  Returns true if specified agent is authorized to change this object.
	 *
	 * @param  agentHandle  the agent handle
	 * @return              The authorizedToChange value
	 */
	public boolean isAuthorizedToChange(String agentHandle) {
		List agents = this.getRelationshipValues("auth:authorizedToChange");
		return agents.contains(agentHandle);
	}


	/**
	 *  Description of the Method
	 *
	 * @param  s  Description of the Parameter
	 */
	private static void prtln(String s) {
		if (debug) {
			// SchemEditUtils.prtln(s, "GroupingObjectReader");
			SchemEditUtils.prtln(s, "");
		}
	}

}

