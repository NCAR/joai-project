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
package org.dlese.dpc.schemedit.input;

import org.dlese.dpc.webapps.servlets.filters.FilterCore;
import org.dlese.dpc.webapps.servlets.filters.CharArrayWrapper;
import org.dlese.dpc.webapps.tools.*;

import org.dlese.dpc.schemedit.repository.RepositoryService;
import org.dlese.dpc.schemedit.FrameworkRegistry;
import org.dlese.dpc.schemedit.MetaDataFramework;
import org.dlese.dpc.schemedit.dcs.DcsDataManager;
import org.dlese.dpc.schemedit.dcs.DcsDataRecord;
import org.dlese.dpc.index.reader.XMLDocReader;

import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.util.*;
import java.text.SimpleDateFormat;
import org.json.XML;
import org.json.JSONStringer;

import org.dlese.dpc.xml.*;
import org.dlese.dpc.webapps.servlets.filters.XMLPostProcessingFilter;

import org.dom4j.*;

/**
 *  Filter that validates XML and updates dcsDataRecord with the results of
 *  validation. Based on {@link org.dlese.dpc.webapps.servlets.filters.XMLValidationFilter}
 *
 * @author    Jonathan Ostwald
 */
public final class RecordValidationFilter extends XMLPostProcessingFilter {
	private static boolean debug = true;
	private ServletContext context = null;


	/**
	 *  Performs XML post-processing and gzipping of the response.
	 *
	 * @param  request               The request
	 * @param  response              The response
	 * @param  chain                 The chain of Filters
	 * @exception  ServletException  Iff error
	 * @exception  IOException       Iff IO error
	 */
	 protected String getValidationMessage (String xml, ServletRequest req) {
		prtln ("getValidationMessage(): recVal");
		String message = null;
		String prettyDoc = null;

		try {
			Document doc = DocumentHelper.parseText(xml);
			prettyDoc = Dom4jUtils.prettyPrint(doc);
		} catch (Throwable t) {}

		try {
			String recId = req.getParameter("fileid");

			// Get a cached validator if at all possible
			RepositoryService repositoryService =
				(RepositoryService) this.context.getAttribute("repositoryService");
			if (repositoryService == null)
				throw new Exception("RepositoryService not found");

			XMLDocReader docReader = repositoryService.getXMLDocReader(recId);
			if (docReader == null)
				throw new Exception("indexed record not found for \"" + recId + "\"");
			String xmlFormat = docReader.getNativeFormat();

			// make sure framework is loaded
			FrameworkRegistry frameworkRegistry =
				(FrameworkRegistry) this.context.getAttribute("frameworkRegistry");
			if (frameworkRegistry == null)
				throw new Exception("Framework Repository not found");
			if (frameworkRegistry.getFramework(xmlFormat) == null)
				throw new Exception("Framework not loaded for " + xmlFormat);

			DcsDataRecord dcsDataRecord = repositoryService.getDcsDataRecord(recId);
			String oldMessage = dcsDataRecord.getValidationReport();
			repositoryService.validateRecord(xml, dcsDataRecord, xmlFormat);
			message = dcsDataRecord.getValidationReport();
			prtln ("old message: " + oldMessage);
			prtln ("new message: " + message);

			if (!oldMessage.equals(message)) {
				dcsDataRecord.flushToDisk();
				repositoryService.updateRecord(recId);
				prtln("record indexed");
			}

			// finish up by normalizing validation message (code below expects message to be null
			// if record is valid
			if (dcsDataRecord.isValid()) {
				prtln("normalizing message for valid record");
				message = null;
			}
		} catch (Throwable t) {
			prtlnErr("Validation WARNING: " + t.getMessage());
			// Run non-cached XML validation over the content
			if (message == null)
				message = XMLValidator.validateString(xml, true);

		}
		return message;
	 }
	 
	 protected String xmlToHtml(String xml) {
		String prettyDoc = null;
		try {
			Document doc = DocumentHelper.parseText(xml);
			prettyDoc = Dom4jUtils.prettyPrint(doc);
		} catch (Throwable t) {}
		
		return OutputTools.xmlToHtml(prettyDoc != null ? prettyDoc : xml);
	}


	/**
	 *  Init is called once at application start-up.
	 *
	 * @param  config                The FilterConfig object that holds the
	 *      ServletContext and init information.
	 * @exception  ServletException  If an error occurs
	 */
	public void init(FilterConfig config) throws ServletException {
		if (context == null) {
			try {
				context = config.getServletContext();
/* 				if (((String) context.getInitParameter("debug")).toLowerCase().equals("true")) {
					debug = true;
					prtln("Outputing debug info");
				} */
				debug=true;
				System.out.println ("RecordValidationFilter initialized, debug is " + debug);
			} catch (Throwable e) {}
		}
	}


	/**  Destroy is called at application shut-down time. */
	public void destroy() { }



	//================================================================

	/**
	 *  Output a line of text to error out, with datestamp.
	 *
	 * @param  s  The text that will be output to error out.
	 */
/* 	protected final void prtlnErr(String s) {
		System.err.println(getDateStamp() + " " + s);
	} */



	/**
	 *  Output a line of text to standard out, with datestamp, if debug is set to
	 *  true.
	 *
	 * @param  s  The String that will be output.
	 */
	protected void prtln(String s) {
		if (debug)
			System.out.println(getDateStamp() + " RecordValidationFilter: " + s);
	}


	/**
	 *  Sets the debug attribute of the RecordValidationFilter object
	 *
	 * @param  db  The new debug value
	 */
	protected void setDebug(boolean db) {
		debug = db;
	}

}

