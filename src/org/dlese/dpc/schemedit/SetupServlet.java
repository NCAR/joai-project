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
package org.dlese.dpc.schemedit;

import org.dlese.dpc.schemedit.config.*;
import org.dlese.dpc.schemedit.sif.SIFRefIdManager;
import org.dlese.dpc.util.Files;
import org.dlese.dpc.util.strings.FindAndReplace;
import org.dlese.dpc.schemedit.standards.config.SuggestionServiceManager;
import org.dlese.dpc.schemedit.standards.adn.AsnToAdnMapper;

import org.dlese.dpc.schemedit.security.auth.AuthUtils;

import org.dlese.dpc.xml.Dom4jUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;

import org.dlese.dpc.webapps.tools.*;

import java.io.*;
import java.util.*;
import java.text.SimpleDateFormat;

// Enterprise imports
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

/**
 *  Servlet responsible for initializing the schemedit environment, such as the
 *  Framework and CollectionRegistries, that do not require access to the index
 *  or vocabManager.
 *
 * @author    Jonathan Ostwald
 */

public final class SetupServlet extends HttpServlet {

	private static boolean debug = true;
	private File configDefaults = null;
	private String dcsConfig = null;


	/**
	 *  Make sure the repository exists and there is a master collection record
	 *  before the repository manager is initilized in the DDSServlet.
	 *
	 * @param  config                Description of the Parameter
	 * @exception  ServletException  Description of the Exception
	 */
	public void init(ServletConfig config)
		 throws ServletException {
		System.out.println("\n" + getDateStamp() + " SetupServlet starting");
		String initErrorMsg = "";
		try {
			super.init(config);
		} catch (Throwable exc) {
			initErrorMsg = "SetupServlet Initialization Error:\n  " + exc;
			prtlnErr(initErrorMsg);
		}

		SetupServlet.setLogging();

		ServletContext servletContext = getServletContext();

		// Set up Records Directories if necessary
		String collBaseDir = getAbsolutePath((String) servletContext.getInitParameter("collBaseDir"));
		String collectionRecordsLocation = getAbsolutePath(collBaseDir + "/dlese_collect");
		prtln("collectionRecordsLocation: " + collectionRecordsLocation);

		File collectionRecords = new File(collectionRecordsLocation, "collect");
		// ensure collectionRecords directory exists
		if (!collectionRecords.exists()) {
			prtln("creating collection records directory at " + collectionRecords.getAbsolutePath());
			try {
				if (!collectionRecords.mkdirs()) {
					throw new Exception("directories not created for unknown reason");
				}
			} catch (Throwable e) {
				initErrorMsg = "ERROR: collection record directory could not be created: " + e.getMessage();
				prtlnErr(initErrorMsg);
				throw new ServletException(initErrorMsg);
			}
		}
		// ensure masterRecord exists
		String masterRecordName = "DCS-COLLECTION-000-000-000-001.xml";
		File masterRecord = new File(collectionRecords, masterRecordName);
		if (!masterRecord.exists()) {
			prtln("master collection record does not exist, creating ...");
			String defaultMaster = getAbsolutePath("WEB-INF/data/" + masterRecordName);
			try {
				Files.copy(new File(defaultMaster), masterRecord);
			} catch (Exception e) {
				prtlnErr("could not create master Record: " + e.getMessage());
			}
		}

		// make sure the directory where idFiles (saving current id for collection IDGenerators) exists
		String repositoryData = getAbsolutePath((String) servletContext.getInitParameter("repositoryData"));
		File idFilesDir = new File (repositoryData, "idFiles");
		if (!idFilesDir.exists()) {
			prtln("directory doesn't exist at " + idFilesDir);
			if (!idFilesDir.mkdirs()) {
				prtlnErr("ERROR: failed to create idFiles directory");
				throw new ServletException ("ERROR: failed to create idFiles directory");
			}
		}
		
		servletContext.setAttribute("idFilesDir", idFilesDir);

		// Set up Collection and Framework Config Directories
		String defaultsPath = getAbsolutePath("WEB-INF/dcs-config");
		configDefaults = new File(defaultsPath);
		if (!configDefaults.exists()) {
			throw new ServletException("Default DCS configs not found at " + configDefaults);
		}

		// collectionConfigDir
		dcsConfig = getAbsolutePath((String) servletContext.getInitParameter("dcsConfig"));

		prtln("\ndefaultsPath: " + defaultsPath);
		prtln("dcsConfig: " + dcsConfig);

		collectionConfigSetUp();
		frameworkConfigSetUp();
		authConfigSetUp();
		usersSetUp();

		logoSetup();

		standardsSuggestionServiceSetup();

		String sifRefIdConfig = getAbsolutePath((String) servletContext.getInitParameter("sifRefIdConfig"));
		SIFRefIdManager.setPath(sifRefIdConfig);

		// authenticationEnabled is set in AuthorizationFilter.init()
		if ((Boolean) servletContext.getAttribute("authenticationEnabled"))
			loginModuleSetUp();

		System.out.println(getDateStamp() + " SetupServlet completed.\n");
	}


	/**
	 *  Determine the enabled loginModules and store them in the ServletContext as
	 *  "loginModules". If the FileLogin module is enabled, initialize the
	 *  PasswordHelper.
	 */
	void loginModuleSetUp() {
		List loginModules = AuthUtils.getConfiguredLoginModules(true);
		getServletContext().setAttribute("loginModules", loginModules);
		if (AuthUtils.loginModuleEnabled("org.dlese.dpc.schemedit.security.login.FileLogin")) {
			String passwordFile = AuthUtils.getPasswordFile();
			if (passwordFile != null) {
				org.dlese.dpc.schemedit.security.login.PasswordHelper.getInstance(passwordFile);
				prtln("PasswordHelper initialized");
			}
		}
	}


	/**
	 *  Activate the standards suggestion service as configured. <p>
	 *
	 *  Two options are available: first we look for the configuration file pointed
	 *  to by the "standardsSuggestionServiceConfig" init param, which is used to
	 *  configure multiple frameworks, or frameworks other than ncs_item. If this
	 *  is not present, then look for the file or directory pointed to by the
	 *  "asnStandardsDocument" parameter, which is used to configure the standards
	 *  suggestion service for the "ncs_item" framework only.
	 *
	 * @exception  ServletException  Description of the Exception
	 */
	void standardsSuggestionServiceSetup() throws ServletException {
		prtln("\nstandardsSuggestionServiceSetup() ...");
		String errMsg;
		File configFile = null;

		// try to find standardsSuggestion Config file
		String configParam =
			(String) getServletContext().getInitParameter("standardsSuggestionServiceConfig");
		if (configParam != null && configParam.trim().length() > 0) {

			String configPath = getAbsolutePath(configParam);
			configFile = new File(configPath);
		}
		else {
			// try to configure only NCS_ITEM framework off of asnStandardsDocument
			configParam =
				(String) getServletContext().getInitParameter("asnStandardsDocument");
			if (configParam != null && configParam.trim().length() > 0) {
				prtln("configuring suggestion service from asnStandardsDocument");
				String asnDocPath = getAbsolutePath(configParam);

				File asnFile = new File(asnDocPath);
				if (!asnFile.exists()) {
					errMsg = "ASN Standards document does not exist at " + asnDocPath;
					throw new ServletException(errMsg);
				}
				try {
					configFile = this.getNcsItemSuggestionConfig(asnFile);
				} catch (Exception e) {
					throw new ServletException("Failed to create ncsItem suggestion config file: " + e.getMessage());
				}
			}
		}

		if (configFile == null) {
			// no suggestion config available
			return;
		}

		if (!configFile.exists()) {
			errMsg = "standards suggestion service config file does not exist at " + configFile;
			throw new ServletException(errMsg);
		}

		// NOTE: this should be done when we KNOW we have a ADN Suggestion service ...

		// set up standardsMapper, which helps us map between ADN and ASN standards.
		// only used by DleseSuggestionServiceHelper
		String mappingDataPath = getAbsolutePath("WEB-INF/data/Adn-to-Asn-v1.2.5-info.xml");
		AsnToAdnMapper standardsMapper = new AsnToAdnMapper(mappingDataPath);
		getServletContext().setAttribute("standardsMapper", standardsMapper);

		SuggestionServiceManager suggestionServiceManager = null;
		try {
			suggestionServiceManager = new SuggestionServiceManager(configFile);
		} catch (Throwable e) {
			errMsg = "Could not instantiate SuggestionServiceManager: " + e.getMessage();
			throw new ServletException(errMsg);
		}
		getServletContext().setAttribute("suggestionServiceManager", suggestionServiceManager);
		prtln("placed suggestionServiceManager in servlet context");
	}


	/**
	 *  Create a suggestionConfig file - for ncs_item framework - pointing to
	 *  provided ASN Standards Document, place it in the dcs_config directory, and
	 *  return the file.
	 *
	 * @param  asnFile        asnDocument file
	 * @return                a standards service configuration file pointing to
	 *      asnFile
	 * @exception  Exception  if unable to create config file
	 */
	private File getNcsItemSuggestionConfig(File asnFile) throws Exception {
		// template path is hard coded (it will be relative to WEB-INF
		String path = this.getAbsolutePath("WEB-INF/data/ncsItemSuggestionConfigTemplate.xml");

		File template = new File(path);
		if (!template.exists()) {
			throw new Exception("config template not found at " + path);
		}
		Document doc = null;
		try {
			doc = Dom4jUtils.getXmlDocument(template);
		} catch (DocumentException e) {
			throw new Exception("Could not parse config template: " + e.getMessage());
		}

		Element stdSource = (Element) doc.selectSingleNode("//stdSource");
		if (stdSource == null) {
			throw new Exception("stdSource element not found in template config");
		}

		if (asnFile.isDirectory()) {
			Element standardsDirectory = stdSource.addElement("standardsDirectory");
			standardsDirectory.setText(asnFile.getAbsolutePath());
			standardsDirectory.setAttributeValue("defaultAuthor", "NSES");
			standardsDirectory.setAttributeValue("defaultTopic", "Science");
		}

		else {
			Element standardsFile = stdSource.addElement("standardsFile");
			standardsFile.setText(asnFile.getAbsolutePath());
		}

		File dest = new File(dcsConfig, "ncsItemServiceConfig.xml");
		try {
			Dom4jUtils.writeDocToFile(doc, dest);
			prtln("wrote ncsSuggestionConfig to " + dest);
		} catch (Exception e) {
			throw new Exception("could not write suggestionServiceConfig: " + e.getMessage());
		}
		return dest;
	}


	/**  Sets the logging attribute of the SetupServlet class */
	public static void setLogging() {

		org.dlese.dpc.schemedit.autoform.RendererHelper.setLogging(false);
		org.dlese.dpc.xml.schema.StructureWalker.setDebug(false);
		org.dlese.dpc.xml.schema.SchemaReader.setDebug(false);
		org.dlese.dpc.xml.schema.SchemaHelper.setVerbose(false);
		org.dlese.dpc.xml.schema.DefinitionMiner.setDebug(false);
	}


	/**
	 *  Ensure that the collectionConfig directory exists, moving config files from
	 *  deploy directory if necessary, finally setting "collectionConfigDir"
	 *  attribute in servletContext
	 *
	 * @exception  ServletException  NOT YET DOCUMENTED
	 */
	private void collectionConfigSetUp() throws ServletException {
		String errorMsg;
		File collectionConfigDir = new File(dcsConfig, "collections");
		if (!collectionConfigDir.exists()) {
			prtln("creating collection config directory at " + collectionConfigDir.getAbsolutePath());
			try {
				if (!collectionConfigDir.mkdirs()) {
					throw new Exception("reason unknown");
				}
			} catch (Throwable e) {
				errorMsg = "ERROR: collection config directory could not be created: " + e.getMessage();
				prtlnErr(errorMsg);
				throw new ServletException(errorMsg);
			}
		}
		prtln("collectionConfigDir: " + collectionConfigDir);
		getServletContext().setAttribute("collectionConfigDir", collectionConfigDir);

		// move files (might be only default) from deploy dir to configDir
		// String deployConfigPath = getAbsolutePath("WEB-INF/collection-config");
		// File deployConfigDir = new File(deployConfigPath);
		File defaultCollections = new File(configDefaults, "collections");
		try {
			populateConfigDir(defaultCollections, collectionConfigDir);
		} catch (Exception e) {
			prtln("collection config seti[ error: " + e.getMessage());
		}
	}


	/**
	 *  Place default auth files into the dcsConfig directory ONLY if they do not
	 *  already exist there.
	 *
	 * @exception  ServletException  NOT YET DOCUMENTED
	 */
	private void authConfigSetUp() throws ServletException {
		String errorMsg;

		File defaultAuth = new File(configDefaults, "auth");
		File authConfigDir = new File(dcsConfig, "auth");
		if (!authConfigDir.exists()) {
			prtln("creating auth config directory at " + authConfigDir.getAbsolutePath());
			try {
				if (!authConfigDir.mkdirs()) {
					throw new Exception("reason unknown");
				}
			} catch (Throwable e) {
				errorMsg = "ERROR: auth config directory could not be created: " + e.getMessage();
				prtlnErr(errorMsg);
				throw new ServletException(errorMsg);
			}
		}

		String[] configFiles = {"access.xml", "ncslogin.config", "passwd"};
		// String deployConfigPath = getAbsolutePath("WEB-INF");
		for (int i = 0; i < configFiles.length; i++) {
			String fileName = configFiles[i];
			File src = new File(defaultAuth, fileName);
			File dest = new File(authConfigDir, fileName);
			if (!src.exists()) {
				throw new ServletException("src config does not exist at " + src.getAbsolutePath());
			}
			if (!dest.exists()) {
				try {
					Files.copy(src, dest);
					prtln("installed " + fileName + " as " + dest.getAbsolutePath());
					if ("ncslogin.config".equals(fileName)) {
						loginConfInit(dest);
					}
				} catch (Exception e) {
					throw new ServletException("unable to copy " + fileName + " to " + dest.getAbsolutePath());
				}
			}
		}
	}


	/**
	 *  Initialize the login configuration file to point to the passwd file in the
	 *  same directory.
	 *
	 * @param  loginConfig    login config file to be altered
	 * @exception  Exception  Description of the Exception
	 */
	private void loginConfInit(File loginConfig) throws Exception {
		String mydir = loginConfig.getParentFile().getAbsolutePath();
		mydir = FindAndReplace.replace(mydir, "\\", "/", false);

		String content = Files.readFile(loginConfig).toString();
		// content = content.replaceFirst("@MY_DIR@", loginConfig.getParentFile().getCanonicalPath());

		content = content.replaceFirst("@MY_DIR@", mydir);
		Files.writeFile(content, loginConfig);
	}


	/**
	 *  Ensure that the frameworkConfig directory exists, moving config files from
	 *  deploy directory if necessary, finally setting "frameworkConfigDir"
	 *  attribute in servletContext.<p>
	 *
	 *  NOTE: FrameworkConfigs only written to when the directory didn't exist a
	 *  priori.
	 *
	 * @exception  ServletException  NOT YET DOCUMENTED
	 */
	private void frameworkConfigSetUp() throws ServletException {
		// create  frameworkConfigDir if it doesn't already exist
		String errorMsg;
		File frameworkConfigDir = new File(dcsConfig, "frameworks");

		prtln("frameworkConfigDir: " + frameworkConfigDir);
		getServletContext().setAttribute("frameworkConfigDir", frameworkConfigDir);

		/*
		 *  FrameworkConfigs only written to when the directory didn't exist prior
		 */
		if (frameworkConfigDir.exists()) {
			return;
		}

		if (!frameworkConfigDir.exists()) {
			prtln("creating framework config directory at " + frameworkConfigDir.getAbsolutePath());
			try {
				if (!frameworkConfigDir.mkdirs()) {
					throw new Exception("reason unknown");
				}
			} catch (Throwable e) {
				errorMsg = "ERROR: framework config directory could not be created: " + e.getMessage();
				prtlnErr(errorMsg);
				throw new ServletException(errorMsg);
			}
		}

		// String deployConfigPath = getAbsolutePath("WEB-INF/framework-config");
		File defaultFrameworks = new File(configDefaults, "frameworks");
		try {
			populateConfigDir(defaultFrameworks, frameworkConfigDir);
		} catch (Exception e) {
			prtln("framework config setup error: " + e.getMessage());
			e.printStackTrace();
		}
	}


	/*
	** Make sure there's a user directory
	*/
	private void usersSetUp() throws ServletException {
		// create  usersDir if it doesn't already exist
		String errorMsg;
		File usersDir = new File(dcsConfig, "users");

		/*
		 *  FrameworkConfigs only written to when the directory didn't exist prior
		 */
		if (usersDir.exists()) {
			return;
		}

		prtln("creating users directory at " + usersDir.getAbsolutePath());
		try {
			if (!usersDir.mkdirs()) {
				throw new Exception("reason unknown");
			}
		} catch (Throwable e) {
			errorMsg = "ERROR: user directory could not be created: " + e.getMessage();
			prtlnErr(errorMsg);
			throw new ServletException(errorMsg);
		}

		// String deployConfigPath = getAbsolutePath("WEB-INF/framework-config");
		File defaultUsers = new File(configDefaults, "users");
		try {
			populateConfigDir(defaultUsers, usersDir);
		} catch (Exception e) {
			prtln("users setup error: " + e.getMessage());
			e.printStackTrace();
		}
	}


	/**
	 *  Copy files from source directory to dest directory if they do not already
	 *  exist in the dest directory.
	 *
	 * @param  src            NOT YET DOCUMENTED
	 * @param  dest           NOT YET DOCUMENTED
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	private void populateConfigDir(File src, File dest) throws Exception {
		if (!src.exists()) {
			throw new Exception("directory does not exist at " + src.getAbsolutePath());
		}

		if (!dest.exists()) {
			throw new Exception("directory does not exist at " + dest.getAbsolutePath());
		}

		if (dest.equals(src))
			return;

		// move contents of oldConfig to New (if oldConfig is empty that's fine)
		File[] files = src.listFiles();
		int moved = 0;
		for (int i = 0; i < files.length; i++) {
			File configFile = files[i];
			String fileName = configFile.getName();
			File newConfigFile = new File(dest, fileName);
			if (newConfigFile.exists() || configFile.isDirectory()) {
				// prtln ("config file already exists - not overwriting " + newConfigFile.getAbsolutePath());
				continue;
			}
			if (Files.copy(configFile, newConfigFile)) {
				moved++;
			}
			else {
				prtln("WARNING: failed to move " + fileName);
			}
		}
		if (moved > 0) {
			prtln("moved " + moved + " of " + files.length + " config files to " + dest.getAbsolutePath());
		}
	}


	/**
	 *  Initialize logo from init params, and copy logo file to auth directory,
	 *  where it can be accessed from logon/logoff pages.
	 */
	private void logoSetup() {
		ServletContext servletContext = getServletContext();
		String logo = (String) servletContext.getInitParameter("logo");
		servletContext.setAttribute("logo", logo);
		prtln("logo: " + logo);

		try {
			// prtln("\ncopying logo ...");
			String logoPath = servletContext.getRealPath("images/" + logo);
			// prtln("\t logoPath: " + logoPath);
			String authLogoPath = servletContext.getRealPath("auth/" + logo);
			// prtln("\t authLogoPath: " + authLogoPath);
			Files.copy(new File(logoPath), new File(authLogoPath));
		} catch (Throwable t) {
			prtlnErr("trouble copying logo to auth dir: " + t.getMessage());
		}
	}


	/**  Performs shutdown operations. */
	public void destroy() {
		prtln("destroy() ...");
		System.out.println(getDateStamp() + " SetupServlet stopped");
	}


	/**
	 *  Return a string for the current time and date, sutiable for display in log
	 *  files and output to standout:
	 *
	 * @return    The dateStamp value
	 */
	public static String getDateStamp() {
		return
			new SimpleDateFormat("MMM d, yyyy h:mm:ss a zzz").format(new Date());
	}


	/**
	 *  Sets the debug attribute of the SetupServlet class
	 *
	 * @param  d  The new debug value
	 */
	public static void setDebug(boolean d) {
		debug = d;
	}


	/**
	 *  Output a line of text to error out, with datestamp.
	 *
	 * @param  s  The text that will be output to error out.
	 */
	private final void prtlnErr(String s) {
		System.err.println(getDateStamp() + " SetupServlet: " + s);
	}


	/**
	 *  Output a line of text to standard out, with datestamp, if debug is set to
	 *  true.
	 *
	 * @param  s  The String that will be output.
	 */
	private final void prtln(String s) {
		if (debug) {
			// System.out.println(getDateStamp() + " SetupServlet: " + s);
			SchemEditUtils.prtln(s, " SetupServlet");
		}
	}



	/**
	 *  Gets the absolute path to a given file or directory. Assumes the path
	 *  passed in is eithr already absolute (has leading slash) or is relative to
	 *  the context root (no leading slash). If the string passed in does not begin
	 *  with a slash ("/"), then the string is converted. For example, an init
	 *  parameter to a config file might be passed in as "WEB-INF/conf/serverParms.conf"
	 *  and this method will return the corresponding absolute path "/export/devel/tomcat/webapps/myApp/WEB-INF/conf/serverParms.conf."
	 *  <p>
	 *
	 *  If the string that is passed in already begings with "/", nothing is done.
	 *  <p>
	 *
	 *  Note: the super.init() method must be called prior to using this method,
	 *  else a ServletException is thrown.
	 *
	 * @param  fname                 An absolute or relative file name or path
	 *      (relative the the context root).
	 * @return                       The absolute path to the given file or path.
	 * @exception  ServletException  An exception related to this servlet
	 */
	private String getAbsolutePath(String fname)
		 throws ServletException {
		if (fname == null) {
			return null;
		}
		return GeneralServletTools.getAbsolutePath(fname, getServletContext());
	}

}

