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
package org.dlese.dpc.services.asn;

import org.dlese.dpc.standards.asn.*;
import org.dlese.dpc.xml.XMLFileFilter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.*;
import java.io.File;

/**
 *  Class to read all ASN standards documents in specified directory, and provide access to 
 their contents, e.g., getAsnDocument, getStandard.
 *
 * @author    Jonathan Ostwald
 */
public class AsnServiceHelper {
	private static Log log = LogFactory.getLog(AsnServiceHelper.class);
	private static boolean debug = true;
	private AsnHelper asnHelper = null;
	private Map docMap = null;
	private List docList = null;
	
	public AsnServiceHelper (String standardsPath) throws Exception {
		this.asnHelper = AsnHelper.getInstance();
		load (standardsPath);
	}
	
	private void load (String standardsPath) throws Exception {
		this.docMap = new HashMap ();
		File standardsDir = new File (standardsPath);
		if (!standardsDir.exists())
			throw new Exception ("Standards Directory does not exist at " + standardsPath);
		File [] files = standardsDir.listFiles(new XMLFileFilter());
		for (int i=0;i<files.length;i++) {
			String asnDocPath = files[i].getAbsolutePath();
			try {
				AsnDocument asnDoc = new AsnDocument (files[i]);
				docMap.put (asnDoc.getAuthor(), asnDoc);
				prtln ((i+1) + "/" + files.length + ": processed " + files[i].getName() + "\n\t " + asnDoc.getIdentifiers().size() + " items read");
			} catch (Exception e) {
				prtln ("WARNING: could not process standards document at " + asnDocPath);
			}
		}
	}
		
	public String getTopic (String topicPurl) {
		return this.asnHelper.getTopic(topicPurl);
	}
	
	public String getAuthor (String authorPurl) {
		return this.asnHelper.getAuthor(authorPurl);
	}
	
	public AsnDocument getAsnDocument (String author) {
		return (AsnDocument)docMap.get (author);
	}
	
	public AsnStandard getStandard (String author, String identifier) {
		AsnStandard std = null;
		AsnDocument asnDoc = this.getAsnDocument (author);
		if (asnDoc != null)
			std = asnDoc.getStandard (identifier);
		return std;
	}
	
	public AsnStandard getStandard (String identifier) {
		AsnStandard std = null;
		for (Iterator i=getAsnDocuments().iterator();i.hasNext();) {
			AsnDocument asnDoc = (AsnDocument)i.next();
			std = asnDoc.getStandard(identifier);
			if (std != null)
				break;
		}
		return std;
	}	
	
	public List getAsnDocuments () {
		if (this.docList == null) {
			this.docList = new ArrayList();
			for (Iterator i=this.docMap.values().iterator();i.hasNext();) {
				this.docList.add ((AsnDocument)i.next());
			}
		}
		return this.docList;
	}
	
	public static void main (String [] args) throws Exception {
		prtln ("hello woild");
		String dir = "D:/Documents and Settings/ostwald/devel/projects/asn-service-project/web/WEB-INF/standards_documents";
		AsnServiceHelper helper = new AsnServiceHelper (dir);
		String purl = "http://purl.org/ASN/resources/S100BA3C";
		AsnStandard std = helper.getStandard(purl);
		// prtln (std.toString());
		prtln  ("author: " + std.getAuthor());
		prtln  ("topic: " + std.getTopic());
		String topicPurl = "http://purl.org/ASN/scheme/ASNTopic/science";
		prtln  ("topic2: " + helper.getTopic(topicPurl));
		
	}

	private static void prtln(String s) {
		if (debug) {
			System.out.println("AsnServiceHelper: " + s);
		}
	}
}

