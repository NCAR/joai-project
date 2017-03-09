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

import org.apache.lucene.document.*;

/**
 *  This Interface provides a mechanism for adding custom fields to Lucene index Documents that are being
 *  created for a given file. Fields created using this Interface should use a unigue field name namespace
 *  prefix to avoid field name conflicts. For information about adding fields, see the Lucene {@link
 *  org.apache.lucene.document.Document} javadoc.<p>
 *
 *  To configure one or more Lucene Analyzers to use on a per-field basis for your plugins, provide a
 *  properties file within your application's class path named 'FileIndexingPluginLuceneAnalyzers.properties'
 *  (used in the DDS, NCS, and DCS Web applications). See {@link org.dlese.dpc.index.analysis.PerFieldAnalyzer}
 *  for details. <p>
 *
 *  Example usage:<p>
 *
 *  <code>
 *private static final String FIELD_NS = "MyMetaMetadata";<br>
 *  <br>
 *  public void addFields(File file, Document newDoc, Document existingDoc)<br>
 *  &nbsp;&nbsp;&nbsp;throws Exception{ <br>
 *  <blockquote> ... code to set up data as needed ... <br>
 *  String recordStatus = myData.getRecordStatus(); <br>
 *  newDoc.add(new Field(FIELD_NS + "recordStatus", recordStatus, Field.Store.YES, Field.Index.NOT_ANALYZED));
 *  <br>
 *  ... add additional fields as needed ... <br>
 *  </blockquote> }<br>
 *  </code>
 *
 * @author    John Weatherley
 * @see       FileIndexingServiceWriter
 */
public interface FileIndexingPlugin {

	/**
	 *  This method may be used to add custom fields to a Lucene Document for a given file prior to it's being
	 *  inserted into the index. This method is called by the {@link FileIndexingServiceWriter} after it has
	 *  completed adding it's fields to the Lucene Document.
	 *
	 * @param  file           The file that is being indexed
	 * @param  newDoc         The new Lucene Document that will be inserted in the index for this file
	 * @param  existingDoc    The previous Lucene Document that existed for this record, or null if not available
	 * @param  docType        The docType for this file, for example 'adn', 'dlese_collect' (equivalent to XML
	 *      format in the DLESE metadata repository)
	 * @param  docGroup       The docGroup associated with this file, for example 'dcc', 'comet', or null if none
	 *      is associated (equivalent to the collection key in the DLESE metadata repository)
	 * @exception  Exception  Exception should be thrown to index this Document as an error
	 * @see                   org.apache.lucene.document.Document
	 */
	public void addFields(File file, Document newDoc, Document existingDoc, String docType, String docGroup)
		 throws Exception;

}

