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
package org.dlese.dpc.index;

import java.io.*;
import java.util.*;
import org.dlese.dpc.index.*;
import org.dlese.dpc.xml.*;
import org.dlese.dpc.datamgr.*;
import org.dlese.dpc.index.writer.*;
import org.dlese.dpc.index.reader.*;
import org.dlese.dpc.util.*;
import org.apache.lucene.document.*;
import java.text.*;

import org.dlese.dpc.webapps.tools.*;
import org.dlese.dpc.repository.*;

/**
 *  Indexes Web and query log files using Lucene.
 *  
 * @author    John Weatherley
 */
public final class WebLogIndexingService {
	private static final int MAX_ITEMS = 2000;
	
	private static boolean debug = true;
	private SimpleLuceneIndex webLogIndex;
	private File indexLive, indexBuild, tempDirForSwap;

	/**
	 *  The main program for the WebLogIndexingService
	 *
	 * @param  args  The command line arguments
	 */
	public static void main(String[] args) {
				
		WebLogIndexingService wlis = new WebLogIndexingService(null);	
		wlis.indexFiles();
		
		System.out.println("Done processing files");
	}


	public WebLogIndexingService(String configFile) {
		
		indexLive = new File("/home/jweather/web_logs/dds_query_logs/index_live");
		indexBuild = new File("/home/jweather/web_logs/dds_query_logs/index_build");
		tempDirForSwap = new File("/home/jweather/web_logs/dds_query_logs/index_build_swap");
		
	}

	public void indexFiles() {
		long totalIndexed = 0;
		
		try{
			Files.deleteDirectory(indexBuild);
		}catch(Throwable e){
			prtlnErr("Enable to delete build index: " + e);
			return;				
		}		
		
		
		webLogIndex = new SimpleLuceneIndex(indexBuild.getAbsolutePath());		
		//File f = new File("/home/jweather/web_logs/dds_query_logs/dds_queries.2004.01-short-test.log");
		File f = new File("/home/jweather/web_logs/dds_query_logs/dds_queries.2004.08.log");

		try{
			totalIndexed += indexWebLogFile(f);
		}catch(Exception e){
			prtln("Caught exception: " + e);
			e.printStackTrace();
			return;
		}
		
		boolean success = swapIndexes();
		
		if(success)
			prtln("New index is now ready for reading. " + totalIndexed + " entries were indexed.");		
		if(!success)
			prtln("New index was built but could not be moved to the live location. " + totalIndexed + " entries were indexed.");	
	}



	
	public long indexWebLogFile(File webLogFile)
		 throws IOException {
				
		long totalIndexed = 0;
		int i = 0;
		
		BufferedReader reader = new BufferedReader(new FileReader(webLogFile));
		
		List indexDocs = new ArrayList(MAX_ITEMS);
				
		IOException exception = null;
		WebLogEntryWriter writer;
		Document newDoc = null;
		try{
			String logEntry = "";
			
			while (logEntry != null) {
				logEntry = reader.readLine();
				
				
				
				if(logEntry != null && logEntry.trim().length() > 0) {
					writer = new WebLogEntryWriter();
					newDoc = writer.createLogEntryDoc(logEntry);
					if(newDoc != null){
						indexDocs.add(newDoc);
						totalIndexed++;
						i++;
					}
				}
				
				if(i == MAX_ITEMS){
					i = 0;
					prtln("Adding1 " + indexDocs.size() + " log entries to the index");
					webLogIndex.addDocs((Document[])indexDocs.toArray(new Document[]{}));	
					indexDocs.clear();
				}
				
			}
			
			// Add the remaining entries to the index
			if(indexDocs.size() > 0){
				prtln("Adding2 " + indexDocs.size() + " log entries to the index");
				webLogIndex.addDocs((Document[])indexDocs.toArray(new Document[]{}));	
				indexDocs.clear();
			}
			
		}catch(IOException ioe){
			exception = ioe;
			totalIndexed = totalIndexed - i;
			prtlnErr("Error while indexing file " + webLogFile.getName() + ": " + ioe + ". Total number indexed for this file: " + totalIndexed);
		}
		finally{
			reader.close();
			prtln("Total number of entries indexed for file " + webLogFile.getName() + ": " + totalIndexed);
		}
		if(exception != null)
			throw exception;
		
		return totalIndexed;		
	}


	public boolean swapIndexes(){
		boolean success = true;
		
		try{
			Files.deleteDirectory(tempDirForSwap);
		
			// If the indexing was successful, replace the live index with the newly built one
			if(indexLive.exists())
				success = indexLive.renameTo(tempDirForSwap);		
			if(!success)
				return false;
			success = indexBuild.renameTo(indexLive);		
			Files.deleteDirectory(tempDirForSwap);
		}catch(Throwable e){
			prtlnErr("Enable to swap indexes: " + e);	
			return false;
		}
		prtln("Index was moved to live: " + success);	
		return success;
	}
	

	// -------------------- Utility methods -------------------

	/**
	 *  Return a string for the current time and date, sutiable for display in log files and
	 *  output to standout:
	 *
	 * @return    The dateStamp value
	 */
	public static String getSimpleDateStamp() {
		try {
			return
				Utils.convertDateToString(new Date(), "EEE, MMM d h:mm:ss a");
		} catch (ParseException e) {
			return "";
		}
	}


	/**
	 *  Return a string for the current time and date, sutiable for display in log files and
	 *  output to standout:
	 *
	 * @return    The dateStamp value
	 */
	public static String getDateStamp() {
		return
			new SimpleDateFormat("MMM d, yyyy h:mm:ss a zzz").format(new Date());
	}


	/**
	 *  Output a line of text to error out, with datestamp.
	 *
	 * @param  s  The text that will be output to error out.
	 */
	private final void prtlnErr(String s) {
		System.err.println(getDateStamp() + " " + s);
	}



	/**
	 *  Output a line of text to standard out, with datestamp, if debug is set to true.
	 *
	 * @param  s  The String that will be output.
	 */
	private final void prtln(String s) {
		if (debug) {
			System.out.println(getDateStamp() + " " + s);
		}
	}


	/**
	 *  Sets the debug attribute object
	 *
	 * @param  db  The new debug value
	 */
	public static void setDebug(boolean db) {
		debug = db;
	}
}

