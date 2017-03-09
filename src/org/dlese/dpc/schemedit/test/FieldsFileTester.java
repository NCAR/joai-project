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

import java.io.*;
import java.util.*;
import java.text.*;
import java.net.URI;
import org.dom4j.Node;
import org.dlese.dpc.schemedit.vocab.*;
import org.dlese.dpc.schemedit.*;

import org.dlese.dpc.xml.Dom4jUtils;
import org.dlese.dpc.xml.Dom4jUtils;




/**
 *  Tester for {@link org.dlese.dpc.schemedit.vocab.FieldInfoMap} 
 *
 *@author    ostwald
 */
public class FieldsFileTester {
	MetaDataFramework framework = null;
	FieldInfoMap fieldInfoMap = null;

	/**
	 *  Constructor for the FieldsFileTester object
	 */
	public FieldsFileTester(String uri) throws Exception {
		fieldInfoMap = new NewFieldInfoMap (uri);
		fieldInfoMap.init();
			
	}
	
	FieldInfoReader getFieldInfoReader (String xpath) throws Exception {
		FieldInfoReader reader = fieldInfoMap.getFieldInfo(xpath);
		if (reader != null)
			prtln (reader.toString());
		else 
			prtln ("FieldInfoReader not found for " + xpath);
		return reader;
	}
	
	void showFieldInfoReaders () {
		prtln ("Field Info Readers");
		for (Iterator i=fieldInfoMap.getFields().iterator();i.hasNext();) {
			prtln ("\t" + (String)i.next());
		}
	}
	
	public static void main (String [] args) throws Exception {
		TesterUtils.setSystemProps();
		URI uri = new URI ("http://www.dls.ucar.edu/people/ostwald/Metadata/mast-field-files/mast/1.0.00/build/fields-list.xml");
		FieldsFileTester tester = new FieldsFileTester (uri.toString());
		String xpath = "/record/general/title";
		tester.getFieldInfoReader (xpath);
		
		tester.showFieldInfoReaders();
		
	}
	
		
	static void pp (Node node) {
		prtln (Dom4jUtils.prettyPrint(node));
	}
	
	/**
	 *  Description of the Method
	 *
	 *@param  s  Description of the Parameter
	 */
	public static void prtln(String s) {
		SchemEditUtils.prtln (s, "");
	}
}

