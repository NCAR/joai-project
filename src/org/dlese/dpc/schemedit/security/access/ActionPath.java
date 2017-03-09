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
package org.dlese.dpc.schemedit.security.access;

import java.io.File;
import java.util.*;

import org.dom4j.Element;
import org.dom4j.DocumentFactory;

import org.apache.struts.action.ActionMapping;
import org.dlese.dpc.schemedit.struts.HotActionMapping;

import org.dlese.dpc.schemedit.security.access.Roles;
import org.dlese.dpc.schemedit.security.access.Roles.Role;

/**
 *  Wrapper for HotActionMapping
 *
 *@author     ostwald
 *@created    August 27, 2006
 */
public class ActionPath {
	DocumentFactory df = DocumentFactory.getInstance();
	HotActionMapping mapping = null;
	String description = "";

	/**
	 *  Constructor for the ActionPath object
	 *
	 *@param  mapping      Description of the Parameter
	 *@param  description  Description of the Parameter
	 *@param  role        Description of the Parameter
	 */
	public ActionPath(HotActionMapping mapping) {
		super();
		this.mapping = mapping;
	}

	/**
	 *  Gets the path attribute of the ActionPath object
	 *
	 *@return    The path value
	 */
	public String getPath() {
		return mapping.getPath();
	}

	public String getName () {
		return mapping.getName();
	}
	
	public Role getRole() {
		return getRole (mapping);
	}
	
	/**
	* Helper method to extract a single role from a roleStr (like the one returned from
	* ActionMapping.getRoles()) that in principle could return mulitiple values. Since we
	* are using a single role string, we need to ensure that only a single value is extracted,
	* and we use the FIRST.
	*/
	public static Role getRole (ActionMapping actionMapping) {
		String mappingRoles = actionMapping.getRoles();
		// prtln ("getRole (" + actionMapping.getPath() + ") mappingRoles: " + mappingRoles);
		String roleStr = "";
		int comma = mappingRoles.indexOf(',');
		if (comma < 0)
			roleStr = mappingRoles.trim();
		else
			roleStr = mappingRoles.substring (0, comma).trim();
		// prtln ("getRole (" + actionMapping.getPath() + ") returning " + roleStr);
		// return roleStr;
		return Roles.toRole(roleStr);
	}
	
	public void setRole (Role role) {
		// prtln ("setRole(" + role.asString() + ") for path: " + this.getPath());
		mapping.setRoles(role.toString());		
	}
	
	/**
	 *  Gets the description attribute of the GuardedPath object
	 *
	 *@return    The description value
	 */
	 public String getDescription() {
		 return description;
	 }
	
	/**
	 *  Sets the description attribute of the GuardedPath object
	 *
	 *@param  d  The new description value
	 */
	 public void setDescription(String description) {
		 this.description = description;
	 }

	public Element asElement () {
		Element actionPath = df.createElement ("action-path");
		Element path = actionPath.addElement ("path");
		path.setText (getPath());
		Element description = actionPath.addElement ("description");
		description.setText (getDescription());
		return actionPath;
	}

	static void prtln (String s) {
		System.out.println ("Action Path: " + s);
	}
	
}

