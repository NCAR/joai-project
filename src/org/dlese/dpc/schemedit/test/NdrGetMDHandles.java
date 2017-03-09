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
package org.dlese.dpc.schemedit.test;

import org.dlese.dpc.schemedit.SchemEditUtils;
import org.dlese.dpc.xml.Dom4jUtils;
import org.dlese.dpc.util.Files;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.XPath;
import java.io.File;
import java.net.URL;
import java.util.*;


/**
 *  Class for testing dom manipulation with help from {@link org.dlese.dpc.xml.schema.SchemaHelper}
 *
 *@author     ostwald<p>
 $Id $
 */
public class NdrGetMDHandles {

	private static boolean debug = true;
	
	NdrGetMDHandles (String collection) throws Exception {
	}
		
	public static void main (String [] args) throws Exception {
		String MDPHandle = "2200/test.20070219032201392T";
		String apiUrl = "http://ndrtest.nsdl.org/api";
		String command = "listMembers";
		URL url = new URL (apiUrl + "/" + command + "/" + MDPHandle);
		Document doc = Dom4jUtils.getXmlDocument(url);
		doc = Dom4jUtils.localizeXml(doc);
		pp (doc);
		List mdHandles = new ArrayList();
		List mdHandleNodes = doc.selectNodes("//handleList/handle");
		if (mdHandleNodes != null) {
			for (Iterator i= mdHandleNodes.iterator();i.hasNext();) {
				Node node = (Node)i.next();
				mdHandles.add (node.getText());
			}
		}
		prtln ("Handles (" + mdHandles.size() + ")");
		for (Iterator i = mdHandles.iterator();i.hasNext();) {
			prtln ("\t" + (String)i.next());
		}
		
	}

	
	private static void  pp (Node node) {
		prtln (Dom4jUtils.prettyPrint(node));
	}
	
	/**
	 *  Description of the Method
	 *
	 *@param  s  Description of the Parameter
	 */
	private static void prtln(String s) {
		if (debug) {
			// SchemEditUtils.prtln(s, "NdrGetMDHandles");
			SchemEditUtils.prtln(s, "");
		}
	}
	
}

