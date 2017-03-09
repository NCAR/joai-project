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
package org.dlese.dpc.xml.nldr;

import org.dlese.dpc.util.Utils;
import org.dlese.dpc.util.Files;
import org.dlese.dpc.xml.Dom4jUtils;
import org.dlese.dpc.xml.XMLUtils;
import org.dlese.dpc.xml.*;
import org.dlese.dpc.util.MetadataUtils;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.io.SAXReader;
import org.dom4j.Element;
import org.dom4j.Branch;
import org.dom4j.Attribute;
import org.dom4j.Node;
import org.dom4j.QName;
import org.dom4j.Namespace;

import java.util.*;
import java.lang.*;
import java.io.*;
import java.text.*;
import java.net.*;
import java.util.Hashtable;

/**
 *  Specializes NldrMetadataRecordExporter to handle export for the osm framework.
 *
 * @author    Jonathan Ostwald
 */
public class OsmRecordExporter extends NldrMetadataRecordExporter {
	private static boolean debug = false;
	
	// The vocab values that can cause an embargo
	static List EMBARGO_TYPES = Arrays.asList(
			new String []{"Author embargo", "Donor embargo", "Publisher embargo"});


	/**
	 *  Constructor that loads the given ADN record for editing. No validation is performed.
	 *
	 * @param  adnXML                 The ADN XML to start with
	 * @exception  DocumentException  If error parsing the XML
	 */
	public OsmRecordExporter(String xml) throws DocumentException {
		super(xml);
	}
	
	
	public List getAssetNodes () {
		List primaryAssetNodes = this.selectNodes ("/record/resources/primaryAsset");
		prtln (primaryAssetNodes.size() + " primaryAssetNodes found");
		List otherAssetNodes = this.selectNodes ("/record/resources/otherAsset");
		prtln (otherAssetNodes.size() + " otherAssetNodes found");
		
		List assetNodes = new ArrayList();
		assetNodes.addAll(primaryAssetNodes);
		assetNodes.addAll(otherAssetNodes);
		return assetNodes;
	}
	
	protected String id_path = "/record/general/recordID";
	
	public String getId() {
		return this.getTextAtPath(id_path);
	}
	
	private String url_path = "/record/general/urlOfRecord";
	
	public String getUrl () {
		return this.getTextAtPath(url_path);
	}
		
	public void setUrl (String newValue) {
		Element general = (Element)selectSingleNode ("/record/general");
		if (general.element("urlOfRecord") == null) {
			QName qname = DocumentHelper.createQName("urlOfRecord", general.getNamespace());
			general.elements().add (1, DocumentHelper.createElement (qname));
		}
		this.setTextAtPath (url_path, newValue);
	}
			
	public static String getExportXml (String xml, String collection, String repositoryUrl) throws Exception {
		OsmRecordExporter rec = new OsmRecordExporter (xml);
		
		String id = rec.getId();
		// String urlOfRecord = parentUrl + "/" + id;
		String urlOfRecord = repositoryUrl + "/" + collection + "/" + id;
		rec.setUrl (urlOfRecord);
		List allAssetNodes = rec.getAssetNodes();
		// prtln ("parentUrl: " + parentUrl);
		/*
		the url for assetNodes has the form:
		http://nldr.library.ucar.edu/collections/soars/2009_Goode_Sharome.pdf
		*/
		
		for (int i=0;i<allAssetNodes.size();i++) {
			Element assetElement = (Element)allAssetNodes.get(i);
			try {
				String physicalUrl = assetElement.attributeValue("url");
				
				// if a record is embargoed, we simply do not provide the url
				String citableUrl = (rec.isEmbargoed() ? "" :
									urlOfRecord + "/" + new File (physicalUrl).getName());
									
				assetElement.addAttribute("url", citableUrl);
			} catch (Throwable t) {
				prtln ("could not process assetElement: " + t.getMessage());
				prtln ("assetElement: " + Dom4jUtils.prettyPrint(assetElement));
			}
		}
				
		// prtln (Dom4jUtils.prettyPrint(rec.getXmlNode())); // tihs is fine!
		return Dom4jUtils.prettyPrint(rec.getXmlNode());
	}
		
	boolean isEmbargoed () {
		List drEls = selectNodes ("/record/coverage/dateRange");
		// prtln (drEls.size() + " date ranges found");
		Date now = new Date();
		for (Iterator i=drEls.iterator();i.hasNext();) {
			DateRange dateRange = new DateRange ((Element)i.next());
			if (this.EMBARGO_TYPES.contains(dateRange.type) &&
			    dateRange.getStartDate().before(now) && 
				now.before(dateRange.getEndDate())) {
				
				// prtln ("embargoed by " + dateRange.type);
				return true;
			}
		}
		return false;
	}
			
	
	public static void main (String [] args) throws Exception {
		// /Users/ostwald/devel/dcs-records/ccs-records/osm/osm/OSM-000-000-000-001.xml
		String localDir = "/Users/ostwald/devel/dcs-records/ccs-records";
		String osm_next = localDir + "/osm";
		String collection = "osm-export-collection";
		String id = "OSM-EXPORT-000-000-000-002";
		
		String filename = id+".xml";
		String xmlPath = osm_next + "/" + collection + "/" + filename;
		String xml = Files.readFile(xmlPath).toString();
 		OsmRecordExporter rec = new OsmRecordExporter(xml);
		// pp(rec.getXmlNode());

		prtln ("------");
		if (rec.isEmbargoed ())
			prtln ("EMBARGOED");
		else
			prtln ("NOT embargoed");
		
		String repositoryUrl = "http://nldr.library.ucar.edu/repository";
		// String repositoryUrl = "http://localhost/nldr";
		String exported = OsmRecordExporter.getExportXml(xml, collection, repositoryUrl);
		prtln ("exported \n" + exported);
	}

/* 	static void batchParseDate () {
		String[] testCases = {
			"",
			" ",
			"2005",
			"2005-01",
			"2005-01-30",
			"2005-01-30T01:01:01Z",
			"2005-01-30T23:01:01z",
			"2005-13",
			"2005-01-33",
			"2005-01-30T00:01:01Z",
			"2005-01-30T24:01:01z",
			"2005-01-30T23:99:01z",
			"2005-01-30T23:59:60z"
			};
			
		for (int i = 0; i < testCases.length; i++) {
			String dateString = testCases[i];
			parseDateString (dateString);
		}
	}
	
	static void parseDateString (String dateStr) {
		try {
			Date parsed = MetadataUtils.parseUnionDateType(dateStr);
			prtln ("parsed: " + parsed.toGMTString());
		} catch (Exception e) {
			prtln ("couldn't parse dateString: \"" + dateStr + "\"");
		}
	}
 */	
	
	static void showAssets (OsmRecordExporter rec) {
		prtln ("\nAssets");
		showAssets (rec.getAssetNodes());
	}
	
	static void showAssets (List assets) {
		showAssets (assets.iterator());
	}
	
	static void showAssets (Iterator i) {
		int counter = 0;
		while (i.hasNext()) {
			Element assetEl = (Element)i.next();
			String url = assetEl.attributeValue("url", "N/A");
			String title = assetEl.attributeValue("title", "Untitled");
			String order = assetEl.attributeValue("order", "N/A");
			prtln (String.valueOf (++counter) + " - " + title + " (" + order + ")");
		}
	}
	
	/**
	 *  Output a line of text to standard out, with datestamp, if debug is set to true.
	 *
	 * @param  s  The String that will be output.
	 */
 	private static void prtln(String s) {
		if (debug) {
			// System.out.println("OsmRecordExporter: " + s);
			System.out.println(s);
		}
	}

	/**
	 *  Sets the debug attribute of the object
	 *
	 * @param  db  The new debug value
	 */
	public static void setDebug(boolean db) {
		debug = db;
	}
	

	class DateRange {
		Element element = null;
		String type;
		String start;
		String end;
		Date never;
		
		DateRange (Element element) {
			// pp (element);
			this.element = element;
			this.type = element.attributeValue("type", "");
			this.start = element.attributeValue ("start", null);
			this.end = element.attributeValue ("end", null);
			try {
				this.never = MetadataUtils.parseUnionDateType("5000");
			} catch (Exception e) {
				prtln ("never error: " + e.getMessage());
			}
		}
	
		Date getStartDate () {
			try {
				return MetadataUtils.parseUnionDateType(this.start);
			} catch (Exception e) {
				prtln ("couldn't parse start date: \"" + this.start + "\"");
			}
			return new Date (0);
		}
		
		Date getEndDate () {
			try {
				return MetadataUtils.parseUnionDateType(this.end);
			} catch (Exception e) {
				// end date is not required!
				// prtln ("couldn't parse end date: \"" + this.end + "\"");
			}
			return this.never;
		}
		
	}
		
}

