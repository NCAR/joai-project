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
package org.dlese.dpc.serviceclients.remotesearch;

import org.dlese.dpc.serviceclients.remotesearch.reader.*;
import org.dlese.dpc.serviceclients.webclient.*;

import java.util.*;
import org.dom4j.Element;
import org.dom4j.Document;

/**
 *  This class wraps the individual items that are returned by {@link
 *  RemoteSearcher#searchDocs(String)}. It is patterned after the {@link
 *  org.dlese.dpc.index.ResultDoc} class.
 *
 *@author    ostwald
 */
public class RemoteResultDoc implements java.io.Serializable {

	private static boolean debug = true;

	private RemoteSearcher rs = null;
	private String id = null;
	private String url = null;
	private String collection = null;
	private GetRecordResponse doc = null;
	private ADNItemDocReader docReader = null;


	/**
	 *  Constructor for the RemoteResultDoc object. Used to create a
	 *  RemoteResultDoc from an <b>alsoCatalogedBy</b> element (see {@link
	 *  RemoteSearcher#searchDocs})
	 *
	 *@param  id          id of item
	 *@param  url         url of item
	 *@param  collection  collection label of item
	 *@param  rs          instance of {@link RemoteSearcher}
	 */
	public RemoteResultDoc(String id, String url, String collection, RemoteSearcher rs) {
		prtln("constructor with id: " + id);
		this.rs = rs;
		this.id = id;
		this.url = url;
		this.collection = collection;
	}


	/**
	 *  Construct a RemoteResultDoc from a MatchingRecord element (of the reponse
	 *  from the UrlCheck Web Service)
	 *
	 *@param  record  matching record {@link org.dom4j.Element}
	 *@param  rs      instance of {@link RemoteSearcher}
	 */
	public RemoteResultDoc(Element record, RemoteSearcher rs) {
		// prtln ("constructor with element:\n\t " + e.asXML());
		this.rs = rs;
		url = record.element("url").getText();
		Element head = record.element("head");
		try {
			Element collectionElement = (Element) head.selectSingleNode("collection");
			collection = collectionElement.getText();
			id = head.selectSingleNode("id").getText();
		} catch (Throwable te) {
			prtln("RemoteResultDoc unable to parse element: " + te.getMessage());
		}
	}


	/**
	 *  Gets the id attribute of the RemoteResultDoc object (identifying the
	 *  resource within DDS)
	 *
	 *@return    The id value
	 */
	public String getId() {
		return id;
	}

	/**
	 *  Gets the doctype attribute of the RemoteResultDoc object (hardcoded to
	 *  "adn")
	 *
	 *@return    The doctype value
	 */
	public final String getDoctype() {
		return "adn";
	}


	/**
	 *  Gets the readerClass attribute of the RemoteResultDoc object
	 *
	 *@return    The readerClass value
	 */
	public final String getReaderClass() {
		return "ADNItemDocReader";
	}


	/**
	 *  Gets the docReader attribute of the RemoteResultDoc object which supports
	 *  display of RemoteResultDoc instances via JSP (see {@link
	 *  ADNItemDocReader}).
	 *
	 *@return    The docReader value
	 */
	public ADNItemDocReader getDocReader() {
		if (docReader == null) {
			try {
				prtln("getDocReader() creating new docReader for id=" + id);
				docReader = new ADNItemDocReader(id, getDocument(), rs.getVocab());
			} catch (Exception e) {
				System.err.println("Error reading document: " + e);
				e.printStackTrace();
				return null;
			}
		}
		else {
			// prtln ("getDocReader() returning existing docReader");
		}
		return docReader;
	}


	/**
	 *  Gets the document attribute of the RemoteResultDoc object. The doc
	 *  attribute is obtained from the {@link RemoteSearcher} instance and then
	 *  cached for future access.
	 *
	 *@return    The document value
	 */
	public GetRecordResponse getDocument() {
		try {
			if (doc == null) {
				prtln("getDocument() document was null ... fetching");
				doc = rs.getDocument(id);
			}
			// prtln ("getDocument() returning existing doc");
			return doc;
		} catch (Throwable e) {
			System.err.println("Error retrieving document: " + e);
			return null;
		}
	}


	/**
	 *  Gets the url attribute of the RemoteResultDoc object, which pionts to the
	 *  resource on the web.
	 *
	 *@return    The url value
	 */
	public String getUrl() {
		return url;
	}


	/**
	 *  Gets the collection attribute of the RemoteResultDoc object
	 *
	 *@return    The collection value (a human readable string)
	 */
	public String getCollection() {
		return collection;
	}


	/**
	 *  A printable representation - used for debugging
	 *
	 *@return    Description of the Return Value
	 */
	public String toString() {
		String s = "\nMatch:";
		s += "\n\tid: " + id;
		s += "\n\turl: " + url;
		s += "\n\tcollection: " + collection;
		// s += "\n\tcollectionKey: " + collectionKey;
		// s += "\n\tsystemCollectionKey: " + systemCollectionKey;
		return s;
	}


	/**
	 *  Print a line to standard out.
	 *
	 *@param  s  The String to print.
	 */
	private void prtln(String s) {
		if (debug) {
			System.out.println("RemoteResultDoc: " + s);
		}
	}

}

