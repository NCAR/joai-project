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
package org.dlese.dpc.ndr.apiproxy;

import java.util.*;
import org.dlese.dpc.schemedit.*;
import org.dlese.dpc.xml.Dom4jUtils;
import org.dom4j.*;

/**
 *  Wrapper for the three main components of the {@link InputXML} Object (Data,
 *  Properties, and Relationships). For the specification of InputXML and it's
 *  components, see <a href="http://ndr.comm.nsdl.org/cgi-bin/wiki.pl?APIBasics">
 *  API Basics</a> in the NDR API Documentation. <p>
 *
 *  InputXMLComponents consist of "commands", which specify criteria that is
 *  used for selection of, or assignment to NDR Objects. For example, the
 *  commands in the "find" request are used to select objects from the NDR,
 *  while the commands in the "addMetadata" request assign values to the
 *  Metadata object to be created.<p>
 *
 *  The Component is represented as a dom4j.Element, with individual commands as
 *  subelements.
 *
 * @author     ostwald<p>
 *
 *      $Id $
 * @version    $Id: InputXMLComponent.java,v 1.3 2007/07/13 22:18:43 ostwald Exp
 *      $
 */

public class InputXMLComponent {
	private static boolean debug = true;
	/**  NOT YET DOCUMENTED */
	public String name = null;
	/**  NOT YET DOCUMENTED */
	public Element component = null;


	/**
	 *  Constructor for the InputXMLComponent object of specified type
	 *  ("relationship"
	 *
	 * @param  name  NOT YET DOCUMENTED
	 */
	public InputXMLComponent(String name) {
		this.name = name;
		this.component = DocumentHelper.createElement(name);
	}


	/**
	 *  Adds a feature to the Command attribute of the InputXMLComponent object
	 *
	 * @param  prop   The feature to be added to the Command attribute
	 * @param  value  The feature to be added to the Command attribute
	 */
	public void addCommand(String prop, String value) {
		addCommand(prop, value, null);
	}


	/**
	 *  Adds a feature to the Command attribute of the InputXMLComponent object.<p>
	 NOTE: Attempting to set a null value will cause the command to fail silently ...
	 *
	 * @param  prop    The feature to be added to the Command attribute
	 * @param  value   The feature to be added to the Command attribute
	 * @param  action  The feature to be added to the Command attribute
	 */
	public void addCommand(String prop, String value, String action) {
		Element command = DocumentHelper.createElement(prop);
		// attempting to set text to null is illegal - this command will fail silently ...
		if (value != null)
			command.setText(value);
		addCommand(command, action);
	}


	/**
	 *  Adds a feature to the Command attribute of the InputXMLComponent object
	 *
	 * @param  e       The feature to be added to the Command attribute
	 * @param  action  The feature to be added to the Command attribute
	 */
	public void addCommand(Element e, String action) {
		Element parent = this.getCommandParent(action);
		parent.add(e.createCopy());
	}


	/**
	 *  Gets the commandParent attribute of the InputXMLComponent object
	 *
	 * @param  cmd  NOT YET DOCUMENTED
	 * @return      The commandParent value
	 */
	protected Element getCommandParent(String cmd) {
		if (cmd == null)
			return this.component;
		else {
			Element cmdElement = this.component.element(cmd);
			if (cmdElement == null)
				cmdElement = this.component.addElement(cmd);
			return cmdElement;
		}
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @return    NOT YET DOCUMENTED
	 */
	public Element asElement() {
		return this.component.createCopy();
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  n  NOT YET DOCUMENTED
	 */
	protected static void pp(Node n) {
		prtln(Dom4jUtils.prettyPrint(n));
	}


	/**
	 *  Description of the Method
	 *
	 * @param  s  Description of the Parameter
	 */
	protected static void prtln(String s) {
		if (debug) {
			// System.out.println("InputXML: " + s);
			System.out.println(s);
		}
	}
}

