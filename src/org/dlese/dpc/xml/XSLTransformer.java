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

import java.io.*;
import java.util.*;
import java.text.*;

// Imported TraX classes
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerConfigurationException;

/**
 *  Transforms files or Strings using XSL stylesheets. In general, Transformer objects are not thread safe, so
 *  external synchornization should be applied in concurrent environments such as servlets. Note: some members
 *  of this class can be run as a stand-alone class that can be invoked from the command line or from a
 *  servlet environment.
 *
 * @author     John Weatherley
 * @version    $Id: XSLTransformer.java,v 1.20 2009/03/20 23:34:01 jweather Exp $
 */
public class XSLTransformer {

	private static boolean debug = true;

	/**  Stores the transform report */
	private StringBuffer buff = new StringBuffer();

	private int numRecordsProcessed = 0;
	private int numRecordsSuccess = 0;
	private int numRecordsUnchanged = 0;
	private int numRecordsError = 0;


	//================================================================

	/**  Constructor does nothing. All processing done in the run() or static stand-alone methods. */
	public XSLTransformer() { }


	/**
	 *  Perform the transform on each file in the inputFilesDir, using the given xsl stylesheet, placing the
	 *  resulting transformed files into outpuFilesDir.
	 *
	 * @param  xslFilePath     Path to an XSL stylesheet.
	 * @param  inputFilesDir   Path to a directory of XML files.
	 * @param  outputFilesDir  Path to a directory where transformed Files will be saved.
	 * @return                 A StringBuffer containing a descriptive report about the trasform.
	 * @exception  Exception   If error.
	 */
	public StringBuffer transform(String xslFilePath, String inputFilesDir, String outputFilesDir)
		 throws Exception {
		return transform(xslFilePath, inputFilesDir, null, outputFilesDir, null);
	}


	/**
	 *  Perform the transform files in inputFilesDir corresponding to the inputFileNames passed in, using the
	 *  given xsl stylesheet, placing the resulting transformed files into outpuFilesDir. If inputFileNames is
	 *  null, all .xml files found in the inputFilesDir are processed.
	 *
	 * @param  xslFilePath        Path to an XSL stylesheet.
	 * @param  inputFilesDir      Path to a directory of XML files.
	 * @param  inputFileNames     An array of file names or null to get the file names from the files found in
	 *      inputFilesDir
	 * @param  outputFilesDir     Path to a directory where transformed Files will be saved.
	 * @param  modifiedSinceDate  Indicates to perform the transform only if the original file has been modified
	 *      since the given date, or null to perform the transform regardless of modification date.
	 * @return                    A StringBuffer containing a descriptive report about the trasform.
	 * @exception  Exception      If error.
	 */
	public StringBuffer transform(String xslFilePath,
	                              String inputFilesDir,
	                              String[] inputFileNames,
	                              String outputFilesDir,
	                              Date modifiedSinceDate)
		 throws Exception {
		buff.append(new java.util.Date().toString() + " Starting transformations\n");

		File inputDir = new File(inputFilesDir);
		File outputDir = new File(outputFilesDir);

		// Create the output dir if it is not already there.
		mkdirs(outputFilesDir);

		// Use the static TransformerFactory.newInstance() method to instantiate
		// a TransformerFactory. The javax.xml.transform.TransformerFactory
		// system property setting determines the actual class to instantiate --
		// org.apache.xalan.transformer.TransformerImpl.
		TransformerFactory tFactory = getTransformerFactory ();

		// Use the TransformerFactory to instantiate a Transformer that will work with
		// the stylesheet you specify. This method call also processes the stylesheet
		// into a compiled Templates object.

		// Create reader

		File xslFile = new File(xslFilePath);
		StreamSource stream = new StreamSource(new FileInputStream(xslFile));

		// Set systemId so xsl style sheets can find relative style sheets referenced in include and import directives
		stream.setSystemId(xslFile);
		Transformer transformer = tFactory.newTransformer(stream);

		Date start = new Date();

		// If the input path is a directory, process all or files in the directory or given list of files
		// fron the directory
		if (inputDir.isDirectory()) {
			// If the input file is a not a direcrory, use it as the sole input file
			String[] inputFiles = {inputDir.getName()};
			inputFiles = (inputFileNames == null) ? inputDir.list(new XMLFileFilter()) : inputFileNames;

			buff.append(" ---- Input from directory: ----\n");
			buff.append(inputDir.getAbsolutePath() + "\n");
			buff.append(" ---- Output to directory: ----\n");
			buff.append(outputDir.getAbsolutePath() + "\n");
			buff.append(" ---- XSL stylesheet used: ----\n");
			buff.append(new File(xslFilePath).getAbsolutePath() + "\n");

			// Loop through each of the files:
			buff.append(" ---- List of any files with processing errors is shown below: ----\n");
			String outfile;
			String inputFilePath;
			for (int i = 0; i < inputFiles.length; i++) {
				numRecordsProcessed++;

				inputFilePath = inputFilesDir + "/" + inputFiles[i];
				//prtln("Infile = " + inputFilePath);

				// Only process if the input file is newer than changedSinceDate
				if (modifiedSinceDate != null) {
					File f = new File(inputFilePath);
					if (f.isFile() && (f.lastModified() < modifiedSinceDate.getTime())) {
						numRecordsUnchanged++;
						continue;
					}
				}

				outfile = outputFilesDir + "/" + encodeStringIntoHex(inputFiles[i]);
				//prtln("Outfile = " + outfile);

				try {
					transformToFile(inputFilePath, outfile, transformer);
					numRecordsSuccess++;
				} catch (Exception e) {
					buff.append("XSLT error on record "
						 + inputFiles[i]
						 + ": " + e.getMessage() + "\n");
					numRecordsError++;
				}
			}
		}
		// If just a single file, process...
		else {
			buff.append(" ---- Input file: ----\n");
			buff.append(inputDir.getAbsolutePath() + "\n");
			buff.append(" ---- Output to directory: ----\n");
			buff.append(outputDir.getAbsolutePath() + "\n");
			buff.append(" ---- XSL stylesheet used: ----\n");
			buff.append(new File(xslFilePath).getAbsolutePath() + "\n");

			try {
				numRecordsProcessed++;
				// Only process if the input file is newer than changedSinceDate
				if (modifiedSinceDate == null || (inputDir.lastModified() >= modifiedSinceDate.getTime())) {
					transformToFile(inputFilesDir,
						outputDir.getAbsolutePath() + "/" + inputDir.getName(),
						transformer);
					numRecordsSuccess++;
				}
				else
					numRecordsUnchanged++;

			} catch (Exception e) {
				buff.append("XSLT error: " + e.getMessage() + "\n");
				numRecordsError++;
			}
		}

		Date end = new Date();

		long ms = (end.getTime() - start.getTime()) % 1000;
		long sec1 = (long) Math.floor((end.getTime() - start.getTime()) / 1000);
		long min = (long) Math.floor(sec1 / 60);
		long sec = sec1 - 60 * min;
		long tms = end.getTime() - start.getTime();

		String timeMsg = min + " min " + sec + " sec and " + ms + " ms.\n";

		// Provide user with a summary.
		buff.append(" ---- Transform summary: ----\n");
		buff.append(numRecordsProcessed + " total records were processed in " + timeMsg);
		buff.append(numRecordsSuccess + " were transformed successfully.\n");
		buff.append(numRecordsError + " transformations had errors.\n");
		if (numRecordsUnchanged > 0)
			buff.append(numRecordsUnchanged + " were up-to-date and did not need to be transformed.\n");

		buff.append(new java.util.Date().toString() + " Transformations completed.\n");
		return buff;
	}


	//================================================================

	/**
	 *  Transform a single file to a file at the given output path using the given transformer.
	 *
	 * @param  inputFilePath   The input file to transform.
	 * @param  outputFilePath  The output file where transformed content will be saved.
	 * @param  transformer     The Transformer used to perform the transform.
	 * @exception  Exception   If unable to perform the transform.
	 */
	public final static void transformToFile(String inputFilePath, String outputFilePath, Transformer transformer)
		 throws Exception {
		transformToFile(new File(inputFilePath), new FileOutputStream(outputFilePath), transformer);
	}


	/**
	 *  Transform a single file to the given output file using the given transformer.
	 *
	 * @param  inputFile      The input file to transform.
	 * @param  outputFile     The output file where transformed content will be saved.
	 * @param  transformer    The Transformer used to perform the transform.
	 * @exception  Exception  If unable to perform the transform.
	 */
	public final static void transformToFile(File inputFile, File outputFile, Transformer transformer)
		 throws Exception {
		transformToFile(inputFile, new FileOutputStream(outputFile), transformer);
	}


	/**
	 *  Transform a single file to the given FileOutputStream using the given transformer.
	 *
	 * @param  inputFile      The input file to transform.
	 * @param  fos            The FileOutputStream where transformed content will be saved.
	 * @param  transformer    The Transformer used to perform the transform.
	 * @exception  Exception  If unable to perform the transform.
	 */
	public final static void transformToFile(File inputFile, FileOutputStream fos, Transformer transformer)
		 throws Exception {
		OutputStreamWriter writer = new OutputStreamWriter(fos, "UTF-8");
		transformer.transform(
		// Must use FileInputStream so that escaped filename chars like %3A work.
			new StreamSource(new FileInputStream(inputFile)),
			new StreamResult(writer)
			);
		fos.close();
		writer.close();
	}


	//================================================================


	/**
	 *  Transforms an XML file using a pre-compiled {@link javax.xml.transform.Transformer}. Use {@link
	 *  #getTransformer(String xslFilePath)} to produce a reusable {@link javax.xml.transform.Transformer} for a
	 *  given XSL stylesheet.
	 *
	 * @param  inputFilePath  The XML file to transform.
	 * @param  transformer    A pre-compiled {@link javax.xml.transform.Transformer} used to produce transformed
	 *      output.
	 * @return                A String containing the transformed content.
	 */
	public final static String transformFile(String inputFilePath, Transformer transformer) {
		try {
			StringWriter writer = new StringWriter();
			transformer.transform(new StreamSource(new FileInputStream(inputFilePath)), new StreamResult(writer));
			return writer.toString();
		} catch (Throwable e) {
			prtlnErr(e);
			return "";
		}
	}


	/**
	 *  Transforms an XML file using a pre-compiled {@link javax.xml.transform.Transformer}. Use {@link
	 *  #getTransformer(String xslFilePath)} to produce a reusable {@link javax.xml.transform.Transformer} for a
	 *  given XSL stylesheet.
	 *
	 * @param  inputFile    The XML file to transform.
	 * @param  transformer  A pre-compiled {@link javax.xml.transform.Transformer} used to produce transformed
	 *      output.
	 * @return              A String containing the transformed content.
	 */
	public final static String transformFile(File inputFile, Transformer transformer) {
		try {
			StringWriter writer = new StringWriter();
			StreamSource ss = new StreamSource(new FileInputStream(inputFile));
			ss.setSystemId(inputFile);
			transformer.transform(ss, new StreamResult(writer));
			return writer.toString();
		} catch (Throwable e) {
			prtlnErr(e);
			return "";
		}
	}


	/**
	 *  Transforms an XML file using a pre-compiled {@link javax.xml.transform.Transformer}. Use {@link
	 *  #getTransformer(String xslFilePath)} to produce a reusable {@link javax.xml.transform.Transformer} for a
	 *  given XSL stylesheet. To convert the resulting StringWriter to a String, call StringWriter.toString().
	 *
	 * @param  inputFilePath  The XML file to transform.
	 * @param  transformer    A pre-compiled {@link javax.xml.transform.Transformer} used to produce transformed
	 *      output.
	 * @return                A StringWriter containing the transformed content.
	 */
	public final static StringWriter transformFileToWriter(String inputFilePath, Transformer transformer) {
		try {
			StringWriter writer = new StringWriter();
			transformer.transform(new StreamSource(new FileInputStream(inputFilePath)), new StreamResult(writer));
			return writer;
		} catch (Throwable e) {
			prtlnErr(e);
			return new StringWriter();
		}
	}


	/**
	 *  Transforms an XML String using a pre-compiled {@link javax.xml.transform.Transformer}. Use {@link
	 *  #getTransformer(String xslFilePath)} to produce a reusable {@link javax.xml.transform.Transformer} for a
	 *  given XSL stylesheet.
	 *
	 * @param  xmlString    The XML String to transform.
	 * @param  transformer  A pre-compiled {@link javax.xml.transform.Transformer} used to produce transformed
	 *      output.
	 * @return              A String containing the transformed content.
	 */
	public final static String transformString(String xmlString, Transformer transformer) {
		try {
			StringWriter writer = new StringWriter();
			StreamSource source = new StreamSource(new StringReader(xmlString));
			transformer.transform(source, new StreamResult(writer));
			return writer.toString();
		} catch (Throwable e) {
			prtlnErr(e);
			return "";
		}
	}


	/**
	 *  Transforms an XML String using a pre-compiled {@link javax.xml.transform.Transformer}. Use {@link
	 *  #getTransformer(String xslFilePath)} to produce a reusable {@link javax.xml.transform.Transformer} for a
	 *  given XSL stylesheet. To convert the resulting StringWriter to a String, call StringWriter.toString().
	 *
	 * @param  xmlString    The XML String to transform.
	 * @param  transformer  A pre-compiled {@link javax.xml.transform.Transformer} used to produce transformed
	 *      output.
	 * @return              A StringWriter containing the transformed content.
	 */
	public final static StringWriter transformStringToWriter(String xmlString, Transformer transformer) {
		try {
			StringWriter writer = new StringWriter();
			StreamSource source = new StreamSource(new StringReader(xmlString));
			transformer.transform(source, new StreamResult(writer));
			return writer;
		} catch (Throwable e) {
			prtlnErr(e);
			return new StringWriter();
		}
	}



	/**
	 *  Transforms an XML file using an XSL stylesheet.
	 *
	 * @param  inputFilePath  The XML file to transform.
	 * @param  xslFilePath    The XSL file used to perform the transform.
	 * @return                A String containing the transformed content.
	 */
	public final static String transformFile(String inputFilePath, String xslFilePath) {
		try {
			return transformFile(inputFilePath, getTransformer(xslFilePath));
		} catch (Throwable e) {
			prtlnErr(e);
			return "";
		}
	}


	/**
	 *  Transforms an XML file using an XSL stylesheet. To convert the resulting StringWriter to a String, call
	 *  StringWriter.toString().
	 *
	 * @param  inputFilePath  The XML file to transform.
	 * @param  xslFilePath    The XSL file used to perform the transform.
	 * @return                A StringWriter containing the transformed content.
	 */
	public final static StringWriter transformFileToWriter(String inputFilePath, String xslFilePath) {
		try {
			return transformFileToWriter(inputFilePath, getTransformer(xslFilePath));
		} catch (Throwable e) {
			prtlnErr(e);
			return new StringWriter();
		}
	}


	/**
	 *  Transforms an XML String using an XSL stylesheet.
	 *
	 * @param  xmlString    The XML String to transform.
	 * @param  xslFilePath  The XSL file used to perform the transform.
	 * @return              A String containing the transformed content.
	 */
	public final static String transformString(String xmlString, String xslFilePath) {
		try {
			return transformString(xmlString, getTransformer(xslFilePath));
		} catch (Throwable e) {
			prtlnErr(e);
			return "";
		}
	}


	/**
	 *  Transforms an XML String using an XSL stylesheet supplied as a String.
	 *
	 * @param  xmlString  The XML String to transform.
	 * @param  xslString  The XSL String used to define the transform.
	 * @return            A String containing the transformed content.
	 */
	public final static String transformStringUsingString(String xmlString, String xslString) {
		try {
			return transformString(xmlString, getTransformerFromXSLString(xslString));
		} catch (Throwable e) {
			prtlnErr(e);
			return "";
		}
	}


	/**
	 *  Removes all namespace information from XML.
	 *
	 * @param  xmlString  The XML String to strip of namespaces
	 * @return            A String containing XML without namespaces
	 * @see               #XSLUtils.getRemoveNamespacesXSL()
	 */
	public final static String localizeXml(String xmlString) {
		return transformStringUsingString(xmlString, XSLUtils.getRemoveNamespacesXSL());
	}


	/**
	 *  Transforms an XML String using an XSL stylesheet. To convert the resulting StringWriter to a String, call
	 *  StringWriter.toString().
	 *
	 * @param  xmlString    The XML String to transform.
	 * @param  xslFilePath  The XSL file used to perform the transform.
	 * @return              A StringWriter containing the transformed content.
	 */
	public final static StringWriter transformStringToWriter(String xmlString, String xslFilePath) {
		try {
			return transformStringToWriter(xmlString, getTransformer(xslFilePath));
		} catch (Throwable e) {
			prtlnErr(e);
			return new StringWriter();
		}
	}


	/**
	 *  Gets a {@link javax.xml.transform.Transformer} used to transform XML using
	 *  a given XSL stylesheet. For efficiency, one {@link javax.xml.transform.Transformer}
	 *  should be used to transform multiple XMLs from a single stylesheet.
	 *
	 * @param  xslFilePath                            A path to an XSL stylesheet
	 *      file.
	 * @return                                        A Transformer used to
	 *      transform XML using a given stylesheet.
	 * @exception  TransformerConfigurationException  If error.
	 * @exception  FileNotFoundException              If file can not be found.
	 */
	public static Transformer getTransformer(String xslFilePath)
		 throws TransformerConfigurationException, FileNotFoundException {

		return getTransformer(xslFilePath, null);
	}


	/**
	 *  Gets a specific {@link javax.xml.transform.Transformer}, used to transform
	 *  XML using a given XSL stylesheet.
	 *
	 * @param  xslFilePath                            A path to an XSL stylesheet
	 *      file.
	 * @param  transformerFactoryClass                TransformerFactory class
	 *      (e.g. "net.sf.saxon.TransformerFactoryImpl")
	 * @return                                        A Transformer used to
	 *      transform XML using a given stylesheet.
	 * @exception  TransformerConfigurationException  If error.
	 * @exception  FileNotFoundException              If file can not be found.
	 */
	public static Transformer getTransformer(String xslFilePath, String transformerFactoryClass)
		 throws TransformerConfigurationException, FileNotFoundException {

		TransformerFactory tFactory = getTransformerFactory(transformerFactoryClass);

		File xslFile = new File(xslFilePath);
		StreamSource stream = new StreamSource(new FileInputStream(xslFile));

		// Set systemId so xsl style sheets can find relative style sheets referenced in include and import directives
		stream.setSystemId(xslFile);
		return tFactory.newTransformer(stream);
	}

	/**
	 *  Gets a {@link javax.xml.transform.Transformer} used to transform XML using a given XSL stylesheet. For
	 *  efficiency, one {@link javax.xml.transform.Transformer} should be used to transform multiple XMLs from a
	 *  single stylesheet.
	 *
	 * @param  xslString                              XSL supplied as a String
	 * @return                                        A Transformer used to transform XML using a given
	 *      stylesheet.
	 * @exception  TransformerConfigurationException  If error.
	 */
	public final static Transformer getTransformerFromXSLString(String xslString)
		 throws TransformerConfigurationException {
		TransformerFactory tFactory = getTransformerFactory ();

		StreamSource stream = new StreamSource(new StringReader(xslString));

		return tFactory.newTransformer(stream);
	}

	private static TransformerFactory getTransformerFactory () {
		return  getTransformerFactory (null);
	}
	
	/**
	 *  Gets a transformerFactory instance using a specified TransformerFactory class, or
	 *  the default TransformerFactory class if "tFactoryClass" in null.
	 *
	 * @param  tFactoryClass  TransformerFactory class (e.g. "net.sf.saxon.TransformerFactoryImpl")
	 * @return                The transformerFactory value
	 */
	private static synchronized TransformerFactory getTransformerFactory(String tFactoryClass) {
		TransformerFactory tFactory = null;
		String tFactoryPropertyName = "javax.xml.transform.TransformerFactory";
		if (tFactoryClass != null) {
			String originalFactoryClass = System.getProperty(tFactoryPropertyName);
			try {

				Class result = Class.forName(tFactoryClass);

				// If the TransformerFactory class is available, use it:
				if (result != null) {
					System.setProperty(tFactoryPropertyName, tFactoryClass);
					tFactory = TransformerFactory.newInstance();
					System.setProperty(tFactoryPropertyName, originalFactoryClass);
				}
			} catch (ClassNotFoundException e) {
				// If class not found, use default TransformerFactory system property, which works in Java 1.4
				prtln(tFactoryClass + " not found - using default");
			} finally {
				// Return system property to it's original value
				System.setProperty(tFactoryPropertyName, originalFactoryClass);
			}
		}

		if (tFactory == null)
			tFactory = TransformerFactory.newInstance();

		// prtln("TransformerFactory returning " + tFactory.getClass().getName());
		// prtln("\t  Sys.prop: " + System.getProperty(tFactoryPropertyName));

		return tFactory;
	}

	

	//================================================================

	/**
	 *  Create a directory and all necessary sub-directories for the given path. If the directory allready exists
	 *  then nothing is done.
	 *
	 * @param  path           DESCRIPTION
	 * @exception  Exception  DESCRIPTION
	 */
	private void mkdirs(String path)
		 throws Exception {
		String errorMsg = null;
		File f = new File(path);

		if (f.isDirectory())
			return;

		else if (f.isFile()) {
			errorMsg = "Error creating directory "
				 + path
				 + ": Path leads to a file, not a directory.";
			throw new Exception(errorMsg);
		}
		else {
			try {
				if (!f.mkdirs()) {
					errorMsg = "Error creating directory " + path;
					throw new Exception(errorMsg);
				}
			} catch (Exception e) {
				errorMsg = "Error creating directory \""
					 + path + ": " + e.getMessage();
				throw new Exception(errorMsg);
			}
		}
	}


	//================================================================

	/**
	 *  Substitutes hex values for all characters EXCEPT digits, letters, and the chars shown below. Output is of
	 *  the form %HEX.<p>
	 *
	 *  Excepted chars include digits, letters and: - _ . %<p>
	 *
	 *  Note: Unix commands cannot include the following chars: * ? ! | \ / ' " { } < > ; , ^ ( ) $ ~ Windows
	 *  file names may not contain: \ / : * ? " < > |
	 *
	 * @param  stg  A String to encode.
	 * @return      An encoded String.
	 */
	public static String encodeStringIntoHex(String stg) {
		// Since the xslt (xalan) code has trouble outputting file names
		// with hex encoding, we're using other encoding instead.
		if (true)
			return encodeCharsInString(stg);

		int ii;
		StringBuffer outbuf = new StringBuffer();
		for (ii = 0; ii < stg.length(); ii++) {
			char chr = stg.charAt(ii);
			if (Character.isLetterOrDigit(chr) ||
			//chr == ':' || // unfortunatly colons don't work in Windows
				chr == '-' ||
				chr == '_' ||
				chr == '%' ||
				chr == '.')
				outbuf.append(chr);
			else
				outbuf.append("%" + Integer.toHexString(chr).toUpperCase());
		}
		return outbuf.toString();
	}


	//================================================================

	/**
	 *  Substitutes escape chars for certain sensitive characthers that don't play well in file names or file
	 *  paths. Note:<p>
	 *
	 *  Unix commands cannot include the following chars: * ? ! | \ / ' " { } < > ; , ^ ( ) $ ~ <p>
	 *
	 *  Windows file names may not contain: \ / : * ? " < > |
	 *
	 * @param  stg  A String to encode.
	 * @return      An encoded String.
	 */
	public static synchronized String encodeCharsInString(String stg) {
		int ii;
		StringBuffer outbuf = new StringBuffer();
		for (ii = 0; ii < stg.length(); ii++) {
			char chr = stg.charAt(ii);
			if (chr == ':')
				outbuf.append("-COLON-");
			else
				outbuf.append(chr);
		}
		return outbuf.toString();
	}



	//================================================================

	/**
	 *  Filter .xml files only
	 *
	 * @author     John Weatherley
	 * @version    $Id: XSLTransformer.java,v 1.20 2009/03/20 23:34:01 jweather Exp $
	 */
	class XMLFileFilter implements FilenameFilter {
		/**
		 *  Filters files by the .xml suffix.
		 *
		 * @param  dir       The directory containing potential XML files.
		 * @param  fileName  The file inquestion.
		 * @return           True if .xml file.
		 */
		public boolean accept(File dir, String fileName) {
			return fileName.toLowerCase().endsWith(".xml") && new File(dir.toString(), fileName).isFile();
		}
	}


	// ======= Util Methods ===================================


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
		System.err.println(getDateStamp() + " XSLTransformer ERROR: " + s);
	}


	/**
	 *  Output a line of text to error out, with datestamp.
	 *
	 * @param  s  The text that will be output to error out.
	 * @param  t  The Throwable to output with message
	 */
	protected final static void prtlnErr(String s, Throwable t) {
		System.err.println(getDateStamp() + " XSLTransformer ERROR: " + s + ": " + t);
		t.printStackTrace();
	}


	/**
	 *  Output a line of text to error out, with datestamp.
	 *
	 * @param  t  The Throwable to output with message
	 */
	protected final static void prtlnErr(Throwable t) {
		System.err.println(getDateStamp() + " XSLTransformer ERROR: " + t);
		t.printStackTrace();
	}


	/**
	 *  Output a line of text to standard out, with datestamp, if debug is set to true.
	 *
	 * @param  s  The String that will be output.
	 */
	protected static void prtln(String s) {
		if (debug) {
			System.out.println(getDateStamp() + " XSLTransformer: " + s);
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

