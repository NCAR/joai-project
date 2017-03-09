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
package org.dlese.dpc.repository;

import org.dlese.dpc.util.Files;
import org.dlese.dpc.repository.action.form.SetDefinitionsForm;
import org.dlese.dpc.xml.Dom4jUtils;
import org.dlese.dpc.xml.Dom4jNodeListComparator;


import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.io.SAXReader;
import org.dom4j.Element;
import org.dom4j.Attribute;
import org.dom4j.Node;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;

import org.apache.lucene.queryParser.*;
import org.apache.lucene.analysis.standard.StandardAnalyzer;

import java.io.*;
import java.text.*;
import java.util.*;

/**
 *  Reads and writes the XML configuration file used to define OAI sets in the {@link RepositoryManager}. See
 *  <a href="../../../../javadoc-includes/ListSets-config-sample.xml">sample ListSets XML config file</a> .
 *
 * @author     John Weatherley
 * @version    $Id: OAISetsXMLConfigManager.java,v 1.8 2009/03/20 23:33:54 jweather Exp $
 */
public class OAISetsXMLConfigManager {
	private static boolean debug = true;


	OAISetsXMLConfigManager() { }


	/**
	 *  Reads the ListSets config XML to extract the set definition for a given set into a SetDefinitionsForm
	 *  bean.
	 *
	 * @param  listSetsXml    The ListSets config XML to read
	 * @param  setSpec        The setSpec to read
	 * @return                The setDefinitionsForm, or null if none configred for that setSpec
	 * @exception  Exception  If error parsing the XML
	 */
	public static SetDefinitionsForm getSetDefinitionsForm(String listSetsXml, String setSpec)
		 throws Exception {

		Document document = Dom4jUtils.getXmlDocument(listSetsXml);
		Element setElement = (Element) document.selectSingleNode("/ListSets/set[setSpec='" + setSpec + "']");
		if (setElement == null)
			return null;

		SetDefinitionsForm setDefinitionsForm = new SetDefinitionsForm();
		setDefinitionsForm.setSetName(setElement.valueOf("setName"));
		setDefinitionsForm.setSetSpec(setElement.valueOf("setSpec"));
		setDefinitionsForm.setSetDescription(setElement.valueOf("setDescription/description"));
		setDefinitionsForm.setSetURL(setElement.valueOf("setDescription/identifier"));
		setDefinitionsForm.setIncludedFormat(setElement.valueOf("virtualSearchField/virtualSearchTermDefinition/Query//booleanQuery/textQuery[@field='xmlFormat']"));
		setDefinitionsForm.setIncludedQuery(setElement.valueOf("virtualSearchField/virtualSearchTermDefinition/Query/booleanQuery/luceneQuery[not(@excludeOrRequire='exclude')]"));
		setDefinitionsForm.setExcludedQuery(setElement.valueOf("virtualSearchField/virtualSearchTermDefinition/Query/booleanQuery/luceneQuery[@excludeOrRequire='exclude']"));

		// Handle include clauses

		// Get included dirs
		List includedDirs = setElement.selectNodes("virtualSearchField/virtualSearchTermDefinition/Query//booleanQuery/textQuery[@field='docdir' and not(@excludeOrRequire='exclude')]");
		ArrayList dirsList = new ArrayList(includedDirs.size());
		for (int i = 0; i < includedDirs.size(); i++)
			dirsList.add( ((Node) includedDirs.get(i)).getText() );
		setDefinitionsForm.setIncludedDirs((String[])dirsList.toArray(new String[]{}));

		// Get included terms/phrases
		List terms = setElement.selectNodes("virtualSearchField/virtualSearchTermDefinition/Query//booleanQuery/textQuery[@field='default' and not(@excludeOrRequire='exclude')]");
		String text = "";
		prtln("terms size: " + terms.size());
		for (int i = 0; i < terms.size(); i++) {
			text += ((Node) terms.get(i)).getText();
			if (i < terms.size() - 1)
				text += ", ";
		}
		setDefinitionsForm.setIncludedTerms(text);

		// Handle exclude clauses

		// Get excluded dirs
		List exdludedDirs = setElement.selectNodes("virtualSearchField/virtualSearchTermDefinition/Query/booleanQuery/textQuery[@field='docdir' and @excludeOrRequire='exclude']");
		dirsList = new ArrayList(exdludedDirs.size());
		for (int i = 0; i < exdludedDirs.size(); i++)
			dirsList.add( ((Node) exdludedDirs.get(i)).getText() );
		setDefinitionsForm.setExcludedDirs((String[])dirsList.toArray(new String[]{}));
		
		/* dirStrings = setDefinitionsForm.getExcludedDirs();
		for (int i = 0; i < exdludedDirs.size(); i++)
			dirStrings[i] = ((Node) exdludedDirs.get(i)).getText();
		setDefinitionsForm.setExcludedDirs(dirStrings); */

		// Get excluded terms/phrases
		terms = setElement.selectNodes("virtualSearchField/virtualSearchTermDefinition/Query/booleanQuery/textQuery[@field='default' and @excludeOrRequire='exclude']");
		text = "";
		for (int i = 0; i < terms.size(); i++) {
			text += ((Node) terms.get(i)).getText();
			if (i < terms.size() - 1)
				text += ", ";
		}
		setDefinitionsForm.setExcludedTerms(text);

		//prtln("setDefinitionsForm.getSetName(): " + setDefinitionsForm.getSetName());
		return setDefinitionsForm;
	}


	/**
	 *  Removes the given OAI set definition from the ListSets config XML file and writes it to disc.
	 *
	 * @param  setsConfigFile  The ListSets config file
	 * @param  setSpec         The set to remove
	 * @return                 True if the set existed and was removed
	 * @exception  Exception   If error
	 */
	public static boolean removeOAISetSpecDefinition(File setsConfigFile, String setSpec) throws Exception {
		Document document = null;
		if (setsConfigFile == null || !setsConfigFile.exists())
			return false;

		// Create the XML DOM
		if (setsConfigFile.exists()) {
			SAXReader reader = new SAXReader();
			document = reader.read(setsConfigFile);
		}

		Element root = document.getRootElement();
		if (!root.getName().equals("ListSets"))
			throw new Exception("OAI Sets XML is incorrect. Root node is not 'ListSets'");

		// Remove the previous set definition, if present
		String xPath = "set[setSpec=\"" + setSpec + "\"]";
		Element prevSet = (Element) root.selectSingleNode(xPath);
		if (prevSet == null)
			return false;
		root.remove(prevSet);

		// Write the XML to disc
		OutputFormat format = OutputFormat.createPrettyPrint();
		XMLWriter writer = new XMLWriter(new FileWriter(setsConfigFile), format);
		writer.write(document);
		writer.close();

		return true;
	}


	/**
	 *  Sets the definition for a given OAI set, writing the ListSets config XML to file. If the given set
	 *  already exists, it will be re-defined, if not, a new set will be added to the existsing sets in the
	 *  config XML file.
	 *
	 * @param  setsConfigFile  File to read/write
	 * @param  sb              The bean that contains the set definition values
	 * @exception  Exception   If error
	 */
	public static void setOAISetSpecDefinition(File setsConfigFile, SetDefinitionsForm sb) throws Exception {

		String setName = sb.getSetName();
		String setSpec = sb.getSetSpec();
		String setDescription = sb.getSetDescription();
		String setUrl = sb.getSetURL();
		String includedFormat = sb.getIncludedFormat();
		String[] includedTermsPhrases = null;
		if(sb.getIncludedTerms() != null) 
			includedTermsPhrases = sb.getIncludedTerms().split(",");
		String[] includedDirs = sb.getIncludedDirs();
		String includedQuery = sb.getIncludedQuery();
		String[] excludedTermsPhrases = null;
		if(sb.getExcludedTerms() != null)
			excludedTermsPhrases = sb.getExcludedTerms().split(",");
		String[] excludedDirs = sb.getExcludedDirs();
		String excludedQuery = sb.getExcludedQuery();

		prtln("setOAISetSpecDefinition(): setSpec:" + setSpec);

		// Error checking:
		if (setsConfigFile == null)
			throw new Exception("setsConfigFile is null");
		if (setSpec == null || setSpec.trim().length() == 0)
			throw new Exception("setSpec must not be empty or null");
		if (setName == null || setName.trim().length() == 0)
			throw new Exception("setName must not be empty or null");

		XMLWriter writer = null;
		try {
			Document document = null;

			// Create the XML DOM
			if (setsConfigFile.exists()) {
				SAXReader reader = new SAXReader();
				document = reader.read(setsConfigFile);
			}
			else {
				document = DocumentHelper.createDocument();
				document.addElement("ListSets");
			}

			Element root = document.getRootElement();
			if (!root.getName().equals("ListSets"))
				throw new Exception("OAI Sets XML is incorrect. Root node is not 'ListSets'");

			// Remove the previous set definition, if present
			String xPath = "set[setSpec=\"" + setSpec + "\"]";
			Element prevSet = (Element) root.selectSingleNode(xPath);
			if (prevSet != null)
				root.remove(prevSet);

			// Add the set Element
			prtln("Adding new set def for '" + setSpec + "'");
			Element set = root.addElement("set");
			set.addElement("setSpec").addText(setSpec.trim());

			if (setName != null && setName.trim().length() > 0)
				set.addElement("setName").addText(setName.trim());

			// Add description and url, if present
			if ((setDescription != null && setDescription.trim().length() > 0) || (setUrl != null && setUrl.trim().length() > 0)) {
				Element setDescriptionElement = set.addElement("setDescription");
				if (setDescription != null && setDescription.trim().length() > 0)
					setDescriptionElement.addElement("description").addText(setDescription.trim());
				if (setUrl != null && setUrl.trim().length() > 0)
					setDescriptionElement.addElement("identifier").addText(setUrl.trim());
			}

			// Add the virtualSearchField
			Element virtualSearchField = set.addElement("virtualSearchField").addAttribute("field", "setSpec");
			Element virtualSearchTermDefinition = virtualSearchField.addElement("virtualSearchTermDefinition").addAttribute("term", setSpec.trim());

			// Add the Query
			Element query = virtualSearchTermDefinition.addElement("Query");
			List queryClauses = new ArrayList();

			// Add included terms and phrase clauses
			if (includedTermsPhrases != null) {
				List includedTermsPhrasesClauses = new ArrayList();
				for (int i = 0; i < includedTermsPhrases.length; i++) {
					if (includedTermsPhrases[i] != null && includedTermsPhrases[i].trim().length() > 0) {
						String type = "matchAnyTerm";
						if (includedTermsPhrases[i].trim().indexOf(' ') > 0)
							type = "matchPhrase";
						includedTermsPhrasesClauses.add(createTextQueryElement("default", type, includedTermsPhrases[i].trim()));
					}
				}
				if (includedTermsPhrasesClauses.size() > 0) {
					Element element = createBooleanQueryElement("OR", includedTermsPhrasesClauses);
					element.addAttribute("excludeOrRequire", "require");
					queryClauses.add(element);
				}
			}

			// Add included XML format
			if (includedFormat != null && includedFormat.trim().length() > 0) {
				Element element = createTextQueryElement("xmlFormat", "matchKeyword", includedFormat);
				element.addAttribute("excludeOrRequire", "require");
				queryClauses.add(element);
			}

			// Add included directories
			if (includedDirs != null) {
				List includedDirsClauses = new ArrayList(includedDirs.length);
				for (int i = 0; i < includedDirs.length; i++) {
					if (includedDirs[i] != null && includedDirs[i].trim().length() > 0)
						includedDirsClauses.add(createTextQueryElement("docdir", "matchKeyword", includedDirs[i].trim()));
				}
				if (includedDirsClauses.size() > 0) {
					Element element = createBooleanQueryElement("OR", includedDirsClauses);
					element.addAttribute("excludeOrRequire", "require");
					queryClauses.add(element);
				}
			}

			// Add included Lucene query
			if (includedQuery != null && includedQuery.trim().length() > 0)
				queryClauses.add(createLuceneQueryElement(includedQuery.trim()));

			// ------------ Handle excluded records ---------------

			List booleanClausesExcludes = new ArrayList();

			// Add excluded terms and phrase clauses
			if (excludedTermsPhrases != null) {
				for (int i = 0; i < excludedTermsPhrases.length; i++) {
					if (excludedTermsPhrases[i] != null && excludedTermsPhrases[i].trim().length() > 0) {
						String type = "matchAnyTerm";
						if (excludedTermsPhrases[i].trim().indexOf(' ') > 0)
							type = "matchPhrase";
						Element element = createTextQueryElement("default", type, excludedTermsPhrases[i].trim());
						element.addAttribute("excludeOrRequire", "exclude");
						booleanClausesExcludes.add(element);
					}
				}
			}

			// Add excluded directories
			if (excludedDirs != null) {
				for (int i = 0; i < excludedDirs.length; i++) {
					if (excludedDirs[i] != null && excludedDirs[i].trim().length() > 0) {
						Element element = createTextQueryElement("docdir", "matchKeyword", excludedDirs[i].trim());
						element.addAttribute("excludeOrRequire", "exclude");
						booleanClausesExcludes.add(element);
					}
				}
			}

			// Add excluded Lucene query
			if (excludedQuery != null && excludedQuery.trim().length() > 0) {
				Element element = createLuceneQueryElement(excludedQuery.trim());
				element.addAttribute("excludeOrRequire", "exclude");
				booleanClausesExcludes.add(element);
			}

			// Check to see that there is at least one clause for includes or excludes:
			if (queryClauses.size() == 0 && booleanClausesExcludes.size() == 0)
				throw new Exception("No definition information was provided for set '" + setSpec + "'");

			// If there is no inclusion clauses, assume all records should be included in the set:
			if (queryClauses.size() == 0)
				queryClauses.add(createTextQueryElement("allrecords", "matchAnyTerm", "true"));

			// Add the exclusion clauses
			queryClauses.addAll(booleanClausesExcludes);

			// Add the clauses to exclude
			/* if (booleanClausesExcludes.size() > 0)
				queryClauses.add(createBooleanQueryElement("OR", booleanClausesExcludes)); */
			// Add all clauses to the XML query
			query.add(createBooleanQueryElement("OR", queryClauses));
			
			// Sort the sets by set name:
			List sortedList = document.selectNodes("/ListSets/set","setName");

			// Create a new document with sorted sets:
			document = DocumentHelper.createDocument();
			document.addElement("ListSets");
			root = document.getRootElement();
			for(int i = 0; i < sortedList.size(); i++) {
				Element e = (Element) sortedList.get(i);
				root.add(e.detach());				
			}
			
			// Write the XML to disc
			OutputFormat format = OutputFormat.createPrettyPrint();
			writer = new XMLWriter(new FileWriter(setsConfigFile), format);
			writer.write(document);

		} catch (Exception e) {
			prtlnErr("Unable to read OAI set configuration: " + e);
			//if (e instanceof NullPointerException)
				e.printStackTrace();
			throw e;
		} finally {
			if(writer != null)
				writer.close();	
		}

	}



	private static Element createTextQueryElement(String fieldName, String fieldType, String termOrPhrase) throws Exception {
		/* if (fieldName == null || fieldName.trim().length() == 0)
			throw new Exception("fieldName must not be empty or null");
		if (fieldType == null || fieldType.trim().length() == 0)
			throw new Exception("fieldType must not be empty or null");
		if (!fieldType.equals("matchAnyTerm") && !fieldType.equals("matchPhrase"))
			throw new Exception("fieldType must be either 'matchAnyTerm' or 'matchPhrase'"); */
		Document document = DocumentHelper.createDocument();
		Element fieldQuery = document.addElement("textQuery");
		if (fieldName != null)
			fieldQuery.addAttribute("field", fieldName);
		fieldQuery.addAttribute("type", fieldType);
		fieldQuery.addText(termOrPhrase);
		return fieldQuery;
	}



	private static Element createLuceneQueryElement(String query) throws Exception {
		if (query == null || query.trim().length() == 0)
			throw new Exception("query must not be empty or null");

		Document document = DocumentHelper.createDocument();
		Element luceneQuery = document.addElement("luceneQuery");
		luceneQuery.addText(query);
		return luceneQuery;
	}


	private static Element createBooleanQueryElement(String type, List clauseElements) throws Exception {
		if (type == null || (!type.equals("OR") && !type.equals("AND")))
			throw new Exception("boolean type must be either 'OR' or 'AND'");
		if (clauseElements == null || clauseElements.size() == 0)
			throw new Exception("boolean clause Elements List is empty or null");

		Document document = DocumentHelper.createDocument();
		Element booleanQuery = document.addElement("booleanQuery ");
		booleanQuery.addAttribute("type", type);
		for (int i = 0; i < clauseElements.size(); i++)
			booleanQuery.add((Element) clauseElements.get(i));
		return booleanQuery;
	}


	//================================================================


	/**
	 *  Return a string for the current time and date, sutiable for display in log files and output to standout:
	 *
	 * @return    The dateStamp value
	 */
	private final static String getDateStamp() {
		return
			new SimpleDateFormat("MMM d, yyyy h:mm:ss a zzz").format(new Date());
	}


	/**
	 *  Output a line of text to error out, with datestamp.
	 *
	 * @param  s  The text that will be output to error out.
	 */
	private final static void prtlnErr(String s) {
		System.err.println(getDateStamp() + " OAISetsXMLConfigManager ERROR: " + s);
	}



	/**
	 *  Output a line of text to standard out, with datestamp, if debug is set to true.
	 *
	 * @param  s  The String that will be output.
	 */
	private final static void prtln(String s) {
		if (debug) {
			System.out.println(getDateStamp() + " OAISetsXMLConfigManager: " + s);
		}
	}


	/**
	 *  Sets the debug attribute of the object
	 *
	 * @param  db  The new debug value
	 */
	public static void setDebug(boolean db) {
		debug = db;
	}

}

