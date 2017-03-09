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
import java.util.regex.*;
import java.net.*;
import java.io.*;
import org.dlese.dpc.schemedit.*;
import org.dlese.dpc.xml.Dom4jUtils;
import org.dlese.dpc.util.Files;
import org.dlese.dpc.util.Utils;
import org.dlese.dpc.standards.asn.NameSpaceXMLDocReader;
import org.dom4j.*;

/**
 *  Provides functions that use the DDSServicesToolkit to get information about collections, itemRecords,
 *  terms and other data from a DDS repository.
 *
 * @author    ostwald, weatherley
 */
public class RepositoryUtils {

	private static boolean debug = true;

	private File backupsDir;
	private DDSServicesToolkit toolkit;
	private File repository;
	private String ddsInstanceName;
	private List collectionInfos = null;


	/**
	 *  Constructor for the RepositoryUtils object
	 *
	 * @param  baseUrl  DDSWebservice baseUrl (e.g., "http://www.dlese.org/dds/services/ddsws1-1")
	 */

	public RepositoryUtils(String baseUrl) {
		this.toolkit = new DDSServicesToolkit(baseUrl, null);
	}


	/**
	 *  Constructor for the RepositoryUtils object
	 *
	 * @param  ddsServicesToolkit  A DDSServicesToolkit
	 */
	public RepositoryUtils(DDSServicesToolkit ddsServicesToolkit) {
		this.toolkit = ddsServicesToolkit;
	}

	public DDSServicesToolkit getToolKit () {
		return this.toolkit;
	}

	/**
	 *  Gets a mapping of recordIds to itemRecords (as Documents) for the specified collection
	 *
	 * @param  collection     collection key (e.g., "dcc")
	 * @return                The itemRecordMap value
	 * @exception  Exception  if the itemRecordMap cannot be constructed
	 */
	public Map getItemRecordMap(String collection) throws Exception {
		Map map = new HashMap();
		CollectionInfo collectionInfo = getCollectionInfo(collection);
		if (collectionInfo == null)
			throw new Exception("collection not found for \"" + collection + "\"");

		int startOffset = 0;
		int batchSize = 400;

		prtln("\nFetching " + collection + " (" + collectionInfo.formatOfRecords + ")  " + collectionInfo.numRecords + " records");
		while (startOffset < collectionInfo.numRecords) {
			try {
				Map itemRecordBatch = getItemRecordMap(collectionInfo, startOffset, batchSize);
				map.putAll(itemRecordBatch);
				startOffset = startOffset + itemRecordBatch.size();
				if (itemRecordBatch.size() == 0)
					throw new Exception ("failed to make progress");
			} catch (Exception e) {
				e.printStackTrace();
				throw new Exception("failed on batch starting with " + startOffset + ": " + e.getMessage());
			}
		}
		prtln(map.size() + " itemRecords found");
		return map;
	}


	/**
	 *  Returns an itemRecordMap for the specified collection, startOffset and batchSize<p>
	 NOTE: to search over all records, 
	 *
	 * @param  collectionInfo  NOT YET DOCUMENTED
	 * @param  startOffset     NOT YET DOCUMENTED
	 * @param  numReturns      NOT YET DOCUMENTED
	 * @return                 The itemRecordMap value
	 * @exception  Exception   NOT YET DOCUMENTED
	 */
	public Map getItemRecordMap(CollectionInfo collectionInfo, int startOffset, int numReturns) throws Exception {

		String collection = collectionInfo.getCollectionKey();
		String query = "collection:0" + collection;
		String formatOfRecords = null;
		String sortByField = null;
		int sortOrder = -1;
		String showRelation = null;
		Map additionalRequestParams = null;
		boolean soAllRecords = true; // necessary to get collection records, and info from "disabled" collections
		boolean localizeXml = false; // necessary to get valid item records

		Map batchMap = new HashMap();

		prtln("\t" + collection + " starting with " + startOffset);

		DDSServicesResponse response = null;
		try {
			response =
				toolkit.search(query, formatOfRecords, startOffset, numReturns, sortByField,
				sortOrder, showRelation, additionalRequestParams, soAllRecords, localizeXml);
			// pp (response.getResponseDocument());
			
			String[] error = this.checkForErrorResponse(response.getResponseDocument());
			if (error != null)
				throw new Exception (error[0] + ": " + error[1]);

		} catch (Exception e) {
			String msg = "ERROR from service response:" + e.getMessage();
			throw new Exception(msg);
			// prtln (msg);
			// continue;
		}

		try {
			Document doc = response.getResponseDocument();
			List recordNodes = doc.selectNodes("/*[local-name()='DDSWebService']/*[local-name()='Search']/*[local-name()='results']/*[local-name()='record']");
			prtln(recordNodes.size() + " records read");
			if (recordNodes.size() > 0) {
				for (Iterator i = recordNodes.iterator(); i.hasNext(); ) {
					Element record = (Element) i.next();
					String id = getValueAtPath(record, "*[local-name()='head']/*[local-name()='id']");
					Element metadata = (Element) record.selectSingleNode("*[local-name()='metadata']");
					Element itemRecord = (Element) metadata.elements().get(0);
					batchMap.put(id, DocumentHelper.createDocument(itemRecord.createCopy()));
				}
			}
		} catch (Exception e) {
			throw new Exception("failed on batch starting with " + startOffset + ": " + e.getMessage());
		}
		return batchMap;
	}

	public String[] checkForErrorResponse (Document doc) {
		return this.toolkit.checkForErrorResponseDDSWS(doc);
	}

	/**
	 *  Returns the itemRecords of the specified collection as dom4j.Documents
	 *
	 * @param  collection     collection key ("dcc")
	 * @return                The itemRecords value
	 * @exception  Exception  Description of the Exception
	 */
	public Collection getItemRecords(String collection) throws Exception {
		return this.getItemRecordMap(collection).values();
	}


	/**
	 *  Gets a batch of itemRecords
	 *
	 * @param  collectionInfo  NOT YET DOCUMENTED
	 * @param  startOffset     NOT YET DOCUMENTED
	 * @param  numReturns      NOT YET DOCUMENTED
	 * @return                 The itemRecords value
	 * @exception  Exception   NOT YET DOCUMENTED
	 */
	public Collection getItemRecords(CollectionInfo collectionInfo, int startOffset, int numReturns) throws Exception {
		return this.getItemRecordMap(collectionInfo, startOffset, numReturns).values();
	}


	/**
	 *  Returns a List of collection keys (e.g., "dcc") defined in the repository.
	 *
	 * @return                The collectionKeys value
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public List getCollectionKeys() throws Exception {
		List keys = new ArrayList();
		for (Iterator i = getCollectionInfos().iterator(); i.hasNext(); ) {
			CollectionInfo info = (CollectionInfo) i.next();
			keys.add(info.getCollectionKey());
		}
		return keys;
	}


	/**
	 *  Gets the collectionInfos attribute of the RepositoryUtils object
	 *
	 * @return                A list of  CollectionInfo instances
	 * @exception  Exception  Description of the Exception
	 */
	public List getCollectionInfos() throws Exception {
		if (this.collectionInfos == null) {
			this.collectionInfos = new ArrayList();
			Document responseDoc = null;
			try {
				DDSServicesResponse response = toolkit.listCollections(true);
				responseDoc = response.getResponseDocument();
			} catch (DDSServiceErrorResponseException e) {
				prtln("Response error: " + e.getMessage());
				throw new Exception ("could not get response: " + e.getMessage());
			}
			List collectionNodes = responseDoc.selectNodes("/DDSWebService/ListCollections/collections/collection");
			// prtln(collectionNodes.size() + " collections found");
			for (Iterator i = collectionNodes.iterator(); i.hasNext(); ) {
				Element collectionElement = (Element) i.next();
				try {
					CollectionInfo myCol = new CollectionInfo();
					myCol.searchKey = this.getValueAtPath(collectionElement, "searchKey");
					myCol.vocabEntry = this.getValueAtPath(collectionElement, "vocabEntry");
					myCol.recordId = this.getValueAtPath(collectionElement, "recordId");
					myCol.formatOfRecords = this.getValueAtPath(collectionElement, "additionalMetadata/dlese_collect/formatOfRecords");
					myCol.numRecords = this.getIntAtPath(collectionElement, "additionalMetadata/dlese_collect/numRecords");
					myCol.label = this.getValueAtPath(collectionElement, "renderingGuidelines/label");
					this.collectionInfos.add(myCol);
				} catch (Exception e) {
					prtln("WARNING: could not process collection element: " + e.getMessage());
					pp(collectionElement);
					throw new Exception("kicking out");
				}

			}
		}
		return this.collectionInfos;
	}


	/**
	 *  Gets the collectionInfo instance for the specified collection
	 *
	 * @param  collection     collection key (e.g., "dcc")
	 * @return                The collectionInfo value
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	protected CollectionInfo getCollectionInfo(String collection) throws Exception {
		
		List collectionInfos = this.getCollectionInfos();
		if (collectionInfos == null)
			throw new Exception ("no collection infos found");
		
		for (Iterator i = collectionInfos.iterator(); i.hasNext(); ) {
			CollectionInfo cInfo = (CollectionInfo) i.next();
			if (cInfo.getCollectionKey().equals(collection))
				return cInfo;
		}
		return null;
	}

	// ------------- DOM Utilities -------------

	/**
	 *  Gets the textual value of the node located at relativePath from given baseElement.
	 *
	 * @param  baseElement    Description of the Parameter
	 * @param  relativePath   Description of the Parameter
	 * @return                The valueAtPath value
	 * @exception  Exception  if there is no node at relativePath from baseElement
	 */
	protected String getValueAtPath(Node baseElement, String relativePath) throws Exception {
		Node node = baseElement.selectSingleNode(relativePath);
		if (node == null) {
			throw new Exception("node not found at relative path: " + relativePath);
		}
		String ret = node.getText();
		if (ret == null) {
			prtln("WARNING: text not found for node at relative path: " + relativePath);
			return ret;
		}
		return ret.trim();
	}


	/**
	 *  Gets the integer value of the node located at relativePath from given baseElement.
	 *
	 * @param  baseElement    Description of the Parameter
	 * @param  relativePath   Description of the Parameter
	 * @return                The intAtPath value
	 * @exception  Exception  if there is no node at relativePath from baseElement, or if the value found is not
	 *      an integer
	 */
	protected int getIntAtPath(Node baseElement, String relativePath) throws Exception {
		String str = getValueAtPath(baseElement, relativePath);
		try {
			if (str == null) {
				throw new Exception("Nothing found at relative path: " + relativePath);
			}

			return Integer.parseInt(str);
		} catch (NumberFormatException nfe) {
			throw new Exception("couldnt parse as int for relative path:" + relativePath);
		}
	}


	/**
	 *  Gets the text value of the specified child of a given parent element.
	 *
	 * @param  parent        Element whose child element supplies the text
	 * @param  childTagName  tagName of child element
	 * @return               textual content of named child element
	 */
	protected String subElementValue(Element parent, String childTagName) {
		Element child = parent.element(childTagName);
		if (child == null) {
			prtln("child not found for " + childTagName);
			return "";
		}
		return child.getTextTrim();
	}


	/**
	 *  The main program for the RepositoryUtils class
	 *
	 * @param  args           The command line arguments
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public static void main(String[] args) throws Exception {
		prtln("RepositoryUtils");
		getItemRecordsTester();
	}


	/**
	 *  Gets the itemRecordMapTester attribute of the RepositoryUtils class
	 *
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	static void getItemRecordMapTester() throws Exception {
		String baseUrl = "http://dcs.dls.ucar.edu/schemedit/services/ddsws1-1";
		String collection = "mynasastds";
		RepositoryUtils utils = new RepositoryUtils(baseUrl);
		Map map = utils.getItemRecordMap(collection);
		prtln(map.size() + " items found");
		pp((Document) map.values().iterator().next());
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  n  NOT YET DOCUMENTED
	 */
	private static void pp(Node n) {
		prtln(Dom4jUtils.prettyPrint(n));
	}


	/**
	 *  Description of the Method
	 *
	 * @param  s  Description of the Parameter
	 */
	private static void prtln(String s) {
		if (debug) {
			// System.out.println("RepositoryUtils: " + s);
			System.out.println(s);
		}
	}

	// TESTERS -----------------

	/**
	 *  Gets the collectionKeysTester attribute of the RepositoryUtils class
	 *
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	static void getCollectionKeysTester() throws Exception {
		String baseUrl = "http://dcs.dls.ucar.edu/schemedit/services/ddsws1-1";
		String collection = "mynasastds";
		RepositoryUtils utils = new RepositoryUtils(baseUrl);
		for (Iterator i = utils.getCollectionKeys().iterator(); i.hasNext(); ) {
			prtln((String) i.next());
		}
	}


	/**
	 *  Gets the itemRecordsTester attribute of the RepositoryUtils class
	 *
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	static void getItemRecordsTester() throws Exception {
		String baseUrl = "http://dcs.dls.ucar.edu/schemedit/services/ddsws1-1";
		String collection = "mynasastds";
		RepositoryUtils utils = new RepositoryUtils(baseUrl);

		Collection recs = utils.getItemRecords(collection);
		prtln(recs.size() + " items found");
		pp((Document) recs.iterator().next());
	}


	
	
	// ----------------- Methods for TermsInfo e.g. ListTerms request --------------

	/**
	 *  Get terms data for one or more fields in a DDS repository index. The request accepts one or more fields
	 *  to list the terms data for. Note that this request is much less efficient when more than one field is
	 *  requested.
	 *
	 * @param  fields                                One or more fields to get terms data for
	 * @return                                       The data about the terms in the given field(s)
	 * @exception  DDSServiceErrorResponseException  If service error
	 * @exception  Exception                         If other error
	 */
	public TermsInfo getTermsInfoForFields(String[] fields) throws DDSServiceErrorResponseException, Exception {

		Document responseDoc = null;

		// Throws DDSServiceErrorResponseException if fields is null or empty...
		DDSServicesResponse response = toolkit.listTerms(fields, false);
		responseDoc = response.getResponseDocument();

		String indexVersion = getValueAtPath(responseDoc, "/*[local-name()='DDSWebService']/*[local-name()='ListTerms']/*[local-name()='head']/*[local-name()='indexVersion']");
		int totalNumTerms = getIntAtPath(responseDoc, "/*[local-name()='DDSWebService']/*[local-name()='ListTerms']/*[local-name()='head']/*[local-name()='totalNumTerms']");

		// Get the fields that were processed:
		List fieldsNodes = responseDoc.selectNodes("/*[local-name()='DDSWebService']/*[local-name()='ListTerms']/*[local-name()='head']/*[local-name()='fields']/*[local-name()='field']");
		List fieldsReturned = new ArrayList(fieldsNodes.size());
		for (Iterator i = fieldsNodes.iterator(); i.hasNext(); ) {
			Element field = (Element) i.next();
			fieldsReturned.add(field.getText());
		}

		// Generate a Map for the terms data that were returned:
		Map termsMap = new TreeMap();
		List termNodes = responseDoc.selectNodes("/*[local-name()='DDSWebService']/*[local-name()='ListTerms']/*[local-name()='terms']/*[local-name()='term']");
		for (Iterator i = termNodes.iterator(); i.hasNext(); ) {
			Element term = (Element) i.next();
			int termCount = getIntAtPath(term, "@termCount");
			int docCount = getIntAtPath(term, "@docCount");
			termsMap.put(term.getText(), new TermData(termCount, docCount));
		}
		
		TermsInfo termsInfo = new TermsInfo(fieldsReturned, termsMap, indexVersion, totalNumTerms);
		
		//prtln("getTermsInfoForFields returning: " + termsInfo);
		return termsInfo;
	}

}

