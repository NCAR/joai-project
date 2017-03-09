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
public class GetStandard extends CATWebService {

	private static boolean debug = true;
	private final static String METHOD = "getStandards";


	/**  Constructor for the GetStandard object */
	public GetStandard() {
		super();
	}


	/**
	 *  Constructor for the GetStandard object supplying username, password and
	 *  baseUrl
	 *
	 * @param  username  the client username
	 * @param  password  the client password
	 * @param  baseUrl   the service baseUrl
	 */
	public GetStandard(String username, String password, String baseUrl) {
		super(username, password, baseUrl);
	}

	/**
	* returns "getStandards"
	*/
	protected String getMethod() {
		return METHOD;
	}


	/**
	 * @param  identifier     id of standard to get
	 * @return                CATStandard instance for specified id
	 * @exception  Exception  
	 */
	public CATStandard get(String identifier) throws Exception {
		Map args = new HashMap();
		args.put("username", this.username);
		args.put("password", this.password);
		args.put("identifier", identifier);
		args.put("method", METHOD);

		Document response = null;
		try {
			response = this.getResponse(args);
		} catch (Throwable t) {
			throw new Exception("webservice error: " + t);
		}

		Element result = (Element) response.selectSingleNode("/CATWebService/Standards/Standard");
		return new CATStandard(result);
	}


	/**
	 *  The main program for the GetStandard class
	 *
	 * @param  args  The command line arguments
	 */
	public static void main(String[] args) {

		GetStandard client = new GetStandard();
		String identifier = "http://purl.org/ASN/resources/S100EAA1";

		CATStandard result = null;
		try {
			result = client.get(identifier);
		} catch (Throwable t) {
			prtln("ERROR: " + t.getMessage());
			System.exit(1);
		}
		prtln("fetched result id: " + result.getAsnId());
		prtln(Dom4jUtils.prettyPrint(result.getElement()));
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

