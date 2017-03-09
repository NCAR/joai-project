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
import org.dom4j.*;
import org.dom4j.tree.DefaultElement;
import org.dom4j.tree.AbstractElement;
import org.dom4j.io.SAXReader;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.dom4j.tree.FlyweightText;

/**
 *  Class to render an element label for metadata editors and viewers. Label components, such as controls and
links, are added to SimpleTypeLabel instance, and then these components are assembled into jsp by the getElement method.
 *
 *@author    ostwald<p>
 */
public class SimpleTypeLabel extends Label {
	/**
	 *  Description of the Field
	 */
	protected static boolean debug = false;
	public Element control = null;
	public Element editMeTag = null;
	private boolean requiredField = false;;
	private boolean repeatingField = false;

	/**
	* build up the label Element
	*/
	public Element getElement () {
		
		Element labelElement = df.createElement ("div");
		
		if (fieldType != null)
			labelElement.addAttribute ("class", fieldType);
		
		if (this.isRequired())
			labelClass = labelClass + " required";
		
		Element textHolder = df.createElement("div");
		if (editMeTag != null) {
			textHolder.add (editMeTag);
			textHolder.addAttribute("class", labelClass);
			labelElement.add (textHolder);
			
			editMeTag.addAttribute ("label", labelText);
		}
		else {
			textHolder.setText (labelText);
			labelElement.addAttribute("class", labelClass);
			labelElement.add (textHolder);
		
			if (control != null) {
				labelElement.add (control.createCopy());
			}
		}
		
		if (bestPractices != null) {
			Element wrapper = df.createElement ("div");
			
			wrapper.add (labelElement);
			wrapper.add(bestPractices.createCopy());
			labelElement = wrapper;
		}
		
		addDebugInfo (labelElement);

		return labelElement;
	}
	
	public void setRepeating () {
		this.repeatingField = true;
	}
	
	public boolean isRepeating () {
		return repeatingField;
	}
	
	public void setRequired () {
		this.requiredField = true;
	}
	
	public void setOptional () {
		this.requiredField = false;
	}
	
	public boolean isRequired () {
		return requiredField;
	}
	
	public void report () {
		prtln ("\t labelText: " + labelText);
		prtln ("\t fieldType: " + fieldType);
		prtln ("\t labelClass: " + labelClass);
		prtln ("\t isRequired: " + isRequired());
	}
	
	public static void main(String[] args) {
		Label stl = new SimpleTypeLabel();
		stl.setText("hello world");
		
		Element bp = stl.df.createElement("span");
		bp.setText("best practices");
		stl.bestPractices = bp;
		
		pp (stl.getElement());
		stl.setLabelClass("label-class");
		pp (stl.getElement());
	}
	
	private static void prtln(String s) {
		if (debug) {
			System.out.println(s);
		}
	}
	
}
			
	
