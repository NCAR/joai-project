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
package org.dlese.dpc.standards.commcore;

import org.dlese.dpc.schemedit.SchemEditUtils;
import org.dlese.dpc.schemedit.standards.*;
import org.dom4j.*;
import org.dlese.dpc.xml.Dom4jUtils;
import org.dlese.dpc.xml.schema.SchemaHelper;
import org.dlese.dpc.util.Files;
import java.util.*;
import java.io.File;

/**
 *  Encapsulates an ASN Standards Document by reading the source XML file and
 *  creating a hierarchy of Standard instances. Also provides lookup for
 *  Standards by their id.
 *
 * @author    ostwald
 */
public class StdDocument {
	private static boolean debug = true;

	private Map map = null;
	private String path;
	private String identifier = null;
	private String uid = null;
	private String title = null;
	private String fileCreated = null;
	private String author = null;
	private String topic = null;
	private String description = null;
	private String version = null;

	/**
	 *  Constructor for the StdDocument object given the path to an  XML file.
	 *
	 * @param  path           Description of the Parameter
	 * @exception  Exception  Description of the Exception
	 */
	public StdDocument(String path) throws Exception {
		this.path = path;
		map = new HashMap();
		try {
			init();
		} catch (Throwable e) {
			e.printStackTrace();
			throw new Exception("init error: " + e.getMessage());
		}
		// prtln ("StdDocument instantiated");
	}


	/**
	 *  Gets the path attribute of the StdDocument object
	 *
	 * @return    The path value
	 */
	public String getPath() {
		return this.path;
	}


	/**
	 *  Gets the fileCreated attribute of the StdDocument object
	 *
	 * @return    The fileCreated value
	 */
	public String getFileCreated() {
		return this.fileCreated;
	}

	/**
	 *  Gets the identifier attribute of the StdDocument object
	 *
	 * @return    The identifier value
	 */
	public String getIdentifier() {
		return this.identifier;
	}


	/**
	 *  Gets the uid attribute of the StdDocument object
	 *
	 * @return    The uid value
	 */
	public String getUid() {
		return this.uid;
	}


	/**
	 *  Gets the title attribute of the StdDocument object
	 *
	 * @return    The title value
	 */
	public String getTitle() {
		return this.title;
	}


	/**
	 *  Gets the description attribute of the StdDocument object
	 *
	 * @return    The description value
	 */
	public String getDescription() {
		return this.description;
	}


	/**
	 *  Gets the version attribute of the StdDocument object
	 *
	 * @return    The version value
	 */
	public String getVersion() {
		return this.version;
	}


	/**
	 *  Gets the author attribute of the StdDocument object
	 *
	 * @return    The author value
	 */
	public String getAuthor() {
		return this.author;
	}

	/**
	 *  Gets the topic attribute of the StdDocument object
	 *
	 * @return    The topic value
	 */
	public String getTopic() {
		return this.topic;
	}

	/**
	 *  Gets the rootStandard attribute of the StdDocument object
	 *
	 * @return    The rootStandard value
	 */
	public Standard getRootStandard() {
		// why aren't we returning a RootStdNode?
		return (Standard) map.get(this.getIdentifier());
	}


	/**
	 *  Gets the Standard having provicded id
	 *
	 * @param  id  Description of the Parameter
	 * @return     The standard value
	 */
	public Standard getStandard(String id) {
		return (Standard) map.get(id);
	}


	/**
	 *  Gets all standards contained in this StdDocument
	 *
	 * @return    The standards value
	 */
	public Collection getStandards() {
		return map.values();
	}


	/**
	 *  Gets the standards at the specified level of the standards hierarchy of the
	 *  StdDocument object
	 *
	 * @param  level  Description of the Parameter
	 * @return        The standardsAtLevel value
	 */
	public List getStandardsAtLevel(int level) {
		List ret = new ArrayList();
		if (getStandards() != null) {
			for (Iterator i = getStandards().iterator(); i.hasNext(); ) {
				Standard std = (Standard) i.next();
				if (std.getLevel() == level) {
					ret.add(std);
				}
			}
		}
		return ret;
	}

	/**
	 *  Gets the lisf of asn IDs defined by the StdDocument object
	 *
	 * @return    The identifiers value
	 */
	public Set getIdentifiers() {
		return map.keySet();
	}


	/**
	 *  Read XML document and initialize values for the AsnDocumennt object
	 *
	 * @exception  Exception  Description of the Exception
	 */
	private void init() throws Exception {
		Document doc = null;
		try {
			doc = Dom4jUtils.getXmlDocument(new File(path), "UTF-8");
		} catch (Exception e) {
			throw new Exception("Couldn't read standards doc: " + e.getMessage());
		}

		Element stdDocElement = (Element) doc.selectSingleNode("/comm-core/Document");
		if (stdDocElement == null) {
			throw new Exception("StandardDocument element not found");
		}

		identifier = getChildText (stdDocElement, "id");
		uid = identifier;
		fileCreated = getChildText (stdDocElement, "fileCreated");
		title = getChildText (stdDocElement, "title");
		description = getChildText (stdDocElement, "description");
		author = getChildText (stdDocElement, "author");
		topic = getChildText (stdDocElement, "topic");
		version = getChildText (stdDocElement, "version");

 		RootStandard root = new RootStandard(stdDocElement, this);
		map.put(root.getId(), root);

		List lsItems = doc.selectNodes("/comm-core/Standard");
		prtln (lsItems.size() + " items found");
		for (Iterator i = lsItems.iterator(); i.hasNext(); ) {
			Element e = (Element) i.next();
			Standard std = new Standard(e, this);
			map.put(std.getId(), std);
		}

	}

	private String getChildText (Element parent, String tagName) throws Exception {
		String text = null;
		try {
			Element child = parent.element (tagName);
			text = child.getTextTrim();
		} catch (Throwable t) {
			prtln ("getChildText: " + t.getMessage());
		}
		return text;
	}

	/**
	 *  Description of the Method
	 *
	 * @return    Description of the Return Value
	 */
	public String toString() {
		String s = "\ntitle: " + this.getTitle();
		s += "\n\t" + "identifier: " + this.getIdentifier();
		s += "\n\t" + "fileCreated: " + this.getFileCreated();
		s += "\n\t" + "version: " + this.getVersion();
		s += "\n\t" + "description: " + this.getDescription();
		s += "\n\t" + "author: " + this.getAuthor();
		s += "\n\t" + "topic: " + this.getTopic();
		return s;
	}


	/**
	 *  The main program for the StdDocument class
	 *
	 * @param  args           The command line arguments
	 * @exception  Exception  Description of the Exception
	 */
	public static void main(String[] args) throws Exception {

		String filename = "comm_core_test.xml";
		// String dir = "H:/python-lib/common_core/";

		String dir = "/Users/ostwald/devel/python-lib/common_core/";
		
		
		StdDocument stdDoc = new StdDocument(dir + filename);
		prtln (stdDoc.toString());

/* 		 Standard root = stdDoc.getRootStandard();
		 if (root == null)
			 throw new Exception ("root standard not found");
		 // prtln ("root: " + root.toString());
		 prtln ("root: " + root.getItemText());
		 for (Iterator i=root.getChildren().iterator();i.hasNext();) {
			 Standard std = (Standard)i.next();
			 prtln ("\t" + std.getItemText());
		 } */
		 
		// test a particular std (with pre-k in grade range
 		String id = "8.EE.3";
		Standard std = stdDoc.getStandard(id);
		if (std == null)
			throw new Exception ("std not found for " + id);
		prtln(std.toString());
	}


	/**
	 *  Gets the standardTest attribute of the StdDocument class
	 *
	 * @param  stdDoc  Description of the Parameter
	 * @param  asnId   Description of the Parameter
	 */
	private static void getStandardTest(StdDocument stdDoc, String asnId) {
		Standard std = stdDoc.getStandard(asnId);
		List children = std.getChildren();
		prtln("children (" + children.size() + ")");
		for (Iterator i = children.iterator(); i.hasNext(); ) {
			Standard childStd = (Standard) i.next();
			if (childStd == null) {
				prtln("NULL");
			}
			else {
				prtln(childStd.getId());
			}
		}
	}


	/**
	 *  Description of the Method
	 *
	 * @param  s  Description of the Parameter
	 */
	private static void prtln(String s) {
		if (debug) {
			// SchemEditUtils.prtln(s, "StdDocument");
			SchemEditUtils.prtln(s, "");
		}
	}
}

