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

import java.io.*;
import java.util.*;
import java.text.*;
import java.net.URI;

import org.dlese.dpc.schemedit.*;
import org.dlese.dpc.schemedit.config.*;
import org.dlese.dpc.xml.*;
import org.dlese.dpc.xml.schema.*;
import org.dlese.dpc.util.*;
import org.dlese.dpc.webapps.tools.GeneralServletTools;
import org.dlese.dpc.repository.*;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import org.dom4j.Attribute;
import org.dom4j.Node;
import org.dom4j.QName;

/**
 *  Tester for {@link org.dlese.dpc.schemedit.config.FrameworkConfigReader} and
 {@link org.dlese.dpc.schemedit.MetaDataFramework}
 *
 *@author    ostwald
 <p>$Id: DefReportTester.java,v 1.3 2009/03/20 23:33:58 jweather Exp $
 */
public class DefReportTester {
	MetaDataFramework framework = null;
	GlobalDefMap globalDefMap = null;

	/**
	 *  Constructor for the DefReportTester object
	 */
	public DefReportTester(String format) throws Exception {
		FrameworkTester ft = new FrameworkTester (format);
		framework = ft.getFramework();
		globalDefMap = framework.getSchemaHelper().getGlobalDefMap();
			
	}
	
	public static void main (String [] args) throws Exception {
		String format = "status_report_simple";
		if (args.length > 0)
			format = args[0];
		
		String reportFunction = "simpleType";
		if (args.length > 1)
			reportFunction = args[1];
		
		DefReportTester drt = new DefReportTester (format);
		GlobalDefMap globalDefMap = drt.globalDefMap;
		
		List defs = new ArrayList ();
		
		prtln ("\n\nDefReportTester:\n\t format: " + format + "\n\t reportFunction: " + reportFunction + "\n");
		
		if (reportFunction.equals ("simpleType")) {
			defs = getSimpleTypes (globalDefMap);
		}
		else if (reportFunction.equals ("complexType")) {
			defs = getComplexTypes (globalDefMap);
		}
		else if (reportFunction.equals ("derivedModel")) {
			defs = getDerivedModels (globalDefMap);
		}
		else if (reportFunction.equals ("globalElement")) {
			defs =  getGlobalElements (globalDefMap);
		}
		else if (reportFunction.equals ("globalAttribute")) {
			defs = getGlobalAttributes (globalDefMap);
		}
		else {
			prtln ("unrecognized reportFunction: " + reportFunction);
		}
		
		if (defs != null) {
			prtln ("reportFunction " + reportFunction + " (" + defs.size() + " found)");
			for (Iterator i=defs.iterator();i.hasNext();) {
				showGlobalDef ((GlobalDef)i.next());
			}
		}	
	}
	
	static List getGlobalElements (GlobalDefMap globalDefMap) {
		List ret = new ArrayList ();
		Iterator i = globalDefMap.getValues().iterator();
		while (i.hasNext()) {
			GlobalDef def = (GlobalDef)i.next();
			if (def.isGlobalElement())
				ret.add (def);
		}
		return ret;
	}

	static List getGlobalAttributes (GlobalDefMap globalDefMap) {
		List ret = new ArrayList ();
		Iterator i = globalDefMap.getValues().iterator();
		while (i.hasNext()) {
			GlobalDef def = (GlobalDef)i.next();
			if (def.isGlobalAttribute())
				ret.add (def);
		}
		return ret;
	}
	
	static List getSimpleTypes (GlobalDefMap globalDefMap) {
		List ret = new ArrayList ();
		Iterator i = globalDefMap.getValues().iterator();
		while (i.hasNext()) {
			GlobalDef def = (GlobalDef)i.next();
			if (def.isSimpleType())
				ret.add (def);
		}
		return ret;
	}
	
	static List getComplexTypes (GlobalDefMap globalDefMap) {
		List ret = new ArrayList ();
		Iterator i = globalDefMap.getValues().iterator();
		while (i.hasNext()) {
			GlobalDef def = (GlobalDef)i.next();
			if (def.isComplexType())
				ret.add (def);
		}
		return ret;
	}
	
	static List getDerivedModels (GlobalDefMap globalDefMap) {
		List ret = new ArrayList ();
		Iterator i = globalDefMap.getValues().iterator();
		while (i.hasNext()) {
			GlobalDef def = (GlobalDef)i.next();
			if (def.isComplexType() && ((ComplexType)def).isDerivedType())
				ret.add (def);
		}
		return ret;
	}

	
/* 	static List getSimpleTypes (GlobalDefMap globalDefMap) {
		List ret = new ArrayList ();
		Iterator i = globalDefMap.getKeys().iterator();
		while (i.hasNext()) {
			String key = (String)i.next();
			GlobalDef def = (GlobalDef)globalDefMap.getValue (key);
			if (def.isSimpleType())
				ret.add (def);
		}
		return ret;
	}
	
	static List getComplexTypes (GlobalDefMap globalDefMap) {
		List ret = new ArrayList ();
		Iterator i = globalDefMap.getKeys().iterator();
		while (i.hasNext()) {
			String key = (String)i.next();
			GlobalDef def = (GlobalDef)globalDefMap.getValue (key);
			if (def.isComplexType())
				ret.add (def);
		}
		return ret;
	}
	
	static List getDerivedModels (GlobalDefMap globalDefMap) {
		List ret = new ArrayList ();
		Iterator i = globalDefMap.getKeys().iterator();
		while (i.hasNext()) {
			String key = (String)i.next();
			GlobalDef def = (GlobalDef)globalDefMap.getValue (key);
			if (def.isComplexType() && ((ComplexType)def).isDerivedType())
				ret.add (def);
		}
		return ret;
	}
 */	
	static void showGlobalDef (GlobalDef def) {
		String s = "";
		String [] pathSplits = def.getClass().getName().split("\\.");
		String className = def.getClass().getName();
		if (pathSplits.length > 0)
			className = pathSplits[pathSplits.length -1];
		s += "\nname: " + def.getName() + "  (" + className + ")" ;
		s += "\n\tnamespace: " + def.getNamespace().getURI();
		s += "\n\tlocation: " + def.getLocation();
		prtln (s);
	}
		

				

		
		
	static void pp (Node node) {
		prtln (Dom4jUtils.prettyPrint(node));
	}
	
	/**
	 *  Description of the Method
	 *
	 *@param  s  Description of the Parameter
	 */
	public static void prtln(String s) {
		System.out.println(s);
	}
}

