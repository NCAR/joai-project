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

import java.io.*;
import java.util.*;
import java.text.*;

import org.dlese.dpc.xml.*;
import org.dlese.dpc.util.*;
import org.dlese.dpc.repository.*;
import org.dlese.dpc.schemedit.*;
import org.dlese.dpc.schemedit.config.*;
import org.dlese.dpc.schemedit.dcs.*;
import org.dlese.dpc.schemedit.vocab.layout.*;

import org.dom4j.Document;
import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import org.dom4j.Attribute;
import org.dom4j.Node;

/**
 *  Tester for {@link org.dlese.dpc.schemedit.config.FrameworkConfigReader}
 *
 *@author    ostwald
 <p>$Id: FrameworkConfigTester.java,v 1.9 2009/07/07 03:29:38 ostwald Exp $
 */
public class FrameworkConfigTester {
	public FrameworkConfigReader reader = null;
	public MetaDataFramework framework = null;
	FrameworkRegistry registry = null;

	// String configDirPath = "/devel/ostwald/projects/schemedit-project/web/WEB-INF/framework-config";
	String configDirPath;
	
	/**
	 *  Constructor for the FrameworkConfigTester object
	 */
	public FrameworkConfigTester(String xmlFormat) throws Exception {
		prtln ("xmlFormat: " + xmlFormat);
		configDirPath = TesterUtils.getFrameworkConfigDir();
		File sourceFile = new File(configDirPath, xmlFormat + ".xml");
		
		if (!sourceFile.exists()) {
			prtln("source File does not exist at " + sourceFile.toString());
			return;
		}
		prtln ("config: " + sourceFile);
		try {
			reader = new FrameworkConfigReader(sourceFile);
			framework = new MetaDataFramework(reader);
		} catch (Exception e) {
			prtln ("Initialization error: " + e.getMessage());
		}
		prtln ("schemaURI: " + framework.getSchemaURI());
	}

	FrameworkRegistry getFrameworkRegistry (String configDirPath) {
		return new FrameworkRegistry(configDirPath, null);
	}

	/**
	 *  Description of the Method
	 */
	public void showFrameworkStuff() {
		prtln("\n-----------------------");
		prtln("FRAMEWORK Stuff");
		prtln ("name: " + framework.getName());
		prtln ("renderer: " + this.framework.getRenderer());
	}
	
	public void showReaderStuff() {
		prtln("\n-----------------------");
		prtln ("READER Stuff");
		prtln ("name: " + this.reader.getName());
		prtln ("xmlFormat: " + this.reader.getXmlFormat());
		prtln ("renderer: " + this.reader.getRenderer());
	}
	
	/**
	 *  The main program for the FrameworkConfigTester class
	 *
	 *@param  args  The command line arguments
	 */
	public static void main(String[] args) throws Exception {
		prtln("FrameworkConfigTester");
		org.dlese.dpc.schemedit.autoform.RendererHelper.setLogging(false);
		TesterUtils.setSystemProps();
		String xmlFormat = "msp2";
		
		if (args.length > 0)
			xmlFormat = args[0];
		
		FrameworkConfigTester tester = new FrameworkConfigTester(xmlFormat);
		tester.showSchemaPathMap();
		// pp (tester.reader.getDocMap().getDocument());
		
		// tester.showReaderStuff ();
		// tester.showFrameworkStuff();
 		// DocMap docMap = reader.getDocMap();

	}

	public void showSchemaPathMap () {
		prtln ("\nSchemaPathMap");
		prtln (reader.getSchemaPathMap().toString());
	}
	
	public Document getMinimalRecord () {
		Document minnie = null;
		try {
			framework.loadSchemaHelper();
			minnie = framework.makeMinimalRecord("FOO-ID");
			// pp(minnie);
		} catch (Exception e) {
			prtln (e.getMessage());
		}
		return minnie;
	}
		
	
	/**
	 *  Utility to show XML in pretty form
	 *
	 *@param  node  Description of the Parameter
	 */
	public static void pp(Node node) {
		prtln(Dom4jUtils.prettyPrint(node));
	}

	/**
	 *  Description of the Method
	 *
	 *@param  s  Description of the Parameter
	 */
	public static void prtln(String s) {
		System.out.println(s);
	}
}

