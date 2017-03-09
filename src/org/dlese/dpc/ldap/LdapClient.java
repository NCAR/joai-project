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
package org.dlese.dpc.ldap;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

import java.security.MessageDigest;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.Properties;
import java.util.Random;

import javax.naming.Context;
import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;

import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.NoSuchAttributeException;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;



/**
 * Handles all LDAP communications for DLESE applications.
 * <p>
 * <h3>General</h3>
 * <p>
 * For info on the properties file, see the
 * {@link #LdapClient LdapClient constructor}.<br>
 *
 * For info on required and optional attributes, see
 * {@link #addUserEntry addUserEntry}.<br>
 *
 * For info on search filter syntax and semantics, see
 * {@link #searchDn searchDn}.<br>
 * <p>
 * <b>Caution</b>: The properties file contains an unencrypted
 * administrator password.  The file must <b>not</b> be readable
 * by outsiders!
 *
 * <h3>Lists</h3>
 * To make list names more practical and intuitive, there is a
 * hierarchical system of list names layered over LDAP's messy
 * syntax.  So, for example, the list name <br>
 * &nbsp;&nbsp;&nbsp;&nbsp; <code>lists/open/hydrology</code> <br>
 * is translated by this LDAP interface to the LDAP
 * dn (distinguished name): <br>
 * &nbsp;&nbsp;&nbsp;&nbsp; <code>DLESElistName=hydrology,DLESEsetName=open,DLESEou=lists,dc=dlese,dc=org</code> <br>
 * <p>
 * All list names must start with either "lists/" or "groups/".
 * They may have as many levels as desired.
 * Contrary to standard LDAP, this interface constructs
 * intermediate levels for list entrys as needed.
 * So if you create a list <br>
 * &nbsp;&nbsp;&nbsp;&nbsp; <code>/lists/committees/steering/voting</code> <br>
 * the intermediate LDAP entries are created if needed: <br>
 * &nbsp;&nbsp;&nbsp;&nbsp; <code>/lists</code> <br>
 * &nbsp;&nbsp;&nbsp;&nbsp; <code>/lists/committees</code> <br>
 * &nbsp;&nbsp;&nbsp;&nbsp; <code>/lists/committees/steering</code> <br>
 * <p>
 * Currently this interface does not remove unused intermediate
 * entries.  So, for example, if you remove the list <br>
 * &nbsp;&nbsp;&nbsp;&nbsp; <code>/lists/committees/steering/voting</code> <br>
 * the intermediate LDAP entries are not removed:<br>
 * &nbsp;&nbsp;&nbsp;&nbsp; <code>/lists</code> <br>
 * &nbsp;&nbsp;&nbsp;&nbsp; <code>/lists/committees</code> <br>
 * &nbsp;&nbsp;&nbsp;&nbsp; <code>/lists/committees/steering</code> <br>
 * If need be, they may be removed using the ldapmodify shell command,
 * which is part of standard OpenLDAP.
 *
 * <p>
 * All lists starting with "lists/open/"
 * have open enrollment, meaning users can self-register and self-remove.
 * All other lists can be changed only by the list owner
 * or an admin.
 *
 * <p>
 * <b>Example:</b> <code>lists/open/hydrology</code> <br>
 * <b>Corresponding LDAP dn:</b> <code>DLESElistName=hydrology,DLESEsetName=open,DLESEou=lists,dc=dlese,dc=org</code> <br>
 * <b>Meaning:</b> Hydrology interest group; open enrollment <br>
 *
 * <p>
 * <b>Example:</b> <code>lists/committees/steering/voting</code><br>
 * <b>Corresponding Ldap dn:</b> <code>DLESElistName=voting,DLESEsetName=steering,DLESEsetName=committees,DLESEou=lists,dc=dlese,dc=org</code> <br>
 * <b>Meaning:</b> Voting members of the steering committee;
 * controlled enrollment <br>
 *
 * <p>
 * <b>Example:</b> <code>groups/dpc/cataloguers/experienced</code><br>
 * <b>Corresponding Ldap dn:</b> <code>DLESElistName=experienced,DLESEsetName=cataloguers,DLESEsetName=dpc,DLESEou=groups,dc=dlese,dc=org</code> <br>
 * <b>Meaning:</b> Experienced DPC cataloguers; controlled enrollment <br>
 * <p>
 * <h3>Summary of Primary API Methods</h3>
 * Many methods come in two flavors.  One flavor is called with
 * a UID and password, which are used for authentication.
 * The other flavor is called without UID and password,
 * and uses the admin UID: it should always authenticate successfully.
 * <p>
 * <table align=center border=1 cellpadding=1 width=100%>
 * <tr> <th> Flavors </th> <th> Method </th><th>Desc </th> </tr> 
 *
 * <tr> <th colspan=3><font color=red>People entries:</font></th> </tr>
 *
 * <tr>
 *     <td> {@link #userAuthenticates(String,String) admin} </td>
 *     <td> <b>userAuthenticates:</b> Test: does a user authenticate </td>
 * </tr> 
 * <tr>
 *     <td> {@link #addUserEntry(String,String[][]) admin} </td>
 *     <td> <b>addUserEntry:</b> Creates a new user entry </td>
 * </tr> 
 * <tr>
 *     <td> {@link #removeUserEntry(String) admin} </td>
 *     <td> <b>removeUserEntry:</b> Removes a user entry </td>
 * </tr> 
 * <tr>
 *     <td> {@link #userExists(String) admin} </td>
 *     <td> <b>userExists:</b> Tests if a user entry exists </td>
 * </tr> 
 * <tr>
 *     <td> {@link #getUserAttributes(String,String,String,String[]) user},
 *          {@link #getUserAttributes(String,String[]) admin} </td>
 *     <td> <b>getUserAttributes:</b> Gets attributes for 1 user entry </td>
 * </tr> 
 * <tr>
 *     <td> {@link #getSingleAttribute(String,String,String,String) user},
 *          {@link #getSingleAttribute(String,String) admin} </td>
 *     <td> <b>getSingleAttribute:</b> Gets 1 attribute for 1 user entry </td>
 * </tr> 
 * <tr>
 *     <td> {@link #setUserAttribute(String,String,String,String,String[]) user},
 *          {@link #setUserAttribute(String,String,String[]) admin} </td>
 *     <td> <b>setUserAttribute:</b> Sets/replaces 1 attribute and all it's values </td>
 * </tr> 
 * <tr>
 *     <td> {@link #addUserAttributeValue(String,String,String,String,String) user},
 *          {@link #addUserAttributeValue(String,String,String) admin} </td>
 *     <td> <b>addUserAttributeValue:</b> Adds a new value for an attribute </td>
 * </tr> 
 * <tr>
 *     <td> {@link #removeUserAttributeValue(String,String,String,String,String) user},
 *          {@link #removeUserAttributeValue(String,String,String) admin} </td>
 *     <td> <b>removeUserAttributeValue:</b> Removes a value for an attribute </td>
 * </tr> 
 *
 * <tr> <th colspan=3><font color=red>Object entries:</font></th> </tr>
 *
 * <tr>
 *     <td> {@link #getUserObject(String,String,String,String) user} </td>
 *     <td> <b>getUserObject:</b> Retrieves a java object </td>
 * </tr> 
 * <tr>
 *     <td> {@link #storeUserObject(String,String,String,String,Object) user} </td>
 *     <td> <b>storeUserObject:</b> Stores a java object associated with a user</td>
 * </tr> 
 * <tr>
 *     <td> {@link #removeUserObject(String,String,String,String) user} </td>
 *     <td> <b>removeUserObject:</b> Removes a java object associated with a user</td>
 * </tr> 
 *
 * <tr> <th colspan=3><font color=red>Lists:</font></th> </tr>
 *
 * <tr>
 *     <td> {@link #createList(String,String) admin} </td>
 *     <td> <b>createList:</b> Creates a new list</td>
 * </tr> 
 * <tr>
 *     <td> {@link #removeEntireList(String) admin} </td>
 *     <td> <b>removeEntireList:</b> Removes an entire list</td>
 * </tr>
 * <tr>
 *     <td> {@link #addListName(String,String,String,String) user} </td>
 *     <td> <b>addListName:</b> Adds a name to a list </td>
 * </tr> 
 * <tr>
 *     <td> {@link #removeListName(String,String,String,String) user} </td>
 *     <td> <b>removeListName:</b> Removes a name from a list </td>
 * </tr> 
 * <tr>
 *     <td> {@link #getListAttributes(String,String,String,String[]) user},
 *          {@link #getListAttributes(String,String[]) admin} </td>
 *     <td> <b>getListAttributes:</b> Gets attributes of all members of a list </td>
 * </tr> 
 * <tr>
 *     <td> {@link #getListMembers(String,String,String,) user},
 *          {@link #getListMembers(String) admin} </td>
 *     <td> <b>getListMembers:</b> Gets dns of all members of a list </td>
 * </tr> 
 * <tr>
 *     <td> {@link #getListNames(String,String,String) user},
 *          {@link #getListNames(String) admin} </td>
 *     <td> <b>getListNames:</b> Gets UIDs of all members of a list </td>
 * </tr> 
 *
 * <tr> <th colspan=3><font color=red>All entries:</font></th> </tr>
 *
 * <tr>
 *     <td> {@link #search(String,String,String,String,String[],int) user},
 *          {@link #search(String,String,String[],int) admin} </td>
 *     <td> <b>search:</b> Searches all entries using the specified filter </td>
 * </tr>
 * </table>
 *
 * <h3>Groups of methods</h3>
 * The methods are divided into four groups:
 * <ul>
 *   <li> Public API methods that use a specified UID and password
 *        for authorization.  These methods, like
 *        <code> getUserAttributes( String authName, String password,
 *          String subjectName, String[] attrNames) </code>,
 *        take the uid and password as the first two parameters.
 *
 *   <li> Public API methods that use the internal administrator's
 *        dn (distinguished name) and password from the
 *        properties file for authorization.
 *        For example: <code> getUserAttributes(
 *          String subjectName, String[] attrNames) </code>.
 *   <li> Public API methods that take full dns (distinguished names)
 *        for parameters instead of UIDs.  These are noted as
 *        "Low level" in the doc, but can be used publicly.
 *   <li> Private internal methods.
 * </ul>
 *
 * <p>
 * <h3>Typical usage</h3>
 * Typical usage is shown below.  For a more complete example,
 * see {@link org.dlese.dpc.ldaptest.LdapDemo LdapDemo}.<br>
 * <pre>
 * void testDemoClient(
 *     String propsFile)       // Name of properties file
 * throws LdapException
 * {
 *     LdapClient democlient = new LdapClient( propsFile);
 *     try { democlient.isAlive(); }
 *     catch( LdapException lex) {
 *         System.out.println(
 *            "\nDemoClient: isAlive says there's trouble: " + lex);
 *         throw lex;
 *     }
 *     LdapEntry[] entries = democlient.search(
 *         null,               // base = null: start at top of DB tree
 *         "objectclass=*",    // filter: find all entries
 *         null,               // attrNames = null: return all attributes
 *         0);                 // maxres = 0 implies return all results
 *     if (entries == null) System.out.println("\nDemoClient: No entries found");
 *     else {
 *         System.out.println("\nDemoClient: num entries found: "
 *             + entries.length);
 * 
 *         // For each returned entry:
 *         for (int ii = 0; ii < entries.length; ii++) {
 *             System.out.println("\nEntry " + ii + " dn: "
 *                 + entries[ii].getDn());
 * 
 *             // For each attribute of the entry:
 *             for (int jj = 0; jj < entries[ii].getAttrsRows(); jj++) {
 *                 System.out.print("  Attribute: "
 *                     + entries[ii].getAttrName( jj) + "  Values:");
 *                 String[] valueStrings = entries[ii].getAttrStrings(jj);
 * 
 *                 // For each value of the attribute:
 *                 for (int kk = 0; kk < valueStrings.length; kk++) {
 *                     System.out.print("  \""
 *                         + valueStrings[kk] + "\"");
 *                 } // end for kk
 *                 System.out.println();
 *             } // end for jj
 *         } // end for ii
 *         System.out.println();
 *     } // if entries != null
 * } // end testDemoClient
 *
 * </pre>
 */

public class LdapClient {

private static String dleseUidTag = "DLESEloginName";

private int bugs = 0;

private String hostUrl = null;
private String dnbase = null;
private String adminDn = null;
private String adminPswd = null;
protected Properties props = null;

//================================================================
/**
 * Creates a client using the info in the specified properties file.
 * <p>
 * <b>Caution</b>: The properties file contains an unencrypted
 * administrator password.  The file must <b>not</b> be readable
 * by outsiders!
 * <p>
 * <b> Properties file fields:</b>
 * <table align=center border=1 cellpadding=1 width=100%>
 * <tr> <th> Field </th> <th> Meaning </th> </tr>
 * <tr> <td> hostUrl </td> <td> URL of the LDAP server </td> </tr>
 * <tr> <td> dnbase </td> <td> The suffix of the dn (distinguished
 *                      names) for all entries in the LDAP database </td> </tr>
 * <tr> <td> adminDn </td> <td> The full dn (distinguished name) of the administrator for this DLESE subgroup </td> </tr>
 * <tr> <td> adminPswd </td> <td> The unencrypted password for the adminDn </td> </tr>
 * </table>
 * <p>
 * Sample properties file:
 * <pre>
 *       hostUrl    ldap://localhost:3890
 *       dnbase     dc=dlese,dc=org
 *       adminDn    DLESEloginName=jsmith,DLESEou=people,dc=dlese,dc=org
 *       adminPswd  someSecret
 * </pre>
 */

public LdapClient( String pfile)
throws LdapException
{
	chkstg( "props file name", pfile);

	try {
		FileInputStream fis = new FileInputStream( pfile);	
		props = new Properties();
		props.load( fis);
		fis.close();
	}
	catch( Exception exc) {
		throw new LdapException(
			"LdapClient.const: could not load properties file \""
			+ pfile + "\"", exc);
	}

	hostUrl   = getProperty( "hostUrl", props, pfile);
	dnbase    = getProperty( "dnbase", props, pfile);
	adminDn   = getProperty( "adminDn", props, pfile);
	adminPswd = getProperty( "adminPswd", props, pfile);

} // end constructer










//================================================================

/**
 * Tests to see if the server, adminDn, and adminPswd specified
 * in the properties file are actually working.
 *
 * @return   void if OK; otherwise throws an LdapException.
 */

public void isAlive()
throws LdapException
{
	LdapEntry entry = null;
	DirContext dirctx = getDirContext( adminDn, adminPswd);
	Attributes attrraw = null;
	try { attrraw = dirctx.getAttributes( adminDn); }
	catch( NameNotFoundException nfexc) {
		// Should never happen:
		if (bugs >= 1) {
			prtln("isAlive: not found: nfexc: " + nfexc);
		}
		throw new LdapNotFoundException("isAlive: entry not found.  dn: \""
			+ adminDn + "\"", nfexc);
	}
	catch( NamingException nexc) {
		if (bugs >= 1) {
			prtln("isAlive: nexc: " + nexc);
			nexc.printStackTrace();
			prtln();
			prtln("adminDn: \"" + adminDn + "\"");
			prtln();
		}
		throw new LdapException("isAlive: exception", nexc);
	}
	return;
} // end isAlive




//================================================================



/**
 * Returns the UTC (GMT) time of creation for a given entry.
 *
 * @param subjectName the uid about which info is requested
 */

public Date getUtcCreateTimestamp(
	String subjectName)
throws LdapException
{
	chkstg( "subject name", subjectName);

	String attrName = "createTimestamp";
	LdapEntry entry = getAttributesDn(
		adminDn,
		adminPswd,
		mkUserDn( subjectName),
		new String[] { attrName} );	// attr names

	String[] datestgs = entry.getAttrStrings( attrName);
	if (datestgs == null || datestgs.length != 1)
		throw new LdapException("getCreateTimeStamp: attr not found: \""
			+ attrName + "\"  subjectName: \"" + subjectName + "\"", null);
	String dstg = datestgs[0];
	if (! dstg.endsWith("Z"))
		throw new LdapException("getCreateTimeStamp: bad time (no zone): \""
			+ dstg + "\"  subjectName: \"" + subjectName + "\"", null);
	String dstgtrim = dstg.substring( 0, dstg.length() - 1);
	SimpleDateFormat dform = new SimpleDateFormat("yyyyMMddHHmmss");
	Date dt = dform.parse( dstgtrim, new ParsePosition(0));
	return dt;
}






//================================================================



/**
 * Returns the UTC (GMT) time of last modification for a given entry.
 *
 * @param subjectName the uid about which info is requested
 */

public Date getUtcModifyTimestamp(
	String subjectName)
throws LdapException
{
	chkstg( "subject name", subjectName);

	String attrName = "modifyTimestamp";
	LdapEntry entry = getAttributesDn(
		adminDn,
		adminPswd,
		mkUserDn( subjectName),
		new String[] { attrName} );	// attr names

	String[] datestgs = entry.getAttrStrings( attrName);
	if (datestgs == null || datestgs.length != 1)
		throw new LdapException("getModifyTimeStamp: attr not found: \""
			+ attrName + "\"  subjectName: \"" + subjectName + "\"", null);
	String dstg = datestgs[0];
	if (! dstg.endsWith("Z"))
		throw new LdapException("getModifyTimeStamp: bad time (no zone): \""
			+ dstg + "\"  subjectName: \"" + subjectName + "\"", null);
	String dstgtrim = dstg.substring( 0, dstg.length() - 1);
	SimpleDateFormat dform = new SimpleDateFormat("yyyyMMddHHmmss");
	Date dt = dform.parse( dstgtrim, new ParsePosition(0));
	return dt;
}




//================================================================



/**
 * Returns true if the subjectName/password pair authenticates
 * successfully; false otherwise.
 *
 * @param subjectName the uid about which info is requested
 * @param password the password associated with subjectName
 */

public boolean userAuthenticates(
	String subjectName,
	String password)
throws LdapException
{
	chkstg( "subject name", subjectName);

	boolean bres = true;
	try {
		DirContext dirctx = getDirContext( mkUserDn( subjectName), password);
	}
	catch( LdapException lex) {
		Exception exc = lex.getException();
		if (exc != null
			&& (exc instanceof javax.naming.AuthenticationException))
			bres = false;
		else throw lex;
	}
	return bres;
}

//================================================================

/**
 * Retrieves values for a single attribute of a single entry,
 * using the specified uid/pswd for authorization.
 * See {@link #getSingleAttributeDn getSingleAttributeDn}.
 * See {@link #addUserEntry addUserEntry} for doc on the
 * attribute names.
 *
 * @param authName the uid of the caller
 * @param password the password associated with authName
 * @param subjectName the uid about which info is requested
 * @param attrName is the name of the desired attribute.
 *
 * @return   An array of the attribute values, or null if
 *    the attribute is not found.  The returned values
 *    may be in any order.
 */

public String[] getSingleAttribute(
	String authName,
	String password,
	String subjectName,
	String attrName)
throws LdapException
{
	chkstg( "subject name", subjectName);
	chkstg( "attr name", attrName);

	return getSingleAttributeDn(
		mkUserDn( authName),
		password,
		mkUserDn( subjectName),
		attrName);
}


//================================================================


/**
 * Retrieves values for a single attribute of a single entry,
 * using the admin dn/pswd for authorization.
 * See {@link #getSingleAttributeDn getSingleAttributeDn}.
 * See {@link #addUserEntry addUserEntry} for doc on the
 * attribute names.
 *
 * @param subjectName the uid about which info is requested
 * @param attrName is the name of the desired attribute.
 *
 * @return   An array of the attribute values, or null if
 *    the attribute is not found.  The returned values
 *    may be in any order.
 */

public String[] getSingleAttribute(
	String subjectName,
	String attrName)
throws LdapException
{
	chkstg( "subject name", subjectName);
	chkstg( "attr name", attrName);

	return getSingleAttributeDn(
		adminDn,
		adminPswd,
		mkUserDn( subjectName),
		attrName);
}


//================================================================

/**
 * Low level method: Returns the values associated with a single
 * attribute of a single entry, or null if no values exist.
 *
 * @param authDn   the authorized dn (distinguished name) of the caller
 * @param password the password associated with authDn
 * @param subjectDn the dn about which info is requested
 * @param attrName is the name of the desired attribute.
 *
 * @return   An array of the attribute values, or null if
 *    the attribute is not found.  The returned values
 *    may be in any order.
 */

public String[] getSingleAttributeDn(
	String authDn,
	String password,
	String subjectDn,
	String attrName)
throws LdapException
{
	chkstg( "attr name", attrName);

	LdapEntry entry = getAttributesDn( authDn, password, subjectDn,
		new String[] {attrName});
	String[] values = entry.getAttrStrings( 0);
	if (values.length == 0) return null;
	else return values;
}





//================================================================



/**
 * Retrieves attributes of a single entry,
 * using the specified uid/pswd for authorization.
 * See {@link #getAttributesDn getAttributesDn}.
 * See {@link #addUserEntry addUserEntry} for doc on the
 * attribute names.
 *
 * @param authName the uid of the caller
 * @param password the password associated with authName
 * @param subjectName the uid about which info is requested
 * @param attrNames an array of attribute names to be returned.
 *    If null, all available attributes are returned.
 *
 * @return   An LdapEntry representing one returned entry.
 *    Throws {@link LdapNotFoundException LdapNotFoundException}
 *    if entry not found.
 *    If attrNames was specified, the LdapEntry has the same
 *    attributes in the specified order.
 *    <p>
 *    If attrNames is null,
 *    the LdapEntry contains all available attributes for the entry,
 *    sorted by attribute name.
 */

public LdapEntry getUserAttributes(
	String authName,
	String password,
	String subjectName,
	String[] attrNames)
throws LdapException
{
	chkstg( "subject name", subjectName);
	chkstgs( "attr names", attrNames, true);	// may be null

	return getAttributesDn(
		mkUserDn( authName),
		password,
		mkUserDn( subjectName),
		attrNames);
}



//================================================================



/**
 * Retrieves attributes of a single entry,
 * using the admin dn/pswd for authorization.
 * See {@link #getAttributesDn getAttributesDn}.
 *
 * @param subjectName the uid about which info is requested
 * @param attrNames an array of attribute names to be returned.
 *    If null, all available attributes are returned.
 *
 * @return   An LdapEntry representing one returned entry.
 *    Throws {@link LdapNotFoundException LdapNotFoundException}
 *    if entry not found.
 *    If attrNames was specified, the LdapEntry has the same
 *    attributes in the specified order.
 *    <p>
 *    If attrNames is null,
 *    the LdapEntry contains all available attributes for the entry,
 *    sorted by attribute name.
 */

public LdapEntry getUserAttributes(
	String subjectName,
	String[] attrNames)
throws LdapException
{
	chkstg( "subject name", subjectName);
	chkstgs( "attr names", attrNames, true);	// may be null

	return getAttributesDn(
		adminDn,
		adminPswd,
		mkUserDn( subjectName),
		attrNames);
}



//================================================================



/**
 * Low level method: Retrieves attributes of a single entry,
 * using the specified dn/pswd for authorization.
 *
 * @param authDn   the authorized dn (distinguished name) of the caller
 * @param password the password associated with authDn
 * @param subjectDn the dn about which info is requested
 * @param attrNames an array of attribute names to be returned.
 *    If null, all available attributes are returned.
 *
 * @return   An LdapEntry representing one returned entry.
 *    Throws {@link LdapNotFoundException LdapNotFoundException}
 *    if entry not found.
 *    If attrNames was specified, the LdapEntry has the same
 *    attributes in the specified order.
 *    <p>
 *    If attrNames is null,
 *    the LdapEntry contains all available attributes for the entry,
 *    sorted by attribute name.
 */
public LdapEntry getAttributesDn(
	String authDn,
	String password,
	String subjectDn,
	String[] attrNames)
throws LdapException
{
	chkstg( "subject dn", subjectDn);
	chkstgs( "attr names", attrNames, true);	// may be null

	LdapEntry entry = null;
	DirContext dirctx = getDirContext( authDn, password);
	Attributes attrraw = null;

	// Whew, what LDAP trickery.  Cannot simply specify
	//        attrraw = dirctx.getAttributes( subjectDn);
	// since that won't get the operational attributes:
	//   createTimestamp, modifyTimestamp, etc.
	// The "*" gets all ordinary attributes;
	// the "+" gets the operational attributes.

	try {
		attrraw = dirctx.getAttributes( subjectDn, new String[] {"*", "+"} );
	}
	catch( NameNotFoundException nfexc) {
		if (bugs >= 1) {
			prtln("getAttributesDn: not found: nfexc: " + nfexc);
		}
		throw new LdapNotFoundException("entry not found.  dn: \""
			+ subjectDn + "\"", nfexc);
	}
	catch( NamingException nexc) {
		if (bugs >= 1) {
			prtln("getAttributesDn: nexc: " + nexc);
			nexc.printStackTrace();
			prtln();
			prtln("authDn: \"" + authDn + "\"");
			prtln("password: \"" + password + "\"");
			prtln("subjectDn: \"" + subjectDn + "\"");
			prtln();
		}
		throw new LdapException("getAttributesDn: exception", nexc);
	}
	if (attrraw != null)
		entry = decodeAttributes( subjectDn, attrNames, attrraw);
	return entry;
} // end getAttributesDn


//================================================================


/**
 * Searches and retrieves attributes for 0 or more entries,
 * using the specified uid/pswd for authorization.
 * See {@link #searchDn searchDn} for details.
 * See {@link #addUserEntry addUserEntry} for doc on the
 * attribute names.
 *
 * @param authName the uid of the caller
 * @param password the password associated with authName
 * @param base The starting dn of the search.  If null or "", start at top.
 * @param filter The search phrase.  See associated notes on filter syntax.
 * @param attrNames an array of attribute names to be returned.
 *    If attrNames is non-null, each LdapEntry has the same
 *    attributes in the specified order.
 *    If attrNames == null, all available attributes are returned
 *    for each entry, sorted by attribute name.
 * @param maxres  the maximum number of LDAP entries to return.
 *    If maxres &lt;= 0, all matching entries are returned.
 *
 */

public LdapEntry[] search(
	String authName,
	String password,
	String base,			// if null, start at top
	String filter,
	String[] attrNames,		// if null, return all attributes
	int maxres)				// if <= 0, return all results
throws LdapException
{
	chkstg( "filter", filter);
	chkstgs( "attr names", attrNames, true);	// may be null

	return searchDn(
		mkUserDn( authName),
		password,
		base,
		filter,
		attrNames,
		maxres);
}


//================================================================


/**
 * Searches and retrieves attributes for 0 or more entries,
 * using the admin dn/pswd for authorization.
 * See {@link #searchDn searchDn} for details.
 * See {@link #addUserEntry addUserEntry} for doc on the
 * attribute names.
 *
 * @param authName the uid of the caller
 * @param password the password associated with authName
 * @param base The starting dn of the search.  If null or "", start at top.
 * @param filter The search phrase.
 * @param attrNames an array of attribute names to be returned
 *    If null, all available attributes are returned.
 * @param maxres  the maximum number of LDAP entries to return.
 *    If maxres &lt;= 0, all matching entries are returned.
 */

public LdapEntry[] search(
	String base,			// if null, start at top
	String filter,
	String[] attrNames,		// if null, return all attributes
	int maxres)				// if <= 0, return all results
throws LdapException
{
	chkstg( "filter", filter);
	chkstgs( "attr names", attrNames, true);	// may be null

	return searchDn(
		adminDn,
		adminPswd,
		base,
		filter,
		attrNames,
		maxres);
}



//================================================================


/**
 * Low level method: searches and retrieves attributes for 0 or more entries,
 * using the specified dn/pswd for authorization.
 * See {@link #addUserEntry addUserEntry} for doc on the
 * attribute names.
 *
 * <p>
 * <b>Filter syntax</b>
 * <p>
 * Filter syntax is described in
 * <a href="http://www.ietf.org/rfc/rfc2254.txt">RFC 2254</a>
 * and associated RFCs.  The attributes contained in the various
 * DLESE entry types are specified in the OpenLDAP configuration
 * file, "slapd.conf", and in the OpenLDAP schema files.
 * Here is a summary of the syntax.
 * <p>
 * <b>Filter character set</b>
 * <p>
 * The character set used in filters is the UTF-8 encoding of
 * ISO 10646 (Unicode): see
 * <a href="http://www.unicode.org/">www.unicode.org/</a>.
 * Any character in the character set may be represented by a backslash with
 * two hex chars.  For example, the asterisk * could also
 * be written \2A.
 * <p>
 * <b>Filter items</b>
 * <p>
 * A filter expression is composed of items.  An item is
 * a single comparison, of the form:
 * &nbsp;&nbsp;&nbsp;&nbsp; <code> ( attributeName=value ) </code><br>
 * The simplest filter is a single item, like: <br>
 * &nbsp;&nbsp;&nbsp;&nbsp; <code> (sn=smith) </code><br>
 * This filter matches all entries having a surname (sn) of smith.
 * Most searches are case insensitive, so this would find
 * surnames like "Smith", "SmiTH", "smith", etc.
 * <p>
 * Some attributes are optional.  To find all entries
 * in which an attribute exists, even if it has a blank
 * value, use a filter item like:<br>
 * &nbsp;&nbsp;&nbsp;&nbsp; <code> (labeledURI=*) </code><br>
 * This finds all entries having a labeledURI (used to specify
 * a URL).
 * <p>
 * The "*" acts much like a shell wildcard, so to find
 * all entries having a labeledURI that uses http and
 * involves dlese, one could use:<br>
 * &nbsp;&nbsp;&nbsp;&nbsp; <code> (labeledURI=*http://*dlese*) </code><br>
 * <p>
 * <b>Filter expressions</b>
 * <p>
 * Boolean expressions may be build up from items using the
 * idiotic "prefix notation" specified by RFC 2254.
 * I guess they didn't feel like writing a real expression parser.
 * For example, to find all entries having
 * givenName = "sally" and surname = "smith" and phone with
 * a "303" area code, one could use:<br>
 * &nbsp;&nbsp;&nbsp;&nbsp; <code> (&amp; (givenName=sally) (sn=smith) (telephoneNumber=303*) ) </code><br>
 * That is, the "&amp;" operator precedes all its operands.
 * <p>
 * <b> Quotes in filters</b>
 * <p>
 * Quotes within filters are <b>not</b> used.
 * In some cases they happen to work, but in general they do not.
 * So the following two filters appear to work identically, although
 * only the first is correct. <br>
 * &nbsp;&nbsp;&nbsp;&nbsp; <code> (cn=sally smith) </code> <b>OK</b><br>
 * &nbsp;&nbsp;&nbsp;&nbsp; <code> (cn="sally smith") </code> <b>Incorrect</b><br>
 * <p>
 * <b> Filter operators and symbols </b>
 * <p>
 * The exact meaning of the comparison operators "=", "~=",
 * "&gt;=", "&lt;=" are defined in the attribute's definition.
 * For example, attributeType "sn" (a surname) is defined
 * in the OpenLDAP schema file "core.schema".
 * There "sn" inherits the syntax of attributeType "name".
 * The definition of "name" specifies "EQUALITY caseIgnoreMatch",
 * meaning that comparisons are case insensitive.
 * <p>
 * Incredibly, RFC 2254 defines no operators for "&lt;" and "&gt;".
 * <p>
 * <table align=center border=1 cellpadding=1 width=100%>
 * <tr> <th> Operator or symbol </th> <th> Meaning </th> </tr>

 * <tr> <th>=</th><td><b>Test equality,</b>
 *  according to the attribute definition<br>
 *  Example: &nbsp;&nbsp;&nbsp;<code>(sn=smith)</code><br>
 *  Notes: Matches all entries having an sn of "smith", or
 *  a case-changed variant thereof.  Usually case insensitive.
 * </td> </tr>

 * <tr> <th>=~</th><td><b>Test approximate equality,</b>
 *  according to the attribute definition<br>
 *  Example: &nbsp;&nbsp;&nbsp;<code>(sn=~smith)</code><br>
 *  Notes: seldom used
 * </td> </tr>

 * <tr> <th>&lt;=</th><td><b>Test less than or equal,</b>
 *  according to the attribute definition<br>
 *  Example: &nbsp;&nbsp;&nbsp;<code>(numWidgets&lt;=33)</code><br>
 *  Notes: seldom used
 * </td> </tr>

 * <tr> <th>&gt;=</th><td><b>Test greater than or equal,</b>
 *  according to the attribute definition<br>
 *  Example: &nbsp;&nbsp;&nbsp;<code>(numWidgets&gt;=33)</code><br>
 *  Notes: seldom used
 * </td> </tr>

 * <tr> <th>*</th><td><b>Wildcard: 0 or more chars</b><br>
 *  Example: &nbsp;&nbsp;&nbsp;<code>(sn=*smith*)</code><br>
 *  Notes: matches all entries having sn that contains "smith" in any case:
 *  "Smithsonian", "Arrowsmith", "blacksmIThing", etc.<br>
 *  Example: &nbsp;&nbsp;&nbsp;<code>(sn=*)</code><br>
 *  Notes: matches all entries having an "sn" attribute<br>
 *  Example: &nbsp;&nbsp;&nbsp;<code>(objectclass=*)</code><br>
 *  Notes: matches all entries in the LDAP database, since all entries
 *    must have an "objectclass" attribute<br>
 * </td> </tr>

 * <tr> <th>&amp;</th><td><b>Logical and</b><br>
 *  Example: &nbsp;&nbsp;&nbsp;<code>(&amp; (sn=smith) (givenName=Sally))</code><br>
 *  Notes: Matches all entries having both sn=smith and givenName=Sally<br>
 *  Caution: uses idiotic prefix notation: (&amp; item1 item2 item3 ... )
 * </td> </tr>

 * <tr> <th>|</th><td><b>Logical or</b><br>
 *  Example: &nbsp;&nbsp;&nbsp;<code>(| (sn=smith) (givenName=Sally))</code><br>
 *  Notes: Matches all entries having either sn=smith or givenName=Sally<br>
 *  Caution: uses idiotic prefix notation: (| item1 item2 item3 ... )
 * </td> </tr>

 * <tr> <th>!</th><td><b>Logical negation</b><br>
 *  Example: &nbsp;&nbsp;&nbsp;<code>(! (sn=*smith*))</code><br>
 *  Notes: Matches all entries that don't contain the string "smith"
 *  or a case-changed variant thereof.
 * </td> </tr>

 * <tr> <th>\</th><td><b>Escape character</b><br>
 *  Example: &nbsp;&nbsp;&nbsp;<code>(cn=stars \2A for sale)</code><br>
 *  Notes: Matches all entries having cn="stars * for sale".
 *  Example: &nbsp;&nbsp;&nbsp;<code>(cn=parens \28\29 for sale)</code><br>
 *  Notes: Matches all entries having cn="parens () for sale".
 *  Example: &nbsp;&nbsp;&nbsp;<code>(cn=*\A9*)</code><br>
 *  Notes: Matches all entries with cn containing a copyright symbol.
 * </td> </tr>

 * </table>
 *
 * @param authDn   the authorized dn (distinguished name) of the caller
 * @param password the password associated with authDn
 * @param specBase The starting dn of the search.
 *   If null or "", start at top.
 * @param filter The search phrase.
 * @param attrNames an array of attribute names to be returned
 *    If null, all available attributes are returned.
 * @param maxres  the maximum number of LDAP entries to return.
 *    If maxres &lt;= 0, all matching entries are returned.
 *
 * @return   An array of LdapEntry.
 *    Each LdapEntry represents one returned entry.
 *    If attrNames was specified, each LdapEntry has the same
 *    attributes in the specified order.
 *    <p>
 *    If attrNames is null, ALL available attributes are returned for
 *     each entry.  In this case:
 *     <ul>
 *       <li> <b>Caution:</b> The entries may have different numbers
 *            of attribute names.
 *       <li> <b>Caution:</b> Within an entry, the attributes may
 *            have different numbers of values.
 *       <li> The attribute names within each entry are sorted independently.
 *     </ul>
 */

public LdapEntry[] searchDn(
	String authDn,
	String password,
	String specBase,		// if null, start at top
	String filter,
	String[] attrNames,		// if null, return all attributes
	int maxres)				// if <= 0, return all results
throws LdapException
{
	int ient;
	LdapEntry[] resvec = null;
	String contextBase;

	chkstg( "filter", filter);
	chkstgs( "attr names", attrNames, true);	// may be null

	if (specBase == null || specBase.length() == 0) contextBase = dnbase;
	else contextBase = specBase;

	DirContext dirctx = getDirContext( authDn, password);
	SearchControls cntls = new SearchControls();
	cntls.setSearchScope( SearchControls.SUBTREE_SCOPE);
	if ( attrNames != null)
		cntls.setReturningAttributes( attrNames);
	if ( maxres > 0) cntls.setCountLimit( maxres);

	// Finally, do the search
	NamingEnumeration resenum = null;
	try { resenum = dirctx.search( contextBase, filter, cntls); }
	catch( NamingException nexc) {
		if (bugs >= 1) {
			prtln("searchDn: nexc: " + nexc);
			nexc.printStackTrace();
			prtln();
			prtln("authDn: \"" + authDn + "\"");
			prtln("password: \"" + password + "\"");
			prtln("contextBase: \"" + contextBase + "\"");
			prtln("filter: \"" + filter + "\"");
			if (attrNames == null)
				prtln("attrNames: (null)");
			else {
				prtln("attrNames.len: " + attrNames.length);
				for (int ii = 0; ii < attrNames.length; ii++) {
					prtln("    attrNames[" + ii + "]: \""
						+ attrNames[ii] + "\"");
				}
			}
			prtln("maxres: \"" + maxres + "\"");
			prtln();
		}
		throw new LdapException("searchDn: exception", nexc);
	}

	if (resenum == null)
		throw new LdapException("LDAP results == null", null);

	ArrayExc dnvec = fixEnum( resenum);
	if (bugs >= 1 && dnvec.exc != null)
		prtln( "dnvec hidden exception: " + dnvec.exc);

	resvec = new LdapEntry[ dnvec.vals.length];
	if (dnvec.vals.length > 0) {
		// Sort the returned entries by dn
		Arrays.sort( dnvec.vals, new Comparator() {
			public int compare( Object obja, Object objb) {
				SearchResult sra = (SearchResult) obja;
				SearchResult srb = (SearchResult) objb;
				return sra.getName().compareTo( srb.getName());
			}
			public boolean equals( Object obj) { return false; }
		});

		// Fill in the attributes for each entry
		for (ient = 0; ient < resvec.length; ient++) {
			SearchResult sr = (SearchResult) dnvec.vals[ ient];
			resvec[ient] = decodeSearchResult( sr, contextBase, attrNames);
			if (bugs >= 1) prtln("Result " + ient + ": " + resvec[ient]);
		}
	}
	return resvec;
} // end searchDn


//================================================================

/**
 * Sets the value of a single attribute,
 * using the specified uid/pswd for authorization.
 * Any previous values for the attribute are removed.
 * See {@link #setAttributeDn setAttributeDn}.
 *
 * @param authName  the uid of the caller
 * @param password the password associated with authName
 * @param subjectName the uid containing the attribute to be modified
 * @param attrName the attribute name
 * @param values  the new values for the attribute
 */
public void setUserAttribute(
	String authName,
	String password,
	String subjectName,
	String attrName,
	String[] values)
throws LdapException
{
	chkstg( "subjectName", subjectName);
	chkstg( "attr name", attrName);
	chkstgs( "values", values, false);		// may not be null

	setAttributeDn(
		mkUserDn( authName),
		password,
		mkUserDn( subjectName),
		attrName,
		values);
}




//================================================================

/**
 * Sets the value of a single attribute,
 * using the admin dn/pswd for authorization.
 * Any previous values for the attribute are removed.
 * See {@link #setAttributeDn setAttributeDn}.
 *
 * @param subjectName the uid containing the attribute to be modified
 * @param attrName the attribute name
 * @param values  the new values for the attribute
 */
public void setUserAttribute(
	String subjectName,
	String attrName,
	String[] values)
throws LdapException
{
	chkstg( "subjectName", subjectName);
	chkstg( "attr name", attrName);
	chkstgs( "values", values, false);		// may not be null

	setAttributeDn(
		adminDn,
		adminPswd,
		mkUserDn( subjectName),
		attrName,
		values);
}


//================================================================

/**
 * Low level method: sets the value of a single attribute,
 * using the specified dn/pswd for authorization.
 * Any previous values for the attribute are removed.
 *
 * @param authDn   the authorized dn (distinguished name) of the caller
 * @param password the password associated with authDn
 * @param subjectDn the dn containing the attribute to be modified
 * @param attrName the attribute name
 * @param values  the new values for the attribute
 */
public void setAttributeDn(
	String authDn,
	String password,
	String subjectDn,
	String attrName,
	String[] values)
throws LdapException
{
	chkstg( "subject dn", subjectDn);
	chkstg( "attr name", attrName);
	chkstgs( "values", values, false);		// may not be null

	modifyAttributeDn( authDn, password, subjectDn,
		attrName, values, DirContext.REPLACE_ATTRIBUTE);
}


//================================================================

/**
 * Adds a single value to a single attribute,
 * using the specified uid/pswd for authorization.
 * See {@link #addAttributeValueDn addAttributeValueDn}.
 *
 * @param authName  the uid of the caller
 * @param password the password associated with authName
 * @param subjectName the uid containing the attribute to be modified
 * @param attrName the attribute name
 * @param value  the new value
 */

public void addUserAttributeValue(
	String authName,
	String password,
	String subjectName,
	String attrName,
	String value)
throws LdapException
{
	chkstg( "subjectName", subjectName);
	chkstg( "attr name", attrName);
	chkstg( "value", value);

	addAttributeValueDn(
		mkUserDn( authName),
		password,
		mkUserDn( subjectName),
		attrName,
		value);
}



//================================================================

/**
 * Adds a single value to a single attribute,
 * using the admin dn/pswd for authorization.
 * See {@link #addAttributeValueDn addAttributeValueDn}.
 *
 * @param authName  the uid of the caller
 * @param password the password associated with authName
 * @param subjectName the uid containing the attribute to be modified
 * @param attrName the attribute name
 * @param value  the new value
 */

public void addUserAttributeValue(
	String subjectName,
	String attrName,
	String value)
throws LdapException
{
	chkstg( "subjectName", subjectName);
	chkstg( "attr name", attrName);
	chkstg( "value", value);

	addAttributeValueDn(
		adminDn,
		adminPswd,
		mkUserDn( subjectName),
		attrName,
		value);
}

//================================================================

/**
 * Low level method: adds a single value to a single attribute,
 * using the specified dn/pswd for authorization.
 *
 * @param authDn   the authorized dn (distinguished name) of the caller
 * @param password the password associated with authDn
 * @param subjectDn the dn containing the attribute to be modified
 * @param attrName the attribute name
 * @param value  the new value
 */

public void addAttributeValueDn(
	String authDn,
	String password,
	String subjectDn,
	String attrName,
	String value)
throws LdapException
{
	chkstg( "subject dn", subjectDn);
	chkstg( "attr name", attrName);
	chkstg( "value", value);

	if (bugs >= 1) {
		prtln("addAttributeValueDn: authDn: \"" + authDn + "\"");
		prtln("addAttributeValueDn: password: \"" + password + "\"");
		prtln("addAttributeValueDn: subjectDn: \"" + subjectDn + "\"");
		prtln("addAttributeValueDn: attrName: \"" + attrName + "\"");
		prtln("addAttributeValueDn: value: \"" + value + "\"");
	}
	String[] vals = new String[] { value };
	modifyAttributeDn( authDn, password, subjectDn,
		attrName, vals, DirContext.ADD_ATTRIBUTE);
}


//================================================================

/**
 * Removes a single value from a single attribute, or removes the
 * entire attribute and all values,
 * using the specified uid/pswd for authorization.
 *
 * @param host     the URI of the LDAP server.
 * @param authName  the uid of the caller
 * @param password the password associated with authName
 * @param subjectName the uid containing the attribute to be modified
 * @param attrName the attribute name
 * @param value  the value to be removed.
 *   If null, the entire attribute and all values are removed.
 */

public void removeUserAttributeValue(
	String authName,
	String password,
	String subjectName,
	String attrName,
	String value)			// may be null
throws LdapException
{
	chkstg( "subjectName", subjectName);
	chkstg( "attr name", attrName);

	removeAttributeValueDn(
		mkUserDn( authName),
		password,
		mkUserDn( subjectName),
		attrName,
		value);
}




//================================================================

/**
 * Removes a single value from a single attribute, or removes the
 * entire attribute and all values,
 * using a the admin dn/pswd for authorization.
 *
 * @param host     the URI of the LDAP server.
 * @param authName  the uid of the caller
 * @param password the password associated with authName
 * @param subjectName the uid containing the attribute to be modified
 * @param attrName the attribute name
 * @param value  the value to be removed.
 *   If null, the entire attribute and all values are removed.
 */

public void removeUserAttributeValue(
	String subjectName,
	String attrName,
	String value)
throws LdapException
{
	chkstg( "subjectName", subjectName);
	chkstg( "attr name", attrName);

	removeAttributeValueDn(
		adminDn,
		adminPswd,
		mkUserDn( subjectName),
		attrName,
		value);
}



//================================================================

/**
 * Low level method: removes a single value from a single attribute,
 * or removes the entire attribute and all values,
 * using the specified dn/pswd for authorization.
 *
 * @param host     the URI of the LDAP server.
 * @param authDn   the authorized dn (distinguished name) of the caller
 * @param password the password associated with authDn
 * @param subjectDn the dn containing the attribute to be modified
 * @param attrName the attribute name
 * @param value  the value to be removed.
 *   If null, the entire attribute and all values are removed.
 */

public void removeAttributeValueDn(
	String authDn,
	String password,
	String subjectDn,
	String attrName,
	String value)				// may be null
throws LdapException
{
	chkstg( "subject dn", subjectDn);
	chkstg( "attr name", attrName);

	String[] vals;
	if (value == null) vals = null;
	else vals = new String[] { value };
	modifyAttributeDn( authDn, password, subjectDn,
		attrName, vals, DirContext.REMOVE_ATTRIBUTE);
}




//================================================================

/**
 * Low level method: modifies a single value from a single attribute,
 * or the entire attribute,
 * using the specified dn/pswd for authorization.
 *
 * @param authDn   the authorized dn (distinguished name) of the caller
 * @param password the password associated with authDn
 * @param subjectDn the dn containing the attribute to be modified
 * @param attrName the attribute name
 * @param values  the values to be added or removed.
 *   If null, the entire attribute and all values are affected.
 * @param func one of:
 *   <ul>
 *	    <li> DirContext.REPLACE_ATTRIBUTE
 *      <li> DirContext.ADD_ATTRIBUTE
 *      <li> DirContext.REMOVE_ATTRIBUTE
 *   </ul>
 */

private void modifyAttributeDn(
	String authDn,
	String password,
	String subjectDn,
	String attrName,
	String[] values,		// may be null
	int func)
throws LdapException
{
	chkstg( "subject dn", subjectDn);
	chkstg( "attr name", attrName);
	chkstgs( "values", values, true);		// may be null

	DirContext dirctx = getDirContext( authDn, password);
	Attribute battr = new BasicAttribute( attrName, false);	// ordered = false
	if (values != null) {
		for (int ii = 0; ii < values.length; ii++) {
			chkstg( attrName, values[ii]);
			battr.add( values[ii]);
		}
	}
	Attributes attrs = new BasicAttributes( true);	// ignoreCase = true
	attrs.put( battr);

	try { dirctx.modifyAttributes( subjectDn, func, attrs); }
	catch( NameNotFoundException nfexc) {
		throw new LdapNotFoundException("dn not found.  dn: \"" + subjectDn
			+ "\"", nfexc);
	}
	catch( NoSuchAttributeException nfexc) {
		throw new LdapNotFoundException("no such attribute.  dn: \""
			+ subjectDn + "\"  attr: \"" + attrName
			+ "\"", nfexc);
	}
	catch( NamingException nexc) {
		if (bugs >= 1) {
			prtln("modifyAttributeDn: nexc: " + nexc);
			nexc.printStackTrace();
			prtln();
			prtln("authDn: \"" + authDn + "\"");
			prtln("password: \"" + password + "\"");
			prtln("subjectDn: \"" + subjectDn + "\"");
			prtln("attrName: \"" + attrName + "\"");
			if (values == null)
				prtln("values: (null)");
			else {
				prtln("values.len: " + values.length);
				for (int ii = 0; ii < values.length; ii++) {
					prtln("    values[" + ii + "]: \""
						+ values[ii] + "\"");
				}
			}
			prtln();
		}
		throw new LdapException("modifyAttributesDn: exception", nexc);
	}
} // end modifyAttributeDn




//================================================================

/**
 * Adds a new LDAP entry for a user,
 * using a the admin dn/pswd for authorization.
 * <p>
 * <b> Required attributes</b><br>
 * All values are unique except as noted.
 * See the OpenLDAP configuration file, slapd.conf, for the final say.
 * <table align=center border=1 cellpadding=1 width=100%>
 * <tr><th>Attribute</th>  <th>Meaning</th>  <th>Typical values</th></tr>


 * <tr><th>DLESEemailPrimary</th>  <td>primary email address</td>  <td>somebody@someplace.org</td></tr>
 * <tr><th>DLESEloginName</th>  <td>login name</td>  <td>jsmith</td></tr>
 * <tr><th>DLESEnameFirst</th>  <td>first name (given name)</td>  <td>James</td></tr>
 * <tr><th>DLESEnameLast</th>  <td>last name (surname or family name)</td>  <td>Smith</td></tr>
 * <tr><th>userPassword</th>  <td>login password</td>  <td>aBigSecret</td></tr>
 * </table>
 *
 *
 * <p>
 * <b> Optional attributes</b><br>
 * All values are unique except as noted.
 * See the OpenLDAP configuration file, slapd.conf, for the final say.
 * <table align=center border=1 cellpadding=1 width=100%>
 * <tr><th>Attribute</th>  <th>Meaning</th>  <th>Typical values</th></tr>
 * <tr><th>DLESEadr1</th>  <td>street address line 1</td>  <td>123 Some Street</td></tr>
 * <tr><th>DLESEadr2</th>  <td>street address line 2</td>  <td>Dept 456</td></tr>
 * <tr><th>DLESEcity</th>  <td>street address: city</td>  <td>Whoville</td></tr>
 * <tr><th>DLESEcounty</th>  <td>street address: county</td>  <td>Tack County</td></tr>
 * <tr><th>DLESEcountry</th>  <td>street address: country</td>  <td>USA</td></tr>
 * <tr><th>DLESEemailAlt</th>  <td>alternate email address</td>  <td>someOther@someU.edu</td></tr>
 * <tr><th>DLESEfax</th>  <td>fax phone number</td>  <td>303-555-1212</td></tr>
 * <tr><th>DLESEfocus</th>  <td>focus (may have multiple values)</td>  <td>elementary school, graduate students, ...</td></tr>
 * <tr><th>DLESEnameMiddle</th>  <td>middle name or initial</td>  <td>M</td></tr>
 * <tr><th>DLESEnameNick</th>  <td>nick name or casual name</td>  <td>Slim</td></tr>
 * <tr><th>DLESEnameSuffix</th>  <td>name suffix, like Sr or PhD</td>  <td>PhD</td></tr>
 * <tr><th>DLESEnameTitle</th>  <td>common title, like Mr, Ms, Dr</td>  <td>Dr</td></tr>
 * <tr><th>DLESEorg1</th>  <td>organization 1</td>  <td>UCAR</td></tr>
 * <tr><th>DLESEorg2</th>  <td>organization 2</td>  <td>U of Colorado</td></tr>
 * <tr><th>DLESEphone1</th>  <td>primary phone number</td>  <td>303-555-1212</td></tr>
 * <tr><th>DLESEphone2</th>  <td>secondary phone number</td>  <td>303-555-1212</td></tr>
 * <tr><th>DLESEpostalCode</th>  <td>street address: postalCode</td>  <td>81234-4321</td></tr>
 * <tr><th>DLESEprofResp</th>  <td>professional responsibility (may have multiple values)</td>  <td>teaching, student, administrator, ...</td></tr>
 * <tr><th>DLESEstate</th>  <td>street address: state</td>  <td>CO</td></tr>
 * <tr><th>DLESEurl</th>  <td>url</td>  <td>http://www.somesite.org</td></tr>
 * <tr><th>DLESEuserPasswordPhrase</th>  <td>password reminder phrase</td>  <td>Mother's name</td></tr>
 * <tr><th>DLESEworkSphere</th>  <td>work sphere (may have multiple values)</td>  <td>atmosphere, biosphere, solid earth, ...</td></tr>
 * </table>
 * <p>
 *
 * @param newName the new uid
 * @param attrStgs the names and values of the attributes.
 *   Each row i represents one attribute and it's values:
 *   attrs[i][0] is the String attribute name,
 *   and attrs[i][1 ... rowlen-1] are the String values.
 *   <p>
 *   Note: the attrStgs matrix need not be rectangular, since
 *   different attributes may have different numbers of values.
 *   <p>
 *   The "objectclass" attribute is added automatically,
 *   and should <b>not</b> be specified in attrStgs.
 */

public void addUserEntry(
	String newName,
	String[][] attrStgs)
throws LdapException
{
	chkstg( "new name", newName);
	chkstgmat( "attr stgs", attrStgs);

	int numadd = 1;			// num additional attributes
	String[][] newStgs;
	if (attrStgs == null || attrStgs.length == 0)
		newStgs = new String[ numadd][];
	else {
		newStgs = new String[ attrStgs.length + numadd][];
		System.arraycopy( attrStgs, 0, newStgs, numadd, attrStgs.length);
	}

	int kk = 0;
	newStgs[kk++] = new String[] { "objectclass", "top", "DLESEperson" };

	addEntryDn(
		adminDn,
		adminPswd,
		mkUserDn( newName),
		newStgs);
} // end addUserEntry




//================================================================

/**
 * Low level method: adds a new LDAP entry,
 * using the specified dn/pswd for authorization.
 *
 * @param authDn   the authorized dn (distinguished name) of the caller
 * @param password the password associated with authDn
 * @param newDn the new dn
 * @param attrStgs the names and values of the attributes.
 *   Each row i represents one attribute and it's values:
 *   attrs[i][0] is the String attribute name,
 *   and attrs[i][1 ... rowlen-1] are the String values.
 *   <p>
 *   Note: the attrStgs matrix need not be rectangular, since
 *   different attributes may have different numbers of values.
 */

public void addEntryDn(
	String authDn,
	String password,
	String newDn,
	String[][] attrStgs)
throws LdapException
{
	chkstg( "new dn", newDn);
	chkstgmat( "attr stgs", attrStgs);

	DirContext dirctx = getDirContext( authDn, password);
	Attributes attrs = mkAttrs( attrStgs);
	try { dirctx.createSubcontext( newDn, attrs); }
	catch( NamingException nexc) {
		if (bugs >= 1) {
			prtln("addEntryDn: nexc: " + nexc);
			nexc.printStackTrace();
			prtln();
			prtln("authDn: \"" + authDn + "\"");
			prtln("password: \"" + password + "\"");
			prtln("newDn: \"" + newDn + "\"");
			if (attrStgs == null)
				prtln("attrStgs: (null)");
			else {
				prtln("attrStgs.len: " + attrStgs.length);
				for (int ii = 0; ii < attrStgs.length; ii++) {
					prtnc("        attrStgs[" + ii + "]:");
					for (int jj = 0; jj < attrStgs[ii].length; jj++) {
						prtnc("  \"" + attrStgs[ii][jj] + "\"");
					}
					prtln("");
				}
			}
			prtln();
		}
		throw new LdapException("addEntryDn: exception", nexc);
	}
} // end addEntryDn




//================================================================


/**
 * Stores a serialized Java Object, and associated attributes,
 * using the specified uid/pswd for authorization.
 * See {@link #getUserObject getUserObject}.
 * <p>
 * <b> CAUTION:</b> Storing a null object leaves
 * a JNDI Context in the database, and on the
 * next getUserObject call the Context will be returned
 * instead of a user object.  Storing a null object
 * is <b>NOT RECOMMENDED!</b>
 *
 * @param authName  the uid of the caller
 * @param password the password associated with authName
 * @param userName the name of the user associated with this object
 * @param objectName the name associated with this object, such as "dcsState".
 * @param attrStgs the names and values of the attributes.
 *   Each row i represents one attribute and it's values:
 *   attrs[i][0] is the String attribute name,
 *   and attrs[i][1 ... rowlen-1] are the String values.
 *   <p>
 *   Note: the attrStgs matrix need not be rectangular, since
 *   different attributes may have different numbers of values.
 * @param obj the Java Object to be serialized.
 */

public void storeUserObject(
	String authName,
	String password,
	String userName,
	String objectName,
	Object obj)
throws LdapException
{
	chkstg( "user name", userName);
	chkstg( "object name", objectName);

	// In the future, attrstgs may be attributes specified
	// by the caller.  For now, it is empty.
	String[][] attrStgs = null;

	int numadd = 4;			// num additional attributes
	String[][] newStgs;
	if (attrStgs == null || attrStgs.length == 0)
		newStgs = new String[ numadd][];
	else {
		newStgs = new String[ attrStgs.length + numadd][];
		System.arraycopy( attrStgs, 0, newStgs, numadd, attrStgs.length);
	}
	int kk = 0;
	// coord with mkObjectDn
	newStgs[kk++] = new String[] { "objectclass", "top", "DLESEjavaObject" };
	newStgs[kk++] = new String[] { "DLESEou", "people"};
	newStgs[kk++] = new String[] { dleseUidTag, userName };
	newStgs[kk++] = new String[] { "DLESEobjectName", objectName };

	storeObjectDn(
		mkUserDn( authName),
		password,
		mkObjectDn( userName, objectName),
		newStgs,
		obj);
} // end storeUserObject



//================================================================


/**
 * Low level method: stores a serialized Java Object,
 * and associated attributes,
 * using the specified dn/pswd for authorization.
 * See {@link #getObjectDn getObjectDn}.
 *
 * @param authDn   the authorized dn (distinguished name) of the caller
 * @param password the password associated with authDn
 * @param newDn the new dn
 * @param attrStgs the names and values of the attributes.
 * This must contain ALL the needed attributes: objectclass, etc. <br>
 *   Each row i represents one attribute and it's values: <br>
 *   attrs[i][0] is the String attribute name, <br>
 *   and attrs[i][1 ... rowlen-1] are the String values. <br>
 *   <p>
 *   Note: the attrStgs matrix need not be rectangular, since
 *   different attributes may have different numbers of values.
 * @param obj the Java Object to be serialized.
 */

public void storeObjectDn(
	String authDn,
	String password,
	String curDn,
	String[][] attrStgs,
	Object obj)
throws LdapException
{
	chkstg( "cur dn", curDn);
	chkstgmat( "attr stgs", attrStgs);

	if (bugs >= 1) prtln("storeObjectDn: curDn: \"" + curDn + "\"");
	DirContext dirctx = getDirContext( authDn, password);
	Attributes attrs = mkAttrs( attrStgs);
	try { dirctx.rebind( curDn, obj, attrs); }
	catch( NamingException nexc) {
		if (bugs >= 1) {
			prtln("storeObjectDn: nexc: " + nexc);
			nexc.printStackTrace();
			prtln();
			prtln("authDn: \"" + authDn + "\"");
			prtln("password: \"" + password + "\"");
			prtln("curDn: \"" + curDn + "\"");
			if (attrStgs == null)
				prtln("attrStgs: (null)");
			else {
				prtln("attrStgs.len: " + attrStgs.length);
				for (int ii = 0; ii < attrStgs.length; ii++) {
					prtnc("        attrStgs[" + ii + "]:");
					for (int jj = 0; jj < attrStgs[ii].length; jj++) {
						prtnc("  \"" + attrStgs[ii][jj] + "\"");
					}
					prtln("");
				}
			}
			prtln("obj: \"" + obj + "\"");
			prtln();
		}
		throw new LdapException("storeObjectDn: exception", nexc);
	}
} // end storeObjectDn






//================================================================


/** Removes a serialized Java Object, and associated attributes,
 * using the specified uid/pswd for authorization.
 *
 * @param authName  the uid of the caller
 * @param password the password associated with authName
 * @param userName the name of the user associated with this object
 * @param objectName the name associated with this object, such as "dcsState".
 * <p>
 * Silly JNDI/LDAP spec: Returns void, with no exceptions, whether
 * or not the objectName existed before the call.
 */

public void removeUserObject(
	String authName,
	String password,
	String userName,
	String objectName)
throws LdapException
{
	chkstg( "user name", userName);
	chkstg( "object name", objectName);

	removeEntryDn(
		mkUserDn( authName),
		password,
		mkObjectDn( userName, objectName));
} // end removeUserObject

//================================================================

/** Retrieves a serialized Java Object,
 * using the specified uid/pswd for authorization.
 * See {@link #storeUserObject storeUserObject}.
 *
 * To retrieve the attributes, use
 * {@link #getUserObjectAttributes getUserObjectAttributes}.
 *
 * @param authName  the uid of the caller
 * @param password the password associated with authName
 * @param userName the name of the user associated with this object
 * @param objectName the name associated with this object.
 */

public Object getUserObject(
	String authName,
	String password,
	String userName,
	String objectName)
throws LdapException
{
	chkstg( "user name", userName);
	chkstg( "object name", objectName);

	return getObjectDn(
		mkUserDn( authName),
		password,
		mkObjectDn( userName, objectName));
} // end getUserObject


//================================================================


/**
 * Low level method: Retrieves a serialized Java Object,
 * using the specified dn/pswd for authorization.
 * See {@link #storeObjectDn storeObjectDn}.
 *
 * @param authDn   the authorized dn (distinguished name) of the caller
 * @param password the password associated with authDn
 * @param objectDn the dn of the object.
 */

public Object getObjectDn(
	String authDn,
	String password,
	String objectDn)
throws LdapException
{
	chkstg( "object dn", objectDn);

	Object obj = null;
	DirContext dirctx = getDirContext( authDn, password);
	try {
		obj = dirctx.lookup( objectDn);
		if (bugs >= 1 && obj instanceof Context) {
			Context ctx = (Context) obj;
			prtln("getObjectDn: got Context: " + ctx.getNameInNamespace());
		}
	}
	catch( NamingException nexc) {
		if (bugs >= 1) {
			prtln("getObjectDn: nexc: " + nexc);
			nexc.printStackTrace();
			prtln();
			prtln("authDn: \"" + authDn + "\"");
			prtln("password: \"" + password + "\"");
			prtln("objectDn: \"" + objectDn + "\"");
			prtln();
		}
		throw new LdapException("getObjectDn: exception", nexc);
	}
	return obj;
} // end getObjectDn



//================================================================

/**
 * <b>Deprecated:</b> Retrieves the attributes associated with
 * a serialized Java Object,
 * using the specified uid/pswd for authorization.
 * <p>
 * <b><i> This method is deprecated since currently no
 *    user-accessible attributes are stored with Java objects.
 *    At some future date we could change this, allowing
 *    attributes to be stored with Java objects.
 *    See {@link #storeUserObject storeUserObject}.
 * </i></b>
 * <p>
 *
 * See {@link #storeUserObject storeUserObject}.
 * To retrieve the Object itself, use
 * {@link #getUserObject getUserObject}.
 *
 * @param authName  the uid of the caller
 * @param password the password associated with authName
 * @param userName the name of the user associated with this object
 * @param objectName the name associated with this object.
 * @param attrNames an array of attribute names to be returned.
 *    If null, all available attributes are returned.
 *
 * @return   An LdapEntry representing one returned entry.
 *    Throws {@link LdapNotFoundException LdapNotFoundException}
 *    if entry not found.
 *    If attrNames was specified, the LdapEntry has the same
 *    attributes in the specified order.
 *    <p>
 *    If attrNames is null,
 *    the LdapEntry contains all available attributes for the entry,
 *    sorted by attribute name.
 */

public LdapEntry getUserObjectAttributes(
	String authName,
	String password,
	String userName,
	String objectName,
	String[] attrNames)
throws LdapException
{
	chkstg( "user name", userName);
	chkstg( "object name", objectName);
	chkstgs( "attr names", attrNames, true);		// may be null

	return getAttributesDn(
		mkUserDn( authName),
		password,
		mkObjectDn( userName, objectName),
		attrNames);
} // end getUserObjectAttributes


//================================================================


/**
 * <b>Deprecated:</b> Renames an user entry in the LDAP database,
 * using a the admin dn/pswd for authorization.
 * <p>
 * <b>CAUTION:</b> This will throw an Exception
 * if any user objects are stored for this user,
 * since OpenLDAP does not yet support renaming subtrees.
 * <b> NOT RECOMMENDED!</b>
 *
 * @param authName  the uid of the caller
 * @param password the password associated with authName
 * @param oldName the old uid.
 * @param newName the new uid.
 */

public void renameUserEntry(
	String oldName,
	String newName)
throws LdapException
{
	chkstg( "old name", oldName);
	chkstg( "new name", newName);

	renameEntryDn(
		adminDn,
		adminPswd,
		mkUserDn( oldName),
		mkUserDn( newName));
}


//================================================================


/**
 * <b>Deprecated:</b> Low level method: renames an entry in the LDAP database,
 * using the specified dn/pswd for authorization.
 *
 * @param host     the URI of the LDAP server.
 * @param authDn   the authorized dn (distinguished name) of the caller
 * @param password the password associated with authDn
 * @param oldDn the old dn.
 * @param newDn the new dn.
 */

public void renameEntryDn(
	String authDn,
	String password,
	String oldDn,
	String newDn)
throws LdapException
{
	chkstg( "old dn", oldDn);
	chkstg( "new dn", newDn);

	DirContext dirctx = getDirContext( authDn, password);
	try { dirctx.rename( oldDn, newDn); }
	catch( NamingException nexc) {
		if (bugs >= 1) {
			prtln("renameEntryDn: nexc: " + nexc);
			nexc.printStackTrace();
			prtln();
			prtln("authDn: \"" + authDn + "\"");
			prtln("password: \"" + password + "\"");
			prtln("oldDn: \"" + oldDn + "\"");
			prtln("newDn: \"" + newDn + "\"");
			prtln();
		}
		throw new LdapException("renameEntryDn: exception", nexc);
	}
} // end renameEntryDn


//================================================================


/**
 * Removes an entry from the LDAP database,
 * using a the admin dn/pswd for authorization.
 * <p>
 * Silly JNDI/LDAP spec: Returns void, with no exceptions, whether
 * or not the subjectName existed before the call.
 *
 * @param subjectName the uid to be removed.
 */

public void removeUserEntry(
	String subjectName)
throws LdapException
{
	chkstg( "subject name", subjectName);

	removeEntryDn(
		adminDn,
		adminPswd,
		mkUserDn( subjectName));
}





//================================================================

/**
 * Low level method: removes an entry from the LDAP database,
 * using the specified dn/pswd for authorization.
 * <p>
 * Silly JNDI/LDAP spec: Returns void, with no exceptions, whether
 * or not the subjectDn existed before the call.
 *
 * @param host     the URI of the LDAP server.
 * @param authDn   the authorized dn (distinguished name) of the caller
 * @param password the password associated with authDn
 * @param subjectDn the dn to be removed.
 */

public void removeEntryDn(
	String authDn,
	String password,
	String subjectDn)
throws LdapException
{
	chkstg( "subject dn", subjectDn);

	// Insure entry exists.  If not, getAttributesDn will throw
	// LdapNotFoundException.
	getAttributesDn(
		authDn,
		password,
		subjectDn,
		null);

	DirContext dirctx = getDirContext( authDn, password);
	try { dirctx.unbind( subjectDn); }
	catch( NamingException nexc) {
		if (bugs >= 1) {
			prtln("removeEntryDn: nexc: " + nexc);
			nexc.printStackTrace();
			prtln();
			prtln("authDn: \"" + authDn + "\"");
			prtln("password: \"" + password + "\"");
			prtln("subjectDn: \"" + subjectDn + "\"");
			prtln();
		}
		throw new LdapException("removeEntryDn: exception", nexc);
	}
} // end removeEntryDn





//================================================================

/**
 * Tests to see if a user entry exists in the LDAP database,
 * using a the admin dn/pswd for authorization.
 *
 * @param subjectName the uid to be removed.
 */

public boolean userExists(
	String subjectName)
throws LdapException
{
	chkstg( "subject name", subjectName);

	boolean bres = true;
	try { LdapEntry entry = getUserAttributes( subjectName, null); }
	catch( LdapNotFoundException nfe) {
		bres = false;
	}
	return bres;
}


//================================================================


/**
 * Creates an empty list of names,
 * using a the admin dn/pswd for authorization,
 * and adds the owner as a DLESElistMember of the list.
 *
 * @param listName the name of the list to be created.
 * On open lists, the user can add/remove themself.
 * On all other lists, only the list owner can add/remove DLESEloginNames.
 * @param ownerName the uid of the list owner.
 */

public void createList(
	String listName,
	String ownerName)
throws LdapException
{
	chkstg( "list name", listName);
	chkstg( "owner name", ownerName);

	// Insure all intermediate entries ("superior entries") exist.
	// Pull apart the listName and rebuild each substring as a dn.
	String[] parts = mkListNameParts( listName);
	if (parts.length < 2) throw new LdapException(
		"createList: invalid list name", null);

	// Coord with mkListDn, mkListAttrs
	// Skip the first entry, "list/" or "groups/".
	// Skip the last entry, which is the list itself that we will create.
	String testName = parts[0];
	for (int ii = 1; ii < parts.length - 1; ii++) {
		testName += "/" + parts[ii];
		String testDn = mkListDn( testName, false);		// finalflag = false
		try {
			getAttributesDn(
				adminDn,
				adminPswd,
				testDn,
				null);		// attrNames
		}
		catch( LdapNotFoundException nef) {
			addEntryDn(
				adminDn,
				adminPswd,
				testDn,
				mkSetAttrs( parts, ii));
		}
	}
			
	addEntryDn(
		adminDn,
		adminPswd,
		mkListDn( listName),
		mkListAttrs( listName, ownerName));

	// Add the owner as a DLESElistMember of the list
	addAttributeValueDn(
		adminDn,
		adminPswd,
		mkListDn( listName),
		"DLESElistMember",				// attrName,
		mkUserDn( ownerName));		// attrValue
}
	

//================================================================

/**
 * Removes a list of names,
 * using a the admin dn/pswd for authorization.
 * <p>
 * Silly JNDI/LDAP spec: Returns void, with no exceptions, whether
 * or not the listName existed before the call.
 *
 * @param listName the name of the list to be created.
 */

public void removeEntireList(
	String listName)
throws LdapException
{
	chkstg( "list name", listName);

	removeEntryDn(
		adminDn,
		adminPswd,
		mkListDn( listName));
}

	

//================================================================

/**
 * Adds a single "DLESElistMember" name to the specified list.
 *
 * @param authName  the uid of the caller
 * @param password the password associated with authName
 * @param listName the name of the list to be created.
 * @param userName the uid to be added to the list.
 */

public void addListName(
	String authName,
	String password,
	String listName,
	String userName)
throws LdapException
{
	chkstg( "list name", listName);
	chkstg( "user name", userName);

	addAttributeValueDn(
		mkUserDn( authName),
		password,
		mkListDn( listName),
		"DLESElistMember",				// attrName,
		mkUserDn( userName));
}
	
//================================================================

/**
 * Removes a single "DLESElistMember" name from the specified list.
 *
 * @param authName  the uid of the caller
 * @param password the password associated with authName
 * @param listName the name of the list to be created.
 * @param userName the uid to be removed from the list.
 */

public void removeListName(
	String authName,
	String password,
	String listName,
	String userName)
throws LdapException
{
	chkstg( "list name", listName);
	chkstg( "user name", userName);

	removeAttributeValueDn(
		mkUserDn( authName),
		password,
		mkListDn( listName),
		"DLESElistMember",				// attrName,
		mkUserDn( userName));
}


//================================================================


/**
 * Returns all the "DLESElistMember" attribute values from a list,
 * as full dn's (distinguished names),
 * using a the specified dn/pswd for authorization.
 * Returns null if no DLESElistMembers in the list.
 *
 * @param authName  the uid of the caller
 * @param password the password associated with authName
 * @param listName the name of the list to be created.
 */

public String[] getListMembers(
	String authName,
	String password,
	String listName)
throws LdapException
{
	chkstg( "list name", listName);

	return getListMembersDn(
		mkUserDn( authName),
		password,
		mkListDn( listName));
}




//================================================================


/**
 * Returns all the "DLESElistMember" attribute values from a list,
 * as full dn's (distinguished names),
 * using a the admin dn/pswd for authorization.
 * Returns null if no DLESElistMembers in the list.
 *
 * @param authName  the uid of the caller
 * @param password the password associated with authName
 * @param listName the name of the list to be created.
 */

public String[] getListMembers(
	String listName)
throws LdapException
{
	chkstg( "list name", listName);

	return getListMembersDn(
		adminDn,
		adminPswd,
		mkListDn( listName));
}


//================================================================


/**
 * Low level: Returns all the "DLESElistMember" attribute values from a list,
 * as full dn's (distinguished names);
 * returns null if no DLESElistMembers in the list.
 *
 * @param authName  the uid of the caller
 * @param password the password associated with authName
 * @param listName the name of the list to be created.
 */

public String[] getListMembersDn(
	String authDn,
	String password,
	String listDn)
throws LdapException
{
	chkstg( "list dn", listDn);

	// getAttributesDn will throw LdapNotFoundException if entry not found.
	LdapEntry entry = getAttributesDn(
		authDn,
		password,
		listDn,
		new String[] {"DLESElistMember"});		// attrNames
	String[] names = null;
	if (entry != null && entry.getAttrsRows() > 0) {
		// The first ele in each row of attrs is the attr name.
		// Get all elements of the one and only row,
		// row 0, for "DLESElistMembers".
		names = entry.getAttrStrings( 0);
	}
	return names;
}


//================================================================


/**
 * Returns all the "DLESElistMember" attribute values from a list,
 * as uids (not full dn's);
 * returns null if no DLESElistMembers in the list.
 * See also  {@link #getListMembers getListMembers}.
 *
 * @param authName  the uid of the caller
 * @param password the password associated with authName
 * @param listName the name of the list to be created.
 */

public String[] getListNames(
	String authName,
	String password,
	String listName)
throws LdapException
{
	chkstg( "list name", listName);

	String[] uids = null;
	String[] dns = getListMembers( authName, password, listName);
	if (dns != null) {
		uids = new String[ dns.length];
		for (int ii = 0; ii < uids.length; ii++) {
			uids[ii] = mkUid( dns[ii]);
		}
	}
	return uids;
}





//================================================================


/**
 * Returns all the "DLESElistMember" attribute values from a list,
 * as uids (not full dn's);
 * returns null if no DLESElistMembers in the list.
 * See also  {@link #getListMembers getListMembers}.
 *
 * @param listName the name of the list to be created.
 */

public String[] getListNames(
	String listName)
throws LdapException
{
	chkstg( "list name", listName);

	String[] uids = null;
	String[] dns = getListMembersDn(
		adminDn,
		adminPswd,
		mkListDn(listName));
	if (dns != null) {
		uids = new String[ dns.length];
		for (int ii = 0; ii < uids.length; ii++) {
			uids[ii] = mkUid( dns[ii]);
		}
	}
	return uids;
}



	
//================================================================


/**
 * Returns the desired attributes for each "DLESElistMember"
 * attribute value from a list,
 * using the specified dn/pswd for authorization.
 * Returns null if no DLESElistMembers in the list.
 * See also  {@link #getListMembers getListMembers}.
 *
 * @param authName  the uid of the caller
 * @param password the password associated with authName
 * @param listName the name of the list to be created.
 * @param attrNames an array of attribute names to be returned.
 *    If null, all available attributes are returned.
 */

public LdapEntry[] getListAttributes(
	String authName,
	String password,
	String listName,
	String[] attrNames)
throws LdapException
{
	chkstg( "list name", listName);
	chkstgs( "attr names", attrNames, true);		// may be null

	LdapEntry[] entries = null;
	String[] dns = getListMembers( authName, password, listName);
	if (dns != null) {
		entries = new LdapEntry[ dns.length];
		for (int ii = 0; ii < dns.length; ii++) {
			// ### should we catch the LdapNotFoundException?
			entries[ii] = getAttributesDn(
				authName,
				password,
				dns[ii],
				attrNames);
		}
	}
	return entries;
}

//================================================================


/**
 * Returns the desired attributes for each "DLESElistMember"
 * attribute value from a list,
 * using a the admin dn/pswd for authorization.
 * Returns null if no DLESElistMembers in the list.
 * See also  {@link #getListMembers getListMembers}.
 *
 * @param listName the name of the list to be created.
 * @param attrNames an array of attribute names to be returned.
 *    If null, all available attributes are returned.
 */

public LdapEntry[] getListAttributes(
	String listName,
	String[] attrNames)
throws LdapException
{
	chkstg( "list name", listName);
	chkstgs( "attr names", attrNames, true);		// may be null

	LdapEntry[] entries = null;
	String[] dns = getListMembers( listName);
	if (dns != null) {
		entries = new LdapEntry[ dns.length];
		for (int ii = 0; ii < dns.length; ii++) {
			// ### should we catch the LdapNotFoundException?
			entries[ii] = getAttributesDn(
				adminDn,
				adminPswd,
				dns[ii],
				attrNames);
		}
	}
	return entries;
}


//================================================================


/**
 * Returns the desired property value; throws an LdapException
 * if not found.
 *
 * @param propName  The name of the desired property.
 * @param props  The Properties container.
 * @param pfile  The name of the properties file: not opened,
 *               only used for error messages.
 */

protected String getProperty(
	String propName,
	Properties props,
	String pfile)
throws LdapException
{
	chkstg( "prop name", propName);
	chkstg( "pfile name", pfile);

	String res = props.getProperty( propName);
	if (res == null || res.length() == 0)
		throw new LdapException(
			"LdapClient: invalid " + propName + " in properties file \""
			+ pfile + "\"", null);
	return res;
}	


	
//================================================================

/**
 * Given a UID, returns the corresponding full dn (distinguished name).
 */

protected String mkUserDn( String userName) {
	return dleseUidTag + "=" + userName + ",DLESEou=people," + dnbase;
	// Coord with addUserEntry(), mkUid()
}

//================================================================

/**
 * Given a dn for a user entry, returns the corresponding UID.
 */

private String mkUid( String dn)
throws LdapException
{
	// Coord with mkUserDn()
	int ix = dn.indexOf(",");
	if ( (! dn.startsWith( dleseUidTag + "="))
		|| (! dn.endsWith( dnbase))
		|| (ix < 0))
	{
		throw new LdapException("mkUid: invalid dn: \"" + dn + "\"", null);
	}
	String uid = dn.substring( dleseUidTag.length() + 1, ix);
	return uid;
}


//================================================================



/**
 * Given a UID and arbitrary object name,
 * returns the corresponding full dn (distinguished name)
 * for the object to be stored.
 */

private String mkObjectDn( String userName, String objectName) {
	return "DLESEobjectName=" + objectName + "," + mkUserDn( userName);
}

//================================================================


/**
 * Given a list name,
 * returns the corresponding full dn (distinguished name).
 *
 * @param listName the name of the list, in the form:<br>
 *  <code> ou/dleseSet/.../dleseSet/dleseList </code> <br>
 * Examples: <br>
 *  <code> lists/open/hydrology </code> <br>
 *  <code> lists/closed/steering/voting </code> <br>
 *  <code> groups/DPC/cataloguers </code> <br>
 * <p>
 * On open lists (<code>lists/open/*</code>),
 * the user can add/remove him/herself.
 * On all other lists,
 * only the list owner can add/remove DLESElistMembers.
 */

private String mkListDn( String listName)
throws LdapException
{
	return mkListDn( listName, true);
}


//================================================================


/**
 * Returns a dn (distinguished name) corresponding to the
 * specified list name.  For example, if listName is <br>
 * &nbsp;&nbsp;&nbsp;&nbsp; <code>"lists/open/hydrology"</code> <br>
 * the returned dn is:
 * &nbsp;&nbsp;&nbsp;&nbsp; <code>"DLESElistName=hydrology,DLESEsetName=open,DLESEou=lists,dc=dlese,dc=org"</code> <br>
 *
 * @param listName The list name, like "lists/open/hydrology".
 * @param listFlag If true, the constructed dn will start
 * with "DLESElistName=...";
 * if false the dn will start with "DLESEsetName=...".
 */

private String mkListDn( String listName, boolean listflag)
throws LdapException
{
	String[] nameParts = mkListNameParts( listName);

	// Coord with mkListAttrs
	String dn = "DLESEou=" + nameParts[0] + "," + dnbase;
	for (int ii = 1; ii < nameParts.length - 1; ii++) {
		dn = "DLESEsetName=" + nameParts[ii] + "," + dn;
	}
	String lastTag;
	if (listflag) lastTag = "DLESElistName=";
	else lastTag = "DLESEsetName=";
	dn = lastTag + nameParts[ nameParts.length - 1] + "," + dn;
	return dn;
}


//================================================================

/**
 * Splits a hierarchical name like <code>"alpha/bravo/charlie/delta"</code>
 * into an array of the component parts:
 * <code> String[] {"alpha", "bravo", "charlie", "delta"} </code>.
 */

private String[] mkListNameParts( String listName)
throws LdapException
{
	int previx, ix;

	if (listName == null
		|| listName.length() == 0
		|| listName.startsWith("/")
		|| listName.endsWith("/")
		|| listName.indexOf("/") < 0)
	{
		throw new LdapException("mkListNameParts: malformed listName: \""
			+ listName + "\"", null);
	}

	// Count number of slashes
	int numslash = 0;
	previx = 0;
	while (true) {
		ix = listName.indexOf("/", previx );
		if (ix < 0) break;
		numslash++;
		previx = ix + 1;
	}

	String[] nameParts = new String[ numslash + 1];
	int kk = 0;
	previx = 0;
	while (true) {
		ix = listName.indexOf("/", previx );
		if (ix < 0) break;
		nameParts[kk++] = listName.substring( previx, ix);
		previx = ix + 1;
	}
	nameParts[kk++] = listName.substring( previx);		// last part
	return nameParts;
}


//================================================================

/**
 * Given an array of name parts, returns the attributes array
 * needed to create that set.
 *
 * @param nameParts The array of name parts, like
 * {"lists","open","hydrology"}.
 * @param imax The max index in nameParts to use: generates attributes
 * for nameParts[ 0 &lt;= i &lt;= imax].
 */

private String[][] mkSetAttrs( String[] nameParts, int imax)
throws LdapException
{
	// Coord with mkListDn, createList

	int numSets = imax;
	String[] setNames = new String[ 1 + numSets];
	setNames[0] = "DLESEsetName";
	System.arraycopy( nameParts, 1, setNames, 1, numSets);

	String[][] attrStgs = new String[][] {
		{ "objectclass", "top", "DLESEset" },
		{ "DLESEou", nameParts[0] },
		setNames
	};
	return attrStgs;
}



//================================================================

/**
 * Given a list name and owner, returns the attributes array
 * needed to create that list.
 */

private String[][] mkListAttrs( String listName, String ownerName)
throws LdapException
{
	// Coord with mkListDn, createList

	String[] nameParts = mkListNameParts( listName);
	int numSets = nameParts.length - 2;
	String[] setNames = new String[ 1 + numSets];
	setNames[0] = "DLESEsetName";
	System.arraycopy( nameParts, 1, setNames, 1, numSets);

	String[][] attrStgs = new String[][] {
		{ "objectclass", "top", "DLESElist" },
		{ "DLESEou", nameParts[0] },
		setNames,
		{ "DLESElistName", nameParts[ nameParts.length - 1] },
		{ "DLESElistOwner", mkUserDn( ownerName) }
	};
	return attrStgs;
}



//================================================================

/**
 * Creates a DirContext for the given dn and password.
 *
 * @param authDn  The dn (distinguished name) to use for authentication.
 * @param password  The password associated with authDn.
 */
private DirContext getDirContext(
	String authDn,
	String password)
throws LdapException
{
	if (bugs >= 1) {
		prtln("getDirContext: hostUrl: \"" + hostUrl + "\"");
		prtln("getDirContext: authDn: \"" + authDn + "\"");
		prtln("getDirContext: password: \"" + password + "\"");
	}
	String INITCTX = "com.sun.jndi.ldap.LdapCtxFactory";

	Hashtable env = new Hashtable();
	env.put( Context.INITIAL_CONTEXT_FACTORY, INITCTX);
	env.put( Context.PROVIDER_URL, hostUrl);
	env.put( Context.SECURITY_AUTHENTICATION, "simple");
	env.put( Context.SECURITY_PRINCIPAL, authDn);
	env.put( Context.SECURITY_CREDENTIALS, password);

	DirContext dirctx = null;
	try { dirctx = new InitialDirContext( env); }
	catch( NamingException nexc) {
		if (bugs >= 1) {
			prtln("getDirContext: nexc: " + nexc);
			nexc.printStackTrace();
			prtln();
			prtln("authDn: \"" + authDn + "\"");
			prtln("password: \"" + password + "\"");
			prtln();
		}
		throw new LdapException("getDirContext: exception", nexc);
	}

	return dirctx;
}


//================================================================

/**
 * Converts a 2-dimensional array of String into a JNDI Attributes object.
 * <p>
 * The input attrstgs is structured as follows:<br>
 *
 * Each row i represents one attribute and it's values:<br>
 * attrs[i][0] is the String attribute name, <br>
 * and attrs[i][1 ... rowlen-1] are the Object values. <br>
 * <p>
 * The attrstgs matrix need not be rectangular, since
 * different attributes may have different numbers of values.
 * <p>
 * If an attribute name is "userpassword" (case insensitive),
 * it's value(s) are converted to the SHA-1 message digests.
 * This way passwords in the database should always be
 * in encrypted form.
 */


private Attributes mkAttrs( String[][] attrstgs)
throws LdapException
{
	Attributes attrs = null;
	if (attrstgs != null && attrstgs.length > 0) {
		attrs = new BasicAttributes( true);		// ignoreCase = true
		for (int iat = 0; iat < attrstgs.length; iat++) {
			String name = attrstgs[iat][0];
			Attribute battr = new BasicAttribute( name, false);
					// ordered = false
			for (int ival = 1; ival < attrstgs[iat].length; ival++) {
				String val = attrstgs[iat][ival];
				if (name.toLowerCase().equals("userpassword"))
					val = cryptDigest( "SSHA", val);
				battr.add( val);
				if (bugs >= 1) prtln("attr " + name + ":    \""
					+ val + "\"");
			}
			attrs.put( battr);
		}
	}
	return attrs;
} // end mkAttrs


//================================================================


/**
 * Converts a JNDI SearchResult into our LdapEntry object.
 */

private LdapEntry decodeSearchResult(
	SearchResult sr,
	String contextBase,
	String[] attrNames)
throws LdapException
{
	// print dn == distinguished name
	String dn;
	if (sr.getName() == null || sr.getName().length() == 0)
		dn = contextBase;
	else dn = sr.getName() + "," + contextBase;

	Attributes attrraw = sr.getAttributes();
	return decodeAttributes( dn, attrNames, attrraw);
}




//================================================================


/**
 * Converts a JNDI Attributes into our LdapEntry object.
 *
 * @param dn The dn associated with the search.
 * @param attrNames an array of attribute names to be returned.
 *    If null, all available attributes are returned.
 * @param attrraw Then JNDI Attributes object resulting from the search.
 */

private LdapEntry decodeAttributes(
	String dn,
	String[] attrNames,
	Attributes attrraw)
throws LdapException
{
	LdapEntry entry = new LdapEntry( dn);
	if (bugs >= 1) prtln("\nReturned dn: " + dn);
	if (attrraw.size() == 0) {
		if (bugs >= 1) prtln("No attributes returned");
		if (attrNames != null) {
			// No attrs returned, but attr names were specified, so
			// we must return an empty array with just the attr names.
			entry.allocAttrs( attrNames.length);
			for (int iattr = 0; iattr < attrNames.length; iattr++) {
				entry.allocAttrsRow( iattr, 1);		// all rows just 1 long
				entry.setAttr( iattr, 0, attrNames[iattr]);
			}
		}
	}
	else {
		NamingEnumeration attrenum = attrraw.getAll();
		ArrayExc attrvec = fixEnum( attrenum);
		if (attrvec.exc != null)
			throw new LdapException("attrvec hidden exception", attrvec.exc);

		// If attrNames specified, use that format
		if (attrNames != null) {
			entry.allocAttrs( attrNames.length);

			// Fill in each requested attr name
			for (int iattr = 0; iattr < attrNames.length; iattr++) {
				// Search result set for matching name
				BasicAttribute foundattr = null;
				for (int ires = 0; ires < attrvec.vals.length; ires++) {
					BasicAttribute testattr = (BasicAttribute)
						attrvec.vals[ ires];
					if (testattr.getID().equals( attrNames[ iattr])) {
						foundattr = testattr;
						break;
					}
				}
				if (foundattr == null) {
					entry.allocAttrsRow( iattr, 1);		// just the attr name
					entry.setAttr( iattr, 0, attrNames[ iattr]);
				}
				else entry.setAttrsRow( iattr, decodeValues( foundattr));
			}
		}

		// Else no attrNames: return ALL attributes.
		else {
			entry.allocAttrs( attrvec.vals.length);

			// Sort the attributes by attribute ID (attribute name)
			Arrays.sort( attrvec.vals, new Comparator() {
				public int compare( Object obja, Object objb) {
					BasicAttribute attra = (BasicAttribute) obja;
					BasicAttribute attrb = (BasicAttribute) objb;
					return attra.getID().compareTo( attrb.getID());
				}
				public boolean equals( Object obj) { return false; }
			});
	
			for (int iattr = 0; iattr < attrvec.vals.length; iattr++) {
				entry.setAttrsRow( iattr, decodeValues(
					(BasicAttribute) attrvec.vals[ iattr]));
			}
		}
	}
	return entry;
} // end decodeAttributes



//================================================================

/**
 * Converts a JNDI BasicAttribute into an array of Object[].
 * <p>
 * The returned Object[] resvec has:<br>
 * resvec[0] == The String attribute name<br>
 * resvec[1] ... resvec[n] == The n values associated with the attribute.<br>
 * <p>
 * Note that the returned values may be any serializable Object,
 * not necessarily Strings.
 */

private Object[] decodeValues( BasicAttribute battr)
throws LdapException
{
	Object[] resvec = null;
	String attrid = battr.getID();
	if (bugs >= 1) prtln("decodeValues: attrid: \"" + attrid + "\"");
	NamingEnumeration valenum = null;
	ArrayExc valvec = null;
	try { valenum = battr.getAll(); }
	catch( NamingException nexc) {
		if (bugs >= 1) {
			prtln("decodeValues: nexc: " + nexc);
			nexc.printStackTrace();
			prtln();
			prtln("battr: \"" + battr + "\"");
			prtln("attrid: \"" + attrid + "\"");
			prtln();
		}
		throw new LdapException("decodeValues: exception", nexc);
	}

	valvec = fixEnum( valenum);
	if (valvec.exc != null)
		throw new LdapException("valvec hidden exception", valvec.exc);

	if (valvec.vals.length == 0) {
		resvec = new Object[1];
		resvec[0] = attrid;
	}
	else {
		// Sort the values
		Arrays.sort( valvec.vals, new Comparator() {
			public int compare( Object obja, Object objb) {
				String stga = obja.toString();
				String stgb = objb.toString();
				return stga.compareTo( stgb);
			}
			public boolean equals( Object obj) { return false; }
		});

		resvec = new Object[ 1 + valvec.vals.length];
		resvec[0] = attrid;
		for (int ival = 0; ival < valvec.vals.length; ival++) {
			if (bugs >= 1) prtln("decodeValues: value " + ival + ": "
				+ valvec.vals[ ival].getClass().getName() + "  \""
				+ valvec.vals[ ival] + "\"");
			resvec[ 1 + ival] = valvec.vals[ ival];
		}
	}
	return resvec;
} // end decodeValues

//================================================================


/**
 * Decodes a javax.naming.NamingEnumeration.
 * Java's NamingEnumeration is a kludge: it holds a list
 * and a hidden exception waiting to be thrown after the last
 * list element is retrieved.  But there's no way to discover
 * the hidden exception without retrieving all the list elements.
 */

private ArrayExc fixEnum( NamingEnumeration enumeration) {
	LinkedList lst = new LinkedList();
	Exception hiddenExc = null;

	try {		// catch the hidden exception
		while (enumeration.hasMore()) {
			lst.add( enumeration.next());
		}
	}
	catch( NamingException nex) {
		hiddenExc = nex;
	}
	Object[] vals = lst.toArray();
	return new ArrayExc( vals, hiddenExc);
}


//================================================================

/**
 * Returns the SHA-1 message digest for the specified String;
 * used for encrypting passwords.
 * Silly Java SDK doesn't support SSHA, so we use SHA-1.
 */
private String cryptDigest( String alg, String stg)
throws LdapException
{
	String finalstg = null;
	byte[] pswdbytes = stg.getBytes();
	try {
		byte[] finalbytes;
		MessageDigest md = MessageDigest.getInstance("SHA");
		if (alg.equals("SHA")) {
			finalbytes = md.digest( pswdbytes);
			finalstg = "{SHA}" + encodeBase64( finalbytes);
		}
		else if (alg.equals("SSHA")) {
			// calc some random salt:
			byte[] saltbytes = new byte[20];
			new Random().nextBytes( saltbytes);
			if (bugs >= 1) {
				prtln("crypt: saltbytes: \"");
				for (int ii = 0; ii < saltbytes.length; ii++) {
					prtnc(" ");
					if (saltbytes[ii] >= 0 && saltbytes[ii] < 16) prtnc("0");
					prtnc( Integer.toHexString( 0xff & saltbytes[ii]));
				}
				prtln("");
			}

			// allbytes = pswdbytes + saltbytes
			byte[] allbytes = new byte[ pswdbytes.length + saltbytes.length];
			System.arraycopy( pswdbytes, 0, allbytes, 0,
				pswdbytes.length);
			System.arraycopy( saltbytes, 0, allbytes, pswdbytes.length,
				saltbytes.length);

			// finalbytes = enc( allbytes) + saltbytes
			byte[] digestbytes = md.digest( allbytes);
			finalbytes = new byte[ digestbytes.length + saltbytes.length];
			System.arraycopy( digestbytes, 0, finalbytes, 0,
				digestbytes.length);
			System.arraycopy( saltbytes, 0, finalbytes, digestbytes.length,
				saltbytes.length);
			finalstg = "{SSHA}" + encodeBase64( finalbytes);
		}
		else throw new LdapException("cryptDigest: invalid alg: \""
			+ alg + "\"", null);
		if (bugs >= 1) prtln("crypt: stg: \"" + stg
			+ "\"  digest: \"" + finalstg + "\"");
	}
	catch( Exception exc) {
		if (bugs >= 1) prtln("cryptDigest: Exception: " + exc);
		exc.printStackTrace();
		throw new LdapException("cryptDigest: exception", exc);
	}
	return finalstg;
}


//================================================================

/**
 * Returns the base-64 encoding of the byte vector bvec.
 */

private String encodeBase64( byte[] bvec)
throws LdapException
{
	return encodeBase64( bvec, 0, bvec.length);
}


//================================================================


/**
 * Returns the base-64 encoding of the byte vector bvec.
 * See: http://www.faqs.org/rfcs/rfc2045.html   section 6.8
 *
 * @param bvec    the input byte array
 * @param offset  the starting offset within bvec
 * @param limit   1 + the offset of the last byte to be encoded in bvec
 */

private String encodeBase64( byte[] bvec, int offset, int limit)
throws LdapException
{

	String charlist = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
	char pad = '=';
	int in0, in1, in2;
	int out0, out1, out2, out3;
	int ii;

	if (limit > bvec.length)
		throw new LdapException("encodeBase64: limit > bvec.len", null);

	StringBuffer outbuf = new StringBuffer();
	for (ii = offset; ii < limit; ii += 3) {

		// in_i = i'th input byte
		in0 = bvec[ii];
		if (ii + 1 < limit) in1 = bvec[ii + 1];
		else in1 = 0;
		if (ii + 2 < limit) in2 = bvec[ii + 2];
		else in2 = 0;

		// out_i = i'th output group of 6 bits
		out0 = 0x3F & ( in0 >> 2);		// 6 bits
		out1 = 0x3F & ( (in0 << 4) | ((in1 >> 4) & 0x0F));	// 2 + 4 bits
		out2 = 0x3F & ( (in1 << 2) | ((in2 >> 6) & 0x03));	// 4 + 2 bits
		out3 = 0x3F & (  in2 & 0x3F);	// 6 bits

		outbuf.append( charlist.charAt( out0));
		outbuf.append( charlist.charAt( out1));
		if (ii + 1 < limit) outbuf.append( charlist.charAt( out2));
		else outbuf.append( pad);
		if (ii + 2 < limit) outbuf.append( charlist.charAt( out3));
		else outbuf.append( pad);

		// Make output lines 60 bytes long.
		// That's 15 units, or 45 input bytes.
		if (ii > 0 && ii % 45 == 0 && ii + 2 < limit)
			outbuf.append('\n');
	}
	return new String( outbuf);
}



//================================================================

private void chkstg( String name, String stg)
throws LdapException
{
	if (stg == null || stg.length() == 0)
		throw new LdapException("The \"" + name + "\" is empty", null);
	if (Character.isWhitespace( stg.charAt(0)))
		throw new LdapException("The \"" + name
			+ "\" has leading white space: \"" + stg + "\"", null);
	if (Character.isWhitespace( stg.charAt( stg.length() - 1)))
		throw new LdapException("The \"" + name
			+ "\" has trailing white space: \"" + stg + "\"", null);
}

//================================================================

private void chkstgs( String name, String[] stgs, boolean mayNull)
throws LdapException
{
	int ii;
	if (stgs == null || stgs.length == 0) {
		if ( ! mayNull)
			throw new LdapException("The \"" + name + "\" is empty", null);
	}
	else {
		for (ii = 0; ii < stgs.length; ii++) {
			chkstg( name, stgs[ii]);
		}
	}
}

//================================================================

private void chkstgmat( String name, String[][] stgmat)
throws LdapException
{
	int ii;
	if (stgmat == null || stgmat.length == 0)
		throw new LdapException("The \"" + name + "\" is empty", null);
	for (ii = 0; ii < stgmat.length; ii++) {
		chkstgs( name, stgmat[ii], false);		// may not be null
	}
}

//================================================================

/**
 * Prints a single line, without a final newline.
 */

private void prtnc( String msg) {
	System.out.print( msg);
}



//================================================================

/**
 * Prints a single newline.
 */

private void prtln() {
	System.out.println();
}


//================================================================


/**
 * Prints a single line, including a final newline.
 */

private void prtln( String msg) {
	System.out.println( msg);
}


//================================================================


} // end class TestLdap


//================================================================

/**
 * Replacement for Java's brain-dead javax.naming.NamingEnumeration.
 * Simply holds an array of values and an exception.
 * <p>
 * Java's NamingEnumeration is a kludge: it holds a list
 * and a hidden exception waiting to be thrown after the last
 * list element is retrieved.  But there's no way to discover
 * the hidden exception without retrieving all the list elements.
 */

class ArrayExc {
	Object[] vals = null;
	Exception exc = null;

	ArrayExc( Object[] vals, Exception exc) {
		this.vals = vals;
		this.exc = exc;
	}
}

//================================================================


