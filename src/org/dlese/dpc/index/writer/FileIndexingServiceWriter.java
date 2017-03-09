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
import org.dlese.dpc.util.*;
import org.apache.lucene.document.*;
import org.dlese.dpc.index.document.DateFieldTools;

/**
 *  Abstract class for creating customized Lucene {@link org.apache.lucene.document.Document}s for different
 *  file formats such as DLESE-IMS, ADN-item, ADN-collection, etc. Concrete sub-classes may be used with a
 *  {@link org.dlese.dpc.index.FileIndexingService} to enable automatic updating of the index whenever changes
 *  in the source file are made. This class, along with the {@link org.dlese.dpc.index.FileIndexingService},
 *  may be used with a {@link org.dlese.dpc.index.SimpleLuceneIndex} to provide simple search support over
 *  files.<p>
 *
 *  Note: after creating a new concrete FileIndexingServiceWriter, add a switch in {@link
 *  org.dlese.dpc.repository.RepositoryManager}, method putDirInIndex(DirInfo, String) to select it for
 *  indexing.<p>
 *
 *  <br>
 *  The Lucene fields that are created by this class are:
 *  <ul>
 *    <li> <code>doctype</code> - The document format type (e.g. dlese_ims, adn, oai_dc, etc.) defined by
 *    concrete classes, with '0' appended to support wildcard searching. </li>
 *    <li> <code>readerclass</code> - The class which is used to read typed {@link
 *    org.apache.lucene.document.Document}s created by the concrete classes, for example "ItemDocReader".</li>
 *
 *    <li> <code>default</code> - The default field containing content added by concrete classes. Generally
 *    this is the field assigned in the Lucene index for default searching.</li>
 *    <li> <code>docsource</code> - The absolute path to the file, which is used by the {@link
 *    org.dlese.dpc.index.FileIndexingService} for updating/deleting and may be used by beans or other classes
 *    that wish to have access to the source file.</li>
 *    <li> <code>docdir</code> - The absolute path to the directory where the file resides, which is used by
 *    the {@link org.dlese.dpc.index.FileIndexingService} for updating/deleting and may be used by beans or
 *    other classes.</li>
 *    <li> <code>modtime</code> - The file modification time, which is used by the {@link
 *    org.dlese.dpc.index.FileIndexingService} to determine if the file has changed and needs update and may
 *    be used by beans or other classes that wish to query the modtime for the record.</li>
 *    <li> <code>filecontent</code> - The full content of the file, stored but not indexed.</li>
 *    <li> <code>deleted</code> - Set to 'true' if the file or record for this document has been deleted,
 *    otherwise this field does not exist. Stored. </li>
 *    <li> <code>valid</code> - Set to 'true' if the file or record for this document is valid, otherwise
 *    'false'. This field may also be ommited. Not stored. </li>
 *    <li> <code>validationreport</code> - Contains a report that provides validation information about the
 *    underlying file. This field may be ommited. Not stored.</li>
 *  </ul>
 *
 *
 * @author    John Weatherley
 */
public abstract class FileIndexingServiceWriter implements DocWriter {
	private static boolean debug = false;
	private boolean validateFiles = false;
	private boolean abortIndexing = false;
	private FileIndexingService fileIndexingService = null;
	private File source = null;
	private File dir = null;
	private String fileContent = null;
	private Document luceneDoc = null;
	private Document previousRecordDoc = null;
	private FileIndexingServiceData newDocData = null;
	private FileIndexingPlugin fileIndexingPlugin = null;
	private HashMap configAttributes = null;
	private HashMap sessionAttributes = null;
	private boolean isMakingDeletedDoc = false;


	// --------------- Abstract methods for subclasses --------------------

	/**
	 *  Gets a unique document type key for this kind of record, corresponding to the format type. In the DLESE
	 *  metadata repository, this corresponds to the XML format, for example "oai_dc," "adn," "dlese_ims," or
	 *  "dlese_anno". The string is parsed using the Lucene {@link org.apache.lucene.analysis.standard.StandardAnalyzer}
	 *  so it must be lowercase and should not contain any stop words.
	 *
	 * @return                The docType String
	 * @exception  Exception  This method should throw and Exception with appropriate error message if an error
	 *      occurs.
	 */
	public abstract String getDocType() throws Exception;


	/**
	 *  Gets the specifier associated with this group of files or null if no group association exists. In the
	 *  DLESE metadata repository, this corresponds to the collection key, for example 'dcc', 'comet'.
	 *
	 * @return                The docGroup specifier
	 * @exception  Exception  If error occured
	 */
	public abstract String getDocGroup() throws Exception;


	/**
	 *  Gets the fully qualified name of the concrete {@link org.dlese.dpc.index.reader.DocReader} class that is
	 *  used to read this type of {@link org.apache.lucene.document.Document}, for example
	 *  "org.dlese.dpc.index.reader.ItemDocReader".
	 *
	 * @return    The name of the {@link org.dlese.dpc.index.reader.DocReader}.
	 */
	public abstract String getReaderClass();


	/**
	 *  This method is called prior to processing and may be used to for any necessary set-up. This method should
	 *  throw and exception with appropriate message if an error occurs. The config attributes are set using the
	 *  {@link org.dlese.dpc.index.FileIndexingService#addDirectory} method.
	 *
	 * @param  source         The source file being indexed
	 * @param  previousRecordDoc    An existing Document that currently resides in the index for the given resource, or
	 *      null if none was previously present
	 * @exception  Exception  If an error occured during set-up.
	 */
	public abstract void init(File source, Document previousRecordDoc) throws Exception;


	/**  This method is called at the conclusion of processing and may be used for tear-down. */
	protected abstract void destroy();


	/**
	 *  Adds additional custom fields that are unique the document format being indexed. When implementing this
	 *  method, use the add method of the {@link org.apache.lucene.document.Document} class to add a {@link
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
	 *  <code>protected void addCustomFields(Document newDoc, Document previousRecordDoc) throws Exception {</code>
	 *  <br>
	 *  &nbsp;<code> String customContent = "Some content";</code><br>
	 *  &nbsp;<code> newDoc.add(Field.Text("mycustomefield", customContent));</code> <br>
	 *  <code>}</code>
	 *
	 * @param  newDoc         The new {@link org.apache.lucene.document.Document} that is being created for this
	 *      resource
	 * @param  previousRecordDoc    An existing {@link org.apache.lucene.document.Document} that currently resides in
	 *      the index for the given resource, or null if none was previously present
	 * @param  sourceFile     The sourceFile that is being indexed
	 * @exception  Exception  This method should throw and Exception with appropriate error message if an error
	 *      occurs.
	 */
	protected abstract void addCustomFields(Document newDoc, Document previousRecordDoc, File sourceFile) throws Exception;


	// --------------- Methods inherited by subclasses --------------------

	/**
	 *  Gets the full content of the file as a String. If the file does not exist or the writer is processing a
	 *  deleted doc, the content is pulled from the existing Lucene Document rather than the file.
	 *
	 * @return                  The full content of the file
	 * @exception  IOException  If error
	 */
	public String getFileContent() throws IOException {
		if (fileContent == null) {
			if (isMakingDeletedDoc())
				fileContent = previousRecordDoc.get("filecontent");
			else if (source.exists())
				fileContent = Files.readFileToEncoding(source, "UTF-8").toString();
			if (fileContent == null && previousRecordDoc != null)
				fileContent = previousRecordDoc.get("filecontent");
			if (fileContent == null)
				fileContent = "";

			// Remove the BOM character, if present, since it crashes the XML processors (Windows notepad places it there when saving files as UTF-8):
			if (fileContent.startsWith("\uFEFF"))
				fileContent = fileContent.replaceFirst("\uFEFF", "");
		}
		return fileContent;
	}


	/**
	 *  Gets the configuration attributes that were set when the writer was created.
	 *
	 * @return    The configuration attributes, or null if none were configured
	 */
	public HashMap getConfigAttributes() {
		return configAttributes;
	}


	/**
	 *  Sets the configuration attributes - called by the factory method that creates the
	 *  FileIndexingServiceWriter.
	 *
	 * @param  attributes  The configuration attributes
	 */
	public void setConfigAttributes(HashMap attributes) {
		configAttributes = attributes;
	}


	/**
	 *  Gets a Map of attributes used in a single indexing session. A seesion is a portion of indexing for a
	 *  given directory of records that will be added to the index as a block update. Since records are added to
	 *  the index at the end of the session, the index can not be used to query information from those records
	 *  during the session. Thus, these attributes can be used to communitcate information across records being
	 *  indexed within a given session, such as the record IDs found so far in the session. The attributes are
	 *  cleared at the end of each session.
	 *
	 * @return    A Map of records IDs keys, or null
	 */
	public HashMap getSessionAttributes() {
		return sessionAttributes;
	}


	/**
	 *  Gets the sourceFile that is being indexed. Only available after create() has been called.
	 *
	 * @return    The sourceFile value
	 */
	public File getSourceFile() {
		return source;
	}


	/**
	 *  Gets the absolute path to the file, which is indexed under the 'docsource' field.
	 *
	 * @return    The absolute path to the file
	 */
	public String getDocsource() {
		return source.getAbsolutePath();
	}


	/**
	 *  Gets the sourceDir that holds the file being indexed. Only available after create() has been called.
	 *
	 * @return    The sourceDir value
	 */
	public File getSourceDir() {
		return dir;
	}
	
	/**
	 *  Gets the Lucene Document that this Writer is building.
	 *
	 * @return    The Lucene Document
	 */
	public Document getLuceneDoc() {
		if(luceneDoc == null)
			luceneDoc = new Document();
		return luceneDoc;
	}	
	
	/**
	 *  Gets the previous Document that currently resides in the index for the given resource, or null if none was
	 *  previously present.
	 *
	 * @return    The previousRecordDoc value
	 */
	public Document getPreviousRecordDoc() {
		return previousRecordDoc;
	}


	/**
	 *  Sets the fileIndexingService attribute of the FileIndexingServiceWriter object
	 *
	 * @param  fileIndexingService  The new fileIndexingService.
	 */
	public void setFileIndexingService(FileIndexingService fileIndexingService) {
		this.fileIndexingService = fileIndexingService;
	}


	/**
	 *  Gets the fileIndexingService attribute of the FileIndexingServiceWriter object
	 *
	 * @return    The fileIndexingService.
	 */
	public FileIndexingService getFileIndexingService() {
		return fileIndexingService;
	}


	/**
	 *  Returns true if the files being indexed should be validated, otherwise false. This method may be ignored
	 *  by concrete classes if not needed.
	 *
	 * @return    true if validateion is enabled.
	 */
	public boolean isValidationEnabled() {
		return validateFiles;
	}


	/**
	 *  Sets whether or not to validate the files being indexed and create a validation report, which is indexed.
	 *  This value is set by the {@link org.dlese.dpc.index.FileIndexingService} prior to indexing. If true, the
	 *  method {@link #getValidationReport()} will be called, otherwise it will not.
	 *
	 * @param  validateFiles  True to validate, else false.
	 * @see                   #getValidationReport()
	 * @see                   org.dlese.dpc.index.FileIndexingService#setValidationEnabled(boolean validateFiles)
	 */
	public void setValidationEnabled(boolean validateFiles) {
		this.validateFiles = validateFiles;
	}


	/**
	 *  Gets a report detailing any errors found in the validation of the file, or null if no error was found.
	 *  This method should be overridden by concrete classes that need to validate the underlying file before
	 *  indexing. Otherwise, this default method will simply return null. This method is called after all other
	 *  method calls.
	 *
	 * @return                Null if no file validation errors were found, otherwise a String that details the
	 *      nature of the error.
	 * @exception  Exception  If error.
	 */
	protected String getValidationReport()
		 throws Exception {
		return null;
	}

	/**
	 *  Adds the given String to the 'default' and 'stems' fields as text and stemmed text, respectively. The
	 *  default and stems fields may be used in queries to quickly search for text across fields. This method
	 *  should be called from the addCustomFields of implementing classes.
	 *
	 * @param  value  A text string to be added to the indexed fields named 'default' and 'stems'
	 */
	protected void addToDefaultField(String value) {
		if (value != null && value.trim().length() > 0)
			IndexingTools.addToDefaultAndStemsFields(getLuceneDoc(), value);
	}

	/**
	 *  Adds the given String to a text field referenced in the index by the field name 'admindefault'. The
	 *  default field may be used in queries to quickly search for text across fields. This method should be
	 *  called from the addCustomFields of implementing classes.
	 *
	 * @param  value  A text string to be added to the indexed field named 'admindefault.'
	 */
	protected void addToAdminDefaultField(String value) {
		if (value != null && value.trim().length() > 0)
			IndexingTools.addToAdminDefaultField(getLuceneDoc(), value);
	}

	/**
	 *  Creates a Lucene {@link org.apache.lucene.document.Document} equal to the exsiting FileIndexingService
	 *  Document except the field "deleted" is to "true" and the field "modtime" has been set to the current
	 *  time. <p>
	 *
	 *  Design note: This method should be overwritten by subclasses that require more envolved logic for
	 *  deletes, and this super method should be called first and then subclassed should check {@link
	 *  #getIsMakingDeletedDoc} to execute as appropriate.
	 *
	 * @param  previousRecordDoc    An existing FileIndexingService Document that currently resides in the index for
	 *      the given file
	 * @return                A Lucene FileIndexingService Document with appropriate fields updated
	 * @exception  Throwable  Thrown if error occurs
	 */
	public synchronized Document getDeletedDoc(Document previousRecordDoc)
		 throws Throwable {

		if (previousRecordDoc == null)
			throw new Exception("getDeletedDoc(): the existing doc is null");
		isMakingDeletedDoc = true;

		return previousRecordDoc;
	}


	/**
	 *  Sets whether this DocWriter is making a deleted document. Used by subclassed that crate a DocWriter in
	 *  their {@link getDeletedDoc} method.
	 *
	 * @param  isMakingDeletedDoc  Sets the making deleted doc status
	 */
	protected void setIsMakingDeletedDoc(boolean isMakingDeletedDoc) {
		this.isMakingDeletedDoc = isMakingDeletedDoc;
	}


	/**
	 *  True if the current execution represents a deleted doc is being created.
	 *
	 * @return    True if a deleted doc is being created
	 */
	protected final boolean isMakingDeletedDoc() {
		return isMakingDeletedDoc;
	}


	/**  Aborts the indexing process by returning a null index document. */
	protected void abortIndexing() {
		abortIndexing = true;
	}


	/**
	 *  Removes a matching item from the index during the FileIndexingService update. This method should be
	 *  called to instruct the indexer to remove documents that should no longer be in the index.
	 *
	 * @param  field  The field to search in.
	 * @param  value  The matching value for the item to remove.
	 */
	protected void addDocToRemove(String field, String value) {
		if (newDocData != null) {
			//prtln("addDocToRemove() removing  field: " + field + " value: " + value);
			newDocData.addDocToRemove(field, value);
		}
		else {
			// prtln("addDocToRemove() NOT removing  field: " + field + " value: " + value);
		}

	}


	/**
	 *  Creates the Lucene {@link org.apache.lucene.document.Document} for the given resource or returns null if
	 *  unable to create. This method is called by class {@link FileIndexingService}.
	 *
	 * @param  sourceFile         The source file to be indexed
	 * @param  existingLuceneDoc  An existing Document that currently resides in the index for the given
	 *      resource, or null if none was previously present
	 * @param  plugin             The FileIndexingPlugin being used, or null
	 * @param  sessionAttr        Attributes used in a given indexing session
	 * @return                    A Lucene Document with it's fields populated, or null.
	 * @exception  Throwable      Thrown if error occurs
	 */
	public synchronized FileIndexingServiceData create(File sourceFile,
	                                                   Document existingLuceneDoc,
	                                                   FileIndexingPlugin plugin,
	                                                   HashMap sessionAttr)
		 throws Throwable {

		try {
			abortIndexing = false;

			if (sourceFile == null && existingLuceneDoc == null)
				throw new Exception("Both the source file and the existing doc are null");

			// If the file does not exist but an existing Doc does, we are processing a deleted doc
			if (sourceFile == null)
				sourceFile = new File(existingLuceneDoc.get("docsource"));

			if (abortIndexing) {
				destroy();
				return null;
			}

			this.sessionAttributes = sessionAttr;
			this.source = sourceFile;
			if (sourceFile != null)
				this.dir = source.getParentFile();
			this.previousRecordDoc = existingLuceneDoc;

			/*
		 *  To do: refactor to add a "getDocReaderClass()" method that is indexed
		 *  with the name of the DocReader class used to read the given doctype. Then
		 *  implement a class loader in ResultDoc to automatically create the
		 *  correct DocReader
		 */

			if (newDocData == null)
				newDocData = new FileIndexingServiceData();
			newDocData.clearAll();
			init(source, previousRecordDoc);
			if (abortIndexing) {
				destroy();
				return null;
			}

			Document doc = getLuceneDoc();

			// A field/term that matches all records:
			doc.add(new Field("allrecords", "true", Field.Store.NO, Field.Index.ANALYZED));

			// A field used to determine whether the file has been deleted (also used for OAI deletion status):
			if (isMakingDeletedDoc())
				doc.add(new Field("deleted", "true", Field.Store.YES, Field.Index.ANALYZED));
			else
				doc.add(new Field("deleted", "false", Field.Store.YES, Field.Index.ANALYZED));

			// -------------- Required fields the for FileIndexingService --------------

			// See class JavaDoc for details on this field.
			doc.add(new Field("docsource", getDocsource(), Field.Store.YES, Field.Index.NOT_ANALYZED));

			// See class JavaDoc for details on this field.
			doc.add(new Field("filename", source.getName(), Field.Store.YES, Field.Index.NOT_ANALYZED));

			// See class JavaDoc for details on this field.
			doc.add(new Field("docdir", dir.getAbsolutePath(), Field.Store.YES, Field.Index.NOT_ANALYZED));

			// Field used to remove docs from the index when directories are deleted (if not a deletedDoc)
			if (!isMakingDeletedDoc())
				doc.add(new Field("docdir_remove", dir.getAbsolutePath(), Field.Store.YES, Field.Index.NOT_ANALYZED));

			// See class JavaDoc for details on this field.
			if (source.exists())
				doc.add(new Field("modtime", DateFieldTools.timeToString(source.lastModified()), Field.Store.YES, Field.Index.NOT_ANALYZED));
			else
				doc.add(new Field("modtime", DateFieldTools.timeToString(System.currentTimeMillis()), Field.Store.YES, Field.Index.NOT_ANALYZED));

			// See class JavaDoc for details on this field.
			doc.add(new Field("filecontent", getFileContent(), Field.Store.YES, Field.Index.NO));

			// -------------- Required fields for DocWriter --------------

			if (abortIndexing) {
				destroy();
				return null;
			}

			// See class JavaDoc for details on this field.
			doc.add(new Field("doctype", '0' + getDocType(), Field.Store.YES, Field.Index.NOT_ANALYZED));

			// See class JavaDoc for details on this field.
			doc.add(new Field("readerclass", getReaderClass(), Field.Store.YES, Field.Index.ANALYZED));

			// ------- Index all standard and custom fields for the framework being indexed by subclasses -------

			addCustomFields(doc, previousRecordDoc, source);

			// ------- Add file validation information, if apporpriate -------

			if (abortIndexing) {
				destroy();
				return null;
			}
			if (validateFiles) {
				String validationReport = getValidationReport();
				if (validationReport != null) {
					doc.add(new Field("valid", "false", Field.Store.NO, Field.Index.ANALYZED));
					doc.add(new Field("validationreport", validationReport, Field.Store.YES, Field.Index.ANALYZED));
				}
				else
					doc.add(new Field("valid", "true", Field.Store.NO, Field.Index.ANALYZED));
			}

			// ------- Store the default field and return -------

			// Index the 'default' and 'stems' fields. See class JavaDoc for details on this field.
			//IndexingTools.indexDefaultAndStemsFields(doc, defaultBuffer.toString());

			// Admin default field
			//doc.add(new Field(adminDefaultFieldName, adminDefaultBuffer.toString(), Field.Store.NO, Field.Index.ANALYZED));

			if (abortIndexing) {
				destroy();
				return null;
			}

			// If a FileIndexingPlugin has been passed in, use it...
			if (plugin != null)
				plugin.addFields(sourceFile, doc, existingLuceneDoc, getDocType(), getDocGroup());
			// Else if we have a pre-configured FileIndexingPlugin, use it...
			else if (fileIndexingPlugin != null)
				fileIndexingPlugin.addFields(sourceFile, doc, existingLuceneDoc, getDocType(), getDocGroup());

			// Record the time this file was indexed:
			doc.add(new Field("fileindexeddate", DateFieldTools.timeToString(System.currentTimeMillis()), Field.Store.YES, Field.Index.ANALYZED));

			newDocData.setDoc(doc);
		} catch (ErrorDocException e) {
			// Throw so ErrorDoc will be created...
			throw e;
		} catch (Throwable t) {
			// Report for debugging
			//t.printStackTrace();
			throw t;
		}
		return newDocData;
	}


	/**
	 *  Sets the FileIndexingPlugin that will be used during the indexing process to index additional fields. Set
	 *  to null to remove.
	 *
	 * @param  plugin  A FileIndexingPlugin to use during indexing.
	 */
	public void setFileIndexingPlugin(FileIndexingPlugin plugin) {
		fileIndexingPlugin = plugin;
	}


	/**
	 *  Gets the FileIndexingPlugin that has been set for use during indexing, or null if none.
	 *
	 * @return    The FileIndexingPlugin configured for use used, or null.
	 */
	public FileIndexingPlugin getFileIndexingPlugin() {
		return fileIndexingPlugin;
	}


	// ---------------- Debugging/utility methods	-----------------------

	/**
	 *  Return a string for the current time and date, sutiable for display in log files and output to standout:
	 *
	 * @return    The dateStamp value
	 */
	private final String getDateStamp() {
		return
			new SimpleDateFormat("MMM d, yyyy h:mm:ss a zzz").format(new Date());
	}


	/**
	 *  Output a line of text to error out, with datestamp.
	 *
	 * @param  s  The text that will be output to error out.
	 */
	protected final void prtlnErr(String s) {
		System.err.println(getDateStamp() + " " + getClass().getSimpleName() + " Error: " + s);
	}



	/**
	 *  Output a line of text to standard out, with datestamp, if debug is set to true.
	 *
	 * @param  s  The String that will be output.
	 */
	protected final void prtln(String s) {
		if (debug)
			System.out.println(getDateStamp() + " " + getClass().getSimpleName() + ": " + s);
	}


	/**
	 *  Sets the debug attribute of the FileIndexingServiceWriter object
	 *
	 * @param  db  The new debug value
	 */
	public final static void setDebug(boolean db) {
		debug = db;
	}

}

