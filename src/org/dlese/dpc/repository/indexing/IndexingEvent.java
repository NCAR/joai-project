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
 *  An event that describes an indexing action that has been requested.
 *
 * @author    John Weatherley
 */
public class IndexingEvent {
	private int type = 0;
	private String collectionKey = null;
	private CollectionIndexer collectionIndexer = null;

	/**  Indicates the index is ready to recieve indexing actions. */
	public final static int INDEXER_READY = 0;
	/**  Indicates the watcher should update it's list of collections. */
	public final static int UPDATE_COLLECTIONS = 1;
	/**  Indicates the watcher should begin indexing all collections. */
	public final static int BEGIN_INDEXING_ALL_COLLECTIONS = 2;
	/**  Indicates the watcher should abort indexing immediately. */
	public final static int ABORT_INDEXING = 3;
	/**  Indicates the watcher should update a given collection. */
	public final static int BEGIN_INDEXING_COLLECTION = 4;
	/**  Indicates the watcher should update its configuration to get any changes and (re)initialize. */
	public final static int CONFIGURE_AND_INITIALIZE = 5;	


	/**
	 *  Constructor for the IndexingEvent object
	 *
	 * @param  type               The event type
	 * @param  collectionKey      The collectionKey
	 * @param  collectionIndexer  The CollectionIndexer instance
	 */
	protected IndexingEvent(int type, String collectionKey, CollectionIndexer collectionIndexer) {
		this.collectionIndexer = collectionIndexer;
		this.type = type;
		this.collectionKey = collectionKey;
	}


	/**
	 *  Constructor for the IndexingEvent object
	 *
	 * @param  type               The event type
	 */
	public IndexingEvent(int type) {
		this.type = type;
	}


	/**
	 *  Gets the event type.
	 *
	 * @return    The type value
	 */
	public int getType() {
		return type;
	}


	/**
	 *  Gets the collectionKey attribute of the IndexingEvent object
	 *
	 * @return    The collectionKey value
	 */
	public String getCollectionKey() {
		return collectionKey;
	}


	/**
	 *  Gets the collectionIndexer attribute of the IndexingEvent object
	 *
	 * @return    The collectionIndexer value
	 */
	public CollectionIndexer getCollectionIndexer() {
		return collectionIndexer;
	}


	/**
	 *  A String representation of this event.
	 *
	 * @return    A String representation of this event.
	 */
	public String toString() {
		switch (type) {
						case ABORT_INDEXING:
							return "Abort indexing";
						case BEGIN_INDEXING_ALL_COLLECTIONS:
							return "Begin indexing all collections";
						case BEGIN_INDEXING_COLLECTION:
							return "Begin indexing collection '" + collectionKey + "'";
						case INDEXER_READY:
							return "Indexer ready";
						case UPDATE_COLLECTIONS:
							return "Update collections";
						case CONFIGURE_AND_INITIALIZE:
							return "Configure and initialize";							
		}

		return "";
	}
}

