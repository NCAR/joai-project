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

import org.dlese.dpc.schemedit.SchemEditUtils;
import org.dlese.dpc.schemedit.url.UrlHelper;
import org.dlese.dpc.xml.XPathUtils;
import org.dlese.dpc.xml.Dom4jUtils;
import org.dlese.dpc.xml.schema.SchemaHelper;

import org.dlese.dpc.xml.schema.SchemaNode;
import java.util.regex.*;
import org.dom4j.*;
import java.util.*;

/**
 *  Helper for translating between http request parameters and metadata elements.<p>
 The metadata editor creates request parameters named for the accessor each field uses
 to obtain its value (e.g., valueOf(/itemRecord/lifecycle/contributors/contributor_2_/person/nameLast)). This class
 stores information derived from the parameterName, such as the {@link org.dlese.dpc.xml.schema.SchemaNode} for this field,
 and provides information about this node that aids in the processing of its value.
 *
 *@author    ostwald
 */
public class AnyTypeInputField extends InputField {

	private static boolean debug = true;
	private Element valueElement = null;
	private String parseError = null;
	boolean empty = false;
	
	/**
	 *  AnyTypeInputField constructor. Below are examples of typical parameters:
	 <dl>
	 	<dt>paramName<dd>valueOf(/itemRecord/lifecycle/contributors/contributor_2_/person/emailAlt)
        <dt>value<dd>anyone@foo.com 
        <dt>xpath<dd> /itemRecord/lifecycle/contributors/contributor[2]/person/emailAlt
        <dt>normalizedXPath<dd> /itemRecord/lifecycle/contributors/contributor/person/emailAlt
		</dl>
		
		paramName is used with the error-report mechanism to locate the field element in the UI
	 *
	 *@param  paramName        paramName as received from the request
	 *@param  value            Description of the Parameter
	 *@param  schemaNode       Description of the Parameter
	 *@param  xpath            Description of the Parameter
	 *@param  normalizedXPath  Description of the Parameter
	 */
	protected AnyTypeInputField(String paramName, 
						 String value, 
						 SchemaNode schemaNode, 
						 String xpath, 
						 InputManager inputManager) {
		super (paramName, value, schemaNode, xpath, inputManager);
		
		empty = getValue() == null || getValue().trim().length() == 0;
		
		if (!empty) {
			try {
				Document doc = DocumentHelper.parseText(getValue());
				valueElement = doc.getRootElement().createCopy();
			} catch (DocumentException de) {
				parseError = formatParseError(de.getMessage());
				// prtln ("\tAny element could not be parsed: " + parseError);
			}
		}		
	}
	
	static String formatParseError (String s) {
		String pat = "Nested exception";
		int x = s.toLowerCase().indexOf(pat.toLowerCase());
		if (x != -1) {
			return s.substring(0, x).trim();
		}
		return s;
	}
	
	public Element getValueElement () {
		return this.valueElement;
	}
	
	public boolean isEmpty () {
		return empty;
	}
	
	public boolean hasParseError () {
		return parseError != null;
	}
	
	public String getParseError () {
		return this.parseError;
	}
		
	/**
	* Debugging utility returns a string listing key fields and values.
	*/
	public String toString () {
		String ret = "---";
		ret += "\n\t" + "fieldName: " + getFieldName();
		ret += "\n\t" + "paramName: " + getParamName();
		ret += "\n\t" + "value: " + getValue();
		ret += "\n\t" + "xpath: " + getXPath();
		ret += "\n\t" + "normalizedXPath: " + getNormalizedXPath();
		return ret + "\n";
	}
	
	public void updateIndexing (int index) {
		String newParamName = null;
		try {
			newParamName = reindexParamName (this.getParamName(), index);
		} catch (Throwable t) {
			prtln ("Update Error: " + t.getMessage());
			return;
		}
		this.setParamName(newParamName);
		this.setXPath (InputManager.paramNameToXPath (newParamName));
	}
	
	static String reindexParamName (String oldParamName, int index) throws Exception {
		String encodedXPath = InputManager.stripFunctionCall(oldParamName);
		String siblingPath = XPathUtils.getSiblingXPath(encodedXPath);
		Pattern pat = Pattern.compile(".*?" + siblingPath + "_([0-9]*?)_\\)");
		Matcher m = pat.matcher(oldParamName);
		if (m.matches())
			return oldParamName.substring(0, m.start(1)) + index + "_)";
		else
			throw new Exception ("ERROR: AnyTypeInputField couldn't create new paramName");
	}
	
	private String pp (Node node) {
		return Dom4jUtils.prettyPrint (node);
	}
	
	/**
	 *  Description of the Method
	 *
	 *@param  s  Description of the Parameter
	 */
	private static void prtln(String s) {
		if (debug) {
			SchemEditUtils.prtln (s, "AnyTypeInputField");
		}
	}
}

