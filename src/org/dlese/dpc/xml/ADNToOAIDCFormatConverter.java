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
package org.dlese.dpc.xml;

import org.dlese.dpc.util.*;
import javax.servlet.ServletContext;
import org.dlese.dpc.webapps.tools.*;
import javax.xml.transform.Transformer;
import org.dlese.dpc.index.*;
import org.dlese.dpc.index.reader.*;
import java.io.*;
import java.util.*;

import org.dom4j.Document;
import org.dom4j.Node;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.io.SAXReader;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;


/**
 *  Converts from ADN format to the OAI DC format. Converts DLESE-specific IDs to URLs.
 *
 * @author    John Weatherley
 * @see       XMLConversionService
 */
public class ADNToOAIDCFormatConverter implements XMLDocumentFormatConverter {

	private File adn_to_oai_file = null;
	private File namespace_out_file = null;
	private SimpleLuceneIndex index = null;

	/**
	 *  Converts from the ADN format.
	 *
	 * @return    The String "adn".
	 */
	public String getFromFormat() {
		return "adn";
	}


	/**
	 *  Converts to the oai_dc format.
	 *
	 * @return    The String "oai_dc".
	 */
	public String getToFormat() {
		return "oai_dc";
	}


	/**
	 *  Gets the time this converter code was last modified. If unknown, this method should
	 *  return -1.
	 *
	 * @param  context  The context in which this is running.
	 * @return          The time this converter code was last modified.
	 */
	public long lastModified(ServletContext context) {
		getXFormFilesAndIndex(context);
		if(adn_to_oai_file.lastModified() > namespace_out_file.lastModified())
			return adn_to_oai_file.lastModified();
		else
			return namespace_out_file.lastModified();
	}


	/**
	 *  Performs XML conversion from ADN to oai_dc format. Characters are encoded as UTF-8.
	 *
	 * @param  xml        XML input in the 'adn' format.
	 * @param  docReader  A lucene doc reader for this record.
	 * @param  context    The servlet context where this is running.
	 * @return            XML in the converted 'oai_dc' format.
	 */
	public String convertXML(String xml, XMLDocReader docReader, ServletContext context) {
		getXFormFilesAndIndex(context);
		try {
			Transformer adn_to_oai_transformer = XSLTransformer.getTransformer(adn_to_oai_file.getAbsolutePath());
			Transformer namespace_out_transformer = XSLTransformer.getTransformer(namespace_out_file.getAbsolutePath());

			String transformed_content = XSLTransformer.transformString(xml, adn_to_oai_transformer);
			transformed_content = XSLTransformer.transformString(transformed_content, namespace_out_transformer);
						
			SAXReader reader = new SAXReader();
			Document document = DocumentHelper.parseText(transformed_content);

			// Replace IDs in the relation field only
			handleRelationField("//oai_dc:dc/dc:relation",document);
		
			// Dom4j automatically writes using UTF-8, unless otherwise specified.
			OutputFormat format = OutputFormat.createPrettyPrint();
			StringWriter outputWriter = new StringWriter();
			XMLWriter writer = new XMLWriter(outputWriter, format);
			writer.write(document);
			outputWriter.close();
			writer.close();
			return outputWriter.toString();			
		} catch (Throwable e) {
			System.err.println("ADNToOAIDCFormatConverter was unable to produce transformed file: " + e);
			e.printStackTrace();
			return "";
		}
	}

	private void handleRelationField(String xPathToField, Document xmlDocument)
	{
		List nodes = xmlDocument.selectNodes( xPathToField );
		if(nodes != null){
			for(int i = 0; i< nodes.size(); i++){
				Node currentNode = (Node)nodes.get(i);
				String relation = currentNode.getText();
				
				// Insert the URL if an ID is present...
				if(!relation.toLowerCase().matches(".*http\\:\\/\\/.*|.*ftp\\:\\/\\/.*")){
					String relatedID = relation.substring(relation.indexOf(" ")+1,relation.length());
					
					ResultDocList results = 
						index.searchDocs("id:" + SimpleLuceneIndex.encodeToTerm(relatedID));
					if(results == null || results.size() == 0){
						currentNode.detach();
					}else{
						String url = ((ItemDocReader)((ResultDoc)results.get(0)).getDocReader()).getUrl();
						currentNode.setText(relation.substring(0,relation.indexOf(" ") + 1) + url );				
					}				
				}
			}			
		}			
	}
	
	private void getXFormFilesAndIndex(ServletContext context){
		if(index == null)
			index = (SimpleLuceneIndex)context.getAttribute("index");
		if(adn_to_oai_file == null)
			adn_to_oai_file = new File(((String) context.getAttribute("xslFilesDirecoryPath")) +
				"/" + context.getInitParameter("adn-to-oai-dc-xsl"));
		if(namespace_out_file == null)
			namespace_out_file = new File(((String) context.getAttribute("xslFilesDirecoryPath")) +
				"/" + context.getInitParameter("namespace-out-xsl"));				
	}	
	
}

