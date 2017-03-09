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
package org.dlese.dpc.schemedit.ndr.util;

import org.dom4j.*;
import java.util.*;



/**
 *  Class representing an Contact attribute of the serviceDescription
 *
 * @author     Jonathan Ostwald
 * @version    $Id: Contact.java,v 1.3 2009/03/20 23:33:56 jweather Exp $
 */
public class Contact {
	public String name = null;
	public String email = null;
	public String info = null;
	

	/**
	 *  Constructor for the Contact object
	 *
	 * @param  name   NOT YET DOCUMENTED
	 * @param  email  NOT YET DOCUMENTED
	 * @param  info   NOT YET DOCUMENTED
	 */
	public Contact(String name, String email, String info) {
		this.name = name;
		this.email = email;
		this.info = info;
	}
	
	public Contact (Element e) {
		this.name = e.attributeValue("name", null);
		this.email = e.attributeValue("email", null);
	}

	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @return    NOT YET DOCUMENTED
	 */
	public Element asElement() {
		Element contact = DocumentHelper.createElement("contact");
		if (name != null)
			contact.addElement("name").setText(name);
		if (email != null)
			contact.addElement("email").setText(email);
		if (info != null)
			contact.addElement("info").setText(info);
		return contact;
	}
	
	private static String getChildText (Element parent, String tag) {
		try {
			return parent.element(tag).getTextTrim();
		} catch (Exception e) {}
		return "";
	}
	
	public static Contact getInstance (Element element) {
		String name = getChildText (element, "name");
		String email = getChildText (element, "email");
		String info = getChildText (element, "info");
		return new Contact (name, email, info);
	}
	
}
