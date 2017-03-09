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
package org.dlese.dpc.index;

import java.io.*;
import java.util.*;
import java.text.*;
import java.net.*;
import java.lang.reflect.*;
//import org.dlese.dpc.catalog.*;
//import org.dlese.dpc.catalog.DleseBean;
//import org.dlese.dpc.catalog.DleseCatalog;
//import org.dlese.dpc.catalog.DleseCatalogRecord;

//import com.lucene.index.*;
//import com.lucene.document.*;
//import com.lucene.analysis.*;
//import com.lucene.queryParser.*;
//import com.lucene.search.*;

import org.dlese.dpc.index.SimpleLuceneIndex;

public class TestSimpleIndex {

	public TestSimpleIndex() {
	}

	public static void main(String[] args) {
		//TestSimpleIndex index = new TestSimpleIndex();
		try {
			SimpleLuceneIndex index = new SimpleLuceneIndex("k:/deniman/testIndex");
			
			//index.create("k:/deniman/testIndex", index.STANDARD_ANALYSIS, "content");
			
			//index.use("k:/deniman/testIndex");

			//index.useReader(new File("k:/deniman/testIndex"), "reader");
			
		}
		catch (Exception e) {
			System.err.println("Exception: " + e.getClass() + " with message: " + e.getMessage()); 
		}
	}

}
