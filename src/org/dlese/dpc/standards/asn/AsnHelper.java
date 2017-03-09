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

import org.dlese.dpc.xml.XPathUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.*;
import java.io.File;

/**
 *  Utility class to provide acess to resolution services for ASN topics and
 *  authors. Useful because values for authors and topics in ASN standards are
 *  represented as purls, and we often need to resolve them into a
 *  human-relevant form.
 *
 * @author    Jonathan Ostwald
 */
public class AsnHelper {
	private static Log log = LogFactory.getLog(AsnHelper.class);
	private static boolean debug = true;

	private AsnTopics topics = null;
	private AsnAuthors authors = null;
	private static AsnHelper instance = null;
	private boolean strict = true;


	/**
	 *  Constructor for the AsnHelper object
	 *
	 * @param  standardsPath  NOT YET DOCUMENTED
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	private AsnHelper()  {
		try {
			topics = AsnTopics.getInstance();
		} catch (Exception e) {
			prtlnErr ("WARNING: AsnTopics helper could not be instantiated: " + e.getMessage());
		}
		try {
			authors = AsnAuthors.getInstance();
		} catch (Exception e) {
			prtlnErr ("WARNING: AsnAuthors helper could not be instantiated: " + e.getMessage());
		}
		
	}

	public static AsnHelper getInstance () {
		if (instance == null) {
			try {
				instance = new AsnHelper();
			} catch (Throwable t) {
				prtln ("WARNING could not instantiate AsnHelper: " + t.getMessage());
			}
		}
		return instance;
	}

	/**
	 *  Resolves provided authorPurl into human-relevant form
	 *
	 * @param  purl  NOT YET DOCUMENTED
	 * @return       The author value
	 */
	public String getAuthor(String purl) {
		if (authors != null)
			return authors.getAuthor(purl);

		if (strict)
			return null;
		
		return XPathUtils.getLeaf(purl);
	}


	/**
	 *  Resolves provided topicPurl into human-relevant form.<p>
	 *
	 * @param  purl  NOT YET DOCUMENTED
	 * @return       The topic value
	 */
	public String getTopic(String purl) {
		if (topics != null)
			return topics.getTopic(purl);
		
		if (strict)
			return null;
		
		else
			return XPathUtils.getLeaf(purl);
	}


	/**
	 *  The main program for the AsnHelper class
	 *
	 * @param  args           The command line arguments
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public static void main(String[] args) throws Exception {
		AsnHelper mgr = new AsnHelper();
		prtln ("topic: " + mgr.getTopic("http://purl.org/ASN/scheme/ASNTopic/math"));
		prtln ("author: " + mgr.getAuthor("http://purl.org/ASN/scheme/ASNJurisdiction/CO"));
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  s  NOT YET DOCUMENTED
	 */
	private static void prtln(String s) {
		if (debug) {
			System.out.println("AsnHelper: " + s);
		}
	}
	
	private static void prtlnErr(String s) {
		System.out.println("AsnHelper: " + s);
	}
	
}

