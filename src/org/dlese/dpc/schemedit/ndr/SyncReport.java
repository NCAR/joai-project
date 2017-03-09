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
package org.dlese.dpc.schemedit.ndr;

import org.dlese.dpc.schemedit.SchemEditUtils;
import org.dlese.dpc.schemedit.config.CollectionConfig;

import org.dlese.dpc.ndr.apiproxy.InfoXML;
import org.dlese.dpc.ndr.apiproxy.NDRConstants.NDRObjectType;

import org.dlese.dpc.xml.Dom4jUtils;

import java.text.SimpleDateFormat;
import java.util.*;
import java.io.File;


import org.dom4j.*;

public class SyncReport {
	private static boolean debug = false;
	
	Map entryMap = null;
	String collectionName;
	CollectionConfig collectionConfig;
	
	public SyncReport (CollectionConfig collectionConfig, String collectionName) {
		this.collectionName = collectionName;
		this.collectionConfig = collectionConfig;
		this.entryMap = new TreeMap ();
	}
	
/* 	public void addEntry (String id, String command, String resourceHandle, InfoXML response) {
		entryMap.put (id, new SyncReportEntry (id, command, resourceHandle, response));
	}
	
	public void addEntry (String id, String errorMsg) {
		entryMap.put (id, new SyncReportEntry (id, errorMsg));
	} */
	
	public void addEntry (ReportEntry entry) {
		entryMap.put (entry.getId(), entry);
	}
	
	public Map getEntries () {
		return this.entryMap;
	}
	
	public int size() {
		return this.entryMap.size();
	}
	
	public List getEntryList () {
		prtln ("getEntryList()");
		List entryList = new ArrayList();
		Set keys = getEntries().keySet();
		String key;
		
		key = NDRObjectType.AGGREGATOR.getNdrResponseType();
		prtln ("looking for " + key);
		if (keys.contains(key)) {
			prtln ("\t FOUND");
			entryList.add ((ReportEntry)getEntry (key));
			keys.remove (key);
		}
		else
			prtln ("\t not found");
		
		key = NDRObjectType.METADATAPROVIDER.getNdrResponseType();
		prtln ("looking for " + key);
		if (keys.contains(key)) {
			prtln ("\t FOUND");
			entryList.add ((ReportEntry)getEntry (key));
			keys.remove (key);
		}
		else
			prtln ("\t not found");
		
		String [] keyArray = (String [])keys.toArray (new String [0]);
		Arrays.sort (keyArray);
		for (int i=0;i<keyArray.length;i++) {
			entryList.add ((ReportEntry)getEntry (keyArray[i]));
		}
		
		return entryList;
	}
		
		
	
	public ReportEntry getEntry (String id) {
		return (ReportEntry)entryMap.get (id);
	}
	
	public String getCollectionName () {
		return collectionName;
	}
	
	public String collection () {
		return collectionConfig.getId();
	}
	
	public static void main (String [] args) throws Exception {
		
	}
	
	/**
	 *  Description of the Method
	 *
	 *@param  s  Description of the Parameter
	 */
	private static void prtln(String s) {
		if (debug)
			SchemEditUtils.prtln(s, "SyncReport");
	}
}
