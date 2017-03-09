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

import java.io.Reader;
import org.apache.lucene.analysis.*;

/**
 *  Includes all characters as part of the token.
 *
 * @author    John Weatherley
 */
public class KeywordTokenizer extends CharTokenizer {
	/**
	 *  Constructor for the KeywordTokenizer object
	 *
	 * @param  in  The Reader
	 */
	public KeywordTokenizer(Reader in) {
		super(in);
	}


	/**
	 *  Accepts all characters.
	 *
	 * @param  c  The c
	 * @return    true
	 */
	protected boolean isTokenChar(char c) {
		return true;
	}
}

