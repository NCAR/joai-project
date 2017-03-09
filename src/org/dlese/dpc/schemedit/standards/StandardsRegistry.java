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
package org.dlese.dpc.schemedit.standards;

import org.dlese.dpc.schemedit.standards.asn.*;
import org.dlese.dpc.schemedit.SchemEditUtils;
import org.dlese.dpc.standards.asn.*;
import org.dlese.dpc.xml.XMLFileFilter;
import org.dlese.dpc.util.Files;

import org.dlese.dpc.serviceclients.cat.CATServiceToolkit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.*;
import java.io.File;
import java.io.FileFilter;

/**
 *  Class to manage potentially many ASN standards documents, refered to using
 *  {@link AsnDocKey} instances. <p>
 *
 *  Keys identify a standards Document uniquely, and we load each document,
 *  making no attempt to identify versions that may supercede other versions of
 *  the same standards doc.<p>
 *
 *  The StandardsRegistry contains {@link AsnDocInfo} instances to represent
 *  each loaded Standards Doc. The hierarchical structure of each ASN
 *  Document is represented as an {@link StandardsDocument} instance. The registry
 *  makes use of a {@link TreeCache} to cache StandardTrees and loads them when
 *  needed, so that many standards documents can be managed.
 *
 *@author     Jonathan Ostwald
 *@created    June 25, 2009
 */
public class StandardsRegistry {
	private static Log log = LogFactory.getLog(StandardsRegistry.class);
	private static boolean debug = true;

	// key --> asnDocInfo
	private Map docMap = null;

	private TreeCache treeCache = null;

	// docId --> key
	private Map docIdMap = null;

	private AsnHelper asnHelper = null;
	private static StandardsRegistry instance = null;
	private String lock = "lock";
	private List rejectedDocs = null;

	/**
	 *  Gets the singleton StandardsRegistry instance
	 *
	 *@return    a StandardsRegistry instance
	 */
	public static StandardsRegistry getInstance() {
		if (instance == null) {
			try {
				instance = new StandardsRegistry();
			} catch (Exception e) {
				prtln("could not instantiate StandardsRegistry: " + e.getMessage());
			}
		}
		return instance;
	}


	/**
	 *  Constructor for the StandardsRegistry object
	 *
	 *@exception  Exception  NOT YET DOCUMENTED
	 */
	private StandardsRegistry() throws Exception {
		asnHelper = AsnHelper.getInstance();
		this.docMap = new TreeMap();
		this.treeCache = new TreeCache(this);
		this.docIdMap = new HashMap();
		this.rejectedDocs = new ArrayList();
	}


	/**
	 *  Load all xml documents found by traversing the specified standardsDirectory
	 *  recursively
	 *
	 *@param  standardsDirectory  a directory containing standards Documents
	 *@return                     A list of AsnDocInfo instances representing
	 *      loaded docs
	 *@exception  Exception       NOT YET DOCUMENTED
	 */
	public List load(String standardsDirectory) throws Exception {
		// prtln ("\nloading ..." + standardsDirectory);

		List loaded = new ArrayList();

		if (standardsDirectory == null) {
			return loaded;
		}

		File standardsDir = new File(standardsDirectory);
		if (!standardsDir.exists()) {
			throw new Exception("Standards Directory does not exist at " + standardsDirectory);
		}

		if (standardsDir.isFile()) {
			throw new Exception("configured for Directory but got File (" + standardsDirectory + ") - check config");
		}

		FileFilter xmlFileFilter = new XMLFileFilter();

		File[] files = standardsDir.listFiles();
		int numToRegister = 2000;
		for (int i = 0; i < files.length && i < numToRegister; i++) {
			File file = files[i];
			String filePath = file.getAbsolutePath();
			if (filePath.startsWith(".")) {
				continue;
			}
			if (file.isDirectory()) {
				try {
					loaded.addAll(load(filePath));
				} catch (Exception e) {
					prtln("error attempting to load from " + filePath + ": " + e.getMessage());
				}
				continue;
			}
			if (!xmlFileFilter.accept(file)) {
				continue;
			}

			String asnDocPath = filePath;
			AsnDocInfo docInfo = null;
			try {
				docInfo = this.register(asnDocPath);
				prtln((i + 1) + "/" + files.length + ": processed " + files[i].getName()
						 + " (" + docInfo.getKey() + ")");
			} catch (Exception e) {
				prtln((i + 1) + "/" + files.length + " NOT processed " + files[i].getName());
				prtln("load error: " + e.getMessage());
			}
			if (docInfo != null) {
				loaded.add(docInfo);
			}
		}
		return loaded;
	}


	/**
	 *  Register a Standards Document located at specified path
	 *
	 *@param  path           filepath of xml document to be loaded
	 *@return                DocInfo for loaded doc
	 *@exception  Exception  if unable to register document.
	 */
	public AsnDocInfo register(String path) throws Exception {
		String key = null;
		AsnDocInfo doc = this.getAsnDocByPath(path);
		if (doc != null) {
			return doc;
		}

		try {
			AsnDocument asnDoc = new AsnDocument(new File (path));
			if (asnDoc == null) {
				throw new Exception("AsnDocument could not be created for " + path);
			}
			if (asnDoc.getAuthor() == null) {
				throw new Exception("CAT Service does not support this document: " + asnDoc.getTitle());
			}
			AsnDocInfo docInfo = new AsnDocInfo(asnDoc);
			if (docInfo == null) {
				throw new Exception("AsnDocInfo could not be created for " + path);
			}
			key = docInfo.key;

			/*
			 *  PROBLEM! - this method of identifying potentially older versions
			 *  does not work because the call to "docMap.get (key)" will only find
			 *  existing of the SAME YEAR!
			 */
			AsnDocInfo existing = (AsnDocInfo) docMap.get(docInfo.key);

			if (existing == null) {
				this.put(docInfo, asnDoc);
			}
		} catch (Throwable e) {
			// e.printStackTrace();
			throw new Exception("register ERROR (" + path + "): " + e.getMessage());
		}
		return this.getDocInfo(key);
	}

	/**
	 * Creates a new standards document (tree) and adds it to the treeCache and docIdMap.
	 *
	 *@param  docInfo        NOT YET DOCUMENTED
	 *@param  asnDoc         NOT YET DOCUMENTED
	 *@exception  Exception  NOT YET DOCUMENTED
	 */
	private void put(AsnDocInfo docInfo, AsnDocument asnDoc) throws Exception {
		synchronized (lock) {
			docMap.put(docInfo.key, docInfo);
			AsnStandardsDocument tree = new AsnStandardsDocument(asnDoc);
			this.treeCache.addTree(docInfo.key, tree);
			docIdMap.put(asnDoc.getIdentifier(), docInfo.key);
		}
	}

	/**
	 *  delete standards doc from registry and tree cache
	 *
	 *@param  docInfo  the docInfo for the document to be deleted
	 */
	private void del(AsnDocInfo docInfo) {
		synchronized (lock) {
			docMap.remove(docInfo.key);
			this.treeCache.removeTree(docInfo.key);
			docIdMap.remove(docInfo.identifier);
		}
	}


	/**
	 *  Gets the rejectedDocs attribute of the StandardsRegistry object
	 *
	 *@return    The rejectedDocs value
	 */
	public List getRejectedDocs() {
		return this.rejectedDocs;
	}


	/**
	 *  Create a AsnStandardsDocument by reading the standards file indicated by
	 *  provided key.
	 *
	 *@param  key            NOT YET DOCUMENTED
	 *@return                NOT YET DOCUMENTED
	 *@exception  Exception  NOT YET DOCUMENTED
	 */
	protected AsnStandardsDocument instantiateStandardsDocument(String key) throws Exception {
		// prtln ("instantiateStandardsDocument() key: " + key);
		AsnDocInfo docInfo = this.getDocInfo(key);
		if (docInfo == null) {
			throw new Exception("could not find docInfo for " + key);
		}
		AsnDocument asnDoc = new AsnDocument(new File (docInfo.path));
		return new AsnStandardsDocument(asnDoc);
	}


	/**
	 *  Remove a standards doc from the registry
	 *
	 *@param  key            key designating the doc to unregister
	 *@exception  Exception  NOT YET DOCUMENTED
	 */
	private synchronized void unregister(String key) throws Exception {
		docMap.remove(key);
		// treeMap.remove(key);
		this.treeCache.removeTree(key);
		for (Iterator i = this.docIdMap.keySet().iterator(); i.hasNext(); ) {
			String docId = (String) i.next();
			String docKey = (String) docIdMap.get(docId);
			if (key.equals(docKey)) {
				docIdMap.remove(docId);
				return;
			}
		}
	}


	/**
	 *  Gets the key (e.g., "NSES.Science.1995.D10001D0") corresponding to provided Asn
	 *  Identifier (i.e, purl) for that Standards Doc.
	 *
	 *@param  docId  full ASN Id for a standards Document
	 *@return        the key used by the registry for this document 
	 */
	public String getKey(String docId) {
		return (String) this.docIdMap.get(docId);
	}


	/**
	 *  Gets the keys attribute of the StandardsRegistry object
	 *
	 *@return    The keys value
	 */
	public Set getKeys() {
		return this.docMap.keySet();
	}


	/**
	 *  Gets the asnDocByPath attribute of the StandardsRegistry object
	 *
	 *@param  path  NOT YET DOCUMENTED
	 *@return       The asnDocByPath value
	 */
	public AsnDocInfo getAsnDocByPath(String path) {
		for (Iterator i = this.docMap.values().iterator(); i.hasNext(); ) {
			AsnDocInfo doc = (AsnDocInfo) i.next();
			if (doc.path.equals(path)) {
				return doc;
			}
		}
		return null;
	}


	/**
	 *  Gets the docInfo attribute of the StandardsRegistry object
	 *
	 *@param  key  key of form "author.topic.year"
	 *@return      The docInfo value
	 */
	public AsnDocInfo getDocInfo(String key) {
		return (AsnDocInfo) docMap.get(key);
	}


	/**
	 *  Gets the standardsTree associated with the provided key
	 *
	 *@param  key  NOT YET DOCUMENTED
	 *@return      The standardsTree value
	 */
	public AsnStandardsDocument getStandardsDocument(String key) {
		return this.treeCache.getTree(key);
	}


	/**
	 *  Gets the standardsTree corresponding to the ASN Document having the ASN
	 *  identifier (purl)
	 *
	 *@param  docId  ASN Purl Id
	 *@return        The standardsTree for that id
	 */
	public AsnStandardsDocument getStandardsDocumentForDocId(String docId) {
		String key = (String) this.docIdMap.get(docId);
		if (key == null) {
			return null;
		}
		return getStandardsDocument(key);
	}


	/**
	 *  Gets the standardsNode having the provided AsnID
	 *
	 *@param  asnId  an ASN identifier (purl)
	 *@return        The standardsNode value
	 */
	public AsnStandardsNode getStandardsNode(String asnId) {
		try {
			return this.treeCache.getStandardsNode(asnId);
		} catch (Exception e) {
			// prtln("Standard not found for \"" + asnId + "\": " + e.getMessage());
		}
		return null;
	}

	public AsnStandard getStandard (String asnId) {
		try {
			return this.getStandardsNode(asnId).getAsnStandard();
		} catch (Throwable t) {
			prtln ("could not find node for " + asnId);
		}
		return null;
	}

	/**
	 *  Find a key for a registered standards doc that matches provided key (which may contain wildcards)
	 *
	 *@param  key  key to match againsted registred doc
	 *@return      key of matching registered doc
	 */
	public String matchKey(String key) {
		return matchKey(new DocMatchKey(key));
	}


	/**
	 *  Find a key for a registered standards doc that matches provided DocKeyMatch instance.
	 *
	 *@param  docMatchKey  pattern to match against
	 *@return              matched key
	 */
	public String matchKey(DocMatchKey docMatchKey) {
		for (Iterator i = this.docMap.keySet().iterator(); i.hasNext(); ) {
			String myKey = (String) i.next();
			DocMatchKey myDocMatchKey = new DocMatchKey(myKey);
			if (docMatchKey.matches(myDocMatchKey)) {
				return myKey;
			}
		}
		prtln("no matching key found for \"" + docMatchKey.toString() + "\"");
		return null;
	}


	/**
	 *  Return the AsnDocInfo's for registered standards documents
	 *
	 *@return    The asnDocuments value
	 */
	public List getDocInfos() {
		List docList = new ArrayList();
		for (Iterator i = this.docMap.values().iterator(); i.hasNext(); ) {
			AsnDocInfo doc = (AsnDocInfo) i.next();
			docList.add(doc);
		}
		return docList;
	}

	public void compareWithCatDocs () {
		CATServiceToolkit catToolkit = new CATServiceToolkit();
		Map allCatDocs = null;
		try {
			allCatDocs = catToolkit.getAllCatDocs();
		} catch (Exception e) {
			prtln ("ERROR: " + e.getMessage());
			return;
		}
		prtln (allCatDocs.size() + " cat docs found");
		for (Iterator i=this.getDocInfos().iterator();i.hasNext();) {
			AsnDocInfo docInfo = (AsnDocInfo)i.next();
			String docId = docInfo.getDocId();
			if (!allCatDocs.containsKey(docId)) {
				prtln (docInfo.toString());
			}
		}
	}

	
	/**
	 *  The main program for the StandardsRegistry class
	 *
	 *@param  args           The command line arguments
	 *@exception  Exception  NOT YET DOCUMENTED
	 */
	public static void main(String[] args) throws Exception {
		prtln("\n------------------------------------\n");
		// String dir = "L:/ostwald/tmp/asn/msp2/Math";
		String dir = "C:/tmp/ASN";
		// String dir = "/Users/ostwald/tmp/ASN/subscience/";
		StandardsRegistry reg = StandardsRegistry.getInstance();
		List docs = reg.load(dir);

		reg.report();

		/*
		 *  prtln ("\nRegistry Contents");
		 *  for (Iterator i = docs.iterator(); i.hasNext(); ) {
		 *  AsnDocInfo docInfo = (AsnDocInfo) i.next();
		 *  if (docInfo != null)
		 *  docInfo.report();
		 *  else
		 *  prtln ("docInfo not found!");
		 *  }
		 */
		 String asnId = "http://purl.org/ASN/resources/S1005D1Ex";
		 prtln("\nfinding node for " + asnId);
/* 		 AsnStandardsNode node = reg.getStandardsNode(asnId);
		 if (node == null)
			 prtln("not found");
		 else {
			 prtln("found: " + node.getId());
			 AsnStandard std = node.getAsnStandard ();
			 prtln (std.toString());
		 } */
		 
		 // prtln (reg.getStandard(asnId).toString());
		 reg.compareWithCatDocs();
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 *@param  s  NOT YET DOCUMENTED
	 */
	private static void prtln(String s) {
		if (debug) {
			// System.out.println("StandardsRegistry: " + s);
			SchemEditUtils.prtln(s, "Registry");
		}
	}


	/**
	 *  Sets the debug attribute of the StandardsRegistry class
	 *
	 *@param  bool  The new debug value
	 */
	public static void setDebug(boolean bool) {
		debug = bool;
	}


	/**
	 *  Debugging utility
	 */
	public void report() {
		prtln("StandardsRegistry report");

		Set keys = this.docMap.keySet();
		prtln(keys.size() + " documents registered");

		// Collections.sort (keys);
		for (Iterator i = keys.iterator(); i.hasNext(); ) {
			String key = (String) i.next();
			AsnDocInfo docInfo = this.getDocInfo(key);
			String s = "\nkey: " + docInfo.getKey();
			s += "\n\t" + "asnId: " + docInfo.identifier;
			s += "\n\t" + "title: " + docInfo.getTitle();
			prtln(s);
			// prtln("\t key: " + key + "\n\tasnId: " + docId + "\n\t" + );
		}
		// this.treeCache.report();
	}


	/*
	 *  Represents a DocInfo key used to access the registered documents using wildcarding
	 *  for various key fields.
	 */
	/**
	 *  Description of the Class
	 *
	 *@author     ostwald
	 *@created    June 25, 2009
	 */
	private class DocMatchKey {
		String author = "*";
		String topic = "*";
		String year = "*";


		/**
		 *  Constructor for the DocMatchKey object
		 *
		 *@param  author  the author field
		 *@param  topic   the topic field
		 *@param  year    the year field
		 */
		DocMatchKey(String author, String topic, String year) {
			this.author = valueOrWildcard(author);
			this.topic = valueOrWildcard(topic);
			this.year = valueOrWildcard(year);
		}


		/**
		 *  Constructor for the DocMatchKey object from provided string, using
		 *  wildcards for unspecified fields.
		 *
		 *@param  key  a '.' delimited string
		 */
		DocMatchKey(String key) {
			String[] splits = key.split("\\.");

			try {
				this.author = valueOrWildcard(splits[0]);
				if (splits.length > 1) {
					this.topic = valueOrWildcard(splits[1]);
				}
				if (splits.length > 2) {
					this.year = valueOrWildcard(splits[2]);
				}
				// prtln ("   instantiated: " + this.toString());
			} catch (Throwable t) {
				prtln("DocMatchKey error: " + t.getMessage());
				t.printStackTrace();
			}
		}

		public String toString () {
			return this.author + "." + this.topic + "." + this.year;
		}

		/**
		 *  Returns true if provided DocMatchKey matches this instance, with wildcard
		 *  values always matching (e.g., "*.Science.1995" matches
		 *  "NSES.Science.1995").
		 *
		 *@param  otherKey  NOT YET DOCUMENTED
		 *@return           NOT YET DOCUMENTED
		 */
		boolean matches(DocMatchKey otherKey) {
			// prtln ("matches this: \"" + this.toString() + "\" vs otherKey: \"" + otherKey.toString() + "\"");
			if (!"*".equals(otherKey.author) && !"*".equals(this.author) && !otherKey.author.equals(this.author)) {
				// prtln ("\t authors don't match");
				return false;
			}
			if (!"*".equals(otherKey.topic) && !"*".equals(this.topic) && !otherKey.topic.equals(this.topic)) {
				// prtln ("\t topics don't match");
				return false;
			}
			if (!"*".equals(otherKey.year) && !"*".equals(this.year) && !otherKey.year.equals(this.year)) {
				// prtln ("\t years don't match");
				return false;
			}
			// prtln ("\t => MATCH!");
			return true;
		}


		/**
		 *  NOT YET DOCUMENTED
		 *
		 *@param  s  NOT YET DOCUMENTED
		 *@return    NOT YET DOCUMENTED
		 */
		String valueOrWildcard(String s) {
			String nonValue = "*";
			if (s == null) {
				return nonValue;
			}
			s = s.trim();
			if (s.length() == 0) {
				return nonValue;
			}
			if (s.equals("null")) {
				return nonValue;
			}
			return s;
		}
	}
}

