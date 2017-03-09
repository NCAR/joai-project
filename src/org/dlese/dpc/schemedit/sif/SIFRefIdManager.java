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
import org.dlese.dpc.xml.XMLFileFilter;

import java.util.*;
import java.io.File;
import org.dom4j.*;

/**
 *  Manages configurations of SIF fields that take references to other SIF
 *  objects.
 *
 * @author    Jonathan Ostwald
 */
public class SIFRefIdManager {
	private static boolean debug = true;

	private File sourceDir = null;
	private Map map = null;

	private static SIFRefIdManager instance = null;
	private static String path = null;


	/**
	 *  Gets singleton SIFRefIdManager instance.
	 *
	 * @return    The instance value
	 */
	public static SIFRefIdManager getInstance() {
		if (instance == null) {
			try {
				instance = new SIFRefIdManager();
			} catch (Exception e) {
				if (path == null) {
					// not configured, fail silently
				}
				else {
					prtln("getInstance error: " + e.getMessage());
				}
			}
		}
		return instance;
	}


	/**
	 *  Constructor for the SIFRefIdManager object
	 *
	 * @param  path           directory containing configuration files.
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	private SIFRefIdManager() throws Exception, Exception {
		if (path == null)
			throw new Exception("path does not have a value");
		this.sourceDir = new File(path);
		if (!sourceDir.exists() || !sourceDir.isDirectory())
			throw new Exception("directory does not exist or is not a directory at " + path);
		this.init();
	}


	/**
	 *  Sets the path attribute of the SIFRefIdManager class
	 *
	 * @param  sourceDir  The new path value
	 */
	public static void setPath(String sourceDir) {
		path = sourceDir;
	}

	public static void reinit() {
		instance = null;
	}

	/**
	 *  Read configuration files for SIF frameworks and populate a map which stores
	 *  the configurations keyed by xmlFormat.
	 */
	private void init() {
		prtln ("init");
		this.map = new HashMap();
		File[] files = sourceDir.listFiles(new XMLFileFilter());
		for (int i = 0; i < files.length; i++) {
			try {
				SIFRefIdMap refIdMap = new SIFRefIdMap(files[i]);
				refIdMap.report();
				this.map.put(refIdMap.getXmlFormat(), refIdMap);
			} catch (Exception e) {
				prtln("could not instantiage RefIdMap for " + files[i] + ": " + e.getMessage());
			}
		}
	}


	/**
	 *  Gets the refIdMap for the specified SIF Framework.
	 *
	 * @param  xmlFormat  NOT YET DOCUMENTED
	 * @return            The refIdMap value
	 */
	public SIFRefIdMap getRefIdMap(String xmlFormat) {
		return (SIFRefIdMap) map.get(xmlFormat);
	}


	/**
	 *  Returns list of supported SIF xmlFormats.
	 *
	 * @return    The xmlFormats value
	 */
	public List getXmlFormats() {
		List formats = new ArrayList();
		for (Iterator i = this.map.keySet().iterator(); i.hasNext(); )
			formats.add((String) i.next());
		return formats;
	}


	/**
	 *  Returns true of specified xmlFormat is configured.
	 *
	 * @param  xmlFormat  NOT YET DOCUMENTED
	 * @return            NOT YET DOCUMENTED
	 */
	public boolean hasXmlFormat(String xmlFormat) {
		return this.map.containsKey(xmlFormat);
	}


	/**
	 *  The main program for the SIFRefIdManager class
	 *
	 * @param  args           The command line arguments
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public static void main(String[] args) throws Exception {
		String path = "C:/Documents and Settings/ostwald/devel/SIF/dcs_config/sifTypePaths";
		SIFRefIdManager.setPath(path);
		SIFRefIdManager refIdMgr = SIFRefIdManager.getInstance();
		prtln("refIdMgr instantiated");

		for (Iterator i = refIdMgr.map.values().iterator(); i.hasNext(); ) {
			((SIFRefIdMap) i.next()).report();
		}
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  s  NOT YET DOCUMENTED
	 */
	protected static void prtln(String s) {
		if (debug) {
			SchemEditUtils.prtln(s, "SIFRefIdManager");
		}
	}
}

