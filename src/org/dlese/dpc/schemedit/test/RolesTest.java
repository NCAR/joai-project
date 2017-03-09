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
package org.dlese.dpc.schemedit.test;

import java.util.*;

/* import org.dlese.dpc.schemedit.security.access.Roles;
import org.dlese.dpc.schemedit.security.user.UserManager;
import org.dlese.dpc.schemedit.SchemEditUtils; */

public class RolesTest {
	private static boolean debug = true;

	public final static Role NO_ROLE = Role.NONE;
	public final static Role GUEST_ROLE = Role.GUEST;
	public final static Role CATALOGER_ROLE = Role.CATALOGER;
	public final static Role MANAGER_ROLE = Role.MANAGER;
	public final static Role ADMIN_ROLE = Role.ADMIN;
	
	static EnumSet<Role> roles = EnumSet.allOf(Role.class);
	
	public enum Role {
		NONE			( "" ),
		GUEST 		( "guest" ),
		CATALOGER 	( "cataloger" ),
		MANAGER 	( "manager" ),
		ADMIN 		( "admin" );
		
		String role;
		
		Role (String _role) {
			this.role = _role;
		}
		
		String getRole () { 
			return this.role; 
		}
		
		String asString () {
			return this.role;
		}
		
		public boolean satisfies (Role roleToCheck) {
			return (this.compareTo(roleToCheck) >= 0);
		}
		
		public boolean controls (Role roleToCheck) {
			return (this.compareTo(roleToCheck) > 0);
		}
	}
	
	public static void showRole (Role role) {
		prtln ("getRole(): " + role.getRole());
		prtln ("name: " + role.name());
		prtln ("ordinal: " + role.ordinal());
		prtln ("toString: " + role.asString());
	}
	
	public static EnumSet getSatisfyingRoles (Role maxRole) {
		return EnumSet.range(Role.NONE, maxRole);
	}
	
	public static Role getRole (String str) {
		for (Role r : Role.values())
			if (r.getRole().equals (str))
				return r;
		return null;
	}
	
	static void comparisons () {
		prtln ("\nsatisfies");
		for (Role r : roles) {
			/* prtln ("\n" + r.getRole());
			showRole (r); */
			prtln ("\t" + r + " satisfies CATALOGER: " + r.satisfies (Role.CATALOGER));
		}
		prtln ("\ncontrols");
		for (Role r : Role.values()) {
			/* prtln ("\n" + r.getRole());
			showRole (r); */
			prtln ("\t" + r + " controls CATALOGER: " + r.controls (Role.CATALOGER));
		}
		
		prtln ("\n roles statisfying MANAGER");
		for (Iterator i = getSatisfyingRoles (MANAGER_ROLE).iterator();i.hasNext();)
			prtln ("\t" + (Role)i.next());
	}
	
	public static void main (String [] args) {
		prtln ("RolesTest\n");
		Role myRole = CATALOGER_ROLE;
		showRole (myRole);
		
		String roleStr = "admin";
		Role fooRole = getRole (roleStr);
		if (fooRole == null)
			prtln ("role not found for \"" + roleStr + "\"");
		else
			showRole (fooRole);

		
	}
	
	private static void prtln(String s) {
		if (debug) {
			// System.out.println("JloTester: " + s);
			System.out.println(s);
		}
	}
}
