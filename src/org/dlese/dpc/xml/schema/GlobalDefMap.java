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

import java.util.*;
import java.io.*;
import org.dom4j.Namespace;

/**
 *  Stores GlobalDef instances, which are wrappers for
 *  important elements of an XML Schema, including <ul>
 <li>Simple and ComplexType definitions,
 <li>Goup definitions, and
<li>Global Element and Global Attribute declarations</ul><p>
The definitions are stored in a two-level mapping. The first level is by namespaceUri, so there is a map for each namespace.
The individual namespace maps store all the GlobalDefs for that namespace. To access a particular globalDefinition, the 
<i>name</i> of the definition (e.g., the type name) is required, as well as the <i>namespaceUri</i> for the namespace in which
the definition is defined.<p>
This class also contains a NamespaceRegistry that stores the namespaces defined at the global level of the schema. NOTE: it is
possible that schemafiles define and use namespaces that are not visible at the top-level of the schema. In this case, there will be
entries in the GlobalDefMap for these namespaces, but the namespaces themselves will not be registered in the NamespaceRegistry.

 *  
 *
 *@author    ostwald
 */
public class GlobalDefMap {

	private static boolean debug = true;
	
	/**
	 *  mapping of GlobalDefNames to GlobalDef instances
	 */
	private Map map;
	private NamespaceRegistry namespaces = null;

	/**
	 *  Should initialize members as required.
	 *
	 *@return    <tt>true</tt> if intialization successful, <tt>false</tt>
	 *      otherwise
	 */
	public boolean init() {
		namespaces = new NamespaceRegistry ();
		map = new TreeMap(new GlobalDefNameComparator());
		return true;
	}


	/**
	 */
	public GlobalDefMap() {
		init();
	}

	public NamespaceRegistry getNamespaces () {
		return namespaces;
	}
	
	/**
	 *  Should release resources and call the finalize method.
	 */
	public void destroy() {
		map.clear();
	}


	/**
	 *  Use this method to populate the <tt>XMLMap</tt> with the desired named
	 *  values.
	 */
	/* public void setMap() { } */


	/**
	 *  Description of the Method
	 *
	 *@param  key  Description of the Parameter
	 *@return      Description of the Return Value
	 */
 	public boolean containsKey(String key) {
		return map.containsKey(key);
	}

	/**
	 *  Method to retrieve the list of names used to identify desired values.
	 *
	 *@return    The keys value
	 */
 	public List getNsKeys() {
		ArrayList list = new ArrayList();
		Iterator i = map.keySet().iterator();
		while (i.hasNext()) {
			list.add(i.next());
		}
		return list;
	}

	public List getValues () {
		List vals = new ArrayList ();
		Iterator nsKeyIterator = getNsKeys().iterator();
		while (nsKeyIterator.hasNext()) {
			String nsUri = (String)nsKeyIterator.next();
			vals.addAll (getNsValues (nsUri));
		}
		return vals;
	}
	
	public Map getNsMap(String nsUri) {
		Map nsMap = (Map)map.get (nsUri);
		if (nsMap == null) {
			if (nsUri.equals(namespaces.schemaNamespaceUri) ||
				nsUri.equals(namespaces.xmlNamespaceUri)) {
				// add a new nsMap to the globalDefMap for schemaNamespace typeDefs (BuiltIns)
				nsMap = new HashMap();
				map.put (nsUri, nsMap);
			}
		}
		return nsMap;
	}

	public List getSimpleTypes () {
		List simpleTypes = new ArrayList();
		Iterator nsKeysIterator = getNsKeys().iterator();
		while (nsKeysIterator.hasNext()) {
			String nsUri = (String) nsKeysIterator.next();
			Map nsMap = getNsMap (nsUri);
			Iterator typeNameIterator = nsMap.keySet().iterator();
			while (typeNameIterator.hasNext()) {
				String typeName = (String) typeNameIterator.next();
				GlobalDef def = getValue (typeName, nsUri);
				if (def != null && def.isSimpleType())
					simpleTypes.add (def);
			}
		}
		return simpleTypes;
	}
	
	public List getComplexTypes () {
		List complexTypes = new ArrayList();
		Iterator nsKeysIterator = getNsKeys().iterator();
		while (nsKeysIterator.hasNext()) {
			String nsUri = (String) nsKeysIterator.next();
			Map nsMap = getNsMap (nsUri);
			Iterator typeNameIterator = nsMap.keySet().iterator();
			while (typeNameIterator.hasNext()) {
				String typeName = (String) typeNameIterator.next();
				GlobalDef def = getValue (typeName, nsUri);
				if (def != null && def.isComplexType())
					complexTypes.add (def);
			}
		}
		return complexTypes;
	}	
	
	public List getDefsOfType (int type) {
		List globalDefs = new ArrayList();
		Iterator nsKeysIterator = getNsKeys().iterator();
		while (nsKeysIterator.hasNext()) {
			String nsUri = (String) nsKeysIterator.next();
			Map nsMap = getNsMap (nsUri);
			Iterator defiterator = nsMap.values().iterator();
			while (defiterator.hasNext()) {
				GlobalDef def = (GlobalDef)defiterator.next();
				if (def != null && def.getDataType() == type)
					globalDefs.add (def);
			}
		}
		return globalDefs;
	}
	
	/**
	 *  Retrieves GlobalDefs associated with specified Namespace.
	 *
	 *@return    The values value
	 */
	 
	public List getNsValues(String nsUri) {
		ArrayList list = new ArrayList();
		Map nsMap = getNsMap(nsUri);
		if (nsMap == null)
			return list;
		Iterator i = nsMap.values().iterator();
		while (i.hasNext()) {
			Object obj = i.next();
			if (obj != null) {
				list.add( (GlobalDef)obj);
			}
		}
		return list;
	}
	
	/**
	 *  Accessor method for retrieving a specific named GlobalDef. Namespace prefixes for the
	 default name space (or the targetNameSpace if there is no defaultNamespace defined), are stripped.
	 *
	 *@param  name  Description of the Parameter
	 *@return       The value value
	 */
	public GlobalDef getValue(String name) {

		GlobalDef ret = null;
		if (NamespaceRegistry.isQualified(name)) {
			String prefix = NamespaceRegistry.getNamespacePrefix(name);
			String unqualifiedName = NamespaceRegistry.stripNamespacePrefix(name);
			Namespace ns = this.getNamespaces().getNSforPrefix(prefix);
			ret = this.getValue(unqualifiedName, ns);			
		}
		else {
			ret = getValue(name, namespaces.getDefaultNamespace());
			try {
				if (ret == null) {
					prtln ("GlobalDefMap failed to find " + name + " in the default namespace");
					throw new Exception ();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return ret;
	}

	public GlobalDef getValue(String name, Namespace ns) {
		return getValue (name, ns.getURI());
	}

	public GlobalDef getValue(String name, String nsUri) {
		// prtln ("\n getValue with name: " + name + "   nsUri: " + nsUri);
		Map nsMap = getNsMap (nsUri);
		if (nsMap == null) {
			prtln ("getValue(): nsMap not found for \"" + nsUri + "\" (eventually looking for " + name + ")");
			if (nsUri.trim().length() > 0) {
				prtln (" .. existing nsURIs");
				for (Iterator i=getNsKeys().iterator();i.hasNext();)
					prtln ("\t" + (String)i.next());
			}
			return null;
		}
		if (!nsMap.containsKey(name)) {
			// for Built-ins, we create a new object and place it in the globalDefMap under the schemaNamespaceUri
			if (nsUri.equals(namespaces.schemaNamespaceUri) ||
				nsUri.equals (namespaces.xmlNamespaceUri)) {
					
				String qualifiedName = NamespaceRegistry.makeQualifiedName(namespaces.getPrefixforUri(nsUri), name);
				GlobalDef globalDef = new BuiltInType (qualifiedName, namespaces.getSchemaNamespace());
				try {
					setValue(name, globalDef, namespaces.getSchemaNamespace());
				} catch (Exception e) {
					prtln ("error trying to create and set built-in type: " + e.getMessage());
					return globalDef;
				}
			}
			else {
				// prtln (" .. nsMap (" + nsUri + ") does not have a key of " + name);
			}
		}
		return (GlobalDef) nsMap.get(name);
	}
	
	/**
	 *  Inserts a GlobalDef instance in the GlobalDefMap.
	 *
	 *@param  name           The GlobalDef name
	 *@param  def            The globalDef object
	 *@exception  Exception  if the GlobalDef is already in the map
	 */
	public void setValue(String name, GlobalDef def, Namespace ns)
		throws Exception {
			
/* 		// debugging
		String s = "setValue()";
		s += "\n\t name: " + name;
		s += "\n\t def: " + def.getQualifiedName();
		s += "\n\t namespace: " + ns.getPrefix() + ": " + ns.getURI();
		prtln (s); 
*/
		if (!map.containsKey(ns.getURI()))
			map.put (ns.getURI(), new HashMap());
		
		Map nsMap = getNsMap (ns.getURI());
		
		if (nsMap.containsKey(name)) {
			String errorMsg = "GlobalDefMap already has key for \"" + name + "\"";
			GlobalDef existingDef = (GlobalDef) nsMap.get(name);
			String existingPath = existingDef.getLocation().toString();
			errorMsg += "\n\tExisting definition in " + existingPath;
			String newPath = def.getLocation();
			errorMsg += "\n\tNew definition in " + newPath;
			throw new Exception(errorMsg);
		}
		nsMap.put(name, def);
	}
	
	public static void setDebug (boolean d) {
		debug = d;
	}
	
	static void prtln(String s) {
		if (debug) {
			if (s.charAt(0) == '\n') {
				System.out.println ("");
				s = s.substring(1);
			}
			System.out.println("GlobalDefMap: " + s);
		}
	}
	
	class GlobalDefNameComparator implements Comparator {
		/**
		* sorts by case-insensitive order of keys
		*/
		public int compare(Object o1, Object o2) {
			try {
				return ((String)o1).toUpperCase().compareTo(((String)o2).toUpperCase());
			} catch (Throwable t) {
				return 0;
			}
		}
	}
	
}

