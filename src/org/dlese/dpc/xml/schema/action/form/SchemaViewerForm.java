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
package org.dlese.dpc.xml.schema.action.form;

import org.dlese.dpc.xml.schema.*;
import org.dlese.dpc.xml.schema.action.GlobalDefReporter;
import org.dlese.dpc.xml.*;
import org.dlese.dpc.serviceclients.remotesearch.RemoteResultDoc;

import org.dlese.dpc.vocab.MetadataVocab;
import org.dlese.dpc.schemedit.MetaDataFramework;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.Namespace;
import org.dom4j.io.XMLWriter;
import org.dom4j.io.HTMLWriter;
import org.dom4j.io.OutputFormat;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionError;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionServlet;
import org.apache.struts.util.MessageResources;
import org.apache.struts.util.LabelValueBean;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.io.*;
import java.text.*;
import java.net.*;
import java.util.regex.*;

/**
 *  Controller for the SchemaViewer app.
 *
 *@author    ostwald
 */
public class SchemaViewerForm extends ActionForm {

	private boolean debug = false;
	private SchemaHelper schemaHelper = null;
	private MetadataVocab vocab = null;
	private MetaDataFramework framework = null;
	private XMLWriter writer = null;
	private ByteArrayOutputStream buffer = new ByteArrayOutputStream();

	private ArrayList xpathHistory = null;
	private String path = null;
	private String typeName = null;
	private SchemaNode schemaNode = null;
	private GlobalDef globalDef = null;
	private String frameworkName = "adn";
	private String xmlFormat = "adn";
	private List frameworks = null;
	
	private String reportFunction = null;
	private String [] selectedFrameworks; // selected frameworks
	private Map report = null;

	
	public void reset(ActionMapping mapping, HttpServletRequest request) {
		prtln ("reset()");
		// selectedFrameworks = new String[]{};
		selectedFrameworks = null;
	}
	
/* 	public List getReportFunctions () {
		List f = new ArrayList ();
		f.add ("simpleType");
		f.add ("complexType");
		f.add ("derivedModel");
		return f;
	} */
	
	public String [] getReportFunctions () {
		return GlobalDefReporter.REPORT_FUNCTIONS;
	}
	
	public String getReportFunction () {
		return reportFunction;
	}
	
	public void setReportFunction (String fn) {
		reportFunction = fn;
	}
	
	public Map getReport () {
		return report;
	}
	
	public void setReport (Map rpt) {
		report = rpt;
	}

	/**
	 *  Gets the frameworks attribute of the SchemaViewerForm object
	 *
	 *@return    The frameworks value
	 */
	public List getFrameworks() {
		if (frameworks == null)
			frameworks = new ArrayList();
		return frameworks;
	}
	/**
	 *  Sets the frameworks attribute of the SchemaViewerForm object
	 *
	 *@param  list  The new frameworks value
	 */
	public void setFrameworks(List list) {
		frameworks = list;
	}
	
	public String [] getSelectedFrameworks () {
/*  		if (selectedFrameworks == null)
			selectedFrameworks = new String []{}; */
		return selectedFrameworks;
	}
	
	public void setSelectedFrameworks (String [] sf) {
		selectedFrameworks = sf;
	}

	
		/**
	 *  Gets the xmlFormat attribute of the SchemEditForm object
	 *
	 *@return    The xmlFormat value
	 */
/* 	public String getXmlFormat() {
		if (framework != null)
			return framework.getXmlFormat();
		else
			return "";
	} */


	/**
	 *  Sets the xmlFormat attribute of the SchemEditForm object
	 *
	 *@param  s  The new xmlFormat value
	 */
/* 	public void setXmlFormat(String s) {
		xmlFormat = s;
	} */

	/**
	 *  Gets the displayType attribute of the SchemaViewerForm object
	 *
	 *@return    The displayType value
	 */
	public String getDisplayType() {
		return globalDef.toString();
	}


	/**
	 *  Gets the displaySchemaNode attribute of the SchemaViewerForm object
	 *
	 *@return    The displayItem value
	 */
	public String getDisplaySchemaNode() {
		return schemaNode.toString();
	}


	/**
	 *  Gets the path attribute of the SchemaViewerForm object
	 *
	 *@return    The path value
	 */
	public String getPath() {
		return path;
	}


	/**
	 *  Sets the path attribute of the SchemaViewerForm object
	 *
	 *@param  path  The new path value
	 */
	public void setPath(String path) {
		this.path = path;
	}


	/**
	 *  Gets the typeName attribute of the SchemaViewerForm object
	 *
	 *@return    The typeName value
	 */
 	public String getTypeName() {
		return typeName;
	}

	public void setTypeName (String s) {
		typeName = s;
	}

	/**
	 *  Gets the schemaNode attribute of the SchemaViewerForm object
	 *
	 *@return    The schemaNode value
	 */
	public SchemaNode getSchemaNode() {
		return schemaNode;
	}

	public String getSchemaNodeName () {
		if (schemaNode != null)
			return XPathUtils.getLeaf(schemaNode.getXpath());
		else
			return "";
	}

	/**
	 *  Sets the schemaNode attribute of the SchemaViewerForm object
	 *
	 *@param  schemaNode  The new schemaNode valueead
	 */
	public void setSchemaNode(SchemaNode schemaNode) {
		this.schemaNode = schemaNode;
	}

	public boolean getSchemaNodeIsRequired () {
		if (schemaNode != null)
			return schemaNode.isRequired();
		else
			return false;
	}
	
	public boolean getSchemaNodeIsAttribute () {
		if (schemaNode != null)
			return schemaNode.isAttribute();
		else
			return false;
	}
	
	public boolean getSchemaNodeIsElement () {
		if (schemaNode != null)
			return schemaNode.isElement();
		else
			return false;
	}

	public List getSubstitutionGroupMembers () {
		List members = new ArrayList ();
		String baseUrl = "schema.do?command=doPath&path=";
		String parentPath = XPathUtils.getParentXPath(path);
		if (schemaNode.isHeadElement()) {
			Iterator elements = schemaNode.getSubstitutionGroup().iterator();
			while (elements.hasNext()) {
				GlobalElement element = (GlobalElement) elements.next();
				String iqn = element.getQualifiedInstanceName();
				// members.add (iqn);
				String href = baseUrl + parentPath + "/" + iqn;
				members.add( "<a href=\'" + href + "\'>" + iqn + "</a>");
			}
		}
		return members;
	}
	
	public String getHeadElement () {
		if (schemaNode != null && 
			globalDef != null && 
			globalDef.getSchemaReader() != null &&
			schemaNode.isSubstitutionGroupMember()) {
				
			String headElementName = schemaNode.getHeadElementName();
			String iqn = globalDef.getSchemaReader().getInstanceQualifiedName(headElementName);
			String baseUrl = "schema.do?command=doPath&path=";
			String parentPath = XPathUtils.getParentXPath(path);
			String href = baseUrl + parentPath + "/" + iqn;
			return "<a href=\'" + href + "\'>" + iqn + "</a>";
		}
		return "";
	}

	/**
	 *  Gets the globalDef attribute of the SchemaViewerForm object
	 *
	 *@return    The globalDef value
	 */
	public GlobalDef getGlobalDef() {
		return globalDef;
	}


	/**
	 *  Sets the globalDef attribute of the SchemaViewerForm object
	 *
	 *@param  def  The new globalDef value
	 */
	public void setGlobalDef(GlobalDef def) {
		globalDef = def;
	}
	
	public boolean getIsSimpleType () {
		if (globalDef != null)
			return (globalDef instanceof SimpleType);
		else
			return false;
	}
	
	public boolean getIsComplexType () {
		if (globalDef != null)
			return (globalDef instanceof ComplexType);
		else
			return false;
	}
	
	
	public boolean getDefIsEnumeration () {
		if (getIsSimpleType())
			return ((SimpleType)globalDef).isEnumeration();
		else
			return false;
	}

	public boolean getDefIsUnion () {
		if (getIsSimpleType())
			return ((SimpleType)globalDef).isUnion();
		else
			return false;
	}
	
	public boolean getDefIsBuiltin () {
		return globalDef.isBuiltIn();
	}
	
	/**
	 *  Gets the globalDef attribute of the SchemaViewerForm object
	 *
	 *@return    The globalDef value
	 */
	public MetaDataFramework getFramework() {
		return framework;
	}


	/**
	 *  Sets the framework attribute of the SchemaViewerForm object
	 *
	 *@param  mdf  The new framework value
	 */
	public void setFramework(MetaDataFramework mdf) {
		framework = mdf;
	}


	/**
	 *  Gets the globalDef attribute of the SchemaViewerForm object
	 *
	 *@return    The globalDef value
	 */
	public String getFrameworkName() {
		if (framework != null)
			return framework.getName();
		else
			return frameworkName;
	}


	/**
	 *  Sets the framework attribute of the SchemaViewerForm object
	 *
	 *@param  val  The new framework value
	 */
	public void setFrameworkName(String val) {
		frameworkName = val;
	}


	/**
	 *  Gets the typeElement attribute of the SchemaViewerForm object
	 *
	 *@return    The typeElement value
	 */
	public String getTypeElement() {
		try {
			return formatXML(globalDef.getElement());
		} catch (Exception e) {
			return "unable to format element: " + e;
		}
	}


	/**
	 *  Gets the prettyTypeElement attribute of the SchemaViewerForm object
	 *
	 *@return    The prettyTypeElement value
	 */
	public String getPrettyTypeElement() {
		String pp = null;
		if (globalDef == null || globalDef.isBuiltIn())
			return "Built-in type definition";
		try {
			pp = prettyPrint(globalDef.getElement());
		} catch (Exception e) {
			return "unable to prettyPrint element: " + e;
		}
		return linkify(pp, path);
	}


	/**
	 *  Gets the minimalTree attribute of the SchemaViewerForm object
	 *
	 *@return    The minimalTree value
	 */
	public String getMinimalTree() {
		String pp = null;
		if (schemaHelper == null) {
			// prtln ("\tschemaHelper is null");
			return "";
		}
		if (path == null || path.trim().length() == 0) {
			// prtln ("\tpath is empty");
			return "";
		}
		
		prtln ("getMinimalTree() with path: " + path);
		
		Element tree = null;
		try {
			tree = schemaHelper.getNewElement(path);
		} catch (Exception e) {
			e.printStackTrace();
			return "could not construct minimal tree for " + path;
		}
		
		if (tree == null) {
			// prtln("\ttree is null!");
			return "";
		}
		try {
			pp = prettyPrint(tree);
		} catch (Exception e) {
			return "unable to prettyPrint tree: " + e;
		}
		Pattern p = Pattern.compile("<");
		pp = p.matcher(pp).replaceAll("&lt;");

		return pp;
	}


	/**
	 *  Gets the breadCrumbs attribute of the SchemaViewerForm object
	 *
	 *@return    The breadCrumbs value
	 */
	public String getBreadCrumbs() {
		return new BreadCrumbs(path).toString();
	}


	/**
	 *  Constructor
	 */
	public SchemaViewerForm() {
		path = "/itemRecord";
		typeName = "geospatialCoverageType";
		writer = getXMLWriter();
	}


	/**
	 *  Gets the xMLWriter attribute of the SchemaViewerForm object
	 *
	 *@return    The xMLWriter value
	 */
	private XMLWriter getXMLWriter() {
		if (writer != null) {
			return writer;
		}

		OutputFormat format = new OutputFormat("  ", true);
		XMLWriter writer = null;
		try {
			writer = new XMLWriter(buffer, format);
		} catch (Exception ex) {
			prtln("getXMLWriter() failed: " + ex);
			return null;
		}
		writer.setEscapeText(true);
		return writer;
	}


	/**
	 *  Sets the schemaHelper attribute of the SchemaViewerForm object
	 *
	 *@param  schemaHelper  The new schemaHelper value
	 */
	public void setSchemaHelper(SchemaHelper schemaHelper) {
		this.schemaHelper = schemaHelper;
	}


	/**
	 *  Gets the schemaHelper attribute of the SchemaViewerForm object
	 *
	 *@return    The schemaHelper value
	 */
	public SchemaHelper getSchemaHelper() {
		return schemaHelper;
	}


	/**
	 *  Constructor for the setVocab object
	 *
	 *@param  vocab
	 */
	public void setVocab(MetadataVocab vocab) {
		this.vocab = vocab;
	}


	/**
	 *  Gets the vocab attribute of the SchemaViewerForm object
	 *
	 *@return    The vocab value
	 */
	public MetadataVocab getVocab() {
		return vocab;
	}
	
	public String [] getUnionMembers () {
		if (getDefIsUnion()) {
			SimpleType def = (SimpleType)globalDef;
			return def.getUnionMemberTypeNames();
		}
		return null;
	}

	public List getEnumerationOptions () {
		LabelValueBean [] lvb = getEnumerationOptions (path);
		List options = new ArrayList ();
		for (int i=0;i<lvb.length;i++) {
			options.add (lvb[i].getValue());
		}
		return options;
	}
	
	/**
	 *  Gets the enumerationOptions attribute of the SchemaViewerForm object
	 *
	 *@param  xpath  Description of the Parameter
	 *@return        The enumerationOptions value
	 */
	public LabelValueBean[] getEnumerationOptions(String xpath) {
		// prtln ("getEnumerationOptions() with xpath = " + xpath);
		LabelValueBean[] emptyArray = new LabelValueBean[]{};
		GlobalDef globalDef = schemaHelper.getGlobalDefFromXPath(xpath);

		if (globalDef == null) {
			prtln("globalDef not found for " + xpath);
			return emptyArray;
		}

		// prtln ("globalDef: " + globalDef.toString());

		if (globalDef.getDataType() != GlobalDef.SIMPLE_TYPE) {
			prtln("a SIMPLE_TYPE is required!");
			return emptyArray;
		}

		SimpleType simpleType = (SimpleType) globalDef;
		if ((!simpleType.isEnumeration()) && (!simpleType.isUnion())) {
			prtln("simpleType (" + simpleType.getQualifiedInstanceName() + " is not an enumeration");
			return emptyArray;
		}
		List values = schemaHelper.getEnumerationValues(simpleType.getQualifiedInstanceName(), false);
		
		if (values == null) {
			prtln ("WARNING: Unable to obtain enumeration values from " + simpleType.getQualifiedInstanceName());
			return emptyArray;
		}
		
		LabelValueBean[] options = new LabelValueBean[values.size()];
		for (int i = 0; i < values.size(); i++) {
			String value = (String) values.get(i);
			options[i] = new LabelValueBean(value, value);
		}
		prtln("getEnumerationOptions() returning " + options.length + " LabelValueBeans");
		return options;
	}


	/**
	 *  Gets the selectOptions attribute of the SchemaViewerForm object
	 *
	 *@param  xpath  Description of the Parameter
	 *@return        The selectOptions value
	 */
	public LabelValueBean[] getSelectOptions(String xpath) {
		LabelValueBean[] enumerationOptions = getEnumerationOptions(xpath);
		LabelValueBean[] selectOptions = new LabelValueBean[enumerationOptions.length + 1];
		selectOptions[0] = new LabelValueBean("", "");
		for (int i = 0; i < enumerationOptions.length; i++) {
			selectOptions[i + 1] = enumerationOptions[i];
		}
		return selectOptions;
	}


	/**
	 *  Description of the Method
	 *
	 *@param  node  Description of the Parameter
	 *@return       Description of the Return Value
	 */
	private String formatXML(Node node) {
		try {
			writer.write(node);
		} catch (Exception e) {
			return "formatXML failed! " + e;
		}
		String ret = buffer.toString();
		try {
			writer.flush();
		} catch (Exception e) {
			prtln("couldn't flush buffer");
		}
		return ret;
	}


	/**
	 *  Description of the Method
	 *
	 *@param  node           Description of the Parameter
	 *@return                Description of the Return Value
	 *@exception  Exception  Description of the Exception
	 */
	private String prettyPrint(Node node)
		throws Exception {
		StringWriter sw = new StringWriter();
		OutputFormat format = OutputFormat.createPrettyPrint();
		//These are the default formats from createPrettyPrint, so you needn't set them:
		//  format.setNewlines(true);
		//  format.setTrimText(true);
		format.setXHTML(true);
		//Default is false, this produces XHTML
		HTMLWriter ppWriter = new HTMLWriter(sw, format);
		ppWriter.write(node);
		ppWriter.flush();
		return sw.toString();
	}

	private String getGloballyQualifiedName (String name, SchemaReader schemaReader) {
		String iqn = schemaReader.getInstanceQualifiedName(name);
		if (schemaHelper.getNamespaceEnabled() && !NamespaceRegistry.isQualified(iqn)) {
			NamespaceRegistry globalNamespaces = schemaHelper.getDefinitionMiner().getNamespaces();
			String prefix = globalNamespaces.getNamedDefaultNamespace().getPrefix();
			iqn = NamespaceRegistry.makeQualifiedName(prefix, iqn);
		}
		return iqn;
	}

	/**
	 *  Description of the Method
	 *
	 *@param  s      Description of the Parameter
	 *@param  xpath  Description of the Parameter
	 *@return        Description of the Return Value
	 */
	private String linkify(String s, String xpath) {
		String baseUrl = "schema.do?command=doPath&path=";
		Pattern p;
		Matcher m;
		int index;
		SchemaReader schemaReader = globalDef.getSchemaReader();
		Namespace schemaNamespace = schemaReader.getNamespaces().getSchemaNamespace();

		prtln ("linkify() ... globalDef: " + globalDef.toString());
		
		// remove comments
		p = Pattern.compile("<!--.+?-->", Pattern.DOTALL);
		index = 0;
		while (true) {
			m = p.matcher(s);

			// replace occurrences one by one
			if (m.find(index)) {
				String content = s.substring(m.start(), m.end());
				s = s.substring(0, m.start()) + s.substring(m.end());
				index = m.start();
			}
			else {
				break;
			}
		}
		
		
		// change all open brackets to entity
		p = Pattern.compile("<");
		s = p.matcher(s).replaceAll("&lt;");
		

		// linkify element names
		String elementPat = "&lt;" + schemaNamespace.getPrefix() + ":element name=\"";
		String namePatStr = elementPat + ".+?\"";
		p = Pattern.compile(namePatStr, Pattern.DOTALL);
		index = 0;
		while (true) {
			m = p.matcher(s);

			// replace occurrences one by one
			if (m.find(index)) {
				String content = s.substring(m.start(), m.end());
				String name = content.substring(elementPat.length(), content.length() - 1);
				prtln ("name: " + name + " ...");
				
/* 				String iqn = schemaReader.getInstanceQualifiedName(name);
				if (schemaHelper.getNamespaceEnabled() && !NamespaceRegistry.isQualified(iqn)) {
					NamespaceRegistry globalNamespaces = schemaHelper.getDefinitionMiner().getNamespaces();
					String prefix = globalNamespaces.getNamedDefaultNamespace().getPrefix();
					iqn = NamespaceRegistry.makeQualifiedName(prefix, iqn);
					
				} */
				String iqn = this.getGloballyQualifiedName(name, schemaReader);
				String href = baseUrl + xpath + "/" + iqn;
				String replaceStr = "<a href=\'" + href + "\'>" + name + "</a>";
				s = s.substring(0, m.start() + elementPat.length()) + replaceStr + s.substring(m.end() - 1);
				index = m.end();
			}
			else {
				break;
			}
		}
		
		// linkify refs
		prtln ("linkify refs: schemaNamespace.prefix: " + schemaNamespace.getPrefix());
		String refPat = "&lt;" + schemaNamespace.getPrefix() + ":element ref=\"";
		String refPatStr = refPat + ".+?\"";
		p = Pattern.compile(refPatStr, Pattern.DOTALL);
		index = 0;
		while (true) {
			m = p.matcher(s);

			// replace occurrences one by one
			if (m.find(index)) {
				String content = s.substring(m.start(), m.end());
				String ref = content.substring(refPat.length(), content.length() - 1);
				prtln ("ref: " + ref + " ...");
				
				String iqn = schemaReader.getInstanceQualifiedName(ref);
				prtln ("... iqn: " + iqn);
				
				String href = baseUrl + xpath + "/" + iqn;
				String replaceStr = "<a href=\'" + href + "\'>" + ref + "</a>";
				s = s.substring(0, m.start() + refPat.length()) + replaceStr + s.substring(m.end() - 1);
				index = m.end();
			}
			else {
				break;
			}
		}
		
		// linkify base types
		baseUrl = "schema.do?command=doType&typeName=";
		String basePat = "&lt;" + schemaNamespace.getPrefix() + ":extension base=\"";
		String basePatStr = basePat + ".+?\"";
		p = Pattern.compile(basePatStr, Pattern.DOTALL);
		index = 0;
		while (true) {
			m = p.matcher(s);

			// replace occurrences one by one
			if (m.find(index)) {
				String content = s.substring(m.start(), m.end());
				String base = content.substring(basePat.length(), content.length() - 1);
				prtln ("base: " + base + " ...");
				
				String iqn = schemaReader.getInstanceQualifiedName(base);
				// String iqn = this.getGloballyQualifiedName(base, schemaReader);
				prtln ("... iqn: " + iqn);
				
				// String href = baseUrl + xpath + "/" + iqn;
				String href = baseUrl + iqn + "&path=" + xpath;
				String replaceStr = "<a href=\'" + href + "\'>" + base + "</a>";
				s = s.substring(0, m.start() + basePat.length()) + replaceStr + s.substring(m.end() - 1);
				index = m.end();
			}
			else {
				break;
			}
		}
		
		// linkify dataTypes
		baseUrl = "schema.do?command=doType&typeName=";
		String prefix = "type=\"";
		String typePatStr = prefix + ".+?\"";
		
		p = Pattern.compile(typePatStr, Pattern.DOTALL);
		index = 0;
		while (true) {
			m = p.matcher(s);

			// replace occurrences one by one
			if (m.find(index)) {
				String content = s.substring(m.start(), m.end());
				String typeName = content.substring(prefix.length(), content.length() - 1);
				prtln ("typeName: " + typeName + " ...");
				
				String iqn = schemaReader.getInstanceQualifiedName(typeName);
				prtln ("... iqn: " + iqn);
				String href = baseUrl + iqn + "&path=" + xpath;
				String replaceStr = "<a href=\'" + href + "\'>" + typeName + "</a>";
				s = s.substring(0, m.start() + prefix.length()) + replaceStr + s.substring(m.end() - 1);
				index = m.end();
			}
			else {
				break;
			}
		}
		return s;
	}

	/**
	 *  Output a line of text to standard out, with datestamp, if debug is set to
	 *  true.
	 *
	 *@param  s  The String that will be output.
	 */
	protected final void prtln(String s) {
		if (debug) {
			System.out.println("SchemaViewerForm: " + s);
		}
	}

}

