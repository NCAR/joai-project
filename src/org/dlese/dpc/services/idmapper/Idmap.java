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
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;

import java.sql.Timestamp;

import java.text.ParseException;
import java.text.SimpleDateFormat;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import java.util.zip.CRC32;

import javax.mail.Message;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.Session;
import javax.mail.Transport;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.dlese.dpc.util.DpcErrors;
import org.dlese.dpc.services.mmd.DbConn;
import org.dlese.dpc.services.mmd.MmdException;
import org.dlese.dpc.services.mmd.MmdRec;
import org.dlese.dpc.xml.XMLDoc;
import org.dlese.dpc.xml.XMLException;
import org.dlese.dpc.services.mmd.CatchDup;

/**
 *  Id mapper main interface. See {@link #badparms badparms} for usage doc.
 *
 * @author     Sonal Bhushan
 * @created    January 31, 2006
 */

public class Idmap {

	final static int EMAIL_NORMAL = 1;
	final static int EMAIL_FORCE = 2;
	final static int EMAIL_PRINT = 3;
	final static int EMAIL_SINGLE = 4;

	String dbUrl = null;
	String printableUrl = null;
	DbConn dbconn = null;
	boolean forceEmail = false;
	// special force email to go out
	XMLDoc propsdoc;
	int bugs = 0;



	/**
	 *  The main program for the Idmap class
	 *
	 * @param  args  The command line arguments are as follows:
	 * 
	 *      props=[properties file name]     # Required
	 *      dir=[input directory]            # Required
	 *      coll=[metadata format]/[collection key]   # Optional, may be repeated
	 *      email=normal | force | print | [email address]
	 * 
	 *  Testing/debug parms ... 
	 *      bugs=[debugLevel]    # Set debug level.  default is 0.
	 *      checknet=[y/n]       # Check network before beginning.  Default is y
	 *      maxthreads=[num]     # Force maxthreads. Default is 0: use the props file number
	 *  
	 *  Example: to check links on all active collections:
	 *      java org.dlese.dpc.services.idmapper.Idmap
	 *          props=somePropsFile dir=/export/dlese/records
	 *  Example: to test two individual collections (adl and dcc),
	 *  both using adn format, and print the output:
	 *      java org.dlese.dpc.services.idmapper.Idmap
	 *          props=somePropsFile
	 *          dir=/export/dlese/records
	 *          coll=adn/adl  coll=adn/dcc email=print
	 */
	public static void main(String[] args) {

		new Idmap(args);
	}


	/**
	 *  Prints out the correct command line parameters. 
	 *
	 * @param  msg  the command line parameters required to run Idmapper from the command line
	 */
	private void badparms(String msg) {
		prtln("\nError: " + msg);
		prtln("Parms:");
		prtln("    props=[properties file name]     # Required");
		prtln("    dir=[input directory]            # Required");
		prtln("    coll=[metadata format]/[collection key]   # Optional, may be repeated");
		prtln("    email=normal | force | print | [email address]");
		prtln("");
		prtln("Testing/debug parms ... caution!");
		prtln("    bugs=[debugLevel]    # Set debug level.  default is 0.");
		prtln("    checknet=[y/n]       # Check network before beginning.  Default is y");
		prtln("    maxthreads=[num]     # Force maxthreads. Default is 0: use the props file number");
		prtln("");
		prtln("Example: to check links on all active collections:");
		prtln("    java org.dlese.dpc.services.idmapper.Idmap");
		prtln("        props=somePropsFile dir=/export/dlese/records");
		prtln("");
		prtln("Example: to test two individual collections (adl and dcc),");
		prtln("both using adn format, and print the output:");
		prtln("    java org.dlese.dpc.services.idmapper.Idmap");
		prtln("        props=somePropsFile");
		prtln("        dir=/export/dlese/records");
		prtln("        coll=adn/adl  coll=adn/dcc email=print");
		System.exit(1);
	}



	/**
	 *Constructor for the Idmap object
	 *
	 * 
	 */
	private Idmap(String[] args) {
		int ii;

		// We get the date at the start and use it throughout,
		// so that all dates of the run are exactly the same.
		long curdate = System.currentTimeMillis();
		// only one current date!
		long starttime = curdate;
		boolean hitError = false;

		CatchDup.reloadIdExclusionDocument();

		try {

			// Acquire command line parms.
			int iarg = 0;
			String bugsParm = null;
			String forceMaxThreadsParm = null;
			String checkNetFlagParm = null;
			String propsParm = null;
			String basedirParm = null;
			String[] collParms = null;
			String emailParm = null;

			for (iarg = 0; iarg < args.length; iarg++) {
				String arg = args[iarg];
				if (arg.startsWith("bugs=")) {
					bugsParm = arg.substring(5);
				} else if (arg.startsWith("maxthreads=")) {
					forceMaxThreadsParm = arg.substring(11);
				} else if (arg.startsWith("checknet=")) {
					checkNetFlagParm = arg.substring(9);
				} else if (arg.startsWith("props=")) {
					propsParm = arg.substring(6);
				} else if (arg.startsWith("dir=")) {
					basedirParm = arg.substring(4);
				} else if (arg.startsWith("coll=")) {
					String collarg = arg.substring(5);
					if (collParms == null) {
						collParms = new String[]{collarg};
					} else {
						String[] tmpParms = new String[collParms.length + 1];
						System.arraycopy(collParms, 0, tmpParms, 0,
								collParms.length);
						tmpParms[collParms.length] = collarg;
						collParms = tmpParms;
					}
				} else if (arg.startsWith("email=")) {
					emailParm = arg.substring(6);
				} else {
					badparms("Unknown parm: \"" + arg + "\"");
				}
			}

			if (bugsParm != null) {
				try {
					bugs = Integer.parseInt(bugsParm);
				} catch (NumberFormatException nfe) {
					badparms("Invalid \"bugs\" spec: \"" + bugsParm + "\"");
				}
			}
			if (bugs < 0 || bugs > 1000) {
				badparms("Property \"bugs\" is < 0 or > 1000");
			}
			if (bugs >= 1) {
				prtln("bugs: " + bugs);
				prtln("forceMaxThreadsParm: \"" + forceMaxThreadsParm + "\"");
				prtln("checkNetFlagParm: \"" + checkNetFlagParm + "\"");
				prtln("propsParm: \"" + propsParm + "\"");
				prtln("basedirParm: \"" + basedirParm + "\"");
				if (collParms == null) {
					prtln("collParms: null");
				} else {
					for (ii = 0; ii < collParms.length; ii++) {
						prtln("collParms[" + ii + "]: \"" + collParms[ii] + "\"");
					}
				}
				prtln("emailParm: \"" + emailParm + "\"");
			}

			// forceMaxThreads: should we override the maxthreads
			// parm in the properties file?
			// Default: 0  (don't override)
			int forceMaxThreads = 0;
			if (forceMaxThreadsParm != null) {
				try {
					forceMaxThreads = Integer.parseInt(forceMaxThreadsParm);
				} catch (NumberFormatException nfe) {
					badparms("Invalid \"maxthreads\" spec: \""
							 + forceMaxThreadsParm + "\"");
				}
			}

			// checkNetFlag: Should we check the network before starting?
			// Default: y
			boolean checkNetFlag = true;
			if (checkNetFlagParm != null) {
				String tmpstg = checkNetFlagParm.toLowerCase();
				if (tmpstg.equals("n") || tmpstg.equals("no")) {
					checkNetFlag = false;
				} else if (tmpstg.equals("y") || tmpstg.equals("yes")) {
					checkNetFlag = true;
				} else {
					badparms("Invalid \"checknet\" spec: \""
							 + checkNetFlagParm + "\"");
				}
			}

			// basedir: the base directory containing all
			// subdirectories with resource XML files
			if (basedirParm == null) {
				badparms("dir not specified");
			}
			if (bugs >= 1) {
				prtln("Idmap: basedirParm: \"" + basedirParm + "\"");
			}
			if (!basedirParm.startsWith("/")) {
				badparms("dir must start with \"/\"");
			}
			if (basedirParm.endsWith("/")) {
				badparms("dir must not end with \"/\"");
			}

			// emailtype: what to do with the reports.
			int emailType = EMAIL_NORMAL;
			if (emailParm == null) {
				emailType = EMAIL_NORMAL;
			} else if (emailParm.equals("normal")) {
				emailType = EMAIL_NORMAL;
			} else if (emailParm.equals("force")) {
				emailType = EMAIL_FORCE;
			} else if (emailParm.equals("print")) {
				emailType = EMAIL_PRINT;
			} else {
				try {
					checkEmailAddress(emailParm);
				} catch (IdmapException iex) {
					badparms("Invalid emailType: \"" + emailParm + "\"");
				}
				emailType = EMAIL_SINGLE;
			}

			// Read the properties file
			if (propsParm == null) {
				badparms("props not specified");
			}
			String propsfile = propsParm;

			if (propsfile.indexOf("://") < 0) {
				if (propsfile.startsWith("/")) {
					propsfile = "file://" + propsfile;
				} else {
					propsfile = "file://" + System.getProperty("user.dir")
							 + "/" + propsfile;
				}
			}
			propsdoc = new XMLDoc(
					propsfile,
					false,
			// validating
					false,
			// namespaceAware
					false);
			// expandEntities

			openDB(propsdoc);
			Object[][] dbmat = null;

			// collParms is either null, meaning process the
			// active collections in the idmapCollection table,
			// or collParms is an array of collections to process,
			// meaning we ignore the idmapCollection table.
			//
			// In either case, create dbmat[collnum][i]  where
			//    dbmat[collnum][0] == collection key, like "dwel" or "comet"
			//    dbmat[collnum][1] == meta data style, like "adn" or "dlese_anno"
			//    dbmat[collnum][2] == collkey/metastyle, like "dwel/adn"

			if (collParms == null) {
				// Read collection table: use only collActive ones.
				dbmat = dbconn.getDbTable(
						"SELECT collKey, metaStyle, dirPath FROM idmapCollection"
						 + " WHERE collActive = " + dbconn.dbstring(1),
						new String[]{"string", "string", "string"},
				// types
						false);
				// allow nulls in fields
				if (dbmat.length == 0) {
					mkerror("no idmapCollection records found having collActive");
				}
			} else {
				dbmat = new String[collParms.length][3];
				for (ii = 0; ii < collParms.length; ii++) {
					String cparm = collParms[ii];
					int ix = cparm.indexOf("/");
					if (ix <= 0) {
						badparms("invalid \"coll\" parameter: \""
								 + cparm + "\"");
					}
					String metastyle = cparm.substring(0, ix);
					String collKey = cparm.substring(ix + 1);
					if (metastyle.length() == 0 || collKey.length() == 0) {
						badparms("invalid \"coll\" parameter: \"" + cparm + "\"");
					}
					dbmat[ii][0] = collKey;
					dbmat[ii][1] = metastyle;
					dbmat[ii][2] = metastyle + "/" + collKey;
				}
			}

			// For each dbmat entry, call checkLinks() to check all links
			for (ii = 0; ii < dbmat.length; ii++) {
				String collKey = (String) dbmat[ii][0];
				String metastyle = (String) dbmat[ii][1];
				String subdir = (String) dbmat[ii][2];

				if (bugs >= 1) {
					prtln("\n\nIdmap.const: start collKey: " + collKey);
					prtln("    metastyle: \"" + metastyle + "\"");
					prtln("    basedirParm: \"" + basedirParm + "\"");
					prtln("    subdir: \"" + subdir + "\"");
				}

				try {
					checkLinks(bugs, propsdoc,
							collKey, metastyle, curdate, basedirParm, subdir,
							emailType, emailParm, forceMaxThreads, checkNetFlag);

				} catch (IdmapException iex) {
					hitError = true;
					prtln("Idmap.const: caught Exception for:"
							 + "  collKey: \"" + collKey + "\""
							 + "  metastyle: \"" + metastyle + "\""
							 + "  basedirParm: \"" + basedirParm + "\""
							 + "  subdir: \"" + subdir + "\""
							 + "  Exception: " + iex);
					iex.printStackTrace();
				} catch (MmdException iex) {
					hitError = true;
					prtln("Idmap.const: caught Exception for:"
							 + "  collKey: \"" + collKey + "\""
							 + "  metastyle: \"" + metastyle + "\""
							 + "  basedirParm: \"" + basedirParm + "\""
							 + "  subdir: \"" + subdir + "\""
							 + "  Exception: " + iex);
					iex.printStackTrace();
				}
			}

		} catch (Exception iex) {
			hitError = true;
			prtln("Idmap.const: caught Exception: " + iex);
			iex.printStackTrace();
		} finally {
			try {
				if (dbconn != null) {
					closeDb();
				}
			} catch (Exception iex) {
				hitError = true;
				prtln("Idmap.const: caught Exception: " + iex);
				iex.printStackTrace();
			}
		}

		if (bugs >= 1) {
			prtmemorystats("main.exit", starttime);
		}
		if (hitError) {
			System.exit(1);
		}
	}


	// end constructor

	/**
	 *  Opens the SQL database.
	 *
	 * @param  propsdoc            The properties document.
	 * @exception  IdmapException  
	 * @exception  XMLException    
	 */

	private void openDB(XMLDoc propsdoc)
			 throws IdmapException, XMLException {

		String dbClass = propsdoc.getXmlString("db/class");
		String dbHost = propsdoc.getXmlString("db/host");
		int dbPort = propsdoc.getXmlInt("db/port");
		if (dbPort <= 0 || dbPort > 64000) {
			mkerror("Property \"dbPort\" is <= 0 or > 64000");
		}
		String dbName = propsdoc.getXmlString("db/dbname");

		String dbUser = propsdoc.getXmlString("db/user");
		String dbPassword = propsdoc.getXmlString("db/password");

		// Get a DB connection
		try {
			Class.forName(dbClass).newInstance();
		} catch (ClassNotFoundException cnf) {
			mkerror("db driver not found.  Insure \""
					 + dbClass + "\" is in the CLASSPATH.  exc: " + cnf);
		} catch (InstantiationException iex) {
			mkerror("db driver not found.  Insure \""
					 + dbClass + "\" is in the CLASSPATH.  exc: " + iex);
		} catch (IllegalAccessException iex) {
			mkerror("db driver not found.  Insure \""
					 + dbClass + "\" is in the CLASSPATH.  exc: " + iex);
		}
		dbUrl = "jdbc:mysql://" + dbHost + ":" + dbPort
				 + "/" + dbName
				 + "?autoReconnect=true&user=" + dbUser
				 // + "?user=" + dbUser
				 + "&password=";
		// all except password, for err msgs
		printableUrl = dbUrl + "(omitted)";
		dbUrl += dbPassword;

		dbconn = null;
		try {
			dbconn = new DbConn(bugs, dbUrl);
		} catch (MmdException mde) {
			mkerror("could not open db connection to URL \""
					 + printableUrl + "\"  exc: " + mde);
		}
		if (bugs >= 1) {
			prtln("openDB: opened: " + printableUrl);
		}
		logit ("openDB: opened:\n\t" + printableUrl);
	}



	/**
	 *  Closes the DB connection.
	 *
	 * @exception  IdmapException  DESCRIPTION
	 */

	private void closeDb()
			 throws IdmapException {
		if (dbconn != null) {
			if (bugs >= 1) {
				prtln("closeDb: closing: " + printableUrl);
			}
			try {
				dbconn.closeDb();
			} catch (MmdException mde) {
				mkerror("could not close db connection to URL \""
						 + printableUrl + "\"  exc:" + mde);
			}
			dbconn = null;
		}
	}



	/**
	 *  Performs all link checking: Create list of ResourceDescs, call runthreads to retrieve
	 *  them all, store the results.
	 *
	 * @param  bugs                debug level
	 * @param  propsdoc            XMLDoc of Properties file.
	 * @param  collKey             The collection key.
	 * @param  metastyle           metadata style: one of the MS_ values defined in {@link
	 *      org.dlese.dpc.services.mmd.MmdRec MmdRec}.
	 * @param  curdate             The current data, in milliseconds since 1970.
	 * @param  basedirParm         The base directory containing subdir
	 * @param  subdir              The subdirectory under basedirParm containing the XML
	 *      metadata files.
	 * @param  emailType          
	 * @param  emailParm           
	 * @param  forceMaxThreads     
	 * @param  checkNetFlag        
	 * @exception  XMLException   
	 * @exception  IdmapException  
	 * @exception  MmdException    
	 */

	void checkLinks(
			int bugs,
			XMLDoc propsdoc,
			String collKey,
			String metastyle,
			long curdate,
			String basedirParm,
			String subdir,
			int emailType,
			String emailParm,
			int forceMaxThreads,
			boolean checkNetFlag)
			 throws XMLException, IdmapException, MmdException {
		int icoll;
		int ii;
		int ix;

		// Simple logging:
		logit("*checkLinks()* with collKey: " + collKey);

		forceEmail = false;
		// special force email to go out

		// Acquire and validate parms from properties doc
		int maxThreads = propsdoc.getXmlInt("general/maxThreads");
		if (maxThreads <= 0 || maxThreads > 1000) {
			mkerror("Property \"maxThreads\" is <= 0 or > 1000");
		}

		if (forceMaxThreads != 0) {
			maxThreads = forceMaxThreads;
		}

		int timeoutSeconds = propsdoc.getXmlInt("general/timeoutSeconds");
		if (timeoutSeconds <= 0 || timeoutSeconds > 1000) {
			mkerror("Property \"timeoutSeconds\" is <= 0 or > 1000");
		}

		int historyDays = propsdoc.getXmlInt("general/historyDays");
		if (historyDays <= 0 || historyDays > 100) {
			mkerror("Property \"historyDays\" is <= 0 or > 100");
		}

		int ivitalCutoff = propsdoc.getXmlInt("general/vitalCutoff");
		if (ivitalCutoff <= 0 || ivitalCutoff >= 100) {
			mkerror("Property \"ivitalCutoff\" is <= 0 or >= 100");
		}

		// Create boolean[] emailDays == seven bits,
		// true if we should send email report on that day of the week.
		String emailDaystg = propsdoc.getXmlString("general/emailDays");
		StringTokenizer tok = new StringTokenizer(emailDaystg, " ,", false);
		boolean[] emailDays = new boolean[7];
		// Sunday == 0
		String[] daynames = new String[]{"sunday", "monday", "tuesday",
				"wednesday", "thursday", "friday", "saturday"};
		while (tok.hasMoreTokens()) {
			String daystg = tok.nextToken();
			int iday = -1;
			for (ii = 0; ii < daynames.length; ii++) {
				if (daynames[ii].equalsIgnoreCase(daystg)) {
					iday = ii;
					break;
				}
			}
			if (iday == -1) {
				mkerror("Property \"emailDays\" has an invalid day: \""
						 + daystg + "\"");
			}
			emailDays[iday] = true;
		}

		String emailHost = propsdoc.getXmlString("general/emailHost");
		String emailFrom = propsdoc.getXmlString("general/emailFrom");

		String[] emailAddrs = null;
		if (emailType == EMAIL_NORMAL || emailType == EMAIL_FORCE) {
			emailAddrs = propsdoc.getXmlFields(0, 0, "general/emailAddr");
		} else if (emailType == EMAIL_PRINT) {
			emailAddrs = null;
		} else if (emailType == EMAIL_SINGLE) {
			emailAddrs = new String[]{emailParm};
		} else {
			mkerror("checklinks: invalid emailType");
		}

		String emailSubject = propsdoc.getXmlString("general/emailSubject");

		int cksumtype = PageDesc.CKSUMTYPE_STD;
		String cksumtypestg = propsdoc.getXmlString("general/cksumtype");
		if (cksumtypestg.equals("standard")) {
			cksumtype = PageDesc.CKSUMTYPE_STD;
		} else if (cksumtypestg.equals("exact")) {
			cksumtype = PageDesc.CKSUMTYPE_EXACT;
		} else {
			mkerror("Property \"cksumtype\" is invalid: \""
					 + cksumtypestg + "\"");
		}

		// Check that the network is alive.
		// Get the urls to be checked to see if the network is up.
		// These must be reachable before any other checking can happen.

		if (checkNetFlag) {
			String[] netcheckurls = propsdoc.getXmlFields(
					0, 0, "networkcheck/url");
			checkNetwork(bugs, maxThreads, timeoutSeconds, netcheckurls,
					emailHost, emailFrom, emailType, emailAddrs, emailSubject);
		}

		// Create rec in idmapCollection if not already there
		try {
			String tmpstg = dbconn.getDbString(
					"SELECT collKey FROM idmapCollection"
					 + " WHERE collKey = " + dbconn.dbstring(collKey));
		} catch (MmdException mex) {
			dbconn.updateDb("INSERT INTO idmapCollection"
					 + " (collKey, collActive, collName, metastyle, dirPath)"
					 + " VALUES("
					 + dbconn.dbstringcom(collKey)
					 + dbconn.dbstringcom(0)
			// initially not active
					 + dbconn.dbstringcom(collKey)
			// temp collName = collKey
					 + dbconn.dbstringcom(metastyle)
					 + dbconn.dbstring(subdir)
					 + ")");
		}

		// dirPath is the dir containing one collection's records
		File dirPath = new File(basedirParm + "/" + subdir);

		//  Create an array of ResourceDescs that need to be scanned.
		ResourceDesc[] rsds = createResourceList(bugs,
				collKey, metastyle, dirPath, cksumtype);

		logit ("*createResourceList()* returned " + rsds.length + " ResourceDescs");
				
		// If there were 0 xml files found then send warning.
		if (rsds.length == 0) {

			String subj = emailSubject + ": MISSING COLLECTION: " + collKey;
			StringBuffer msgbuf = new StringBuffer();
			msgbuf.append(subj + "\n");
			msgbuf.append("No XML files were found for collection: "
					 + collKey + "\n"
					 + "in the specified directory: " + dirPath);
			try {
				sendemail(emailHost, emailFrom, emailAddrs,
						subj, msgbuf.toString());
			} catch (Exception exc) {
				mkerror("Idmap.checkLinks: cannot send email.  exc: " + exc
						 + "\n\nsubj: " + subj
						 + "\nmsg:\n" + msgbuf);
			}
		} else {
			// else at least one XML file was found

			// ***** Run all the threads *****
			// For each ResourceDesc in rsds, starts a ScanThread to retrieve
			// and analyze its pages, then joins all the threads
			// and accumulates the warnings in each ResourceDesc.
			logit ("Calling runThreads()");
			runThreads(bugs, maxThreads, timeoutSeconds, rsds);
			logit ("back from runThreads()");
			
			// Process results
			// For each ResourceDesc in rsds, store the results in the
			// various DB tables.
			storeResults(collKey, metastyle, curdate,
					historyDays, ivitalCutoff, rsds);

			logit ("returned from storeResults");
					
			// If it's time to send email, do so
			boolean sentEmail = testEmailTime(bugs,
					collKey, metastyle, dirPath, rsds.length, curdate,
					emailDays, emailHost,
					emailFrom, emailType, emailAddrs, emailSubject);

			// If we sent email, update the lastEmailDate
			if (sentEmail) {
				dbconn.updateDb("UPDATE idmapCollection SET lastEmailDate = "
						 + dbconn.dbstring(new Timestamp(curdate))
						 + " WHERE collKey = " + dbconn.dbstring(collKey));
			}

			// Find the num of warnings we issued.
			Object[][] dbmat = dbconn.getDbTable(
					"SELECT COUNT(*) FROM idmapMessages"
					 + " WHERE collKey = " + dbconn.dbstring(collKey)
					 + " AND recCheckDate = "
					 + dbconn.dbstring(new Timestamp(curdate)),
					new String[]{"string"},
					true);
			// allow nulls in fields
			int numwarns = 0;
			if (dbmat != null && dbmat.length > 0) {
				try {
					numwarns = Integer.parseInt((String) dbmat[0][0], 10);
				} catch (NumberFormatException nfe) {
					mkerror("checkLinks: invalid numwarns: \""
							 + dbmat[0][0] + "\"");
				}
			}

			// Update idmapCollection to show check date and num warnings.
			dbconn.updateDb("UPDATE idmapCollection SET collCheckDate = "
					 + dbconn.dbstring(new Timestamp(curdate))
					 + ", numResources = " + dbconn.dbstring(rsds.length)
					 + ", numWarnings = " + dbconn.dbstring(numwarns)
					 + " WHERE collKey = " + dbconn.dbstring(collKey));

			dbconn.updateDb("INSERT INTO idmapCheckDate"
					 + " (collKey, collCheckDate)"
					 + " VALUES("
					 + dbconn.dbstringcom(collKey)
					 + dbconn.dbstring(new Timestamp(curdate))
					 + ")");
		}
		// else at least one XML file was found

		logit("checkLinks end: collKey: " + collKey);
	}


	// end checkLinks


	/**
	 *  Checks that the network is up. If not, throws an IdmapException.
	 *
	 * @param  bugs                debug level
	 * @param  maxThreads          the max number of concurrent threads
	 * @param  timeoutSeconds      timeout value for sockets, in seconds
	 * @param  emailHost           host name of the email server
	 * @param  emailFrom           "from" name to be used in email messages
	 * @param  emailType           email cmd line parm: EMAIL_NORMAL, EMAIL_FORCE,
	 *      EMAIL_PRINT, or EMAIL_SINGLE
	 * @param  emailAddrs          array of "to" names for email messages
	 * @param  emailSubject        initial part of the email "Subject" line
	 * @param  netcheckurls        DESCRIPTION
	 * @exception  IdmapException  DESCRIPTION
	 */

	void checkNetwork(
			int bugs,
			int maxThreads,
			int timeoutSeconds,
			String[] netcheckurls,
			String emailHost,
			String emailFrom,
			int emailType,
			String[] emailAddrs,
			String emailSubject)
			 throws IdmapException {
		int ii;
		int numpages = netcheckurls.length;

		// Build a ResourceDesc for each page.
		ResourceDesc[] rsds = new ResourceDesc[numpages];
		WarnBuf warnBuf = new WarnBuf();
		for (ii = 0; ii < numpages; ii++) {
			rsds[ii] = new ResourceDesc(
					"network check",
			// collKey
					MmdRec.metastyleNames[MmdRec.MS_ADN],
			// metastyle
					"network_check",
			// dirPath
					"network_check",
			// fileName
					null);
			// urlOnlyTests
			rsds[ii].setId("network check: " + netcheckurls[ii]);
			rsds[ii].addPage(new PageDesc(
					rsds[ii], "no xpath", "netcheck-url", netcheckurls[ii],
					PageDesc.CKSUMTYPE_STD));
		}

		// ***** Run all the threads to check the network *****
		runThreads(bugs, maxThreads, timeoutSeconds, rsds);

		// Make a list of the errors
		for (ii = 0; ii < numpages; ii++) {
			if (bugs >= 1) {
				PageDesc page = rsds[ii].pages[0];
				prtln("checkNetwork " + ii + ": respcode: " + page.respcode
						 + "  resptime: " + page.resptime
						 + "  URL: \"" + page.urlstg + "\"");
			}
			warnBuf.addAll(rsds[ii].warnBuf);
		}

		// Allow some sites to be down.
		double maxdownfrac = 0.15;
		// max fraction of sites that can be down
		if (warnBuf.length() > maxdownfrac * rsds.length) {
			String subj = emailSubject + ": NOT RUN - NETWORK FAILURE";
			StringBuffer msgbuf = new StringBuffer();
			msgbuf.append(subj + "\n");
			msgbuf.append("The network is not reliable right now.\n"
					 + warnBuf.length() + " out of " + netcheckurls.length
					 + " reference sites were not accessible.\n");
			msgbuf.append("Error messages from the network check reference"
					 + " sites are:\n\n");
			msgbuf.append(warnBuf.toString());
			try {
				sendemail(emailHost, emailFrom, emailAddrs,
						subj, msgbuf.toString());
			} catch (Exception exc) {
				mkerror("Idmap.checkNetwork: cannot send email.  exc: " + exc
						 + "\n\nsubj: " + subj
						 + "\nmsg:\n" + msgbuf);
			}
			mkerror(subj + "\nmsg:\n" + msgbuf);
		}
	}


	// end checkNetwork

	/**
	 *  Returns an array of ResourceDescs that need to be scanned.
	 *
	 * @param  bugs                debug level
	 * @param  collKey             The collection key.
	 * @param  metastyle           metadata style: one of the MS_ values defined in {@link
	 *      org.dlese.dpc.services.mmd.MmdRec MmdRec}.
	 * @param  dirPath             The path of the directory containing collKey's XML files.
	 * @param  cksumtype           DESCRIPTION
	 * @return                     An array of Resoucedesc, each of which describes a
	 *      resource (an Id) to be checked. Each ResourceDesc may contain multiple PageDescs,
	 *      one for each URL: primary, mirrors, related sites, etc.
	 * @exception  IdmapException  DESCRIPTION
	 * @exception  XMLException    DESCRIPTION
	 */

	ResourceDesc[] createResourceList(
			int bugs,
			String collKey,
	// collection key
			String metastyle,
			File dirPath,
			int cksumtype)
			 throws IdmapException, XMLException {
		int ii;
		LinkedList rsdlist = new LinkedList();

		// logit ("in createResourceList()");
		
		// Get all the .xml file names in a dir that have
		// been modified since prevmoddate.
		File[] xmlfiles = getDirFiles(0, dirPath);
		// prevmoddate = 0
		if (bugs >= 10) {
			prtln("createResourceList: num xmlfiles: " + xmlfiles.length);
			for (ii = 0; ii < xmlfiles.length; ii++) {
				prtln("createResourceList: xmlfile: \"" + xmlfiles[ii] + "\"");
			}
		}

		// For some flakey sites, we cannot compare the resource content
		// in PageDesc.java.  The content changes too frequently.
		// So we simply compare URLs in PageDesc.java.
		String[] urlOnlyTests = new String[]{
				"http://meted.comet.ucar.edu/fogstrat/ic31/ic311/frames"
				};

		// Add ResourceDescs to rsdlist.
		// Check for duplicate ids using dupmap.
		// dupmap maps:  String resourceId -> ResourceDesc.
		HashMap dupmap = new HashMap();
		// check for dup ids
		for (ii = 0; ii < xmlfiles.length; ii++) {
			File file = xmlfiles[ii];

			ResourceDesc rsd = new ResourceDesc(collKey, metastyle,
					dirPath.getAbsolutePath(), file.getName(), urlOnlyTests);
			try {
				checkResource(metastyle, rsd, dupmap, cksumtype);
			} catch (Exception exc) {
				prtln("createResourceList: caught: " + exc);
				exc.printStackTrace();

				// Caution: rsd.getId() could be null at this point
				String errorid = rsd.getId();
				if (errorid == null) {
					errorid = "(file: " + rsd.getFullFileName() + ")";
				}
				rsd.addWarning(new Warning(
						DpcErrors.IDMAP_BAD_XML_FILE,
						errorid,
						rsd.getFullFileName(),
						null,
				// xpath
						null,
				// urllabel
						null,
				// url
						exc.getMessage(),
				// msg = exception msg
						null));
				// auxinfo
			}

			rsdlist.add(rsd);
		}

		ResourceDesc[] rsds = (ResourceDesc[]) rsdlist.toArray(
				new ResourceDesc[0]);

		///// Sort by id so we can check for duplicate ids
		///Arrays.sort( rsds, new Comparator() {
		///	public int compare( Object obja, Object objb) {
		///		int ires;
		///		String ida = ((ResourceDesc) obja).id;
		///		String idb = ((ResourceDesc) objb).id;
		///		if (ida == null) {
		///			if (idb == null) ires = 0;
		///			else ires = -1;
		///		}
		///		else {
		///			if (idb == null) ires = 1;
		///			else ires = ida.compareTo( idb);
		///		}
		///		return ires;
		///	}
		///});
		///String previd = null;
		///for (ii = 0; ii < rsds.length; ii++) {
		///	ResourceDesc rsd = rsds[ii];
		///	prtln("testiddup: ii: " + ii + "  id: " + rsd.getId());
		///	if (rsd.getId() != null && previd != null
		///		&& rsd.getId().equals( previd))
		///	{
		///		rsd.addWarning( new Warning( DpcErrors.IDMAP_DUP_ID,
		///			rsd.getId(), rsd.getFileName(), "dup id:", previd, null));
		///	}
		///	previd = rsd.getId();
		///}

		// logit ("end createResourceList (" + rsds.length + " ResourceDescs"); 
		
		return rsds;
	}


	// end createResourceList

	/**
	 *  Gets the XML doc in: dirPath/rsd.fileName. Extracts all the important urls and email
	 *  addresses, from the XML doc, checks them for syntax, and fills in the ResourceDesc
	 *  from them. <p>
	 *
	 *  To do the real work, depending on the metastyle we call one of:
	 *  <ul>
	 *    <li> {@link #check_adn check_adn}
	 *    <li> {@link #check_dlese_anno check_dlese_anno}
	 *    <li> {@link #check_dlese_collect check_dlese_collect}
	 *    <li> (@link #check_news_opps check_news_opps)
	 *  </ul>
	 *
	 * @param  metastyle           metadata style: one of the MS_ values defined in {@link
	 *      org.dlese.dpc.services.mmd.MmdRec MmdRec}.
	 * @param  rsd                 The ResourceDesc to be filled in.
	 * @param  dupmap              HashMap used for checking for duplicate ids. # dupmap
	 *      maps: String resourceId -> ResourceDesc.
	 * @param  cksumtype           DESCRIPTION
	 * @exception  IdmapException  DESCRIPTION
	 * @exception  XMLException    DESCRIPTION
	 */

	private void checkResource(
			String metastyle,
			ResourceDesc rsd,
			HashMap dupmap,
			int cksumtype)
			 throws IdmapException, XMLException {
		int ipage;
		int ii;
		int kk;

		rsd.setMetaChecksum(getFileChecksum(rsd.getFullFileName()));

		XMLDoc doc = new XMLDoc(
				"file://" + rsd.getFullFileName(),
		// read the resource's xml file
				false,
		// validating
				false,
		// namespaceAware
				false);
		// expandEntities

		if (metastyle.equals(MmdRec.metastyleNames[MmdRec.MS_ADN])) {
			check_adn(rsd, doc, dupmap, cksumtype);
		} else if (metastyle.equals(MmdRec.metastyleNames[MmdRec.MS_DLESE_ANNO])) {
			check_dlese_anno(rsd, doc, dupmap, cksumtype);
		} else if (metastyle.equals(MmdRec.metastyleNames[MmdRec.MS_DLESE_COLLECT])) {
			check_dlese_collect(rsd, doc, dupmap, cksumtype);
		} else if (metastyle.equals(MmdRec.metastyleNames[MmdRec.MS_NEWS_OPPS])) {
			check_news_opps(rsd, doc, dupmap, cksumtype);
		} else {
			mkerror("checkResource: invalid metastyle: \"" + metastyle + "\"");
		}

	}


	// end checkResource

	/**
	 *  Returns the CRC32 checksum of a file. Used to determine if a resource XML file has
	 *  changed.
	 *
	 * @param  fileName            DESCRIPTION
	 * @return                     The fileChecksum value
	 * @exception  IdmapException  DESCRIPTION
	 */

	long getFileChecksum(String fileName)
			 throws IdmapException {
		CRC32 crcobj = new CRC32();
		try {
			BufferedReader rdr = new BufferedReader(
					new FileReader(fileName));
			while (true) {
				int ib = rdr.read();
				if (ib < 0) {
					break;
				}
				crcobj.update(ib);
			}
			rdr.close();
		} catch (IOException ioe) {
			throw new IdmapException("Cannot read file \"" + fileName + "\"");
		}
		return crcobj.getValue();
	}



	/**
	 *  For metastyle MS_ADN ("adn") records, extracts all the important urls and email
	 *  addresses, checks them for syntax, and fills in the ResourceDesc from them.
	 *
	 * @param  rsd                 The ResourceDesc to be filled in.
	 * @param  doc                 The XML document for the resource.
	 * @param  dupmap              HashMap used for checking for duplicate ids. # dupmap
	 *      maps: String resourceId -> ResourceDesc.
	 * @param  cksumtype           DESCRIPTION
	 * @exception  IdmapException  DESCRIPTION
	 * @exception  XMLException    DESCRIPTION
	 */

//### do we want to return allok? ... allok &&= checkUrls(...)

	void check_adn(
			ResourceDesc rsd,
			XMLDoc doc,
			HashMap dupmap,
			int cksumtype)
			 throws IdmapException, XMLException {
		int ii;
		String[] fields;
		String xpath;

		// Get and validate id
		xpath = "metaMetadata/catalogEntries/catalog@entry";
		fields = doc.getXmlFields(0, 0, xpath);
		if (checkId(xpath, "adn-id", fields, rsd, dupmap)) {
			rsd.setId(fields[0]);
		}

		// primary url
		xpath = "technical/online/primaryURL";
		fields = doc.getXmlFields(0, 0, xpath);
		checkUrls(xpath, "primary-url", fields, rsd, 1, 1, true, cksumtype);

		// mirror urls
		xpath = "technical/online/mirrorURLs/mirrorURL";
		fields = doc.getXmlFields(0, 0, xpath);
		checkUrls(xpath, "mirror-url", fields, rsd, 0, 0, true, cksumtype);
		// min, max, retrieveFlag

		// relation urls
		xpath = "relations/relation/urlEntry@url";
		fields = doc.getXmlFields(0, 0, xpath);
		checkUrls(xpath, "relation-url", fields, rsd, 0, 0, true, cksumtype);
		// min, max, retrieveFlag


		// misc urls to be syntax checked ...
		String[][] syntaxXpaths = {
				{"keyword",
				"general/keywords/keyword@url"},
				{"life-org-url",
				"lifecycle/contributors/contributor/organization/instUrl"},
				{"meta-org-url",
				"metaMetadata/contributors/contributor/organization/instUrl"},
				{"meta-terms",
				"metaMetadata/termsOfUse@URI"},
				{"appr-org-url",
				"approval/contributors/contributor/organization/instUrl"},
				{"geo-bbsrc",
				"geospatialCoverages/geospatialCoverage/boundBox/bbSrcIDandURL@URL"},
				{"geo-bbplc",
				"geospatialCoverages/geospatialCoverage/boundBox/bbPlaces/IDandURL@URL"},
				{"geo-bbevt",
				"geospatialCoverages/geospatialCoverage/boundBox/bbEvents/IDandURL@URL"},
				{"geo-detsrc",
				"geospatialCoverages/geospatialCoverage/detGeos/detGeo/detSrcIDandURL@URL"},
				{"geo-detplc",
				"geospatialCoverages/geospatialCoverage/detPlaces/place/IDandURL@URL"},
				{"geo-detevt",
				"geospatialCoverages/geospatialCoverage/detEvents/event/IDandURL@URL"},
				{"time-url",
				"temporalCoverages/timeAndPeriod/periods/period/IDandURL@URL"}
				};

		for (ii = 0; ii < syntaxXpaths.length; ii++) {
			String urllabel = syntaxXpaths[ii][0];
			xpath = syntaxXpaths[ii][1];
			fields = doc.getXmlFields(0, 0, xpath);
			checkUrls(xpath, urllabel, fields, rsd, 0, 0, false, cksumtype);
			// min, max, retrieveFlag
		}

		// misc emails to be syntax checked ...
		String[][] emailXpaths = {
				{"life-person-email",
				"lifecycle/contributors/contributor/person/emailPrimary"},
				{"life-person-email-alt",
				"lifecycle/contributors/contributor/person/emailAlt"},
				{"life-org-email",
				"lifecycle/contributors/contributor/organization/instEmail"},
				{"meta-person-email",
				"metaMetadata/contributors/contributor/person/emailPrimary"},
				{"meta-person-alt",
				"metaMetadata/contributors/contributor/person/emailAlt"},
				{"meta-org-email",
				"metaMetadata/contributors/contributor/organization/instEmail"}
				};

		for (ii = 0; ii < emailXpaths.length; ii++) {
			String urllabel = emailXpaths[ii][0];
			xpath = emailXpaths[ii][1];
			fields = doc.getXmlFields(0, 0, xpath);
			checkEmails(xpath, urllabel, fields, rsd, 0, 0);
			// min, max
		}

		// check if accessiondate is filled in
		xpath = "metaMetadata/dateInfo@accessioned";
		fields = doc.getXmlFields(0, 0, xpath);
		if (fields.length == 0) {
			rsd.addWarning(new Warning(DpcErrors.IDMAP_ACCESSION_DATE_MISSING,
					rsd.getId(), rsd.getFileName(), xpath, null,
					null, "Accession Date Missing", null));
			// url, msg, auxinfo
		}
	}


	// end check_adn

	/**
	 *  For metastyle "dlese_anno" records, extracts all the important urls and email
	 *  addresses, checks them for syntax, and fills in the ResourceDesc from them.
	 *
	 * @param  rsd                 The ResourceDesc to be filled in.
	 * @param  doc                 The XML document for the resource.
	 * @param  dupmap              HashMap used for checking for duplicate ids. # dupmap
	 *      maps: String resourceId -> ResourceDesc.
	 * @param  cksumtype           DESCRIPTION
	 * @exception  IdmapException  DESCRIPTION
	 * @exception  XMLException    DESCRIPTION
	 */

//### do we want to return allok? ... allok &&= checkUrls(...)

	void check_dlese_anno(
			ResourceDesc rsd,
			XMLDoc doc,
			HashMap dupmap,
			int cksumtype)
			 throws IdmapException, XMLException {
		int ii;
		String[] fields;
		String xpath;
		String xpath1;
		String xpath2;

		// Get and validate id
		xpath = "service/recordID";
		fields = doc.getXmlFields(0, 0, xpath);
		if (checkId(xpath, "anno-id", fields, rsd, dupmap)) {
			rsd.setId(fields[0]);
		}

		boolean oldver = true;
		String path = "item";

		String find = doc.getXmlField(path);
		if (find == "") {
			oldver = false;

		} else {
			oldver = true;

		}

		// Content url may or may not exist.
		// If it exists, it is syntax checked AND retrieved:
		if (oldver == true) {
			xpath = "item/content/url";
		} else {
			xpath = "annotation/content/url";
		}

		fields = doc.getXmlFields(0, 0, xpath);

		checkUrls(xpath, "content-url", fields, rsd, 0, 1, true, cksumtype);
		// min, max, retrieveFlag

		// Context url may or may not exist.
		// If it exists, it is syntax checked AND retrieved:
		if (oldver == true) {
			xpath = "item/context";
		} else {
			xpath = "annotation/context";
		}
		fields = doc.getXmlFields(0, 0, xpath);

		checkUrls(xpath, "context-url", fields, rsd, 0, 1, true, cksumtype);
		// min, max, retrieveFlag


		// misc urls to be syntax checked ...

		String[][] syntaxXpaths1 = {
				{"org-url",
				"item/contributors/contributor/organization/instUrl"}
				};

		String[][] syntaxXpaths2 = {
				{"org-url",
				"annotation/contributors/contributor/organization/url"}
				};

		if (oldver == true) {
			for (ii = 0; ii < syntaxXpaths1.length; ii++) {
				String urllabel = syntaxXpaths1[ii][0];
				xpath = syntaxXpaths1[ii][1];
				fields = doc.getXmlFields(0, 0, xpath);
				if (fields.length != 0) {
					checkUrls(xpath, urllabel, fields, rsd, 0, 0, false, cksumtype);
				}
				// min, max, retrieveFlag
			}
		} else {
			for (ii = 0; ii < syntaxXpaths2.length; ii++) {
				String urllabel = syntaxXpaths2[ii][0];
				xpath = syntaxXpaths2[ii][1];
				fields = doc.getXmlFields(0, 0, xpath);
				if (fields.length != 0) {
					checkUrls(xpath, urllabel, fields, rsd, 0, 0, false, cksumtype);
				}
				// min, max, retrieveFlag
			}

		}

		// misc emails to be syntax checked ...
		String[][] emailXpaths1 = {
				{"person-email", "item/contributors/contributor/person/emailPrimary"},
				{"person-email-alt", "item/contributors/contributor/person/emailAlt"},
				{"org-email", "item/contributors/contributor/organization/instEmail"}
				};

		String[][] emailXpaths2 = {
				{"person-email", "annotation/contributors/contributor/person/email"},
				{"person-email-alt", "annotation/contributors/contributor/person/emailAlt"},
				{"org-email", "annotation/contributors/contributor/organization/email"}
				};

		if (oldver == true) {
			for (ii = 0; ii < emailXpaths1.length; ii++) {
				String urllabels = emailXpaths1[ii][0];
				xpath = emailXpaths1[ii][1];
				fields = doc.getXmlFields(0, 0, xpath);
				checkEmails(xpath, urllabels, fields, rsd, 0, 0);
				// min, max
			}
		} else {
			for (ii = 0; ii < emailXpaths2.length; ii++) {
				String urllabels = emailXpaths2[ii][0];
				xpath = emailXpaths2[ii][1];
				fields = doc.getXmlFields(0, 0, xpath);
				checkEmails(xpath, urllabels, fields, rsd, 0, 0);
				// min, max
			}

		}

	}


	// end check_dlese_anno

	/**
	 *  For metastyle "news_opps" records, extracts all the important urls and email
	 *  addresses, checks them for syntax, and fills in the ResourceDesc from them.
	 *
	 * @param  rsd                 The ResourceDesc to be filled in.
	 * @param  doc                 The XML document for the resource.
	 * @param  dupmap              HashMap used for checking for duplicate ids. # dupmap
	 *      maps: String resourceId -> ResourceDesc.
	 * @param  cksumtype           DESCRIPTION
	 * @exception  IdmapException  DESCRIPTION
	 * @exception  XMLException    DESCRIPTION
	 */



	void check_news_opps(
			ResourceDesc rsd,
			XMLDoc doc,
			HashMap dupmap,
			int cksumtype)
			 throws IdmapException, XMLException {
		int ii;
		String[] fields;
		String xpath;

		// Get and validate id
		xpath = "recordID";
		fields = doc.getXmlFields(0, 0, xpath);
		if (checkId(xpath, "news_opps-id", fields, rsd, dupmap)) {
			rsd.setId(fields[0]);
		}

		// Announcement url has to exist
		// it is syntax checked AND retrieved:
		xpath = "announcementURL";
		fields = doc.getXmlFields(0, 0, xpath);

		String archDate = "archiveDate";
		String datefields[];
		datefields = doc.getXmlFields(0, 0, archDate);

		Date aDate;
		Date compareDateNow = new Date();
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
		try {
			aDate = formatter.parse(datefields[0]);

			if ((compareDateNow.before(aDate)) || (compareDateNow.equals(aDate))) {
				checkUrls(xpath, "announcement-url", fields, rsd, 1, 1, true, cksumtype);
			}
			// min, max, retrieveFlag
			/*
			 *  else
			 *  {
			 *  rsd.addWarning( new Warning( DpcErrors.IDMAP_NEWS_PAST_ARCHIVE_DATE,
			 *  rsd.getId(), rsd.getFileName(), archDate, "announcementURL",
			 *  fields[0], "Check date is past archive date", null));
			 *  }
			 */
			// commented out because we do not want long clunky error reports

		} catch (ParseException e) {
			/*
			 *  rsd.addWarning( new Warning( DpcErrors.IDMAP_NEWS_ARCHIVE_DATE_NOPARSE,
			 *  rsd.getId(), rsd.getFileName(), archDate, "announcementURL",
			 *  fields[0], "archive date could not be parsed", null));
			 */
		}

		// misc emails to be syntax checked ...
		String[][] emailXpaths = {
				{"person-email", "contributors/contributor/person/email"},
				{"person-email-alt", "contributors/contributor/person/emailAlt"},
				{"org-email", "contributors/contributor/organization/email"}
				};
		for (ii = 0; ii < emailXpaths.length; ii++) {
			String urllabels = emailXpaths[ii][0];
			xpath = emailXpaths[ii][1];
			fields = doc.getXmlFields(0, 0, xpath);
			checkEmails(xpath, urllabels, fields, rsd, 0, 0);
			// min, max
		}
	}


	// end check_news_opps

	/**
	 *  For metastyle "dlese_collect" records, extracts all the important urls and email
	 *  addresses, checks them for syntax, and fills in the ResourceDesc from them.
	 *
	 * @param  rsd                 The ResourceDesc to be filled in.
	 * @param  doc                 The XML document for the resource.
	 * @param  dupmap              HashMap used for checking for duplicate ids. # dupmap
	 *      maps: String resourceId -> ResourceDesc.
	 * @param  cksumtype           DESCRIPTION
	 * @exception  IdmapException  DESCRIPTION
	 * @exception  XMLException    DESCRIPTION
	 */



	void check_dlese_collect(
			ResourceDesc rsd,
			XMLDoc doc,
			HashMap dupmap,
			int cksumtype)
			 throws IdmapException, XMLException {
		int ii;
		String[] fields;
		String xpath;

		// Get and validate id
		xpath = "metaMetadata/catalogEntries/catalog@entry";
		fields = doc.getXmlFields(0, 0, xpath);
		if (checkId(xpath, "coll-id", fields, rsd, dupmap)) {
			rsd.setId(fields[0]);
		}

		// Collection location url may or may not exist.
		// If it exists, it is syntax checked AND retrieved:
		xpath = "access/collectionLocation";
		fields = doc.getXmlFields(0, 0, xpath);
		checkUrls(xpath, "coll-url", fields, rsd, 0, 1, true, cksumtype);
		// min, max, retrieveFlag

		// general/reviewProcess@url may or may not exist.
		// If it exists, it is syntax checked AND retrieved:
		xpath = "general/reviewProcess/@url";
		fields = doc.getXmlFields(0, 0, xpath);
		checkUrls(xpath, "review-url", fields, rsd, 0, 1, true, cksumtype);
		// min, max, retrieveFlag


		// If general/policies/policy@type="Collection scope" then
		// general/policies/policy@url must exist and be retrieved.
		// Otherwise syntax check general/policies/policy@url
		xpath = "general/policies/policy@type";
		fields = doc.getXmlFields(0, 0, xpath);
		boolean foundit = false;
		for (ii = 0; ii < fields.length; ii++) {
			if (fields[ii].equals("Collection scope")) {
				foundit = true;
				break;
			}
		}
		xpath = "general/policies/policy@url";
		fields = doc.getXmlFields(0, 0, xpath);
		checkUrls(xpath, "scope-url", fields, rsd,
				foundit ? 1 : 0,
		// min num to find
				0,
		// max num (no limit)
				true,
		// retrieveFlag
				cksumtype);

		// misc urls to be syntax checked ...
		String[][] syntaxXpaths = {
				{"life-org-url",
				"lifecycle/contributors/contributor/organization/instUrl"},
				{"meta-org-url",
				"metaMetadata/contributors/contributor/organization/instUrl"},
				{"meta-terms",
				"metaMetadata/termsOfUse@URI"},
				{"appr-org-url",
				"approval/contributors/contributor/organization/instUrl"}
				};
		for (ii = 0; ii < syntaxXpaths.length; ii++) {
			String urllabel = syntaxXpaths[ii][0];
			xpath = syntaxXpaths[ii][1];
			fields = doc.getXmlFields(0, 0, xpath);
			checkUrls(xpath, urllabel, fields, rsd, 0, 0, false, cksumtype);
		}

		// misc emails to be syntax checked ...
		String[][] emailXpaths = {
				{"life-person-email",
				"lifecycle/contributors/contributor/person/emailPrimary"},
				{"life-person-email-alt",
				"lifecycle/contributors/contributor/person/emailAlt"},
				{"life-org-email",
				"lifecycle/contributors/contributor/organization/instEmail"},
				{"meta-person-email",
				"metaMetadata/contributors/contributor/person/emailPrimary"},
				{"meta-person-alt",
				"metaMetadata/contributors/contributor/person/emailAlt"},
				{"meta-org-email",
				"metaMetadata/contributors/contributor/organization/instEmail"},
				{"appr-person-email",
				"approval/contributors/contributor/person/emailPrimary"},
				{"appr-person-email-alt",
				"approval/contributors/contributor/person/emailAlt"},
				{"appr-org-email",
				"approval/contributors/contributor/person/instEmail"}
				};
		for (ii = 0; ii < emailXpaths.length; ii++) {
			String urllabel = emailXpaths[ii][0];
			xpath = emailXpaths[ii][1];
			fields = doc.getXmlFields(0, 0, xpath);
			checkEmails(xpath, urllabel, fields, rsd, 0, 0);
			// min, max
		}
	}


	// end check_dlese_collect

	/**
	 *  Checks that an array of IDs is valid: there must be only one.
	 *
	 * @param  xpath               The XML path of the field; used only in error messages.
	 * @param  fields              The array of IDs: should have exactly one element.
	 * @param  rsd                 The corresponding ResourceDesc.
	 * @param  dupmap              HashMap used for checking for duplicate ids. # dupmap
	 *      maps: String resourceId -> ResourceDesc.
	 * @param  urllabel            DESCRIPTION
	 * @return                     DESCRIPTION
	 * @exception  IdmapException  DESCRIPTION
	 */

	boolean checkId(
			String xpath,
			String urllabel,
			String[] fields,
			ResourceDesc rsd,
			HashMap dupmap)
			 throws IdmapException {
		boolean allok = true;
		if (fields.length < 1) {
			allok = false;
			rsd.addWarning(new Warning(DpcErrors.IDMAP_MISSING_FIELD,
					rsd.getId(), rsd.getFileName(), xpath, urllabel,
					null, "no id", null));
			// url, msg, auxinfo
		} else if (fields.length > 1) {
			allok = false;
			rsd.addWarning(new Warning(DpcErrors.IDMAP_MULT_FIELD,
					rsd.getId(), rsd.getFileName(), xpath, urllabel,
					null, "too many ids found", null));
			// url, msg, auxinfo
		} else {
			String id = fields[0];
			if (id.length() == 0 || id.length() > 100) {
				allok = false;
				rsd.addWarning(new Warning(DpcErrors.IDMAP_ID_SYNTAX,
						rsd.getId(), rsd.getFileName(), xpath, urllabel,
						null, "id syntax", null));
				// url, msg, auxinfo
			}
			ResourceDesc prevrsd = (ResourceDesc) dupmap.get(id);
			if (prevrsd != null) {
				rsd.addWarning(new Warning(DpcErrors.IDMAP_DUP_ID,
						rsd.getId(), rsd.getFileName(), xpath, urllabel,
						null, "dup id file", prevrsd.getFileName()));
			}
			// url, msg, aux
			else {
				dupmap.put(id, rsd);
			}
		}
		return allok;
	}



	/**
	 *  Checks that an array of URLs is valid. If retrieveFlag is set, adds a PageDesc for
	 *  each valid URL to rsd, so the pages will be retrieved later.
	 *
	 * @param  xpath               The XML path of the field; used only in error messages.
	 * @param  minnum              The minimum number of URLs that may be in fields.
	 * @param  maxnum              The maximum number of URLs that may be in fields.
	 * @param  retrieveFlag        If true, for each valid URL add a PageDesc to rsd so the
	 *      page will be retrieved later.
	 * @param  fields              The array of URLs.
	 * @param  rsd                 The corresponding ResourceDesc.
	 * @param  urllabel            DESCRIPTION
	 * @param  cksumtype           DESCRIPTION
	 * @return                     DESCRIPTION
	 * @exception  IdmapException  DESCRIPTION
	 */
//### redo doc

	boolean checkUrls(
			String xpath,
			String urllabel,
			String[] fields,
			ResourceDesc rsd,
			int minnum,
			int maxnum,
			boolean retrieveFlag,
			int cksumtype)
			 throws IdmapException {
		int ii;
		int jj;
		boolean allok = true;

		if (fields.length < minnum) {
			allok = false;
			rsd.addWarning(new Warning(DpcErrors.IDMAP_MISSING_FIELD,
					rsd.getId(), rsd.getFileName(), xpath, urllabel,
					null, "no URL field found", null));
			// url, msg, auxinfo
		}
		if (maxnum != 0 && fields.length > maxnum) {
			allok = false;
			rsd.addWarning(new Warning(DpcErrors.IDMAP_MULT_FIELD,
					rsd.getId(), rsd.getFileName(), xpath, urllabel,
					null, "too many URL fields found", null));
			// url, msg, auxinfo
		}

		for (ii = 0; ii < fields.length; ii++) {
			String urlstg = fields[ii];

			// Check for legal chars.
			// See: http://www.ietf.org/rfc/rfc2396.txt
			//
			// NOTE: We also allow space characters in URLS.
			// Although spaces aren't legal, NASA uses them frequently
			// so we oblige them.
			//
			// From rfc 2396:
			//      URI-reference = [ absoluteURI | relativeURI ] [ "#" fragment ]
			// ...
			//      uric          = reserved | unreserved | escaped
			//      reserved      = ";" | "/" | "?" | ":" | "@" | "&" | "=" | "+" |
			//                      "$" | ","
			//      unreserved    = alphanum | mark
			//      mark          = "-" | "_" | "." | "!" | "~" | "*" | "'" |
			//                      "(" | ")"
			//      escaped       = "%" hex hex

			String okchars = "#"
			// URI-reference: fragment
					 + ";/?:@&=+$,"
			// reserved
					 + "abcdefghijklmnopqrstuvwxyz"
			// unreserved: alphanum
					 + "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
					 + "0123456789"
					 + "-_.!~*'()|\\"
			// unreserved: mark
					 + "%"
			// escaped
					 + " ";
			// kluge for NASA

			for (jj = 0; jj < urlstg.length(); jj++) {
				char cc = urlstg.charAt(jj);
				if (okchars.indexOf(cc) < 0) {
					allok = false;
					rsd.addWarning(new Warning(DpcErrors.IDMAP_URL_SYNTAX,
							rsd.getId(), rsd.getFileName(), xpath, urllabel,
							urlstg, "invalid char", "offset: " + jj));
				}
			}

			if (allok) {
				if (urlstg.indexOf("://") < 0) {
					urlstg = "http://" + urlstg;
				}
				try {
					new URL(urlstg);
				} catch (MalformedURLException mfe) {
					allok = false;
					rsd.addWarning(new Warning(DpcErrors.IDMAP_URL_SYNTAX,
							rsd.getId(), rsd.getFileName(), xpath, urllabel,
							urlstg, "malformed url", mfe.getMessage()));
				}
			}
			if (allok && retrieveFlag) {
				rsd.addPage(new PageDesc(rsd, xpath, urllabel, urlstg,
						cksumtype));
			}
		}
		// for ii
		return allok;
	}


	// end checkUrls

	/**
	 *  Checks that an array of email addresses is valid.
	 *
	 * @param  xpath               The XML path of the field; used only in error messages.
	 * @param  minnum              The minimum number of emails that may be in fields.
	 * @param  maxnum              The maximum number of emails that may be in fields.
	 * @param  fields              The array of email addresses.
	 * @param  rsd                 The corresponding ResourceDesc.
	 * @param  urllabel            DESCRIPTION
	 * @return                     DESCRIPTION
	 * @exception  IdmapException  DESCRIPTION
	 */

	boolean checkEmails(
			String xpath,
			String urllabel,
			String[] fields,
			ResourceDesc rsd,
			int minnum,
			int maxnum)
			 throws IdmapException {
		int ii;
		boolean allok = true;

		if (fields.length < minnum) {
			allok = false;
			rsd.addWarning(new Warning(DpcErrors.IDMAP_MISSING_FIELD,
					rsd.getId(), rsd.getFileName(), xpath, urllabel,
					null, "email not found", null));
		}
		if (maxnum != 0 && fields.length > maxnum) {
			allok = false;
			rsd.addWarning(new Warning(DpcErrors.IDMAP_MULT_FIELD,
					rsd.getId(), rsd.getFileName(), xpath, urllabel,
					null, "too many emails found", null));
		}

		for (ii = 0; ii < fields.length; ii++) {
			String emailstg = fields[ii];
			if (!emailstg.equalsIgnoreCase("Unknown")) {
				try {
					checkEmailAddress(emailstg);
				} catch (IdmapException iex) {
					allok = false;
					rsd.addWarning(new Warning(DpcErrors.IDMAP_EMAIL_SYNTAX,
							rsd.getId(), rsd.getFileName(), xpath, urllabel,
							emailstg, iex.getMessage(), null));
				}
			}
		}
		return allok;
	}


	// end checkEmails

	/**
	 *  Checks that the specified email address is valid. If not, throws an IdmapException.
	 *  <br>
	 *  We cannot use simply:<br>
	 *  <code>new InternetAddress( emailaddr)</code><br>
	 *  since that constructor accepts nearly any bogus String.
	 *
	 * @param  emailaddrparm       DESCRIPTION
	 * @exception  IdmapException  DESCRIPTION
	 */

	void checkEmailAddress(String emailaddrparm)
			 throws IdmapException {
		int ii;
		String emailaddr = emailaddrparm.toLowerCase();

		// Check for valid email address.
		// Just constructing new InternetAddress( emailaddr)
		// won't work, as InternetAddress accepts nearly anything.
		// See:
		// RFC 821, 822, 2487, 2821, 2822, 2920, 3030
		// In particular, see RFC 822 ...
		// http://www.faqs.org/rfcs/rfc822.html
		// Section 6.1, Address specification syntax:
		//		address = mailbox / group
		//		mailbox = addr-spec / ...
		//		addr-spec = local-part "@" domain
		//		local-part = word *("." word)
		//		domain = sub-domain ("." sub-domain)
		//		sub-domain = domain-ref / domain-literal
		//		domain-ref = atom
		// Section 3.3, Lexical tokens:
		//		word = atom / quoted-string
		//		atom = 1*<any CHAR except specials, SPACE and CTLs>
		//
		//		specials    =  "(" / ")" / "<" / ">" / "@"
		//			/  "," / ";" / ":" / "\" / <">
		//			/  "." / "[" / "]"
		//		Specials must be in a quoted string to use within a word.

		String errmsg = null;
		String okchars = "abcdefghijklmnopqrstuvwxyz0123456789-+_.@'";

		// Legal top level domains
		// See:
		//   http://www.ntia.doc.gov/ntiahome/domainname/domainhome.htm
		//   http://www.iana.org/cctld/cctld.htm
		String[] tlds = {
				"com", "edu", "gov", "info", "int", "mil", "net", "org",
				"ac", "ad", "ae", "af", "ag", "ai", "al", "am", "an", "ao",
				"aq", "ar", "as", "at", "au", "aw", "az", "ba", "bb", "bd",
				"be", "bf", "bg", "bh", "bi", "bj", "bm", "bn", "bo", "br",
				"bs", "bt", "bv", "bw", "by", "bz", "ca", "cc", "cd", "cf",
				"cg", "ch", "ci", "ck", "cl", "cm", "cn", "co", "cr", "cu",
				"cv", "cx", "cy", "cz", "de", "dj", "dk", "dm", "do", "dz",
				"ec", "ee", "eg", "eh", "er", "es", "et", "fi", "fj", "fk",
				"fm", "fo", "fr", "ga", "gd", "ge", "gf", "gg", "gh", "gi",
				"gl", "gm", "gn", "gp", "gq", "gr", "gs", "gt", "gu", "gw",
				"gy", "hk", "hm", "hn", "hr", "ht", "hu", "id", "ie", "il",
				"im", "in", "io", "iq", "ir", "is", "it", "je", "jm", "jo",
				"jp", "ke", "kg", "kh", "ki", "km", "kn", "kp", "kr", "kw",
				"ky", "kz", "la", "lb", "lc", "li", "lk", "lr", "ls", "lt",
				"lu", "lv", "ly", "ma", "mc", "md", "mg", "mh", "mk", "ml",
				"mm", "mn", "mo", "mp", "mq", "mr", "ms", "mt", "mu", "mv",
				"mw", "mx", "my", "mz", "na", "nc", "ne", "nf", "ng", "ni",
				"nl", "no", "np", "nr", "nu", "nz", "om", "pa", "pe", "pf",
				"pg", "ph", "pk", "pl", "pm", "pn", "pr", "ps", "pt", "pw",
				"py", "qa", "re", "ro", "ru", "rw", "sa", "sb", "sc", "sd",
				"se", "sg", "sh", "si", "sj", "sk", "sl", "sm", "sn", "so",
				"sr", "st", "sv", "sy", "sz", "tc", "td", "tf", "tg", "th",
				"tj", "tk", "tm", "tn", "to", "tp", "tr", "tt", "tv", "tw",
				"tz", "ua", "ug", "uk", "um", "us", "uy", "uz", "va", "vc",
				"ve", "vg", "vi", "vn", "vu", "wf", "ws", "ye", "yt", "yu",
				"za", "zm", "zw"};

		int numat = 0;
		// number of "@" must be 1
		int ix;
		for (ix = 0; ix < emailaddr.length(); ix++) {
			char cc = emailaddr.charAt(ix);
			if (cc == '@') {
				numat++;
			}
			if (okchars.indexOf(cc) < 0) {
				throw new IdmapException("illegal char: \"" + cc
						 + "\" (decimal " + ((int) cc) + ", at offset "
						 + ix + ")");
			}
		}
		if (numat != 1) {
			throw new IdmapException("num @ symbols not 1");
		}
		ix = emailaddr.lastIndexOf(".");
		if (ix < 0) {
			throw new IdmapException("no period found");
		}

		String suffix = emailaddr.substring(ix + 1);
		boolean foundtld = false;
		for (ii = 0; ii < tlds.length; ii++) {
			if (suffix.equals(tlds[ii])) {
				foundtld = true;
				break;
			}
		}
		if (!foundtld) {
			throw new IdmapException("unknown suffix: \""
					 + suffix + "\"");
		}
	}



	/**
	 *  For each ResourceDesc in rsds, starts a ScanThread to retrieve and analyze its pages,
	 *  then joins all the threads and accumulates the warnings in each ResourceDesc. <p>
	 *
	 *  We could use standard Java synchronization here with a thread pool and wait() and
	 *  notify(). But this is much simpler and just as effective in the current situation.
	 *  Java reclaims memory as threads exit, so even though the thdlist is large not all are
	 *  active or taking memory.
	 *
	 * @param  bugs                debug level
	 * @param  maxThreads          the max number of concurrent threads
	 * @param  timeoutSeconds      timeout value for sockets, in seconds
	 * @param  rsds                An array of Resoucedesc, each of which describes a
	 *      resource (an Id) to be checked. The results from scanning are put in the rsds
	 *      entries.
	 * @exception  IdmapException  DESCRIPTION
	 */

	void runThreads(
			int bugs,
			int maxThreads,
			int timeoutSeconds,
			ResourceDesc[] rsds)
			 throws IdmapException {

		int irsd;

		int ipage;
		long starttime = System.currentTimeMillis();
		if (bugs >= 1) {
			prtmemorystats("runThreads.entry", starttime);
		}

		ThreadGroup thdgrp = new ThreadGroup("some name");
		LinkedList thdlist = new LinkedList();
		int threadCounter = 0;

		for (irsd = 0; irsd < rsds.length; irsd++) {
			ResourceDesc rsd = rsds[irsd];
			for (ipage = 0; ipage < rsd.numpages; ipage++) {
				PageDesc page = rsd.pages[ipage];

				// Wait until a thread is free
				boolean sentwaitmsg = false;
				while (thdgrp.activeCount() >= maxThreads) {
					if (bugs >= 50 && !sentwaitmsg) {
						sentwaitmsg = true;
						prtln("runThreads: waiting on thread:  irsd: "
								 + irsd + "  ipage: " + ipage + "  \""
								 + page.urlstg + "\"");
					}
					try {
						Thread.sleep(100);
					}
					// 0.1 second
					catch (InterruptedException iex) {}
				}

				// Harvest any finished threads
				// to free memory resources.
				harvestThreads(rsds);

				// Create and start the next thread
				int threadId = threadCounter++;
				page.scanThread = new ScanThread(bugs, thdgrp,
						threadId, timeoutSeconds, page);

				if (bugs >= 10) {
					prtln("runThreads: start thread:"
							 + "  irsd: " + irsd + "  ipage: " + ipage + "  \""
							 + page.urlstg + "\"");
				}
				if (bugs >= 10) {
					prtmemorystats("runThreads: before start"
							 + "  irsd: " + irsd + "  ipage: " + ipage, starttime);
				}
				page.scanThread.start();
			}
		}

		// Join all threads and accumulate the warnings.
		// Perhaps we could just wait until activeCount is 0, but
		// it appears that Java leaves activeCount > 0.
		// Maybe that's because of the Processes we spawn.

		if (bugs >= 10) {
			prtln("runThreads: begin final join:");
		}

		// Harvest all finished threads.
		while (true) {
			int numleft = harvestThreads(rsds);
			if (bugs >= 10) {
				prtln("runThreads harvest: numleft: " + numleft);
			}
			if (numleft == 0) {
				break;
			}
			try {
				Thread.sleep(1000);
			} catch (InterruptedException iex) {}
		}

		if (bugs >= 1) {
			prtmemorystats("runThreads.exit", starttime);
		}
	}


	// end runThreads

	/**
	 *  Joins threads and appends each PageDesc's Warnings to it's ResourceDesc.
	 *
	 * @param  rsds                DESCRIPTION
	 * @return                     DESCRIPTION
	 * @exception  IdmapException  DESCRIPTION
	 */

	int harvestThreads(
			ResourceDesc[] rsds)
			 throws IdmapException {
		int irsd;
		int ipage;

		int numleft = 0;
		for (irsd = 0; irsd < rsds.length; irsd++) {
			ResourceDesc rsd = rsds[irsd];
			for (ipage = 0; ipage < rsd.numpages; ipage++) {
				PageDesc page = rsd.pages[ipage];
				if (bugs >= 50) {
					prtln("runThreads: before join thread:  irsd: "
							 + irsd + "  ipage: " + ipage + "  \""
							 + page.urlstg + "\"");
				}

				if (page.scanThread != null) {
					if (page.scanThread.isAlive()) {
						numleft++;
					} else {
						try {
							page.scanThread.join(100);
						} catch (InterruptedException iex) {
							mkerror("runThreads: join interrupted");
						}
						if (bugs >= 10) {
							prtln("    joined irsd: " + irsd
									 + "  ipage: " + ipage
									 + "  respcode: " + page.respcode
									 + "  resptime: " + page.resptime
									 + "  \"" + page.urlstg + "\"");
						}

						page.scanThread = null;
						// free memory
						System.gc();
					}
				}
			}
		}
		return numleft;
	}

	void touchDbconn () {
		try {
			dbconn.getDbTable(
				"SELECT CURRENT_DATE",
				new String[]{"string"},
				true);
		} catch (MmdException e) {
			logit ("touchDbconn() DB connection is not available, creating new connection ...");
			try {
				closeDb();
				openDB(propsdoc);
			} catch (Exception ee) {
				logit ("caught exception trying to create new connection: " + ee.getMessage());
			}
		}
	}
				

	/**
	 *  For each ResourceDesc in rsds, store the results in the various DB tables.
	 *
	 * @param  collKey             The collection key.
	 * @param  metastyle           metadata style: one of the MS_ values defined in {@link
	 *      org.dlese.dpc.services.mmd.MmdRec MmdRec}.
	 * @param  curdate             The current data, in milliseconds since 1970.
	 * @param  historyDays         The numbers of past history we examine when computing a
	 *      site's vitality.
	 * @param  ivitalCutoff        The percent of the time a site must be fully fully
	 *      functional. If the site's vitality falls below ivitalCutoff, a warning message is
	 *      sent.
	 * @param  rsds                An array of Resoucedesc, each of which describes a
	 *      resource (an Id) and the results from scanning it.
	 * @exception  IdmapException  DESCRIPTION
	 * @exception  MmdException    DESCRIPTION
	 * @exception  XMLException    Description of the Exception
	 */
	 
	void storeResults(
			String collKey,
			String metastyle,
			long curdate,
			int historyDays,
	// num days past history we examine.
			int ivitalCutoff,
	// % vitality below which we issue warning
			ResourceDesc[] rsds)
			 throws IdmapException, MmdException, XMLException {
		int ii;
		int irsd;
		int jrsd;
		Object[][] dbmat;
		int totalwarnings = 0;

		// Get all db recs and insure all are in the list of XML files.
		// SKIP: If not, set the status to STATUS_DEACCESSIONED.

		logit ("*storeResults()*");
		
		HashMap idmap = new HashMap();
		for (ii = 0; ii < rsds.length; ii++) {
			idmap.put(rsds[ii].getId(), rsds[ii]);
		}

		// logit ("about to select idmapMmd records for \'" + collKey + "\' collection");
		touchDbconn ();  // make sure db is awake
		dbmat = dbconn.getDbTable(
				"SELECT id, fileName, status, primaryUrl, hasFile FROM idmapMmd"
				 + " WHERE collKey = " + dbconn.dbstring(collKey),
				new String[]{"string", "string", "string", "string",
				"boolean"},
		// types
				true);
			
		// allow nulls in primaryUrl
		logit ("about to loop through " + dbmat.length + " idmapMmd records");
		for (ii = 0; ii < dbmat.length; ii++) {
			String dbid = (String) dbmat[ii][0];
			String dbfilename = (String) dbmat[ii][1];
			String dbstatus = (String) dbmat[ii][2];
			String dbprimaryUrl = (String) dbmat[ii][3];
			boolean dbhasFile = ((Boolean) dbmat[ii][4]).booleanValue();

			int errornum = -1;
			if ((!idmap.containsKey(dbid)) && dbhasFile) {
				errornum = DpcErrors.IDMAP_NO_XML_FILE;
				dbconn.updateDb("UPDATE idmapMmd SET hasFile = "
						 + dbconn.dbstring(0)
						 + " WHERE collKey = " + dbconn.dbstring(collKey)
						 + " AND id = " + dbconn.dbstring(dbid));
			} else if ((idmap.containsKey(dbid)) && !dbhasFile) {
				errornum = DpcErrors.IDMAP_XML_FILE_REAPPEARED;
				dbconn.updateDb("UPDATE idmapMmd SET hasFile = "
						 + dbconn.dbstring(1)
						 + " WHERE collKey = " + dbconn.dbstring(collKey)
						 + " AND id = " + dbconn.dbstring(dbid));
			} else if ((idmap.containsKey(dbid)) && dbhasFile) {
				ResourceDesc tmprsds = (ResourceDesc) idmap.get(dbid);
				String fname = tmprsds.getFileName();
				if (!dbfilename.equals(fname)) {
					errornum = DpcErrors.IDMAP_XML_FILE_NAME_CHANGED;
					dbconn.updateDb("UPDATE idmapMmd SET fileName = "
							 + dbconn.dbstring(fname)
							 + " WHERE collKey = " + dbconn.dbstring(collKey)
							 + " AND id = " + dbconn.dbstring(dbid));
					dbfilename = fname;
				}
			}
			if (errornum != -1) {
				// logit ("\tstoreResults about to INSERT into idmapMessages for id: " + dbid);
				dbconn.updateDb("INSERT INTO idmapMessages"
						 + " (collKey, id, recCheckDate, msgType, fileName, "
						 + " xpath, urllabel, url, msg, auxinfo)"
						 + " VALUES ( "
						 + dbconn.dbstringcom(collKey)
				// collkey
						 + dbconn.dbstringcom(dbid)
				// id
						 + dbconn.dbstringcom(new Timestamp(curdate))
				// recCheckDate
						 + dbconn.dbstringcom(DpcErrors.getMessage(
				// msgtype
						errornum))
						 + dbconn.dbstringcom(dbfilename)
				// fileName
						 + dbconn.dbstringcom((String) null)
				// xpath
						 + dbconn.dbstringcom((String) null)
				// urllabel
						 + dbconn.dbstringcom((String) null)
				// url
						 + dbconn.dbstringcom((String) null)
				// msg
						 + dbconn.dbstring((String) null)
				// auxinfo
						 + ")");
				totalwarnings++;
				forceEmail = true;
			}
		}
		// for ii

		// For each ResourceDesc, check for duplicates

		String exc = propsdoc.getXmlString("general/dupexclusionFile");
		if (exc.equals("null")) {
			exc = null;
		}

		logit ("looping through ResourceDescs looking for dups");
		for (irsd = 0; irsd < rsds.length; irsd++) {
			ResourceDesc basersd = rsds[irsd];

			long basechecksum = basersd.getPrimaryChecksum();

			if (basersd.getDuplicateRsd() == null) {
				// n^2 search is not optimal.  But since n is small
				// and time isn't critical, it'll work here.
				long numdups = 0;
				for (jrsd = irsd + 1; jrsd < rsds.length; jrsd++) {
					ResourceDesc testrsd = rsds[jrsd];
					long testchecksum = testrsd.getPrimaryChecksum();

					if ((basechecksum == 0) && (testchecksum == 0) && (basersd.getPrimaryUrl() == null) && (testrsd.getPrimaryUrl() == null)) {
						;
					} else {
						CatchDup c = new CatchDup(basechecksum, basersd.getPrimaryUrl(), basersd.getId(), testchecksum, testrsd.getPrimaryUrl(), testrsd.getId(), exc);
						boolean isdup = c.isDup();
						if (isdup == true) {

							numdups++;
						}
					}
				}
				if (numdups > 0) {
					String idstg = "";
					for (jrsd = irsd + 1; jrsd < rsds.length; jrsd++) {
						ResourceDesc testrsd = rsds[jrsd];
						long testchecksum = testrsd.getPrimaryChecksum();
						CatchDup c = new CatchDup(basechecksum, basersd.getPrimaryUrl(), basersd.getId(), testchecksum, testrsd.getPrimaryUrl(), testrsd.getId(), exc);
						boolean isdup = c.isDup();
						if (isdup == true) {
							idstg += "  id: " + testrsd.getId()
									 + "  file: " + testrsd.getFileName()
									 + "  URL: " + testrsd.getPrimaryUrl() + "\n";
							testrsd.addWarning(new Warning(
									DpcErrors.IDMAP_DUP,
									testrsd.getId(),
									testrsd.getFileName(),
									testrsd.getPrimaryXpath(),
									testrsd.getPrimaryUrllabel(),
									testrsd.getPrimaryUrl(),
									"This rec has " + numdups + " duplicates",
									"See " + basersd.getId() + " for details"));
							totalwarnings++;
							testrsd.setDuplicateRsd(basersd);
						}
					}
					basersd.addWarning(new Warning(
							DpcErrors.IDMAP_DUP,
							basersd.getId(),
							basersd.getFileName(),
							basersd.getPrimaryXpath(),
							basersd.getPrimaryUrllabel(),
							basersd.getPrimaryUrl(),
							"This rec has " + numdups + " duplicates",
							"The duplicates are:\n" + idstg));
					totalwarnings++;
				}
			}
		}

		// For each ResourceDesc, check and store its results
		logit ("looping through ResourceDescs storing results");
		touchDbconn ();
		for (irsd = 0; irsd < rsds.length; irsd++) {
			try {
				ResourceDesc rsd = rsds[irsd];
				if (bugs >= 1) {
					prtln("result rsd: " + rsd);
				}

				if (bugs >= 20) {
					prtln("\n===============storeResults: rsd.id: " + rsd.getId());
					if (rsd.numpages == 0) {
						prtln("    rsd.numpages is 0");
					} else {
						prtln("    urlstg: \"" + rsd.pages[0].urlstg + "\"");
						prtln("    checksum: " + rsd.pages[0].pageChecksum);
						if (rsd.pages[0].pagesummary == null) {
							prtln("    pagesummary: null\n\n\n");
						} else {
							prtln("===== storeresults: pagesummary =====\n"
									 + rsd.pages[0].pagesummary
									 + "\n===== storeresults: end pagesummary =====\n\n");
						}
					}
				}

				totalwarnings += rsd.numWarnings();

				storeResultsSingle(rsd, collKey, metastyle, curdate,
						historyDays, ivitalCutoff);
			} catch (IdmapException iex) {
				prtln("storeResults: caught exc: " + iex);
				iex.printStackTrace();
			}

		}
		// for irsd

		if (bugs >= 10) {
			prtln("\n==========================\n"
					 + "storeResults: warnings summary: totalwarnings: "
					 + totalwarnings);
			for (irsd = 0; irsd < rsds.length; irsd++) {
				ResourceDesc rsd = rsds[irsd];
				prtln("" + irsd + ": " + rsd.getId()
						 + "  warns: " + rsd.numWarnings());
				if (rsd.numWarnings() > 0) {
					prtln("Warnings:\n" + rsd.warnBuf);
				}
			}
			prtln("\n==========================\n"
					 + "storeResults: end warnings summary\n");
		}
	}


	// storeResults


	/**
	 *  Stores the results in a ResourceDesc in the various DB tables.
	 *
	 * @param  rsd                 The ResourceDesc containing results to be saved.
	 * @param  collKey             The collection key.
	 * @param  metastyle           metadata style: one of the MS_ values defined in {@link
	 *      org.dlese.dpc.services.mmd.MmdRec MmdRec}.
	 * @param  curdate             The current data, in milliseconds since 1970.
	 * @param  historyDays         The numbers of past history we examine when computing a
	 *      site's vitality.
	 * @param  ivitalCutoff        The percent of the time a site must be fully fully
	 *      functional. If the site's vitality falls below ivitalCutoff, a warning message is
	 *      sent.
	 * @exception  IdmapException  DESCRIPTION
	 * @exception  MmdException    DESCRIPTION
	 */

	void storeResultsSingle(
			ResourceDesc rsd,
			String collKey,
			String metastyle,
			long curdate,
			int historyDays,
	// num days past history we examine.
			int ivitalCutoff)
	// % vitality below which we issue warning
			 throws IdmapException, MmdException {
		int ipage;
		int ii;

		// logit ("*storeResultsSingle()* for id: " + rsd.getId());
		
		Object[][] dbmat = null;
		double vitscale = 100;
		int ivitality;

		// Copy warning messages from separate pages into rsd.warnBuf
		for (ii = 0; ii < rsd.numpages; ii++) {
			PageDesc page = rsd.pages[ii];
			if (page.pagewarning != null) {
				rsd.addWarning(page.pagewarning);
			}
		}

		// Add PrimaryContent of the Primary URL Page into the ResourceDesc
		if (rsd.testPrimary() == true) {
			rsd.PrimaryContent = rsd.pages[0].PrimaryContent;
			rsd.primarycontentType = rsd.pages[0].primarycontentType;
		}

		// Check that mirror pages have the same checksum as the primary

		if ((!rsd.hasSevereError())
				 && rsd.numpages > 0
				 && rsd.pages[0].pagewarning == null
				 && rsd.pages[0].urllabel.equals("primary-url")
				 && rsd.pages[0].pageChecksum != 0) {
			for (ii = 1; ii < rsd.numpages; ii++) {
				if (rsd.pages[ii].urllabel.equals("mirror-url")
						 && rsd.pages[ii].pagewarning == null
						 && rsd.pages[ii].pageChecksum != rsd.pages[0].pageChecksum) {
					rsd.addWarning(new Warning(
							DpcErrors.IDMAP_MIRROR_DIFFERS,
							rsd.getId(),
							rsd.getFileName(),
							rsd.pages[0].xpath,
							rsd.pages[0].urllabel,
							rsd.pages[0].urlstg,
							"mirror urls",
							rsd.pages[ii].urlstg));
				}
			}
		}

		// Get existing DB rec.
		// Create the idmapMmd entry if it doesn't exist.
		// If it does exist:
		//    - if checksum changed, issue warning

		// check for columns primaryContent and primarycontentType

		try {
			// logit ("about to check for columns primaryContent and primarycontentType");
			dbmat = dbconn.getDbTable(
					"SELECT fileName, primaryUrl, status, firstAccessionDate,"
					 + " metaChecksum, primaryChecksum, primaryContent, primarycontentType"
					 + " FROM idmapMmd"
					 + " WHERE collKey = " + dbconn.dbstring(collKey)
					 + " AND id = " + dbconn.dbstring(rsd.getId()),
					new String[]{"string", "string", "string", "date",
					"long", "long", "string", "string"},
			// types
					true);
			// allow primaryUrl to be null
		} catch (Throwable t) {
			logit ("caught error trying to select " + rsd.getId() + " from idmapMmd: " + t.getMessage());
		}

		if (dbmat == null) {
			logit (" ... dbmat is NULL - trying same select again ..");
			dbmat = dbconn.getDbTable(
					"SELECT fileName, primaryUrl, status, firstAccessionDate,"
					 + " metaChecksum, primaryChecksum"
					 + " FROM idmapMmd"
					 + " WHERE collKey = " + dbconn.dbstring(collKey)
					 + " AND id = " + dbconn.dbstring(rsd.getId()),
					new String[]{"string", "string", "string", "date",
					"long", "long"},
			// types
					true);
			// allow primaryUrl to be null
			if (dbmat == null)
				logit ("...... dbmat is null after second try");
			else
				logit ("...... dbmat has " + dbmat.length + " records after second try");
		}

		if (dbmat.length == 0) {
			// if rec is new...
			forceEmail = true;
			rsd.addWarning(new Warning(DpcErrors.IDMAP_NEW_XML_FILE,
					rsd.getId(),
					rsd.getFileName(),
					rsd.getPrimaryXpath(),
					rsd.getPrimaryUrllabel(),
					rsd.getPrimaryUrl(),
					null, null));

			try {
				// logit ("attempting to insert into idmapMmd");
				dbconn.updateDb("INSERT INTO idmapMmd"
						 + " (collKey, id, hasFile, fileName, primaryUrl, status,"
						 + " firstAccessiondate, lastMetaModDate, recCheckDate,"
						 + " metaChecksum, primaryChecksum, primaryContent, primarycontentType)"
						 + " VALUES ( "
						 + dbconn.dbstringcom(rsd.getCollKey())
						 + dbconn.dbstringcom(rsd.getId())
						 + dbconn.dbstringcom(1)
				// hasFile
						 + dbconn.dbstringcom(rsd.getFileName())
						 + dbconn.dbstringcom(rsd.getPrimaryUrl())
						 + dbconn.dbstringcom(MmdRec.statusNames[
						MmdRec.STATUS_ACCESSIONED_DISCOVERABLE])
						 + dbconn.dbstringcom(new Timestamp(curdate))
				// firstAccession
						 + dbconn.dbstringcom(new Timestamp(curdate))
				// lastMetaModDate
						 + dbconn.dbstringcom(new Timestamp(curdate))
				// recCheckDate
						 + dbconn.dbstringcom(rsd.getMetaChecksum())
						 + dbconn.dbstringcom(rsd.getPrimaryChecksum())
						 + dbconn.dbstringforcecom(rsd.PrimaryContent)
						 + dbconn.dbstring(rsd.primarycontentType)
						 + ")");
			} catch (Throwable t) {
				try {
					logit ("caught error trying to insert " + rsd.getId() + " into idmapMmd: " + t.getMessage());
					logit (" ... reopening connection");
					closeDb();
					openDB(propsdoc);
				} catch (XMLException e) {
					logit ("...  caught error reopening!: " + e.getMessage());
				}
				logit ("second try to insert ...");
				try {
					dbconn.updateDb("INSERT INTO idmapMmd"
							 + " (collKey, id, hasFile, fileName, primaryUrl, status,"
							 + " firstAccessiondate, lastMetaModDate, recCheckDate,"
							 + " metaChecksum, primaryChecksum)"
							 + " VALUES ( "
							 + dbconn.dbstringcom(rsd.getCollKey())
							 + dbconn.dbstringcom(rsd.getId())
							 + dbconn.dbstringcom(1)
					// hasFile
							 + dbconn.dbstringcom(rsd.getFileName())
							 + dbconn.dbstringcom(rsd.getPrimaryUrl())
							 + dbconn.dbstringcom(MmdRec.statusNames[
							MmdRec.STATUS_ACCESSIONED_DISCOVERABLE])
							 + dbconn.dbstringcom(new Timestamp(curdate))
					// firstAccession
							 + dbconn.dbstringcom(new Timestamp(curdate))
					// lastMetaModDate
							 + dbconn.dbstringcom(new Timestamp(curdate))
					// recCheckDate
							 + dbconn.dbstringcom(rsd.getMetaChecksum())
							 + dbconn.dbstring(rsd.getPrimaryChecksum())
							 + ")");
	
				} catch (Throwable tt) {
					prtln ("caught error on second try\n" + tt.getMessage() + "\n continuing ...");
				}
			}				

		}
		// if rec is new
		// else idmapMmd rec exists
		else {
			String oldFilename = (String) dbmat[0][0];
			String oldPrimaryUrl = (String) dbmat[0][1];
			String oldStatus = (String) dbmat[0][2];
			Long firstaccessiondate = (Long) dbmat[0][3];
			long oldMetaChecksum = ((Long) dbmat[0][4]).longValue();
			long oldPrimaryChecksum = ((Long) dbmat[0][5]).longValue();

			// If fileName has changed, update it
			if (!rsd.getFileName().equals(oldFilename)) {
				rsd.addWarning(new Warning(
						DpcErrors.IDMAP_CHANGE_FILENAME,
						rsd.getId(),
						rsd.getFileName(),
						rsd.getPrimaryXpath(),
						rsd.getPrimaryUrllabel(),
						rsd.getPrimaryUrl(),
						"Old file name:",
						oldFilename));
				dbconn.updateDb("UPDATE idmapMmd SET fileName = "
						 + dbconn.dbstring(rsd.getFileName())
						 + " WHERE collKey = " + dbconn.dbstring(collKey)
						 + " AND id = " + dbconn.dbstring(rsd.getId()));
			}

			// If metaChecksum has changed, update it
			if (rsd.getMetaChecksum() != oldMetaChecksum) {
				rsd.addWarning(new Warning(
						DpcErrors.IDMAP_CHANGE_MMD,
						rsd.getId(),
						rsd.getFileName(),
						rsd.getPrimaryXpath(),
						rsd.getPrimaryUrllabel(),
						rsd.getPrimaryUrl(),
						"Old, new mmd checksums:",
						"" + oldMetaChecksum + " " + rsd.getMetaChecksum()));
				dbconn.updateDb("UPDATE idmapMmd SET metaChecksum = "
						 + dbconn.dbstring(rsd.getMetaChecksum())
						 + " WHERE collKey = " + dbconn.dbstring(collKey)
						 + " AND id = " + dbconn.dbstring(rsd.getId()));
			}

			// Always update recCheckDate
			dbconn.updateDb("UPDATE idmapMmd SET recCheckDate = "
					 + dbconn.dbstring(new Timestamp(curdate))
					 + " WHERE collKey = " + dbconn.dbstring(collKey)
					 + " AND id = " + dbconn.dbstring(rsd.getId()));
			
			try {
				//Always update primaryContent
				dbconn.updateDb("UPDATE idmapMmd SET primaryContent = "
						 + dbconn.dbstringforce(rsd.PrimaryContent)
						 + " WHERE collKey = " + dbconn.dbstring(collKey)
						 + " AND id = " + dbconn.dbstring(rsd.getId()));

				//Always update primarycontentType
				dbconn.updateDb("UPDATE idmapMmd SET primarycontentType = "
						 + dbconn.dbstringforce(rsd.primarycontentType)
						 + " WHERE collKey = " + dbconn.dbstring(collKey)
						 + " AND id = " + dbconn.dbstring(rsd.getId()));
			} catch (Throwable t) {
				logit ("Caught exception trying to update primary Content for " + rsd.getId());
				
				// most/all exceptions are because PrimaryContent is too large. In this case,
				// the value written to db is truncated.
				if (rsd.PrimaryContent == null)
					logit (rsd.getId() + " primaryContent is: NULL");
				else
					logit (rsd.getId() + " primaryContent size: " + rsd.PrimaryContent.length());
				
				// System.out.println("Exception !" + t);
				try {
					closeDb();
					openDB(propsdoc);
				} catch (XMLException e) {}
			}

			// metastyle "adn" checks ...

			// If there is no primary URL, or it's invalid,
			// but there are other URLs, we don't want to use one
			// of the others as the primary.
			if (metastyle.equals(MmdRec.metastyleNames[MmdRec.MS_ADN])
					 && rsd.numpages > 0
					 && rsd.pages[0].urllabel != null
					 && rsd.pages[0].urllabel.equals("primary-url")) {
				if (!rsd.pages[0].urllabel.equals("primary-url")) {
					mkerror("storeResultsSingle: adn first page not primary."
							 + "  id: " + rsd.getId());
				}

				// If url has changed, update it
				if (oldPrimaryUrl == null && rsd.pages[0].urlstg != null
						 || oldPrimaryUrl != null
						 && !rsd.pages[0].urlstg.equals(oldPrimaryUrl)) {
					rsd.addWarning(new Warning(
							DpcErrors.IDMAP_CHANGE_PRIMARY_URL,
							rsd.getId(),
							rsd.getFileName(),
							rsd.getPrimaryXpath(),
							rsd.getPrimaryUrllabel(),
							rsd.getPrimaryUrl(),
							"old primary url:",
							oldPrimaryUrl));
					dbconn.updateDb("UPDATE idmapMmd SET primaryurl = "
							 + dbconn.dbstring(rsd.getPrimaryUrl())
							 + " WHERE collKey = " + dbconn.dbstring(collKey)
							 + " AND id = " + dbconn.dbstring(rsd.getId()));
				}

				// If primary checksum has changed, update it.
				long newPrimaryChecksum = rsd.getPrimaryChecksum();

				if (bugs >= 10) {
					prtln("storeResults: rsd.id: " + rsd.getId()
							 + "  oldPrimaryChecksum: " + oldPrimaryChecksum
							 + "  newPrimaryChecksum: " + newPrimaryChecksum);
				}

				// Skip this warning, because it happens so
				// frequently to sites updating news or trivia.
				//if (oldChecksum != 0 && newChecksum != 0
				//	&& newChecksum != oldChecksum) {
				//	rsd.addWarning( new Warning(
				//		DpcErrors.IDMAP_CONTENT_CHANGED, rsd.getId(),
				//		rsd.getFileName(),
				//		null, rsd.getPrimaryUrl(), null));
				//}

				/*
				 *  IMPORTANT : Removing the newPrimaryChecksum !=0 condition
				 *  * if (newPrimaryChecksum != 0
				 *  && newPrimaryChecksum != oldPrimaryChecksum)
				 */
				if (newPrimaryChecksum != oldPrimaryChecksum) {
					dbconn.updateDb("UPDATE idmapMmd SET primaryChecksum = "
							 + dbconn.dbstring(newPrimaryChecksum)
							 + " WHERE collKey = " + dbconn.dbstring(collKey)
							 + " AND id = " + dbconn.dbstring(rsd.getId()));
				}
			}
			// if "adn"

		}
		// else idmapMmd rec exists

		// Handle vitality.
		// vitality = avg of all stats within the last  historyDays.
		// Add in the current result by hand.

		// logit ("handling vitality for " + rsd.getId());
		
		long prevdate = curdate - 1000 * 60 * 60 * 24 * historyDays;

		for (ipage = 0; ipage < rsd.numpages; ipage++) {
			PageDesc page = rsd.pages[ipage];

			dbmat = dbconn.getDbTable(
					"SELECT lastMsgType, lastDateUp, lastDateDown FROM idmapVitality"
					 + " WHERE collKey = " + dbconn.dbstring(collKey)
					 + " AND id = " + dbconn.dbstring(rsd.getId())
					 + " AND url = " + dbconn.dbstring(page.urlstg)
					 + " AND recCheckDate >= "
					 + dbconn.dbstring(new Timestamp(prevdate))
					 + " ORDER BY recCheckDate",
					new String[]{"string", "date", "date"},
			// types
					true);
			// allow nulls in fields

			// Get previous history values from the last (most recent) row
			long prevLastDateUp = 0;
			long prevLastDateDown = 0;
			if (dbmat.length > 0) {
				prevLastDateUp = ((Long) dbmat[dbmat.length - 1][1]).longValue();
				prevLastDateDown = ((Long) dbmat[dbmat.length - 1][2]).longValue();
			}

			int numok = 0;
			for (ii = 0; ii < dbmat.length; ii++) {
				if (dbmat[ii][0] == null) {
					numok++;
				}
				// if lastMsgType is null
			}
			if (page.pagewarning == null) {
				numok++;
			}
			// add in current page
			int totalnum = dbmat.length + 1;
			// +1 for current page

			double vitality = numok / (double) totalnum;
			ivitality = (int) (vitscale * vitality);
			// convert to percent

			long lastDateUp;
			// convert to percent

			long lastDateDown;
			if (page.pagewarning != null) {
				lastDateUp = prevLastDateUp;
				lastDateDown = curdate;
			} else {
				lastDateUp = curdate;
				lastDateDown = prevLastDateDown;
			}

			String lastMsgType = null;
			if (page.pagewarning != null) {
				lastMsgType = DpcErrors.getMessage(page.pagewarning.msgType);
			}
			
			// logit ("about to insert into idmapVitality (" + rsd.getId() + ")");
			try {
				dbconn.updateDb("INSERT INTO idmapVitality"
						 + " (collKey, id, recCheckDate, fileName, xpath, urllabel, url,"
						 + " lastMsgType, vitality, lastDateUp, lastDateDown)"
						 + " VALUES ( "
						 + dbconn.dbstringcom(rsd.getCollKey())
						 + dbconn.dbstringcom(rsd.getId())
						 + dbconn.dbstringcom(new Timestamp(curdate))
				// recCheckDate
						 + dbconn.dbstringcom(rsd.getFileName())
						 + dbconn.dbstringcom(page.xpath)
						 + dbconn.dbstringcom(page.urllabel)
						 + dbconn.dbstringcom(page.urlstg)
						 + dbconn.dbstringcom(lastMsgType)
						 + dbconn.dbstringcom(ivitality)
						 + dbconn.dbstringcom(new Timestamp(lastDateUp))
						 + dbconn.dbstring(new Timestamp(lastDateDown))
						 + ")");
			} catch (MmdException e) {
				logit ("caught error trying to insert " + rsd.getId() + " into idmapVitality: " + e.getMessage());
				logit ("  ... chances are the url is too long: \"" + page.urlstg + "\"");
			}
					 
			if (page.pagewarning != null && ivitality < ivitalCutoff) {
				double downdays;
				if (lastDateUp == 0) {
					downdays = -1;
				} else {
					downdays = (curdate - lastDateUp)
							 / (double) (1000 * 60 * 60 * 24);
				}

				rsd.addWarning(new Warning(
						DpcErrors.IDMAP_VITALITY,
						rsd.getId(),
						rsd.getFileName(),
						page.xpath,
						page.urllabel,
						page.urlstg,
						"URL was up " + ivitality + " % over the last "
						 + historyDays + " days",
				// msg
						(page.pagewarning == null) ? null :
				// auxinfo
						DpcErrors.getMessage(page.pagewarning.msgType)));

			}
		}
		// for ipage

		if (rsd.numWarnings() > 0) {
			Iterator iter = rsd.warningIterator();
			while (iter.hasNext()) {
				Warning wng = (Warning) iter.next();
				dbconn.updateDb("INSERT INTO idmapMessages"
						 + " (collKey, id, recCheckDate, msgType, fileName,"
						 + " xpath, urllabel, url, msg, auxinfo)"
						 + " VALUES ( "
						 + dbconn.dbstringcom(collKey)
						 + dbconn.dbstringcom(wng.id)
						 + dbconn.dbstringcom(new Timestamp(curdate))
				// recCheckDate
						 + dbconn.dbstringcom(DpcErrors.getMessage(wng.msgType))
						 + dbconn.dbstringcom(rsd.getFileName())
						 + dbconn.dbstringcom(wng.xpath)
						 + dbconn.dbstringcom(wng.urllabel)
						 + dbconn.dbstringcom(wng.url)
						 + dbconn.dbstringforcecom(wng.msg)
						 + dbconn.dbstringforce(wng.auxinfo)
						 + ")");
			}
		}
	}


	// storeResultsSingle


	/**
	 *  Returns all the file names in a dir that end in ".xml" and have been modified since
	 *  prevmoddate.
	 *
	 * @param  prevmoddate         The previous date, expressed as milliseconds since 1970.
	 * @param  dirPath             The path of the directory containing collKey's XML files.
	 * @return                     The dirFiles value
	 * @exception  IdmapException  DESCRIPTION
	 */

	File[] getDirFiles(
			final long prevmoddate,
			File dirPath)
			 throws IdmapException {
		File[] resfiles = dirPath.listFiles(
			new FileFilter() {
				public boolean accept(File file) {
					if (file.isFile()
							 && file.getPath().endsWith(".xml")
							 && (file.lastModified() >= prevmoddate)) {
						return true;
					} else {
						return false;
					}
				}
			});
		if (resfiles == null) {
			resfiles = new File[0];
		}
		Arrays.sort(resfiles);
		return resfiles;
	}



	/**
	 *  If it's time to send email, does so.
	 *
	 * @param  bugs                debug level
	 * @param  collKey             The collection key.
	 * @param  metastyle           metadata style: one of the MS_ values defined in {@link
	 *      org.dlese.dpc.services.mmd.MmdRec MmdRec}.
	 * @param  dirPath             The directory containing the XML metadata files.
	 * @param  numRecords          The number of records that were checked
	 * @param  curdate             The current data, in milliseconds since 1970.
	 * @param  emailDays           bit vec of days of the week to send email.
	 * @param  emailHost           host name of the email server
	 * @param  emailFrom           "from" name to be used in email messages
	 * @param  emailType           email cmd line parm: EMAIL_NORMAL, EMAIL_FORCE,
	 *      EMAIL_PRINT, or EMAIL_SINGLE
	 * @param  emailAddrs          array of "to" names for email messages
	 * @param  emailSubject        initial part of the email "Subject" line
	 * @return                     true if we sent email
	 * @exception  IdmapException  DESCRIPTION
	 * @exception  MmdException    DESCRIPTION
	 */

	boolean testEmailTime(
			int bugs,
			String collKey,
			String metastyle,
			File dirPath,
			int numRecords,
			long curdate,
			boolean[] emailDays,
			String emailHost,
			String emailFrom,
			int emailType,
			String[] emailAddrs,
			String emailSubject)
			 throws IdmapException, MmdException {
		int ii;
		Object[][] dbmat;

		// If this is a "send email day" and we haven't sent email
		// today, send it.
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(curdate);
		int javaday = cal.get(Calendar.DAY_OF_WEEK);
		// Frickin Java ... why couldn't they just return an int
		// instead of having to use defined constants ...
		int iday = -1;
		if (javaday == Calendar.SUNDAY) {
			iday = 0;
		} else if (javaday == Calendar.MONDAY) {
			iday = 1;
		} else if (javaday == Calendar.TUESDAY) {
			iday = 2;
		} else if (javaday == Calendar.WEDNESDAY) {
			iday = 3;
		} else if (javaday == Calendar.THURSDAY) {
			iday = 4;
		} else if (javaday == Calendar.FRIDAY) {
			iday = 5;
		} else if (javaday == Calendar.SATURDAY) {
			iday = 6;
		} else {
			mkerror("testEmailTime: invalid day of week: " + javaday);
		}

		boolean sentEmail = emailDays[iday];

		// Set prevdate = last date we sent email.
		Object[][] datemat = dbconn.getDbTable(
				"SELECT lastEmailDate FROM idmapCollection"
				 + " WHERE collKey = " + dbconn.dbstring(collKey),
				new String[]{"date"},
		// types
				true);
		// allow nulls
		long prevdate;
		if (datemat == null || datemat.length == 0 || datemat[0][0] == null) {
			prevdate = 0;
		} else {
			prevdate = ((Long) datemat[0][0]).longValue();
		}

		// If we never sent email, send it now.
		if (prevdate == 0) {
			sentEmail = true;
		}

		// If we already sent email today, set sentEmail = false.
		if (prevdate > 0 && sentEmail) {
			Calendar prevcal = Calendar.getInstance();
			Calendar curcal = Calendar.getInstance();
			prevcal.setTimeInMillis(prevdate);
			curcal.setTimeInMillis(curdate);
			if (prevcal.get(Calendar.YEAR) == curcal.get(Calendar.YEAR)
					 && prevcal.get(Calendar.MONTH) == curcal.get(Calendar.MONTH)
					 && prevcal.get(Calendar.DAY_OF_MONTH)
					 == curcal.get(Calendar.DAY_OF_MONTH)) {
				sentEmail = false;
			}
		}

		if (forceEmail
				 || emailType == EMAIL_FORCE
				 || emailType == EMAIL_SINGLE
				 || emailType == EMAIL_PRINT) {
			sentEmail = true;
		}

		// However, if there's nobody to send it to, don't send it.
		if (emailType != EMAIL_PRINT
				 && (emailAddrs == null || emailAddrs.length == 0)) {
			sentEmail = false;
		}

		// Create big email message and either send or print it.
		if (sentEmail) {

			StringBuffer msgbuf = new StringBuffer();
			// Extract the warnings and send the email
			dbmat = dbconn.getDbTable(
					"SELECT id, msgType, fileName, xpath, urllabel, url, msg, auxinfo"
					 + " FROM idmapMessages"
					 + " WHERE collKey = " + dbconn.dbstring(collKey)
					 + " AND recCheckDate = "
					 + dbconn.dbstring(new Timestamp(curdate)),
					new String[]{"string", "string", "string", "string",
					"string", "string", "string", "string"},
					true);
			// allow nulls in fields

			// Make array of warnings so we can sort them by msgtype
			Warning[] warns = new Warning[dbmat.length];
			for (ii = 0; ii < dbmat.length; ii++) {
				String id = (String) dbmat[ii][0];
				String msgtypestg = (String) dbmat[ii][1];
				String filename = (String) dbmat[ii][2];
				String xpath = (String) dbmat[ii][3];
				String urllabel = (String) dbmat[ii][4];
				String url = (String) dbmat[ii][5];
				String msg = (String) dbmat[ii][6];
				String auxinfo = (String) dbmat[ii][7];

				int msgtype = DpcErrors.getType(msgtypestg);
				if (msgtype < 0) {
					mkerror("invalid msgtypestg: \"" + msgtypestg + "\"");
				}
				warns[ii] = new Warning(msgtype, id, filename,
						xpath, urllabel, url, msg, auxinfo);
			}

			Arrays.sort(warns,
				new Comparator() {
					public int compare(Object obja, Object objb) {
						int ires;
						Warning warna = (Warning) obja;
						Warning warnb = (Warning) objb;
						if (warna.msgType < warnb.msgType) {
							ires = -1;
						} else if (warna.msgType > warnb.msgType) {
							ires = 1;
						} else {
							ires = warna.id.compareTo(warnb.id);
						}
						return ires;
					}
				});

			for (ii = 0; ii < warns.length; ii++) {
				if (bugs >= 1) {
					prtln("testEmailTime: warning " + ii
							 + ":  id: \"" + warns[ii].id + "\""
							 + "  msgType: " + DpcErrors.getMessage(warns[ii].msgType)
							 + "  filename: \"" + warns[ii].filename + "\""
							 + "  xpath: \"" + warns[ii].xpath + "\""
							 + "  urllabel: \"" + warns[ii].urllabel + "\""
							 + "  url: \"" + warns[ii].url + "\""
							 + "  msg: \"" + warns[ii].msg + "\""
							 + "  auxinfo: \"" + warns[ii].auxinfo + "\"");
				}

				// Don't report timeout errors, except
				// as vitality errors when they accumulate.
				////if (warns[ii].msgtype != DpcErrors.IDMAP_COMM_TIMEOUT) ...

				if (ii == 0 || warns[ii - 1].msgType != warns[ii].msgType) {
					msgbuf.append("\n==========\n\n");
					msgbuf.append("The following ids have \""
							 + DpcErrors.getMessage(warns[ii].msgType)
							 + "\" errors.\n\n");
				}

				String msg = "" + (ii + 1)
				// change origin 0 to origin 1
						 + ": " + warns[ii].id;
				String fname = warns[ii].filename;
				if (fname != null) {
					msg += "    file: " + fname;
				}
				msg += "\n";

				// Use short form for IDMAP_NEW_XML_FILE
				// ("New XML file found") messages.
				if (warns[ii].msgType != DpcErrors.IDMAP_NEW_XML_FILE) {
					msg += "    xpath: \"" + warns[ii].xpath + "\"";
					msg += "\n";
					msg += "    url: \"" + warns[ii].url + "\"";
					msg += "\n";
					msg += "    url label: \"" + warns[ii].urllabel + "\"";
					if (warns[ii].msg != null) {
						msg += "    msg: \"" + warns[ii].msg + "\"";
					}
					msg += "\n";
					if (warns[ii].auxinfo != null) {
						msg += "    info: \"" + warns[ii].auxinfo + "\"\n";
					}
				}
				msgbuf.append(msg);

			}
			int totalwarnings = warns.length;

			String subj = emailSubject + ", " + collKey
					 + ", " + totalwarnings + " warnings";

			// Insert heading before main output
			String heading = subj + "\n";
			if (emailAddrs != null) {
				for (ii = 0; ii < emailAddrs.length; ii++) {
					heading += "Recipient: \"" + emailAddrs[ii] + "\"\n";
				}
			}
			heading += "Time: " + new Timestamp(curdate) + "\n"
					 + "Directory path: " + dirPath.getAbsolutePath() + "\n"
					 + "Number of records checked: " + numRecords + "\n"
					 + "\n";
			msgbuf.insert(0, heading);

			// If emailParm == "print", print all to stdout
			if (bugs >= 1 || emailType == EMAIL_PRINT) {
				prtln("\n");
				prtln("Printed version:");
				prtln("\n");
				prtln(msgbuf.toString());
				prtln("\n");
			}

			if (emailType != EMAIL_PRINT) {
				try {
					sendemail(emailHost, emailFrom, emailAddrs,
							subj, msgbuf.toString());
				} catch (Exception exc) {
					mkerror("Idmap.const: cannot send email.  exc: " + exc
							 + "\n\nsubj: " + subj
							 + "\nmsgbuf:\n" + msgbuf);
				}
			}
		}
		// if sentEmail

		return sentEmail;
	}


	// testEmailTime

	/**
	 *  Sends an email message.
	 *
	 * @param  emailHost                    host name of the email server
	 * @param  emailFrom                    "from" name to be used in email messages
	 * @param  emailAddrs                   array of "to" names for email messages
	 * @param  emailSubject                 initial part of the email "Subject" line
	 * @param  msgtext                      The message text.
	 * @exception  MessagingException       DESCRIPTION
	 * @exception  NoSuchProviderException  DESCRIPTION
	 * @exception  IOException              DESCRIPTION
	 */

	void sendemail(
			String emailHost,
			String emailFrom,
			String[] emailAddrs,
			String emailSubject,
			String msgtext)
			 throws MessagingException, NoSuchProviderException, IOException {
		int ii;

		// Convert strange chars to %xx hex encoding.
		// Java's MimeMessage will automatically convert the ENTIRE
		// message to some strange encoding if the message
		// contains a single weird char.

		StringBuffer finalBuf = new StringBuffer();
		for (ii = 0; ii < msgtext.length(); ii++) {
			char cc = msgtext.charAt(ii);
			if ((cc >= 0x20 && cc < 0x7f)
					 || cc == '\t'
					 || cc == '\n'
					 || cc == '\f'
					 || cc == '\r') {
				finalBuf.append(cc);
			} else {
				String hexstg = Integer.toHexString(cc);
				if (hexstg.length() == 1) {
					hexstg = '0' + hexstg;
				}
				finalBuf.append('%');
				finalBuf.append(hexstg.toUpperCase());
			}
		}

		boolean debug = false;
		Properties props = new Properties();
		if (debug) {
			props.put("mail.debug", "true");
		}
		props.put("mail.smtp.host", emailHost);
		Session session = Session.getInstance(props, null);
		session.setDebug(debug);

		MimeMessage msg = new MimeMessage(session);
		msg.setFrom(new InternetAddress(emailFrom));

		InternetAddress[] recips = new InternetAddress[emailAddrs.length];
		for (ii = 0; ii < emailAddrs.length; ii++) {
			recips[ii] = new InternetAddress(emailAddrs[ii]);
		}
		msg.setRecipients(RecipientType.TO, recips);
		msg.setSubject(emailSubject);
		msg.setContent(finalBuf.toString(), "text/plain");
		Transport.send(msg);
	}


	// end sendemail

	/**
	 *  Prints statistics on memory and time use.
	 *
	 * @param  msg        An identifying message.
	 * @param  starttime  The time at which the current run started.
	 */

	static void prtmemorystats(String msg, long starttime) {
		System.gc();
		prtln("");
		prtln(msg);
		prtln("  elapsed time: "
				 + (0.001 * (System.currentTimeMillis() - starttime)) + " seconds");
		Runtime rt = Runtime.getRuntime();
		prtln("  memory stats:");
		prtln("  total: " + (rt.totalMemory() / (1024 * 1024)) + " MB");
		prtln("  max: " + (rt.maxMemory() / (1024 * 1024)) + " MB");
		prtln("  free: " + (rt.freeMemory() / (1024 * 1024)) + " MB");
		prtln("  used: "
				 + ((rt.totalMemory() - rt.freeMemory()) / (1024 * 1024)) + " MB");
	}



	/**
	 *  Simply throws an IdmapException.
	 *
	 * @param  msg                 DESCRIPTION
	 * @exception  IdmapException  DESCRIPTION
	 */

	static void mkerror(String msg)
			 throws IdmapException {
		throw new IdmapException(msg);
	}



	/**
	 *  Prints a String without a trailing newline.
	 *
	 * @param  msg  DESCRIPTION
	 */

	static void prtstg(String msg) {
		System.out.print(msg);
	}



	/**
	 *  Prints a String with a trailing newline.
	 *
	 * @param  msg  DESCRIPTION
	 */

	static void prtln(String msg) {
		System.out.println(msg);
	}

    static String getDateStamp () {
	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
	return sdf.format(new Date());
    }


	/**
	 *  Simple logger
	 *
	 * @param  msg  DESCRIPTION
	 */
	static void logit(String msg) {
	    System.out.println("\n" + getDateStamp() + " - " + msg);
	    System.out.flush();
	}

}
// end class Idmap


