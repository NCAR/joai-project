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
 *  See
 *
 * @author    Jonathan Ostwald
 */
public class GetAllStandardsDocuments extends CATWebService {

	private static boolean debug = true;
	private final static String METHOD = "getAllStandardDocuments";

	/**  Constructor for the GetAllStandardsDocuments object */
 	public GetAllStandardsDocuments() {
		super ();
	}

	/**
	 *  Constructor for the GetAllStandardsDocuments object
	 *
	 * @param  username  NOT YET DOCUMENTED
	 // * @param  password  NOT YET DOCUMENTED
	 */
 	public GetAllStandardsDocuments(String username, String password, String baseUrl) {
		super(username, password, baseUrl);
	}

	protected String getMethod () {
		return METHOD;
	}
	
	/**
	 *  Gets the suggestions attribute of the GetAllStandardsDocuments object
	 *
	 * @param  resourceUrl    NOT YET DOCUMENTED
	 * @param  constraints    NOT YET DOCUMENTED
	 * @return                The suggestions value
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public List getAllDocs() throws Exception {
		
		Map args = new HashMap();
		args.put("username", this.username);
		args.put("password", this.password);
		args.put("method", this.getMethod ());
		
		Document response = null;
		try {
			response = this.getResponse(args);
		} catch (Throwable t) {
			throw new Exception("webservice error: " + t);
		}

		// DEBUGGING - show response
		/* pp (response.selectSingleNode("//RequestInfo")); */
		List results = new ArrayList();
		List resultNodes = response.selectNodes("/CATWebService/StandardDocuments/StandardDocument");
		for (Iterator i = resultNodes.iterator(); i.hasNext(); ) {
			Element e = (Element) i.next();
			results.add(new CATStandardDocument(e));
		}
		return results;
	}

	/**
	 *  Gets the suggestions attribute of the GetAllStandardsDocuments object
	 *
	 * @param  resourceUrl    NOT YET DOCUMENTED
	 * @param  constraints    NOT YET DOCUMENTED
	 * @return                The suggestions value
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public Map getAllDocsMap() throws Exception {
		
		Map args = new HashMap();
		args.put("username", this.username);
		args.put("password", this.password);
		args.put("method", this.getMethod ());
		
		Document response = null;
		try {
			response = this.getResponse(args);
		} catch (Throwable t) {
			throw new Exception("webservice error: " + t);
		}

		// DEBUGGING - show response
		/* pp (response.selectSingleNode("//RequestInfo")); */
		Map results = new HashMap();
		List resultNodes = response.selectNodes("/CATWebService/StandardDocuments/StandardDocument");
		for (Iterator i = resultNodes.iterator(); i.hasNext(); ) {
			Element e = (Element) i.next();
			CATStandardDocument stdDoc = new CATStandardDocument(e);
			results.put(stdDoc.getId(), stdDoc);
		}
		return results;
	}
	
	/**
	 *  Gets the resultIds attribute of the CATWebService class
	 *
	 * @param  results  NOT YET DOCUMENTED
	 * @return          The resultIds value
	 */
	static List getResultIds(List results) {
		prtln(results.size() + " suggestions returned");
		List ids = new ArrayList();
		for (Iterator i = results.iterator(); i.hasNext(); ) {
			CATStandardDocument stdDoc = (CATStandardDocument) i.next();
			String id = stdDoc.getId();
			ids.add(id);
			prtln("\t" + id);
		}
		return ids;
	}


	/**
	 *  Debugging Utility
	 *
	 * @param  results  NOT YET DOCUMENTED
	 */
	static void showTopics(List results) {
		List topics = new ArrayList();
		for (Iterator i = results.iterator(); i.hasNext(); ) {
			CATStandardDocument result = (CATStandardDocument) i.next();
			String topic = result.getTopic();
			if (!topics.contains(topic))
				topics.add(topic);
		}
		prtln("Topics");
		for (Iterator i = topics.iterator(); i.hasNext(); )
			prtln("\t" + (String) i.next());
	}

	static void showResults(List results) {
		prtln ("\nAll Standards Documents (" + results.size() + ")");
		for (Iterator i = results.iterator(); i.hasNext(); ) {
			CATStandardDocument result = (CATStandardDocument) i.next();
			prtln ("\n" + result.toString());
		}
	}
	
	/**
	 *  The main program for the GetAllStandardsDocuments class
	 *
	 * @param  args  The command line arguments
	 */
	public static void main(String[] args) {

 		// GetAllStandardsDocuments client = new GetAllStandardsDocuments("dlese", "p");
 		GetAllStandardsDocuments client = new GetAllStandardsDocuments();

		List results = null;
		try {
			results = client.getAllDocs();
		} catch (Throwable t) {
			prtln("ERROR: " + t.getMessage());
			System.exit(1);
		}
		prtln(results.size() + " results found");
		showResults(results);
	}


	/**
	 *  Description of the Method
	 *
	 * @param  s  Description of the Parameter
	 */
	private static void prtln(String s) {
		String prefix = null;
		if (debug) {
			SchemEditUtils.prtln(s, prefix);
		}
	}

}

