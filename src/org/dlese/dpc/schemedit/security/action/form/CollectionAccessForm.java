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
package org.dlese.dpc.schemedit.security.action.form;

import java.util.*;
import java.io.Serializable;
import javax.servlet.http.HttpServletRequest;
import org.apache.struts.action.ActionError;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.util.LabelValueBean;

import org.dlese.dpc.repository.RepositoryManager;
import org.dlese.dpc.repository.SetInfo;
import org.dlese.dpc.schemedit.dcs.DcsSetInfo;
import org.dlese.dpc.schemedit.config.CollectionRegistry;
import org.dlese.dpc.schemedit.config.CollectionConfig;
import org.dlese.dpc.schemedit.security.user.User;
import org.dlese.dpc.schemedit.security.user.UserManager;
import org.dlese.dpc.schemedit.security.access.Roles;
import org.dlese.dpc.schemedit.security.util.RoleCollectionComparator;
/**

 */

public final class CollectionAccessForm extends ActionForm {

	private static boolean debug = true;
	
	public void reset (ActionMapping mapping, HttpServletRequest request) {}
	
	private SetInfo set = null;
	
	public SetInfo getSet () {
		return this.set;
	}
	
	public void setSet (SetInfo set) {
		this.set = set;
	}
	
	private List sets = null;
	
	
	/**
	 *  Gets the sets configured in the RepositoryManager. Overloaded method from
	 *  RepositoryForm.
	 *
	 *@return    The sets value
	 */
	public List getSets() {
		if (sets == null) {
			return new ArrayList();
		}
		return sets;
	}
	
	/**
	 *  Sets the sets attribute of the RepositoryAdminForm object. The set items
	 are instances of {@link org.dlese.dpc.schemedit.dcs.DcsSetInfo}.
	 *
	 *@param  setInfoSets  The new sets value
	 */
	public void setSets(List setInfoSets) {
		if (setInfoSets == null) {
			sets = new ArrayList();
		}
		else {
			sets = setInfoSets;
			String sortBy = getSortSetsBy();
			if (sortBy.equals("collection")) {
				Collections.sort(sets);
			}
			else {
				Collections.sort(sets, DcsSetInfo.getComparator(sortBy));
			}
		}
	}
	
	public List managableUsers = null;
	
	public List getManagableUsers () {
		return managableUsers;
	}
	
	public void setManagableUsers (List userList) {
		managableUsers = userList;
	}
	
	private String sortSetsBy = "collection";
	
	/**
	 *  Gets the sortSetsBy attribute of the RepositoryAdminForm object
	 *
	 *@return    The sortSetsBy value
	 */
	public String getSortSetsBy() {
		return sortSetsBy;
	}


	/**
	 *  Sets the sortSetsBy attribute of the RepositoryAdminForm object
	 *
	 *@param  sortSetsBy  The new sortSetsBy value
	 */
	public void setSortSetsBy(String sortSetsBy) {
		this.sortSetsBy = sortSetsBy;
	}
	
	/**
	* Create a representation of the user's roleMap, only using collection NAMES for the 
	key, and a RoleBean (collection, role) for the map's values.
	*/
	public Map getUserRoles (String username) {
		Map roles = new TreeMap (new RoleCollectionComparator());
	
		UserManager userManager = 
			(UserManager) getServlet().getServletContext().getAttribute("userManager");

		User user = userManager.getUser(username);
		if (user == null)
			return null;
		try {
			for (Iterator i=collections.iterator();i.hasNext();) {
				LabelValueBean bean = (LabelValueBean)i.next();
				String colName = bean.getLabel();
				String colKey = bean.getValue();
				String role = Roles.toString (user.getRole(colKey));
				roles.put (colName, 
					new RoleBean (colKey, role));				
			}
		} catch (Throwable t) {
			prtln ("getUserRoles error: " + t.getMessage());
			t.printStackTrace();
		}

		return roles;
	}				

	/**
	* mapping from collection to role for the current user
	*/
	private List collectionRoles = null;
	
	public List getCollectionRoles () {
		return collectionRoles;
	}
	
	public void	setCollectionRoles (List list) {
		collectionRoles = list;
	}
	
	private Map collectionAccessMap = null;
	
	public Map getCollectionAccessMap () {
		return this.collectionAccessMap;
	}
	
	public void setCollectionAccessMap (Map map) {
		this.collectionAccessMap = map;
	}
	
	private List users;
	
	public List getUsers () {
		return users;
	}
	
	public void setUsers (List users) {
		this.users = users;
	}
		
	private String username;
	
	public String getUsername () {
		return username;
	}
	
	public void setUsername (String username) {
		this.username = username;
	}
		
	private boolean newRole = false;
	
	public boolean getNewRole () {
		return newRole;
	}
	
	public void setNewRole (boolean newRole) {
		this.newRole = newRole;
	}
	
	private String collection;
	
	public String getCollection () {
		return collection;
	}
	
	public void setCollection (String collection) {
		this.collection = collection;
	}
	
	private List collectionOptions;
	
	public List getCollectionOptions () {
		return collectionOptions;
	}
		
	public void setCollectionOptions (List options) {
		prtln ("setting " + options.size() + " options");
		collectionOptions = options;
	}
	
	private List collections;
	
	public List getCollections () {
		return collections;
	}
		
	public void setCollections (List options) {
		prtln ("setting " + options.size() + " collections");
		collections = options;
	}
	
	private String role;
	
	public String getRole () {
		return role;
	}
	
	public void setRole (String role) {
		this.role = role;
	}
	
	private Map roles;
	
	public Map getRoles () {
		if (roles == null)
			roles = new TreeMap();
		return roles;
	}
	
	public void setRoles (Map roles) {
		this.roles = roles;
	}
	
	private List roleOptions;
	
	public List getRoleOptions () {
		return roleOptions;
	}
	
	public void setRoleOptions (List options) {
		this.roleOptions = options;
	}
	
	private String [] crs = null;
	
	public String [] getCrs () {
		if (crs == null)
			crs = new String[]{};
		return crs;
	}
	
	public void setCrs (String[] crs) {
		this.crs = crs;
	}
	
	public ActionErrors validate(ActionMapping mapping,
                                 HttpServletRequest request) {
        ActionErrors errors = new ActionErrors();
		prtln ("validate()");
        return errors;
    }
	
	public class RoleBean {
		private String collection;
		private String role;
		
		public RoleBean (String collection, String role) {
			this.collection = collection;
			this.role = role;
		}
		
		public String getCollection () {
			return collection;
		}
		
		public String getRole () {
			return role;
		}
	}
	
	static void prtln(String s) {
		while (s.length() > 0 && s.charAt(0) == '\n') {
			System.out.println ("");
			s = s.substring(1);
		}
		System.out.println("CollectionAccessForm: " + s);
	}
	
}
