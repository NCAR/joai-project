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
import org.dlese.dpc.schemedit.standards.StandardsRegistry;
import org.dlese.dpc.standards.asn.AsnDocument;

import java.io.File; 

/**
 * Bean to hold summary information about {@link AsnDocument} for use by the suggestion service.
 *
 * @author    Jonathan Ostwald
 */

public class AsnDocInfo {
	
	private static boolean debug = true;
	
	/**  NOT YET DOCUMENTED */
	public String path;
	/**  NOT YET DOCUMENTED */
	public String author;
	/**  NOT YET DOCUMENTED */
	public String topic;
	/**  NOT YET DOCUMENTED */
	public String fileCreated;
	/**  NOT YET DOCUMENTED */
	public String created;
	/**  NOT YET DOCUMENTED */
	public int numItems;
	/**  NOT YET DOCUMENTED */
	public String filename;
	/**  NOT YET DOCUMENTED */
	public String identifier;
	public String title;
	public String key;
	public String uid;


	/**
	 *  Constructor for the AsnDocInfo object
	 *
	 * @param  asnDoc  the full AsnDocument object to be summarized
	 */
	public AsnDocInfo(AsnDocument asnDoc) {
		this.path = asnDoc.getPath();
		this.identifier = asnDoc.getIdentifier();
		this.author = asnDoc.getAuthor();
		this.topic = asnDoc.getTopic();
		this.fileCreated = asnDoc.getFileCreated();
		this.created = asnDoc.getCreated();
		this.title = asnDoc.getTitle();
		this.numItems = asnDoc.getIdentifiers().size();
		this.filename = new File(path).getName();
		// this.key = StandardsRegistry.makeDocKey (asnDoc);
		this.key = new AsnDocKey (asnDoc).toString();
		this.uid = asnDoc.getUid();
	}

	public String getDocId () {
		return this.identifier;
	}
	
	public String getKey () {
		return this.key;
	}
	
	public String getAuthor () {
		return this.author;
	}
	
	public String getTopic () {
		return this.topic;
	}
	
	public String getTitle () {
		return this.title;
	}
	
	public String getCreated () {
		return this.created;
	}

	public File getSource () {
		return new File (this.path);
	}
	
	public String getUid () {
		return this.uid;
	}
	
	/**  NOT YET DOCUMENTED */
	public void report() {
		prtln("\n" + this.key);
		// prtln("\t author: " + this.author);
		// prtln("\t topic: " + this.topic);
		// prtln("\t created: " + this.created);
		// prtln ("\t fileCreated: " + this.fileCreated);
		// prtln("\t identifier: " + this.identifier);
		// prtln("\t numItems: " + this.numItems);
		prtln("\t filename: " + this.filename);
		//prtln ("\t path: " + this.path);
	}

	public String toString() {
		String s = "\nkey: " + this.getKey();
		s += "\n\t" + "asnId: " + this.identifier;
		s += "\n\t" + "title: " + this.getTitle();
		return s;
	}

	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  s  NOT YET DOCUMENTED
	 */
	private static void prtln(String s) {
		if (debug) {
			SchemEditUtils.prtln(s, "AsnDocInfo");
		}
	}
}

