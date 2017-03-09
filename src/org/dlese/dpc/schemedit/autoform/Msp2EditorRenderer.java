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

import org.dlese.dpc.schemedit.autoform.mde.*;
import org.dlese.dpc.schemedit.standards.StandardsManager;
import org.dlese.dpc.xml.*;
import org.dlese.dpc.xml.schema.*;

import java.util.*;
import java.util.regex.*;

import org.dom4j.Node;
import org.dom4j.Attribute;
import org.dom4j.Element;

/**
 *  Includes msp2-specific kludges, most notably the /record/general/subjects
 *  field, which requires an editing approach OTHER than than implied by the
 *  schema ...<p>
 *
 *  Elements created by the Msp2EditorRenderer starting with "msp2__" (e.g.,
 *  "msp2__subjects") are rendered tag calls (e.g., "msg:subjects") which are
 *  handled by tag files (e.g., "subjects.tag") in the tags/msp2 directory.
 *
 * @author    ostwald
 */
public class Msp2EditorRenderer extends DleseEditorRenderer {
	/**  Description of the Field  */
	private static boolean debug = false;

	String subjectsPath = "/record/general/subjects";
	String scienceSubjectPath = "/record/general/subjects/scienceSubject";
	String languagePath = "/record/general/language";


	/**
	 *  Intercept renderNode calls for certain paths and use jsp tags instead of
	 *  autoform
	 */
	public void renderNode() {
		if (subjectsPath.equals(xpath)) {
			prtln("Msp2EditorRenderer got SUBJECTS node");
			renderSubjectsNode();
		}
		else if (languagePath.equals(xpath)) {
			renderLanguageNode();
		}
		else {
			super.renderNode();
		}
	}


	/**  Custom renderer for the Subjects node using a tag file.  */
	protected void renderSubjectsNode() {
		prtln("renderSubjectsNode()");
		Element subjectTag = df.createElement("msp2__subjects");
		parent.add(subjectTag);
	}


	/**
	 *  Custom renderer for the Subjects node - calls a tag file to render this
	 *  field.
	 */
	private void renderLanguageNode() {
		Element lingoTag = df.createElement("msp2__language");
		parent.add(lingoTag);
	}


	/**
	 *  Sets the debug attribute of the Msp2EditorRenderer class
	 *
	 * @param  bool  The new debug value
	 */
	public static void setDebug(boolean bool) {
		debug = bool;
	}


	/**
	 *  Description of the Method
	 *
	 * @param  s  Description of the Parameter
	 */
	private static void prtln(String s) {
		if (debug) {
			while (s.length() > 0 && s.charAt(0) == '\n') {
				System.out.println("");
				s = s.substring(1);
			}
			System.out.println("Msp2EditorRenderer: " + s);
		}
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  s  NOT YET DOCUMENTED
	 */
	private final void prtlnErr(String s) {
		System.err.println("Msp2EditorRenderer: " + s);
	}

}

