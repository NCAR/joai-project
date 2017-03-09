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



import org.apache.lucene.document.*;

import org.apache.lucene.index.*;

import org.apache.lucene.index.Term;

import org.apache.lucene.search.*;

import org.apache.lucene.queryParser.ParseException;

import org.apache.lucene.queryParser.QueryParser;

import java.util.*;

import java.text.SimpleDateFormat;



import java.net.URL;



import org.dom4j.Document;

import org.dom4j.Node;

import org.dom4j.Element;

import org.dom4j.Attribute;

import org.dom4j.DocumentException;

import org.dom4j.io.SAXReader;



import org.dlese.dpc.index.reader.*;

import org.dlese.dpc.xml.Dom4jUtils;

import org.dlese.dpc.index.queryParser.XMLQueryParser;



import java.io.*;



/**

 *  Maps virtual search field/term pairs to Lucene Queries. For example, the field/term pair cats:cougar might

 *  be mapped to the Lucene Query (cougar OR puma OR "mountain lion"). To define the mappings, the API can be

 *  given one or more XML files that contain &lt;virtualSearchField&gt; elements that contain XML queries

 *  parsed by the {@link org.dlese.dpc.index.queryParser.XMLQueryParser} class. See <a

 *  href="../../../../javadoc-includes/VirtualSearchFieldMapper-sample.xml"> sample XML file</a> . Mappings

 *  can also be added or removed directly using the API. The API returns Queries for the given field/term

 *  pairs, and a {@link org.dlese.dpc.index.queryParser.FieldExpansionQueryParser} may be used to apply the

 *  field/term mappings when supplied in regular Lucene query strings.

 *

 * @author     John Weatherley

 * @see        org.dlese.dpc.index.queryParser.FieldExpansionQueryParser

 * @see        org.dlese.dpc.index.queryParser.XMLQueryParser

 */

public class VirtualSearchFieldMapper {

	private boolean debug = true;



	private HashMap virtualFields = new HashMap();

	private QueryParser localQueryParser = null;





	/**

	 *  Constructor for the VirtualSearchFieldMapper object

	 *

	 * @param  parser  The QueryParser used to parse Lucene queries that are defined in the configuration file

	 */

	public VirtualSearchFieldMapper(QueryParser parser) {

		localQueryParser = parser;

	}





	/**

	 *  Gets the Lucene Query that is defined for the given virtual field/term pair, or null if none avaialable.

	 *

	 * @param  virtualField  The virtual search field

	 * @param  virtualTerm   The virtual search term

	 * @return               The Lucene Query, or null if none avaialable

	 */

	public Query getQuery(String virtualField, String virtualTerm) {

		if (virtualField == null || virtualTerm == null)

			return null;



		HashMap virtualFieldMap = (HashMap) virtualFields.get(virtualField);

		if (virtualFieldMap == null)

			return null;

		return (Query) virtualFieldMap.get(virtualTerm);

	}





	/**

	 *  Gets the Lucene Query as a String for the given virtual field and term, or null if not avaialable.

	 *

	 * @param  virtualField  The virtual search field

	 * @param  virtualTerm   The virtual search term

	 * @return               The Lucene Query as a String, or null if not avaialable

	 */

	public String getQueryString(String virtualField, String virtualTerm) {

		Query query = getQuery(virtualField, virtualTerm);

		if (query == null)

			return null;

		return query.toString();

	}





	/**

	 *  Gets the number of virtual terms configured for a given virtual field.

	 *

	 * @param  virtualField  The virtual search field

	 * @return               The number of virtual terms configured for this field or zero if the given field

	 *      does not exist

	 */

	public int getNumTermsConfiguredForField(String virtualField) {

		if (virtualField == null)

			return 0;



		HashMap virtualFieldMap = (HashMap) virtualFields.get(virtualField);

		if (virtualFieldMap == null)

			return 0;

		else

			return virtualFieldMap.size();

	}





	/**

	 *  Determines whether the given virtual term is configured for the given field.

	 *

	 * @param  virtualField  The virtual search field

	 * @param  virtualTerm   The virtual search term

	 * @return               True if the given virtual field exists and the given virtual term is configured for

	 *      that field

	 */

	public boolean getIsTermConfiguredForField(String virtualField, String virtualTerm) {

		if (virtualField == null || virtualTerm == null)

			return false;



		HashMap virtualFieldMap = (HashMap) virtualFields.get(virtualField);

		if (virtualFieldMap == null)

			return false;

		else

			return virtualFieldMap.containsKey(virtualTerm);

	}





	/**

	 *  Sets the Query assigned for the given field and term, overwriting any previous definition.

	 *

	 * @param  virtualField  The field

	 * @param  virtualTerm   The term

	 * @param  query         The new Query value for this field/term

	 */

	public void setQuery(String virtualField, String virtualTerm, Query query) {

		if (virtualField == null || virtualTerm == null || query == null)

			return;



		HashMap virtualFieldMap = (HashMap) virtualFields.get(virtualField);

		if (virtualFieldMap == null)

			virtualFieldMap = new HashMap();

		virtualFieldMap.put(virtualTerm, query);

		virtualFields.put(virtualField, virtualFieldMap);

	}





	/**

	 *  Sets the Query assigned for the given field and term, overwriting any previous definition. The query

	 *  String provided must conform to the grammar understood by the {@link

	 *  org.apache.lucene.queryParser.QueryParser} Object that was passed into the constructor.

	 *

	 * @param  virtualField        The field

	 * @param  virtualTerm         The term

	 * @param  query               The new Query value for this field/term

	 * @exception  ParseException  If error parsing the query String

	 */

	public void setQuery(String virtualField, String virtualTerm, String query) throws ParseException {

		if (virtualField == null || virtualTerm == null || query == null || localQueryParser == null)

			return;



		HashMap virtualFieldMap = (HashMap) virtualFields.get(virtualField);

		if (virtualFieldMap == null)

			virtualFieldMap = new HashMap();

		virtualFieldMap.put(virtualTerm, localQueryParser.parse(query));

		virtualFields.put(virtualField, virtualFieldMap);

	}





	/**

	 *  Removes the definition that is defined for the given field and term. After removal, subsequent calls to

	 *  {@link #getQuery} or {@link #getQueryString} for this field/term pair will return null.

	 *

	 * @param  virtualField  The field

	 * @param  virtualTerm   The term

	 * @return               True if a mapping existed for this field/term pair and was removed, otherwise false

	 */

	public boolean remove(String virtualField, String virtualTerm) {

		if (virtualField == null || virtualTerm == null)

			return false;

		HashMap virtualFieldMap = (HashMap) virtualFields.get(virtualField);

		if (virtualFieldMap == null)

			return false;

		Object o = virtualFieldMap.remove(virtualTerm);

		return (o != null);

	}





	/**

	 *  Removes all term definitions that are defined for the given field. After removal, subsequent calls to

	 *  {@link #getQuery} or {@link #getQueryString} will return null for all references to this field.

	 *

	 * @param  virtualField  The field to remove

	 * @return               True if one or more mappings existed for this field and were removed, otherwise

	 *      false

	 */

	public boolean remove(String virtualField) {

		if (virtualField == null)

			return false;



		HashMap hm = (HashMap) virtualFields.remove(virtualField);

		if (hm != null)

			hm.clear();

		return (hm != null);

	}





	/**

	 *  Clears all field/term definitions that are defined. After calling this method, subsequent calls to {@link

	 *  #getQuery} or {@link #getQueryString} will return null for all calls.

	 */

	public void clear() {

		String[] fields = (String[]) virtualFields.keySet().toArray(new String[]{});

		for (int i = 0; i < fields.length; i++) {

			Map termsMap = (Map) virtualFields.get(fields[i]);

			termsMap.clear();

		}

		virtualFields.clear();

		virtualFields = new HashMap();

	}





	/**

	 *  Gets the virtual fields that are defined.

	 *

	 * @return    The virtual fields

	 */

	public String[] getVirtualFields() {

		return (String[]) virtualFields.keySet().toArray(new String[]{});

	}





	/**

	 *  Gets the virtual terms that are defined for the given field, or null.

	 *

	 * @param  field  The field

	 * @return        The virtual terms defined for the given field, or null

	 */

	public String[] getVirtualTerms(String field) {

		if (field == null)

			return null;

		Map termsMap = (Map) virtualFields.get(field);

		if (termsMap == null)

			return null;

		else

			return (String[]) termsMap.keySet().toArray(new String[]{});

	}







	/**

	 *  Adds all virtual field/term query definitions (virtualSearchField elements) contained in the given XML

	 *  configuration String, replacing any previous definitions.

	 *

	 * @param  xmlConfig      The VirtualSearchFieldMapper configuration XML string

	 * @exception  Exception  If error parsing the String

	 */

	public void addVirtualFieldConfiguration(String xmlConfig) throws Exception {

		Document document = Dom4jUtils.getXmlDocument(xmlConfig);

		addAllVirtualSearchFields(document);

	}





	/**

	 *  Adds all virtual field/term query definitions (virtualSearchField elements) contained in the given XML

	 *  configuration file, replacing any previous definitions.

	 *

	 * @param  xmlConfigFile  The VirtualSearchFieldMapper configuration XML file

	 * @exception  Exception  If error parsing or reading the file

	 */

	public void addVirtualFieldConfiguration(File xmlConfigFile) throws Exception {

		addVirtualFieldConfiguration(new URL("file://" + xmlConfigFile.getAbsolutePath()));

	}





	/**

	 *  Adds all virtual field/term query definitions (virtualSearchField elements) contained in the given XML

	 *  configuration file, replacing any previous definitions.

	 *

	 * @param  xmlConfig      The URL to the VirtualSearchFieldMapper configuration XML file

	 * @exception  Exception  If error parsing or reading the url

	 */

	public void addVirtualFieldConfiguration(URL xmlConfig) throws Exception {

		Document document = Dom4jUtils.getXmlDocument(xmlConfig);

		addAllVirtualSearchFields(document);

	}





	/**

	 *  Extracts all virtualSearchField elements from any location in the XML, and adds them to the

	 *  configuration. Note that even if an exception is thrown by this method, some mappings may have been

	 *  successfully added.

	 *

	 * @param  node           The XML document or node to traverse

	 * @return                The number of virtualSearchFields that were found and configured

	 * @exception  Exception  If error processing one or more virtualSearchField elements

	 */

	private int addAllVirtualSearchFields(Node node) throws Exception {

		List virtualSearchFields = node.selectNodes("//virtualSearchField");

		ArrayList exceptions = null;

		int numConfigured = 0;

		for (int i = 0; i < virtualSearchFields.size(); i++) {

			try {

				addVirtualSearchField((Element) virtualSearchFields.get(i));

				numConfigured++;

			} catch (Exception e) {

				if (exceptions == null)

					exceptions = new ArrayList();

				exceptions.add(e);

			}

		}



		if (exceptions != null) {

			String msg = exceptions.size() + " error(s) found, " + numConfigured + " virtualSearchField(s) successfully configured";

			for (int i = 0; i < exceptions.size(); i++) {

				Exception e = (Exception) exceptions.get(i);

				msg += " :: " + e.getMessage();

			}

			throw new Exception(msg);

		}

		return numConfigured;

	}





	private void addVirtualSearchField(Element virtualSearchField) throws Exception {

		String field = virtualSearchField.valueOf("@field");

		if (field == null || field.trim().length() == 0)

			throw new Exception("Error parsing document: element <virtualSearchField> must have a single, non-empty 'field' attribute. Error found at " + virtualSearchField.getPath());



		List virtualSearchTermDefinitions = virtualSearchField.selectNodes("virtualSearchTermDefinition");

		for (int i = 0; i < virtualSearchTermDefinitions.size(); i++)

			addVirtualSearchTermDefinition((Element) virtualSearchTermDefinitions.get(i), field);

	}





	private void addVirtualSearchTermDefinition(Element virtualSearchTermDefinition, String field) throws Exception {

		String term = virtualSearchTermDefinition.valueOf("@term");

		if (term == null || term.trim().length() == 0)

			throw new Exception("Error parsing document: element <virtualSearchTermDefinition> must have a single, non-empty 'term' attribute. Error found at " + virtualSearchTermDefinition.getPath());



		List virtualDefinitionElement = virtualSearchTermDefinition.selectNodes("Query/*");

		if (virtualDefinitionElement == null || virtualDefinitionElement.size() == 0)

			throw new Exception("Error parsing document: <virtualSearchTermDefinition> child element '<Query>' is empty or missing. Error found at " + virtualSearchTermDefinition.getPath());

		if (virtualDefinitionElement.size() > 1)

			throw new Exception("Error parsing document: element <virtualSearchTermDefinition> must contain a single child element within the '<Query>' element but contains more than one. Error found at " + virtualSearchTermDefinition.getPath());



		Query query = XMLQueryParser.getLuceneQuery((Element) virtualDefinitionElement.get(0), localQueryParser);



		setQuery(field, term, query);

	}





	/**

	 *  Outputs a String representation of the fields/terms/queries that are defined in this

	 *  VirtulSearchFieldMapper.

	 *

	 * @return    A String representation of the fields/terms/queries

	 */

	public String toString() {

		String toString = "";

		String[] fields = (String[]) virtualFields.keySet().toArray(new String[]{});

		for (int i = 0; i < fields.length; i++) {

			Map termsMap = (Map) virtualFields.get(fields[i]);

			String[] terms = (String[]) termsMap.keySet().toArray(new String[]{});

			for (int j = 0; j < terms.length; j++) {

				Query q = (Query) termsMap.get(terms[j]);

				toString += fields[i] + ":" + terms[j] + " is mapped to ' " + q.toString() + " '\n";

			}

		}

		return toString;

	}



	//================================================================



	/**

	 *  Return a string for the current time and date, sutiable for display in log files and output to standout:

	 *

	 * @return    The dateStamp value

	 */

	private static String getDateStamp() {

		return

			new SimpleDateFormat("MMM d, yyyy h:mm:ss a zzz").format(new Date());

	}





	/**

	 *  Output a line of text to error out, with datestamp.

	 *

	 * @param  s  The text that will be output to error out.

	 */

	private final void prtlnErr(String s) {

		System.err.println(getDateStamp() + " VirtualSearchFieldMapper Error: " + s);

	}





	/**

	 *  Output a line of text to standard out, with datestamp, if debug is set to true.

	 *

	 * @param  s  The String that will be output.

	 */

	private final void prtln(String s) {

		if (debug)

			System.out.println(getDateStamp() + " VirtualSearchFieldMapper: " + s);

	}





	/**

	 *  Sets the debug attribute of the DocumentService object

	 *

	 * @param  db  The new debug value

	 */

	private final void setDebug(boolean db) {

		debug = db;

	}

}



