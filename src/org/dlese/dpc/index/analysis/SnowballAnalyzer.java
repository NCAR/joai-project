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

import org.dlese.dpc.index.SimpleLuceneIndex;

/**
 *  A Snowball Analyzer for English word stemming. See <a href="http://tartarus.org/~martin/PorterStemmer/">Porter and
 *  Snowball stemming algorithm</a> documentation. Simply delegates to the Lucene SnowballAnalyzer English
 *  implementation from org.tartarus.snowball.
 *
 * @author    John Weatherley
 */
public final class SnowballAnalyzer extends org.apache.lucene.analysis.snowball.SnowballAnalyzer {
	SnowballAnalyzer() {
		super(SimpleLuceneIndex.getLuceneVersion(),"English");
	}
}


