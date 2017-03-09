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
public class NCS_COLLECTToNSDL_DCFormatConverter extends NCS_ITEMToNSDL_DCFormatConverter {

	
	/**
	 *  Converts from the ADN format.
	 *
	 * @return    The String "adn".
	 */
	public String getFromFormat() {
		return "ncs_collect";
	}

	protected void getXFormFilesAndIndex(ServletContext context){
		if(index == null)
			index = (SimpleLuceneIndex)context.getAttribute("index");
		if(transform_file == null)
			transform_file = new File(((String) context.getAttribute("xslFilesDirecoryPath")) +
				"/" + context.getInitParameter("ncs-collect-to-nsdl-dc-xsl"));
	}
	
}

