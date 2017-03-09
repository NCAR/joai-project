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
package org.dlese.dpc.schemedit.sif;

import org.dlese.dpc.schemedit.SchemEditUtils;
import org.dlese.dpc.xml.Dom4jUtils;

import java.util.*;
import java.io.File;
import org.dom4j.*;

/**
 *  Class holding information about the paths for a SIF framework that can
 *  accept References to other SIF objects, including the acceptable object
 *  types.
 *
 * @author    Jonathan Ostwald
 */
public class SIFRefIdMap {
	private static boolean debug = true;

	private Document doc = null;
	private File source = null;
	private String xmlFormat = null;
	private Map map = null;


	/**
	 *  Constructor for the SIFRefIdMap object
	 *
	 * @param  source         NOT YET DOCUMENTED
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public SIFRefIdMap(File source) throws Exception {
		if (!source.exists())
			throw new Exception("source does not exist at " + source);
		this.source = source;
		this.doc = Dom4jUtils.getXmlDocument(source);
		this.xmlFormat = this.doc.getRootElement().attributeValue("xmlFormat");
		this.init();
	}


	/**  Reads config file for SIF format and initializes map of xpaths.  */
	private void init() {
		this.map = new HashMap();
		List paths = doc.selectNodes("/sifTypePaths/sifTypePath");
		// prtln(paths.size() + " paths found");
		for (Iterator i = paths.iterator(); i.hasNext(); ) {
			Element pathElement = (Element) i.next();
			String path = pathElement.element("path").getText();
			// prtln ("path: " + path);
			List types = new ArrayList();
			for (Iterator t = pathElement.elementIterator("type"); t.hasNext(); ) {
				Element e = (Element) t.next();
				String type = e.getText();
				types.add(type);
			}
			map.put(path, types);
		}
	}


	/**
	 *  Gets a list of SIF Types accepted by the field at specified path.
	 *
	 * @param  path  NOT YET DOCUMENTED
	 * @return       The typeList value
	 */
	public List getTypeList(String path) {
		return (List) map.get(path);
	}


	/**
	 *  Gets comma-separated string containing SIF types accepted by element at
	 *  given path.
	 *
	 * @param  path  NOT YET DOCUMENTED
	 * @return       The types value
	 */
	public String getTypes(String path) {
		String types = "";
		List typeList = this.getTypeList(path);
		if (typeList != null) {
			for (Iterator i = typeList.iterator(); i.hasNext(); ) {
				types += (String) i.next();
				if (i.hasNext())
					types += ',';
			}
		}
		return types;
	}


	/**
	 *  Gets List of paths that accept SIF Object Refs.
	 *
	 * @return    The paths value
	 */
	public List getPaths() {
		List paths = new ArrayList();
		for (Iterator i = this.map.keySet().iterator(); i.hasNext(); )
			paths.add((String) i.next());
		return paths;
	}


	/**
	 *  Returns true if the specified path accepts SIF Object References.
	 *
	 * @param  path  NOT YET DOCUMENTED
	 * @return       NOT YET DOCUMENTED
	 */
	public boolean hasPath(String path) {
		return this.map.containsKey(path);
	}


	/**
	 *  Gets the xmlFormat attribute of the SIFRefIdMap object
	 *
	 * @return    The xmlFormat value
	 */
	public String getXmlFormat() {
		return this.xmlFormat;
	}


	/**  NOT YET DOCUMENTED */
	public void report() {
		prtln("\nSIFRefIdMap ** " + this.xmlFormat + " **");
		for (Iterator i = this.map.keySet().iterator(); i.hasNext(); ) {
			String mypath = (String) i.next();
			List types = this.getTypeList(mypath);
			prtln("\n" + mypath);
			for (int ii = 0; ii < types.size(); ii++)
				prtln("\t" + (String) types.get(ii));
			// prtln ("\t (" + this.getTypes (mypath) + ")");
		}
	}


	/**
	 *  The main program for the SIFRefIdMap class
	 *
	 * @param  args           The command line arguments
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public static void main(String[] args) throws Exception {
		String path = "C:/Documents and Settings/ostwald/devel/SIF/dcs_config/sifTypePaths/sif_activity.xml";
		SIFRefIdMap refIdMap = new SIFRefIdMap(new File(path));
		prtln("refIdMap instantiated");
		refIdMap.report();
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  s  NOT YET DOCUMENTED
	 */
	protected static void prtln(String s) {
		if (debug) {
			SchemEditUtils.prtln(s, "SIFRefIdMap");
		}
	}
}

