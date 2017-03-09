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
package org.dlese.dpc.propertiesmgr;

import java.util.*;
import java.io.*;

/**
 *  Reads and writes Java properties files. Properties files can be on disc or contained within a Jar file.
 *
 * @author     John Weatherley
 * @version    $Id: PropertiesManager.java,v 1.6 2009/03/20 23:33:54 jweather Exp $
 */
public class PropertiesManager extends Properties {

	private String emptyValMsg = "[none] ";
	private String propsFileName = "";
	private boolean isInJar = false;


	/**  Create an unloaded Properties object. Same as Properties(). */
	public PropertiesManager() {
		super();
	}


	//================================================================

	/**
	 *  Create and load this Properties hash map from the given Properties file or properties resource located in
	 *  the jar.
	 *
	 * @param  propsFileName    The name of the Properties file to load or URL to the properties location inside
	 *      a jar
	 * @exception  IOException  If unable to load the properties.
	 */
	public PropertiesManager(String propsFileName)
		 throws IOException {
		this.propsFileName = propsFileName;
		loadPropertiesFile();
	}


	/**
	 *  Loads or re-loads the properties from its file or JAR, replacing all previously loaded properties, if
	 *  any. If the file or JAR is not configured, does nothing.
	 *
	 * @exception  IOException  If error
	 */
	public void loadPropertiesFile()
		 throws IOException {
		if (propsFileName == null || propsFileName.length() == 0)
			return;

		File pfile = new File(propsFileName);
		if (pfile.canRead()) {
			BufferedInputStream bis = new BufferedInputStream(
				new FileInputStream(pfile));
			// Clear the HashTable first so we start with blank properties
			this.clear();
			this.load(bis);
			// Load the props hash map
			bis.close();
		}
		else {
			// Read the props from the jar file:

			InputStream input = this.getClass().getResourceAsStream(propsFileName);
			if (input == null)
				input = this.getClass().getResourceAsStream("/" + propsFileName);
			if (input != null) {
				// Clear the HashTable first so we start with blank properties
				this.clear();				
				this.load(input);
				// Load the props hash map
				input.close();
			}
			else {
				throw new IOException(
					"Could not load properties \""
					 + propsFileName + "\"");
			}
			isInJar = true;
		}
	}

	//================================================================

	/**
	 *  Writes the properties to the same file that was used to read them, preserving any comments in the file.
	 *  If the original file was contained in a jar then an I/O excetion is thrown.
	 *
	 * @exception  IOException  If error in input/output
	 */
	public void writePropsFile()
		 throws IOException {

		if (isInJar)
			throw new IOException("Properties cannot be written to a jar file");

		// Write propertis to the same file that was used to read them.
		writePropsFile(propsFileName);
	}



	/**
	 *  Writes the properties to the given file path. If the file path is the same as the one used to upen this
	 *  file, then all comments in the file are preserved.
	 *
	 * @param  propsFilePath    The full path to the props file to be written.
	 * @exception  IOException  If error in input/output
	 */
	public void writePropsFile(String propsFilePath)
		 throws IOException {

		LinkedList lines = new LinkedList();

		String s = "";

		// Read the lines in the file into a LinkedList
		try {
			BufferedReader in =
				new BufferedReader(
				new InputStreamReader(
				new FileInputStream(propsFilePath)));

			while ((s = in.readLine()) != null)
				lines.add(s);
			in.close();
		} catch (IOException e) {
			throw new IOException("Unable to write properties file \""
				 + propsFilePath + ".\" Reason: " + e);
		}

		Hashtable found = new Hashtable();

		String fullFile = "";

		// Construct a String containing the correct text for the output
		try {
			for (int i = 0; i < lines.size(); i++) {
				s = (String) lines.get(i);
				s = s.trim();

				// Output comments and blank lines
				if (s.startsWith("#") || s.length() == 0)
					fullFile += s + "\n";

				// Output the properties themselves
				else {
					StringTokenizer izer = new StringTokenizer(s, " \t\n\r\f=");

					String t = "";
					String param = null;

					// Grab the first token, which is the prop key
					if (izer.hasMoreTokens()) {
						t = izer.nextToken().trim();
						param = getProperty(t);
					}

					// Keep track of the params found so far
					found.put(t, "");

					if (param == null) {
						throw new IOException("Syntax error in properties file \""
							 + propsFileName
							 + "\" on the line containing the text: \""
							 + s + "\"");
					}

					fullFile += t + "     " + param + "\n";
				}
			}

			// Output any properties that have been set that were not in the original
			// properties file.
			Enumeration names = propertyNames();
			String name = "";
			while (names.hasMoreElements()) {
				name = (String) names.nextElement();
				if (!found.containsKey(name))
					fullFile += name + "     " + getProperty(name) + "\n";
			}

		} catch (IOException e) {
			throw new IOException("Unable to write properties file \""
				 + propsFileName + ".\" Reason: " + e);
		}

		// Only write the file if there has not been an error caught above
		try {
			BufferedWriter out =
				new BufferedWriter(
				new OutputStreamWriter(
				new FileOutputStream(propsFileName)));

			out.write(fullFile);

			out.close();
		} catch (IOException e) {
			throw new IOException("Error writing properties file \""
				 + propsFilePath + ".\" Reason: " + e);
		}
	}



	/**
	 *  Retrieves the property value from this object's property file. If the property does not exist and a
	 *  default value is specified, the default is set in the Properties and is returned.
	 *
	 * @param  property       The property sting to retrieve
	 * @param  defaultValue   The default value for this property, if none is found
	 * @return                The prop value
	 * @exception  Exception  If error
	 */
	public String getProp(String property, String defaultValue)
		 throws Exception {
		//String dStg = "";


		String propVal = getProperty(property);
		if (propVal == null || propVal.equals("")) {
			if (defaultValue != null) {
				setProperty(property, defaultValue);
				propVal = defaultValue;
				//dStg = defaultValMsg;
			}
		}
		if (propVal == null)
			throw new Exception("Properties file parameter \"" + property + "\" is not set.");

		String ret = propVal.trim();

		/*
		 *  try {
		 *  ret = expandAliases(propVal.trim());
		 *  } catch (OaiException e) {
		 *  throw new Exception("Internal Server Error", e.getMessage(), null);
		 *  }
		 */
		return ret;
	}



	/**
	 *  Same as the {@link #getProp(String,String)} method only it returns it's parameter as an integer.
	 *
	 * @param  property       The property sting to retrieve
	 * @param  defaultValue   The default value for this property, if none is found
	 * @return                The prop value
	 * @exception  Exception  If error
	 */
	public int getPropAsInt(String property, String defaultValue)
		 throws Exception {
		//String dStg = "";


		String propVal = getProperty(property);
		int retVal = 0;
		if (propVal == null || propVal.equals("")) {
			if (defaultValue != null) {
				setProperty(property, defaultValue);
				propVal = defaultValue;
				//dStg = defaultValMsg;
			}
		}
		if (propVal == null)
			throw new Exception("Properties file parameter \"" + property + "\" is not set.");

		try {
			retVal = Integer.parseInt(propVal.trim());
		} catch (NumberFormatException nfe) {
			throw new Exception("Properties file parameter \"" + property + "\" cannot be converted to type int.");
		}

		return retVal;
	}



	/**
	 *  Same as the {@link #getProp(String,String)} method only it returns it's parameter as a boolean. Possible
	 *  inputs are [yes|no|true|false|on|off|enabled|disabled].
	 *
	 * @param  property       The property sting to retrieve
	 * @param  defaultValue   The default value for this property, if none is found
	 * @return                The prop value
	 * @exception  Exception  If error
	 */
	public boolean getPropAsBoolean(String property, String defaultValue)
		 throws Exception {
		//String dStg = "";


		String propVal = getProperty(property);
		boolean retVal = false;
		if (propVal == null || propVal.equals("")) {
			if (defaultValue != null) {
				setProperty(property, defaultValue);
				propVal = defaultValue;
				//dStg = defaultValMsg;
			}
		}
		if (propVal == null)
			throw new Exception("Properties file parameter \"" + property + "\" is not set.");

		propVal = propVal.trim().toLowerCase();

		if (propVal.equals("yes") ||
			propVal.equals("true") ||
			propVal.equals("on") ||
			propVal.equals("enabled"))
			retVal = true;
		else if (propVal.equals("no") ||
			propVal.equals("false") ||
			propVal.equals("disabled") ||
			propVal.equals("off"))
			retVal = false;
		else
			throw new Exception("Properties file parameter \"" + property + "\" does not match [yes|no|true|false|on|off|enabled|disabled]");

		return retVal;
	}



	//================================================================

	private void prtln(String s) {
		System.out.println(s);
	}
}

