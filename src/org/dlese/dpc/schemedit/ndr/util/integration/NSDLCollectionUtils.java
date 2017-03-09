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

import org.dlese.dpc.schemedit.ndr.util.NCSCollectReader;
import org.dlese.dpc.schemedit.ndr.util.NCSWebServiceClient;
import org.dlese.dpc.schemedit.SchemEditUtils;
import org.dlese.dpc.ndr.apiproxy.*;
import org.dlese.dpc.ndr.NdrUtils;
import org.dlese.dpc.ndr.reader.*;
import org.dlese.dpc.ndr.request.*;
import org.dlese.dpc.xml.Dom4jUtils;
import org.dlese.dpc.util.Files;
import org.dlese.dpc.util.strings.FindAndReplace;
import org.dlese.dpc.index.SimpleLuceneIndex;
import org.dom4j.*;
import java.util.*;
import java.io.File;
import java.net.*;

/**
 *  Utilities for working with NSDL Collection records housed in the NCS instance.
 *
 * @author    ostwald
 */
public class NSDLCollectionUtils {
	private static boolean debug = true;

	
	private static String baseServiceUrl = "http://ncs.nsdl.org/mgr/services/";

	private static NCSWebServiceClient getNCSClient () {
		return new NCSWebServiceClient (baseServiceUrl);
	}
	
	/**
	*  Creates and tests a {@link org.dlese.dpc.ndr.reader.NSDLCollectionReader} instance.
	 *
	 * @exception  Exception  Description of the Exception
	 */
	static void collectionReaderTest() throws Exception {
		String aggHandle;
		aggHandle = "2200/20061002124859565T";
		// dlese.org
		// aggHandle = "2200/20061002125006440T"; // mathDL

		NSDLCollectionReader nsdlColl = new NSDLCollectionReader(aggHandle);
		// nsdlColl.report();
		String resourceUrl = nsdlColl.getResourceUrl();
		if (resourceUrl == null || resourceUrl.trim().length() == 0) {
			throw new Exception("resourceUrl not found");
		}
		prtln("resourceURL: " + resourceUrl);
		String ncsRecId = getNCSClient().getNCSCollectionID(resourceUrl);
		prtln("NCS Record ID: " + ncsRecId);
	}

 	public static void setBaseServiceUrl (String baseUrl) {
		baseServiceUrl = baseUrl;
	}
	
	public static String getBaseServiceUrl () {
		return baseServiceUrl;
	}
	
	public static String  getCollectionMetadataHandle (String aggHandle) {
		NSDLCollectionReader reader = null;
		try {
			reader = new NSDLCollectionReader (aggHandle);
		} catch (Exception e) {
			prtln ("ERROR: " + e.getMessage());
			return null;
		}
		return reader.metadata.getHandle();
	}

	/**
	 *  Extracts the NCS collection id from a NSDLCollectionReader instance.<p>
	 Never returns null so we can stuff into xml without worry
	 *
	 * @param  collReader  Description of the Parameter
	 * @return             The collection id value, or an empty string if one cannot be found
	 */
 	public static String getNCSCollectionID(NSDLCollectionReader collReader) {
		if (collReader == null) {
			return "";
		}
		String resourceUrl = collReader.getResourceUrl();
		if (resourceUrl == null || resourceUrl.trim().length() == 0) {
			// throw new Exception ("resourceUrl not found");
			prtln("resourceUrl not found");
			return "";
		}
		return getNCSClient().getNCSCollectionID(resourceUrl);
	}


	/**
	 *  Gets an NCSCollectReader instance for a given resourceUrl.<p>
	 Uses web service to find a metadata record for the resourceUrl and then
	 creates a reader object from the response.
	 *
	 * @param  resourceUrl  Description of the Parameter
	 * @return              The nCSRecord value
	 */
 	public static NCSCollectReader getNCSRecord(URL resourceUrl) {
		return getNCSClient().getNCSRecord(resourceUrl);
	}

	/**
	 *  Finds the collection id for the provided collection resource url.<p>
	 *
	 * @param  resourceUrl  Description of the Parameter
	 * @return              The nCSCollectionID value
	 */
	public static String getNCSCollectionID(String resourceUrl) {
		return getNCSClient().getNCSCollectionID(resourceUrl);
	} 


	/**
	 *  Gets a list of collection ids from the NCSL Collections collection.
	 *
	 * @return                The nCSCollectionIDs value
	 * @exception  Exception  Description of the Exception
	 */
 	public static List getNCSCollectionRecordIDs() throws Exception {
		return getNCSClient().getNCSCollectionRecordIDs();
	} 

	public static List getNCSCollectionIDs() throws Exception {
		return getNCSClient().getNCSCollectionIDs();
	}

	/**
	 *  Returns NCSCollectReader instance for NCS Collect record having provided id.
	 *
	 * @param  recId          Description of the Parameter
	 * @return                The nCSRecord value
	 * @exception  Exception  Description of the Exception
	 */
 	public static NCSCollectReader getNCSRecord(String recId) throws Exception {
		return getNCSClient().getNCSRecord (recId);
	} 


	/**
	 *  Gets the nCSRecordByTitle attribute of the NSDLCollectionUtils class
	 *
	 * @param  title          Description of the Parameter
	 * @return                The nCSRecordByTitle value
	 * @exception  Exception  Description of the Exception
	 */
 	public static NCSCollectReader getNCSRecordByTitle(String title) throws Exception {
		return getNCSClient().getNCSRecordByTitle(title);
	} 

	/**
	 *  The main program for the NSDLCollectionUtils class
	 *
	 * @param  args  The command line arguments
	 */
	public static void main(String[] args) {
		File propFile = null; // propFile must be assigned!
		NdrUtils.setup (propFile);

		boolean verbosity = false;
		NdrRequest.setDebug(verbosity);
		NdrRequest.setVerbose(verbosity);

		try {
			// updateCollectionsXML ();
			// collectionReaderTest();
			// pp (getNCSRecord ("NSDL-COLLECTION-4743"));
			// compareInfoModels();
			// compare ("2200/20061002125052244T");

			// getNCSRecordByTitle ("MathDL: The Mathematical Sciences Digital Library");

		} catch (Exception e) {
			prtln("Update ERROR: " + e.getMessage());
			e.printStackTrace();
		}
	}


	/**
	 *  Description of the Method
	 *
	 * @param  aggHandle      Description of the Parameter
	 * @exception  Exception  Description of the Exception
	 */
	public static void compare(String aggHandle) throws Exception {
		CollectionXSLRecord rec = CollectionXSLReader.getCollectionRecord("aggregatorhandle", aggHandle);
		if (rec == null) {
			throw new Exception("aggregator not found for " + aggHandle);
		}
		NSDLCollectionReader nsdlReader = new NSDLCollectionReader(aggHandle);
		// Document ncs_collect = getNCSRecord (rec.get("ncsrecordid"));
		NCSCollectReader ncsReader = getNCSClient().getNCSRecord(rec.get("ncsrecordid"));
		compare(nsdlReader, ncsReader);
	}


	/**
	 *  Description of the Method
	 *
	 * @param  nsdl  Description of the Parameter
	 * @param  ncs   Description of the Parameter
	 */
	public static void compare(NSDLCollectionReader nsdl, NCSCollectReader ncs) {
		prtln("\n--------------------------------");
		prtln("NSDL Collection");
		prtln("resourceURL: " + nsdl.getResourceUrl());
		prtln("title: " + nsdl.getTitle());
		/*
		    prtln ("harvestInfo sets: ");
		    for (Iterator i=nsdl.getHarvestInfo().getSets().iterator();i.hasNext();)
		    prtln ("\t" + (String)i.next());
		 */
		prtln("NCS Collect");
		prtln("url: " + ncs.getUrl());
		prtln("title: " + ncs.getTitle());
	}


	/**
	 *  Description of the Method
	 *
	 * @exception  Exception  Description of the Exception
	 */
	public static void compareInfoModels() throws Exception {
		List ncsCollections = CollectionXSLReader.getNcsCollectionRecords();
		prtln(ncsCollections.size() + " NCS recs found");
		int counter = 0;
		int max = 3;
		for (Iterator i = ncsCollections.iterator(); i.hasNext(); ) {
			if (counter++ >= max) {
				break;
			}
			CollectionXSLRecord rec = (CollectionXSLRecord) i.next();
			String aggHandle = rec.get("aggregatorhandle");

			NSDLCollectionReader nsdlReader = new NSDLCollectionReader(aggHandle);
			NCSCollectReader ncsReader = getNCSClient().getNCSRecord(rec.get("ncsrecordid"));
			compare(nsdlReader, ncsReader);
		}

	}


	/**
	 *  Description of the Method
	 *
	 * @param  node  Description of the Parameter
	 */
	private static void pp(Node node) {
		prtln(Dom4jUtils.prettyPrint(node));
	}


	/**
	 *  Description of the Method
	 *
	 * @param  s  Description of the Parameter
	 */
	private static void prtln(String s) {
		String prefix = null;
		if (debug) {
			NdrUtils.prtln(s, prefix);
		}
	}
}

