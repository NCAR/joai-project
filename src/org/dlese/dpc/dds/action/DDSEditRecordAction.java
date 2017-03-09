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
import org.dlese.dpc.webapps.servlets.filters.GzipFilter;

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
 *  An <strong>Action</strong> that handles editing recrods in DDS.
 *
 * @author    John Weatherley
 * @see       org.dlese.dpc.dds.action.form.DDSEditRecordForm
 */
public final class DDSEditRecordAction extends Action {

	private static boolean debug = true;


	// --------------------------------------------------------- Public Methods

	/**
	 *  Processes the DDS request by forwarding to the appropriate corresponding
	 *  JSP page for rendering.
	 *
	 * @param  mapping        The ActionMapping used to select this instance
	 * @param  request        The HTTP request we are processing
	 * @param  response       The HTTP response we are creating
	 * @param  form           The ActionForm for the given page
	 * @return                The ActionForward instance describing where and how
	 *      control should be forwarded
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

			DDSEditRecordForm def = (DDSEditRecordForm) form;

			RepositoryManager rm =
					(RepositoryManager) servlet.getServletContext().getAttribute("repositoryManager");

			SimpleLuceneIndex index = rm.getIndex();

			String verb = request.getParameter("verb");
			if (verb == null) {
				return (mapping.findForward("ddseditrecord.index"));
			}
			// Get Record:
			else if (verb.equals("GetRecord")) {
				return doGetRecord(def, rm, mapping, request, response);
			}
			// Put Record:
			else if (verb.equals("PutRecord")) {
				return doPutRecord(def, rm, mapping, request, response);
			}
			// Delete Record:
			else if (verb.equals("DeleteRecord")) {
				return doDeleteRecord(def, rm, mapping, request, response);
			}
			else {
				return (mapping.findForward("ddseditrecord.index"));
			}

		} catch (NullPointerException npe) {
			prtln("DDSEditRecordAction caught exception. " + npe);
			npe.printStackTrace();
			return (mapping.findForward("ddsreporting.index"));
		} catch (Throwable e) {
			prtln("DDSEditRecordAction caught exception. " + e);
			return (mapping.findForward("ddsreporting.index"));
		}
	}

	private ActionForward doGetRecord(
			DDSEditRecordForm def,
			RepositoryManager rm,
			ActionMapping mapping,
			HttpServletRequest request,
			HttpServletResponse response) {
		ActionErrors errors = new ActionErrors();

		String id = request.getParameter("id");
		
		if(id == null){
			errors.add("error", new ActionError("generic.error", "No record ID provided!"));
			saveErrors(request, errors);
			return (mapping.findForward("ddseditrecord.index"));			
		}
		
		ResultDocList results = rm.getIndex().searchDocs("id:" + SimpleLuceneIndex.encodeToTerm(id));
		if(results == null || results.size() == 0){
			errors.add("error", new ActionError("generic.error", "Could not locate record ID " + id));
			saveErrors(request, errors);
			return (mapping.findForward("ddseditrecord.index"));			
		}
		
		// Populate our bean with this record's data:
		XMLDocReader xmlDocReader = (XMLDocReader)results.get(0).getDocReader();
		def.setCollection(xmlDocReader.getCollection());
		def.setRecordXml(xmlDocReader.getXml());
		//def.setRecordId(xmlDocReader.getId()); - not necessary - obtained in the XML
		def.setXmlFormat(xmlDocReader.getNativeFormat());

		saveErrors(request, errors);

		return (mapping.findForward("ddseditrecord.index"));
	}	
	

	private ActionForward doPutRecord(
			DDSEditRecordForm def,
			RepositoryManager rm,
			ActionMapping mapping,
			HttpServletRequest request,
			HttpServletResponse response) {
		ActionErrors errors = new ActionErrors();

		String recordXml = request.getParameter("recordXml");
		String recordId = request.getParameter("recordId");
		String collection = request.getParameter("collection");
		String xmlFormat = request.getParameter("xmlFormat");

		if (recordXml != null && collection != null && xmlFormat != null) {
			try {
				FileIndexingPlugin indexingPlugin = new SimpleFileIndexingPlugin();
				rm.putRecord(recordXml, xmlFormat, collection, recordId, indexingPlugin, true);
				errors.add("message", new ActionError("generic.message", "Record saved successfully"));
				errors.add("redirect.fullview", new ActionError("generic.message", ""));
			} catch (Throwable e) {
				errors.add("error", new ActionError("generic.error", "Could not save record: " + e.getMessage()));
				e.printStackTrace();
			}
		}

		saveErrors(request, errors);

		return (mapping.findForward("ddseditrecord.index"));
	}


	private ActionForward doDeleteRecord(
			DDSEditRecordForm def,
			RepositoryManager rm,
			ActionMapping mapping,
			HttpServletRequest request,
			HttpServletResponse response) {

		ActionErrors errors = new ActionErrors();

		String deleteRecord = request.getParameter("deleteRecord");

		if (deleteRecord != null) {
			if (deleteRecord.trim().length() == 0)
				errors.add("error", new ActionError("generic.error", "Please enter an ID"));
			else {
				try {
					if( rm.deleteRecord(deleteRecord) )
						errors.add("message", new ActionError("generic.message", "Delete completed normally"));
					else	
						errors.add("error", new ActionError("generic.message", "Record ID " + deleteRecord + " not found. No delete was performed."));
				} catch (Throwable e) {
					errors.add("error", new ActionError("generic.error", "Could not delete record: " + e.getMessage()));
				}
			}
		}

		saveErrors(request, errors);

		return (mapping.findForward("ddseditrecord.index"));
	}


	// --------------- Debug output ------------------

	/**
	 *  Return a string for the current time and date, sutiable for display in log
	 *  files and output to standout:
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
	 *  Output a line of text to standard out, with datestamp, if debug is set to
	 *  true.
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


