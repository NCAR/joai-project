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

import javax.servlet.ServletContext;

/**
 *  A simple demonstraion implementation of the {@link XMLFormatConverter} interface. Just
 *  appends a comment to the end of the XML content. Can be used on any XML format for
 *  demonstration and testing.
 *
 * @author    John Weatherley
 * @see       XMLConversionService
 */
public class SimpleXMLFormatConverter implements XMLFormatConverter {

	/**
	 *  This converter can convert from any format.
	 *
	 * @return    An empty string, since there is no format association.
	 */
	public String getFromFormat() {
		return "";
	}


	/**
	 *  This converter can convert to any format.
	 *
	 * @return    An empty string, since there is no format association.
	 */
	public String getToFormat() {
		return "";
	}

	/**
	 *  Gets the time this converter code was last modified. If unknown, this method should
	 *  return -1.
	 *
	 * @return    The time this converter code was last modified.
	 */
	public long lastModified(ServletContext context)
	{
		return -1;				
	}
	
	/**
	 *  Performs XML conversion from the input format to the output format by simply adding a
	 *  coment to the end of the input XML record.
	 *
	 * @param  xml  XML input in the 'from' format.
	 * @return      XML in the converted 'to' format.
	 */
	public String convertXML(String xml, ServletContext context) {
		return xml + "\n<!-- SimpleXMLFormatConverter created this XML -->";
	}

}

