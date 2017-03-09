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


import java.util.Date;

import org.dlese.dpc.ldap.LdapClient;
import org.dlese.dpc.ldap.LdapEntry;
import org.dlese.dpc.ldap.LdapException;
import org.dlese.dpc.ldap.LdapNotFoundException;


/**
 * Test driver for LdapClient.
 */

public class LdapDemo {

/**
 * Initial invokation point for the driver;
 * Expects 3 arg in the args array:
 * <ul>
 *   <li> the name of the properties file.
 *   <li> the UID of the test administrator
 *   <li> the password for the administrator
 * </ul>
 */

public static void main( String[] args) {
	new LdapDemo( args);
}


/**
 * Print error and usages messages, and quit.
 */
void badparms( String msg) {
	prtln("\nLdapDemo: Error: " + msg);
	prtln("LdapDemo: parms: propertiesFileName  testAdminUID  adminPassword");
	System.exit(1);
}


/**
 * Driver constructor: just invokes testit().
 */
LdapDemo( String[] args) {
	try {
		testit( args);
	}
	catch( Exception exc) {
		prtln("LdapDemo: Exception: " + exc);
		exc.printStackTrace();
		System.exit(1);
	}
}


/**
 * Test driver: test add, search, remove, mail lists, etc.
 */

void testit( String[] args)
throws Exception
{
	LdapEntry entry;
	LdapEntry[] entries;
	boolean hitexc;

	if (args.length != 3) badparms("wrong num parms");
	String propsFile = args[0];
	String adminUid  = args[1];
	String adminPswd = args[2];


	// Set of attribute names to get
	String[] attrNames = new String[] {
		"DLESEloginName", "DLESEnameLast", "DLESEnameFirst"
	};

	// INFO FOR SALLY SMITH:
	String smithUid = "ssmith";		// Test UID
	String smithPswd = "abc";		// Test password

	// Set of attribute names and values used to create
	// a new LDAP entry.
	// In each row the first column is the attribute name,
	// and the remaining column(s) are the value(s) for
	// that attribute.
	//
	// For a list of required and optional attributes, see the
	// javadoc for LdapClient.addUserEntry.

	String[][] smithAttrStgs = new String[][] {
		// Required attributes:
		{ "DLESEou",				"people"},
		{ "DLESEloginName",		smithUid},			// unique UID
		{ "userPassword",	smithPswd},			// password
		{ "DLESEnameLast",				"Smith"},		// surname
		{ "DLESEnameFirst",		"Sally"},
		{ "DLESEemailPrimary",			"smith@ucar.edu"},	// email

		// Optional attributes:
		{ "DLESEaffiliation1", "UCAR"},	// affiliated institution
		{ "DLESEfocus",		"graduate students"},
		{ "DLESEprofResp",	"administrator"},
		{ "DLESEurl",		"http://www.someU.edu/~ssmith"}	// personal URL
	};


	// INFO FOR OSCAR OWENS:
	String owensUid = "oowens";		// Test UID
	String owensPswd = "def";		// Test password

	// Set of attribute names and values used to create
	// a new LDAP entry.
	// In each row the first column is the attribute name,
	// and the remaining column(s) are the value(s) for
	// that attribute.
	//
	// For a list of required and optional attributes, see the
	// javadoc for LdapClient.addUserEntry.

	String[][] owensAttrStgs = new String[][] {
		// Required attributes:
		{ "DLESEou",				"people"},
		{ "DLESEloginName",		owensUid},			// unique UID
		{ "userPassword",	owensPswd},			// password
		{ "DLESEnameLast",				"Owens"},		// surname
		{ "DLESEnameFirst",		"John"},
		{ "DLESEemailPrimary",			"owens@Cornell.edu"},	// email

		// Optional attributes:
		{ "DLESEaffiliation1", "Cornell"},	// affiliated institution
		{ "DLESEfocus",		"graduate students"},
		{ "DLESEprofResp",	"administrator"},
		{ "DLESEurl",		"http://www.someU.edu/~oowens"}	// personal URL
	};



	int ii;

	// Create a new LDAP client.
	LdapClient client = new LdapClient( propsFile);

	// Search for all entries in the database;
	// return all attributes for each entry.
	// Within each entry, the attributes are in alphabetic order.
	entries = client.search(
		null,				// base = null: start at top of DB tree
		"objectclass=*",	// filter
		null,				// attrNames = null: return all attributes
		0);					// maxres = 0 implies return all results
	prtentries("LdapDemo: search all, all attrs", entries);



	// Search for all entries in the database;
	// return only selected attributes for each entry.
	// Within each entry, the attributes are in the
	// order specified in attrNames.
	entries = client.search(
		null,				// base = null: start at top of DB tree
		"objectclass=*",	// filter
		attrNames,			// names of attributes to be returned
		0);					// maxres = 0 implies return all results
	prtentries("LdapDemo: search all, selected attrs", entries);



	// Add new user: Sally Smith
	client.addUserEntry(
		smithUid,			// the new UID
		smithAttrStgs);		// array of attributes

	// Add new user: Oscar Owens
	client.addUserEntry(
		owensUid,			// the new UID
		owensAttrStgs);		// array of attributes
	prtall("LdapDemo: after add users: smith", client,
		"DLESEnameLast=*smith*");
	prtall("LdapDemo: after add users: owens", client,
		"DLESEnameLast=*owens*");
	prtall("LdapDemo: after add users", client,
		"(| (DLESEnameLast=*smith*) (DLESEnameLast=*owens*))");

	prtln("LdapDemo: userAuthenticates for owens: "
		+ client.userAuthenticates( owensUid, owensPswd));
	prtln("LdapDemo: userAuthenticates for owens, bad userid: "
		+ client.userAuthenticates( owensUid + "x", owensPswd));
	prtln("LdapDemo: userAuthenticates for owens, bad pswd: "
		+ client.userAuthenticates( owensUid, owensPswd + "x"));

	// Get all attributes for a user.
	// Attributes are returned in alphabetic order.
	entry = client.getUserAttributes(
		smithUid,			// authentication UID
		smithPswd,			// password
		smithUid,			// UID we want attributes for
		null);				// attrNames = null: get ALL attributes
	prtln("\nLdapDemo: getUserAttributes for smith (all attrs): " + entry);
		
	// Get only selected attributes for a user.
	// Attributes are returned in the same order as specified
	// in attrNames.
	entry = client.getUserAttributes(
		smithUid,			// authentication UID
		smithPswd,			// password
		smithUid,			// UID we want attributes for
		attrNames);			// the attributes we want returned.
	prtln("\nLdapDemo: getUserAttributes for smith (select attrs): " + entry);

	// Get attributes for NON-existant user.
	// Should get exception.
	hitexc = false;
	try {
		entry = client.getUserAttributes(
			"noneSuchUid",		// UID we want attributes for
			attrNames);			// the attributes we want returned.
	}
	catch( LdapNotFoundException lex) {
		prtln("LdapDemo: Good: cannot getUserAttributes on nonexistant entry");
		hitexc = true;
	}
	if (! hitexc) throw new Exception("could getattr on nonexistant entry");


	// Search for DLESEnameLast="*smith*";
	// return all attributes of each entry found.
	entries = client.search(
		smithUid,			// authentication UID
		smithPswd,			// password
		null,				// base = null: start at top of DB tree
		"DLESEnameLast=*smith*",		// filter
		null,				// attrNames
		0);					// maxres = 0 implies return all results
	prtentries("LdapDemo: search DLESEnameLast=*smith*, all attrs", entries);

	// Search for DLESEnameLast="*smith*";
	// return only selected attributes of each entry found.
	entries = client.search(
		smithUid,			// authentication UID
		smithPswd,			// password
		null,				// base = null: start at top of DB tree
		"DLESEnameLast=*smith*",		// filter
		attrNames,
		0);					// maxres = 0 implies return all results
	prtentries("LdapDemo: search DLESEnameLast=*smith*, selected attrs",
		entries);



	// Add DLESEfocus
	// If DLESEfocus doesn't exist, it will be created.
	// If DLESEfocus does and is not SINGLE-VALUE,
	// the value will be added.
	client.addUserAttributeValue(
		smithUid,			// authentication UID
		smithPswd,			// password
		smithUid,			// UID to change
		"DLESEfocus",				// attribute to change
		"hydrosphere");	// new value to add
	prtall("LdapDemo: after add DLESEfocus", client, "DLESEnameLast=*smith*");


	// Add another value for DLESEfocus
	client.addUserAttributeValue(
		smithUid,			// authentication UID
		smithPswd,			// password
		smithUid,			// UID to change
		"DLESEfocus",				// attribute to change
		"atmosphere");	// new value to add
	prtall("LdapDemo: after add another DLESEfocus", client,
		"DLESEnameLast=*smith*");



	// Change DLESEfocus value
	// Any previous values of DLESEfocus are removed.
	client.setUserAttribute(
		smithUid,			// authentication UID
		smithPswd,			// password
		smithUid,			// UID to change
		"DLESEfocus",				// attribute to change
		new String[] { "biosphere", "solid earth"}		// new value of attr
	);
	prtall("LdapDemo: after change DLESEfocus", client,
		"DLESEnameLast=*smith*");

	// Remove a single DLESEfocus value
	client.removeUserAttributeValue(
		smithUid,			// authentication UID
		smithPswd,			// password
		smithUid,			// UID to change
		"DLESEfocus",
		"biosphere");
	prtall("LdapDemo: after remove DLESEfocus value", client,
		"DLESEnameLast=*smith*");

	// Remove value again: should throw an exception,
	// since it's not there.
	hitexc = false;
	try {
		client.removeUserAttributeValue(
			smithUid,			// authentication UID
			smithPswd,			// password
			smithUid,			// UID to change
			"DLESEfocus",
			"biosphere");
	}
	catch( LdapNotFoundException lex) {
		prtln("LdapDemo: Good: cannot remove attr value again");
		hitexc = true;
	}
	if (! hitexc) throw new Exception("remove value again should have failed");


	// Remove a entire DLESEfocus attr by specifying value=null
	client.removeUserAttributeValue(
		smithUid,			// authentication UID
		smithPswd,			// password
		smithUid,			// UID to change
		"DLESEfocus",
		null);
	prtall("LdapDemo: after remove entire DLESEfocus", client,
		"DLESEnameLast=*smith*");





	// Remove an entire attribute and all values
	client.removeUserAttributeValue(
		smithUid,			// authentication UID
		smithPswd,			// password
		smithUid,			// UID to change
		"DLESEurl",			// attribute to remove
		null);				// value == null implies remove all values
	prtall("LdapDemo: after remove entire DLESEurl", client,
		"DLESEnameLast=*smith*");

	// try remove again: it should fail
	hitexc = false;
	try {
		client.removeUserAttributeValue(
			smithUid,			// authentication UID
			smithPswd,			// password
			smithUid,			// UID to change
			"DLESEurl",			// attribute to remove
			null);				// value == null implies remove all values
	}
	catch( LdapNotFoundException lex) {
		prtln("LdapDemo: Good: cannot remove attr again");
		hitexc = true;
	}
	if (! hitexc) throw new Exception("remove attr again should have failed");

	// Add back DLESEurl
	client.addUserAttributeValue(
		smithUid,			// authentication UID
		smithPswd,			// password
		smithUid,			// UID to change
		"DLESEurl",			// attribute to add value to
		"http://www.someU.edu/~jsmith");	// new value
	prtall("LdapDemo: after add back DLESEurl", client,
		"DLESEnameLast=*smith*");

	// Set DLESEurl attribute to null: <b> Not recommended.</b>
	// This is the same as
	// {@linke #removeUserAttributeValue removeUserAttributeValue}.
	// Should get exception ...
	try {
		client.setUserAttribute(
			smithUid,			// authentication UID
			smithPswd,			// password
			smithUid,			// UID to change
			"DLESEurl",			// attribute to remove
			null);				// set it to null: remove it.
		prtall("LdapDemo: after set DLESEurl to null", client,
			"DLESEnameLast=*smith*");
	}
	catch( LdapException lex) {
		prtln("LdapDemo: Good: cannot set attr values to null");
	}



	// Store a Java Object
	// The Object to store is testStg:
	String testStg = "============== testStg ============";

	client.storeUserObject(
		smithUid,			// authentication UID
		smithPswd,			// password
		smithUid,			// UID to store it under
		"testObjectA",		// name to give the object.
		testStg);			// object
	prtall("LdapDemo: after store object", client, "DLESEobjectName=*");

	// Retrieve a Java Object using owner's uid
	Object getObj = client.getUserObject(
		smithUid,			// authentication UID
		smithPswd,			// password
		smithUid,			// UID where the object is stored
		"testObjectA");		// object name
	prtln("\nLdapDemo: getObj after store object, using smithUid: ("
		+ getObj.getClass().getName()
		+ "): \"" + getObj + "\"");

	// Retrieve a Java Object using admin uid
	getObj = client.getUserObject(
		adminUid,			// authentication UID
		adminPswd,			// password
		smithUid,			// UID where the object is stored
		"testObjectA");		// object name
	prtln("\nLdapDemo: getObj after store object, using Admin: ("
		+ getObj.getClass().getName()
		+ "): \"" + getObj + "\"");

	// Retrieve attributes associated with stored Java object
	// Deprecated, since we don't store user attributes.
	// Maybe someday we will.
	entry = client.getUserObjectAttributes(
		smithUid,			// authentication UID
		smithPswd,			// password
		smithUid,			// UID where the object is stored
		"testObjectA",		// object name
		null);				// attrNames = null, implies retrieve ALL attrs
	prtln("\nLdapDemo: object attrs: " + entry);

	// Store null object
	// CAUTION: this does not remove the entry.
	// It leaves a JNDI Context in the database, and on the
	// next getUserObject call the Context will be returned
	// instead of a user object.
	// NOT RECOMMENDED!
	client.storeUserObject(
		smithUid,			// authentication UID
		smithPswd,			// password
		smithUid,			// UID to store it under
		"testObjectA",		// name to give the object.
		null);				// object
	prtall("LdapDemo: after store null object", client, "DLESEobjectName=*");
	getObj = client.getUserObject(
		smithUid,			// authentication UID
		smithPswd,			// password
		smithUid,			// UID where the object is stored
		"testObjectA");		// object name
	prtln("\nLdapDemo: getObj after store null: ("
		+ getObj.getClass().getName() + "): \"" + getObj + "\"");

	// Remove the object
	client.removeUserObject(
		smithUid,			// authentication UID
		smithPswd,			// password
		smithUid,			// UID where the object is stored
		"testObjectA");		// object name
	prtall("LdapDemo: after remove object", client, "DLESEobjectName=*");

	// Rename a user entry: rename "jsmith" to "ysmith".
	// CAUTION: This cannot be done if any user objects are
	// stored for this user.
	// NOT RECOMMENDED!
	client.renameUserEntry(
		smithUid,		// old UID
		"ysmith");		// new UID
	prtall("LdapDemo: after rename ssmith to ysmith", client,
		"DLESEnameLast=*smith*");

	// Rename it back
	client.renameUserEntry(
		"ysmith",		// old UID
		smithUid);		// new UID
	prtall("LdapDemo: after rename ysmith back to ssmith", client,
		"DLESEnameLast=*smith*");



	// Get create, modify dates
	Date dt = client.getUtcCreateTimestamp( smithUid);
	prtln("LdapDemo: after get create UTC timestamp: " + dt);
	dt = client.getUtcModifyTimestamp( smithUid);
	prtln("LdapDemo: after get modify UTC timestamp: " + dt);




	// Test open mail lists
	testList(
		"lists/open/testa/testb/geology",		// list name
		client,				// the LdapClient to use for testing
		owensUid,			// UID of list owner
		owensPswd,			// password
		smithUid,			// the UID to use for testing
		smithPswd,			// password for smithUid
		adminUid,			// admin UID
		adminPswd,			// password
		attrNames);

	// Test closed mail lists
	testList(
		"lists/closed/secretList",	// list name
		client,				// the LdapClient to use for testing
		owensUid,			// UID of list owner
		owensPswd,			// password
		smithUid,			// the UID to use for testing
		smithPswd,			// password for smithUid
		adminUid,			// admin UID
		adminPswd,			// password
		attrNames);

	// Test the demo from LdapClient doc.
	testDemoClient( propsFile);

	// Check if smithUid exists
	boolean existsFlag;
	existsFlag = client.userExists(
		smithUid);			// UID to test
	prtln("LdapDemo: userExists for smith: " + existsFlag);

	// Remove both UIDs that we added.  Uses admin authorization.
	// No error message if it's not there to begin with.
	client.removeUserEntry( smithUid);
	client.removeUserEntry( owensUid);

	// Try to remove it again.  Should fail.
	hitexc = false;
	try {
		client.removeUserEntry( owensUid);
	}
	catch( LdapNotFoundException lex) {
		prtln("LdapDemo: Good: cannot remove entry again");
		hitexc = true;
	}
	if (! hitexc) throw new Exception("remove entry again should have failed");

	// Check if smithUid exists
	existsFlag = client.userExists(
		smithUid);			// UID to test
	prtln("LdapDemo: userExists after remove for smith: " + existsFlag);

} // end testit




//================================================================


/**
 * Tests mail lists.
 */
void testList(
	String listName,		// the list name
	LdapClient client,		// the client to use
	String owensUid,		// UID for list owner
	String owensPswd,		// password
	String smithUid,		// UID for list member
	String smithPswd,		// password
	String adminUid,		// admin UID
	String adminPswd,		// password
	String[] attrNames)
throws LdapException
{
	int ii;

	// Create list.  Uses admin authentication.
	// Automatically adds the owner to the list.
	client.createList(
		listName,		// name of the list
		owensUid);		// UID of list owner
	prtall("LdapDemo: after create list " + listName, client,
		"dleseListName=*");

	try {
		// Add name to list, using self for authorization
		// This will succeed for open lists, 
		// and will fail for closed lists.
		client.addListName(
			smithUid,			// authentication UID
			smithPswd,			// password
			listName,			// name of the list
			smithUid);			// UID to add to the list
		prtall("LdapDemo: after self add name to list " + listName,
			client, "dleseListName=*");

		// Remove name from list, using self for authorization
		client.removeListName(
			smithUid,			// authentication UID
			smithPswd,			// password
			listName,			// name of the list
			smithUid);			// UID to remove from the list
		prtall("LdapDemo: after self remove name from list " + listName,
			client, "dleseListName=*");
	}
	catch( Exception exc) {
		prtln("\nLdapDemo: testList: after self add/removed: failed. exc: "
			+ exc + "\n");
	}

	// Again add name to list, using the list owner for authorization
	client.addListName(
		owensUid,			// authentication UID
		owensPswd,			// password
		listName,			// name of the list
		smithUid);			// UID to add to the list
	prtall("LdapDemo: after owner add name to list " + listName,
		client, "dleseListName=*");

	// Get all members of the list, as a user
	String[] uids;
	uids = client.getListNames(
		smithUid,			// authentication UID
		smithPswd,			// password
		listName);			// name of the list
	prtln("LdapDemo: display list uids, as user auth: entries found: "
		+ uids.length);
	for (ii = 0; ii < uids.length; ii++) {
		prtln("    uid: \"" + uids[ii] + "\"");
	}

	// Get all members of the list, as the list owner
	uids = client.getListNames(
		owensUid,			// authentication UID
		owensPswd,			// password
		listName);			// name of the list
	prtln("LdapDemo: display list uids, as owner auth: entries found: "
		+ uids.length);
	for (ii = 0; ii < uids.length; ii++) {
		prtln("    uid: \"" + uids[ii] + "\"");
	}

	// Get all members of the list, as the admin
	uids = client.getListNames(
		adminUid,			// authentication UID
		adminPswd,			// password
		listName);			// name of the list
	prtln("LdapDemo: display list uids, as admin auth: entries found: "
		+ uids.length);
	for (ii = 0; ii < uids.length; ii++) {
		prtln("    uid: \"" + uids[ii] + "\"");
	}


	// Get the specified attributes for each member of a list.
	LdapEntry[] listattrs = client.getListAttributes( listName, attrNames);
	prtln("\nLdapDemo: getListAttributes for list: " + listName);
	for (ii = 0; ii < listattrs.length; ii++) {
		prtln("  member: " + listattrs[ii]);
	}


	// Remove name from list, using owner for authorization
	client.removeListName(
		owensUid,			// authentication UID
		owensPswd,			// password
		listName,			// name of the list
		smithUid);			// UID to remove from list
	prtall("LdapDemo: after owner remove name from list " + listName,
		client, "dleseListName=*");

	// Remove entire list
	client.removeEntireList(
		listName);			// name of the list
	prtall("LdapDemo: after remove entire list " + listName,
		client, "dleseListName=*");
}


//================================================================

/**
 * Tests the demo code in the javadoc for LdapClient.
 */

void testDemoClient(
	String propsFile)		// Name of properties file
throws LdapException
{
	LdapClient democlient = new LdapClient( propsFile);
	try { democlient.isAlive(); }
	catch( LdapException lex) {
		System.out.println(
			"\nDemoClient: isAlive says there's trouble: " + lex);
		throw lex;
	}

	LdapEntry[] entries = democlient.search(
		null,				// base = null: start at top of DB tree
		"objectclass=*",	// filter: find all entries
		null,				// attrNames = null: return all attributes
		0);					// maxres = 0 implies return all results
	if (entries == null) System.out.println("\nDemoClient: No entries found");
	else {
		System.out.println("\nDemoClient: num entries found: "
			+ entries.length);

		// For each returned entry:
		for (int ii = 0; ii < entries.length; ii++) {
			System.out.println("\nEntry " + ii + " dn: "
				+ entries[ii].getDn());

			// For each attribute of the entry:
			for (int jj = 0; jj < entries[ii].getAttrsRows(); jj++) {
				System.out.print("  Attribute: "
					+ entries[ii].getAttrName( jj) + "  Values:");
				String[] valueStrings = entries[ii].getAttrStrings(jj);

				// For each value of the attribute:
				for (int kk = 0; kk < valueStrings.length; kk++) {
					System.out.print("  \""
						+ valueStrings[kk] + "\"");
				} // end for kk
				System.out.println();
			} // end for jj
		} // end for ii
		System.out.println();
	} // if entries != null
} // end testDemoClient

//================================================================


/**
 * Prints all entries matching the specified filter.
 * 
 * @param msg		Title message
 * @param client	The LdapClient
 * @param filter	The filter expression to use.  To print <b>all</b>
 *		entries in the database, use:
 *		<code>filter = "objectclass=*";</code>
 */

void prtall(
	String msg,
	LdapClient client,
	String filter)
throws LdapException
{
	if (filter == null) filter = "objectclass=*";
	LdapEntry[] entries = client.search(
		null,				// base = null: start at top of DB tree
		filter,
		null,				// attrNames = null: return all attributes
		0);					// maxres = 0 implies return all results
	prtentries( msg, entries);
}

//================================================================


/**
 * Prints the specified entries.
 *
 * @param msg		Title message
 * @param entries	The entries to be printed.
 */
void prtentries(
	String msg,
	LdapEntry[] entries)
{
	prtln("\n" + msg + ":  num entries: " + entries.length);
	for (int ii = 0; ii < entries.length; ii++) {
		prtln("    " + entries[ii]);
	}
	prtln("");
	prtln("");
}

//================================================================

/**
 * Prints a single line, with ending "\n".
 */

private static void prtln( String msg) {
	int bugs = 1;
	if (bugs >= 1) System.out.println( msg);
}


} // end class LdapDemo


