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
package org.dlese.dpc.schemedit.standards;

import org.dlese.dpc.xml.schema.*;
import org.dlese.dpc.schemedit.*;

import java.io.*;
import java.util.*;

import java.net.*;

/**
 *  Manages information about a standards document in the context of a {@link
 *  SuggestionServiceHelper}. The standards within a document are represented as
 *  a {@link StandardsDocument}.
 *
 * @author    ostwald
 */
public interface StandardsManager {

	/**
	 *  Gets the xmlFormat attribute of the StandardsManager object
	 *
	 * @return    The xmlFormat value
	 */
	public String getXmlFormat();


	/**
	 *  Gets the xpath attribute of the StandardsManager object
	 *
	 * @return    The xpath value
	 */
	public String getXpath();


	/**
	 *  The name of the JSP tag that will render the standards hierarchy
	 *
	 * @return    The rendererTag value
	 */
	public String getRendererTag();

}

