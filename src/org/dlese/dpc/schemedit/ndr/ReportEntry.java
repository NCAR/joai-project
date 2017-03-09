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

import org.dlese.dpc.xml.Dom4jUtils;

import java.text.SimpleDateFormat;
import java.util.*;
import java.io.File;

import org.dom4j.*;

/**
 *  Class to capture the results of a NDRSync operation in which a metadata record is written to the NDR.
 *
 * @author     Jonathan Ostwald
 * @version    $Id: ReportEntry.java,v 1.3 2009/03/20 23:33:56 jweather Exp $
 */
public class ReportEntry {
	private static boolean debug = true;

	protected String id;
	protected String errorMsg;
	protected String command = null;
	protected InfoXML ndrResponse = null;

	/**
	 *  Constructor for the ReportEntry object
	 *
	 * @param  collectionConfig  NOT YET DOCUMENTED
	 * @param  collectionName    NOT YET DOCUMENTED
	 */
	public ReportEntry(String id, String errorMsg) {
		this.id = id;
		this.errorMsg = errorMsg;
		this.command = "package";
	}

	public ReportEntry(String id, String command, InfoXML ndrResponse) {
		this (id, null);
		this.command = command;
		this.ndrResponse = ndrResponse;
	}

	public String getId () {
		return id;
	}
	
	public boolean isError () {
		return (this.getErrorMsg() != null);
	}
	
	public boolean getIsError () {
		return this.isError();
	}
	
	public String getErrorMsg () {
		if (errorMsg != null)
			return errorMsg;
		else if (ndrResponse != null)
			return ndrResponse.getError();
		else
			return null;
	}
	
	public InfoXML getResponse () {
		return this.ndrResponse;
	}
	
	public String getCommand () {
		return command;
	}
		
	public String getHandle () {
		return ndrResponse.getHandle();
	}
		
	/**
	 *  The main program for the ReportEntry class
	 *
	 * @param  args           The command line arguments
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public static void main(String[] args) throws Exception {

	}


	/**
	 *  Description of the Method
	 *
	 * @param  s  Description of the Parameter
	 */
	private static void prtln(String s) {
		SchemEditUtils.prtln(s, "ReportEntry");
	}
}

