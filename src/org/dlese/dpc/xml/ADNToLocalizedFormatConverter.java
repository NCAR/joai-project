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

/**
 *  Converts from ADN format to a localized ADN format, which contains no namespace declarations. 
 Localized XML may be accessed using XPath notation without the local-name() function, making it 
 easier to use.
 *
 * @author    John Weatherley
 * @see       XMLConversionService
 */
public class ADNToLocalizedFormatConverter implements XMLFormatConverter {

	/**
	 *  Converts from the ADN format.
	 *
	 * @return    The String "adn".
	 */
	public String getFromFormat() {
		return "adn";
	}


	/**
	 *  Converts to the adn-localized format
	 *
	 * @return    The String "adn-localized".
	 */
	public String getToFormat() {
		return "adn-localized";
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
	 *  Performs XML conversion from ADN to ADN localized.
	 *
	 * @param  xml  XML input in the 'adn' format.
	 * @return      XML in the converted 'adn-localized' format.
	 */
	public String convertXML(String xml,ServletContext context) {
		return Dom4jUtils.localizeXml(xml);	
	}

}

