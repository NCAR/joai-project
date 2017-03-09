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
package org.dlese.dpc.services.idmapper;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import java.sql.Timestamp;
import java.util.Date;
import java.util.LinkedList;

import org.dlese.dpc.services.mmd.MmdException;
import org.dlese.dpc.services.mmd.MmdRec;
import org.dlese.dpc.services.mmd.Query;
import org.dlese.dpc.services.mmd.MmdWarning;
import org.dlese.dpc.util.DpcErrors;



/**
 * Test driver for org.dlese.dpc.services.mmd.Query.<br>
 *  Command line parameters are:
 * <ul>
 *   <li> dbUrl: The URL of the MySQL database.  Example:
 *        <code> jdbc:mysql://someHost:3306/DLESE_Systems?user=someUser&password=someSecret </code>
 *   <li> collKey: The collection key.  Example: <code> dcc </code>.
 *   <li> idfile: The name of the file containing IDs to be tested.
 *        The IDs are one per line.  Example idfile:<br>
 *          <code>
 *            DLESE-000-000-003-001<br>
 *            DLESE-000-000-003-289<br>
 *            DLESE-000-000-003-290<br>
 *          </code>
 * <ul>
 * Example of the full command line:<br>
 * <code> java org.dlese.dpc.services.idmapper.TestQuery 'jdbc:mysql://someHost:3306/DLESE_Systems?user=someUser&amp;password=someSecret' dcc testids.txt");
 */


public class TestQuery {


    public static void main( String[] args) {
	try {
	    new TestQuery( args);
	}
	catch( Exception iex) {
	    prtln("TestQuery: caught: " + iex);
	    iex.printStackTrace();
	}
    }



    void badparms( String msg) {
	prtln("Error: " + msg);
	prtln("Parms: dbUrl collKey idfile");
	prtln("Example:");
	prtln("  java org.dlese.dpc.services.idmapper.TestQuery 'jdbc:mysql://someHost:3306/DLESE_Systems?user=someUser&password=someSecret' dcc testids");
	prtln("Where the file testids contains the IDs to be tested:");
	prtln("DLESE-000-000-003-001");
	prtln("DLESE-000-000-003-289");
	prtln("DLESE-000-000-003-290");
	prtln("DLESE-000-000-003-291");
	System.exit(1);
    }





    TestQuery( String[] args)
	throws MmdException, IOException, FileNotFoundException
    {
	int itest, ii;
	String testid;

	if (args.length != 3) badparms("wrong num args");
	int iarg = 0;// arguments:
	String dbUrl     = args[iarg++];// DB URL
	String collKey   = args[iarg++];// collection key
	String idfile    = args[iarg++];// file of ids


	LinkedList testidlist = new LinkedList();
	BufferedReader rdr = new BufferedReader( new FileReader( idfile));
	while (true) {
	    testid = rdr.readLine();
	    if (testid == null) break;
	    testid = testid.trim();
	    if (testid.length() > 0 && ! testid.startsWith("#")) {
		testidlist.add( testid);
	    }
	}
	rdr.close();
	String[] testids = (String[]) testidlist.toArray( new String[0]);

	Query query = new Query(// create the DB connection
				0,// debug level
				dbUrl);

	prtln("\n\n========== TEST 0: GET COLLECTION DIRECTORY =========\n\n");
	String dirpath = query.getDirectory( collKey);
	prtln("dirpath: \"" + dirpath + "\"");


	///prtln("\n\n========== TEST 1A: OBSOLETE PRINT MMD RECORDS ======\n\n");
	///for (itest = 0; itest < testids.length; itest++) {
	///testid = testids[itest];
	///prtln("collKey: " + collKey + "  id: " + testid);
	///MmdRecord record = null;
	///try {
	///record = query.getMmdRecord( collKey, testid);
	///}
	///catch( Exception exc) {
	///prtln("caught exc for id: " + testid + "  exc: " + exc);
	///exc.printStackTrace();
	///}
	///if (record == null) prtln("    (record not found)");
	///else {
	///prtln("    collKey: \"" + record.getCollKey() + "\"");
	///prtln("    id: \"" + record.getId() + "\"");
	///prtln("    fileName: \"" + record.getFileName() + "\"");
	///prtln("    id: \"" + record.getId() + "\"");
	///prtln("    status: \"" + record.getStatus() + "\"");
	///prtln("    metastyle: \"" + record.getMetastyle() + "\"");
	///prtln("    firstAccessionDate: "
	///+ new Date( record.getFirstAccessionDate()));
	///prtln("    lastMetaModDate:    "
	///+ new Date( record.getLastMetaModDate()));
	///prtln("    recCheckDate:       "
	///+ new Date( record.getRecCheckDate()));

	///ErrorDesc[] errs = record.getErrors();
	///int numerrors = 0;
	///if (errs != null) numerrors = errs.length;
	///prtln("    num errors: " + numerrors);
	///prtln("");
	///}
	///}


	prtln("\n\n========== TEST 1B: CURRENT PRINT MMD REC ======\n\n");
	for (itest = 0; itest < testids.length; itest++) {
	    testid = testids[itest];
	    prtln("collKey: " + collKey + "  id: " + testid);
	    MmdRec rec = null;
	    try {
		rec = query.getMmdRec( collKey, testid);
	    }
	    catch( Exception exc) {
		prtln("caught exc for id: " + testid + "  exc: " + exc);
		exc.printStackTrace();
	    }
	    if (rec == null) prtln("    (rec not found)");
	    else {
		prtln("    collKey: \"" + rec.getCollKey() + "\"");
		prtln("    id: \"" + rec.getId() + "\"");
		prtln("    fileName: \"" + rec.getFileName() + "\"");
		prtln("    id: \"" + rec.getId() + "\"");
		prtln("    status: " + rec.getStatus() + "  \""
		      + rec.getStatusString() + "\"");
		prtln("    metastyle: " + rec.getMetastyle() + "  \""
		      + rec.getMetastyleString() + "\"");
		prtln("    firstAccessionDate: "
		      + new Date( rec.getFirstAccessionDate()));
		prtln("    lastMetaModDate:    "
		      + new Date( rec.getLastMetaModDate()));
		prtln("    recCheckDate:       "
		      + new Date( rec.getRecCheckDate()));

		prtln("    primary Content :     " + rec.getPrimaryContent());



		MmdWarning[] warnings = rec.getWarnings();
		printWarnings( warnings);
	    }
	}


	///prtln("\n\n========== TEST 2A: OBSOLETE PRINT DUPLICATES ======\n\n");
	///// For each ID in the test id file, find and print
	///// all duplicate record from OTHER collections.
	///// That is, they have the same primaryUrl checksumduplicates.
	/////
	///// Note that this ONLY finds duplicates in OTHER collections,
	///// per the API spec.  Duplicates in the same collection
	///// are not reported here.

	///for (itest = 0; itest < testids.length; itest++) {
	///testid = testids[itest];
	///MmdRecord[] duplicates = null;
	///try {
	///duplicates = query.findDuplicates( collKey, testid);
	///}
	///catch( Exception exc) {
	///prtln("caught exc for id: " + testid + "  exc: " + exc);
	///exc.printStackTrace();
	///}

	///if (duplicates == null || duplicates.length == 0) {
	///prtln("========== no duplicates for: \"" + collKey
	///+ "\"  id: \"" + testid + "\"");
	///}
	///else {
	///prtln("========== found duplicates for: \"" + collKey
	///+ "\"  id \"" + testid + "\": " + duplicates.length);
	///for (ii = 0; ii < duplicates.length; ii++) {
	///prtln(" ===== dup: " + duplicates[ii]);
	///}
	///}
	///}


	prtln("\n\n========== TEST 2B: CURRENT PRINT DUPLICATES ======\n\n");
	// For each ID in the test id file, find and print
	// all duplicate record from OTHER collections.
	// That is, they have the same primaryUrl checksumduplicates.
	//
	// Note that this ONLY finds duplicates in OTHER collections,
	// per the API spec.  Duplicates in the same collection
	// are not reported here.

	prtln("Coll key " + collKey); 
	for (itest = 0; itest < testids.length; itest++) {
	    testid = testids[itest];
	    // Find duplicates.
	    // Returns an array of tuples.
	    // Each tuple is: [collectionName, id, filepath]
	    MmdRec[] dups = null;
	    try {
		dups = query.findDups( query.QUERY_BOTH, collKey, testid, "http://www.dlese.org/Metadata/documents/xml/nondups.xml");
	    }
	    catch( Exception exc) {
		prtln("caught exc for id: " + testid + "  exc: " + exc);
		exc.printStackTrace();
	    }

	    if (dups == null || dups.length == 0) {
		prtln("========== no dups for: \"" + collKey
		      + "\"  id: \"" + testid + "\"");
	    }
	    else {
		prtln("========== found dups for: \"" + collKey
		      + "\"  id \"" + testid + "\": " + dups.length);
		for (ii = 0; ii < dups.length; ii++) {
		    prtln(" ===== dup: " + dups[ii]);
		}
	    }
	}


	query.closeDb();// Close DB connection; release resources
    }







    void printWarnings( MmdWarning[] warnings) {
	int ii;
	int numwarnings = 0;
	if (warnings != null) numwarnings = warnings.length;
	prtln("    numwarnings: " + numwarnings);
	for (ii = 0; ii < numwarnings; ii++) {
	    MmdWarning warn = warnings[ii];
	    prtln("    warning " + ii + ":\n"
		  + "      msgType: " + warn.getMsgType() + "\""
		  + DpcErrors.getMessage( warn.getMsgType()) + "\"\n"
		  + "      filename: \"" + warn.getFilename() + "\n"
		  + "      xpath: \"" + warn.getXpath() + "\n"
		  + "      urllabel: \"" + warn.getUrllabel() + "\n"
		  + "      url: \"" + warn.getUrl() + "\n"
		  + "      msg: \"" + warn.getMsg() + "\n"
		  + "      auxinfo: \"" + warn.getAuxinfo() + "\n");
	}
    }








    /**
     * Prints a single line.
     */

    static void prtln( String msg) {
	System.out.println( msg);
    }

} // end class TestQuery

