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
package org.dlese.dpc.index;

import java.io.*;
import java.util.*;
import java.text.*;

/**
 *  This class simulates saving, moving and deleting records from the records
 *  directory. NOTE: Should NOT be used when pointing at the DLESE metadata
 *  repository!
 *
 * @author    John Weatherley
 */
public final class FileMoveTester
{
	private boolean debug = true;
	
	final static Random randgen = new Random(new Date().getTime());

	private File inputFilesDir = null;
	private File tempFilesDir = null;
	private String tempFilesDirLoc = null;
	private boolean terminateMoveFilesThread = false;


	/**
	 *  Constructor for the FileMoveTester object
	 *
	 * @param  inputFileLoc  DESCRIPTION
	 * @param  tempFileLoc   DESCRIPTION
	 */
	public FileMoveTester(String inputFileLoc, String tempFileLoc) {
		prtln("FileMoveTester started.");

		if (inputFileLoc.indexOf("catalog_data") != -1 ||
				inputFileLoc.indexOf("records_done") != -1 ||
				inputFileLoc.indexOf("dtd_version") != -1) {
			prtln("Warning: FileMoveTester detected that the input files directory might be the DLESE metadata repository.");
			prtln("FileMoveTester should used on test files only! Exiting now without processing...");
			return;
		}

		inputFilesDir = new File(inputFileLoc);
		if (!inputFilesDir.isDirectory()) {
			prtln("FileMoveTester error: inputFileLoc is not a directory!");
			return;
		}

		tempFilesDir = new File(tempFileLoc);
		if (!tempFilesDir.exists() && tempFilesDir.mkdirs()) {
			prtln("FileMoveTester created directory \"" + tempFilesDir.getAbsolutePath() + "\"");
		}

		if (!tempFilesDir.isDirectory()) {
			prtln("FileMoveTester error: tempFilesDirLoc is not a directory!");
			return;
		}

		new MoveFilesThread().start();
	}


	/**  DESCRIPTION */
	private void moveRandomFiles() {
		File fromDir;
		File toDir;

		if (getRandomBoolean()) {
			fromDir = inputFilesDir;
			toDir = tempFilesDir;
		}
		else {
			fromDir = tempFilesDir;
			toDir = inputFilesDir;
		}

		File[] fromFiles = null;

		//prtln("Moving files from " + fromDir.getAbsolutePath());
		//prtln("Moving files to " + toDir.getAbsolutePath());

		// Move 1 to 4 files...
		for (int i = 0; i < getRandomIntBetween(1, 5); i++) {
			//prtln("Moving Files...");
			fromFiles = fromDir.listFiles();
			if (!(fromFiles.length > 0))
				return;

			if (getRandomBoolean())
				mvFileViaFS(fromFiles[getRandomIntBetween(0, fromFiles.length)], toDir);
			else
				mvFileViaCopy(fromFiles[getRandomIntBetween(0, fromFiles.length)], toDir);
		}
	}


	/**
	 *  DESCRIPTION
	 *
	 * @param  file     DESCRIPTION
	 * @param  destDir  DESCRIPTION
	 */
	private final void mvFileViaFS(File file, File destDir) {
		File destination = new File(destDir.getAbsolutePath() + "/" + file.getName());
		prtln("\nfs mv file from: " + file.getAbsolutePath());
		prtln("fs mv file to: " + destination.getAbsolutePath() + "\n");
		file.renameTo(destination);
	}


	/**
	 *  DESCRIPTION
	 *
	 * @param  file     DESCRIPTION
	 * @param  destDir  DESCRIPTION
	 */
	private final void mvFileViaCopy(File file, File destDir) {
		File destination = new File(destDir.getAbsolutePath() + "/" + file.getName());
		prtln("\ncp move file from: " + file.getAbsolutePath());
		prtln("cp move file to: " + destination.getAbsolutePath() + "\n");

		try {
			FileReader in = new FileReader(file);
			FileWriter out = new FileWriter(destination);
			int c;

			while ((c = in.read()) != -1)
				out.write(c);

			in.close();
			out.close();
		} catch (Exception e) {
			prtln("problem copying file: " + e);
			return;
		}

		file.delete();
	}


	/**  DESCRIPTION */
	public void stop() {
		terminateMoveFilesThread = true;
	}


	/**
	 *  DESCRIPTION
	 *
	 * @author    John Weatherley
	 */
	public class MoveFilesThread extends Thread
	{
		/**  Constructor for the SimpleThread object */
		public MoveFilesThread() {
			this.setDaemon(true);
			prtln("MoveFilesThread()");
			// Terminate with app
		}


		/**  Main processing method for the SimpleThread object */
		public void run() {
			prtln("MoveFilesThread starting");
			while (!terminateMoveFilesThread) {

				try {
					moveRandomFiles();
					sleep(getRandomIntBetween(750, 6000));
				} catch (InterruptedException e) {}
			}
			prtln("MoveFilesThread killed ");
		}
	}


	/**
	 *  Generate a random integer >= low and < high
	 *
	 * @param  low   DESCRIPTION
	 * @param  high  DESCRIPTION
	 * @return       The randomIntBetween value
	 */
	private static int getRandomIntBetween(int low, int high) {
		if (low >= high) {
			System.err.println("first param must be >= the second param");
			return 0;
		}
		int total = high - low;
		return low + ((int)(Math.abs(randgen.nextLong() % total)));
	}


	/**
	 *  Gets the randomBoolean attribute of the FileMoveTester class
	 *
	 * @return    The randomBoolean value
	 */
	private static boolean getRandomBoolean() {
		return (getRandomIntBetween(0, 2) == 0);
	}


	//------------------- Utility methods -------------------
	
	/**
	 *  Return a string for the current time and date, sutiable for display in log
	 *  files and output to standout:
	 *
	 * @return    The dateStamp value
	 */
	public static String getDateStamp() {
		return
				new SimpleDateFormat("MMM d, yyyy h:mm:ss a zzz").format(new Date());
	}


	/**
	 *  Output a line of text to error out, with datestamp.
	 *
	 * @param  s  The text that will be output to error out.
	 */
	private final void prtlnErr(String s) {
		System.err.println(getDateStamp() + " " + s);
	}



	/**
	 *  Output a line of text to standard out, with datestamp, if debug is set to true.
	 *
	 * @param  s  The String that will be output.
	 */
	private final void prtln(String s) {
		if (debug)
			System.out.println(getDateStamp() + " " + s);
	}



	public final void setDebug(boolean db) {
		debug = db;
	}

}

