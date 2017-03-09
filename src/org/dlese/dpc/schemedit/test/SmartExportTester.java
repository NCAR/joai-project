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
package org.dlese.dpc.schemedit.test;

import org.dlese.dpc.schemedit.*;
import org.dlese.dpc.schemedit.config.*;
import org.dlese.dpc.xml.*;
import org.dlese.dpc.util.*;

import org.dom4j.*;

import java.util.*;
import java.io.*;

/**
 *  <pre>
</pre>
 *
 *@author    ostwald
 */
public class SmartExportTester {

	private static boolean debug = true;
	private File destDir, srcDir;
	int numExported = 0;
	int numNotWritten = 0;
	int numNotExported = 0;
	int numDeleted = 0;

	SmartExportTester (String srcDirPath, String destDirPath) throws Exception {

		srcDir = new File (srcDirPath);
		if (!srcDir.exists() && srcDir.isDirectory())
			throw new Exception ("srcDir does not exist at " + srcDirPath);
		destDir = new File (destDirPath);
		if (!destDir.exists() && destDir.isDirectory())
			throw new Exception ("destDir does not exist at " + destDirPath);
	}
		
	public void export () {
		XMLFileFilter xmlFilter = new XMLFileFilter();
		File [] srcFiles = srcDir.listFiles(xmlFilter);  // simulate ResultDoc []
		List exportedFileNames = new ArrayList ();
		String errorMsg;
		
		numExported = 0;
		numNotWritten = 0;
		numNotExported = 0;
		numDeleted = 0;
		
		String xslPath = "/devel/ostwald/projects/schemedit-project/web/WEB-INF/xsl_files/cataloger-out.xslf";
		javax.xml.transform.Transformer transformer = null;
		try {
			transformer = XSLTransformer.getTransformer(xslPath);
		} catch (Exception e) {
			prtln (e.getMessage());
			return;
		}
		
		for (int i=0;i<srcFiles.length;i++) {
			File srcFile = srcFiles[i];
			String fileName = srcFile.getName();
			File destFile = new File (destDir, fileName);
			exportedFileNames.add (fileName);
			
			// here we validate src if necessary
			
			// check against dest dir contents
			if (destFile.exists() && srcFile.lastModified() <= destFile.lastModified()) {
				prtln ("srcFile is not newer than destFile - not overwriting");
				numExported++;
				numNotWritten++;
				continue;
			}
			// EXPORT file (write it to destDir)
			try {
				File exportedFile = new File(destDir, fileName);
				
				
				// StringBuffer xmlContent = Files.readFile(srcFile);
				
/* 				if (false) {
					String xml = XSLTransformer.transformFile (srcFile, transformer);
					prtln ("transformed xml:\n" + xml);
					Files.writeFile(xml, exportedFile);
				} */
				
				
				XSLTransformer.transformToFile (srcFile, exportedFile, transformer);
				
				
				numExported++;
				// addExportingMessage (id + " exported");
/* 			} catch (IOException ioe) {
				numNotExported++;
				errorMsg = "readFile error: " + ioe.getMessage();
				prtln(errorMsg);
				// throw new Exception (errorMsg); */
			} catch (Exception e) {
				numNotExported++;
				errorMsg = "remove Contributor error: " + e.getMessage();
				prtln(errorMsg);
			}
/* 			} catch (Throwable we) {
				numNotExported++;
				errorMsg = "writeFile error: " + we.getMessage();
				prtln(errorMsg);
				// throw new Exception (errorMsg);
			} */
		}
		
		// we've written all the files we need to write.
		// now delete all files in destDir that aren't on exportedFileNames list
		File [] destFiles = destDir.listFiles(xmlFilter);
		for (int i=destFiles.length - 1; i > -1; i--) {
			File destFile = destFiles[i];
			String destFileName = destFile.getName();
			if (!exportedFileNames.contains(destFileName)) {
				prtln ("deleting " + destFileName);
				destFile.delete();
				numDeleted++;
			}
		}
	}
	
	
	/**
	 *  The main program for the SmartExportTester class
	 *
	 *@param  args           The command line arguments
	 *@exception  Exception  Description of the Exception
	 */
	public static void main(String[] args) throws Exception {
		String srcDirPath = "/devel/ostwald/tmp/Exported/src";
		String destDirPath = "/devel/ostwald/tmp/Exported/dest";
		SmartExportTester set = new SmartExportTester (srcDirPath, destDirPath);
		set.export();
		prtln ("export completed");
		prtln ("numExported (should be size of dest) = " + set.numExported);
		prtln ("numNotWritten because dest version was current = " + set.numNotWritten);
		prtln ("numNotExported because of errors = " + set.numNotExported);
		prtln ("numDeleted from dest = " + set.numDeleted);
	}

	/**
	 *  Description of the Method
	 *
	 *@param  s  Description of the Parameter
	 */
	private static void prtln(String s) {
		if (debug) {
			System.out.println(s);
		}
	}
}

