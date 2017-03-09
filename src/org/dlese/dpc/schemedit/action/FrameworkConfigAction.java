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
package org.dlese.dpc.schemedit.action;

import java.io.*;
import org.dlese.dpc.schemedit.*;
import org.dlese.dpc.schemedit.input.SchemEditValidator;
import org.dlese.dpc.schemedit.action.form.SchemEditForm;
import org.dlese.dpc.schemedit.autoform.AutoForm;
import org.dlese.dpc.xml.schema.SchemaHelper;
import org.dlese.dpc.util.Files;

import org.dom4j.Document;

import javax.servlet.*;
import javax.servlet.http.*;

import org.apache.struts.action.ActionError;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

/**
 *  Controller for the DcsDataFramework editor. As a stand-alone schemedit action, this supports framework-config files
 to be created and edited in a particular directory, and is not connected to the index or repository.
 *
 *@author     ostwald
 <p>$Id: FrameworkConfigAction.java,v 1.6 2009/03/20 23:33:55 jweather Exp $
 */
public final class FrameworkConfigAction extends StandAloneSchemEditAction {

	private static boolean debug = true;


	protected String getXmlFormat() {
		return "framework_config";
	}

	protected File getRecordsDir() {
		return (File) servlet.getServletContext().getAttribute("frameworkConfigDir");
	}

	/**
	* Place the name of the edited framework in the request before calling super.execute.
	*/
	public ActionForward execute(
			ActionMapping mapping,
			ActionForm form,
			HttpServletRequest request,
			HttpServletResponse response)
		throws IOException, ServletException {
			
		String xmlFormat = null;
		try {
			xmlFormat = request.getParameter("recId");
			MetaDataFramework editedFramework = this.getMetaDataFramework(xmlFormat);
			request.setAttribute ("editedFormat", editedFramework.getName());
		} catch (Throwable e) {
			prtln ("Unable to stash editedFrameworkName in request: " + e.getMessage());
			// e.printStackTrace();
			request.setAttribute ("editedFormat", xmlFormat);
		}
		
		return super.execute (mapping, form, request, response);
	}
		
	
	/*
	* Overides super putRecord to decouple file name from xmlFormat. As a standalone editor,
	* this class obtains the xmlFormat of the framework config records through the request's
	* "recId" parameter. But we don't want to force files to be named according to the format.
	Instead, the source file is stored in the FrameworkConfigReader instance for this framework.
	*/
	protected File getFileToEdit(ActionMapping mapping,
			ActionForm form,
			HttpServletRequest request,
			SchemaHelper schemaHelper)
		throws Exception {

		prtln("getFileToEdit()");

		SchemEditForm sef = (SchemEditForm) form;
		MetaDataFramework metadataFramework = getMetaDataFramework();
		Document record = null;
		ActionErrors errors = new ActionErrors();
		String errorMsg;

		String xmlFormat = request.getParameter("recId");
		
		return getConfigFile (xmlFormat);
	}
	
	/*
	* Obtain the config file using the xmlFormat to query the frameworkConfigReader.
	*/
	private File getConfigFile (String xmlFormat) {
		return new File (this.frameworkRegistry.getConfigDir(), xmlFormat + ".xml");
	}
	
	/*
	* Overides super putRecord to decouple file name from xmlFormat
	*/
	protected void putRecord(ActionMapping mapping,
			ActionForm form,
			HttpServletRequest request)
		throws Exception {

		prtln ("FrameworkConfigAction.putRecord()");
		SchemEditForm sef = (SchemEditForm) form;
		MetaDataFramework metadataFramework = getMetaDataFramework();
		String recordXml = metadataFramework.getWritableRecordXml(sef.getDocMap().getDocument());
		
		String xmlFormat = request.getParameter("recId");
		File dest = getConfigFile (xmlFormat);
		
		try {
			Files.writeFile(recordXml, dest);
			prtln("record saved to disk at " + dest.toString());
			getMetaDataFramework(xmlFormat).refresh();
			// sef.setRecId("");
		} catch (Exception e) {
			e.printStackTrace();
			String errorMsg = "unable to write document to disk: " + e;
			throw new Exception(errorMsg);
		}
	}
	

	protected ActionForward handleMissingCommand(
			ActionMapping mapping,
			ActionForm form,
			HttpServletRequest request,
			HttpServletResponse response)
		throws ServletException {

		SchemEditForm sef = (SchemEditForm) form;

		String command = request.getParameter("command");
		
		if (command != null && "updateConfig".equals(command)) {
			prtln ("UPDATED CONFIG!!");
		}
		
		prtln("no command specified - returning homePage (" + homePage + ")");
		sef.setRecId("");
		sef.setCollection("");
		return mapping.findForward(homePage);
	}
	
	
	protected void prtln(String s) {
		if (debug) {
			System.out.println("StandAloneSchemEditAction: " + s);
		}
	}	
	
}

