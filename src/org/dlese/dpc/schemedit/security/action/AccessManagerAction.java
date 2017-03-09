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
package org.dlese.dpc.schemedit.security.action;

import java.io.IOException;
import java.util.*;
import java.util.Locale;
import java.net.URLEncoder;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionError;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.util.MessageResources;
import org.apache.struts.config.ModuleConfig;
import org.apache.struts.config.ActionConfig;
import org.apache.struts.util.LabelValueBean;

import org.dlese.dpc.schemedit.action.DCSAction;
import org.dlese.dpc.schemedit.SessionBean;
import org.dlese.dpc.schemedit.SchemEditUtils;
import org.dlese.dpc.schemedit.config.CollectionRegistry;
import org.dlese.dpc.schemedit.security.auth.Auth;
import org.dlese.dpc.schemedit.ActionServlet;
import org.dlese.dpc.schemedit.struts.MyModuleConfig;
import org.dlese.dpc.schemedit.struts.HotActionMapping;
import org.dlese.dpc.schemedit.security.access.GuardedPath;
import org.dlese.dpc.schemedit.security.access.ActionPath;
import org.dlese.dpc.schemedit.security.access.AccessManager;
import org.dlese.dpc.schemedit.security.access.Roles;
import org.dlese.dpc.schemedit.security.action.form.AccessManagerForm;


/**

 */

public final class AccessManagerAction extends DCSAction {

	private static boolean debug = true;

	// --------------------------------------------------------- Public Methods

	/**
	 *  Process the specified HTTP request, and create the corresponding HTTP response (or forward to another web
	 *  component that will create it). Return an <code>ActionForward</code> instance describing where and how
	 *  control should be forwarded, or <code>null</code> if the response has already been completed.
	 *
	 * @param  mapping               The ActionMapping used to select this instance
	 * @param  request               The HTTP request we are processing
	 * @param  response              The HTTP response we are creating
	 * @param  form                  NOT YET DOCUMENTED
	 * @return                       NOT YET DOCUMENTED
	 * @exception  IOException       if an input/output error occurs
	 * @exception  ServletException  if a servlet exception occurs
	 */
	public ActionForward execute(ActionMapping mapping,
	                             ActionForm form,
	                             HttpServletRequest request,
	                             HttpServletResponse response)
		 throws IOException, ServletException {

		ActionErrors errors = initializeFromContext (mapping, request);
		if (!errors.isEmpty()) {
			saveErrors (request, errors);
			return (mapping.findForward("error.page"));
		}
		
		ServletContext servletContext = servlet.getServletContext();		
		boolean authenticationEnabled = (Boolean)servletContext.getAttribute ("authenticationEnabled");
		if (!authenticationEnabled) {
			errors.add ("auth-not-enabled", 
				new ActionError ("authentication.not.enabled"));
			saveErrors (request, errors);
			return (mapping.findForward("edit.access"));
		}
		
		AccessManagerForm amForm = (AccessManagerForm)form;
		
		Locale locale = getLocale(request);
		MessageResources messages = getResources(request);

		amForm.setActionPaths (accessManager.getActionPaths());
		amForm.setGuardedPaths (accessManager.getGuardedPaths());
		amForm.setRoleOptions(getRoleOptions (accessManager.getRoles()));

		// Query Args
		String command = request.getParameter("command");
		String path = request.getParameter("path");

		if (command == null || isCancelled (request)) {
			amForm.setPath  (null);
			amForm.setRole (null);
			amForm.setNewPath (false);
			amForm.setDescription(null);
			return (mapping.findForward("edit.access"));
		}
		
		// show request params excluding collapse bean and form input
		SchemEditUtils.showRequestParameters(request);
		if ("create".equals (command)) {
			amForm.setRole (null);
			amForm.setNewPath (true);
			amForm.setDescription(null);
		}
		
		else if ("edit".equals(command)) {
			// ActionPath actionPath = accessManager.getActionPath(path);
			GuardedPath guardedPath = accessManager.getGuardedPath (path);
			
			amForm.setDescription (guardedPath.getDescription());
			amForm.setRole (guardedPath.getRole().toString());
		}
		
		else if ("delete".equals(command)) {
			// ActionPath actionPath = accessManager.getActionPath(path);
			GuardedPath guardedPath = accessManager.getGuardedPath (amForm.getPath());
			if (guardedPath == null) {
				errors.add(ActionErrors.GLOBAL_ERROR,
					new ActionError("generic.error", "no guarded path exists for " + amForm.getPath()));
			}
			else {
				accessManager.deleteGuardedPath (guardedPath);
				try {
					accessManager.flush();
				} catch (Exception e) {
					prtln ("accessManager flush ERROR: " + e.getMessage());
				}
				amForm.setGuardedPaths(accessManager.getGuardedPaths());
				// bump collectionConfigMod to force update of searching controls.
				collectionRegistry.setCollectionConfigMod();
				errors.add(ActionErrors.GLOBAL_MESSAGE,
					new ActionError("generic.error", "path deleted " + amForm.getPath()));
			}
			amForm.setPath  (null);
			amForm.setRole (null);
			amForm.setNewPath (false);
			amForm.setDescription(null);
			saveErrors (request, errors);	
		}
		
		else if ("save".equals(command)) {
			if (path == null) {
				errors.add(ActionErrors.GLOBAL_ERROR,
					new ActionError("generic.error", "no path supplied"));
				saveErrors (request, errors);
				return (mapping.findForward("edit.access"));
			}			
			
			GuardedPath guardedPath = null;
			/* new path:
				- create a new path (checking to make sure it is a legal pattern and it does not already exist)
				- add new path to accessManager
			*/
			if (amForm.isNewPath()) {
				guardedPath = new GuardedPath(path);
				prtln ("!!!!!!! Finish up here !!!!!");
				amForm.setNewPath(false);
				accessManager.addGuardedPath(guardedPath);
			}
			else {
				guardedPath = accessManager.getGuardedPath (path);
			}

			guardedPath.setRole(Roles.toRole (amForm.getRole()));
			guardedPath.setDescription (amForm.getDescription());
			amForm.setPath (null);
			
			try {
				accessManager.flush();
			} catch (Exception e) {
				prtln ("accessManager flush ERROR: " + e.getMessage());
			}
			// update crucial data structures
			accessManager.alignActionsToGuardedPaths();
			updateSessionBeans();
			
			amForm.setGuardedPaths(accessManager.getGuardedPaths());
			// bump collectionConfigMod to force update of searching controls.
			collectionRegistry.setCollectionConfigMod();
		}	
		else {
			errors.add(ActionErrors.GLOBAL_ERROR,
				new ActionError("generic.error", "no path supplied"));
			saveErrors (request, errors);
		}
		
		// Forward control to the specified success URI
		return (mapping.findForward("edit.access"));
	}
	
	private List getRoleOptions (Collection roles) {
		List options = new ArrayList ();
		options.add (new LabelValueBean ("- none -", ""));
		for (Iterator i=roles.iterator();i.hasNext();) {
			String role = Roles.toString((Roles.Role)i.next());
			options.add (new LabelValueBean (role, role));
		}
		return options;
	}
	
	private void updateSessionBeans () {
		Iterator i = sessionRegistry.getSessionBeans().iterator();
		while (i.hasNext()) {
			SessionBean sb = (SessionBean)i.next();
			sb.setQuerySelectorsInitialized(false);
		}
	}
	
	static void prtln(String s) {
		while (s.length() > 0 && s.charAt(0) == '\n') {
			System.out.println ("");
			s = s.substring(1);
		}
		System.out.println("AccessManagerAction: " + s);
	}
	
	
}

