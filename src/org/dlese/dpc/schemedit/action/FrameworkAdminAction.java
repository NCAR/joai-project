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

import org.dlese.dpc.schemedit.action.form.FrameworkAdminForm;
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

import java.util.*;

/**
 *  Controller for the ADN Editor
 *
 *@author    ostwald
 <p>$Id: FrameworkAdminAction.java,v 1.8 2010/08/10 20:28:33 ostwald Exp $
 */
public final class FrameworkAdminAction extends DCSAction {

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
		FrameworkAdminForm faForm;
		MetaDataFramework framework = null;

		ActionErrors errors = initializeFromContext (mapping, request);
		if (!errors.isEmpty()) {
			saveErrors (request, errors);
			return (mapping.findForward("error.page"));
		}

		try {
			faForm = (FrameworkAdminForm) form;
		} catch (Throwable e) {
			errors.add ("errors", 
					new ActionError ("FrameworkAdminAction caught exception. " + e.getMessage()));
			saveErrors (request, errors);
			return (mapping.findForward("error.page"));
		}		
		
		String command = request.getParameter("command");

		// Sanity Checks
		prtParam("command", command);

/* 		// there must be a value for xmlFormat
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
		} */

		try {
		
			if (command == null || command.trim().length() == 0) {
				loadForm (faForm);
				return mapping.findForward("frameworks.admin");
			}
			
			if (command.equals("loadFramework")) {
				String xmlFormat = request.getParameter("xmlFormat");
				if (xmlFormat == null) {
					errors.add ("error",
						new ActionError ("generic.error", "no format provided"));
				}
				if (errors.isEmpty()) {
					try {
						// force re-read of sif config files
						org.dlese.dpc.schemedit.sif.SIFRefIdManager.reinit();
						this.frameworkRegistry.loadFramework (xmlFormat);
						errors.add ("message",
							new ActionError ("generic.message", xmlFormat + " Framework loaded"));
					} catch (Throwable t) {
						errors.add ("error",
							new ActionError ("generic.error", "Couldn't load framework for " + 
								xmlFormat + ": " + t.getMessage()));
					}
				}
			}
	
			if (command.equals("unloadFramework")) {
				String xmlFormat = request.getParameter("xmlFormat");
				if (xmlFormat == null) {
					errors.add ("error",
						new ActionError ("generic.error", "no format provided"));
				}
				try {
					this.frameworkRegistry.unregister(xmlFormat);
				} catch (Throwable t) {
					errors.add ("error",
						new ActionError ("generic.error", "Couldn't unload framework for " + 
							xmlFormat + ": " + t.getMessage()));
				}
			}
			
			if (command.equals("reloadFieldInfo")) {
				prtln ("reload Field Info()");
				prtln ("framework name: " + framework.getName());
				FieldInfoMap fieldInfoMap = framework.getFieldInfoMap();
				if (fieldInfoMap == null || fieldInfoMap.getKeySet().size() == 0) {
					errors.add("message",
						new ActionError("generic.message", "No Field Info found for this framework"));
					saveErrors(request, errors);
				}
				else {
					try {
						fieldInfoMap.reload();
						errors.add("message",
							new ActionError("generic.message", "Field Info reloaded"));
					} catch (Exception e) {
						errors.add ("error", new ActionError ("generic.error", e.getMessage()));
						saveErrors (request, errors);
					}
					if (errors.isEmpty()) {
						try {
							framework.renderEditorPages();
							errors.add("message",
									new ActionError("generic.message", "Editor pages successfully regenerated"));
						} catch (Exception e) {
							prtln(e.getMessage());
							errors.add("error",
									new ActionError("generic.error", "Editor pages could not be created because: " + e.getMessage()));
						}
					}
				}
			}
			else if (command.equals ("clearFrameworkMessages")) {
				frameworkRegistry.clearLoadErrors();
				frameworkRegistry.clearLoadWarnings();
			}
		} catch (Exception e) {
			errors.add("message",
					new ActionError("generic.message", "Command could not be procesed: " + e.getMessage()));
		} catch (Throwable t) {
			errors.add("message",
					new ActionError("generic.message", "Unknown Error: " + t.getMessage()));
			t.printStackTrace();
		}
		loadForm (faForm);
		saveErrors (request, errors);
		return mapping.findForward("frameworks.admin");
	}

	private void loadForm (FrameworkAdminForm faForm) {
		Map frameworks = new HashMap();
		List uninitializedFrameworks = new ArrayList();

		// each framework has an editor that can be configured
		for (Iterator i = frameworkRegistry.getAllFormats().iterator(); i.hasNext(); ) {
			String xmlFormat = (String) i.next();
			MetaDataFramework framework = this.getMetaDataFramework(xmlFormat);
			
			// don't allow modification of framework_config!
			if (xmlFormat.equals("framework_config")) continue;
			
			if (framework != null) {
				frameworks.put(xmlFormat, framework);
				if (!framework.isInitialized())
					uninitializedFrameworks.add (framework);
			}
			
		}
		faForm.setUninitializedFrameworks (uninitializedFrameworks);
		faForm.setFrameworks(frameworks);
		// make a copy of the unloadedFrameworks map
		
		faForm.setUnloadedFrameworks (frameworkRegistry.getUnloadedFrameworks());
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
			System.out.println("FrameworkAdminAction: " + s);
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

