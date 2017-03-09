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
import java.util.*;

import org.apache.lucene.index.*;
import org.apache.lucene.store.*;
import org.apache.lucene.search.*;
import org.apache.lucene.analysis.*;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.util.Version;
import org.apache.lucene.store.instantiated.InstantiatedIndexReader;
import org.apache.lucene.document.*;
import org.apache.lucene.queryParser.*;
import java.text.SimpleDateFormat;

import org.dlese.dpc.util.*;

/**
 *  A simple API for searching, reading and writing <a href="http://lucene.apache.org/"> Lucene</a> indexes.
 *
 * @author    John Weatherley, Dave Deniman
 * @see       ResultDoc
 * @see       org.dlese.dpc.index.reader.DocReader
 */
public final class SimpleLuceneIndex {

	/*
	To Do (June 2008):
	- Refactor listDocs methods or consider alternatives to eliminate
	getting all Documents - jconsole tests show spike in memory with large result sets.
	- Look at all instances where Documents are referenced to make sure memory is not unnecessarily locked up
	*/
	/**  The version of Lucene. */
	private final static Version luceneVersion = Version.LUCENE_30;

	/**  The maximum number of hits that can be returned in a search */
	private final int MAX_NUM_HITS = 10000000;

	/**  The number of milliseconds to wait after the last index update before running the optimizer (30 minutes) */
	private final static long OPTIMIZE_DELAY_TIME = 1800000;
	//private final static long OPTIMIZE_DELAY_TIME = 2000; // test every 3 seconds...

	/**
	 *  The number of records updated before the optimizer is run. Use -1 to never run the optimizer based on
	 *  number of records updated (optimizer will be run based on time elapsed since the last update only)
	 */
	private final static long NUM_RECORDS_FOR_OPTIMIZER = -1;

	// This increases the number of clauses for wildcard and other searches (default is 1024):
	private final static int MAX_CLAUSE_COUNT = 30000;

	/**
	 *  Indicates update operations will be blocked until the current one returns. When this is passed into a
	 *  method, the method will not return until the update operation has completed.
	 */
	public final static boolean BLOCK = true;

	/**
	 *  Indicates update operations will be allowed while others are still in progress. When this is passed into
	 *  a method, the method will return immediately rather than waiting for the update operation to complete.
	 */
	public final static boolean NO_BLOCK = false;

	/**
	 *  Use to set the boolean search operator to OR.
	 *
	 * @see    #setOperator(int operator)
	 * @see    #getOperator()
	 */
	public final static int DEFAULT_OR = 0;
	/**
	 *  Use to set the boolean search operator to AND.
	 *
	 * @see    #setOperator(int operator)
	 * @see    #getOperator()
	 */
	public final static int DEFAULT_AND = 1;

	private int defaultBooleanOperator = DEFAULT_OR;

	private final static String DEFAULT_DOC_FIELD = "default";

	private long cachedLastModifiedCount = 0;
	private boolean abortUpdate = false;

	private File indexDir;
	private File luceneIndexDir;

	private Analyzer defaultAnalyzer = null;

	private IndexReader _myReader = null;
	private IndexSearcher _mySearcher = null;
	private Object _readerSearcherUpdateLock = new Object();
	private Object _updateLock = new Object();
	private Object _stopperLock = new Object();

	private boolean isIndexing = false;

	private HashMap attributes = null;

	private static boolean debug = true;

	private String defaultSearchField;

	// The max number of search re-tries if there was an exception (which can occur during index updating)
	private final static int MAX_TRIES = 3;

	private OptimizeMonitorThread optimizeMonitorThread = null;


	/**
	 *  Initializes or creates an index at the given location using a default search field named "default" and a
	 *  StandardAnalyzer for index searching and creation.
	 *
	 * @param  indexDirPath  The directory where the index is located or will be created.
	 */
	public SimpleLuceneIndex(String indexDirPath) {
		this(indexDirPath, null, null);
	}


	/**
	 *  Initializes or creates an index at the given location using a default search field named "default" and
	 *  the given Analyzer.
	 *
	 * @param  indexDirPath  The directory where the index is located or will be created.
	 * @param  analyzer      The default Analyzer to use for searching and index creation
	 */
	public SimpleLuceneIndex(String indexDirPath, Analyzer analyzer) {
		this(indexDirPath, null, analyzer);
	}


	/**
	 *  Initializes or creates an index at the given location using the default search field, additional stop
	 *  words and analyzer indicated.
	 *
	 * @param  indexDirPath  The directory where the index is located or will be created.
	 * @param  defaultField  The name of the field used for default searching, for example "default".
	 * @param  analyzer      The default Analyzer to use for searching and index creation
	 */
	public SimpleLuceneIndex(String indexDirPath,
	                         String defaultField,
	                         Analyzer analyzer) {
		try {
			if (analyzer == null)
				defaultAnalyzer = new StandardAnalyzer(luceneVersion);
			else
				defaultAnalyzer = analyzer;

			if (defaultField == null)
				defaultSearchField = "default";

			// Set up variables.
			attributes = new HashMap();
			attributes.put("thisIndex", this);

			// This increases the number of clauses for wildcard searches (default is 1024):
			BooleanQuery.setMaxClauseCount(MAX_CLAUSE_COUNT);

			indexDir = new File(indexDirPath);
			luceneIndexDir = new File(indexDir, "lucene_index");

			optimizeMonitorThread = new OptimizeMonitorThread(this);
			optimizeMonitorThread.start();

			init();
		} catch (Throwable t) {
			prtlnErr("Unable to create new SimpleLuceneIndex: " + t);
			if (t instanceof java.lang.NullPointerException)
				t.printStackTrace();
		}
	}


	/**  Initializes the existing index or creates a new one if none exists. */
	private void init() {
		// If an old SimpleLuceneIndex (before 2005-04-28) is here, convert it to the new style...
		if (!luceneIndexDir.exists()) {
			File[] files = indexDir.listFiles();
			if (files != null) {
				try {
					for (int i = 0; i < files.length; i++) {
						if (files[i].getName().startsWith("read_index"))
							files[i].renameTo(luceneIndexDir);
						else if (files[i].getName().equals("write_index"))
							Files.deleteDirectory(files[i]);
						else if (files[i].getName().equals("indexReader"))
							files[i].delete();
					}
				} catch (Exception e) {
					prtlnErr("Unable to clean up the old-tyle SimpleLuceneIndex: " + e);
				}
			}
		}

		// Check for the existance of an index...
		IndexReader tmpReader = null;
		try {
			tmpReader = InstantiatedIndexReader.open(FSDirectory.open(luceneIndexDir));
			tmpReader.numDocs();
		} catch (IOException e) {
			// If no index exists or it is corrupt, create a new one

			prtln("No index at location '" + luceneIndexDir + "' - creating a new one...");
			luceneIndexDir.mkdirs();

			try {
				synchronized (_updateLock) {
					// Open and close the writer, which creates a new index
					IndexWriter tmpWriter = new IndexWriter(FSDirectory.open(luceneIndexDir), this.getAnalyzer(), true, IndexWriter.MaxFieldLength.UNLIMITED);
					tmpWriter.optimize();
					tmpWriter.close();
					tmpWriter = null;
				}
			} catch (IOException e2) {
				prtlnErr("Unable to create index at location '" + luceneIndexDir + "': " + e2);
			}
		} finally {
			try {
				if (tmpReader != null)
					tmpReader.close();
			} catch (Throwable t) {
				prtlnErr("Error closing writer: " + t);
			}
		}

		// Create and load a new searcher and reader
		loadNewReaderAndSearcher();
		prtln("Index initialized successfully. The index contains " + getNumDocs() + " entries.");
	}


	/**
	 *  Creates and loads a new IndexReader and IndexSearcher. Should be called only within a synchronized
	 *  (_updateLock) block after any IndexWriter.close() operations have been called.
	 */
	private void loadNewReaderAndSearcher() {
		try {
			synchronized (_updateLock) {
				synchronized (_readerSearcherUpdateLock) {
					if (_myReader != null)
						_myReader.close();
					if (_mySearcher != null)
						_mySearcher.close();

					/* Note: FSDirectory.open instantiates SimpleFSDirectory on Windows and NIOFSDirectory on Linux. 
					Some NIO ClosedChannelExceptions were seen on Linux when using FSDirectory.open so trying SimpleFSDirectory instead... */
					//_myReader = IndexReader.open(FSDirectory.open(luceneIndexDir));
					_myReader = IndexReader.open(new SimpleFSDirectory(luceneIndexDir)); // The basic directory reader, blocks on IO operations...
					_mySearcher = new IndexSearcher(_myReader);
				}
			}
		} catch (Throwable t) {
			prtlnErr("Unable to load a new reader or searcher: " + t);
		}
	}


	/**  Deletes the index and re-initializes a new, empty one in its place. */
	public void deleteAndReinititlize() {
		stopIndexing();
		synchronized (_updateLock) {
			try {
				cachedLastModifiedCount = getLastModifiedCount() + 1;
				synchronized (_readerSearcherUpdateLock) {
					if (_myReader != null)
						_myReader.close();
					if (_mySearcher != null)
						_mySearcher.close();
				}

				Files.deleteDirectory(indexDir);
			} catch (Throwable e) {
				prtlnErr("deleteAndReinititlize(): " + e);
			}
			init();
		}
	}


	/**
	 *  Sets an attribute that will be available for access in search results by calling {@link
	 *  org.dlese.dpc.index.reader.DocReader#getAttribute(String)} or {@link ResultDoc#getAttribute(String)}.
	 *
	 * @param  key        The key used to reference the attribute.
	 * @param  attribute  Any Java Object.
	 * @see               ResultDoc#getAttribute(String)
	 * @see               org.dlese.dpc.index.reader.DocReader#getAttribute(String)
	 */
	public void setAttribute(String key, Object attribute) {
		attributes.put(key, attribute);
	}


	/**
	 *  Gets an attribute from this SimpleLuceneIndex. Note that these attributes are available for access in
	 *  search results by calling {@link org.dlese.dpc.index.reader.DocReader#getAttribute(String)} or {@link
	 *  ResultDoc#getAttribute(String)}. The key 'thisIndex' returns this index.
	 *
	 * @param  key  The key used to reference the attribute.
	 * @return      The Java Object that is stored under the given key or null if none exists.
	 * @see         ResultDoc#getAttribute(String)
	 * @see         org.dlese.dpc.index.reader.DocReader#getAttribute(String)
	 */
	public Object getAttribute(String key) {
		return attributes.get(key);
	}


	/**
	 *  Gets the ablsolute path to the directory where the index resides.
	 *
	 * @return    The absolue path to the index.
	 */
	public String getIndexLocation() {
		return (indexDir == null ? null : indexDir.getAbsolutePath());
	}


	/**
	 *  Performs a search over the index using the qiven query String, returning an ordered array of matching
	 *  ranked results.
	 *
	 * @param  query  The query to perform over the index.
	 * @return        An ordered array of ranked results matching the given query.
	 * @see           #setOperator(int operator)
	 * @see           #getOperator()
	 */
	public ResultDocList searchDocs(String query) {
		try {
			return doSearchDocs(query, null, null, null, null, null, 0);
		} catch (Throwable e) {
			// NullPointer is thrown if nothing is in the index
		}
		return null;
	}


	/**
	 *  Performs a search over the index using the qiven query String, returning an ordered array of matching
	 *  ranked results.
	 *
	 * @param  query   The query to perform over the index.
	 * @param  sortBy  A Sort to apply to the results or null to use relevancy ranking
	 * @return         An ordered array of ranked results matching the given query.
	 * @see            #setOperator(int operator)
	 * @see            #getOperator()
	 */
	public ResultDocList searchDocs(String query, Sort sortBy) {
		try {
			return doSearchDocs(query, null, null, sortBy, null, null, 0);
		} catch (Throwable e) {
			// NullPointer is thrown if nothing is in the index
		}
		return null;
	}


	/**
	 *  Performs a search over the index using the qiven query String and Analyzer, returning an ordered array of
	 *  matching ranked results.
	 *
	 * @param  query     The query to perform over the index.
	 * @param  analyzer  The Analyzer to use to determine the tokens in the query.
	 * @return           An ordered array of ranked results matching the given query.
	 * @see              #setOperator(int operator)
	 * @see              #getOperator()
	 */
	public ResultDocList searchDocs(String query, Analyzer analyzer) {
		try {
			return doSearchDocs(query, null, null, null, analyzer, null, 0);
		} catch (Throwable e) {
			// NullPointer is thrown if nothing is in the index
		}
		return null;
	}


	/**
	 *  Performs a search over the index using the qiven query String, returning an ordered array of matching
	 *  ranked results.
	 *
	 * @param  query                The query to perform over the index.
	 * @param  docReaderAttributes  Attributes that are included for use in DocReaders via the ResultDocConfig.
	 * @return                      An ordered array of ranked results matching the given query.
	 * @see                         #setOperator(int operator)
	 * @see                         #getOperator()
	 */
	public ResultDocList searchDocs(String query, HashMap docReaderAttributes) {
		try {
			return doSearchDocs(query, null, null, null, null, docReaderAttributes, 0);
		} catch (Throwable e) {
			// NullPointer is thrown if nothing is in the index
		}
		return null;
	}


	/**
	 *  Performs a search over the index using the Query object, returning an ordered array of matching ranked
	 *  results.
	 *
	 * @param  query                The Query to search over the index.
	 * @param  docReaderAttributes  Attributes that are included for use in DocReaders via the ResultDocConfig.
	 * @return                      An ordered array of ranked results matching the given query.
	 * @see                         #setOperator(int operator)
	 * @see                         #getOperator()
	 */
	public ResultDocList searchDocs(Query query, HashMap docReaderAttributes) {
		try {
			return doSearchDocs(query, null, null, null, null, docReaderAttributes, 0);
		} catch (Throwable e) {
			// NullPointer is thrown if nothing is in the index
		}
		return null;
	}


	/**
	 *  Performs a search over the index using the qiven query String, returning an ordered array of matching
	 *  ranked results.
	 *
	 * @param  query                The query to perform over the index.
	 * @param  docReaderAttributes  Attributes that are included for use in DocReaders via the ResultDocConfig.
	 * @param  analyzer             The analyzer to use, or null to use the default
	 * @return                      An ordered array of ranked results matching the given query.
	 * @see                         #setOperator(int operator)
	 * @see                         #getOperator()
	 */
	public ResultDocList searchDocs(String query, HashMap docReaderAttributes, Analyzer analyzer) {
		try {
			return doSearchDocs(query, null, null, null, analyzer, docReaderAttributes, 0);
		} catch (Throwable e) {
			// NullPointer is thrown if nothing is in the index
		}
		return null;
	}


	/**
	 *  Performs a search over the index using the qiven query String, returning an ordered array of matching
	 *  ranked results.
	 *
	 * @param  query         The query to perform over the index.
	 * @param  defaultField  The default field to search in.
	 * @return               An ordered array of ranked results matching the given query.
	 * @see                  #setOperator(int operator)
	 * @see                  #getOperator()
	 */
	public ResultDocList searchDocs(String query, String defaultField) {
		try {
			return doSearchDocs(query, defaultField, null, null, null, null, 0);
		} catch (Throwable e) {
			// NullPointer is thrown if nothing is in the index
		}
		return null;
	}


	/**
	 *  Performs a search over the index using the qiven query String, default field and Filter, returning an
	 *  ordered array of matching ranked results.
	 *
	 * @param  query         The query to perform over the index.
	 * @param  defaultField  The default field to search in, or null to use the pre-defined default field.
	 * @param  filter        A filter used for the search.
	 * @param  sortBy        A Sort to apply to the results or null to use relevancy ranking
	 * @return               An ordered array of ranked results matching the given query.
	 * @see                  #setOperator(int operator)
	 * @see                  #getOperator()
	 */
	public ResultDocList searchDocs(String query, String defaultField, Filter filter, Sort sortBy) {
		try {
			return doSearchDocs(query, defaultField, filter, sortBy, null, null, 0);
		} catch (Throwable e) {
			// NullPointer is thrown if nothing is in the index
		}
		return null;
	}


	/**
	 *  Performs a search over the index using the qiven query String and Filter using the pre-defined default
	 *  field, returning an ordered array of matching ranked results.
	 *
	 * @param  query   The query to perform over the index.
	 * @param  filter  A filter used for the search.
	 * @return         An ordered array of ranked results matching the given query.
	 * @see            #setOperator(int operator)
	 * @see            #getOperator()
	 */
	public ResultDocList searchDocs(String query, Filter filter) {
		try {
			return doSearchDocs(query, null, filter, null, null, null, 0);
		} catch (Throwable e) {
			// NullPointer is thrown if nothing is in the index
		}
		return null;
	}


	/**
	 *  Performs a search over the index using the qiven Query and Filter using the pre-defined default field,
	 *  returning an ordered array of matching ranked results.
	 *
	 * @param  query   The Query to perform over the index.
	 * @param  filter  A filter used for the search.
	 * @return         An ordered array of ranked results matching the given query.
	 * @see            #setOperator(int operator)
	 * @see            #getOperator()
	 */
	public ResultDocList searchDocs(Query query, Filter filter) {
		try {
			return doSearchDocs(query, null, filter, null, null, null, 0);
		} catch (Throwable e) {
			// NullPointer is thrown if nothing is in the index
		}
		return null;
	}


	/**
	 *  Performs a search over the index using the qiven Query using the pre-defined default field, returning an
	 *  ordered array of matching ranked results.
	 *
	 * @param  query  The Query to perform over the index.
	 * @return        An ordered array of ranked results matching the given query.
	 * @see           #setOperator(int operator)
	 * @see           #getOperator()
	 */
	public ResultDocList searchDocs(Query query) {
		try {
			return doSearchDocs(query, null, null, null, null, null, 0);
		} catch (Throwable e) {
			// NullPointer is thrown if nothing is in the index
		}
		return null;
	}


	/**
	 *  Performs a search over the index using the qiven query String and Filter using the pre-defined default
	 *  field, returning an ordered array of matching ranked results.
	 *
	 * @param  query                The query to perform over the index.
	 * @param  filter               A filter used for the search.
	 * @param  docReaderAttributes  Attributes that are included for use in DocReaders via the ResultDocConfig.
	 * @return                      An ordered array of ranked results matching the given query.
	 * @see                         #setOperator(int operator)
	 * @see                         #getOperator()
	 */
	public ResultDocList searchDocs(String query, Filter filter, HashMap docReaderAttributes) {
		try {
			return doSearchDocs(query, null, filter, null, null, docReaderAttributes, 0);
		} catch (Throwable e) {
			// NullPointer is thrown if nothing is in the index
		}
		return null;
	}


	/**
	 *  Performs a search over the index using the qiven Query Object and Filter using the pre-defined default
	 *  field, returning an ordered array of matching ranked results.
	 *
	 * @param  query                The Query to perform over the index.
	 * @param  filter               A filter used for the search.
	 * @param  docReaderAttributes  Attributes that are included for use in DocReaders via the ResultDocConfig.
	 * @param  sortBy               A Sort to apply to the results or null to use relevancy ranking
	 * @return                      An ordered array of ranked results matching the given query.
	 * @see                         #setOperator(int operator)
	 * @see                         #getOperator()
	 */
	public ResultDocList searchDocs(Query query, Filter filter, Sort sortBy, HashMap docReaderAttributes) {
		try {
			return doSearchDocs(query, null, filter, sortBy, null, docReaderAttributes, 0);
		} catch (Throwable e) {
			// NullPointer is thrown if nothing is in the index
		}
		return null;
	}


	/**
	 *  Performs a search over the index using the qiven query String and Filter using the pre-defined default
	 *  field, returning an ordered array of matching ranked results.
	 *
	 * @param  query                The query to perform over the index.
	 * @param  filter               A Filter used for the search or null for none.
	 * @param  sortBy               A Sort to apply to the results or null to use relevancy ranking
	 * @param  docReaderAttributes  Attributes that are included for use in DocReaders via the ResultDocConfig or
	 *      null for none
	 * @param  analyzer             The analyzer to use, or null to use the default
	 * @return                      An ordered array of ranked results matching the given query.
	 * @see                         #setOperator(int operator)
	 * @see                         #getOperator()
	 */
	public ResultDocList searchDocs(String query, Filter filter, Sort sortBy, HashMap docReaderAttributes, Analyzer analyzer) {
		try {
			return doSearchDocs(query, null, filter, sortBy, analyzer, docReaderAttributes, 0);
		} catch (Throwable e) {
			// NullPointer is thrown if nothing is in the index
		}
		return null;
	}


	/**
	 *  Performs the actual search. See {@link #searchDocs(String, Filter, Collector)} for description. Note that
	 *  this method uses a single copy of QueryParser, so it's not thread safe. Care should be taken to ensure
	 *  that only one thread uses the search at a time.
	 *
	 * @param  query                The query String or Query to perform over the index.
	 * @param  defaultField         The default field to search in, or null to use the pre-configured default
	 *      field.
	 * @param  filter               A filter used for the search, or null to use none.
	 * @param  analyzer             The Analyzer to use, or null to use the default Analyzer then
	 *      KeywordFieldAnalyzer combo.
	 * @param  docReaderAttributes  Attributes that will be available for access in ResulDocs.
	 * @param  numTries             The current number of times this method has been called for a given search
	 * @param  sort                 A Sort to apply to the results or null to use relevancy ranking
	 * @return                      An ordered List of ranked results matching the given query, or empty List
	 *      (never null)
	 * @see                         #setOperator(int operator)
	 * @see                         #getOperator()
	 */
	private final ResultDocList doSearchDocs(
	                                         Object query,
	                                         String defaultField,
	                                         Filter filter,
	                                         Sort sort,
	                                         Analyzer analyzer,
	                                         HashMap docReaderAttributes,
	                                         int numTries) {

		if (query == null)
			return new ResultDocList();

		try {

			// If an Analyzer is supplied, use only it. If no
			// Analyzer is supplied and this is not a Query, first use
			// the default Analyzer, then
			// a KeywordAnalyzer if no matches were found.
			boolean doKeywordSearch = true;

			Query luceneQueryObj = null;
			String queryString = null;

			// If query is a Query...
			if (query instanceof Query) {

				//collector = new ResultDocCollector(conf);

				luceneQueryObj = (Query) query;
				queryString = query.toString();

				doKeywordSearch = false;
			}

			// If query is a String...
			else {
				queryString = (String) query;

				if (defaultField == null && analyzer == null) {
					//prtln("(defaultField == null && analyzer == null)");
					// String operator = "AND";
					// if(queryParser.getOperator() == QueryParser.DEFAULT_OPERATOR_OR)
					// 	operator = "OR";
					// System.out.print("\nSimpleLuceneIndex 1 query is: " + query + " default operator " + operator);
					// //q = queryParser.parse((String) query);
					luceneQueryObj = getQueryParser().parse((String) query);
				}
				else {
					if (defaultField == null)
						defaultField = defaultSearchField;
					if (analyzer == null)
						analyzer = getAnalyzer();
					else
						doKeywordSearch = false;
					
					QueryParser qp = new QueryParser(luceneVersion, defaultField, analyzer);
					qp.setDefaultOperator(getLuceneOperator());

					//System.out.println("\nSimpleLuceneIndex 2 query is: " + query);
					luceneQueryObj = qp.parse((String) query);
				}
			}

			TopDocs topDocs = null;

			// Peform the search over the current read index
			try {
				synchronized (_readerSearcherUpdateLock) {
					if (_mySearcher != null) {
						//prtln("Search qeury: " + luceneQueryObj + " sort: " + sort);
						
						if (sort == null)
							topDocs = _mySearcher.search(luceneQueryObj, filter, MAX_NUM_HITS);
						else {
							try {
								//prtln("Search with sort on '" + sort + "'");
								topDocs = _mySearcher.search(luceneQueryObj, filter, MAX_NUM_HITS, sort);
							} catch (ArrayIndexOutOfBoundsException ae) {
								// If the sort field is tokenized it can throw this exception, so perform the search without sorting:
								topDocs = _mySearcher.search(luceneQueryObj, filter, MAX_NUM_HITS);
								prtlnErr("Requested sort field '" + sort + "' appears to be invalid. Sort fields must contain a single token only (e.g. should not be analyzed). Sort criteria was ignored...");
							}
						}
					}
				}
			} catch (Throwable e) {
				if (!(e instanceof ParseException) && numTries < MAX_TRIES)
					return doSearchDocs(query, defaultField, filter, sort, analyzer, docReaderAttributes, ++numTries);
				prtlnErr("doSearchDocs() caught exception after " + numTries + " tries for query ' " + luceneQueryObj + "' msg: " + e);
				e.printStackTrace();
			}

			// If there were no matches, try searching using a KeywordFieldAnalyzer
			if ((topDocs == null || topDocs.totalHits == 0) && doKeywordSearch) {
				if (defaultField == null)
					defaultField = defaultSearchField;

				QueryParser qp = new QueryParser(luceneVersion, defaultField, new KeywordAnalyzer());
				qp.setDefaultOperator(getLuceneOperator());

				//System.out.print("\nSimpleLuceneIndex 3 query is: " + query);
				qp.parse((String) query);

				// Peform the search over the current read index
				try {
					synchronized (_readerSearcherUpdateLock) {
						if (_mySearcher != null)
							topDocs = _mySearcher.search(luceneQueryObj, filter, MAX_NUM_HITS);
					}
				} catch (Throwable e) {
					if (!(e instanceof ParseException) && numTries < MAX_TRIES)
						return doSearchDocs(query, defaultField, filter, sort, analyzer, docReaderAttributes, ++numTries);
					//prtlnErr("doSearchDocs() 2 caught exception after " + numTries + " tries: " + e);
					//e.printStackTrace();
				}
			}

			ResultDocConfig resultDocConfig = new ResultDocConfig(queryString, luceneQueryObj, filter, docReaderAttributes, this);

			//System.out.println(". Num results: " + collector.results().length + "\n");
			if (topDocs == null) {
				//prtln("doSearchDocs() topDocs null - returning empty ResultDocList...");
				return new ResultDocList();
			}
			else {
				ResultDocList resultDocList = new ResultDocList(topDocs, resultDocConfig);
				//prtln("doSearchDocs() returning empty ResultDocList.size(): " + resultDocList.size());
				return resultDocList;
			}
		} catch (Throwable e) {
			//prtlnErr("doSearchDocs() numTries: " + numTries + " exception: " + e);

			// If parse error, etc.
			if (!(e instanceof ParseException) && numTries < MAX_TRIES)
				return doSearchDocs(query, defaultField, filter, sort, analyzer, docReaderAttributes, ++numTries);
			prtlnErr("doSearchDocs() 3 caught exception after " + numTries + " tries: " + e);
			e.printStackTrace();
		}

		return new ResultDocList();
	}


	/**
	 *  Searches the index and returns a ResultDocCollection that is backed by Lucene {@link
	 *  org.apache.lucene.search.Hits}.
	 *
	 * @param  operator  The new operator value
	 * @see              #setOperator(int operator)
	 * @see              #getOperator()
	 */
	/* 	public final ResultDocCollection search(
	                                        Object query,
	                                        String defaultField,
	                                        Filter filter,
	                                        Analyzer analyzer,
	                                        HashMap docReaderAttributes,
	                                        int numTries) {
		if (query == null)
			return null;
		ResultDocConfig conf = null;
		Hits hits = null;
		try {
			// If an Analyzer is supplied, use only it. If no
			// Analyzer is supplied and this is not a Query, first use
			// the default Analyzer, then
			// a KeywordAnalyzer if no matches were found.
			boolean doKeywordSearch = true;
			Query q;
			if (query instanceof Query) {
				conf = new ResultDocConfig(query.toString(), filter, docReaderAttributes, this);
				q = (Query) query;
				doKeywordSearch = false;
			}
			else {
				conf = new ResultDocConfig((String) query, filter, docReaderAttributes, this);
				if (defaultField == null && analyzer == null) {
					// String operator = "AND";
					// if(queryParser.getOperator() == QueryParser.DEFAULT_OPERATOR_OR)
					// 	operator = "OR";
					// System.out.print("\nSimpleLuceneIndex 1 query is: " + query + " default operator " + operator);
					// q = getQueryParser().parse((String) query);
				}
				else {
					if (defaultField == null)
						defaultField = defaultSearchField;
					if (analyzer == null)
						analyzer = getAnalyzer();
					else
						doKeywordSearch = false;
					QueryParser qp = new QueryParser(defaultField, analyzer);
					qp.setDefaultOperator(getLuceneOperator());
					//System.out.print("\nSimpleLuceneIndex 2 query is: " + query);
					q = qp.parse((String) query);
				}
			}
			//prtln("Parsed Query: '" + q.toString() + "'");
			// Peform the search over the current read index
			try {
				synchronized (_readerSearcherUpdateLock) {
					if (_mySearcher != null)
						hits = _mySearcher.search(q, filter);
				}
			} catch (Throwable e) {
				if (!(e instanceof ParseException) && numTries < MAX_TRIES)
					return search(query, defaultField, filter, analyzer, docReaderAttributes, ++numTries);
				//prtlnErr("search() 1 caught exception after " + numTries + " tries: " + e);
				//e.printStackTrace();
			}
			// If there were no matches, try searching using a KeywordFieldAnalyzer
			if ((hits == null || hits.length() == 0) && doKeywordSearch) {
				if (defaultField == null)
					defaultField = defaultSearchField;
				QueryParser qp = new QueryParser(defaultField, new KeywordAnalyzer());
				qp.setDefaultOperator(getLuceneOperator());
				//System.out.print("\nSimpleLuceneIndex 3 query is: " + query);
				q = qp.parse((String) query);
				// Peform the search over the current read index
				try {
					synchronized (_readerSearcherUpdateLock) {
						if (_mySearcher != null)
							hits = _mySearcher.search(q, filter);
					}
				} catch (Throwable e) {
					if (!(e instanceof ParseException) && numTries < MAX_TRIES)
						return search(query, defaultField, filter, analyzer, docReaderAttributes, ++numTries);
					//prtlnErr("search() 1 caught exception after " + numTries + " tries: " + e);
					//e.printStackTrace();
				}
			}
			//System.out.println(". Num results: " + collector.results().length + "\n");
			return new ResultDocCollection(hits, conf);
		} catch (Throwable e) {
			if (!(e instanceof ParseException) && numTries < MAX_TRIES)
				return search(query, defaultField, filter, analyzer, docReaderAttributes, ++numTries);
			//prtlnErr("search() 1 caught exception after " + numTries + " tries: " + e);
			//e.printStackTrace();
		}
		return null;
	} */
	/**
	 *  Sets the boolean operator used during searches. Once set, the given boolean operator will be used for all
	 *  subsequent searches. If this method is never called the boolean operator defaults to OR.
	 *
	 * @param  operator  The new boolean operator value.
	 * @see              #DEFAULT_OR
	 * @see              #DEFAULT_AND
	 */
	public void setOperator(int operator) {
		defaultBooleanOperator = operator;
	}


	/**
	 *  Gets the boolean operator that is currently being used for searches.
	 *
	 * @return    The boolean operator value.
	 * @see       #DEFAULT_OR
	 * @see       #DEFAULT_AND
	 */
	public int getOperator() {
		return defaultBooleanOperator;
	}


	/**
	 *  Gets the Lucene boolean operator that is currently being used for searches.
	 *
	 * @return    The boolean operator value.
	 */
	public QueryParser.Operator getLuceneOperator() {
		if (defaultBooleanOperator == DEFAULT_OR)
			return QueryParser.Operator.OR;
		else
			return QueryParser.Operator.AND;
	}


	/**
	 *  Gets a new instance of the {@link org.apache.lucene.queryParser.QueryParser} used by this
	 *  SimpleLuceneIndex that uses it's Analyzers, defaultField and boolean operator settings.
	 *
	 * @return    The QueryParser used by this SimpleLuceneIndex
	 */
	public final QueryParser getQueryParser() {
		QueryParser parser = new QueryParser(getLuceneVersion(), defaultSearchField, defaultAnalyzer);
		parser.setDefaultOperator(getLuceneOperator());
		
		//System.out.println("getQueryParser() using analyzer class: " + defaultAnalyzer.getClass().getName());
		return parser;
	}


	/**
	 *  Gets a new instance of the {@link org.apache.lucene.queryParser.QueryParser} used by this
	 *  SimpleLuceneIndex that uses it's Analyzers and boolean operator settings, allowing one to specify the
	 *  default search field.
	 *
	 * @param  defaultSearchField  The search field used as default when none is specified in the query
	 * @return                     The QueryParser used by this SimpleLuceneIndex with the given default search
	 *      field
	 */
	public final QueryParser getQueryParser(String defaultSearchField) {
		QueryParser parser = new QueryParser(getLuceneVersion(), defaultSearchField, defaultAnalyzer);
		parser.setDefaultOperator(getLuceneOperator());
		return parser;
	}


	/**
	 *  Gets the boolean operator that is currently being used for searches as a String (AND or OR).
	 *
	 * @return    The boolean operator value as a String (AND or OR).
	 * @see       #DEFAULT_OR
	 * @see       #DEFAULT_AND
	 */
	public String getOperatorString() {
		if (getOperator() == DEFAULT_AND)
			return "AND";
		else
			return "OR";
	}


	/**
	 *  Gets the name of the field that is searched by default if no field is indicated.
	 *
	 * @return    The defaultSearchFirld value
	 */
	public String getDefaultSearchField() {
		return defaultSearchField;
	}


	/**
	 *  Gets the IndexReader.
	 *
	 * @return    The reader value
	 */
	public IndexReader getReader() {
		synchronized (_readerSearcherUpdateLock) {
			return _myReader;
		}
	}


	/**
	 *  Gets the number of documents that match the given query.
	 *
	 * @param  query  The query to perform over the index.
	 * @return        The number of matching documents.
	 */
	public int getNumDocs(String query) {
		List docs = searchDocs(query);
		if (docs == null)
			return 0;
		else
			return docs.size();
	}


	/**
	 *  Gets the number of documents that match the given query.
	 *
	 * @param  query  The query to perform over the index.
	 * @return        The number of matching documents.
	 */
	public int getNumDocs(Query query) {
		List docs = searchDocs(query);
		if (docs == null)
			return 0;
		else
			return docs.size();
	}


	/**
	 *  Gets the total number of documents in the index.
	 *
	 * @return    The number of documents in the index.
	 */
	public int getNumDocs() {
		//prtln("numDocs()1");
		int num = 0;
		synchronized (_readerSearcherUpdateLock) {
			if (_myReader != null) {
				try {
					num = _myReader.numDocs();
				} catch (Throwable e) {}
			}
		}
		//prtln("numDocs()2");
		return num;
	}


	/**
	 *  Gets a list of all {@link org.apache.lucene.document.Document}s in the index. Note: This method loads all
	 *  Documents and requires a large amount of memory for large result sets (consider using search instead).
	 *
	 * @return    A list of all documents in the index.
	 */
	public List listDocs() {
		//prtln("listDocs() called");
		ArrayList list = new ArrayList();
		synchronized (_readerSearcherUpdateLock) {
			if (_myReader != null) {
				try {
					int numdocs = _myReader.numDocs();
					for (int i = 0; i < numdocs; i++) {
						try {
							list.add(_myReader.document(i));
						} catch (Exception e) {}
					}
				} catch (Throwable e) {}
			}
			else {
				prtlnErr("listDocs couldn't get reader...");
			}
		}

		return list;
	}


	/**
	 *  Gets a list of all {@link org.apache.lucene.document.Document}s in the index that match the given term in
	 *  the given field. Note: This method loads all Documents and requires a large amount of memory for large
	 *  result sets (consider using search instead).
	 *
	 * @param  field  The field searched.
	 * @param  term   The term to match.
	 * @return        A list of matching documents.
	 */
	public List listDocs(String field, String term) {
		//prtln("listDocs(String field, String term) called");

		return doListDocs(field, term);
	}


	/**
	 *  Gets a list of all {@link org.apache.lucene.document.Document}s in the index that match the given terms
	 *  in the given field. Note: This method loads all Documents and requires a large amount of memory for large
	 *  result sets (consider using search instead).
	 *
	 * @param  field  The field searched.
	 * @param  terms  The terms to match.
	 * @return        A list of matching documents.
	 */
	public List listDocs(String field, String[] terms) {
		//prtln("listDocs(String field, String term) called");
		ArrayList list = null;

		if (terms != null && terms.length > 0 && field != null)
			list = new ArrayList(terms.length);
		else
			return null;

		for (int i = 0; i < terms.length; i++)
			list.addAll(doListDocs(field, terms[i]));

		return list;
	}

	/**
	 * Calls the callback function of cal for each document matching the terms in the given field
	 * 
	 * @param cal
	 * @param field
	 * @param terms
	 */
	public void doWithDocument(Callback cal, String field, String[] terms ) {

		if (terms == null || terms.length == 0 || field == null)
			return;

		for (int i = 0; i < terms.length; i++)
			doWithDocument(cal,field, terms[i]);
	}

	/**
	 * Calls the callback function of cal for each document matching the term in the given field
	 * @param cal
	 * @param field
	 * @param term
	 */
	public void doWithDocument(Callback cal, String field, String term ) {

		Document doc;
		synchronized (_readerSearcherUpdateLock) {
			if (_myReader != null) {
				try {
					TermDocs iterator = _myReader.termDocs(new Term(field, term));
					while (iterator.next() && !abortUpdate ) {
						doc = _myReader.document(iterator.doc());
						cal.doWithDocument(doc);
					}
					iterator.close();
					iterator = null;
				} catch (Throwable e) {}
			}
		}

	}


	/**
	 *  Helper function to create a list of Documents that match the given term in the given field.
	 *
	 * @param  field  The field searched.
	 * @param  term   The term to match.
	 * @return        A list of matching documents.
	 */
	private final ArrayList doListDocs(String field, String term) {
		//prtln("doListDocs() 1");

		ArrayList list = new ArrayList();
		Document doc;
		synchronized (_readerSearcherUpdateLock) {
			if (_myReader != null) {
				try {
					TermDocs iterator = _myReader.termDocs(new Term(field, term));
					while (iterator.next()) {
						doc = _myReader.document(iterator.doc());
						list.add(doc);
					}
					iterator.close();
					iterator = null;
				} catch (Throwable e) {}
			}
		}
		//prtln("doListDocs() 2");

		//prtln("doListDocs() field:" + field + " term:" + term + " num results: " + list.size());
		return list;
	}


	/**
	 *  Gets a list of all terms in the index.
	 *
	 * @return    A list of all terms in the index.
	 */
	public List listTerms() {
		//prtln("listTerms()");

		ArrayList list = null;
		synchronized (_readerSearcherUpdateLock) {
			if (_myReader != null) {
				try {
					list = new ArrayList();
					TermEnum iterator = _myReader.terms();
					while (iterator.next()) {
						list.add(iterator.term());
					}
				} catch (Exception e) {}
			}
		}
		return list;
	}


	/**
	 *  Gets a list of all fields in the index listed alphabetically. Depending on the state of the index, the
	 *  list may contain fileds that are empty, meaning all terms for the given field have been deleted and there
	 *  are no possible matching queries within the field.
	 *
	 * @return    A list of all fields in the index.
	 */
	public List getFields() {
		//prtln("getFields()");

		ArrayList fields = null;
		synchronized (_readerSearcherUpdateLock) {
			if (_myReader != null) {
				try {
					fields = new ArrayList();
					Map map = new HashMap();
					TermEnum iterator = _myReader.terms();
					while (iterator.next()) {
						Term t = iterator.term();
						Object obj = map.get(t.field());
						if (obj == null) {
							obj = new Object();

							if (t.text() != null && t.text().trim().length() > 0) {
								map.put(t.field(), obj);
								fields.add(t.field());
							}
						}
					}
					map.clear();
					map = null;
				} catch (Throwable e) {}
			}
		}

		return fields;
	}


	/**
	 *  Gets a Map of Lists that contain the terms for each field in the index. The keys in the Map are Strings
	 *  that represent all fields in the index. The List that is returned for each key contains all terms that
	 *  are in the index for the given field.
	 *
	 * @return    A Map of term Lists keyed by field Strings.
	 */
	public Map getTermLists() {
		//prtln("getTermLists()");


		HashMap map = null;
		synchronized (_readerSearcherUpdateLock) {
			if (_myReader != null) {
				try {
					map = new HashMap();
					TermEnum iterator = _myReader.terms();
					while (iterator.next()) {
						Term t = iterator.term();
						List list = (List) map.get(t.field());
						if (list == null) {
							list = new ArrayList();
							map.put(t.field(), list);
						}
						list.add(t.text());
					}
				} catch (Throwable e) {}
			}
		}

		return map;
	}


	/**
	 *  Gets a list of all terms that are in the index under the given field name. Implementation note: this
	 *  method is not efficient. If you need to use this method frequently, consider caching the results and
	 *  using {@link #getLastModifiedCount()} to determe when to update the cache.
	 *
	 * @param  field  The indexed field name.
	 * @return        List of terms in the index under the given field.
	 */
	public List getTerms(String field) {
		//prtln("getTerms()");

		ArrayList terms = null;
		TermEnum iterator = null;
		synchronized (_readerSearcherUpdateLock) {
			if (_myReader != null) {
				try {
					iterator = _myReader.terms();
				} catch (Throwable e) {}
			}
			if (iterator == null)
				return null;

			try {
				terms = new ArrayList();
				while (iterator.next()) {
					Term t = iterator.term();
					if (t.field().equals(field))
						terms.add(t.text());
				}
			} catch (Throwable e) {}
		}

		return terms;
	}


	/**
	 *  Gets a Map of all terms that are in the index under the given field. The keys in the map are Strings that
	 *  list the terms. The values in the Map are Integers that hold the total count of the terms in the given
	 *  field across all documents. <p>
	 *
	 *  Implementation note: this method is not efficient. If you need to use this method frequently, consider
	 *  caching the results and using {@link #getLastModifiedCount()} to determe when to update the cache.
	 *
	 * @param  field  The indexed field name.
	 * @return        Map containing terms/counts for all terms in the index under the given field.
	 */
	public Map getTermCounts(String field) {
		return getTermCounts(new String[]{field});
	}


	/**
	 *  Gets a Map of all terms that are in the index. The keys in the map are Strings that list the terms. The
	 *  values in the Map are Integers that hold the total count of the terms across all documents. <p>
	 *
	 *  Implementation note: this method is not efficient. If you need to use this method frequently, consider
	 *  caching the results and using {@link #getLastModifiedCount()} to determe when to update the cache.
	 *
	 * @return    Map containing terms/counts for all terms in the index under the given field.
	 */
	public Map getTermCounts() {
		String[] s = null;
		return getTermCounts(s);
	}


	/**
	 *  Gets a Map of all terms that are in the index under the given fields. The keys in the map are Strings
	 *  that list the terms. The values in the Map are Integers that hold the total count of the terms in the
	 *  given fields across all documents. <p>
	 *
	 *  Implementation note: this method is not efficient. If you need to use this method frequently, consider
	 *  caching the results and using {@link #getLastModifiedCount()} to determe when to update the cache.
	 *
	 * @param  fields  The indexed field names.
	 * @return         Map containing terms/counts for all terms in the index under the given fields.
	 */
	public final Map getTermCounts(String[] fields) {
		//prtln("getTermCounts()");
		HashMap map = null;
		try {
			HashMap fMap = null;
			if (fields != null) {
				fMap = new HashMap(fields.length);
				for (int i = 0; i < fields.length; i++)
					fMap.put(fields[i], new Object());
			}

			map = new HashMap();
			TermEnum iterator = null;
			synchronized (_readerSearcherUpdateLock) {
				if (_myReader != null) {
					try {
						iterator = _myReader.terms();
					} catch (Throwable e) {}
				}
				while (iterator.next()) {
					Term t = iterator.term();
					TermDocs td = null;
					if (fMap == null || fMap.containsKey(t.field())) {
						if (_myReader != null) {
							try {
								td = _myReader.termDocs(t);
							} catch (Throwable e) {}
						}
						int total = 0;
						while (td.next())
							total += td.freq();

						Integer i = (Integer) map.get(t.text());
						if (i == null) {
							map.put(t.text(), new Integer(total));
						}
						else {
							map.put(t.text(), new Integer(total + i.intValue()));
						}
					}
				}
			}
		} catch (Exception e) {}
		return map;
	}


	/**
	 *  Gets a Map of all terms that are in the index under the given fields. The keys in the map are Strings
	 *  that list the terms. The values in the Map are {@link TermDocCount} Objects, which contain the term
	 *  count, the total number of documents containing the term in one or more of the given field(s), and a list
	 *  of fields in which the term appears. <p>
	 *
	 *  Implementation note: this method is not efficient. If you need to use this method frequently, consider
	 *  caching the results and using {@link #getLastModifiedCount()} to determe when to update the cache. Also,
	 *  this method is considerably slower when more than one field is requested. This is because an extry query
	 *  is required for each term that is found.<p>
	 *
	 *
	 *
	 * @param  fields  The indexed field names.
	 * @return         Map containing a {@link TermDocCount} Object for all terms in the index under the given
	 *      fields.
	 * @see            TermDocCount
	 */
	public final Map getTermAndDocCounts(String[] fields) {
		//prtln("getTermAndDocCounts()");

		String[] _fields = null;

		// Ensure there are no empty fields:
		if (fields != null) {
			List f = new ArrayList(fields.length);
			for (int i = 0; i < fields.length; i++) {
				if (fields[i].trim().length() > 0)
					f.add(fields[i].trim());
			}
			_fields = (String[]) f.toArray(new String[]{});
		}

		// If no fields indicated, return empty map:
		if (_fields == null || _fields.length == 0)
			return new HashMap(1);

		Map map = null;
		try {

			// Create a Map of the possible fields
			HashMap fMap = null;
			fMap = new HashMap(_fields.length);
			for (int i = 0; i < _fields.length; i++)
				fMap.put(_fields[i], new Object());

			// Iterate over the term docs and keep counts.
			map = new TreeMap();
			TermEnum iterator = null;
			synchronized (_readerSearcherUpdateLock) {
				if (_myReader != null) {
					try {
						iterator = _myReader.terms();
					} catch (Throwable e) {}
				}
				TermDocs td = null;
				while (iterator.next()) {
					Term t = iterator.term();
					if (fMap == null || fMap.containsKey(t.field())) {
						if (_myReader != null) {
							try {
								td = _myReader.termDocs(t);
							} catch (Throwable e) {}
						}
						int total_terms = 0;
						int total_docs = 0;
						while (td.next()) {
							total_terms += td.freq();
							total_docs++;
						}

						TermDocCount termDocCount = (TermDocCount) map.get(t.text());

						if (termDocCount == null) {
							termDocCount = new TermDocCount();

							// If more than one field, need to do a separate search
							// to determine the number of docs.
							if (_fields.length > 1) {
								String q = _fields[0] + ":\"" + t.text() + "\"";
								for (int i = 1; i < _fields.length; i++)
									q += " OR " + _fields[i] + ":\"" + t.text() + "\"";
								termDocCount.addToDocCount(getNumDocs(q));
							}
							else
								termDocCount.addToDocCount(total_docs);
						}

						termDocCount.addField(t.field());
						termDocCount.addToTermCount(total_terms);

						if (termDocCount.getTermCount() > 0)
							map.put(t.text(), termDocCount);
					}
				}
			}
		} catch (Exception e) {}
		return map;
	}


	/**
	 *  Gets a list of all stop words for this index.
	 *
	 * @param  term  DESCRIPTION
	 * @return       A List of stop word Strings.
	 */
	/* public List listStopWords() {
		String[] stopwords = null;
		try {
			LuceneIndexConfig cfg = getConfig();
			stopwords = (cfg.stopWords == null) ? StopAnalyzer.ENGLISH_STOP_WORDS : cfg.stopWords;
			return Arrays.asList(stopwords);
		} catch (Exception e) {}
		return null;
	} */
	/**
	 *  Gets the termFrequency across all fields in the index
	 *
	 * @param  term  The term.
	 * @return       The termFrequency value.
	 */
	public int getTermFrequency(String term) {
		//prtln("getTermFrequency()");

		int freq = -1;
		List fields = getFields();
		if (fields != null) {
			synchronized (_readerSearcherUpdateLock) {
				if (_myReader != null) {
					try {
						freq = 0;
						for (int i = 0; i < fields.size(); i++) {
							freq += _myReader.docFreq(new Term((String) fields.get(i), term));
						}
					} catch (Exception e) {}
				}
			}
		}
		return freq;
	}


	/**
	 *  Gets the termFrequency of terms in the given field.
	 *
	 * @param  field  The field.
	 * @param  term   The term.
	 * @return        The termFrequency.
	 */
	public int getTermFrequency(String field, String term) {
		//prtln("getTermFrequency()");

		int freq = -1;
		synchronized (_readerSearcherUpdateLock) {
			if (_myReader != null) {
				try {
					freq = _myReader.docFreq(new Term(field, term));
				} catch (Throwable e) {}
			}
		}
		return freq;
	}



	/**
	 *  Adds a Document to the index. Blocks all other update operations until complete.
	 *
	 * @param  doc  The Document to add.
	 * @return      True if successful.
	 */
	public boolean addDoc(Document doc) {
		return update(null, (String[]) null, new Document[]{doc}, BLOCK);
	}


	/**
	 *  Adds a Document to the index.
	 *
	 * @param  doc    The Document to add.
	 * @param  block  Indicates whether to block other updates until complete.
	 * @return        True if successful.
	 */
	public boolean addDoc(Document doc, boolean block) {
		return update(null, (String[]) null, new Document[]{doc}, block);
	}


	/**
	 *  Adds a group of Documents to the index. Blocks all other update operations until complete.
	 *
	 * @param  docs  The Documents to add.
	 * @return       True if successful.
	 */
	public boolean addDocs(Document[] docs) {
		return update(null, (String[]) null, docs, BLOCK);
	}


	/**
	 *  Adds a group of Documents to the index. Blocks all other update operations until complete.
	 *
	 * @param  docs   The Documents to add.
	 * @param  block  Indicates whether to block other updates until complete.
	 * @return        True if successful.
	 */
	public boolean addDocs(Document[] docs, boolean block) {
		return update(null, (String[]) null, docs, block);
	}


	/**
	 *  Removes all Documents that match the given term within the given field. This is useful for removing a
	 *  single document that is indexed with a unique ID field, or to remove a group of documents mathcing the
	 *  same term for a given field. Blocks all other index update operations until this is complete.
	 *
	 * @param  field  The field that is searched.
	 * @param  value  The term that is matched for deletes.
	 * @return        True if the delete was successful.
	 */
	public boolean removeDocs(String field, String value) {
		return update(field, new String[]{value}, null, BLOCK);
	}


	/**
	 *  See {@link #removeDocs(String,String)} for description. Adds the ability to control whether blocking
	 *  occurs during the update.
	 *
	 * @param  field  The field that is searched.
	 * @param  value  The term that is matched for deletes.
	 * @param  block  Indicates whether or not to block other update operations.
	 * @return        True if the delete was successful.
	 */
	public boolean removeDocs(String field, String value, boolean block) {
		return update(field, new String[]{value}, null, block);
	}


	/**
	 *  Removes all documents that match the given terms within the given field. This is useful for removing all
	 *  individual documents that are indexed with a unique ID field. Blocks all other index update operations
	 *  until this is complete.
	 *
	 * @param  field   The field that is searched.
	 * @param  values  The terms that are matched for deletes.
	 * @return         True if the delete was successful.
	 */
	public boolean removeDocs(String field, String[] values) {
		return update(field, values, null, BLOCK);
	}


	/**
	 *  Updates the index by first deleting the documents that match the value(s) indicated in <code>deleteValues</code>
	 *  in the field <code>deleteField,</code> then adding the documents in <code>addDocs</code>. Assuming the
	 *  <code>deleteField</code> contains a unique ID for the Document, the Document may be removed by indicating
	 *  the ID in the <code>deleteValues</code> list. To replace an entry in the index for a single item, supply
	 *  the item's ID in the <code>deleteValues</code> list and supply the new Document for the item in the
	 *  <code>addDocs</code> list.
	 *
	 * @param  deleteField   The field searched for <code>deleteValues</code>.
	 * @param  deleteValues  The value matched in <code>deleteField</code> to indicate which document(s) to
	 *      delete.
	 * @param  addDocs       An array of Documents to add to the index
	 * @param  block         Indicates whether or not to block other threads or JVMs from read/write from the
	 *      index during the delete/add operation.
	 * @return               True if no errors, otherwise false.
	 */
	public boolean update(
	                      String deleteField,
	                      String[] deleteValues,
	                      Document[] addDocs,
	                      boolean block) {

		return doUpdateIndex(deleteField, deleteValues, addDocs);
	}


	/**
	 *  Updates the index by first deleting the documents that match the value(s) indicated in <code>deleteValues</code>
	 *  in the field <code>deleteField,</code> then adding the documents in <code>addDocs</code>. See {@link
	 *  #update(String, String[], Document[], boolean)} for description. Performs an update with blocking on.
	 *
	 * @param  deleteField   The field searched for <code>deleteValues</code>.
	 * @param  deleteValues  Array of Strings containing the value matched in <code>deleteField</code> to
	 *      indicate which document(s) to delete
	 * @param  addDocs       Array containing Documents to add to the index
	 * @return               True if no errors, otherwise false.
	 */
	public boolean update(String deleteField,
	                      String[] deleteValues,
	                      Document[] addDocs) {
		return update(deleteField, deleteValues, addDocs, BLOCK);
	}


	/**
	 *  See {@link #update(String, String[], Document[], boolean)} for description.
	 *
	 * @param  deleteField  The field searched for <code>deleteValue</code>.
	 * @param  deleteValue  Matching docs are deleted.
	 * @param  addDocs      These Docs are added to the index
	 * @param  block        Block or run in background.
	 * @return              True if no errors.
	 */
	public boolean update(String deleteField,
	                      String deleteValue,
	                      Document[] addDocs,
	                      boolean block) {
		return update(deleteField, new String[]{deleteValue}, addDocs, block);
	}


	/**
	 *  See {@link #update(String, String[], Document[], boolean)} for description.
	 *
	 * @param  deleteField  The field searched for <code>deleteValue</code>.
	 * @param  deleteValue  Matching docs are deleted.
	 * @param  addDoc       The Doc to be added to the index
	 * @param  block        Block or run in background.
	 * @return              True if no errors.
	 */
	public boolean update(String deleteField,
	                      String deleteValue,
	                      Document addDoc,
	                      boolean block) {
		return update(deleteField, new String[]{deleteValue}, new Document[]{addDoc}, block);
	}


	/**
	 *  See {@link #update(String, String[], Document[], boolean)} for description.
	 *
	 * @param  deleteField  The field searched for <code>deleteValue</code>.
	 * @param  deleteValue  Matching docs are deleted.
	 * @param  addDocs      These Docs are added to the index
	 * @param  block        Block or run in background.
	 * @return              True if no errors.
	 */
	public boolean update(String deleteField,
	                      String deleteValue,
	                      ArrayList addDocs,
	                      boolean block) {
		return update(deleteField, new String[]{deleteValue}, (Document[]) addDocs.toArray(new Document[]{}), block);
	}


	/**
	 *  Updates the index by first deleting the documents that match the value(s) indicated in <code>deleteValues</code>
	 *  in the field <code>deleteField,</code> then adding the documents in <code>addDocs</code>. See {@link
	 *  #update(String, String[], Document[], boolean)} for description.
	 *
	 * @param  deleteField   The field searched for <code>deleteValues</code>.
	 * @param  deleteValues  ArrayList of Strings containing the value matched in <code>deleteField</code> to
	 *      indicate which document(s) to delete
	 * @param  addDocs       An ArrayList containing Documents to add to the index
	 * @param  block         Indicates whether or not to block other threads or JVMs from read/write from the
	 *      index during the delete/add operation.
	 * @return               True if no errors, otherwise false.
	 */
	public boolean update(
	                      String deleteField,
	                      ArrayList deleteValues,
	                      ArrayList addDocs,
	                      boolean block) {

		try {
			return update(deleteField,
				(String[]) deleteValues.toArray(new String[]{}),
				(Document[]) addDocs.toArray(new Document[]{}),
				block);
		} catch (Throwable e) {
			prtlnErr("Failure to update(): " + e);
			//e.printStackTrace();
			return false;
		}
	}


	/**
	 *  Updates the index by first deleting the documents that match the value(s) indicated in <code>deleteValues</code>
	 *  in the field <code>deleteField,</code> then adding the documents in <code>addDocs</code>. See {@link
	 *  #update(String, String[], Document[], boolean)} for description. Performs an update with blocking on.
	 *
	 * @param  deleteField   The field searched for <code>deleteValues</code>.
	 * @param  deleteValues  ArrayList of Strings containing the value matched in <code>deleteField</code> to
	 *      indicate which document(s) to delete
	 * @param  addDocs       An ArrayList containing Documents to add to the index
	 * @return               True if no errors, otherwise false.
	 */
	public boolean update(
	                      String deleteField,
	                      ArrayList deleteValues,
	                      ArrayList addDocs) {
		return update(deleteField, deleteValues, addDocs, BLOCK);
	}


	/**
	 *  Gets the version number of the last time the index was modified by adding, deleting or changing a
	 *  document. The version number counts the number of times the index was modified. If the index is deleted
	 *  and rebuilt, the count will continue to be incremented until the next time the JVM is re-started. After
	 *  the JVM has been re-started, the count will resume with the count of the new current index.
	 *
	 * @return    The lastModifiedCount value
	 */
	public long getLastModifiedCount() {
		//prtln("getLastModifiedCount()");

		long lastMod = cachedLastModifiedCount;
		synchronized (_readerSearcherUpdateLock) {
			if (_myReader != null) {
				try {
					lastMod = _myReader.getCurrentVersion(FSDirectory.open(luceneIndexDir)) + cachedLastModifiedCount;
				} catch (Throwable e) {}
			}
		}
		return lastMod;
	}


	/**
	 *  Gets the nth document in the index.
	 *
	 * @param  n  The document number
	 * @return    The document value
	 */
	public Document getDocument(int n) {
		//prtln("getDocument()");

		Document doc = null;
		synchronized (_readerSearcherUpdateLock) {
			if (_myReader != null) {
				try {
					doc = _myReader.document(n);
				} catch (Throwable e) {
					prtlnErr("Error retrieving document " + n + ": " + e);
					//e.printStackTrace();
				}
			}
		}
		return doc;
	}


	/**
	 *  Indicates whether the index is currently being updated or modified. This means documents are in the
	 *  process of being added or removed from the index.
	 *
	 * @return    True if the index is in the process of being updated.
	 * @see       #stopIndexing()
	 */
	public boolean isIndexing() {
		return isIndexing;
	}


	/**
	 *  Instructs the indexer to stop processing updates. Once complete, the index will be ready for future
	 *  updating and searching but any additions or deletions that had not been completed will be lost. This
	 *  method may take several seconds to return.
	 *
	 * @see    #isIndexing()
	 */
	public void stopIndexing() {
		synchronized (_stopperLock) {
			abortUpdate = true;
			while (isIndexing)
				Thread.yield();
			abortUpdate = false;
		}
		
		if (optimizeMonitorThread != null) // stop the monitoring thread to avoid zombies
			optimizeMonitorThread.stopMonitoring();
		
	}


	private long optimize_count = 0;
	private long last_update_time = System.currentTimeMillis();
	private boolean is_optimized = true;


	/**
	 *  Updates the index by deleting and then adding Documents.
	 *
	 * @param  deleteField   The field name for which deletes should be checked.
	 * @param  deleteValues  Values found in the deleteField when matched, the Document will be deleted from the
	 *      index.
	 * @param  addDocs       Documents to add to the index after the deletes are performed.
	 * @return               True if successful.
	 */
	private final boolean doUpdateIndex(
	                                    String deleteField,
	                                    String[] deleteValues,
	                                    Document[] addDocs) {
		boolean complete = false;

		// Wait for other indexing updates to complete before continuing...
		synchronized (_updateLock) {
			IndexWriter tmpWriter = null;
			//boolean changed = false;
			try {
				//prtln("Beginning doUpdateIndex(). Num docs: " + getNumDocs());

				tmpWriter = new IndexWriter(FSDirectory.open(luceneIndexDir), getAnalyzer(), false, IndexWriter.MaxFieldLength.UNLIMITED);
				isIndexing = true;

				/* if (deleteValues != null && deleteValues.length > 0)
					prtln("processing deleteField: " + deleteField + " deleteValues[0]: " + deleteValues[0]);
				else
					prtln("processing deleteField: " + deleteField + " deleteValues[0]: " + deleteValues[0]); */
				// ---- Handle deletions first ----
				if (deleteField != null && deleteValues != null && deleteValues.length > 0) {
					//IndexWriter tmpWriter = null;
					/* try { */
					//tmpWriter = new IndexWriter(luceneIndexDir, getAnalyzer(), false);
					//int count = 0;
					for (int i = 0; i < deleteValues.length && !abortUpdate; i++) {
						if (deleteValues[i] != null) {
							try {
								tmpWriter.deleteDocuments(new Term(deleteField, deleteValues[i]));
								//changed = true;
								//prtln("Deleting " + deleteField + ":" + deleteValues[i] + " had " + count + " deletions");
							} catch (IOException ioe) {
								prtlnErr("Problem deleting a document: " + ioe);
							}
						}
					}
					/* if (count > 0) {
							changed = true;
						} */
					//tmpWriter.close();
					//tmpWriter = null;

					//prtln("\tdeleted " + count + " documents.");

					/* } catch (IOException ioe) {
						// this means we had trouble opening a reader on the writeDirectory?!
						prtlnErr("doUpdateIndex() deletion exception: " + ioe);
						try {
							if (tmpWriter != null) {
								tmpWriter.close();
								tmpWriter = null;
							}
						} catch (Throwable t) {
							prtlnErr("doUpdateIndex() error closing writer: " + t);
						}
						loadNewReaderAndSearcher();
						isIndexing = false;
						return false;
					} */
				}
				else {
					//prtln("\tno docs to remove");
				}

				// ---- Add in the new and/or replacement docs ----

				//IndexWriter tmpWriter = null;
				try {

					if (addDocs != null && addDocs.length > 0) {
						//tmpWriter = new IndexWriter(luceneIndexDir, getAnalyzer(), false);
						int count = 0;
						for (int i = 0; i < addDocs.length && !abortUpdate; i++) {

							if (addDocs[i] != null) {
								try {
									tmpWriter.addDocument(addDocs[i]);
									count++;
									optimize_count++;
								} catch (Throwable t) {
									// if one add fails for some reason,
									// we don't want to abort the rest...
									prtlnErr("Problem writing a document: " + t);
									if (t instanceof java.lang.NullPointerException)
										t.printStackTrace();
								}
							}
						}
						/* if (count > 0) {
							changed = true;
						} */
						//prtln("doUpdateIndex() added " + count + " documents.");

					}
					else {
						// prtln("no docs to write");
					}

					//if (changed) {
					last_update_time = System.currentTimeMillis();
					is_optimized = false;
					if (NUM_RECORDS_FOR_OPTIMIZER > 0 && optimize_count >= NUM_RECORDS_FOR_OPTIMIZER) {
						//prtln("Optimizer starting.");
						tmpWriter.optimize();
						optimize_count = 0;
						//prtln("Optimizer finished.");
						is_optimized = true;
					}
					//}

					/* if (tmpWriter != null) {
						tmpWriter.close();
						tmpWriter = null;
					} */
				} catch (IOException ioe) {
					// this means we had trouble opening a writer on the writeDirectory?!
					prtlnErr("doUpdateIndex() write document exception: " + ioe);
					try {
						if (tmpWriter != null) {
							tmpWriter.close();
							tmpWriter = null;
						}
					} catch (Throwable t) {
						prtlnErr("doUpdateIndex() error closing writer: " + t);
					}
					isIndexing = false;
					loadNewReaderAndSearcher();
					return false;
				}

				// finsihed processing index updates...

				//System.gc();
				//System.runFinalization();

				complete = true;

			} catch (Throwable e) {
				prtlnErr("doUpdateIndex(): " + e);
				e.printStackTrace();
			} finally {
				isIndexing = false;
				try {
					if (tmpWriter != null) {
						tmpWriter.close();
						tmpWriter = null;
					}
					// Load new reader and searcher so changes will be seen:
					loadNewReaderAndSearcher();
				} catch (Throwable t) {
					prtlnErr("doUpdateIndex() error closing writer: " + t);
				}
				//prtln("Finished doUpdateIndex(). Num docs: " + getNumDocs());
			}
		}

		return complete;
	}


	/**
	 *  Monitors the index and performs delayed optimization if enough time has elapsed since the last write
	 *  operation.
	 *
	 * @author    jweather
	 */
	private class OptimizeMonitorThread extends Thread {
		//private Object syncObj = null;
		private boolean isRunning = true;


		/**
		 *  Constructor for the OptimizeMonitorThread object
		 *
		 * @param  syncObj  The object used to synchronize indexing write operations
		 */
		public OptimizeMonitorThread(Object syncObj) {
			//this.syncObj = syncObj;
			setDaemon(true);
		}


		/**  Tells this thread to stop */
		public void stopMonitoring() {
			isRunning = false;
			interrupt();
		}


		/**  Main processing method for the OptimizeMonitorThread object */
		public void run() {
			while (isRunning) {
				try {
					sleep(OPTIMIZE_DELAY_TIME);
					//prtln("Optimizer thread: checking if need to optimize...");
					if (!is_optimized) {
						// if enough time has passed, optimize...
						if ((System.currentTimeMillis() - last_update_time) > OPTIMIZE_DELAY_TIME) {
							synchronized (_updateLock) {
								if (isRunning) {
									IndexWriter tmpWriter = null;
									try {
										tmpWriter = new IndexWriter(FSDirectory.open(luceneIndexDir), getAnalyzer(), false, IndexWriter.MaxFieldLength.UNLIMITED);
										tmpWriter.optimize();
										tmpWriter.close();
										tmpWriter = null;
										last_update_time = System.currentTimeMillis();
										is_optimized = true;
										loadNewReaderAndSearcher();
									} catch (Throwable t) {
										prtlnErr("Optimizer thread: Unable to optimize index: " + t);
										try {
											if (tmpWriter != null) {
												tmpWriter.close();
												tmpWriter = null;
											}
										} catch (Throwable t2) {
											prtlnErr("Optimizer thread: Error closing writer: " + t2);
										}
										last_update_time = System.currentTimeMillis();
									}
								}
							}
						}
					}
				} catch (Throwable t3) {
					if (!isRunning) // we interrupt if we are stopping te thread, do not show error message then 
					prtlnErr("Optimizer thread: Indexing excetpion: " + t3);
				}
			}
		}
	}


	/**
	 *  Gets the analyzer that has been configured for this index.
	 *
	 * @return    The Analyzer
	 */
	public final Analyzer getAnalyzer() {
		return defaultAnalyzer;
	}



	/**  Closes the writers and performs clean-up */
	public void close() {
		try {
			stopIndexing();
			synchronized (_readerSearcherUpdateLock) {
				if (_myReader != null)
					_myReader.close();
				if (_mySearcher != null)
					_mySearcher.close();
				if (optimizeMonitorThread != null)
					optimizeMonitorThread.stopMonitoring();
				_myReader = null;
				_mySearcher = null;
			}
			optimizeMonitorThread = null;
		} catch (Exception e) {}
	}


	/**  Override finalize to ensure resources are released... */
	protected void finalize() {
		try {
			close();
			super.finalize();
			//prtln("SimpleLuceneIndex.finalize()");
		} catch (Throwable e) {
			prtlnErr("SimpleLuceneIndex.finalize(): " + e);
		}
	}


	/**
	 *  Escapes all Lucene QueryParser reserved characters with a preceeding \. The resulting String will be
	 *  interpereted by the QueryParser as a single term.
	 *
	 * @param  term  The original String
	 * @return       The escaped term
	 * @see          org.apache.lucene.queryParser.QueryParser#escape(String)
	 */
	public final static String escape(String term) {
		try {
			return QueryParser.escape(term);
		} catch (Throwable e) {
			return term;
		}
	}


	/**
	 *  Escapes the Lucene QueryParser reserved characters with a preceeding \ except those included in
	 *  preserveChars.
	 *
	 * @param  term           The original String
	 * @param  preserveChars  List of characters NOT to escape
	 * @return                The escaped term
	 * @see                   org.apache.lucene.queryParser.QueryParser#escape(String)
	 */
	public final static String escape(String term, String preserveChars) {
		if (term == null)
			return null;
		try {
			if (preserveChars == null)
				return QueryParser.escape(term);
			else {
				String escaped = QueryParser.escape(term);
				char[] chars = preserveChars.toCharArray();
				for (int i = 0; i < chars.length; i++)
					escaped = escaped.replaceAll("\\\\\\Q" + chars[i] + "\\E", "" + chars[i]);
				return escaped;
			}
		} catch (Throwable e) {
			return term;
		}
	}


	/**
	 *  Encodes a String to an appropriate format that can be indexed as a single term using a StandardAnalyzer.
	 *  White-space is also encoded and incorporated into the single term. Note that this can not be unencoded.
	 *  Save the value of the term in a separate field if it needs to be retrieved for display.<p>
	 *
	 *  Specifically: each letter or number character is left unchanded. All other characters are encoded as the
	 *  letter 'x' followed by the integer value of the character, for example '@' is encoded as 'x64'.
	 *
	 * @param  s  The string to encode.
	 * @return    Encoded String that can be used as a single term.
	 */
	public final static String encodeToTerm(String s) {
		return encodeToTerm(s, true, true);
	}


	/**
	 *  Encodes a String to an appropriate format that can be indexed as a single term using a StandardAnalyzer.
	 *  White-space is also encoded and incorporated into the single term. Leaving the wild card '*' char
	 *  un-encoded will produce a String that can be used to search encoded terms using wild cards. Note that
	 *  this can not be unencoded. Save the value of the term in a separate field if it needs to be retrieved for
	 *  display.<p>
	 *
	 *  Specifically: each letter or number character is left unchanded. All other characters are encoded as the
	 *  letter 'x' followed by the integer value of the character, for example '@' is encoded as 'x64'.
	 *
	 * @param  s                The string to encode.
	 * @param  encodeWildCards  True to have the '*' char encoded, false to leave it un-encoded.
	 * @return                  Encoded String that can be used as a single term.
	 */
	public final static String encodeToTerm(String s, boolean encodeWildCards) {
		return encodeToTerm(s, encodeWildCards, false);
	}


	/**
	 *  Encodes a String to an appropriate format that can be indexed as a single term or terms using a
	 *  StandardAnalyzer. Leaving the space char un-encoded will produce a String that will be tokenized by the
	 *  space char into individual terms. Leaving the wild card '*' char un-encoded will produce a String that
	 *  can be used to search encoded terms using wild cards. Note that this can not be unencoded. Save the value
	 *  of the term in a separate field if it needs to be retrieved for display.<p>
	 *
	 *  Specifically: each letter or number character is left unchanded. All other characters are encoded as the
	 *  letter 'x' followed by the integer value of the character, for example '@' is encoded as 'x64'.
	 *
	 * @param  s                The string to encode.
	 * @param  encodeWildCards  True to have the '*' char encoded, false to leave it un-encoded.
	 * @param  encodeSpace      True to have the space ' ' char encoded, false to leave it un-encoded.
	 * @return                  Encoded String that can be used as a single term or terms.
	 */
	public final static String encodeToTerm(String s, boolean encodeWildCards, boolean encodeSpace) {
		if (s == null)
			return null;
		StringBuffer buf = new StringBuffer();
		char[] chars = s.toCharArray();

		int intVal;
		for (int i = 0; i < chars.length; i++) {
			if (Character.isLetterOrDigit(chars[i]))
				buf.append(chars[i]);
			else {
				if (encodeWildCards == false && chars[i] == '*')
					buf.append("*");
				else if (encodeSpace == false && chars[i] == ' ')
					buf.append(" ");
				else {
					intVal = (int) chars[i];
					buf.append("x");
					buf.append(intVal);
				}
			}
		}
		return buf.toString().toLowerCase();
	}


	/**
	 *  Gets /the version of Lucene.
	 *
	 * @return    The luceneVersion value
	 */
	public static Version getLuceneVersion() {
		return luceneVersion;
	}


	// ---------------------- Debug methods -------------------------------
	/**
	 *  Gets a datestamp of the current time formatted for display with logs and output.
	 *
	 * @return    A datestamp for display purposes.
	 */
	public final static String getDateStamp() {
		return
			new SimpleDateFormat("MMM d, yyyy h:mm:ss a zzz").format(new Date());
	}


	/**
	 *  Output a line of text to error out, with datestamp.
	 *
	 * @param  s  The text that will be output to error out.
	 */
	private final void prtlnErr(String s) {
		System.err.println(getDateStamp() + " SimpleLuceneIndex ERROR: " + s);
	}



	/**
	 *  Output a line of text to standard out, with datestamp, if debug is set to true.
	 *
	 * @param  s  The String that will be output.
	 */
	private final static void prtln(String s) {
		if (debug)
			System.out.println(getDateStamp() + " SimpleLuceneIndex: " + s);
	}


	/**
	 *  Sets the debug attribute of the SimpleLuceneIndex object
	 *
	 * @param  db  The new debug value
	 */
	public static void setDebug(boolean db) {
		debug = db;
	}
}

