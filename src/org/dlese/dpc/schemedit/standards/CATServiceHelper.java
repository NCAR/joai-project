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
package org.dlese.dpc.schemedit.standards;

import org.dlese.dpc.schemedit.MetaDataFramework;
import org.dlese.dpc.schemedit.action.form.SchemEditForm;
import org.dlese.dpc.xml.schema.*;
import org.dlese.dpc.xml.XPathUtils;
import org.dlese.dpc.schemedit.*;
import org.dlese.dpc.schemedit.display.CollapseBean;
import org.dlese.dpc.schemedit.display.CollapseUtils;

import org.dlese.dpc.schemedit.standards.config.SuggestionServiceConfig;
import org.dlese.dpc.schemedit.standards.config.SuggestionServiceManager;
import org.dlese.dpc.schemedit.standards.adn.DleseSuggestionServiceHelper;
import org.dlese.dpc.schemedit.standards.asn.AsnSuggestionServiceHelper;
import org.dlese.dpc.schemedit.standards.asn.ResQualSuggestionServiceHelper;
import org.dlese.dpc.schemedit.standards.asn.AsnDocKey;
import org.dlese.dpc.schemedit.standards.commcore.CommCoreServiceHelper;
import org.dlese.dpc.schemedit.standards.td.TeachersDomainServiceHelper;

import org.dlese.dpc.serviceclients.cat.CATServiceToolkit;
import org.dlese.dpc.serviceclients.cat.CATStandard;
import org.dlese.dpc.serviceclients.cat.CATRequestConstraints;

import javax.servlet.http.HttpServletRequest;

import org.dom4j.*;

import java.io.*;
import java.util.*;
import org.apache.struts.util.LabelValueBean;

import java.net.*;

/**
 *  Run-time support for CAT suggestion service, which acts as intermediary
 *  between CAT Service client and Form bean/JSP pages.<p>
 *
 *  The CAT service UI involves extraction of several values from the item
 *  record being edited for each framework, such as selected keywords, selected
 *  graderanges, etc. The functionality to extract these values is delegated to
 *  the framework-specific plug-in, which implments {@link CATHelperPlugin}.
 *
 * @author    ostwald
 */
public abstract class CATServiceHelper implements SuggestionServiceHelper {
	private static boolean debug = true;

	/**  NOT YET DOCUMENTED */
	public final static String TREE_MODE = "tree";
	/**  NOT YET DOCUMENTED */
	public final static String LIST_MODE = "list";
	/**  NOT YET DOCUMENTED */
	public final static String SELECTED_CONTENT = "selected";
	/**  NOT YET DOCUMENTED */
	public final static String SUGGESTIONS_CONTENT = "suggested";
	/**  NOT YET DOCUMENTED */
	public final static String BOTH_CONTENT = "both";
	/**  NOT YET DOCUMENTED */
	public final static String ALL_CONTENT = "all";

	CATHelperPlugin frameworkPlugin = null;
	private SchemEditForm sef = null;
	private MetaDataFramework framework = null;

	private List suggestedStandards = null;
	private String url = null;

	private String[] selectedGradeRanges = null;
	private String selectedKeywords = null;

	private String displayMode = TREE_MODE;
	private String displayContent = BOTH_CONTENT;
	private boolean useDescription = true;
	private boolean useKeywords = true;
	private boolean useGradeRanges = true;
	private boolean useSubjects = true;
	private boolean serviceIsActive = true;

	private int keywordWeighting = 4;


	/**
	 *  Constructor for the CATServiceHelper object
	 *
	 * @param  sef              Description of the Parameter
	 * @param  frameworkPlugin  NOT YET DOCUMENTED
	 */
	public CATServiceHelper(SchemEditForm sef, CATHelperPlugin frameworkPlugin) {
		this.sef = sef;
		this.frameworkPlugin = frameworkPlugin;
		frameworkPlugin.init(this);
		framework = sef.getFramework();
		/*
		 *  register this path as a repeating field so that the case when no selections are made
		 *  is properly processed. NOTE: an empty hidden variable must also be defined in the jsp!
		 */
		// necessary so all selections can be cleared.
		sef.setRepeatingField(this.getXpath());
	}


	/**
	 *  Gets the standardsDocument from which suggestions are requested and received.
	 *
	 * @return    The standardsDocument value
	 */
	public abstract StandardsDocument getStandardsDocument();


	/**
	 *  Gets the standardsFormat attribute of the CATServiceHelper object
	 *
	 * @return    The standardsFormat value (e.g., "dlese", "asn")
	 */
	public abstract String getStandardsFormat();


	/**
	 *  Gets the frameworkPlugin attribute of the SuggestionServiceHelper object
	 *
	 * @return    The frameworkPlugin value
	 */
	public CATHelperPlugin getFrameworkPlugin() {
		return this.frameworkPlugin;
	}


	/**
	 *  Sets the frameworkPlugin attribute of the CATServiceHelper object
	 *
	 * @param  plugin  The new frameworkPlugin value
	 */
	protected void setFrameworkPlugin(CATHelperPlugin plugin) {
		this.frameworkPlugin = plugin;
	}


	/**
	 *  Gets the standardsManager attribute of the CATServiceHelper object
	 *
	 * @return    The standardsManager value
	 */
	public StandardsManager getStandardsManager() {
		if (this.getActionForm() == null)
			prtlnErr("getStandardsManager: actionForm is unavailable");
		else if (this.getActionForm().getFramework() == null)
			prtlnErr("getStandardsManager: framework is unavailable");
		return this.getActionForm().getFramework().getStandardsManager();
	}


	/**
	 *  Gets the rootStandardNode attribute of the CATServiceHelper object
	 *
	 * @return    The rootStandardNode value
	 */
	public StandardsNode getRootStandardNode() {
		return this.getStandardsDocument().getRootNode();
	}


	/**
	 *  Gets a list of all standardsNodes in the current StandardsDocument in standards document order.
	 *
	 * @return    The standardsNodes value
	 */
	public List getStandardsNodes() {
		return this.getStandardsDocument().getNodeList();
	}


	/**
	 *  Resolves author from the asnDocument (which it gets from the
	 *  StandardsDocument)
	 *
	 * @return    The author value
	 */
	public String getAuthor() {
		String author = this.getStandardsDocument().getAuthor();
		// KLUDGES to convert between ASN and CAT representations for author
		if ("NSES".equals(author))
			author = "National Science Education Standards (NSES)";
		if ("AAAS".equals(author))
			author = "American Association of Advancement of Science Project 2061";
		return author;
	}


	/**
	 *  Resolves topic for this asnDocument asnDocument (which it gets from the
	 *  StandardsDocument)
	 *
	 * @return    The topic value
	 */
	public String getTopic() {
		return this.getStandardsDocument().getTopic();
	}


	/**
	 *  Gets the availableDocs attribute of the CATServiceHelper object
	 *
	 * @return    The availableDocs value
	 */
	public List getAvailableDocs() {
		return new ArrayList();
	}


	/**
	 *  Gets the currentDoc attribute of the CATServiceHelper object
	 *
	 * @return    The currentDoc value
	 */
	public String getCurrentDoc() {
		return null;
	}


	/**
	 *  Gets the instance attribute of the CATServiceHelper class
	 *
	 * @param  sef            Description of the Parameter
	 * @return                The instance value
	 * @exception  Exception  Description of the Exception
	 */
	public static CATServiceHelper getInstance(SchemEditForm sef)
		 throws Exception {

		javax.servlet.ServletContext servletContext = sef.getServletContext();

		SuggestionServiceManager suggestionServiceManager =
			(SuggestionServiceManager) servletContext.getAttribute("suggestionServiceManager");
		if (suggestionServiceManager == null) {
			throw new Exception("SuggestionServiceManager not found in servlet context");
		}

		SuggestionServiceConfig config = suggestionServiceManager.getConfig(sef.getXmlFormat());
		if (config == null) {
			throw new Exception("config not found for framework: " + sef.getXmlFormat());
		}

		String pluginClassName = config.getPluginClass();
		// prtln("Suggestion service plugin ClassName: " + pluginClassName);

		try {

			Class pluginClass = Class.forName(pluginClassName);
			CATHelperPlugin plugin = (CATHelperPlugin) pluginClass.newInstance();
			if (plugin == null) {
				throw new Exception("did not instantate plugin");
			}
			///Users/ostwald/devel/projects/dlese-tools-project/src/org/dlese/dpc/schemedit/standards/adn/DleseSuggestionServiceHelper.java

			if (sef == null)
				prtlnErr("getInstance() : actionForm not available");

			CATServiceHelper helper = null;
			if (sef.getXmlFormat().equals("adn"))
				helper = new DleseSuggestionServiceHelper(sef, plugin);
			else if (sef.getXmlFormat().equals("res_qual")) {
				/* res_qual is a special case since the CAT UI is embedded in
				   a complex node, rather than implemented as a leaf
				*/
				helper = new ResQualSuggestionServiceHelper(sef, plugin);
			}
			else if (sef.getXmlFormat().equals("comm_core")) {
				helper = new CommCoreServiceHelper(sef, plugin);
			}
			else if (sef.getXmlFormat().equals("td_demo")) {
				helper = new TeachersDomainServiceHelper(sef, plugin);
			}
			else {
				helper = new AsnSuggestionServiceHelper(sef, plugin);
			}
			if (helper == null) {
				throw new Exception("did not instantate helper");
			}

			/* 			prtln("instantiated, now initializing...");
			helper.init(sef, plugin); */
			return helper;
		} catch (Throwable e) {
			String errMsg = "Error loading SuggestionServiceHelper class '" + pluginClassName + "'. " + e;
			e.printStackTrace();
			throw new Exception(errMsg);
		}
	}


	/**
	 *  Gets the actionForm attribute of the CATServiceHelper object
	 *
	 * @return    The actionForm value
	 */
	public SchemEditForm getActionForm() {
		return this.sef;
	}


	/**
	 *  Gets the framework attribute of the CATServiceHelper object
	 *
	 * @return    The framework value
	 */
	protected MetaDataFramework getFramework() {
		return this.framework;
	}


	/**
	 *  Gets the document attribute of the CATServiceHelper object
	 *
	 * @return    The document value
	 */
	protected Document getInstanceDocument() {
		return this.getActionForm().getDocMap().getDocument();
	}


	/**
	 *  Gets the serviceIsActive attribute of the CATServiceHelper object
	 *
	 * @return    true if the SuggestionService is available
	 */
	public boolean getServiceIsActive() {
		return this.serviceIsActive;
	}


	/**
	 *  Sets the serviceIsActive attribute of the CATServiceHelper object
	 *
	 * @param  b  The new serviceIsActive value
	 */
	public void setServiceIsActive(boolean b) {
		this.serviceIsActive = b;
	}


	/**
	 *  Gets the xpath of the metadata element containing the managedStandards
	 *
	 * @return    The xpath value
	 */
	public String getXpath() {
		return this.getStandardsManager().getXpath();
	}

	/**
	 *  Gets the xmlFormat attribute of the CATServiceHelper object
	 *
	 * @return    The xmlFormat value
	 */
	public String getXmlFormat() {
		return this.getFramework().getXmlFormat();
	}



	/**
	 *  Gets the suggested Standards represented in ADN format.
	 *
	 * @return    The suggestedStandards value
	 */
	public List getSuggestedStandards() {
		if (suggestedStandards == null) {
			suggestedStandards = new ArrayList();
		}
		return suggestedStandards;
	}


	/**
	 *  Gets the numSelectedStandards attribute of the CATServiceHelper object
	 *
	 * @return    The numSelectedStandards value
	 */
	public int getNumSelectedStandards() {
		return this.getSelectedStandards().size();
	}


	/**
	 *  Retrieves a list of selected standards that can be fed into the
	 *  "moreLikeThis" method.
	 *
	 * @return    The selectedStandards value
	 */
	public List getSelectedStandards() {
		return getSelectedStandards(this.getInstanceDocument());
	}


	/**
	 *  Retrieves a list of selected standards from the provided instance document
	 *  that can be fed into the "moreLikeThis" method.<P>
	 *
	 *  NOTE: used to support asyncronous calls, in which the instance document of
	 *  the formBean may not reflect the most recent values.
	 *
	 * @param  doc  the instance document
	 * @return      The selectedStandards as a list of standards IDS
	 */
	protected List getSelectedStandards(Document doc) {
		// prtln ("getSelectedStandards (" + this.getXpath() + ")");
		List nodes = doc.selectNodes(this.getXpath());
		if (nodes == null) {
			prtln("\tunable to find any nodes for " + this.getXpath());
			return new ArrayList();
		}

		List selectedStandards = new ArrayList();
		for (Iterator i = nodes.iterator(); i.hasNext(); ) {
			Node node = (Node) i.next();
			String id = node.getText();

			if (id != null
				 && id.trim().length() > 0
				 && !selectedStandards.contains(id)) {
				selectedStandards.add(id);
			}
		}
		return selectedStandards;
	}


	/**
	 *  Gets the otherSelectedStandards attribute of the CATServiceHelper object
	 *
	 * @return    The otherSelectedStandards value
	 */
	public Map getOtherSelectedStandards() {
		return new HashMap();
	}


	/**
	 *  Sets the suggestedStandards attribute of the CATServiceHelper object
	 *
	 * @param  stds  The new suggestedStandards value
	 */
	public void setSuggestedStandards(List stds) {
		suggestedStandards = stds;
	}


	/**
	 *  Specifies whether user has chosen to constrain suggestion service using
	 *  current value of description field in the current metadata record.
	 *
	 * @return    true if description contstraint is to be used.
	 */
	public boolean getUseDescription() {
		return useDescription;
	}


	/**
	 *  Sets the useDescription attribute of the CATServiceHelper object
	 *
	 * @param  useIt  The new useDescription value
	 */
	public void setUseDescription(boolean useIt) {
		useDescription = useIt;
	}


	/**
	 *  Specifies whether user has chosen to constrain suggestion service using
	 *  current value of keywords box in the suggestion service interface.
	 *
	 * @return    The useKeywords value
	 */
	public boolean getUseKeywords() {
		return useKeywords;
	}


	/**
	 *  Sets the useKeywords attribute of the CATServiceHelper object
	 *
	 * @param  useIt  The new useKeywords value
	 */
	public void setUseKeywords(boolean useIt) {
		useKeywords = useIt;
	}


	/**
	 *  Specifies whether user has chosen to constrain suggestion service using
	 *  current value of keywords box in the suggestion service interface.
	 *
	 * @return    The useSubjects value
	 */
	public boolean getUseSubjects() {
		return useSubjects;
	}


	/**
	 *  Sets the useSubjects attribute of the CATServiceHelper object
	 *
	 * @param  useIt  The new useSubjects value
	 */
	public void setUseSubjects(boolean useIt) {
		useSubjects = useIt;
	}


	/**
	 *  Specifies whether user has chosen to constrain suggestion service using
	 *  current value of gradeRange checkboxes in the suggestion service interface.
	 *
	 * @return    The useGradeRanges value
	 */
	public boolean getUseGradeRanges() {
		return useGradeRanges;
	}


	/**
	 *  Sets the useGradeRanges attribute of the CATServiceHelper object
	 *
	 * @param  useIt  The new useGradeRanges value
	 */
	public void setUseGradeRanges(boolean useIt) {
		useGradeRanges = useIt;
	}


	/**
	 *  Determines whether standards are displayed as a heirarchical tree or flat
	 *  list.
	 *
	 * @return    LIST_MODE for list mode, TREE_MODE for tree mode.
	 */
	public String getDisplayMode() {
		return displayMode;
	}


	/**
	 *  Sets the displayMode attribute to either TREE_MODE (to display all
	 *  standards) or LIST_MODE (to display either suggested or selected
	 *  standards);
	 *
	 * @param  mode  The new displayMode value
	 */
	public void setDisplayMode(String mode) {
		if (mode != TREE_MODE) {
			displayMode = (mode.equals(LIST_MODE)) ? LIST_MODE : TREE_MODE;
		}
	}


	/**
	 *  Determines what standards to display (SUGGESTED_CONTENT, STANDARDS_CONTENT,
	 *  BOTH, ALL)
	 *
	 * @return    The displayContent value
	 */
	public String getDisplayContent() {
		return displayContent;
	}


	/**
	 *  Sets the displayContent attribute of the CATServiceHelper object
	 *
	 * @param  content  The new displayContent value
	 */
	public void setDisplayContent(String content) {
		displayContent = content;
	}


	/**
	 *  Gets the url attribute of the CATServiceHelper object
	 *
	 * @return    The url value
	 */
	public String getUrl() {
		if (url == null || url.trim().length() == 0) {
			return getRecordUrl();
		}
		else {
			return url;
		}
	}


	/**
	 *  Sets the url attribute of the CATServiceHelper object
	 *
	 * @param  url  The new url value
	 */
	public void setUrl(String url) {
		this.url = url;
	}


	/**
	 *  Gets a list specifying what optional fields (e.g., "subject", "keyword") is
	 *  exported by this framework. The UI controls for these fields will be
	 *  active.
	 *
	 * @return    The optionalCatUIFields value
	 */
	public List getOptionalCatUIFields() {
		return this.frameworkPlugin.getOptionalCatUIFields();
	}


	/**
	 *  Gets the gradeRanges assigned for the current record
	 *
	 * @return    The recordGradeRanges value
	 */
	public String[] getRecordGradeRanges() {
		return this.frameworkPlugin.getRecordGradeRanges();
	}


	/**
	 *  Gets the GradeRanges curently selected in the CAT UI.
	 *
	 * @return    The selectedGradeRanges value
	 */
	public String[] getSelectedGradeRanges() {
		if (selectedGradeRanges == null) {
			prtln("\nWARNING: there are no selected GradeRanges!");
			// selectedGradeRanges = this.getRecordGradeRanges(); // old PROBLEMATIC way
			// selectedGradeRanges = new String[]{};
		}
		return selectedGradeRanges;
	}


	/**
	 *  Gets the gradeRangeOptionValue corresponding to the lowest selected
	 *  gradeRange in the current instance document.<p>
	 *
	 *  NOTE: this requires converting from possible gradeRange metadata values to
	 *  the values supplied for gradeRangeOptions.
	 *
	 * @return    The startGradeOptionValue value
	 */
	public String getDerivedCATStartGrade() {
		return this.frameworkPlugin.getDerivedCATStartGrade();
	}


	/**
	 *  Gets the gradeRangeOptionValue corresponding to the highest selected
	 *  gradeRange in the current instance document.<p>
	 *
	 *  NOTE: this requires converting from possible gradeRange metadata values to
	 *  the values supplied for gradeRangeOptions.
	 *
	 * @return    The endGrade value
	 */
	public String getDerivedCATEndGrade() {
		return this.frameworkPlugin.getDerivedCATEndGrade();
	}



	/**
	 *  Sets the selectedGradeRanges attribute of the CATServiceHelper object
	 *
	 * @param  grs  The new selectedGradeRanges value
	 */
	public void setSelectedGradeRanges(String[] grs) {
		selectedGradeRanges = grs;
	}


	/**
	 * @return    The keywordWeighting value
	 */
	public int getKeywordWeighting() {
		if (keywordWeighting < 1) {
			keywordWeighting = 1;
		}
		return keywordWeighting;
	}


	/**
	 *  Keywordweighting specifies how many times the keyword contents are repeated
	 *  before they are inserted into the keyword field of the Contraint instance.
	 *
	 * @param  weight  The new keywordWeighting value
	 */
	public void setKeywordWeighting(int weight) {
		keywordWeighting = weight;
	}


	/**
	 *  Gets the gradeRanges corresponding to the grades searchable in the current
	 *  framework
	 *
	 * @return    The gradeRanges value
	 */
	public List getGradeRangeOptions() {
		try {
			return this.frameworkPlugin.getGradeRangeOptions();
		} catch (Throwable t) {
			prtln("getGradeRangeOptions error: " + t.getMessage());
			t.printStackTrace();
		}
		return new ArrayList();
	}


	/**
	 *  Gets the value of the description field of the current metadata record
	 *
	 * @return    The description
	 */
	public String getRecordDescription() {
		return this.frameworkPlugin.getRecordDescription();
	}


	/**
	 *  Gets the value of the description field of the current metadata record
	 *
	 * @return    The keywords
	 */
	public String[] getRecordKeywords() {
		/* 		String path = this.frameworkPlugin.getKeywordPath();
		if (path != null && path.trim().length() > 0) {
			return sef.getEnumerationValuesOf(path);
		}
		else {
			return null;
		} */
		return this.frameworkPlugin.getRecordKeywords();
	}


	/**
	 *  Gets the recordSubjects attribute of the CATServiceHelper object
	 *
	 * @return    The recordSubjects value
	 */
	public String[] getRecordSubjects() {
		/* 		String path = this.frameworkPlugin.getKeywordPath();
		if (path != null && path.trim().length() > 0) {
			return sef.getEnumerationValuesOf(path);
		}
		else {
			return null;
		} */
		return this.frameworkPlugin.getRecordSubjects();
	}


	/**
	 *  Gets the keywords currently selected in the service controls.
	 *
	 * @return    The selectedKeywords value
	 */
	public String getSelectedKeywords() {
		if (selectedKeywords == null) {
			String[] recordKeywords = getRecordKeywords();
			String rkws = "";
			if (recordKeywords != null) {
				for (int i = 0; i < recordKeywords.length; i++) {
					rkws += recordKeywords[i];
					if (i < recordKeywords.length - 1) {
						rkws += ", ";
					}
				}
				selectedKeywords = rkws;
			}
		}
		return selectedKeywords;
	}


	/**
	 *  Sets the selectedKeywords attribute of the CATServiceHelper object
	 *
	 * @param  kws  The new selectedKeywords value
	 */
	public void setSelectedKeywords(String kws) {
		selectedKeywords = kws;
	}


	/**
	 *  Gets the recordUrl specified in the instance document (via the {@link
	 *  SchemEditForm}).<p>
	 *
	 *  Note: this method depends on the url path being defined in the framework
	 *  configuration.
	 *
	 * @return    The recordUrl value
	 */
	public String getRecordUrl() {

		String url = "";
		try {
			String urlPath = framework.getNamedSchemaPath("url").xpath;
			if (urlPath != null) {
				url = (String) sef.getValueOf(urlPath);
			}
		} catch (Exception e) {
			prtln(e.getMessage());
		}
		return url;
	}


	/*  ----------------------------------------------------------
		methods for communicating with the CAT Service
		----------------------------------------------------------	*/
	/**
	 *  Description of the Method
	 *
	 * @exception  Exception  Description of the Exception
	 */
	public void updateSuggestions() throws Exception {
		updateSuggestions(null);
	}


	/**
	 *  Performs a query on the suggestion server using the current contraints, and
	 *  updates the suggestedStandards attribute of the CATServiceHelper with the
	 *  results, which is a list of standards IDS.
	 *
	 * @param  feedbackStandards  Description of the Parameter
	 * @exception  Exception      NOT YET DOCUMENTED
	 */
	public void updateSuggestions(List feedbackStandards) throws Exception {
		String url = getUrl();
		if (url == null || url.trim().length() == 0) {
			throw new Exception("updateSuggestions could not obtain url");
		}

		CATRequestConstraints constraints = getConstraints();
		constraints.setQuery(url);
		if (feedbackStandards != null) {
			prtln(feedbackStandards.size() + " feedbackStandards added to contraints");
			constraints.setFeedbackStandards(feedbackStandards);
		}
		prtln("\n------------------------------------------");
		prtln(constraints.toString());
		
		CATServiceToolkit cat = new CATServiceToolkit();
		List standardsList = cat.getSuggestions(constraints);
		prtln(standardsList.size() + " items returned by suggestion service");

		// prtln("framework.xmlFormat is " + this.getXmlFormat());
		// prtln ("standardsMapper source: " + this.standardsMapper.getSource());

		List suggested = new ArrayList();
		for (Iterator i = standardsList.iterator(); i.hasNext(); ) {
			CATStandard std = (CATStandard) i.next();
			String id = getIdFromCATStandard(std);
			if (id != null && id.trim().length() > 0) {
				suggested.add(id);
				if (!this.getXmlFormat().equals("adn")) // adn ids are too verbose!
					prtln("\t" + id);
			}
			else {
				prtln("couldn't map Id from CAT Standard: " + std.getIdentifier());
			}
		}
		this.setSuggestedStandards(suggested);
	}


	/**
	 *  Update the suggestions based on the currently selected standards and the
	 *  current suggestionConstraints.
	 *
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public void moreLikeThis() throws Exception {
		prtln("\nmoreLikeThis()");
		List feedbackStandards = this.getSelectedStandards();
		prtln("\t there are " + feedbackStandards.size() + " selected standards");
		updateSuggestions(feedbackStandards);
	}


	/**
	 *  Build a MetadataContraint instance from current CATServiceHelper
	 *  attributes, including keywords, description, gradeRanges (when the
	 *  corresponding "use" attributes return true).<p>
	 *
	 *  NOTE: Author and Topic hardcoded to "National Science Education Standards
	 *  (NSES)" and "Science", respectively
	 *
	 * @return    A contraint for a SuggestionService Search request.
	 */
	protected CATRequestConstraints getConstraints() {
		CATRequestConstraints constraints = new CATRequestConstraints();
 		constraints.setAuthor(getAuthor());
		constraints.setTopic(getTopic());
		
		String docId = AsnDocKey.makeAsnDocKey(this.getCurrentDoc()).getAsnId();
		constraints.addStandardDocument(docId);
		
		prtln ("getContraints: " + this.getCurrentDoc());
		prtln ("docId: " + docId);
		
		String additionalText = (constraints.getKeywords() == null) ? "" : constraints.getKeywords();
		if (this.getUseDescription()) {
			additionalText += " " + getRecordDescription();
		}
		if (this.getUseKeywords() && getSelectedKeywords() != null) {
			for (int i = 0; i < getKeywordWeighting(); i++) {
				additionalText += " " + getSelectedKeywords();
			}
		}

		if (this.getUseSubjects() && getRecordSubjects() != null) {
			String[] subjects = getRecordSubjects();
			if (subjects != null && subjects.length > 0) {
				for (int i = 0; i < getKeywordWeighting(); i++) {
					for (int ii = 0; ii < subjects.length; ii++)
						additionalText += " " + subjects[ii];
				}
			}
		}

		constraints.setKeywords(additionalText);
		constraints.setStartGrade(this.getFrameworkPlugin().getSelectedCATStartGrade(this.getSelectedGradeRanges()));
		constraints.setEndGrade(this.getFrameworkPlugin().getSelectedCATEndGrade(this.getSelectedGradeRanges()));
		return constraints;
	}


	/**
	 *  Gets the idFromCATStandard attribute of the CATServiceHelper object
	 *
	 * @param  std  NOT YET DOCUMENTED
	 * @return      The idFromCATStandard value
	 */
	protected abstract String getIdFromCATStandard(CATStandard std);


	/*  ----------------------------------------------------------
		Methods for updating standards display and service controls
		----------------------------------------------------------	*/
	/**
	 *  Sets various control attributes from the information contained in the
	 *  request, such as displayMode, displayContent, and whether description,
	 *  grade ranges, subjects, keywords, will be used in the CAT Service request.
	 *
	 * @param  request        NOT YET DOCUMENTED
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public void updateDisplayControls(HttpServletRequest request)
		 throws Exception {
		String pathArg = request.getParameter("pathArg");
		String command = request.getParameter("command");
		try {

			String displayMode = request.getParameter("displayMode");
			if (displayMode != null) {
				setDisplayMode(displayMode);
			}

			String displayContent = request.getParameter("displayContent");
			if (displayContent != null) {
				setDisplayContent(displayContent);
			}

			String useDescription = request.getParameter("useDescription");
			if (useDescription != null && useDescription.trim().length() > 0) {
				setUseDescription(true);
			}
			else {
				setUseDescription(false);
			}

			String useKeywords = request.getParameter("useKeywords");
			if (useKeywords != null && useKeywords.trim().length() > 0) {
				setUseKeywords(true);
			}
			else {
				setUseKeywords(false);
			}

			String keywords = request.getParameter("keywords");
			setSelectedKeywords((keywords == null) ? "" : keywords);

			String useGradeRanges = request.getParameter("useGradeRanges");
			if (useGradeRanges != null && useGradeRanges.trim().length() > 0) {
				setUseGradeRanges(true);
			}
			else {
				setUseGradeRanges(false);
			}

			String[] gradeRanges = request.getParameterValues("gradeRanges");
			setSelectedGradeRanges((gradeRanges == null) ? new String[]{} : gradeRanges);

			String useSubjects = request.getParameter("useSubjects");
			if (useSubjects != null && useSubjects.trim().length() > 0) {
				setUseSubjects(true);
			}
			else {
				setUseSubjects(false);
			}

		} catch (Throwable t) {
			String errorMsg = "updateDisplayControls ERROR: " + t.getMessage();
			throw new Exception(errorMsg);
		}
	}


	/**
	 *  Initialize the collapse bean to show selected and suggested standards nodes
	 *  in the display specified by "displayContent".
	 *
	 * @param  displayContent  Description of the Parameter
	 * @exception  Exception   NOT YET DOCUMENTED
	 */
	public void updateStandardsDisplay(String displayContent) throws Exception {
		// prtln ("updateStandardsDisplay()");
		CollapseBean cb = this.sef.getCollapseBean();
		StandardsDocument standardsTree = this.getStandardsDocument();

		if (standardsTree == null)
			throw new Exception("standardsTree not found");

		// close all before opening desired (this may not be what we want to do ...)
		for (Iterator i = standardsTree.getNodeList().iterator(); i.hasNext(); ) {
			StandardsNode node = (StandardsNode) i.next();
			if (node.getHasSubList()) {
				String key = CollapseUtils.pairToId(this.getXpath(), node.getId());
				cb.closeElement(key);
			}
		}

		// expose selected nodes
		if (displayContent.equalsIgnoreCase("selected") || displayContent.equalsIgnoreCase("both")) {
			// prtln ("\nexposing selected nodes");
			String[] selected = sef.getEnumerationValuesOf(this.getXpath());
			for (int i = 0; i < selected.length; i++) {
				String id = selected[i];
				StandardsNode node = standardsTree.getStandard(id);
				// prtln ("id: " + id);
				if (node == null) {
					if (!(this.getStandardsManager() instanceof
						org.dlese.dpc.schemedit.standards.asn.DynaStandardsManager)) {
						// when multiple standards docs are being used, a selected node may exist in a different tree
						prtln("WARNING: selected node not found for \"" + id +
							"\" in standardsTree for " + this.getCurrentDoc());
					}
					continue;
				}
				List ancestors = node.getAncestors();
				for (Iterator a = ancestors.iterator(); a.hasNext(); ) {
					StandardsNode ancestor = (StandardsNode) a.next();
					String key = CollapseUtils.pairToId(this.getXpath(), ancestor.getId());
					cb.openElement(key);
				}
			}
			this.sef.exposeNode(XPathUtils.decodeXPath(this.getXpath()));
		}

		// expose suggested nodes
		if (displayContent.equalsIgnoreCase("suggested") || displayContent.equalsIgnoreCase("both")) {
			// prtln ("\nexposing suggested nodes");
			List suggested = getSuggestedStandards();
			for (Iterator s = suggested.iterator(); s.hasNext(); ) {
				String id = (String) s.next();
				// prtln ("id: " + id);
				StandardsNode node = standardsTree.getStandard(id);
				if (node == null) {
					// prtln("WARNING: node not found for \"" + id + "\"");
					continue;
				}
				List ancestors = node.getAncestors();
				for (Iterator a = ancestors.iterator(); a.hasNext(); ) {
					StandardsNode ancestor = (StandardsNode) a.next();
					String key = CollapseUtils.pairToId(this.getXpath(), ancestor.getId());
					cb.openElement(key);
				}
			}
			this.sef.exposeNode(XPathUtils.decodeXPath(this.getXpath()));
		}
		// prtln ("   ... done with updateStandardsDisplay()");
	}


	/**
	 *  Debugging
	 *
	 * @param  standardsList  A list of StandardsWrapper instances to display.
	 */
	public void displaySuggestions(List standardsList) {
		prtln(standardsList.size() + " items returned by suggestion service ...");
		for (int i = 0; i < standardsList.size(); i++) {
			CATStandard std = (CATStandard) standardsList.get(i);
			String id = std.getIdentifier();
			String text = std.getText();
			prtln("\n" + i + "\n" + id + "\n" + text + "\n");
		}
	}


	/**
	 *  Print a line to standard out.
	 *
	 * @param  s  The String to print.
	 */
	private static void prtln(String s) {
		if (debug) {
			SchemEditUtils.prtln(s, "CATServiceHelper");
		}
	}


	private static void prtlnErr(String s) {
		SchemEditUtils.prtln(s, "CATServiceHelper");
	}

}

