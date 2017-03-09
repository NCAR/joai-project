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
import org.dlese.dpc.util.*;
import org.apache.lucene.document.*;
import org.dlese.dpc.repository.*;
import org.dlese.dpc.vocab.*;

/**
 *  A FileIndexingPlugin that indexes meta-metadata about items (educational resources) in
 *  the index.
 *
 * @author     John Weatherley
 * @see        FileIndexingServiceWriter
 * <p>
 *
 *      $Log: DDSItemMetaMetadataIndexingPlugin.java,v $
 *      Revision 1.6  2010/07/14 00:18:49  jweather
 *      -All Lucene-related classes have been upgraded to Lucene v3.0.2. Major changes include:
 *      --Search results are now returned in a new ResultDocList Object instead of a ResultDoc[]
 *        array. This provides for more efficient searching (does not require an additional
 *        loop over results as before) and expandability (methods can be added to the ResultDocList
 *        to support future fuctnionality) and better utilizes the built-in Lucene classes
 *        for search than before (TopDocs, Sort, etc.)
 *      --Uses Lucene Sort class for sorting at search time. Replaces logic that sorted
 *        results after the search (deprecated but still supported for backward-compatibility)
 *
 *      - Final previous version was tagged in CVS with 'lucene_2_4_final_version'
 *
 *      Revision 1.5  2009/03/20 23:33:53  jweather
 *      -updated the license statement in all Java files to Educational Community License v1.0.
 *
 *      Revision 1.4  2007/07/05 22:29:35  jweather
 *      -Upgradded from Lucene v1.4.3 to Lucene v2.2.0  (code merged from lucene-2-upgrade-branch to head)
 *
 *      Revision 1.3.6.1  2007/07/05 21:12:25  jweather
 *      -Updated Field BooleanQuery method calls to use Lucene 2.0 syntax
 *
 *      Revision 1.3  2004/10/08 17:35:53  jweather
 *      fixed the "too many open files" problem when DDS is auto-reloaded
 *      in Tomcat multiple times
 *
 *      Revision 1.2  2004/09/10 22:46:05  jweather
 *      added XML format (docType) and collection (docGroup) to the FileIndexingServicePlugin
 * Revision 1.1 2004/09/10 01:58:06
 *      jweather FileIndexingPlugins for use in the DDS Servlet indexing of items <p>
 *
 *
 */
public class DDSItemMetaMetadataIndexingPlugin extends ServletContextFileIndexingPlugin {
	private final static String FIELD_NS = "itemMetaMetadata";

	private static File recordMetaMetadataDir = null;


	/**
	 *  Gets the meta-metadata file associated with the given metadata file. The file that is
	 *  returned is not guaranteed to exist.
	 *
	 * @param  metadataFile  A metadata File
	 * @return               The meta-metadata File associated with the File
	 */
	public final static File getMetaMetadataFile(File metadataFile) {
		if (recordMetaMetadataDir == null)
			recordMetaMetadataDir = (File) getServletContext().getAttribute("recordMetaMetadataDir");

		String filePath = metadataFile.getParentFile().getParentFile().getName() + File.separatorChar +
			metadataFile.getParentFile().getName() + File.separatorChar + metadataFile.getName();

		return new File(recordMetaMetadataDir, filePath);
	}


	/**
	 *  Indexes a single field 'DDSItemMetaMetadataIndexingPlugin' with the value 'true'. The
	 *  index may be searched using this field/value to determine which records have been
	 *  indexed using this plugin.
	 *
	 * @param  file           The file that is being indexed
	 * @param  newDoc         The new Lucene Document that will be inserted in the index for
	 *      this file
	 * @param  existingDoc    The previous Lucene Document that existed for this record, or
	 *      null if not available
	 * @param  docType        The docType for this file, for example 'adn', 'dlese_collect'
	 *      (equivalent to XML format in the DLESE metadata repository)
	 * @param  docGroup       The docGroup associated with this file, for example 'dcc',
	 *      'comet', or null if none is associated (equivalent to the collection key in the DLESE metadata
	 *      repository)
	 * @exception  Exception  Exception should be thrown to index this Document as an error
	 * @see                   org.apache.lucene.document.Document
	 */
	public void addFields(File file, Document newDoc, Document existingDoc, String docType, String docGroup)
		 throws Exception {

		File myMetaMetadataFile = getMetaMetadataFile(file);

		//System.out.println("myMetaMetadataFile: " + myMetaMetadataFile.getAbsolutePath()
		//	 + " myDocType: " + docType + " myCollection: " + docGroup);

		if (!myMetaMetadataFile.canRead()) {
			newDoc.add(new Field(FIELD_NS + "DiscoverableStatus", "discoverable", Field.Store.YES, Field.Index.ANALYZED));
			return;
		}
	}

}

