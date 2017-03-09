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
package org.dlese.dpc.index.queryParser;

import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.*;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Token;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.queryParser.ParseException;
import java.util.*;
import java.text.SimpleDateFormat;
import java.io.*;
import java.net.URL;

import org.dom4j.Document;
import org.dom4j.Node;
import org.dom4j.Element;
import org.dom4j.Attribute;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;

import org.dlese.dpc.index.reader.*;
import org.dlese.dpc.xml.Dom4jUtils;

/**
 *  Creates a Lucene Query from an XML representation of a query. See <a
 *  href="../../../../../javadoc-includes/XMLQueryParser-sample.xml">sample Query XML file</a> . Currently
 *  supports term, phrase, boolean and Lucene query syntax formatted queries, with option to set the boost
 *  factor assigned to any query (default boost is 1.0). If a Lucene query syntax based query may be found in
 *  the Query XML, then a Lucene QueryParser must be supplied, otherwise it may be ommitted.
 *
 * @author    John Weatherley
 * @see       org.dlese.dpc.index.VirtualSearchFieldMapper
 */
public class XMLQueryParser {
	private boolean debug = true;


	/**
	 *  Gets the Lucene query String representation for the given XML Query.
	 *
	 * @param  queryXml       A XML Query
	 * @param  queryParser    QueryParser used to parse Lucene syntax based queries and tokenize the text
	 * @return                The queryString value
	 * @exception  Exception  If error
	 */
	public static String getQueryString(String queryXml, QueryParser queryParser) throws Exception {
		Query query = getLuceneQuery(queryXml, queryParser);
		if (query == null)
			return "";
		return query.toString();
	}


	/**
	 *  Gets the Lucene Query representation for the given XML Query.
	 *
	 * @param  queryXml       A file containing an XML Query
	 * @param  queryParser    QueryParser used to parse Lucene syntax based queries and tokenize the text
	 * @return                The Lucene Query object representation of this XML Query
	 * @exception  Exception  If error
	 */
	public static Query getLuceneQuery(File queryXml, QueryParser queryParser) throws Exception {
		return doGetLuceneQuery(Dom4jUtils.getXmlDocument(new URL("file://" + queryXml.getAbsolutePath())), queryParser);
	}


	/**
	 *  Gets the Lucene Query representation for the given XML Query.
	 *
	 * @param  queryXml       A URL to a file containing an XML Query
	 * @param  queryParser    QueryParser used to parse Lucene syntax based queries and tokenize the text
	 * @return                The Lucene Query object representation of this XML Query
	 * @exception  Exception  If error
	 */
	public static Query getLuceneQuery(URL queryXml, QueryParser queryParser) throws Exception {
		return doGetLuceneQuery(Dom4jUtils.getXmlDocument(queryXml), queryParser);
	}


	/**
	 *  Gets the Lucene Query representation for the given XML Query.
	 *
	 * @param  queryXml       An XML Query
	 * @param  queryParser    QueryParser used to parse Lucene syntax based queries and tokenize the text
	 * @return                The Lucene Query object representation of this XML Query
	 * @exception  Exception  If error
	 */
	public static Query getLuceneQuery(String queryXml, QueryParser queryParser) throws Exception {
		return doGetLuceneQuery(Dom4jUtils.getXmlDocument(queryXml), queryParser);
	}


	private static Query doGetLuceneQuery(Document xmlDoc, QueryParser queryParser) throws Exception {
		List queryElement = xmlDoc.selectNodes("/Query/*");
		if (queryElement == null || queryElement.size() == 0)
			throw new Exception("Error parsing Query: root element '<Query>' is empty or missing.");
		if (queryElement.size() > 1)
			throw new Exception("Error parsing Query: '<Query>' may contain only 1 child element but " + queryElement.size() + " were found.");

		return getLuceneQuery((Element) queryElement.get(0), queryParser);
	}


	/**
	 *  Gets the Lucene Query representation for the given XML Query starting at the Query element.
	 *
	 * @param  queryElement   A dom4j representation of the Query element
	 * @param  queryParser    QueryParser used to parse Lucene syntax based queries and tokenize the text
	 * @return                The Lucene Query object representation of this XML Query
	 * @exception  Exception  If error
	 */
	public static Query getLuceneQuery(Element queryElement, QueryParser queryParser) throws Exception {
		// Check if requested to exclude from results, which is an error if not within a boolean clause
		String excludeOrRequire = queryElement.attributeValue("excludeOrRequire");
		if (excludeOrRequire != null)
			throw new Exception("Error parsing document: attribute excludeOrRequire may only be used when the query is enclosed in an encompassing <booleanQuery>. Error found at: " + queryElement.getUniquePath());

		if (queryElement.getName().equals("booleanQuery"))
			return makeBooleanQuery(queryElement, queryParser);
		else if (queryElement.getName().equals("textQuery"))
			return makeLuceneQuery(queryElement, queryParser);
		else if (queryElement.getName().equals("luceneQuery"))
			return makeLuceneQuery(queryElement, queryParser);
		else
			throw new Exception("Error parsing document: invalid element name '<" + queryElement.getName() + ">' at " + queryElement.getUniquePath());
	}



	// ------------------- Methods that parse the query  XML to create Lucene Queries ------------------

	private static Query makeBooleanQuery(Element booleanQueryElement, QueryParser queryParser) throws Exception {
		String operator = booleanQueryElement.valueOf("@type");

		boolean isRequired = false;
		boolean isProhibited = false;

		if (operator == null)
			throw new Exception("Error parsing document: element <booleanQuery> must contain an attribite named 'type' that contains the value 'AND' or 'OR'. Error found at " + booleanQueryElement.getUniquePath());
		else if (operator.equalsIgnoreCase("OR"))
			isRequired = false;
		else if (operator.equalsIgnoreCase("AND"))
			isRequired = true;
		else
			throw new Exception("Error parsing document: element <booleanQuery> must contain an attribite named 'type' that contains the value 'AND' or 'OR' but value '" + operator + "' was found. Error found at " + booleanQueryElement.getUniquePath());

		BooleanQuery booleanQuery = new BooleanQuery();

		// iterate through child elements of booleanClause
		Query query = null;
		for (Iterator i = booleanQueryElement.elementIterator(); i.hasNext(); ) {
			Element element = (Element) i.next();

			// Exclude from results or require (overrides previous boolean designation)?
			String excludeOrRequire = element.attributeValue("excludeOrRequire");
			if (excludeOrRequire != null) {
				excludeOrRequire = excludeOrRequire.trim();
				if (excludeOrRequire.equalsIgnoreCase("exclude")) {
					isRequired = false;
					isProhibited = true;
				}
				else if (excludeOrRequire.equalsIgnoreCase("require")) {
					isRequired = true;
					isProhibited = false;
				}
				else if (excludeOrRequire.equalsIgnoreCase("neither")) {
					isRequired = false;
					isProhibited = false;
				}
				else {
					throw new Exception("Error parsing document: the value of attribute excludeOrRequire must be one of 'exclude', 'require' or 'neither' but '"
						 + excludeOrRequire + "' was found at " + booleanQueryElement.getUniquePath());
				}
			}

			if (element.getName().equals("booleanQuery")) {
				query = makeBooleanQuery(element, queryParser);
				if (query != null) {
					if (isRequired && !isProhibited)
						booleanQuery.add(query, BooleanClause.Occur.MUST);
					else if (!isRequired && isProhibited)
						booleanQuery.add(query, BooleanClause.Occur.MUST_NOT);
					else
						booleanQuery.add(query, BooleanClause.Occur.SHOULD);
				}
			}
			else {
				query = makeLuceneQuery(element, queryParser);
				if (query != null) {
					if (isRequired && !isProhibited)
						booleanQuery.add(query, BooleanClause.Occur.MUST);
					else if (!isRequired && isProhibited)
						booleanQuery.add(query, BooleanClause.Occur.MUST_NOT);
					else
						booleanQuery.add(query, BooleanClause.Occur.SHOULD);
				}
			}
		}

		return applyBoost(booleanQueryElement, booleanQuery);
	}


	private static Query makeLuceneQuery(Element luceneQueryElement, QueryParser queryParser) throws Exception {
		Query query;

		// Handle textQuery:
		if (luceneQueryElement.getName().equals("textQuery")) {
			String type = luceneQueryElement.valueOf("@type");
			String field = luceneQueryElement.valueOf("@field");
			String text = luceneQueryElement.getText();

			if (text == null || text.trim().length() == 0)
				return null;
			if (type == null || type.trim().length() == 0)
				throw new Exception("Error parsing document: element <textQuery> has empty or missing attribute 'type'. Error found at " + luceneQueryElement.getUniquePath());

			if (field == null || field.trim().length() == 0)
				field = queryParser.getField();

			if (type.equals("matchAllTerms") || type.equals("matchAnyTerm")) {
				BooleanQuery booleanQuery = new BooleanQuery();
				boolean isRequired = true;
				if (type.equals("matchAnyTerm"))
					isRequired = false;
				synchronized (queryParser) {
					TokenStream tokenStream = queryParser.getAnalyzer().tokenStream(field, new StringReader(text));
					tokenStream.reset();

					do {
						Token token = tokenStream.getAttribute(new Token().getClass());
						if (token != null) {
							if (isRequired)
								booleanQuery.add(new TermQuery(new Term(field, token.term())), BooleanClause.Occur.MUST);
							else
								booleanQuery.add(new TermQuery(new Term(field, token.term())), BooleanClause.Occur.SHOULD);
						}
					} while (tokenStream.incrementToken());

					tokenStream.end();
					tokenStream.close();
				}
				query = booleanQuery;
			}
			else if (type.equals("matchPhrase")) {
				PhraseQuery phraseQuery = new PhraseQuery();
				synchronized (queryParser) {
					TokenStream tokenStream = queryParser.getAnalyzer().tokenStream(field, new StringReader(text));
					tokenStream.reset();

					do {
						Token token = tokenStream.getAttribute(new Token().getClass());
						if (token != null) {
							phraseQuery.add(new Term(field, token.term()));
						}
					} while (tokenStream.incrementToken());

					tokenStream.end();
					tokenStream.close();
				}
				query = phraseQuery;
			}
			else if (type.equals("matchKeyword")) {
				query = new TermQuery(new Term(field, text));
			}
			// Add support for RangeQuery and others here later...
			else
				throw new Exception("Error parsing document: element <textQuery> type attribute was found to be '" + type + "' but must be of one of 'matchAllTerms', 'matchAnyTerm', 'matchPhrase' or 'matchKeyword'. Error found at " + luceneQueryElement.getUniquePath());
		}

		// Handle luceneQuery:
		else if (luceneQueryElement.getName().equals("luceneQuery")) {
			String queryString = luceneQueryElement.getText();
			if (queryString == null)
				throw new Exception("Error parsing document: element <luceneQuery> is missing the attribute 'query'. Error found at " + luceneQueryElement.getUniquePath());
			if (queryParser == null)
				throw new Exception("Error parsing document: <luceneQuery> element found at " + luceneQueryElement.getUniquePath() + " but the Lucene queryParser is null");

			synchronized (queryParser) {
				try {
					query = queryParser.parse(queryString);
				} catch (ParseException pe) {
					throw new Exception("These was a Lucene query syntax error found at " + luceneQueryElement.getUniquePath() + ". Error was: " + pe.getMessage());
				}
			}
		}
		else
			throw new Exception("Error parsing document: invalid element name '<" + luceneQueryElement.getName() + ">'. Error found at " + luceneQueryElement.getUniquePath());

		return applyBoost(luceneQueryElement, query);
	}


	private static Query applyBoost(Element queryElement, Query query) throws Exception {
		if (query == null || queryElement == null)
			return query;

		String boost = queryElement.valueOf("@boost");
		if (boost != null && boost.length() > 0) {
			float b = 1;
			try {
				b = Float.parseFloat(boost);
			} catch (NumberFormatException nfe) {
				String path = queryElement.attribute("boost").getUniquePath();
				throw new Exception("Error parsing document: boost value was not valid (" +
					nfe.getMessage() + "). Value must be a number, for example 1.5, 2.4. Error found at " + path);
			}
			query.setBoost(b);
		}
		return query;
	}


	//================================================================

	/**
	 *  Return a string for the current time and date, sutiable for display in log files and output to standout:
	 *
	 * @return    The dateStamp value
	 */
	private static String getDateStamp() {
		return
			new SimpleDateFormat("MMM d, yyyy h:mm:ss a zzz").format(new Date());
	}


	/**
	 *  Output a line of text to error out, with datestamp.
	 *
	 * @param  s  The text that will be output to error out.
	 */
	private final void prtlnErr(String s) {
		System.err.println(getDateStamp() + " XMLQueryParser Error: " + s);
	}


	/**
	 *  Output a line of text to standard out, with datestamp, if debug is set to true.
	 *
	 * @param  s  The String that will be output.
	 */
	private final void prtln(String s) {
		if (debug)
			System.out.println(getDateStamp() + " XMLQueryParser: " + s);
	}


	/**
	 *  Sets the debug attribute of the DocumentService object
	 *
	 * @param  db  The new debug value
	 */
	private final void setDebug(boolean db) {
		debug = db;
	}
}


