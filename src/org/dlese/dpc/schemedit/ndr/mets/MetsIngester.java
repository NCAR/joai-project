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
package org.dlese.dpc.schemedit.ndr.mets;

import org.dlese.dpc.services.dds.toolkit.*;

import java.util.*;
import java.net.*;
import java.io.*;
import org.dlese.dpc.schemedit.*;
import org.dlese.dpc.ndr.NdrUtils;
import org.dlese.dpc.ndr.toolkit.NDRToolkit;
import org.dlese.dpc.ndr.toolkit.MimeTypes;
import org.nsdl.repository.model.types.*;
import org.nsdl.repository.model.types.Type;
import org.nsdl.repository.model.Datastream;
import org.nsdl.repository.model.Datastream.Name;
import org.nsdl.repository.model.NDRObject;
import org.nsdl.repository.model.NDRObjectInfo;
import org.nsdl.repository.access.Results;
import org.nsdl.repository.util.NDRObjectTemplate;

import org.dlese.dpc.schemedit.test.TesterUtils;
import org.dlese.dpc.standards.asn.NameSpaceXMLDocReader;

import org.dlese.dpc.xml.Dom4jUtils;
import org.dlese.dpc.ndr.toolkit.ContentUtils;
import org.dlese.dpc.ndr.apiproxy.NDRConstants;
import org.dom4j.*;

/**
 *  Unpack a METS envelop and place the contents (metadata + content) into the
 *  NDR.<p>
 *
 *  For now, assume the collection (mdp and agg) objects already exist in the
 *  NDR and are provided to this class.
 *
 * @author    ostwald
 */
public abstract class MetsIngester extends NameSpaceXMLDocReader {

	private static boolean debug = true;
	File source;
	Map structMap = null;
	Map fileMap = null;
	Map dmdMap = null;
	NDRToolkit ndrToolkit = null;
	MetadataProvider metadataProvider = null;
	Aggregator aggregator = null;


	/**
	 *  Constructor for the MetsIngester object
	 *
	 * @param  doc            NOT YET DOCUMENTED
	 * @param  aggHandle      NOT YET DOCUMENTED
	 * @param  mdpHandle      NOT YET DOCUMENTED
	 * @exception  Exception  NOT YET DOCUMENTED
	 */

	MetsIngester(Document doc, String aggHandle, String mdpHandle) throws Exception {
		super(doc);
		// collectionInfoInit(aggHandle, mdpHandle);
		dmdMapInit();
		fileMapInit();
		try {
			prtln("ndrApiBaseUrl = " + NDRConstants.getNdrApiBaseUrl());
			ndrToolkitInit();
		} catch (Throwable t) {
			prtln("ndrToolkit init error: " + t.getMessage());
			return;
		}
		this.aggregator = this.ndrToolkit.getAggregator(aggHandle);
		if (aggregator == null)
			throw new Exception("Aggregator not found for " + aggHandle);
		this.metadataProvider = this.ndrToolkit.getMetadataProvider(mdpHandle);
		if (metadataProvider == null)
			throw new Exception("MetadataProvider not found for " + mdpHandle);
		
		clear_collection();
		
		ingest();

	}



	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	void clear_collection() throws Exception {

		prtln ("Purging Metadata");
		Results <Metadata> results = metadataProvider.getMembers();
		try {
			for (Metadata md : results) {
				this.ndrToolkit.getNdrAccess().purgeObject (md);
				prtln ("\t" + md.handle() + " purged");
			}
		} finally {
			results.close();
		}
		
		prtln ("Purging Resources");
		NDRObject resTemplate = new NDRObjectTemplate (Type.Resource);
		resTemplate.addRelationship(Resource.MEMBER_OF, aggregator);
		
		Results <NDRObjectInfo> resources = this.ndrToolkit.getFinder().matchObject(resTemplate);
		try {
			for (NDRObjectInfo res : resources) {
				this.ndrToolkit.getNdrAccess().purgeObject (res);
				prtln ("\t" + res.handle() + " purged");
			}
		} finally {
			resources.close();
		}
		
	}
	
	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	void ndrToolkitInit() throws Exception {
		// MimeTypes.setPath ("C:/Program Files/Apache Software Foundation/Tomcat 5.5/conf/web.xml");
		// MimeTypes.setPath("/Users/ostwald/devel/projects/dlese-tools-project/src/org/dlese/dpc/schemedit/ndr/mets/mime-mappings.xml");
		ndrToolkit = new NDRToolkit();
		if (ndrToolkit == null)
			throw new Exception("no tool kit");
		if (ndrToolkit.getNdrAccess() == null)
			throw new Exception("ndr access is NOT there");
	}


	/**  Walk the structMap and ingest using the dmd and file ids */
	void ingest() {
		List structDivs = this.getNodes("/this:mets/this:structMap/this:div/this:div");
		prtln(structDivs.size() + " struct elements found");
		for (Iterator i = structDivs.iterator(); i.hasNext(); ) {
			Element structDiv = (Element) i.next();

			try {
				String dmdID = this.getValueAtPath(structDiv, "@DMDID");
				String fileID = this.getValueAtPath(structDiv, "this:fptr/@FILEID");
				prtln("\n- " + dmdID + " -> " + fileID);
				this.ingestItem(dmdID, fileID);
			} catch (Throwable t) {
				prtln("couldn't ingest record! " + t.getMessage());
				t.printStackTrace();
			}
			
		}
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  dmdID          descriptive metadata id
	 * @param  fileID         file id
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	void ingestItem(String dmdID, String fileID) throws Exception {

		prtln("ingestItem() dmdID: " + dmdID + "  fileID: " + fileID);
		boolean useExistingResource = false;

		// get metadata and determine filename from url
		DmdSec dmdSec = (DmdSec) dmdMap.get(dmdID);
		if (dmdSec == null)
			prtln ("dmdSec is null for " + dmdID);
		Element metadataElement = dmdSec.metadata;
		String filename = dmdSec.filename;
		
		// Create Resource using file
		Resource resource = null;
		if (useExistingResource) {
			// use existing resource to keep ndr pollution down
			resource = ndrToolkit.getResource("2200/test.20090722191119450T");
		}
		else {
			// Resource with primary content
			resource = ndrToolkit.newResource();
			FileData fileData = (FileData) fileMap.get(fileID);
			String resHandle = ndrToolkit.setResourceContent(resource, fileData.binData, dmdSec.filename, fileData.mimeType);
			// prtln("resHandle: " + resHandle);
		}

		// connect resource to aggregator
		aggregator.addMember(resource);
		aggregator.commit();

		// now make the metadata
		Metadata metadata = metadataProvider.newMetadata(resource);
		if (metadata == null)
			prtln ("metadata is null");
		
		
		setNDRResourceUrl (metadataElement, resource);
		
		String metadataString = metadataElement.asXML();
		InputStream data = new ByteArrayInputStream(metadataString.getBytes("UTF-8"));

		Datastream.Name itemDatastream =
			new Datastream.Name("format_" + this.getXmlFormat());

		Datastream ncsItemDS = metadata.addDatastream(itemDatastream);
		ncsItemDS.setMimeType("text/xml");
		ncsItemDS.setContent(data);

		metadata.commit();
		prtln("metadata handle: " + metadata.handle());
	}

	void setNDRResourceUrl (Element metadataElement, Resource resource) throws Exception {
		
		// we need to fetch the resource again to get it's current properties
		Resource reloaded = ndrToolkit.getResource(resource.handle());
		
		URL contentURL = reloaded.getContentURL();
		// prtln ("contentURL: " + contentURL.toString());
		Node node = metadataElement.selectSingleNode (this.getUrlXpath());
		
		if (node == null)
			throw new Exception ("url element not found");
		
		node.setText (contentURL.toString());
	}
	
	abstract String getXmlFormat();
	
	/**
	* Returns RELATIVE xpath to URL starting from root (E.g., if the absolute path to
	* url is "/record/URL", this method should return "URL");
	*/
	abstract String getUrlXpath ();
	
	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	void fileMapInit() throws Exception {
		fileMap = new HashMap();
		List fileElements = this.getNodes("/this:mets/this:fileSec/this:fileGrp/this:file");
		prtln("\nfileMap - " + fileElements.size() + " file elements found");
		for (Iterator i = fileElements.iterator(); i.hasNext(); ) {
			Element fileElement = (Element) i.next();
			try {
				FileData fileData = new FileData(fileElement);
				String id = fileElement.attributeValue("ID");
				fileMap.put(id, fileData);
			} catch (Throwable t) {
				prtln("WARNING: could not process fileElement");
			}
		}
	}


	/**
	 *  Descriptive Metadata Map - maps id to DmdSec instance
	 *
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	void dmdMapInit() throws Exception {
		dmdMap = new HashMap();
		List dmdSecs = this.getNodes("this:mets/this:dmdSec");
		prtln("\ndmdMap - " + dmdSecs.size() + " dmdSec nodes found");
		for (Iterator i = dmdSecs.iterator(); i.hasNext(); ) {
			Element dmdSec = (Element) i.next();
			DmdSec dmd = new DmdSec(dmdSec);
			String id = dmdSec.attributeValue("ID");
			dmdMap.put(id, dmd);
		}
	}


	class FileData {
		String id;
		String mimeType = null;
		String name;
		String content;
		byte[] binData;


		/**
		 *  Constructor for the FileData object
		 *
		 * @param  fileElement    NOT YET DOCUMENTED
		 * @exception  Exception  NOT YET DOCUMENTED
		 */
		FileData(Element fileElement) throws Exception {
			id = fileElement.attributeValue("ID");
			mimeType = fileElement.attributeValue("MIMETYPE");
			Element fContent = fileElement.element(getQName("this:FContent"));
			if (fContent == null)
				throw new Exception("fContent not found for file " + id);
			name = fContent.attributeValue("ID");
			content = fContent.element("binData").getTextTrim();
			binData = ContentUtils.decodeString(content);
		}


		/**  NOT YET DOCUMENTED */
		void report() {
			prtln("\n** FileData");
			prtln("id: " + id);
			prtln("mimeType: " + mimeType);
			prtln("name: " + name);
			prtln("\n- " + id);
			prtln(content.substring(0, 300));
		}
	}


	class DmdSec {
		String id;
		String xmlFormat;
		Element metadata;
		String filename;


		/**
		 *  Constructor for the DmdSec object
		 *
		 * @param  e              NOT YET DOCUMENTED
		 * @exception  Exception  NOT YET DOCUMENTED
		 */
		DmdSec(Element e) throws Exception {

			id = getValueAtPath(e, "@ID");
			xmlFormat = getValueAtPath(e, "this:mdWrap/@OTHERMDTYPE");
			Element metadataParent = (Element) getNode(e, "this:mdWrap/this:xmlData");
			metadata = (Element) metadataParent.elements().get(0);
			filename = getFilename();
			
		}

		String getFilename () {
			String url = null;
			try {
				url = URLDecoder.decode (getValueAtPath (metadata, getUrlXpath()));
			} catch (Exception e) {
				prtln ("getFilename error: " + e.getMessage());
				prtln (pp (metadata));
				System.exit(1);
				url = "UNKNOWN.jpg";
			}
			return new File(url).getName();
		}
		
		
		/**  NOT YET DOCUMENTED */
		void report() {
			prtln("\n** DmdSec");
			prtln("id: " + id);
			prtln("xmlFormat: " + xmlFormat);
			prtln("metada: " + pp(metadata));
		}
	}


	/**
	 *  The main program for the MetsIngester class
	 *
	 * @param  args           The command line arguments
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public static void main(String[] args) throws Exception {
		TesterUtils.setSystemProps();

		String env = "sanluis";

		String propsPath = null;
		String metsPath = null;

		if (env.equals("sanluis")) {
			propsPath = "C:/Documents and Settings/ostwald/devel/ndrServerProps/ndr.test.properties";
			// propsPath = "C:/Documents and Settings/ostwald/devel/ndrServerProps/dls.ndr.properties";
			metsPath = "H:/Documents/Alliance/mets-work/mets-record.xml";
		}
		if (env.equals("taos")) {
			propsPath = "/Users/ostwald/projects/dcs.properties";
			metsPath = "/Users/ostwald/devel/tmp/mets-record.xml";
		}
		NdrUtils.setup(new File(propsPath));

		Document metsDoc = null;
		try {
			metsDoc = Dom4jUtils.getXmlDocument(new File(metsPath));
		} catch (Throwable e) {
			prtln(e.getMessage());
			return;
		}

		String ndrHost = "ndrtest";
		String mdpHandle = null;
		String aggHandle = null;
		if (ndrHost.equals("dls")) {
			aggHandle = "ndr:2";
			mdpHandle = "ndr:3";
		}
		if (ndrHost.equals("ndrtest")) {
			aggHandle = "2200/test.20090821185036799T";
			mdpHandle = "2200/test.20090821185037493T";
		}

		// MetsIngester ingester = new MetsIngester(metsDoc, aggHandle, mdpHandle);
		// prtln (pp (mdr.getDocument()));

	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  n  NOT YET DOCUMENTED
	 * @return    NOT YET DOCUMENTED
	 */
	private static String pp(Node n) {
		return Dom4jUtils.prettyPrint(n);
	}


	/**
	 *  Description of the Method
	 *
	 * @param  s  Description of the Parameter
	 */
	private static void prtln(String s) {
		if (debug) {
			// System.out.println("MetsIngester: " + s);
			System.out.println(s);
		}
	}
	
}

