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
package org.dlese.dpc.schemedit;

import java.io.*;
import java.util.*;

import javax.servlet.ServletContext;

import org.dlese.dpc.xml.*;
import org.dlese.dpc.schemedit.security.user.User;
import org.dlese.dpc.schemedit.security.access.Roles;

/**

 *
 *@author    ostwald
 <p>$Id: RoleManager.java,v 1.9 2009/03/20 23:33:55 jweather Exp $
 */
public class RoleManager {
	private static boolean debug = false;
	private ServletContext context;
	private HashMap roleMap = null;
	private File configDir = null;
	private String instanceName;
	
	public static final String NO_OP = "no_operation";
	public static final String DELETE_RECORD = "deleteRecord";
	public static final String BATCH_DELETE = "batchDelete";
	public static final String EXPORT_COLLECTION = "exportCollection";
	public static final String SETTINGS = "settings";
	public static final String MANAGE_COLLECTIONS = "manageCollections";
	public static final String MANAGE = "manage";
	public static final String STATUS_HISTORY = "statusHistory";
	
	/**
	 *  Constructor for the RoleManager object
	 *
	 */
	public RoleManager(ServletContext context) {
		// prtln ("instantiating RoleManager");
		
		instanceName = (String)context.getAttribute("instanceName");
		this.context = context;
		init();

	}
	
	private boolean authIsEnabled () {
		try {
			return ((Boolean) context.getAttribute("authenticationEnabled"));
		} catch (Throwable t) {
			prtln ("error checking authEnabled: " + t.getMessage());
		}
		return false;
	}
	
	/* 
		quick and dirty isAuthorized methods to call from jsp 
	*/
	public boolean isAuthorized (String operation, SessionBean sessionBean) {
		return isAuthorized (operation, sessionBean, null);
	}
	/* 
		quick and dirty isAuthorized methods to call from jsp. this is only a bandaid implementation!!
	*/
	public boolean isAuthorized (String operation, SessionBean sessionBean, String collection) {
		if (!authIsEnabled()) return true;
		try {
			// String userRole = sessionBean.getUserRole();
			User user = sessionBean.getUser();
			if (user == null)
				throw new Exception ("User not obtained from sessionBean");
			
			if (true) {
				prtln ("isAuthorized()");
				prtln (" ... instanceName: " + instanceName);
				prtln (" ... operation: " + operation);
				prtln (" ... user: " + user.getUsername());
				prtln (" ... collection: " + collection);
				prtln (" ... userRole: " + user.getRole(collection));
		/* 
				prtln ("is instance name dcc? " + instanceName.equals("dcc"));
				prtln ("is operation a delete record? " + operation.equals(DELETE_RECORD) + " --- *" + operation + "*");
				prtln ("is the users role NOT admin? " + !userRole.equals(Roles.ADMIN_ROLE)); 
		*/
			}
			
			// describes the situation that preclude a role from being authorize
			boolean NOTauth = false;
			
			if (instanceName.equals("dcc")) {
				NOTauth =  ((operation.equals(DELETE_RECORD) ||
							operation.equals(BATCH_DELETE)) &&
							  user.hasRole(Roles.ADMIN_ROLE, collection));
			}
							   
			// decribes requirements for being authorized
			boolean ISauth = true;
			
			if (operation.equals(EXPORT_COLLECTION) || 
				operation.equals(SETTINGS) ||
				operation.equals(STATUS_HISTORY))
				ISauth = user.hasRole(Roles.ADMIN_ROLE, collection);
				
			if (operation.equals(MANAGE_COLLECTIONS) || 
				operation.equals(MANAGE))
				ISauth = user.hasRole(Roles.MANAGER_ROLE, collection);
			
			// prtln ("NOTauth: " + NOTauth + "  ISauth: " + ISauth);
			// prtln ("about to return: " + (ISauth && !NOTauth));
			return (ISauth && !NOTauth);
				
		} catch (Throwable t) {
/* 			String errorMsg = "WARNING: Authorization could not find user info - blindly allowing operation";
			prtln (errorMsg + " : " + t.getMessage());
			return true; */
			
			String errorMsg = "WARNING: Authorization could not find user info - refusing";
			prtln (errorMsg + " : " + t.getMessage());
			return false;
		}
	}
	

	
	
	private void init () {
		// intitialize hashmap so we have something to play with
		roleMap = new HashMap();
		ArrayList adminOps = new ArrayList();
		adminOps.add ("export");
		roleMap.put (Roles.ADMIN_ROLE, adminOps);
	}
	
	public boolean isAuthorized (String role, String operation) {
		if (!authIsEnabled()) return true;
		List operations = (List) roleMap.get(role);
		if (operations != null)
			return operations.contains(operation);
		else
			return false;
	}
	
	public void destroy () {

	}
	
	/**
	 *  Description of the Method
	 *
	 *@param  s  Description of the Parameter
	 */
	private static void prtln(String s) {
		if (debug) {
			System.out.println("RoleManager: " + s);
		}
	}

}

