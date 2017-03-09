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
package org.dlese.dpc.util;

import java.io.*;
//import java.util.*;
//import java.text.*;


public class FileCopy {

	static char dirSep = System.getProperty("file.separator").charAt(0);

	public static String getIDFromFilename(String filename) {
		String id = null;
		if (filename != null) {
			int period = filename.indexOf('.');
			int underscore = filename.indexOf('_');
			if ( (period > 0) && (underscore > 0) ) {
				int pos = (period > underscore) ? underscore : period;
				id = filename.substring(0, pos);
			}
			else if (period > 0) {
				id = filename.substring(0, period);
			}
			else {
				id = filename;
			}
		}	
		return id;
	}


	public static String getIDFromFilename(File file) {
		if (file != null) {
			return getIDFromFilename(file.getName());
		}
		return null;
	}


	public static boolean copyFile(File infile, File outfile) throws Exception {
		if (!infile.equals(outfile)) {
			try {
				FileInputStream in = new FileInputStream(infile);
				FileOutputStream out = new FileOutputStream(outfile);

				long size = infile.length();
				int bytes = 0;
				while (size > 0) {
					bytes = (size > 65536) ? 65536 : (int)size;
					byte [] b = new byte[bytes];
					in.read(b);
					out.write(b);
					size -= bytes;
					b = null;
				}
				in.close();
				out.close();
				in = null;
				out = null;
				return true;
			}
			catch (Exception e) {
				throw e;
			}
		}
		return false;
	}


	public static boolean moveFile(File infile, File outfile) throws Exception {
		try {
			if (copyFile(infile, outfile)) {
				infile.delete();
				return true;
			}
		}
		catch (Exception e) { 
			throw e;
		}
		return false;
	}
	
}
