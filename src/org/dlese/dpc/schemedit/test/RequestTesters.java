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
package org.dlese.dpc.schemedit.test;

import org.dlese.dpc.schemedit.SchemEditUtils;
import org.dlese.dpc.schemedit.test.TesterUtils;
import org.dlese.dpc.schemedit.ndr.util.ServiceDescription;

import org.dlese.dpc.ndr.NdrUtils;
import org.dlese.dpc.ndr.DleseAsUseCaseHelper;
import org.dlese.dpc.ndr.request.*;
import org.dlese.dpc.ndr.reader.*;
import org.dlese.dpc.ndr.apiproxy.InfoXML;
import org.dlese.dpc.ndr.apiproxy.NDRConstants;
import org.dlese.dpc.ndr.apiproxy.NDRConstants.NDRObjectType;
import org.dlese.dpc.xml.Dom4jUtils;
import org.dom4j.*;

import java.util.*;

import java.lang.reflect.Method;

public class RequestTesters {

	private static boolean debug = true;
	
	private static String NCS_AGENT = NDRConstants.NCS_TEST_AGENT;
	private static String DLESE_AGENT = NDRConstants.DLESE_TEST_AGENT;
	
	public  RequestTesters () {
	}

	
	public static void enumTester () {
		String objectType = "Metadata";
		
/* 		NDRConstants.NDRObjectType [] types = NDRConstants.NDRObjectType.values();
		for (int i=0;i<types.length;i++)
			prtln ( types[i].getTag() ); */
		
		NDRObjectType supportedType = NDRConstants.getNdrResponseType (objectType);
		if (supportedType == null)
			prtln ("NDRObjectType not found for " + objectType);
		else
			prtln ("Supported Type: " + supportedType.toString());
		
		
	}
	
	public static void previewDCSSetup() {
		String ndrApiBaseUrl = "http://ndrtest.nsdl.org/api";
		String ncsAgent = "2200/test.20070601114303740T"; // ncs.nsdl.org
		String keyFile = "D:/mykeys/ndrtest_private";
		
		NdrUtils.setup(ndrApiBaseUrl, ncsAgent, keyFile);
	}
	
	public static void taosSetup() {

		String keyFile = "/Users/ostwald/devel/misc/ndrPrivateKey";
		String ndrApiBaseUrl = "http://ndrtest.nsdl.org/api";
		String ncsAgent = "2200/test.20060829130238941T";
		NdrUtils.setup(ndrApiBaseUrl, ncsAgent, keyFile);
	}
	
	public static void ndrTestSetup() {
		String ndrApiBaseUrl = "http://ndrtest.nsdl.org/api";
		String ncsAgent = "2200/test.20060829130238941T"; // dlese.test
		String keyFile = "C:/mykeys/ndrtest_private";
		NdrUtils.setup(ndrApiBaseUrl, ncsAgent, keyFile);
	}
	
	private static void setup () {
		// NdrUtils.ndrProductionSetup();
		// NdrUtils.ndrNearProductionSetup();
		ndrTestSetup();
		// previewDCSSetup();
		// org.dlese.dpc.schemedit.ndr.util.CollectionIntegrator.setup();
		// taosSetup();
	}
	
	public static void main (String [] args) throws Exception {
		setup();
		
		String methodName = null;
		
		SimpleNdrRequest.setDebug (true);
		SimpleNdrRequest.setVerbose (true);
		// prtln ("HOST: " + NDRConstants.getNdrServer());
		prtln ("NDR Base Url: " + NDRConstants.getNdrApiBaseUrl());
		prtln ("AGENT: " + NDRConstants.getNcsAgent());
		
		if (args.length > 0) {
			invokeMethod (args[0].trim());
			// prtln ("methodName: " + methodName);
		}
		else {
			prtln ("no method provided");
/* 			prtln ("no method provided - running default method");
			deletedRecordsTester(); */
		}
	}
	
	private static void invokeMethod (String methodName) throws Exception {
		Method method = new RequestTesters().getClass().getMethod(methodName, new Class[0]);
		if (method == null) {
			prtln (" method not found for " + methodName);
			return;
		}
		try {
			method.invoke (null);
		} catch (Throwable e) {
			throw new Exception (e.getCause().getMessage());
		}
	}	
	/*
	aggregator: 2200/test.20070806193858997T
	member (agent): 2200/test.20070806194051429T
	*/
	public static void delete () throws Exception {
		String handle = "2200/20090212184708343T";
		DeleteRequest request = new DeleteRequest(handle);
		// request.setRequestAgent(NCS_AGENT);
		request.submit();
	}
	
	public static void undelete () throws Exception {
		String handle = "2200/20070618152029897T";
		NdrUtils.setObjectState(handle, NDRConstants.ObjectState.ACTIVE);
	}
	
	public static void deleteMdp () throws Exception {
		String mdpHandle = "2200/test.20080208200109041T";
		// CASCASE approach has not been successful ...
		/*
		DeleteRequest request = new DeleteRequest(mdpHandle, true);
		request.submit();
		*/
		
		NdrUtils.deleteNDRCollection(mdpHandle);
	}
	
	public static void rmSetInfo () throws Exception {
		String mdpHandle = "2200/test.20080205131527997T";
		MetadataProviderReader reader = new MetadataProviderReader (mdpHandle);
		if (reader == null)
			throw new Exception ("mdp object not found for " + mdpHandle);
		String setSpec = reader.getProperty("setSpec");
		String setName = reader.getProperty("setName");
		ModifyMetadataProviderRequest request = new ModifyMetadataProviderRequest (mdpHandle);
		request.addCommand ("property", "setSpec", setSpec, "delete");
		request.addCommand ("property", "setName", setName, "delete");
		request.submit();
	}
	
	public static void mdpReader () throws Exception {
		String mdpHandle = "2200/test.20080206181854391T";
		MetadataProviderReader reader = new MetadataProviderReader (mdpHandle);
		if (reader == null)
			throw new Exception ("mdp object not found for " + mdpHandle);	
		prtln (reader.getInactiveMemberCount() + " inactive items");
	}
	
	public static void findPCResource () throws Exception {
		FindRequest request = new FindRequest (NDRConstants.NDRObjectType.RESOURCE);
		// request.addNcsPropertyCmd("contentType", "application/pdf");
		request.addNcsPropertyCmd("hasPrimaryContent", "true");
		// request.addCommand("property", "hasResourceURL", "http://foo/bar/6");
		String resHandle = request.getResultHandle();
		prtln ("resHandle: " + resHandle);
	}
	
	public static void findResourceOLD () throws Exception {
		
		FindResourceRequest request = new FindResourceRequest("http://www.mockobjects.com/");
		
		InfoXML proxyResponse = request.submit();
		
		if (proxyResponse.hasErrors())
			prtln ("ERROR: " + proxyResponse.getError());
		
		String handle = null;
		try  {
			handle = proxyResponse.getHandle();
		} catch (NullPointerException e) {}
		if (handle != null)
			prtln ("handle: " + handle);
		else
			prtln ("resource not found");
	}	
	
	public static void findResource () throws Exception {
		
		String url = "http://imaginemars.jpl.nasa.gov/index1.html";

		String handle = null;
		try  {
			handle = NdrUtils.findResource(url);
		} catch (Exception e) {
			prtln ("findResource Exception: " + e.getMessage());
		}
		if (handle != null)
			prtln ("handle: " + handle);
		else
			prtln ("resource not found");
	}
	
	public static void findDupResources () throws Exception {
		String url = "http://imaginemars.jpl.nasa.gov/index1.html";
		
		FindRequest request = new FindRequest (NDRConstants.NDRObjectType.RESOURCE);
		
		// specify a metadataProvider
		request.addCommand("property", "hasResourceURL", url);

		List records = request.getResultHandles();
		prtln (records.size() + " records found for " + url);
		for (Iterator i=records.iterator();i.hasNext();) {
			prtln ("\t" + (String)i.next());
		}
	}
	
	public static void getMDP () throws Exception {
		String mdpHandle = "2200/test.20080129143218997T";
		MetadataProviderReader reader = new MetadataProviderReader (mdpHandle);
		prtln (reader.toString());
	}
	
	public static void getAggregator () throws Exception {
		prtln ("NDR Base Url: " + NDRConstants.getNdrApiBaseUrl());
		String aggHandle = "2200/20061002125241336T";
		// AggregatorReader reader = new AggregatorReader (aggHandle);
		NdrObjectReader reader = new NdrObjectReader (aggHandle);
		prtln (reader.toString());
		pp (reader.getDocument());
	}
	
	public static void getAgent () throws Exception {
		String agentHandle = NDRConstants.getNcsAgent();
		prtln ("Agent handle: " + agentHandle);
		AgentReader reader = new AgentReader (agentHandle);
		prtln (reader.toString());
	}
	
	public static void findAgentById () throws Exception {
		
		/* FindRequest.setDebug(true); */
		FindRequest request = new FindRequest (NDRObjectType.AGENT);
		
		String id = "ncs.nsdl.org";
		String type = "HOST";
		

		request.addCommand("property", "hasResource"+type, id);
		
		prtln ("FindRequest result: " + request.getResultHandle());
	}

	public static void findAgent () throws Exception {
		
		// String agentHandle = "2200/20061002124937799T";
		String agentHandle = "2200/NSDL_Agents_Collectio";
		try {
			NdrObjectReader reader = new NdrObjectReader (agentHandle);
			if (reader.getObjectType() != NDRConstants.NDRObjectType.AGENT)
				throw new Exception ("This aint no Agent");
			// AgentReader reader = new AgentReader (agentHandle);
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}
	
	public static void addAgent () throws Exception {
		String id = "foo.com";
		String type = "HOST";
		prtln ("added agent: " + AddAgentRequest.addAgent(id, type));
	}

	public static void dsTester () throws Exception {
		MetadataReader reader = new MetadataReader ("2200/test.20070817133829402T", "adn");
		Element foo = reader.getDataStream("adn");
		if (foo == null)
			prtln ("foo is null");
	}
		
	
	public static void addMetadata() throws Exception {
		AddMetadataRequest request = new AddMetadataRequest();

		String id = "FAKO-001";
		request.setUniqueId(id);

		String resourceHandle = "2200/test.20090211181011540T";
		request.setMetadataFor(resourceHandle);

/* 		String aggregatorHandle = "2200/test.20090211175402341T";
		request.setMetadataFor(aggregatorHandle); */

		request.setMetadataProvidedBy("2200/test.20090211175403264T");

		Element ncs_collect = DocumentHelper.createElement("record");
		ncs_collect.addElement("recordID").setText(id);
		ncs_collect.addElement("description").setText("this is the metadata for the collection");

		request.addDataStreamCmd("ncs_collect", ncs_collect);

		request.report();
		request.submit();
	}
	
	public static void modifyMetadata () throws Exception {
		
		String mdHandle = "2200/test.20090211175405312T";
		try {
			ModifyMetadataRequest request = new ModifyMetadataRequest(mdHandle);
			// String mdpHandle = "2200/test.20070201143043986T";
			// request.addNcsRelationshipCmd("collectionMetadataFor", mdpHandle, "add");
			
			// String aggHandle = "2200/20061002130115472T";
			// request.addDataStreamCmd("dcs_data", null, "delete");
			// request.addCommand ("property", "myProp", "my value");
			
			// request.addNcsPropertyCmd("isValid", "false"); //NCSFinalStatus
			
			request.addOaiVisibilityCmd(NDRConstants.OAIVisibilty.PUBLIC);
			
			// request.addStateCmd (NDRConstants.ObjectState.ACTIVE);
			
			// was modifyMetadata
			// request.addCommand ("relationship", "metadataFor", "2200/test.20080825134906293T", "add");
			request.submit ();
		} catch (Throwable t) {
			prtln (t.getMessage());
			t.printStackTrace();
		}
	}
	
	public static void setState () throws Exception {
		String handle = "2200/test.20080425204206935T";
		NDRConstants.ObjectState state = NDRConstants.ObjectState.ACTIVE; // ACTIVE, DELETED, INACTIVE
		NdrUtils.setObjectState (handle, state);
	}
	
	public static void getMetadata () throws Exception {
		String mdHandle = "2200/20080317203738000T";
		MetadataReader reader = new MetadataReader (mdHandle, "ncs_item");
		prtln (Dom4jUtils.prettyPrint(reader.getDocument()));
		String mdp = reader.getRelationship("ncs:collectionMetadataFor");
		prtln ("mdp: " + mdp);
	}
	
	/** NOTE: this does not work for dlese.org metadata:
	no metadatadata is found that is metadataFor 2200/NSDL_Recommended_Resources_Collection
	*/
	public static void getInfoLink () throws Exception {
		
		String mdHandle = "2200/20061002124900276T";  // visionlearning
		
/* 		MetadataReader md = new MetadataReader (mdHandle);
		String mdpHandle = md.getRelationship("metadataProvidedBy");
		
		MetadataProviderReader mdp = new MetadataProviderReader (mdpHandle);
		String aggHandle = mdp.getRelationship("aggregatedBy");
		
		FindRequest request = new FindRequest(NDRConstants.NDRObjectType.METADATA);
		request.addCommand("relationship", "metadataFor", aggHandle);
		String aggMetadataHandle = request.getResultHandle();
		prtln ("Super Collection Metadata handle: " + aggMetadataHandle);
		MetadataReader aggMetadata = new MetadataReader(aggMetadataHandle);
		
		// finally, grab the itemId from the aggregator's MD
		String link = aggMetadata.getProperty("itemId"); */
		
		String link = NdrUtils.getInfoLink (mdHandle);
		
		prtln (" ... derived link: " + link);
	}
	
	public static void modifyMetadataProvider () throws Exception {
		
		// should have "metadataFor" relationships to both of these ...
		String mdpHandle = "2200/test.20090212190724299T";
		
/* 		Element empty = DocumentHelper.createElement("empty");
		Element sd = DocumentHelper.createElement("serviceDescription");
		sd.add(DocumentHelper.createNamespace("dc", "http://purl.org/dc/elements/1.1/")); */
		
		ModifyMetadataProviderRequest request = new ModifyMetadataProviderRequest(mdpHandle);
/* 		request.addQualifiedCommand (NDRConstants.DLESE_NAMESPACE, "property", "collectionName", "Zombie MDP 2");
		request.addDataStreamCmd ("dcs_data", empty);
		request.addDataStreamCmd ("dlese_collect", empty);
		request.addDataStreamCmd ( "replace", null, "delete" ); */
		
		request.addCommand ("property", "testing", "what do you know", "add");
		
		request.submit ();
	}
	

	public static int countMembers () throws Exception {
		String handle = "2200/test.20071219161432112T";
		return (new CountMembersRequest(handle).getCount());
	}
	
	public static void listMembers () throws Exception {
		prtln ("listMembers()");
		String mdpHandle = NDRConstants.getMasterAgent();
		// String mdpHandle = "2200/test.20070810170903636T";
		try {
/* 			ListMembersRequest request = new ListMembersRequest(mdpHandle);
			List mdHandles = request.getResultHandles(); */
			// request.submit();
			
			List mdHandles = new ListMembersRequest(mdpHandle).getResultHandles();
			
 			if (mdHandles == null) {
				prtln ("mdHandles == NULL!");
				return;
			}
			prtln (mdHandles.size() + " handles found");
/* 			for (Iterator i=mdHandles.iterator();i.hasNext();)
				prtln ("\t" + (String)i.next()); */
		} catch (Throwable t) {
			prtln (t.getMessage());
			t.printStackTrace();
		}
	}
		
	public static void listAgents () throws Exception {
		prtln ("listAgents()");
		String mdpHandle = "2200/test.20070810170903636T";
		try {
			FindRequest request = new FindRequest(NDRObjectType.AGENT);
		
			// String parentAggregatorHandle = "2200/NSDL_TRUSTED_APPLICATIONS";
			// request.addCommand("relationship", "memberOf", parentAggregatorHandle);
			
			request.submit();
/* 			List mdHandles = new ListMembersRequest(mdpHandle).getResultHandles();
			
 			if (mdHandles == null) {
				prtln ("mdHandles == NULL!");
				return;
			}
			prtln (mdHandles.size() + " handles found");
			for (Iterator i=mdHandles.iterator();i.hasNext();)
				prtln ("\t" + (String)i.next()); */
		} catch (Throwable t) {
			prtln (t.getMessage());
			t.printStackTrace();
		}
	}
	
	public static void listCollections () throws Exception {
		FindRequest request = new FindRequest ();
		request.setObjectType (NDRConstants.NDRObjectType.AGENT);
		request.addCommand ("relationship", "memberOf", "2200/test.20070806193858997T");
		request.submit();
	}
	
	public static void lister () throws Exception {
		FindRequest request = new FindRequest ();
		
		request.setObjectType (NDRConstants.NDRObjectType.METADATAPROVIDER);
		String agenthandle = "2200/test.20070806194051429T";
		request.addCommand ("relationship", "metadataProviderFor", agenthandle);
		
/* 		request.setObjectType (NDRConstants.NDRObjectType.AGGREGATOR);
		String agenthandle = "2200/test.20070806194051429T";
		request.addCommand ("relationship", "aggregatorFor", agenthandle);		
 */
 		request.submit();
	}
	
	public static void grantAggAuthority () throws Exception {
		String aggHandle = "2200/test.20070803183341719T";
		String ncsAgent = NDRConstants.getNcsAgent();
		
		ModifyAggregatorRequest request = new ModifyAggregatorRequest (aggHandle);
		request.setRequestAgent(DLESE_AGENT);
		request.authorizeToChange (NCS_AGENT);
		request.submit();
	}
		
	public static void grantMdpAuthority () throws Exception {
		String mdpHandle = "2200/test.20070810171912045T";
		
		ModifyMetadataProviderRequest request = new ModifyMetadataProviderRequest (mdpHandle);
		request.setRequestAgent(NCS_AGENT);
		request.authorizeToChange (DLESE_AGENT);
		
		request.submit();
	}
	
	public static void find () throws Exception {
		// String url = "http://www.dlese.org/dds/histogram.do?group=subject&key=eserev";
		String url = "http://www.rsc.org:80/chemsoc/dcp/enc/FECS/100chemists.htm";
		FindRequest request = new FindRequest (NDRConstants.NDRObjectType.RESOURCE);
		// request.addCommand("property", "hasResourceURL",  url);
		NDRConstants.ObjectState state = NDRConstants.ObjectState.DELETED;
		request.addQualifiedCommand(
			NDRConstants.FEDORA_MODEL_NAMESPACE, "property", "state", state.toString());
		String handle = request.getResultHandle();
		prtln ("handle: " + handle);
	}
	
	/**
	* to find non-DLESE MDP, we have to start with an aggregator and then find
	* the MDP that is aggregated by it.
	*/
	public static void findMdpForAgg () throws Exception {
		String aggHandle = "2200/test.20061207181240512T";
		FindRequest request = new FindRequest (NDRConstants.NDRObjectType.METADATAPROVIDER);
		request.addCommand("relationship", "aggregatedBy", aggHandle);
		String mdpHandle = request.getResultHandle();
		prtln ("metadataProvider: " + mdpHandle);
	}
	
	public static void findMdpBySetSpec () throws Exception {
		String setSpec = "edmall";
		FindRequest request = new FindRequest (NDRConstants.NDRObjectType.METADATAPROVIDER);
		request.addCommand("property", "setSpec", setSpec);
		String mdpHandle = request.getResultHandle();
		prtln ("metadataProvider: " + mdpHandle);
	}
	
	public static void findMdById () throws Exception {
		FindRequest request = new FindRequest (NDRConstants.NDRObjectType.METADATA);
		request.addCommand("property", "itemId", "oai:nsdl.org:nsdl:4002");
		String mdHandle = request.getResultHandle();
		prtln ("metadata: " + mdHandle);
	}	
	
	public static void findMdByNCSRecordId () throws Exception {
		FindRequest request = new FindRequest (NDRConstants.NDRObjectType.METADATA);
		request.addNcsPropertyCmd("recordId", "Prada-000-000-000-014");
		String mdHandle = request.getResultHandle();
		prtln ("metadata: " + mdHandle);
	}
	
	public static void findCollectionMdpForMD () throws Exception {
		String mdpHandle = "2200/20061002130326847T";
		FindRequest request = new FindRequest (NDRConstants.NDRObjectType.METADATA);
		request.addNcsRelationshipCmd("collectionMetadataFor", mdpHandle);
		/* request.addCommand ("relationship", "ncs:collectionMetadataFor", mdpHandle); */
		List mdHandles = request.getResultHandles();
		for (Iterator i=mdHandles.iterator();i.hasNext();)
			prtln ((String)i.next());
	}
	
	public static void findMdForResource () throws Exception {
		String resHandle = "2200/20061002130134807T";
		FindRequest request = new FindRequest (NDRConstants.NDRObjectType.METADATA);
		request.addCommand ("relationship", "metadataFor", resHandle);
		List mdHandles = request.getResultHandles();
		for (Iterator i=mdHandles.iterator();i.hasNext();)
			prtln ((String)i.next());
	}
	
	public static void deletedRecordsTester () {
		prtln ("\n------------------------\ndeletedRecordsTester");
		String ndrApiBaseUrl = "";
		String ncsAgent = "";
		String keyFile = "";
		NdrUtils.setup(ndrApiBaseUrl, ncsAgent, keyFile);
		
		List records = null;
		
		String mdpHandle = "2200/test.20071219145902683T"; // test NSDL collection
		records = NdrUtils.findDeletedMetadataObjects(mdpHandle);
		
		String collection = "1193182292279";
		// records = DleseAsUseCaseHelper.getDeletedRecordHandles (collection);
		
		prtln (records.size() + " deleted records found");
	}
	
	public static void findDeletedRecords () throws Exception {
		String ndrApiBaseUrl = "";
		String ncsAgent = "";
		String keyFile = "";
		NdrUtils.setup(ndrApiBaseUrl, ncsAgent, keyFile);
		
		String mdpHandle = "2200/test.20071219145902683T"; // test NSDL collection
		String mdHandle = "2200/test.20071219145905297T"; // non-deleted record
		
		FindRequest request = new FindRequest (NDRConstants.NDRObjectType.METADATA);
		
		// specify a metadataProvider
		request.addCommand("relationship", "metadataProvidedBy", mdpHandle);
		
		// request.addCommand("property", "uniqueID", "NCS-ITEM-000-000-000-027");
		// request.addCommand("property", "uniqueID", "TEST-000-000-000-007");
		
		// specify a mdHandle
		// request.addCommand("property", "hasHandle", mdHandle); // deleted
		
		// Specify DELETED State
		request.addQualifiedCommand(NDRConstants.FEDORA_MODEL_NAMESPACE, "property", "state", "Deleted");
		
		
		List records = request.getResultHandles();
		prtln (records.size() + " deleted records found");
	}
	
	public static void myProp () throws Exception {
		String mdHandle = "2200/test.20070810180538329T";		
		ModifyMetadataRequest request = new ModifyMetadataRequest(mdHandle);
		request.setRequestAgent(DLESE_AGENT);
		request.addCommand ("property", "myProp", "<i>hello</i>");
		request.submit();
	}
		
	public static void myProp1 () throws Exception {
		
		String mdHandle = "2200/test.20070810180538329T";		
		ModifyMetadataRequest request = new ModifyMetadataRequest(mdHandle);
		request.setRequestAgent(DLESE_AGENT);
		String inputXML = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" + 
			"<InputXML xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" \n" +
			     "xmlns=\"http://ns.nsdl.org/ndr/request_v1.00/\" \n" +
				 "xsi:schemaLocation=\"http://ns.nsdl.org/ndr/request_v1.00/ \n" +
				    "http://ns.nsdl.org/schemas/ndr/request_v1.00.xsd\" \n" +
				 "schemaVersion=\"1.00.000\"> \n" +
			  "<metadata> \n" +
				"<properties> \n" +
				  "<myProp><i>hello</i></myProp> \n" +
				"</properties> \n" +
			  "</metadata> \n" +
			"</InputXML> \n";
		
		// request.report();
		request.submit (inputXML);
	}
	
	public static void addResource () throws Exception {
		AddResourceRequest request = new AddResourceRequest();
		// request.setIdentifier ("http://www.dlsciences.org" + "/" + org.dlese.dpc.schemedit.SchemEditUtils.getUniqueId());
		String uid = org.dlese.dpc.schemedit.SchemEditUtils.getUniqueId();
		request.setIdentifier ("my_resource/" + uid, "BROTHER");
		// request.addCommand ("relationship", "memberOf", "2200/NSDL_Collection_of_Resources");
		// request.report();
		request.submit();
	}

	public static void addAggregator() throws Exception {
		// String agentHandle = "2200/NCS";
		AddAggregatorRequest request = new AddAggregatorRequest();

		request.addCommand("relationship", "aggregatorFor", NDRConstants.getNcsAgent());

 		String resourceHandle = "2200/20061002130800895T";
		request.addCommand("relationship", "associatedWith", resourceHandle);
		request.authorizeToChange (NDRConstants.getNcsAgent());
		request.addCommand("relationship", "memberOf", "2200/NSDL_Collection_of_Collections_Aggregator");

		request.report();
		request.submit();
	}
	
	public static void addAgentAggregator() throws Exception {
		String agentHandle = DLESE_AGENT;
		AddAggregatorRequest request = new AddAggregatorRequest();

		request.addCommand("relationship", "aggregatorFor", agentHandle);
		
		String title = "Aggregator for DLESE TEST Agents";
		String description = "Aggregates agents that were created by the dlese.test ncs application";
		ServiceDescription sd = new ServiceDescription (title, description, "");
		
		request.addServiceDescriptionCmd(sd.asElement());
		
		// request.addCommand("relationship", "memberOf", "2200/NSDL_Collection_of_Resources");

		request.submit();
	}
	
		/*
	 2200/test.20070621192337930T
	 2200/test.20070711172802573T
	 2200/test.20070726183707086T
	 */
	
	public static void modifyAggregator () throws Exception {
		
		String aggHandle = "2200/20061002125040618T";
		
		ModifyAggregatorRequest request = new ModifyAggregatorRequest(aggHandle);
		request.addCommand("relationship", "associatedWith", "2200/20061005110743505T");
		// request.addCommand("relationship", "memberOf", NDRConstants.COLLECTION_OF_COLLECTIONS_AGGREGATOR);
		// request.addCommand ("property", "testing", "what do you know", "delete");
		request.report();
		request.submit ();
	}
	
	public static void modifyResource () throws Exception {
		
		String resourceHandle = "2200/test.20080825134906293T";
		
		ModifyAggregatorRequest request = new ModifyAggregatorRequest(resourceHandle);
		// request.addCommand("relationship", "memberOf", "2200/20080317171343451T", "add");
		request.addCommand("relationship", "memberOf", "2200/test.20080710155602928T", "add");
		// request.report();
		request.submit ();
	}
	
	public static void modifyAgent () throws Exception {
		String agentHandle = NCS_AGENT;
		ModifyAgentRequest request = new ModifyAgentRequest (agentHandle);
		request.setRequestAgent (NCS_AGENT);
		request.addNcsPropertyCmd("isNCSApplication", "true");
		request.submit();
	}
		
	public static void isAuthorizedToChangeAgg () throws Exception {
		String aggHandle = "2200/20061002130016930T";
		String agentHandle = "2200/NSDL_COLLECTION_ADMIN";
		AggregatorReader reader = new AggregatorReader (aggHandle);
		boolean auth = reader.isAuthorizedToChange(agentHandle);
		if (auth)
			prtln (agentHandle + " IS authorized to change");
		else
			prtln (agentHandle + " is NOT authorized to change");
	}
	
	public static void isAuthorizedToChangeMdp () throws Exception {
		String mdpHandle = "2200/20061002130018029T";
		String agentHandle = "2200/NSDL_COLLECTION_ADMIN";
		MetadataProviderReader reader = new MetadataProviderReader (mdpHandle);
		boolean auth = reader.isAuthorizedToChange(agentHandle);
		if (auth)
			prtln (agentHandle + " IS authorized to change");
		else
			prtln (agentHandle + " is NOT authorized to change");
	}
	
	public static void isNDRCollectionMetadataProvider1 () throws Exception {
		// String mdpHandle = "2200/20080124181821474T"; // no
		String mdpHandle = "2200/test.20080314021731929T"; // yes
		FindRequest request = new FindRequest (NDRConstants.NDRObjectType.METADATA);
		request.addNcsRelationshipCmd ("collectionMetadataFor", mdpHandle);
		if (request.getResultHandle() == null)
			prtln ("NO");
		else
			prtln ("YES");
	}
	
	public static void isNDRCollectionMetadataProvider () throws Exception {
		// String mdpHandle = "2200/20080124181821474T"; // no
		String mdpHandle = "2200/test.20080314021731929T"; // yes
		if (NdrUtils.isNDRCollectionMetadataProvider (mdpHandle))
			prtln ("YES");
		else
			prtln ("NO");
/* 		try {
			MetadataProviderReader mdpReader = new MetadataProviderReader (mdpHandle);
			String aggHandle = mdpReader.getRelationship("aggregatedBy");
			AggregatorReader aggReader = new AggregatorReader (aggHandle);
			String resHandle = aggReader.getRelationship("associatedWith");
			
			FindRequest request = new FindRequest (NDRConstants.NDRObjectType.METADATA);
			request.addCommand ("relationship", "metadataFor", aggHandle);
			request.addCommand ("relationship", "metadataFor", resHandle);
			
			if (request.getResultHandle() == null)
				prtln ("NO");
			else
				prtln ("YES");
		} catch (Throwable t) {
			t.printStackTrace();
		} */
	}
	
	public static void authorizeToChangeMdp () throws Exception {
		
		String mdpHandle = "2200/20061002130521143T";
		
		// String toAuthAgent = NDRConstants.getNcsAgent();
		String toAuthAgent = NDRConstants.getMasterAgent();
		ModifyMetadataProviderRequest request = new ModifyMetadataProviderRequest(mdpHandle);

		// request.addCommand("property", "foo", "foo value (settable by dlese.test agent but not ncs agent)");
		request.authorizeToChange(toAuthAgent);
		
		request.report();
		request.submit ();
	}
	
	private static void  pp (Node node) {
		prtln (Dom4jUtils.prettyPrint(node));
	}
	
	
	/**
	 *  Description of the Method
	 *
	 *@param  s  Description of the Parameter
	 */
	private static void prtln(String s) {
		if (debug) {
			SchemEditUtils.prtln(s, "");
		}
	}
}
