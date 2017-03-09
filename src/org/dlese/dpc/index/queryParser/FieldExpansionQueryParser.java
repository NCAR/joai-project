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

import java.io.*;
import java.util.*;

import org.apache.lucene.analysis.*;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.*;
import org.dlese.dpc.index.SimpleLuceneIndex;
import org.apache.lucene.queryParser.FastCharStream;
import org.apache.lucene.queryParser.TokenMgrError;

import java.text.SimpleDateFormat;

import org.dlese.dpc.util.*;
import org.dlese.dpc.propertiesmgr.*;
import org.dlese.dpc.index.analysis.*;
import org.dlese.dpc.index.VirtualSearchFieldMapper;

/**
 *  A QueryParser that modifies a user's query by expanding the fields that are searched, applying boosting,
 *  and applying Query mappings for the given virtual field/terms. The static parse method must be used - see
 *  it's definition for details.
 *
 * @author     John Weatherley
 * @see        org.dlese.dpc.index.VirtualSearchFieldMapper
 */
public class FieldExpansionQueryParser extends QueryParser {
	private static boolean debug = true;

	private Analyzer analyzer = null;

	private String[] expansionFields = null;
	private String[] boostingFields = null;
	private Map boostValues = null;
	private List terms = new ArrayList();
	private VirtualSearchFieldMapper virtualSearchFieldMapper = null;


	/**
	 *  Constructor for the FieldExpansionQueryParser. Private to enforce single use of this Object through the
	 *  static parse method.
	 *
	 * @param  izer          Analyzer used
	 * @param  ef            Fields to search
	 * @param  bf            Fields to boost
	 * @param  bv            Boost factors
	 * @param  mappedFields  Mapped fields to replace
	 */
	private FieldExpansionQueryParser(
	                                  Analyzer izer,
	                                  String[] ef,
	                                  String[] bf,
	                                  Map bv,
	                                  VirtualSearchFieldMapper mappedFields) {
		super(SimpleLuceneIndex.getLuceneVersion(),null, izer);
		analyzer = izer;

		expansionFields = ef;
		boostingFields = bf;
		boostValues = bv;
		virtualSearchFieldMapper = mappedFields;
	}


	/**
	 *  Parses the query text to create an expanded Lucene Query. Default text is searched in the given expansion
	 *  fields with boosting applied for the given boosting fields. At least one field must be supplied in the
	 *  expansion fields in order to form a valid query.<p>
	 *
	 *  If <i>n</i> fields are specified, this effectively constructs: <pre>
	 * <code>
	 * (field1:query) (field2:query) (field3:query)...(field<i>n</i>:query) </code> </pre>
	 *
	 * @param  query               The query text
	 * @param  analyzer            The Analyzer to user
	 * @param  expansionFields     Fields to search in for default text, which must contain at least one field
	 * @param  boostingFields      Fields to boost terms found in the default text, or null
	 * @param  boostValues         A Map of boosting values corresponding the the boostingFields - Map of
	 *      String/Float pairs, or null
	 * @param  mappedFields        Field/term mappings to apply, or null
	 * @param  defaultOperator     Default AND or OR operator
	 * @return                     A Query
	 * @exception  ParseException  If unable to parse the query
	 */
	public static Query parse(String query,
	                          Analyzer analyzer,
	                          String[] expansionFields,
	                          String[] boostingFields,
	                          Map boostValues,
	                          VirtualSearchFieldMapper mappedFields,
	                          QueryParser.Operator defaultOperator) throws ParseException {

		FieldExpansionQueryParser qp = new FieldExpansionQueryParser(
			analyzer,
			expansionFields,
			boostingFields,
			boostValues,
			mappedFields);
		qp.setDefaultOperator(defaultOperator);
		return qp.parse(query);
	}


	/**
	 *  Parses a query which searches on the fields specified. This expands the query so that multiple fields are
	 *  searched for the default text, and boosting is applied for the given boosting fields.<p>
	 *
	 *  If <i>n</i> fields are specified, this effectively constructs: <pre>
	 * <code>
	 * (field1:query) (field2:query) (field3:query)...(field<i>n</i>:query) </code> </pre>
	 *
	 * @param  query            Query string to parse
	 * @return                  A Query or null if none
	 * @throws  ParseException  if query parsing fails
	 * @throws  TokenMgrError   if query parsing fails
	 */
	public Query parse(String query)
		 throws ParseException {
		if (expansionFields == null || expansionFields.length == 0)
			return null;

		BooleanQuery bQuery = new BooleanQuery();
		for (int i = 0; i < expansionFields.length; i++) {
			Query q = parse(query, expansionFields[i]);
			bQuery.add(q, BooleanClause.Occur.SHOULD);
		}

		// Boost records with matching terms in the given boosting fields (title, etc.)
		if (terms.size() > 0 && boostingFields != null && boostingFields.length > 0) {

			BooleanQuery fullQuery = new BooleanQuery();
			fullQuery.add(bQuery, BooleanClause.Occur.MUST);

			BooleanQuery boosterQuery = new BooleanQuery();
			boosterQuery.add(super.getFieldQuery("allrecords", "true"), BooleanClause.Occur.SHOULD);
			Query tempQuery = null;
			Float boostValue = null;
			for (int i = 0; i < terms.size(); i++) {
				for (int j = 0; j < boostingFields.length; j++) {
					tempQuery = super.getFieldQuery(boostingFields[j], (String) terms.get(i));
					if (tempQuery != null) {
						if (boostValues != null) {
							boostValue = (Float) boostValues.get(boostingFields[j]);
							if (boostValue != null)
								tempQuery.setBoost(boostValue.floatValue());
						}
						boosterQuery.add(tempQuery, BooleanClause.Occur.SHOULD);
					}
					boostValue = null;
				}
			}
			fullQuery.add(boosterQuery, BooleanClause.Occur.MUST);
			return fullQuery;
		}

		return bQuery;
	}


	/**
	 *  Parses a query string, returning a Query. Same as the QueryParser parse method but adds the ability to
	 *  specify the default field rather than using the one initialized in the constructor.
	 *
	 * @param  query            the query string to be parsed.
	 * @param  field            the default field for this query.
	 * @return                  A Query Object.
	 * @throws  ParseException  if the parsing fails
	 */
	public Query parse(String query, String field) throws ParseException {
		ReInit(new FastCharStream(new StringReader(query)));
		try {
			return Query(field);
		} catch (TokenMgrError tme) {
			throw new ParseException(tme.getMessage());
		} catch (BooleanQuery.TooManyClauses tmc) {
			throw new ParseException("Too many boolean clauses");
		}
	}


	/**
	 *  Gets the fieldQuery attribute of the FieldExpansionQueryParser object
	 *
	 * @param  field               The field being processed
	 * @param  queryText           The text in the field
	 * @return                     The fieldQuery value
	 * @exception  ParseException  If error
	 */
	protected Query getFieldQuery(String field, String queryText)
		 throws ParseException {

		// prtln("getFieldQuery( field: '" + field + "', queryText: '" + queryText + "'");

		// If a VirtualSearchFieldMapper is available and has a mapping for this field, use it:
		if (virtualSearchFieldMapper != null) {
			Query q = virtualSearchFieldMapper.getQuery(field, queryText);
			if (q != null)
				return q;
		}

		// Extract and store the terms in the default field once only - used for boosting
		if (field.equals(expansionFields[0])) {
			String[] tmp = queryText.split("\\s+");
			for (int i = 0; i < tmp.length; i++)
				terms.add(tmp[i]);
		}

		// Then perform the normal field query
		return super.getFieldQuery(field, queryText);
	}



	// ---------------------- Debug methods -------------------------------
	/**
	 *  Gets a datestamp of the current time formatted for display with logs and output.
	 *
	 * @return    A datestamp for display purposes.
	 */
	private final static String getDateStamp() {
		return
			new SimpleDateFormat("MMM d, yyyy h:mm:ss a zzz").format(new Date());
	}


	/**
	 *  Output a line of text to error out, with datestamp.
	 *
	 * @param  s  The text that will be output to error out.
	 */
	private final void prtlnErr(String s) {
		System.err.println(getDateStamp() + " FieldQueryParser ERROR: " + s);
	}



	/**
	 *  Output a line of text to standard out, with datestamp, if debug is set to true.
	 *
	 * @param  s  The String that will be output.
	 */
	private final static void prtln(String s) {
		if (debug)
			System.out.println(getDateStamp() + " FieldQueryParser: " + s);
	}


	/**
	 *  Sets the debug attribute of the FieldQueryParser object
	 *
	 * @param  db  The new debug value
	 */
	public static void setDebug(boolean db) {
		debug = db;
	}
}


