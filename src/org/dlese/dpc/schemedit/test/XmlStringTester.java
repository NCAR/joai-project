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

import org.dlese.dpc.schemedit.*;
import java.util.*;
import java.util.regex.*;
import java.net.*;
import java.io.*;
import org.dlese.dpc.xml.Dom4jUtils;
import org.dlese.dpc.xml.schema.*;
import org.dlese.dpc.util.Files;
import org.dom4j.*;
import org.dom4j.io.*;


/**
 *  Class for testing dom manipulation with help from {@link
 *  org.dlese.dpc.xml.schema.SchemaHelper}
 *
 *@author    ostwald<p>
 *
 *      $Id $
 */
public class XmlStringTester {

	private static boolean debug = true;

	Document doc = null;
	static String descriptionPath = "/itemRecord/general/description";
	XMLWriter stringWriter = null;
	String xml = null;


	/**
	 *  Constructor for the XmlStringTester object
	 *
	 *@param  in             Description of the Parameter
	 *@exception  Exception  Description of the Exception
	 */
	XmlStringTester(File in)
		throws Exception {
		xml = Files.readFile(in).toString();
		xml = SchemEditUtils.expandAmpersands(xml);
		doc = Dom4jUtils.getXmlDocument(xml);
		stringWriter = getStringWriter();
	}


	/**
	 *  Gets the stringWriter attribute of the XmlStringTester object
	 *
	 *@return    The stringWriter value
	 */
	XMLWriter getStringWriter() {
		OutputFormat format = new OutputFormat("    ", true);
		format.setTrimText(true);
		StringWriter sw = new StringWriter();
		try {
			stringWriter = new XMLWriter(sw, format);
		} catch (Exception e) {
			prtln("getStringWriter(): " + e);
		}

		prtln("  ... format.encoding: " + format.getEncoding());
		prtln("  ... format.isNewLines: " + format.isNewlines());
		prtln("  ... format.isTrimText: " + format.isTrimText());
		prtln("  ... format.indent: \'" + format.getIndent() + "\'");

		stringWriter.setEscapeText(false);
		if (stringWriter.isEscapeText()) {
			prtln("  ... isEscapeText");
		}
		else {
			prtln("  ... is NOT escapeText");
		}
		return stringWriter;
	}


	/**
	 *  Description of the Method
	 *
	 *@param  in  Description of the Parameter
	 *@return     Description of the Return Value
	 */
	static String prettyXml(File in) {
		String xml = "";
		try {
			xml = Files.readFile(in).toString();
			return prettyXml(xml);
		} catch (Exception e) {
			prtln("prettyXml error: " + e.getMessage());
			return xml;
		}
	}


	/**
	 *  Description of the Method
	 *
	 *@param  xml  Description of the Parameter
	 *@return      Description of the Return Value
	 */
	static String prettyXml(String xml) {
		Document doc = null;
		OutputFormat format = new OutputFormat("    ", true);
		format.setTrimText(true);
		StringWriter sw = new StringWriter();
		try {
			xml = SchemEditUtils.expandAmpersands(xml);
			doc = Dom4jUtils.getXmlDocument(xml);
			XMLWriter prettyWriter = new XMLWriter(sw, format);
			prettyWriter.write(doc);
		} catch (Throwable e) {
			prtln("prettyXml error: " + e.getMessage());
			return "";
		}
		return sw.toString();
	}


	/**
	 *  A unit test for JUnit
	 *
	 *@param  in             Description of the Parameter
	 *@return                Description of the Return Value
	 *@exception  Exception  Description of the Exception
	 */
	static String test1(File in)
		throws Exception {
		XmlStringTester dt = new XmlStringTester(in);
		return dt.getString(dt.doc);
	}


	/**
	 *  Gets the filePath attribute of the XmlStringTester class
	 *
	 *@param  uri            Description of the Parameter
	 *@return                The filePath value
	 *@exception  Exception  Description of the Exception
	 */
	static String getFilePath(URI uri)
		throws Exception {
		
		String uriPath = uri.getPath();
		if (uriPath == null || uriPath.length() == 0) {
			throw new Exception ("getFilePath error: provided uri (" + uri.toString() + ") contains no path component");
		}
		
		String path = uri.toString();
		if (path.length() > 4 && path.substring(0,5).equals("file:"))
			path = path.substring (5);
/* 		Pattern filePat = Pattern.compile("file:/[/]*(.*)");
		Matcher m = filePat.matcher(uri.toString());
		if (m.find()) {
			// prtln ("pattern found: " + m.group(1));
			path = "/" + m.group(1);
		}
		else {
			prtln("pattern not found");
		} */
		return path;
	}

	static void testUriStr (String uriStr, String docRoot) throws Exception {
		prtln ("uriStr: " + uriStr);
		prtln ("docRoot: " + docRoot);
		prtln ("");
		try {
			Object refObj = SchemEditUtils.getUriRef(uriStr, docRoot);
			if (refObj instanceof File) {
				File file = (File) refObj;
				prtln ("refObj is a FILE\n\t" + file.getAbsolutePath());
				if (file.exists())
					prtln ("file exists");
				else
					prtln ("file does NOT exist");
			}
			else if (refObj instanceof URL) {
				prtln ("refObj is a URL:\n\t" + ((URL)refObj).toString());
			}
			else
				prtln ("refObj could not be resolved");
		} catch (URISyntaxException e) {
			prtln ("ERROR: " + e.getMessage());
		}
		prtln ("===========\n");
	}
	
	
	static void testOne (String [] args) throws Exception {
		String uriStr = "file:///export/devel/ostwald/records/adn/sercnagt/oai%3Aserc.carleton.edu%3ASERC-NAGT-000-000-000-596.xml";
		if (args.length > 0)
			uriStr = args[0];
		prtln ("uriStr: " + uriStr);
		// String uriStr = "file:blah";
		URI uri;
		try {
			uri = new URI(uriStr);
		} catch (Exception e) {
			prtln("couldn't parse urlStr: " + e.getMessage());
			return;
		}
		prtln ("URI attributes");
		prtln("\turiPath: " + uri.getPath());
		prtln("\turiRawPath: " + uri.getRawPath());
		prtln("\turi is absolute: " + uri.isAbsolute());
		prtln("\turi is opaque: " + uri.isOpaque());
		prtln("\tscheme: " + uri.getScheme());

		prtln ("\n==================");
		prtln ("filePath: " + getFilePath(uri));

	}
	/**
	 *  The main program for the XmlStringTester class
	 *
	 *@param  args           The command line arguments
	 *@exception  Exception  Description of the Exception
	 */
	public static void main(String[] args)
		throws Exception {

		// testOne (args);
		prtln ("==================================================");
		
		String [] testers = {
			"file:///export/devel/ostwald/records/adn/sercnagt/oai%3Aserc.carleton.edu%3ASERC-NAGT-000-000-000-596.xml",
			"file:/export/devel/ostwald/records/adn/sercnagt/oai%3Aserc.carleton.edu%3ASERC-NAGT-000-000-000-596.xml",
			"file://export/devel/ostwald/records/adn/sercnagt/oai%3Aserc.carleton.edu%3ASERC-NAGT-000-000-000-596.xml",
			"/export/devel/ostwald/records/adn/sercnagt/oai%3Aserc.carleton.edu%3ASERC-NAGT-000-000-000-596.xml",
			"oai%3Aserc.carleton.edu%3ASERC-NAGT-000-000-000-596.xml",
			"file:oai%3Aserc.carleton.edu%3ASERC-NAGT-000-000-000-596.xml",
		};
		
		String docRoot = null;
		
		for (int i=0;i<testers.length;i++)
			testUriStr (testers[i], docRoot);
		
  		docRoot = "/export/devel/ostwald/records/adn/sercnagt";
		
		for (int i=0;i<testers.length;i++)
			testUriStr (testers[i], docRoot);
	}


	/**
	 *  Gets the string attribute of the XmlStringTester object
	 *
	 *@param  node  Description of the Parameter
	 *@return       The string value
	 */
	private String getString(Node node) {

		if (stringWriter == null) {
			prtln("ERROR: trying to execute sp without a stringWriter");
			return null;
		}
		StringWriter sw = new StringWriter();

		try {
			stringWriter.setWriter(sw);
			stringWriter.write(node);
			stringWriter.flush();
		} catch (Exception e) {
			prtln("sp: " + e.getMessage());
		}
		return sw.toString();
	}


	/**
	 *  Description of the Method
	 *
	 *@param  s  Description of the Parameter
	 */
	private static void prtln(String s) {
		if (debug) {
			// System.out.println("XmlStringTester: " + s);
			System.out.println(s);
		}
	}

}

