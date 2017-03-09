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

import org.dlese.dpc.index.SimpleLuceneIndex;
import java.io.*;
import java.util.*;

import org.apache.lucene.analysis.*;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.*;

import java.text.SimpleDateFormat;

import org.dlese.dpc.util.*;
import org.dlese.dpc.index.analysis.*;

/**
 *  A QueryParser for extracting terms and phrases from a given query. Note: this QueryParser does not return
 *  a usable Query.
 *
 * @author     John Weatherley
 */
public class ExtractorQueryParser extends QueryParser {
	private static boolean debug = true;
	private List fieldTerms = new ArrayList();
	private String extractField = null;


	/**
	 *  Constructor for the ExtractorQueryParser object
	 *
	 * @param  defaultFieldInQuery  The defult field in the query you are parsing
	 * @param  analyzer             The Analyzer to use when parsing the query
	 * @param  fieldToExtract       The field you wish to extract the terms from
	 */
	public ExtractorQueryParser(String defaultFieldInQuery, Analyzer analyzer, String fieldToExtract) {
		super(SimpleLuceneIndex.getLuceneVersion(),defaultFieldInQuery, analyzer);
		extractField = (fieldToExtract == null ? "" : fieldToExtract);
	}


	/**
	 *  Gets the fieldQuery attribute of the ExtractorQueryParser object
	 *
	 * @param  field               The field being processed
	 * @param  queryText           The text in the field
	 * @return                     The fieldQuery value
	 * @exception  ParseException  If error
	 */
	protected Query getFieldQuery(String field, String queryText)
		 throws ParseException {

		if (field.equals(extractField)) {
			// If > 1 term, "this was a phrase"...
			String[] terms = queryText.split("\\s+");
			for (int i = 0; i < terms.length; i++)
				fieldTerms.add(terms[i]);
		}

		return null;
	}


	/**
	 *  Gets the terms extracted from the given field after calling the parse method. All terms are included,
	 *  whether they were from a phrase or not.
	 *
	 * @return    The extractedTerms or null;
	 */
	public String[] getExtractedTerms() {
		return (String[]) fieldTerms.toArray(new String[]{});
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


