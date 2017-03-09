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
package org.dlese.dpc.schemedit.config;

import java.util.*;
import java.io.Serializable;
import org.dom4j.Element;

/**
 *  Provides information about a schema element that is not expressed in the XML
 *  Schema, but that is helpful in displaying and editing documents within a
 *  framework. <p>
 *
 *  For example, a SchemaPath provides information when a new metadata record is
 *  created or copied, such as which elements should be blanked out, which are
 *  required by the Indexer, and which should be displayed as read-only fields
 *  in the metadata editor.<p>
 *
 *  SchemaPath instances are created by the {@link
 *  org.dlese.dpc.schemedit.config.FrameworkConfigReader} as it reads the
 *  configuration file for a particular framework. These SchemaPath instances
 *  are stored in a {@link org.dlese.dpc.schemedit.config.SchemaPathMap}
 *  structure, which is used mainly by the {@link
 *  org.dlese.dpc.schemedit.MetaDataFramework} class.
 *
 *@author    ostwald
 */
public class SchemaPath implements Serializable {

	private static boolean debug = true;
	/**
	 *  A short name (such as "url") that can be used to access the instance by name.
	 */
	public String pathName = null;
	/**
	 *  The xpath in the Schema to which this SchemaPath instance refers.
	 */
	public String xpath = null;
	/**
	 *  An optional default for this element
	 */
	public String defaultValue = null;
	/**
	 *  A String used to (somewhat informally) describe the type of the element
	 */
	public String valueType = null;
	
	public String inputHelper = null;
	
	public String initialFieldCollapse = null;
	
	public int maxLen = -1;
	/**
	 *  Description of the Field
	 */
	public boolean requiredByCopyRecord = false;
	/**
	 *  Is this element required by the copyRecord
	 */
	public boolean requiredByMinimalRecord = false;
	/**
	 *  Is this element required (by the indexer) to be present in all records?
	 */
	public boolean readOnly = false;


	/**
	 *  Constructor for the SchemaPath object
	 *
	 *@param  pathName                 Description of the Parameter
	 *@param  xpath                    Description of the Parameter
	 *@param  defaultValue             Description of the Parameter
	 *@param  valueType                Description of the Parameter
	 *@param  requiredByCopyRecord     Description of the Parameter
	 *@param  readOnly                 Description of the Parameter
	 *@param  requiredByMinimalRecord  Description of the Parameter
	 */
	public SchemaPath(String pathName,
			String xpath,
			String defaultValue,
			String valueType,
			String inputHelper,
			String initialFieldCollapse,
			boolean requiredByCopyRecord,
			boolean readOnly,
			boolean requiredByMinimalRecord) {
		this.pathName = pathName;
		this.xpath = xpath;
		this.defaultValue = defaultValue;
		this.valueType = valueType;
		this.initialFieldCollapse = initialFieldCollapse;
		this.inputHelper = inputHelper;
		this.requiredByCopyRecord = requiredByCopyRecord;
		this.requiredByMinimalRecord = requiredByMinimalRecord;
		this.readOnly = readOnly;
	}


	/**
	 *  Constructor for the SchemaPath object
	 *
	 *@param  e  Description of the Parameter
	 */
	public SchemaPath(Element e) {
		// prtln ("SchemaPath with \n" + e.asXML());
		xpath = e.getText();
		pathName = e.attributeValue("pathName");
		defaultValue = e.attributeValue("defaultValue");
		valueType = e.attributeValue("valueType");
		inputHelper = e.attributeValue("inputHelper");
		initialFieldCollapse = e.attributeValue("initialFieldCollapse");
		String copyRecord = e.attributeValue("requiredByCopyRecord");
		if (copyRecord != null && copyRecord.equals("true")) {
			requiredByCopyRecord = true;
		}

		String minimalRecord = e.attributeValue("requiredByMinimalRecord");
		if (minimalRecord != null && minimalRecord.equals("true")) {
			requiredByMinimalRecord = true;
		}

		String ro = e.attributeValue("readOnly");
		if (ro != null && ro.equals("true")) {
			readOnly = true;
		}
		
		String maxLenStr = e.attributeValue("maxLen");
		if (maxLenStr != null) {
			try {
				maxLen = Integer.parseInt(maxLenStr);
			} catch (NumberFormatException nfe) {
				prtln ("WARNING: could not parse \"" + maxLenStr + "\" as an integer");
			}
		}
	}


	/**
	 *  Description of the Method
	 *
	 *@return    Description of the Return Value
	 */
	public String toString() {
		String ret = "\n";
		ret += "pathName: " + pathName;
		ret += "\n\t xpath: " + xpath;
		ret += "\n\t pathName: " + pathName;
		ret += "\n\t defaultValue: " + defaultValue;
		ret += "\n\t valueType: " + valueType;
		ret += "\n\t inputHelper: " + inputHelper;
		ret += "\n\t initialFieldCollapse: " + initialFieldCollapse;
		if (requiredByCopyRecord) {
			ret += "\n\t requiredByCopyRecord: true";
		}
		if (requiredByMinimalRecord) {
			ret += "\n\t requiredByMinimalRecord: true";
		}
		if (readOnly) {
			ret += "\n\t readOnly: true";
		}
		return ret;
	}


	/**
	 *  Description of the Method
	 *
	 *@param  s  Description of the Parameter
	 */
	private void prtln(String s) {
		System.out.println(s);
	}

}

