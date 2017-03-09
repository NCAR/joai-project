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
package org.dlese.dpc.schemedit.vocab;

import java.io.*;
import java.util.*;
import java.net.*;

import org.dom4j.*;

import org.dlese.dpc.xml.*;
import org.dlese.dpc.util.Utils;
import org.dlese.dpc.schemedit.SchemEditUtils;
import org.dlese.dpc.serviceclients.webclient.*;

import org.apache.struts.util.LabelValueBean;

/**
 *  Provides access to infomormation (beyond that expressed in the metadata
 *  schema) about a metadata field, such as cataloging best practices and
 *  definitions of controlled vocabulary.<p>
 *
 *  FieldInfoReader reads FieldInfo xml documents and is used by {@link
 *  org.dlese.dpc.schemedit.autoform} classes as well as {@link
 *  org.dlese.dpc.schemedit.MetaDataFramework} instances.<p>
 *
 *  Note: why doesn't this class extend the {@link
 *  org.dlese.dpc.schemedit.config.AbstractConfigReader} class?
 *
 *@author    ostwald
 */
public class FieldInfoReader {
	private static boolean debug = false;
	/**
	 *  Description of the Field
	 */
	public URI uri = null;
	private String rootElementName = "metadataFieldInfo";
	private Document doc = null;
	private HashMap termMap = null;
	private List dos = null;
	private List donts = null;
	private List otherPractices = null;
	private List examples = null;
	private List vocabTerms = null;
	private List termList = null;
	private List prompts = null;


	/**
	 *  Constructor for the FieldInfoReader object
	 *
	 *@param  path           Description of the Parameter
	 *@exception  Exception  Description of the Exception
	 */
	public FieldInfoReader(String path)
			 throws Exception {
		this(new URI(path));
	}


	/**
	 *  Constructor for the FieldInfoReader object
	 *
	 *@param  uri            Description of the Parameter
	 *@exception  Exception  Description of the Exception
	 */
	public FieldInfoReader(URI uri)
			 throws Exception {
		this.uri = uri;
		try {
			doc = SchemEditUtils.getLocalizedXmlDocument(uri);
			// prtln (Dom4jUtils.prettyPrint(doc));
		} catch (Exception e) {
			throw new Exception("FieldInfoReader error: " + e.getMessage());
		}
	}

	/**
	* Returns URI (as String) of the source file for this FieldInfoReader
	*/
	public String getSource () {
		return this.uri.toString();
	}

	/**
	 *  Gets the document attribute of the FieldInfoReader object
	 *
	 *@return    The document value
	 */
	public Document getDocument() {
		return doc;
	}


	/**
	 *  Sets the debug attribute of the FieldInfoReader class
	 *
	 *@param  d  The new debug value
	 */
	public static void setDebug(boolean d) {
		debug = d;
	}


	/**
	 *  Gets a List of {@link TermAndDeftn} instances for specified term (usually there
	 is only one item in the list)
	 *
	 *@param  term  Description of the Parameter
	 *@return       The termAndDeftn value
	 */
	public List getTermAndDeftns(String term) {
		return (List) termMap.get(term);
	}


	/**
	 *  Gets the {@link org.dlese.dpc.schemedit.vocab.TermAndDeftn} instances
	 *  defined for this field.
	 *
	 *@return    The vocabTerms value
	 */
	public Collection getVocabTerms() {
		if (vocabTerms == null) {
			vocabTerms = new ArrayList();
			Map map = getTermMap();
			for (Iterator i = map.keySet().iterator(); i.hasNext(); ) {
				String key = (String) i.next();
				vocabTerms.addAll((List) map.get(key));
			}
			Collections.sort(vocabTerms, new SortTermAndDeftns());
		}
		return vocabTerms;
	}


	/**
	 *  Gets a sorted list of vocab Terms defined by this field.
	 *
	 *@return    The termList value
	 */
	public List getTermList() {
		if (termList == null) {
			termList = new ArrayList();
			Set termSet = getTermMap().keySet();
			for (Iterator i = termSet.iterator(); i.hasNext(); ) {
				termList.add((String) i.next());
			}
			Collections.sort(termList);
		}
		return termList;
	}


	/**
	 *  Build an array of terms (represented as LabelValueBean instances) to be
	 *  used in a html select object.
	 *
	 *@return    The termSelectOptions value as an array of
	 */
	public LabelValueBean[] getTermSelectOptions() {
		List terms = getTermList();
		LabelValueBean[] options = new LabelValueBean[terms.size() + 1];
		options[0] = new LabelValueBean(org.dlese.dpc.schemedit.action.form.SchemEditForm.UNSPECIFIED, "");
		int index = 1;
		for (Iterator i = terms.iterator(); i.hasNext(); ) {
			String term = (String) i.next();
			options[index++] = new LabelValueBean(term, term);
		}
		return options;
	}


	/**
	 *  Gets the mapping from vocab term to corresponding {@link
	 *  org.dlese.dpc.schemedit.vocab.TermAndDeft} instance.
	 *
	 *@return    The termMap for this field.
	 */
	private HashMap getTermMap() {
		if (termMap == null) {
			termMap = new HashMap();
			Node terms = doc.selectSingleNode("/metadataFieldInfo/field/terms");
			if (terms != null) {
				Element termsElement = (Element) terms;
				for (Iterator i = termsElement.elementIterator("termAndDeftn"); i.hasNext(); ) {
					try {
						Element termAndDeftnElement = (Element) i.next();
						TermAndDeftn termAndDeftn = new TermAndDeftn(termAndDeftnElement);
						String term = termAndDeftn.getTerm();
						List values = null;
						if (!termMap.containsKey(term)) {
							values = new ArrayList();
						} else {
							values = (List) termMap.get(term);
						}
						values.add(termAndDeftn);
						termMap.put(term, values);
					} catch (Exception e) {
						prtln("TermAndDeftn error: " + e.getMessage());
					}
				}
			}
		}
		return termMap;
	}


	/**
	 *  Get the xpath to the field this file documents.
	 *
	 *@return                The path value
	 *@exception  Exception  Description of the Exception
	 */
	public String getPath()
			 throws Exception {
		/*
		 *  return "/news-oppsRecord" + doc.selectSingleNode ("/metadataFieldInfo/field/@path").getText();
		 */
		return doc.selectSingleNode("/metadataFieldInfo/field/@path").getText();
	}


	/**
	 *  Get the name of the field this file documents.
	 *
	 *@return                The name value
	 *@exception  Exception  Description of the Exception
	 */
	public String getName()
			 throws Exception {
		return doc.selectSingleNode("/metadataFieldInfo/field/@name").getText();
	}


	/**
	 *  Get the metadata format of the framework containing this field.
	 *
	 *@return                The format value
	 *@exception  Exception  Description of the Exception
	 */
	public String getFormat()
			 throws Exception {
		return doc.selectSingleNode("/metadataFieldInfo/field/@metaFormat").getText();
	}


	/**
	 *  Gets the definition for this field.
	 *
	 *@return                The definition value
	 */
	public String getDefinition() {
		try {
			return doc.selectSingleNode("/metadataFieldInfo/field/definition").getText();
		} catch (Throwable t) {}
		return "";
	}


	/**
	 *  Gets the prompts for this field.
	 *
	 *@return                The definition value
	 */
	public List getPrompts() {
		if (prompts == null) {
			prompts = new ArrayList();
			List nodes = doc.selectNodes("/metadataFieldInfo/field/prompts/prompt");
			if (nodes != null) {
				for (Iterator i = nodes.iterator(); i.hasNext(); ) {
					Element promptEl = (Element) i.next();
					prompts.add(promptEl.getTextTrim());
				}
			}
		}
		return prompts;
	}


	/**
	 *  Gets the dos attribute of the FieldInfoReader object
	 *
	 *@return    The dos value
	 */
	public List getDos() {
		if (dos == null) {
			dos = getFormattedPractices("/metadataFieldInfo/field/bestPractices/dos");
		}
		return dos;
	}


	/**
	 *  Gets the donts attribute of the FieldInfoReader object
	 *
	 *@return    The donts value
	 */
	public List getDonts() {
		if (donts == null) {
			donts = getFormattedPractices("/metadataFieldInfo/field/bestPractices/donts");
		}
		return donts;
	}


	/**
	 *  Gets the examples attribute of the FieldInfoReader object
	 *
	 *@return    The examples value
	 */
	public List getExamples() {
		if (examples == null) {
			examples = getFormattedPractices("/metadataFieldInfo/field/bestPractices/examples");
		}
		return examples;
	}


	/**
	 *  Gets the formattedPractices attribute of the FieldInfoReader object
	 *
	 *@param  basePath  Description of the Parameter
	 *@return           The formattedPractices value
	 */
	private List getFormattedPractices(String basePath) {
		String status = "current";
		return getFormattedPractices(basePath, status);
	}


	/**
	 *  Gets the formattedPractices attribute of the FieldInfoReader object
	 *
	 *@param  basePath  Description of the Parameter
	 *@param  status    Description of the Parameter
	 *@return           The formattedPractices value
	 */
	private List getFormattedPractices(String basePath, String status) {
		String xpath = basePath + "/practice";
		if (status != null && status.trim().length() > 0) {
			xpath += "[@status=\'" + status + "\']";
		}

		// prtln ("\n" + xpath + "\n");
		List practiceNodes = doc.selectNodes(xpath);

		List practices = new ArrayList();
		for (Iterator i = practiceNodes.iterator(); i.hasNext(); ) {
			Node pNode = (Node) i.next();
			String pNodeText = getElementText((Element) pNode);
			practices.add(pNodeText);
		}
		return practices;
	}


	/**
	 *  Get the list of otherPractices specified by this fields file.<p>
	 *
	 *  Returns a list of OtherPractice instances, each of which has a header (e.g,
	 *  "No title? Do this") and a set of "pratices".
	 *
	 *@return    OtherPractices for this field.
	 */
	public List getOtherPractices() {
		if (otherPractices != null) {
			return otherPractices;
		}

		String otherPracticePath = "/metadataFieldInfo/field/bestPractices/otherPractices/otherPractice";
		List otherPracticeNodes = doc.selectNodes(otherPracticePath);
		otherPractices = new ArrayList();
		int index = 0;

		for (Iterator i = otherPracticeNodes.iterator(); i.hasNext(); ) {
			Element otherPracticeElement = (Element) i.next();
			String header = otherPracticeElement.attributeValue("header");
			prtln("getting practices for \"" + header + "\"");
			List practices = getFormattedPractices(otherPracticePath + "[" + ++index + "]");
			if (practices.size() > 0) {
				prtln(" . . . " + practices.size() + " found");
				otherPractices.add(new OtherPractice(header, practices));
			} else {
				prtln(" . . . none found");
			}
		}

		return otherPractices;
	}


	/**
	 *  Apply specified markup (url, style) to the first occurrence of a substring
	 *  (<b>link</b> ) of the provided text.
	 *
	 *@param  text   A string of which a substring will receive formatting
	 *@param  link   the substring to recieve formatting
	 *@param  url    if present, provides destination for link (link becomes a
	 *      hyperlink to url)
	 *@param  style  specifies the formatting to be applied to link (bold, italic,
	 *      bold and italic)
	 *@return        text containing a marked-up substring
	 */
	public static String applyMarkup(String text, String link, String url, String style) {

		if (false) {
			prtln("applyMarkup:");
			prtln("\t text: " + text);
			prtln("\t link: " + link);
			prtln("\t url: " + url);
			prtln("\t style: " + style);
		}

		Element linkElement = null;

		// make sure the base text actually contains link!
		if (link != null && text != null &&
				text.indexOf(link) != -1) {

			DocumentFactory df = DocumentFactory.getInstance();

			// style expresses formatting (bold, italic, or bold and italic)
			// styleElement holds formatting markup as well as marked up text (e.g., "link")
			Element styleElement = null;
			if (style != null) {
				if (style.equalsIgnoreCase("bold")) {
					styleElement = df.createElement("b");
					styleElement.setText(link);
				} else if (style.equalsIgnoreCase("italic")) {
					styleElement = df.createElement("i");
					styleElement.setText(link);
				} else if (style.equalsIgnoreCase("bold and italic")) {
					styleElement = df.createElement("i");
					Element subEmp = styleElement.addElement("b");
					subEmp.setText(link);
				}
				linkElement = styleElement;
			}

			// url expresses target for hyperlink
			if (url != null) {
				Element hyperlink = df.createElement("a")
						.addAttribute("target", "_blank")
						.addAttribute("href", url);
				if (styleElement != null) {
					hyperlink.add(styleElement);
				} else {
					hyperlink.setText(link);
				}
				linkElement = hyperlink;
			}

			// if neither style or url is specified, then there is no linkElement (i.e., nothing to change in the text)
			if (linkElement != null) {
				text = replaceFirst(text, link, linkElement.asXML());
			}
		}

		return text;
	}



	/**
	 *  Return formatted contents for given Element, inserting hyperlink when
	 *  "link" and "url" attributes are present
	 *
	 *@param  e  Description of the Parameter
	 *@return    The elementText value
	 */
	public static String getElementText(Element e) {
		String text = e.getText();
		for (int i = 1; i < 10; i++) {
			String n = Integer.toString(i);
			String link = e.attributeValue("link" + n);
			String url = e.attributeValue("url" + n);
			String style = e.attributeValue("style" + n);

			if (link == null) {
				break;
			}
			text = applyMarkup(text, link, url, style);
		}
		return text;
	}


	/**
	 *  Replace first occurrance of exact match (case sensitive)<p>
	 *
	 *  needed to replace strings with regex pattern chars, which don't get
	 *  replaced by String.replace
	 *
	 *@param  in       Description of the Parameter
	 *@param  find     Description of the Parameter
	 *@param  replace  Description of the Parameter
	 *@return          Description of the Return Value
	 */

	public static String replaceFirst(String in,
			String find,
			String replace) {

		StringBuffer ret = new StringBuffer();

		int start = in.indexOf(find);
		if (start != -1) {
			ret.append(in.substring(0, start));
			ret.append(replace);
			ret.append(in.substring(start + find.length()));
		}

		return ret.toString();
	}


	/**
	 *  Debugging method.
	 *
	 *@return    Description of the Return Value
	 */
	public String toString() {
		ArrayList buf = new ArrayList();
		buf.add("FieldInfoReader:");
		try {
			buf.add("path: " + getPath());
		} catch (Exception e) {
			return ("getPath failed: " + e.getMessage());
		}
		try {
			buf.add("name: " + getName());
		} catch (Exception e) {
			return ("getName failed: " + e.getMessage());
		}
		try {
			buf.add("format: " + getFormat());
		} catch (Exception e) {
			return ("getFormat failed: " + e.getMessage());
		}
		try {
			buf.add("definition: " + getDefinition());
		} catch (Exception e) {
			return ("getDefinition failed: " + e.getMessage());
		}

		for (Iterator i = getTermMap().values().iterator(); i.hasNext(); ) {
			List definitions = (List) i.next();

			for (Iterator j = definitions.iterator(); j.hasNext(); ) {
				TermAndDeftn t = (TermAndDeftn) j.next();
				buf.add(t.toString());
			}
		}

		String ret = "";
		for (Iterator i = buf.iterator(); i.hasNext(); ) {
			ret += (String) i.next();
			if (i.hasNext()) {
				ret += "\n\t";
			}
		}
		return ret;
	}


	/**
	 *  Output a line of text to standard out, with datestamp, if debug is set to
	 *  true.
	 *
	 *@param  s  The String that will be output.
	 */
	private static void prtln(String s) {
		if (debug) {
			System.out.println(s);
		}
	}


	/**
	 *  Description of the Method
	 */
	public void destroy() {
		termMap = null;
		doc = null;
	}


	/**
	 *  Other practices have a header and a list of "practices".
	 *
	 *@author    ostwald
	 */
	public class OtherPractice {
		/**
		 *  Description of the Field
		 */
		public String header = "";
		private List practices;


		/**
		 *  Constructor for the OtherPractice object
		 *
		 *@param  header     Header for this set of otherPractices
		 *@param  practices  List of formattedPractices
		 */
		public OtherPractice(String header, List practices) {
			this.header = header;
			this.practices = practices;
		}


		/**
		 *  Gets the header attribute of the OtherPractice object
		 *
		 *@return    The header value
		 */
		public String getHeader() {
			return header;
		}


		/**
		 *  Gets the practices attribute of the OtherPractice object
		 *
		 *@return    The practices value
		 */
		public List getPractices() {
			return practices;
		}


		/**
		 *  Description of the Method
		 *
		 *@return    Description of the Return Value
		 */
		public String toString() {
			String ret = header + ":";
			for (Iterator i = practices.iterator(); i.hasNext(); ) {
				ret += "\n\t" + (String) i.next();
			}
			return ret;
		}
	}

}

