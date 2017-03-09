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
package org.dlese.dpc.dds.action;

import org.dlese.dpc.repository.*;
import org.dlese.dpc.index.*;
import org.dlese.dpc.dds.action.form.*;
import org.dlese.dpc.index.reader.*;
import org.dlese.dpc.xml.*;
import org.apache.lucene.search.*;
import org.dlese.dpc.oai.*;
import org.dlese.dpc.vocab.*;

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
import org.apache.struts.util.MessageResources;
import java.net.URLEncoder;

/**
 *  A Struts Action for managing the items in the DDS collections.
 *
 *
 * @author    John Weatherley
 */
public final class DDSManageCollectionsAction extends Action {

	private static boolean debug = false;


	// --------------------------------------------------------- Public Methods

	/**
	 *  Processes the specified HTTP request and creates the corresponding HTTP response by
	 *  forwarding to a JSP that will create it. A {@link
	 *  org.dlese.dpc.index.SimpleLuceneIndex} must be available to this class via a
	 *  ServletContext attribute under the key "index." Returns an {@link
	 *  org.apache.struts.action.ActionForward} instance that maps to the Struts forwarding
	 *  name "simple.query," which must be configured in struts-config.xml to forward to the
	 *  JSP page that will handle the request.
	 *
	 * @param  mapping               The ActionMapping used to select this instance
	 * @param  request               The HTTP request we are processing
	 * @param  response              The HTTP response we are creating
	 * @param  form                  The ActionForm for the given page
	 * @return                       The ActionForward instance describing where and how
	 *      control should be forwarded
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
		DDSManageCollectionsForm mf = (DDSManageCollectionsForm) form;
		

		RepositoryManager rm =
			(RepositoryManager) servlet.getServletContext().getAttribute("repositoryManager");

		
		// Handle editing/changing record status
		if (request.getParameter("recs") != null) {
			return handleRecordStatusEditing(mapping, (DDSManageCollectionsForm) form, request, response, rm);
		}
		
		return mapping.findForward("edit.record.status");
	}

	/**
	 *  Handle a request to search over metadata collections and forwared to the appropriate
	 *  jsp page to render the response.
	 *
	 * @param  mapping               The ActionMapping used to select this instance
	 * @param  request               The HTTP request we are processing
	 * @param  response              The HTTP response we are creating
	 * @param  mf             		The DDSManageCollectionsForm
	 * @param  rm                    The RepositoryManager
	 * @return                       The ActionForward instance describing where and how
	 *      control should be forwarded
	 * @exception  IOException       if an input/output error occurs
	 * @exception  ServletException  if a servlet exception occurs
	 */
	public ActionForward handleRecordStatusEditing(
	                                                 ActionMapping mapping,
	                                                 DDSManageCollectionsForm mf,
	                                                 HttpServletRequest request,
	                                                 HttpServletResponse response,
	                                                 RepositoryManager rm)
		 throws IOException, ServletException {
			 
		ActionErrors errors = new ActionErrors();
		
		SimpleLuceneIndex index =
			(SimpleLuceneIndex) servlet.getServletContext().getAttribute("index");

		if (index == null)
			throw new ServletException("The attribute \"index\" could not be found in the Servlet Context.");
		
		errors.add("message", new ActionError("generic.message",
			"editing record number " + request.getParameter("recs")));
		saveErrors(request, errors);


		return mapping.findForward("edit.record.status");		
	}



	// -------------- Debug ------------------


	public static void setDebug(boolean isDebugOutput) {
		debug = isDebugOutput;
	}



	/**
	 *  Print a line to standard out.
	 *
	 * @param  s  The String to print.
	 */
	private void prtln(String s) {
		if (debug)
			System.out.println("DDSManageCollectionsAction: " + s);
	}
}

