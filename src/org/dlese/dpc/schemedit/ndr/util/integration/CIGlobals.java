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

import org.dlese.dpc.schemedit.SchemEditUtils;
import org.dlese.dpc.ndr.NdrUtils;
import org.dlese.dpc.ndr.apiproxy.NDRConstants;
import java.io.File;

/**
 * Reads an XML file containing mappings between ncsrecordid and aggregator handle, and produces
 	an XML file containing more detailed information about the NCS Collection record and the NDR Collection
	Objects associated with the aggregator.
 * @author    Jonathan Ostwald
 */
public class CIGlobals {
	
	static String BASE_DIR = "C:/Documents and Settings/ostwald/devel/CollectionsIntegration/Production_2008_03_17/";
	
	// static String BASE_DIR = "H:/Documents/NDR/CollectionsIntegration/2008_03_16/";
	public static String RECORDS_DIR = BASE_DIR + "records/"; 
	static String DCS_CONFIG_DIR = BASE_DIR + "dcs_conf/";

	static String FRAMEWORK_CONFIG_DIR = DCS_CONFIG_DIR + "frameworks/";
	static String COLLECTION_CONFIG_DIR = DCS_CONFIG_DIR + "collections/";
	
	public static String COLLECTION_MAPPINGS_DIR = BASE_DIR + "collection-mappings/";
	public static String MAPPINGS_MANAGER_DATA = COLLECTION_MAPPINGS_DIR + "MappingsManagerData.xml";
	
	public static void ndrProductionSetup() {
		// String ndrServer = "repo4.nsdl.org";
		String ndrApiBaseUrl = "http://ndr.nsdl.org/api";
		String ncsAgent = "2200/NCS";
		String keyFile = "C:/mykeys/ncs_private_pkcs8";

		NdrUtils.setup(ndrApiBaseUrl, ncsAgent, keyFile);
	}
	
	public static void ndrNearProductionSetup() {
		String ndrApiBaseUrl = "http://server7.nsdl.org/api";
		String ncsAgent = "2200/NCS";
		String keyFile = "C:/mykeys/ncs_private_pkcs8";

		NdrUtils.setup(ndrApiBaseUrl, ncsAgent, keyFile);
	}
	
	public static void setup () throws Exception {
		String config = "production";
		if (config.equals("server7"))
			ndrNearProductionSetup();
		else if (config.equals("production"))
			ndrProductionSetup();
		else
			throw new Exception ("setup error: unknown config: " + config);
		// for taos!
		// NDRConstants.setPrivateKeyFile(new File (TAOS_PRIVATE_KEY));
		SchemEditUtils.prtln ("setup for " + config);
	}
	
}
