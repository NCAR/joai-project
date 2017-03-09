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
import java.util.*;
import java.text.SimpleDateFormat;

import org.dlese.dpc.index.reader.*;

import java.io.*;

/**
 *  A factory container for a hit that is returned from a {@link org.dlese.dpc.index.SimpleLuceneIndex}
 *  search. This factory uses the class name provided in the {@link
 *  org.dlese.dpc.index.writer.DocWriter#getReaderClass()} method to return the {@link
 *  org.dlese.dpc.index.reader.DocReader} bean that is best suited to read the type of index {@link
 *  org.apache.lucene.document.Document} that was returned by the search. Also provides access to a {@link
 *  org.dlese.dpc.index.reader.DocumentMap} for reading the fields in the lucene Document. This container also
 *  provides acces to the {@link org.apache.lucene.document.Document}, the score rank and the document number.
 *  <p>
 *
 *  Example that uses JSTL inside a JSP page (assumes result is an instance of ResultDoc and that the
 *  DocReader has a method named getFormattedDate()): <p>
 *
 *  <code>
 *  The title is: ${result.docMap["title"]}.
 *  The date is: ${result.docReader.formattedDate}.
 *  </code>
 *
 * @author    John Weatherley
 * @see       SimpleLuceneIndex
 * @see       org.dlese.dpc.index.reader.DocReader
 * @see       org.dlese.dpc.index.reader.DocumentMap
 */
public class ResultDoc implements Comparable, Serializable {
	private static boolean debug = true;

	private float _score;
	private int _docNum;
	private ResultDocConfig _resultDocConfig = null;
	private Document _document = null;

	private DocReader docReader = null;
	private DocumentMap documentMap = null;
	private LazyDocumentMap lazyDocumentMap = null;
	private String comparatorField = null;


	/**
	 *  Gets the {@link org.dlese.dpc.index.reader.DocReader} used to read the specific {@link
	 *  org.apache.lucene.document.Document} type that is returned in a search. Remember that all concrete
	 *  DocReader classes must have a default constructor with no arguments.
	 *
	 * @return    The docReader value
	 * @see       org.dlese.dpc.index.reader.DocReader
	 */
	public final DocReader getDocReader() {
		if (docReader != null) {
			return docReader;
		}
		try {
			String className = getDocument().get("readerclass");

			if (className == null) {
				docReader = new SimpleDocReader();
				docReader.doInit(getDocument(), this, _score, _resultDocConfig);
				return docReader;
			}

			//A factory that dynamically loads the appropriate DocReader...
			try {
				Class docReaderClass = Class.forName(className);
				docReader = (DocReader) docReaderClass.newInstance();
				docReader.doInit(getDocument(), this, _score, _resultDocConfig);
				return docReader;
			} catch (Throwable e) {
				// If we can't instantiate the docReader, create a generic one
				prtlnErr("Error loading DocReader class '" + className + "'. " + e);
				e.printStackTrace();
				docReader = new SimpleDocReader();
				docReader.doInit(getDocument(), this, _score, _resultDocConfig);
				return docReader;
			}
		} catch (Exception e) {
			return new SimpleDocReader();
		}
	}


	/**
	 *  Gets a {@link org.dlese.dpc.index.reader.DocumentMap} of all field/values contained in the Lucene {@link
	 *  org.apache.lucene.document.Document}. The text values in each field is stored in the Map as Strings. For
	 *  example getDocMap.get("title") returns the text that was indexed and stored under the field name "title"
	 *  for this Document.<p>
	 *
	 *  Example that uses JSTL inside a JSP page (assumes result is an instance of ResultDoc):<p>
	 *
	 *  <code>
	 *  The title is: ${result.docMap["title"]}
	 *  </code>
	 *
	 * @return    A Map of all field/values contained in this Document, or null
	 * @see       org.dlese.dpc.index.reader.DocumentMap
	 */
	public DocumentMap getDocMap() {
		if (documentMap == null)
			documentMap = new DocumentMap(getDocument());

		return documentMap;
	}


	/**
	 *  Gets a {@link org.dlese.dpc.index.reader.LazyDocumentMap} of all field/values contained in the Lucene
	 *  {@link org.apache.lucene.document.Document}. The text values in each field is stored in the Map as
	 *  Strings. For example getDocMap.get("title") returns the text that was indexed and stored under the field
	 *  name "title" for this Document.<p>
	 *
	 *  Example that uses JSTL inside a JSP page (assumes result is an instance of ResultDoc):<p>
	 *
	 *  <code>
	 *  The title is: ${result.docMap["title"]}
	 *  </code>
	 *
	 * @return    A Map of all field/values contained in this Document, or null
	 * @see       org.dlese.dpc.index.reader.DocumentMap
	 */
	public LazyDocumentMap getLazyDocMap() {
		if (lazyDocumentMap == null)
			lazyDocumentMap = new LazyDocumentMap(getDocument());

		return lazyDocumentMap;
	}


	/**
	 *  Gets the doctype for this ResultDoc. Assumes there is a stored field in the Lucene document named
	 *  'doctype' that contains the docytype name such as "dlese_ims" or "adn."
	 *
	 * @return    The doctype value
	 */
	public final String getDoctype() {
		String doctype = getDocument().get("doctype");
		if (doctype == null)
			return "";
		else
			return doctype.substring(1, doctype.length());
	}


	/**
	 *  Gets the readerClass attribute of the ResultDoc object
	 *
	 * @return    The readerClass value
	 */
	public final String getReaderClass() {
		String readerclass = getDocument().get("readerclass");
		if (readerclass == null)
			return "";
		else
			return readerclass;
	}


	/**
	 *  Constructor for the ResultDoc that is used by the {@link org.dlese.dpc.index.SimpleLuceneIndex} class
	 *  during search result hit list compilation to create a {@link ResultDocCollection}.
	 *
	 * @param  c   The configuration for this ResultDoc
	 * @param  dn  The Lucene document number for the {@link org.apache.lucene.document.Document} associated with
	 *      this ResultDoc.
	 * @param  s   The rank assigned to this result by the Lucene search engine.
	 */
	public ResultDoc(ResultDocConfig c, int dn, float s) {
		_resultDocConfig = c;
		_docNum = dn;
		_score = s;
	}


	/**
	 *  Constructor for the ResultDoc that is used by the {@link org.dlese.dpc.index.SimpleLuceneIndex} class
	 *  during search result hit list compilation to create a {@link ResultDocCollection}.
	 *
	 * @param  d  A Lucene {@link org.apache.lucene.document.Document} used to populate this ResultDoc.
	 * @param  c  The configuration for this ResultDoc
	 */
	public ResultDoc(Document d, ResultDocConfig c) {
		_document = d;
		_resultDocConfig = c;
		_score = 0;
		_docNum = -1;
	}


	/**
	 *  Constructs a ResultDoc using the {@link org.apache.lucene.document.Document} provided. Use this
	 *  constructor to wrap a {@link org.apache.lucene.document.Document} inside a ResultDoc.
	 *
	 * @param  doc  A Lucene {@link org.apache.lucene.document.Document} used to populate this ResultDoc.
	 */
	public ResultDoc(Document doc) {
		_document = doc;
		_score = 0;
		_docNum = -1;
	}


	/**
	 *  Gets the Lucene {@link org.apache.lucene.document.Document} associated with this ResultDoc. If the index
	 *  has changed since the search was conducted, this method may return an empty or incorrect Document. It is
	 *  therefore best to read all Documents as soon as possible after a search if the index is being
	 *  concurrently modified.
	 *
	 * @return    The {@link org.apache.lucene.document.Document} associated with this ResultDoc.
	 */
	public final Document getDocument() {
		try {
			if (_document == null)
				_document = _resultDocConfig.index.getReader().document(_docNum);
			return _document;
		} catch (Throwable e) {
			prtlnErr("Error retrieving document: " + e);
			e.printStackTrace();
			return new Document();
		}
	}


	/**
	 *  Gets the field content used by {@link LuceneFieldComparator} for sorting. Note that it is not possible to
	 *  re-sort a single set of ResultDocs. To re-sort, first do a fresh search, then a fresh sort over the new
	 *  ResultDocs using the differnt field.
	 *
	 * @param  field  The field name used for sorting
	 * @return        The given field content
	 * @see           LuceneFieldComparator
	 * @deprecated    Sorting should now be done by supplying a {@link org.apache.lucene.search.Sort} object at
	 *      search time. Sorting on returned ResultDocs is less efficient and may cause OutOfMemory errors on large
	 *      result sets.
	 */
	public final String getComparatorField(String field) {
		if (comparatorField == null) {
			try {
				comparatorField = getDocument().get(field);
			} catch (Throwable e) {
				prtlnErr("Error retrieving comparatorField: " + e);
				e.printStackTrace();
				comparatorField = "";
			}
		}
		return comparatorField;
	}


	/**
	 *  Gets the index that was searched over.
	 *
	 * @return    The index.
	 */
	public final SimpleLuceneIndex getIndex() {
		return _resultDocConfig.index;
	}


	/**
	 *  Gets an attribute that has been previously set using {@link SimpleLuceneIndex#setAttribute(String,Object)}.
	 *
	 * @param  key  The key for the attribute
	 * @return      The attruibute, or null if none exists under the given key
	 * @see         SimpleLuceneIndex#setAttribute(String,Object)
	 */
	public Object getAttribute(String key) {
		try {
			return _resultDocConfig.index.getAttribute(key);
		} catch (Throwable e) {
			return null;
		}
	}


	/**
	 *  Gets the query that was used in the search.
	 *
	 * @return    The query value
	 */
	public final String getQuery() {
		return _resultDocConfig.query;
	}


	/**
	 *  Gets the score assigned to this ResultDoc by the Lucene engine.
	 *
	 * @return    The score.
	 */
	public final float getScore() {
		return _score;
	}


	/**
	 *  Compares two ResultDocs for sorting by score.
	 *
	 * @param  obj  A ResultDoc to compare with this one.
	 * @return      Negative is less-than, positive if greater-than or zero if equal.
	 */
	public final int compareTo(Object obj) {
		if (((ResultDoc) obj)._score < _score) {
			return -1;
		}
		else if (((ResultDoc) obj)._score > _score) {
			return 1;
		}
		else {
			return 0;
		}
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
		System.err.println(getDateStamp() + " ResultDoc ERROR: " + s);
	}



	/**
	 *  Output a line of text to standard out, with datestamp, if debug is set to true.
	 *
	 * @param  s  The String that will be output.
	 */
	private final static void prtln(String s) {
		if (debug)
			System.out.println(getDateStamp() + " ResultDoc: " + s);
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


