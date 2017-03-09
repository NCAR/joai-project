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
package org.dlese.dpc.schemedit.ndr.util.integration;

import org.dlese.dpc.ndr.apiproxy.*;
import org.dlese.dpc.ndr.NdrUtils;
import org.dlese.dpc.ndr.reader.*;
import org.dlese.dpc.ndr.request.*;
import org.dlese.dpc.xml.Dom4jUtils;
import org.dlese.dpc.util.Files;
import org.dlese.dpc.util.strings.FindAndReplace;
import org.dlese.dpc.index.SimpleLuceneIndex;
import org.dom4j.*;
import java.util.*;
import java.io.File;
import java.net.*;

/**
 *  Wraps a row from CollectionXSLReader, reprsenting information about a particular collection
 with the purpose of determining overlaps and gaps
 between the collection management info in NDR and NCS models.
 *
 * @author    Jonathan Ostwald
 */
public class CollectionXSLRecord {
	private static boolean debug = true;
	public Element element = null;
	private Map map = null;

	/**
	 *  Constructor for the CollectionXSLRecord object
	 *
	 * @param  xslPath  NOT YET DOCUMENTED
	 */
	public CollectionXSLRecord(Element rec) {
		this.element = rec;
		this.map = new HashMap ();
		for (Iterator i=rec.elementIterator();i.hasNext();) {
			Element e = (Element)i.next();
			map.put (e.getName(), e.getTextTrim());
		}
	}

	
	public String get (String field) {
		String val = (String)this.map.get (field);
		return (val != null ? val : "");
	}
	
	public String toString () {
		String s = "";
		for (Iterator i=map.keySet().iterator();i.hasNext();) {
			String name = (String)i.next();
			String val = (String)map.get(name);
			s += name + ": " + val;
			if (i.hasNext())
				s += "\n";			
		}
		return s;
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  args  NOT YET DOCUMENTED
	 */
	public static void main(String[] args) {
		String xml = "H:/Documents/NDR/NSDLCollections/NDRCollectionsNCSIDs.xml";
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  node  NOT YET DOCUMENTED
	 */
	private static void pp(Node node) {
		prtln(Dom4jUtils.prettyPrint(node));
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  s  NOT YET DOCUMENTED
	 */
	private static void prtln(String s) {
		String prefix = null;
		if (debug) {
			NdrUtils.prtln(s, prefix);
		}
	}
	

	
}

