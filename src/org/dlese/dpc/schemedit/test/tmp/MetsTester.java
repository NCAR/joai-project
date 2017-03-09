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
package org.dlese.dpc.schemedit.test.tmp;

import org.dlese.dpc.xml.Dom4jUtils;
import org.dlese.dpc.xml.schema.DocMap;
import org.dlese.dpc.xml.schema.SchemaReader;
import org.dlese.dpc.xml.schema.SchemaHelper;
import org.dlese.dpc.xml.schema.StructureWalker;
import org.dlese.dpc.xml.schema.compositor.Compositor;
import org.dlese.dpc.schemedit.SchemEditUtils;

import java.io.File;
import java.net.URL;
import org.dom4j.Node;
import org.dom4j.Document;

/**
 *  Class to insert values into METS instance documents.
 *
 * @author     Jonathan Ostwald
 * @version    $Id: MetsTester.java,v 1.3 2009/03/20 23:33:58 jweather Exp $
 */
public class MetsTester {
	private static boolean debug = true;

	SchemaHelper schemaHelper = null;
	Document doc = null;
	DocMap docMap = null;


	/**  Constructor for the MetsTester object */
	public MetsTester() {
		this(null);
	}


	/**
	 *  Constructor for the MetsTester object for an existing METS xml document at
	 *  instanceDocPath.
	 *
	 * @param  instanceDocPath  NOT YET DOCUMENTED
	 */
	public MetsTester(String instanceDocPath) {
		SchemaHelper.setDebug(false);
		StructureWalker.setDebug(false);
		SchemaReader.setDebug(false);

		String metsUrl = "http://www.loc.gov/standards/mets/mets.xsd";
		try {
			URL schemaUrl = new URL(metsUrl);
			schemaHelper = new SchemaHelper(schemaUrl);
		} catch (Throwable t) {
			prtln("could not process schema at " + metsUrl);
			System.exit(1);
		}

		if (instanceDocPath == null) {
			doc = schemaHelper.getMinimalDocument();
		}
		else {
			try {
				doc = Dom4jUtils.getXmlDocument(new File(instanceDocPath));
			} catch (Exception e) {
				prtln("Couldn't parse document at " + instanceDocPath);
				System.exit(1);
			}
		}
		docMap = new DocMap(doc, schemaHelper);
	}


	/**
	 *  Assign a value at the specified xpath, creating a new Node in the instance
	 *  document if necessary. NOTE: checks to ensure the xpath is schema-legal.
	 *
	 * @param  xpath  xpath where value is assigned
	 * @param  value  value to be assigned
	 */
	void putValue(String xpath, String value) {
		try {
			docMap.smartPut(xpath, value);
		} catch (Exception e) {
			prtln("WARNING: putValue failed: " + e);
		}
	}


	/**
	 *  Demonstrates how values are inserted into an Existing Mets document. NOTE:
	 *  the namespace prefix used in the xpath must mach the namespace declared in
	 *  the instance document.
	 */
	public static void existingMetsDoc() {
		String instanceDocPath = "C:/tmp/metsInstanceDoc.xml";
		MetsTester tester = new MetsTester(instanceDocPath);

		prtln("\nInitial Document");
		pp(tester.doc);

		String xpath = "/METS:mets/METS:dmdSec/@ID";
		String value = "DM129276";
		tester.putValue(xpath, value);

		xpath = "/METS:mets/METS:dmdSec/METS:mdWrap/@MDTYPE";
		value = "OTHER";
		tester.putValue(xpath, value);

		pp(tester.doc);

		try {
			Dom4jUtils.writePrettyDocToFile(tester.doc,
				new File("L:/ostwald/tmp/validate-sandbox/MetsTesterOut.xml"));
		} catch (Exception e) {
			prtln("couldn't write to file: " + e);
		}
	}


	/**
	 *  Demonstrates how values are inserted into an empty Mets document. NOTE: the
	 *  namespace prefix in the Mets document is "this", so your xpaths must also
	 *  use this prefix.
	 */
	public static void newMetsDoc() {
		MetsTester tester = new MetsTester();

		prtln("\nInitial Document");
		pp(tester.doc);

		String xpath = "/this:mets/this:dmdSec/@ID";
		String value = "DM129276";
		tester.putValue(xpath, value);

		xpath = "/this:mets/this:dmdSec/this:mdWrap/@MDTYPE";
		value = "OTHER";
		tester.putValue(xpath, value);

		pp(tester.doc);

		try {
			Dom4jUtils.writePrettyDocToFile(tester.doc,
				new File("L:/ostwald/tmp/validate-sandbox/MetsTesterOut.xml"));
		} catch (Exception e) {
			prtln("couldn't write to file: " + e);
		}
	}


	/**
	 *  The main program for the MetsTester class
	 *
	 * @param  args           The command line arguments
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public static void main(String[] args) throws Exception {
		// newMetsDoc();
		existingMetsDoc();
	}


	/**
	 *  Prints a formatted Node to stdout.
	 *
	 * @param  node  NOT YET DOCUMENTED
	 */
	private static void pp(Node node) {
		prtln(Dom4jUtils.prettyPrint(node));
	}


	/**
	 *  Description of the Method
	 *
	 * @param  s  Description of the Parameter
	 */
	private static void prtln(String s) {
		System.out.println(s);
	}
}

