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
package org.dlese.dpc.services.dds.action.form;

import org.dlese.dpc.webapps.tools.*;
import org.dlese.dpc.index.*;
import org.dlese.dpc.index.reader.*;
import org.dlese.dpc.oai.*;
import org.dlese.dpc.repository.*;

import org.dlese.dpc.dds.action.form.VocabForm;
import org.apache.struts.action.Action;
import org.apache.struts.action.ActionError;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionServlet;
import org.apache.struts.util.MessageResources;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.ServletContext;
import java.util.*;
import java.io.*;
import java.text.*;
import java.net.URLEncoder;

/**
 *  A ActionForm bean that holds data for DDS repository update web service.
 *
 * @author    John Weatherley
 */
public class DDSRepositoryUpdateServiceForm extends ActionForm implements Serializable {

	public static final String RESULT_CODE_SUCCESS = "success";
	public static final String RESULT_CODE_NO_SUCH_RECORD = "recordDoesNotExist";
	public static final String RESULT_CODE_NO_SUCH_COLLECTION = "collectionDoesNotExist";
	
	public static final String ERROR_CODE_BADARGUMENT = "badArgument";
	public static final String ERROR_CODE_BADVERB = "badVerb";
	public static final String ERROR_CODE_NOTAUTHORIZED = "notAuthorized";
	public static final String ERROR_CODE_INTERNALSERVERERROR = "internalServerError";
	public static final String ERROR_CODE_SERVICE_DISABLED = "serviceDisabled";
	public static final String ERROR_CODE_ILLEGAL_OPERATION = "illegalOperation";

	private static boolean debug = true;

	private String errorMsg = null;
	private String errorCode = null;
	private String authorizedFor = null;
	private String recordXml = null;
	private String id = null;
	private String collectionKey = null;
	private String xmlFormat = null;
	private String responseDate = null;
	private String resultCode = null;
		
	// Bean properties:

	/**  Constructor for the RepositoryForm object */
	public DDSRepositoryUpdateServiceForm() { }


	/**
	 *  Gets the id attribute of the DDSRepositoryUpdateServiceForm object
	 *
	 * @return    The id value
	 */
	public String getId() {
		return id;
	}


	/**
	 *  Sets the id attribute of the DDSRepositoryUpdateServiceForm object
	 *
	 * @param  id  The new id value
	 */
	public void setId(String id) {
		this.id = id;
	}


	/**
	 *  Returns the value of collection.
	 *
	 * @return    The collection value
	 */
	public String getCollectionKey() {
		return collectionKey;
	}


	/**
	 *  Sets the value of collection.
	 *
	 * @param  collection  The value to assign collection.
	 */
	public void setCollectionKey(String collectionKey) {
		this.collectionKey = collectionKey;
	}


	/**
	 *  Gets the xmlFormat attribute of the DDSRepositoryUpdateServiceForm object
	 *
	 * @return    The xmlFormat value
	 */
	public String getXmlFormat() {
		return xmlFormat;
	}


	/**
	 *  Sets the xmlFormat attribute of the DDSRepositoryUpdateServiceForm object
	 *
	 * @param  xmlFormat  The new xmlFormat value
	 */
	public void setXmlFormat(String xmlFormat) {
		this.xmlFormat = xmlFormat;
	}

	
	/**
	 *  Gets the response date String.
	 *
	 * @return    The response date String
	 */
	public String getResponseDate() {
		if(responseDate == null)
			responseDate = OAIUtils.getDatestampFromDate(new Date());
		return responseDate;
	}


	/**
	 *  Returns the value of errorMsg.
	 *
	 * @return    The errorMsg value
	 */
	public String getErrorMsg() {
		return errorMsg;
	}


	/**
	 *  Sets the value of errorMsg.
	 *
	 * @param  errorMsg  The value to assign errorMsg.
	 */
	public void setErrorMsg(String errorMsg) {
		this.errorMsg = errorMsg;
	}

	/**
	 * Returns the value of resultCode.
	 */
	public String getResultCode()
	{
		return resultCode;
	}

	/**
	 * Sets the value of resultCode.
	 * @param resultCode The value to assign resultCode.
	 */
	public void setResultCode(String resultCode)
	{
		this.resultCode = resultCode;
	}	
	
	/**
	 *  Gets the errorCode attribute of the object
	 *
	 * @return    The errorCode value
	 */
	public String getErrorCode() {
		return errorCode;
	}


	/**
	 *  Sets the errorCode attribute of the object
	 *
	 * @param  errorMsg  The new errorMsg value
	 */
	public void setErrorCode(String errorCode) {
		this.errorCode = errorCode;
	}


	/**
	 *  Gets the role name for which this user is authorized
	 *
	 * @return    The authorizedFor value
	 */
	public String getAuthorizedFor() {
		return authorizedFor;
	}


	/**
	 *  Sets the role name for which this user is authorized
	 *
	 * @param  val  The new authorizedFor value
	 */
	public void setAuthorizedFor(String val) {
		authorizedFor = val;
	}



	//================================================================

	/**
	 *  Return a string for the current time and date, sutiable for display in log files and output to standout:
	 *
	 * @return    The dateStamp value
	 */
	protected final static String getDs() {
		return
			new SimpleDateFormat("MMM d, yyyy h:mm:ss a zzz").format(new Date());
	}


	/**
	 *  Output a line of text to error out, with datestamp.
	 *
	 * @param  s  The text that will be output to error out.
	 */
	protected final void prtlnErr(String s) {
		System.err.println(getDs() + " " + s);
	}


	/**
	 *  Output a line of text to standard out, with datestamp, if debug is set to true.
	 *
	 * @param  s  The String that will be output.
	 */
	protected final void prtln(String s) {
		if (debug)
			System.out.println(getDs() + " " + s);
	}


	/**
	 *  Sets the debug attribute
	 *
	 * @param  isDebugOuput  The new debug value
	 */
	public static void setDebug(boolean isDebugOuput) {
		debug = isDebugOuput;
	}
}


