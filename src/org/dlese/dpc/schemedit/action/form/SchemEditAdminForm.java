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
package org.dlese.dpc.schemedit.action.form;

import org.dlese.dpc.schemedit.*;
import org.dlese.dpc.xml.schema.*;
import org.dlese.dpc.xml.*;
import org.dlese.dpc.serviceclients.remotesearch.RemoteResultDoc;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Node;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionError;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionServlet;
import org.apache.struts.util.MessageResources;
import org.apache.struts.util.LabelValueBean;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.io.*;
import java.text.*;
import java.net.*;
import java.util.regex.*;

import org.apache.struts.upload.FormFile;
import org.apache.struts.upload.MultipartRequestHandler;

/**
 *  ActionForm bean for handling requests to support Schemaedit. Most methods
 *  acesss the {@link DocMap} attribute, which wraps the XML Document that is
 *  being edited.
 *
 *@author    ostwald
 */
public class SchemEditAdminForm extends ActionForm {

	private boolean debug = true;

	private String frameworkName = null;
	private String xmlFormat = null;
	private String sampleRecordFile = null;
	private String workingSchemaURI = null;
	private String schemaURI = null;
	private String discussionURL = null;
	private String recordsDir = "";

	private PageList pageList = null;
	private String pageErrors = null;

	private String editorMode = null;
	private String exitPath = "";
	private String renderer = "";
	private String editorConfig = "";

	/**
	 *  Description of the Field
	 */
	// protected FormFile sampleFile;


	/**
	 *  Gets the AutoForm renderers to populate choices in select object.
	 *
	 *@return    The renderers value
	 */
	public List getRenderers() {
		String[] names = {"EditorRenderer",
				// "BasicJspRenderer",
				// "CollapsibleJspRenderer",
				"DleseEditorRenderer"
				};
		return Arrays.asList(names);
	}


	/**
	 *  Gets the current editor configuration (frames, noframes).
	 *
	 *@return    The editorConfig value
	 */
	public String getEditorConfig() {
		return editorConfig;
	}


	/**
	 *  Sets the editorConfig attribute of the SchemEditAdminForm object
	 *
	 *@param  config  The new editorConfig value
	 */
	public void setEditorConfig(String config) {
		editorConfig = config;
	}


	/**
	 *  Gets the renderer attribute of the SchemEditAdminForm object
	 *
	 *@return    The renderer value
	 */
	public String getRenderer() {
		return renderer;
	}


	/**
	 *  Sets the renderer attribute of the SchemEditAdminForm object
	 *
	 *@param  r  The new renderer value
	 */
	public void setRenderer(String r) {
		if (getRenderers().contains(r)) {
			renderer = r;
		}
		else {
			prtln("could not set renderer as " + r);
		}
	}


	/**
	 *  Retrieve a representation of the file the user has uploaded
	 *
	 *@return    The sampleFile value
	 */
/* 	public FormFile getSampleFile() {
		return sampleFile;
	} */


	/**
	 *  Set a representation of the file the user has uploaded
	 *
	 *@param  sampleFile  The new sampleFile value
	 */
/* 	public void setSampleFile(FormFile sampleFile) {
		this.sampleFile = sampleFile;
	} */


	/**
	 *  The exit path determines where control is returned to when the user is done
	 *  with the admin interface. The value for the exit path is determined by the
	 *  {@link org.dlese.dpc.schemedit.MetaDataFramework}
	 *
	 *@return    The exitPath value
	 */
	public String getExitPath() {
		return exitPath;
	}


	/**
	 *  Sets the exitPath attribute of the SchemEditAdminForm object
	 *
	 *@param  path  The new exitPath value
	 */
	public void setExitPath(String path) {
		exitPath = path;
	}


	/**
	 *  Keeps track of from where the schemedit admin interface was entered.
	 *  Necessary because we need to send user back to either the stand-alone
	 *  editor or the DCS
	 *
	 *@param  editorMode  The new editorMode value
	 */
	public void setEditorMode(String editorMode) {
		this.editorMode = editorMode;
	}


	/**
	 *  Gets the editorMode attribute of the SchemEditAdminForm object (Stand-alone or DCS).
	 *
	 *@return    The editorMode value
	 */
	public String getEditorMode() {
		return editorMode;
	}


	/**
	 *  Gets the xmlFormat attribute of the SchemEditAdminForm object
	 *
	 *@return    The xmlFormat value
	 */
	public String getXmlFormat() {
		return xmlFormat;
	}


	/**
	 *  Sets the xmlFormat attribute of the SchemEditAdminForm object
	 *
	 *@param  s  The new xmlFormat value
	 */
	public void setXmlFormat(String s) {
		xmlFormat = s;
	}


	/**
	 *  Gets the frameworkName attribute of the SchemEditAdminForm object
	 *
	 *@return    The frameworkName value
	 */
	public String getFrameworkName() {
		return frameworkName;
	}


	/**
	 *  Sets the frameworkName attribute of the SchemEditAdminForm object
	 *
	 *@param  s  The new frameworkName value
	 */
	public void setFrameworkName(String s) {
		frameworkName = s;
	}


	/**
	 *  Gets the sampleRecordFile attribute of the SchemEditAdminForm object
	 *
	 *@return    The sampleRecordFile value
	 */
	public String getSampleRecordFile() {
		return sampleRecordFile;
	}


	/**
	 *  Sets the sampleRecordFile attribute of the SchemEditAdminForm object
	 *
	 *@param  s  The new sampleRecordFile value
	 */
	public void setSampleRecordFile(String s) {
		sampleRecordFile = s;
	}


	/**
	 *  Gets the schemaURI attribute of the SchemEditAdminForm object
	 *
	 *@return    The schemaURI value
	 */
	public String getSchemaURI() {
		return schemaURI;
	}


	/**
	 *  Sets the schemaURI attribute of the SchemEditAdminForm object
	 *
	 *@param  s  The new schemaURI value
	 */
	public void setSchemaURI(String s) {
		schemaURI = s;
	}


	/**
	 *  Gets the workingSchemaURI attribute of the SchemEditAdminForm object
	 *
	 *@return    The workingSchemaURI value
	 */
	public String getWorkingSchemaURI() {
		return workingSchemaURI;
	}


	/**
	 *  Sets the workingSchemaURI attribute of the SchemEditAdminForm object
	 *
	 *@param  s  The new workingSchemaURI value
	 */
	public void setWorkingSchemaURI(String s) {
		workingSchemaURI = s;
	}


	/**
	 *  Gets the discussionURL attribute of the SchemEditAdminForm object
	 *
	 *@return    The discussionURL value
	 */
	public String getDiscussionURL() {
		return discussionURL;
	}


	/**
	 *  Sets the discussionURL attribute of the SchemEditAdminForm object
	 *
	 *@param  s  The new discussionURL value
	 */
	public void setDiscussionURL(String s) {
		discussionURL = s;
	}


	/**
	 *  Gets the recordsDir attribute of the SchemEditAdminForm object
	 *
	 *@return    The recordsDir value
	 */
	public String getRecordsDir() {
		return recordsDir;
	}


	/**
	 *  Sets the recordsDir attribute of the SchemEditAdminForm object
	 *
	 *@param  s  The new recordsDir value
	 */
	public void setRecordsDir(String s) {
		recordsDir = s;
	}


	/**
	 *  Gets the pageList attribute of the SchemEditAdminForm object
	 *
	 *@return    The pageList value
	 */
	public PageList getPageList() {
		return pageList;
	}


	/**
	 *  Sets the pageList attribute of the SchemEditAdminForm object
	 *
	 *@param  list  The new pageList value
	 */
	public void setPageList(PageList list) {
		pageList = list;
	}


	/**
	 *  Check to make sure the client hasn't exceeded the maximum allowed upload
	 *  size inside of this validate method.
	 *
	 *@param  mapping  Description of the Parameter
	 *@param  request  Description of the Parameter
	 *@return          Description of the Return Value
	 */
	public ActionErrors validate(ActionMapping mapping, HttpServletRequest request) {
		prtln("validating");
		// ActionErrors errors = null;
		ActionErrors errors = new ActionErrors();

		String command = request.getParameter("command");
		if ( command == null) {
			prtln("command is null ... bailing");
			return errors;
		}

		if (command.equals("loadFramework"))
			return errors;
		
		String errorMsg = "";
		//has the maximum length been exceeded?
		Boolean maxLengthExceeded = (Boolean)
				request.getAttribute(MultipartRequestHandler.ATTRIBUTE_MAX_LENGTH_EXCEEDED);

		if ((maxLengthExceeded != null) && (maxLengthExceeded.booleanValue())) {
			errors.add("pageErrors",
					new ActionError("admin.maxLengthExceeded"));
		}

		// validate URI --------
		// is there a value for schemaURI?
		prtln("validating schemaURL: " + getSchemaURI());
		if (getSchemaURI() == null || getSchemaURI().trim().length() == 0) {
			errorMsg = "An Schema URI is reqired!";
			prtln(errorMsg);
			errors.add("schemaURI",
					new ActionError("generic.error", errorMsg));
			return errors;
		}

		// create URI object and validate it
		URI uri = null;
		try {
			uri = new URI(getSchemaURI());
		} catch (URISyntaxException e) {
			errorMsg = "bad URI syntax: " + e.getMessage();
			prtln(errorMsg);
			errors.add("schemaURI", new ActionError("generic.error", errorMsg));
			return errors;
		}

		if (uri == null) {
			errorMsg = "invalid URI";
			errors.add("schemaURI", new ActionError("generic.error", errorMsg));
			return errors;
		}

		String scheme = uri.getScheme();
		prtln("scheme: " + scheme);
		String path = uri.getPath();
		prtln("path: " + path);

		// absolute uri required
		if (!uri.isAbsolute()) {
			prtln("no scheme specified for \"schemaURI\"");
			errors.add("schemaURI",
					new ActionError("generic.error", "a URI scheme of \"file:\" or \"http\" is required"));
			return errors;
		}

		if (path == null) {
			errorMsg = "invalid URI - an absolute path is required";
			errors.add("schemaURI",
					new ActionError("generic.error", errorMsg));
			return errors;
		}
		if (scheme.equals("file")) {
			File file = new File(uri.getPath());
			if (file == null || !file.exists()) {
				prtln("caught an error for \"schemaFile\"");
				errors.add("schemaURI",
						new ActionError("generic.error", "warning: file does not exist"));
				return errors;
			}
		}
		else if (!scheme.equals("http")) {
			errorMsg = "unrecognized scheme: " + scheme;
			prtln(errorMsg);
			errors.add("schemaURI",
					new ActionError("generic.error", errorMsg));
		}

		return errors;
	}


	/**
	 *  Description of the Method
	 *
	 *@param  s  Description of the Parameter
	 */
	private void prtln(String s) {
		if (debug) {
			System.out.println("SchemaEditAdminForm: " + s);
		}
	}
}

