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
package org.dlese.dpc.dds.action.form;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionError;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionServlet;
import org.apache.struts.util.MessageResources;
import java.util.*;
import java.io.*;
import org.dlese.dpc.index.*;
import org.dlese.dpc.index.reader.*;

/**
 *  A Struts Form bean for handling DDS view collection requests
 *
 * @author    Ryan Deardorff
 */
public final class DDSViewCollectionForm extends VocabCachingActionForm implements Serializable {
	private ResultDoc resultDoc = null;
	private String error = null;
	private boolean headless = false;

	/**
	 *  Constructor for the DDSViewResourceForm object
	 */
	public DDSViewCollectionForm() { }

	/**
	 *  Sets the resultDoc attribute of the DDSViewCollectionForm object
	 *
	 * @param  resultDoc  The new resultDoc value
	 */
	public void setResultDoc( ResultDoc resultDoc ) {
		this.resultDoc = resultDoc;
	}

	/**
	 *  Sets the headless attribute of the DDSViewCollectionForm object
	 *
	 * @param  headless  The new headless value
	 */
	public void setHeadless( boolean headless ) {
		this.headless = headless;
	}

	/**
	 *  Headless means display collection info without surrounding HTML (banners,
	 *  footers, etc).
	 *
	 * @return    The headless value
	 */
	public String getHeadless() {
		if ( headless ) {
			return "true";
		}
		else {
			return "false";
		}
	}

	/**
	 *  Gets the resultDoc attribute of the DDSViewCollectionForm object
	 *
	 * @return    The resultDoc value
	 */
	public ResultDoc getResultDoc() {
		return resultDoc;
	}

	/**
	 *  Gets the docReader attribute of the DDSViewCollectionForm object
	 *
	 * @return    The docReader value
	 */
	public DleseCollectionDocReader getDocReader() {
		return (DleseCollectionDocReader)resultDoc.getDocReader();
	}

	/**
	 *  Sets the error attribute of the DDSViewCollectionForm object
	 *
	 * @param  error  The new error value
	 */
	public void setError( String error ) {
		this.error = error;
	}

	/**
	 *  Gets the error attribute of the DDSViewCollectionForm object
	 *
	 * @return    The error value
	 */
	public String getError() {
		return error;
	}
}

