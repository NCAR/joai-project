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
package org.dlese.dpc.schemedit.ndr.action.form;

import org.dlese.dpc.schemedit.SchemEditUtils;

import org.apache.struts.upload.FormFile;
import org.apache.struts.upload.CommonsMultipartRequestHandler;
import org.apache.struts.action.ActionForm;

public class FileUploadForm extends ActionForm {

	private static boolean debug = true;
	
	public FileUploadForm () {
		setMultipartRequestHandler(new CommonsMultipartRequestHandler());
	}
	
	private FormFile myFile;
	public void setMyFile(FormFile myFile) {
		  this.myFile = myFile;
	}

	public FormFile getMyFile() {
		  return myFile;
	}
	
	private String contentURL;
	
	public void setContentURL (String handle) {
		this.contentURL = handle;
	}
	
	public String getContentURL () {
		return this.contentURL;
	}
	
	private String forwardPath;
	
	public void setForwardPath (String handle) {
		this.forwardPath = handle;
	}
	
	public String getForwardPath () {
		return this.forwardPath;
	}
	
	private String error;
	
	public void setError (String error) {
		this.error = error;
	}
	
	public String getError () {
		return this.error;
	}
	
	public boolean getHasError () {
		return this.error == null || this.error.trim().length() == 0;
	}
	
	private void prtln(String s) {
		if (debug) {
			SchemEditUtils.prtln(s, "");
		}
	}

}
	  

