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
package org.dlese.dpc.repository.indexing;

import org.dlese.dpc.repository.*;

import java.io.*;
import java.util.*;

import org.dlese.dpc.index.*;
import org.dlese.dpc.index.writer.*;
import java.text.*;

/**
 *  Manage collections of records in a DDS repository.
 *
 * @author    John Weatherley
 */
public class CollectionIndexer {
	private static boolean debug = false;

	private RepositoryManager repositoryManager;
	private IndexingManager indexingManager;


	/**
	 *  Constructor for the CollectionIndexer object
	 *
	 * @param  repositoryManager  The RepositoryManager
	 * @param  indexingManager    The IndexingManager
	 */
	public CollectionIndexer(RepositoryManager repositoryManager, IndexingManager indexingManager) {
		this.repositoryManager = repositoryManager;
		this.indexingManager = indexingManager;
	}


	/**
	 *  Put a collection in the repository. If the collection key does not exist, the collection will be created
	 *  otherwise it will be updated with this new information.
	 *
	 * @param  collectionKey       The collection key, for example 'dcc'
	 * @param  format              The XML format for the records that will reside in the collection
	 * @param  name                A display name for the collection
	 * @param  description         A long description for the collection
	 * @param  additionalMetadata  A text or XML string to be inserted into the additionalMetadata element of the
	 *      collection record, or null for none
	 * @exception  Exception       If error
	 */
	public void putCollection(String collectionKey, String format, String name, String description, String additionalMetadata)
		 throws Exception {
		printPrivateStatusMessage("Adding collection '" + name + "', key '" + collectionKey + "'");
		repositoryManager.putCollection(collectionKey, format, name, description, additionalMetadata);
	}


	/**
	 *  Delete a collection and all its records from a DDS repository.
	 *
	 * @param  collectionKey  The key indicatint the collection to delete
	 * @exception  Exception  If error
	 */
	public void deleteCollection(String collectionKey) throws Exception {
		if (collectionKey.equalsIgnoreCase("collect")) {
			String msg = "Permission denied to delete collection 'collect': it is managed internally";
			printPrivateStatusMessage(msg);
			throw new Exception(msg);
		}
		printPrivateStatusMessage("Deleting collection '" + collectionKey + "'");
		repositoryManager.deleteCollection(collectionKey);
	}


	/**
	 *  Get a list of the collections currently configured.
	 *
	 * @return    A List of collection key Strings, for example 'dcc'
	 */
	public List getConfiguredCollections() {
		List collections = repositoryManager.getConfiguredSets();

		// Do not advertise the "collect" collection:
		if (collections != null)
			collections.remove("collect");
		return collections;
	}


	/**
	 *  Gets the collectionConfigured attribute of the CollectionIndexer object
	 *
	 * @param  collectionKey  The key for the collection
	 * @return                The collectionConfigured value
	 */
	public boolean isCollectionConfigured(String collectionKey) {
		//prtln("isCollectionConfigured() '" + collectionKey + "'");
		return repositoryManager.isSetConfigured(collectionKey);
	}


	/**
	 *  Put a record in a collection, adding or replacing the given record in the repository.
	 *
	 * @param  recordXml      The XML for this record
	 * @param  collectionKey  The collection this record should be put
	 * @param  id             The ID of the record - ignored if the ID can be derived from the record XML
	 * @param  session        An indexing session ID - used for deleting records later after a new indexing
	 *      session occurs
	 * @exception  Exception  If error
	 */
	public void putRecord(
	                      String recordXml,
	                      String collectionKey,
	                      String id,
	                      CollectionIndexingSession session) throws Exception {
		SetInfo setInfo = repositoryManager.getSetInfo(collectionKey);
		if (setInfo == null)
			throw new Exception("Collection '" + collectionKey + "' is not configured");

		//printPrivateStatusMessage("Adding record ID '" + id + "', collection '" + collectionKey + "'");
		SessionIndexingPlugin plugIn = new SessionIndexingPlugin(session.getSessionId());
		repositoryManager.putRecord(recordXml, setInfo.getFormat(), collectionKey, id, plugIn, false);
	}


	/**
	 *  Deletes a record in the repository.
	 *
	 * @param  id             Id of the record.
	 * @exception  Exception  If error
	 */
	public void deleteRecord(String id) throws Exception {
		//printPrivateStatusMessage("Deleting record ID '" + id + "'");
		repositoryManager.deleteRecord(id);
	}


	/**
	 *  Gets the newCollectionIndexingSession attribute of the CollectionIndexer object
	 *
	 * @param  collectionKey  The collection
	 * @return                A new indexing session ID
	 */
	public CollectionIndexingSession getNewCollectionIndexingSession(String collectionKey) {
		return new CollectionIndexingSession(collectionKey);
	}


	/**
	 *  Post a status message to the indexing process to let admins know the current indexing status.
	 *
	 * @param  msg  The message to post
	 */
	public void printStatusMessage(String msg) {
		indexingManager.addIndexingMessage(msg);
	}


	private void printPrivateStatusMessage(String msg) {
		printStatusMessage("Indexer: " + msg);
	}


	/**
	 *  Gets the existingCollectionIndexingSession attribute of the CollectionIndexer object
	 *
	 * @param  sessionId  The session ID
	 * @return            The existingCollectionIndexingSession value
	 */
	protected CollectionIndexingSession getExistingCollectionIndexingSession(String sessionId) {
		CollectionIndexingSession sess = new CollectionIndexingSession();
		sess.setSessionId(sessionId);
		return sess;
	}


	/**
	 *  Delete all records that DO NOT have the given session ID.
	 *
	 * @param  currentSession  The session ID
	 */
	public void deletePreviousSessionRecords(CollectionIndexingSession currentSession) {
		SimpleLuceneIndex index = repositoryManager.getIndex();

		List indexedSessions = index.getTerms("indexSessionId");

		if (indexedSessions != null) {
			for (int i = 0; i < indexedSessions.size(); i++) {
				CollectionIndexingSession indexedSession = getExistingCollectionIndexingSession((String) indexedSessions.get(i));
				//prtln("Session ID found: " + indexedSession + " collection: " + indexedSession.getCollection());

				if (currentSession.getCollection().equals(indexedSession.getCollection()) && !currentSession.equals(indexedSession)) {
					int numBefore = index.getNumDocs();
					//String msg = "Deleting records for session: " + indexedSession + ". Num docs: " + index.getNumDocs();
					//prtln(msg);
					//printPrivateStatusMessage(msg);
					index.removeDocs("indexSessionId", indexedSession.getSessionId());

					String msg = "Deleted records for session: " + indexedSession + ". Num deleted: " + (numBefore - index.getNumDocs());
					printPrivateStatusMessage(msg);
				}
			}
		}
	}



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
		System.err.println(getDateStamp() + " CollectionIndexer Error: " + s);
	}


	/**
	 *  Output a line of text to standard out, with datestamp, if debug is set to true.
	 *
	 * @param  s  The String that will be output.
	 */
	private final static void prtln(String s) {
		if (debug) {
			System.out.println(getDateStamp() + " CollectionIndexer: " + s);
		}
	}


	/**
	 *  Sets the debug attribute of the CollectionIndexer object
	 *
	 * @param  db  The new debug value
	 */
	public final static void setDebug(boolean db) {
		debug = db;
	}

}

