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
package org.dlese.dpc.schemedit.standards.asn;

import org.dlese.dpc.schemedit.SchemEditUtils;
import org.dlese.dpc.schemedit.Constants;
import org.dlese.dpc.standards.asn.*;
import org.dlese.dpc.xml.XMLFileFilter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.*;

/**
 *  A key representing an ASN StandardDoc that encodes the "author/subject",
 *  "topic/jurisdiction", "created date", and "UID" part of the full ASN purl id
 *  for the document (e.g., "D10001D0").<p>
 *
 *  Used to facilitate matching among keys for similar key components (such as
 *  "topic") using a wildcard, and to provide a human-readible, unique key for
 *  managing documents. The UID segment of the key facilitates mapping to actual
 *  ASN purl when necessary.
 *
 * @author    Jonathan Ostwald
 */
public class AsnDocKey {
	private static Log log = LogFactory.getLog(AsnDocKey.class);
	private static boolean debug = true;
	private String author;
	private String topic;
	private String created;
	private String uid;



	/**
	 *  Constructor for the AsnDocKey object given an AsnDocument
	 *
	 * @param  doc  an asn Standards Docuement
	 */
	public AsnDocKey(AsnDocument doc) {
		this(doc.getAuthor(), doc.getTopic(), doc.getCreated(), doc.getUid());
	}


	/**
	 *  Constructor for the AsnDocKey object given author, topic and created
	 *  segments. The resulting key has a wildcard for the "UID" segment.
	 *
	 * @param  author   the author
	 * @param  topic    the topic
	 * @param  created  the created
	 */
	public AsnDocKey(String author, String topic, String created) {
		this(author, topic, created, "*");
	}

	/**
	 *  Constructor for the AsnDocKey object with provided field data. Converts CAT
	 *  authors to NSES author values to overcome inconsistencies between CAT and ASN naming
	 conventions for authors (aka, jurisdiction).
	 *
	 * @param  author   the author
	 * @param  topic    the topic
	 * @param  created  the created
	 * @param  uid      unique part of asn id (e.g., "D10001D0")
	 */
	public AsnDocKey(String author, String topic, String created, String uid) {
		// for NSES, CAT service requires a different value for author than the ASN doc contains ...
		// map from CAT values to those recognizable to ASN
		if (author.equals("National Science Education Standards (NSES)"))
			this.author = "NSES";
		else if (author.equals("American Association of Advancement of Science Project 2061"))
			this.author = "AAAS";
		else if (author.equals("National Geography Education Standards (NGES)"))
			this.author = "NGES";
		else
			this.author = author;
		this.topic = topic;
		this.created = created;
		this.uid = uid;
	}

	/**
	* Returns a AsnDocKey instance created from provided docKeyAsString value (e.g., "NSES.Science.1995.D10001D0")
	*/
	public static AsnDocKey makeAsnDocKey (String docKeyAsString) {
		prtln ("makeAsnDocKey: " + docKeyAsString);
		String [] splits = docKeyAsString.split("\\.");
		prtln ("  splits has " + splits.length + " items");
		return new AsnDocKey (splits[0], splits[1], splits[2], splits[3]);
	}

	public String getAsnId () {
		return Constants.ASN_PURL_BASE + this.uid;
	}
	
	/**
	 *  return a string representation of this AsnDocKey object
	 *
	 * @return    string representation of key
	 */
	public String toString() {
		return this.author + "." + this.topic + "." + this.created + "." + this.uid;
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  s  NOT YET DOCUMENTED
	 */
	private static void prtln(String s) {
		if (debug) {
			SchemEditUtils.prtln(s, "AsnDocKey");
		}
	}
}

