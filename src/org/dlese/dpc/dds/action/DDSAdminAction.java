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



import org.dlese.dpc.index.*;

import org.dlese.dpc.dds.*;

import org.dlese.dpc.dds.action.form.*;

import org.dlese.dpc.vocab.*;

import org.dlese.dpc.vocab.MetadataVocab;

import org.dlese.dpc.repository.*;



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



/**

 *  Implementation of <strong>Action</strong> that handles administration of the DDS.

 *

 * @author     John Weatherley

 */

public final class DDSAdminAction extends Action {



	// --------------------------------------------------------- Public Methods



	/**

	 *  Process the specified HTTP request, and create the corresponding HTTP response (or forward to another web

	 *  component that will create it). Return an <code>ActionForward</code> instance describing where and how

	 *  control should be forwarded, or <code>null</code> if the response has already been completed.

	 *

	 * @param  mapping               The ActionMapping used to select this instance

	 * @param  request               The HTTP request we are processing

	 * @param  response              The HTTP response we are creating

	 * @param  form                  The ActionForm for the given page

	 * @return                       The ActionForward instance describing where and how control should be

	 *      forwarded

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

		// Extract attributes we will need

		//Locale locale = getLocale(request);

		//MessageResources messages = getResources();





		RepositoryManager rm =

			(RepositoryManager) servlet.getServletContext().getAttribute("repositoryManager");

		SimpleLuceneIndex index =

			(SimpleLuceneIndex) servlet.getServletContext().getAttribute("index");

		MetadataVocab vocab = (MetadataVocab) servlet.getServletContext().getAttribute("MetadataVocab");



		DDSAdminForm adminForm = (DDSAdminForm) form;



		adminForm.setIndex(index);

		adminForm.setMetadataVocab(vocab);



		String paramVal = null;

		try {



			// Handle admin actions:

			if (request.getParameter("command") != null) {

				paramVal = request.getParameter("command");



				if (paramVal.equals("Update index")) {

					rm.indexFiles(null,false);

					adminForm.setShowNumChanged(true);

					adminForm.setMessage(

						DDSServlet.getDateStamp() +

						". Discovery is synchronizing it's index with the metadata files...");

					return mapping.findForward("admin.index");

				}

				if (paramVal.equals("Start File Tester")) {

					adminForm.setMessage("Stoped moving files");

					//fileIndexingService.startTester(servlet.getServletContext().getRealPath("/"));

					return mapping.findForward("admin.index");

				}

				if (paramVal.equals("Stop File Tester")) {

					adminForm.setMessage("Stoped moving files");

					//fileIndexingService.stopTester();

					return mapping.findForward("admin.index");

				}

			}



			// No recognizable param existed:

			else if (request.getParameterNames().hasMoreElements()) {

				adminForm.setMessage("The request is not valid in this context.");

				return mapping.findForward("admin.index");

			}



			// If there were no parameters at all:

			return mapping.findForward("admin.index");

		} catch (Throwable t) {

			adminForm.setMessage("There was a server problem: " + t.getMessage());

			return mapping.findForward("admin.index");

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



