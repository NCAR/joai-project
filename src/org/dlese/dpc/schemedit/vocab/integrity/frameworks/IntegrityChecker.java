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
package org.dlese.dpc.schemedit.vocab.integrity.frameworks;

import org.dlese.dpc.schemedit.vocab.integrity.Utils;

import org.dlese.dpc.schemedit.SchemEditUtils;
import org.dlese.dpc.xml.Dom4jUtils;
import org.dlese.dpc.xml.schema.SchemaHelper;
import org.dlese.dpc.xml.schema.DefinitionMiner;
import org.dlese.dpc.xml.schema.StructureWalker;
import org.dlese.dpc.xml.schema.SchemaReader;
import org.dlese.dpc.xml.schema.SchemaHelperException;
import org.dlese.dpc.xml.schema.GlobalDefMap;
import org.dlese.dpc.xml.schema.SchemaNodeMap;
import org.dlese.dpc.xml.schema.SchemaNode;
import org.dlese.dpc.xml.schema.GlobalDef;
import org.dlese.dpc.xml.schema.GenericType;

import org.dlese.dpc.xml.XPathUtils;

import java.io.*;
import java.util.*;
import java.text.*;
import java.net.*;
import java.lang.*;

import org.dom4j.*;

/**
 *  Same purpose as {@link org.dlese.dpc.schemedit.vocab.integrity.IntegrityChecker},
 *  but works over files in the framework-project, rather than the MUI.
 *
 * @author    ostwald <p>
 *
 *
 */
public class IntegrityChecker {

	static boolean debug = false;
	static boolean SCHEMA_DEBUG = false;

	File projectDir; // frameworks-project
	File frameworksFile; // XML file containing specs for the various frameworks to be tested
	Element frameworks = null; // rootElement of frameworksFile
	List checkers;
	boolean truncate = false;


	/**
	 *  This class In this class we are given the path to the frameworks.xml file,
	 *  within a frameworks-project directory. We parse the frameworks.xml file to
	 *  gather information about the frameworks we want to check. We check the
	 *  fields files for the specified frameworks against the schemas that are also
	 *  specified in the frmameworks.xml files. the schemas can be within the
	 *  frameworks-project or they may reside at any URI
	 *
	 * @param  frameworksFilepath  file containing information about the frameworks
	 *      to check
	 * @exception  Exception       NOT YET DOCUMENTED
	 */
	public IntegrityChecker(String frameworksFilepath) throws Exception {

		frameworksFile = new File(frameworksFilepath).getCanonicalFile();
		if (!frameworksFile.exists())
			throw new Exception("Frameworks file does not exist at " + frameworksFilepath);

		// we know that the frameworksFile is X levels deep into the frameworks-project
		projectDir = frameworksFile.getParentFile().getParentFile().getParentFile();

		try {
			Document doc = Dom4jUtils.getXmlDocument(frameworksFile);
			frameworks = doc.getRootElement();
		} catch (Exception e) {
			throw new Exception("Unable to parse XML at " + frameworksFilepath);
		}

		this.checkers = new ArrayList();
	}


	/**
	 *  Gets the fieldFilesChecker attribute of the IntegrityChecker object
	 *
	 * @param  versionDir     a framework/version directory within the frameworks
	 *      project (e.g., adn/0.6.50)
	 * @param  rootElement    NOT YET DOCUMENTED
	 * @param  schemaURI      NOT YET DOCUMENTED
	 * @return                The fieldFilesChecker value
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	FieldFilesChecker getFieldFilesChecker(File versionDir, URI schemaURI, String rootElement) throws Exception {
		SchemaHelper schemaHelper = null;

		try {
			if ("http".equals(schemaURI.getScheme()))
				schemaHelper = new SchemaHelper(schemaURI.toURL(), rootElement);
			else if ("file".equals(schemaURI.getScheme()))
				schemaHelper = new SchemaHelper(new File(schemaURI.getPath()), rootElement);
			else
				throw new Exception("could not process schemaURI: \"" + schemaURI + "\"");
		} catch (Throwable t) {
			prtln(t.getMessage());
			schemaHelper = null;
		}

		if (schemaHelper == null)
			throw new Exception("schemaHelper not instantiated");
		FieldFilesChecker newChecker = new FieldFilesChecker(versionDir, schemaHelper);
		checkers.add(newChecker);
		return newChecker;
	}


	/**  for each framework element in the frameworks file, */
	void processFrameworks() {
		// process each of the framework directories
		Iterator frameworksElements = frameworks.elementIterator();
		while (frameworksElements.hasNext()) {
			Element frameworkElement = (Element) frameworksElements.next();
			String format = frameworkElement.attributeValue("name", "??");
			try {
				processFramework(frameworkElement);
			} catch (Exception e) {
				prtlnErr("unable to process framework for " + format + "\n" + e.getMessage() + "\n ... skipping");
			}
		}
	}


	/**
	 *  Check the fields files in the specified framework against the specified
	 *  schema. 
	 *
	 * @param  frameworkElement  element from framework.xml file
	 * @exception  Exception     NOT YET DOCUMENTED
	 */
	void processFramework(Element frameworkElement) throws Exception {

		String name = frameworkElement.attributeValue("name", null);
		String dir = frameworkElement.attributeValue("dir", null);
		String uri = frameworkElement.attributeValue("uri", null);
		String rootElement = frameworkElement.attributeValue("rootElement", null);

		processFramework(name, dir, uri, rootElement);
	}


	/**
	 *  Check the fields files in the specified framework against the specified
	 *  schema. Both Fields fiels and schema are determined from parameters. Field
	 *  files are found using the "dir" parameter, and the schema is found using
	 *  the "uri" parameter. The schema may be local or at a web address.
	 *
	 * @param  name           framework name (e.g., 'adn')
	 * @param  dir            location of fields file listing
	 * @param  uri            specifies SchemaURI (can be absolute or relative)
	 * @param  rootElement    required to instantiate schemaHelper
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	void processFramework(String name, String dir, String uri, String rootElement) throws Exception {

		/* 		prtln("\nPROCESS FRAMEWORK");
		prtln("\t name: " + name);
		prtln("\t dir: " + dir);
		prtln("\t uri: " + uri);
		prtln("\t rootElement: " + rootElement);
 */
		URI schemaUri;
		// if uri is absolute, then resolving will not change it.
		// if it is relative, then it will be resolved against projectDir
		try {
			schemaUri = this.projectDir.toURI().resolve(uri);

		} catch (Exception e) {
			throw new Exception("unable to create schemaURI from " + uri);
		}

		// Framework Directory is of the form: frameworks-project/frameworks/<format>/<version>/
		File frameworkDir = new File(this.projectDir, "frameworks/" + dir);
		if (!frameworkDir.exists())
			throw new Exception("framework dir does not exist at " + frameworkDir.toString());

		/* 		prtln(Utils.line(20, "+"));
		prtln("Processing Framework (" + name + " - " + frameworkDir + ") ..."); */
		if (this.truncate) {
			prtln("\nTRUNCATED ... returning");
			return;
		}

		FieldFilesChecker checker = null;
		try {
			checker = this.getFieldFilesChecker(frameworkDir, schemaUri, rootElement);
			checker.doCheck();
		} catch (Exception e) {

			if (checkers != null) {
				checkers.remove(checker);
			}
			throw e;
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}


	/**  NOT YET DOCUMENTED */
	void report() {

		prtln("\n\n" + Utils.line("="));
		prtln(Utils.underline("FieldFiles Integrety Checker Report - " + Utils.getTimeStamp()));
		prtln("project directory: " + this.projectDir.toString());
		prtln("Reporting on " + checkers.size() + " Fields File Listings");
		prtln("");
		for (Iterator i = checkers.iterator(); i.hasNext(); ) {
			FieldFilesChecker checker = (FieldFilesChecker) i.next();
			checker.doReport();
		}
	}


	/**
	 *  Read a set of fields files
	 *
	 * @param  args  The command line arguments
	 */
	public static void main(String[] args) {
		// System.setProperty("javax.xml.transform.TransformerFactory",
			// "com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl");
		SchemaHelper.setDebug(SCHEMA_DEBUG);
		DefinitionMiner.setDebug(SCHEMA_DEBUG);
		StructureWalker.setDebug(SCHEMA_DEBUG);
		SchemaReader.setDebug(SCHEMA_DEBUG);
		GlobalDefMap.setDebug(SCHEMA_DEBUG);

		boolean verbose = false;

		// defaults - get overridden if there is a param passed by caller (which there always should be)
		String frameworksProjectDir = "C:/Documents and Settings/ostwald/devel/projects/frameworks-project/";
		String frameworksPath = frameworksProjectDir + "frameworks-support/frameworks.xml";

		if (args.length > 0)
			frameworksPath = args[0];

		if (args.length == 2)
			verbose = args[1].equals("true");

		FieldFilesChecker.setVerbose(verbose);
		ErrorManager.setVerbose(verbose);

		IntegrityChecker checker = null;

		try {
			checker = new IntegrityChecker(frameworksPath);
			checker.processFrameworks();
			checker.report();
		} catch (Exception e) {
			prtln(e.getMessage());
			return;
		} catch (Throwable t) {
			prtln("Unknown Error!");
			t.printStackTrace();
		}
	}


	/**
	 *  Output a line of text to standard out, if debug is set to true.
	 *
	 * @param  s  The String that will be output.
	 */
	private static void prtln(String s) {
		System.out.println(s);
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  s  NOT YET DOCUMENTED
	 */
	private static void prtlnErr(String s) {
		System.out.println("\nERROR: " + s);
	}


	class CVSDirectoryFilter implements FileFilter {
		/**
		 *  A FileFilter to accept on directories that aren't named 'CVS'.
		 *
		 * @param  file  The file in question.
		 * @return       True if the file is a Directory and is not named 'CVS'
		 */
		public boolean accept(File file) {
			return (file.isDirectory() && !file.getName().equals("CVS"));
		}
	}

}


