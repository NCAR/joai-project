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
package org.dlese.dpc.schemedit.action.form;

import org.dlese.dpc.schemedit.*;
import org.dlese.dpc.schemedit.input.*;
import org.dlese.dpc.schemedit.autoform.RendererHelper;
import org.dlese.dpc.schemedit.display.CollapseBean;
import org.dlese.dpc.schemedit.display.CollapseUtils;
import org.dlese.dpc.schemedit.dcs.DcsSetInfo;
import org.dlese.dpc.schemedit.dcs.DcsDataRecord;
import org.dlese.dpc.schemedit.threadedservices.ExportingService;
import org.dlese.dpc.schemedit.vocab.FieldInfoReader;
import org.dlese.dpc.schemedit.vocab.FieldInfoMap;
import org.dlese.dpc.schemedit.vocab.layout.VocabLayoutConfig;
import org.dlese.dpc.schemedit.vocab.layout.VocabLayout;

import org.dlese.dpc.schemedit.standards.StandardsNode;
import org.dlese.dpc.schemedit.standards.CATServiceHelper;
import org.dlese.dpc.schemedit.standards.asn.AsnSuggestionServiceHelper;
import org.dlese.dpc.schemedit.standards.asn.ResQualSuggestionServiceHelper;

import org.dlese.dpc.schemedit.standards.CATServiceHelper;
import org.dlese.dpc.schemedit.standards.StandardsManager;

import org.dlese.dpc.xml.schema.*;
import org.dlese.dpc.xml.schema.compositor.*;
import org.dlese.dpc.xml.XPathUtils;
import org.dlese.dpc.xml.Dom4jUtils;
import org.dlese.dpc.xml.XMLFileFilter;
import org.dlese.dpc.serviceclients.remotesearch.reader.ADNItemDocReader;
import org.dlese.dpc.vocab.MetadataVocab;
import org.dlese.dpc.vocab.VocabNode;
import org.dlese.dpc.index.ResultDoc;
import org.dlese.dpc.index.ResultDocList;
import org.dlese.dpc.util.Files;

import org.dom4j.DocumentHelper;
import org.dom4j.DocumentException;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.Attribute;
import org.dom4j.Namespace;

import javax.servlet.ServletContext;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionError;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.util.LabelValueBean;

import java.util.*;
import java.io.*;
import java.text.*;
import java.net.*;

/**
 *  ActionForm bean for handling requests to support MetaDataEditor. Most
 *  methods acesss the {@link DocMap} attribute, which wraps the XML Document
 *  that is being edited.
 *
 * @author    ostwald
 */
public class SchemEditForm extends ActionForm {

	private static boolean debug = true;

	/**  Description of the Field */
	public static String UNSPECIFIED = "-- unspecified --";
	/**  NOT YET DOCUMENTED */
	public static String TRUE = "true";
	/**  NOT YET DOCUMENTED */
	public static String FALSE = "false";

	private MetaDataFramework framework = null;
	private CATServiceHelper suggestionServiceHelper = null;
	private String contextURL = null;
	private ADNItemDocReader adnDocReader = null;
	private String recId = "";
	private DocMap docMap = null;
	private SchemaHelper schemaHelper = null;
	private InputManager inputManager = null;

	private PageList pageList = null;
	private DcsSetInfo setInfo = null;

	private Map multiValuesCache = null;
	private ArrayList repeatingFieldsToPrune = null;
	private ArrayList closedElements = null;
	private String repeatingField = null;
	private String currentPage = null;
	private String previousPage = null;
	private String pathArg = null;
	private String guardedExitPath = null;
	private String hash = "bottom";
	private String tmpArg = null;
	private boolean forceValidation = false;

	private String collection = null;
	private String collectionName = null;
	private DcsDataRecord dcsDataRecord = null;

	private String metadata = null;
	private String pageTitle = null;
	private String editorConfig = null;
	private String discussionURL = null;

	private File recordsDir = null;
	private Record[] records = null;
	private Map savedContent = null;

	private ResultDoc resultDoc = null;
	private CollapseBean collapseBean = null;
	private ResultDocList similarUrlRecs = null;
	private ResultDocList duplicateUrlRecs = null;
	private List dups = null;
	private List sims = null;
	private String validatedUrl = null;

	private String asyncJason = null;


	/**  Constructor */
	public SchemEditForm() {
		multiValuesCache = new HashMap();
		repeatingFieldsToPrune = new ArrayList();
	}


	/**  NOT YET DOCUMENTED */
	public void clear() {
		recId = null;
		docMap = null;
		savedContent = null;
		suggestionServiceHelper = null;
	}

	// --------------------------- Suggestion Service Methods ---------------------------

	// used by dynaAsnStandardsLayout to keep track of current standards document
	private String currentStdDocKey = null;


	/**
	 *  Gets the currentStdDocKey attribute of the SchemEditForm object
	 *
	 * @return    The currentStdDocKey value
	 */
	public String getCurrentStdDocKey() {
		return this.currentStdDocKey;
	}


	/**
	 *  Sets the currentStdDocKey attribute of the SchemEditForm object
	 *
	 * @param  key  The new currentStdDocKey value
	 */
	public void setCurrentStdDocKey(String key) {
		this.currentStdDocKey = key;
	}


	/**
	 *  Gets the servletContext attribute of the SchemEditForm object
	 *
	 * @return    The servletContext value
	 */
	public ServletContext getServletContext() {
		return this.servlet.getServletContext();
	}


	/**
	 *  Gets the suggestionServiceHelper attribute of the SchemEditForm object
	 *
	 * @return    The suggestionServiceHelper value
	 */
	public CATServiceHelper getSuggestionServiceHelper() {
		return this.suggestionServiceHelper;
	}


	/**
	 *  Sets the suggestionServiceHelper attribute of the SchemEditForm object
	 *
	 * @param  helper  The new suggestionServiceHelper value
	 */
	public void setSuggestionServiceHelper(CATServiceHelper helper) {
		this.suggestionServiceHelper = helper;
	}

	// ----  end suggestion service methods -------

	// ------ MUI-related stuff -----------------
	private MetadataVocab vocab = null;
	private String vocabField = null;
	private String vocabInterface = null;
	private String vocabAudience = null;
	private String vocabLanguage = null;


	/**
	 *  Constructor for the setVocab object
	 *
	 * @param  vocab
	 */
	public void setVocab(MetadataVocab vocab) {
		this.vocab = vocab;
	}


	/**
	 *  Gets the vocab attribute of the SchemEditForm object
	 *
	 * @return    The vocab value
	 */
	public MetadataVocab getVocab() {
		return vocab;
	}



	/**
	 *  Sets the vocabField attribute to an xpath extracted from the proviced
	 *  paramName.
	 *
	 * @param  paramName  The new vocabField value
	 */
	public void setVocabField(String paramName) {
		String normalizedXpath = InputManager.paramNameToNormalizedXPath(paramName);
		String fieldName = XPathUtils.getNodeName(normalizedXpath);
		// prtln ("setVocabField: " + normalizedXpath);
		this.vocabField = normalizedXpath;
	}


	/**
	 *  Sets the vocabInterface attribute of the SchemEditForm object
	 *
	 * @param  vocabInterface  The new vocabInterface value
	 */
	public void setVocabInterface(String vocabInterface) {
		this.vocabInterface = vocabInterface;
	}


	/**
	 *  Sets the vocabAudience attribute of the SchemEditForm object
	 *
	 * @param  vocabAudience  The new vocabAudience value
	 */
	public void setVocabAudience(String vocabAudience) {
		this.vocabAudience = vocabAudience;
	}


	/**
	 *  Sets the vocabLanguage attribute of the SchemEditForm object
	 *
	 * @param  vocabLanguage  The new vocabLanguage value
	 */
	public void setVocabLanguage(String vocabLanguage) {
		this.vocabLanguage = vocabLanguage;
	}


	/**
	 *  Returns true if a MUI groups file exists for the provided path
	 *
	 * @param  encodedPath  a jsp-encoded xpath
	 * @return              The muiFormattable value
	 */
	public boolean getMuiFormattable(String encodedPath) {
		String xpath = XPathUtils.decodeXPath(encodedPath);
		VocabNode vNode = vocab.getVocabNode(getXmlFormat(), vocabAudience, "en-us", encodedPath, null);
		if (vNode != null)
			prtln("vNode found");
		return (vNode != null);
	}


	/**
	 *  Gets a list of VocabNodes under the current value of "vocabField". This
	 *  method uses the MUI.
	 *
	 * @return    The vocabList value
	 */
	public ArrayList getVocabList() {
		if (vocab == null) {
			prtln("Vocab is null");
			return new ArrayList();
		}

		/* 		// debugging - can we test for presense of node?
		VocabNode vNode  = vocab.getVocabNode( getXmlFormat(), vocabAudience, "en-us", vocabField, null);
		if (vNode != null) prtln ("vNode found");
		*/
		// getVocabList using MetadataVocabOPML.getVocabNodes
		ArrayList vList = getVocabList(vocab.getVocabNodes(getXmlFormat(), vocabAudience, vocabLanguage, vocabField));

		return vList;
	}


	/**
	 *  Flattens the hierarchy of the given vocab list, and returns it as a single
	 *  arrayList.This method uses the MUI.
	 *
	 * @param  vocList
	 * @return          The vocabList value
	 */
	private ArrayList getVocabList(ArrayList vocList) {
		ArrayList ret = new ArrayList();
		for (int i = 0; i < vocList.size(); i++) {
			VocabNode addNode = (VocabNode) vocList.get(i);
			ArrayList sublist = addNode.getSubList();
			ret.add(addNode);
			if (sublist.size() > 0) {
				ret.addAll(getVocabList(sublist));
			}
		}
		return ret;
	}

	// ------ end of MUI-related stuff -----------------

	/**
	 *  Get VocabLayout instance for the current vocabField.
	 *
	 * @return    The vocabLayout value
	 */
	public VocabLayout getVocabLayout() {
		String xpath = this.vocabField;
		VocabLayout vocabLayout = null;
		try {
			if (xpath == null)
				throw new Exception("xpath not obtained from vocabField");

			VocabLayoutConfig layouts = this.framework.getVocabLayouts();
			if (layouts == null)
				throw new Exception("No Layouts found for \"" + this.getXmlFormat() + "\" framework");

			if (!layouts.hasVocabLayout(xpath))
				throw new Exception("layout not found for " + xpath);

			vocabLayout = layouts.getVocabLayout(xpath);
		} catch (Throwable t) {
			prtln("getVocabLayout ERROR: " + t.getMessage());
		}
		return vocabLayout;
	}


	/**
	 *  Get top-level vocabLayoutNodes for the current vocabField.
	 *
	 * @return    The vocabLayoutNodes value
	 */
	public List getVocabLayoutNodes() {
		List layoutNodes = new ArrayList();
		try {
			VocabLayout vocabLayout = this.getVocabLayout();
			if (vocabLayout == null)
				throw new Exception("vocabLayout not found");
			layoutNodes = vocabLayout.getLayoutNodes();
		} catch (Throwable t) {
			prtln("getVocabLayoutNodes ERROR: " + t.getMessage());
		}
		return layoutNodes;
	}


	/**
	 *  Gets the asyncJason attribute of the SchemEditForm object
	 *
	 * @return    The asyncJason value
	 */
	public String getAsyncJason() {
		return this.asyncJason;
	}


	/**
	 *  Sets the asyncJason attribute of the SchemEditForm object
	 *
	 * @param  json  The new asyncJason value
	 */
	public void setAsyncJason(String json) {
		this.asyncJason = json;
	}


	/**
	 *  Gets the inputManager attribute of the SchemEditForm object
	 *
	 * @return    The inputManager value
	 */
	public InputManager getInputManager() {
		return this.inputManager;
	}


	/**
	 *  Gets the framework attribute of the SchemEditForm object
	 *
	 * @return    The framework value
	 */
	public MetaDataFramework getFramework() {
		return framework;
	}


	/**
	 *  Sets the framework attribute of the SchemEditForm object
	 *
	 * @param  framework  The new framework value
	 */
	public void setFramework(MetaDataFramework framework) {
		this.framework = framework;
		if (this.framework != null)
			this.schemaHelper = framework.getSchemaHelper();
	}


	/**
	 *  Gets the baseExportDir attribute of the SchemEditForm object
	 *
	 * @return    The baseExportDir value
	 */
	public String getBaseExportDir() {
		ExportingService es = (ExportingService) getServlet().getServletContext().getAttribute("exportingService");
		if (es != null) {
			return es.getExportBaseDir();
		}
		else {
			return "";
		}
	}


	/**
	 *  Gets the resultDoc attribute of the SchemEditForm object
	 *
	 * @return    The resultDoc value
	 */
	public ResultDoc getResultDoc() {
		return resultDoc;
	}


	/**
	 *  Sets the resultDoc attribute of the SchemEditForm object
	 *
	 * @param  resultDoc  The new resultDoc value
	 */
	public void setResultDoc(ResultDoc resultDoc) {
		this.resultDoc = resultDoc;
	}


	/**
	 *  Gets the contextURL attribute of the DCSBrowseForm object
	 *
	 * @return    The contextURL value
	 */
	public String getContextURL() {
		return contextURL;
	}


	/**
	 *  Sets the contextURL attribute of the DCSBrowseForm object
	 *
	 * @param  contextURL  The new contextURL value
	 */
	public void setContextURL(String contextURL) {
		this.contextURL = contextURL;
	}


	/**
	 *  Gets the savedContent attribute of the SchemEditForm object
	 *
	 * @return    The savedContent value
	 */
	public Map getSavedContent() {
		return savedContent;
	}


	/**
	 *  Sets the savedContent attribute of the SchemEditForm object
	 *
	 * @param  map  The new savedContent value
	 */
	public void setSavedContent(Map map) {
		savedContent = map;
	}


	/**
	 *  Gets the validatedUrl attribute of the SchemEditForm object
	 *
	 * @return    The validatedUrl value
	 */
	public String getValidatedUrl() {
		return validatedUrl;
	}


	/**
	 *  Sets the validatedUrl attribute of the SchemEditForm object
	 *
	 * @param  validatedUrl  The new validatedUrl value
	 */
	public void setValidatedUrl(String validatedUrl) {
		this.validatedUrl = validatedUrl;
	}


	/**
	 *  Gets the similarUrlRecs attribute of the SchemEditForm object
	 *
	 * @return    The similarUrlRecs value
	 */
	public ResultDocList getSimilarUrlRecs() {
		return similarUrlRecs;
	}


	/**
	 *  Sets the similarUrlRecs attribute of the SchemEditForm object
	 *
	 * @param  results  The new similarUrlRecs value
	 */
	public void setSimilarUrlRecs(ResultDocList results) {
		similarUrlRecs = results;
	}


	/**
	 *  Gets the duplicateUrlRecs attribute of the SchemEditForm object
	 *
	 * @return    The duplicateUrlRecs value
	 */
	public ResultDocList getDuplicateUrlRecs() {
		return duplicateUrlRecs;
	}


	/**
	 *  Sets the duplicateUrlRecs attribute of the SchemEditForm object
	 *
	 * @param  results  The new duplicateUrlRecs value
	 */
	public void setDuplicateUrlRecs(ResultDocList results) {
		duplicateUrlRecs = results;
	}


	/**
	 *  Gets the dups attribute of the SchemEditForm object
	 *
	 * @return    The dups value
	 */
	public List getDups() {
		if (dups == null)
			dups = new ArrayList();
		return dups;
	}


	/**
	 *  Sets the dups attribute of the SchemEditForm object
	 *
	 * @param  simDupList  The new dups value
	 */
	public void setDups(List simDupList) {
		dups = simDupList;
	}


	/**
	 *  Gets the sims attribute of the SchemEditForm object
	 *
	 * @return    The sims value
	 */
	public List getSims() {
		if (sims == null)
			sims = new ArrayList();
		return sims;
	}


	/**
	 *  Sets the sims attribute of the SchemEditForm object
	 *
	 * @param  simDupList  The new sims value
	 */
	public void setSims(List simDupList) {
		sims = simDupList;
	}


	/**
	 *  Gets the dcsDataRecord attribute of the SchemEditForm object
	 *
	 * @return    The dcsDataRecord value
	 */
	public DcsDataRecord getDcsDataRecord() {
		return dcsDataRecord;
	}


	/**
	 *  Gets the collapseBean attribute of the SchemEditForm object
	 *
	 * @return    The collapseBean value
	 */
	public CollapseBean getCollapseBean() {
		if (collapseBean == null) {
			collapseBean = new CollapseBean();
		}
		return collapseBean;
	}


	/**
	 *  Gets the setInfo attribute of the SchemEditForm object
	 *
	 * @return    The setInfo value
	 */
	public DcsSetInfo getSetInfo() {
		return setInfo;
	}


	/**
	 *  Sets the setInfo attribute of the SchemEditForm object
	 *
	 * @param  info  The new setInfo value
	 */
	public void setSetInfo(DcsSetInfo info) {
		setInfo = info;
	}


	/**
	 *  Make a node visible in the editor. First create the node if necessary and
	 *  set its displayState to OPEN. Then, if the node has a compositor, expose
	 *  the compositor members. Fintally, walk back up the node's path setting each
	 *  ancestor's displayState to OPEN (accomplished by call to getCollapseBean().exposeElement()).
	 *
	 * @param  xpath  xpath to node to be exposed
	 */
	public void exposeNode(String xpath) {
		// prtln("exposeNode with " + xpath);

		if (docMap != null && !docMap.nodeExists(xpath)) {
			// prtln("exposeNode() - node does not exist at " + xpath + " .. creating");
			try {
				docMap.createNewNode(xpath);
			} catch (Exception e) {
				prtlnErr("docMap.createNewNode: " + e.getMessage());
			}
		}

		// expand the members of compositor, if present
		if (schemaHelper != null && schemaHelper.hasCompositor(xpath)) {
			try {
				List members = schemaHelper.getCompositor(xpath).getMembers();
				for (Iterator i = members.iterator(); i.hasNext(); ) {
					CompositorMember member = (CompositorMember) i.next();
					String memberName = member.getInstanceQualifiedName();
					String memberPath = xpath + "/" + memberName;
					SchemaNode memberNode = this.schemaHelper.getSchemaNode(memberPath);

					// if this is a repeating element, we have to add indexing if there is none
					if (memberNode != null) {
						if (this.schemaHelper.isRepeatingElement(memberNode) &&
							XPathUtils.getIndex(memberPath) == 0) {
							memberPath = memberPath + "[1]";
						}
					}
					getCollapseBean().exposeElement(memberPath);
				}
			} catch (Throwable e) {
				prtlnErr("couldn't expose children: " + e.getMessage());
			}
		}
		else {
			// prtln("no compositor exists at " + xpath);
		}

		getCollapseBean().exposeElement(xpath);
	}


	/**
	 *  Sets the dcsDataRecord attribute of the SchemEditForm object
	 *
	 * @param  dataRec  The new dcsDataRecord value
	 */
	public void setDcsDataRecord(DcsDataRecord dataRec) {
		dcsDataRecord = dataRec;
	}



	/**
	 *  Sets the metadata attribute of the SchemEditForm object
	 *
	 * @param  s  The new metadata value
	 */
	public void setMetadata(String s) {
		metadata = s;
	}


	/**
	 *  Gets the metadata attribute of the SchemEditForm object
	 *
	 * @return    The metadata value
	 */
	public String getMetadata() {
		return metadata;
	}


	/**
	 *  Gets the collection attribute of the SchemEditForm object
	 *
	 * @return    The collection value
	 */
	public String getCollection() {
		return collection;
	}


	/**
	 *  Sets the collection attribute of the SchemEditForm object
	 *
	 * @param  collection  The new collection value
	 */
	public void setCollection(String collection) {
		this.collection = collection;
	}


	/**
	 *  Gets the collectionName attribute of the SchemEditForm object
	 *
	 * @return    The collectionName value
	 */
	public String getCollectionName() {
		DcsSetInfo setInfo = getSetInfo();
		if (setInfo != null) {
			return setInfo.getName();
		}
		else {
			return "";
		}
	}


	/**
	 *  Gets the discussionURL attribute of the SchemEditForm object
	 *
	 * @return    The discussionURL value
	 */
	public String getDiscussionURL() {
		return discussionURL;
	}


	/**
	 *  Sets the discussionURL attribute of the SchemEditForm object
	 *
	 * @param  url  The new discussionURL value
	 */
	public void setDiscussionURL(String url) {
		discussionURL = url;
	}


	/**
	 *  Gets the frameworkName attribute of the SchemEditForm object
	 *
	 * @return    The frameworkName value
	 */
	public String getFrameworkName() {
		return framework.getName();
	}


	/**
	 *  Gets the location of metadata records for a StandAlone Editor.
	 *
	 * @return    The recordsDir value
	 */
	public File getRecordsDir() {
		return recordsDir;
	}


	/**
	 *  Sets the recordsDir attribute of the SchemEditForm object
	 *
	 * @param  dir  The new recordsDir value
	 */
	public void setRecordsDir(File dir) {
		recordsDir = dir;
	}


	/**
	 *  Gets the records attribute of the SchemEditForm object
	 *
	 * @return    The records value
	 */
	public Record[] getRecords() {
		if (recordsDir == null) {
			prtln("getRecords: recordsDir is null");
			return null;
		}

		File[] files = recordsDir.listFiles(new XMLFileFilter());
		if (files == null) {
			prtln("no files were found in recordsDir (" + recordsDir.toString() + ")");
			return null;
		}

		ArrayList recList = new ArrayList();
		for (int i = 0; i < files.length; i++) {
			File file = files[i];
			if (!file.isDirectory()) {
				recList.add(new Record(file));
			}
		}

		Collections.sort(recList, new SortRecsByLastMod());

		records = (Record[]) recList.toArray(new Record[]{});
		// prtln ("getRecords: " + recs.length + " items found");
		return records;
	}


	/**
	 *  Gets the dirs attribute of the SchemEditForm object
	 *
	 * @return    The dirs value
	 */
	public List getDirs() {
		if (recordsDir == null) {
			prtln("getDirs: recordsDir is null");
			return null;
		}
		File[] files = recordsDir.listFiles();
		ArrayList dirs = new ArrayList();
		for (int i = 0; i < files.length; i++) {
			File file = files[i];
			if (file.isDirectory()) {
				dirs.add(file.getName());
			}
		}

		return dirs;
	}


	/**
	 *  Gets the hash attribute of the SchemEditForm object
	 *
	 * @return    The hash value
	 */
	public String getHash() {
		return hash;
	}


	/**
	 *  Sets the hash attribute of the SchemEditForm object
	 *
	 * @param  s  The new hash value
	 */
	public void setHash(String s) {
		hash = s;
	}


	/**
	 *  Gets the pageTitle attribute of the SchemEditForm object
	 *
	 * @return    The pageTitle value
	 */
	public String getPageTitle() {
		return pageTitle;
	}


	/**
	 *  Sets the pageTitle attribute of the SchemEditForm object
	 *
	 * @param  s  The new pageTitle value
	 */
	public void setPageTitle(String s) {
		pageTitle = s;
	}


	/**
	 *  Gets the xmlFormat attribute of the SchemEditForm object
	 *
	 * @return    The xmlFormat value
	 */
	public String getXmlFormat() {
		return framework.getXmlFormat();
	}


	/**
	 *  Gets the pageList attribute of the SchemEditForm object
	 *
	 * @return    The pageList value
	 */
	public PageList getPageList() {
		return pageList;
	}


	/**
	 *  Sets the pageList attribute of the SchemEditForm object
	 *
	 * @param  pageList  The new pageList value
	 */
	public void setPageList(PageList pageList) {
		this.pageList = pageList;
	}


	/**
	 *  Gets the forceValidation attribute of the SchemEditForm object
	 *
	 * @return    The forceValidation value
	 */
	public boolean getForceValidation() {
		return forceValidation;
	}


	/**
	 *  Sets the forceValidation attribute of the SchemEditForm object
	 *
	 * @param  forceValidation  The new forceValidation value
	 */
	public void setForceValidation(boolean forceValidation) {
		this.forceValidation = forceValidation;
	}


	/**
	 *  Gets the tmpArg attribute of the SchemEditForm object
	 *
	 * @return    The tmpArg value
	 */
	public String getTmpArg() {
		return tmpArg;
	}


	/**
	 *  Sets the tmpArg attribute of the SchemEditForm object
	 *
	 * @param  s  The new tmpArg value
	 */
	public void setTmpArg(String s) {
		tmpArg = s;
	}


	/**
	 *  Gets the recId attribute of the SchemEditForm object
	 *
	 * @return    The recId value
	 */
	public String getRecId() {
		return recId;
	}


	/**
	 *  Sets the recId attribute of the SchemEditForm object
	 *
	 * @param  id  The new recId value
	 */
	public void setRecId(String id) {
		recId = id;
	}


	/**
	 *  Sets the pathArg attribute of the SchemEditForm object
	 *
	 * @param  arg  The new pathArg value
	 */
	public void setPathArg(String arg) {
		pathArg = arg;
	}


	/**
	 *  Gets the pathArg attribute of the SchemEditForm object
	 *
	 * @return    The pathArg value
	 */
	public String getGuardedExitPath() {
		return guardedExitPath;
	}


	/**
	 *  Sets the guardedExitPath attribute of the SchemEditForm object
	 *
	 * @param  arg  The new guardedExitPath value
	 */
	public void setGuardedExitPath(String arg) {
		guardedExitPath = arg;
	}


	/**
	 *  Gets the pathArg attribute of the SchemEditForm object
	 *
	 * @return    The pathArg value
	 */
	public String getPathArg() {
		return pathArg;
	}


	/**
	 *  Gets the currentPage attribute of the SchemEditForm object
	 *
	 * @return    The currentPage value
	 */
	public String getCurrentPage() {
		return currentPage;
	}


	/**
	 *  Sets the currentPage attribute of the SchemEditForm object
	 *
	 * @param  s  The new currentPage value
	 */
	public void setCurrentPage(String s) {
		currentPage = s;
	}


	/**
	 *  Gets the currentPageEncoded attribute of the SchemEditForm object
	 *
	 * @return    The currentPageEncoded value
	 */
	public String getCurrentPageEncoded() {
		String cpe = currentPage;
		try {
			cpe = Files.encode(currentPage);
		} catch (Exception e) {
			prtln("getCurrentPageEncoded encoding error: " + e.getMessage());
		}
		return cpe;
	}


	/**
	 *  Gets the previousPage attribute of the SchemEditForm object
	 *
	 * @return    The previousPage value
	 */
	public String getPreviousPage() {
		return previousPage;
	}


	/**
	 *  Sets the previousPage attribute of the SchemEditForm object
	 *
	 * @param  s  The new previousPage value
	 */
	public void setPreviousPage(String s) {
		previousPage = s;
	}

	// -------- Fields stuff -------------

	private FieldInfoReader fieldInfoReader = null;


	/**
	 *  Gets the fieldInfoMap attribute of the SchemEditForm object
	 *
	 * @return    The fieldInfoMap value
	 */
	public FieldInfoMap getFieldInfoMap() {
		return framework.getFieldInfoMap();
	}


	/**
	 *  Gets the fieldInfoReader attribute of the SchemEditForm object
	 *
	 * @return    The fieldInfoReader value
	 */
	public FieldInfoReader getFieldInfoReader() {
		return fieldInfoReader;
	}


	/**
	 *  Sets the fieldInfoReader attribute of the SchemEditForm object
	 *
	 * @param  fieldInfoReader  The new fieldInfoReader value
	 */
	public void setFieldInfoReader(FieldInfoReader fieldInfoReader) {
		this.fieldInfoReader = fieldInfoReader;
	}


	/**
	 *  Returns a {@link org.dlese.dpc.schemedit.vocab.FieldInfoReader} for the
	 *  specified path. FieldInfoReaders are stored in the fieldInfoMap attribute
	 *  of the SchemEditForm object (key is xpath).
	 *
	 * @param  encodedPath  a jsp-encoded xpath
	 * @return              The fieldInfo value
	 */
	public FieldInfoReader getFieldInfo(String encodedPath) {
		// prtln ("getFieldInfo with encodedPath: " + encodedPath);
		if (schemaHelper == null) {
			prtlnErr("getFieldInfo() schemaHelper is null");
			return null;
		}
		String xpath = SchemaHelper.toSchemaPath(encodedPath);
		FieldInfoMap fieldInfoMap = getFieldInfoMap();
		if (fieldInfoMap == null) {
			prtlnErr("getFieldInfo cant find a fieldInfoMap!?");
			return null;
		}
		else {
			FieldInfoReader reader = fieldInfoMap.getFieldInfo(xpath);
			if (reader == null) {
				// this is not always a problem - sometimes there is no field info
				// prtln("getFieldInfo: reader not found for " + xpath);
			}

			/*			// print terms for debugging
			prtln ("\nFieldInfoReader for " + xpath + "\n\tterms");
			for (Iterator i=reader.getTermList().iterator();i.hasNext();) {
				prtln ("\t\t" + (String)i.next());
			}
*/
			return reader;
		}
	}


	// -------- end of Fields stuff -------------

	/**
	 *  Gets the schemaHelper attribute of the SchemEditForm object.<p>
	 *
	 *  Note: schemaHelper is SET as a side-effect of setFramework().
	 *
	 * @return    The schemaHelper value
	 */
	public SchemaHelper getSchemaHelper() {
		return this.schemaHelper;
	}


	/**
	 *  Gets the docMap attribute of the SchemEditForm object
	 *
	 * @return    The docMap value
	 */
	public DocMap getDocMap() {
		return docMap;
	}


	/**
	 *  Sets the docMap attribute of the SchemEditForm object
	 *
	 * @param  document  The new docMap value
	 */
	public void setDocMap(Document document) {
		this.docMap = new DocMap(document, schemaHelper);
	}


	/**
	 *  Sets the adnDocReader attribute of the SchemEditForm object
	 *
	 * @param  adnDocReader  The new adnDocReader value
	 */
	public void setAdnDocReader(ADNItemDocReader adnDocReader) {
		this.adnDocReader = adnDocReader;
	}


	/**
	 *  Gets the adnDocReader attribute of the SchemEditForm object
	 *
	 * @return    The adnDocReader value
	 */
	public ADNItemDocReader getAdnDocReader() {
		return adnDocReader;
	}


	/**
	 *  Gets the valueOf attribute of the SchemEditForm object
	 *
	 * @param  key  a jsp-encoded xpath
	 * @return      The valueOf value
	 */
	public Object getValueOf(String key) {
		return docMap.get(key);
	}


	/**
	 *  Sets the valueOf attribute of the SchemEditForm object
	 *
	 * @param  key  a jsp-encoded xpath
	 * @param  val  the value to set
	 */
	public void setValueOf(String key, Object val) {
		this.docMap.put(key, val);
	}


	/**
	 *  Gets the anyTypeValueOf attribute of the SchemEditForm object
	 *
	 * @param  key  a jsp-encoded xpath
	 * @return      The anyTypeValueOf value
	 */
	public Object getAnyTypeValueOf(String key) {
		// prtln("\nGET AnyTypeValueOf(): key = " + key);

		String xpath = XPathUtils.decodeXPath(key);
		// prtln ("\t decodedKey: " + xpath);
		if (this.inputManager != null) {

			String paramName = "anyTypeValueOf(" + key + ")";
			AnyTypeInputField field = (AnyTypeInputField) inputManager.getInputField(paramName);
			if (field != null && field.hasParseError())
				return field.getValue();
		}

		xpath = this.schemaHelper.encodeAnyTypeXpath(xpath);

		Node node = docMap.selectSingleNode(xpath);
		if (node != null) {
			return Dom4jUtils.prettyPrint(node);
		}
		return "";
	}


	/**
	 *  Gets the nodeExists attribute of the SchemEditForm object
	 *
	 * @param  key  a jsp-encoded xpath
	 * @return      The nodeExists value
	 */
	public String getNodeExists(String key) {
		// prtln ("getNodeExists with " + key);
		String xpath = XPathUtils.decodeXPath(key);
		boolean exists = docMap.nodeExists(xpath);
		return exists ? TRUE : FALSE;
	}


	/**
	 *  Return TRUE if this node has a value. Used by fullView to determine whether
	 *  a value should be shown as "missing or not".<p>
	 *
	 *  We don't want to flag simpleOrComplexContent nodes having type of
	 *  "xsd:string" as missing, though.
	 *
	 * @param  key  a jsp-encoded xpath
	 * @return      The nodeHasValue value
	 */
	public String getNodeHasValue(String key) {
		String xpath = XPathUtils.decodeXPath(key);

		String value = (String) docMap.get(xpath);

		/*		// diagnostics
		prtln("nodeHasValue(" + key + ")");
		if (value == null || value.trim().length() > 0) {
			prtln(" ... value is NULL");
		}
		else {
			prtln("  ... value is \"" + value + "\"");
		}
*/
		// a node is Empty if it has a value of SchemEditForm.UNSPECIFIED
		boolean nodeHasValue = (value != null &&
		// 2/18/07 no longer trim whitespace here
		// value.trim().length() > 0
			value.length() > 0
			 && !value.equals(SchemEditForm.UNSPECIFIED));

		if (!nodeHasValue) {
			prtln("  ... found an UNSPECIFIED node at " + xpath + " returning TRUE");
		}
		return nodeHasValue ? TRUE : FALSE;
	}


	/**
	 *  Determines if a text node in the VIEW UI (as opposed to the editor) of a
	 *  record is missing a value, so it can be highlighted accordingly. A node is
	 *  missing a value if it does not have a value or it has a value of
	 *  SchemEditForm.UNSPECIFIED, which is used to denote a unmade menu selection.
	 *  Nodes that are defined as "string" and are extension element of a
	 *  derivedType are not considered to be missing a value, even if they are
	 *  empty (these nodes are not even rendered as input fields in the editor).
	 *
	 * @param  key  encoded path
	 * @return      The nodeIsMissingValue value
	 */
	public String getNodeIsMissingValue(String key) {
		try {
			String xpath = XPathUtils.decodeXPath(key);

			xpath = this.schemaHelper.encodeAnyTypeXpath(xpath);
			String value = (String) docMap.get(xpath);
			// a node is missing a value if it has a value of SchemEditForm.UNSPECIFIED
			boolean hasValue = (value != null &&
			// 2/18/07 no longer trim whitespace here
			// value.trim().length() > 0 &&
				value.length() > 0 &&
				!value.equals(SchemEditForm.UNSPECIFIED));

			// if this node has a value, return ""
			if (hasValue) {
				return FALSE;
			}

			// simple- or complexTypes that hava a validating type of "xsd:string" do
			// NOT need to have a value (by DLESE convention)

			SchemaNode schemaNode = schemaHelper.getSchemaNode(xpath);
			if (schemaNode != null) {
				GlobalDef typeDef = schemaHelper.getGlobalDef(schemaNode);
				if (typeDef != null && typeDef.isComplexType()) {
					ComplexType complexTypeDef = (ComplexType) typeDef;

					if (complexTypeDef.isDerivedType()) {
						GlobalDef validatingType = schemaNode.getValidatingType();
						Namespace schemaNamespace = this.schemaHelper.getSchemaNamespace();
						String stringType = NamespaceRegistry.makeQualifiedName(this.schemaHelper.getSchemaNamespace(), "string");
						if (validatingType.getName().equals(stringType)) {
							return FALSE;
						}
					}
				}
			}

			return TRUE;
		} catch (Throwable t) {
			t.printStackTrace();
		}
		return TRUE;
	}


	/**
	 *  return true if the node exists and it is not empty
	 *
	 * @param  key  a jsp-encoded xpath
	 * @return      The nodeExistsWithContent value
	 */
	public String getNodeExistsWithContent(String key) {
		// prtln ("nodeExistsWithContent (" + key + ")");
		String xpath = XPathUtils.decodeXPath(key);

		boolean nodeExistsWithContent =
			docMap.nodeExists(xpath) && !docMap.isEmpty(xpath);
		return nodeExistsWithContent ? TRUE : FALSE;
	}


	/**
	 *  Return true if the node specified by key exists in the instance document
	 *  and it has a required attribute in the instance document.
	 *
	 * @param  key  a jsp-encoded xpath
	 * @return      The nodeExistsWithRequiredAttribute value
	 */
	public String getNodeExistsWithRequiredAttribute(String key) {
		String xpath = XPathUtils.decodeXPath(key);
		Node node = docMap.selectSingleNode(xpath);
		if (node == null) {
			return FALSE;
		}
		if (node.getNodeType() != Node.ELEMENT_NODE) {
			return FALSE;
		}
		Element element = (Element) node;
		if (element.attributes().isEmpty()) {
			return FALSE;
		}

		/* check attributes for a required one */
		for (Iterator i = element.attributeIterator(); i.hasNext(); ) {
			Attribute attribute = (Attribute) i.next();
			String attPath = xpath + "/@" + attribute.getQualifiedName();
			SchemaNode schemaNode = this.schemaHelper.getSchemaNode(attPath);
			if (schemaNode == null) {
				// prtln ("schemaNode not found for attribute (" + attPath + ")");
				continue;
			}
			if (schemaNode.isRequired())
				return TRUE;
		}
		return FALSE;
	}


	/**
	 *  return true iff schemaHelper.isRequiredBranch returns TRUE
	 *
	 * @param  key  a jsp-encoded xpath
	 * @return      The branchIsRequired value
	 */
	public String getBranchIsRequired(String key) {
		String xpath = XPathUtils.decodeXPath(key);
		boolean branchIsRequired = false;
		try {
			branchIsRequired = schemaHelper.isRequiredBranch(schemaHelper.getSchemaNode(xpath));
		} catch (Throwable t) {
			prtlnErr("error testing for requiredBranch (" + xpath + ": " + t.getMessage());
		}
		return branchIsRequired ? TRUE : FALSE;
	}


	/**
	 *  Returns TRUE if node is Required OR has a value (i.e., satisfies the
	 *  "nodeExistsWithContent" predicate).<p>
	 *
	 *  Used with views for displaying (as opposed to editing) metadata fields.
	 *
	 * @param  key  a jsp-encoded xpath
	 * @return      The viewNode value
	 */
	public String getViewNode(String key) {
		// prtln ("getViewNode(" + key + ")");

		String xpath = XPathUtils.decodeXPath(key);
		SchemaNode schemaNode = schemaHelper.getSchemaNode(xpath);
		boolean requiredContent = false;
		if (schemaNode != null) {
			if (schemaNode.isAttribute()) {
				requiredContent = schemaNode.isRequired();
			}
			else {
				requiredContent = schemaHelper.isRequiredContentElement(schemaNode);
			}
		}
		else {
			prtln("schemaNode NOT found for " + xpath + ", requiredContent left as false");
		}

		if (requiredContent) {
			return TRUE;
		}

		if (getBranchIsRequired(key).equals(TRUE)) {
			return TRUE;
		}
		if (getNodeExistsWithContent(key).equals(TRUE)) {
			return TRUE;
		}
		if (getNodeExistsWithRequiredAttribute(key).equals(TRUE)) {
			return TRUE;
		}

		return FALSE;
	}


	/**
	 *  Returns true of the node designated by key can be expanded.<p>
	 *
	 *  A node can be expanded if: - it is a required branch - the node exists and
	 *  it is NOT empty (it should also be complex??) -
	 *
	 * @param  key  a jsp-encoded xpath
	 * @return      The nodeIsExpandable value
	 */
	public String getNodeIsExpandable(String key) {
		/*
			getBranchIsRequired test commented out  9/23/09 -
			rationale: it makes no sense to have an expand widget if
			branch is empty (but would a branch ever be empty if it is required???
		*/
		/* 		if (getBranchIsRequired(key).equals(TRUE)) {
			prtln (" ... returning TRUE (branch is required)");
			return TRUE;
		} */
		String xpath = XPathUtils.decodeXPath(key);

		// docMap.isEmpty returns true even if there are children elements - as long as they are empty
		// use docMap.hasChildren instead
		if (docMap.nodeExists(xpath) && docMap.hasChildren(xpath)) {
			// prtln (" ... returning TRUE (node is not empty)");
			return TRUE;
		}
		return FALSE;
	}


	/**
	 *  Gets the parentNodeExists attribute of the SchemEditForm object
	 *
	 * @param  key  a jsp-encoded xpath
	 * @return      The parentNodeExists value
	 */
	public String getParentNodeExists(String key) {
		// prtln ("getParentNodeExists with " + key);
		String xpath = XPathUtils.decodeXPath(key);
		String parentXPath = XPathUtils.getParentXPath(xpath);

		// 2/18/07 - no longer ignore whitespace
		/* boolean parentNodeExists =
			(parentXPath.trim().length() > 0 && docMap.nodeExists(parentXPath)); */
		boolean parentNodeExists =
			(parentXPath.length() > 0 && docMap.nodeExists(parentXPath));

		return parentNodeExists ? TRUE : FALSE;
	}


	/**
	 *  Gets the nodeIsEmpty attribute of the SchemEditForm object
	 *
	 * @param  key  a jsp-encoded xpath
	 * @return      The nodeIsEmpty value
	 */
	public String getNodeIsEmpty(String key) {
		// prtln ("getNodeIsEmpty with " + key);
		String xpath = XPathUtils.decodeXPath(key);
		boolean nodeIsEmpty = docMap.isEmpty(xpath);

		return nodeIsEmpty ? TRUE : FALSE;
	}


	/**
	 *  Gets the enumerationPaths of a Document given an xpath to a member. For
	 *  exmample, given "/itemRecord/general/subjects/subject", a list of all
	 *  elements satisfying this path would be returned . /itemRecord/general/subjects/subject[1]
	 *  would be the first member of this list.
	 *
	 * @param  encodedPath  a jsp-encoded xpath
	 * @return              a list of xpaths corresponding to the values of
	 *      enumeration.
	 */
	public List getEnumerationPaths(String encodedPath) {
		String xpath = XPathUtils.decodeXPath(encodedPath);
		List nodes = docMap.selectNodes(xpath);
		List paths = new ArrayList();
		for (int i = 0; i < nodes.size(); i++) {
			Node node = (Node) nodes.get(i);
			int index = i + 1;
			String path = xpath + "[" + index + "]";
			paths.add(path);
		}
		return paths;
	}


	/**
	 *  Clear cache of eneration values that are used to optimize property lookup
	 *  during display of form. (cleared before presenting form)
	 */
	public void clearMultiValuesCache() {
		multiValuesCache.clear();
	}


	/**
	 *  Gets the curently assigned values for the given xpath in the current
	 *  Document. This method can be called very many times for the same page, so
	 *  we cache values in the multiValuesCache
	 *
	 * @param  encodedPath  a jsp-encoded xpath
	 * @return              The enumerationValuesOf value
	 */
	public String[] getEnumerationValuesOf(String encodedPath) {
		// prtln("getEnumerationValuesOf() with " + encodedPath);
		String xpath = XPathUtils.decodeXPath(encodedPath);

		if (multiValuesCache.containsKey(xpath)) {
			String[] foundValues = (String[]) multiValuesCache.get(xpath);
			return foundValues;
		}
		List nodes = docMap.selectNodes(xpath);
		if (nodes == null) {
			prtlnErr("\tunable to find any nodes for " + xpath);
			return null;
		}
		String[] values = new String[nodes.size()];
		for (int i = 0; i < nodes.size(); i++) {
			Node node = (Node) nodes.get(i);
			String value = node.getText();
			values[i] = value;
		}

		multiValuesCache.put(xpath, values);
		// prtln("put " + values.length + " items in cache");
		return values;
	}


	/**
	 *  Gets the membersOf attribute of the SchemEditForm object
	 *
	 * @param  encodedPath  a jsp-encoded xpath
	 * @return              The membersOf value
	 */
	public List getMembersOf(String encodedPath) {
		String xpath = XPathUtils.decodeXPath(encodedPath);
		List nodes = docMap.selectNodes(xpath);
		return nodes;
	}


	/**
	 *  Returns number of elements in the Document matching a given xpath
	 *
	 * @param  encodedPath  a jsp-encoded xpath
	 * @return              The membersOf value
	 */
	public int getMemberCountOf(String encodedPath) {
		//prtln ("getMemberCountOf() - " + encodedPath);

		encodedPath = this.schemaHelper.encodePathIfAnyType(encodedPath);

		return getMembersOf(encodedPath).size();
	}


	/**
	 *  Gets the childElementCountOf attribute of the SchemEditForm object
	 *
	 * @param  encodedPath  a jsp-encoded xpath
	 * @return              The childElementCountOf value
	 */
	public int getChildElementCountOf(String encodedPath) {
		int count = getMemberCountOf(encodedPath + "/*");
		return count;
	}


	/**
	 *  Gets the hasChildren attribute of the SchemEditForm object
	 *
	 * @param  encodedPath  a jsp-encoded xpath
	 * @return              The hasChildren value
	 */
	public boolean getHasChildren(String encodedPath) {
		return (getChildElementCountOf(encodedPath) > 0);
	}


	/**
	 *  Select all substitutionGroup elements by building up complex selection
	 *  xpath ORing together the xpaths for the substitutionGroup members
	 *
	 * @param  encodedPath  a jsp-encoded xpath
	 * @return              The substitutionGroupMembersOf value
	 */
	public List getSubstitutionGroupMembersOf(String encodedPath) {
		String selectionPath = "";
		String xpath = XPathUtils.decodeXPath(encodedPath);
		SchemaNode schemaNode = schemaHelper.getSchemaNode(xpath);
		if (schemaNode == null || !schemaNode.isHeadElement())
			return new ArrayList();

		if (!schemaNode.isAbstract()) {
			selectionPath = xpath;
		}
		String parentXPath = XPathUtils.getParentXPath(xpath);
		Iterator groupMembers = schemaNode.getSubstitutionGroup().iterator();
		while (groupMembers.hasNext()) {
			GlobalElement member = (GlobalElement) groupMembers.next();
			if (selectionPath.length() > 0)
				selectionPath += " | ";
			String memberPath = parentXPath + "/" + member.getQualifiedInstanceName();
			selectionPath += memberPath;
		}
		// prtln ("selectionPath: " + selectionPath);
		List nodes = docMap.selectNodes(selectionPath);
		// prtln ("    " + nodes.size() + " elements found");
		return nodes;
	}


	/**
	 *  Gets the substitutionGroupMemberCountOf attribute of the SchemEditForm
	 *  object
	 *
	 * @param  encodedPath  a jsp-encoded xpath
	 * @return              The substitutionGroupMemberCountOf value
	 */
	public int getSubstitutionGroupMemberCountOf(String encodedPath) {
		return getSubstitutionGroupMembersOf(encodedPath).size();
	}


	/**
	 *  Gets the legal values an enumeration (controlled vocab defined in schema) can assume.
	 *
	 * @param  encodedPath  encoded xpath of field
	 * @return              The legal values
	 */
	public List getLegalEnumerationValuesOf(String encodedPath) {
		// prtln ("getLegalEnumerationValuesOf() with encodedPath = " + encodedPath);
		String xpath = XPathUtils.decodeXPath(encodedPath);
		List emptyList = new ArrayList();

		// normalize path since we are after schema information
		String normalizedXpath = SchemaHelper.toSchemaPath(xpath);
		
		SchemaNode schemaNode = schemaHelper.getSchemaNode(normalizedXpath);
		if (schemaNode == null) {
			prtlnErr("schemaNode not found for " + normalizedXpath);
			return emptyList;
		}

		/*  Use the validatingType associated with the schemaNode to account for
			references, in which case the enumerations are defined by the validatingType.
			NOTE also, that it is possible that a complexType defines
			simpleContent that extends an enumerated type. So we use the
			the "validatingType" for the complexType, which might refer
			to a SimpleType def (the validatingTypeName is different from the
			typeName in the case of ComplexTypes that define either Simple- or
			ComplexContent)
		*/
		GlobalDef globalDef = schemaNode.getValidatingType();
		if (globalDef == null) {
			prtlnErr("globalDef not found for " + xpath);
			return emptyList;
		}

		if (!globalDef.isSimpleType()) {
			prtlnErr("getLegalEnumerationValuesOf(): a SIMPLE_TYPE is required! (" + globalDef.getQualifiedName() + ")");
			return emptyList;
		}

		return ((SimpleType) globalDef).getEnumerationValues(false);
	}


	/**
	 *  Gets the possible values and labels (defined by the an EnumerationType
	 *  representing a controlled vocab) that an element may assume.
	 *
	 * @param  encodedPath  a jsp-encoded xpath
	 * @return              array of LabelValue beans for controlled vocab defined
	 *      for the xpath
	 */
	public LabelValueBean[] getEnumerationOptions(String encodedPath) {

		LabelValueBean[] emptyArray = new LabelValueBean[]{};
		List values = this.getLegalEnumerationValuesOf(encodedPath);

		if (values == null)
			return emptyArray;

		//  5/14/04 - katy didn't want to use any leaf values, so labels is now left null
		// List labels = schemaHelper.getEnumerationValues(typeName, true);  // get leaf values (aka labels)
		List labels = null;

		if ((labels == null) || (labels.size() != values.size())) {
			// prtln("didn't create labels, assigning labels to values instead");
			labels = new ArrayList();
			labels.addAll(values);
		}

		/* if metadata field at xpath contains a value that is not contained in the controlled vocab,
		we add it to the select options so it shows up in the UI*/
		String xpath = XPathUtils.decodeXPath(encodedPath);
		String currentValue = (String)this.docMap.get(xpath);
 		if (currentValue != null && currentValue.trim().length() > 0
			&& !values.contains(currentValue)) {
			prtln ("inserting \"" + currentValue + "\" into enumeration options");
			values.add (0, currentValue);
			labels.add (0, currentValue + " (THIS TERM IS NOT ALLOWED)");
		}
		
		// now create array of LabelValueBeans
		LabelValueBean[] options = new LabelValueBean[values.size()];
		for (int i = 0; i < values.size(); i++) {
			String value = (String) values.get(i);
			String label = (String) labels.get(i);
			options[i] = new LabelValueBean(label, value);
		}

		// prtln("getEnumerationOptions() returning " + options.length + " LabelValueBeans");
		return options;
	}


	/**
	 *  Given the path to a schemaNode that isHeadElement(), return an array of
	 *  LabelValueBean objects representing, the substitutionGroup, where both the
	 *  label and the value are the group memebers qualifiedName
	 *
	 * @param  encodedPath  a jsp-encoded xpath
	 * @return              The substitutionGroupOptions value
	 */
	public LabelValueBean[] getSubstitutionGroupOptions(String encodedPath) {
		// prtln ("getEnumerationOptions() with encodedPath = " + encodedPath);

		LabelValueBean[] emptyArray = new LabelValueBean[]{};
		String xpath = SchemaHelper.toSchemaPath(encodedPath);
		SchemaNode schemaNode = schemaHelper.getSchemaNode(xpath);
		if (schemaNode == null || !schemaNode.isHeadElement()) {
			prtlnErr("WARNING: getSubstitutionGroupOptions did not find substitution group for " + xpath);
			return emptyArray;
		}

		List labels = new ArrayList();
		if (!schemaNode.isAbstract())
			labels.add(XPathUtils.getNodeName(xpath));
		for (Iterator members = schemaNode.getSubstitutionGroup().iterator(); members.hasNext(); ) {
			GlobalElement ge = (GlobalElement) members.next();
			labels.add(ge.getQualifiedInstanceName());
		}

		if (labels.isEmpty()) {
			prtln("WARNING: getSubstitutionGroupOptions found no substitutionGroup members for " + xpath);
			return emptyArray;
		}

		String parentXPath = XPathUtils.getParentXPath(xpath);
		LabelValueBean[] selectOptions = new LabelValueBean[labels.size() + 1];
		selectOptions[0] = new LabelValueBean("-- choice --", "");
		for (int i = 0; i < labels.size(); i++) {
			String label = (String) labels.get(i);
			String value = parentXPath + "/" + label;
			selectOptions[i + 1] = new LabelValueBean(label, value);
		}
		return selectOptions;
	}


	/**
	 *  provide values to use as constants in comboUnionInput processing
	 *
	 * @return    The comboOtherOption value
	 */
	public LabelValueBean getComboOtherOption() {
		String label = " -- type in another value -- ";
		String value = "_|-other-|_";
		return new LabelValueBean(label, value);
	}


	/**
	 *  Gets the options for a comboSelect input element, which presents a list of
	 *  options to the user but also allows input of arbitrary values. <p>
	 *
	 *  If the field being edited contains a value other than those specified in
	 *  the schema, then add this value to the options returned.
	 *
	 * @param  encodedPath  jsp-encoded xpath to the element
	 * @return              The comboSelectOptions value
	 */
	public LabelValueBean[] getComboSelectOptions(String encodedPath) {
		// prtln ("getComboSelectOptions(): " + encodedPath);
		LabelValueBean[] options = getSelectOptions(encodedPath);
		String currentValue = (String) getValueOf(encodedPath);
		// no longer ignore whitespace
		/* 		if (currentValue == null || currentValue.trim().length() == 0) {
			return options; */
		if (currentValue == null || currentValue.length() == 0) {
			return options;
		}
		else {
			LabelValueBean[] newOptions = new LabelValueBean[options.length + 1];
			for (int i = 0; i < options.length; i++) {
				String value = options[i].getLabel();
				if (value.equals(currentValue)) {
					return options;
				}
				else {
					newOptions[i] = options[i];
				}
			}
			newOptions[options.length] = new LabelValueBean(currentValue, currentValue);
			return newOptions;
		}
	}


	/**
	 *  Gets the selectOptions for the given xpath from the Schema. If the element
	 *  specified by xpath is an optionalSingleSelect then method returns results
	 *  identical to getEnumerationOptions, except a new, blank option is added to
	 *  the list of options.<p>
	 *
	 *  If the element at xpath is NOT an optionalSingleSelect, then
	 *  getEnumerationOptions is returned.
	 *
	 * @param  encodedPath  a jsp-encoded xpath
	 * @return              an array of LabelValueBean instances
	 */
	public LabelValueBean[] getSelectOptions(String encodedPath) {
		try {
			String xpath = SchemaHelper.toSchemaPath(encodedPath);
			SchemaNode schemaNode = schemaHelper.getSchemaNode(xpath);
			if (schemaNode == null) {
				prtlnErr("getSelectOptions: schemaNode not found for " + xpath);
				return null;
			}
			// prtln ("getSelectOptions() current value is: " + this.docMap.get(xpath));
			LabelValueBean[] enumerationOptions = getEnumerationOptions(xpath);

			List selectOptionsList = new ArrayList();
			selectOptionsList.add(new LabelValueBean(UNSPECIFIED, ""));

			for (int i = 0; i < enumerationOptions.length; i++) {
				// selectOptions[i + 1] = enumerationOptions[i];
				selectOptionsList.add(enumerationOptions[i]);
			}

			return (LabelValueBean[]) selectOptionsList.toArray(new LabelValueBean[]{});
		} catch (Throwable t) {
			prtlnErr("getSelectOption error: " + t.getMessage());
			t.printStackTrace();
			return null;
		}
	}


	/**
	 *  Determines whether the parent of the element corresponding to encodedPath
	 *  can accept a new sibling of encodedPath.<p>
	 *
	 *  The indexing of the target element is important, since it is required by
	 *  the compositor to determine whether a new sibling can be accepted.
	 *
	 * @param  encodedPath  a jsp-encoded xpath
	 * @return              TRUE if a new sibling can be added.
	 */
	public String getAcceptsNewSibling(String encodedPath) {
		// prtln ("\ngetAcceptsNewSibling with encodedPath = " + encodedPath);
		String xpath = XPathUtils.decodeXPath(encodedPath);

		xpath = schemaHelper.encodePathIfAnyType(xpath);

		String parentXpath = XPathUtils.getParentXPath(xpath);
		int elementIndex = XPathUtils.getIndex(xpath);
		String elementName = XPathUtils.getNodeName(xpath);
		// prtln ("\t parentXpath: " + parentXpath + "  elementName: " + elementName +"  elementIndex: " + elementIndex);
		try {
			// can be sequence or choice
			// Sequence compositor = (Sequence) schemaHelper.getCompositor(parentXpath);
			Compositor compositor = schemaHelper.getCompositor(parentXpath);
			if (compositor != null) {
				Element instanceElement = (Element) docMap.selectSingleNode(parentXpath);
				if (instanceElement == null) {
					/* 	prtln  ("WARNING:  getAcceptsNewChild() couldn't get instance node at " + xpath);
						sometimes there won't be an element in the instance document (the field in the editor is
						created before the element in the instanceDoc. in this case we want to return false so
						the control will be presented (it is presented in the parent).
					*/
					return FALSE;
				}
				// prtln ("\t instanceElement: " + Dom4jUtils.prettyPrint(instanceElement));
				// prtln ("\t elementName: " + elementName + ", elementIndex: " + elementIndex);
				// prtln (compositor.toString());
				boolean accepts =
					compositor.acceptsNewMember(instanceElement, elementName, elementIndex);
				return accepts ? TRUE : FALSE;
			}
			else {
				prtln("compositor not found!");
				return FALSE;
			}
		} catch (Throwable t) {
			prtln("getAcceptsNewSibling error: " + t.getMessage());
			t.printStackTrace();
		}
		// prtln("returning false");
		return FALSE;
	}


	/**
	 *  Gets the acceptsNewSubstitionGroupMember attribute of the SchemEditForm
	 *  object
	 *
	 * @param  encodedPath  a jsp-encoded xpath
	 * @return              The acceptsNewSubstitionGroupMember value
	 */
	public String getAcceptsNewSubstitionGroupMember(String encodedPath) {
		String xpath = XPathUtils.decodeXPath(encodedPath);
		SchemaNode schemaNode = schemaHelper.getSchemaNode(xpath);
		if (schemaNode == null) {
			prtlnErr("WARNING: getAcceptsNewSubstitionGroupMember did not find schemaNode for " + xpath);
			return FALSE;
		}
		boolean accepts =
			getSubstitutionGroupMemberCountOf(encodedPath) < schemaNode.getMaxOccurs();
		// prln ("  .. accepts ? " + accepts);
		return accepts ? TRUE : FALSE;
	}


	/**
	 *  Gets the acceptsNewChoice attribute of the SchemEditForm object
	 *
	 * @param  encodedPath  a jsp-encoded xpath
	 * @return              The acceptsNewChoice value
	 */
	public String getAcceptsNewChoice(String encodedPath) {
		// prtln ("\ngetAcceptsNewChoice(): " + encodedPath);
		String xpath = XPathUtils.decodeXPath(encodedPath);
		if (schemaHelper.hasChoiceCompositor(xpath)) {
			Choice compositor = (Choice) schemaHelper.getCompositor(xpath);
			Element instanceElement = (Element) docMap.selectSingleNode(xpath);

			if (compositor != null && instanceElement != null) {
				boolean accepts = compositor.acceptsNewMember(instanceElement);
				// prtln ("compositor says accept is " + accepts);
				return (accepts) ? TRUE : FALSE;
			}
		}
		return FALSE;
	}


	/**
	 *  Gets the choiceOptions for the element specified by "encodedPath", adding
	 *  indexing if the element is a multiChoice.
	 *
	 * @param  encodedPath  a jsp-encoded xpath
	 * @return              The choiceOptions value
	 */
	public LabelValueBean[] getChoiceOptions(String encodedPath) {
		// prtln ("choiceOptions - " + encodedPath);
		List choices = new ArrayList();
		String xpath = XPathUtils.decodeXPath(encodedPath);
		Choice compositor = null;
		if (schemaHelper.hasChoiceCompositor(xpath)) {
			compositor = (Choice) schemaHelper.getCompositor(xpath);
			Element instanceElement = (Element) docMap.selectSingleNode(xpath);
			if (compositor != null && instanceElement != null) {
				choices = compositor.getAcceptableMembers(instanceElement);
			}
		}

		LabelValueBean[] selectOptions = new LabelValueBean[choices.size() + 1];
		selectOptions[0] = new LabelValueBean("-- choice --", "");
		for (int i = 0; i < choices.size(); i++) {
			String label = (String) choices.get(i);
			String choicePath = encodedPath + "/" + label;

			// paths for multichoice parents and repeating choices require indexing
			if (schemaHelper.isMultiChoiceElement(xpath) ||
				schemaHelper.isRepeatingElement(choicePath)) {
				int choiceIndex = this.getMemberCountOf(choicePath) + 1;
				choicePath = choicePath + "_" + choiceIndex + "_";
			}
			selectOptions[i + 1] = new LabelValueBean(label, choicePath);
		}
		return selectOptions;
	}


	/**
	 *  Cache of repeating values that stores repeating elements which may have
	 *  empty values after the form is submitted. The cache is cleared after
	 *  validation of form.
	 *
	 * @return    The repeatingFieldsToPrune value
	 */
	public ArrayList getRepeatingFieldsToPrune() {
		return repeatingFieldsToPrune;
	}


	/**
	 *  Clear the cache of repeating values that stores the repeating elements
	 *  which may have empty values after form is submitted. Called by {@link
	 *  org.dlese.dpc.schemedit.input.SchemEditValidator#pruneRepeatingFields}
	 */
	public void clearRepeatingFieldsToPrune() {
		repeatingFieldsToPrune.clear();
	}


	/**
	 *  Stores repeating values that stores repeating elements which may have empty
	 *  values after the form is submitted. Necessary to support the case in which
	 *  a user unselects all previously selected values
	 *
	 * @param  xpath  The new repeatingField value
	 */
	public void setRepeatingField(String xpath) {
		repeatingFieldsToPrune.add(xpath);
	}


	/**
	 *  Gets a list of all nodes for a given xpath, PLUS a bogus node that supports
	 *  addition of a new member in the JSP form.
	 *
	 * @param  encodedPath  a jsp-encoded xpath
	 * @return              A list of existing nodes PLUS a new, empty one
	 */
	public List getRepeatingMembersOf(String encodedPath) {
		// side effect: add path of this node's parent to the fieldsToPrune
		String xpath = XPathUtils.decodeXPath(encodedPath);

		xpath = this.schemaHelper.encodePathIfAnyType(xpath);

		String parentPath = XPathUtils.getParentXPath(xpath);
		repeatingFieldsToPrune.add(parentPath);

		List nodes = docMap.selectNodes(xpath);
		return nodes;
	}


	/**
	 *  Validate the request parameters before the Action sees them). NOTE:
	 *  "validate" must be 2set to TRUE in action mapping (see struts-config) for
	 *  this method to be called.<p>
	 *
	 *  NOTE: is this necessary for the entities to get displayed correctly in the
	 *  editing forms??
	 *
	 * @param  mapping  NOT YET DOCUMENTED
	 * @param  request  NOT YET DOCUMENTED
	 * @return          NOT YET DOCUMENTED
	 */
	public SchemEditActionErrors validate(org.apache.struts.action.ActionMapping mapping, javax.servlet.http.HttpServletRequest request) {
		SchemEditActionErrors errors = new SchemEditActionErrors();
		this.inputManager = null;
		try {
			errors = (SchemEditActionErrors) super.validate(mapping, request);

			if (errors == null) { // to avoid problems if we try to add errors subsequently
				errors = new SchemEditActionErrors();
			}

			// We don't want to validate input if the user is trying to exit the editor
			String command = request.getParameter("command");

			// always let exits go, since we don't want to save anything
			if (command != null && command.equalsIgnoreCase("exit")) {
				return errors;
			}

			/*	framework will not be initialized the first time this is executed for each record,
				so we skip it in these cases.
			*/
			if (getFramework() != null) {
				SchemaHelper schemaHelper = getFramework().getSchemaHelper();

				// res_qual hack ...
				if (this.getFramework().getXmlFormat().equals("res_qual")) {
					CATServiceHelper helper = this.getSuggestionServiceHelper();
					if (helper != null) {
						try {
							((ResQualSuggestionServiceHelper) helper).rectifyInstanceDoc(request);
						} catch (Throwable t) {
							prtlnErr("resQualHack: " + t.getMessage());
						}
					}
				}

				// SchemEditActionErrors needs schemaHelper to sort by doc order
				errors.setSchemaHelper(schemaHelper);

				InputManager im = new InputManager(request, getFramework());

				// process Element fields
				// prtln ("processing Element fields");
				for (Iterator i = im.getElementFields().iterator(); i.hasNext(); ) {
					InputField field = (InputField) i.next();
					try {
						docMap.put(field.getXPath(), field.getValue());
						// prtln ("\n" + field.getXPath() + "\n\t"  + field.getValue());
					} catch (Exception e) {
						String errorMsg = "failed to put \"" + field.getValue() + "\"";
						errorMsg += " at \"" + field.getXPath() + "\": " + e.getMessage();
						prtlnErr(errorMsg);
					}
				}

				// process AntType fields
				if (!im.getAnyTypeFields().isEmpty()) {
					removeEmptyAnyTypeFields(im);
					rebuildAnyTypeFields(im, errors);
				}

				// Process entityError fields

				List errorFields = im.getEntityErrorFields();
				for (Iterator f = errorFields.iterator(); f.hasNext(); ) {
					InputField field = (InputField) f.next();
					List entityErrors = field.getEntityErrors();

					String errorMsg = "";
					for (Iterator e = entityErrors.iterator(); e.hasNext(); ) {
						ReferenceException refEx = (ReferenceException) e.next();
						errorMsg += "<br/>&nbsp;-&nbsp;" + refEx.getErrorMessage();
					}

					SchemEditErrors.addEntityError(errors, field, errorMsg);
					exposeNode(field.getXPath());
					setHash("top");
				}
				this.inputManager = im;
			}

			return errors;
		} catch (Throwable t) {
			prtlnErr("validator caught an error: " + t.getMessage());
			t.printStackTrace();
			errors.add("error", new ActionError("session.timeout.msg"));
			return errors;
		}
	}


	/**
	 *  Only called if there were no ill-formed xml elements.
	 *
	 * @param  inputManager  NOT YET DOCUMENTED
	 * @param  errors        NOT YET DOCUMENTED
	 */
	private void rebuildAnyTypeFields(InputManager inputManager, SchemEditActionErrors errors) {
		for (Iterator i = inputManager.getAnyTypeFields().iterator(); i.hasNext(); ) {
			AnyTypeInputField field = (AnyTypeInputField) i.next();

			boolean hasParseError = field.hasParseError();

			// Mark field error
			if (hasParseError) {
				SchemEditErrors.addAnyTypeError(errors, field, field.getParseError());
				exposeNode(field.getXPath());
				setHash("top");
			}

			Element valueElement = hasParseError ?
				DocumentHelper.createElement("error") :
				field.getValueElement();

			try {
				String encodedPath = schemaHelper.encodeAnyTypeXpath(field.getXPath());
				Node parent = docMap.selectSingleNode(XPathUtils.getParentXPath(encodedPath));
				if (parent == null)
					throw new Exception("parent node not found");
				List children = ((Element) parent).elements();
				if (children.isEmpty()) {
					children.add(valueElement);
				}
				else {
					// replace the element at "encodedPath" if possible
					int index = XPathUtils.getIndex(encodedPath);
					if (index > 0)
						--index;
					if (index >= children.size()) // append to children (index is too big)
						children.add(valueElement);
					else {
						children.set(index, valueElement);
					}
				}
			} catch (Exception e) {
				String errorMsg = "failed to put \"" + field.getValue() + "\"";
				errorMsg += " at \"" + field.getXPath() + "\": " + e.getMessage();
				prtlnErr(errorMsg);
			}
		}
	}


	/**
	 *  Make a list of all empty AnyType fields, and then call InputManager.removeAnyTypeFields
	 *  to actually remove them from the fieldList and also from the instanceDoc
	 *
	 * @param  inputManager  NOT YET DOCUMENTED
	 */
	private void removeEmptyAnyTypeFields(InputManager inputManager) {
		List anyTypeFieldsToRemove = new ArrayList();

		for (Iterator i = inputManager.getAnyTypeFields().iterator(); i.hasNext(); ) {
			InputField field = (InputField) i.next();
			String value = field.getValue();
			// don't process this field at all if there is no content!
			if (value == null || value.trim().length() == 0) {
				anyTypeFieldsToRemove.add(field);
			}
		}

		if (!anyTypeFieldsToRemove.isEmpty()) {
			Collections.sort(anyTypeFieldsToRemove, new SortInputFieldDescending());
			inputManager.removeAnyTypeFields(anyTypeFieldsToRemove, docMap);
		}
	}


	/**
	 *  Output a line of text to standard out, with datestamp, if debug is set to
	 *  true.
	 *
	 * @param  s  The String that will be output.
	 */
	protected static void prtln(String s) {
		if (debug) {
			SchemEditUtils.prtln(s, "SchemEditForm");
		}
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  s  NOT YET DOCUMENTED
	 */
	private final void prtlnErr(String s) {
		System.err.println("SchemEditForm: " + s);
	}

}

