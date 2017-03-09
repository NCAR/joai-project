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
package org.dlese.dpc.schemedit.input;

import org.dlese.dpc.schemedit.url.UrlHelper;
import org.dlese.dpc.schemedit.action.form.SchemEditForm;
import org.dlese.dpc.schemedit.MetaDataFramework;
import org.dlese.dpc.schemedit.SchemEditUtils;

import org.dlese.dpc.xml.schema.*;
import org.dlese.dpc.xml.schema.compositor.*;
import org.dlese.dpc.xml.*;

import java.util.*;
import java.io.*;
import java.text.ParseException;
import java.net.MalformedURLException;

import org.apache.struts.action.ActionError;
import org.apache.struts.action.ActionMapping;
import javax.servlet.http.HttpServletRequest;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Attribute;
import org.dom4j.Node;

/**
 *  Preprocesses instance documents to ensure required paths are present so even
 *  if the metadata framework changes, the older documents can still be edited.
 *
 *@author    ostwald <p>
 *
 *
 */
public class EnsureMinimalDocument {

	private static boolean debug = false;

	private SchemaHelper schemaHelper;

	private DocMap docMap = null;
	private Document doc = null;
	private Document minimalDoc = null;


	/**
	 *  Constructor for the EnsureMinimalDocument object
	 *
	 *@param  doc           instanceDocument
	 *@param  schemaHelper  the schemaHelper for doc's framework
	 */
	private EnsureMinimalDocument(Document doc, SchemaHelper schemaHelper) {
		this.schemaHelper = schemaHelper;
		this.minimalDoc = this.schemaHelper.getMinimalDocument();
		prtln("----------------");
		prtln("Minimal Doc **\n" + Dom4jUtils.prettyPrint(this.minimalDoc));
		prtln("----------------");
		this.doc = doc;
		this.docMap = new DocMap(doc, schemaHelper);
	}


	/**Static method to process given Document with help of given schemaHelper
	 *
	 *@param  doc           instanceDocument
	 *@param  schemaHelper  the schemaHelper for doc's framework
	 */
	public static void process(Document doc, SchemaHelper schemaHelper) {
		EnsureMinimalDocument ensurer = new EnsureMinimalDocument(doc, schemaHelper);
		ensurer.process();
	}


	/**
	 *  Ensure the document has all required paths.
	 */
	private void process() {
		Element rootElement = docMap.getDocument().getRootElement();
		processTree(rootElement);
	}


	/**
	 *  Recursively processes each subelementn of the provided instanceDoc element, adding required fields
	 when necessary.
	 *
	 *@param  instElement  Description of the Parameter
	 */
	private void processTree(Element instElement) {
		// prtln ("\nprocessTree(" + instElement.getPath() + ")");
		String schemaPath = XPathUtils.normalizeXPath(instElement.getPath());
		Element minElement = (Element) this.minimalDoc.selectSingleNode(schemaPath);

		// make sure the instance doc has at least one of all paths in the minDoc
		for (Iterator i = minElement.elementIterator(); i.hasNext(); ) {
			Element child = (Element) i.next();
			SchemaNode childNode = schemaHelper.getSchemaNode(child.getPath());
			String childName = child.getQualifiedName();
			Element instChild = instElement.element(childName);
			if (instChild == null && childNode.isRequired()) {
				try {
					instChild = (Element) this.docMap.createNewNode(instElement.getPath() + '/' + childName);
				} catch (Exception e) {
					prtln(e.getMessage());
					continue;
				}
				// prtln("created new element at " + child.getPath());
			}
			processTree(instChild);
		}
	}



	/**
	 *  Print a line to standard out.
	 *
	 *@param  s  The String to print.
	 */
	private static void prtln(String s) {
		if (debug) {
			SchemEditUtils.prtln(s, "EnsureMinimalDocument");
		}
	}

}

