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
package org.dlese.dpc.standards.asn;

import org.dlese.dpc.xml.Dom4jUtils;
import org.dlese.dpc.util.Files;
import org.dom4j.*;
import java.net.URL;
import java.util.*;
import java.io.*;

/**
 *  Class to read the ASN authors document from ASN web service and provide
 *  author lookup by author purl. In event ASN webservice is not available, used
 *  cached version.
 *
 * @author    Jonathan Ostwald
 */
public class AsnAuthors {
	private static boolean debug = true;

	final static String CACHED_ASN_AUTHORS = "/org/dlese/dpc/standards/asn/cachedxml/ASNJurisdiction.xml";
	private final static String AUTHOR_DOC_PURL = "http://purl.org/ASN/scheme/ASNJurisdiction/";
	private NameSpaceXMLDocReader doc = null;
	private Map map = null;
	private static AsnAuthors instance = null;


	/**
	 *  Gets the AsnAuthor instance
	 *
	 * @return                The instance value
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public static AsnAuthors getInstance() throws Exception {
		if (instance == null) {
			instance = new AsnAuthors();
		}
		return instance;
	}


	/**
	 *  Constructor for the AsnAuthors object
	 *
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	private AsnAuthors() throws Exception {
		try {
			doc = new NameSpaceXMLDocReader(new URL(AUTHOR_DOC_PURL));
		} catch (Throwable t) {
			prtln("WARNING: unable to process " + AUTHOR_DOC_PURL + ": " + t.getMessage());
			try {
				String cachedAuthors = Files.readFileFromJarClasspath(CACHED_ASN_AUTHORS).toString();
				doc = new NameSpaceXMLDocReader(cachedAuthors);
			} catch (Exception e) {
				throw new Exception("Unable to use cached author document: " + e.getMessage());
			}
		}
		getAuthorMap();
	}


	/**
	 *  Returns a author for a given author purl (e.g., http://purl.org/ASN/scheme/ASNTopic/behavioralStudies)
	 *
	 * @param  purl  NOT YET DOCUMENTED
	 * @return       The author value
	 */
	public String getAuthor(String purl) {
		return (String) map.get(purl);
	}


	/**
	 *  Utility to update cached Authors File
	 *
	 * @param  srcDir         path to local src directory
	 * @exception  Exception  if file cannot be written
	 */
	public static void cacheAuthorsDoc(String srcDir) throws Exception {
		String cacheFilePath = srcDir + CACHED_ASN_AUTHORS;
		File cacheFile = new File(cacheFilePath);
		AsnAuthors authorHelper = new AsnAuthors();
		Dom4jUtils.writePrettyDocToFile(authorHelper.doc.getDocument(), cacheFile);
	}


	/**
	 *  The main program for the AsnAuthors class - runs cacheAuthorsDoc utility.
	 *
	 * @param  args  The command line arguments
	 */
	public static void main(String[] args) {
		String srcDir = "C:/Documents and Settings/ostwald/devel/projects/dlese-tools-project/src";
		try {
			cacheAuthorsDoc(srcDir);
		} catch (Throwable t) {
			prtln("cacheAuthorsDoc error: " + t.getMessage());
		}
	}


	/**
	 *  Returns a mapping from author purl to author label
	 *
	 * @return                The authorMap value
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	private Map getAuthorMap() throws Exception {
		if (map == null) {
			map = new HashMap();
			List conceptNodes = doc.getNodes("rdf:RDF/skos:Concept");
			for (Iterator i = conceptNodes.iterator(); i.hasNext(); ) {
				Element concept = (Element) i.next();
				String about = concept.attributeValue(doc.getQName("rdf:about"));
				Element labelElement = concept.element(doc.getQName("skos:prefLabel"));
				if (labelElement == null) {
					prtln("WARNING: label element not found for " + about);
					continue;
				}
				String label = labelElement.getTextTrim();
				map.put(about, label);
			}
		}
		// Kludge to cover for missing NV
		map.put("http://purl.org/ASN/scheme/ASNJurisdiction/NV", "Nevada");
		return map;
	}


	/**  NOT YET DOCUMENTED */
	public void report() {
		Map authorMap = null;
		try {
			authorMap = getAuthorMap();
		} catch (Throwable t) {
			System.out.println("couldn't get author map: " + t.getMessage());
		}
		Iterator authorIter = authorMap.keySet().iterator();
		while (authorIter.hasNext()) {
			String key = (String) authorIter.next();
			String val = (String) authorMap.get(key);
			System.out.println(key + ": " + val);
		}
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  s  NOT YET DOCUMENTED
	 */
	private static void prtln(String s) {
		if (debug) {
			System.out.println("AsnAuthors: " + s);
		}
	}
}

