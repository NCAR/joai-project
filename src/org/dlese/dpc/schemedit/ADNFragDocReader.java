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
package org.dlese.dpc.schemedit;

import org.dlese.dpc.serviceclients.remotesearch.reader.*;
import org.dlese.dpc.serviceclients.webclient.*;
import org.dlese.dpc.xml.*;

import java.io.*;
import java.util.*;

import org.dom4j.Node;
import org.dom4j.Element;
import org.dom4j.Document;
import org.dom4j.DocumentFactory;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;

import org.dlese.dpc.index.ResultDoc;

public class ADNFragDocReader extends ADNItemDocReader {

	protected static boolean debug = true;
	protected String id = "";
	protected String collection = "";
	protected String readerType = "ADNFragDocReader";
	//String content = null;
	protected String [] EMPTY_ARRAY = new String[]{};
	protected String DEFAULT = "-- unspecified --";
	protected ArrayList EMPTY_LIST = new ArrayList();

	
	/**
	 * ADNFragDocReader constructor requiring an itemRecordDoc
	 */
	public ADNFragDocReader (String id, String collection, Document itemRecordDoc) {
		super (id, collection, itemRecordDoc, null);  // vocab=null;
		doc = Dom4jUtils.localizeXml(doc, "itemRecord");
		if (doc == null) {
			prtln (" constructor: doc never set");
		}
		else {
			// prtln (Dom4jUtils.prettyPrint (doc));
		}
			
	}
		
	// ----------------------- begin getters --------------------
	
	public String getMetadataPrefix() {
		return "adn_frag";
	}
	
	public static void prtln (String s) {
		if (debug)
			System.out.println("ADNFragDocReader: " + s);
	}
	

	
}
