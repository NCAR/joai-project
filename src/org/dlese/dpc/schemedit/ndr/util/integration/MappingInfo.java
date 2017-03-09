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
 *
 * @author    Jonathan Ostwald
 */
public class MappingInfo {
	private static boolean debug = true;

	private NCSInfo ncsInfo = null;
	private NDRInfo ndrInfo = null;
	private String id = null;
	private String aggregator = null;
	private List mismatches = null;
	private Element error = null;
	
	public static void main (String [] args ) throws Exception {
		CIGlobals.setup();
		MappingInfo info = getInstance ("NSDL-COLLECTION-475993", "2200/20061002125921403T");

		// MappingsManager mm = MappingsManager.getInstance();
		MappingsManager mm = new MappingsManager();
		mm.update (info);
	}
	
	public static MappingInfo getInstance (String id, String aggregator) throws Exception {
		// trim to avoid copy/paste errors
		Element mapping = MappingsData.makeMapping (id.trim(), aggregator.trim());
		return new MappingInfo (mapping);
	}
	
	public MappingInfo (Element mapping) throws Exception {
		id = mapping.attributeValue("id");
		aggregator = mapping.attributeValue("aggregator");
		ndrInfo = new NDRInfo (mapping.element("ndr"), aggregator);
		ncsInfo = new NCSInfo (mapping.element("ncs"), id);
		if (!this.getNcsTitle().equals(this.getNdrTitle()))
			addMismatch ("title");
		if (!this.getNcsResourceUrl().equals(this.getNdrResourceUrl())) {
			addMismatch ("resourceUrl");
		}
		if (!this.getAggregatorAgent().equals(this.getMetadataProviderAgent())) {
			prtln ("agent mismatch");
			prtln ("\t agent: \"" + this.getAggregatorAgent() + "\"");
			prtln ("\t mdp: \"" + this.getMetadataProviderAgent() + "\"");
			addMismatch ("agent");
		}
		this.error = mapping.element ("error");
	}
		
	void addMismatch (String s) {
		if (this.mismatches == null)
			this.mismatches = new ArrayList();
		this.mismatches.add (s);
	}
			
	public boolean getHasError () {
		return this.error != null;
	}
	
	public String getErrorMessage () {
		if (this.getHasError())
			return this.error.getTextTrim();
		else
			return "";
	}
	
	public List getMismatches () {
		return this.mismatches;
	}
		
	public String getId () {
		return this.id;
	}
	
	public String getItemcount () {
		return this.ndrInfo.itemcount;
	}
	
	public String getNcsTitle () {
		return this.ncsInfo.title;
	}
	
	public String getNcsResourceUrl () {
		return this.ncsInfo.resourceUrl;
	}
	
	public boolean getOaiIngest () {
		return (this.ncsInfo.oaiIngest == "true" ? true : false);
	}
	
	public String getNdrTitle () {
		return this.ndrInfo.title;
	}

	public String getNdrResourceUrl () {
		return this.ndrInfo.resourceUrl;
	}
	
	public String getAggregatorHandle () {
		return this.aggregator;
	}
	
	public String getAggregatorAgent () {
		return this.ndrInfo.aggagent;
	}
	
	public String getMetadataHandle () {
		return this.ndrInfo.metadata;
	}
	
	public String getMetadataProviderHandle () {
		return this.ndrInfo.metadataprovider;
	}
	
	public String getMetadataProviderAgent () {
		return this.ndrInfo.mdpagent;
	}
	
	public String getResourceHandle () {
		return this.ndrInfo.resource;
	}
	
	public Element toElement () {
		Element mapping = DocumentHelper.createElement ("mapping");
		mapping.addAttribute ("id", this.getId());
		mapping.addAttribute ("aggregator", this.getAggregatorHandle());
		
		Element ncs = mapping.addElement ("ncs");
		MappingsData.addChild (ncs, "title", this.getNcsTitle());
		MappingsData.addChild (ncs, "resourceUrl", this.getNcsResourceUrl());
		MappingsData.addChild (ncs, "oaiIngest", this.ncsInfo.oaiIngest);
		
		Element ndr = mapping.addElement ("ndr");
		MappingsData.addChild (ndr, "title", this.getNdrTitle());
		MappingsData.addChild (ndr, "resourceUrl", this.getNdrResourceUrl());
		MappingsData.addChild (ndr, "metadata", this.getMetadataHandle());
		Element mdp = MappingsData.addChild (ndr, "metadataprovider", this.getMetadataProviderHandle());
		mdp.addAttribute ("agent", this.getMetadataProviderAgent());
		Element agg = MappingsData.addChild (ndr, "aggregator", this.getAggregatorHandle());
		agg.addAttribute ("agent", this.getAggregatorAgent());
		MappingsData.addChild (ndr, "resource", this.getResourceHandle());
		MappingsData.addChild (ndr, "itemcount", this.getItemcount());
		
		if (this.getHasError())
			MappingsData.addChild(mapping, "error", this.getErrorMessage());
		
		return mapping;
	}
	
	class NCSInfo {
		String id;
		String title;
		String resourceUrl;
		String oaiIngest;
		
		NCSInfo (Element e, String id) {
			this.id = id;
			this.title = getElementText (e, "title");
			this.resourceUrl = getElementText (e, "resourceUrl");
			this.oaiIngest = getElementText (e, "oaiIngest");
		}
	}
	
	class NDRInfo {
		String aggregator;
		String title;
		String resourceUrl;
		String metadata;
		String metadataprovider;
		String resource;
		String itemcount;
		String aggagent;
		String mdpagent;
		
		NDRInfo (Element e, String aggregator) {
			this.aggregator = aggregator;
			this.title = getElementText (e, "title");
			this.resourceUrl = getElementText (e, "resourceUrl");
			this.metadata = getElementText (e, "metadata");
			this.metadataprovider = getElementText (e, "metadataprovider");
			this.resource = getElementText (e, "resource");
			this.itemcount = getElementText (e, "itemcount");
/* 			this.aggagent = e.element("aggregator").attributeValue("agent");
			this.mdpagent = e.element("metadataprovider").attributeValue("agent"); */
			this.aggagent = getChildAttributeValue (e, "aggregator", "agent");
			this.mdpagent = getChildAttributeValue (e, "metadataprovider", "agent");
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
		return FindAndReplace.replace (text, "&amp;", "&", false);
	}
	
	public static String getChildAttributeValue(Element parent, String child, String attName) {

		String text = "";
		try {
			text = parent.element(child).attributeValue(attName).trim();
		} catch (Throwable t) {}
		return FindAndReplace.replace (text, "&amp;", "&", false);
	}
	
}
