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
package org.dlese.dpc.schemedit.config;

import java.io.*;
import java.util.*;
import java.text.*;

import org.dlese.dpc.xml.*;
import org.dlese.dpc.util.*;
import org.dlese.dpc.schemedit.*;

import org.dom4j.Document;
import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import org.dom4j.Attribute;
import org.dom4j.Node;

/**
 *  Map holding {@link org.dlese.dpc.schemedit.config.SchemaPath} instances that are defined in the framework
 *  configuration files, instantiated by FrameworkConfigReader instances, and accessed by MetaDataFramework.
 *
 * @author     ostwald <p>
 *
 */
public class SchemaPathMap {
	private HashMap pathMap = null;
	private HashMap nameMap = null;



	/**  Constructor for the SchemaPathMap object */
	public SchemaPathMap() {
		pathMap = new HashMap();
		nameMap = new HashMap();
	}

	private List copyRecordPaths = null;
	
	/**
	 *  CopyRecordPaths represent schema fields that will be copied to new records during record copy operations.
	 *
	 * @return    SchemaPaths having value of "true" for the "requiredByCopyRecord" attribute
	 */
	public List getCopyRecordPaths() {
		if (this.copyRecordPaths == null) {
			ArrayList paths = new ArrayList();
			for (Iterator i = nameMap.values().iterator(); i.hasNext(); ) {
				SchemaPath schemaPath = (SchemaPath) i.next();
				if (schemaPath.requiredByCopyRecord)
					paths.add(schemaPath);
			}
			Collections.sort (paths, new SchemaPathComparator());
			this.copyRecordPaths = paths;
			}
		return this.copyRecordPaths;
	}


	/**
	 *  CollectionConfigPaths represent schema fields that hold default information that will be inserted in new
	 *  records. <p>
	 *
	 *  For example, the ADN framework config contains collectionConfig paths for "termsOfUse", among others.
	 *
	 * @return    SchemaPaths having value "collectionConfig" for the valueType attribute
	 */
	public List getCollectionConfigPaths() {
		return getSchemaPathsByValueType("collectionConfig");
	}

	private List minimalRecordPaths = null;
	
	/**
	 *  MinimalRecordPaths represent fields that are required for new records.
	 *
	 * @return    SchemaPaths having value of "true" for the "requiredByMinimalRecord" attribute
	 */
	public List getMinimalRecordPaths() {
		if (this.minimalRecordPaths == null) {
			ArrayList paths = new ArrayList();
			for (Iterator i = nameMap.values().iterator(); i.hasNext(); ) {
				SchemaPath schemaPath = (SchemaPath) i.next();
				if (schemaPath.requiredByMinimalRecord)
					paths.add(schemaPath);
			}
			Collections.sort (paths, new SchemaPathComparator());
			this.minimalRecordPaths =  paths;
		}
		return this.minimalRecordPaths;
	}

	private List inputHelperPaths = null;;
	
	public List getInputHelperPaths() {
		if (this.inputHelperPaths == null) {
			ArrayList paths = new ArrayList();
			for (Iterator i = nameMap.values().iterator(); i.hasNext(); ) {
				SchemaPath schemaPath = (SchemaPath) i.next();
				if (schemaPath.inputHelper != null && schemaPath.inputHelper.trim().length() > 0)
					paths.add(schemaPath);
			}
			Collections.sort (paths, new SchemaPathComparator());
			this.inputHelperPaths = paths;
		}
		return this.inputHelperPaths;
	}
	
	private List initialFieldCollapsePaths = null;
	
	/**
	* Return a list of xpaths for which an initialFieldCollapse state has been specified"
	*/
	public List getInitialFieldCollapsePaths() {
		if (this.initialFieldCollapsePaths == null) {
			ArrayList paths = new ArrayList();
			for (Iterator i = nameMap.values().iterator(); i.hasNext(); ) {
				SchemaPath schemaPath = (SchemaPath) i.next();
				String val = schemaPath.initialFieldCollapse;
				if (val == null) continue;
				val = val.trim();
				if ("open".equals(val) || "closed".equals(val)) {
					paths.add(schemaPath.xpath);
					// prtln (val + ": " + schemaPath.xpath);
				}
			}
			// sort so we process in top-down fashion. this way lower nodes that were
			// supposed to be open won't get closed by higher nodes
			Collections.sort (paths);
			return this.initialFieldCollapsePaths = paths;
		}
		return this.initialFieldCollapsePaths;
	}
	
	/**
	 *  Returns list of SchemaPath instances whose valueType is contained in the given list of pathTypes.
	 *
	 * @param  valueTypes  NOT YET DOCUMENTED
	 * @return             SchemaPaths having requested valueTypes
	 */
	public List getSchemaPathsByValueTypes(List valueTypes) {
		ArrayList paths = new ArrayList();
		for (Iterator i = nameMap.values().iterator(); i.hasNext(); ) {
			SchemaPath schemaPath = (SchemaPath) i.next();
			if (valueTypes != null && valueTypes.contains(schemaPath.valueType))
				paths.add(schemaPath);
		}
		Collections.sort (paths, new SchemaPathComparator());
		return paths;
	}


	/**
	 *  Returns list of SchemaPath instances having specified valueType.
	 *
	 * @param  valueType  A SchemaPath valueType
	 * @return            SchemaPath instances of requested type
	 */
	public List getSchemaPathsByValueType(String valueType) {
		ArrayList paths = new ArrayList();
		for (Iterator i = nameMap.values().iterator(); i.hasNext(); ) {
			SchemaPath schemaPath = (SchemaPath) i.next();
			if (schemaPath.valueType != null && schemaPath.valueType.equals(valueType))
				paths.add(schemaPath);
		}
		Collections.sort (paths, new SchemaPathComparator());
		return paths;
	}

	private List readOnlyPaths = null;

	/**
	 *  Returns lists of xpaths instances representing fields configured as "readOnly".<p>
	 *
	 *  For example, ID fields are sometimes desireable to be read only"
	 *
	 * @return    xPaths for read only fields.
	 */
	public List getReadOnlyPaths() {
		if (this.readOnlyPaths == null) {
			ArrayList paths = new ArrayList();
			for (Iterator i = nameMap.values().iterator(); i.hasNext(); ) {
				SchemaPath schemaPath = (SchemaPath) i.next();
				if (schemaPath.readOnly)
					paths.add(schemaPath.xpath);
			}
			// prtln (" about to return " + paths.size() + " paths");
			this.readOnlyPaths = paths;
		}
		return this.readOnlyPaths;
	}


	/**
	 *  Add a SchemaPath.
	 *
	 * @param  schemaPath  SchemaPath instance to be added.
	 */
	public void putPath(SchemaPath schemaPath) {
		String xpath = schemaPath.xpath;
		pathMap.put(xpath, schemaPath);
		String name = schemaPath.pathName;
		nameMap.put(name, schemaPath);
	}


	/**
	 *  Gets a SchemaPath for specified xpath.
	 *
	 * @param  xpath  xpath for SchemaPath
	 * @return        A SchemaPath instance or null if none found.
	 */
	public SchemaPath getPathByPath(String xpath) {
		return (SchemaPath) pathMap.get(xpath);
	}


	/**
	 *  Gets a SchemaPath for specified pathName.
	 *
	 * @param  name  pathName defined in the framework config
	 * @return       A SchemaPath instance or null if none found.
	 */
	public SchemaPath getPathByName(String name) {
		return (SchemaPath) nameMap.get(name);
	}


	/**  NOT YET DOCUMENTED */
	public void clearPaths() {
		pathMap.clear();
		nameMap.clear();
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @return    NOT YET DOCUMENTED
	 */
	public String toString() {
		String ret = "\nSchemaPathMap: ";
		for (Iterator i = nameMap.values().iterator(); i.hasNext(); ) {
			SchemaPath schemaPath = (SchemaPath) i.next();
			ret += "\n" + schemaPath.toString();
		}
		return ret;
	}


	/**
	 *  Description of the Method
	 *
	 * @param  s  Description of the Parameter
	 */
	public static void prtln(String s) {
		System.out.println("SchemaPathMap: " + s);
	}
}

