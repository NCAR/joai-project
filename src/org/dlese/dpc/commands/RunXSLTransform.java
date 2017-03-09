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
package org.dlese.dpc.commands;

import org.dlese.dpc.propertiesmgr.PropertiesManager;
import org.dlese.dpc.xml.XMLValidator;
import org.dlese.dpc.xml.XSLTransformer;
import org.dlese.dpc.oai.OAIUtils;

import java.io.*;
import java.util.*;
import java.text.*;

/**
 *  Command-line routine that performs XSL transforms of XML files or directories of
 *  files. Has option for validating records and will return a report of the validation as
 *  well as the outcome of the transform
 *
 * @author    John Weatherley
 */
public class RunXSLTransform {

	/**
	 *  Structure to hold parameters
	 *
	 * @author    John Weatherley
	 */
	private class TxfmProps {
		/**  DESCRIPTION */
		public String xslFile = "";
		/**  DESCRIPTION */
		public String outputDir = "";
		/**  DESCRIPTION */
		public String inputDir = "";
		/**  DESCRIPTION */
		public String modifiedSinceDate = "null";
		/**  DESCRIPTION */
		public boolean validateInputFiles = false;
		/**  DESCRIPTION */
		public boolean validateOutputFiles = false;
	}


	/**
	 *  The main program for the RunXSLTransform class
	 *
	 * @param  args  The command line arguments
	 */
	public static void main(
	                        String[] args) {
		try {
			String usage = "Usage: java RunXSLTransform propertiesFile.properties\n" +
				"         --- or --- \n" +
				"       java RunXSLTransform stylesheet.xsl input output_dir [ modifiedSinceDate [true|FALSE] [true|FALSE] ]\n" +
				"       where 'modifiedSinceDate' is a UTCdatetime string of the form YYYY-MM-DDThh:mm:ssZ or the string 'null'\n" +
				"       to use none.";

			RunXSLTransform tFormer = new RunXSLTransform();
			if (args.length == 0 || args.length > 6) {
				prtln(usage);
				System.exit(1);
			}

			// Read properties file
			else if (args.length == 1 && args[0].toLowerCase().endsWith(".properties")) {

				TxfmProps txfmProps;
				try {
					txfmProps = tFormer.getProps(args[0]);
				} catch (Exception e) {
					prtlnErr("Error: Unable to read properties file. Reason: " + e);
					return;
				}
				tFormer.processRecords(txfmProps.xslFile, txfmProps.inputDir, txfmProps.outputDir, parseDate(txfmProps.modifiedSinceDate), txfmProps.validateInputFiles, txfmProps.validateOutputFiles);
			}
			// Read input from command line
			else if (args.length == 3) {
				tFormer.processRecords(args[0], args[1], args[2], null, false, false);
			}
			else if (args.length == 4) {
				tFormer.processRecords(args[0], args[1], args[2], parseDate(args[3]), false, false);
			}
			else if (args.length == 5) {
				tFormer.processRecords(args[0], args[1], args[2], parseDate(args[3]), args[4].equalsIgnoreCase("true"), false);
			}
			else if (args.length == 6) {
				tFormer.processRecords(args[0], args[1], args[2], parseDate(args[3]), args[4].equalsIgnoreCase("true"), args[5].equalsIgnoreCase("true"));
			}
			else {
				prtln(usage);
				System.exit(1);
			}
		} catch (ParseException e) {
			prtlnErr("Error found in modifiedSinceDate. " + e.getMessage() +
				".\nParameter modifiedSinceDate must be 'null' or a UTCdatetime of the form YYYY-MM-DDThh:mm:ssZ .");
			System.exit(1);
		} catch (Exception e) {
			prtlnErr("Error: " + e);
			System.exit(1);
		}
	}


	private static Date parseDate(String dateString)
		 throws ParseException {
		if (dateString.equalsIgnoreCase("null"))
			return null;

		return OAIUtils.getDateFromDatestamp(dateString);
	}


	//================================================================

	/**
	 *  Process the given harvested records using the preferences in the given props file.
	 *
	 */
	private void processRecords(String xslFile,
	                            String inputDir,
	                            String outputDir,
	                            Date modifiedSinceDate,
	                            boolean validateInputFiles,
	                            boolean validateOutputFiles)
		 throws Exception {
		String reportFileName = null;// Print all message to std out
		String inputFilesDirFull = inputDir;
		String xslFileFull = xslFile;
		String outputDirFull = outputDir;

		StringBuffer reportTxt = null;

		// ********** Validate input files ************

		if (validateInputFiles == true) {
			prtln("Starting validation of input files...");

			reportTxt = null;
			try {
				XMLValidator validator = new XMLValidator();
				reportTxt = validator.validate(inputFilesDirFull);

				// Write report
				if (reportTxt != null)
					appendReport(reportFileName, reportTxt);

			} catch (Exception e) {
				prtln("Error during input file validation: " + e);
			}
		}

		// *********** Transform *************

		if (xslFile != null && !xslFile.equals("")) {
			prtln("Starting transform...");

			reportTxt = null;
			try {
				XSLTransformer transformer = new XSLTransformer();
				reportTxt = transformer.transform(xslFileFull, inputFilesDirFull, null, outputDirFull, modifiedSinceDate);

				// Write report
				if (reportTxt != null)
					appendReport(reportFileName, reportTxt);

			} catch (Throwable e) {
				prtln("Error during transform: " + e);
			}
		}

		// ********** Validate output files ************

		if (validateOutputFiles == true) {
			prtln("Starting validation of output files...");

			reportTxt = null;
			try {
				XMLValidator validator = new XMLValidator();
				reportTxt = validator.validate(outputDirFull);

				// Write report
				if (reportTxt != null)
					appendReport(reportFileName, reportTxt);

			} catch (Exception e) {
				prtln("Error during output file validation: " + e);
			}
		}
	}


	//================================================================

	/**
	 *  Append text to the given report file. If the file does not exist, it will be created.
	 *  If reportFilePath is null, then the ouput will be sent to standard out
	 */
	private void appendReport(String reportFilePath, String msg) {
		if (reportFilePath == null)
			System.out.print(msg + "\n\n");
		else {

			try {
				// Write validation report:
				FileWriter writer = new FileWriter(reportFilePath, true);
				writer.write(msg);
				writer.write("\n\n");
				writer.close();
			} catch (Exception e) {
				prtln("Error writing transform report file: " + e);
			}
		}
	}


	/**
	 *  Append text to the given report file. If the file does not exist, it will be created.
	 *  If reportFilePath is null, then the ouput will be sent to standard out
	 */
	private void appendReport(String reportFilePath, StringBuffer msg) {
		appendReport(reportFilePath, msg.toString());
	}


	//================================================================

	/** Read the properties file and set all internal variables accordingly */
	private TxfmProps getProps(String propsName)
		 throws Exception {
		PropertiesManager props = new PropertiesManager(propsName);

		TxfmProps txfmProps = new TxfmProps();
		txfmProps.xslFile = props.getProp("xslFile", "");
		txfmProps.outputDir = props.getProp("outputDir", "");
		txfmProps.inputDir = props.getProp("inputDir", "");
		txfmProps.modifiedSinceDate = props.getProp("modifiedSinceDate", "null");
		txfmProps.validateInputFiles = props.getPropAsBoolean("validateInputFiles", "false");
		txfmProps.validateOutputFiles = props.getPropAsBoolean("validateOutputFiles", "false");
		return txfmProps;
	}


	//================================================================

	private static void prtln(String s) {
		System.out.println(s);
	}


	private static void prtlnErr(String s) {
		System.err.println(s);
	}

	//================================================================
}
