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
package org.dlese.dpc.services.mmd;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;

import java.util.LinkedList;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Provides a DB Connection and some low level access methods.
 */

public class DbConn {

private static boolean logging = false;
	
private int bugs;
private String printableUrl;				// saved for error msgs
private Connection conn = null;
private Statement stmt = null;


//### not thread safe.
//### check threadId on each call?

public DbConn( int bugs, String dbUrl)
throws MmdException
{
	this.bugs = bugs;
	this.printableUrl = dbUrl;
	String pswdtag = "password=";
	int ix = printableUrl.indexOf( pswdtag);
	if (ix >= 0) printableUrl = printableUrl.substring(
		0, ix + pswdtag.length()) + "(omitted)";

	String dbClass = "com.mysql.jdbc.Driver";

	// Get a DB connection
	try { Class.forName( dbClass).newInstance(); }
	catch( ClassNotFoundException cnf) {
		mkerror("db driver not found.  Insure \""
			+ dbClass + "\" is in the CLASSPATH.  exc: " + cnf);
	}
	catch( InstantiationException iex) {
		mkerror("db driver not found.  Insure \""
			+ dbClass + "\" is in the CLASSPATH.  exc: " + iex);
	}
	catch( IllegalAccessException iex) {
		mkerror("db driver not found.  Insure \""
			+ dbClass + "\" is in the CLASSPATH.  exc: " + iex);
	}
	conn = null;
	try { conn = DriverManager.getConnection( dbUrl); }
	catch( SQLException sqe) {
		mkerror("could not open db connection to URL \""
			+ printableUrl + "\"  exc: " + sqe);
	}
	if (bugs >= 1) prtln("openDB: opened: " + printableUrl);


	try { stmt = conn.createStatement(); }
	catch( SQLException sqe) {
		mkerror("DbConn.const: cannot create sql stmt: " + sqe);
	}
}

public void closeDb()
throws MmdException
{
	//prtln ("closeDB()");
	if (stmt != null) {
		try { stmt.close(); }
		catch( SQLException sqeb) {}
		stmt = null;
	}
	if (conn != null) {
		if (bugs >= 1) prtln("closeDb: closing: " + printableUrl);
		try { conn.close(); }
		catch( SQLException sqe) {
			mkerror( "could not close db connection to URL \""
				+ printableUrl + "\"");
		}
		conn = null;
	}
}

/**
 * Retrieves a single String from the DB.
 *
 * @param sqlstg The SQL string used to query the DB.
 */

public String getDbString( String sqlstg)
throws MmdException
{
	// logit ("getDbString: sqlstg: " + sqlstg);
	Object[][] resmat = getDbTable(
		sqlstg,
		new String[] { "string"},		// types
		false);					// allow nulls in fields
	if (resmat.length < 1 || resmat[0].length < 1)
		mkerror("getDbString: query not satisfied in db: \"" + sqlstg + "\"");
	String resval = (String) resmat[0][0];
	if (bugs >= 10) prtln("getDbString: sqlstg: \"" + sqlstg
		+ "\"  result: \"" + resval + "\"");
	return resval;
}

/**
 * Updates the DB.
 *
 * @param sqlstg The SQL string to be used.
 */

public void updateDb( String sqlstg)
throws MmdException
{
	if (bugs >= 10) prtln("updateDb: sqlstg: \"" + sqlstg + "\"");

	logit("updateDb: sqlstg: \"" + sqlstg + "\"");

	int upnum = 0;
	try { upnum = stmt.executeUpdate( sqlstg); }
	catch ( SQLException sqe) {
		mkerror("updateDb: db update failed. exc: \"" + sqe 
			+ "\" sqlstg: " + sqlstg);
	}
	///if (upnum != 1)
	///	mkerror("updateDb: db update failed.  sqlstg: \"" + sqlstg + "\"");
}

/**
 * Retrieves a table from the DB:
 * each array row represents one DB row,
 * and each array column represents a DB column.
 *
 * @param sqlstg The SQL string used to query the DB.
 * @param types An array of the types of the returned columns.
 *		Valid values are:
 * <table border=1>
 * <tr><th>Specified type</th>		<th>Returned java type</th></tr>
 * <tr><td>"boolean"</td>			<td>Boolean</td></tr>
 * <tr><td>"date"</td>				<td>Long (milliseconds since 1970)</td></tr>
 * <tr><td>"double"</td>			<td>Double</td></tr>
 * <tr><td>"int"</td>				<td>Integer</td></tr>
 * <tr><td>"long"</td>				<td>Long</td></tr>
 * <tr><td>"string"</td>			<td>String</td></tr>
 * </table>
 */

 
public Object[][] getDbTable(
	String sqlstg,
	String[] types,
	boolean allownull)			// allow nulls in any field
throws MmdException
{
	int ii, jj;

	if (types == null || types.length == 0)
		mkerror("getDbTable: invalid types");
	LinkedList reslist = new LinkedList();
	ResultSet rs = null;
	logit ("\nin getDbTable: sqlstg: \"" + sqlstg + "\"");
	try {
		stmt.execute( sqlstg);
		rs = stmt.getResultSet();
		if (rs == null) 
			mkerror("getDbTable: empty db result from sqlstg: \""
				+ sqlstg + "\"");

		while (rs.next()) {
			Object[] rowvals = new Object[ types.length];
			for (jj = 0; jj < types.length; jj++) {
				int ix = jj + 1;
				try {
					String typestg = types[jj].toLowerCase();
					if (typestg.equals("boolean"))
						rowvals[jj] = new Boolean( rs.getBoolean( ix));
					else if (typestg.equals("date")) {
						// return Date as a Long
						// Caution: rs.getDate truncates the time portion.
						Timestamp  tmpts = rs.getTimestamp( ix);
						if (tmpts == null) rowvals[jj] = null;
						else rowvals[jj] = new Long( tmpts.getTime());
					}
					else if (typestg.equals("double"))
						rowvals[jj] = new Double( rs.getDouble( ix));
					else if (typestg.equals("int"))
						rowvals[jj] = new Integer( rs.getInt( ix));
					else if (typestg.equals("long"))
						rowvals[jj] = new Long( rs.getLong( ix));
					else if (typestg.equals("string"))
						rowvals[jj] = rs.getString( ix);
					else mkerror("getDbTable: unknown type");

					if ((! allownull) && rowvals[jj] == null)
						mkerror("getDbTable: found a null value"
							+ "  in row " + (reslist.size() + 1) + "\n"
							+ "  in column " + ix + " from query:\n"
							+ "  " + sqlstg);
				}
				catch( SQLException sqe) {
					mkerror("getDbTable: could not get db info for"
						+ " (1-origin) field: " + ix
						+ " (" + rs.getObject(ix) + ")  from sqlstg: \""
						+ sqlstg + "\"  exc: " + sqe);
				}
			} // for jj
			reslist.add( rowvals);
		} // while rs.next
	} // try
	catch( SQLException sqe) {
		mkerror("getDbTable: could not get db info from sqlstg: \""
			+ sqlstg + "\"  exc: " + sqe);
	}
	finally {
		if (rs != null) {
			try { rs.close(); } catch( SQLException exc) {}
			rs = null;
		}
	}
	Object[][] resmat = (Object[][]) reslist.toArray( new Object[0][0]);
	if (bugs >= 10) {
		prtln("getDbTable: sqlstg: \"" + sqlstg + "\"");
		prtln("    result: ( " + resmat.length + " rows)");
		for (ii = 0; ii < resmat.length; ii++) {
			prtstg("row " + ii + ":");
			for (jj = 0; jj < resmat[ii].length; jj++) {
				if (resmat[ii][jj] == null) prtstg("  (null)");
				else prtstg("  \"" + resmat[ii][jj] + "\"");
			}
			prtln("");
		}
	}
	return resmat;
}

public static String dbstringcom( Timestamp ts)
throws MmdException
{
	if (ts == null) return dbstringcom( (String) null);
	else return dbstringcom( ts.toString());
}

public static String dbstring( Timestamp ts)
throws MmdException
{
	if (ts == null) return dbstring( (String) null);
	else return dbstring( ts.toString());
}

public static String dbstringcom( double vv)
throws MmdException
{
	return dbstringcom( Double.toString( vv));
}

public static String dbstring( double vv)
throws MmdException
{
	return dbstring( Double.toString( vv));
}

public static String dbstringcom( int vv)
throws MmdException
{
	return dbstringcom( Integer.toString( vv));
}

public static String dbstring( int vv)
throws MmdException
{
	return dbstring( Integer.toString( vv));
}

public static String dbstringcom( long vv)
throws MmdException
{
	return dbstringcom( Long.toString( vv));
}

public static String dbstring( long vv)
throws MmdException
{
	return dbstring( Long.toString( vv));
}


//### doc

public static String dbstringcom( String stg)
throws MmdException
{
	return dbstring( stg) + ",";
}


//### doc

public static String dbstring( String stg)
throws MmdException
{
	return dbstringsub( stg, false);
}


public static String dbstringforce( String stg)
throws MmdException
{
	return dbstringsub( stg, true);
}


public static String dbstringforcecom( String stg)
throws MmdException
{
	return dbstringsub( stg, true) + ",";
}

/** Creates a string suitable for MySQL commands,
 *  including escaped quotes and the surrounding single quotes.
 *
 *  Throws MmdException if the input string contains an invalid char,
 *  unless forceit == true in which case invalid chars are ouput
 *  as format "0xabcd".
 */

public static String dbstringsub( String stg, boolean forceit)
throws MmdException
{
	int ii;
	String res;
	String legalchars = "\n !\"#$%&\'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~";
	if (stg == null) res = "NULL";
	else {
		StringBuffer bufa = new StringBuffer();
		bufa.append("'");
		for (ii = 0; ii < stg.length(); ii++) {
			char cc = stg.charAt(ii);
			if (legalchars.indexOf( cc) < 0 && ! (cc >= 0xa0 && cc <= 0xff)) {
				if (forceit) {
				    
				    
					String hex = Integer.toHexString( cc);
					while (hex.length() < 4) {
						hex = "0" + hex;
					}
					// Cannot use "\\u0123" since mysql silently
					// deletes backslashes.
					bufa.append("0x" + hex);
				}
				else mkerror("invalid char (dec " + (int) cc
					+ ") in db update string: \"" + stg + "\"");
			}
			else if (cc == '\\') bufa.append("\\\\");
			else if (cc == '\'') bufa.append("\\\'");
			else if (cc == '\"') bufa.append("\\\"");
			else if (cc == '\n') bufa.append("\\n");	// allow newline
			else bufa.append( cc);
		}
		bufa.append("'");
		res = bufa.toString();
	}
	return res;
}

/**
 * Simply throws an MmdException.
 */

static void mkerror( String msg)
throws MmdException
{
	throw new MmdException( msg);
}

/**
 * Prints a String without a trailing newline.
 */

static void prtstg( String msg) {
	System.out.print( msg);
}

/**
 * Prints a String with a trailing newline.
 */

static void prtln( String msg) {
	System.out.println( msg);
}

static void logit(String msg) {
	if (logging) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
		String datestg = sdf.format(new Date());
		System.out.println("\n" + datestg + " - " + msg);
		System.out.flush();
	}
}



} // end class DbConn

