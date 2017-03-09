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
package org.dlese.dpc.ndr;

import org.dlese.dpc.ndr.apiproxy.NDRConstants;
import org.dlese.dpc.ndr.reader.*;
import org.dlese.dpc.ndr.request.*;
import org.dlese.dpc.ndr.apiproxy.InfoXML;

import java.io.File;
import java.util.*;
import org.dom4j.Document;

/**
 *  Just a few examples of fetching info from NDR. Approach:
 *  <ol>
 *    <li> first get the MetadataProviderReader (which is the object type in the
 *    NDR from which collection information is obtained.</li>
 *    <li> then using the list of metadataHandles obtained from the mdp, get the
 *    item-level records (not a speedy operation)</li>
 *  </ol>
 *
 *
 * @author     Jonathan Ostwald
 * @version    $Id: DleseAsUseCaseHelper.java,v 1.4 2007/12/04 20:24:15 ostwald
 *      Exp $
 */
public class DleseAsUseCaseHelper {
	private static boolean debug = true;


	/**
	 *  Gets a metadataProviderReader given a collection key.<p>
	 *
	 *  The key for MY NASA DATA collection in ndrtest is "1123828776963".
	 *
	 * @param  collection  collection key
	 * @return             The metadataProvider value
	 */
	public static MetadataProviderReader getMetadataProvider(String collection) {
		try {
			return NdrUtils.getMetadataProvider(collection);
		} catch (Exception e) {
			prtln("could not get MDP for " + collection + ": " + e);
			e.printStackTrace();
			return null;
		}
	}


	/**
	 *  Gets the itemRecords for a given collection starting from the
	 *  MetadataProvider.
	 *
	 * @param  mdp  NOT YET DOCUMENTED
	 * @return      The itemRecords value
	 */
	public static List getItemRecords(MetadataProviderReader mdp) {
		List mdHandles = new ArrayList();
		String nativeFormat = mdp.getNativeFormat();
		try {
			mdHandles = mdp.getItemHandles();
		} catch (Exception e) {
			prtln("could not get itemHandles for " + mdp.getHandle());
		}
		List itemRecords = new ArrayList();
		/*
			iterate through metadata handles, grabbing itemRecords (from the datastream
			associated with the collection's "nativeFormat".
		*/
		for (Iterator i = mdHandles.iterator(); i.hasNext(); ) {
			String mdHandle = (String) i.next();
			try {
				MetadataReader mdReader = new MetadataReader(mdHandle, nativeFormat);
				Document itemRecord = mdReader.getItemRecord();
				itemRecords.add(itemRecord);
			} catch (Exception e) {
				prtln("could not get itemRecord for " + mdHandle + ": " + e);
			}
		}
		return itemRecords;
	}


	/**
	 *  Gets the handles of deleted metadata records for the specified collection.<p>Note:
	 only DLESE collections (written by the NCS) can be found by their collection key.
	 *
	 * @param  collection  DLESE collection kdy (e.g., "dcc")
	 * @return             List of handles for deleted item-level metadata records
	 */
	public static List getDeletedRecordHandles(String collection) {
		prtln("getDeletedRecordHandles");
		MetadataProviderReader mdpReader = getMetadataProvider(collection);
		if (mdpReader != null)
			return NdrUtils.findDeletedMetadataObjects(mdpReader.getHandle());
		else
			return new ArrayList();
	}

	/**
	 *  Alternate to using a props file to configure parameters to communicate with
	 *  an NDR instance
	 */
	static void ndrSetup() {
		String ndrApiBaseUrl = "";
		String ncsAgent = "";
		String keyFile = "";

		NdrUtils.setup(ndrApiBaseUrl, ncsAgent, keyFile);
	}


	/**
	 *  Examples of how to obtain DLESE info from NDR objects.
	 *
	 * @param  args           The command line arguments
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public static void main(String[] args) throws Exception {
		File propFile = null; // propFile must be assigned!
		propFile = new File ("C:/Documents and Settings/ostwald/devel/ndrServerProps/ndr.test.properties");
		NdrUtils.setup (propFile);
		NdrRequest.setVerbose(true);
		NdrRequest.setDebug(true);

		prtln ("Dlese as Use Case Helper");
		MetadataProviderReader mdpReader = null;

		int testcase = 1;

		if (testcase == 1) {
			// Get metadata for a DLESE Collection
			String collection = "1259607701181";
			mdpReader = getMetadataProvider(collection);
			prtln("MetadataProvider for " + collection);
			prtln("\t" + "collectionId: " + mdpReader.getCollectionId());
			prtln("\t" + "collectionName: " + mdpReader.getCollectionName());
			prtln("\t" + "nativeFormat: " + mdpReader.getNativeFormat());

			if (true) {
				prtln("-----------------\nCollection Record");
				NdrUtils.pp(mdpReader.getCollectionRecord());
			}
		}
		else if (testcase == 2) {
			// Get Metadata for an arbitrary (nsdl_dc) collection
			String mdpHandle = "2200/test.20061207181243233T";
			mdpReader = new MetadataProviderReader(mdpHandle);
			prtln("MetadataProvider handle " + mdpHandle);
			prtln("\t" + "setName: " + mdpReader.getSetName());
			prtln("\t" + "setSpec: " + mdpReader.getSetSpec());
			prtln("\t" + "nativeFormat: " + mdpReader.getNativeFormat());
		}

		prtln("\n------------\nthere are " + mdpReader.getItemHandles().size() + " handles");

		if (true) {
			prtln("\t fetching records ...");
			List itemRecords = getItemRecords(mdpReader);
			prtln("\n-- " + itemRecords.size() + " item records retrieved --");
			prtln("-----------------\nAn Item Record");
			if (itemRecords.isEmpty() != true)
				NdrUtils.pp((Document) itemRecords.get(0));
		}
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  s  NOT YET DOCUMENTED
	 */
	private static void prtln(String s) {
		String prefix = null;
		if (debug) {
			NdrUtils.prtln(s, prefix);
		}
	}

}

