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

import org.dlese.dpc.dds.action.form.*;

import org.dlese.dpc.oai.*;
import org.dlese.dpc.xml.*;
import org.dlese.dpc.repository.*;
import org.dlese.dpc.index.reader.*;
import org.dlese.dpc.index.*;
import org.dlese.dpc.index.writer.*;
import org.dlese.dpc.webapps.tools.*;
import org.dlese.dpc.webapps.servlets.filters.GzipFilter;
import org.dlese.dpc.services.mmd.Query;

import java.util.*;
import java.text.*;
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
 *  An <strong>Action</strong> that handles DDS related reporting requests.
 *
 * @author    John Weatherley
 * @see       org.dlese.dpc.dds.action.form.DDSReportingForm
 */
public final class DDSReportingAction extends Action {


	private static boolean debug = true;
	private SimpleLuceneIndex webLogIndex = null;

	// --------------------------------------------------------- Public Methods

	/**
	 *  Processes the DDS request by forwarding to the appropriate corresponding
	 *  JSP page for rendering.
	 *
	 * @param  mapping        The ActionMapping used to select this instance
	 * @param  request        The HTTP request we are processing
	 * @param  response       The HTTP response we are creating
	 * @param  form           The ActionForm for the given page
	 * @return                The ActionForward instance describing where and how control
	 *      should be forwarded
	 * @exception  Exception  If error.
	 */
	public ActionForward execute(
	                             ActionMapping mapping,
	                             ActionForm form,
	                             HttpServletRequest request,
	                             HttpServletResponse response)
		 throws Exception {
		/*
		 *  Design note:
		 *  Only one instance of this class gets created for the app and shared by
		 *  all threads. To be thread-safe, use only local variables, not instance
		 *  variables (the JVM will handle these properly using the stack). Pass
		 *  all variables via method signatures rather than instance vars.
		 */
		try {

			DDSReportingForm drf = (DDSReportingForm) form;

			RepositoryManager rm =
				(RepositoryManager) servlet.getServletContext().getAttribute("repositoryManager");

			String outputType = request.getParameter("outputType");
				
			String indexToUse = drf.getIndexToUse();
			if(indexToUse != null && indexToUse.equalsIgnoreCase("webLogIndex")){
				String webLogIndexDir = 
					GeneralServletTools.getAbsolutePath( (String)servlet.getServletContext().getInitParameter("webLogIndexLocation"),servlet.getServletContext() );
				if(webLogIndexDir != null){
					// Make sure cleanup happens on the previous webLogIndex by calling finalization.
					//System.gc();
					//System.runFinalization();
					if(webLogIndex == null)
						webLogIndex = new SimpleLuceneIndex(webLogIndexDir);
					//else
						//webLogIndex.readNewIndex();
					drf.setIndex(webLogIndex);					
				}
			}
			else
				drf.setIndex(rm.getIndex());
			
			String verb = request.getParameter("verb");
			if (verb == null) {
				//prtln("Error: verb null");
				return (mapping.findForward("ddsreporting.index"));
			}
			// Show term counts:
			else if (verb.equals("TermCount")) {
				if(outputType != null && outputType.equals("csv"))
					return (mapping.findForward("ddsreporting.termcount.csv"));
				return (mapping.findForward("ddsreporting.termcount"));
			}	
			// Show a list of fields:
			else if (verb.equals("ListFields")) {
				return (mapping.findForward("ddsreporting.listfields"));
			}				
			// View stems:
			else if (verb.equals("ViewStems")) {
				return (mapping.findForward("ddsreporting.viewstems"));
			}
			// View dup id's:
			else if (verb.equals("ViewDupIds")) {
				return (mapping.findForward("ddsreporting.viewdupids"));
			}
			// View IDMapper report for IDs:
			else if (verb.equals("ViewIDMapper")) {
				//drf.setId(request.getParameter("id"));
				//drf.setCollection(request.getParameter("collection"));				
				Query.reloadIdExclusionDocument();
				return (mapping.findForward("ddsreporting.viewidmapper"));
			}					
			else {
				//prtln("Error: bad verb");
				return (mapping.findForward("ddsreporting.index"));
			}
			

		} catch (NullPointerException npe) {
			prtln("DDSReportingAction caught exception. " + npe);
			npe.printStackTrace();
			return (mapping.findForward("ddsreporting.index"));
		} catch (Throwable e) {
			prtln("DDSReportingAction caught exception. " + e);
			return (mapping.findForward("ddsreporting.index"));
		}
	}


	// --------------- Debug output ------------------

	/**
	 *  Return a string for the current time and date, sutiable for display in log files and
	 *  output to standout:
	 *
	 * @return    The dateStamp value
	 */
	protected final static String getDateStamp() {
		return
			new SimpleDateFormat("MMM d, yyyy h:mm:ss a zzz").format(new Date());
	}


	/**
	 *  Output a line of text to error out, with datestamp.
	 *
	 * @param  s  The text that will be output to error out.
	 */
	protected final void prtlnErr(String s) {
		System.err.println(getDateStamp() + " " + s);
	}



	/**
	 *  Output a line of text to standard out, with datestamp, if debug is set to true.
	 *
	 * @param  s  The String that will be output.
	 */
	protected final void prtln(String s) {
		if (debug) {
			System.out.println(getDateStamp() + " " + s);
		}
	}


	/**
	 *  Sets the debug attribute of the object
	 *
	 * @param  db  The new debug value
	 */
	public static void setDebug(boolean db) {
		debug = db;
	}
}


