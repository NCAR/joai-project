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
package org.dlese.dpc.index.analysis;

import org.apache.lucene.analysis.*;
import org.apache.lucene.analysis.standard.*;
import java.io.Reader;
import java.util.Set;

/**
 *  An Analyzer that uses a WhitespaceTokenizer and a LowerCaseFilter to normalize the
 *  text to lower case.
 *
 * @author    John Weatherley
 * @see       org.apache.lucene.analysis.LowerCaseFilter
 */
public class LowerCaseWhitespaceAnalyzer extends Analyzer {

	/**
	 *  Normalizes the text to lower case.
	 *
	 * @param  fieldName  Name of the field being tokenized
	 * @param  reader     The Reader
	 * @return            The appropriate TokenStream
	 * @see               org.apache.lucene.analysis.LowerCaseFilter
	 */
	public TokenStream tokenStream(String fieldName, Reader reader) {
		TokenStream result = new WhitespaceTokenizer(reader);
		result = new LowerCaseFilter(result);
		return result;
	}
}

