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
package org.dlese.dpc.schemedit.ndr.util.integration;

import org.dlese.dpc.schemedit.ndr.util.NCSCollectReader;
import org.dlese.dpc.ndr.apiproxy.*;
import org.dlese.dpc.ndr.NdrUtils;
import org.dlese.dpc.ndr.reader.*;
import org.dlese.dpc.ndr.request.*;
import org.dlese.dpc.xml.Dom4jUtils;
import org.dlese.dpc.util.Files;
import org.dlese.dpc.util.strings.FindAndReplace;
import org.dlese.dpc.index.SimpleLuceneIndex;
import org.dom4j.*;
import java.util.*;
import java.io.File;
import java.net.*;

/**
 *  Reads spreadsheet data (xml file created from spreadsheet) with data
 *  supplied by NSDL but augmented from NCS Collect records, with the purpose of
 *  determining overlaps and gaps between the collection management info in both
 *  models.
 *
 * @author    Jonathan Ostwald
 */
public class CollectionXSLReader {
	private static boolean debug = true;
	/**  NOT YET DOCUMENTED */
	public Map records = null;

	/**  NOT YET DOCUMENTED */
	public static String baseDir = "H:/Documents/NDR/NSDLCollections";
	// private static String baseDir = "/Users/ostwald/Desktop/NCS";
	/**  NOT YET DOCUMENTED */
	public static String data = baseDir + "/NDRCollections.xml";


	/**
	 *  Constructor for the CollectionXSLReader object
	 *
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public CollectionXSLReader() throws Exception {
		this(data);
	}


	/**
	 *  Constructor for the CollectionXSLReader object taking a path to a
	 *  tab-delimited* datafile.<p>
	 *
	 *  records is a map of CollectionXSLRecords keyed by aggregator handle
	 *
	 * @param  xslPath        NOT YET DOCUMENTED
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public CollectionXSLReader(String xslPath) throws Exception {
		File file = new File(xslPath);
		if (!file.exists())
			throw new Exception("file doesn't exist at " + xslPath);
		Document doc = Dom4jUtils.getXmlDocument(file);
		int counter = 0;
		int ii = 0;
		int max = 200;
		this.records = new HashMap();
		for (Iterator i = doc.getRootElement().elementIterator(); i.hasNext(); ) {
			try {
				Element collectionElement = (Element) i.next();
				CollectionXSLRecord rec = new CollectionXSLRecord(collectionElement);
				String aggHandle = rec.get("aggregatorhandle");
				if (this.records.keySet().contains(aggHandle))
					throw new Exception("dup aggHandle: " + aggHandle);
				this.records.put(aggHandle, rec);
			} catch (Throwable t) {}
			if (ii++ >= max)
				break;
		}
		prtln(this.records.size() + " records read");
	}


	/**
	 *  returns a map of Records, keyed by resourceUrl
	 *
	 * @return                The resourceUrlMap value
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	static Map getResourceUrlMap() throws Exception {
		CollectionXSLReader reader = new CollectionXSLReader();
		Map urlMap = new HashMap();
		for (Iterator i = reader.records.values().iterator(); i.hasNext(); ) {
			CollectionXSLRecord record = (CollectionXSLRecord) i.next();
			urlMap.put(record.get("resourceurl"), record);
		}
		return urlMap;
	}


	/**
	 *  Gets the collectionRecord attribute of the CollectionXSLReader class
	 *
	 * @param  field          NOT YET DOCUMENTED
	 * @param  value          NOT YET DOCUMENTED
	 * @return                The collectionRecord value
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public static CollectionXSLRecord getCollectionRecord(String field, String value) throws Exception {
		CollectionXSLReader reader = new CollectionXSLReader();
		for (Iterator i = reader.records.values().iterator(); i.hasNext(); ) {
			CollectionXSLRecord rec = (CollectionXSLRecord) i.next();
			if (rec.get(field).length() > 0) {
				return (rec);
			}
		}
		return null;
	}


	/**
	 *  Gets the ncsCollectionRecords attribute of the CollectionXSLReader class
	 *
	 * @return                The ncsCollectionRecords value
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public static List getNcsCollectionRecords() throws Exception {
		CollectionXSLReader reader = new CollectionXSLReader();
		List ncsRecs = new ArrayList();
		for (Iterator i = reader.records.values().iterator(); i.hasNext(); ) {
			CollectionXSLRecord rec = (CollectionXSLRecord) i.next();
			if (rec.get("ncsrecordid").length() > 0) {
				ncsRecs.add(rec);
			}
		}
		return ncsRecs;
	}


	/**
	 *  Gets the nonNcsCollectionRecords attribute of the CollectionXSLReader class
	 *
	 * @param  dataFile       NOT YET DOCUMENTED
	 * @return                The nonNcsCollectionRecords value
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public static List getNonNcsCollectionRecords(String dataFile) throws Exception {
		// String updated = "NDRCollectionsNCSIDs.xml";
		File file = new File(baseDir, dataFile);
		CollectionXSLReader reader = new CollectionXSLReader(file.toString());
		List ncsRecs = new ArrayList();
		for (Iterator i = reader.records.values().iterator(); i.hasNext(); ) {
			CollectionXSLRecord rec = (CollectionXSLRecord) i.next();
			if (rec.get("ncsrecordid").length() == 0) {
				ncsRecs.add(rec);
			}
		}
		return ncsRecs;
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @return    NOT YET DOCUMENTED
	 */
	public String toString() {
		String s = ("CollectionXSLReader values:\n");
		for (Iterator i = this.records.values().iterator(); i.hasNext(); ) {
			CollectionXSLRecord rec = (CollectionXSLRecord) i.next();
			s += rec.toString();
			if (i.hasNext())
				s += "\n\n";
		}
		return s;
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  args           NOT YET DOCUMENTED
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public static void main(String[] args) throws Exception {
		File propFile = null; // propFile must be assigned!
		NdrUtils.setup (propFile);
		
		// String path = "H:/Documents/NDR/NSDLCollections/NDRCollectionsNCSIDs.xml";

		// List recs = getNonNcsCollectionRecords ("NDRCollectionsNCSDataFiltered.xml");
		// prtln (recs.size() + " non NCS Collect Records found");

		// updateCollectionsByNCSData();
		// pp (matchByTitle());
		// updateByMatchingTitles();
		// matchByNormalizedURL();

		reportUnmatchedNCSRecords();

	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  node  NOT YET DOCUMENTED
	 */
	private static void pp(Node node) {
		prtln(Dom4jUtils.prettyPrint(node));
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  s  NOT YET DOCUMENTED
	 */
	private static void prtln(String s) {
		String prefix = null;
		if (debug) {
			NdrUtils.prtln(s, prefix);
		}
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  urlStr  NOT YET DOCUMENTED
	 * @return         NOT YET DOCUMENTED
	 */
	static String normalizeUrl(String urlStr) {
		prtln("normalizing: " + urlStr);
		URL url = null;
		try {
			url = new URL(urlStr);
		} catch (Exception e) {
			prtln("couldn't form url");
			return urlStr;
		}
		String path = url.getPath();
		String query = url.getQuery();
		/* 		prtln ("path: " + path);
		prtln ("query: " + query); */
		if ((path == null || "".equals(path)) && (query == null || "".equals(query)))
			return urlStr + "/";
		else
			return urlStr;
	}


	/**
	 *  Adds a feature to the Child attribute of the CollectionXSLReader class
	 *
	 * @param  parent  The feature to be added to the Child attribute
	 * @param  tag     The feature to be added to the Child attribute
	 * @param  value   The feature to be added to the Child attribute
	 */
	static void addChild(Element parent, String tag, String value) {
		parent.addElement(tag).setText(value);
	}


	/**
	 *  Find matches in NCS Collect records for titles of NSDL collection records.
	 *  write an XML file with info about the matches
	 *
	 * @return                NOT YET DOCUMENTED
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	static Document matchByTitle() throws Exception {

		boolean writeToFile = true;

		String data = "NDRCollectionsNCSDataFiltered.xml";
		List nonNcsCollections = CollectionXSLReader.getNonNcsCollectionRecords(data);
		prtln(nonNcsCollections.size() + " NON NCS recs found");
		int counter = 0;
		int max = 120;
		Element root = DocumentHelper.createElement("matchByTitle");
		Document doc = DocumentHelper.createDocument(root);
		for (Iterator i = nonNcsCollections.iterator(); i.hasNext(); ) {
			if (counter++ >= max)
				break;

			CollectionXSLRecord rec = (CollectionXSLRecord) i.next();
			String aggHandle = rec.get("aggregatorhandle");
			prtln("\naggregatorHandle: " + aggHandle);
			try {

				NSDLCollectionReader nsdlReader = new NSDLCollectionReader(aggHandle);
				if (nsdlReader == null)
					throw new Exception("NSDLCollectionReader could not be instantiated");

				String title = nsdlReader.getTitle();
				if (title == null)
					throw new Exception("title not found in metadata" + nsdlReader.metadata.getHandle());

				NCSCollectReader ncsReader = null;
				ncsReader = NSDLCollectionUtils.getNCSRecordByTitle(title);

				if (ncsReader != null) {
					/*
					prtln (ncsReader.getRecordID() + "  --> " + nsdlReader.getResourceUrl() +
					" (" + ncsReader.getUrl() + ")");
					*/
					Element match = root.addElement("match");
					addChild(match, "aggregatorhandle", aggHandle);
					addChild(match, "ncsrecordid", ncsReader.getRecordID());
					addChild(match, "ncsrecordurl", ncsReader.getUrl());
					addChild(match, "resourceurl", nsdlReader.getResourceUrl());
				}
			} catch (Exception e) {
				prtln("match by title ERROR (" + aggHandle + "): " + e.getMessage());
				// e.printStackTrace();
			}
		}

		if (writeToFile) {
			File out = new File(baseDir, "matchByTitle.xml");
			Dom4jUtils.writePrettyDocToFile(doc, out);
			prtln("wrote to " + out);
		}

		return doc;
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @return                NOT YET DOCUMENTED
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	static Document matchByNormalizedURL() throws Exception {

		boolean writeToFile = true;
		String path = "unmatchedNCSRecords.xml";
		Document doc = Dom4jUtils.getXmlDocument(new File(baseDir, path));
		List nodes = doc.selectNodes("unmatchedNCSRecords/ncsrecordid");
		List ids = new ArrayList();
		for (Iterator i = nodes.iterator(); i.hasNext(); ) {
			Element e = (Element) i.next();
			ids.add(e.getTextTrim());
		}
		prtln(ids.size() + " unmatched ncs record ids found");
		Map urlMap = null;
		try {
			urlMap = getResourceUrlMap();
		} catch (Exception e) {
			prtln("urlMap error: " + e.getMessage());
			e.printStackTrace();
			return null;
		}
		int counter = 0;
		int max = 120;
		Element root = DocumentHelper.createElement("matchByNormalizedURL");
		doc = DocumentHelper.createDocument(root);
		for (Iterator i = ids.iterator(); i.hasNext(); ) {
			if (counter++ >= max)
				break;
			String id = (String) i.next();
			NCSCollectReader ncsReader = null;
			try {
				ncsReader = NSDLCollectionUtils.getNCSRecord(id);
				if (ncsReader == null)
					throw new Exception("NCS was null");
			} catch (Exception e) {
				prtln("failed to retrieve ncs record for " + id);
				continue;
			}
			String ncsrecordurl = ncsReader.getUrl();
			String normalized = normalizeUrl(ncsrecordurl);
			if (!normalized.equals(ncsrecordurl) && urlMap.containsKey(normalized)) {
				CollectionXSLRecord rec = (CollectionXSLRecord) urlMap.get(normalized);
				Element match = root.addElement("match");
				addChild(match, "aggregatorhandle", rec.get("aggregatorhandle"));
				addChild(match, "ncsrecordid", ncsReader.getRecordID());
				addChild(match, "ncsrecordurl", ncsrecordurl);
				addChild(match, "resourceurl", rec.get("resourceurl"));
			}
		}

		if (writeToFile) {
			File out = new File(baseDir, "matchByNormalizedURL.xml");
			Dom4jUtils.writePrettyDocToFile(doc, out);
		}

		return doc;
	}


	/**
	 *  Converts the matchByTitle xml file into a form that can be used to augment
	 *  the CollectionXML file with NCSCollect Records that were found by matching
	 *  titles. The map returned holds holds the following info keyed by
	 *  aggregatorhandle<oi>
	 *  <li> aggregatorhandle
	 *  <li> ncsrecordid
	 *  <li> ncsrecordurl
	 *  <li> resourceurl
	 *</ol>
	 *
	 *
	 * @return                NOT YET DOCUMENTED
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	static Map matchByTitleMap() throws Exception {
		boolean verbose = true;
		Map matchMap = new HashMap();
		String path = "matchByTitle.xml";
		File file = new File(baseDir, path);
		Document doc = Dom4jUtils.getXmlDocument(file);
		Element root = doc.getRootElement();
		prtln("there are " + root.elements().size() + " matches by title");
		for (Iterator i = root.elementIterator(); i.hasNext(); ) {
			Element e = (Element) i.next();
			Map entryMap = new HashMap();
			String aggregatorhandle = e.element("aggregatorhandle").getTextTrim();
			entryMap.put("aggregatorhandle", aggregatorhandle);
			entryMap.put("ncsrecordid", e.element("ncsrecordid").getTextTrim());
			entryMap.put("ncsrecordurl", e.element("ncsrecordurl").getTextTrim());
			entryMap.put("resourceurl", e.element("resourceurl").getTextTrim());
			matchMap.put(aggregatorhandle, entryMap);
		}

		if (verbose) {
			for (Iterator i = matchMap.keySet().iterator(); i.hasNext(); ) {
				String key = (String) i.next();
				Map entryMap = (Map) matchMap.get(key);
				prtln("");
				for (Iterator ii = entryMap.keySet().iterator(); ii.hasNext(); ) {
					String name = (String) ii.next();
					String value = (String) entryMap.get(name);
					prtln(name + ": " + value);
				}
			}
		}

		return matchMap;
	}


	/**
	 *  For each collection in NDRCollections.xml perform two operations:
	 *  <ol>
	 *    <li> use NDRCollectionReader to find metadatahandle, resourcehandle, and
	 *    resourceurl
	 *    <li> use webservice to find the NCS Collect Record with the same url as
	 *    the resourceUrl.
	 *  </ol>
	 *  Then insert the extra info into the file and save under a different name.
	 *
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	static void updateCollectionsByNCSData() throws Exception {
		String xml = "NDRCollections.xml";
		File file = new File(baseDir, xml);
		if (!file.exists())
			throw new Exception("file doesn't exist at " + xml);
		Document doc = Dom4jUtils.getXmlDocument(file);
		int nodeCount = doc.getRootElement().elements().size();
		int counter = 1;
		int max = 200;
		for (Iterator i = doc.getRootElement().elementIterator(); i.hasNext(); ) {
			counter++;
			Element collection = (Element) i.next();
			String aggHandle = collection.elementTextTrim("aggregatorhandle");
			if (aggHandle == null || aggHandle.length() == 0) {
				continue;
			}
			prtln("aggHandle: " + aggHandle);
			NSDLCollectionReader reader = null;
			try {
				reader = new NSDLCollectionReader(aggHandle);
			} catch (Exception e) {
				prtln("Couldn't obtain NDRCollectionReader for " + aggHandle + ": " + e);
			}

			// Extract info from NSDLCollectionReader
			String mdpHandle = "";
			String resourceHandle = "";
			String resourceUrl = "";
			String title = "";
			// extract info from the record
			try {
				if (reader == null)
					throw new Exception("reader is null");
				mdpHandle = reader.mdp.getHandle();
				resourceHandle = reader.resource.getHandle();
				resourceUrl = reader.getResourceUrl();
				title = reader.getTitle();
			} catch (Exception e) {
				prtln("couldn't get info from reader: " + e.getMessage());
			}

			collection.addElement("metadatahandle").setText(mdpHandle);
			collection.addElement("resourcehandle").setText(resourceHandle);
			collection.addElement("resourceurl").setText(resourceUrl);

			// Extract information from NCSCollection Record
			String ncsrecordid = "";
			String ncsrecordurl = "";

			try {
				URL url = new URL(resourceUrl);
				NCSCollectReader ncs = NSDLCollectionUtils.getNCSRecord(url);
				ncsrecordid = ncs.getRecordID();
				ncsrecordurl = ncs.getUrl();
			} catch (Exception e) {
				prtln("couldn't get NCS Record for " + resourceUrl);
			}
			prtln("id: " + ncsrecordid);
			try {
				collection.addElement("ncsrecordid").setText(ncsrecordid);
				collection.addElement("ncsrecordurl").setText(ncsrecordurl);
			} catch (Exception e) {
				prtln(e.getMessage());
			}

			// pp (collection);
			if (counter >= max)
				break;
			prtln(counter + "/" + nodeCount);
		}
		String updated = "NDRCollectionsNCSData.xml";
		File outfile = new File(baseDir, updated);
		Dom4jUtils.writePrettyDocToFile(doc, outfile);
		prtln("wrote to " + updated);
	}


	/**
	 *  again update the spreadsheet (this time NDRCollectionsNCSIDs.xml) by adding
	 *  the match-by-title information. we also want to add an "ncsresourceurl"
	 *  column.
	 *
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	static void updateByMatchingTitles() throws Exception {
		Map map = matchByTitleMap();
		String data = "NDRCollectionsNCSDataFiltered.xml";
		File file = new File(baseDir, data);
		if (!file.exists())
			throw new Exception("file doesn't exist at " + data);
		Document doc = Dom4jUtils.getXmlDocument(file);
		int nodeCount = doc.getRootElement().elements().size();
		int counter = 1;
		int max = 200;
		for (Iterator i = doc.getRootElement().elementIterator(); i.hasNext(); ) {
			counter++;
			Element collection = (Element) i.next();
			String aggHandle = collection.elementTextTrim("aggregatorhandle");
			if (aggHandle == null || aggHandle.length() == 0) {
				continue;
			}
			prtln("aggHandle: " + aggHandle);

			if (map.containsKey(aggHandle)) {
				Map entryMap = (Map) map.get(aggHandle);
				String ncsrecordid = (String) entryMap.get("ncsrecordid");
				String ncsrecordurl = (String) entryMap.get("ncsrecordurl");
				collection.element("ncsrecordid").setText(ncsrecordid);
				collection.element("ncsrecordurl").setText(ncsrecordurl);
			}

			// pp (collection);
			if (counter >= max)
				break;
			prtln(counter + "/" + nodeCount);
		}
		String updated = "NDRCollectionsNCSTitle.xml";
		File outfile = new File(baseDir, updated);
		Dom4jUtils.writePrettyDocToFile(doc, outfile);
		prtln("wrote to " + updated);
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	static void reportUnmatchedNCSRecords() throws Exception {
		String idfilename = "unmatchedNCSRecordIDs.xml";
		File idfile = new File(baseDir, idfilename);
		// set up report doc
		Element root = DocumentHelper.createElement("unmatchedNCSRecords");
		Document report = DocumentHelper.createDocument(root);
		// get a list of ids
		Document doc = Dom4jUtils.getXmlDocument(idfile);
		List ids = new ArrayList();
		List nodes = doc.selectNodes("/unmatchedNCSRecords/ncsrecordid");
		for (Iterator i = nodes.iterator(); i.hasNext(); ) {
			Element e = (Element) i.next();
			String id = e.getTextTrim();
			NCSCollectReader reader = NSDLCollectionUtils.getNCSRecord(id);
			Element ncsrec = root.addElement("NCSRecord");
			ncsrec.addElement("id").setText(reader.getRecordID());
			ncsrec.addElement("url").setText(reader.getUrl());
			ncsrec.addElement("title").setText(reader.getTitle());
		}
		String outfilename = "unmatchedNCSRecords.xml";
		File outfile = new File(baseDir, outfilename);
		Dom4jUtils.writeDocToFile(report, outfile);
		prtln("wrote to " + outfile);

	}
}

