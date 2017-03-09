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
import org.dlese.dpc.schemedit.action.form.*;
import org.dlese.dpc.schemedit.*;
import org.dlese.dpc.schemedit.dcs.*;
import org.dlese.dpc.schemedit.config.*;
import org.dlese.dpc.index.reader.*;
import org.dlese.dpc.xml.*;
import org.apache.lucene.search.*;
import org.dlese.dpc.oai.*;
import org.dlese.dpc.webapps.tools.GeneralServletTools;

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
import org.dom4j.Document;
import org.dom4j.DocumentException;

/**
 *  A Struts Action for handling query requests that access a {@link
 *  org.dlese.dpc.index.SimpleLuceneIndex}. This class works in conjunction with the
 *  {@link org.dlese.dpc.schemedit.action.form.StaticRecordForm} Struts form bean class.
 *
 *
 *
 * @author    Jonathan Ostwald
 */
public final class StaticRecordAction extends DCSAction {

	private static boolean debug = false;

	// --------------------------------------------------------- Public Methods

	/**
	 *  Processes the specified HTTP request and creates the corresponding HTTP response by
	 *  forwarding to a JSP that will create it. A {@link
	 *  org.dlese.dpc.index.SimpleLuceneIndex} must be available to this class via a
	 *  ServletContext attribute under the key "index." Returns an {@link
	 *  org.apache.struts.action.ActionForward} instance that maps to the Struts forwarding
	 *  name "browse.query," which must be configured in struts-config.xml to forward to the
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
		StaticRecordForm srForm = (StaticRecordForm) form;
		 
		ActionErrors errors = initializeFromContext (mapping, request);
		if (!errors.isEmpty()) {
			saveErrors (request, errors);
			return (mapping.findForward("error.page"));
		}	
		
		SchemEditUtils.showRequestParameters(request);

		
		try {
			String id = request.getParameter ("id");
			
			if (id != null && id.length() > 0) {
				return viewRecordById (mapping, form, request, response);
			}
			
			else  {
				srForm.setResult (null);
					errors.add("message", new ActionError("generic.message",
						"No ID indicated. Please supply a record ID."));
					saveErrors(request, errors);
					return mapping.findForward("static.record");
				}

		} catch (NullPointerException e) {
			prtln("StaticRecordAction caught exception.");
			e.printStackTrace();
			return mapping.findForward("error.page");
		} catch (Throwable e) {
			prtln("StaticRecordAction caught exception: " + e);
			return mapping.findForward("error.page");
		}
	}
	
	private ActionForward viewRecordById ( ActionMapping mapping,
	                             ActionForm form,
	                             HttpServletRequest request,
	                             HttpServletResponse response ) throws ServletException {
		
		StaticRecordForm srForm = (StaticRecordForm) form;
		ActionErrors errors = new ActionErrors ();

		String id = request.getParameter ("id");
		
		SearchHelper searchHelper = getSessionBean(request).getSearchHelper();
		ResultDoc result = searchHelper.getResultDoc (id);
		
		if (result == null) {
			srForm.setResult (null);
			errors.add("message", new ActionError("generic.message",
				"No record was found in the index for ID \"" + id + "\""));
			saveErrors(request, errors);
			return mapping.findForward("static.record");
		}
		
		// initialize the srForm.docMap
		XMLDocReader resultDocReader = (XMLDocReader) result.getDocReader();
 		String xmlFormat = resultDocReader.getNativeFormat();
		// String collection = resultDocReader.getCollection();
 		MetaDataFramework framework = this.getMetaDataFramework(xmlFormat);

		srForm.setResult(result);
		srForm.setFramework(framework);
		
		XMLDocReader annotatedItem = null;
		prtln ("xmlFormat: " + xmlFormat);
		if (xmlFormat.equals ("dlese_anno")) {
			DleseAnnoDocReader annoReader = (DleseAnnoDocReader)resultDocReader;
			String annotatedItemId = annoReader.getItemId();
			ResultDoc annotatedItemResult = searchHelper.getResultDoc (annotatedItemId);
			if (annotatedItemResult != null) {
				annotatedItem = (XMLDocReader) annotatedItemResult.getDocReader();
				if (annotatedItem != null)
					prtln ("annotatedItem: " + annotatedItem.getId());
			}
			else {
				prtln ("annotedItemResult not found for " + annoReader.getItemId());
			}
		}
		srForm.setAnnotatedItem (annotatedItem);
		
/* 		srForm.setDcsSetInfo(SchemEditUtils.getDcsSetInfo(collection, rm));
		srForm.setDcsDataRecord(dcsDataManager.getDcsDataRecord(id, rm));
		srForm.setSchemaHelper(framework.getSchemaHelper());
		srForm.setDocMap(doc); */

			
		return mapping.findForward("static.record");
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
			System.out.println("StaticRecordAction: " + s);
	}
}

