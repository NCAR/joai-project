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

import org.dlese.dpc.schemedit.config.FrameworkConfigReader;

import org.dlese.dpc.xml.*;
import org.dlese.dpc.xml.schema.*;
import org.dlese.dpc.xml.schema.compositor.Compositor;
import org.dlese.dpc.schemedit.*;
import org.dlese.dpc.util.strings.*;
import org.dlese.dpc.util.EnvReader;

import java.io.*;
import java.util.*;
import java.text.*;
import java.util.regex.*;

import java.net.*;
import org.dom4j.Node;
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
 *  Methods to help tester classes use information in the framework config files.
 *
 *@author     ostwald
 *@created    June 27, 2006
 */
public class TesterUtils {

	public static MetaDataFramework getFramework (String xmlFormat) throws Exception {
		String configFileDir = TesterUtils.getFrameworkConfigDir();
		String docRoot = TesterUtils.getDocRoot();
		
		File configFile = new File(configFileDir, xmlFormat + ".xml");
		String configFilePath = configFile.toString();
		if (!configFile.exists()) {
			throw new Exception ("configFile doesn't exist at " + configFilePath);
		}
		MetaDataFramework framework = new MetaDataFramework(configFilePath, docRoot);

		try {
			framework.loadSchemaHelper();
		} catch (Exception e) {
			throw new Exception ("failed to instantiate SchemaHelper: " + e);
		}
		
		return framework;
	}
	
	public static String getTomcatDir () throws Exception {
		String host = EnvReader.getProperty("HOST").toLowerCase();
		
		if (host.indexOf("maryjane") == 0 ) {
			return "/Library/Java/Extensions/tomcat/tomcat/";
		}

		if (host.indexOf("taos") == 0) {
			return "/Library/Java/Extensions/tomcat/tomcat/";
		}
		if (host.indexOf("tremor") == 0) {
			return "/devel/ostwald/tomcat/tomcat/";
		}
		if (host.indexOf("mtsherman") == 0) {
			return "/Library/Java/Extensions/tomcat/tomcat/";
		}
		
		if (host.indexOf("dls-sanluis") == 0) {
			return "C:/Program Files/Apache Software Foundation/Tomcat 5.5/";
		}
		
		throw new Exception ("ERROR: Unknown host: " + host);
	}
		
	public static String getTmpDir () throws Exception {
		String host = EnvReader.getProperty("HOST").toLowerCase();
		
		if (host.indexOf("maryjane") == 0 ) {
			return "/Users/ostwald/devel/tmp";
		}

		if (host.indexOf("taos") == 0) {
			return "/Users/ostwald/devel/tmp";
		}

		if (host.indexOf("mtsherman") == 0) {
			return "/Users/ostwald/devel/tmp";
		}
		
		if (host.indexOf("dls-sanluis") == 0) {
			return "C:/tmp";
		}
		
		throw new Exception ("ERROR: Unknown host: " + host);
	}
	
	public static String getCannedCMPath () throws Exception {
		String host = EnvReader.getProperty("HOST").toLowerCase();
		if (host.indexOf("tremor") == 0) {
			return "/home/ostwald/python-lib/ndr/CannedCollectionMetadata.xml";
		}
		if (host.indexOf("mtsherman") == 0) {
			return "/Users/jonathan/devel/tmp/CannedCollectionMetadata.xml";
		}
		if (host.indexOf("dls-sanluis") == 0) {
			// return "C:ndr\\CannedCollectionMetadata.2.xml"
			return "C:/ndr/CannedCollectionMetadata.jlo.xml";
		}
		throw new Exception ("could not get CannedCMPath for host \"" + host + "\"");
	}
	
	public static String getRecordsPath () throws Exception {
		String host = EnvReader.getProperty("HOST").toLowerCase();
		if (host.indexOf("tremor") == 0) {
			return "/devel/ostwald/records";
		}
		
		if (host.indexOf("MtSherman") == 0) {
			return "/Users/jonathan/Devel/dcs-records/lean-records";
		}
		
		if (host.indexOf("dls-sanluis") == 0) {
			return "C:/Documents and Settings/ostwald/My Documents/devel/records";
		}
		
		throw new Exception ("recordsPath not defined for host \"" + host + "\"");
	}
		
	public static String getDocRoot () throws Exception {
		return getTomcatDir() + "webapps/schemedit";
	}
	
	 public static String getFrameworkConfigDir ()  throws Exception {
		 String host = EnvReader.getProperty("HOST").toLowerCase();
		 prtln ("host: " + host);
		 if (host.indexOf("dls-sanluis") == 0)
			 return getTomcatDir() + "var/dcs_conf/frameworks";
		 else if (host.indexOf("taos") == 0)
			 return getTomcatDir() + "var/dcs_conf/frameworks";
		 else
			return getTomcatDir() + "dcs_conf/frameworks";
	 }
	 
	 public static String getCollectionConfigDir ()  throws Exception {
		// return getTomcatDir() + "dcs_conf/collection_config";
		return "C:/Documents and Settings/ostwald/devel/dcs-instance-data/dev/config/collections";
	 }		

	/**
	 *  Gets the frameworkConfigReader attribute of the TesterUtils class
	 *
	 *@param  framework  Description of the Parameter
	 *@return            The frameworkConfigReader value
	 */
	public static FrameworkConfigReader getFrameworkConfigReader(String framework)
	 throws Exception {
		File source = new File(getFrameworkConfigDir(), framework + ".xml");
		prtln ("looking for file at " + source.toString());
		return getFrameworkConfigReader(source);
	}


	/**
	 *  Gets the frameworkConfigReader attribute of the TesterUtils class
	 *
	 *@param  source  Description of the Parameter
	 *@return         The frameworkConfigReader value
	 */
	public static FrameworkConfigReader getFrameworkConfigReader(File source) {

		if (!source.exists()) {
			prtln("WARNING: config file does not exist at " + source.toString());
			return null;
		}
		try {
			return new FrameworkConfigReader(source);
		} catch (Exception e) {
			prtln (e.getMessage());
			return null;
		}
	}

	public static String getSchemaUri (String xmlFormat) {
		prtln ("getSchemaUri() with xmlFormat: " + xmlFormat);
		FrameworkConfigReader fcr = null;
		try {
			fcr = getFrameworkConfigReader (xmlFormat);
		} catch (Exception e) {
			prtln (e.getMessage());
		}
		if (fcr == null)
			return "";
		else
			return fcr.getSchemaURI ();
	}
		
	public static void setSystemProps () {
		System.setProperty( "javax.xml.transform.TransformerFactory", 
			"com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl" );
	}

	/**
	 *  The main program for the TesterUtils class
	 *
	 *@param  args  The command line arguments
	 */
	public static void main(String[] args) {
		prtln("Hello from Tester Utils");
		String xmlFormat = "cd";
		String schemaUri = getSchemaUri (xmlFormat);
		prtln ("schemaUri: " + schemaUri);
	}


	/**
	 *  Description of the Method
	 *
	 *@param  node  Description of the Parameter
	 */
	private static String pp(Node node) {
		return Dom4jUtils.prettyPrint(node);
	}


	/**
	 *  Description of the Method
	 *
	 *@param  s  Description of the Parameter
	 */
	private static void prtln(String s) {
		// System.out.println("TesterUtils: " + s);
		System.out.println(s);
	}
}

