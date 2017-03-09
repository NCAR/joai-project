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

import org.apache.lucene.document.*;

import java.util.*;
import java.io.*;


public class FileIndexingServiceData {
	private Document newDoc = null;
	private HashMap docsToRemove = null;
	
	public FileIndexingServiceData() {}
	
	public void setDoc(Document documentToAdd){
		newDoc = documentToAdd;	
	}
	
	public Document getDoc(){
		return newDoc;	
	}

	public HashMap getDocsToRemove(){
		return docsToRemove;	
	}
	
	public void addDocToRemove(String field, String value){
		if(docsToRemove == null)
			docsToRemove = new HashMap();
		ArrayList values = (ArrayList)docsToRemove.get(field);
		if(values == null)
			values = new ArrayList();
		values.add(value);
		docsToRemove.put(field,values);	
	}
	
	public void clearAll(){
		newDoc = null;
		if(docsToRemove != null)
			docsToRemove.clear();
	}
}
