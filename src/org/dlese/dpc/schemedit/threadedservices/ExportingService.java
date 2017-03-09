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
package org.dlese.dpc.schemedit.threadedservices;

import org.dlese.dpc.schemedit.*;
import org.dlese.dpc.schemedit.dcs.*;
import org.dlese.dpc.xml.schema.*;
import org.dlese.dpc.xml.*;
import org.dlese.dpc.xml.nldr.*;
import org.dlese.dpc.index.*;
import org.dlese.dpc.index.reader.*;

import org.dlese.dpc.util.*;
import org.dlese.dpc.datamgr.*;
import org.dlese.dpc.repository.*;

import javax.xml.transform.Transformer;
import javax.servlet.ServletContext;

import java.util.*;
import java.text.*;
import java.io.*;

/**
 *  Supports exporting of collections and portions of collections to disk.
 *  Exported records are stripped of cataloger information.
 *
 * @author    ostwald
 */
public class ExportingService extends ThreadedService {

	static boolean debug = true;

	private String exportBaseDir = null;
	private Transformer transformer = null;


	/**
	 *  Constructor for the ExportingService object
	 *
	 * @param  exportingServiceDataDir  Description of the Parameter
	 * @param  servletContext           NOT YET DOCUMENTED
	 * @param  exportBaseDir            NOT YET DOCUMENTED
	 * @param  xslPath                  NOT YET DOCUMENTED
	 */
	public ExportingService(
	                        ServletContext servletContext,
	                        String exportingServiceDataDir,
	                        String exportBaseDir,
	                        String xslPath) {
		// set up

		super(servletContext, exportingServiceDataDir);
		this.exportBaseDir = exportBaseDir;

		if (!(new File(exportBaseDir).exists())) {
			prtln("WARNING: exportBaseDir doe not exist at " + exportBaseDir);
		}
		try {
			transformer = XSLTransformer.getTransformer(xslPath);
		} catch (Throwable e) {
			prtln("transformer WARNING: " + e.getMessage());
			prtln("xslPath: " + xslPath);
		}
	}


	/**
	 *  Gets the exportingSetInfo attribute of the ExportingService object
	 *
	 * @return    The exportingSetInfo value
	 */
	public DcsSetInfo getExportingSetInfo() {
		if (isProcessing && dcsSetInfo != null)
			return dcsSetInfo;
		else
			return null;
	}


	/**
	 *  Gets the exportBaseDir attribute of the ExportingService object
	 *
	 * @return    The exportBaseDir value
	 */
	public String getExportBaseDir() {
		return exportBaseDir;
	}


	/**
	 *  Gets the legalExportDest attribute of the ExportingService object
	 *
	 * @param  destDir  NOT YET DOCUMENTED
	 * @return          The legalExportDest value
	 */
	public boolean isLegalExportDest(File destDir) {
		File exportRoot = new File(exportBaseDir);

		try {
			// prtln ("\t" + "destDir.getCanonicalPath(): " +  destDir.getCanonicalPath());
			// prtln ("\t" + "exportRoot.getCanonicalPath(): " + exportRoot.getCanonicalPath());
			return (destDir.getCanonicalPath().startsWith(exportRoot.getCanonicalPath()));
		} catch (Throwable t) {
			prtln("isLegalExportDest ERROR: " + t.getMessage());
		}
		return false;
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  exportBaseDir     NOT YET DOCUMENTED
	 * @param  relativeDestPath  NOT YET DOCUMENTED
	 * @return                   NOT YET DOCUMENTED
	 * @exception  Exception     NOT YET DOCUMENTED
	 */
	public static File validateExportDestination(String exportBaseDir, String relativeDestPath)
		 throws Exception {

		String errorMsg;
		if (relativeDestPath == null || relativeDestPath.trim().length() == 0) {
			throw new Exception("destination path is required");
		}

		File test = new File(relativeDestPath);
		// prtln("relativeDestPath: " + relativeDestPath);
		if (test.isAbsolute()) {
			throw new Exception("Relative destination path is absolute (a relative path is required)");
		}

		File destDir = new File(exportBaseDir, relativeDestPath);
		String destDirPath = destDir.getAbsolutePath();
		String rootDirCpath = new File(exportBaseDir).getCanonicalPath();
		String destDirCpath = null;

		try {
			destDirCpath = new File(relativeDestPath).getCanonicalPath();
		} catch (Exception e) {
			throw new Exception("Illegal path fragment: \"" + relativeDestPath + "\"");
		}

		// prtln ("rootDirCpath " + rootDirCpath);
		// prtln ("destDirCpath " + destDirCpath);

		if (destDirCpath.indexOf(rootDirCpath) > -1) {
			errorMsg = "Destination Path (" + relativeDestPath + ") contains the baseExport Directory (" + exportBaseDir + ")";
			errorMsg += " whereas a _relative_ path is expected.";
			errorMsg += " You may want to visit the settings for this collection and modify the export directory there ...";
			prtln(errorMsg);
			throw new Exception(errorMsg);
		}

		if (!destDir.exists()) {
			prtln("dest does not exist at " + destDirPath + " .. attempting to create");
			if (!destDir.mkdirs()) {
				throw new Exception("destination could not be created at " + destDirPath);
			}
		}
		if (!destDir.isDirectory()) {
			throw new Exception("destination (" + destDirPath + ") exists but is not a directory");
		}
		if (!destDir.canWrite()) {
			throw new Exception("destination (" + destDirPath + ") exists and is a directory but is not writable");
		}
		return destDir;
	}



	/**
	 *  Gets the exportReport attribute of the ExportingService object
	 *
	 * @return    The exportReport value
	 */
	public ExportReport getExportReport() {
		return (ExportReport) super.getServiceReport();
	}


	/**
	 *  Exports records from specified collection and having specified statuses to
	 *  disk in a separate ExportThread.
	 *
	 * @param  destDir                        export directory
	 * @param  dcsSetInfo                     setInfo for collection to be exported
	 * @param  statuses                       record statuses to be exported
	 * @param  sessionBean                    NOT YET DOCUMENTED
	 * @exception  ExportingServiceException  if unable to export
	 */
	public void exportRecords(File destDir, DcsSetInfo dcsSetInfo, String[] statuses, SessionBean sessionBean)
		 throws ExportingServiceException {

		exportRecords(destDir, dcsSetInfo, statuses, sessionBean, null);
	}


	/**
	 *  Exports records from specified collection and having specified statuses to
	 *  disk, notifying observer when ExportThread is completed.<p>
	 *
	 *  This method will not complete if another ExportThread is active.
	 *
	 * @param  destDir                        export directory
	 * @param  dcsSetInfo                     setInfo for collection to be exported
	 * @param  statuses                       record statuses to be exported
	 * @param  sessionBean                    NOT YET DOCUMENTED
	 * @param  observer                       observer receives notification when
	 *      export is complete.
	 * @exception  ExportingServiceException  NOT YET DOCUMENTED
	 */
	public void exportRecords(File destDir, DcsSetInfo dcsSetInfo, String[] statuses, SessionBean sessionBean, ThreadedServiceObserver observer) throws ExportingServiceException {
		String destPath = destDir.getAbsolutePath();
		// prtln("exportRecords(" + destPath + ")");

		if (!isLegalExportDest(destDir)) {
			String errorMsg = "Export destination Path (" + destPath + ") is not contained by exportBaseDir (" + exportBaseDir + ")";
			throw new ExportingServiceException(errorMsg);
		}

		if (isProcessing) {
			prtln("ALERT: isProcessing!");
			return;
		}
		isProcessing = true;
		this.sessionBean = sessionBean;
		this.dcsSetInfo = dcsSetInfo;
		this.statuses = statuses;

		List idList = getIdList(dcsSetInfo.getSetSpec(), statuses);
		// Here is where we should obtain lock on records to export if we're going to?!

		try {
			new ExportThread(idList, destDir, observer).start();
		} catch (Throwable t) {
			prtln("WARNING: export Records: " + t.getMessage());
		}
	}


	/**
	 *  Triggered by ExportThread.run, writes specified records to a directory on
	 *  disk and save report.<p>
	 *
	 *  NOTE: this method assumes the destination directory has been validated and
	 *  exists.
	 *
	 * @param  destDir      Description of the Parameter
	 * @param  idsToExport  NOT YET DOCUMENTED
	 * @param  observer     NOT YET DOCUMENTED
	 */
	public void exportRecords(List idsToExport, File destDir, ThreadedServiceObserver observer) {
		// this is where we do the work

		int numNotExported = 0; // because of invalid or other problems
		int numExported = 0;
		int numNotChanged = 0;
		int numToExport = idsToExport.size();
		int granularity = 50;

		List filesDeleted = new ArrayList();
		List filesOverWritten = new ArrayList();
		List filesNewlyWritten = new ArrayList();

		String errorMsg = "";
		String msg = "";
		List exportedFileNames = new ArrayList();

		clearStatusMessages();
		clearServiceReport();
		addStatusMessage("Starting to Export " + numToExport + " records to " + destDir.getAbsolutePath());

		// Export Report
		Report report = new ExportReport(dcsSetInfo, statuses);
		report.setProp("destDir", destDir.getAbsolutePath());
		long start = new Date().getTime();

		for (int i = 0; i < idsToExport.size() && !stopProcessing; i++) {
			String id = (String) idsToExport.get(i);
			XMLDocReader docReader = getDocReader(id);

			if (i % granularity == 0 && i != 0) {
				msg = "Processed " + (i - 1) + " of " + numToExport + " items";
				msg += ". Progressing normally...";
				addStatusMessage(msg);
			}

			if (docReader == null) {
				errorMsg = "ERROR: could not find record for " + id + " in index";
				prtln(errorMsg);
				addStatusMessage(errorMsg);
				numNotExported++;
				continue;
			}
			File sourceFile = docReader.getFile();
			String fileName = sourceFile.getName();

			if (!sourceFile.exists()) {
				errorMsg = "source file does not exist at " + sourceFile.getAbsolutePath();
				// addStatusMessage (errorMsg);
				numNotExported++;
				continue;
			}

			RepositoryManager rm = docReader.getRepositoryManager();
			DcsDataRecord dcsDataRecord = dcsDataManager.getDcsDataRecord(id, rm);

			// perform validation and bad char checking if this record's file has
			// been modified outside of the DCS, OR if the record's validity is unknown
			// if (sourceFile.lastModified() != docReader.getLastModified() ||
			if (this.validationIsStale(sourceFile, docReader) ||
				dcsDataRecord.getIsValidityUnknown()) {
				prtln(id + " file mod does not match with reader mod or validity is unknown ... validating ...");

				// lastMods are not EXACTLY the same. need to allow a second or two difference ...
				/* 				prtln ("\tsourceFile.lastModified(): " + sourceFile.lastModified());
				prtln ("\tdocReader.getLastModified(): " + docReader.getLastModified());
				prtln ("\tdcsDataRecord.getIsValidityUnknown(): " + dcsDataRecord.getIsValidityUnknown()); */
				validate(docReader, dcsDataRecord);
				dcsDataManager.removeFromCache(id);
			}

			if (dcsDataRecord.getIsValid() == "false") {
				errorMsg = id + " is NOT VALID";
				// addStatusMessage (errorMsg);
				// prtln(errorMsg);

				numNotExported++;
				report.addEntry(dcsDataRecord);
				continue;
			}

			// if we haven't eliminated the current record from export due to invalid, then add
			// it to exportedFileNames, which is later used to delete records from the export directory that
			// were exported previously but weren't exported this run.
			exportedFileNames.add(fileName);

			// Only write to disk if the record to be exported has changed since last export
			File destFile = new File(destDir, fileName);
			if (destFile.exists() && sourceFile.lastModified() <= destFile.lastModified()) {
				// prtln ("sourceFile is not newer than destFile - not overwriting");
				numNotChanged++;
				numExported++;
				continue;
			}

			try {

				if (destFile.exists()) {
					// prtln ("adding " + fileName + " to filesOverWritten");
					filesOverWritten.add(fileName);
				}
				else {
					// prtln ("adding " + fileName + " to filesNewlyWritten");
					filesNewlyWritten.add(fileName);
				}

				// write file to destDir, using transform if possible
				String exportXml = getExportXml(docReader);
				// prtln ("exportXml for " + id + exportXml);
				
				Files.writeFile(exportXml, destFile);

				numExported++;
			} catch (Exception e) {
				numNotExported++;
				errorMsg = "export error: " + e.getMessage();
				prtln(errorMsg);
			}
			report.addEntry(dcsDataRecord);

			Thread.yield();
		}

		// clean up the destDir by deleting files that aren't on the exportedFileList
		File[] destFiles = destDir.listFiles(new XMLFileFilter());
		int numDeleted = 0;
		for (int i = destFiles.length - 1; i > -1; i--) {
			File destFile = destFiles[i];
			String destFileName = destFile.getName();
			if (!exportedFileNames.contains(destFileName)) {
				// prtln("deleting " + destFileName);
				destFile.delete();
				filesDeleted.add(destFile.getName());
				numDeleted++;
			}
		}

		prtln(numDeleted + " files removed from dest directory");
		// prtln ("exportRecords() stopProcessing: " + stopProcessing);

		// close out report
		report.recordsProcessed = numToExport;
		report.processingTime = new Date().getTime() - start;
		report.setProp("numDeleted", Integer.toString(numDeleted));
		report.setProp("numToExport", Integer.toString(numToExport));
		report.setProp("numExported", Integer.toString(numExported));
		report.setProp("numNotExported", Integer.toString(numNotExported));
		report.setProp("numNotChanged", Integer.toString(numNotChanged));

		Collections.sort(filesDeleted);
		report.setProp("filesDeleted", list2delimitedString(filesDeleted, ","));

		Collections.sort(filesOverWritten);
		report.setProp("filesOverWritten", list2delimitedString(filesOverWritten, ","));
		report.setProp("numOverWritten", Integer.toString(filesOverWritten.size()));

		Collections.sort(filesNewlyWritten);
		report.setProp("filesNewlyWritten", list2delimitedString(filesNewlyWritten, ","));
		report.setProp("numNewlyWritten", Integer.toString(filesNewlyWritten.size()));

		setServiceReport(report);
		archiveServiceReport(report);

		String exportSummary = numExported + " records exported, " + numNotExported + " NOT exported";
		if (stopProcessing) {
			msg = "Exporting stopped by user - " + exportSummary;
			addStatusMessage(msg);
			if (observer != null)
				observer.serviceCompleted(ThreadedServiceObserver.SERVICE_COMPLETED_ABORTED, msg);
		}
		else {
			msg = "Completed exporting records - " + exportSummary;
			addStatusMessage(msg);
			if (observer != null)
				observer.serviceCompleted(ThreadedServiceObserver.SERVICE_COMPLETED_SUCCESS, msg);
		}
	}


	/**
	 *  Get the XML to be exported, performing necessary transformations.<p>
	 *
	 *  for NLDR formats (osm, osm_next, library_dc) we cannonicalize urls to the
	 *  form:<br/>
	 *  baseNLDRRepositoryURL/collectionId/itemId/assetId (assetId when approp).
	 *
	 * @param  docReader      NOT YET DOCUMENTED
	 * @return                The exportXml value
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	private String getExportXml(XMLDocReader docReader) throws Exception {
		/*
		previously we had blindly run "cataloger-out" over all records, but really
		it only applied to a couple frameworks. (NOTE: we're still appying the cataloger
		out to every record, even though it really applies only to ADN (ask Katy for verification)).
		- we'll need some way to specify (on a per-framework basis, at least) what
		  transform, if any, to apply here.
		- also, we have to accomodate java-based transforms ...

		*/
		String REPO_PATH = (String) this.servletContext.getInitParameter("baseNLDRRepositoryURL");
		File sourceFile = docReader.getFile();
		String collection = docReader.getCollection();
		String exportXml = null;

		// OSM - we want to make a "citableURL" export
		if (REPO_PATH != null && docReader.getNativeFormat().equals("osm")) {
			// prtln ("apply OSM - exporting transform here!");
			StringBuffer xmlContent = Files.readFileToEncoding(sourceFile, "utf-8");
			exportXml = OsmRecordExporter.getExportXml(xmlContent.toString(), collection, REPO_PATH);
		}
		else if (transformer != null) {
			// prtln ("transformer: " + transformer.toString());
			exportXml = XSLTransformer.transformFile(sourceFile, transformer);
		}
		else {
			exportXml = Files.readFile(sourceFile).toString();
		}
		return exportXml;
	}


	/**
	 *  Print a line to standard out.
	 *
	 * @param  s  The String to print.
	 */
	protected static void prtln(String s) {
		if (debug) {
			System.out.println("ExportingService: " + s);
		}
	}


	/**
	 *  Description of the Class
	 *
	 */
	private class ExportThread extends Thread {
		List idList;
		File destDir = null;
		ThreadedServiceObserver observer = null;


		/**
		 *  Constructor for the ExportThread object
		 *
		 * @param  destDir   Description of the Parameter
		 * @param  idList    NOT YET DOCUMENTED
		 * @param  observer  NOT YET DOCUMENTED
		 */
		public ExportThread(List idList, File destDir, ThreadedServiceObserver observer) {
			this.idList = idList;
			this.destDir = destDir;
			this.observer = observer;
			setDaemon(true);
		}


		/**  Main processing method for the ExportThread object */
		public void run() {
			try {
				exportRecords(idList, destDir, observer);
			} catch (Throwable e) {
				addStatusMessage(e.getMessage());
				prtln("Error exporting records: " + e);
			} finally {
				isProcessing = false;
				sessionBean = null;
				dcsSetInfo = null;
			}
		}
	}

}

