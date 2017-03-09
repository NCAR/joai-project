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
package org.dlese.dpc.xml.schema.action;

import java.util.*;

import org.dlese.dpc.schemedit.*;
import org.dlese.dpc.schemedit.config.*;
import org.dlese.dpc.xml.*;
import org.dlese.dpc.xml.schema.*;


/**
 *  Tester for {@link org.dlese.dpc.schemedit.config.FrameworkConfigReader} and
 {@link org.dlese.dpc.schemedit.MetaDataFramework}
 *
 *@author    ostwald
 <p>$Id: GlobalDefReporter.java,v 1.3 2009/03/20 23:34:01 jweather Exp $
 */
public class GlobalDefReporter {
	
	public static final String [] REPORT_FUNCTIONS = 
		{
		"globalElements", 
		"globalAttributes",
		"simpleTypes", 
		"complexTypes", 
// 		"derivedModels", 
		"textOnlyModels", 
		"derivedContentModels",
		};	

		
	public static List getGlobalDefs (String reportFunction, GlobalDefMap globalDefMap) throws Exception {
		
		if (reportFunction.equals ("simpleTypes")) {
			return getSimpleTypes (globalDefMap);
		}
		else if (reportFunction.equals ("complexTypes")) {
			return getComplexTypes (globalDefMap);
		}
		else if (reportFunction.equals ("derivedModels")) {
			return getDerivedModels (globalDefMap);
		}
 		else if (reportFunction.equals ("globalElements")) {
			return getGlobalElements (globalDefMap);
		}
		else if (reportFunction.equals ("globalAttributes")) {
			return getGlobalAttributes (globalDefMap);
		}
		else if (reportFunction.equals ("derivedContentModels")) {
			return getDerivedContentModels (globalDefMap);
		}
		else if (reportFunction.equals ("textOnlyModels")) {
			return getDerivedTextOnlyModels (globalDefMap);
		}		
		else {
			throw new Exception ("unrecognized reportFunction: " + reportFunction);
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
	
	public static List getSimpleTypes (GlobalDefMap globalDefMap) {
		List ret = new ArrayList ();
		Iterator i = globalDefMap.getValues().iterator();
		while (i.hasNext()) {
			GlobalDef def = (GlobalDef)i.next();
			if (def.isSimpleType())
				ret.add (def);
		}
		return ret;
	}
	
	public static List getComplexTypes (GlobalDefMap globalDefMap) {
		List ret = new ArrayList ();
		Iterator i = globalDefMap.getValues().iterator();
		while (i.hasNext()) {
			GlobalDef def = (GlobalDef)i.next();
			if (def.isComplexType())
				ret.add (def);
		}
		return ret;
	}
	
	public static List getDerivedModels (GlobalDefMap globalDefMap) {
		List ret = new ArrayList ();
		Iterator i = globalDefMap.getValues().iterator();
		while (i.hasNext()) {
			GlobalDef def = (GlobalDef)i.next();
			if (def.isComplexType() && ((ComplexType)def).isDerivedType())
				ret.add (def);
		}
		return ret;
	}

	public static List getDerivedContentModels (GlobalDefMap globalDefMap) {
		List ret = new ArrayList ();
		Iterator i = globalDefMap.getValues().iterator();
		while (i.hasNext()) {
			GlobalDef def = (GlobalDef)i.next();
			if (def.isComplexType() && ((ComplexType)def).isDerivedContentModel())
				ret.add (def);
		}
		return ret;
	}

	public static List getDerivedTextOnlyModels (GlobalDefMap globalDefMap) {
		List ret = new ArrayList ();
		Iterator i = globalDefMap.getValues().iterator();
		while (i.hasNext()) {
			GlobalDef def = (GlobalDef)i.next();
			if (def.isComplexType() && ((ComplexType)def).isDerivedTextOnlyModel())
				ret.add (def);
		}
		return ret;
	}
	
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

	
	/**
	 *  Description of the Method
	 *
	 *@param  s  Description of the Parameter
	 */
	public static void prtln(String s) {
		System.out.println(s);
	}
}

