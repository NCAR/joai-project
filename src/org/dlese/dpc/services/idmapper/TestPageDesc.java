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
import java.io.FileReader;

import org.dlese.dpc.util.DpcErrors;



class TestPageDesc {

public static void main( String[] args) {
	new TestPageDesc( args);
}


void badparms( String msg) {
	prtln("Error: " + msg);
	prtln("Parms: timeoutSeconds infile");
	prtln("Each line of infile contains a url to be tested.");
	prtln("Blank lines and those starting with \"#\" are ignored.");
	prtln("Example:");
	prtln("    java org.dlese.dpc.services.idmapper.TestPageDesc infile");
	System.exit(1);
}




TestPageDesc( String[] args) {
	if (args.length != 2) badparms("wrong num parms");
	int iarg = 0;
	String timeoutstg = args[iarg++];
	String infname = args[iarg++];

	int timeout = 0;
	try { timeout = Integer.parseInt( timeoutstg); }
	catch( NumberFormatException nfe) {
		badparms("invalid timeout: " + timeoutstg);
	}

	String collKey = "someCollKey";
	String metastyle = "someMetastyle";
	String dirPath = "someDirPath";
	String fileName = "someFileName";
	String[] urlOnlyTests = null;
	int bugs = 100;
	int threadId = 9999;


	try {
		BufferedReader rdr = new BufferedReader( new FileReader( infname));
		while (true) {
			String inline = rdr.readLine();
			if (inline == null) break;
			String urlstg = inline.trim();
			if (urlstg.length() > 0 && ! urlstg.startsWith("#")) {
				prtln("\n===== begin test of \"" + urlstg + "\" =====\n");
				ResourceDesc rsd = new ResourceDesc( collKey, metastyle,
					dirPath, fileName, urlOnlyTests);

				PageDesc page = new PageDesc(
					rsd, "test/xpath", "primary-url", urlstg,
					PageDesc.CKSUMTYPE_STD);
				rsd.addPage( page);

				page.processPage( bugs, threadId, timeout);
				prtln("\nTestPageDesc: respcode: " + page.respcode
					+ " (" + DpcErrors.getMessage( page.respcode) + ")"
					+ "  url: \"" + urlstg + "\""
					+ "  pagewarning: " + page.pagewarning);

				// Cannot print or use the page contents,
				// since the contents are cleared at end of processPage.
			}
		}
	}
	catch( Exception exc) {
		prtln("TestPageDesc: caught: " + exc);
		exc.printStackTrace();
	}
} // end constructor





void prtln( String msg) {
	System.out.println( msg);
}


} // end class TestPageDesc
