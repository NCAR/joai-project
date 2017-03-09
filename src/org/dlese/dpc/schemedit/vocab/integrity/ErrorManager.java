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
package org.dlese.dpc.schemedit.vocab.integrity;

import java.io.*;
import java.util.*;
import java.net.*;



public class ErrorManager  {
	static boolean debug = false;
	static boolean verbose = false;
	
	static int ANY = -1;
	
	List errorList;
	Map fileMap;
	Map errorTypes;
	
	public ErrorManager () {
		errorList = new ArrayList();
		fileMap = new TreeMap();
		initErrorTypes();
	}
	
	/**
	* Report on files containing errors.
	*/
	public String report () {
		String rpt = "\n";
		// rpt += Utils.box ("Field Files Containing Errors");
		if (verbose || errorList.size() > 0) {
			rpt += Utils.line () + "\n";
			rpt += "Field Files Containing Errors";
			rpt += "\n";
		}
		
		if (verbose && errorList.size() == 0) {
			rpt += "\n\t- " + "no errors to report - \n";
			return rpt;
		}
		
		for (Iterator i=fileMap.keySet().iterator();i.hasNext();) {
			String fileName = (String)i.next();
			List errors = (List) fileMap.get (fileName);
			rpt += "\n";
			// rpt += Utils.overline (fileName + " - " + errors.size() + " errors found");
			// rpt += "\n";
			rpt += Utils.box (fileName);
			
			
			for (int c = 0; c < 7;c++) {
				errors = getErrors (fileName, c);
				if (errors == null || errors.isEmpty())
					continue;

				ErrorType cType = (ErrorType) errorTypes.get(c);
				rpt += "\n";
				// rpt += cType.name + " (" + errors.size() + ") - " + cType.description;
				rpt += cType.name + " (" + cType.description + ")";
				rpt += "\n";
	
				
				if (!errors.isEmpty()) {
					for (Iterator e=errors.iterator(); e.hasNext();) {
						FieldFilesChecker.FileError error = (FieldFilesChecker.FileError)e.next();
						String errorRpt = error.report();
						if (errorRpt != null && errorRpt.length() > 0) {
							rpt += "    - " + errorRpt;
							rpt += "\n";
						}
					}
				}
			}
		}
		return rpt;
	}
	
	void initErrorTypes () {
		errorTypes = new TreeMap();

		errorTypes.put ( FieldFilesChecker.READER_ERROR,
			new ErrorType (
				FieldFilesChecker.READER_ERROR,
				"Reader Error", 
				"This file does not exist or could not be read as XML"));
		
		errorTypes.put ( FieldFilesChecker.ILLEGAL_PATH,
			new ErrorType (
				FieldFilesChecker.ILLEGAL_PATH,
				"Illegal Path", 
				"This file defines an xpath that is not schema-legal"));

		errorTypes.put ( FieldFilesChecker.MISSING_PATH,
			new ErrorType (
				FieldFilesChecker.MISSING_PATH,
				"Missing Path",
				"NOT defined in any field file"));
				
		errorTypes.put ( FieldFilesChecker.DUPLICATE_PATH,
			new ErrorType (
				FieldFilesChecker.DUPLICATE_PATH,
				"Duplicate Path",
				"Contained in mulitiple fields files"));
			
		errorTypes.put ( FieldFilesChecker.MISSING_VOCAB,
			new ErrorType (
				FieldFilesChecker.MISSING_VOCAB,
				"Missing Vocab",
				"Schema vocab terms NOT defined"));
			
		errorTypes.put ( FieldFilesChecker.DUPLICATE_VOCAB,
			new ErrorType (
				FieldFilesChecker.DUPLICATE_VOCAB,
				"Duplicate Vocab Terms",
				"defined more than once"));
	}
	
	private void addBucket (Map map, Object key) {
		map.put (key, new ArrayList());
	}
	
	private void addMapItem (Map map, Object key, Object item) {
		if (!map.containsKey(key))
			addBucket (map, key);
		List bucket = (List)map.get(key);
		bucket.add (item);
		map.put (key, bucket);
	}
	
	public void add (FieldFilesChecker.FileError error) {
		errorList.add (error);
		addMapItem (fileMap, error.fileName, error);
	}
		
	public boolean hasErrors (String fileName) {
		return fileMap.containsKey(fileName);
	}
	
	public List getErrors (String fileName, int error_code) {
		List all = (List)fileMap.get (fileName);
		List ret = new ArrayList();
		for (Iterator i=all.iterator();i.hasNext();) {
			FieldFilesChecker.FileError error = (FieldFilesChecker.FileError)i.next();
			if (error.error_code == error_code || error_code == ANY)
				ret.add (error);
		}
		return ret;
	}
			
	private static void prtln(String s) {
		if (debug)
			System.out.println(s);
	}
	
	public static void setVerbose (boolean v) {
		verbose = v;
	}
	public static void setDebug (boolean d) {
		debug = d;
	}
	
	class ErrorType {
		int error_code;
		String name;
		String description;
		
		public ErrorType (int error_code, String name, String description) {
			this.error_code = error_code;
			this.name = name;
			this.description = description;
		}
		
		public String toString () {
			String s = "";
			s += "error_code: " + error_code + "\n";
			s += "name: " + name + "\n";
			s += "description: " + description + "\n";
			return s;
		}
	}


}

