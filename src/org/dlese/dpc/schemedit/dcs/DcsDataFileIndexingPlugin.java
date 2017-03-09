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
package org.dlese.dpc.schemedit.dcs;

import java.io.*;
import java.util.*;
import java.text.*;

import org.dlese.dpc.schemedit.Constants;
import org.dlese.dpc.schemedit.SchemEditUtils;
import org.dlese.dpc.schemedit.MetaDataFramework;
import org.dlese.dpc.schemedit.FrameworkRegistry;
import org.dlese.dpc.schemedit.repository.RepositoryService;
import org.dlese.dpc.schemedit.security.user.User;
import org.dlese.dpc.schemedit.security.user.UserManager;

import org.dlese.dpc.index.writer.*;
import org.dlese.dpc.util.*;
import org.apache.lucene.document.*;
import org.dlese.dpc.repository.*;


/**
 *  Helper for indexing DCS-specific workflow status information along with the
 *  normally-indexed fields for a metadata record.<p>
 *
 *  Status information is obtained from a {@link org.dlese.dpc.schemedit.dcs.DcsDataRecord}
 *  via the {@link org.dlese.dpc.schemedit.dcs.DcsDataManager}.<p>
 *
 *  Invoked by the {@link org.dlese.dpc.repository.RepositoryManager} <b>
 *  putRecord</b> method during all indexing operations.
 *
 * @author     ostwald
 */
public class DcsDataFileIndexingPlugin extends ServletContextFileIndexingPlugin {

	/**  Description of the Field  */
	protected static boolean debug = true;

	/**  Name space prefix for dcs indexing fields  */
	public final static String FIELD_NS = "dcs";

	/**
	 *  Adds workflow status fields to the index for a particular record.
	 *
	 * @param  newDoc         The new Document that is being created for this
	 *      resource
	 * @param  existingDoc    An existing Document that currently resides in the
	 *      index for the given resource, or null if none was previously present
	 * @param  file           The sourceFile that is being indexed.
	 * @param  docType        The feature to be added to the Fields attribute
	 * @param  docGroup       The feature to be added to the Fields attribute
	 * @exception  Exception  If an error occurs
	 */
	public final void addFields(File file,
	                            org.apache.lucene.document.Document newDoc,
	                            org.apache.lucene.document.Document existingDoc,
	                            String docType, String docGroup)
		 throws Exception {
			 
		// try to obtain id from the source record to be indexed if at all possible.
		String id = null;
		try {
			id = newDoc.getField("idvalue").stringValue();
		} catch (Throwable t) {
			prtln("failed to obtain id from newDoc, trying fileName ...");
			if (file.exists()) {
				id = file.getName().split(".xml")[0];
			}
		} 
		
		// prtln ("id: " + id);
		
		DcsDataManager dcsDataManager = (DcsDataManager) getServletContext().getAttribute("dcsDataManager");
		if (dcsDataManager == null) {
			throw new Exception ("DcsDataManager not found in servlet context");
		}
		
		DcsDataRecord dcsDataRecord = dcsDataManager.getDcsDataRecord(docGroup, docType, file.getName(), id);
		if (dcsDataRecord == null) {
			throw new Exception("unable to obtain dcsDataRecord");
		}

/* 		System.out.println("dcsDataFile: " + dcsDataRecord.getSource().getAbsolutePath()
			 + " myDocType: " + docType + " myCollection: " + docGroup); */

		String lastEditor = dcsDataRecord.getLastEditor();
		if (lastEditor.trim().length() == 0) {
			lastEditor = Constants.UNKNOWN_EDITOR;
		}
		newDoc.add(new Field(FIELD_NS + "lastEditor", lastEditor, 
			Field.Store.YES, Field.Index.NOT_ANALYZED));

		// lastEditorName is used to sort search results, so it must be "NOT_ANALYZED"
		newDoc.add (new Field (FIELD_NS + "lastEditorName", getFullName(lastEditor), 
			Field.Store.YES, Field.Index.NOT_ANALYZED));
		
		newDoc.add(new Field(FIELD_NS + "isValid", dcsDataRecord.getIsValid(), 
			Field.Store.YES, Field.Index.NOT_ANALYZED));

		newDoc.add(new Field(FIELD_NS + "status", dcsDataRecord.getStatus(), 
			Field.Store.YES, Field.Index.NOT_ANALYZED));
		
		newDoc.add(new Field(FIELD_NS + "statusLabel", dcsDataRecord.getStatusLabel(), 
			Field.Store.YES, Field.Index.NOT_ANALYZED));

		String isFinalStatus = (dcsDataRecord.isFinalStatus() ? "true" : "false");
		newDoc.add(new Field(FIELD_NS + "isFinalStatus", isFinalStatus, 
			Field.Store.YES, Field.Index.NOT_ANALYZED));

		newDoc.add(new Field(FIELD_NS + "statusNote", dcsDataRecord.getStatusNote(), 
			Field.Store.YES, Field.Index.ANALYZED));

		newDoc.add(new Field(FIELD_NS + "hasSyncError", dcsDataRecord.getHasSyncError(), 
			Field.Store.YES, Field.Index.NOT_ANALYZED));

		newDoc.add(new Field(FIELD_NS + "ndrHandle", dcsDataRecord.getNdrHandle(), 
			Field.Store.YES, Field.Index.NOT_ANALYZED));

		Date lastTouchDate = getLastTouchDate(dcsDataRecord, file);
		newDoc.add(
			new Field(FIELD_NS + "lastTouchDate", DateField.dateToString(lastTouchDate), 
				Field.Store.YES, Field.Index.NOT_ANALYZED));
				
				
		/* index all status entries (except most recent??) */
		for (Iterator i=dcsDataRecord.getEntryList().iterator();i.hasNext();) {
			StatusEntry statusEntry = (StatusEntry)i.next();
			
			newDoc.add(new Field(FIELD_NS + "statusEntryLabel", 
								 dcsDataRecord.getStatusLabel(statusEntry.getStatus()), 
								 Field.Store.NO, Field.Index.NOT_ANALYZED));
			
			newDoc.add(new Field(FIELD_NS + "statusEntryNote", 
								 statusEntry.getStatusNote(), 
								 Field.Store.YES, Field.Index.ANALYZED));
				
			newDoc.add(new Field(FIELD_NS + "statusEntryChangeDate", 
								 DateField.dateToString(statusEntry.getDate()), 
								 Field.Store.NO, Field.Index.NOT_ANALYZED));	
							
			newDoc.add(new Field(FIELD_NS + "statusEntryEditor", 
								 statusEntry.getEditor(), 
								 Field.Store.NO, Field.Index.NOT_ANALYZED));
								 
			newDoc.add(new Field(FIELD_NS + "statusEntryEditorName", 
								 getFullName(statusEntry.getEditor()), 
								 Field.Store.NO, Field.Index.ANALYZED));
								 
			// entries processed in reverse chron order 
			if (!i.hasNext()) {
				//we're looking at the first status entry
				newDoc.add(new Field(FIELD_NS + "recordCreationDate", 
						 DateField.dateToString(statusEntry.getDate()), 
						 Field.Store.YES, Field.Index.NOT_ANALYZED));
						 
				String recordCreator = statusEntry.getEditor().trim();
				if (recordCreator.length() == 0)
					recordCreator = Constants.UNKNOWN_EDITOR;
				newDoc.add(new Field(FIELD_NS + "recordCreator", 
						 recordCreator, 
						 Field.Store.YES, Field.Index.NOT_ANALYZED));
			}
								 
		}

	}
	
 	private String getFullName (String userName) {
		try {
			UserManager userManager = 
				(UserManager)getServletContext().getAttribute("userManager");
			User user = userManager.getUser(userName);
			return user.getFullName();
		} catch (Throwable t) {}
		
		if (userName != null && userName.trim().length() > 0)
			return userName;
		else
			return Constants.UNKNOWN_USER;
	}

	/**
	 *  calculate a lastTouchDate for the DcsDataRecord<p>
	 *
	 *  New records will not have a lastTouchDate, so we give it a default date for
	 *  now so it may be indexed. The default date uses the lastModified time of
	 *  the indexed file, and if this is not found, then the "epoch" date is used.
	 *
	 * @param  file           NOT YET DOCUMENTED
	 * @return                The lastTouchDate value
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	private Date getLastTouchDate(DcsDataRecord dcsDataRecord, File file) throws Exception {

		String lastTouchString = dcsDataRecord.getLastTouchDate();

		// if there is no lastTouchDate stored in the dcsDataRecord, attempt to get the
		// file's lastModified date
		if (lastTouchString == null || lastTouchString.length() == 0) {
			prtln("  ... lastTouchDate not found in DcsDataRecord");
			if (file != null && file.exists()) {
				lastTouchString = SchemEditUtils.fullDateFormat.format(new Date(file.lastModified()));
				prtln("lastTouchString based on file lastMod: " + lastTouchString);
			}
			prtln("setting lastTouchSTring to " + lastTouchString);
			dcsDataRecord.setLastTouchDate(lastTouchString);
		}

		return dcsDataRecord.getLastTouchDateDate();
	}


	/**
	 *  Print a line to standard out.
	 *
	 * @param  s  The String to print.
	 */
	protected void prtln(String s) {
		if (debug) {
			System.out.println("DcsDataFileIndexingPlugin: " + s);
		}
	}
}

