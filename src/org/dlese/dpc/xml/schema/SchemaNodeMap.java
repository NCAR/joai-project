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

/**
 *  Map structure holding {@link org.dlese.dpc.xml.schema.SchemaNode} instances,
 *  keyed by XPaths to the Nodes.<p>
 *
 *  All Nodes (Elements & Attributes) defined by the Schema will be represented
 *  in the SchemaNodeMap. The keys of the SchemaNodeMap are normalized in the
 *  sense that there is only one entry for a repeating element (i.e., no Xpath
 *  key will contain indexing).
 *
 * @author    ostwald<p>
 *
 *
 */
public class SchemaNodeMap {
	private static boolean debug = false;

	/**  mapping of XPaths to GlobalDef instances */
	private Map map;
	private int docOrderCounter;
	private Map keyMap;


	/**
	 *  Should initialize members as required.
	 *
	 * @return    <tt>true</tt> if intialization successful, <tt>false</tt>
	 *      otherwise
	 */
	public boolean init() {
		/*

		NOTE: i changed implementation of "map" from TreeMap to HashMap when i
		realized that "getValue" and "containsKey" methods were not working
		properly (e.g., containsKey would return true when a key was not really
		in the map, and getValue would return THE WRONG VALUE when the key
		wasn't in the map! It workes fine under HashMap.

		*/
		map = new TreeMap(new DocOrderComparator());
		// map = new HashMap();
		keyMap = new HashMap();
		docOrderCounter = 0;
		return true;
	}



	/**  Constructor for the SchemaNodeMap object */
	public SchemaNodeMap() {
		init();
	}


	/**  Should release resources and call the finalize method. */
	public void destroy() {
		map.clear();
	}


	/**
	 *  Use this method to populate the <tt>XMLMap</tt> with the desired named
	 *  values.
	 */
	public void setMap() { }


	/**
	 *  Method to retrieve the list of names used to identify desired values.
	 *
	 * @return    The keys value
	 */
	public List getKeys() {
		ArrayList list = new ArrayList();
		Iterator i = map.keySet().iterator();
		while (i.hasNext()) {
			list.add(i.next());
		}
		return list;
	}


	/**
	 *  Description of the Method
	 *
	 * @param  key  Description of the Parameter
	 * @return      Description of the Return Value
	 */
	public boolean containsKey(String key) {
		return keyMap.containsKey(key);
	}


	/**
	 *  Method to retrieve the list of names used to identify desired values of a
	 *  particular type.
	 *
	 * @param  nodeType  Description of the Parameter
	 * @return           The keys value
	 */
	public List getKeys(short nodeType) {
		ArrayList list = new ArrayList();
		Iterator i = map.keySet().iterator();
		while (i.hasNext()) {
			String key = (String) i.next();
			SchemaNode n = (SchemaNode) getValue(key);
			if (n.getNodeType() == nodeType) {
				list.add(key);
			}
		}
		return list;
	}


	/**
	 *  Method to retrieve the list of values stored in this map.
	 *
	 * @return    The values value
	 */
	public List getValues() {
		ArrayList list = new ArrayList();
		Iterator i = map.keySet().iterator();
		while (i.hasNext()) {
			String key = (String) i.next();
			Object obj = map.get(key);
			if (obj != null) {
				list.add(obj);
			}
		}
		return list;
	}


	/**
	 *  Accessor method for retrieving a specific named GlobalDef.
	 *
	 * @param  path  Description of the Parameter
	 * @return       The value value
	 */
	public Object getValue(String path) {
		if (this.containsKey(path))
			return map.get(path);
		else
			return null;
	}


	/**
	 *  Setter method for updating a specific named value.
	 *
	 * @param  path        The new value value
	 * @param  schemaNode  The new value value
	 */
	public void setValue(String path, SchemaNode schemaNode) {
		schemaNode.setDocOrderIndex(docOrderCounter);
		keyMap.put(path, new Integer(docOrderCounter));
		map.put(path, schemaNode);
		docOrderCounter++;
	}


	/**
	 *  Description of the Method
	 *
	 * @return    Description of the Return Value
	 */
	public String toString() {
		StringBuffer s = new StringBuffer("\n");
		Iterator i = map.keySet().iterator();
		s.append("SchemaNodeMap has " + map.keySet().size() + " entries");
		while (i.hasNext()) {
			String path = (String) i.next();
			SchemaNode schemaNode = (SchemaNode) getValue(path);
			s.append("path: " + path);
			s.append(schemaNode.toString());
		}
		return s.toString();
	}


	/**
	 *  Comparator to sort the keyMap in document order. Element paths are
	 *  processed in doc order, so we map the paths to an incrementing counter, and
	 *  then maintain the map's order using the counter values.
	 *
	 * @author    Jonathan Ostwald
	 */
	public class DocOrderComparator implements Comparator {
		/**
		 *  sorts by order in which paths are processed by StructureWalker (and
		 *  therefore are added to the SchemaNodeMap)
		 *
		 * @param  o1  NOT YET DOCUMENTED
		 * @param  o2  NOT YET DOCUMENTED
		 * @return     NOT YET DOCUMENTED
		 */
		public int compare(Object o1, Object o2) {

			prtln("got\n\t 1 - " + (String) o1 + "\n\t 2 - " + (String) o2);

			Integer index1 = (Integer) keyMap.get((String) o1);
			Integer index2 = (Integer) keyMap.get((String) o2);

			if (index1 == null) {
				return 0;
			}

			if (index2 == null) {
				return 0;
			}

			return index1.compareTo(index2);
		}

	}


	/**
	 *  Sets the debug attribute of the SchemaNodeMap class
	 *
	 * @param  d  The new debug value
	 */
	public static void setDebug(boolean d) {
		debug = d;
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  s  NOT YET DOCUMENTED
	 */
	private static void prtln(String s) {
		if (debug) {
			System.out.println("SchemaNodeMap: " + s);
		}
	}

}

