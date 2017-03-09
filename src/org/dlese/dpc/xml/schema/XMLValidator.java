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
package org.dlese.dpc.xml.schema;

import org.dlese.dpc.xml.XMLFileFilter;
import org.dlese.dpc.util.*;

import javax.xml.XMLConstants;
import javax.xml.validation.Validator;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import org.xml.sax.SAXException;
import org.xml.sax.InputSource;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXParseException;

import org.w3c.dom.*;

import java.io.*;
import java.net.*;

/**
 *  Validate XML against a schema using a cached validator.
 *
 * @author    Jonathan Ostwald
 */
public class XMLValidator {

	Validator validator = null;
	URI uri = null;


	/**
	 *  Constructor for the XMLValidator object
	 *
	 * @param  uri            NOT YET DOCUMENTED
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public XMLValidator(URI uri) throws Exception {
		this.uri = uri;
		SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
		Schema schema = null;
		try {
			String uriScheme = uri.getScheme();
			if (uriScheme != null && uriScheme.equals("http"))
				schema = factory.newSchema(uri.toURL());
			else
				schema = factory.newSchema(new File(uri));
			if (schema == null)
				throw new Exception("Schema could not be read from " + uri.toString());
		} catch (Throwable t) {
			throw new Exception("Validator init error: " + t.getMessage());
		}

		this.validator = schema.newValidator();
	}


	/**
	 *  Validate XML and stores validation messages for errors, and optionally,
	 *  warnings.
	 *
	 * @param  xml           NOT YET DOCUMENTED
	 * @param  messages      NOT YET DOCUMENTED
	 * @param  showWarnings  NOT YET DOCUMENTED
	 * @return               NOT YET DOCUMENTED
	 */
	private final boolean doValidate(String xml, StringBuffer messages, boolean showWarnings) {
		boolean isValid = true;
		try {

			Source source = new StreamSource(new StringReader(xml));

			StringBuffer errorBuff = new StringBuffer();
			StringBuffer warningBuff = new StringBuffer();
			MyErrorHandler handler = new MyErrorHandler(errorBuff, warningBuff);
			validator.setErrorHandler(handler);

			validator.validate(source);
			if (showWarnings && handler.hasWarnings()) {
				// warnings
				messages.append("WARNING: ");
				messages.append(warningBuff.toString());
			}
			if (handler.hasErrors()) {
				// not valid
				messages.append("NOT VALID: ");
				messages.append(errorBuff.toString());
				isValid = false;
			}
		} catch (Exception e) {
			// Serious problem!
			messages.append("NOT WELL-FORMED: " + e.getMessage());
			isValid = false;
		}
		return isValid;
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  s  NOT YET DOCUMENTED
	 * @return    NOT YET DOCUMENTED
	 */
	public final String validateString(String s) {
		return validateString(s, false);
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  s             NOT YET DOCUMENTED
	 * @param  showWarnings  NOT YET DOCUMENTED
	 * @return               NOT YET DOCUMENTED
	 */
	public final String validateString(String s, boolean showWarnings) {
		StringBuffer messages = new StringBuffer();
		if (doValidate(s, messages, showWarnings))
			return null;
		else
			return messages.toString();
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  f  NOT YET DOCUMENTED
	 * @return    NOT YET DOCUMENTED
	 */
	public final String validateFile(File f) {
		return validateFile(f, false);
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  f             NOT YET DOCUMENTED
	 * @param  showWarnings  NOT YET DOCUMENTED
	 * @return               NOT YET DOCUMENTED
	 */
	public final String validateFile(File f, boolean showWarnings) {
		try {
			String input = Files.readFile(f).toString();
			return validateString(input, showWarnings);
		} catch (Throwable e) {
			return "Unable to validate: " + e;
		}
	}


	/**
	 *  The main program for the XMLValidator class
	 *
	 * @param  args  The command line arguments
	 */
	public static void main(String[] args) {
		prtln("\n\n======= XMLValidator ========================");
		// String xsdPath = "file:/C:/Documents%20and%20Settings/ostwald/devel/tmp/smile/smile-item.xsd";
		String xsdPath = "ostwald/devel/tmp/smile/smile-item.xsd";
		// String xsdPath = "http://sv.berkeley.edu/~db/SMILE/Metadata/smile-item/0.0.1/smile-item.xsd";
		String encoded = URLEncoder.encode(xsdPath);
		URI uri = null;
		try {
			uri = new URI(xsdPath);
		} catch (Exception e) {
			prtln("bogus uri: " + e.getMessage());
			return;
		}
		try {
			new XMLValidator(uri);
		} catch (Exception e) {
			prtln(e.getMessage());
			return;
		}
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  s  NOT YET DOCUMENTED
	 */
	private static void prtln(String s) {
		System.out.println(s);
	}


	/**
	 *  Error Handler to store validation messages, and compile them into a string
	 *  buffer as a report.
	 *
	 * @author     Jonathan Ostwald
	 * @version    $Id: XMLValidator.java,v 1.4 2009/03/20 23:34:01 jweather Exp $
	 */
	public class MyErrorHandler implements ErrorHandler {

		private boolean error;
		private boolean warning;
		private boolean containsDTD;
		private StringBuffer errorBuff, warningBuff;


		/**
		 *  Constructor for the MyErrorHandler object
		 *
		 * @param  errorBuff    NOT YET DOCUMENTED
		 * @param  warningBuff  NOT YET DOCUMENTED
		 */
		MyErrorHandler(StringBuffer errorBuff, StringBuffer warningBuff) {
			super();
			this.errorBuff = errorBuff;
			this.warningBuff = warningBuff;
			error = false;
			warning = false;
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
	}

}

