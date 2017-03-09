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
package org.dlese.dpc.schemedit.repository;

import org.dlese.dpc.schemedit.*;
import org.dlese.dpc.schemedit.config.CollectionRegistry;
import org.dlese.dpc.schemedit.config.CollectionConfig;

import org.dlese.dpc.repository.*;
import org.dlese.dpc.index.*;

import java.util.*;
import java.io.*;
import java.text.*;

/**
 *  RepositoryIndexingObsever initializes IDGenerators for each collection after
 *  reindexing the entire repository (all collections). This is necessary
 *  because if metadata records have been added by hand (outside the DCS), then
 *  the existing "nextID" may no longer be valid.
 *
 * @author    ostwald<p>
 *
 *
 */
public class CollectionIndexingObserver implements FileIndexingObserver {

	private static boolean debug = true;
	CollectionRegistry collectionRegistry;
	String collectionKey;
	RepositoryManager rm;


	/**
	 *  Constructor for the CollectionIndexingObserver object
	 *
	 * @param  collectionRegistry  the collectionRegistry (contains IDGenerators)
	 * @param  rm                  the repositoryManager
	 */
	public CollectionIndexingObserver(String collectionKey, CollectionRegistry collectionRegistry, RepositoryManager rm) {
		this.collectionKey = collectionKey;
		this.collectionRegistry = collectionRegistry;
		this.rm = rm;
		prtln ("starting to index collection");
	}


	/**
	 *  Called when indexing is complete.
	 *
	 * @param  status   status code returned by indexer
	 * @param  message  indexing message
	 */
	public void indexingCompleted(int status, String message) {
		// prtln("* Indexing Completed: " + message + " *");
		prtln("Indexing Completed for " + collectionKey);
		// prtln("Reinitializing IDGenerator for " + collectionKey);
		try {
			CollectionConfig collectionConfig = collectionRegistry.getCollectionConfig(collectionKey, false);
			if (collectionConfig == null)
				throw new Exception ("collection config not found for " + collectionKey);
			collectionRegistry.initializeIDGenerator (collectionConfig, rm.getIndex());
		} catch (Throwable e) {
			prtlnErr ("IDGenerator NOT re-initialized: " + e.getMessage());
		}

	}
	// ---------------------- Debug info --------------------

	/**
	 *  Return a string for the current time and date, sutiable for display in log
	 *  files and output to standout:
	 *
	 * @return    The dateStamp value
	 */
	protected final static String getDateStamp() {
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
	 *  Output a line of text to standard out, with datestamp, if debug is set to
	 *  true.
	 *
	 * @param  s  The String that will be output.
	 */
	private final void prtln(String s) {
		if (debug) {
			System.out.println(getDateStamp() + " CollectionIndexingObserver: " + s);
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

