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

import org.apache.lucene.document.*;
import org.apache.lucene.search.*;
import org.apache.lucene.index.*;
import org.apache.lucene.analysis.KeywordAnalyzer;

import org.dlese.dpc.xml.*;
import org.dlese.dpc.index.*;
import org.dlese.dpc.index.reader.*;
import org.dlese.dpc.util.*;
import org.dlese.dpc.repository.*;
import org.dlese.dpc.vocab.*;
import org.dlese.dpc.index.document.DateFieldTools;
import org.dlese.dpc.index.writer.xml.*;

/**
 *  Creates a Lucene {@link org.apache.lucene.document.Document} from any XML file by stripping the XML tags
 *  to extract and index the content. The reader for this type of Document is XMLDocReader.<p>
 *
 *  <b>The Lucene Document fields that are created by this class are (in addition the the ones listed for
 *  {@link org.dlese.dpc.index.writer.FileIndexingServiceWriter}):</b> <br>
 *  <br>
 *  <code><b>collection</b> </code> - The collection associated with this resource. <br>
 *
 *
 * @author    John Weatherley
 * @see       org.dlese.dpc.index.FileIndexingService
 * @see       org.dlese.dpc.index.reader.XMLDocReader
 */
public abstract class XMLFileIndexingWriter extends FileIndexingServiceWriter {

	private File sourceFile = null;
	private DleseCollectionDocReader myCollectionDocReader = null;
	private XMLIndexer _xmlIndexer = null;
	private String[] _collections = null;
	private ResultDocList _myAnnoResultDocs = null;

	/**  Constructor for the XMLFileIndexingWriter. */
	public XMLFileIndexingWriter() { }


	/**
	 *  Returns the ids for the item being indexed. If more than one record catalogs the same item, this
	 *  represents the primary ID.
	 *
	 * @return                The id String
	 * @exception  Exception  If error
	 * @see                   #getIds
	 */
	public String[] getIds() throws Exception {
		return getXmlIndexer().getIds();
	}


	/**
	 *  Returns the unique primary record ID for the item being indexed. If more than one record catalogs the
	 *  same item, this represents the primary ID.
	 *
	 * @return                The id String
	 * @exception  Exception  If error
	 * @see                   #getIds
	 */
	public String getPrimaryId() throws Exception {
		if (getIds() == null || getIds().length == 0)
			return null;
		return getIds()[0];
	}


	/**
	 *  Gets the ids of related records.
	 *
	 * @return                            The related ids value, or null if none
	 * @exception  IllegalStateException  If called prior to calling method #indexFields
	 * @exception  Exception              If error
	 */
	public List getRelatedIds() throws IllegalStateException, Exception {
		return getXmlIndexer().getRelatedIds();
	}


	/**
	 *  Gets the urls of related records.
	 *
	 * @return                            The related urls value, or null if none
	 * @exception  IllegalStateException  If called prior to calling method #indexFields
	 * @exception  Exception              If error
	 */
	public List getRelatedUrls() throws IllegalStateException, Exception {
		return getXmlIndexer().getRelatedUrls();
	}

	
	/**
	 *  Gets the ids of related records. The Map key contains the relationship (isAnnotatedBy, etc.) and the Map
	 *  value contains a List of Strings that indicate the ids of the target records.
	 *
	 * @return                            The related ids value, or null if none
	 * @exception  IllegalStateException  If called prior to calling method #indexFields
	 * @exception  Exception              If error
	 */
	public Map getRelatedIdsMap() throws IllegalStateException, Exception {
		return getXmlIndexer().getRelatedIdsMap();
	}


	/**
	 *  Gets the urls of related records. The Map key contains the relationship (isAnnotatedBy, etc.) and the Map
	 *  value contains a List of Strings that indicate the urls of the target records.
	 *
	 * @return                            The related urls value, or null if none
	 * @exception  IllegalStateException  If called prior to calling method #indexFields
	 * @exception  Exception              If error
	 */
	public Map getRelatedUrlsMap() throws IllegalStateException, Exception {
		return getXmlIndexer().getRelatedUrlsMap();
	}	
	

	/**
	 *  Returns unique collection keys for the item being indexed. For example "dcc" (single collection) or "dcc
	 *  dwel" (multiple collections). If more than one collection is provided, the first one must be the primary
	 *  collection. May be overridden by sub-classes as appropriate (overridden by ADNFileIndexingWriter).
	 *
	 * @return                The collection keys
	 * @exception  Exception  This method should throw and Exception with appropriate error message if an error
	 *      occurs.
	 */
	protected String[] getCollections() throws Exception {
		if (_collections == null) {
			Map configAttr = getConfigAttributes();
			if (configAttr == null)
				return null;
			_collections = ((String) configAttr.get("collection")).split(" ");
		}
		return _collections;
	}


	/**
	 *  Gets the collection specifier, for example 'dcc', 'comet'.
	 *
	 * @return                The collection specifier
	 * @exception  Exception  If error occured
	 */
	public String getDocGroup() throws Exception {
		return getCollections()[0];
	}


	/**
	 *  Return the geospatial BoundingBox footprint that represnets the resource being indexed, or null if none
	 *  apply. Override if nessary.
	 *
	 * @return                BoundingBox, or null
	 * @exception  Exception  This method should throw and Exception with appropriate error message if an error
	 *      occurs.
	 */
	protected BoundingBox getBoundingBox() throws Exception {
		return null;
	}

	// --------------- Abstract methods for subclasses --------------------

	/**
	 *  This method is called prior to processing and may be used to for any necessary set-up. This method should
	 *  throw and exception with appropriate message if an error occurs.
	 *
	 * @param  source         The source file being indexed
	 * @param  existingDoc    An existing Document that currently resides in the index for the given resource, or
	 *      null if none was previously present
	 * @exception  Exception  If an error occured during set-up.
	 */
	public abstract void init(File source, Document existingDoc) throws Exception;


	/**
	 *  Return unique IDs for the item being indexed, one for each collection that catalogs the resource. For
	 *  example "DLESE-000-000-000-001" (single ID) or "DLESE-000-000-000-036 COMET-60" (multiple IDs). If more
	 *  than one ID is present, the first one is the primary.
	 *
	 * @return                The id(s)
	 * @exception  Exception  This method should throw and Exception with appropriate error message if an error
	 *      occurs.
	 */
	protected abstract String[] _getIds() throws Exception;


	/**
	 *  Return a title for the document being indexed, or null if none applies. The String is tokenized, stored
	 *  and indexed under the field key 'title' and is also indexed in the 'default' field.
	 *
	 * @return                The title String
	 * @exception  Exception  This method should throw and Exception with appropriate error message if an error
	 *      occurs.
	 */
	public abstract String getTitle() throws Exception;


	/**
	 *  Return a description for the document being indexed, or null if none applies. The String is tokenized,
	 *  stored and indexed under the field key 'description' and is also indexed in the 'default' field.
	 *
	 * @return                The description String
	 * @exception  Exception  This method should throw and Exception with appropriate error message if an error
	 *      occurs.
	 */
	public abstract String getDescription() throws Exception;


	/**
	 *  Return the URL(s) to the resource being indexed, or null if none apply. If more than one URL references
	 *  the resource, the first one is the primary. The URL Strings are tokenized and indexed under the field key
	 *  'uri' and is also indexed in the 'default' field. It is also stored in the index untokenized under the
	 *  field key 'url.'
	 *
	 * @return                The url String(s)
	 * @exception  Exception  This method should throw and Exception with appropriate error message if an error
	 *      occurs.
	 */
	public abstract String[] getUrls() throws Exception;


	/**
	 *  Return true to have the full XML content indexed in the 'default' and 'stems' fields, false if handled by
	 *  the sub-class. If true, the content is indexed using the #addToDefaultField method.
	 *
	 * @return    True to have the full XML content indexed in the 'default' and 'stems'
	 */
	public abstract boolean indexFullContentInDefaultAndStems();


	/**
	 *  Returns the date used to determine "What's new" in the library, or null if none is available.
	 *
	 * @return                The what's new date for the item or null if not available.
	 * @exception  Exception  This method should throw and Exception with appropriate error message if an error
	 *      occurs.
	 */
	protected abstract Date getWhatsNewDate() throws Exception;


	/**
	 *  Returns the type of category for "What's new" in the library, or null if none is available. Must be a
	 *  simple lower case String with no spaces, for example 'itemnew,' 'itemannocomplete,' 'itemannoinprogress,'
	 *  'annocomplete,' 'annoinprogress,' 'collection'.
	 *
	 * @return                The what's new type.
	 * @exception  Exception  This method should throw and Exception with appropriate error message if an error
	 *      occurs.
	 */
	protected abstract String getWhatsNewType() throws Exception;


	/**
	 *  Adds additional fields that are unique the document format being indexed. When implementing this method,
	 *  use the add method of the {@link org.apache.lucene.document.Document} class to add a {@link
	 *  org.apache.lucene.document.Field}.<p>
	 *
	 *  The following Lucene {@link org.apache.lucene.document.Field} types are available for indexing with the
	 *  {@link org.apache.lucene.document.Document}:<br>
	 *  Field.Text(string name, string value) -- tokenized, indexed, stored<br>
	 *  Field.UnStored(string name, string value) -- tokenized, indexed, not stored <br>
	 *  Field.Keyword(string name, string value) -- not tokenized, indexed, stored <br>
	 *  Field.UnIndexed(string name, string value) -- not tokenized, not indexed, stored<br>
	 *  Field(String name, String string, boolean store, boolean index, boolean tokenize) -- allows control to do
	 *  anything you want<p>
	 *
	 *  Example code:<br>
	 *  <code>protected void addCustomFields(Document newDoc, Document existingDoc) throws Exception {</code>
	 *  <br>
	 *  &nbsp;<code> String customContent = "Some content";</code><br>
	 *  &nbsp;<code> newDoc.add(Field.Text("mycustomefield", customContent));</code> <br>
	 *  <code>}</code>
	 *
	 * @param  newDoc         The new {@link org.apache.lucene.document.Document} that is being created for this
	 *      resource
	 * @param  existingDoc    An existing {@link org.apache.lucene.document.Document} that currently resides in
	 *      the index for the given resource, or null if none was previously present
	 * @param  sourceFile     The sourceFile that is being indexed
	 * @exception  Exception  This method should throw and Exception with appropriate error message if an error
	 *      occurs.
	 */
	protected abstract void addFields(Document newDoc, Document existingDoc, File sourceFile) throws Exception;


	// --------------- Concrete methods --------------------

	/**
	 *  Adds the full content of the XML to the default search field. Strips the XML tags to extract the content.
	 *  Will not work properly if the XML is not well-formed.<p>
	 *
	 *
	 *
	 * @param  newDoc         The new {@link org.apache.lucene.document.Document} that is being created for this
	 *      resource
	 * @param  existingDoc    An existing {@link org.apache.lucene.document.Document} that currently resides in
	 *      the index for the given resource, or null if none was previously present
	 * @param  sourceFile     The feature to be added to the CustomFields attribute
	 * @exception  Exception  This method should throw and Exception with appropriate error message if an error
	 *      occurs.
	 */
	protected void addCustomFields(Document newDoc, Document existingDoc, File sourceFile) throws Exception {
		RecordDataService recordDataService = getRecordDataService();
		MetadataVocab vocab = null;
		if (recordDataService != null)
			vocab = recordDataService.getVocab();

		// ------ Standard XML indexing handled by XMLIndexer ------------

		// Index all xPath and custom fields
		XMLIndexer xmlIndexer = getXmlIndexer();

		// Set up the default values to index for the standard fields:

		// Get the ids provided by sub-class:
		String[] providedIds = _getIds();

		// If no ID provided by sub-class, use the file name: simply remove the ".xml" from the end of the filename to get the ID.
		if (providedIds == null || providedIds.length == 0) {
			String fileName = getSourceFile().getName();
			providedIds = new String[]{Files.decode(fileName.substring(0, (fileName.length() - 4)))};
		}
		xmlIndexer.setIds(providedIds);
		xmlIndexer.setUrls(getUrls());
		xmlIndexer.setTitle(getTitle());
		xmlIndexer.setDescription(getDescription());
		try {
			xmlIndexer.setBoundingBox(getBoundingBox());
		} catch (Throwable e) {
			prtlnErr("Unable to index Bounding Box coordinates: " + e.getMessage());
		}

		// If indicated by sub-class, index the 'default', 'admindefault', and 'stems' fields with the full XML content (Note: This performs the addToDefaultField and addToAdminDefaultField):
		xmlIndexer.setIndexDefaultAndStemsField(indexFullContentInDefaultAndStems());

		// Perform the XML indexing...:
		xmlIndexer.indexFields(newDoc);

		// Remove old docs for those we're adding:
		String[] idsEncoded = xmlIndexer.getIdsEncoded();
		if (idsEncoded != null) {
			for (int i = 0; i < idsEncoded.length; i++) {
				// Remove all IDs in the index that are the same as this one
				addDocToRemove("id", idsEncoded[i]);
			}
		}

		// --- Index things this item relates to (is an annotation for, etc), e.g. isRelatedTo:

		boolean itemAssignsRelationships = false;

		// Index the related IDs for this item:
		Map relatedIdsMap = xmlIndexer.getRelatedIdsMap();
		//prtln("xmlIndexer.getRelatedIds()");
		if (relatedIdsMap != null) {
			//prtln("xmlIndexer.getRelatedIds() has some!");
			Iterator it = relatedIdsMap.keySet().iterator();
			while (it.hasNext()) {
				String relationshipName = (String) it.next();
				List ids = (List) relatedIdsMap.get(relationshipName);
				//prtln("processing id relation: relationshipName: " + relationshipName + " ids: " + Arrays.toString(ids.toArray()));

				// Index the IDs so these docs can be retrieved later:
				for (int i = 0; i < ids.size(); i++) {
					itemAssignsRelationships = true;
					//newDoc.add(new Field("indexedRelationIds.isRelatedTo", ids.get(i).toString(), Field.Store.YES, Field.Index.NOT_ANALYZED));
					newDoc.add(new Field("assignsRelationshipById." + relationshipName, ids.get(i).toString(), Field.Store.YES, Field.Index.NOT_ANALYZED));
					newDoc.add(new Field("assignsRelationshipById", ids.get(i).toString(), Field.Store.YES, Field.Index.NOT_ANALYZED));
				}
				//newDoc.add(new Field("indexedRelations", "isRelatedTo", Field.Store.YES, Field.Index.NOT_ANALYZED));
				newDoc.add(new Field("assignedRelationshipsById", relationshipName, Field.Store.YES, Field.Index.NOT_ANALYZED));
			}
		}
		else {
			//prtln("xmlIndexer.getRelatedIds() has no related IDs!");
		}

		// Index the related urls for this item:
		Map relatedUrlsMap = xmlIndexer.getRelatedUrlsMap();
		if (relatedUrlsMap != null) {
			Iterator it = relatedUrlsMap.keySet().iterator();
			while (it.hasNext()) {
				String relationshipName = (String) it.next();
				List urls = (List) relatedUrlsMap.get(relationshipName);
				//prtln("processing url relation: relationshipName: " + relationshipName + " urls: " + Arrays.toString(ids.toArray()));

				// Index the IDs so these docs can be retrieved later:
				for (int i = 0; i < urls.size(); i++) {
					itemAssignsRelationships = true;
					//newDoc.add(new Field("indexedRelationIds.isRelatedTo", ids.get(i).toString(), Field.Store.YES, Field.Index.NOT_ANALYZED));
					newDoc.add(new Field("assignsRelationshipByUrl." + relationshipName, urls.get(i).toString(), Field.Store.YES, Field.Index.NOT_ANALYZED));
					newDoc.add(new Field("assignsRelationshipByUrl", urls.get(i).toString(), Field.Store.YES, Field.Index.NOT_ANALYZED));
				}
				//newDoc.add(new Field("indexedRelations", "isRelatedTo", Field.Store.YES, Field.Index.NOT_ANALYZED));
				newDoc.add(new Field("assignedRelationshipsByUrl", relationshipName, Field.Store.YES, Field.Index.NOT_ANALYZED));
			}
		}

		// Mark if this item assigns a relationship:
		newDoc.add(new Field("assignedRelationshipIsDefined", (itemAssignsRelationships ? "true" : "false"), Field.Store.YES, Field.Index.NOT_ANALYZED));
		
		// JW - Should we assitn the "isRelatedTo" relationship to this?
		if(itemAssignsRelationships) {
			//luceneDoc.add(new Field("indexedRelations", "isRelatedTo", Field.Store.YES, Field.Index.NOT_ANALYZED));	
		}

		// ------ [end] Standard XML indexing handled by XMLIndexer ------------


		// ------ Index relations for this item (records that have a relation to this (isAnnotatedBy, etc.) ------------

		// The new way to handle relations....
		indexRelations(newDoc);

		// Index the annotations as a standard relation:
		//indexRelation(myAnnoResultDocs,"isAnnotatedBy",newDoc);

		// To do: Implement support for other configurable relations types...



		// ------ [end] Index relations for this item ------------


		// ----------- Annotations for this item ------------------

		// Note: See some related index fields applied in ItemFileIndexingWriter

		ResultDocList myAnnoResultDocs = getMyAnnoResultDocs();

		// Add anno fields only available if the RecordDataService is avail:
		if (recordDataService != null) {

			String fieldContent = null;
			List fieldList = null;

			// Flag anno
			if (myAnnoResultDocs != null && myAnnoResultDocs.size() > 0)
				newDoc.add(new Field("itemhasanno", "true", Field.Store.YES, Field.Index.ANALYZED));
			else
				newDoc.add(new Field("itemhasanno", "false", Field.Store.YES, Field.Index.ANALYZED));

			// Anno types
			fieldContent = "";
			fieldList = recordDataService.getAnnoTypesFromResultDocs(myAnnoResultDocs);
			if (fieldList != null && fieldList.size() > 0) {
				for (int i = 0; i < fieldList.size(); i++)
					fieldContent += ((String) fieldList.get(i)).replaceAll(" ", "+") + " ";
				newDoc.add(new Field("itemannotypes", fieldContent, Field.Store.YES, Field.Index.ANALYZED));
			}

			// Anno pathways
			fieldContent = "";
			fieldList = recordDataService.getAnnoPathwaysFromResultDocs(myAnnoResultDocs);
			if (fieldList != null && fieldList.size() > 0) {
				for (int i = 0; i < fieldList.size(); i++)
					fieldContent += ((String) fieldList.get(i)).replaceAll(" ", "+") + " ";
				newDoc.add(new Field("itemannopathways", fieldContent, Field.Store.YES, Field.Index.ANALYZED));
			}

			// Anno collection keys, e.g. {06, 09}
			fieldContent = "";
			fieldList = recordDataService.getCollectionKeysFromResultDocs(myAnnoResultDocs);
			if (fieldList != null && fieldList.size() > 0) {
				for (int i = 0; i < fieldList.size(); i++) {
					fieldContent += (String) fieldList.get(i);
					if (i < (fieldList.size() - 1))
						fieldContent += "+";
				}
				//prtln("itemannocollectionkeys for " + this.getId() + " is: " + fieldContent);
				newDoc.add(new Field("itemannocollectionkeys", fieldContent, Field.Store.YES, Field.Index.ANALYZED));
			}

			// Anno collection keys e.g. {06, 09} for those with status completed only
			fieldContent = "";
			ArrayList completedAnnoCollectionKeys
				 = recordDataService.getCompletedAnnoCollectionKeysFromResultDocs(myAnnoResultDocs);
			if (completedAnnoCollectionKeys != null && completedAnnoCollectionKeys.size() > 0) {
				for (int i = 0; i < completedAnnoCollectionKeys.size(); i++) {
					fieldContent += (String) completedAnnoCollectionKeys.get(i);
					if (i < (completedAnnoCollectionKeys.size() - 1))
						fieldContent += "+";
				}
				//prtln("itemannocompletedcollectionkeys for " + this.getId() + " is: " + fieldContent);
				newDoc.add(new Field("itemannocompletedcollectionkeys", fieldContent, Field.Store.YES, Field.Index.ANALYZED));
			}

			// Anno status
			fieldContent = "";
			fieldList = recordDataService.getAnnoStatusFromResultDocs(myAnnoResultDocs);
			if (fieldList != null && fieldList.size() > 0) {
				for (int i = 0; i < fieldList.size(); i++)
					fieldContent += ((String) fieldList.get(i)).replaceAll(" ", "+") + " ";
				newDoc.add(new Field("itemannostatus", fieldContent, Field.Store.YES, Field.Index.ANALYZED));
			}

			// Anno formats
			fieldContent = "";
			fieldList = recordDataService.getAnnoFormatsFromResultDocs(myAnnoResultDocs);
			if (fieldList != null && fieldList.size() > 0) {
				for (int i = 0; i < fieldList.size(); i++)
					fieldContent += ((String) fieldList.get(i)) + " ";
				newDoc.add(new Field("itemannoformats", fieldContent, Field.Store.YES, Field.Index.ANALYZED));
			}

			// Anno rating information and statistics
			indexAnnoRatings(myAnnoResultDocs, newDoc);

		}
		// If no record data service, mark the item as having no annos
		else {
			newDoc.add(new Field("itemhasanno", "false", Field.Store.YES, Field.Index.ANALYZED));
		}

		// ----------- [end] Annotations for this item ------------------

		// ----------- Global fields for all XML records and sub-class handlers -------------

		//prtln("Adding index fields for ID: " + getPrimaryId());

		String[] collections = getCollections();
		// Add my collection and collectionKey
		if (collections != null && collections.length > 0) {
			String colStg = "";
			for (int i = 0; i < collections.length; i++) {
				colStg += " 0" + collections[i];
			}

			colStg = colStg.trim();
			newDoc.add(new Field("collection", colStg, Field.Store.YES, Field.Index.ANALYZED));
			if (!getCollections()[0].equals("configuredcollections")) {
				newDoc.add(new Field(getFieldName("key", "dlese_collect"), getFieldContent(collections, "key", "dlese_collect"), Field.Store.YES, Field.Index.ANALYZED));
			}
		}

		// Store the ID for the collection I am a member of. (The first time the index is built, the DocReader for the 'collect' collection is not available):
		String key = getCollections()[0];
		String myCollectionRecordIdValue = null;
		DleseCollectionDocReader dleseCollectionDocReader = getMyCollectionDoc();
		if (dleseCollectionDocReader != null)
			myCollectionRecordIdValue = dleseCollectionDocReader.getId();
		else if (recordDataService != null && recordDataService.getCollectCollectionID() != null)
			myCollectionRecordIdValue = recordDataService.getCollectCollectionID();
		else if (key != null && key.equals("collect"))
			myCollectionRecordIdValue = "ID-FOR-COLLECT-NOT-YET-AVAILABLE";

		// If no collection info (such as jOAI).
		if (myCollectionRecordIdValue == null)
			myCollectionRecordIdValue = "COLLECTION-ID-NOT-AVAILABLE";

		newDoc.add(new Field("myCollectionRecordIdValue", myCollectionRecordIdValue, Field.Store.YES, Field.Index.NO));

		newDoc.add(new Field("metadatapfx", '0' + getDocType(), Field.Store.YES, Field.Index.NOT_ANALYZED));
		newDoc.add(new Field("xmlFormat", getDocType(), Field.Store.YES, Field.Index.NOT_ANALYZED));

		String oaimodtime = getOaiModtime(sourceFile, existingDoc);
		if (oaimodtime != null)
			newDoc.add(new Field("oaimodtime", oaimodtime, Field.Store.YES, Field.Index.NOT_ANALYZED));

		// ---------- Handle all required metadata fields from sub-classes ------

		// What's new date and time
		Date wndate = getWhatsNewDate();
		if (wndate != null)
			newDoc.add(new Field("wndate", DateFieldTools.dateToString(wndate), Field.Store.YES, Field.Index.NOT_ANALYZED));
		String wntype = getWhatsNewType();
		if (wntype != null)
			newDoc.add(new Field("wntype", wntype, Field.Store.YES, Field.Index.ANALYZED));

		// ------ Check that this record does not already exist in the repository, throw ErrorDocException if needed ------
		if (!isMakingDeletedDoc()) {
			// Get the ids for this XML document (used below):
			String[] ids = getIds();

			ErrorDocException errorDocException = null;
			if (ids != null && ids.length > 0) {

				for (int i = 0; i < ids.length; i++) {

					// Verify that this record's ID (the first ID) does not already exist in the repository:
					if (i == 0) {

						File recordIdAlreadyInRepository = null;

						// Check for duplicate IDs in the repository (not de-duping, but actual dup IDs), if not temp file for checking:
						if (!getSourceFile().getName().equals("temp_dlese_repository_put_record_file.xml")) {

							// Check if another file with the same ID exists in the index:
							if (recordIdAlreadyInRepository == null) {
								BooleanQuery idQ = new BooleanQuery();
								idQ.add(new TermQuery(new Term("id", idsEncoded[i])), BooleanClause.Occur.MUST);
								idQ.add(new TermQuery(new Term("multirecord", "true")), BooleanClause.Occur.MUST_NOT);
								idQ.add(new TermQuery(new Term("docsource", getDocsource())), BooleanClause.Occur.MUST_NOT);
								idQ.add(new TermQuery(new Term("deleted", "false")), BooleanClause.Occur.MUST);

								ResultDocList results = getIndex().searchDocs(idQ);
								if (results != null && results.size() > 0) {
									//prtln("Found " + results.length + " potential dup ID(s)");
									DocReader reader = ((ResultDoc)results.get(0)).getDocReader();
									if (reader instanceof XMLDocReader) {
										XMLDocReader xmlDocReader = (XMLDocReader) reader;
										if (xmlDocReader.getId().equals(ids[i])) {
											recordIdAlreadyInRepository = xmlDocReader.getFile();
											//prtln("found dup ID in index: " + getIds()[i]);
											//prtln("Dup ID matched for ID '" + getIds()[i] + "'");
										}
										else {
											//prtln("no dup ID in index: " + getIds()[i]);
											//prtln("Dup ID not matched for ID '" + getIds()[i] + "' found instead '" + xmlDocReader.getId() + "'");
										}
									}
								}
							}

							// Check if another file with the same ID exists in the current indexing session:
							if (recordIdAlreadyInRepository == null) {
								HashMap sessionIDs = null;
								HashMap sessionAttributes = getSessionAttributes();
								if (sessionAttributes != null) {
									sessionIDs = (HashMap) sessionAttributes.get("sessionIDs");
									if (sessionIDs == null) {
										sessionIDs = new HashMap();
										sessionAttributes.put("sessionIDs", sessionIDs);
									}

									// If another file in the directory has the same ID, throw error...
									File fileInDirectory = (File) sessionIDs.get(idsEncoded[i]);
									if (fileInDirectory != null && getSourceFile().exists() && fileInDirectory.exists()) {
										//prtln("found dup ID in same dir: " + getIds()[i] + " dup path: " + fileInDirectory.getAbsolutePath() + " my path: " + this.getSourceFile().getAbsolutePath());
										recordIdAlreadyInRepository = fileInDirectory;
									}
									else {
										//prtln("no dup ID in same dir: " + getIds()[i]);
										//prtln("sessionIDs.put: " + getSourceFile());
										sessionIDs.put(idsEncoded[i], getSourceFile());
									}
								}
							}

							// If this ID is already in the repository, throw an Exception and store the dup info:
							if (recordIdAlreadyInRepository != null) {
								errorDocException = makeErrorDocException(getIds()[i], idsEncoded[i], recordIdAlreadyInRepository);
							}
							else {
								//prtln("Not making an error doc for: " + ids[i]);
							}
						}
					}
				}
			}

			/* 			// Remove any IDs in the index that are the same as this one
			if(encodedIdsString != null) {
				addDocToRemove("id", encodedIdsString.trim());
			} */
			// Throw if we've got an exception
			if (errorDocException != null)
				throw errorDocException;
		}

		// Grab fields from sub-classes.
		addFields(newDoc, existingDoc, sourceFile);
	}


	private void indexRelations(Document luceneDoc) throws Exception {
		boolean hasIndexedRelation = false;
		
		// Get all the records that are related to me:
		String[] myIds = getIds();
		String[] myUrls = getUrls();
		if ( ((myIds == null || myIds.length == 0) && (myUrls == null || myUrls.length == 0)) || getIndex() == null) {
			return;
		}
		try {
			
			BooleanQuery idQ = new BooleanQuery();
			if(myIds != null) {
				for (int i = 0; i < myIds.length; i++)
					idQ.add(new TermQuery(new Term("assignsRelationshipById", myIds[i])), BooleanClause.Occur.SHOULD);
			}
			
			if(myUrls != null) {
				for (int i = 0; i < myUrls.length; i++)
					idQ.add(new TermQuery(new Term("assignsRelationshipByUrl", myUrls[i])), BooleanClause.Occur.SHOULD);
			}
			
			ResultDocList relatedDocs =
				getIndex().searchDocs(idQ);
			if (relatedDocs == null || relatedDocs.size() == 0) {
				//prtln("indexRelations(): " + idQ + " num: 0");
				return;
			}
			else {
				//prtln("indexRelations(): " + idQ + " num: " + relatedDocs.size());

				//Index my relations...
				//List relatedIds = new ArrayList();
				for (int i = 0; i < relatedDocs.size(); i++) {
					XMLDocReader xmlDocReader = (XMLDocReader)relatedDocs.get(i).getDocReader();
					
					// Get the list of relations assigned for this
					List myRelationTypes = (List)xmlDocReader.getAssignedRelationshipsForItemsMap().get(getPrimaryId());
					
					//prtln("indexRelations() for id: " + getPrimaryId() + " myRelationTypes:" + (myRelationTypes == null ? " null" : Arrays.toString(myRelationTypes.toArray())));
					
					if(myRelationTypes != null) {
						for(int j = 0; j < myRelationTypes.size(); j++) { 
							String relationType = (String)myRelationTypes.get(j);
		
							// Index all xPaths for this item
							XMLIndexer xmlIndexer = new XMLIndexer(xmlDocReader.getXml(), xmlDocReader.getDoctype(), getXmlIndexerFieldsConfig());
							xmlIndexer.setXPathFieldsPrefix("/relation." + relationType + "/");
							
							// Add the related records text to this one's default fields?
							xmlIndexer.setIndexDefaultAndStemsField(true);
		
							// Index just the XPath fields:
							xmlIndexer.indexXpathFields(luceneDoc);
							//relatedIds.add(xmlDocReader.getId());
							
							// Index the IDs so these docs can be retrieved later:
							luceneDoc.add(new Field("indexedRelationIds." + relationType, xmlDocReader.getId(), Field.Store.YES, Field.Index.NOT_ANALYZED));
							luceneDoc.add(new Field("indexedRelations", relationType, Field.Store.YES, Field.Index.NOT_ANALYZED));
							hasIndexedRelation = true;
						}							
					}
				}
			}	
			
		} catch (Throwable e) {
			prtlnErr("indexRelations(): " + e);
			e.printStackTrace();
			return;
		} finally {
			luceneDoc.add(new Field("hasIndexedRelation", (hasIndexedRelation ? "true" : "false"), Field.Store.YES, Field.Index.NOT_ANALYZED));	
		}
	}


	/**
	 *  Indexes a relation for this item.
	 *
	 * @param  relatedDocs    An array of ResultDocs that contain the records that are related to this one
	 * @param  luceneDoc      The Document to add the fields to
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
/* 	private void indexRelationZZZ(ResultDoc[] relatedDocs, Document luceneDoc) throws Exception {
		if (relatedDocs != null && relatedDocs.length > 0) {
			List relatedIds = new ArrayList();
			for (int i = 0; i < relatedDocs.length; i++) {
				XMLDocReader xmlDocReader = (XMLDocReader) (relatedDocs[i].getDocReader());

				String relationType = "isAnnotatedBy";

				// Index all xPaths for this item
				XMLIndexer xmlIndexer = new XMLIndexer(xmlDocReader.getXml(), xmlDocReader.getDoctype(), getXmlIndexerFieldsConfig());
				xmlIndexer.setXPathFieldsPrefix("/relation." + relationType + "/");

				// Index just the XPath fields:
				xmlIndexer.indexXpathFields(luceneDoc);
				relatedIds.add(xmlDocReader.getId());
			}

			// Index the IDs so these docs can be retrieved later:
			for (int i = 0; i < relatedIds.size(); i++) {
				luceneDoc.add(new Field("indexedRelationIds." + relationType, relatedIds.get(i).toString(), Field.Store.YES, Field.Index.NOT_ANALYZED));
			}

			luceneDoc.add(new Field("indexedRelations", relationType, Field.Store.YES, Field.Index.NOT_ANALYZED));

			itemHasRelations = true;
		}
	} */


	/**
	 *  Indexes the annotation rating information and tabulated statistics.
	 *
	 * @param  annoResultDocs  An array of anno ResultDocs
	 * @param  newDoc          The Document to add the fields to
	 */
	private void indexAnnoRatings(ResultDocList annoResultDocs, Document newDoc) {

		float numRatings = 0;
		float totalRating = 0;
		String ratings = null;
		if (annoResultDocs != null && annoResultDocs.size() > 0) {
			ratings = "";
			for (int i = 0; i < annoResultDocs.size(); i++) {
				String rating = ((DleseAnnoDocReader) annoResultDocs.get(i).getDocReader()).getRating();
				if (rating != null && rating.length() > 0) {
					try {
						totalRating += Float.parseFloat(rating);
						numRatings++;
						ratings += rating + " ";
					} catch (Exception nfe) {}
				}
			}
		}

		// The total number of ratings assigned to this resource
		newDoc.add(new Field("itemannonumratings", new DecimalFormat("00000").format(numRatings), Field.Store.YES, Field.Index.NOT_ANALYZED));

		// A String of all the ratings assigned to this resource, as numbers (e.g. '1 1 2 4 2 3')
		if (ratings != null && ratings.length() > 0)
			newDoc.add(new Field("itemannoratingvalues", ratings, Field.Store.YES, Field.Index.ANALYZED));

		// The average rating for this resource
		if (numRatings > 0 || totalRating > 0) {
			float aveRating = (totalRating / numRatings);

			NumberFormat formatter = new DecimalFormat("0.000");
			//prtln("ave rating: " + aveRating + " string: " + formatter.format(aveRating));
			newDoc.add(new Field("itemannoaveragerating", formatter.format(aveRating), Field.Store.YES, Field.Index.NOT_ANALYZED));
		}
	}



	private ErrorDocException makeErrorDocException(String id,
	                                                String encodedId,
	                                                File recordIdAlreadyInRepository) {
		ErrorDocException errorDocException =
			new ErrorDocException("The ID '" + id + "' that was found in this file is the same as the ID already found in file '" + recordIdAlreadyInRepository + "'", "dupIdError");
		errorDocException.putKeywordField("duplicateIdValue", id);
		errorDocException.putTextField("duplicateIdEnc", encodedId);
		errorDocException.putKeywordField("duplicateIdDocsource", recordIdAlreadyInRepository.getAbsolutePath());
		//prtln("Throwing an ErrorDocException for dup ID: " + id + " path: " + recordIdAlreadyInRepository.getAbsolutePath());
		return errorDocException;
	}


	/**
	 *  Creates a Lucene Document for the XML that is equal to the exsiting Document.
	 *
	 * @param  existingDoc    An existing FileIndexingService Document that currently resides in the index for
	 *      the given file
	 * @return                A Lucene FileIndexingService Document
	 * @exception  Throwable  Thrown if error occurs
	 */
	public synchronized Document getDeletedDoc(Document existingDoc)
		 throws Throwable {
		existingDoc = super.getDeletedDoc(existingDoc);

		Document newDocument = null;
		try {
			if (existingDoc == null) {
				//prtln("existingDoc is null!");
			}
			ResultDocConfig resultDocConfig = new ResultDocConfig(getIndex());
			ResultDoc resultDoc = new ResultDoc(existingDoc, resultDocConfig);
			XMLDocReader recordInRepository = null;

			DocReader docReader = resultDoc.getDocReader();

			// If this is an ErrorDoc, return null so it is removed:
			if (docReader instanceof ErrorDocReader) {
				//prtln("getDeletedDoc() docReader is an ErrorDocReader");
				return null;
			}

			recordInRepository = (XMLDocReader) resultDoc.getDocReader();
			//prtln("docReader is " + recordInRepository.getReaderType());

			BooleanQuery idQ = new BooleanQuery();
			idQ.add(new TermQuery(new Term("id", recordInRepository.getIdEncoded())), BooleanClause.Occur.MUST);
			idQ.add(new TermQuery(new Term("deleted", "false")), BooleanClause.Occur.MUST);

			ResultDocList resultDocs = getIndex().searchDocs((idQ));

			// If another file has replaced this one, don't create a deleted doc:
			if (resultDocs != null && resultDocs.size() > 0) {
				XMLDocReader existingDocReader = (XMLDocReader) ((ResultDoc)resultDocs.get(0)).getDocReader();
				if (!existingDocReader.getDocsource().equals(recordInRepository.getDocsource())) {
					//prtln("getDeletedDoc() There was an existing doc in the index in another location, not deleting");
					return null;
				}
				else {
					//prtln("getDeletedDoc() The existing doc in the index is this one... deleting");
				}
			}

			// Make a new Document and return it
			XMLFileIndexingWriterFactory xmlFileIndexingWriterFactory = new XMLFileIndexingWriterFactory(getRecordDataService(), getIndex(), getXmlIndexerFieldsConfig());
			XMLFileIndexingWriter xmlFileIndexingWriter = xmlFileIndexingWriterFactory.getIndexingWriter(recordInRepository.getCollection(), recordInRepository.getNativeFormat());
			xmlFileIndexingWriter.setIsMakingDeletedDoc(true);
			FileIndexingServiceData fileIndexingServiceData = xmlFileIndexingWriter.create(null, existingDoc, getFileIndexingPlugin(), getSessionAttributes());
			newDocument = fileIndexingServiceData.getDoc();
			if (newDocument != null) {
				//prtln("Returning a deletedDoc for id: " + recordInRepository.getId() + " file: " + recordInRepository.getDocsource());
			}
		} catch (Throwable t) {
			//t.printStackTrace();
			throw t;
		}

		return newDocument;
	}


	/**
	 *  Gets the annotations for this record, null or zero length if none available.
	 *
	 * @return                The myAnnoResultDocs value
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	protected ResultDocList getMyAnnoResultDocs() throws Exception {
		if (_myAnnoResultDocs == null) {
			RecordDataService recordDataService = getRecordDataService();
			if (recordDataService != null) {
				// Get annotation for this record only. If I am a multi-doc, these (should) include all annos for all records
				_myAnnoResultDocs = recordDataService.getDleseAnnoResultDocs(getIds());
			}
		}
		return _myAnnoResultDocs;
	}


	/**
	 *  Gets the XMLIndexerFieldsConfig to use for XML indexing, or null if none available.
	 *
	 * @return    The xmlIndexerFieldsConfig value
	 */
	protected XMLIndexerFieldsConfig getXmlIndexerFieldsConfig() {
		return (XMLIndexerFieldsConfig) getConfigAttributes().get("xmlIndexerFieldsConfig");
	}


	/**
	 *  Gets the vocab encoded keys for the given values, separated by the '+' symbol.
	 *
	 * @param  values           The valuse to encode.
	 * @param  useVocabMapping  The mapping to use, for example "contentStandards".
	 * @param  metadataFormat   The metadata format, for example 'adn'
	 * @return                  The encoded vocab keys.
	 * @exception  Exception    If error.
	 */
	protected String getFieldContent(String[] values, String useVocabMapping, String metadataFormat)
		 throws Exception {
		if (values == null || values.length == 0) {
			return "";
		}

		RecordDataService recordDataService = getRecordDataService();

		MetadataVocab vocab = null;
		if (recordDataService != null)
			vocab = recordDataService.getVocab();

		StringBuffer ret = new StringBuffer();
		for (int i = 0; i < values.length; i++) {
			String str = values[i].trim();
			if (str.length() > 0) {
				// Use vocabMgr mapping if available, otherwise output unchanged
				if (useVocabMapping != null && vocab != null) {
					try {
						ret.append(vocab.getTranslatedValue(metadataFormat, useVocabMapping, str));
					} catch (Throwable t) {
						// prtlnErr("getFieldContent(): " + t);
						ret.append(str);
						//prtlnErr("Warning: Unable to get vocab mapping for '" + useVocabMapping + ":" + str + "'. Using unencoded value instead. Reason: " + t);
					}
				}
				else {
					ret.append(str);
				}

				// Separate each term with +
				if (i < (values.length - 1)) {
					ret.append("+");
				}
			}
		}
		//prtln("Field content: " + ret.toString());
		return ret.toString();
	}


	/**
	 *  Gets the encoded vocab key for the given content.
	 *
	 * @param  value            The value to encode
	 * @param  useVocabMapping  The vocab mapping to use, for example 'contentStandard'
	 * @param  metadataFormat   The metadata format, for example 'adn'
	 * @return                  The encoded value, or unchanged if unable to encode
	 * @exception  Exception    If error
	 */
	protected String getFieldContent(String value, String useVocabMapping, String metadataFormat)
		 throws Exception {
		if (value == null || value.trim().length() == 0) {
			return "";
		}

		RecordDataService recordDataService = getRecordDataService();

		MetadataVocab vocab = null;
		if (recordDataService != null)
			vocab = recordDataService.getVocab();

		// Use vocabMgr mapping if available, otherwise output unchanged
		if (useVocabMapping != null && vocab != null) {
			try {
				return vocab.getTranslatedValue(metadataFormat, useVocabMapping, value);
			} catch (Throwable t) {
				// prtlnErr("getFieldContent(): " + t);
				return value;
			}
		}
		else {
			return value;
		}
	}


	/**
	 *  Gets the field ID, for example 'gr', for a given vocab, for example 'gradeRange'. If unable to get the
	 *  field ID, the vocab field String is returned unchanged.
	 *
	 * @param  vocabFieldString  The field, for example 'gradeRange'
	 * @param  metadataFormat    The metadata format, for example 'adn'
	 * @return                   The field key, for example 'gr', or unchanged if unable to determine
	 * @exception  Exception     If error
	 */
	protected String getFieldName(String vocabFieldString, String metadataFormat)
		 throws Exception {
		if (vocabFieldString == null || vocabFieldString.trim().length() == 0) {
			return "";
		}

		RecordDataService recordDataService = getRecordDataService();

		MetadataVocab vocab = null;
		if (recordDataService != null)
			vocab = recordDataService.getVocab();

		String fieldName;
		if (vocab == null) {
			fieldName = vocabFieldString;
		}
		else {
			try {
				fieldName = vocab.getTranslatedField(metadataFormat, vocabFieldString);
			} catch (Throwable t) {
				// prtlnErr("getFieldName(): " + t);
				fieldName = vocabFieldString;
				//prtlnErr("Warning: Unable to get vocab mapping for '" + vocabFieldString + "'. Using unencoded value instead. Reason: " + t);
			}
		}

		//prtln("Field name " + vocabFieldString + " encoded as id: " + fieldName);
		return fieldName;
	}



	/**
	 *  Gets the appropriate terms from a string array of metadata fields. Uses all terms found after the last
	 *  colon ":" found in the string.
	 *
	 * @param  vals  Metadata fields that must be delemited by colons.
	 * @return       The individual terms used for indexing.
	 */
	protected String getTermStringFromStringArray(String[] vals) {
		if (vals == null) {
			return "";
		}
		String tmp = "";
		try {
			for (int i = 0; i < vals.length; i++) {
				tmp += " " + vals[i].substring(vals[i].lastIndexOf(":") + 1, vals[i].length());
			}
		} catch (Throwable e) {}
		return tmp.trim();
	}


	/**
	 *  Gets the XMLIndexer for use by sub-classes
	 *
	 * @return                The XMLIndexer
	 * @exception  Exception  If error
	 */
	protected XMLIndexer getXmlIndexer() throws Exception {
		if (_xmlIndexer == null)
			_xmlIndexer = new XMLIndexer(getFileContent(), getDocType(), getXmlIndexerFieldsConfig());
		return _xmlIndexer;
	}


	/**
	 *  Gets the dom4j Document for use by sub-classes
	 *
	 * @return                The Document
	 * @exception  Exception  If error
	 */
	protected org.dom4j.Document getDom4jDoc() throws Exception {
		return getXmlIndexer().getXmlDocument();
	}


	/**
	 *  Gets the DLESECollectionDocReader for the collection in which this item is a part, or null if not
	 *  available.
	 *
	 * @return    The myCollectionDoc value
	 */
	protected DleseCollectionDocReader getMyCollectionDoc() {
		if (myCollectionDocReader == null) {
			RecordDataService recordDataService = getRecordDataService();

			// RecordDataService is not available in OAI app:
			if (recordDataService == null) {
				//prtlnErr("Error: recordDataService is null! Cannot get collection doc.");
				return null;
			}

			ResultDocList collections;
			try {
				String q = "key:" + getCollections()[0];
				collections = recordDataService.getIndex().searchDocs(q);
			} catch (Throwable e) {
				// When the index is first built, the 'collect' collection is not avaialble until it is written to the index
				prtlnErr("Unable to get collection doc: " + e);
				prtln("Unable to get collection doc: " + e);
				return null;
			}
			if (collections != null && collections.size() > 0)
				myCollectionDocReader = (DleseCollectionDocReader) ((ResultDoc)collections.get(0)).getDocReader();
		}
		return myCollectionDocReader;
	}


	/**
	 *  Gets the oaiModtime for the given File or Document, set to 3 minutes in the future to account for any
	 *  delay in indexing updates.
	 *
	 * @param  sourceFile   The source file
	 * @param  existingDoc  The existing Doc
	 * @return              The oaiModtime value
	 */
	public final static String getOaiModtime(File sourceFile, Document existingDoc) {

		// If this method is being called, that means something has changed
		// that effects this file.
		return DateFieldTools.timeToString(System.currentTimeMillis() + 180000);
	}


	/**
	 *  Gets the recordDataService used by this XML File Indexer
	 *
	 * @return    The recordDataService, or null if not available.
	 */
	protected RecordDataService getRecordDataService() {
		Map configAttr = getConfigAttributes();
		if (configAttr == null)
			return null;
		return (RecordDataService) configAttr.get("recordDataService");
	}


	/**
	 *  Gets the index used by this XML File Indexer
	 *
	 * @return    The index, or null if not available.
	 */
	protected SimpleLuceneIndex getIndex() {
		Map configAttr = getConfigAttributes();
		if (configAttr == null)
			return null;
		return (SimpleLuceneIndex) configAttr.get("index");
	}

}

