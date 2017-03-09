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
package org.dlese.dpc.serviceclients.cat;

import org.dlese.dpc.schemedit.SchemEditUtils;
import org.dlese.dpc.xml.Dom4jUtils;
import org.dlese.dpc.util.TimedURLConnection;
import org.dlese.dpc.util.URLConnectionTimedOutException;
import org.dom4j.*;
import java.util.*;
import java.net.*;

/**
 * @author    Jonathan Ostwald
 */
public class CATServiceToolkit {

	private static boolean debug = true;
	static final String DEFAULT_USERNAME = "nsdl";
	static final String DEFAULT_PASSWORD = "digi!lib";
	static final String DEFAULT_BASE_URL = "http://grace.syr.edu:8080/casaa/service.do";
	
	private String username;
	private String password;
	private String baseUrl;
	
	public CATServiceToolkit() {
		this (DEFAULT_USERNAME, DEFAULT_PASSWORD, DEFAULT_BASE_URL);
	}
	
	/**
	 *  Constructor for the CATServiceToolkit object
	 *
	 * @param  username  username for authenticating to the CAT Web Service
	 * @param  password  password for authenticating to the CAT Web Service
	 */
	public CATServiceToolkit(String username, String password, String baseUrl) {
		this.username = username;
		this.password = password;
		this.baseUrl = baseUrl;
	}

	public Map getAllCatDocs () throws Exception {
		GetAllStandardsDocuments client = new GetAllStandardsDocuments (this.username, this.password, this.baseUrl);
		return client.getAllDocsMap();
	}
		
	public List getSuggestions (CATRequestConstraints constraints) throws Exception {
		SuggestStandards client = new SuggestStandards ();
		return client.getSuggestions(constraints);
	}
	
	/**
	 *  Description of the Method
	 *
	 * @param  node  Description of the Parameter
	 */
	protected static void pp(Node node) {
		prtln(Dom4jUtils.prettyPrint(node));
	}


	/**
	 *  Description of the Method
	 *
	 * @param  s  Description of the Parameter
	 */
	private static void prtln(String s) {
		String prefix = "CATServiceToolkit";
		if (debug) {
			SchemEditUtils.prtln(s, prefix);
		}
	}

}

