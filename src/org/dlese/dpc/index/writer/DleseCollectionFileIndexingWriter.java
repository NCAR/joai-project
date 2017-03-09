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
import org.dlese.dpc.index.reader.*;
import org.dlese.dpc.services.mmd.MmdRec;
import org.dlese.dpc.util.*;
import org.apache.lucene.document.*;
import org.dlese.dpc.repository.*;
import org.dlese.dpc.vocab.*;
import org.dlese.dpc.index.document.DateFieldTools;

import org.dom4j.Element;
import org.dom4j.Attribute;
import org.dom4j.Node;

/**
 *  Used to write a Lucene {@link org.apache.lucene.document.Document} for a DLESE Collection XML record. The
 *  reader for this type of {@link org.apache.lucene.document.Document} is {@link
 *  org.dlese.dpc.index.reader.DleseCollectionDocReader}. <p>
 *
 *
 *
 * @author    John Weatherley
 * @see       org.dlese.dpc.index.reader.XMLDocReader
 * @see       org.dlese.dpc.repository.RecordDataService
 * @see       org.dlese.dpc.index.writer.FileIndexingServiceWriter
 */
public class DleseCollectionFileIndexingWriter extends XMLFileIndexingWriter {

	private String metadataFormat = "dlese_collect";

	private static long num_instances = 0;


	/**  Create a DleseCollectionFileIndexingWriter. */
	public DleseCollectionFileIndexingWriter() {
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
	 *  Gets the numInstances attribute of the DleseCollectionFileIndexingWriter class
	 *
	 * @return    The numInstances value
	 */
	public static long getNumInstances() {
		return num_instances;
	}


	/**
	 *  Returns the full title for the collection.
	 *
	 * @return                The fullTitle value
	 * @exception  Exception  If error reading XML.
	 */
	protected String getFullTitle() throws Exception {
		List nodes = getDom4jDoc().selectNodes("/*[local-name()='collectionRecord']/*[local-name()='general']/*[local-name()='fullTitle']");
		if (nodes.size() != 1)
			return getShortTitle();
		Element element = (Element) nodes.get(0);
		return element.getText();
	}


	/**
	 *  Returns the short title for the collection.
	 *
	 * @return                The shortTitle value
	 * @exception  Exception  If error reading XML.
	 */
	protected String getShortTitle() throws Exception {
		List nodes = getDom4jDoc().selectNodes("/*[local-name()='collectionRecord']/*[local-name()='general']/*[local-name()='shortTitle']");
		if (nodes.size() != 1)
			throw new Exception("Unable to get Short Title. Wrong number of elements: " + nodes.size());
		Element element = (Element) nodes.get(0);
		return element.getText();
	}


	/**
	 *  Gets the full title
	 *
	 * @return                The title value
	 * @exception  Exception  If error
	 */
	public String getTitle() throws Exception {
		return getFullTitle();
	}


	/**
	 *  The description for the collection.
	 *
	 * @return                The description String
	 * @exception  Exception  If error reading XML.
	 */
	public String getDescription() throws Exception {
		List nodes = getDom4jDoc().selectNodes("/*[local-name()='collectionRecord']/*[local-name()='general']/*[local-name()='description']");
		if (nodes.size() != 1)
			throw new Exception("Unable to get Description. Wrong number of elements: " + nodes.size());
		String text = ((Element) nodes.get(0)).getText();
		return text;
	}


	/**
	 *  Gets the additional metadata for this collection that was indicated in {@link
	 *  org.dlese.dpc.repository.RepositoryManager.putRecord} when the collection was created inside an
	 *  additionalMetadata element, or null.
	 *
	 * @return    The additional metadata element as an String, or null if none.
	 */
	public String getAdditionalMetadata() {
		try {
			Node additionalMetadataNode = getDom4jDoc().selectSingleNode("/*[local-name()='collectionRecord']/*[local-name()='additionalMetadata']");
			if (additionalMetadataNode == null)
				return null;
			String additionalMetadata = additionalMetadataNode.asXML();
			if (additionalMetadata == null || additionalMetadata.trim().length() == 0)
				return null;
			return additionalMetadata;
		} catch (Throwable t) {
			//prtlnErr("getAdditionalMetadata(): " + t);
			t.printStackTrace();
		}
		return null;
	}


	/**
	 *  Gets whether the collection is part of the DRC [true|false].
	 *
	 * @return                The partOfDRC Value
	 * @exception  Exception  If error
	 */
	protected String getPartOfDRC() throws Exception {
		List nodes = getDom4jDoc().selectNodes("/*[local-name()='collectionRecord']/*[local-name()='access']/*[local-name()='drc']");
		if (nodes.size() != 1)
			return "false";
		Element element = (Element) nodes.get(0);
		return element.getText();
	}


	/**
	 *  Gets the most recent accession status found in the XML record.
	 *
	 * @return                The most recent accession status.
	 * @exception  Exception  If error
	 */
	protected String getAccessionStatus() throws Exception {
		return getCurrentCollectionStatus(getDom4jDoc()).toLowerCase();
	}


	/**
	 *  Gets the collectionStatus attribute of the DleseCollectionFileIndexingWriter object
	 *
	 * @return                The collectionStatus value
	 * @exception  Exception  If error
	 */
	protected String getCollectionStatuses() throws Exception {
		return "";
	}


	/**
	 *  Gets the collection key used to identify the items in the collection this record refers to. For example,
	 *  dcc or comet.
	 *
	 * @return                The Key value
	 * @exception  Exception  If error
	 */
	protected String getKey() throws Exception {
		List nodes = getDom4jDoc().selectNodes("/*[local-name()='collectionRecord']/*[local-name()='access']/*[local-name()='key']");
		if (nodes.size() != 1)
			throw new Exception("Unable to get key. Wrong number of elements: " + nodes.size());
		Element element = (Element) nodes.get(0);
		return element.getText();
	}


	/**
	 *  Gets the URL to the collection.
	 *
	 * @return                The collectionUrl value
	 * @exception  Exception  If error
	 */
	public String[] getUrls() throws Exception {
		List nodes = getDom4jDoc().selectNodes("/*[local-name()='collectionRecord']/*[local-name()='access']/*[local-name()='collectionLocation']");
		if (nodes.size() == 0)
			return null;
		Element element = (Element) nodes.get(0);
		return new String[]{element.getText()};
	}


	/**
	 *  Gets the URL to the collection's scope statement.
	 *
	 * @return                The URL to the collection's scope statement, or null if none.
	 * @exception  Exception  If error
	 */
	protected String getScopeUrl() throws Exception {
		List nodes = getDom4jDoc().selectNodes("/*[local-name()='collectionRecord']/*[local-name()='general']/*[local-name()='policies']/*[local-name()='policy'][@type='Collection scope']/@url");
		if (nodes == null || nodes.size() == 0)
			return null;
		Attribute attrib = (Attribute) nodes.get(0);
		return attrib.getValue();
	}


	/**
	 *  Gets the URL to the collection's review process statement.
	 *
	 * @return                The URL to the collection's review process statement.
	 * @exception  Exception  If error
	 */
	protected String getReviewProcessUrl() throws Exception {
		List nodes = getDom4jDoc().selectNodes("/*[local-name()='collectionRecord']/*[local-name()='general']/*[local-name()='reviewProcess']/@url");
		if (nodes.size() > 1)
			prtln("More than one review process URL!");
		if (nodes.size() == 0)
			return null;
		Attribute attrib = (Attribute) nodes.get(0);
		return attrib.getValue();
	}


	/**
	 *  Gets the collection's review process statement.
	 *
	 * @return                The collection's review process statement.
	 * @exception  Exception  If error
	 */
	protected String getReviewProcess() throws Exception {
		List nodes = getDom4jDoc().selectNodes("/*[local-name()='collectionRecord']/*[local-name()='general']/*[local-name()='reviewProcess']");
		if (nodes.size() > 1)
			prtln("More than one review process!");
		if (nodes.size() == 0)
			return null;
		Element element = (Element) nodes.get(0);
		return element.getText();
	}


	/**
	 *  Gets the format of the records in this collection.
	 *
	 * @return                The records format.
	 * @exception  Exception  If error
	 */
	protected String getFormatOfRecords() throws Exception {
		List nodes = getDom4jDoc().selectNodes("/*[local-name()='collectionRecord']/*[local-name()='access']/*[local-name()='key']/@libraryFormat");
		if (nodes.size() != 1)
			throw new Exception("Unable to get format of records. Wrong number of elements: " + nodes.size());
		Attribute attrib = (Attribute) nodes.get(0);
		return attrib.getValue();
	}



	/**
	 *  Gets the cost associated with this collection.
	 *
	 * @return                The cost.
	 * @exception  Exception  If error
	 */
	protected String getCost() throws Exception {
		List nodes = getDom4jDoc().selectNodes("/*[local-name()='collectionRecord']/*[local-name()='general']/*[local-name()='cost']");

		if (nodes.size() != 1)
			return null;
		Element element = (Element) nodes.get(0);
		return element.getText().replaceAll(":", "").replaceAll(" ", "");
	}



	/**
	 *  Gets the keywords associated with this collection.
	 *
	 * @return                The all keywords separated by spaces.
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	protected String getKeywords() throws Exception {
		List nodes = getDom4jDoc().selectNodes("/*[local-name()='collectionRecord']/*[local-name()='general']/*[local-name()='keywords']/*[local-name()='keyword']");
		String tmp = "";
		if (nodes.size() > 1)
			tmp = ((Element) nodes.get(0)).getText();
		for (int i = 1; i < nodes.size(); i++)
			tmp += "+" + ((Element) nodes.get(i)).getText();
		return tmp.trim();
	}



	/**
	 *  Gets the gradeRanges for this collection.
	 *
	 * @return                The gradeRanges value
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	protected String[] getGradeRanges() throws Exception {
		List nodes = getDom4jDoc().selectNodes("/*[local-name()='collectionRecord']/*[local-name()='general']/*[local-name()='gradeRanges']/*[local-name()='gradeRange']");
		ArrayList vals = new ArrayList();
		String tmp;
		for (int i = 0; i < nodes.size(); i++) {
			tmp = ((Element) nodes.get(i)).getText();
			if (!vals.contains(tmp))
				vals.add(tmp);
		}
		return (String[]) vals.toArray(new String[]{});
	}


	/**
	 *  Gets the subjects for this collection.
	 *
	 * @return                The subjects value
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	protected String[] getSubjects() throws Exception {
		List nodes = getDom4jDoc().selectNodes("/*[local-name()='collectionRecord']/*[local-name()='general']/*[local-name()='subjects']/*[local-name()='subject']");
		ArrayList vals = new ArrayList();
		String tmp;
		for (int i = 0; i < nodes.size(); i++) {
			tmp = ((Element) nodes.get(i)).getText();
			if (!vals.contains(tmp))
				vals.add(tmp);
		}
		return (String[]) vals.toArray(new String[]{});
	}



	// --------------- Methods from FileIndexingServiceWriter --------------------

	/**
	 *  Gets the ID of this collection record.
	 *
	 * @return                The ID
	 * @exception  Exception  If error
	 */
	protected String[] _getIds() throws Exception {
		List nodes = getDom4jDoc().selectNodes("/*[local-name()='collectionRecord']/*[local-name()='metaMetadata']/*[local-name()='catalogEntries']/*[local-name()='catalog']/@entry");
		if (nodes.size() != 1)
			throw new Exception("Unable to get ID. Wrong number of elements: " + nodes.size());
		Attribute attrib = (Attribute) nodes.get(0);
		return new String[]{attrib.getValue()};
	}


	/**
	 *  Gets the docType attribute of the DleseCollectionFileIndexingWriter, which is 'dlesecollect.'
	 *
	 * @return    The docType, which is 'dlese_collect.'
	 */
	public String getDocType() {
		return metadataFormat;
	}


	/**
	 *  Gets the name of the concrete {@link org.dlese.dpc.index.reader.DocReader} class that is used to read
	 *  this type of {@link org.apache.lucene.document.Document}, which is "DleseCollectionDocReader".
	 *
	 * @return    The String "org.dlese.dpc.index.reader.DleseCollectionDocReader".
	 */
	public String getReaderClass() {
		return "org.dlese.dpc.index.reader.DleseCollectionDocReader";
	}


	/**
	 *  Returns the accession date or null if this collection is not currently accessioned.
	 *
	 * @return                The accession date or null
	 * @exception  Exception  This method should throw and Exception with appropriate error message if an error
	 *      occurs.
	 */
	protected Date getAccessionDate() throws Exception {
		if (!getAccessionStatus().equals("accessioned"))
			return null;
		Node dateElement = getDom4jDoc().selectSingleNode("/collectionRecord/approval/collectionStatuses/collectionStatus[@state='Accessioned']");
		if (dateElement != null)
			return MetadataUtils.parseUnionDateType(((Element) dateElement).attribute("date").getText());
		return null;
	}


	/**
	 *  Returns the date used to determine "What's new" in the library. Just returns the file mod date.
	 *
	 * @return                The what's new date for the item
	 * @exception  Exception  This method should throw and Exception with appropriate error message if an error
	 *      occurs.
	 */
	protected Date getWhatsNewDate() throws Exception {
		if (isMakingDeletedDoc())
			return new Date();
		else
			return new Date(getSourceFile().lastModified());
	}


	/**
	 *  Returns 'collection'.
	 *
	 * @return    The string 'collection'.
	 */
	protected String getWhatsNewType() {
		return "collection";
	}


	/**
	 *  Performs the necessary init functions (nothing done).
	 *
	 * @param  source         The source file being indexed
	 * @param  existingDoc    An existing Document that currently resides in the index for the given resource, or
	 *      null if none was previously present
	 * @exception  Exception  If an error occured during set-up.
	 */
	public void init(File source, Document existingDoc) throws Exception {

	}


	/**  This method is called at the conclusion of processing and may be used for tear-down. */
	protected void destroy() {

	}


	/**
	 *  Gets a report detailing any errors found in the XML validation of the collection record, or null if no
	 *  error was found.
	 *
	 * @return                Null if no data validation errors were found, otherwise a String that details the
	 *      nature of the error.
	 * @exception  Exception  If error in performing the validation.
	 */
	protected String getValidationReport() throws Exception {
		if (isMakingDeletedDoc())
			return XMLValidator.validateString(getFileContent());
		else
			return XMLValidator.validateFile(getSourceFile());
	}


	// --------------- Inherited methods --------------------

	/**
	 *  Default and stems fields handled here, so do not index full content.
	 *
	 * @return    False
	 */
	public boolean indexFullContentInDefaultAndStems() {
		return false;
	}


	/**
	 *  Adds fields to the index that are part of the collection-level Document.
	 *
	 * @param  newDoc         The new Document that is being created for this resource
	 * @param  existingDoc    An existing Document that currently resides in the index for the given resource, or
	 *      null if none was previously present
	 * @param  sourceFile     The sourceFile that is being indexed.
	 * @exception  Exception  If an error occurs
	 */
	protected final void addFields(Document newDoc, Document existingDoc, File sourceFile)
		 throws Exception {

		// The first time the index is built, the 'collect' DocReader is not available, so let RecordDataService know the collect ID:
		if (getRecordDataService() != null && getKey().equals("collect")) {
			getRecordDataService().setCollectCollectionID(getPrimaryId());
		}

		addToDefaultField(getDescription());

		String value;

		// Store but do not index the additional metadata content:
		value = getAdditionalMetadata();
		if (value != null)
			newDoc.add(new Field("collectionAdditionalMetadata", value, Field.Store.YES, Field.Index.NO));

		value = getFullTitle();
		if (value == null)
			value = "";
		newDoc.add(new Field("fulltitle", value, Field.Store.YES, Field.Index.ANALYZED));
		addToDefaultField(value);

		value = getShortTitle();
		if (value == null)
			value = "";
		newDoc.add(new Field("shorttitle", value, Field.Store.YES, Field.Index.ANALYZED));
		addToDefaultField(value);

		value = getPartOfDRC();
		if (value == null)
			value = "";
		newDoc.add(new Field("partofdrc", value, Field.Store.YES, Field.Index.ANALYZED));

		value = getFormatOfRecords();
		if (value == null)
			value = "";
		newDoc.add(new Field("formatofrecords", value, Field.Store.YES, Field.Index.ANALYZED));

		value = getAccessionStatus();
		if (value == null)
			value = "";
		newDoc.add(new Field("collaccessionstatus", value, Field.Store.YES, Field.Index.ANALYZED));

		Date accessionDate = getAccessionDate();
		if (accessionDate != null)
			newDoc.add(new Field("collaccessiondate", DateFieldTools.dateToString(accessionDate), Field.Store.YES, Field.Index.NOT_ANALYZED));

		value = this.getKey();
		if (value == null)
			value = "";
		newDoc.add(new Field("key", value, Field.Store.YES, Field.Index.ANALYZED));

		value = this.getCost();
		if (value == null)
			value = "";
		newDoc.add(new Field("cost", value, Field.Store.YES, Field.Index.ANALYZED));

		value = getScopeUrl();
		if (value == null)
			value = "";
		newDoc.add(new Field("scopeurl", value, Field.Store.YES, Field.Index.NOT_ANALYZED));
		// Then parse it for indexing
		newDoc.add(new Field("scopeuri", IndexingTools.tokenizeURI(value), Field.Store.NO, Field.Index.ANALYZED));
		addToDefaultField(IndexingTools.tokenizeURI(value));

		value = this.getReviewProcessUrl();
		if (value == null)
			value = "";
		newDoc.add(new Field("reviewprocessurl", value, Field.Store.YES, Field.Index.NOT_ANALYZED));
		// Then parse it for indexing
		newDoc.add(new Field("reviewprocessuri", IndexingTools.tokenizeURI(value), Field.Store.NO, Field.Index.ANALYZED));
		addToDefaultField(IndexingTools.tokenizeURI(value));

		value = this.getReviewProcess();
		if (value == null)
			value = "";
		newDoc.add(new Field("reviewprocess", value, Field.Store.YES, Field.Index.ANALYZED));

		newDoc.add(new Field(getFieldName("gradeRange", metadataFormat), getFieldContent(getGradeRanges(), "gradeRange", metadataFormat), Field.Store.YES, Field.Index.ANALYZED));

		String terms;
		newDoc.add(new Field(getFieldName("subject", metadataFormat), getFieldContent(getSubjects(), "subject", metadataFormat), Field.Store.YES, Field.Index.ANALYZED));
		terms = getTermStringFromStringArray(getSubjects());
		newDoc.add(new Field("subject", terms, Field.Store.YES, Field.Index.ANALYZED));
		addToDefaultField(terms);

		String keywords = getKeywords();
		if (keywords != null && keywords.trim().length() > 0)
			newDoc.add(new Field("keyword", keywords, Field.Store.YES, Field.Index.ANALYZED));
		addToDefaultField(keywords);

	}

	// --------------- Overridden methods from the super class ---------------

	/**
	 *  Creates a Lucene {@link org.apache.lucene.document.Document} from an existing CollectionFileIndexing
	 *  Document by setting the field "deleted" to "true" and making the modtime equal to current time.
	 *
	 * @param  existingDoc    An existing FileIndexingService Document that currently resides in the index for
	 *      the given resource.
	 * @return                A Lucene FileIndexingService Document with the field "deleted" set to "true" and
	 *      modtime set to current time.
	 * @exception  Throwable  Thrown if error occurs
	 */
	public synchronized Document getDeletedDoc_OFF_2006_08_23(Document existingDoc)
		 throws Throwable {
		if (existingDoc == null)
			throw new Exception("getDeletedDoc(): the existing doc is null");

		if (existingDoc.get("readerclass") == null || !existingDoc.get("readerclass").equals(getReaderClass()))
			return null;

		Document newDoc = new Document();

		return newDoc;
	}


	// --------------- Concrete methods ---------------


	/**
	 *  Gets the status of the collection based on the values in the collection-level record.
	 *
	 * @param  doc  A dlese_collect XML Document
	 * @return      The currentCollectionStatus value
	 */
	public final static String getCurrentCollectionStatus(org.dom4j.Document doc) {
		List nodes = doc.selectNodes("/*[local-name()='collectionRecord']/*[local-name()='approval']/*[local-name()='collectionStatuses']/*[local-name()='collectionStatus']");

		if (nodes == null || nodes.size() == 0)
			return "Error: no collectionStatus elements found in the XML";

		Collections.sort(nodes, new CollectionAccessionStatusComparator());

		Element collectionStatus = (Element) nodes.get(0);
		Attribute collecitonState = collectionStatus.attribute("state");

		return collecitonState.getText();
	}


	/**
	 *  Allows sorting of a Collection accession status XML Node, by date giving precedence to status =
	 *  accessioned if dates are equal.
	 *
	 * @author    John Weatherley
	 */
	public static class CollectionAccessionStatusComparator implements Comparator {

		/**
		 *  Compares two collection accession XML nodes for sorting.
		 *
		 * @param  O1                      A collectionStatus dom4j XML Node from a dlese_collect XML Document.
		 * @param  O2                      A collectionStatus dom4j XML Node from a dlese_collect XML Document.
		 * @return                         Returns a negative integer, zero, or a positive integer as the first
		 *      argument is less than, equal to, or greater than the second.
		 * @exception  ClassCastException  If error.
		 */
		public int compare(Object O1, Object O2)
			 throws ClassCastException {
			Date dateOne;
			Date dateTwo;
			try {
				dateOne = MetadataUtils.parseUnionDateType(((Element) O1).attribute("date").getText());
				dateTwo = MetadataUtils.parseUnionDateType(((Element) O2).attribute("date").getText());
			} catch (ParseException e) {
				System.err.println("Error: unable to parse collection status date: " + e.getMessage());
				return 0;
			}
			if (dateOne.equals(dateTwo)) {
				// Compare the status by precedence to see which one should be returned first:
				String statusOne = ((Element) O1).attribute("state").getText();
				String statusTwo = ((Element) O2).attribute("state").getText();
				if (statusOne.equalsIgnoreCase(statusTwo))
					return 0;
				if (statusOne.equalsIgnoreCase("Accessioned"))
					return -1;
				else if (statusTwo.equalsIgnoreCase("Accessioned"))
					return 1;
				else
					return 0;
			}
			else
				return dateTwo.compareTo(dateOne);
		}
	}

}

