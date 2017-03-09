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
import org.dlese.dpc.util.strings.FindAndReplace;
import org.dlese.dpc.schemedit.*;
import org.dlese.dpc.schemedit.vocab.*;

import org.dom4j.Document;
import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import org.dom4j.Attribute;
import org.dom4j.Node;

/**
 *  Extracts info to augment that of the Schema from a framework configuration
 *  file. <p>
 *
 *  Used by the {@link org.dlese.dpc.schemedit.MetaDataFramework} and is
 *  required for each of of the frameworks supported by DCS.
 *
 * @author    ostwald <p>
 *
 */
public class FrameworkConfigReader extends AbstractConfigReader {

	/**  NOT YET DOCUMENTED */
	protected static boolean debug = true;

	private SchemaPathMap schemaPathMap = null;
	private FieldInfoMap fieldInfoMap = null;
	private PageList pageList = null;
	private UserInfo userInfo = null;


	/**
	 *  Create a FrameworkConfigReader.
	 *
	 * @param  source         Description of the Parameter
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public FrameworkConfigReader(File source) throws Exception {
		super(source);
	}


	/**  Force update by clearing cached values */
	public void refresh() {
		this.schemaPathMap = null;
		this.fieldInfoMap = null;
		this.pageList = null;
		super.refresh();
	}


	/**
	 *  Gets the baseRenderLevel attribute of the FrameworkConfigReader object
	 *
	 * @return    The baseRenderLevel value
	 */
	public int getBaseRenderLevel() {
		try {
			String s = getNode("/frameworkConfigRecord/editorInfo/baseRenderLevel").getText();
			return Integer.parseInt(s);
		} catch (Throwable e) {
			return 2;
		}
	}


	/**
	 *  Sets the baseRenderLevel attribute of the FrameworkConfigReader object
	 *
	 * @param  level  The new baseRenderLevel value
	 */
	public void setBaseRenderLevel(int level) {
		getNode("/frameworkConfigRecord/editorInfo/baseRenderLevel").setText(Integer.toString(level));
	}


	/**
	 *  optional node
	 *
	 * @return    The discussionURL value
	 */
	public String getDiscussionURL() {
		/* return getNode ("/frameworkConfigRecord/discussionURL").getText(); */
		return getNodeText("/frameworkConfigRecord/editorInfo/discussionURL");
	}


	/**
	 *  Sets the discussionURL attribute of the FrameworkConfigReader object
	 *
	 * @param  s  The new discussionURL value
	 */
	public void setDiscussionURL(String s) {
		setNodeText("/frameworkConfigRecord/editorInfo/discussionURL", s);
	}


	/**
	 *  Gets the rebuildOnStart attribute of the FrameworkConfigReader object. If
	 *  no value is provided, or if the value is not "false", return "true".
	 *
	 * @return    The rebuildOnStart value
	 */
	public boolean getRebuildOnStart() {
		String bool = getNodeText("/frameworkConfigRecord/editorInfo/rebuildOnStart");
		if (bool != null && bool.equals("false")) {
			return false;
		}
		else {
			return true;
		}
	}


	/**
	 *  Sets the rebuildOnStart attribute of the FrameworkConfigReader object
	 *
	 * @param  bool  The new rebuildOnStart value
	 */
	public void setRebuildOnStart(boolean bool) {
		String s = "false";
		if (bool) {
			s = "true";
		}
		setNodeText("/frameworkConfigRecord/editorInfo/rebuildOnStart", s);
	}


	// -------- Schema paths ---------------------

	/**
	 *  Gets the schemaPathMap attribute of the FrameworkConfigReader object
	 *
	 * @return    The schemaPathMap value
	 */
	public SchemaPathMap getSchemaPathMap() {
		if (this.schemaPathMap == null) {
			// prtln ("getSchemaPathMap()");
			schemaPathMap = new SchemaPathMap();
			Node schemaPathsNode = getNode("/frameworkConfigRecord/schemaInfo/paths");
			if (schemaPathsNode != null) {
				Element schemaPathsElement = (Element) schemaPathsNode;
				for (Iterator i = schemaPathsElement.elementIterator(); i.hasNext(); ) {
					Element pathElement = (Element) i.next();
					SchemaPath schemaPath = new SchemaPath(pathElement);
					schemaPathMap.putPath(schemaPath);
				}
			}
		}
		return schemaPathMap;
	}

	public UserInfo getUserInfo() {
		if (this.userInfo == null) {
			// prtln ("getuserInfo()");
			
			Node userInfoNode = getNode("/frameworkConfigRecord/userInfo");
			if (userInfoNode != null) {
				this.userInfo = new UserInfo( (Element) userInfoNode);
			}
		}
		return userInfo;
	}
	

	/**
	 *  Not currently used - designed for future "pathSpec" which will be more
	 *  flexible in specifying path-specific attributes and behaviours.
	 *
	 * @return    The schemaPathMapNew value
	 */
	public SchemaPathMap getSchemaPathMapNew() {
		prtln("getSchemaPathMapNew()");
		if (this.schemaPathMap == null) {
			schemaPathMap = new SchemaPathMap();
			List schemaPathSpecs = getNodes("/frameworkConfigRecord/schemaInfo/paths/path/pathSpec");
			prtln(schemaPathSpecs.size() + " pathSpecs found");
			if (schemaPathSpecs != null && schemaPathSpecs.size() > 0) {

				for (Iterator i = schemaPathSpecs.iterator(); i.hasNext(); ) {
					Element pathSpec = (Element) i.next();
					SchemaPath schemaPath = new SchemaPath(pathSpec);
					schemaPathMap.putPath(schemaPath);
				}
			}
		}
		return schemaPathMap;
	}

	// ----------- end of schema paths ---------------

	/**
	 *  Gets the name attribute of the FrameworkConfigReader object
	 *
	 * @return    The name value
	 */
	public String getName() {
		return getNodeText("/frameworkConfigRecord/name");
	}


	/**
	 *  Sets the name attribute of the FrameworkConfigReader object
	 *
	 * @param  s  The new name value
	 */
	public void setName(String s) {
		setNodeText("/frameworkConfigRecord/name", s);
	}


	/**
	 *  Gets the recordsDir attribute of the FrameworkConfigReader object
	 *
	 * @return    The recordsDir value
	 */
	public String getRecordsDir() {
		return getNodeText("/frameworkConfigRecord/standAloneInfo/recordsDir");
	}


	/**
	 *  Sets the recordsDir attribute of the FrameworkConfigReader object
	 *
	 * @param  s  The new recordsDir value
	 */
	public void setRecordsDir(String s) {
		prtln("\n\n*** setting records dir to " + s + " ***\n\n");
		setNodeText("/frameworkConfigRecord/standAloneInfo/recordsDir", s);
	}
		
	
	/**
	 *  Gets the renderer attribute of the FrameworkConfigReader object
	 *
	 * @return    The renderer value
	 */
	public String getRenderer() {
		String renderer = null;
		try {
			renderer = getNodeText("/frameworkConfigRecord/editorInfo/renderer");
			if ("".equals(renderer.trim()))
				throw new Exception ("No renderer found");
		} catch (Throwable t) {
			prtln ("ERROR: " + t.getMessage());
			// prtln (Dom4jUtils.prettyPrint(this.getDocument()));
			renderer = "DleseEditorRenderer";
		}
		return renderer;
	}

	/**
	 *  Sets the renderer attribute of the FrameworkConfigReader object
	 *
	 * @param  s  The new renderer value
	 */
	public void setRenderer(String s) {
		setNodeText("/frameworkConfigRecord/editorInfo/renderer", s);
	}

	/**
	 *  Gets the renderer attribute of the FrameworkConfigReader object
	 *
	 * @return    The renderer value
	 */
	public String getBestPracticesLabel() {
		String bestPracticesLabel = null;
		try {
			bestPracticesLabel = getNodeText("/frameworkConfigRecord/editorInfo/bestPracticesLabel");
		} catch (Throwable t) {
			prtln ("ERROR: " + t.getMessage());
		}
		return bestPracticesLabel;
	}

	/**
	 *  Sets the renderer attribute of the FrameworkConfigReader object
	 *
	 * @param  s  The new renderer value
	 */
	public void setBestPracticesLabel(String s) {
		setNodeText("/frameworkConfigRecord/editorInfo/bestPracticesLabel", s);
	}

	/**
	 *  Gets the rootElementName attribute of the FrameworkConfigReader object
	 *
	 * @return    The rootElementName value
	 */
	public String getRootElementName() {
		return getNodeText("/frameworkConfigRecord/schemaInfo/rootElementName");
	}


	/**
	 *  Sets the rootElementName attribute of the FrameworkConfigReader object
	 *
	 * @param  s  The new rootElementName value
	 */
	public void setRootElementName(String s) {
		setNodeText("/frameworkConfigRecord/schemaInfo/rootElementName", s);
	}


	/**
	 *  Gets the sampleRecordPath attribute of the FrameworkConfigReader object
	 *
	 * @return    The sampleRecordPath value
	 */
	/* 	public String getSampleRecordPath() {
		return getNodeText("/frameworkConfigRecord/standAloneInfo/sampleRecordPath");
	} */

	/**
	 *  Sets the sampleRecordPath attribute of the FrameworkConfigReader object
	 *
	 * @return    The schemaURI value
	 */
	/* 	public void setSampleRecordPath(String s) {
		setNodeText("/frameworkConfigRecord/standAloneInfo/sampleRecordPath", s);
	} */

	/**
	 *  Gets the schemaURI attribute of the FrameworkConfigReader object
	 *
	 * @return    The schemaURI value
	 */
	public String getSchemaURI() {
		String uriStr = getNodeText("/frameworkConfigRecord/schemaInfo/schemaURI");
		if (uriStr != null)
			uriStr = SchemEditUtils.escapeUriSpaces(uriStr);
		return uriStr;
	}


	/**
	 *  Gets the vocabLayoutURI attribute of the FrameworkConfigReader object
	 *
	 * @return    The vocabLayoutURI value
	 */
	public String getVocabLayoutURI() {
		String uriStr = getNodeText("/frameworkConfigRecord/editorInfo/vocabLayoutURI");
		if (uriStr != null)
			uriStr = SchemEditUtils.escapeUriSpaces(uriStr);
		return uriStr;
	}


	/**
	 *  Sets the schemaURI attribute of the FrameworkConfigReader object
	 *
	 * @param  s  The new schemaURI value
	 */
	public void setSchemaURI(String s) {
		setNodeText("/frameworkConfigRecord/schemaInfo/schemaURI", s);
	}


	/**
	 *  Gets the xmlFormat attribute of the FrameworkConfigReader object
	 *
	 * @return    The xmlFormat value
	 */
	public String getXmlFormat() {
		return getNodeText("/frameworkConfigRecord/xmlFormat");
	}


	/**
	 *  Sets the xmlFormat attribute of the FrameworkConfigReader object
	 *
	 * @param  s  The new xmlFormat value
	 */
	public void setXmlFormat(String s) {
		setNodeText("/frameworkConfigRecord/xmlFormat", s);
	}


	/**
	 *  Gets the pageList attribute of the FrameworkConfigReader object
	 *
	 * @return    The pageList value
	 */
	public PageList getPageList() {
		if (this.pageList == null) {
			pageList = new PageList();
			Node editorPagesNode = getNode("/frameworkConfigRecord/editorInfo/editorPages");
			if (editorPagesNode != null) {
				Element fieldInfoElement = (Element) editorPagesNode;
				for (Iterator i = fieldInfoElement.elementIterator(); i.hasNext(); ) {
					Element editorPage = (Element) i.next();
					String pageLabel = editorPage.attributeValue("pageLabel");
					String elementName = editorPage.getText();
					pageList.addPage(elementName, pageLabel);
				}
			}
			String firstPage = getNodeText("/frameworkConfigRecord/editorInfo/firstPage");
			if (firstPage == null || firstPage.trim().length() == 0)
				firstPage = this.getRootElementName();
			pageList.setFirstPage(firstPage);
			pageList.setHomePage(getXmlFormat() + ".index");
		}
		return pageList;
	}


	/**
	 *  Create a FieldInfoMap containing information about each fieldInfo path
	 *  specified in the config file. FieldInfo elements point to a FieldInfo file
	 *  containing best practices, definitions, and other information about a
	 *  particular element in this framework.
	 *
	 * @return    The fieldInfoMap value
	 */
	public FieldInfoMap getFieldInfoMap() {
		// prtln ("getFieldInfoMap(): xmlFormat: " + getXmlFormat());

		if (this.fieldInfoMap == null) {

			String fieldInfoUri = getNodeText("/frameworkConfigRecord/editorInfo/fieldInfoURI");
			if (fieldInfoUri == null || fieldInfoUri.length() == 0) {
				// prtln ("no fieldInfoUrl found");
				fieldInfoMap = new NewFieldInfoMap();
			}
			else {
				// ensure that uri does not contain spaces
				fieldInfoUri = SchemEditUtils.escapeUriSpaces(fieldInfoUri);

				// choose FieldInfoMap class based on the fieldInfoUri {
				if (fieldInfoUri.indexOf("fields-list.xml") > -1) {
					// prtln ("instantiating NEW FieldInfoMap");
					fieldInfoMap = new NewFieldInfoMap(fieldInfoUri);
				}
				else
					fieldInfoMap = new OldFieldInfoMap(fieldInfoUri);
				try {
					fieldInfoMap.init();
				} catch (Exception e) {
					prtln("fieldInfoMap init error: " + e);
					// e.printStackTrace();
				}
			}
		}

		return fieldInfoMap;
	}


	/**
	 *  Print a line to standard out.
	 *
	 * @param  s  The String to print.
	 */
	protected static void prtln(String s) {
		if (debug) {
			System.out.println("FrameworkConfigReader: " + s);
		}
	}

}

