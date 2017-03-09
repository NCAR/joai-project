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
package org.dlese.dpc.ndr.request;

import org.dlese.dpc.ndr.apiproxy.InfoXML;
import org.dlese.dpc.ndr.apiproxy.NDRConstants;
import org.dlese.dpc.ndr.connection.NDRConnection;

import org.dlese.dpc.xml.Dom4jUtils;
import org.dom4j.*;
import java.util.*;

/**
 *  Class to communiate directly with NDR via {@link org.dlese.dpc.ndr.connection.NDRConnection}
 *  with no payload (url only - no InputXML object).
 *
 * @author     Jonathan Ostwald
 * @version    $Id: SimpleNdrRequest.java,v 1.8 2007/12/05 22:36:14 ostwald Exp
 *      $
 */
public class SimpleNdrRequest {
	private static boolean debug = true;
	/**  NOT YET DOCUMENTED */
	protected static boolean verbose = false;

	/**  NOT YET DOCUMENTED */
	protected String verb = null;
	/**  NOT YET DOCUMENTED */
	protected String handle = null;

	/**  NOT YET DOCUMENTED */
	protected String requestAgent = NDRConstants.getNcsAgent();


	/**  Constructor for the SimpleNdrRequest object */
	public SimpleNdrRequest() { }


	/**
	 *  Constructor for the SimpleNdrRequest object with specified verb.
	 *
	 * @param  verb  NOT YET DOCUMENTED
	 */
	public SimpleNdrRequest(String verb) {
		this();
		this.verb = verb;
	}


	/**
	 *  Constructor for the SimpleNdrRequest object with specified verb and handle.
	 *
	 * @param  verb    NOT YET DOCUMENTED
	 * @param  handle  NOT YET DOCUMENTED
	 */
	public SimpleNdrRequest(String verb, String handle) {
		this(verb);
		this.handle = handle;
	}


	/**
	 *  Sets the verb attribute of the SimpleNdrRequest object
	 *
	 * @param  verb  The new verb value
	 */
	public void setVerb(String verb) {
		this.verb = verb;
	}


	/**
	 *  Gets the verb attribute of the SimpleNdrRequest object
	 *
	 * @return    The verb value
	 */
	public String getVerb() {
		return this.verb;
	}


	/**
	 *  Sets the requestAgent attribute of the SimpleNdrRequest object
	 *
	 * @param  handle  The new requestAgent value
	 */
	public void setRequestAgent(String handle) {
		this.requestAgent = handle;
	}


	/**
	 *  Gets the requestAgent attribute of the SimpleNdrRequest object
	 *
	 * @return    The requestAgent value
	 */
	public String getRequestAgent() {
		return this.requestAgent;
	}


	/**
	 *  Sets the handle attribute of the SimpleNdrRequest object
	 *
	 * @param  handle  The new handle value
	 */
	public void setHandle(String handle) {
		this.handle = handle;
	}


	/**
	 *  Gets the handle attribute of the SimpleNdrRequest object
	 *
	 * @return    The handle value
	 */
	public String getHandle() {
		return this.handle;
	}

	/**
	 *  Instantiates and initializes an NDRConnection for communicating with the NDR server.
	 *
	 * @param  path           NOT YET DOCUMENTED
	 * @return                The nDRConnection value
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	protected NDRConnection getNDRConnection(String path) throws Exception {
		NDRConnection connection = null;
		try {
			connection = new NDRConnection(path);
			connection.setTimeout(NDRConstants.NDR_CONNECTION_TIMEOUT);
		} catch (Exception e) {
			prtln("NDRConnection could not be established");
			throw new Exception("NDRConnection could not be established: " + e);
		}
		return connection;
	}

	protected String makePath () throws Exception {
		if (verb == null || verb.trim().length() == 0)
			throw new Exception("attempting to submit request without specifying verb");
		String path = NDRConstants.getNdrApiBaseUrl() + "/" + this.verb;
		if (handle != null && handle.trim().length() > 0)
			path = path + "/" + handle;
		return path;
	}
	
	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @return                NOT YET DOCUMENTED
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public InfoXML submit() throws Exception {

		String path;
		try {
			path = makePath ();
		} catch (Exception e) {
			throw new Exception ("could not make request path: " + e.getMessage());
		}
		return submit(path);
	}

	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  path           NOT YET DOCUMENTED
	 * @return                NOT YET DOCUMENTED
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public InfoXML submit(String path) throws Exception {

		NDRConnection connection = getNDRConnection(path);

		if (verbose) {
			prtln("\n===============\nproxyRequest");
			prtln(path);
		}

		InfoXML proxyResponse = new InfoXML(connection.request());

		if (verbose) {
			prtln("\n===============\nproxyResponse");
			try {
				Document responseDoc = DocumentHelper.parseText(proxyResponse.getResponse());
				pp(responseDoc);
			} catch (Exception e) {
				prtln("response could not be displayed: " + e.getMessage());
			}
		}
		return proxyResponse;
	}


	/**
	 *  Gets the verbose attribute of the SimpleNdrRequest class
	 *
	 * @return    The verbose value
	 */
	public static boolean getVerbose() {
		return verbose;
	}


	/**
	 *  Sets the verbose attribute of the SimpleNdrRequest class
	 *
	 * @param  v  The new verbose value
	 */
	public static void setVerbose(boolean v) {
		verbose = v;
	}


	/**
	 *  Sets the debug attribute of the SimpleNdrRequest class
	 *
	 * @param  d  The new debug value
	 */
	public static void setDebug(boolean d) {
		debug = d;
	}


	/**
	 *  Gets the debug attribute of the SimpleNdrRequest class
	 *
	 * @return    The debug value
	 */
	public static boolean getDebug() {
		return debug;
	}


	/**  NOT YET DOCUMENTED */
	public void report() {
		prtln("\n** SimpleNdrRequest report **");
		prtln("verb: " + this.verb);
	}


	/**
	 *  Prints a dom4j.Node as formatted string.
	 *
	 * @param  node  NOT YET DOCUMENTED
	 */
	protected static void pp(Node node) {
		prtln(Dom4jUtils.prettyPrint(node));
	}


	/**
	 *  Description of the Method
	 *
	 * @param  s  Description of the Parameter
	 */
	protected static void prtln(String s) {
		if (debug) {
			System.out.println(s);
		}
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  s  NOT YET DOCUMENTED
	 */
	protected static void prtlnErr(String s) {
		System.out.println(s);
	}
}

