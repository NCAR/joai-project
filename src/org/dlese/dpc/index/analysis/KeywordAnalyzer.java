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
import java.io.*;

/**
 *  An Analyzer that includes all characters in its tokens. Used for searching fields that are indexed using
 *  the {@link org.apache.lucene.document.Field#Keyword(String,String)} method. Note: Deprecated as of Lucene
 *  v1.9 org.apache.lucene.analysis.KeywordAnalyzer is now available.
 *
 * @author     John Weatherley
 */
public final class KeywordAnalyzer extends Analyzer {

	/**
	 *  A TokenStream that includes all characters.
	 *
	 * @param  field   The field
	 * @param  reader  The Reader
	 * @return         A TokenStream that includes all characters
	 */
	public TokenStream tokenStream(String field, final Reader reader) {
		return new KeywordTokenizer(reader);
	}
}


