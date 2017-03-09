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

import org.dlese.dpc.index.reader.DleseCollectionDocReader;

import org.dlese.dpc.xml.Dom4jUtils;
import org.dlese.dpc.xml.XPathUtils;
import org.dom4j.*;
import java.util.*;

/**
 *  Class to construct serviceDescription elements to be used in NDR Objects (i.e., MetadataProvider and Aggregator),
 as described at http://ndr.comm.nsdl.org/cgi-bin/wiki.pl?addMetadataProvider.
 *
 * @author     Jonathan Ostwald
 * @version    $Id: DcStream.java,v 1.3 2009/03/20 23:33:56 jweather Exp $
 */
public class DcStream {

	private static boolean debug = true;

	private Document doc = null;
	private String title = null;
	private String description = null;
	private String type = null;
	private String identifier = null;
	private Map props = null;


	/**  Constructor for the DcStream object */
	public DcStream() {
		this.props = new HashMap ();
	}

	private void addProp (String prop, String value) {
		List values = (List)props.get (prop);
		if (values == null)
			values = new ArrayList ();
		values.add (value);
		props.put(prop, values);
	}

	/**
	 *  Constructor for the DcStream object, given title, description and type values.
	 *
	 * @param  title        NOT YET DOCUMENTED
	 * @param  description  NOT YET DOCUMENTED
	 * @param  type         NOT YET DOCUMENTED
	 */
	public DcStream(String title, String description, String subject) {
		this();
		addProp("title", title);
		addProp("description", description);
		addProp("subject", subject);
	}


	/**
	 *  Sets the title attribute of the DcStream object
	 *
	 * @param  title  The new title value
	 */
	public void setTitle(String title) {
		this.addProp ("title", title);
	}


	/**
	 *  Sets the description attribute of the DcStream object
	 *
	 * @param  description  The new description value
	 */
	public void setDescription(String description) {
		addProp ( "description", description);
	}


	/**
	 *  Sets the type attribute of the DcStream object
	 *
	 * @param  type  The new type value
	 */
	public void setType(String type) {
		addProp ("type", type);
	}


	/**
	 *  Sets the identifier attribute of the DcStream object
	 *
	 * @param  type  The new identifier value
	 */
	public void setIdentifier(String type) {
		addProp ("identifier", identifier);
	}


	/**
	 *  Utility to test if a string is not null and has non-whitespace conten.
	 *
	 * @param  s  string to be tested
	 * @return    true if string has content
	 */
	private boolean notEmpty(String s) {
		return (s != null && s.trim().length() > 0);
	}


	/**
	 *  Returns serviceDescription as an dom4j.Element.
	 *
	 * @return    NOT YET DOCUMENTED
	 */
	public Element asElement() {
		Element root = getRootElement();
		
		for (Iterator pi=props.keySet().iterator();pi.hasNext();) {
			String prop = (String)pi.next();
			List values = (List)props.get (prop);
			for (Iterator vi=values.iterator();vi.hasNext();) {
				String value = (String)vi.next();
				if (notEmpty(value)) {
					Element e = root.addElement ("dc:" + prop);
					e.setText (value);
				}
			}
		}
		
		return root;
	}


	/**
	 *  Creates the root element for the serviceDescription in required namespaces.
	 *
	 * @return    The rootElement value
	 */
	private Element getRootElement() {
		Namespace oai_dc = DocumentHelper.createNamespace("oai_dc", "http://www.openarchives.org/OAI/2.0/oai_dc/");
		Namespace xsi = DocumentHelper.createNamespace("xsi", "http://www.w3.org/2001/XMLSchema-instance");
		QName qname = DocumentHelper.createQName("dc", oai_dc);
		Element root = DocumentHelper.createElement(qname);
		root.add(xsi);
		root.add(DocumentHelper.createNamespace("dc", "http://purl.org/dc/elements/1.1/"));

		qname = DocumentHelper.createQName ("schemaLocation", xsi);
		root.addAttribute (qname, 
			"http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd");
		
		return root;
	}


	/**
	 *  The main program for the DcStream class
	 *
	 * @param  args           The command line arguments
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public static void main(String[] args) throws Exception {
		String title = "Agent for yada";
		String description = "its all about yada";
		String subject = "none that i can see";
		DcStream dc_stream = new DcStream(title, description, subject);
		pp(dc_stream.asElement());

	}

	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  n  NOT YET DOCUMENTED
	 */
	private static void pp(Node n) {
		prtln(Dom4jUtils.prettyPrint(n));
	}

	/**
	 *  Description of the Method
	 *
	 * @param  s  Description of the Parameter
	 */
	private static void prtln(String s) {
		if (debug) {
			System.out.println(s);
		}
	}

}

