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

import java.util.*;
import org.dlese.dpc.repository.*;
import org.dlese.dpc.index.*;
import org.dlese.dpc.index.writer.xml.*;

/**
 *  Factory used to create the XmlFileIndexingWriter appropriate for handling a given XML format.
 *
 * @author     John Weatherley
 * @see        XMLFileIndexingWriter
 */
public class XMLFileIndexingWriterFactory {

	private RecordDataService recordDataService = null;
	private SimpleLuceneIndex index = null;
	private XMLIndexerFieldsConfig xmlIndexerFieldsConfig = null;

	/**  A HashMap of FileIndexingService Classes */
	private static HashMap indexerClasses = null;

	// Register new XMLFileIndexingWriters here:
	private final static Object[][] INDEXING_WRITER_CLASSES = {
		{"default_handler_class", SimpleXMLFileIndexingWriter.class},
		{"adn", ADNFileIndexingWriter.class},
		{"dlese_ims", DleseIMSFileIndexingWriter.class},
		{"dlese_anno", DleseAnnoFileIndexingServiceWriter.class},
		{"dlese_collect", DleseCollectionFileIndexingWriter.class},
		{"news_opps", NewsOppsFileIndexingWriter.class},
		{"ncs_collect", NCSCollectionFileIndexingWriter.class}
		};

	// Initialize the HashMap of index writer classes
	static {
		indexerClasses = new HashMap(INDEXING_WRITER_CLASSES.length);
		for (int i = 0; i < INDEXING_WRITER_CLASSES.length; i++)
			indexerClasses.put(INDEXING_WRITER_CLASSES[i][0], INDEXING_WRITER_CLASSES[i][1]);
	}


	/**
		 * Adds additional classes of XMLFileIndexingWriters
		 * Key are the xmlformat, value the Class
		 * 
		 * @param ht
		 */
		private void addAdditionalIndices(Hashtable<String,Class> ht) {
			
			if (ht == null) return;
			Enumeration<String> e = ht.keys();
			 
			//iterate through Hashtable keys Enumeration
			while(e.hasMoreElements()) {
				String format = e.nextElement();
				indexerClasses.put(format, ht.get(format));
			}
			
		}
		


	/**
	 *  Constructor for use when no RecordDataService is needed.
	 *
	 * @param  index  The index being used
	 */
	public XMLFileIndexingWriterFactory(SimpleLuceneIndex simpleLuceneIndex, XMLIndexerFieldsConfig xmlIndexerFieldsConfig) { 
		index = simpleLuceneIndex;
		this.xmlIndexerFieldsConfig = xmlIndexerFieldsConfig;
	}


	/**
	 *  Constructor for use when a RecordDataService is needed.
	 *
	 * @param  rds                The RecordDataService being used, or null if none needed.
	 * @param  simpleLuceneIndex  The index being used
	 * @param additionalIndexers 
	 */
	public XMLFileIndexingWriterFactory(RecordDataService rds, SimpleLuceneIndex simpleLuceneIndex, XMLIndexerFieldsConfig xmlIndexerFieldsConfig, Hashtable additionalIndexers) {
		index = simpleLuceneIndex;
		recordDataService = rds;
		this.xmlIndexerFieldsConfig = xmlIndexerFieldsConfig;
		addAdditionalIndices(additionalIndexers);
		
	}

	/**
	 *  Constructor for use when a RecordDataService is needed.
	 *
	 * @param  rds                The RecordDataService being used, or null if none needed.
	 * @param  simpleLuceneIndex  The index being used
	 */
	public XMLFileIndexingWriterFactory(RecordDataService rds, SimpleLuceneIndex simpleLuceneIndex, XMLIndexerFieldsConfig xmlIndexerFieldsConfig) {
		index = simpleLuceneIndex;
		recordDataService = rds;
		this.xmlIndexerFieldsConfig = xmlIndexerFieldsConfig;
	}

		/**
		 * returns a FileIndexing Class that can be instantiated 
		 * @param xmlFormat
		 * @return
		 */
		public Class getIndexingWriterClass(String xmlFormat) {
			
			Class writerClass = (Class) indexerClasses.get(xmlFormat);
			if (writerClass == null)
				writerClass = (Class) indexerClasses.get("default_handler_class");

			return writerClass;
		}
	

	/**
	 *  Gets the XML indexingWriter appropriate for indexing the given xml format.
	 *
	 * @param  collection     The collection key, for example dcc, comet, etc.
	 * @param  xmlFormat      The xml format specifier, for example adn, news_opps, dlese_collect.
	 * @return                The indexingWriter value
	 * @exception  Exception  If error creating the writer
	 */
	public XMLFileIndexingWriter getIndexingWriter(String collection, String xmlFormat)
		 throws Exception {
			 
		//System.out.println("getIndexingWriter()");
		
		Class writerClass = getIndexingWriterClass(xmlFormat);

		XMLFileIndexingWriter xw = (XMLFileIndexingWriter) writerClass.newInstance();
		
		HashMap writerConfigAttributes = new HashMap(5);
		//System.out.println("collection: " + collection + " xmlFormat: " + xmlFormat);
		writerConfigAttributes.put("collection", collection);
		writerConfigAttributes.put("xmlFormat", xmlFormat);
		if(recordDataService != null)
			writerConfigAttributes.put("recordDataService", recordDataService);
		writerConfigAttributes.put("index", index);
		if(xmlIndexerFieldsConfig != null) {
			writerConfigAttributes.put("xmlIndexerFieldsConfig", xmlIndexerFieldsConfig);
			//System.out.println("xmlIndexerFieldsConfig NOT null!");
		}
		/* else
			System.out.println("xmlIndexerFieldsConfig is null!"); */
		xw.setConfigAttributes(writerConfigAttributes);
		return xw;
	}
}

