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
package org.dlese.dpc.schemedit.ndr.action;

import org.dlese.dpc.schemedit.ndr.action.form.FileUploadForm;
import org.dlese.dpc.schemedit.SchemEditUtils;
import org.dlese.dpc.ndr.toolkit.NDRToolkit;

import org.nsdl.repository.model.types.Resource;

import javax.servlet.*;
import javax.servlet.http.*;

import org.apache.struts.upload.FormFile;
import org.apache.struts.upload.CommonsMultipartRequestHandler;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionError;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionServlet;

import java.util.*;
import java.io.BufferedReader;
import java.nio.CharBuffer;

/**
 *  Struts Action to control the uploading of primary content into an NDR
 *  instance. Accepts request parameter "forwardPath", which (if present) will
 *  cause successful uploads to forward to that path (context-sensitive). This
 *  enables this action to be used in a variety of situations, such as
 *  "inputHelpers" that can specify a "forwardPath" and thus control the overall
 *  behaviour of the upload process.
 *
 * @author     Jonathan Ostwald
 */
public class FileUploadAction extends Action {

	private static boolean debug = true;


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  mapping        the ActionMapping
	 * @param  form           the ActionForm
	 * @param  request        the Request
	 * @param  response       the Response
	 * @return                NOT YET DOCUMENTED
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public ActionForward execute(ActionMapping mapping,
	                             ActionForm form,
	                             HttpServletRequest request,
	                             HttpServletResponse response) throws Exception {

		FileUploadForm uploadForm = (FileUploadForm) form;
		ActionErrors errors = new ActionErrors();
		prtln("\nFileUploadAction");
		String command = request.getParameter("command");
		SchemEditUtils.showRequestParameters(request, null);

		if (command != null) {
			try {

				if (command.equals("upload")) {
					return doUpload(mapping, form, request, response);
					// return doUploadFake (mapping, form, request, response);
				}
				else
					throw new Exception("unrecognized command: \"" + command + "\"");
			} catch (Exception e) {
				errors.add("error", new ActionError("generic.error", e.getMessage()));
				saveErrors(request, errors);
			}
		}

		return mapping.findForward("ndr.upload.form");
	}


	/**
	 *  Uploads file from request and
	 *
	 * @param  mapping        the ActionMapping
	 * @param  form           the ActionForm
	 * @param  request        the Request
	 * @param  response       the Response
	 * @return                NOT YET DOCUMENTED
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	ActionForward doUpload(ActionMapping mapping,
	                       ActionForm form,
	                       HttpServletRequest request,
	                       HttpServletResponse response) throws Exception {
		prtln("doUpload");
		ActionErrors errors = new ActionErrors();
		FileUploadForm uploadForm = (FileUploadForm) form;
		// clear values to be calculated ...
		// uploadForm.setUploadedResourceHandle(null);
		uploadForm.setContentURL(null);

		// Process the FormFile
		FormFile myFile = uploadForm.getMyFile();
		String contentType = myFile.getContentType();
		String fileName = myFile.getFileName();
		int fileSize = myFile.getFileSize();
		byte[] fileData = myFile.getFileData();
		prtln("contentType: " + contentType);
		prtln("fileName: " + fileName);
		prtln("fileSize: " + fileSize);

		try {
			NDRToolkit tk = new NDRToolkit();

			// hack to use same resource over and over
			/* 			String tmpResourceHandle = "2200/test.20090410170951993T"; // TEMPORARY
			Resource cannedResource = tk.getResource(tmpResourceHandle); */
			// production:
			Resource resource = tk.newResource();
			String uploadedResourceHandle = tk.setResourceContent(resource, fileData, fileName);

			// must UPDATE to get the contentURL property!
			Resource updatedResource = tk.getResource(uploadedResourceHandle);
			uploadForm.setContentURL(updatedResource.getContentURL().toString());

		} catch (Throwable t) {
			t.printStackTrace();
			errors.add("error", new ActionError("generic.error", t.getMessage()));
			saveErrors(request, errors);
			return mapping.findForward("ndr.upload.form");
		}

		String forwardPath = request.getParameter("forwardPath");
		if (forwardPath != null && forwardPath.trim().length() > 0)
			return new ActionForward(forwardPath);
		else
			return mapping.findForward("ndr.upload.confirm");
	}


	/**
	 *  A stub used to develop user-interaction without requiring actuall
	 *  uploading.
	 *
	 * @param  mapping        the ActionMapping
	 * @param  form           the ActionForm
	 * @param  request        the Request
	 * @param  response       the Response
	 * @return                NOT YET DOCUMENTED
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	ActionForward doUploadFake(ActionMapping mapping,
	                           ActionForm form,
	                           HttpServletRequest request,
	                           HttpServletResponse response) throws Exception {
		prtln("doUploadFake");
		ActionErrors errors = new ActionErrors();
		try {
			FileUploadForm uploadForm = (FileUploadForm) form;
			uploadForm.setContentURL("http://ndrtest.nsdl.org/api/get/2200/test.20090410170951993T/content");

		} catch (Throwable t) {
			errors.add("error", new ActionError("generic.error", t.getMessage()));
			saveErrors(request, errors);
			return mapping.findForward("ndr.upload.form");
		}

		String forwardPath = request.getParameter("forwardPath");
		if (forwardPath != null && forwardPath.trim().length() > 0)
			return new ActionForward(forwardPath);
		else
			return mapping.findForward("ndr.upload.confirm");
	}


	/**
	 *  Debugging/understanding upload form
	 *
	 * @param  request        NOT YET DOCUMENTED
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	void showRequestInfo(HttpServletRequest request) throws Exception {
		prtln("\nREQUEST INFO");
		prtln("attributeNames:");
		for (Enumeration e = request.getAttributeNames(); e.hasMoreElements(); ) {
			prtln("\t" + (String) e.nextElement());
		}

		prtln("parameterNames:");
		for (Enumeration e = request.getParameterNames(); e.hasMoreElements(); ) {
			prtln("\t" + (String) e.nextElement());
		}

		int contentLength = request.getContentLength();
		String contentType = request.getContentType();
		prtln("content: " + contentType + " (" + contentLength + ")");

		ServletInputStream stream = request.getInputStream();
	}


	/**
	 *  Gets the content attribute of the FileUploadAction object
	 *
	 * @param  request        NOT YET DOCUMENTED
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	private void getContent(HttpServletRequest request) throws Exception {
		int contentLength = request.getContentLength();
		String contentType = request.getContentType();
		prtln("content: " + contentType + " (" + contentLength + ")");

		BufferedReader reader = request.getReader();
		CharBuffer buffer = CharBuffer.allocate(contentLength);
		int charsRead = reader.read(buffer);
		prtln(charsRead + " characters read from buffer");
		prtln("\n--------------------------------");
		prtln(buffer.toString());
		prtln("\n--------------------------------");
	}


	/**
	 *  Sets the debug attribute of the NDRAction class
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
			SchemEditUtils.prtln(s, "");
		}
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  s  NOT YET DOCUMENTED
	 */
	private void prtlnErr(String s) {
		SchemEditUtils.prtln(s, "");
	}

}

