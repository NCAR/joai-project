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
package org.dlese.dpc.schemedit.input;

import org.dlese.dpc.xml.schema.*;
import org.dlese.dpc.xml.*;
import org.dlese.dpc.schemedit.display.CollapseUtils;

import java.util.*;
import java.io.*;

import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionErrors;

/**
 *  Methods to create ActionErrors for the Metadata Editor. Most create two
 *  error messages :
 *  <ol>
 *    <li> At the top of the page with a hyperlink to the field in which the
 *    error occurs
 *    <li> At the field itself
 *  </ol>
 *  MsgKey naming convention: The two-part errors require two keyed messages in
 *  the ApplicationsResource file. The key for the linked message is named as
 *  follows: msgKey+".link". For example, if the msgKey was "invalid.value",
 *  then the linked message key is named "invalid.value.link"
 *
 * @author    ostwald
 */
public class SchemEditErrors {

	private static boolean debug = false;


	/**
	 *  Creates two ActionErrors, one for the top of page message, and one for the
	 *  error message that is attached to a specific field. Used when no InputField
	 *  is available, so fieldProperty and elementName must be passed explicitly.
	 *  <p>
	 *
	 *  Example use: to notify of an element that could not be deleted. Here there
	 *  is not a specific InputField involved, but rather an branch of the XML
	 *  document (which is identified by xpath).<p>
	 *
	 *  Note: both the pathArg and the path component of the fieldProperty contain
	 *  xpaths that are encoded for jsp (e.g., the indexing notation is converted
	 *  from "[1]" to "_1_".
	 *
	 * @param  errors         ActionErrors object to which the new errors are added
	 * @param  fieldProperty  The property to which this error is attached (e.g.,
	 *      "valueOf(/itemRecord/general/description_1_)")
	 * @param  msgKey         Reference to a message in the "ApplicationResources"
	 * @param  elementName    The feature to be added to the Error attribute
	 * @param  pathArg        xpath (encoded for jsp) to the field in which the
	 *      error occurs
	 */
	public static void addError(SchemEditActionErrors errors, String fieldProperty, String msgKey, String elementName, String pathArg) {
		prtln("addError() #1");

		//debegging messages
		prtln("\tfieldProperty: " + fieldProperty);
		prtln("\tmsgKey: " + msgKey);
		prtln("\telementName: " + elementName);
		prtln("\tpathArg: " + pathArg);

		errors.add(fieldProperty,
			new ActionMessage(msgKey, elementName));
		errors.add("pageErrors",
			new ActionMessage(msgKey + ".link", elementName, pathArg));
	}


	/**
	 *  Creates two ActionErrors signifying a Generic error, one for the top of
	 *  page message, and one for the error message that is attached to a specific
	 *  field.
	 *
	 * @param  errors         error list to which we add
	 * @param  msg            error msg content
	 * @param  xpath          The feature to be added to the GenericError attribute
	 */
	public static void addGenericError(SchemEditActionErrors errors,
	                                   String xpath,
	                                   String msg) {
		prtln("addGenericError()");

		//debegging messages
		prtln("\txpath: " + xpath);
		prtln("\tmsg: " + msg);

		String encodedPath = XPathUtils.encodeXPath(xpath);
		String id = xpath2Id(xpath);
		String fieldProperty = "valueOf(" + encodedPath + ")";

		String msgKey = "generic.field.error";

		errors.add(fieldProperty,
			new ActionMessage(msgKey, msg));
		errors.add("pageErrors",
			new ActionMessage(msgKey + ".link", msg, id));
	}


	/**
	 *  Creates two ActionErrors signifying an invalidUrlError error, one for the
	 *  top of page message, and one for the error message that is attached to a
	 *  specific field.
	 *
	 * @param  errors         error list to which we add
	 * @param  uniqueUrlPath  path of field that should contain uniqueUrl
	 * @param  msg            error msg content
	 */
	public static void addInvalidUrlError(SchemEditActionErrors errors, String uniqueUrlPath, String msg) {
		prtln("addInvalidUrlError");
		//debegging messages
		prtln("\tuniqueUrlPath: " + uniqueUrlPath);

		String msgKey = "invalid.url";
		String encodedPath = XPathUtils.encodeXPath(uniqueUrlPath);
		String id = xpath2Id(uniqueUrlPath);
		String fieldProperty = "valueOf(" + encodedPath + ")";

		errors.add(fieldProperty,
			new ActionMessage(msgKey, msg));
		errors.add("pageErrors",
			new ActionMessage(msgKey + ".link", msg, id));
	}


	/**
	 *  Creates two ActionErrors signifying a similarURL, one for the top of page
	 *  message, and one for the error message that is attached to a specific
	 *  field.
	 *
	 * @param  errors         error list to which we add
	 * @param  msgKey         msgKey to select message from applicationResources
	 * @param  uniqueUrlPath  path of field that should contain uniqueUrl
	 */
	public static void addSimilarUrlError(SchemEditActionErrors errors, String uniqueUrlPath, String msgKey) {
		prtln("addSimilarUrlError");
		addUniqueUrlError(errors, uniqueUrlPath, msgKey, "similarUrls");
	}


	/**
	 *  Create a duplicateURL error(s) and attach to provided errors lsit
	 *
	 * @param  errors         error list to which we add
	 * @param  msgKey         msgKey to select message from applicationResources
	 * @param  uniqueUrlPath  path of field that should contain uniqueUrl
	 */
	public static void addDuplicateUrlError(SchemEditActionErrors errors, String uniqueUrlPath, String msgKey) {
		prtln("addDuplicateUrlError");
		addUniqueUrlError(errors, uniqueUrlPath, msgKey, "duplicateUrls");
	}


	/**
	 *  Creates two ActionErrors signifying a duplicateValue error, one for the top
	 *  of page message, and one for the error message that is attached to a
	 *  specific field.
	 *
	 * @param  errors      the error list to which we add
	 * @param  inputField  the input field containing a dup value
	 * @param  dupRecId    The Id of a record containing a dup value
	 */
	public static void addDuplicateValueError(SchemEditActionErrors errors, InputField inputField, String dupRecId) {
		prtln("addDuplicateValueError");
		// addUniqueUrlError (errors, uniqueValuePath, msgKey, "duplicateValues");
		prtln("addDuplicateValueError()");

		String msgKey = "duplicate.value.error";
		String uniqueUrlPath = inputField.getXPath();
		//debegging messages
		prtln("\tuniqueUrlPath: " + uniqueUrlPath);
		prtln("\tmsgKey: " + msgKey);

		String encodedPath = XPathUtils.encodeXPath(uniqueUrlPath);
		String fieldId = xpath2Id(uniqueUrlPath);
		String fieldProperty = "valueOf(" + encodedPath + ")";
		String elementName = XPathUtils.getLeaf(uniqueUrlPath);

		errors.add(fieldProperty,
			new ActionMessage(msgKey, dupRecId));
		errors.add("duplicateValues",
			new ActionMessage(msgKey + ".link", inputField.getValue(), dupRecId, fieldId));
	}


	/**
	 *  Creates two ActionErrors for a "nonUniqueUrl", one for the top of page
	 *  message, and one for the error message that is attached to a specific
	 *  field.
	 *
	 * @param  errors         error list to which we add
	 * @param  msgKey         msgKey to select message from applicationResources
	 * @param  uniqueUrlPath  path of field that should contain uniqueUrl
	 * @param  msgProperty    identifies the type of error and how it is displayed
	 *      in UI
	 */
	public static void addUniqueUrlError(SchemEditActionErrors errors, String uniqueUrlPath, String msgKey, String msgProperty) {
		prtln("addUniqueUrlError() with msgProperty: " + msgProperty);

		//debegging messages
		prtln("\tuniqueUrlPath: " + uniqueUrlPath);
		prtln("\tmsgKey: " + msgKey);

		String encodedPath = XPathUtils.encodeXPath(uniqueUrlPath);
		String id = xpath2Id(uniqueUrlPath);
		String fieldProperty = "valueOf(" + encodedPath + ")";
		String elementName = XPathUtils.getLeaf(uniqueUrlPath);

		errors.add(fieldProperty,
			new ActionMessage(msgKey, elementName));
		errors.add(msgProperty,
			new ActionMessage(msgKey + ".link", elementName, id));
		/* SchemEditErrors.addError(errors, fieldProperty, "unique.url.required", elementName, encodedPath, "similarUrls"); */
	}


	private static String getFieldId(InputField field) {
		return xpath2Id(field.getXPath());
	}


	private static String xpath2Id(String xpath) {
		return CollapseUtils.pathToId(XPathUtils.encodeXPath(xpath));
	}


	/**
	 *  Creates two ActionErrors, one for the top of page message, and one for the
	 *  error message that is attached to a specific field.
	 *
	 * @param  errors  ActionErrors object to which the new errors are added
	 * @param  field   The InputField containing an error
	 * @param  msgKey  Reference to a message in the "ApplicationResources"
	 */
	public static void addError(SchemEditActionErrors errors, InputField field, String msgKey) {
		prtln("addError() #2");
		String elementName = field.getFieldName();
		String id = xpath2Id(field.getXPath());
		String fieldProperty = field.getParamName();

		//debegging messages
		prtln("\txpath (field.xpath): " + field.getXPath());
		prtln("\tid : " + id);
		prtln("\tfieldProperty (field.paramName): " + fieldProperty);

		errors.add(fieldProperty,
			new ActionMessage(msgKey, elementName));
		errors.add("pageErrors",
			new ActionMessage(msgKey + ".link", elementName, id));
	}


	/**
	 *  Creates two ActionErrors, one for the top of page message, and one for the
	 *  error message that is attached to a specific field.
	 *
	 * @param  errors    ActionErrors object to which the new errors are added
	 * @param  field     The InputField containing an error
	 * @param  errorMsg  The feature to be added to the EntityError attribute
	 */
	public static void addEntityError(SchemEditActionErrors errors, InputField field, String errorMsg) {
		prtln("addEntityError()");
		String elementName = field.getFieldName();
		// String pathArg = XPathUtils.encodeXPath(field.getXPath());

		String fieldProperty = field.getParamName();
		String msgKey = "entity.error";
		String id = xpath2Id(field.getXPath());

		//debegging messages
		prtln("\txpath (field.xpath): " + field.getXPath());
		// prtln ("\tpathArg (encoded xpath): " + pathArg);
		prtln("\tid: " + id);
		prtln("\tfieldProperty (field.paramName): " + fieldProperty);

		prtln("\t errors class: " + errors.getClass().getName());
		errors.add(fieldProperty,
			new ActionMessage(msgKey, errorMsg));
		errors.add("entityErrors",
		// new ActionMessage(msgKey + ".link", elementName, errorMsg, pathArg));
			new ActionMessage(msgKey + ".link", elementName, id));
	}


	/**
	 *  Adds a feature to the AnyTypeError attribute of the SchemEditErrors class
	 *
	 * @param  errors    error list to which we add
	 * @param  field     inputField that error message is attached
	 * @param  errorMsg  The text of the error message
	 */
	public static void addAnyTypeError(SchemEditActionErrors errors, InputField field, String errorMsg) {
		prtln("addAnyTypeError()");
		String elementName = field.getFieldName();
		// String pathArg = XPathUtils.encodeXPath(field.getXPath());

		String fieldProperty = field.getParamName();
		String msgKey = "any.type.error";
		String id = xpath2Id(field.getXPath());

		//debegging messages
		prtln("\txpath (field.xpath): " + field.getXPath());
		// prtln ("\tpathArg (encoded xpath): " + pathArg);
		prtln("\tid: " + id);
		prtln("\tfieldProperty (field.paramName): " + fieldProperty);

		errors.add(fieldProperty,
			new ActionMessage(msgKey, errorMsg));
		errors.add("entityErrors",
		// new ActionMessage(msgKey + ".link", elementName, errorMsg, pathArg));
			new ActionMessage(msgKey + ".link", elementName, id));
	}


	/**
	 *  Creates ActionErrors for the specific case in which an element that has
	 *  children is missing a value.<p>
	 *
	 *  NOT USED!?
	 *
	 * @param  errors    ActionErrors object to which the new errors are added
	 * @param  field     The InputField containing an error
	 * @param  msgKey    Reference to a message in the "ApplicationResources"
	 * @param  errorMsg  The feature to be added to the XSDdatatypeError attribute
	 */
	/* 	public static void addEmptyWithChildError(ActionErrors errors, InputField field, String msgKey) {
		// prtln ("addEmptyWithChildError() xpath: " + field.getXPath());
		String elementName = field.getFieldName();
		String pathArg = XPathUtils.encodeXPath(field.getXPath());
		String fieldProperty = field.getParamName();
		errors.add(fieldProperty,
				new ActionMessage(msgKey));
		errors.add("pageErrors",
				new ActionMessage(msgKey + ".link", pathArg));
	} */
	/**
	 *  Adds errors in the case where an invalid value message has been supplied by
	 *  the XSDDataType validator.
	 *
	 * @param  errors    error list to which we add
	 * @param  field     inputField that error message is attached
	 * @param  msgKey    msgKey to select message from applicationResources
	 * @param  errorMsg  The text of the error message
	 */
	public static void addXSDdatatypeError(SchemEditActionErrors errors, InputField field, String msgKey, String errorMsg) {
		String fieldProperty = field.getParamName();
		String id = getFieldId(field);
		prtln("addXSDdatatypeError()");

		prtln("\tmsgKey: " + msgKey);
		prtln("\terrorMsg: " + errorMsg);
		prtln("\tid : " + id);
		errors.add(fieldProperty, new ActionMessage(msgKey, errorMsg));
		errors.add("pageErrors", new ActionMessage(msgKey + ".link", errorMsg, id));
	}


	/**
	 *  Print a line to standard out.
	 *
	 * @param  s  The String to print.
	 */
	private static void prtln(String s) {
		if (debug) {
			System.out.println("SchemEditErrors: " + s);
		}
	}

}

