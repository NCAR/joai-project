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


	
public class CollectionIndexingSession {
	private String sessionId = null;
	protected CollectionIndexingSession(String collectionKey){
		sessionId = System.currentTimeMillis() + "-" + collectionKey;
	}
	
	protected CollectionIndexingSession() {}

	protected void setSessionId(String sessionId){
		this.sessionId = sessionId;
	}
	
	public String getSessionId(){
		return sessionId;	
	}
	
	public String getCollection(){
		return sessionId.substring(sessionId.indexOf('-')+1,sessionId.length());
	}

	public String toString() {
		return sessionId;	
	}
	
	/**
	 *  Checks equality of two CollectionIndexingSession objects.
	 *
	 * @param  o  The CollectionIndexingSession to compare to this
	 * @return    True iff the compared object is equal
	 */
	public boolean equals(Object o) {
		if (o == null || !(o instanceof CollectionIndexingSession))
			return false;
		try {
			return this.toString().equals(o.toString());
		} catch (Throwable e) {
			// Catch null pointer...
			return false;
		}
	}		
}

