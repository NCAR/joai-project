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
package org.dlese.dpc.index.writer;

import java.io.*;
import java.util.*;
import java.text.*;

import org.dlese.dpc.xml.*;
import org.dlese.dpc.index.*;
import org.dlese.dpc.repository.*;
import org.dlese.dpc.util.*;
import org.apache.lucene.document.*;
import org.dlese.dpc.index.reader.*;
import org.dlese.dpc.index.document.DateFieldTools;

/**
 *  Creates a Lucene {@link org.apache.lucene.document.Document}s for a DLESE annotation record. <p>
 *
 *  The Lucene fields that are created by this class are:
 *  <ul>
 *    <li> <code>collection</code> - The collection associated with this resource. Stored. Appended with a '0'
 *    at the beginning to support wildcard searching.</li>
 *    <li> Others... </li>
 *  </ul>
 *
 *
 * @author     John Weatherley
 * @see        org.dlese.dpc.index.reader.DleseAnnoDocReader
 */
public class DleseAnnoFileIndexingServiceWriter extends XMLFileIndexingWriter {

	private static long num_instances = 0;
	private File sourceFile = null;
	private XMLDoc dleseAnnoXmlDoc;
	private boolean statusIsCompleted = false;
	private String statusString = null;
	private String pathway = null;
	private String formatString = null;
	private String metadataFormat = "dlese_anno";


	/**  Create a DleseAnnoFileIndexingServiceWriter */
	public DleseAnnoFileIndexingServiceWriter() {
		num_instances++;
	}


	/**
	 *  Perform finalization... closing resources, etc.
	 *
	 * @exception  Throwable  If error
	 */
	protected void finalize() throws Throwable {
		try {
			num_instances--;
		} finally {
			super.finalize();
		}
	}


	/**
	 *  Gets the numInstances attribute of the DleseAnnoFileIndexingServiceWriter class
	 *
	 * @return    The numInstances value
	 */
	public static long getNumInstances() {
		return num_instances;
	}


	/**
	 *  Gets the docType attribute of the DleseAnnoFileIndexingServiceWriter object
	 *
	 * @return                The docType value
	 * @exception  Exception  if error
	 */
	public String getDocType()
		 throws Exception {
		return metadataFormat;
	}



	/**
	 *  Gets the fully qualified name of the concrete {@link org.dlese.dpc.index.reader.DocReader} class that is
	 *  used to read this type of {@link org.apache.lucene.document.Document}, for example
	 *  "org.dlese.dpc.index.reader.ItemDocReader".
	 *
	 * @return    The String "org.dlese.dpc.index.reader.DleseAnnoDocReader"
	 */
	public String getReaderClass() {
		return "org.dlese.dpc.index.reader.DleseAnnoDocReader";
	}


	/**
	 *  This method is called prior to processing and may be used to for any necessary set-up. This method should
	 *  throw and exception with appropriate message if an error occurs.
	 *
	 * @param  source         The source file being indexed
	 * @param  existingDoc    An existing Document that currently resides in the index for the given resource, or
	 *      null if none was previously present
	 * @exception  Exception  If an error occured during set-up.
	 */
	public void init(File source, Document existingDoc) throws Exception {
		//setDefaultFieldName("annodefault");

		// Initialize/reset values to null
		pathway = null;
		statusString = null;
		formatString = null;

		sourceFile = source;
		// Design note: If making a deleted doc, get content from getFileContent(), not the file:
		if(isMakingDeletedDoc()){
			dleseAnnoXmlDoc = new XMLDoc();
			dleseAnnoXmlDoc.useXmlString(
				getFileContent(),
				this.isValidationEnabled(),
				true,
				true);
		}		
		else {			
			String sourcePath = source.getAbsolutePath();
			if (sourcePath.startsWith("/"))
				sourcePath = sourcePath.substring(1, sourcePath.length());

			dleseAnnoXmlDoc = new XMLDoc(
				"file:///" + source.getAbsolutePath(),
				this.isValidationEnabled(),
				true,
				true);
		}


		// Determine the whether the status of the anno is 'completed' or not
		statusIsCompleted = (getStatus().matches(".*completed"));

		// If the item-level record for this annotation is not in the index, abort indexing.
		/* if(recordDataService != null){
			ResultDoc itemResultDoc = recordDataService.getItemResultDoc(getItemId());
			if (itemResultDoc == null){
				abortIndexing();
				prtln("anno record will not be created: item-level record " + getItemId() + " does not exist in the index.");
			}
			// If there is an item-level record, touch it's file so it will be re-indexed to capture this anno.
			else{
				if(Files.touch(((FileIndexingServiceDocReader)itemResultDoc.getDocReader()).getDocsource()))
					prtln("anno: touching file for item-level record " + getItemId() + " so it will be re-indexed.");
				else
					prtlnErr("DleseAnnoFileIndexingServiceWriter error: unable to touch record "
							+ getItemId() + ". File must have write permissions?");
			}
		} */
	}


	/**
	 *  Gets a report detailing any errors found in the validation of the data, or null if no error was found.
	 *
	 * @return                Null if no data validation errors were found, otherwise a String that details the
	 *      nature of the error.
	 * @exception  Exception  If error in performing the validation.
	 */
	protected String getValidationReport()
		 throws Exception {

		if (dleseAnnoXmlDoc.hasErrors())
			return dleseAnnoXmlDoc.getErrors().toString();
		else
			return null;
	}


	/**  This method is called at the conclusion of processing and may be used for tear-down. */
	protected void destroy() {
		dleseAnnoXmlDoc = null;
	}


	/**
	 *  Gets the id of this annotation
	 *
	 * @return    The id value
	 */
	protected String[] _getIds() {
		return new String[] {dleseAnnoXmlDoc.getXmlField("service/recordID")};
	}
	
	public String[] getUrls() {
		// Path for anno framework v1.0
		String fieldContent = dleseAnnoXmlDoc.getXmlField("annotation/content/url");
		// Path for anno framework v0.1
		if (fieldContent.length() == 0)
			fieldContent = dleseAnnoXmlDoc.getXmlField("item/content/url");
		
		if (fieldContent.length() > 0)		
			return new String[] {fieldContent};
		else
			return null;
	}	
	
	public String getTitle() throws Exception {
		// Path for anno framework v1.0
		String fieldContent = dleseAnnoXmlDoc.getXmlField("annotation/title");
		// Path for anno framework v0.1
		if (fieldContent.trim().length() == 0)
			fieldContent = dleseAnnoXmlDoc.getXmlField("item/title");
		if (fieldContent.trim().length() == 0)
			return null;
		return fieldContent;
	}
	
	public String getDescription() throws Exception {
		// Path for anno framework v1.0
		String fieldContent = dleseAnnoXmlDoc.getXmlField("annotation/content/description");
		// Path for anno framework v0.1
		if (fieldContent.length() == 0)
			fieldContent = dleseAnnoXmlDoc.getXmlField("item/content/description");
		if (fieldContent.length() == 0)
			return null;
		return fieldContent;
	}

	private String getPathway() {
		if (pathway == null) {
			// Path for anno framework v1.0
			pathway = dleseAnnoXmlDoc.getXmlField("service/pathway");
			// Path for anno framework v0.1
			if (pathway.length() == 0)
				pathway = dleseAnnoXmlDoc.getXmlField("item/pathway");
		}
		return pathway;
	}


	/**
	 *  Gets the status as a String, which is normalized to be the same for v1.0 and v0.1 of the framework. One
	 *  of 'completed', 'in progress' or 'retired'.
	 *
	 * @return    The status value
	 */
	private String getStatus() {
		if (statusString == null) {

			// Path for anno framework v1.0
			statusString = dleseAnnoXmlDoc.getXmlField("annotation/status").trim().toLowerCase();

			// Path for anno framework v0.1
			if (statusString.length() == 0) {
				statusString = dleseAnnoXmlDoc.getXmlField("item/statusOf@status").trim().toLowerCase();
				if (statusString.indexOf("completed") != -1)
					statusString = "completed";
				else if (statusString.indexOf("in progress") != -1)
					statusString = "in progress";
			}

			// If no status is given, set it to "completed":
			if (statusString.length() == 0)
				statusString = "completed";
		}
		return statusString;
	}


	/**
	 *  Gets the format as a String, which is normalized to be the same for v1.0 and v0.1 of the framework. One
	 *  of 'audio', 'graphical', 'text' or 'video'.
	 *
	 * @return    The format value
	 */
	private String getFormat() {
		if (formatString == null) {

			// Format is new in v1.0 - was combined as part of status in v0.1
			formatString = dleseAnnoXmlDoc.getXmlField("annotation/format").trim().toLowerCase();

			// Path for anno framework v1.0 - format was part of status in v0.1
			if (formatString.length() == 0) {
				formatString = dleseAnnoXmlDoc.getXmlField("item/statusOf@status").trim().toLowerCase();

				// Strip out the status part so we just have the format:
				formatString = formatString.replaceFirst(" annotation completed", "").replaceFirst(" annotation in progress", "");
			}
		}
		return formatString;
	}


	/**
	 *  Returns the the first available of item/statusOf@date, service/date@modified or service/date@created.
	 *  Note that the statusOf@date was deprecated as of anno framework v1.0.
	 *
	 * @return                The what's new date for the annotation
	 * @exception  Exception  This method should throw and Exception with appropriate error message if an error
	 *      occurs.
	 */
	protected Date getWhatsNewDate() throws Exception {
		// The statusOf@date was deprecated as of v1.0
		String wnDate = dleseAnnoXmlDoc.getXmlField("item/statusOf@date");
		if (wnDate == null || wnDate.length() == 0)
			wnDate = dleseAnnoXmlDoc.getXmlField("service/date@modified");
		if (wnDate == null || wnDate.length() == 0)
			wnDate = dleseAnnoXmlDoc.getXmlField("service/date@created");
		try {
			return MetadataUtils.parseUnionDateType(wnDate);
		} catch (ParseException e) {
			throw new Exception("Error parsing one of item/statusOf@date, service/date@modified " +
				"or service/date@created for what's new date. " + e.getMessage());
		}
	}


	/**
	 *  Returns 'annocomplete,' 'annoinprogress,' 'drcannocomplete,' or 'drcannoinprogress'.
	 *
	 * @return                The string 'annocomplete,' 'annoinprogress,' 'drcannocomplete,' or
	 *      'drcannoinprogress'.
	 * @exception  Exception  If error
	 */
	protected String getWhatsNewType() throws Exception {
		String pathway = getPathway();
		DleseCollectionDocReader myCollectionDoc = getMyCollectionDoc();
		boolean isPartOfDrc = false;
		if (myCollectionDoc == null)
			isPartOfDrc = (pathway != null && pathway.length() > 0);
		else
			isPartOfDrc = (myCollectionDoc.isPartOfDRC() && pathway != null && pathway.length() > 0);

		if (statusIsCompleted) {
			if (isPartOfDrc)
				return "drcannocomplete";
			else
				return "annocomplete";
		}
		else {
			if (isPartOfDrc)
				return "drcannoinprogress";
			else
				return "annoinprogress";
		}
	}

	/**
	 *  Default and stems fields handled here, so do not index full content.
	 *
	 * @return    False
	 */
	public boolean indexFullContentInDefaultAndStems() {
		return false;
	}
	
	/**
	 *  Adds additional custom fields that are unique to the dlese anno document format being indexed.
	 *
	 * @param  newDoc         The new {@link org.apache.lucene.document.Document} that is being created for this
	 *      resource
	 * @param  existingDoc    An existing {@link org.apache.lucene.document.Document} that currently resides in
	 *      the index for the given resource, or null if none was previously present
	 * @param  sourceFile     The sourceFile that is being indexed
	 * @exception  Exception  This method should throw and Exception with appropriate error message if an error
	 *      occurs.
	 */
	protected void addFields(Document newDoc, Document existingDoc, File sourceFile) throws Exception {
		String fieldContent;

		fieldContent = dleseAnnoXmlDoc.getXmlField("service/name");
		newDoc.add(new Field("annoservicename", fieldContent, Field.Store.YES, Field.Index.ANALYZED));

		fieldContent = getTitle();
		if(fieldContent != null){
			newDoc.add(new Field("annotitle", fieldContent, Field.Store.YES, Field.Index.ANALYZED));
			addToDefaultField(fieldContent);
		}

		// The ID of the item being annotated
		fieldContent = getItemId();
		newDoc.add(new Field("annoitemid", fieldContent, Field.Store.YES, Field.Index.NOT_ANALYZED));

		// The pathway
		String pathway = getPathway();
		newDoc.add(new Field("annopathway", pathway, Field.Store.YES, Field.Index.ANALYZED));

		// Path for anno framework v1.0
		fieldContent = dleseAnnoXmlDoc.getXmlField("annotation/type");
		// Path for anno framework v0.1
		if (fieldContent.length() == 0)
			fieldContent = dleseAnnoXmlDoc.getXmlField("item/type");
		newDoc.add(new Field("annotype", fieldContent, Field.Store.YES, Field.Index.ANALYZED));

		// Anno date is only available in v0.1
		fieldContent = dleseAnnoXmlDoc.getXmlField("item/date");
		if (fieldContent != null && fieldContent.length() > 0) {
			Date date = null;
			try {
				date = MetadataUtils.parseUnionDateType(fieldContent);
			} catch (Throwable t) {}
			if (date != null)
				newDoc.add(new Field("annodate", DateFieldTools.dateToString(date), Field.Store.YES, Field.Index.ANALYZED));
		}

		// Status
		newDoc.add(new Field("annostatus", getStatus(), Field.Store.YES, Field.Index.ANALYZED));

		// Format
		newDoc.add(new Field("annoformat", getFormat(), Field.Store.YES, Field.Index.ANALYZED));

		// Star rating is new in v1.0
		fieldContent = dleseAnnoXmlDoc.getXmlField("annotation/content/rating");
		String annorating = "";
		if (fieldContent.startsWith("Five"))
			annorating = "5";
		else if (fieldContent.startsWith("Four"))
			annorating = "4";
		else if (fieldContent.startsWith("Three"))
			annorating = "3";
		else if (fieldContent.startsWith("Two"))
			annorating = "2";
		else if (fieldContent.startsWith("One"))
			annorating = "1";
		newDoc.add(new Field("annorating", annorating, Field.Store.YES, Field.Index.ANALYZED));

		// Determine whether the anno is part of the DRC
		DleseCollectionDocReader collectionDoc = getMyCollectionDoc();
		if (collectionDoc != null) {
			if (pathway == null)
				pathway = "";
			if (collectionDoc.isPartOfDRC() && statusIsCompleted && pathway.length() > 0)
				newDoc.add(new Field("annoispartofdrc", "true", Field.Store.YES, Field.Index.ANALYZED));
			else
				newDoc.add(new Field("annoispartofdrc", "false", Field.Store.YES, Field.Index.ANALYZED));
		}
		else
			newDoc.add(new Field("annoispartofdrc", "false", Field.Store.YES, Field.Index.ANALYZED));

		// Index anno URLs separately:
		if (getUrls() != null && getUrls().length > 0) {
			String annoUrl = getUrls()[0];
			
			// Store the resource url for reference
			newDoc.add(new Field("annourl", annoUrl, Field.Store.YES, Field.Index.NO));
			// Then parse it for indexing
			newDoc.add(new Field("annouri", IndexingTools.tokenizeURI(annoUrl), Field.Store.NO, Field.Index.ANALYZED));
			addToDefaultField(IndexingTools.tokenizeURI(annoUrl));
			addToAdminDefaultField(IndexingTools.tokenizeURI(annoUrl));
		}

		fieldContent = getDescription();
		if (fieldContent != null) {
			newDoc.add(new Field("annodescription", fieldContent, Field.Store.YES, Field.Index.ANALYZED));
			addToDefaultField(fieldContent);
		}

		// Add the entire content of the XML to the admin default search field:
		String xml = getFileContent();
		addToAdminDefaultField(XMLConversionService.getContentFromXML(xml));
	}


	/**
	 *  Gets the itemId attribute of the DleseAnnoFileIndexingServiceWriter object
	 *
	 * @return    The itemId value
	 */
	private String getItemId() {
		// Path for anno framework v1.0
		String val = dleseAnnoXmlDoc.getXmlField("itemID");
		// Path for anno framework v0.1
		if (val.length() == 0)
			val = dleseAnnoXmlDoc.getXmlField("item/itemID");
		return val;
	}

}

