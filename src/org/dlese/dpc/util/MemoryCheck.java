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

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 *  This class provides a utility to determine how much memory your application is using at any given time.
 *  Useful for determining the memory requirements of a given Object and for debugging possible memory leaks.
 *  See <a href="http://www.javaworld.com/javaworld/javatips/jw-javatip130.html"> JavaWorld article</a> for
 *  more info. Includes a sample main method that shows how it might be used. <p>
 *
 *  To get a snapshot of current memory first call <code>runGC()</code> and then call <code>getUsedMemory()</code>
 *  . <p>
 *
 *  Example code: <p>
 *
 *  <code>MemoryCheck.runGC();</code> <br>
 *  <code>Long currentMem = MemoryCheck.getUsedMemory();</code>
 *
 * @author     John Weatherley
 * @version    $Id: MemoryCheck.java,v 1.6 2009/03/20 23:34:00 jweather Exp $
 */
public class MemoryCheck {
	private static boolean debug = false;


	/**
	 *  The main program for the MemoryCheck class
	 *
	 * @param  args           The command line arguments
	 * @exception  Exception  If error
	 */
	public static void main(String[] args) throws Exception {
		// Warm up all classes/methods we will use
		runGC();
		getUsedMemory();

		// Array to keep strong references to allocated objects
		final int count = 100000;
		Object[] objects = new Object[count];

		long heap1 = 0;

		// Allocate count+1 objects, discard the first one
		for (int i = -1; i < count; ++i) {
			Object object = null;

			// Instantiate your data here and assign it to object

			object = new Object();
			//object = new Integer (i);
			//object = new Long (i);
			//object = new String ();
			//object = new byte [128][1]

			if (i >= 0)
				objects[i] = object;
			else {
				object = null; // Discard the warm up object
				runGC();
				heap1 = getUsedMemory(); // Take a before heap snapshot
			}
		}

		runGC();
		long heap2 = getUsedMemory(); // Take an after heap snapshot:

		final int size = Math.round(((float) (heap2 - heap1)) / count);
		System.out.println("'before' heap: " + heap1 +
			", 'after' heap: " + heap2);
		System.out.println("heap delta: " + (heap2 - heap1) +
			", {" + objects[0].getClass() + "} size = " + size + " bytes");

		for (int i = 0; i < count; ++i)
			objects[i] = null;
		objects = null;
	}


	/**
	 *  Gets the current amount of memory being used by this app, in bytes. For best accuracy you should call
	 *  <code>runGC()</code> prior to calling this method.
	 *
	 * @return    The current amount of memory being used by the JVM.
	 */
	public static long getUsedMemory() {
		return s_runtime.totalMemory() - s_runtime.freeMemory();
	}


	/**
	 *  Gets the current amount of memory being used by this app, in megabytes. For best accuracy you should call
	 *  <code>runGC()</code> prior to calling this method.
	 *
	 * @return    The current amount of memory being used by the JVM.
	 */
	public static String getUsedMemoryInMegs() {
		return (getUsedMemory() / 1048576) + "M";
	}


	/**
	 *  Runs object finization and the garbage collector several times. This tends to be more effective than juse
	 *  calling garbage collection once.
	 */
	public static void runGC() {
		try {
			// It helps to call Runtime.gc()
			// using several method calls:
			for (int r = 0; r < 4; ++r)
				_runGC();
		} catch (Exception e) {
			System.err.println("MemoryCheck.runGC() threw exception: " + e);
		}
	}


	/**
	 *  Actually does the gc
	 *
	 * @exception  Exception  if error
	 */
	private static void _runGC() throws Exception {
		long usedMem1 = getUsedMemory();
		long usedMem2 = Long.MAX_VALUE;
		prtln("Running gc - usedMem1: " + usedMem1 + " usedMem2: " + usedMem2);
		for (int i = 0; (usedMem1 < usedMem2) && (i < 500); ++i) {
			s_runtime.gc();
			s_runtime.runFinalization();
			Thread.currentThread().yield();

			usedMem2 = usedMem1;
			usedMem1 = getUsedMemory();
			prtln("Ran gc - usedMem1: " + usedMem1 + " usedMem2: " + usedMem2);
		}
	}


	private final static Runtime s_runtime = Runtime.getRuntime();



	// ------------------------------------------------------------------

	/**
	 *  Return a string for the current time and date, sutiable for display in log files and output to standout:
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
	private final static void prtlnErr(String s) {
		System.err.println(getDateStamp() + " MemoryCheck: " + s);
	}


	/**
	 *  Output a line of text to standard out, with datestamp, if debug is set to true.
	 *
	 * @param  s  The String that will be output.
	 */
	private final static void prtln(String s) {
		if (debug) {
			System.out.println(getDateStamp() + " MemoryCheck: " + s);
		}
	}

}

