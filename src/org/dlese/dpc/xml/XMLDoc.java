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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringReader;

import java.util.LinkedList;
import java.util.StringTokenizer;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 *  Reads and parses an XML document.
 *
 * @author    Steve Sullivan
 * @see       XMLException
 */
public class XMLDoc {

	private String systemid = "none specified";
	private Document doc;
	private int bugs = 0;
	private StringBuffer errors = new StringBuffer();
	private StringBuffer warnings = new StringBuffer();
	private boolean hasErrors = false, hasWarnings = false;


	/**
	 * Usage info for test driver.
	 */

	static void badparms( String msg) {
		prtln("Parms: validate namespaces expandEntities minnum maxnum infile xmlspec iters");
		prtln("Examples:");
		prtln("    java XMLDoc n n n 0 0 file:///usr/local/nonesuch.xml  a/b/c  1");
		prtln("    java XMLDoc n n n 0 0 http:///www.nonesuch.com/nonsuch.xml  a/b@myattr  1");
		System.exit(1);
	}


	/**
	 * Test driver.
	 */

	public static void main( String[] args) {
		int ii, jj;

		try {
			if (args.length != 8) badparms("wrong num parms");
			int iarg = 0;
			boolean isValidating = getBoolean( args[iarg++]);
			boolean isNamespace  = getBoolean( args[iarg++]);
			boolean isExpand     = getBoolean( args[iarg++]);
			int minnum           = getInt( args[iarg++]);
			int maxnum           = getInt( args[iarg++]);
			String xmlSource     = args[iarg++];
			String xpathspec     = args[iarg++];
			int iters            = getInt( args[iarg++]);
	
			for (ii = 0; ii < iters; ii++) {
				XMLDoc doc = new XMLDoc( xmlSource,
					isValidating, isNamespace, isExpand);

				String[] stgs = doc.getXmlFields( minnum, maxnum, xpathspec);
				prtln("iter " + ii + ":");
				for (jj = 0; jj < stgs.length; jj++) {
					prtln("    " + jj + ": \"" + stgs[jj] + "\"");
				}
			}
		}
		catch( XMLException xex) {
			prtln("main: caught: " + xex);
			xex.printStackTrace();
		}
	}


	/**
	 * Parse boolean parm for test driver.
	 */

	static boolean getBoolean( String stg)
	throws XMLException
	{
		boolean bres = false;
		if (stg.toLowerCase().equals("y")) bres = true;
		else if (stg.toLowerCase().equals("n")) bres = false;
		else mkerror("invalid boolean parm: \"" + stg + "\"");
		return bres;
	}



	/**
	 * Parses int parm for test driver.
	 */

	static int getInt( String stg)
	throws XMLException
	{
		int ires = 0;
		try { ires = Integer.parseInt( stg, 10); }
		catch( NumberFormatException nfe) {
			mkerror("invalid int parm: \"" + stg + "\"");
		}
		return ires;
	}



	/**
	 *  Reads and parses the XML document.
	 *
	 * @param  systemid         Specifies the input file or url: e.g,
	 *      <ul>
	 *        <li> <code>file:///usr/local/nonesuch.xml</code>
	 *        <li> <code>http://www.nonesuch.com/nonsuch.xml</code>
	 *      </ul>
	 *      If it represents a file, it must have 3 slashes
	 *      and the path must be fully qualified.
	 * @param  validating        Is the parser to be validating?
	 * @param  namespaceAware    Is the parser to be namespace aware?
	 * @param  expandEntities    DESCRIPTION
	 * @exception  XMLException  DESCRIPTION
	 */
	public XMLDoc(
		String systemid,			    // URI to the XML document.
		boolean validating,			// does parser validate
		boolean namespaceAware,		// is parser namespace aware
		boolean expandEntities)		// expand Entity refs
	throws XMLException
	{
		this.systemid = systemid;

		// Java's org.xml.sax.InputSource will change
		// "file://foo%3aBar"  to  "file://foo:Bar"
		// But here we really want the escaped file name.
		InputSource isrc;
		FileInputStream infile = null;
		if (systemid.startsWith("file:")) {
			String fname = systemid.substring(5);
			if (fname.startsWith("///")) fname = fname.substring(2);
			else if (fname.startsWith("//")) fname = fname.substring(1);
			try { 
				infile = new FileInputStream( fname); 
				isrc = new InputSource( infile);
				doParseDoc( isrc, validating, namespaceAware, expandEntities);
			}
			catch( Exception e) {
				throw new XMLException( e.getMessage() );
			}
			finally{
				try{ 
					if(infile != null)
						infile.close(); 
				}
				catch( IOException ioe) {
					throw new XMLException( ioe.getMessage());	
				}
			}	
		}
		else{ 
			isrc = new InputSource( systemid);
			doParseDoc( isrc, validating, namespaceAware, expandEntities);
		}
	}

	/**
	 *  A non-initialized XMLDoc. To read the actual XML, use 
	 *  method {@link #useXmlString(String, boolean,boolean,boolean)}.
	 */		
	public XMLDoc(){}
	
	
	/**
	 *  Reads and parses the XML string, which is then the source of the XML 
	 *  used in this XMLDoc.
	 */		
	public void useXmlString(
		String xmlString,			// An XML string
		boolean validating,			// does parser validate
		boolean namespaceAware,		// is parser namespace aware
		boolean expandEntities)		// expand Entity refs
	throws XMLException
	{
		this.systemid = "none specified";
		InputSource isrc = new InputSource( new StringReader(xmlString));
		doParseDoc( isrc, validating, namespaceAware, expandEntities);	
	}
	
	/**
	 *  Reads and parses the XML from the given source.
	 */		
	private final void doParseDoc(
		InputSource xmlSource,
		boolean validating,			// does parser validate
		boolean namespaceAware,		// is parser namespace aware
		boolean expandEntities)		// expand Entity refs
	throws XMLException
	{
		this.doc = null;

		if (bugs >= 1)
			prtln("getXmlDoc: systemid: \"" + systemid + "\"");
		DocumentBuilderFactory factory = null;
		try {
			factory = DocumentBuilderFactory.newInstance();
		} catch (FactoryConfigurationError fce) {
			mkerror("factory config error: " + fce);
		}

		factory.setValidating(validating);
		factory.setNamespaceAware(namespaceAware);
		factory.setExpandEntityReferences(expandEntities); // expand Entity rfs

		factory.setCoalescing(true);	// convert CDATA nodes to Text nodes
		factory.setIgnoringElementContentWhitespace(true);

		DocumentBuilder bldr = null;
		try {
			bldr = factory.newDocumentBuilder();
		} catch (ParserConfigurationException pce) {
			mkerror("parser config exc: " + pce);
		}

		SimpleErrorHandler simpleErrorHandler
			= new SimpleErrorHandler(errors, warnings);
		bldr.setErrorHandler(simpleErrorHandler);

		try {
			doc = bldr.parse(xmlSource);
		} catch (IOException ioe) {
			mkerror("Could not read xml doc file \"" + systemid
				 + "\"  exc: " + ioe.getMessage());
		} catch (SAXException sxe) {
			mkerror("Could not parse xml doc file \"" + systemid
				 + "\"  exc: " + sxe.getMessage());
		}
		hasErrors = simpleErrorHandler.hasErrors();
		hasWarnings = simpleErrorHandler.hasWarnings();
		///try{
		///	xmlSource.getByteStream().close();
		///}catch(Throwable e){}
		///xmlSource = null;
	}	
	
	
	
	

	/**
	 *  Retrieves a single int from an XML document.
	 *  The specified xpath must occur exactly
	 *  once in the document.
	 *  See {@link #getXmlFields getXmlFields} for additional doc on
	 *  xpaths and on how occurances are counted.
	 *
	 * @param  xpath             The partial xpath specification.
	 * @return                   The int value
	 * @exception  XMLException  DESCRIPTION
	 */
	public int getXmlInt(String xpath)
		 throws XMLException {
		String stg = getXmlString(xpath);
		int ires = 0;
		try {
			ires = Integer.parseInt(stg, 10);
		} catch (NumberFormatException nfe) {
			mkerror("Invalid integer: \"" + stg
				+ "\"  in xpath \"" + xpath + "\"  in file \""
				+ systemid + "\"");
		}
		return ires;
	}



	/**
	 *  Retrieves a single boolean from an XML document.
	 *  The specified xpath must occur exactly
	 *  once in the document.
	 *  The boolean value is case insensitive and may be encoded
	 *  as one of: "true", "false", "yes", "no".
	 *  See {@link #getXmlFields getXmlFields} for additional doc on
	 *  xpaths and on how occurances are counted.
	 *
	 * @param  xpath             The partial xpath specification.
	 * @return                   The boolean value
	 * @exception  XMLException  DESCRIPTION
	 */
	public boolean getXmlBoolean(String xpath)
	throws XMLException
	{
		String stg = getXmlString(xpath);
		boolean bres = false;
		if (stg.equalsIgnoreCase("true") || stg.equalsIgnoreCase("yes"))
			bres = true;
		else if (stg.equalsIgnoreCase("false") || stg.equalsIgnoreCase("no"))
			bres = false;
		else mkerror("Invalid boolean: \"" + stg
			+  "\"  in xpath \"" + xpath + "\"  in file \""
			+ systemid + "\"");
		return bres;
	}



	/**
	 *  Retrieves a single String from an XML document.
	 *  The specified xpath must occur
	 *  exactly once in the document.
	 *  See {@link #getXmlFields getXmlFields} for additional
	 *  doc on xpaths and on how occurances are counted.
	 *
	 * @param  xpath             The partial xpath specification.
	 * @return                   The xmlString value
	 * @exception  XMLException  If the requested xpath was not found.
	 */
	public String getXmlString(String xpath)
		 throws XMLException {
		String[] values = getXmlFields(1, 1, xpath);
		return values[0];
	}






	/**
	 * Returns all Elements in an XML document that match
	 * the specified partial xpath.
	 * The xpathspec specifies the tail end (right side) of an xpath.
	 * For example:
	 * <ul>
	 *   <li> "gamma": return the contents of all elements named "gamma"
	 *   <li> "beta/gamma": return the contents of all elements
	 *         named "gamma" whose immediate parent is named "beta",
	 *         no matter what the higher level nodes are named.
	 *
	 *   <li> "alpha/beta/gamma": return the contents of all
	 *         elements named "gamma" whose
	 *         immediate ancestors going up the chain are "beta", "alpha".
	 *   <li> "beta/gamma@tau": find all beta/gamma as above,
	 *         and for those having an
	 *         attribute "tau" return the value of "tau".
	 * </ul>
	 *
	 * <b>Notes</b> : getXmlFields ...
	 * <ul>
	 *   <li> Trims all leading and trailing white space.
	 *   <li> <b>Never</b> returns a null or zero-length String.
	 *        If the specified xpath or
	 *        attribute exists in the document,
	 *        but it's content is empty or all blank,
	 *        it is <b>not</b> returned.
	 *   <li> Never returns a null array. If there are no results,
	 *        the returned array has length zero.
	 *   <li> If the specified xpath is an interior node (not a leaf),
	 *        returns the concatenation of all leafs under it.
	 *        For example if the xpath is <code>"beta"</code>
	 *        and the xml document contains:
	 *        <p>
	 *        <code>&lt;beta&gt;<br>
	 *        b1 <br>
	 *        &lt;gamma&gt; ggg &lt;/gamma&gt;<br>
	 *        b2 <br>
	 *        &lt;iota&gt; iii &lt;/iota&gt; <br>
	 *        b3 <br>
	 *        &lt;/beta&gt;</code>
	 *        <p>
	 *   the returned string will be <code>"b1gggb2iiib3"</code>.
	 * </ul>
	 *
	 *
	 * @param minnum   Minimum number of times xpathspec must be found.<br>
	 *      If there are fewer than minnum occurances of the
	 *      xpathspec containing non-blank data,
	 *      an XMLException is thrown.<br>
	 *      Occurances of xpathspec that are empty or all blank
	 *      are not counted: see Notes above.
	 * @param maxnum Maximum number of times xpathspec must be found.<br>
	 *      If there are more than maxnum occurances of the
	 *      xpathspec containing non-blank data,
	 *      an XMLException is thrown.<br>
	 *      If maxnum == 0, it is considered to be infinity.<br>
	 *      Occurances of xpathspec that are empty or all blank
	 *      are not counted: see Notes above.
	 * @param  xpathspec the partial xpath specification; see above.
	 *
	 * @return                The xmlFields value
	 * @throws  XMLException  if the minnum or maxnum constraints are violated.
	 */
	public Element[] getXmlElements(
		int minnum,			// min num times xpathspec must be found
		int maxnum,			// max num times, or 0 ==> no max
		String xpathspec)
		throws XMLException
	{
		int inode;
		int jj;
		LinkedList reslist = new LinkedList();
		String xpath = xpathspec;

		// Parse the xpath into components
		LinkedList toklist = new LinkedList();
		StringTokenizer stok = new StringTokenizer(xpath, "/", false);
		while (stok.hasMoreTokens()) {
			toklist.add(stok.nextToken());
		}
		String[] xpathparts = (String[]) toklist.toArray(new String[0]);
		String tagname = xpathparts[xpathparts.length - 1];

		// Get all nodes matching last xpath component
		NodeList nodelist = doc.getElementsByTagName(tagname);
		if (bugs >= 1)
			prtln("getXmlFields: xpath: \"" + xpath
				 + "\"  num nodes: " + nodelist.getLength());

		int numfound = 0;
		for (inode = 0; inode < nodelist.getLength(); inode++) {
			Node nd = nodelist.item(inode);
			// Check that all xpath components match.
			// Work backwards through the xpathparts
			// as we work up the tree, comparing tag names.
			boolean allmatch = true;
			Node testnode = nd;
			for (jj = xpathparts.length - 1; jj >= 0; jj--) {
				if (testnode == null) {
					allmatch = false;
					break;
				}
				if (testnode.getNodeType() != Node.ELEMENT_NODE
					 || !(testnode.getNodeName().equals(xpathparts[jj]))) {
					allmatch = false;
					break;
				}
				testnode = testnode.getParentNode();
			}
			if (allmatch) {
				reslist.add( nd);
				numfound++;
			}
		} // for inode

		if (numfound < minnum)
			mkerror("Element at xpath \"" + xpath
				+ "\"  found less than " + minnum
				+ " " + ((minnum == 1) ? "time" : "times")
				+ " in file \"" + systemid + "\"");
		if (maxnum > 0 && numfound > maxnum)
			mkerror("Element at xpath \"" + xpath
				+ "\"  found more than " + minnum
				+ " " + ((minnum == 1) ? "time" : "times")
				+ " in file \"" + systemid + "\"");

		Element[] resvals = (Element[]) reslist.toArray(new Element[0]);
		return resvals;
	} // getXmlElements





	/**
	 * Returns all strings in an XML document that match
	 * the specified partial xpath.
	 * The xpathspec specifies the tail end (right side) of an xpath.
	 * For example:
	 * <ul>
	 *   <li> "gamma": return the contents of all elements named "gamma"
	 *   <li> "beta/gamma": return the contents of all elements
	 *         named "gamma" whose immediate parent is named "beta",
	 *         no matter what the higher level nodes are named.
	 *
	 *   <li> "alpha/beta/gamma": return the contents of all
	 *         elements named "gamma" whose
	 *         immediate ancestors going up the chain are "beta", "alpha".
	 *   <li> "beta/gamma@tau": find all beta/gamma as above,
	 *         and for those having an
	 *         attribute "tau" return the value of "tau".
	 * </ul>
	 *
	 * <b>Notes</b> : getXmlFields ...
	 * <ul>
	 *   <li> Trims all leading and trailing white space.
	 *   <li> <b>Never</b> returns a null or zero-length String.
	 *        If the specified xpath or
	 *        attribute exists in the document,
	 *        but it's content is empty or all blank,
	 *        it is <b>not</b> returned.
	 *   <li> Never returns a null array. If there are no results,
	 *        the returned array has length zero.
	 *   <li> If the specified xpath is an interior node (not a leaf),
	 *        returns the concatenation of all leafs under it.
	 *        For example if the xpath is <code>"beta"</code>
	 *        and the xml document contains:
	 *        <p>
	 *        <code>&lt;beta&gt;<br>
	 *        b1 <br>
	 *        &lt;gamma&gt; ggg &lt;/gamma&gt;<br>
	 *        b2 <br>
	 *        &lt;iota&gt; iii &lt;/iota&gt; <br>
	 *        b3 <br>
	 *        &lt;/beta&gt;</code>
	 *        <p>
	 *   the returned string will be <code>"b1gggb2iiib3"</code>.
	 * </ul>
	 *
	 *
	 * @param minnum   Minimum number of times xpathspec must be found.<br>
	 *      If there are fewer than minnum occurances of the
	 *      xpathspec containing non-blank data,
	 *      an XMLException is thrown.<br>
	 *      Occurances of xpathspec that are empty or all blank
	 *      are not counted: see Notes above.
	 * @param maxnum Maximum number of times xpathspec must be found.<br>
	 *      If there are more than maxnum occurances of the
	 *      xpathspec containing non-blank data,
	 *      an XMLException is thrown.<br>
	 *      If maxnum == 0, it is considered to be infinity.<br>
	 *      Occurances of xpathspec that are empty or all blank
	 *      are not counted: see Notes above.
	 * @param  xpathspec the partial xpath specification; see above.
	 *
	 * @return                The xmlFields value
	 * @throws  XMLException  if the minnum or maxnum constraints are violated.
	 */

	public String[] getXmlFields(
		int minnum,			// min num times xpathspec must be found
		int maxnum,			// max num times, or 0 ==> no max
		String xpathspec)
		throws XMLException
	{
		int inode;
		int ii;
		LinkedList reslist = new LinkedList();
		String xpath = xpathspec;

		// Strip off the "@attrname" if any
		int ixat = xpath.indexOf("@");
		String attrname = null;
		if (ixat >= 0) {
			attrname = xpath.substring(ixat + 1);
			xpath = xpath.substring(0, ixat);
		}

		int numfound = 0;

		Element[] eles = getXmlElements( minnum, maxnum, xpath);
		for (ii = 0; ii < eles.length; ii++) {
			Element ele = eles[ii];
			String resval = null;
			if (attrname == null)
				resval = getAllContent( ele);
			else {
				// Get attribute value.
				resval = ele.getAttribute(attrname);
			}
			if (resval != null) {
				resval = resval.trim();
				if (resval.length() > 0) {
					reslist.add(resval);
					numfound++;
				}
			}
		} // if allmatch

		if (numfound < minnum)
			mkerror("xpath \"" + xpath + "\"  found less than " + minnum
				 + " " + ((minnum == 1) ? "time" : "times")
				 + " in file \"" + systemid + "\"");
		if (maxnum > 0 && numfound > maxnum)
			mkerror("xpath \"" + xpath + "\"  found more than " + minnum
				 + " " + ((minnum == 1) ? "time" : "times")
				 + " in file \"" + systemid + "\"");

		String[] resvals = (String[]) reslist.toArray(new String[0]);
		return resvals;
	} // getXmlFields


	/**
	 *  Returns all strings in an XML document that match
     *  the specified partial xpath, or a
	 *  zero-length array if none were found.
	 *
	 * @param  xpathspec  the partial xpath specification.
	 * @return            The xmlFields value
	 */
	public String[] getXmlFields(String xpathspec) {
		try {
			return getXmlFields(0, 0, xpathspec);
		} catch (XMLException e) {
			return new String[]{};
		}
	}



	/**
	 *  Returns the first occurance of the field that matches the specified partial xpath, or a
	 *  an empty String if not found.
	 *
	 * @param  xpathspec  the partial xpath specification.
	 * @return            The xmlField value or empty String.
	 */
	public String getXmlField(String xpathspec) {
		try {
			String [] vals = getXmlFields(0, 0, xpathspec);
			if(vals.length == 0)
				return "";
			else
				return vals[0];
		} catch (Throwable e) {
			return "";
		}
	}	
	

	/**
	 * Returns a concatenation of all the text and cdata content
	 * in a Node's subtree.
	 *
	 * @param  nd  DESCRIPTION
	 * @return     The allContent value
	 */
	private String getAllContent(Node nd) {
		StringBuffer resbuf = new StringBuffer();
		getAllContentSub(nd, resbuf);
		return resbuf.toString();
	}


	/**
	 * Returns a concatenation of all the text and cdata content
	 * in a Node's subtree.
	 *
	 * @param  nd      DESCRIPTION
	 * @param  resbuf  DESCRIPTION
	 */
	private void getAllContentSub(Node nd, StringBuffer resbuf) {
		if (nd.getNodeType() == Node.TEXT_NODE
			 || nd.getNodeType() == Node.CDATA_SECTION_NODE) {
			resbuf.append(nd.getNodeValue());
		}
		else {
			Node subnd = nd.getFirstChild();
			while (subnd != null) {
				getAllContentSub(subnd, resbuf);
				subnd = subnd.getNextSibling();
			}
		}
	}


	/**
	 * Determines whether the parser found any validation errors.
	 *
	 * @return    True if errors were found, else false.
	 * @see       #getErrors()
	 */
	public boolean hasErrors() {
		return hasErrors;
	}


	/**
	 * Determines whether the parser found any validation warnings.
	 *
	 * @return    True if warnings were found, else false.
	 * @see       #getWarnings()
	 */
	public boolean hasWarnings() {
		return hasWarnings;
	}


	/**
	 * Gets a human-readable validation error report
	 * if errors were found in the XML,
	 * otherwise returns an empty StringBuffer.
	 *
	 * @return    Error messages or an empty StringBuffer.
	 * @see       #hasErrors()
	 */
	public StringBuffer getErrors() {
		return errors;
	}


	/**
	 * Gets a human-readable validation warning report
	 * if warnings were found in the XML,
	 * otherwise returns an empty StringBuffer.
	 *
	 * @return    Warning messages or an empty StringBuffer.
	 * @see       #hasWarnings()
	 */
	public StringBuffer getWarnings() {
		return warnings;
	}


	/**
	 * Throws an XMLException.
	 *
	 * @param  msg               DESCRIPTION
	 * @exception  XMLException  DESCRIPTION
	 */
	static void mkerror(String msg)
		 throws XMLException {
		throw new XMLException(msg);
	}


	/**
	 * Prints a string with no newline.
	 *
	 * @param  msg  DESCRIPTION
	 */
	static void prtstg(String msg) {
		System.out.print(msg);
	}


	/**
	 * Prints a string with a final newline.
	 *
	 * @param  msg  DESCRIPTION
	 */
	static void prtln(String msg) {
		System.out.println(msg);
	}

} // end class Xmldoc


