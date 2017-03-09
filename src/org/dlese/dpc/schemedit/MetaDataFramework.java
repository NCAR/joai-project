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

import org.dlese.dpc.util.strings.FindAndReplace;
import org.dlese.dpc.util.Files;
import org.dlese.dpc.vocab.MetadataVocab;
import org.dlese.dpc.xml.Dom4jUtils;
import org.dlese.dpc.xml.schema.*;
import org.dlese.dpc.schemedit.autoform.*;
import org.dlese.dpc.schemedit.config.*;
import org.dlese.dpc.schemedit.input.EnsureMinimalDocument;
import org.dlese.dpc.schemedit.input.ElementsOrderer;
import org.dlese.dpc.schemedit.vocab.FieldInfoReader;
import org.dlese.dpc.schemedit.vocab.FieldInfoMap;
import org.dlese.dpc.schemedit.vocab.layout.VocabLayoutConfig;
import org.dlese.dpc.schemedit.vocab.layout.VocabLayout;
import org.dlese.dpc.schemedit.security.user.User;
import org.dlese.dpc.schemedit.standards.StandardsManager;
import org.dlese.dpc.webapps.tools.GeneralServletTools;

import java.io.*;
import java.util.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.Comparator;

import org.dom4j.Document;
import org.dom4j.Attribute;
import org.dom4j.Element;
import org.dom4j.DocumentException;
import org.dom4j.Namespace;

/**
 *  Encapsulates information about a metadata framework in support of creating,
 *  displaying, editing and managing metadata records.<p>
 *
 *  <p>
 *
 *  MetaDataFramework instances are configured from a framework configuration
 *  file, and registered in a {@link org.dlese.dpc.schemedit.FrameworkRegistry}.
 *  <p>
 *
 *  Major components include:
 *  <ul>
 *    <li> a {@link org.dlese.dpc.xml.schema.SchemaHelper} instance</li>
 *    <li> a {@link org.dlese.dpc.schemedit.vocab.FieldInfoMap} for information
 *    such as best practices and definitions</li>
 *    <li> a {@link org.dlese.dpc.schemedit.config.SchemaPathMap}</li>
 *    <li> a {@link org.dlese.dpc.schemedit.config.FrameworkConfigReader} that
 *    provides information from the framework configuration file</li>
 *  </ul>
 *
 *
 * @author    ostwald
 */
public class MetaDataFramework implements Serializable {
	private static boolean debug = true;

	private List muiGroups = null;
	private StandardsManager standardsManager = null;

	private SchemaHelper schemaHelper = null;

	private String docRoot = null;
	private String workingSchemaURI = null;
	private String workingRenderer = null;
	private String recordsDir = null;
	private String urlPath = null;
	private FrameworkConfigReader configReader = null;
	private XMLValidator validator = null;
	private boolean initialized = false;


	/**
	 *  Constructor for the MetaDataFramework object given the path to a config
	 *  file for the particular framework (e.g., "adn"). The docRoot parameter
	 *  enables expanding of partial paths into absolute paths.
	 *
	 * @param  configFilePath  Description of the Parameter
	 * @param  docRoot         The context document root as obtainied by calling
	 *      getServletContext().getRealPath("/");
	 */
	public MetaDataFramework(String configFilePath, String docRoot) {
		this(new File(configFilePath), docRoot);
	}



	/**
	 *  Constructor for the MetaDataFramework object
	 *
	 * @param  configFile  Description of the Parameter
	 * @param  docRoot     path to the application context
	 */
	public MetaDataFramework(File configFile, String docRoot) {
		try {
			this.configReader = new FrameworkConfigReader(configFile);
		} catch (Exception e) {
			prtlnErr("Initialize ERROR: " + e.getMessage());
		}
		this.docRoot = docRoot;
		this.init();
	}


	/**
	 *  Constructor for the MetaDataFramework object
	 *
	 * @param  configReader  Description of the Parameter
	 */
	public MetaDataFramework(FrameworkConfigReader configReader) {
		this(configReader, null);
	}


	/**
	 *  Constructor for the MetaDataFramework object
	 *
	 * @param  configReader  Description of the Parameter
	 * @param  docRoot       Description of the Parameter
	 */
	public MetaDataFramework(FrameworkConfigReader configReader, String docRoot) {
		this.configReader = configReader;
		this.docRoot = docRoot;
		this.init();
		prtln("MetaDataFramework created with docRoot: " + docRoot + " and xmlFormat: " + getXmlFormat());
	}


	/**  Description of the Method */
	public void init() {
		try {
			getPageList(); // just to make sure the config can be read
			initialized = true;
		} catch (Exception e) {
			prtlnErr("Trouble reading from config file (" + configReader.getSourcePath() + ")");
		}

		String vocabLayoutsURI = this.configReader.getVocabLayoutURI();
		if (vocabLayoutsURI != null && vocabLayoutsURI.trim().length() > 0) {
			try {
				this.vocabLayouts = new VocabLayoutConfig(vocabLayoutsURI);
				this.vocabLayouts.init();
			} catch (Throwable t) {
				prtlnErr("ERROR: could not instantiate vocabLayouts: " + t.getMessage());
			}
		}
	}

	/**
	* check the vocabLayoutConfig paths to make sure they are legal schema paths ...
	*/
	public List validateVocabLayoutPaths () {
		List errors = new ArrayList();
		if (this.schemaHelper != null && this.vocabLayouts != null) {
			for (Iterator i=this.vocabLayouts.getAllVocabLayout().iterator();i.hasNext();) {
				VocabLayout vocabLayout = (VocabLayout)i.next();
				// prtln (vocabLayout.getPath());
				if (this.getSchemaHelper().getSchemaNode(vocabLayout.getPath()) == null) {
					errors.add ("VocabLayout (" + vocabLayout.getSource() + ") has a BOGUS XPATH (" +
						   vocabLayout.getPath() + ")");
				}
			}
		}
		return errors;
	}
	
	/**
	* check the fieldInfo paths to make sure they are legal schema paths ...
	*/
	public List validateFieldInfoPaths () {
		List errors = new ArrayList();
		FieldInfoMap fieldInfoMap = this.getFieldInfoMap();
		if (this.schemaHelper != null && fieldInfoMap != null) {
			for (Iterator i=fieldInfoMap.getAllFieldInfo().iterator();i.hasNext();) {
				FieldInfoReader reader = (FieldInfoReader)i.next();
				String path = null;
				try {
					path = reader.getPath();
				} catch (Exception e) {
					prtlnErr ("could not verify path for fieldInfo at " + reader.getSource());
					continue;
				}
				if (this.schemaHelper.getSchemaNode (path) == null) {
					errors.add ("FieldsFile (" + reader.getSource() + 
							  ") has a BOGUS XPATH (" + path + ")");
				}
			}
		}
		return errors;
	}

	/**  NOT YET DOCUMENTED */
	public void refresh() {
		this.getConfigReader().refresh();
		this.initialized = false;
	}


	/**
	 *  Gets the initialized attribute of the MetaDataFramework object
	 *
	 * @return    The initialized value
	 */
	public boolean isInitialized() {
		return this.initialized;
	}


	private VocabLayoutConfig vocabLayouts = null;


	/**
	 *  Gets the vocabLayouts attribute of the MetaDataFramework object
	 *
	 * @return    The vocabLayouts value
	 */
	public VocabLayoutConfig getVocabLayouts() {
		return this.vocabLayouts;
	}


	/**
	 *  Gets the configReader attribute of the MetaDataFramework object
	 *
	 * @return    The configReader value
	 */
	public FrameworkConfigReader getConfigReader() {
		return configReader;
	}


	/**
	 *  update config file with current values
	 *
	 * @exception  Exception  Description of the Exception
	 */
	public void writeProps()
		 throws Exception {
		configReader.flush();
	}


	/**
	 *  Gets the docRoot attribute of the MetaDataFramework object
	 *
	 * @return    The docRoot value
	 */
	public String getDocRoot() {
		return docRoot;
	}


	/**
	 *  used by stand-alone AutoForm to explicitly set docRoot
	 *
	 * @param  docRoot  The new docRoot value
	 */
	public void setDocRoot(String docRoot) {
		this.docRoot = docRoot;
	}


	/**
	 *  Gets the pageList attribute of the MetaDataFramework object
	 *
	 * @return    The pageList value
	 */
	public PageList getPageList() {
		return configReader.getPageList();
	}


	/**
	 *  Gets the fieldInfoMap attribute of the MetaDataFramework object
	 *
	 * @return    The fieldInfoMap value
	 */
	public FieldInfoMap getFieldInfoMap() {
		return configReader.getFieldInfoMap();
	}


	/**
	 *  Gets the FieldInfo for the given xpath
	 *
	 * @param  xpath  Description of the Parameter
	 * @return        The fieldInfo value
	 */
	public FieldInfoReader getFieldInfo(String xpath) {
		String key = RendererHelper.normalizeXPath(xpath);
		return getFieldInfoMap().getFieldInfo(key);
	}


	/**
	 *  Return configured value for initialFieldCollapse for this xpath
	 *
	 * @param  xpath  NOT YET DOCUMENTED
	 * @return        The initialFieldCollapse value
	 */
	public String getInitialFieldCollapse(String xpath) {
		String key = RendererHelper.normalizeXPath(xpath);
		if (this.getSchemaPathMap().getInitialFieldCollapsePaths().contains(key)) {
			SchemaPath schemaPath = this.getSchemaPathMap().getPathByPath(key);
			return schemaPath.initialFieldCollapse;
		}
		else
			return null;
	}


	/**
	 *  Gets the schemaPathMap for this framework.
	 *
	 * @return    The schemaPathMap value
	 */
	public SchemaPathMap getSchemaPathMap() {
		return configReader.getSchemaPathMap();
	}


	/**
	 *  Gets the userInfo been for this framework, which specifies xpaths that can
	 *  be populated with attributes from the SessionUser object, such as name,
	 *  institution, and email.
	 *
	 * @return    The userInfo object derived from the framework configuration
	 */
	public UserInfo getUserInfo() {
		return configReader.getUserInfo();
	}


	/**
	 *  Returns a list of xpaths for which MUI Groups files exist for this
	 *  framework.
	 *
	 * @param  vocab     The new muiGroups value
	 * @param  audience  The new muiGroups value
	 * @param  language  The new muiGroups value
	 */
	public void setMuiGroups(MetadataVocab vocab, String audience, String language) {
		// prtln ("setMuiGroups() for " + getXmlFormat());
		muiGroups = new ArrayList();
		Iterator xpaths = getSchemaHelper().getSchemaNodeMap().getKeys().iterator();
		while (xpaths.hasNext()) {
			String xpath = (String) xpaths.next();
			if (vocab.getVocabNode(getXmlFormat(), audience, language, xpath, null) != null) {
				muiGroups.add(xpath);
				//prtln ("\t" + xpath);
			}
		}
	}


	/**
	 *  Gets the muiGroups attribute of the MetaDataFramework object
	 *
	 * @return    The muiGroups value
	 */
	public List getMuiGroups() {
		if (muiGroups == null)
			return new ArrayList();
		else
			return muiGroups;
	}


	/**
	 *  Gets the standardsManager attribute of the MetaDataFramework object
	 *
	 * @return    The standardsManager value
	 */
	public StandardsManager getStandardsManager() {
		return standardsManager;
	}

	/**
	 *  Sets the standardsManager attribute of the MetaDataFramework object (called
	 *  by loadSchemaHelper)
	 *
	 * @param  standardsManager  The new standardsManager value
	 */
	public void setStandardsManager(StandardsManager standardsManager) {
		this.standardsManager = standardsManager;
	}


	/**
	 *  Gets the renderer that will create the editor for records of this
	 *  framework.
	 *
	 * @return    The renderer value
	 */
	public String getRenderer() {
		return configReader.getRenderer();
	}


	/**
	 *  Sets the renderer that will create the editor for records of this
	 *  framework.
	 *
	 * @param  r  The new renderer value
	 */
	public void setRenderer(String r) {
		configReader.setRenderer(r);
	}


	/**
	 *  Gets the bestPracticesLabel attribute of the MetaDataFramework object
	 *
	 * @return    The bestPracticesLabel value
	 */
	public String getBestPracticesLabel() {
		return configReader.getBestPracticesLabel();
	}


	/**
	 *  Sets the bestPracticesLabel attribute of the MetaDataFramework object
	 *
	 * @param  r  The new bestPracticesLabel value
	 */
	public void setBestPracticesLabel(String r) {
		configReader.setBestPracticesLabel(r);
	}


	/**
	 *  Gets the workingSchemaURI attribute (pointing to the location of the last
	 *  successfully loaded schema) of the MetaDataFramework object.
	 *
	 * @return    The workingSchemaURI value
	 */
	public String getWorkingSchemaURI() {
		return workingSchemaURI;
	}


	/**
	 *  Sets the workingSchemaURI attribute of the MetaDataFramework object
	 *
	 * @param  uri  The new workingSchemaURI value
	 */
	public void setWorkingSchemaURI(String uri) {
		workingSchemaURI = uri;
	}


	/**
	 *  Gets the workingRenderer attribute of the MetaDataFramework object
	 *
	 * @return    The workingRenderer value
	 */
	public String getWorkingRenderer() {
		return workingRenderer;
	}


	/**
	 *  Sets the workingRenderer attribute of the MetaDataFramework object
	 *
	 * @param  renderer  The new workingRenderer value
	 */
	public void setWorkingRenderer(String renderer) {
		workingRenderer = renderer;
	}


	/**
	 *  Gets the recordsDir attribute of the MetaDataFramework object
	 *
	 * @return    The recordsDir value
	 */

	public String getRecordsDir() {
		String dir = configReader.getRecordsDir();
		if (dir != null && dir.trim().length() > 0) {
			return GeneralServletTools.getAbsolutePath(dir, docRoot);
		}
		else {
			prtln("getRecordsDir returning nothing");
			return "";
		}
	}


	/**
	 *  Sets the recordsDir attribute of the MetaDataFramework object
	 *
	 * @param  dir  The new recordsDir value
	 */
	public void setRecordsDir(File dir) {
		recordsDir = dir.toString();
		// prtln ("\n\n** calling configReader.setRecordsDir with " + dir.toString() + "**\n\n");
		configReader.setRecordsDir(recordsDir);
	}


	/**
	 *  Sets the recordsDir attribute of the MetaDataFramework object
	 *
	 * @param  dir  The new recordsDir value
	 */
	public void setRecordsDir(String dir) {
		recordsDir = dir;
		configReader.setRecordsDir(dir);
	}


	/**
	 *  Gets the schemaHelper attribute of the MetaDataFramework object
	 *
	 * @return    The schemaHelper value
	 */
	public SchemaHelper getSchemaHelper() {
		return schemaHelper;
	}


	/**
	 *  Sets the schemaHelper attribute of the MetaDataFramework object
	 *
	 * @param  schemaHelper  The new schemaHelper value
	 */
	public void setSchemaHelper(SchemaHelper schemaHelper) {
		this.schemaHelper = schemaHelper;
	}


	/**
	 *  Load a SchemaHelper instance for this framework. Necessary before calls to
	 *  renderEditorPages
	 *
	 * @exception  Exception  Description of the Exception
	 */
	public void loadSchemaHelper()
		 throws Exception {
		// prtln("loadSchemaHelper()");
		// prtln (" ... rootElementName (before loading): " + getRootElementName());
		try {
			URI uri = new URI(getSchemaURI());
			String scheme = uri.getScheme();
			String errorMsg = null;

			// is URI a url or file??
			if (scheme.equals("file")) {
				String path = uri.getPath();
				File schemaFile = new File(path);

				if (!schemaFile.exists()) {
					errorMsg = "schemaFile not found at " + schemaFile.toString();
					prtlnErr(errorMsg);
					throw new Exception(errorMsg);
				}
				prtln("reading schema from file (" + schemaFile.toString() + ")");
				setSchemaHelper(new SchemaHelper(schemaFile, getRootElementName()));
				// prtln(" ... schema read");
			}
			else if (scheme.equals("http")) {
				URL schemaURL = uri.toURL();
				prtln("reading schema from net: " + schemaURL.toString());
				setSchemaHelper(new SchemaHelper(schemaURL, getRootElementName()));
				// prtln(" ... schema read");
			}
			else {
				errorMsg = "unrecognized uri scheme: " + scheme;
				prtlnErr(errorMsg);
				throw new Exception(errorMsg);
			}

			// prtln ("after loading: schemaHelper.getRootElementName: " + getSchemaHelper().getRootElementName());
			/*
			issues:
			1 - for frameworks where we have to "assign" a namespace prefix to the rootElement (e.g. lead),
			we can't give the prefix in the config, but after schema is read, the prefix is known.

			however, the editor has issues when we change the rootElementName here ....
			(document these issues ...). (NOTE: as it is now, supplying the prefix in this case
			chokes schemaHelper, but the metadata editor needs it.)

			2 - rootElementName is not required for unambigous cases, but if it is not known to framework-
			config, then we HAVE to assign it HERE so it will be known to the framework.
			*/
			// schemaHelper adds namespace prefix, which is required by metadata editor
			// setRootElementName (getSchemaHelper().getRootElementName());

			// if rootElementName was not specified in framework-config, obtain it from schemaHelper
			if (getRootElementName() == null || getRootElementName().trim().length() == 0) {
				setRootElementName(getSchemaHelper().getRootElementName());
				prtln("set RootElementName to: " + getRootElementName());
			}

			// set the read only paths from the framework config
			SchemaPathMap schemaPathMap = getSchemaPathMap();
			if (schemaPathMap != null) {
				for (Iterator i = schemaPathMap.getReadOnlyPaths().iterator(); i.hasNext(); ) {
					String readOnlyPath = (String) i.next();
					schemaHelper.setSchemaNodeReadOnly(readOnlyPath);
				}
			}
			else {
				prtlnErr("WARNING: schemaPathMap not found");
			}

		} catch (SchemaHelperException she) {
			throw new Exception(she.getMessage());
		} catch (Exception e) {
			throw new Exception(e.getMessage());
		} catch (Throwable t) {
			String errorMsg;
			if (t.getMessage() == null) {
				errorMsg = "Unable to load Schema for unknown reason";
			}
			else {
				errorMsg = "Unable to load Schema: " + t.getMessage();
			}
			prtlnErr(errorMsg);
			throw new Exception(errorMsg);
		}

		workingSchemaURI = getSchemaURI();
		workingRenderer = getRenderer();
	}



	/**
	 *  Gets the rootElementName attribute of the MetaDataFramework.
	 *
	 * @return    The rootElementName value
	 */
	public String getRootElementName() {
		return configReader.getRootElementName();
	}


	/**
	 *  Sets the rootElementName attribute of the MetaDataFramework object
	 *
	 * @param  name  The new rootElementName value
	 */
	public void setRootElementName(String name) {
		configReader.setRootElementName(name);
	}


	/**
	 *  Gets the schemaFile attribute of the MetaDataFramework object
	 *
	 * @return    The schemaFile value
	 */
	public String getSchemaURI() {
		String uriStr = configReader.getSchemaURI();

		try {
			if (uriStr == null || uriStr.trim().length() == 0) {
				throw new Exception("configReader returned null or empty string");
			}

			URI uri = new URI(uriStr);
			String scheme = uri.getScheme();
			String errorMsg = null;

			if (!uri.isAbsolute()) {

				// if we are running on a Windows platform, convert docRoot
				// to Unix to be compatable with the uri string, which presumably will
				// be expressed as a unix path.
				if (File.separator.equals("\\")) {
					docRoot = FindAndReplace.replace(docRoot, "\\", "/", false);
				}

				String path = GeneralServletTools.getAbsolutePath(uriStr, docRoot);

				File f = new File(path);
				if (!f.exists()) {
					throw new Exception("file not found at " + path);
				}
				else {
					uri = f.toURI();
				}

				uriStr = uri.toString();
			}

			return uriStr;
		} catch (Exception e) {
			prtlnErr("getSchemaURI error: " + e.getMessage());
			e.printStackTrace();
		}

		return configReader.getSchemaURI();
	}


	/**
	 *  Gets the validator attribute of the MetaDataFramework object
	 *
	 * @return    The validator value
	 */
	public XMLValidator getValidator() {
		// prtln ("getValidator() for " + this.getXmlFormat());
		if (validator == null) {
			try {
				String schemaURI = this.getSchemaURI();
				// prtln ("   ... instantiating (" + schemaURI + ")");
				if (schemaURI == null)
					throw new Exception("schemaURI not initialized");
				validator = new XMLValidator(new URI(schemaURI));
			} catch (Throwable t) {
				prtlnErr("WARNING: validator not instantiated for " + this.getXmlFormat() + " framework");
				prtlnErr(t.getMessage());
			}
		}
		return validator;
	}


	/**  NOT YET DOCUMENTED */
	public void resetValidator() {
		this.validator = null;
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  record  NOT YET DOCUMENTED
	 * @return         NOT YET DOCUMENTED
	 */
	public String validateRecord(Object record) {
		XMLValidator validator = this.getValidator();
		if (validator == null) {
			prtln("validator not found for \"" + this.getXmlFormat() + "\"");
			return Constants.UNKNOWN_VALIDITY;
		}

		if (record instanceof String) {
			return validator.validateString((String) record);
		}
		else if (record instanceof File) {
			return validator.validateFile((File) record);
		}
		else {
			prtlnErr("record type not recognized by validator");
			return Constants.UNKNOWN_VALIDITY;
		}
	}


	/*
	* does this framework specify a uniqueUrl? currently, to specify a uniqueUrl, the config
	* file must define a path must named "url" and the path must have a "valueType" attribute of "uniqueUrl".
	*/
	/**
	 *  Gets the uniqueUrlPath attribute of the MetaDataFramework object
	 *
	 * @return    The uniqueUrlPath value
	 */
	public String getUniqueUrlPath() {
		SchemaPath schemaPath = getNamedSchemaPath("url");
		if (schemaPath == null) {
			// prtln("url schemaPath not found ... returning");
			return null;
		}

		if (schemaPath.valueType == null || !schemaPath.valueType.equals("uniqueUrl")) {
			// prtln("framework does not specify uniqueUrl");
			return null;
		}
		return schemaPath.xpath;
	}


	/**
	 *  Gets the namedSchemaPath attribute of the MetaDataFramework object
	 *
	 * @param  pathName  Description of the Parameter
	 * @return           The namedSchemaPath value
	 */
	public String getNamedSchemaPathXpath(String pathName) {
		SchemaPath schemaPath = getNamedSchemaPath(pathName);
		if (schemaPath == null) {
			return null;
		}
		else {
			return schemaPath.xpath;
		}
	}


	/**
	 *  Gets the namedSchemaPath attribute of the MetaDataFramework object
	 *
	 * @param  pathName  Description of the Parameter
	 * @return           The namedSchemaPath value
	 */
	public SchemaPath getNamedSchemaPath(String pathName) {
		return getSchemaPathMap().getPathByName(pathName);
	}


	/**
	 *  Returns normalized xpaths specified in framework configuration as "url
	 *  paths".
	 *
	 * @return    A List of urlPaths value (never null)
	 */
	public List getUrlPaths() {
		List urlPathTypes = Arrays.asList(new String[]{"url", "uniqueUrl"});
		List xpaths = new ArrayList();
		Iterator iterator = getSchemaPathMap().getSchemaPathsByValueTypes(urlPathTypes).iterator();
		while (iterator.hasNext()) {
			xpaths.add(((SchemaPath) iterator.next()).xpath);
		}
		return xpaths;
	}


	/**
	 *  Gets the idPath attribute of the MetaDataFramework object
	 *
	 * @return    The idPath value
	 */
	public String getIdPath() {
		return getNamedSchemaPathXpath("id");
	}


	/**
	 *  Gets the dateCreatedPath attribute of the MetaDataFramework object
	 *
	 * @return    The dateCreatedPath value
	 */
	public String getDateCreatedPath() {
		return getNamedSchemaPathXpath("dateCreated");
	}


	/**
	 *  Gets the urlPath attribute of the MetaDataFramework object
	 *
	 * @return    The urlPath value
	 */
	public String getUrlPath() {
		return getNamedSchemaPathXpath("url");
	}


	/**
	 *  Extracts the URL from the provided record using the "urlPath" configured
	 *  SchemaPath for this framework.
	 *
	 * @param  itemRecord     NOT YET DOCUMENTED
	 * @return                The recordUrl value
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public String getRecordUrl(Document itemRecord) throws Exception {
		String urlPath = this.getUrlPath();
		if (urlPath == null) {
			String msg = "item framework (" + this.getName() + ") configuration ";
			msg += "does not define a URL path";
			throw new Exception(msg);
		}

		if (!this.getSchemaHelper().getNamespaceEnabled()) {
			itemRecord = Dom4jUtils.localizeXml(itemRecord);
		}

		String resourceUrl = null;
		try {
			resourceUrl = itemRecord.selectSingleNode(urlPath).getText();
		} catch (Throwable t) {
			// prtln ("getRecordUrl did not find a value at urlPath (" + urlPath + ")");
		}
		return resourceUrl;
	}


	/**
	 *  Sets the schemaURI attribute of the MetaDataFramework object
	 *
	 * @param  uri  The new schemaURI value
	 */
	public void setSchemaURI(String uri) {
		configReader.setSchemaURI(uri);
	}


	/**
	 *  Gets the directory to which metadata editor pages are written.<p>
	 *
	 *  E.g., for "adn" format, the autoFormDir would be ${docRoot}/editor/adn.
	 *
	 * @return    The autoFormDir value
	 */
	public String getAutoFormDir() {
		String sep = Files.getFileSeparatorStr();
		return this.docRoot + sep + "editor" + sep + getXmlFormat();
	}


	/**
	 *  Gets the xmlFormat attribute of the MetaDataFramework object
	 *
	 * @return    The xmlFormat value
	 */
	public String getXmlFormat() {
		return configReader.getXmlFormat();
	}


	/**
	 *  Gets the version attribute of the MetaDataFramework object
	 *
	 * @return    The version value
	 */
	public String getVersion() {
		String version = null;
		SchemaHelper sh = this.getSchemaHelper();
		if (sh != null)
			version = sh.getVersion();
		return (version == null) ? "" : version;
	}


	/**
	 *  Gets the name attribute of the MetaDataFramework.
	 *
	 * @return    The name value
	 */
	public String getName() {
		String ret = configReader.getName();
		if (ret != null && ret.trim().length() > 0) {
			return ret;
		}
		else {
			return getXmlFormat();
		}
	}


	/**
	 *  Gets the baseRenderLevel attribute of the MetaDataFramework object. The
	 *  baseRenderLevel tells the renderer at which level of the schema to begin
	 *  rendering. This level is determined by the way the schema is split into
	 *  pages. If there is a single page the baselevel is 2 (skipping the root
	 *  element which is level 1). if the editor is split into pages representing
	 *  the next level of the schema (as is the case with ADN) then the base level
	 *  is 3
	 *
	 * @return    The baseRenderLevel value
	 */
	public int getBaseRenderLevel() {
		return configReader.getBaseRenderLevel();
	}


	/**
	 *  Sets the baseRenderLevel attribute of the MetaDataFramework object
	 *
	 * @param  level  The new baseRenderLevel value
	 */
	public void setBaseRenderLevel(int level) {
		/* properties.setProperty("baseRenderLevel", Integer.toString(level)); */
		configReader.setBaseRenderLevel(level);
	}


	/**
	 *  Gets the discussionURL attribute of the MetaDataFramework object. Used to
	 *  create a link at the top of all the editor pages to a discussion page
	 *
	 * @return    The discussionURL value
	 */
	public String getDiscussionURL() {
		return configReader.getDiscussionURL();
	}


	/**
	 *  Sets the discussionURL attribute of the MetaDataFramework object
	 *
	 * @param  s  The new discussionURL value
	 */
	public void setDiscussionURL(String s) {
		configReader.setDiscussionURL(s);
	}


	/**
	 *  Gets the rebuildOnStart attribute of the MetaDataFramework object
	 *
	 * @return    The rebuildOnStart value
	 */
	public boolean getRebuildOnStart() {
		return configReader.getRebuildOnStart();
	}


	/**
	 *  Sets the rebuildOnStart attribute of the MetaDataFramework object
	 *
	 * @param  bool  The new rebuildOnStart value
	 */
	public void setRebuildOnStart(boolean bool) {
		setRebuildOnStart(bool);
	}


	/**
	 *  Description of the Method
	 *
	 * @param  doc               Description of the Parameter
	 * @param  schemaPaths       Description of the Parameter
	 * @param  id                Description of the Parameter
	 * @param  collectionConfig  Description of the Parameter
	 * @return                   Description of the Return Value
	 * @exception  Exception     Description of the Exception
	 */
	private Document populateFields(Document doc, List schemaPaths, String id, CollectionConfig collectionConfig)
		 throws Exception {
		return populateFields(doc, schemaPaths, id, collectionConfig, false);
	}


	/**
	 *  Populate a metadata record with values that are specified in framework and
	 *  collection configurations.<p>
	 *
	 *  Framework config define schemaPaths, which include a pathname, xpath and
	 *  default value for selected elements. Collections can also provide default
	 *  values for named path (obtained via getTupleValue).
	 *
	 * @param  doc               Description of the Parameter
	 * @param  schemaPaths       Description of the Parameter
	 * @param  id                Description of the Parameter
	 * @param  copyRecord        Description of the Parameter
	 * @param  collectionConfig  Description of the Parameter
	 * @return                   Description of the Return Value
	 * @exception  Exception     Description of the Exception
	 */
	private Document populateFields(Document doc,
	                                List schemaPaths,
	                                String id,
	                                CollectionConfig collectionConfig,
	                                boolean copyRecord)
		 throws Exception {
		SchemaHelper sh = getSchemaHelper();
		if (sh == null) {
			throw new Exception("populateFields requires schemaHelper");
		}

		DocMap docMap = new DocMap(doc, sh);

		if (schemaPaths == null || schemaPaths.size() == 0) {
			prtln("no schemaPaths defined for " + this.getXmlFormat() + " xmlFormat");
			return doc;
		}

		for (Iterator i = schemaPaths.iterator(); i.hasNext(); ) {
			SchemaPath schemaPath = (SchemaPath) i.next();
			String pathName = schemaPath.pathName;
			String value = null;

			/*
				- if the path refers to a repeating node, we want to eliminate all but one of the siblings
				- if the path refers to a complexType (contains children rather than a textual element)
					we want to empty it
				--> we first remove all nodes at the specified xpath
					- if the specified node is a SimpleType, a new element will be created by docMap.smartPut
					- otherwise (the node ComplexType), the user creates a new one in the copied record
			*/

			SchemaNode schemaNode = schemaHelper.getSchemaNode(schemaPath.xpath);
			if (schemaNode == null) {
				prtln("unable to process schemapath for " + schemaPath.xpath + ": schemaNode not found");
				continue;
			}
			if (schemaNode.isElement()) {
				try {
					docMap.removeSiblings(schemaPath.xpath);
				} catch (Exception e) {}
			}

			if (schemaNode == null)
				continue;
			if (schemaNode.getTypeDef().isComplexType() &&
				!schemaNode.isDerivedTextOnlyModel()) {
				continue;
			}

			if (pathName.equals("id")) {
				value = id;
				if (value == null) {
					value = "ID GOES HERE";
				}
			}
			else if (pathName.equals("title") && copyRecord) {
				value = "COPIED RECORD";
			}
			else if (schemaPath.valueType != null && schemaPath.valueType.equals("date")) {
				// as of 2/2/05 we are using the simple yyyy-MM-dd date format as default format for all date fields
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
				value = sdf.format(new Date());
			}
			else if (schemaPath.valueType != null && schemaPath.valueType.equalsIgnoreCase("utcDate")) {
				value = SchemEditUtils.utcDateFormat.format(new Date());
			}
			else if (schemaPath.valueType != null &&
				schemaPath.valueType.equals("collectionInfo") &&
				collectionConfig != null) {

				value = collectionConfig.getTupleValue(pathName);
				if (value == null || value.trim().length() == 0) {
					prtln("skipping empty tuple value");
					continue;
				}
			}

			// if a value has not yet been assigned, set with default if possible, or derive a
			// value based on the schemaPath
			if (value == null) {
				value = schemaPath.defaultValue;
				if (value == null) {
					value = schemaPath.pathName + "_goes_here";
				}
			}

			try {
				docMap.smartPut(schemaPath.xpath, value);
			} catch (Exception e) {
				throw new Exception("smartPut failed with " + schemaPath.xpath + ": " + e.getMessage());
			}
		}

		return docMap.getDocument();
	}


	/**
	 *  Populate user-related fields as configured in framework config. Only called
	 *  for NEW records (including copied records).
	 *
	 * @param  doc            NOT YET DOCUMENTED
	 * @param  user           NOT YET DOCUMENTED
	 * @return                NOT YET DOCUMENTED
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	private Document populateUserFields(Document doc, User user) throws Exception {
		// prtln ("populateUserFields");
		UserInfo userInfo = this.getUserInfo();

		/*
			Check for configuration errors
			NOTE: this field should be REQUIRED in framework config schema
		*/
		if (userInfo != null && userInfo.autoPopulate == null)
			throw new Exception("framework configuration error for user info: \"autoPopulate\" field not populated");

		if (userInfo == null ||
			!userInfo.autoPopulate.equals("Record Creation") ||
			this.getSchemaHelper() == null)
			return doc;

		DocMap docMap = new DocMap(doc, this.getSchemaHelper());

		for (Iterator i = userInfo.propNameIterator(); i.hasNext(); ) {
			String propName = (String) i.next();
			String value = (String) SchemEditUtils.getAttr(user, propName);
			String xpath = userInfo.getPath(propName);
			try {
				docMap.smartPut(xpath, value);
				// prtln ("put " + value + " at " + xpath);
			} catch (Exception e) {
				throw new Exception("smartPut failed with " + xpath + ": " + e.getMessage());
			}
		}

		return doc;
	}


	/**
	 *  Copy the given xml record, inserting new values as specified by the
	 *  Collection configuration.
	 *
	 * @param  xml               xml record to be copied
	 * @param  id                id for new record
	 * @param  collectionConfig  the collection configuration
	 * @param  user              NOT YET DOCUMENTED
	 * @return                   new Document
	 * @exception  Exception     Description of the Exception
	 */
	/* 	public Document copyRecord(String xml, String id, CollectionConfig collectionConfig)
		 throws Exception {
			 return copyRecord (xml, id, collectionConfig, null);
		 } */
	/**
	 *  Copy the given xml record, inserting new values as specified by the
	 *  Collection configuration.
	 *
	 * @param  xml               xml record to be copied
	 * @param  id                id for new record
	 * @param  collectionConfig  the collection configuration
	 * @param  user              the current user (from which to populate user
	 *      fields if so configured)
	 * @return                   new Document
	 * @exception  Exception     Description of the Exception
	 */
	public Document copyRecord(String xml, String id, CollectionConfig collectionConfig, User user)
		 throws Exception {
		// prtln("copyRecord()");
		Document doc = null;
		try {
			// do we want to localize when there are multi-namespaces???
			String newDocXml = xml;

			if (!getSchemaHelper().getNamespaceEnabled())
				newDocXml = Dom4jUtils.localizeXml(xml, getRootElementName());
			doc = Dom4jUtils.getXmlDocument(newDocXml);
			if (doc == null) {
				throw new Exception("failed to create localized copy of original record");
			}

			/*
				if we don't have a user, create a blank user so the user field values
				will be overwritten if framework is configured to autofill
			*/
			if (user == null)
				user = new User();

			List schemaPaths = getSchemaPathMap().getCopyRecordPaths();

			try {
				doc = populateFields(doc, schemaPaths, id, collectionConfig, true);
				doc = populateUserFields(doc, user);
			} catch (Throwable t) {
				t.printStackTrace();
				throw new Exception("populateFields caught error: " + t.getMessage());
			}

			// now prepare document to write to file by inserting namespace information
			doc = this.getWritableRecord(doc);

		} catch (Exception e) {
			throw new Exception("Unable to copy Record: " + e);
		}
		return doc;
	}


	/**
	 *  Returns an instance document containing only required nodes, with the "id"
	 *  field populated with provided id
	 *
	 * @param  id             id value to be inserted into the id field for created
	 *      record
	 * @return                minimal instance Document for this framework.
	 * @exception  Exception
	 */
	public Document makeMinimalRecord(String id)
		 throws Exception {
		return makeMinimalRecord(id, null);
	}


	/**
	 *  Returns an instance document containing only required nodes, with the "id"
	 *  field populated with provided id
	 *
	 * @param  id                id value to be inserted into the id field for
	 *      created record
	 * @param  collectionConfig  NOT YET DOCUMENTED
	 * @return                   minimal instance Document for this framework.
	 * @exception  Exception
	 */
	public Document makeMinimalRecord(String id, CollectionConfig collectionConfig)
		 throws Exception {
		return makeMinimalRecord(id, collectionConfig, null);
	}


	/**
	 *  Returns an instance document containing only required nodes, with the "id"
	 *  field populated with provided id and collectionConfig fields populated if
	 *  collectionConfig is provided.
	 *
	 * @param  id                id value to be inserted into the id field for
	 *      created record
	 * @param  collectionConfig  Description of the Parameter
	 * @param  user              NOT YET DOCUMENTED
	 * @return                   minimal instance Document for this framework.
	 * @exception  Exception     Description of the Exception
	 */
	public Document makeMinimalRecord(String id, CollectionConfig collectionConfig, User user)
		 throws Exception {
		Document doc = getSchemaHelper().getMinimalDocument();

		List schemaPaths = getSchemaPathMap().getMinimalRecordPaths();
		try {
			doc = populateFields(doc, schemaPaths, id, collectionConfig);
			if (user != null) {
				prtln("populating user fields");
				doc = populateUserFields(doc, user);
			}
		} catch (Exception e) {
			String errorMsg = "makeMinimalRecord error: " + e.getMessage();
			prtlnErr(errorMsg);
			SchemaUtils.showSchemaNodeMap(getSchemaHelper());
			throw new Exception(errorMsg);
		}

		return doc;
	}


	/**
	 *  Generate the pages of the editor for records of this MetaDataFramework.
	 *
	 * @exception  Exception  Description of the Exception
	 */
	public void renderEditorPages()
		 throws Exception {
		// test that dest directory for pages exists
		try {
			File file = new File(getAutoFormDir());
			if (!file.exists() && !file.mkdirs()) {
				throw new Exception("framework.autoFormDir could not be created at " + getAutoFormDir());
			}
			// AutoForm creates the editor pages
			AutoForm autoform = new AutoForm(this);
			// DcsViewRecord provides a view of the entire record for the DCS FullView page
			DcsViewRecord dcsViewRecord = new DcsViewRecord(this);
			// EditorViewRecord provides a view of the entire record for the MetaDataEditor
			EditorViewRecord editorViewRecord = new EditorViewRecord(this);
			/* autoform.setRendererClassName(getRenderer()); */
			int baseLevel = getBaseRenderLevel();
			if (baseLevel == 2) {
				// prtln("rendering as single page");
				String rootElementPath = "/" + getRootElementName();
				autoform.renderAndWrite(rootElementPath);
				dcsViewRecord.renderAndWrite(rootElementPath);
				editorViewRecord.renderAndWrite(rootElementPath);
			}
			else if (baseLevel == 3) {
				// prtln("rendering as multiple pages");
				autoform.batchRenderAndWrite();
				dcsViewRecord.batchRenderAndWrite();
				editorViewRecord.batchRenderAndWrite();
			}
			else {
				throw new Exception("renderEditorPages failed because baseRenderLevel is out of range (" + baseLevel + ")");
			}
			prtln(" ... metadata editor pages written");
		} catch (Exception e) {
			e.printStackTrace();
			throw new Exception(e.getMessage());
		} catch (Throwable t) {
			t.printStackTrace();
			throw new Exception("Unknown error: " + t.getMessage());
		}
	}


	/**
	 *  Gets a localized Document (with ampersands expanded) that is suitable for
	 *  editing.
	 *
	 * @param  file                   file containing xml Record
	 * @return                        an editable Document
	 * @exception  DocumentException  Description of the Exception
	 */
	public Document getEditableDocument(File file)
		 throws DocumentException {
		return getEditableDocument(file.getAbsolutePath());
	}


	/**
	 *  Gets a localized Document (with ampersands expanded) that is suitable for
	 *  editing.
	 *
	 * @param  filePath               path to file containing xml record.
	 * @return                        an editable Document
	 * @exception  DocumentException  Description of the Exception
	 */
	public Document getEditableDocument(String filePath)
		 throws DocumentException {
		String uriStr = new File(filePath).toURI().toString();
		// prtln ("getEditableDocument fetching: " + uriStr);
		return SchemEditUtils.getEditableDocument(uriStr, this);
	}


	/**
	 *  Gets the writableRecordXml attribute of the MetaDataFramework object
	 *
	 * @param  doc                    NOT YET DOCUMENTED
	 * @return                        The writableRecordXml value
	 * @exception  DocumentException  NOT YET DOCUMENTED
	 */
	public String getWritableRecordXml(Document doc)
		 throws DocumentException {

		Document delocalized = getWritableRecord(doc);
		return "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" + Dom4jUtils.prettyPrint(delocalized);
	}


	/**
	 *  Converts a editable Document into an xml record that can be written to
	 *  disk.<p>
	 *
	 *  The xml record returned is delocalized (namespace info added) and the
	 *  ampersands are contracted.
	 *
	 * @param  doc                    an editable Document
	 * @return                        string representation of xml record suitable
	 *      to be written to disk.
	 * @exception  DocumentException  Description of the Exception
	 */
	public Document getWritableRecord(Document doc)
		 throws DocumentException {
		try {
			String fwrkRootElementName = getRootElementName();

			String rootElementName = doc.getRootElement().getQualifiedName();
			if (!rootElementName.equals(fwrkRootElementName)) {
				throw new Exception("Root element (\"" + rootElementName +
					"\") does not match schema (expected \"" +
					fwrkRootElementName + "\")");
			}

			if (!getSchemaHelper().getNamespaceEnabled()) {

				Element root = doc.getRootElement();

				// add schemaInstance namespace if it is not already there
				if (root.getNamespaceForURI("http://www.w3.org/2001/XMLSchema-instance") == null) {
					root.addNamespace("xsi", "http://www.w3.org/2001/XMLSchema-instance");
				}

				/*
				prtln ("\nAttributes before");
				for (Iterator i = root.attributeIterator();i.hasNext();) {
					Attribute att = (Attribute)i.next();
					prtln ("\t" + att.getQualifiedName() + ": " + att.getValue());
				} */
				String targetNs = schemaHelper.getTargetNamespace();

				if (targetNs != null && targetNs.trim().length() > 0) {
					root.addAttribute("xmlns", targetNs);
					// localized records are likely to still have the schemaLocation attribute, but without a prefix
					//  - in this case we have to remove it (by setting it to null) before qualifying
					root.addAttribute("schemaLocation", null);
					root.addAttribute("xsi:schemaLocation", schemaHelper.getTargetNamespace() + " " + getSchemaURI());
				}
				else {
					String uriStr = getSchemaURI();
					// we don't want the "file:" part (scheme) in the noNamespaceSchemaLocation value
					// getSchemaURI() must return a complete URI string because it is used by schemaHelper to find
					// the actual schema file, so we chop off the "file:" part here rather than in getSchemaURI()
					try {
						URI uri = new URI(uriStr).normalize();
						if (uri.getScheme().equals("file")) {
							// replace pesky spaces in file paths
							uriStr = FindAndReplace.replace(uri.getPath(), " ", "%20", false);
						}
						root.addAttribute("xsi:noNamespaceSchemaLocation", uriStr);
					} catch (Throwable t) {
						root.addAttribute("xsi:noNamespaceSchemaLocation", getSchemaURI());
					}
				}
			}
		} catch (Exception e) {
			throw new DocumentException("getWritableRecordXml ERROR: " + e.getMessage());
		} catch (Throwable t) {
			t.printStackTrace();
			throw new DocumentException("getWritableRecordXml: unknown error");
		}

		return doc;
	}


	/**
	 *  Orders elements as specified by the framework configuration. In the config,
	 *  ordered paths have a "value" type of "ordered".
	 *
	 * @param  doc  instanceDocument containing elements to be ordered
	 */
	public void processOrderedElements(Document doc) {

		List orderedElementPaths = getSchemaPathMap().getSchemaPathsByValueType("ordered");
		// prtln(orderedElementPaths.size() + " ordered paths found");

		for (Iterator i = orderedElementPaths.iterator(); i.hasNext(); ) {
			SchemaPath schemaPath = (SchemaPath) i.next();
			ElementsOrderer.orderElements(doc, schemaPath.xpath);
		}
	}


	/**
	 *  Converts an editable Document into a writable xmlRecord (string) via {@link
	 *  #getWritableRecordXml(Document) getWritableRecordXml} and writes it to
	 *  disk.
	 *
	 * @param  doc                    an editable Document
	 * @param  file                   destination file for write operation.
	 * @exception  DocumentException  Description of the Exception
	 */
	public void writeEditableDocument(Document doc, File file)
		 throws DocumentException {
		String xmlRecord = getWritableRecordXml(doc);
		try {
			Files.writeFile(xmlRecord, file);
		} catch (IOException ioe) {
			throw new DocumentException("Could not write document to file: " + ioe.getMessage());
		}
	}


	/**
	 *  Sets the debug attribute of the MetaDataFramework class
	 *
	 * @param  bool  The new debug value
	 */
	public static void setDebug(boolean bool) {
		debug = bool;
	}


	/**
	 *  String representation for debugging
	 *
	 * @return    Description of the Return Value
	 */
	public String toString() {
		String s = "Selected MetaDataFramework Properties";
		// s += "\n formBeanName: " + formBeanName;
		s += "\n autoFormDir: " + getAutoFormDir();
//		s += "\n FormActionPath: " + getFormActionPath();
		s += "\n autoFormDir: " + getAutoFormDir();
		s += "\n baseRenderLevel: " + getBaseRenderLevel();
		// s += "\n pageTitle: " + getPageTitle();
//		s += "\n submitAction: " + getSubmitAction();
		if (getPageList() != null) {
			s += "\n\n" + getPageList().toString() + "\n";
		}
		return s;
	}


	/*
	* Ensure that required metadata elements are present, and take care of any framework-specific
	* document preprocessing (e.g., smile_item element).
	*/
	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  docMap  NOT YET DOCUMENTED
	 * @return         NOT YET DOCUMENTED
	 */
	public DocMap preprocessEditableDocument(DocMap docMap) {

		/*	all frameworks - make sure all required nodes(?? parents ??) are present in document
			( kind of the opposite of PRUNE DOCUMENT )
			- walk the schema instance doc (recursively)
			- for each element - get the schemaNode using the element path
			-- test that all required elements are in the instanceDoc
		*/
		try {
			EnsureMinimalDocument.process(docMap.getDocument(), this.getSchemaHelper());
		} catch (Throwable t) {
			prtlnErr("EnsureMinimalDocument ERROR: " + t.getMessage());
		}

		// framework specific stuff follows
		if (this.getXmlFormat().startsWith("smile_item")) {
			String pathArg = "/smileItem/costTimeMaterials/materialsList/materialsListItem";
			int existing = docMap.selectNodes(pathArg).size();
			for (int i = existing; i < 5; i++) {
				try {
					Element newElement = schemaHelper.getNewElement(pathArg);
					if (newElement == null) {
						throw new Exception("getNewElement failed");
					}
					else {
						// prtln (" ... calling docMap.addElement()");
						if (!docMap.addElement(newElement, pathArg)) {
							throw new Exception("docMap.addElement failed");
						}
					}
				} catch (Throwable t) {
					prtlnErr("could not attach element: " + t.getMessage());
				}
			}
		}
		return docMap;
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  doc  NOT YET DOCUMENTED
	 * @return      NOT YET DOCUMENTED
	 */
	public Document preprocessEditableDocument(Document doc) {
		DocMap docMap = new DocMap(doc, this.schemaHelper);
		docMap = preprocessEditableDocument(docMap);
		return docMap.getDocument();
	}


	/**  Description of the Method */
	public void destroy() {
		prtln("destroying " + getName());
		try {
			schemaHelper.destroy();
			configReader.destroy();
		} catch (Throwable e) {
			prtlnErr("destroy error: " + e.getMessage());
		}
	}


	/**
	 *  Description of the Method
	 *
	 * @param  s  Description of the Parameter
	 */
	private static void prtln(String s) {
		if (debug) {
			SchemEditUtils.prtln(s, "MetaDataFramework");
		}
	}


	private final void prtlnErr(String s) {
		System.err.println("MetaDataFramework: " + s);
	}

}

