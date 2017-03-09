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
package org.dlese.dpc.vocab;

import org.dlese.dpc.vocab.*;

/**
 *  NOT YET DOCUMENTED
 *
 * @author     John Weatherley
 * @version    $Id: VocabUtils.java,v 1.3 2009/03/20 23:34:00 jweather Exp $
 */
public class VocabUtils {

	private static boolean debug = true;


	/**
	 *  Gets the vocab encoded keys for the given values, separated by the '+' symbol.
	 *
	 * @param  metaFormat       The metadata format, for example 'adn'
	 * @param  values           The valuse to encode.
	 * @param  useVocabMapping  The mapping to use, for example 'contentStandards'
	 * @param  vocab            The MetadataVocab instance
	 * @return                  The encoded vocab keys.
	 * @exception  Exception    If error.
	 */
	public static String getFieldContent(String metaFormat, String[] values, String useVocabMapping, MetadataVocab vocab)
		 throws Exception {
		if (values == null || values.length == 0) {
			return "";
		}

		StringBuffer ret = new StringBuffer();
		for (int i = 0; i < values.length; i++) {
			String str = values[i].trim();
			if (str.length() > 0) {
				// Use vocabMgr mapping if available, otherwise output unchanged
				if (useVocabMapping != null && vocab != null) {
					ret.append(vocab.getTranslatedValue(metaFormat, useVocabMapping, str));
				}
				else {
					ret.append(str);
				}

				// Separate each term with +
				if (i < (values.length - 1)) {
					ret.append("+");
				}
			}
		}
		//prtln("Field content: " + ret.toString());
		return ret.toString();
	}


	/**
	 *  Gets the encoded vocab key for the given content.
	 *
	 * @param  metaFormat       The metadata format, for example 'adn'
	 * @param  value            The value to encode.
	 * @param  useVocabMapping  The vocab mapping to use, for example "contentStandard".
	 * @param  vocab            The MetadataVocab instance
	 * @return                  The encoded value.
	 * @exception  Exception    If error.
	 */
	public static String getFieldContent(String metaFormat, String value, String useVocabMapping, MetadataVocab vocab)
		 throws Exception {
		if (value == null || value.trim().length() == 0) {
			return "";
		}

		// Use vocabMgr mapping if available, otherwise output unchanged
		if (useVocabMapping != null && vocab != null) {
			return vocab.getTranslatedValue(metaFormat, useVocabMapping, value);
		}
		else {
			return value;
		}
	}


	private static void prtln(String s) {
		System.out.println(s);
	}
}

