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
import org.xml.sax.*;
import org.xml.sax.helpers.*;
import javax.servlet.*;
import java.text.*;
import org.dlese.dpc.vocab.MetadataVocab;
import org.dlese.dpc.webapps.tools.GeneralServletTools;

/**
 *  Loads metadata groups from files specified by a given XML configuration
 *  file.
 *
 * @author    Ryan Deardorff
 */
public class LoadMetadataOPML implements org.xml.sax.ContentHandler {

	private boolean debug = false;
	private Locator saxLocator;
	private String currentSAXElementName = "";
	private String configFile = "MetadataUI.xml";                  // name of base groups loader list
	private ServletContext servletContext = null;
	private MetadataVocab returnVocab = null;
	private ArrayList groupsList = new ArrayList();                // list of OPML lists files
	private ArrayList uiFiles = new ArrayList();                   // all OPML files to load
	private StringBuffer message = new StringBuffer();
	private StringBuffer currentChars = new StringBuffer();
	private String currentDir = "";
	private String contextAttributeName = "MetadataUI";            // MetadataVocab instance stored in app scope as this

	/**
	 *  Constructor for the LoadMetadataVocabs object that does nothing (uses
	 *  getMetadataVocabInstance() method to load and retrieve an instance of a
	 *  vocab).
	 *
	 * @param  debug
	 */
	public LoadMetadataOPML( boolean debug ) {
		this.debug = debug;
	}

	/**
	 *  Loads an instance of a MetadataVocab object and sets it as a servlet
	 *  context attribute.
	 *
	 * @param  servletContext        The java servlet context in which to store the
	 *      loaded vocabularies
	 * @param  vocabTextFile         Optional (can be NULL) file where vocabs get
	 *      listed as text strings (for use by log analysis/reporting)
	 * @param  contextAttributeName  The MetadataVocab instance is stored in app
	 *      scope as this
	 * @param  configFile            Base loader file (lists groups/fields
	 *      listings)
	 * @param  debug
	 */
	public static synchronized void getMetadataVocabInstance( String configFile,
	                                                          String contextAttributeName,
	                                                          ServletContext servletContext,
	                                                          String vocabTextFile,
	                                                          boolean debug ) {
		LoadMetadataOPML loader = new LoadMetadataOPML( debug );
		loader.contextAttributeName = contextAttributeName;
		loader.servletContext = servletContext;
		int splitUrl = configFile.lastIndexOf( "/" );
		if ( splitUrl > -1 ) {
			loader.currentDir = configFile.substring( 0, splitUrl + 1 );
			String filename = configFile.substring( splitUrl, configFile.length() );
			if ( !loader.currentDir.startsWith( "http://" ) ) {
				try {
					loader.currentDir = getAbsolutePath( loader.currentDir, servletContext );
				}
				catch ( Exception e ) {
					e.printStackTrace();
				}
			}
			loader.configFile = loader.currentDir + filename;
		}
		loader.load();
		servletContext.setAttribute( "LoadMetadataOPML", loader );
	}

	/**
	 *  Gets the metadataVocabInstance attribute of the LoadMetadataOPML class
	 *
	 * @param  configFile
	 * @return             The metadataVocabInstance value
	 */
	public static synchronized MetadataVocab getMetadataVocabInstance( String configFile ) {
		LoadMetadataOPML loader = new LoadMetadataOPML( true );
		int splitUrl = configFile.lastIndexOf( "/" );
		if ( splitUrl > -1 ) {
			loader.currentDir = configFile.substring( 0, splitUrl + 1 );
			String filename = configFile.substring( splitUrl, configFile.length() );
			loader.configFile = loader.currentDir + filename;
		}
		loader.load();
		return loader.returnVocab;
	}

	/**
	 *  Load (or reload) the vocabulary
	 */
	public void load() {
		uiFiles.clear();
		message.setLength( 0 );
		try {
			XMLReader reader = null;
			try {
				reader = XMLReaderFactory.createXMLReader();
			}
			catch ( SAXException e2 ) {
				try {
					reader = XMLReaderFactory.createXMLReader( "org.apache.xerces.parsers.SAXParser" );
				}
				catch ( SAXException e3 ) {
					throw new NoClassDefFoundError( "No SAX parser is available" );
				}
			}
			reader.setContentHandler( this );
			reader.parse( configFile );
			for ( int i = 0; i < groupsList.size(); i++ ) {
				reader.parse( (String)groupsList.get( i ) );
			}
			MetadataVocab vocab = new MetadataVocabOPML( debug, configFile, servletContext );
			reader.setContentHandler( vocab );
			for ( int i = 0; i < uiFiles.size(); i++ ) {
				String opmlFile = ( (String)uiFiles.get( i ) ).replaceAll( "\\\\", "/" )
					.replaceAll( "//", "/" ).replaceAll( " ", "%20" ).replaceFirst( "http:/", "http://" );
				try {
					reader.parse( opmlFile );
				}
				catch ( Exception e ) {
					vocab.addError( "<b>ERROR loading OPML - " + opmlFile + "</b><br>" + e.getMessage() );
					e.printStackTrace();
				}
			}
			vocab.doneLoading();
			if ( servletContext != null ) {
				servletContext.setAttribute( contextAttributeName, vocab );
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
			returnVocab = new MetadataVocabOPML( debug, configFile, servletContext );
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
	 *  Gets the absolute path to a given file or directory. Assumes the path
	 *  passed in is eithr already absolute (has leading slash) or is relative to
	 *  the context root (no leading slash). If the string passed in does not begin
	 *  with a slash ("/"), then the string is converted. For example, an init
	 *  parameter to a config file might be passed in as
	 *  "WEB-INF/conf/serverParms.conf" and this method will return the
	 *  corresponding absolute path "/export/devel/tomcat/webapps/myApp/WEB-INF/conf/serverParms.conf."
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
	 * @param  servletContext
	 * @return                       The absolute path to the given file or path.
	 * @exception  ServletException  An exception related to this servlet
	 */
	private static String getAbsolutePath( String fname, ServletContext servletContext )
		 throws ServletException {
		return GeneralServletTools.getAbsolutePath( fname, servletContext );
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
		if ( atts.getValue( "uri" ) != null ) {
			currentDir = atts.getValue( "uri" );
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
		if ( localName.equals( "file" ) ) {
			uiFiles.add( currentDir + currentChars.toString() );
		}
		else if ( localName.equals( "groups" ) ) {
			groupsList.add( currentDir + currentChars.toString() );
		}
		currentChars.setLength( 0 );
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
		currentChars.append( new String( ch, start, length ).trim() );
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

	/**
	 *  Output a line of text to standard out, with datestamp, if debug is set to
	 *  true.
	 *
	 * @param  s  The String that will be output.
	 */
	private final void prtln( String s ) {
		if ( debug ) {
			System.out.println( getDateStamp() + " " + s );
		}
	}

	/**
	 *  Return a string for the current time and date, sutiable for display in log
	 *  files and output to standout:
	 *
	 * @return    The dateStamp value
	 */
	public static String getDateStamp() {
		return
			new SimpleDateFormat( "MMM d, yyyy h:mm:ss a zzz" ).format( new Date() );
	}
}

