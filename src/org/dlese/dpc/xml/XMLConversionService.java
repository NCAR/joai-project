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

import org.dlese.dpc.util.*;
import org.dlese.dpc.index.reader.*;

// Imported java.io classes
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.FileWriter;
import java.io.StringReader;
import java.util.*;
import java.text.*;
import java.io.*;
import java.text.*;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;
import javax.servlet.ServletException;

import javax.xml.transform.Transformer;

/**
 *  Manages the conversion of XML files from one format to another using XSL or
 *  Java classes. Caches the converted XML to disc and provides rapid access to
 *  the converted format. Conversions may be accomplished using XSL stylesheets
 *  or Java classes that implement one of either the {@link XMLFormatConverter}
 *  or {@link XMLDocumentFormatConverter} interfaces.
 *
 * @author     John Weatherley
 * @version    $Id: XMLConversionService.java,v 1.24 2007/02/23 21:20:00
 *      jweather Exp $
 * @see        XMLFormatConverter
 * @see        XMLDocumentFormatConverter
 */
public final class XMLConversionService {

	private static boolean debug = false;
	private File cacheDir = null;
	private Hashtable converters = new Hashtable();
	private boolean filter = false;


	/**
	 *  Constructor for the XMLConversionService.
	 *
	 * @param  cacheDir            The directory where converted files will be
	 *      cached and stored for later retrieval by ID.
	 * @param  filterDeclarations  Set to true to filter out the XML and DTD
	 *      declarations in the converted XML.
	 * @exception  IOException     If the directory is not valid or does not have
	 *      read/write access.
	 */
	public XMLConversionService(File cacheDir, boolean filterDeclarations)
		 throws IOException {
		if (cacheDir == null ||
			!cacheDir.isDirectory() ||
			!cacheDir.canRead() ||
			!cacheDir.canWrite())
			throw new IOException("XMLConversionService cache directory " +
				"is not valid or does not have read/write access.");
		this.cacheDir = cacheDir;
		this.filter = filterDeclarations;
	}


	/**
	 *  Converts XML from one format to another, saving and retrieving the
	 *  converted content to and from a file cache. Returns null if there are no
	 *  known conversions available for the requested conversion, or if a
	 *  processing error occurs. The cache file is automatically updated if the
	 *  modification time of the input XML file is newer than the cache. Characters
	 *  returned in the StringBuffer are encoded in UTF-8.
	 *
	 * @param  fromFormat       The XML format to convert from. Example:
	 *      'dlese_ims.'
	 * @param  toFormat         The format to convert to. Example: 'adn.'
	 * @param  originalXMLFile  The original XML file, in the 'from' format.
	 * @return                  Content converted to the 'to' format, or null if
	 *      unable to process.
	 */
	public StringBuffer getConvertedXml(String fromFormat,
	                                    String toFormat,
	                                    File originalXMLFile) {
		return getConvertedXml(fromFormat, toFormat, originalXMLFile, null);
	}


	/**
	 *  Converts XML from one format to another, without the use of a cache to
	 *  store or save results. Returns null if there are no known conversions
	 *  available for the requested conversion, or if a processing error occurs.
	 *  Characters returned in the StringBuffer are encoded in UTF-8.
	 *
	 * @param  fromFormat   The XML format to convert from. Example: 'dlese_ims.'
	 * @param  toFormat     The format to convert to. Example: 'adn.'
	 * @param  originalXML  The original XML as a String, in the 'from' format.
	 *      Should not be null.
	 * @return              Content converted to the 'to' format, or null if unable
	 *      to process.
	 */
	public String convertXml(String fromFormat,
	                               String toFormat,
	                               String originalXML) {
		// Check to see if the input and output formats are the same.
		if (fromFormat.equals(toFormat)) {
			if (originalXML != null) {
				try {
					if (filter)
						return stripXmlDeclaration(new BufferedReader(
							new StringReader(originalXML))).toString();
					else
						return originalXML;
				} catch (Exception e) {
					prtlnErr("Unable to process originalXML file: " + e);
					return null;
				}
			}
		}

		// Grab the converter, checking to see if we can convert.
		XmlConverter xmlConverter = (XmlConverter) getConverter(fromFormat, toFormat);
		if (xmlConverter == null)
			return null;

		return xmlConverter.convertXml(originalXML);
	}


	/**
	 *  Converts XML from one format to another, saving and retrieving the
	 *  converted content to and from a file cache. Returns null if there are no
	 *  known conversions available for the requested conversion, or if a
	 *  processing error occurs. The cache file is automatically updated if the
	 *  modification time of the input XML file is newer than the cache. Characters
	 *  returned in the StringBuffer are encoded in UTF-8.
	 *
	 * @param  fromFormat       The XML format to convert from. Example:
	 *      'dlese_ims.'
	 * @param  toFormat         The format to convert to. Example: 'adn.'
	 * @param  originalXMLFile  The original XML file, in the 'from' format. Should
	 *      not be null.
	 * @param  luceneDoc        A Lucene {@link org.apache.lucene.document.Document}
	 *      that holds content that may be used by the conversion class, or null.
	 * @return                  Content converted to the 'to' format, or null if
	 *      unable to process.
	 */
	public StringBuffer getConvertedXml(String fromFormat,
	                                    String toFormat,
	                                    File originalXMLFile,
	                                    XMLDocReader luceneDoc) {
		//prtln("getConvertedXml(): fromFormat: " + fromFormat + " toFormat: " + toFormat + " originalXMLFile: " +
		//	originalXMLFile.getAbsolutePath());

		// Check to see if the input and output formats are the same.
		if (fromFormat.equals(toFormat)) {
			if (originalXMLFile != null && originalXMLFile.canRead()) {
				try {
					if (filter)
						return stripXmlDeclaration(new BufferedReader(
							new InputStreamReader(new FileInputStream(originalXMLFile), "UTF-8")));
					else
						return Files.readFileToEncoding(originalXMLFile, "UTF-8");
				} catch (Exception e) {
					prtlnErr("Unable to read file: " + e);
					return null;
				}
			}
			else if (luceneDoc != null) {
				if (filter)
					return new StringBuffer(luceneDoc.getXmlStripped());
				else
					return new StringBuffer(luceneDoc.getXml());
			}
			else
				return null;
		}

		// Grab the converter, checking to see if we can convert.
		XmlConverter xmlConverter = (XmlConverter) getConverter(fromFormat, toFormat);
		if (xmlConverter == null)
			return null;

		// If the cached file is up-to-date, return it.
		File cachedFile = new File(cacheDir.getAbsolutePath() + "/" +
			toFormat + "/" +
			Files.escapeWindowsPath(originalXMLFile.getAbsolutePath()));
		if (cachedFile.isFile() && cachedFile.lastModified() > originalXMLFile.lastModified() &&
			cachedFile.lastModified() > xmlConverter.lastModified()) {
			try {
				StringBuffer cachedFileAsBuffer = Files.readFileToEncoding(cachedFile, "UTF-8");
				if (cachedFileAsBuffer.length() > 0)
					return cachedFileAsBuffer;
			} catch (IOException e) {
				prtlnErr("Could not read cached XML file (1): " + e);
			}
		}

		// Delete if cached file needs updating.
		cachedFile.delete();

		cachedFile.getParentFile().mkdirs();
		xmlConverter.writeConvertedFile(originalXMLFile, luceneDoc, cachedFile);

		// Return the transformed content and save in cache.
		try {
			if (filter)
				return stripXmlDeclaration(new BufferedReader(
					new InputStreamReader(new FileInputStream(cachedFile), "UTF-8")));
			else
				return Files.readFileToEncoding(cachedFile, "UTF-8");
		} catch (IOException e) {
			prtlnErr("Could not read cached XML file (2): " + e);
		}

		return null;
	}


	/**
	 *  Determines whether this XMLConversionService can perform the given
	 *  converstion. Returns true of the toFormat is the same as the fromFormat.
	 *
	 * @param  fromFormat  The format to convert from.
	 * @param  toFormat    The format to convert to.
	 * @return             True if this XMLConversionService can perform the
	 *      conversion.
	 */
	public boolean canConvert(String fromFormat, String toFormat) {
		//prtln("canConvert(): fromFormat '" + fromFormat + "' toFormat '" + toFormat + "'");
		if (fromFormat.equals(toFormat))
			return true;
		Hashtable formatConverters = (Hashtable) converters.get(fromFormat);
		if (formatConverters == null)
			return false;
		return (formatConverters.containsKey(toFormat));
	}


	/**
	 *  Gets the metadata format conversions that are available for the given
	 *  format, including the given format.
	 *
	 * @param  fromFormat  The format to convert from.
	 * @return             The availableFormats.
	 */
	public ArrayList getAvailableFormats(String fromFormat) {
		ArrayList formats = new ArrayList();
		formats.add(fromFormat);
		Hashtable formatConverters = (Hashtable) converters.get(fromFormat);
		if (formatConverters == null)
			return formats;
		Enumeration keys = formatConverters.keys();
		while (keys.hasMoreElements())
			formats.add(keys.nextElement());
		return formats;
	}


	/**
	 *  Gets the converter that is available to convert from one format to another.
	 *  Returns null if no converter is available.
	 *
	 * @param  fromFormat  The format to convert from.
	 * @param  toFormat    The format to convert to.
	 * @return             The converter.
	 */
	private Object getConverter(String fromFormat, String toFormat) {
		try {
			Hashtable formatConverters = (Hashtable) converters.get(fromFormat);
			return formatConverters.get(toFormat);
		} catch (Exception e) {
			return null;
		}
	}


	/**
	 *  Adds a XSL stylesheet that can convert from one XML format to another.
	 *
	 * @param  fromFormat   The XML format from which the stylesheet will convert.
	 * @param  toFormat     The XML format to which the stylesheet will convert.
	 * @param  xslFilePath  The absolute path to the stylesheet.
	 */
	public void addXslStylesheet(String fromFormat, String toFormat, String xslFilePath) {
		//fromFormat = fromFormat;
		//toFormat = toFormat;

		Hashtable formatConverters = (Hashtable) converters.get(fromFormat);
		if (formatConverters == null)
			formatConverters = new Hashtable();
		formatConverters.put(toFormat, new XslConverter(fromFormat, toFormat, xslFilePath));
		converters.put(fromFormat, formatConverters);
	}


	/**
	 *  Adds a concrete implementation of the {@link XMLFormatConverter} interface
	 *  that can convert XML from one format to another. The class must be
	 *  available in the classpath for Tomcat or the running JVM that is using this
	 *  broker.
	 *
	 * @param  fromFormat      The XML format from which the class will convert.
	 * @param  toFormat        The XML format to which the class will convert.
	 * @param  className       The fully-qualified class that will perform the
	 *      conversion.
	 * @param  servletContext  The feature to be added to the JavaConverterClass
	 *      attribute
	 */
	public void addJavaConverterClass(
	                                  String fromFormat,
	                                  String toFormat,
	                                  String className,
	                                  ServletContext servletContext) {
		Hashtable formatConverters = (Hashtable) converters.get(fromFormat);
		if (formatConverters == null)
			formatConverters = new Hashtable();
		formatConverters.put(toFormat, new JavaXmlConverter(fromFormat, toFormat, className, servletContext));
		converters.put(fromFormat, formatConverters);
	}



	/**
	 *  Adds an XSL converter by parsing a String of the form [ fromFormat |
	 *  toFormat | xslFileName ]. Useful for using initParameters to configure XML
	 *  format converters.
	 *
	 * @param  paramVal              A String of the form [ fromFormat | toFormat |
	 *      xslFileName ].
	 * @param  xslFilesDirecoryPath  The full path to the directory where the xsl
	 *      file exists.
	 * @return                       False if the number of values parsed in
	 *      paramVal is incorrect, otherwise true.
	 */
	public boolean addXslConverterHelper(String paramVal, File xslFilesDirecoryPath) {
		String[] vals = paramVal.split("\\|");
		if (vals.length != 3) {
			prtlnErr("addXslConverter() error: could not parse parameter '" + paramVal + "'");
			return false;
		}

		addXslStylesheet(vals[1], vals[2], xslFilesDirecoryPath.getAbsolutePath() + File.separator + vals[0]);
		return true;
	}


	/**
	 *  Adds an Java class XML converter by parsing a String of the form [
	 *  fromFormat | toFormat | className ]. Useful for using initParameters to
	 *  configure XML format converters.
	 *
	 * @param  paramVal        A String of the form [ fromFormat | toFormat |
	 *      xslFileName ].
	 * @param  servletContext  The ServletContext that will be available to the
	 *      class when it executes.
	 * @return                 False if the number of values parsed in paramVal is
	 *      incorrect, otherwise true.
	 */
	public boolean addJavaConverterHelper(String paramVal, ServletContext servletContext) {

		String[] vals = paramVal.split("\\|");
		if (vals.length != 3) {
			prtlnErr("addJavaConverter() error: could not parse parameter '" + paramVal + "'");
			return false;
		}

		addJavaConverterClass(vals[1], vals[2], vals[0], servletContext);
		return true;
	}


	/**
	 *  Factory for use in Servlets to create an XMLConversionService using
	 *  configurations found in the Servlet context parameters. This method also
	 *  places the path to the XSL files directory in the servlet context under
	 *  'xslFilesDirecoryPath', for use by the JavaXmlConverters. <p>
	 *
	 *  To configure a format converter that uses an XSL style sheet or Java class,
	 *  the context param-name must begin with the either 'xslconverter' or
	 *  'javaconverter' and must be unique. The param-value must be of the form
	 *  'xslfile.xsl|[from format]\[to format]' or '[fully qualified Java
	 *  class]|[from format]\[to format]'.
	 *
	 * @param  servletContext      The ServletContext
	 * @param  xslFilesDirecory    The base directory where the XSL files are
	 *      located
	 * @param  xmlCachDirecory     The directory where the service will cache the
	 *      converted XML files
	 * @param  filterDeclarations  Set to true to filter out the XML and DTD
	 *      declarations in the converted XML
	 * @return                     The XMLConversionService
	 * @exception  Exception       If error
	 */
	public static XMLConversionService xmlConversionServiceFactoryForServlets(
	                                                                          ServletContext servletContext,
	                                                                          File xslFilesDirecory,
	                                                                          File xmlCachDirecory,
	                                                                          boolean filterDeclarations)
		 throws Exception {

		// Set up an XML Conversion Service:

		// Place the xsl dir path in the Servlet context for use by the JavaXmlConverters
		servletContext.setAttribute("xslFilesDirecoryPath", xslFilesDirecory.getAbsolutePath());
		XMLConversionService xmlConversionService = null;
		xmlCachDirecory.mkdir();

		xmlConversionService =
			new XMLConversionService(xmlCachDirecory, filterDeclarations);

		// Add configured converters:
		Enumeration enumeration = servletContext.getInitParameterNames();
		String param;
		while (enumeration.hasMoreElements()) {
			param = (String) enumeration.nextElement();
			if (param.toLowerCase().startsWith("xslconverter")) {
				xmlConversionService.addXslConverterHelper(servletContext.getInitParameter(param), xslFilesDirecory);
			}
			if (param.toLowerCase().startsWith("javaconverter")) {
				xmlConversionService.addJavaConverterHelper(servletContext.getInitParameter(param), servletContext);
			}
		}
		return xmlConversionService;
	}



	/**
	 *  Strips the XML declaration and DTD declaration from the given XML. The resulting content is sutable for
	 *  insertion inside an existing XML element.
	 *
	 * @param  rdr              A BufferedReader containing XML.
	 * @return                  Content with the XML and DTD declarations stipped out.
	 * @exception  IOException  If error
	 */
	public static final StringBuffer stripXmlDeclaration(BufferedReader rdr)
		 throws IOException {
		StringBuffer out = new StringBuffer("");
		String tmp = "";

		// Check the first few lines in the file for the xml encoding string and
		// the DLESE dtd declaration and handle appropriately
		for (int i = 0; i < 4 && rdr.ready(); i++) {
			tmp = rdr.readLine();
			if (tmp == null)
				break;
			// Remove the encoding attribute, if present
			// <?xml version="1.0" encoding="UTF-8"?>.
			if (tmp.toLowerCase().indexOf("<?xml") != -1) {
				// Leave it out
			}
			// Handle special case of dlese_ims version 2.1 dtd format
			// (dtd declaration needs to be commented out):
			//<!DOCTYPE record SYSTEM "http://www.dlese.org/catalog/DT... (etc.)
			else if (tmp.toUpperCase().indexOf("<!DOCTYPE") != -1 &&
				tmp.toUpperCase().indexOf("DLESE.ORG") != -1 &&
				tmp.toUpperCase().indexOf(".DTD") != -1) {
				out.append("<!--");
				out.append(tmp);
				out.append("-->\n");
			}
			else if (tmp != null)
				out.append(tmp + "\n");
		}

		// Output the remainder file.
		tmp = "";
		while (rdr.ready() && tmp != null) {
			tmp = rdr.readLine();
			if (tmp != null)
				out.append(tmp + "\n");
		}

		rdr.close();

		return out;
	}


	/**
	 *  Gets the content from XML by stripping all XML tags. The input XML should
	 *  be valid prior to calling this method.
	 *
	 * @param  input  A valid XML string.
	 * @return        The contentFromXML.
	 */
	public final static String getContentFromXML(String input) {
		if (input == null)
			return "";

		StringBuffer bufa = new StringBuffer();
		char c1;
		boolean outputOn = true;
		for (int i = 0; i < input.length(); i++) {
			c1 = input.charAt(i);

			if (c1 == '<')
				outputOn = false;

			if (outputOn)
				bufa.append(c1);

			if (c1 == '>') {
				outputOn = true;
				bufa.append(' ');
			}
		}

		return bufa.toString();
	}


	private interface XmlConverter {
		/**
		 *  Converts XML from one format to another.
		 *
		 * @param  originalXMLFile  The original XML file.
		 * @param  luceneDoc        A lucene Document for this item.
		 * @param  fileToWrite      The file that must be written to with the
		 *      converted output.
		 * @return                  True if successful, else false.
		 */
		public boolean writeConvertedFile(File originalXMLFile, XMLDocReader luceneDoc, File fileToWrite);


		/**
		 *  Converts XML as string from one format to another.
		 *
		 * @param  originalXML  the original XML String.
		 * @return              converted XML, or null if unsuccessful
		 */
		public String convertXml(String originalXML);


		/**
		 *  Gets the time this converter code was last modified. If unknown, this
		 *  method should return -1 to indicate updates should occur only when the
		 *  source XML has change or System.currentTimeMillis() + 100000 to indicate
		 *  updates should occur upon every request for a conversion.
		 *
		 * @return    The time this converter code was last modified.
		 */
		public long lastModified();
	}


	private class XslConverter implements XmlConverter {
		private String toFormat = null, fromFormat = null, xslFilePath = null;
		private Transformer transformer = null;
		private long xslLastModified = -1;


		/**
		 *  Constructor for the XslConverter object
		 *
		 * @param  fromFormat   Format to convert from
		 * @param  toFormat     Format to convert to
		 * @param  xslFilePath  Absolute path to the XSL stylsheet that will be used.
		 */
		public XslConverter(String fromFormat, String toFormat, String xslFilePath) {
			this.toFormat = toFormat;
			this.fromFormat = fromFormat;
			this.xslFilePath = xslFilePath;
		}


		/**
		 *  Gets the time this converter code was last modified. If unknown, this
		 *  method should return -1.
		 *
		 * @return    The time this converter code was last modified.
		 */
		public long lastModified() {
			if (xslFilePath == null)
				return -1;
			return new File(xslFilePath).lastModified();
		}


		/**
		 *  Convert the XML from the given file, saving it to the output file
		 *  indicated.
		 *
		 * @param  originalXMLFile  The original XML file.
		 * @param  fileToWrite      File where converted content will be written.
		 * @param  luceneDoc        The Lucene Document for this item.
		 * @return                  True if successful.
		 */
		public boolean writeConvertedFile(File originalXMLFile, XMLDocReader luceneDoc, File fileToWrite) {
			//prtln("XslConverter.writeConvertedFile()");
			try {
				if (transformer == null || lastModified() != xslLastModified) {
					transformer = XSLTransformer.getTransformer(xslFilePath);
					xslLastModified = lastModified();
				}

				String content = null;
				if (originalXMLFile != null & originalXMLFile.canRead()) {
					synchronized (transformer) {
						content = XSLTransformer.transformFile(originalXMLFile, transformer);
					}
				}
				else if (luceneDoc != null) {
					synchronized (transformer) {
						content = XSLTransformer.transformString(luceneDoc.getXml(), transformer);
					}
				}

				// Make sure the transformed content is not empty:
				if (content == null || content.trim().length() == 0) {
					String m = "empty";
					if (content == null)
						m = "null";
					prtlnErr("XslConverter was unable to produce transformed file from format '"+fromFormat+"' to format '"+toFormat+"': The transformed content was " + m);
					return false;
				}
				// Write the transformed content to file
				else {
					if (filter) {
						StringBuffer stripped =
							stripXmlDeclaration(new BufferedReader(new StringReader(content)));
						Files.writeFile(stripped, fileToWrite);
					}
					else
						Files.writeFile(content, fileToWrite);
				}

			} catch (Throwable e) {
				prtlnErr("XslConverter was unable to produce transformed file from format '"+fromFormat+"' to format '"+toFormat+"': " + e);
				return false;
			}
			return true;
		}
		
		/**
		 *  Convert the XML from the XML string.
		 *
		 * @param  originalXML    The original XML string.
		 * @return                  convertedXML, or null if not successful.
		 */
		public String convertXml(String originalXML) {
			//prtln("XslConverter.convertXml()");
			String convertedXML = null;
			try {
				if (transformer == null || lastModified() != xslLastModified) {
					transformer = XSLTransformer.getTransformer(xslFilePath);
					xslLastModified = lastModified();
				}

				synchronized (transformer) {
					convertedXML = XSLTransformer.transformString(originalXML, transformer);
				}

				// Make sure the transformed content is not empty:
				if (convertedXML == null || convertedXML.trim().length() == 0) {
					String m = "empty";
					if (convertedXML == null)
						m = "null";
					prtlnErr("XslConverter was unable to produce transformed XML from format '"+fromFormat+"' to format '"+toFormat+"': The transformed content was " + m);
					return null;
				}

			} catch (Throwable e) {
				prtlnErr("XslConverter was unable to produce transformed XML from format '"+fromFormat+"' to format '"+toFormat+"': " + e);
				return null;
			}
			return convertedXML;
		}
		
	}


	private class JavaXmlConverter implements XmlConverter {
		private String toFormat = null, fromFormat = null, className = null;
		private XMLFormatConverter xmlFormatConverter = null;
		private XMLDocumentFormatConverter xmlDocumentFormatConverter = null;
		private ServletContext context = null;
		private boolean fatalError = false;


		/**
		 *  Constructor for the JavaXmlConverter object
		 *
		 * @param  fromFormat  DESCRIPTION
		 * @param  toFormat    DESCRIPTION
		 * @param  className   DESCRIPTION
		 * @param  context     DESCRIPTION
		 */
		public JavaXmlConverter(
		                        String fromFormat,
		                        String toFormat,
		                        String className,
		                        ServletContext context) {
			this.toFormat = toFormat;
			this.fromFormat = fromFormat;
			this.className = className;
			this.context = context;
		}


		/**
		 *  Gets the time this converter code was last modified. If unknown, this
		 *  method should return -1.
		 *
		 * @return    The time this converter code was last modified.
		 */
		public long lastModified() {
			if (!loadConverterClass())
				return -1;
			if (xmlFormatConverter != null)
				return xmlFormatConverter.lastModified(context);
			else if (xmlDocumentFormatConverter != null)
				return xmlDocumentFormatConverter.lastModified(context);
			else
				return -1;
		}


		/**
		 *  Writes the converted content to file.
		 *
		 * @param  originalXMLFile  The original file.
		 * @param  fileToWrite      The file to be written to.
		 * @param  luceneDoc        A Lucene Document that may conatin content.
		 * @return                  True if successful, else false.
		 */
		public boolean writeConvertedFile(File originalXMLFile, XMLDocReader luceneDoc, File fileToWrite) {
			//prtln("JavaXmlConverter.writeConvertedFile()");

			if (fatalError)
				return false;

			// Dynamically load the given XMLFormatConverter or XMLDocumentFormatConverterclass.
			if (!loadConverterClass())
				return false;

			try {
				String origContent = null;
				String content = null;
				if (originalXMLFile != null && originalXMLFile.canRead())
					origContent = Files.readFileToEncoding(originalXMLFile, "UTF-8").toString();
				else if (luceneDoc != null)
					origContent = luceneDoc.getXml();

				if (xmlFormatConverter != null)
					content = xmlFormatConverter.convertXML(origContent, context);
				else if (xmlDocumentFormatConverter != null)
					content = xmlDocumentFormatConverter.convertXML(origContent, luceneDoc, context);
				else
					return false;

				// Make sure the transformed content is not empty:
				if (content == null || content.trim().length() == 0) {
					String m = "empty";
					if (content == null)
						m = "null";
					prtlnErr("JavaXmlConverter was unable to produce transformed file: The transformed content was " + m);
					return false;
				}
				// Write the transformed content to file
				else {
					if (filter) {
						StringBuffer stripped =
							stripXmlDeclaration(new BufferedReader(new StringReader(content)));
						Files.writeFile(stripped, fileToWrite);
					}
					else
						Files.writeFile(content, fileToWrite);
				}

			} catch (Throwable e) {
				prtlnErr("JavaXmlConverter was unable to produce transformed file: " + e);
				//e.printStackTrace();
				return false;
			}
			return true;
		}

		/**
		 *  Converts originalXML from one format to another.
		 *
		 * @param  originalXMLFile  The original XML string.
		 * @return                  Converted XML string if successful, else null.
		 */
		public String convertXml(String originalXML) {
			// prtln("JavaXmlConverter.convertXml()");

			if (fatalError)
				return null;

			// Dynamically load the given XMLFormatConverter or XMLDocumentFormatConverterclass.
			if (!loadConverterClass())
				return null;
			
			String convertedXML = null;
			try {

				if (xmlFormatConverter != null)
					convertedXML = xmlFormatConverter.convertXML(originalXML, context);
				else if (xmlDocumentFormatConverter != null)
					convertedXML = xmlDocumentFormatConverter.convertXML(originalXML, null, context);
				else
					return null;

				// Make sure the transformed content is not empty:
				if (convertedXML == null || convertedXML.trim().length() == 0) {
					String m = "empty";
					if (convertedXML == null)
						m = "null";
					prtlnErr("JavaXmlConverter was unable to produce transformed XML: The transformed content was " + m);
					return null;
				}
				
			} catch (Throwable e) {
				prtlnErr("JavaXmlConverter was unable to produce transformed file: " + e);
				//e.printStackTrace();
				return null;
			}
			return convertedXML;
		}


		/**
		 *  NOT YET DOCUMENTED
		 *
		 * @return    NOT YET DOCUMENTED
		 */
		private boolean loadConverterClass() {
			// Dynamically load the given XMLFormatConverter or XMLDocumentFormatConverterclass.
			if (xmlFormatConverter == null && xmlDocumentFormatConverter == null) {
				try {
					Class xmlConverter = Class.forName(className);
					Object o = xmlConverter.newInstance();
					if (o instanceof XMLFormatConverter)
						xmlFormatConverter = (XMLFormatConverter) o;
					else if (o instanceof XMLDocumentFormatConverter)
						xmlDocumentFormatConverter = (XMLDocumentFormatConverter) o;
					else {
						prtlnErr("Error: Class '" + className +
							"' was not of type XMLFormatConverter or XMLDocumentFormatConverter.");
						fatalError = true;
						return false;
					}
				} catch (Throwable e) {
					prtlnErr("Error loading XMLFormatConverter or XMLDocumentFormatConverter class '" + className + "'. " + e);
					fatalError = true;
					return false;
				}
			}
			return true;
		}

	}


	// ----------------------------------------------------------------------

	/**
	 *  Gets a datestamp of the current time formatted for display with logs and
	 *  output.
	 *
	 * @return    A datestamp for display purposes.
	 */
	public final static String getDateStamp() {
		return
			new SimpleDateFormat("MMM d, yyyy h:mm:ss a zzz").format(new Date());
	}


	/**
	 *  Output a line of text to error out, with datestamp.
	 *
	 * @param  s  The text that will be output to error out.
	 */
	private final void prtlnErr(String s) {
		System.err.println(getDateStamp() + " XMLConversionService Error: " + s);
	}



	/**
	 *  Output a line of text to standard out, with datestamp, if debug is set to
	 *  true.
	 *
	 * @param  s  The String that will be output.
	 */
	private final void prtln(String s) {
		if (debug)
			System.out.println(getDateStamp() + " " + s);
	}


	/**
	 *  Sets the debug attribute of the SimpleLuceneIndex object
	 *
	 * @param  db  The new debug value
	 */
	public final static void setDebug(boolean db) {
		debug = db;
	}
}

