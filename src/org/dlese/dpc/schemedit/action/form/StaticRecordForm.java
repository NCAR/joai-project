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

import org.dlese.dpc.index.*;
import org.dlese.dpc.repository.*;
import org.dlese.dpc.schemedit.*;
import org.dlese.dpc.schemedit.dcs.*;
import org.dlese.dpc.schemedit.action.*;
import org.dlese.dpc.index.reader.*;
import org.dlese.dpc.util.*;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionError;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionServlet;
import org.apache.struts.util.MessageResources;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.io.*;
import java.text.*;
import java.net.URLEncoder;

/**

 *
 * @author    Jonathan Ostwald
 */
public final class StaticRecordForm extends ActionForm implements Serializable {
	private static boolean debug = false;
	
	private String id;
	private HttpServletRequest request;


	private XMLDocReader docReader = null;
	private ResultDoc resultDoc = null;
	MetaDataFramework framework = null;

	private XMLDocReader annotatedItem = null;
	
	/**  Constructor for the StaticRecordForm object */
	public StaticRecordForm() { }
	

	public String getId () {
		return id;
	}
	
	public void setId (String id) {
		this.id = id;
	}
	
	/**
	 *  Sets the result attribute of the StaticRecordForm object
	 *
	 * @param  resultDoc  The new result value
	 */
	public void setResult(ResultDoc resultDoc) {
		this.resultDoc = resultDoc;
	}


	/**
	 *  Gets the result attribute of the StaticRecordForm object
	 *
	 * @return    The result value
	 */
	public ResultDoc getResult() {
		return resultDoc;
	}

	/**
	 *  Gets the result attribute of the StaticRecordForm object
	 *
	 * @return    The result value
	 */
	public DocReader getDocReader() {
		if (resultDoc == null)
			return null;
		return resultDoc.getDocReader();
	}

	public DocReader getAnnotatedItem() {
		return annotatedItem;
	}
	
	public void setAnnotatedItem(XMLDocReader annotatedItem) {
		this.annotatedItem = annotatedItem;
	}
	
	/**
	 *  Gets the framework attribute of the SchemEditForm object
	 *
	 * @return    The framework value
	 */
	public MetaDataFramework getFramework() {
		return framework;
	}


	/**
	 *  Sets the framework attribute of the SchemEditForm object
	 *
	 * @param  framework  The new framework value
	 */
	public void setFramework(MetaDataFramework framework) {
		this.framework = framework;
	}
	
	/**
	 *  Sets the request attribute of the StaticRecordForm object.
	 *
	 * @param  request  The new request value
	 */
	public void setRequest(HttpServletRequest request) {
		this.request = request;
	}


	//================================================================

	/**
	 *  Output a line of text to standard out, with datestamp, if debug is set to true.
	 *
	 * @param  s  The String that will be output.
	 */
	protected static void prtln(String s) {
		if (debug) {
			System.out.println("StaticRecordForm: " + s);
		}
	}

	protected static void prtlnError(String s) {
		System.out.println("StaticRecordForm: " + s);
	}

	/**
	 *  Return a string for the current time and date, sutiable for display in log files and
	 *  output to standout:
	 *
	 * @return    The dateStamp value
	 */
	private final static String getDateStamp() {
		return
			new SimpleDateFormat("MMM d, yyyy h:mm:ss a zzz").format(new Date());
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


