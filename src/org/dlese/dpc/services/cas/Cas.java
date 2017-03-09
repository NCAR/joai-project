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
package org.dlese.dpc.services.cas;

import org.dlese.dpc.util.*;
import java.net.URL;
import java.net.HttpURLConnection;

import java.io.InputStream;
import java.io.IOException;
import java.io.*;

import java.util.List;
import java.util.Iterator;
import java.util.Properties;
import java.util.Calendar;

import org.dom4j.Node;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;

import java.sql.Timestamp;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.text.ParseException;

import org.dlese.dpc.services.mmd.DbConn;
import org.dlese.dpc.services.mmd.MmdException;

import org.dlese.dpc.xml.XMLDoc;
import org.dlese.dpc.xml.XMLException;

import javax.mail.Message;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.Session;
import javax.mail.Transport;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

/**
 *  Creator Acknowledgement System main interface
 *
 * @author     Sonal Bhushan
 * @created    October 02, 2004
 */
public class Cas {

	String dbUrl = null;
	String printableUrl = null;
	DbConn dbconn = null;
	int totalNum = 0;
	int totalEmails = 0;
	long curdate;
	XMLDoc propsdoc = null;
	String[] exclude_emails = null;
	String[] exclude_colls = null;
	boolean db_empty = false;
	boolean sendemails = false;


	/**
	 *  The main program for the Cas class
	 *
	 * @param  args                         The command line arguments which are 
	 * props = [properties file name] #Required
	 * send_emails = [true|false]
	 * @exception  CasException
	 * @exception  MmdException
	 * @exception  XMLException
	 * @exception  MessagingException
	 * @exception  IOException
	 * @exception  NoSuchProviderException
	 */
	public static void main(String[] args)
		 throws CasException, MmdException, MessagingException, IOException, NoSuchProviderException, XMLException {
		new Cas(args);
	}


	// end main

	/**
	 *  Constructor for the Cas object
	 *
	 * @param  args                       
	 * @exception  CasException
	 * @exception  MmdException
	 * @exception  XMLException
	 * @exception  MessagingException
	 * @exception  IOException
	 * @exception  NoSuchProviderException
	 */
	private Cas(String[] args)
		 throws CasException, MmdException, XMLException, MessagingException, IOException, NoSuchProviderException {

		// We get the date at the start and use it throughout,
		// so that all dates of the run are exactly the same.
		curdate = System.currentTimeMillis();

		//System.out.println("entering Cas:");
		String propsParm = null;
		String send_Emails = null;
		int iarg;
		for (iarg = 0; iarg < args.length; iarg++) {
			String arg = args[iarg];
			if (arg.startsWith("props=")) {
				propsParm = arg.substring(6);
			}
			if (arg.startsWith("send_emails=")) {
				send_Emails = arg.substring(12);
			}
		}

		// Read the properties file
		if (propsParm == null) {
			prtlnErr("props not specified");
		}

		String propsfile = propsParm;

		if (propsfile.indexOf("://") < 0) {
			if (propsfile.startsWith("/")) {
				propsfile = "file://" + propsfile;
			}
			else {
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
		//expandEntities

		//testRecEmail(); /* function to test the Rec object */

		/*
                 *  get list of email addresses to be xcluded, if any, from the propsdoc
                 */
		exclude_emails = propsdoc.getXmlFields(0, 0, "runparams/excludeemails/email");
		for (int k = 0; k < exclude_emails.length; k++) {
			prtln(exclude_emails[k]);
		}

		/*
                 *  get list of collections to be xcluded, if any, from the propsdoc
                 */
		exclude_colls = propsdoc.getXmlFields(0, 0, "runparams/excludecolls/coll");
		for (int k = 0; k < exclude_colls.length; k++) {
			prtln(exclude_colls[k]);
		}

		//checkforEmptyDB(); /* function which checks if the CAS database is empty */

		/*
                 *  should this run actually send emails or not?
                 */
		if (send_Emails.indexOf("true") > -1) {
			sendemails = true;
		}
		else {
			sendemails = false;
		}

		/*
                 *  get all the Records from the DDS
                 */
		getAllRecords();

		sendEmails();
		populateHistory();


	}


	// end constructor

	/**
	 *  Populates the MySQL database's history table with data about past emails.
	 *
	 * @exception  CasException  Description of the Exception
	 * @exception  MmdException  Description of the Exception
	 * @exception  XMLException  Description of the Exception
	 */
	void populateHistory()
		 throws CasException, MmdException, XMLException {

		openDB();
		Object[][] dbmat = null;
		dbmat = dbconn.getDbTable("SELECT recordID, collID, collLabel, xmlFormat, status, dateCASed, " +
			"dateLastEmailSent, checkDate, email_address, email_type , accessionDate FROM casMmd WHERE checkDate!=\"" + new Timestamp(curdate) + "\"",
			new String[]{"string", "string", "string", "string", "string", "date", "date", "date", "string", "string", "string"}, true);

		if (dbmat.length != 0) {

			//                       prtln("no of records whose checkdate is old is " + dbmat.length);

			for (int i = 0; i < dbmat.length; i++) {

				boolean excluded_collection = false;
				if ((exclude_colls != null) && (excluded_collection == false)) {
					for (int s = 0; s < exclude_colls.length; s++) {
						String c = (String) dbmat[i][1];
						if (c.equalsIgnoreCase(exclude_colls[s])) {
							excluded_collection = true;
							break;
						}
					}
				}

				boolean excluded_email = false;
				if ((exclude_emails != null) && (excluded_email == false)) {
					for (int s = 0; s < exclude_emails.length; s++) {
						String c = (String) dbmat[i][8];
						if (c.equalsIgnoreCase(exclude_emails[s])) {
							excluded_email = true;
							break;
						}
					}
				}

				if ((excluded_email == false) && (excluded_collection == false)) {

					Long accdate = (Long) dbmat[i][5];
					Long emaildate = (Long) dbmat[i][6];
					Long checkdate = (Long) dbmat[i][7];
					Timestamp acc = null;
					Timestamp email = null;
					Timestamp check = null;

					if (accdate != null) {
						long l = accdate.longValue();
						acc = new Timestamp(l);
					}
					if (emaildate != null) {
						long l = emaildate.longValue();
						email = new Timestamp(l);
					}
					if (checkdate != null) {
						long l = checkdate.longValue();
						check = new Timestamp(l);
					}

					if (emaildate != null) {
						dbconn.updateDb("INSERT INTO casHistory"
							 + " (recordID, collID, collLabel, xmlFormat,"
							 + " rec_status, dateCASed, dateLastEmailSent, lastcheckDate,"
							 + " email_address, email_type, dateHistory, status, accessionDate)"
							 + " VALUES ("
							 + dbconn.dbstringcom((String) dbmat[i][0])
							 + dbconn.dbstringcom((String) dbmat[i][1])
							 + dbconn.dbstringcom((String) dbmat[i][2])
							 + dbconn.dbstringcom((String) dbmat[i][3])
							 + dbconn.dbstringcom((String) dbmat[i][4])
							 + dbconn.dbstringcom(acc)
							 + dbconn.dbstringcom(email)
							 + dbconn.dbstringcom(check)
							 + dbconn.dbstringcom((String) dbmat[i][8])
							 + dbconn.dbstringcom((String) dbmat[i][9])
							 + dbconn.dbstringcom(new Timestamp(curdate))
							 + dbconn.dbstringcom("DELETED")
							 + dbconn.dbstring((String) dbmat[i][10])
							 + ")");
					}
					else {
						dbconn.updateDb("INSERT INTO casHistory"
							 + " (recordID, collID, collLabel, xmlFormat,"
							 + " rec_status, dateCASed, lastcheckDate,"
							 + " email_address, email_type, dateHistory, status, accessionDate)"
							 + " VALUES ("
							 + dbconn.dbstringcom((String) dbmat[i][0])
							 + dbconn.dbstringcom((String) dbmat[i][1])
							 + dbconn.dbstringcom((String) dbmat[i][2])
							 + dbconn.dbstringcom((String) dbmat[i][3])
							 + dbconn.dbstringcom((String) dbmat[i][4])
							 + dbconn.dbstringcom(acc)
							 + dbconn.dbstringcom(check)
							 + dbconn.dbstringcom((String) dbmat[i][8])
							 + dbconn.dbstringcom((String) dbmat[i][9])
							 + dbconn.dbstringcom(new Timestamp(curdate))
							 + dbconn.dbstringcom("DELETED")
							 + dbconn.dbstring((String) dbmat[i][10])
							 + ")");

					}

					String recID = (String) dbmat[i][0];
					String emailadd = (String) dbmat[i][8];
					dbconn.updateDb("DELETE FROM casMmd WHERE recordID = \"" + recID + "\" AND email_address = \"" +
						emailadd + "\"");
				}
			}
			dbconn.updateDb("DELETE FROM casMmd WHERE checkDate!=\"" + new Timestamp(curdate) + "\"");
		}

		closeDb();
		//System.out.println("exiting PopulateHistory:");

	}


	/**
	 * Checks if the MySQL database is empty or not. If it is , sets db_empty = true. 
	 *
	 * @exception  CasException  Description of the Exception
	 * @exception  MmdException  Description of the Exception
	 * @exception  XMLException  Description of the Exception
	 */
	void checkforEmptyDB()
		 throws CasException, MmdException, XMLException {


		openDB();
		Object[][] dbmat = null;

		dbmat = dbconn.getDbTable("SELECT recordID FROM "
			 + "casMmd",
			new String[]{"string"},
			true);

		if ((dbmat.length == 0) || (dbmat == null)) {
			db_empty = true;
		}

		closeDb();


	}


	/**  Test function to test the Rec object */
	void testRecEmail() {
		//System.out.println("entering testrecemail:");

		Rec testrec = new Rec();

		testrec.setId("1");
		testrec.setXMLFormat("adn");
		testrec.setAccessionStatus("accessioned and ready to go");
		testrec.setCollId("DCC 1");
		testrec.setCollLabel("DCC!");

		EmailAdd testemail = new EmailAdd("INST", "staff@dpc.ucar.edu");
		EmailAdd testemail2 = new EmailAdd("PERSON", "sonal@dpc.ucar.edu");
		testrec.addEmail(testemail);
		testrec.addEmail(testemail2);

		// print out the testrec
		System.out.println(testrec.toString());

		Rec rec2 = new Rec("2", "DWEL 1", "DWEL", "adn", "corrupted");
		System.out.println(rec2.toString());
		rec2.addEmail(testemail);
		System.out.println(rec2.toString());
		rec2.addEmail(testemail2);

		// print out the testrec
		System.out.println(rec2.toString());
		//System.out.println("exiting testrecemail:");

	}


	/**
	 *  Connects to the DDS via webservices, and extracts the records into a Dom4J document.
	 *
	 * @exception  CasException
	 * @exception  MmdException
	 * @exception  XMLException
	 */
	void getAllRecords()
		 throws CasException, MmdException, XMLException {

		//System.out.println("entering getAllRecords:");

		boolean firstrun = true;
		int totalResults = -1;
		int s = 0;
		// offset
		int n = 100;
		// number of records returned by the webservice at each call
		Rec[] records = null;

		do {
			String request = "http://www.dlese.org/dds/services/ddsws1-0?verb=Search&q=ky:0*&xmlFormat=adn-localized&s=" + s + "&n=" + n + "&client=ddsws-explorer";
			try {
				Document document = null;
				URL url = new URL(request);
				int timeOutPeriod = 180000;
				InputStream istm = TimedURLConnection.getInputStream(url,timeOutPeriod);
				
				// Process the InputStream as desired. The InputStream may be used to
				// create an XML DOM or to convert the content to a String, for example.
				// For example, lets create a dom4j DOM document using the InputStream:
				try {
					SAXReader reader = new SAXReader();
					document = reader.read(istm);
				} catch (DocumentException e) {
					// Handle the Exception as desired...
					prtlnErr("Error! : " + e);
				}

				// Get the total number of records, if this is the first set of records returned by DDS
				if (firstrun == true) {
					firstrun = false;
					Node node = document.selectSingleNode("/DDSWebService/Search/resultInfo/totalNumResults");
					totalResults = Integer.parseInt(node.getText());
				}

				// Extract info like email addresses, record IDs, etc from the XML records, and store in DB
				extractRecordsInfo(document, records);

			} catch (URLConnectionTimedOutException exc) {
				// The URLConnection timed out...
				prtlnErr("URLConnection timed out while attempting to connect to " + request);
			} catch (IOException ioe) {
				// The URLConnection threw an IOException while attempting to connect...
				prtlnErr("URLConnection threw an IOException while attempting to connect to " + request);
			}

			prtln ("processed " + s + " through " + (s + n) + " of " + totalResults);
			
			s = s + n;
		} while (s < totalResults);
		
	}


	// end AllRecords

	/**
	 *  extract the records's creator info from a Dom4J document
	 *
	 * @param  document the Dom4j document containing the records
	 * @param  records           array of Records extracted from the DDS
	 * @exception  CasException
	 * @exception  MmdException
	 * @exception  XMLException
	 */
	void extractRecordsInfo(Document document, Rec[] records)
		 throws CasException, MmdException, XMLException {

		//System.out.println("entering extractRecordsInfo:");

		String recordid;
		String collid;
		String colllabel;
		String xmlformat;
		String accessionstatus;

		Node node = null;
		// get a List of Records
		List list = document.selectNodes("//record");
		int numrecords = list.size();
		totalNum += numrecords;
		records = new Rec[numrecords];
		// may or may not be the same as n
		int i = 0;

		// iterate through the List of "record" Nodes and populate the array of Records.
		for (Iterator iter = list.iterator(); iter.hasNext(); ) {

			node = (Node) iter.next();
			records[i] = new Rec();

			recordid = (node.selectSingleNode(".//head/id")).getText();

			records[i].setId(recordid);

			Node y = node.selectSingleNode(".//head/collection");
			collid = y.valueOf("@recordId");
			records[i].setCollId(collid);

			colllabel = y.getText();
			records[i].setCollLabel(colllabel);

			xmlformat = (node.selectSingleNode(".//head/xmlFormat")).getText();
			records[i].setXMLFormat(xmlformat);

			accessionstatus = (node.selectSingleNode(".//head/additionalMetadata/accessionStatus")).getText();
			records[i].setAccessionStatus(accessionstatus);

			/*
                         *  extract Emails from the Node.
                         */
			/*
                         *  first, the people emails
                         */
			List personemaillist = node.selectNodes(".//emailPrimary");

			for (Iterator iterpeople = personemaillist.iterator(); iterpeople.hasNext(); ) {
				Node nodeperson = (Node) iterpeople.next();
				String address = nodeperson.getText();
				if (!(address.equalsIgnoreCase("unknown"))) {
					EmailAdd em = new EmailAdd("PERSON", address);
					records[i].setPersonFlag(true);
					records[i].addEmail(em);
				}
			}

			/*
                         *  and then the institute emails
                         */
			List instemaillist = node.selectNodes(".//instEmail");

			for (Iterator iterinst = instemaillist.iterator(); iterinst.hasNext(); ) {
				Node nodeinst = (Node) iterinst.next();
				String address = nodeinst.getText();
				if (!(address.equalsIgnoreCase("unknown"))) {
					EmailAdd em_inst = new EmailAdd("INST", address);
					records[i].addEmail(em_inst);
				}
			}

			//prtln(records[i].toString());
			i++;
		}

		/*
                 *  store these records in the DB
                 */
		storeinDB(records, numrecords);

		//System.out.println("exiting extractRecordsInfo:");

	}


	/**
	 *  store the records' info in the CAS database
	 *
	 * @param  records           array of Records returned by the DDS
	 * @param  numrecords        total number of Records
	 * @exception  CasException
	 * @exception  MmdException
	 * @exception  XMLException
	 */
	public void storeinDB(Rec[] records, int numrecords)
		 throws CasException, MmdException, XMLException {

		//System.out.println("entering storeinDB:");

		/*
                 *  open DB connection
                 */
		openDB();
		Object[][] dbmat = null;
		Object dbHist[][] = null;

		boolean dateexistsinprops = false;

		/*
                 *  Check propsdoc for date/email requirements
                 */
		SimpleDateFormat df = new SimpleDateFormat("MM/dd/yyyy");
		Date d = null;
		long dateprops;
		Timestamp tprops = null;
		String[] dates = propsdoc.getXmlFields(0, 0, "runparams/dateToBeChecked");
		if (dates.length != 0) {
			dateexistsinprops = true;
		}

		for (int i = 0; i < numrecords; i++) {
			for (int ec = 0; ec < records[i].getNumEmails(); ec++) {

				//	prtln("Processing record " + records[i].getId());
				/*
                                 *  at least one email address is present in the Rec
                                 */
				EmailAdd[] emailsforrec = records[i].getEmailAdds();
				String e = emailsforrec[ec].getEmailAddress();

				/*
                                 *  make sure this email address is not to be excluded
                                 */
				boolean excluded = false;
				if (exclude_emails != null) {
					for (int s = 0; s < exclude_emails.length; s++) {
						if (e.equalsIgnoreCase(exclude_emails[s])) {
							excluded = true;
							break;
						}
					}
				}
				/*
                                 *  make sure this collection is not to be excluded
                                 */
				if ((exclude_colls != null) && (excluded == false)) {
					for (int s = 0; s < exclude_colls.length; s++) {
						String c = records[i].getCollId();
						if (c.equalsIgnoreCase(exclude_colls[s])) {
							excluded = true;
							break;
						}
					}
				}

				if (excluded != true) {
					boolean newtohistory;
					boolean readd;

					/* check if record is present in mmd */
					dbmat = dbconn.getDbTable("SELECT email_address, email_type, dateLastEmailSent FROM "
						 + "casMmd WHERE recordID = " +
						dbconn.dbstring(records[i].getId())
						 + " AND collID = " + dbconn.dbstring(records[i].getCollId())
						 + " AND email_address = " + dbconn.dbstring(emailsforrec[ec].getEmailAddress())
						 + " AND email_type = " + dbconn.dbstring(emailsforrec[ec].getEmailType()),
						new String[]{"string", "string", "date"},
						true);

					/*check if it is present in History*/
					dbHist = dbconn.getDbTable("SELECT recordID, collID, collLabel, xmlFormat, rec_status, dateCASed, " +
						"dateLastEmailSent, lastcheckDate, email_address, email_type, dateHistory, status, accessionDate FROM casHistory WHERE recordID=" + dbconn.dbstring(records[i].getId())
						 + " AND collID = " + dbconn.dbstring(records[i].getCollId())
						 + " AND email_address = " + dbconn.dbstring(emailsforrec[ec].getEmailAddress())
						 + " AND email_type = " + dbconn.dbstring(emailsforrec[ec].getEmailType())
						 + " ORDER BY dateHistory, email_address",
						new String[]{"string", "string", "string", "string", "string", "date", "date", "date", "string", "string", "date", "string", "string"}, true);

					String status = null;
					if (dbHist.length != 0) {
						newtohistory = false;
						status = (String) dbHist[dbHist.length - 1][11];
						if (status.equalsIgnoreCase("READDED"))
							readd = false;
						else
							readd = true;
					}
					else {
						newtohistory = true;
						readd = false;
					}

					/*  rec is new to Mmd.*/
					if (dbmat.length == 0) {

						/* if it is new to both mmd and history*/
						if (newtohistory == true) {

							boolean really_new = false;
							Date libaccDate = null;
							String libacc = null;
							String request_acc = "http://www.dlese.org/dds/services/ddsws1-0?verb=GetRecord&id=" + records[i].getId();

							try {
								Document document_acc = null;
								URL url_acc = new URL(request_acc);
								int timeOutPeriod_acc = 180000;
								InputStream istm_acc = TimedURLConnection.getInputStream(url_acc,timeOutPeriod_acc);
								
								// Process the InputStream as desired. The InputStream may be used to
								// create an XML DOM or to convert the content to a String, for example.
								// For example, lets create a dom4j DOM document using the InputStream:
								try {
									SAXReader reader_acc = new SAXReader();
									document_acc = reader_acc.read(istm_acc);
								} catch (DocumentException e_acc) {
									// Handle the Exception as desired...
									prtlnErr("Error! : " + e_acc);
								}

								Node node = document_acc.selectSingleNode("/DDSWebService/GetRecord/record/metadata/itemRecord/metaMetadata/dateInfo");
								libacc = node.valueOf("@accessioned");
								if (libacc == "")
								{
									really_new = true;
								}
								else {
									SimpleDateFormat formatter_2 = new SimpleDateFormat("yyyy-MM-dd");
									libaccDate = formatter_2.parse(libacc);


									Calendar now = Calendar.getInstance();
									Calendar working;

									working = (Calendar) now.clone();
									working.add(Calendar.DAY_OF_YEAR, -(30 * 3));
									Date threeMonthsDate = working.getTime();

									if (libaccDate.after(threeMonthsDate)) {
										really_new = true;
									}
									else {
										really_new = false;
									}
								}

							} catch (URLConnectionTimedOutException exc_acc) {
								// The URLConnection timed out...
								prtlnErr("URLConnection timed out while attempting to connect to " + request_acc);
							} catch (IOException ioe_acc) {
								// The URLConnection threw an IOException while attempting to connect...
								prtlnErr("URLConnection threw an IOException while attempting to connect to " + request_acc);
							} catch (ParseException e_acc) {
								prtlnErr("Parse Exception" + e_acc);
							}

							/* if it is less then 3 months old, then add it to Mmd and send them an email */
							if (really_new == true) {
								if (((emailsforrec[ec].getEmailType()).equalsIgnoreCase("PERSON")) || (((emailsforrec[ec].getEmailType()).equalsIgnoreCase("INST")) && (records[i].getPersonFlag() == false))) {

									if (sendemails == true) {
										dbconn.updateDb("INSERT INTO casMmd"
											 + " (recordID, collID, collLabel, xmlFormat,"
											 + " status, dateCASed, dateLastEmailSent, checkDate,"
											 + "  accessionDate, email_address, email_type)"
											 + " VALUES ("
											 + dbconn.dbstringcom(records[i].getId())
											 + dbconn.dbstringcom(records[i].getCollId())
											 + dbconn.dbstringcom(records[i].getCollLabel())
											 + dbconn.dbstringcom(records[i].getXMLFormat())
											 + dbconn.dbstringcom(records[i].getAccessionStatus())
											 + dbconn.dbstringcom(new Timestamp(curdate))
											 + dbconn.dbstringcom(new Timestamp(curdate))
											 + dbconn.dbstringcom(new Timestamp(curdate))
											 + dbconn.dbstringcom(libacc)
											 + dbconn.dbstringcom(emailsforrec[ec].getEmailAddress())
											 + dbconn.dbstring(emailsforrec[ec].getEmailType())
											 + ")");

									}
									/* end if sendemails == true */
									else {
										dbconn.updateDb("INSERT INTO casMmd"
											 + " (recordID, collID, collLabel, xmlFormat,"
											 + " status, dateCASed, checkDate,"
											 + " accessionDate, email_address, email_type)"
											 + " VALUES ("
											 + dbconn.dbstringcom(records[i].getId())
											 + dbconn.dbstringcom(records[i].getCollId())
											 + dbconn.dbstringcom(records[i].getCollLabel())
											 + dbconn.dbstringcom(records[i].getXMLFormat())
											 + dbconn.dbstringcom(records[i].getAccessionStatus())
											 + dbconn.dbstringcom(new Timestamp(curdate))
											 + dbconn.dbstringcom(new Timestamp(curdate))
											 + dbconn.dbstringcom(libacc)
											 + dbconn.dbstringcom(emailsforrec[ec].getEmailAddress())
											 + dbconn.dbstring(emailsforrec[ec].getEmailType())
											 + ")");

									}
									/* end if sendemails != true */
								}
								/* end if person email */
								else if (((emailsforrec[ec].getEmailType()).equalsIgnoreCase("INST")) && (records[i].getPersonFlag() == true)) {
									/*
									*  institute email address, which doesn't need to be emailed. it is just
									*  added to the CAS database but no email is sent to this email address
									*/
									dbconn.updateDb("INSERT INTO casMmd"
										 + " (recordID, collID, collLabel, xmlFormat,"
										 + " status, dateCASed,  checkDate,"
										 + " accessionDate, email_address, email_type)"
										 + " VALUES ("
										 + dbconn.dbstringcom(records[i].getId())
										 + dbconn.dbstringcom(records[i].getCollId())
										 + dbconn.dbstringcom(records[i].getCollLabel())
										 + dbconn.dbstringcom(records[i].getXMLFormat())
										 + dbconn.dbstringcom(records[i].getAccessionStatus())
										 + dbconn.dbstringcom(new Timestamp(curdate))
										 + dbconn.dbstringcom(new Timestamp(curdate))
										 + dbconn.dbstringcom(libacc)
										 + dbconn.dbstringcom(emailsforrec[ec].getEmailAddress())
										 + dbconn.dbstring(emailsforrec[ec].getEmailType())
										 + ")");

								} /* end if institute email */
							}
							/* end if really_new == true */
							/* if it is more than 3 months old , then add it to Mmd but dont send an email */
							if (really_new == false) {

								dbconn.updateDb("INSERT INTO casMmd"
									 + " (recordID, collID, collLabel, xmlFormat,"
									 + " status,dateCASed,  checkDate,"
									 + " accessionDate, email_address, email_type)"
									 + " VALUES ("
									 + dbconn.dbstringcom(records[i].getId())
									 + dbconn.dbstringcom(records[i].getCollId())
									 + dbconn.dbstringcom(records[i].getCollLabel())
									 + dbconn.dbstringcom(records[i].getXMLFormat())
									 + dbconn.dbstringcom(records[i].getAccessionStatus())
									 + dbconn.dbstringcom(new Timestamp(curdate))
									 + dbconn.dbstringcom(new Timestamp(curdate))
									 + dbconn.dbstringcom(libacc)
									 + dbconn.dbstringcom(emailsforrec[ec].getEmailAddress())
									 + dbconn.dbstring(emailsforrec[ec].getEmailType())
									 + ")");

							} /* end if really_new == false */
						}
						/* end of if it is new to both mmd and history */
						else {
							/* if it is new to mmd but not new to history */
							/* if it is present in History as READDED, then do nothing and output an error */
							if (status.equalsIgnoreCase("READDED")) {
								readd = false;
								prtln("ERROR new to mmd & present in history as readded");
							}
							/*end if it present in History as READDED */
							/* if it is present in History as DELETED, then readd to History as READDED, and readd to Mmd. Do not email.*/
							if (status.equalsIgnoreCase("DELETED")) {

								readd = true;
								Long accdate = (Long) dbHist[dbHist.length - 1][5];
								Long emaildate = (Long) dbHist[dbHist.length - 1][6];
								Long checkdate = (Long) dbHist[dbHist.length - 1][7];
								Timestamp acc = null;
								Timestamp email = null;
								Timestamp check = null;

								if (accdate != null) {
									long l = accdate.longValue();
									acc = new Timestamp(l);
								}
								if (emaildate != null) {
									long l = emaildate.longValue();
									email = new Timestamp(l);
								}
								if (checkdate != null) {
									long l = checkdate.longValue();
									check = new Timestamp(l);
								}

								if (emaildate != null) {
									dbconn.updateDb("INSERT INTO casMmd"
										 + " (recordID, collID, collLabel, xmlFormat,"
										 + " status, dateCASed, dateLastEmailSent, checkDate,"
										 + " email_address, email_type, accessionDate)"
										 + " VALUES ("
										 + dbconn.dbstringcom((String) dbHist[dbHist.length - 1][0])
										 + dbconn.dbstringcom((String) dbHist[dbHist.length - 1][1])
										 + dbconn.dbstringcom((String) dbHist[dbHist.length - 1][2])
										 + dbconn.dbstringcom((String) dbHist[dbHist.length - 1][3])
										 + dbconn.dbstringcom((String) dbHist[dbHist.length - 1][4])
										 + dbconn.dbstringcom(acc)
										 + dbconn.dbstringcom(email)
										 + dbconn.dbstringcom(new Timestamp(curdate))
										 + dbconn.dbstringcom((String) dbHist[dbHist.length - 1][8])
										 + dbconn.dbstringcom((String) dbHist[dbHist.length - 1][9])
										 + dbconn.dbstring((String) dbHist[dbHist.length - 1][12])
										 + ")");

									dbconn.updateDb("INSERT INTO casHistory"
										 + " (recordID, collID, collLabel, xmlFormat, "
										 + " rec_status, dateCASed, dateLastEmailSent, lastcheckDate,"
										 + " email_address, email_type, dateHistory, status, accessionDate)"
										 + " VALUES ("
										 + dbconn.dbstringcom((String) dbHist[dbHist.length - 1][0])
										 + dbconn.dbstringcom((String) dbHist[dbHist.length - 1][1])
										 + dbconn.dbstringcom((String) dbHist[dbHist.length - 1][2])
										 + dbconn.dbstringcom((String) dbHist[dbHist.length - 1][3])
										 + dbconn.dbstringcom((String) dbHist[dbHist.length - 1][4])
										 + dbconn.dbstringcom(acc)
										 + dbconn.dbstringcom(email)
										 + dbconn.dbstringcom(check)
										 + dbconn.dbstringcom((String) dbHist[dbHist.length - 1][8])
										 + dbconn.dbstringcom((String) dbHist[dbHist.length - 1][9])
										 + dbconn.dbstringcom(new Timestamp(curdate))
										 + dbconn.dbstringcom("READDED")
										 + dbconn.dbstring((String) dbHist[dbHist.length - 1][12])
										 + ")");
								}
								/* end if emaildate != null */
								else {
									dbconn.updateDb("INSERT INTO casMmd"
										 + " (recordID, collID, collLabel, xmlFormat,"
										 + " status, dateCASed, checkDate,"
										 + " email_address, email_type, accessionDate)"
										 + " VALUES ("
										 + dbconn.dbstringcom((String) dbHist[dbHist.length - 1][0])
										 + dbconn.dbstringcom((String) dbHist[dbHist.length - 1][1])
										 + dbconn.dbstringcom((String) dbHist[dbHist.length - 1][2])
										 + dbconn.dbstringcom((String) dbHist[dbHist.length - 1][3])
										 + dbconn.dbstringcom((String) dbHist[dbHist.length - 1][4])
										 + dbconn.dbstringcom(acc)
										 + dbconn.dbstringcom(new Timestamp(curdate))
										 + dbconn.dbstringcom((String) dbHist[dbHist.length - 1][8])
										 + dbconn.dbstringcom((String) dbHist[dbHist.length - 1][9])
										 + dbconn.dbstring((String) dbHist[dbHist.length - 1][12])
										 + ")");

									dbconn.updateDb("INSERT INTO casHistory"
										 + " (recordID, collID, collLabel, xmlFormat, "
										 + " rec_status, dateCASed, lastcheckDate,"
										 + " email_address, email_type, dateHistory, status, accessionDate)"
										 + " VALUES ("
										 + dbconn.dbstringcom((String) dbHist[dbHist.length - 1][0])
										 + dbconn.dbstringcom((String) dbHist[dbHist.length - 1][1])
										 + dbconn.dbstringcom((String) dbHist[dbHist.length - 1][2])
										 + dbconn.dbstringcom((String) dbHist[dbHist.length - 1][3])
										 + dbconn.dbstringcom((String) dbHist[dbHist.length - 1][4])
										 + dbconn.dbstringcom(acc)
										 + dbconn.dbstringcom(check)
										 + dbconn.dbstringcom((String) dbHist[dbHist.length - 1][8])
										 + dbconn.dbstringcom((String) dbHist[dbHist.length - 1][9])
										 + dbconn.dbstringcom(new Timestamp(curdate))
										 + dbconn.dbstringcom("READDED")
										 + dbconn.dbstring((String) dbHist[dbHist.length - 1][12])
										 + ")");

								}
								/* end else if emaildate != null */
							} /* end if it is present in History as DELETED*/
						}
						/* end of if it is new to mmd but not new to history */
					}
					/* end of if rec is new to Mmd */
					else {
						/* rec is not new to mmd */
						/* if you can get rec from mmd, then it is not new. update AccessionStatus and checkDate and exit */
						dbconn.updateDb("UPDATE casMmd SET checkDate = "
							 + dbconn.dbstring(new Timestamp(curdate))
							 + " WHERE recordID = "
							 + dbconn.dbstring(records[i].getId())
							 + " AND collID = " + dbconn.dbstring(records[i].getCollId())
							 + " AND email_address = " + dbconn.dbstring(emailsforrec[ec].getEmailAddress())
							 + " AND email_type = " + dbconn.dbstring(emailsforrec[ec].getEmailType()));

						dbconn.updateDb("UPDATE casMmd SET status = "
							 + dbconn.dbstring(records[i].getAccessionStatus())
							 + " WHERE recordID = "
							 + dbconn.dbstring(records[i].getId())
							 + " AND collID = " + dbconn.dbstring(records[i].getCollId())
							 + " AND email_address = " + dbconn.dbstring(emailsforrec[ec].getEmailAddress())
							 + " AND email_type = " + dbconn.dbstring(emailsforrec[ec].getEmailType()));

						if (readd == true) {
							dbconn.updateDb("UPDATE casHistory SET lastcheckDate = "
								 + dbconn.dbstring(new Timestamp(curdate))
								 + " WHERE recordID = "
								 + dbconn.dbstring(records[i].getId())
								 + " AND collID = " + dbconn.dbstring(records[i].getCollId())
								 + " AND email_address = " + dbconn.dbstring(emailsforrec[ec].getEmailAddress())
								 + " AND email_type = " + dbconn.dbstring(emailsforrec[ec].getEmailType())
								 + " AND dateHistory = \"" + new Timestamp(curdate)
								 + "\" AND status = \"READDED\"");

							dbconn.updateDb("UPDATE casHistory SET rec_status = "
								 + dbconn.dbstring(records[i].getAccessionStatus())
								 + " WHERE recordID = "
								 + dbconn.dbstring(records[i].getId())
								 + " AND collID = " + dbconn.dbstring(records[i].getCollId())
								 + " AND email_address = " + dbconn.dbstring(emailsforrec[ec].getEmailAddress())
								 + " AND email_type = " + dbconn.dbstring(emailsforrec[ec].getEmailType())
								 + " AND dateHistory = \"" + new Timestamp(curdate)
								 + "\" AND status = \"READDED\"");
						}

						if (sendemails == true) {
							if (((emailsforrec[ec].getEmailType()).equalsIgnoreCase("PERSON")) || (((emailsforrec[ec].getEmailType()).equalsIgnoreCase("INST")) && (records[i].getPersonFlag() == false))) {

								if (dateexistsinprops == true) {
									boolean never = false;
									String[] neversent = propsdoc.getXmlFields("runparams/dateToBeChecked/neversent");
									if (neversent.length != 0) {
										never = true;
									}
									String[] years = propsdoc.getXmlFields("runparams/dateToBeChecked/years");
									String[] months = propsdoc.getXmlFields("runparams/dateToBeChecked/months");
									String[] days = propsdoc.getXmlFields("runparams/dateToBeChecked/days");
									String Years = null;
									String Months = null;
									String Days = null;
									if (years.length != 0) {
										Years = propsdoc.getXmlString("runparams/dateToBeChecked/years");
									}
									if (months.length != 0) {
										Months = propsdoc.getXmlString("runparams/dateToBeChecked/months");
									}

									if (days.length != 0) {
										Days = propsdoc.getXmlString("runparams/dateToBeChecked/days");
									}

									Long ldb = (Long) dbmat[0][2];

									if ((ldb == null) && (never == true)) {
										//this email address has never been emailed and should be emailed

										/*
                                                                                 *  update the dateLastEmailSent field in the DB
                                                                                 */
										dbconn.updateDb("UPDATE casMmd SET dateLastEmailSent = "
											 + dbconn.dbstring(new Timestamp(curdate))
											 + " WHERE recordID = "
											 + dbconn.dbstring(records[i].getId())
											 + " AND collID = " + dbconn.dbstring(records[i].getCollId())
											 + " AND email_address = " + dbconn.dbstring(emailsforrec[ec].getEmailAddress())
											 + " AND email_type = " + dbconn.dbstring(emailsforrec[ec].getEmailType()));

										if (readd == true) {
											dbconn.updateDb("UPDATE casHistory SET dateLastEmailSent = "
												 + dbconn.dbstring(new Timestamp(curdate))
												 + " WHERE recordID = "
												 + dbconn.dbstring(records[i].getId())
												 + " AND collID = " + dbconn.dbstring(records[i].getCollId())
												 + " AND email_address = " + dbconn.dbstring(emailsforrec[ec].getEmailAddress())
												 + " AND email_type = " + dbconn.dbstring(emailsforrec[ec].getEmailType())
												 + " AND dateHistory = \"" + new Timestamp(curdate) + "\""
												 + " AND status = \"READDED\"");
										}

										totalEmails++;
									}
									if (ldb != null) {
										long l = ldb.longValue();
										Calendar cal = Calendar.getInstance();
										cal.setTimeInMillis(l);

										if (Years != null) {
											cal.add(Calendar.YEAR, Integer.parseInt(Years.trim()));
										}
										if (Months != null) {
											cal.add(Calendar.MONTH, Integer.parseInt(Months.trim()));
										}
										if (Days != null) {
											cal.add(Calendar.DAY_OF_YEAR, Integer.parseInt(Days.trim()));
										}

										//Timestamp tdb = new Timestamp(l);
										Calendar todaycal = Calendar.getInstance();
										todaycal.setTimeInMillis(curdate);

										//if ((tdb.before(tprops) == true)) {
										if (todaycal.after(cal) == true) {
											/*
                                                                                         *  update the dateLastEmailSent field in the DB
                                                                                         */
											dbconn.updateDb("UPDATE casMmd SET dateLastEmailSent = "
												 + dbconn.dbstring(new Timestamp(curdate))
												 + " WHERE recordID = "
												 + dbconn.dbstring(records[i].getId())
												 + " AND collID = " + dbconn.dbstring(records[i].getCollId())
												 + " AND email_address = " + dbconn.dbstring(emailsforrec[ec].getEmailAddress())
												 + " AND email_type = " + dbconn.dbstring(emailsforrec[ec].getEmailType()));

											if (readd == true) {
												dbconn.updateDb("UPDATE casHistory SET dateLastEmailSent = "
													 + dbconn.dbstring(new Timestamp(curdate))
													 + " WHERE recordID = "
													 + dbconn.dbstring(records[i].getId())
													 + " AND collID = " + dbconn.dbstring(records[i].getCollId())
													 + " AND email_address = " + dbconn.dbstring(emailsforrec[ec].getEmailAddress())
													 + " AND email_type = " + dbconn.dbstring(emailsforrec[ec].getEmailType())
													 + " AND dateHistory = \"" + new Timestamp(curdate) + "\""
													 + " AND status = \"READDED\"");
											}

											totalEmails++;
										}
									}
								}
							}
						}
					}
					/* end of rec is not new to mmd */
				}
			}
		}
		/*
                 *  close DB connection
                 */
		closeDb();
		//System.out.println("exiting storeinDB:");

	}


	/**
	 *  send Emails to appropriate email addresses
	 *
	 * @exception  CasException
	 * @exception  MmdException
	 * @exception  XMLException
	 * @exception  MessagingException
	 * @exception  NoSuchProviderException
	 * @exception  IOException
	 */
	void sendEmails()
		 throws CasException, MmdException, XMLException, MessagingException, NoSuchProviderException, IOException {
		//System.out.println("entering sendEmails:");

		/*
                 *  run through the whole DB and send emails to rows which have dateLastEmailSent as currentdate
                 */
		openDB();
		Object[][] dbmat = null;

		dbmat = dbconn.getDbTable("SELECT recordID, collID, collLabel, email_address, email_type FROM "
			 + "casMmd WHERE dateLastEmailSent =  " +
			dbconn.dbstring(new Timestamp(curdate))
			 + " ORDER BY email_address",
			new String[]{"string", "string", "string", "string", "string"},
			true);

		/*
                for (int ii = 0; ii < dbmat.length; ii++) {
                        prtln(dbmat[ii][0] + " " + dbmat[ii][1] + " " + dbmat[ii][2] + " " + dbmat[ii][3] + " " + dbmat[ii][4]);
                }
		*/
		int counter = 0;
		int num = 0;
		EmailRecs[] er = new EmailRecs[dbmat.length];
		while (counter < dbmat.length) {
			er[num] = new EmailRecs();

			do {
				Rec r = new Rec();

				r.setId((String) dbmat[counter][0]);
				r.setCollId((String) dbmat[counter][1]);
				r.setCollLabel((String) dbmat[counter][2]);
				// add to EmailRecs object
				//prtln("will add " + dbmat[counter][3] + "to the EmailRecs object");
				er[num].setAddress((String) dbmat[counter][3]);
				er[num].setType((String) dbmat[counter][4]);
				er[num].addRec(r);
				counter++;
				if (counter == dbmat.length) {
					break;
				}
				if (!(dbmat[counter][3].equals(dbmat[counter - 1][3]))) {
					break;
				}
			} while (true);
			num++;
		}

		prtln(" no of emails to be sent is " + dbmat.length);
		  prtln(" no of emailrecs objects is " + num);
		String summary = "Time of this CAS run : " + new Timestamp(curdate) + "\n";
		if (num == 0) {
			summary += new String("No emails were sent on this run.\n");
		}
		else {
			summary += new String("The following is a summary report of this run.\nEmails were sent to:\n");
		}

		String email_file_name = propsdoc.getXmlString("runparams/emailText");

		String message = "";

		File inFile = new File(email_file_name);

		try {
			BufferedReader bufRdr = new BufferedReader(new FileReader(inFile));

			String line = null;
			while ((line = bufRdr.readLine()) != null) {
				message += line;
				message += "\n";
			}

			bufRdr.close();

		} catch (IOException e) {
			System.err.println(e);
			System.exit(1);
		}

		int index = message.indexOf("inform you that");
		String prestring = message.substring(0, index + 15);
		String poststring = message.substring(index + 15);

		for (int k = 0; k < num; k++) {

			summary += "\n" + er[k].getAddress() + " (" + er[k].getType() + ")\nNew Records: " + er[k].getNumRecs() + "\n";
			if ((er[k].getType()).equalsIgnoreCase("INST")) {
				summary += "http://www.dlese.org/jsp/cas/index.jsp?qc=emailOrganization:" + er[k].getAddress() + "\n";
			}
			else {
				summary += "http://www.dlese.org/jsp/cas/index.jsp?qc=emailPrimary:" + er[k].getAddress() + "\n";
			}
			int ii;

			int numrecs = er[k].getNumRecs();
			String msgtext = new String(prestring + " " + numrecs + " ");

			msgtext += poststring;
			int index2 = msgtext.indexOf("to DLESE at :");
			String newpre = msgtext.substring(0, index2 + 13);
			String newpost = msgtext.substring(index2 + 13);
			String newmsg = "";

			if ((er[k].getType()).equalsIgnoreCase("PERSON")) {
				newmsg = new String(newpre + "\n\nhttp://www.dlese.org/jsp/cas/index.jsp?qc=emailPrimary:");
			}
			else {
				newmsg = new String(newpre + "\n\nhttp://www.dlese.org/jsp/cas/index.jsp?qc=emailOrganization:");
			}

			newmsg += er[k].getAddress();
			newmsg += "\n\n" + newpost;

			//                        newmsg += "\n\nYour email address is " + er[k].getAddress() + " \n You are a " + er[k].getType() + " \n\n";

			// Convert strange chars to %xx hex encoding.
			// Java's MimeMessage will automatically convert the ENTIRE
			// message to some strange encoding if the message
			// contains a single weird char.

			StringBuffer finalBuf = new StringBuffer();
			for (ii = 0; ii < newmsg.length(); ii++) {
				char cc = newmsg.charAt(ii);
				if ((cc >= 0x20 && cc < 0x7f)
					 || cc == '\t'
					 || cc == '\n'
					 || cc == '\f'
					 || cc == '\r') {
					finalBuf.append(cc);
				}
				else {
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
			String emailHost = propsdoc.getXmlString("general/emailHost");
			props.put("mail.smtp.host", emailHost);
			Session session = Session.getInstance(props, null);
			session.setDebug(debug);

			MimeMessage msg = new MimeMessage(session);

			String emailFrom = propsdoc.getXmlString("general/emailFrom");
			String emailSubject = propsdoc.getXmlString("general/emailSubject");

			String emailReplyTo = propsdoc.getXmlString("general/emailReplyTo");
			InternetAddress[] replyTo = {new InternetAddress(emailReplyTo)};

			msg.setFrom(new InternetAddress(emailFrom));
			msg.setReplyTo(replyTo);

			InternetAddress[] recips = new InternetAddress[1];

			recips[0] = new InternetAddress(er[k].getAddress());
			//recips[0] = new InternetAddress("sonal@ucar.edu");

			msg.setRecipients(RecipientType.TO, recips);

			InternetAddress[] bccrecips = new InternetAddress[1];
			bccrecips[0] = new InternetAddress("sonalbhushan@gmail.com");

			msg.setRecipients(RecipientType.BCC, bccrecips);

			msg.setSubject(emailSubject);
			msg.setContent(finalBuf.toString(), "text/plain");
			Transport.send(msg);

			/*
                         *  add email text to database
                         */
			String record_ids = " ";

			for (int y = 0; y < numrecs; y++) {
				String recordid = er[k].recs[y].getId();
				String collid = er[k].recs[y].getCollId();

				record_ids += recordid + "  ";

				dbconn.updateDb("UPDATE casMmd SET email_text = "
					 + dbconn.dbstring(newmsg)
					 + " WHERE recordID = "
					 + dbconn.dbstring(recordid)
					 + " AND collID = " + dbconn.dbstring(collid)
					 + " AND email_address = " + dbconn.dbstring(er[k].getAddress())
					 + " AND email_type = " + dbconn.dbstring(er[k].getType()));

			}

			dbconn.updateDb("INSERT INTO casEmail"
				 + " (email_address, email_type, dateEmailSent, records,email_text)"
				 + " VALUES ("
				 + dbconn.dbstringcom(er[k].getAddress())
				 + dbconn.dbstringcom(er[k].getType())
				 + dbconn.dbstringcom(new Timestamp(curdate))
				 + dbconn.dbstringcom(record_ids)
				 + dbconn.dbstring(newmsg)
				 + ")");

		}

		/*
                 *  Send Katy/Holly a summary email
                 */
		summary += "\n The email text was : \n\n" + message;
		sendSummaryEmail(summary);
		closeDb();

		//System.out.println("exiting sendEmails:");

	}


	/**
	 *  Sends a summary email to the admins everytime CAS runs, detailing info about who was emailed. 
	 *
	 * @param  summary                      Text of the summary email
	 * @exception  CasException             Description of the Exception
	 * @exception  MmdException             Description of the Exception
	 * @exception  XMLException             Description of the Exception
	 * @exception  MessagingException       Description of the Exception
	 * @exception  NoSuchProviderException  Description of the Exception
	 * @exception  IOException              Description of the Exception
	 */
	void sendSummaryEmail(String summary)
		 throws CasException, MmdException, XMLException, MessagingException, NoSuchProviderException, IOException {

		//System.out.println("entering sendSummaryEmail:");

		// Convert strange chars to %xx hex encoding.
		// Java's MimeMessage will automatically convert the ENTIRE
		// message to some strange encoding if the message
		// contains a single weird char.

		StringBuffer finalBuf = new StringBuffer();
		for (int ii = 0; ii < summary.length(); ii++) {
			char cc = summary.charAt(ii);
			if ((cc >= 0x20 && cc < 0x7f)
				 || cc == '\t'
				 || cc == '\n'
				 || cc == '\f'
				 || cc == '\r') {
				finalBuf.append(cc);
			}
			else {
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
		String emailHost = propsdoc.getXmlString("general/emailHost");
		props.put("mail.smtp.host", emailHost);
		Session session = Session.getInstance(props, null);
		session.setDebug(debug);

		MimeMessage msg = new MimeMessage(session);

		String emailFrom = propsdoc.getXmlString("general/emailFrom");
		String emailSubject = new String("Summary of CAS run at " + new Timestamp(curdate));

		msg.setFrom(new InternetAddress(emailFrom));
		String emailReplyTo = propsdoc.getXmlString("general/emailReplyTo");
		InternetAddress[] replyTo = {new InternetAddress(emailReplyTo)};

		msg.setReplyTo(replyTo);

		String[] emailAdmins = null;
		emailAdmins = propsdoc.getXmlFields(0, 0, "general/emailAdmin");

		InternetAddress[] recips = new InternetAddress[emailAdmins.length];
		for (int ii = 0; ii < emailAdmins.length; ii++) {
			recips[ii] = new InternetAddress(emailAdmins[ii]);
		}
		msg.setRecipients(RecipientType.TO, recips);
		msg.setSubject(emailSubject);
		msg.setContent(finalBuf.toString(), "text/plain");
		Transport.send(msg);

		// write a log file

		File outFile = new File("Log_CAS_Run" + new Timestamp(curdate));

		try {
			BufferedWriter bufWrtr = new BufferedWriter(new FileWriter(outFile));

			bufWrtr.write(summary);
			bufWrtr.newLine();

			bufWrtr.close();

		} catch (IOException e) {
			System.err.println(e);
			System.exit(1);
		}

		//System.out.println("exiting sendSummaryEmail:");

	}


	/**
	 *  Open a DB connection
	 *
	 * @exception  CasException
	 * @exception  XMLException
	 */
	private void openDB()
		 throws CasException, XMLException {

		//System.out.println("entering openDb:");

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
			 + "?user=" + dbUser
			 + "&password=";
		// all except password, for err msgs
		printableUrl = dbUrl + "(omitted)";
		dbUrl += dbPassword;

		// configure Driver to treat zero'd out time strings as null - jlo 3/2/2010
		dbUrl += "&zeroDateTimeBehavior=convertToNull";


		dbconn = null;
		try {
			dbconn = new DbConn(0, dbUrl);
		} catch (MmdException mde) {
			mkerror("could not open db connection to URL \""
				 + printableUrl + "\"  exc: " + mde);
		}

		//System.out.println("exiting openDb:");

	}



	/**
	 *  Closes the DB connection.
	 *
	 * @exception  CasException
	 */

	private void closeDb()
		 throws CasException {

		//System.out.println("entering closeDb:");

		if (dbconn != null) {
			try {
				dbconn.closeDb();
			} catch (MmdException mde) {
				mkerror("could not close db connection to URL \""
					 + printableUrl + "\"  exc:" + mde);
			}
			dbconn = null;
		}
		//System.out.println("exiting closeDb:");

	}


	/**
	 *  Prints to stdout./
	 *
	 * @param  msg
	 */
	private static void prtln(String msg) {
		System.out.println(msg);
	}


	/**
	 *  Prints error messages to stdout
	 *
	 * @param  msg
	 */
	private static void prtlnErr(String msg) {
		System.err.println(msg);
	}


	/**
	 *  Description of the Method
	 *
	 * @param  msg               Description of the Parameter
	 * @exception  CasException  Description of the Exception
	 */
	static void mkerror(String msg)
		 throws CasException {
		throw new CasException(msg);
	}

}
// end class Cas


