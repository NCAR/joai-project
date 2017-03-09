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

import java.util.*;

import org.apache.lucene.search.*;
import org.apache.lucene.analysis.*;
import org.apache.lucene.document.*;


public interface LuceneIndex {

	// LuceneIndex provides a simple intuitive abstraction of the 
	// Lucene API as a single interface for creating, managing and
	// searching a Lucene index.

	public static final int STOP_ANALYSIS = 0;
	public static final int SIMPLE_ANALYSIS = 1;
	public static final int STANDARD_ANALYSIS = 2;
	public static final int WHITESPACE_ANALYSIS = 3;
	
	public void use(String location)
		throws InvalidIndexException, IndexInitializationException;
	
	public void create(String location, String defaultDocField, int analysisType)
		throws IndexInitializationException;

	public void create(String location, String defaultDocField, String [] stopWords, int analysisType)
		throws IndexInitializationException;
	
	public boolean tryConfig(int AnalysisType, String defaultDocField, String [] stopWords, String indexReader);

	public boolean addDoc(Document doc);
	 
	public boolean addDocs(Document [] docs);

	public boolean removeDocs(String field, String value);
	
	public boolean removeDocs(String field, String [] values);

	public boolean update(String field, String [] values, Document [] docs);
		
	public ResultDoc [] searchDocs(String query);

	public ResultDoc [] searchDocs(String query, String defaultField);
	
	public ResultDoc [] searchDocs(String query, String defaultField, Filter filter);
	
	public ResultDoc [] searchDocs(String query, String defaultField, Collector collector);

	public ResultDoc [] searchDocs(String query, String defaultField, Filter filter, Collector collector);

	public int numDocs(String query);

	public int numDocs();

	public List listDocs();

	public List listDocs(String field, String value);

	public List listTerms();
	
	public List getFields();
	
	public Map getTermLists();
	
	public List listStopWords ();

	public int getTermFrequency(String value);

	public int getTermFrequency(String field, String value);
	

}

