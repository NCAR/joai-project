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
import org.dom4j.Element;
import org.dom4j.Namespace;

/**
 *  Mains a pushdown stack of SchemaReaders for use in StructureWalker to resolve qualified names and find
 GobalDef instances from the GlobalDefMap.
 *
 * @author     ostwald
 * @version    $Id: ReaderStack.java,v 1.3 2009/03/20 23:34:01 jweather Exp $
 */
public class ReaderStack {
	private static boolean debug = false;
	private GlobalDefMap globalDefMap;
	private List items = null;


	/**  Constructor for the ReaderStack object */
	public ReaderStack(GlobalDefMap globalDefMap) {
		this.globalDefMap = globalDefMap;
		items = new ArrayList();
	}

	/**
	* Traverse the stack to find a SchemaReader that defines a namespace for the given prefix. Return null if
	a reader is not found.
	*/
	public SchemaReader getReaderForPrefix (String prefix) {
		SchemaReader sr = null;
		prtln ("\tReaderStack.getReaderForPrefix() looking for prefix: " + prefix);
		for (int i=0; i < size(); i++) {
			sr = getItemAt(i);
			prtln ("\t\t " + sr.getLocation());
			Namespace ns = sr.getNamespaces().getNSforPrefix(prefix);
			if (ns != Namespace.NO_NAMESPACE) {
				prtln ("\t\t\t ... found reader at -- " + sr.getLocation().toString());
				break;
			}
		}
		return sr;
	}
	/**
	* Search the stack (moving from local to more global SchemaReaders) for the namespace
	* belonging to the given prefix.
	*/
	public Namespace getNamespaceForPrefix (String prefix) {
		Namespace ns = Namespace.NO_NAMESPACE;
		prtln ("\n\tReaderStack.getNamespace() looking for prefix: " + prefix);
		for (int i=0; i < size(); i++) {
			SchemaReader sr = getItemAt(i);
			prtln ("\t\t " + sr.getLocation());
			NamespaceRegistry namespaceContext = sr.getNamespaces();
			ns = namespaceContext.getNSforPrefix(prefix);
			if (!ns.getPrefix().equals("")) {
				prtln ("\t\t\t ... found namespace -- " + ns.getPrefix() + ": " + ns.getURI());
				break;
			}
		}
		return ns;
	}

	/**
	 *  Gets the itemAt attribute of the ReaderStack object
	 *
	 * @param  index  NOT YET DOCUMENTED
	 * @return        The itemAt value
	 */
	public SchemaReader getItemAt(int index) {
		if (index > items.size())
			return null;
		return (SchemaReader) items.get(index);
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  reader  NOT YET DOCUMENTED
	 */
	public void push(SchemaReader reader) {
		items.add(0, reader);
		prtln("\n PUSHED " + getReaderUri(getTos()));
	}


	/**
	 *  Gets the tos attribute of the ReaderStack object
	 *
	 * @return    The tos value
	 */
	public SchemaReader getTos() {
		if (items.size() < 1) {
			prtln("WARNING: getTos called with empty stack!");
			return null;
		}
		return (SchemaReader) items.get(0);
	}

	public GlobalDef getGlobalDef (String typeName) {
		prtln ("\ngetGlobalDef() typeName: " + typeName);
		prtln ("\n---------" + toString() + "\n-----------");
		SchemaReader schemaReader = getTos();
		Namespace namespace = schemaReader.getNamespaces().getTargetNamespace();
		String prefix = null;
		String baseName = typeName;
		boolean isMultiNamespace = globalDefMap.getNamespaces().isMultiNamespace();
		prtln ("\t isMultiNamespace: " + isMultiNamespace);
		if (NamespaceRegistry.isQualified(typeName)) {
			baseName = NamespaceRegistry.stripNamespacePrefix(typeName);
			prefix = NamespaceRegistry.getNamespacePrefix(typeName);
			if (prefix == null) prefix="";
			namespace = getNamespaceForPrefix(prefix);
			if (namespace == Namespace.NO_NAMESPACE) {
				prtln ("\t WARNING: namespace not found for \"" + typeName + "\"");
				return null;
			}
/* 			if (namespace == namespaces.getSchemaNamespace()) {
				// return null;
				prtln ("\t\t\t creating builtInType: " + NamespaceRegistry.makeQualifiedName(prefix, typeName));
				return new BuiltInType (NamespaceRegistry.makeQualifiedName(prefix, typeName));
			} */
		}
		// experimental 3/29/2007 - if there is a default Namespace, look in it for 
		
		else {
			prtln ("\t typeName is NOT qualified ... ");
			
			// try to use default name space, then use target ...
			Namespace defaultNamespace = schemaReader.getNamespaces().getDefaultNamespace();
			prtln ("\t\t default namespace is " + NamespaceRegistry.nsToString(namespace));
			if (defaultNamespace != Namespace.NO_NAMESPACE)
				namespace = defaultNamespace;
			
			prtln ("\t\t target namespace is " + NamespaceRegistry.nsToString(namespace));
		}
		return globalDefMap.getValue (baseName, namespace);
	}

	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @return    NOT YET DOCUMENTED
	 */
	public SchemaReader pop() {
		if (items.size() < 1) {
			prtln("WARNING: pop called with empty stack!");
			return null;
		}
		prtln("\n POPPING " + getReaderUri(getTos()));
		return (SchemaReader) items.remove(0);
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @return    NOT YET DOCUMENTED
	 */
	public int size() {
		return items.size();
	}


	/**
	 *  Returns a list of Item instances - one for each element in the choice ReaderStack
	 *
	 * @return    The items value
	 */
	public List getItems() {
		return items;
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  uri  NOT YET DOCUMENTED
	 * @return      NOT YET DOCUMENTED
	 */
	public boolean contains(String uri) {
		return getItemUris().contains(uri);
	}


	/**
	 *  Gets the itemUris attribute of the ReaderStack object
	 *
	 * @return    The itemUris value
	 */
	public List getItemUris() {
		List uris = new ArrayList();
		for (Iterator i = items.iterator(); i.hasNext(); ) {
			SchemaReader reader = (SchemaReader) i.next();
			uris.add(getReaderUri(reader));
		}
		return uris;
	}


	/**
	 *  Gets the readerUri attribute of the ReaderStack class
	 *
	 * @param  reader  NOT YET DOCUMENTED
	 * @return         The readerUri value
	 */
	public static String getReaderUri(SchemaReader reader) {
		return reader.getLocation().toString();
	}


	/**  NOT YET DOCUMENTED */
	public void clear() {
		items.clear();
	}


	/**
	 *  Gets the indexOfItem attribute of the ReaderStack object
	 *
	 * @param  uri  NOT YET DOCUMENTED
	 * @return      The indexOfItem value
	 */
	public int getIndexOfItem(String uri) {
		for (int i = 0; i < items.size(); i++) {
			SchemaReader reader = (SchemaReader) items.get(i);
			if (getReaderUri(reader).equals(uri))
				return i;
		}
		return -1;
	}


	/**
	 *  Finds a particular Item from the items list
	 *
	 * @param  uri   NOT YET DOCUMENTED
	 * @return       The item value
	 */
	public SchemaReader getItem(String uri) {
		for (Iterator i = items.iterator(); i.hasNext(); ) {
			SchemaReader reader = (SchemaReader) i.next();
			if (getReaderUri(reader).equals(uri))
				return reader;
		}
		return null;
	}


	/**
	 *  Description of the Method
	 *
	 * @return    Description of the Return Value
	 */
	public String toString() {
		String s = "ReaderStack (" + items.size() + " ) items";
		for (int i = 0; i < items.size(); i++) {
			if (i == 0)
				s += "\n\t" + "TOS: ";
			else
				s += "\n\t" + i + ": ";
			SchemaReader reader = (SchemaReader) items.get(i);
			s += getReaderUri(reader);
		}
		return s;
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  s  NOT YET DOCUMENTED
	 */
	public static void prtln(String s) {
		if (debug) {
			while (s.length() > 0 && s.charAt(0) == '\n') {
				System.out.println("");
				s = s.substring(1);
			}
			
			System.out.println("ReaderStack: " + s);
		}
	}
}

