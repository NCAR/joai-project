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
package org.dlese.dpc.schemedit.ndr.util;

import org.dlese.dpc.ndr.NdrUtils;
import org.dlese.dpc.ndr.reader.*;
import org.dlese.dpc.ndr.request.*;
import org.dlese.dpc.ndr.apiproxy.InfoXML;
import org.dlese.dpc.ndr.apiproxy.NDRConstants;
import org.dlese.dpc.ndr.apiproxy.NDRConstants.NDRObjectType;
import org.dlese.dpc.schemedit.SchemEditUtils;
import org.dlese.dpc.schemedit.test.TesterUtils;
import org.dlese.dpc.schemedit.config.CollectionConfigReader;
import org.dlese.dpc.xml.Dom4jUtils;
import org.dlese.dpc.xml.XSLTransformer;
import org.dlese.dpc.util.Files;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.XPath;
import java.io.File;
import java.net.URL;
import java.util.*;
import java.text.*;
import javax.xml.transform.Transformer;

/**
 *  Utilities for testing an NDR repository
 - can we create and modify a resource??
 *
 * @author     ostwald<p>
 *
 */
public class NDRRepoTester {

	private static boolean debug = true;
	private static boolean verbose = true;
	String mdpHandle = null;
	String aggHandle = null;
	String mdHandle = null;
	String resHandle = null;
	String agentHandle = null;
	
	public NDRRepoTester () throws Exception {
		SimpleNdrRequest.setDebug (debug);
		SimpleNdrRequest.setVerbose (verbose);
		
		prtln ("agentHandle: " + NDRConstants.getNcsAgent());
		prtln ("ndrApiBaseUrl: " + NDRConstants.getNdrApiBaseUrl());
	}


	void addResource (String resourceUrl) throws Exception {
		// does the resource exist?
		String resHandle = NdrUtils.findResource(resourceUrl);
		if (resHandle != null)
			prtln ("resource for " + resourceUrl + " EXISTS");
		else {
			prtln ("resource for " + resourceUrl + " DOES NOT exists");
			AddResourceRequest addResReq = new AddResourceRequest ();
			addResReq.setIdentifier(resourceUrl);
			// aggregator isn't necessary ...
			if (this.aggHandle != null)
				addResReq.addCommand("relationship", "memberOf", this.aggHandle);
		
			// add it (don't need an aggregator)
			InfoXML response = addResReq.submit();
			if (response.hasErrors()) {
				throw new Exception(response.getError());
			}
	
			resHandle = response.getHandle();
		}
		
		// By here we've created or found a resource, or we've thrown an exception
		
		
		// set a property
		
		
		// test by reading the resource object
	}
	
	/**
	 *  The main program for the NDRRepoTester class
	 *
	 * @param  args           The command line arguments
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public static void main(String[] args) throws Exception {
		TesterUtils.setSystemProps();
		
		File propFile = null; // propFile must be assigned!
		NdrUtils.setup (propFile);
		
		NDRRepoTester tester = new NDRRepoTester();
		
		// tester.aggHandle = "2200/test.20090211175402341T";
		
		String url = "http://www.nsidc.org/noaa/noodle/bowl";
		tester.addResource(url);
	}
	
	/**
	* Doesn't work for some strange reason ...
	*/
	void deleteDataStream (String mdHandle, String fmt) throws Exception {
		ModifyMetadataRequest request = new ModifyMetadataRequest (mdHandle);
		request.deleteDataStream(fmt);
		request.submit();
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
	 *  Sets the debug attribute of the NDRRepoTester class
	 *
	 * @param  bool  The new debug value
	 */
	public static void setDebug(boolean bool) {
		debug = bool;
	}


	/**
	 *  Description of the Method
	 *
	 * @param  s  Description of the Parameter
	 */
	private static void prtln(String s) {
		if (debug) {
			// SchemEditUtils.prtln(s, "NDRRepoTester");
			SchemEditUtils.prtln(s, "");
		}
	}

}

