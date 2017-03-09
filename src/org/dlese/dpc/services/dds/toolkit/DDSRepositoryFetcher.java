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
package org.dlese.dpc.services.dds.toolkit;

import java.util.*;
import java.util.regex.*;
import java.net.*;
import java.io.*;
import org.dlese.dpc.schemedit.*;
import org.dlese.dpc.xml.Dom4jUtils;
import org.dlese.dpc.util.Files;
import org.dlese.dpc.util.Utils;
import org.dom4j.*;

/**
 *  Downloads entire DDS repositories and writes to local disk using the
 *  DDSServicesToolkit.
 *
 * @author    ostwald
 */
public class DDSRepositoryFetcher {

	private static boolean debug = true;

	String baseUrl;
	File backupsDir;
	DDSServicesToolkit toolkit;
	File repository;
	String ddsInstanceName;
	List errors;
	RepositoryUtils repoUtils;


	/**
	 *  Download all collections of repository and write to disk.
	 *
	 * @param  baseUrl       DDS webservice baseUrl
	 * @param  backupsDir    Destination repository to be written
	 * @param  instanceName  name of repository used to name disk files
	 */
	public static void fetch(String baseUrl, File backupsDir, String instanceName) {
		DDSRepositoryFetcher fetcher = new DDSRepositoryFetcher(baseUrl, backupsDir, instanceName);
		try {
			fetcher.fetchRepository();
		} catch (Exception e) {
			prtln("Fetch error: " + e.getMessage());
		}
	}


	/**
	 *  Constructor for the DDSRepositoryFetcher object
	 *
	 * @param  baseUrl          DDS webservice baseUrl
	 * @param  backupsDir       Destination repository to be written
	 * @param  ddsInstanceName  name of repository used to name disk files
	 */

	DDSRepositoryFetcher(String baseUrl, File backupsDir, String ddsInstanceName) {
		this.baseUrl = baseUrl;
		this.backupsDir = backupsDir;
		this.ddsInstanceName = ddsInstanceName;
		try {
			this.repository = this.getRepositoryDir();
		} catch (Exception e) {
			prtln("Error: could not establish repository directory: " + e.getMessage());
			return;
		}
		this.toolkit = new DDSServicesToolkit(baseUrl, null);
		this.repoUtils = new RepositoryUtils(baseUrl);
		this.errors = new ArrayList();
	}


	/**
	 *  Gets the directory that will hold the fetched information for this
	 *  repository.<p>
	 *
	 *  Directories containing fetched repository is named by the ddsInstanceName
	 *  (e.g., "DLESE"), then the date ("2009_08-14"). If more than one fetch is
	 *  done on a particular date, a version number is appended ("2009_08-14_1").
	 *
	 * @return                The repositoryDir value
	 * @exception  Exception  if repositoryDir cannot be initialized
	 */
	File getRepositoryDir() throws Exception {
		if (!this.backupsDir.exists())
			throw new Exception("Master backup directory does not exist at " + backupsDir);

		File instanceDir = new File(backupsDir, ddsInstanceName);
		if (!instanceDir.exists() && !instanceDir.mkdir())
			throw new Exception("couldnt create instanceDir at " + instanceDir);

		String repoDirName = Utils.convertDateToString(new Date(), "yyyy-MM-dd");

		File repoDir = new File(instanceDir, repoDirName);

		int num = 0;
		while (repoDir.exists())
			repoDir = new File(instanceDir, repoDirName + "_" + ++num);

		if (!repoDir.mkdir())
			throw new Exception("could not created repository directory at " + repoDir);

		return repoDir;
	}


	/**
	 *  Description of the Method
	 *
	 * @exception  Exception  Description of the Exception
	 */
	void fetchRepository() throws Exception {

		// we've got an empty repository - do collections
		List collections = this.repoUtils.getCollectionInfos();
		prtln(collections.size() + " collections to process");

		// Walk the collections (using collection Infos)
		for (Iterator i = collections.iterator(); i.hasNext(); ) {
			CollectionInfo collectionInfo = (CollectionInfo) i.next();
			String collectionKey = collectionInfo.getCollectionKey();
			File formatDir = new File(repository, collectionInfo.formatOfRecords);
			if (!formatDir.exists() && !formatDir.mkdir()) {
				throw new Exception("could not create formatDir at " + formatDir);
			}

			File collectionDir = new File(formatDir, collectionKey);
			if (!collectionDir.exists() && !collectionDir.mkdir()) {
				throw new Exception("could not create collectionDir at " + collectionDir);
			}

			try {
				// prtln ("\n\ncalling fetchCollection with ... " + collectionInfo.toString());
				fetchCollection(collectionInfo, collectionDir);
			} catch (Exception e) {
				String msg = "ERROR could not fetch collection for \"" + collectionKey + "\": " + e.getMessage();
				prtln("ERROR! - see error report at the end of output");
				collectionInfo.errorMsg = msg;
				this.errors.add(collectionInfo);
				continue;
			}
		}
		if (!errors.isEmpty()) {
			prtln("\nERRORS: The following collections could not be fetched");
			for (Iterator i = errors.iterator(); i.hasNext(); ) {
				CollectionInfo colInfo = (CollectionInfo) i.next();
				prtln("\t" + colInfo.getCollectionKey() + " - " + colInfo.errorMsg);
			}
			prtln("\nNote: ADN version 0.7.00 records cannot be fetched with DDS Webservice." +
				"\nThis usually results in a \"Invalid HTTP response code: 500\" message.\n");
		}
	}


	/**
	 *  Fetches the items in specified collection and writes them into
	 *  collectionsDir
	 *
	 * @param  collectionInfo  the collection to fetch
	 * @param  collectionDir   destination for itemRecords on disk
	 * @exception  Exception   if any error fetching or writing itemRecords to disk
	 */
	void fetchCollection(CollectionInfo collectionInfo, File collectionDir) throws Exception {

		int startOffset = 0;
		int batchSize = 400;

		prtln("\nFetching " + collectionInfo.getCollectionKey() + " (" + collectionInfo.formatOfRecords +
			")  " + collectionInfo.numRecords + " records");

		while (startOffset < collectionInfo.numRecords) {
			try {
				Map itemRecordBatch = this.repoUtils.getItemRecordMap(collectionInfo, startOffset, batchSize);

				for (Iterator i = itemRecordBatch.keySet().iterator(); i.hasNext(); ) {
					String id = (String) i.next();
					Document itemRecord = (Document) itemRecordBatch.get(id);
					try {
						/*
						 *  WRITE the record item Document to disk, named by id+'.xml'
						 */
						String filename = id + ".xml";
						File dest = new File(collectionDir, filename);
						Dom4jUtils.writeDocToFile(itemRecord, dest);
						// prtln("processed " + id);
					} catch (Throwable e) {
						throw new Exception("Could not write " + id + " to disk: " + e.getMessage());
					}
				}

				startOffset = startOffset + itemRecordBatch.size();
			} catch (Exception e) {
				throw new Exception("failed on batch starting with " + startOffset + ": " + e.getMessage());
			}
		}

		int numWritten = collectionDir.listFiles().length;
		prtln(numWritten + " files written to disk");
	}


	/**
	 *  The main program for the DDSRepositoryFetcher class
	 *
	 * @param  args           The command line arguments
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public static void main(String[] args) throws Exception {
		org.dlese.dpc.schemedit.test.TesterUtils.setSystemProps();
		prtln("DDSRepositoryFetcher");

		fetchPreview();
		// fetchDlese();

	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  n  NOT YET DOCUMENTED
	 */
	private static void pp(Node n) {
		prtln(Dom4jUtils.prettyPrint(n));
	}


	/**
	 *  Description of the Method
	 *
	 * @param  s  Description of the Parameter
	 */
	private static void prtln(String s) {
		if (debug) {
			// System.out.println("DDSRepositoryFetcher: " + s);
			System.out.println(s);
		}
	}

	// testers --------------------------------

	/**  NOT YET DOCUMENTED */
	static void fetchPreview() {
		prtln("Fetching from Preview");

		String baseUrl = "http://dcs.dls.ucar.edu/schemedit/services/ddsws1-1";
		File backupsDir = new File("C:/tmp/FetchedRepos/"); // dls-local
		String instanceName = "preview";

		DDSRepositoryFetcher.fetch(baseUrl, backupsDir, instanceName);
	}


	/**  NOT YET DOCUMENTED */
	static void fetchDlese() {
		prtln("Fetching from Dlese");

		String baseUrl = "http://www.dlese.org/dds/services/ddsws1-1";
		File backupsDir = new File("C:/tmp/FetchedRepos/"); // dls-local
		String instanceName = "dlese";

		DDSRepositoryFetcher.fetch(baseUrl, backupsDir, instanceName);
	}

}

