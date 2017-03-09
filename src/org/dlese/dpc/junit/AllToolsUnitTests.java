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

import junit.framework.*;
import org.dlese.dpc.datamgr.*;
import java.util.*;
import java.text.*;

/**
 *  This class packages and runs unit tests for all DLESE Tools classes that
 *  have unit tests written for them.
 *
 * @author    John Weatherley
 */
public class AllToolsUnitTests
{

	/**
	 *  A unit test suite for JUnit
	 *
	 * @return    The test suite
	 */
	public static Test suite() {

		String start = new SimpleDateFormat("MMM d, h:mm:ss a zzz").format(new Date());

		prtln("\n\n##### Running all JUnit tests for joai-project  ... (" + start + ") #####\n");

		TestSuite suite = new TestSuite();
		
		suite.addTest(HarvestAPITests.suite());
		//suite.addTest(DDSPerformanceTests.suite());
		//suite.addTest(OAIRecordTest.suite());
		// Add test suites for each class here:
		//suite.addTest(OAILuceneDataManagerTest.suite());
		//suite.addTest(SerializedDataManagerTest.suite());
		//suite.addTest(SerializedDataManagerPerformanceTests.suite());

		return suite;
	}


	/*
	 *  Main method for running this set of suites from the command line
	 */
	/**
	 *  The main program for the AllToolsUnitTests class
	 *
	 * @param  args  The command line arguments
	 */
	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}


	/**
	 *  DESCRIPTION
	 *
	 * @param  s  DESCRIPTION
	 */
	private static void prtln(String s) {
		System.out.println(s);
	}

}

