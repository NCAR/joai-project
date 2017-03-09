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

import javax.servlet.*;
import javax.servlet.http.*;

import org.dlese.dpc.schemedit.action.form.*;
import org.dlese.dpc.schemedit.*;
import org.dlese.dpc.schemedit.vocab.*;
import org.dlese.dpc.xml.*;
import org.dlese.dpc.xml.schema.*;
import org.dlese.dpc.serviceclients.remotesearch.*;

import org.dom4j.Document;
import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import org.dom4j.Node;

import org.apache.struts.upload.FormFile;
import org.apache.struts.upload.MultipartRequestHandler;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionError;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionServlet;
import org.apache.struts.util.MessageResources;

/**
 *  Controller for the ADN Editor
 *
 *@author    ostwald
 <p>$Id: SchemEditAdminAction.java,v 1.16 2009/03/20 23:33:55 jweather Exp $
 */
public final class SchemEditAdminAction extends DCSAction {

	private static boolean debug = true;

	/**
	 *  Description of the Method
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

		String errorMsg = null;
		/*
		    Design note:
		    Only one instance of this class gets created for the app and shared by
		    all threads. To be thread-safe, use only local variables, not instance
		    variables (the JVM will handle these properly using the stack). Pass
		    all variables via method signatures rather than instance vars.
		  */
		SchemEditAdminForm adminForm;
		MetaDataFramework framework = null;

		ActionErrors errors = initializeFromContext (mapping, request);
		if (!errors.isEmpty()) {
			saveErrors (request, errors);
			return (mapping.findForward("error.page"));
		}
		// messages are in doReloadSchema
		ActionErrors messages = new ActionErrors();

		try {
			adminForm = (SchemEditAdminForm) form;
		} catch (Throwable e) {
			prtln("SchemEditAdminAction caught exception. " + e);
			return null;
		}		
		
		String command = request.getParameter("command");
		String xmlFormat = request.getParameter("xmlFormat");

		// Sanity Checks
		prtParam("command", command);
		prtParam("xmlFormat", xmlFormat);

		// there must be a value for xmlFormat
		if (xmlFormat == null || xmlFormat.trim().length() == 0) {
			errorMsg = "xmlFormat not specified!";
			prtln(errorMsg);
			throw new ServletException(errorMsg);
		}
		else {
			framework = getMetaDataFramework(xmlFormat);
			if (framework == null) {
				errorMsg = "framework not found for " + xmlFormat;
				prtln(errorMsg);
				throw new ServletException(errorMsg);
			}
		}

		if (command == null || command.trim().length() == 0) {
			prtln("command is null");

			loadAdminForm(adminForm, framework);
			return mapping.findForward("admin");
		}

		if (command.equals("update")) {

/* 			FormFile sampleFile = adminForm.getSampleFile();
			if (sampleFile != null && sampleFile.getFileSize() > 0) {
				try {
					doUploadSampleFile(sampleFile, adminForm, framework);
					String uploadMsg = "Sample File successfully uploaded";
					errors.add("message",
							new ActionError("generic.message", uploadMsg));
					saveErrors(request, errors);
				} catch (Exception e) {
					errorMsg = e.getMessage();
					prtln(errorMsg);
					errors.add("pageErrors",
							new ActionError("generic.error", errorMsg));
				}
			} */

			// framework.setEditorConfig(adminForm.getEditorConfig());
			framework.setDiscussionURL(adminForm.getDiscussionURL());
			framework.setRenderer(adminForm.getRenderer());
			framework.setSchemaURI(adminForm.getSchemaURI());

			if (!framework.getWorkingSchemaURI().equals(adminForm.getSchemaURI()) ||
					!framework.getWorkingRenderer().equals(adminForm.getRenderer())) {
				doReloadSchema(adminForm, framework, errors, messages);
			}

			if (errors.size() > 0) {
				prtln("there are " + errors.size() + " errors");
				saveErrors(request, errors);
			}

			if (messages.size() > 0) {
				prtln("there are " + messages.size() + " messages");
				saveErrors(request, messages);
			}
			if (errors.size() == 0) {
				try {
					framework.writeProps();
				} catch (Exception e) {
					errorMsg = "Properties could not be saved because: " + e.getMessage();
					prtln(errorMsg);
					errors.add("pageErrors",
							new ActionError("generic.error", errorMsg));
					saveErrors(request, errors);
				}
				// do we want to automatically exit if there are no errors??
				// adminForm.setExitPath(framework.getFormActionPath());
			}
		}

		if (command.equals("reloadSchema")) {
			doReloadSchema(adminForm, framework, errors, messages);

			if (errors.size() > 0) {
				prtln("there are " + errors.size() + " errors");
				saveErrors(request, errors);
			}

			if (messages.size() > 0) {
				prtln("there are " + messages.size() + " messages");
				saveErrors(request, messages);
			}
		}
		
		if (command.equals("loadFramework")) {
			try {
				this.frameworkRegistry.loadFramework (xmlFormat);
			} catch (Throwable t) {
				errors.add ("pageErrors",
					new ActionError ("generic.error", "Couldn't load framework for " + 
						xmlFormat + ": " + t.getMessage()));
				saveErrors (request, errors);
				return mapping.findForward("admin");
			}
		}

		if (command.equals("unloadFramework")) {
			try {
				this.frameworkRegistry.unregister(xmlFormat);
			} catch (Throwable t) {
				errors.add ("pageErrors",
					new ActionError ("generic.error", "Couldn't unload framework for " + 
						xmlFormat + ": " + t.getMessage()));
				saveErrors (request, errors);
			}
		}
		
		if (command.equals("reloadFieldInfo")) {
			prtln ("reload Field Info()");
			prtln ("framework name: " + framework.getName());
			FieldInfoMap fieldInfoMap = framework.getFieldInfoMap();
			if (fieldInfoMap == null || fieldInfoMap.getKeySet().size() == 0) {
				errors.add("message",
					new ActionError("generic.message", "No Field Info found for this framework"));
				saveErrors(request, messages);
			}
			else {
				try {
					fieldInfoMap.reload();
					messages.add("message",
						new ActionError("generic.message", "Field Info reloaded"));
					saveErrors(request, messages);
				} catch (Exception e) {
					errors.add ("pageErrors", new ActionError ("generic.error", e.getMessage()));
					saveErrors (request, errors);
				}
				if (errors.size() == 0) {
					try {
						framework.renderEditorPages();
						messages.add("message",
								new ActionError("generic.message", "Editor pages successfully regenerated"));
					} catch (Exception e) {
						prtln(e.getMessage());
						errors.add("pageErrors",
								new ActionError("generic.error", "Editor pages could not be created because: " + e.getMessage()));
					}
				}
			}
		}

		if (command.equals("saveProps")) {
			try {
				framework.writeProps();
			} catch (Exception e) {
				errorMsg = "Properties could not be saved because: " + e.getMessage();
				prtln(errorMsg);
				errors.add("pageErrors",
						new ActionError("generic.error", errorMsg));
				saveErrors(request, errors);
			}
		}

		return mapping.findForward("admin");
	}


	/**
	 *  Description of the Method
	 *
	 *@param  adminForm  Description of the Parameter
	 *@param  framework  Description of the Parameter
	 *@param  errors     Description of the Parameter
	 *@param  messages   Description of the Parameter
	 */
	public void doReloadSchema(SchemEditAdminForm adminForm, MetaDataFramework framework, ActionErrors errors, ActionErrors messages) {

		// reload schema helper
		try {
			framework.resetValidator ();
			framework.loadSchemaHelper();
			adminForm.setWorkingSchemaURI(framework.getWorkingSchemaURI());
			messages.add("message",
					new ActionError("generic.message", "Schema successfully loaded"));

		} catch (Exception e) {
			prtln(e.getMessage());
			errors.add("schemaURI", new ActionError("generic.error", e.getMessage()));
		}

		// render editor pages
		if (errors.size() == 0) {
			try {
				framework.renderEditorPages();
				messages.add("message",
						new ActionError("generic.message", "Editor pages successfully regenerated"));
			} catch (Exception e) {
				prtln(e.getMessage());
				errors.add("pageErrors",
						new ActionError("generic.error", "Editor pages could not be created because: " + e.getMessage()));
			}
		}
	}


	/**
	 *  Description of the Method
	 *
	 *@param  sampleFile     Description of the Parameter
	 *@param  adminForm      Description of the Parameter
	 *@param  framework      Description of the Parameter
	 *@exception  Exception  Description of the Exception
	 */
/* 	private void doUploadSampleFile(FormFile sampleFile, SchemEditAdminForm adminForm, MetaDataFramework framework)
		throws Exception {

		//retrieve the content type
		String contentType = sampleFile.getContentType();
		//retrieve the file size
		String size = (sampleFile.getFileSize() + " bytes");
		prtln("sampleFile size in bytes: " + size);
		String uploadMsg = "";
		String data = null;

		//retrieve the file data
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		InputStream stream = sampleFile.getInputStream();

		//write the file to the sampleFile
		String sampleFilePath = framework.getSampleRecordFile();

		OutputStream bos = new FileOutputStream(sampleFilePath);
		int bytesRead = 0;
		byte[] buffer = new byte[8192];
		while ((bytesRead = stream.read(buffer, 0, 8192)) != -1) {
			bos.write(buffer, 0, bytesRead);
		}
		bos.close();
		uploadMsg = "The sampleFile has been written to \"" + sampleFilePath + "\"";
		//close the stream
		stream.close();

	} */


	/**
	 *  Description of the Method
	 *
	 *@param  framework  Description of the Parameter
	 *@param  adminForm  Description of the Parameter
	 */
	private void loadAdminForm(SchemEditAdminForm adminForm, MetaDataFramework framework) {
		prtln("loading Admin Form from framework values");

		adminForm.setWorkingSchemaURI(framework.getWorkingSchemaURI());
		adminForm.setSchemaURI(framework.getSchemaURI());
		prtln("loadAdminForm set schemaURI to " + adminForm.getSchemaURI());
		adminForm.setPageList(framework.getPageList());
		adminForm.setRenderer(framework.getRenderer());
		adminForm.setFrameworkName(framework.getName());
		adminForm.setXmlFormat(framework.getXmlFormat());
		// adminForm.setEditorConfig(framework.getEditorConfig());
		adminForm.setDiscussionURL(framework.getDiscussionURL());

		adminForm.setExitPath("");
	}


	/**
	 *  Description of the Method
	 *
	 *@param  path        Description of the Parameter
	 *@param  propertyId  Description of the Parameter
	 *@return             Description of the Return Value
	 */
	private ActionErrors validateFilePath(String path, String propertyId) {

		ActionErrors errors = new ActionErrors();

		File file = new File(path);
		if (!file.exists()) {
			prtln("caught an error for " + propertyId);
			errors.add(propertyId,
					new ActionError("generic.error", "warning: file does not exist"));
		}

		return errors;
	}


	/**
	 *  Print a line to standard out.
	 *
	 *@param  s  The String to print.
	 */
	protected void prtln(String s) {
		if (debug) {
			System.out.println("SchemEditAdminAction: " + s);
		}
	}


	/**
	 *  Description of the Method
	 *
	 *@param  name   Description of the Parameter
	 *@param  value  Description of the Parameter
	 */
	protected void prtParam(String name, String value) {
		if (value == null) {
			prtln(name + " is null");
		}
		else {
			prtln(name + " is " + value);
		}
	}
}

