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

/**
 *  Displays an XML metadata record using the ViewerRenderer.
 *  DcsViewRecord provides the view of the entire record for the DCS
 *  DcsViewRecord page
 *
 *@author    ostwald
 *
 */
public class DcsViewRecord extends AutoForm {
	private static boolean debug = true;

	String libDir = "../../../schemedit-project/web/lib";

	/**
	 *  Constructor for the DcsViewRecord object
	 *
	 *@param  framework  Description of the Parameter
	 */
	public DcsViewRecord(MetaDataFramework framework) {
		super(framework);
		formBeanName = "viewForm";
	}


	/**
	 *  Constructor for the Stand-alone DcsViewRecord object, meaning it is created
	 *  from command line rather than via schemedit.
	 *
	 *@param  xmlFormat                  Description of the Parameter
	 *@exception  SchemaHelperException  Description of the Exception
	 */
	public DcsViewRecord(String xmlFormat)
		throws Exception, SchemaHelperException {
		super(xmlFormat);
		formBeanName = "viewForm";
	}

	/**
	 *  The main program for the DcsViewRecord class. The first argument is
	 *  command, the second is arg (if nec);
	 *
	 *@param  args  The command line arguments
	 */
	public static void main(String[] args) throws Exception {
		TesterUtils.setSystemProps();
		setVerbose(true);
		prtln("\n==========================================================\nDCS View Record:");
		String xmlFormat = "mast_demo";
		String command = "batchRender";
		String xpath = "/news_oppsRecord";
		
		try {
			xmlFormat = args[0];
			command = args[1];
			xpath = args[2];
		} catch (Exception e) {}
		
		prtln ("\t xmlFormat = " + xmlFormat);
		prtln ("\t command = " + command);
		prtln ("\t xpath = " + xpath);
		
		DcsViewRecord fullView = null;
		try {
			fullView = new DcsViewRecord(xmlFormat);
		} catch (Exception e) {
			prtln(e.getMessage());
			return;
		}
		fullView.setLogging(false);
		AutoForm.setVerbose(true);
		
		if (command.equals("batchRender")) {
			prtln("batchRenderAndWrite");
			fullView.batchRenderAndWrite();
			return;
		}

		if (command.equals("renderAndWrite")) {
			prtln("renderAndWrite");
			fullView.renderAndWrite(xpath);
		}
	}

	public void renderAndWrite() throws Exception {
		renderAndWrite ("/" + this.framework.getRootElementName());
	}
	
	private void renderMaster () throws Exception {
		Element base = df.createElement("div");
		/* base.addAttribute ("class", "level--1"); */
		List elements = instanceDocument.getRootElement().elements();
		// prtln("batchRenderAndWrite found " + elements.size() + " elements");
		for (Iterator i = elements.iterator(); i.hasNext(); ) {
			Element child = (Element) i.next();
			String xpath = child.getPath();
			String pageName = XPathUtils.getNodeName(xpath);
			Element include = base.addElement("jsp__include");
			include.addAttribute("page", getMasterComponentPath (pageName));
		}
		
		File dest = getJspDest (null);
		if (!writeJsp(base, dest, this.getMasterJspHeader())) {
			throw new Exception ("renderMaster(): failed to write jsp to " + dest);
		}
		
	}
		
	public void batchRenderAndWrite() throws Exception {
		renderMaster ();
		super.batchRenderAndWrite();
	}

	
	protected String getRendererClassName () {
		return "ViewerRenderer";
	}
	
	protected File getJspDest (String pageName) {
		String fileName = framework.getXmlFormat() + "_record.jsp";
		String viewingDirPath = framework.getDocRoot() + "/browse/viewing";
		return new File(viewingDirPath, fileName);
	}
	
	protected String getMasterJspHeader () {
		String header = "<%@ include file=\"/lib/includes.jspf\" %>\n\n";
/* 		header += "<%@ page import=\"org.dlese.dpc.schemedit.display.CollapseBean\" %>\n\n";
		header +=
				"<bean:define id=\"collapseBean\" name=\"" + formBeanName + "\" property=\"collapseBean\" type=\"CollapseBean\" />\n\n"; */
		return header;
	}
	
	protected File getBatchJspDest (String pageName) throws Exception {
		String fileName = pageName + ".jsp";
		String viewingDirPath = framework.getDocRoot() + "/browse/viewing/" + framework.getXmlFormat();
		return new File(viewingDirPath, Files.encode(fileName));
	}
	
	/*
	** Path from Master jsp file to component jsp pages
	*/
	protected String getMasterComponentPath (String pageName) {
		return this.framework.getXmlFormat() + "/" + pageName + ".jsp";
	}
	
	public static void setLogging (boolean verbose) {
		try {
			/* RendererHelper.setDebug (verbose); */
			org.dlese.dpc.schemedit.autoform.mde.MdeNode.setDebug(verbose);
			org.dlese.dpc.schemedit.autoform.mde.MdeAttribute.setDebug(verbose);
			org.dlese.dpc.schemedit.autoform.mde.MdeSimpleType.setDebug(verbose);
			org.dlese.dpc.schemedit.autoform.mde.MdeComplexType.setDebug(verbose);
			org.dlese.dpc.schemedit.autoform.mde.MdeRepeatingComplexType.setDebug(verbose);
			org.dlese.dpc.schemedit.autoform.mde.MdeRepeatingSimpleType.setDebug(verbose);
			org.dlese.dpc.schemedit.autoform.mde.MdeSequence.setDebug(verbose);
			org.dlese.dpc.schemedit.autoform.mde.MdeDerivedTextOnlyModel.setDebug(verbose);
			
			/* org.dlese.dpc.schemedit.autoform.RendererHelper.setDebug(verbose); */
			org.dlese.dpc.schemedit.autoform.Renderer.setDebug(verbose);
			org.dlese.dpc.schemedit.autoform.RendererImpl.setDebug(verbose);
			org.dlese.dpc.schemedit.autoform.ViewerRenderer.setDebug(verbose);
			org.dlese.dpc.schemedit.autoform.EditorRenderer.setDebug(verbose);
			org.dlese.dpc.schemedit.autoform.DleseEditorRenderer.setDebug(verbose);
		} catch (Throwable t) {
			prtln ("setLogging ERROR: " + t.getMessage());
		}
	}

	
	/**
	 *  Description of the Method
	 *
	 *@param  s  Description of the Parameter
	 */
	private static void prtln(String s) {
		if (debug) {
			System.out.println(s);
		}
	}
}

