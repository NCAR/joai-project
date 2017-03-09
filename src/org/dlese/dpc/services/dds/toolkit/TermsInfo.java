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
package org.dlese.dpc.services.dds.toolkit;

import java.util.*;

/**
 *  Class that holds data about the terms in one or more fields in the index. Wraps the data returned from the
 *  ListTerms request to Java Maps and Objects.
 *
 * @author    John Weatherley
 */
public class TermsInfo {
	private List fields = null;
	private Map terms = null;
	private String indexVersion = null;
	private int totalNumTerms = 0;


	/**
	 *  Constructor for the TermsInfo object
	 *
	 * @param  fields         Fields
	 * @param  terms          Terms
	 * @param  indexVersion   Index version
	 * @param  totalNumTerms  Total num terms in given fields
	 */
	public TermsInfo(List fields,
	                 Map terms,
	                 String indexVersion,
	                 int totalNumTerms) {
		this.fields = fields;
		this.terms = terms;
		this.indexVersion = indexVersion;
		this.totalNumTerms = totalNumTerms;
	}


	/**
	 *  Gets a list of the fields in which the terms reside.
	 *
	 * @return    The fields value
	 */
	public List getFields() {
		return fields;
	}


	/**
	 *  Returns a Map of {@link TermData} Objects, keyed by term String.
	 *
	 * @return    The terms value
	 */
	public Map getTerms() {
		return terms;
	}


	/**
	 *  Returns version of the index at the time the request was made.
	 *
	 * @return    The indexVersion value
	 */
	public String getIndexVersion() {
		return indexVersion;
	}


	/**
	 *  Returns the total number of terms found in the given field(s).
	 *
	 * @return    The totalNumTerms value
	 */
	public int getTotalNumTerms() {
		return totalNumTerms;
	}

	/**
	 *  Debugging
	 *
	 * @return    Stringi representation of the TermsInfo object
	 */
	public String toString() {
		return "fields: " + Arrays.toString((String[])fields.toArray(new String[]{})) + ", totalNumTerms: " + totalNumTerms + ", termsMapSize: " + terms.size() + ", indexVersion: " + indexVersion;
	}

}

