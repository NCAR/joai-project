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

import  org.dlese.dpc.schemedit.*;
import org.dlese.dpc.xml.*;
import org.dlese.dpc.util.Files;

import org.dom4j.*;

import java.util.*;
import java.util.regex.*;
import java.net.*;

public class GetMirrorsTester {
	
	private static boolean debug = true;
	
	public static void main (String[] args) {
		String path = "/devel/ostwald/tmp/adn-record.xml";
		List mirrors = new ArrayList();
		try {
			StringBuffer xmlBuff = Files.readFile (path);
			String xml = Dom4jUtils.localizeXml (xmlBuff.toString(), "itemRecord");
			mirrors = SchemEditUtils.getMirrorUrls (xml);
			if (mirrors != null && mirrors.size() > 0) {
				prtln (mirrors.size() + " mirrors found");
				for (Iterator i=mirrors.iterator();i.hasNext();) {
					prtln ("\t" + (String)i.next());
				}
			}
			else
				prtln ("NO mirrors found");
		} catch (Exception e) {
			prtln ("error: " + e.getMessage());
		}
	}
	
	private static void prtln(String s) {
		if (debug) {
			System.out.println(s);
		}
	}
	
}
