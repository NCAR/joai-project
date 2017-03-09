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
package org.dlese.dpc.util.uri;

import java.io.*;
import java.util.*;
import java.net.*;
import javax.servlet.http.*;
import javax.servlet.*;
import java.util.regex.*;
import org.xml.sax.*;
import org.xml.sax.helpers.*;

/**
 *  Utility class for mapping "static" URIs (i.e. "catalog_(\S+).htm") to
 *  dynamic ones (i.e. "view_resource.do?description=$1") using regexes
 *
 * @author     ryandear@ucar.edu
 * @created    May 16, 2006
 */
public class UriMappings implements org.xml.sax.ContentHandler {

	private HashMap attributes = null;                             // stores uri/mapto pairs
	private String xmlFile;                                        // xml file specifying uri/mapto pairs
	private String xmlParserClass;                                 // name of the SAX implementing class
	private Locator saxLocator;                                    // stores things like line number of the parser
	private static boolean debug = false;                          // debug output?

	/**
	 *  Constructor for the UrlMappings object
	 *
	 * @param  xmlParserClass
	 * @param  servletContext
	 */
	public UriMappings( ServletContext servletContext,
	                    String xmlParserClass ) {
		this.xmlParserClass = xmlParserClass;
		if ( servletContext == null ) {
			System.out.println( "ServletContext is NULL" );
		}
	}

	/**
	 *  Load URL mappings from the supplied configuration file
	 *
	 * @param  configFile
	 */
	public void loadMappings( String configFile ) {
		try {
			InputSource inputSource = new InputSource( configFile );
			XMLReader reader = null;
			try {
				reader = XMLReaderFactory.createXMLReader( xmlParserClass );
			}
			catch ( SAXException e ) {
				try {
					reader = XMLReaderFactory.createXMLReader();
				}
				catch ( SAXException e2 ) {
					throw new NoClassDefFoundError( "No SAX parser is available" );
				}
			}
			reader.setContentHandler( this );
			reader.parse( inputSource );
			prtln( "loadMappings() loaded URL mappings located at " + configFile );
		}
		catch ( Exception e ) {
			System.err.println( "Error loading URL mappings:" );
			e.printStackTrace();
		}
	}

	/**
	 *  Gets the forwardPage attribute of the UrlMappings object
	 *
	 * @param  request
	 * @return          The forwardPage value
	 */
	public String getForwardPage( HttpServletRequest request ) {
		String requestURI = request.getRequestURI();
		requestURI = requestURI.substring( request.getContextPath().length(), requestURI.length() );
		String forwardPage = null;
		Set s = attributes.keySet();
		Iterator i = attributes.keySet().iterator();
		// prtln( "getForwardPage() requestURI: " + requestURI );
		while ( ( forwardPage == null ) && i.hasNext() ) {
			String key = (String)i.next();
			Pattern p = Pattern.compile( key );
			Matcher m = p.matcher( requestURI );
			if ( m.matches() ) {
				prtln( "getForwardPage() matches: " + key );
				String mapto = (String)attributes.get( key );
				prtln( "getForwardPage() maps to: " + mapto );
				for ( int j = 1; j <= m.groupCount(); j++ ) {
					int ind = mapto.indexOf( "$" + ( j ) );
					if ( ind > -1 ) {
						mapto = mapto.substring( 0, ind )
							 + m.group( j )
							 + mapto.substring( ind + 2, mapto.length() );
					}
				}
				forwardPage = mapto;
				prtln( "getForwardPage() forwardPage = " + forwardPage );
			}
		}
		if ( forwardPage == null ) {
			forwardPage = "/error.jsp";
		}
		return forwardPage;
	}

	// SAX parser implementation:
	//----------------------------------------------------------------------------------

	/**
	 *  (SAX) Sets the SAX locator, which indicates the current position of the
	 *  parser within the document (line number, column number). Could be used to
	 *  indicate the spot where an error occured.
	 *
	 * @param  locator  The new saxLocator value
	 */
	public void setDocumentLocator( Locator locator ) {
		saxLocator = locator;
		xmlFile = saxLocator.getSystemId();
	}

	/**
	 *  (SAX) Invoked at the start of any document parse
	 *
	 * @exception  SAXException
	 */
	public void startDocument() throws SAXException {
		attributes = new HashMap();
	}

	/**
	 *  (SAX) Invoked at the end of parsing. Rewrite the definitions XML if new Ids
	 *  have been assigned.
	 *
	 * @exception  SAXException
	 */
	public void endDocument() throws SAXException {
	}

	/**
	 *  (SAX) Invoked upon opening tag of an XML element
	 *
	 * @param  namespaceURI
	 * @param  localName
	 * @param  qName
	 * @param  atts
	 * @exception  SAXException
	 */
	public void startElement( String namespaceURI,
	                          String localName,
	                          String qName,
	                          Attributes atts ) throws SAXException {
		if ( localName.equals( "map" ) ) {
			String uri = null;
			String mapto = null;
			for ( int i = 0; i < atts.getLength(); i++ ) {
				if ( atts != null ) {
					if ( atts.getLocalName( i ).equals( "uri" ) ) {
						uri = atts.getValue( i );
					}
					else if ( atts.getLocalName( i ).equals( "to" ) ) {
						mapto = atts.getValue( i );
					}
				}
			}
			if ( ( uri != null ) && ( mapto != null ) ) {
				// Store every uri/mapto attribute pair in a global hashMap:
				// prtln(" storing attributes " + uri + "/" + mapto);
				attributes.put( uri, mapto );
			}
		}
	}

	/**
	 *  (SAX) Invoked upon closing tag of an XML element (not used here)
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
	 *  (SAX) Element data (characters between tags, not used here)
	 *
	 * @param  ch
	 * @param  start
	 * @param  length
	 * @exception  SAXException
	 */
	public void characters( char ch[], int start, int length ) throws SAXException {
	}

	/**
	 *  (SAX) Reports any whitespace that is ignored because it falls outside of
	 *  the DTD or schema definition--usefull for re-generating the file with
	 *  indents intact.
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

	// Debugging
	//----------------------------------------------------------------------------------

	/**
	 *  Sets the debug attribute of the DDSServlet object
	 *
	 * @param  db  The new debug value
	 */
	public final static void setDebug( boolean db ) {
		debug = db;
	}	
	
	/**
	 *  Shorthand for System.out.println that prepends the name of the class
	 *
	 * @param  s
	 */
	private final static void prtln( String s ) {
		if ( debug ) {
			System.out.println( "org.dlese.dpc.util.uri.UriMappings." + s );
		}
	}
}

