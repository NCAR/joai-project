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
package org.dlese.dpc.xml;

import java.util.*;
import java.util.regex.*;
import java.lang.*;
import java.io.*;
import java.text.*;
import java.net.*;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.Attribute;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.io.SAXReader;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.dom4j.io.HTMLWriter;
import org.dom4j.tree.AbstractElement;

import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerConfigurationException;
import org.xml.sax.InputSource;
import java.beans.*;

import org.dlese.dpc.util.TimedURLConnection;
import org.dlese.dpc.util.URLConnectionTimedOutException;
import org.dlese.dpc.util.Files;

/**
 *  Utility methods for working with dom4j Documents.
 *
 * @author     John Weatherley<p>
 *
 *
 * @version    $Id: Dom4jUtils.java,v 1.17 2010/07/14 22:41:16 jweather Exp $
 */
public class Dom4jUtils {

	private static boolean debug = true;


	/**
	 *  Load an XML file into a dom4j Document. If error, return null. No validation is performed.
	 *
	 * @param  file                       An XML file
	 * @return                            An XML Document containing the dom4j DOM, or null if unable to process
	 *      the file.
	 * @exception  DocumentException      If dom4j error
	 * @exception  MalformedURLException  If error in file name
	 */
	public static Document getXmlDocument(File file)
		 throws DocumentException, MalformedURLException {
		// No validation...
		SAXReader reader = new SAXReader(false);
		Document document = reader.read(file);
		return document;
	}


	/**
	 *  Load an XML file into a dom4j Document using the given character encoding. If error, return null. No validation is performed.
	 *
	 * @param  file                       An XML file
	 * @param  encoding                   The character encoding to use, for example 'UTF-8'
	 * @return                            An XML Document containing the dom4j DOM, or null if unable to process
	 *      the file.
	 * @exception  DocumentException      If dom4j error
	 * @exception  MalformedURLException  If error in file name
	 * @exception  IOException            If IO error
	 */
	public static Document getXmlDocument(File file, String encoding)
		 throws DocumentException, MalformedURLException, IOException {
		return getXmlDocument(Files.readFileToEncoding(file, encoding).toString());
	}

	/**
	 *  Load XML into a dom4j Document. If error, return null. No validation is performed.
	 *
	 * @param  url                        A URL to an XML document
	 * @return                            An XML Document containing the dom4j DOM, or null if unable to process
	 *      the file.
	 * @exception  DocumentException      If dom4j error
	 * @exception  MalformedURLException  If error in the URL
	 */
	public static Document getXmlDocument(URL url)
		 throws DocumentException, MalformedURLException {
		// No validation...
		SAXReader reader = new SAXReader(false);
		Document document = reader.read(url);
		return document;
	}


	/**
	 *  Load XML into a dom4j Document, specifying a max time to wait for resopnse. Supports gzip transfer of the
	 *  response. If error, returns null. No validation is performed.
	 *
	 * @param  url                                 A URL to an XML document
	 * @param  timoutMs                            Number of milliseconds to wait before timming out, use 0 for
	 *      infinity
	 * @return                                     An XML Document containing the dom4j DOM, or null if unable to
	 *      process the file.
	 * @exception  DocumentException               If dom4j error
	 * @exception  MalformedURLException           If error in the URL
	 * @exception  IOException                     If IO error
	 * @exception  URLConnectionTimedOutException  If no response before reaching timoutMs
	 */
	public static Document getXmlDocument(URL url, int timoutMs)
		 throws DocumentException, MalformedURLException, IOException, URLConnectionTimedOutException {

		InputStream inputStream = TimedURLConnection.getInputStream(url, timoutMs);
		// No validation...
		SAXReader reader = new SAXReader(false);
		Document document = reader.read(inputStream);
		return document;
	}


	/**
	 *  Load XML into a dom4j Document, specifying a max time to wait for resopnse. Supports gzip transfer of the
	 *  response. If error, returns null. No validation is performed.
	 *
	 * @param  url                                 A URL to an XML document
	 * @param  timoutMs                            Number of milliseconds to wait before timming out, use 0 for
	 *      infinity
	 * @return                                     An XML Document containing the dom4j DOM, or null if unable to
	 *      process the file.
	 * @exception  DocumentException               If dom4j error
	 * @exception  MalformedURLException           If error in the URL
	 * @exception  IOException                     If IO error
	 * @exception  URLConnectionTimedOutException  If no response before reaching timoutMs
	 */
	public static Document getXmlDocument(String url, int timoutMs)
		 throws DocumentException, MalformedURLException, IOException, URLConnectionTimedOutException {

		InputStream inputStream = TimedURLConnection.getInputStream(url, timoutMs);
		// No validation...
		SAXReader reader = new SAXReader(false);
		Document document = reader.read(inputStream);
		return document;
	}


	/**
	 *  Post data to a URL and load the response XML into a dom4j Document, specifying a max time to wait for
	 *  resopnse. Supports gzip transfer of the response. If error, returns null. No validation is performed. <p>
	 *
	 *
	 *
	 * @param  url                                 A base URL to the service with no http params
	 * @param  timoutMs                            Number of milliseconds to wait before timming out, use 0 for
	 *      infinity
	 * @param  postData                            Data to post in the request of the form
	 *      parm1=value1&amp;param2=value2
	 * @return                                     An XML Document containing the dom4j DOM, or null if unable to
	 *      process the file.
	 * @exception  DocumentException               If dom4j error
	 * @exception  MalformedURLException           If error in the URL
	 * @exception  IOException                     If IO error
	 * @exception  URLConnectionTimedOutException  If no response before reaching timoutMs
	 */
	public static Document getXmlDocumentPostData(URL url, String postData, int timoutMs)
		 throws DocumentException, MalformedURLException, IOException, URLConnectionTimedOutException {
		InputStream inputStream = TimedURLConnection.getInputStream(url, postData, timoutMs);
		// No validation...
		SAXReader reader = new SAXReader(false);
		Document document = reader.read(inputStream);
		return document;
	}


	/**
	 *  Load XML into a dom4j Document. If error, return null. No validation is performed.
	 *
	 * @param  s                      A string representation of an XML document
	 * @return                        An XML Document containing the dom4j DOM, or null if unable to process the
	 *      string.
	 * @exception  DocumentException  If dom4j error
	 */
	public static Document getXmlDocument(String s)
		 throws DocumentException {
		// No validation...
		SAXReader reader = new SAXReader(false);
		StringReader sr = new StringReader(s);
		Document document = reader.read(sr);
		sr.close();
		return document;
	}


	/**
	 *  Load XML into a dom4j Document and localize the XML by removing all namespaces from it. If error, return
	 *  null. No validation is performed.
	 *
	 * @param  xml                    A string representation of an XML document
	 * @return                        An XML Document containing the localized dom4j DOM, or null if unable to
	 *      process the string.
	 * @exception  DocumentException  If dom4j error
	 */
	public static Document getXmlDocumentLocalized(String xml)
		 throws DocumentException {
		// No validation...
		SAXReader reader = new SAXReader(false);
		StringReader sr = new StringReader(localizeXml(xml));
		Document document = reader.read(sr);
		sr.close();
		return document;
	}


	/**
	 *  Takes a dom4j Node that contains an XML serialized JavaBean and returns a JavaBean Object.
	 *
	 * @param  javaObjectXmlNode  Dom4j Node
	 * @return                    JavaBean
	 * @exception  Exception      If error
	 */
	public static Object dom4j2JavaBean(Node javaObjectXmlNode) throws Exception {
		XMLDecoder d = new XMLDecoder(new BufferedInputStream(new ByteArrayInputStream(javaObjectXmlNode.asXML().getBytes("UTF-8"))));
		Object result = d.readObject();
		d.close();
		return result;
	}


	//================================================================

	/**
	 *  Gets the empty attribute of the Dom4jUtils class
	 *
	 * @param  e  NOT YET DOCUMENTED
	 * @return    The empty value
	 */
	public static boolean isEmpty(Element e) {
		return isEmpty(e, false);
	}


	/**
	 *  isEmpty returns true if neither the element, or any children recursively, have textual content or have a
	 *  non-empty attribute.
	 *
	 * @param  e                 an Element to test
	 * @param  ignoreWhiteSpace  determines whether whitespace is considered as textual content .
	 * @return                   true if Element and all children recursively are empty
	 */
	public static boolean isEmpty(Element e, boolean ignoreWhiteSpace) {
		String t = e.getText();
		// if ((t == null) || (t.trim().length() > 0)) {

		// 02/28/07 no longer ignore whitespace!
		if (ignoreWhiteSpace && t.trim().length() > 0) {
			return false;
		}
		if (!ignoreWhiteSpace && t.length() > 0) {
			return false;
		}

		// is there a non-empty attribute? (note: we don't ignore whitespace for attributes ...)
		for (Iterator i = e.attributeIterator(); i.hasNext(); ) {
			Attribute a = (Attribute) i.next();
			String value = a.getValue();
			if ((value == null) || (value.trim().length() > 0)) {
				return false;
			}
		}

		// is there a non-empty child element?
		for (Iterator i = e.elementIterator(); i.hasNext(); ) {
			Element child = (Element) i.next();
			boolean isChildEmpty = isEmpty(child);
			if (!isChildEmpty) {
				return false;
			}
		}

		return true;
	}


	/**
	 *  Strips all attributes (including namespace information) from a particular element within a string
	 *  representation of an XML element (having arbitrary content and subelements), then removes all namespace
	 *  prefixes throughout the xml using a trnasformer. Textually remove all namespace (and all other
	 *  attributes) from the first occurance of an element of <b> elementName</b> in the input string. This is
	 *  done using a regex pattern replace.<P>
	 *
	 *  NOTE: this method is DANGEROUS because it not only strips namespace information, but all attributes from
	 *  the element.
	 *
	 * @param  xml          Description of the Parameter
	 * @param  elementName  Description of the Parameter
	 * @return              Description of the Return Value
	 */
	public static String localizeXml(String xml, String elementName) {
		// Create a pattern to match the element start tag
		Pattern p = Pattern.compile("<" + elementName + ".+?>", Pattern.MULTILINE);

		// Create a matcher with an input string
		Matcher m = p.matcher(xml);
		return localizeXml(m.replaceFirst("<" + elementName + ">"));
	}


	/**
	 *  Gets the localizingTransformer attribute of the Dom4jUtils class
	 *
	 * @return                                        The localizingTransformer value
	 * @exception  TransformerConfigurationException  NOT YET DOCUMENTED
	 * @exception  FileNotFoundException              NOT YET DOCUMENTED
	 */
	public final static Transformer getLocalizingTransformer()
		 throws TransformerConfigurationException, FileNotFoundException {
		String xsl = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + XSLUtils.getRemoveNamespacesXSL();
		// String xsl = XSLUtils.getRemoveNamespacesXSL();
		TransformerFactory tFactory = TransformerFactory.newInstance();
		return tFactory.newTransformer(new StreamSource(new StringBufferInputStream(xsl)));
	}


	/**
	 *  Remove nameSpaceInformation from the root element (as well as any attributes!) to facilitate xpath access
	 *
	 * @param  doc              Description of the Parameter
	 * @param  rootElementName  Description of the Parameter
	 * @return                  Description of the Return Value
	 */
	public static Document localizeXml(Document doc, String rootElementName) {
		String localizedXml = localizeXml(doc.asXML(), rootElementName);
		try {
			return getXmlDocument(localizedXml);
		} catch (Exception e) {
			prtln("unable to localizeItemRecord(): " + e);
		}
		return null;
	}


	/**
	 *  Localizes an XML String by removing all namespaces from it. I think this is a SAFE way to localize, since
	 *  it doesn't strip attributes from the root (except those that define namespaces). Obviously, this is good
	 *  because it preserves non-namespace-defining attributes in the root node (except the schema-instance
	 *  namespace (e.g., xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"). But because that particular
	 *  xmlns:xsi attribute is hanging around, we have to test for its presense when we "delocalize" (see
	 *  MetaDataFramework.getWritableRecord()).
	 *
	 * @param  xml  XML as a String
	 * @return      XML without namespaces
	 */
	public static String localizeXml(String xml) {
		javax.xml.transform.Transformer transformer = null;
		String localizedXml = "";
		try {
			transformer = getLocalizingTransformer();
			localizedXml = XSLTransformer.transformString(xml, transformer);
		} catch (Exception e) {
			prtlnErr(e.getMessage());
		}
		return localizedXml;
	}


	/**
	 *  Localizes a Dom4j Document by removing all namespaces from it. With namespaces removed, the XPath syntax
	 *  necessary to work with the document is greatly simplified.
	 *
	 * @param  doc  The Document to localize
	 * @return      A localized Document, or null if fail
	 */
	public static Document localizeXml(Document doc) {
		try {
			return getXmlDocument(localizeXml(doc.asXML()));
		} catch (Exception e) {
			prtlnErr("unable to localize Document: " + e);
		}
		return null;
	}


	/**
	 *  Localizes a Dom4j Element by removing all namespaces from it. With namespaces removed, the XPath syntax
	 *  necessary to work with the document is greatly simplified.
	 *
	 * @param  element  The Element to localize
	 * @return          A localized Element, or null if fail
	 */
	public static Element localizeXml(Element element) {

		String localizedXml = localizeXml(element.asXML());
		try {
			Document doc = getXmlDocument(localizedXml);
			if (doc == null)
				throw new Exception("could not process localized xml");
			return doc.getRootElement().createCopy();
		} catch (Exception e) {
			prtln("unable to element: " + e);
		}
		return null;
	}


	/**
	 *  Localizes a Dom4j Node, Document, Branch or Element by removing all namespaces from it, returning a
	 *  Document. With namespaces removed, the XPath syntax necessary to work with the document is greatly
	 *  simplified.
	 *
	 * @param  node  The Node to localize
	 * @return       A localized Document, or null if fail
	 */
	public static Document localizeXml(Node node) {
		// Note: Needs to be tested/refined... it would be better to return the same object type as was passed in...
		String localizedXml = localizeXml(node.asXML());
		try {
			Document doc = getXmlDocument(localizedXml);
			if (doc == null)
				throw new Exception("could not process localized xml");
			return doc;
		} catch (Exception e) {
			prtln("unable to localizeXML node: " + e);
		}
		return null;
	}


	/**
	 *  Insert nameSpaceInformation into the root element to make validation possible.<p>
	 *
	 *  NOTE: only delocalizes if the rootElement is empty.
	 *
	 * @param  s                Description of the Parameter
	 * @param  rootElementName  Description of the Parameter
	 * @param  nameSpaceInfo    Description of the Parameter
	 * @return                  Description of the Return Value
	 */
	public static String delocalizeDocStr(String s, String rootElementName, String nameSpaceInfo) {
		// Create a pattern to match the rootElement
		// Pattern p = Pattern.compile("<" + rootElementName + "\\s*>", Pattern.MULTILINE);

		// accomodate attributes in root element
		Pattern p = Pattern.compile("<" + rootElementName + "([^>]*)>", Pattern.MULTILINE);

		// Create a matcher with an input string
		Matcher m = p.matcher(s);
		if (!m.find()) {
			prtlnErr("ERROR: delocalizeDocStr: rootElementName (" + rootElementName + ") not found");
			return s;
		}
		return m.replaceFirst("<" + rootElementName + m.group(1) + " " + nameSpaceInfo + ">");
	}


	/**
	 *  Insert nameSpaceInformation into the root element
	 *
	 * @param  doc              Description of the Parameter
	 * @param  rootElementName  Description of the Parameter
	 * @param  nameSpaceInfo    Description of the Parameter
	 * @return                  Description of the Return Value
	 */
	public static Document delocalizeXml(Document doc, String rootElementName, String nameSpaceInfo) {
		Element root = doc.getRootElement();
		String rootName = root.getName();
		String namespaceXml = delocalizeDocStr(doc.asXML(), rootElementName, nameSpaceInfo);
		try {
			return getXmlDocument(namespaceXml);
		} catch (Exception e) {
			prtln("unable to delocalizeItemRecordDoc(): " + e);
		}
		return null;
	}


	/**
	 *  Gets the nameSpaceInfo associated with the root element of a Document. Returns all contents of the root
	 *  Element except the element name.
	 *
	 * @param  doc              Description of the Parameter
	 * @param  rootElementName  Description of the Parameter
	 * @return                  The nameSpaceInfo value
	 */
	public static String getNameSpaceInfo(Document doc, String rootElementName) {
		String xml = doc.asXML();
		String elementName = rootElementName;
		Pattern p = Pattern.compile("<" + rootElementName + "(.+?)>", Pattern.MULTILINE);
		Matcher m = p.matcher(xml);

		if (!m.find()) {
			prtln("root Element not found!?: " + p.toString());
			return "";
		}
		else {
			return m.group(1);
		}
	}


	/**
	 *  Writes {@link org.dom4j.Document} to File
	 *
	 * @param  doc            Description of the Parameter
	 * @param  out            Description of the Parameter
	 * @exception  Exception  Description of the Exception
	 */
	public static void writeDocToFile(Document doc, File out)
		 throws Exception {
		writeDocToFile(doc, out, null);
	}


	/**
	 *  write formated xml to file to faciliate human-readibility
	 *
	 * @param  doc            Description of the Parameter
	 * @param  out            Description of the Parameter
	 * @exception  Exception  Description of the Exception
	 */
	public static void writePrettyDocToFile(Document doc, File out)
		 throws Exception {
		OutputFormat format = OutputFormat.createPrettyPrint();
		writeDocToFile(doc, out, format);
	}


	/**
	 *  Write XML Document to disk using specified OutputFormat
	 *
	 * @param  doc            Description of the Parameter
	 * @param  out            Description of the Parameter
	 * @param  format         Description of the Parameter
	 * @exception  Exception  Description of the Exception
	 */
	public static void writeDocToFile(Document doc, File out, OutputFormat format)
		 throws Exception {
		String encoding = "UTF-8";
		if (format == null) {
			format = new OutputFormat();
		}
		OutputStreamWriter fileWriter = new OutputStreamWriter(new FileOutputStream(out), encoding);
		XMLWriter writer = new XMLWriter(fileWriter, format);
		writer.write(doc);
		fileWriter.close();
	}


	/**
	 *  Gets an XMLWriter object
	 *
	 * @return    The xMLWriter value
	 */
	public static XMLWriter getXMLWriter() {
		OutputFormat format = new OutputFormat("  ", false);
		XMLWriter writer = null;
		try {
			writer = new XMLWriter(System.out, format);
		} catch (Exception e) {
			prtln("getXMLWriter(): " + e);
		}
		return writer;
	}


	/**
	 *  Formats an {@link org.dom4j.Node} as a printable string
	 *
	 * @param  node  the Node to display
	 * @return       a nicley-formatted String representation of the Node.
	 */
	public static String prettyPrint(Node node) {
		StringWriter sw = new StringWriter();
		OutputFormat format = OutputFormat.createPrettyPrint();

		format.setXHTML(true);
		//Default is false, this produces XHTML
		HTMLWriter ppWriter = new HTMLWriter(sw, format);
		try {
			ppWriter.write(node);
			ppWriter.flush();
		} catch (Exception e) {
			return ("Pretty Print Failed");
		}
		return sw.toString();
	}


	//================================================================

	/**
	 *  Logger hook
	 *
	 * @param  msg  Message to be logged.
	 */
	public void log(String msg) {
		prtln(msg);
	}



	/**
	 *  Return a string for the current time and date, sutiable for display in log files and output to standout:
	 *
	 * @return    The dateStamp value
	 */
	protected final static String getDateStamp() {
		return
			new SimpleDateFormat("MMM d, yyyy h:mm:ss a zzz").format(new Date());
	}


	/**
	 *  Output a line of text to error out, with datestamp.
	 *
	 * @param  s  The text that will be output to error out.
	 */
	protected final static void prtlnErr(String s) {
		System.err.println(getDateStamp() + " " + s);
	}



	/**
	 *  Output a line of text to standard out, with datestamp, if debug is set to true.
	 *
	 * @param  s  The String that will be output.
	 */
	protected final static void prtln(String s) {
		if (debug) {
			// System.out.println(getDateStamp() + " " + s);
			System.out.println(s);
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

