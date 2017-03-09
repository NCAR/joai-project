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
package org.dlese.dpc.schemedit.test;

import org.dlese.dpc.serviceclients.webclient.WebServiceClient;
import org.dlese.dpc.xml.*;
import org.dlese.dpc.xml.schema.*;
import org.dlese.dpc.schemedit.*;
import org.dlese.dpc.util.strings.*;

import java.io.*;
import java.util.*;
import java.text.*;
import java.util.regex.*;

import java.net.*;
import org.dom4j.Node;
import org.dom4j.Attribute;
import org.dom4j.Element;
import org.dom4j.Document;
import org.dom4j.DocumentFactory;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;

import com.sun.msv.datatype.xsd.*;
import org.relaxng.datatype.*;

/**
 *  Description of the Class
 *
 * @author    ostwald
 */
public class NameSpaceTester {
	private WebServiceClient webServiceClient = null;
	private GlobalDefMap globalDefMap = null;
	private Document instanceDocument = null;
	private XSDatatypeManager xsdDatatypeManager = null;
	private SchemaNodeMap schemaNodeMap = null;
	private DefinitionMiner definitionMiner;
	private XMLWriter writer;
	private DocumentFactory df = DocumentFactory.getInstance();
	private SchemaHelper sh;

	String schemaURI;

	/**
	 *  Constructor for the NameSpaceTester object
	 */
	public NameSpaceTester(String schemaURI) {
		this.schemaURI = schemaURI;
		try {
			sh = getSchemaHelper (schemaURI);
		} catch (Exception e) {
			prtln ("ERROR: " + e.getMessage());
			return;
		}
		
		if (sh != null) {
			prtln("SchemaHelper instantiated");
		}
		globalDefMap = sh.getGlobalDefMap();
		instanceDocument = sh.getInstanceDocument();
		schemaNodeMap = sh.getSchemaNodeMap();
		writer = Dom4jUtils.getXMLWriter();

		xsdDatatypeManager = new XSDatatypeManager(globalDefMap);
	}


	/**
	 *  The main program for the NameSpaceTester class
	 *
	 * @param  args  The command line arguments
	 */
	public static void main(String[] args) {
		NameSpaceTester t = null;
		String uri = "file:/devel/ostwald/metadata-frameworks/adn-item-project/record.xsd";
		
		t = new NameSpaceTester(uri);
		if (t.sh == null) {
			prtln ("schemaHelper not instantiated");
			return;
		}
		
		prtln ("version: " + t.sh.getVersion());
		prtln ("targetNamespace: " + t.sh.getTargetNamespace());
		
		prtln ("\n Instance Doc namespace info");
		prtln (t.getNameSpaceInfo());
	}
	
	String getNameSpaceInfo () {
		String s = "xmlns=\"" + sh.getTargetNamespace() + "\" ";
		s += "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" ";
		s += "xsi:schemaLocation=\"" + sh.getTargetNamespace() + " " + schemaURI + "\"";
		return s;
	}
		
	
	/**
	 *  Description of the Method
	 *
	 * @param  uriStr  Description of the Parameter
	 */
	public static SchemaHelper getSchemaHelper (String schemaURI) throws Exception {
		URI uri = new URI(schemaURI);

		// t = new NameSpaceTester(uri);
		prtln("schemaURI: " + uri.toString());
		String scheme = uri.getScheme();
		// prtln("Scheme: " + scheme);
		// prtln("isAbsolute: " + uri.isAbsolute());
		String path = uri.getPath();

		if (scheme == null) {
			throw new Exception ("scheme is null");
		}
		else if (scheme.equals("file")) {
			File file = new File(path);
			if (!file.exists()) {
				throw new Exception ("file doesn't exist at " + path);
			}
			return new SchemaHelper(file);
		}
		else if (scheme.equals("http")) {
			prtln("scheme is http");
			URL url = uri.toURL();
			return new SchemaHelper (url);
		}
		else {
			throw new Exception ("unrecognized scheme: " + scheme);
		}
	}

	/**
	 *  Description of the Method
	 *
	 * @param  o  Description of the Parameter
	 */
	private void write(Object o) {
		try {
			writer.write(o);
			prtln("");
		} catch (Exception e) {
			prtln("couldn write");
		}
	}


	/**
	 *  Description of the Method
	 *
	 * @param  node  Description of the Parameter
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
		// System.out.println("NameSpaceTester: " + s);
		System.out.println(s);
	}
}

