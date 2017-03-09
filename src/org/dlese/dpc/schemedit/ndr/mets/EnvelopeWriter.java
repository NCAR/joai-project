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
import java.util.regex.*;
import java.net.*;
import java.io.*;
import org.dlese.dpc.schemedit.*;

import org.dlese.dpc.standards.asn.NameSpaceXMLDocReader;
import org.dlese.dpc.schemedit.test.TesterUtils;
import org.dlese.dpc.xml.Dom4jUtils;
import org.dlese.dpc.xml.schema.SchemaHelper;
import org.dlese.dpc.xml.schema.DocMap;
import org.dlese.dpc.util.Files;
import org.dlese.dpc.util.Utils;
import org.dlese.dpc.ndr.toolkit.ContentUtils;
import org.dom4j.*;

/**
 * @author    ostwald
 */
public abstract class EnvelopeWriter {

	private static boolean debug = true;
	NameSpaceXMLDocReader reader = null;
	DocMap docMap = null;
	SchemaHelper sh = null;
	
	Element fileGrp = null;
	Element structMap = null;
	String collection = null;

	/**
	 *  Constructor for the EnvelopeWriter object<p>
	 TODO: ensure envelope validates against mets schema!
	 *
	 * @param  path  NOT YET DOCUMENTED
	 */

	EnvelopeWriter(String ddsServiceUrl, String collection ) throws Exception {
		this.collection = collection;
		try {
			URL metsSchemaUrl = new URL ("http://www.loc.gov/standards/mets/mets.xsd");
			String rootElementName = "this:mets";
			sh = new SchemaHelper(metsSchemaUrl, rootElementName);
		} catch (Exception e) {
			prtln ("could not load schema helper for METS: " + e.getMessage());
			return;
		}
		
		try {
			metsInit();
		} catch (Throwable t) {
			prtln ("mets document could not be initialized: " + t.getMessage());
			return;
		}

		RepositoryUtils repoUtils = new RepositoryUtils (ddsServiceUrl);
		Map itemRecordMap = repoUtils.getItemRecordMap (collection);
		
		for (Iterator i=itemRecordMap.keySet().iterator();i.hasNext();) {
			String id = (String)i.next();
			prtln ("\nprocessing " + id);
			Document doc = (Document)itemRecordMap.get(id);
			doc = Dom4jUtils.localizeXml(doc);
			try {
/* 				Node urlElement = doc.selectSingleNode ("/record/general/url");
				if (urlElement == null)
					throw new Exception ("urlElement not found");
				String urlStr = urlElement.getText().trim();
				if (urlStr == null || urlStr.length() == 0)
					throw new Exception ("url not found for record " + id);
				URL url = new URL (urlStr); */
				
				URL url = getResourceUrl (doc);
				this.addRecord(id, url, doc, getMetadataFormat());
			} catch (Exception e) {
				prtln ("could not process " + id + ": " + e.getMessage());
			}
		}
	}
	
	abstract URL getResourceUrl (Document doc) throws Exception;
	
	abstract String getMetadataFormat ();
	
	void metsInit() throws Exception {
		try {
			Document stub = sh.getMinimalDocument();
			this.reader = new NameSpaceXMLDocReader(stub);
			this.docMap = new DocMap (this.reader.getDocument(), this.sh);
		} catch (Exception e) {
			prtln("could not process xml: " + e.getMessage());
			return;
		}
		
		Element fileSec = this.reader.getRootElement().addElement (this.reader.getQName("this:fileSec"));
		fileGrp = fileSec.addElement (this.reader.getQName("this:fileGrp"));
		fileGrp.addAttribute("ID", "GRP01");
		fileGrp.addAttribute("USE", "ACCESS");
		
		// the struct map element is already present, but it has a div element that must be removed
		structMap = (Element) this.reader.getNode("this:mets/this:structMap/this:div");
		// structMap.clearContent();
	}		

	/**
	 *  Gets the fileGrp attribute of the EnvelopeWriter object
	 *
	 * @return    The fileGrp value
	 */
	void addMetadata (Document metadata, String dmdID, String xmlFormat) throws Exception {
		Element dmdSec = this.reader.getRootElement().addElement(this.reader.getQName ("this:dmdSec"));
		dmdSec.addAttribute("ID", dmdID);
		
		Element mdWrap = dmdSec.addElement (this.reader.getQName("this:mdWrap"));
		mdWrap.addAttribute("MIMETYPE", "text/xml");
		mdWrap.addAttribute("MDTYPE", "OTHER");
		mdWrap.addAttribute("OTHERMDTYPE", xmlFormat);
		
		Element xmlData = mdWrap.addElement (this.reader.getQName("this:xmlData"));
		xmlData.add (metadata.getRootElement().createCopy());
	}

	void orderElements () {
		this.docMap.orderSequenceElements(this.reader.getRootElement());
	}

	/**
	 *  Adds a feature to the File attribute of the EnvelopeWriter object
	 *
	 * @param  url            an URL pointing to a binary object
	 * @param  fileID         the id to be assigned to this file
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	void addFile(URL url, String fileID) throws Exception {
		DownLoadedFile dlf = new DownLoadedFile(url);
		if (!dlf.getIsBinary())
			throw new Exception("content at " + url + " is not binary!");
		Element fileEl = this.fileGrp.addElement (this.reader.getQName("this:file"));
		fileEl.addAttribute ("ID", fileID);
		fileEl.addAttribute ("MIMETYPE", dlf.getContentType());
		
		Element fContent = fileEl.addElement(this.reader.getQName("this:FContent"));
		/* ID is optional and using filename is problematic because file names are often
			not schema-valid as IDs.
			Instead, get file name (when needed) from image URL, which is in the metadata
		*/
		// fContent.addAttribute("ID", dlf.getFileName());
		
		
		fContent.addAttribute("USE", "ACCESS");
		
		Element binData = fContent.addElement (this.reader.getQName("this:binData"));
		
		binData.setText(dlf.getContent());
		
/* 		// track memory usage
		java.lang.Runtime runtime = java.lang.Runtime.getRuntime();
		long totalMemory = runtime.totalMemory();
		long freeMemory = runtime.freeMemory();
		prtln ("total memory: " + totalMemory);
		prtln ("free memory: " + freeMemory);
		prtln ("total - free: " + (totalMemory - freeMemory)); */
		
/* 		// the steps below do not seem to matter 
		dlf = null;
		System.runFinalization();
		System.gc(); */
		
		
		// to use fake data ...
		// binData.setText("UjBsR09EbGhjZ0dTQUxNQUFBUUNBRU1tQ1p0dU1GUXhEUzhi"); // fake data
	}

	void addStructDiv(String dmdID, String fileID) throws Exception {

		Element div = this.structMap.addElement ("this:div");
		div.addAttribute("DMDID", dmdID);
		
		Element fptr = div.addElement ("this:fptr");
		fptr.addAttribute("FILEID", fileID);

	}

	public void addRecord (String id, URL contentUrl, Document metadata, String xmlFormat) throws Exception {
		String dmdID = "dmd-" + id;
		String fileID = "file-" + id;
		addFile(contentUrl, fileID);
		addMetadata(metadata, dmdID, xmlFormat);
		addStructDiv(dmdID, fileID);
		orderElements();
		prtln ("added " + id);
	}
	
	/**
	 *  The main program for the EnvelopeWriter class
	 *
	 * @param  args           The command line arguments
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public static void main(String[] args) throws Exception {
/* 		TesterUtils.setSystemProps();
		prtln("Mets Doc");
		String baseUrl = "http://dcs.dls.ucar.edu/schemedit/services/ddsws1-1";
		String collection = "1247859732310";
		EnvelopeWriter md = new EnvelopeWriter(baseUrl, collection);
		// pp (md.reader.getDocument());
		
		// URL contentUrl = new URL("http://www.libpng.org/pub/png/img_png/png-gammatest-ie52mac.png");
		// Document metadata = DocumentHelper.createDocument(DocumentHelper.createElement("record"));
		// md.addRecord (id, contentUrl, metadata, "ncs_item");
		// pp (md.reader.getDocument());
		
		String path = "H:/Documents/Alliance/mets-work/mets-record.xml";
		Dom4jUtils.writeDocToFile(md.reader.getDocument(), new File (path));
		prtln ("wrote to " + path); */
	}
	

	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  n  NOT YET DOCUMENTED
	 */
	private static void pp(Node n) {
		prtln(Dom4jUtils.prettyPrint(n));
	}


	/**
	 *  Description of the Method
	 *
	 * @param  s  Description of the Parameter
	 */
	private static void prtln(String s) {
		if (debug) {
			// System.out.println("EnvelopeWriter: " + s);
			System.out.println(s);
		}
	}

}

