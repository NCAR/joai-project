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
package org.dlese.dpc.xml.schema.action;

import org.dlese.dpc.schemedit.*;

import org.dlese.dpc.xml.schema.*;
import org.dlese.dpc.xml.schema.action.form.*;
import org.dlese.dpc.xml.*;
import org.dlese.dpc.serviceclients.remotesearch.*;

import org.dlese.dpc.vocab.MetadataVocab;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Node;

import java.util.*;
import java.io.*;
import java.util.Hashtable;
import java.util.Locale;

import javax.servlet.*;
import javax.servlet.http.*;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionError;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionServlet;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;
import org.apache.struts.util.MessageResources;

import java.net.URLEncoder;
import java.net.URLDecoder;

/**
 *  Just a minimal action that will set up a form bean and then forward to a jsp
 *  page this action depends upon an action-mapping in the struts-config file!
 *
 *@author     ostwald
 */
public final class SchemaViewerAction extends Action {

	private static boolean debug = false;


	// --------------------------------------------------------- Public Methods

	/**
	 *  Processes the specified HTTP request and creates the corresponding HTTP
	 *  response by forwarding to a JSP that will create it. Returns an {@link
	 *  org.apache.struts.action.ActionForward} instance that maps to the Struts
	 *  forwarding name "xxx.xxx," which must be configured in struts-config.xml to
	 *  forward to the JSP page that will handle the request.
	 *
	 *@param  mapping               Description of the Parameter
	 *@param  form                  Description of the Parameter
	 *@param  request               Description of the Parameter
	 *@param  response              Description of the Parameter
	 *@return                       Description of the Return Value
	 *@exception  IOException       Description of the Exception
	 *@exception  ServletException  Description of the Exception
	 */
	public ActionForward execute(
			ActionMapping mapping,
			ActionForm form,
			HttpServletRequest request,
			HttpServletResponse response)
		throws IOException, ServletException {
		/*
		 *  Design note:
		 *  Only one instance of this class gets created for the app and shared by
		 *  all threads. To be thread-safe, use only local variables, not instance
		 *  variables (the JVM will handle these properly using the stack). Pass
		 *  all variables via method signatures rather than instance vars.
		 */
		 
		prtln ("\nexecuting");
		 
		SchemaViewerForm svf;
		MetaDataFramework framework = null;

		try {
			svf = (SchemaViewerForm) form;
		} catch (Throwable e) {
			prtln("SchemaViewerAction caught exception. " + e);
			return null;
		}

		String errorMsg;
		
		MetadataVocab vocab = (MetadataVocab) servlet.getServletContext().getAttribute("MetadataVocab");

		// Pass vocab to the form bean
		svf.setVocab(vocab);
		// list of strings
		svf.setFrameworks(getRegistry().getAllFormats());

		// RemoteSearcher rs = (RemoteSearcher) servlet.getServletContext().getAttribute("RemoteSearcher");

		SchemaHelper schemaHelper = null;

		ActionErrors errors = new ActionErrors();
		ActionMessages messages = new ActionMessages();

		// Query Args
		String id = request.getParameter("id");
		String command = request.getParameter("command");
		String path = request.getParameter("path");
		String typeName = request.getParameter("typeName");
		/* String xmlFormat = request.getParameter("xmlFormat"); */

		prtParam ("command", command);
		prtParam ("id", id);
		prtParam ("path", path);
		prtParam ("typeName", typeName);
		/* prtParam ("xmlFormat", xmlFormat); */
		
		SchemEditUtils.showRequestParameters(request);
		
/* 		if ((xmlFormat == null) || (xmlFormat.trim().length() == 0)) {
			// Default Framework is ADN
			prtln ("xmlFormat is unknown: assigning adn" + svf.getXmlFormat());
			framework = getMetaDataFramework("adn");
			// framework = setFramework (svf.getXmlFormat(), svf);
		}
		else {
			framework = getMetaDataFramework (xmlFormat);
		} */
		
		

		if (svf.getFramework () == null) {
			prtln ("there is no framework in the form bean");
			// get the first registered framework
			FrameworkRegistry reg = this.getRegistry();
			String defaultFormat = (String)reg.getAllFormats().get(0);
			framework = getMetaDataFramework(defaultFormat);
		}
		else {
			prtln ("reusing current framework");
			framework = svf.getFramework ();
		}
		
		if (framework == null)
			prtln ("framework is null!");
		errors.add (setFramework (framework.getXmlFormat(), svf));
		if (!errors.isEmpty()) {
			saveErrors(request, errors);
			return mapping.findForward("schema.index");
		}
		
		schemaHelper = framework.getSchemaHelper();
		
		String qs = request.getQueryString();
		if (qs == null) {
			prtln("request contained no query string");
		}
		else {
			prtln("Query is " + qs);
		}

		if (command == null) {
			return mapping.findForward("schema.index");
		}
		
		if (command.equals("setFramework")) {
			//  framework = setFramework (id, svf);
			framework = getMetaDataFramework(id);
			errors.add (setFramework (framework.getXmlFormat(), svf));
			if (!errors.isEmpty())
				saveErrors(request, errors);
			return mapping.findForward("schema.index");
		}
		
		if (command.equals("doPath")) {
			prtln ("path: " + path);
			if ((path==null) || (path.trim().length() == 0)) {
				errors.add(errors.GLOBAL_ERROR,
					new ActionError("generic.error", "View didn't get an path!"));
				saveErrors(request, errors);
				return mapping.findForward("schema.index");
			}
			else {
				if (path.endsWith ("/"))
					path = path.substring(0, path.length()-1);
				svf.setPath(path);
			}
			
			errors.add (setGlobalDef (path, schemaHelper, svf));
			if (!errors.isEmpty()) {
				saveErrors(request, errors);
				return mapping.findForward("schema.index");
			}
				
/* 			GlobalDef globalDef = schemaHelper.getGlobalDefFromXPath(path);
			if (globalDef == null) {
				errors.add(errors.GLOBAL_ERROR,
					new ActionError("schemaviewer.def.notfound.error", path));
				saveErrors(request, errors);
				return mapping.findForward("schema.index");
			}
			else {
				svf.setGlobalDef(globalDef);
				// svf.setTypeName(globalDef.getName());
			} */
			
			SchemaNode schemaNode = schemaHelper.getSchemaNode(path);
			if (schemaNode == null) {
				errors.add(errors.GLOBAL_ERROR,
					new ActionError("schema.schemaNode.notfound.error", path));
				saveErrors(request, errors);
				return mapping.findForward("schema.index");
			}
			else {
				svf.setSchemaNode(schemaNode);
			}
		}
		
		if (command.equals("doType")) {
			if ((typeName==null) || (typeName.trim().length() == 0)) {
				errors.add(errors.GLOBAL_ERROR,
					new ActionError("generic.error", "Didn't get an type name!"));
				saveErrors(request, errors);
				return mapping.findForward("schema.index");
			}
			else {
				svf.setPath(path);
			}
			
			// GlobalDef globalDef = (GlobalDef)schemaHelper.getGlobalDefMap().getValue(typeName);
			GlobalDef contextDef = schemaHelper.getGlobalDefFromXPath(path);
			GlobalDef globalDef = contextDef.getSchemaReader().getGlobalDef(typeName);
			
			// GlobalDef globalDef = schemaHelper.getGlobalDef(typeName);
			
			if (globalDef == null) {
				errors.add(errors.GLOBAL_ERROR,
					new ActionError("schemaviewer.def.notfound.error", typeName));
				saveErrors(request, errors);
				return mapping.findForward("schema.index");
			}
			else {
				svf.setGlobalDef(globalDef);
			}
			
			// schemaNode to show current path
			SchemaNode schemaNode = schemaHelper.getSchemaNode(path);
			if (schemaNode == null) {
				errors.add(errors.GLOBAL_ERROR,
					new ActionError("schema.schemaNode.notfound.error", path));
				saveErrors(request, errors);
				return mapping.findForward("schema.index");
			}
			else {
				svf.setSchemaNode(schemaNode);
			}
		}		

		if (command.equals("report")) {
			try {
				svf.setReport(generateReport (form));
			} catch (Throwable t) {
				prtln ("ERROR: " + t.getMessage());
				t.printStackTrace();
			}
			return mapping.findForward("schema.report");
		}
		
		return mapping.findForward("schema.index");
	}

 	private Map generateReport (ActionForm form) throws Exception {
		SchemaViewerForm svf = (SchemaViewerForm) form;
		Map report = new TreeMap ();
		String reportFunction = svf.getReportFunction();
		if (reportFunction == null) return report;
		
		String [] frameworks = svf.getSelectedFrameworks ();
		if (frameworks == null || frameworks.length == 0)
			return report;
		
		for (int i=0;i<frameworks.length;i++) {
			String xmlFormat = frameworks[i];
			MetaDataFramework framework = this.getMetaDataFramework(xmlFormat);
			GlobalDefMap globalDefMap = framework.getSchemaHelper().getGlobalDefMap();
			List defs = null;
			try {
				defs = GlobalDefReporter.getGlobalDefs (reportFunction, globalDefMap);
			} catch (Exception e) {
				prtln ("ERROR: " + e.getMessage());
				continue;
			}
			report.put (xmlFormat, defs);
		}
		return report;
	}
	
	private FrameworkRegistry getRegistry () throws ServletException {
		FrameworkRegistry reg = (FrameworkRegistry)servlet.getServletContext()
			.getAttribute("frameworkRegistry");
		if (reg == null) {
			throw new ServletException ("frameworkRegistry not found in servletContext");
		}
		else {
			return reg;
		}
	}
	
	private ActionErrors setGlobalDef (String path, SchemaHelper schemaHelper, SchemaViewerForm svf) {
		ActionErrors errors = new ActionErrors ();
		GlobalDef globalDef = schemaHelper.getGlobalDefFromXPath(path);
		if (globalDef == null) {
			errors.add(errors.GLOBAL_ERROR,
				new ActionError("schemaviewer.def.notfound.error", path));
		}
		else {
			svf.setGlobalDef(globalDef);
			// svf.setTypeName(globalDef.getName());
		}
		return errors;
	}
	
	private MetaDataFramework getMetaDataFramework(String xmlFormat) throws ServletException {
/* 		FrameworkRegistry reg = (FrameworkRegistry)servlet.getServletContext()
			.getAttribute("frameworkRegistry"); 
		return reg.getFramework (xmlFormat);*/
		return getRegistry().getFramework (xmlFormat);
	}
	
	private ActionErrors setFramework (String xmlFormat, SchemaViewerForm svf ) throws ServletException {
		return setFramework (this.getMetaDataFramework(xmlFormat), svf);
	}
	
	private ActionErrors setFramework (MetaDataFramework mdf, SchemaViewerForm svf ) throws ServletException {
		// MetaDataFramework mdf = getMetaDataFramework (xmlFormat);
		// mdf = (MetaDataFramework) servlet.getServletContext().getAttribute(xmlFormat);
		ActionErrors errors = new ActionErrors();
		String errorMsg = "";
		if (mdf == null) {
			errorMsg = "setFramework: Framework is NULL";
			errors.add (errors.GLOBAL_ERROR, new ActionError ("generic.error", errorMsg));
			return errors;
		}
		
		
		// if the framework has changed, set the form bean path to the default
		String rootElementName = mdf.getRootElementName();
		String rootPath = "/" + rootElementName;
		svf.setPath (rootPath);
		
		SchemaHelper schemaHelper = mdf.getSchemaHelper();
		SchemaNode schemaNode = schemaHelper.getSchemaNode (rootPath);
		if (schemaNode == null) {
			errors.add(errors.GLOBAL_ERROR,
				new ActionError("schema.schemaNode.notfound.error", rootPath));
			return errors;
		}
		svf.setSchemaNode(schemaNode);
		svf.setFramework(mdf);
		svf.setSchemaHelper(mdf.getSchemaHelper());
		errors.add (setGlobalDef (rootPath, mdf.getSchemaHelper(), svf));

		return errors;
	}
	
		
	private void prtParam (String name, String value) {
		if (value == null)
			prtln(name + " is null");
		else
			prtln(name + " is " + value);
	}

	/**
	 *  Print a line to standard out.
	 *
	 *@param  s  The String to print.
	 */
	private void prtln(String s) {
		if (debug) {
			// System.out.println("SchemaViewerAction: " + s);
			SchemEditUtils.prtln (s, "SchemaViewerAction");
		}
	}

}

