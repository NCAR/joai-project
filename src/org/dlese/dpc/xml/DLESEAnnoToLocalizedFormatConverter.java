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
 *  Converts from DLESE Anno format to a localized DLESE Anno format, which contains
 *  no namespace declarations. Localized XML may be accessed using XPath notation without
 *  the local-name() function, making it easier to use.
 *
 * @author    John Weatherley
 * @see       XMLConversionService
 */
public class DLESEAnnoToLocalizedFormatConverter implements XMLFormatConverter {

	/**
	 *  Converts from the DLESE Anno format.
	 *
	 * @return    The String "dlese_anno".
	 */
	public String getFromFormat() {
		return "dlese_anno";
	}


	/**
	 *  Converts to the dlese_anno-localized format
	 *
	 * @return    The String "dlese_anno-localized".
	 */
	public String getToFormat() {
		return "dlese_anno-localized";
	}


	/**
	 *  Gets the time this converter code was last modified. If unknown, this method should
	 *  return -1.
	 *
	 * @param  context  Servlet context
	 * @return          The time this converter code was last modified.
	 */
	public long lastModified(ServletContext context) {
		return -1;
	}


	/**
	 *  Performs XML conversion from DLESE Anno to DLESE Anno  localized.
	 *
	 * @param  xml      XML input in the 'dlese_anno' format.
	 * @param  context  Servlet context
	 * @return          XML in the converted 'dlese_anno-localized' format.
	 */
	public String convertXML(String xml, ServletContext context) {
		//return Dom4jUtils.localizeXml(xml.replaceFirst("<\\?xml.+version=.+encoding=.+\\?>", ""), "itemRecord");
		// return Dom4jUtils.localizeXml(xml, "annotationRecord");
		return Dom4jUtils.localizeXml(xml);
	}

}

