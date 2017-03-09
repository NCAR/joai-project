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
package org.dlese.dpc.serviceclients.webclient;

import org.dlese.dpc.schemedit.test.TesterUtils;
import org.dlese.dpc.xml.Dom4jUtils;

import org.dom4j.Node;
import org.dom4j.Element;
import org.dom4j.Document;
import org.dom4j.DocumentException;

import java.io.File;
import java.util.*;

public class TestWebServiceClient {
	
	private static boolean debug = true;
	
	

	private WebServiceClient wsClient = null;
	
	public TestWebServiceClient(String baseWebServiceUrl) {
		// this.baseWebServiceUrl = baseWebServiceUrl;
		wsClient = new WebServiceClient (baseWebServiceUrl);
	}
	
	static void putRecordTest () {
		String baseWebServiceUrl = "http://dcs.dlese.org/roles/services/dcsws1-0";
		TestWebServiceClient tester = new TestWebServiceClient(baseWebServiceUrl);
		
		Document doc = null;
		String collection = "1153768875664";
		String xmlFormat = "dlese_anno";
		String status = "Fooberry";
		String statusNote = "This is but a test ..";
		String id = null;
		String recordsDir = "C:/Documents and Settings/ostwald/devel/records";
		String path = recordsDir + "/dlese_anno/1175109490577/TANNO-000-000-000-001.xml";
		try {
			doc = Dom4jUtils.getXmlDocument(new File (path));
			id = tester.wsClient.doPutRecord (doc.asXML(), xmlFormat, collection, status, statusNote);
		} catch (Exception e) {
			prtln ("putRecordTest error: " + e.getMessage());
		} catch (Throwable t) {
			prtln ("putRecordTest error: " + t.getMessage());
			t.printStackTrace();
		}
		prtln ("id: " + id);
	}
	
	static void getRecordTest () {
		String baseWebServiceUrl = "http://www.dlese.org/dds/services/ddsws1-1";
		TestWebServiceClient tester = new TestWebServiceClient(baseWebServiceUrl);
		String id = "DLESE-000-000-004-409";
		Document doc = null;
		Map props = new HashMap();
		try {
			GetRecordResponse response = tester.wsClient.getRecord(id);
			// doc = response.getDocument();
			doc = response.getItemRecord();
			prtln (pp(doc));
		} catch (Exception e) {
			prtln (e.getMessage());
			return;
		}
		
		String title_path = "/itemRecord/general/title";
		try {
			props.put ("title", ((Element)doc.selectSingleNode (title_path)).getText());
		} catch (Throwable t){}
		
		String id_path = "/itemRecord/metaMetadata/catalogEntries/catalog/@entry";
		try {
			props.put ("id", ((Node)doc.selectSingleNode (id_path)).getText());
		} catch (Throwable t){}
		
		prtln ("title: " + (String)props.get("title"));
		prtln ("id: " + (String)props.get("id"));
	}
			
			
		
	public static void main (String [] args) {
		
		
		TesterUtils.setSystemProps();
		
		prtln ("\n\n--------------------------------------------");
		prtln ("TestWebServiceClient \n");
		
		// String baseWebServiceUrl = "http://dcs.dlese.org/dds/services/ddsws1-0";
		// private String baseWebServiceUrl = "http://dcs.dlese.org/roles/services/dcsws1-0";
		// TestWebServiceClient tester = new TestWebServiceClient(baseWebServiceUrl);
		// putRecordTest();
		getRecordTest ();
	}
		
	private static String pp (Node node) {
		return Dom4jUtils.prettyPrint(node);
	}
	
	private static void prtln(String s) {
		if (debug) {
			org.dlese.dpc.schemedit.SchemEditUtils.prtln(s, "");
		}
	}
	
}
