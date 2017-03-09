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
package org.dlese.dpc.services.dcs;

import org.dlese.dpc.xml.*;
import org.dlese.dpc.index.*;
import org.dlese.dpc.util.*;


import org.dlese.dpc.schemedit.*;
import org.dlese.dpc.schemedit.dcs.*;

import java.util.*;
import java.text.*;
import java.io.*;

import org.dom4j.*;

public class PutRecordData  {
	Document doc = null;
	String id;
	String format;
	String collection;
	MetaDataFramework framework = null;; 
	
	public PutRecordData () {
	}
	
	public String getId () {
		return id;
	}
	
	public String getCollection () {
		return collection;
	}
	
	public String getFormat () {
		return format;
	}
	
	public Document getDocument () {
		return doc;
	}
	
	public void init (String recordXml, String format, String collection, FrameworkRegistry frameworks) throws Exception {
		this.format = format;
		this.collection = collection;

		framework = frameworks.getFramework(format);
		if (framework == null)
			throw new Exception ("framework not found for \"" + format + "\" format");
			
		try {
			doc = Dom4jUtils.localizeXml(Dom4jUtils.getXmlDocument(recordXml), framework.getRootElementName());
		} catch (Exception e) {
			throw new Exception ("recordXml could not be read as XML");
		}
		
		// validate the id. ignore the parameter and use the id in the record
		String idPath = framework.getIdPath();
		try {
			id = doc.selectSingleNode(idPath).getText();
		} catch (Throwable t) {
			throw new Exception ("id could not be found in xmlRecord");
		}
	}
}

