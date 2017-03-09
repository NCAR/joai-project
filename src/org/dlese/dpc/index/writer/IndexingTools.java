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
package org.dlese.dpc.index.writer;

import java.io.*;
import java.util.*;
import java.text.*;

import org.dom4j.Node;

import org.dlese.dpc.index.*;
import org.dlese.dpc.util.*;

import org.apache.lucene.analysis.*;
import org.apache.lucene.document.*;

/**
 *  Tools to aid in indexing.
 *
 * @author    John Weatherley
 */
public class IndexingTools {

	/**  Default field 'default' */
	public final static String defaultFieldName = "default";
	/**  Stems field 'stems' */
	public final static String stemsFieldName = "stems";
	/**  Admin default field 'admindefault' */
	public final static String adminDefaultFieldName = "admindefault";

	/**
	 *  String used to separate and preserve phrases indexed as text, includes leading and trailing white space.
	 */
	public final static String PHRASE_SEPARATOR = " 4phrase4separator4 ";


	/**
	 *  Indexes the given text into the default and stems fields.
	 *
	 * @param  myDoc    Document to add to
	 * @param  content  Content to add
	 */
	public final static void addToDefaultAndStemsFields(Document myDoc, String content) {
		// See class JavaDoc for details on this field.
		myDoc.add(new Field(defaultFieldName, content, Field.Store.NO, Field.Index.ANALYZED));
		// Note that the Analyzer will handle converting the tokens in this field to their stem form.
		myDoc.add(new Field(stemsFieldName, content, Field.Store.NO, Field.Index.ANALYZED));
	}


	/**
	 *  Indexes the given text into the admin default field.
	 *
	 * @param  myDoc    Document to add to
	 * @param  content  Content to add
	 */
	public final static void addToAdminDefaultField(Document myDoc, String content) {
		myDoc.add(new Field(adminDefaultFieldName, content, Field.Store.NO, Field.Index.ANALYZED));
	}


	/**
	 *  Creates a String separated by the phrase separator term from the text of each of the Element or
	 *  Attributes dom4j Nodes provided. The input list may be null. <p>
	 *
	 *  A call to this method might look like:<br>
	 *  <code>String value = makeIndexPhrasesFromNodes(xmlDoc.selectNodes("/news-oppsRecord/topics/topic"));</code>
	 *
	 * @param  nodes  List of Elements or Attributes
	 * @return        A String or null
	 */
	public final static String makeSeparatePhrasesFromNodes(List nodes) {
		String value = null;
		if (nodes != null && nodes.size() > 0) {
			value = ((Node) nodes.get(0)).getText();
			for (int i = 1; i < nodes.size(); i++)
				value += IndexingTools.PHRASE_SEPARATOR + ((Node) nodes.get(i)).getText();
		}
		return value;
	}


	/**
	 *  Creates a String separated by the phrase separator term from each of the Strings provided. The input list
	 *  may be null. <p>
	 *
	 *
	 *
	 * @param  strings  List of Strings or null
	 * @return          A String or null
	 */
	public final static String makeSeparatePhrasesFromStrings(List strings) {
		String value = null;
		if (strings != null && strings.size() > 0) {
			value = strings.get(0).toString();
			for (int i = 1; i < strings.size(); i++)
				value += IndexingTools.PHRASE_SEPARATOR + strings.get(i);
		}
		return value;
	}


	/**
	 *  Creates a String separated by the phrase separator term from each of the Strings provided. The input list
	 *  may be null. <p>
	 *
	 *
	 *
	 * @param  strings  Array of Strings or null
	 * @return          A String or null
	 */
	public final static String makeSeparatePhrasesFromStrings(String[] strings) {
		String value = null;
		if (strings != null && strings.length > 0) {
			value = strings[0].toString();
			for (int i = 1; i < strings.length; i++)
				value += IndexingTools.PHRASE_SEPARATOR + strings[i];
		}
		return value;
	}


	/**
	 *  Extracts the phrases from a String that was created using the method {@link
	 *  #makeSeparatePhrasesFromNodes(List nodes)} or {@link #makeSeparatePhrasesFromStrings(List strings)}.
	 *
	 * @param  separatedPhrases  String that contains the phrase separator to seperate phrases
	 * @return                   An array of phrase Strings or null if the imput is null
	 */
	public final static String[] extractSeparatePhrasesFromString(String separatedPhrases) {
		if (separatedPhrases == null || separatedPhrases.trim().length() == 0)
			return null;
		return separatedPhrases.split(PHRASE_SEPARATOR);
	}


	/**
	 *  Creates a String separated by spaces from the text of each of the Element or Attributes dom4j Nodes
	 *  provided. The input list may be null.<p>
	 *
	 *  A call to this method might look like:<br>
	 *  <code>String value = makeStringFromNodes(xmlDoc.selectNodes("/news-oppsRecord/topics/topic"));</code>
	 *
	 * @param  nodes  List of dom4j Nodes of Elements or Attributes
	 * @return        A String or null
	 */
	public final static String makeStringFromNodes(List nodes) {
		String value = null;
		if (nodes != null && nodes.size() > 0) {
			value = ((Node) nodes.get(0)).getText();
			for (int i = 1; i < nodes.size(); i++)
				value += " " + ((Node) nodes.get(i)).getText();
		}
		return value;
	}


	/**
	 *  Extracts the words from a String that was created using the method {@link #makeStringFromNodes(List
	 *  nodes)}.
	 *
	 * @param  separatedWords  DESCRIPTION
	 * @return                 An array of word Strings
	 */
	public final static String[] extractStringsFromString(String separatedWords) {
		if (separatedWords == null)
			return null;
		return separatedWords.split(" ");
	}


	/**
	 *  Tokenizes a DLESE ID by replacing the char - with a blank space.
	 *
	 * @param  ID  The ID String
	 * @return     The tokenized ID
	 */
	public final static String tokenizeID(String ID) {
		return ID.replaceAll("-|_|\\.", " ");
	}


	/**
	 *  Tokenizes a URI by replacing the unindexable chars with a blank space.
	 *
	 * @param  uri  A URL or URI
	 * @return      The tokenized URI
	 */
	public final static String tokenizeURI(String uri) {
		return uri.replaceAll("/| |\\?|=|\\.|\\&|:", " ");
	}


	/**
	 *  Same as {org.dlese.dpc.index.SimpleLuceneIndex#encodeToTerm(String)}.
	 *
	 * @param  text  Text
	 * @return       Encoded text
	 */
	public final static String encodeToTerm(String text) {
		return SimpleLuceneIndex.encodeToTerm(text);
	}


	/**
	 *  Same as {org.dlese.dpc.index.SimpleLuceneIndex#encodeToTerm(String,boolean)}.
	 *
	 * @param  text             Text
	 * @param  encodeWildCards  True to encode the '*' wildcard char, false to leave unencoded.
	 * @return                  Encoded text
	 */
	public final static String encodeToTerm(String text, boolean encodeWildCards) {
		return SimpleLuceneIndex.encodeToTerm(text);
	}


	/**
	 *  Extracts all {@link org.apache.lucene.analysis.Token}s from a Lucene query using the given {@link
	 *  org.apache.lucene.analysis.Analyzer}.
	 *
	 * @param  textToParse  The text to analyze with the analyzer
	 * @param  analyzer     The analyzer to use
	 * @param  field        The field this Analyzer should interpret the text as, or null to use 'default'
	 * @return              The Tokens generated by the analyzer
	 */
	public final static Token[] getAnalyzedTokens(String textToParse, String field, Analyzer analyzer) {
		Reader reader = new StringReader(textToParse);
		TokenStream tokenStream = analyzer.tokenStream((field == null ? "default" : field), reader);
		try {
			tokenStream.reset();
		} catch (IOException ioe) {
			return new Token[]{};	
		}
		
		ArrayList tokenList = new ArrayList();
		try {
			do {
				Token token = tokenStream.getAttribute(new Token().getClass());
				if (token != null) {
					tokenList.add(token);
				}
			} while (tokenStream.incrementToken());
			tokenStream.end();
			tokenStream.close();			
		} catch (Throwable t) {}
		return (Token[]) tokenList.toArray(new Token[]{});
	}


	/**
	 *  Extracts all terms in any field from a Lucene query using the given {@link
	 *  org.apache.lucene.analysis.Analyzer}.
	 *
	 * @param  textToParse  The text to analyze with the analyzer
	 * @param  analyzer     The analyzer to use
	 * @param  field        The field this Analyzer should interpret the text as, or null to use 'default'
	 * @return              The terms generated by the analyzer
	 */
	public final static String[] getAnalyzedTerms(String textToParse, String field, Analyzer analyzer) {
		Reader reader = new StringReader(textToParse);
		TokenStream tokenStream = analyzer.tokenStream((field == null ? "default" : field), reader);
		try {
			tokenStream.reset();
		} catch (IOException ioe) {
			return new String[] {};	
		}
		
		ArrayList termList = new ArrayList();
		try {
			do {
				Token token = tokenStream.getAttribute(new Token().getClass());
				if (token != null) {
					termList.add(token.term());
				}
			} while (tokenStream.incrementToken());
			tokenStream.end();
			tokenStream.close();
		} catch (Throwable t) {}
		return (String[]) termList.toArray(new String[]{});
	}


	/**
	 *  Extracts all terms in any field from a Lucene query using the given {@link
	 *  org.apache.lucene.analysis.Analyzer} and places them into a HashMap. The keys in the HashMap correspond
	 *  to the field, the values are Lists of the terms in that field.
	 *
	 * @param  textToParse  The text to analyze with the analyzer
	 * @param  analyzer     The analyzer to use
	 * @param  field        DESCRIPTION
	 * @return              The terms HashMap generated by the analyzer
	 */
	/* public final static HashMap getAnalyzedTermsHashMap(String textToParse, String defaultField, Analyzer analyzer) {
		Reader reader = new StringReader(textToParse);
		TokenStream in = analyzer.tokenStream((defaultField == null ? "default" : defaultField), reader);
		HashMap map = new HashMap();
		ArrayList termList = new ArrayList();
		Token token;
		List list;
		try {
			for (; ; ) {
				token = in.next();
				if (token == null)
					break;
				list = map.get(token.)
				termList.add(token.termText());
			}
		} catch (Throwable t) {}
		return (String[]) termList.toArray(new String[]{});
	} */
	/**
	 *  Creates a StringBuffer to display the tokens created by a given analyzer. Output is of the form: [token1]
	 *  [token2].
	 *
	 * @param  textToParse  The text to analyze with the analyzer
	 * @param  analyzer     The analyzer to use
	 * @param  field        The lucene field name, or null to use default
	 * @return              The analyzerTokenOutput value
	 */
	public final static StringBuffer getAnalyzerOutput(String textToParse, String field, Analyzer analyzer) {
		java.io.Reader reader = new StringReader(textToParse);
		TokenStream tokenStream = analyzer.tokenStream((field == null ? "default" : field), reader);
		StringBuffer buf = new StringBuffer();
		try {
			tokenStream.reset();
		} catch (IOException ioe) {
			return buf;	
		}
		
		try {
			
			do {
				Token token = tokenStream.getAttribute(new Token().getClass());
				if (token != null) {
					buf.append("[" + token.term() + "] ");
				}
			} while (tokenStream.incrementToken());
			tokenStream.end();
			tokenStream.close();			

		} catch (Throwable t) {}
		return buf;
	}

}

