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
package org.dlese.dpc.schemedit.vocab;

import org.dlese.dpc.schemedit.SchemEditUtils;
import org.dlese.dpc.xml.Dom4jUtils;
import org.dlese.dpc.xml.schema.SchemaHelper;
import org.dlese.dpc.xml.schema.SchemaHelperException;
import org.dlese.dpc.xml.schema.SchemaNodeMap;

import java.io.*;
import java.util.*;
import java.text.*;
import java.net.*;
import java.lang.*;

import org.dom4j.*;



/**
 *  Command line routine that checks  fields files for well-formedness, and ensures that the xpaths associated with the field files
 exist within the given metadata framework.
 *
 *@author    ostwald <p>
 *
 *      $Id: FieldFilesCheck.java,v 1.3 2009/03/20 23:33:58 jweather Exp $
 */
public class FieldFilesCheck {
	
	private HashMap map = null;
	private URI listingUri = null;
	private URI schemaUri = null;
	private SchemaHelper schemaHelper = null;
	
	int filesRead = 0;
	List badPaths = null;
	List readerErrors = null;
	SchemaPaths schemaPaths = null;

	class FieldFileError  {
		public URI location;
		public String data;
		
		FieldFileError (URI location, String data) {
			this.location = location;
			this.data = data;
		}
	}
	
	class ReaderError extends FieldFileError {
		ReaderError (URI location, String data) {
			super(location, data);
			String [] splits = data.split("\\:");
			if (splits.length == 3) {
				this.data = splits[2].trim();
			}
		}
	}
	
	class SchemaPaths {
		Map map = null;
		
		SchemaPaths (SchemaNodeMap schemaNodeMap) {
			map = new TreeMap ();
			for (Iterator i=schemaHelper.getSchemaNodeMap().getKeys().iterator();i.hasNext();) {
				String path = (String)i.next();
				map.put (path, new ArrayList());
			}
		}
		
		boolean isLegalPath (String xpath) {
			if (xpath == null || xpath.trim().length() == 0)
				return false;
			return (map.containsKey(xpath));
	}
		
		void markAsSeen (String xpath, URI uri) {
			List locs = (List) map.get(xpath);
			if (locs != null) {
				locs.add (uri);
				map.put (xpath, locs);
			}
			else {
				prtln ("add error: no xpath found for " + xpath);
			}
		}
		
		List getLocs (String xpath) {
			return (List)map.get(xpath);
		}
		
		List getUnseenPaths () {
			List ret = new ArrayList();
			for (Iterator i=map.keySet().iterator();i.hasNext();) {
				String key = (String)i.next();
				List locs = getLocs (key);
				if (locs == null || locs.size() == 0){
					ret.add (key);
				}
			}
			return ret;
		}
		
		Map getMultiples () {
			Map multiples = new TreeMap();
			for (Iterator i=map.keySet().iterator();i.hasNext();) {
				String key = (String)i.next();
				List locs = getLocs (key);
				if (locs.size() > 1) {
					multiples.put (key, locs);
				}
			}
			return multiples;
		}
	}
	
	/**
	 *  Constructor for the FieldFilesCheck object
	 */
	public FieldFilesCheck (URI listingUri, URI schemaUri) throws Exception {
		this.schemaUri = schemaUri;
		this.listingUri = listingUri;
		
		String dateStamp = new SimpleDateFormat( "MMM d, yyyy h:mm:ss a zzz" ).format( new Date() );
		prtln("\nFieldFilesCheck - " + dateStamp + "\n");
		prtln ("File Listing: " + listingUri.toString());
		prtln ("Schema: " + schemaUri.toString());
		prtln ("");
		
		String scheme = schemaUri.getScheme();
		// prtln ("SchemaUri scheme: " + scheme);
		try {
			if (scheme.equals("file"))
				schemaHelper = new SchemaHelper (new File (schemaUri.getPath()));
			else if (scheme.equals("http"))
				schemaHelper = new SchemaHelper (schemaUri.toURL());
			else
				throw new Exception ("ERROR: Unrecognized scheme (" + scheme + ")");
			
			if (schemaHelper == null)
				throw new Exception ();
		} catch (Exception e) {
			if (e.getMessage() != null && e.getMessage().trim().length() > 0)
				throw new Exception ("Unable to instantiate SchemaHelper at " + schemaUri + ": " + e.getMessage());
			else
				throw new Exception ("Unable to instantiate SchemaHelper at " + schemaUri);
		}
	}
	/**
	*  Read a listing of URIs Fields files from directoryUri and then loads each
	 *  of the listed files as FieldInfoReader objects, which are stored in a map.
	 */
	void doCheck () throws Exception {
		Document fieldsFileListing = null;
		try {
			fieldsFileListing = SchemEditUtils.getLocalizedXmlDocument(listingUri);
		} catch (Exception e) {
			throw new Exception ("ERROR: File listing document either does not exist or cannot be parsed as XML");
		}

		// prtln("Processing fields file listing at : " + listingUri.toString());

		Node filesNode = fieldsFileListing.selectSingleNode("/metadataFieldsInfo/files");
		if (filesNode == null) {
			throw new Exception ("no filesNode found");
		}

		Element filesElement = (Element) filesNode;
		URI baseUri = null;
		try {
			baseUri = new URI (filesElement.attributeValue("uri"));
		} catch (URISyntaxException e) {
			throw new Exception (e.getMessage());
		}

		filesRead = 0;
		badPaths = new ArrayList ();
		readerErrors = new ArrayList ();
		schemaPaths = new SchemaPaths (schemaHelper.getSchemaNodeMap());
		// load each of the files in the fields file listing
		for (Iterator i = filesElement.elementIterator(); i.hasNext(); ) {
			Node fileNode = (Node) i.next();
			String fileName = fileNode.getText();
			FieldInfoReader reader = null;
			String xpath;
			try {
				URI myUri = baseUri.resolve(fileName);
				try {
					reader = new FieldInfoReader(myUri);
				} catch (Exception e) {
					readerErrors.add (new ReaderError (myUri, e.getMessage()));
					continue;
				}
					
				filesRead++;
				
				try {
					xpath = reader.getPath();
				} catch (Throwable pathEx) {
					badPaths.add (new FieldFileError (myUri, "path not found"));
					continue;
				}
				
				if (!schemaPaths.isLegalPath (xpath)) {
					/* throw new Exception ("ERROR: Fields file at " + myUri.toString() + " contains an illegal path: " + xpath); */
					badPaths.add (new FieldFileError (myUri, xpath));
				}
				else {
					schemaPaths.markAsSeen (xpath, myUri);
				}
				
			} catch (Throwable t) {
				prtln(t.getMessage());
			}
		}
	}

	void doReport () {
		prtln (filesRead + " fields files read\n");
		
		if (readerErrors.size() > 0) {
			prtln (readerErrors.size() + " files could not be read:");
			for (Iterator i=readerErrors.iterator();i.hasNext();) {
				FieldFileError error = (FieldFileError)i.next();
				prtln (error.data);
				prtln ("");
			}
		}
		else {
			prtln ("All Listed Files were successfully read\n");
		}
		
		if (badPaths.size() > 0) {
			prtln (badPaths.size() + " bad Xpaths found:");
			for (Iterator i=badPaths.iterator();i.hasNext();) {
				FieldFileError error = (FieldFileError)i.next();
				prtln (error.location.toString());
				prtln ("\tbad path: " + error.data);
				prtln ("");
			}
		}
		else {
			prtln ("No bad Xpaths found\n");
		}
		
		List unSeenPaths = schemaPaths.getUnseenPaths();
		if (unSeenPaths == null || unSeenPaths.size() == 0)
			prtln ("\nAll Xpaths defined in schema have corresponding field files\n");
		else {
			prtln ("\n" + unSeenPaths.size() + " Xpaths have no corresponding field file:");
			for (Iterator i=unSeenPaths.iterator();i.hasNext();) {
				prtln ("\t" + (String)i.next());
			}
		}
		
		Map multiples = schemaPaths.getMultiples();
		if (multiples.size() > 0) {
			prtln ("\n" + multiples.size() + " Xpaths are contained in mulitple fields files:");
			for (Iterator i=multiples.keySet().iterator();i.hasNext();) {
				String key = (String)i.next();
				List locations = (List)multiples.get (key);
				prtln (key);
				for (Iterator j=locations.iterator();j.hasNext();) {
					URI uri = (URI)j.next();
					prtln ("\t" + uri.toString());
				}
				prtln ("");
			}
		}
	}
		
	/**
	 *  Read a set of fields files
	 *
	 *@param  args           The command line arguments
	 *@exception  Exception  Description of the Exception
	 */
	public static void main(String[] args) {
		
		// usage assumes this script is being called from the shell script named "checkFieldFiles"
		String usage = "Usage: checkFieldFiles fieldsFileListingUri schemaUri";

		if (args.length != 2) {
			prtln(usage);
			System.exit(1);
		}
		
		String devel_adn = "file:///devel/ostwald/metadata-frameworks/metadata-ui/adn/filename-list.xml";
		
		String dlese_adn_0_6_50 = "http://www.dlese.org/Metadata/adn-item/0.6.50/build/filename-list.xml";
		String dlese_news_opps_1_0_00 = "http://www.dlese.org/Metadata/news-opps/1.0.00/build/filename-list.xml";
		String dlese_dlese_anno_0_1_01 = "http://www.dlese.org/Metadata/annotation/0.1.01/build/filename-list.xml";
		String dlese_dlese_anno_1_0_0 = "http://www.dlese.org/Metadata/annotation/1.0.00/build/filename-list.xml";
		
		String listing = devel_adn;
		String schema = "http://www.dlese.org/Metadata/adn-item/0.6.50/record.xsd";
		
		try {
			// FieldFilesCheck checker = new FieldFilesCheck(new URI (listing), new URI (schema));
			FieldFilesCheck checker = new FieldFilesCheck(new URI (args[0]), new URI (args[1]));
			checker.doCheck();
			checker.doReport();
		} catch (Exception e) {
			prtln ("ERROR: " + e.getMessage());
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

	/**
	 *  Output a line of text to standard out, with datestamp, if debug is set to
	 *  true.
	 *
	 *@param  s  The String that will be output.
	 */
	private static void prtln(String s) {
		System.out.println(s);
	}
}

