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



public class SerializedDataManagerTest extends TestCase
{
	SerializedDataManager dm = null;
	
	public static Test suite() {
		// Use java reflection to run all test methods in this class:
		prtln("");
		return new TestSuite(SerializedDataManagerTest.class);
	} 
	
	static int recNum = 0;
	
	/* setUp() gets called prior to running EACH test method in this class */
	protected void setUp()
	throws Exception
	{
		prtln(".");// introduce some space between the tests
		
		// Assumes a java prop was set that defines junit.test.dir prior to execution.
		// This prop is defined in the Ant build.xml file.
		dm = new SerializedDataManager( System.getProperty("junit.test.dir") , false );
		
		// Remove everything from the DM:
		String [] IDs = dm.getIDs();
		for(int i = 0; i < IDs.length; i++)
			dm.delete(IDs[i]);
		
		IDs = dm.getIDs();
		Assert.assertTrue("The DM is not empty", IDs.length == 0 && dm.getNumRecords() == 0 );	
	}
	
	
	
	public void test_getIDsSorted_method()
	throws Exception
	{
		int NUM_RECORDS = 50;
		int ID_LEN = 10;
		String payload = "hi";
		String id = null;
				
		String [] original_ids = new String [NUM_RECORDS];
		String [] returned_ids = null;
		
		
		for(int i = 0; i < NUM_RECORDS ; i++)
		{
			id = TestTools.getRandomCharsString(ID_LEN);
			original_ids[i] = id;
			dm.put(	id, payload );
		}
		Arrays.sort(original_ids);
		returned_ids = dm.getIDsSorted();
		for(int i = 0; i < NUM_RECORDS ; i++)
		{
			Assert.assertTrue(	"Not equals: original: \"" + original_ids[i] +
								"\"            returned: \"" + returned_ids[i] + "\"",
								original_ids[i].equals(returned_ids[i]) );
		}
	}

	public void test_locking_methods()
	throws Exception
	{
		int NUM_RECORDS = 20;
		int locked_count = 0;
		int unlocked_count = 0;
		
		int ID_LEN = 10;
		String payload = "hi";
		String id = null;
				
		String [] original_ids = new String [NUM_RECORDS];
		boolean [] is_locked = new boolean [NUM_RECORDS];
		String [] lock_keys = new String [NUM_RECORDS];
		Object o = null;
		
		
		// Load up a corpus of records:
		for(int i = 0; i < NUM_RECORDS ; i++)
		{
			id = TestTools.getRandomCharsString(ID_LEN);
			original_ids[i] = id;
			dm.put(	id, payload );
		}
		
		
		for(int i = 0; i < NUM_RECORDS ; i++)
		{
			// Lock aprox 1/3 of the records
			int ran = TestTools.getRandomIntBetween(0,3);
			if (ran == 0)
			{
				is_locked[i] = true;
				lock_keys[i] = dm.lock(original_ids[i]);
				locked_count++;
			}
			else
				unlocked_count++;
			
		}
		

		for(int i = 0; i < NUM_RECORDS ; i++)
		{
			if(is_locked[i])
				Assert.assertTrue("Record "+original_ids[i]+" should be locked but is not", dm.isLocked(original_ids[i]));
			else
				Assert.assertTrue("Record "+original_ids[i]+" should not be locked but is", !dm.isLocked(original_ids[i]));
		}
		
		// Be sure all locked records properly return an lock exception when called with bogus key:
		int num_thrown1 = 0, num_thrown2 = 0, num_thrown3 = 0;
		for(int i = 0; i < NUM_RECORDS ; i++)
		{
			if (is_locked[i])
			{
				// Test update 3 parms
				try
				{
					dm.update(original_ids[i],"updated content","bogus_key");
					Assert.assertTrue("InvalidLockException should have been thrown but was not",false);
				}
				catch (InvalidLockException ie)
				{
					num_thrown1++;		
				}
				
				// Test update 2 parms
				try
				{
					dm.update(original_ids[i],"updated content");
					Assert.assertTrue("InvalidLockException should have been thrown but was not",false);
				}
				catch (LockNotAvailableException ie)
				{
					num_thrown2++;		
				}
				
				// Test unlock
				try
				{
					dm.unlock(original_ids[i],"bogus_key");
					Assert.assertTrue("InvalidLockException should have been thrown but was not",false);
				}
				catch (InvalidLockException ie)
				{
					num_thrown3++;		
				}
			}
			else
			{
			}
		}
		Assert.assertTrue("Incorrect number of InvalidLockExceptions thrown",num_thrown1 == locked_count);
		Assert.assertTrue("Incorrect number of InvalidLockExceptions thrown",num_thrown2 == locked_count);
		Assert.assertTrue("Incorrect number of InvalidLockExceptions thrown",num_thrown3 == locked_count);
		
		
		
		//  Check update method
		String new_record = "new record";
		for(int i = 0; i < NUM_RECORDS ; i++)
		{
			if (is_locked[i])
			{
				dm.update(original_ids[i],new_record,lock_keys[i]);
				// Make sure the updated record actuall was updated:
				Assert.assertEquals(new_record,(String)dm.get(original_ids[i]));
			}
		}
		
		// Remove all locks:
		for(int i = 0; i < NUM_RECORDS ; i++)
		{
			if (is_locked[i])
				dm.unlock(original_ids[i],lock_keys[i]);
		}
		
		// Test that all locks are removed
		for(int i = 0; i < NUM_RECORDS ; i++)
		{
			try
			{
				// No exceptions should be thrown here
				dm.update(original_ids[i],"updated");
				
			}catch (LockNotAvailableException e)
			{
				Assert.assertTrue("LockNotAvaiableException was thrown when it should not",false);
			}
		}
		
		
		for(int i = 0; i < NUM_RECORDS ; i++)
		{
			//Assert.assertTrue(	"Not equals: original: \"" + original_ids[i] +
			//					"\"            returned: \"" + returned_ids[i] + "\"",
			//					original_ids[i].equals(returned_ids[i]) );
		}
	}

	
	public void test_put_and_get_methods()
	throws Exception
	{
		int NUM_RECORDS = 50;
		String original = null,returned = null;

		for(int i = 0; i < NUM_RECORDS ; i++)
		{
			original = getUniqueString();
			dm.put(	original, original + " content" );
		}
		for(int i = 0; i < NUM_RECORDS ; i++)
		{
			returned = (String)dm.get(original); 
			Assert.assertEquals(original + " content",returned);
		}
		
	}
	
	
	public void test_simple_filename_encode_decode()
	{
		final String original = "oai:dlese.org:DLESE-000-000-002-254";
		
		String encoded = dm.encodeFileName(original);
		String decoded = dm.decodeFileName(encoded);
		//prtln("original: " + original);
		//prtln(" encoded: " + encoded);
	    //prtln(" decoded: " + decoded);
		Assert.assertEquals(original,decoded);	
	}
	
	public void test_getNumRecords()
	{
		long num = dm.getNumRecords();
		String [] IDs = dm.getIDs();
		
		Assert.assertEquals(IDs.length,num);	
	}
		
	
	public void test_random_filename_encode_decode()
	{
		dm.decodeFileName("init first run"); // Init on first run. 
		
		String original,encoded,decoded;
		Random randgen = new Random(new Date().getTime());
		char c;
		
		final int ID_LEN = 15;
		final int NUM_IDS = 60;
		
		for(int i = 0; i < NUM_IDS; i++)
		{
			original = 	TestTools.getRandomCharsString(ID_LEN);
			encoded = dm.encodeFileName(original);
			decoded = dm.decodeFileName(encoded);
			//prtln("original: " + original);
			//prtln(" decoded: " + decoded);
			//prtln(" encoded: " + encoded);
			Assert.assertEquals(original,decoded);	
		}
		
	}
			
	private String getUniqueString()
	{
		return TestTools.getUniqueID();		
	}
	
	/* tearDown() gets called after running EACH test method in this class */
	protected void tearDown()
	{
		//prtln("tearDown()");		
	}
		
	
	private static void prtln(String s)
	{
		System.out.println(s);
	}
	
	/* Main method for running this single suite from the command line*/
	public static void main(String [] args)
	{
		junit.textui.TestRunner.run(suite());
	}
	
	
}
