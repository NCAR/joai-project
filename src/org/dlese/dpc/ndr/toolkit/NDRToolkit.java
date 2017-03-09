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
package org.dlese.dpc.ndr.toolkit;

import org.dlese.dpc.ndr.apiproxy.NDRConstants;
import org.dlese.dpc.ndr.NdrUtils;

import org.nsdl.repository.client.Client;
import org.nsdl.repository.util.Configuration;
import org.nsdl.repository.admin.util.MimeTypedStream;
import org.nsdl.repository.model.types.*;
import org.nsdl.repository.model.Property;
import org.nsdl.repository.access.*;
import org.nsdl.repository.access.filters.*;
import org.nsdl.repository.model.NDRObjectInfo;

import org.nsdl.repository.model.NDRObject;
import org.nsdl.repository.access.Results;
import org.nsdl.repository.util.NDRObjectTemplate;

import org.nsdl.repository.util.StaticObjectInfo;
import org.nsdl.repository.client.access.InterpretedNDRAccess;

import org.dom4j.Namespace;

import org.dlese.dpc.util.Files;

import java.io.*;
import java.util.*;

/**
 *  Class for interacting with the NDRToolkit (sorry about the name confusion!).
 *  <p>
 *
 *  Initialized using an Agent Identity obtained from {@link org.dlese.dpc.ndr.apiproxy.NDRConstants}
 *  (see getNdrAccess method) NOTE: methods that change an objects state should
 *  return a HANDLE and not the OBJECT, since the object will not reflect it's
 *  actaul state anymore!!
 *
 * @author    Jonathan Ostwald
 */
public class NDRToolkit {

	// private final FilteredNDRAccess ndr;
	private NDRAccess ndr;
	private Finder finder;
	private Client client = null;
	private Configuration config;
	private MimeTypes mimeTypes;
	private Agent agent;


	/**  Constructor for the ToolKit object */
	public NDRToolkit() {
		prtln("NDRToolkit!");
		try {
			mimeTypes = MimeTypes.getInstance();
		} catch (Throwable e) {
			prtln("Could not read mimetypes: " + e.getMessage());
			// return;
		}
		try {
			ndr = getNdrAccess();
			prtln("access class: " + ndr.getClass().getName());
			agent = getAgent();
			if (agent != null)
				prtln("myAgent: " + agent.getTitle());
			else
				prtln ("No agent supplied - read only mode");
		} catch (Throwable e) {
			prtln("ToolKit ERROR: " + e.getMessage());
		}
	}


	/**
	 *  Instantiates a Client instance, configures the client properties to enable
	 *  it to authenticate with the NDR, and finally obtains access from the
	 *  client.<p>
	 *
	 *  NOTE: AgentHandle and privateKey are NOT required for read operations.<p>
	 *
	 *  This version gets the ndrAccess WITHOUT USING PROPERTIES FILE, but instead
	 *  by relying on NDRConstants values ...
	 *
	 * @return    The ndrAccess value
	 */
	public NDRAccess getNdrAccess() {
		if (client == null) {
			client = Client.getInstance();
			config = client.getConfig();
			config.putProperty("ndr.client.repository.baseURL", NDRConstants.getNdrApiBaseUrl());
			config.putProperty("ndr.client.authentication.agentHandle", NDRConstants.getNcsAgent());
			
			// we may not have a privateKeyFile (if we are in read-only mode)
			if (NDRConstants.getPrivateKeyFile() != null) {
				config.putProperty("ndr.client.authentication.privateKey",
					NDRConstants.getPrivateKeyFile().getAbsolutePath());
			}
			
			config.update();

			prtln("config");
			String property = "ndr.client.repository.baseURL";
			prtln("\t" + property + ": " + config.getProperty(property));
			property = "ndr.client.authentication.agentHandle";
			prtln("\t" + property + ": " + config.getProperty(property));
			property = "ndr.client.authentication.privateKey";
			prtln("\t" + property + ": " + config.getProperty(property));
			ndr = client.getAccess();
		}
		return ndr;
	}


	/**
	 *  Obtains a Finder instance
	 *
	 * @return    The finder value
	 */
	public Finder getFinder() {
		return this.ndr.getObjectFinder();
	}

		/**
	 *  Get a new resource object
	 *
	 * @return    NOT YET DOCUMENTED
	 */
	public Resource newResource() {
		return ndr.newObject(Type.Resource);
	}


	/**
	 *  Get a new metadata object
	 *
	 * @return    NOT YET DOCUMENTED
	 */
	public Metadata newMetadata() {
		return ndr.newObject(Type.Metadata);
	}


	/**
	 *  Gets the resource instance from the NDR for the provided resource handle
	 *
	 * @param  handle         NOT YET DOCUMENTED
	 * @return                The resource value
	 * @exception  Exception  if handle is not a Resource or Resource cannot be found for handle
	 */
	public Resource getResource(String handle) throws Exception {
		NDRObjectInfo identity = new StaticObjectInfo(handle, Type.Resource);
		return ndr.getObject(identity, Type.Resource);
	}

		/**
	 *  Obtain a MetadataProvider object for provided handle
	 *
	 * @param  handle         NOT YET DOCUMENTED
	 * @return                The metadataProvider value
	 * @exception  Exception  if handle is not a MetadataProvider or MetadataProvider cannot be found for handle
	 */
	public MetadataProvider getMetadataProvider(String handle) throws Exception {
		NDRObjectInfo identity = new StaticObjectInfo(handle, Type.MetadataProvider);
		return ndr.getObject(identity, Type.MetadataProvider);
	}


	/**
	 *  Gets the aggregator attribute of the NDRToolkit object
	 *
	 * @param  handle         NOT YET DOCUMENTED
	 * @return                The aggregator value
	 * @exception  Exception  if handle is not a Aggregator or Aggregator cannot be found for handle
	 */
	public Aggregator getAggregator(String handle) throws Exception {
		NDRObjectInfo identity = new StaticObjectInfo(handle, Type.Aggregator);
		return ndr.getObject(identity, Type.Aggregator);
	}

	
	/**
	 *  Find a resource object for the provided url.
	 *
	 * @param  url            the url
	 * @return                NOT YET DOCUMENTED
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public Resource findResource(String url) throws Exception {
		return ndr.getObjectFinder().findResource(Resource.IDENTIFIER_URL, url);
	}


	/**  Output configuration properties to the console.  */
	public void showConfig() {
		prtln("config");
		String property = "ndr.client.repository.baseURL";
		prtln("\t" + property + ": " + config.getProperty(property));
		property = "ndr.client.authentication.agentHandle";
		prtln("\t" + property + ": " + config.getProperty(property));
		property = "ndr.client.authentication.privateKey";
		prtln("\t" + property + ": " + config.getProperty(property));
	}


	/**
	 *  Find ALL resources in the NDR
	 *
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public void getAllResources() throws Exception {

		prtln("getAllResources");

		NDRObject resTemplate = new NDRObjectTemplate(Type.Resource);
		// resTemplate.addRelationship(Resource.MEMBER_OF, aggregator);

		Results <NDRObjectInfo> resources = this.getFinder().matchObject(resTemplate);
		int count = 0;
 		try {
			for (NDRObjectInfo res : resources) {
				// prtln ("\t" + res.handle());
				count++;
			}
		} finally {
			resources.close();
		}
		prtln(count + " resources found");
	}

	/**
	* Find the aggregators corresponding to NSDL.org collections
	*/
	public void findNsdlOrgAggregators() throws Exception {

		prtln("findNsdlOrgAggregators");

		// create aggregator used in MEMBER_OF relationship
		Aggregator nsdlCollections = 
			this.getAggregator("2200/NSDL_Collection_of_Collections_Aggregator");
		if (nsdlCollections == null)
			throw new Exception ("NSDL_Collection_of_Collections_Aggregator not found");
			
		// create property to test for ncs:status = "NCSFinalStatus"
		Namespace ncsNamespace = NDRConstants.NCS_NAMESPACE;
		// Property.Name statusProp = new Property.Name(ncsNamespace.getURI(), "status", ncsNamespace.getPrefix());
		Property.Name statusProp = new Property.Name("http://ncs.nsdl.org", "status", "ncs");

		// build the template
		NDRObject resTemplate = new NDRObjectTemplate(Type.Aggregator);
		resTemplate.addRelationship(Resource.MEMBER_OF, nsdlCollections);
		resTemplate.addProperty (statusProp, "NCSFinalStatus");

		Results <NDRObjectInfo> aggregators = this.getFinder().matchObject(resTemplate);
		int count = 0;
 		try {
			for (NDRObjectInfo res : aggregators) {
				// prtln ("\t" + res.handle());
				count++;
			}
		} finally {
			aggregators.close();
		}
		prtln(count + " aggregators found");
	}
	

	/**
	 *  Gets the agent attribute of the ToolKit object
	 *
	 * @return    The agent value
	 */
	public Agent getAgent() {
		String handle = config.getProperty(
			"ndr.client.authentication.agentHandle");
		if (handle == null)
			return null;
		NDRObjectInfo identity = new StaticObjectInfo(handle, Type.Agent);
		return ndr.getObject(identity, Type.Agent);
	}


	/**
	 *  Create new resource, set content to provided file, and return updated
	 *  resource.
	 *
	 * @param  contentFile    NOT YET DOCUMENTED
	 * @return                NOT YET DOCUMENTED
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public String uploadFile(File contentFile) throws Exception {
		try {

			// hack to use same resource over and over
			/* 			String tmpResourceHandle = "2200/test.20090410170951993T"; // TEMPORARY
			Resource cannedResource = tk.getResource(tmpResourceHandle); */
			// production:
			Resource resource = this.newResource();
			String uploadedResourceHandle = this.setResourceContent(resource, contentFile);

			// How do we know if things haven't gone well?
			// must UPDATE to get the contentURL property!
			// updatedResource = this.getResource(uploadedResourceHandle);


			return uploadedResourceHandle;
		} catch (Throwable t) {
			throw new Exception(t.getMessage());
		}
	}


	/**
	 *  The main program for the ToolKit class
	 *
	 * @param  args           The command line arguments
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public static void main(String[] args) throws Exception {
		org.dlese.dpc.schemedit.test.TesterUtils.setSystemProps();
		String props = "C:/Documents and Settings/ostwald/devel/ndrServerProps/ndr.test.properties";
		NdrUtils.setup(new File(props));
		NDRToolkit tester = new NDRToolkit();

		// Resource res = tester.ndr.newObject(Type.Resource);

		/* 		String resHandle = "2200/test.20090410170951993T";
		Resource res = tester.getResource(resHandle);
		String dataPath = "C:/tmp/star.gif";
		// String dataPath = "C:/tmp/box.jpg"; */
		/* 		String dataPath = "C:/tmp/CollectionsAPI.png";
		Resource res = tester.newResource ();
		File dataFile = new File(dataPath);
		try {
			tester.setResourceContent(res, dataFile);
		} catch (Exception e) {
			prtln("Could not set content: " + e.getMessage());
		} */
		// tester.getAllResources();
		tester.findNsdlOrgAggregators();
	}


	/**
	 *  Set the provided content into the NDR Resource object having provided
	 *  resourceUrl
	 *
	 * @param  resourceUrl    NOT YET DOCUMENTED
	 * @param  content        NOT YET DOCUMENTED
	 * @param  label          NOT YET DOCUMENTED
	 * @return                NOT YET DOCUMENTED
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public Resource putContent(String resourceUrl, byte[] content, String label) throws Exception {
		// if resource exists at resourceUrl, then use it
		Resource resource = findResource(resourceUrl);
		if (resource == null) {
			prtln("putContent: resource does not exist");
			resource = ndr.newObject(Type.Resource);
		}
		if (resource == null)
			throw new Exception("Unable to create new resource");
		setResourceContent(resource, content, label);
		return resource;
	}


	/**
	 *  Sets the resourceContent of the provided resource object
	 *
	 * @param  resource       The new resourceContent value
	 * @param  contentFile    The new resourceContent value
	 * @return                NOT YET DOCUMENTED
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public String setResourceContent(Resource resource, File contentFile) throws Exception {
		byte[] bytes = ContentUtils.readBinaryFile(contentFile);
		return setResourceContent(resource, bytes, contentFile.getName());
	}


	/**
	 *  Sets the resourceContent attribute of the NDRToolkit object
	 *
	 * @param  resource       The new resourceContent value
	 * @param  content        The new resourceContent value
	 * @param  label          The new resourceContent value
	 * @return                NOT YET DOCUMENTED
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public String setResourceContent(Resource resource, byte[] content, String label) throws Exception {
		return setResourceContent(resource, content, label, null);
	}


	/**
	 *  Sets the resourceContent attribute of the NDRToolkit object
	 *
	 * @param  resource       The new resourceContent value
	 * @param  content        The new resourceContent value
	 * @param  label          The new resourceContent value
	 * @param  mimeType       The new resourceContent value
	 * @return                handle of
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public String setResourceContent(Resource resource, byte[] content, String label, String mimeType) throws Exception {
		ByteArrayInputStream input = new ByteArrayInputStream(content);

		if (mimeType == null && this.mimeTypes != null) {
			try {
				mimeType = mimeTypes.getMimeType(label);
			} catch (Exception e) {
				prtln("couldn't resolve mimetype, can NDR handle it??");
			}
		}
		if (mimeType == null) {
			resource.setContent(input, label);
		}
		else {
			prtln("calling resource.setContent with mimeType = " + mimeType);
			resource.setContent(input, label, mimeType);
		}

		// no REAL reason to set label (other than to make up for fact that DS attrs aren't updated)
		/* 		Property.Name myLabel_prop = new Property.Name("http://ns.nsdl.org/myns#", "mine", "myLabel");
		Collection labels = resource.getProperties (myLabel_prop);
		prtln (labels.size() + " props found");
		resource.removeProperties (labels);

		myLabel_prop = new Property.Name("http://ns.nsdl.org/myns#", "mine", "myLabel");
		resource.addProperty(myLabel_prop, label); */
		resource.commit();
		prtln("COMMITED ... res: " + resource.handle());
		return resource.handle();
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  s  NOT YET DOCUMENTED
	 */
	public static void prtln(String s) {
		System.out.println(s);
	}

}

