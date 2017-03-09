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

import org.dlese.dpc.xml.*;

import java.io.*;
import java.util.*;
import java.net.*;
import org.jaxen.SimpleNamespaceContext;
import org.dom4j.Node;
import org.dom4j.Attribute;
import org.dom4j.Element;
import org.dom4j.Document;
import org.dom4j.Namespace;
import org.dom4j.QName;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

/**
 *  NamespaceRegistry holds namespace information and provides namespace
 *  utilities.
 *
 *
 *
 * @author     ostwald
 */
public class NamespaceRegistry {

	private static boolean debug = false;

	/**  schema Instance Namespace Uri */
	public final static String schemaInstanceNamespaceUri = "http://www.w3.org/2001/XMLSchema-instance";
	/**  schema Namespace Uri */
	public final static String schemaNamespaceUri = "http://www.w3.org/2001/XMLSchema";

	/**  URI of xml namespace */
	public final static String xmlNamespaceUri = "http://www.w3.org/XML/1998/namespace";
	/**  NOT YET DOCUMENTED */
	public final static String xmlNamespacePrefix = "xml";

	/**  NOT YET DOCUMENTED */
	public final static Namespace NO_NAMESPACE = Namespace.NO_NAMESPACE;

	/**  NOT YET DOCUMENTED */
	public static boolean namedDefaultCreationEnabled = true;

	private HashMap prefixMap = new HashMap();
	private HashMap uriMap = new HashMap();

	private String targetNamespaceUri = null;
	private Namespace namedDefaultNamespace = null;
	private Namespace defaultNamespace = null;
	private Namespace schemaNamespace = null;
	private Namespace schemaInstanceNamespace = null;


	/**  Initialize the data structures used by NamespaceRegistry */
	public NamespaceRegistry() {
		// prtln ("\nNamespaceRegistry: namedDefaultCreationEnabled: " + namedDefaultCreationEnabled + "\n");
		Namespace xmlNS = new Namespace(xmlNamespacePrefix, xmlNamespaceUri);
		register(xmlNS);
	}


	/**
	 *  Register a namespace by placing itto the uriMap (mapping uri to its
	 *  namespace) and resets default namespaces so they will be recomputed using
	 *  updated uriMap.
	 *
	 * @param  ns  namespace to be registered
	 */
	public void register(Namespace ns) {
		// prtln ("registering namespace (" + ns.getPrefix() + ") " + ns.getURI());
		prefixMap.put(ns.getPrefix(), ns);
		/*
			NOTE: this implementation of uriMap assumes that there will be only one namespace per uri,
			which is probably not safe. In particular, if we define a "namedDefaultNamespace" then we
			have two namespaces with the same uri. HOWEVER, this implementation allows us to assume that
			the namespace returned for a given uri is the last-registered namespace, and that is a nice
			property, e.g., when we register a namedDefaultNamespace, then we get this when we ask for
			the namespace assigned to it's uri, and this is what we count on in schema processing when we
			call "getSchemaNamespace()".
		*/
		String uri = ns.getURI();
		Namespace existing = getNSforUri(uri);
		if (existing != NO_NAMESPACE) {
			// prtln ("\t NOTE: overwriting existing namespace with prefix: \"" + existing.getPrefix() + "\"");
		}

		uriMap.put(ns.getURI(), ns);

		// reset these namespaces, which will get recomputed when needed
		namedDefaultNamespace = null;
		defaultNamespace = null;

	}


	/**
	 *  Returns true if there is a namespace defined in addition to the default ns
	 *  and the schemaNamespace. Multinamespace documents require that we ensure
	 *  there is a named default namespace so all xPath references are qualified.
	 *
	 * @return    The multiNamespace value
	 */
	public boolean isMultiNamespace() {
		// prtln ("isMultiNamespace()");
		List uris = new ArrayList();
		for (Iterator i = prefixMap.values().iterator(); i.hasNext(); ) {
			Namespace ns = (Namespace) i.next();
			String uri = ns.getURI();
			if (uri != this.schemaNamespaceUri &&
				uri != this.xmlNamespaceUri) {
				uris.add(uri);
				// prtln ("\tadded " + uri);
			}
		}
		return (uris.size() > 1);
	}


	/**
	 *  Register all the namespaces defined in the docuement's rootElement.
	 *
	 * @param  doc  NOT YET DOCUMENTED
	 */
	public void registerNamespaces(Document doc) {

		Element root = doc.getRootElement();
		setTargetNamespaceUri(root.attributeValue("targetNamespace"));
		register(getNamespace(doc));
		List otherNamespaces = root.additionalNamespaces();
		if (otherNamespaces != null) {
			for (Iterator i = otherNamespaces.iterator(); i.hasNext(); ) {
				register((Namespace) i.next());
			}
		}
	}


	/**
	 *  Gets the namespaceContext attribute of the NamespaceRegistry object
	 *
	 * @return    The namespaceContext value
	 */
	public SimpleNamespaceContext getNamespaceContext() {
		SimpleNamespaceContext nsContext = new SimpleNamespaceContext();
		for (Iterator i = this.getNamespaces().iterator(); i.hasNext(); ) {
			Namespace ns = (Namespace) i.next();
			nsContext.addNamespace(ns.getPrefix(), ns.getURI());
		}
		return nsContext;
	}


	/**
	 *  Utility returns an iterator for the registed namespaces
	 *
	 * @return    NOT YET DOCUMENTED
	 */
	public Iterator nsIterator() {
		return this.getNamespaces().iterator();
	}


	/**
	 *  Utility returns all Namespaces as a Collection.
	 *
	 * @return    The namespaces value
	 */
	public Collection getNamespaces() {
		return prefixMap.values();
	}


	/**
	 *  Debugging utility returns a printable representation of the uriMap.
	 *
	 * @return    NOT YET DOCUMENTED
	 */
	public String uriMapToString() {
		String s = "\nUriMap";
		for (Iterator i = uriMap.entrySet().iterator(); i.hasNext(); ) {
			Map.Entry entry = (Map.Entry) i.next();
			String uri = (String) entry.getKey();
			Namespace ns = (Namespace) entry.getValue();
			s += "\n\t" + uri + "(" + this.nsToString(ns) + ")";
		}
		return s + "\n";
	}


	/**
	 *  Remove a namespace from prefixMap and uriMap structures.
	 *
	 * @param  ns  NOT YET DOCUMENTED
	 */
	public void unregister(Namespace ns) {
		prtln("unregistering namespace (" + ns.getPrefix() + ")");
		prefixMap.remove(ns.getPrefix());
		uriMap.remove(ns.getURI());
	}


	/**
	 *  Returns a namespace having a non-empty prefix and the same URI as the
	 *  defaultNameSpace (if one exists). Returns NO_NAMESPACE if there is no
	 *  defaultNamespace in the registry.
	 *
	 * @return    The namedDefaultNamespace value
	 */
	public Namespace getNamedDefaultNamespace() {
		// prtln ("getNamedDefaultNamespace()");
		if (namedDefaultNamespace == null) {
			namedDefaultNamespace = NO_NAMESPACE;
			Namespace defaultNS = getDefaultNamespace();

			// if there is no default, then there is no named default
			if (defaultNS == NO_NAMESPACE) {
				// prtln ("\t no default namespace, returning NO_NAMESPACE");
				return namedDefaultNamespace;
			}

			// namedDefaultNamespace = findNamedDefaultNamespace();
			String defaultURI = defaultNS.getURI();
			// look for a Namespace having defaultURI and a non-null prefix
			for (Iterator i = prefixMap.entrySet().iterator(); i.hasNext(); ) {
				Map.Entry entry = (Map.Entry) i.next();
				String prefix = (String) entry.getKey();
				Namespace ns = (Namespace) entry.getValue();
				String uri = (String) ns.getURI();
				if (prefix.trim().length() > 0 && defaultURI.equals(uri)) {
					namedDefaultNamespace = ns;
					return namedDefaultNamespace;
				}
			}

			// if we get here, we haven't found a namedDefaultNamespace
			if (namedDefaultCreationEnabled) {
				// namedDefaultNamespace = new Namespace("__default__", defaultURI);
				namedDefaultNamespace = new Namespace("this", defaultURI);
			}
		}
		return namedDefaultNamespace;
	}


	/**
	 *  Gets the Namespace for provided URI.
	 *
	 * @param  uri  NOT YET DOCUMENTED
	 * @return      A namespace object if one is found, NO_NAMESPACE otherwise
	 */
	public Namespace getNSforUri(String uri) {
		// prtln ("\ngetNSforUri() with " + uri);
		Namespace ns = (Namespace) uriMap.get(uri);
		if (ns == null)
			return NO_NAMESPACE;
		else {
			return ns;
		}
	}


	/**
	 *  Gets the prefix corresponding to the provided uri after finding that uri's
	 *  namespace.
	 *
	 * @param  uri  NOT YET DOCUMENTED
	 * @return      The prefixforUri value
	 */
	public String getPrefixforUri(String uri) {
		Namespace ns = getNSforUri(uri);
		if (ns == Namespace.NO_NAMESPACE) {
			// prtln ("WARNING: getPrefixforUri() did not find namespace not found for " + uri);
		}
		return ns.getPrefix();
	}


	/**
	 *  Gets the prefix for given namespace object, but uses the LOCAL namespace
	 *  (obtained by the provided namespace's uri) so that the prefix returned is
	 *  correct within the local context. <p>
	 *
	 *
	 *
	 * @param  ns  NOT YET DOCUMENTED
	 * @return     The prefixforNS value
	 */
	public String getPrefixforNS(Namespace ns) {
		// the given namespace may come from a different context, and therefore we
		// use the ns.Uri to find the local namespace before determining prefix
		String nsUri = ns.getURI();
		Namespace localNs = getNSforUri(nsUri);
		return localNs.getPrefix();
	}


	/**
	 *  Gets the namespace corresponding to the specified prefix
	 *
	 * @param  prefix  namespace prefix
	 * @return         A namespace object, or NO_NAMESPACE if one cannot be found.
	 */
	public Namespace getNSforPrefix(String prefix) {
		Namespace ns = (Namespace) prefixMap.get(prefix);
		if (ns == null)
			return NO_NAMESPACE;
		else
			return ns;
	}


	/**
	 *  Gets the prefixMap attribute of the NamespaceRegistry object
	 *
	 * @return    The prefixMap value
	 */
	public Map getPrefixMap() {
		return prefixMap;
	}


	/**
	 *  Gets the targetNamespaceUri attribute of the NamespaceRegistry object
	 *
	 * @return    The targetNamespaceUri value
	 */
	public String getTargetNamespaceUri() {
		return targetNamespaceUri;
	}


	/**
	 *  Gets the targetNamespace attribute of the NamespaceRegistry object
	 *
	 * @return    The targetNamespace value
	 */
	public Namespace getTargetNamespace() {
		return getNSforUri(getTargetNamespaceUri());
	}


	/**
	 *  Sets the targetNamespaceUri attribute of the NamespaceRegistry object
	 *
	 * @param  uri  The new targetNamespaceUri value
	 */
	public void setTargetNamespaceUri(String uri) {
		targetNamespaceUri = uri;
	}


	/**
	 *  Gets the defaultNamespace attribute of the NamespaceRegistry object
	 *
	 * @return    The defaultNamespace value
	 */
	public Namespace getDefaultNamespace() {
		if (defaultNamespace == null)
			defaultNamespace = getNSforPrefix("");
		return defaultNamespace;
	}


	/**
	 *  The schemaInstanceNamespace is used to introduce "xsi:type", "xsi:nil",
	 *  "xsi:schemaLocation", and "xsi:noNamespaceSchmaLocation" attributes in
	 *  instance documents. This namespece should always be defined with the xsi
	 *  prefix.<p>
	 *
	 *  ISSUE: is it okay to assume that the schemaInstanceNamespace prefix will
	 *  always be "xsi"??
	 *
	 * @return    The schemaInstanceNamespace value
	 */
	public Namespace getSchemaInstanceNamespace() {
		if (schemaInstanceNamespace == null) {
			schemaInstanceNamespace = new Namespace("xsi", schemaInstanceNamespaceUri);
		}
		return schemaInstanceNamespace;
	}


	/**
	 *  Gets the schemaNamespace attribute of the NamespaceRegistry object
	 *
	 * @return    The schemaNamespace value
	 */
	public Namespace getSchemaNamespace() {
		return getNSforUri(schemaNamespaceUri);
	}


	/* ---------- static utility methods -----------------*/
	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  element  NOT YET DOCUMENTED
	 * @return          NOT YET DOCUMENTED
	 */
	public static String doQname(Element element) {
		String s = "\tdoQname()";
		QName qname = element.getQName();
		s += "\n\t\t prefix: " + qname.getNamespacePrefix();
		s += "\n\t\t qualifiedName: " + qname.getQualifiedName();
		s += "\n\t\t uri(uri): " + qname.getNamespaceURI();
		return s;
	}


	/**
	 *  Make a qualified name - prefix:name;
	 *
	 * @param  prefix  a namespace prefix
	 * @param  name    name to be qualified
	 * @return         qualified name
	 */
	public static String makeQualifiedName(String prefix, String name) {
		if (prefix != null && prefix.trim().length() > 0)
			return prefix + ":" + name;
		else
			return name;
	}


	/**
	 *  Returns namespace.prefix:name, or just name if a namespace is not provided;
	 *
	 * @param  namespace  a namespace instance
	 * @param  name       name to be qualified
	 * @return            qualified name
	 */
	public static String makeQualifiedName(Namespace namespace, String name) {
		if (namespace == null)
			return name;

		return makeQualifiedName(namespace.getPrefix(), name);
	}


	/**
	 *  Gets the qName attribute of the NamespaceRegistry object
	 *
	 * @param  qualifiedName  NOT YET DOCUMENTED
	 * @return                The qName value
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public QName getQName(String qualifiedName) throws Exception {
		if (!isQualified(qualifiedName))
			throw new Exception("getQName expects a qualifiedName is input");
		String prefix = getNamespacePrefix(qualifiedName);
		Namespace ns = this.getNSforPrefix(prefix);
		if (ns == NO_NAMESPACE)
			throw new Exception("Namespace not found for prefix: " + prefix);
		String name = stripNamespacePrefix(qualifiedName);
		return DocumentHelper.createQName(name, ns);
	}


	/**
	 *  Gets the qualified attribute of the NamespaceRegistry class
	 *
	 * @param  s  NOT YET DOCUMENTED
	 * @return    The qualified value
	 */
	public static boolean isQualified(String s) {
		String prefix = getNamespacePrefix(s);
		return (prefix != null && !prefix.equals(""));
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  ns  NOT YET DOCUMENTED
	 * @return     NOT YET DOCUMENTED
	 */
	public static String nsToString(Namespace ns) {
		if (ns == null) {
			return "NULL NAMESPACE";
		}

		return "\"" + ns.getPrefix() + "\" : \"" + ns.getURI() + "\"";
	}


	/**
	 *  Gets the namespace attribute of the NamespaceRegistry class
	 *
	 * @param  doc  NOT YET DOCUMENTED
	 * @return      The namespace value
	 */
	public static Namespace getNamespace(Document doc) {
		Element root = doc.getRootElement();

		Namespace ns = root.getNamespace();
		if (ns == null)
			prtln("namespace not found");

		return ns;
	}


	/**
	 *  Gets the namespacePrefix attribute of the NamespaceRegistry class
	 *
	 * @param  name  NOT YET DOCUMENTED
	 * @return       The namespacePrefix value
	 */
	public static String getNamespacePrefix(String name) {
		int i = name.indexOf(":");
		if (i != -1)
			return name.substring(0, i);
		else
			return null;
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  name  NOT YET DOCUMENTED
	 * @return       NOT YET DOCUMENTED
	 */
	public static String stripNamespacePrefix(String name) {
		int i = name.indexOf(":");
		if (i != -1)
			return name.substring(i + 1);
		else
			return name;
	}


	/**
	 *  The main program for the NamespaceRegistry class
	 *
	 * @param  args  The command line arguments
	 */
	public static void main(String[] args) {
		String s = "foo:farb";
		if (args.length > 0) {
			s = args[0];
		}
		prtln("input: " + s + ",  prefix: " + getNamespacePrefix(s));
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @return    NOT YET DOCUMENTED
	 */
	public String toString() {
		String s = "Registered Namespaces (namedDefaultCreationEnabled: " + namedDefaultCreationEnabled + ")";
		for (Iterator i = getNamespaces().iterator(); i.hasNext(); ) {
			Namespace ns = (Namespace) i.next();
			s += "\n\t" + nsToString(ns);
		}
		s += "\n key namespaces";
		s += "\n\t" + "defaultNamespace: " + nsToString(getDefaultNamespace());
		s += "\n\t" + "schemaNamespace: " + nsToString(getSchemaNamespace());
		s += "\n\t" + "schemaInstanceNamespace: " + nsToString(getSchemaInstanceNamespace());
		s += "\n\t" + "namedDefaultNamespace: " + nsToString(getNamedDefaultNamespace());
		s += "\n\t" + "targetNamespace: " + nsToString(getTargetNamespace());
		return s;
	}


	/* --------------------------------------------------*/
	/**
	 *  Description of the Method
	 *
	 * @param  s  Description of the Parameter
	 */
	static void prtln(String s) {
		if (debug) {
			// System.out.println("NamespaceRegistry: " + s);
			System.out.println(s);
		}
	}

}

