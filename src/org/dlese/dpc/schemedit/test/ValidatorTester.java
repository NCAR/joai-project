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
package org.dlese.dpc.schemedit.test;

import org.dlese.dpc.xml.XMLFileFilter;
import org.dlese.dpc.xml.Dom4jUtils;
import org.dlese.dpc.util.*;

import javax.xml.XMLConstants;
import javax.xml.validation.Validator;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.dom.DOMSource;

import org.xml.sax.SAXException;
import org.xml.sax.InputSource;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXParseException;

import org.w3c.dom.*;

import java.io.*;
import java.lang.*;
import java.net.URL;

public class ValidatorTester {

	Validator validator = null;
	String schemaURI = null;
	
	public ValidatorTester (String schemaURI) throws Exception {
		this.schemaURI = schemaURI;
		prtln ("schemaNS_URI: " + XMLConstants.W3C_XML_SCHEMA_NS_URI);
		SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
		
		Schema schema = null;
		try {
			if (schemaURI.startsWith("http://"))
				schema = factory.newSchema(new URL (schemaURI));
			else
				schema = factory.newSchema(new File (schemaURI));
			if (schema == null)
				throw new Exception ("Schema could not be read from " + schemaURI);
		} catch (Throwable t) {
			throw new Exception ("Validator init error: " + t.getMessage());
		}
		
		this.validator = schema.newValidator();
	}
		
	private boolean hasProtocol (String xml) {
		return true;
	}
	
	private String preprocessXML (String xml) {
		if (!hasProtocol (xml))
			return "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" + xml;
		else
			return xml;
	}
	
	private final boolean doValidate(String xml, StringBuffer messages, boolean showWarnings) {
		boolean isValid = true;
		
		// xml = preprocessXML (xml);
		
		try {
			
			Source source = new StreamSource (new StringReader (xml));
			
			StringBuffer errorBuff = new StringBuffer();
			StringBuffer warningBuff = new StringBuffer();
			MyErrorHandler handler = new MyErrorHandler(errorBuff, warningBuff);
			validator.setErrorHandler(handler);	

			prtln ("about to validate: " + xml);
			validator.validate (source);
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

	public final String validateString(String s) {
		return validateString(s, false);
	}
	
	public final String validateString(String s, boolean showWarnings) {
		StringBuffer messages = new StringBuffer();
		if (doValidate(s, messages, showWarnings))
			return null;
		else
			return messages.toString();
	}
	
	public final String validateFile(File f) {
		return validateFile(f, false);
	}
	
	public final String validateFile(File f, boolean showWarnings) {
		try {
			StringBuffer messages = new StringBuffer();
			if (doValidate(f.toString(), messages, showWarnings))
				return null;
			else
				return messages.toString();
		} catch (Throwable e) {
			return "Unable to validate: " + e;
		}
	}
	
	
	void validateDir (String path) {
		File [] files = new File (path).listFiles(new XMLFileFilter());
		for (int i=0;i<files.length;i++) {
			File file = files[i];
			StringBuffer messages = new StringBuffer();
			String msg = validateFile (file);
			// prtln ("msg: " + msg);
			if (msg == null) {
				prtln ("\n" + file.getName() + " : VALID");
			}
			else {
				prtln ("\n" + file.getName() + " : " + msg);
			}
		}
	}
	
	public static void main (String[] args){
		prtln ("\n\n==================================================");
		String xsdPath = "O:/www.dlese.org/docroot/Metadata/adn-item/0.6.50/record.xsd";
		ValidatorTester vt = null;
		
		try {		
			String xmlFormat = "adn";
			String schemaURI = SchemaRegistry.getSchemaPath(xmlFormat);
			if (schemaURI == null)
				throw new Exception ("SchemaURI not found for " + xmlFormat);

			vt = new ValidatorTester (xsdPath);
		} catch (Exception e) {
			prtln (e.getMessage());
			return;
		}
		
		// vt.validateDirTester();
		// vt.validateFileTester();
		// vt.validateStringTester();
		// vt.validateDocTester();
	}
		
	private void validateFileTester () {
		String path = "C:/tmp/validate/adn.xml";
		String msg = validateFile (new File(path));
		
		prtln ("\nvalidateFileTester()");
		prtln ("\t source: " + path);
		if (msg == null)
			prtln ("VALID");
		else
			prtln (msg);
	}
	
	private void validateDocTester () {
		String path = "C:/tmp/validate/adn.xml";
		prtln ("\nvalidateDocTester()");
		String msg = null;
		try {
			org.dom4j.Document doc = Dom4jUtils.getXmlDocument(new File (path));
			msg = validateString (doc.asXML());
		} catch (Throwable t) {
			msg = t.getMessage();
		}
		
		prtln ("\t source: " + path);
		if (msg == null)
			prtln ("VALID");
		else
			prtln (msg);
	}
	
	private void validateDirTester () {
		String collection = "1176742803770";
		String path = "C:/Documents and Settings/ostwald/devel/records/adn/" + collection;
		validateDir (path);
	}
	
	private static void prtln (String s) {
		System.out.println (s);
	}
	
	public class MyErrorHandler implements ErrorHandler {
	
		private boolean error;
		private boolean warning;
		private boolean containsDTD;
		private StringBuffer errorBuff, warningBuff;
		
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

	// ================================================================================
		public static void validate (String xmlPath, String xsdPath) throws Exception {
	
		// create a SchemaFactory capable of understanding WXS schemas
		prtln ("schemaNS_URI: " + XMLConstants.W3C_XML_SCHEMA_NS_URI);
		SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
		
		// load a WXS schema, represented by a Schema instance
		Schema schema = null;
		
		/* use schema from file */
		schema = factory.newSchema(new File(xsdPath));
		
		/* use schema from web */
		// String adnURI = "http://www.dlese.org/Metadata/adn-item/0.6.50/record.xsd";
		// schema = factory.newSchema(new URL(adnURI));
		
		/* use schema from instance document */
		// schema = factory.newSchema();
		
		/* create a Validator instance, which can be used to validate an instance document */
		Validator validator = schema.newValidator();
		
		/* the xml to be validated */
		Source mysource = new StreamSource (xmlPath);
		
		// validation errors passed as SAXException message
		try {
			validator.validate(mysource);
		} catch (SAXException e) {
			prtln (e.getMessage());
		}
	}

	
	public class ForgivingErrorHandler implements ErrorHandler {
	
		public void warning(SAXParseException ex) {
			System.err.println(ex.getMessage());
		}
	
		public void error(SAXParseException ex) {
			System.err.println(ex.getMessage());
		}
	
		public void fatalError(SAXParseException ex) throws SAXException {
			throw ex;
		}
	}
	
	
	
}

