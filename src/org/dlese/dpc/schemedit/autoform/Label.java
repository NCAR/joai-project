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
import org.dom4j.*;


/**
 *  Basic Class to render an element label for metadata editors and viewers.
 *
 *@author    ostwald<p>
 *
 */
public class Label {
	/**
	 *  Description of the Field
	 */
	protected static boolean debug = true;
	DocumentFactory df = DocumentFactory.getInstance();
	String labelText;
	String labelClass = "field-label";
	String fieldType = null;
	String debugInfo = null;
	public Element bestPractices = null;
	int level = -1;
	boolean isRequired = false;

	public Label() {
	}


	/**
	 *  Constructor for the Label object
	 *
	 *@param  level  Description of the Parameter
	 */
	public Label(int level) {
		this.level = level;
	}


	/**
	 *  Sets the text attribute of the Label object
	 *
	 *@param  text  The new text value
	 */
	public void setText(String text) {
		labelText = text;
	}

	public String getText () {
		return labelText;
	}
	
	public void setFieldType (String type) {
		this.fieldType = type;
	}
	
	public String getFieldType () {
		return fieldType;
	}
	
	public void setDebugInfo (String info) {
		debugInfo = info;
	}

	/**
	 *  Sets the labelClass attribute of the Label object
	 *
	 *@param  klass  The new labelClass value
	 */
	public void setLabelClass(String klass) {
		labelClass = klass;
	}


	/**
	 *  The main program for the Label class
	 *
	 *@param  args  The command line arguments
	 */
	public static void main(String[] args) {
		Label ctl = new Label();
		ctl.setText("hello world");
		pp (ctl.getElement());
		ctl.setLabelClass("label-class");
		pp (ctl.getElement());
	}


	/**
	 *  build up the label Element
	 *
	 *@return    The element value
	 */
	public Element getElement() {

		Element labelElement = df.createElement("div");
		labelElement.addAttribute("debug-info", "Label - labelElement");
		
		Element textHolder = labelElement.addElement("div");
		textHolder.setText(labelText);
		
		if (fieldType != null)
			labelElement.addAttribute ("class", fieldType);

		if (labelClass != null)
			textHolder.addAttribute ("class", labelClass);
		
		if (bestPractices != null) {
			labelElement.add(bestPractices);
		}
		
		addDebugInfo (labelElement);
		
		return labelElement;
	}

	protected void addDebugInfo (Element e) {
		if (debugInfo != null) {
			Element info = e.addElement("div")
				.addAttribute("class", "label-debug-info");
			info.addText(debugInfo);
		}
	}

	/**
	 *  Description of the Method
	 */
	public static void pp (Node node) {
		prtln (Dom4jUtils.prettyPrint(node));
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


