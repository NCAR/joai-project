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
package org.dlese.dpc.vocab;

import java.io.*;
import java.util.*;
import org.dlese.dpc.vocab.MetadataVocab;
import org.xml.sax.*;
import org.xml.sax.helpers.*;
import javax.servlet.*;

/**
 *  Loads controlled vocabularies from files specified by a given XML
 *  configuration file. The following are examples of how to use this class to
 *  obtain a MetadataVocab class instance that is <i>not</i> stored as a servlet
 *  context attribute: <h5>No database connection</h5> <tt>MetadataVocab vocab =
 *  new LoadMetadataVocabs().getVocabs(<br>
 *  &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
 *  "/devel/ryandear/dds_test_data/vocab/DLESE_Errors.xml",<br>
 *  &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
 *  "org.apache.xerces.parsers.SAXParser" );</tt> <h5>Connected to an SQL
 *  database (for new encoded Id assignment and tracking of label changes)</h5>
 *  <tt>MetadataVocab vocab = new LoadMetadataVocabs().getVocabs(<br>
 *  &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
 *  "/devel/ryandear/dds_test_data/vocab/DLESE_Errors.xml",<br>
 *  &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
 *  "org.apache.xerces.parsers.SAXParser",<br>
 *  &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; "org.gjt.mm.mysql.Driver",
 *  <br>
 *  &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
 *  "jdbc:mysql://quake.dpc.ucar.edu:3306/DLESE_Systems",<br>
 *  &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; "myUser",<br>
 *  &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; "myPasswd" );</tt>
 *
 * @author    Ryan Deardorff
 */
public class LoadMetadataVocabs implements org.xml.sax.ContentHandler {
	private Locator saxLocator;
	private String xmlParserClass;
	private String currentSAXElementName = "";
	private String configDir;
	private String configFile;
	private String vocabTextFile = null;
	private ServletContext servletContext = null;
	private MetadataVocab returnVocab = null;
	private ArrayList uiFiles = new ArrayList();
	private ArrayList definitionFiles = new ArrayList();
	private StringBuffer message = new StringBuffer();
	private String sqlDriver = null;
	private String sqlURL = null;
	private String sqlUser = null;
	private String sqlPassword = null;
	private boolean parsingDefinitions = true;

	/**
	 *  Constructor for the LoadMetadataVocabs object that does nothing (use
	 *  getMetadataVocabs() method to load and retrieve and instance of a vocab).
	 */
	public LoadMetadataVocabs() { }

	/**
	 *  Constructor for the LoadMetadataVocabs object that loads an instance of a
	 *  MetadataVocab object and sets it as a servlet context attribute.
	 *
	 * @param  configDir       Base dir of vocab XML files
	 * @param  configFile      XML defining vocabs to be loaded
	 * @param  xmlParserClass  SAX parser class, i.e.
	 *      "org.apache.xerces.parsers.SAXParser"
	 * @param  servletContext  The java servlet context in which to store the
	 *      loaded vocabularies
	 * @param  sqlDriver       Optional (can be NULL) driver class for SQL access,
	 *      i.e. "org.gjt.mm.mysql.Driver"
	 * @param  sqlURL          Optional (can be NULL) URL to SQL access, i.e.
	 *      "jdbc:mysql://quake.dpc.ucar.edu:3306/DLESE_Systems"
	 * @param  sqlUser         Optional (can be NULL) username for access to SQL
	 *      tables
	 * @param  sqlPassword     Optional (can be NULL) password for access to SQL
	 *      tables
	 * @param  vocabTextFile   Optional (can be NULL) file where vocabs get listed
	 *      as text strings (for use by log analysis/reporting)
	 */
	public LoadMetadataVocabs( String configDir,
	                           String configFile,
	                           String xmlParserClass,
	                           ServletContext servletContext,
	                           String sqlDriver,
	                           String sqlURL,
	                           String sqlUser,
	                           String sqlPassword,
	                           String vocabTextFile ) {
		this.servletContext = servletContext;
		this.xmlParserClass = xmlParserClass;
		this.configDir = configDir + "/";
		this.configFile = configDir + "/" + configFile;
		this.sqlDriver = sqlDriver;
		this.sqlURL = sqlURL;
		this.sqlUser = sqlUser;
		this.sqlPassword = sqlPassword;
		this.vocabTextFile = vocabTextFile;
		load();
		servletContext.setAttribute( "LoadMetadataVocabs", this );
	}

	/**
	 *  Load (or reload) the vocabulary
	 */
	public void load() {
		uiFiles.clear();
		definitionFiles.clear();
		message.setLength( 0 );
		parsingDefinitions = true;
		try {
			InputSource inputSource = new InputSource( configFile );
			XMLReader reader = null;
			try {
				reader = XMLReaderFactory.createXMLReader( xmlParserClass );
			} catch (SAXException e) {
				try {
				  	reader = XMLReaderFactory.createXMLReader();
				}
				catch (SAXException e2) {
					throw new NoClassDefFoundError("No SAX parser is available");
				}
			}
			reader.setContentHandler( this );
			reader.parse( inputSource );
			MetadataVocabTermsGroups vocab = new MetadataVocabTermsGroups( sqlDriver, sqlURL, sqlUser, sqlPassword, vocabTextFile );
			reader.setContentHandler( vocab );
			for ( int i = 0; i < uiFiles.size(); i++ ) {
				inputSource = new InputSource( configDir + (String)uiFiles.get( i ) );
				reader.parse( inputSource );
				message.append( "LOADED UI file " + (String)uiFiles.get( i ) + "\n" );
			}
			vocab.setParsingDefinitions( true );
			for ( int i = 0; i < definitionFiles.size(); i++ ) {
				inputSource = new InputSource( configDir + (String)definitionFiles.get( i ) );
				reader.parse( inputSource );
				message.append( "Loaded definition file " + (String)definitionFiles.get( i ) + "\n" );
			}
			vocab.doneLoading();
			if ( servletContext != null ) {
				servletContext.setAttribute( "MetadataVocab", vocab );
			}
			else {
				returnVocab = vocab;
			}
		}
		catch ( Exception e ) {
			System.out.println( "Error loading vocabulary:" );
			e.printStackTrace();
		}
		if ( returnVocab == null ) {
			returnVocab = new MetadataVocabTermsGroups( sqlDriver, sqlURL, sqlUser, sqlPassword, vocabTextFile );
		}
	}

	/**
	 *  Gets the message attribute of the LoadMetadataVocabs object
	 *
	 * @return    The message value
	 */
	public String getMessage() {
		return message.toString();
	}

	/**
	 *  Gets the configFile attribute of the LoadMetadataVocabs object
	 *
	 * @return    The configFile value
	 */
	public String getConfigFile() {
		return configFile;
	}

	/**
	 *  Gets the vocabTextFile attribute of the LoadMetadataVocabs object
	 *
	 * @return    The vocabTextFile value
	 */
	public String getVocabTextFile() {
		return vocabTextFile;
	}

	/**
	 *  Gets the databaseURL attribute of the LoadMetadataVocabs object
	 *
	 * @return    The databaseURL value
	 */
	public String getDatabaseURL() {
		return sqlURL;
	}

	// SAX parser implementation:
	//----------------------------------------------------------------------------------

	/**
	 *  (SAX) Sets the SAX locator, which indicates the current position of the
	 *  parser within the document (line number, column number). Could be used to
	 *  indicate the spot where an error occured.
	 *
	 * @param  locator  The new documentLocator value
	 */
	public void setDocumentLocator( Locator locator ) {
		saxLocator = locator;
	}

	/**
	 *  (SAX) Required by SAX, but not used here
	 *
	 * @exception  SAXException
	 */
	public void startDocument() throws SAXException {
	}

	/**
	 *  (SAX) Required by SAX, but not used here
	 *
	 * @exception  SAXException
	 */
	public void endDocument() throws SAXException {
	}

	/**
	 *  (SAX) Required by SAX, but not used here
	 *
	 * @param  prefix
	 * @param  uri
	 * @exception  SAXException
	 */
	public void startPrefixMapping( String prefix, String uri ) throws SAXException {
	}

	/**
	 *  (SAX) Required by SAX, but not used here
	 *
	 * @param  prefix
	 * @exception  SAXException
	 */
	public void endPrefixMapping( String prefix ) throws SAXException {
	}

	/**
	 *  (SAX) Invoked upon opening tag of an XML element
	 *
	 * @param  namespaceURI      XML namespace
	 * @param  localName         local tag name
	 * @param  qName             fully qualified tag name
	 * @param  atts              tag attributes
	 * @exception  SAXException
	 */
	public void startElement( String namespaceURI,
	                          String localName,
	                          String qName,
	                          Attributes atts ) throws SAXException {
		currentSAXElementName = localName;
		if ( localName.equals( "user_interfaces" ) ) {
			parsingDefinitions = false;
		}
	}

	/**
	 *  (SAX) Invoked upon closing tag of an XML element
	 *
	 * @param  namespaceURI      XML namespace
	 * @param  localName         local tag name
	 * @param  qName             fully qualified tag name
	 * @exception  SAXException
	 */
	public void endElement( String namespaceURI, String localName,
	                        String qName ) throws SAXException {
	}

	/**
	 *  (SAX) Element data (characters between tags)
	 *
	 * @param  ch                character array
	 * @param  start             starting index of character data
	 * @param  length            length of character data
	 * @exception  SAXException
	 */
	public void characters( char ch[], int start, int length ) throws SAXException {
		String charsValue = new String( ch, start, length );
		if ( currentSAXElementName.equals( "file" ) ) {
			if ( parsingDefinitions ) {
				definitionFiles.add( charsValue );
			}
			else {
				uiFiles.add( charsValue );
			}
		}
	}

	/**
	 *  (SAX) Reports any whitespace that is ignored because it falls outside of
	 *  the DTD or schema definition--usefull for re-generating the file with
	 *  indents intact, though.
	 *
	 * @param  ch
	 * @param  start
	 * @param  length
	 * @exception  SAXException
	 */
	public void ignorableWhitespace( char ch[], int start, int length ) throws SAXException {
	}

	/**
	 *  (SAX) Required by SAX, but not used here
	 *
	 * @param  target
	 * @param  data
	 * @exception  SAXException
	 */
	public void processingInstruction( String target, String data ) throws SAXException {
	}

	/**
	 *  (SAX) Required by SAX, but not used here
	 *
	 * @param  name
	 * @exception  SAXException
	 */
	public void skippedEntity( String name ) throws SAXException {
	}
}

