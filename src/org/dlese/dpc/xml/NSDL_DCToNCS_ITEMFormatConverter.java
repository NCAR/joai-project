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
 *  Converts from NSDL_DC format to the NCS_ITEM format. NOTE: assumes
 *  nsdl_dc is version 1.02 and first transforms to nsdl_dc 1.03 before
 *  converting to ncs_item.
 *
 * @author     Ostwald
 * @version    $Id: NSDL_DCToNCS_ITEMFormatConverter.java,v 1.3 2009/03/20 23:34:01 jweather Exp $
 * @see        XMLConversionService
 */
public class NSDL_DCToNCS_ITEMFormatConverter implements XMLDocumentFormatConverter {

	/**  NOT YET DOCUMENTED */
	protected File format_transform_file = null;
	/**  NOT YET DOCUMENTED */
	protected File version_transform_file = null;
	/**  NOT YET DOCUMENTED */
	protected SimpleLuceneIndex index = null;


	/**
	 *  Converts from the ncs_item format.
	 *
	 * @return    The String "nsdl_dc".
	 */
	public String getFromFormat() {
		return "nsdl_dc";
	}


	/**
	 *  Converts to the nsdl_dc format.
	 *
	 * @return    The String "ncs_item".
	 */
	public String getToFormat() {
		return "ncs_item";
	}


	/**
	 *  Gets the time this converter code was last modified. If unknown, this
	 *  method should return -1.
	 *
	 * @param  context  The context in which this is running.
	 * @return          The time this converter code was last modified.
	 */
	public long lastModified(ServletContext context) {
		getXFormFilesAndIndex(context);
		return format_transform_file.lastModified();
	}


	/**
	 *  Performs XML conversion from ADN to oai_dc format. Characters are encoded
	 *  as UTF-8.
	 *
	 * @param  xml        XML input in the 'adn' format.
	 * @param  docReader  A lucene doc reader for this record.
	 * @param  context    The servlet context where this is running.
	 * @return            XML in the converted 'oai_dc' format.
	 */
	public String convertXML(String xml, XMLDocReader docReader, ServletContext context) {
		getXFormFilesAndIndex(context);

		System.out.println("xml: " + xml);

		String xsl2Transformer = "net.sf.saxon.TransformerFactoryImpl";

		try {
			Transformer version_transformer = XSLTransformer.getTransformer(version_transform_file.getAbsolutePath());
			Transformer format_transformer =
				XSLTransformer.getTransformer(format_transform_file.getAbsolutePath(), xsl2Transformer);

			/* 
				when working with nsdl-dc-v1.03 source we had to first transform to nsdl-dc-v1.02 before
				applying format transform to ncs-item.
			*/
			// String transformed_content = XSLTransformer.transformString(xml, version_transformer);
			// transformed_content = XSLTransformer.transformString(transformed_content, format_transformer);

			/*
				when working with nsdl-dc-v1.02-020 source - no nsdl-dc version transform is required
			*/
			String transformed_content = XSLTransformer.transformString(xml, format_transformer);
			
			SAXReader reader = new SAXReader();
			Document document = DocumentHelper.parseText(transformed_content);

			// Dom4j automatically writes using UTF-8, unless otherwise specified.
			OutputFormat format = OutputFormat.createPrettyPrint();
			StringWriter outputWriter = new StringWriter();
			XMLWriter writer = new XMLWriter(outputWriter, format);
			writer.write(document);
			outputWriter.close();
			writer.close();
			return outputWriter.toString();
		} catch (Throwable e) {
			System.err.println("NSDL_DCToNCS_ITEMFormatConverter was unable to produce transformed file: " + e);
			e.printStackTrace();
			return "";
		}
	}


	/**
	 *  Gets the xFormFilesAndIndex attribute of the NSDL_DCToNCS_ITEMFormatConverter
	 *  object
	 *
	 * @param  context  NOT YET DOCUMENTED
	 */
	protected void getXFormFilesAndIndex(ServletContext context) {
		if (index == null)
			index = (SimpleLuceneIndex) context.getAttribute("index");
		if (format_transform_file == null)
			format_transform_file = new File(((String) context.getAttribute("xslFilesDirecoryPath")) +
				"/" + context.getInitParameter("nsdl-dc-to-ncs-item-xsl"));
		if (version_transform_file == null)
			version_transform_file = new File(((String) context.getAttribute("xslFilesDirecoryPath")) +
				"/" + context.getInitParameter("nsdl-dc-v1.02-to-nsdl-dc-v1.03-xsl"));
	}

}

