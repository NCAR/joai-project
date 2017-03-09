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
package org.dlese.dpc.schemedit;

import org.dom4j.*;
import java.util.*;
import org.dlese.dpc.xml.schema.SchemaHelper;
import org.dlese.dpc.xml.schema.DocMap;
import org.dlese.dpc.xml.Dom4jUtils;


/**
 *  NOT YET DOCUMENTED
 *
 * @author     Jonathan Ostwald
 * @version    $Id: MetaDataHelper.java,v 1.3 2009/03/20 23:33:55 jweather Exp $
 */
public class MetaDataHelper {

	/**  NOT YET DOCUMENTED */
	public static boolean debug = true;


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  id                   NOT YET DOCUMENTED
	 * @param  pathValueMap         NOT YET DOCUMENTED
	 * @param  collectionFramework  NOT YET DOCUMENTED
	 * @return                      NOT YET DOCUMENTED
	 * @exception  Exception        NOT YET DOCUMENTED
	 */
	public static Document makeCollectionDoc(String id, Map pathValueMap, MetaDataFramework collectionFramework)
		 throws Exception {

		SchemaHelper schemaHelper = collectionFramework.getSchemaHelper();
		if (schemaHelper == null) {
			prtln("makeCollectionDoc: schemaHelper is null!");
		}

		// create a miminal document and then insert values from csForm
		Document doc = collectionFramework.makeMinimalRecord(id);

		// use docMap as wraper for Document
		DocMap docMap = new DocMap(doc, schemaHelper);
		// load the dlese-collect metadata document with values from the form
		// pattern for smartPut: docMap.smartPut(xpath, value)
		for (Iterator i = pathValueMap.keySet().iterator(); i.hasNext(); ) {
			try {
				String xpath = (String) i.next();
				String val = (String) pathValueMap.get(xpath);
				docMap.smartPut(xpath, val);
			} catch (Exception e) {
				prtln(e.getMessage());
			}
		}

		try {
			// now prepare document to write to file by inserting namespace information
			doc = collectionFramework.getWritableRecord (doc);

		} catch (Exception e) {
			throw new Exception("makeCollection error: " + e);
		}
		return doc;
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  s  NOT YET DOCUMENTED
	 */
	private static void prtln(String s) {
		if (debug) {

			while (s.length() > 0 && s.charAt(0) == '\n') {
				System.out.println("");
				s = s.substring(1);
			}

			System.out.println(s);
		}
	}
}


