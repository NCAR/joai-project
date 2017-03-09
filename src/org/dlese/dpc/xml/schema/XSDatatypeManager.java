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
package org.dlese.dpc.xml.schema;

import java.io.*;
import java.util.*;
import java.text.ParseException;
import org.dlese.dpc.util.MetadataUtils;

import org.dom4j.Element;

import com.sun.msv.datatype.xsd.*;
import org.relaxng.datatype.*;

/**
 *  Provides Map-like interface to XSdatatypes - including built-ins and those
 *  datatypes defined by a Schema - used to validate element values within a
 schema-based document. XSdatatypes validate a
 *  value against a defined datatype, whether it is built-in or derived.
 *  Built-in data types are supplied by Sun's XSDatatype library, data types
 *  specified by a particular schema are derived by extending the built-in types
 *  (see <i>deriveType()</i>).
 *
 *@author    ostwald
 *
 */
public class XSDatatypeManager {

	private static boolean debug = false;
	private HashMap derivedTypes = new HashMap();
	private GlobalDefMap globalDefMap = null;


	/**
	 *  Constructor for the XSDatatypeManager object
	 */
	public XSDatatypeManager() { }


	/**
	 *  Constructor for the XSDatatypeManager object
	 *
	 *@param  globalDefMap  Description of the Parameter
	 */
	public XSDatatypeManager(GlobalDefMap globalDefMap) {
		this.globalDefMap = globalDefMap;
		deriveSimpleTypes();

	}

	public Map getDerivedTypes () {
		return derivedTypes;
	}

	/**
	 *  Adds a derived XSDatatype to the map of derived types
	 *
	 *@param  dt  The XSDatatype to be added
	 */
	public void addDerivedType(XSDatatype dt) {
		derivedTypes.put(dt.getName(), dt);
	}

	public String getSchemaNSPrefix () {
		try {
			return this.globalDefMap.getNamespaces().getSchemaNamespace().getPrefix();
		} catch (Throwable t) {
			prtln ("could not obtain schema namespace prefix: " + t.getMessage());
		}
		return null;
	}

	/**
	 *  Gets a XSDatatype class used to validate a value against a datatype
	 *  declared or defined in an XML Schema. XSDatatypes are found either in the
	 *  derivedTypes map or in the DatatypeFactory (which returns XSDatatyps for
	 *  built-in types)
	 *
	 *@param  typeName  XSDatatype name
	 *@return           The XSDatatype object if found, or null
	 */
	public XSDatatype getTypeByName(String typeName) {
		// prtln ("*** getTypeByName() with " + typeName);
		XSDatatype dt = null;
		try {
			// first, strip prefix from typeName and look for a built-in type
			String localizedName = NamespaceRegistry.stripNamespacePrefix(typeName);
			dt = DatatypeFactory.getTypeByName(localizedName);
		} catch (DatatypeException e) {
			// if a built-in type was not found, now look for a derived type (using prefix)
			dt = (XSDatatype) derivedTypes.get(typeName);
		}
		// if (dt == null) prtln ("getTypeByName() unable to find XSDatatype for " + typeName);
		return dt;
	}
	 
	 public XSDatatype getTypeByNameOLD(String typeName) {
		prtln ("*** getTypeByName() with " + typeName);
		XSDatatype dt = null;
		try {
			// need to strip "xsd:" from front of built-in type typeNames
			String schemaNSPrefix = this.getSchemaNSPrefix();
			String localizedName = typeName;
			if (schemaNSPrefix != null)
				localizedName = typeName.substring((schemaNSPrefix+":").length());
			dt = DatatypeFactory.getTypeByName(localizedName);
		} catch (DatatypeException e) {
			// prtln ("typeGetter() caught exception: " + e);
			dt = (XSDatatype) derivedTypes.get(typeName);
		}
		// if (dt == null) prtln ("getTypeByName() unable to find XSDatatype for " + typeName);
		return dt;
	}


	/**
	 *  Checks a value against it's datatype. If a validator (XSDatatype) can't be
	 *  found for the given typeName, a message is printed and false is returned.
	 *  This is a pretty weak way of dealing with this problem, since in other
	 *  cases, an error is thrown to signify an invalid value.<p>
	 *
	 *  date values are intercepted and handled by {@link
	 *  org.dlese.dpc.util.MetadataUtils#parseUnionDateType(String)}
	 *
	 *@param  typeName       Description of the Parameter
	 *@param  value          Description of the Parameter
	 *@return                Description of the Return Value
	 *@exception  Exception  Description of the Exception
	 */
	public boolean checkValid(String typeName, String value)
		throws Exception {
		// prtln ("checkValid(" + typeName + ", " + value + ")");
		
		//Handle types implemented by custom validators
		if (typeName.equals("union.dateType")) {
			return (DateValidator.checkValidUnionDate(value));
		}
		
		String schemaNSPrefix = this.getSchemaNSPrefix();
		String dateType = NamespaceRegistry.makeQualifiedName(schemaNSPrefix, "date");
		if (typeName.equals(dateType)) {
			return (DateValidator.checkValidXsdDate(value));
		}

		if (typeName.equals("BCType")) {
			// throw new Exception ("BCType not currently validated");
			return (DateValidator.checkValidBCType(value, this));
		}
		
		XSDatatype dt = getTypeByName(typeName);
		String errorMsg = "";
		if (dt == null) {
			// prtln("checkValid(): XSDatatype not found for " + typeName);
			return false;
		}
		try {
			dt.checkValid(value, null);
		} catch (DatatypeException de) {
			if (de.getMessage() == null) {
				// errorMsg = "invalid: diagnosis not supported";
				errorMsg = "invalid value";
			}
			else {
				errorMsg = "invalid: " + de.getMessage();
			}
			throw new Exception(errorMsg);
		}
		// prtln ("  .... valid");
		return true;
	}

	
	/**
	 *  validates a String value against an XSDatatype. If the value is not valid
	 *  an Exception is thrown describing the error.
	 *
	 *@param  dt             Description of the Parameter
	 *@param  v              Description of the Parameter
	 *@return                Description of the Return Value
	 *@exception  Exception  Description of the Exception
	 */
	public boolean checkValid(XSDatatype dt, String v)
		throws Exception {
		String errorMsg = "";
		try {
			dt.checkValid(v, null);
		} catch (DatatypeException de) {
			if (de.getMessage() == null) {
				errorMsg = "invalid: diagnosis not supported";
			}
			else {
				errorMsg = "invalid: " + de.getMessage();
			}
			throw new Exception(errorMsg);
		}
		return true;
	}


	/**
	 *  For each simpleDataType in the GlobalDefMap, derive a XSDatatype. NOTE: the
	 *  union data types must be derived after all the others, since the union's
	 *  memberTypes must exist!
	 */
	private void deriveSimpleTypes() {
		List simpleTypeDefs = globalDefMap.getSimpleTypes();
		List unionNodes = new ArrayList();
		for (Iterator i = simpleTypeDefs.iterator(); i.hasNext(); ) {
			SimpleType def = (SimpleType) i.next();
			if (def.isUnion()) {
				unionNodes.add(def);
			}
			else {
				XSDatatype dt = deriveType(def);
				if (dt != null) {
					addDerivedType(dt);
				}
			}
		}
		for (Iterator i = unionNodes.iterator(); i.hasNext(); ) {
			SimpleType unionNode = (SimpleType) i.next();
			XSDatatype dt = deriveType(unionNode);
			if (dt != null) {
				addDerivedType(dt);
			}
		}
	}

/* 	private void deriveSimpleTypesOld() {
		List keys = globalDefMap.getKeys(GlobalDef.SIMPLE_TYPE);
		List unionNodes = new ArrayList();
		for (Iterator i = keys.iterator(); i.hasNext(); ) {
			String key = (String) i.next();
			SimpleType def = (SimpleType) globalDefMap.getValue(key);
			if (def.isUnion()) {
				unionNodes.add(def);
			}
			else {
				XSDatatype dt = deriveType(def);
				if (dt != null) {
					addDerivedType(dt);
				}
			}
		}
		for (Iterator i = unionNodes.iterator(); i.hasNext(); ) {
			SimpleType unionNode = (SimpleType) i.next();
			XSDatatype dt = deriveType(unionNode);
			if (dt != null) {
				addDerivedType(dt);
			}
		}
	}
 */
	/**
	 *  Derive a XSDatatype for the given SimpleType (a GlobalDef) by calling
	 *  either
	 *
	 *@param  def  A wrapper for a Simple Data Type definition
	 *@return      Description of the Return Value
	 */
	private XSDatatype deriveType(SimpleType def) {
		prtln ("deriveType() def.getName(): " + def.getQualifiedName());
		XSDatatype dt = null;
		try {
			if (def.isUnion()) {
				dt = deriveByUnion(def);
			}
			else {
				dt = deriveByRestriction(def);
			}
		} catch (Exception e) {
			// we know that BCType cannot be derived because of the date problems in the Schema Spec,
			// so don't bother printing message for this type.
			if (!def.getName().equals("BCType")) {
				prtln("Couldnt derive type for " + def.getQualifiedName() + ": " + e);
			}
		}
		return dt;
	}


	/**
	 *  Relies on the SimpleType def to derive a XSDatatype
	 *
	 *@param  def            Description of the Parameter
	 *@return                Description of the Return Value
	 *@exception  Exception  Description of the Exception
	 */
	private XSDatatype deriveByRestriction(SimpleType def)
		throws Exception {

		// prtln ("deriveByRestriction with " + def.getElement().asXML());
		Element restriction = def.getFirstChild();
		String baseName = getBaseRestrictionName(restriction);
		// prtln (" ... baseName: " + baseName);
		if (baseName == null) {
			prtln("didn't get getBaseRestrictionName!\n\t" + def.toString());
			return null;
		}
		XSDatatype baseType = DatatypeFactory.getTypeByName(baseName);
		if (baseType == null)
			throw new Exception ("deriveByRestriction error: base XSDatatype not found for " + baseName);
		TypeIncubator incubator = new TypeIncubator(baseType);
		// now add facets to incubator
		for (Iterator i = restriction.elementIterator(); i.hasNext(); ) {
			Element child = (Element) i.next();
			String facetName = child.getName();
			String facetValue = child.valueOf("@value");
			try {
				incubator.addFacet(facetName, facetValue, true, null);
			} catch (Exception e) {
				prtln("couldnt add facet: " + facetName + ", " + facetValue + "\n" + e);
			}
		}
		// XSDatatype derived = incubator.derive("", def.getName());
		XSDatatype derived = incubator.derive (def.getNamespace().getURI(), def.getName());
		return derived;
	}


	/**
	 *  Description of the Method
	 *
	 *@param  def            Description of the Parameter
	 *@return                Description of the Return Value
	 *@exception  Exception  Description of the Exception
	 */
	public XSDatatype deriveByUnion(SimpleType def)
		throws Exception {
		
		String[] memberTypeNames = def.getUnionMemberTypeNames();
		if (memberTypeNames == null) {
			throw new Exception("deriveByUnion error: no member types found");
		}
		XSDatatype [] memberTypes = new XSDatatype [memberTypeNames.length];
		for (int i = 0; i < memberTypeNames.length; i++) {
			String memberTypeName = NamespaceRegistry.stripNamespacePrefix(memberTypeNames[i]);
			XSDatatype dt = getTypeByName(memberTypeName);
			if (dt == null) {
				throw new Exception("XSDatatype not found for " + memberTypeName);
			}
			memberTypes[i] = dt;
		}
		return DatatypeFactory.deriveByUnion("", def.getName(), memberTypes);
	}


	/**
	*  Given a restriction element, return the value of the <i>base</i> attribute. 
		Currently requires
	 *  that a "xsd:" prefix is present in the base attribute, and remove the prefix before returning remainder
	 *  of base attribute. 
	 
	 @see #deriveByRestriction(SimpleType)
	 *
	 *@param  e  Description of the Parameter
	 *@return    The baseName value
	 */
	private String getBaseRestrictionName(Element e) {
		String bn = e.valueOf("@base");
		String prefix = e.getNamespacePrefix()+":";
		if (!bn.startsWith(prefix)) {
			// prtln ("getBaseRestrictionName report");
			// prtln ("\tnamespaceURI: " + e.getNamespaceURI());
			
			// return null;
			return bn;
		}
		else {
			return bn.substring(prefix.length());
		}
	}


	/**
	 *  The main program for the XSDatatypeManager class
	 *
	 *@param  args           The command line arguments
	 *@exception  Exception  Description of the Exception
	 */
	public static void main(String[] args)
		throws Exception {
		String mode = "value";
		XSDatatypeManager p = new XSDatatypeManager();
		// String s = "ice be hoopin is a quote by george girven";

		/*
		 *  for (int i=0;i<args.length;i++)
		 *  prtln (i + ": " + args[i]);
		 */
		if (mode.equals("type")) {
			// code for testing getTypeByName
			String typeName = "";
			if (args.length > 0) {
				typeName = args[0];
			}
			else {
				typeName = "gYear";
			}
			XSDatatype dt = p.getTypeByName(typeName);
			if (dt == null) {
				prtln("no type found for " + typeName);
			}
			else {
				prtln("the datatype name is: " + dt.getName());
			}
		}
	}


	/**
	 *  Description of the Method
	 */
	public void destroy() {
		derivedTypes.clear();
	}


	/**
	 *  Description of the Method
	 *
	 *@param  s  Description of the Parameter
	 */
	private static void prtln(String s) {
		if (debug) {
			System.out.println("XSDatatypeManager: " + s);
		}
	}
}

