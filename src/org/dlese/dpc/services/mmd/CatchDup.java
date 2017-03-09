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
package org.dlese.dpc.services.mmd;

import org.dlese.dpc.util.*;
import java.net.URL;
import java.net.HttpURLConnection;
import java.net.*;

import java.io.InputStream;
import java.io.IOException;
import java.io.*;
import java.util.regex.Pattern;

import org.dom4j.Node;
import org.dom4j.Element;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;
import java.util.List;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;

import org.dlese.dpc.xml.XMLDoc;
import org.dlese.dpc.xml.XMLException;

/**
 *  Catches duplicate resources
 *
 * @author    Sonal Bhushan, John Weatherley
 */

public class CatchDup {
	private static boolean debug = false;

	long check1, check2;
	String url1, url2, id1, id2;
	String exclusion_file;
	private static Document document = null;
	private static HashMap idMap = null;
	
	private static boolean documentIsLoaded = false;

	public static void reloadIdExclusionDocument() {
		prtln("reloadIdExclusionDocument()");
		documentIsLoaded = false;	
	}
	
	/**
	 *  Constructor for the CatchDup object
	 *
	 * @param  c1   Checksum of the First Resource
	 * @param  c2   Checksum of the Second Resource
	 * @param  u1   Url of the First Resource
	 * @param  u2   Url of the Second Resource
	 * @param  id1  ID of the First Resource
	 * @param  id2  ID of the Second Resource
	 * @param  exc  the location of the dup exclusion file
	 */
	public CatchDup(long c1, String u1, String id1, long c2, String u2, String id2, String exc) {
		this.check1 = c1;
		this.check2 = c2;
		this.url1 = u1;
		this.url2 = u2;
		this.id1 = id1;
		this.id2 = id2;
		this.exclusion_file = exc;
	}

	
	/**
	 *  Gets the dup attribute of the CatchDup object
	 *
	 * @return    The dup value
	 */
	public boolean isDup() {
		boolean yes = false;
		prtln("isDup()");
		
		// ----- Check for IDs in the non-dup list ----- :
		if (this.exclusion_file != null) {
			// try to connect to the dupexclusionFile

			try {
				if (!documentIsLoaded) {
					
					URI uri = URI.create(this.exclusion_file); 
					//System.out.println("uri is " + uri.toString());
					java.net.URLConnection urlConnection = uri.toURL().openConnection();
					InputStream istm; 
					if (urlConnection instanceof java.net.HttpURLConnection)
					{
						int timeOutPeriod = 180000;			
						istm = TimedURLConnection.getInputStream(URI.create(this.exclusion_file).toURL(),timeOutPeriod);
					}
					else
					{
						URL url = new URL(this.exclusion_file);
						File file = new File(url.getFile()); 
						istm = url.openStream();    
					}
					SAXReader reader = new SAXReader();
					document = reader.read(istm);
					
					List allIds = document.selectNodes("//id");
					idMap = new HashMap();
					for (Iterator iterids = allIds.iterator(); iterids.hasNext(); ) {
						Node node_id = (Node) iterids.next();
						idMap.put(node_id.getText(),"");
						// prtln("nondup: " + node_id.getText());
					}					
					documentIsLoaded = true;
				}
				
				// if either url is in the urlStartsWith, then return false
				List excludeByStartsWith = document.selectNodes ("/nonDups/urlsToExclude/urlStartsWith");
				if (excludeByStartsWith != null) {
					for (Iterator iter = excludeByStartsWith.iterator();iter.hasNext();) {
						String subUrl = ((Element)iter.next()).getTextTrim();
						
						if (subUrl.length() == 0) {
							prtln ("pattern is empty - it would match everything - continuing ...");
							continue;
						}
						
						if (url1.startsWith(subUrl)) {
							prtln ("url1 (" + url1 + ") found on excludeByStartsWith - returning FALSE");
							return false;
						}
						if (url2.startsWith(subUrl)) {
							prtln ("url2 (" + url2 + ") found on excludeByStartsWith - returning FALSE");
							return false;
						}
					}
				}
				
				// if either url is in the urlMatchesWith, then return false
				List excludeByMatchesWith = document.selectNodes ("/nonDups/urlsToExclude/urlMatchesWith");
				if (excludeByMatchesWith != null) {
					for (Iterator iter = excludeByMatchesWith.iterator();iter.hasNext();) {
						String matchExpression = ((Element)iter.next()).getTextTrim();
						if (matchExpression.length() == 0) {
							// empty matchExpression will match anything!
							prtln ("matchExpression is empty! - continuing ...");
							continue;
						}
						Pattern p = null;
						try {
							p = Pattern.compile(matchExpression);
						} catch (Throwable t) {
							prtln ("illegal matchExpression: " + t.getMessage());
							continue;
						}
						
						if (p.matcher(url1).find()) {
							prtln ("url1 (" + url1 + ") matches exclude expression - returning FALSE");
							return false;
						}
						if (p.matcher(url2).find()) {
							prtln ("url2 (" + url2 + ") matches exclude expression - returning FALSE");
							return false;
						}
					}
				}
				
				// If both IDs are in the non-dups file as a whole, dig deeper to see if they are in the same non-dups group:
				if(idMap != null && idMap.containsKey(id1) && idMap.containsKey(id2)) {
					prtln("Searching non-dups file...");
				
					List list = document.selectNodes("//nonDup");
					Node node = null;
	
					if (list != null) {
	
						for (Iterator iter = list.iterator(); iter.hasNext(); ) {
							node = (Node) iter.next();
							List ids = node.selectNodes(".//id");
	
							if (ids != null) {
								int numids = ids.size();
								String[] id_t = new String[numids];
								int k = 0;
	
								for (Iterator iterids = ids.iterator(); iterids.hasNext(); ) {
									Node node_id = (Node) iterids.next();
									id_t[k] = node_id.getText();
									k++;
								}
	
								boolean found1 = false;
								boolean found2 = false;
								for (int i = 0; i < numids; i++) {
									if (id1.equals(id_t[i])) {
										found1 = true;
										break;
									}
								}
	
								if (found1 == true) {
									for (int i = 0; i < numids; i++)
										if (id2.equals(id_t[i])) {
											found2 = true;
											break;
										}
								}
	
								// If both IDs are in a nondup list, return false:
								if ((found1 == true) && (found2 == true)) {
									
									prtln("both IDs were in the non-dups list, returning false. id1:" + id1 + " id2:" + id2);
									return false;
								}
							}
						}
					}
				}
			} catch (URLConnectionTimedOutException exc) {
				prtlnErr("Error reading the IDMapper dup exclusion file " + this.exclusion_file + " Reason: " + " The URL Connection timed out." );
			} catch (IOException ioe) {
				prtlnErr("Error reading the IDMapper dup exclusion file. The UrlConnection threw an IOException while attempting to connect to " + this.exclusion_file + " : " + ioe);
			} catch (DocumentException e) {
				prtlnErr("Error parsing the IDMapper dup exclusion file located at " + this.exclusion_file + " : " + e);
			} catch (Exception e){
				prtlnErr("Error reading the IDMapper dup exclusion file " + this.exclusion_file + " Reason:" + e);	
			}
		}

		
		try {
			if (check1 != 0) {
				
				// ----- If the checksum of the two are the same, verify that they are dupes ----- :
				if (check1 == check2) {

					// And if the URLs are different, check for anchor links:
					if (!(url1.equals(url2))) {
						boolean hasAnchor = false;
						String sub1 = url1;
						int panchor1 = url1.lastIndexOf("#");
						if ((panchor1 > -1)) {
							sub1 = url1.substring(0, panchor1);
							hasAnchor = true;
						}
						
						String sub2 = url2;
						int panchor2 = url2.lastIndexOf("#");
						if ((panchor2 > -1)) {
							sub2 = url2.substring(0, panchor2);
							hasAnchor = true;
						}
						
						prtln("sub1: " + sub1 + " sub2: " + sub2);
						
						if (hasAnchor) {
							if (!(sub1.equals(sub2)))
								yes = true;
						}
						else
							yes = true;
					}
					else
						yes = true;

				}
			}
			else {// if base checksum == 0
				if (url1.equals(url2))
					yes = true;
			}

		} catch (Exception e) {
			prtlnErr("Error while trying to check two records for dups in CatchDup.java :  " + e);
			yes = false;
		}
		
		prtln("isDup(): " + yes);
		return yes;
	}


	private static void prtlnErr(String msg) {
		System.err.println("CatchDup Error: " + msg);
	}
	
	private static void prtln(String msg) {
		if(debug)
			System.out.println("CatchDup: " + msg);
	}	
}// end class CatchDup

