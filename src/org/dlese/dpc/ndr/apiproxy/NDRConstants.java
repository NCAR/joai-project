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
package org.dlese.dpc.ndr.apiproxy;

import java.util.*;
import java.io.File;
import org.dom4j.DocumentHelper;
import org.dom4j.Namespace;

/**
 * This class contains constants that are used in communicating with the NDR
 * 
 * 
 * @author ostwald
 *
 */
public class NDRConstants {

	// uses same key as "2200/test.20070601114303740T"
	static public final String DLESE_TEST_AGENT		 = "2200/test.20060829130238941T"; // dlese.test
	static public final String NCS_TEST_AGENT        = "2200/test.20070601114303740T";
	
	/** Timeout for connections to ndr server in millis*/
	static public final int NDR_CONNECTION_TIMEOUT       = 5 * 60 * 1000; // 5 minutes per nsdl recommendation
	
	/** Used in nsdl_dc_info data streams */
	static public final String NSDL_DC_METADATA_NAMESPACE = "http://ns.nsdl.org/nsdl_dc_v1.02/";
	
	
	/**  NOT YET DOCUMENTED */
	static public String MASTER_COLLECTION = null;
	//	NSDL Master collection is "2200/NSDL_Collection_of_Collections_Aggregator";
	
	static public String MASTER_AGENT = null;
	// NSDL Master agent is	"2200/NSDL_Harvest_Ingest";
	
	static public String FEED_EATER_AGENT = "2200/20091105132121677T";
	
	static public final Namespace NCS_NAMESPACE =
		DocumentHelper.createNamespace("ncs", "http://ncs.nsdl.org");
		
	static public final Namespace DLESE_NAMESPACE =
		DocumentHelper.createNamespace("dlese", "http://dlese.org");
		
	static public final Namespace AUTH_NAMESPACE =
		DocumentHelper.createNamespace("auth", "http://ns.nsdl.org/ndr/auth#");
		
	static public final Namespace FEDORA_MODEL_NAMESPACE =
		DocumentHelper.createNamespace("fedora-model", "info:fedora/fedora-system:def/model#");
		
	static public final Namespace FEDORA_VIEW_NAMESPACE =
		DocumentHelper.createNamespace("fedora-view", "info:fedora/fedora-system:def/view#");	
		
	static public final Namespace NSDL_NAMESPACE =
		DocumentHelper.createNamespace("nsdl", "http://ns.nsdl.org/api/relationships#");	
	
	static public final Namespace OAI_NAMESPACE =
		DocumentHelper.createNamespace("oai", "http://ns.nsdl.org/ndr/oai#");
		
	static public final Namespace WIKI_NAMESPACE =
		DocumentHelper.createNamespace("wiki", "http://ns.nsdl.org/ndr/wiki#");
		
	static public final String NCS_FINAL_STATUS = "NCSFinalStatus";
	
	/**
	* Convenience method to assign values to the three values required for signed request to NDR.
	*/
	public static void init (String ndrApiBaseUrl, String ncsAgentHandle, String privateKeyPath) {
		setNdrApiBaseUrl(ndrApiBaseUrl);
		setNcsAgent(ncsAgentHandle);
		if (privateKeyPath != null)
			setPrivateKeyFile(new File (privateKeyPath));
	}
	
	public enum Service {
		/** TYPES and their XML tags **/
		FIND 			("find"),
		GET 			("get");
		
		/** ENUM data and access methods **/
		String serviceEndpoint;
		
		Service ( String _tag )
		{
			this.serviceEndpoint = _tag;
		}
		
		public String getTag () { return this.serviceEndpoint; }
	};
	
	/**
	* Permissible states for an NDRObject.
	*/
	public enum ObjectState {
		ACTIVE ("Active"),
		INACTIVE ("Inactive"),
		DELETED ("Deleted");
		
		String state;
		
		ObjectState ( String _state ) {
			this.state = _state;
		}
		
		public String toString () {
			return this.state;
		}
		
		public static ObjectState getState (String s) {
			ObjectState ret = null;
			for (Iterator i=EnumSet.allOf (ObjectState.class).iterator();i.hasNext();) {
				ObjectState os = (ObjectState)i.next();
				if (os.toString().equals(s)) {
					ret = os;
					break;
				}
			}
			return ret;
		}
	}
	
	private static Map ndrResponseTypes = null;
	
	private static Map getNdrResponseTypes () {
		if (ndrResponseTypes == null) {
			// prtln (" ** building supported Tag map **");
			ndrResponseTypes = new HashMap();
			for (Iterator i=EnumSet.allOf (NDRObjectType.class).iterator();i.hasNext();) {
				NDRObjectType objectType = (NDRObjectType)i.next();
				ndrResponseTypes.put (objectType.getNdrResponseType(), objectType );
			}
		}
		return ndrResponseTypes;
	}
	
	public static NDRObjectType getNdrResponseType (String tag) {
		return (NDRObjectType) getNdrResponseTypes().get(tag);
	}
	
	/**
	* Permitted values for Metadata and MetadataProvider Objects in the NDR to
	control their visibility to the OAI.
	*/
	public enum OAIVisibilty {
		PUBLIC ("public"),
		PROTECTED ("protected"),
		PRIVATE ("private");
		
		String visibility;
		
		OAIVisibilty ( String _visibility ) {
			this.visibility = _visibility;
		}
		
		public String toString () {
			return this.visibility;
		}
		
		public static OAIVisibilty getVisibility (String s) {
			OAIVisibilty ret = null;
			for (Iterator i=EnumSet.allOf (OAIVisibilty.class).iterator();i.hasNext();) {
				OAIVisibilty vis = (OAIVisibilty)i.next();
				if (vis.toString().equals(s)) {
					ret = vis;
					break;
				}
			}
			return ret;
		}
	}
	
	/**
	 * Supported object types of the NDR, used in InputXML instances and
	 responses.
	 * 
	 */
	public enum NDRObjectType {
		/** TYPES and their XML tags **/
		AGGREGATOR 			("aggregator", "Aggregator"), 
		AGENT 				("agent", "Agent"), 
		RESOURCE 			("resource", "Resource"), 
		METADATA 			("metadata", "Metadata"),
		METADATAPROVIDER	("metadataProvider", "MetadataProvider");
		
		/** ENUM data and access methods **/
		String typeTag;
		String ndrResponseType;
		
		NDRObjectType ( String _typeTag, String _ndrResponseType )
		{
			this.typeTag = _typeTag;
			this.ndrResponseType = _ndrResponseType;
		}
		
		public String getTag () { return this.typeTag; }
		
		public String getNdrResponseType () { return this.ndrResponseType; }
	};
	
	
	public enum SupportedCommand {
		ADD 	( "add" ),
		DELETE 	( "delete" ),
		REPLACE ( "replace" ),  // NOTE: replace should never be used explicitly!!
		FIND	( "match" );
	
		String commandTag;
		
		SupportedCommand( String _commandTag )
		{
			this.commandTag = _commandTag;
		}
		
		String getTag () { return this.commandTag; }		
	}
	
	private static String ndrApiBaseUrl = null;
	
	//ndrApiBaseUrl
	public static String getNdrApiBaseUrl() {
		return ndrApiBaseUrl;
	}
	
	public static void setNdrApiBaseUrl (String url) {
		ndrApiBaseUrl = url;
	}
	
	static private String ncsAgent		 = null; // "2200/test.20070601114303740T";
	
	public static void setNcsAgent (String agentHandle) {
		ncsAgent = agentHandle;
	}

	public static String getNcsAgent () {
		return ncsAgent;
	}	
	
	static private File privateKeyFile = null;
	
	public static void setPrivateKeyFile (File file) {
		privateKeyFile = file;
	}
	
	public static File getPrivateKeyFile () {
		return privateKeyFile;
	}
	//---------
	public static void setMasterCollection (String handle) {
		MASTER_COLLECTION = handle;
	}
	
	public static String getMasterCollection () {
		if (MASTER_COLLECTION == null || MASTER_COLLECTION.trim().length() == 0)
			return null;
		return MASTER_COLLECTION;
	}
	
	public static void setMasterAgent (String handle) {
		MASTER_AGENT = handle;
	}
	
	public static String getMasterAgent () {
		if (MASTER_AGENT == null || MASTER_AGENT.trim().length() == 0)
			return null;
		return MASTER_AGENT;
	}	

	static void prtln (String s) {
		org.dlese.dpc.ndr.NdrUtils.prtln (s, "NDRConstants");
	}

}
