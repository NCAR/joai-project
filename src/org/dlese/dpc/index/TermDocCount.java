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

import java.io.*;
import java.util.*;

import org.apache.lucene.index.*;
import org.apache.lucene.store.*;
import org.apache.lucene.search.*;
import org.apache.lucene.analysis.*;
import org.apache.lucene.analysis.standard.*;
import org.apache.lucene.document.*;
import org.apache.lucene.queryParser.*;
import java.text.*;

import org.dlese.dpc.util.*;


/**
 *  A TermDocCount is returned by {@link SimpleLuceneIndex#getTermAndDocCounts(String[])}
 *  and contains the term count, the total number of documents containing the term and a
 *  list of fields in which the term appears.
 *
 * @author    John Weatherley
 */
public class TermDocCount {
	private int term_count = 0;
	private int doc_count = 0;
	private ArrayList fields = new ArrayList();


	/**
	 *  Adds a feature to the ToTermCount attribute of the TermDocCount object
	 *
	 * @param  add  The feature to be added to the ToTermCount attribute
	 */
	public void addToTermCount(int add) {
		term_count += add;
	}


	/**
	 *  Adds a feature to the ToDocCount attribute of the TermDocCount object
	 *
	 * @param  add  The feature to be added to the ToDocCount attribute
	 */
	public void addToDocCount(int add) {
		doc_count += add;
	}


	/**
	 *  Adds a feature to the Field attribute of the TermDocCount object
	 *
	 * @param  field  The feature to be added to the Field attribute
	 */
	public void addField(String field) {
		fields.add(field);
	}


	/**
	 *  Gets the termCount attribute of the TermDocCount object
	 *
	 * @return    The termCount value
	 */
	public int getTermCount() {
		return term_count;
	}


	/**
	 *  Gets the docCount attribute of the TermDocCount object
	 *
	 * @return    The docCount value
	 */
	public int getDocCount() {
		return doc_count;
	}


	/**
	 *  Gets the fields attribute of the TermDocCount object
	 *
	 * @return    The fields value
	 */
	public ArrayList getFields() {
		return fields;
	}
	
	/**
	 *  Compares two TermDocCount by the term count. Collections.sort() or Arrays.sort() can thus
	 *  be used to sort a list of TermDocCount by Name.
	 *
	 * @param  o                       The TermDocCount to compare
	 * @return                         Returns a negative integer, zero, or a positive
	 *      integer as this object is less than, equal to, or greater than the specified
	 *      object.
	 * @exception  ClassCastException  If the object passed in is not a proper.
	 */
	public int compareTo(Object o)
		 throws ClassCastException {
		TermDocCount other = (TermDocCount) o;
		return (term_count - other.getTermCount());
	}	
}


