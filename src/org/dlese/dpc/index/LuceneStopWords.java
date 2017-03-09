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

import org.apache.lucene.analysis.StopAnalyzer;
import java.util.HashMap;

/**
 *  This class simply loads a hashmap of Lucene stop words from the array given by
 *  StopAnalyzer. This way a single map lookup can be used instead of iterating over the
 *  array every time.
 *
 * @author    ryandear, John Weatherley
 */
public final class LuceneStopWords {
	private static HashMap stopWords = null;

	/**  Keep a copy of the stop words here in case the Lucene package is not available */
	public final static String[] ENGLISH_STOP_WORDS = {
		"a", "an", "and", "are", "as", "at", "be", "but", "by",
		"for", "if", "in", "into", "is", "it",
		"no", "not", "of", "on", "or", "s", "such",
		"t", "that", "the", "their", "then", "there", "these",
		"they", "this", "to", "was", "will", "with"
		};

	/**  Constructor for the LuceneStopWords object */
	public LuceneStopWords() { }

	static {
		// Initialize the HashMap once, statically...
		if (stopWords == null) {
			stopWords = new HashMap();
			try {
				String [] stopWordsArray = (String[])StopAnalyzer.ENGLISH_STOP_WORDS_SET.toArray();
				for (int i = 0; i < stopWordsArray.length; i++) {
					stopWords.put(stopWordsArray[i], new Boolean(true));
				}
			} catch (Throwable e) {
				stopWords.clear();
				for (int i = 0; i < ENGLISH_STOP_WORDS.length; i++) {
					stopWords.put(ENGLISH_STOP_WORDS[i], new Boolean(true));
				}
			}
		}
	}


	/**
	 *  Indicates whether the given word is a Lucene stop word
	 *
	 * @param  word
	 * @return       The luceneStopWord value
	 */
	public static boolean isStopWord(String word) {
		Boolean test = (Boolean) stopWords.get(word.toLowerCase());
		if (test != null) {
			return true;
		}
		return false;
	}
}

