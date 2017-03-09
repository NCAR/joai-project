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
package org.dlese.dpc.junit;

import org.dlese.dpc.oai.harvester.Harvester;
import org.dlese.dpc.oai.harvester.OAIChangeListener;
import org.dlese.dpc.oai.harvester.OAIChangeListenerImpl;
import org.dlese.dpc.oai.harvester.SimpleHarvestMessageHandler;

import org.dlese.dpc.junit.TestTools;

import junit.framework.*;
import java.util.Date;
import java.util.Random;
import java.util.Enumeration;
import java.util.*;
//import com.meterware.httpunit.*;

//import org.dlese.dpc.oai.datamgr.*;



public class HarvestAPITests extends TestCase
{

	
	public static Test suite() {
		// Use java reflection to run all test methods in this class:
		prtln("Running Harvest API tests...");
		return new TestSuite(HarvestAPITests.class);
	} 
	
	static int recNum = 0;
	
	/* setUp() gets called prior to running EACH test method in this class */
	protected void setUp()
	throws Exception
	{
		prtln(".");// introduce some space between the tests
	}
	
	
	
	public void test_OAIChangeListener()
	throws Exception
	{
		OAIChangeListener oaiChangeListener = new OAIChangeListenerImpl();
		SimpleHarvestMessageHandler simpleHarvestMessageHandler = null;//new SimpleHarvestMessageHandler();
		int timeOutMilliseconds = 6000;
		
		String baseURL = "http://localhost/oai/provider";
		String metadataPrefix = "oai_dc";
		String setSpec = null;
		Date from = null;
		Date until = null;
		String outdir = "C:\\oai_harvest\\test_harvest"; // Use null or empty to return String [][]
		boolean splitBySet = false;
		String zipName = null; // Use null not to zip
		String zipDir = null;
		boolean writeHeaders = false;
		boolean harvestAll = false;
		boolean harvestAllIfNoDeletedRecord = false;
		
		Harvester harvester = new Harvester(simpleHarvestMessageHandler, oaiChangeListener, timeOutMilliseconds);
		
		String[][] res = harvester.doHarvest(baseURL,
				metadataPrefix, setSpec, from, until, outdir, splitBySet, zipName, zipDir, writeHeaders, harvestAll, harvestAllIfNoDeletedRecord);
		
		// Output the records if available (when outdir is null):
		if(res != null) {
			for( int i = 0; i < res.length; i++) {
				String [] record = res[i];
				prtln("\nIdentifier '" + record[0] + "' content:\n'" + record[1] + "'");	
			}
		}
		prtln( "test OAIChangeListener..." );		
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
