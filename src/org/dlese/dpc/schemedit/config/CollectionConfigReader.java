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
package org.dlese.dpc.schemedit.config;

import java.io.*;
import java.util.*;
import java.text.*;

import org.dlese.dpc.xml.*;
import org.dlese.dpc.util.*;
import org.dlese.dpc.schemedit.*;

import org.dom4j.Document;
import org.dom4j.DocumentFactory;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Attribute;
import org.dom4j.Node;

/**
 *  Extracts info from a Collection configuration document. Used to populate
 *  {@link org.dlese.dpc.schemedit.config.CollectionConfig} objects for each of
 *  the frameworks supported by DCS.
 *
 * @author     ostwald
 */
public class CollectionConfigReader extends AbstractConfigReader {

	/**  NOT YET DOCUMENTED */
	protected static boolean debug = true;
	private Map statusMap = null;
	private Map tupleMap = null;


	/**
	 *  Create a CollectionConfigReader.
	 *
	 * @param  source         Description of the Parameter
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public CollectionConfigReader(File source) throws Exception {
		super(source);
	}


	String idPath = "/collectionConfigRecord/collectionId";


	/**
	 *  optional node
	 *
	 * @return    The discussionURL value
	 */
	public String getId() {
		return getNodeText(idPath);
	}


	/**
	 *  Sets the discussionURL attribute of the CollectionConfigReader object
	 *
	 * @param  s  The new discussionURL value
	 */
	public void setId(String s) {
		setNodeText(idPath, s);
	}


	String idPrefixPath = "/collectionConfigRecord/idPrefix";


	/**
	 *  optional node
	 *
	 * @return    The idPrefix value
	 */
	public String getIdPrefix() {
		return getNodeText(idPrefixPath);
	}


	/**
	 *  Sets the idPrefix attribute of the CollectionConfigReader object
	 *
	 * @param  s  The new idPrefix value
	 */
	public void setIdPrefix(String s) {
		// prtln("setIdPrefix(): " + s);
		setNodeText(idPrefixPath, s);
		// prtln("\t sanity check: idPrefix: " + this.getIdPrefix());
	}


	String exportDirectoryPath = "/collectionConfigRecord/exportDirectory";


	/**
	 *  Gets the name attribute of the CollectionConfigReader object
	 *
	 * @return    The name value
	 */
	public String getExportDirectory() {
		return getNodeText(exportDirectoryPath);
	}


	/**
	 *  Sets the exportDirectory attribute of the CollectionConfigReader object
	 *
	 * @param  s  The new exportDirectory value
	 */
	public void setExportDirectory(String s) {
		setNodeText(exportDirectoryPath, s);
	}


	String xmlFormatPath = "/collectionConfigRecord/xmlFormat";


	/**
	 *  Gets the xmlFormat attribute of the CollectionConfigReader object
	 *
	 * @return    The xmlFormat value
	 */
	public String getXmlFormat() {
		return getNodeText(xmlFormatPath);
	}


	/**
	 *  Sets the xmlFormat attribute of the CollectionConfigReader object
	 *
	 * @param  s  The new xmlFormat value
	 */
	public void setXmlFormat(String s) {
		setNodeText(xmlFormatPath, s);
	}


	String authorityPath = "/collectionConfigRecord/authority";


	/**
	 *  Gets the authority attribute of the CollectionConfigReader object
	 *
	 * @return    The authority value
	 */
	public String getAuthority() {
		String a = getNodeText(authorityPath);
		return (a == null || a.trim().equals("")) ? "dcs" : a;
	}


	/**
	 *  Sets the authority attribute of the CollectionConfigReader object
	 *
	 * @param  s  The new authority value
	 */
	public void setAuthority(String s) {
		setNodeText(authorityPath, s);
	}

	// ---------- Services ---------------
	String servicesPath = "/collectionConfigRecord/services";
	String allowSuggestionsPath = "/collectionConfigRecord/services/allowSuggestions";
	
	public boolean getAllowSuggestions () {
		String value = getNodeText (allowSuggestionsPath);
		return "true".equals(value);
	}
	
	public void setAllowSuggestions (boolean value) {
		setNodeText (allowSuggestionsPath, (value ? "true" : "false"));
	}
		
	
	// ------- NDR INFO ---------------

	String ndrInfoPath = "/collectionConfigRecord/ndrInfo";


	/**  
	* Remove all ndrInfo from the CollectionConfig and set "authority" to "dcs".
	*/
	public void clearNdrInfo() {
		Element ndrInfo = (Element) getNode(ndrInfoPath);
		if (ndrInfo != null) {
			ndrInfo.clearContent();
		}
		setAuthority("dcs");
	}


	String mdpPath = ndrInfoPath + "/metadataProvider";


	String aggPath = ndrInfoPath + "/aggregator";


	String metadataProviderHandlePath = mdpPath + "/handle";


	/**
	 *  Gets the metadataProviderHandle attribute of the CollectionConfigReader
	 *  object
	 *
	 * @return    The metadataProviderHandle value
	 */
	public String getMetadataProviderHandle() {

		String ret = getNodeText(metadataProviderHandlePath);
		// prtln ("getMetadataProviderHandle()  returning: \"" + ret + "\"");
		return ret;
	}


	/**
	 *  Sets the metadataProviderHandle attribute of the CollectionConfigReader
	 *  object
	 *
	 * @param  handle  The new metadataProviderHandle value
	 */
	public void setMetadataProviderHandle(String handle) {
		// prtln("setMetadataProviderHandle()  handle: \"" + handle + "\"");
		Node node =
			DocumentHelper.makeElement(this.getDocument(), metadataProviderHandlePath);
		node.setText(handle);
	}


	String aggregatorHandlePath = aggPath + "/handle";


	/**
	 *  Gets the aggregatorHandle attribute of the CollectionConfigReader object
	 *
	 * @return    The aggregatorHandle value
	 */
	public String getAggregatorHandle() {
		return getNodeText(aggregatorHandlePath);
	}


	/**
	 *  Sets the aggregatorHandle attribute of the CollectionConfigReader object
	 *
	 * @param  pid  The new aggregatorHandle value
	 */
	public void setAggregatorHandle(String pid) {
		Node node = DocumentHelper.makeElement(this.getDocument(), aggregatorHandlePath);
		node.setText(pid);
	}

	String agentPath = ndrInfoPath + "/agent";
	String agentHandlePath = agentPath + "/handle";


	/**
	 *  Gets the agentHandle attribute of the CollectionConfigReader object
	 *
	 * @return    The agentHandle value
	 */
	public String getAgentHandle() {
		return getNodeText(agentHandlePath);
	}


	/**
	 *  Sets the agentHandle attribute of the CollectionConfigReader object
	 *
	 * @param  pid  The new agentHandle value
	 */
	public void setAgentHandle(String pid) {
		Node node = DocumentHelper.makeElement(this.getDocument(), agentHandlePath);
		node.setText(pid);
	}
	
	
	String ndrOaiLinkPath = ndrInfoPath + "/ndrOaiLink";


	/**
	 *  Gets the ndrOaiLink attribute of the CollectionConfigReader object
	 *
	 * @return    The ndrOaiLink value
	 */
	public String getNdrOaiLink() {
		return getNodeText(ndrOaiLinkPath);
	}


	/**
	 *  Sets the ndrOaiLink attribute of the CollectionConfigReader object
	 *
	 * @param  link  The new ndrOaiLink value
	 */
	public void setNdrOaiLink(String link) {
		Node node = DocumentHelper.makeElement(this.getDocument(), ndrOaiLinkPath);
		node.setText(link);
	}

	String ndrServerPath = ndrInfoPath + "/ndrServer";


	/**
	 *  Gets the ndrServer attribute of the CollectionConfigReader object
	 *
	 * @return    The ndrServer value
	 */
	public String getNdrServer() {
		return getNodeText(ndrServerPath);
	}


	/**
	 *  Sets the ndrServer attribute of the CollectionConfigReader object
	 *
	 * @param  link  The new ndrServer value
	 */
	public void setNdrServer(String server) {
		Node node = DocumentHelper.makeElement(this.getDocument(), ndrServerPath);
		node.setText(server);
	}

	String finalStatusLabelPath = "/collectionConfigRecord/statusFlags/@finalStatusLabel";


	/**
	 *  Gets the finalStatusLabel attribute of the CollectionConfigReader object
	 *
	 * @return    The finalStatusLabel value
	 */
	public String getFinalStatusLabel() {
		String s = getNodeText(finalStatusLabelPath);
		if (s == null || s.trim().length() == 0)
			s = StatusFlags.DEFAULT_FINAL_STATUS;
		return s;
	}


	/**
	 *  Sets the finalStatusLabel attribute of the CollectionConfigReader object
	 *
	 * @param  s  The new finalStatusLabel value
	 */
	public void setFinalStatusLabel(String s) {
		if (s == null || s.trim().length() == 0)
			s = StatusFlags.DEFAULT_FINAL_STATUS;
		prtln("setting finalStatusLabel to " + s);
		setNodeText(finalStatusLabelPath, s);
	}


	String statusFlagsPath = "/collectionConfigRecord/statusFlags";


	/*
	** return a Map where the keys are status flags and the values are the descriptions
	*/
	/**
	 *  Gets the statusMap attribute of the CollectionConfigReader object
	 *
	 * @return    The statusMap value
	 */
	public Map getStatusMap() {
		if (statusMap == null) {
			statusMap = new HashMap();
			Node statusFlagsNode = getNode(statusFlagsPath);
			if (statusFlagsNode != null) {
				Element statusFlagsElement = (Element) statusFlagsNode;
				for (Iterator i = statusFlagsElement.elementIterator(); i.hasNext(); ) {
					Element statusFlagElement = (Element) i.next();
					try {
						StatusFlag statusFlag = new StatusFlag(statusFlagElement);
						statusMap.put(statusFlag.getLabel(), statusFlag);
					} catch (Exception e) {
						prtln("getStatusMapError: " + e.getMessage());
					}
				}
			}
		}
		return statusMap;
	}


	/**
	 *  NOTE: if we edit the config record via schemedit, the record is
	 *  automatically updated and then we have to reload it. But if we edit the
	 *  statusMap via another editor, then this method is necessary to update the
	 *  config file
	 *
	 * @param  sMap           The new statusMap value
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public void setStatusMap(Map sMap) throws Exception {
		// prtln ("setStatusMap()");
		Node statusFlagsNode = getNode(statusFlagsPath);
		if (statusFlagsNode == null) {
			statusFlagsNode = getDocMap().createNewNode(statusFlagsPath);
			if (statusFlagsNode == null)
				throw new Exception("statusMap node could not found or created");
		}
		Element statusFlagsElement = (Element) statusFlagsNode;
		statusFlagsElement.clearContent();
		statusMap = null;

		for (Iterator i = sMap.keySet().iterator(); i.hasNext(); ) {
			String status = (String) i.next();
			String description = (String) sMap.get(status);

			Element statusFlag = statusFlagsElement.addElement("statusFlag");

			Element statusElement = statusFlag.addElement("status");
			statusElement.setText(status);

			Element descriptionElement = statusFlag.addElement("description");
			descriptionElement.setText(description);
		}
	}


	String tuplesPath = "/collectionConfigRecord/tuples";


	/**
	 *  Gets the tupleMap attribute of the CollectionConfigReader object
	 *
	 * @return    The tupleMap value
	 */
	public Map getTupleMap() {
		if (tupleMap == null) {
			// prtln ("getTupleMap()");
			tupleMap = new HashMap();
			Node tuplesNode = getNode(tuplesPath);
			if (tuplesNode != null) {
				Element tuplesElement = (Element) tuplesNode;
				for (Iterator i = tuplesElement.elementIterator(); i.hasNext(); ) {
					Element tupleElement = (Element) i.next();
					Element nameElement = tupleElement.element("name");
					if (nameElement == null)
						continue;
					String name = nameElement.getText();
					Element valueElement = tupleElement.element("value");
					String value;
					if (valueElement == null)
						value = "";
					else
						value = valueElement.getText();
					tupleMap.put(name, value);

				}
			}
		}
		return tupleMap;
	}


	/**
	 *  Sets the tupleMap attribute of the CollectionConfigReader object
	 *
	 * @param  tMap           The new tupleMap value
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public void setTupleMap(Map tMap) throws Exception {
		Node tuplesNode = getNode(tuplesPath);
		if (tuplesNode == null) {
			tuplesNode = getDocMap().createNewNode(tuplesPath);
			if (tuplesNode == null)
				throw new Exception("tupleMap node could not found or created");
		}
		Element tuplesElement = (Element) tuplesNode;
		tuplesElement.clearContent();
		tupleMap = null;

		for (Iterator i = tMap.keySet().iterator(); i.hasNext(); ) {
			String name = (String) i.next();
			String value = (String) tMap.get(name);

			Element tuple = tuplesElement.addElement("tuple");

			Element statusElement = tuple.addElement("name");
			statusElement.setText(name);

			Element descriptionElement = tuple.addElement("value");
			descriptionElement.setText(value);
		}
	}


	/**  Force update by clearing cached values  */
	public void refresh() {
		super.refresh();
		statusMap = null;
		tupleMap = null;
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @return    NOT YET DOCUMENTED
	 */
	private String showDoc() {
		try {
			return (Dom4jUtils.prettyPrint(this.getDocument()));
		} catch (Throwable t) {
			prtln("document could not be pretty-printed: " + t.getMessage());
		}
		return "";
	}


	/**
	 *  Print a line to standard out.
	 *
	 * @param  s  The String to print.
	 */
	protected static void prtln(String s) {
		if (debug) {
			SchemEditUtils.prtln(s, "CollectionConfigReader");
		}
	}

}

