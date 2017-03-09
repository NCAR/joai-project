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

import org.dlese.dpc.ndr.apiproxy.*;
import org.dlese.dpc.ndr.NdrUtils;
import org.dlese.dpc.ndr.reader.*;
import org.dlese.dpc.ndr.request.*;
import org.dlese.dpc.xml.Dom4jUtils;
import org.dlese.dpc.util.Files;
import org.dlese.dpc.util.strings.FindAndReplace;
import org.dom4j.*;
import java.util.*;
import java.io.File;

/**
 *  NOT YET DOCUMENTED
 *
 * @author    Jonathan Ostwald
 */
public class NDRCollectionUtils {
	private static boolean debug = true;

	// file to which output is written
	static String logpath = "c:/tmp/ndrProductionCollections.txt";


	/**
	 *  Reports on collections controlled by the DLESE_TEST_AGENT
	 *
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	static void reportDleseTestCollections() throws Exception {
		reportCollections(NdrUtils.getAggregatorHandles(NDRConstants.DLESE_TEST_AGENT));
	}


	/**
	 *  Reports on collections controlled by the currently configured NCS Agent
	 *
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	static void reportCollections() throws Exception {
		FindRequest request = new FindRequest();
		request.setObjectType(NDRConstants.NDRObjectType.AGGREGATOR);
		request.addCommand("relationship", "memberOf", NDRConstants.getMasterAgent());
		reportCollections(request.getResultHandles());
	}


	/**
	 *  Reports on the provided aggregator handles (each of which represents a
	 *  collection.
	 *
	 * @param  aggHandles     NOT YET DOCUMENTED
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	static void reportCollections(List aggHandles) throws Exception {
		File logfile = new File(logpath);
		if (logfile.exists())
			logfile.delete();

		if (aggHandles == null) {
			prtln("No aggregators found");
			return;
		}

		prtln(aggHandles.size() + " aggregators found");
		int end = 1000;
		int start = 0;
		for (int i = start; i < aggHandles.size() && i < end; i++) {
			String aggHandle = (String) aggHandles.get(i);
			if (i < start)
				continue;
			// prtln (i + "/" + aggHandles.size() + "  " + aggHandle);
			prtln(i + "/" + aggHandles.size());
			try {
				NDRCollectionReader coll = new NDRCollectionReader(aggHandle);
				log(getRecord(coll));
				/* 				if (!coll.aggHandle.equals ("2200/test.20071205133522873T")) {
					log (getRecord(coll));
					deleteCollection (coll.mdpHandle);
				} */
			} catch (Exception e) {
				String msg = "error reading collection for " + aggHandle + ": " + e.getMessage();
				log("ERROR\t" + aggHandle + "\t" + e.getMessage());
				prtln(msg);
				e.printStackTrace();
			}
		}
	}


	/**
	 *  Deletes NDR objects (CAREFUL!)related to each of the provided aggregator
	 *  handles.
	 *
	 * @param  aggHandles     NOT YET DOCUMENTED
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	static void cullCollections(List aggHandles) throws Exception {

		if (aggHandles == null) {
			prtln("No aggregators found");
			return;
		}

		prtln(aggHandles.size() + " aggregators found");
		int end = 1000;
		int start = 0;
		prtln("aggregators to cull");
		for (int i = start; i < aggHandles.size() && i < end; i++) {
			String aggHandle = (String) aggHandles.get(i);
			if (i < start)
				continue;
			try {
				NDRCollectionReader coll = new NDRCollectionReader(aggHandle);
				if (coll.resourceCount == 0) {
					prtln("\tagg: " + coll.aggHandle + "   mdp: " + coll.mdpHandle);
					// NdrUtils.deleteNDRCollection(coll.mdpHandle);
					NdrUtils.deleteNDRObject(coll.aggHandle);
				}
			} catch (Exception e) {
				prtln("error reading collection for " + aggHandle + ": " + e.getMessage());
				e.printStackTrace();
			}
		}
	}


	/**
	 *  Replaces tabs and newlines in the given string
	 *
	 * @param  s  NOT YET DOCUMENTED
	 * @return    NOT YET DOCUMENTED
	 */
	static String flatten(String s) {
		if (s == null) {
			return "";
		}
		String ret = FindAndReplace.replace(s, "\n", " ", true);
		ret = FindAndReplace.replace(ret, "\t", " ", true);
		int i = 0;
		while (ret.indexOf("  ") > -1) {
			if (i++ > 10)
				break;
			ret = FindAndReplace.replace(ret, "  ", " ", true);
		}
		return ret;
	}


	/**
	 *  Creates a report record for the provided collection
	 *
	 * @param  coll  NOT YET DOCUMENTED
	 * @return       The record value
	 */
	static String getRecord(NDRCollectionReader coll) {
		List fields = new ArrayList();
		fields.add(flatten(coll.title));
		fields.add(coll.aggHandle);
		fields.add(String.valueOf(coll.resourceCount));
		fields.add(flatten(coll.setName));
		fields.add(coll.setSpec);
		fields.add(coll.mdpHandle);
		fields.add(String.valueOf(coll.metadataCount));
		return join(fields);
	}


	/**
	 *  Gets the header attribute of the NDRCollectionUtils class
	 *
	 * @return    The header value
	 */
	static String getHeader() {
		List fields = new ArrayList();
		fields.add("title");
		fields.add("aggHandle");
		fields.add("resourceCount");
		fields.add("setName");
		fields.add("setSpec");
		fields.add("mdpHandle");
		fields.add("metadataCount");
		return join(fields);
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  list  NOT YET DOCUMENTED
	 * @return       NOT YET DOCUMENTED
	 */
	static String join(List list) {
		String row = "";

		for (Iterator i = list.iterator(); i.hasNext(); ) {
			String val = (String) i.next();
			if (val == null)
				row += "";
			else
				row += val.trim();
			if (i.hasNext())
				row += "\t";
		}
		return row;
	}


	/**
	 *  Writes string to log file
	 *
	 * @param  s  NOT YET DOCUMENTED
	 */
	static void log(String s) {
		prtln(s);
		File logfile = new File(logpath);
		if (!logfile.exists()) {
			try {
				Files.writeFile(getHeader() + "\n", logfile);
			} catch (Exception e) {
				prtln("could not initialize log: " + e);
				return;
			}
		}
		try {
			StringBuffer sb = Files.readFile(logpath);
			sb.append(s + "\n");
			Files.writeFile(sb, logpath);
		} catch (Exception e) {
			prtln("log error: " + e);
		}
	}


	/**
	 *  Makes a report line for provided metadata provider
	 *
	 * @param  mdp            NOT YET DOCUMENTED
	 * @return                NOT YET DOCUMENTED
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public static String mdpToString(MetadataProviderReader mdp) throws Exception {
		List fields = new ArrayList();
		fields.add(mdp.getHandle());
		fields.add(String.valueOf(mdp.getMemberCount()));
		fields.add(mdp.getCollectionName());
		fields.add(mdp.getPropertyValues("setSpec").toString());

		return "\t" + join(fields);
	}


	/**
	 *  Reports on all metadata providers for currently configured NCS Agent
	 *
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	static void reportMetadataProviders() throws Exception {
		FindRequest request = new FindRequest();
		request.setObjectType(NDRConstants.NDRObjectType.METADATAPROVIDER);
		reportMetadataProviders(request.getResultHandles());
	}


	/**
	 *  Reports on provided list of mdp handles.
	 *
	 * @param  mdpHandles     NOT YET DOCUMENTED
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public static void reportMetadataProviders(List mdpHandles) throws Exception {
		prtln("\nMetadataProviders");
		if (mdpHandles != null) {
			for (Iterator i = mdpHandles.iterator(); i.hasNext(); )
				try {
					MetadataProviderReader mdp = new MetadataProviderReader((String) i.next());
					prtln(mdpToString(mdp));

					/* ModifyMetadataProviderRequest request = new ModifyMetadataProviderRequest(mdp.getHandle());
				request.addCommand ("property", "replace", "null", "delete");
				request.submit ();
				*/
					// deleteCollection (mdp.getHandle());

				} catch (Throwable t) {
					prtln(t.getMessage());
				}
		}
	}


	/**
	 *  Deletes all objects associated with provided mdpHandle, including
	 *  aggregator and metadata objects.
	 *
	 * @param  mdpHandle  NOT YET DOCUMENTED
	 */
	public static void deleteCollection(String mdpHandle) {
		try {
			NdrUtils.deleteNDRCollection(mdpHandle);
		} catch (Exception e) {
			prtln("deleteCollection error: " + e.getMessage());
		}
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
	 *  The main program for the NDRCollectionUtils class
	 *
	 * @param  args           The command line arguments
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public static void main(String[] args) throws Exception {

		File propFile = null; // propFile must be assigned!
		NdrUtils.setup(propFile);

		boolean verbosity = false;
		NdrRequest.setDebug(verbosity);
		NdrRequest.setVerbose(verbosity);

		String agent;
		// agent = NDRConstants.DLESE_TEST_AGENT;
		// agent = NDRConstants.NCS_TEST_AGENT;
		agent = NDRConstants.getNcsAgent();
		prtln("agent: " + agent);

		List mdps = NdrUtils.getMDPHandles(agent);
		reportMetadataProviders(mdps);

		// List aggs = NdrUtils.getAggregatorHandles(agent);
		// reportCollections(aggs);
		// reportCollections();

		// deleteCollection ("2200/test.20080131200537169T");
		// reportMetadataProviders();
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

