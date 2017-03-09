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
package org.dlese.dpc.ndr.reader;

import org.dlese.dpc.ndr.apiproxy.NDRConstants;
import org.dlese.dpc.ndr.NdrUtils;
import org.dlese.dpc.xml.Dom4jUtils;
import org.dlese.dpc.xml.schema.NamespaceRegistry;
import org.dom4j.*;
import org.jaxen.SimpleNamespaceContext;
import java.io.File;
import java.util.regex.*;
import java.net.URL;
import java.util.*;

/**
 *  Base Class for reading NDR responses to GET Requests, used primarily to
 *  support NDR Import operations. Extended to read specific types of NDR
 *  Objects, such as Metadata and MetadataProvider.
 *
 * @author    ostwald
 */
public class NdrObjectReader {

	private static boolean debug = true;

	private HashMap dataStreams = null;
	protected Document doc = null;
	protected List dsFormats;
	protected String handle = null;
	protected NDRConstants.NDRObjectType objectType = null;
	protected String lastModifiedDate = null;
	protected String createdDate = null;

	/**  Native data stream format for this object */
	protected String nativeDataStreamFormat = null;

	/**  nsContext to support xpath ops */
	protected SimpleNamespaceContext nsContext = null;


	/**
	 *  Constructor for the NdrObjectReader object with a XML Document (for testing
	 *  purposes)
	 *
	 * @param  response       NOT YET DOCUMENTED
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public NdrObjectReader(Document response) throws Exception {
		this(response, null);
	}


	/**
	 *  Constructor for the NdrObjectReader object with ndrHandle
	 *
	 * @param  handle         handle of NDR object to read
	 * @exception  Exception  if object cannot be read from NDR
	 */
	public NdrObjectReader(String handle) throws Exception {
		this(handle, null);
	}


	/**
	 *  Constructor for the NdrObjectReader object with ndrHandle and specified
	 *  nativeDataStreamFormat.
	 *
	 * @param  handle                  handle of NDR object to read
	 * @param  nativeDataStreamFormat  native metadata format (helps extract native
	 *      data_stream)
	 * @exception  Exception           NOT YET DOCUMENTED
	 */
	public NdrObjectReader(String handle, String nativeDataStreamFormat) throws Exception {
		/* this.dataStreams = new HashMap(); */
		this.nativeDataStreamFormat = nativeDataStreamFormat;
		this.handle = handle;
		this.init(NdrUtils.getNDRObjectDoc(handle));

	}


	/**
	 *  Constructor for the NdrObjectReader object
	 *
	 * @param  nativeDataStreamFormat  NOT YET DOCUMENTED
	 * @param  ndrResponse             NOT YET DOCUMENTED
	 * @exception  Exception           NOT YET DOCUMENTED
	 */
	public NdrObjectReader(Document ndrResponse, String nativeDataStreamFormat) throws Exception {
		/* this.dataStreams = new HashMap(); */
		this.nativeDataStreamFormat = nativeDataStreamFormat;
		this.init(ndrResponse);
	}


	/**
	 *  Returns a namespace context instance, containing namespaces occuring in NDR
	 *  objects, that is used in xpath operations.
	 *
	 * @return    The nsContext value
	 */
	protected SimpleNamespaceContext getNsContext() {
		if (this.nsContext == null) {
			this.nsContext = new SimpleNamespaceContext();

			Namespace NDR_RESPONSE_NAMESPACE =
				DocumentHelper.createNamespace("ndr", "http://ns.nsdl.org/ndr/response_v1.00/");

			nsContext.addNamespace(NDR_RESPONSE_NAMESPACE.getPrefix(),
				NDR_RESPONSE_NAMESPACE.getURI());
			nsContext.addNamespace(NDRConstants.AUTH_NAMESPACE.getPrefix(),
				NDRConstants.AUTH_NAMESPACE.getURI());
			nsContext.addNamespace(NDRConstants.NCS_NAMESPACE.getPrefix(),
				NDRConstants.NCS_NAMESPACE.getURI());
			nsContext.addNamespace(NDRConstants.FEDORA_VIEW_NAMESPACE.getPrefix(),
				NDRConstants.FEDORA_VIEW_NAMESPACE.getURI());
			nsContext.addNamespace(NDRConstants.FEDORA_MODEL_NAMESPACE.getPrefix(),
				NDRConstants.FEDORA_MODEL_NAMESPACE.getURI());
			nsContext.addNamespace(NDRConstants.NSDL_NAMESPACE.getPrefix(),
				NDRConstants.NSDL_NAMESPACE.getURI());
			nsContext.addNamespace(NDRConstants.OAI_NAMESPACE.getPrefix(),
				NDRConstants.OAI_NAMESPACE.getURI());
		}
		return nsContext;
	}


	/**
	 *  Intitialize the reader.
	 *
	 * @param  ndrResponse    XML Document representation of NDR "get" response
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	private void init(Document ndrResponse) throws Exception {
		this.doc = ndrResponse;
		this.handle = this.getHandle();
		this.initDataStreams();
	}


	/**
	 *  Gets the original "get" response for this reader object as a dom4j.Document
	 *  instance.
	 *
	 * @return    The document value
	 */
	public Document getDocument() {
		return this.doc;
	}


	/**
	 *  Gets the specified property of the NdrObjectReader object (returning the
	 *  first if more than one property is present). If provide prop is not
	 *  qualified, a namespace prefix of "nsdl" is assumed. If a namespace prefix
	 *  is provided (e.g., "ncs:status"), it must be contained in the nameSpace
	 *  Context for this reader.
	 *
	 * @param  prop  Description of the Parameter
	 * @return       The property value
	 */
	public String getProperty(String prop) {
		List values = getPropertyValues(prop);
		if (values.size() > 0) {
			return (String) values.get(0);
		}
		else {
			return null;
		}
	}


	/**
	 *  Gets a list of values for the specified property
	 *
	 * @param  name  propertyname, qualifed with "nsdl" if no namespace prefix is
	 *      present.
	 * @return       a list of property values for specified property.
	 */
	public List getPropertyValues(String name) {
		String path = "/ndr:NSDLDataRepository/ndr:NDRObject/ndr:properties/" + qname(name);
		List nodes = getNodes(path);
		List values = new ArrayList();
		for (Iterator i = nodes.iterator(); i.hasNext(); ) {
			Element e = (Element) i.next();
			values.add(e.getText());
		}
		return values;
	}


	/**
	 *  Gets the FIRST relationship (there may be more) with specified name, which
	 *  is assumed to be in "nsdl" namespace if no prefix is present.
	 *
	 * @param  name  relationship name ("auth:authorizedToChange")
	 * @return       the relationship value (a ndrHandle)
	 */
	public String getRelationship(String name) {
		List rels = getRelationshipValues(name);
		if (rels.size() > 0) {
			return (String) rels.get(0);
		}
		else {
			return null;
		}
	}


	/**
	 *  Gets all the relationships for the specified name.
	 *
	 * @param  name  Relationship name ("auth:authorizedToChange)
	 * @return       List of relationship values (ndr handles) for provided name
	 */
	public List getRelationshipValues(String name) {
		String xpath = "/ndr:NSDLDataRepository/ndr:NDRObject/ndr:relationships/" + qname(name);
		List nodes = getNodes(xpath);
		List values = new ArrayList();
		for (Iterator i = nodes.iterator(); i.hasNext(); ) {
			Element e = (Element) i.next();
			values.add(e.getText());
		}
		return values;
	}


	/**
	 *  Gets the handle attribute of the NdrObjectReader object.
	 *
	 * @return    The handle value
	 */
	public String getHandle() {
		return this.getProperty("hasHandle");
	}


	/**
	 *  Gets the objectType property of the NdrObject. Can be used as integrity
	 *  check.
	 *
	 * @return    The objectType value
	 */
	public NDRConstants.NDRObjectType getObjectType() {
		String objectTypeStr = this.getProperty("objectType");
		return NDRConstants.getNdrResponseType(objectTypeStr);
	}


	/**
	 *  Gets the fedora-model:state property of the NdrObject
	 *
	 * @return    The state value
	 */
	public NDRConstants.ObjectState getState() {
		String st = getProperty("fedora-model:state");
		return NDRConstants.ObjectState.getState(st);
	}


	/**
	 *  Gets the fedora-view:lastModifiedDate property of the NdrObject as a string
	 *
	 * @return    The lastModifiedDate value
	 */
	public String getLastModifiedDate() {
		return this.getProperty("fedora-view:lastModifiedDate");
	}


	/**
	 *  Gets the fedora-view:lastModifiedDate property of the NdrObject as a Date
	 *  object
	 *
	 * @return    The lastModified value
	 */
	public Date getLastModified() {
		return NdrUtils.parseNdrDateString(this.getLastModifiedDate());
	}


	/**
	 *  Gets the fedora-view:createdDate property of the NdrObject
	 *
	 * @return    The createdDate value
	 */
	public String getCreatedDate() {
		return this.getProperty("fedora-view:createdDate");
	}


	/**
	 *  Gets the fedora-view:createdDate property of the NdrObject as a Date object
	 *
	 * @return    The created value
	 */
	public Date getCreated() {
		return NdrUtils.parseNdrDateString(this.getCreatedDate());
	}


	/**
	 *  Builds a map containing entries for each datastream format in this object,
	 *  but does not populate the values of the map until necessary.<P>
	 *
	 *  NOTE: datastreams ending in "_info" are ignored and therefore not
	 *  accessible by "getDataStream" <P>
	 *
	 *  DataStream elements have format attribute named "format_"+DATA_STREAM_FORMAT"
	 */
	protected void initDataStreams() {
		this.dataStreams = new HashMap();
		String dsPath = "/ndr:NSDLDataRepository/ndr:NDRObject/ndr:data/ndr:format";
		List dsNodes = this.getNodes(dsPath);
		// prtln(dsNodes.size() + " dsNodes found");
		for (Iterator i = dsNodes.iterator(); i.hasNext(); ) {
			Element dsElement = (Element) i.next();

			String id = dsElement.attributeValue("ID");
			String format = id;
			String marker = "format_";
			if (id.startsWith(marker) && !id.endsWith("_info")) {
				format = format.substring(marker.length());
				this.dataStreams.put(format, null);
			}
		}
	}



	/**
	 *  Returns the data stream formats as a Set.
	 *
	 * @return    The formats value
	 */
	public Set getFormats() {
		return this.dataStreams.keySet();
	}


	/**
	 *  Resolves provided formatStr (e.g., "nsdl_dc") and version (e.g., "v1.01")
	 *  into a format spec that is used to select a dataStream from this reader
	 *  object.<p>
	 *
	 *  the formatStr and version params are used to select a single dataStream
	 *  from this reader object according the following rules:<P>
	 *
	 *
	 *  <ul>
	 *    <li> formatStr must contain "native" to select a native stream
	 *    <li> if formatStr is "native", version may be null - in this case the
	 *    "most recent" format matching the formatStr is selected. E.g., if
	 *    formatStr is "native_nsdl_dc" and there are streams for "native_nsdl_dc_v1.01"
	 *    and "native_nsdl_dc_v1.02" - the latter is selected. if supplied the
	 *    version and stream must match exactly.
	 *    <li> if formatStr does not contains "native", then an exacty match is
	 *    required. E.g., if format is null, the format selected will not have a
	 *    version, and if version is supplied, the format selected must match the
	 *    formatSpec exactly.
	 *  </ul
	 *
	 * @param  formatStr  e.g., "nsdl_dc"
	 * @param  version    e.g., "v1.01"
	 * @return            a formatSpec that can be used to select a data_stream
	 *      from this reader object.
	 */
	private String resolveDataStreamFormat(String formatStr, String version) {
		// prtln("ndr: " + NDRConstants.getNdrApiBaseUrl());
		// prtln("resolveDataStreamFormat  (\"" + formatStr + "\", \"" + version + "\")");
		List matches = new ArrayList();
		String formatSpec = formatStr;
		if (version != null)
			formatSpec += "_" + version;
		else if (formatStr.startsWith("native"))
			formatSpec += ".*";
		Pattern p = Pattern.compile(formatSpec);
		for (Iterator i = this.getFormats().iterator(); i.hasNext(); ) {
			String format = (String) i.next();
			String foo = "  " + format;
			if (p.matcher(format).matches()) {
				matches.add(format);
			}
		}

		int matchCnt = matches.size();
		if (matchCnt == 0)
			return null;
		else if (matchCnt == 1)
			return (String) matches.get(0);
		else {
			// resolve candidates by selecting last in lexical sort
			Collections.sort(matches);
			return (String) matches.get(matchCnt - 1);
		}
	}


	/**
	 *  Gets the native dataStream of the NdrObject for the specified native
	 *  "format".<p>
	 *
	 *  Note: this method cannot return a non-native datastream.
	 *
	 * @param  format  native datastream format (e.g., "oai_dc")
	 * @return         The dataStream value
	 */
	public Element getNativeDataStream(String format) {
		return getNativeDataStream(format, null);
	}


	/**
	 *  Gets the navite dataStream of the NdrObject for specified format and
	 *  version. If version is provided, the datastream must exactly match the
	 *  version.<p>
	 *
	 *  Note: this method cannot return a non-native datastream.
	 *
	 * @param  format   native datastream format (.e.g, "oai_dc")
	 * @param  version  version (e.g., "v1.01")
	 * @return          The nativeDataStream value
	 */
	public Element getNativeDataStream(String format, String version) {
		String formatSpec = "native_" + format;
		return getDataStream(formatSpec, version);
	}


	/**
	 *  Gets the dataStream of the NdrObjectReader for specified format.
	 *
	 * @param  format  datastream format (e.g., "nsdl_dc")
	 * @return         The dataStream value
	 */
	public Element getDataStream(String format) {
		return getDataStream(format, null);
	}


	/**
	 *  Gets the dataStream matching the provided formatSpec, or null if requested
	 *  stream is not present. Examples of formatSpec:
	 *  <ul>
	 *    <li> "nsdl_dc"
	 *    <li> "native_nsdl_dc", "v1.02"
	 *    <li> "native_msp2"
	 *  </ul>
	 *
	 *
	 * @param  format   datastream format (e.g., "nsdl_dc")
	 * @param  version  version (e.g., "v1.01")
	 * @return          The dataStream value
	 */
	public Element getDataStream(String format, String version) {
		// prtln("getDataStream() format: " + format + ", version: " + version);
		Element stream = null;
		String formatSpec = resolveDataStreamFormat(format, version);
		// prtln("resolved formatSpec: " + formatSpec);
		if (this.getFormats().contains(formatSpec)) {
			stream = (Element) this.dataStreams.get(formatSpec);
			if (stream == null) {
				// lazily fetch the data stream
				String dsPath = "/ndr:NSDLDataRepository/ndr:NDRObject/ndr:data/ndr:format";
				String formatPath = dsPath + "[@ID=\'format_" + formatSpec + "\']";
				Node dsNode = getNode(formatPath);
				if (dsNode != null) {
					try {
						String handle = dsNode.getText();
						URL handleUrl = new URL(handle);
						Document dsDoc = NdrUtils.getNDRObjectDoc(handleUrl);
						this.dataStreams.put(formatSpec, dsDoc.getRootElement().createCopy());
					} catch (Exception e) {
						// remove this format so we don't try again
						this.dataStreams.remove(formatSpec);
						prtlnErr("unable to get datastream for " + formatSpec);
						e.printStackTrace();
					}
				}
			}
			stream = (Element) this.dataStreams.get(formatSpec);
		}
		else {
			prtlnErr("\t object (" + getHandle() + " does NOT contain format: " + formatSpec);
		}
		return stream;
	}


	/**
	 *  Returns the NON_NATIVE data stream labeled simply as "nsdl_dc" with no
	 *  version
	 *
	 * @return    The canonicalNsdlDcDataStream value
	 */
	public Element getCanonicalNsdlDcDataStream() {
		return this.getDataStream("nsdl_dc");
	}

	// --------- DOM utilities ---------------

	/**
	 *  Get all Nodes satisfying the given xpath.
	 *
	 * @param  path  NOT YET DOCUMENTED
	 * @return       a List of all nodes satisfying given XPath, or null if path
	 *      does not exist.
	 */
	protected List getNodes(String path) {
		try {
			XPath xpath = getXPath(path);
			return xpath.selectNodes(doc);
		} catch (Throwable e) {
			// prtln("getNodes() failed with " + path + ": " + e);
		}
		return null;
	}


	/**
	 *  Converts the provided xpath string into an XPath instance using the
	 *  nsContext (see {@link getNsContext})
	 *
	 * @param  path  string representation of an xpath
	 * @return       The xPath value
	 */
	protected XPath getXPath(String path) {
		XPath xpath = DocumentHelper.createXPath(path);
		xpath.setNamespaceContext(this.getNsContext());
		return xpath;
	}


	/**
	 *  Return qualified name, using "nsdl" prefix if supplied name does not have a
	 *  namespace prefix.
	 *
	 * @param  name  provided name, which may or may not be qualified by namespace
	 *      prefix
	 * @return       qualified name
	 */
	private String qname(String name) {
		return (NamespaceRegistry.isQualified(name) ? name : "nsdl:" + name);
	}


	/**
	 *  Gets a single Node satisfying give XPath. If more than one Node is found,
	 *  the first is returned (and a msg is printed).
	 *
	 * @param  xpath  an XPath
	 * @return        a dom4j Node
	 */

	protected Node getNode(String xpath) {
		List list = getNodes(xpath);
		if ((list == null) || (list.size() == 0)) {
			// prtln ("getNode() did not find node for " + xpath);
			return null;
		}
		if (list.size() > 1) {
			prtln("getNode() found mulitple modes for " + xpath + " (returning first)");
		}
		return (Node) list.get(0);
	}


	/**
	 *  Return the Text of a Node satisfying the given XPath.
	 *
	 * @param  xpath  an XPath
	 * @return        Text of Node or empty String if no Node is found
	 */
	protected String getNodeText(String xpath) {
		Node node = getNode(xpath);
		try {
			return node.getText();
		} catch (Throwable t) {

			// prtln ("getNodeText() failed with " + xpath + "\n" + t.getMessage());
			// Dom4jUtils.prettyPrint (docMap.getDocument());
		}
		return "";
	}


	/**
	 *  Sets the debug attribute of the NdrObjectReader class
	 *
	 * @param  bool  The new debug value
	 */
	public static void setDebug(boolean bool) {
		debug = bool;
	}


	/**
	 *  Prints a dom4j.Node as formatted string.
	 *
	 * @param  node  NOT YET DOCUMENTED
	 */
	protected static void pp(Node node) {
		prtln(Dom4jUtils.prettyPrint(node));
	}


	/**
	 *  Description of the Method
	 *
	 * @param  s  Description of the Parameter
	 */
	private static void prtln(String s) {
		if (debug) {
			/* System.out.println("NdrObjectReader: " + s); */
			NdrUtils.prtln(s, "");
		}
	}


	private static void prtlnErr(String s) {
		NdrUtils.prtln(s, "NdrObjectReader");
	}

}

