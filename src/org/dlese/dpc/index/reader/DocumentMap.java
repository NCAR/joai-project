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
package org.dlese.dpc.index.reader;

import org.apache.lucene.document.*;
import org.dlese.dpc.index.*;
import org.dlese.dpc.repository.*;
import java.util.*;

/**
 *  A {@link java.util.Map} for accessing the data stored in a Lucene {@link
 *  org.apache.lucene.document.Document} as field/value pairs. All data is loaded from the index into the Map
 *  when any Map accessor method is called (e.g. eager loading), making data available even if the underlying
 *  index later changes, but also making access to very large Documents inefficient. Each stored field in the
 *  {@link org.apache.lucene.document.Document} corresponds to a key in the Map. For example
 *  documentMap.get("title") gets the text that was indexed and stored under the field name "title" for the
 *  Document. Supports read operations only. A DocumentMap is available from search results by calling {@link
 *  org.dlese.dpc.index.ResultDoc#getDocMap()}. <p>
 *
 *  Example that uses JSTL inside a JSP page (assumes result is an instance of ResultDoc): <p>
 *
 *  <code>
 *  The title is: ${result.docMap["title"]}.
 *  </code>
 *
 * @author    John Weatherley
 * @see       org.apache.lucene.document.Document
 * @see       org.dlese.dpc.index.ResultDoc
 * @see       LazyDocumentMap
 */
public class DocumentMap implements Map {
	private Document myDoc = null;
	private Map documentMap = null;


	/**
	 *  Constructor for the DocumentMap object.
	 *
	 * @param  doc  The Lucene Document that is exposed by this Map.
	 */
	public DocumentMap(Document doc) {
		myDoc = doc;
	}


	/**  Constructor for the DocumentMap object. */
	public DocumentMap() { }

	// ---------- Supported Map methods --------------

	/**
	 *  Gets the text content of the given Lucene field as a String or null if the given field is not available
	 *  or was not stored in the index. Text contains all stored values for the given field.
	 *
	 * @param  fieldName  A Lucene {@link org.apache.lucene.document.Document} field name
	 * @return            The text of the field as a String, a Date, or null if not available.
	 */
	public Object get(Object fieldName) {
		if (documentMap != null)
			return documentMap.get(fieldName);
		if (myDoc == null || fieldName == null)
			return null;
		if (!(fieldName instanceof String))
			return null;
		String value = "";
		String[] values = myDoc.getValues((String) fieldName);
		if (values != null)
			for (int i = 0; i < values.length; i++)
				value += " " + values[i];
		return value.trim();
	}


	/**
	 *  Gets the field names in the Lucene {@link org.apache.lucene.document.Document}.
	 *
	 * @return    The field names
	 */
	public Set keySet() {
		doPopulateSets();
		return documentMap.keySet();
	}


	/**
	 *  Gets the Set of field/value entries for the Lucene {@link org.apache.lucene.document.Document}. Each
	 *  {@link java.util.Map.Entry} Object in the Set contains the field name (key) and corresponding field
	 *  value. The field value will be an empty String if the given field was not set to be stored in the index.
	 *
	 * @return    The Set of field/value entries for the Lucene Document
	 * @see       java.util.Map.Entry
	 */
	public Set entrySet() {
		doPopulateSets();
		return documentMap.entrySet();
	}


	/**
	 *  Determines whether a given field exists in the Lucene {@link org.apache.lucene.document.Document}.
	 *
	 * @param  fieldName  A field name
	 * @return            True if the field exists in the Lucene {@link org.apache.lucene.document.Document}
	 */
	public boolean containsKey(Object fieldName) {
		doPopulateSets();
		return documentMap.containsKey(fieldName);
	}


	/**
	 *  Gets all field values that are present in the Lucene {@link org.apache.lucene.document.Document}.
	 *
	 * @return    The field values
	 */
	public Collection values() {
		doPopulateSets();
		return documentMap.values();
	}


	/**
	 *  Determines whether the given field value is present in the Lucene {@link
	 *  org.apache.lucene.document.Document}.
	 *
	 * @param  value  A field value
	 * @return        True if the field value is present in this {@link org.apache.lucene.document.Document}
	 */
	public boolean containsValue(Object value) {
		doPopulateSets();
		return documentMap.containsValue(value);
	}


	/**
	 *  Determines whether there are no fields in this {@link org.apache.lucene.document.Document}.
	 *
	 * @return    True if there are no fields in the Document.
	 */
	public boolean isEmpty() {
		doPopulateSets();
		return documentMap.isEmpty();
	}


	/**
	 *  Gets the number of fields in the {@link org.apache.lucene.document.Document}.
	 *
	 * @return    The number of fields in the {@link org.apache.lucene.document.Document}
	 */
	public int size() {
		doPopulateSets();
		if (documentMap == null)
			return 0;
		return documentMap.size();
	}

	// ------- Non-implemented methods ------------

	/**
	 *  Method not supported.
	 *
	 * @param  t  Not supported.
	 */
	public void putAll(Map t) {
		throw new UnsupportedOperationException("putAll operation not supported");
	}


	/**
	 *  Method not supported.
	 *
	 * @param  key    Not supported.
	 * @param  value  Not supported.
	 * @return        Not supported.
	 */
	public Object put(Object key, Object value) {
		throw new UnsupportedOperationException("Put operation not supported");
	}


	/**
	 *  Method not supported.
	 *
	 * @param  key  Not supported.
	 * @return      Not supported.
	 */
	public Object remove(Object key) {
		throw new UnsupportedOperationException("Remove operation not supported");
	}


	/**  Method not supported. */
	public void clear() {
		throw new UnsupportedOperationException("Clear operation not supported");
	}

	// ------- Private helper methods ----------

	private final void doPopulateSets() {
		if (documentMap == null) {
			documentMap = new TreeMap();
			if (myDoc == null)
				return;
			List fields = myDoc.getFields();
			if (fields == null)
				return;
			String key;
			String value;
			for(int j=0; j < fields.size(); j++) {
				Field field = (Field) fields.get(j);
				key = field.name();
				value = "";
				String[] values = myDoc.getValues(key);
				if (values != null)
					for (int i = 0; i < values.length; i++)
						value += " " + values[i];
				documentMap.put(key, value.trim());
			}
		}
	}
}


