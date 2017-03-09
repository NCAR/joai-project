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
package org.dlese.dpc.index.writer.xml;

import java.io.*;
import java.util.*;
import java.text.*;
import java.net.URL;
import org.dom4j.*;

import org.dlese.dpc.xml.*;
import org.dlese.dpc.util.*;
import org.dlese.dpc.index.analysis.PerFieldAnalyzer;

/**
 *  Holds a configuration for XML fields indexed by XMLIndexer and used in PerFieldAnalyzer.
 *
 * @author    John Weatherley
 * @see       XMLIndexer
 */
public class XMLIndexerFieldsConfig {
	/*
		TO DO:
		- add config for indexing in XMLIndexer
		- document
	*/
	private Document configIndexXmlDoc = null;
	private Map formatConfigDocs = new TreeMap();
	private Map fieldAnalyzers = new TreeMap();


	/**
	 * @param  configIndexUrl  URL to the config index file.
	 * @exception  Exception   If error
	 */
	public XMLIndexerFieldsConfig(URL configIndexUrl) throws Exception {

		configIndexXmlDoc = Dom4jUtils.localizeXml(Dom4jUtils.getXmlDocument(configIndexUrl));

		// Loop through each format's configuration file:
		List nodes = configIndexXmlDoc.selectNodes("/XMLIndexerFieldsConfigIndex/configurationFiles/configurationFile");
		if (nodes != null) {
			for (int i = 0; i < nodes.size(); i++) {
				try {
					URL confUrl = new URL(configIndexUrl, ((Node) nodes.get(i)).getText().trim());
					Document confDoc = Dom4jUtils.localizeXml(Dom4jUtils.getXmlDocument(confUrl));
					//prtln("confDoc: " + confDoc.asXML());
					String xmlFormat = confDoc.valueOf("/XMLIndexerFieldsConfig/@xmlFormat").trim();
					String schema = confDoc.valueOf("/XMLIndexerFieldsConfig/@schema").trim();
					if (xmlFormat.length() > 0 && schema.trim().length() > 0) {
						prtlnErr("Not valid to specify both an XML format and a schema. Skipping xmlFormat: '" + xmlFormat + "' schema: '" + schema + "'");
						continue;
					}
					else if (xmlFormat.length() > 0)
						formatConfigDocs.put(xmlFormat, confDoc);
					else if (schema.length() > 0)
						formatConfigDocs.put(schema, confDoc);
					
					// Loop through all custom fields and extract their type/analyzers:
					List fields = confDoc.selectNodes("/XMLIndexerFieldsConfig/customFields/customField");
					for (int j = 0; j < fields.size(); j++) { 
						Node field = (Node) fields.get(j);
						String fieldName = field.valueOf("@name").trim();
						String type = field.valueOf("@type").trim();
						String analyzer = field.valueOf("@analyzer").trim();
						if (fieldName.length() > 0) {
							if (analyzer.length() > 0)
								fieldAnalyzers.put(fieldName, analyzer);
							else {
								if (type.equals("text"))
									fieldAnalyzers.put(fieldName, PerFieldAnalyzer.TEXT_ANALYZER);
								else if (type.equals("key"))
									fieldAnalyzers.put(fieldName, PerFieldAnalyzer.KEYWORD_ANALYZER);
								else if (type.equals("stems"))
									fieldAnalyzers.put(fieldName, PerFieldAnalyzer.STEMS_ANALYZER);
								else
									throw new Exception("Valid field type or analyzer must be specified for field '" + fieldName + "'. Found type:'" + type + "' analyzer:'" + analyzer + "'");
							}
						}
					}
					//prtln("fieldAnalyzers: " + fieldAnalyzers + " size: " + fieldAnalyzers.size());
				} catch (Exception e) {
					prtlnErr("Error processing configuration file: '" + configIndexUrl + "'");
					throw e;
				}
			}
		}
	}


	/**
	 *  Gets a Map of field/analyzer pairs where keys are field or schema names and values are the corresponding Analyzer
	 *  class names as Strings for the custom fields that are defined in this configuration.
	 *
	 * @return    Map of field/analyzer pairs
	 */
	public Map getFieldAnalyzers() {
		return fieldAnalyzers;
	}

	/**
	 *  Gets the configuration Document for a given xmlFormat or schema. Example xmlFormat keys are 'oai_dc',
	 *  'library_dc'. An example schema is 'http://www.openarchives.org/OAI/2.0/oai_dc.xsd'.
	 *
	 * @param  xmlFormatOrSchema  An xmlFormat key or schema location for the format
	 * @return                    A configuration Document or null if not available
	 */
	public Document getFormatConfig(String xmlFormatOrSchema) {
		return (Document)formatConfigDocs.get(xmlFormatOrSchema);
	}
	
	/**
	 *  Determine if the given xmlFormat or schema has a configuration. Example xmlFormat keys are 'oai_dc',
	 *  'library_dc'. An example schema is 'http://www.openarchives.org/OAI/2.0/oai_dc.xsd'.
	 *
	 * @param  xmlFormatOrSchema  An xmlFormat key or schema location for the format
	 * @return                    True if the xmlFormat or schema has a configuration
	 */
	public boolean formatIsConfigured(String xmlFormatOrSchema) {
		return formatConfigDocs.containsKey(xmlFormatOrSchema);
	}


	private void prtln(String s) {
		System.out.println("XMLIndexerFieldsConfig: " + s);
	}


	private void prtlnErr(String s) {
		System.err.println("XMLIndexerFieldsConfig ERROR: " + s);
	}

}

