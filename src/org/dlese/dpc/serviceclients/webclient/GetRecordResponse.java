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
package org.dlese.dpc.serviceclients.webclient;

import org.dlese.dpc.xml.*;

import java.util.*;
import java.io.*;

import org.dom4j.Document;
import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import org.dom4j.Node;

/**
 *  GetRecordResponse class wraps the response from the <a
 *  href="http://swiki.dpc.ucar.edu/Project-Discovery/108#GetRecord">GetRecord
 *  Web Service (version 1.0)</a>
 *
 *@author    ostwald
 */
public class GetRecordResponse {

	private static boolean debug = true;

	private Document document = null;
	private Document itemRecord = null;


	/**
	 *  Constructor for the GetRecordResponse object
	 *
	 *@param  responseStr  Description of the Parameter
	 */
	public GetRecordResponse(String responseStr) {
		String localizedResponseStr = Dom4jUtils.localizeXml(responseStr, "itemRecord");
		try {
			document = Dom4jUtils.getXmlDocument(localizedResponseStr);
		} catch (Exception e) {
			prtln("failed to parse xml: " + e);
			return;
		}
	}


	/**
	 *  Gets the document attribute of the GetRecordResponse object. This is a
	 *  {@link org.dom4j.Document} representation of the GetRecord Service
	 *  response.
	 *
	 *@return    The document value
	 */
	public Document getDocument() {
		return document;
	}


	/**
	 *  Gets the itemRecord attribute of the GetRecordResponse class. The
	 *  itemRecord is the ADN item-level record contained in the GetRecord Service
	 *  Response.
	 *
	 *@return                                The itemRecord value
	 *@exception  WebServiceClientException  Description of the Exception
	 */
	public Document getItemRecord()
		throws WebServiceClientException {
		if (itemRecord == null) {
			// now extract the itemRecord element
			String rootXPath = "/DDSWebService/GetRecord/record/metadata/itemRecord";
			try {
				Node rootNode = document.selectSingleNode(rootXPath);
				if (rootNode == null) {
					String msg = "getItemRecord() failed to find itemRecord";
					prtln(msg);
					throw new WebServiceClientException(msg);
					// prtln(Dom4jUtils.prettyPrint(document));
				}

				// clone the root and create a new document with it
				Element rootClone = (Element) rootNode.clone();
				itemRecord = DocumentFactory.getInstance().createDocument(rootClone);
			} catch (Throwable e) {
				throw new WebServiceClientException(e.getMessage());
			}
		}
		return itemRecord;
	}


	/**
	 *  Description of the Method
	 *
	 *@param  s  Description of the Parameter
	 */
	private static void prtln(String s) {
		if (debug) {
			System.out.println(s);
		}
	}
}

