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

import org.dlese.dpc.serviceclients.metaextract.MetaExtractService;
import org.dlese.dpc.repository.*;
import org.dlese.dpc.index.*;
import org.dlese.dpc.schemedit.*;
import org.dlese.dpc.schemedit.url.UrlHelper;
import org.dlese.dpc.schemedit.dcs.*;
import org.dlese.dpc.schemedit.config.*;
import org.dlese.dpc.schemedit.security.user.User;
import org.dlese.dpc.schemedit.action.form.*;
import org.dlese.dpc.xml.*;
import org.dlese.dpc.xml.schema.*;
import org.dlese.dpc.util.strings.*;

import org.dom4j.Document;

import java.util.*;
import java.net.*;
import java.io.*;
import java.text.*;

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
 *  A Struts Action controlling interaction during creation of records for the
 *  "mast" framework, including iteraction with "metaextract" service to
 *  populate record fields.<p>
 *
 *  Ensures that new records have unique URLs across their collection.
 *
 * @author     Jonathan Ostwald
 */
public final class CreateMastRecordAction extends CreateADNRecordAction {

	private static boolean debug = false;


	/**
	 *  Gets the xmlFormat attribute of the CreateMastRecordAction object
	 *
	 * @return    The xmlFormat value
	 */
	protected String getXmlFormat() {
		return "mast";
	}


	/**
	 *  Gets the createForward attribute of the CreateMastRecordAction object
	 *
	 * @param  mapping  NOT YET DOCUMENTED
	 * @return          The createForward value
	 */
	protected ActionForward getCreateForward(ActionMapping mapping) {
		return mapping.findForward("mast.create");
	}


	/**
	 *  Gets the confirmForward attribute of the CreateMastRecordAction object
	 *
	 * @param  mapping   the ActionMapping
	 * @param  carForm   the ActionForm
	 * @param  request   the Request
	 * @param  response  the Response
	 * @return           The confirmForward value
	 */
	protected ActionForward getConfirmForward(ActionMapping mapping,
	                                          CreateADNRecordForm carForm,
	                                          HttpServletRequest request,
	                                          HttpServletResponse response) {
		String editUrl = "/editor/edit.do?command=edit&recId=" + carForm.getRecId();
		String redirectUrl = request.getContextPath() + editUrl;
		prtln("\nREDIRECTING TO " + redirectUrl);
		try {
			response.sendRedirect(redirectUrl);
		} catch (IOException e) {
			prtln("getConfirmForward error: " + e.getMessage());
		}
		return null;
	}

	// --------------------------------------------------------- Public Methods

	/**
	 *  Create an empty collection metadata document and populate from ActionForm
	 *  (carForm). If MetaExtract is unable to provide an instance Document (e.g.,
	 *  it returns an error), then create a new instanceDoc and pass the
	 *  metaextract message back to caller as an ActionError.
	 *
	 * @param  carForm        the ActionForm
	 * @param  framework      the MetaDataFramework of the record being created
	 * @param  errors         messages passed back to caller
	 * @return                Description of the Return Value
	 * @exception  Exception  Description of the Exception
	 */

	protected Document makeRecordDoc(CreateADNRecordForm carForm,
									 MetaDataFramework framework,
									 ActionMapping mapping,
									 HttpServletRequest request,
									 HttpServletResponse response)
		 throws Exception {

		// create a miminal document and then insert values from carForm
		String id = carForm.getRecId();
		String query = carForm.getPrimaryUrl(); // this has been validated and trimmed for us
		// QUESTION: has it been normalized yet? and does this matter (e.g., it is done later)

		// if we've already tried and failed to get a metaextract response, carForm.getBogusUrl()
		// will be non-null. if this is the case, then just create a record from scratch
		
		Document doc = null;
		DocMap docMap = null;
		
		if (carForm.getBogusUrl() != null && carForm.getBogusUrl().equals(query)) {
			String collection = carForm.getCollection();
			CollectionConfig collectionConfig = this.collectionRegistry.getCollectionConfig(collection, false);
			if (collectionConfig == null)
				throw new Exception ("Unable to find collection configuration for \"" + collection + "\"");
			doc = framework.makeMinimalRecord(id, collectionConfig);
			docMap = new DocMap(doc, framework.getSchemaHelper());
			carForm.setBogusUrl(null);
		}
		else {
			carForm.setBogusUrl(null);
			// HERE IS WHERE WE OBTAIN A RECORD FROM METAEXTRACT!!!
			MetaExtractService metaExtractService = new MetaExtractService("test", "p");
			if (metaExtractService == null)
				throw new Exception("could not instantiate web service");	
			
			Document responseDoc = null;
			try {
				responseDoc = metaExtractService.getResponse(query);
			} catch (Exception e) {
				String errMsg = "MetaExtract unable to process the resource URL (" + e.getMessage() + "). ";
				errMsg += " Please verify it is entered correctly and the page exists.";
				prtln(errMsg);
				carForm.setBogusUrl(query);
				throw new Exception (errMsg);
			}

			try {
				doc = Dom4jUtils.localizeXml(responseDoc);
				prtln("\n-----------------------------");
				prtln("Response from METAEXTRACT");
				prtln(Dom4jUtils.prettyPrint(doc));
				prtln("-----------------------------\n");
		
				// use docMap as wraper for Document
				docMap = new DocMap(doc, framework.getSchemaHelper());
				// load the metadata document with values from the form
				//   pattern for smartPut: docMap.smartPut(xpath, value)
			} catch (Throwable t) {
				carForm.setBogusUrl(query);
				throw new Exception ("Unable to process metaextract response: " + t.getMessage());
			}
		}
	
		// populate the id and url fields
		try {

			String idPath = framework.getNamedSchemaPathXpath("id");
			docMap.smartPut(idPath, id);

			docMap.smartPut("/record/general/url", query);

		} catch (Exception e) {
			throw new Exception("error populating record: " + e);
		}
		
		// DO WE WANT TO INSERT USER INFO INTO THE MAST INSTANCE DOC??
 		try {
			User sessionUser = this.getSessionUser(request);
			if (sessionUser != null)
				insertUserInfo(docMap, sessionUser);
		} catch (Throwable t) {
			prtln("insertUserInfo error: " + t.getMessage());
			t.printStackTrace();
		}
		
		// now prepare document to write to file by inserting namespace information
		doc = framework.getWritableRecord(doc);
		return doc;
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  docMap         NOT YET DOCUMENTED
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	private void insertUserInfo(DocMap docMap, User user) throws Exception {
		docMap.smartPut("/record/contributions/cataloging/cataloging", user.getFullName());
	}


	/**
	 *  Validate the input from user. Put changed or default values into carForm.
	 *  After this method returns, the form, carForm (rather than request), is used
	 *  to process user input
	 *
	 * @param  request        Description of the Parameter
	 * @param  carForm        Description of the Parameter
	 * @param  mastFramework  NOT YET DOCUMENTED
	 * @return                Description of the Return Value
	 */
	protected ActionErrors validateForm(HttpServletRequest request,
	                                    CreateADNRecordForm carForm,
	                                    MetaDataFramework mastFramework) {
		ActionErrors errors = new ActionErrors();

		String primaryUrl = request.getParameter("primaryUrl");
		// does url value exist?
		if ((primaryUrl != null) && (primaryUrl.trim().equals(""))) {
			errors.add("primaryUrl", new ActionError("field.required", "Url"));
			return errors;
		}

		try {
			primaryUrl = UrlHelper.normalize(primaryUrl);
			UrlHelper.validateUrl(primaryUrl);
		} catch (MalformedURLException e) {
			if (e.getMessage() == null)
				errors.add("primaryUrl", new ActionError("generic.error", "malformed url"));
			else
				errors.add("primaryUrl", new ActionError("generic.error", "malformed url: " + e.getMessage()));
			return errors;
		}

		primaryUrl = primaryUrl.trim();
		carForm.setPrimaryUrl(primaryUrl);

		//  check dups if this framework has specified primaryUrl as a "uniqueUrl" valueType
		if (mastFramework.getUniqueUrlPath() != null) {
			prtln("uniqueUrlPath is NOT NULL - checking for dups");
			List dups = repositoryService.getDups(primaryUrl, carForm.getCollection());
			carForm.setDups(dups);
			if (dups.size() > 0) {
				errors.add("primaryUrl", new ActionError("invalid.url", "URL already cataloged in this collection"));
			}
		}

		return errors;
	}


	/**
	 *  Sets the debug attribute of the CreateMastRecordAction class
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
			System.out.println("CreateMastRecordAction: " + s);
		}
	}
}

