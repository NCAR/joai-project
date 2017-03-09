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
package org.dlese.dpc.serviceclients.cat;

import org.dlese.dpc.schemedit.SchemEditUtils;
import org.dlese.dpc.xml.Dom4jUtils;
import org.dlese.dpc.util.Files;
import org.dom4j.*;
import java.util.*;
import java.net.*;

/**
 *  Wraps standard element returned by SuggestCATStandards call.
 *
 * @author    Jonathan Ostwald
 */
public class CATStandard {

	private static boolean debug = true;
	private static String defaultValue = null;
	private Element element;
	private String identifier;
	private String author;
	private String topic;
	private String gradeLevels;
	private String benchmark;


	/**
	 *  Constructor for the CATStandard object
	 *
	 * @param  e  NOT YET DOCUMENTED
	 */
	public CATStandard(Element e) {
		this.element = e;
	}


	/**
	 *  Gets the elementText attribute of the CATStandard object
	 *
	 * @param  tag  NOT YET DOCUMENTED
	 * @return      The elementText value
	 */
	private String getElementText(String tag) {
		return getElementText(tag, this.defaultValue);
	}

	public Element getElement () {
		return this.element;
	}
	
	/**
	 *  Gets the elementText attribute of the CATStandard object
	 *
	 * @param  tag           NOT YET DOCUMENTED
	 * @param  defaultValue  NOT YET DOCUMENTED
	 * @return               The elementText value
	 */
	private String getElementText(String tag, String defaultValue) {
		try {
			return this.element.elementTextTrim(tag);
		} catch (Throwable t) {}
		return defaultValue;
	}


	/**
	 *  Gets the asnId attribute of the CATStandard object
	 *
	 * @return    The asnId value
	 */
	public String getAsnId() {
		return this.getIdentifier();
	}


	/**
	 *  Gets the identifier attribute of the CATStandard object
	 *
	 * @return    The identifier value
	 */
	public String getIdentifier() {
		return getElementText("Identifier");
	}


	/**
	 *  Gets the author attribute of the CATStandard object
	 *
	 * @return    The author value
	 */
	public String getAuthor() {
		return getElementText("Author");
	}


	/**
	 *  Gets the topic attribute of the CATStandard object
	 *
	 * @return    The topic value
	 */
	public String getTopic() {
		return getElementText("Topic");
	}


	/**
	 *  Gets the gradeLevels attribute of the CATStandard object
	 *
	 * @return    The gradeLevels value
	 */
	public String getGradeLevels() {
		return getElementText("GradeLevels");
	}


	/**
	 *  Gets the text attribute of the CATStandard object
	 *
	 * @return    The text value
	 */
	public String getText() {
		return getElementText("Text");
	}


	/**
	 *  Gets the benchmark attribute of the CATStandard object
	 *
	 * @return    The benchmark value
	 */
	public String getBenchmark() {
		return getElementText("Benchmark");
	}


	/**
	 *  Description of the Method
	 *
	 * @param  node  Description of the Parameter
	 */
	private static void pp(Node node) {
		prtln(Dom4jUtils.prettyPrint(node));
	}


	/**
	 *  Description of the Method
	 *
	 * @param  s  Description of the Parameter
	 */
	private static void prtln(String s) {
		String prefix = null;
		if (debug) {
			SchemEditUtils.prtln(s, prefix);
		}
	}

}

