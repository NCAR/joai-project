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
package org.dlese.dpc.dds;

import org.dlese.dpc.util.strings.StringUtil;
import org.dlese.dpc.index.*;

/**
 *  Provide highlighting of keywords in any given string of text.
 *
 * @author    ryandear
 */
public final class KeywordsHighlight {

	final static int MAX_KEYWORDS = 50;
	int firstHighlightIndex = 0;
	String[] keywords = new String[MAX_KEYWORDS];
	int numKeywords = 0;
	int maxKeywordLength = 0;
	String HIGHLIGHT_PREFIX = "";
	String HIGHLIGHT_POSTFIX = "</span>";

	/**
	 *  Parse keyword input string and populate keywords array
	 *
	 * @param  words           The new keywords input value
	 * @param  highlightColor
	 */
	public KeywordsHighlight( String words, String highlightColor, String cssClassName ) {
		if(cssClassName != null)
			HIGHLIGHT_PREFIX = "<span class='" + cssClassName + "'>";
		else {
			if(highlightColor == null)
				highlightColor = "#000000";
			HIGHLIGHT_PREFIX = "<span style='font-weight: bold; color: " + highlightColor + "'>";	
		}
		numKeywords = 0;
		words = grabQuotePhrases( words );
		words += " ";
		words = StringUtil.replace( words, "\"", " ", false );
		int ind1 = 0;
		int ind2 = words.indexOf( " " );
		while ( ( ind2 > -1 ) && ( numKeywords < MAX_KEYWORDS ) ) {
			String thisWord = words.substring( ind1, ind2 ).toLowerCase().trim();
			LuceneStopWords stopWords = new LuceneStopWords();
			if ( !stopWords.isStopWord( thisWord ) ) {
				keywords[numKeywords++] = thisWord;
				if ( keywords[numKeywords - 1].length() > maxKeywordLength ) {
					maxKeywordLength = keywords[numKeywords - 1].length();
				}
			}
			ind1 = ind2 + 1;
			ind2 = words.indexOf( " ", ind1 );
		}
	}


	/**
	 *  Find any instances of quoted phrases in a string, add them to the keyword
	 *  list, and then strip them out, returning the string for further processing
	 *  by KeywordsHighlight(). NOTE: Lucene, and therefore this parser, only
	 *  considers " as a quoted phrase delimeter, not '!
	 *
	 * @param  words
	 * @return
	 */
	private String grabQuotePhrases( String words ) {
		if ( words != null ) {
			int ind1 = words.indexOf( "\"" );
			int ind2;
			if ( ( ind1 > -1 ) && ( numKeywords < MAX_KEYWORDS ) ) {
				ind2 = words.indexOf( "\"", ind1 + 1 );
				if ( ind2 > -1 ) {
					keywords[numKeywords++] = words.substring( ind1 + 1, ind2 ).toLowerCase().trim();
					if ( keywords[numKeywords - 1].length() > maxKeywordLength ) {
						maxKeywordLength = keywords[numKeywords - 1].length();
					}
					words = words.substring( 0, ind1 ) + words.substring( ind2 + 1, words.length() );
					words = grabQuotePhrases( words );
				}
			}
			return words;
		}
		return "";
	}


	/**
	 *  Given any string of text, return the text with all keywords highlighted.
	 *
	 * @param  text
	 * @param  addWbr  if true, replace '/' with '/<wbr>' (allows URLs to wrap)
	 * @return         DESCRIPTION
	 */
	public String highlight( String text, boolean addWbr ) {
		if ( addWbr ) {
			text = StringUtil.replace( text, "/", "/<wbr>", false );
		}
		for ( int i = 0; i < numKeywords; i++ ) {
			if ( ( keywords[i].length() > 0 ) &&
				( !keywords[i].equals( "or" ) ) &&
				( !keywords[i].equals( "and" ) ) ) {
				text = replace( text, keywords[i] );
			}
		}
		return text.replaceAll( "<HPREFIX>", HIGHLIGHT_PREFIX )
			.replaceAll( "</HPREFIX>", HIGHLIGHT_POSTFIX );
	}


	/**
	 *  Perform string match and replace
	 *
	 * @param  in    input string
	 * @param  find  string to match against
	 * @return       the newly altered string
	 */
	public String replace( String in,
	                       String find ) {
		StringBuffer ret = new StringBuffer();
		int findLength = find.length();
		int ind1 = 0;
		int ind2;
		in = in + " ";
		String match = in.toLowerCase();
		ind2 = match.indexOf( find );
		firstHighlightIndex = 0;
		while ( ind2 > -1 ) {
			char ch1;
			if ( ind2 > 0 ) {
				ch1 = in.charAt( ind2 - 1 );
			}
			else {
				ch1 = ' ';
			}
			char ch2 = in.charAt( ind2 + findLength );
			if ( !Character.isLetterOrDigit( ch1 ) && !Character.isLetterOrDigit( ch2 ) ) {
				if ( ( firstHighlightIndex == 0 ) || ( ind2 < firstHighlightIndex ) ) {
					firstHighlightIndex = ind2;
				}
				ret.append( in.substring( ind1, ind2 ) )
					.append( "<HPREFIX>" + in.substring( ind2, ind2 + findLength )
					 + "</HPREFIX>" );
			}
			else {
				ret.append( in.substring( ind1, ind2 + findLength ) );
			}
			ind1 = ind2 + findLength;
			ind2 = match.indexOf( find, ind1 + 1 );
		}
		ret.append( in.substring( ind1 ) );
		return ret.toString();
	}


	/**
	 *  Gets the firstHighlightIndex attribute of the KeywordsHighlight
	 *  object--when keyword highlighting is performed, firstHighlightIndex is
	 *  loaded with the index of the first occurrence of a keyword within the given
	 *  string.
	 *
	 * @return    The firstHighlightIndex value
	 */
	public int getFirstHighlightIndex() {
		return firstHighlightIndex + maxKeywordLength + 40;
		// 40 =~ length of HTML strings above (<b><font ...> etc.)
	}
}

