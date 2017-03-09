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
 *  org.apache.lucene.document.Document} as field/value pairs. The data is loaded from the index only when
 *  requested via the Map accessor methods (e.g. lazy loading), making access to very large Documents possible
 *  in an efficient manner. Each stored field in the {@link org.apache.lucene.document.Document} corresponds
 *  to a key in the Map. For example documentMap.get("title") gets the text that was indexed and stored under
 *  the field name "title" for the Document. Supports read operations only. A LazyDocumentMap is available
 *  from search results by calling {@link org.dlese.dpc.index.ResultDoc#getDocMap()}. <p>
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
 * @see       DocumentMap
 */
public class LazyDocumentMap implements Map {
	private Document myDoc = null;
	private FieldNameList fieldNameList = null;
	private FieldNameSet fieldNameSet = null;
	private FieldValuesList fieldValuesList = null;
	private FieldValuesSet fieldValuesSet = null;


	/**
	 *  Constructor for the LazyDocumentMap object.
	 *
	 * @param  doc  The Lucene Document that is exposed by this Map.
	 */
	public LazyDocumentMap(Document doc) {
		myDoc = doc;
		//System.out.println(	"fieldNameList.size(): " + getFieldNames().size() + "fieldValuesList.size(): " + getFieldValues().size() +"this.size(): " + this.size() + " fields: " + this.getFieldNames());
	}


	/**  Constructor for the LazyDocumentMap object. */
	public LazyDocumentMap() { }


	// ---------- Extended methods beyond Map inteface --------------

	/**
	 *  Gets the names of all fields in the document as Strings. This is similar to #keySet, but does not load
	 *  all field/values into the Map and is therefore more efficient if only some fields are needed.
	 *
	 * @return    The fieldNames value
	 * @see       #keySet
	 */
	public FieldNameList getFieldNames() {
		if (fieldNameList == null)
			fieldNameList = new FieldNameList(myDoc.getFields());
		return fieldNameList;
	}

	private FieldNameSet getFieldNameSet() {
		if (fieldNameSet == null)
			fieldNameSet = new FieldNameSet(getFieldNames());
		return fieldNameSet;
	}

	/**
	 *  Gets the values for all fields in the document as Map.Entry Objects.
	 *
	 * @return    The fieldNames value
	 */
	public FieldValuesList getFieldValues() {
		if (fieldValuesList == null) {
			fieldValuesList = new FieldValuesList(myDoc, getFieldNames());
		}
		return fieldValuesList;
	}

	/**
	 *  Gets the values Set for all fields in the document as Map.Entry Objects.
	 *
	 * @return    The fieldNames Set value
	 */
	private Set getFieldValuesSet() {
		if (fieldValuesSet == null) {
			fieldValuesSet = new FieldValuesSet(getFieldValues());
		}
		return fieldValuesSet;
	}

	/**
	 *  Returns the string value of the field with the given name if any exist in this document, or null. If
	 *  multiple fields exist with this name, this method returns the first value added. If only binary fields
	 *  with this name exist, returns null.
	 *
	 * @param  fieldName  Name of the field
	 * @return            The text value or null if none
	 */
	public String getFirstTextValue(String fieldName) {
		return myDoc.get(fieldName);
	}

	// ---------- Supported Map methods --------------

	/**
	 *  Gets the text content of the given Lucene field as a String or null if the given field is not available
	 *  or was not stored in the index. Text includes all values stored in the index for the given field.
	 *
	 * @param  fieldName  A Lucene {@link org.apache.lucene.document.Document} field name
	 * @return            The text of the field as a String, a Date, or null if not available.
	 */
	public String get(Object fieldName) {
		if (myDoc == null || fieldName == null)
			return null;
		if (!(fieldName instanceof String))
			return null;
		String val = (String) getFieldValues().getAllValues((String) fieldName).getValue();
		return val;
	}


	/**
	 *  Gets the field names in the Lucene {@link org.apache.lucene.document.Document}. This is similar to
	 *  #getFieldNames but it also loads all field/values into the Map and is therfore more expensive than
	 *  #getFieldNames, especially for large Documents.
	 *
	 * @return    The field names
	 * @see       #getFieldNames
	 */
	public Set keySet() {
		return getFieldNameSet();
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
		return getFieldValuesSet();
	}


	/**
	 *  Determines whether a given field exists in the Lucene {@link org.apache.lucene.document.Document}.
	 *
	 * @param  fieldName  A field name
	 * @return            True if the field exists in the Lucene {@link org.apache.lucene.document.Document}
	 */
	public boolean containsKey(Object fieldName) {
		if (!(fieldName instanceof String))
			return false;
		return (myDoc.getFieldable((String) fieldName) != null);
	}


	/**
	 *  Gets all field values that are present in the Lucene {@link org.apache.lucene.document.Document}.
	 *
	 * @return    The field values
	 */
	public Collection values() {
		return (Collection) getFieldValues();
	}


	/**
	 *  Determines whether the given field value is present in the Lucene {@link
	 *  org.apache.lucene.document.Document}.
	 *
	 * @param  value  A field value
	 * @return        True if the field value is present in this {@link org.apache.lucene.document.Document}
	 */
	public boolean containsValue(Object value) {
		return getFieldValues().contains(value);
	}


	/**
	 *  Determines whether there are no fields in this {@link org.apache.lucene.document.Document}.
	 *
	 * @return    True if there are no fields in the Document.
	 */
	public boolean isEmpty() {
		if (myDoc.getFields() == null)
			return true;
		return myDoc.getFields().isEmpty();
	}


	/**
	 *  Gets the number of fields in the {@link org.apache.lucene.document.Document}.
	 *
	 * @return    The number of fields in the {@link org.apache.lucene.document.Document}
	 */
	public int size() {
		if (getFieldNames() == null)
			return 0;
		return getFieldNames().size();
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

	// ------- Helper methods and classes ----------

	/**
	 *  A List for field names in a Lucene Document.
	 *
	 * @author    John Weatherley
	 */
	public class FieldNameList extends AbstractList {
		List _fieldList = null;
		int cursor = 0;


		/**
		 *  Constructor for the FieldNameList object
		 *
		 * @param  fieldList  A List of Fields
		 */
		public FieldNameList(List fieldList) {
			if(fieldList != null) {
				// Ensure there are no duplicate field names:
				_fieldList = new ArrayList(fieldList.size());
				for(int i = 0; i < fieldList.size(); i++) {
					String fieldName = ((Field)fieldList.get(i)).name();
					if(!_fieldList.contains(fieldName))
						_fieldList.add(fieldName);
				}
				Collections.sort(_fieldList);
			}
		}

		protected List getFieldList() {
			return _fieldList;
		}

		public String toString() {
			if(_fieldList == null)
				return "";
			String val = "";
			for(int i = 0; i < _fieldList.size(); i ++ ){
				val += (i == 0 ? "{" : " {") + _fieldList.get(i) + "}";
			}
			return val;
		}

		/**
		 *  Gets a field name String at the given position
		 *
		 * @param  i  Postion
		 * @return    Field name String
		 */
		public Object get(int i) {
			if (_fieldList == null)
				return null;
			if (_fieldList.get(i) == null)
				return null;
			return ((Field) _fieldList.get(i)).name();
		}


		/**
		 *  Number of Fields in the Document
		 *
		 * @return    Number of Fields
		 */
		public int size() {
			if (_fieldList == null)
				return 0;
			return _fieldList.size();
		}
	}



	/**
	 *  A Set for field names in a Lucene Document.
	 *
	 * @author    John Weatherley
	 */
	public class FieldNameSet extends AbstractSet {
		FieldNameList _fieldNameList = null;

		public Iterator iterator(){
			return _fieldNameList.iterator();
		}

		public int size(){
			return _fieldNameList.size();
		}

		public FieldNameSet(FieldNameList fieldNameList) {
			FieldNameList _fieldNameList = fieldNameList;
		}


	}


	/**
	 *  A List for field values in a Lucene Document.
	 *
	 * @author    John Weatherley
	 */
	public class FieldValuesList extends AbstractList {
		List _fieldListForVals = null;
		Document _myDoc = null;
		Map _valuesMap = null;
		int cursor = 0;

		/**
		 *  Constructor for the FieldValuesList object
		 *
		 * @param  myDoc  Lucene Document
		 */
		public FieldValuesList(Document myDoc, FieldNameList fieldNameList) {
			_fieldListForVals = fieldNameList.getFieldList();
			_myDoc = myDoc;
			if (_fieldListForVals == null)
				_valuesMap = new HashMap();
			else
				_valuesMap = new HashMap(_fieldListForVals.size());
		}

		public FieldValuesList() {}

		/**
		 *  Gets a field values String at the given position
		 *
		 * @param  i  Postion
		 * @return    Field name String
		 */
		public Object get(int i) {
			if (_fieldListForVals == null)
				return null;
			if (_fieldListForVals.get(i) == null)
				return null;
			String fieldName = (String)_fieldListForVals.get(i);
			return getAllValues(fieldName);
		}


		/**
		 *  Gets a field values String for the given field.
		 *
		 * @param  fieldName  Field name
		 * @return            Field values
		 */
		public Map.Entry getAllValues(String fieldName) {
			if (_fieldListForVals == null || fieldName == null)
				return null;
			if (!_valuesMap.containsKey(fieldName)) {
				String value = "";
				String[] values = _myDoc.getValues(fieldName);
				if (values != null) {
					for (int j = 0; j < values.length; j++) {
						if (j != 0)
							value += " ";
						value += values[j];
					}
				}
				_valuesMap.put(fieldName, value);
			}
			String retVal = (String) _valuesMap.get(fieldName);
			return new FieldMapEntry(fieldName, retVal);
		}


		/**
		 *  Number of field values in the Document
		 *
		 * @return    Number of Fields
		 */
		public int size() {
			if (_fieldListForVals == null)
				return 0;
			return _fieldListForVals.size();
		}
	}

	/**
	 *  A Set for field values in a Lucene Document.
	 *
	 * @author    John Weatherley
	 */
	public class FieldValuesSet extends AbstractSet {

		FieldValuesList _fieldValuesList = null;

		public Iterator iterator(){
			return _fieldValuesList.iterator();
		}

		public int size(){
			return _fieldValuesList.size();
		}

		public FieldValuesSet(FieldValuesList fieldValuesList) {
			_fieldValuesList = fieldValuesList;
		}
	}


	/**
	 *  A Map entry containing lucene fieldName/fieldValues
	 *
	 * @author    John Weatherley
	 */
	public class FieldMapEntry implements Map.Entry {
		private String _fieldName = null;
		private String _fieldValue = null;


		/**
		 *  Constructor for the FieldMapEntry object
		 *
		 * @param  fieldName   fieldName
		 * @param  fieldValue  fieldValue
		 */
		public FieldMapEntry(String fieldName, String fieldValue) {
			_fieldName = fieldName;
			_fieldValue = fieldValue;
		}


		/**
		 *  Gets the key attribute of the FieldMapEntry object
		 *
		 * @return    The key value
		 */
		public Object getKey() {
			return _fieldName;
		}


		/**
		 *  Gets the value attribute of the FieldMapEntry object
		 *
		 * @return    The value value
		 */
		public Object getValue() {
			return _fieldValue;
		}


		/**
		 *  Throws UnsupportedOperationException
		 *
		 * @param  val  The new value value
		 * @return      Not supported
		 */
		public Object setValue(Object val) {
			throw new UnsupportedOperationException("setValue operation not supported");
		}

	}
}

