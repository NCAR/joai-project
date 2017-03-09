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

import org.dlese.dpc.junit.TestTools;

import junit.framework.*;
import java.util.Date;
import java.util.Random;
import java.util.Enumeration;
import java.util.*;
//import com.meterware.httpunit.*;

//import org.dlese.dpc.oai.datamgr.*;



public class DDSPerformanceTests extends TestCase
{

	
	public static Test suite() {
		// Use java reflection to run all test methods in this class:
		prtln("Running DDS Performance tests...");
		return new TestSuite(DDSPerformanceTests.class);
	} 
	
	static int recNum = 0;
	
	/* setUp() gets called prior to running EACH test method in this class */
	protected void setUp()
	throws Exception
	{
		prtln(".");// introduce some space between the tests
	}
	
	
	
	public void test_dds_searches()
	throws Exception
	{
		//WebConversation wc = new WebConversation();
		//WebResponse wr = wc.getResponse( "http://quake.dpc.ucar.edu:9187/dds_testing/admin/query.do" );
		//prtln( wr.getText() );		
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
