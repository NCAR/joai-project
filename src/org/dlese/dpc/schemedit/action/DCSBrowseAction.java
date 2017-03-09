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

import org.dlese.dpc.repository.*;
import org.dlese.dpc.index.*;
import org.dlese.dpc.schemedit.*;
import org.dlese.dpc.schemedit.dcs.*;
import org.dlese.dpc.schemedit.config.ErrorLog;
import org.dlese.dpc.schemedit.action.form.*;
import org.dlese.dpc.schemedit.security.user.User;
import org.dlese.dpc.index.reader.*;
import org.dlese.dpc.xml.*;
import org.dlese.dpc.xml.schema.*;
import org.apache.lucene.search.*;
import org.dlese.dpc.oai.*;
import org.dlese.dpc.webapps.tools.GeneralServletTools;

import java.util.*;
import java.io.*;
import java.util.Hashtable;
import java.util.Locale;
import javax.servlet.*;
import javax.servlet.http.*;
import java.net.URLEncoder;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionError;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionServlet;
import org.apache.struts.util.MessageResources;

/**
 *  A Struts Action for handling query requests for browsing a collection in the
 *  DCS. This class works in conjunction with the {@link org.dlese.dpc.schemedit.action.form.DCSBrowseForm}
 *  Struts form bean class.
 *
 * @author    Jonathan Ostwald
 */
public final class DCSBrowseAction extends DCSAction {

	private static boolean debug = true;

	SimpleLuceneIndex index;

	// --------------------------------------------------------- Public Methods

	/**
	 *  Processes the specified HTTP request and creates the corresponding HTTP
	 *  response by forwarding to a JSP that will create it. A {@link
	 *  org.dlese.dpc.index.SimpleLuceneIndex} must be available to this class via
	 *  a ServletContext attribute under the key "index." Returns an {@link
	 *  org.apache.struts.action.ActionForward} instance that maps to the Struts
	 *  forwarding name "browse.collection," which must be configured in
	 *  struts-config.xml to forward to the JSP page that will handle the request.
	 *
	 * @param  mapping               The ActionMapping used to select this instance
	 * @param  request               The HTTP request we are processing
	 * @param  response              The HTTP response we are creating
	 * @param  form                  The ActionForm for the given page
	 * @return                       The ActionForward instance describing where
	 *      and how control should be forwarded
	 * @exception  IOException       if an input/output error occurs
	 * @exception  ServletException  if a servlet exception occurs
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
		ActionErrors errors = initializeFromContext(mapping, request);
		if (!errors.isEmpty()) {
			saveErrors(request, errors);
			return (mapping.findForward("error.page"));
		}
		DCSBrowseForm browseForm = (DCSBrowseForm) form;

		browseForm.setRequest(request);
		ServletContext servletContext = servlet.getServletContext();

		browseForm.setContextURL(GeneralServletTools.getContextUrl(request));

		/* clearing search params effectively forgets the state of the search page.
		   this line commented out so search is remembered ... */
		// this.sessionBean.clearSearchParams();

		index = repositoryManager.getIndex();
		if (index == null) {
			throw new ServletException("The attribute \"index\" could not be found in the Servlet Context.");
		}
		browseForm.setNumDocs(index.getNumDocs());

		// SchemEditUtils.showRoleInfo (sessionUser, mapping);

		SchemEditUtils.showRequestParameters(request);
		User sessionUser = this.getSessionUser(request);
		try {
			List sets = repositoryService.getAuthorizedSets(sessionUser, this.requiredRole);

			String command = request.getParameter("command");
			if (command != null && command.length() > 0) {
				if (command.equals("clearFrameworkMessages")) {
					frameworkRegistry.clearLoadErrors();
					frameworkRegistry.clearLoadWarnings();
				}
			}

			browseForm.setSets(sets);
			browseForm.setUserRoles(this.getUserRolesMap(sessionUser, sets));

		} catch (NullPointerException e) {
			prtln("DCSBrowseAction caught exception.");
			e.printStackTrace();
			return mapping.findForward("browse.home");
		} catch (Throwable e) {
			prtln("DCSBrowseAction caught exception: " + e);
			return mapping.findForward("browse.home");
		}

		errors.add(indexCheck(form, request));
		// errors.add (getFrameworkRegistryErrors(form, request));
		if (!errors.isEmpty())
			saveErrors(request, errors);

		return mapping.findForward("browse.home");
	}


	/**
	 *  Return mapping from collectionKey to permission for authorized sets. Used
	 *  to display the sessionUser's role for each collection in the collections
	 *  table.
	 *
	 * @param  user      NOT YET DOCUMENTED
	 * @param  setInfos  NOT YET DOCUMENTED
	 * @return           The userRolesMap value
	 */
	private Map getUserRolesMap(User user, List setInfos) {

		HashMap map = new HashMap();
		for (Iterator i = setInfos.iterator(); i.hasNext(); ) {
			SetInfo setInfo = (SetInfo) i.next();
			String collection = setInfo.getSetSpec();
			if (user == null)
				map.put(collection, "<i>roles disabled</i>");
			else
				map.put(collection, user.getRole(collection));
		}
		return map;
	}


	private ActionErrors indexCheck(ActionForm form, HttpServletRequest request) {
		DCSBrowseForm browseForm = (DCSBrowseForm) form;
		ActionErrors errors = new ActionErrors();

		List sets = browseForm.getSets();
		for (Iterator i = sets.iterator(); i.hasNext(); ) {
			DcsSetInfo set = (DcsSetInfo) i.next();
			int numIndexed = set.getNumIndexedInt() + set.getNumIndexingErrorsInt();
			int numFiles = set.getNumFilesInt();
			// prtln ("indexCheck for " + set.getName() + " numIndexed + numErrors = " + numIndexed + ", numFiles = " + numFiles);
			if (numIndexed != numFiles) {
				errors.add("indexErrors", new ActionError("generic.error", set.getName()));
			}
		}
		return errors;
	}


	/* 	private ActionErrors getFrameworkRegistryErrors ( ActionForm form, HttpServletRequest request) {
		DCSBrowseForm browseForm = (DCSBrowseForm) form;
		ActionErrors errors = new ActionErrors ();

		ErrorLog errorLog = browseForm.getFrameworkLoadErrors ();
		if (errorLog != null && !errorLog.isEmpty()) {
			for (Iterator i=errorLog.getEntries().iterator();i.hasNext();) {
				ErrorLog.LogEntry entry = (ErrorLog.LogEntry) i.next();
				String name = entry.getName();
				String msg = entry.getMsg();
					errors.add ("frameworkMessages",
						new ActionError ("generic.error", name + ": " + msg));
			}
		}
		prtln ("frameworkRegistryErrors returning " + errors.size() + " messages");
		return errors;
	} */
	// -------------- Debug ------------------


	/**
	 *  Sets the debug attribute of the DCSBrowseAction class
	 *
	 * @param  isDebugOutput  The new debug value
	 */
	public static void setDebug(boolean isDebugOutput) {
		debug = isDebugOutput;
	}



	/**
	 *  Print a line to standard out.
	 *
	 * @param  s  The String to print.
	 */
	private void prtln(String s) {
		if (debug) {
			System.out.println("DCSBrowseAction: " + s);
		}
	}
}

