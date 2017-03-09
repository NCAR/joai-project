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

import org.apache.lucene.document.Field;

public class SessionIndexingPlugin implements FileIndexingPlugin {
	
	private String sessionId = null;
	
	public SessionIndexingPlugin(String sessionId){
		this.sessionId = sessionId;		
	}
	
	public void addFields(
				File file, 
				org.apache.lucene.document.Document newDoc, 
				org.apache.lucene.document.Document existingDoc, 
				String docType, 
				String docGroup) {
		newDoc.add(new Field("indexSessionId", sessionId,Field.Store.YES, Field.Index.NOT_ANALYZED));
	}
}

