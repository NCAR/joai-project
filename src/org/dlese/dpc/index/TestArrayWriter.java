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

public class TestArrayWriter {

	public TestArrayWriter() {
	}

	static String [] getArray(File arrayFile) {
		String [] array = null;
		try {
			FileInputStream f = new FileInputStream(arrayFile);
			ObjectInputStream p = new ObjectInputStream(f);
			try {
				array = (String [])p.readObject();
				for (int i=0; i<array.length; i++) {
					System.out.println("read string = " + array[i]);
				}
			}
			catch (Exception e) {
				array = null;
			}
			finally {
				f.close();
				p.close();
			}
		}
		catch (Exception e) {
			// Will always happen if file doesn't exist.
			// monitor.logError("Exception reading record file: " + file.getName());
		}
		return array;
	}

	static void setArray(File arrayFile, String [] array) {
	
		try {
			FileOutputStream f = new FileOutputStream(arrayFile);
			ObjectOutputStream p = new ObjectOutputStream(f);

			try {
				p.writeObject(array);
				p.flush();
			}
			catch (Exception e) {
				//
			}
			finally {
				f.close();
				p.close();
			}
		}
		catch (Exception e) {}
	}


	public static void main(String[] args) {
		Tester tst = new Tester();
		try {
			File arrayFile = new File("k:/deniman/testfile.obj");
			String [] array = { "one", "two", "three" };
			
			setArray(arrayFile, array);
			
			array = getArray(arrayFile);
			
		}
		catch (Exception e) {
			System.err.println("Exception: " + e.getClass() + " with message: " + e.getMessage()); 
		}
	}

}
