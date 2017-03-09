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

import java.io.*;
import java.util.*;

import javax.servlet.ServletContext;

import org.dlese.dpc.schemedit.standards.config.SuggestionServiceManager;

import org.dlese.dpc.xml.*;
import org.dlese.dpc.vocab.MetadataVocab;
import org.dlese.dpc.vocab.MetadataVocabReloadEvent;
import org.dlese.dpc.vocab.MetadataVocabReloadListener;

import org.dlese.dpc.schemedit.config.ErrorLog;

/**
 *  A map holding {@link org.dlese.dpc.schemedit.MetaDataFramework} instances,
 *  and keyed by the short name (e.g., "adn") of each particular framework.<p>
 *
 *  The Registry is populated at system startup time. It reads framework config
 *  files and instantiates a MetaDataFramework instance for each one.
 *
 * @author    ostwald
 */
public class FrameworkRegistry implements MetadataVocabReloadListener {
	private static boolean debug = true;

	String AUDIENCE = "cataloger";
	String LANGUAGE = "en-us";

	private HashMap loadedFrameworks = null;
	// private List unloadedFrameworks = null;
	private File configDir = null;
	private MetadataVocab vocab = null;
	String docRoot;
	String configDirPath;
	List muiKnownFormats = null;
	private SuggestionServiceManager suggestionServiceManager = null;
	private boolean allowDleseCollectItems = false;

	ErrorLog loadErrors = new ErrorLog();
	ErrorLog loadWarnings = new ErrorLog();

	String[] NON_ITEM_FORMATS = {"dcs_data", "framework_config", "collection_config"};


	/**  Constructor for the FrameworkRegistry object */
	public FrameworkRegistry() {
		// prtln ("instantiating registry");
		this.loadedFrameworks = new HashMap();
	}


	/**
	 *  FrameworkRegistry constructure with ServletContext
	 *
	 * @param  servletContext  the servletContext
	 */
	public FrameworkRegistry(ServletContext servletContext) {
		this();
		this.configDir = (File) servletContext.getAttribute("frameworkConfigDir");
		this.vocab = (MetadataVocab) servletContext.getAttribute("MetadataVocab");
		this.suggestionServiceManager = (SuggestionServiceManager)
			servletContext.getAttribute("suggestionServiceManager");

		if (suggestionServiceManager == null)
			prtln("suggestionServiceManager is NULL");

		this.allowDleseCollectItems = "true".equals(
			(String) servletContext.getInitParameter("allowDleseCollectItems"));

		this.docRoot = servletContext.getRealPath("/");
		load();
	}


	/**
	 *  Constructor for the FrameworkRegistry object for specified configuration
	 *  directory and docRoot (used for debugging - docRoot is normally calculated
	 *  from servletContext).
	 *
	 * @param  configDirPath  path to directory containing framework config files
	 * @param  docRoot        path to servlet baseDir
	 */
	public FrameworkRegistry(String configDirPath, String docRoot) {
		this();
		this.configDir = new File(configDirPath);
		this.docRoot = docRoot;
		load();
	}


	/**
	 *  Gets the configFiles present in the framework config directory
	 *
	 * @return    an Array of framework configuration files
	 */
	private File[] getConfigFiles() {
		return configDir.listFiles(new XMLFileFilter());
	}


	/**
	 *  Gets the directory in which framework config files are located.
	 *
	 * @return    The configDir value
	 */
	public File getConfigDir() {
		return this.configDir;
	}


	/**
	 *  Loads the framework for specified xmlformat after finding the framework
	 *  config file
	 *
	 * @param  xmlFormat      the xmlFormat for the framework (e.g., "adn")
	 * @exception  Exception  if the framework could not be loaded
	 */
	public void loadFramework(String xmlFormat) throws Exception {
		File configFile = new File(configDir, xmlFormat + ".xml");
		loadFramework(configFile);
	}


	/**
	 *  Instantiate a {@link MetaDataFramework} from the provided configFile,
	 *  render the metadata editor pages for the framework, and register it in this
	 *  FrameworkRegistry.
	 *
	 * @param  configFile     Framework configuration file
	 * @exception  Exception  if the framework could not be loaded, or the metadata
	 *      editor could not be registered
	 */
	private void loadFramework(File configFile) throws Exception {
		MetaDataFramework mdf = new MetaDataFramework(configFile, docRoot);
		if (mdf == null) {
			throw new Exception("failed to initialize metadataFramework for " + configFile.getAbsolutePath());
		}
		try {
			mdf.loadSchemaHelper();
		} catch (Exception e) {
			throw new Exception("Could not load schema: " + e.getMessage());
		}

		if (suggestionServiceManager != null && suggestionServiceManager.hasConfig(mdf.getXmlFormat())) {
			try {
				mdf.setStandardsManager(suggestionServiceManager.createStandardsManager(mdf));
				prtln("Instantiated " + mdf.getStandardsManager().getClass().getName());
			} catch (Throwable t) {
				prtln("WARNING: unable to instantiate StandardsManager for \"" +
					mdf.getXmlFormat() + "\": " + t.getMessage());
			}
		}

		if (vocab != null && getMuiKnownFormats().contains(mdf.getXmlFormat())) {
			mdf.setMuiGroups(vocab, AUDIENCE, LANGUAGE);
		}

		if (mdf.getRebuildOnStart()) {
			try {
				mdf.renderEditorPages();
			} catch (Throwable e) {
				throw new Exception("RenderEditorPages error: " + e.getMessage());
			}
		}

		// Warn of vocabLayout path errors
		List errors = mdf.validateVocabLayoutPaths();
		for (Iterator i=errors.iterator();i.hasNext();) {
			loadWarnings.add (mdf.getXmlFormat(), (String)i.next());
		}
			
		// Warn of fieldInfo path errors
		errors = mdf.validateFieldInfoPaths();
		for (Iterator i=errors.iterator();i.hasNext();) {
			loadWarnings.add (mdf.getXmlFormat(), (String)i.next());
		}

		register(mdf);
		prtln("registered " + mdf.getXmlFormat() + "\n");
	}



	/**
	 *  Loads the FrameworkRegistry by traversing the framework config files in the
	 *  config directory.
	 */
	public void load() {

		if (configDir == null || !configDir.exists()) {
			String msg = "framework config directory does not exist at " + configDirPath;
			// throw new Exception (msg);
			prtln("\n\n *** ERROR: " + msg + " ***\n\n");
			return;
		}

		prtln("Loading Framework Registry from " + configDir);

		File[] configFiles = this.getConfigFiles();
		// prtln ("about to process " + configFiles.length + " framework config files");
		for (int i = 0; i < configFiles.length; i++) {
			prtln("Processing framework config file (" + (i + 1) + " of " + configFiles.length + ") : " + configFiles[i].getName());
			String xmlFormat = getFormatForConfigFile(configFiles[i]);
			try {
				loadFramework(configFiles[i]);
			} catch (Exception e) {
				String errorMsg = configFiles[i].getName() + " format NOT registered: " + e.getMessage();
				prtln("ERROR: " + errorMsg + "\n");
				loadErrors.add(xmlFormat, e.getMessage());
				// this.unloadedFrameworks.put (mdf.getXmlFormat(), mdf);
			}
		}
	}


	/**
	 *  Gets the loadErrors attribute of the FrameworkRegistry object
	 *
	 * @return    The loadErrors value
	 */
	public ErrorLog getLoadErrors() {
		return loadErrors;
	}


	/**  Clear the load errors */
	public void clearLoadErrors() {
		loadErrors.clear();
	}

	/**
	 *  Gets the loadWarnings attribute of the FrameworkRegistry object
	 *
	 * @return    The loadWarnings value
	 */
	public ErrorLog getLoadWarnings() {
		return loadWarnings;
	}


	/**  Clear the load warnings */
	public void clearLoadWarnings() {
		loadWarnings.clear();
	}

	/**
	 *  Finds the groups files in the MetadataVocab for the "cataloger" audience
	 *
	 * @return    The muiKnownFormats value
	 */
	private List getMuiKnownFormats() {
		if (muiKnownFormats == null) {
			muiKnownFormats = new ArrayList();
			if (vocab != null) {
				for (Iterator i = vocab.getVocabSystemInterfaces().iterator(); i.hasNext(); ) {
					String system = (String) i.next();
					String[] splits = system.split("/");
					if (splits != null && splits.length > 3) {
						String format = splits[0];
						String version = splits[1];
						String audience = splits[2];
						String language = splits[3];
						for (int j = 0; j < splits.length; j++)
							if (!muiKnownFormats.contains(splits[0]) && audience.equals(AUDIENCE)) {
								muiKnownFormats.add(splits[0]);
							}
					}
				}
			}
		}
		return muiKnownFormats;
	}


	/**
	 *  Event handler for MetadataVocabReloadEvent registers MUI groups with
	 *  appropriate frameworks and then re-renders the editors for frameworks that
	 *  have registered MUI groups.
	 *
	 * @param  event  NOT YET DOCUMENTED
	 */
	public void metadataVocabReloaded(MetadataVocabReloadEvent event) {
		prtln("Registering Mui groups and re-rendering EditorPages");
		this.vocab = (MetadataVocab) event.getSource();
		this.muiKnownFormats = null;
		for (Iterator i = loadedFrameworks.values().iterator(); i.hasNext(); ) {
			MetaDataFramework mdf = (MetaDataFramework) i.next();
			if (getMuiKnownFormats().contains(mdf.getXmlFormat())) {
				mdf.setMuiGroups(vocab, AUDIENCE, LANGUAGE);
				if (mdf.getRebuildOnStart()) {
					try {
						mdf.renderEditorPages();
					} catch (Throwable e) {
						prtln("RenderEditorPages error: " + e.getMessage());
					}
				}
			}
		}
		prtln("  ... done re-rendering");
	}


	/**
	 *  Initialize each framework with information about which fields can be
	 *  formatted using MUI Groups files.
	 */
	public void extractMuiGroups() {
		if (vocab != null) {
			prtln("\nextractMuiGroups() ... ");
			for (Iterator i = loadedFrameworks.values().iterator(); i.hasNext(); ) {
				MetaDataFramework framework = (MetaDataFramework) i.next();
				framework.setMuiGroups(vocab, AUDIENCE, LANGUAGE);
			}
		}
	}


	/**
	 *  Register provided MetaDataFramework.
	 *
	 * @param  mdf  the framework to be loaded
	 */
	public void register(MetaDataFramework mdf) {
		// unloadedFrameworks.remove(mdf.getXmlFormat());
		String xmlFormat = mdf.getXmlFormat();
		this.unregister(xmlFormat);
		loadedFrameworks.put(mdf.getXmlFormat(), mdf);
	}


	/**
	 *  Remove the framework for provided xmlFormat from the registry
	 *
	 * @param  xmlFormat  format (e.g., "adn") corresponding to a loaded framework
	 */
	public void unregister(String xmlFormat) {
		MetaDataFramework mdf = this.getFramework(xmlFormat);
		loadedFrameworks.remove(xmlFormat);
		if (mdf != null) {
			mdf.destroy();
			prtln("unregistered " + xmlFormat + "\n");
		}
	}


	/**
	 *  Gets the framework for the specified xmlFormat
	 *
	 * @param  xmlFormat  format (e.g., "adn") corresponding to a loaded framework
	 * @return            The framework
	 */
	public MetaDataFramework getFramework(String xmlFormat) {
		// prtln ("getFramework with " + frameworkName);
		return (MetaDataFramework) loadedFrameworks.get(xmlFormat);
	}


	/**
	 *  The number of registered frameworks
	 *
	 * @return    the number of registered frameworks
	 */
	public int size() {
		return loadedFrameworks.size();
	}


	/**
	 *  Gets the xmlFormat (e.g., "adn") for the proviced framework config file
	 *  (assumes config files are named by their format (e.g. "adn.xml")
	 *
	 * @param  file  a framework config file
	 * @return       the xmlFormat (e.g., "adn")
	 */
	private String getFormatForConfigFile(File file) {
		String filename = file.getName();
		return filename.substring(0, filename.length() - ".xml".length());
	}


	/**
	 *  Returns true if the framework for provided xmlFormat is currently loaded in
	 *  this FrameworkRegistry
	 *
	 * @param  xmlFormat  e.g., ("adn")
	 * @return            true if framework is loaded.
	 */
	public boolean getIsLoaded(String xmlFormat) {
		return (this.getFramework(xmlFormat) != null);
	}


	/**
	 *  Returns a list of xmlFormats corresponding to framework config files that
	 *  are present in the framework config directory, but are not currently loaded
	 *  in the FrameworkRegistry object
	 *
	 * @return    The unloadedFrameworks
	 */
	public List getUnloadedFrameworks() {
		// return this.unloadedFrameworks;
		List unloaded = new ArrayList();
		List loaded = this.getAllFormats();
		File[] configs = this.getConfigFiles();
		for (int i = 0; i < configs.length; i++) {
			String xmlFormat = getFormatForConfigFile(configs[i]);
			if (!loaded.contains(xmlFormat))
				unloaded.add(xmlFormat);
		}
		return unloaded;
	}


	/**
	 *  Return a string representation of the registry for debugging purposes.
	 *
	 * @return    Description of the Return Value
	 */
	public String toString() {
		String s = "Framework Registry";
		for (Iterator i = getAllFormats().iterator(); i.hasNext(); ) {
			String key = (String) i.next();
			s += "\n\t" + key;
		}
		return s;
	}


	/**
	 *  Return a list of formats for registered frameworks.
	 *
	 * @return    The allFormats value
	 */
	public List getAllFormats() {
		List formats = new ArrayList();
		for (Iterator i = loadedFrameworks.keySet().iterator(); i.hasNext(); ) {
			String format = (String) i.next();
			formats.add(format);
		}
		return formats;
	}


	/**
	 *  Gets the registered formats that are "item" frameworks (e.g., "adn",
	 *  "dlese_anno", as opposed to frameworks used internally (e.g., "dcs_data")
	 *  by the system.
	 *
	 * @return    The itemFormats value
	 */
	public List getItemFormats() {
		List formats = new ArrayList();
		List nonItemFormats = Arrays.asList(NON_ITEM_FORMATS);
		for (Iterator i = getAllFormats().iterator(); i.hasNext(); ) {
			String format = (String) i.next();
			if ("dlese_collect".equals(format) && !this.allowDleseCollectItems)
				continue;
			if (!nonItemFormats.contains(format) && !formats.contains(format))
				formats.add(format);
		}
		return formats;
	}


	/**
	 *  Gets the formats that are available to oai services, which are the
	 *  itemFrameworks plus "dlese_collect"
	 *
	 * @return    The oaiFormats value
	 */
	public List getOaiFormats() {
		List oaiFormats = this.getItemFormats();
		oaiFormats.add("dlese_collect");
		return oaiFormats;
	}


	/**
	 *  Return the formats of the registered frameworks, excluding "dlese_collect"
	 *
	 * @return    The names value
	 */
	public List getNames() {
		ArrayList names = new ArrayList();
		for (Iterator i = loadedFrameworks.values().iterator(); i.hasNext(); ) {
			MetaDataFramework framework = (MetaDataFramework) i.next();
			String name = framework.getName();
			if (!name.equals("dlese_collect") && !name.equals("dcs_data"))
				names.add(name);
		}
		return names;
	}


	/**  Destroys the loaded frameworks */
	public void destroy() {
		prtln("detroying registered frameworks");
		for (Iterator i = loadedFrameworks.values().iterator(); i.hasNext(); ) {
			MetaDataFramework framework = (MetaDataFramework) i.next();
			framework.destroy();
		}
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  args  NOT YET DOCUMENTED
	 */
	public static void main(String args[]) {
		String configDirPath = "/devel/ostwald/tomcat/tomcat/dcs_conf/framework_config";
		String docRoot = null;
		FrameworkRegistry reg = null;
		try {
			reg = new FrameworkRegistry(configDirPath, docRoot);
		} catch (Throwable t) {
			prtln("ERROR: " + t.getMessage());
		}
		prtln(reg.toString());
	}


	/**
	 *  Description of the Method
	 *
	 * @param  s  Description of the Parameter
	 */
	private static void prtln(String s) {
		if (debug) {
			// System.out.println("FrameworkRegistry: " + s);
			SchemEditUtils.prtln(s, "FrameworkRegistry");
		}
	}

}

