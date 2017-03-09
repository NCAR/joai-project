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
package org.dlese.dpc.schemedit.ndr.util;

import org.dlese.dpc.ndr.NdrUtils;
import org.dlese.dpc.ndr.reader.*;
import org.dlese.dpc.ndr.request.*;
import org.dlese.dpc.ndr.apiproxy.InfoXML;
import org.dlese.dpc.ndr.apiproxy.NDRConstants;
import org.dlese.dpc.ndr.apiproxy.NDRConstants.NDRObjectType;
import org.dlese.dpc.schemedit.SchemEditUtils;
import org.dlese.dpc.schemedit.test.TesterUtils;
import org.dlese.dpc.schemedit.config.CollectionConfigReader;
import org.dlese.dpc.xml.Dom4jUtils;
import org.dlese.dpc.util.Files;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.XPath;
import java.io.File;
import java.net.URL;
import java.util.*;
import java.text.*;

/**
 *  Utilities for cleaning up NDR test repository.
 *
 * @author     ostwald<p>
 *
 *      $Id $
 * @version    $Id: DevelTools.java,v 1.9 2009/03/20 23:33:56 jweather Exp $
 */
public class DevelTools {

	private static boolean debug = true;
	/**  NOT YET DOCUMENTED */
	public final static String ndrHost = "ndrtest.nsdl.org";
	/**  NOT YET DOCUMENTED */
	public final static String apiUrl = "http://" + ndrHost + "/api";


	/**
	 *  Utility for cleaning up - Delete all metadata records that d0 NOT belong to
	 *  collections modified since the threshold date. <p>
	 *
	 *  <pre>
	 *  - for each MDP
	 *  -- if fresh (last modified AFTER threshold)
	 *  --- add MD handles to "keepers"
	 *  - for each MDP
	 *  --- if stale (last modified BEFORE threshold)
	 *  ---- for each MD
	 *  ----- if not in "keepers", delete MD
	 *  </pre>
	 *
	 * @param  thresholdDateStr  NOT YET DOCUMENTED
	 * @exception  Exception     NOT YET DOCUMENTED
	 */
	public static void purge(String thresholdDateStr) throws Exception {
		Date thresholdDate = NdrUtils.parseSimpleDateString(thresholdDateStr);
		List keepers = new ArrayList();
		List mdpHandles = NdrUtils.getMDPHandles();
		List mdpReaders = new ArrayList();

		prtln("\n getting MDP objects from handles");
		for (Iterator i = mdpHandles.iterator(); i.hasNext(); ) {
			System.out.print(".");
			mdpReaders.add(new MetadataProviderReader((String) i.next()));
		}
		prtln("\n" + mdpReaders.size() + " MDP found in NDR");

		prtln("\t\t ... finding keepers ...");
		for (Iterator i = mdpReaders.iterator(); i.hasNext(); ) {
			System.out.print(".");
			MetadataProviderReader mdp = (MetadataProviderReader) i.next();

			if (!mdp.getLastModified().after(thresholdDate))
				continue;
			keepers.addAll(mdp.getItemHandles());
		}
		prtln("\nKeepers");
		for (Iterator i = keepers.iterator(); i.hasNext(); )
			prtln("\t" + (String) i.next());

		prtln("\t\t ... deleting trash ...");
		List trashItems = new ArrayList();
		for (Iterator i = mdpReaders.iterator(); i.hasNext(); ) {
			System.out.print(".");
			MetadataProviderReader mdp = (MetadataProviderReader) i.next();
			if (!mdp.getLastModified().before(thresholdDate))
				continue;
			List itemHandles = mdp.getItemHandles();
			for (Iterator ii = itemHandles.iterator(); ii.hasNext(); ) {
				String h = (String) ii.next();
				if (keepers.contains(h))
					prtln(h + " is a keeper!");
				else {
					if (trashItems.contains(h))
						prtln(h + " is already in trash");
					else {
						trashItems.add(h);
						try {
							NdrUtils.deleteNDRObject(h);
						} catch (Exception e) {
							prtln("delete error: " + e.getMessage());
						}
					}
				}
			}
		}

		prtln("\nTrash");
		for (Iterator i = trashItems.iterator(); i.hasNext(); )
			prtln("\t" + (String) i.next());

	}

	/**
	* This call does not seem to be working??
	*/
	public static void undeleteNDRObject (String handle, NDRObjectType objectType) throws Exception {
		NdrRequest request = new NdrRequest ();
		request.setRequestAgent(handle);
		request.setObjectType(objectType);
		request.setHandle (handle);
		request.setVerb ("modify" + objectType.getNdrResponseType());
		request.addQualifiedCommand (NDRConstants.FEDORA_MODEL_NAMESPACE, "property", "state", "Active");
		request.submit();
	}
	
	public static void addAgent (String id) throws Exception {
		NdrRequest request = new NdrRequest ();
		request.setObjectType(NDRObjectType.AGENT);
		request.setVerb ("addAgent");
		
		Element identifier = DocumentHelper.createElement("identifier");
		identifier.setText (id);
		identifier.addAttribute ("type", "HOST");
		request.addCommand ("property", identifier);
		request.addCommand ("relationship", "memberOf", "2200/test.20070806193858997T");
		String title = "Agent for mud collection";
		String description = "its all about mud";
		String subject = "none that i can see";
		NsdlDcWriter dc_stream = new NsdlDcWriter(title, description, subject);
		
		request.addDCStreamCmd(dc_stream.asElement());
		
		request.submit();
	}
		
	public static void modififyAgent () throws Exception {
		String handle = "2200/test.20070806191736386T";
		NdrRequest request = new NdrRequest ();
		request.setObjectType(NDRObjectType.AGENT);
		request.setVerb ("modifyAgent");
		request.setHandle(handle);
		
		request.addCommand ("relationship", "memberOf", "2200/NSDL_Agents_Collection");
		request.submit();
	}
	
	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public static void modifyMetadataProvider() throws Exception {
		String handle = "2200/test.20070806114624011T";
		NdrRequest request = new NdrRequest();
		request.setHandle(handle);
		request.setObjectType(NDRObjectType.METADATAPROVIDER);
		request.setVerb("modifyMetadataProvider");
		// request.addCommand ("relationship", "aggregatedBy", "2200/test.20070726184936697T", "delete");
		request.addCommand("relationship", "metadataProviderFor", "2200/test.20070806194051429T");
		request.submit();
	}
	
	public static String findCollectionMetadata (String collectionAggregator) throws Exception {
		FindRequest request = new FindRequest (NDRObjectType.METADATA);
		// request.addCommand("relationship", "metadataFor", collectionAggregator);
		request.addCommand("relationship", "authorizedToChange", "2200/test.20060829130238941T");
		return request.getResultHandle();
	}
		
	public static List findNCSApplicationAgents () {
		
		FindRequest request = new FindRequest (NDRObjectType.AGENT);
		request.addNcsPropertyCmd("isNCSApplication", "true");
		// request.addCommand("property", "ncs:isNCSApplication", "true");
		// request.addCommand("property", "hasResourceHOST", "ncs.nsdl.org");
		List agents = null;
		try {
			agents = request.getResultHandles();
		} catch (Exception e) {
			prtln ("findNCSApplicationAgents error: " + e);
		}
		return (agents != null ? agents : new ArrayList());
	}
	
	public static String getCollectionMetadataItemId () {
		String mdHandle = "2200/test.20070809191627698T"; // item record in mud
		String aggHandle = "2200/test.20070803205249015T"; // aggregator for New Item collection
		String xmlFormat = "ncs_collect";
 		String link = null;
		try {
			FindRequest request = new FindRequest (NDRObjectType.METADATA);
			request.addCommand ("relationship", "metadataFor", aggHandle);
			String aggMetadataHandle = request.getResultHandle();
			MetadataReader aggMetadata = new MetadataReader (aggMetadataHandle, xmlFormat);
			// finally, grab the itemId from the aggregator's MD
			link = aggMetadata.getProperty ("itemId");
		}
		catch (Throwable t) {
			prtln ("WARNING: could not construct infoStream link: " + t.getMessage());
		}
		return link;
	}
	
	
	/**
	 *  The main program for the DevelTools class
	 *
	 * @param  args           The command line arguments
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public static void main(String[] args) throws Exception {
		TesterUtils.setSystemProps();
/* 		NdrObjectReader.setDebug(false);
		setDebug(true);
		
		String aggHandle = "2200/test.20070726190044390T";
		prtln ("collectionMetadata: " + findCollectionMetadata (aggHandle)); */
		
		// addAgent ("ncs.mud.agent");
		// modififyAgent ();
		// modifyMetadataProvider();

		// getCollectionMetadataItemId ();
		 
		findNCSApplicationAgents ();
			

	}

	public static void displayStaleMDPs(String dateStr) {
		Date thresholdDate = NdrUtils.parseSimpleDateString(dateStr);
		List mdps = NdrUtils.getStaleMDPHandles(thresholdDate);
		prtln("MDPS lastModified before " + dateStr);
		for (Iterator i = mdps.iterator(); i.hasNext(); ) {
			MetadataProviderReader mdp = (MetadataProviderReader) i.next();
			prtln(mdp.getHandle() + "  (" + mdp.getLastModifiedDate() + ")");
		}
	}
	
	/**  NOT YET DOCUMENTED */
	public static void displayRecentMDPs() {
		String dateStr = "2007-05-20";
		Date thresholdDate = NdrUtils.parseSimpleDateString(dateStr);
		List mdps = NdrUtils.getRecentMDPHandles(thresholdDate);
		prtln("MDPS lastModified after " + dateStr);
		for (Iterator i = mdps.iterator(); i.hasNext(); ) {
			MetadataProviderReader mdp = (MetadataProviderReader) i.next();
			prtln(mdp.getHandle() + "  (" + mdp.getLastModifiedDate() + ")");
		}
	}

		/**
	 *  Display MDPs that have no items
	 *
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public static void displayEmptyMDPs() throws Exception {

		List mdps = getEmtpyMDPHandles();
		prtln("Empty MDPs (" + mdps.size() + ")");
		for (Iterator i = mdps.iterator(); i.hasNext(); ) {
			prtln("\t" + (String) i.next());
		}
	}

	public static List getEmtpyMDPHandles () throws Exception {
		List handles = NdrUtils.getMDPHandles ();
		List emptyHandles = new ArrayList();
		for (Iterator i=handles.iterator();i.hasNext();) {
			String mdpHandle = (String)i.next();
			// prtln ("proccessing " + mdpHandle);
			System.out.print (".");
			try {
				MetadataProviderReader mdp = new MetadataProviderReader (mdpHandle);
				List items = mdp.getItemHandles();
				// prtln ("\t" + items.size() + " items");
				if (items.size() == 0)
					emptyHandles.add (mdpHandle);
			} catch (Exception re) {
				prtln ("reader error: " + re.getMessage());
			}
		}
		return emptyHandles;
	}
	
	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public static void deleteEmptyMDPs() throws Exception {

		List mdps = NdrUtils.getEmtpyMDPHandles();
		prtln("Empty MDPs (" + mdps.size() + ")");
		for (Iterator i = mdps.iterator(); i.hasNext(); ) {
			String handle = (String) i.next();
			try {
				NdrUtils.deleteNDRObject(handle);
				prtln(handle + " deleted");
			} catch (Exception e) {
				prtln(handle + " NOT deleted: " + e.getMessage());
			}
		}
	}

	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  dateStr  NOT YET DOCUMENTED
	 */
	public static void deleteStaleMDPs(String dateStr) {
		Date thresholdDate = NdrUtils.parseSimpleDateString(dateStr);
		List mdps = NdrUtils.getStaleMDPHandles(thresholdDate);
		prtln("Deleting MDPS lastModified before " + dateStr);
		for (Iterator i = mdps.iterator(); i.hasNext(); ) {
			MetadataProviderReader mdp = (MetadataProviderReader) i.next();
			prtln(mdp.getHandle() + "  (" + mdp.getLastModifiedDate() + ")");
			try {
				NdrUtils.deleteNDRObject(mdp.getHandle());
			} catch (Exception e) {
				prtln("delete error: " + e.getMessage());
			}
		}
	}

	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  node  NOT YET DOCUMENTED
	 */
	private static void pp(Node node) {
		prtln(Dom4jUtils.prettyPrint(node));
	}


	/**
	 *  Sets the debug attribute of the DevelTools class
	 *
	 * @param  bool  The new debug value
	 */
	public static void setDebug(boolean bool) {
		debug = bool;
	}


	/**
	 *  Description of the Method
	 *
	 * @param  s  Description of the Parameter
	 */
	private static void prtln(String s) {
		if (debug) {
			// SchemEditUtils.prtln(s, "DevelTools");
			SchemEditUtils.prtln(s, "");
		}
	}

}

