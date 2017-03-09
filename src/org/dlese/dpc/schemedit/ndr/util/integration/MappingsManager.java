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
 *
 * @author    Jonathan Ostwald
 */
public class MappingsManager {
	private static boolean debug = true;
	private List mappings = null;
	private Map idMap = null;
	private Map aggMap = null;
	private List mismatches = null;
	private List errors = null;
	
	private static MappingsManager instance;
	private String ndrServer = null;
	private String timeStamp = null;
	private File dataFile = null;
	
	// public static String dataPath = "H:/Documents/NDR/CollectionsIntegration/2008_03_11/CurrentMappingData.xml";
	// public static String dataPath = CIGlobals.MAPPINGS_MANAGER_DATA;
	
	public MappingsManager () throws Exception {
		this(CIGlobals.MAPPINGS_MANAGER_DATA);
	}
	
	public MappingsManager (String dataPath) throws Exception {
		prtln ("MappingsManager datapath: " + dataPath);
		// String data = "/Users/ostwald/Desktop/NCS/2008_03_10/MapperData.xml";
		dataFile = new File (dataPath);
		if (!dataFile.exists())
			throw new Exception ("dataFile does not exist at " + dataPath);

		init();
	}
	
/* 	public static MappingsManager getInstance () {
		if (instance == null) {
			try {
				instance = new MappingsManager();
			} catch (Exception e) {
				prtln ("MappingsManager init error: " + e.getMessage());
			}
		}
		return instance;
	} */
		
	public void reset () throws Exception {
		// this.instance = null;
		this.init();
	}
	
	public String getNdrServer () {
		return this.ndrServer;
	}
	
	public String getTimeStamp () {
		return timeStamp;
	}
	
	public List getMappings () {
		return this.mappings;
	}
	
	public MappingInfo getMappingInfo (String id) {
		return (MappingInfo) this.getIdMap().get (id);
	}
	
	public synchronized List getMismatches  () {
		if (this.mismatches == null) {
			this.mismatches = new ArrayList ();
			for (Iterator i=this.mappings.iterator();i.hasNext();) {
				MappingInfo info = (MappingInfo)i.next();
				if (info.getMismatches() != null)
					this.mismatches.add (info);
			}
		}
		
		return this.mismatches;
	}
	
	public synchronized List  getErrors() {
		if (this.errors == null) {
			this.errors = new ArrayList ();
			for (Iterator i=this.mappings.iterator();i.hasNext();) {
				MappingInfo info = (MappingInfo)i.next();
				if (info.getHasError())
					this.errors.add (info);
			}
		}
		return this.errors;
	}			
		
	
	public Map getIdMap () {
		return this.idMap;
	}
	
	public Map getAggMap () {
		return this.aggMap;
	}
	
	public String getDataFile () {
		return this.dataFile.getAbsolutePath();
	}
	
	public static void main(String[] args) throws Exception {
		CIGlobals.setup();
		// MappingsManager.dataPath = "H:/Documents/NDR/CollectionsIntegration/CollectionMappingData/server7MappingData.xml";
		
		String dataPath = CIGlobals.MAPPINGS_MANAGER_DATA;
		MappingsManager mm = new MappingsManager(dataPath);
		prtln ("mappings read: " + mm.mappings.size());
		
		prtln ("mismatches (" + mm.getMismatches().size() + ")");
		for (Iterator i=mm.getMismatches().iterator();i.hasNext();) {
			MappingInfo info = (MappingInfo)i.next();
			// prtln (info.getId() + ": " + info.getNcsTitle());
			prtln ("\t" + info.getId() + ": " + info.getMismatches());
		}
		
		prtln ("errors: (" + mm.getErrors().size() + ")");
		for (Iterator i=mm.getErrors().iterator();i.hasNext();) {
			MappingInfo info = (MappingInfo)i.next();
			prtln ("\t" + info.getId() + ": " + info.getErrorMessage());
		}
		
		// pp (mm.toDocument());
	}

	public void update (String id) {
		MappingInfo info = (MappingInfo)this.idMap.get (id);
		if (info == null)
			prtln ("update: mappingInfo not found for " + id);
		else
			update (info);
	}
	
	public void update (MappingInfo info) {
		try {
			MappingInfo updated = MappingInfo.getInstance(info.getId(), info.getAggregatorHandle());
			this.add (updated);
			Dom4jUtils.writePrettyDocToFile(this.toDocument(), this.dataFile);
		} catch (Throwable t) {
			prtln ("update error: " + t.getMessage());
		}
	}
	
	Document toDocument () {
		Element root = DocumentHelper.createElement("mappings");
		Document mydoc = DocumentHelper.createDocument (root);
		root.addAttribute("ndrServer", this.getNdrServer());
		root.addAttribute("timeStamp", SchemEditUtils.fullDateString(new Date()));
		for (Iterator i=this.mappings.iterator();i.hasNext();) {
			MappingInfo info = (MappingInfo)i.next();
			root.add (info.toElement());
		}
		return mydoc;
	}
	
	public void remove (MappingInfo existing) {
			mappings.remove(existing);
			idMap.remove (existing.getId());
			aggMap.remove (existing.getAggregatorHandle());
	}
	
	public synchronized void add (MappingInfo info) {
		String id = info.getId();
		MappingInfo existing = (MappingInfo)idMap.get (id);
		if (existing != null) {
			remove (existing);
		}
		mappings.add (info);
		idMap.put (id, info);
		aggMap.put (info.getAggregatorHandle(), info);
		this.mismatches = null;
		this.errors = null;
	}
	
	void init() throws Exception {
		Document doc = Dom4jUtils.getXmlDocument(dataFile);
		Element root = doc.getRootElement();
		ndrServer = root.attributeValue("ndrServer");
		timeStamp = root.attributeValue("timeStamp");
		int count = 0;
		int max = 200;
		idMap = new HashMap();
		aggMap = new HashMap();
		mappings = new ArrayList();
		List nodes = doc.selectNodes ("/mappings/mapping");
		for (Iterator i=nodes.iterator();i.hasNext();) {
			Element mapping = (Element)i.next();
			try {
				MappingInfo info = new MappingInfo (mapping);
				this.add (info);
/* 				mappings.add (info);
				idMap.put (info.getId(), info);
				aggMap.put (info.getAggregatorHandle(), info); */
			} catch (Exception e) {
				prtln ("Init error: " + e.getMessage());
				e.printStackTrace();
				pp (mapping);
				break;
			}
			count++;
			// prtln (count + "/" + max);
			if (count >= max)
				break;
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
		return text;
	}
	
	public static Element addChild (Element parent, String tag, String text) {
		Element child = parent.addElement (tag);
		child.setText (text != null ? text : "");
		return child;
	}
}
