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
public class FrameworkAdminForm extends ActionForm {

	private boolean debug = true;

	private Map frameworks = null;
	private List unloadedFrameworks = null;
	private List uninitializedFrameworks = null;

	/**
	 *  Description of the Field
	 */
	// protected FormFile sampleFile;

	public Map getFrameworks () {
		return frameworks;
	}
	
	public void setFrameworks (Map map) {
		frameworks = map;
	}
	
	public void setUnloadedFrameworks (List formats) {
		this.unloadedFrameworks = formats;
	}
	
	public List getUnloadedFrameworks () {
		return this.unloadedFrameworks;
	}
	
	public void setUninitializedFrameworks (List formats) {
		this.uninitializedFrameworks = formats;
	}
	
	public List getUninitializedFrameworks () {
		return this.uninitializedFrameworks;
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

