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
public class SuggestStandards extends CATWebService {

	private static boolean debug = true;
	private final static String METHOD = "suggestStandards";


	/**  Constructor for the SuggestStandards object */
	public SuggestStandards() {
		super();
	}


	/**
	 *  Constructor for the SuggestStandards object supplying username, password and baseUrl
	 *
	 * @param  username  the client username
	 * @param  password  the client password
	 * @param  baseUrl   the service baseUrl
	 */
	public SuggestStandards(String username, String password, String baseUrl) {
		super(username, password, baseUrl);
	}


	/*
	** The method name for this service client: "suggestStandards"
	*/
	protected String getMethod() {
		return METHOD;
	}


	/**
	 *  Gets the suggestions attribute of the SuggestStandards object
	 *
	 * @param  constraints    search request contraints
	 * @return                List of suggestions from CAT
	 * @exception  Exception  if request is not successful
	 */
	public List getSuggestions(CATRequestConstraints constraints) throws Exception {

		Document response = null;
		try {
			response = this.getResponse(constraints);
		} catch (Throwable t) {
			throw new Exception("webservice error: " + t.getMessage());
		}

		// DEBUGGING - show response
		/* pp (response.selectSingleNode("//RequestInfo")); */
		List results = new ArrayList();
		List resultNodes = response.selectNodes("/CATWebService/SuggestedStandards/Results/Result/Standard");
		for (Iterator i = resultNodes.iterator(); i.hasNext(); ) {
			Element e = (Element) i.next();
			results.add(new CATStandard(e));
		}
		return results;
	}


	/**
	 * @param  resourceUrl    url for resource for which suggestions are made
	 * @param  optionalArgs   additional search contraints
	 * @return                suggested stds from CAT
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public List getSuggestions(String resourceUrl, Map optionalArgs) throws Exception {
		Map args = null;
		if (optionalArgs != null)
			args = optionalArgs;
		else
			args = new HashMap();
		args.put("username", this.username);
		args.put("password", this.password);
		args.put("query", resourceUrl);
		args.put("method", METHOD);

		Document response = null;
		try {
			response = this.getResponse(args);
		} catch (Throwable t) {
			throw new Exception("webservice error: " + t.getMessage());
		}

		// DEBUGGING - show response
		/* pp (response.selectSingleNode("//RequestInfo")); */
		List results = new ArrayList();
		List resultNodes = response.selectNodes("/CATWebService/SuggestedStandards/Results/Result/Standard");
		for (Iterator i = resultNodes.iterator(); i.hasNext(); ) {
			Element e = (Element) i.next();
			results.add(new CATStandard(e));
		}
		return results;
	}


	/**
	 *  Extracts the resultIds from a list of CATStandard instances created from CAT response
	 *
	 * @param  results  list of CATStandards
	 * @return          The resultIds value
	 */
	static List getResultIds(List results) {
		prtln(results.size() + " suggestions returned");
		List ids = new ArrayList();
		for (Iterator i = results.iterator(); i.hasNext(); ) {
			CATStandard std = (CATStandard) i.next();
			String id = std.getIdentifier();
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
	static void showGradeLevels(List results) {
		List gradeLevels = new ArrayList();
		for (Iterator i = results.iterator(); i.hasNext(); ) {
			CATStandard result = (CATStandard) i.next();
			String gr = result.getGradeLevels();
			if (!gradeLevels.contains(gr))
				gradeLevels.add(gr);
		}
		prtln("GradeLevels");
		for (Iterator i = gradeLevels.iterator(); i.hasNext(); )
			prtln("\t" + (String) i.next());
	}


	/**
	 *  Debugging Utility
	 *
	 * @param  results  NOT YET DOCUMENTED
	 */
	static void showTopics(List results) {
		List topics = new ArrayList();
		for (Iterator i = results.iterator(); i.hasNext(); ) {
			CATStandard result = (CATStandard) i.next();
			String topic = result.getTopic();
			if (!topics.contains(topic))
				topics.add(topic);
		}
		prtln("Topics");
		for (Iterator i = topics.iterator(); i.hasNext(); )
			prtln("\t" + (String) i.next());
	}


	/**
	 *  The main program for the SuggestStandards class
	 *
	 * @param  args  The command line arguments
	 */
	public static void main(String[] args) {

		SuggestStandards client = new SuggestStandards();
		String resourceUrl = "http://nsidc.org/snow/";

		CATRequestConstraints constraints = new CATRequestConstraints();
		constraints.setQuery(resourceUrl);
		constraints.setAuthor("National Science Education Standards (NSES)");
		constraints.setTopic("Science");
		constraints.setStartGrade(1);
		constraints.setEndGrade(12);
		constraints.setMaxResults(10);

		List results = null;
		try {
			results = client.getSuggestions(constraints);
		} catch (Throwable t) {
			prtln("ERROR: " + t.getMessage());
			System.exit(1);
		}
		prtln(results.size() + " results found");
		showGradeLevels(results);
		showTopics(results);
	}


	/**  Tester using old-style method of expressing parameters as a Map  */
	static void tester1() {
		SuggestStandards client = new SuggestStandards();
		String resourceUrl = "http://nsidc.org/snow/";

		Map optionalArgs = new HashMap();
		optionalArgs.put("author", "National Science Education Standards (NSES)");
		optionalArgs.put("topic", "Science");
		optionalArgs.put("startGrade", "10");
		optionalArgs.put("endGrade", "1");
		optionalArgs.put("maxResults", "10");

		List results = null;
		try {
			results = client.getSuggestions(resourceUrl, optionalArgs);
		} catch (Throwable t) {
			prtln("ERROR: " + t.getMessage());
			System.exit(1);
		}
		prtln(results.size() + " results found");
		showGradeLevels(results);
		showTopics(results);
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

