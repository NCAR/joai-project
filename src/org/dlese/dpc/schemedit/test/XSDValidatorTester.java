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

import org.dlese.dpc.xml.*;
import org.dlese.dpc.xml.schema.*;
import org.dlese.dpc.util.*;

import java.io.*;
import java.lang.*;

public class XSDValidatorTester {

	static String [] testers = { "0", "999", "9999", "99999", "0001", "-0001", "-1"};
	
	public static void main (String[] args) throws Exception {
		// build schemaHelper for given framework
		File schemaFile = new File ("/devel/ostwald/metadata-frameworks/adn-item-project/record.xsd");
		SchemaHelper sh = new SchemaHelper (schemaFile);
		
		String type = "BCType";
		
		for (int i=0;i<testers.length;i++) {
			String value = testers[i];
			prtln ("\n\n ** year: " + value + " ***");
			try {
				if (sh.checkValidValue(type, value))
					prtln (value + " is a valid " + type);
				else
					prtln (value + " is NOT a valid " + type);
			} catch (Exception e) {
				prtln ("ERROR: " + e.getMessage());
			}
		}
	}
		
	private static void prtln (String s) {
		System.out.println (s);
	}
}
