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
package org.dlese.dpc.oai.provider.action;

import org.dlese.dpc.oai.provider.action.form.*;
import org.dlese.dpc.repository.*;
import org.dlese.dpc.xml.XMLValidator;
import org.dlese.dpc.webapps.tools.GeneralServletTools;

import java.util.*;
import java.lang.*;
import java.io.*;
import java.util.Hashtable;
import java.util.Locale;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServletResponse;
import org.apache.struts.action.Action;
import org.apache.struts.action.ActionError;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionServlet;
import org.apache.struts.util.MessageResources;

/**
 *  Implementation of <strong>Action</strong> that handles the OAI-PMH search page.
 *
 * @author    John Weatherley
 */
public final class OaiPmhSearchAction extends Action {


	/**
	 *  Process the specified HTTP request, and create the corresponding HTTP response (or
	 *  forward to another web component that will create it). Return an <code>ActionForward</code>
	 *  instance describing where and how control should be forwarded, or <code>null</code>
	 *  if the response has already been completed.
	 *
	 * @param  mapping        The ActionMapping used to select this instance
	 * @param  response       The HTTP response we are creating
	 * @param  form           The ActionForm for the given page
	 * @param  req            DESCRIPTION
	 * @return                The ActionForward instance describing where and how control
	 *      should be forwarded
	 * @exception  Exception  DESCRIPTION
	 */
	public ActionForward execute(
	                             ActionMapping mapping,
	                             ActionForm form,
	                             HttpServletRequest req,
	                             HttpServletResponse response)
		 throws Exception {

		/*
		 *  Design note:
		 *  Only one instance of this class gets created for the app and shared by
		 *  all threads. To be thread-safe, use only local variables, not instance
		 *  variables (the JVM will handle these properly using the stack). Pass
		 *  all variables via method signatures rather than instance vars.
		 */
		// Extract attributes we will need
		
		Locale locale = getLocale(req);
		MessageResources messages = getResources(req);
		
		OaiPmhSearchForm opsf = (OaiPmhSearchForm) form;

		RepositoryManager rm =
			(RepositoryManager) servlet.getServletContext().getAttribute("repositoryManager");
		
		// Set up data for use int the bean.
		opsf.setContextURL(GeneralServletTools.getContextUrl(req));
		opsf.setOaiIdPfx(rm.getOaiIdPrefix());
		opsf.setOaiIdPfx(rm.getOaiIdPrefix());
		opsf.setAvailableSets(rm.getEnabledSets());
		opsf.setExampleId(rm.getExampleID());
		List formats = rm.getAvailableFormatsList();
		opsf.setAvailableFormats(formats);
		if(formats != null && formats.size() > 0)
			opsf.setExampleFormat((String)formats.get(0));
		
		// Add formats list
		// Add sets list
		// Add method in rm to get the BaseURL...

		try {

			// Handle admin actions:
			if (req.getParameter("command") != null) {
				if (req.getParameter("command").equals("Update index")) {

				}
			}

			if (req.getParameter("show") != null) {
				if (req.getParameter("show").equals("odl")) {
					return mapping.findForward("odl.search");
				}
			}						

			return mapping.findForward("oaipmh.search");
		} catch (NullPointerException e) {
			prtln("OaiPmhSearchAction caught null pointer exception.");
			e.printStackTrace();
			return mapping.findForward("oaipmh.search");		
		} catch (Throwable e) {
			prtln("OaiPmhSearchAction caught exception. " + e);
			return mapping.findForward("oaipmh.search");
		}
	}


	/**
	 *  Gets the index associated with a request parameter of the form myParameter[i] where
	 *  the collection index is indicated in brackets.
	 *
	 * @param  paramName  The request parameter String
	 * @return            The index value
	 */
	private final int getIndex(String paramName) {
		return getIntValue(paramName.substring(paramName.indexOf("[") + 1, paramName.indexOf("]")));
	}


	private final int getIntValue(String isInt) {
		try {
			return Integer.parseInt(isInt);
		} catch (Throwable e) {
			return -1;
		}
	}


	/**
	 *  DESCRIPTION
	 *
	 * @param  s  DESCRIPTION
	 */
	private void prtln(String s) {
		System.out.println(s);
	}	
}

