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
package org.dlese.dpc.suggest;

import org.dlese.dpc.suggest.comment.*;

import org.dlese.dpc.xml.schema.SchemaHelper;
import org.dlese.dpc.serviceclients.webclient.WebServiceClient;
import org.dlese.dpc.webapps.tools.GeneralServletTools;
import org.dlese.dpc.vocab.MetadataVocab;


// Enterprise imports
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

import java.util.*;
import java.text.SimpleDateFormat;
import java.io.File;
import java.net.URL;

/**
 *  Initializes the SuggestCommentHelper and sets up the Suggest a Resource
 *  context.
 *
 *@author    Jonathan
 */

public final class SuggestCommentServlet extends HttpServlet {

	private static boolean debug = true;


	/**
	 *  Initialize the SuggestCommentServlet
	 *
	 *@param  conf                  Description of the Parameter
	 *@exception  ServletException  Description of the Exception
	 */
	public void init(ServletConfig conf)
		throws ServletException {
		super.init(conf);
		System.out.println(getDateStamp() + " SuggestCommentServlet starting");
		String initErrorMsg = "";

		ServletContext context = getServletContext();

		// The MetadataVocabServlet puts the MetadataVocab object in the context under "MetadataVocab"
		MetadataVocab vocab = (MetadataVocab) context.getAttribute("MetadataVocab");
		
		// Use schema definition from web to load schemaHelper
		String schemaLoc = (String) context.getInitParameter("schemaUrl-comment");
		prtln("schemaLoc: " + schemaLoc);
		
		URL schemaUrl = null;
		SchemaHelper schemaHelper = null;
		try {
			schemaUrl = new URL(schemaLoc);
			schemaHelper = new SchemaHelper(schemaUrl, "annotationRecord");
		} catch (Throwable e) {
			initErrorMsg = "did not instantiate schemaHelper: " + e;
			prtlnErr(initErrorMsg);
			throw new ServletException(initErrorMsg);
		}
		prtln("schemaHelper instantianted");
 
 		File suggestionTemplate = new File(getAbsolutePath("WEB-INF/data/commentTemplate.xml"));
		if (!suggestionTemplate.exists())
			throw new ServletException ("commentTemplate not found at " + suggestionTemplate.toString());
 
		SuggestCommentHelper serviceHelper = new SuggestCommentHelper(suggestionTemplate, schemaHelper);
		prtln("instantiated SuggestCommentHelper");
		context.setAttribute("SuggestCommentHelper", serviceHelper); 

		// searchService is used by the DupSimUrlChecker and other classes requiring Search Web Services
		String searchServiceUrl = (String) context.getInitParameter("searchServiceUrl-comment");
		WebServiceClient searchServiceClient = new WebServiceClient (searchServiceUrl);
		serviceHelper.setSearchServiceClient(searchServiceClient);
		
		// repositoryServiceClient is used to put records to the DCS
		String putServiceUrl = (String) context.getInitParameter("putServiceUrl-comment");
		WebServiceClient repositoryServiceClient = new WebServiceClient (putServiceUrl);
		serviceHelper.setRepositoryServiceClient(repositoryServiceClient);
		
		// putCollection designates the collection in which new suggestions are placed via the putRecord service
		String putCollection = (String) context.getInitParameter("putCollection-comment");
		serviceHelper.setDestCollection(putCollection);
		
		// mailServer for emailing notifications
		String mailServer = (String) context.getInitParameter("mailServer");
		serviceHelper.setMailServer(mailServer);
		
		serviceHelper.setDcsStatus(
			(String) context.getInitParameter("dcsStatus-comment"));
			
		serviceHelper.setDcsStatusNote(
			(String) context.getInitParameter("dcsStatusNote-comment"));
			
		String emailFrom = (String) context.getInitParameter("emailFrom-comment");
		serviceHelper.setEmailFrom(emailFrom);
			
		String [] emailTo = SuggestUtils.commaDelimitedToArray (
			(String) context.getInitParameter("emailTo-comment"));
		serviceHelper.setEmailTo(emailTo);
			
		String msg = "SuggestCommentServlet configuration";
		msg += "\n\t" + "putServiceUrl: " + putServiceUrl;
		msg += "\n\t" + "putCollection: " + putCollection;
		msg += "\n\t" + "emailFrom: " + emailFrom;
		msg += "\n\t" + "emailTo:";
		for (int i=0;i<emailTo.length;i++)
			msg += "\n\t\t" + emailTo[i];
		prtln (msg + "\n");
		
		String bugs = context.getInitParameter("debug");
		if (bugs != null && bugs.equalsIgnoreCase("true")) {
			debug = true;
			prtln("Outputting debug info");
		}
		else {
			debug = false;
			prtln("Debug info disabled");
		}
		
		// Set all debugging:
		Emailer.setDebug(debug);
/* 		SuggestCommentAction.setDebug(debug);
		SuggestCommentHelper.setDebug(debug);
		SuggestCommentForm.setDebug(debug);
		Emailer.setDebug(debug);
		CommentRecord.setDebug(debug);
		SuggestUtils.setDebug(debug);
		UrlValidator.setDebug (debug); */
		
		
		System.out.println(getDateStamp() + " SuggestCommentServlet initialized.");
		

		
	}


	//==================================================
	/**
	 *  Performs shutdown operations.
	 */
	public void destroy() {
		System.out.println(getDateStamp() + " SuggestCommentServlet stopped");
	}


	/**
	 *  Gets the absolute path to a given file or directory. Assumes the path
	 *  passed in is eithr already absolute (has leading slash) or is relative to
	 *  the context root (no leading slash). If the string passed in does not begin
	 *  with a slash ("/"), then the string is converted. For example, an init
	 *  parameter to a config file might be passed in as
	 *  "WEB-INF/conf/serverParms.conf" and this method will return the
	 *  corresponding absolute path "/export/devel/tomcat/webapps/myApp/WEB-INF/conf/serverParms.conf."
	 *  <p>
	 *
	 *  If the string that is passed in already begings with "/", nothing is done.
	 *  <p>
	 *
	 *  Note: the super.init() method must be called prior to using this method,
	 *  else a ServletException is thrown.
	 *
	 *@param  fname                 An absolute or relative file name or path
	 *      (relative the the context root).
	 *@return                       The absolute path to the given file or path.
	 *@exception  ServletException  An exception related to this servlet
	 */
	private String getAbsolutePath(String fname)
		throws ServletException {
		return GeneralServletTools.getAbsolutePath(fname, getServletContext());
	}


	/**
	 *  Return a string for the current time and date, sutiable for display in log
	 *  files and output to standout:
	 *
	 *@return    The dateStamp value
	 */
	public static String getDateStamp() {
		return
			new SimpleDateFormat("MMM d, yyyy h:mm:ss a zzz").format(new Date());
	}


	/**
	 *  Output a line of text to error out, with datestamp.
	 *
	 *@param  s  The text that will be output to error out.
	 */
	private final void prtlnErr(String s) {
		System.err.println(getDateStamp() + " SuggestCommentServlet: " + s);
	}


	/**
	 *  Output a line of text to standard out, with datestamp, if debug is set to
	 *  true.
	 *
	 *@param  s  The String that will be output.
	 */
	private final void prtln(String s) {
		if (debug) {
			System.out.println(getDateStamp() + " SuggestCommentServlet: " + s);
		}
	}


	/**
	 *  Sets the debug attribute of the DDSServlet object
	 *
	 *@param  db  The new debug value
	 */
	public final void setDebug(boolean db) {
		debug = db;
	}
}

