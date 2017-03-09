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
 *  Implementations of this interface are used by the {@link XMLConversionService} to
 *  convert XML from one format to another.
 *
 * @author    John Weatherley
 * @see       XMLConversionService
 */
public interface XMLFormatConverter {

	/**
	 *  The metadataPrefix of the format from which this XMLFormatConverter converts, for
	 *  example "dlese_ims," "adn" or "oai_dc".
	 *
	 * @return    The metadataPrefix of the format being converted.
	 */
	public String getFromFormat();


	/**
	 *  The metadataPrefix of the format to which this XMLFormatConverter converts, for
	 *  example "dlese_ims," "adn" or "oai_dc".
	 *
	 * @return    The metadataPrefix of the format being output.
	 */
	public String getToFormat();


	/**
	 *  Performs XML conversion from the input format to the output format. This method
	 *  should retrun null if the conversion fails for any reason.
	 *
	 * @param  xml      XML input in the 'from' format.
	 * @param  context  The context in which this is running.
	 * @return          XML in the converted 'to' format.
	 */
	public String convertXML(String xml, ServletContext context);


	/**
	 *  Gets the time this converter code was last modified. If unknown, this method should
	 *  return -1.
	 *
	 * @param  context  The context in which this is running.
	 * @return          The time this converter code was last modified.
	 */
	public long lastModified(ServletContext context);

}

