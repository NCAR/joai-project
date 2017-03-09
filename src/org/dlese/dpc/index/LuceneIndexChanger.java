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
import java.net.*;
import java.util.*;

class LuceneIndexChanger implements Serializable {

	String reader;

	private ArrayList listeners;
	
	/**
	 * Construct the DataEvent object and invoke the appropriate method
	 * of all the listeners to this DataManager.
	 *
	 * @param dataToAdd	The list of OIDs for data added to this manager
	 */
	void notifyListeners(String newReader) {
		// make LuceneIndexChangedEvent with a new reader file using long data
		LuceneIndexChangedEvent event = new LuceneIndexChangedEvent(this);
		
		for (int i=0; i<listeners.size(); i++) {
			try {
				((LuceneIndexChangeListener)listeners.get(i)).indexChanged(event);
			}
			catch (Throwable t) {
				System.err.println("Unexpected exception occurred while notifying listeners...");
			}
		}
	}

	
	/**
	 * Add a <code>DataListener</code> to this <code>DataManager</code>.
	 *
	 * @param listener	The listener to add
	 */
	void addListener(LuceneIndexChangeListener listener) {
		if (listener != null) {
			if (listeners == null) {
				listeners = new ArrayList();
			}
			else if (listeners.contains(listener)) {
				return;
			}

			listeners.add(listener);
		}
	}

	
	/*********************************************************************************/
	
	void removeListener(LuceneIndexChangeListener listener) {
		if (listener != null) {
			int index = listeners.indexOf(listener);
			if (index > -1) {
				try {
					listeners.remove(index);
				}
				catch (IndexOutOfBoundsException ioobe) {
					return;
				}
			}
		}
	}



}
