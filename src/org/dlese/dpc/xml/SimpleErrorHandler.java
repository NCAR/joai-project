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

// Imported JAXP classes
import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.Attributes;

// SAX import
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;
//import org.xml.sax.Parser;
//import org.xml.sax.helpers.ParserFactory;
import org.xml.sax.helpers.ParserAdapter;

// Imported java.io classes
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.FileWriter;
import java.io.StringReader;
import java.util.*;

/**
 *  A simple XML error handler that catches XML errors and warnings and saves them to
 *  StringBuffers so the errors can be extracted and displayed.
 *
 * @author    John Weatherley
 */
public class SimpleErrorHandler extends DefaultHandler implements LexicalHandler {

	private boolean error;
	private boolean warning;
	private boolean containsDTD;
	private StringBuffer errorBuff, warningBuff;


	/**
	 *  Constructor for the ErrorHandler object
	 *
	 * @param  errorBuff    A StringBuffer that will capture error messages, if present.
	 * @param  warningBuff  A StringBuffer that will capture warning messages, if present.
	 */
	SimpleErrorHandler(StringBuffer errorBuff, StringBuffer warningBuff) {
		super();
		this.errorBuff = errorBuff;
		this.warningBuff = warningBuff;
		error = false;
		warning = false;
		containsDTD = false;
	}


	/**
	 *  DESCRIPTION
	 *
	 * @param  exc  DESCRIPTION
	 */
	public void error(SAXParseException exc) {
		errorBuff.append(" Error: " + exc.getMessage() + "\n");
		error = true;
	}


	/**
	 *  Determines whether the parser found any validation errors.
	 *
	 * @return    True if errors were found, else false.
	 */
	public boolean hasErrors() {
		return error;
	}


	/**
	 *  Determines whether the parser found any validation warnings.
	 *
	 * @return    True if warnings were found, else false.
	 */
	public boolean hasWarnings() {
		return warning;
	}


	/**
	 *  DESCRIPTION
	 *
	 * @param  exc  DESCRIPTION
	 */
	public void fatalError(SAXParseException exc) {
		errorBuff.append(" Fatal error:" + exc.getMessage() + "\n");
		error = true;
	}


	/**
	 *  DESCRIPTION
	 *
	 * @param  exc  DESCRIPTION
	 */
	public void warning(SAXParseException exc) {
		if (warning == false) {
			warningBuff.append(" Warning: " + exc.getMessage() + " (first warning reported only)\n");
			warning = true;
		}
	}


	// LexicalHandler methods; all no-op except startDTD().

	// Set containsDTD to true when startDTD event occurs.
	/**
	 *  DESCRIPTION
	 *
	 * @param  name              DESCRIPTION
	 * @param  publicId          DESCRIPTION
	 * @param  systemId          DESCRIPTION
	 * @exception  SAXException  DESCRIPTION
	 */
	public void startDTD(String name, String publicId, String systemId)
		 throws SAXException {
		containsDTD = true;
	}


	/**
	 *  DESCRIPTION
	 *
	 * @exception  SAXException  DESCRIPTION
	 */
	public void endDTD() throws SAXException {
	}


	/**
	 *  DESCRIPTION
	 *
	 * @param  name              DESCRIPTION
	 * @exception  SAXException  DESCRIPTION
	 */
	public void startEntity(String name) throws SAXException {
	}


	/**
	 *  DESCRIPTION
	 *
	 * @param  name              DESCRIPTION
	 * @exception  SAXException  DESCRIPTION
	 */
	public void endEntity(String name) throws SAXException {
	}


	/**
	 *  DESCRIPTION
	 *
	 * @exception  SAXException  DESCRIPTION
	 */
	public void startCDATA() throws SAXException {
	}


	/**
	 *  DESCRIPTION
	 *
	 * @exception  SAXException  DESCRIPTION
	 */
	public void endCDATA() throws SAXException {
	}


	/**
	 *  DESCRIPTION
	 *
	 * @param  ch                DESCRIPTION
	 * @param  start             DESCRIPTION
	 * @param  length            DESCRIPTION
	 * @exception  SAXException  DESCRIPTION
	 */
	public void comment(char ch[], int start, int length) throws SAXException {
	}
}


