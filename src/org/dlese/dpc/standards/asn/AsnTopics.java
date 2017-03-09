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
 *  Class to read the ASN topics document from ASN web service and provide topic
 *  lookup by topic purl.
 *
 * @author    Jonathan Ostwald
 */
public class AsnTopics {
	private static boolean debug = true;

	final static String CASHED_ASN_SUBJECTS = "/org/dlese/dpc/standards/asn/cachedxml/ASNTopic.xml";
	private final static String TOPIC_DOC_PURL = "http://purl.org/ASN/scheme/ASNTopic/";
	private NameSpaceXMLDocReader doc = null;
	private Map map = null;
	private static AsnTopics instance = null;


	/**
	 *  Gets the AsnTopic instance
	 *
	 * @return                The instance value
	 * @exception  Exception  if topicURL cannot be processed
	 */
	public static AsnTopics getInstance() throws Exception {
		if (instance == null) {
			instance = new AsnTopics();
		}
		return instance;
	}


	/**
	 *  Constructor for the AsnTopics object
	 *
	 * @exception  Exception  if topicURL can't be processed
	 */
	private AsnTopics() throws Exception {
		try {
			doc = new NameSpaceXMLDocReader(new URL(TOPIC_DOC_PURL));
		} catch (Throwable t) {
			prtln("WARNING: unable to process " + TOPIC_DOC_PURL + ": " + t.getMessage());
			try {
				String cachedTopicsPath = "/org/dlese/dpc/standards/asn/cachedxml/ASNTopic.xml";
				String cachedTopics = Files.readFileFromJarClasspath(CASHED_ASN_SUBJECTS).toString();
				doc = new NameSpaceXMLDocReader(cachedTopics);
			} catch (Exception e) {
				throw new Exception("Unable to use cached author document: " + e.getMessage());
			}
		}
		getTopicMap();
	}


	/**
	 *  Utility to update cached Topics File
	 *
	 * @param  srcDir         path to local src directory
	 * @exception  Exception  if file cannot be written
	 */
	public static void cacheTopicsDoc(String srcDir) throws Exception {
		String cacheFilePath = srcDir + CASHED_ASN_SUBJECTS;
		File cacheFile = new File(cacheFilePath);
		/* 		if (!cacheFile.canWrite())
			throw new Exception ("Cant write to " + cacheFilePath); */
		AsnTopics topicHelper = new AsnTopics();
		Dom4jUtils.writePrettyDocToFile(topicHelper.doc.getDocument(), cacheFile);
	}


	/**
	 *  The main program for the AsnTopics class - runs cacheTopicsDoc utility.
	 *
	 * @param  args  The command line arguments
	 */
	public static void main(String[] args) {
		String srcDir = "C:/Documents and Settings/ostwald/devel/projects/dlese-tools-project/src";
		try {
			cacheTopicsDoc(srcDir);
		} catch (Throwable t) {
			prtln("cacheTopicsDoc error: " + t.getMessage());
		}
	}


	/**
	 *  Returns a topic for a given topic purl (e.g., http://purl.org/ASN/scheme/ASNTopic/behavioralStudies)
	 *
	 * @param  purl  NOT YET DOCUMENTED
	 * @return       The topic value
	 */
	public String getTopic(String purl) {
		return (String) map.get(purl);
	}


	/**
	 *  Returns a mapping from topic purl to topic label
	 *
	 * @return                The topicMap value
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public Map getTopicMap() throws Exception {
		if (map == null) {
			map = new HashMap();
			List conceptNodes = doc.getNodes("rdf:RDF/skos:Concept");
			for (Iterator i = conceptNodes.iterator(); i.hasNext(); ) {
				Element concept = (Element) i.next();

				// QName qn = DocumentHelper.createQName("about", doc.getNamespace("rdf"));
				String about = concept.attributeValue(doc.getQName("rdf:about"));
				Element labelElement = concept.element(doc.getQName("skos:prefLabel"));
				if (labelElement == null) {
					prtln("WARNING: label element not found");
					continue;
				}
				String label = labelElement.getTextTrim();
				map.put(about, label);
			}
		}
		return map;
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  s  NOT YET DOCUMENTED
	 */
	private static void prtln(String s) {
		if (debug) {
			System.out.println("AsnTopics: " + s);
		}
	}
}

