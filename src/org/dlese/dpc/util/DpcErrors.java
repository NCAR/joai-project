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
package org.dlese.dpc.util;

import java.util.HashMap;



/**
 * Overall compendium of error codes for DLESE DPC software components.
 * Error ranges:
 * <ul>
 *    <li> 0 - 9999 (reserved)
 *    <li> 10,000 - 19,999  Registration system
 *    <li> 20,000 - 29,999  XML transformation system
 *    <li> 30,000 - 39,999  XML validation system
 *    <li> 40,000 - 49,999  Idmapper
 *    <li> 50,000 - 59,999  Indexer
 * </ul>
 */
public class DpcErrors {

/**
 * Internal hashmap: Integer msgID -> String description
 */
private static HashMap msgmap = new HashMap();

/**
 * Internal hashmap: String description -> Integer msgID
 */
private static HashMap idmap = new HashMap();



// ============ Reserved Errors.
//============= Prefix RSV.  0 - 9,999

// ============ Registration System Errors.
// ============ Prefix ???.  10,000 - 19,999

// ============ XML Transformation System Errors.
// ============ Prefix XMLTRANS.  20,000 - 29,999

// ============ XML Validation System Errors.
// ============ Prefix XMLVAL.  30,000 - 39,999

// ============ Idmapper Errors.
// ============ Prefix IDMAP.  40,000 - 49,999


public final static int IDMAP_OK
	= addmsg( 41000, "OK");

// IN ORDER OF DECREASING SEVERITY

public final static int IDMAP_UNKNOWN_PROTOCOL
	= addmsg( 41010, "Unknown protocol");

public final static int IDMAP_MISSING_FIELD
	= addmsg( 41020, "Missing field");

public final static int IDMAP_MULT_FIELD
	= addmsg( 41030, "Multiply defined field");

public final static int IDMAP_BAD_XML_FILE
	= addmsg( 41040, "Invalid XML file");
	
public final static int IDMAP_VITALITY
	= addmsg( 41045, "Vitality too low");

public final static int IDMAP_ID_SYNTAX
	= addmsg( 41050, "ID syntax");

public final static int IDMAP_DUP_ID
	= addmsg( 41060, "Duplicate ID"); 

public final static int IDMAP_URL_SYNTAX
	= addmsg( 41070, "URL syntax");

public final static int IDMAP_EMAIL_SYNTAX
	= addmsg( 41080, "Email syntax");
	
public final static int IDMAP_ACCESSION_DATE_MISSING
	=addmsg(41085, "Accession Date Missing"); 
	
public final static int IDMAP_PERM_REDIRECT
	= addmsg( 41090, "Permanent redirect");
	
public final static int IDMAP_MIRROR_DIFFERS
	= addmsg( 41100, "Mirror differs from primary");
	
public final static int IDMAP_DUP
	= addmsg( 41110, "Duplicate resource");
	
public final static int IDMAP_NOT_FOUND
	= addmsg( 41130, "Not found");


public final static int IDMAP_SERVER_ERROR
	= addmsg( 41140, "Server error");

public final static int IDMAP_NO_SERVICE
	= addmsg( 41150, "Service not available");

public final static int IDMAP_CONNECT_REFUSED
	= addmsg( 41160, "Connection refused");

public final static int IDMAP_FTP_MISC
	= addmsg( 41170, "FTP error");

public final static int IDMAP_FTP_LOGIN
	= addmsg( 41180, "FTP login failed or too busy");

public final static int IDMAP_REDIRECT_LIMIT
	= addmsg( 41190, "Too many redirects");

public final static int IDMAP_TIMEOUT
	= addmsg( 41200, "Time out");

public final static int IDMAP_UNKNOWN_HOST
	= addmsg( 41210, "Unknown host");

public final static int IDMAP_HTTP_HEADER
	= addmsg( 41220, "Bad HTTP header line");

public final static int IDMAP_HTTP_STATUSLINE
	= addmsg( 41230, "Bad HTTP status line");

public final static int IDMAP_HTTP_RESPONSE
	= addmsg( 41240, "Bad HTTP response");

public final static int IDMAP_AUTHORIZATION
	= addmsg( 41250, "Not authorized");

public final static int IDMAP_MISC
	= addmsg( 41260, "Misc error");



// =========== All IDMAP error codes below are less severe ===========
public final static int IDMAP_SEVERE_LIMIT = 42000;


public final static int IDMAP_CHANGE_STATUS
	= addmsg( 42010, "Changing status");

public final static int IDMAP_CHANGE_FILENAME
	= addmsg( 42020, "Changing file name");

public final static int IDMAP_CHANGE_MMD
	= addmsg( 42030, "Changing meta-metadata");

public final static int IDMAP_CHANGE_PRIMARY_URL
	= addmsg( 42040, "Changing primary URL");

public final static int IDMAP_SITE_CONTENT_CHANGED
	= addmsg( 42050, "Site content has changed");

public final static int IDMAP_NO_XML_FILE
	= addmsg( 42110, "XML file disappeared");

public final static int IDMAP_NEW_XML_FILE
	= addmsg( 42120, "New XML file found");

public final static int IDMAP_XML_FILE_NAME_CHANGED
	= addmsg( 42130, "XML file name changed");

public final static int IDMAP_XML_FILE_REAPPEARED
	= addmsg( 42140, "XML file reappeared");

public final static int IDMAP_NEWS_PAST_ARCHIVE_DATE
	=addmsg(42150, "Check date of news record is past the archive date");
	
public final static int IDMAP_NEWS_ARCHIVE_DATE_NOPARSE
	=addmsg(42160, "Archive Date could not be parsed - not in the yyyy-MM-dd format");


// ============ Indexer Errors.
// ============ Prefix ???.  50,000 - 59,999







/**
 * Returns the message associated with the specified id.
 * Sample usage: <br>
 * <code>
 *		String msg = DpcErrors.getMessage( DpcErrors.IDMAP_NODBREC);
 * </code>
 */

public static String getMessage( int id) {
	String msg = (String) msgmap.get( new Integer( id));
	return msg;
}







/**
 * Returns the id associated with the specified message.
 * Sample usage: <br>
 * <code>
 *		int type = DpcErrors.getType( "No DB rec");
 * </code>
 * @return The message type or, if not found, -1.
 */

public static int getType( String msg) {
	Integer intid = (Integer) idmap.get( msg);
	if (intid == null) return -1;
	else return intid.intValue();
}







/**
 * Adds the association (id, desc) to msgmap and returns id.
 */

private static int addmsg( int id, String desc) {
	Object oldval;
	Integer idint = new Integer( id);
	oldval = msgmap.put( idint, desc);
	if (oldval != null) {
		throw new Error("org.dlese.dpc.util.DpcErrors:\n"
			+ "    the id: " + id + " is multiply defined.\n"
			+ "    old def: \"" + oldval + "\"\n"
			+ "    new def: \"" + desc + "\"\n");
	}

	oldval = idmap.put( desc, idint);
	if (oldval != null) {
		throw new Error("org.dlese.dpc.util.DpcErrors:\n"
			+ "    the message: " + desc + " is multiply defined.\n"
			+ "    old def: \"" + oldval + "\"\n"
			+ "    new def: \"" + id + "\"\n");
	}

	return id;
}


/**
 * Test driver.
 */
public static void main( String[] args) {
	String msg = DpcErrors.getMessage( DpcErrors.IDMAP_NEW_XML_FILE);
	System.out.println( "msg: \"" + msg + "\"");
}

} // end class
