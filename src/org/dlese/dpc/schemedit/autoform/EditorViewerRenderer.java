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
import org.dom4j.tree.FlyweightText;

/**
 *  Renders JSP for viewing (rather than editing) XML documents in the MetaData
 *  Editor. <p>
 *
 *  Unlike {@link org.dlese.dpc.schemedit.autoform.ViewerRenderer}, this class
 *  presents best Practice information (when available) for all elements.
 *
 *@author    ostwald
 */
public class EditorViewerRenderer extends ViewerRenderer {
	/**
	 *  Description of the Field
	 */
	private static boolean debug = false;


	/**
	 *  Gets the editMeTag attribute of the EditorViewerRenderer object
	 *
	 *@param  xpath        Description of the Parameter
	 *@param  siblingPath  Description of the Parameter
	 *@param  indexId      Description of the Parameter
	 *@return              The editMeTag value
	 */
	protected Element getEditMeTag(String xpath, String siblingPath, String indexId) {

		String tagPath = (siblingPath != null ? siblingPath : xpath);
		Element editMeTag = df.createElement("st__editorViewEditMeLabel")
				.addAttribute("xpath", tagPath);

		// the destination page in the editor depends on the baseRenderLevel attribute of this framework, which
		// specifies whether the editor is split up into several pages (e.g., adn) or rendered
		// as a single page (e.g., news_opps).
		String page = "";
		try {
			page = xpath.split("/")[rhelper.getFramework().getBaseRenderLevel() - 1];
		} catch (Throwable e) {
			prtln("getEditMeTag couldn't obtain page attribute");
		}
		prtln("  ... page: " + page);
		editMeTag.addAttribute("page", page);

		return editMeTag;
	}


	/**
	 *  Sets the debug attribute of the EditorViewerRenderer class
	 *
	 *@param  bool  The new debug value
	 */
	public static void setDebug(boolean bool) {
		debug = bool;
	}


	/**
	 *  Description of the Method
	 *
	 *@param  s  Description of the Parameter
	 */
	private static void prtln(String s) {
		if (debug) {
			System.out.println("EditorViewerRenderer: " + s);
		}
	}

}

