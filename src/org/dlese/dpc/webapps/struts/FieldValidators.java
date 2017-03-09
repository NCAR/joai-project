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
package org.dlese.dpc.webapps.struts;

import javax.servlet.http.*;

import org.dlese.dpc.util.*;
import org.dlese.dpc.repository.RepositoryManager;
import org.dlese.dpc.repository.SetInfo;

// Struts/Commons imports
import org.apache.struts.action.ActionError;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;
import org.apache.struts.validator.Resources;
import org.apache.commons.validator.Arg;
import org.apache.commons.validator.ValidatorAction;
import org.apache.commons.validator.util.ValidatorUtils;
import org.apache.commons.validator.Validator;
import org.apache.commons.validator.Field;
import javax.servlet.ServletContext;

import java.io.*;
import java.text.*;
import java.util.*;

/**
 *  Static methods used in the Struts validation framework that implement custom validation actions. The
 *  static methods are configured in validator-rules.xml and applied to a specific form in your app using
 *  validation.xml, which are typically located in the WEB-INF directory of your web application.
 *
 * @author     John Weatherley
 * @version    $Id: FieldValidators.java,v 1.6 2009/03/20 23:34:01 jweather Exp $
 */
public class FieldValidators {
	private static boolean debug = true;


	/**
	 *  Validates that the field value is an existing directory on the server that the application is running on.
	 *
	 * @param  bean            The Struts bean
	 * @param  va              the ValidatorAction
	 * @param  field           The Field
	 * @param  messages        The ActionMessages
	 * @param  validator       The Validator
	 * @param  request         The HttpServletRequest
	 * @param  servletContext  The ServletContext
	 * @return                 True if the directory exists
	 */
	public static boolean validateIsDirectory(
	                                          Object bean,
	                                          ValidatorAction va,
	                                          Field field,
	                                          ActionMessages messages,
	                                          Validator validator,
	                                          HttpServletRequest request,
	                                          ServletContext servletContext) {		
		// Get the value the user entered:
		String value = ValidatorUtils.getValueAsString(bean, field.getProperty());

		File dir = new File(value.trim());
		// Validate that this is a directory on the server that already exists:
		if (!dir.isDirectory()) {
			ActionMessage message = Resources.getActionMessage(validator, request, va, field);
			messages.add(field.getKey(), message);
			return false;
		}
		else
			return true;
	}

	
	/**
	 *  Validates that the String is a valid namespace identifier for OAI.
	 *
	 * @param  bean            The Struts bean
	 * @param  va              the ValidatorAction
	 * @param  field           The Field
	 * @param  messages        The ActionMessages
	 * @param  validator       The Validator
	 * @param  request         The HttpServletRequest
	 * @param  servletContext  The ServletContext
	 * @return                 True if valid
	 */
	public static boolean validateNamespaceIdentifier(
	                                          Object bean,
	                                          ValidatorAction va,
	                                          Field field,
	                                          ActionMessages messages,
	                                          Validator validator,
	                                          HttpServletRequest request,
	                                          ServletContext servletContext) {		
		// Get the value the user entered:
		String repositoryIdentifier = ValidatorUtils.getValueAsString(bean, field.getProperty());
		boolean isValid = (
				repositoryIdentifier == null || 
				repositoryIdentifier.length() == 0 ||
				repositoryIdentifier.matches("[a-zA-Z][a-zA-Z0-9\\-]*(\\.[a-zA-Z][a-zA-Z0-9\\-]+)+"));
		if(!isValid) {
			ActionMessage message = Resources.getActionMessage(validator, request, va, field);
			messages.add(field.getKey(), message);			
		}
		return isValid;
	}	

	/**
	 *  Validates that the field value is a directory that is not already configured in the repository
	 *  (RepositoryManager). Checks the request parameter 'edit' for the previous directory setInfo, if editing.
	 *
	 * @param  bean            The Struts bean
	 * @param  va              the ValidatorAction
	 * @param  field           The Field
	 * @param  messages        The ActionMessages
	 * @param  validator       The Validator
	 * @param  request         The HttpServletRequest
	 * @param  servletContext  The ServletContext
	 * @return                 True if the directory exists
	 */
	public static boolean validateDirectoryNotInRepository(
	                                                       Object bean,
	                                                       ValidatorAction va,
	                                                       Field field,
	                                                       ActionMessages messages,
	                                                       Validator validator,
	                                                       HttpServletRequest request,
	                                                       ServletContext servletContext) {

		// Get the value the user entered:
		String value = ValidatorUtils.getValueAsString(bean, field.getProperty());

		File dir = new File(value.trim());
		
		RepositoryManager rm =
			(RepositoryManager) servletContext.getAttribute("repositoryManager");
		
		// If editing the same dir, allow it:
		String prevKey = request.getParameter("edit");
		SetInfo setInfo = rm.getSetInfo(prevKey);
		if(setInfo != null && setInfo.getDirectory().equals(dir.getAbsolutePath()))
			return true;
		
		if (rm != null && rm.isDirectoryConfigured(dir)) {
			ActionMessage message = Resources.getActionMessage(validator, request, va, field);
			messages.add(field.getKey(), message);
			return false;
		}
		else
			return true;
	}


	// -------------------- Utility methods -------------------

	/**
	 *  Return a string for the current time and date, sutiable for display in log files and output to standout:
	 *
	 * @return    The dateStamp value
	 */
	public static String getSimpleDateStamp() {
		try {
			return
				Utils.convertDateToString(new Date(), "EEE, MMM d h:mm:ss a");
		} catch (ParseException e) {
			return "";
		}
	}


	/**
	 *  Return a string for the current time and date, sutiable for display in log files and output to standout:
	 *
	 * @return    The dateStamp value
	 */
	public static String getDateStamp() {
		return
			new SimpleDateFormat("MMM d, yyyy h:mm:ss a zzz").format(new Date());
	}


	/**
	 *  Output a line of text to error out, with datestamp.
	 *
	 * @param  s  The text that will be output to error out.
	 */
	private final static void prtlnErr(String s) {
		System.err.println(getDateStamp() + " FieldValidators ERROR: " + s);
	}



	/**
	 *  Output a line of text to standard out, with datestamp, if debug is set to true.
	 *
	 * @param  s  The String that will be output.
	 */
	private final static void prtln(String s) {
		if (debug) {
			System.out.println(getDateStamp() + " FieldValidators: " + s);
		}
	}


	/**
	 *  Sets the debug attribute object
	 *
	 * @param  db  The new debug value
	 */
	public static void setDebug(boolean db) {
		debug = db;
	}
}

