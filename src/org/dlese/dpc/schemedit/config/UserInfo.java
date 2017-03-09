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
 *  Bean to handle the "userInfo" element of the framework_config record, which
 *  is used to specify where information about the current user is to be placed
 *  in metadata records.<p>
 *
 *  The userInfo element contains a "autoPopulate" attribute that specifies WHEN
 *  the user info is to be inserted (e.g., at record creation time) and the
 *  repeating "property" elements specify a) the value (obtained from the User
 *  object) and b) the xpath of the metadataData record at which the value is to
 *  be inserted.
 *
 *@author    ostwald
 */
public class UserInfo implements Serializable {

	private static boolean debug = true;
	private Map infoMap = null;
	public String autoPopulate = null;


	/**
	 *  Constructor for the UserInfo object
	 *
	 *@param  e  a userInfo element from the framework-config record
	 */
	public UserInfo(Element e) {
		this.infoMap = new HashMap();
		this.autoPopulate = e.attributeValue("autoPopulate");

		for (Iterator i = e.elementIterator("property"); i.hasNext(); ) {
			Element prop = (Element) i.next();
			String propName = null;
			String xpath = null;
			try {
				propName = prop.element("property").getTextTrim();
				xpath = prop.element("path").getTextTrim();
			} catch (Throwable t) {
				prtln("couldn't get property ..." + t.getMessage());
			}
			if (propName != null && propName.length() > 0 &&
					xpath != null && xpath.length() > 0) {

				this.infoMap.put(propName, xpath);
			}
		}
	}


	/**
	 *  Gets the xpath for the specified property in the metadata record
	 *
	 *@param  propName  attribute of the User object
	 *@return           The path value
	 */
	public String getPath(String propName) {
		return (String) this.infoMap.get(propName);
	}


	/**
	 *  Returns an Iterator over the properties specified by this UserInfo object.
	 *
	 *@return    iterator over avail
	 */
	public Iterator propNameIterator() {
		return this.infoMap.keySet().iterator();
	}


	/**
	 *  Description of the Method
	 *
	 *@return    Description of the Return Value
	 */
	public String toString() {
		String ret = "\n";
		ret += "autoPopulate: " + autoPopulate;

		for (Iterator i = this.infoMap.keySet().iterator(); i.hasNext(); ) {
			String propName = (String) i.next();
			String xpath = (String) this.infoMap.get(propName);
			ret += "\n\t propName: " + propName;
			ret += "\n\t xpath: " + xpath;
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

