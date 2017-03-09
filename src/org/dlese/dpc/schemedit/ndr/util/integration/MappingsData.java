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

import org.dlese.dpc.schemedit.ndr.util.*;
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
 * Reads an XML file containing mappings between ncsrecordid and aggregator handle, and produces
 	an XML file containing more detailed information about the NCS Collection record and the NDR Collection
	Objects associated with the aggregator.
 * @author    Jonathan Ostwald
 */
public class MappingsData {
	private static boolean debug = true;
	private List mappings = null;
	Document input = null;
	Document doc = null;
	String outpath; 
	
	static final  String COLLECTION_ADMIN_AGENT =
		"2200/NSDL_COLLECTION_ADMIN";
	
	public static void main(String[] args) throws Exception {
 		CIGlobals.setup();
		MappingsData mappings = new MappingsData();
		prtln ("wrote mappings data to " + mappings.outpath);
		
		// Element mapping = makeMapping ("NSDL-COLLECTION-2669651", "2200/20061129120245867T");
		// pp (mapping);

	}
	
	MappingsData () throws Exception {
		this(200);
	}
	
	MappingsData (int max) throws Exception {
		outpath = CIGlobals.MAPPINGS_MANAGER_DATA;
		String data = CIGlobals.COLLECTION_MAPPINGS_DIR + "Agg-ID-Mappings.xml";
		input = Dom4jUtils.getXmlDocument(new File (data));
		Element root = DocumentHelper.createElement ("mappings");
		root.addAttribute("ndrApiBaseUrl", NDRConstants.getNdrApiBaseUrl());
		root.addAttribute("timeStamp", SchemEditUtils.fullDateString(new Date()));
		
		doc = DocumentHelper.createDocument (root);
		
		populateOutput (max);
	}
	
	void write () throws Exception {
		Dom4jUtils.writePrettyDocToFile(this.doc, new File (outpath));
		// prtln ("wrote data to " + out);
	}
	
	void populateOutput (int max) {
		int count = 0;
		// int max = 2;
		List simplemappings = input.selectNodes ("/mappings/mapping");
		max = Math.min (max, simplemappings.size());
		prtln (simplemappings.size() + " AGG to ID mappings found - processing " + max);
		Element root = doc.getRootElement();
		for (Iterator i=simplemappings.iterator();i.hasNext();) {
			Element sm = (Element)i.next();
			if (++count > max) {
				prtln ("Max reached - quitting");
				break;
			}
			prtln (count + "/" + max);

			try {
				Element mapping = makeMapping (sm);
				root.add (mapping);
				if (mapping.element("error") != null) {
					// break;
				}
				write ();

			} catch (Exception e) {
				prtln ("makeMapping error: " + e.getMessage());
				// e.printStackTrace();
				break;
			}

		}
	}
			
	
	public static Element makeMapping (Element data)  {
		String aggregatorhandle = getElementText (data, "aggregatorhandle");
		String ncsrecordid = getElementText (data, "ncsrecordid");
		return makeMapping (ncsrecordid, aggregatorhandle);
	}
		
	public static Element makeMapping (String ncsrecordid, String aggregatorhandle) {
		Element mapping = DocumentHelper.createElement ("mapping");
		try {
			populateNCSInfo (mapping, ncsrecordid);
			populateNDRInfo (mapping, aggregatorhandle);
		} catch (Exception e) {
			addChild (mapping, "error", e.getMessage());
		}
		return mapping;
	}
	
	static void populateNCSInfo (Element element, String id) throws Exception {
		element.addAttribute("id", id);
		Element ncs = element.addElement ("ncs");
		// NCS Record Info
		NCSCollectReader reader = null;
		try {
			reader = NSDLCollectionUtils.getNCSRecord(id);
		} catch (Exception e) {
			throw new Exception ("could not get NCSRecord for " + id + ": " + e.getMessage());
		}
		
		addChild (ncs, "title", reader.getTitle());
		addChild (ncs, "resourceUrl", reader.getUrl());
		addChild (ncs, "oaiIngest", (reader.isOaiIngest() ? "true" : "false"));
	}
		
	static void populateNDRInfo (Element element, String aggHandle) throws Exception {
		element.addAttribute("aggregator", aggHandle);
		Element nsdl = element.addElement ("ndr");
		// NCS Record Info
		NSDLCollectionReader reader = null;
		try {
			reader = new NSDLCollectionReader(aggHandle);
		} catch (Exception e) {
			throw new Exception ("could not get NSDLCollectionReader for " + aggHandle + ": " + e.getMessage());
		}
		
		addChild (nsdl, "title", reader.getTitle());
		addChild (nsdl, "resourceUrl", reader.getResourceUrl());
		addChild (nsdl, "metadata", reader.metadata.getHandle());
		
		Element mdp = addChild (nsdl, "metadataprovider", reader.mdp.getHandle());
		boolean auth = reader.mdp.isAuthorizedToChange(COLLECTION_ADMIN_AGENT);
		mdp.addAttribute ("auth", (auth ? "true" : "false"));
		mdp.addAttribute("agent", reader.mdp.getRelationship("metadataProviderFor"));
		
		Element agg = addChild (nsdl, "aggregator", reader.aggregator.getHandle());
		auth = reader.aggregator.isAuthorizedToChange(COLLECTION_ADMIN_AGENT);
		agg.addAttribute ("auth", (auth ? "true" : "false"));
		agg.addAttribute("agent", reader.aggregator.getRelationship("aggregatorFor"));
		
		addChild (nsdl, "resource", reader.resource.getHandle());
		String itemcount = "not checked";
/* 		try {
			itemcount = Integer.toString(reader.mdp.getMemberCount());
		} catch (Throwable t) {} */
		addChild (nsdl, "itemcount", itemcount);
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

	public static String getElementText(Element e, String tag) {
		String text = "";
		try {
			text = e.element(tag).getTextTrim();
		} catch (Throwable t) {}
		return text;
	}
	
	public static Element addChild (Element parent, String tag, String text) {
		Element child = parent.addElement (tag);
		child.setText (text != null ? text : "");
		return child;
	}
}
