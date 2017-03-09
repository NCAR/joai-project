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





/**

 *  A FileIndexingObserver that simply outputs messages to System out. Can be turned off by calling the static debug(false) method.

 *

 * @author     John Weatherley

 */

public class SimpleFileIndexingObserver implements FileIndexingObserver {

	private static boolean debug = true;

	

	

	String mp = null;





	/**  Constructor for the SimpleFileIndexingObserver object */

	public SimpleFileIndexingObserver() {

		mp = "SimpleFileIndexingObserver";

	}





	/**

	 *  Constructor for the SimpleFileIndexingObserver object

	 *

	 * @param  startUpMessage  A message that gets is sent to System out when this Object is created.

	 */

	public SimpleFileIndexingObserver(String startUpMessage) {

		mp = "SimpleFileIndexingObserver";

		prtln(startUpMessage);

	}





	/**

	 *  Constructor for the SimpleFileIndexingObserver object

	 *

	 * @param  messagePrefix   A identifier String that is inserted at the beginning of all messages output by

	 *      this SimpleFileIndexingObserver

	 * @param  startUpMessage  A message that gets is set to System out when this Object is created.

	 */

	public SimpleFileIndexingObserver(String messagePrefix, String startUpMessage) {

		mp = messagePrefix;

		prtln(startUpMessage);

	}





	/**

	 *  Outputs a message when indexing is complete.

	 *

	 * @param  status   The status code upon completion

	 * @param  message  A message describing how the indexer completed

	 */

	public void indexingCompleted(int status, String message) {

		if (status == FileIndexingObserver.INDEXING_COMPLETED_SUCCESS)

			prtln("Status INDEXING_COMPLETED_SUCCESS: " + message);

		if (status == FileIndexingObserver.INDEXING_COMPLETED_ITEM_ERROR)

			prtln("Status INDEXING_COMPLETED_ITEM_ERROR: " + message);		

		if (status == FileIndexingObserver.INDEXING_COMPLETED_ABORTED)

			prtln("Status INDEXING_COMPLETED_ABORTED: " + message);

		if (status == FileIndexingObserver.INDEXING_COMPLETED_ERROR)

			prtln("Status INDEXING_COMPLETED_ERROR: " + message);

		if (status == FileIndexingObserver.INDEXING_COMPLETED_DIR_DOES_NOT_EXIST)

			prtln("Status INDEXING_COMPLETED_DIR_DOES_NOT_EXIST: " + message);

		if (status == FileIndexingObserver.INDEXING_COMPLETED_DIR_READ_ERROR)

			prtln("Status INDEXING_COMPLETED_DIR_READ_ERROR: " + message);		

	}





	/**

	 *  Return a string for the current time and date, sutiable for display in log files and output to standout.

	 *

	 * @return    The dateStamp value

	 */

	private String getDateStamp() {

		return FileIndexingService.getDateStamp();

	}

	

	

	/**

	 *  Output a line of text to standard out, with datestamp, if debug is set to true.

	 *

	 * @param  s  The String that will be output.

	 */

	private final void prtln(String s) {

		if (debug) {

			System.out.println(getDateStamp() + " " + mp + ": " + s);

		}

	}





	/**

	 *  Sets the debug attribute of the object

	 *

	 * @param  db  The new debug value

	 */

	public static void setDebug(boolean db) {

		debug = db;

	}	



}



