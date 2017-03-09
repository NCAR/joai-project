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
import org.dom4j.tree.AbstractElement;

/**
 *  Converts from ADN format to the NSDL DC format. Converts DLESE-specific IDs to URLs.
 *
 * @author    John Weatherley
 * @see       XMLConversionService
 */
public class ADNToNSDLDCFormatConverter implements XMLDocumentFormatConverter {

	private File adn_to_nsdl_file = null;
	private File namespace_out_file = null;
	private SimpleLuceneIndex index = null;

	// Fields in NSDL_DC format that may contain IDs that should be replaced with URLs
	private final static String[] fieldsToReplace = {
		"dct:isVersionOf",
		"dct:hasVersion",
		"dct:isReplacedBy",
		"dct:replaces",
		"dct:isRequiredBy",
		"dct:requires",
		"dct:isPartOf",
		"dct:hasPart",
		"dct:isReferencedBy",
		"dct:references",
		"dct:isFormatOf",
		"dct:conformsTo"
		};


	/**
	 *  Converts from the ADN format.
	 *
	 * @return    The String "adn".
	 */
	public String getFromFormat() {
		return "adn";
	}


	/**
	 *  Converts to the nsdl_dc format.
	 *
	 * @return    The String "nsdl_dc".
	 */
	public String getToFormat() {
		return "nsdl_dc";
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
		if (adn_to_nsdl_file.lastModified() > namespace_out_file.lastModified())
			return adn_to_nsdl_file.lastModified();
		else
			return namespace_out_file.lastModified();
	}


	/**
	 *  Performs XML conversion from ADN to nsdl_dc format. Characters are encoded as UTF-8.
	 *
	 * @param  xml        XML input in the 'adn' format.
	 * @param  context    The context in which this is running.
	 * @param  docReader  Lucene DocReader for the item.
	 * @return            XML in the converted 'nsdl_dc' format.
	 */
	public String convertXML(String xml, XMLDocReader docReader, ServletContext context) {
		getXFormFilesAndIndex(context);
		try {
			Transformer adn_to_nsdl_transformer = XSLTransformer.getTransformer(adn_to_nsdl_file.getAbsolutePath());
			Transformer namespace_out_transformer = XSLTransformer.getTransformer(namespace_out_file.getAbsolutePath());

			String transformed_content = XSLTransformer.transformString(xml, adn_to_nsdl_transformer);
			transformed_content = XSLTransformer.transformString(transformed_content, namespace_out_transformer);

			SAXReader reader = new SAXReader();
			Document document = DocumentHelper.parseText(transformed_content);

			// Replace IDs with URL in each field as appropriate
			for (int i = 0; i < fieldsToReplace.length; i++)
				handleRelationField("//nsdl_dc:nsdl_dc/" + fieldsToReplace[i], document);

			// Dom4j automatically writes using UTF-8, unless otherwise specified.
			OutputFormat format = OutputFormat.createPrettyPrint();
			StringWriter outputWriter = new StringWriter();
			XMLWriter writer = new XMLWriter(outputWriter, format);
			writer.write(document);
			outputWriter.close();
			writer.close();
			return outputWriter.toString();
		} catch (Throwable e) {
			System.err.println("ADNToNSDLDCFormatConverter was unable to produce transformed file: " + e);
			e.printStackTrace();
			return "";
		}
	}


	private void handleRelationField(String xPathToField, Document xmlDocument)
		 throws Throwable {
		// Only process elements that have attribute xsi:type=dc:URI
		List nodes = xmlDocument.selectNodes(xPathToField + "[@xsi:type='dct:URI']");
		if (nodes != null) {
			for (int i = 0; i < nodes.size(); i++) {
				AbstractElement currentElement = (AbstractElement) nodes.get(i);

				String relation = currentElement.getText();

				// Insert the URL if an ID is present...
				if (!relation.toLowerCase().matches(".*http\\:\\/\\/.*|.*ftp\\:\\/\\/.*")) {
					ResultDocList results =
						index.searchDocs("id:" + SimpleLuceneIndex.encodeToTerm(relation));
					if (results == null || results.size() == 0) {
						currentElement.detach();
					}
					else {
						String url = ((ItemDocReader) ((ResultDoc)results.get(0)).getDocReader()).getUrl();
						currentElement.setText(url);
					}
				}
			}
		}
	}


	private void getXFormFilesAndIndex(ServletContext context) {
		if (index == null)
			index = (SimpleLuceneIndex) context.getAttribute("index");
		if (adn_to_nsdl_file == null)
			adn_to_nsdl_file = new File(((String) context.getAttribute("xslFilesDirecoryPath")) +
				"/" + context.getInitParameter("adn-to-nsdl-dc-xsl"));
		if (namespace_out_file == null)
			namespace_out_file = new File(((String) context.getAttribute("xslFilesDirecoryPath")) +
				"/" + context.getInitParameter("namespace-out-xsl"));
	}
}

