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
package org.dlese.dpc.schemedit.security.login;

import java.util.Map;
import java.io.*;
import java.util.*;
import java.security.Principal;
import java.sql.*;
import java.math.*;

import javax.security.auth.*;
import javax.security.auth.callback.*;
import javax.security.auth.login.*;
import javax.security.auth.spi.*;

import org.dlese.dpc.schemedit.security.auth.TypedPrincipal;

/**
 * Simple database based authentication module.
 *
 * @author Andy Armstrong, <A HREF="mailto:andy@tagish.com">andy@tagish.com</A>
 * @version 1.0.3
 */
public class DBLogin extends SimpleLogin
{
	protected String                dbDriver;
	protected String                dbURL;
	protected String                dbUser;
	protected String                dbPassword;
	protected String                userTable;
	protected String                roleMapTable;
	protected String                roleTable;
	protected String                where;

	protected synchronized Vector validateUser(String username, char password[]) throws LoginException
	{
		ResultSet rsu = null, rsr = null;
		Connection con = null;
		PreparedStatement psu = null, psr = null;

		try
		{
			Class.forName(dbDriver);
			if (dbUser != null)
			   con = DriverManager.getConnection(dbURL, dbUser, dbPassword);
			else
			   con = DriverManager.getConnection(dbURL);

			psu = con.prepareStatement("SELECT UserID,Password FROM " + userTable +
									   " WHERE UserName=?" + where);
			psr = con.prepareStatement("SELECT " + roleTable + ".RoleName FROM " +
									   roleMapTable + "," + roleTable +
									   " WHERE " + roleMapTable + ".UserID=? AND " +
									   roleMapTable + ".RoleID=" + roleTable + ".RoleID");

			psu.setString(1, username);
			rsu = psu.executeQuery();
			if (!rsu.next()) throw new FailedLoginException("Unknown user");
			int uid = rsu.getInt(1);
			String upwd = rsu.getString(2);
			String tpwd = null;
			try {
				tpwd = new String(Utils.cryptPassword(password));
			} catch (Exception e) {
				throw new LoginException("Error encoding password (" + e.getMessage() + ")");
			}
			if (!upwd.equals(tpwd)) throw new FailedLoginException("Bad password");
			Vector p = new Vector();
			p.add(new TypedPrincipal(username, TypedPrincipal.USER));
			psr.setInt(1, uid);
			rsr = psr.executeQuery();
			while (rsr.next())
				p.add(new TypedPrincipal(rsr.getString(1), TypedPrincipal.GROUP));
			return p;
		}
		catch (ClassNotFoundException e)
		{
			throw new LoginException("Error reading user database (" + e.getMessage() + ")");
		}
		catch (SQLException e)
		{
			throw new LoginException("Error reading user database (" + e.getMessage() + ")");
		}
		finally
		{
			try {
				if (rsu != null) rsu.close();
				if (rsr != null) rsr.close();
				if (psu != null) psu.close();
				if (psr != null) psr.close();
				if (con != null) con.close();
			} catch (Exception e) { }
		}
	}

	public void initialize(Subject subject, CallbackHandler callbackHandler, Map sharedState, Map options)
	{
		super.initialize(subject, callbackHandler, sharedState, options);

		dbDriver = getOption("dbDriver", null);
		if (dbDriver == null) throw new Error("No database driver named (dbDriver=?)");
		dbURL = getOption("dbURL", null);
		if (dbURL == null) throw new Error("No database URL specified (dbURL=?)");
		dbUser = getOption("dbUser", null);
		dbPassword = getOption("dbPassword", null);
		if ((dbUser == null && dbPassword != null) || (dbUser != null && dbPassword == null))
		   throw new Error("Either provide dbUser and dbPassword or encode both in dbURL");

		userTable    = getOption("userTable",    "User");
		roleMapTable = getOption("roleMapTable", "RoleMap");
		roleTable    = getOption("roleTable",    "Role");
		where        = getOption("where",        "");
		if (null != where && where.length() > 0)
			where = " AND " + where;
		else
			where = "";
	}
}
