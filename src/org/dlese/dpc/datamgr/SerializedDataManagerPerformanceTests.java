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
package org.dlese.dpc.datamgr;

import org.dlese.dpc.junit.TestTools;

import junit.framework.*;
import java.util.Date;
import java.util.Random;
import java.util.Enumeration;
import java.util.*;
//import org.dlese.dpc.oai.datamgr.*;


/**
 *  DESCRIPTION
 *
 * @author    John Weatherley
 */
public class SerializedDataManagerPerformanceTests extends TestCase
{
	SerializedDataManager dm = null;


	/**
	 *  A unit test suite for JUnit
	 *
	 * @return    The test suite
	 */
	public static Test suite() {
		// Use java reflection to run all test methods in this class:
		prtln("");
		return new TestSuite(SerializedDataManagerPerformanceTests.class);
	}


	static int recNum = 0;


	/**
	 *  The JUnit setup method. Gets called prior to running EACH test method call.
	 *
	 * @exception  Exception  DESCRIPTION
	 */
	protected void setUp()
			 throws Exception {
		prtln(".");
		// introduce some space between the tests

		// Assumes a java prop was set that defines junit.test.dir prior to execution.
		// This prop is defined in the Ant build.xml file.
		dm = new SerializedDataManager(System.getProperty("junit.test.dir") , false);

		// Nuke everything in the DM:
		String[] IDs = dm.getIDs();
		for (int i = 0; i < IDs.length; i++)
			dm.delete(IDs[i]);

		IDs = dm.getIDs();
		Assert.assertTrue("SetUp(): The DM is not empty", IDs.length == 0 && dm.getNumRecords() == 0);
	}


	/**
	 *  A unit test for JUnit
	 *
	 * @exception  Exception  DESCRIPTION
	 */
	public void test_getIDsSorted_method()
			 throws Exception {
		int NUM_RECORDS = 500;
		int ID_LEN = 10;
		String payload = "hi";
		String id = null;

		String[] original_ids = new String[NUM_RECORDS];
		String[] returned_ids = null;

		// Populate with NUM_RECORDS records
		for (int i = 0; i < NUM_RECORDS; i++) {
			id = TestTools.getRandomAlphaString(ID_LEN);
			original_ids[i] = id;
			dm.put(id, payload);
		}
		Arrays.sort(original_ids);

		Date start = new Date();
		returned_ids = dm.getIDsSorted();
		// Make the call
		Date end = new Date();
		TestTools.printElapsedTime("Time call getIDsSorted with " +
				NUM_RECORDS +
				" records in the DataManager: ", start, end);

		// Verify all is well...
		for (int i = 0; i < NUM_RECORDS; i++) {
			Assert.assertTrue("Not equals: original: \"" + original_ids[i] +
					"\"            returned: \"" + returned_ids[i] + "\"",
					original_ids[i].equals(returned_ids[i]));
		}
	}


	/**
	 *  A unit test for JUnit
	 *
	 * @exception  Exception  Exception
	 */
	public void test_put_and_get_methods()
			 throws Exception {
		int NUM_RECORDS = 1000;
		String original = null;
		String returned = null;

		Date start = new Date();

		Date start2 = new Date();
		for (int i = 0; i < NUM_RECORDS; i++) {
			original = getUniqueString();
			dm.put(original, original + " content");
		}
		Date end2 = new Date();

		for (int i = 0; i < NUM_RECORDS; i++) {
			returned = (String)dm.get(original);
			Assert.assertEquals(original + " content", returned);
		}

		Date end = new Date();
		TestTools.printElapsedTime("Time to put " +
				NUM_RECORDS +
				" records in the DataManager: ", start2, end2);
		TestTools.printElapsedTime("Time to put and then get and assertEquals " +
				NUM_RECORDS +
				" records in the DataManager: ", start, end);
	}


	/**  A unit test for JUnit */
	public void test_simple_filename_encode_decode() {
		final String original = "oai:dlese.org:DLESE-000-000-002-254";

		String encoded = dm.encodeFileName(original);
		String decoded = dm.decodeFileName(encoded);
		//prtln("original: " + original);
		//prtln(" encoded: " + encoded);
		//prtln(" decoded: " + decoded);
		Assert.assertEquals(original, decoded);
	}


	/**  A unit test for JUnit */
	public void test_getNumRecords() {
		Date start = new Date();
		long num = dm.getNumRecords();
		Date end = new Date();
		String[] IDs = dm.getIDs();

		TestTools.printElapsedTime("Time to call getNumRecords with " + num + " records in the DM: ", start, end);
		Assert.assertEquals(IDs.length, num);
	}


	/**  A unit test for JUnit */
	public void test_getIDs() {
		Date start = new Date();
		String[] IDs = dm.getIDs();
		Date end = new Date();
		if (IDs.length > 0) {
			int mid = (int)(Math.floor(IDs.length / 2));
			int last = IDs.length - 1;
			TestTools.printElapsedTime("Time to call getIDs with " + IDs.length + " records in the DM: ", start, end);
			prtln("First record is: " + IDs[0] + ". " + mid + "th record is: " + IDs[mid] + ". " + last + "th record is: " + IDs[last]);
		}
	}


	/**  A unit test for JUnit */
	public void test_getIDsSorted() {
		Date start = new Date();
		String[] IDs = dm.getIDsSorted();
		Date end = new Date();
		if (IDs.length > 0) {
			int mid = (int)(Math.floor(IDs.length / 2));
			int last = IDs.length - 1;
			TestTools.printElapsedTime("Time to call getIDsSorted with " + IDs.length + " records in the DM: ", start, end);
			prtln("First record is: " + IDs[0] + ". " + mid + "th record is: " + IDs[mid] + ". " + last + "th record is: " + IDs[last]);
		}
	}


	/**  A unit test for JUnit */
	public void test_random_filename_encode_decode() {
		dm.decodeFileName("init first run");
		// Init on first run.

		String original;
		// Init on first run.

		String encoded;
		// Init on first run.

		String decoded;
		Random randgen = new Random(new Date().getTime());
		char c;

		final int ID_LEN = 15;
		final int NUM_IDS = 60;

		Date start = new Date();

		for (int i = 0; i < NUM_IDS; i++) {
			original = TestTools.getRandomCharsString(ID_LEN);
			encoded = dm.encodeFileName(original);
			decoded = dm.decodeFileName(encoded);
			//prtln("original: " + original);
			//prtln(" decoded: " + decoded);
			//prtln(" encoded: " + encoded);
			Assert.assertEquals(original, decoded);
		}

		Date end = new Date();
		TestTools.printElapsedTime("Encoding then decoding " + NUM_IDS + " IDs of length " + ID_LEN + " took: ", start, end);

	}


	/**
	 *  Gets the uniqueString attribute of the
	 *  SerializedDataManagerPerformanceTests object
	 *
	 * @return    The uniqueString value
	 */
	private String getUniqueString() {
		return TestTools.getUniqueID();
	}


	/*
	 *  tearDown() gets called after running EACH test method in this class
	 */
	/**  The teardown method for JUnit */
	protected void tearDown() {
		//prtln("tearDown()");
	}


	/**
	 *  DESCRIPTION
	 *
	 * @param  s  DESCRIPTION
	 */
	private static void prtln(String s) {
		System.out.println(s);
	}


	/*
	 *  Main method for running this single suite from the command line
	 */
	/**
	 *  The main program for the SerializedDataManagerPerformanceTests class
	 *
	 * @param  args  The command line arguments
	 */
	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}
}

