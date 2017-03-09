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
import org.dlese.dpc.serviceclients.webclient.*;

import java.io.*;
import java.util.*;
import java.net.*;
import org.dom4j.Node;
import org.dom4j.Attribute;
import org.dom4j.Element;
import org.dom4j.Document;
import org.dom4j.Namespace;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

/**
 *  DefinitionMiner builds data structures that aid in processing and validation
 *  of XML Instance Documents. These key schema elements are represented as
 *  GlobalDefs and stored in a {@link GlobalDefMap} which is keyed by element
 *  name.
 *
 * @author    ostwald
 */
public class DefinitionMiner {

	private static boolean debug = true;

	private File schemaFile = null;
	private URL schemaURL = null;

	private URI schemaURI = null;

	private Map schemaReaders;
	private GlobalDefMap globalDefMap;
	private Document rootDoc;
	private Element schemaRootElement;
	private HashMap parsedDocs;
	private NamespaceRegistry namespaces;
	private long inlineIndex = 0;
	private Log log = null;


	/**
	 *  Constructor for the DefinitionMiner object.
	 *
	 * @param  schemaURI                  URI of root Schema File
	 * @param  rootElementName            NOT YET DOCUMENTED
	 * @param  log                        NOT YET DOCUMENTED
	 * @exception  SchemaHelperException  Description of the Exception
	 */
	public DefinitionMiner(URI schemaURI, String rootElementName, Log log) throws SchemaHelperException {
		this.log = log;
		String errorMsg = "";
		this.schemaURI = schemaURI;
		prtln("schemaURI is " + schemaURI.toString());
		init();
		try {
			rootDoc = getParsedDoc(schemaURI);
			namespaces.registerNamespaces(rootDoc);
			if (schemaDocNeedsConverting()) {
				prtln("Converting Schema Doc");

				Namespace namedDefaultNS = namespaces.getNamedDefaultNamespace();
				namespaces.register(namedDefaultNS);

				SchemaNamespaceConverter converter = new SchemaNamespaceConverter();
				rootDoc = converter.convert(rootDoc, namedDefaultNS.getPrefix());
			}

			prtln(namespaces.toString());
			processSchemaFile(schemaURI, null);
			processSubstitutionGroups();

			schemaRootElement = getRootElement(rootElementName);
			if (schemaRootElement == null) {
				errorMsg = "Could not find schemaRootElement in " + schemaURI.toString();
				throw new SchemaHelperException(errorMsg);
			}
		} catch (SchemaHelperException she) {
			errorMsg = "Definition miner: " + she.getMessage();
			prtlnErr(errorMsg);
			log.add(errorMsg);
			throw new SchemaHelperException(she.getMessage());
		} catch (NullPointerException e) {
			errorMsg = "Definition miner caught unknown Throwable: " + e.getMessage();
			log.add(errorMsg);
			throw new SchemaHelperException(errorMsg);
		}

	}


	/**
	 *  Returns true if the unqualified names in this schema file should be
	 *  converted to the namedDefault (prefix = "this") namespace, addressing the
	 *  problem that we cannot process multi-namespace documents having a "default"
	 *  namespace (i.e., with no prefix).<p>
	 *
	 *  We used to check if the default namespace was the same as the
	 *  schemaNamespace, but this did not cover the case in which the default
	 *  namespace was some OTHER namespace.<p>
	 *
	 *  Now we check for multi-namespace and that there is a default namespace
	 *  declared.
	 *
	 * @return    NOT YET DOCUMENTED
	 */
	private boolean schemaDocNeedsConverting() {

		// prtln("needsConverting");
		// prtln("defaultNS: " + NamespaceRegistry.nsToString(namespaces.getDefaultNamespace()));

		if (namespaces.isMultiNamespace() && namespaces.getDefaultNamespace().getURI().length() > 0)
			return true;

		return false;
	}


	/**  Initialize the data structures used by DefinitionMiner */
	private void init() {
		schemaReaders = new HashMap();
		globalDefMap = new GlobalDefMap();
		namespaces = globalDefMap.getNamespaces();
		parsedDocs = new HashMap();
	}


	/**
	 *  Gets the schemaURI attribute of the DefinitionMiner object
	 *
	 * @return    The schemaURI value
	 */
	public URI getSchemaURI() {
		return this.schemaURI;
	}


	/**
	 *  Sets the debug attribute of the DefinitionMiner class
	 *
	 * @param  d  The new debug value
	 */
	public static void setDebug(boolean d) {
		debug = d;
	}


	/**
	 *  For each GlobalElement in the globalDefMap that declares a substitutionGroup
	 *  attribute, place a reference to that GlobalElement in the head element's
	 *  substitutionGroup list.
	 */
	private void processSubstitutionGroups() {
		List globalElements = globalDefMap.getDefsOfType(GlobalDef.GLOBAL_ELEMENT);
		for (Iterator i = globalElements.iterator(); i.hasNext(); ) {
			GlobalElement globalElement = (GlobalElement) i.next();
			String headRef = globalElement.getElement().attributeValue("substitutionGroup", null);
			if (headRef != null) {
				GlobalDef headElement = globalElement.getSchemaReader().getGlobalDef(headRef);
				if (headElement != null) {
					if (!headElement.isGlobalElement())
						prtln("Substitution group (" + headRef + ") points to non-globalElement");
					else
						((GlobalElement) headElement).addSubstitutionGroupMember(globalElement);
				}
				else {
					prtln("global def not found for Substitution group (" + headRef + ")");
				}
			}
		}
	}


	/**
	 *  Gets the inlineTypeName attribute of the DefinitionMiner object
	 *
	 * @param  base  NOT YET DOCUMENTED
	 * @return       The inlineTypeName value
	 */
	public String getInlineTypeName(String base) {
		String name = base + "-inline-" + new Long(inlineIndex).toString();
		inlineIndex++;
		return name;
	}


	/**
	 *  Gets the namespaces attribute of the DefinitionMiner object
	 *
	 * @return    The namespaces value
	 */
	public NamespaceRegistry getNamespaces() {
		return namespaces;
	}


	/**
	 *  Adds a feature to the GlobalDef attribute of the DefinitionMiner object
	 *
	 * @param  name           The feature to be added to the GlobalDef attribute
	 * @param  globalDef      The feature to be added to the GlobalDef attribute
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public void addGlobalDef(String name, GlobalDef globalDef) throws Exception {
		globalDefMap.setValue(name, globalDef, Namespace.NO_NAMESPACE);
	}


	/**
	 *  Adds a feature to the GlobalDef attribute of the DefinitionMiner object
	 *
	 * @param  name           The feature to be added to the GlobalDef attribute
	 * @param  globalDef      The feature to be added to the GlobalDef attribute
	 * @param  ns             The feature to be added to the GlobalDef attribute
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public void addGlobalDef(String name, GlobalDef globalDef, Namespace ns) throws Exception {
		globalDefMap.setValue(name, globalDef, ns);
	}



	/**
	 *  Gets the globalDefMap attribute of the DefinitionMiner object
	 *
	 * @return    The globalDefMap value
	 */
	public GlobalDefMap getGlobalDefMap() {
		return globalDefMap;
	}


	/**
	 *  Gets the schemaRootElement attribute of the DefinitionMiner object
	 *
	 * @return    The schemaRootElement value
	 */
	public Element getSchemaRootElement() {
		return schemaRootElement;
	}


	/**
	 *  Gets the rootDoc attribute of the DefinitionMiner object
	 *
	 * @return    The rootDoc value
	 */
	public Document getRootDoc() {
		return rootDoc;
	}


	/**
	 *  Adds a feature to the SchemaFile attribute of the DefinitionMiner object
	 *
	 * @param  uri     The feature to be added to the SchemaFile attribute
	 * @param  reader  The feature to be added to the SchemaReader attribute
	 */
	public void addSchemaReader(URI uri, SchemaReader reader) {
		schemaReaders.put(uri, reader);
	}


	/**
	 *  Gets the schemaReader attribute of the DefinitionMiner object
	 *
	 * @param  uri  NOT YET DOCUMENTED
	 * @return      The schemaReader value
	 */
	public SchemaReader getSchemaReader(URI uri) {
		return (SchemaReader) schemaReaders.get(uri);
	}


	/**
	 *  Gets the schemaReader attribute of the DefinitionMiner object
	 *
	 * @param  nsUri  NOT YET DOCUMENTED
	 * @return        The schemaReader value
	 */
	public SchemaReader getSchemaReader(String nsUri) {
		try {
			return getSchemaReader(new URI(nsUri));
		} catch (Exception e) {
			prtlnErr("WARNING: getSchemaReader error: " + e.getMessage());
		}
		return null;
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  uri  NOT YET DOCUMENTED
	 * @return      NOT YET DOCUMENTED
	 */
	public boolean hasSchemaReader(URI uri) {
		return schemaReaders.containsKey(uri);
	}


	/**
	 *  Reads the contents of a file as a {@link org.dom4j.Document}. Documents are
	 *  cached in {@link parsedDocs}.
	 *
	 * @param  uri                        NOT YET DOCUMENTED
	 * @return                            The parsedDoc value
	 * @exception  SchemaHelperException  Description of the Exception
	 */
	private Document getParsedDoc(URI uri)
		 throws SchemaHelperException {
		Document doc = null;
		if (parsedDocs.containsKey(uri.toString())) {
			// prtln("getParsedDoc() returning existing Document");
			doc = (Document) parsedDocs.get(uri.toString());
		}
		else {
			// prtln("\n----------------------\ngetParsedDoc() parsing " + uri.toString());
			if (uri.getScheme().equals("file")) {
				File file = new File(uri.getPath());
				try {
					doc = Dom4jUtils.getXmlDocument(file);
				} catch (DocumentException de) {
					throw new SchemaHelperException("XML could not be parsed in " + file.toString());
				} catch (MalformedURLException me) {
					throw new SchemaHelperException("File not found: " + file.toString());
				}
			}
			else if (uri.getScheme().equals("http")) {
				URL url = null;
				String msg = "";
				try {
					url = uri.toURL();
					// doc = WebServiceClient.getTimedXmlDocument(url);
					doc = Dom4jUtils.getXmlDocument(url);
				} catch (MalformedURLException me) {
					msg = "URL could not be created from " + uri.toString() + ": " + me.getMessage();
					throw new SchemaHelperException(msg);
				} catch (DocumentException de) {
					msg = "XML could not be parsed in " + url.toString() + ": " + de.getMessage();
					throw new SchemaHelperException(msg);
				}
				/* catch (WebServiceClientException she) {
					throw new SchemaHelperException(she.getMessage());
				} */
			}
			parsedDocs.put(uri.toString(), doc);
		}
		return doc;
	}


	/**
	 *  Gets the root element of the schema (representing the rootElement of the
	 *  INSTANCE document).<P>
	 *
	 *  NOTE: Gets the FIRST root element. If there is more than one top-level
	 *  element, the root element must be defined first. This is NOT a great
	 *  assumption, since schemas may define more than one top-level element. ToDO:
	 *  figure out how spy handles schemas that define multiple top-level elements.
	 *
	 * @param  rootElementName            NOT YET DOCUMENTED
	 * @return                            The rootElement value
	 * @exception  SchemaHelperException  NOT YET DOCUMENTED
	 */
	private Element getRootElement(String rootElementName) throws SchemaHelperException {

		/* 		prtln("\n--------------------------");
		prtln("getRootElement()");
		prtln("\t ... rootElementName: " + rootElementName); */
		List globalElements = this.globalDefMap.getDefsOfType(GlobalDef.GLOBAL_ELEMENT);
		// prtln ("GlobalElements (" + globalElements.size() + " found)");

		// find the globalElements defined in the targetNamespace of ComplexType
		TreeMap candidates = new TreeMap();
		for (Iterator i = globalElements.iterator(); i.hasNext(); ) {
			GlobalElement globalElement = (GlobalElement) i.next();
			String name = globalElement.getQualifiedInstanceName();
			// prtln ("\t" + name + " (" + globalElement.getNamespace().getURI() + ")");
			if (name != null && name.equals(rootElementName))
				return globalElement.getElement();
			else {
				candidates.put(globalElement.getQualifiedInstanceName(), globalElement);
			}
		}
		prtln(candidates.size() + " candidates found");

		if (candidates.size() != 1) {
			throw new SchemaHelperException(ambiguousRootElementMsg(candidates));
		}
		String name = (String) candidates.firstKey();
		GlobalElement rootElementDef = (GlobalElement) candidates.get(name);

		return rootElementDef.getElement();
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  candidates  NOT YET DOCUMENTED
	 * @return             NOT YET DOCUMENTED
	 */
	private String ambiguousRootElementMsg(TreeMap candidates) {
		String NL = "\n\t";
		String msg = "Unable to identify root element from schema";
		msg += NL + "framework config must specify one of the following global " +
			"elements as the rootElement:";
		for (Iterator i = candidates.keySet().iterator(); i.hasNext(); ) {
			String name = (String) i.next();
			GlobalElement ge = (GlobalElement) candidates.get(name);
			msg += NL + "\t" + name + " (" + ge.getNamespace().getURI() + ")";
		}
		return msg;
	}


	/**
	 *  Process all files in the schema tree recursively. Does a depth-first
	 *  traversal of the include structure of the multi-file schema, and for each
	 *  file, finds the DataType definitions (ComplexType, SimpleType and
	 *  GlobalElement) and adds them all to the global GlobalDefMap.
	 *
	 * @param  uri                        the particular file to be processed
	 * @param  defaultTargetNamespaceURI  NOT YET DOCUMENTED
	 * @exception  SchemaHelperException  NOT YET DOCUMENTED
	 */
	protected void processSchemaFile(URI uri, String defaultTargetNamespaceURI) throws SchemaHelperException {
		if (hasSchemaReader(uri)) {
			return;
		}
		Document doc;
		// prtln ("\nprocessSchemaFile: " + uri);

		try {
			doc = getParsedDoc(uri);
		} catch (Exception e) {
			prtlnErr(e.getMessage());
			throw new SchemaHelperException(e.getMessage());
		}

		SchemaReader reader = new SchemaReader(doc, uri, this, defaultTargetNamespaceURI);
		// addSchemaReader before actually reading to prevent infinite loop with mutually refering schema files
		addSchemaReader(uri, reader);
		reader.read();
	}


	/**
	 *  The main program for the DefinitionMiner class
	 *
	 * @param  args           The command line arguments
	 * @exception  Exception  Description of the Exception
	 */
	public static void main(String[] args)
		 throws Exception {
		String schemaFilePath = "/export/devel/ostwald/metadata-frameworks/ADN-v0.6.50/record.xsd";
		File schemaFile = new File(schemaFilePath);
		String rootElementName = null;
		DefinitionMiner miner = new DefinitionMiner(schemaFile.toURI(), rootElementName, new Log());

		if (args.length == 1) {
			String type = args[0];
			GlobalDef def = (GlobalDef) miner.globalDefMap.getValue(type);
			if (def != null) {
				/*
				    switch (def.getDataType()) {
				    case GlobalDef.SIMPLE_TYPE:
				    def = (SimpleType)def;
				    break;
				    case GlobalDef.COMPLEX_TYPE:
				    def = (ComplexType)def;
				    break;
				    default:
				    System.out.println ("found XSD Node was of unhandled type: " + def.getDataType());
				    break;
				    }
				  */
				System.out.println(def.toString());
			}
			else {
				System.out.println(type + " not found");
			}
		}
	}


	/**  NOT YET DOCUMENTED */
	public void destroy() {
		schemaReaders.clear();
		parsedDocs.clear();
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
			System.out.println("defMiner: " + s);
		}
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  s  NOT YET DOCUMENTED
	 */
	private static void prtlnErr(String s) {
		while (s.length() > 0 && s.charAt(0) == '\n') {
			System.out.println("");
			s = s.substring(1);
		}
		System.out.println("defMiner: " + s);
	}

}

