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
import org.apache.lucene.index.*;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Filter;
import java.util.*;

import org.dlese.dpc.index.reader.*;

import java.io.*;

/**
 *  Contains the arbitraty attributes, the index and the users query, making them available in DocReaders.
 *
 * @author    John Weatherley
 * @see       org.dlese.dpc.index.reader.DocReader
 */
public final class ResultDocConfig {

	/**  The index being used */
	public SimpleLuceneIndex index = null;
	/**  The query used */
	public String query = null;
	/**  The Query used */
	public Query luceneQuery = null;
	/**  The Lucene serach Filter used */
	public Filter filter = null;
	/**  The attributes that will be made availalbe to DocReaders at search time. */
	public HashMap attributes = null;


	/**
	 *  Constructor for the ResultDocConfig.
	 *
	 * @param  myQuery              The query that was used (as a String)
	 * @param  luceneQueryObj       The Lucene Query object used	 
	 * @param  myFilter             The Filter used to refine the search, or null if none used.
	 * @param  docReaderAttributes  Attributes that will be made availalbe to DocReaders. May be null.
	 * @param  myIndex              A pointer to the index that was searched over.
	 */
	public ResultDocConfig(String myQuery, Query luceneQueryObj, Filter myFilter, HashMap docReaderAttributes, SimpleLuceneIndex myIndex) {
		index = myIndex;
		query = myQuery;
		luceneQuery = luceneQueryObj;
		filter = myFilter;
		attributes = docReaderAttributes;

	}


	/**
	 *  Minimum constructor for the ResultDocConfig.
	 *
	 * @param  myIndex  A pointer to the index that was searched over.
	 */
	public ResultDocConfig(SimpleLuceneIndex myIndex) {
		index = myIndex;
		query = "";
		filter = null;
		attributes = null;
	}
}


