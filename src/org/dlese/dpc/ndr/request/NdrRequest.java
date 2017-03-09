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
 * @version    $Id: NdrRequest.java,v 1.18 2010/05/28 19:16:43 ostwald Exp $
 */
public class NdrRequest extends SimpleNdrRequest {

	/**  NOT YET DOCUMENTED */
	protected InputXML inputXML = null;
	/**  NOT YET DOCUMENTED */
	protected NDRConstants.NDRObjectType objectType = null;
	protected String payload = null;


	/**  Constructor for the NdrRequest object */
	public NdrRequest() {
		super();
	}


	/**
	 *  Constructor for the NdrRequest object with specified verb.
	 *
	 * @param  verb  NOT YET DOCUMENTED
	 */
	public NdrRequest(String verb) {
		this();
		this.verb = verb;
	}


	/**
	 *  Constructor for the NdrRequest object with specified verb and handle.
	 *
	 * @param  verb    NOT YET DOCUMENTED
	 * @param  handle  NOT YET DOCUMENTED
	 */
	public NdrRequest(String verb, String handle) {
		this(verb);
		this.handle = handle;
	}


	/**
	 *  Gets the {@link inputXML} attribute of the NdrRequest object, which stores
	 *  the commands for this request.
	 *
	 * @return    The inputXML value
	 */
	public InputXML getInputXML() {
		return this.inputXML;
	}


	/**
	 *  A human readable representation of the XMLInput payload of the request.
	 *
	 * @return    The payload value
	 */
	protected String getPayload() {
		return payload;
	}


	/**
	 *  Sets the objectType attribute of the NdrRequest object
	 *
	 * @param  objectType  The new objectType value
	 */
	public void setObjectType(NDRConstants.NDRObjectType objectType) {
		this.objectType = objectType;
		this.inputXML = new InputXML(objectType);
	}


	/**
	 *  Gets the objectType attribute of the NdrRequest object
	 *
	 * @return    The objectType value
	 */
	public NDRConstants.NDRObjectType getObjectType() {
		return this.objectType;
	}


	/**
	 *  Gets the component of specified type ("property", "data", "relationship"
	 *  from the InfoXML instance, if it exists.
	 *
	 * @param  type           NOT YET DOCUMENTED
	 * @return                The component value
	 * @exception  Exception  If inputXML does not exist or the specified type is
	 *      unknown
	 */
	protected InputXMLComponent getComponent(String type) throws Exception {
		if (this.inputXML == null)
			throw new Exception("getComponent called while inputXML is null");
		if ("property".equals(type))
			return this.inputXML.getProperties();
		if ("relationship".equals(type))
			return this.inputXML.getRelationships();
		if ("data".equals(type))
			return this.inputXML.getData();
		throw new Exception("getComponent got unknown type: " + type);
	}


	/**
	 *  Adds a command represented as an Element of the specified type.
	 *
	 * @param  type     command type ("property", "relationship", or "data")
	 * @param  element  command represented as Element.
	 */
	public void addCommand(String type, Element element) {
		this.addCommand(type, element, null);
	}


	/**
	 *  Adds a command represented as an Element of the specified type and with the
	 *  specified action.
	 *
	 * @param  type     The feature to be added to the Command attribute
	 * @param  element  The feature to be added to the Command attribute
	 * @param  action   specifies command action ("delete", "add")
	 */
	public void addCommand(String type, Element element, String action) {
		InputXMLComponent comp;
		try {
			comp = getComponent(type);
		} catch (Exception e) {
			prtln("addCommand error: " + e);
			return;
		}
		comp.addCommand(element, action);
	}


	/**
	 *  Adds a command specified as a prop and value pair of the specified type.
	 *
	 * @param  type   The feature to be added to the Command attribute
	 * @param  prop   The feature to be added to the Command attribute
	 * @param  value  The feature to be added to the Command attribute
	 */
	public void addCommand(String type, String prop, String value) {
		this.addCommand(type, prop, value, null);
	}


	/**
	 *  Adds a command specified as a prop and value pair of the specified type and
	 *  action.
	 *
	 * @param  type    The feature to be added to the Command attribute
	 * @param  prop    The feature to be added to the Command attribute
	 * @param  value   The feature to be added to the Command attribute
	 * @param  action  The feature to be added to the Command attribute
	 */
	public void addCommand(String type, String prop, String value, String action) {
		InputXMLComponent comp;
		try {
			comp = getComponent(type);
		} catch (Exception e) {
			prtln("addCommand error: " + e);
			return;
		}
		comp.addCommand(prop, value, action);
	}


	/**
	 *  Adds a property command with the specified property belonging to the dlese
	 *  namespace.
	 *
	 * @param  prop   The feature to be added to the NcsProperty attribute
	 * @param  value  The feature to be added to the NcsProperty attribute
	 */
	public void addNcsPropertyCmd(String prop, String value) {
		addNcsPropertyCmd(prop, value, null);
	}


	/**
	 *  Adds a property command with the specified property belonging to the dlese
	 *  namespace.
	 *
	 * @param  prop    The feature to be added to the NcsProperty attribute
	 * @param  value   The feature to be added to the NcsProperty attribute
	 * @param  action  The feature to be added to the NcsProperty attribute
	 */
	public void addNcsPropertyCmd(String prop, String value, String action) {
		addQualifiedCommand(NDRConstants.NCS_NAMESPACE, "property", prop, value, action);
	}


	/**
	 *  Adds a feature to the NcsRelationshipCmd attribute of the NdrRequest object
	 *
	 * @param  prop   The feature to be added to the NcsRelationshipCmd attribute
	 * @param  value  The feature to be added to the NcsRelationshipCmd attribute
	 */
	public void addNcsRelationshipCmd(String prop, String value) {
		addNcsRelationshipCmd(prop, value, null);
	}


	/**
	 *  Adds a property command with the specified property belonging to the dlese
	 *  namespace.
	 *
	 * @param  prop    The feature to be added to the NcsProperty attribute
	 * @param  value   The feature to be added to the NcsProperty attribute
	 * @param  action  The feature to be added to the NcsProperty attribute
	 */
	public void addNcsRelationshipCmd(String prop, String value, String action) {
		addQualifiedCommand(NDRConstants.NCS_NAMESPACE, "relationship", prop, value, action);
	}


	/**
	 *  Adds a feature to the DleseCommand attribute of the NdrRequest object
	 *
	 * @param  type   The feature to be added to the DleseCommand attribute
	 * @param  prop   The feature to be added to the DleseCommand attribute
	 * @param  value  The feature to be added to the DleseCommand attribute
	 */
	public void addDleseCommand(String type, String prop, String value) {
		addDleseCommand(type, prop, value, null);
	}


	/**
	 *  Adds a feature to the DleseCommand attribute of the NdrRequest object
	 *
	 * @param  type    The feature to be added to the DleseCommand attribute
	 * @param  prop    The feature to be added to the DleseCommand attribute
	 * @param  value   The feature to be added to the DleseCommand attribute
	 * @param  action  The feature to be added to the DleseCommand attribute
	 */
	public void addDleseCommand(String type, String prop, String value, String action) {
		addQualifiedCommand(NDRConstants.DLESE_NAMESPACE, type, prop, value, action);
	}


	/**
	 *  Adds a feature to the QualifiedCommand attribute of the NdrRequest object
	 *
	 * @param  namespace  The feature to be added to the QualifiedCommand attribute
	 * @param  type       The feature to be added to the QualifiedCommand attribute
	 * @param  prop       The feature to be added to the QualifiedCommand attribute
	 * @param  value      The feature to be added to the QualifiedCommand attribute
	 */
	public void addQualifiedCommand(Namespace namespace,
	                                String type,
	                                String prop,
	                                String value) {
		addQualifiedCommand(namespace, type, prop, value, null);
	}


	/**
	 *  Adds a feature to the QualifiedCommand attribute of the NdrRequest object
	 *
	 * @param  namespace  The feature to be added to the QualifiedCommand attribute
	 * @param  type       The feature to be added to the QualifiedCommand attribute
	 * @param  prop       The feature to be added to the QualifiedCommand attribute
	 * @param  value      The feature to be added to the QualifiedCommand attribute
	 * @param  action     The feature to be added to the QualifiedCommand attribute
	 */
	public void addQualifiedCommand(Namespace namespace,
	                                String type,
	                                String prop,
	                                String value,
	                                String action) {
		QName qname = new QName(prop, namespace);
		Element element = DocumentHelper.createElement(qname);
		element.setText(value);
		addCommand(type, element, action);
	}

	// ------------ DataStream commands ----------
	
	public void addNativeDataStreamCmd(String format, Element content) throws Exception {
		addNativeDataStreamCmd(format, content, null);
	}
	
	public void addNativeDataStreamCmd(String format, Element content, String action) throws Exception {
		if (this.inputXML == null)
			throw new Exception("attempting to addNativeDataStream to null inputXML");
		this.inputXML.getData().addDataStreamCmd("native_"+format, content, action);
	}
	
	/**
	 *  Adds a datastream command to the data component with given datastream of
	 *  specified format (e.g., "ndsl_dc");
	 *
	 * @param  format         format of the datastream
	 * @param  content        datastream represented as Element
	 * @exception  Exception  If inputXML does not exist or content element is null
	 */
	public void addDataStreamCmd(String format, Element content) throws Exception {
		addDataStreamCmd(format, content, null);
	}

	/**
	 *  Adds a datastream command to the data component with given datastream of
	 *  specified format (e.g., "ndsl_dc");
	 *
	 * @param  format         The feature to be added to the DataStreamCmd
	 *      attribute
	 * @param  content        The feature to be added to the DataStreamCmd
	 *      attribute
	 * @param  action         The feature to be added to the DataStreamCmd
	 *      attribute
	 * @exception  Exception  If inputXML does not exist or content element is null
	 */
	public void addDataStreamCmd(String format, Element content, String action) throws Exception {
		if (this.inputXML == null)
			throw new Exception("attempting to addDataStream to null inputXML");
		this.inputXML.getData().addDataStreamCmd(format, content, action);
	}

	

	/**
	 *  Sets the dataInfoStream attribute of the NdrRequest object
	 *
	 * @param  format         The new dataInfoStream value
	 * @param  info           The new dataInfoStream value
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public void setDataInfoStream(String format, Element info) throws Exception {
		if (this.inputXML == null)
			throw new Exception("attempting to addDataInfoStream to null inputXML");
		this.inputXML.getData().setInfoStream(format, info);
	}


	/**
	 *  Adds a serviceDescription command to the data element.
	 *
	 * @param  content        ServiceDescription as Element
	 * @exception  Exception  if inputXML does not exist
	 */
	public void addServiceDescriptionCmd(Element content) throws Exception {
		addServiceDescriptionCmd(content, null);
	}


	/**
	 *  Adds a feature to the StateCmd attribute of the NdrRequest object
	 *
	 * @param  state  The feature to be added to the StateCmd attribute
	 */
	public void addStateCmd(NDRConstants.ObjectState state) {
		addStateCmd(state, null);
	}


	/**
	 *  Adds a feature to the StateCmd attribute of the NdrRequest object
	 *
	 * @param  state   The feature to be added to the StateCmd attribute
	 * @param  action  The feature to be added to the StateCmd attribute
	 */
	public void addStateCmd(NDRConstants.ObjectState state, String action) {
		addQualifiedCommand(NDRConstants.FEDORA_MODEL_NAMESPACE, "property", "state", state.toString(), action);
	}


	/**
	 *  Adds a feature to the OaiVisibilityCmd attribute of the NdrRequest object
	 *
	 * @param  visibility  The feature to be added to the OaiVisibilityCmd
	 *      attribute
	 */
	public void addOaiVisibilityCmd(NDRConstants.OAIVisibilty visibility) {
		addOaiVisibilityCmd(visibility, null);
	}


	/**
	 *  Adds a feature to the OaiVisibilityCmd attribute of the NdrRequest object
	 *
	 * @param  visibility  The feature to be added to the OaiVisibilityCmd
	 *      attribute
	 * @param  action      The feature to be added to the OaiVisibilityCmd
	 *      attribute
	 */
	public void addOaiVisibilityCmd(NDRConstants.OAIVisibilty visibility, String action) {
		addQualifiedCommand(NDRConstants.OAI_NAMESPACE, "property", "visibility", visibility.toString(), action);
	}


	/**
	 *  Adds a feature to the ServiceDescriptionCmd attribute of the NdrRequest
	 *  object
	 *
	 * @param  content        The feature to be added to the ServiceDescriptionCmd
	 *      attribute
	 * @param  action         The feature to be added to the ServiceDescriptionCmd
	 *      attribute
	 * @exception  Exception  if inputXML does not exist
	 */
	public void addServiceDescriptionCmd(Element content, String action) throws Exception {
		if (this.inputXML == null)
			throw new Exception("attempting to addDataStream to null inputXML");
		this.inputXML.getData().addCommand(content, action);
	}


	/**
	 *  Adds a feature to the DCStreamCmd attribute of the NdrRequest object
	 *
	 * @param  oai_dc         The feature to be added to the DCStreamCmd attribute
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public void addDCStreamCmd(Element oai_dc) throws Exception {
		if (this.inputXML == null)
			throw new Exception("attempting to add DC Stream to null inputXML");
		Element DC = DocumentHelper.createElement("DC");
		DC.add(oai_dc);
		this.inputXML.getData().addCommand(DC, null);
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
		NDRConnection connection = super.getNDRConnection(path);

		payload = inputXMLStr;
		if (payload == null && this.inputXML != null) {
			// format the inputXML so it is in a human-readable form
			payload = Dom4jUtils.prettyPrint(this.inputXML.asDocument());
		}

		if (payload != null)
			connection.setContent("inputXML=" + java.net.URLEncoder.encode(payload, "UTF-8"));
		return connection;
	}

		/**
	 *  NOT YET DOCUMENTED
	 *
	 * @return                NOT YET DOCUMENTED
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public InfoXML submit() throws Exception {
		return submit(null);
	}

	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  inputXMLStr    NOT YET DOCUMENTED
	 * @return                NOT YET DOCUMENTED
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public InfoXML submit(String inputXMLStr) throws Exception {
		if (verb == null || verb.trim().length() == 0)
			throw new Exception("attempting to submit request without specifying verb");

		String path = makePath();
		
		// report(path);

		NDRConnection connection = getNDRConnection(path, inputXMLStr);
		if (getVerbose()) {
			prtln("\n===============\nproxyRequest");
			prtln(path);

			if (getPayload() != null)
				prtln(getPayload());
		}

		InfoXML proxyResponse = new InfoXML(connection.request());

		if (getVerbose()) {
			prtln("\n===============\nproxyResponse");
			try {
				Document responseDoc = DocumentHelper.parseText(proxyResponse.getResponse());
				pp(responseDoc);
			} catch (Exception e) {
				prtln("response could not be displayed: " + e.getMessage());
			}
		}
		return proxyResponse;
	}


	/**  NOT YET DOCUMENTED */
	public void report(String path) {

		prtln("NdrRequest: submit");
		prtln("\t path: " + path);
		prtln("\t verbose: " + getVerbose());
	}

}

