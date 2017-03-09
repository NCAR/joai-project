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
package org.dlese.dpc.schemedit.test;

import org.dlese.dpc.schemedit.*;
import org.dlese.dpc.schemedit.autoform.*;
import java.util.*;
import java.util.regex.*;
import java.net.*;
import java.io.*;
import org.dlese.dpc.xml.*;
import org.dlese.dpc.xml.schema.*;
import org.dlese.dpc.xml.schema.compositor.Compositor;
import org.dlese.dpc.util.Files;
import org.dom4j.*;


/**
 *
 *@author     ostwald
 */
public class EditorViewRenderTester extends RenderTester {

	private static boolean debug = true;
	String xmlFormat;
	
	EditorViewRenderTester (String xmlFormat) throws Exception {
		super(xmlFormat);
	}
	
	
	public static void main (String [] args) throws Exception {

		String xmlFormat = "mast_demo";
		if (args.length > 0)
			xmlFormat = args[0];
		
		String renderPath = null;
		renderPath = "/record/general";
		// renderPath = "/annotationRecord/moreInfo";
		if (args.length > 1)
			renderPath = args[1];
			
		boolean showJsp = false;
		boolean dumpJsp = true;
		if (args.length > 2)
			dumpJsp = true;
		
		EditorViewRenderTester rt = new EditorViewRenderTester(xmlFormat);
		if (rt.framework == null) {
			prtln ("FRAMEWORK IS NULL");
			return;
		}
		EditorViewRecord editorViewRecord = null;
		try {
			editorViewRecord = new EditorViewRecord(rt.framework);
		} catch (Exception e) {
			e.printStackTrace();
			prtln(e.getMessage());
			return;
		}
		
		// rt.sh.showSchemaNodeMap();
		// rt.sh.showGlobalDefs();
		
		if (renderPath == null)
			renderPath = "/" + rt.framework.getRootElementName();
		prtln ("renderPath: " + renderPath);
		Element jsp = editorViewRecord.render (renderPath);
		if (showJsp)
			prtln (AutoForm.elementToJsp (jsp));
		if (dumpJsp) {
			String jspStr = AutoForm.elementToJsp (jsp);
			File out = new File ("C:/tmp/EditorViewRenderTester.jsp");
			Files.writeFile(jspStr, out);
			prtln ("\njsp dumped to " + out);
		}
	}

	private static void  pp (Node node) {
		prtln (Dom4jUtils.prettyPrint(node));
	}
	
	
	/**
	 *  Description of the Method
	 *
	 *@param  s  Description of the Parameter
	 */
	private static void prtln(String s) {
		if (debug) {
			// System.out.println("EditorViewRenderTester: " + s);
			System.out.println(s);
		}
	}

	
}

