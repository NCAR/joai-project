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

import java.security.*;
import javax.security.auth.*;
import java.util.*;

/** 
 * Assigns an integer "value" to a role to support "inheritance" of role-based permissions
*/

public class Roles {	
	

	public final static Role NO_ROLE = Role.NONE;
	// public final static Role GUEST_ROLE = Role.GUEST;
	public final static Role CATALOGER_ROLE = Role.CATALOGER;
	public final static Role MANAGER_ROLE = Role.MANAGER;
	public final static Role ADMIN_ROLE = Role.ADMIN;
	// public final static Role ROOT_ROLE = Role.ROOT;
	
	
	public enum Role {
		NONE		( "none" ),
		// GUEST 	( "guest" ), // guest is a User, NOT a role ...
		CATALOGER 	( "cataloger" ),
		MANAGER 	( "manager" ),
		ADMIN 		( "admin" );
		// ROOT     ( "root" );
		
		String role;
		
		Role (String _role) {
			this.role = _role;
		}
		
		String _getRole () { 
			return this.role; 
		}
		
		public String toString () {
			return this.role;
		}
		
		public boolean satisfies (Role roleToCheck) {
			return (this.compareTo(roleToCheck) >= 0);
		}
		
		public boolean controls (Role roleToCheck) {
			return (this.compareTo(roleToCheck) > 0);
		}
	}

	public static Role toRole (String str) {
		for (Role r : Role.values())
			if (r.toString().equals (str))
				return r;
		return NO_ROLE;
	}
	
	public static String toString (Role r) {
		return r.toString();
	}
	
	public static EnumSet roles = EnumSet.allOf (Role.class);
	private static Roles instance = null;
	
	public static Roles getInstance() {
		if (instance == null)
			instance = new Roles();
		return instance;
	}
		
	public static void main (String [] args) {
		Role maxRole = Roles.MANAGER_ROLE;
		
		EnumSet roles = getSatisfyingRoles (maxRole);
		for (Iterator i=roles.iterator();i.hasNext();)
			prtln ("\t\"" + (String)i.next() + "\"");
	}
	
	public static EnumSet getSatisfyingRoles(Role maxRole) {
		return EnumSet.range(Role.NONE, maxRole);
	}
	
	static void prtln (String s) {
		System.err.println("\n" + s);
	}
	

}

