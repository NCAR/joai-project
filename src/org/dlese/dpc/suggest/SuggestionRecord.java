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
package org.dlese.dpc.suggest;

import org.dlese.dpc.xml.schema.*;
import org.dlese.dpc.xml.*;

import java.io.*;
import java.util.*;
import java.net.URL;

import org.dom4j.Element;
import org.dom4j.Document;
import org.dom4j.DocumentException;

/**
 *  Wrapper providing accessors for a suggestion record, which is represented as
 *  a {@link org.dom4j.Document} of the framework determined by the SchemaHelper
 *  attribute.
 *
 * @author     Jonathan Ostwald
 * @version    $Id: SuggestionRecord.java,v 1.7 2009/03/20 23:34:00 jweather Exp $
 */
public abstract class SuggestionRecord {

	/**  NOT YET DOCUMENTED */
	protected Document doc = null;
	/**  NOT YET DOCUMENTED */
	protected DocMap docMap = null;
	SchemaHelper schemaHelper = null;



	/**
	 *  Constructor for the SuggestionRecord object
	 *
	 * @param  doc           a dom4j.Document
	 * @param  schemaHelper  a SchemaHelper for the doc's metadata format
	 */
	public SuggestionRecord(Document doc, SchemaHelper schemaHelper) {
		try {
			this.doc = doc;
			this.schemaHelper = schemaHelper;
			this.docMap = new DocMap(doc, schemaHelper);
		} catch (Exception e) {
			System.out.println(e);
		}
	}


	/**
	 *  Gets the doc attribute of the SuggestionRecord object
	 *
	 * @return    The doc value
	 */
	public Document getDoc() {
		return doc;
	}


	/**
	 *  Gets the docMap attribute of the SuggestionRecord object
	 *
	 * @return    The docMap value
	 */
	protected DocMap getDocMap() {
		return docMap;
	}


	/**
	 *  Assign the provided value to the XML Node specified by xpath.
	 *
	 * @param  xpath          NOT YET DOCUMENTED
	 * @param  value          NOT YET DOCUMENTED
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	protected void put(String xpath, String value) throws Exception {
		docMap.smartPut(xpath, value);
	}


	/**
	 *  Get the value of the node specified by provided xpath.
	 *
	 * @param  xpath  NOT YET DOCUMENTED
	 * @return        NOT YET DOCUMENTED
	 */
	protected String get(String xpath) {
		return (String) docMap.get(xpath);
	}

}

