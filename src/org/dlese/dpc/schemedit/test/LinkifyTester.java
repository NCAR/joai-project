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
import org.dlese.dpc.schemedit.autoform.*;
import java.util.*;
import java.util.regex.*;
import java.net.*;
import java.io.*;
import org.dlese.dpc.xml.*;
import org.dlese.dpc.xml.schema.*;
import org.dlese.dpc.xml.schema.compositor.Compositor;
import org.dlese.dpc.util.Files;
import org.dom4j.*;


/**
 *  Class for testing dom manipulation with help from {@link org.dlese.dpc.xml.schema.SchemaHelper}
 *
 *@author     ostwald<p>
 $Id $
 */
public class LinkifyTester {

	private static boolean debug = true;
	MetaDataFramework framework = null;
	String xmlFormat;
	
	LinkifyTester (String xmlFormat) {
		this.xmlFormat = xmlFormat;
		String configFileDir = "/devel/ostwald/tomcat/tomcat/dcs_conf/framework_config/";
		String docRoot = "/devel/ostwald/tomcat/tomcat/webapps/schemedit/";

		// make sure the prop file really exists
		File configFile = new File(configFileDir, xmlFormat + ".xml");
		String configFilePath = configFile.toString();
		if (!configFile.exists()) {
			prtln("propfile doesn't exist at " + configFilePath);
			return;
		}
		else {
			framework = new MetaDataFramework(configFilePath, docRoot);
		}

		try {
			framework.loadSchemaHelper();
		} catch (Exception e) {
			prtln("failed to instantiate SchemaHelper: " + e);
			return;
		}
		prtln ("LinkifyTester instantiated\n");
	}
	
	MetaDataFramework getFramework() {
		return framework;
	}
	
	// patStr defines two groups:
	// 1 - the part of the pattern _before_ the match
	// 2 - the part of the pattern that will be replaced
	private String linker (String s, Pattern p, String urlStr) {
/* 		String elementPat = "&lt;" + schemaNamespace.getPrefix() + ":element name=\"";
		String namePatStr = elementPat + ".+?\""; 
		p = Pattern.compile(namePatStr); */
		Matcher m;
		int index = 0;
		while (true) {
			m = p.matcher(s);

			// replace occurrences one by one
			if (m.find(index)) {
				// prtln("group: " + m.group());
				String content = s.substring(m.start(), m.end());

				String oldStr = m.group(2);
				prtln ("oldStr: " + oldStr);
				
				String href = urlStr + "/" + oldStr;
				String newStr = "<a href=\'" + href + "\'>" + oldStr + "</a>";
				prtln ("newStr: " + newStr);
				
				s = s.substring(0, m.start() + m.group(1).length()) + newStr + s.substring(m.end() - 1);
				index = m.end();
			}
			else {
				break;
			}
		}
		return s;
	}

	
	private String linkify (String s, String xpath) {
		String nameBaseUrl = "/schemedit/schema/schema.do?command=doPath&path=";
		Pattern p = Pattern.compile("<");
		s = p.matcher(s).replaceAll("&lt;");

		// linkify element names
		Namespace schemaNamespace = (Namespace)getFramework().getSchemaHelper().getSchemaProps().getProp("namespace");
		String elementPat = "(&lt;" + schemaNamespace.getPrefix() + ":element name=\")";
		String namePatStr = elementPat + "(.+?)\"";
		prtln ("name pattern: " + namePatStr);
		p = Pattern.compile(namePatStr);
		s = linker (s, p, nameBaseUrl + xpath);
		
		// linkify type names
		String typeBaseUrl = "/schemedit/schema/schema.do?command=doType&typeName=";
		String typePatStr = "(type=\")(.+?)\"";
		prtln ("type pattern: " + typePatStr);
		p = Pattern.compile(typePatStr);
		s = linker (s, p, typeBaseUrl + xpath);
		
		return s;
	}
	
	private String linkifyOriginal (String s, String xpath) {
		String baseUrl = "/schemedit/schema/schema.do?command=doPath&path=";
		Pattern p = Pattern.compile("<");
		s = p.matcher(s).replaceAll("&lt;");

		// linkify element names
		Namespace schemaNamespace = (Namespace)getFramework().getSchemaHelper().getSchemaProps().getProp("namespace");
		String elementPat = "&lt;" + schemaNamespace.getPrefix() + ":element name=\"";
		String namePatStr = elementPat + ".+?\"";
		// String namePatStr = "&lt;xsd:element name=\".+?\"";
		p = Pattern.compile(namePatStr);
		Matcher m;
		int index = 0;
		while (true) {
			m = p.matcher(s);

			// replace occurrences one by one
			if (m.find(index)) {
				// prtln("group: " + m.group());
				String content = s.substring(m.start(), m.end());
				// prtln ("content: " + content + "(" + content.length() + ")");
				String name = content.substring(elementPat.length(), content.length() - 1);
				// prtln ("name: " + name);
				String href = baseUrl + xpath + "/" + name;
				String replaceStr = "<a href=\'" + href + "\'>" + name + "</a>";
				s = s.substring(0, m.start() + elementPat.length()) + replaceStr + s.substring(m.end() - 1);
				index = m.end();
			}
			else {
				break;
			}
		}

		// linkify dataTypes
		baseUrl = "/schemedit/schema/schema.do?command=doType&typeName=";
		String prefix = "type=\"";
		String typePatStr = prefix + ".+?\"";
		// String namePatStr = "&lt;xsd:element name=\".+?\"";
		p = Pattern.compile(typePatStr);
		index = 0;
		while (true) {
			m = p.matcher(s);

			// replace occurrences one by one
			if (m.find(index)) {
				// prtln("group: " + m.group());
				String content = s.substring(m.start(), m.end());
				// prtln ("content: " + content + "(" + content.length() + ")");
				String typeName = content.substring(prefix.length(), content.length() - 1);
				// prtln ("typeName: " + typeName);
				String href = baseUrl + typeName + "&path=" + xpath;
				String replaceStr = "<a href=\'" + href + "\'>" + typeName + "</a>";
				s = s.substring(0, m.start() + prefix.length()) + replaceStr + s.substring(m.end() - 1);
				index = m.end();
			}
			else {
				break;
			}
		}
		return s;
	}
	
	public static void main (String [] args) throws Exception {
		String xmlFormat = "status_report_simple";
		// String renderPath = "/statusReport";
		LinkifyTester lt = new LinkifyTester(xmlFormat);
		SchemaHelper sh = lt.getFramework().getSchemaHelper();
		if (sh == null) {
			throw new Exception ("schemaHelper not found");
		}
	
		SchemaUtils.showGlobalDefs (sh);
		
		String xpath = "/statusReport";
		String typeName = "statusReportType";
		GlobalDef globalDef = sh.getGlobalDef (typeName);
		if (globalDef == null)
			prtln ("globalDef not found for " + typeName);
		SchemaNode schemaNode = sh.getSchemaNode(xpath);
		if (schemaNode == null)
			prtln ("schemaNode not found at " + xpath);
		pp (globalDef.getElement());
		
		String linked = "<pre>" + lt.linkify(globalDef.getElement().asXML(), xpath) + "</pre>";
		Files.writeFile(linked, new File ("/home/ostwald/tmp/linkify.html"));
		prtln (linked);

	}

	private static void  pp (Node node) {
		prtln (Dom4jUtils.prettyPrint(node));
	}
	
	
	/**
	 *  Description of the Method
	 *
	 *@param  s  Description of the Parameter
	 */
	private static void prtln(String s) {
		if (debug) {
			// System.out.println("LinkifyTester: " + s);
			System.out.println(s);
		}
	}

	
}

