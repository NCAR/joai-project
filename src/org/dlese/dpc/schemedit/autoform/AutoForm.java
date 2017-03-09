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
package org.dlese.dpc.schemedit.autoform;

import org.dlese.dpc.xml.*;
import org.dlese.dpc.xml.schema.*;
import org.dlese.dpc.schemedit.MetaDataFramework;
import org.dlese.dpc.schemedit.test.TesterUtils;
import org.dlese.dpc.schemedit.SchemEditUtils;
import org.dlese.dpc.util.Files;
import org.dlese.dpc.util.strings.FindAndReplace;

import java.io.*;
import java.util.*;
import java.util.regex.*;

import java.net.URL;
import org.dom4j.Node;
import org.dom4j.Attribute;
import org.dom4j.Element;
import org.dom4j.Document;
import org.dom4j.DocumentFactory;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;

/**
 *  Class to automatically generate jsp pages (using a Renderer class such as {@link
 *  org.dlese.dpc.schemedit.autoform.DleseEditorRenderer}) for editing and viewing of schemedit-based xml
 *  documents.<p>
 *
 *  Called from command line for debugging as well as from {@link org.dlese.dpc.schemedit.MetaDataFramework}
 *  at start-up time and after run-time reconfiguration.
 *
 * @author     ostwald
 *
 */
public class AutoForm {
	private static boolean debug = true;
	private static boolean verbose = false;

	// lib directory is where templates live
	String libDir = "../../../schemedit-project/web/lib";

//	private WebServiceClient webServiceClient = null;
	/**  NOT YET DOCUMENTED */
	protected Document instanceDocument = null;
	/**  NOT YET DOCUMENTED */
	protected XMLWriter writer;
	/**  NOT YET DOCUMENTED */
	protected DocumentFactory df = DocumentFactory.getInstance();
	/**  NOT YET DOCUMENTED */
	protected SchemaHelper sh;
	/**  NOT YET DOCUMENTED */
	protected File schemaFile;
	/**  NOT YET DOCUMENTED */
	protected MetaDataFramework framework;
	/**  NOT YET DOCUMENTED */
	protected String formBeanName = "sef";


	/**
	 *  Constructor for the AutoForm object
	 *
	 * @param  framework  Description of the Parameter
	 */
	public AutoForm(MetaDataFramework framework) {
		this.framework = framework;
		this.sh = framework.getSchemaHelper();
		instanceDocument = sh.getInstanceDocument();
		if (instanceDocument == null) {
			prtln("instanceDocument not initialized in schemaHelper");
		}
		writer = Dom4jUtils.getXMLWriter();
	}


	/**
	 *  Constructor for the Stand-along AutoForm object, meaning it is created from command line rather than via
	 *  schemedit.
	 *
	 * @param  xmlFormat                  Description of the Parameter
	 * @exception  SchemaHelperException  Description of the Exception
	 * @exception  Exception              NOT YET DOCUMENTED
	 */
	public AutoForm(String xmlFormat)
		 throws Exception, SchemaHelperException {

		framework = TesterUtils.getFramework(xmlFormat);
		sh = framework.getSchemaHelper();

		instanceDocument = sh.getInstanceDocument();
		if (instanceDocument == null) {
			prtln("instanceDocument not initialized in schemaHelper");
		}
		writer = Dom4jUtils.getXMLWriter();
	}


	/**
	 *  The main program for the AutoForm class. The first argument is command, the second is arg (if nec);
	 *
	 * @param  args           The command line arguments
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public static void main(String[] args) throws Exception {
		TesterUtils.setSystemProps();
		verbose = true;
		String xmlFormat = "concepts";
		String command = "renderAndWrite";
		String xpath = ""; // "/collectionRecord/general";

		if (args.length == 0) {
			prtln("Usage: \n\tframework (adn)\n\t command (renderAndWrite or batchRender) \n\txpath");
			return;
		}

		try {
			xmlFormat = args[0];
			command = args[1];
			xpath = args[2];
		} catch (Exception e) {}

		prtln("--------------------------------------------------------------------------");
		prtln("AutoForm:");
		prtln("\tframework: " + xmlFormat);
		prtln("\tcommand: " + command);
		prtln("\txpath: " + xpath);

		// RendererHelper.setVerbose (true);

		AutoForm autoForm = null;
		try {
			autoForm = new AutoForm(xmlFormat);
		} catch (Exception e) {
			prtln(e.getMessage());
			return;
		}

		// prtln (autoForm.framework.toString());

		if (command.equals("batchRender")) {
			autoForm.batchRenderAndWrite();
			return;
		}

		if (command.equals("renderAndWrite")) {
			prtln("autoForm for " + xpath);
			/* autoForm.setRendererClassName ("CollapsibleJspRenderer"); */
			autoForm.renderAndWrite(xpath);
		}
	}


	/**
	 *  Description of the Method
	 *
	 * @param  xpath          Description of the Parameter
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public void renderAndWrite(String xpath) throws Exception {

		File dest = getJspDest(XPathUtils.getNodeName(xpath));
		renderAndWrite(xpath, dest);
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  xpath          NOT YET DOCUMENTED
	 * @param  dest           NOT YET DOCUMENTED
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public void renderAndWrite(String xpath, File dest) throws Exception {
		Element e = render(xpath);

		if (!writeJsp(e, dest, getMasterJspHeader())) {
			throw new Exception("failed to write jsp to " + dest);
		}
	}


	/**
	 *  Create a jsp file for each top-level element of the schema
	 *
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public void batchRenderAndWrite() throws Exception {
		List elements = instanceDocument.getRootElement().elements();
		// prtln("batchRenderAndWrite found " + elements.size() + " elements");
		for (Iterator i = elements.iterator(); i.hasNext(); ) {
			Element child = (Element) i.next();
			String xpath = child.getPath();
			String pageName = XPathUtils.getNodeName(xpath);
			// prtln(pageName + " (" + xpath + ")");

			Element e = render(xpath);
			if (e != null &&
				!writeJsp(e, getBatchJspDest(pageName), this.getComponentJspHeader())) {
					throw new Exception ("batchRenderAndWrite could not write jsp for " + pageName);
			}
		}
	}

	/**
	 *  Gets the rendererClassName attribute of the AutoForm object
	 *
	 * @return    The rendererClassName value
	 */
	protected String getRendererClassName() {
		return framework.getRenderer();
	}

	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @return    NOT YET DOCUMENTED
	 */
	public Element render() {
		return render(null);
	}	

	/**
	 *  Produce a {@link org.dom4j.Element} representing an editor for the node at xpath. The Element returned by
	 *  render is eventually converted to JSP.
	 *
	 * @param  xpath  XPath to a Node in the Schema
	 * @return        An Element representing an editor for the given schema node
	 */
	public Element render(String xpath) {
		// Element e = (Element) instanceDocument.selectSingleNode(xpath);
		Element instanceDocElement = (Element) sh.getInstanceDocNode(RendererHelper.normalizeXPath(xpath));
		if (instanceDocElement == null) {
			prtln("renderer couldn't find node for " + xpath);
			return null;
		}
		Element root = df.createElement("div");
		// prtln("\nformBeanName: " + formBeanName);
		RendererHelper rhelper = new RendererHelper(root, framework, formBeanName, getRendererClassName());
		RendererImpl renderer = null;
		try {
			renderer = rhelper.getRenderer(xpath, root);
			// renderer = getRenderer (rendererClassName, xpath, root);
		} catch (Exception e) {
			prtln("Unable to create renderer: " + e.getMessage());
			return null;
		}

		renderer.renderNode();
		rhelper.destroy();
		return root;
	}


	/**
	 *  Perform any modifications to the XML to create legal JSP. Some strings, such as tag-like notation (e.g.,
	 *  "c:set") are not convenient in XML processing, since they are interpreted as namespaces, so they are
	 *  encoded by the renderer using a convention (e.g., "c__set") and then decoded here to convert to JSP
	 *
	 * @param  e  Element produced by renderer
	 * @return    JSP representation of Element
	 */
	public static String elementToJsp(Element e) {
		String s = Dom4jUtils.prettyPrint(e);
		Pattern p = Pattern.compile("__");
		Matcher m = p.matcher(s);
		s = m.replaceAll(":");

		// replace ^V^ pattern with single quotes
		p = Pattern.compile("\\^v\\^");
		m = p.matcher(s);
		s = m.replaceAll("'");
		
		s = replaceDirectives (s);
		
		return s;
	}

	static String replaceDirectives (String s) {
		// pattern to detect page directives (e.g., includes) that were inserted
		// into Document as text and therefore have the tags escaped.
		Pattern p = Pattern.compile("&lt;(%@.*?%)&gt;");
		Matcher m = null;
		while (true) {
			m = p.matcher(s);
			if (m.find()) {
				String repl = "<" + m.group(1) + ">";
				s = m.replaceFirst (repl);
			}
			else
				break;
		}
		return s;
	}

	/**
	 *  Path for writing component jsp pages. 
	 *
	 * @param  pageName       NOT YET DOCUMENTED
	 * @return                The batchJspDest value
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	protected File getBatchJspDest(String pageName) throws Exception {
		return getJspDest(pageName);
	}


	/**
	 *  Path for writing master jsp files.
	 *
	 * @param  pageName       NOT YET DOCUMENTED
	 * @return                The jspDest value
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	protected File getJspDest(String pageName) throws Exception {
		String fileName = pageName + ".jsp";
		File autoFormDir = new File(framework.getAutoFormDir());
		return new File(autoFormDir, Files.encode(fileName));
	}


	/**
	 *  JSP code to insert at the top of master jsp files.
	 *
	 * @return    The masterJspHeader value
	 */
	protected String getMasterJspHeader() {
		String header = "<%@ include file=\"/lib/includes.jspf\" %>\n\n";
		header += "<%@ page import=\"org.dlese.dpc.schemedit.display.CollapseBean\" %>\n\n";
		header +=
			"<bean:define id=\"collapseBean\" name=\"" + formBeanName + "\" property=\"collapseBean\" type=\"CollapseBean\" />\n\n";
		return header;
	}


	/**
	 *  JSP code to insert at the top of component jsp files. In the case of AutoForm, where the components are
	 * on separate JSP pages, the component and master headers are the same. In subclasses, the component JSP pages
	 * maybe included (via jsp:include) in the master, and therefore they may require a different header.
	 *
	 * @return    The componentJspHeader value
	 */
	protected String getComponentJspHeader() {
		return getMasterJspHeader();
	}


	/**
	 *  Writes Element to disk as JSP page to be included in a master page at run time.
	 *
	 * @param  element    Element representing editor page
	 * @param  dest       NOT YET DOCUMENTED
	 * @param  jspHeader  NOT YET DOCUMENTED
	 * @return            true if JSP was successfully written
	 */
	protected boolean writeJsp(Element element, File dest, String jspHeader) {

		String autoJsp = jspHeader + elementToJsp(element);
		String errorMsg;

		try {

			File destDir = dest.getParentFile();
			if (!destDir.exists() && !destDir.mkdirs()) {
				errorMsg = "couldn't find or make a directory at " + destDir;
				throw new Exception(errorMsg);
			}

			Files.writeFile(autoJsp, dest);
			if (verbose)
				System.out.println("jspf written to " + dest.toString());
		} catch (Exception e) {
			prtln("writeJspFragment couldn't write to disk: " + e);
			return false;
		}
		return true;
	}


	/**
	 *  Prints string representation of XML element using XMLWriter.
	 *
	 * @param  o  Description of the Parameter
	 */
	private void write(Object o) {
		try {
			writer.write(o);
			prtln("");
		} catch (Exception e) {
			prtln("couldn write");
		}
	}


	/**
	 *  Sets the logging attribute of the AutoForm class
	 *
	 * @param  verbose  The new logging value
	 */
	public static void setLogging(boolean verbose) {
		try {
			/* RendererHelper.setDebug (verbose); */
			org.dlese.dpc.schemedit.autoform.mde.MdeNode.setDebug(verbose);
			org.dlese.dpc.schemedit.autoform.mde.MdeAttribute.setDebug(verbose);
			org.dlese.dpc.schemedit.autoform.mde.MdeSimpleType.setDebug(verbose);
			org.dlese.dpc.schemedit.autoform.mde.MdeComplexType.setDebug(verbose);
			org.dlese.dpc.schemedit.autoform.mde.MdeRepeatingComplexType.setDebug(verbose);
			org.dlese.dpc.schemedit.autoform.mde.MdeRepeatingSimpleType.setDebug(verbose);
			org.dlese.dpc.schemedit.autoform.mde.MdeSequence.setDebug(verbose);

			/* 			org.dlese.dpc.schemedit.autoform.RendererHelper.setDebug(verbose);
			org.dlese.dpc.schemedit.autoform.Renderer.setDebug(verbose);
			org.dlese.dpc.schemedit.autoform.RendererImpl.setDebug(verbose);
			org.dlese.dpc.schemedit.autoform.ViewerRenderer.setDebug(verbose);
			org.dlese.dpc.schemedit.autoform.EditorRenderer.setDebug(verbose);
			org.dlese.dpc.schemedit.autoform.DleseEditorRenderer.setDebug(verbose); */
		} catch (Throwable t) {
			prtln("setLogging ERROR: " + t.getMessage());
		}
	}


	/*  	public static void setVerbose(boolean bool) {
		verbose = bool;
	} */
	/**
	 *  Description of the Method
	 *
	 * @param  s  Description of the Parameter
	 */
	private static void prtln(String s) {
		if (debug) {
			// System.out.println(s);
			SchemEditUtils.prtln(s, "");
		}
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  s  NOT YET DOCUMENTED
	 */
	protected static void prtlnErr(String s) {
		System.out.println(s);
	}
	
	public static void setVerbose (boolean verbosity) {
		verbose = verbosity;
	}
}

